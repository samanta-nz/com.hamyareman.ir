package com.hamyareman.ir.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

private val Leather = Color(0xFF3A2A20)
private val Gold = Color(0xFFB88A4A)

@Composable
fun RealisticDeskFrame(
    backgroundKey: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize().background(Color(0xFF111B28))) {
        RemoteDesignImage(
            key = backgroundKey,
            modifier = Modifier.fillMaxSize(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x12000000), Color(0x26000000), Color(0x75000000))
                    )
                )
        )
        Box(Modifier.fillMaxSize().padding(contentPadding)) { content() }
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
            .shadow(22.dp, RoundedCornerShape(4.dp), clip = false)
            .graphicsLayer {
                cameraDistance = 22f * density
                rotationY = (1f - progress) * -7f
                scaleX = 0.985f + 0.015f * progress
                scaleY = 0.985f + 0.015f * progress
            }
            .clip(RoundedCornerShape(4.dp)),
    ) {
        for (i in depth.coerceIn(0, 9) downTo 1) {
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(x = (i * 1.7f).dp, y = (i * 0.9f).dp)
                    .background(Color(0xFFE2D9C9))
                    .border(0.6.dp, Color(0x2A4B3B2E), RoundedCornerShape(3.dp))
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Paper)
                .border(0.8.dp, Color(0x5C594B3D), RoundedCornerShape(4.dp))
        ) {
            content()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0x16000000),
                                Color.Transparent,
                                Color(0x18000000),
                            )
                        )
                    )
            )
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
            .background(Leather)
            .shadow(if (selected) 12.dp else 7.dp, shape, clip = false)
            .border(
                if (selected) 2.dp else 0.8.dp,
                if (selected) Gold else Color(0x552F241D),
                shape,
            )
            .padding(if (selected) 3.dp else 1.dp)
            .androidx.compose.foundation.clickable(onClick = onClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RemoteDesignImage(
                key = key,
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.72f),
                contentScale = ContentScale.Crop,
            )
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
    icon: ImageVector = androidx.compose.material.icons.Icons.Outlined.AutoStories,
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(44.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(0x28000000))
            .border(0.8.dp, Color(0x55FFFFFF), androidx.compose.foundation.shape.CircleShape),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.45f),
        )
    }
}
