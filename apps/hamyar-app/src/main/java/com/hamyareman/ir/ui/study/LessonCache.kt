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
 * Stores the exact payload for a stable HTML URL. The payload may be an HMK1
 * envelope or ordinary HTML; the current bytes, not the previous cache format,
 * determine how it is validated and delivered.
 */
object LessonCache {
    const val MAX_DECRYPT_BYTES: Int = 32 * 1024 * 1024
    enum class Freshness { CACHED, UPDATED, DOWNLOADED }
    data class PrepareResult(val file: File, val freshness: Freshness)

    private data class RemoteMeta(
        val contentLength: Long,
        val etag: String,
        val lastModified: Long,
        val contentType: String,
    )

    private const val DIR = "lessons"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val PROBE_BYTES = 8 * 1024
    // Coalesce the UI preflight with its immediate WebView request, but don't hide
    // replacements for the old 30-second freshness window.
    private const val FRESH_CHECK_TTL_MS = 1_500L
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
    fun fileFor(ctx: Context, url: String): File = File(dir(ctx), keyOf(url) + ".bin")
    private fun metaFor(ctx: Context, url: String): File = File(dir(ctx), keyOf(url) + ".meta")

    fun isCached(ctx: Context, url: String): Boolean =
        looksUsable(ctx, fileFor(ctx, url), canonical(url))

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
        runCatching {
            dir(ctx).listFiles()?.filter { it.extension == "bin" }?.sumOf { it.length() } ?: 0L
        }.getOrDefault(0L)

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

    /**
     * A good old cache survives network failures and failed updates. Changed content
     * replaces it only after the downloaded bytes pass validation.
     */
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
            val hasCache = looksUsable(ctx, target, canonicalUrl)
            if (hasCache) {
                val now = System.currentTimeMillis()
                if (now - (recentChecks[key] ?: 0L) < FRESH_CHECK_TTL_MS) {
                    onStatus?.invoke(Freshness.CACHED)
                    return PrepareResult(target, Freshness.CACHED)
                }

                val remote = fetchMeta(canonicalUrl)
                if (remote == null) {
                    // Do not confuse offline with a missing or invalid cache, and do not
                    // suppress retries when connectivity comes back.
                    onStatus?.invoke(Freshness.CACHED)
                    return PrepareResult(target, Freshness.CACHED)
                }

                if (isSameContent(ctx, canonicalUrl, target, remote)) {
                    writeMeta(ctx, canonicalUrl, remote)
                    recentChecks[key] = now
                    onStatus?.invoke(Freshness.CACHED)
                    return PrepareResult(target, Freshness.CACHED)
                }

                val downloaded = runCatching {
                    download(canonicalUrl, target, onProgress)
                }.getOrNull()
                if (downloaded == null) {
                    onStatus?.invoke(Freshness.CACHED)
                    return if (looksUsable(ctx, target, canonicalUrl)) {
                        PrepareResult(target, Freshness.CACHED)
                    } else null
                }

                (fetchMeta(canonicalUrl) ?: remote).let { writeMeta(ctx, canonicalUrl, it) }
                recentChecks[key] = System.currentTimeMillis()
                onStatus?.invoke(Freshness.UPDATED)
                return PrepareResult(downloaded, Freshness.UPDATED)
            }

            onStatus?.invoke(Freshness.DOWNLOADED)
            val downloaded = runCatching {
                download(canonicalUrl, target, onProgress)
            }.getOrNull() ?: return null
            fetchMeta(canonicalUrl)?.let { writeMeta(ctx, canonicalUrl, it) }
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
                    conn.contentLengthLong,
                    conn.getHeaderField("ETag").orEmpty(),
                    conn.lastModified,
                    conn.contentType.orEmpty(),
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
        if (head != null) return head

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
                    ?: -1L
                conn.inputStream.use { input ->
                    val buffer = ByteArray(PROBE_BYTES)
                    input.read(buffer)
                }
                RemoteMeta(
                    total,
                    conn.getHeaderField("ETag").orEmpty(),
                    conn.lastModified,
                    conn.contentType.orEmpty(),
                )
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
        if (!looksUsable(ctx, local, url)) return false
        if (remote.contentLength >= 0L && remote.contentLength != local.length()) return false

        // Byte samples are checked before trusting validators, so a stale sidecar or
        // an encrypted/plain transition cannot be hidden by equal lengths or ETags.
        val prefixMatches = compareRange(url, local, 0L, PROBE_BYTES)
        if (prefixMatches == false) return false
        val suffixStart = (local.length() - PROBE_BYTES).coerceAtLeast(0L)
        val suffixMatches = compareRange(url, local, suffixStart, PROBE_BYTES)
        if (suffixMatches == false) return false

