package com.hamyareman.ir.ui.gamification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.SectionCard

@Composable
fun BadgesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("امتیاز و بج", onBack)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionCard("🏅 پنج روز پشت‌سرهم به هدفت رسیدی", "فقط بج مثبت. از دست رفتن استریک پیام منفی ندارد.") { }
            SectionCard("مبتدی → آشنا", "موضوع: تنفس") { }
        }
    }
}
