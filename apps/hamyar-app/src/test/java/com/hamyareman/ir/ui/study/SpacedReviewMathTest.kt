package com.hamyareman.ir.ui.study

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ریاضی مرور فاصله‌دار باید برای نوجوان قابل توضیح باشد:
 * درست ⇒ ۲.۵ برابر (سقف ۳۰ روز)، غلط ⇒ فردا.
 */
class SpacedReviewMathTest {

    @Test
    fun `correct answer grows the interval by 2_5x`() {
        assertEquals(3, nextIntervalDays(1, answeredCorrectly = true))
        assertEquals(8, nextIntervalDays(3, answeredCorrectly = true))
        assertEquals(20, nextIntervalDays(8, answeredCorrectly = true))
    }

    @Test
    fun `interval never exceeds the 30-day cap`() {
        assertEquals(MAX_INTERVAL_DAYS, nextIntervalDays(20, answeredCorrectly = true))
        assertEquals(MAX_INTERVAL_DAYS, nextIntervalDays(MAX_INTERVAL_DAYS, answeredCorrectly = true))
    }

    @Test
    fun `wrong answer always resets to tomorrow`() {
        assertEquals(1, nextIntervalDays(20, answeredCorrectly = false))
        assertEquals(1, nextIntervalDays(1, answeredCorrectly = false))
    }

    @Test
    fun `a full ladder reaches the cap in five reviews`() {
        var interval = 1
        val ladder = buildList {
            repeat(5) {
                interval = nextIntervalDays(interval, answeredCorrectly = true)
                add(interval)
            }
        }
        assertEquals(listOf(3, 8, 20, 30, 30), ladder)
    }

    @Test
    fun `degenerate intervals are clamped to at least one day`() {
        assertEquals(3, nextIntervalDays(0, answeredCorrectly = true))
        assertEquals(3, nextIntervalDays(-4, answeredCorrectly = true))
    }
}
