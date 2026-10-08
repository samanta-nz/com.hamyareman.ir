package com.hamyareman.ir.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

/** نتیجهٔ بازچینی تایپی: صفحات جدید، صفحهٔ مقصد و مکان نشانگر. */
internal data class MeasuredEditResult(
    val pages: List<String>,
    val page: Int,
    val cursor: Int,
)

/** تعداد سطر واقعی با همان TextStyle و عرضی که روی صفحه استفاده می‌شود. */
internal fun measuredLineCount(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    widthPx: Int,
): Int = measurer.measure(
    text = AnnotatedString(text),
    style = style,
    constraints = Constraints(maxWidth = widthPx.coerceAtLeast(1)),
).lineCount

/**
 * بزرگ‌ترین بخش ابتدای متن که در [maxLines] سطر واقعی جا می‌شود.
 * شکست نرم فقط بعد از فاصله/خط جدید است؛ خود اندازه‌گیری تعیین‌کننده است.
 */
internal fun firstMeasuredPageFit(
    text: String,
    measurer: TextMeasurer,
    style: TextStyle,
    widthPx: Int,
    maxLines: Int,
): Pair<String, String> {
    val cap = maxLines.coerceAtLeast(1)
    if (text.isEmpty() || measuredLineCount(measurer, text, style, widthPx) <= cap) {
        return text to ""
    }

    var lo = 0
    var hi = text.length
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        if (measuredLineCount(measurer, text.substring(0, mid), style, widthPx) <= cap) {
            lo = mid
        } else {
            hi = mid - 1
        }
    }

    var cut = lo.coerceAtLeast(1)
    val soft = text.lastIndexOfAny(charArrayOf('\n', ' '), (cut - 1).coerceAtLeast(0))
    if (soft > 0 && soft >= cut - 28) {
        return text.substring(0, soft) to text.substring(soft + 1)
    }
    if (cut >= text.length) cut = text.length - 1
    return text.substring(0, cut) to text.substring(cut)
}

/** متن را بر اساس تعداد سطر واقعی روی چند صفحه می‌چیند. */
internal fun reflowTextMeasured(
    text: String,
    measurer: TextMeasurer,
    style: TextStyle,
    widthPx: Int,
    firstCapacity: Int,
    nextCapacity: Int,
): List<String> {
    val out = mutableListOf<String>()
    var carry = text
    var capacity = firstCapacity
    do {
        val (fit, rest) = firstMeasuredPageFit(carry, measurer, style, widthPx, capacity)
        out += fit
        carry = rest
        capacity = nextCapacity
    } while (carry.isNotEmpty())
    return out
}

/**
 * تغییر یک صفحه را محاسبه می‌کند و اگر از ظرفیت عبور کرد، باقیمانده را روی صفحات بعدی
 * می‌گذارد؛ هیچ برشی بر مبنای تعداد کاراکتر انجام نمی‌شود.
 */
internal fun applyMeasuredEdit(
    pages: List<String>,
    index: Int,
    text: String,
    cursor: Int,
    capacityForPage: (Int) -> Int,
    nextCapacity: Int,
    measurer: TextMeasurer,
    style: TextStyle,
    widthPx: Int,
): MeasuredEditResult {
    val safeIndex = index.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    val base = pages.toMutableList().also {
        while (it.size <= safeIndex) it += ""
    }
    val (first, firstRest) = firstMeasuredPageFit(
        text = text,
        measurer = measurer,
        style = style,
        widthPx = widthPx,
        maxLines = capacityForPage(safeIndex),
    )

    if (firstRest.isEmpty()) {
        base[safeIndex] = first
        return MeasuredEditResult(
            pages = base,
            page = safeIndex,
            cursor = cursor.coerceIn(0, first.length),
        )
    }

    val chunks = mutableListOf(first)
    var carry = firstRest
    while (carry.isNotEmpty()) {
        val (fit, rest) = firstMeasuredPageFit(
            text = carry,
            measurer = measurer,
            style = style,
            widthPx = widthPx,
            maxLines = nextCapacity,
        )
        chunks += fit
        carry = rest
    }

    base[safeIndex] = chunks.first()
    base.addAll(safeIndex + 1, chunks.drop(1))

    var remainingCursor = cursor.coerceAtLeast(0)
    chunks.forEachIndexed { chunkIndex, chunk ->
        if (remainingCursor <= chunk.length || chunkIndex == chunks.lastIndex) {
            return MeasuredEditResult(
                pages = base,
                page = safeIndex + chunkIndex,
                cursor = remainingCursor.coerceIn(0, chunk.length),
            )
        }
        remainingCursor -= chunk.length
    }

    return MeasuredEditResult(
        pages = base,
        page = safeIndex,
        cursor = chunks.first().length,
    )
}
