package com.hamyareman.ir.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
    val curve = abs(offset)
    val depth = stackDepth.coerceIn(0, 9)
    val radius = if (isCover) 7.dp else 3.dp

    Box(
        modifier
            .fillMaxSize()
            .padding(horizontal = if (isCover) 9.dp else 6.dp, vertical = 6.dp)
            .graphicsLayer {
                rotationY = -offset * if (isCover) 13f else 8f
                rotationZ = offset * .55f
                scaleX = 1f - curve * if (isCover) .024f else .015f
                scaleY = 1f - curve * .008f
                translationX = -offset * 8.dp.toPx()
                cameraDistance = 42f * density
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    pivotFractionX = if (offset > 0f) 1f else 0f,
                    pivotFractionY = .5f,
                )
            }
            .shadow(if (isCover) 24.dp else 18.dp, RoundedCornerShape(radius)),
    ) {
        repeat(depth) { index ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = ((index + 1) * 1.6f).dp,
                        end = ((index + 1) * 1.6f).dp,
                        top = ((index + 1) * .65f).dp,
                    )
                    .shadow(1.4.dp, RoundedCornerShape(3.dp))
                    .background(Color(0xFFECE4D6)),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .shadow(if (isCover) 26.dp else 17.dp, RoundedCornerShape(radius))
                .background(if (isCover) Color(0xFF172A3C) else PaperWarm, RoundedCornerShape(radius)),
        ) {
            content()
            Canvas(Modifier.fillMaxSize()) {
                val shade = (.08f + curve * .20f).coerceAtMost(.28f)
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x10FFFFFF),
                            Color(0x36000000).copy(alpha = shade),
                        ),
                    ),
                )
                drawLine(
                    color = Color(0x55FFFFFF),
                    start = Offset(2f, 2f),
                    end = Offset(size.width - 2f, 2f),
                    strokeWidth = 1.6f,
                )
                drawLine(
                    color = Color(0x55000000),
                    start = Offset(size.width - 1.5f, 3f),
                    end = Offset(size.width - 1.5f, size.height - 3f),
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
