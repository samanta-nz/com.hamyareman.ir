package com.hamyareman.ir.platform.core.designsystem

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/** دو قفل مستقل: قفل اپ (غروب کوهستان) و قفل فضای امن (شب جنگلی). */
enum class LockVariant { App, SafeSpace }

/** وضعیت بازخورد الگو: عادی / خطا (لرزش + قرمز) / موفق (حلقهٔ سبز). */
enum class PatternFeedback { Idle, Error, Success }

/**
 * اپ می‌تواند اینجا پس‌زمینهٔ تصویری (JPG باکت) را ثبت کند. زیر آن همیشه صحنهٔ
 * رسم‌شده با کد (آسمان، ستاره، کوه) دیده می‌شود؛ پس اگر تصویر هنوز لود نشده یا
 * اینترنت قطع باشد صفحهٔ قفل هرگز خالی نیست.
 */
object LockBackdrop {
    var provider: (@Composable (LockVariant) -> Unit)? = null
}

private val LockGlow = Color(0xFF3D8BFF)
private val LockError = Color(0xFFFF5A5F)
private val LockSuccess = Color(0xFF3DDC97)

// ───────────────────────── پس‌زمینهٔ صفحهٔ قفل ─────────────────────────

@Composable
fun LockBackdropLayer(variant: LockVariant, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawLockScene(variant) }
        val image = LockBackdrop.provider
        if (image != null) image(variant)
        // لایهٔ تیره‌کنندهٔ ملایم برای خوانایی متن‌ها روی هر تصویری.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x66000000), Color(0x00000000), Color(0x99000000)),
                    ),
                ),
        )
    }
}

private fun DrawScope.drawLockScene(variant: LockVariant) {
    val w = size.width
    val h = size.height
    val app = variant == LockVariant.App
    val sky = if (app) {
        listOf(
            0f to Color(0xFF08122B),
            0.38f to Color(0xFF2E2F73),
            0.62f to Color(0xFF9A5C9B),
            0.78f to Color(0xFFE8808A),
            1f to Color(0xFFF4B27A),
        )
    } else {
        listOf(
            0f to Color(0xFF040A1C),
            0.45f to Color(0xFF0A1B3C),
            0.8f to Color(0xFF0D2B45),
            1f to Color(0xFF0E3340),
        )
    }
    drawRect(Brush.verticalGradient(*sky.toTypedArray()))

    val rnd = kotlin.random.Random(if (app) 11 else 29)
    repeat(if (app) 40 else 90) {
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * h * (if (app) 0.45f else 0.6f)
        val r = (0.5f + rnd.nextFloat() * 1.1f).dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.25f + rnd.nextFloat() * 0.6f), r, Offset(x, y))
    }

    if (app) {
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x77FFC38A), Color(0x00FFC38A)),
                center = Offset(w * 0.5f, h * 0.76f),
                radius = w * 0.85f,
            ),
            radius = w * 0.85f,
            center = Offset(w * 0.5f, h * 0.76f),
        )
        drawRidge(h * 0.72f, h * 0.075f, 0.6f, 1.3f, Color(0xFF3A3170))
        drawRidge(h * 0.80f, h * 0.065f, 2.1f, 1.7f, Color(0xFF1E1D4A))
        drawRidge(h * 0.89f, h * 0.05f, 4.0f, 2.2f, Color(0xFF0C0F2B))
    } else {
        val moon = Offset(w * 0.82f, h * 0.12f)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0x66B8C6FF), Color(0x00B8C6FF)),
                center = moon,
                radius = w * 0.32f,
            ),
            radius = w * 0.32f,
            center = moon,
        )
        drawCircle(Color(0xFFE9E4FF), 17.dp.toPx(), moon)
        val lantern = Offset(w * 0.8f, h * 0.88f)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xAAFFB35A), Color(0x00FFB35A)),
                center = lantern,
                radius = w * 0.36f,
            ),
            radius = w * 0.36f,
            center = lantern,
        )
        drawRidge(h * 0.74f, h * 0.06f, 1.0f, 1.8f, Color(0xFF0B2233))
        drawRidge(h * 0.83f, h * 0.05f, 3.3f, 2.6f, Color(0xFF071822))
        drawRidge(h * 0.92f, h * 0.04f, 5.2f, 3.4f, Color(0xFF040F16))
    }
}

