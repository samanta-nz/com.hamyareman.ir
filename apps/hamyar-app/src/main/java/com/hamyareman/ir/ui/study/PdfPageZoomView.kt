package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
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
 * Compose/LazyColumn در زوم دو انگشتی رویداد را نمی‌دزد: تا وقتی صفحه در اندازهٔ
 * اصلی است، اسکرول عمودی والد آزاد است؛ با دومین انگشت یا پس از زوم، والد از
 * رهگیری رویداد منع می‌شود. لمس دوضربه همیشه صفحه را به اندازهٔ اصلی برمی‌گرداند.
 */
internal class PdfZoomHost(context: Context) : FrameLayout(context) {
    val page = PdfPageZoomView(context)

    init {
        setBackgroundColor(android.graphics.Color.WHITE)
        addView(page, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        isClickable = true
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // در حالت عادی به LazyColumn اجازهٔ اسکرول بده؛ فقط پینچ/پَنِ زوم‌شده
        // باید کاملاً در اختیار صفحهٔ PDF باشد.
        if (ev.pointerCount > 1 || page.isZoomed()) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(ev)
    }
}

internal class PdfPageZoomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val mat = Matrix()
    private val tmp = FloatArray(9)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
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
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                disallowParent(true)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val cur = currentScale()
                var factor = detector.scaleFactor
                if (!factor.isFinite() || factor <= 0f) return true
                val next = (cur * factor).coerceIn(minScale, maxScale)
                factor = next / cur
                if (!factor.isFinite() || factor <= 0f) return true
                mat.postScale(factor, factor, detector.focusX, detector.focusY)
                clamp()
                invalidate()
                return true
            }
        },
    )

    private val tapDet = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                resetToOriginal()
                performClick()
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
            bmpW = 1f
            bmpH = 1f
            fitted = false
            invalidate()
            return
        }
        bmpW = bmp.width.toFloat().coerceAtLeast(1f)
        bmpH = bmp.height.toFloat().coerceAtLeast(1f)
        if (!same || !fitted) {
            if (width > 0 && height > 0) fit() else post { if (width > 0 && height > 0) fit() }
        }
        invalidate()
    }

    fun isZoomed(): Boolean = currentScale() > minScale * 1.01f

    /** بازگشت دقیق به قاب اولیهٔ همان صفحه، نه یک پله zoomOut. */
    fun resetToOriginal() {
        if (bitmap != null && width > 0 && height > 0) fit()
    }

    private fun fit() {
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val sx = vw / bmpW
        val sy = vh / bmpH
        minScale = min(sx, sy).let { if (!it.isFinite() || it <= 0f) 1f else it }
        maxScale = minScale * 3.5f
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
        val scale = tmp[Matrix.MSCALE_X]
        return if (scale.isFinite() && scale > 0f) scale else minScale.coerceAtLeast(0.01f)
    }

    private fun clamp() {
        mat.getValues(tmp)
        var tx = tmp[Matrix.MTRANS_X]
        var ty = tmp[Matrix.MTRANS_Y]
        val scale = tmp[Matrix.MSCALE_X]
        if (!scale.isFinite() || scale <= 0f) {
            fit()
            return
        }
        val vw = width.toFloat().coerceAtLeast(1f)
        val vh = height.toFloat().coerceAtLeast(1f)
        val dw = bmpW * scale
        val dh = bmpH * scale
        tx = if (dw <= vw) (vw - dw) / 2f else min(0f, max(tx, vw - dw))
        ty = if (dh <= vh) (vh - dh) / 2f else min(0f, max(ty, vh - dh))
        if (!tx.isFinite()) tx = 0f
        if (!ty.isFinite()) ty = 0f
        tmp[Matrix.MTRANS_X] = tx
        tmp[Matrix.MTRANS_Y] = ty
        mat.setValues(tmp)
    }

    private fun disallowParent(disallow: Boolean) {
        parent?.requestDisallowInterceptTouchEvent(disallow)
        parent?.parent?.requestDisallowInterceptTouchEvent(disallow)
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap
        if (bmp == null || bmp.isRecycled) return
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(bmp, mat, paint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        tapDet.onTouchEvent(event)
        scaleDet.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                last.set(event.x, event.y)
                panning = true
            }
            MotionEvent.ACTION_POINTER_DOWN -> disallowParent(true)
            MotionEvent.ACTION_MOVE -> {
                if (panning && !scaleDet.isInProgress && isZoomed()) {
                    disallowParent(true)
                    mat.postTranslate(event.x - last.x, event.y - last.y)
                    clamp()
                    invalidate()
                }
                last.set(event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                panning = false
                if (!isZoomed()) disallowParent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap != null && w > 0 && h > 0 && currentScale() <= minScale * 1.04f) fit()
    }
}
