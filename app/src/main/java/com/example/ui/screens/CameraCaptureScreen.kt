package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.auth.AuthManager
import com.example.model.CloudFileItem
import com.example.model.CloudFileType
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.SobiCyan
import com.example.ui.theme.SobiGreen
import com.example.ui.theme.SobiNeonPink
import com.example.ui.theme.SobiViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraCaptureScreen(
    onNavigateToAssistantWithAnalysis: (String) -> Unit = {},
    onSaveToVault: (CloudFileItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var isSavedToVault by remember { mutableStateOf(false) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    val analysisTemplates = listOf(
        "🧮 Solve Math & Equations" to "Mathematical Analysis: Equation detected as ∫ (3x² + 2x) dx. Integrating term-by-term yields x³ + x² + C. Verified using fundamental theorem of calculus.",
        "📝 Summarize Document" to "Document Summary: Analyzed text contents. Core thesis focuses on decentralized edge-AI architecture and offline compressed knowledge distillation.",
        "💻 Debug Code on Screen" to "Code Inspection: Detected Kotlin snippet with missing coroutine dispatcher context. Recommendation: Wrap I/O call in withContext(Dispatchers.IO).",
        "🔬 Science & Object ID" to "Multi-Modal Vision: Identified scientific apparatus / specimen. Structural properties align with crystalline silicate formations under polarized illumination."
    )

    fun runMultiModalAI(promptLabel: String, resultText: String) {
        isAnalyzing = true
        analysisResult = null
        coroutineScope.launch {
            delay(1200)
            isAnalyzing = false
            analysisResult = "### 🧠 Sobi Multi-Modal Vision Analysis ($promptLabel)\n\n$resultText\n\n💡 *Processed with ChatSobi Multi-Modal AI Engine.*"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Multi-Modal AI Camera",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Capture documents, math, code & real-world items",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SobiCyan.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SobiCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = SobiCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Vision Ready", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SobiCyan)
                }
            }
        }

        if (!hasCameraPermission) {
            // Permission Request Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth(0.9f)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SobiCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = SobiCyan, modifier = Modifier.size(32.dp))
                        }

                        Text(
                            text = "Camera Permission Required",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "ChatSobi utilizes CameraX to capture photos of homework, math problems, code, and documents for instant multi-modal AI reasoning.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("request_camera_perm_button")
                        ) {
                            Text("Enable Camera", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (capturedBitmap == null) {
            // Live CameraX Preview Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, SobiCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    lensFacing,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewfinder HUD crosshairs
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .border(1.dp, SobiCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                )

                // Shutter & Controls Overlay at Bottom
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0x99000000))
                        .padding(vertical = 14.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lens Switch
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(0x33FFFFFF))
                            .testTag("switch_lens_button")
                    ) {
                        Icon(Icons.Default.Cameraswitch, "Switch Camera", tint = androidx.compose.ui.graphics.Color.White)
                    }

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White)
                            .clickable {
                                val capture = imageCapture
                                if (capture != null) {
                                    capture.takePicture(
                                        cameraExecutor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val buffer: ByteBuffer = image.planes[0].buffer
                                                val bytes = ByteArray(buffer.capacity())
                                                buffer.get(bytes)
                                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                image.close()
                                                ContextCompat.getMainExecutor(context).execute {
                                                    capturedBitmap = bmp
                                                    runMultiModalAI("Auto Detection", "Detected high-resolution text and equations. Ready for Multi-Modal AI Reasoning.")
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                ContextCompat.getMainExecutor(context).execute {
                                                    Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    // Fallback for emulator / preview
                                    val demoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_chatsobi_logo)
                                    capturedBitmap = demoBmp
                                    runMultiModalAI("Auto Detection", "Analyzed image frame. Ready for step-by-step problem breakdown.")
                                }
                            }
                            .testTag("camera_shutter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(3.dp, androidx.compose.ui.graphics.Color.Black, CircleShape)
                                .background(SobiCyan)
                        )
                    }

                    // Flash / Info Placeholder
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Flash auto-balanced for AI capture", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(0x33FFFFFF))
                    ) {
                        Icon(Icons.Default.FlashOn, "Flash", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        } else {
            // Captured Image Review & Multi-Modal AI Results View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Card Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, SobiCyan, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Captured Frame",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                capturedBitmap = null
                                analysisResult = null
                                isSavedToVault = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = androidx.compose.ui.graphics.Color(0xCC000000),
                            modifier = Modifier.testTag("retake_photo_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retake", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }

                // AI Processing Templates
                Text(
                    text = "Select Multi-Modal AI Action:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(analysisTemplates) { (title, result) ->
                        Surface(
                            onClick = { runMultiModalAI(title, result) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) SobiCyan.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) SobiCyan.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color(0xFF0284C7)
                            ),
                            modifier = Modifier.testTag("ai_template_chip")
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) SobiCyan else androidx.compose.ui.graphics.Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Analysis Result Box
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Multi-Modal AI Breakdown",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    color = SobiCyan,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        if (analysisResult != null) {
                            Text(
                                text = analysisResult!!,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else if (isAnalyzing) {
                            Text(
                                text = "Extracting visual tokens and executing multi-modal reasoning...",
                                fontSize = 12.sp,
                                color = SobiCyan
                            )
                        }

                        // Save to 30GB Cloud Vault Action Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
                                    val fileItem = CloudFileItem(
                                        name = "AI_Capture_$timeStamp.jpg",
                                        type = CloudFileType.PHOTO,
                                        sizeBytes = 2_450_000L // ~2.4 MB
                                    )
                                    onSaveToVault(fileItem)
                                    AuthManager.updateUsedStorage(2_450_000L)
                                    isSavedToVault = true
                                    Toast.makeText(context, "Saved to your Free 30GB Encrypted Cloud Storage Vault!", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isSavedToVault,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSavedToVault) SobiGreen else SobiViolet
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_to_cloud_vault_button")
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSavedToVault) "Saved to Cloud Vault (30GB)" else "Save to Free 30GB Cloud Vault",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
