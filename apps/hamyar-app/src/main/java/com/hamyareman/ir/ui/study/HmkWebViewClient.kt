package com.hamyareman.ir.ui.study

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection

/**
 * میانجیِ درخواست‌های WebView برای محتوای رمزشدهٔ باکت.
 *
 * چرا این‌طور: WebView باید روی **آدرس واقعی باکت** باشد تا origin ثابت بماند و
 * لینک نسبی «قبلی/بعدی»، `iframe` موسیقی و `localStorage` کار کنند. ولی بایت‌هایی
 * که از باکت می‌آیند `HMK1` هستند و مرورگر نمی‌فهمدشان. پس اینجا وسط راه:
 * بایت رمزشده از [LessonCache] خوانده، در حافظه رمزگشایی و متن‌ساده تحویل می‌شود.
 *
 * نکتهٔ تشخیص پیش‌دانلود: هدر `X-Hy-Prefetch` در هیچ‌کدام از HTMLهای فعلی وجود
 * ندارد (بررسی شد: صفر مورد در ۷۴ فایل) و قرار است HTMLها دست‌نخورده بمانند.
 * پس پیش‌دانلود از روی «main-frame نبودن + پسوند html + نبودن نام موسیقی»
 * تشخیص داده می‌شود؛ همان اثر را بدون تغییر محتوا می‌دهد.
 */
open class HmkWebViewClient(
    private val appContext: Context,
    private val bucketHost: String,
    private val onProblem: (Problem) -> Unit = {},
) : WebViewClient() {

    enum class Problem { KEY_MISSING, CORRUPT, OFFLINE }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        if (!"GET".equals(request.method, ignoreCase = true)) return null
        val uri = request.url ?: return null
        if (!bucketHost.equals(uri.host, ignoreCase = true)) return null

        val url = uri.toString()
        val path = uri.path.orEmpty()
        val isHtml = path.endsWith(".html", ignoreCase = true)
        val isMusic = path.contains("background-music", ignoreCase = true)

        // ۳) پیش‌دانلود درس بعدی: فقط کش کن، رمزگشایی نکن.
        if (isHtml && !request.isForMainFrame && !isMusic) {
            LessonCache.ensure(appContext, url)
            return empty200()
        }

        val file = LessonCache.ensure(appContext, url)
        if (file == null) {
            onProblem(Problem.OFFLINE)
            return if (request.isForMainFrame) errorPage(MSG_OFFLINE) else null
        }

        val raw = runCatching { file.readBytes() }.getOrNull()
            ?: return if (request.isForMainFrame) errorPage(MSG_CORRUPT) else null

        // ۲-۲) غیررمزی (تصویر، فونت، …): همان‌طور عبور بده، با MIME حدس‌زده از نام.
        if (!HtmlCodec.hasMagic(raw)) {
            val mime = URLConnection.guessContentTypeFromName(path) ?: "application/octet-stream"
            return WebResourceResponse(mime, null, 200, "OK", passthroughHeaders(), ByteArrayInputStream(raw))
        }

        if (raw.size > LessonCache.MAX_DECRYPT_BYTES) {
            return if (request.isForMainFrame) errorPage(MSG_TOO_BIG) else null
        }

        val plain = decryptTwice(url, file, raw)
        if (plain == null) {
            return if (request.isForMainFrame) errorPage(lastMessage) else null
        }
        return WebResourceResponse(
            "text/html", "utf-8", 200, "OK", htmlHeaders(), ByteArrayInputStream(plain),
        )
    }

    /** یک بار رمزگشایی؛ اگر شکست خورد کش را دور بینداز، یک بار دیگر دانلود و تلاش کن. */
    private fun decryptTwice(url: String, file: File, first: ByteArray): ByteArray? {
        runCatching { HtmlCodec.unwrap(appContext, first) }.onSuccess { return it }

        if (HtmlMediaKey.get(appContext) == null) {
            lastMessage = MSG_NO_KEY
            onProblem(Problem.KEY_MISSING)
            return null // دانلود تکراری راه نمی‌اندازیم؛ مشکل کلید است نه فایل.
        }

        LessonCache.evict(appContext, url)
        val again = LessonCache.ensure(appContext, url) ?: run {
            lastMessage = MSG_OFFLINE
            onProblem(Problem.OFFLINE)
            return null
        }
        val bytes = runCatching { again.readBytes() }.getOrNull()
        if (bytes != null && bytes.size <= LessonCache.MAX_DECRYPT_BYTES) {
            runCatching { HtmlCodec.unwrap(appContext, bytes) }.onSuccess { return it }
        }
        lastMessage = MSG_CORRUPT
        onProblem(Problem.CORRUPT)
        return null
    }

    @Volatile private var lastMessage: String = MSG_CORRUPT

    private fun empty200(): WebResourceResponse =
        WebResourceResponse(
            "text/plain", "utf-8", 200, "OK",
            mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0)),
        )

    private fun htmlHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "text/html; charset=utf-8",
        "Cache-Control" to "no-store",
    )

    private fun passthroughHeaders(): Map<String, String> = mapOf("Cache-Control" to "no-store")

    private fun errorPage(message: String): WebResourceResponse {
        val body = """
            <!doctype html><html lang="fa" dir="rtl"><meta charset="utf-8">
            <style>body{background:#101014;color:#e8e8ee;font-family:sans-serif;
            display:flex;align-items:center;justify-content:center;height:100vh;margin:0;text-align:center}
            p{font-size:17px;line-height:2;padding:24px}</style><p>$message</p></html>
        """.trimIndent().toByteArray()
        return WebResourceResponse(
            "text/html", "utf-8", 200, "OK", htmlHeaders(), ByteArrayInputStream(body),
        )
    }

    companion object {
        const val MSG_OFFLINE = "برای بار اول باز کردن این درس به اینترنت نیاز است."
        const val MSG_NO_KEY = "کلید دسترسی در دسترس نیست. دوباره وارد حساب شو."
        const val MSG_CORRUPT = "فایل درس خراب است. دوباره تلاش کن."
        const val MSG_TOO_BIG = "این فایل برای باز شدن روی این دستگاه خیلی بزرگ است."

        /** هاست باکت داخلی، از روی همان ثابت [ServerResolver.INTERNAL_PUBLIC]. */
        fun bucketHost(): String =
            runCatching { Uri.parse(ServerResolver.INTERNAL_PUBLIC).host.orEmpty() }.getOrDefault("")
    }
}
