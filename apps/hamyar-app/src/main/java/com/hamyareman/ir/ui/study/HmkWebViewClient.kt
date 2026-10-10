package com.hamyareman.ir.ui.study

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection

/**
 * Serves the current bytes at the bucket URL without assuming that the payload
 * is encrypted. HMK1 is decrypted in memory; valid ordinary HTML is delivered
 * unchanged. Both formats keep the same URL/origin for relative links and frames.
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
        val isHtml = path.endsWith(".html", ignoreCase = true) ||
            path.endsWith(".htm", ignoreCase = true)
        val isMusic = path.contains("background-music", ignoreCase = true)

        // HTML frames are documents too. Only the explicit prefetch header suppresses
        // rendering; normal iframe requests pass through the same freshness/type logic.
        val explicitPrefetch = request.requestHeaders["X-Hy-Prefetch"] == "1"
        if (isHtml && !request.isForMainFrame && explicitPrefetch && !isMusic) {
            LessonCache.ensure(appContext, url)
            return empty200()
        }

        val prepared = LessonCache.prepare(appContext, url)
        if (prepared == null) {
            if (request.isForMainFrame && isHtml) {
                lastMessage = MSG_OFFLINE
                onProblem(Problem.OFFLINE)
                return errorPage(MSG_OFFLINE)
            }
            // Let WebView make its ordinary request for non-HTML resources or a
            // subframe when no validated cache payload could be prepared.
            return null
        }

        val wasCached = prepared.freshness == LessonCache.Freshness.CACHED
        if (request.isForMainFrame && isHtml) mainDocumentFromCache = wasCached
        val file = prepared.file
        val raw = runCatching { file.readBytes() }.getOrNull()
            ?: return if (request.isForMainFrame && isHtml) errorPage(MSG_CORRUPT) else null

        // Dispatch by the current file's signature, not the previous cache version.
        if (!HtmlCodec.hasMagic(raw)) {
            if (isHtml) {
                if (!HtmlCodec.isPlainHtml(raw)) {
                    lastMessage = MSG_CORRUPT
                    onProblem(Problem.CORRUPT)
                    return if (request.isForMainFrame) errorPage(MSG_CORRUPT) else null
                }
                val delivered = if (isMusic && wasCached) musicCacheHint(raw) else raw
                return WebResourceResponse(
                    "text/html", "utf-8", 200, "OK", htmlHeaders(), ByteArrayInputStream(delivered),
                )
            }
            val mime = URLConnection.guessContentTypeFromName(path) ?: "application/octet-stream"
            return WebResourceResponse(
                mime, null, 200, "OK", passthroughHeaders(), ByteArrayInputStream(raw),
            )
        }

        if (raw.size > LessonCache.MAX_DECRYPT_BYTES) {
            return if (request.isForMainFrame) errorPage(MSG_TOO_BIG) else null
        }

        val plain = decryptTwice(url, file, raw)
        if (plain == null) {
            return if (request.isForMainFrame) errorPage(lastMessage) else null
        }
        val delivered = if (isMusic && wasCached) musicCacheHint(plain) else plain
        return WebResourceResponse(
            "text/html", "utf-8", 200, "OK", htmlHeaders(), ByteArrayInputStream(delivered),
        )
    }

    /**
     * If the cached envelope fails authentication, download a fresh candidate into
     * a temporary file while preserving the previous cache until the candidate is
     * validated. A replacement may now be plain HTML, so classify it again.
     */
    private fun decryptTwice(url: String, file: File, first: ByteArray): ByteArray? {
        runCatching { HtmlCodec.unwrap(appContext, first) }.onSuccess { return it }

        if (HtmlMediaKey.get(appContext) == null) {
            lastMessage = MSG_NO_KEY
            onProblem(Problem.KEY_MISSING)
            return null
        }

        val refreshed = LessonCache.refresh(appContext, url)?.file ?: file
        val bytes = runCatching { refreshed.readBytes() }.getOrNull()
        if (bytes != null && bytes.size <= LessonCache.MAX_DECRYPT_BYTES) {
            if (!HtmlCodec.hasMagic(bytes) && HtmlCodec.isPlainHtml(bytes)) return bytes
            runCatching { HtmlCodec.unwrap(appContext, bytes) }.onSuccess { return it }
        }
        lastMessage = MSG_CORRUPT
        onProblem(Problem.CORRUPT)
        return null
    }

    @Volatile private var lastMessage: String = MSG_CORRUPT
    @Volatile private var mainDocumentFromCache: Boolean = false

    fun mainDocumentWasLoadedFromCache(): Boolean = mainDocumentFromCache

    private fun empty200(): WebResourceResponse =
        WebResourceResponse(
            "text/plain", "utf-8", 200, "OK",
            mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0)),
        )

    /** Add the local cache marker inside head so doctype/layout stay unchanged. */
    private fun musicCacheHint(plain: ByteArray): ByteArray {
        val hint = "<script>window.__hamyarHmkCacheHit=true;</script>".toByteArray(Charsets.UTF_8)
        val needle = "<head>".toByteArray(Charsets.UTF_8)
        val limit = minOf(plain.size - needle.size, 4096)
        var at = -1
        var i = 0
        while (i <= limit) {
            var match = true
            for (j in needle.indices) {
                if (plain[i + j] != needle[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                at = i + needle.size
                break
            }
            i++
        }
        if (at < 0) return plain
        return ByteArray(plain.size + hint.size).also { output ->
            plain.copyInto(output, 0, 0, at)
            hint.copyInto(output, at)
            plain.copyInto(output, at + hint.size, at, plain.size)
        }
    }

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

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        view.installMusicFrameOverlayBridge()
    }

    companion object {
        const val MSG_OFFLINE = "برای بار اول باز کردن این درس به اینترنت نیاز است."
        const val MSG_NO_KEY = "کلید دسترسی در دسترس نیست. دوباره وارد حساب شو."
        const val MSG_CORRUPT = "فایل درس خراب است. دوباره تلاش کن."
        const val MSG_TOO_BIG = "این فایل برای باز شدن روی این دستگاه خیلی بزرگ است."

        const val BUCKET_BASE = "https://c539776.parspack.net"
        const val BUCKET_HOST = "c539776.parspack.net"

        fun bucketHost(): String = BUCKET_HOST

        fun bucketUrl(key: String): String =
            BUCKET_BASE + "/" + key.trimStart('/').replace(" ", "%20")
    }
}
