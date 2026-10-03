package com.hamyareman.ir.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.art.readGallery
import com.hamyareman.ir.ui.exercise.exerciseMinutesOn
import java.time.LocalDate
import kotlin.math.roundToInt

/** وقتی مطالعه قفل است — هیچ ورودی دیگری به فلش‌کارت/آزمون راه ندارد. */
@Composable
fun LockedStudyScreen(onBack: () -> Unit) {
    AppTopBar(title = "قفل است 🔒", onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("اول تدریس، بعد تمرین 🌱", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            "«مطالعه و آزمون» بعد از اتمام دوره‌ی اول تدریسِ همان درس باز می‌شود.\nصوت یا ویدیوی تدریس را تا انتها ببین؛ خودکار باز می‌شود.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("متوجه شدم", onClick = onBack)
    }
}

@Composable
fun NeedSubScreen(onBack: () -> Unit) {
    AppTopBar(title = "نیاز به اشتراک", onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("این درس با اشتراک فعال باز می‌شود", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            "فصل ۱ ریاضی و اولین درس هر کتاب دیگر بدون اشتراک باز است. بقیهٔ درس‌ها با اشتراک فعال باز می‌شوند.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("متوجه شدم", onClick = onBack)
    }
}

private fun fa(n: Int) = toPersianDigits(n.toString())

@Composable
fun HealthProgressScreen(onBack: () -> Unit) {
    val daily = LocalAppContainer.current.dailyHealth
    var days by remember { mutableStateOf(emptyList<com.hamyareman.ir.ui.hub.DailyHealthSnapshot>()) }
    var selected by remember { mutableIntStateOf(6) }
    var detailMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val base = java.time.LocalDate.parse(JalaliDate.todayIso())
        days = (0..6).map { i ->
            daily.pull(base.minusDays((6 - i).toLong()).toString())
        }
    }

    val selectedSnapshot = days.getOrNull(selected)
        ?: com.hamyareman.ir.ui.hub.DailyHealthSnapshot(dayIso = JalaliDate.todayIso())
    val weekScore = if (days.isEmpty()) 0 else days.map(::healthScore).average().roundToInt()

    AppTopBar(title = "۷ روز اخیر سلامتی", onBack = onBack)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نمای هفته", style = MaterialTheme.typography.titleLarge)
                        Text(
                            JalaliDate.weekDayFa(selectedSnapshot.dayIso) + " · " + selectedSnapshot.dayIso,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(fa(weekScore) + "٪", style = MaterialTheme.typography.displaySmall)
                }
                LinearProgressIndicator(
                    progress = { (weekScore / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Text(
                    "آب، تحرک و روتین در امتیاز روز اثر دارند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEachIndexed { index, day ->
                FilterChip(
                    selected = selected == index,
                    onClick = { selected = index },
                    label = { Text(JalaliDate.weekDayFa(day.dayIso).take(3)) },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("جزئیات روز", style = MaterialTheme.typography.titleMedium)
                val waterGoal = selectedSnapshot.waterGoal.coerceAtLeast(1)
                metric("💧", "آب", fa(selectedSnapshot.waterConsumed) + " / " + fa(waterGoal), "لیوان",
                    (selectedSnapshot.waterConsumed.toFloat() / waterGoal).coerceIn(0f, 1f))
                metric("🏃", "تحرک", fa(selectedSnapshot.sportsMinutes), "دقیقه",
                    (selectedSnapshot.sportsMinutes / 45f).coerceIn(0f, 1f))
                val routinePct = if (selectedSnapshot.routineTotal == 0) 0f else
                    (selectedSnapshot.routineDone.toFloat() / selectedSnapshot.routineTotal).coerceIn(0f, 1f)
                metric("✅", "روتین", fa(selectedSnapshot.routineDone) + " / " + fa(selectedSnapshot.routineTotal), "انجام",
                    routinePct)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    smallMetric("🧘", selectedSnapshot.yogaMinutes, "یوگا", Modifier.weight(1f))
                    smallMetric("⚡", selectedSnapshot.exerciseMinutes, "ورزش", Modifier.weight(1f))
                    smallMetric("🌿", selectedSnapshot.wellnessMinutes, "آرامش", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = detailMode,
                        onClick = { detailMode = !detailMode },
                        label = { Text(if (detailMode) "بستن جزئیات" else "فعالیت‌های ثبت‌شده") },
                    )
                    FilterChip(
                        selected = selectedSnapshot.lightDay,
                        onClick = {},
                        label = { Text(if (selectedSnapshot.lightDay) "روز سبک" else "روز معمولی") },
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("روند ۷ روزه", style = MaterialTheme.typography.titleMedium)
                days.forEach { day ->
                    val score = healthScore(day)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(JalaliDate.weekDayFa(day.dayIso), Modifier.width(70.dp), style = MaterialTheme.typography.labelSmall)
                        Box(
                            Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth((score / 100f).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(fa(score) + "٪", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (detailMode) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("فعالیت‌های روز", style = MaterialTheme.typography.titleMedium)
                    if (selectedSnapshot.activities.isEmpty()) {
                        Text("فعالیتی ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        selectedSnapshot.activities.take(15).forEach { activity ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(activity.title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                if (activity.value != 0) Text(fa(activity.value), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun healthScore(day: com.hamyareman.ir.ui.hub.DailyHealthSnapshot): Int {
    val water = (day.waterConsumed.toFloat() / day.waterGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val sport = (day.sportsMinutes / 45f).coerceIn(0f, 1f)
    val routine = if (day.routineTotal == 0) 0f else
        (day.routineDone.toFloat() / day.routineTotal).coerceIn(0f, 1f)
    return ((water + sport + routine) * 100f / 3f).roundToInt()
}

@Composable
private fun metric(
    icon: String,
    title: String,
    value: String,
    suffix: String,
    progress: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon + " " + title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(4.dp))
            Text(suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(7.dp))
    }
}

@Composable
private fun smallMetric(icon: String, value: Int, title: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(9.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon)
            Text(fa(value), style = MaterialTheme.typography.titleSmall)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}
