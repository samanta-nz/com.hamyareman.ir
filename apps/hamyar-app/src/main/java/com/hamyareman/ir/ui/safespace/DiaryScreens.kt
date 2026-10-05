package com.hamyareman.ir.ui.safespace

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import com.hamyareman.ir.ui.components.RemoteDesignImage
import com.hamyareman.ir.ui.components.RealisticDeskFrame
import com.hamyareman.ir.ui.components.RealisticBookPager
import com.hamyareman.ir.ui.components.DesignAsset
import com.hamyareman.ir.ui.components.BookStage
import com.hamyareman.ir.ui.components.BookOpening
import com.hamyareman.ir.ui.components.BookSkin
import com.hamyareman.ir.ui.components.BookSkinCover
import com.hamyareman.ir.ui.components.BookSkinSpread
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.components.NOTEBOOK_PAGE_SEPARATOR
import com.hamyareman.ir.ui.components.NotebookBookPage
import com.hamyareman.ir.ui.components.NotebookPaper
import com.hamyareman.ir.ui.components.NotebookTitlePicker
import com.hamyareman.ir.ui.components.oppositeTextAlign
import com.hamyareman.ir.ui.components.NotebookAlignmentPicker
import com.hamyareman.ir.ui.components.nextRegisteredTitle
import com.hamyareman.ir.ui.components.notebookAlignmentWire
import com.hamyareman.ir.ui.components.notebookTextAlignFromWire
import com.hamyareman.ir.ui.study.SecureWebEffect
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val DIARY_STORE = "hamyar_private_diary"
private const val NOTEBOOKS = "notebooks"

private data class Notebook(
    val id: String,
    val title: String,
    val createdAt: Long,
    val cipher: String,
    val alignment: String = "right",
)

private fun readNotebooks(store: LocalStore): List<Notebook> = runCatching {
    val array = JSONArray(store.getString(NOTEBOOKS, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(Notebook(o.getString("id"), o.getString("title"), o.getLong("createdAt"), o.getString("cipher"), o.optString("alignment", "right")))
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writeNotebooks(store: LocalStore, notebooks: List<Notebook>) {
    val array = JSONArray()
    notebooks.forEach { n ->
        array.put(JSONObject().put("id", n.id).put("title", n.title).put("createdAt", n.createdAt).put("cipher", n.cipher).put("alignment", n.alignment))
    }
    store.putString(NOTEBOOKS, array.toString())
}
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
    val imageOffsetX: Float = 0f,
    val imageScale: Float = 1f,
    val imageRotation: Float = 0f,
    val wrap: ImageWrap = ImageWrap.NONE,
    val alignment: String = "right",
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

/** جلد، کتاب باز و ورق دفتر خاطرات: PNG دوربری‌شدهٔ سرمه‌ای گل‌دوزی (بوم مشترک ۱۰۵۹×۱۴۸۶). */
private val diarySkin = BookSkin.NavyFloral

private val diaryCovers = listOf(
    DiaryCover("navy", "خاطرات من", DesignAsset.COVER_NAVY_FLORAL),
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
                .put("imageOffsetX", page.imageOffsetX)
                .put("imageScale", page.imageScale)
                .put("imageRotation", page.imageRotation)
                .put("wrap", page.wrap.wire)
                .put("alignment", page.alignment),
        )
    }
    return JSONObject().put("version", 4).put("pages", arr).toString()
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
                        imageWidth = o.optDouble("imageWidth", 0.56).toFloat().coerceIn(0.25f, 0.78f),
                        imageOffsetY = o.optDouble("imageOffsetY", 0.0).toFloat().coerceIn(-0.30f, 0.30f),
                        imageOffsetX = o.optDouble("imageOffsetX", 0.0).toFloat().coerceIn(-0.35f, 0.35f),
                        imageScale = o.optDouble("imageScale", 1.0).toFloat().coerceIn(0.45f, 1.60f),
                        imageRotation = o.optDouble("imageRotation", 0.0).toFloat().coerceIn(-10f, 10f),
                        wrap = ImageWrap.entries.firstOrNull { it.wire == o.optString("wrap") } ?: ImageWrap.NONE,
                        alignment = o.optString("alignment", "right"),
                    ),
                )
            }
        }.ifEmpty { listOf(DiaryPageModel("")) }.let { DiaryPayload(it) }
    }.getOrElse {
        DiaryPayload(
            plain.split(NOTEBOOK_PAGE_SEPARATOR).map { DiaryPageModel(it) }.ifEmpty { listOf(DiaryPageModel(plain)) },
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
private fun DiaryImageWrapPicker(
    value: ImageWrap,
    onValueChange: (ImageWrap) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("جای عکس: " + value.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ImageWrap.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    },
                )
            }
        }
    }
}

