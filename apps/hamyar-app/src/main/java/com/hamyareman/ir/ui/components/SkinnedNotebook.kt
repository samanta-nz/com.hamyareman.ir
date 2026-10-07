package com.hamyareman.ir.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** جوهر متن روی ورق؛ همان رنگ قبلی دفترها. */
internal val SkinInk = Color(0xFF19364B)

/** هندسهٔ ورق PNG برای یک عرض مشخص (همان فرمول BookSkinSpread). */
internal class SkinGeometry(val skin: BookSkin, maxWidthDp: Float) {
    private val k = maxWidthDp / BookSkin.CANVAS_W
    val line: Dp = (skin.lineSpacing * k).dp
    val textWidth: Dp = ((skin.textRight - skin.textLeft) * k).dp
    val lines: Int get() = skin.lineCount
}

/**
 * سبک متن روی خط‌های ورق. اندازه از ارتفاع سطر می‌آید؛ [bonusSp] برای صفحه‌های تورق
 * بزرگ‌تر است. تراز پایینِ سطر باعث می‌شود نوشته دقیقاً روی خط بنشیند.
 */
internal fun skinTextStyle(
    line: Dp,
    density: Density,
    align: TextAlign,
    bonusSp: Float = 0f,
    scale: Float = 0.72f,
): TextStyle {
    val lineSp = with(density) { line.toSp() }
    return TextStyle(
        fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
        fontSize = (lineSp.value * scale + bonusSp).sp,
        fontWeight = FontWeight.Bold,
        lineHeight = lineSp,
        color = SkinInk,
        textAlign = align,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Bottom,
            trim = LineHeightStyle.Trim.None,
        ),
    )
}

internal fun measureLineCount(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    widthPx: Int,
): Int = measurer.measure(
    text = AnnotatedString(text),
    style = style,
    constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx.coerceAtLeast(1)),
).lineCount

/**
 * بزرگ‌ترین بخش ابتدای [text] که در [maxLines] سطر جا شود و باقیماندهٔ آن.
 * شکستن ترجیحاً سر فاصله یا خط جدید است.
 */
internal fun firstPageFit(
    text: String,
    measurer: TextMeasurer,
    style: TextStyle,
    widthPx: Int,
    maxLines: Int,
): Pair<String, String> {
    val cap = maxLines.coerceAtLeast(1)
    if (text.isEmpty() || measureLineCount(measurer, text, style, widthPx) <= cap) return text to ""
    var lo = 0
    var hi = text.length
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        if (measureLineCount(measurer, text.substring(0, mid), style, widthPx) <= cap) lo = mid else hi = mid - 1
    }
    var cut = lo.coerceAtLeast(1)
    val soft = text.lastIndexOfAny(charArrayOf('\n', ' '), cut - 1)
    if (soft > 0 && soft >= cut - 28) {
        return text.substring(0, soft) to text.substring(soft + 1)
    }
    if (cut >= text.length) cut = text.length - 1
    return text.substring(0, cut) to text.substring(cut)
}

/** متن را روی چند صفحه می‌چیند: ظرفیت صفحهٔ اول [firstCapacity] و بقیه [nextCapacity] سطر. */
internal fun reflowText(
    text: String,
    measurer: TextMeasurer,
    style: TextStyle,
    widthPx: Int,
    firstCapacity: Int,
    nextCapacity: Int,
): List<String> {
    val out = mutableListOf<String>()
    var carry = text
    var cap = firstCapacity
    do {
        val (fit, rest) = firstPageFit(carry, measurer, style, widthPx, cap)
        out += fit
        carry = rest
        cap = nextCapacity
    } while (carry.isNotEmpty())
    return out
}

private class EditResult(val pages: List<String>, val page: Int, val cursor: Int)

private fun applyEdit(
    pages: List<String>,
    index: Int,
    text: String,
    cursor: Int,
    capacity: (Int) -> Int,
    fullCapacity: Int,
    fit: (String, Int) -> Pair<String, String>,
): EditResult {
    val out = pages.toMutableList()
    val (first, rest) = fit(text, capacity(index))
    out[index] = first
    if (rest.isEmpty()) return EditResult(out, index, cursor.coerceIn(0, first.length))
    val chunks = mutableListOf(first)
    var carry = rest
    while (carry.isNotEmpty()) {
        val (f, r) = fit(carry, fullCapacity)
        chunks += f
        carry = r
    }
    out.addAll(index + 1, chunks.drop(1))
    var pos = 0
    chunks.forEachIndexed { i, chunk ->
        val start = text.indexOf(chunk, pos).let { if (it < 0) pos else it }
        val end = start + chunk.length
        if (cursor <= end || i == chunks.lastIndex) {
            return EditResult(out, index + i, (cursor - start).coerceIn(0, chunk.length))
        }
        pos = end
    }
    return EditResult(out, index, first.length)
}

