package com.hamyareman.ir.ui.study

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Downloads exact source bytes. Resume is bound to a strong ETag; old cache survives every failure. */
internal object BucketByteDownloader {
    data class Result(val etag: String, val modified: Long, val length: Long)
    internal class InvalidContentException : IOException("قالب محتوای دریافتی معتبر نیست")
    private class Paused : IOException()
    fun download(url: String, target: File, initial: HttpURLConnection? = null,
        silent: Boolean = false, validate: (File) -> Boolean,
        onProgress: (Long, Long) -> Unit = { _, _ -> }): Result {
        target.parentFile?.mkdirs()
        val part = File(target.path + ".part")
        val tagFile = File(part.path + ".etag")
        val transfer = if (silent) null else BucketTransfer.begin(url)
        var connection = initial
        var failures = 0
        try {
            while (true) {
                transfer?.let(BucketTransfer::checkpoint)
                val offset = if (tagFile.exists() && part.exists()) part.length() else 0L
                val savedTag = if (offset > 0L) tagFile.readText() else ""
                if (offset > 0L && connection != null) { connection.disconnect(); connection = null }
                val c = connection ?: (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000; readTimeout = 15_000; useCaches = false
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Cache-Control", "no-cache")
                    if (offset > 0L && savedTag.isNotBlank() && !savedTag.startsWith("W/")) {
                        setRequestProperty("Range", "bytes=$offset-")
                        setRequestProperty("If-Range", savedTag)
                    }
                }
                connection = c
                try {
                    val code = c.responseCode
                    if (code == 404 || code == 410) throw StudyPdfCache.RemoteMissingException(url)
                    if (code !in 200..299) throw IOException("HTTP $code")
                    val etag = c.getHeaderField("ETag").orEmpty()
                    val range = c.getHeaderField("Content-Range").orEmpty()
                    val append = code == 206 && offset > 0 && range.startsWith("bytes $offset-") && etag == savedTag
                    if (code == 206 && !append) throw IOException("بازهٔ دانلود معتبر نیست")
                    val total = if (code == 206) range.substringAfterLast('/').toLongOrNull() ?: -1L else c.contentLengthLong
                    if (etag.isNotBlank() && !etag.startsWith("W/")) tagFile.writeText(etag) else tagFile.delete()
                    var done = if (append) offset else 0L
                    FileOutputStream(part, append).use { output -> c.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            if (transfer != null && BucketTransfer.isPaused(transfer)) throw Paused()
                            transfer?.let(BucketTransfer::checkpoint)
                            val count = input.read(buffer); if (count < 0) break
                            output.write(buffer, 0, count); done += count
                            transfer?.let { BucketTransfer.progress(it, done, total) }
                            onProgress(done, total)
                        }
                        output.fd.sync()
                    } }
                    if (total >= 0L && part.length() != total) throw IOException("دانلود ناقص است")
                    if (!validate(part)) { part.delete(); tagFile.delete(); throw InvalidContentException() }
                    val result = Result(etag, c.lastModified, part.length())
                    Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    tagFile.delete()
                    return result
                } catch (paused: Paused) {
                    c.disconnect(); connection = null
                    transfer?.let(BucketTransfer::checkpoint)
                } catch (cancelled: BucketTransfer.Cancelled) { throw cancelled }
                catch (missing: StudyPdfCache.RemoteMissingException) { throw missing }
                catch (invalid: InvalidContentException) { throw invalid }
                catch (error: IOException) {
                    c.disconnect(); connection = null
                    if (++failures >= 3 || error.message.orEmpty().startsWith("HTTP 4")) {
                        if (transfer == null || !BucketTransfer.retryAfterFailure(transfer)) throw error
                        failures = 0
                    }
                } finally { c.disconnect(); connection = null }
            }
        } finally { connection?.disconnect(); transfer?.let(BucketTransfer::finish) }
    }
}
