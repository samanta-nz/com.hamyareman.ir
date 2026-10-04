package com.hamyareman.ir.platform.core.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PatternLockGrid(
    pattern: String,
    onPatternChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    val view = LocalView.current
    val primary = MaterialTheme.colorScheme.primary
    val background = Color(0xFF081321)
    val gridDot = Color(0x3379A8CA)
    val nodeOuter = Color(0x5585B9D8)
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pattern-pulse")
    val pulse by transition.animateFloat(
        initialValue = .7f,
        targetValue = 1.18f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1500),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "pattern-pulse-value",
    )
    val selected = remember(pattern) {
        pattern.mapNotNull { it.digitToIntOrNull()?.minus(1) }
    }
    val points = remember {
        List(9) { index ->
            Offset((index % 3) / 2f, (index / 3) / 2f)
        }
    }

    fun hit(position: Offset, width: Float, height: Float): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        val radius = minOf(width, height) * .12f
        points.forEachIndexed { index, normalized ->
            val center = Offset(normalized.x * width, normalized.y * height)
            val dx = position.x - center.x
            val dy = position.y - center.y
            val d2 = dx * dx + dy * dy
            if (d2 <= radius * radius && d2 < bestDistance) {
                best = index
                bestDistance = d2
            }
        }
        return best
    }

    fun appendNode(base: String, idx: Int): String {
        val digit = (idx + 1).toString()
        if (base.contains(digit)) return base
        val last = base.lastOrNull()?.digitToIntOrNull()?.minus(1)
        if (last != null) {
            val middle = when {
                (last == 0 && idx == 2) || (last == 2 && idx == 0) -> 1
                (last == 3 && idx == 5) || (last == 5 && idx == 3) -> 4
                (last == 6 && idx == 8) || (last == 8 && idx == 6) -> 7
                (last == 0 && idx == 6) || (last == 6 && idx == 0) -> 3
                (last == 2 && idx == 8) || (last == 8 && idx == 2) -> 5
                (last == 0 && idx == 8) || (last == 8 && idx == 0) -> 4
                (last == 2 && idx == 6) || (last == 6 && idx == 2) -> 4
                else -> -1
            }
            if (middle >= 0) {
                val middleDigit = (middle + 1).toString()
                if (!base.contains(middleDigit)) return base + middleDigit + digit
            }
        }
        return base + digit
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF17314A), Color(0xFF0D1B2C), background),
                    radius = 760f,
                ),
            )
            .pointerInput(enabled, pattern) {
                if (!enabled) return@pointerInput
                var current = pattern
                detectDragGestures(
                    onDragStart = { start ->
                        current = ""
                        hit(start, size.width, size.height)?.let { idx ->
                            current = appendNode(current, idx)
                            onPatternChange(current)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        } ?: onPatternChange("")
                    },
                    onDrag = { change, _ ->
                        hit(change.position, size.width, size.height)?.let { idx ->
                            val next = appendNode(current, idx)
                            if (next != current) {
                                current = next
                                onPatternChange(current)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        }
                    },
                    onDragEnd = {},
                    onDragCancel = {},
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val glow = minOf(size.width, size.height) * .26f
            drawCircle(
                Brush.radialGradient(
                    colors = listOf(Color(0x304C96C6), Color.Transparent),
                    radius = glow,
                ),
                radius = glow,
                center = Offset(size.width * .5f, size.height * .5f),
            )

            val spacing = size.minDimension / 12f
            for (x in 1..11) {
                for (y in 1..11) {
                    val alpha = if ((x + y) % 2 == 0) .16f else .08f
                    drawCircle(
                        color = gridDot.copy(alpha = alpha),
                        radius = if ((x + y) % 3 == 0) 1.9f else 1.25f,
                        center = Offset(x * spacing, y * spacing),
                    )
                }
            }

            selected.zipWithNext().forEach { (a, b) ->
                val start = Offset(points[a].x * size.width, points[a].y * size.height)
                val end = Offset(points[b].x * size.width, points[b].y * size.height)
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(Color(0x006FC7FF), primary, Color(0x00FFFFFF)),
                        start = start,
                        end = end,
                    ),
                    start = start,
                    end = end,
                    strokeWidth = 18f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    color = primary.copy(alpha = .92f),
                    start = start,
                    end = end,
                    strokeWidth = 7f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }

            points.forEachIndexed { index, normalized ->
                val center = Offset(normalized.x * size.width, normalized.y * size.height)
                val isSelected = selected.contains(index)
                if (isSelected) {
                    drawCircle(
                        color = primary.copy(alpha = .11f * pulse),
                        radius = 34f * pulse,
                        center = center,
                    )
                    drawCircle(
                        color = primary.copy(alpha = .34f),
                        radius = 23f,
                        center = center,
                    )
                } else {
                    drawCircle(nodeOuter, 21f, center)
                }
                drawCircle(
                    color = if (isSelected) primary else Color(0xFFC7D9E7),
                    radius = if (isSelected) 11.5f else 8.5f,
                    center = center,
                )
                if (isSelected) {
                    drawCircle(background, 4.2f, center)
                }
            }
        }
    }
}

