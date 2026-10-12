package com.hamyareman.ir.platform.feature.study

data class BookModule(val bookCode: String, val title: String, val subject: String, val packs: List<StudyPack>)

/** Navigation metadata only. Lesson bodies, cards, questions and solutions live in the bucket. */
object BookModuleRegistry {
    private fun flatten(nodes: List<BookToc.TocNode>): List<BookToc.TocNode> = nodes.flatMap { listOf(it) + flatten(it.children) }
    private fun module(code: String, title: String): BookModule = BookModule(code, title, title,
        flatten(BookToc.forBook(code)).mapNotNull { node -> node.packId?.let { id ->
            StudyPack(packId = id, bookCode = code, lessonId = id.substringAfter('_'),
                title = BookToc.packTitle(id) ?: node.title, bookTitle = title,
                pdfFileName = id + "_BOOK.pdf", audioFileId = id + "_AUDIO.mp3",
                sections = emptyList(), flashcards = emptyList(), questions = emptyList(), solutions = emptyList())
        } }.distinctBy { it.packId })
    val modules: List<BookModule> = listOf(
        module("C909", "عربی نهم"),
        module("C908", "فرهنگ و هنر نهم"),
        module("C915", "آمادگی دفاعی نهم"),
        module("C910", "انگلیسی نهم"),
        module("C911", "کتاب کار انگلیسی نهم"),
        module("C903", "فارسی نهم"),
        module("C941", "هدیه‌های آسمان نهم"),
        module("C917", "کار و فناوری نهم"),
        module("C905", "ریاضی نهم"),
        module("C904", "نگارش نهم"),
        module("C902", "پیام‌های آسمان نهم"),
        module("C901", "آموزش قرآن نهم"),
        module("C906", "علوم تجربی نهم"),
        module("C907", "مطالعات اجتماعی نهم")
    )
    fun pack(packId: String): StudyPack? = modules.asSequence().flatMap { it.packs }.firstOrNull { it.packId == packId }
    fun books(): List<BookModule> = modules
}
