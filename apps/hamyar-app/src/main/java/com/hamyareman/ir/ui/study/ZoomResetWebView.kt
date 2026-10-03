package com.hamyareman.ir.ui.study

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView

/**
 * WebView با رفتار ثابتِ زوم: pinch همان زوم بومی مرورگر است و دوضربه همیشه
 * اندازه را به viewport اولیه بازمی‌گرداند. این کار به HTML تزریق نمی‌شود تا
 * هیچ‌کدام از فایل‌های درس تغییر نکنند.
 */
class ZoomResetWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    private var consumingDoubleTap = false
    private val taps = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onDoubleTap(e: MotionEvent): Boolean {
                consumingDoubleTap = true
                // WebView ممکن است در همان فریم ژست خودش را پردازش کند؛ post باعث
                // می‌شود ریست بعد از آن اعمال شود و نتیجه همیشه اندازهٔ اولیه باشد.
                post { resetToOriginalZoom() }
                return true
            }
        },
    )

    /** پایین‌آوردن تا کوچک‌ترین scale همان صفحه (fit/original viewport). */
    fun resetToOriginalZoom() {
        var steps = 0
        while (steps++ < 24 && zoomOut()) {
            // zoomOut تا رسیدن به minimum false می‌شود.
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        taps.onTouchEvent(event)
        if (consumingDoubleTap) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                consumingDoubleTap = false
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
