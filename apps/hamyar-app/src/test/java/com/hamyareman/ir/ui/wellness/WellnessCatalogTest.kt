package com.hamyareman.ir.ui.wellness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست خالص JVM برای کاتالوگ سلامتی.
 *
 * معیارهای پذیرش پرامپت ۰۲:
 *  - ۱۵ یوگا، ۱۵ ورزش، ۸ تنفس، ۵ یادگیری = ۴۳ حرکت
 *  - هر حرکت slug یکتا دارد
 *  - audioCueId برای هر حرکت غیرخالی است
 *  - durationSec > 0
 *  - یوگا audioCueId فرمت ۳-فایلی (start|mid|end) دارد
 *  - سایر دسته‌ها audioCueId تک فایل دارند
 */
class WellnessCatalogTest {

    @Test
    fun `catalog has exactly 43 moves`() {
        assertEquals(43, WellnessCatalog.all.size)
    }

    @Test
    fun `catalog has 15 yoga moves`() {
        assertEquals(15, WellnessCatalog.yoga.size)
    }

    @Test
    fun `catalog has 15 exercise moves`() {
        assertEquals(15, WellnessCatalog.exercise.size)
    }

    @Test
    fun `catalog has 8 breathing moves`() {
        assertEquals(8, WellnessCatalog.breathing.size)
    }

    @Test
    fun `catalog has 5 learning moves`() {
        assertEquals(5, WellnessCatalog.learning.size)
    }

