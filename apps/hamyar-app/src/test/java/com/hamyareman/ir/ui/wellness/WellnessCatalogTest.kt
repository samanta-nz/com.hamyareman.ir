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
 *  - durationSec > 0
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

    // کاتالوگ دیگر audioCueId ندارد، پس این سه تست قرارداد خودِ helper را روی
    // یک نمونهٔ ساختگی می‌سنجند تا اگر روزی مقدار از جدول Appwrite آمد، کار کند.

    @Test
    fun `audioCueIds helper splits pipe-separated values`() {
        val move = WellnessCatalog.bySlug("yoga-balasana")!!
            .copy(audioCueId = "a-start.mp3|a-mid.mp3|a-end.mp3")
        val ids = move.audioCueIds
        assertEquals(3, ids.size)
        assertEquals("a-start.mp3", ids[0])
        assertEquals("a-mid.mp3", ids[1])
        assertEquals("a-end.mp3", ids[2])
    }

    @Test
    fun `audioCueIds helper returns single element for non-pipe value`() {
        val move = WellnessCatalog.bySlug("ex-squat")!!.copy(audioCueId = "solo.mp3")
        val ids = move.audioCueIds
        assertEquals(1, ids.size)
        assertEquals("solo.mp3", ids[0])
    }

    @Test
    fun `startCueId, midCueId, endCueId return correct files`() {
        val move = WellnessCatalog.bySlug("yoga-balasana")!!
            .copy(audioCueId = "a-start.mp3|a-mid.mp3|a-end.mp3")
        assertEquals("a-start.mp3", move.startCueId)
        assertEquals("a-mid.mp3", move.midCueId)
        assertEquals("a-end.mp3", move.endCueId)
    }

    @Test
    fun `audioCueIds helper returns empty list for blank value`() {
        assertTrue(WellnessCatalog.bySlug("yoga-balasana")!!.audioCueIds.isEmpty())
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

    @Test
    fun `catalog carries no dead media addresses`() {
        // تصاویر و صوت‌های باکت wellness-media هرگز ساخته نشدند؛ آدرسشان حذف شد.
        // این تست جلوی برگشتن اتفاقی آن ارجاع‌های مرده را می‌گیرد.
        WellnessCatalog.all.forEach { move ->
            assertTrue(
                "move ${move.slug} نباید referenceImageUrl داشته باشد",
                move.referenceImageUrl.isBlank(),
            )
            assertTrue(
                "move ${move.slug} نباید audioCueId داشته باشد",
                move.audioCueId.isBlank(),
            )
        }
    }
}
