package com.example.bikerental.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QRScannerDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onQRCodeScanned: (String) -> Unit,
    title: String = "Scan QR Code"
) {
    if (isVisible) {
        val context = LocalContext.current
        var hasCameraPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasCameraPermission = isGranted
        }

        LaunchedEffect(Unit) {
            if (!hasCameraPermission) {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                color = Color.Black
            ) {
                if (hasCameraPermission) {
                    QRScannerContent(
                        title = title,
                        onDismiss = onDismiss,
                        onQRCodeScanned = onQRCodeScanned
                    )
                } else {
                    PermissionDeniedContent(onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun QRScannerContent(
    title: String,
    onDismiss: () -> Unit,
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    var isProcessing by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                
                // Use async initialization instead of blocking get()
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        bindCameraUseCases(
                            cameraProvider = provider,
                            previewView = previewView,
                            lifecycleOwner = lifecycleOwner,
                            cameraExecutor = cameraExecutor,
                            onQRCodeScanned = { qrCode ->
                                if (!isProcessing) {
                                    isProcessing = true
                                    onQRCodeScanned(qrCode)
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("QRScanner", "Camera initialization failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center scanning area indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Scanning frame - Smaller
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    // Corner indicators
                    val cornerSize = 16.dp
                    val cornerWidth = 2.dp
                    
                    // Top-left corner
                    Box(
                        modifier = Modifier
                            .size(cornerSize)
                            .align(Alignment.TopStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cornerSize)
                                .height(cornerWidth)
                                .background(Color.White)
                        )
                        Box(
                            modifier = Modifier
                                .width(cornerWidth)
                                .height(cornerSize)
                                .background(Color.White)
                        )
                    }
                    
                    // Top-right corner
                    Box(
                        modifier = Modifier
                            .size(cornerSize)
                            .align(Alignment.TopEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cornerSize)
                                .height(cornerWidth)
                                .background(Color.White)
                                .align(Alignment.TopEnd)
                        )
                        Box(
                            modifier = Modifier
                                .width(cornerWidth)
                                .height(cornerSize)
                                .background(Color.White)
                                .align(Alignment.TopEnd)
                        )
                    }
                    
                    // Bottom-left corner
                    Box(
                        modifier = Modifier
                            .size(cornerSize)
                            .align(Alignment.BottomStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cornerSize)
                                .height(cornerWidth)
                                .background(Color.White)
                                .align(Alignment.BottomStart)
                        )
                        Box(
                            modifier = Modifier
                                .width(cornerWidth)
                                .height(cornerSize)
                                .background(Color.White)
                                .align(Alignment.BottomStart)
                        )
                    }
                    
                    // Bottom-right corner
                    Box(
                        modifier = Modifier
                            .size(cornerSize)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(cornerSize)
                                .height(cornerWidth)
                                .background(Color.White)
                                .align(Alignment.BottomEnd)
                        )
                        Box(
                            modifier = Modifier
                                .width(cornerWidth)
                                .height(cornerSize)
                                .background(Color.White)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            }

            // Bottom instruction text
            Text(
                text = "Position the QR code within the frame to scan",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Proper cleanup when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            try {
                // Unbind camera first, then shutdown executor
                cameraProvider?.unbindAll()
                cameraExecutor.shutdown()
                Log.d("QRScanner", "Camera resources cleaned up")
            } catch (e: Exception) {
                Log.e("QRScanner", "Error during cleanup", e)
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera Permission Required",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "To scan QR codes, please grant camera permission in app settings.",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Close", fontSize = 14.sp)
        }
    }
}

private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onQRCodeScanned: (String) -> Unit
) {
    val preview = Preview.Builder().build()
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    
    preview.setSurfaceProvider(previewView.surfaceProvider)

    val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
        processImageProxy(imageProxy, onQRCodeScanned)
    }

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )
    } catch (e: Exception) {
        Log.e("QRScanner", "Camera binding failed", e)
    }
}

@androidx.camera.core.ExperimentalGetImage
private fun processImageProxy(
    imageProxy: ImageProxy,
    onQRCodeScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val scanner = BarcodeScanning.getClient()
        
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    when (barcode.valueType) {
                        Barcode.TYPE_TEXT -> {
                            barcode.rawValue?.let { value ->
                                onQRCodeScanned(value)
                            }
                        }
                        Barcode.TYPE_URL -> {
                            barcode.url?.url?.let { url ->
                                onQRCodeScanned(url)
                            }
                        }
                        else -> {
                            barcode.rawValue?.let { value ->
                                onQRCodeScanned(value)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("QRScanner", "Barcode scanning failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
} 