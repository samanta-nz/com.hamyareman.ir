package com.hamyareman.ir.ui.appearance

import android.content.Context
import android.util.Log
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.hamyareman.ir.platform.core.common.LocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * کتابخانه‌ی فونت: فونت‌های فارسی قابل‌دانلود از اینترنت (منابع آزاد گیت‌هاب)
 * با کش محلی در filesDir/fonts. پس از دانلود، FontFamily ساخته و در UiPrefs
 * کلیدش ذخیره می‌شود؛ در اجراهای بعدی از کش خوانده می‌شود (بدون اینترنت).
 */
object FontLibrary {

    data class Entry(val key: String, val title: String, val url: String)

    /** منابع آزاد (ریلیزهای رسمی در گیت‌هاب) */
    val catalog = listOf(
        Entry(
            "vazirmatn", "وزیرمتن (پیشنهادی)",
            "https://github.com/rastikerdar/vazirmatn/releases/download/v33.003/vazirmatn-v33.003.zip",
        ),
        Entry(
            "lalezar", "لاله‌زار (تیترها)",
            "https://github.com/BornaIz/Lalezar/raw/master/fonts/ttf/Lalezar-Regular.ttf",
        ),
        Entry(
            "markazi", "مرکزی (متن کتاب‌مانند)",
            "https://github.com/google/fonts/raw/main/ofl/markazitext/MarkaziText%5Bwght%5D.ttf",
        ),
    )

    private const val DIR = "fonts"

    fun downloadedFile(context: Context, key: String): File =
        File(File(context.filesDir, DIR), "$key.ttf")

    fun isDownloaded(context: Context, key: String): Boolean =
        downloadedFile(context, key).length() > 10_000

    /** دانلود با کش؛ خروجی: پیام موفقیت/خطا برای نمایش */
    suspend fun download(context: Context, entry: Entry): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            val out = downloadedFile(context, entry.key)
            if (out.length() > 10_000) return@runCatching out
            val conn = URL(entry.url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "HamyarApp/1.0")
            conn.connect()
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            conn.inputStream.use { input -> out.outputStream().use { output -> input.copyTo(output) } }
            if (out.length() < 10_000) error("فایل دانلودشده ناقص است")
            out
        }.onFailure { Log.w("FontLibrary", "دانلود ${entry.key} ناموفق: ${it.message}") }
    }

    private val cache = mutableMapOf<String, FontFamily>()

    /** فونت انتخابی؛ اگر نبود فونت سیستم (با کش) */
    @Synchronized
    fun fontFamilyFor(context: Context, key: String): FontFamily {
        if (key.isBlank()) return FontFamily.Default
        cache[key]?.let { return it }
        val f = downloadedFile(context, key)
        if (!isDownloaded(context, key)) return FontFamily.Default
        val fam = runCatching { FontFamily(Font(f)) }.getOrDefault(FontFamily.Default)
        cache[key] = fam
        return fam
    }

    fun titleOf(key: String): String = catalog.firstOrNull { it.key == key }?.title ?: "سیستم"

    fun delete(context: Context, key: String) {
        downloadedFile(context, key).delete()
    }
}
