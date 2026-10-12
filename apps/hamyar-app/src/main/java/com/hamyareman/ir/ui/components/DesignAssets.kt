package com.hamyareman.ir.ui.components

import android.content.Context
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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * کلید هر تصویر همان مسیر روی باکت است: https://c539776.parspack.net/ + کلید.
 * نسخهٔ محلیِ داخل APK (در صورت وجود) زیر `src/main/assets/` و بدون پیشوند `assets/` است.
 */
internal object DesignAsset {
    // قفل‌ها (۱۰۸۰×۲۳۴۰) — اگر روی باکت نباشند، صحنهٔ رسم‌شده با کد دیده می‌شود.
    const val LOCK_APP = "assets/lock/app-lock-bg.jpg"
    const val LOCK_SAFE = "assets/lock/safespace-lock-bg.jpg"

    // دفتر خاطرات / شعر / آلبوم و گالری جزوه‌ها
    const val DIARY_DESK = "assets/diary/desk-bg.jpg"
    const val POETRY_BG = "assets/poetry/poetry-bg.jpg"
    const val ALBUM_BG = "assets/album/album-bg.jpg"
    const val PAPER_CREAM = "assets/diary/paper-cream.jpg"

    // کتاب‌های دوربری‌شده (WebP با شفافیت، بوم مشترک ۱۰۵۹×۱۴۸۶، دقیقاً روی هم می‌نشینند):
    // <n>.webp جلد بسته، -inside کتاب باز، -sheet ورق تکی با خط‌های اندازه‌گیری‌شده.
    const val COVER_NAVY_FLORAL = "assets/diary/cover-navy-floral.webp"
    const val COVER_NAVY_FLORAL_INSIDE = "assets/diary/cover-navy-floral-inside.webp"
    const val COVER_NAVY_FLORAL_SHEET = "assets/diary/cover-navy-floral-sheet.webp"
    const val COVER_LEATHER = "assets/diary/cover-leather-brown.webp"
    const val COVER_LEATHER_INSIDE = "assets/diary/cover-leather-brown-inside.webp"
    const val COVER_LEATHER_SHEET = "assets/diary/cover-leather-brown-sheet.webp"

    // جلدهای قدیمی (برای سازگاری)
    const val COVER_CELESTIAL = "assets/diary/cover-celestial.jpg"
    const val COVER_BOTANICAL = "assets/diary/cover-botanical.jpg"
    const val COVER_GEOMETRIC = "assets/diary/cover-geometric.jpg"
    const val PAGE_LINED = "assets/diary/page-lined.jpg"

    /**
     * جلد/ورق کتاب‌های WebP (دفتر شعر چرمی و دفتر خاطرات سرمه‌ای): در APK نیستند؛ یک‌بار از
     * باکت می‌آیند، روی دیسک می‌نشینند و با یک GET شرطی (ETag) تازه می‌مانند.
     */
    fun isEmbeddedBook(key: String): Boolean =
        key.endsWith(".webp") && key.substringAfterLast('/').startsWith("cover-")

    fun remoteUrl(key: String): String =
        ServerResolver.INTERNAL_PUBLIC.trimEnd('/') + "/" +
            key.split('/').joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }

    /** فایل‌های داخل APK زیر assets/diary/... هستند، نه assets/assets/diary/... */
    fun localUri(key: String): String =
        "file:///android_asset/" + key.removePrefix("assets/")
}

@Composable
fun RemoteDesignImage(key: String, modifier: Modifier = Modifier, contentDescription: String? = null, contentScale: ContentScale = ContentScale.Crop) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val remote = remember(key) { DesignAsset.remoteUrl(key) }
    val file = com.hamyareman.ir.ui.study.LessonCache.fileFor(context, remote)
    var revision by remember(key) { mutableStateOf(file.lastModified()) }
    var ready by remember(key) { mutableStateOf(file.takeIf { it.exists() }) }
    LaunchedEffect(key) {
        val result = withContext(Dispatchers.IO) { com.hamyareman.ir.ui.study.LessonCache.prepare(context, remote) }
        ready = result?.file; revision = ready?.lastModified() ?: 0L
    }
    AsyncImage(model = coil.request.ImageRequest.Builder(context).data(ready)
        .memoryCacheKey(remote + "@" + revision).build(), contentDescription = contentDescription,
        contentScale = contentScale, modifier = modifier)
}
