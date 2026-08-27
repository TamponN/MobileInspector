package com.bestplus.mobileinspector.service

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap

/**
 * Тип модели детектора.
 *
 * YOLO: 10 классов, TFLite, classId = цифра (0–9).
 * RF-DETR: 11 классов, ONNX, class 0 — группировочный (пропускается),
 *           classId 1–10 → цифры 0–9 (digit = classId − 1).
 */
enum class ModelType(
    val fileName: String,
    val numClasses: Int,
    val classOffset: Int,
    val displayName: String,
    val inputSize: Int,
) {
    YOLO_FULL("best_float16_full.tflite",    numClasses = 10, classOffset = 0, displayName = "Стандартная",  inputSize = 640),
    YOLO_HEAD("best_float16_head.tflite",    numClasses = 10, classOffset = 0, displayName = "Быстрая",      inputSize = 640),
    RF_DETR("inference_model.onnx",          numClasses = 11, classOffset = 1, displayName = "Точная",       inputSize = 384),
    RF_DETR_100EP("inference_model_100ep.onnx", numClasses = 11, classOffset = 1, displayName = "Точная+",   inputSize = 384),
    ;

    val isOnnx: Boolean get() = fileName.endsWith(".onnx")

    companion object {
        fun fromFileName(name: String) = entries.find { it.fileName == name } ?: YOLO_FULL
    }
}

object TextRecognitionHelper {

    private const val TAG = "MeterOCR"
    private const val CONF_THRESHOLD = 0.25f
    private const val IOU_THRESHOLD = 0.45f

    // TFLite interpreters (YOLO models)
    private val interpreters = ConcurrentHashMap<String, Interpreter>()

    // ONNX Runtime sessions (RF-DETR models)
    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private val ortSessions = ConcurrentHashMap<String, OrtSession>()

    // Models that failed to load — don't retry
    private val failedModels = ConcurrentHashMap.newKeySet<String>()

    fun isModelAvailable(modelType: ModelType) = modelType.fileName !in failedModels

    // ── Entry point ──────────────────────────────────────────────────────────

    suspend fun recognizeTestimony(
        context: Context,
        imageFile: File,
        modelType: ModelType = ModelType.YOLO_FULL,
    ): String = withContext(Dispatchers.Default) {
        val rawBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: throw IllegalArgumentException("Не удалось загрузить изображение")
        val bitmap = ImageUtils.applyExifRotation(rawBitmap, imageFile)

        Log.d(TAG, "=== РАСПОЗНАВАНИЕ [${modelType.displayName}] ===")
        Log.d(TAG, "Фото: ${rawBitmap.width}x${rawBitmap.height} → ${bitmap.width}x${bitmap.height}")

        if (modelType.isOnnx) {
            // RF-DETR: simple resize (no letterbox) — matches training transforms
            val resized = Bitmap.createScaledBitmap(bitmap, modelType.inputSize, modelType.inputSize, true)
            recognizeOnnx(context, resized, modelType)
        } else {
            val (letterboxed, _) = letterbox(bitmap, modelType.inputSize)
            recognizeTflite(context, letterboxed, modelType)
        }
    }

    // ── TFLite (YOLO) ────────────────────────────────────────────────────────

    private fun getTfliteInterpreter(context: Context, modelType: ModelType): Interpreter {
        if (modelType.fileName in failedModels) incompatibleError(modelType)
        return interpreters.getOrPut(modelType.fileName) {
            try {
                val model = loadModelFile(context, modelType.fileName)
                val options = Interpreter.Options().apply { setNumThreads(4) }
                val interp = Interpreter(model, options)
                Log.d(TAG, "=== TFLite [${modelType.displayName}] ===")
                Log.d(TAG, "Input : ${interp.getInputTensor(0).shape().contentToString()} ${interp.getInputTensor(0).dataType()}")
                Log.d(TAG, "Output: ${interp.getOutputTensor(0).shape().contentToString()} ${interp.getOutputTensor(0).dataType()}")
                interp
            } catch (e: Exception) {
                failedModels.add(modelType.fileName)
                Log.e(TAG, "[${modelType.displayName}] TFLite load failed: ${e.message}")
                incompatibleError(modelType, e)
            }
        }
    }

