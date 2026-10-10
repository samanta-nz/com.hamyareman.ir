package com.hamyareman.ir.ui.study

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * کش بایت‌های **رمزشدهٔ** HTML روی دیسک.
 *
 * روی دیسک فقط HMK1 می‌نشیند. متن ساده هیچ‌وقت به‌عنوان کش ماندگار ذخیره نمی‌شود؛
 * رمزگشایی فقط هنگام تحویل به WebView و در حافظه انجام می‌شود.
 *
 * قرارداد محتوا: هر فایل یک آدرس ثابت روی باکت دارد. کافی است فایل جدید دقیقاً در
 * همان آدرس آپلود شود؛ اپ بدون APK جدید تغییر را تشخیص می‌دهد و فایل تازه را می‌گیرد.
 *
 * برای تشخیص تغییر محتوا، قبل از هر نمایشِ HTML:
 * ۱) متادیتای سبک HTTP (ETag/Last-Modified/Length) بررسی می‌شود. اگر ETag هر دو طرف
 *    موجود باشد، همان تصمیم قطعی است (حتی وقتی طول فایل برابر باشد).
 * ۲) اگر برای کش قدیمی متادیتا نداشتیم، ابتدا و انتهای فایل با Range کوچک مقایسه می‌شود
 *    (IV تازهٔ هر رمزنگاری در ابتدای فایل است، پس بازآپلودِ هم‌طول هم دیده می‌شود).
 * ۳) اگر محتوا تغییر کرده باشد، فایل تازه کامل دانلود و اتمیک جایگزین می‌شود.
 * ۴) اگر دانلودِ نسخهٔ تازه شکست بخورد، نسخهٔ سالم قبلی نمایش داده می‌شود.
 */
object LessonCache {

    const val MAX_DECRYPT_BYTES: Int = 32 * 1024 * 1024

    enum class Freshness {
        CACHED,
        UPDATED,
        DOWNLOADED,
    }

    data class PrepareResult(
        val file: File,
        val freshness: Freshness,
    )

    private data class RemoteMeta(
        val contentLength: Long,
        val etag: String,
        val lastModified: Long,
    )

    private const val DIR = "lessons"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val PROBE_BYTES = 8 * 1024
    private const val FRESH_CHECK_TTL_MS = 30_000L

