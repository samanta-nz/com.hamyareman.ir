package com.hamyareman.ir.ui.tools

import android.content.Context
import com.hamyareman.ir.ui.content.ContentCatalog
import com.hamyareman.ir.ui.profile.AppEdition
import com.hamyareman.ir.ui.study.HtmlCodec
import com.hamyareman.ir.ui.study.LessonCache
import com.hamyareman.ir.ui.study.ServerResolver
import java.io.File

/** ابزارهای HTML؛ cache مشترک payload اصلی را چه HMK1 باشد چه HTML عادی نگه می‌دارد. */
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

    /** خروجی اجرای محلی ابزار؛ کش مشترک، payload اصلی را با قالب واقعی خودش نگه می‌دارد. */
    fun ensure(ctx: Context, toolId: String): String? {
        val id = fileId(toolId)
        // از همان لایهٔ مشترک درس‌ها می‌خوانیم؛ RemoteHtmlCache بازنشسته شد.
        val key = ContentCatalog.keyFor(id) ?: return null
        val cached = LessonCache.ensure(ctx, ServerResolver.internal(key)) ?: return null
        val raw = runCatching { cached.readBytes() }.getOrNull() ?: return null
        val plain = runCatching { HtmlCodec.decodeHtml(ctx, raw) }.getOrNull() ?: return null
        val dest = plainFile(ctx, toolId)
        val part = File(dest.absolutePath + ".part")
        val offlineHtml = String(plain, Charsets.UTF_8).replace(
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