private fun DrawScope.drawRidge(baseY: Float, amp: Float, phase: Float, freq: Float, color: Color) {
    val w = size.width
    val h = size.height
    val tau = 6.2832f
    val p = Path()
    p.moveTo(0f, h)
    val steps = 64
    for (i in 0..steps) {
        val x = w * i / steps
        val t = x / w
        val y = baseY - amp * (
            0.55f * sin(t * freq * tau + phase) +
                0.30f * sin(t * freq * 2.7f * tau + phase * 1.7f) +
                0.15f * sin(t * freq * 7.1f * tau + phase * 0.6f)
            )
        p.lineTo(x, y)
    }
    p.lineTo(w, h)
    p.close()
    drawPath(p, color)
}

// ───────────────────────── الگوی نقطه + خط ─────────────────────────

private fun patternPoints(w: Float, h: Float): List<Offset> {
    val inset = 0.17f
    return List(9) { i ->
        val nx = (i % 3) / 2f
        val ny = (i / 3) / 2f
        Offset(w * (inset + nx * (1f - 2f * inset)), h * (inset + ny * (1f - 2f * inset)))
    }
}

private fun appendNode(base: String, idx: Int): String {
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
            (last == 1 && idx == 7) || (last == 7 && idx == 1) -> 4
            else -> -1
        }
        if (middle >= 0) {
            val middleDigit = (middle + 1).toString()
            if (!base.contains(middleDigit)) return base + middleDigit + digit
        }
    }
    return base + digit
}

/**
 * الگوی واقعی ۳×۳: نقطه‌های سفید، انگشت روی صفحه کشیده می‌شود و خط نورانی آبی
 * پشت انگشت می‌آید. [onPatternComplete] با رها شدن انگشت صدا زده می‌شود تا قفل
 * بدون دکمهٔ جدا بررسی شود. [feedback] خطا (لرزش + قرمز) یا موفقیت را نشان می‌دهد.
 */
