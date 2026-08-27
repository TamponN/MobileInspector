package com.bestplus.mobileinspector.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кодирует фото показания для отправки в 1С (поле Image).
 *
 * Пайплайн: декодирование → EXIF-поворот → масштабирование по длинной стороне
 * до [MAX_SIDE]px → JPEG [QUALITY] → Base64 (без data-URI префикса).
 *
 * 1С применяет `Base64Значение(Картинка)` (см. ПрисоединитьМассивФайлов),
 * поэтому префикс `data:image/jpeg;base64,` НЕ добавляется.
 *
 * Результат: null/пустой/несуществующий файл → "" (фото не приложится).
 */
@Singleton
class ImageEncoder @Inject constructor() {

    suspend fun encodeForUpload(path: String?): String = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext ""
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return@withContext ""

        runCatching { encodeInternal(file) }.getOrDefault("")
    }

    private fun encodeInternal(file: File): String {
        val (width, height) = ImageUtils.decodeBounds(file)
        if (width <= 0 || height <= 0) return ""

        // 1. Декодируем сразу уменьшенным (экономия памяти на больших кадрах камеры)
        val sample = ImageUtils.sampleSizeFor(width, height, MAX_SIDE)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return ""

        // 2. EXIF-поворот + финальный ресайз до целевой длинной стороны
        val rotated = ImageUtils.applyExifRotation(decoded, file)
        val scaled = ImageUtils.scaleToMaxSide(rotated, MAX_SIDE)

        // 3. Сжатие в JPEG
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)

        // Освобождаем промежуточные битмапы, если были созданы копии
        if (rotated !== decoded) rotated.recycle()
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()

        // 4. Base64 без переноса строк (1С ожидает сплошную строку)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private companion object {
        const val MAX_SIDE = 1920
        const val QUALITY = 90
    }
}
