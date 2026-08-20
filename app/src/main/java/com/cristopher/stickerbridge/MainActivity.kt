package com.cristopher.stickerbridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.cristopher.stickerbridge.domain.Platform
import com.cristopher.stickerbridge.presentation.StickerViewModel

class MainActivity : ComponentActivity() {
    private val vm by viewModels<StickerViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); handleIntent(intent); setContent { StickerBridgeApp(vm) } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }
    private fun handleIntent(intent: Intent?) {
        val uris = when (intent?.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
        if (uris.isNotEmpty()) vm.importUris(uris)
    }
}

@Composable fun StickerBridgeApp(vm: StickerViewModel) {
    val state by vm.state.collectAsState(); val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { it?.let(vm::importUris) }
    MaterialTheme(colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Scaffold(topBar = { TopAppBar(title = { Text("StickerBridge") }) }) { pad ->
            LazyColumn(Modifier.padding(pad).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text("Transfiere tus stickers", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("entre TikTok y WhatsApp") }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = { vm.setRoute(Platform.TIKTOK) }) { Text("TikTok → WhatsApp") }; OutlinedButton(onClick = { vm.setRoute(Platform.WHATSAPP) }) { Text("WhatsApp → TikTok") } } }
                item { StatusCard("TikTok", false) { showInfo = true }; StatusCard("WhatsApp", isWhatsAppInstalled(context)) {} }
                item { Text("Origen: ${state.source.label}   ·   Destino: ${state.destination.label}", style = MaterialTheme.typography.titleMedium) }
                item { OutlinedButton(onClick = { picker.launch(arrayOf("image/webp", "image/png", "image/jpeg", "image/gif", "video/mp4")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Seleccionar stickers") } }
                if (state.incoming.isNotEmpty()) item { Text("Seleccionados: ${state.incoming.size}") }
                items(state.incoming) { item { ListItem(headlineContent = { Text(item.name) }, supportingContent = { Text(item.mimeType) }) } }
                if (state.incoming.isNotEmpty()) item { Button(onClick = vm::process, enabled = !state.processing, modifier = Modifier.fillMaxWidth()) { Text(if (state.processing) "Procesando…" else "Transferir ${state.incoming.size} sticker(s)") } }
                if (state.processed.isNotEmpty()) { item { Text("✓ Sticker preparado", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) }; item { Button(onClick = { share(context, state.processed.map { it.uri }, state.destination) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("Compartir en ${state.destination.label}") } } }
                state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                item { Text("Transferencias", style = MaterialTheme.typography.titleLarge); state.history.forEach { Text("${it.source.label} → ${it.destination.label} · ${it.name} · ${it.date}") } }
            }
        }
        if (showInfo) AlertDialog(onDismissRequest = { showInfo = false }, confirmButton = { TextButton({ showInfo = false }) { Text("Entendido") } }, title = { Text("TikTok") }, text = { Text("TikTok no ofrece una API pública documentada para leer o exportar la colección personal de stickers. StickerBridge no pide credenciales ni simula una conexión: recibe contenido mediante el menú Compartir de Android. La autenticación oficial solo se habilitará cuando configures credenciales OAuth aprobadas por TikTok.") })
    }
}

@Composable private fun StatusCard(name: String, connected: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth(), onClick = onClick) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (connected) "✓" else "○", color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline); Spacer(Modifier.width(12.dp)); Column { Text(name, fontWeight = FontWeight.Bold); Text(if (connected) "Conectado" else "No conectado") } } } }
private fun isWhatsAppInstalled(context: android.content.Context) = runCatching { context.packageManager.getPackageInfo("com.whatsapp", 0); true }.getOrDefault(false)
private fun share(context: android.content.Context, files: List<Uri>, destination: Platform) { val uris = files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(it.path!!)) }; val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply { type = "image/webp"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris)); if (destination == Platform.WHATSAPP) setPackage("com.whatsapp") }; context.startActivity(Intent.createChooser(intent, "Compartir sticker")) }
