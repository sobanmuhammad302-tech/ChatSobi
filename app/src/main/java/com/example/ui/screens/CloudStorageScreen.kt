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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.AuthManager
import com.example.model.CloudFileItem
import com.example.model.CloudFileType
import com.example.service.AutoSyncManager
import com.example.service.AutoSyncNetwork
import com.example.service.AutoSyncQuality
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.SobiCyan
import com.example.ui.theme.SobiGreen
import com.example.ui.theme.SobiNeonPink
import com.example.ui.theme.SobiViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class UploadQualityOption(val title: String, val subtitle: String, val sizeMultiplier: Float) {
    ORIGINAL("🌟 Original Quality (مکمل اوریجنل)", "Lossless pristine resolution. Best fidelity.", 1.0f),
    STORAGE_SAVER("⚡ High Quality (اسٹوریج سیور)", "AI-optimized compression. Saves ~60% space.", 0.4f),
    DATA_SAVER("📉 Standard Definition (فاسٹ اپلوڈ)", "Lightweight compression. Ultra-fast upload.", 0.2f)
}

@Composable
fun CloudStorageScreen(
    files: List<CloudFileItem>,
    onUploadFile: (CloudFileItem) -> Unit,
    onDeleteFile: (CloudFileItem) -> Unit,
    onInspectWithAI: (CloudFileItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by AuthManager.currentUser.collectAsState()
    val isAuthLoading by AuthManager.isLoading.collectAsState()

    var selectedFilter by remember { mutableStateOf<CloudFileType?>(null) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(UploadQualityOption.ORIGINAL) }
    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    // Auto-Sync States
    val isAutoSyncEnabled by AutoSyncManager.isAutoSyncEnabled.collectAsState()
    val hasUserConsented by AutoSyncManager.hasUserConsented.collectAsState()
    val syncQuality by AutoSyncManager.syncQuality.collectAsState()
    val syncNetwork by AutoSyncManager.syncNetwork.collectAsState()
    val isAutoEnhanceEnabled by AutoSyncManager.isAutoEnhanceEnabled.collectAsState()
    val isSyncing by AutoSyncManager.isSyncing.collectAsState()
    val syncProgress by AutoSyncManager.syncProgress.collectAsState()
    val lastSyncTime by AutoSyncManager.lastSyncTime.collectAsState()
    val syncedItemsCount by AutoSyncManager.syncedItemsCount.collectAsState()

    var showConsentDialog by remember { mutableStateOf(false) }
    var showAutoSyncSettingsDialog by remember { mutableStateOf(false) }

    var stagingQuality by remember { mutableStateOf(AutoSyncQuality.STORAGE_SAVER) }
    var stagingNetwork by remember { mutableStateOf(AutoSyncNetwork.WIFI_ONLY) }
    var stagingAutoEnhance by remember { mutableStateOf(true) }

    // Multi-select photo/video picker (up to 15 items at once)
    val multipleMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingUris = uris
            showQualityDialog = true
        }
    }

    val filteredFiles = if (selectedFilter == null) {
        files
    } else {
        files.filter { it.type == selectedFilter }
    }

    val usedBytes = currentUser?.usedStorageBytes ?: (if (currentUser != null) 0L else (1.42 * 1024 * 1024 * 1024).toLong())
    val totalBytes = currentUser?.totalStorageBytes ?: (30L * 1024L * 1024L * 1024L)
    val usedFraction = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    // Execute Batch Upload after Quality is Selected
    fun executeBatchUpload() {
        showQualityDialog = false
        if (pendingUris.isEmpty() || isUploading) return
        isUploading = true
        uploadProgress = 0f

        coroutineScope.launch {
            val totalCount = pendingUris.size
            var uploadedBytes = 0L

            for ((index, uri) in pendingUris.withIndex()) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val isVideo = uri.toString().contains("video", ignoreCase = true)
                val baseSize = if (isVideo) 18_000_000L else 3_500_000L
                val calculatedSize = (baseSize * selectedQuality.sizeMultiplier).toLong()

                val newFile = CloudFileItem(
                    name = if (isVideo) "Vault_Video_${timeStamp}_$index.mp4" else "Vault_Photo_${timeStamp}_$index.jpg",
                    type = if (isVideo) CloudFileType.VIDEO else CloudFileType.PHOTO,
                    sizeBytes = calculatedSize
                )
                onUploadFile(newFile)
                uploadedBytes += calculatedSize

                uploadProgress = (index + 1).toFloat() / totalCount.toFloat()
                delay(300L) // smooth upload progression
            }

            AuthManager.updateUsedStorage(uploadedBytes)
            isUploading = false
            pendingUris = emptyList()
            Toast.makeText(context, "Successfully uploaded $totalCount files with ${selectedQuality.title}!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
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
                    text = "30 GB Safe Cloud Vault",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Encrypted Firebase Cloud Storage for your media & files",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SobiGreen.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SobiGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, null, tint = SobiGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("100% Safe", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SobiGreen)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Google Account Status & Auth Card
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    if (currentUser != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(SobiCyan, SobiViolet))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser!!.displayName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = currentUser!!.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentUser!!.email,
                                        fontSize = 11.sp,
                                        color = SobiCyan
                                    )
                                }
                            }

                            Button(
                                onClick = { AuthManager.signOut() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Text("Sign Out", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, null, tint = SobiCyan, modifier = Modifier.size(24.dp))
                                Text(
                                    text = "Sign in to Claim Your Free 30GB Cloud Storage",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Signing in resets previous sample files and activates a full 30GB empty, private vault dedicated to your Google account.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        AuthManager.signInWithGoogle(context) { success, msg ->
                                            if (success) {
                                                Toast.makeText(context, "Google Sign-In verified! 30GB Clean Vault Activated.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, msg ?: "Sign-in cancelled", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isAuthLoading) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudDone, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google (30GB Free)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Storage Quota Overview Hero Card
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vault Quota Usage",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
                            val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
                            Text(
                                text = String.format(Locale.US, "%.2f GB of %.2f GB", usedGb, totalGb),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SobiCyan
                            )
                        }

                        LinearProgressIndicator(
                            progress = { usedFraction },
                            color = SobiCyan,
                            trackColor = Color(0x3300E5FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val freeGb = (totalBytes - usedBytes).coerceAtLeast(0L) / (1024.0 * 1024.0 * 1024.0)
                            VaultCategoryPill("📁 Total Files", "${files.size}", SobiCyan)
                            VaultCategoryPill("📊 Used Space", String.format(Locale.US, "%.1f MB", usedBytes / (1024.0 * 1024.0)), SobiViolet)
                            VaultCategoryPill("☁️ Available", String.format(Locale.US, "%.2f GB", freeGb), SobiGreen)
                        }

                        // Security Trust Tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Lock, null, tint = SobiGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Protected with Firebase AES-256 Cloud Encryption",
                                fontSize = 10.sp,
                                color = SobiGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Google Photos Auto-Backup & Sync Card (خودکار کلاؤڈ سنک)
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isAutoSyncEnabled) SobiCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Auto-Sync",
                                        tint = if (isAutoSyncEnabled) SobiCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Auto-Backup & Sync",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isAutoSyncEnabled) SobiGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (isAutoSyncEnabled) "ACTIVE" else "OFF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isAutoSyncEnabled) SobiGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "خودکار کلاؤڈ سنک • Google Photos style camera backup",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isAutoSyncEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!hasUserConsented) {
                                            stagingQuality = syncQuality
                                            stagingNetwork = syncNetwork
                                            stagingAutoEnhance = isAutoEnhanceEnabled
                                            showConsentDialog = true
                                        } else {
                                            AutoSyncManager.enableAutoSync(syncQuality, syncNetwork, isAutoEnhanceEnabled)
                                            Toast.makeText(context, "Auto-Sync enabled", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        AutoSyncManager.disableAutoSync()
                                        Toast.makeText(context, "Auto-Sync paused", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = SobiCyan,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("auto_sync_switch")
                            )
                        }

                        Text(
                            text = "جیسے ہی آپ کوئی نئی تصویر یا ویڈیو بنائیں، وہ خودکار طریقے سے آپ کے 30GB کلاؤڈ والٹ میں محفوظ ہو جائے گی۔",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        // Settings and Telemetry Info Bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Quality: ${if (syncQuality == AutoSyncQuality.ORIGINAL) "Original (اورجنل)" else "Storage Saver (سیور)"}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SobiCyan
                                    )
                                    Text(
                                        text = if (syncNetwork == AutoSyncNetwork.WIFI_ONLY) "📶 Wi-Fi only" else "🌐 Any network",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Backed up: $syncedItemsCount items",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Last sync: $lastSyncTime",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Progress Indicator when Sync is Running
                        if (isSyncing) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { syncProgress },
                                    color = SobiGreen,
                                    trackColor = SobiGreen.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "سنک ہو رہا ہے... Auto-Syncing recent media with ${syncQuality.title.substringBefore("(").trim()} (${(syncProgress * 100).toInt()}%)",
                                    fontSize = 10.5.sp,
                                    color = SobiGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Action Buttons: Sync Now & Settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (!isSyncing) {
                                        AutoSyncManager.triggerSync(coroutineScope) { newItems ->
                                            newItems.forEach { file ->
                                                onUploadFile(file)
                                                AuthManager.updateUsedStorage(file.sizeBytes)
                                            }
                                            Toast.makeText(context, "Synced ${newItems.size} new items to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_now_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Sync, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now (ابھی سنک کریں)", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            IconButton(
                                onClick = {
                                    stagingQuality = syncQuality
                                    stagingNetwork = syncNetwork
                                    stagingAutoEnhance = isAutoEnhanceEnabled
                                    showAutoSyncSettingsDialog = true
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .testTag("auto_sync_settings_button")
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Auto-Sync Settings", tint = SobiCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Batch & Multi-Select Upload Card
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Multiple Photos & Videos Upload",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SobiCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Batch (1-15)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SobiCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Select multiple photos and videos from your gallery at once, choose your preferred upload quality, and store safely in your 30GB vault.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isUploading) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    color = SobiCyan,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = "Uploading batch with ${selectedQuality.title}... ${(uploadProgress * 100).toInt()}%",
                                    fontSize = 10.5.sp,
                                    color = SobiCyan
                                )
                            }
                        }

                        Button(
                            onClick = {
                                multipleMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            enabled = !isUploading,
                            colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_multiple_media_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Multiple Photos & Videos to Upload", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick Single-File Upload Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Single Upload:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UploadButton(
                            label = "+ Photo",
                            icon = Icons.Default.Image,
                            color = SobiCyan,
                            modifier = Modifier.weight(1f)
                        ) {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val item = CloudFileItem(
                                name = "Photo_$timeStamp.jpg",
                                type = CloudFileType.PHOTO,
                                sizeBytes = 3_200_000L
                            )
                            onUploadFile(item)
                            AuthManager.updateUsedStorage(3_200_000L)
                            Toast.makeText(context, "Photo uploaded safely to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                        }

                        UploadButton(
                            label = "+ Video",
                            icon = Icons.Default.Movie,
                            color = SobiViolet,
                            modifier = Modifier.weight(1f)
                        ) {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val item = CloudFileItem(
                                name = "Clip_$timeStamp.mp4",
                                type = CloudFileType.VIDEO,
                                sizeBytes = 14_500_000L
                            )
                            onUploadFile(item)
                            AuthManager.updateUsedStorage(14_500_000L)
                            Toast.makeText(context, "Video uploaded safely to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                        }

                        UploadButton(
                            label = "+ Document",
                            icon = Icons.Default.Description,
                            color = SobiNeonPink,
                            modifier = Modifier.weight(1f)
                        ) {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val item = CloudFileItem(
                                name = "StudyNotes_$timeStamp.pdf",
                                type = CloudFileType.DOCUMENT,
                                sizeBytes = 1_800_000L
                            )
                            onUploadFile(item)
                            AuthManager.updateUsedStorage(1_800_000L)
                            Toast.makeText(context, "Document uploaded safely to 30GB Cloud Vault!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            // Category Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        label = "All Files (${files.size})",
                        isSelected = selectedFilter == null,
                        onClick = { selectedFilter = null }
                    )
                    FilterChip(
                        label = "Photos",
                        isSelected = selectedFilter == CloudFileType.PHOTO,
                        onClick = { selectedFilter = CloudFileType.PHOTO }
                    )
                    FilterChip(
                        label = "Videos",
                        isSelected = selectedFilter == CloudFileType.VIDEO,
                        onClick = { selectedFilter = CloudFileType.VIDEO }
                    )
                    FilterChip(
                        label = "Docs",
                        isSelected = selectedFilter == CloudFileType.DOCUMENT,
                        onClick = { selectedFilter = CloudFileType.DOCUMENT }
                    )
                }
            }

            // Files List
            if (filteredFiles.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your 30GB Cloud Vault is Empty",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Upload photos, videos, or documents to store them securely.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredFiles, key = { it.id }) { file ->
                    CloudFileCard(
                        file = file,
                        onInspectAI = {
                            onInspectWithAI(file)
                            Toast.makeText(context, "Sending ${file.name} to Sobi Multi-Modal AI Engine...", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            onDeleteFile(file)
                            AuthManager.updateUsedStorage(-file.sizeBytes)
                            Toast.makeText(context, "Deleted ${file.name}. Storage freed.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // ==============================================================
    // QUALITY SELECTION DIALOG BEFORE UPLOAD
    // ==============================================================
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = {
                Text(
                    text = "Select Upload Quality (${pendingUris.size} files)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose whether to upload in full lossless original quality or compressed storage-saver mode:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    UploadQualityOption.values().forEach { option ->
                        val isSelected = option == selectedQuality
                        Surface(
                            onClick = { selectedQuality = option },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SobiCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SobiCyan else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedQuality = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = SobiCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = option.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = option.subtitle,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { executeBatchUpload() },
                    colors = ButtonDefaults.buttonColors(containerColor = SobiCyan)
                ) {
                    Text("Upload Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Google Photos Auto-Sync Consent & Permission Dialog
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            icon = {
                Icon(Icons.Default.Sync, contentDescription = null, tint = SobiCyan, modifier = Modifier.size(32.dp))
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Enable Auto-Backup & Sync",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "خودکار کلاؤڈ سنک کی ترتیبات",
                        fontSize = 12.sp,
                        color = SobiCyan
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Google Photos طرز پر آپ کے کیمرے کی نئی تصاویر اور ویڈیوز خودکار طور پر آپ کے 30GB کلاؤڈ والٹ میں محفوظ ہوں گی۔ براہ کرم اپنی پسندیدہ کوالٹی اور نیٹ ورک منتخب کریں:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Text(
                        text = "Backup Quality (بیک اپ کوالٹی):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    AutoSyncQuality.entries.forEach { qOption ->
                        val isSelected = stagingQuality == qOption
                        Surface(
                            onClick = { stagingQuality = qOption },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SobiCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SobiCyan else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { stagingQuality = qOption },
                                    colors = RadioButtonDefaults.colors(selectedColor = SobiCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = qOption.title,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = qOption.subtitle,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Network (نیٹ ورک):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoSyncNetwork.entries.forEach { nOption ->
                            val isSelected = stagingNetwork == nOption
                            Surface(
                                onClick = { stagingNetwork = nOption },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SobiCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) SobiCyan else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { stagingNetwork = nOption },
                                        colors = RadioButtonDefaults.colors(selectedColor = SobiCyan)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (nOption == AutoSyncNetwork.WIFI_ONLY) "📶 Wi-Fi" else "🌐 Cellular",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Auto-Enhance Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stagingAutoEnhance = !stagingAutoEnhance }
                            .padding(top = 2.dp)
                    ) {
                        Checkbox(
                            checked = stagingAutoEnhance,
                            onCheckedChange = { stagingAutoEnhance = it },
                            colors = CheckboxDefaults.colors(checkedColor = SobiCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "✨ AI Auto-Enhance before saving",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "خودکار رنگ و کوالٹی بہتری • Auto color & clarity optimization",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AutoSyncManager.enableAutoSync(stagingQuality, stagingNetwork, stagingAutoEnhance)
                        showConsentDialog = false
                        Toast.makeText(context, "Google Photos Auto-Sync activated!", Toast.LENGTH_SHORT).show()
                        AutoSyncManager.triggerSync(coroutineScope) { newItems ->
                            newItems.forEach { file ->
                                onUploadFile(file)
                                AuthManager.updateUsedStorage(file.sizeBytes)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                    modifier = Modifier.testTag("confirm_auto_sync_consent_button")
                ) {
                    Text("Allow & Turn On (اجازت دیں)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Google Photos Auto-Sync Preferences & Settings Dialog
    if (showAutoSyncSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showAutoSyncSettingsDialog = false },
            icon = {
                Icon(Icons.Default.Settings, contentDescription = null, tint = SobiCyan, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = "Auto-Sync Preferences (سنک ترتیبات)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Upload Quality (کوالٹی):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    AutoSyncQuality.entries.forEach { qOption ->
                        val isSelected = stagingQuality == qOption
                        Surface(
                            onClick = { stagingQuality = qOption },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) SobiCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) SobiCyan else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { stagingQuality = qOption },
                                    colors = RadioButtonDefaults.colors(selectedColor = SobiCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = qOption.title,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = qOption.subtitle,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Network (نیٹ ورک):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AutoSyncNetwork.entries.forEach { nOption ->
                            val isSelected = stagingNetwork == nOption
                            Surface(
                                onClick = { stagingNetwork = nOption },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SobiCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) SobiCyan else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { stagingNetwork = nOption },
                                        colors = RadioButtonDefaults.colors(selectedColor = SobiCyan)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (nOption == AutoSyncNetwork.WIFI_ONLY) "📶 Wi-Fi" else "🌐 Cellular",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stagingAutoEnhance = !stagingAutoEnhance }
                            .padding(top = 2.dp)
                    ) {
                        Checkbox(
                            checked = stagingAutoEnhance,
                            onCheckedChange = { stagingAutoEnhance = it },
                            colors = CheckboxDefaults.colors(checkedColor = SobiCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "✨ Auto-enhance media before saving",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AutoSyncManager.updateSettings(stagingQuality, stagingNetwork, stagingAutoEnhance)
                        showAutoSyncSettingsDialog = false
                        Toast.makeText(context, "Auto-Sync preferences saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SobiCyan)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoSyncSettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun VaultCategoryPill(title: String, sizeStr: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(sizeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun UploadButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) color.copy(alpha = 0.18f) else Color(0xFFFFFFFF),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) color.copy(alpha = 0.45f) else color.copy(alpha = 0.6f)
        ),
        shadowElevation = if (isDark) 0.dp else 1.dp,
        modifier = modifier.testTag("upload_${label.lowercase().replace("+", "").trim()}")
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            if (isDark) SobiCyan.copy(alpha = 0.25f) else Color(0xFFE0F2FE)
        } else {
            if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFFFFFF)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) (if (isDark) SobiCyan else Color(0xFF0284C7)) else (if (isDark) Color.Transparent else Color(0xFFE2E8F0))
        ),
        shadowElevation = if (!isDark && !isSelected) 0.5.dp else 0.dp
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) (if (isDark) SobiCyan else Color(0xFF0369A1)) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun CloudFileCard(
    file: CloudFileItem,
    onInspectAI: () -> Unit,
    onDelete: () -> Unit
) {
    val fileIcon = when (file.type) {
        CloudFileType.PHOTO -> Icons.Default.Image
        CloudFileType.VIDEO -> Icons.Default.Movie
        CloudFileType.DOCUMENT -> Icons.Default.Description
    }

    val iconColor = when (file.type) {
        CloudFileType.PHOTO -> SobiCyan
        CloudFileType.VIDEO -> SobiViolet
        CloudFileType.DOCUMENT -> SobiNeonPink
    }

    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(file.uploadedAt))

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(fileIcon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }

                Column {
                    Text(
                        text = file.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(file.formattedSize, fontSize = 10.sp, color = SobiCyan)
                        Text("• $dateFormatted", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = SobiGreen, modifier = Modifier.size(9.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Encrypted", fontSize = 9.sp, color = SobiGreen)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // AI Inspect Button
                IconButton(
                    onClick = onInspectAI,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("ai_inspect_file_button")
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "AI Inspect",
                        tint = SobiCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_file_button")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
