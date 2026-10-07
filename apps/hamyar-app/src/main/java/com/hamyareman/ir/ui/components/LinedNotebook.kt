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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
private const val SIDE_GUTTER_DP = 60
private const val BOTTOM_GUTTER_DP = 26
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
    // فاصلهٔ تایپی بخشی از محتوای صفحه است؛ trim کردن متن در هر تغییر، Space را می‌بلعد.
    val normalized = text.replace("\r", "")
    if (normalized.isEmpty()) return listOf("")
    if (normalized.length <= capacity) return listOf(normalized)
    val out = mutableListOf<String>()
    var rest = normalized
    while (rest.length > capacity) {
        val floor = (capacity * 0.72f).toInt()
        val cut = rest.lastIndexOfAny(charArrayOf('\n', ' ', '،', '.', '؛', '؟'), capacity)
            .takeIf { it >= floor } ?: capacity
        out += rest.substring(0, cut)
        val nextStart = if (cut < rest.length && rest[cut] == ' ') cut + 1 else cut
        rest = rest.substring(nextStart)
    }
    out += rest
    return out
}

private fun decodePages(value: String, capacity: Int): List<String> {
    val normalized = value.replace("\r", "")
    if (normalized.contains(NOTEBOOK_PAGE_SEPARATOR)) {
        return normalized.split(NOTEBOOK_PAGE_SEPARATOR).map { it }.ifEmpty { listOf("") }
    }
    return cutText(normalized, capacity)
}

private fun encodePages(pages: List<String>): String =
    pages.joinToString(NOTEBOOK_PAGE_SEPARATOR.toString())

@Composable
fun NotebookPaper(
    modifier: Modifier = Modifier,
    header: String = "",
    headerAlign: TextAlign = TextAlign.Right,
    showVerticalGuides: Boolean = true,
    headerOnFirstLine: Boolean = false,
    realistic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val contentTop = if (header.isBlank()) 0.dp else
        if (headerOnFirstLine) (LINE_HEIGHT_SP * 2).dp else (LINE_HEIGHT_SP * 2).dp
    // این کادر از ابعاد محدودیت استفاده نمی‌کند؛ Box ساده کافی است (لینت: UnusedBoxWithConstraintsScope).
    Box(
        (if (realistic) {
            modifier
                .shadow(10.dp, RoundedCornerShape(3.dp), clip = false)
                .border(0.7.dp, Color(0x335C4631), RoundedCornerShape(3.dp))
        } else modifier)
            .fillMaxWidth()
            .aspectRatio(PAGE_ASPECT)
            .clip(RoundedCornerShape(3.dp)),
    ) {
        val density = LocalDensity.current
        val line = with(density) { LINE_HEIGHT_SP.sp.toPx() }
        val first = with(density) { 18.dp.toPx() }
        val side = with(density) { SIDE_GUTTER_DP.dp.toPx() }
        val bottom = with(density) { BOTTOM_GUTTER_DP.dp.toPx() }
        Box(Modifier.fillMaxSize()) {
            RemoteDesignImage(
                key = DesignAsset.PAGE_LINED,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "کاغذ خط‌دار دفتر",
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            )
            Canvas(Modifier.fillMaxSize()) {
                var y = first
                while (y < size.height - bottom) {
                    drawLine(
                        color = Color(0x2A5E83A5),
                        start = Offset(side * .88f, y),
                        end = Offset(size.width - side * .42f, y),
                        strokeWidth = 0.72f,
                    )
                    y += line
                }
                if (showVerticalGuides) {
                    val right = size.width - side * .70f
                    drawLine(
                        Color(0x6590AABD),
                        Offset(right, 0f),
                        Offset(right, size.height - bottom),
                        1.25f,
                    )
                    drawLine(
                        Color(0x3A90AABD),
                        Offset(right + with(density) { 7.dp.toPx() }, 0f),
                        Offset(right + with(density) { 7.dp.toPx() }, size.height - bottom),
                        0.9f,
                    )
                }
                drawLine(
                    Color(0x32FFFFFF),
                    Offset(1.5f, 2f),
                    Offset(1.5f, size.height - 2f),
                    1.5f,
                )
                drawLine(
                    Color(0x30000000),
                    Offset(size.width - 1.5f, 3f),
                    Offset(size.width - 1.5f, size.height - 3f),
                    1.2f,
                )
            }

            if (header.isNotBlank()) {
                Text(
                    text = header,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SIDE_GUTTER_DP.dp,
                            end = SIDE_GUTTER_DP.dp,
                            top = if (headerOnFirstLine) 2.dp else 0.dp,
                        ),
                    textAlign = headerAlign,
                    fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                    fontSize = 17.sp,
                    lineHeight = LINE_HEIGHT_SP.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PaperInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = SIDE_GUTTER_DP.dp,
                        end = SIDE_GUTTER_DP.dp,
                        top = contentTop,
                        bottom = BOTTOM_GUTTER_DP.dp,
                    ),
            ) {
                content()
            }
        }
    }
}