/**
 * ویرایشگر دفتر روی همان ورق PNG نمایشگر: هر سطر تایپ دقیقاً روی یک خط ورق می‌نشیند.
 * سطر اول عنوان، سطر دوم خالی، از سطر سوم متن. [firstPageTopRows]/[firstPageBottomRows]
 * سطرهایی از صفحهٔ اول‌اند که عکس می‌گیرد و متن زیرشان نمی‌رود.
 * پر شدن صفحه به‌طور خودکار صفحهٔ بعد را باز می‌کند (جداکنندهٔ صفحه: [NOTEBOOK_PAGE_SEPARATOR]).
 */
@Composable
internal fun SkinnedNotebookEditor(
    skin: BookSkin,
    value: String,
    onValueChange: (String) -> Unit,
    header: String,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    firstPageTopRows: Int = 0,
    firstPageBottomRows: Int = 0,
    firstPageOverlay: @Composable BoxScope.(slotTop: Dp, line: Dp) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val geo = remember(skin, maxWidth) { SkinGeometry(skin, maxWidth.value) }
        val layoutDirection = LocalLayoutDirection.current
        val style = remember(geo, textAlign, density) { skinTextStyle(geo.line, density, textAlign) }
        val widthPx = with(density) { geo.textWidth.roundToPx() }
        val pages = remember(value) { value.split(NOTEBOOK_PAGE_SEPARATOR) }
        val latestPages by rememberUpdatedState(pages)
        val pager = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
        val scope = rememberCoroutineScope()
        var field by remember {
            mutableStateOf(TextFieldValue(pages.first(), TextRange(pages.first().length)))
        }
        var pendingCursor by remember { mutableStateOf<Int?>(null) }
        var started by remember { mutableStateOf(false) }
        val focus = remember { FocusRequester() }

        fun headerRowsOf(page: Int) = if (page == 0 && header.isNotBlank()) 2 else 0
        fun capacityOf(page: Int): Int {
            val top = if (page == 0) firstPageTopRows else 0
            val bottom = if (page == 0) firstPageBottomRows else 0
            return (skin.lineCount - headerRowsOf(page) - top - bottom).coerceAtLeast(1)
        }
        val fit: (String, Int) -> Pair<String, String> =
            { t, cap -> firstPageFit(t, measurer, style, widthPx, cap) }

        fun publish(result: EditResult) {
            onValueChange(result.pages.joinToString(NOTEBOOK_PAGE_SEPARATOR.toString()))
        }

        fun goTo(page: Int, cursor: Int) {
            pendingCursor = cursor
            scope.launch {
                withTimeoutOrNull(600) { snapshotFlow { pager.pageCount }.first { it > page } }
                pager.animateScrollToPage(page)
            }
        }

        fun commit(newField: TextFieldValue) {
            val cur = pager.currentPage
            val result = applyEdit(latestPages, cur, newField.text, newField.selection.start, ::capacityOf, skin.lineCount, fit)
            publish(result)
            if (result.page == cur) {
                val t = result.pages[cur]
                field = if (t == newField.text) newField else TextFieldValue(t, TextRange(result.cursor.coerceIn(0, t.length)))
            } else {
                goTo(result.page, result.cursor)
            }
        }

        fun mergeWithPrevious() {
            val cur = pager.currentPage
            if (cur <= 0) return
            val prev = latestPages[cur - 1]
            val list = latestPages.toMutableList()
            list.removeAt(cur)
            val result = applyEdit(list, cur - 1, prev + field.text, prev.length, ::capacityOf, skin.lineCount, fit)
            publish(result)
            goTo(result.page, result.cursor)
        }

        // تغییر صفحه: متن همان صفحه در فیلد فعال بارگذاری می‌شود.
        LaunchedEffect(pager.currentPage) {
            val txt = latestPages.getOrElse(pager.currentPage) { "" }
            val cursor = (pendingCursor ?: txt.length).coerceIn(0, txt.length)
            field = TextFieldValue(txt, TextRange(cursor))
            pendingCursor = null
            if (started) {
                delay(80)
                runCatching { focus.requestFocus() }
            }
            started = true
        }
        // تغییر بیرونی متن (باز کردن صفحهٔ دیگر، پاک‌شدن بعد از ذخیره).
        LaunchedEffect(value) {
            if (pager.currentPage > latestPages.lastIndex) {
                pager.scrollToPage(latestPages.lastIndex.coerceAtLeast(0))
            }
            val txt = latestPages.getOrElse(pager.currentPage) { "" }
            if (field.text != txt) field = TextFieldValue(txt, TextRange(txt.length))
        }

        Column {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth(),
                reverseLayout = PersianPaging.pagerReverseLayout(layoutDirection),
                beyondViewportPageCount = 1,
            ) { pageIndex ->
                BookSkinSpread(skin) { line, lines ->
                    val headerRows = headerRowsOf(pageIndex)
                    val topRows = if (pageIndex == 0) firstPageTopRows else 0
                    val bottomRows = if (pageIndex == 0) firstPageBottomRows else 0
                    val cap = capacityOf(pageIndex)
                    Box(Modifier.fillMaxSize()) {
                        if (headerRows > 0) {
                            Text(
                                header,
                                Modifier.fillMaxWidth().height(line),
                                style = style.copy(textAlign = TextAlign.Right),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            Modifier
                                .offset(y = line * (headerRows + topRows))
                                .fillMaxWidth()
                                .height(line * cap),
                        ) {
                            if (pageIndex == pager.currentPage) {
                                BasicTextField(
                                    value = field,
                                    onValueChange = { commit(it) },
                                    textStyle = style,
                                    cursorBrush = SolidColor(Color(0xFF27485C)),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusRequester(focus)
                                        .onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown &&
                                                ev.key == Key.Backspace &&
                                                field.selection.collapsed &&
                                                field.selection.start == 0 &&
                                                pager.currentPage > 0
                                            ) {
                                                mergeWithPrevious()
                                                true
                                            } else false
                                        },
                                )
                            } else {
                                Text(
                                    pages.getOrElse(pageIndex) { "" },
                                    Modifier.fillMaxSize(),
                                    style = style,
                                    maxLines = cap,
                                    overflow = TextOverflow.Clip,
                                )
                            }
                        }
                        if (pageIndex == 0) {
                            val slotTop = if (firstPageTopRows > 0) line * headerRows else line * (lines - bottomRows)
                            firstPageOverlay(slotTop, line)
                        }
                    }
                }
            }
            Text(
                "صفحهٔ ${pager.currentPage + 1} از ${pages.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * یک ورق ثابت (برای تورق و نمایش): عنوان در سطر اول، یک سطر خالی، بعد متن روی خط‌ها.
 * [overlay] برای جای عکس است؛ [slotTop] بالای ناحیهٔ رزروشدهٔ عکس را می‌دهد.
 */
@Composable
internal fun SkinnedStaticPage(
    skin: BookSkin,
    header: String,
    text: String,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    bonusSp: Float = 0f,
    topRows: Int = 0,
    bottomRows: Int = 0,
    overlay: @Composable BoxScope.(slotTop: Dp, line: Dp) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    BookSkinSpread(skin, modifier) { line, lines ->
        val style = skinTextStyle(line, density, textAlign, bonusSp)
        val headerRows = if (header.isNotBlank()) 2 else 0
        val cap = (lines - headerRows - topRows - bottomRows).coerceAtLeast(1)
        Box(Modifier.fillMaxSize()) {
            if (headerRows > 0) {
                Text(
                    header,
                    Modifier.fillMaxWidth().height(line),
                    style = style.copy(textAlign = oppositeTextAlign(textAlign)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text,
                Modifier
                    .offset(y = line * (headerRows + topRows))
                    .fillMaxWidth()
                    .height(line * cap),
                style = style,
                maxLines = cap,
                overflow = TextOverflow.Clip,
            )
            val slotTop = if (topRows > 0) line * headerRows else line * (lines - bottomRows)
            overlay(slotTop, line)
        }
    }
}
