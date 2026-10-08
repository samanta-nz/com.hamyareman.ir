package com.hamyareman.ir.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * مشخصات یک کتاب PNG: همهٔ فایل‌ها یک بوم ۱۰۵۹×۱۴۸۶ دارند و دقیقاً روی هم می‌نشینند.
 * خط‌ها با اندازه‌گیری خود PNG ورق به‌دست آمده‌اند (شروع سطر اول، فاصلهٔ سطرها، تعداد، بازهٔ افقی).
 */
internal class BookSkin(
    val cover: String,
    val inside: String,
    val sheet: String,
    val firstLineY: Float,
    val lineSpacing: Float,
    val lineCount: Int,
    val textLeft: Float,
    val textRight: Float,
) {
    companion object {
        const val CANVAS_W = 1059f
        const val CANVAS_H = 1486f

        val NavyFloral = BookSkin(
            cover = DesignAsset.COVER_NAVY_FLORAL,
            inside = DesignAsset.COVER_NAVY_FLORAL_INSIDE,
            sheet = DesignAsset.COVER_NAVY_FLORAL_SHEET,
            firstLineY = 133.5f,
            lineSpacing = 49.26f,
            lineCount = 24,
            textLeft = 133f,
            textRight = 907f,
        )

        val LeatherBrown = BookSkin(
            cover = DesignAsset.COVER_LEATHER,
            inside = DesignAsset.COVER_LEATHER_INSIDE,
            sheet = DesignAsset.COVER_LEATHER_SHEET,
            firstLineY = 192.8f,
            lineSpacing = 46.73f,
            lineCount = 26,
            textLeft = 143f,
            textRight = 873f,
        )
    }
}

/**
 * صفحهٔ باز کتاب: اول `inside` (بدنهٔ کتاب باز)، روی آن `sheet` (ورق) در همان بوم،
 * و محتوا دقیقاً روی خط‌های ورق. [content] اندازهٔ هر سطر را (به dp) می‌گیرد؛ کافی است
 * متن را با `lineHeight = lineHeight` و تراز پایین سطر بنویسی تا روی خط بنشیند.
 *
 * [content] فقط در ناحیهٔ خط‌دار (از سطر اول تا آخر) چیده می‌شود، پس متن هرگز از ورق بیرون نمی‌زند.
 */
@Composable
internal fun BookSkinSpread(
    skin: BookSkin,
    modifier: Modifier = Modifier,
    content: @Composable (lineHeight: androidx.compose.ui.unit.Dp, lines: Int) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(BookSkin.CANVAS_W / BookSkin.CANVAS_H)) {
        val k = maxWidth.value / BookSkin.CANVAS_W
        val density = LocalDensity.current
        RemoteDesignImage(
            key = skin.inside,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        RemoteDesignImage(
            key = skin.sheet,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        val line = (skin.lineSpacing * k).dp
        // سطر اول روی اولین خط می‌نشیند: بالای کادر متن = خط اول − یک سطر.
        val top = ((skin.firstLineY - skin.lineSpacing) * k).dp
        val left = (skin.textLeft * k).dp
        val width = ((skin.textRight - skin.textLeft) * k).dp
        val height = (line.value * skin.lineCount).dp
        Box(
            Modifier
                .offset(x = left, y = top)
                .size(width, height),
        ) {
            content(line, skin.lineCount)
        }
        @Suppress("UNUSED_EXPRESSION") density
    }
}

/** جلد بسته (PNG دوربری‌شده، بدون برش) روی هر پس‌زمینه. */
@Composable
internal fun BookSkinCover(
    skin: BookSkin,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    RemoteDesignImage(
        key = skin.cover,
        modifier = modifier.fillMaxWidth().aspectRatio(BookSkin.CANVAS_W / BookSkin.CANVAS_H),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
    )
}
