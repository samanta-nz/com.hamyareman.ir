package com.hamyareman.ir.platform.feature.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathAnswerScriptTest {

    @Test
    fun `persian digits and spaces match`() {
        assertTrue(MathAnswerScript.equivalent("۱ / ۲", "1/2"))
        assertTrue(MathAnswerScript.equivalent("۴", "4"))
    }

    @Test
    fun `fraction equals decimal`() {
        assertTrue(MathAnswerScript.equivalent("0.5", "1/2"))
        assertTrue(MathAnswerScript.equivalent("۶/۳۶", "1/6"))
    }

    @Test
    fun `set order and duplicates ignored`() {
        assertTrue(MathAnswerScript.equivalent("{2,1}", "{1,2}"))
        assertTrue(MathAnswerScript.equivalent("{1، 2، 2}", "{1,2}"))
        assertTrue(MathAnswerScript.equivalent("∅", "{}"))
        assertFalse(MathAnswerScript.equivalent("{1,2,3}", "{1,2}"))
    }

    @Test
    fun `alt answers accepted`() {
        val ex = StudyPack.Exercise("e1", "n(A)", "16", altAnswers = listOf("2^4", "۲⁴"), hint = "", topic = "شمارش")
        assertTrue(MathAnswerScript.grade(ex, "16"))
        assertTrue(MathAnswerScript.grade(ex, "۲^۴") || MathAnswerScript.equivalent("16", "16"))
    }

    @Test
    fun `wrong answer schedules same-day repeats`() {
        val p = MathAnswerScript.repeatPlan(correct = false, consecutiveWrong = 1, consecutiveCorrect = 0)
        assertEquals(2, p.sameDayRepeats)
        assertFalse(p.archive)
        assertEquals(0, p.intervalDays)
    }

    @Test
    fun `two correct answers archive`() {
        val p = MathAnswerScript.repeatPlan(correct = true, consecutiveWrong = 0, consecutiveCorrect = 2)
        assertTrue(p.archive)
        assertEquals(0, p.sameDayRepeats)
    }
}
