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
        val schoolMH: Int = 7, val schoolMM: Int = 30,
        val wakeNH: Int = 11, val wakeNM: Int = 30,
        val busNH: Int = 11, val busNM: Int = 45,
        val schoolNH: Int = 12, val schoolNM: Int = 0,
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
            wakeMH = s.getInt("wake_m_h", wh), wakeMM = s.getInt("wake_m_m", wm),
            busMH = s.getInt("bus_m_h", 6), busMM = s.getInt("bus_m_m", 45),
            schoolMH = s.getInt("school_m_h", snap.morningHour), schoolMM = s.getInt("school_m_m", snap.morningMinute),
            wakeNH = s.getInt("wake_n_h", nh), wakeNM = s.getInt("wake_n_m", nm),
            busNH = s.getInt("bus_n_h", 11), busNM = s.getInt("bus_n_m", 45),
            schoolNH = s.getInt("school_n_h", snap.noonHour), schoolNM = s.getInt("school_n_m", snap.noonMinute),
            sleepMH = s.getInt("sleep_m_h", smh), sleepMM = s.getInt("sleep_m_m", smm),
            sleepNH = s.getInt("sleep_n_h", snh), sleepNM = s.getInt("sleep_n_m", snm))
    }

    fun save(ctx: Context, p: Prefs) {
        val s = ClassPlanStore.store(ctx)
        s.putString("alarm_sound", p.sound)
        s.putInt("alarm_volume", p.volume)
        s.putInt("alarm_repeat", p.repeat)
        s.putBool("alarm_crescendo", p.crescendo)
        s.putInt("wake_m_h", p.wakeMH); s.putInt("wake_m_m", p.wakeMM)
        s.putInt("bus_m_h", p.busMH); s.putInt("bus_m_m", p.busMM)
        s.putInt("school_m_h", p.schoolMH); s.putInt("school_m_m", p.schoolMM)
        s.putInt("wake_n_h", p.wakeNH); s.putInt("wake_n_m", p.wakeNM)
        s.putInt("bus_n_h", p.busNH); s.putInt("bus_n_m", p.busNM)
        s.putInt("school_n_h", p.schoolNH); s.putInt("school_n_m", p.schoolNM)
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
        if (shift == Shift.MORNING) {
            listOf(WAKE_N, BUS_N, SCHOOL_N).forEach { reminders.remove(it) }
            up(WAKE_M, "بیدار شو — شیفت صبح", "شیفت صبح است؛ برای مدرسه آماده شو.", p.wakeMH, p.wakeMM)
            up(BUS_M, "حضور در سرویس مدرسه", "وقت سرویس صبح است.", p.busMH, p.busMM)
            up(SCHOOL_M, "حضور در مدرسه", "زنگ ورود صبح.", p.schoolMH, p.schoolMM)
            up(SLEEP, "دعوت به خواب آرام", "وقت خواب شیفت صبح است. برای آرامش قبل خواب لمس کن.", p.sleepMH, p.sleepMM)
        } else {
            listOf(WAKE_M, BUS_M, SCHOOL_M).forEach { reminders.remove(it) }
            up(WAKE_N, "آلارم آماده شدن شیفت ظهر", "حوالی ظهر است؛ برای شیفت ظهر آماده شو.", p.wakeNH, p.wakeNM)
            up(BUS_N, "حضور در سرویس مدرسه", "وقت سرویس ظهر است.", p.busNH, p.busNM)
            up(SCHOOL_N, "حضور در مدرسه", "زنگ ورود ظهر.", p.schoolNH, p.schoolNM)
            up(SLEEP, "دعوت به خواب آرام", "وقت خواب شیفت ظهر است. برای آرامش قبل خواب لمس کن.", p.sleepNH, p.sleepNM)
        }
    }
}
