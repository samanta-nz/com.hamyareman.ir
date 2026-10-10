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

private val syncedThisProcess = ConcurrentHashMap.newKeySet<String>()

private fun coverFile(context: Context, key: String): File {
    val dir = File(context.filesDir, "design-covers").apply { mkdirs() }
    return File(dir, key.substringAfterLast('/'))
}

/** نسخهٔ محلیِ موجود (اگر باشد) بدون هیچ شبکه‌ای. */
private fun localCoverUri(context: Context, key: String): String? {
    val file = coverFile(context, key)
    return file.takeIf { it.exists() && it.length() > 1024L }?.toURI()?.toString()
}

/**
 * جلد را از باکت می‌گیرد و روی دیسک نگه می‌دارد. هر جلد در هر اجرای اپ یک‌بار با GET شرطی
 * (`If-None-Match`) اعتبارسنجی می‌شود: ۳۰۴ یعنی همان نسخه، ۲۰۰ یعنی فایل تازه در همان آدرس.
 * اشکال شبکه با نسخهٔ محلیِ موجود، همان نسخه را برمی‌گرداند؛ بدون نسخهٔ محلی `null`.
 */
private fun syncCover(context: Context, key: String): String? {
    val target = coverFile(context, key)
    val etagFile = File(target.path + ".etag")
    val local = localCoverUri(context, key)
    if (local != null && syncedThisProcess.contains(key)) return local

    var conn: HttpURLConnection? = null
    try {
        conn = (URL(DesignAsset.remoteUrl(key)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-cache")
            if (local != null) {
                val etag = runCatching { etagFile.readText().trim() }.getOrDefault("")
                if (etag.isNotEmpty()) setRequestProperty("If-None-Match", etag)
            }
        }
        val code = conn.responseCode
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED && local != null) {
            syncedThisProcess.add(key)
            return local
        }
        if (code !in 200..299) {
            // Older graphic uploads may still be PNG. Preserve the cached WebP when present;
            // on a first install, try the matching PNG before giving up.
            if (local != null) return local
            if (key.endsWith(".webp")) {
                return syncCover(context, key.removeSuffix(".webp") + ".png")
            }
            return null
        }

        val etag = conn.getHeaderField("ETag").orEmpty()
        val tmp = File(target.path + ".tmp")
        runCatching { tmp.delete() }
        conn.inputStream.use { input ->
            FileOutputStream(tmp).use { output -> input.copyTo(output) }
        }
        if (tmp.length() <= 1024L) {
            tmp.delete()
            return local
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        runCatching { etagFile.writeText(etag) }
        syncedThisProcess.add(key)
        return localCoverUri(context, key)
    } catch (_: Throwable) {
        return local
    } finally {
        runCatching { conn?.disconnect() }
    }
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
        mutableStateOf<Any?>(if (embedded) localCoverUri(context, key) else remote)
    }

    LaunchedEffect(key) {
        source = if (embedded) {
            withContext(Dispatchers.IO) { syncCover(context, key) } ?: remote
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
