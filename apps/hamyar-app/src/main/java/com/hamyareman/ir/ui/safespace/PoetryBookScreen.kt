package com.hamyareman.ir.ui.safespace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.components.RealisticDeskFrame
import com.hamyareman.ir.ui.components.DesignAsset
import com.hamyareman.ir.ui.components.RealisticBookPager
import com.hamyareman.ir.ui.components.BookStage
import com.hamyareman.ir.ui.components.BookOpening
import com.hamyareman.ir.ui.components.BookSkin
import com.hamyareman.ir.ui.components.BookSkinCover
import com.hamyareman.ir.ui.components.BookSkinSpread
import com.hamyareman.ir.ui.components.BookFlipper
import com.hamyareman.ir.ui.components.DraftAutoSave
import com.hamyareman.ir.ui.components.readDraft
import com.hamyareman.ir.ui.components.writeDraft
import org.json.JSONObject
import com.hamyareman.ir.ui.components.SkinGeometry
import com.hamyareman.ir.ui.components.SkinnedNotebookEditor
import com.hamyareman.ir.ui.components.firstPageFit
import com.hamyareman.ir.ui.components.measureLineCount
import com.hamyareman.ir.ui.components.rememberBookFlipState
import com.hamyareman.ir.ui.components.skinTextStyle
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import com.hamyareman.ir.ui.components.NOTEBOOK_PAGE_SEPARATOR
import com.hamyareman.ir.ui.components.NotebookBookPage
import com.hamyareman.ir.ui.components.NotebookPaper
import com.hamyareman.ir.ui.components.NotebookAlignmentPicker
import com.hamyareman.ir.ui.components.nextRegisteredTitle
import com.hamyareman.ir.ui.components.notebookAlignmentWire
import com.hamyareman.ir.ui.components.notebookTextAlignFromWire
import com.hamyareman.ir.ui.components.oppositeTextAlign
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val POETRY_STORE = "hamyar_poetry_book"
private const val POEMS_KEY = "poems"

/** جلد، کتاب باز و ورق دفتر شعر: PNG دوربری‌شدهٔ چرم قهوه‌ای (بوم مشترک ۱۰۵۹×۱۴۸۶). */
private val poetrySkin = BookSkin.LeatherBrown

private data class Poem(
    val id: String,
    val createdAt: Long,
    val title: String,
    val type: String,
    val cipher: String,
    val alignment: String = "right",
)

/** نوع‌های اصلی؛ «سایر» فهرست نوع‌های دیگر را باز می‌کند. */
private val mainPoemTypes = listOf("غزل", "قصیده", "دوبیتی", "رباعی", "مثنوی")

private val otherPoemTypes = listOf(
    "قطعه", "شعر نو", "نیمایی", "سپید", "چهارپاره", "مستزاد", "ترکیب‌بند", "ترجیع‌بند",
    "مسمط", "مخمس", "تک‌بیت", "ترانه", "نثر شاعرانه", "هایکو", "شعر کودک", "سایر",
)

private val poemTypes = mainPoemTypes + otherPoemTypes

/** قالب‌هایی که هر بیت دو مصراع مستقل دارد. */
private fun twoHemistich(type: String): Boolean =
    type in setOf(
        "غزل", "قصیده", "دوبیتی", "رباعی", "قطعه", "مثنوی", "مستزاد", "ترکیب‌بند", "ترجیع‌بند", "تک‌بیت",
    )

private const val POEM_DRAFT_KEY = "draft_poem"

/** صفحه‌های تورق ۳ سایز بزرگ‌تر از صفحهٔ تایپ‌اند. */
private const val POETRY_VIEW_BONUS_SP = 3f

