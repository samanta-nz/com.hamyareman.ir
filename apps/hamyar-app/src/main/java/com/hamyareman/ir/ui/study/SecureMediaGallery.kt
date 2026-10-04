package com.hamyareman.ir.ui.study

import android.content.Intent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import androidx.core.content.FileProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
fun SecureMediaGalleryScreen(onBack: () -> Unit, onOpenDiary: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, "hamyar_secure_media") }
    val diaryStore = remember { LocalStore(context, "hamyar_private_diary") }
    val diaryCover = when (diaryStore.getString("cover", "celestial")) {
        "botanical" -> "file:///android_asset/diary/cover-botanical.jpg"
        "geometric" -> "file:///android_asset/diary/cover-geometric.jpg"
        else -> "file:///android_asset/diary/cover-celestial.jpg"
    }
    var media by remember { mutableStateOf(readSecureMedia(store).filter { File(it.path).exists() }) }
    var selected by remember { mutableStateOf<SecureMediaItem?>(null) }
    var exporting by remember { mutableStateOf<SecureMediaItem?>(null) }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { importMedia.launch(arrayOf("image/*", "video/*", "audio/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text("افزودن رسانه") }
                Text("چیدمان تطبیقی", style = MaterialTheme.typography.labelMedium)
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 118.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // دفتر خاطرات همیشه مثل یک کتاب واقعی، کنار بقیهٔ رسانه‌ها دیده می‌شود.
                item(key = "private-diary-book") {
                    Card(Modifier.fillMaxWidth().clickable(onClick = onOpenDiary)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = diaryCover,
                                contentDescription = "دفتر خاطرات",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.72f),
                            )
                            Text("دفتر خاطرات", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(5.dp))
                        }
                    }
                }
                items(media, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth().clickable { selected = item }) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant),
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
                if (media.isEmpty()) {
                    item(key = "empty-media") {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                            Text("هنوز رسانه‌ای اضافه نشده است.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        SecureMediaViewer(
            item = item,
            playlist = media.filter { candidate ->
                (item.mime.startsWith("audio/") && candidate.mime.startsWith("audio/")) ||
                    (item.mime.startsWith("video/") && candidate.mime.startsWith("video/"))
            },
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
    playlist: List<SecureMediaItem>,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onChanged: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var revision by remember(item.path) { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF07111F))) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    item.mime.startsWith("image/") -> ZoomableSecureImage(item.path, revision)
                    item.mime.startsWith("video/") -> SecureMedia3Player(item, playlist, video = true)
                    else -> SecureMedia3Player(item, playlist, video = false)
                }
            }
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                if (item.mime.startsWith("image/")) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { transformSecureImage(item.path, rotate = -90f, crop = false); revision++; onChanged() }, modifier = Modifier.weight(1f)) { Text("↶") }
                        OutlinedButton(onClick = { transformSecureImage(item.path, rotate = 90f, crop = false); revision++; onChanged() }, modifier = Modifier.weight(1f)) { Text("↷") }
                        OutlinedButton(onClick = { transformSecureImage(item.path, rotate = 0f, crop = true); revision++; onChanged() }, modifier = Modifier.weight(1f)) { Text("□") }
                        OutlinedButton(onClick = { transformSecureImage(item.path, rotate = 0f, crop = false, mirrorX = true); revision++; onChanged() }, modifier = Modifier.weight(1f)) { Text("⇋") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) { Text("بستن") }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { shareSecureMedia(context, item) }) { Icon(Icons.Default.IosShare, contentDescription = "اشتراک") }
                        IconButton(onClick = onExport) { Icon(Icons.Default.Download, contentDescription = "خروجی") }
                        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
                    }
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
private fun SecureMedia3Player(item: SecureMediaItem, playlist: List<SecureMediaItem>, video: Boolean) {
    val context = LocalContext.current
    val queue = remember(item.id, playlist) { playlist.ifEmpty { listOf(item) } }
    val startIndex = remember(item.id, queue) { queue.indexOfFirst { it.id == item.id }.coerceAtLeast(0) }
    val playback = remember(item.id) {
        com.hamyareman.ir.platform.feature.playback.PlaybackController(context)
    }
    val state by playback.state.collectAsState()
    var position by remember { mutableLongStateOf(0L) }
    var scrub by remember { mutableLongStateOf(-1L) }
    var sleepEndsAt by remember { mutableLongStateOf(0L) }
    var sleepRemaining by remember { mutableLongStateOf(0L) }
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(item.id, queue) {
        if (playback.connect()) {
            val mediaItems = queue.map { row ->
                MediaItem.Builder()
                    .setMediaId(row.id)
                    .setUri(Uri.fromFile(File(row.path)))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(row.name).setDisplayTitle(row.name).build())
                    .build()
            }
            playback.setMediaItems(mediaItems, startIndex)
        }
    }
    LaunchedEffect(state.connected, state.playing) {
        if (!state.connected) return@LaunchedEffect
        while (true) {
            position = playback.positionMs
            if (state.playing) phase += 0.12f
            delay(if (state.playing) 120L else 500L)
        }
    }
    LaunchedEffect(sleepEndsAt) {
        if (sleepEndsAt <= 0L) return@LaunchedEffect
        while (sleepEndsAt > System.currentTimeMillis()) {
            sleepRemaining = (sleepEndsAt - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(1_000L)
        }
        playback.pause()
        sleepEndsAt = 0L
        sleepRemaining = 0L
    }
    DisposableEffect(playback) {
        // همان Media3 مشترک برای آلبوم و کتاب صوتی؛ رسانهٔ خصوصی با بستن نمایشگر قطع می‌شود.
        com.hamyareman.ir.platform.feature.playback.TeachGate.enter()
        onDispose {
            runCatching { playback.stop() }
            playback.release()
            com.hamyareman.ir.platform.feature.playback.TeachGate.exit()
        }
    }

    val current = queue.getOrNull(state.currentIndex) ?: item
    val duration = state.durationMs.coerceAtLeast(0L)
    val shown = if (scrub >= 0L) scrub else position.coerceAtLeast(0L)
    val sliderMax = maxOf(duration, shown, 1L)

    Column(
        Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (video) {
            AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        useController = true
                        player = playback.asPlayer()
                    }
                },
                update = { it.player = playback.asPlayer() },
                modifier = Modifier.fillMaxWidth().weight(1f),
                onRelease = { it.player = null },
            )
        } else {
            AlbumArtEqualizer(current.path, state.playing, phase)
        }
        Text(state.title.ifBlank { current.name }, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Slider(
            value = shown.coerceIn(0L, sliderMax).toFloat(),
            onValueChange = { scrub = it.toLong() },
            onValueChangeFinished = {
                playback.seekTo(if (scrub >= 0L) scrub else position)
                position = if (scrub >= 0L) scrub else position
                scrub = -1L
            },
            valueRange = 0f..sliderMax.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(mediaTime(shown), color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text("−${mediaTime((duration - shown).coerceAtLeast(0L))}", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { value ->
                FilterChip(selected = state.speed == value, onClick = { playback.setSpeed(value) }, label = { Text(value.toString() + "x") })
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedButton(onClick = { playback.seekBy(-10_000L) }, modifier = Modifier.weight(1f)) { Text("۱۰−") }
            OutlinedButton(onClick = { playback.seekBy(10_000L) }, modifier = Modifier.weight(1f)) { Text("۱۰+") }
            OutlinedButton(onClick = {
                val p = playback.asPlayer()
                if (p != null) p.repeatMode = if (p.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) androidx.media3.common.Player.REPEAT_MODE_OFF else androidx.media3.common.Player.REPEAT_MODE_ONE
            }, modifier = Modifier.weight(1f)) { Text("تکرار") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedButton(
                onClick = { playback.setShuffle(!state.shuffleEnabled) },
                modifier = Modifier.weight(1f),
            ) { Text(if (state.shuffleEnabled) "تصادفی ✓" else "تصادفی") }
            OutlinedButton(
                onClick = { playback.seekToPrevious() },
                enabled = state.mediaCount > 1,
                modifier = Modifier.weight(1f),
            ) { Text("قبلی") }
            OutlinedButton(
                onClick = {
                    com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
                    if (state.playing) playback.pause() else playback.play()
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (state.playing) "مکث" else "پخش") }
            OutlinedButton(
                onClick = { playback.seekToNext() },
                enabled = state.mediaCount > 1,
                modifier = Modifier.weight(1f),
            ) { Text("بعدی") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (sleepEndsAt > 0L) {
                TextButton(onClick = { sleepEndsAt = 0L; sleepRemaining = 0L }, modifier = Modifier.weight(1f)) {
                    Text("لغو تایمر ${mediaTime(sleepRemaining)}")
                }
            } else {
                listOf(15, 30, 45).forEach { minutes ->
                    TextButton(
                        onClick = {
                            sleepEndsAt = System.currentTimeMillis() + minutes * 60_000L
                            sleepRemaining = minutes * 60_000L
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("خواب $minutes د") }
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun mediaTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1_000L
    return com.hamyareman.ir.platform.core.common.toPersianDigits(
        "%d:%02d".format(java.util.Locale.US, seconds / 60L, seconds % 60L),
    )
}

@Composable
internal fun AlbumArtEqualizer(path: String, playing: Boolean, phase: Float) {
    val context = LocalContext.current
    val art = remember(path) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (path.startsWith("content://")) retriever.setDataSource(context, Uri.parse(path))
                else retriever.setDataSource(path)
                retriever.embeddedPicture?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }
    Box(Modifier.size(238.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val base = size.minDimension * 0.36f
            repeat(36) { i ->
                val angle = (Math.PI * 2.0 * i / 36.0).toFloat()
                val pulse = if (playing) (kotlin.math.sin(phase + i * 0.77f) + 1f) / 2f else 0.12f
                val length = 7f + pulse * 24f
                val start = Offset(center.x + kotlin.math.cos(angle) * base, center.y + kotlin.math.sin(angle) * base)
                val end = Offset(center.x + kotlin.math.cos(angle) * (base + length), center.y + kotlin.math.sin(angle) * (base + length))
                drawLine(Color(0xFF62D8B3), start, end, strokeWidth = 5f, cap = StrokeCap.Round)
            }
        }
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = "جلد صوت",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(154.dp).clip(CircleShape),
            )
        } else {
            Box(
                Modifier.size(154.dp).clip(CircleShape).background(Color(0xFF183E45)),
                contentAlignment = Alignment.Center,
            ) { Text("🎧", style = MaterialTheme.typography.displayLarge) }
        }
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

private fun transformSecureImage(path: String, rotate: Float, crop: Boolean, mirrorX: Boolean = false) {
    runCatching {
        var bitmap = BitmapFactory.decodeFile(path) ?: return
        if (rotate != 0f || mirrorX) {
            bitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height,
                Matrix().apply { postRotate(rotate); if (mirrorX) postScale(-1f, 1f) },
                true,
            )
        }
        if (crop) {
            val side = minOf(bitmap.width, bitmap.height)
            bitmap = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        }
        File(path).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    }
}

private fun shareSecureMedia(context: android.content.Context, item: SecureMediaItem) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", File(item.path))
        val share = Intent(Intent.ACTION_SEND).apply {
            type = item.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "اشتراک رسانه"))
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
