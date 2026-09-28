package com.hamyareman.ir.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.AppTypography
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TomorrowPrepScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val reminders = container.reminders
    val syncScope = rememberCoroutineScope()
    var syncNotice by remember { mutableStateOf<String?>(null) }
    val today = LocalDate.now(JalaliDate.TEHRAN)

    fun pushChecks() {
        syncScope.launch {
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isBlank()) {
                syncNotice = "روی دستگاه ذخیره شد؛ برای سینک وارد شو."
                return@launch
            }
            val ok = ClassPlanSync.push(ctx, container.tables, uid, StateSync.KEY_CHECKS)
            syncNotice = if (ok) "تیک‌ها با سرور همگام شد." else "ذخیره شد؛ سینک ناموفق بود."
        }
    }

    // با هر تغییرِ تیک، گزارشِ ماهانه دوباره ساخته می‌شود.
    var reportTick by remember { mutableIntStateOf(0) }
    // بعد از نیمه‌شب، «فردا»ی دیشب همان «امروز» است.
    val prepDate = ClassPlanStore.prepTargetDate()
    var snap by remember { mutableStateOf(ClassPlanStore.load(ctx)) }

    LaunchedEffect(Unit) {
        // رفرشِ همهٔ اطلاع‌رسانی‌های فردا در ساعت خروج/پایانِ کلاس مجازی.
        ClassPlanStore.maybeRefreshAtExit(ctx, reminders = reminders)
        snap = ClassPlanStore.load(ctx)
        val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isBlank()) return@LaunchedEffect
        val changed = ClassPlanSync.pullAll(ctx, container.tables, uid)
        if (changed) {
            snap = ClassPlanStore.load(ctx)
            syncNotice = "اطلاعات از سرور به‌روز شد."
            reportTick++
        }
        // فقط وقتی نسخهٔ دستگاه تازه‌تر است push می‌شود؛ وگرنه تیک‌های سرور پاک می‌شد.
        if (ClassPlanSync.pushIfNewer(ctx, container.tables, uid, StateSync.KEY_CHECKS)) {
            syncNotice = "تیک‌ها با سرور همگام شد."
        }
    }

    val tomorrow = ClassPlanStore.firstSchoolDay(snap, prepDate)
    val dayWord = ClassPlanStore.dayWordFor(tomorrow, today)
    val dayLabel = ClassPlanStore.dayLabelFor(tomorrow, today, ClassPlanStore.shiftOf(snap, tomorrow))
    remember { ClassPlanStore.syncAlarms(ctx, reminders, snap, today); true }
    val isoN = tomorrow.toString()
    val isoT = today.toString()
    var bag by remember { mutableStateOf(ClassPlanStore.prepBag(ctx, isoN)) }
    var hw by remember { mutableStateOf(ClassPlanStore.prepHw(ctx, isoN)) }
    var bagLock by remember { mutableStateOf(ClassPlanStore.bagLocked(ctx, isoN)) }
    var hwLock by remember { mutableStateOf(ClassPlanStore.hwLocked(ctx, isoN)) }
    var exam by remember { mutableStateOf(ClassPlanStore.examOf(ctx, isoN)) }
    var examReport by remember { mutableStateOf(ClassPlanStore.reportOf(ctx, isoN)) }
    var todayReport by remember { mutableStateOf(ClassPlanStore.reportOf(ctx, isoT)) }
    var examOpen by remember { mutableStateOf(false) }
    val shift = ClassPlanStore.shiftOf(snap, today)
    val (ah, am) = ClassPlanStore.wakeHourMinute(snap, shift)
    val sleep = ClassPlanStore.sleepText(snap, shift)
    val alarmOn = ClassPlanStore.alarmIsSet(reminders, snap, today)
    val lessons = ClassPlanStore.lessonsFor(snap, tomorrow)
    val tomorrowFa = JalaliDate.formatFaLong(isoN)
    val tomorrowWeekDay = JalaliDate.weekDayFa(isoN)

    // همهٔ اطلاع‌رسانی‌های مربوط به فردا (بر اساسِ شیفتِ همان روز)
    val alarmList = remember(alarmOn, isoN) {
        reminders.all()
            .filter { it.channel == com.hamyareman.ir.platform.core.notifications.NotificationChannels.SCHOOL_ALARM && it.enabled }
            .sortedBy { it.hour * 60 + it.minute }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("آماده‌سازی $dayWord", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "$dayWord $tomorrowWeekDay $tomorrowFa · ${ClassPlanStore.captionOf(snap, tomorrow)}")
            if (lessons.isNotEmpty()) {
                Text("درس‌های $dayWord: ${lessons.joinToString("، ")}")
            } else {
                Text(
                    "برای $dayWord درسی در برنامهٔ هفتگی نیست.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = bag,
                    enabled = !bagLock,
                    onCheckedChange = {
                        bag = it
                        bagLock = it
                        ClassPlanStore.setPrepBag(ctx, isoN, it)
                        reportTick++
                        pushChecks()
                    })
                Text("کیف مدرسه آماده است")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = hw,
                    enabled = !hwLock,
                    onCheckedChange = {
                        hw = it
                        hwLock = it
                        ClassPlanStore.setPrepHw(ctx, isoN, it)
                        reportTick++
                        pushChecks()
                    })
                Text("تکالیف انجام شده")
            }
            if (bagLock || hwLock) {
                Text(
                    "این تیک‌ها تا ساعت خروج از مدرسه (یا پایان کلاس مجازی) قفل‌اند؛ بعد از آن برای $dayWord آزاد می‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = alarmOn, onCheckedChange = {}, enabled = false)
                Text("آلارم برای ساعت ${toPersianDigits("%d:%02d".format(ah, am))} تنظیم شده")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = true, onCheckedChange = null, enabled = false)
                Text("ساعت خوابت ${toPersianDigits(sleep)} باشد")
            }

            // --- اطلاع‌رسانی‌های فردا ---
            Text(
                "اطلاع‌رسانی‌های $dayWord",
                fontWeight = FontWeight.Bold)
            if (alarmList.isEmpty()) {
                Text(
                    "اطلاع‌رسانی فعالی برای $dayWord نیست.",
                    style = MaterialTheme.typography.bodySmall)
            } else {
                alarmList.forEach { r ->
                    Text(
                        "• ${r.title} — ساعت ${toPersianDigits("%d:%02d".format(r.hour, r.minute))}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "همهٔ این‌ها در ساعت خروج از مدرسه یا پایان کلاس مجازی، برای روزِ بعد تازه می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)

            // --- امتحانِ فردا (با تاریخِ روز و گزارشِ ضمیمه) ---
            Text(
                "$dayWord امتحان داری؟ ($tomorrowWeekDay $tomorrowFa)",
                fontWeight = FontWeight.Bold)
            Text(
                "اگر امتحان داری، از درس‌های همان روز انتخاب کن.",
                style = MaterialTheme.typography.bodySmall)
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { examOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(exam.ifBlank { "بدون امتحان / انتخاب درس" })
                }
                DropdownMenu(expanded = examOpen, onDismissRequest = { examOpen = false }) {
                    DropdownMenuItem(text = { Text("بدون امتحان") }, onClick = {
                        exam = ""
                        ClassPlanStore.setExam(ctx, isoN, "")
                        examOpen = false
                        pushChecks()
                    })
                    lessons.forEach { sub ->
                        DropdownMenuItem(text = { Text(sub) }, onClick = {
                            exam = sub
                            ClassPlanStore.setExam(ctx, isoN, sub)
                            examOpen = false
                            pushChecks()
                        })
                    }
                }
            }

            if (exam.isNotBlank()) {
                Text(
                    "آمادگیِ امتحان $exam — $tomorrowWeekDay $tomorrowFa",
                    fontWeight = FontWeight.Bold)
                ClassPlanStore.examPrepOptions().forEach { opt ->
                    var done by remember(exam, isoN) { mutableStateOf(ClassPlanStore.examPrepDone(ctx, isoN, opt)) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = done,
                            onCheckedChange = {
                                done = it
                                ClassPlanStore.setExamPrepDone(ctx, isoN, opt, it)
                                pushChecks()
                            })
                        Text(opt)
                    }
                }
                OutlinedTextField(
                    value = examReport,
                    onValueChange = {
                        examReport = it
                        ClassPlanStore.saveExamReport(ctx, isoN, exam, it)
                        pushChecks()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("گزارش نتیجه امتحان") },
                    textStyle = androidx.compose.ui.text.TextStyle())
            }

            Text("گزارش عملکرد امروز (بعد از رسیدن به خانه)")
            OutlinedTextField(
                value = todayReport,
                onValueChange = {
                    todayReport = it
                    ClassPlanStore.setReport(ctx, isoT, it)
                    pushChecks()
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("چه کارهایی انجام شد؟") })

            syncNotice?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }

            // --- آکاردیونِ گزارشِ ماهانه (پیش‌فرض بسته) ---
            MonthlyChecksAccordion(reportTick)
        }
    }
}

/**
 * آکاردیونِ «گزارش ماهانه آمادگی حضور در مدرسه» — پیش‌فرض بسته.
 * فقط مواردی که تیک نخورده‌اند فهرست می‌شوند؛ روزی که تیک‌هایش کامل است
 * در گزارش نمی‌آید (و «کیف مدرسه» در روزهای مجازی ناقص حساب نمی‌شود).
 */
@Composable
private fun MonthlyChecksAccordion(tick: Int = 0) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val groups = remember(tick, open) { ClassPlanStore.readinessReport(ctx) }
    val entries = remember(groups) { groups.flatMap { it.second } }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(if (open) "▾" else "◂")
            Text(
                "گزارش ماهانه آمادگی حضور در مدرسه",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp))
        }
        if (open) {
            if (entries.isEmpty()) {
                Text(
                    "همهٔ روزها کامل تیک خورده‌اند — مورد جامانده‌ای نیست.",
                    style = MaterialTheme.typography.bodySmall)
            } else {
                groups.forEach { (month, list) ->
                    Text(month, fontWeight = FontWeight.Bold)
                    list.forEach { e ->
                        val fa = JalaliDate.formatFaLong(e.iso)
                        Text(
                            "• $fa — ${e.title} تیک نخورده",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
