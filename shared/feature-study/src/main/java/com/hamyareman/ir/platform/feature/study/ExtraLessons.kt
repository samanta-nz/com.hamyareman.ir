package com.hamyareman.ir.platform.feature.study

/**
 * درس‌های ۲ به بعد همه‌ی کتاب‌ها (v1.7) — ثبت بر اساس فهرست واقعی باکت:
 * هر درس PDF کتابش را دارد (نام‌ها یک‌به‌یک با فایل‌های سرور چک شده) و
 * صفحه‌ی «تدریس» همان لحظه کار می‌کند (کتاب + صوت به‌محض آپلودشدن).
 *
 * v1.9: عنوان هر واحد از خودِ PDF/ساختار رسمی کتاب خوانده شده (LessonTitles) —
 * فارسی هر درس، حکایت، شعرخوانی و روان‌خوانی یک ردیف جدا با صوت مخصوص خودش است.
 *
 * محتوای تعاملی (فلش‌کارت/آزمون/حل) نوبت‌به‌نوبت از PDF بازسازی و به پکِ همان
 * درس اضافه می‌شود — تا آن موقع تب‌ها پیام صادقانه‌ی «به‌زودی» می‌دهند.
 * صوت‌ها با قرارداد شناخته‌شده‌ی `<packId-خط‌دار>-AUDIO.mp3` خوانده می‌شوند؛
 * به‌محض آپلود در باکت، بدون آپدیت اپ روشن می‌شوند.
 */
object ExtraLessons {

    /** واحد شمارش هر کتاب، فقط برای عنوان‌های عمومی (بدون عنوان از PDF). */
    private val unitWords: Map<String, String> = mapOf("C906" to "فصل", "C917" to "پودمان")

    private val lessonIds: Map<String, List<String>> = mapOf(
        "C901" to (2..11).map { "L%02d".format(it) },
        "C902" to listOf(
            "E01-L02", "E02-L01", "E02-L02", "E02-L03", "E03-L01", "E03-L02",
            "E04-L01", "E05-L01", "E05-L02", "E05-L03", "E05-L04",
        ),
        "C903" to listOf(
            "E01-L02", "E01-L03", "E01-L04",
            "E02-L01", "E02-L02", "E02-L03", "E02-L04",
            "E03-L01", "E03-L02", "E03-L03",
            "E04-L01", "E04-L02", "E04-L03", "E04-L04", "E04-L05",
            "E05-L01", "E05-L02", "E05-L03", "E05-L04", "E05-L05",
            "E06-L01", "E06-L02", "E06-L03", "E06-L04", "E06-L05",
            "E07-L01", "E07-L02",
            "E08-L01", "E08-L02", "E08-L03",
        ),
        "C904" to (2..8).map { "L%02d".format(it) },
        "C905" to listOf(
            "E01-L01", "E01-L02", "E01-L03", "E01-L04",
            "E02-L01", "E02-L02", "E02-L03",
            "E03-L01", "E03-L02", "E03-L03", "E03-L04", "E03-L05",
            "E04-L01", "E04-L02", "E04-L03", "E04-L04",
            "E05-L01", "E05-L02", "E05-L03",
            "E06-L01", "E06-L02", "E06-L03",
            "E07-L01", "E07-L02", "E07-L03",
            "E08-L01", "E08-L02", "E08-L03",
        ),
        "C906" to (2..15).map { "E%02d-L01".format(it) },
        "C907" to listOf(
            "E01-L02",
            "E02-L01", "E02-L02",
            "E03-L01", "E03-L02",
            "E04-L01", "E04-L02",
            "E05-L01", "E05-L02",
            "E06-L01", "E06-L02",
            "E07-L01", "E07-L02",
            "E08-L01", "E08-L02",
            "E09-L01", "E09-L02",
            "E10-L01", "E10-L02",
            "E11-L01", "E11-L02",
            "E12-L01", "E12-L02",
        ),
        "C908" to listOf(
            "E01-L02", "E01-L03", "E01-L04",
            "E02-L01", "E02-L02", "E02-L03",
            "E03-L01", "E03-L02", "E03-L03",
            "E04-L01", "E04-L02", "E04-L03", "E04-L04",
            "E05-L01", "E05-L02", "E05-L03",
            "E06-L01", "E06-L02", "E06-L03",
            "E07-L01", "E07-L02", "E07-L03",
            "E08-L01", "E08-L02", "E08-L03", "E08-L04", "E08-L05",
            "E09-L01", "E09-L02", "E09-L03",
        ),
        "C909" to (2..10).map { "L%02d".format(it) },
        "C910" to (2..9).map { "L%02d".format(it) },
        "C911" to (2..6).map { "L%02d".format(it) },
        "C917" to listOf("E01-L02", "E01-L03", "E01-L04", "E01-L05", "E02-L01", "E02-L02", "E02-L03", "E02-L04", "E02-L05", "E02-L06"),
        "C915" to listOf(
            "E01-L01", "E01-L02",
            "E02-L01", "E02-L02", "E02-L03", "E02-L04", "E02-L05",
            "E03-L01", "E03-L02", "E03-L03", "E03-L04",
        ),
        "C941" to (2..12).map { "L%02d".format(it) },
    )

