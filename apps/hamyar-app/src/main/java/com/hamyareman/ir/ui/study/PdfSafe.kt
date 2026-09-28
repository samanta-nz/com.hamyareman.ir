package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.content.Context

/**
 * رندر امن صفحهٔ PDF.
 *
 * PdfRenderer فقط ARGB_8888 می‌پذیرد؛ بلافاصله به RGB_565 جمع می‌شود
 * (صفحات کتاب آلفا لازم ندارند — نصف حافظه). OOM → بدون System.gc،
 * یک‌بار با نصف سقف دوباره امتحان، وگرنه null.
 */
internal object PdfSafe {

    const val MAX_W = 900
    const val MAX_H = 1400

    fun renderPage(page: PdfRenderer.Page, maxW: Int = MAX_W, maxH: Int = MAX_H): Bitmap? {
        return renderOnce(page, maxW, maxH)
            ?: renderOnce(page, (maxW / 2).coerceAtLeast(320), (maxH / 2).coerceAtLeast(480))
    }

    private fun renderOnce(page: PdfRenderer.Page, maxW: Int, maxH: Int): Bitmap? {
        var argb: Bitmap? = null
        return try {
            val pw = page.width.coerceAtLeast(1)
            val ph = page.height.coerceAtLeast(1)
            var scale = maxW.toFloat() / pw.toFloat()
            var w = (pw * scale).toInt().coerceIn(1, maxW)
            var h = (ph * scale).toInt().coerceAtLeast(1)
            if (h > maxH) {
                scale = maxH.toFloat() / ph.toFloat()
                w = (pw * scale).toInt().coerceIn(1, maxW)
                h = maxH
            }
            val px = w.toLong() * h.toLong()
            if (px > 16L * 1024L * 1024L) {
                val f = kotlin.math.sqrt((12L * 1024L * 1024L).toDouble() / px.toDouble()).toFloat()
                w = (w * f).toInt().coerceAtLeast(1)
                h = (h * f).toInt().coerceAtLeast(1)
                scale = w.toFloat() / pw.toFloat()
            }
            val created = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            argb = created
            created.eraseColor(Color.WHITE)
            val m = android.graphics.Matrix().apply { setScale(scale, scale) }
            page.render(created, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val rgb = try {
                created.copy(Bitmap.Config.RGB_565, false)
            } catch (_: OutOfMemoryError) {
                null
            } catch (_: Throwable) {
                null
            }
            if (rgb != null) {
                if (!created.isRecycled) created.recycle()
                argb = null
                rgb
            } else {
                argb = null
                created
            }
        } catch (_: OutOfMemoryError) {
            if (argb != null && !argb.isRecycled) argb.recycle()
            null
        } catch (_: Throwable) {
            if (argb != null && !argb.isRecycled) argb.recycle()
            null
        }
    }

    fun rotate(src: Bitmap, deg: Int): Bitmap {
        if (deg % 360 == 0) return src
        return try {
            val m = android.graphics.Matrix().apply { postRotate(deg.toFloat()) }
            val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
            if (out === src) return src
            val compact = if (out.config != Bitmap.Config.RGB_565) {
                try {
                    out.copy(Bitmap.Config.RGB_565, false)
                } catch (_: Throwable) {
                    null
                }
            } else {
                out
            }
            if (compact != null && compact !== out && !out.isRecycled) out.recycle()
            if (!src.isRecycled) src.recycle()
            compact ?: out
        } catch (_: Throwable) {
            src
        }
    }

    fun decodeCover(ctx: Context, bookCode: String, maxSide: Int = 320): Bitmap? = runCatching {
        val path = "book-covers/$bookCode.jpg"
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val h = bounds.outHeight.coerceAtLeast(1)
        val w = bounds.outWidth.coerceAtLeast(1)
        while (h / sample > maxSide || w / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        ctx.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()

    fun decodeFileCapped(path: String, maxSide: Int = 1600): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val h = bounds.outHeight.coerceAtLeast(1)
        val w = bounds.outWidth.coerceAtLeast(1)
        while (h / sample > maxSide || w / sample > maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(path, opts)
    }.getOrNull()
}
