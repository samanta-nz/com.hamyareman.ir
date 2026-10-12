package com.hamyareman.ir.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry

@Composable
internal fun RemoteLessonScreen(packId: String, initialTab: Int = 0, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    val context = LocalContext.current
    if (pack != null && LessonAccess.gate(context, pack.bookCode, packId) == LessonAccess.Gate.NeedSub) {
        NeedSubScreen(onBack = onBack)
        return
    }
    val baseTabs = if (packId.endsWith("SUM")) listOf("تدریس" to "teach", "فلش‌کارت" to "flash", "خلاصه" to "summary", "آزمون" to "exam")
        else listOf("تدریس" to "teach", "تمرینات کتابی" to "book", "خلاصه و نکات" to "summary", "فلش‌کارت" to "flash", "آزمون" to "exam", "حل تشریحی" to "solutions")
    val tabs = baseTabs + listOf("کتاب درسی" to "pdf")
    var selected by remember(packId) { mutableIntStateOf(initialTab.coerceIn(0, tabs.lastIndex)) }
    Column(Modifier.fillMaxSize()) {
        if (pack != null) TeachAudioBar(packId, pack.title, pack.bookTitle, teachTracksOf(pack))
        ScrollableTabRow(selectedTabIndex = selected) {
            tabs.forEachIndexed { index, tab ->
                Tab(selected = selected == index, onClick = { selected = index }, text = { Text(tab.first) })
            }
        }
        key(packId, selected) {
            if (tabs[selected].second == "pdf") {
                val fileId = pack?.pdfFileName ?: packId + "_BOOK.pdf"
                RemotePdfPage(com.hamyareman.ir.ui.content.ContentCatalog.keyFor(fileId) ?: "Bucket/Pdf-files/app/" + fileId)
            } else RemoteHtmlPage(BucketRoutes.lesson(packId, tabs[selected].second), pack?.audioFileId.orEmpty())
        }
    }
}