    /** آیتم‌های زیرِ فارسی (حکایت/شعرخوانی/روان‌خوانی) — عنوان مستقل، بدون شماره. */
    private val subItemPrefixes = listOf("حکایت", "شعرخوانی", "روان‌خوانی")

    /**
     * صوت‌هایی که نامشان از قرارداد پیروی نمی‌کند — صوتِ «حکایت سفر» در باکت
     * به‌صورت بخش ۲ صوت درس ۱ فارسی آپلود شده است.
     */
    private val audioOverride: Map<String, String> = mapOf(
        "C903_E01-L02" to "C903_E01-L01-2_AUDIO.mp3",
    )

    /** عنوان نمایشی: از PDF اگر هست؛ وگرنه «درس/فصل/پودمان N». */
    fun displayTitle(bookCode: String, lessonId: String, fallbackNumber: Int): String {
        val packId = "${bookCode}_$lessonId"
        val fromPdf = LessonTitles.titles[packId]
        if (fromPdf != null) {
            val isSub = subItemPrefixes.any { fromPdf.startsWith(it) }
            val hasUnit = listOf("درس", "فصل", "پودمان", "Lesson", "مرور").any { fromPdf.startsWith(it) }
            return when {
                isSub || hasUnit -> fromPdf
                else -> "${unitWords[bookCode] ?: "درس"} ${com.hamyareman.ir.platform.core.common.toPersianDigits(fallbackNumber.toString())} - $fromPdf"
            }
        }
        val unit = unitWords[bookCode] ?: "درس"
        return "$unit ${com.hamyareman.ir.platform.core.common.toPersianDigits(fallbackNumber.toString())}"
    }

    /** کارتِ فقط-PDF بدون سربرگ (فهرست کتاب / ستایش / پیوست‌ها). */
    private fun pdfOnlyPack(module: BookModule, lessonId: String, title: String, pdfFileName: String): StudyPack =
        StudyPack(
            packId = "${module.bookCode}_$lessonId",
            bookCode = module.bookCode,
            lessonId = lessonId,
            title = title,
            bookTitle = module.title,
            pdfFileName = pdfFileName,
            sections = emptyList(),
            flashcards = emptyList(),
            questions = emptyList(),
            solutions = emptyList(),
            pdfOnly = true,
        )

    /** فهرست + پیوست‌های فارسی — همه مثل «فهرست» ریاضی فقط PDF می‌گیرند. */
    private fun frontMatter(module: BookModule): List<StudyPack> {
        val out = mutableListOf<StudyPack>()
        if (module.bookCode != "C905") {
            out += pdfOnlyPack(module, "TOC", "فهرست", "${module.bookCode}-fehrest.pdf")
        }
        if (module.bookCode == "C903") {
            out += pdfOnlyPack(module, "SETAYESH", "ستایش", "C903-setayesh.pdf")
            out += pdfOnlyPack(module, "NIYAYESH", "نیایش", "C903-niyayesh.pdf")
            out += pdfOnlyPack(module, "VAJEH", "واژه‌نامه", "C903-vajeh.pdf")
            out += pdfOnlyPack(module, "AALAM", "اعلام", "C903-aalam.pdf")
            out += pdfOnlyPack(module, "KETABNAMEH", "کتاب‌نامه", "C903-ketabnameh.pdf")
        }
        return out
    }

    /** درس‌هایِ هنوز ثبت‌نشده‌ی یک ماژول (PDF واقعی، بدون محتوای تعاملی تا بازسازی). */
    private fun mathSkeletonSections(): List<StudyPack.Section> = listOf(
        StudyPack.Section("teach", "متن تدریس", "concept", "ساختار تدریس این درس آماده است؛ متن کامل به‌زودی اضافه می‌شود."),
        StudyPack.Section("summary", "خلاصه درس", "note", "خلاصه‌ی چندسطری به‌زودی."),
        StudyPack.Section("exam", "نکات امتحانی", "exam", "نکات امتحانی به‌زودی."),
    )

