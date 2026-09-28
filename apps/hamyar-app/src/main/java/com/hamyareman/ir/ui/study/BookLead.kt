package com.hamyareman.ir.ui.study

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.feature.study.BookModule

/** آیکون اختصاصی هر کتاب: کاور واقعی، وگرنه نشان موضوعی (نه 📘 عمومی). */
fun bookGlyph(bookCode: String, title: String = ""): String {
    val t = title
    return when (bookCode) {
        "C901" -> "🕌"
        "C902" -> "☪️"
        "C903" -> "✍"
        "C904" -> "✏️"
        "C905" -> "🔢"
        "C906" -> "🔬"
        "C907" -> "🌍"
        "C908" -> "🎨"
        "C909" -> "🌙"
        "C910", "C911" -> "🔤"
        "C917" -> "🔧"
        "C941" -> "💡"
        else -> when {
            t.contains("قرآن") -> "🕌"
            t.contains("اسلام") -> "☪️"
            t.contains("فارسی") -> "✍"
            t.contains("نگارش") -> "✏️"
            t.contains("ریاضی") -> "🔢"
            t.contains("علوم") -> "🔬"
            t.contains("اجتماع") -> "🌍"
            t.contains("هنر") -> "🎨"
            t.contains("عربی") -> "🌙"
            t.contains("انگلیس") -> "🔤"
            t.contains("کار") -> "🔧"
            t.contains("تفکر") -> "💡"
            else -> "📗"
        }
    }
}

@Composable
fun BookLeadIcon(
    module: BookModule,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val bmp = remember(module.bookCode) {
        runCatching { BitmapFactory.decodeStream(ctx.assets.open("book-covers/${module.bookCode}.jpg")) }.getOrNull()
    }
    val shape = RoundedCornerShape(10.dp)
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = module.title,
            modifier = modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(bookGlyph(module.bookCode, module.title), style = MaterialTheme.typography.titleLarge)
        }
    }
}
