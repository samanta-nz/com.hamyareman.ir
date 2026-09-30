package com.hamyareman.ir.ui.hub

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.navigation.NavController
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.appearance.FontCatalog

/** سربرگ مشترک صفحات هاب با دکمه‌ی بازگشت. */
@Composable
fun HubHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null, slotId: String = FontCatalog.ROLE_HEADING) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") }
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontFamily = AppTypography.title.family, fontWeight = AppTypography.title.weight, fontSize = AppTypography.title.size)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = AppTypography.titleSub.family, fontWeight = AppTypography.titleSub.weight, fontSize = AppTypography.titleSub.size)
        }
    }
}

/** آیتم منوی هاب — کارت قابل‌کلیک با ایموجی و توضیح. */
@Composable
fun HubCard(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    slotId: String = FontCatalog.ROLE_TILE,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f).height(46.dp)) {
                AutoShrinkTileText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AppTypography.cardTitle.family,
                        fontWeight = AppTypography.cardTitle.weight,
                        fontSize = AppTypography.cardTitle.size,
                    ),
                    maxLines = 1,
                )
                AutoShrinkTileText(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = AppTypography.cardSub.family,
                        fontWeight = AppTypography.cardSub.weight,
                        fontSize = AppTypography.cardSub.size,
                    ),
                    maxLines = 2,
                )
            }
        }
    }
}

/** بدنه‌ی اسکرول‌شونده‌ی استاندارد هاب‌ها. */
@Composable
fun HubBody(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** ناوبری استاندارد به یک مسیر از داخل هاب. */
fun NavController.hubTo(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** رفتن به لایه‌ی بعدی منو — برگشت، منوی پدر همان بخش است نه ریشه‌ی اپ. */
fun NavController.layerTo(route: String) {
    navigate(route)
}

/** کاشی جلد مربعی — الگوی کتاب‌های مدرسه: ۲ تا در هر ردیف، ۱×۱، عنوان + زیرعنوان. */
data class HubCoverTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
    /** نسبت عرض به ارتفاعِ خود تصویر؛ کاورهای عادی مربع و حرکت‌ها افقی‌اند. */
    val imageAspectRatio: Float = 1f,
)

@Composable
fun HubCoverGrid(tiles: List<HubCoverTile>, slotId: String = "hub.practice.item") {
    // سه ستون به‌جای دو ستون: عرض هر کاشی حدود ۳۰٪ کوچک‌تر می‌شود.
    Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
        tiles.chunked(3).forEach { rowTiles ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowTiles.forEach { tile ->
                    HubCoverCard(tile, Modifier.weight(1f), slotId)
                }
                repeat(3 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HubCoverCard(tile: HubCoverTile, modifier: Modifier, slotId: String) {
    val ctx = LocalContext.current
    val cover = remember(tile.id) { loadPracticeCover(ctx, tile.id) }
    Card(modifier = modifier.clickable(onClick = tile.onClick)) {
        Column {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = tile.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(tile.imageAspectRatio),
                    // تصاویر حرکات با نسبت واقعی خود نمایش داده می‌شوند؛ crop فقط نقش
                    // محافظ را برای کاورهای مربعی دارد و بدن/حرکت را قطع نمی‌کند.
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(tile.imageAspectRatio),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tile.title.take(1), style = MaterialTheme.typography.headlineLarge)
                }
            }
            // ارتفاع متن ثابت: یک سطر عنوان + دو سطر توضیح. فونت فقط کوچک می‌شود
            // و برای متن کوتاه هرگز از اندازهٔ طراحی بزرگ‌تر نمی‌شود.
            Column(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 7.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                AutoShrinkTileText(
                    text = tile.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = AppTypography.cardTitle.family,
                        fontWeight = AppTypography.cardTitle.weight,
                        fontSize = AppTypography.cardTitle.size,
                    ),
                    maxLines = 1,
                )
                if (tile.subtitle.isNotBlank()) {
                    AutoShrinkTileText(
                        text = tile.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = AppTypography.cardSub.family,
                            fontWeight = AppTypography.cardSub.weight,
                            fontSize = AppTypography.cardSub.size,
                        ),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AutoShrinkTileText(
    text: String,
    style: TextStyle,
    maxLines: Int,
) {
    var size by remember(text, style.fontSize, maxLines) { mutableStateOf(style.fontSize) }
    Text(
        text = text,
        style = style.copy(fontSize = size),
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if ((result.didOverflowWidth || result.didOverflowHeight) && size.value > 8f) {
                size = (size.value * 0.9f).coerceAtLeast(8f).sp
            }
        },
    )
}

internal fun loadPracticeCover(ctx: android.content.Context, id: String) = runCatching {
    ctx.assets.open("practice-covers/$id.jpg").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
}.getOrNull()

/** گروه تاشوی منوی هاب — دسته‌بندی در تو در تو (مشترک بین مدرسه/آموزشگاه/سلامتی). */
@Composable
/**
 * گروه منوی هاب — نسخه‌ی مستقل (حافظه‌دارِ خودش) یا کنترل‌شده برای آکاردئون:
 * اگر open/onToggle داده شود، وضعیتش را والد نگه می‌دارد («یکی باز شد، اونیکی بسته»).
 */
fun HubMenuGroup(
    title: String,
    subtitle: String,
    open: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    slotId: String = FontCatalog.ROLE_HEADING,
    content: @Composable () -> Unit,
) {
    var selfOpen by rememberSaveable { mutableStateOf(false) }
    val isOpen = open ?: selfOpen
    Card(modifier = Modifier.fillMaxWidth().clickable {
        if (onToggle != null) onToggle() else selfOpen = !selfOpen
    }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = AppTypography.accordionTitle.family, fontWeight = AppTypography.accordionTitle.weight, fontSize = AppTypography.accordionTitle.size)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = AppTypography.accordionSub.family, fontWeight = AppTypography.accordionSub.weight, fontSize = AppTypography.accordionSub.size)
                }
                Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = if (isOpen) "بستن" else "بازکردن")
            }
            AnimatedVisibility(visible = isOpen) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
        }
    }
}
