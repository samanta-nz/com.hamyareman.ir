package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.notifications.AlarmRinger
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.home.IranOfficialHolidays
import java.time.LocalDate

/** ساعت به صورت «HH:MM» با یک انتخابگرِ ساده. */
@Composable
private fun TimePickText(label: String, value: String, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val parts = value.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    OutlinedButton(
        onClick = {
            android.app.TimePickerDialog(ctx, { _, hh, mm -> onPick("%d:%02d".format(hh, mm)) }, h, m, true).show()
        },
        modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label  ${toPersianDigits("%d:%02d".format(h, m))}")
    }
}



@Composable
private fun DerivedAlarmTime(
    label: String,
    hour: Int,
    minute: Int,
    onHelp: () -> Unit,
) {
    OutlinedButton(onClick = onHelp, modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label  ${toPersianDigits("%02d:%02d".format(hour, minute))}  ·  خودکار",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
@Composable
fun ClassPlanScreen(onBack: () -> Unit, initialTab: Int = 0, onVirtualHours: (() -> Unit)? = null, onHelp: () -> Unit = {}) {
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("برنامه کلاسی مدرسه", onBack, onHelp = onHelp)
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            listOf("هفتگی", "تقویم", "شیفت مدرسه").forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold)
                })
            }
        }
        when (tab) {
            0 -> WeeklyTimetableSection()
            1 -> ShamsiCalendarSection()
            else -> ShiftSection(onVirtualHours = onVirtualHours)
        }
    }
}

