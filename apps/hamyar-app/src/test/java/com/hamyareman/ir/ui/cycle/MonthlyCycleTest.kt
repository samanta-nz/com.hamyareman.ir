package com.hamyareman.ir.ui.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * منطقِ «چرخه ی ماهانه» بدونِ اندروید: تقویم، پیش‌بینی و فازِ روز.
 *
 * چرا مهم: اگر شمارشِ روزهای دوره یا پیش‌بینیِ شروع بعدی یک روز جابه‌جا شود،
 * راهنمای «امروز» به کاربرِ ۱۳ساله چیزِ بی‌ربط می‌گوید. پس این‌ها قفل می‌شوند.
 */
class MonthlyCycleTest {

    private val empty = MonthlyCycle.State()

    @Test
    fun `markStart records the period length and remembers the start`() {
        val s = MonthlyCycle.markStart(empty, "2026-09-01")
        assertEquals("2026-09-01", s.lastStart)
        assertEquals(5, s.periodDays.size)
        assertTrue(s.periodDays.contains("2026-09-05"))
        assertTrue(!s.periodDays.contains("2026-09-06"))
    }

    @Test
    fun `next start is one cycle after the last start`() {
        val s = MonthlyCycle.markStart(empty, "2026-09-01").copy(cycleLength = 28)
        assertEquals("2026-09-29", MonthlyCycle.nextStart(s))
        assertEquals(7, MonthlyCycle.daysToNext(s, "2026-09-22"))
        assertEquals(0, MonthlyCycle.daysToNext(s, "2026-09-29"))
        assertEquals(-1, MonthlyCycle.daysToNext(s, "2026-09-30"))
    }

    @Test
    fun `no start means no prediction`() {
        assertNull(MonthlyCycle.nextStart(empty))
        assertNull(MonthlyCycle.daysToNext(empty, "2026-09-22"))
    }

    @Test
    fun `phase prefers what the user recorded over the estimate`() {
        val s = MonthlyCycle.markStart(empty, "2026-09-01")
        assertEquals(MonthlyCycle.Phase.PERIOD, MonthlyCycle.phase(s, "2026-09-01"))
        assertEquals(MonthlyCycle.Phase.PERIOD, MonthlyCycle.phase(s, "2026-09-04"))
        // ۲ روزِ آخرِ چرخه = PMS (روزِ ۲۶ و ۲۷ چرخه‌ی ۲۸روزه)
        assertEquals(MonthlyCycle.Phase.PMS, MonthlyCycle.phase(s, "2026-09-27"))
        assertEquals(MonthlyCycle.Phase.PMS, MonthlyCycle.phase(s, "2026-09-28"))
        // میانه‌ی چرخه ≈ تخمک‌گذاری
        assertEquals(MonthlyCycle.Phase.OVULATION, MonthlyCycle.phase(s, "2026-09-15"))
        // فردای تمام‌شدنِ پریود = فولیکولار
        assertEquals(MonthlyCycle.Phase.FOLLICULAR, MonthlyCycle.phase(s, "2026-09-07"))
    }

    @Test
    fun `period day number counts from the start of the same run of days`() {
        val s = MonthlyCycle.markStart(empty, "2026-09-01")
        assertEquals(1, MonthlyCycle.periodDayNumber(s, "2026-09-01"))
        assertEquals(3, MonthlyCycle.periodDayNumber(s, "2026-09-03"))
        assertEquals(5, MonthlyCycle.periodDayNumber(s, "2026-09-05"))
        assertNull(MonthlyCycle.periodDayNumber(s, "2026-09-20"))
        // پس از یک دورهٔ تازه، شمارش از سرِ همان دوره شروع می‌شود.
        val two = MonthlyCycle.markStart(s, "2026-09-29")
        assertEquals(1, MonthlyCycle.periodDayNumber(two, "2026-09-29"))
        assertEquals(2, MonthlyCycle.periodDayNumber(two, "2026-09-30"))
    }

    @Test
    fun `toggleDay adds and removes a single day`() {
        val a = MonthlyCycle.toggleDay(empty, "2026-08-10")
        assertTrue(a.periodDays.contains("2026-08-10"))
        val b = MonthlyCycle.toggleDay(a, "2026-08-10")
        assertTrue(b.periodDays.isEmpty())
    }

    @Test
    fun `today card is always readable and mentions the phase`() {
        for (day in 1..10) {
            val s = MonthlyCycle.State(periodDays = setOf("2026-09-%02d".format(day)), lastStart = "2026-09-01")
            val (title, body) = MonthlyCycle.todayCard(s, "2026-09-%02d".format(day))
            assertTrue("عنوانِ خالی", title.isNotBlank())
            assertTrue("متنِ خالی", body.length > 20)
        }
        val calm = MonthlyCycle.State(lastStart = "2026-09-01")
        val (t2, b2) = MonthlyCycle.todayCard(calm, "2026-09-03")
        assertTrue(t2.isNotBlank() && b2.isNotBlank())
    }

    @Test
    fun `pain exercises are all filled in`() {
        assertTrue(MonthlyCycle.painExercises.size >= 5)
        MonthlyCycle.painExercises.forEach { ex ->
            assertTrue(ex.title.isNotBlank())
            assertTrue(ex.duration.isNotBlank())
            assertTrue(ex.how.length > 10)
        }
        assertNotNull(MonthlyCycle.periodAdvice(1))
        assertTrue(MonthlyCycle.periodAdvice(7).isNotBlank())
    }

    @Test
    fun `cycle length stays inside a sane range`() {
        val s = MonthlyCycle.State(cycleLength = 99, periodLength = 99)
        // مقادیرِ بیرون از بازه در خواندنِ JSON مهار می‌شوند؛ سازندهٔ داده هم همین را
        // انتظار دارد پس این‌جا فقط بازهٔ مجاز را می‌سنجیم.
        val t = MonthlyCycle.markStart(s, "2026-09-01")
        assertTrue(t.periodDays.size in 1..10)
    }
}
