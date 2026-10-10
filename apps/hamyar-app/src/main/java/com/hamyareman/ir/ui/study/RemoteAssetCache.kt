package com.hamyareman.ir.ui.study

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * Freshness-aware disk cache for raw (non-HMK1 HTML) bucket resources requested by
 * WebView: images, audio/video, CSS, JavaScript, fonts and documents.
 * Encrypted HTML stays exclusively on LessonCache.
 */
object RemoteAssetCache {
    private const val DIR = "remote-assets"
    private const val CHECK_TTL_MS = 30_000L
    private const val PROBE_BYTES = 8 * 1024

    private data class Snapshot(
        val length: Long = -1L,
        val etag: String = "",
        val lastModified: String = "",
        val mimeType: String = "",
        val checkedAt: Long = 0L,
    )
    private data class Probe(val status: Int, val snapshot: Snapshot?)
    private data class Chosen(val file: File, val snapshot: Snapshot?)
    private val locks = ConcurrentHashMap<String, Any>()

    fun intercept(ctx: Context, url: String, path: String, rangeHeader: String?): WebResourceResponse? {
        val canonical = LessonCache.canonical(url)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        val dir = File(ctx.filesDir, DIR).apply { mkdirs() }
        val target = File(dir, digest + ".bin")
        val metaFile = File(dir, digest + ".meta")
        val lock = locks.getOrPut(digest) { Any() }

        val chosen = synchronized(lock) {
            val hasCache = target.isFile && target.length() > 0L && isPlausible(target)
            val saved = readSnapshot(metaFile)
            val now = System.currentTimeMillis()
            if (hasCache && saved != null && now >= saved.checkedAt &&
                now - saved.checkedAt < CHECK_TTL_MS
            ) {
                Chosen(target, saved)
            } else {
                val probe = inspect(url)
                if (probe.status == 403 || probe.status == 404 || probe.status == 410) {
                    null
                } else if (probe.snapshot == null) {
                    // A transient network/server error is not proof that an object was removed.
                    if (hasCache) Chosen(target, saved) else null
                } else {
                    val remote = probe.snapshot
                    if (hasCache && isSameContent(url, target, saved, remote)) {
                        val confirmed = remote.copy(
                            length = remote.length.takeIf { it >= 0L } ?: target.length(),
                            mimeType = remote.mimeType.ifBlank { saved?.mimeType.orEmpty() },
                            checkedAt = now,
                        )
                        writeSnapshot(metaFile, confirmed)
                        Chosen(target, confirmed)
                    } else {
                        val temp = File(target.path + ".tmp")
                        runCatching { temp.delete() }
                        val downloaded = download(url, temp, remote)
                        if (downloaded != null && isPlausible(temp) && replaceFile(temp, target)) {
                            val confirmed = downloaded.copy(
                                length = target.length(),
                                mimeType = detectedMime(target, path, downloaded.mimeType),
                                checkedAt = System.currentTimeMillis(),
                            )
                            writeSnapshot(metaFile, confirmed)
                            Chosen(target, confirmed)
                        } else {
                            runCatching { temp.delete() }
                            // A failed update never destroys the previously verified file.
                            if (hasCache) Chosen(target, saved) else null
                        }
                    }
                }
            }
        } ?: return null
        return createResponse(chosen.file, path, chosen.snapshot, rangeHeader)
    }

