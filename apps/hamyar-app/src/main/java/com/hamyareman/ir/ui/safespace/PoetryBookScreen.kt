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

private val poemTypes = listOf(
    "غزل", "قصیده", "دوبیتی", "رباعی", "مثنوی", "شعر نو", "سپید", "نثر شاعرانه", "ترانه", "سایر",
)

private fun twoHemistich(type: String): Boolean =
    type in setOf("غزل", "قصیده", "دوبیتی", "رباعی", "قطعه", "مثنوی")

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
fun PoetryBookScreen(onBack: () -> Unit) {
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

    fun reset() {
        editingId = null
        title = poemTypes.first()
        type = poemTypes.first()
        text = ""
        alignment = androidx.compose.ui.text.style.TextAlign.Right
        notice = null
    }

    fun edit(poem: Poem) {
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
        poems = (listOf(changed) + poems.filterNot { it.id == changed.id }).sortedByDescending { it.createdAt }
        writePoems(container, poems)
        reset()
        notice = "شعر ذخیره شد."
    }

    RealisticDeskFrame(
        backgroundKey = DesignAsset.POETRY_BG,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            com.hamyareman.ir.platform.core.designsystem.AppTopBar("دفتر شعر", onBack, onHelp = { })
        LazyColumn(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("دفتر شعر", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "همان موتور کاغذ و تورق دفترچه‌ها، با چینش ویژهٔ شعرهای دو مصراعی.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                            label = { Text("عنوان شعر") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        NotebookAlignmentPicker(alignment, { alignment = it })
                        if (twoHemistich(type)) {
                            NotebookPaper(
                                header = if (title.isBlank()) type else title,
                                headerAlign = oppositeTextAlign(alignment),
                                showVerticalGuides = false,
                                headerOnFirstLine = true,
                                realistic = true,
                            ) {
                                PoetryHemistichEditor(
                                    value = text,
                                    onValueChange = { text = it },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        } else {
                            LinedNotebookInput(
                                text,
                                { text = it },
                                header = if (title.isBlank()) type else title,
                                textAlign = alignment,
                                showVerticalGuides = false,
                                headerOnFirstLine = true,
                                realistic = true,
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

@Composable
private fun PoetryHemistichEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val rowsPerPage = ((maxHeight.value - 18f) / 42f).toInt().coerceIn(6, 15)
        val rows = remember(value) {
            val lines = value.replace("\r", "").split('\n')
            val paired = lines.chunked(2).map {
                PoetryHemistichRow(
                    right = it.getOrNull(0).orEmpty(),
                    left = it.getOrNull(1).orEmpty(),
                )
            }
            if (paired.isEmpty()) listOf(PoetryHemistichRow()) else paired
        }
        val pages = rows.chunked(rowsPerPage).ifEmpty { listOf(listOf(PoetryHemistichRow())) }
        val pager = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            Column(
                Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pages[pageIndex].forEachIndexed { rowIndex, row ->
                    val absoluteRow = pageIndex * rowsPerPage + rowIndex
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        PoetryHemistichCell(
                            value = row.right,
                            label = "مصراع اول، ردیف ${absoluteRow + 1}",
                            modifier = Modifier.weight(1f),
                            onValueChange = { changed ->
                                val next = rows.toMutableList()
                                while (next.size <= absoluteRow) next += PoetryHemistichRow()
                                next[absoluteRow] = next[absoluteRow].copy(right = changed)
                                onValueChange(
                                    next.joinToString("\n") { r -> r.right + "\n" + r.left }
                                        .trimEnd('\n'),
                                )
                            },
                        )
                        PoetryHemistichCell(
                            value = row.left,
                            label = "مصراع دوم، ردیف ${absoluteRow + 1}",
                            modifier = Modifier.weight(1f),
                            onValueChange = { changed ->
                                val next = rows.toMutableList()
                                while (next.size <= absoluteRow) next += PoetryHemistichRow()
                                next[absoluteRow] = next[absoluteRow].copy(left = changed)
                                onValueChange(
                                    next.joinToString("\n") { r -> r.right + "\n" + r.left }
                                        .trimEnd('\n'),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PoetryHemistichCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    var focused by remember(label) { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(180)) },
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color(0xFF18384F),
            textAlign = TextAlign.Right,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                else Color.Transparent,
            )
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        decorationBox = { innerTextField ->
            Box(Modifier.fillMaxWidth()) { innerTextField() }
        },
    )
}

/** یک «واحد» روی ورق: یک بیت دو مصراعی (یک سطر) یا یک سطر آزاد (یک یا چند سطر خط‌دار). */
private data class PoemUnit(val rows: Int, val first: String, val second: String?, val key: String)

@Composable
private fun PoetryTypePicker(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("نوع شعر: "+value, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            poemTypes.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    },
                )
            }
        }
    }
}

private fun buildPoemUnits(text: String, twoCol: Boolean): List<PoemUnit> {
    val flat = text.replace("\r", "")
        .split(NOTEBOOK_PAGE_SEPARATOR)
        .flatMap { it.split('\n') }
        .map { it.trimEnd() }
    if (twoCol) {
        return flat.filter { it.isNotBlank() }.chunked(2).mapIndexed { i, pair ->
            PoemUnit(1, pair.getOrNull(0).orEmpty(), pair.getOrNull(1).orEmpty(), "pair-$i")
        }
    }
    return flat.mapIndexed { i, line ->
        val rows = ((line.length + 35) / 36).coerceAtLeast(1)
        PoemUnit(rows, line, null, "line-$i")
    }
}

/** چیدن واحدها روی صفحه‌ها بر اساس ظرفیت خط‌های ورق؛ صفحهٔ اول دو سطر برای عنوان و فاصله کم می‌کند. */
private fun paginatePoem(units: List<PoemUnit>, lineCount: Int): List<List<PoemUnit>> {
    val pages = mutableListOf<MutableList<PoemUnit>>()
    var current = mutableListOf<PoemUnit>()
    var free = lineCount - 2
    for (u in units) {
        if (u.rows > free && current.isNotEmpty()) {
            pages += current
            current = mutableListOf()
            free = lineCount
        }
        current += u
        free -= u.rows
    }
    pages += current
    return pages
}

@Composable
private fun PoetryViewer(
    poem: Poem,
    text: String,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val twoCol = twoHemistich(poem.type)
    val pages = remember(text, poem.type) {
        paginatePoem(buildPoemUnits(text, twoCol), poetrySkin.lineCount)
    }
    var activePage by remember(poem.id, pages.size) { mutableStateOf(0) }
    var selectedPair by remember(poem.id) { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BookStage {
            Box(Modifier.fillMaxSize()) {
                BookOpening(visible = true, modifier = Modifier.fillMaxSize()) {
                    RealisticBookPager(
                        pageCount = pages.size + 1,
                        initialPage = 0,
                        viewerGesture = true,
                        skinned = true,
                        onPageChanged = { activePage = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 20.dp, bottom = 10.dp)
                            .pointerInput(pages.size) {
                                detectTapGestures(
                                    onTap = {
                                        val next = (activePage + 1).coerceAtMost(pages.size)
                                        if (next != activePage) activePage = next
                                    },
                                    onDoubleTap = {
                                        val previous = (activePage - 1).coerceAtLeast(0)
                                        if (previous != activePage) activePage = previous
                                    },
                                )
                            },
                    ) { index, _ ->
                        if (index == 0) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                BookSkinCover(poetrySkin, Modifier.fillMaxWidth(), "دفتر شعر من")
                            }
                        } else {
                            val page = index - 1
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                BookSkinSpread(poetrySkin) { line, _ ->
                                    val lh = with(LocalDensity.current) { line.toSp() }
                                    val style = TextStyle(
                                        fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                        fontSize = lh * 0.87f,
                                        lineHeight = lh,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = Color(0xFF18384F),
                                        textAlign = TextAlign.Right,
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                                        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                                            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Bottom,
                                            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
                                        ),
                                    )
                                    Column(Modifier.fillMaxSize()) {
                                        if (page == 0) {
                                            // عنوان در سطر اول (تراز معکوس)، یک سطر فاصله، بعد شعر.
                                            Text(
                                                poem.title.ifBlank { poem.type },
                                                Modifier.fillMaxWidth(),
                                                style = style.copy(
                                                    textAlign = oppositeTextAlign(notebookTextAlignFromWire(poem.alignment)),
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Spacer(Modifier.height(line))
                                        }
                                        pages.getOrElse(page) { emptyList() }.forEach { unit ->
                                            if (unit.second != null) {
                                                Row(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(line)
                                                        .background(
                                                            if (selectedPair == unit.key) Color(0x183B82F6)
                                                            else Color.Transparent,
                                                        )
                                                        .clickable {
                                                            selectedPair = if (selectedPair == unit.key) null else unit.key
                                                        },
                                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                ) {
                                                    Text(unit.first, Modifier.weight(1f), style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(unit.second, Modifier.weight(1f), style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            } else {
                                                Text(
                                                    unit.first,
                                                    Modifier.fillMaxWidth().height(line * unit.rows),
                                                    style = style.copy(textAlign = notebookTextAlignFromWire(poem.alignment)),
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
                            if (pages.size > 1 && activePage > 0) activePage.toString() + "/" + pages.size.toString() else "",
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
