package com.hamyareman.ir.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * وضعیت تورق کتاب. [progress] بین −۱ و ۱ است:
 * مثبت = ورق فعلی دور شیرازه جمع می‌شود و صفحهٔ بعد زیرش پیدا می‌شود،
 * منفی = ورق قبلی از شیرازه باز می‌شود و روی صفحهٔ فعلی می‌نشیند.
 */
@Stable
class BookFlipState(initialPage: Int, count: Int) {
    var pageCount by mutableIntStateOf(count.coerceAtLeast(1))
    var current by mutableIntStateOf(initialPage.coerceIn(0, count.coerceAtLeast(1) - 1))
    internal val progress = Animatable(0f)

    /** تک‌لمس: صفحهٔ بعد. */
    suspend fun flipForward() {
        if (current >= pageCount - 1) return
        settle(1f)
    }

    /** دو لمس پیاپی: صفحهٔ قبل. */
    suspend fun flipBackward() {
        if (current <= 0) return
        settle(-1f)
    }

    internal suspend fun settle(target: Float) {
        progress.animateTo(target, tween(durationMillis = 520, easing = FastOutSlowInEasing))
        when {
            target > 0.5f -> current = (current + 1).coerceAtMost(pageCount - 1)
            target < -0.5f -> current = (current - 1).coerceAtLeast(0)
        }
        progress.snapTo(0f)
    }
}

@Composable
fun rememberBookFlipState(initialPage: Int, pageCount: Int): BookFlipState {
    val state = remember { BookFlipState(initialPage, pageCount) }
    SideEffect {
        state.pageCount = pageCount.coerceAtLeast(1)
        if (state.current > state.pageCount - 1) state.current = state.pageCount - 1
    }
    return state
}

/**
 * تورق واقعی: ورق در حال برگشتن به دور شیرازهٔ سمت راست جمع می‌شود (مقیاس افقی با سایه)،
 * صفحهٔ زیرین ثابت می‌ماند و کم‌کم نمایان می‌شود.
 * تک‌لمس = صفحهٔ بعد، دو لمس پیاپی = صفحهٔ قبل، کشیدن افقی = ورق‌زدن دستی.
 */
@Composable
fun BookFlipper(
    state: BookFlipState,
    modifier: Modifier = Modifier,
    pageContent: @Composable (page: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // فقط وقتی علامت پیشرفت عوض شود ترتیب ترکیب (زیر/رو) عوض می‌شود؛ نه در هر فریم.
    val forward by remember(state) { derivedStateOf { state.progress.value >= 0f } }
    val current = state.current
    val pages = buildList {
        if (forward) {
            if (current + 1 < state.pageCount) add(current + 1)
            add(current)
        } else {
            add(current)
            if (current - 1 >= 0) add(current - 1)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { scope.launch { state.flipForward() } },
                    onDoubleTap = { scope.launch { state.flipBackward() } },
                )
            }
            .pointerInput(state) {
                val width = size.width.toFloat().coerceAtLeast(1f)
                detectHorizontalDragGestures(
                    onDragStart = { scope.launch { state.progress.stop() } },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        val lower = if (state.current > 0) -1f else 0f
                        val upper = if (state.current < state.pageCount - 1) 1f else 0f
                        val next = (state.progress.value + dx / width * 1.25f).coerceIn(lower, upper)
                        scope.launch { state.progress.snapTo(next) }
                    },
                    onDragEnd = {
                        scope.launch {
                            val p = state.progress.value
                            state.settle(
                                when {
                                    p > 0.32f -> 1f
                                    p < -0.32f -> -1f
                                    else -> 0f
                                },
                            )
                        }
                    },
                    onDragCancel = { scope.launch { state.settle(0f) } },
                )
            },
    ) {
        pages.forEach { page ->
            key(page) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val o = (state.current - page) + state.progress.value
                            transformOrigin = TransformOrigin(1f, 0.5f)
                            alpha = if (o >= 1f || o <= -1f) 0f else 1f
                            if (o > 0f && o < 1f) {
                                scaleX = cos(o * PI / 2).toFloat().coerceIn(0.001f, 1f)
                                scaleY = 1f + 0.025f * sin(o * PI).toFloat()
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            val o = (state.current - page) + state.progress.value
                            if (o > 0f && o < 1f) {
                                // لبهٔ ورق در حال خم‌شدن تیره‌تر می‌شود.
                                drawRect(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.34f * sin(o * PI).toFloat()),
                                            Color.Transparent,
                                        ),
                                    ),
                                )
                            } else if (o <= 0f && o > -1f) {
                                // صفحهٔ زیرین زیر سایهٔ ورق در حال برگشتن است.
                                val fold = 1f - abs(o)
                                drawRect(Color.Black.copy(alpha = 0.22f * sin(fold * PI).toFloat()))
                            }
                        },
                ) {
                    pageContent(page)
                }
            }
        }
    }
}
