package com.hamyareman.ir.ui.study

import com.hamyareman.ir.ui.content.ContentCatalog
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * نقشه‌ی رسانه‌های درس‌ها — صوت/ویدیوی هر پک از باکت عمومی Appwrite.
 * ریاضی نهم: نام جدید روی دیسک (`ryazif01d01.mp3` / `C905f01d01.pdf`)؛
 * اگر در باکت نبود، نام قدیمی `*_AUDIO.mp3` / `*_BOOK.pdf` امتحان می‌شود.
 */
object StudyMedia {
    const val BUCKET = "6aa1eaae00303400117b"
    private const val PROJECT = "6a9d59e3002751cc3ea8"
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"

    fun videoIds(packId: String): List<String> =
        listOf("${packId.replace("_", "-")}-V01.mp4")

    /** نشانی خام Appwrite — مبدأِ حقیقت (کاوش وجود فایل و کاندیداهای قدیمی). */
    fun externalUrl(fileId: String): String =
        "$ENDPOINT/storage/buckets/$BUCKET/files/$fileId/view?project=$PROJECT"

    /**
     * نشانی دوآدرسی: طبق «تنظیمات سرور» از سرور ایرانی (کلید catalog.json) یا Appwrite.
     * فایل‌های بدون کلید داخلی (آواتار، ویدیو، سفارشی‌ها) همیشه خارجی می‌مانند.
     */
    fun viewUrl(fileId: String): String =
        ServerResolver.pick(fileId, ContentCatalog.keyFor(fileId))

    fun candidateIds(fileId: String): List<String> {
        if (fileId.isBlank()) return emptyList()
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
    /** فایل‌هایی که روی سرور نیستند — تا هر بار درخواستِ بیهوده نفرستیم. */
    private val missing = ConcurrentHashMap.newKeySet<String>()

    /**
     * آیا هیچ‌کدام از نام‌های محتمل این صوت روی باکت هست؟
     * (پیش‌نمایش قبل از پخش تا پلیر روی فایلِ غایب گیر نکند.)
     */
    fun audioExists(fileId: String): Boolean {
        if (fileId.isBlank()) return false
        if (missing.contains(fileId)) return false
        val ok = candidateIds(fileId).any { existsOnServer(it) }
        if (!ok) missing += fileId
        return ok
    }

    /** پاک‌کردن حافظه‌ی «نیست» — برای دکمه‌ی بررسی دوباره. */
    fun forgetMissing(fileId: String) { missing.remove(fileId) }

    fun resolveFileId(fileId: String): String {
        if (fileId.isBlank()) return fileId
        resolved[fileId]?.let { return it }
        for (id in candidateIds(fileId)) {
            if (existsOnServer(id)) {
                resolved[fileId] = id
                return id
            }
        }
        return fileId
    }

    fun existsOnServer(fileId: String): Boolean {
        return runCatching {
            val conn = (URL(externalUrl(fileId)).openConnection() as HttpURLConnection).apply {
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