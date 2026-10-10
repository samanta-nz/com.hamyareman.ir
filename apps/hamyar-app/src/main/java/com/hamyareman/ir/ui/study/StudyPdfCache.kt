package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import com.hamyareman.ir.ui.net.awaitOnlineBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/** یک downloader قطعی PDF برای همهٔ صفحات: resume، fallback خودکار و rename اتمیک. */
object StudyPdfCache {

    fun file(ctx: Context, fileId: String): File =
        File(ctx.filesDir, "media/pdf-cache/$fileId").also { it.parentFile?.mkdirs() }

    fun isValid(file: File): Boolean = runCatching {
        if (!file.isFile || file.length() < 1024L) return@runCatching false
        val header = file.inputStream().use { input ->
            val magic = ByteArray(5)
            input.read(magic) == 5 && String(magic, Charsets.US_ASCII) == "%PDF-"
        }
        val count = minOf(4096L, file.length()).toInt()
        val tail = ByteArray(count)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(file.length() - count)
            raf.readFully(tail)
        }
        header && String(tail, Charsets.ISO_8859_1).contains("%%EOF")
    }.getOrDefault(false)

    /**
     * EXTERNAL/INTERNAL دقیقاً یک URL دارند. FASTEST ابتدا برنده و در شکست، mirror
     * دوم را از همان offset ادامه می‌دهد؛ mirror قبل از public شدن hash/size verify می‌شود.
     */
    fun obtain(ctx: Context, fileId: String, onProgress: (Int) -> Unit = {}): File {
        val remoteId = StudyMedia.resolveFileId(fileId)
        return obtain(ctx, fileId, StudyMedia.candidateUrls(remoteId), onProgress)
    }

    /**
     * دریافت PDF با کلید cache و URLهای جداگانه.
     *
     * منوی زندهٔ کتاب‌ها کلید کاملِ Bucket را دارد و نامِ فایل به‌تنهایی در چند
     * کتاب تکرار می‌شود. این overload باعث می‌شود cache از مسیر واقعی جدا شود،
     * بی‌آنکه قرارداد قدیمیِ fileIdهای server-map شکسته شود.
     */
    fun obtain(
        ctx: Context,
        cacheId: String,
        urls: List<String>,
        onProgress: (Int) -> Unit = {},
    ): File {
        val target = file(ctx, cacheId)
        val candidates = urls.distinct().filter { it.isNotBlank() }
        require(candidates.isNotEmpty()) { "برای PDF نشانی سرور وجود ندارد." }
        val lock = locks.getOrPut(cacheId) { Any() }

        synchronized(lock) {
            val hasCache = isValid(target)
            val savedMeta = if (hasCache) readMeta(target) else null
            if (hasCache && savedMeta != null &&
                System.currentTimeMillis() - savedMeta.checkedAt < CHECK_TTL_MS
            ) return target

            val probe = probeRemote(candidates)
            when (probe) {
                is Probe.Missing -> throw RemoteMissingException(probe.url)
                Probe.Unavailable -> if (hasCache) return target
                is Probe.Available -> {
                    if (hasCache && sameContent(target, probe.url, savedMeta, probe.meta)) {
                        writeMeta(target, probe.meta.copy(checkedAt = System.currentTimeMillis()))
                        return target
                    }
                    if (hasCache) File(target.absolutePath + ".part").delete()
                }
            }

            val priorMeta = (probe as? Probe.Available)?.meta
            val priorMetaUrl = (probe as? Probe.Available)?.url
            val part = File(target.absolutePath + ".part")
            var last: Throwable? = null
            var confirmedMissing: String? = null

            for (url in candidates) {
                var attempts = 0
                while (attempts < 7) {
                    val offset = part.takeIf { it.isFile }?.length() ?: 0L
                    var conn: HttpURLConnection? = null
                    try {
                        conn = (URL(url).openConnection() as HttpURLConnection).apply {
                            connectTimeout = CONNECT_TIMEOUT_MS
                            readTimeout = READ_TIMEOUT_MS
                            instanceFollowRedirects = true
                            requestMethod = "GET"
                            setRequestProperty("Accept-Encoding", "identity")
                            setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                            setRequestProperty("Pragma", "no-cache")
                            if (offset > 0L) setRequestProperty("Range", "bytes=" + offset + "-")
                        }
                        val status = conn.responseCode
                        if (status == 403 || status == 404 || status == 410) {
                            confirmedMissing = url
                            part.delete()
                            break
                        }
                        if (status !in 200..299) throw IOException("HTTP " + status)

                        val append = offset > 0L && status == 206
                        if (offset > 0L && !append) {
                            part.delete()
                            attempts++
                            continue
                        }
                        val base = if (append) offset else 0L
                        val total = conn.contentLengthLong.let { if (it > 0L) base + it else -1L }
                        FileOutputStream(part, append).use { output ->
                            conn.inputStream.use { input ->
                                val buffer = ByteArray(64 * 1024)
                                var written = base
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    output.write(buffer, 0, read)
                                    written += read
                                    if (total > 0L) {
                                        onProgress(((written * 100L) / total).toInt().coerceIn(0, 100))
                                    }
                                }
                                output.fd.sync()
                            }
                        }
                        if (total > 0L && part.length() < total) {
                            throw IOException("دانلود ناقص " + part.length() + "/" + total)
                        }
                        if (!isValid(part)) throw IOException("PDF ناقص یا نامعتبر است")
                        if (!replacePreservingPrevious(part, target)) {
                            throw IOException("جایگزینی امن PDF ناموفق بود.")
                        }
                        val freshMeta = if (priorMeta != null && priorMetaUrl == url) priorMeta else head(url)
                        if (freshMeta != null) writeMeta(target, freshMeta)
                        onProgress(100)
                        return target
                    } catch (error: Throwable) {
                        last = error
                        attempts = if (error is IOException && error.message.orEmpty().startsWith("HTTP 4")) {
                            7
                        } else attempts + 1
                        if (!NetState.isOnline(ctx)) NetState.awaitOnlineBlocking(ctx)
                        if (attempts < 7) Thread.sleep((400L * attempts).coerceAtMost(3_000L))
                    } finally {
                        runCatching { conn?.disconnect() }
                    }
                }
                if (confirmedMissing != null) break
            }

            if (confirmedMissing != null) throw RemoteMissingException(confirmedMissing!!)
            if (hasCache && isValid(target)) return target
            part.delete()
            throw IOException(last?.message ?: "دانلود PDF کامل نشد.", last)
        }
    }

    private data class RemoteMeta(
        val length: Long,
        val etag: String,
        val lastModified: String,
        val checkedAt: Long = System.currentTimeMillis(),
    )

    private sealed interface Probe {
        data class Available(val url: String, val meta: RemoteMeta) : Probe
        data class Missing(val url: String) : Probe
        data object Unavailable : Probe
    }

    class RemoteMissingException(val missingUrl: String) :
        IOException("فایل PDF در نشانی مورد انتظار موجود نیست: " + missingUrl)

    private const val CHECK_TTL_MS = 30_000L
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val SAMPLE_BYTES = 8 * 1024
    private val locks = ConcurrentHashMap<String, Any>()

    private fun probeRemote(urls: List<String>): Probe {
        var sawMissing: String? = null
        var sawUnavailable = false
        for (url in urls) {
            val conn = runCatching {
                (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                    requestMethod = "HEAD"
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                    setRequestProperty("Pragma", "no-cache")
                }
            }.getOrNull()
            if (conn == null) {
                sawUnavailable = true
                continue
            }
            try {
                val status = conn.responseCode
                if (status in 200..299) {
                    return Probe.Available(url, RemoteMeta(
                        length = conn.contentLengthLong,
                        etag = conn.getHeaderField("ETag").orEmpty().trim(),
                        lastModified = conn.getHeaderField("Last-Modified").orEmpty().trim(),
                    ))
                }
                if (status == 403 || status == 404 || status == 410) sawMissing = url
                else sawUnavailable = true
            } catch (_: Throwable) {
                sawUnavailable = true
            } finally {
                runCatching { conn.disconnect() }
            }
        }
        if (sawMissing != null && !sawUnavailable) return Probe.Missing(sawMissing!!)

        // بعضی مبدأها HEAD را پشتیبانی نمی‌کنند؛ Range GET جایگزین می‌شود.
        for (url in urls) {
            val conn = runCatching {
                (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                    setRequestProperty("Pragma", "no-cache")
                    setRequestProperty("Range", "bytes=0-0")
                }
            }.getOrNull() ?: continue
            try {
                val status = conn.responseCode
                if (status in 200..299) {
                    val range = conn.getHeaderField("Content-Range").orEmpty()
                    val length = range.substringAfterLast("/", "").toLongOrNull()
                        ?: conn.contentLengthLong
                    runCatching { conn.inputStream.read() }
                    return Probe.Available(url, RemoteMeta(
                        length = length,
                        etag = conn.getHeaderField("ETag").orEmpty().trim(),
                        lastModified = conn.getHeaderField("Last-Modified").orEmpty().trim(),
                    ))
                }
                if (status == 403 || status == 404 || status == 410) sawMissing = url
            } catch (_: Throwable) {
                // از «فایل موجود نیست» تفکیک می‌شود.
            } finally {
                runCatching { conn.disconnect() }
            }
        }
        return sawMissing?.let { Probe.Missing(it) } ?: Probe.Unavailable
    }

    private fun head(url: String): RemoteMeta? =
        (probeRemote(listOf(url)) as? Probe.Available)?.meta

    private fun sameContent(file: File, url: String, saved: RemoteMeta?, remote: RemoteMeta): Boolean {
        if (remote.length >= 0L && remote.length != file.length()) return false
        if (saved != null) {
            if (saved.etag.isNotBlank() && remote.etag.isNotBlank()) {
                return saved.etag == remote.etag
            }
            if (saved.lastModified.isNotBlank() && remote.lastModified.isNotBlank()) {
                if (saved.lastModified != remote.lastModified) return false
                if (saved.length >= 0L && remote.length >= 0L) return saved.length == remote.length
            }
        }
        return compareRange(url, file, 0L) &&
            compareRange(url, file, (file.length() - SAMPLE_BYTES).coerceAtLeast(0L))
    }

    private fun compareRange(url: String, file: File, start: Long): Boolean {
        if (!file.isFile || start >= file.length()) return false
        val length = minOf(SAMPLE_BYTES.toLong(), file.length() - start).toInt()
        if (length <= 0) return false
        val conn = runCatching {
            (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("Range", "bytes=" + start + "-" + (start + length - 1))
            }
        }.getOrNull() ?: return false
        val remote = try {
            if (conn.responseCode != HttpURLConnection.HTTP_PARTIAL) return false
            conn.inputStream.use { input ->
                val bytes = ByteArray(length)
                var offset = 0
                while (offset < length) {
                    val n = input.read(bytes, offset, length - offset)
                    if (n <= 0) break
                    offset += n
                }
                if (offset != length) return false
                bytes
            }
        } catch (_: Throwable) {
            return false
        } finally {
            conn.disconnect()
        }
        val local = ByteArray(length)
        val count = runCatching {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                raf.read(local)
            }
        }.getOrDefault(-1)
        return count == length && local.contentEquals(remote)
    }

    private fun metaFile(target: File) = File(target.absolutePath + ".meta")

    private fun readMeta(target: File): RemoteMeta? = runCatching {
        val p = Properties()
        metaFile(target).inputStream().use(p::load)
        RemoteMeta(
            length = p.getProperty("length", "-1").toLong(),
            etag = p.getProperty("etag", ""),
            lastModified = p.getProperty("lastModified", ""),
            checkedAt = p.getProperty("checkedAt", "0").toLong(),
        )
    }.getOrNull()

    private fun writeMeta(target: File, meta: RemoteMeta) {
        runCatching {
            val temp = File(metaFile(target).absolutePath + ".tmp")
            val p = Properties().apply {
                setProperty("length", meta.length.toString())
                setProperty("etag", meta.etag)
                setProperty("lastModified", meta.lastModified)
                setProperty("checkedAt", meta.checkedAt.toString())
            }
            temp.outputStream().use { p.store(it, null) }
            val output = metaFile(target)
            if (!temp.renameTo(output)) {
                temp.copyTo(output, overwrite = true)
                temp.delete()
            }
        }
    }

    private fun replacePreservingPrevious(part: File, target: File): Boolean {
        val backup = File(target.absolutePath + ".bak")
        runCatching { backup.delete() }
        val hadPrevious = target.exists()
        if (hadPrevious && !target.renameTo(backup)) return false
        val installed = runCatching {
            if (part.renameTo(target)) true
            else {
                part.copyTo(target, overwrite = true)
                part.delete()
                true
            }
        }.getOrDefault(false)
        if (!installed || !isValid(target)) {
            runCatching { target.delete() }
            if (hadPrevious) runCatching { backup.renameTo(target) }
            return false
        }
        runCatching { backup.delete() }
        return true
    }
}
