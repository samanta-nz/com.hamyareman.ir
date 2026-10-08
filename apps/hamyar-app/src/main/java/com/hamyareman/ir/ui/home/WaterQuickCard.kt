package com.hamyareman.ir.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.water.WaterViewModel

/** هر لیوان در ردیاب آب = ۲۵۰ میلی‌لیتر. */
private const val GLASS_ML = 250

/**
 * کارت ساده‌ی «ردیاب آب» برای داشبورد: یک ضربه = یک لیوان. نوار پیشرفت نرم پر می‌شود،
 * قطره می‌تپد، و لمس خود کارت صفحه‌ی کامل آب را باز می‌کند. داده همان WaterViewModel است.
 */
@Composable
fun WaterQuickCard(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    viewModel: WaterViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.syncNow() }

    val goal = state.goal.coerceAtLeast(1)
    val target = (state.consumed.toFloat() / goal).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "water-progress",
    )
    val pulse = rememberInfiniteTransition(label = "water-pulse")
    val drop by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isGoalReached) 1.18f else 1.08f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "water-drop",
    )

    val accent = MaterialTheme.colorScheme.secondary
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .scale(drop)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) { Text("💧", fontSize = 18.sp) }
                Text(
                    "ردیاب آب و هیدراتاسیون",
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .clickable { viewModel.addGlass() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+۱ لیوان (۲۵۰ml)",
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth().height(14.dp)) {
                val dot = 12.dp
                val travel = (maxWidth - dot).coerceAtLeast(0.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.18f)),
                )
                // RTL: پر شدن از راست؛ فقط عرض و جای نقطه با پیشرفت می‌آید.
                Box(
                    Modifier
                        .width(maxWidth * progress)
                        .height(8.dp)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(50))
                        .background(accent),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = travel * progress)
                        .size(dot)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    toPersianDigits((state.consumed * GLASS_ML).toString()) + " از " +
                        toPersianDigits((goal * GLASS_ML).toString()) + " میلی‌لیتر",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (state.isGoalReached) "هدف امروز کامل شد 🎉"
                    else toPersianDigits(state.remaining.toString()) + " لیوان مانده",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.isGoalReached) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (state.isGoalReached) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
