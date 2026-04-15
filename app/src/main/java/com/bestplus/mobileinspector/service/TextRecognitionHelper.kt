package com.bestplus.mobileinspector.service

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Обёртка над ML Kit Text Recognition.
 * Читает цифровые показания счётчика с фотографии.
 * Повторяет логику C# InputTestimony.cs — распознавание цифр OCR.
 */
object TextRecognitionHelper {

    /**
     * Распознаёт текст на изображении и возвращает наиболее вероятное числовое показание.
     * Фильтрует блоки, содержащие только цифры и точки/запятые (формат показаний счётчика).
     */
    suspend fun recognizeTestimony(context: Context, imageFile: File): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromFilePath(context, android.net.Uri.fromFile(imageFile))

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Extract number-like segments (meter readings)
                    val numberPattern = Regex("^[0-9][0-9.,\\s]*$")
                    val candidates = visionText.textBlocks
                        .flatMap { it.lines }
                        .map { it.text.trim().replace(",", ".") }
                        .filter { it.matches(numberPattern) }
                        .sortedByDescending { it.length }

                    cont.resume(candidates.firstOrNull() ?: "")
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
}
