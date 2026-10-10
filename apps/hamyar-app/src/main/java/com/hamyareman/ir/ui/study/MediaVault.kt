package com.hamyareman.ir.ui.study

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * «گاوصندوق رسانه» — فایل‌های صوتی/تصویریِ دانلودشده هرگز به‌صورت خام روی گوشی
 * نمی‌مانند؛ با AES رمز می‌شوند (کلید داده با Keystore پوشانده می‌شود) و پخشِ
 * محلی فقط از راه [LocalMediaServer] (فقط ۱۲۷.۰.۰.۱ داخل خود اپ) انجام می‌شود.
 * نتیجه: فایل از بیرون اپ (مدیر فایل/کپی/اشتراک) قابل پخش یا خواندن نیست.
 */
object MediaVault {

    private const val DIR = "media-vault"
    private const val MAGIC = "HMV1"
    private const val PREFS = "hamyar_vault_keys"
    private const val WRAPPED_KEY = "wrapped_data_key"
    private const val EPOCH_KEY = "vault_epoch"
    /** نسخه‌ی محتوای گاوصندوق — v2: پاک‌سازی placeholderهای کش‌شده‌ی نسخه‌های قدیمی اپ. */
    private const val CACHE_EPOCH = 2
    private const val VERSION_PREFS = "hamyar_media_versions"
    private const val VERSION_CHECK_TTL_MS = 30_000L
    private const val VERSION_SAMPLE_BYTES = 4096

    private var dataKey: SecretKey? = null

