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
        forceRefresh: Boolean = false,
    ): File {
        val target = file(ctx, cacheId)
        val part = File(target.absolutePath + ".part")
        val metaFile = File(target.absolutePath + ".meta")
        val candidates = urls.distinct().filter { it.isNotBlank() }
        require(candidates.isNotEmpty()) { "برای PDF نشانی سرور وجود ندارد." }

        val hasValidCache = isValid(target)
        var expectedMeta: RemoteMeta? = null
        if (hasValidCache && !forceRefresh) {
            val oldMeta = readRemoteMeta(metaFile)
            val now = System.currentTimeMillis()
            if (oldMeta != null && now >= oldMeta.checkedAt && now - oldMeta.checkedAt < CHECK_TTL_MS) {
                return target
            }
            when (val probe = probe(candidates)) {
                is ProbeResult.Missing -> throw IOException("HTTP " + probe.status)
                ProbeResult.Unavailable -> return target
                is ProbeResult.Available -> {
                    expectedMeta = probe.meta
                    if (sameContent(probe.url, target, oldMeta, probe.meta)) {
                        writeRemoteMeta(metaFile, probe.meta.copy(
                            length = probe.meta.length.takeIf { it >= 0L } ?: target.length(),
                            checkedAt = now,
                        ))
                        return target
                    }
                    // Remote object changed: never resume a partial file from the previous version.
                    part.delete()
                }
            }
        } else if (!hasValidCache) {
            target.delete()
            metaFile.delete()
            // A partial left by an earlier process has no proven version identity.
            part.delete()
        } else if (forceRefresh) {
            // Force refresh never resumes a partial from an unknown prior version.
            part.delete()
        }

        var last: Throwable? = null

        for ((originIndex, url) in candidates.withIndex()) {
            var attempts = 0
            while (attempts < 7) {
                val offset = part.takeIf { it.isFile }?.length() ?: 0L
                var conn: java.net.HttpURLConnection? = null
                try {
                    val active = ResilientHttp.open(
                        url,
                        range = if (offset > 0) "bytes=$offset-" else null,
                        connectMs = 15_000,
                        readMs = 30_000,
                        attempts = 3,
                    )
                    conn = active
                    val status = active.responseCode
                    if (status !in 200..299) throw IOException("HTTP $status")
                    val responseMeta = metaFrom(active)
                    if (expectedMeta?.etag?.isNotBlank() == true && responseMeta.etag.isNotBlank() &&
                        expectedMeta?.etag != responseMeta.etag
                    ) throw IOException("نسخهٔ PDF حین دانلود تغییر کرد؛ دوباره تلاش شود.")
                    val append = offset > 0 && status == 206
                    if (offset > 0 && !append) part.delete()
                    val base = if (append) offset else 0L
                    val total = active.contentLengthLong.let { if (it > 0) base + it else -1L }
                    FileOutputStream(part, append).use { output ->
                        active.inputStream.use { input ->
                            val buffer = ByteArray(64 * 1024)
                            var written = base
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                written += read
                                if (total > 0) onProgress(((written * 100L) / total).toInt().coerceIn(0, 100))
                            }
                            output.fd.sync()
                        }
                    }
                    if (total > 0 && part.length() < total) throw IOException("دانلود ناقص ${part.length()}/$total")
                    if (!isValid(part)) throw IOException("PDF ناقص یا نامعتبر است")
                    replaceVerified(part, target)
                    check(isValid(target)) { "اعتبارسنجی PDF نهایی شکست خورد." }
                    val finalMeta = probe(candidates).let { result ->
                        when (result) {
                            is ProbeResult.Available -> result.meta
                            else -> responseMeta
                        }
                    }
                    writeRemoteMeta(metaFile, finalMeta.copy(
                        length = target.length(),
                        etag = finalMeta.etag.ifBlank { responseMeta.etag },
                        lastModified = finalMeta.lastModified.ifBlank { responseMeta.lastModified },
                        checkedAt = System.currentTimeMillis(),
                    ))
                    onProgress(100)
                    return target
                } catch (error: Throwable) {
                    last = error
                    attempts = if (error is IOException && error.message.orEmpty().startsWith("HTTP 4")) 7 else attempts + 1
                    if (!NetState.isOnline(ctx)) NetState.awaitOnlineBlocking(ctx)
                    if (attempts < 7) Thread.sleep((400L * attempts).coerceAtMost(3_000L))
                } finally {
                    runCatching { conn?.disconnect() }
                }
            }
            if (originIndex < candidates.lastIndex) continue
        }
        part.delete()
        // A valid old cache is still usable when an update download fails. It was never
        // removed while the replacement was being verified.
        if (hasValidCache && isValid(target)) return target
        throw IOException(last?.message ?: "دانلود PDF کامل نشد.", last)
    }

    private const val CHECK_TTL_MS = 30_000L

    private data class RemoteMeta(
        val length: Long = -1L,
        val etag: String = "",
        val lastModified: String = "",
        val checkedAt: Long = 0L,
    )

    private sealed interface ProbeResult {
        data class Available(val meta: RemoteMeta, val url: String) : ProbeResult
        data class Missing(val status: Int) : ProbeResult
        data object Unavailable : ProbeResult
    }

    private fun probe(urls: List<String>): ProbeResult {
        var sawMissing: Int? = null
        for (url in urls.distinct()) {
            when (val result = probeFor(url)) {
                is ProbeResult.Available -> return result
                is ProbeResult.Missing -> sawMissing = result.status
                ProbeResult.Unavailable -> Unit
            }
        }
        return sawMissing?.let { ProbeResult.Missing(it) } ?: ProbeResult.Unavailable
    }

    private fun probeFor(url: String): ProbeResult {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                requestMethod = "HEAD"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            val status = conn.responseCode
            when {
                status in 200..299 -> ProbeResult.Available(metaFrom(conn), url)
                status == 403 || status == 404 || status == 410 -> ProbeResult.Missing(status)
                status == 405 || status == 501 -> probeRange(url)
                else -> ProbeResult.Unavailable
            }
        } catch (_: Throwable) {
            ProbeResult.Unavailable
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun probeRange(url: String): ProbeResult {
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
            when (val status = conn.responseCode) {
                HttpURLConnection.HTTP_PARTIAL -> ProbeResult.Available(metaFrom(conn), url)
                403, 404, 410 -> ProbeResult.Missing(status)
                else -> ProbeResult.Unavailable
            }
        } catch (_: Throwable) {
            ProbeResult.Unavailable
        } finally {
            runCatching { conn?.inputStream?.close() }
            runCatching { conn?.disconnect() }
        }
    }

    private fun metaFrom(conn: HttpURLConnection): RemoteMeta {
        val rangeTotal = conn.getHeaderField("Content-Range")
            ?.substringAfterLast('/', "")
            ?.toLongOrNull()
        return RemoteMeta(
            length = rangeTotal ?: conn.getHeaderField("Content-Length")?.toLongOrNull()
                ?: conn.contentLengthLong,
            etag = conn.getHeaderField("ETag").orEmpty().trim(),
            lastModified = conn.getHeaderField("Last-Modified").orEmpty().trim(),
        )
    }

    private fun sameContent(url: String, local: File, saved: RemoteMeta?, remote: RemoteMeta): Boolean {
        if (remote.length >= 0L && remote.length != local.length()) return false
        if (saved != null) {
            if (saved.etag.isNotBlank() && remote.etag.isNotBlank()) return saved.etag == remote.etag
            if (saved.lastModified.isNotBlank() && remote.lastModified.isNotBlank()) {
                return saved.lastModified == remote.lastModified
            }
        }
        return rangeEquals(url, local, 0L) &&
            rangeEquals(url, local, (local.length() - PROBE_BYTES).coerceAtLeast(0L))
    }

    private fun rangeEquals(url: String, local: File, start: Long): Boolean {
        if (local.length() <= start) return false
        val count = minOf(PROBE_BYTES.toLong(), local.length() - start).toInt()
        var conn: HttpURLConnection? = null
        val bytes = try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=" + start + "-" + (start + count - 1))
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_PARTIAL) return false
            conn.inputStream.use { input ->
                val result = ByteArray(count)
                var offset = 0
                while (offset < count) {
                    val read = input.read(result, offset, count - offset)
                    if (read <= 0) break
                    offset += read
                }
                if (offset != count) return false
                result
            }
        } catch (_: Throwable) {
            return false
        } finally {
            runCatching { conn?.disconnect() }
        }
        val localBytes = ByteArray(count)
        val read = runCatching {
            RandomAccessFile(local, "r").use { raf -> raf.seek(start); raf.read(localBytes) }
        }.getOrDefault(-1)
        return read == count && localBytes.contentEquals(bytes)
    }

    private fun readRemoteMeta(file: File): RemoteMeta? = runCatching {
        if (!file.isFile) return null
        val p = Properties()
        file.inputStream().use { p.load(it) }
        RemoteMeta(
            length = p.getProperty("length", "-1").toLong(),
            etag = p.getProperty("etag", ""),
            lastModified = p.getProperty("lastModified", ""),
            checkedAt = p.getProperty("checkedAt", "0").toLong(),
        )
    }.getOrNull()

    private fun writeRemoteMeta(file: File, meta: RemoteMeta) {
        runCatching {
            val temp = File(file.path + ".tmp")
            val p = Properties()
            p.setProperty("length", meta.length.toString())
            p.setProperty("etag", meta.etag)
            p.setProperty("lastModified", meta.lastModified)
            p.setProperty("checkedAt", meta.checkedAt.toString())
            temp.outputStream().use { p.store(it, null) }
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    private fun replaceVerified(part: File, target: File) {
        if (part.renameTo(target)) return
        val backup = File(target.path + ".old")
        backup.delete()
        if (target.exists() && !target.renameTo(backup)) throw IOException("نسخهٔ قبلی PDF حفظ نشد.")
        if (part.renameTo(target)) {
            backup.delete()
            return
        }
        if (part.isFile) part.copyTo(target, overwrite = true)
        if (!isValid(target)) {
            target.delete()
            if (backup.exists()) backup.renameTo(target)
            throw IOException("جایگزینی امن PDF انجام نشد.")
        }
        backup.delete()
    }

    private const val PROBE_BYTES = 8 * 1024
}
