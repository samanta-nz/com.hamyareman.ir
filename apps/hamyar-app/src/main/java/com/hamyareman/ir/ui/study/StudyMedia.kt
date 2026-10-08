package com.hamyareman.ir.ui.study

import com.hamyareman.ir.ui.content.ContentCatalog
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** مسیر رسانه‌های کتاب و صوت؛ فایل‌های محتوایی روی پارس‌پک هستند. */
object StudyMedia {
    // باکت Appwrite فقط برای ارسال فیش اشتراک استفاده می‌شود؛ نه HTMLهای محتوا.
    const val BUCKET = "6abb564d00155cc56d65"

    fun videoIds(packId: String): List<String> =
        listOf(packId.replace("_", "-") + "-V01.mp4")

    /**
     * کلید cache امن و یکتا برای یک شیء باکت.
     *
     * basename به‌تنهایی کافی نیست: مثلاً فایل‌های English و workbook نام مشابه
     * دارند. کتاب‌خوان و صفحهٔ دانلود هر دو دقیقاً از همین کلید استفاده می‌کنند
     * تا وضعیت «دانلود شده» واقعاً همان فایلی را نشان دهد که صفحهٔ کتاب می‌خواند.
     */
    fun bookCacheKey(kind: String, bucketKey: String): String {
        val safeKind = kind.filter { it.isLetterOrDigit() }.ifBlank { "media" }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(bucketKey.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            .take(20)
        return "$safeKind-$digest"
    }

    /**
     * URL فایل روی پارس‌پک. کلید کامل باکت مستقیم و شناسه‌های قدیمی از server-map
     * به کلید مسیر فعلی تبدیل می‌شوند؛ هیچ URL خارجی برای محتوا ساخته نمی‌شود.
     */
    fun candidateUrls(fileId: String): List<String> = when {
        fileId.startsWith("Bucket/") -> listOf(ServerResolver.internal(fileId))
        else -> ContentCatalog.keyFor(fileId)?.let { listOf(ServerResolver.internal(it)) }.orEmpty()
    }

    fun viewUrl(fileId: String): String = candidateUrls(fileId).firstOrNull().orEmpty()

    fun candidateIds(fileId: String): List<String> {
        if (fileId.isBlank()) return emptyList()
        if (fileId.startsWith("Bucket/")) return listOf(fileId)

        val out = linkedSetOf(fileId)
        Regex("""^ryazif(\d{2})d(\d{2})\.mp3$""").find(fileId)?.let { m ->
            out += "C905_E%02d-L%02d_AUDIO.mp3".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        Regex("""^ryazif(\d{2})review\.mp3$""").find(fileId)?.let { m ->
            out += "C905_E%02d-SUM_AUDIO.mp3".format(m.groupValues[1].toInt())
        }
        Regex("""^C905f(\d{2})d(\d{2})\.pdf$""").find(fileId)?.let { m ->
            out += "C905_E%02d-L%02d_BOOK.pdf".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        return out.toList()
    }

    private val resolved = ConcurrentHashMap<String, String>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    /** آیا هیچ‌کدام از نام‌های محتمل این صوت روی باکت هست؟ */
    fun audioExists(fileId: String): Boolean {
        if (fileId.isBlank()) return false
        if (missing.contains(fileId)) return false
        val ok = candidateIds(fileId).any { existsOnServer(it) }
        if (!ok) missing += fileId
        return ok
    }

    fun forgetMissing(fileId: String) { missing.remove(fileId) }

    fun resolveFileId(fileId: String): String {
        if (fileId.isBlank() || fileId.startsWith("Bucket/")) return fileId
        resolved[fileId]?.let { return it }
        for (id in candidateIds(fileId)) {
            if (existsOnServer(id)) {
                resolved[fileId] = id
                return id
            }
        }
        return fileId
    }

    fun existsOnServer(fileId: String): Boolean = candidateUrls(fileId).any { url ->
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                requestMethod = "HEAD"
            }
            conn.connect()
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        }.getOrDefault(false)
    }
}
