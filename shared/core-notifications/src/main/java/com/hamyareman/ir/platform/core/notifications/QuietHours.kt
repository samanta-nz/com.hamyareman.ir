package com.hamyareman.ir.platform.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import java.time.LocalTime

/**
 * ساعات سکوت: بازه‌ای که هیچ اعلانی (یادآور، پیام، تماس غیرضروری) نشان داده نمی‌شود.
 * پیش‌فرض ۲۲ تا ۷ است چون خواب نوجوان بخشی از طراحی محصول است، نه یک تنظیم اختیاری.
 */
class QuietHoursManager(private val store: LocalStore) {

    data class State(val enabled: Boolean, val startHour: Int, val endHour: Int)

    fun state(): State = State(
        enabled = store.getBool(KEY_ENABLED, true),
        startHour = store.getInt(KEY_START, DEFAULT_START).coerceIn(0, 23),
        endHour = store.getInt(KEY_END, DEFAULT_END).coerceIn(0, 23),
    )

    fun update(enabled: Boolean? = null, startHour: Int? = null, endHour: Int? = null) {
        val current = state()
        store.putBool(KEY_ENABLED, enabled ?: current.enabled)
        store.putInt(KEY_START, (startHour ?: current.startHour).coerceIn(0, 23))
        store.putInt(KEY_END, (endHour ?: current.endHour).coerceIn(0, 23))
    }

    fun isQuietNow(): Boolean = isQuietAt(LocalTime.now())

    fun isQuietAt(time: LocalTime): Boolean {
        val state = state()
        if (!state.enabled) return false
        val hour = time.hour
        val start = state.startHour
        val end = state.endHour
        if (start == end) return false
        return if (start > end) hour >= start || hour < end else hour in start until end
    }

    companion object {
        private const val KEY_ENABLED = "quiet_enabled"
        private const val KEY_START = "quiet_start"
        private const val KEY_END = "quiet_end"
        const val DEFAULT_START = 22
        const val DEFAULT_END = 7
    }
}

/** شناسه‌ی کانال‌های اعلان — همین نام‌ها باید در کنسول/FCM هم استفاده شوند. */
object NotificationChannels {
    const val MESSAGES = "messages"
    const val CALLS = "calls"
    const val REMINDERS = "reminders"
    const val WEEKLY = "weekly"
    const val SYNC = "sync"
    const val SCHOOL_ALARM = "school_alarm"
    const val UPDATES = "updates"

    val all: List<String> = listOf(MESSAGES, CALLS, REMINDERS, WEEKLY, SYNC, SCHOOL_ALARM, UPDATES)

    private data class ChannelSpec(val id: String, val name: String, val importance: Int)

    private fun specs(): List<ChannelSpec> = listOf(
        ChannelSpec(MESSAGES, "پیام‌های حرف دل", NotificationManager.IMPORTANCE_HIGH),
        ChannelSpec(CALLS, "تماس", NotificationManager.IMPORTANCE_HIGH),
        ChannelSpec(REMINDERS, "یادآورهای ملایم", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelSpec(WEEKLY, "خلاصه‌ی هفتگی", NotificationManager.IMPORTANCE_LOW),
        ChannelSpec(SYNC, "همگام‌سازی", NotificationManager.IMPORTANCE_MIN),
        ChannelSpec(SCHOOL_ALARM, "آلارم مدرسه و خواب", NotificationManager.IMPORTANCE_HIGH),
    )

    /** ساخت کانال‌ها — باید یک‌بار در Application.onCreate صدا زده شود. */
    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        specs().forEach { spec ->
            val channel = NotificationChannel(spec.id, spec.name, spec.importance).apply {
                description = spec.name
                setShowBadge(spec.importance >= NotificationManager.IMPORTANCE_DEFAULT)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