@Composable
fun DiaryScreen(onBack: () -> Unit, onHelp: () -> Unit = {}) {
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
    var imageOffsetX by remember { mutableStateOf(0f) }
    var imageScale by remember { mutableStateOf(1f) }
    var imageRotation by remember { mutableStateOf(0f) }
    var placementMode by remember { mutableStateOf(false) }
    var wrap by remember { mutableStateOf(ImageWrap.NONE) }
    var alignment by remember { mutableStateOf(androidx.compose.ui.text.style.TextAlign.Right) }
    var viewerStart by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val copied = copyDiaryImage(context, uri)
        if (copied != null) {
            deleteDiaryImage(imagePath)
            imagePath = copied
            imageCaption = imageCaption.ifBlank { "عکس دفتر خاطرات" }
            imageWidth = 0.56f
            imageOffsetY = 0f
            imageOffsetX = 0f
            imageScale = 1f
            imageRotation = 0f
            placementMode = true
            notice = "تصویر انتخاب شد؛ با یک انگشت جابه‌جا و با دو انگشت بزرگ‌نمایی/چرخش کن."
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
        imageOffsetX = 0f
        imageScale = 1f
        imageRotation = 0f
        placementMode = false
        wrap = ImageWrap.NONE
        alignment = androidx.compose.ui.text.style.TextAlign.Right
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
        imageOffsetX = page.imageOffsetX
        imageScale = page.imageScale
        imageRotation = page.imageRotation
        placementMode = page.imagePath.isNotBlank()
        wrap = page.wrap
        alignment = notebookTextAlignFromWire(page.alignment)
        notice = "صفحهٔ " + (pageIndex + 1) + " برای ویرایش باز شد."
    }

    fun savePage() {
        if (text.isBlank() && imagePath.isBlank()) {
            notice = "صفحه خالی است."
            return
        }
        val currentEntry = editingEntryId?.let { id -> entries.firstOrNull { it.id == id } }
        val pageTexts = text.split(NOTEBOOK_PAGE_SEPARATOR).ifEmpty { listOf("") }
        val heading = if (currentEntry == null) nextRegisteredTitle(titleOrDefault(title, "خاطرات امروز"), entries.map { it.title }) else titleOrDefault(title, "خاطرات امروز")
        val pageBase = DiaryPageModel(
            text = pageTexts.firstOrNull().orEmpty(),
            imagePath = imagePath,
            caption = imageCaption.trim(),
            imageWidth = imageWidth,
            imageOffsetY = imageOffsetY,
            imageOffsetX = imageOffsetX,
            imageScale = imageScale,
            imageRotation = imageRotation,
            wrap = wrap,
            alignment = notebookAlignmentWire(alignment),
        )

        if (currentEntry == null) {
            val pages = pageTexts.mapIndexed { index, value ->
                if (index == 0) pageBase else DiaryPageModel(value, alignment = notebookAlignmentWire(alignment))
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
            val replacement = pageTexts.mapIndexed { index, value ->
                if (index == 0) pageBase else DiaryPageModel(value, alignment = notebookAlignmentWire(alignment))
            }
            pages.removeAt(editingPageIndex)
            pages.addAll(editingPageIndex, replacement)
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
        placementMode = false
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

    RealisticDeskFrame(
        backgroundKey = DesignAsset.DIARY_DESK,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("دفتر خاطرات", onBack, onHelp = onHelp)
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("کتاب خاطرات", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            diaryCovers.forEach { cover ->
                                Column(
                                    Modifier
                                        .width(150.dp)
                                        .clickable {
                                            coverId = cover.id
                                            store.putString(DIARY_COVER, cover.id)
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    BookSkinCover(diarySkin, Modifier.fillMaxWidth(), cover.title)
                                    Text(
                                        cover.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
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
                        NotebookAlignmentPicker(alignment, { alignment = it })
                        LinedNotebookInput(
                            text,
                            { text = it },
                            header = titleOrDefault(title, "خاطرات امروز"),
                            textAlign = alignment,
                            overlay = { pageIndex ->
                                if (pageIndex == 0 && imagePath.isNotBlank()) {
                                    DiaryTouchPlacement(
                                        path = imagePath,
                                        widthFraction = imageWidth,
                                        offsetX = imageOffsetX,
                                        offsetY = imageOffsetY,
                                        scale = imageScale,
                                        rotation = imageRotation,
                                        active = placementMode,
                                        onTransform = { panX, panY, zoom, turn, w, h ->
                                            imageOffsetX = (imageOffsetX + panX / w).coerceIn(-.35f, .35f)
                                            imageOffsetY = (imageOffsetY + panY / h).coerceIn(-.30f, .30f)
                                            imageScale = (imageScale * zoom).coerceIn(.45f, 1.60f)
                                            imageRotation = (imageRotation + turn).coerceIn(-10f, 10f)
                                        },
                                    )
                                }
                            },
                        )
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
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (placementMode) "عکس روی خود کاغذ فعال است؛ لمس و دو انگشت برای جابه‌جایی/اندازه." else "عکس روی کاغذ ثبت شده است.",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = { placementMode = !placementMode }) {
                                    Text(if (placementMode) "تأیید" else "تنظیم دوباره")
                                }
                            }
                            OutlinedTextField(
                                value = imageCaption,
                                onValueChange = { imageCaption = it.take(140) },
                                label = { Text("عنوان عکس / پانویس") },
                                singleLine = true,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            DiaryImageWrapPicker(
                                value = wrap,
                                onValueChange = { wrap = it },
                            )
                            TextButton(onClick = {
                                deleteDiaryImage(imagePath)
                                imagePath = ""
                                imageCaption = ""
                                placementMode = false
                            }) { Text("برداشتن عکس") }
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
                    Modifier
                        .fillMaxWidth()
                        .clickable {
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
    var activePage by remember(startPage, pages.size) { mutableStateOf((startPage + 1).coerceIn(0, pages.size)) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BookStage {
            Box(Modifier.fillMaxSize()) {
                BookOpening(visible = true, modifier = Modifier.fillMaxSize()) {
                    RealisticBookPager(
                        pageCount = pages.size + 1,
                        initialPage = activePage,
                        viewerGesture = true,
                        skinned = true,
                        onPageChanged = { activePage = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 22.dp, bottom = 10.dp)
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
                                BookSkinCover(diarySkin, Modifier.fillMaxWidth(), cover.title)
                            }
                        } else {
                            val item = pages[index - 1]
                            DiaryRenderedPage(
                                item.page,
                                header = if (item.pageIndex == 0) item.entry.title else "",
                            )
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClose) { Text("بستن", color = Color.White) }
                    Text("کتاب خاطرات", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = {
                            pages.getOrNull(activePage - 1)?.let(onEdit)
                        },
                        enabled = activePage > 0,
                    ) { Text("ویرایش", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun DiaryTouchPlacement(
    path: String,
    widthFraction: Float,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    rotation: Float,
    active: Boolean,
    onTransform: (panX: Float, panY: Float, zoom: Float, rotation: Float, width: Float, height: Float) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        Box(
            Modifier
                .fillMaxWidth(widthFraction.coerceIn(.25f, .78f))
                .aspectRatio(.92f)
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = offsetX * w
                    translationY = offsetY * h
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    alpha = if (active) .98f else .94f
                }
                .shadow(20.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, if (active) Color.White else Color(0x50FFFFFF), RoundedCornerShape(4.dp))
                .pointerInput(active, path) {
                    if (!active) return@pointerInput
                    detectTransformGestures { _, pan, zoom, rotationDelta ->
                        onTransform(pan.x, pan.y, zoom, rotationDelta, w, h)
                    }
                },
        ) {
            AsyncImage(
                model = File(path),
                contentDescription = "عکس دفتر خاطرات",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * صفحهٔ خاطرات روی ورق PNG: هر سطر دقیقاً روی خط‌های ورق می‌نشیند.
 * سطر اول عنوان (تراز معکوس)، سطر دوم خالی، از سطر سوم متن. عکس ۷ سطر جا می‌گیرد و پانویسش
 * یک سطر، پس متنِ بعد از آن هم روی خط می‌ماند. BOTTOM: متن اول و عکس بعد؛ بقیه: عکس اول.
 */
@Composable
private fun DiaryRenderedPage(page: DiaryPageModel, header: String = "") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BookSkinSpread(diarySkin) { line, _ ->
            val lh = with(LocalDensity.current) { line.toSp() }
            val style = androidx.compose.ui.text.TextStyle(
                fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                fontSize = lh * 0.72f,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                lineHeight = lh,
                color = Color(0xFF19364B),
                textAlign = notebookTextAlignFromWire(page.alignment),
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                    alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Bottom,
                    trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
                ),
            )
            Column(Modifier.fillMaxSize()) {
                if (header.isNotBlank()) {
                    Text(
                        header,
                        Modifier.fillMaxWidth(),
                        style = style.copy(textAlign = oppositeTextAlign(notebookTextAlignFromWire(page.alignment))),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(line))
                }
                val hasImage = page.imagePath.isNotBlank()
                if (hasImage && page.wrap != ImageWrap.BOTTOM) {
                    DiaryImageSlot(page, line, style)
                }
                Text(page.text, Modifier.fillMaxWidth(), style = style)
                if (hasImage && page.wrap == ImageWrap.BOTTOM) {
                    DiaryImageSlot(page, line, style)
                }
            }
        }
    }
}

@Composable
private fun DiaryImageSlot(
    page: DiaryPageModel,
    line: Dp,
    style: androidx.compose.ui.text.TextStyle,
) {
    Box(
        Modifier.fillMaxWidth().height(line * 7),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = File(page.imagePath),
            contentDescription = page.caption.ifBlank { "عکس" },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(page.imageWidth.coerceIn(.25f, .78f))
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = page.imageScale
                    scaleY = page.imageScale
                    rotationZ = page.imageRotation
                    translationX = page.imageOffsetX * 240f
                    translationY = page.imageOffsetY * 180f
                }
                .shadow(8.dp, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp)),
        )
    }
    if (page.caption.isNotBlank()) {
        Text(
            page.caption,
            Modifier.fillMaxWidth(),
            style = style.copy(color = Color(0xFF526B80), textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    var alignment by remember { mutableStateOf(androidx.compose.ui.text.style.TextAlign.Right) }
    var notice by remember { mutableStateOf<String?>(null) }
    val selected = notebooks.firstOrNull { it.id == selectedId }
    LaunchedEffect(selected?.id) {
        selected?.let {
            text = container.encryptor.decrypt(it.cipher).orEmpty()
            alignment = notebookTextAlignFromWire(it.alignment)
        }
    }

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
                                    val finalTitle = nextRegisteredTitle(title.trim().ifBlank { "یادداشت‌های روزانه" }, notebooks.map { it.title })
                                    val item = Notebook(
                                        id = UUID.randomUUID().toString(),
                                        title = finalTitle,
                                        createdAt = System.currentTimeMillis(),
                                        cipher = container.encryptor.encrypt(""),
                                        alignment = "right",
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
                                "عنوانِ شماره‌دار در سطر اول می‌آید؛ یک سطر فاصله دارد و سپس نوشتن شروع می‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            NotebookAlignmentPicker(alignment, { alignment = it })
                            LinedNotebookInput(text, { text = it }, header = selected.title, textAlign = alignment)
                            Button(
                                onClick = {
                                    if (text.isBlank()) {
                                        notice = "اول چیزی بنویس."
                                    } else {
                                        val changed = selected.copy(
                                            cipher = container.encryptor.encrypt(text),
                                            alignment = notebookAlignmentWire(alignment),
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
                val pages = fullText.split(NOTEBOOK_PAGE_SEPARATOR).filter { it.isNotBlank() }
                pages.forEachIndexed { index, page ->
                    item(key = "notebook-page-" + selected.id + "-" + index) {
                        NotebookBookPage(
                            pageNumber = index + 1,
                            pageCount = pages.size.coerceAtLeast(1),
                            stackPages = (pages.size - index - 1).coerceIn(0, 7),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            NotebookPaper(
                                header = if (index == 0) selected.title else "",
                                headerAlign = oppositeTextAlign(notebookTextAlignFromWire(selected.alignment)),
                                showVerticalGuides = true,
                            ) {
                                Text(
                                    page,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 74.dp, end = 74.dp, top = 0.dp, bottom = 28.dp),
                                    fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                    fontSize = 21.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    lineHeight = 24.sp,
                                    textAlign = notebookTextAlignFromWire(selected.alignment),
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
