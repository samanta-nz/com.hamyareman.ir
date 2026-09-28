package com.hamyareman.ir.platform.feature.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * پرامپت ۰۱ — تست‌های واحد برای منطق ساده‌ی Progress (محاسبات fraction، MediaType).
 *
 * این تست‌ها مستقل از Context اندروید اجرا می‌شوند و فقط ساختارهای داده‌ای
 * خالص (data class و enum) را بررسی می‌کنند.
 */
class ProgressEventMathTest {

    @Test
    fun `progress fraction computes ratio`() {
        val p = LessonMediaProgressRepository.Progress(
            bookCode = "C905", lessonId = "E01-L01",
            mediaType = LessonMediaProgressRepository.MediaType.VIDEO,
            lastPositionSec = 25.0, durationSec = 100.0,
        )
        assertEquals(0.25f, p.fraction, 0.001f)
    }

    @Test
    fun `progress fraction is zero when duration is zero`() {
        val p = LessonMediaProgressRepository.Progress("C905", "L", LessonMediaProgressRepository.MediaType.AUDIO)
        assertEquals(0f, p.fraction, 0.001f)
    }

    @Test
    fun `progress fraction is clamped to 0`() {
        val p = LessonMediaProgressRepository.Progress("C905", "L", LessonMediaProgressRepository.MediaType.VIDEO, -10.0, 100.0)
        assertEquals(0f, p.fraction, 0.001f)
    }

    @Test
    fun `progress fraction is clamped to 1`() {
        val p = LessonMediaProgressRepository.Progress("C905", "L", LessonMediaProgressRepository.MediaType.VIDEO, 150.0, 100.0)
        assertEquals(1f, p.fraction, 0.001f)
    }

    @Test
    fun `MediaType fromWire parses video`() {
        assertEquals(LessonMediaProgressRepository.MediaType.VIDEO, LessonMediaProgressRepository.MediaType.fromWire("video"))
    }

    @Test
    fun `MediaType fromWire parses audio`() {
        assertEquals(LessonMediaProgressRepository.MediaType.AUDIO, LessonMediaProgressRepository.MediaType.fromWire("audio"))
    }

    @Test
    fun `MediaType fromWire defaults to video on null or unknown`() {
        assertEquals(LessonMediaProgressRepository.MediaType.VIDEO, LessonMediaProgressRepository.MediaType.fromWire(null))
        assertEquals(LessonMediaProgressRepository.MediaType.VIDEO, LessonMediaProgressRepository.MediaType.fromWire(""))
        assertEquals(LessonMediaProgressRepository.MediaType.VIDEO, LessonMediaProgressRepository.MediaType.fromWire("xyz"))
    }

    @Test
    fun `MediaType wire strings are stable contract`() {
        assertEquals("video", LessonMediaProgressRepository.MediaType.VIDEO.wire)
        assertEquals("audio", LessonMediaProgressRepository.MediaType.AUDIO.wire)
    }

    @Test
    fun `PlayerState default values are safe`() {
        val s = LessonMediaPlayer.PlayerState()
        assertFalse(s.connected)
        assertFalse(s.playing)
        assertEquals(0L, s.positionMs)
        assertEquals(0L, s.durationMs)
        assertEquals(1f, s.speed, 0.001f)
        assertEquals(null, s.error)
        assertEquals(null, s.mediaType)
    }

    @Test
    fun `PlayerState ALLOWED_SPEEDS is from 0_75 to 2_0`() {
        val allowed = LessonMediaPlayer.ALLOWED_SPEEDS
        assertTrue(allowed.size == 5)
        assertEquals(0.75f, allowed[0], 0.001f)
        assertEquals(1f, allowed[1], 0.001f)
        assertEquals(1.25f, allowed[2], 0.001f)
        assertEquals(1.5f, allowed[3], 0.001f)
        assertEquals(2f, allowed[4], 0.001f)
    }

    @Test
    fun `seek jump threshold is 3 seconds per spec`() {
        assertEquals(3.0, LessonMediaProgressRepository.SEEK_JUMP_THRESHOLD_SEC, 0.0)
    }

    @Test
    fun `progress tick is 5 seconds per spec`() {
        assertEquals(5_000L, LessonMediaPlayer.PROGRESS_TICK_MS)
    }

    @Test
    fun `completion fraction is 90 percent per spec`() {
        assertEquals(0.9, LessonMediaPlayer.COMPLETION_FRACTION, 0.0)
    }

    @Test
    fun `seek increment is 10 seconds per spec`() {
        assertEquals(10_000L, LessonMediaPlayer.SEEK_INCREMENT_MS)
    }

    @Test
    fun `min and max playback speeds are clamped per spec`() {
        assertEquals(0.75f, LessonMediaPlayer.MIN_SPEED, 0.001f)
        assertEquals(2.0f, LessonMediaPlayer.MAX_SPEED, 0.001f)
    }

    @Test
    fun `ViewEvent copy preserves fields`() {
        val v = LessonMediaProgressRepository.ViewEvent(
            dateIso = "2024-01-01T00:00:00Z",
            speed = 1.25,
            fromSec = 10.0,
            toSec = 50.0,
            isReplay = true,
        )
        val c = v.copy()
        assertEquals(v.dateIso, c.dateIso)
        assertEquals(v.speed, c.speed, 0.0)
        assertEquals(v.fromSec, c.fromSec, 0.0)
        assertEquals(v.toSec, c.toSec, 0.0)
        assertEquals(v.isReplay, c.isReplay)
    }

    @Test
    fun `SeekEvent copy preserves fields`() {
        val s = LessonMediaProgressRepository.SeekEvent(
            atClientTimeIso = "2024-01-01T00:00:00Z",
            fromSec = 1.0, toSec = 99.0, direction = "backward",
        )
        val c = s.copy()
        assertEquals(s.atClientTimeIso, c.atClientTimeIso)
        assertEquals(s.fromSec, c.fromSec, 0.0)
        assertEquals(s.toSec, c.toSec, 0.0)
        assertEquals(s.direction, c.direction)
    }
}
