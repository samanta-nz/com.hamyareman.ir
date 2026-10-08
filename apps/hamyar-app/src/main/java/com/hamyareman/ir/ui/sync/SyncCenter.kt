package com.hamyareman.ir.ui.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.hamyareman.ir.di.AppContainer
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.study.MediaVault
import com.hamyareman.ir.ui.study.StateSync
import com.hamyareman.ir.ui.profile.AvatarSync
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

/** آنچه در صفحهٔ «کش و همگام‌سازی» نشان داده می‌شود. */
data class SyncStatus(
    val online: Boolean,
    val backendConfigured: Boolean,
    val cacheBytes: Long,
    val cacheItems: Int,
    val mediaBytes: Long,
    val mediaFiles: Int,
    val stateRows: Int,
    val outbox: Int,
    val heartPending: Int,
    val lastSyncAt: Long,
    val lastError: String?,
    val limitMb: Int,
)

/**
 * مرکزِ کش و همگام‌سازی.
 *
 * قراردادِ کلِ اپ: **اول روی گوشی ذخیره، بعد فرستادن.** هر داده‌ای که باید سینک
 * شود بی‌درنگ در حافظهٔ محلی و صفِ ارسالِ [AppContainer.sync] می‌نشیند؛ اگر
 * اینترنت نبود هیچ داده‌ای نمی‌رود زیر و به‌محضِ برگشتنِ شبکه، [installAutoSync]
 * خودش (بدونِ دکمه و بدونِ دخالتِ کاربر) صف را می‌فرستد.
 */
object SyncCenter {

    private const val PREF = "hamyar_cache"
    private const val KEY_LIMIT_MB = "limit_mb"
    private const val WORK_NAME = "hamyar-background-sync"

    /** سقفِ پیش‌فرضِ کش (مگابایت) — کاربر می‌تواند عوضش کند. */
    const val DEFAULT_LIMIT_MB = 512
    const val MIN_LIMIT_MB = 64
    const val MAX_LIMIT_MB = 4096

    private var started = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun cacheLimitMb(ctx: Context): Int =
        store(ctx).getInt(KEY_LIMIT_MB, DEFAULT_LIMIT_MB).coerceIn(MIN_LIMIT_MB, MAX_LIMIT_MB)

    fun setCacheLimitMb(ctx: Context, mb: Int) {
        store(ctx).putInt(KEY_LIMIT_MB, mb.coerceIn(MIN_LIMIT_MB, MAX_LIMIT_MB))
        enforceCacheLimit(ctx)
    }

    private fun folderBytes(dir: File?): Long =
        if (dir == null || !dir.exists()) 0L
        else dir.listFiles()?.sumOf { if (it.isDirectory) folderBytes(it) else it.length() } ?: 0L

    private fun folderCount(dir: File?): Int =
        if (dir == null || !dir.exists()) 0
        else dir.listFiles()?.sumOf { if (it.isDirectory) folderCount(it) else 1 } ?: 0

    fun cacheBytes(ctx: Context): Long = MediaVault.cachedBytes(ctx) + folderBytes(ctx.cacheDir)

    /** چند «قلم» دادهٔ محلی نگه داشته‌ایم (رسانه + ردیف‌های حالتِ سینک‌شده). */
    fun cacheItems(ctx: Context): Int =
        folderCount(File(ctx.filesDir, "media-vault")) +
            folderCount(ctx.cacheDir) +
            LocalStore(ctx, "hamyar_state_sync").keysWithPrefix("at_").size

    fun status(ctx: Context, container: AppContainer): SyncStatus {
        val mediaBytes = MediaVault.cachedBytes(ctx)
        val mediaFiles = folderCount(File(ctx.filesDir, "media-vault"))
        return SyncStatus(
            online = NetState.isOnline(ctx),
            backendConfigured = container.isBackendConfigured,
            cacheBytes = cacheBytes(ctx),
            cacheItems = cacheItems(ctx),
            mediaBytes = mediaBytes,
            mediaFiles = mediaFiles,
            stateRows = LocalStore(ctx, "hamyar_state_sync").keysWithPrefix("at_").size,
            outbox = container.sync.pendingCount(),
            heartPending = container.heart.pendingCount(),
            lastSyncAt = maxOf(container.sync.lastSyncAt(), StateSync.lastSyncAt(ctx)),
            lastError = StateSync.lastError(ctx, StateSync.KEY_CHECKS),
            limitMb = cacheLimitMb(ctx),
        )
    }

    /**
     * رعایتِ سقفِ کش: قدیمی‌ترین فایل‌های رسانه (دانلودشده‌ها) پاک می‌شوند تا حجم
     * به زیرِ سقف بیاید. داده‌های سینک‌شده (برنامه، تیک‌ها، نکته‌ها) دست‌نخورده‌اند؛
     * فقط فایلِ صوتی/تصویریِ دانلودشده دوباره لازم شود از سرور می‌آید.
     */
    fun enforceCacheLimit(ctx: Context) {
        runCatching {
            val limit = cacheLimitMb(ctx) * 1024L * 1024L
            val vault = File(ctx.filesDir, "media-vault")
            var total = MediaVault.cachedBytes(ctx) + folderBytes(ctx.cacheDir)
            if (total <= limit) return
            val files = vault.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: emptyList()
            files.forEach { f ->
                if (total <= limit) return
                val key = f.name.removeSuffix(".enc")
                runCatching { MediaVault.delete(ctx, key) }
                total -= f.length()
            }
            // اگر باز هم جا کم بود، کشِ موقتِ سیستم هم خالی می‌شود (چیزی از دست نمی‌رود).
            if (total > limit) {
                ctx.cacheDir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
            }
        }
    }

    /** پاک‌کردنِ کشِ رسانه‌ها (با تأییدِ کاربر در UI) — دادهٔ سینک‌شده پاک نمی‌شود. */
    fun clearMediaCache(ctx: Context) {
        runCatching {
            File(ctx.filesDir, "media-vault").listFiles()?.forEach { runCatching { it.delete() } }
        }
    }

    /**
     * همگام‌سازیِ خودکار: به‌محضِ برگشتنِ اینترنت صف می‌رود و هر چند دقیقه هم
     * یک تلاشِ دوره‌ای انجام می‌شود. دیگر هیچ دکمه‌ای در UI لازم نیست.
     */
    fun installAutoSync(ctx: Context, container: AppContainer) {
        if (started) return
        started = true
        val app = ctx.applicationContext

        scope.launch {
            // اجرای سریع هنگام برگشتنٔ شبکه، برای اینکه دادهٔ تازه منتظر نوبت
            // دوره‌ای نماند. خودِ WorkManager پایداری پس از خروج process و reboot را
            // تأمین می‌کند.
            delay(1500)
            runCatching {
                val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        scope.launch { pushNow(app, container) }
                    }
                })
            }
            schedulePersistentWorker(app)
        }
    }

    /** زمان‌بندی پایدارِ حداقل هر ۱۵ دقیقه، فقط با شبکهٔ متصل. */
    private fun schedulePersistentWorker(ctx: Context) {
        val request = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** یک تلاشِ ارسال (هیچ‌وقت throw نمی‌کند). */
    suspend fun pushNow(ctx: Context, container: AppContainer) {
        runCatching { container.heart.pushPending() }
        runCatching { container.sync.pushAll() }
        runCatching {
            val uid = container.auth.cachedUserId().orEmpty()
            if (uid.isNotBlank()) AvatarSync.push(ctx, container.storage, uid)
        }
        runCatching { enforceCacheLimit(ctx) }
    }
}
