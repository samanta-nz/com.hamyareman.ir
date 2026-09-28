package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.StudyPack
import org.json.JSONArray
import org.json.JSONObject

/**
 * محتوای ریاضی نهم از پوشهٔ Books (HTML/TXT/نام فایل PDF و صوت).
 * JSON روی classpath است تا هم اپ و هم تست JVM بخوانند.
 */
object MathC905Content {

    private val byId: Map<String, JSONObject> by lazy { load() }

    fun applyTo(pack: StudyPack): StudyPack {
        val derivedPdf = derivedPdf(pack.packId)
        val o = byId[pack.packId]
        if (o == null) {
            return if (derivedPdf != null) pack.copy(pdfFileName = derivedPdf) else pack
        }
        fun cards(): List<StudyPack.Flashcard> {
            val a = o.optJSONArray("flashcards") ?: return pack.flashcards
            return (0 until a.length()).map { i ->
                val c = a.getJSONObject(i)
                StudyPack.Flashcard(
                    id = c.optString("id"),
                    front = c.optString("front"),
                    back = c.optString("back"),
                    topic = c.optString("topic"),
                    hint = c.optString("hint"),
                )
            }
        }
        fun qs(): List<StudyPack.Question> {
            val a = o.optJSONArray("questions") ?: return pack.questions
            return (0 until a.length()).map { i ->
                val q = a.getJSONObject(i)
                val opts = q.optJSONArray("options")
                val options = if (opts == null) emptyList()
                else (0 until opts.length()).map { opts.getString(it) }
                StudyPack.Question(
                    id = q.optString("id"),
                    type = q.optString("type", "mcq"),
                    text = q.optString("text"),
                    options = options,
                    answer = q.optString("answer"),
                    explanation = q.optString("explanation"),
                    topic = q.optString("topic"),
                    difficulty = q.optInt("difficulty", 1),
                    refSectionId = q.optString("ref", q.optString("refSectionId")),
                )
            }
        }
        return pack.copy(
            pdfFileName = o.optString("pdfFileName").ifBlank { derivedPdf ?: pack.pdfFileName },
            audioFileId = o.optString("audioFileId").ifBlank { derivedAudio(pack.packId) ?: pack.audioFileId },
            teachText = o.optString("teachText").ifBlank { pack.teachText },
            teachHtml = o.optString("teachHtml").ifBlank { pack.teachHtml },
            teachSpeech = o.optString("teachSpeech").ifBlank { pack.teachSpeech },
            summary = o.optString("summary").ifBlank { pack.summary },
            examTips = o.optString("examTips").ifBlank { pack.examTips },
            flashcards = cards().ifEmpty { pack.flashcards },
            questions = qs().ifEmpty { pack.questions },
            pdfOnly = o.optBoolean("pdfOnly", pack.pdfOnly),
        )
    }

    private fun derivedPdf(packId: String): String? =
        com.hamyareman.ir.platform.feature.study.ExtraLessons.c905PdfName(packId)

    private fun derivedAudio(packId: String): String? =
        com.hamyareman.ir.platform.feature.study.ExtraLessons.c905AudioName(packId)

    private fun load(): Map<String, JSONObject> {
        val stream = MathC905Content::class.java.classLoader?.getResourceAsStream("math_c905.json")
            ?: return emptyMap()
        val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val a = JSONArray(raw)
        val m = LinkedHashMap<String, JSONObject>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            m[o.optString("packId")] = o
        }
        return m
    }
}
