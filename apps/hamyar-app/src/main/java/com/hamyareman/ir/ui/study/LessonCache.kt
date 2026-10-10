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

                // Freshness is determined only by exact byte length from HTTP HEAD.
                // No response body is downloaded when local and remote lengths match.
                // If the origin cannot report Content-Length, keep the good cache and
                // retry the lightweight check on a later open instead of downloading
                // the whole document just to compare it.
                val remote = fetchMeta(canonicalUrl)
                if (remote == null || remote.contentLength < 0L) {
                    onStatus?.invoke(Freshness.CACHED)
                    return PrepareResult(target, Freshness.CACHED)
                }

                when (sameSize(target.length(), remote.contentLength)) {
                    true -> {
                        recentChecks[key] = now
                        onStatus?.invoke(Freshness.CACHED)
                        return PrepareResult(target, Freshness.CACHED)
                    }
                    null -> {
                        onStatus?.invoke(Freshness.CACHED)
                        return PrepareResult(target, Freshness.CACHED)
                    }
                    false -> Unit
                }

                // Different byte length: download a candidate and validate it before
                // atomically replacing the old payload. Failed updates keep the cache.
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
     * The only freshness criterion: exact file length in bytes.
     * null means one side has no trustworthy size, so it must not trigger a download.
     */
    internal fun sameSize(localBytes: Long, remoteBytes: Long): Boolean? {
        if (localBytes < 0L || remoteBytes < 0L) return null
        return localBytes == remoteBytes
    }

    /**
     * Reads only HTTP response headers. Never falls back to GET/Range for freshness,
     * so checking an unchanged cache does not download the document body.
     */
    private fun fetchMeta(url: String): RemoteMeta? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "HEAD"
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (conn.responseCode !in 200..299) return null
            val length = conn.contentLengthLong
            if (length < 0L) return null
            RemoteMeta(
                contentLength = length,
                contentType = conn.contentType.orEmpty(),
            )
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun readMeta(ctx: Context, url: String): RemoteMeta? = runCatching {
        val p = Properties()
        metaFor(ctx, url).inputStream().use(p::load)
        RemoteMeta(
            contentLength = p.getProperty("length", "-1").toLong(),
            contentType = p.getProperty("contentType", ""),
        )
    }.getOrNull()

    private fun writeMeta(ctx: Context, url: String, remote: RemoteMeta) {
        runCatching {
            val meta = metaFor(ctx, url)
            val tmp = File(meta.absolutePath + ".tmp")
            val p = Properties()
            p.setProperty("length", remote.contentLength.toString())
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
                    contentLength = conn.contentLengthLong,
                    contentType = conn.contentType.orEmpty(),
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
