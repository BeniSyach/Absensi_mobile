package com.dss.absensiKoas.ui.component

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Composable untuk capture foto absen menggunakan CameraX.
 * Default menggunakan kamera depan (selfie) untuk verifikasi wajah saat absen.
 *
 * @param onPhotoTaken callback dengan File foto hasil capture
 * @param onDismiss callback saat user membatalkan
 */
@Composable
fun CameraCaptureScreen(
    onPhotoTaken: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Preview Kamera
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                startCamera(
                    context = ctx,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    lensFacing = lensFacing,
                    onImageCaptureReady = { imageCapture = it }
                )
                previewView
            },
            update = { previewView ->
                startCamera(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    lensFacing = lensFacing,
                    onImageCaptureReady = { imageCapture = it }
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay frame wajah (panduan posisi)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .background(Color.Transparent)
        ) {
            // Frame oval sederhana sebagai panduan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }

        Text(
            text = "Posisikan wajah Anda di tengah",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Tombol Close
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
        }

        // Tombol switch kamera
        IconButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Icon(Icons.Default.Cameraswitch, contentDescription = "Ganti Kamera", tint = Color.White)
        }

        // Tombol Capture
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = {
                    if (!isCapturing) {
                        isCapturing = true
                        capturePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            executor = cameraExecutor,
                            onSuccess = { file ->
                                isCapturing = false
                                onPhotoTaken(file)
                            },
                            onError = {
                                isCapturing = false
                            }
                        )
                    }
                },
                modifier = Modifier.size(72.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = Color.White
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Ambil Foto",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    lensFacing: Int,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            onImageCaptureReady(imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: ExecutorService,
    onSuccess: (File) -> Unit,
    onError: (Exception) -> Unit
) {
    val imageCaptureInstance = imageCapture ?: return

    val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        .format(System.currentTimeMillis())
    val photoFile = File(context.cacheDir, "absen_$fileName.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCaptureInstance.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onSuccess(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}