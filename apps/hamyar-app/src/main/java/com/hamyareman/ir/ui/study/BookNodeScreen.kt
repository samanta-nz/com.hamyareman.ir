package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * نمایش یک گره از منوی کتاب.
 *
 *  - اگر فایلِ خودش روی باکت آماده باشد: همان PDF صفحه‌به‌صفحه رندر می‌شود.
 *  - وگرنه: تک‌فایل مشترک «در دست تولید» (`spaceholder.html`) باز می‌شود.
 *
 * PDFها روی باکت رمز نیستند، پس از مسیر [StudyPdfCache] می‌آیند؛ اسپیس‌هولدر
 * HTML رمزشده است و از میانجی [HmkWebViewClient] عبور می‌کند.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BookNodeScreen(title: String, bucketKey: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title.ifBlank { "درس" }, onBack)
        if (BooksMenu.isPdf(bucketKey)) {
            BookPdfPages(bucketKey)
        } else {
            SpaceholderPage()
        }
    }
}

@Composable
private fun SpaceholderPage() {
    SecureWebEffect()
    AndroidView(
        factory = { c ->
            WebView(c).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = HmkWebViewClient(c.applicationContext, HmkWebViewClient.bucketHost())
                loadUrl(ServerResolver.internal(BooksMenu.SPACEHOLDER_KEY))
            }
        },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.destroy() },
    )
}

private sealed interface PdfState {
    data object Loading : PdfState
    data class Ready(val pages: Int) : PdfState
    data class Failed(val message: String) : PdfState
}

@Composable
private fun BookPdfPages(bucketKey: String) {
    val ctx = LocalContext.current
    val fileId = remember(bucketKey) { bucketKey.substringAfterLast('/') }
    var state by remember(fileId) { mutableStateOf<PdfState>(PdfState.Loading) }
    var progress by remember(fileId) { mutableStateOf(0) }
    var renderer by remember(fileId) { mutableStateOf<PdfRenderer?>(null) }
    val lock = remember(fileId) { Any() }
    var retry by remember(fileId) { mutableStateOf(0) }

    DisposableEffect(fileId) {
        onDispose {
            synchronized(lock) {
                runCatching { renderer?.close() }
                renderer = null
            }
        }
    }

    LaunchedEffect(fileId, retry) {
        state = PdfState.Loading
        progress = 0
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = StudyPdfCache.obtain(ctx, fileId) { p -> handler.post { progress = p } }
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(fd)
            }
        }
        result.onSuccess { r ->
            synchronized(lock) { renderer = r }
            state = PdfState.Ready(r.pageCount)
        }.onFailure {
            state = PdfState.Failed(it.message?.take(180) ?: "دریافت این فایل ممکن نشد.")
        }
    }

    when (val s = state) {
        is PdfState.Loading -> Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) { HtmlPercentLoader(progress) }

        is PdfState.Failed -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(s.message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { retry++ }) { Text("تلاش دوباره") }
        }

        is PdfState.Ready -> LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items((0 until s.pages).toList()) { index ->
                val bitmap = remember(fileId, index) {
                    synchronized(lock) {
                        val r = renderer ?: return@synchronized null
                        runCatching {
                            r.openPage(index).use { page ->
                                val width = 1080
                                val height = (width.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bmp
                            }
                        }.getOrNull()
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "صفحهٔ ${index + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }
    }
}
