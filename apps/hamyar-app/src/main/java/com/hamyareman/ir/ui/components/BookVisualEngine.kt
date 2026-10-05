package com.hamyareman.ir.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

internal val BookBackdrop = Color(0xFF07111E)
internal val PaperWarm = Color(0xFFFFFBF1)
internal val PaperInk = Color(0xFF203D50)

@Composable
fun BookStage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF31465B),
                        Color(0xFF0B1725),
                        BookBackdrop,
                    ),
                    radius = 1500f,
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * .5f, size.height * .55f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x304C89BA), Color.Transparent),
                    center = center,
                    radius = size.minDimension * .78f,
                ),
                radius = size.minDimension * .78f,
                center = center,
            )
        }
        content()
    }
}

@Composable
fun BookOpening(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = .77f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }
    val p = progress.value
    AnimatedVisibility(visible = visible) {
        Box(
            modifier
                .graphicsLayer {
                    val eased = 1f - (1f - p) * (1f - p)
                    alpha = p.coerceIn(.12f, 1f)
                    scaleX = .93f + eased * .07f
                    scaleY = .93f + eased * .07f
                    rotationY = -17f * (1f - eased)
                    cameraDistance = 45f * density
                },
        ) {
            content()
        }
    }
}

@Composable
fun RealisticBookPage(
    modifier: Modifier = Modifier,
    pageOffset: Float = 0f,
    stackDepth: Int = 0,
    isCover: Boolean = false,
    content: @Composable () -> Unit,
) {
    val offset = pageOffset.coerceIn(-1.2f, 1.2f)
    val bend = abs(offset)
    val depth = stackDepth.coerceIn(0, 10)
    val radius = if (isCover) 7.dp else 3.dp
    val warmEdge = Color(0xFFE4DAC9)

    Box(
        modifier
            .fillMaxSize()
            .padding(horizontal = if (isCover) 8.dp else 5.dp, vertical = 6.dp)
            .graphicsLayer {
                val eased = (bend * bend).coerceAtMost(1f)
                rotationY = -offset * if (isCover) 14f else 9f
                rotationZ = offset * 0.55f
                scaleX = 1f - bend * if (isCover) 0.023f else 0.014f
                scaleY = 1f - bend * 0.007f
                translationX = -offset * 8.dp.toPx()
                cameraDistance = 48f * density
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    pivotFractionX = if (offset >= 0f) 1f else 0f,
                    pivotFractionY = 0.5f,
                )
                shadowElevation = (12f + eased * 10f) * density
            }
            .shadow(if (isCover) 24.dp else 18.dp, RoundedCornerShape(radius), clip = false),
    ) {
        // صفحه‌های زیرین واقعاً به شکل stack دیده می‌شوند، نه یک shadow تخت.
        repeat(depth) { index ->
            val d = index + 1
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = (d * 1.45f).dp,
                        end = (d * 1.45f).dp,
                        top = (d * 0.68f).dp,
                    )
                    .shadow(1.8.dp, RoundedCornerShape(3.dp), clip = false)
                    .background(
                        Brush.verticalGradient(
                            listOf(warmEdge, Color(0xFFF0E7D7)),
                        ),
                        RoundedCornerShape(3.dp),
                    )
                    .border(0.55.dp, Color(0x3A6A5A48), RoundedCornerShape(3.dp)),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(radius))
                .background(
                    if (isCover) {
                        Brush.linearGradient(
                            listOf(Color(0xFF263E56), Color(0xFF102235), Color(0xFF1D3146)),
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(PaperWarm, Color(0xFFF4EBDD), PaperWarm),
                        )
                    },
                    RoundedCornerShape(radius),
                )
                .border(
                    if (isCover) 1.1.dp else 0.7.dp,
                    if (isCover) themeTertiary.copy(alpha = 0.55f) else themePrimary.copy(alpha = 0.18f),
                    RoundedCornerShape(radius),
                ),
        ) {
            content()

            Canvas(Modifier.fillMaxSize()) {
                // شکست نور روی جلد/کاغذ و سایهٔ داخلی.
                drawRect(
                    Brush.horizontalGradient(
                        colors = if (offset >= 0f) {
                            listOf(
                                Color(0x08000000),
                                Color.Transparent,
                                Color(0x36000000),
                            )
                        } else {
                            listOf(
                                Color(0x36000000),
                                Color.Transparent,
                                Color(0x08000000),
                            )
                        },
                    ),
                )

                val edgeX = if (offset >= 0f) size.width - 3f else 3f
                val edgeDirection = if (offset >= 0f) -1f else 1f
                repeat(if (isCover) 2 else 6) { i ->
                    val p = i * (if (isCover) 2.2f else 1.55f)
                    drawLine(
                        color = if (isCover) themeTertiary.copy(alpha = 0.42f) else themePrimary.copy(alpha = 0.28f),
                        start = Offset(edgeX + edgeDirection * p, 5f),
                        end = Offset(edgeX + edgeDirection * p, size.height - 6f),
                        strokeWidth = if (isCover) 1.05f else 0.72f,
                    )
                }

                if (isCover) {
                    val spineX = if (offset >= 0f) 8f else size.width - 8f
                    drawLine(
                        color = themePrimary.copy(alpha = 0.28f),
                        start = Offset(spineX, 4f),
                        end = Offset(spineX, size.height - 4f),
                        strokeWidth = 4.5f,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.20f),
                        start = Offset(spineX + if (offset >= 0f) 3f else -3f, 6f),
                        end = Offset(spineX + if (offset >= 0f) 3f else -3f, size.height - 6f),
                        strokeWidth = 1.0f,
                    )
                }

                // curl بسیار ملایم در لبهٔ ورق هنگام swipe.
                if (!isCover && bend > 0.08f) {
                    val curl = (bend.coerceIn(0f, 1f) * size.width * 0.07f)
                    val x = if (offset >= 0f) size.width - curl else curl
                    drawCircle(
                        color = Color(0x26000000),
                        radius = 16f + bend * 12f,
                        center = Offset(x, size.height * 0.5f),
                    )
                }

                drawLine(
                    color = Color(0x38FFFFFF),
                    start = Offset(2f, 2f),
                    end = Offset(size.width - 2f, 2f),
                    strokeWidth = 1.4f,
                )
            }
        }
    }
}