/**
 * دروازه‌ی قفل PIN — یک کامپوننت خالص که منطق رمز در آن نیست.
 *
 * [onVerify] باید true برگرداند اگر PIN درست بود (منطق هش در `core-security/AppLock` است).
 * [maxAttempts] برای جلوگیری از حدس زدن بی‌پایان است؛ بعد از آن دکمه قفل می‌شود.
 *
 * بیومتریک اختیاری است: اگر [biometricLabel] و [onBiometricRequest] داده شوند، یک دکمه‌ی
 * «بازکردن با اثر انگشت/چهره» نشان داده می‌شود. اجرای واقعی پرامپت بیرون از این کامپوننت است
 * (`core-security/BiometricPromptRunner`) تا این لایه به androidx.biometric وابسته نشود.
 * [externalNotice] پیام خطای بیومتریک است که از بیرون می‌آید.
 */
@Composable
fun PinLockGate(
    title: String = "این اپ قفل است",
    subtitle: String = "برای ادامه PIN را وارد کن.",
    minLength: Int = 4,
    maxLength: Int = 8,
    maxAttempts: Int = 5,
    biometricLabel: String? = null,
    patternEnabled: Boolean = false,
    onPatternVerify: ((String) -> Boolean)? = null,
    biometricBusy: Boolean = false,
    externalNotice: String? = null,
    onBiometricRequest: (() -> Unit)? = null,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var pattern by remember { mutableStateOf("") }
    val lockedOut = attempts >= maxAttempts

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF203A53),
                        Color(0xFF0B1725),
                        Color(0xFF07111E),
                    ),
                    radius = 1400f,
                ),
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD0DCE7),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        if (patternEnabled && onPatternVerify != null) {
            Text("الگوی اختصاصی همیار من", style = MaterialTheme.typography.titleMedium)
            Text(
                "الگو را با کشیدن انگشت بین نقطه‌ها رسم کن؛ این قفل کاملاً مستقل از قفل گوشی است.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            PatternLockGrid(pattern = pattern, onPatternChange = { pattern = it }, enabled = !lockedOut)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pattern = ""; error = null }, modifier = Modifier.weight(1f)) {
                    Text("پاک‌کردن")
                }
                PrimaryButton(
                    text = if (lockedOut) "قفل موقت" else "بازکردن با الگو",
                    onClick = {
                        if (pattern.length < 4) {
                            error = "الگو باید حداقل ۴ نقطهٔ متفاوت داشته باشد."
                        } else if (onPatternVerify(pattern)) {
                            pattern = ""
                            onUnlocked()
                        } else {
                            attempts += 1
                            pattern = ""
                            error = if (attempts >= maxAttempts) {
                                "چند بار الگو اشتباه شد؛ قفل موقت فعال است."
                            } else {
                                "الگو درست نیست. (" + attempts + " تلاش از " + maxAttempts + ")"
                            }
                        }
                    },
                    modifier = Modifier.weight(2f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("یا PIN را وارد کن", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedTextField(
            value = pin,
            onValueChange = { value ->
                if (!lockedOut) {
                    pin = value.filter { it.isDigit() }.take(maxLength)
                    error = null
                }
            },
            label = { Text("PIN") },
            singleLine = true,
            isError = error != null,
            enabled = !lockedOut,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = if (lockedOut) "قفل موقت" else "بازکردن",
            onClick = {
                if (pin.length < minLength) {
                    error = "PIN حداقل $minLength رقم است."
                    return@PrimaryButton
                }
                if (onVerify(pin)) {
                    pin = ""
                    onUnlocked()
                } else {
                    attempts += 1
                    pin = ""
                    error = if (lockedOut) {
                        "چند بار اشتباه وارد شد. برای محافظت از داده‌ها فعلاً قفل می‌ماند."
                    } else {
                        "PIN درست نیست. (${attempts} تلاش از $maxAttempts)"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.7f),
        )

        if (biometricLabel != null && onBiometricRequest != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBiometricRequest,
                enabled = !biometricBusy,
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                if (biometricBusy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("در حال تأیید…")
                } else {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(biometricLabel)
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        externalNotice?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "یادت رفته؟ داده‌های این دستگاه پاک نمی‌شوند، ولی باید اپ را دوباره نصب کنی یا از راه حساب کاربری وارد شوی.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