@Composable
private fun WeeklyTimetableSection() {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val syncScope = rememberCoroutineScope()
    var syncNotice by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }
    fun uidNow(): String = container.auth.cachedUserId()
        ?: runCatching { kotlinx.coroutines.runBlocking { container.auth.currentUserId() } }.getOrNull().orEmpty()
    val options = remember { ClassPlanStore.subjectOptions(ctx) }
    var snap by remember { mutableStateOf(ClassPlanStore.load(ctx)) }
    var days by remember {
        mutableStateOf(
            (1..5).associateWith { d ->
                val v = snap.days[d].orEmpty()
                (if (v.isEmpty()) listOf("", "", "") else v).toMutableList()
            })
    }
    // هر خانه می‌تواند «دو درس» داشته باشد: درسِ دوم هم‌اندازهٔ درسِ اول.
    var seconds by remember {
        mutableStateOf(
            (1..5).associateWith { d ->
                val n = days[d]?.size ?: 3
                val v = snap.second[d].orEmpty()
                (v + List((n - v.size).coerceAtLeast(0)) { "" }).take(n).toMutableList()
            })
    }
    var bells by remember { mutableStateOf(snap.bells) }
    var locked by remember { mutableStateOf(snap.locked) }
    var confirmEdit by remember { mutableStateOf(false) }
    // بعد از pull، stateهای صفحه از روی دادهٔ تازهٔ سرور ساخته می‌شوند — وگرنه
    // برنامهٔ کشیده‌شده تا خروج از صفحه دیده نمی‌شد (همان «سینک درست نمی‌شود»).
    fun reloadFromStore() {
        snap = ClassPlanStore.load(ctx)
        days = (1..5).associateWith { d ->
            val v = snap.days[d].orEmpty()
            (if (v.isEmpty()) listOf("", "", "") else v).toMutableList()
        }
        seconds = (1..5).associateWith { d ->
            val n = days[d]?.size ?: 3
            val v = snap.second[d].orEmpty()
            (v + List((n - v.size).coerceAtLeast(0)) { "" }).take(n).toMutableList()
        }
        bells = snap.bells
        locked = snap.locked
    }
    LaunchedEffect(Unit) {
        // شناسه را اول از کشِ محلی می‌گیریم: اگر لحظهٔ باز شدنِ صفحه اینترنت نباشد،
        // `account.get()` شکست می‌خورد و سینک کلاً متوقف می‌شد (همان «سینک نمی‌شود»).
        val uid = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isNotBlank()) {
            val changed = ClassPlanSync.pullAll(ctx, container.tables, uid)
            if (changed) {
                reloadFromStore()
                syncNotice = "برنامه از سرور به‌روز شد."
            }
            // فقط اگر نسخهٔ دستگاه تازه‌تر باشد؛ وگرنه دادهٔ سرور پاک می‌شد.
            if (ClassPlanSync.pushIfNewer(ctx, container.tables, uid, StateSync.KEY_WEEK)) {
                syncNotice = "برنامه با سرور همگام شد."
            }
            syncError = StateSync.lastError(ctx, StateSync.KEY_WEEK)
        } else {
            syncError = "برای همگام‌سازی اول وارد حساب شو."
        }
    }

    fun persist(lock: Boolean, map: Map<Int, List<String>> = days, sec: Map<Int, List<String>> = seconds, bell: List<String> = bells) {
        val clean = map.mapValues { e ->
            val v = e.value.toMutableList()
            while (v.size < 3) v.add("")
            v.toList()
        }
        val cleanSec = sec.mapValues { e ->
            val n = clean[e.key]?.size ?: 0
            val v = e.value.toMutableList()
            while (v.size < n) v.add("")
            v.take(n).toList()
        }
        ClassPlanStore.saveDays(ctx, clean, lock, seconds = cleanSec, bells = bell)
        locked = lock
        snap = ClassPlanStore.load(ctx)
    }
    fun persistAndSync(lock: Boolean, map: Map<Int, List<String>> = days, sec: Map<Int, List<String>> = seconds, bell: List<String> = bells) {
        persist(lock, map, sec, bell)
        syncScope.launch {
            val uid = container.auth.cachedUserId() ?: uidNow()
            val pushed = ClassPlanSync.pushIfNewer(ctx, container.tables, uid, StateSync.KEY_WEEK)
            syncNotice = if (pushed) "برنامه با سرور همگام شد." else syncNotice
            syncError = StateSync.lastError(ctx, StateSync.KEY_WEEK)
        }
    }

    val maxCells = (days.values.maxOfOrNull { it.size } ?: 3).coerceAtLeast(1)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "شنبه تا چهارشنبه. هر خانه می‌تواند دو درس داشته باشد و ساعتِ شروع و پایانِ هر زنگ («تایم زنگ ۱…» در پایین) برای همهٔ روزها یکی است. پس از تکمیل، فهرست‌ها غیرفعال می‌شوند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        syncNotice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        syncError?.let {
            Text(
                "⚠ $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
        ClassPlanStore.WEEKDAYS.forEachIndexed { i, name ->
            val di = i + 1
            val slots = (days[di] ?: emptyList()).let { s ->
                if (s.size >= 3) s else (s + List(3 - s.size) { "" })
            }
            val slots2 = (seconds[di] ?: emptyList()).let { s ->
                if (s.size >= slots.size) s else (s + List(slots.size - s.size) { "" })
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name)
                slots.forEachIndexed { si, value ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SubjectDropdown(
                                value = value,
                                options = options,
                                enabled = !locked,
                                modifier = Modifier.weight(1f),
                                onPick = { picked ->
                                    val next = slots.toMutableList()
                                    while (next.size <= si) next.add("")
                                    next[si] = picked
                                    val map = days.toMutableMap().also { it[di] = next }
                                    days = map
                                    persistAndSync(lock = false, map = map)
                                })
                            SubjectDropdown(
                                value = slots2.getOrElse(si) { "" },
                                options = options,
                                enabled = !locked,
                                placeholder = "درس دوم (اختیاری)",
                                modifier = Modifier.weight(1f),
                                onPick = { picked ->
                                    val next = slots2.toMutableList()
                                    while (next.size <= si) next.add("")
                                    next[si] = picked
                                    val sec = seconds.toMutableMap().also { it[di] = next }
                                    seconds = sec
                                    persistAndSync(lock = false, sec = sec)
                                })
                        }
                        val bellText = ClassPlanStore.bellLabel(snap, si)
                        Text(
                            if (bellText.isBlank()) "تایم زنگ ${toPersianDigits((si + 1).toString())}: تنظیم نشده"
                            else "تایم زنگ ${toPersianDigits((si + 1).toString())}: $bellText",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (bellText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                    }
                }
                if (!locked) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = {
                            days = days.toMutableMap().also { it[di] = (slots + "").toMutableList() }
                            seconds = seconds.toMutableMap().also { it[di] = (slots2 + "").toMutableList() }
                        }) { Text("افزودن خانه") }
                        if (slots.size > 1) {
                            TextButton(onClick = {
                                ClassPlanStore.removeSlot(ctx, di, slots.size - 1)
                                snap = ClassPlanStore.load(ctx)
                                days = days.toMutableMap().also {
                                    it[di] = (ClassPlanStore.load(ctx).days[di].orEmpty()).toMutableList()
                                }
                                seconds = seconds.toMutableMap().also {
                                    it[di] = (ClassPlanStore.load(ctx).second[di].orEmpty()).toMutableList()
                                }
                                persistAndSync(lock = false, map = days, sec = seconds)
                            }) { Text("حذف آخرین خانه") }
                        }
                    }
                }
            }
        }

        // ---- «تایم زنگ ۱..n» — یک‌بار برای هر دو شیفت و همهٔ روزها، جمع‌شونده ----
        // اگر هیچ زنگی تنظیم نشده باشد باز می‌آید؛ وگرنه جمع است و با یک لمس باز می‌شود.
        var bellsOpen by remember { mutableStateOf(snap.bells.none { it.isNotBlank() }) }
        val bellsSet = bells.count { it.isNotBlank() }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable { bellsOpen = !bellsOpen }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "تایم زنگ‌ها",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text(
                    toPersianDigits("$bellsSet/$maxCells") + if (bellsOpen) "  ▲ بستن" else "  ▼ باز کردن",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "ساعتِ شروع و پایانِ هر زنگ فقط یک‌بار این‌جا وارد می‌شود و برای هر دو شیفتِ صبح و عصر و همهٔ روزها ذخیره و با سرور همگام می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (bellsOpen) (0 until maxCells).forEach { si ->
                val raw = bells.getOrElse(si) { "" }
                val parts = raw.split("-")
                val from = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "07:30"
                val to = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "08:15"
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "تایم زنگ ${toPersianDigits((si + 1).toString())}  ·  ${toPersianDigits(from)} تا ${toPersianDigits(to)}",
                        fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) {
                            TimePickText("شروع", from) { hh ->
                                val next = (bells + List((si + 1 - bells.size).coerceAtLeast(0)) { "" }).toMutableList()
                                next[si] = "$hh-$to"
                                bells = next
                                persistAndSync(lock = locked, bell = next)
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            TimePickText("پایان", to) { hh ->
                                val next = (bells + List((si + 1 - bells.size).coerceAtLeast(0)) { "" }).toMutableList()
                                next[si] = "$from-$hh"
                                bells = next
                                persistAndSync(lock = locked, bell = next)
                            }
                        }
                    }
                }
            }
        }

        if (locked) {
            OutlinedButton(onClick = { confirmEdit = true }, modifier = Modifier.fillMaxWidth()) {
                Text("ویرایش برنامه هفتگی")
            }
        } else {
            OutlinedButton(
                onClick = {
                    persistAndSync(lock = (1..5).all { d -> (days[d]?.count { it.isNotBlank() } ?: 0) >= 3 })
                },
                modifier = Modifier.fillMaxWidth()) { Text("ذخیره برنامه") }
        }
        // کارتِ کاملِ مرخصی به صفحهٔ جداگانهٔ «مرخصی» منتقل شد
        // (از منوی «برنامه هفتگی و مرخصی» در صفحهٔ اصلی مدرسه باز می‌شود).
    }
    if (confirmEdit) {
        AlertDialog(
            onDismissRequest = { confirmEdit = false },
            title = { Text("ویرایش برنامه؟") },
            text = { Text("برنامهٔ هفتگی قفل است. مطمئنی می‌خواهی تغییرش بدهی؟") },
            confirmButton = {
                TextButton(onClick = { confirmEdit = false; locked = false }) { Text("بله، ویرایش") }
            },
            dismissButton = { TextButton(onClick = { confirmEdit = false }) { Text("انصراف") } })
    }
}

