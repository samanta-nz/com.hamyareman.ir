package com.hamyareman.ir.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.hamyareman.ir.ui.study.ServerResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * کلیدِ هر تصویر همان مسیر روی باکت است: https://c539776.parspack.net/ + کلید.
 * نسخهٔ محلیِ داخل APK (در صورت وجود) زیر `src/main/assets/` و بدون پیشوند `assets/` است.
 */
internal object DesignAsset {
    const val DIARY_PACK = "diary-assets.zip"
    // قفل‌ها (۱۰۸۰×۲۳۴۰) — اگر روی باکت نباشند، صحنهٔ رسم‌شده با کد دیده می‌شود.
    const val LOCK_APP = "assets/lock/app-lock-bg.jpg"
    const val LOCK_SAFE = "assets/lock/safespace-lock-bg.jpg"

    // دفتر خاطرات / شعر / آلبوم و گالری جزوه‌ها
    const val DIARY_DESK = "assets/diary/desk-bg.jpg"
    const val POETRY_BG = "assets/poetry/poetry-bg.jpg"
    const val ALBUM_BG = "assets/album/album-bg.jpg"
    const val PAPER_CREAM = "assets/diary/paper-cream.jpg"

    // کتاب‌های PNG دوربری‌شده (بوم مشترک ۱۰۵۹×۱۴۸۶، دقیقاً روی هم می‌نشینند):
    // <name>.png جلد بسته، -inside کتاب باز، -sheet ورق تکی با خط‌های اندازه‌گیری‌شده.
    const val COVER_NAVY_FLORAL = "assets/diary/cover-navy-floral.png"
    const val COVER_NAVY_FLORAL_INSIDE = "assets/diary/cover-navy-floral-inside.png"
    const val COVER_NAVY_FLORAL_SHEET = "assets/diary/cover-navy-floral-sheet.png"
    const val COVER_LEATHER = "assets/diary/cover-leather-brown.png"
    const val COVER_LEATHER_INSIDE = "assets/diary/cover-leather-brown-inside.png"
    const val COVER_LEATHER_SHEET = "assets/diary/cover-leather-brown-sheet.png"

    // جلدهای قدیمی (برای سازگاری)
    const val COVER_CELESTIAL = "assets/diary/cover-celestial.jpg"
    const val COVER_BOTANICAL = "assets/diary/cover-botanical.jpg"
    const val COVER_GEOMETRIC = "assets/diary/cover-geometric.jpg"
    const val PAGE_LINED = "assets/diary/page-lined.jpg"

    /**
     * جلد/ورق کتاب‌های PNG داخل خود APK امبد شده‌اند و بدون شبکه و بدون انتظار برای باکت
     * نمایش داده می‌شوند (دفتر شعر چرمی و دفتر خاطرات سرمه‌ای).
     */
    fun isEmbeddedBook(key: String): Boolean =
        key.endsWith(".png") && key.substringAfterLast('/').startsWith("cover-")

    fun remoteUrl(key: String): String =
        ServerResolver.INTERNAL_PUBLIC.trimEnd('/') + "/" +
            key.split('/').joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }

    /** فایل‌های داخل APK زیر assets/diary/... هستند، نه assets/assets/diary/... */
    fun localUri(key: String): String =
        "file:///android_asset/" + key.removePrefix("assets/")
}

private suspend fun readable(url: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 4_000
        c.readTimeout = 4_000
        c.instanceFollowRedirects = true
        c.requestMethod = "GET"
        c.setRequestProperty("Range", "bytes=0-1")
        val ok = c.responseCode == 200 || c.responseCode == 206
        c.disconnect()
        ok
    }.getOrDefault(false)
}

/**
 * طراحی از باکت داخلی/سرور اولویت دارد؛ در صورت قطع سرور، همان asset محلی به‌عنوان
 * fallback استفاده می‌شود تا UI هرگز به جای تصویر سفید یا placeholder نرود.
 */
private fun diaryFileName(key: String): String =
    key.substringAfterLast('/').takeIf { it.endsWith(".png") && !it.contains("..") } ?: ""

private fun extractDiaryAsset(context: android.content.Context, key: String): String? {
    if (!DesignAsset.isEmbeddedBook(key)) return null
    val name = diaryFileName(key)
    if (name.isBlank()) return null
    val dir = File(context.cacheDir, "hamyar-diary-assets")
    val target = File(dir, name)
    if (target.exists() && target.length() > 1024L) {
        return target.toURI().toString()
    }

    return runCatching {
        dir.mkdirs()
        ZipInputStream(context.assets.open(DesignAsset.DIARY_PACK).buffered()).use { zis ->
            var found = false
            while (true) {
                val entry = zis.nextEntry ?: break
                val entryName = entry.name
                if (!entry.isDirectory &&
                    entryName == "diary/$name" &&
                    entryName.substringAfterLast('/').equals(name)
                ) {
                    val tmp = File(dir, ".$name.tmp")
                    FileOutputStream(tmp).use { out -> zis.copyTo(out) }
                    if (tmp.length() <= 0L) error("empty diary asset")
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                    found = true
                    break
                }
            }
            if (!found) return@runCatching null
        }
        target.takeIf { it.exists() && it.length() > 1024L }?.toURI()?.toString()
    }.getOrNull()
}

@Composable
fun RemoteDesignImage(
    key: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val remote = remember(key) { DesignAsset.remoteUrl(key) }
    val embedded = remember(key) { DesignAsset.isEmbeddedBook(key) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var source by remember(key) {
        mutableStateOf<Any>(remote)
    }

    LaunchedEffect(key) {
        source = if (embedded) {
            withContext(Dispatchers.IO) {
                extractDiaryAsset(context, key)
            } ?: remote
        } else {
            if (readable(remote)) remote else DesignAsset.localUri(key)
        }
    }

    AsyncImage(
        model = source,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
