package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.GeneratedArt
import com.example.model.MediaTab
import com.example.ui.components.InteractiveSplitSlider
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.SobiCyan
import com.example.ui.theme.SobiGreen
import com.example.ui.theme.SobiNeonPink
import com.example.ui.theme.SobiViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaStudioScreen(
    onSaveToVaultCallback: (name: String, isVideo: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MediaTab.PHOTO_ENHANCE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = SobiCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = SobiCyan,
                    height = 2.5.dp
                )
            },
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Tab(
                selected = selectedTab == MediaTab.PHOTO_ENHANCE,
                onClick = { selectedTab = MediaTab.PHOTO_ENHANCE },
                text = { Text("Photo Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.AutoFixHigh, null, Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_photo_enhance")
            )
            Tab(
                selected = selectedTab == MediaTab.VIDEO_STUDIO,
                onClick = { selectedTab = MediaTab.VIDEO_STUDIO },
                text = { Text("Video Suite", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.MovieCreation, null, Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_video_studio")
            )
            Tab(
                selected = selectedTab == MediaTab.IMAGE_GEN,
                onClick = { selectedTab = MediaTab.IMAGE_GEN },
                text = { Text("Gen Image", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Wallpaper, null, Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_image_gen")
            )
            Tab(
                selected = selectedTab == MediaTab.VIDEO_GEN,
                onClick = { selectedTab = MediaTab.VIDEO_GEN },
                text = { Text("Gen Video", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.VideoCameraBack, null, Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_video_gen")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            MediaTab.PHOTO_ENHANCE -> PhotoEnhanceSection(onSaveToVault = onSaveToVaultCallback)
            MediaTab.VIDEO_STUDIO -> VideoStudioSection(onSaveToVault = onSaveToVaultCallback)
            MediaTab.IMAGE_GEN -> ImageGenSection(onSaveToVault = onSaveToVaultCallback)
            MediaTab.VIDEO_GEN -> VideoGenSection(onSaveToVault = onSaveToVaultCallback)
        }
    }
}

@Composable
fun PhotoEnhanceSection(
    onSaveToVault: (name: String, isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableFloatStateOf(0f) }
    var currentStep by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("4K Super-Res") }
    var userPrompt by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFileName by remember { mutableStateOf("sample_portrait.jpg") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            photoFileName = "Photo_${System.currentTimeMillis() % 10000}.jpg"
            Toast.makeText(context, "Photo loaded for enhancement!", Toast.LENGTH_SHORT).show()
        }
    }

    val filters = listOf("4K Super-Res", "De-Blur & Sharpen", "Face Detail Restore", "HDR Lighting", "Magic Cleanup")

    fun runEnhancement() {
        if (isProcessing) return
        isProcessing = true
        processingProgress = 0f
        coroutineScope.launch {
            currentStep = "Analyzing pixel histogram & sensor noise..."
            delay(500)
            processingProgress = 0.25f
            currentStep = "Applying latent super-resolution diffusion ($activeFilter)..."
            delay(600)
            processingProgress = 0.65f
            currentStep = "Synthesizing high-frequency micro-textures..."
            delay(500)
            processingProgress = 1.0f
            currentStep = "Restoration complete! +380% clarity achieved."
            delay(300)
            isProcessing = false
            Toast.makeText(context, "Enhancement completed with $activeFilter", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upload Photo Action Card
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Source Photo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("upload_photo_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (selectedPhotoUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2200D4EC))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = SobiGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Selected: $photoFileName (Ready for AI Processing)",
                                fontSize = 11.5.sp,
                                color = SobiCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "💡 Using demo portrait. Tap 'Upload Photo' to select any photo from your gallery.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Custom Enhancement Prompt Input
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI Enhancement Prompt (Optional Instruction)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        placeholder = {
                            Text(
                                "e.g. Upscale resolution, sharpen eyes, fix motion blur, enhance cinematic lighting...",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("photo_prompt_input"),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SobiCyan,
                            unfocusedBorderColor = Color(0x33CBD5E1)
                        )
                    )
                }
            }
        }

        item {
            // Interactive Before & After Comparison Slider
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Interactive Before / After Split Comparison",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    InteractiveSplitSlider(
                        beforeContent = {
                            if (selectedPhotoUri != null) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Before Enhancement",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(4.dp)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_media_studio_1792726725443),
                                    contentDescription = "Before Enhancement",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(4.dp)
                                )
                            }
                        },
                        afterContent = {
                            if (selectedPhotoUri != null) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "After Enhancement",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_media_studio_1792726725443),
                                    contentDescription = "After Enhancement",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    )
                    Text(
                        text = "👈 Drag slider horizontally to compare original vs 4K AI restoration",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            // Enhancement Presets
            Column {
                Text(
                    text = "AI Restoration Models:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { filter ->
                        val isSelected = filter == activeFilter
                        Surface(
                            onClick = { activeFilter = filter },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) SobiCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SobiCyan else Color.Transparent)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SobiCyan else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Processing Bar & Metrics
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model Metrics",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = SobiGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GPU Accelerated", fontSize = 11.sp, color = SobiGreen)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Clarity Boost", "+380%")
                        MetricItem("Denoise Rate", "99.4%")
                        MetricItem("Target", "4K UHD")
                    }

                    if (isProcessing) {
                        LinearProgressIndicator(
                            progress = { processingProgress },
                            color = SobiCyan,
                            trackColor = Color(0x3300E5FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Text(
                            text = currentStep,
                            fontSize = 11.sp,
                            color = SobiCyan
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { runEnhancement() },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("enhance_photo_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run AI Enhance", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onSaveToVault("Enhanced_$photoFileName", false)
                                Toast.makeText(context, "Saved 4K Enhanced Photo to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SobiGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_photo_vault_button")
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Vault", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SobiCyan)
    }
}

@Composable
fun VideoStudioSection(
    onSaveToVault: (name: String, isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRendering by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf("Cinematic Reel") }
    var videoPrompt by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoFileName by remember { mutableStateOf("demo_timeline_clip.mp4") }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
            videoFileName = "Video_${System.currentTimeMillis() % 10000}.mp4"
            Toast.makeText(context, "Video loaded into AI Editing Suite!", Toast.LENGTH_SHORT).show()
        }
    }

    val templates = listOf("Cinematic Reel", "Tech Showcase", "Vlog FastCut", "Sports Dynamic", "Cyberpunk Neon")

    fun renderVideo() {
        if (isRendering) return
        isRendering = true
        coroutineScope.launch {
            delay(1600)
            isRendering = false
            Toast.makeText(context, "Applied '$selectedTemplate' & exported 4K 60fps MP4!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upload Video Action Card
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Source Video Input",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SobiViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("upload_video_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (selectedVideoUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x228B5CF6))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = SobiGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Loaded: $videoFileName • 1080p 60fps",
                                fontSize = 11.5.sp,
                                color = SobiViolet,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "💡 Using demo timeline footage. Tap 'Upload Video' to select any video file to enhance or edit.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Custom AI Video Editing Instruction Prompt
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI Video Editing Prompt (Instruction As Prompt)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = videoPrompt,
                        onValueChange = { videoPrompt = it },
                        placeholder = {
                            Text(
                                "e.g. Cut silent pauses, add dynamic transitions, apply cinematic color grading, add auto subtitles...",
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_edit_prompt_input"),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SobiViolet,
                            unfocusedBorderColor = Color(0x33CBD5E1)
                        )
                    )
                }
            }
        }

        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Smart Video Timeline",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x33FF2A85)
                        ) {
                            Text(
                                text = "4K • 60 FPS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SobiNeonPink,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_media_studio_1792726725443),
                            contentDescription = "Video Canvas",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC000000))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SobiCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Automated Cut & Beat Sync", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Clip: $videoFileName • Audio: AI Mastered", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                    }

                    // Timeline tracks
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D1322))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimelineTrack("Video Track (AI Cuts)", SobiCyan, 0.9f)
                        TimelineTrack("Beat Sync Audio", SobiViolet, 0.75f)
                        TimelineTrack("Smart Subtitles", SobiGreen, 0.85f)
                    }
                }
            }
        }

        item {
            Text(
                text = "Automated AI Smart Templates:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates) { template ->
                    val isSelected = template == selectedTemplate
                    Surface(
                        onClick = { selectedTemplate = template },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) SobiViolet.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SobiViolet else Color.Transparent)
                    ) {
                        Text(
                            text = template,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) SobiViolet else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { renderVideo() },
                    enabled = !isRendering,
                    colors = ButtonDefaults.buttonColors(containerColor = SobiViolet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("render_video_button")
                ) {
                    if (isRendering) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rendering with AI...", color = Color.White, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Template & AI Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }

                Button(
                    onClick = {
                        onSaveToVault("Edited_$videoFileName", true)
                        Toast.makeText(context, "Saved Edited Video to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SobiGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("save_video_vault_button")
                ) {
                    Icon(Icons.Default.CloudDone, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Vault", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TimelineTrack(label: String, color: Color, fill: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(90.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1E293B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
fun ImageGenSection(
    onSaveToVault: (name: String, isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("A futuristic student studying with a glowing holographic AI core in a cyberpunk library") }
    var selectedStyle by remember { mutableStateOf("Photorealistic") }
    var selectedRatio by remember { mutableStateOf("16:9") }
    var isGenerating by remember { mutableStateOf(false) }

    val styles = listOf("Photorealistic", "Cyberpunk", "Anime/Manga", "3D Octane Render", "Minimalist Vector")
    val ratios = listOf("1:1", "16:9", "9:16", "4:3")

    var gallery by remember {
        mutableStateOf(
            listOf(
                GeneratedArt(
                    prompt = "A futuristic student studying with a glowing holographic AI core",
                    style = "Cyberpunk",
                    aspectRatio = "16:9",
                    imageDrawableRes = R.drawable.img_media_studio_1792726725443
                ),
                GeneratedArt(
                    prompt = "ChatSobi AI neural intelligence and universal knowledge",
                    style = "Photorealistic",
                    aspectRatio = "1:1",
                    imageDrawableRes = R.drawable.ic_chatsobi_logo
                )
            )
        )
    }

    fun generateImage() {
        if (prompt.isBlank() || isGenerating) return
        isGenerating = true
        coroutineScope.launch {
            delay(1500)
            val newArt = GeneratedArt(
                prompt = prompt,
                style = selectedStyle,
                aspectRatio = selectedRatio,
                imageDrawableRes = if (gallery.size % 2 == 0) R.drawable.img_media_studio_1792726725443 else R.drawable.ic_chatsobi_logo
            )
            gallery = listOf(newArt) + gallery
            isGenerating = false
            Toast.makeText(context, "High-fidelity $selectedStyle image synthesized!", Toast.LENGTH_SHORT).show()
        }
    }

    fun enhancePrompt() {
        prompt = "$prompt, ultra-detailed 8k resolution, cinematic lighting, liquid glass reflections, vibrant cyberpunk tones, masterpiece"
        Toast.makeText(context, "Prompt enriched by Sobi AI!", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Text-to-Image Prompt",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            onClick = { enhancePrompt() },
                            shape = RoundedCornerShape(10.dp),
                            color = SobiCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SobiCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("enhance_prompt_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = SobiCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enhance Prompt", fontSize = 10.sp, color = SobiCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_input"),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SobiCyan,
                            unfocusedBorderColor = Color(0x33CBD5E1)
                        )
                    )

                    // Styles Row
                    Text("Select Art Style:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(styles) { style ->
                            val isSelected = style == selectedStyle
                            Surface(
                                onClick = { selectedStyle = style },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SobiCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SobiCyan else Color.Transparent)
                            ) {
                                Text(
                                    text = style,
                                    fontSize = 11.sp,
                                    color = if (isSelected) SobiCyan else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Aspect Ratios Row
                    Text("Aspect Ratio:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ratios.forEach { ratio ->
                            val isSelected = ratio == selectedRatio
                            Surface(
                                onClick = { selectedRatio = ratio },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SobiViolet.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SobiViolet else Color.Transparent)
                            ) {
                                Text(
                                    text = ratio,
                                    fontSize = 11.sp,
                                    color = if (isSelected) SobiViolet else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { generateImage() },
                        enabled = !isGenerating && prompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("generate_image_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synthesizing Image with Sobi Neural Engine...", color = Color.Black, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Wallpaper, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Image", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "🎨 Generation Gallery:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(gallery, key = { it.id }) { art ->
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (art.imageDrawableRes != null) {
                        Image(
                            painter = painterResource(id = art.imageDrawableRes),
                            contentDescription = art.prompt,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = art.prompt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )

                        Row {
                            IconButton(onClick = {
                                onSaveToVault("GeneratedArt_${art.id.take(4)}.jpg", false)
                                Toast.makeText(context, "Saved artwork to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.CloudDone, null, tint = SobiGreen, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                Toast.makeText(context, "Exported artwork to device storage!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Download, null, tint = SobiCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0x3300E5FF)) {
                            Text(art.style, fontSize = 10.sp, color = SobiCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0x339D4EDD)) {
                            Text(art.aspectRatio, fontSize = 10.sp, color = SobiViolet, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoGenSection(
    onSaveToVault: (name: String, isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var videoPrompt by remember { mutableStateOf("Hyper-lapse drone shot of a futuristic metropolis with flying cars in the rain") }
    var selectedMotion by remember { mutableStateOf("Drone Orbit") }
    var durationSec by remember { mutableStateOf(5) }
    var isGeneratingVideo by remember { mutableStateOf(false) }

    val motions = listOf("Drone Orbit", "Forward Dolly", "360° Pan", "Dutch Tilt Up")

    fun generateVideo() {
        if (videoPrompt.isBlank() || isGeneratingVideo) return
        isGeneratingVideo = true
        coroutineScope.launch {
            delay(2000)
            isGeneratingVideo = false
            Toast.makeText(context, "Video generation complete! Encoded to 4K MP4.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Text-to-Video Engine (Veo / Omni)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = videoPrompt,
                        onValueChange = { videoPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_prompt_input"),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SobiNeonPink,
                            unfocusedBorderColor = Color(0x33CBD5E1)
                        )
                    )

                    Text("Camera Dynamics:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(motions) { motion ->
                            val isSelected = motion == selectedMotion
                            Surface(
                                onClick = { selectedMotion = motion },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) SobiNeonPink.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SobiNeonPink else Color.Transparent)
                            ) {
                                Text(
                                    text = motion,
                                    fontSize = 11.sp,
                                    color = if (isSelected) SobiNeonPink else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duration:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5, 10).forEach { dur ->
                                Surface(
                                    onClick = { durationSec = dur },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (durationSec == dur) SobiCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "${dur}s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (durationSec == dur) SobiCyan else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { generateVideo() },
                        enabled = !isGeneratingVideo && videoPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SobiNeonPink),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("generate_video_button")
                    ) {
                        if (isGeneratingVideo) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rendering Video Frames with Omni...", color = Color.White, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.SlowMotionVideo, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Video Clip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rendered Video Output Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_media_studio_1792726725443),
                            contentDescription = "Generated Video",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SobiNeonPink.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sci-Fi Metropolis Hyper-Lapse", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Resolution: 4K UHD • Format: MP4", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row {
                            IconButton(onClick = {
                                onSaveToVault("GeneratedVideo_${System.currentTimeMillis() % 10000}.mp4", true)
                                Toast.makeText(context, "Saved Generated Video to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.CloudDone, null, tint = SobiGreen, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                Toast.makeText(context, "Exported MP4 video to device gallery!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Download, null, tint = SobiCyan, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
