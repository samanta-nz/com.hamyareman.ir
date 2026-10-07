package com.hamyareman.ir.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.unit.LayoutDirection

class PersianPagingTest {

    @Test
    fun `rtl pager is not double reversed`() {
        assertFalse(PersianPaging.pagerReverseLayout(LayoutDirection.Rtl))
        assertTrue(PersianPaging.pagerReverseLayout(LayoutDirection.Ltr))
    }

    @Test
    fun `swipe next moves toward newer page index`() {
        assertEquals(2, PersianPaging.indexAfterSwipe(current = 1, count = 4, forward = true))
        assertEquals(3, PersianPaging.indexAfterSwipe(current = 3, count = 4, forward = true))
    }

    @Test
    fun `swipe previous moves toward older page index`() {
        assertEquals(1, PersianPaging.indexAfterSwipe(current = 2, count = 4, forward = false))
        assertEquals(0, PersianPaging.indexAfterSwipe(current = 0, count = 4, forward = false))
    }
}
