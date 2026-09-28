package com.hamyareman.ir.ui.art

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * تست خالص JVM برای استریک نقاشی (بدون اندروید، بدون Appwrite).
 *
 * چرا مهم است؟ استریک وعده‌ی اصلی ماژول هنر است و نباید با یک روز فراموش‌شده
 * یا تاریخ تکراری اشتباه حساب شود.
 */
class ArtStreakTest {

    private val today = LocalDate.of(2026, 9, 7)

    private fun entry(day: LocalDate) =
        GalleryEntry(promptId = "p", title = "t", moodTag = "", dateIso = day.toString())

    @Test
    fun `empty gallery has zero streak`() {
        assertEquals(0, artStreak(emptyList(), today))
    }

    @Test
    fun `three consecutive days count as three`() {
        val entries = listOf(
            entry(today),
            entry(today.minusDays(1)),
            entry(today.minusDays(2)),
        )
        assertEquals(3, artStreak(entries, today))
    }

    @Test
    fun `yesterday still keeps the streak alive`() {
        // «امروز نه» استریک را نمی‌شکند: آخرین کار دیروز بوده.
        val entries = listOf(entry(today.minusDays(1)), entry(today.minusDays(2)))
        assertEquals(2, artStreak(entries, today))
    }

    @Test
    fun `a two-day gap breaks the streak`() {
        val entries = listOf(entry(today.minusDays(2)), entry(today.minusDays(3)))
        assertEquals(0, artStreak(entries, today))
    }

    @Test
    fun `duplicates on the same day are counted once`() {
        val entries = listOf(entry(today), entry(today), entry(today.minusDays(1)))
        assertEquals(2, artStreak(entries, today))
    }

    @Test
    fun `streak stops at the first hole`() {
        val entries = listOf(
            entry(today),
            entry(today.minusDays(1)),
            // جای خالی
            entry(today.minusDays(3)),
            entry(today.minusDays(4)),
        )
        assertEquals(2, artStreak(entries, today))
    }

    @Test
    fun `future dates do not inflate the streak`() {
        val entries = listOf(entry(today.plusDays(1)), entry(today))
        assertEquals(1, artStreak(entries, today))
    }
}
