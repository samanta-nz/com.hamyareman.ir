package com.hamyareman.ir.ui.study

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** cache فقط-ciphertext برای HTMLهای HMK1؛ plaintext فقط در حافظه برمی‌گردد. */
object RemoteHtmlCache {

    private const val TTL_MS = 6L * 60L * 60L * 1000L

    data class Loaded(
        val html: String,
        val source: ServerPrefs.Origin,
        val fromCache: Boolean,
    )

    data class Failure(val message: String)

    private fun sourceOf(url: String): ServerPrefs.Origin =
        if (url.startsWith(ServerResolver.INTERNAL_PUBLIC)) ServerPrefs.Origin.INTERNAL
        else ServerPrefs.Origin.EXTERNAL

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(32)

    private fun dir(ctx: Context) = File(ctx.filesDir, "html-cipher-cache").apply { mkdirs() }
    private fun file(ctx: Context, fileId: String, url: String) = File(dir(ctx), digest("$fileId|$url") + ".hmk1")

    fun clear(ctx: Context) {
        dir(ctx).listFiles()?.forEach { runCatching { it.delete() } }
    }

    private fun cachedPlain(ctx: Context, target: File): String? {
        if (!target.isFile || target.length() <= 32) return null
        if (System.currentTimeMillis() - target.lastModified() > TTL_MS) return null
        return runCatching {
            String(HtmlCodec.unwrap(ctx, target.readBytes()), Charsets.UTF_8)
        }.getOrElse {
            target.delete()
            null
        }
    }

    /**
     * در EXTERNAL/INTERNAL فقط همان origin؛ در FASTEST برنده و سپس fallback.
     * onProgress برای هر candidate از صفر آغاز می‌شود.
     */
    fun load(
        ctx: Context,
        fileId: String,
        internalKey: String?,
        onProgress: (downloaded: Long, total: Long, source: ServerPrefs.Origin) -> Unit = { _, _, _ -> },
    ): Result<Loaded> {
        val candidates = ServerResolver.candidates(fileId, internalKey)
        if (candidates.isEmpty()) {
            return Result.failure(IllegalStateException("این فایل روی سرور انتخاب‌شده نگاشت نشده است."))
        }
        val failures = mutableListOf<String>()
        for (url in candidates) {
            val source = sourceOf(url)
            val target = file(ctx, fileId, url)
            cachedPlain(ctx, target)?.let { return Result.success(Loaded(it, source, true)) }
            val part = File(target.absolutePath + ".part")
            part.delete()
            var conn: HttpURLConnection? = null
            val downloaded = runCatching {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 90_000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("Accept-Encoding", "identity")
                }
                val status = conn!!.responseCode
                if (status !in 200..299) error("HTTP $status")
                val total = conn!!.contentLengthLong.coerceAtLeast(0L)
                var count = 0L
                conn!!.inputStream.use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            count += n
                            onProgress(count, total, source)
                        }
                        output.fd.sync()
                    }
                }
                require(total <= 0L || count == total) { "دانلود ناقص: $count/$total" }
                require(part.length() == count && count > 32L) { "فایل ناقص" }
                val plain = HtmlCodec.unwrap(ctx, part.readBytes())
                val html = String(plain, Charsets.UTF_8)
                require(html.contains("<html", ignoreCase = true) || html.contains("<!doctype", ignoreCase = true)) {
                    "ساختار HTML معتبر نیست"
                }
                if (!part.renameTo(target)) {
                    part.copyTo(target, overwrite = true)
                    part.delete()
                }
                target.setLastModified(System.currentTimeMillis())
                Loaded(html, source, false)
            }
            runCatching { conn?.disconnect() }
            if (downloaded.isSuccess) return downloaded
            part.delete()
            failures += (downloaded.exceptionOrNull()?.message ?: "خطای نامشخص")
        }
        return Result.failure(IllegalStateException(failures.joinToString(" | ")))
    }
}
