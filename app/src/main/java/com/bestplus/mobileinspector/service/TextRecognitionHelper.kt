package com.bestplus.mobileinspector.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Распознавание показаний счётчика с помощью YOLOv8n TFLite-модели.
 * Модель обучена на 10 классов (цифры 0–9).
 * Заменяет ML Kit OCR, который захватывал лишний текст (серийники, kWh).
 */
object TextRecognitionHelper {

    private const val TAG = "MeterOCR"
    private const val MODEL_FILENAME = "best_float16_full.tflite"
    private const val INPUT_SIZE = 640
    private const val NUM_CLASSES = 10
    private const val CONF_THRESHOLD = 0.25f
    private const val IOU_THRESHOLD = 0.45f

    @Volatile
    private var interpreter: Interpreter? = null

    private fun getInterpreter(context: Context): Interpreter {
        return interpreter ?: synchronized(this) {
            interpreter ?: createInterpreter(context).also { interpreter = it }
        }
    }

    private fun createInterpreter(context: Context): Interpreter {
        val model = loadModelFile(context)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        val interp = Interpreter(model, options)

        // Логируем информацию о модели
        val inputTensor = interp.getInputTensor(0)
        val outputTensor = interp.getOutputTensor(0)
        Log.d(TAG, "=== МОДЕЛЬ ЗАГРУЖЕНА ===")
        Log.d(TAG, "Input shape:  ${inputTensor.shape().contentToString()}, dtype: ${inputTensor.dataType()}")
        Log.d(TAG, "Output shape: ${outputTensor.shape().contentToString()}, dtype: ${outputTensor.dataType()}")

        return interp
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILENAME)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        Log.d(TAG, "Загрузка модели: $MODEL_FILENAME (${fileDescriptor.declaredLength / 1024} KB)")
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength,
        )
    }

    /**
     * Распознаёт цифры на фото счётчика и возвращает показание (строку цифр).
     * Детектирует каждую цифру отдельно, сортирует слева направо.
     */
    suspend fun recognizeTestimony(context: Context, imageFile: File): String =
        withContext(Dispatchers.Default) {
            val rawBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: throw IllegalArgumentException("Не удалось загрузить изображение")

            // Исправляем поворот по EXIF (CameraX сохраняет ориентацию в метаданных)
            val bitmap = fixRotation(rawBitmap, imageFile)

            Log.d(TAG, "=== НОВОЕ РАСПОЗНАВАНИЕ ===")
            Log.d(TAG, "Фото: ${rawBitmap.width}x${rawBitmap.height} → ${bitmap.width}x${bitmap.height}, файл: ${imageFile.name}")

            // Letterboxing: масштабируем пропорционально + серые поля
            val (letterboxed, padInfo) = letterbox(bitmap, INPUT_SIZE)

            val inputBuffer = bitmapToByteBuffer(letterboxed)

            val tflite = getInterpreter(context)

            // Определяем формат вывода
            val outputShape = tflite.getOutputTensor(0).shape()

            // YOLOv8 TFLite может выдавать [1, 14, 8400] или [1, 8400, 14]
            val dim1 = outputShape[1]
            val dim2 = outputShape[2]
            val transposed = dim1 < dim2
            val numPredictions: Int
            val numValues: Int

            if (transposed) {
                numValues = dim1
                numPredictions = dim2
            } else {
                numPredictions = dim1
                numValues = dim2
            }

            // Запускаем инференс
            val rawOutput = Array(1) { Array(dim1) { FloatArray(dim2) } }
            tflite.run(inputBuffer, rawOutput)

            // Парсим детекции
            val detections = mutableListOf<Detection>()
            val classCount = minOf(NUM_CLASSES, numValues - 4)

            for (i in 0 until numPredictions) {
                val cx: Float
                val cy: Float
                val w: Float
                val h: Float
                val classScores = FloatArray(classCount)

                if (transposed) {
                    cx = rawOutput[0][0][i]
                    cy = rawOutput[0][1][i]
                    w = rawOutput[0][2][i]
                    h = rawOutput[0][3][i]
                    for (c in 0 until classCount) {
                        classScores[c] = rawOutput[0][4 + c][i]
                    }
                } else {
                    cx = rawOutput[0][i][0]
                    cy = rawOutput[0][i][1]
                    w = rawOutput[0][i][2]
                    h = rawOutput[0][i][3]
                    for (c in 0 until classCount) {
                        classScores[c] = rawOutput[0][i][4 + c]
                    }
                }

                var maxScore = 0f
                var maxClass = 0
                for (c in classScores.indices) {
                    if (classScores[c] > maxScore) {
                        maxScore = classScores[c]
                        maxClass = c
                    }
                }

                if (maxScore >= CONF_THRESHOLD) {
                    detections.add(
                        Detection(
                            cx = cx, cy = cy, w = w, h = h,
                            classId = maxClass,
                            confidence = maxScore,
                        )
                    )
                }
            }

            // NMS
            val nmsDetections = nonMaxSuppression(detections)

            // Формируем результат
            val sorted = nmsDetections.sortedBy { it.cx }
            val result = sorted.joinToString("") { it.classId.toString() }
            Log.d(TAG, "Детекций: ${nmsDetections.size}, результат: \"$result\"")
            result
        }

    /**
     * Letterbox: пропорциональное масштабирование + серые поля (как в YOLO training pipeline).
     * Сохраняет пропорции изображения, заполняет оставшееся пространство серым (114, 114, 114).
     */
    private fun letterbox(bitmap: Bitmap, targetSize: Int): Pair<Bitmap, PadInfo> {
        val srcW = bitmap.width
        val srcH = bitmap.height
        val scale = minOf(targetSize.toFloat() / srcW, targetSize.toFloat() / srcH)
        val newW = (srcW * scale).toInt()
        val newH = (srcH * scale).toInt()
        val padX = (targetSize - newW) / 2
        val padY = (targetSize - newH) / 2

        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(scaled, padX.toFloat(), padY.toFloat(), null)

        return Pair(result, PadInfo(scale, padX, padY))
    }

    private data class PadInfo(val scale: Float, val padX: Int, val padY: Int)

    /**
     * Читает EXIF-ориентацию из JPEG и поворачивает bitmap.
     * CameraX сохраняет фото в ландшафтной ориентации сенсора + EXIF-тег поворота.
     * BitmapFactory.decodeFile() игнорирует EXIF → изображение повёрнуто.
     */
    private fun fixRotation(bitmap: Bitmap, imageFile: File): Bitmap {
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        Log.d(TAG, "EXIF orientation=$orientation → поворот на ${degrees.toInt()}°")
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f) // R
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)  // G
            buffer.putFloat((pixel and 0xFF) / 255f)           // B
        }
        buffer.rewind()
        return buffer
    }

    private fun nonMaxSuppression(detections: List<Detection>): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            sorted.removeAll { iou(best, it) > IOU_THRESHOLD }
        }
        return result
    }

    private fun iou(a: Detection, b: Detection): Float {
        val aX1 = a.cx - a.w / 2; val aY1 = a.cy - a.h / 2
        val aX2 = a.cx + a.w / 2; val aY2 = a.cy + a.h / 2
        val bX1 = b.cx - b.w / 2; val bY1 = b.cy - b.h / 2
        val bX2 = b.cx + b.w / 2; val bY2 = b.cy + b.h / 2

        val interX1 = maxOf(aX1, bX1); val interY1 = maxOf(aY1, bY1)
        val interX2 = minOf(aX2, bX2); val interY2 = minOf(aY2, bY2)

        val interArea = maxOf(0f, interX2 - interX1) * maxOf(0f, interY2 - interY1)
        val aArea = (aX2 - aX1) * (aY2 - aY1)
        val bArea = (bX2 - bX1) * (bY2 - bY1)
        val unionArea = aArea + bArea - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    private data class Detection(
        val cx: Float,
        val cy: Float,
        val w: Float,
        val h: Float,
        val classId: Int,
        val confidence: Float,
    )
}
