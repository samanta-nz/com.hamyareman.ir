package com.hamyareman.ir.ui.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Freshness is intentionally decided only by exact byte length, without reading payloads. */
class LessonCacheSizeFreshnessTest {

    @Test
    fun equalByteLengthsMeanSameVersionForThisPolicy() {
        assertEquals(true, LessonCache.sameSize(256L * 1024L, 256L * 1024L))
    }

    @Test
    fun differentByteLengthsMeanVersionChanged() {
        assertEquals(false, LessonCache.sameSize(256L * 1024L, 256L * 1024L + 1L))
        assertEquals(false, LessonCache.sameSize(99L, 100L))
    }

    @Test
    fun comparisonUsesLongPrecisionForLargeFiles() {
        val largerThanFourGiB = 5L * 1024L * 1024L * 1024L
        assertEquals(false, LessonCache.sameSize(largerThanFourGiB, largerThanFourGiB + 1L))
    }

    @Test
    fun unknownSizeDoesNotForceAFullDownload() {
        assertNull(LessonCache.sameSize(4096L, -1L))
        assertNull(LessonCache.sameSize(-1L, 4096L))
    }
}