private fun readPoems(container: com.hamyareman.ir.di.AppContainer): List<Poem> = runCatching {
    val arr = JSONArray(container.store.getString(POEMS_KEY, "[]"))
    buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(Poem(o.optString("id"), o.optLong("createdAt"), o.optString("title"), o.optString("type"), o.optString("cipher"), o.optString("alignment", "right")))
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writePoems(container: com.hamyareman.ir.di.AppContainer, poems: List<Poem>) {
    val arr = JSONArray()
    poems.forEach { poem ->
        arr.put(
            JSONObject()
                .put("id", poem.id)
                .put("createdAt", poem.createdAt)
                .put("title", poem.title)
                .put("type", poem.type)
                .put("cipher", poem.cipher)
                .put("alignment", poem.alignment),
        )
    }
    container.store.putString(POEMS_KEY, arr.toString())
}

@Composable
fun PoetryBookScreen(onBack: () -> Unit, onHelp: () -> Unit = {}) {
    val container = LocalAppContainer.current
    var poems by remember { mutableStateOf(readPoems(container)) }
    var title by remember { mutableStateOf(poemTypes.first()) }
    var type by remember { mutableStateOf(poemTypes.first()) }
    var text by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<Poem?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var alignment by remember { mutableStateOf(androidx.compose.ui.text.style.TextAlign.Right) }
    var guideExpanded by remember { mutableStateOf(false) }
    val draftStore = remember { container.store }
    var draftReady by remember { mutableStateOf(false) }

    fun draftSnapshot(): String? =
        if (text.isBlank()) null
        else JSONObject()
            .put("title", title)
            .put("type", type)
            .put("text", text)
            .put("align", notebookAlignmentWire(alignment))
            .toString()

    fun restoreDraft(): Boolean {
        val d = readDraft(draftStore, POEM_DRAFT_KEY) { container.encryptor.decrypt(it) } ?: return false
        type = d.optString("type", type).ifBlank { poemTypes.first() }
        title = d.optString("title", title).ifBlank { type }
        text = d.optString("text", "")
        alignment = notebookTextAlignFromWire(d.optString("align"))
        return text.isNotBlank()
    }

    LaunchedEffect(Unit) {
        if (restoreDraft()) notice = "پیش‌نویس قبلی‌ات برگشت؛ از همان‌جا ادامه بده."
        draftReady = true
    }
    DraftAutoSave(
        enabled = draftReady && editingId == null,
        current = draftSnapshot(),
        onSave = { writeDraft(draftStore, POEM_DRAFT_KEY, it) { s -> container.encryptor.encrypt(s) } },
    )

    fun reset() {
        editingId = null
        title = poemTypes.first()
        type = poemTypes.first()
        text = ""
        alignment = androidx.compose.ui.text.style.TextAlign.Right
        notice = null
    }

    fun edit(poem: Poem) {
        if (editingId == null) {
            writeDraft(draftStore, POEM_DRAFT_KEY, draftSnapshot()) { s -> container.encryptor.encrypt(s) }
        }
        editingId = poem.id
        title = poem.title.ifBlank { poem.type.ifBlank { poemTypes.first() } }
        type = poem.type.ifBlank { poemTypes.first() }
        text = container.encryptor.decrypt(poem.cipher).orEmpty()
        alignment = notebookTextAlignFromWire(poem.alignment)
        notice = "این شعر برای ویرایش باز شد."
    }

    fun save() {
        if (text.isBlank()) {
            notice = "اول شعر را بنویس."
            return
        }
        val changed = Poem(
            id = editingId ?: UUID.randomUUID().toString(),
            createdAt = editingId?.let { id -> poems.firstOrNull { it.id == id }?.createdAt } ?: System.currentTimeMillis(),
            title = if (editingId == null) nextRegisteredTitle(title.ifBlank { type }, poems.map { it.title }) else title.ifBlank { type },
            type = type,
            cipher = container.encryptor.encrypt(text),
            alignment = notebookAlignmentWire(alignment),
        )
        val wasEditing = editingId != null
        poems = (listOf(changed) + poems.filterNot { it.id == changed.id }).sortedByDescending { it.createdAt }
        writePoems(container, poems)
        if (!wasEditing) writeDraft(draftStore, POEM_DRAFT_KEY, null) { it }
        reset()
        if (wasEditing) restoreDraft()
        notice = "شعر ذخیره شد."
    }

    RealisticDeskFrame(
        backgroundKey = DesignAsset.POETRY_BG,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            com.hamyareman.ir.platform.core.designsystem.AppTopBar("دفتر شعر", onBack, onHelp = onHelp)
        LazyColumn(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("دفتر شعر", style = MaterialTheme.typography.titleMedium)
                        PoetryTypePicker(
                            value = type,
                            onValueChange = { selected ->
                                type = selected
                                if (editingId == null) title = selected
                            },
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(90) },
                            label = { Text("عنوان شعر (قابل ویرایش)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        NotebookAlignmentPicker(alignment, { alignment = it })
                        val headerText = if (title.isBlank()) type else title
                        if (twoHemistich(type)) {
                            PoetryHemistichEditor(
                                value = text,
                                onValueChange = { text = it },
                                header = headerText,
                            )
                        } else {
                            SkinnedNotebookEditor(
                                skin = poetrySkin,
                                value = text,
                                onValueChange = { text = it },
                                header = headerText,
                                textAlign = alignment,
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::save, modifier = Modifier.weight(2f)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null)
                                Text(" ذخیره")
                            }
                            OutlinedButton(
                                onClick = ::reset,
                                enabled = editingId != null,
                                modifier = Modifier.weight(1f),
                            ) { Text("لغو") }
                        }
                        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item {
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
            item { Text("شعرهای ذخیره‌شده", style = MaterialTheme.typography.titleMedium) }
            items(poems, key = { it.id }) { poem ->
                Card(Modifier.fillMaxWidth().clickable { viewer = poem }) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(
                                poem.title.ifBlank { "شعر من" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Text(
                            JalaliDate.stampFa(poem.createdAt),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            poem.type,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        IconButton(onClick = { edit(poem) }) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش شعر")
                        }
                        IconButton(onClick = {
                            poems = poems.filterNot { it.id == poem.id }
                            writePoems(container, poems)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف شعر")
                        }
                    }
                }
            }
        }
    }
    }

    viewer?.let { poem ->
        PoetryViewer(
            poem = poem,
            text = container.encryptor.decrypt(poem.cipher).orEmpty(),
            onEdit = { edit(poem); viewer = null },
            onClose = { viewer = null },
        )
    }
}

data class PoetryHemistichRow(
    val right: String = "",
    val left: String = "",
)

/** ویرایشگر شعر دو مصراعی روی ورق چرمی: هر بیت یک سطر، دو مصراع کنار هم، روی خط‌های ورق. */
@Composable
private fun PoetryHemistichEditor(
    value: String,
    onValueChange: (String) -> Unit,
    header: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val geo = remember(maxWidth) { SkinGeometry(poetrySkin, maxWidth.value) }
        val style = remember(geo, density) { skinTextStyle(geo.line, density, TextAlign.Right) }
        val gap = 14.dp
        val cellPx = with(density) { ((geo.textWidth - gap) / 2).roundToPx() }
        val rows = remember(value) {
            val lines = value.replace("\r", "").split('\n')
            lines.chunked(2).map {
                PoetryHemistichRow(right = it.getOrNull(0).orEmpty(), left = it.getOrNull(1).orEmpty())
            }.ifEmpty { listOf(PoetryHemistichRow()) }
        }
        // همیشه یک سطر خالی برای ادامهٔ شعر در انتها هست.
        val shown = remember(rows) {
            val last = rows.last()
            if (last.right.isNotBlank() || last.left.isNotBlank()) rows + PoetryHemistichRow() else rows
        }
        val firstCap = (poetrySkin.lineCount - if (header.isNotBlank()) 2 else 0).coerceAtLeast(1)
        val pageRanges = remember(shown.size, firstCap) {
            val out = mutableListOf<IntRange>()
            var start = 0
            var cap = firstCap
            while (start < shown.size) {
                val end = minOf(shown.size, start + cap)
                out += start until end
                start = end
                cap = poetrySkin.lineCount
            }
            out
        }
        val pager = rememberPagerState(pageCount = { pageRanges.size.coerceAtLeast(1) })

        fun update(absRow: Int, right: String? = null, left: String? = null) {
            val list = shown.toMutableList()
            val r = list[absRow]
            list[absRow] = r.copy(right = right ?: r.right, left = left ?: r.left)
            onValueChange(list.joinToString("\n") { it.right + "\n" + it.left }.trimEnd('\n'))
        }

        fun accepts(old: String, new: String): Boolean =
            new.length <= old.length || (new.length <= 180 && measureLineCount(measurer, new, style, cellPx) <= 1)

        Column {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth(),
                reverseLayout = true,
                beyondViewportPageCount = 1,
            ) { pageIndex ->
                BookSkinSpread(poetrySkin) { line, _ ->
                    Column(Modifier.fillMaxSize()) {
                        if (pageIndex == 0 && header.isNotBlank()) {
                            Text(
                                header,
                                Modifier.fillMaxWidth().height(line),
                                style = style.copy(textAlign = oppositeTextAlign(TextAlign.Right)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(line))
                        }
                        val range = pageRanges.getOrNull(pageIndex) ?: IntRange.EMPTY
                        range.forEach { absRow ->
                            val row = shown[absRow]
                            Row(
                                Modifier.fillMaxWidth().height(line),
                                horizontalArrangement = Arrangement.spacedBy(gap),
                            ) {
                                PoetryHemistichCell(
                                    value = row.right,
                                    style = style,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onValueChange = { if (accepts(row.right, it)) update(absRow, right = it) },
                                )
                                PoetryHemistichCell(
                                    value = row.left,
                                    style = style,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onValueChange = { if (accepts(row.left, it)) update(absRow, left = it) },
                                )
                            }
                        }
                    }
                }
            }
            Text(
                "صفحهٔ ${pager.currentPage + 1} از ${pageRanges.size.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PoetryHemistichCell(
    value: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        cursorBrush = SolidColor(Color(0xFF27485C)),
        modifier = modifier,
    )
}

/** یک «واحد» روی ورق: یک بیت دو مصراعی (یک یا چند سطر) یا یک سطر آزاد (یک یا چند سطر خط‌دار). */
private data class PoemUnit(val rows: Int, val first: String, val second: String?, val key: String)

/**
 * انتخاب نوع شعر: نوع‌های اصلی و یک گزینهٔ «سایر» که با لمس، نوع‌های دیگر
 * (شعر نو، نیمایی، سپید، چهارپاره و …) را در همان منو باز می‌کند.
 */
@Composable
private fun PoetryTypePicker(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var othersOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { othersOpen = value in otherPoemTypes; expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("نوع شعر: " + value, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            mainPoemTypes.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    },
                )
            }
            androidx.compose.material3.DropdownMenuItem(
                text = { Text(if (othersOpen) "سایر ⌃" else "سایر ⌄") },
                onClick = { othersOpen = !othersOpen },
            )
            if (othersOpen) {
                otherPoemTypes.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("   " + option) },
                        onClick = {
                            expanded = false
                            onValueChange(option)
                        },
                    )
                }
            }
        }
    }
}

