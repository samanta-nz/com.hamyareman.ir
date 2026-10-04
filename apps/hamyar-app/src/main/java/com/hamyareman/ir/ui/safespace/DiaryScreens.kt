package com.hamyareman.ir.ui.safespace

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.components.NOTEBOOK_PAGE_SEPARATOR
import com.hamyareman.ir.ui.components.NotebookBookPage
import com.hamyareman.ir.ui.components.NotebookPaper
import com.hamyareman.ir.ui.components.NotebookTitlePicker
import com.hamyareman.ir.ui.study.SecureWebEffect
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val DIARY_STORE = "hamyar_private_diary"
private const val DIARY_ENTRIES = "entries"
private const val DIARY_COVER = "cover"
private const val DIARY_MEDIA_DIR = "diary-media"

private enum class ImageWrap(val wire: String, val title: String) {
    NONE("none", "بدون پیچش"),
    TOP("top", "عکس بالا / متن پایین"),
    BOTTOM("bottom", "عکس پایین / متن بالا"),
}

private data class DiaryPageModel(
    val text: String,
    val imagePath: String = "",
    val caption: String = "",
    val imageWidth: Float = 0.56f,
    val imageOffsetY: Float = 0f,
    val wrap: ImageWrap = ImageWrap.NONE,
)

private data class DiaryPayload(val pages: List<DiaryPageModel>)

private data class DiaryEntry(
    val id: String,
    val createdAt: Long,
    val title: String,
    val cipher: String,
)

private data class DiaryListPage(
    val entry: DiaryEntry,
    val index: Int,
    val page: DiaryPageModel,
    val pages: List<DiaryPageModel>,
)

private data class DiaryViewerPage(
    val entry: DiaryEntry,
    val pageIndex: Int,
    val page: DiaryPageModel,
)

private data class DiaryCover(val id: String, val title: String, val asset: String)

private val diaryCovers = listOf(
    DiaryCover("celestial", "آسمان خیال", "file:///android_asset/diary/cover-celestial.jpg"),
    DiaryCover("botanical", "باغ خیال", "file:///android_asset/diary/cover-botanical.jpg"),
    DiaryCover("geometric", "هندسهٔ رؤیا", "file:///android_asset/diary/cover-geometric.jpg"),
)

private fun readDiary(store: LocalStore): List<DiaryEntry> = runCatching {
    val array = JSONArray(store.getString(DIARY_ENTRIES, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(
                DiaryEntry(
                    id = o.getString("id"),
                    createdAt = o.getLong("createdAt"),
                    title = o.optString("title"),
                    cipher = o.getString("cipher"),
                ),
            )
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writeDiary(store: LocalStore, entries: List<DiaryEntry>) {
    val array = JSONArray()
    entries.forEach { e ->
        array.put(
            JSONObject()
                .put("id", e.id)
                .put("createdAt", e.createdAt)
                .put("title", e.title)
                .put("cipher", e.cipher),
        )
    }
    store.putString(DIARY_ENTRIES, array.toString())
}

private fun encodePayload(pages: List<DiaryPageModel>): String {
    val arr = JSONArray()
    pages.forEach { page ->
        arr.put(
            JSONObject()
                .put("text", page.text)
                .put("imagePath", page.imagePath)
                .put("caption", page.caption)
                .put("imageWidth", page.imageWidth)
                .put("imageOffsetY", page.imageOffsetY)
                .put("wrap", page.wrap.wire),
        )
    }
    return JSONObject().put("version", 3).put("pages", arr).toString()
}

private fun decodePayload(cipher: String, decrypt: (String) -> String): DiaryPayload {
    val plain = decrypt(cipher)
    if (plain.isBlank()) return DiaryPayload(listOf(DiaryPageModel("")))
    return runCatching {
        val arr = JSONObject(plain).getJSONArray("pages")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    DiaryPageModel(
                        text = o.optString("text"),
                        imagePath = o.optString("imagePath"),
                        caption = o.optString("caption"),
                        imageWidth = o.optDouble("imageWidth", 0.56).toFloat().coerceIn(0.25f, 0.82f),
                        imageOffsetY = o.optDouble("imageOffsetY", 0.0).toFloat().coerceIn(-0.25f, 0.25f),
                        wrap = ImageWrap.entries.firstOrNull { it.wire == o.optString("wrap") } ?: ImageWrap.NONE,
                    ),
                )
            }
        }.ifEmpty { listOf(DiaryPageModel("")) }.let { DiaryPayload(it) }
    }.getOrElse {
        DiaryPayload(
            plain.split(NOTEBOOK_PAGE_SEPARATOR).map { DiaryPageModel(it.trim()) }.ifEmpty { listOf(DiaryPageModel(plain.trim())) },
        )
    }
}

