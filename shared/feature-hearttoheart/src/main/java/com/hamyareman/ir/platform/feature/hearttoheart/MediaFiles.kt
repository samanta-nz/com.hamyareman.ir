package com.hamyareman.ir.platform.feature.hearttoheart

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * کپی محتوای یک Uri به حافظه‌ی موقت اپ.
 *
 * لازم است چون `InputFile.fromPath` مسیر واقعی فایل می‌خواهد و Uri های گالری
 * معمولاً content:// هستند.
 */
object MediaFiles {

    fun copyToCache(context: Context, uri: Uri, prefix: String = "media"): File? = runCatching {
        val dir = File(context.cacheDir, "heart").apply { mkdirs() }
        val extension = context.contentResolver.getType(uri)?.substringAfterLast('/')?.take(4) ?: "bin"
        val target = File(dir, "${prefix}_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return@runCatching null
        target.takeIf { it.exists() && it.length() > 0 }
    }.getOrNull()

    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()

    /** سقف منطقی برای رسانه‌ی حرف دل تا آپلود طولانی نشود (۱۰ مگابایت). */
    const val MAX_BYTES = 10L * 1024 * 1024

    fun isWithinLimit(file: File): Boolean = file.length() in 1..MAX_BYTES
}
