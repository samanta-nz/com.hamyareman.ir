package com.hamyareman.ir.ui.wellness

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * پرامپت ۰۲ — کش فایل‌های صوتی TTS فارسی در حافظه‌ی داخلی.
 *
 * ساختار:
 *  - cacheDir/audio_cues/{md5(audioCueId_or_url)}.mp3
 *  - در اولین درخواست، از Storage دانلود می‌شود.
 *  - اگر نت (offline) نبود، null برمی‌گردد (تایمر بدون صدا ادامه می‌دهد).
 *
 * نکته: این کلاس **وابسته به اینترنت نیست** برای شروع کار. اگر فایل صوتی
 * دانلود نشد، تایمر فقط با شمارنده‌ی بصری و بوق کار می‌کند.
 */
object AudioCueCache {

    private const val CACHE_SUBDIR = "audio_cues"
    private const val PROJECT_ID = "6a9d59e3002751cc3ea8"
    private const val STORAGE_BASE = "https://fra.cloud.appwrite.io/v1/storage/buckets/wellness-media/files/"
    private const val FILE_VIEW_SUFFIX = "/view?project=$PROJECT_ID"

    /**
     * برگرداندن فایل محلی برای پخش.
     * @param key شناسه‌ی صوت (audioCueId یا URL) — هر چیزی که یکتا باشد.
     */
    fun getLocalFile(context: Context, key: String): File? {
        val cacheFile = cacheFile(context, key)
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile
        // تلاش برای دانلود
        val url = if (key.startsWith("http")) key else "$STORAGE_BASE$key$FILE_VIEW_SUFFIX"
        val downloaded = runCatching { downloadTo(url, cacheFile) }.getOrDefault(false)
        return if (downloaded) cacheFile else null
    }

    /**
     * از قبل فایل صوتی را دانلود می‌کند (برای دانلود گروهی در پس‌زمینه).
     * @return true اگر فایل از قبل موجود باشد یا با موفقیت دانلود شود.
     */
    fun prefetch(context: Context, key: String): Boolean {
        val cacheFile = cacheFile(context, key)
        if (cacheFile.exists() && cacheFile.length() > 0) return true
        val url = if (key.startsWith("http")) key else "$STORAGE_BASE$key$FILE_VIEW_SUFFIX"
        return runCatching { downloadTo(url, cacheFile) }.getOrDefault(false)
    }

    private fun cacheFile(context: Context, key: String): File {
        val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
        val hash = MessageDigest.getInstance("MD5")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$hash.mp3")
    }

    private fun downloadTo(url: String, dest: File): Boolean {
        var connection: HttpURLConnection? = null
        return runCatching {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 15000
            }
            if (connection!!.responseCode !in 200..299) return@runCatching false
            connection!!.inputStream.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            dest.length() > 0
        }.getOrDefault(false).also { connection?.disconnect() }
    }

    /** پاک کردن کل کش (مثلاً در تنظیمات حریم خصوصی). */
    fun clearAll(context: Context) {
        runCatching {
            val dir = File(context.cacheDir, CACHE_SUBDIR)
            if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
        }
    }
}
