package com.hamyareman.ir.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits

/**
 * انتخاب تاریخ تولد شمسی (سال / ماه / روز). سن از روی همین تاریخ حساب می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JalaliBirthDateFields(
    year: Int?,
    month: Int?,
    day: Int?,
    onChange: (year: Int?, month: Int?, day: Int?) -> Unit,
    isError: Boolean = false,
) {
    val today = remember { JalaliDate.todayJalali() }
    val years = remember(today.year) { (today.year - 80..today.year - 5).toList().asReversed() }
    val dim = if (year != null && month != null) JalaliDate.daysInMonth(year, month) else 31
    val days = remember(dim) { (1..dim).toList() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("تاریخ تولد شمسی *", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BirthDropdown(
                modifier = Modifier.weight(1.1f),
                label = "سال",
                value = year?.let { toPersianDigits(it.toString()) }.orEmpty(),
                options = years.map { it to toPersianDigits(it.toString()) },
                isError = isError && year == null,
                onPick = { y ->
                    val d = clampDay(y, month, day)
                    onChange(y, month, d)
                },
            )
            BirthDropdown(
                modifier = Modifier.weight(1.4f),
                label = "ماه",
                value = month?.let { JalaliDate.monthName(it) }.orEmpty(),
                options = (1..12).map { it to JalaliDate.monthName(it) },
                isError = isError && month == null,
                onPick = { m ->
                    val d = clampDay(year, m, day)
                    onChange(year, m, d)
                },
            )
            BirthDropdown(
                modifier = Modifier.weight(1f),
                label = "روز",
                value = day?.let { toPersianDigits(it.toString()) }.orEmpty(),
                options = days.map { it to toPersianDigits(it.toString()) },
                isError = isError && day == null,
                onPick = { d -> onChange(year, month, d) },
            )
        }
        if (isError) {
            Text(
                "تاریخ تولد شمسی را کامل انتخاب کن",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun clampDay(year: Int?, month: Int?, day: Int?): Int? {
    if (day == null) return null
    if (year == null || month == null) return day
    val max = JalaliDate.daysInMonth(year, month)
    return day.coerceAtMost(max)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDropdown(
    modifier: Modifier,
    label: String,
    value: String,
    options: List<Pair<Int, String>>,
    isError: Boolean,
    onPick: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (id, labelText) ->
                DropdownMenuItem(
                    text = { Text(labelText) },
                    onClick = { onPick(id); open = false },
                )
            }
        }
    }
}
