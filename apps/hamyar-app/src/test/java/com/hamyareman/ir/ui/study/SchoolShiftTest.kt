package com.hamyareman.ir.ui.study

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * ریاضی تقویم مدرسه باید دقیق باشد: هفته‌ی ایرانی از شنبه شروع می‌شود و
 * چرخه‌ی شیفت دوهفته‌ای است. این تست‌ها روی JVM خالص اجرا می‌شوند.
 */
class SchoolShiftTest {

    private val saturday = LocalDate.of(2026, 9, 5)   // شنبه
    private val friday = LocalDate.of(2026, 9, 11)     // جمعه

    @Test
    fun `persian week starts on saturday and ends on friday`() {
        assertEquals(1, SchoolShift.dayIndex(saturday))
        assertEquals(2, SchoolShift.dayIndex(saturday.plusDays(1)))
        assertEquals(7, SchoolShift.dayIndex(friday))
    }

    @Test
    fun `start of persian week is the nearest previous saturday`() {
        assertEquals(saturday, SchoolShift.startOfPersianWeek(saturday))
        assertEquals(saturday, SchoolShift.startOfPersianWeek(friday))
        assertEquals(saturday, SchoolShift.startOfPersianWeek(saturday.plusDays(3)))
    }

    @Test
    fun `anchor week is morning and the next week is evening`() {
        assertEquals(Shift.MORNING, SchoolShift.shiftOn(saturday.toString(), saturday))
        assertEquals(Shift.MORNING, SchoolShift.shiftOn(saturday.toString(), friday))
        assertEquals(Shift.EVENING, SchoolShift.shiftOn(saturday.toString(), saturday.plusDays(7)))
        assertEquals(Shift.MORNING, SchoolShift.shiftOn(saturday.toString(), saturday.plusDays(14)))
    }

    @Test
    fun `dates before the anchor still alternate correctly`() {
        val anchor = saturday.toString()
        assertEquals(Shift.EVENING, SchoolShift.shiftOn(anchor, saturday.minusDays(7)))
        assertEquals(Shift.MORNING, SchoolShift.shiftOn(anchor, saturday.minusDays(14)))
    }

    @Test
    fun `a broken anchor falls back to today instead of crashing`() {
        assertEquals(Shift.MORNING, SchoolShift.shiftOn("not-a-date", friday))
    }

    @Test
    fun `four week cycle is two morning then two afternoon`() {
        val a = saturday.toString()
        assertEquals(Shift.MORNING, SchoolShift.shiftOn(a, saturday, 4))
        assertEquals(Shift.MORNING, SchoolShift.shiftOn(a, saturday.plusDays(7), 4))
        assertEquals(Shift.EVENING, SchoolShift.shiftOn(a, saturday.plusDays(14), 4))
        assertEquals(Shift.EVENING, SchoolShift.shiftOn(a, saturday.plusDays(21), 4))
        assertEquals("ماه اول", SchoolShift.cycleCaption(a, saturday, 4))
    }
}
