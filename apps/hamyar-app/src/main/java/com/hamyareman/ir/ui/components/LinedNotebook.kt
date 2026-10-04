package com.hamyareman.ir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import kotlinx.coroutines.launch

internal const val NOTEBOOK_PAGE_SEPARATOR = '\u000c'

data class NotebookLayoutMetrics(
    val visibleLines: Int,
    val charsPerLine: Int,
    val pageCapacity: Int,
    val topSkipLines: Int,
    val lineHeightSp: Int,
)

private const val TOP_SKIP_LINES = 0
private const val LINE_HEIGHT_SP = 24
private const val SIDE_GUTTER_DP = 74
private const val BOTTOM_GUTTER_DP = 24
private const val PAGE_ASPECT = 0.707f

private fun metrics(widthDp: Float, heightDp: Float): NotebookLayoutMetrics {
    val usableWidth = (widthDp - SIDE_GUTTER_DP * 2).coerceAtLeast(140f)
    val charsPerLine = (usableWidth / 10.7f).toInt().coerceIn(14, 42)
    val usableHeight = (heightDp - BOTTOM_GUTTER_DP - TOP_SKIP_LINES * LINE_HEIGHT_SP).coerceAtLeast(120f)
    val visibleLines = (usableHeight / LINE_HEIGHT_SP).toInt().coerceIn(8, 26)
    return NotebookLayoutMetrics(
        visibleLines = visibleLines,
        charsPerLine = charsPerLine,
        pageCapacity = (visibleLines * charsPerLine * 0.9f).toInt().coerceAtLeast(180),
        topSkipLines = TOP_SKIP_LINES,
        lineHeightSp = LINE_HEIGHT_SP,
    )
}

private fun cutText(text: String, capacity: Int): List<String> {
    val normalized = text.replace("\r", "").trim()
    if (normalized.isBlank()) return listOf("")
    if (normalized.length <= capacity) return listOf(normalized)
    val out = mutableListOf<String>()
    var rest = normalized
    while (rest.length > capacity) {
        val floor = (capacity * 0.72f).toInt()
        val cut = rest.lastIndexOfAny(charArrayOf('\n', ' ', '،', '.', '؛', '؟'), capacity)
            .takeIf { it >= floor } ?: capacity
        out += rest.substring(0, cut).trim()
        rest = rest.substring(cut).trimStart()
    }
    if (rest.isNotBlank()) out += rest
    return out.ifEmpty { listOf("") }
}

private fun decodePages(value: String, capacity: Int): List<String> {
    val normalized = value.replace("\r", "")
    if (normalized.contains(NOTEBOOK_PAGE_SEPARATOR)) {
        return normalized.split(NOTEBOOK_PAGE_SEPARATOR).map { it.trim() }.ifEmpty { listOf("") }
    }
    return cutText(normalized, capacity)
}

private fun encodePages(pages: List<String>): String =
    pages.joinToString(NOTEBOOK_PAGE_SEPARATOR.toString()) { it.trim() }

@Composable
fun NotebookPaper(
    modifier: Modifier = Modifier,
    header: String = "",
    headerAlign: TextAlign = TextAlign.Right,
    showVerticalGuides: Boolean = true,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(PAGE_ASPECT)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFCF2)),
    ) {
        val density = LocalDensity.current
        val lineHeightPx = with(density) { LINE_HEIGHT_SP.dp.toPx() }
        val side = with(density) { SIDE_GUTTER_DP.dp.toPx() }
        val bottom = with(density) { BOTTOM_GUTTER_DP.dp.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            var y = 0f
            while (y <= size.height - bottom) {
                drawLine(
                    color = Color(0xFFB9CEE5),
                    start = Offset(side, y),
                    end = Offset(size.width - side, y),
                    strokeWidth = 1.2f,
                )
                y += lineHeightPx
            }
            if (showVerticalGuides) {
                drawLine(Color(0xFF7EA5C9), Offset(side, 0f), Offset(side, size.height - bottom), 2.2f)
                drawLine(Color(0xFF7EA5C9), Offset(size.width - side, 0f), Offset(size.width - side, size.height - bottom), 2.2f)
            }
        }
        if (header.isNotBlank()) {
            Text(
                text = header,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SIDE_GUTTER_DP.dp, end = SIDE_GUTTER_DP.dp, top = 1.dp),
                textAlign = headerAlign,
                fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                fontSize = 17.sp,
                lineHeight = LINE_HEIGHT_SP.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B3448),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(Modifier.fillMaxSize().padding(top = (LINE_HEIGHT_SP * 2).dp)) { content() }
        } else {
            content()
        }
    }
}

