package com.hamyareman.ir.ui.study

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * منطقِ شیفت: آلفا = شیفتِ بلوکِ لنگر؛ بتا = هفته داخل بلوک.
 * بلوکِ ۲ یا ۴ هفته همان آلفا می‌ماند و بعد به شیفت مخالف می‌رود.
 */
class ClassPlanShiftTest {

    private val saturday = LocalDate.of(2026, 9, 5) // شنبه

    private fun snap(
        cycleWeeks: Int,
        anchor: LocalDate = saturday,
        base: Shift = Shift.MORNING,
        offset: Int = 1,
        weekPattern: List<String> = emptyList(),
    ) = ClassPlanStore.Snapshot(
        locked = false,
        days = (1..5).associateWith { emptyList<String>() },
        cycleWeeks = cycleWeeks,
        anchorIso = anchor.toString(),
        fixedEvening = base == Shift.EVENING,
        lunarOffset = 0,
        morningHour = 7,
        morningMinute = 30,
        wakeLeadMin = 60,
        noonHour = 12,
        noonMinute = 0,
        sleepMorning = "21:30",
        sleepEvening = "23:00",
        weekPattern = weekPattern,
        virtualMorningHour = 8,
        virtualMorningMinute = 0,
        virtualNoonHour = 14,
        virtualNoonMinute = 0,
        thisWeekShift = base,
        cycleWeekOffset = offset,
    )

    @Test
    fun `derived shift alternates every week from the anchor base`() {
        assertEquals(Shift.MORNING, derivedShift(Shift.MORNING, 0))
        assertEquals(Shift.EVENING, derivedShift(Shift.MORNING, 1))
        assertEquals(Shift.MORNING, derivedShift(Shift.MORNING, 2))
        assertEquals(Shift.EVENING, derivedShift(Shift.MORNING, 3))
        assertEquals(Shift.EVENING, derivedShift(Shift.MORNING, -1))
        assertEquals(Shift.EVENING, derivedShift(Shift.EVENING, 0))
        assertEquals(Shift.MORNING, derivedShift(Shift.EVENING, 1))
        assertEquals(Shift.EVENING, derivedShift(Shift.EVENING, 2))
    }

    @Test
    fun `two week cycle keeps alpha then flips at next block`() {
        val s = snap(2, base = Shift.MORNING)
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday.plusDays(7)))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.plusDays(14)))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.minusDays(7)))

        val sEven = snap(2, base = Shift.EVENING)
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(sEven, saturday))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(sEven, saturday.plusDays(7)))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(sEven, saturday.plusDays(14)))
    }

    @Test
    fun `fixed cycle always uses this week shift`() {
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(snap(1, base = Shift.MORNING), saturday.plusDays(21)))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(snap(1, base = Shift.EVENING), saturday.plusDays(21)))
    }

    @Test
    fun `cycle week position follows the selected offset`() {
        val s1 = snap(2, offset = 1)
        assertEquals(1, ClassPlanStore.cycleWeekPos(s1, saturday))
        assertEquals(2, ClassPlanStore.cycleWeekPos(s1, saturday.plusDays(7)))
        assertEquals(1, ClassPlanStore.cycleWeekPos(s1, saturday.plusDays(14)))

        val s2 = snap(4, offset = 3, base = Shift.EVENING)
        assertEquals(3, ClassPlanStore.cycleWeekPos(s2, saturday))
        assertEquals(4, ClassPlanStore.cycleWeekPos(s2, saturday.plusDays(7)))
        assertEquals(1, ClassPlanStore.cycleWeekPos(s2, saturday.plusDays(14)))
        assertEquals(2, ClassPlanStore.cycleWeekPos(s2, saturday.plusDays(21)))
    }

    @Test
    fun `four week cycle keeps alpha for the block then flips`() {
        val s = snap(4, offset = 3, base = Shift.EVENING)
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.plusDays(7)))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday.plusDays(14)))
        assertEquals(Shift.MORNING, ClassPlanStore.shiftOf(s, saturday.plusDays(21)))
    }

    @Test
    fun `legacy manual pattern still wins until re-saved`() {
        val s = snap(2, base = Shift.MORNING, weekPattern = listOf("evening", "evening"))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday))
        assertEquals(Shift.EVENING, ClassPlanStore.shiftOf(s, saturday.plusDays(7)))
    }

    @Test
    fun `dashboard shows today until exit then next school day`() {
        val wed = LocalDate.of(2026, 9, 23) // چهارشنبه ۱ مهر ۱۴۰۵
        val sat = LocalDate.of(2026, 9, 26)
        val s = snap(1, base = Shift.EVENING).copy(exitNoon = "17:30", days = (1..5).associateWith { listOf("ریاضی") })
        val before = java.time.LocalDateTime.of(2026, 9, 23, 15, 0)
        val after = java.time.LocalDateTime.of(2026, 9, 23, 18, 0)
        assertEquals(wed, ClassPlanStore.dashboardShowDate(s, before))
        assertEquals(sat, ClassPlanStore.dashboardShowDate(s, after))
    }

    @Test
    fun `captions use the stored wording`() {
        assertEquals("همیشه شیفت صبح", ClassPlanStore.captionOf(snap(1, base = Shift.MORNING), saturday))
        assertEquals("همیشه شیفت ظهر", ClassPlanStore.captionOf(snap(1, base = Shift.EVENING), saturday))
        assertEquals(
            "این هفته اولین هفته از شیفت صبح است",
            ClassPlanStore.captionOf(snap(2, offset = 1), saturday),
        )
        assertEquals(
            "این هفته سومین هفته از شیفت ظهر است",
            ClassPlanStore.captionOf(snap(4, offset = 3, base = Shift.EVENING), saturday),
        )
        assertEquals(
            "این هفته چهارمین هفته از شیفت ظهر است",
            ClassPlanStore.captionOf(snap(4, offset = 3, base = Shift.EVENING), saturday.plusDays(7)),
        )
        assertEquals(
            "این هفته دومین هفته از شیفت ظهر است",
            ClassPlanStore.captionOf(snap(4, offset = 2, base = Shift.EVENING), saturday),
        )
    }
}
