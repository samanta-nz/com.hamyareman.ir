package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.notifications.NotificationChannels
import com.hamyareman.ir.platform.core.notifications.Reminder
import com.hamyareman.ir.platform.core.notifications.ReminderScheduler
import java.time.LocalDate

/** تنظیمات آلارم مدرسه/خواب — نوتیف صدادار درون اپ. */
object SchoolAlarmStore {

    data class Prefs(
        val sound: String = "default",
        val volume: Int = 80,
        val repeat: Int = 2,
        val crescendo: Boolean = false,
        val wakeMH: Int = 6, val wakeMM: Int = 30,
        val busMH: Int = 6, val busMM: Int = 45,
        val wakeNH: Int = 11, val wakeNM: Int = 30,
        val busNH: Int = 11, val busNM: Int = 45,
        val sleepMH: Int = 21, val sleepMM: Int = 30,
        val sleepNH: Int = 23, val sleepNM: Int = 0)

    const val WAKE_M = ClassPlanStore.ALARM_MORNING
    const val BUS_M = "school_bus_morning"
    const val SCHOOL_M = "school_arrive_morning"
    const val WAKE_N = ClassPlanStore.ALARM_NOON
    const val BUS_N = "school_bus_noon"
    const val SCHOOL_N = "school_arrive_noon"
    const val SLEEP = "school_sleep"

    fun load(ctx: Context): Prefs {
        val s = ClassPlanStore.store(ctx)
        val snap = ClassPlanStore.load(ctx)
        val (wh, wm) = ClassPlanStore.wakeHourMinute(snap, Shift.MORNING)
        val (nh, nm) = ClassPlanStore.wakeHourMinute(snap, Shift.EVENING)
        fun parse(t: String, dh: Int, dm: Int): Pair<Int, Int> {
            val p = t.split(":")
            val h = p.getOrNull(0)?.toIntOrNull() ?: dh
            val m = p.getOrNull(1)?.toIntOrNull() ?: dm
            return h.coerceIn(0, 23) to m.coerceIn(0, 59)
        }
        val (smh, smm) = parse(snap.sleepMorning, 21, 30)
        val (snh, snm) = parse(snap.sleepEvening, 23, 0)
        return Prefs(
            sound = s.getString("alarm_sound").ifBlank { "default" },
            volume = s.getInt("alarm_volume", 80).coerceIn(0, 100),
            repeat = s.getInt("alarm_repeat", 2).coerceIn(1, 5),
            crescendo = s.getBool("alarm_crescendo", false),
            // wakeMH/wakeMM فقط مقدار مشتق‌شده از برنامهٔ شیفت است؛ از ذخیرهٔ مستقل نسخه‌های قدیمی استفاده نمی‌کنیم.
            wakeMH = wh, wakeMM = wm,
            busMH = s.getInt("bus_m_h", 6), busMM = s.getInt("bus_m_m", 45),
            wakeNH = nh, wakeNM = nm,
            busNH = s.getInt("bus_n_h", 11), busNM = s.getInt("bus_n_m", 45),
            sleepMH = s.getInt("sleep_m_h", smh), sleepMM = s.getInt("sleep_m_m", smm),
            sleepNH = s.getInt("sleep_n_h", snh), sleepNM = s.getInt("sleep_n_m", snm))
    }

    fun save(ctx: Context, p: Prefs) {
        val s = ClassPlanStore.store(ctx)
        s.putString("alarm_sound", p.sound)
        s.putInt("alarm_volume", p.volume)
        s.putInt("alarm_repeat", p.repeat)
        s.putBool("alarm_crescendo", p.crescendo)
        // زمان‌های بیداری مستقل ذخیره نمی‌شوند؛ منبع حقیقت ClassPlanStore است.
        s.remove("wake_m_h"); s.remove("wake_m_m")
        s.putInt("bus_m_h", p.busMH); s.putInt("bus_m_m", p.busMM)
        s.remove("wake_n_h"); s.remove("wake_n_m")
        s.putInt("bus_n_h", p.busNH); s.putInt("bus_n_m", p.busNM)
        s.putInt("sleep_m_h", p.sleepMH); s.putInt("sleep_m_m", p.sleepMM)
        s.putInt("sleep_n_h", p.sleepNH); s.putInt("sleep_n_m", p.sleepNM)
        val snap = ClassPlanStore.load(ctx)
        ClassPlanStore.saveTimes(
            ctx,
            snap.morningHour, snap.morningMinute, snap.wakeLeadMin,
            snap.noonHour, snap.noonMinute,
            "%d:%02d".format(p.sleepMH, p.sleepMM),
            "%d:%02d".format(p.sleepNH, p.sleepNM))
    }

    fun sync(ctx: Context, reminders: ReminderScheduler, snap: ClassPlanStore.Snapshot, date: LocalDate) {
        ClassPlanStore.refreshOffCache(ctx)
        val p = load(ctx)
        val target = if (ClassPlanStore.isSchoolOff(ctx, snap, date)) {
            ClassPlanStore.firstSchoolDay(snap, date.plusDays(1), ctx)
        } else date
        val shift = ClassPlanStore.shiftOf(snap, target)
        val ch = NotificationChannels.SCHOOL_ALARM
        fun up(id: String, title: String, body: String, h: Int, m: Int) {
            reminders.upsert(Reminder(id, title, body, h, m, channel = ch, enabled = true))
        }
        // اعلان «حضور در مدرسه» حذف شده است؛ فقط آماده‌شدن، سرویس و دعوت خواب می‌مانند.
        reminders.remove(SCHOOL_M)
        reminders.remove(SCHOOL_N)
        if (shift == Shift.MORNING) {
            listOf(WAKE_N, BUS_N).forEach { reminders.remove(it) }
            up(WAKE_M, "آماده‌شدن برای شیفت صبح", "زمان آماده‌شدن برای مدرسه است.", p.wakeMH, p.wakeMM)
            up(BUS_M, "سرویس مدرسه", "زمان حضور در سرویس صبح است.", p.busMH, p.busMM)
            up(SLEEP, "دعوت به خواب", "وقت یک خواب آرام است؛ یکی از تمرین‌های پیش از خواب را انتخاب کن.", p.sleepMH, p.sleepMM)
        } else {
            listOf(WAKE_M, BUS_M).forEach { reminders.remove(it) }
            up(WAKE_N, "آماده‌شدن برای شیفت ظهر", "زمان آماده‌شدن برای مدرسه است.", p.wakeNH, p.wakeNM)
            up(BUS_N, "سرویس مدرسه", "زمان حضور در سرویس ظهر است.", p.busNH, p.busNM)
            up(SLEEP, "دعوت به خواب", "وقت یک خواب آرام است؛ یکی از تمرین‌های پیش از خواب را انتخاب کن.", p.sleepNH, p.sleepNM)
        }
    }
}
