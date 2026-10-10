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
 * کش بایت‌های HTML باکت روی دیسک.
 *
 * دو قالب پذیرفته می‌شود و از روی خود فایل تشخیص داده می‌شود:
 *  - HMK1 (رمزشده): همان‌طور رمزشده روی دیسک می‌ماند و فقط هنگام تحویل به WebView
 *    در حافظه رمزگشایی می‌شود.
 *  - HTML ساده (فقط برای آدرس‌های `.html`): همان‌طور ذخیره و تحویل می‌شود.
 * فایل‌های غیر HTML (تصویر، فونت و…) مثل قبل وارد این کش نمی‌شوند.
 *
 * قرارداد محتوا: هر فایل یک آدرس ثابت روی باکت دارد. کافی است فایل جدید دقیقاً در
 * همان آدرس آپلود شود؛ اپ بدون APK جدید تغییر را تشخیص می‌دهد و فایل تازه را می‌گیرد.
 *
 * تشخیص تغییر با **یک** درخواست شرطی HTTP (Conditional GET) انجام می‌شود:
 * ۱) اگر کش داریم، GET با `If-None-Match: <ETag ذخیره‌شده>` (یا `If-Modified-Since`) زده می‌شود.
 * ۲) پاسخ ۳۰۴: بدنه‌ای دریافت نمی‌شود (فقط هدر)؛ همان کش نمایش داده می‌شود.
 * ۳) پاسخ ۲۰۰: همین پاسخ بدنهٔ تازه را دارد؛ اگر ETag با ذخیره‌شده برابر بود (سرور شرط را
 *    نادیده گرفته) اتصال بدون خواندن بدنه بسته می‌شود، وگرنه بدنه در .tmp ذخیره، اعتبارسنجی
 *    و اتمیک جایگزین می‌شود. ETag از همین پاسخ ثبت می‌شود (نه از یک HEAD جداگانه).
 * ۴) بدون کش: یک GET ساده؛ متادیتا از همان پاسخ ثبت می‌شود (بدون HEAD اضافه).
 * ۵) خطای شبکه/۴xx/۵xx با کش سالم: همان کش نمایش داده می‌شود و بار بعد دوباره بررسی می‌شود.
 * ۶) سرور بدون ETag و Last-Modified: ابتدا و انتهای فایل با Range کوچک مقایسه می‌شود.
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
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val PROBE_BYTES = 8 * 1024

    /** جلوگیری از درخواست تکراری وقتی پیش‌بررسی صفحه و WebView هم‌زمان همان آدرس را می‌خواهند. */
    private const val DEDUPE_MS = 4_000L

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
        if (!f.exists()) return false
        if (f.length() > HtmlCodec.MIN_WRAPPED_BYTES && looksWrapped(f)) return true
        return acceptsPlain(url) && looksLikeHtml(f)
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
            useCaches = false
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
        val key = keyOf(canonicalUrl)
        val lock = locks.getOrPut(key) { Any() }
        return synchronized(lock) {
            prepareLocked(ctx, canonicalUrl, key, onProgress, onStatus)
        }
    }

    fun ensure(ctx: Context, url: String, onProgress: ((Int, Int) -> Unit)? = null): File? =
        prepare(ctx, url, onProgress)?.file

    private fun prepareLocked(
        ctx: Context,
        canonicalUrl: String,
        key: String,
        onProgress: ((Int, Int) -> Unit)?,
        onStatus: ((Freshness) -> Unit)?,
    ): PrepareResult? {
        val target = fileFor(ctx, canonicalUrl)
        val hasCache = isCached(ctx, canonicalUrl)
        val now = System.currentTimeMillis()

        if (hasCache && now - (recentChecks[key] ?: 0L) < DEDUPE_MS) {
            onStatus?.invoke(Freshness.CACHED)
            return PrepareResult(target, Freshness.CACHED)
        }

        val stored = if (hasCache) readMeta(ctx, canonicalUrl) else null
        var conn: HttpURLConnection? = null
        try {
            conn = openGet(canonicalUrl, stored)
            val code = conn.responseCode

            // تغییری نکرده: بدون دریافت بدنه.
            if (code == HttpURLConnection.HTTP_NOT_MODIFIED && hasCache) {
                recentChecks[key] = now
                onStatus?.invoke(Freshness.CACHED)
                return PrepareResult(target, Freshness.CACHED)
            }

            // ۴۰۴/۴۰۳/۵xx و مانند آن: نسخهٔ سالم قبلی می‌ماند؛ recentChecks ثبت نمی‌شود
            // تا بار بعد دوباره بررسی شود.
            if (code !in 200..299) {
                if (!hasCache) return null
                onStatus?.invoke(Freshness.CACHED)
                return PrepareResult(target, Freshness.CACHED)
            }

            val remote = RemoteMeta(
                contentLength = conn.contentLengthLong,
                etag = conn.getHeaderField("ETag").orEmpty(),
                lastModified = conn.lastModified,
            )

            // سرور شرط را نادیده گرفته ولی محتوا همان است: اتصال بدون خواندن بدنه بسته می‌شود.
            if (hasCache && isUnchanged(canonicalUrl, target, stored, remote)) {
                writeMeta(ctx, canonicalUrl, remote)
                recentChecks[key] = now
                onStatus?.invoke(Freshness.CACHED)
                return PrepareResult(target, Freshness.CACHED)
            }

            val freshness = if (hasCache) Freshness.UPDATED else Freshness.DOWNLOADED
            onStatus?.invoke(freshness)

            val saved = saveBody(conn, target, onProgress, acceptsPlain(canonicalUrl))
            if (saved == null) {
                // دانلود شکست خورد؛ فایل قبلی دست‌نخورده مانده (جایگزینی فقط پس از اعتبارسنجی).
                if (hasCache && isCached(ctx, canonicalUrl)) {
                    return PrepareResult(target, Freshness.CACHED)
                }
                return null
            }

            writeMeta(ctx, canonicalUrl, remote)
            recentChecks[key] = now
            return PrepareResult(saved, freshness)
        } catch (_: Throwable) {
            // آفلاین/timeout: کش سالم همان‌جا نمایش داده می‌شود.
            if (hasCache && isCached(ctx, canonicalUrl)) {
                return PrepareResult(target, Freshness.CACHED)
            }
            return null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun openGet(url: String, stored: RemoteMeta?): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = true
        conn.useCaches = false
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept-Encoding", "identity")
        conn.setRequestProperty("Cache-Control", "no-cache")
        if (stored != null) {
            if (stored.etag.isNotBlank()) {
                conn.setRequestProperty("If-None-Match", stored.etag)
            } else if (stored.lastModified > 0L) {
                conn.setRequestProperty("If-Modified-Since", httpDate(stored.lastModified))
            }
        }
        return conn
    }

    private fun httpDate(millis: Long): String =
        java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("GMT") }
            .format(java.util.Date(millis))

    /**
     * پاسخ ۲۰۰ آمد؛ آیا محتوا همان کش است؟
     * طول متفاوت = قطعاً عوض شده. با ETag هر دو طرف، تصمیم قطعی است (حتی با طول برابر).
     * کش قدیمی بدون متادیتا (stored == null) و هم‌طول: پذیرفته می‌شود و متادیتا ثبت می‌شود.
     */
    private fun isUnchanged(
        url: String,
        local: File,
        stored: RemoteMeta?,
        remote: RemoteMeta,
    ): Boolean {
        if (remote.contentLength >= 0 && remote.contentLength != local.length()) return false
        if (stored == null) return true

        if (stored.etag.isNotBlank() && remote.etag.isNotBlank()) {
            return stored.etag == remote.etag
        }
        if (stored.lastModified > 0L && remote.lastModified > 0L) {
            return stored.lastModified == remote.lastModified
        }

        // سرور اعتبارسنج ندارد: ابتدا و انتها مقایسه شود (IV تازهٔ هر رمزنگاری در ابتدای فایل است).
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
                useCaches = false
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

    /**
     * بدنهٔ پاسخ ۲۰۰ را در .tmp می‌ریزد، قالب را می‌سنجد (HMK1 یا — اگر [allowPlain] —
     * HTML ساده) و اتمیک جایگزین می‌کند.
     */
    private fun saveBody(
        conn: HttpURLConnection,
        target: File,
        onProgress: ((Int, Int) -> Unit)?,
        allowPlain: Boolean,
    ): File? {
        val tmp = File(target.path + ".tmp")
        runCatching { tmp.delete() }
        try {
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

            val valid = looksWrapped(tmp) || (allowPlain && looksLikeHtml(tmp))
            if (!valid) {
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

    /** فقط آدرس‌های `.html` اجازهٔ ذخیرهٔ HTML ساده دارند. */
    private fun acceptsPlain(url: String): Boolean =
        runCatching { Uri.parse(url).path.orEmpty().endsWith(".html", ignoreCase = true) }
            .getOrDefault(false)

    /** بایت‌های اول با (BOM و فاصله‌های ابتدایی) به `<` برسد؛ صفحهٔ خطا/JSON/باینری رد می‌شود. */
    private fun looksLikeHtml(file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        return runCatching {
            file.inputStream().use { input ->
                val head = ByteArray(2048)
                val n = input.read(head)
                if (n <= 0) return false
                var i = 0
                if (n >= 3 && head[0] == 0xEF.toByte() && head[1] == 0xBB.toByte() && head[2] == 0xBF.toByte()) {
                    i = 3
                }
                while (i < n) {
                    val b = head[i].toInt()
                    if (b == 0x20 || b == 0x09 || b == 0x0A || b == 0x0D) i++ else break
                }
                i < n && head[i].toInt() == 0x3C
            }
        }.getOrDefault(false)
    }
}
