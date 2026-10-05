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

/**
 * کلیدِ هر تصویر همان مسیر روی باکت است: https://c539776.parspack.net/ + کلید.
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
@Composable
fun RemoteDesignImage(
    key: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val remote = remember(key) { DesignAsset.remoteUrl(key) }
    var source by remember(key) { mutableStateOf<Any>(remote) }

    LaunchedEffect(key) {
        source = if (readable(remote)) remote else DesignAsset.localUri(key)
    }

    AsyncImage(
        model = source,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
