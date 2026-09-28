package com.hamyareman.ir.platform.feature.study

/**
 * ماژول هر کتاب — معماری مصوب: هر کتاب یک فایل Kotlin با کل محتوای درس‌هایش
 * (سکشن/فلش‌کارت/سوال/حل داخل کد). فصل‌های جدید به همان فایل اضافه می‌شوند.
 */
data class BookModule(
    val bookCode: String,
    val title: String,
    val subject: String,
    val packs: List<StudyPack>,
)

/**
 * رجیستری مرکزی ماژول کتاب‌ها — [StudyPackRepository] اول اینجا را می‌گردد.
 * کتاب جدید = ماژول جدید + افزودن به این فهرست.
 *
 * ساختار نهایی: ماژول‌های کامل (محتوای تعاملی) + درس‌های ۲ به بعد همه‌ی
 * کتاب‌ها از [ExtraLessons] (PDF واقعی از باکت؛ محتوای تعاملی به‌مرور بازسازی
 * و به همان ماژول اصلی اضافه می‌شود — پس اول ماژول کامل، بعد اسکلت‌ها).
 */
object BookModuleRegistry {

    /** ماژول‌های کامل — فقط محتوای واقعی authored. */
    private val authored: List<BookModule> = listOf(
        com.hamyareman.ir.platform.feature.study.books.MathC905.module,
        com.hamyareman.ir.platform.feature.study.books.QuranC901.module,
        com.hamyareman.ir.platform.feature.study.books.EslamiC902.module,
        com.hamyareman.ir.platform.feature.study.books.FarsiC903.module,
        com.hamyareman.ir.platform.feature.study.books.NegarC904.module,
        com.hamyareman.ir.platform.feature.study.books.ScienceC906.module,
        com.hamyareman.ir.platform.feature.study.books.EjtemaiC907.module,
        com.hamyareman.ir.platform.feature.study.books.HonarC908.module,
        com.hamyareman.ir.platform.feature.study.books.ArabicC909.module,
        com.hamyareman.ir.platform.feature.study.books.EnglishC910.module,
        com.hamyareman.ir.platform.feature.study.books.EnglishWbC911.module,
        com.hamyareman.ir.platform.feature.study.books.KarC917.module,
        com.hamyareman.ir.platform.feature.study.books.TafakkorC941.module,
        com.hamyareman.ir.platform.feature.study.books.EdafaiC915.module,
    )

    val modules: List<BookModule> =
        authored.map { m ->
            m.copy(
                // عنوان پک‌ها از فهرست رسمی کتاب (BookToc) می‌آید — v1.10؛
                // اگر پک در فهرست نبود عنوان قبلی‌اش می‌ماند.
                packs = (m.packs + ExtraLessons.extrasFor(m).filter { e -> m.packs.none { it.packId == e.packId } }).map { p ->
                    BookToc.packTitle(p.packId)?.let { t -> p.copy(title = t) } ?: p
                },
            )
        }

    fun pack(packId: String): StudyPack? =
        modules.asSequence().flatMap { it.packs }.firstOrNull { it.packId == packId }

    fun books(): List<BookModule> = modules
}