@Composable
fun NotebookBookPage(
    pageNumber: Int,
    pageCount: Int,
    stackPages: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(PAGE_ASPECT)
            .padding(8.dp),
    ) {
        val stack = stackPages.coerceIn(0, 7)
        repeat(stack) { index ->
            Box(
                Modifier
                    .matchParentSize()
                    .padding(start = ((index + 1) * 2).dp, top = ((index + 1) * 1.2f).dp)
                    .shadow(2.dp, RoundedCornerShape(15.dp))
                    .background(Color(0xFFFFFDF7), RoundedCornerShape(15.dp))
                    .border(1.dp, Color(0xFFD5D0C3), RoundedCornerShape(15.dp)),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .shadow(10.dp, RoundedCornerShape(15.dp))
                .background(Color(0xFFFFFCF2), RoundedCornerShape(15.dp)),
        ) {
            content()
            Text(
                text = "${pageNumber.coerceAtLeast(1)} / ${pageCount.coerceAtLeast(1)}",
                color = Color(0xFF58718A),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
fun LinedNotebookInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: String = "",
    textAlign: TextAlign = TextAlign.Right,
    showVerticalGuides: Boolean = true,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val baseLayout = remember(maxWidth) {
            metrics(maxWidth.value, (maxWidth.value / PAGE_ASPECT).coerceAtLeast(400f))
        }
        val pageCapacity = remember(baseLayout.pageCapacity, header) {
            if (header.isBlank()) baseLayout.pageCapacity
            else ((baseLayout.visibleLines - 2).coerceAtLeast(6) * baseLayout.charsPerLine * 0.9f).toInt().coerceAtLeast(120)
        }
        var pages by remember(value) { mutableStateOf(decodePages(value, pageCapacity)) }
        val pager = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
        val scope = rememberCoroutineScope()

        LaunchedEffect(value, pageCapacity) {
            val normalized = encodePages(pages)
            if (value.isNotBlank() && normalized != value) pages = decodePages(value, pageCapacity)
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth(),
            reverseLayout = true,
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            NotebookPaper(
                header = if (pageIndex == 0) header else "",
                headerAlign = oppositeTextAlign(textAlign),
                showVerticalGuides = showVerticalGuides,
            ) {
                BasicTextField(
                    value = pages.getOrElse(pageIndex) { "" },
                    onValueChange = { changed ->
                        val split = cutText(changed, pageCapacity)
                        val next = pages.toMutableList()
                        next[pageIndex] = split.firstOrNull().orEmpty()
                        if (split.size > 1) next.addAll(pageIndex + 1, split.drop(1))
                        pages = next
                        onValueChange(encodePages(next))
                        if (split.size > 1) scope.launch { pager.animateScrollToPage(pageIndex + split.lastIndex) }
                    },
                    textStyle = TextStyle(
                        fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = LINE_HEIGHT_SP.sp,
                        color = Color(0xFF1B3448),
                        textAlign = textAlign,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Bottom,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    cursorBrush = SolidColor(Color(0xFF1B3448)),
                    modifier = Modifier.fillMaxSize().padding(
                        start = SIDE_GUTTER_DP.dp,
                        end = SIDE_GUTTER_DP.dp,
                        bottom = BOTTOM_GUTTER_DP.dp,
                    ),
                )
            }
        }
    }
}

fun oppositeTextAlign(value: TextAlign): TextAlign = if (value == TextAlign.Center) TextAlign.Right else TextAlign.Center

fun notebookAlignmentWire(value: TextAlign): String = when (value) {
    TextAlign.Center -> "center"
    TextAlign.Left -> "left"
    else -> "right"
}

fun notebookTextAlignFromWire(value: String): TextAlign = when (value) {
    "center" -> TextAlign.Center
    "left" -> TextAlign.Left
    else -> TextAlign.Right
}

internal fun nextRegisteredTitle(baseTitle: String, existingTitles: List<String>): String {
    val base = baseTitle.trim().replace(Regex("\\s+شماره\\s+\\d+$"), "").trim().ifBlank { "دفترچه" }
    val pattern = Regex("^" + Regex.escape(base) + "\\s+شماره\\s+(\\d+)$")
    val maxNumber = existingTitles.mapNotNull {
        pattern.matchEntire(it.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
    }.maxOrNull() ?: 0
    return base + " شماره " + (maxNumber + 1)
}

@Composable
fun NotebookAlignmentPicker(
    value: TextAlign,
    onValueChange: (TextAlign) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Text("چینش", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp))
        IconButton(onClick = { onValueChange(TextAlign.Right) }) {
            Icon(Icons.Default.FormatAlignRight, contentDescription = "راست‌چین", tint = if (value == TextAlign.Right) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onValueChange(TextAlign.Center) }) {
            Icon(Icons.Default.FormatAlignCenter, contentDescription = "وسط‌چین", tint = if (value == TextAlign.Center) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onValueChange(TextAlign.Left) }) {
            Icon(Icons.Default.FormatAlignLeft, contentDescription = "چپ‌چین", tint = if (value == TextAlign.Left) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NotebookTitlePicker(
    value: String,
    defaultTitle: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = buildList {
        add(defaultTitle)
        add("عنوان جدید")
        suggestions.forEach { if (it != defaultTitle && it != "عنوان جدید") add(it) }
    }.distinct()
    var expanded by remember(value) { mutableStateOf(false) }
    var customRequested by remember(value) {
        mutableStateOf(value.isNotBlank() && value !in presets)
    }
    val shown = when {
        customRequested -> if (value.isBlank()) "عنوان جدید" else value
        value.isBlank() -> defaultTitle
        else -> value
    }

    Box(modifier) {
        OutlinedTextField(
            value = shown,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            maxLines = 1,
            label = { Text("عنوان") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            presets.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        expanded = false
                        if (index == 1) {
                            customRequested = true
                            onValueChange("")
                        } else {
                            customRequested = false
                            onValueChange(option)
                        }
                    },
                )
            }
        }
        if (customRequested) {
            Column(Modifier.fillMaxWidth().padding(top = 72.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it.take(90)) },
                    label = { Text("عنوان جدید") },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
