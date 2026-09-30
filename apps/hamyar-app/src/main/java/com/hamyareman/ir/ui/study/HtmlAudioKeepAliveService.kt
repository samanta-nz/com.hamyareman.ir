package com.hamyareman.ir.ui.study

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hamyareman.ir.R

/**
 * فقط عمر process را هنگام پخش صدای HTML روی صفحهٔ قفل نگه می‌دارد؛ خود صدا همچنان
 * داخل WebView پخش می‌شود. مینیمایز/تعویض صفحه ابتدا رسانه را pause و سپس سرویس را
 * متوقف می‌کند، اما خاموش‌شدن صفحه این کار را انجام نمی‌دهد.
 */
class HtmlAudioKeepAliveService : Service() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(CHANNEL, "پخش محتوای تعاملی", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
