package com.hamyareman.ir.ui.study

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * کش بایت‌های **رمزشدهٔ** درس‌ها روی دیسک.
 *
 * قانون سفت: روی دیسک فقط `HMK1` می‌نشیند. متن‌ساده هیچ‌وقت نوشته نمی‌شود؛
 * رمزگشایی فقط لحظهٔ تحویل به WebView و در حافظه انجام می‌شود (`HtmlCodec`).
 *
 * نکته: پروژه OkHttp ندارد، پس `HttpURLConnection` استفاده می‌شود. رفتار یکی است:
 * اول در `<key>.tmp` نوشته، بعد magic بررسی و تنها در صورت سالم بودن rename می‌شود،
 * تا فایل نیمه‌کاره یا صفحهٔ خطای سرور هرگز به‌جای درس کش نشود.
 */
object LessonCache {

    /** سقف رمزگشایی در حافظه. بزرگ‌ترین درس حدود ۱۴ مگابایت است. */
    const val MAX_DECRYPT_BYTES: Int = 32 * 1024 * 1024

    private const val DIR = "lessons"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 30_000

    /** قفل به‌ازای هر کلید: دانلود هم‌زمان یک URL فقط یک بار انجام می‌شود. */
    private val locks = ConcurrentHashMap<String, Any>()

    // ---------------------------------------------------------------- کلید

    /** URL بدون `#fragment` و بدون پارامترهای امضای `X-Amz-*`. */
    fun canonical(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return url
        if (uri.isOpaque) return url.substringBefore('#')
        val builder = uri.buildUpon().fragment(null).clearQuery()
        val names = runCatching { uri.queryParameterNames }.getOrNull().orEmpty()
        for (name in names) {
            if (name.startsWith("X-Amz-", ignoreCase = true)) continue
            for (value in uri.getQueryParameters(name)) builder.appendQueryParameter(name, value)
        }
        return builder.build().toString()
    }

    fun keyOf(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical(url).toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun dir(ctx: Context): File = File(ctx.filesDir, DIR).apply { mkdirs() }

    fun fileFor(ctx: Context, url: String): File = File(dir(ctx), keyOf(url) + ".bin")

    /** فقط برای نشانگر «دانلود شده» روی کاشی‌ها. */
    fun isCached(ctx: Context, url: String): Boolean {
        val f = fileFor(ctx, url)
        return f.exists() && f.length() > HtmlCodec.MIN_WRAPPED_BYTES
    }

    fun evict(ctx: Context, url: String) {
        runCatching { fileFor(ctx, url).delete() }
    }

    fun clearAll(ctx: Context) {
        runCatching { dir(ctx).listFiles()?.forEach { it.delete() } }
    }

    fun cachedBytes(ctx: Context): Long =
        runCatching { dir(ctx).listFiles()?.sumOf { it.length() } ?: 0L }.getOrDefault(0L)

    // ------------------------------------------------------------- دریافت

    /**
     * فایل رمزشده را برمی‌گرداند؛ اگر نبود دانلود می‌کند.
     * **همگام** است و باید روی نخ پس‌زمینه صدا زده شود (مثل `shouldInterceptRequest`).
     *
     * @return فایل سالم، یا `null` اگر دانلود نشد یا محتوای دریافتی معتبر نبود.
     */
    fun ensure(ctx: Context, url: String, onProgress: ((Int, Int) -> Unit)? = null): File? {
        val target = fileFor(ctx, url)
        if (target.exists() && target.length() > HtmlCodec.MIN_WRAPPED_BYTES) return target
        val lock = locks.getOrPut(keyOf(url)) { Any() }
        synchronized(lock) {
            // ممکن است نخ دیگری همین حالا تمامش کرده باشد.
            if (target.exists() && target.length() > HtmlCodec.MIN_WRAPPED_BYTES) return target
            return runCatching { download(url, target, onProgress) }.getOrNull()
        }
    }

    private fun download(url: String, target: File, onProgress: ((Int, Int) -> Unit)?): File? {
        val tmp = File(target.path + ".tmp")
        runCatching { tmp.delete() }
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
            }
            if (conn.responseCode !in 200..299) return null
            val total = conn.contentLength
            var done = 0
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                        done += n
                        onProgress?.invoke(done, total)
                    }
                    output.flush()
                }
            }
            // اعتبارسنجی پیش از rename: باید واقعاً یک فایل HMK1 سالم باشد.
            if (!looksWrapped(tmp)) {
                tmp.delete()
                return null
            }
            runCatching { target.delete() }
            return if (tmp.renameTo(target)) target else { tmp.delete(); null }
        } catch (_: Throwable) {
            runCatching { tmp.delete() }
            return null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    /** چهار بایت اول فایل را می‌خواند و با magic می‌سنجد. */
    private fun looksWrapped(file: File): Boolean {
        if (!file.exists() || file.length() <= HtmlCodec.MIN_WRAPPED_BYTES) return false
        return runCatching {
            file.inputStream().use { input ->
                val head = ByteArray(4)
                if (input.read(head) != 4) return false
                HtmlCodec.hasMagic(head)
            }
        }.getOrDefault(false)
    }
}
