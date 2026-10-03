package com.hamyareman.ir.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.delay

@Composable
fun ExerciseDetailScreen(exerciseId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    // اول از کاتالوگ داخلی (آفلاین/فوری)، بعد از سرور یا کش.
    var exercise by remember(exerciseId) { mutableStateOf(ExerciseCatalog.byId(exerciseId)) }
    LaunchedEffect(exerciseId) {
        val loaded = container.catalog.exercise(exerciseId)
        // فقط وقتی نسخه‌ی داخلی نداریم جایگزین می‌کنیم؛ وگرنه وسط تایمر
        // تعداد گام‌ها عوض می‌شود و شمارنده به هم می‌ریزد.
        if (loaded != null && ExerciseCatalog.byId(exerciseId) == null) exercise = loaded
    }
    val current = exercise
    if (current == null) {
        Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("این حرکت پیدا نشد.")
            TextButton(onClick = onBack) { Text("بازگشت") }
        }
        return
    }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var running by rememberSaveable { mutableStateOf(false) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(current.steps.first().seconds) }
    LaunchedEffect(running) {
        if (running) {
            while (running && secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            if (running && secondsLeft <= 0) {
                if (stepIndex < current.steps.lastIndex) {
                    stepIndex += 1
                    secondsLeft = current.steps[stepIndex].seconds
                } else {
                    running = false
                }
            }
        }
    }
    val currentStep = current.steps[stepIndex]
    val isFinished = !running && stepIndex == current.steps.lastIndex && secondsLeft == 0
    // ثبت جلسه‌ی تمام‌شده (یک بار به ازای هر پایان؛ «دوباره» یعنی جلسه‌ی تازه).
    LaunchedEffect(isFinished) {
        if (isFinished) recordExerciseSession(container.store, container.sync, current, container.dailyHealth)
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
            Spacer(Modifier.weight(1f))
            Text("${current.category.emoji} ${current.totalDurationMinutes} دقیقه", color = MaterialTheme.colorScheme.primary)
        }
        Text(current.title, style = MaterialTheme.typography.headlineMedium)
        Text(current.summary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        if (isFinished) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉 آفرین! جلسه‌ی تمرین تمام شد.", style = MaterialTheme.typography.titleMedium)
                    Text("حالا کمی آب بنوش و استراحت کن.", textAlign = TextAlign.Center)
                    Button(onClick = { stepIndex = 0; secondsLeft = current.steps.first().seconds; running = true }) { Text("دوباره") }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("قدم ${stepIndex + 1} از ${current.steps.size}", color = MaterialTheme.colorScheme.primary)
                    Text(currentStep.title, style = MaterialTheme.typography.titleMedium)
                    val progress = (currentStep.seconds - secondsLeft).toFloat() / currentStep.seconds.toFloat()
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("%d:%02d".format(secondsLeft / 60, secondsLeft % 60), style = MaterialTheme.typography.headlineMedium)
                        TextButton(onClick = { running = !running }) { Text(if (running) "توقف" else "شروع") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("نکته: ${current.tips}")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { running = !running }, modifier = Modifier.fillMaxWidth()) {
                Text(if (running) "توقف جلسه" else "شروع جلسه")
            }
        }
    }
}
