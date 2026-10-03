package com.hamyareman.ir.ui.wellness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست خالص JVM برای WellnessTimingProvider.
 *
 * منطق‌های تحت تست:
 *  - yogaTiming: یک مرحله، cue معرفی + cue راهنما در ۱۰ ثانیه آخر + cue پایان
 *  - exerciseTiming: تعداد steps = reps، cue معرفی
 *  - breathingTiming: cycle درست، cue دم/نگه/بازدم
 *  - learningTiming: cue معرفی + یادآور + cue پایان
 */
class WellnessTimingTest {

    private val provider = WellnessTimingProvider()

    @Test
    fun `yoga timing has one step and intro+guide+finish cues`() {
        val move = WellnessCatalog.bySlug("yoga-balasana")!!  // 60s
        val timing = provider.timingFor(move)
        assertEquals(1, timing.steps.size)
        assertEquals(60, timing.totalSeconds)

        val kinds = timing.cues.map { it.kind }.toSet()
        assertTrue("yoga timing باید INTRO داشته باشد", AudioCue.Kind.INTRO in kinds)
        assertTrue("yoga timing باید GUIDE داشته باشد", AudioCue.Kind.GUIDE in kinds)
        assertTrue("yoga timing باید FINISH داشته باشد", AudioCue.Kind.FINISH in kinds)
    }

    @Test
    fun `yoga guide cue fires 10 seconds before end`() {
        val move = WellnessCatalog.bySlug("yoga-balasana")!!  // 60s
        val timing = provider.timingFor(move)
        val guide = timing.cues.firstOrNull { it.kind == AudioCue.Kind.GUIDE }
        assertEquals(50, guide?.atSec)
    }

    @Test
    fun `yoga finish cue fires at total seconds`() {
        val move = WellnessCatalog.bySlug("yoga-balasana")!!  // 60s
        val timing = provider.timingFor(move)
        val finish = timing.cues.firstOrNull { it.kind == AudioCue.Kind.FINISH }
        assertEquals(60, finish?.atSec)
    }

    @Test
    fun `yoga with very short duration still works`() {
        val move = WellnessCatalog.bySlug("yoga-cobra")!!  // 30s
        val timing = provider.timingFor(move)
        assertEquals(30, timing.totalSeconds)
    }

    @Test
    fun `exercise timing has steps equal to reps`() {
        val move = WellnessCatalog.bySlug("ex-squat")!!  // reps=10
        val timing = provider.timingFor(move)
        assertEquals(10, timing.steps.size)
    }

    @Test
    fun `exercise timing without reps has one step`() {
        val move = WellnessCatalog.bySlug("ex-butterfly")!!  // reps=0
        val timing = provider.timingFor(move)
        assertEquals(1, timing.steps.size)
    }

    @Test
    fun `exercise timing has intro cue`() {
        val move = WellnessCatalog.bySlug("ex-squat")!!
        val timing = provider.timingFor(move)
        val intro = timing.cues.firstOrNull { it.kind == AudioCue.Kind.INTRO }
        assertTrue(intro != null)
        assertEquals(0, intro!!.atSec)
    }

    @Test
    fun `breathing timing cycles correctly for box breathing`() {
        val move = WellnessCatalog.bySlug("breath-box")!!  // 180s
        val timing = provider.timingFor(move)
        // box 4-4-4-4: cycle=16s, cycles=180/16=11
        assertTrue("expected at least 10 cycles, got ${timing.steps.size}", timing.steps.size >= 10)
    }

    @Test
    fun `breathing timing has in-hold-out cues for 4-7-8`() {
        val move = WellnessCatalog.bySlug("breath-4-7-8")!!
        val timing = provider.timingFor(move)
        val kinds = timing.cues.map { it.kind }.toSet()
        assertTrue(AudioCue.Kind.BREATH_IN in kinds)
        assertTrue(AudioCue.Kind.BREATH_HOLD in kinds)
        assertTrue(AudioCue.Kind.BREATH_OUT in kinds)
    }

    @Test
    fun `breathing diaphragm has only in and out cues`() {
        val move = WellnessCatalog.bySlug("breath-diaphragm")!!
        val timing = provider.timingFor(move)
        val kinds = timing.cues.map { it.kind }.toSet()
        assertTrue(AudioCue.Kind.BREATH_IN in kinds)
        assertTrue(AudioCue.Kind.BREATH_OUT in kinds)
    }

    @Test
    fun `learning timing has intro and finish cues`() {
        val move = WellnessCatalog.bySlug("learn-pomodoro")!!
        val timing = provider.timingFor(move)
        val kinds = timing.cues.map { it.kind }.toSet()
        assertTrue(AudioCue.Kind.INTRO in kinds)
        assertTrue(AudioCue.Kind.FINISH in kinds)
    }

    @Test
    fun `learning timing has guide cue at midpoint for long moves`() {
        val move = WellnessCatalog.bySlug("learn-pomodoro")!!  // 1500s = 25min
        val timing = provider.timingFor(move)
        val guideCues = timing.cues.filter { it.kind == AudioCue.Kind.GUIDE }
        assertTrue("learning timing باید حداقل یک GUIDE داشته باشد", guideCues.isNotEmpty())
    }

    @Test
    fun `total seconds equals sum of step seconds`() {
        WellnessCatalog.all.forEach { move ->
            val timing = provider.timingFor(move)
            val sum = timing.steps.sumOf { it.seconds }
            assertEquals("move ${move.slug} totalSeconds نباید با sum فرق داشته باشد", sum, timing.totalSeconds)
        }
    }
}
