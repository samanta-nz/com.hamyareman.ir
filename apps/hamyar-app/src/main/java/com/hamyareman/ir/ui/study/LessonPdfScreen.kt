package com.hamyareman.ir.ui.study

import androidx.compose.runtime.*
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.ui.content.ContentCatalog

@Composable
fun LessonPdfScreen(packId: String, onBack: () -> Unit) {
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    val fileId = pack?.pdfFileName ?: packId + "_BOOK.pdf"
    BookNodeScreen(pack?.title ?: "کتاب", ContentCatalog.keyFor(fileId) ?: "Bucket/Pdf-files/app/" + fileId,
        pack?.audioFileId.orEmpty(), onBack)
}