private fun buildPoemUnits(
    text: String,
    twoCol: Boolean,
    rowsOf: (String, Boolean) -> Int,
): List<PoemUnit> {
    val flat = text.replace("\r", "")
        .split(NOTEBOOK_PAGE_SEPARATOR)
        .flatMap { it.split('\n') }
        .map { it.trimEnd() }
    if (twoCol) {
        return flat.filter { it.isNotBlank() }.chunked(2).mapIndexed { i, pair ->
            val a = pair.getOrNull(0).orEmpty()
            val b = pair.getOrNull(1).orEmpty()
            PoemUnit(maxOf(rowsOf(a, true), rowsOf(b, true)), a, b, "pair-$i")
        }
    }
    return flat.mapIndexed { i, line -> PoemUnit(rowsOf(line, false), line, null, "line-$i") }
}

/**
 * چیدن واحدها روی صفحه‌ها بر اساس ظرفیت خط‌های ورق؛ صفحهٔ اول برای عنوان و فاصله کم می‌کند.
 * سطر آزادِ بلندتر از باقی‌ماندهٔ صفحه روی صفحهٔ بعد ادامه پیدا می‌کند.
 */
private fun paginatePoem(
    units: List<PoemUnit>,
    lineCount: Int,
    firstFree: Int,
    split: (String, Int) -> Pair<String, String>,
    rowsOf: (String) -> Int,
): List<List<PoemUnit>> {
    val pages = mutableListOf<List<PoemUnit>>()
    var current = mutableListOf<PoemUnit>()
    var free = firstFree.coerceAtLeast(1)
    val queue = java.util.ArrayDeque(units)
    var guard = 0
    while (queue.isNotEmpty() && guard++ < 5000) {
        val u = queue.pollFirst() ?: break
        when {
            u.rows <= free -> {
                current += u
                free -= u.rows
            }
            current.isNotEmpty() -> {
                pages += current
                current = mutableListOf()
                free = lineCount
                queue.addFirst(u)
            }
            u.second == null -> {
                val (fit, rest) = split(u.first, free)
                if (rest.isEmpty()) {
                    current += u
                    free = 0
                } else {
                    current += PoemUnit(rowsOf(fit).coerceIn(1, free), fit, null, u.key + "a")
                    queue.addFirst(PoemUnit(rowsOf(rest), rest, null, u.key + "b"))
                    pages += current
                    current = mutableListOf()
                    free = lineCount
                }
            }
            else -> {
                current += u
                free = 0
            }
        }
    }
    if (current.isNotEmpty() || pages.isEmpty()) pages += current
    return pages
}

