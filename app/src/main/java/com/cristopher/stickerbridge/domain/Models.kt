package com.cristopher.stickerbridge.domain

import android.net.Uri

enum class Platform(val label: String) { TIKTOK("TikTok"), WHATSAPP("WhatsApp") }
data class StickerItem(val uri: Uri, val name: String, val mimeType: String, val sizeBytes: Long = 0)
data class TransferRecord(val source: Platform, val destination: Platform, val name: String, val date: String, val result: String)
data class UiState(
    val source: Platform = Platform.TIKTOK,
    val destination: Platform = Platform.WHATSAPP,
    val incoming: List<StickerItem> = emptyList(),
    val processed: List<StickerItem> = emptyList(),
    val history: List<TransferRecord> = emptyList(),
    val processing: Boolean = false,
    val message: String? = null,
    val tiktokConnected: Boolean = false
)
