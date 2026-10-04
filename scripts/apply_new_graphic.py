#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Apply the New graphic specification to the checked-out repository.

The script is idempotent. After the first successful transformation it makes
no further source changes.
"""

from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, value: str) -> None:
    (ROOT / rel).write_text(value, encoding="utf-8", newline="\n")


def must_replace(text: str, old: str, new: str, label: str, count: int = 1) -> str:
    if old not in text:
        raise RuntimeError(f"anchor not found: {label}")
    return text.replace(old, new, count)


# Shared notebook and book visual engine.
path = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt"
s = read(path)

if "import androidx.compose.ui.graphics.Brush\n" not in s:
    s = must_replace(
        s,
        "import androidx.compose.ui.geometry.Offset\n",
        "import androidx.compose.ui.geometry.Offset\nimport androidx.compose.ui.graphics.Brush\n",
        "Brush import",
    )

paper = '''@Composable
fun NotebookPaper(
    modifier: Modifier = Modifier,
    header: String = "",
    headerAlign: TextAlign = TextAlign.Right,
    showVerticalGuides: Boolean = true,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(PAGE_ASPECT)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFFCF2)),
    ) {
        val density = LocalDensity.current
        val lineHeightPx = with(density) { LINE_HEIGHT_SP.dp.toPx() }
        val side = with(density) { SIDE_GUTTER_DP.dp.toPx() }
        val bottom = with(density) { BOTTOM_GUTTER_DP.dp.toPx() }
        val rightMargin = with(density) { (SIDE_GUTTER_DP - 2).dp.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFFFFFCF2))
            var y = 0f
            while (y <= size.height - bottom) {
                drawLine(
                    color = Color(0xFFB9CEE5),
                    start = Offset(side * 0.55f, y),
                    end = Offset(size.width - side * 0.25f, y),
                    strokeWidth = 1.05f,
                )
                y += lineHeightPx
            }

            if (showVerticalGuides) {
                drawLine(
                    color = Color(0xFF9FB5C8),
                    start = Offset(size.width - rightMargin, 0f),
                    end = Offset(size.width - rightMargin, size.height - bottom),
                    strokeWidth = 1.7f,
                )
                drawLine(
                    color = Color(0xFF9FB5C8),
                    start = Offset(
                        size.width - rightMargin + with(density) { 6.dp.toPx() },
                        0f,
                    ),
                    end = Offset(
                        size.width - rightMargin + with(density) { 6.dp.toPx() },
                        size.height - bottom,
                    ),
                    strokeWidth = 1.2f,
                )
            }

            drawLine(
                color = Color(0x40FFFFFF),
                start = Offset(1f, 2f),
                end = Offset(1f, size.height - 2f),
                strokeWidth = 1.8f,
            )
            drawLine(
                color = Color(0x18000000),
                start = Offset(size.width - 1.2f, 2f),
                end = Offset(size.width - 1.2f, size.height - 2f),
                strokeWidth = 1.2f,
            )
        }

        if (header.isNotBlank()) {
            Text(
                text = header,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = SIDE_GUTTER_DP.dp, end = SIDE_GUTTER_DP.dp, top = 1.dp),
                textAlign = headerAlign,
                fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                fontSize = 17.sp,
                lineHeight = LINE_HEIGHT_SP.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1B3448),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(Modifier.fillMaxSize().padding(top = (LINE_HEIGHT_SP * 2).dp)) { content() }
        } else {
            content()
        }
    }
}

'''
s = re.sub(
    r'@Composable\nfun NotebookPaper\(.*?(?=@Composable\nfun NotebookBookPage\()',
    paper,
    s,
    count=1,
    flags=re.S,
)

book = '''@Composable
fun NotebookBookPage(
    pageNumber: Int,
    pageCount: Int,
    stackPages: Int,
    modifier: Modifier = Modifier,
    fullScreen: Boolean = false,
    content: @Composable () -> Unit,
) {
    val pageModifier = if (fullScreen) {
        modifier.fillMaxHeight().aspectRatio(PAGE_ASPECT).padding(1.dp)
    } else {
        modifier.fillMaxWidth().aspectRatio(PAGE_ASPECT).padding(6.dp)
    }
    Box(pageModifier) {
        val stack = stackPages.coerceIn(0, 8)
        repeat(stack) { index ->
            Box(
                Modifier
                    .matchParentSize()
                    .padding(
                        start = ((index + 1) * 2.1f).dp,
                        top = ((index + 1) * 1.15f).dp,
                    )
                    .shadow(1.8.dp, RoundedCornerShape(4.dp))
                    .background(Color(0xFFF5F0E5), RoundedCornerShape(4.dp)),
            )
        }

        Box(
            Modifier
                .matchParentSize()
                .shadow(16.dp, RoundedCornerShape(4.dp))
                .background(Color(0xFFFFFCF2), RoundedCornerShape(4.dp)),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x14000000),
                                Color(0x26000000),
                            ),
                        ),
                    ),
            )
            content()

            Canvas(Modifier.matchParentSize()) {
                drawLine(
                    color = Color(0x28FFFFFF),
                    start = Offset(2f, 2f),
                    end = Offset(size.width - 2f, 2f),
                    strokeWidth = 1.6f,
                )
                drawLine(
                    color = Color(0x22000000),
                    start = Offset(size.width - 1.2f, 5f),
                    end = Offset(size.width - 1.2f, size.height - 5f),
                    strokeWidth = 1.1f,
                )
            }

            Text(
                text = "\${pageNumber.coerceAtLeast(1)} / \${pageCount.coerceAtLeast(1)}",
                color = Color(0xFF58718A),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp),
            )
        }
    }
}

'''
s = re.sub(
    r'@Composable\nfun NotebookBookPage\(.*?(?=@Composable\nfun LinedNotebookInput\()',
    book,
    s,
    count=1,
    flags=re.S,
)

align = '''@Composable
fun NotebookAlignmentPicker(
    value: TextAlign,
    onValueChange: (TextAlign) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onValueChange(TextAlign.Right) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.FormatAlignRight,
                contentDescription = "راست‌چین",
                tint = if (value == TextAlign.Right) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onValueChange(TextAlign.Center) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.FormatAlignCenter,
                contentDescription = "وسط‌چین",
                tint = if (value == TextAlign.Center) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onValueChange(TextAlign.Left) },
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Default.FormatAlignLeft,
                contentDescription = "چپ‌چین",
                tint = if (value == TextAlign.Left) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

'''
s = re.sub(
    r'@Composable\nfun NotebookAlignmentPicker\(.*?(?=@Composable\nfun NotebookTitlePicker\()',
    align,
    s,
    count=1,
    flags=re.S,
)
write(path, s)


# Real pattern interaction: dot + line + haptic + live current segment.
path = "shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/PinLockGate.kt"
s = read(path)

if "import android.view.HapticFeedbackConstants\n" not in s:
    s = s.replace(
        "package com.hamyareman.ir.platform.core.designsystem\n\n",
        "package com.hamyareman.ir.platform.core.designsystem\n\nimport android.view.HapticFeedbackConstants\n",
        1,
    )
if "import androidx.compose.ui.platform.LocalView\n" not in s:
    s = s.replace(
        "import androidx.compose.ui.text.input.KeyboardType\n",
        "import androidx.compose.ui.platform.LocalView\nimport androidx.compose.ui.text.input.KeyboardType\n",
        1,
    )

pattern = '''@Composable
fun PatternLockGrid(
    pattern: String,
    onPatternChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    val latestPattern by androidx.compose.runtime.rememberUpdatedState(pattern)
    val latestChange by androidx.compose.runtime.rememberUpdatedState(onPatternChange)
    val view = LocalView.current
    var pointer by remember { mutableStateOf<Offset?>(null) }

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val dot = MaterialTheme.colorScheme.onSurfaceVariant
    val background = MaterialTheme.colorScheme.background
    val points = remember {
        listOf(
            Offset(0f, 0f), Offset(.5f, 0f), Offset(1f, 0f),
            Offset(0f, .5f), Offset(.5f, .5f), Offset(1f, .5f),
            Offset(0f, 1f), Offset(.5f, 1f), Offset(1f, 1f),
        )
    }

    fun hit(offset: Offset, width: Float, height: Float): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        points.forEachIndexed { i, p ->
            val center = Offset(p.x * width, p.y * height)
            val dx = offset.x - center.x
            val dy = offset.y - center.y
            val d2 = dx * dx + dy * dy
            val radius = minOf(width, height) * .145f
            if (d2 < bestDistance && d2 <= radius * radius) {
                best = i
                bestDistance = d2
            }
        }
        return best
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(surface.copy(alpha = .16f))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos ->
                        pointer = pos
                        hit(pos, size.width, size.height)?.let { idx ->
                            val digit = (idx + 1).toString()
                            if (!latestPattern.contains(digit)) {
                                latestChange(digit)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        pointer = change.position
                        hit(change.position, size.width, size.height)?.let { idx ->
                            val digit = (idx + 1).toString()
                            if (!latestPattern.contains(digit)) {
                                latestChange(latestPattern + digit)
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        }
                    },
                    onDragEnd = { pointer = null },
                    onDragCancel = { pointer = null },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val selected = latestPattern.mapNotNull { it.digitToIntOrNull()?.minus(1) }

            selected.zipWithNext().forEach { (a, b) ->
                drawLine(
                    color = primary,
                    start = Offset(points[a].x * size.width, points[a].y * size.height),
                    end = Offset(points[b].x * size.width, points[b].y * size.height),
                    strokeWidth = 8f,
                )
            }

            if (pointer != null && selected.isNotEmpty()) {
                val last = selected.last()
                drawLine(
                    color = primary.copy(alpha = .55f),
                    start = Offset(points[last].x * size.width, points[last].y * size.height),
                    end = pointer!!,
                    strokeWidth = 6f,
                )
            }

            points.forEachIndexed { i, p ->
                val center = Offset(p.x * size.width, p.y * size.height)
                val chosen = selected.contains(i)
                if (chosen) {
                    drawCircle(primary.copy(alpha = .14f), 30f, center)
                    drawCircle(primary.copy(alpha = .28f), 21f, center)
                }
                drawCircle(if (chosen) primary else dot, if (chosen) 15f else 10.5f, center)
                if (chosen) drawCircle(background, 5.5f, center)
            }
        }
    }
}

'''
s = re.sub(
    r'@Composable\nfun PatternLockGrid\(.*?(?=\n/\*\*\n \* دروازهٔ قفل PIN)',
    pattern,
    s,
    count=1,
    flags=re.S,
)
write(path, s)


# Lighter premium SectionCard for shared design-system sections.
path = "shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/Components.kt"
s = read(path)
if "import androidx.compose.material3.CardDefaults\n" not in s:
    s = s.replace(
        "import androidx.compose.material3.Card\n",
        "import androidx.compose.material3.Card\nimport androidx.compose.material3.CardDefaults\n",
        1,
    )
if "import androidx.compose.foundation.shape.RoundedCornerShape\n" not in s:
    s = s.replace(
        "import androidx.compose.foundation.layout.Column\n",
        "import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.shape.RoundedCornerShape\n",
        1,
    )
s = re.sub(
    r'@Composable\nfun SectionCard\(title: String, body: String, modifier: Modifier = Modifier, onClick: \(\(\) -> Unit\)\? = null\) \{.*?\n\}',
    '''@Composable
fun SectionCard(title: String, body: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 5.dp,
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}''',
    s,
    count=1,
    flags=re.S,
)
write(path, s)


# Poetry persistence + collapsed guide + full-height viewer spacing.
path = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/PoetryBookScreen.kt"
s = read(path)
s = must_replace(
    s,
    'add(Poem(o.optString("id"), o.optLong("createdAt"), o.optString("title"), o.optString("type"), o.optString("cipher")))',
    'add(Poem(o.optString("id"), o.optLong("createdAt"), o.optString("title"), o.optString("type"), o.optString("cipher"), o.optString("alignment", "right")))',
    "poetry alignment restore",
)
if "var guideExpanded by remember" not in s:
    s = must_replace(
        s,
        'var alignment by remember { mutableStateOf(androidx.compose.ui.text.style.TextAlign.Right) }',
        'var alignment by remember { mutableStateOf(androidx.compose.ui.text.style.TextAlign.Right) }\n    var guideExpanded by remember { mutableStateOf(false) }',
        "poetry guide state",
    )

guide = '''            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { guideExpanded = !guideExpanded },
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("✒️", style = MaterialTheme.typography.titleLarge)
                                Text("راهنمای دفتر شعر", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(if (guideExpanded) "⌃" else "⌄", style = MaterialTheme.typography.titleMedium)
                        }
                        if (guideExpanded) {
                            Text(
                                "غزل، قصیده، دوبیتی، رباعی، قطعه و مثنوی: هر بیت دو مصراع مستقل دارد و در نمایش کتاب بدون کادر و با تراز حرفه‌ای دیده می‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "نمونه: «بهار آمد / و دل دوباره جوان شد» — روی هر مصراع می‌توان جداگانه تمرکز کرد.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "شعر سپید و نثر شاعرانه آزادتر و مانند صفحهٔ شعر نمایش داده می‌شوند.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
'''
s = re.sub(
    r'            item \{\n                Card\(Modifier\.fillMaxWidth\(\)\) \{\n                    Column\(Modifier\.padding\(12\.dp\), verticalArrangement = Arrangement\.spacedBy\(6\.dp\)\) \{\n                        Text\("راهنمای دفتر شعر".*?\n                    \}\n                \}\n            \}\n            item \{ Text\("شعرهای ذخیره‌شده"',
    guide + '            item { Text("شعرهای ذخیره‌شده"',
    s,
    count=1,
    flags=re.S,
)
s = s.replace(
    "modifier = Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 150.dp, bottom = 32.dp),",
    "modifier = Modifier.fillMaxSize().padding(start = 76.dp, end = 76.dp, top = 0.dp, bottom = 32.dp),",
)
write(path, s)


# Diary / notebook rendered pages: do not waste half the page above text.
path = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt"
s = read(path)
s = s.replace(
    "padding(start = 74.dp, end = 74.dp, top = 150.dp, bottom = 28.dp)",
    "padding(start = 74.dp, end = 74.dp, top = 0.dp, bottom = 28.dp)",
)
s = s.replace(
    "padding(start = 76.dp, end = 76.dp, top = 150.dp, bottom = 32.dp)",
    "padding(start = 76.dp, end = 76.dp, top = 0.dp, bottom = 32.dp)",
)
write(path, s)


# Safe Space: backup/restore first, security remains last and collapsed.
path = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/SafeSpaceScreens.kt"
s = read(path)
needle = "            sections.forEach { section ->"
if needle in s and "            SafeSpaceBackupCard()" in s:
    pos = s.find(needle)
    before = s[:pos]
    after = s[pos:]
    if "            SafeSpaceBackupCard()" not in before:
        s = before + "            SafeSpaceBackupCard()\n" + after
        last = s.rfind("            SafeSpaceBackupCard()")
        inserted = s.find("            SafeSpaceBackupCard()")
        if last != inserted:
            s = s[:last] + s[last + len("            SafeSpaceBackupCard()\n"):]
write(path, s)


# Personal album: destructive action confirmation.
path = "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/SecureMediaGallery.kt"
s = read(path)
if "import androidx.compose.material3.AlertDialog\n" not in s:
    s = s.replace(
        "import androidx.compose.material3.Card\n",
        "import androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.Card\n",
        1,
    )
if "var confirmDelete by remember(item.path)" not in s:
    s = must_replace(
        s,
        "var revision by remember(item.path) { mutableIntStateOf(0) }",
        "var revision by remember(item.path) { mutableIntStateOf(0) }\n    var confirmDelete by remember(item.path) { mutableStateOf(false) }",
        "album delete state",
    )
s = must_replace(
    s,
    'IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }',
    'IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "حذف") }',
    "album delete button",
)
marker = """        }
    }
}

