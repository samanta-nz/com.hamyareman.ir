package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * زوم PDF روی [PdfZoomHost] — Compose از ژست خبر ندارد و Pager را وسط پینچ
 * از نو نمی‌سازد. [onZoomed] دیگر استفاده نمی‌شود (عمداً؛ همان callback حلقهٔ ANR بود).
 */
@Composable
internal fun ZoomablePdfPage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onZoomed: (Boolean) -> Unit = {},
) {
    if (bitmap.isRecycled || bitmap.width < 1 || bitmap.height < 1) return
    AndroidView(
        factory = { ctx ->
            PdfZoomHost(ctx).apply { page.bind(bitmap) }
        },
        update = { host -> host.page.bind(bitmap) },
        modifier = modifier.fillMaxSize(),
    )
}
