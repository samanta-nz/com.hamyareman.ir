package com.hamyareman.ir.ui.study

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.BookToc
import com.hamyareman.ir.platform.feature.study.BookToc.TocNode
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.hub.HubBody

/**
 * صفحه‌ی یک کتاب (v1.13) — فهرست درختیِ رسمی با قالب کارتیِ نسخه‌ی قبلی:
 *  - فصل/بخش = کارتِ جمع‌شونده (پیش‌فرض جمع‌شده، وضعیت با حافظه — مثل جمع‌شونده‌های دیگر اپ)؛
 *  - درس = کارت کامل با آمار، نوار تسلط و دو دکمه‌ی «تدریس» و «مطالعه و آزمون»
 *    (مطالعه با همان شرط اتمام اولین دوره‌ی تدریس باز می‌شود + دیالوگِ قفل)؛
 *  - ردیف‌های بدون درس (ستایش/نیایش/واژه‌نامه/جلسه‌ها/…) کارتِ ساده‌ی ثابت‌اند.
 */
@Composable
fun BookDetailScreen(
    bookCode: String,
    onBack: () -> Unit,
    onTeach: (String) -> Unit,
    onStudy: (String) -> Unit,
    onVideoTeach: (String) -> Unit,
    onCharts: () -> Unit,
    onOpenNode: (String, String) -> Unit = { _, _ -> },
) {
    val module = remember(bookCode) {
        runCatching { BookModuleRegistry.modules.firstOrNull { it.bookCode == bookCode } }.getOrNull()
    }
    val container = LocalAppContainer.current
    val today = remember { com.hamyareman.ir.platform.core.common.JalaliDate.todayIso() }

    Column(Modifier.fillMaxSize()) {
    AppTopBar(title = module?.title ?: "کتاب", onBack = onBack)
    HubBody {
        if (module == null) {
            Text("این کتاب هنوز محتوایی ندارد.")
            return@HubBody
        }

        val ctx = LocalContext.current
        val cover = remember(bookCode) { PdfSafe.decodeCover(ctx, bookCode) }
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (cover != null) {
                    Image(
                        bitmap = cover.asImageBitmap(),
                        contentDescription = "کاور ${module.title}",
                        modifier = Modifier.width(96.dp).height(128.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(module.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "فصل‌ها را باز کن و هر درس را لمس کن — اول تدریس، بعد تمرین.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onCharts) { Text("📈 نمودار پیشرفت دروس") }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val tocStore = remember(bookCode) { LocalStore(ctx, "hamyar_toc") }
        // v1.16: آکاردئون — هر لحظه فقط یک فصل باز است؛ پیش‌فرض همه جمع‌شده؛
        // فصلِ بازِ آخر در حافظه می‌ماند.
        var openSection by remember(bookCode) {
            mutableStateOf(tocStore.getString("acc_$bookCode", ""))
        }
        val toggle: (String) -> Unit = { id ->
            val next = if (openSection == id) "" else id
            openSection = next
            tocStore.putString("acc_$bookCode", next)
        }
        // منو دقیقاً از `books-menu.json` ساخته می‌شود — همان عنوان‌هایی که در
        // menu.json هر کتاب روی باکت نوشته شده‌اند. گره‌ای که فایل آماده ندارد
        // به تک‌فایل مشترک «در دست تولید» می‌رود.
        val menu = remember(bookCode) { BooksMenu.forBook(ctx, bookCode) }
        Column(Modifier.fillMaxWidth()) {
            if (menu == null) {
                runCatching { BookToc.forBook(bookCode) }.getOrDefault(emptyList()).forEach { node ->
                    TocRow(bookCode, node, 0, tocStore, onTeach, onStudy, onVideoTeach, openId = openSection, onToggle = toggle)
                }
            } else {
                menu.items.forEachIndexed { index, node ->
                    BookMenuNode(
                        node = node,
                        nodeId = "n$index",
                        openId = openSection,
                        onToggle = toggle,
                        onOpen = onOpenNode,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun TocRow(
    bookCode: String,
    node: TocNode,
    depth: Int,
    store: LocalStore,
    onTeach: (String) -> Unit,
    onStudy: (String) -> Unit,
    onVideoTeach: (String) -> Unit,
    openId: String,
    onToggle: (String) -> Unit,
) {
    val isSection = node.packId == null && node.children.isNotEmpty()
    when {
        isSection -> SectionCardCollapsible(node, depth, openId == node.id) { onToggle(node.id) }
        node.packId != null -> LessonCard(node, depth, onTeach, onStudy, onVideoTeach, subOpen = openId == node.id, onSubToggle = { onToggle(node.id) })
        else -> StaticCard(node, depth)
    }
    // v1.16: فرزندانِ درس (جلسه‌ها/…) هم جمع‌شوندگی آکاردئونی دارند — با باز شدن،
    // زیر کارتِ درس می‌آیند (نه داخل آن) تا دوبار رندر نشوند.
    if (node.children.isNotEmpty() && openId == node.id) {
        node.children.forEach { child ->
            TocRow(bookCode, child, depth + 1, store, onTeach, onStudy, onVideoTeach, openId = openId, onToggle = onToggle)
        }
    }
}

/** کارت فصل/بخش — جمع‌شونده با حافظه (مثل جمع‌شونده‌های برنامه‌ی هفتگی و آزمون‌ها). */
@Composable
private fun SectionCardCollapsible(node: TocNode, depth: Int, open: Boolean, onToggle: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp, top = 4.dp, bottom = 4.dp)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (open) "بستن فصل" else "بازکردن فصل",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                node.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** کارت درس — مثل نسخه‌ی قبلی: آمار + تسلط + دو دکمه‌ی تدریس/مطالعه با شرط اتمام. */
@Composable
private fun LessonCard(
    node: TocNode,
    depth: Int,
    onTeach: (String) -> Unit,
    onStudy: (String) -> Unit,
    onVideoTeach: (String) -> Unit,
    subOpen: Boolean,
    onSubToggle: () -> Unit,
) {
    val packId = node.packId ?: return
    val pack = remember(packId) { runCatching { BookModuleRegistry.pack(packId) }.getOrNull() }
    val container = LocalAppContainer.current
    val ctx = LocalContext.current
    val today = remember { com.hamyareman.ir.platform.core.common.JalaliDate.todayIso() }

    val teachDone = if (pack == null) false else {
        TeachStats.expectMedia(ctx, packId, expectedTeachMedia(pack))
        TeachStats.isDone(ctx, packId)
    }
    val mastery = remember(packId) {
        if (pack == null) 0 else runCatching { container.studyProgress.masteryPct(pack) }.getOrDefault(0)
    }
    val due = remember(packId) {
        if (pack == null) 0 else runCatching { container.studyProgress.dueCards(pack, today).size }.getOrDefault(0)
    }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp, top = 4.dp, bottom = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            // v1.16: اگر درس زیرمنو دارد (جلسه‌های قرآن/…)، سرتیتر کلیک‌پذیر است
            // و با شورون باز/جمع می‌شود — «یکی باز شد، اونیکی بسته» (آکاردئون کتاب).
            val hasSubs = node.children.isNotEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (hasSubs) Modifier.clickable(onClick = onSubToggle) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(node.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (hasSubs) {
                    Icon(
                        if (subOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (subOpen) "بستن زیرمنو" else "بازکردن زیرمنو",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            if (pack != null && !pack.pdfOnly) {
                Text(
                    "${pack.sections.size} سکشن · ${pack.flashcards.size} کارت · ${pack.questions.size} سؤال",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تسلط ${toPersianDigits(mastery.toString())}٪", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { mastery / 100f },
                        modifier = Modifier.weight(1f).height(8.dp),
                    )
                }
                if (due > 0) {
                    Text("🔔 ${toPersianDigits(due.toString())} کارت امروز باید مرور شود", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (pack == null) {
                // کتابی که تازه اضافه شده (فهرست/جلد آماده، محتوا هنوز نه) و درس‌های
                // اوّلِ کتاب‌های نیمه‌کامل: پیش‌تر دکمهٔ «ورود به درس» به صفحه‌ی
                // «این درس پیدا نشد.» می‌رفت. حالا همان‌جا شفاف می‌گوییم که متنِ درس
                // هنوز آماده نشده، تا دکمه‌ای که به بن‌بست می‌رسد نشان داده نشود.
                Text(
                    "📖 متنِ این درس هنوز آماده نشده — فهرست و عنوانش سرِ جایش است و به‌زودی اضافه می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            if (!pack.pdfOnly) {
                Text(
                    if (teachDone) "✅ دوره‌ی اول تدریس کامل شده"
                    else "ورود به درس و سربرگ‌ها باز است",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (teachDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            val bookCode = pack.bookCode
            val gate = LessonAccess.gate(ctx, bookCode, packId)
            var gateDialog by remember(packId) { mutableStateOf(false) }
            if (gateDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { gateDialog = false },
                    confirmButton = { androidx.compose.material3.TextButton(onClick = { gateDialog = false }) { Text("باشه") } },
                    title = { Text("نیاز به تهیه اشتراک") },
                    text = { Text("فصل ۱ ریاضی و اولین درس هر کتاب دیگر بدون اشتراک باز است. برای بقیهٔ درس‌ها اشتراک فعال لازم است.") },
                )
            }
            Button(
                onClick = {
                    if (gate == LessonAccess.Gate.Open) onTeach(packId) else gateDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        gate != LessonAccess.Gate.Open -> "نیاز به تهیه اشتراک"
                        pack.pdfOnly -> "باز کردن"
                        else -> "ورود به درس"
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

/** کارت ساده‌ی ردیف‌های بدون درس (ستایش/نیایش/واژه‌نامه/جلسه/…). */
@Composable
private fun StaticCard(node: TocNode, depth: Int) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(start = (depth * 10).dp, top = 3.dp, bottom = 3.dp),
    ) {
        Text(
            node.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/**
 * یک گره از منوی کتاب. گرهٔ `plain` یک ردیف ساده است؛ گرهٔ `lesson` آکاردئونی
 * است که سربرگ‌هایش (تدریس، تمرینات کتابی، نکات گرامری، …) داخلش باز می‌شوند.
 * هر ردیف که فایل آماده نداشته باشد با برچسب «به‌زودی» به اسپیس‌هولدر می‌رود.
 */
@Composable
private fun BookMenuNode(
    node: BooksMenu.Node,
    nodeId: String,
    openId: String,
    onToggle: (String) -> Unit,
    onOpen: (String, String) -> Unit,
) {
    val open = openId == nodeId
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (node.isLesson) onToggle(nodeId)
                        else onOpen(BooksMenu.destination(node.key, node.ready), node.title)
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (node.isLesson) (if (open) "▾" else "▸") else "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(node.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (!node.isLesson && !node.hasContent) SoonBadge()
            }
            if (node.isLesson && open) {
                Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, bottom = 8.dp)) {
                    node.tabs.forEach { tab ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(BooksMenu.destination(tab.key, tab.ready), tab.title) }
                                .padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                tab.title,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (!tab.hasContent) SoonBadge()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoonBadge() {
    Text(
        "به‌زودی",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
