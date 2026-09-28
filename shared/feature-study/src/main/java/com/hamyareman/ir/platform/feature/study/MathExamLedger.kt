package com.hamyareman.ir.platform.feature.study

import org.json.JSONArray
import org.json.JSONObject

/**
 * دفتر آزمون چهارگزینه‌ای ریاضی.
 *
 *  - هر نشست به فهرست متوالی اضافه می‌شود (تعداد تکرار، نتیجه، شماره سوالات غلط).
 *  - نمودار فقط [latest] را نشان می‌دهد؛ تجدید آزمون نتیجهٔ قبلی نمودار را جایگزین می‌کند.
 */
object MathExamLedger {

    data class Sitting(
        val n: Int,
        val scorePct: Int,
        val total: Int,
        val correctCount: Int,
        val wrongNumbers: List<Int>,
        val wrongIds: List<String>,
        val atMs: Long,
        val dateKey: String,
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("n", n)
            .put("scorePct", scorePct)
            .put("total", total)
            .put("correctCount", correctCount)
            .put("wrongNumbers", JSONArray(wrongNumbers))
            .put("wrongIds", JSONArray(wrongIds))
            .put("atMs", atMs)
            .put("dateKey", dateKey)

        companion object {
            fun fromJson(o: JSONObject): Sitting = Sitting(
                n = o.optInt("n"),
                scorePct = o.optInt("scorePct"),
                total = o.optInt("total"),
                correctCount = o.optInt("correctCount"),
                wrongNumbers = intList(o.optJSONArray("wrongNumbers")),
                wrongIds = strList(o.optJSONArray("wrongIds")),
                atMs = o.optLong("atMs"),
                dateKey = o.optString("dateKey"),
            )
        }
    }

    data class State(val sittings: List<Sitting> = emptyList()) {
        val repeatCount: Int get() = sittings.size
        /** نتیجهٔ جایگزین‌شونده برای نمودار. */
        val latest: Sitting? get() = sittings.maxByOrNull { it.n }
        fun toJson(): String {
            val a = JSONArray()
            sittings.forEach { a.put(it.toJson()) }
            return JSONObject().put("sittings", a).toString()
        }

        companion object {
            fun fromJson(raw: String): State {
                if (raw.isBlank()) return State()
                val o = runCatching { JSONObject(raw) }.getOrNull() ?: return State()
                val a = o.optJSONArray("sittings") ?: return State()
                val list = (0 until a.length()).mapNotNull { i ->
                    a.optJSONObject(i)?.let { Sitting.fromJson(it) }
                }
                return State(list)
            }
        }
    }

    fun mcqOf(pack: StudyPack): List<StudyPack.Question> =
        pack.questions.filter { it.type == "mcq" && it.topic != "book" }

    /**
     * تصحیح با کلید [QuizGrader]، شماره‌گذاری ۱-پایهٔ سوالات غلط، افزودن نشست جدید.
     * [latest] بعد از این همان نشست تازه است (جایگزین نمودار).
     */
    fun record(
        previous: State,
        questions: List<StudyPack.Question>,
        answers: Map<String, String>,
        dateKey: String,
        atMs: Long,
    ): State {
        val mcq = questions.filter { it.type == "mcq" }
        if (mcq.isEmpty()) return previous
        val wrongNumbers = mutableListOf<Int>()
        val wrongIds = mutableListOf<String>()
        var correct = 0
        mcq.forEachIndexed { i, q ->
            val ok = QuizGrader.grade(q, answers[q.id].orEmpty()).second
            if (ok) correct++ else {
                wrongNumbers += i + 1
                wrongIds += q.id
            }
        }
        val sitting = Sitting(
            n = previous.repeatCount + 1,
            scorePct = ((correct * 100) / mcq.size).coerceIn(0, 100),
            total = mcq.size,
            correctCount = correct,
            wrongNumbers = wrongNumbers,
            wrongIds = wrongIds,
            atMs = atMs,
            dateKey = dateKey,
        )
        return State(previous.sittings + sitting)
    }

    fun merge(local: State, remote: State): State {
        val byN = LinkedHashMap<Int, Sitting>()
        (local.sittings + remote.sittings).sortedBy { it.n }.forEach { s ->
            val old = byN[s.n]
            if (old == null || s.atMs >= old.atMs) byN[s.n] = s
        }
        val ordered = byN.values.sortedBy { it.n }
        return State(ordered.mapIndexed { i, s -> s.copy(n = i + 1) })
    }

    private fun intList(a: JSONArray?): List<Int> {
        a ?: return emptyList()
        return (0 until a.length()).map { a.optInt(it) }
    }

    private fun strList(a: JSONArray?): List<String> {
        a ?: return emptyList()
        return (0 until a.length()).map { a.optString(it) }
    }
}