@Composable
private fun ZoomableSecureImage"""
dialog = """        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("حذف این رسانه؟") },
                text = { Text("این فایل از پوشهٔ خصوصی برنامه حذف می‌شود و برگشت‌پذیر نیست.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        onDelete()
                    }) { Text("حذف") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) { Text("انصراف") }
                },
            )
        }
    }
}

@Composable
private fun ZoomableSecureImage"""
if marker not in s:
    raise RuntimeError("album viewer closing anchor not found")
s = s.replace(marker, dialog, 1)
write(path, s)


# Version bump.
path = "apps/hamyar-app/build.gradle.kts"
s = read(path)
s = must_replace(s, "?: 253", "?: 254", "versionCode 253 -> 254")
s = must_replace(s, '?: "2.5.3"', '?: "2.5.4"', "versionName 2.5.3 -> 2.5.4")
write(path, s)


# Derive the forced 2.5.4 release workflow from the proven 2.5.3 workflow.
source_workflow = read(".github/workflows/arena-release-grade9-v2.5.3.yml")
wf = source_workflow
wf = wf.replace("Release grade 9 v2.5.3", "Release grade 9 v2.5.4")
wf = wf.replace("v2.5.3", "v2.5.4")
wf = wf.replace("2.5.3", "2.5.4")
wf = wf.replace("253", "254")
wf = wf.replace("branches:\n      - 'main'", "branches:\n      - 'New-graphic'")
wf = wf.replace("hamyar-grade9-2.5.3.apk", "hamyar-grade9-2.5.4.apk")
wf = wf.replace("hamyar-grade9-2.5.3", "hamyar-grade9-2.5.4")
wf = wf.replace("arena-release-grade9-v2.5.3", "arena-release-grade9-v2.5.4")

new_notes = (
    "بازطراحی موتور مشترک دفترچه، دفتر خاطرات و دفتر شعر بر اساس گرافیک جدید و نمونه‌های مرجع|"
    "کاغذ تمام‌قد با شروع نوشتن از سطر اول، یک سطر فاصلهٔ عنوان و خطوط حاشیهٔ RTL|"
    "جلد، spine، ضخامت و stack واقعی صفحات با سایه‌های چندلایه و حس کتاب|"
    "تورق افقی RTL، ذخیرهٔ تمام صفحات overflow و حفظ وضعیت هنگام بازگشایی|"
    "نمای شعر بدون دو خط عمودی و چینش دو مصراع با انتخاب ظریف و راهنمای جمع‌شونده|"
    "قفل الگوی واقعی نقطه‌وخط با بازخورد لمسی و خط زنده هنگام drag|"
    "آلبوم شخصی adaptive با پخش واقعی صوت و ویدئو و تأیید حذف رسانه|"
    "آپدیت اجباری ۲٫۵٫۴ برای نسخه‌های پایین‌تر"
)
wf = re.sub(r'--notes "[^"]*"', f'--notes "{new_notes}"', wf, count=1)
write(".github/workflows/arena-release-grade9-v2.5.4.yml", wf)


# Release/change document.
doc = """# انتشار همیار من نهم — نسخهٔ ۲٫۵٫۴

