package com.hamyareman.ir.platform.feature.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2Test {

    @Test
    fun `اولین مرور خوب یک روز بعد`() {
        val next = Sm2.review(Sm2.CardState(), 4, "2026-09-11")
        assertEquals(1, next.reps)
        assertEquals(1, next.intervalDays)
        assertEquals("2026-09-12", next.dueKey)
    }

    @Test
    fun `دومین مرور خوب سه روز بعد و سپس فاصله تطبیقی`() {
        val s1 = Sm2.review(Sm2.CardState(), 4, "2026-09-11")
        val s2 = Sm2.review(s1, 4, "2026-09-12")
        assertEquals(2, s2.reps)
        assertEquals(3, s2.intervalDays)
        val s3 = Sm2.review(s2, 5, "2026-09-15")
        assertTrue(s3.intervalDays > 3)
        assertEquals(2.6, s3.ease, 1e-9)
    }

    @Test
    fun `بلد نبودن کارت را به فردا برمی‌گرداند و ease را کم می‌کند`() {
        val s1 = Sm2.review(Sm2.CardState(), 4, "2026-09-11")
        val s2 = Sm2.review(s1, 1, "2026-09-12")
        assertEquals(0, s2.reps)
        assertEquals(1, s2.intervalDays)
        assertEquals("2026-09-13", s2.dueKey)
        assertEquals(1, s2.lapses)
        assertTrue(s2.ease < s1.ease)
    }

    @Test
    fun `تسلط بین صفر و صد`() {
        val fresh = Sm2.CardState()
        assertEquals(0, Sm2.mastery(fresh))
        val s = Sm2.CardState(ease = 2.5, reps = 8, intervalDays = 60, dueKey = "2026-11-01")
        assertTrue(Sm2.mastery(s) in 90..100)
    }

    @Test
    fun `سررسید امروز`() {
        assertTrue(Sm2.isDue(Sm2.CardState(), "2026-09-11"))
        val s = Sm2.CardState(dueKey = "2026-09-30")
        assertFalse(Sm2.isDue(s, "2026-09-11"))
        assertTrue(Sm2.isDue(s, "2026-09-30"))
    }
}
