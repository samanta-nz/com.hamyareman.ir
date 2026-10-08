package com.hamyareman.ir.ui.study

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Keeps the process alive while HTML audio is playing so WebView media can continue
 * through screen-off. The media itself remains owned by the WebView.
 */
class HtmlAudioKeepAliveService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "پخش محتوای تعاملی",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
        wakeLock = runCatching {
            getSystemService(PowerManager::class.java)
                ?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Hamyar:HtmlAudioKeepAlive",
                )
                ?.apply { setReferenceCounted(false) }
        }.getOrNull()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (wakeLock?.isHeld != true) {
            runCatching { wakeLock?.acquire() }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(com.hamyareman.ir.platform.core.notifications.R.drawable.ic_stat_notification)
            .setContentTitle("همیار من")
            .setContentText("محتوای صوتی تعاملی در حال پخش است.")
            .setSilent(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        wakeLock?.let { lock ->
            runCatching { if (lock.isHeld) lock.release() }
        }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "html_media_playback"
        private const val NOTIFICATION_ID = 0x484D

        fun start(context: Context) {
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context.applicationContext, HtmlAudioKeepAliveService::class.java),
                )
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, HtmlAudioKeepAliveService::class.java),
                )
            }
        }
    }
}