    private fun recognizeTflite(context: Context, letterboxed: Bitmap, modelType: ModelType): String {
        val inputBuffer = bitmapToHwcBuffer(letterboxed, modelType.inputSize)
        val tflite = getTfliteInterpreter(context, modelType)

        val outputShape = tflite.getOutputTensor(0).shape()
        val dim1 = outputShape[1]; val dim2 = outputShape[2]
        val transposed = dim1 < dim2
        val numPredictions = if (transposed) dim2 else dim1
        val numValues = if (transposed) dim1 else dim2
        val rawOutput = Array(1) { Array(dim1) { FloatArray(dim2) } }

        val startMs = System.currentTimeMillis()
        tflite.run(inputBuffer, rawOutput)
        Log.d(TAG, "[${modelType.displayName}] TFLite инференс: ${System.currentTimeMillis() - startMs} мс")

        val classCount = minOf(modelType.numClasses, numValues - 4)
        val detections = mutableListOf<Detection>()
        for (i in 0 until numPredictions) {
            val cx: Float; val cy: Float; val w: Float; val h: Float
            val scores = FloatArray(classCount)
            if (transposed) {
                cx = rawOutput[0][0][i]; cy = rawOutput[0][1][i]
                w  = rawOutput[0][2][i]; h  = rawOutput[0][3][i]
                for (c in 0 until classCount) scores[c] = rawOutput[0][4 + c][i]
            } else {
                cx = rawOutput[0][i][0]; cy = rawOutput[0][i][1]
                w  = rawOutput[0][i][2]; h  = rawOutput[0][i][3]
                for (c in 0 until classCount) scores[c] = rawOutput[0][i][4 + c]
            }
            var maxScore = 0f; var maxClass = 0
            for (c in scores.indices) { if (scores[c] > maxScore) { maxScore = scores[c]; maxClass = c } }
            if (maxScore >= CONF_THRESHOLD && maxClass >= modelType.classOffset)
                detections.add(Detection(cx, cy, w, h, maxClass, maxScore))
        }
        return formatResult(detections, modelType)
    }

    // ── ONNX Runtime (RF-DETR) ───────────────────────────────────────────────