@Composable
fun RealisticBookPager(
    pageCount: Int,
    initialPage: Int = 0,
    modifier: Modifier = Modifier,
    viewerGesture: Boolean = true,
    onPageChanged: ((Int) -> Unit)? = null,
    pageContent: @Composable (page: Int, offset: Float) -> Unit,
) {
    val count = pageCount.coerceAtLeast(1)
    val pager = rememberPagerState(
        initialPage = initialPage.coerceIn(0, count - 1),
        pageCount = { count },
    )
    val scope = rememberCoroutineScope()
    val settle = animateFloatAsState(1f, animationSpec = spring(dampingRatio = .82f), label = "book-settle")

    LaunchedEffect(pager.currentPage) {
        onPageChanged?.invoke(pager.currentPage)
    }

    Box(modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (viewerGesture) {
                        Modifier.pointerInput(count) {
                            var dx = 0f
                            var dy = 0f
                            detectDragGestures(
                                onDragStart = {
                                    dx = 0f
                                    dy = 0f
                                },
                                onDrag = { change, amount ->
                                    dx += amount.x
                                    dy += amount.y
                                    if (abs(dx) > 18f && abs(dx) > abs(dy) * 1.2f) {
                                        change.consume()
                                    }
                                },
                                onDragEnd = {
                                    if (abs(dx) > 70f && abs(dx) > abs(dy) * 1.2f) {
                                        val target = if (dx < 0f) {
                                            (pager.currentPage + 1).coerceAtMost(count - 1)
                                        } else {
                                            (pager.currentPage - 1).coerceAtLeast(0)
                                        }
                                        scope.launch { pager.animateScrollToPage(target) }
                                    }
                                },
                                onDragCancel = {},
                            )
                        }
                    } else Modifier,
                )
                .graphicsLayer { alpha = settle.value },
            reverseLayout = true,
            beyondViewportPageCount = 2,
        ) { page ->
            val offset = pager.currentPage - page + pager.currentPageOffsetFraction
            RealisticBookPage(
                modifier = Modifier.fillMaxSize(),
                pageOffset = offset,
                stackDepth = (count - page - 1).coerceIn(0, 9),
                isCover = page == 0,
            ) {
                pageContent(page, offset)
            }
        }
    }
}
