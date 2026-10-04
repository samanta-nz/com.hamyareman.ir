package com.hamyareman.ir.platform.core.designsystem

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.consume
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
    val latestPattern by androidx.compose.runtime.rememberUpdatedState(pattern)
    val latestChange by androidx.compose.runtime.rememberUpdatedState(onPatternChange)
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val dot = MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.background
    val points = remember {
        listOf(
            Offset(0f, 0f), Offset(.5f, 0f), Offset(1f, 0f),
            Offset(0f, .5f), Offset(.5f, .5f), Offset(1f, .5f),
            Offset(0f, 1f), Offset(.5f, 1f), Offset(1f, 1f),
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(surface.copy(alpha = .18f))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                fun hit(offset: Offset): Int? {
                    var best: Int? = null
                    var bestDistance = Float.MAX_VALUE
                    points.forEachIndexed { i, p ->
                        val center = Offset(p.x * size.width, p.y * size.height)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val d2 = dx * dx + dy * dy
                        val radius = size.minDimension * .20f
                        if (d2 < bestDistance && d2 <= radius * radius) {
                            best = i
                            bestDistance = d2
                        }
                    }
                    return best
                }
                detectDragGestures(
                    onDragStart = { pos ->
                        hit(pos)?.let { idx ->
                            val digit = (idx + 1).toString()
                            if (!latestPattern.contains(digit)) latestChange(digit)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        hit(change.position)?.let { idx ->
                            val digit = (idx + 1).toString()
                            if (!latestPattern.contains(digit)) latestChange(latestPattern + digit)
                        }
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val selected = latestPattern.mapNotNull { it.digitToIntOrNull()?.minus(1) }
            selected.zipWithNext().forEach { (a, b) ->
                drawLine(primary, Offset(points[a].x * size.width, points[a].y * size.height), Offset(points[b].x * size.width, points[b].y * size.height), 8f)
            }
            points.forEachIndexed { i, p ->
                val center = Offset(p.x * size.width, p.y * size.height)
                val chosen = selected.contains(i)
                drawCircle(if (chosen) primary else dot, if (chosen) 15f else 11f, center)
                if (chosen) drawCircle(background, 5f, center)
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
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
