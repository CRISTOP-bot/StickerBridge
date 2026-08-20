package com.cristopher.stickerbridge.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cristopher.stickerbridge.domain.*
import com.cristopher.stickerbridge.processor.StickerProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StickerViewModel(app: Application) : AndroidViewModel(app) {
    private val processor = StickerProcessor(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun setRoute(source: Platform) = _state.value.let { _state.value = it.copy(source = source, destination = if (source == Platform.TIKTOK) Platform.WHATSAPP else Platform.TIKTOK) }
    fun importUris(uris: List<Uri>) = viewModelScope.launch(Dispatchers.IO) {
        val items = uris.mapNotNull { runCatching { processor.import(it) }.getOrNull() }
        _state.value = _state.value.copy(incoming = items, message = if (items.isEmpty()) "No se encontró un formato compatible" else null)
    }
    fun process() = viewModelScope.launch(Dispatchers.IO) {
        val current = _state.value
        _state.value = current.copy(processing = true, processed = emptyList())
        val result = current.incoming.mapNotNull { runCatching { processor.process(it) }.getOrNull() }
        val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        _state.value = _state.value.copy(processing = false, processed = result, history = result.map { TransferRecord(current.source, current.destination, it.name, now, "Preparado") })
    }
}
