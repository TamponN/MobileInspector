package com.bestplus.mobileinspector.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Общие утилиты обработки Bitmap: расчёт размера файла, EXIF-поворот,
 * масштабирование по длинной стороне. Используются OCR-хелпером и энкодером
 * фото для отправки в 1С.
 */
object ImageUtils {

    /** Декодирует габариты изображения без загрузки всего Bitmap в память. */
    fun decodeBounds(file: File): IntArray {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return intArrayOf(opts.outWidth, opts.outHeight)
    }

    /**
     * Степень двойки для inSampleSize, чтобы первая загрузка была сразу уменьшенной.
     * Цель — итоговая длинная сторона ~2×[targetMaxSide].
     */
    fun sampleSizeFor(width: Int, height: Int, targetMaxSide: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        val halfTarget = (targetMaxSide * 2).coerceAtLeast(1)
        while (longest / (sample * 2) >= halfTarget) {
            sample *= 2
        }
        return sample
    }

    /** Поворот Bitmap по EXIF-ориентации файла. */
    fun applyExifRotation(bitmap: Bitmap, imageFile: File): Bitmap {
        val degrees = when (ExifInterface(imageFile.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply { postRotate(degrees) }, true,
        )
    }

    /**
     * Масштабирует Bitmap так, чтобы длинная сторона стала [maxSide]px
     * (если исходник больше). Пропорции сохраняются.
     */
    fun scaleToMaxSide(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
