package com.hamyareman.ir.platform.feature.calls

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.hamyareman.ir.platform.feature.calls.R

/**
 * سرویس پیش‌زمینه‌ی تماس.
 *
 * بدون آن، وقتی صفحه خاموش شود یا کاربر از اپ بیرون برود، اندروید بعد از چند لحظه
 * ضبط میکروفون را متوقف می‌کند و تماس قطع می‌شود. نوع سرویس `microphone` است چون
 * فقط صدا (و در تماس تصویری، دوربین در همان فرایند) استفاده می‌شود.
 */
class CallForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "تماس"
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(label),
            foregroundServiceType(),
        )
        return START_NOT_STICKY
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

    private fun buildNotification(label: String): android.app.Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("تماس در جریان است")
            .setContentText(label)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .build()
    }

    companion object {
        const val EXTRA_LABEL = "call_label"

        /** همان شناسه‌ی کانال «تماس» در `core-notifications` (بدون وابستگی بین ماژول‌ها). */
        const val CHANNEL_ID = "calls"
        private const val NOTIFICATION_ID = 4711

        fun start(context: Context, label: String) {
            val intent = Intent(context, CallForegroundService::class.java)
                .putExtra(EXTRA_LABEL, label)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, CallForegroundService::class.java)) }
        }
    }
}
