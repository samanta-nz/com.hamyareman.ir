package com.hamyareman.ir.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.study.ClassPlanStore
import com.hamyareman.ir.ui.study.ClassPlanSync
import com.hamyareman.ir.ui.study.SchoolShift
import com.hamyareman.ir.ui.study.StateSync
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.hamyareman.ir.ui.AppTypography

private val LessonColors = listOf(
    Color(0xFF0F766E),
    Color(0xFF4338CA),
    Color(0xFFB45309),
    Color(0xFFBE185D),
    Color(0xFF0369A1))

@Composable
fun ClassPlanCard(
    modifier: Modifier = Modifier,
    onOpenPlan: () -> Unit,
    onOpenPrep: () -> Unit,
    onOpenAlarm: () -> Unit,
    onOpenLeave: () -> Unit = {}) {
    val ctx = LocalContext.current
    val reminders = LocalAppContainer.current.reminders
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val today = remember(tick) { LocalDate.now(JalaliDate.TEHRAN) }
    val snap = remember(tick) { ClassPlanStore.load(ctx) }
    LaunchedEffect(tick, snap.cycleWeeks, snap.anchorIso, snap.fixedEvening, snap.morningHour, snap.noonHour) {
        ClassPlanStore.maybeRefreshAtExit(ctx, reminders = reminders)
        ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
    }
    val syncBox = LocalAppContainer.current
    LaunchedEffect(Unit) {
        val uid = runCatching { syncBox.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isBlank()) return@LaunchedEffect
        val got = ClassPlanSync.pullAll(ctx, syncBox.tables, uid)
        ClassPlanSync.keys.forEach { key ->
            runCatching { ClassPlanSync.pushIfNewer(ctx, syncBox.tables, uid, key) }
        }
        if (got) tick++
    }
    val virtEnd = remember(tick) {
        val sh = ClassPlanStore.shiftOf(snap, today)
        ClassPlanStore.virtualSessions(ctx).firstOrNull { it.dayIndex == SchoolShift.dayIndex(today) }
            ?.endMinFor(sh)
    }
    val showDate = remember(tick, snap.exitMorning, snap.exitNoon, snap.anchorIso, virtEnd) {
        ClassPlanStore.dashboardShowDate(snap, virtualEndMin = virtEnd, ctx = ctx)
    }
    val j = JalaliDate.toJalali(showDate.toString())
    val dayName = JalaliDate.weekDayFa(showDate.toString())
    val dateFa = j?.let { toPersianDigits("${it.day} ${JalaliDate.monthName(it.month)}") } ?: ""
    val shift = ClassPlanStore.shiftOf(snap, showDate)
    val holiday = ClassPlanStore.isSchoolHoliday(snap, showDate, ctx)
    val dayWord = ClassPlanStore.dayWordFor(showDate, today)
    val dayLabel = ClassPlanStore.dayLabelFor(showDate, today, ClassPlanStore.shiftOf(snap, showDate))
    // هر خانه می‌تواند دو درس داشته باشد: «درسِ اول / درسِ دوم» در یک کادر.
    val tomorrowLessons = ClassPlanStore.lessonsFor(snap, showDate)
    val tomorrowSecond = snap.second[com.hamyareman.ir.ui.study.SchoolShift.dayIndex(showDate)].orEmpty()
    val boxes = tomorrowLessons.mapIndexed { i, name ->
        val b = tomorrowSecond.getOrElse(i) { "" }
        if (b.isNotBlank()) "$name / $b" else name
    }.ifEmpty { listOf("—", "—", "—") }.take(5)
    val isoN = showDate.toString()
    val isoT = today.toString()
    var bag by remember(tick, isoN) { mutableStateOf(ClassPlanStore.prepBag(ctx, isoN)) }
    var hw by remember(tick, isoN) { mutableStateOf(ClassPlanStore.prepHw(ctx, isoN)) }
    val bagLock = remember(tick, isoN, bag) { ClassPlanStore.bagLocked(ctx, isoN) }
    val hwLock = remember(tick, isoN, hw) { ClassPlanStore.hwLocked(ctx, isoN) }
    val alarmOn = ClassPlanStore.alarmIsSet(reminders, snap, today)
    val alarmPrefs = remember(tick) { com.hamyareman.ir.ui.study.SchoolAlarmStore.load(ctx) }
    val (ah, am) = if (shift == com.hamyareman.ir.ui.study.Shift.MORNING) alarmPrefs.wakeMH to alarmPrefs.wakeMM else alarmPrefs.wakeNH to alarmPrefs.wakeNM
    val alarmLabel = toPersianDigits("%d:%02d".format(ah, am))
    val sleep = toPersianDigits(
        if (shift == com.hamyareman.ir.ui.study.Shift.MORNING) "%d:%02d".format(alarmPrefs.sleepMH, alarmPrefs.sleepMM)
        else "%d:%02d".format(alarmPrefs.sleepNH, alarmPrefs.sleepNM))
    val virtual = ClassPlanStore.isVirtual(ctx, isoN)
    var exam by remember(tick, isoN) { mutableStateOf(ClassPlanStore.examOf(ctx, isoN)) }
    var report by remember(tick, isoN, exam) { mutableStateOf(ClassPlanStore.reportOf(ctx, isoN)) }
    val pushScope = rememberCoroutineScope()
    val container = LocalAppContainer.current
    fun pushChecks() {
        pushScope.launch {
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isBlank()) return@launch
            ClassPlanSync.push(ctx, container.tables, uid, StateSync.KEY_CHECKS)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "برنامه کلاسی مدرسه",
                fontFamily = AppTypography.d9Section.family, fontWeight = AppTypography.d9Section.weight,
                fontSize = AppTypography.d9Section.size)
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenPlan),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayName, fontFamily = AppTypography.d12ClassDate.family, fontWeight = AppTypography.d12ClassDate.weight, fontSize = AppTypography.d12ClassDate.size)
                    Text(dateFa, fontFamily = AppTypography.d12ClassDate.family, fontWeight = AppTypography.d12ClassDate.weight, fontSize = AppTypography.d12ClassDate.size, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(shift.label, fontFamily = AppTypography.d12ClassDate.family, fontWeight = AppTypography.d12ClassDate.weight, fontSize = AppTypography.d12ClassDate.size, color = MaterialTheme.colorScheme.primary)
                }
                if (holiday) {
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFEF3C7)).padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Text(
                            ClassPlanStore.holidayRoutine(today),
                            fontFamily = AppTypography.d10ClassBox.family, fontWeight = AppTypography.d10ClassBox.weight,
                            fontSize = AppTypography.d10ClassBox.size,
                            color = Color(0xFF92400E),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    boxes.forEachIndexed { i, name ->
                        Box(
                            Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(14.dp))
                                .background(LessonColors[i % LessonColors.size]),
                            contentAlignment = Alignment.Center) {
                            Text(
                                name,
                                color = Color.White,
                                fontFamily = AppTypography.d10ClassBox.family, fontWeight = AppTypography.d10ClassBox.weight,
                                fontSize = AppTypography.d10ClassBox.size,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }
            if (virtual) {
                Text(
                    "$dayWord مجازی است",
                    color = Color(0xFFB91C1C),
                    fontFamily = AppTypography.d9Section.family, fontWeight = AppTypography.d9Section.weight,
                    fontSize = AppTypography.d9Section.size)
            }
            val leave = ClassPlanStore.leaveOn(ctx, isoN)
            if (leave != null) {
                Text(
                    "$dayWord مرخصی است — ${leave.reason} (${ClassPlanStore.justificationLabel(leave.justification)})",
                    color = Color(0xFFB45309),
                    fontFamily = AppTypography.d9Section.family, fontWeight = AppTypography.d9Section.weight,
                    fontSize = AppTypography.d9Section.size)
            }
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PrepTick(
                        label = "کیف مدرسه آماده است",
                        checked = bag && !virtual,
                        enabled = !virtual && !bagLock,
                        modifier = Modifier.weight(1f)) {
                        bag = it
                        ClassPlanStore.setPrepBag(ctx, isoN, it)
                        pushChecks()
                    }
                    PrepTick(
                        label = "تکالیف انجام شده",
                        checked = hw,
                        enabled = !hwLock,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        hw = it
                        ClassPlanStore.setPrepHw(ctx, isoN, it)
                        pushChecks()
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PrepTick(
                        label = "آلارم برای $alarmLabel",
                        checked = alarmOn,
                        enabled = false,
                        modifier = Modifier.weight(1f).clickable(onClick = onOpenAlarm))
                    PrepTick(
                        label = "ساعت خوابت $sleep باشد",
                        checked = true,
                        enabled = false,
                        modifier = Modifier.weight(1f).padding(start = 8.dp))
                }
            }
            // در داشبورد فقط نمایش است؛ خودِ متن به صفحهٔ آماده‌سازی فردا می‌رود.
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (exam.isBlank()) "$dayLabel امتحان داری؟" else "$dayLabel امتحان $exam",
                    fontFamily = AppTypography.d9Section.family, fontWeight = AppTypography.d9Section.weight,
                    fontSize = AppTypography.d9Section.size,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).clickable(onClick = onOpenPrep))
                Text(
                    "ثبت مرخصی",
                    fontFamily = AppTypography.d9Section.family, fontWeight = AppTypography.d9Section.weight,
                    fontSize = AppTypography.d9Section.size,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenLeave))
            }
            if (exam.isNotBlank()) {
                OutlinedTextField(
                    value = report,
                    onValueChange = {
                        report = it
                        ClassPlanStore.saveExamReport(ctx, isoN, exam, it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("گزارش نتیجه امتحان", fontFamily = AppTypography.d11Check.family, fontWeight = AppTypography.d11Check.weight) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = AppTypography.d11Check.family, fontWeight = AppTypography.d11Check.weight,
                        fontSize = AppTypography.d11Check.size))
            }
        }
    }
}

@Composable
private fun PrepTick(
    label: String,
    checked: Boolean,
    enabled: Boolean = false,
    modifier: Modifier = Modifier,
    onChecked: ((Boolean) -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Checkbox(
            checked = checked,
            onCheckedChange = { if (enabled && onChecked != null) onChecked(it) },
            enabled = enabled)
        Text(label, fontFamily = AppTypography.d11Check.family, fontWeight = AppTypography.d11Check.weight, fontSize = AppTypography.d11Check.size, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