    private fun vaultDir(ctx: Context): File {
        val dir = File(ctx.filesDir, DIR).apply { mkdirs() }
        // نسخه‌ی کش: با بالا رفتن CACHE_EPOCH، همه‌ی فایل‌های دانلودشده با نسخه‌ی
        // قبلی (مثلاً placeholderهای قدیمی) یک‌بار پاک می‌شوند و دوباره دانلود واقعی می‌شود.
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(EPOCH_KEY, 0) != CACHE_EPOCH) {
            dir.listFiles()?.forEach { it.delete() }
            prefs.edit().putInt(EPOCH_KEY, CACHE_EPOCH).apply()
        }
        return dir
    }

    fun vaultFile(ctx: Context, cacheKey: String) = File(vaultDir(ctx), "$cacheKey.enc")

    fun isCached(ctx: Context, cacheKey: String): Boolean =
        vaultFile(ctx, cacheKey).let { it.exists() && it.length() > 32 }

    /**
     * آیا این فایلِ دانلودشده **تأیید شده** است؟ (دانلود کامل و قابلِ رمزگشایی)
     * فقط فایل‌های تأییدشده برای پخشِ آفلاین استفاده می‌شوند تا یک فایلِ نیمه‌کاره
     * پخش را نشکند.
     */
    fun isVerified(ctx: Context, cacheKey: String): Boolean =
        isCached(ctx, cacheKey) && ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("verified", emptySet()).orEmpty().contains(cacheKey)

    fun markVerified(ctx: Context, cacheKey: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("verified", emptySet()).orEmpty().toMutableSet()
        set += cacheKey
        prefs.edit().putStringSet("verified", set).apply()
    }

    /** نگاهِ کوتاه به ابتدای فایلِ رمزگشایی‌شده — برای اطمینان از سالم‌بودنِ دانلود. */
    fun peek(ctx: Context, cacheKey: String, bytes: Int = 4096): ByteArray? {
        val all = vaultFile(ctx, cacheKey)
        if (!all.exists() || all.length() <= 20) return null
        return runCatching {
            // فقط همان چند کیلوبایتِ اول خوانده می‌شود (نه کل فایل) — در CTR
            // رمزگشاییِprefix معتبر است و حافظه هم مصرف نمی‌شود.
            val want = (20 + bytes).coerceAtMost(all.length().toInt())
            val raw = ByteArray(want)
            java.io.FileInputStream(all).use { fin ->
                var off = 0
                while (off < want) {
                    val n = fin.read(raw, off, want - off)
                    if (n <= 0) break
                    off += n
                }
            }
            if (!raw.startsWith(MAGIC.toByteArray(Charsets.US_ASCII))) return@runCatching null
            val iv = raw.copyOfRange(4, 20)
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(ctx), IvParameterSpec(iv))
            cipher.doFinal(raw, 20, raw.size - 20)
        }.getOrNull()
    }

    fun delete(ctx: Context, cacheKey: String) {
        vaultFile(ctx, cacheKey).delete()
        File(vaultDir(ctx), "$cacheKey.enc.part").delete()
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet("verified", emptySet()).orEmpty().toMutableSet()
        if (set.remove(cacheKey)) prefs.edit().putStringSet("verified", set).apply()
    }

    /**
     * تا برگشتنِ اینترنت صبر می‌کند (نسخهٔ بلوکه‌ایِ [NetState.awaitOnline] — برای
     * نخ‌های دانلود که سواسپند نیستند). وقتی شبکه عوض می‌شود یا پروکسی می‌پرد،
     * به‌جای سوزاندنِ تلاش‌ها این‌جا صبر می‌کنیم و بعد از همان‌جا ادامه می‌دهیم.
     */
    internal fun awaitNetwork(ctx: Context, timeoutMs: Long = 120_000L): Boolean {
        if (NetState.isOnline(ctx)) return true
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (NetState.isOnline(ctx)) return true
            runCatching { Thread.sleep(700) }
        }
        return NetState.isOnline(ctx)
    }

    fun cachedBytes(ctx: Context): Long =
        vaultDir(ctx).listFiles()?.sumOf { it.length() } ?: 0L

    // ------------------------------------------------------------ کلیدها

    @Synchronized
    private fun key(ctx: Context): SecretKey {
        dataKey?.let { return it }
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = runCatching { unwrapWithKeystore(prefs.getString(WRAPPED_KEY, null)) }.getOrNull()
        if (raw != null) {
            dataKey = SecretKeySpec(raw, "AES")
            return dataKey!!
        }
        // fail-closed: کلید داده هرگز Base64 خام روی دیسک ذخیره نمی‌شود. اگر
        // Android Keystore در دسترس نیست، دانلود امن شروع نمی‌شود.
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrapped = wrapWithKeystore(bytes)
        prefs.edit().putString(WRAPPED_KEY, wrapped).apply()
        dataKey = SecretKeySpec(bytes, "AES")
        return dataKey!!
    }

    private fun masterKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("hamyar_media_master", null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                "hamyar_media_master",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private fun wrapWithKeystore(plain: ByteArray): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, masterKey())
        val ct = c.doFinal(plain)
        val out = ByteArray(12 + ct.size)
        c.iv.copyInto(out)
        ct.copyInto(out, 12)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun unwrapWithKeystore(wrapped: String?): ByteArray? {
        wrapped ?: return null
        val all = Base64.decode(wrapped, Base64.NO_WRAP)
        if (all.size <= 12) return null
        val iv = all.copyOfRange(0, 12)
        val ct = all.copyOfRange(12, all.size)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        return c.doFinal(ct)
    }

    // ------------------------------------------------------ دانلود رمزشده

    /** تعدادِ رشته‌های موازیِ دانلودِ صوت (هر رشته یک بازه‌ی Range). */
    /**
     * شمارِ رشته‌های موازیِ دانلود. سرور برای هر اتصال سقفِ سرعت دارد و با
     * یک رشته، یک درسِ ۲۴ مگابایتی دقیقه‌ها طول می‌کشید؛ با ۶ رشته (وقتی
     * سرور Content-Range می‌دهد) مجموعِ سرعت چند برابر می‌شود. اگر سرور
     * رنج ندهد، خودکار به دانلودِ تک‌رشته‌ای برمی‌گردیم.
     */
    private const val PARALLEL_STREAMS = 6

    /** اندازهٔ هر بازه — مضربی از ۱۶ بایت (بلاکِ AES) تا CTR مستقل درست دربیاید. */
    /** اندازهٔ هر بازه: با تُکه‌های کوچک‌تر، رشته‌ها زودتر تمام نمی‌شوند و
     * موازی‌سازی روی کلِ فایل حفظ می‌شود (۴MB → ۲MB). */
    private const val RANGE_CHUNK = 2L * 1024 * 1024

    /** چند بار یک بازهٔ نیمه‌کاره دوباره تلاش شود (از همان‌جا که مانده). */
    private const val MAX_RANGE_ATTEMPTS = 8

    /**
     * دانلود از [url] و نوشتنِ رمزشده (AES/CTR با IV تازه) در گاوصندوق.
     * روی دیسک حتی یک بایتِ خام از رسانه نوشته نمی‌شود.
     *
     * سرعت: اول اندازهٔ کل با یک درخواستِ سبک خوانده می‌شود و بعد دانلود
     * «چندرشته‌ای» انجام می‌گیرد (سه بازه‌ی هم‌زمان). چون رمزنگاری CTR است،
     * هر بازه با شمارندهٔ همان بلاک رمز می‌شود و نتیجه دقیقاً با رمزنگاریِ
     * تک‌رشته یکی است (آزمونِ MediaVaultParallelCryptoTest همین را می‌سنجد).
     * اگر سرور به Range پاسخِ ۲۰۶ نداد یا هر خطایی رخ داد، بی‌صدا به مسیرِ
     * تک‌رشته برمی‌گردیم.
     */
    fun downloadEncrypted(ctx: Context, url: String, cacheKey: String, onProgress: (Long, Long) -> Unit) {
        val probed = probeSize(url)
        val parallelSucceeded = probed > 0 &&
            runCatching { downloadParallel(ctx, url, cacheKey, probed, onProgress) }.isSuccess
        if (!parallelSucceeded) downloadSingle(ctx, url, cacheKey, probed, onProgress)
        // finishDownload ثبت امضا را بعد از جایگزینیِ تأییدشده انجام می‌دهد.
    }

    /**
     * FASTEST: origin برنده و سپس origin دوم؛ EXTERNAL/INTERNAL: فقط همان سرور.
     * URL موفق برای نمایش/ثبت diagnostics برگردانده می‌شود.
     */
    fun downloadEncrypted(
        ctx: Context,
        urls: List<String>,
        cacheKey: String,
        onProgress: (Long, Long) -> Unit,
    ): String {
        require(urls.isNotEmpty()) { "برای این فایل نشانی سرور وجود ندارد." }
        val failures = mutableListOf<String>()
        for (url in urls.distinct()) {
            val result = runCatching { downloadEncrypted(ctx, url, cacheKey, onProgress) }
            if (result.isSuccess) return url
            failures += (result.exceptionOrNull()?.message ?: "خطای نامشخص")
            // کش قبلی را نگه می‌داریم؛ خطای سرور دوم نباید فایل سالمِ محلی را پاک کند.
        }
        throw java.io.IOException(failures.joinToString(" | "))
    }

    /**
     * تازه‌سازی کش رمزشده قبل از انتخاب نسخهٔ محلی برای پخش/نمایش.
     * پاسخ شبکهٔ ناموفق کش سالم قبلی را حذف نمی‌کند. برای کش‌های قدیمیِ بدون امضا، یک بار
     * نسخهٔ روی سرور دوباره دانلود می‌شود تا نسخهٔ قبلی بی‌صدا تازه فرض نشود.
     * فقط روی نخ IO فراخوانی شود.
     */
    fun ensureFresh(
        ctx: Context,
        urls: List<String>,
        cacheKey: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Boolean {
        val candidates = urls.distinct().filter { it.isNotBlank() }
        if (candidates.isEmpty()) return false
        if (!isVerified(ctx, cacheKey)) {
            return runCatching {
                downloadEncrypted(ctx, candidates, cacheKey, onProgress)
                isVerified(ctx, cacheKey)
            }.getOrDefault(false)
        }

        val prefs = versionPrefs(ctx)
        val checkedKey = "checked:" + cacheKey
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(checkedKey, 0L) < VERSION_CHECK_TTL_MS) return true

        var currentUrl: String? = null
        var current: RemoteVersion? = null
        for (url in candidates) {
            val candidate = remoteVersion(url)
            if (candidate != null) {
                currentUrl = url
                current = candidate
                break
            }
        }
        // آفلاین/خطای CDN: cache سالمِ فعلی را برای حالت آفلاین حفظ کن و در بازشدن بعدی دوباره بررسی کن.
        if (current == null || currentUrl == null) return true

        val savedEtag = prefs.getString("etag:" + cacheKey, "").orEmpty()
        val savedLength = prefs.getLong("length:" + cacheKey, -1L)
        val savedModified = prefs.getString("modified:" + cacheKey, "").orEmpty()
        val savedUrl = prefs.getString("url:" + cacheKey, "").orEmpty()

        val same = when {
            savedEtag.isNotBlank() && current.etag.isNotBlank() ->
                (savedLength < 0L || current.length < 0L || savedLength == current.length) &&
                    savedEtag == current.etag
            savedModified.isNotBlank() && current.lastModified.isNotBlank() ->
                (savedLength < 0L || current.length < 0L || savedLength == current.length) &&
                    savedModified == current.lastModified &&
                    compareCachedSamples(ctx, currentUrl, cacheKey, current.length)
            savedEtag.isBlank() && savedModified.isBlank() && savedUrl.isNotBlank() ->
                compareCachedSamples(ctx, currentUrl, cacheKey, current.length)
            else -> false
        }

        if (same) {
            saveRemoteVersion(ctx, cacheKey, currentUrl, current, now)
            return true
        }

        // اگر امضا از نسخهٔ قدیمی وجود نداشت یا تغییر دیده شد، نسخهٔ سرور را دریافت کن؛
        // finishDownload کش قبلی را نگه می‌دارد تا جایگزینی سالم تأیید شود.
        val ordered = listOf(currentUrl) + candidates.filterNot { it == currentUrl }
        return runCatching {
            downloadEncrypted(ctx, ordered, cacheKey, onProgress)
            isVerified(ctx, cacheKey)
        }.getOrElse {
            isVerified(ctx, cacheKey)
        }
    }

    private data class RemoteVersion(
        val length: Long,
        val etag: String,
        val lastModified: String,
    )

    private fun versionPrefs(ctx: Context) =
        ctx.getSharedPreferences(VERSION_PREFS, Context.MODE_PRIVATE)

    private fun remoteVersion(url: String): RemoteVersion? = runCatching {
        val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            instanceFollowRedirects = true
            requestMethod = "HEAD"
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
            setRequestProperty("Pragma", "no-cache")
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val version = RemoteVersion(
                length = conn.contentLengthLong,
                etag = conn.getHeaderField("ETag").orEmpty().trim(),
                lastModified = conn.getHeaderField("Last-Modified").orEmpty().trim(),
            )
            if (version.length < 0L && version.etag.isBlank() && version.lastModified.isBlank()) null
            else version
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun saveRemoteVersion(
        ctx: Context,
        cacheKey: String,
        url: String,
        version: RemoteVersion,
        checkedAt: Long = System.currentTimeMillis(),
    ) {
        versionPrefs(ctx).edit()
            .putString("url:" + cacheKey, url)
            .putString("etag:" + cacheKey, version.etag)
            .putLong("length:" + cacheKey, version.length)
            .putString("modified:" + cacheKey, version.lastModified)
            .putLong("checked:" + cacheKey, checkedAt)
            .apply()
    }

    private fun compareCachedSamples(ctx: Context, url: String, cacheKey: String, length: Long): Boolean {
        if (length <= 0L) return false
        val localHead = peek(ctx, cacheKey, VERSION_SAMPLE_BYTES) ?: return false
        val wanted = minOf(VERSION_SAMPLE_BYTES.toLong(), length).toInt()
        val remoteHead = runCatching {
            val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("Range", "bytes=0-" + (wanted - 1))
            }
            try {
                if (conn.responseCode != 206) return false
                conn.inputStream.use { input ->
                    val out = ByteArray(wanted)
                    var off = 0
                    while (off < wanted) {
                        val n = input.read(out, off, wanted - off)
                        if (n <= 0) break
                        off += n
                    }
                    if (off != wanted) return false
                    out
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull() ?: return false
        val headMatches = localHead.size >= wanted &&
            localHead.copyOfRange(0, wanted).contentEquals(remoteHead)
        return headMatches && tailMatchesServer(ctx, url, cacheKey, length)
    }

    private fun unmarkVerified(ctx: Context, cacheKey: String) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val verified = prefs.getStringSet("verified", emptySet()).orEmpty().toMutableSet()
        verified.remove(cacheKey)
        prefs.edit().putStringSet("verified", verified).apply()
    }

    /**
     * گزارشِ تُنُکِ پیشرفت: دستِ‌کم هر ۵۱۲KB یا هر ۲۰۰ms (نه هر تکهٔ شبکه) تا
     * بازترکیبِ صفحه گلوگاهِ دانلود نشود. [lastBytes] و [lastTime] گزارشِ قبلی‌اند.
     */
    private fun reportProgress(
        onProgress: (Long, Long) -> Unit,
        done: Long,
        total: Long,
        lastBytes: java.util.concurrent.atomic.AtomicLong,
        lastTime: java.util.concurrent.atomic.AtomicLong,
    ) {
        val now = System.currentTimeMillis()
        val prevBytes = lastBytes.get()
        // ۱MB/۴۰۰ms: هر به‌روزرسانیِ پیشرفت یک بازترکیبِ صفحهٔ تدریس است؛ با
        // ۵۱۲KB/۲۰۰ms این بازترکیب‌ها روی گوشیِ ضعیف، خودِ نخِ دانلود را هم می‌خواباند.
        if (done - prevBytes >= 1024 * 1024 || now - lastTime.get() >= 400 || done >= total) {
            if (lastBytes.compareAndSet(prevBytes, done)) {
                lastTime.set(now)
                onProgress(done, total)
            }
        }
    }

    /** دانلودِ چندرشته‌ای با Range + رمزنگاریِ مستقلِ هر بازه (CTR). */
    private fun downloadParallel(
        ctx: Context,
        url: String,
        cacheKey: String,
        total: Long,
        onProgress: (Long, Long) -> Unit,
    ) {
        val secret = key(ctx)
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val target = vaultFile(ctx, cacheKey)
        val part = File(vaultDir(ctx), "$cacheKey.enc.part")
        val magic = MAGIC.toByteArray(Charsets.US_ASCII)
        val headerLen = (magic.size + 16).toLong()
        try {
            val ranges = mutableListOf<LongArray>()
            var off = 0L
            while (off < total) {
                val end = minOf(off + RANGE_CHUNK, total) - 1
                ranges += longArrayOf(off, end, headerLen + off)
                off = end + 1
            }
            java.io.RandomAccessFile(part, "rw").use { head ->
                head.setLength(headerLen + total)
                head.seek(0)
                head.write(magic)
                head.write(iv)
            }
            val done = java.util.concurrent.atomic.AtomicLong(0)
            val lastBytes = java.util.concurrent.atomic.AtomicLong(0)
            val lastTime = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
            val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()
            val pool = java.util.concurrent.Executors.newFixedThreadPool(minOf(PARALLEL_STREAMS, ranges.size))
            try {
                val latch = java.util.concurrent.CountDownLatch(ranges.size)
                ranges.forEach { r ->
                    pool.execute {
                        try {
                            fetchRange(ctx, url, r[0], r[1], secret, iv, part, r[2]) { n ->
                                reportProgress(onProgress, done.addAndGet(n), total, lastBytes, lastTime)
                            }
                        } catch (t: Throwable) {
                            errors += t
                        } finally {
                            latch.countDown()
                        }
                    }
                }
                latch.await()
                if (errors.isNotEmpty()) throw errors.first()
            } finally {
                pool.shutdownNow()
            }
            if (done.get() != total) error("دانلودِ چندرشته‌ای کامل نشد (${done.get()} از $total بایت).")
            onProgress(total, total)
            finishDownload(ctx, url, cacheKey, part, target, total)
        } catch (t: Throwable) {
            part.delete()
            throw t
        }
    }

    /**
     * یک بازهٔ [start]..[end] را می‌گیرد و رمزِ همان بازه را در [fileOff]ِ فایل
     * می‌نویسد. هر رشته `RandomAccessFile` خودش را دارد (بدونِ قفلِ مشترک).
     * فقط پاسخِ **۲۰۶** پذیرفته می‌شود: اگر سرور Range را نادیده بگیرد و کلِ فایل
     * را بفرستد (۲۰۰)، نوشتنش فایل را خراب می‌کرد.
     *
     * **تلاشِ مجددِ درون‌بازه‌ای:** روی موبایل یک بازهٔ ۲ مگابایتی خیلی وقت‌ها
     * وسطِ راه قطع می‌شود. قبلاً یک قطعیِ کوچک کلِ دانلودِ چندرشته‌ای را می‌انداخت
     * و برنامه از **صفر** با یک رشته دانلود می‌کرد — همان «کندیِ عجیبِ» کاربر.
     * حالا همان بازه از همان‌جایی که مانده ادامه می‌یابد (چند بار، با فاصلهٔ
     * پرشونده) و بقیهٔ رشته‌ها هم دست‌نخورده به کارشان ادامه می‌دهند.
     */
    private fun fetchRange(
        ctx: Context,
        url: String,
        start: Long,
        end: Long,
        secret: javax.crypto.SecretKey,
        iv: ByteArray,
        part: File,
        fileOff: Long,
        onBytes: (Long) -> Unit,
    ) {
        var from = start
        var attempt = 0
        java.io.RandomAccessFile(part, "rw").use { raf ->
            while (from <= end) {
                val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secret, IvParameterSpec(counterIv(iv, from / 16)))
                var conn: java.net.HttpURLConnection? = null
                try {
                    raf.seek(fileOff + (from - start))
                    // اتصالِ تازه برای هر تلاش: اگر شبکه/پروکسی عوض شده باشد، مسیرِ
                    // تازه خوانده می‌شود (تنظیمِ دستیِ پروکسی نداریم؛ همه از سیستم می‌آید).
                    conn = ResilientHttp.open(url, range = "bytes=$from-$end", connectMs = 15000, readMs = 30000, attempts = 3)
                    if (conn.responseCode != 206) error("سرور به بازه پاسخِ ۲۰۶ نداد (کد ${conn.responseCode}).")
                    conn.inputStream.use { input ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buf).also { read = it } > 0) {
                            val enc = cipher.update(buf, 0, read)
                            if (enc != null) raf.write(enc)
                            from += read
                            onBytes(read.toLong())
                        }
                        val tail = cipher.doFinal()
                        if (tail != null) raf.write(tail)
                    }
                } catch (t: Throwable) {
                    attempt++
                    if (attempt > MAX_RANGE_ATTEMPTS) throw t
                    // اگر اینترنت نیست، اول تا برگشتنش صبر می‌کنیم (تغییرِ شبکه/پروکسی
                    // نباید دانلود را بکُشد) و بعد از همان‌جا ادامه می‌دهیم.
                    if (!NetState.isOnline(ctx)) awaitNetwork(ctx)
                    // عقب‌نشینیِ پرشونده: ۰٫۴s، ۰٫۸s، ۱٫۶s …
                    runCatching { Thread.sleep(400L shl (attempt - 1)) }
                    // بازگشت به مرزِ ۱۶ بایتی تا جریان‌کلیدِ CTR همان‌جا ادامه یابد
                    // (بایت‌های نوشته‌شده بعد از این مرز دوباره و یکسان نوشته می‌شوند).
                    val done = from - start
                    from = start + (done / 16) * 16
                } finally {
                    runCatching { conn?.disconnect() }
                }
            }
        }
        if (from != end + 1) error("بازهٔ $start-$end ناقص رسید.")
    }

    /**
     * IV شمارندهٔ CTR برای بلاکِ شمارهٔ [blockIndex]: همان IV با اضافه‌شدنِ
     * شمارهٔ بلاک به‌صورتِ عددِ ۱۲۸بیتیِ big-endian (دقیقاً کاری که حالتِ CTR
     * در JCE می‌کند) — پس رمزِ هر بازه جداگانه با رمزِ تک‌رشته یکی است.
     */
    internal fun counterIv(iv: ByteArray, blockIndex: Long): ByteArray {
        val out = iv.copyOf()
        var carry = blockIndex
        var i = 15
        while (carry != 0L && i >= 0) {
            val v = (out[i].toInt() and 0xFF) + (carry and 0xFF).toInt()
            out[i] = (v and 0xFF).toByte()
            carry = (carry ushr 8) + (v shr 8).toLong()
            i--
        }
        return out
    }

    /**
     * مسیرِ تک‌رشته — حالا **قابلِ ادامه** است.
     *
     * اگر وسطِ دانلود شبکه عوض شود (وای‌فای → دیتا) یا پروکسی بپرد، اتصال می‌شکند؛
     * به‌جای از‌صفر‌شروع‌کردن، تا برگشتنِ اینترنت صبر می‌کنیم و با
     * `Range: bytes=<مانده>-` از همان‌جا ادامه می‌دهیم. بازگشت به مرزِ ۱۶ بایتی
     * انجام می‌شود تا جریان‌کلیدِ CTR بی‌دردسر ادامه پیدا کند.
     */
    private fun downloadSingle(
        ctx: Context,
        url: String,
        cacheKey: String,
        probed: Long,
        onProgress: (Long, Long) -> Unit,
    ) {
        val target = vaultFile(ctx, cacheKey)
        val part = File(vaultDir(ctx), "$cacheKey.enc.part")
        val headerLen = (MAGIC.length + 16).toLong()
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        var total = probed
        var done = 0L
        var attempt = 0
        val lastBytes = java.util.concurrent.atomic.AtomicLong(0)
        val lastTime = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())

        java.io.RandomAccessFile(part, "rw").use { raf ->
            raf.setLength(headerLen)
            raf.seek(0)
            raf.write(MAGIC.toByteArray(Charsets.US_ASCII))
            raf.write(iv)

            while (attempt <= MAX_RANGE_ATTEMPTS) {
                val aligned = done - (done % 16)
                try {
                    val conn = ResilientHttp.open(
                        url,
                        range = if (aligned > 0) "bytes=$aligned-" else null,
                        connectMs = 15000,
                        readMs = 30000,
                        attempts = 3,
                    )
                    if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                    if (aligned > 0 && conn.responseCode == 200) {
                        // سرور رنج را نادیده گرفت: از صفر شروع کن.
                        done = 0
                        raf.setLength(headerLen)
                    }
                    val cl = conn.contentLengthLong
                    if (total <= 0 && cl > 0) total = if (aligned > 0) aligned + cl else cl

                    val cipher = Cipher.getInstance("AES/CTR/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, key(ctx), IvParameterSpec(counterIv(iv, aligned / 16)))
                    raf.seek(headerLen + aligned)
                    done = aligned

                    conn.inputStream.use { input ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buf).also { read = it } > 0) {
                            val enc = cipher.update(buf, 0, read)
                            if (enc != null) raf.write(enc)
                            done += read
                            reportProgress(onProgress, done, total, lastBytes, lastTime)
                        }
                        val tail = cipher.doFinal()
                        if (tail != null) raf.write(tail)
                    }
                    runCatching { conn.disconnect() }
                    if (total <= 0 || done >= total) break
                    attempt++  // اتصال تمام شد ولی فایل کامل نیست ⇒ ادامه بده
                } catch (t: Throwable) {
                    attempt++
                    if (attempt > MAX_RANGE_ATTEMPTS) {
                        part.delete()
                        throw t
                    }
                    // تغییرِ شبکه/پروکسی: صبر تا برگشتنِ اینترنت، بعد ادامه از همان‌جا.
                    if (!NetState.isOnline(ctx)) awaitNetwork(ctx)
                    runCatching { Thread.sleep(400L shl (attempt - 1).coerceAtMost(4)) }
                }
            }
            raf.setLength(headerLen + done)
        }

        onProgress(done, total)
        if (total > 0 && done < total) {
            part.delete()
            error("دانلود کامل نشد ($done از $total بایت).")
        }
        try {
            finishDownload(ctx, url, cacheKey, part, target, total)
        } finally {
            part.delete()
        }
    }

    /**
     * انتقالِ فایلِ موقت به گاوصندوق + تأییدِ سلامت.
     * تأیید دو سرِ فایل است: ابتدا (که باید قابلِ رمزگشایی باشد) و انتها (که با
     * همان بایت‌های سرور مقایسه می‌شود) — این‌طور اگر شمارندهٔ CTR در بازه‌های
     * میانی اشتباه بسته شده باشد، فایل «تأییدشده» علامت نمی‌خورد.
     */
    private fun finishDownload(ctx: Context, url: String, cacheKey: String, part: File, target: File, total: Long) {
        val backup = File(vaultDir(ctx), "$cacheKey.enc.bak")
        runCatching { backup.delete() }
        val hadPrevious = target.exists()
        val previousVerified = isVerified(ctx, cacheKey)
        if (hadPrevious && !target.renameTo(backup)) {
            error("کش قبلی رسانه قابل حفظ نیست؛ دانلود تازه جایگزین نشد.")
        }
        try {
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            // peek به فایل تأییدشده نیاز دارد؛ فایل تازه را موقتاً verified می‌کنیم و در
            // صورت شکست، وضعیت/فایل قبلی برگردانده می‌شود.
            markVerified(ctx, cacheKey)
            val head = peek(ctx, cacheKey)
            if (head == null || head.size < 64) {
                error("فایل ناقص دانلود شد؛ دوباره تلاش کن.")
            }
            if (total >= 4096 && !tailMatchesServer(ctx, url, cacheKey, total)) {
                error("انتهای فایل با سرور یکی نبود؛ دوباره تلاش کن.")
            }
            remoteVersion(url)?.let { saveRemoteVersion(ctx, cacheKey, url, it) }
            runCatching { backup.delete() }
        } catch (error: Throwable) {
            runCatching { target.delete() }
            if (hadPrevious && backup.exists() && backup.renameTo(target)) {
                if (previousVerified) markVerified(ctx, cacheKey) else unmarkVerified(ctx, cacheKey)
            } else {
                unmarkVerified(ctx, cacheKey)
            }
            throw error
        }
    }

    /** مقایسهٔ ~۶۴ بایتِ آخرِ رمزگشاشده با همان بازه از سرور. */
    private fun tailMatchesServer(ctx: Context, url: String, cacheKey: String, total: Long): Boolean = runCatching {
        val alignStart = ((total - 64) / 16) * 16
        val len = (total - alignStart).toInt()
        if (len <= 0) return@runCatching true
        val conn = ResilientHttp.open(
            url,
            range = "bytes=$alignStart-${total - 1}",
            connectMs = 10000,
            readMs = 20000,
            attempts = 3,
        )
        val remote = try {
            if (conn.responseCode != 206) return@runCatching true // سرور Range نمی‌دهد ⇒ سخت نگیر
            conn.inputStream.readBytes()
        } finally {
            conn.disconnect()
        }
        if (remote.size != len) return@runCatching true
        val f = vaultFile(ctx, cacheKey)
        val headerLen = (MAGIC.toByteArray(Charsets.US_ASCII).size + 16).toLong()
        val iv = ByteArray(16)
        java.io.RandomAccessFile(f, "r").use { raf ->
            raf.seek(MAGIC.toByteArray(Charsets.US_ASCII).size.toLong())
            raf.readFully(iv)
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(ctx), IvParameterSpec(counterIv(iv, alignStart / 16)))
            raf.seek(headerLen + alignStart)
            val enc = ByteArray(len)
            raf.readFully(enc)
            val plain = cipher.doFinal(enc)
            plain.contentEquals(remote)
        }
    }.getOrDefault(false)

    /**
     * اندازه‌ی کلِ فایل از هدرِ `Content-Range`.
     *
     * باکتِ Appwrite پاسخِ chunked می‌دهد (بدونِ `Content-Length`) و rangeِ
     * تک‌بایتی `bytes=0-0` را با **416** رد می‌کند، ولی `bytes=0-1023` را با
     * `206` و هدرِ `Content-Range: bytes 0-1023/<total>` جواب می‌دهد؛ پس کاوش
     * با ۱ کیلوبایت انجام می‌شود (و اگر پاسخِ ۴۱۶ هم «ستاره/اندازه» برگرداند،
     * همان خوانده می‌شود).
     */
    private fun probeSize(url: String): Long {
        // ۱) «bytes=0-1023» → ۲۰۶ با «Content-Range: bytes 0-1023/کل»
        val a = probeTotal(url, "bytes=0-1023")
        if (a != null && a > 0) return a
        // ۲) بعضی لبه‌های CDN رنج را نادیده می‌گیرند (۲۰۰ بدونِ Content-Length):
        //    آن‌گاه «bytes=0-0» → ۴۱۶ با «Content-Range: bytes ستاره/کل»
        val b = probeTotal(url, "bytes=0-0")
        if (b != null && b > 0) return b
        return -1L
    }

    /** یک درخواستِ رنجِ کوچک فقط برای خواندنِ اندازه‌ی کل از «Content-Range». */
    private fun probeTotal(url: String, range: String): Long? = runCatching {
        val c = ResilientHttp.open(url, range = range, connectMs = 8000, readMs = 8000, attempts = 3)
        val code = c.responseCode
        val header = c.getHeaderField("Content-Range")
        val len = c.contentLengthLong
        runCatching { c.inputStream.close() }
        runCatching { c.errorStream?.close() }
        c.disconnect()
        val total = totalFromContentRange(header)
        when {
            total > 0 -> total
            code == 200 && len > 0 -> len
            else -> null
        }
    }.getOrNull()

    /**
     * بازکردنِ جریانِ خوانشِ جسته‌گریخته روی فایلِ گاوصندوق (برای پخش).
     * برخلاف [decryptToMemory] کل فایل را در حافظه نمی‌ریخت و رمزگشایی نمی‌کند؛
     * هر بلاک درست لحظه‌ی درخواستِ پلیر رمزگشایی می‌شود — بنابراین بازکردن آنی است
     * و STATE_BUFFERINGِ ساختگیِ «پخش محلی شروع نشد» از بین می‌رود.
     */
    fun openPlainStream(ctx: Context, cacheKey: String): com.hamyareman.ir.platform.feature.playback.VaultStream? {
        val f = vaultFile(ctx, cacheKey)
        if (!isVerified(ctx, cacheKey) || !f.exists() || f.length() <= 20) return null
        return runCatching {
            val raf = java.io.RandomAccessFile(f, "r")
            val header = ByteArray(20)
            raf.seek(0)
            raf.readFully(header)
            if (String(header, 0, 4, Charsets.US_ASCII) != MAGIC) {
                raf.close()
                return null
            }
            val raw = key(ctx).encoded ?: return null
            com.hamyareman.ir.platform.feature.playback.CtrPlainReader(raw, header.copyOfRange(4, 20), 20L, f.length() - 20, raf)
        }.getOrNull()
    }

    /** رمزگشایی کامل به حافظه — فقط داخل اپ و برای پخش (خروجی هرگز ذخیره نمی‌شود). */
    fun decryptToMemory(ctx: Context, cacheKey: String): ByteArray {
        val f = vaultFile(ctx, cacheKey)
        val all = f.readBytes()
        if (all.size <= 20 || !all.startsWith(MAGIC.toByteArray(Charsets.US_ASCII))) {
            error("فایل گاوصندوق معتبر نیست.")
        }
        val iv = all.copyOfRange(4, 20)
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(ctx), IvParameterSpec(iv))
        val plain = cipher.doFinal(all, 20, all.size - 20)
        return HtmlCodec.unwrap(ctx, plain)
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) if (this[i] != prefix[i]) return false
        return true
    }

    /**
     * آدرسِ پخشِ محلیِ رمزگشایی‌شده‌ی [cacheKey].
     * این آدرس دیگر روی شبکه نمی‌رود: با طرحِ `vault://` مستقیم در همان پروسه و
     * بدون سوکت/پورت/توکن خوانده می‌شود (نگاه کنید به VaultDataSource).
     */
    fun localUrl(ctx: Context, cacheKey: String): String = "vault://$cacheKey"
}

