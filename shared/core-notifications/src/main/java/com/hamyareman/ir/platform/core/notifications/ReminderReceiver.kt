package com.hamyareman.ir.platform.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.SchoolOffCache
import com.hamyareman.ir.platform.core.notifications.R

/**
 * یادآور را به اعلان تبدیل می‌کند.
 *
 * دو اصل محصول اینجا اعمال می‌شود:
 *  1) در ساعات سکوت هیچ اعلانی نشان داده نمی‌شود (مگر تماس).
 *  2) بعد از نمایش، یادآور فردا دوباره زمان‌بندی می‌شود (تکرار روزانه).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: NotificationChannels.REMINDERS

        val scheduler = ReminderScheduler(context)
        val school = id.startsWith("school_")
        val schoolDayAlarm = school && id != "school_sleep"
        if (schoolDayAlarm && SchoolOffCache.isOff(context)) {
            // تعطیل رسمی، پنجشنبه/جمعه یا مرخصی ثبت‌شده: بیداری/سرویس/حضور خاموش.
            scheduler.find(id)?.let { scheduler.schedule(it) }
            return
        }
        if (!school && scheduler.quietHours.isQuietNow()) {
            // سکوت یعنی سکوت: فقط فردا دوباره زمان‌بندی می‌کنیم.
            scheduler.find(id)?.let { scheduler.schedule(it) }
            return
        }

        show(context, id, title, body, channel, openSleep = id == "school_sleep")
        scheduler.find(id)?.let { scheduler.schedule(it) }
        if (school) scheduleRepeatIfNeeded(context, id, title, body, channel)
    }

    private fun show(
        context: Context,
        id: String,
        title: String,
        body: String,
        channel: String,
        openSleep: Boolean = false,
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (openSleep) putExtra(EXTRA_OPEN_SLEEP, true)
        }
        val contentIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                id.hashCode(),
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val school = channel == NotificationChannels.SCHOOL_ALARM || id.startsWith("school_")
        // زنگِ واقعی: آهنگِ انتخابیِ کاربر با صدای خودمان پخش می‌شود (نه فقط صدای اعلان).
        if (school) {
            AlarmRinger.start(context)
        }
        val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPi = PendingIntent.getBroadcast(
            context,
            (id + "_stop").hashCode(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            context,
            if (school) NotificationChannels.SCHOOL_ALARM else channel,
        )
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (school) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (school) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .apply {
                if (school) {
                    // صدا را خودمان (AlarmRinger) می‌زنیم؛ اعلان فقط لرزش و نمایش.
                    setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    setSound(null)
                    addAction(
                        android.R.drawable.ic_lock_idle_alarm,
                        "توقف زنگ",
                        stopPi,
                    )
                } else {
                    setDefaults(NotificationCompat.DEFAULT_ALL)
                }
                contentIntent?.let { setContentIntent(it) }
            }
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id.hashCode(), notification) }
        // ذخیره‌ی «آخرین یادآور نمایش‌داده‌شده» برای آمار ملایم (بدون فشار به کاربر)
        LocalStore(context, ReminderScheduler.REMINDER_STORE).putLong("last_shown_${id}", System.currentTimeMillis())
    }

    private fun scheduleRepeatIfNeeded(
        context: Context,
        id: String,
        title: String,
        body: String,
        channel: String,
    ) {
        if (id.endsWith("_r")) return
        val store = LocalStore(context, "hamyar_class_plan")
        val times = store.getInt("alarm_repeat", 2).coerceIn(1, 5)
        if (times < 2) return
        val alarm = context.getSystemService(android.app.AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, "${id}_r")
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_CHANNEL, channel)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            "${id}_r".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarm.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5 * 60_000L, pi)
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_BODY = "reminder_body"
        const val EXTRA_CHANNEL = "reminder_channel"
        const val EXTRA_OPEN_SLEEP = "open_sleep"
        const val ACTION_STOP_ALARM = "com.hamyareman.ir.STOP_ALARM"
    }
}
