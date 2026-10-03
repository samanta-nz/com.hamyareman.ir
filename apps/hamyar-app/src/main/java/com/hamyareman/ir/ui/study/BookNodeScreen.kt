package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import androidx.compose.ui.viewinterop.AndroidView
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
/**
 * تمام‌صفحه: نوار عنوان و فلش برگشت عمداً نیست — طبق خواستهٔ طراحی فقط پلیر
 * جمع‌شونده و منوی پایین دیده می‌شوند. برگشت با دکمهٔ back دستگاه.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BookNodeScreen(
    title: String,
    bucketKey: String,
    audioKey: String = "",
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        if (audioKey.isNotBlank()) {
            val packId = remember(audioKey) { "book-" + StudyMedia.bookCacheKey("track", audioKey) }
            val cacheKey = remember(audioKey) { StudyMedia.bookCacheKey("book-audio", audioKey) }
            // برخلاف نسخهٔ قبل، کلید کامل باکت پاس داده می‌شود؛ basename در
            // server-map نبود و پلیر صوت‌های تدریس ریاضی را «غایب» می‌دید.
            TeachAudioBar(
                packId = packId,
                screenTitle = title,
                bookTitle = "",
                tracks = listOf(TeachTrack("صوت تدریس", audioKey, cacheKey)),
            )
        }
        if (BooksMenu.isPdf(bucketKey)) BookPdfPages(bucketKey) else RemoteHtmlPage(bucketKey)
    }
}

/** هر HTML روی باکت — صفحهٔ تدریس آماده یا همان اسپیس‌هولدر مشترک. */
@Composable
private fun RemoteHtmlPage(bucketKey: String) {
    SecureWebEffect()
    val appearance = LocalUiPrefs.current
    AndroidView(
        factory = { context ->
            ZoomResetWebView(context).apply {
                installHamyarAppearanceBridge(appearance)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = object : HmkWebViewClient(
                    context.applicationContext,
                    HmkWebViewClient.bucketHost(),
                ) {
                    override fun onPageFinished(view: android.webkit.WebView, url: String) {
                        super.onPageFinished(view, url)
                        view.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
                    }
                }
                loadUrl(ServerResolver.internal(bucketKey.ifBlank { BooksMenu.SPACEHOLDER_KEY }))
            }
        },
        update = { it.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme) },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.destroy() },
    )
}

private sealed interface BookPdfState {
    data object Loading : BookPdfState
    data class Ready(val pages: Int) : BookPdfState
    data class Failed(val message: String) : BookPdfState
}

@Composable
private fun BookPdfPages(bucketKey: String) {
    val context = LocalContext.current
    // مسیر کامل، نه نام فایل: نام‌های یکسان در چند کتاب نباید cache یکدیگر را
    // بازنویسی کنند. DownloadsScreen هم دقیقاً همین کلید را استفاده می‌کند.
    val cacheId = remember(bucketKey) { StudyMedia.bookCacheKey("book-pdf", bucketKey) }
    var state by remember(cacheId) { mutableStateOf<BookPdfState>(BookPdfState.Loading) }
    var progress by remember(cacheId) { mutableIntStateOf(0) }
    var renderer by remember(cacheId) { mutableStateOf<PdfRenderer?>(null) }
    val lock = remember(cacheId) { Any() }
    var retry by remember(cacheId) { mutableIntStateOf(0) }

    DisposableEffect(cacheId) {
        onDispose {
            synchronized(lock) {
                runCatching { renderer?.close() }
                renderer = null
            }
        }
    }

    LaunchedEffect(cacheId, retry) {
        state = BookPdfState.Loading
        progress = 0
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val file = StudyPdfCache.obtain(
                    context,
                    cacheId,
                    StudyMedia.candidateUrls(bucketKey),
                ) { percent -> handler.post { progress = percent } }
                PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
            }
        }
        result.onSuccess { pdf ->
            synchronized(lock) { renderer = pdf }
            state = BookPdfState.Ready(pdf.pageCount)
        }.onFailure {
            state = BookPdfState.Failed(it.message?.take(180) ?: "دریافت این فایل ممکن نشد.")
        }
    }

    when (val current = state) {
        is BookPdfState.Loading -> Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) { HtmlPercentLoader(progress) }

        is BookPdfState.Failed -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(current.message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { retry++ }) { Text("تلاش دوباره") }
        }

        // PDF همیشه سفید است؛ تم تاریک اپ هرگز روی محتوای کتاب درسی نمی‌افتد.
        is BookPdfState.Ready -> BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items((0 until current.pages).toList(), key = { it }) { index ->
                    val bitmap = remember(cacheId, index) {
                        synchronized(lock) {
                            val pdf = renderer ?: return@synchronized null
                            runCatching {
                                pdf.openPage(index).use { page ->
                                    val width = 1080
                                    val height = (width.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { image ->
                                        image.eraseColor(android.graphics.Color.WHITE)
                                        page.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    }
                                }
                            }.getOrNull()
                        }
                    }
                    if (bitmap != null) {
                        val ratio = bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
                        // هر صفحه View مستقل دارد: pinch و پَن واقعی روی همان صفحه
                        // است و LazyColumn در اندازهٔ اصلی همچنان طبیعی اسکرول می‌شود.
                        ZoomablePdfPage(
                            bitmap = bitmap,
                            modifier = Modifier.fillMaxWidth().height(maxWidth * ratio),
                        )
                    }
                }
            }
        }
    }
}
