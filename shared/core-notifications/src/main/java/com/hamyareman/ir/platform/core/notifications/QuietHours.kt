package com.hamyareman.ir.platform.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hamyareman.ir.platform.core.common.LocalStore
import java.time.LocalTime
import java.util.Calendar

/** یک بازهٔ روزانهٔ سکوت با دقت دقیقه؛ شروع و پایان برابر یعنی بازهٔ نامعتبر/خاموش. */
class QuietHoursManager(private val store: LocalStore) {

    data class State(
        val enabled: Boolean,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int,
    )

    fun state(): State = State(
        enabled = store.getBool(KEY_ENABLED, false),
        startHour = store.getInt(KEY_START_HOUR, DEFAULT_START_HOUR).coerceIn(0, 23),
        startMinute = store.getInt(KEY_START_MINUTE, DEFAULT_START_MINUTE).coerceIn(0, 59),
        endHour = store.getInt(KEY_END_HOUR, DEFAULT_END_HOUR).coerceIn(0, 23),
        endMinute = store.getInt(KEY_END_MINUTE, DEFAULT_END_MINUTE).coerceIn(0, 59),
    )

    fun update(
        enabled: Boolean? = null,
        startHour: Int? = null,
        startMinute: Int? = null,
        endHour: Int? = null,
        endMinute: Int? = null,
    ) {
        val current = state()
        store.putBool(KEY_ENABLED, enabled ?: current.enabled)
        store.putInt(KEY_START_HOUR, (startHour ?: current.startHour).coerceIn(0, 23))
        store.putInt(KEY_START_MINUTE, (startMinute ?: current.startMinute).coerceIn(0, 59))
        store.putInt(KEY_END_HOUR, (endHour ?: current.endHour).coerceIn(0, 23))
        store.putInt(KEY_END_MINUTE, (endMinute ?: current.endMinute).coerceIn(0, 59))
    }

    fun isQuietNow(): Boolean = isQuietAt(LocalTime.now())

    fun isQuietAt(time: LocalTime): Boolean {
        val state = state()
        if (!state.enabled) return false
        val now = time.hour * 60 + time.minute
        val start = state.startHour * 60 + state.startMinute
        val end = state.endHour * 60 + state.endMinute
        if (start == end) return false
        return if (start > end) now >= start || now < end else now in start until end
    }

    companion object {
        private const val KEY_ENABLED = "quiet_enabled"
        private const val KEY_START_HOUR = "quiet_start"
        private const val KEY_START_MINUTE = "quiet_start_minute"
        private const val KEY_END_HOUR = "quiet_end"
        private const val KEY_END_MINUTE = "quiet_end_minute"
        const val DEFAULT_START_HOUR = 22
        const val DEFAULT_START_MINUTE = 0
        const val DEFAULT_END_HOUR = 7
        const val DEFAULT_END_MINUTE = 0
    }
}

/**
 * زمان‌بندی و اعمال ساعات سکوت روی خود گوشی.
 *
 * اندروید اجازهٔ تغییر حالت مزاحم‌نشو را فقط پس از تأیید صریح کاربر می‌دهد. وقتی
 * دسترسی وجود داشته باشد، بازه با «مزاحم‌نشو: سکوت کامل» و RINGER_MODE_SILENT
 * اجرا می‌شود؛ وضعیت قبلی در پایان بازگردانده می‌شود. بدون دسترسی، تنظیم ذخیره
 * می‌ماند اما هرگز وانمود نمی‌کنیم که گوشی سایلنت شده است.
 */
object QuietHoursAutomation {
    private const val ACTION_START = "com.hamyareman.ir.QUIET_START"
    private const val ACTION_END = "com.hamyareman.ir.QUIET_END"
    const val ACTION_DISABLE = "com.hamyareman.ir.QUIET_DISABLE"
    private const val KEY_ACTIVE = "quiet_system_active"
    private const val KEY_PREVIOUS_FILTER = "quiet_previous_filter"
    private const val KEY_PREVIOUS_RINGER = "quiet_previous_ringer"
    private const val KEY_PREVIOUS_ALARM_VOLUME = "quiet_previous_alarm_volume"
    private const val NOTIFICATION_ID = 0x5148

    fun manager(context: Context): QuietHoursManager =
        QuietHoursManager(LocalStore(context.applicationContext, ReminderScheduler.REMINDER_STORE))

