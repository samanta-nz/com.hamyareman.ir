package com.hamyareman.ir.ui.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hamyareman.ir.di.AppContainer
import com.hamyareman.ir.ui.net.NetState

/**
 * سینک پایدار خارج از عمر UI/process.
 *
 * WorkManager اجرای کار را به شرایط شبکه و چرخهٔ عمر سیستم می‌سپارد؛
 * خود صف SyncEngine و HeartRepository همچنان منبع حقیقت محلی هستند.
 */
class BackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        if (!NetState.isOnline(app)) return Result.retry()

        val container = runCatching { AppContainer(app) }
            .getOrElse { return Result.retry() }

        if (!container.isBackendConfigured) return Result.success()

        runCatching { SyncCenter.pushNow(app, container) }

        return if (
            container.sync.pendingCount() == 0 &&
            container.heart.pendingCount() == 0
        ) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
