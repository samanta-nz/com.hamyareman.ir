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
import com.hamyareman.ir.ui.components.BookFlipper
import com.hamyareman.ir.ui.components.DraftAutoSave
import com.hamyareman.ir.ui.components.readDraft
import com.hamyareman.ir.ui.components.writeDraft
import com.hamyareman.ir.ui.components.SkinGeometry
import com.hamyareman.ir.ui.components.SkinnedNotebookEditor
import com.hamyareman.ir.ui.components.SkinnedStaticPage
import com.hamyareman.ir.ui.components.reflowText
import com.hamyareman.ir.ui.components.rememberBookFlipState
import com.hamyareman.ir.ui.components.skinTextStyle
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.hamyareman.ir.ui.study.StateSync
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.components.NOTEBOOK_PAGE_SEPARATOR
import com.hamyareman.ir.ui.components.NotebookBookPage
import com.hamyareman.ir.ui.components.NotebookPaper
import com.hamyareman.ir.ui.components.NotebookTitlePicker
import com.hamyareman.ir.ui.components.oppositeTextAlign
import com.hamyareman.ir.ui.components.NotebookAlignmentPicker
import com.hamyareman.ir.ui.components.PersianPaging
import com.hamyareman.ir.ui.components.nextRegisteredTitle
import com.hamyareman.ir.ui.components.notebookAlignmentWire
import com.hamyareman.ir.ui.components.notebookTextAlignFromWire
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val DIARY_STORE = "hamyar_private_diary"
private const val DIARY_DRAFT_KEY = "draft_diary_page"
private const val NOTEBOOK_DRAFT_PREFIX = "draft_notebook_"
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
    }.let { PersianPaging.oldestToNewest(it) { item -> item.createdAt } }
}.getOrDefault(emptyList())

private fun writeNotebooks(store: LocalStore, notebooks: List<Notebook>) {
    val array = JSONArray()
    notebooks.forEach { n ->
        array.put(JSONObject().put("id", n.id).put("title", n.title).put("createdAt", n.createdAt).put("cipher", n.cipher).put("alignment", n.alignment))
    }
    store.putString(NOTEBOOKS, array.toString())
}

private fun encodeNotebooks(notebooks: List<Notebook>): String =
    JSONArray().apply {
        notebooks.forEach { n ->
            put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("createdAt", n.createdAt)
                    .put("cipher", n.cipher)
                    .put("alignment", n.alignment),
            )
        }
    }.toString()

private fun decodeNotebooks(payload: String): List<Notebook> = runCatching {
    val array = JSONArray(payload)
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(
                Notebook(
                    id = o.getString("id"),
                    title = o.optString("title", "یادداشت‌های روزانه"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    cipher = o.optString("cipher"),
                    alignment = o.optString("alignment", "right"),
                ),
            )
        }
    }.let { PersianPaging.oldestToNewest(it) { item -> item.createdAt } }
}.getOrDefault(emptyList())

private fun mergeNotebooks(local: List<Notebook>, remote: List<Notebook>): List<Notebook> {
    val byId = linkedMapOf<String, Notebook>()
    remote.forEach { byId[it.id] = it }
    local.forEach { localItem ->
        val remoteItem = byId[localItem.id]
        if (remoteItem == null || localItem.createdAt >= remoteItem.createdAt) {
            byId[localItem.id] = localItem
        }
    }
    return PersianPaging.oldestToNewest(byId.values.toList()) { item -> item.createdAt }
}
private const val DIARY_ENTRIES = "entries"
private const val DIARY_COVER = "cover"
private const val DIARY_MEDIA_DIR = "diary-media"

private enum class ImageWrap(val wire: String, val title: String) {
    NONE("none", "عکس بالا / متن پایین"),
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
    }.let { PersianPaging.oldestToNewest(it) { item -> item.createdAt } }
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

/** ردیف‌هایی از ورق که عکس (۷ سطر) و پانویسش (۱ سطر) می‌گیرند؛ متن زیر عکس نمی‌رود. */
private fun photoRows(path: String, caption: String): Int =
    if (path.isBlank()) 0 else 7 + if (caption.isNotBlank()) 1 else 0

/** صفحه‌های تورق ۳ سایز بزرگ‌تر از صفحهٔ تایپ‌اند. */
private const val DIARY_VIEW_BONUS_SP = 3f

