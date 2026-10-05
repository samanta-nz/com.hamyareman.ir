package com.hamyareman.ir.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

private val Leather = Color(0xFF3A2A20)
private val Gold = Color(0xFFB88A4A)
private val Paper = Color(0xFFF7F0E3)

@Composable
fun RealisticDeskFrame(
    backgroundKey: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RemoteDesignImage(
            key = backgroundKey,
            modifier = Modifier.fillMaxSize(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color(0x12000000),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.045f),
                        Color(0x72000000),
                    ),
                ),
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x66000000),
                    ),
                    radius = size.maxDimension * 0.80f,
                ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) { content() }
    }
}

@Composable
fun RealisticPaperStage(
    modifier: Modifier = Modifier,
    depth: Int = 4,
    openProgress: Float = 1f,
    content: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = openProgress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "book-open-progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .shadow(26.dp, RoundedCornerShape(5.dp), clip = false)
            .graphicsLayer {
                cameraDistance = 28f * density
                rotationY = (1f - progress) * -8f
                rotationZ = (1f - progress) * 0.4f
                scaleX = 0.978f + 0.022f * progress
                scaleY = 0.978f + 0.022f * progress
            }
            .clip(RoundedCornerShape(5.dp)),
    ) {
        for (i in depth.coerceIn(0, 9) downTo 1) {
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(x = (i * 1.9f).dp, y = (i * 0.8f).dp)
                    .background(Color(0xFFE2D9C9))
                    .border(0.65.dp, Color(0x284B3B2E), RoundedCornerShape(3.dp)),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Paper, Color(0xFFF2E8D7)),
                    ),
                )
                .border(0.8.dp, Color(0x5C594B3D), RoundedCornerShape(4.dp)),
        ) {
            content()
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0x22000000),
                            Color.Transparent,
                            Color(0x18000000),
                        ),
                    ),
                )
                drawLine(
                    Gold.copy(alpha = 0.22f),
                    androidx.compose.ui.geometry.Offset(8f, 4f),
                    androidx.compose.ui.geometry.Offset(size.width - 8f, 4f),
                    1.1f,
                )
            }
        }
    }
}

@Composable
fun RealisticCoverTile(
    key: String,
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Leather, Color(0xFF5A3D2B), Leather),
                ),
            )
            .shadow(if (selected) 14.dp else 8.dp, shape, clip = false)
            .border(
                if (selected) 2.dp else 0.8.dp,
                if (selected) Gold else Color(0x552F241D),
                shape,
            )
            .padding(if (selected) 3.dp else 1.dp)
            .clickable(onClick = onClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.72f)) {
                RemoteDesignImage(
                    key = key,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Gold.copy(alpha = 0.05f),
                                Color(0x55000000),
                            ),
                        ),
                    )
                }
            }
            Text(
                text = if (selected) "✓ $title" else title,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
fun RealisticIconAction(
    contentDescription: String,
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), CircleShape)
            .shadow(4.dp, CircleShape, clip = false),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.42f),
        )
    }
}