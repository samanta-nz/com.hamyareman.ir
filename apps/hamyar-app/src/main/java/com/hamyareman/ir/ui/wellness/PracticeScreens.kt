package com.hamyareman.ir.ui.wellness

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.hub.AutoShrinkTileText
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubCoverGrid
import com.hamyareman.ir.ui.hub.HubCoverTile
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.HubMenuGroup
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.hub.loadPracticeCover
import com.hamyareman.ir.ui.navigation.Screen

/** هاب ریشه‌ی یک شاخه — جلدهای مربعی ۲ در هر ردیف، مثل کتاب‌ها. */
@Composable
fun PracticeHubScreen(
    nav: NavController,
    rootIds: List<String>,
    title: String,
    subtitle: String,
    headerSlot: String,
    accKey: String = "",
    onBack: (() -> Unit)? = null,
    extraTop: @Composable () -> Unit = {},
    extraBottom: @Composable () -> Unit = {},
) {
    val groups = remember(rootIds) { WellnessLayout.groupsOf(rootIds) }
    HubBody {
        HubHeader(title, subtitle, onBack, slotId = headerSlot)
        extraTop()
        HubCoverGrid(
            tiles = groups.map { g ->
                HubCoverTile(
                    id = g.id,
                    title = g.title,
                    subtitle = if (g.id == "cl-breath") "تمرین‌های تنفسی تعاملی" else g.subtitle,
                    onClick = {
                        when (g.id) {
                            "cl-breath" -> nav.layerTo(Screen.ContentCategory.of("breath"))
                            "mf-journal" -> nav.layerTo(Screen.GratitudeJournal.route)
                            else -> nav.layerTo(Screen.PracticeGroup.of(g.id))
                        }
                    },
                )
            },
        )
        extraBottom()
    }
}

/**
 * یک بخش از «صفحهٔ دوم»: یا کاشی مستقل، یا آکاردئونِ گروه (کاشی‌های محتوا مستقیم زیر آن باز می‌شوند).
 * قاعده: صفحهٔ اول همیشه کاشی؛ صفحهٔ دوم کاشی یا آکاردئون + کاشی؛ صفحهٔ سوم فقط خودِ محتوا.
 */
private sealed interface PracticeSection {
    data class Tile(val item: PracticeItem) : PracticeSection
    data class Accordion(
        val id: String,
        val emoji: String,
        val title: String,
        val subtitle: String,
        val items: List<PracticeItem>,
    ) : PracticeSection
}

private fun sectionsOf(group: PracticeGroup): List<PracticeSection> = buildList {
    group.items.forEach { item ->
        val child = item.childGroupId.takeIf { it.isNotBlank() }?.let { WellnessLayout.group(it) }
        if (child != null) {
            add(PracticeSection.Accordion(child.id, item.emoji, item.title, item.subtitle, flatItems(child)))
        } else {
            add(PracticeSection.Tile(item))
        }
    }
    group.childGroupIds.forEach { cid ->
        val child = WellnessLayout.group(cid) ?: return@forEach
        add(PracticeSection.Accordion(child.id, child.emoji, child.title, child.subtitle, flatItems(child)))
    }
}

/** آیتم‌های یک گروه؛ اگر خودش زیرگروه دارد، آیتم‌هایشان هم پشت سر هم می‌آیند (بدون صفحهٔ واسط). */
private fun flatItems(group: PracticeGroup): List<PracticeItem> =
    group.items + group.childGroupIds.mapNotNull { WellnessLayout.group(it) }.flatMap { flatItems(it) }

@Composable
fun PracticeGroupScreen(nav: NavController, groupId: String, onBack: () -> Unit) {
    val group = WellnessLayout.group(groupId)
    val ctx = LocalContext.current
    val memory = remember { LocalStore(ctx, "hamyar_accordion_memory") }
    // اولین ورود همه بسته‌اند؛ بعد از آن آخرین آکاردئونِ بازشده در حافظهٔ همین منو می‌ماند. فقط یکی باز است.
    var openId by remember(groupId) { mutableStateOf(memory.getString("open_$groupId", "")) }

    HubBody {
        if (!WellnessLayout.hideInternalChromeForGroup(groupId)) {
            HubHeader(
                title = group?.title ?: "تمرین",
                subtitle = group?.subtitle ?: "",
                onBack = onBack,
                slotId = "hub.practice.header",
            )
        }
        if (group == null) {
            Text("این بخش پیدا نشد.", style = AppTypography.pageBody.style)
            return@HubBody
        }
        val sections = remember(groupId) { sectionsOf(group) }
        val tints = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.surfaceVariant,
        )
        sections.forEachIndexed { index, section ->
            when (section) {
                is PracticeSection.Tile -> PracticeRowTile(
                    emoji = section.item.emoji,
                    coverId = section.item.id,
                    title = section.item.title,
                    subtitle = section.item.subtitle,
                    tint = tints[index % tints.size],
                    onClick = { openPractice(nav, section.item) },
                )
                is PracticeSection.Accordion -> HubMenuGroup(
                    title = section.emoji + "  " + section.title,
                    subtitle = section.subtitle,
                    open = openId == section.id,
                    onToggle = {
                        openId = if (openId == section.id) "" else section.id
                        memory.putString("open_$groupId", openId)
                    },
                ) {
                    HubCoverGrid(
                        section.items.map { item ->
                            HubCoverTile(
                                id = item.id,
                                title = item.title,
                                subtitle = item.subtitle,
                                onClick = { openPractice(nav, item) },
                            )
                        },
                    )
                }
            }
        }
    }
}

