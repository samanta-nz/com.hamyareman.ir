package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * «تازه‌بودنِ محتوای دانلودشده» — کانالِ محتواییِ آپدیت، بدونِ APK.
 *
 * مسئله: فایل‌های صوتی/PDFِ تدریس روی سرور عوض می‌شوند (بازنویسیِ درس، اصلاحِ
 * PDF، صوتِ بهتر). نسخه‌ی دانلودشده‌ی روی گوشی بی‌خبر کهنه می‌ماند و کاربر
 * فکر می‌کند محتوا همان است.
 *
 * راه‌حل: Appwrite برای هر فایل یک `signature` (اثرِ انگشتِ محتوا) و
 * `sizeOriginal` می‌دهد و همین متادیتا **بدونِ کلیدِ API** خوانده می‌شود
 * (آزمونِ زنده: `GET /storage/buckets/<b>/files/<f>` با فقط هدرِ project و بدونِ
 * احراز هویت، ۲۰۰ می‌دهد). پس:
 *  ۱. هنگامِ هر دانلودِ موفق، اثرِ انگشتِ سرور برای آن کلید ذخیره می‌شود؛
 *  ۲. هنگامِ بررسی، اثرِ انگشتِ فعلیِ سرور با ذخیره‌شده مقایسه می‌شود؛
 *  ۳. تفاوت ⇒ فقط همان فایل «کهنه» است و فقط همان دانلود می‌شود.
 *
 * یعنی «فقط فایل‌های تغییریافته» — نه کلِ اپ، نه کلِ محتوا.
 */
object MediaFreshness {

    private const val PREF = "hamyar_media_sig"
    private const val P_SIG = "s_"
    private const val P_ID = "f_"
    private const val P_CK = "c_"
    private const val P_PDF = "p_"
    private const val KEY_LAST = "last_check"

    /** عمرِ بررسی: شش ساعت (تا هر بار بازکردنِ صفحه، چند درخواست بی‌مورد نرود). */
    const val TTL_MS = 6L * 60L * 60L * 1000L

    /** یک فایلِ دانلودشده که می‌تواند کهنه شود. */
    data class Item(
        /** کلیدِ وضعیت در صفحهٔ دانلود (همان `statusKey`). */
        val key: String,
        val fileId: String,
        /** کلیدِ گاوصندوق برای صوت؛ برای PDF بی‌استفاده. */
        val cacheKey: String,
        val isPdf: Boolean,
    )

    data class Check(
        val checked: Int,
        val stale: List<Item>,
        val failed: Int,
    ) {
        val allFresh: Boolean get() = stale.isEmpty() && failed == 0
    }

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun lastCheckAt(ctx: Context): Long = store(ctx).getLong(KEY_LAST, 0L)
    fun isFresh(ctx: Context): Boolean = System.currentTimeMillis() - lastCheckAt(ctx) < TTL_MS

    /** نشانیِ **متادیتا** (نه `view`): همان چیزی که اثرِ انگشت را برمی‌گرداند. */
    private fun metaUrl(fileId: String): String =
        StudyMedia.viewUrl(StudyMedia.resolveFileId(fileId)).substringBefore("/view")

    /**
     * اثرِ انگشتِ محتوای فایل روی سرور — `signature|size|updatedAt`.
     * `null` = نشد (آفلاین/نبودِ فایل).
     */
    fun remoteSignatureBlocking(fileId: String): String? = runCatching {
        val conn = (URL(metaUrl(fileId)).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
        }
        try {
            conn.connect()
            if (conn.responseCode !in 200..299) return@runCatching null
            val body = conn.inputStream.use { String(it.readBytes(), Charsets.UTF_8) }
            val o = JSONObject(body)
            val sig = o.optString("signature").trim()
            if (sig.isEmpty()) null
            else sig + "|" + o.optLong("sizeOriginal") + "|" + o.optString("\$updatedAt")
        } finally {
            runCatching { conn.disconnect() }
        }
    }.getOrNull()

    /** ثبتِ یک دانلودِ موفق: شناسه‌ها + اثرِ انگشتِ سرور در همان لحظه. */
    fun rememberDownload(ctx: Context, key: String, fileId: String, cacheKey: String, isPdf: Boolean) {
        val s = store(ctx)
        s.putString(P_ID + key, fileId)
        s.putString(P_CK + key, cacheKey)
        s.putString(P_PDF + key, if (isPdf) "1" else "0")
        remoteSignatureBlocking(fileId)?.let { s.putString(P_SIG + key, it) }
    }

    /** فهرستِ فایل‌هایی که تا حالا دانلود شده‌اند. */
    fun downloaded(ctx: Context): List<Item> = store(ctx)
        .keysWithPrefix(P_ID)
        .mapNotNull { k ->
            val key = k.removePrefix(P_ID)
            val fileId = store(ctx).getString(k)
            if (key.isBlank() || fileId.isBlank()) null
            else Item(
                key = key,
                fileId = fileId,
                cacheKey = store(ctx).getString(P_CK + key),
                isPdf = store(ctx).getString(P_PDF + key) == "1",
            )
        }

    fun forget(ctx: Context, key: String) {
        store(ctx).remove(P_ID + key, P_CK + key, P_PDF + key, P_SIG + key)
    }

    /**
     * بررسیِ همه‌ی فایل‌های دانلودشده: کدام‌ها روی سرور عوض شده‌اند؟
     *
     * اثرِ انگشتِ سرور همان چیزی است که هنگامِ دانلود ثبت شده؛ اگر نداشته باشیم
     * (مثلاً دانلودِ نسخه‌های قدیمیِ اپ) فقط ثبت می‌شود و «کهنه» اعلام نمی‌شود.
     */
    suspend fun findStale(ctx: Context): Check = withContext(Dispatchers.IO) {
        val s = store(ctx)
        val items = downloaded(ctx)
        val stale = mutableListOf<Item>()
        var failed = 0
        for (item in items) {
            val remote = remoteSignatureBlocking(item.fileId)
            if (remote == null) {
                failed++
                continue
            }
            val known = s.getString(P_SIG + item.key)
            when {
                known.isBlank() -> s.putString(P_SIG + item.key, remote)
                known != remote -> stale += item
            }
        }
        s.putLong(KEY_LAST, System.currentTimeMillis())
        Check(checked = items.size, stale = stale, failed = failed)
    }
}
