package com.hamyareman.ir.platform.feature.study

import org.json.JSONArray
import org.json.JSONObject

/**
 * «پک مطالعه» — کل محتوای تفکیک‌شده‌ی یک درس در یک JSON آفلاین:
 * بخش‌های مهم/نکات/نکات امتحانی، فلش‌کارت‌ها، سوالات نمونه با جواب و توضیح، و حل کتاب.
 * منبع داده: assets اپ (آفلاین کامل) — به‌روزرسانی محتوایی بعدها از سرور هم می‌آید.
 */
data class StudyPack(
    val packId: String,
    val bookCode: String,
    val lessonId: String,
    val title: String,
    val bookTitle: String,
    val pdfFileName: String,
    val sections: List<Section>,
    val flashcards: List<Flashcard>,
    val questions: List<Question>,
    val solutions: List<Solution>,
    /** شناسه‌ی فایل صوتی روخوانی درس روی سرور (باکت) — خالی یعنی صوت ندارد. */
    val audioFileId: String = "",
    /** بخش دوم صوت همان درس (حکایت/شعرخوانیِ داخل درس) — خالی یعنی ندارد. */
    val audio2FileId: String = "",
    val audio2Title: String = "",
    /** متن تدریس برای نمایش در سربرگ ۱ (ریاضی). خالی = از سکشن‌ها ساخته می‌شود. */
    val teachText: String = "",
    /** HTML تدریس (متن + SVG از پوشهٔ Books). اگر پر باشد در WebView نشان داده می‌شود. */
    val teachHtml: String = "",
    /** متن تدریس برای تبدیل به صوت (TTS بعدی). */
    val teachSpeech: String = "",
    /** خلاصه‌ی چندسطری درس (سربرگ ۴). */
    val summary: String = "",
    /** نکات امتحانی (سربرگ ۴). */
    val examTips: String = "",
    /** تمرین‌های کتاب با جای خالی (سربرگ ۲، بعد از PDF). */
    val exercises: List<Exercise> = emptyList(),
    /** فقط PDF بدون سربرگ (کارت فهرست کتاب). */
    val pdfOnly: Boolean = false,
) {
    data class Section(val id: String, val title: String, val kind: String, val body: String, val images: List<String> = emptyList())
    data class Flashcard(val id: String, val front: String, val back: String, val topic: String, val hint: String)
    data class Question(
        val id: String,
        val type: String,            // mcq | numeric | short
        val text: String,
        val options: List<String>,   // فقط mcq
        val answer: String,
        val explanation: String,
        val topic: String,
        val difficulty: Int,         // 1..3
        val refSectionId: String,
    )
    data class Solution(val id: String, val title: String, val body: String)

    /**
     * تمرین کتاب — جواب مخفی است؛ دانش‌آموز با کیبورد ریاضی در جای خالی می‌نویسد.
     * [altAnswers] جواب‌های هم‌ارز (مثلاً `1/2` و `۰٫۵`).
     */
    data class Exercise(
        val id: String,
        val prompt: String,
        val answer: String,
        val altAnswers: List<String> = emptyList(),
        val hint: String = "",
        val topic: String = "",
    )

    fun sectionById(id: String): Section? = sections.firstOrNull { it.id == id }

    companion object {
        fun fromJson(raw: String): StudyPack? = runCatching {
            val o = JSONObject(raw)
            fun strArray(obj: JSONObject, key: String): List<String> {
                val a = obj.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).map { a.getString(it) }
            }
            val sections = (o.optJSONArray("sections") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val s = a.getJSONObject(it)
                    Section(s.optString("id"), s.optString("title"), s.optString("kind", "note"), s.optString("body"), strArray(s, "images"))
                }
            }
            val cards = (o.optJSONArray("flashcards") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val c = a.getJSONObject(it)
                    Flashcard(c.optString("id"), c.optString("front"), c.optString("back"), c.optString("topic"), c.optString("hint"))
                }
            }
            val questions = (o.optJSONArray("questions") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val q = a.getJSONObject(it)
                    Question(
                        id = q.optString("id"), type = q.optString("type", "mcq"), text = q.optString("text"),
                        options = strArray(q, "options"), answer = q.optString("answer"),
                        explanation = q.optString("explanation"), topic = q.optString("topic"),
                        difficulty = q.optInt("difficulty", 1), refSectionId = q.optString("refSectionId"),
                    )
                }
            }
            val solutions = (o.optJSONArray("solutions") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val s = a.getJSONObject(it)
                    Solution(s.optString("id"), s.optString("title"), s.optString("body"))
                }
            }
            val exercises = (o.optJSONArray("exercises") ?: JSONArray()).let { a ->
                (0 until a.length()).map {
                    val e = a.getJSONObject(it)
                    Exercise(
                        id = e.optString("id"),
                        prompt = e.optString("prompt"),
                        answer = e.optString("answer"),
                        altAnswers = strArray(e, "altAnswers"),
                        hint = e.optString("hint"),
                        topic = e.optString("topic"),
                    )
                }
            }
            StudyPack(
                packId = o.optString("packId"), bookCode = o.optString("bookCode"),
                lessonId = o.optString("lessonId"), title = o.optString("title"),
                bookTitle = o.optString("bookTitle"), pdfFileName = o.optString("pdfFileName"),
                sections = sections, flashcards = cards, questions = questions, solutions = solutions,
                audioFileId = o.optString("audioFileId"),
                audio2FileId = o.optString("audio2FileId"),
                audio2Title = o.optString("audio2Title"),
                teachText = o.optString("teachText"),
                teachHtml = o.optString("teachHtml"),
                teachSpeech = o.optString("teachSpeech"),
                summary = o.optString("summary"),
                examTips = o.optString("examTips"),
                exercises = exercises,
                pdfOnly = o.optBoolean("pdfOnly", false),
            )
        }.getOrNull()
    }
}
