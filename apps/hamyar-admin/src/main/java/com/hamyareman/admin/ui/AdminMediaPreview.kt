package com.hamyareman.admin.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.delay
import java.io.File

internal enum class PreviewKind { IMAGE, TEXT, HTML, AUDIO, PDF, OTHER }

internal fun previewKind(name: String, mime: String): PreviewKind {
    val n = name.lowercase()
    val m = mime.lowercase()
    return when {
        m.contains("pdf") || n.endsWith(".pdf") -> PreviewKind.PDF
        m.startsWith("image/") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".bmp") -> PreviewKind.IMAGE
        m.contains("html") || n.endsWith(".html") || n.endsWith(".htm") -> PreviewKind.HTML
        m.startsWith("audio/") || n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".wav") || n.endsWith(".ogg") || n.endsWith(".aac") -> PreviewKind.AUDIO
        m.startsWith("text/") || n.endsWith(".txt") || n.endsWith(".json") || n.endsWith(".csv") || n.endsWith(".md") || n.endsWith(".xml") -> PreviewKind.TEXT
        else -> PreviewKind.OTHER
    }
}

@Composable
fun AdminFilePreview(
    bucketId: String,
    fileId: String,
    name: String,
    mime: String,
    onClose: () -> Unit,
) {
    val api = LocalAdmin.current.api
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    val kind = remember(name, mime) { previewKind(name, mime) }

    LaunchedEffect(bucketId, fileId) {
        loading = true
        error = null
        when (val r = adminIo { api.downloadFile(bucketId, fileId) }) {
            is AppResult.Ok -> bytes = r.value
            is AppResult.Err -> error = r.error.userMessage
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(name.ifBlank { "پیش‌نمایش" }, onBack = onClose)
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
            error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            bytes == null -> Text("فایل خالی است.", modifier = Modifier.padding(16.dp))
            else -> Column(Modifier.fillMaxSize()) {
                val data = bytes!!
                when (kind) {
                    PreviewKind.IMAGE -> {
                        val bmp = remember(data) { runCatching { BitmapFactory.decodeByteArray(data, 0, data.size) }.getOrNull() }
                        if (bmp == null) Text("تصویر خوانده نشد.", modifier = Modifier.padding(16.dp))
                        else Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = name,
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    PreviewKind.TEXT -> {
                        val text = remember(data) { data.toString(Charsets.UTF_8).take(200_000) }
                        Text(text, modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), style = MaterialTheme.typography.bodySmall)
                    }
                    PreviewKind.HTML -> {
                        val html = remember(data) { data.toString(Charsets.UTF_8) }
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                }
                            },
                            update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) },
                        )
                    }
                    PreviewKind.AUDIO -> AudioPlayer(bytes = data, fileId = fileId, cacheDir = context.cacheDir)
                    PreviewKind.PDF -> PdfPager(bytes = data, fileId = fileId, cacheDir = context.cacheDir)
                    PreviewKind.OTHER -> Text(
                        "این نوع ($mime) پیش‌نمایش اختصاصی ندارد. اگر عکس/صدا/پی‌دی‌اف است نام فایل را با پسوند درست ذخیره کن.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PdfPager(bytes: ByteArray, fileId: String, cacheDir: File) {
    var page by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(fileId, bytes) {
        val f = File(cacheDir, "admin-pdf-$fileId.pdf")
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            f.writeBytes(bytes)
            pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            count = renderer.pageCount
        } catch (t: Throwable) {
            error = t.message ?: "پی‌دی‌اف باز نشد."
        }
        onDispose {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
            runCatching { f.delete() }
        }
    }
    LaunchedEffect(page, count, bytes) {
        if (count <= 0) return@LaunchedEffect
        val f = File(cacheDir, "admin-pdf-$fileId.pdf")
        runCatching {
            ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { r ->
                    val i = page.coerceIn(0, r.pageCount - 1)
                    r.openPage(i).use { p ->
                        val img = Bitmap.createBitmap((p.width * 1.4f).toInt().coerceAtLeast(1), (p.height * 1.4f).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        p.render(img, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp = img
                    }
                }
            }
        }.onFailure { error = it.message }
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        val img = bmp
        if (img != null) {
            Image(bitmap = img.asImageBitmap(), contentDescription = "صفحه پی‌دی‌اف", modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentScale = ContentScale.Fit)
        } else if (error == null) {
            CircularProgressIndicator(Modifier.padding(24.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(8.dp)) {
            TextButton(onClick = { if (page > 0) page -= 1 }, enabled = page > 0) { Text("قبلی") }
            Text("${page + 1} / ${count.coerceAtLeast(1)}")
            TextButton(onClick = { if (page + 1 < count) page += 1 }, enabled = page + 1 < count) { Text("بعدی") }
        }
    }
}

@Composable
internal fun AudioPlayer(bytes: ByteArray, fileId: String, cacheDir: File) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var pos by remember { mutableFloatStateOf(0f) }
    var dur by remember { mutableFloatStateOf(1f) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(fileId, bytes) {
        val f = File(cacheDir, "admin-preview-$fileId.bin")
        val mp = MediaPlayer()
        try {
            f.writeBytes(bytes)
            mp.setDataSource(f.absolutePath)
            mp.setOnPreparedListener {
                dur = it.duration.coerceAtLeast(1).toFloat()
                ready = true
                it.start()
                playing = true
            }
            mp.setOnCompletionListener { playing = false }
            mp.setOnErrorListener { _, _, _ -> error = "پخش نشد."; true }
            mp.prepareAsync()
            player = mp
        } catch (t: Throwable) {
            error = t.message ?: "پخش نشد."
            mp.release()
        }
        onDispose {
            runCatching { mp.stop() }
            runCatching { mp.release() }
            runCatching { f.delete() }
            player = null
        }
    }
    LaunchedEffect(playing, player) {
        while (playing) {
            val p = player
            if (p != null) pos = p.currentPosition.toFloat()
            delay(200)
        }
    }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("پخش‌کننده داخلی", style = MaterialTheme.typography.titleMedium)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!ready && error == null) CircularProgressIndicator()
        Slider(
            value = if (dur <= 0f) 0f else (pos / dur).coerceIn(0f, 1f),
            onValueChange = { pos = it * dur },
            onValueChangeFinished = { player?.seekTo(pos.toInt()) },
            enabled = ready,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("${fmtMs(pos.toInt())} / ${fmtMs(dur.toInt())}")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = {
                    val p = player ?: return@TextButton
                    if (playing) { p.pause(); playing = false } else { p.start(); playing = true }
                },
                enabled = ready,
            ) { Text(if (playing) "مکث" else "پخش") }
            TextButton(onClick = { player?.seekTo(0); pos = 0f }, enabled = ready) { Text("از اول") }
        }
    }
}

private fun fmtMs(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
