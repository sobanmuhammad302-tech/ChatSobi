package com.example.model

enum class Role {
    USER,
    SOBI
}

enum class KnowledgeDomain(val label: String, val iconName: String) {
    ALL("All Domains", "AutoAwesome"),
    SCIENCE("Science & Physics", "Science"),
    MATHEMATICS("Math & Calculus", "Calculate"),
    PROGRAMMING("Code & Tech", "Code"),
    HISTORY("World History", "HistoryEdu"),
    LITERATURE("Writing & Essay", "MenuBook"),
    LOGIC("Everyday Problem Solving", "Psychology")
}

data class AttachedMediaItem(
    val uri: String,
    val isVideo: Boolean = false,
    val quality: String = "Original Quality",
    val isCompressed: Boolean = false,
    val fileName: String = "media_item"
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineEngine: Boolean = true,
    val domain: KnowledgeDomain = KnowledgeDomain.ALL,
    val codeBlock: String? = null,
    val mathSteps: List<String>? = null,
    val formulas: List<String> = emptyList(),
    val sourceLabel: String = "",
    val isStreaming: Boolean = false,
    val attachments: List<AttachedMediaItem> = emptyList()
)

enum class MediaTab {
    PHOTO_ENHANCE,
    VIDEO_STUDIO,
    IMAGE_GEN,
    VIDEO_GEN
}

enum class CallMode {
    AUDIO,
    VIDEO
}

data class GeneratedArt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val prompt: String,
    val style: String,
    val aspectRatio: String,
    val imageDrawableRes: Int? = null,
    val isVideo: Boolean = false,
    val durationSec: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class MediaEnhanceProject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val originalBadge: String,
    val enhancedBadge: String,
    val improvementPercent: Int,
    val processLog: List<String>
)
