package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.BookToc
import com.hamyareman.ir.ui.profile.StudentProfileState

/**
 * رایگان: کل فصل ۱ ریاضی؛ در بقیهٔ کتاب‌ها فقط اولین درس کتاب.
 * TOC و pdfOnly باز می‌مانند. بقیه نیاز به اشتراک فعال.
 */
object LessonAccess {

    enum class Gate { Open, NeedSub }

    fun isPremium(): Boolean = StudentProfileState.isPaid()

    fun isToc(packId: String): Boolean =
        packId.endsWith("_TOC") || packId.substringAfter('_', "") == "TOC"

    fun firstLessonPack(bookCode: String): String? =
        orderedPackIds(bookCode).firstOrNull { id ->
            !isToc(id) && BookModuleRegistry.pack(id)?.pdfOnly != true
        }

    fun isAlwaysOpen(packId: String): Boolean {
        if (isToc(packId)) return true
        val pack = BookModuleRegistry.pack(packId)
        if (pack?.pdfOnly == true) return true
        if (packId.startsWith("C905_E01")) return true
        if (packId.startsWith("C905_")) return false
        val book = pack?.bookCode ?: return false
        return packId == firstLessonPack(book)
    }

    fun orderedPackIds(bookCode: String): List<String> {
        fun walk(n: BookToc.TocNode): List<String> {
            val self = n.packId?.let { listOf(it) } ?: emptyList()
            return self + n.children.flatMap { walk(it) }
        }
        val fromToc = BookToc.forBook(bookCode).flatMap { walk(it) }
        if (fromToc.isNotEmpty()) return fromToc
        return BookModuleRegistry.modules.firstOrNull { it.bookCode == bookCode }?.packs?.map { it.packId }.orEmpty()
    }

    fun gate(ctx: Context, bookCode: String, packId: String): Gate {
        if (isAlwaysOpen(packId)) return Gate.Open
        if (isPremium()) return Gate.Open
        return Gate.NeedSub
    }
}
