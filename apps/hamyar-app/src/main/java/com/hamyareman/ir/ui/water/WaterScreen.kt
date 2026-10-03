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

    LaunchedEffect(Unit) { viewModel.syncNow() }

    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("آب بنوش 💧", style = MaterialTheme.typography.headlineSmall)
        Text(
            "هیدراتاسیون مناسب به تنظیم دمای بدن، انتقال مواد مغذی و عملکرد طبیعی بدن کمک می‌کند. بهتر است مصرف مایعات را در طول روز پخش کنی و به نشانه‌های تشنگی توجه داشته باشی.",
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
                    "ثبت آب روی دستگاه انجام می‌شود و دریافت/ارسال آن به دیتابیس در پس‌زمینه و به‌صورت خودکار انجام می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            ),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("یادآوری هیدراتاسیون", style = MaterialTheme.typography.titleSmall)
                Text(
                    "نیاز به مایعات با سن، فعالیت، آب‌وهوا و شرایط بدن تغییر می‌کند. این هدف‌ها برای یادآوری و ثبت عادت روزانه‌اند و جای توصیهٔ پزشکی را نمی‌گیرند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
