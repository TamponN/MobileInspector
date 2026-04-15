package com.bestplus.mobileinspector.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Экран камеры для захвата фотографии показания счётчика.
 * После захвата запускает OCR через ML Kit и возвращает распознанное значение.
 * Повторяет логику C# InputTestimony.cs — PhotoCapture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onPhotoCapturedAndProcessed: (String) -> Unit = {},
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(uiState.photoSaved) {
        if (uiState.photoSaved) {
            onPhotoCapturedAndProcessed(uiState.recognizedText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фото показания") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
        ) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener(
                        {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            } catch (_: Exception) { }
                        },
                        ContextCompat.getMainExecutor(ctx),
                    )
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )

            // OCR result overlay
            if (uiState.recognizedText.isNotBlank() && !uiState.isProcessing) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Распознано:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Text(
                            text = uiState.recognizedText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                    }
                }
            }

            // Error overlay
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            // Processing indicator
            if (uiState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Распознаётся…", color = Color.White)
                    }
                }
            }

            // Capture button
            FloatingActionButton(
                onClick = {
                    if (!uiState.isProcessing) {
                        capturePhoto(context, imageCapture) { file ->
                            viewModel.onPhotoCaptured(file)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Сделать фото",
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (File) -> Unit,
) {
    val photoFile = File(
        context.externalCacheDir,
        "testimony_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg",
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onCaptured(photoFile)
            }
            override fun onError(exc: ImageCaptureException) { }
        },
    )
}
