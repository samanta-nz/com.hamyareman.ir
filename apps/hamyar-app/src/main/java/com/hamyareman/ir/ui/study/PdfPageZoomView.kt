package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

/**
 * زوم PDF فقط داخل خودِ View اندروید.
 *
 * ریشهٔ ANR: Compose (Pager / pointerInput Initial / graphicsLayer) وسط پینچ
 * رویداد را می‌دزدید و صفحه را از نو می‌کشید. این ظرف همیشه
 * requestDisallowIntercept می‌گیرد تا والد هیچ سهمی از ژست نداشته باشد.
 */
internal class PdfZoomHost(context: Context) : FrameLayout(context) {
    val page = PdfPageZoomView(context)

    init {
        addView(page, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(ev)
    }
}

internal class PdfPageZoomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val mat = Matrix()
    private val tmp = FloatArray(9)
    private val paint = Paint()
    private var minScale = 1f
    private var maxScale = 3.5f
    private var bmpW = 1f
    private var bmpH = 1f
    private val last = PointF()
    private var panning = false
    private var fitted = false
    private var bitmap: Bitmap? = null

    private val scaleDet = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val cur = currentScale()
                var f = detector.scaleFactor
                if (!f.isFinite() || f <= 0f) return true
                val next = (cur * f).coerceIn(minScale, maxScale)
                f = next / cur
                if (!f.isFinite() || f <= 0f) return true
                mat.postScale(f, f, detector.focusX, detector.focusY)
                clamp()
                invalidate()
                return true
            }
        },
    )

    fun bind(bmp: Bitmap?) {
        if (bmp != null && bmp === bitmap && fitted) return
        if (bmp != null && bmp.isRecycled) {
            bitmap = null
            fitted = false
            invalidate()
            return
        }
        val same = bmp === bitmap
        bitmap = bmp
        if (bmp == null) {
            bmpW = 1f; bmpH = 1f
            fitted = false
            invalidate()
            return
        }
        bmpW = bmp.width.toFloat().coerceAtLeast(1f)
        bmpH = bmp.height.toFloat().coerceAtLeast(1f)
        if (!same || !fitted) {
            if (width > 0 && height > 0) fit() else post { if (width > 0) fit() }
        }
        invalidate()
    }

    private fun fit() {
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val sx = vw / bmpW
        val sy = vh / bmpH
        minScale = min(sx, sy).let { if (!it.isFinite() || it <= 0f) 1f else it }
        maxScale = (minScale * 3.5f)
        mat.reset()
        mat.postScale(minScale, minScale)
        val dw = bmpW * minScale
        val dh = bmpH * minScale
        mat.postTranslate((vw - dw) / 2f, (vh - dh) / 2f)
        fitted = true
        invalidate()
    }

    private fun currentScale(): Float {
        mat.getValues(tmp)
        val s = tmp[Matrix.MSCALE_X]
        return if (s.isFinite() && s > 0f) s else minScale.coerceAtLeast(0.01f)
    }

    private fun clamp() {
        mat.getValues(tmp)
        var tx = tmp[Matrix.MTRANS_X]
        var ty = tmp[Matrix.MTRANS_Y]
        val sc = tmp[Matrix.MSCALE_X]
        if (!sc.isFinite() || sc <= 0f) {
            fit()
            return
        }
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val dw = bmpW * sc
        val dh = bmpH * sc
        tx = if (dw <= vw) (vw - dw) / 2f else min(0f, max(tx, vw - dw))
        ty = if (dh <= vh) (vh - dh) / 2f else min(0f, max(ty, vh - dh))
        if (!tx.isFinite()) tx = 0f
        if (!ty.isFinite()) ty = 0f
        tmp[Matrix.MTRANS_X] = tx
        tmp[Matrix.MTRANS_Y] = ty
        mat.setValues(tmp)
    }

    override fun onDraw(canvas: Canvas) {
        val b = bitmap
        if (b == null || b.isRecycled) return
        canvas.drawBitmap(b, mat, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleDet.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                last.set(event.x, event.y)
                panning = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (panning && !scaleDet.isInProgress) {
                    mat.postTranslate(event.x - last.x, event.y - last.y)
                    clamp()
                    invalidate()
                }
                last.set(event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> panning = false
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap != null && w > 0 && h > 0 && currentScale() <= minScale * 1.04f) fit()
    }
}