/**
 * تورق شعر: تک‌لمس = صفحهٔ بعد، دو لمس پیاپی = صفحهٔ قبل، با افکت جمع‌شدن ورق دور شیرازه.
 * متن بولد و ۳ سایز بزرگ‌تر از صفحهٔ تایپ است و هر سطر روی خط ورق می‌نشیند.
 */
@Composable
private fun PoetryViewer(
    poem: Poem,
    text: String,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val twoCol = twoHemistich(poem.type)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BookStage {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val geo = remember(maxWidth) { SkinGeometry(poetrySkin, maxWidth.value) }
                val gap = 14.dp
                val align = notebookTextAlignFromWire(poem.alignment)
                val style = remember(geo, density) {
                    skinTextStyle(geo.line, density, TextAlign.Right, POETRY_VIEW_BONUS_SP)
                }
                val fullPx = with(density) { geo.textWidth.roundToPx() }
                val cellPx = with(density) { ((geo.textWidth - gap) / 2).roundToPx() }
                val pages = remember(text, poem.type, geo, fullPx) {
                    val rowsFor = { t: String, cell: Boolean ->
                        maxOf(1, measureLineCount(measurer, t, style, if (cell) cellPx else fullPx))
                    }
                    paginatePoem(
                        units = buildPoemUnits(text, twoCol, rowsFor),
                        lineCount = poetrySkin.lineCount,
                        firstFree = poetrySkin.lineCount - 2,
                        split = { t, rows -> firstPageFit(t, measurer, style, fullPx, rows) },
                        rowsOf = { t -> rowsFor(t, false) },
                    )
                }
                val flip = rememberBookFlipState(initialPage = 0, pageCount = pages.size + 1)

                Box(Modifier.fillMaxSize()) {
                    BookOpening(visible = true, modifier = Modifier.fillMaxSize()) {
                        BookFlipper(
                            state = flip,
                            modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 10.dp),
                        ) { index ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (index == 0) {
                                    BookSkinCover(poetrySkin, Modifier.fillMaxWidth(), "دفتر شعر من")
                                } else {
                                    val page = index - 1
                                    BookSkinSpread(poetrySkin) { line, _ ->
                                        val pageStyle = skinTextStyle(line, density, TextAlign.Right, POETRY_VIEW_BONUS_SP)
                                        Column(Modifier.fillMaxSize()) {
                                            if (page == 0) {
                                                // عنوان در سطر اول (تراز معکوس)، یک سطر فاصله، بعد شعر.
                                                Text(
                                                    poem.title.ifBlank { poem.type },
                                                    Modifier.fillMaxWidth().height(line),
                                                    style = pageStyle.copy(textAlign = oppositeTextAlign(align)),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Spacer(Modifier.height(line))
                                            }
                                            pages.getOrElse(page) { emptyList() }.forEach { unit ->
                                                if (unit.second != null) {
                                                    Row(
                                                        Modifier.fillMaxWidth().height(line * unit.rows),
                                                        horizontalArrangement = Arrangement.spacedBy(gap),
                                                    ) {
                                                        Text(
                                                            unit.first,
                                                            Modifier.weight(1f),
                                                            style = pageStyle,
                                                            maxLines = unit.rows,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                        Text(
                                                            unit.second,
                                                            Modifier.weight(1f),
                                                            style = pageStyle,
                                                            maxLines = unit.rows,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        unit.first,
                                                        Modifier.fillMaxWidth().height(line * unit.rows),
                                                        style = pageStyle.copy(textAlign = align),
                                                        maxLines = unit.rows,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onClose) { Text("بستن", color = Color.White) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                poem.title.ifBlank { "شعر من" },
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(poem.type, color = Color(0xFFB8C6D8), style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (pages.size > 1 && flip.current > 0) flip.current.toString() + "/" + pages.size.toString() else "",
                                color = Color(0xFFB8C6D8),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, contentDescription = "ویرایش شعر", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
