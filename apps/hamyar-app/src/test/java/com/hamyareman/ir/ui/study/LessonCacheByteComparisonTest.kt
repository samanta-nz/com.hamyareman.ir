package com.hamyareman.ir.ui.study

import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonCacheByteComparisonTest {

    private fun withPayloadFiles(
        firstBytes: ByteArray,
        secondBytes: ByteArray,
        assertion: (File, File) -> Unit,
    ) {
        val directory = File(
            System.getProperty("java.io.tmpdir"),
            "hamyar-html-compare-" + UUID.randomUUID(),
        )
        assertTrue(directory.mkdirs())
        try {
            val first = File(directory, "cached.bin").apply { writeBytes(firstBytes) }
            val second = File(directory, "candidate.bin").apply { writeBytes(secondBytes) }
            assertion(first, second)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun detectsChangedMiddleByteWhenLengthsAndEdgesMatch() {
        val original = ByteArray(256 * 1024) { (it % 251).toByte() }
        val changed = original.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }

        assertEquals(original.size.toLong(), changed.size.toLong())
        assertEquals(original.take(8 * 1024), changed.take(8 * 1024))
        assertEquals(original.takeLast(8 * 1024), changed.takeLast(8 * 1024))

        withPayloadFiles(original, changed) { cache, candidate ->
            assertFalse(LessonCache.sameBytes(cache, candidate))
        }
    }

    @Test
    fun acceptsIdenticalPayloads() {
        val bytes = ByteArray(80 * 1024) { ((it * 13) % 255).toByte() }
        withPayloadFiles(bytes, bytes.copyOf()) { cache, candidate ->
            assertTrue(LessonCache.sameBytes(cache, candidate))
        }
    }

    @Test
    fun detectsLengthDifferenceAtBytePrecision() {
        val first = byteArrayOf(1, 2, 3, 4)
        val second = byteArrayOf(1, 2, 3, 4, 5)
        withPayloadFiles(first, second) { cache, candidate ->
            assertFalse(LessonCache.sameBytes(cache, candidate))
        }
    }
}
