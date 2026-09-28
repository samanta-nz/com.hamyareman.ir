package com.hamyareman.ir.platform.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hamyareman.ir.platform.core.common.LocalStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** یک یادآور روزانه‌ی محلی (آب، روتین، مرور درس). */
data class Reminder(
    val id: String,
    val title: String,
    val body: String,
    val hour: Int,
    val minute: Int,
    val channel: String = NotificationChannels.REMINDERS,
    val enabled: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("body", body)
        .put("hour", hour)
        .put("minute", minute)
        .put("channel", channel)
        .put("enabled", enabled)

    companion object {
        fun fromJson(o: JSONObject): Reminder = Reminder(
            id = o.optString("id"),
            title = o.optString("title"),
            body = o.optString("body"),
            hour = o.optInt("hour", 18).coerceIn(0, 23),
            minute = o.optInt("minute", 0).coerceIn(0, 59),
            channel = o.optString("channel", NotificationChannels.REMINDERS),
            enabled = o.optBoolean("enabled", true),
        )
    }
}

/**
 * زمان‌بندی یادآورها با AlarmManager.
 *
 * عمداً از `setAndAllowWhileIdle` (غیردقیق) استفاده می‌کنیم تا نه به مجوز
 * SCHEDULE_EXACT_ALARM نیاز باشد و نه باتری کاربر درگیر شود؛ یادآور «آب بخور»
 * دقیقه‌ی دقیق نمی‌خواهد.
 *
 * همه‌ی یادآورها در ساعات سکوت ([QuietHoursManager]) نشان داده نمی‌شوند.
 */
class ReminderScheduler(private val context: Context) {

    private val store = LocalStore(context, REMINDER_STORE)
    val quietHours = QuietHoursManager(store)

    fun all(): List<Reminder> = runCatching {
        val array = JSONArray(store.getString(KEY_REMINDERS, "[]"))
        buildList {
            for (i in 0 until array.length()) add(Reminder.fromJson(array.getJSONObject(i)))
        }
    }.getOrDefault(emptyList())

    fun find(id: String): Reminder? = all().firstOrNull { it.id == id }

    fun upsert(reminder: Reminder) {
        val list = all().filterNot { it.id == reminder.id } + reminder
        persist(list)
        schedule(reminder)
    }

    fun remove(id: String) {
        cancel(id)
        persist(all().filterNot { it.id == id })
    }

    fun setEnabled(id: String, enabled: Boolean) {
        find(id)?.let { upsert(it.copy(enabled = enabled)) }
    }

    fun schedule(reminder: Reminder) {
        if (!reminder.enabled) {
            cancel(reminder.id)
            return
        }
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTriggerMillis(reminder.hour, reminder.minute, System.currentTimeMillis())
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(reminder))
    }

    fun cancel(id: String) {
        val reminder = find(id) ?: return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pendingIntent(reminder))
    }

    /** بعد از ریبوت دستگاه یا تغییر ساعات سکوت. */
    fun rescheduleAll() {
        all().filter { it.enabled }.forEach { schedule(it) }
    }

    private fun persist(list: List<Reminder>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        store.putString(KEY_REMINDERS, array.toString())
    }

    private fun pendingIntent(reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_BODY, reminder.body)
            putExtra(ReminderReceiver.EXTRA_CHANNEL, reminder.channel)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** نام فروشگاه یادآورها — مستقل از استور هر اپ تا رسیورها خودکفا بمانند. */
        const val REMINDER_STORE = "platform_reminders"
        private const val KEY_REMINDERS = "reminders"

        /**
         * نزدیک‌ترین زمان امروز/فردا برای ساعت و دقیقه‌ی خواسته‌شده.
         * اگر در ساعات سکوت بیفتد، به ابتدای بازه‌ی بیداری منتقل می‌شود.
         */
        fun nextTriggerMillis(hour: Int, minute: Int, nowMs: Long, quietEndHour: Int? = null): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (calendar.timeInMillis <= nowMs) calendar.add(Calendar.DAY_OF_YEAR, 1)
            quietEndHour?.let { end ->
                val triggerHour = calendar.get(Calendar.HOUR_OF_DAY)
                if (triggerHour in 0 until end.coerceIn(1, 23)) {
                    calendar.set(Calendar.HOUR_OF_DAY, end)
                    calendar.set(Calendar.MINUTE, 0)
                }
            }
            return calendar.timeInMillis
        }
    }
}
