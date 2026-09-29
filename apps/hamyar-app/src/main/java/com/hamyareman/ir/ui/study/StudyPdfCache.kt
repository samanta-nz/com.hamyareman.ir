package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import com.hamyareman.ir.ui.net.awaitOnlineBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile

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
        val target = file(ctx, fileId)
        if (isValid(target)) return target
        target.delete()
        val part = File(target.absolutePath + ".part")
        val remoteId = StudyMedia.resolveFileId(fileId)
        val urls = StudyMedia.candidateUrls(remoteId).distinct()
        require(urls.isNotEmpty()) { "برای PDF نشانی سرور وجود ندارد." }
        var last: Throwable? = null

        for ((originIndex, url) in urls.withIndex()) {
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
                    if (!part.renameTo(target)) {
                        part.copyTo(target, overwrite = true)
                        part.delete()
                    }
                    check(isValid(target)) { "اعتبارسنجی PDF نهایی شکست خورد." }
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
            if (originIndex < urls.lastIndex) continue
        }
        part.delete()
        throw IOException(last?.message ?: "دانلود PDF کامل نشد.", last)
    }
}
