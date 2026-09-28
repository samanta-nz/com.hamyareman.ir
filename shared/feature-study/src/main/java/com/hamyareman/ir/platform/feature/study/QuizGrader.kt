package com.hamyareman.ir.platform.feature.study

import kotlin.math.abs

/**
 * تصحیح‌گر آفلاین قطعی آزمون — بدون AI هم کامل کار می‌کند.
 *  - mcq: تطبیق گزینه (حرف یا متن کامل)
 *  - numeric: نرمال‌سازی ارقام فارسی/عربی، اعشار «٫/،» و تساهل جزئی (ε)
 *  - short: هم‌پوشانی کلیدواژه‌های جواب (≥0.6 کامل، ≥0.3 نیم‌نمره)
 * خروجی: نمره، موضوع‌های ضعف، و ارجاع «نکات مرتبط» برای هر اشتباه.
 */
object QuizGrader {

    data class ItemResult(
        val questionId: String,
        val given: String,
        val score: Double,          // 0.0 .. 1.0
        val correct: Boolean,
        val refSectionId: String,
        val topic: String,
    )

    data class Outcome(
        val results: List<ItemResult>,
        val scorePct: Int,          // 0..100
        val weakTopics: List<String>,
        val wrongQuestionIds: List<String>,
    )

    fun normalize(s: String): String {
        val en = s.trim()
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
        return en.replace('،', ' ').replace('٫', '.').lowercase().trim()
    }

    fun grade(question: StudyPack.Question, givenRaw: String): Pair<Double, Boolean> {
        val given = normalize(givenRaw)
        if (given.isBlank()) return 0.0 to false
        return when (question.type) {
            "mcq" -> {
                // سه راه درست: متن کامل برابر، یا برچسب گزینه («الف»)، یا همان با برچسب.
                val want = normalize(question.answer)
                val label = want.substringBefore(')').trim()
                val wantBody = if (label != want) want.substringAfter(')').trim() else want
                val ok = given == want ||
                    given == wantBody ||
                    (label.isNotBlank() && (given == label || want.startsWith("$given)") || given.startsWith("$label)")))
                (if (ok) 1.0 else 0.0) to ok
            }
            "numeric" -> {
                val a = toNumber(question.answer)
                val b = toNumber(givenRaw)
                val ok = a != null && b != null && abs(a - b) < 1e-6
                (if (ok) 1.0 else 0.0) to ok
            }
            else -> { // short
                val key = normalize(question.answer).split(Regex("\\s+")).filter { it.length > 1 }.toSet()
                if (key.isEmpty()) return 0.0 to false
                val tokens = given.split(Regex("\\s+")).toSet()
                val overlap = key.count { k -> tokens.any { t -> t == k || t.contains(k) || k.contains(t) } }.toDouble() / key.size
                val score = when {
                    overlap >= 0.6 -> 1.0
                    overlap >= 0.3 -> 0.5
                    else -> 0.0
                }
                score to (score >= 1.0)
            }
        }
    }

    fun gradeAll(pack: StudyPack, answers: Map<String, String>): Outcome {
        val results = pack.questions.mapNotNull { q ->
            val given = answers[q.id] ?: return@mapNotNull null
            val (score, correct) = grade(q, given)
            ItemResult(q.id, given, score, correct, q.refSectionId, q.topic)
        }
        val total = results.size.coerceAtLeast(1)
        val scorePct = ((results.sumOf { it.score } / total) * 100).toInt().coerceIn(0, 100)
        val wrong = results.filter { !it.correct }
        val weak = wrong.groupingBy { it.topic }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        return Outcome(results, scorePct, weak, wrong.map { it.questionId })
    }

    private fun toNumber(s: String): Double? {
        normalize(s).toDoubleOrNull()?.let { return it }
        // کسر ساده: a/b
        val parts = normalize(s).replace('٫', '.').split("/")
        if (parts.size == 2) {
            val a = parts[0].trim().toDoubleOrNull()
            val b = parts[1].trim().toDoubleOrNull()
            if (a != null && b != null && b != 0.0) return a / b
        }
        return normalize(s).replace(Regex("[^0-9.\\-]"), "").toDoubleOrNull()
    }
}
