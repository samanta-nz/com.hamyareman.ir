package com.hamyareman.ir.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ProfileClockAvatar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val ring = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val time = remember(now) { LocalDateTime.ofInstant(Instant.ofEpochMilli(now), JalaliDate.TEHRAN) }
    val avatarPath = StudentProfileState.avatarPath
    val initial = StudentProfileState.firstName.take(1).ifBlank { "؟" }
    val bmp = remember(avatarPath) {
        avatarPath.takeIf { it.isNotBlank() }?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
    }
    Box(
        modifier
            .size(124.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center) {
            if (bmp != null) {
                Image(
                    bmp.asImageBitmap(),
                    contentDescription = "عکس پروفایل",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop)
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val c = Offset(w / 2f, h / 2f)
                        // آواتار پیش‌فرض وکتوری: سبک، مدرن و مرتبط با هویت «دانش‌آموز/کاربر».
                        drawCircle(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            radius = w * 0.39f,
                            center = c,
                        )
                        drawCircle(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            radius = w * 0.28f,
                            center = Offset(c.x, c.y - w * 0.13f),
                            style = Stroke(width = w * 0.055f),
                        )
                        drawArc(
                            color = MaterialTheme.colorScheme.primary,
                            startAngle = 205f,
                            sweepAngle = 130f,
                            useCenter = false,
                            topLeft = Offset(c.x - w * 0.30f, c.y + w * 0.02f),
                            size = androidx.compose.ui.geometry.Size(w * 0.60f, w * 0.47f),
                            style = Stroke(width = w * 0.065f),
                        )
                        drawCircle(
                            color = MaterialTheme.colorScheme.primary,
                            radius = w * 0.045f,
                            center = Offset(c.x - w * 0.13f, c.y - w * 0.13f),
                        )
                        drawCircle(
                            color = MaterialTheme.colorScheme.primary,
                            radius = w * 0.045f,
                            center = Offset(c.x + w * 0.13f, c.y - w * 0.13f),
                        )
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f
            drawCircle(
                color = Color.Black,
                radius = r - 1.dp.toPx(),
                style = Stroke(width = 2f))
            drawCircle(
                color = ring,
                radius = r - 4.dp.toPx(),
                style = Stroke(width = 2.dp.toPx()))
            for (i in 0 until 12) {
                val rad = Math.toRadians(i * 30.0 - 90.0)
                val outer = r - 3.dp.toPx()
                val inner = outer - if (i % 3 == 0) 8.dp.toPx() else 4.dp.toPx()
                drawLine(
                    color = if (i % 3 == 0) ring else outline,
                    start = Offset(cx + cos(rad).toFloat() * inner, cy + sin(rad).toFloat() * inner),
                    end = Offset(cx + cos(rad).toFloat() * outer, cy + sin(rad).toFloat() * outer),
                    strokeWidth = if (i % 3 == 0) 2.6.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round)
            }
            val sec = time.second + time.nano / 1_000_000_000f
            val min = time.minute + sec / 60f
            val hour = (time.hour % 12) + min / 60f
            fun hand(angleDeg: Float, length: Float, back: Float, color: Color, width: Float) {
                rotate(angleDeg, Offset(cx, cy)) {
                    drawLine(
                        color = Color.White,
                        start = Offset(cx, cy + back),
                        end = Offset(cx, cy - length),
                        strokeWidth = width + 2.4.dp.toPx(),
                        cap = StrokeCap.Round)
                    drawLine(
                        color = color,
                        start = Offset(cx, cy + back),
                        end = Offset(cx, cy - length),
                        strokeWidth = width,
                        cap = StrokeCap.Round)
                }
            }
            hand(hour * 30f, r * 0.52f, r * 0.10f, Color(0xFF0F172A), 5.2.dp.toPx())
            hand(min * 6f, r * 0.70f, r * 0.12f, Color(0xFF1E293B), 3.6.dp.toPx())
            hand(sec * 6f, r * 0.82f, r * 0.16f, Color(0xFFDC2626), 1.8.dp.toPx())
            drawCircle(color = Color.White, radius = 5.4.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = Color(0xFF0F172A), radius = 3.4.dp.toPx(), center = Offset(cx, cy))
        }
        Text(
            "پروفایل من",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(color = Color.Black, offset = Offset(0f, 1f), blurRadius = 4f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp))
    }
}
