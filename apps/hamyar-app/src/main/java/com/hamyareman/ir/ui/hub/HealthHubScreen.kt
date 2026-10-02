package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.R
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** هاب «سلامتی»: جلدهای مربعی ۲ در ردیف، مثل کتاب‌ها. */
@Composable
fun HealthHubScreen(nav: NavController) {
    val girl = com.hamyareman.ir.ui.profile.StudentProfileState.gender != "boy"
    val tiles = buildList {
        add(HubCoverTile("hl-progress", "پیشرفت سلامتی", "آب، ورزش و آمار درس", { nav.hubTo(Screen.HealthProgress.route) }))
        if (girl) {
            add(HubCoverTile("hl-period", "چرخه ماهانه", "تقویم، علائم، تنفس درد و تمرین ملایم", { nav.hubTo(Screen.PracticeGroup.of("hl-cycle")) }))
        }
        add(HubCoverTile("hl-yoga", "یوگا", "حرکات تعاملی با راهنمای کامل", { nav.hubTo(Screen.ContentCategory.of("yoga")) }))
        add(HubCoverTile("hl-exercise", "ورزش عمومی", "تمرین‌های تعاملی مرحله‌به‌مرحله", { nav.hubTo(Screen.ContentCategory.of("sport")) }))
        add(HubCoverTile("hl-food", "آب و تغذیه", "یادآور آب و راهنمای تمرکز", { nav.hubTo(Screen.PracticeGroup.of("hl-nutrition")) }))
        add(HubCoverTile("hl-sleep", "خواب", "ثبت، قصه، بشنو و بخواب، تنفس شب", { nav.hubTo(Screen.PracticeGroup.of("hl-sleep")) }))
        add(HubCoverTile("hl-meds", "یادآور دارو و مراقبت", "هشدار سرِ وقت", { nav.hubTo(Screen.Meds.route) }))
        add(HubCoverTile("hl-routine", "روتین روز", "بلوک‌های روز یا روز سبک", { nav.hubTo(Screen.Routine.route) }))
    }
    HubBody {
        HubHeader("سلامتی 💚", "بدنت دوست توست — هر روز یک قدم مهربانی", slotId = "hub.health.header")
        HubCoverGrid(tiles)
    }
}

private val SleepLalezar = FontFamily(Font(R.font.lalezar, FontWeight.Normal))
private val SleepVazirmatnRegular = FontFamily(Font(R.font.vazirmatn_regular, FontWeight.Normal))

private fun TextStyle.sleepHeading() = copy(fontFamily = SleepLalezar, fontWeight = FontWeight.Normal)
private fun TextStyle.sleepBody() = copy(fontFamily = SleepVazirmatnRegular, fontWeight = FontWeight.Normal)

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
                    style = MaterialTheme.typography.displayMedium.sleepHeading().copy(fontSize = 42.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${JalaliDate.weekDayFa(JalaliDate.todayIso())}، ${JalaliDate.formatFaLong(now)}",
                    style = MaterialTheme.typography.bodyMedium.sleepBody(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Text(
            "برای ثبت زمان واقعی، فقط همان لحظه دکمه را بزن. ساعت‌ها خودکار روی همین دستگاه ذخیره می‌شوند.",
            style = MaterialTheme.typography.bodyMedium.sleepBody(),
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
                Text("الان می‌خوابم", style = MaterialTheme.typography.labelLarge.sleepBody())
            }
            Button(
                onClick = { saveAndSync(SleepLogStore.recordWakeTime(store)) },
                modifier = Modifier.weight(1f).sizeIn(minHeight = 54.dp),
            ) {
                Text("الان بیدار شدم", style = MaterialTheme.typography.labelLarge.sleepBody())
            }
        }

        SleepStatusCard(openEntry = openEntry, completed = completed)

        if (entries.isNotEmpty()) {
            Text("ثبت‌های پیشین", style = MaterialTheme.typography.titleLarge.sleepHeading())
            entries.forEach { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(JalaliDate.toPersianDigits(entry.dayIso), style = MaterialTheme.typography.bodyMedium.sleepBody())
                            Text(
                                listOfNotNull(
                                    entry.bedtimeAt?.let { "خواب: ${JalaliDate.stampFa(it)}" },
                                    entry.wokeAt?.let { "بیداری: ${JalaliDate.stampFa(it)}" },
                                ).joinToString("  •  "),
                                style = MaterialTheme.typography.bodySmall.sleepBody(),
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
                    style = MaterialTheme.typography.titleLarge.sleepHeading(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    if (streak > 0) "${JalaliDate.toPersianDigits(streak.toString())} شبِ پیوسته ثبت شده" else "با اولین ثبت، رشتهٔ خوابت از همین‌جا شروع می‌شود.",
                    style = MaterialTheme.typography.bodyMedium.sleepBody(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Text(
            "ثبت‌ها در صف امن همگام‌سازی قرار می‌گیرند و وقتی اتصال آماده باشد خودکار فرستاده می‌شوند.",
            style = MaterialTheme.typography.bodySmall.sleepBody(),
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
            Text("ثبت امروز", style = MaterialTheme.typography.titleLarge.sleepHeading())
            when {
                openEntry != null -> {
                    Text(
                        "زمان خواب: ${JalaliDate.stampFa(openEntry.bedtimeAt ?: 0L)}",
                        style = MaterialTheme.typography.bodyMedium.sleepBody(),
                    )
                    Text(
                        "وقتی بیدار شدی «الان بیدار شدم» را بزن تا همان شب کامل شود.",
                        style = MaterialTheme.typography.bodySmall.sleepBody(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                completed != null -> {
                    completed.bedtimeAt?.let {
                        Text("خواب: ${JalaliDate.stampFa(it)}", style = MaterialTheme.typography.bodyMedium.sleepBody())
                    }
                    Text(
                        "بیداری: ${JalaliDate.stampFa(completed.wokeAt ?: 0L)}",
                        style = MaterialTheme.typography.bodyMedium.sleepBody(),
                    )
                }
                else -> Text(
                    "هنوز زمانی ثبت نشده؛ هر زمان آماده بودی یکی از دو دکمه را لمس کن.",
                    style = MaterialTheme.typography.bodyMedium.sleepBody(),
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