## وضعیت
- شاخه: New-graphic
- نسخه: 2.5.4
- versionCode: 254
- نوع انتشار: اجباری
- min: 254
- کانال: stable

## متن انتشار
- بازطراحی موتور مشترک دفترچه، دفتر خاطرات و دفتر شعر بر اساس گرافیک جدید و نمونه‌های مرجع
- کاغذ تمام‌قد با شروع نوشتن از سطر اول، یک سطر فاصلهٔ عنوان و خطوط حاشیهٔ RTL
- جلد، spine، ضخامت و stack صفحات با عمق و سایه‌های چندلایه
- تورق افقی RTL و حفظ همهٔ صفحات overflow در persistence
- نمایش شعر بدون دو خط عمودی، چینش دو مصراع و راهنمای جمع‌شونده
- قفل الگوی واقعی نقطه‌وخط با haptic و خط زنده هنگام drag
- آلبوم شخصی adaptive، پخش واقعی رسانه و تأیید حذف
- فعال‌سازی آپدیت اجباری برای نسخه‌های پایین‌تر از 2.5.4

## قرارداد انتشار
ورکفلو نسخهٔ 2.5.4 از مسیر تأییدشدهٔ انتشار نسخهٔ 2.5.3 مشتق شده است و ترتیب «اول ساخت و انتشار فایل، بعد اعلام سرور» را حفظ می‌کند. پس از تأیید APK و هر دو مقصد، ردیف app_release_grade9 با min=254 به‌روزرسانی می‌شود.
"""
write("docs/maintenance/grade9/انتشار-2.5.4.md", doc)


# Remove the one-shot patcher/helper workflow from the final branch.
for rel in ("scripts/apply_new_graphic.py", ".github/workflows/apply-new-graphic.yml"):
    p = ROOT / rel
    if p.exists():
        p.unlink()

print("New graphic transformation complete.")
