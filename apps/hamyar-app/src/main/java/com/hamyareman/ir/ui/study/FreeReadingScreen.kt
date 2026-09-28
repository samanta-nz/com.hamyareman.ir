package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.AppTypography

@Composable
fun FreeReadingScreen(nav: NavController, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("مطالعه آزاد", onBack)
        TabRow(selectedTabIndex = tab) {
            listOf("کتاب متنی", "کتاب صوتی").forEachIndexed { i, l ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l, style = AppTypography.pageHeading.style) })
            }
        }
        if (tab == 0) LibraryScreen(onBack = onBack)
        else AudiobookScreen(onBack = onBack)
    }
}
