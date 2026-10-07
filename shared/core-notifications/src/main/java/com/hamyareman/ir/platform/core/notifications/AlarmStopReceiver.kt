package com.hamyareman.ir.platform.core.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * اکشن اعلان‌های مدرسه: زنگ و اعلان را کامل می‌بندد. برای دعوت خواب، برنامه را
 * حتی از حالت بسته باز می‌کند و مقصد انتخاب‌شده را به MainActivity می‌سپارد.
 */
class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlarmRinger.stop()
        val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        if (notificationId != Int.MIN_VALUE) {
            context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        }
        intent?.getStringExtra(EXTRA_REMINDER_ID)?.let { id ->
            cancelRepeat(context, id)
            // اعلان تکراری (_r) و اصلی هر دو بسته شوند تا اعلان معلق نماند.
            val base = id.removeSuffix("_r")
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.cancel(base.hashCode())
            nm?.cancel("${base}_r".hashCode())
        }

        val destination = intent?.getStringExtra(EXTRA_SLEEP_DESTINATION).orEmpty()
        // از اندروید ۱۲ باز کردن Activity از گیرندهٔ اعلان مسدود است؛ اکشن‌های باز کردن صفحه
        // اکنون مستقیماً PendingIntent.getActivity هستند. این مسیر فقط برای نسخه‌های قدیمی‌تر است.
        if (destination.isNotBlank() && android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(ReminderReceiver.EXTRA_SLEEP_DESTINATION, destination)
            }?.let { context.startActivity(it) }
        }
    }

    private fun cancelRepeat(context: Context, id: String) {
        val repeatId = if (id.endsWith("_r")) id else "${id}_r"
        val repeat = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, repeatId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            repeatId.hashCode(),
            repeat,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java)?.cancel(pi)
        pi.cancel()
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_REMINDER_ID = "notification_reminder_id"
        const val EXTRA_SLEEP_DESTINATION = "sleep_destination"
    }
}
