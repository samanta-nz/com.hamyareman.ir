package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * بررسی تازه‌بودن دانلودهای صوتی/PDF با متادیتای HTTP سرور پارس‌پک.
 * هیچ درخواست فایل به آدرس باکت Appwrite یا آروان ارسال نمی‌شود.
 */
object MediaFreshness {

    private const val PREF = "hamyar_media_sig"
    private const val P_SIG = "s_"
    private const val P_ID = "f_"
    private const val P_CK = "c_"
    private const val P_PDF = "p_"
    private const val P_AT = "t_"
    private const val KEY_LAST = "last_check"

    /** هر ورودی کش، هنگام بازشدن یا آغاز پخش حداکثر هر ۳۰ ثانیه بررسی می‌شود. */
    const val TTL_MS = 30_000L

    data class Item(
        val key: String,
        val fileId: String,
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

    /** URL جاری همان فایل روی پارس‌پک، نه API متادیتای باکت Appwrite. */
    private fun metaUrl(fileId: String): String =
        StudyMedia.viewUrl(StudyMedia.resolveFileId(fileId))

    /**
     * امضای HTTP با ETag، طول فایل و Last-Modified.
     * null یعنی URL یا متادیتای قابل‌استفاده در دسترس نیست.
     */
    fun remoteSignatureBlocking(fileId: String): String? = runCatching {
        val url = metaUrl(fileId).takeIf { it.isNotBlank() } ?: return@runCatching null
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            requestMethod = "HEAD"
            setRequestProperty("Accept-Encoding", "identity")
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val etag = conn.getHeaderField("ETag").orEmpty().trim()
            val size = conn.getHeaderField("Content-Length")?.toLongOrNull()
                ?: conn.contentLengthLong
            val modified = conn.getHeaderField("Last-Modified").orEmpty().ifBlank {
                conn.lastModified.takeIf { it > 0L }?.toString().orEmpty()
            }
            if (etag.isBlank() && size < 0L && modified.isBlank()) return@runCatching null
            listOf(etag, size.toString(), modified).joinToString("|")
        } finally {
            runCatching { conn.disconnect() }
        }
    }.getOrNull()

    /** ثبت دانلود موفق: شناسه‌ها و امضای HTTP فعلی. */
    fun rememberDownload(ctx: Context, key: String, fileId: String, cacheKey: String, isPdf: Boolean) {
        val s = store(ctx)
        s.putString(P_ID + key, fileId)
        s.putString(P_CK + key, cacheKey)
        s.putString(P_PDF + key, if (isPdf) "1" else "0")
        remoteSignatureBlocking(fileId)?.let { s.putString(P_SIG + key, it) }
        s.putLong(P_AT + key, System.currentTimeMillis())
    }

    /** فهرست فایل‌هایی که تا حالا دانلود شده‌اند. */
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
        store(ctx).remove(P_ID + key, P_CK + key, P_PDF + key, P_SIG + key, P_AT + key)
    }

    /**
     * Refresh a cached PDF or encrypted MediaVault resource when its remote signature changed.
     * New payloads are staged and verified before replacing a working local copy.
     *
     * @return true only when a replacement was downloaded successfully.
     */
    suspend fun ensureCurrent(
        ctx: Context,
        key: String,
        fileId: String,
        cacheKey: String,
        isPdf: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        val s = store(ctx)
        val cached = if (isPdf) {
            StudyPdfCache.isValid(StudyPdfCache.file(ctx, cacheKey))
        } else {
            MediaVault.isVerified(ctx, cacheKey)
        }
        if (!cached) return@withContext false

        val now = System.currentTimeMillis()
        val checkedAt = s.getLong(P_AT + key, 0L)
        val known = s.getString(P_SIG + key)
        if (known.isNotBlank() && now >= checkedAt && now - checkedAt < TTL_MS) {
            return@withContext false
        }

        val remote = remoteSignatureBlocking(fileId)
        if (remote == null) {
            // Don't thrash the server during a temporary outage; retain the old cache.
            s.putLong(P_AT + key, now)
            return@withContext false
        }
        if (known.isNotBlank() && known == remote) {
            s.putLong(P_AT + key, now)
            s.putString(P_ID + key, fileId)
            s.putString(P_CK + key, cacheKey)
            s.putString(P_PDF + key, if (isPdf) "1" else "0")
            return@withContext false
        }

        try {
            val urls = StudyMedia.candidateUrls(fileId).filter { it.isNotBlank() }
            if (urls.isEmpty()) throw java.io.IOException("برای این رسانه URL تعریف نشده است.")
            if (isPdf) {
                // forceRefresh prevents a changed signature being hidden by a valid local PDF.
                StudyPdfCache.obtain(ctx, cacheKey, urls, {}, true)
            } else {
                val stagedKey = StudyMedia.bookCacheKey("refresh", cacheKey)
                MediaVault.delete(ctx, stagedKey)
                try {
                    MediaVault.downloadEncrypted(ctx, urls, stagedKey) { _, _ -> }
                    if (!MediaVault.isVerified(ctx, stagedKey)) {
                        throw java.io.IOException("فایل تازه پس از دانلود تأیید نشد.")
                    }
                    if (!MediaVault.replaceVerifiedCache(ctx, stagedKey, cacheKey)) {
                        throw java.io.IOException("جایگزینی امن کش صوت/ویدیو انجام نشد.")
                    }
                } catch (error: Throwable) {
                    MediaVault.delete(ctx, stagedKey)
                    throw error
                }
            }
            val confirmed = remoteSignatureBlocking(fileId) ?: remote
            s.putString(P_ID + key, fileId)
            s.putString(P_CK + key, cacheKey)
            s.putString(P_PDF + key, if (isPdf) "1" else "0")
            s.putString(P_SIG + key, confirmed)
            s.putLong(P_AT + key, System.currentTimeMillis())
            true
        } catch (_: Throwable) {
            // A failed update does not bless the old signature or remove the old usable copy.
            s.putLong(P_AT + key, now)
            false
        }
    }

    /** بررسی همه دانلودهای ثبت‌شده؛ اختلاف امضا یعنی فایل کهنه است. */
    suspend fun findStale(ctx: Context): Check = withContext(Dispatchers.IO) {
        val s = store(ctx)
        val items = downloaded(ctx)
        val stale = mutableListOf<Item>()
        var failed = 0
        for (item in items) {
            val remote = remoteSignatureBlocking(item.fileId)
            s.putLong(P_AT + item.key, System.currentTimeMillis())
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
