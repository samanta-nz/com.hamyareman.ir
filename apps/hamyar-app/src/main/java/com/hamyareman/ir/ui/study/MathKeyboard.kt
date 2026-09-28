package com.hamyareman.ir.ui.study

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MathKeyCat(val label: String, val keys: List<String>)

private val MATH_CATS = listOf(
    MathKeyCat("عدد", listOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", ".", "/", ",")),
    MathKeyCat("عمل", listOf("+", "−", "×", "÷", "=", "±", "(", ")", "[", "]", "{", "}")),
    MathKeyCat("مقایسه", listOf("=", "≠", "≈", "<", "≤", ">", "≥")),
    MathKeyCat("مجموعه", listOf("∈", "∉", "⊂", "⊆", "⊃", "⊇", "∪", "∩", "∅", "∖", "ℕ", "ℤ", "ℚ", "ℝ")),
    MathKeyCat("توان", listOf("√", "∛", "^", "²", "³", "ⁿ", "°", "π", "∞", "!", "|", "%")),
    MathKeyCat("حرف", listOf("x", "y", "z", "a", "n", "θ", "α", "β", "Δ", "Σ")),
)

/**
 * کیبورد فشردهٔ نماد و عدد ریاضی — دسته‌بندی استاندارد، کلیدهای کوچک.
 * کیبورد گوشی استفاده نمی‌شود؛ درج فقط از اینجا.
 */
@Composable
fun MathSymbolKeyboard(
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cat by rememberSaveable { mutableIntStateOf(0) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MATH_CATS.forEachIndexed { i, c ->
                    FilterChip(
                        selected = cat == i,
                        onClick = { cat = i },
                        label = { Text(c.label, fontSize = 12.sp, maxLines = 1) },
                    )
                }
            }
            MATH_CATS[cat.coerceIn(0, MATH_CATS.lastIndex)].keys.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { key ->
                        OutlinedButton(
                            onClick = { onValue(insertAtCursor(value, key)) },
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { Text(key, fontSize = 13.sp, maxLines = 1) }
                    }
                    repeat(6 - row.size) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                OutlinedButton(onClick = { onValue(insertAtCursor(value, " ")) }, modifier = Modifier.weight(1f).height(32.dp)) {
                    Text("فاصله", fontSize = 11.sp)
                }
                OutlinedButton(onClick = { onValue(backspace(value)) }, modifier = Modifier.weight(1f).height(32.dp)) {
                    Text("⌫", fontSize = 14.sp)
                }
            }
        }
    }
}

internal fun insertAtCursor(value: TextFieldValue, insert: String): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(0, value.text.length)
    val next = value.text.substring(0, start) + insert + value.text.substring(end)
    val pos = start + insert.length
    return TextFieldValue(next, TextRange(pos))
}

internal fun backspace(value: TextFieldValue): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    if (start != end) {
        val next = value.text.removeRange(start, end)
        return TextFieldValue(next, TextRange(start))
    }
    if (start <= 0) return value
    val next = value.text.removeRange(start - 1, start)
    return TextFieldValue(next, TextRange(start - 1))
}
