package com.hamyareman.ir.ui.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamyareman.ir.platform.core.common.toPersianDigits

@Composable
fun WaterScreen(modifier: Modifier = Modifier, viewModel: WaterViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val pending by viewModel.pendingSync.collectAsState()
    val note by viewModel.note.collectAsState()

    LaunchedEffect(Unit) { viewModel.syncNow() }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("یادآور نوشیدن آب", style = MaterialTheme.typography.titleLarge)
        Text("هر روز کمی آب بنوش تا بدنت شاداب بمونه.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    toPersianDigits("${state.consumed} از ${state.goal} لیوان"),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(toPersianDigits("${state.remaining} لیوان باقی است"))
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (state.consumed.toFloat() / state.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(onClick = { viewModel.addGlass() }, modifier = Modifier.weight(1f)) {
                        Text("یک لیوان بنوشم ✨")
                    }
                    OutlinedButton(onClick = { viewModel.undoGlass() }, modifier = Modifier.weight(0.6f)) {
                        Text("کم کن")
                    }
                }
            }
        }
        if (state.isGoalReached) {
            Spacer(Modifier.height(16.dp))
            Text("🎉 آفرین! به هدف امروز رسیدی.", style = MaterialTheme.typography.titleMedium)
        }
        if (pending > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                toPersianDigits("$pending قلم در صف همگام‌سازی است."),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        note?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.labelSmall)
        }
    }
}
