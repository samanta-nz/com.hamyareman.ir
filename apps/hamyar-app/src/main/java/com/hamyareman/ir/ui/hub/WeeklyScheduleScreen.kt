package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.ui.study.StateSync
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Days = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
private val Kinds = listOf("درس", "تکلیف", "مرور", "ورزش", "استراحت", "آزاد")
private val KindColors = listOf(
    Color(0xFF0F766E),
    Color(0xFFB45309),
    Color(0xFF4338CA),
    Color(0xFFBE185D),
    Color(0xFF0369A1),
    Color(0xFF334155),
)
private val Hours = (6..22).toList()

private data class PlanBlock(
    val id: String,
    val day: Int,
    val startH: Int,
    val endH: Int,
    val title: String,
    val kind: String,
    val note: String,
)

private const val KEY = "week_plan_blocks"

private fun readBlocks(store: LocalStore): List<PlanBlock> = runCatching {
    val arr = JSONArray(store.getString(KEY, "[]"))
    buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(
                PlanBlock(
                    id = o.optString("id"),
                    day = o.optInt("day"),
                    startH = o.optInt("startH", 8),
                    endH = o.optInt("endH", 9),
                    title = o.optString("title"),
                    kind = o.optString("kind", "درس"),
                    note = o.optString("note"),
                ),
            )
        }
    }
}.getOrDefault(emptyList())

private fun writeBlocks(store: LocalStore, list: List<PlanBlock>) {
    val arr = JSONArray()
    list.forEach { b ->
        arr.put(
            JSONObject()
                .put("id", b.id).put("day", b.day).put("startH", b.startH).put("endH", b.endH)
                .put("title", b.title).put("kind", b.kind).put("note", b.note),
        )
    }
    store.putString(KEY, arr.toString())
}

private fun todayDayIndex(): Int {
    val iso = LocalDate.now(JalaliDate.TEHRAN).toString()
    val name = JalaliDate.weekDayFa(iso)
    return Days.indexOf(name).coerceAtLeast(0)
}

/**
 * برنامهٔ شخصی هفتگی — صفحهٔ قبلی (شیفت صبح/عصر/شب + کامای درس‌ها) حذف شد.
 * جدول زمانی شنبه تا جمعه، دسته‌بندی، جمع ساعات، کپی روز، و افزودن سریع از کتاب‌ها.
 */
