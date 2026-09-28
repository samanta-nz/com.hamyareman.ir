package com.hamyareman.ir.ui.wellness

/**
 * پرامپت ۰۲ — مدل داده‌ی یک حرکت سلامتی.
 *
 * ساختار مطابق جدول `wellness_moves` در Appwrite. شامل فیلدهای:
 *  - [category]: yoga | exercise | breathing | learning
 *  - [slug]: شناسه‌ی پایدار (مثل `yoga-balasana`) برای ارجاع در URL و logs.
 *  - [level]: سطح مهارت ۱ تا ۱۰.
 *  - [audioCueId]: نام فایل صوتی در Storage (مثل `cue-box-breath-intro.mp3`).
 *  - [referenceImageUrl]: URL عمومی تصویر مرجع در باکت `wellness-media`.
 *  - [referenceImagePromptTemplate]: قالب پرامپت برای «تولید دوباره» با چهره‌ی کاربر.
 */
data class WellnessMove(
    val slug: String,
    val category: Category,
    val titleFa: String,
    val level: Int = 1,
    val durationSec: Int = 60,
    val reps: Int = 0,
    val instructionsFa: String = "",
    val audioCueId: String = "",
    val referenceImageUrl: String = "",
    val referenceImagePromptTemplate: String = "",
    val orderIndex: Int = 0,
    val tags: List<String> = emptyList(),
) {
    enum class Category(val wire: String, val displayFa: String) {
        YOGA("yoga", "یوگا"),
        EXERCISE("exercise", "ورزش"),
        BREATHING("breathing", "تنفس"),
        LEARNING("learning", "یادگیری");

        companion object {
            fun fromWire(s: String?): Category = entries.firstOrNull { it.wire == s } ?: EXERCISE
        }
    }

    /** مدت کل بر حسب ثانیه (اگر reps>0 باشد، reps * durationSec فرض می‌شود). */
    val totalSeconds: Int
        get() = if (reps > 0) reps * durationSec else durationSec

    /** شدت تقریبی برای فیلتر کاربر (کم/متوسط/زیاد). */
    val intensity: Intensity
        get() = when {
            level <= 3 -> Intensity.LOW
            level <= 6 -> Intensity.MEDIUM
            else -> Intensity.HIGH
        }

    /**
     * لیست فایل‌های صوتی cue. اگر `audioCueId` با `|` جدا شده باشد
     * (مثل `cue-yoga-balasana-start.mp3|cue-yoga-balasana-mid.mp3|cue-yoga-balasana-end.mp3`)،
     * چند فایل برگردانده می‌شود تا در زمان‌های مختلف (شروع/میانه/پایان) پخش شوند.
     */
    val audioCueIds: List<String>
        get() = audioCueId.split("|").map { it.trim() }.filter { it.isNotEmpty() }

    /** فایل صوتی اول (شروع) — یا تنها فایل اگر فقط یکی باشد. */
    val startCueId: String? get() = audioCueIds.firstOrNull()

    /** فایل صوتی میانه (در صورت وجود). */
    val midCueId: String? get() = audioCueIds.getOrNull(1)

    /** فایل صوتی پایان (در صورت وجود). */
    val endCueId: String? get() = audioCueIds.getOrNull(2)

    enum class Intensity { LOW, MEDIUM, HIGH }
}

/**
 * ساختار مراحل یک حرکت. هر مرحله یک شمارنده‌ی صوتی و تصویری دارد.
 *
 * نکته: این ساختار به‌صورت محلی در کدبیس است (نه در Appwrite) چون هر حرکت
 * الگوی زمانی ثابتی دارد و نیازی به ویرایش توسط ادمین نیست.
 */
data class WellnessStep(
    val title: String,
    val seconds: Int,
    /** نوع شمارنده: شمارش معکوس یا شمارش رو به جلو. */
    val counterType: CounterType = CounterType.COUNTDOWN,
    /** آیا این مرحله نقطه‌ی استراحت است (نفس). */
    val isRest: Boolean = false,
) {
    enum class CounterType { COUNTDOWN, COUNTUP, REPS }
}

/**
 * اعلان صوتی راهنما که با تایمر بصری هماهنگ پخش می‌شود.
 *
 * - [atSec]: ثانیه‌ی شروع پخش (نسبت به شروع مرحله).
 * - [textFa]: متن فارسی که با TTS تولید شده یا ضبط شده.
 * - [kind]: نوع صدا (intro/beep/breath-in/breath-out/...).
 */
data class AudioCue(
    val atSec: Int,
    val textFa: String,
    val kind: Kind = Kind.GUIDE,
) {
    enum class Kind {
        INTRO, GUIDE, BEEP, BREATH_IN, BREATH_OUT, BREATH_HOLD, COUNT, FINISH
    }
}
