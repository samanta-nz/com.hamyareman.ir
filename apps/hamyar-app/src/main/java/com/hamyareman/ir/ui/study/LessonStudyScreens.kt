package com.hamyareman.ir.ui.study

import androidx.compose.runtime.Composable

@Composable
fun LessonStudyScreen(packId: String, onBack: () -> Unit, onOpenPdf: (String) -> Unit = {}) {
    RemoteLessonScreen(packId, 1, onBack)
}
