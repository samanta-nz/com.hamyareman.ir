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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("آب بنوش 💧", style = MaterialTheme.typography.headlineSmall)
        Text(
            "ثبت آب، هدف روزانه و آخرین رویدادها از همین کارت به سلامت امروز وصل‌اند.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            toPersianDigits(state.consumed.toString()) + " از " + toPersianDigits(state.goal.toString()),
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Text("لیوان امروز", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        if (state.isGoalReached) "هدف کامل شد 🎉" else toPersianDigits(state.remaining.toString()) + " لیوان مانده",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                LinearProgressIndicator(
                    progress = { (state.consumed.toFloat() / state.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::addGlass, modifier = Modifier.weight(1f)) {
                        Text("یک لیوان +۱")
                    }
                    OutlinedButton(onClick = viewModel::undoGlass, modifier = Modifier.weight(1f)) {
                        Text("اصلاح −۱")
                    }
                }

                Text("هدف روزانه", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(6, 8, 10, 12, 14).forEach { goal ->
                        FilterChip(
                            selected = state.goal == goal,
                            onClick = { viewModel.setGoal(goal) },
                            label = { Text(toPersianDigits(goal.toString())) },
                        )
                    }
                }

                Text(
                    "هر تغییر هم در مصرف آب و هم در سلامت روزانه ثبت می‌شود؛ سینک یعنی دریافت و ارسال با دیتابیس.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (pending > 0) {
            Text(
                toPersianDigits(pending.toString()) + " قلم در صف ارسال است.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        note?.let {
            Text(it, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = viewModel::syncNow,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("دریافت و ارسال با دیتابیس")
        }
    }
}
