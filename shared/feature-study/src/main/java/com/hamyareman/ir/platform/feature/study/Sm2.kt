package com.hamyareman.ir.platform.feature.study

import org.json.JSONObject

/**
 * موتور مرور فاصله‌دار — SM-2 سبک (تطبیقی و قابل‌توضیح برای نوجوان).
 *
 * کیفیت پاسخ q ∈ 0..5:
 *   0-2 = بلد نبودم (کارت به روزِ بعد برمی‌گردد و شمارنده‌ی یادگیری صفر می‌شود)
 *   3   = سخت بود   | 4 = خوب بود   | 5 = عالی بود
 *
 * فاصله: تکرار اول = ۱ روز، دوم = ۳ روز، بعدی‌ها = فاصله‌ی قبلی × ضریب سختی (ease).
 * ease با کیفیت جابه‌جا می‌شود (۵→+۰٫۱ ، ۴→بدون تغییر ، ۳→−۰٫۱۵) تا کارت‌های
 * مشکل‌دار زودتر و مکرر برگردند — «تا زمانی که مطمئن شوی یاد گرفته‌ای».
 */
object Sm2 {

    data class CardState(
        val ease: Double = 2.5,
        val reps: Int = 0,
        val intervalDays: Int = 0,
        val dueKey: String = "",
        val lapses: Int = 0,
    ) {
        fun toJson(): String = JSONObject()
            .put("ease", ease).put("reps", reps).put("intervalDays", intervalDays)
            .put("dueKey", dueKey).put("lapses", lapses).toString()

        companion object {
            fun fromJson(raw: String): CardState = runCatching {
                val o = JSONObject(raw)
                CardState(
                    ease = o.optDouble("ease", 2.5),
                    reps = o.optInt("reps", 0),
                    intervalDays = o.optInt("intervalDays", 0),
                    dueKey = o.optString("dueKey", ""),
                    lapses = o.optInt("lapses", 0),
                )
            }.getOrDefault(CardState())
        }
    }

    const val MIN_EASE = 1.3
    const val MAX_INTERVAL_DAYS = 365

    /** اعمال یک مرور روی وضعیت کارت. [todayKey] قالب yyyy-MM-dd. */
    fun review(state: CardState, quality: Int, todayKey: String): CardState {
        val q = quality.coerceIn(0, 5)
        if (q < 3) {
            return state.copy(
                ease = (state.ease - 0.2).coerceAtLeast(MIN_EASE),
                reps = 0,
                intervalDays = 1,
                dueKey = addDays(todayKey, 1),
                lapses = state.lapses + 1,
            )
        }
        val ease = (state.ease + when (q) {
            5 -> 0.10
            4 -> 0.00
            else -> -0.15
        }).coerceAtLeast(MIN_EASE)
        val reps = state.reps + 1
        val interval = when {
            reps == 1 -> 1
            reps == 2 -> 3
            else -> kotlin.math.ceil(state.intervalDays * ease).toInt().coerceAtLeast(4).coerceAtMost(MAX_INTERVAL_DAYS)
        }
        return state.copy(ease = ease, reps = reps, intervalDays = interval, dueKey = addDays(todayKey, interval))
    }

    /** نمره‌ی تسلط ۰..۱۰۰ برای نمایش (پیشرفت فاصله‌ها + پایداری). */
    fun mastery(state: CardState): Int {
        // کارت نو حتی با ease پیش‌فرض تسلط ندارد؛ تسلط فقط با مرور واقعی ساخته می‌شود.
        if (state.reps == 0 && state.intervalDays == 0) return 0
        val repsPart = (state.reps.coerceAtMost(8).toDouble() / 8.0) * 0.30
        val intervalPart = (state.intervalDays.coerceAtMost(60).toDouble() / 60.0) * 0.50
        val easePart = ((state.ease - MIN_EASE) / (2.5 - MIN_EASE)).coerceIn(0.0, 1.0) * 0.20
        return ((repsPart + intervalPart + easePart) * 100).toInt().coerceIn(0, 100)
    }

    /** آیا کارت امروز سررسید است؟ (کارتِ نو هم برای اولین یادگیری سررسید است) */
    fun isDue(state: CardState, todayKey: String): Boolean =
        state.dueKey.isBlank() || state.dueKey <= todayKey

    fun addDays(todayKey: String, days: Int): String {
        val parts = todayKey.split("-").map { it.toIntOrNull() ?: 0 }
        if (parts.size != 3) return todayKey
        val cal = java.util.Calendar.getInstance().apply {
            clear()
            set(parts[0], parts[1] - 1, parts[2], 12, 0, 0)
            add(java.util.Calendar.DAY_OF_YEAR, days)
        }
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
    }
}
