package com.hamyareman.ir.platform.feature.study

import kotlin.math.abs

/**
 * اسکریپت تصحیح و تکرار ریاضی نهم.
 *
 *  - نرمال‌سازی رقم فارسی/عربی، فاصله، آکولاد، نمادهای هم‌ارز؛
 *  - کسر `a/b` با مقدار اعشاری مقایسه می‌شود؛
 *  - مجموعه با ترتیب/تکرار بی‌اثر؛
 *  - برنامه‌ی تکرار: غلط → همان روز دوباره (+دفعات بر اساس اشتباه‌های پشت‌سرهم)؛
 *    درست → فاصله بیشتر؛ چند درست پشت‌سرهم → آرشیو.
 */
object MathAnswerScript {

    data class RepeatPlan(
        val sameDayRepeats: Int,
        val intervalDays: Int,
        val archive: Boolean,
        val message: String,
    )

    fun normalize(raw: String): String {
        val b = StringBuilder(raw.length)
        raw.trim().forEach { ch ->
            when (ch) {
                in '۰'..'۹' -> b.append('0' + (ch - '۰'))
                in '٠'..'٩' -> b.append('0' + (ch - '٠'))
                '٫' -> b.append('.')
                '،' -> b.append(',')
                '×', '⋅', '*' -> b.append('*')
                '÷' -> b.append('/')
                '−', '–', '—' -> b.append('-')
                '｛', '［' -> b.append('{')
                '｝', '］' -> b.append('}')
                '（' -> b.append('(')
                '）' -> b.append(')')
                '⊂' -> b.append('⊆')
                '∅', 'ø' -> b.append('∅')
                ' ', '\t', '\u200c', '\u200d' -> Unit
                else -> b.append(ch)
            }
        }
        return b.toString().lowercase()
            .replace("\\subseteq", "⊆")
            .replace("\\in", "∈")
            .replace("\\notin", "∉")
            .replace("\\cup", "∪")
            .replace("\\cap", "∩")
            .replace("\\emptyset", "∅")
            .replace("emptyset", "∅")
            .replace("تهی", "∅")
    }

    fun equivalent(givenRaw: String, expected: String, alts: List<String> = emptyList()): Boolean {
        val given = normalize(givenRaw)
        if (given.isBlank()) return false
        val candidates = (listOf(expected) + alts).map { normalize(it) }.filter { it.isNotBlank() }
        if (candidates.any { it == given }) return true
        if (candidates.any { numericEq(given, it) }) return true
        if (candidates.any { setEq(given, it) }) return true
        if (candidates.any { fractionEq(given, it) }) return true
        return false
    }

    fun grade(exercise: StudyPack.Exercise, given: String): Boolean =
        equivalent(given, exercise.answer, exercise.altAnswers)

    /**
     * [consecutiveWrong] تعداد غلط پشت‌سرهم قبل از این پاسخ (۰ اگر اولین تلاش).
     * [consecutiveCorrect] تعداد درست پشت‌سرهم شامل همین پاسخ اگر درست باشد.
     */
    fun repeatPlan(correct: Boolean, consecutiveWrong: Int, consecutiveCorrect: Int): RepeatPlan {
        return if (correct) {
            val archive = consecutiveCorrect >= 2
            RepeatPlan(
                sameDayRepeats = 0,
                intervalDays = if (archive) 14 else 3,
                archive = archive,
                message = if (archive) "درست بود — این مورد به آرشیو رفت. هر وقت خواستی از آرشیو دوباره ببین."
                else "درست بود ✓  سه روز دیگر یک‌بار دیگر مرور می‌شود.",
            )
        } else {
            val extra = (1 + consecutiveWrong).coerceAtMost(4)
            RepeatPlan(
                sameDayRepeats = extra,
                intervalDays = 0,
                archive = false,
                message = "دوباره حل کن. این مورد امروز ${extra} بار دیگر می‌آید تا جا بیفتد.",
            )
        }
    }

    /** کیفیت SM-2 از روی درستی پاسخ فلش‌کارتِ تایپی (اگر بعداً تایپ شد). */
    fun flashQuality(correct: Boolean, firstTry: Boolean): Int = when {
        correct && firstTry -> 5
        correct -> 4
        else -> 1
    }

    private fun numericEq(a: String, b: String): Boolean {
        val x = toNumber(a) ?: return false
        val y = toNumber(b) ?: return false
        return abs(x - y) < 1e-6
    }

    private fun fractionEq(a: String, b: String): Boolean {
        val x = toNumber(a) ?: return false
        val y = toNumber(b) ?: return false
        return abs(x - y) < 1e-6
    }

    private fun setEq(a: String, b: String): Boolean {
        val sa = parseSet(a) ?: return false
        val sb = parseSet(b) ?: return false
        return sa == sb
    }

    private fun parseSet(s: String): Set<String>? {
        val t = s.trim()
        if (t == "∅" || t == "{}" || t == "{ }") return emptySet()
        if (!t.startsWith("{") || !t.endsWith("}")) return null
        val inner = t.substring(1, t.length - 1)
        if (inner.isBlank()) return emptySet()
        return inner.split(',', '،').map { normalize(it) }.filter { it.isNotBlank() }.toSet()
    }

    private fun toNumber(s: String): Double? {
        val n = normalize(s)
        n.toDoubleOrNull()?.let { return it }
        val frac = n.split("/")
        if (frac.size == 2) {
            val a = frac[0].toDoubleOrNull()
            val b = frac[1].toDoubleOrNull()
            if (a != null && b != null && b != 0.0) return a / b
        }
        return n.replace(Regex("[^0-9.\\-]"), "").toDoubleOrNull()
    }
}
