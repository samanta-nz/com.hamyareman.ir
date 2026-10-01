package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * زوم PDF روی [PdfZoomHost] انجام می‌شود تا پینچ و دابل‌تپ از Lazy/Pager عبور
 * نکنند. خودِ View هم فقط هنگام زوم، پَن را می‌گیرد؛ در اندازهٔ اصلی اسکرول
 * والد برقرار می‌ماند.
 */
@Composable
internal fun ZoomablePdfPage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier.fillMaxSize(),
    onZoomed: (Boolean) -> Unit = {},
) {
    if (bitmap.isRecycled || bitmap.width < 1 || bitmap.height < 1) return
    AndroidView(
        factory = { ctx ->
            PdfZoomHost(ctx).apply { page.bind(bitmap) }
        },
        update = { host -> host.page.bind(bitmap) },
        modifier = modifier,
    )
}
