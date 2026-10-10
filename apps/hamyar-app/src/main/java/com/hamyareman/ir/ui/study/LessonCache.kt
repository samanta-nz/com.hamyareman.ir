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
    enum class FailureReason { NETWORK, INVALID_PAYLOAD, NOT_FOUND }
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
    private val lastFailures = ConcurrentHashMap<String, FailureReason>()

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

    fun failureReason(url: String): FailureReason =
        lastFailures[keyOf(url)] ?: FailureReason.NETWORK

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

                // Always fetch a complete candidate on each freshness check. Exact byte
                // length is the fast first comparison; equal-length files are then
                // compared byte-for-byte, including their middle. HTTP validators and
                // small prefix/suffix samples are deliberately not trusted as proof
                // that the content is unchanged.
                val candidate = File(target.path + ".candidate")
                runCatching { candidate.delete() }
                val downloaded = runCatching {
                    download(ctx, canonicalUrl, candidate, onProgress, requireEncryptionKey = true)
                }.getOrNull()

                if (downloaded == null) {
                    runCatching { candidate.delete() }
                    onStatus?.invoke(Freshness.CACHED)
                    return if (looksUsable(ctx, target, canonicalUrl)) {
                        PrepareResult(target, Freshness.CACHED)
                    } else {
                        null
                    }
                }

                if (sameBytes(target, downloaded)) {
                    runCatching { downloaded.delete() }
                    recentChecks[key] = System.currentTimeMillis()
                    onStatus?.invoke(Freshness.CACHED)
                    return PrepareResult(target, Freshness.CACHED)
                }

                if (!replaceTarget(downloaded, target)) {
                    runCatching { downloaded.delete() }
                    onStatus?.invoke(Freshness.CACHED)
                    return if (looksUsable(ctx, target, canonicalUrl)) {
                        PrepareResult(target, Freshness.CACHED)
                    } else {
                        null
                    }
                }

                recentChecks[key] = System.currentTimeMillis()
                onStatus?.invoke(Freshness.UPDATED)
                return PrepareResult(target, Freshness.UPDATED)
            }

            onStatus?.invoke(Freshness.DOWNLOADED)
            val downloaded = runCatching {
                download(ctx, canonicalUrl, target, onProgress)
            }.getOrNull() ?: return null
            recentChecks[key] = System.currentTimeMillis()
            return PrepareResult(downloaded, Freshness.DOWNLOADED)
        }
    }

    fun ensure(ctx: Context, url: String, onProgress: ((Int, Int) -> Unit)? = null): File? =
        prepare(ctx, url, onProgress)?.file

    /**
     * Force a fresh download after payload-level validation fails. A valid candidate
     * replaces the cache only after format validation; failures leave the old cache intact.
     */
    fun refresh(
        ctx: Context,
        url: String,
        onProgress: ((Int, Int) -> Unit)? = null,
    ): PrepareResult? {
        val canonicalUrl = canonical(url)
        val target = fileFor(ctx, canonicalUrl)
        val key = keyOf(canonicalUrl)
        val lock = locks.getOrPut(key) { Any() }
        synchronized(lock) {
            val hadCache = looksUsable(ctx, target, canonicalUrl)
            val downloaded = runCatching {
                download(ctx, canonicalUrl, target, onProgress, requireEncryptionKey = hadCache)
            }.getOrNull()
            if (downloaded == null) {
                return if (hadCache) PrepareResult(target, Freshness.CACHED) else null
            }
            recentChecks[key] = System.currentTimeMillis()
            return PrepareResult(
                downloaded,
                if (hadCache) Freshness.UPDATED else Freshness.DOWNLOADED,
            )
        }
    }

    /**
     * Fast exact length check, followed by a complete byte-for-byte comparison.
     * Unlike ETag or sampling, this detects a changed byte anywhere in an equal-size file.
     */
    internal fun sameBytes(first: File, second: File): Boolean {
        if (!first.exists() || !second.exists() || first.length() != second.length()) return false
        return try {
            first.inputStream().buffered().use { left ->
                second.inputStream().buffered().use { right ->
                    val a = ByteArray(64 * 1024)
                    val b = ByteArray(64 * 1024)
                    while (true) {
                        val readA = readChunk(left, a)
                        val readB = readChunk(right, b)
                        if (readA != readB) return false
                        if (readA < 0) return true
                        for (i in 0 until readA) {
                            if (a[i] != b[i]) return false
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    false
                }
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun readChunk(input: java.io.InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = input.read(buffer, total, buffer.size - total)
            if (n < 0) break
            if (n == 0) continue
            total += n
        }
        return if (total == 0) -1 else total
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
        ctx: Context,
        url: String,
        target: File,
        onProgress: ((Int, Int) -> Unit)?,
        requireEncryptionKey: Boolean = false,
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
            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                lastFailures[keyOf(url)] = if (responseCode == 403 || responseCode == 404 || responseCode == 410) {
                    FailureReason.NOT_FOUND
                } else {
                    FailureReason.NETWORK
                }
                return null
            }
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
                lastFailures[keyOf(url)] = FailureReason.INVALID_PAYLOAD
                tmp.delete()
                return null
            }
            if (!looksUsable(
                    ctx, tmp, url, conn.contentType.orEmpty(),
                    verifyEncrypted = true,
                    requireEncryptionKey = requireEncryptionKey,
                )
            ) {
                lastFailures[keyOf(url)] = FailureReason.INVALID_PAYLOAD
                tmp.delete()
                return null
            }
            if (!replaceTarget(tmp, target)) {
                lastFailures[keyOf(url)] = FailureReason.NETWORK
                tmp.delete()
                return null
            }
            // Save validators attached to these exact downloaded bytes. A later HEAD
            // could describe a concurrent replacement instead of this local payload.
            writeMeta(
                ctx,
                url,
                RemoteMeta(
                    conn.contentLengthLong,
                    conn.getHeaderField("ETag").orEmpty(),
                    conn.lastModified,
                    conn.contentType.orEmpty(),
                ),
            )
            lastFailures.remove(keyOf(url))
            return target
        } catch (_: Throwable) {
            lastFailures[keyOf(url)] = FailureReason.NETWORK
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
        verifyEncrypted: Boolean = false,
        requireEncryptionKey: Boolean = false,
    ): Boolean {
        if (!file.exists() || file.length() <= 0L || file.length() > MAX_DECRYPT_BYTES) return false
        val prefix = readPrefix(file, PROBE_BYTES)
        if (HtmlCodec.hasMagic(prefix)) {
            if (file.length() < HtmlCodec.MIN_WRAPPED_BYTES) return false
            // Verify GCM authentication before replacing a good cache when the key is
            // already available. If there is no key yet, keep the structurally valid
            // envelope so a later authenticated session can decode it.
            val hasKey = ctx != null && runCatching { HtmlMediaKey.get(ctx) != null }
                .getOrDefault(false)
            if (!verifyEncrypted) return true
            if (ctx == null || !hasKey) return !requireEncryptionKey
            val wrapped = runCatching { file.readBytes() }.getOrNull() ?: return false
            val plain = runCatching { HtmlCodec.unwrap(ctx, wrapped) }.getOrNull() ?: return false
            return HtmlCodec.isPlainHtml(plain)
        }

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
            val restored = runCatching { backup.copyTo(target, overwrite = true) }.isSuccess
            if (restored) runCatching { backup.delete() }
        }
        return false
    }
}