/**
 * تجزیه‌ی هدرِ `Content-Range` برای گرفتنِ اندازه‌ی کل:
 * «bytes 0-1023/23947850» ⇒ 23947850 و «bytes ستاره/12345» ⇒ 12345؛
 * در نبودِ عدد (ستاره یا هدرِ غایب) ⇒ 1-.
 */
internal fun totalFromContentRange(header: String?): Long {
    val total = header?.substringAfterLast('/', "")?.trim()
    if (total.isNullOrEmpty() || total == "*") return -1L
    return total.toLongOrNull()?.takeIf { it > 0 } ?: -1L
}

/**
 * سرور حلقه‌ی محلی (۱۲۷.۰.۰.۱) — تنها دروازه‌ی پخش فایل‌های رمزشده.
 * به درخواست‌های Range جواب می‌دهد (سازگار با MediaPlayer و ExoPlayer).
 */
object LocalMediaServer {

    private var port: Int = -1
    private var secret: String = ""
    private var lastKey: String? = null
    private var lastBytes: ByteArray? = null
    private var appCtx: Context? = null

    @Synchronized
    fun ensureStarted(ctx: Context) {
        if (port > 0) return
        appCtx = ctx.applicationContext
        secret = (0..3).joinToString("") { "${System.nanoTime()}" } + ctx.packageName
        val sock = ServerSocket(0, 8, java.net.InetAddress.getByName("127.0.0.1"))
        port = sock.localPort
        val server = sock
        Thread {
            while (true) {
                runCatching {
                    val conn = server.accept()
                    Thread { handle(appCtx ?: return@Thread, conn) }.start()
                }.onFailure { runCatching { Thread.sleep(80) } }
            }
        }.apply { isDaemon = true; name = "hamyar-media-server" }.start()
    }

