package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** زوم بیت‌مپ همان مسیر امن PDF است — graphicsLayer حذف شد چون GPU را قفل می‌کرد. */
@Composable
internal fun ZoomableBitmap(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    onZoomed: (Boolean) -> Unit = {},
) {
    ZoomablePdfPage(bitmap = bitmap, modifier = modifier, onZoomed = onZoomed)
}
