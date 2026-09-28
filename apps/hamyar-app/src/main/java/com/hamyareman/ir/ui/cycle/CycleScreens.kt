package com.hamyareman.ir.ui.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.LocalAppContainer

private val faces = listOf("😔", "😕", "😐", "🙂", "😄")

@Composable
fun MoodCheckInScreen(onBack: () -> Unit) {
    val store = LocalAppContainer.current.store
    var level by remember { mutableFloatStateOf(store.getInt("mood_today", 3).toFloat()) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("امروز حالت چطوره؟", onBack)
        Column(Modifier.padding(16.dp)) {
            Text(faces[level.toInt().coerceIn(0, 4)], style = MaterialTheme.typography.displayLarge)
            Slider(value = level, onValueChange = { level = it }, valueRange = 0f..4f, steps = 3)
            Text("ثبت شدنی و اختیاریه. هیچ برچسبی نمی‌ذاریم.")
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ثبت") { store.putInt("mood_today", level.toInt()); onBack() }
        }
    }
}

@Composable
fun MindfulnessScreen(onBack: () -> Unit) {
    var seconds by remember { mutableIntStateOf(20) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("تنفس ۴-۷-۸", onBack)
        Column(Modifier.padding(16.dp)) {
            Text("دم ۴، نگه ۷، بازدم ۸. اگه دوست نداشتی بی‌خیال.")
            Spacer(Modifier.height(16.dp))
            Text("$seconds ثانیه", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { }
            PrimaryButton("شروع یک دور") { seconds = 19 }
            Spacer(Modifier.height(8.dp))
            PrimaryButton("بازگشت", onBack)
        }
    }
}