@Composable
fun PatternLockGrid(
    pattern: String,
    onPatternChange: (String) -> Unit,
    enabled: Boolean = true,
    feedback: PatternFeedback = PatternFeedback.Idle,
    onPatternComplete: ((String) -> Unit)? = null,
    transparent: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
) {
    val view = LocalView.current
    val changeState = rememberUpdatedState(onPatternChange)
    val completeState = rememberUpdatedState(onPatternComplete)
    var finger by remember { mutableStateOf<Offset?>(null) }
    val selected = remember(pattern) { pattern.mapNotNull { it.digitToIntOrNull()?.minus(1) } }

    val transition = rememberInfiniteTransition(label = "pattern-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pattern-pulse-value",
    )
    val shake = remember { Animatable(0f) }
    LaunchedEffect(feedback) {
        if (feedback == PatternFeedback.Error) {
            for (v in listOf(18f, -16f, 12f, -8f, 4f, 0f)) shake.animateTo(v, tween(45))
        } else {
            shake.snapTo(0f)
        }
    }
    val success by animateFloatAsState(
        targetValue = if (feedback == PatternFeedback.Success) 1f else 0f,
        animationSpec = tween(650),
        label = "pattern-success",
    )
    val color = when (feedback) {
        PatternFeedback.Error -> LockError
        PatternFeedback.Success -> LockSuccess
        PatternFeedback.Idle -> LockGlow
    }

    val panel = if (transparent) {
        Modifier
    } else {
        Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF17314A), Color(0xFF0D1B2C), Color(0xFF081321)),
                    radius = 760f,
                ),
            )
    }

    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer { translationX = shake.value }
            .then(panel)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pts = patternPoints(size.width.toFloat(), size.height.toFloat())
                    val radius = minOf(size.width, size.height) * 0.14f
                    fun hitAt(p: Offset): Int? {
                        var best: Int? = null
                        var bestD = Float.MAX_VALUE
                        pts.forEachIndexed { i, c ->
                            val dx = p.x - c.x
                            val dy = p.y - c.y
                            val d2 = dx * dx + dy * dy
                            if (d2 <= radius * radius && d2 < bestD) {
                                best = i
                                bestD = d2
                            }
                        }
                        return best
                    }
                    var current = ""
                    changeState.value("")
                    finger = down.position
                    hitAt(down.position)?.let { idx ->
                        current = appendNode(current, idx)
                        changeState.value(current)
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    drag(down.id) { change ->
                        change.consume()
                        finger = change.position
                        hitAt(change.position)?.let { idx ->
                            val next = appendNode(current, idx)
                            if (next != current) {
                                current = next
                                changeState.value(current)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        }
                    }
                    finger = null
                    if (current.isNotEmpty()) completeState.value?.invoke(current)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pts = patternPoints(size.width, size.height)

            if (!transparent) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0x304C96C6), Color.Transparent),
                        radius = size.minDimension * 0.4f,
                    ),
                    radius = size.minDimension * 0.4f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }

            // خط‌های ثبت‌شده: هالهٔ نرم، بدنهٔ رنگی، هستهٔ سفید.
            selected.zipWithNext().forEach { (a, b) ->
                drawLine(color.copy(alpha = 0.22f), pts[a], pts[b], 11.dp.toPx(), StrokeCap.Round)
                drawLine(color.copy(alpha = 0.65f), pts[a], pts[b], 4.8.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.95f), pts[a], pts[b], 1.8.dp.toPx(), StrokeCap.Round)
            }
            // خط زنده از آخرین نقطه تا زیر انگشت.
            val tip = finger
            val last = selected.lastOrNull()
            if (tip != null && last != null && feedback == PatternFeedback.Idle) {
                drawLine(color.copy(alpha = 0.18f), pts[last], tip, 9.dp.toPx(), StrokeCap.Round)
                drawLine(color.copy(alpha = 0.7f), pts[last], tip, 3.6.dp.toPx(), StrokeCap.Round)
            }

            pts.forEachIndexed { index, c ->
                val on = selected.contains(index)
                if (on) {
                    drawCircle(color.copy(alpha = 0.16f), 22.dp.toPx() * pulse, c)
                    drawCircle(color.copy(alpha = 0.35f), 13.dp.toPx(), c)
                    drawCircle(color, 13.dp.toPx(), c, style = Stroke(width = 1.8.dp.toPx()))
                    drawCircle(Color.White, 5.6.dp.toPx(), c)
                    if (success > 0f) {
                        drawCircle(
                            LockSuccess.copy(alpha = (1f - success) * 0.7f),
                            13.dp.toPx() + success * 34.dp.toPx(),
                            c,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                } else {
                    drawCircle(Color.White.copy(alpha = 0.10f), 14.dp.toPx(), c)
                    drawCircle(Color.White.copy(alpha = 0.92f), 4.4.dp.toPx(), c)
                }
            }
        }
    }
}

// ───────────────────────── دروازهٔ قفل ─────────────────────────

