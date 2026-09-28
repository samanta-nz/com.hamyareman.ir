package com.hamyareman.ir.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min

/**
 * برش دایره‌ای عکس پروفایل: زوم دو انگشتی + جابه‌جایی، فریم دایره در وسط.
 */
@Composable
fun AvatarCircleCropDialog(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    val img = remember(bitmap) { bitmap.asImageBitmap() }

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
            val circleD = min(viewW, viewH) * 0.72f
            val minSide = min(bitmap.width, bitmap.height).toFloat().coerceAtLeast(1f)
            val baseFit = circleD / minSide
            val drawW = bitmap.width * baseFit
            val drawH = bitmap.height * baseFit

            fun clamp(s: Float, x: Float, y: Float): Triple<Float, Float, Float> {
                val ns = s.coerceIn(1f, 4f)
                val maxX = ((drawW * ns - circleD) / 2f).coerceAtLeast(0f)
                val maxY = ((drawH * ns - circleD) / 2f).coerceAtLeast(0f)
                return Triple(ns, x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap) {
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
                        },
                )
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawRect(Color(0xB3000000))
                    drawCircle(Color.Black, circleD / 2f, c, blendMode = BlendMode.DstOut)
                    drawCircle(Color.White.copy(alpha = 0.92f), circleD / 2f, c, style = Stroke(width = 3.dp.toPx()))
                }
            }

            Text(
                "زوم کن و داخل دایره جا بده",
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp),
            )
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("انصراف") }
                Button(
                    onClick = {
                        val cropped = cropCircle(bitmap, drawW, drawH, scale, ox, oy, circleD)
                        if (cropped != null) onCropped(cropped)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("برش و ذخیره") }
            }
        }
    }
}

internal fun loadOrientedBitmap(ctx: Context, uri: Uri, maxSide: Int = 2048): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val w = bounds.outWidth
    val h = bounds.outHeight
    if (w <= 0 || h <= 0) return null
    var sample = 1
    val longest = max(w, h)
    while (longest / sample > maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
    val orient = ctx.contentResolver.openInputStream(uri)?.use { input ->
        runCatching { ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val matrix = Matrix()
    when (orient) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        else -> return raw
    }
    return runCatching {
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also {
            if (it != raw) raw.recycle()
        }
    }.getOrDefault(raw)
}

private fun cropCircle(
    src: Bitmap,
    drawW: Float,
    drawH: Float,
    scale: Float,
    ox: Float,
    oy: Float,
    circleD: Float,
): Bitmap? {
    if (drawW <= 0f || drawH <= 0f || scale <= 0f) return null
    val srcLeft = ((-circleD / 2f - ox) / scale + drawW / 2f) * (src.width / drawW)
    val srcTop = ((-circleD / 2f - oy) / scale + drawH / 2f) * (src.height / drawH)
    val srcSize = (circleD / scale) * (src.width / drawW)
    val left = srcLeft.toInt().coerceIn(0, src.width - 1)
    val top = srcTop.toInt().coerceIn(0, src.height - 1)
    val size = srcSize.toInt().coerceAtLeast(1)
    val w = size.coerceAtMost(src.width - left)
    val h = size.coerceAtMost(src.height - top)
    val side = min(w, h)
    if (side <= 0) return null
    val square = Bitmap.createBitmap(src, left, top, side, side)
    val out = Bitmap.createScaledBitmap(square, 512, 512, true)
    if (out != square && square != src) square.recycle()
    return out
}
