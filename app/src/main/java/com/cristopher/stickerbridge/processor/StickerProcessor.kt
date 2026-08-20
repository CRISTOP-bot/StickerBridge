package com.cristopher.stickerbridge.processor

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.cristopher.stickerbridge.domain.StickerItem
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class StickerProcessor(private val context: Context) {
    private val supported = setOf("image/webp", "image/png", "image/jpeg", "image/gif", "video/mp4")

    fun import(uri: Uri): StickerItem {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        require(mime in supported) { "Formato no compatible: $mime" }
        val name = queryName(resolver, uri) ?: "sticker_${UUID.randomUUID()}"
        val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        return StickerItem(uri, name, mime, size)
    }

    fun process(item: StickerItem): StickerItem {
        val extension = when (item.mimeType) { "image/webp" -> "webp"; "image/png" -> "png"; "image/jpeg" -> "jpg"; "image/gif" -> "gif"; else -> "mp4" }
        val output = File(context.filesDir, "stickers").apply { mkdirs() }.resolve("${item.name.hashCode()}.$extension")
        context.contentResolver.openInputStream(item.uri).use { input -> requireNotNull(input) { "No se pudo leer el contenido" }; FileOutputStream(output).use { out -> input!!.copyTo(out) } }
        return item.copy(uri = Uri.fromFile(output), sizeBytes = output.length())
    }

    private fun queryName(resolver: ContentResolver, uri: Uri): String? = resolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null }
}