    private fun getOrtSession(context: Context, modelType: ModelType): OrtSession {
        if (modelType.fileName in failedModels) incompatibleError(modelType)
        return ortSessions.getOrPut(modelType.fileName) {
            try {
                val bytes = context.assets.open(modelType.fileName).readBytes()
                val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(4) }
                val session = ortEnv.createSession(bytes, opts)
                Log.d(TAG, "=== ONNX [${modelType.displayName}] ===")
                session.inputNames.forEach { name ->
                    Log.d(TAG, "Input  '$name': ${session.inputInfo[name]?.info}")
                }
                session.outputNames.forEach { name ->
                    Log.d(TAG, "Output '$name': ${session.outputInfo[name]?.info}")
                }
                session
            } catch (e: Exception) {
                failedModels.add(modelType.fileName)
                Log.e(TAG, "[${modelType.displayName}] ONNX load failed: ${e.message}")
                incompatibleError(modelType, e)
            }
        }
    }

    private fun recognizeOnnx(context: Context, letterboxed: Bitmap, modelType: ModelType): String {
        val session = getOrtSession(context, modelType)

        // RF-DETR expects CHW float32 [1, 3, H, W] with ImageNet normalization
        val sz = modelType.inputSize
        val pixels = IntArray(sz * sz)
        letterboxed.getPixels(pixels, 0, sz, 0, 0, sz, sz)
        val inputData = FloatArray(3 * sz * sz)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255f
            val g = ((p shr 8)  and 0xFF) / 255f
            val b = (p          and 0xFF) / 255f
            // ImageNet normalization (matches rfdetr training transforms)
            inputData[i]               = (r - 0.485f) / 0.229f
            inputData[sz * sz + i]     = (g - 0.456f) / 0.224f
            inputData[2 * sz * sz + i] = (b - 0.406f) / 0.225f
        }

        val inputName = session.inputNames.iterator().next()
        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            FloatBuffer.wrap(inputData),
            longArrayOf(1, 3, sz.toLong(), sz.toLong()),
        )

        val startMs = System.currentTimeMillis()
        val outputs = session.run(mapOf(inputName to inputTensor))
        inputTensor.close()
        Log.d(TAG, "[${modelType.displayName}] ONNX инференс: ${System.currentTimeMillis() - startMs} мс")

        val detections = parseOrtOutputs(outputs, modelType)
        outputs.close()
        return formatResult(detections, modelType)
    }

    /**
     * Парсит выходы RF-DETR в единый список детекций.
     *
     * rfdetr ONNX формат:
     *   `dets`   [1, N, 4]  — абсолютные координаты (x1, y1, x2, y2) в пикселях [0, inputSize]
     *   `labels` [1, N, C]  — вероятности классов (sigmoid уже применён при экспорте)
     *
     * Запасной вариант — объединённый тензор [1, N, 4+C].
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseOrtOutputs(outputs: OrtSession.Result, modelType: ModelType): List<Detection> {
        val outputMap = outputs.associate { it.key to (it.value as OnnxTensor) }

        val detsKey   = outputMap.keys.firstOrNull { "det" in it.lowercase() }
        val labelsKey = outputMap.keys.firstOrNull {
            val k = it.lowercase()
            "label" in k || "logit" in k || "score" in k || "cls" in k
        }

        val detections = mutableListOf<Detection>()

        if (detsKey != null && labelsKey != null) {
            // rfdetr format: separate boxes + class scores
            val dets   = outputMap[detsKey]!!.floatBuffer
            val labels = outputMap[labelsKey]!!.floatBuffer
            val numQ   = outputMap[detsKey]!!.info.shape[1].toInt()
            val numC   = outputMap[labelsKey]!!.info.shape[2].toInt()

            var maxScoreSeen = 0f
            for (q in 0 until numQ) {
                // dets: (cx, cy, w, h) normalized [0, 1]
                val cx = dets[q * 4]
                val cy = dets[q * 4 + 1]
                val w  = dets[q * 4 + 2]
                val h  = dets[q * 4 + 3]

                // Class scores: raw logits — apply sigmoid, skip class 0
                var maxScore = 0f; var maxClass = 0
                for (c in modelType.classOffset until numC) {
                    val s = sigmoid(labels[q * numC + c])
                    if (s > maxScore) { maxScore = s; maxClass = c }
                }
                if (maxScore > maxScoreSeen) maxScoreSeen = maxScore
                if (maxScore >= CONF_THRESHOLD && w > 0f && h > 0f)
                    detections.add(Detection(cx, cy, w, h, maxClass, maxScore))
            }
            Log.d(TAG, "RF-DETR: $numQ queries, maxScore=%.3f, threshold=$CONF_THRESHOLD, found=${detections.size}".format(maxScoreSeen))
        } else {
            // Fallback: single combined output [1, N, 4+C]
            val tensor = outputMap.values.first()
            val raw    = tensor.floatBuffer
            val shape  = tensor.info.shape
            val dim1 = shape[1].toInt(); val dim2 = shape[2].toInt()
            val transposed = dim1 < dim2
            val numQ = if (transposed) dim2 else dim1
            val numV = if (transposed) dim1 else dim2
            val classCount = minOf(modelType.numClasses, numV - 4)

            for (q in 0 until numQ) {
                val cx: Float; val cy: Float; val w: Float; val h: Float
                val scores = FloatArray(classCount)
                if (transposed) {
                    cx = raw[0 * numQ + q]; cy = raw[1 * numQ + q]
                    w  = raw[2 * numQ + q]; h  = raw[3 * numQ + q]
                    for (c in 0 until classCount) scores[c] = raw[(4 + c) * numQ + q]
                } else {
                    cx = raw[q * numV + 0]; cy = raw[q * numV + 1]
                    w  = raw[q * numV + 2]; h  = raw[q * numV + 3]
                    for (c in 0 until classCount) scores[c] = raw[q * numV + 4 + c]
                }
                var maxScore = 0f; var maxClass = 0
                for (c in scores.indices) { if (scores[c] > maxScore) { maxScore = scores[c]; maxClass = c } }
                if (maxScore >= CONF_THRESHOLD && maxClass >= modelType.classOffset)
                    detections.add(Detection(cx, cy, w, h, maxClass, maxScore))
            }
        }
        return detections
    }

    // ── Shared utilities ─────────────────────────────────────────────────────

    private fun formatResult(detections: List<Detection>, modelType: ModelType): String {
        val nms = nonMaxSuppression(detections)
        val result = nms.sortedBy { it.cx }
            .joinToString("") { (it.classId - modelType.classOffset).toString() }
        Log.d(TAG, "[${modelType.displayName}] Детекций: ${nms.size}, результат: \"$result\"")
        return result
    }

    private fun sigmoid(x: Float) = 1f / (1f + Math.exp(-x.toDouble()).toFloat())

    private fun letterbox(bitmap: Bitmap, targetSize: Int): Pair<Bitmap, Unit> {
        val scale = minOf(targetSize.toFloat() / bitmap.width, targetSize.toFloat() / bitmap.height)
        val newW = (bitmap.width * scale).toInt()
        val newH = (bitmap.height * scale).toInt()
        val padX = (targetSize - newW) / 2
        val padY = (targetSize - newH) / 2
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawColor(Color.rgb(114, 114, 114))
            drawBitmap(scaled, padX.toFloat(), padY.toFloat(), null)
        }
        return Pair(result, Unit)
    }

    // HWC float32 buffer for TFLite
    private fun bitmapToHwcBuffer(bitmap: Bitmap, size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(size * size * 3 * 4)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8)  and 0xFF) / 255f)
            buffer.putFloat((pixel          and 0xFF) / 255f)
        }
        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val fd = context.assets.openFd(fileName)
        Log.d(TAG, "Загрузка: $fileName (${fd.declaredLength / 1024} KB)")
        return FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
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
        val inter = maxOf(0f, minOf(aX2, bX2) - maxOf(aX1, bX1)) *
                    maxOf(0f, minOf(aY2, bY2) - maxOf(aY1, bY1))
        val union = (aX2 - aX1) * (aY2 - aY1) + (bX2 - bX1) * (bY2 - bY1) - inter
        return if (union > 0f) inter / union else 0f
    }

    private fun incompatibleError(modelType: ModelType, cause: Exception? = null): Nothing =
        throw UnsupportedOperationException(
            "Модель «${modelType.displayName}» недоступна. " +
            if (cause != null) "Проверьте наличие файла ${modelType.fileName} в assets."
            else "Выберите другую модель в настройках.",
            cause,
        )

    private data class Detection(
        val cx: Float, val cy: Float, val w: Float, val h: Float,
        val classId: Int, val confidence: Float,
    )
}
