package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private data class SecureMediaItem(
    val id: String,
    val name: String,
    val mime: String,
    val path: String,
    val addedAt: Long,
)

/** آلبوم داخلیِ فضای امن؛ فقط عکس، ویدیو و صوت را می‌پذیرد. */
@Composable
fun SecureMediaGalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, "hamyar_secure_media") }
    var media by remember { mutableStateOf(readSecureMedia(store).filter { File(it.path).exists() }) }
    var selected by remember { mutableStateOf<SecureMediaItem?>(null) }
    var exporting by remember { mutableStateOf<SecureMediaItem?>(null) }
    var columns by remember { mutableIntStateOf(3) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    SecureWebEffect("Screenshots are disabled in the private album.")

    fun save(items: List<SecureMediaItem>) {
        media = items
        writeSecureMedia(store, items)
    }

    val importMedia = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> copySecureMedia(context, uri) }
            }
            if (added.isNotEmpty()) {
                save(added + media)
                notice = "${added.size} رسانه فقط داخل فضای امن گوشی ذخیره شد."
            } else notice = "فایل قابل‌قبولی انتخاب نشد."
        }
    }
    val exportMedia = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val item = exporting
        exporting = null
        if (uri == null || item == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        File(item.path).inputStream().use { it.copyTo(output) }
                    } ?: error("output")
                    true
                }.getOrDefault(false)
            }
            notice = if (ok) "یک نسخه در حافظهٔ انتخابی ذخیره شد." else "خروجی گرفتن ممکن نشد."
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آلبوم شخصی", onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("عکس، ویدیو و صوت؛ فایل‌ها در پوشهٔ خصوصی برنامه می‌مانند.", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { importMedia.launch(arrayOf("image/*", "video/*", "audio/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text("افزودن رسانه") }
                (2..4).forEach { span ->
                    TextButton(onClick = { columns = span }) { Text(if (span == columns) "● $span" else span.toString()) }
                }
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            if (media.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("آلبوم هنوز خالی است.") }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(media, key = { it.id }) { item ->
                        Card(Modifier.fillMaxWidth().clickable { selected = item }) {
                            Box(
                                Modifier.fillMaxWidth().size((310 / columns).dp).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (item.mime.startsWith("image/")) {
                                    val bitmap = remember(item.path, File(item.path).lastModified()) { PdfSafe.decodeFileCapped(item.path, 500) }
                                    bitmap?.let {
                                        Image(it.asImageBitmap(), item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } ?: Text("تصویر")
                                } else {
                                    Text(if (item.mime.startsWith("video/")) "🎬" else "🎧", style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        SecureMediaViewer(
            item = item,
            onClose = { selected = null },
            onExport = {
                exporting = item
                exportMedia.launch(item.name)
            },
            onChanged = {
                // تغییر timestamp باعث refresh تامبنیل می‌شود.
                selected = item.copy(addedAt = System.currentTimeMillis())
                media = media.toList()
            },
            onDelete = {
                File(item.path).delete()
                save(media.filterNot { it.id == item.id })
                selected = null
            },
        )
    }
}

@Composable
private fun SecureMediaViewer(
    item: SecureMediaItem,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onChanged: () -> Unit,
    onDelete: () -> Unit,
) {
    var revision by remember(item.path) { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF07111F))) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    item.mime.startsWith("image/") -> ZoomableSecureImage(item.path, revision)
                    item.mime.startsWith("video/") -> AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                setVideoURI(Uri.fromFile(File(item.path)))
                                setMediaController(MediaController(context).also { it.setAnchorView(this) })
                                setOnPreparedListener { it.isLooping = false; start() }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { it.stopPlayback() },
                    )
                    else -> SecureAudioPlayer(item)
                }
            }
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                if (item.mime.startsWith("image/")) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            transformSecureImage(item.path, rotate = -90f, crop = false)
                            revision++; onChanged()
                        }, modifier = Modifier.weight(1f)) { Text("چرخش چپ") }
                        OutlinedButton(onClick = {
                            transformSecureImage(item.path, rotate = 90f, crop = false)
                            revision++; onChanged()
                        }, modifier = Modifier.weight(1f)) { Text("چرخش راست") }
                        OutlinedButton(onClick = {
                            transformSecureImage(item.path, rotate = 0f, crop = true)
                            revision++; onChanged()
                        }, modifier = Modifier.weight(1f)) { Text("برش مربع") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("بستن") }
                    OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("خروجی") }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("حذف") }
                }
            }
        }
    }
}

@Composable
private fun ZoomableSecureImage(path: String, revision: Int) {
    val bitmap = remember(path, revision) { PdfSafe.decodeFileCapped(path, 2200) }
    var scale by remember(path, revision) { mutableFloatStateOf(1f) }
    var offsetX by remember(path, revision) { mutableFloatStateOf(0f) }
    var offsetY by remember(path, revision) { mutableFloatStateOf(0f) }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(path, revision) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                        else { offsetX += pan.x; offsetY += pan.y }
                    }
                }
                .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY },
        )
    } ?: Text("تصویر خوانده نشد.", color = Color.White)
}

@Composable
private fun SecureAudioPlayer(item: SecureMediaItem) {
    var playing by remember { mutableStateOf(false) }
    val player = remember(item.path) { MediaPlayer.create(LocalContext.current, Uri.fromFile(File(item.path))) }
    DisposableEffect(player) { onDispose { runCatching { player?.release() } } }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎧", style = MaterialTheme.typography.displayLarge)
        OutlinedButton(onClick = {
            if (player?.isPlaying == true) { player.pause(); playing = false }
            else { player?.start(); playing = true }
        }) { Text(if (playing) "مکث" else "پخش") }
    }
}

private fun secureMediaDir(context: android.content.Context): File = File(context.filesDir, "secure-media").apply { mkdirs() }

private fun copySecureMedia(context: android.content.Context, uri: Uri): SecureMediaItem? = runCatching {
    val mime = context.contentResolver.getType(uri).orEmpty()
    if (!(mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/"))) return null
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty().ifBlank {
        when { mime.startsWith("image/") -> "jpg"; mime.startsWith("video/") -> "mp4"; else -> "m4a" }
    }
    val id = "sm_${System.currentTimeMillis()}_${uri.hashCode().toUInt()}"
    val file = File(secureMediaDir(context), "$id.$ext")
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } ?: return null
    SecureMediaItem(id, "رسانه-${JalaliDate.todayIso()}.$ext", mime, file.absolutePath, System.currentTimeMillis())
}.getOrNull()

private fun transformSecureImage(path: String, rotate: Float, crop: Boolean) {
    runCatching {
        var bitmap = BitmapFactory.decodeFile(path) ?: return
        if (rotate != 0f) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotate) }, true)
        }
        if (crop) {
            val side = minOf(bitmap.width, bitmap.height)
            bitmap = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        }
        File(path).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }
}

private fun readSecureMedia(store: LocalStore): List<SecureMediaItem> = runCatching {
    val arr = JSONArray(store.getString("items", "[]"))
    buildList {
        for (i in 0 until arr.length()) arr.getJSONObject(i).let { o ->
            add(SecureMediaItem(o.getString("id"), o.getString("name"), o.getString("mime"), o.getString("path"), o.optLong("addedAt")))
        }
    }
}.getOrDefault(emptyList())

private fun writeSecureMedia(store: LocalStore, items: List<SecureMediaItem>) {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(JSONObject().put("id", item.id).put("name", item.name).put("mime", item.mime).put("path", item.path).put("addedAt", item.addedAt))
    }
    store.putString("items", arr.toString())
}