@Composable
private fun DiaryImageWrapPicker(
    value: ImageWrap,
    onValueChange: (ImageWrap) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                "جای عکس: " + (if (value == ImageWrap.NONE) ImageWrap.TOP else value).title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ImageWrap.entries.filter { it != ImageWrap.NONE }.forEach { option ->
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
            if (wrap == ImageWrap.NONE) wrap = ImageWrap.TOP
            notice = "تصویر انتخاب شد؛ با یک انگشت جابه‌جا و با دو انگشت بزرگ‌نمایی/چرخش کن."
        } else {
            notice = "عکس قابل ذخیره‌سازی نبود."
        }
    }

    var draftReady by remember { mutableStateOf(false) }

    /** پیش‌نویس صفحهٔ جدید؛ فقط وقتی چیزی تایپ یا عکسی انتخاب شده باشد. */
    fun draftSnapshot(): String? =
        if (text.isBlank() && imagePath.isBlank()) null
        else JSONObject()
            .put("title", title)
            .put("text", text)
            .put("imagePath", imagePath)
            .put("caption", imageCaption)
            .put("w", imageWidth.toDouble())
            .put("ox", imageOffsetX.toDouble())
            .put("oy", imageOffsetY.toDouble())
            .put("scale", imageScale.toDouble())
            .put("rot", imageRotation.toDouble())
            .put("wrap", wrap.name)
            .put("align", notebookAlignmentWire(alignment))
            .toString()

    fun restoreDraft(): Boolean {
        val d = readDraft(store, DIARY_DRAFT_KEY) { container.encryptor.decrypt(it) } ?: return false
        title = d.optString("title", title).ifBlank { "خاطرات امروز" }
        text = d.optString("text", "")
        val savedImage = d.optString("imagePath", "")
        imagePath = if (savedImage.isNotBlank() && File(savedImage).exists()) savedImage else ""
        imageCaption = if (imagePath.isBlank()) "" else d.optString("caption", "")
        imageWidth = d.optDouble("w", 0.56).toFloat()
        imageOffsetX = d.optDouble("ox", 0.0).toFloat()
        imageOffsetY = d.optDouble("oy", 0.0).toFloat()
        imageScale = d.optDouble("scale", 1.0).toFloat()
        imageRotation = d.optDouble("rot", 0.0).toFloat()
        wrap = runCatching { ImageWrap.valueOf(d.optString("wrap")) }.getOrDefault(ImageWrap.TOP)
        alignment = notebookTextAlignFromWire(d.optString("align"))
        return text.isNotBlank() || imagePath.isNotBlank()
    }

    LaunchedEffect(Unit) {
        if (restoreDraft()) notice = "پیش‌نویس قبلی‌ات برگشت؛ از همان‌جا ادامه بده."
        draftReady = true
    }
    DraftAutoSave(
        enabled = draftReady && editingEntryId == null,
        current = draftSnapshot(),
        onSave = { writeDraft(store, DIARY_DRAFT_KEY, it) { s -> container.encryptor.encrypt(s) } },
    )

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
        // پیش‌نویس صفحهٔ جدید قبل از جایگزین‌شدن با صفحهٔ در حال ویرایش نگه داشته می‌شود.
        if (editingEntryId == null) {
            writeDraft(store, DIARY_DRAFT_KEY, draftSnapshot()) { s -> container.encryptor.encrypt(s) }
        }
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
            entries = PersianPaging.oldestToNewest(entries + entry) { it.createdAt }
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
        val wasEditingExisting = currentEntry != null
        if (!wasEditingExisting) writeDraft(store, DIARY_DRAFT_KEY, null) { it }
        text = ""
        imagePath = ""
        imageCaption = ""
        editingEntryId = null
        editingPageIndex = 0
        placementMode = false
        if (wasEditingExisting) restoreDraft()
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
                        val photoRowsNow = photoRows(imagePath, imageCaption)
                        SkinnedNotebookEditor(
                            skin = diarySkin,
                            value = text,
                            onValueChange = { text = it },
                            header = if (editingEntryId == null || editingPageIndex == 0) titleOrDefault(title, "خاطرات امروز") else "",
                            textAlign = alignment,
                            firstPageTopRows = if (wrap != ImageWrap.BOTTOM) photoRowsNow else 0,
                            firstPageBottomRows = if (wrap == ImageWrap.BOTTOM) photoRowsNow else 0,
                            firstPageOverlay = { slotTop, line ->
                                if (imagePath.isNotBlank()) {
                                    DiaryPhotoBlock(
                                        slotTop = slotTop,
                                        line = line,
                                        path = imagePath,
                                        caption = imageCaption,
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

private data class DiarySlice(
    val entry: DiaryEntry,
    val pageIndex: Int,
    val sub: Int,
    val page: DiaryPageModel,
    val text: String,
)

/**
 * تورق کتاب: تک‌لمس = صفحهٔ بعد، دو لمس پیاپی = صفحهٔ قبل، کشیدن = ورق‌زدن دستی با افکت
 * جمع‌شدن ورق دور شیرازه. متن با فونت بولد و بزرگ‌تر نشان داده می‌شود و اگر از یک ورق
 * بیشتر شد، خودکار روی ورق بعدی ادامه پیدا می‌کند.
 */
@Composable
private fun DiaryBookViewer(
    cover: DiaryCover,
    pages: List<DiaryViewerPage>,
    startPage: Int,
    onEdit: (DiaryViewerPage) -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BookStage {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val geo = remember(maxWidth) { SkinGeometry(diarySkin, maxWidth.value) }
                val style = remember(geo, density) {
                    skinTextStyle(geo.line, density, TextAlign.Right, DIARY_VIEW_BONUS_SP)
                }
                val widthPx = with(density) { geo.textWidth.roundToPx() }
                val slices = remember(pages, geo, widthPx) {
                    pages.flatMap { vp ->
                        val headerRows = if (vp.pageIndex == 0 && vp.entry.title.isNotBlank()) 2 else 0
                        val firstCap = (diarySkin.lineCount - headerRows - photoRows(vp.page.imagePath, vp.page.caption))
                            .coerceAtLeast(1)
                        reflowText(vp.page.text, measurer, style, widthPx, firstCap, diarySkin.lineCount)
                            .mapIndexed { sub, t -> DiarySlice(vp.entry, vp.pageIndex, sub, vp.page, t) }
                    }
                }
                val startSlice = pages.getOrNull(startPage)?.let { vp ->
                    slices.indexOfFirst { it.entry.id == vp.entry.id && it.pageIndex == vp.pageIndex && it.sub == 0 }
                } ?: -1
                val flip = rememberBookFlipState(
                    initialPage = startSlice.coerceAtLeast(0) + 1,
                    pageCount = slices.size + 1,
                )
                Box(Modifier.fillMaxSize()) {
                    BookOpening(visible = true, modifier = Modifier.fillMaxSize()) {
                        BookFlipper(
                            state = flip,
                            modifier = Modifier.fillMaxSize().padding(top = 22.dp, bottom = 10.dp),
                        ) { index ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (index == 0) {
                                    BookSkinCover(diarySkin, Modifier.fillMaxWidth(), cover.title)
                                } else {
                                    slices.getOrNull(index - 1)?.let { DiarySlicePage(it) }
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
                        Text("کتاب خاطرات", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = {
                                slices.getOrNull(flip.current - 1)?.let {
                                    onEdit(DiaryViewerPage(it.entry, it.pageIndex, it.page))
                                }
                            },
                            enabled = flip.current > 0,
                        ) { Text("ویرایش", color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiarySlicePage(slice: DiarySlice) {
    val page = slice.page
    val first = slice.sub == 0
    val hasImage = first && page.imagePath.isNotBlank()
    val rows = if (hasImage) photoRows(page.imagePath, page.caption) else 0
    SkinnedStaticPage(
        skin = diarySkin,
        header = if (first && slice.pageIndex == 0) slice.entry.title else "",
        text = slice.text,
        textAlign = notebookTextAlignFromWire(page.alignment),
        bonusSp = DIARY_VIEW_BONUS_SP,
        topRows = if (page.wrap != ImageWrap.BOTTOM) rows else 0,
        bottomRows = if (page.wrap == ImageWrap.BOTTOM) rows else 0,
        overlay = { slotTop, line ->
            if (hasImage) {
                DiaryPhotoBlock(
                    slotTop = slotTop,
                    line = line,
                    path = page.imagePath,
                    caption = page.caption,
                    widthFraction = page.imageWidth,
                    offsetX = page.imageOffsetX,
                    offsetY = page.imageOffsetY,
                    scale = page.imageScale,
                    rotation = page.imageRotation,
                    bonusSp = DIARY_VIEW_BONUS_SP,
                )
            }
        },
    )
}

/**
 * عکسِ روی ورق؛ هم در ویرایشگر (پیش‌نمایش و جابه‌جایی) و هم در تورق با همین کد کشیده می‌شود،
 * پس جای عکس هنگام تایپ دقیقاً همان است که در کتاب دیده می‌شود. ۷ سطر برای عکس و ۱ سطر
 * پانویس رزرو است و متن هیچ‌وقت زیر عکس نمی‌رود.
 */
@Composable
private fun DiaryPhotoBlock(
    slotTop: Dp,
    line: Dp,
    path: String,
    caption: String,
    widthFraction: Float,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    rotation: Float,
    active: Boolean = false,
    bonusSp: Float = 0f,
    onTransform: (panX: Float, panY: Float, zoom: Float, rotation: Float, width: Float, height: Float) -> Unit =
        { _, _, _, _, _, _ -> },
) {
    val density = LocalDensity.current
    Column(Modifier.offset(y = slotTop).fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(line * 7)) {
            val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            Box(
                Modifier
                    .fillMaxWidth(widthFraction.coerceIn(.25f, .78f))
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = offsetX * w
                        translationY = offsetY * h
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
                    .shadow(8.dp, RoundedCornerShape(2.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .then(
                        if (active) Modifier.border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        else Modifier,
                    )
                    .pointerInput(active, path) {
                        if (!active) return@pointerInput
                        detectTransformGestures { _, pan, zoom, rotationDelta ->
                            onTransform(pan.x, pan.y, zoom, rotationDelta, w, h)
                        }
                    },
            ) {
                AsyncImage(
                    model = File(path),
                    contentDescription = caption.ifBlank { "عکس" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (caption.isNotBlank()) {
            Text(
                caption,
                Modifier.fillMaxWidth().height(line),
                style = skinTextStyle(line, density, TextAlign.Center, bonusSp)
                    .copy(color = Color(0xFF526B80)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** ورق‌های ذخیره‌شدهٔ یک متن؛ اگر از یک ورق بیشتر شد زیر هم ادامه پیدا می‌کند. */
@Composable
private fun SkinnedReflowedPages(
    skin: BookSkin,
    header: String,
    text: String,
    textAlign: TextAlign,
    bonusSp: Float = 0f,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val geo = remember(skin, maxWidth) { SkinGeometry(skin, maxWidth.value) }
        val style = remember(geo, density) { skinTextStyle(geo.line, density, textAlign, bonusSp) }
        val widthPx = with(density) { geo.textWidth.roundToPx() }
        val headerRows = if (header.isNotBlank()) 2 else 0
        val chunks = remember(text, geo, widthPx, headerRows) {
            reflowText(text, measurer, style, widthPx, (skin.lineCount - headerRows).coerceAtLeast(1), skin.lineCount)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            chunks.forEachIndexed { i, chunk ->
                SkinnedStaticPage(
                    skin = skin,
                    header = if (i == 0) header else "",
                    text = chunk,
                    textAlign = textAlign,
                    bonusSp = bonusSp,
                )
            }
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
    var alignment by remember { mutableStateOf(androidx.compose.ui.text.style.TextAlign.Right) }
    var notice by remember { mutableStateOf<String?>(null) }
    val selected = notebooks.firstOrNull { it.id == selectedId }
    var loadedNotebookId by remember { mutableStateOf<String?>(null) }
    val savedNotebookText = selected?.let { container.encryptor.decrypt(it.cipher).orEmpty() }
    LaunchedEffect(selected?.id) {
        loadedNotebookId = null
        selected?.let {
            val saved = container.encryptor.decrypt(it.cipher).orEmpty()
            val d = readDraft(store, NOTEBOOK_DRAFT_PREFIX + it.id) { raw -> container.encryptor.decrypt(raw) }
            text = d?.optString("text", saved) ?: saved
            alignment = notebookTextAlignFromWire(d?.optString("align") ?: it.alignment)
            if (d != null && text != saved) notice = "پیش‌نویس قبلی‌ات برگشت؛ از همان‌جا ادامه بده."
            loadedNotebookId = it.id
        }
    }
    fun notebookDraftJson(): String? =
        if (selected == null || text == savedNotebookText) null
        else JSONObject().put("text", text).put("align", notebookAlignmentWire(alignment)).toString()
    fun persistNotebookDraft() {
        val id = selected?.id ?: return
        if (loadedNotebookId != id) return
        writeDraft(store, NOTEBOOK_DRAFT_PREFIX + id, notebookDraftJson()) { s -> container.encryptor.encrypt(s) }
    }
    DraftAutoSave(
        enabled = selected != null && loadedNotebookId == selected.id,
        current = notebookDraftJson(),
        onSave = {
            val id = selectedId
            if (id != null && loadedNotebookId == id) {
                writeDraft(store, NOTEBOOK_DRAFT_PREFIX + id, it) { s -> container.encryptor.encrypt(s) }
            }
        },
    )

    fun queueSnapshot(snapshot: List<Notebook>) {
        val uid = container.auth.cachedUserId().orEmpty()
        if (uid.isBlank()) return
        val now = System.currentTimeMillis()
        val payload = encodeNotebooks(snapshot)
        StateSync.markLocal(context, StateSync.KEY_NOTEBOOKS)
        container.sync.enqueue(
            TableIds.APP_STATE,
            StateSync.rowId(uid, StateSync.KEY_NOTEBOOKS),
            mapOf(
                "userId" to uid,
                "key" to StateSync.KEY_NOTEBOOKS,
                "payload" to payload,
                "updatedAt" to now,
            ),
        )
        scope.launch { runCatching { container.sync.pushAll() } }
    }

    LaunchedEffect(Unit) {
        val uid = container.auth.cachedUserId().orEmpty()
        if (uid.isBlank()) return@LaunchedEffect
        val local = readNotebooks(store)
        val localAt = StateSync.localAt(context, StateSync.KEY_NOTEBOOKS)
        val remote = StateSync.pull(context, container.tables, uid, StateSync.KEY_NOTEBOOKS)
        if (remote == null) {
            if (local.isNotEmpty()) queueSnapshot(local)
            return@LaunchedEffect
        }

        val remoteList = decodeNotebooks(remote.first)
        if (localAt == 0L && local.isNotEmpty()) {
            val merged = mergeNotebooks(local, remoteList)
            if (merged != remoteList) {
                notebooks = merged
                writeNotebooks(store, merged)
                queueSnapshot(merged)
            } else if (notebooks != remoteList) {
                notebooks = remoteList
                writeNotebooks(store, remoteList)
                StateSync.markSyncedAt(context, StateSync.KEY_NOTEBOOKS, remote.second)
            }
        } else if (remote.second > localAt) {
            notebooks = remoteList
            writeNotebooks(store, remoteList)
            StateSync.markSyncedAt(context, StateSync.KEY_NOTEBOOKS, remote.second)
        } else if (localAt > remote.second && local.isNotEmpty()) {
            queueSnapshot(local)
        } else if (remoteList != local) {
            val merged = mergeNotebooks(local, remoteList)
            notebooks = merged
            writeNotebooks(store, merged)
            queueSnapshot(merged)
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            if (selected == null) "دفترچه‌های یادداشت" else selected.title,
            if (selected == null) onBack else ({ persistNotebookDraft(); selectedId = null; text = ""; notice = null }),
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
                                    queueSnapshot(notebooks)
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
                                queueSnapshot(notebooks)
                                scope.launch { runCatching { container.sync.pushAll() } }
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
                            NotebookAlignmentPicker(alignment, { alignment = it })
                            SkinnedNotebookEditor(
                                skin = BookSkin.NavyFloral,
                                value = text,
                                onValueChange = { text = it },
                                header = selected.title,
                                textAlign = alignment,
                            )
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
                                        queueSnapshot(notebooks)
                                        writeDraft(store, NOTEBOOK_DRAFT_PREFIX + selected.id, null) { it }
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
                        SkinnedReflowedPages(
                            skin = BookSkin.NavyFloral,
                            header = if (index == 0) selected.title else "",
                            text = page,
                            textAlign = notebookTextAlignFromWire(selected.alignment),
                        )
                    }
                }
            }
        }
    }
}
