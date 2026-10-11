package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

/**
 * نمایش یک گره از منوی کتاب (v3.0.8).
 *
 * همهٔ فایل‌ها (HTML و PDF) از مسیر یکپارچهٔ [ContentHost] می‌آیند:
 *  - فایل روی باکت هست: دانلود با نوار پیشرفت، کش، و نمایش؛ HTML رمزشده (HMK1) فقط در RAM
 *    رمزگشایی می‌شود. اگر کش موجود باشد از کش نمایش داده و با ETag بررسی بروزرسانی می‌شود.
 *  - فایل روی باکت نیست (۴۰۴): تک‌فایل مشترک `spaceholder.html`.
 *  - اینترنت مشکل دارد: پیام بررسی اتصال + «تلاش دوباره».
 *
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
        val effectiveKey = bucketKey.ifBlank { BooksMenu.SPACEHOLDER_KEY }
        if (BooksMenu.isPdf(effectiveKey)) BookPdfPages(effectiveKey) else RemoteHtmlPage(effectiveKey, audioKey)
    }
}

/** هر HTML روی باکت — صفحهٔ تدریس آماده یا همان placeholder مشترک. */
@Composable
private fun RemoteHtmlPage(bucketKey: String, audioKey: String = "") {
    SecureWebEffect()
    val pageUrl = remember(bucketKey) { ServerResolver.internal(bucketKey) }
    ContentHost(url = pageUrl, kind = ContentKind.HTML) { ready ->
        // با هر جایگزینی محتوا (revision) WebView از نو ساخته می‌شود.
        key(ready.revision) {
            val baseUrl = if (ready.isPlaceholder) {
                ServerResolver.internal(BooksMenu.SPACEHOLDER_KEY)
            } else {
                pageUrl
            }
            HtmlWebView(
                html = String(ready.htmlBytes ?: ByteArray(0), Charsets.UTF_8),
                baseUrl = baseUrl,
                audioKey = audioKey,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlWebView(html: String, baseUrl: String, audioKey: String) {
    AndroidView(
        factory = { context ->
            ZoomResetWebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                if (audioKey.isNotBlank()) addJavascriptInterface(TeachHtmlBridge(), "HamyarPlayer")
                webViewClient = object : HmkWebViewClient(
                    context.applicationContext,
                    HmkWebViewClient.bucketHost(),
                ) {
                    override fun onPageFinished(view: android.webkit.WebView, url: String) {
                        super.onPageFinished(view, url)
                        if (audioKey.isNotBlank()) {
                            view.evaluateJavascript(
                                """
                                (function(){
                                  if(window.__hamyarSeekBound)return;
                                  window.__hamyarSeekBound=true;
                                  function bind(){
                                    document.querySelectorAll('[data-seek-ms]').forEach(function(el){
                                      if(el.dataset.hamyarSeekBound)return;
                                      el.dataset.hamyarSeekBound='1';
                                      el.addEventListener('click',function(ev){
                                        var ms=parseInt(this.getAttribute('data-seek-ms')||'',10);
                                        if(!Number.isFinite(ms)||ms<0)return;
                                        ev.preventDefault();ev.stopPropagation();
                                        if(window.HamyarPlayer&&window.HamyarPlayer.seek)window.HamyarPlayer.seek(ms);
                                      },false);
                                    });
                                  }
                                  bind();
                                  new MutationObserver(bind).observe(document.documentElement,{subtree:true,childList:true});
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                        // محتوای HTML کتاب «سند چاپی» است؛ عمداً از تم اپ مستقل است.
                    }
                }
                // HTML (ساده یا رمزگشایی‌شده در RAM) با مبدأ باکت بارگذاری می‌شود تا لینک‌های
                // نسبی (قبلی/بعدی، پلیر موسیقی) همچنان از همان پوشهٔ باکت و از طریق
                // HmkWebViewClient حل شوند.
                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
            }
        },
        update = { },
        modifier = Modifier.fillMaxSize(),
        onRelease = { it.destroy() },
    )
}

@Composable
private fun BookPdfPages(bucketKey: String) {
    val pdfUrl = remember(bucketKey) { ServerResolver.internal(bucketKey) }
    // PDF بدون رمزگذاری است؛ همان منطق دانلود/کش/ETag بدون مرحلهٔ رمزگشایی.
    ContentHost(url = pdfUrl, kind = ContentKind.PDF) { ready ->
        PdfPagesView(ready.file, ready.revision)
    }
}

@Composable
private fun PdfPagesView(file: File, revision: Int) {
    val lock = remember(file.path, revision) { Any() }
    val renderer = remember(file.path, revision) {
        runCatching {
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
        }.getOrNull()
    }
    DisposableEffect(renderer) {
        onDispose {
            synchronized(lock) { runCatching { renderer?.close() } }
        }
    }
    if (renderer == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("باز کردن این فایل ممکن نشد.", color = MaterialTheme.colorScheme.error)
        }
        return
    }
    val pages = remember(renderer) { renderer.pageCount }

    // PDF همیشه سفید است؛ تم تاریک اپ هرگز روی محتوای کتاب درسی نمی‌افتد.
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items((0 until pages).toList(), key = { it }) { index ->
                val bitmap = remember(file.path, revision, index) {
                    synchronized(lock) {
                        runCatching {
                            renderer.openPage(index).use { page ->
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
