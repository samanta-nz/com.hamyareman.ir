package com.hamyareman.ir.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.launch
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.AppTypography
import java.time.LocalDate

/**
 * تنظیم ساعت کلاس‌های مجازی — ساعت هر روز هفته + ثبت بازه‌های تاریخ مجازی.
 * داده فقط روی همین گوشی می‌ماند (Sync نمی‌شود).
 */
@Composable
fun VirtualClassScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val syncScope = rememberCoroutineScope()
    var syncNotice by remember { mutableStateOf<String?>(null) }
    var sessions by remember { mutableStateOf(ClassPlanStore.virtualSessions(ctx)) }
    var ranges by remember { mutableStateOf(ClassPlanStore.virtualRanges(ctx)) }
    val today = LocalDate.now(JalaliDate.TEHRAN)

    fun pushVirtual() {
        syncScope.launch {
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isBlank()) {
                syncNotice = "روی دستگاه ذخیره شد؛ برای سینک وارد شو."
                return@launch
            }
            val ok = ClassPlanSync.push(ctx, container.tables, uid, StateSync.KEY_VIRTUAL)
            syncNotice = if (ok) "ذخیره و با سرور همگام شد." else "ذخیره شد؛ سینک ناموفق بود."
        }
    }

    LaunchedEffect(Unit) {
        val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isBlank()) return@LaunchedEffect
        val changed = ClassPlanSync.pull(ctx, container.tables, uid, StateSync.KEY_VIRTUAL)
        if (changed) {
            sessions = ClassPlanStore.virtualSessions(ctx)
            ranges = ClassPlanStore.virtualRanges(ctx)
            syncNotice = "ساعت‌ها از سرور به‌روز شد."
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("تنظیم ساعت کلاس‌های مجازی", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "ساعت شروع و پایان کلاس مجازی هر روز را بنویس. این ساعت‌ها در برنامه‌ی کلاسی و آماده‌سازی همان روز نشان داده می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth())

            ClassPlanStore.WEEKDAYS.forEachIndexed { i, dayName ->
                val di = i + 1
                val s = sessions.firstOrNull { it.dayIndex == di }
                    ?: ClassPlanStore.VirtualSession(di, 8, 0, 9, 0, "")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(dayName, fontWeight = FontWeight.Bold)
                        Text("شیفت صبح", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        TimePick("شروع صبح", s.startH, s.startM) { h, m ->
                            sessions = upsertSession(sessions, s.copy(startH = h, startM = m))
                            ClassPlanStore.saveVirtualSessions(ctx, sessions)
                            pushVirtual()
                        }
                        TimePick("پایان صبح", s.endH, s.endM) { h, m ->
                            sessions = upsertSession(sessions, s.copy(endH = h, endM = m))
                            ClassPlanStore.saveVirtualSessions(ctx, sessions)
                            pushVirtual()
                        }
                        Text("شیفت ظهر", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        TimePick("شروع ظهر", s.eveStartH, s.eveStartM) { h, m ->
                            sessions = upsertSession(sessions, s.copy(eveStartH = h, eveStartM = m))
                            ClassPlanStore.saveVirtualSessions(ctx, sessions)
                            pushVirtual()
                        }
                        TimePick("پایان ظهر", s.eveEndH, s.eveEndM) { h, m ->
                            sessions = upsertSession(sessions, s.copy(eveEndH = h, eveEndM = m))
                            ClassPlanStore.saveVirtualSessions(ctx, sessions)
                            pushVirtual()
                        }
                        OutlinedTextField(
                            value = s.subject,
                            onValueChange = { v ->
                                sessions = upsertSession(sessions, s.copy(subject = v))
                                ClassPlanStore.saveVirtualSessions(ctx, sessions)
                            },
                            label = { Text("درس (اختیاری)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true)
                    }
                }
            }

            Text(
                "بازه‌های روزهای مجازی",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth())
            VirtualRangeSection(
                today = today,
                ranges = ranges,
                onAdd = { from, to ->
                    ClassPlanStore.addVirtualRange(ctx, from, to)
                    ranges = ClassPlanStore.virtualRanges(ctx)
                    pushVirtual()
                },
                onDelete = { id ->
                    ClassPlanStore.removeVirtualRange(ctx, id)
                    ranges = ClassPlanStore.virtualRanges(ctx)
                    pushVirtual()
                })
            syncNotice?.let { notice ->
                Text(
                    notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right)
            }
        }
    }
}

private fun upsertSession(
    list: List<ClassPlanStore.VirtualSession>,
    next: ClassPlanStore.VirtualSession): List<ClassPlanStore.VirtualSession> {
    val out = list.filterNot { it.dayIndex == next.dayIndex }.toMutableList()
    out += next
    return out.sortedBy { it.dayIndex }
}

/**
 * انتخاب بازه با تقویم شمسی + نمایش آکاردیونیِ بازه‌ها با حذفِ تأییدی.
 */
@Composable
fun VirtualRangeSection(
    today: LocalDate,
    ranges: List<ClassPlanStore.VirtualRange>,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit) {
    var fromIso by remember { mutableStateOf(today.toString()) }
    var toIso by remember { mutableStateOf(today.toString()) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf<ClassPlanStore.VirtualRange?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickFrom = true }, modifier = Modifier.weight(1f)) {
                Text("از: ${faDate(fromIso)}", maxLines = 1)
            }
            OutlinedButton(onClick = { pickTo = true }, modifier = Modifier.weight(1f)) {
                Text("تا: ${faDate(toIso)}", maxLines = 1)
            }
        }
        OutlinedButton(
            onClick = { onAdd(fromIso, toIso) },
            modifier = Modifier.fillMaxWidth()) { Text("افزودن این بازه") }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "بازه‌های ثبت‌شده (${toPersianDigits(ranges.size.toString())})",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Right)
                }
                AnimatedVisibility(visible = expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (ranges.isEmpty()) {
                            Text(
                                "بازه‌ای ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right)
                        }
                        ranges.forEach { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { askDelete = r }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "حذف بازه",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                                Text(
                                    "${faDate(r.fromIso)} → ${faDate(r.toIso)}",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right)
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickFrom) {
        ShamsiDatePickerDialog(
            initialIso = fromIso,
            title = "انتخاب تاریخ شروع",
            onDismiss = { pickFrom = false },
            onPick = { fromIso = it })
    }
    if (pickTo) {
        ShamsiDatePickerDialog(
            initialIso = toIso,
            title = "انتخاب تاریخ پایان",
            onDismiss = { pickTo = false },
            onPick = { toIso = it })
    }
    askDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { askDelete = null },
            title = { Text("حذف این بازه؟") },
            text = {
                Text(
                    "روزهای ${faDate(r.fromIso)} تا ${faDate(r.toIso)} از حالت مجازی خارج می‌شوند.",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(r.id)
                    askDelete = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { askDelete = null }) { Text("انصراف") } })
    }
}

private fun faDate(iso: String): String {
    val j = JalaliDate.toJalali(iso) ?: return iso
    return "${JalaliDate.weekDayFa(iso)} ${toPersianDigits("${j.day} ${JalaliDate.monthName(j.month)}")}"
}

/** کادرِ کوچکِ سفید برای نمایش یک بازه‌ی مجازی (داخل صفحه‌ی کلاسی هم استفاده می‌شود). */
@Composable
fun VirtualRangeChipBox(range: ClassPlanStore.VirtualRange) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart) {
        Text(
            "${faDate(range.fromIso)} → ${faDate(range.toIso)}",
            color = Color(0xFFB91C1C),
            style = MaterialTheme.typography.labelLarge)
    }
}
