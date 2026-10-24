package com.example.model

enum class CloudFileType(val label: String) {
    PHOTO("Photos"),
    VIDEO("Videos"),
    DOCUMENT("Documents")
}

data class CloudFileItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: CloudFileType,
    val sizeBytes: Long,
    val uploadedAt: Long = System.currentTimeMillis(),
    val localUri: String? = null,
    val isEncrypted: Boolean = true,
    val cloudUrl: String = "https://firebasestorage.googleapis.com/v0/b/chatsobi-vault/o/"
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}
