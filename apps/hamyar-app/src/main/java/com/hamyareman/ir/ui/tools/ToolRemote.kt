package com.hamyareman.ir.ui.tools

import android.content.Context
import com.hamyareman.ir.ui.content.ContentCatalog
import com.hamyareman.ir.ui.profile.AppEdition
import com.hamyareman.ir.ui.study.RemoteHtmlCache
import java.io.File

/** ابزارهای HMK1 با انتخاب دقیق سرور و cache دائمیِ فقط-ciphertext. */
object ToolRemote {

    private val labs = setOf("chemistry", "physics", "biology")

    fun gradeToken(): String {
        val n = AppEdition.grade.num
        val folder = AppEdition.booksFolder
        val track = when {
            folder.contains("ریاضی") -> "r"
            folder.contains("تجربی") -> "t"
            folder.contains("انسانی") -> "e"
            else -> ""
        }
        return if (n >= 10 && track.isNotEmpty()) "$n$track" else "%02d".format(n)
    }

    fun fileId(toolId: String): String {
        val id = toolId.trim().lowercase().replace('_', '-')
        return if (id in labs) "lab-${gradeToken()}-$id.html" else "tool-$id.html"
    }

    /**
     * آیا این دستهٔ ابزار اصلاً آیتمی در کاتالوگ دارد؟ بعد از مهاجرت به پارس‌پک
     * دسته‌های بدون فایل از `catalog.json` حذف شدند، پس این تابع خودبه‌خود کارت
     * مربوط را پنهان می‌کند و با آپلود شدن فایل‌ها دوباره برمی‌گردد.
     */
    fun hasCategory(ctx: Context, cat: String): Boolean = runCatching {
        ContentCatalog.load(ctx)
        ContentCatalog.categories().any { it.id == cat }
    }.getOrDefault(false)

    /** آیا HTML همین ابزار برای edition این پایه در manifest دو سرور وجود دارد؟ */
    fun isAvailable(toolId: String): Boolean {
        val id = fileId(toolId)
        return ContentCatalog.keyFor(id) != null
    }

    private fun plainDir(ctx: Context) = File(ctx.cacheDir, "hamyar-tools-plain").apply { mkdirs() }
    private fun plainFile(ctx: Context, toolId: String) = File(plainDir(ctx), fileId(toolId))

    /** plaintext فقط در cacheDir موقت WebView است؛ نسخهٔ ماندگار HMK1 می‌ماند. */
    fun ensure(ctx: Context, toolId: String): String? {
        val id = fileId(toolId)
        val loaded = RemoteHtmlCache.load(ctx, id, ContentCatalog.keyFor(id)).getOrNull() ?: return null
        val dest = plainFile(ctx, toolId)
        val part = File(dest.absolutePath + ".part")
        val offlineHtml = loaded.html.replace(
            Regex("""https://cdn\.jsdelivr\.net/[^"']*jalaali[^"']*\.js""", RegexOption.IGNORE_CASE),
            "file:///android_asset/tools/vendor/jalaali.min.js",
        )
        return runCatching {
            part.writeText(offlineHtml, Charsets.UTF_8)
            if (!part.renameTo(dest)) {
                part.copyTo(dest, overwrite = true)
                part.delete()
            }
            dest.absolutePath
        }.getOrElse {
            part.delete()
            null
        }
    }

    fun release(ctx: Context, toolId: String) {
        runCatching { plainFile(ctx, toolId).delete() }
        runCatching { File(plainFile(ctx, toolId).absolutePath + ".part").delete() }
    }

    fun clearPlainCache(ctx: Context) {
        plainDir(ctx).listFiles()?.forEach { runCatching { it.delete() } }
        // پاکسازی cache plaintext نسخه‌های قدیمی در filesDir.
        File(ctx.filesDir, "hamyar-tools").deleteRecursively()
    }
}
