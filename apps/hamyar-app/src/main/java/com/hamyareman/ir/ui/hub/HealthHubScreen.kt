package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.water.WaterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** هاب «سلامتی» با کارت روزانهٔ تعاملی و منوی میانبرها. */
@Composable
fun HealthHubScreen(nav: NavController) {
    val container = LocalAppContainer.current
    val daily = container.dailyHealth
    val scope = rememberCoroutineScope()
    val water = remember { WaterRepository(container.store, container.sync) }
    var snapshot by remember { mutableStateOf(daily.snapshot()) }
    var refreshing by remember { mutableStateOf(false) }

    suspend fun refreshCloud() {
        refreshing = true
        snapshot = daily.pullToday()
        daily.syncNow()
        snapshot = daily.snapshot()
        refreshing = false
    }

    fun runSync() {
        scope.launch(Dispatchers.IO) { runCatching { refreshCloud() } }
    }

    LaunchedEffect(Unit) { refreshCloud() }

    val girl = com.hamyareman.ir.ui.profile.StudentProfileState.gender != "boy"
    val tiles = buildList {
        add(HubCoverTile("hl-progress", "پیشرفت سلامتی", "آب، ورزش، روتین و فعالیت‌های روزانه", { nav.hubTo(Screen.HealthProgress.route) }))
        if (girl) {
            add(HubCoverTile("hl-period", "چرخه ماهانه", "تقویم، علائم، تنفس درد و تمرین ملایم", { nav.hubTo(Screen.PracticeGroup.of("hl-cycle")) }))
        }
        add(HubCoverTile("hl-yoga", "یوگا", "حرکات تعاملی با ثبت خودکار فعالیت", { nav.hubTo(Screen.ContentCategory.of("yoga")) }))
        add(HubCoverTile("hl-exercise", "ورزش عمومی", "تمرین‌های مرحله‌به‌مرحله با ثبت خودکار", { nav.hubTo(Screen.ContentCategory.of("sport")) }))
        add(HubCoverTile("hl-food", "آب و تغذیه", "ثبت سریع آب و هدف روزانه", { nav.hubTo(Screen.PracticeGroup.of("hl-nutrition")) }))
        add(HubCoverTile("hl-sleep", "خواب", "ثبت زمان خواب و بیداری", { nav.hubTo(Screen.PracticeGroup.of("hl-sleep")) }))
        add(HubCoverTile("hl-meds", "یادآور دارو و مراقبت", "هشدارهای زمان‌دار", { nav.hubTo(Screen.Meds.route) }))
        add(HubCoverTile("hl-routine", "روتین امروز", "افزودن، ویرایش، تیک‌زدن و سینک", { nav.hubTo(Screen.Routine.route) }))
    }

    HubBody {
        HubHeader("سلامتی 💚", "بدنت دوست توست — هر روز یک قدم مهربانی", slotId = "hub.health.header")
        DailyHealthCard(
            snapshot = snapshot,
            busy = refreshing,
            onWaterDelta = { delta ->
                if (delta > 0) water.addGlass() else water.undoGlass()
                val state = water.state()
                snapshot = daily.recordWater(state.goal, state.consumed, delta)
                runSync()
            },
            onRoutine = { nav.hubTo(Screen.Routine.route) },
            onYoga = { nav.hubTo(Screen.ContentCategory.of("yoga")) },
            onExercise = { nav.hubTo(Screen.ContentCategory.of("sport")) },
            onBreathing = { nav.hubTo(Screen.ContentCategory.of("breath")) },
            onSleep = { nav.hubTo(Screen.PracticeGroup.of("hl-sleep")) },
            onProgress = { nav.hubTo(Screen.HealthProgress.route) },
            onSync = { runSync() },
        )
        HubCoverGrid(tiles)
    }
}