@Composable
private fun SubjectDropdown(
    value: String,
    options: List<String>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "انتخاب درس",
    onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { if (enabled) open = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()) {
            Text(value.ifBlank { placeholder }, maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    // گزینهٔ «— خالی —» خانه را خالی می‌کند (مقدارِ ذخیره‌شده = "").
                    onClick = {
                        onPick(if (opt == ClassPlanStore.EMPTY_SUBJECT) "" else opt)
                        open = false
                    })
            }
        }
    }
}

@Composable
private fun ShamsiCalendarSection() {
    val ctx = LocalContext.current
    val todayJ = JalaliDate.todayJalali()
    var year by remember { mutableIntStateOf(todayJ.year) }
    var month by remember { mutableIntStateOf(todayJ.month) }
    val weekHdr = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    val offset = com.hamyareman.ir.ui.home.CalendarPrefs.lunarOffset(ctx)

    fun firstDow(y: Int, m: Int): Int {
        val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(y, m, 1)) ?: return 0
        return (SchoolShift.dayIndex(LocalDate.parse(iso)) - 1).coerceIn(0, 6)
    }

    val dim = JalaliDate.daysInMonth(year, month)
    val todayIso = JalaliDate.toGregorianIso(todayJ)
    val todayG = todayIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val todayH = todayG?.let { com.hamyareman.ir.ui.home.CalendarOccasions.hijriOf(it) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("تقویم شمسی، قمری و میلادی", fontWeight = FontWeight.Bold)
        if (todayG != null && todayH != null) {
            Text(
                "شمسی: ${todayJ.faLong}",
                style = MaterialTheme.typography.bodyMedium)
            Text(
                "میلادی: ${todayG.year}/${todayG.monthValue}/${todayG.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium)
            Text(
                "قمری: ${com.hamyareman.ir.ui.home.CalendarOccasions.hijriFa(todayG)}",
                style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (month == 1) { month = 12; year-- } else month--
            }) { Text("ماه قبل") }
            Text("${JalaliDate.monthName(month)} ${toPersianDigits(year.toString())}", fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                if (month == 12) { month = 1; year++ } else month++
            }) { Text("ماه بعد") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFDC2626)))
                Text("تعطیل رسمی", style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF93C5FD)))
                Text("تعطیل مدرسه", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weekHdr.forEach { h ->
                Text(h, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        val pad = firstDow(year, month)
        val cells = List(pad) { 0 } + (1..dim).toList()
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(0.85f).padding(1.dp), contentAlignment = Alignment.Center) {
                        if (day > 0) {
                            val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(year, month, day))
                            val date = iso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                            val evs = date?.let { com.hamyareman.ir.ui.home.CalendarOccasions.matching(ctx, it, offset) }.orEmpty()
                            val official = date != null && com.hamyareman.ir.ui.home.CalendarOccasions.isOfficialHoliday(ctx, date, offset)
                            val school = date != null && com.hamyareman.ir.ui.home.CalendarOccasions.isSchoolWeekend(date) && !official
                            val isToday = year == todayJ.year && month == todayJ.month && day == todayJ.day
                            val g = date
                            val h = date?.let { com.hamyareman.ir.ui.home.CalendarOccasions.hijriOf(it) }
                            val bg = when {
                                official -> Color(0xFFFECACA)
                                school -> Color(0xFFDBEAFE)
                                else -> Color.Transparent
                            }
                            val fg = when {
                                official -> Color(0xFF9F1239)
                                school -> Color(0xFF1D4ED8)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .then(
                                        if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        else Modifier),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(toPersianDigits(day.toString()), color = fg, fontWeight = FontWeight.Bold)
                                if (g != null) {
                                    Text(
                                        "${g.dayOfMonth}",
                                        color = fg.copy(alpha = 0.8f),
                                        fontSize = 9.sp)
                                }
                                if (h != null) {
                                    Text(
                                        toPersianDigits(h[2].toString()),
                                        color = fg.copy(alpha = 0.8f),
                                        fontSize = 9.sp)
                                }
                                if (evs.isNotEmpty()) {
                                    Text("•", color = fg, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text("مناسبات این ماه", fontWeight = FontWeight.Bold)
        val monthOcc = com.hamyareman.ir.ui.home.CalendarOccasions.monthOccasions(ctx, year, month, dim, offset)
        // پنجشنبه و جمعهٔ بدون رویداد واقعی در «مناسبات این ماه» ردیف ندارند.
        monthOcc.distinctBy { it.first to it.second.title }.forEach { (d, e) ->
            val mark = if (e.holiday) " — تعطیل رسمی" else ""
            Text(
                toPersianDigits("$d ${JalaliDate.monthName(month)}") + " — ${e.title}$mark",
                style = MaterialTheme.typography.bodySmall,
                color = if (e.holiday) Color(0xFF9F1239) else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ShiftSection(onVirtualHours: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val reminders = container.reminders
    val syncScope = rememberCoroutineScope()
    var syncNotice by remember { mutableStateOf<String?>(null) }
    fun syncUid(): String = container.auth.cachedUserId()
        ?: runCatching { kotlinx.coroutines.runBlocking { container.auth.currentUserId() } }.getOrNull().orEmpty()
    var snap by remember { mutableStateOf(ClassPlanStore.load(ctx)) }
    val today = LocalDate.now(JalaliDate.TEHRAN)
    LaunchedEffect(Unit) {
        val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isNotBlank()) {
            val changed = ClassPlanSync.pullAll(ctx, container.tables, uid)
            snap = ClassPlanStore.load(ctx)
            if (changed) syncNotice = "تنظیمات از سرور به‌روز شد."
            ClassPlanSync.pushAll(ctx, container.tables, uid)
        }
    }
    var alarm by remember { mutableStateOf(SchoolAlarmStore.load(ctx)) }
    var settingsOpen by remember { mutableStateOf(false) }
    var shiftSettings by remember { mutableStateOf(false) }
    var ranges by remember { mutableStateOf(ClassPlanStore.virtualRanges(ctx)) }
    val current = ClassPlanStore.shiftOf(snap, today)
    var derivedHelp by remember { mutableStateOf(false) }

    fun flushAlarm(next: SchoolAlarmStore.Prefs = alarm) {
        SchoolAlarmStore.save(ctx, next)
        alarm = SchoolAlarmStore.load(ctx)
        snap = ClassPlanStore.load(ctx)
        ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { shiftSettings = true }, modifier = Modifier.fillMaxWidth()) {
            Text("تنظیمات شیفت مدرسه")
        }
        Text(ClassPlanStore.captionOf(snap, today), fontWeight = FontWeight.Bold)
        ClassPlanStore.nextCycleStartCaption(snap, ctx = ctx)?.let { cap ->
            Text(cap, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("آلارم‌های صدادار", fontWeight = FontWeight.Bold)
            IconButton(onClick = { settingsOpen = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "تنظیمات آلارم")
            }
        }
        Text("شیفت صبح — سه کادر ساعت", fontWeight = FontWeight.Bold)
        DerivedAlarmTime(
            label = "آلارم بیداری شیفت صبح",
            hour = ClassPlanStore.wakeHourMinute(snap, Shift.MORNING).first,
            minute = ClassPlanStore.wakeHourMinute(snap, Shift.MORNING).second,
            onHelp = { derivedHelp = true },
        )
        TimePick("حضور در سرویس", alarm.busMH, alarm.busMM) { h, m -> flushAlarm(alarm.copy(busMH = h, busMM = m)) }
        Text("شیفت ظهر — سه کادر ساعت", fontWeight = FontWeight.Bold)
        DerivedAlarmTime(
            label = "آلارم آماده‌شدن شیفت ظهر",
            hour = ClassPlanStore.wakeHourMinute(snap, Shift.EVENING).first,
            minute = ClassPlanStore.wakeHourMinute(snap, Shift.EVENING).second,
            onHelp = { derivedHelp = true },
        )
        TimePick("حضور در سرویس", alarm.busNH, alarm.busNM) { h, m -> flushAlarm(alarm.copy(busNH = h, busNM = m)) }
        Text("خواب — دعوت به خواب آرام", fontWeight = FontWeight.Bold)
        TimePick("خواب شیفت صبح", alarm.sleepMH, alarm.sleepMM) { h, m -> flushAlarm(alarm.copy(sleepMH = h, sleepMM = m)) }
        TimePick("خواب شیفت ظهر", alarm.sleepNH, alarm.sleepNM) { h, m -> flushAlarm(alarm.copy(sleepNH = h, sleepNM = m)) }
        Text(
            "آلارم شیفت مخالف خاموش می‌شود. اگر دعوت خواب لمس نشود، یک‌بار دیگر بعد از ۵ دقیقه تکرار می‌شود.",
            style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("کلاس مجازی", fontWeight = FontWeight.Bold)
            TextButton(onClick = { onVirtualHours?.invoke() }) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("تنظیم ساعت کلاس‌های مجازی")
            }
        }
        Text(
            "یک روز یا بازه را مجازی کن. روی داشبورد قرمز می‌شود و تیک کیف غیرفعال.",
            style = MaterialTheme.typography.bodySmall)
        VirtualRangeSection(
            today = today,
            ranges = ranges,
            onAdd = { from, to ->
                ClassPlanStore.addVirtualRange(ctx, from, to)
                ranges = ClassPlanStore.virtualRanges(ctx)
                snap = ClassPlanStore.load(ctx)
            },
            onDelete = { id ->
                ClassPlanStore.removeVirtualRange(ctx, id)
                ranges = ClassPlanStore.virtualRanges(ctx)
                snap = ClassPlanStore.load(ctx)
            })
        val vdays = ClassPlanStore.virtualDays(ctx)
        if (vdays.isNotEmpty()) {
            Text(
                "جمعاً ${toPersianDigits(vdays.size.toString())} روز مجازی",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                syncScope.launch {
                    val uid = runCatching { container.auth.currentUserId() }.getOrNull()
                        ?: container.auth.cachedUserId().orEmpty()
                    if (uid.isBlank()) {
                        syncNotice = "برای همگام‌سازی باید وارد حساب شوی."
                        return@launch
                    }
                    val pulled = ClassPlanSync.pullAll(ctx, container.tables, uid)
                    val pushed = ClassPlanSync.pushAll(ctx, container.tables, uid)
                    snap = ClassPlanStore.load(ctx)
                    syncNotice = buildString {
                        append(if (pulled) "از سرور گرفته شد" else "داده‌ی تازه‌ای در سرور نبود")
                        append(if (pushed) "؛ ارسال انجام شد." else "؛ چیزی برای ارسال نبود.")
                    }
                }
            }) { Text("همگام‌سازی با سرور") }
        }
        syncNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        val sessions = ClassPlanStore.virtualSessions(ctx)
        if (sessions.isNotEmpty()) {
            Text("ساعت‌های ثبت‌شده", fontWeight = FontWeight.Bold)
            sessions.forEach { s ->
                val dayName = ClassPlanStore.WEEKDAYS.getOrElse(s.dayIndex - 1) { "" }
                Text(
                    "$dayName: ${s.timeFa}${if (s.subject.isBlank()) "" else " — ${s.subject}"}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (derivedHelp) {
        AlertDialog(
            onDismissRequest = { derivedHelp = false },
            title = { Text("زمان‌های محاسبه‌شده") },
            text = {
                Text(
                    "این دو زمان قابل تنظیم مستقیم نیستند. ابتدا شیفت مدرسه و ساعت حضور همان شیفت تعیین می‌شود؛ سپس زمان آماده‌سازی پیش از حرکت از آن کم می‌شود. " +
                        "تغییر ساعت حضور یا مدت آماده‌سازی، هر دو آلارم بیداری را خودکار دوباره محاسبه می‌کند."
                )
            },
            confirmButton = {
                TextButton(onClick = { derivedHelp = false }) { Text("متوجه شدم") }
            },
        )
    }
    if (shiftSettings) {
        AlertDialog(
            onDismissRequest = { shiftSettings = false },
            title = { Text("تنظیمات شیفت مدرسه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ── گام ۱: شیفتِ هفتهٔ جاری
                    Text("۱) شیفت هفتهٔ جاری", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Shift.MORNING, Shift.EVENING).forEach { sh ->
                            FilterChip(
                                selected = current == sh,
                                onClick = {
                                    ClassPlanStore.setCurrentWeekShift(ctx, sh)
                                    snap = ClassPlanStore.load(ctx)
                                },
                                label = { Text(sh.label) })
                        }
                    }
                    // ── گام ۲: چرخهٔ شیفت
                    Text("۲) چرخهٔ شیفت", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "ثابت", 2 to "دوهفته‌ای", 4 to "چهارهفته‌ای").forEach { (w, label) ->
                            FilterChip(
                                selected = snap.cycleWeeks == w,
                                onClick = {
                                    ClassPlanStore.setCycleWeeks(ctx, w)
                                    snap = ClassPlanStore.load(ctx)
                                },
                                label = { Text(label) })
                        }
                    }
                    // ── گام ۳: شیفتِ هر هفته (مشتق از گام ۱)
                    when (snap.cycleWeeks) {
                        1 -> {
                            Text("۳) شیفت ثابت", fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = {
                                    ClassPlanStore.setCurrentWeekShift(ctx, current)
                                    snap = ClassPlanStore.load(ctx)
                                },
                                modifier = Modifier.fillMaxWidth()) {
                                Text("همیشه ${current.label}")
                            }
                        }
                        else -> {
                            val nowPos = ClassPlanStore.cycleWeekPos(snap, today)
                            Text("۳) هفتهٔ جاری از ${current.label}", fontWeight = FontWeight.Bold)
                            (1..snap.cycleWeeks).forEach { k ->
                                FilterChip(
                                    selected = nowPos == k,
                                    onClick = {
                                        ClassPlanStore.setCycleWeekOffset(ctx, k)
                                        snap = ClassPlanStore.load(ctx)
                                    },
                                    label = {
                                        Text("${ClassPlanStore.weekOrdinal(k)} هفته از ${current.label}")
                                    })
                            }
                        }
                    }
                    TimePick("ساعت ورود شیفت صبح", snap.morningHour, snap.morningMinute) { h, m ->
                        ClassPlanStore.saveTimes(
                            ctx, h, m, snap.wakeLeadMin,
                            snap.noonHour, snap.noonMinute,
                            snap.sleepMorning, snap.sleepEvening)
                        snap = ClassPlanStore.load(ctx)
                    }
                    TimePick("ساعت ورود شیفت ظهر", snap.noonHour, snap.noonMinute) { h, m ->
                        ClassPlanStore.saveTimes(
                            ctx, snap.morningHour, snap.morningMinute, snap.wakeLeadMin,
                            h, m, snap.sleepMorning, snap.sleepEvening)
                        snap = ClassPlanStore.load(ctx)
                    }
                    Text("ساعت خروج از مدرسه", fontWeight = FontWeight.Bold)
                    TimePickText("خروج شیفت صبح", snap.exitMorning) { hhmm ->
                        ClassPlanStore.saveExitTimes(ctx, hhmm, snap.exitNoon)
                        snap = ClassPlanStore.load(ctx)
                    }
                    TimePickText("خروج شیفت ظهر", snap.exitNoon) { hhmm ->
                        ClassPlanStore.saveExitTimes(ctx, snap.exitMorning, hhmm)
                        snap = ClassPlanStore.load(ctx)
                    }
                    Text(
                        "آماده‌سازیِ پیش از حرکت: ${toPersianDigits(snap.wakeLeadMin.toString())} دقیقه")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 45, 60, 90).forEach { lead ->
                            FilterChip(
                                selected = snap.wakeLeadMin == lead,
                                onClick = {
                                    ClassPlanStore.saveTimes(
                                        ctx, snap.morningHour, snap.morningMinute, lead,
                                        snap.noonHour, snap.noonMinute,
                                        snap.sleepMorning, snap.sleepEvening)
                                    snap = ClassPlanStore.load(ctx)
                                },
                                label = { Text(toPersianDigits(lead.toString())) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    shiftSettings = false
                    ClassPlanStore.syncAlarms(ctx, reminders, snap, today)
                    syncScope.launch {
                        val uid = syncUid()
                        ClassPlanSync.push(ctx, container.tables, uid, StateSync.KEY_SHIFT)
                        ClassPlanSync.push(ctx, container.tables, uid, StateSync.KEY_WEEK)
                        syncNotice = if (uid.isBlank()) "تنظیمات روی دستگاه ذخیره شد (برای سینک وارد شو)." else "تنظیمات ذخیره و با سرور همگام شد."
                    }
                }) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { shiftSettings = false }) { Text("بستن") } })
    }
    if (derivedHelp) {
        AlertDialog(
            onDismissRequest = { derivedHelp = false },
            title = { Text("آلارم خودکار") },
            text = {
                Text(
                    "این دو زمان عمداً قابل تنظیم مستقیم نیستند. زمان بیداری از ساعت حضور همان شیفت منهای مدت آماده‌سازی قبل از حرکت به دست می‌آید. بنابراین تغییر ساعت ورود شیفت یا مدت آماده‌سازی، زمان بیداری همان شیفت و آلارم واقعی آن را هم‌زمان تغییر می‌دهد.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { TextButton(onClick = { derivedHelp = false }) { Text("متوجه شدم") } },
        )
    }
    if (settingsOpen) {
        val sounds = remember(ctx) { AlarmRinger.deviceSounds(ctx) }
        var soundUri by remember { mutableStateOf(AlarmRinger.savedSound(ctx)) }
        AlertDialog(
            onDismissRequest = { settingsOpen = false },
            title = { Text("تنظیمات آلارم") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "آهنگ: ${AlarmRinger.titleOf(ctx, soundUri)}",
                        fontWeight = FontWeight.Bold)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState())) {
                        AlarmSoundRow(
                            label = "پیش‌فرض گوشی",
                            selected = soundUri.isBlank(),
                            onPick = {
                                soundUri = ""
                                AlarmRinger.saveSound(ctx, "")
                                flushAlarm(alarm.copy(sound = "default"))
                            },
                            onPreview = { AlarmRinger.preview(ctx, "", alarm.volume) })
                        sounds.forEach { (name, uri) ->
                            AlarmSoundRow(
                                label = name,
                                selected = soundUri == uri,
                                onPick = {
                                    soundUri = uri
                                    AlarmRinger.saveSound(ctx, uri)
                                    flushAlarm(alarm.copy(sound = uri))
                                },
                                onPreview = { AlarmRinger.preview(ctx, uri, alarm.volume) })
                        }
                        if (sounds.isEmpty()) {
                            Text(
                                "آهنگی روی گوشی پیدا نشد؛ همان پیش‌فرض سیستم زنگ می‌زند.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("بلندی: ${toPersianDigits(alarm.volume.toString())}٪")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(40, 60, 80, 100).forEach { v ->
                            FilterChip(selected = alarm.volume == v, onClick = { flushAlarm(alarm.copy(volume = v)) }, label = { Text(toPersianDigits(v.toString())) })
                        }
                    }
                    Text("تعداد تکرار")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { n ->
                            FilterChip(selected = alarm.repeat == n, onClick = { flushAlarm(alarm.copy(repeat = n)) }, label = { Text(toPersianDigits(n.toString())) })
                        }
                    }
                    FilterChip(
                        selected = alarm.crescendo,
                        onClick = { flushAlarm(alarm.copy(crescendo = !alarm.crescendo)) },
                        label = { Text("صدای افزایشی") })
                    OutlinedButton(
                        onClick = { AlarmRinger.start(ctx) },
                        modifier = Modifier.fillMaxWidth()) { Text("تست زنگ") }
                    TextButton(onClick = { AlarmRinger.stop() }, modifier = Modifier.fillMaxWidth()) {
                        Text("توقف زنگ")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { AlarmRinger.stop(); settingsOpen = false }) { Text("بستن") } })
    }
}

@Composable
internal fun TimePick(label: String, hour: Int, minute: Int, onChange: (Int, Int) -> Unit) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            android.app.TimePickerDialog(ctx, { _, h, m -> onChange(h, m) }, hour, minute, true).show()
        },
        modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label  ${toPersianDigits("%d:%02d".format(hour, minute))}")
    }
}

@Composable
private fun AlarmSoundRow(
    label: String,
    selected: Boolean,
    onPick: () -> Unit,
    onPreview: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onPick)
        Text(
            label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        TextButton(onClick = onPreview) { Text("شنیدن") }
    }
}

/**
 * بخش «مرخصی» در انتهای کادرِ برنامهٔ هفتگی:
 * بازه با تقویم شمسی، علت (با امکانِ افزودن علتِ خاص)، گواهی پزشکی (فقط مریضی)
 * و وضعیتِ توجیه (فقط یکی و فقط یک‌بار — مگر «موجّه نشده» که قابلِ تغییر می‌ماند).
 * مرخصی‌های ثبت‌شده در یک آکاردیونِ **پیش‌فرض بسته** فهرست می‌شوند.
 */
@Composable
internal fun LeaveSection() {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val syncScope = rememberCoroutineScope()
    val today = LocalDate.now(JalaliDate.TEHRAN)

    var records by remember { mutableStateOf(ClassPlanStore.leaves(ctx)) }
    var fromIso by remember { mutableStateOf(today.toString()) }
    var toIso by remember { mutableStateOf(today.toString()) }
    var reason by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf("") }
    var showCustom by remember { mutableStateOf(false) }
    var medCert by remember { mutableStateOf(false) }
    var just by remember { mutableStateOf("") }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    var reasonsOpen by remember { mutableStateOf(false) }
    var listOpen by remember { mutableStateOf(false) } // پیش‌فرض بسته
    var msg by remember { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    val reasons = remember(records, showCustom) { ClassPlanStore.leaveReasons(ctx) }
    val isSick = reason == ClassPlanStore.SICK

    fun pushLeaves() {
        syncScope.launch {
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isBlank()) return@launch
            ClassPlanSync.push(ctx, container.tables, uid, StateSync.KEY_LEAVES)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("مرخصی", fontWeight = FontWeight.Bold)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickFrom = true }, modifier = Modifier.weight(1f)) {
                Text("از: ${JalaliDate.formatFaLong(fromIso)}")
            }
            OutlinedButton(onClick = { pickTo = true }, modifier = Modifier.weight(1f)) {
                Text("تا: ${JalaliDate.formatFaLong(toIso)}")
            }
        }

        // علتِ مرخصی
        Box {
            OutlinedButton(onClick = { reasonsOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (reason.isBlank()) "علت مرخصی" else reason)
            }
            DropdownMenu(expanded = reasonsOpen, onDismissRequest = { reasonsOpen = false }) {
                reasons.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r) },
                        onClick = { reason = r; medCert = false; just = ""; reasonsOpen = false })
                }
                DropdownMenuItem(
                    text = { Text(ClassPlanStore.ADD_CUSTOM) },
                    onClick = { showCustom = true; reasonsOpen = false })
            }
        }
        if (showCustom) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("علتِ خاص") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    if (custom.isBlank()) return@TextButton
                    ClassPlanStore.addLeaveReason(ctx, custom)
                    reason = custom.trim()
                    medCert = false
                    just = ""
                    custom = ""
                    showCustom = false
                    msg = "علت اضافه شد."
                }) { Text("افزودن") }
                TextButton(onClick = { showCustom = false; custom = "" }) { Text("انصراف") }
            }
        }

        // گواهی پزشکی — فقط برای مریضی
        if (isSick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = medCert, onCheckedChange = { medCert = it })
                Text("گواهی پزشکی داشتم")
            }
        }

        // وضعیتِ توجیه — فقط یکی، و فقط یک‌بار (مگر «موجّه نشده»)
        if (reason.isNotBlank()) {
            Text("وضعیت توجیه به مدرسه", fontWeight = FontWeight.Bold)
            val options = buildList {
                add(ClassPlanStore.JUST_FATHER)
                add(ClassPlanStore.JUST_MOTHER)
                if (isSick) add(ClassPlanStore.JUST_MEDICAL)
                add(ClassPlanStore.JUST_NONE)
            }
            options.forEach { code ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { if (just.isBlank() || just == ClassPlanStore.JUST_NONE) just = code }) {
                    Checkbox(
                        checked = just == code,
                        enabled = just.isBlank() || just == ClassPlanStore.JUST_NONE,
                        onCheckedChange = { if (it) just = code })
                    Text(ClassPlanStore.justificationLabel(code))
                }
            }
        }

        Button(onClick = {
            msg = when {
                reason.isBlank() -> "علت مرخصی را انتخاب کن."
                toIso < fromIso -> "روزِ پایان نمی‌تواند پیش از روزِ شروع باشد."
                just.isBlank() -> "وضعیت توجیه به مدرسه را انتخاب کن."
                else -> {
                    ClassPlanStore.addLeave(ctx, fromIso, toIso, reason, medCert, just)
                    records = ClassPlanStore.leaves(ctx)
                    just = ""
                    medCert = false
                    reason = ""
                    pushLeaves()
                    "مرخصی ثبت شد."
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("ثبت مرخصی") }

        msg?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        // ---- آکاردیونِ مرخصی‌های ثبت‌شده (پیش‌فرض بسته) ----
        Row(
            Modifier.fillMaxWidth().clickable { listOpen = !listOpen }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(if (listOpen) "▾" else "◂")
            Text(
                "مرخصی‌های ثبت‌شده (${toPersianDigits(records.size.toString())})",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp))
        }
        if (listOpen) {
            if (records.isEmpty()) {
                Text("هنوز مرخصی‌ای ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
            } else {
                records.forEach { rec ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val days = ClassPlanStore.daysBetween(rec.fromIso, rec.toIso)
                        days.take(10).forEach { iso ->
                            Text(
                                "${JalaliDate.weekDayFa(iso)} ${JalaliDate.formatFaLong(iso)} — ${rec.reason} — ${ClassPlanStore.justificationLabel(rec.justification)}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (days.size > 10) {
                            Text(
                                "و ${toPersianDigits((days.size - 10).toString())} روز دیگر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        // اگر «موجّه نشده» ثبت شده، بعداً هم می‌توان آن را اصلاح کرد.
                        if (rec.justification == ClassPlanStore.JUST_NONE) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(ClassPlanStore.JUST_FATHER, ClassPlanStore.JUST_MOTHER)
                                    .plus(if (rec.reason == ClassPlanStore.SICK) listOf(ClassPlanStore.JUST_MEDICAL) else emptyList())
                                    .forEach { code ->
                                        TextButton(onClick = {
                                            ClassPlanStore.updateLeaveJustification(ctx, rec.id, code)
                                            records = ClassPlanStore.leaves(ctx)
                                            pushLeaves()
                                        }) { Text(ClassPlanStore.justificationLabel(code)) }
                                    }
                            }
                        }
                        TextButton(onClick = { deleteId = rec.id }) {
                            Text("حذف این مرخصی", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (pickFrom) {
        ShamsiDatePickerDialog(
            initialIso = fromIso,
            title = "از روز",
            onDismiss = { pickFrom = false },
            onPick = { fromIso = it; if (it > toIso) toIso = it; pickFrom = false })
    }
    if (pickTo) {
        ShamsiDatePickerDialog(
            initialIso = toIso,
            title = "تا روز",
            onDismiss = { pickTo = false },
            onPick = { toIso = it; pickTo = false })
    }
    if (deleteId != null) {
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("حذف مرخصی؟") },
            text = { Text("این مرخصی از فهرست پاک می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteId?.let { ClassPlanStore.removeLeave(ctx, it) }
                    deleteId = null
                    records = ClassPlanStore.leaves(ctx)
                    pushLeaves()
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("انصراف") } })
    }
}
