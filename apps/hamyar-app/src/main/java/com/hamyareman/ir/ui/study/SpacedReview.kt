package com.hamyareman.ir.ui.study

import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.content.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * مرور فاصله‌دار (Spaced Repetition) — فقط روی دستگاه زهرا.
 *
 * قاعده‌ی ساده و قابل‌توضیح برای نوجوان:
 *  - جواب درست  → فاصله ۲.۵ برابر می‌شود (سقف ۳۰ روز).
 *  - جواب غلط   → فردا دوباره (بدون تنبیه، بدون از دست‌دادن استریک).
 *
 * چرا SharedPreferences و نه سرور؟ چون این داده «خصوصی زهرا» است و
 * سیاست ما این است که داده‌ی آموزشی خام هرگز Sync نشود؛ فقط خلاصه‌ی هفتگی
 * (با opt-in) به پدر می‌رود.
 */
internal data class ReviewItem(
    val questionId: String,
    val question: String,
    val choices: List<String>,
    val answerIndex: Int,
    val intervalDays: Int,
    val dueIso: String,
    val reps: Int,
)

private const val REVIEW_KEY = "study_review_items"
internal const val MAX_INTERVAL_DAYS = 30
private const val GROWTH_FACTOR = 2.5

internal fun readReviewItems(store: LocalStore): List<ReviewItem> = runCatching {
    val array = JSONArray(store.getString(REVIEW_KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val choices = o.optJSONArray("choices")?.let { a ->
                buildList { for (j in 0 until a.length()) add(a.optString(j)) }
            } ?: emptyList()
            if (choices.size < 2) continue
            add(
                ReviewItem(
                    questionId = o.getString("questionId"),
                    question = o.optString("question"),
                    choices = choices,
                    answerIndex = o.optInt("answerIndex").coerceIn(0, choices.lastIndex),
                    intervalDays = o.optInt("intervalDays", 1).coerceAtLeast(1),
                    dueIso = o.optString("dueIso", LocalDate.now().toString()),
                    reps = o.optInt("reps"),
                ),
            )
        }
    }
}.getOrDefault(emptyList())

private fun writeReviewItems(store: LocalStore, items: List<ReviewItem>) {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("questionId", item.questionId)
                .put("question", item.question)
                .put("choices", JSONArray(item.choices))
                .put("answerIndex", item.answerIndex)
                .put("intervalDays", item.intervalDays)
                .put("dueIso", item.dueIso)
                .put("reps", item.reps),
        )
    }
    store.putString(REVIEW_KEY, array.toString())
}

/** سؤالی که در آزمون غلط جواب داده شده → فردا دوباره. */
internal fun scheduleWrong(store: LocalStore, q: QuizQuestion) {
    val items = readReviewItems(store).toMutableList()
    val due = LocalDate.now().plusDays(1).toString()
    val existing = items.indexOfFirst { it.questionId == q.id }
    val item = ReviewItem(
        questionId = q.id,
        question = q.question,
        choices = q.choices,
        answerIndex = q.answerIndex,
        intervalDays = 1,
        dueIso = due,
        reps = if (existing >= 0) items[existing].reps else 0,
    )
    if (existing >= 0) items[existing] = item else items.add(item)
    writeReviewItems(store, items)
}

/**
 * ریاضی خالص فاصله‌ی مرور — جدا از ذخیره‌سازی تا قابل تست باشد:
 * درست ⇒ ×۲.۵ (سقف ۳۰ روز)؛ غلط ⇒ فردا (۱ روز).
 *
 * توالی واقعی: ۱ → ۳ → ۸ → ۲۰ → ۳۰ (رسید به سقف).
 */
internal fun nextIntervalDays(currentInterval: Int, answeredCorrectly: Boolean): Int =
    if (answeredCorrectly) {
        (currentInterval.coerceAtLeast(1) * GROWTH_FACTOR).roundToInt().coerceIn(1, MAX_INTERVAL_DAYS)
    } else {
        1
    }

/** مرورِ درست: فاصله ۲.۵ برابر (سقف ۳۰ روز). */
internal fun growInterval(store: LocalStore, item: ReviewItem) {
    val next = nextIntervalDays(item.intervalDays, answeredCorrectly = true)
    upsert(store, item.copy(intervalDays = next, dueIso = LocalDate.now().plusDays(next.toLong()).toString(), reps = item.reps + 1))
}

/** مرورِ غلط: برگشت به فردا. */
internal fun resetInterval(store: LocalStore, item: ReviewItem) {
    upsert(store, item.copy(intervalDays = nextIntervalDays(item.intervalDays, answeredCorrectly = false), dueIso = LocalDate.now().plusDays(1).toString(), reps = item.reps + 1))
}

private fun upsert(store: LocalStore, item: ReviewItem) {
    val items = readReviewItems(store).toMutableList()
    val i = items.indexOfFirst { it.questionId == item.questionId }
    if (i >= 0) items[i] = item else items.add(item)
    writeReviewItems(store, items)
}

/** آنچه امروز (یا عقب‌افتاده) باید مرور شود. */
internal fun dueItems(store: LocalStore, today: LocalDate = LocalDate.now()): List<ReviewItem> =
    // تاریخ‌های ISO رشته‌ای به‌درستی با هم مقایسه می‌شوند.
    readReviewItems(store).filter { it.dueIso <= today.toString() }.sortedBy { it.dueIso }