@Composable
private fun DailyHealthCard(
    snapshot: DailyHealthSnapshot,
    busy: Boolean,
    onWaterDelta: (Int) -> Unit,
    onRoutine: () -> Unit,
    onYoga: () -> Unit,
    onExercise: () -> Unit,
    onBreathing: () -> Unit,
    onSleep: () -> Unit,
    onProgress: () -> Unit,
    onSync: () -> Unit,
) {
    val waterProgress = (snapshot.waterConsumed.toFloat() / snapshot.waterGoal).coerceIn(0f, 1f)
    val routineProgress = if (snapshot.routineTotal == 0) 0f else snapshot.routineDone.toFloat() / snapshot.routineTotal
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سلامتی امروز", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "آب، ورزش، روتین و فعالیت‌ها در این کارت جمع می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                FilterChip(
                    selected = snapshot.lightDay,
                    onClick = {},
                    label = { Text(if (snapshot.lightDay) "روز سبک" else "روز معمولی") },
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyMetric("💧", snapshot.waterConsumed.toString() + "/" + snapshot.waterGoal, "لیوان", Modifier.weight(1f))
                DailyMetric("🏃", snapshot.sportsMinutes.toString(), "دقیقه ورزش", Modifier.weight(1f))
                DailyMetric("✅", snapshot.routineDone.toString() + "/" + snapshot.routineTotal, "روتین", Modifier.weight(1f))
            }

            Text("آب", style = MaterialTheme.typography.labelLarge)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { waterProgress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("روتین", style = MaterialTheme.typography.labelLarge)
            androidx.compose.material3.LinearProgressIndicator(
                progress = { routineProgress },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onWaterDelta(1) }, modifier = Modifier.weight(1f)) { Text("آب +۱") }
                Button(
                    onClick = { onWaterDelta(-1) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                ) { Text("آب −۱") }
            }

            Text("فعالیت سریع", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onRoutine, modifier = Modifier.weight(1f)) { Text("روتین") }
                TextButton(onClick = onExercise, modifier = Modifier.weight(1f)) { Text("ورزش") }
                TextButton(onClick = onYoga, modifier = Modifier.weight(1f)) { Text("یوگا") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onBreathing, modifier = Modifier.weight(1f)) { Text("تنفس") }
                TextButton(onClick = onSleep, modifier = Modifier.weight(1f)) { Text("خواب") }
                TextButton(onClick = onProgress, modifier = Modifier.weight(1f)) { Text("گزارش") }
            }

            if (snapshot.activities.isNotEmpty()) {
                Text("آخرین فعالیت‌ها", style = MaterialTheme.typography.titleMedium)
                snapshot.activities.take(4).forEach { activity ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(activity.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        if (activity.value != 0) {
                            Text(activity.value.toString(), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            JalaliDate.stampFa(activity.atMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = onSync,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) {
                Text(if (busy) "در حال دریافت و ارسال…" else "دریافت و ارسال با دیتابیس")
            }
        }
    }
}

@Composable
private fun DailyMetric(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon + " " + value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * «خواب من» یک دفترچه‌ی خودکار است: زمان تایپ نمی‌شود تا یک مهر نادرست وارد نشود.
 * ساعت و تاریخ همیشه با منطقهٔ تهران/تقویم جلالی نشان داده می‌شوند؛ هر رویداد اول
 * محلی ثبت و سپس بدون منتظرکردن کاربر در outbox همگام‌سازی می‌شود.
 */
@Composable
fun SleepLogScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val store = container.store
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var revision by remember { mutableIntStateOf(0) }
    val entries = remember(revision) { SleepLogStore.entries(store) }
    val openEntry = entries.firstOrNull { it.bedtimeAt != null && it.wokeAt == null }
    val completed = entries.firstOrNull { it.wokeAt != null }
    val streak = remember(revision) { SleepLogStore.streak(store) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    fun saveAndSync(entry: SleepLogEntry) {
        SleepLogStore.enqueueSync(container.sync, store, entry)
        revision++
        scope.launch(Dispatchers.IO) { runCatching { container.sync.pushAll() } }
    }

    HubBody {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 20.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    sleepClock(now),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${JalaliDate.weekDayFa(JalaliDate.todayIso())}، ${JalaliDate.formatFaLong(now)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Text(
            "برای ثبت زمان واقعی، فقط همان لحظه دکمه را بزن. ساعت‌ها خودکار روی همین دستگاه ذخیره می‌شوند.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { saveAndSync(SleepLogStore.recordBedtime(store)) },
                modifier = Modifier.weight(1f).sizeIn(minHeight = 54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF294A74)),
            ) {
                Text("الان می‌خوابم", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { saveAndSync(SleepLogStore.recordWakeTime(store)) },
                modifier = Modifier.weight(1f).sizeIn(minHeight = 54.dp),
            ) {
                Text("الان بیدار شدم", style = MaterialTheme.typography.labelLarge)
            }
        }

        SleepStatusCard(openEntry = openEntry, completed = completed)

        if (entries.isNotEmpty()) {
            Text("ثبت‌های پیشین", style = MaterialTheme.typography.titleLarge)
            entries.forEach { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(JalaliDate.toPersianDigits(entry.dayIso), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                listOfNotNull(
                                    entry.bedtimeAt?.let { "خواب: ${JalaliDate.stampFa(it)}" },
                                    entry.wokeAt?.let { "بیداری: ${JalaliDate.stampFa(it)}" },
                                ).joinToString("  •  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = {
                            if (SleepLogStore.delete(store, entry.id)) revision++
                        }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "رشتهٔ خواب",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    if (streak > 0) "${JalaliDate.toPersianDigits(streak.toString())} شبِ پیوسته ثبت شده" else "با اولین ثبت، رشتهٔ خوابت از همین‌جا شروع می‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Text(
            "ثبت‌ها در صف امن همگام‌سازی قرار می‌گیرند و وقتی اتصال آماده باشد خودکار فرستاده می‌شوند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun SleepStatusCard(openEntry: SleepLogEntry?, completed: SleepLogEntry?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ثبت امروز", style = MaterialTheme.typography.titleLarge)
            when {
                openEntry != null -> {
                    Text(
                        "زمان خواب: ${JalaliDate.stampFa(openEntry.bedtimeAt ?: 0L)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "وقتی بیدار شدی «الان بیدار شدم» را بزن تا همان شب کامل شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                completed != null -> {
                    completed.bedtimeAt?.let {
                        Text("خواب: ${JalaliDate.stampFa(it)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "بیداری: ${JalaliDate.stampFa(completed.wokeAt ?: 0L)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> Text(
                    "هنوز زمانی ثبت نشده؛ هر زمان آماده بودی یکی از دو دکمه را لمس کن.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** ساعت دیجیتال ۲۴ساعتهٔ تهران با رقم فارسی. */
private fun sleepClock(epochMillis: Long): String {
    val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), JalaliDate.TEHRAN)
    return JalaliDate.toPersianDigits(local.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
}

/** سازگاری با فراخوانی‌های قدیمی و آزمون‌های سطح UI. */
internal fun todayKey(): String = JalaliDate.todayIso()
