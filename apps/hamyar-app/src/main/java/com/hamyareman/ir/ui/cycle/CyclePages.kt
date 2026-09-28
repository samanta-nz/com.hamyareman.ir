package com.hamyareman.ir.ui.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.navigation.Screen
import org.json.JSONArray
import java.time.LocalDate

private val PhasePeriod = Color(0xFFE8A0B0)
private val PhasePms = Color(0xFFD4B483)
private val PhaseMid = Color(0xFF8FBF9F)
private val PhaseAfter = Color(0xFFA7C4E8)

@Composable
fun CycleCalScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf(MonthlyCycle.load(ctx)) }
    val todayJ = JalaliDate.todayJalali()
    var y by remember { mutableIntStateOf(todayJ.year) }
    var m by remember { mutableIntStateOf(todayJ.month) }

    HubBody {
        HubHeader("تقویم ماهانه با فازبندی", "روز را بزن تا پریود ثبت شود", onBack)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (m == 1) { m = 12; y -= 1 } else m -= 1
            }) { Text("ماه قبل") }
            Text(
                JalaliDate.monthName(m) + " " + toPersianDigits(y.toString()),
                style = AppTypography.pageHeading.style,
            )
            TextButton(onClick = {
                if (m == 12) { m = 1; y += 1 } else m += 1
            }) { Text("ماه بعد") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach {
                Text(it, style = AppTypography.pageBody.style, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val days = JalaliDate.daysInMonth(y, m)
        val firstIso = JalaliDate.toGregorianIso(JalaliDate.Jalali(y, m, 1)) ?: ""
        val firstDow = runCatching { LocalDate.parse(firstIso).dayOfWeek.value }.getOrDefault(6) // 1=Mon
        val startPad = when (firstDow) {
            6 -> 0 // Sat
            7 -> 1 // Sun
            else -> firstDow + 1
        }
        val cells = List(startPad) { null } + (1..days).toList()
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (day != null) {
                            val iso = JalaliDate.toGregorianIso(JalaliDate.Jalali(y, m, day)) ?: return@Box
                            val phase = MonthlyCycle.phase(state, iso)
                            val marked = iso in state.periodDays
                            val bg = when (phase) {
                                MonthlyCycle.Phase.PERIOD -> PhasePeriod
                                MonthlyCycle.Phase.PMS -> PhasePms
                                MonthlyCycle.Phase.OVULATION -> PhaseMid
                                MonthlyCycle.Phase.FOLLICULAR -> PhaseAfter
                                MonthlyCycle.Phase.LUTEAL -> PhasePms.copy(alpha = 0.55f)
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg.copy(alpha = if (marked) 1f else 0.45f))
                                    .then(
                                        if (iso == JalaliDate.todayIso()) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                        else Modifier,
                                    )
                                    .clickable {
                                        val next = state.periodDays.toMutableSet()
                                        if (iso in next) next.remove(iso) else next.add(iso)
                                        state = state.copy(periodDays = next, lastStart = next.minOrNull() ?: state.lastStart)
                                        MonthlyCycle.saveLocal(ctx, state)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(toPersianDigits(day.toString()), style = AppTypography.pageBody.style)
                            }
                        }
                    }
                }
                repeat(7 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("صورتی پریود · طلایی پیش‌قاعدگی · سبز میانه · آبی پس از قاعدگی", style = AppTypography.pageBody.style)
        Text("امروز را با حاشیه می‌بینی. لمس روز = ثبت یا برداشتن پریود.", style = AppTypography.pageBody.style)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CycleLogScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { LocalStore(ctx, "hamyar_cycle") }
    val today = JalaliDate.todayIso()
    val key = "log_$today"
    var pain by remember { mutableIntStateOf(store.getInt("${key}_pain", 0)) }
    var note by remember { mutableStateOf(store.getString("${key}_note", "")) }
    var tags by remember {
        mutableStateOf(runCatching {
            val a = JSONArray(store.getString("${key}_tags", "[]"))
            buildSet { for (i in 0 until a.length()) add(a.getString(i)) }
        }.getOrDefault(emptySet()))
    }
    val options = listOf("درد شکم", "کمر", "خلق پایین", "خلق بالا", "جریان", "سردرد", "نفخ", "خستگی", "گرسنگی")

    HubBody {
        HubHeader("علائم و یادداشت روزانه", JalaliDate.formatFaLong(today), onBack)
        Text("هر علامتی که امروز هست را لمس کن. شدت درد را جدا بگو.", style = AppTypography.pageBody.style)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { t ->
                FilterChip(
                    selected = t in tags,
                    onClick = {
                        tags = if (t in tags) tags - t else tags + t
                    },
                    label = { Text(t) },
                )
            }
        }
        Text("شدت درد: " + if (pain == 0) "ندارم" else toPersianDigits(pain.toString()) + " از ۵", style = AppTypography.pageHeading.style)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..5).forEach { n ->
                val on = pain == n
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (on) PhasePeriod else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { pain = n }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(toPersianDigits(n.toString())) }
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { if (it.length <= 400) note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("یادداشت خصوصی امروز") },
            minLines = 4,
        )
        PrimaryButton("ذخیرهٔ امروز") {
            store.putInt("${key}_pain", pain)
            store.putString("${key}_note", note)
            val arr = JSONArray(); tags.forEach { arr.put(it) }
            store.putString("${key}_tags", arr.toString())
        }
    }
}

@Composable
fun CycleTodayScreen(nav: NavController, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val state = remember { MonthlyCycle.load(ctx) }
    val iso = JalaliDate.todayIso()
    val (title, body) = MonthlyCycle.todayCard(state, iso)
    val phase = MonthlyCycle.phase(state, iso)
    HubBody {
        HubHeader("امروز بدنت چی می‌خواد", JalaliDate.formatFaLong(iso), onBack)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    when (phase) {
                        MonthlyCycle.Phase.PERIOD -> PhasePeriod
                        MonthlyCycle.Phase.PMS -> PhasePms
                        MonthlyCycle.Phase.OVULATION -> PhaseMid
                        MonthlyCycle.Phase.FOLLICULAR -> PhaseAfter
                        MonthlyCycle.Phase.LUTEAL -> PhasePms.copy(alpha = 0.6f)
                    },
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = AppTypography.pageHeading.style)
                Text(body, style = AppTypography.pageBody.style)
            }
        }
        Text("یک کار کوچک برای همین فاز", style = AppTypography.pageHeading.style)
        PrimaryButton("شروع جلسهٔ تنفس درد") { nav.layerTo(Screen.PracticeGroup.of("pd-breath")) }
        PrimaryButton("شروع جلسهٔ یوگای ملایم") { nav.layerTo(Screen.PracticeGroup.of("pd-yoga")) }
        PrimaryButton("شروع جلسهٔ کشش و گرما") { nav.layerTo(Screen.PracticeGroup.of("pd-stretch")) }
        PrimaryButton("راهنمای غذای این فاز") { nav.layerTo(Screen.PracticeGroup.of("pd-food")) }
    }
}