@Composable
private fun GlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        LockGlow.copy(alpha = if (enabled) 0.45f else 0.18f),
                        LockGlow.copy(alpha = if (enabled) 0.25f else 0.10f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * دروازهٔ قفل — منطق رمز در آن نیست.
 *
 * [onVerify] باید true برگرداند اگر PIN درست بود (هش در `core-security/AppLock`).
 * الگو با رها شدن انگشت خودکار بررسی می‌شود؛ [variant] ظاهر قفل اپ یا فضای امن را
 * تعیین می‌کند و هر دو کاملاً مستقل‌اند. بیومتریک اختیاری است و اجرای واقعی‌اش
 * بیرون از این کامپوننت است.
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
    variant: LockVariant = LockVariant.App,
    biometricBusy: Boolean = false,
    externalNotice: String? = null,
    onBiometricRequest: (() -> Unit)? = null,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val usePattern = patternEnabled && onPatternVerify != null
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    var pattern by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf(PatternFeedback.Idle) }
    var showPin by remember { mutableStateOf(!usePattern) }
    val lockedOut = attempts >= maxAttempts

    LaunchedEffect(feedback) {
        when (feedback) {
            PatternFeedback.Error -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            PatternFeedback.Success -> view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.VIRTUAL_KEY,
            )
            PatternFeedback.Idle -> Unit
        }
    }

    fun failPattern(message: String) {
        feedback = PatternFeedback.Error
        error = message
        scope.launch {
            delay(750)
            pattern = ""
            feedback = PatternFeedback.Idle
        }
    }

    fun submitPattern(value: String) {
        if (!usePattern || lockedOut || feedback != PatternFeedback.Idle) return
        if (value.length < 4) {
            failPattern("الگو باید حداقل ۴ نقطهٔ متفاوت داشته باشد.")
            return
        }
        if (onPatternVerify?.invoke(value) == true) {
            feedback = PatternFeedback.Success
            error = null
            scope.launch {
                delay(520)
                pattern = ""
                onUnlocked()
            }
        } else {
            attempts += 1
            failPattern(
                if (attempts >= maxAttempts) {
                    "چند بار الگو اشتباه شد؛ قفل موقت فعال است."
                } else {
                    "الگو درست نیست. ($attempts تلاش از $maxAttempts)"
                },
            )
        }
    }

    fun submitPin() {
        if (lockedOut) return
        if (pin.length < minLength) {
            error = "PIN حداقل $minLength رقم است."
            return
        }
        if (onVerify(pin)) {
            pin = ""
            onUnlocked()
        } else {
            attempts += 1
            pin = ""
            error = if (attempts >= maxAttempts) {
                "چند بار اشتباه وارد شد. برای محافظت از داده‌ها فعلاً قفل می‌ماند."
            } else {
                "PIN درست نیست. ($attempts تلاش از $maxAttempts)"
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LockBackdropLayer(variant)
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val ring = when (feedback) {
                PatternFeedback.Error -> LockError
                PatternFeedback.Success -> LockSuccess
                PatternFeedback.Idle -> Color.White
            }
            Box(
                Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.5.dp, ring.copy(alpha = 0.85f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = ring, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (usePattern) "الگوی خود را رسم کنید" else subtitle,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            if (usePattern) {
                PatternLockGrid(
                    pattern = pattern,
                    onPatternChange = { if (feedback == PatternFeedback.Idle) pattern = it },
                    enabled = !lockedOut && feedback == PatternFeedback.Idle,
                    feedback = feedback,
                    onPatternComplete = { submitPattern(it) },
                    transparent = true,
                    modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }

            error?.let {
                Text(
                    it,
                    color = Color(0xFFFF9A9A),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }
            externalNotice?.let {
                Text(
                    it,
                    color = Color(0xFFFF9A9A),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (usePattern && !showPin) {
                GlassPill("ورود با PIN", onClick = { showPin = true; error = null })
            }
            if (showPin) {
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
                    isError = error != null && !usePattern,
                    enabled = !lockedOut,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White.copy(alpha = 0.5f),
                        focusedBorderColor = LockGlow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.75f),
                    ),
                    modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                GlassPill(
                    text = if (lockedOut) "قفل موقت" else "بازکردن",
                    enabled = !lockedOut,
                    onClick = { submitPin() },
                )
            }

            if (biometricLabel != null && onBiometricRequest != null) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable(enabled = !biometricBusy, onClick = onBiometricRequest),
                    contentAlignment = Alignment.Center,
                ) {
                    if (biometricBusy) {
                        CircularProgressIndicator(
                            Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Fingerprint,
                            contentDescription = biometricLabel,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "یادت رفته؟ داده‌های این دستگاه پاک نمی‌شوند، ولی باید اپ را دوباره نصب کنی یا از راه حساب کاربری وارد شوی.",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
