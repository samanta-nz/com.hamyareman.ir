package com.hamyareman.ir.ui.safespace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.hamyareman.ir.ui.components.NOTEBOOK_PAGE_SEPARATOR
import com.hamyareman.ir.ui.components.NotebookBookPage
import com.hamyareman.ir.ui.components.NotebookPaper
import com.hamyareman.ir.ui.components.NotebookTitlePicker
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

private data class Poem(
    val id: String,
    val createdAt: Long,
    val title: String,
    val type: String,
    val cipher: String,
    val alignment: String = "right",
)

private val poemTypes = listOf(
    "غزل", "قصیده", "دوبیتی", "رباعی", "قطعه", "مثنوی", "سپید", "نثر شاعرانه",
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
            com.hamyareman.ir.platform.core.designsystem.AppTopBar("دفتر شعر", onBack)
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
                        Text("نوع شعر", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            poemTypes.forEach { option ->
                                FilterChip(
                                    selected = type == option,
                                    onClick = { type = option; if (editingId == null && (title.isBlank() || title in poemTypes || title == "شعر من")) title = option },
                                    label = { Text(option) },
                                )
                            }
                        }
                        NotebookTitlePicker(
                            value = title,
                            defaultTitle = "شعر من",
                            suggestions = listOf("شعر برای امروز", "دل‌نوشتهٔ شاعرانه", "شعر کوتاه"),
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        NotebookAlignmentPicker(alignment, { alignment = it })
                        LinedNotebookInput(
                            text,
                            { text = it },
                            header = if (title.isBlank()) type else title,
                            textAlign = alignment,
                            showVerticalGuides = false,
                            headerOnFirstLine = true,
                            realistic = true,
                        )
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

    viewer?.let { poem ->
        PoetryViewer(
            poem = poem,
            text = container.encryptor.decrypt(poem.cipher).orEmpty(),
            onEdit = { edit(poem); viewer = null },
            onClose = { viewer = null },
        )
    }
}

@Composable
private fun PoetryViewer(
    poem: Poem,
    text: String,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val lines = text.split(NOTEBOOK_PAGE_SEPARATOR).ifEmpty { listOf("") }
    var activePage by remember(poem.id, lines.size) { mutableStateOf(0) }
    var selectedPair by remember(poem.id) { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BookStage {
            Box(Modifier.fillMaxSize()) {
                BookOpening(visible = true, modifier = Modifier.fillMaxSize()) {
                    RealisticBookPager(
                        pageCount = lines.size.coerceAtLeast(1),
                        initialPage = 0,
                        viewerGesture = true,
                        onPageChanged = { activePage = it },
                        modifier = Modifier.fillMaxSize().padding(top = 20.dp, bottom = 10.dp),
                    ) { page, _ ->
                        NotebookPaper(
                            header = if (page == 0) poem.title.ifBlank { poem.type } else "",
                            headerAlign = oppositeTextAlign(notebookTextAlignFromWire(poem.alignment)),
                            showVerticalGuides = false,
                        ) {
                            if (twoHemistich(poem.type)) {
                                val all = lines.getOrElse(page) { "" }
                                    .split('\n')
                                    .filter { it.isNotBlank() }
                                val pairs = all.chunked(2)
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(start = 62.dp, end = 62.dp, top = 4.dp, bottom = 30.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    pairs.forEachIndexed { idx, pair ->
                                        val pairKey = page.toString() + ":" + idx
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(
                                                    if (selectedPair == pairKey) Color(0x183B82F6)
                                                    else Color.Transparent,
                                                )
                                                .clickable {
                                                    selectedPair = if (selectedPair == pairKey) null else pairKey
                                                }
                                                .padding(vertical = 5.dp),
                                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                                        ) {
                                            Text(
                                                pair.getOrNull(0).orEmpty(),
                                                Modifier.weight(1f),
                                                textAlign = TextAlign.Right,
                                                color = Color(0xFF18384F),
                                                fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                                fontSize = 18.sp,
                                                lineHeight = 24.sp,
                                            )
                                            Text(
                                                pair.getOrNull(1).orEmpty(),
                                                Modifier.weight(1f),
                                                textAlign = TextAlign.Right,
                                                color = Color(0xFF18384F),
                                                fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                                fontSize = 18.sp,
                                                lineHeight = 24.sp,
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    lines.getOrElse(page) { "" },
                                    Modifier
                                        .fillMaxSize()
                                        .padding(start = 62.dp, end = 62.dp, top = 4.dp, bottom = 30.dp),
                                    textAlign = notebookTextAlignFromWire(poem.alignment),
                                    color = Color(0xFF18384F),
                                    fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                )
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
                            if (lines.size > 1) (activePage + 1).toString() + "/" + lines.size.toString() else "",
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
