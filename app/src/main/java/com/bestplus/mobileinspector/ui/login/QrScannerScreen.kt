package com.bestplus.mobileinspector.ui.login

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Структура QR-кода, генерируемого со стороны 1С.
 * Пример: {"address":"192.168.1.100:8080","database":"WorkBase","ssl":false,"uuid":"550e8400-..."}
 */
@Serializable
data class QrConnectionData(
    val address: String,
    val database: String,
    val ssl: Boolean = false,
    val uuid: String,
)

private val qrJson = Json { ignoreUnknownKeys = true }
private const val TAG = "QrScanner"

/**
 * Полноэкранный сканер QR-кода для настройки подключения к 1С.
 * После успешного распознавания вызывает [onScanned] и закрывается.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onScanned: (QrConnectionData) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Флаг: уже обработали один QR, не обрабатываем повторно.
    // AtomicBoolean нужен потому что проверка идёт из фонового потока executor.
    val scanned = remember { AtomicBoolean(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Всегда ссылаемся на актуальную лямбду, даже если factory создан раньше.
    val currentOnScanned by rememberUpdatedState(onScanned)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сканировать QR-код") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!hasCameraPermission) {
                // Нет разрешения
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Необходим доступ к камере для сканирования QR-кода",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Предоставить доступ")
                    }
                }
            } else {
                // Превью камеры
                val executor = remember { Executors.newSingleThreadExecutor() }
                val barcodeScanner = remember { BarcodeScanning.getClient() }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                if (scanned.get()) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees,
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val qrBarcode = barcodes.firstOrNull {
                                                it.format == Barcode.FORMAT_QR_CODE &&
                                                    it.rawValue != null
                                            }
                                            qrBarcode?.rawValue?.let { raw ->
                                                Log.d(TAG, "QR detected, raw='$raw'")
                                                if (scanned.compareAndSet(false, true)) {
                                                    val parsed = parseQrPayload(raw)
                                                    if (parsed != null) {
                                                        Log.d(TAG, "Parsed OK: $parsed")
                                                        currentOnScanned(parsed)
                                                    } else {
                                                        Log.e(TAG, "Parse failed for raw='$raw'")
                                                        errorText = "Неверный формат QR-кода:\n$raw"
                                                        scanned.set(false)
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis,
                                )
                            } catch (_: Exception) { }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Прицел — тёмный оверлей с прозрачным окном
                QrAimOverlay(modifier = Modifier.fillMaxSize())

                // Подсказка
                Text(
                    text = "Наведите камеру на QR-код,\nсгенерированный в 1С",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                )

                // Ошибка формата
                errorText?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { errorText = null }) { Text("OK") }
                        },
                    ) { Text(msg) }
                }
            }
        }
    }
}

/**
 * Полупрозрачный оверлей с прицелом для QR-сканирования.
 */
@Composable
private fun QrAimOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val windowSize = size.minDimension * 0.65f
        val left = (size.width - windowSize) / 2f
        val top = (size.height - windowSize) / 2f

        // Тёмный фон
        drawRect(color = Color.Black.copy(alpha = 0.55f))

        // Прозрачное окно (вырез)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(windowSize, windowSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            blendMode = BlendMode.Clear,
        )

        // Угловые маркеры
        val markerLen = 36.dp.toPx()
        val stroke = 4.dp.toPx()
        val r = 16.dp.toPx()
        val markerColor = Color.White

        // Верхний левый
        drawLine(markerColor, Offset(left + r, top + stroke / 2), Offset(left + r + markerLen, top + stroke / 2), stroke)
        drawLine(markerColor, Offset(left + stroke / 2, top + r), Offset(left + stroke / 2, top + r + markerLen), stroke)

        // Верхний правый
        val right = left + windowSize
        drawLine(markerColor, Offset(right - r - markerLen, top + stroke / 2), Offset(right - r, top + stroke / 2), stroke)
        drawLine(markerColor, Offset(right - stroke / 2, top + r), Offset(right - stroke / 2, top + r + markerLen), stroke)

        // Нижний левый
        val bottom = top + windowSize
        drawLine(markerColor, Offset(left + r, bottom - stroke / 2), Offset(left + r + markerLen, bottom - stroke / 2), stroke)
        drawLine(markerColor, Offset(left + stroke / 2, bottom - r - markerLen), Offset(left + stroke / 2, bottom - r), stroke)

        // Нижний правый
        drawLine(markerColor, Offset(right - r - markerLen, bottom - stroke / 2), Offset(right - r, bottom - stroke / 2), stroke)
        drawLine(markerColor, Offset(right - stroke / 2, bottom - r - markerLen), Offset(right - stroke / 2, bottom - r), stroke)
    }
}

/**
 * Парсинг QR-кода.
 * Поддерживает два формата:
 * 1. JSON: {"address":"...","database":"...","ssl":false,"uuid":"..."}
 * 2. Упрощённый URI: mobileinspector://connect?address=...&database=...&ssl=false&uuid=...
 */
private fun parseQrPayload(raw: String): QrConnectionData? {
    // Попытка 1: JSON
    if (raw.trimStart().startsWith("{")) {
        val result = runCatching { qrJson.decodeFromString<QrConnectionData>(raw) }
        result.exceptionOrNull()?.let { Log.e(TAG, "JSON parse error: $it") }
        return result.getOrNull()
    }
    // Попытка 2: URI-схема
    if (raw.startsWith("mobileinspector://connect")) {
        return runCatching {
            val uri = android.net.Uri.parse(raw)
            val address = uri.getQueryParameter("address") ?: return null
            val database = uri.getQueryParameter("database") ?: return null
            val ssl = uri.getQueryParameter("ssl")?.toBooleanStrictOrNull() ?: false
            val uuid = uri.getQueryParameter("uuid") ?: return null
            QrConnectionData(address = address, database = database, ssl = ssl, uuid = uuid)
        }.getOrNull()
    }
    Log.e(TAG, "Unknown QR format, raw='$raw'")
    return null
}