@Composable
fun WeeklyScheduleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val store = remember { LocalStore(context, "hamyar_week_plan") }
    var blocks by remember { mutableStateOf(readBlocks(store)) }
    var day by remember { mutableIntStateOf(todayDayIndex()) }
    var editor by remember { mutableStateOf<PlanBlock?>(null) }
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("درس") }
    var startH by remember { mutableIntStateOf(16) }
    var endH by remember { mutableIntStateOf(17) }
    var note by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }

    fun payloadOf(list: List<PlanBlock>): String {
        val arr = JSONArray()
        list.forEach { b ->
            arr.put(
                JSONObject()
                    .put("id", b.id).put("day", b.day).put("startH", b.startH).put("endH", b.endH)
                    .put("title", b.title).put("kind", b.kind).put("note", b.note),
            )
        }
        return arr.toString()
    }

    fun save(list: List<PlanBlock>) {
        blocks = list
        writeBlocks(store, list)
        StateSync.markLocal(context, StateSync.KEY_WEEK_PLAN)
        scope.launch {
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isNotBlank()) StateSync.push(context, container.tables, uid, StateSync.KEY_WEEK_PLAN, payloadOf(list))
        }
    }

    LaunchedEffect(Unit) {
        val uid = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isBlank()) return@LaunchedEffect
        val remote = StateSync.pull(context, container.tables, uid, StateSync.KEY_WEEK_PLAN)
        val local = payloadOf(readBlocks(store))
        if (remote != null && remote.first.isNotBlank() && remote.first != local &&
            remote.second >= StateSync.localAt(context, StateSync.KEY_WEEK_PLAN)
        ) {
            store.putString(KEY, remote.first)
            blocks = readBlocks(store)
            StateSync.markSyncedAt(context, StateSync.KEY_WEEK_PLAN, remote.second)
        } else if (local != "[]") {
            StateSync.push(context, container.tables, uid, StateSync.KEY_WEEK_PLAN, local)
        }
    }

    val todayBlocks = blocks.filter { it.day == day }.sortedBy { it.startH }
    val weekMinutes = blocks.sumOf { (it.endH - it.startH).coerceAtLeast(0) * 60 }
    val studyH = blocks.filter { it.kind == "درس" || it.kind == "مرور" }.sumOf { (it.endH - it.startH).coerceAtLeast(0) }
    val hwN = blocks.count { it.kind == "تکلیف" }
    val sportN = blocks.count { it.kind == "ورزش" }
    val subjects = remember {
        BookModuleRegistry.modules.filter { com.hamyareman.ir.ui.profile.GradeGate.canSeeBook(it.bookCode) }
            .map { it.title }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("برنامهٔ هفتگی من", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "برنامهٔ شخصی روزانه‌ات اینجاست — جدا از برنامهٔ کلاسی و شیفت مدرسه. درس، تکلیف، مرور، ورزش و استراحت را روی ساعت بچین.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMini("ساعت مطالعه", toPersianDigits(studyH.toString()), Modifier.weight(1f))
                StatMini("تکلیف", toPersianDigits(hwN.toString()), Modifier.weight(1f))
                StatMini("ورزش", toPersianDigits(sportN.toString()), Modifier.weight(1f))
            }
            Text(
                "جمع بلوک‌های هفته: ${toPersianDigits((weekMinutes / 60).toString())} ساعت",
                style = MaterialTheme.typography.labelMedium,
            )

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Days.forEachIndexed { i, name ->
                    val selected = i == day
                    val n = blocks.count { it.day == i }
                    val isToday = i == todayDayIndex()
                    Column(
                        Modifier
                            .width(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    selected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                            )
                            .clickable { day = i }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            name,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            if (n == 0) "خالی" else "${toPersianDigits(n.toString())} مورد",
                            color = if (selected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Text("جدول ${Days[day]}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (todayBlocks.isEmpty()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("این روز هنوز خالی است.", fontWeight = FontWeight.Bold)
                        Text("پیشنهاد: ۱۶–۱۷ مرور ریاضی · ۱۷–۱۸ تکلیف · ۱۸–۱۹ ورزش یا استراحت.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Hours.forEach { h ->
                val covering = todayBlocks.filter { h >= it.startH && h < it.endH }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        toPersianDigits("%02d:00".format(h)),
                        modifier = Modifier.width(52.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        covering.firstOrNull()?.let { b ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(KindColors[Kinds.indexOf(b.kind).coerceAtLeast(0)].copy(alpha = 0.9f))
                                    .clickable {
                                        editor = b
                                        title = b.title; kind = b.kind; startH = b.startH; endH = b.endH; note = b.note
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text("${b.kind} · ${b.title}", color = Color.White, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            PrimaryButton("افزودن به ${Days[day]}") {
                editor = PlanBlock("new", day, 16, 17, "", "درس", "")
                title = ""; kind = "درس"; startH = 16; endH = 17; note = ""
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val src = (day + 6) % 7
                        val now = System.currentTimeMillis()
                        val copied = blocks.filter { it.day == src }.mapIndexed { i, b ->
                            b.copy(id = "b_${now}_$i", day = day)
                        }
                        if (copied.isEmpty()) notice = "روز قبل خالی بود."
                        else {
                            save(blocks.filterNot { it.day == day } + copied)
                            notice = "از ${Days[src]} کپی شد."
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("کپی از روز قبل") }
                OutlinedButton(
                    onClick = {
                        save(blocks.filterNot { it.day == day })
                        notice = "این روز پاک شد."
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("پاک کردن روز") }
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

            Text("راهنمای رنگ‌ها", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Kinds.forEachIndexed { i, k ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(KindColors[i]))
                        Spacer(Modifier.width(4.dp))
                        Text(k, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    editor?.let { current ->
        AlertDialog(
            onDismissRequest = { editor = null },
            title = { Text(if (current.id == "new") "بلوک تازه" else "ویرایش بلوک") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("نوع", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Kinds.forEach { k ->
                            FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k) })
                        }
                    }
                    if (kind == "درس" && subjects.isNotEmpty()) {
                        Text("از کتاب‌ها", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            subjects.forEach { s ->
                                FilterChip(selected = title == s, onClick = { title = s }, label = { Text(s) })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { startH = (startH - 1).coerceIn(6, 21) }) { Text("شروع ${toPersianDigits(startH.toString())}") }
                        OutlinedButton(onClick = { startH = (startH + 1).coerceIn(6, 21); if (endH <= startH) endH = startH + 1 }) { Text("+") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { endH = (endH - 1).coerceIn(startH + 1, 23) }) { Text("پایان ${toPersianDigits(endH.toString())}") }
                        OutlinedButton(onClick = { endH = (endH + 1).coerceIn(startH + 1, 23) }) { Text("+") }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("یادداشت کوتاه") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = if (current.id == "new") "b_${System.currentTimeMillis()}" else current.id
                    val block = PlanBlock(id, day, startH, endH.coerceAtLeast(startH + 1), title.trim().ifBlank { kind }, kind, note.trim())
                    val rest = blocks.filterNot { it.id == id }
                    save(rest + block)
                    editor = null
                }) { Text("ذخیره") }
            },
            dismissButton = {
                Row {
                    if (current.id != "new") {
                        TextButton(onClick = {
                            save(blocks.filterNot { it.id == current.id })
                            editor = null
                        }) { Text("حذف") }
                    }
                    TextButton(onClick = { editor = null }) { Text("انصراف") }
                }
            },
        )
    }
}

@Composable
private fun StatMini(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = AppTypography.h2, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