/** کاشی عرض‌کامل صفحهٔ دوم: تصویر/نشان در یک سمت، عنوان و توضیح، و فلش ادامه. */
@Composable
private fun PracticeRowTile(
    emoji: String,
    coverId: String,
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val cover = remember(coverId) { loadPracticeCover(ctx, coverId) }
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(5.dp, shape)
            .clip(shape)
            .background(tint)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = .72f)),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null) {
                Image(cover, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AutoShrinkTileText(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = AppTypography.cardTitle.family,
                    fontWeight = AppTypography.cardTitle.weight,
                    fontSize = AppTypography.cardTitle.size,
                ),
                maxLines = 1,
            )
            if (subtitle.isNotBlank()) {
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
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun PracticeItemScreen(
    itemId: String,
    onBack: () -> Unit,
    onWellness: (String) -> Unit,
    onRoute: (String) -> Unit,
) {
    val item = WellnessMenu.item(itemId)
    val move = item?.wellnessSlug?.takeIf { it.isNotBlank() }?.let { WellnessCatalog.bySlug(it) }
    val (body, steps) = if (item != null) WellnessMenu.resolveInstructions(item) else "" to emptyList()
    var session by remember { mutableStateOf(false) }
    var stepIx by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        if (!WellnessLayout.isPlaceholderItem(itemId)) {
            com.hamyareman.ir.platform.core.designsystem.AppTopBar(
                item?.title ?: "تمرین",
                onBack,
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (item == null) {
                Text("این تمرین پیدا نشد.", style = AppTypography.pageBody.style)
                return@Column
            }
            val staticPlaceholder = WellnessLayout.isPlaceholderItem(item.id)
            if (!staticPlaceholder) {
                Text(item.subtitle, style = AppTypography.pageHeading.style)
                if (item.minutes.isNotBlank()) {
                    Text(
                        item.minutes,
                        style = AppTypography.pageBody.style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (staticPlaceholder) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("این بخش به‌زودی فعال می‌شود", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "این مقصد هنوز محتوای HTML یا ابزار اجرایی ندارد. به‌محض آماده‌شدن نسخهٔ واقعی، همین‌جا در دسترس قرار می‌گیرد.",
                            style = AppTypography.pageBody.style,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@Column
            }
            if (!session) {
                if (body.isNotBlank()) {
                    Text(body, style = AppTypography.pageBody.style)
                }
                steps.forEachIndexed { i, s ->
                    Text(
                        toPersianDigits((i + 1).toString()) + ". " + s,
                        style = AppTypography.pageBody.style,
                    )
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton("شروع جلسه") {
                    when {
                        steps.isNotEmpty() || body.isNotBlank() -> {
                            session = true
                            stepIx = 0
                        }
                        move != null -> onWellness(move.category.wire)
                        item.route.isNotBlank() -> onRoute(item.route)
                        else -> {
                            session = true
                            stepIx = 0
                        }
                    }
                }
            } else {
                val lines = if (steps.isNotEmpty()) steps else listOf(body.ifBlank { "یک دقیقه آرام بمان؛ بعد تمام." })
                Text(
                    "گام " + toPersianDigits((stepIx + 1).toString()) + " از " + toPersianDigits(lines.size.toString()),
                    style = AppTypography.pageHeading.style,
                )
                Text(lines[stepIx], style = AppTypography.pageBody.style)
                if (stepIx < lines.lastIndex) {
                    PrimaryButton("گام بعد") { stepIx++ }
                } else {
                    PrimaryButton("پایان جلسه") { session = false; stepIx = 0 }
                }
            }
        }
    }
}

@Composable
fun BetweenLessonsHubScreen(nav: NavController, onBack: () -> Unit) {
    PracticeHubScreen(
        nav = nav,
        rootIds = WellnessMenu.betweenIds,
        title = "${WellnessMenu.BETWEEN_ICON} تمرینات بین دروس",
        subtitle = "رفع خستگی، ۲ تا ۵ دقیقه — بعد برگرد سر درس",
        headerSlot = "hub.between.header",
        accKey = "acc_between",
        onBack = onBack,
    )
}

internal fun openPractice(nav: NavController, item: PracticeItem) {
    when {
        // پل خودهیپنوز به صفحهٔ بشنو و بخواب: همیشه ابزار داخلی (FIX)، نه متن مرحله‌ای.
        item.id == "hyp-sleep" -> nav.layerTo(Screen.SleepNight.route)
        item.childGroupId.isNotBlank() -> nav.layerTo(Screen.PracticeGroup.of(item.childGroupId))
        item.route.isNotBlank() && item.steps.isEmpty() && item.body.isBlank() && item.wellnessSlug.isBlank() ->
            nav.layerTo(item.route)
        item.wellnessSlug.isNotBlank() && item.steps.isEmpty() && item.body.isBlank() ->
            nav.layerTo(Screen.PracticeItem.of(item.id))
        item.steps.isNotEmpty() || item.body.isNotBlank() ->
            nav.layerTo(Screen.PracticeItem.of(item.id))
        item.route.isNotBlank() -> nav.layerTo(item.route)
        else -> nav.layerTo(Screen.PracticeItem.of(item.id))
    }
}