        val previous = readMeta(ctx, url)
        if (previous != null &&
            previous.etag.isNotBlank() && remote.etag.isNotBlank()
        ) {
            return previous.etag == remote.etag
        }
        if (previous != null && previous.lastModified > 0L && remote.lastModified > 0L) {
            if (previous.lastModified != remote.lastModified) return false
            return prefixMatches == true && suffixMatches == true
        }
        return prefixMatches == true && suffixMatches == true
    }

    /** true/false if comparable; null if the server did not provide a usable range. */
    private fun compareRange(url: String, local: File, start: Long, requested: Int): Boolean? {
        if (!local.exists() || local.length() <= 0L || start < 0L || start >= local.length()) return null
        val length = minOf(requested.toLong(), local.length() - start).toInt()
        if (length <= 0) return null
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Range", "bytes=" + start + "-" + (start + length - 1))
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_PARTIAL &&
                !(start == 0L && code == HttpURLConnection.HTTP_OK)
            ) return null

            val remoteBytes = ByteArray(length)
            var offset = 0
            conn.inputStream.use { input ->
                while (offset < length) {
                    val n = input.read(remoteBytes, offset, length - offset)
                    if (n <= 0) break
                    offset += n
                }
            }
            if (offset != length) return false
            val localBytes = ByteArray(length)
            val count = java.io.RandomAccessFile(local, "r").use {
                it.seek(start)
                it.read(localBytes)
            }
            count == length && localBytes.contentEquals(remoteBytes)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun readMeta(ctx: Context, url: String): RemoteMeta? = runCatching {
        val p = Properties()
        metaFor(ctx, url).inputStream().use(p::load)
        RemoteMeta(
            p.getProperty("length", "-1").toLong(),
            p.getProperty("etag", ""),
            p.getProperty("lastModified", "0").toLong(),
            p.getProperty("contentType", ""),
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
            p.setProperty("contentType", remote.contentType)
            tmp.outputStream().use { p.store(it, null) }
            if (!tmp.renameTo(meta)) {
                meta.delete()
                tmp.renameTo(meta)
            }
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
                    output.fd.sync()
                }
            }
            if (conn.contentLengthLong >= 0L && done.toLong() != conn.contentLengthLong) {
                tmp.delete()
                return null
            }
            if (!looksUsable(null, tmp, url, conn.contentType.orEmpty())) {
                tmp.delete()
                return null
            }
            if (!replaceTarget(tmp, target)) {
                tmp.delete()
                return null
            }
            return target
        } catch (_: Throwable) {
            runCatching { tmp.delete() }
            return null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun looksUsable(
        ctx: Context?,
        file: File,
        url: String,
        responseContentType: String = "",
    ): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val prefix = readPrefix(file, PROBE_BYTES)
        if (HtmlCodec.hasMagic(prefix)) return file.length() >= HtmlCodec.MIN_WRAPPED_BYTES

        val savedType = if (ctx != null) readMeta(ctx, url)?.contentType.orEmpty() else ""
        val contentType = responseContentType.ifBlank { savedType }
        val htmlType = contentType.substringBefore(';').trim().equals("text/html", ignoreCase = true)
        return (isHtmlUrl(url) || htmlType) && HtmlCodec.isPlainHtml(prefix)
    }

    private fun isHtmlUrl(url: String): Boolean {
        val path = runCatching { Uri.parse(url).path.orEmpty() }.getOrDefault("")
        return path.endsWith(".html", ignoreCase = true) || path.endsWith(".htm", ignoreCase = true)
    }

    private fun readPrefix(file: File, maxBytes: Int): ByteArray {
        if (!file.exists() || file.length() <= 0L) return ByteArray(0)
        val size = minOf(file.length(), maxBytes.toLong()).toInt()
        val out = ByteArray(size)
        var offset = 0
        file.inputStream().use { input ->
            while (offset < size) {
                val n = input.read(out, offset, size - offset)
                if (n <= 0) break
                offset += n
            }
        }
        return if (offset == size) out else out.copyOf(offset)
    }

    private fun replaceTarget(tmp: File, target: File): Boolean {
        if (!target.exists()) return tmp.renameTo(target)
        // Prefer a direct atomic replacement when the filesystem supports it.
        if (tmp.renameTo(target)) return true
        val backup = File(target.path + ".bak")
        runCatching { backup.delete() }
        if (!target.renameTo(backup)) return false
        if (tmp.renameTo(target)) {
            runCatching { backup.delete() }
            return true
        }
        if (!backup.renameTo(target)) {
            runCatching { backup.copyTo(target, overwrite = true) }
            runCatching { backup.delete() }
        }
        return false
    }
}
