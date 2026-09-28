package com.hamyareman.ir.ui.content

/** دستور آشپزی ساده و قابل‌انجام برای نوجوان. */
data class Recipe(
    val id: String,
    val title: String,
    val minutes: Int,
    val servings: Int,
    val difficulty: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val tip: String,
)

/** یک درس کوتاه مهارتی یا درسی. */
data class Lesson(
    val id: String,
    val title: String,
    val subject: String,
    val grade: Int,
    val body: String,
    /** پرامپت ۰۱: کد کتاب برای سینک پیشرفت (مثل C905). */
    val bookCode: String = "",
    /** پرامپت ۰۱: شماره‌ی درس در کتاب. */
    val lessonNumber: Int = 0,
    /** پرامپت ۰۱: آدرس ویدیوی آموزشی (MP4). خالی = بدون ویدیو. */
    val videoUrl: String = "",
    /** پرامپت ۰۱: آدرس صوتی آموزشی (MP3). خالی = بدون صوت. */
    val audioUrl: String = "",
    /** پرامپت ۰۱: نشانه‌های فصل (به ثانیه). */
    val chapterMarkers: List<Double> = emptyList(),
)

/** سؤال چهارگزینه‌ای. */
data class QuizQuestion(
    val id: String,
    val lessonId: String,
    val question: String,
    val choices: List<String>,
    val answerIndex: Int,
)

/** گره‌ی نقشه‌ی راه آموزش آزاد (با پیش‌نیاز). */
data class LearningNode(
    val id: String,
    val title: String,
    val track: String,
    val orderIndex: Int,
    val prerequisiteId: String,
) {
    val isUnlocked: Boolean get() = prerequisiteId.isBlank()
}

/** ایده‌ی روز برای سیاه‌قلم/اسکیس. */
data class ArtPrompt(
    val id: String,
    val title: String,
    val prompt: String,
    val moodTag: String,
)