    private fun inspect(url: String): Probe {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                requestMethod = "HEAD"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            val code = conn.responseCode
            if (code in 200..299) return Probe(code, snapshotFrom(conn))
            if (code == 403 || code == 404 || code == 410) return Probe(code, null)
            if (code != 405 && code != 501) return Probe(code, null)
        } catch (_: Throwable) {
            return Probe(0, null)
        } finally {
            runCatching { conn?.disconnect() }
        }
        return inspectRange(url)
    }

    private fun inspectRange(url: String): Probe {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-0")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            val code = conn.responseCode
            when {
                code == 403 || code == 404 || code == 410 -> Probe(code, null)
                code == HttpURLConnection.HTTP_PARTIAL -> Probe(code, snapshotFrom(conn))
                else -> Probe(code, null)
            }
        } catch (_: Throwable) {
            Probe(0, null)
        } finally {
            runCatching { conn?.inputStream?.close() }
            runCatching { conn?.disconnect() }
        }
    }

    private fun snapshotFrom(conn: HttpURLConnection): Snapshot {
        val rangeTotal = conn.getHeaderField("Content-Range")
            ?.substringAfterLast('/', "")
            ?.toLongOrNull()
        return Snapshot(
            length = rangeTotal ?: conn.getHeaderField("Content-Length")?.toLongOrNull()
                ?: conn.contentLengthLong,
            etag = conn.getHeaderField("ETag").orEmpty().trim(),
            lastModified = conn.getHeaderField("Last-Modified").orEmpty().trim(),
            mimeType = conn.getHeaderField("Content-Type").orEmpty().substringBefore(';').trim(),
        )
    }

    private fun isSameContent(url: String, local: File, saved: Snapshot?, remote: Snapshot): Boolean {
        if (remote.length >= 0L && remote.length != local.length()) return false
        if (saved != null) {
            if (saved.etag.isNotBlank() && remote.etag.isNotBlank()) return saved.etag == remote.etag
            if (saved.lastModified.isNotBlank() && remote.lastModified.isNotBlank()) {
                return saved.lastModified == remote.lastModified
            }
        }
        // With no usable validators, compare both ends. Same-length changed files are detected
        // when their prefix or suffix changes, including freshly wrapped payloads with a new IV.
        return compareRange(url, local, 0L) &&
            compareRange(url, local, (local.length() - PROBE_BYTES).coerceAtLeast(0L))
    }

    private fun compareRange(url: String, local: File, start: Long): Boolean {
        if (!local.isFile || local.length() <= start) return false
        val count = minOf(PROBE_BYTES.toLong(), local.length() - start).toInt()
        if (count <= 0) return false
        var conn: HttpURLConnection? = null
        val remote = try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=" + start + "-" + (start + count - 1))
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_PARTIAL) return false
            conn.inputStream.use { input ->
                val bytes = ByteArray(count)
                var offset = 0
                while (offset < count) {
                    val read = input.read(bytes, offset, count - offset)
                    if (read <= 0) break
                    offset += read
                }
                if (offset != count) return false
                bytes
            }
        } catch (_: Throwable) {
            return false
        } finally {
            runCatching { conn?.disconnect() }
        }
        val localBytes = ByteArray(count)
        val read = runCatching {
            java.io.RandomAccessFile(local, "r").use { raf ->
                raf.seek(start)
                raf.read(localBytes)
            }
        }.getOrDefault(-1)
        return read == count && localBytes.contentEquals(remote)
    }

    private fun download(url: String, temp: File, expected: Snapshot): Snapshot? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 45_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val received = snapshotFrom(conn)
            if (expected.etag.isNotBlank() && received.etag.isNotBlank() &&
                expected.etag != received.etag
            ) return null
            if (expected.length >= 0L && received.length >= 0L &&
                expected.length != received.length
            ) return null
            conn.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                    output.fd.sync()
                }
            }
            if (temp.length() <= 0L) return null
            if (expected.length >= 0L && temp.length() != expected.length) return null
            received.copy(
                length = temp.length(),
                etag = received.etag.ifBlank { expected.etag },
                lastModified = received.lastModified.ifBlank { expected.lastModified },
                mimeType = received.mimeType.ifBlank { expected.mimeType },
            )
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun isPlausible(file: File): Boolean {
        if (!file.isFile || file.length() <= 0L) return false
        val bytes = ByteArray(32)
        val count = runCatching { file.inputStream().use { it.read(bytes) } }.getOrDefault(-1)
        if (count <= 0) return false
        if (count >= 4 && String(bytes, 0, 4, Charsets.US_ASCII) == "HMK1") return false
        val prefix = String(bytes, 0, count, Charsets.ISO_8859_1).trimStart().lowercase()
        return !prefix.startsWith("<!doctype html") && !prefix.startsWith("<html")
    }

    private fun detectedMime(file: File, path: String, declared: String): String {
        val head = ByteArray(16)
        val count = runCatching { file.inputStream().use { it.read(head) } }.getOrDefault(-1)
        if (count >= 8 && (head[0].toInt() and 0xFF) == 0x89 &&
            head[1] == 0x50.toByte() && head[2] == 0x4E.toByte() && head[3] == 0x47.toByte()
        ) return "image/png"
        if (count >= 3 && (head[0].toInt() and 0xFF) == 0xFF &&
            (head[1].toInt() and 0xFF) == 0xD8 && (head[2].toInt() and 0xFF) == 0xFF
        ) return "image/jpeg"
        if (count >= 6 && String(head, 0, 6, Charsets.US_ASCII) in setOf("GIF87a", "GIF89a")) return "image/gif"
        if (count >= 12 && String(head, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(head, 8, 4, Charsets.US_ASCII) == "WEBP"
        ) return "image/webp"
        if (count >= 8 && String(head, 4, 4, Charsets.US_ASCII) == "ftyp") return "video/mp4"
        if (count >= 4 && String(head, 0, 4, Charsets.US_ASCII) == "OggS") return "audio/ogg"
        if (count >= 3 && String(head, 0, 3, Charsets.US_ASCII) == "ID3") return "audio/mpeg"
        if (count >= 5 && String(head, 0, 5, Charsets.US_ASCII) == "%PDF-") return "application/pdf"
        val fromHeader = declared.substringBefore(';').trim()
        if (fromHeader.isNotBlank() && fromHeader != "application/octet-stream") return fromHeader
        return URLConnection.guessContentTypeFromName(path) ?: fromHeader.ifBlank { "application/octet-stream" }
    }

    private fun createResponse(file: File, path: String, snapshot: Snapshot?, rangeHeader: String?): WebResourceResponse {
        val mime = detectedMime(file, path, snapshot?.mimeType.orEmpty())
        val length = file.length()
        val headers = linkedMapOf("Accept-Ranges" to "bytes", "Cache-Control" to "no-store")
        var start = 0L
        var end = length - 1L
        var status = 200
        var reason = "OK"
        if (!rangeHeader.isNullOrBlank() && rangeHeader.startsWith("bytes=")) {
            val spec = rangeHeader.removePrefix("bytes=").trim()
            if (spec.contains(',')) return rangeError(mime, length)
            val split = spec.split('-', limit = 2)
            if (split.size == 2) {
                val left = split[0].toLongOrNull()
                val right = split[1].toLongOrNull()
                if (left == null && right != null && right > 0L) start = (length - right).coerceAtLeast(0L)
                else if (left != null && left >= 0L) {
                    start = left
                    if (right != null) end = right
                }
            }
            if (start >= length || end < start) return rangeError(mime, length)
            end = end.coerceAtMost(length - 1L)
            headers["Content-Length"] = (end - start + 1L).toString()
            headers["Content-Range"] = "bytes " + start + "-" + end + "/" + length
            status = 206
            reason = "Partial Content"
        } else {
            headers["Content-Length"] = length.toString()
        }
        val input = FileInputStream(file)
        var skipped = 0L
        while (skipped < start) {
            val step = input.skip(start - skipped)
            if (step <= 0L) {
                if (input.read() < 0) break
                skipped++
            } else skipped += step
        }
        val stream = if (status == 206) LimitedInputStream(input, end - start + 1L) else input
        return WebResourceResponse(mime, charsetFor(mime), status, reason, headers, stream)
    }

    private fun rangeError(mime: String, length: Long) = WebResourceResponse(
        mime, charsetFor(mime), 416, "Range Not Satisfiable",
        mapOf("Content-Range" to ("bytes */" + length), "Content-Length" to "0"),
        ByteArrayInputStream(ByteArray(0)),
    )

    private fun charsetFor(mime: String): String? =
        if (mime.startsWith("text/") || mime == "application/javascript" ||
            mime == "application/json" || mime == "application/xml" || mime == "image/svg+xml"
        ) "utf-8" else null

    private class LimitedInputStream(input: InputStream, limit: Long) : FilterInputStream(input) {
        private var remaining = limit
        override fun read(): Int {
            if (remaining <= 0L) return -1
            val value = super.read()
            if (value >= 0) remaining--
            return value
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0L) return -1
            val allowed = minOf(length.toLong(), remaining).toInt()
            val count = super.read(buffer, offset, allowed)
            if (count > 0) remaining -= count.toLong()
            return count
        }
    }

    private fun replaceFile(temp: File, target: File): Boolean {
        if (temp.renameTo(target)) return true
        val backup = File(target.path + ".old")
        runCatching { backup.delete() }
        if (target.exists() && !target.renameTo(backup)) return false
        if (temp.renameTo(target)) {
            runCatching { backup.delete() }
            return true
        }
        if (backup.exists()) runCatching { backup.renameTo(target) }
        return false
    }

    private fun readSnapshot(file: File): Snapshot? = runCatching {
        if (!file.isFile) return null
        val properties = Properties()
        file.inputStream().use(properties::load)
        Snapshot(
            length = properties.getProperty("length", "-1").toLong(),
            etag = properties.getProperty("etag", ""),
            lastModified = properties.getProperty("lastModified", ""),
            mimeType = properties.getProperty("mimeType", ""),
            checkedAt = properties.getProperty("checkedAt", "0").toLong(),
        )
    }.getOrNull()

    private fun writeSnapshot(file: File, snapshot: Snapshot) {
        runCatching {
            val temp = File(file.path + ".tmp")
            val properties = Properties()
            properties.setProperty("length", snapshot.length.toString())
            properties.setProperty("etag", snapshot.etag)
            properties.setProperty("lastModified", snapshot.lastModified)
            properties.setProperty("mimeType", snapshot.mimeType)
            properties.setProperty("checkedAt", snapshot.checkedAt.toString())
            temp.outputStream().use { properties.store(it, null) }
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }
}
