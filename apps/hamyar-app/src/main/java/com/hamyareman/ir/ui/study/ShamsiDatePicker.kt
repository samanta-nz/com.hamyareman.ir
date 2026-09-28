package com.hamyareman.ir.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.AppTypography
import java.time.LocalDate

/**
 * پاپ‌آپ تقویم شمسی برای انتخاب یک روز. خروجی به‌صورت ISO میلادی است
 * (همان قراردادی که بقیه‌ی برنامه برای تاریخ‌ها استفاده می‌کند).
 */
@Composable
fun ShamsiDatePickerDialog(
    initialIso: String,
    title: String = "انتخاب تاریخ",
    onDismiss: () -> Unit,
    onPick: (String) -> Unit) {
    val startJ = JalaliDate.toJalali(initialIso) ?: JalaliDate.todayJalali()
    var year by remember { mutableIntStateOf(startJ.year) }
    var month by remember { mutableIntStateOf(startJ.month) }
    var selectedDay by remember { mutableIntStateOf(startJ.day) }
    val todayJ = JalaliDate.todayJalali()
    val dim = JalaliDate.daysInMonth(year, month)

    fun isoOf(day: Int): String? = JalaliDate.toGregorianIso(JalaliDate.Jalali(year, month, day))

    val firstIso = JalaliDate.toGregorianIso(JalaliDate.Jalali(year, month, 1))
    val firstDow = firstIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let { (SchoolShift.dayIndex(it) - 1).coerceIn(0, 6) } ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (month == 1) { month = 12; year-- } else month--
                        selectedDay = selectedDay.coerceAtMost(JalaliDate.daysInMonth(year, month))
                    }) { Text("◀") }
                    Text(
                        "${JalaliDate.monthName(month)} ${toPersianDigits(year.toString())}",
                        fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        if (month == 12) { month = 1; year++ } else month++
                        selectedDay = selectedDay.coerceAtMost(JalaliDate.daysInMonth(year, month))
                    }) { Text("▶") }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { h ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                h,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val cells = List(firstDow) { 0 } + (1..dim)
                val rows = cells.chunked(7)
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { day ->
                            if (day == 0) {
                                Spacer(Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val isToday = year == todayJ.year && month == todayJ.month && day == todayJ.day
                                val selected = day == selectedDay
                                val bg = when {
                                    selected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else -> Color.Transparent
                                }
                                val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .then(
                                            if (isToday && !selected) {
                                                Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(8.dp))
                                            } else Modifier)
                                        .clickable { selectedDay = day },
                                    contentAlignment = Alignment.Center) {
                                    Text(
                                        toPersianDigits(day.toString()),
                                        color = fg)
                                }
                            }
                        }
                        repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Text(
                    "انتخاب‌شده: ${JalaliDate.weekDayFa(isoOf(selectedDay) ?: initialIso)} " +
                        toPersianDigits(
                            "%d %s %d".format(selectedDay, JalaliDate.monthName(month), year)),
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val iso = isoOf(selectedDay)
                if (iso != null) onPick(iso)
                onDismiss()
            }) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}
