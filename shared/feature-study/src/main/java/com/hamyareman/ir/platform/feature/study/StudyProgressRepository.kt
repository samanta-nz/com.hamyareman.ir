package com.hamyareman.ir.platform.feature.study

import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.sync.SyncEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * حافظه‌ی پیشرفت مطالعه: وضعیت SM-2 هر فلش‌کارت + تاریخچه‌ی آزمون‌های هر پک.
 * الگو مانند پرامپت ۰۱: نوشتن همیشه محلی، ارسال با صف [SyncEngine] (آفلاین‌پسند).
 */
class StudyProgressRepository(
    private val store: LocalStore,
    private val sync: SyncEngine,
    private val userIdProvider: () -> String,
) {

    data class Attempt(
        val dateKey: String,      // yyyy-MM-dd
        val scorePct: Int,
        val total: Int,
        val wrongIds: List<String>,
        val weakTopics: List<String>,
        /** لحظه‌ی دقیق آزمون (epoch) — v1.13: نمایش با تاریخ/ساعت شمسی. */
        val atMs: Long = 0L,
    ) {
        fun toJson(): String = JSONObject()
            .put("dateKey", dateKey).put("scorePct", scorePct).put("total", total)
            .put("wrongIds", JSONArray(wrongIds)).put("weakTopics", JSONArray(weakTopics))
            .put("atMs", atMs).toString()

        companion object {
            fun fromJson(raw: String): Attempt = runCatching {
                val o = JSONObject(raw)
                fun arr(o: JSONObject, key: String): List<String> {
                    val a = o.optJSONArray(key) ?: return emptyList()
                    return (0 until a.length()).map { a.getString(it) }
                }
                Attempt(
                    dateKey = o.optString("dateKey"),
                    scorePct = o.optInt("scorePct"),
                    total = o.optInt("total"),
                    wrongIds = arr(o, "wrongIds"),
                    weakTopics = arr(o, "weakTopics"),
                    atMs = o.optLong("atMs"),
                )
            }.getOrDefault(Attempt("", 0, 0, emptyList(), emptyList()))
        }
    }

    /** آزمون دوره‌ای: هر ۷ روز یک‌بار مرورِ اشتباه‌های قبلی پیشنهاد می‌شود. */
    val PERIODIC_DAYS = 7

    // ---------- فلش‌کارت‌ها ----------

    fun cards(packId: String): Map<String, Sm2.CardState> {
        val raw = store.getString("study:$packId:cards")
        if (raw.isBlank()) return emptyMap()
        val o = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
        val out = mutableMapOf<String, Sm2.CardState>()
        o.keys().forEach { k -> out[k] = Sm2.CardState.fromJson(o.getJSONObject(k).toString()) }
        return out
    }

    fun stateOf(packId: String, cardId: String): Sm2.CardState = cards(packId)[cardId] ?: Sm2.CardState()

    fun reviewCard(packId: String, cardId: String, quality: Int, todayKey: String): Sm2.CardState {
        val all = cards(packId).toMutableMap()
        val next = Sm2.review(stateOf(packId, cardId), quality, todayKey)
        all[cardId] = next
        val o = JSONObject()
        all.forEach { (k, v) -> o.put(k, JSONObject(v.toJson())) }
        store.putString("study:$packId:cards", o.toString())
        enqueue(packId)
        return next
    }

    /** کارت‌های سررسید امروز (کارت‌های نو هم اولین‌بار سررسیدند). */
    fun dueCards(pack: StudyPack, todayKey: String): List<StudyPack.Flashcard> {
        val states = cards(pack.packId)
        val archived = archivedIds(pack.packId)
        val sameDay = sameDayLeft(pack.packId)
        return pack.flashcards.filter { c ->
            if (sameDay[c.id] ?: 0 > 0) true
            else if (c.id in archived) false
            else Sm2.isDue(states[c.id] ?: Sm2.CardState(), todayKey)
        }
    }

    fun archivedIds(packId: String): Set<String> {
        val raw = store.getString("study:$packId:archive")
        if (raw.isBlank()) return emptySet()
        val a = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until a.length()).map { a.getString(it) }.toSet()
    }

    fun archivedCards(pack: StudyPack): List<StudyPack.Flashcard> {
        val ids = archivedIds(pack.packId)
        return pack.flashcards.filter { it.id in ids }
    }

    fun setArchived(packId: String, cardId: String, archived: Boolean) {
        val next = archivedIds(packId).toMutableSet()
        if (archived) next.add(cardId) else next.remove(cardId)
        val arr = JSONArray()
        next.forEach { arr.put(it) }
        store.putString("study:$packId:archive", arr.toString())
        enqueue(packId)
    }

    fun sameDayLeft(packId: String): Map<String, Int> {
        val raw = store.getString("study:$packId:sameday")
        if (raw.isBlank()) return emptyMap()
        val o = runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
        val out = mutableMapOf<String, Int>()
        o.keys().forEach { k -> out[k] = o.optInt(k) }
        return out
    }

    private fun writeSameDay(packId: String, map: Map<String, Int>) {
        val o = JSONObject()
        map.filter { it.value > 0 }.forEach { (k, v) -> o.put(k, v) }
        store.putString("study:$packId:sameday", o.toString())
    }

    /**
     * مرور فلش‌کارت با اسکریپت ریاضی: کیفیت SM-2 + آرشیو/تکرار همان‌روز.
     */
    fun reviewCardMath(packId: String, cardId: String, quality: Int, todayKey: String): Sm2.CardState {
        val before = stateOf(packId, cardId)
        val next = reviewCard(packId, cardId, quality, todayKey)
        val correct = quality >= 4
        val wrongStreak = if (correct) 0 else before.lapses + 1
        val correctStreak = if (correct) next.reps else 0
        val plan = MathAnswerScript.repeatPlan(correct, wrongStreak.coerceAtLeast(0), correctStreak)
        val sd = sameDayLeft(packId).toMutableMap()
        if (plan.sameDayRepeats > 0) sd[cardId] = plan.sameDayRepeats
        else sd.remove(cardId)
        writeSameDay(packId, sd)
        setArchived(packId, cardId, plan.archive)
        return next
    }

    fun consumeSameDay(packId: String, cardId: String) {
        val sd = sameDayLeft(packId).toMutableMap()
        val left = (sd[cardId] ?: 0) - 1
        if (left <= 0) sd.remove(cardId) else sd[cardId] = left
        writeSameDay(packId, sd)
    }

    fun recordExercise(packId: String, exerciseId: String, correct: Boolean, todayKey: String) {
        val raw = store.getString("study:$packId:ex")
        val o = runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrDefault(JSONObject())
        val item = o.optJSONObject(exerciseId) ?: JSONObject()
        item.put("tries", item.optInt("tries") + 1)
        if (correct) item.put("ok", item.optInt("ok") + 1) else item.put("bad", item.optInt("bad") + 1)
        item.put("last", todayKey)
        item.put("lastOk", correct)
        o.put(exerciseId, item)
        store.putString("study:$packId:ex", o.toString())
        val wrong = if (correct) 0 else item.optInt("bad")
        val okStreak = if (correct) item.optInt("ok") else 0
        val plan = MathAnswerScript.repeatPlan(correct, wrong, okStreak)
        val sd = sameDayLeft(packId).toMutableMap()
        val key = "ex-$exerciseId"
        if (plan.sameDayRepeats > 0) sd[key] = plan.sameDayRepeats else sd.remove(key)
        writeSameDay(packId, sd)
        enqueue(packId)
    }

    fun exerciseStats(packId: String): JSONObject =
        runCatching { JSONObject(store.getString("study:$packId:ex").ifBlank { "{}" }) }.getOrDefault(JSONObject())

    /** درصد تسلط کل پک (کارت‌های نو = صفر). */
    fun masteryPct(pack: StudyPack): Int {
        if (pack.flashcards.isEmpty()) return 0
        val states = cards(pack.packId)
        val sum = pack.flashcards.sumOf { Sm2.mastery(states[it.id] ?: Sm2.CardState()) }
        return (sum / pack.flashcards.size).coerceIn(0, 100)
    }

    // ---------- آزمون‌ها ----------

    fun attempts(packId: String): List<Attempt> {
        val raw = store.getString("study:$packId:attempts")
        if (raw.isBlank()) return emptyList()
        val a = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return (0 until a.length()).map { Attempt.fromJson(a.getJSONObject(it).toString()) }
    }

    fun recordAttempt(packId: String, attempt: Attempt) {
        val all = attempts(packId) + attempt
        val arr = JSONArray()
        all.takeLast(50).forEach { arr.put(JSONObject(it.toJson())) }
        store.putString("study:$packId:attempts", arr.toString())
        enqueue(packId)
    }

    fun examState(packId: String): MathExamLedger.State =
        MathExamLedger.State.fromJson(store.getString("study:$packId:exam"))

    fun writeExamState(packId: String, state: MathExamLedger.State) {
        store.putString("study:$packId:exam", state.toJson())
        val latest = state.latest
        if (latest != null) {
            val chart = Attempt(
                dateKey = latest.dateKey,
                scorePct = latest.scorePct,
                total = latest.total,
                wrongIds = latest.wrongIds,
                weakTopics = emptyList(),
                atMs = latest.atMs,
            )
            store.putString("study:$packId:attempts", JSONArray().put(JSONObject(chart.toJson())).toString())
        }
        enqueue(packId)
    }

    fun recordExamSitting(
        pack: StudyPack,
        answers: Map<String, String>,
        dateKey: String,
        atMs: Long = System.currentTimeMillis(),
    ): MathExamLedger.State {
        val next = MathExamLedger.record(examState(pack.packId), MathExamLedger.mcqOf(pack), answers, dateKey, atMs)
        writeExamState(pack.packId, next)
        return next
    }

    /** نمره از HTML تعاملی (فلش/آزمون/تمرین). */
    fun recordHtmlExam(
        packId: String,
        scorePct: Int,
        total: Int,
        correctCount: Int,
        wrongIds: List<String>,
        dateKey: String,
        atMs: Long = System.currentTimeMillis(),
    ): MathExamLedger.State {
        val prev = examState(packId)
        val sitting = MathExamLedger.Sitting(
            n = prev.repeatCount + 1,
            scorePct = scorePct.coerceIn(0, 100),
            total = total.coerceAtLeast(0),
            correctCount = correctCount.coerceAtLeast(0),
            wrongNumbers = wrongIds.mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() },
            wrongIds = wrongIds,
            atMs = atMs,
            dateKey = dateKey,
        )
        val next = MathExamLedger.State(prev.sittings + sitting)
        writeExamState(packId, next)
        return next
    }

    /** آزمون دوره‌ای سررسید شده؟ (۷ روز از آخرین آزمون گذشته باشد) */
    fun periodicQuizDue(packId: String, todayKey: String): Boolean {
        val last = attempts(packId).maxByOrNull { it.dateKey } ?: return false
        return Sm2.addDays(last.dateKey, PERIODIC_DAYS) <= todayKey
    }

    /** سوال‌های «دوره‌ای»: اشتباه‌های آزمون‌های قبلی (بازپرسی تا اطمینان). */
    fun periodicWrongIds(packId: String): List<String> =
        attempts(packId).flatMap { it.wrongIds }.distinct()

    // ---------- سینک ----------

    private fun enqueue(packId: String) {
        val uid = userIdProvider().ifBlank { "anon" }
        val rowId = "sp-${uid}-${packId}".replace(Regex("[^A-Za-z0-9_.\\-]"), "_")
        val extras = JSONObject()
            .put("archive", store.getString("study:$packId:archive"))
            .put("sameday", store.getString("study:$packId:sameday"))
            .put("ex", store.getString("study:$packId:ex"))
            .toString()
        val payload = mapOf(
            "userId" to uid,
            "packId" to packId,
            "srsState" to store.getString("study:$packId:cards"),
            "attempts" to store.getString("study:$packId:attempts"),
            "examLedger" to store.getString("study:$packId:exam"),
            "extras" to extras,
            "updatedAtIso" to java.time.Instant.now().toString(),
        )
        sync.enqueue(com.hamyareman.ir.platform.core.common.TableIds.STUDY_PROGRESS, rowId, payload)
        afterWrite?.invoke(packId)
    }

    /** وضعیتِ فعلیِ این پک را دوباره در صفِ ارسال می‌گذارد (برای سینکِ نمودار پیشرفت). */
    fun flush(packId: String) = enqueue(packId)

    companion object {
        @Volatile var afterWrite: ((String) -> Unit)? = null
    }
}
