package com.hamyareman.ir.ui.study

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import com.hamyareman.ir.platform.core.common.toPersianDigits
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** نسبت‌های استانداردِ کادرِ برش. */
private enum class FramePreset(val label: String, val ratio: Float?) {
    FREE("آزاد", null),
    SQUARE("۱:۱", 1f),
    FOUR_THREE("۴:۳", 4f / 3f),
    THREE_FOUR("۳:۴", 3f / 4f),
    WIDE("۱۶:۹", 16f / 9f),
    A4("A4", 1f / 1.414f),
    FULL("کل عکس", null),
}

/**
 * برش مستطیلی عکس جزوه/کتاب: زوم، جابه‌جایی، چرخش با لغزنده (زاویه) و چرخش ۹۰درجه،
 * کادر با اندازه‌ی **قابل‌تغییر** (کشیدنِ گوشه‌ها) + ابعاد استانداردِ آماده.
 */
@Composable
fun ImageRectCropDialog(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    var turns by remember { mutableIntStateOf(0) }
    var angle by remember { mutableFloatStateOf(0f) }
    var frameW by remember { mutableFloatStateOf(0f) }
    var frameH by remember { mutableFloatStateOf(0f) }
    var preset by remember { mutableStateOf(FramePreset.FREE) }

    var baked by remember(bitmap) { mutableStateOf(bitmap) }
    val img = remember(baked) { baked.asImageBitmap() }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
        ) {
            val density = LocalDensity.current
            val viewW = with(density) { maxWidth.toPx() }
            val viewH = with(density) { maxHeight.toPx() }
            val minFrame = with(density) { 96.dp.toPx() }
            val maxW = viewW * 0.96f
            val maxH = viewH * 0.78f
            val defaultW = viewW * 0.88f
            val defaultH = viewH * 0.52f
            val fw = (if (frameW <= 1f) defaultW else frameW).coerceIn(minFrame, maxW)
            val fh = (if (frameH <= 1f) defaultH else frameH).coerceIn(minFrame, maxH)
            // اندازه‌ی پایه‌ی عکس **ثابت** است (فیت روی بیشینه‌ی کادر) — تغییر اندازه‌ی کادر
            // نباید عکس را دوباره فیت/حرکت کند؛ کادر روی عکسِ ثابت جابه‌جا/تغییر می‌کند.
            val baseFit = min(maxW / baked.width.coerceAtLeast(1), maxH / baked.height.coerceAtLeast(1))
            val drawW = baked.width * baseFit
            val drawH = baked.height * baseFit

            fun clamp(s: Float, x: Float, y: Float): Triple<Float, Float, Float> {
                val ns = s.coerceIn(1f, 6f)
                val maxX = ((drawW * ns - fw) / 2f).coerceAtLeast(0f)
                val maxY = ((drawH * ns - fh) / 2f).coerceAtLeast(0f)
                return Triple(ns, x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
            }

            fun applyPreset(p: FramePreset) {
                preset = p
                val w: Float
                val h: Float
                when (p) {
                    FramePreset.FREE -> return
                    FramePreset.FULL -> {
                        w = drawW
                        h = drawH
                    }
                    else -> {
                        val r = p.ratio ?: return
                        w = min(maxW, maxH * r)
                        h = w / r
                    }
                }
                frameW = w.coerceIn(minFrame, maxW)
                frameH = h.coerceIn(minFrame, maxH)
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(baked) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val t = clamp(scale * zoom, ox + pan.x, oy + pan.y)
                            scale = t.first
                            ox = t.second
                            oy = t.third
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = img,
                    contentDescription = null,
                    modifier = Modifier
                        .size(
                            width = with(density) { drawW.toDp() },
                            height = with(density) { drawH.toDp() },
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = ox
                            translationY = oy
                            rotationZ = angle
                        }

                )

                // لایه‌ی رویی: تیرگی دورِ کادر، خطوطِ راهنما و دستگیره‌های گوشه
                val handlePx = with(density) { 22.dp.toPx() }
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    val left = (size.width - fw) / 2f
                    val top = (size.height - fh) / 2f
                    drawRect(Color(0xB3000000))
                    drawRect(
                        Color.Black,
                        topLeft = Offset(left, top),
                        size = Size(fw, fh),
                        blendMode = BlendMode.DstOut,
                    )
                    drawRect(
                        Color.White.copy(alpha = 0.92f),
                        topLeft = Offset(left, top),
                        size = Size(fw, fh),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    // خطوط یک‌سوم
                    for (i in 1..2) {
                        val x = left + fw * i / 3f
                        drawLine(
                            color = Color.White.copy(alpha = 0.45f),
                            start = Offset(x, top),
                            end = Offset(x, top + fh),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash,
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.45f),
                            start = Offset(left, top + fh * i / 3f),
                            end = Offset(left + fw, top + fh * i / 3f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash,
                        )
                    }
                    // دستگیره‌های چهار گوشه (دایره‌ی سفید پررنگ)
                    listOf(
                        Offset(left, top),
                        Offset(left + fw, top),
                        Offset(left, top + fh),
                        Offset(left + fw, top + fh),
                    ).forEach { c ->
                        drawCircle(Color(0xFF0F172A), handlePx * 0.62f, c)
                        drawCircle(Color.White, handlePx * 0.5f, c)
                    }
                }

                // لمسِ دستگیره‌ها: کشیدن برای تغییر اندازه‌ی کادر.
                // مقادیر با rememberUpdatedState خوانده می‌شوند تا با هر بار تغییرِ کادر،
                // اشاره‌گر از نو راه‌اندازی نشود و کشیدن در ابعاد آزاد قطع نشود.
                val fwNow = rememberUpdatedState(fw)
                val fhNow = rememberUpdatedState(fh)
                val presetNow = rememberUpdatedState(preset)
                val minNow = rememberUpdatedState(minFrame)
                val maxWNow = rememberUpdatedState(maxW)
                val maxHNow = rememberUpdatedState(maxH)
                val handleNow = rememberUpdatedState(handlePx)
                val viewWNow = rememberUpdatedState(viewW)
                val viewHNow = rememberUpdatedState(viewH)
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val cw = fwNow.value
                                val ch = fhNow.value
                                val left = (viewWNow.value - cw) / 2f
                                val top = (viewHNow.value - ch) / 2f
                                val hit = cornerOf(
                                    down.position.x, down.position.y,
                                    left, top, cw, ch, handleNow.value * 2.8f,
                                ) ?: return@awaitEachGesture
                                down.consume()
                                drag(down.id) { change ->
                                    change.consume()
                                    val baseW = fwNow.value
                                    val baseH = fhNow.value
                                    val dx = change.position.x - change.previousPosition.x
                                    val dy = change.position.y - change.previousPosition.y
                                    var nw = baseW
                                    var nh = baseH
                                    when (hit) {
                                        0 -> { nw = baseW - dx; nh = baseH - dy }
                                        1 -> { nw = baseW + dx; nh = baseH - dy }
                                        2 -> { nw = baseW - dx; nh = baseH + dy }
                                        else -> { nw = baseW + dx; nh = baseH + dy }
                                    }
                                    val ratio = presetNow.value.ratio
                                    if (ratio != null) {
                                        nw = max(nw, nh * ratio)
                                        nh = nw / ratio
                                    }
                                    frameW = nw.coerceIn(minNow.value, maxWNow.value)
                                    frameH = nh.coerceIn(minNow.value, maxHNow.value)
                                    preset = FramePreset.FREE
                                }
                            }
                        },
                )
            }

            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "گوشه‌های سفید را بکش تا اندازهٔ کادر عوض شود. دو انگشت برای زوم.",
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "اندازهٔ کادر: ${toPersianDigits((fw / density.density).roundToInt().toString())} × " +
                        toPersianDigits((fh / density.density).roundToInt().toString()),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xCC0F172A))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FramePreset.entries.forEach { p ->
                        FilterChip(
                            selected = preset == p,
                            onClick = { applyPreset(p) },
                            label = { Text(p.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "چرخش: ${toPersianDigits(angle.roundToInt().toString())}°",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Slider(
                        value = angle,
                        // زاویه بعد از رها کردن هم همان‌جا که گذاشته‌اید می‌ماند؛
                        // فقط هنگام برش (دکمهٔ برش) روی تصویر اعمال می‌شود.
                        onValueChange = { angle = it },
                        valueRange = -45f..45f,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("انصراف") }
                    OutlinedButton(
                        onClick = {
                            turns = (turns + 3) % 4
                            baked = rotateBitmap(baked, -90f)
                            scale = 1f; ox = 0f; oy = 0f
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("چپ ۹۰°") }
                    OutlinedButton(
                        onClick = {
                            turns = (turns + 1) % 4
                            baked = rotateBitmap(baked, 90f)
                            scale = 1f; ox = 0f; oy = 0f
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("راست ۹۰°") }
                    Button(
                        onClick = {
                            // برش = معکوسِ دقیقِ تبدیل نمایش (زوم + جابه‌جایی + زاویه) روی
                            // عکسِ ثابت — خروجی دقیقاً همان چیزی است که در کادر دیده می‌شود.
                            val cropped = cropRegion(baked, drawW, drawH, scale, ox, oy, angle, fw, fh)
                            if (cropped != null) onCropped(cropped)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("برش") }
                }
            }
        }
    }
}

/** کدام گوشه لمس شده؟ ۰=بالا-چپ، ۱=بالا-راست، ۲=پایین-چپ، ۳=پایین-راست؛ null = هیچ‌کدام. */
private fun cornerOf(
    x: Float,
    y: Float,
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    threshold: Float,
): Int? {
    val candidates = listOf(
        0 to Offset(left, top),
        1 to Offset(left + w, top),
        2 to Offset(left, top + h),
        3 to Offset(left + w, top + h),
    )
    return candidates.firstOrNull { (_, c) ->
        abs(x - c.x) <= threshold && abs(y - c.y) <= threshold
    }?.first
}

internal fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    if (degrees % 360f == 0f) return src
    val m = Matrix().apply { postRotate(degrees) }
    val out = runCatching {
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        // برای زاویه‌های غیر۹۰درجه، بوم را بزرگ می‌کنیم تا گوشه‌ها بریده نشوند.
        if (abs(degrees % 90f) < 0.001f) {
            rotated
        } else {
            val rad = Math.toRadians(degrees.toDouble())
            val cos = abs(kotlin.math.cos(rad)).toFloat()
            val sin = abs(kotlin.math.sin(rad)).toFloat()
            val newW = maxOf(1, (src.width * cos + src.height * sin).roundToInt())
            val newH = maxOf(1, (src.width * sin + src.height * cos).roundToInt())
            val canvasBitmap = Bitmap.createBitmap(newW, newH, src.config ?: Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(canvasBitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val matrix = Matrix().apply {
                postTranslate(-src.width / 2f, -src.height / 2f)
                postRotate(degrees)
                postTranslate(newW / 2f, newH / 2f)
            }
            canvas.drawBitmap(src, matrix, paint)
            if (rotated != src) rotated.recycle()
            canvasBitmap
        }
    }.getOrNull()
    return out ?: src
}

/**
 * برشِ دقیق: گوشه‌های کادر با معکوسِ تبدیل نمایش (مرکز + پن + زوم + زاویه)
 * روی تصویر اصلی نگاشت می‌شوند — خروجی با آنچه در کادر دیده می‌شود یکسان است،
 * حتی وقتی زاویه‌ی غیرصفر فعال است.
 */
private fun cropRegion(
    src: Bitmap,
    drawW: Float,
    drawH: Float,
    scale: Float,
    ox: Float,
    oy: Float,
    angleDeg: Float,
    frameW: Float,
    frameH: Float,
): Bitmap? {
    if (drawW <= 0f || drawH <= 0f || scale <= 0f || frameW <= 0f || frameH <= 0f) return null
    val outW = frameW.roundToInt().coerceAtLeast(1)
    val outH = frameH.roundToInt().coerceAtLeast(1)
    val out = Bitmap.createBitmap(outW, outH, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    canvas.drawColor(android.graphics.Color.WHITE)
    val m = Matrix()
    // برای نقطه‌ی (u,v) خروجی (رئختنِ بالا-چپ کادر):
    m.setTranslate(-outW / 2f, -outH / 2f)              // → مختصاتِ نسبت به مرکز کادر
    m.postTranslate(-ox, -oy)                            // → لغوِ جابه‌جایی (پن)
    m.postScale(1f / scale, 1f / scale)                  // → لغوِ زوم
    m.postRotate(-angleDeg)                              // → لغوِ زاویه‌ی نمایش
    m.postTranslate(drawW / 2f, drawH / 2f)              // → پیکسلِ تصویر (رئختنِ بالا-چپ)
    m.postScale(src.width / drawW, src.height / drawH)   // → پیکسلِ تصویر اصلی
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(src, m, paint)
    return out
}

internal fun compressReadableJpeg(src: Bitmap, maxSide: Int = 2048, quality: Int = 82): Pair<Bitmap, Int> {
    val longest = max(src.width, src.height).toFloat().coerceAtLeast(1f)
    val scaled = if (longest > maxSide) {
        val s = maxSide / longest
        Bitmap.createScaledBitmap(
            src,
            (src.width * s).toInt().coerceAtLeast(1),
            (src.height * s).toInt().coerceAtLeast(1),
            true,
        )
    } else src
    return scaled to quality.coerceIn(70, 90)
}
