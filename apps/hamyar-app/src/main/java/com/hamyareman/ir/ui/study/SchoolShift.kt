package com.hamyareman.ir.ui.study

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Shift(val label: String) { MORNING("شیفت صبح"), EVENING("شیفت ظهر") }

fun Shift.opposite(): Shift = if (this == Shift.MORNING) Shift.EVENING else Shift.MORNING

/** شیفتِ مشتقِ هفته‌ی [weekDelta] هفتهٔ فاصله از هفتهٔ لنگر (۰ = خودِ لنگر). */
fun derivedShift(base: Shift, weekDelta: Long): Shift =
    if (Math.floorMod(weekDelta, 2L) == 0L) base else base.opposite()

/** ریاضی تقویم مدرسه — شنبه=۱ … جمعه=۷؛ چرخه‌ی شیفت از لنگر. */
object SchoolShift {

    fun dayIndex(date: LocalDate): Int = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> 1
        DayOfWeek.SUNDAY -> 2
        DayOfWeek.MONDAY -> 3
        DayOfWeek.TUESDAY -> 4
        DayOfWeek.WEDNESDAY -> 5
        DayOfWeek.THURSDAY -> 6
        DayOfWeek.FRIDAY -> 7
    }

    fun weekIndex(anchorIso: String, date: LocalDate): Long {
        val anchor = runCatching { LocalDate.parse(anchorIso) }.getOrDefault(date)
        val days = ChronoUnit.DAYS.between(anchor, date)
        return Math.floorDiv(days, 7L)
    }

    fun shiftOn(anchorIso: String, date: LocalDate): Shift = shiftOn(anchorIso, date, 2)

    fun shiftOn(anchorIso: String, date: LocalDate, cycleWeeks: Int): Shift {
        val cycle = cycleWeeks.coerceIn(1, 8)
        if (cycle == 1) return Shift.MORNING
        val pos = Math.floorMod(weekIndex(anchorIso, date), cycle.toLong())
        val morningSlots = (cycle + 1) / 2
        return if (pos < morningSlots) Shift.MORNING else Shift.EVENING
    }

    fun cycleCaption(anchorIso: String, date: LocalDate, cycleWeeks: Int): String {
        val cycle = cycleWeeks.coerceIn(1, 8)
        val pos = Math.floorMod(weekIndex(anchorIso, date), cycle.toLong()).toInt()
        return when (cycle) {
            1 -> "هفته جاری"
            2 -> if (pos == 0) "هفته اول" else "هفته دوم"
            4 -> "ماه اول"
            else -> "هفتهٔ ${pos + 1}"
        }
    }

    fun startOfPersianWeek(date: LocalDate): LocalDate =
        date.minusDays((dayIndex(date) - 1).toLong())
}
