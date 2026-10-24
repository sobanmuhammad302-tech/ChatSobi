package com.example.service

import com.example.model.CloudFileItem
import com.example.model.CloudFileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AutoSyncQuality(val title: String, val subtitle: String, val sizeMultiplier: Float) {
    ORIGINAL(
        title = "🌟 Original Quality (اورجنل کوالٹی)",
        subtitle = "Full lossless camera resolution. Preserves all details & RAW megapixel data.",
        sizeMultiplier = 1.0f
    ),
    STORAGE_SAVER(
        title = "⚡ Storage Saver (کمپریسڈ / اسٹوریج سیور)",
        subtitle = "Slightly reduces resolution & size (~60% space savings). Stores 3x more photos safely.",
        sizeMultiplier = 0.4f
    )
}

enum class AutoSyncNetwork(val label: String, val description: String) {
    WIFI_ONLY(
        label = "📶 Wi-Fi Only (تجویز کردہ)",
        description = "Only back up when connected to Wi-Fi. Saves mobile cellular data."
    ),
    ANY_NETWORK(
        label = "🌐 Wi-Fi & Cellular Data",
        description = "Instant backup even on cellular mobile network."
    )
}

object AutoSyncManager {
    private val _isAutoSyncEnabled = MutableStateFlow(false)
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private val _hasUserConsented = MutableStateFlow(false)
    val hasUserConsented: StateFlow<Boolean> = _hasUserConsented.asStateFlow()

    private val _syncQuality = MutableStateFlow(AutoSyncQuality.STORAGE_SAVER)
    val syncQuality: StateFlow<AutoSyncQuality> = _syncQuality.asStateFlow()

    private val _syncNetwork = MutableStateFlow(AutoSyncNetwork.WIFI_ONLY)
    val syncNetwork: StateFlow<AutoSyncNetwork> = _syncNetwork.asStateFlow()

    private val _isAutoEnhanceEnabled = MutableStateFlow(true)
    val isAutoEnhanceEnabled: StateFlow<Boolean> = _isAutoEnhanceEnabled.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Never")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _syncedItemsCount = MutableStateFlow(0)
    val syncedItemsCount: StateFlow<Int> = _syncedItemsCount.asStateFlow()

    fun enableAutoSync(
        quality: AutoSyncQuality,
        network: AutoSyncNetwork,
        autoEnhance: Boolean
    ) {
        _syncQuality.value = quality
        _syncNetwork.value = network
        _isAutoEnhanceEnabled.value = autoEnhance
        _hasUserConsented.value = true
        _isAutoSyncEnabled.value = true
    }

    fun disableAutoSync() {
        _isAutoSyncEnabled.value = false
    }

    fun updateSettings(
        quality: AutoSyncQuality,
        network: AutoSyncNetwork,
        autoEnhance: Boolean
    ) {
        _syncQuality.value = quality
        _syncNetwork.value = network
        _isAutoEnhanceEnabled.value = autoEnhance
    }

    fun triggerSync(
        scope: CoroutineScope,
        onNewFiles: (List<CloudFileItem>) -> Unit
    ) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncProgress.value = 0.05f

        scope.launch {
            // Scanning stage
            delay(500L)
            _syncProgress.value = 0.35f

            // Uploading & Compressing stage
            delay(600L)
            _syncProgress.value = 0.75f

            // Create synced backup files based on chosen quality
            val currentQuality = _syncQuality.value
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val basePhotoSize = 4_500_000L
            val photoSize = (basePhotoSize * currentQuality.sizeMultiplier).toLong()

            val newItems = listOf(
                CloudFileItem(
                    name = "AutoSync_Camera_${timeStamp}_01.jpg",
                    type = CloudFileType.PHOTO,
                    sizeBytes = photoSize
                ),
                CloudFileItem(
                    name = "AutoSync_Camera_${timeStamp}_02.jpg",
                    type = CloudFileType.PHOTO,
                    sizeBytes = photoSize
                ),
                CloudFileItem(
                    name = "AutoSync_Video_${timeStamp}.mp4",
                    type = CloudFileType.VIDEO,
                    sizeBytes = (24_000_000L * currentQuality.sizeMultiplier).toLong()
                )
            )

            delay(400L)
            _syncProgress.value = 1.0f
            delay(300L)

            _syncedItemsCount.value += newItems.size
            _lastSyncTime.value = SimpleDateFormat("h:mm a", Locale.US).format(Date())
            _isSyncing.value = false
            _syncProgress.value = 0f

            onNewFiles(newItems)
        }
    }
}
