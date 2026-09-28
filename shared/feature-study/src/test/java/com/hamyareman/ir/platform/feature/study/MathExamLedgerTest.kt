package com.hamyareman.ir.platform.feature.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExamLedgerTest {

    private fun q(id: String, answer: String, vararg options: String) = StudyPack.Question(
        id = id, type = "mcq", text = "؟", options = options.toList(), answer = answer,
        explanation = "", topic = "ت", difficulty = 1, refSectionId = "s1",
    )

    private val bank = listOf(
        q("q1", "الف", "الف", "ب", "ج", "د"),
        q("q2", "ب", "الف", "ب", "ج", "د"),
        q("q3", "ج", "الف", "ب", "ج", "د"),
        q("q4", "د", "الف", "ب", "ج", "د"),
    )

    @Test
    fun `first sitting numbers and score`() {
        val s = MathExamLedger.record(
            MathExamLedger.State(),
            bank,
            mapOf("q1" to "الف", "q2" to "ب", "q3" to "الف", "q4" to "د"),
            dateKey = "1404-01-01",
            atMs = 1000L,
        )
        assertEquals(1, s.repeatCount)
        assertEquals(1, s.latest!!.n)
        assertEquals(75, s.latest!!.scorePct)
        assertEquals(listOf(3), s.latest!!.wrongNumbers)
        assertEquals(listOf("q3"), s.latest!!.wrongIds)
    }

    @Test
    fun `retake replaces chart latest but keeps sequential log`() {
        val first = MathExamLedger.record(
            MathExamLedger.State(),
            bank,
            mapOf("q1" to "ب", "q2" to "ب", "q3" to "ب", "q4" to "ب"),
            "1404-01-01",
            1000L,
        )
        assertEquals(25, first.latest!!.scorePct)
        val second = MathExamLedger.record(
            first,
            bank,
            mapOf("q1" to "الف", "q2" to "ب", "q3" to "ج", "q4" to "د"),
            "1404-01-02",
            2000L,
        )
        assertEquals(2, second.repeatCount)
        assertEquals(100, second.latest!!.scorePct)
        assertEquals(2, second.latest!!.n)
        assertEquals(25, second.sittings.first().scorePct)
        assertTrue(second.sittings.first().wrongNumbers.containsAll(listOf(1, 3, 4)))
        assertTrue(second.sittings.last().wrongNumbers.isEmpty())
    }

    @Test
    fun `blank answers count as wrong sequential numbers`() {
        val s = MathExamLedger.record(
            MathExamLedger.State(),
            bank,
            emptyMap(),
            "1404-01-01",
            1L,
        )
        assertEquals(0, s.latest!!.scorePct)
        assertEquals(listOf(1, 2, 3, 4), s.latest!!.wrongNumbers)
    }

    @Test
    fun `empty mcq bank is no-op`() {
        val prev = MathExamLedger.State()
        val next = MathExamLedger.record(prev, emptyList(), emptyMap(), "x", 1L)
        assertEquals(0, next.repeatCount)
    }
}