    fun hasPolicyAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    fun policySettingsIntent(): Intent =
        Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun schedule(context: Context) {
        val ctx = context.applicationContext
        val alarm = ctx.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pending(ctx, ACTION_START, 0x5141))
        alarm.cancel(pending(ctx, ACTION_END, 0x5142))
        val state = manager(ctx).state()
        if (state.enabled &&
            (state.startHour != state.endHour || state.startMinute != state.endMinute)
        ) {
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextOccurrence(state.startHour, state.startMinute),
                pending(ctx, ACTION_START, 0x5141),
            )
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextOccurrence(state.endHour, state.endMinute),
                pending(ctx, ACTION_END, 0x5142),
            )
        }
        syncNow(ctx)
    }

    fun syncNow(context: Context) {
        val quiet = manager(context)
        if (quiet.isQuietNow()) activate(context) else deactivate(context)
    }

    fun disable(context: Context) {
        manager(context).update(enabled = false)
        schedule(context)
        deactivate(context)
    }

    internal fun activate(context: Context) {
        val ctx = context.applicationContext
        val stateStore = LocalStore(ctx, ReminderScheduler.REMINDER_STORE)
        val manager = ctx.getSystemService(NotificationManager::class.java)
        val audio = ctx.getSystemService(AudioManager::class.java)
        val granted = manager?.isNotificationPolicyAccessGranted == true
        if (granted) {
            // مقدار قبلی فقط بار اول ذخیره می‌شود؛ اما حالت سکوت هر بار دوباره اعمال
            // می‌شود تا reboot یا بازنشانی موقت سیستم بازه را بی‌اثر نکند.
            if (!stateStore.getBool(KEY_ACTIVE, false)) {
                stateStore.putInt(
                    KEY_PREVIOUS_FILTER,
                    manager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL,
                )
                stateStore.putInt(KEY_PREVIOUS_RINGER, audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL)
                stateStore.putInt(
                    KEY_PREVIOUS_ALARM_VOLUME,
                    audio?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0,
                )
            }
            runCatching { manager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE) }
            runCatching { audio?.ringerMode = AudioManager.RINGER_MODE_SILENT }
            runCatching { audio?.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0) }
            stateStore.putBool(KEY_ACTIVE, true)
        }
        showStatus(ctx, granted)
    }

    internal fun deactivate(context: Context) {
        val ctx = context.applicationContext
        val stateStore = LocalStore(ctx, ReminderScheduler.REMINDER_STORE)
        if (stateStore.getBool(KEY_ACTIVE, false)) {
            val manager = ctx.getSystemService(NotificationManager::class.java)
            val audio = ctx.getSystemService(AudioManager::class.java)
            if (manager?.isNotificationPolicyAccessGranted == true) {
                val previousFilter = stateStore.getInt(
                    KEY_PREVIOUS_FILTER,
                    NotificationManager.INTERRUPTION_FILTER_ALL,
                )
                runCatching { manager.setInterruptionFilter(previousFilter) }
            }
            val previousRinger = stateStore.getInt(KEY_PREVIOUS_RINGER, AudioManager.RINGER_MODE_NORMAL)
            val previousAlarmVolume = stateStore.getInt(KEY_PREVIOUS_ALARM_VOLUME, 1)
            runCatching { audio?.ringerMode = previousRinger }
            runCatching { audio?.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, 0) }
            stateStore.putBool(KEY_ACTIVE, false)
        }
        NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID)
    }

    private fun showStatus(context: Context, policyGranted: Boolean) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val disable = PendingIntent.getBroadcast(
            context,
            0x5143,
            Intent(context, QuietHoursReceiver::class.java).setAction(ACTION_DISABLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = if (policyGranted) {
            "ساعات سکوت فعال است؛ صداها و آلارم‌های گوشی تا پایان بازه بی‌صدا هستند."
        } else {
            "ساعات سکوت فعال است، اما برای سایلنت‌کردن گوشی دسترسی «مزاحم نشو» لازم است."
        }
        val notification = NotificationCompat.Builder(context, NotificationChannels.QUIET_STATUS)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle("ساعات سکوت")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "غیرفعال شود", disable)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun pending(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, QuietHoursReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun nextOccurrence(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

    internal fun onAlarm(context: Context, action: String?) {
        when (action) {
            ACTION_START -> activate(context)
            ACTION_END -> deactivate(context)
            ACTION_DISABLE -> disable(context)
        }
        if (action != ACTION_DISABLE) schedule(context)
    }
}

class QuietHoursReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        QuietHoursAutomation.onAlarm(context, intent?.action)
    }
}

/** شناسهٔ کانال‌های اعلان — همین نام‌ها باید در کنسول/FCM هم استفاده شوند. */
object NotificationChannels {
    const val MESSAGES = "messages"
    const val CALLS = "calls"
    const val REMINDERS = "reminders"
    const val WEEKLY = "weekly"
    const val SYNC = "sync"
    const val SCHOOL_ALARM = "school_alarm"
    const val UPDATES = "updates"
    const val QUIET_STATUS = "quiet_status"

    val all: List<String> = listOf(
        MESSAGES, CALLS, REMINDERS, WEEKLY, SYNC, SCHOOL_ALARM, UPDATES, QUIET_STATUS,
    )

    private data class ChannelSpec(val id: String, val name: String, val importance: Int)

    private fun specs(): List<ChannelSpec> = listOf(
        ChannelSpec(MESSAGES, "پیام‌های حرف دل", NotificationManager.IMPORTANCE_HIGH),
        ChannelSpec(CALLS, "تماس", NotificationManager.IMPORTANCE_HIGH),
        ChannelSpec(REMINDERS, "یادآورهای ملایم", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelSpec(WEEKLY, "خلاصهٔ هفتگی", NotificationManager.IMPORTANCE_LOW),
        ChannelSpec(SYNC, "همگام‌سازی", NotificationManager.IMPORTANCE_MIN),
        ChannelSpec(SCHOOL_ALARM, "آلارم مدرسه و خواب", NotificationManager.IMPORTANCE_HIGH),
        ChannelSpec(QUIET_STATUS, "وضعیت ساعات سکوت", NotificationManager.IMPORTANCE_LOW),
    )

    /** ساخت کانال‌ها — باید یک‌بار در Application.onCreate صدا زده شود. */
    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        specs().forEach { spec ->
            val channel = NotificationChannel(spec.id, spec.name, spec.importance).apply {
                description = spec.name
                setShowBadge(spec.importance >= NotificationManager.IMPORTANCE_DEFAULT)
                if (spec.id == QUIET_STATUS) {
                    setSound(null, null)
                    enableVibration(false)
                }
            }
            manager.createNotificationChannel(channel)
        }
    }
}
