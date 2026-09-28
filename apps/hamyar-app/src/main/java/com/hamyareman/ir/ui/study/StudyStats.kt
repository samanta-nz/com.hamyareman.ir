package com.hamyareman.ir.ui.study

import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import org.json.JSONArray
import org.json.JSONObject

/** نتیجه‌ی یک آزمون — خصوصی زهرا؛ هرگز Sync نمی‌شود (فقط خلاصه‌ی هفتگی با opt-in). */
internal data class QuizAttempt(
    val dayIso: String,
    val lessonId: String,
    val score: Int,
    val total: Int,
    val atMs: Long,
) {
    val accuracyPercent: Int get() = if (total == 0) 0 else (score * 100) / total
}

private const val KEY = "study_quiz_log"
private const val MAX_HISTORY = 200

internal fun readQuizAttempts(store: LocalStore): List<QuizAttempt> = runCatching {
    val array = JSONArray(store.getString(KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val total = o.optInt("total")
            if (total <= 0) continue
            add(
                QuizAttempt(
                    dayIso = o.optString("dayIso"),
                    lessonId = o.optString("lessonId"),
                    score = o.optInt("score").coerceIn(0, total),
                    total = total,
                    atMs = o.optLong("atMs"),
                ),
            )
        }
    }
}.getOrDefault(emptyList())

internal fun recordQuizAttempt(store: LocalStore, lessonId: String, score: Int, total: Int) {
    if (total <= 0) return
    val attempts = readQuizAttempts(store).toMutableList()
    attempts.add(
        0,
        QuizAttempt(JalaliDate.todayIso(), lessonId, score.coerceIn(0, total), total, System.currentTimeMillis()),
    )
    val array = JSONArray()
    attempts.take(MAX_HISTORY).forEach { a ->
        array.put(
            JSONObject()
                .put("dayIso", a.dayIso).put("lessonId", a.lessonId)
                .put("score", a.score).put("total", a.total).put("atMs", a.atMs),
        )
    }
    store.putString(KEY, array.toString())
}

internal fun quizAttemptsOn(store: LocalStore, dayIso: String): List<QuizAttempt> =
    readQuizAttempts(store).filter { it.dayIso == dayIso }