private fun secureDiaryMediaDir(context: Context): File =
    File(context.filesDir, DIARY_MEDIA_DIR).apply { mkdirs() }

private fun copyDiaryImage(context: Context, uri: Uri): String? = runCatching {
    val ext = context.contentResolver.getType(uri).orEmpty().substringAfterLast('/', "jpg").ifBlank { "jpg" }
    val target = File(
        secureDiaryMediaDir(context),
        "img_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(8) + "." + ext,
    )
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: return null
    target.absolutePath
}.getOrNull()

private fun deleteDiaryImage(path: String) {
    if (path.isNotBlank()) runCatching { File(path).delete() }
}

private fun titleOrDefault(value: String, default: String): String =
    value.trim().ifBlank { default }

@Composable
fun DiaryScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, DIARY_STORE) }
    var entries by remember { mutableStateOf(readDiary(store)) }
    var coverId by remember { mutableStateOf(store.getString(DIARY_COVER, diaryCovers.first().id)) }
    var editingEntryId by remember { mutableStateOf<String?>(null) }
    var editingPageIndex by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("خاطرات امروز") }
    var text by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf("") }
    var imageCaption by remember { mutableStateOf("") }
    var imageWidth by remember { mutableStateOf(0.56f) }
    var imageOffsetY by remember { mutableStateOf(0f) }
    var wrap by remember { mutableStateOf(ImageWrap.NONE) }
    var viewerStart by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val copied = copyDiaryImage(context, uri)
        if (copied != null) {
            deleteDiaryImage(imagePath)
            imagePath = copied
            imageCaption = imageCaption.ifBlank { "عکس دفتر خاطرات" }
            notice = "عکس داخل فضای خصوصی دفتر قرار گرفت."
        } else {
            notice = "عکس قابل ذخیره‌سازی نبود."
        }
    }

    fun openNew() {
        editingEntryId = null
        editingPageIndex = 0
        title = "خاطرات امروز"
        text = ""
        imagePath = ""
        imageCaption = ""
        imageWidth = 0.56f
        imageOffsetY = 0f
        wrap = ImageWrap.NONE
        notice = null
    }

    fun openExisting(entry: DiaryEntry, pageIndex: Int) {
        val pages = decodePayload(entry.cipher) { container.encryptor.decrypt(it).orEmpty() }.pages
        val page = pages.getOrElse(pageIndex) { DiaryPageModel("") }
        editingEntryId = entry.id
        editingPageIndex = pageIndex
        title = titleOrDefault(entry.title, "خاطرات")
        text = page.text
        imagePath = page.imagePath
        imageCaption = page.caption
        imageWidth = page.imageWidth
        imageOffsetY = page.imageOffsetY
        wrap = page.wrap
        notice = "صفحهٔ " + (pageIndex + 1) + " برای ویرایش باز شد."
    }

    fun savePage() {
        if (text.isBlank() && imagePath.isBlank()) {
            notice = "صفحه خالی است."
            return
        }
        val currentEntry = editingEntryId?.let { id -> entries.firstOrNull { it.id == id } }
        val pageTexts = text.split(NOTEBOOK_PAGE_SEPARATOR).map { it.trim() }.ifEmpty { listOf("") }
        val heading = titleOrDefault(title, "خاطرات امروز")
        val pageBase = DiaryPageModel(
            text = pageTexts.firstOrNull().orEmpty(),
            imagePath = imagePath,
            caption = imageCaption.trim(),
            imageWidth = imageWidth,
            imageOffsetY = imageOffsetY,
            wrap = wrap,
        )

        if (currentEntry == null) {
            val pages = pageTexts.mapIndexed { index, value ->
                if (index == 0) pageBase else DiaryPageModel(value)
            }
            val entry = DiaryEntry(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                title = heading,
                cipher = container.encryptor.encrypt(encodePayload(pages)),
            )
            entries = listOf(entry) + entries
        } else {
            val pages = decodePayload(currentEntry.cipher) { container.encryptor.decrypt(it).orEmpty() }.pages.toMutableList()
            while (pages.size <= editingPageIndex) pages += DiaryPageModel("")
            pages[editingPageIndex] = pageBase
            val changed = currentEntry.copy(
                title = heading,
                cipher = container.encryptor.encrypt(encodePayload(pages)),
            )
            entries = entries.map { if (it.id == changed.id) changed else it }
        }
        writeDiary(store, entries)
        text = ""
        imagePath = ""
        imageCaption = ""
        editingEntryId = null
        editingPageIndex = 0
        notice = "صفحه ذخیره شد و فهرست دفتر آماده است."
    }

    fun deletePage(item: DiaryListPage) {
        val pages = item.pages.toMutableList()
        val removed = pages.removeAt(item.index)
        deleteDiaryImage(removed.imagePath)
        entries = if (pages.isEmpty()) {
            entries.filterNot { it.id == item.entry.id }
        } else {
            val changed = item.entry.copy(cipher = container.encryptor.encrypt(encodePayload(pages)))
            entries.map { if (it.id == changed.id) changed else it }
        }
        writeDiary(store, entries)
        notice = "صفحه حذف شد."
    }

    val listPages = remember(entries) {
        entries.flatMap { entry ->
            val pages = decodePayload(entry.cipher) { container.encryptor.decrypt(it).orEmpty() }.pages
            pages.mapIndexed { index, page -> DiaryListPage(entry, index, page, pages) }
        }
    }
    val viewerPages = remember(entries) {
        entries.flatMap { entry ->
            val pages = decodePayload(entry.cipher) { container.encryptor.decrypt(it).orEmpty() }.pages
            pages.mapIndexed { pageIndex, page -> DiaryViewerPage(entry, pageIndex, page) }
        }
    }

    SecureWebEffect("Screenshots are disabled in the private diary.")

    Column(Modifier.fillMaxSize()) {
        AppTopBar("دفتر خاطرات", onBack)
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("کتاب خاطرات", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            diaryCovers.forEach { cover ->
                                Card(Modifier.weight(1f).clickable {
                                    coverId = cover.id
                                    store.putString(DIARY_COVER, cover.id)
                                }) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AsyncImage(
                                            model = cover.asset,
                                            contentDescription = cover.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth().aspectRatio(0.72f),
                                        )
                                        Text(
                                            if (cover.id == coverId) "✓ " + cover.title else cover.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::openNew, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                Text(" صفحهٔ تازه")
                            }
                            OutlinedButton(
                                onClick = { viewerStart = 0 },
                                enabled = viewerPages.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null)
                                Text(" تورق کتاب")
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (editingEntryId == null) "نوشتن صفحه" else "ویرایش صفحهٔ " + (editingPageIndex + 1),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        NotebookTitlePicker(
                            value = title,
                            defaultTitle = "خاطرات امروز",
                            suggestions = listOf("سفر", "خاطرهٔ مدرسه", "احساسات", "روز خاص"),
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LinedNotebookInput(text) { text = it }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { imagePicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                Text("الصاق عکس")
                            }
                            if (editingEntryId != null) {
                                TextButton(onClick = ::openNew, modifier = Modifier.weight(1f)) { Text("لغو") }
                            }
                        }
                        if (imagePath.isNotBlank()) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AsyncImage(
                                        model = File(imagePath),
                                        contentDescription = "پیش‌نمایش عکس",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)),
                                    )
                                    OutlinedTextField(
                                        value = imageCaption,
                                        onValueChange = { imageCaption = it.take(140) },
                                        label = { Text("عنوان عکس / پانویس") },
                                        singleLine = true,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text("اندازهٔ عکس", style = MaterialTheme.typography.labelMedium)
                                    Slider(value = imageWidth, onValueChange = { imageWidth = it }, valueRange = 0.30f..0.78f)
                                    Text("جایگاه عمودی", style = MaterialTheme.typography.labelMedium)
                                    Slider(value = imageOffsetY, onValueChange = { imageOffsetY = it }, valueRange = -0.20f..0.20f)
                                    Text("پیچش متن", style = MaterialTheme.typography.labelMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ImageWrap.entries.forEach { option ->
                                            FilterChip(
                                                selected = wrap == option,
                                                onClick = { wrap = option },
                                                label = { Text(option.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            )
                                        }
                                    }
                                    TextButton(onClick = {
                                        deleteDiaryImage(imagePath)
                                        imagePath = ""
                                        imageCaption = ""
                                    }) { Text("برداشتن عکس") }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::savePage, modifier = Modifier.weight(2f)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null)
                                Text(" ذخیره و برگشت به کتاب")
                            }
                            notice?.let {
                                Text(
                                    it,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            item { Text("صفحه‌های ذخیره‌شده", style = MaterialTheme.typography.titleMedium) }
            if (listPages.isEmpty()) {
                item { Text("هنوز صفحه‌ای در دفتر خاطرات ذخیره نشده.") }
            }
            items(listPages, key = { it.entry.id + "-" + it.index }) { item ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        val idx = viewerPages.indexOfFirst {
                            it.entry.id == item.entry.id && it.pageIndex == item.index
                        }
                        viewerStart = idx.takeIf { it >= 0 }
                    },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(
                                item.entry.title.ifBlank { "خاطرات" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Text(
                            JalaliDate.stampFa(item.entry.createdAt),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            (item.index + 1).toString() + "/" + item.pages.size,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { openExisting(item.entry, item.index) }) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش صفحه")
                        }
                        IconButton(onClick = { deletePage(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف صفحه")
                        }
                    }
                }
            }
        }
    }

    viewerStart?.let { start ->
        DiaryBookViewer(
            cover = diaryCovers.firstOrNull { it.id == coverId } ?: diaryCovers.first(),
            pages = viewerPages,
            startPage = start,
            onEdit = { page ->
                openExisting(page.entry, page.pageIndex)
                viewerStart = null
            },
            onClose = { viewerStart = null },
        )
    }
}

@Composable
private fun DiaryBookViewer(
    cover: DiaryCover,
    pages: List<DiaryViewerPage>,
    startPage: Int,
    onEdit: (DiaryViewerPage) -> Unit,
    onClose: () -> Unit,
) {
    val pager = rememberPagerState(pageCount = { pages.size + 1 })
    LaunchedEffect(startPage) {
        pager.scrollToPage((startPage + 1).coerceIn(0, pages.size))
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF070C16))) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 28.dp),
                reverseLayout = true,
                beyondViewportPageCount = 1,
            ) { index ->
                if (index == 0) {
                    NotebookBookPage(
                        pageNumber = 1,
                        pageCount = pages.size + 1,
                        stackPages = pages.size.coerceIn(0, 7),
                    ) {
                        AsyncImage(
                            model = cover.asset,
                            contentDescription = cover.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp)),
                        )
                    }
                } else {
                    val page = pages[index - 1]
                    NotebookBookPage(
                        pageNumber = index + 1,
                        pageCount = pages.size + 1,
                        stackPages = (pages.size - index).coerceIn(0, 7),
                    ) {
                        DiaryRenderedPage(page.page)
                    }
                }
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) { Text("بستن", color = Color.White) }
                Text("کتاب خاطرات", color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = {
                        val index = pager.currentPage - 1
                        pages.getOrNull(index)?.let(onEdit)
                    },
                    enabled = pager.currentPage > 0,
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش این صفحه", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DiaryRenderedPage(page: DiaryPageModel) {
    NotebookPaper {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val area = Modifier.fillMaxSize().padding(start = 78.dp, end = 78.dp, top = 150.dp, bottom = 28.dp)
            when {
                page.imagePath.isBlank() -> {
                    Text(
                        page.text,
                        modifier = area,
                        color = Color(0xFF19364B),
                        fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Right,
                    )
                }
                page.wrap == ImageWrap.TOP -> {
                    Column(area, horizontalAlignment = Alignment.CenterHorizontally) {
                        DiaryImageBlock(page)
                        Text(
                            page.text,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = Color(0xFF19364B),
                            fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Right,
                        )
                    }
                }
                page.wrap == ImageWrap.BOTTOM -> {
                    Column(area, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            page.text,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF19364B),
                            fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Right,
                        )
                        DiaryImageBlock(page, paddingTop = 8.dp)
                    }
                }
                else -> {
                    Box(area) {
                        Text(
                            page.text,
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF19364B),
                            fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Right,
                        )
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(page.imageWidth)
                                .padding(top = (page.imageOffsetY * 520f).dp),
                        ) {
                            DiaryImageBlock(page)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryImageBlock(
    page: DiaryPageModel,
    paddingTop: androidx.compose.ui.unit.Dp = 0.dp,
) {
    Column(
        Modifier.padding(top = paddingTop),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = File(page.imagePath),
            contentDescription = page.caption.ifBlank { "عکس" },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        )
        if (page.caption.isNotBlank()) {
            Text(
                page.caption,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = Color(0xFF526B80),
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
            )
        }
    }
}


@Composable
fun NotebooksScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, DIARY_STORE) }
    var notebooks by remember { mutableStateOf(readNotebooks(store)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val selected = notebooks.firstOrNull { it.id == selectedId }

    fun rowId(id: String): String =
        "notebook_" + container.auth.cachedUserId().orEmpty().take(32) + "_" + id.take(32)

    fun queue(notebook: Notebook) {
        val uid = container.auth.cachedUserId().orEmpty()
        if (uid.isNotBlank()) {
            container.sync.enqueue(
                TableIds.APP_STATE,
                rowId(notebook.id),
                mapOf(
                    "userId" to uid,
                    "key" to "private_notebook",
                    "notebookId" to notebook.id,
                    "title" to notebook.title,
                    "createdAt" to notebook.createdAt,
                    "cipher" to notebook.cipher,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            if (selected == null) "دفترچه‌های یادداشت" else selected.title,
            if (selected == null) onBack else ({ selectedId = null; text = ""; notice = null }),
        )
        LazyColumn(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selected == null) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("دفترچهٔ تازه", style = MaterialTheme.typography.titleMedium)
                            NotebookTitlePicker(
                                value = title,
                                defaultTitle = "یادداشت‌های روزانه",
                                suggestions = listOf("درس", "ایده‌ها", "برنامه‌ریزی", "کارهای مهم"),
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    val finalTitle = title.trim().ifBlank { "یادداشت‌های روزانه" }
                                    val item = Notebook(
                                        id = UUID.randomUUID().toString(),
                                        title = finalTitle,
                                        createdAt = System.currentTimeMillis(),
                                        cipher = container.encryptor.encrypt(""),
                                    )
                                    notebooks = listOf(item) + notebooks
                                    writeNotebooks(store, notebooks)
                                    queue(item)
                                    selectedId = item.id
                                    title = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("ساخت دفترچه") }
                        }
                    }
                }
                if (notebooks.isEmpty()) {
                    item { Text("هنوز دفترچه‌ای نساخته‌ای.") }
                }
                items(notebooks, key = { it.id }) { notebookItem ->
                    val body = remember(notebookItem.cipher) {
                        container.encryptor.decrypt(notebookItem.cipher).orEmpty()
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.weight(1f).clickable { selectedId = notebookItem.id },
                            ) {
                                Text(
                                    notebookItem.title.ifBlank { "یادداشت‌های روزانه" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 15.sp,
                                )
                            }
                            Text(
                                JalaliDate.stampFa(notebookItem.createdAt),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                paginate(body).size.toString(),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp),
                            )
                            IconButton(onClick = { selectedId = notebookItem.id }) {
                                Icon(Icons.Default.Edit, contentDescription = "ویرایش دفترچه")
                            }
                            IconButton(onClick = {
                                notebooks = notebooks.filterNot { it.id == notebookItem.id }
                                writeNotebooks(store, notebooks)
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    runCatching {
                                        container.tables.delete(TableIds.APP_STATE, rowId(notebookItem.id))
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف دفترچه")
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "نوشتن در " + selected.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "شش سطر بالای کاغذ خالی می‌ماند؛ وقتی آخرین سطر پر شود، متن خودکار به برگ بعد می‌رود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LinedNotebookInput(text) { text = it }
                            Button(
                                onClick = {
                                    if (text.isBlank()) {
                                        notice = "اول چیزی بنویس."
                                    } else {
                                        val old = container.encryptor.decrypt(selected.cipher).orEmpty()
                                        val joined = listOf(old, text.trim()).filter { it.isNotBlank() }.joinToString("\n\n")
                                        val changed = selected.copy(
                                            cipher = container.encryptor.encrypt(joined),
                                        )
                                        notebooks = notebooks.map { if (it.id == changed.id) changed else it }
                                        writeNotebooks(store, notebooks)
                                        queue(changed)
                                        text = ""
                                        selectedId = null
                                        notice = "ذخیره شد و به فهرست دفترچه‌ها برگشتی."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null)
                                Text(" ذخیره و برگشت")
                            }
                            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
                val fullText = container.encryptor.decrypt(selected.cipher).orEmpty()
                val pages = fullText.split(NOTEBOOK_PAGE_SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
                pages.forEachIndexed { index, page ->
                    item(key = "notebook-page-" + selected.id + "-" + index) {
                        NotebookBookPage(
                            pageNumber = index + 1,
                            pageCount = pages.size.coerceAtLeast(1),
                            stackPages = (pages.size - index - 1).coerceIn(0, 7),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            NotebookPaper {
                                Text(
                                    page,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 74.dp, end = 74.dp, top = 150.dp, bottom = 28.dp),
                                    fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    textAlign = TextAlign.Right,
                                    color = Color(0xFF19364B),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