    @Test
    fun `all slugs are unique`() {
        val slugs = WellnessCatalog.all.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size)
    }

    @Test
    fun `every move has non-blank audioCueId`() {
        val empty = WellnessCatalog.all.filter { it.audioCueId.isBlank() }
        assertTrue("moves with blank audioCueId: $empty", empty.isEmpty())
    }

    @Test
    fun `every move has positive durationSec`() {
        val invalid = WellnessCatalog.all.filter { it.durationSec <= 0 }
        assertTrue("moves with non-positive durationSec: $invalid", invalid.isEmpty())
    }

    @Test
    fun `every move has level between 1 and 10`() {
        val invalid = WellnessCatalog.all.filter { it.level !in 1..10 }
        assertTrue("moves with invalid level: $invalid", invalid.isEmpty())
    }

    @Test
    fun `yoga moves use 3-file audioCueId format`() {
        // هر یوگا باید audioCueId با | جدا شده داشته باشد و ۳ فایل داشته باشد
        WellnessCatalog.yoga.forEach { move ->
            val ids = move.audioCueId.split("|").map { it.trim() }
            assertEquals(
                "yoga ${move.slug} باید ۳ فایل صوتی داشته باشد، ولی ${ids.size} تا دارد",
                3, ids.size,
            )
            assertTrue("فایل صوتی یوگا ${move.slug} باید شامل -start باشد", ids[0].endsWith("-start.mp3"))
            assertTrue("فایل صوتی یوگا ${move.slug} باید شامل -mid باشد", ids[1].endsWith("-mid.mp3"))
            assertTrue("فایل صوتی یوگا ${move.slug} باید شامل -end باشد", ids[2].endsWith("-end.mp3"))
        }
    }

    @Test
    fun `exercise moves use single-file audioCueId`() {
        WellnessCatalog.exercise.forEach { move ->
            assertTrue(
                "exercise ${move.slug} نباید | داشته باشد، ولی audioCueId=${move.audioCueId}",
                !move.audioCueId.contains("|"),
            )
            assertTrue(
                "exercise ${move.slug} باید .mp3 داشته باشد",
                move.audioCueId.endsWith(".mp3"),
            )
        }
    }

    @Test
    fun `breathing moves use single-file audioCueId`() {
        WellnessCatalog.breathing.forEach { move ->
            assertTrue(
                "breathing ${move.slug} نباید | داشته باشد",
                !move.audioCueId.contains("|"),
            )
        }
    }

    @Test
    fun `learning moves use single-file audioCueId`() {
        WellnessCatalog.learning.forEach { move ->
            assertTrue(
                "learning ${move.slug} نباید | داشته باشد",
                !move.audioCueId.contains("|"),
            )
        }
    }

    @Test
    fun `bySlug returns correct move`() {
        val balasana = WellnessCatalog.bySlug("yoga-balasana")
        assertNotNull(balasana)
        assertEquals("کودک (Balasana)", balasana!!.titleFa)
        assertEquals(WellnessMove.Category.YOGA, balasana.category)
    }

    @Test
    fun `bySlug returns null for unknown slug`() {
        assertNull(WellnessCatalog.bySlug("nonexistent"))
    }

    @Test
    fun `byCategory returns correct moves`() {
        val yoga = WellnessCatalog.byCategory(WellnessMove.Category.YOGA)
        assertEquals(15, yoga.size)
        val breathing = WellnessCatalog.byCategory(WellnessMove.Category.BREATHING)
        assertEquals(8, breathing.size)
    }

    @Test
    fun `referenceImageUrl uses bundled approved yoga and exercise images`() {
        (WellnessCatalog.yoga + WellnessCatalog.exercise).forEach { move ->
            assertTrue(
                "move ${move.slug} باید تصویر تأییدشدهٔ داخل برنامه داشته باشد، ولی url=${move.referenceImageUrl}",
                move.referenceImageUrl.startsWith("file:///android_asset/practice-covers/"),
            )
            assertTrue("move ${move.slug} باید تصویر JPG داشته باشد", move.referenceImageUrl.endsWith(".jpg"))
        }
        (WellnessCatalog.breathing + WellnessCatalog.learning).forEach { move ->
            assertTrue(
                "move ${move.slug} باید به wellness-media اشاره کند، ولی url=${move.referenceImageUrl}",
                move.referenceImageUrl.contains("wellness-media"),
            )
        }
    }

    @Test
    fun `audioCueIds helper splits pipe-separated values`() {
        val balasana = WellnessCatalog.bySlug("yoga-balasana")!!
        val ids = balasana.audioCueIds
        assertEquals(3, ids.size)
        assertEquals("cue-yoga-balasana-start.mp3", ids[0])
        assertEquals("cue-yoga-balasana-mid.mp3", ids[1])
        assertEquals("cue-yoga-balasana-end.mp3", ids[2])
    }

    @Test
    fun `audioCueIds helper returns single element for non-pipe value`() {
        val squat = WellnessCatalog.bySlug("ex-squat")!!
        val ids = squat.audioCueIds
        assertEquals(1, ids.size)
        assertEquals("cue-ex-squat.mp3", ids[0])
    }

    @Test
    fun `startCueId, midCueId, endCueId return correct files`() {
        val balasana = WellnessCatalog.bySlug("yoga-balasana")!!
        assertEquals("cue-yoga-balasana-start.mp3", balasana.startCueId)
        assertEquals("cue-yoga-balasana-mid.mp3", balasana.midCueId)
        assertEquals("cue-yoga-balasana-end.mp3", balasana.endCueId)
    }

    @Test
    fun `intensity is computed correctly`() {
        val easy = WellnessCatalog.bySlug("yoga-balasana")!!  // level 1
        assertEquals(WellnessMove.Intensity.LOW, easy.intensity)

        val medium = WellnessCatalog.bySlug("yoga-cobra")!!  // level 3
        assertEquals(WellnessMove.Intensity.LOW, medium.intensity)

        val hard = WellnessCatalog.bySlug("yoga-half-boat")!!  // level 6
        assertEquals(WellnessMove.Intensity.MEDIUM, hard.intensity)
    }

    @Test
    fun `yoga slugs cover all required poses`() {
        val required = setOf(
            "yoga-balasana", "yoga-cat-cow", "yoga-downward-dog", "yoga-cobra",
            "yoga-warrior-1", "yoga-warrior-2", "yoga-tree", "yoga-bridge",
            "yoga-spinal-twist", "yoga-wide-child", "yoga-pigeon", "yoga-triangle",
            "yoga-savasana", "yoga-half-boat", "yoga-standing-forward-bend",
        )
        val actual = WellnessCatalog.yoga.map { it.slug }.toSet()
        assertEquals(required, actual)
    }
}