    fun extrasFor(module: BookModule): List<StudyPack> {
        val front = frontMatter(module)
        val ids = lessonIds[module.bookCode] ?: return front
        val packs = ids.mapIndexed { i, lessonId ->
            val packId = "${module.bookCode}_$lessonId"
            val n = i + if (ids.any { it == "E01-L01" || it == "L01" }) 1 else 2
            val skeleton = module.bookCode == "C905"
            StudyPack(
                packId = packId,
                bookCode = module.bookCode,
                lessonId = lessonId,
                title = displayTitle(module.bookCode, lessonId, n),
                bookTitle = module.title,
                pdfFileName = c905PdfName(packId) ?: "${packId}_BOOK.pdf",
                sections = if (skeleton) mathSkeletonSections() else emptyList(),
                flashcards = emptyList(),
                questions = emptyList(),
                solutions = emptyList(),
                audioFileId = audioOverride[packId] ?: c905AudioName(packId) ?: "${packId}_AUDIO.mp3",
                teachText = if (skeleton) "متن تدریس این درس به‌زودی اضافه می‌شود." else "",
                teachSpeech = if (skeleton) "متن تدریس این درس به‌زودی اضافه می‌شود." else "",
                summary = if (skeleton) "خلاصه‌ی چندسطری این درس به‌زودی نوشته می‌شود." else "",
                examTips = if (skeleton) "نکات امتحانی این درس به‌زودی اضافه می‌شود." else "",
                exercises = emptyList(),
            )
        }
        if (module.bookCode != "C905") return front + packs
        val toc = StudyPack(
            packId = "C905_TOC",
            bookCode = "C905",
            lessonId = "TOC",
            title = "فهرست",
            bookTitle = module.title,
            pdfFileName = "C905-fehrest.pdf",
            sections = emptyList(),
            flashcards = emptyList(),
            questions = emptyList(),
            solutions = emptyList(),
            pdfOnly = true,
        )
        val byChapter = packs.groupBy { it.lessonId.substringBefore("-") }
        val ordered = mutableListOf<StudyPack>()
        ordered += toc
        byChapter.keys.sorted().forEach { ch ->
            ordered += byChapter.getValue(ch)
            val n = ch.removePrefix("E").toIntOrNull() ?: 0
            ordered += StudyPack(
                packId = "C905_${ch}-SUM",
                bookCode = "C905",
                lessonId = "$ch-SUM",
                title = "جمع‌بندی فصل ${com.hamyareman.ir.platform.core.common.toPersianDigits(n.toString())}",
                bookTitle = module.title,
                pdfFileName = "",
                sections = mathSkeletonSections(),
                flashcards = emptyList(),
                questions = emptyList(),
                solutions = emptyList(),
                audioFileId = "",
                teachText = "متن جمع‌بندی این فصل به‌زودی اضافه می‌شود.",
                teachSpeech = "متن جمع‌بندی این فصل به‌زودی اضافه می‌شود.",
                summary = "خلاصه‌ی فصل به‌زودی.",
                examTips = "نکات امتحانی فصل به‌زودی.",
            )
        }
        return ordered.map { com.hamyareman.ir.platform.feature.study.books.MathC905Content.applyTo(it) }
    }

    /** PDF ریاضی نهم: `C905f01d01.pdf` مطابق `Books/Base-09/ریاضی/01-متن کتاب`. */
    fun c905PdfName(packId: String): String? {
        if (packId == "C905_TOC") return "C905-fehrest.pdf"
        Regex("""^C905_E(\d+)-L(\d+)$""").find(packId)?.let { m ->
            return "C905f%02dd%02d.pdf".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        return null
    }

    /**
     * صوت ریاضی نهم: `ryazif01d01.mp3` مطابق `Books/Base-09/ریاضی/04- صوت تدریس`.
     * جمع‌بندیِ هر فصل هم صوت دارد: `ryazif01review.mp3`.
     */
    fun c905AudioName(packId: String): String? {
        Regex("""^C905_E(\d+)-L(\d+)$""").find(packId)?.let { m ->
            return "ryazif%02dd%02d.mp3".format(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        Regex("""^C905_E(\d+)-SUM$""").find(packId)?.let { m ->
            return "ryazif%02dreview.mp3".format(m.groupValues[1].toInt())
        }
        return null
    }
}