@Composable
fun NotebookBookPage(
    pageNumber: Int,
    pageCount: Int,
    stackPages: Int,
    modifier: Modifier = Modifier,
    fullScreen: Boolean = false,
    content: @Composable () -> Unit,
) {
    RealisticBookPage(
        modifier = modifier.then(
            if (fullScreen) Modifier.fillMaxHeight().aspectRatio(PAGE_ASPECT)
            else Modifier.fillMaxWidth().aspectRatio(PAGE_ASPECT)
        ),
        pageOffset = 0f,
        stackDepth = stackPages,
        isCover = pageNumber == 1,
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
            Text(
                text = "${pageNumber.coerceAtLeast(1)} / ${pageCount.coerceAtLeast(1)}",
                color = Color(0xFF5E7180),
                fontSize = 10.sp,
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
    headerOnFirstLine: Boolean = false,
    realistic: Boolean = false,
    overlay: @Composable (pageIndex: Int) -> Unit = {},
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val layout = remember(maxWidth) {
            metrics(
                widthDp = maxWidth.value,
                heightDp = (maxWidth.value / PAGE_ASPECT).coerceAtLeast(430f),
            )
        }
        val pageCapacity = remember(layout, header) {
            val reserved = if (header.isBlank()) 0 else if (headerOnFirstLine) 3 else 2
            ((layout.visibleLines - reserved).coerceAtLeast(8) * layout.charsPerLine * .93f)
                .toInt()
                .coerceAtLeast(140)
        }
        var pages by remember(value) { mutableStateOf(decodePages(value, pageCapacity)) }
        val pager = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
        val layoutDirection = LocalLayoutDirection.current
        val scope = rememberCoroutineScope()

        LaunchedEffect(value, pageCapacity) {
            pages = decodePages(value, pageCapacity)
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth(),
            reverseLayout = PersianPaging.pagerReverseLayout(layoutDirection),
            beyondViewportPageCount = 2,
        ) { pageIndex ->
            NotebookPaper(
                header = if (pageIndex == 0) header else "",
                headerAlign = TextAlign.Right,
                showVerticalGuides = showVerticalGuides,
                headerOnFirstLine = headerOnFirstLine,
                realistic = realistic,
            ) {
                Box(Modifier.fillMaxSize()) {
                    BasicTextField(
                    value = pages.getOrElse(pageIndex) { "" },
                    onValueChange = { changed ->
                        val chunks = cutText(changed, pageCapacity)
                        val next = pages.toMutableList()
                        next[pageIndex] = chunks.firstOrNull().orEmpty()
                        if (chunks.size > 1) next.addAll(pageIndex + 1, chunks.drop(1))
                        pages = next
                        onValueChange(encodePages(next))
                        if (chunks.size > 1) {
                            scope.launch {
                                pager.animateScrollToPage(
                                    (pageIndex + chunks.lastIndex).coerceAtMost(next.lastIndex),
                                )
                            }
                        }
                    },
                    textStyle = TextStyle(
                        fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = LINE_HEIGHT_SP.sp,
                        color = PaperInk,
                        textAlign = textAlign,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    cursorBrush = SolidColor(Color(0xFF27485C)),
                        modifier = Modifier.fillMaxSize(),
                    )
                    overlay(pageIndex)
                }
            }
        }
    }
}


fun oppositeTextAlign(value: TextAlign): TextAlign =
    if (value == TextAlign.Center) TextAlign.Right else TextAlign.Center

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
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onValueChange(TextAlign.Right) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.FormatAlignRight,
                contentDescription = "راست‌چین",
                tint = if (value == TextAlign.Right) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onValueChange(TextAlign.Center) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.FormatAlignCenter,
                contentDescription = "وسط‌چین",
                tint = if (value == TextAlign.Center) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onValueChange(TextAlign.Left) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.FormatAlignLeft,
                contentDescription = "چپ‌چین",
                tint = if (value == TextAlign.Left) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * فیلد عنوان قابل ویرایش. فلش کنار فیلد عنوان‌های پیشنهادی را می‌دهد؛ انتخاب یکی از آن‌ها
 * فقط فیلد را پر می‌کند و کاربر می‌تواند آزادانه تغییرش دهد.
 */
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
        suggestions.forEach { if (it != defaultTitle) add(it) }
    }.distinct()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(90)) },
            singleLine = true,
            maxLines = 1,
            label = { Text("عنوان") },
            placeholder = { Text(defaultTitle) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                        contentDescription = "عنوان‌های پیشنهادی",
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            presets.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    },
                )
            }
        }
    }
}