    fun urlFor(ctx: Context, cacheKey: String): String {
        ensureStarted(ctx)
        val token = token(cacheKey)
        return "http://127.0.0.1:$port/v/$cacheKey?t=$token"
    }

    private fun token(cacheKey: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest("$cacheKey#$secret".toByteArray())
        return d.joinToString("") { "%02x".format(it) }.take(24)
    }

    private fun contentType(key: String): String = when {
        key.endsWith(".mp3") -> "audio/mpeg"
        key.endsWith(".m4a") -> "audio/mp4"
        key.endsWith(".mp4") -> "video/mp4"
        key.endsWith(".ogg") -> "audio/ogg"
        else -> "application/octet-stream"
    }

    private fun handle(ctx: Context, conn: Socket) {
        /** پاسخِ خطا — به‌جای بسته‌شدنِ بی‌صدا، پلیر خطای واقعی ببیند و فال‌بک کند. */
        fun fail(code: Int) {
            runCatching {
                conn.getOutputStream().apply {
                    write("HTTP/1.1 $code ERROR\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                    flush()
                }
            }
        }
        runCatching {
            val reader = conn.getInputStream().bufferedReader()
            val requestLine = reader.readLine() ?: return
            var rangeStart: Long = -1
            var rangeEnd: Long = -1
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Range:", ignoreCase = true)) {
                    val m = Regex("bytes=(\\d*)-(\\d*)").find(line.substring(6).trim())
                    if (m != null) {
                        rangeStart = m.groupValues[1].toLongOrNull() ?: -1
                        rangeEnd = m.groupValues[2].toLongOrNull() ?: -1
                    }
                }
            }
            // GET /v/<key>?t=<token>
            val parts = requestLine.split(" ")
            if (parts.size < 2) { fail(400); return }
            val path = parts[1]
            val seg = Regex("/v/([^?]+)\\?t=([0-9a-f]+)").find(path) ?: run { fail(400); return }
            val (cacheKey, token) = seg.destructured
            if (token(cacheKey) != token) { fail(403); return }

            val bytes = synchronized(this) {
                if (cacheKey == lastKey && lastBytes != null) lastBytes!!
                else {
                    val b = try {
                        MediaVault.decryptToMemory(ctx.applicationContext, cacheKey)
                    } catch (e: Exception) {
                        conn.getOutputStream().write(
                            ("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").toByteArray(),
                        )
                        conn.getOutputStream().flush()
                        return
                    }
                    lastKey = cacheKey; lastBytes = b
                    b
                }
            }

            val out = conn.getOutputStream()
            val isHead = parts[0].equals("HEAD", true)
            if (rangeStart >= 0) {
                val start = rangeStart
                val end = if (rangeEnd >= 0) rangeEnd.coerceAtMost(bytes.size - 1L) else bytes.size - 1L
                val len = if (end >= start) (end - start + 1).toInt() else 0
                out.write(
                    (
                        "HTTP/1.1 206 Partial Content\r\n" +
                            "Content-Type: ${contentType(cacheKey)}\r\n" +
                            "Content-Length: $len\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Content-Range: bytes $start-$end/${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                        ).toByteArray(),
                )
                if (!isHead && len > 0) out.write(bytes, start.toInt(), len)
            } else {
                out.write(
                    (
                        "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: ${contentType(cacheKey)}\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            "Connection: close\r\n\r\n"
                        ).toByteArray(),
                )
                if (!isHead) out.write(bytes)
            }
            out.flush()
        }
        runCatching { conn.close() }
    }
}
