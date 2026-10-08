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
    @Test
    fun `oldest first keeps newest item on the physical left in rtl`() {
        data class Entry(val id: String, val createdAt: Long)
        val ordered = PersianPaging.oldestToNewest(
            listOf(Entry("new", 30), Entry("old", 10), Entry("mid", 20)),
        ) { it.createdAt }
        assertEquals(listOf("old", "mid", "new"), ordered.map { it.id })
    }

}
