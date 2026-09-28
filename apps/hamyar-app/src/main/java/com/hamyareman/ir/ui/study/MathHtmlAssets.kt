package com.hamyareman.ir.ui.study

/**
 * نگاشت پک ریاضی نهم به فایل HTML داخل assets/math/c905.
 * درس‌ها: فقط تمرین کتاب. جمع‌بندی فصل: فلش + نمونه سوال (بدون تمرین کتاب).
 */
internal object MathHtmlAssets {

    data class Spec(
        val isSum: Boolean,
        val chapter: Int,
        val lesson: Int,
        val bookAsset: String?,
        val flashAsset: String?,
        val examAsset: String?,
    )

    fun of(packId: String): Spec? {
        Regex("""^C905_E(\d+)-SUM$""").find(packId)?.let { m ->
            val ch = m.groupValues[1].toInt()
            return Spec(
                isSum = true,
                chapter = ch,
                lesson = 0,
                bookAsset = null,
                flashAsset = chapterFile(ch, "cards"),
                examAsset = chapterFile(ch, "soalat"),
            )
        }
        Regex("""^C905_E(\d+)-L(\d+)$""").find(packId)?.let { m ->
            val ch = m.groupValues[1].toInt()
            val les = m.groupValues[2].toInt()
            return Spec(
                isSum = false,
                chapter = ch,
                lesson = les,
                bookAsset = lessonBook(ch, les),
                flashAsset = null,
                examAsset = null,
            )
        }
        return null
    }

    fun pdfFileName(packId: String): String? {
        if (packId == "C905_TOC") return "C905-fehrest.pdf"
        Regex("""^C905_E(\d+)-L(\d+)$""").find(packId)?.let { m ->
            return "C905f%02dd%02d.pdf".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        return null
    }

    /** HTML تدریس داخل assets/math/c905 — بدون متن خلاصه/فلش/آزمون. */
    fun teachAsset(packId: String): String? {
        Regex("""^C905_E(\d+)-SUM$""").find(packId)?.let { m ->
            return "math/c905/ryazif%02dreview.html".format(m.groupValues[1].toInt())
        }
        Regex("""^C905_E(\d+)-L(\d+)$""").find(packId)?.let { m ->
            return "math/c905/ryazif%02dd%02d.html".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        return null
    }

    private fun chapterFile(ch: Int, kind: String): String =
        "math/c905/ryazif%02d-%s.html".format(ch, kind)

    private fun lessonBook(ch: Int, les: Int): String =
        "math/c905/ryazif%02dd%d-tamrinat.html".format(ch, les)
}
