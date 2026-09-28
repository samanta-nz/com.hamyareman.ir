package com.hamyareman.ir.platform.feature.study

/** چرخش تصحیحی (درجه) برای PDFهایی که 180° آپلود شده‌اند؛ به‌کاررفته هنگام رندر. */
object PdfRotations {
    val degrees: Map<String, Int> = mapOf(
        "C905_E02-L01_BOOK.pdf" to 180,
        "C905_E03-L05_BOOK.pdf" to 180,
        "C905_E04-L04_BOOK.pdf" to 180,
        "C905_E07-L03_BOOK.pdf" to 180,
        "C905_E08-L01_BOOK.pdf" to 180,
    )
}
