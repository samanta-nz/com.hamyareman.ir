package com.hamyareman.ir.platform.feature.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizGraderTest {

    private fun q(type: String, answer: String) = StudyPack.Question(
        id = "q1", type = type, text = "؟", options = emptyList(), answer = answer,
        explanation = "", topic = "ت", difficulty = 1, refSectionId = "s1",
    )

    @Test
    fun `گزینه چندجوابی با حرف یا متن کامل`() {
        val q = q("mcq", "الف) {۱،۲،۳}")
        assertEquals(1.0, QuizGrader.grade(q, "الف").first, 1e-9)
        assertEquals(1.0, QuizGrader.grade(q, "{۱،۲،۳}").first, 1e-9)
        assertEquals(0.0, QuizGrader.grade(q, "ب").first, 1e-9)
    }

    @Test
    fun `عدد با ارقام فارسی و کسر`() {
        val q = q("numeric", "1.5")
        assertEquals(1.0, QuizGrader.grade(q, "۱٫۵").first, 1e-9)
        assertEquals(1.0, QuizGrader.grade(q, "3/2").first, 1e-9)
        assertEquals(0.0, QuizGrader.grade(q, "2").first, 1e-9)
    }

    @Test
    fun `جواب کوتاه با هم‌پوشانی کلیدواژه`() {
        val q = q("short", "مجموعه‌ای که هیچ عنصری ندارد، مجموعه تهی نام دارد")
        assertTrue(QuizGrader.grade(q, "مجموعه تهی").first >= 0.5)
        assertEquals(0.0, QuizGrader.grade(q, "علی").first, 1e-9)
    }

    @Test
    fun `نرمال‌سازی ارقام`() {
        assertEquals("12.5", QuizGrader.normalize("۱۲٫۵"))
        assertEquals("abc", QuizGrader.normalize(" ABC "))
    }
}
