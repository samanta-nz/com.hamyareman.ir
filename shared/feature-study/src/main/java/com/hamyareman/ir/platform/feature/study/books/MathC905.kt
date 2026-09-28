package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.BookModule
import com.hamyareman.ir.platform.feature.study.StudyPack

/**
 * ریاضی نهم (C905) — فقط هویت کتاب.
 * متن/تمرین/فلش/آزمون/صوت از پوشهٔ `Books` و باکت می‌آیند؛ اینجا محتوای تألیفی نیست.
 * اسکلت درس‌ها از [com.hamyareman.ir.platform.feature.study.ExtraLessons] ساخته می‌شود.
 */
object MathC905 {
    val packs: List<StudyPack> = emptyList()
    val module = BookModule(
        bookCode = "C905",
        title = "ریاضی پایه نهم",
        subject = "ریاضی",
        packs = packs,
    )
}