    private val locks = ConcurrentHashMap<String, Any>()
    private val recentChecks = ConcurrentHashMap<String, Long>()

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
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical(url).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) } 
    }

    private fun dir(ctx: Context): File = File(ctx.filesDir, DIR).apply { mkdirs() }

    fun fileFor(ctx: Context, url: String): File =
        File(dir(ctx), keyOf(url) + ".bin")

    private fun metaFor(ctx: Context, url: String): File =
        File(dir(ctx), keyOf(url) + ".meta")

    fun isCached(ctx: Context, url: String): Boolean {
        val f = fileFor(ctx, url)
        return f.exists() && f.length() > HtmlCodec.MIN_WRAPPED_BYTES && looksWrapped(f)
    }

    fun evict(ctx: Context, url: String) {
        val key = keyOf(url)
        recentChecks.remove(key)
        runCatching { fileFor(ctx, url).delete() }
        runCatching { metaFor(ctx, url).delete() }
    }

    fun clearAll(ctx: Context) {
        recentChecks.clear()
        runCatching { dir(ctx).listFiles()?.forEach { it.delete() } }
    }

    fun cachedBytes(ctx: Context): Long =
        runCatching { dir(ctx).listFiles()?.filter { it.extension == "bin" }?.sumOf { it.length() } ?: 0L }
            .getOrDefault(0L)

    /**
     * فقط وقتی `true` است که باکت صریحاً بگوید فایل وجود ندارد (۴۰۴/۴۱۰/۴۰۳).
     * خطای شبکه یا آفلاین بودن `false` می‌دهد، تا «آفلاین» با «هنوز آپلود نشده» قاطی نشود.
     * همگام است؛ فقط روی نخ پس‌زمینه صدا زده شود.
     */
    fun isRemoteMissing(url: String): Boolean = runCatching {
        val conn = (URL(canonical(url)).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "HEAD"
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            val code = conn.responseCode
            code == 404 || code == 410 || code == 403
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(false)

    fun prepare(
        ctx: Context,
        url: String,
        onProgress: ((Int, Int) -> Unit)? = null,
        onStatus: ((Freshness) -> Unit)? = null,
    ): PrepareResult? {
        val canonicalUrl = canonical(url)
        val target = fileFor(ctx, canonicalUrl)
        val key = keyOf(canonicalUrl)
        val lock = locks.getOrPut(key) { Any() }

        synchronized(lock) {
            val hasCache = isCached(ctx, canonicalUrl)
            if (hasCache) {
                val now = System.currentTimeMillis()
                if (now - (recentChecks[key] ?: 0L) < FRESH_CHECK_TTL_MS) {
                    onStatus?.invoke(Freshness.CACHED)
                    return PrepareResult(target, Freshness.CACHED)
                }

                val remote = fetchMeta(canonicalUrl)
                if (remote == null) {
                    // آفلاین یا فایل از باکت برداشته شده: نسخهٔ سالم قبلی نمایش داده می‌شود.
                    recentChecks[key] = now
                    return PrepareResult(target, Freshness.CACHED)
                }

                if (isSameContent(ctx, canonicalUrl, target, remote)) {
                    writeMeta(ctx, canonicalUrl, remote)
                    recentChecks[key] = now
                    onStatus?.invoke(Freshness.CACHED)
                    return PrepareResult(target, Freshness.CACHED)
                }

                onStatus?.invoke(Freshness.UPDATED)
                val downloaded = runCatching {
                    download(canonicalUrl, target, onProgress)
                }.getOrNull()

                if (downloaded == null) {
                    // دانلود نسخهٔ تازه شکست خورد. فایل قبلی دست‌نخورده مانده
                    // (جایگزینی فقط پس از اعتبارسنجی انجام می‌شود)، پس همان نمایش داده
                    // می‌شود. recentChecks عمداً ثبت نمی‌شود تا بار بعد دوباره تلاش شود.
                    return if (isCached(ctx, canonicalUrl)) {
                        PrepareResult(target, Freshness.CACHED)
                    } else {
                        null
                    }
                }

                writeMeta(ctx, canonicalUrl, remote)
                recentChecks[key] = now
                return PrepareResult(downloaded, Freshness.UPDATED)
            }

            onStatus?.invoke(Freshness.DOWNLOADED)
            // متادیتا پیش از دانلود گرفته می‌شود: اگر وسط کار فایل عوض شود، متادیتای
            // قدیمی‌تر ثبت می‌شود و بررسی بعدی تغییر را می‌بیند (نه برعکس).
            val remoteBefore = fetchMeta(canonicalUrl)
            val downloaded = runCatching {
                download(canonicalUrl, target, onProgress)
            }.getOrNull() ?: return null

            remoteBefore?.let { writeMeta(ctx, canonicalUrl, it) }
            recentChecks[key] = System.currentTimeMillis()
            return PrepareResult(downloaded, Freshness.DOWNLOADED)
        }
    }

    fun ensure(ctx: Context, url: String, onProgress: ((Int, Int) -> Unit)? = null): File? =
        prepare(ctx, url, onProgress)?.file

    private fun fetchMeta(url: String): RemoteMeta? {
        val head = runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "HEAD"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                if (conn.responseCode !in 200..299) null
                else RemoteMeta(
                    contentLength = conn.contentLengthLong,
                    etag = conn.getHeaderField("ETag").orEmpty(),
                    lastModified = conn.lastModified,
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()

        if (head != null) return head

        // اگر HEAD رد شد، یک GET با Range کوچک طول واقعی را از Content-Range می‌گیرد.
        return runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Range", "bytes=0-" + (PROBE_BYTES - 1))
            }
            try {
                if (conn.responseCode !in 200..299) return null
                val range = conn.getHeaderField("Content-Range").orEmpty()
                val total = range.substringAfterLast("/", "").toLongOrNull()
                    ?: conn.contentLengthLong.takeIf { it >= 0L }
                    ?: return null
                conn.inputStream.use { input ->
                    val buffer = ByteArray(PROBE_BYTES)
                    input.read(buffer)
                }
                RemoteMeta(total, "", 0L)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun isSameContent(
        ctx: Context,
        url: String,
        local: File,
        remote: RemoteMeta,
    ): Boolean {
        // طول متفاوت یعنی قطعاً محتوای دیگر.
        if (remote.contentLength >= 0 && remote.contentLength != local.length()) return false

        val meta = readMeta(ctx, url)
        if (meta != null) {
            // ETag هر دو طرف موجود: تصمیم قطعی. ETag متفاوت = فایل بازآپلود شده،
            // حتی اگر طول دقیقاً برابر باشد.
            if (meta.etag.isNotBlank() && remote.etag.isNotBlank()) {
                return meta.etag == remote.etag
            }
            if (meta.lastModified > 0 && remote.lastModified > 0) {
                return meta.lastModified == remote.lastModified
            }
        }

        // متادیتای قابل‌اتکا نداریم (کش قدیمی یا سرور بدون ETag): ابتدا و انتها مقایسه شود.
        return compareRange(url, local, 0L, PROBE_BYTES) &&
            compareRange(
                url,
                local,
                (local.length() - PROBE_BYTES).coerceAtLeast(0L),
                PROBE_BYTES,
            )
    }

    private fun compareRange(url: String, local: File, start: Long, requested: Int): Boolean {
        if (requested <= 0 || !local.exists() || local.length() < start) return false
        val length = minOf(requested.toLong(), local.length() - start).toInt()
        if (length <= 0) return false

        val remoteBytes = runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty(
                    "Range",
                    "bytes=" + start + "-" + (start + length - 1),
                )
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_PARTIAL) return false
                conn.inputStream.use { input ->
                    val bytes = ByteArray(length)
                    var off = 0
                    while (off < length) {
                        val n = input.read(bytes, off, length - off)
                        if (n <= 0) break
                        off += n
                    }
                    if (off != length) return false
                    bytes
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return false

        val localBytes = ByteArray(length)
        val read = runCatching {
            java.io.RandomAccessFile(local, "r").use {
                it.seek(start)
                it.read(localBytes)
            }
        }.getOrDefault(-1)
        return read == length && localBytes.contentEquals(remoteBytes)
    }

    private fun readMeta(ctx: Context, url: String): RemoteMeta? =
        runCatching {
            val p = Properties()
            metaFor(ctx, url).inputStream().use(p::load)
            RemoteMeta(
                contentLength = p.getProperty("length", "-1").toLong(),
                etag = p.getProperty("etag", ""),
                lastModified = p.getProperty("lastModified", "0").toLong(),
            )
        }.getOrNull()

    private fun writeMeta(ctx: Context, url: String, remote: RemoteMeta) {
        runCatching {
            val meta = metaFor(ctx, url)
            val tmp = File(meta.absolutePath + ".tmp")
            val p = Properties()
            p.setProperty("length", remote.contentLength.toString())
            p.setProperty("etag", remote.etag)
            p.setProperty("lastModified", remote.lastModified.toString())
            tmp.outputStream().use { p.store(it, null) }
            meta.delete()
            tmp.renameTo(meta)
        }
    }

    private fun download(
        url: String,
        target: File,
        onProgress: ((Int, Int) -> Unit)?,
    ): File? {
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
                setRequestProperty("Cache-Control", "no-cache")
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

            if (!looksWrapped(tmp)) {
                tmp.delete()
                return null
            }

            // rename روی همان پارتیشن، فایل قبلی را اتمیک جایگزین می‌کند؛ پس فایل
            // قدیمی پیش از آماده شدن جدید پاک نمی‌شود. delete فقط یدک است.
            if (tmp.renameTo(target)) return target
            runCatching { target.delete() }
            return if (tmp.renameTo(target)) target else {
                tmp.delete()
                null
            }
        } catch (_: Throwable) {
            runCatching { tmp.delete() }
            return null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

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
