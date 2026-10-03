package com.hamyareman.ir.ui.safespace

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.ui.appearance.EmbeddedFonts
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.study.SecureWebEffect
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val DIARY_STORE = "hamyar_private_diary"
private const val DIARY_ENTRIES = "entries"
private const val DIARY_COVER = "cover"
private const val NOTEBOOKS = "notebooks"

private data class DiaryCover(val id: String, val title: String, val asset: String)
private val DiaryCovers = listOf(
    DiaryCover("celestial", "آسمان خیال", "file:///android_asset/diary/cover-celestial.jpg"),
    DiaryCover("botanical", "باغ خیال", "file:///android_asset/diary/cover-botanical.jpg"),
    DiaryCover("geometric", "هندسهٔ رؤیا", "file:///android_asset/diary/cover-geometric.jpg"),
)

private data class DiaryEntry(
    val id: String,
    val createdAt: Long,
    val title: String,
    val cipher: String,
)

private data class Notebook(
    val id: String,
    val title: String,
    val createdAt: Long,
    val cipher: String,
)

private fun readDiary(store: LocalStore): List<DiaryEntry> = runCatching {
    val array = JSONArray(store.getString(DIARY_ENTRIES, "[]"))
    buildList {
        for (i in 0 until array.length()) array.getJSONObject(i).let { o ->
            add(DiaryEntry(o.getString("id"), o.getLong("createdAt"), o.optString("title"), o.getString("cipher")))
        }
    }.sortedBy { it.createdAt }
}.getOrDefault(emptyList())

private fun writeDiary(store: LocalStore, entries: List<DiaryEntry>) {
    val array = JSONArray()
    entries.forEach { e ->
        array.put(JSONObject().put("id", e.id).put("createdAt", e.createdAt).put("title", e.title).put("cipher", e.cipher))
    }
    store.putString(DIARY_ENTRIES, array.toString())
}

private fun readNotebooks(store: LocalStore): List<Notebook> = runCatching {
    val array = JSONArray(store.getString(NOTEBOOKS, "[]"))
    buildList {
        for (i in 0 until array.length()) array.getJSONObject(i).let { o ->
            add(Notebook(o.getString("id"), o.getString("title"), o.getLong("createdAt"), o.getString("cipher")))
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writeNotebooks(store: LocalStore, notebooks: List<Notebook>) {
    val array = JSONArray()
    notebooks.forEach { n ->
        array.put(JSONObject().put("id", n.id).put("title", n.title).put("createdAt", n.createdAt).put("cipher", n.cipher))
    }
    store.putString(NOTEBOOKS, array.toString())
}

/** هر صفحه تقریباً یک برگ واقعی است؛ شکست در نزدیک‌ترین فاصله انجام می‌شود. */
private fun paginate(text: String, maxChars: Int = 720): List<String> {
    if (text.isBlank()) return listOf("")
    val out = mutableListOf<String>()
    var rest = text.trim()
    while (rest.length > maxChars) {
        val floor = (maxChars * 0.72f).toInt()
        val split = rest.lastIndexOfAny(charArrayOf('\n', ' ', '،', '.'), startIndex = maxChars)
            .takeIf { it >= floor } ?: maxChars
        out += rest.substring(0, split).trim()
        rest = rest.substring(split).trim()
    }
    if (rest.isNotBlank()) out += rest
    return out.ifEmpty { listOf("") }
}

@Composable
fun DiaryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, DIARY_STORE) }
    var coverId by remember { mutableStateOf(store.getString(DIARY_COVER, DiaryCovers.first().id)) }
    var entries by remember { mutableStateOf(readDiary(store)) }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    SecureWebEffect("Screenshots are disabled in the private diary.")

    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val payload = JSONObject()
            .put("format", "hamyar-diary-1")
            .put("cover", coverId)
            .put("entries", JSONArray(store.getString(DIARY_ENTRIES, "[]")))
            .toString()
        notice = runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
            "بکاپ رمز‌شدهٔ جلد و همهٔ صفحه‌ها ذخیره شد."
        }.getOrElse { "ساخت بکاپ ممکن نشد." }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        notice = runCatching {
            val payload = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = JSONObject(payload)
            require(root.optString("format") == "hamyar-diary-1")
            val incomingCover = root.optString("cover", DiaryCovers.first().id)
            val incomingEntries = root.getJSONArray("entries").toString()
            store.putString(DIARY_COVER, incomingCover)
            store.putString(DIARY_ENTRIES, incomingEntries)
            coverId = incomingCover
            entries = readDiary(store)
            "جلد و محتوای دفتر خاطرات از بکاپ بازیابی شد."
        }.getOrElse { "این فایل بکاپ معتبر نیست یا روی این دستگاه رمزگشایی نمی‌شود." }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("دفتر خاطرات", onBack)
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("جلد دفتر", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiaryCovers.forEach { cover ->
                        Column(
                            Modifier.weight(1f).clickable {
                                coverId = cover.id
                                store.putString(DIARY_COVER, cover.id)
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = cover.asset,
                                contentDescription = cover.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(if (cover.id == coverId) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier),
                            )
                            Text(
                                if (cover.id == coverId) "✓ ${cover.title}" else cover.title,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            item {
                Text("افزودن به دفتر", style = MaterialTheme.typography.titleMedium)
                Text(
                    JalaliDate.stampFa(System.currentTimeMillis()),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("عنوان یا زیرمتن (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LinedNotebookInput(text, { text = it })
                PrimaryButton(if (editingId == null) "افزودن صفحه‌ها به انتهای دفتر" else "ذخیرهٔ ویرایش صفحه‌ها") {
                    if (text.isBlank()) {
                        notice = "اول چیزی بنویس."
                    } else {
                        val current = editingId?.let { id -> entries.firstOrNull { it.id == id } }
                        val changed = DiaryEntry(
                            current?.id ?: UUID.randomUUID().toString(),
                            current?.createdAt ?: System.currentTimeMillis(),
                            title.trim(),
                            container.encryptor.encrypt(text.trim()),
                        )
                        val next = if (current == null) entries + changed else entries.map { if (it.id == changed.id) changed else it }
                        writeDiary(store, next)
                        entries = next
                        title = ""
                        text = ""
                        editingId = null
                        notice = if (current == null) {
                            "نوشته بدون پاک‌کردن صفحه‌های قبلی به انتهای دفتر اضافه شد."
                        } else {
                            "ویرایش ذخیره شد و ترتیب صفحه‌ها تغییر نکرد."
                        }
                    }
                }
                if (editingId != null) {
                    TextButton(onClick = { editingId = null; title = ""; text = "" }) { Text("لغو ویرایش") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewer = true }, modifier = Modifier.weight(1f), enabled = entries.isNotEmpty()) {
                        Text("ورق‌زدن تمام‌صفحه")
                    }
                    OutlinedButton(onClick = { backup.launch("hamyar-diary-${JalaliDate.todayIso()}.json") }, modifier = Modifier.weight(1f)) {
                        Text("بکاپ")
                    }
                    OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.weight(1f)) {
                        Text("بازیابی")
                    }
                }
                notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
            if (entries.isEmpty()) {
                item { Text("دفتر هنوز صفحه‌ای ندارد.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(entries.asReversed(), key = { it.id }) { entry ->
                val plain = remember(entry.cipher) { container.encryptor.decrypt(entry.cipher).orEmpty() }
                Card(Modifier.fillMaxWidth().clickable {
                    editingId = entry.id
                    title = entry.title
                    text = plain
                    notice = "این نوشته برای ویرایش در دفتر بالا باز شد."
                }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(JalaliDate.stampFa(entry.createdAt), color = MaterialTheme.colorScheme.primary)
                        if (entry.title.isNotBlank()) Text(entry.title, style = MaterialTheme.typography.titleSmall)
                        Text(plain, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            val next = entries.filterNot { it.id == entry.id }
                            writeDiary(store, next)
                            entries = next
                            if (editingId == entry.id) { editingId = null; title = ""; text = "" }
                        }) { Text("حذف این نوشته") }
                    }
                }
            }
        }
    }

    if (viewer) {
        DiaryFullscreenViewer(
            cover = DiaryCovers.firstOrNull { it.id == coverId } ?: DiaryCovers.first(),
            entries = entries,
            decrypt = { container.encryptor.decrypt(it).orEmpty() },
            onClose = { viewer = false },
        )
    }
}

private data class DiaryPage(val date: String, val subtitle: String, val text: String)

@Composable
private fun DiaryFullscreenViewer(
    cover: DiaryCover,
    entries: List<DiaryEntry>,
    decrypt: (String) -> String,
    onClose: () -> Unit,
) {
    val pages = remember(entries) {
        buildList {
            entries.forEach { entry ->
                paginate(decrypt(entry.cipher)).forEach { page ->
                    add(DiaryPage(JalaliDate.stampFa(entry.createdAt), entry.title, page))
                }
            }
        }
    }
    val pager = rememberPagerState(pageCount = { pages.size + 1 })
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF080B16))) {
            HorizontalPager(
                state = pager,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) { index ->
                // چرخش Y و جابه‌جایی افقی حس برگشتن ورق را می‌دهد؛ ترتیب خود Pager
                // راست‌به‌چپ است و برای همین جلد صفحهٔ صفر باقی می‌ماند.
                val pageOffset = (pager.currentPage - index) + pager.currentPageOffsetFraction
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        rotationY = (pageOffset * 24f).coerceIn(-34f, 34f)
                        translationX = pageOffset * 22f
                        cameraDistance = 18f * density
                        alpha = (1f - kotlin.math.abs(pageOffset) * 0.16f).coerceIn(0.72f, 1f)
                        shadowElevation = kotlin.math.abs(pageOffset) * 10f
                    },
                ) {
                    if (index == 0) {
                        AsyncImage(
                            model = cover.asset,
                            contentDescription = cover.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(18.dp),
                        )
                    } else {
                        val page = pages[index - 1]
                        Box(
                            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 36.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/diary/page-lined.jpg",
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Column(Modifier.fillMaxSize().padding(start = 34.dp, end = 58.dp, top = 42.dp, bottom = 30.dp)) {
                                Text(page.date, color = Color(0xFF36506B), fontFamily = EmbeddedFonts.family("vazirmatn", EmbeddedFonts.W_BOLD))
                                if (page.subtitle.isNotBlank()) {
                                    Text(page.subtitle, color = Color(0xFF5F4774), fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD), fontSize = 16.sp)
                                }
                                Text(
                                    page.text,
                                    color = Color(0xFF172B3A),
                                    fontFamily = EmbeddedFonts.family("vazirmatn", EmbeddedFonts.W_LIGHT),
                                    fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    textAlign = TextAlign.Right,
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    toPersianDigits("${pager.currentPage + 1}/${pages.size + 1}"),
                    color = Color.White,
                    modifier = Modifier.background(Color(0x88000000), RoundedCornerShape(8.dp)).padding(8.dp),
                )
                TextButton(onClick = onClose) { Text("بستن", color = Color.White) }
            }
        }
    }
}

@Composable
fun NotebooksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, DIARY_STORE) }
    val scope = rememberCoroutineScope()
    var notebooks by remember { mutableStateOf(readNotebooks(store)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val selected = notebooks.firstOrNull { it.id == selectedId }
    fun rowId(id: String) = "notebook_" + container.auth.cachedUserId().orEmpty().take(32) + "_" + id.take(32)
    fun queue(n: Notebook) {
        val uid = container.auth.cachedUserId().orEmpty()
        if (uid.isNotBlank()) container.sync.enqueue(TableIds.APP_STATE, rowId(n.id), mapOf("userId" to uid, "key" to "private_notebook", "notebookId" to n.id, "title" to n.title, "createdAt" to n.createdAt, "cipher" to n.cipher, "updatedAt" to System.currentTimeMillis()))
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (selected == null) "دفترچه‌های من" else selected.title, if (selected == null) onBack else ({ selectedId = null; text = "" }))
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selected == null) {
                item {
                    Text("دفترچه تازه", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(title, { title = it.take(80) }, label = { Text("نام دفترچه") }, modifier = Modifier.fillMaxWidth())
                    PrimaryButton("ساخت دفترچه") {
                        if (title.isNotBlank()) {
                            val item = Notebook(UUID.randomUUID().toString(), title.trim(), System.currentTimeMillis(), container.encryptor.encrypt(""))
                            notebooks = listOf(item) + notebooks
                            writeNotebooks(store, notebooks)
                            queue(item)
                            selectedId = item.id
                            title = ""
                        }
                    }
                }
                if (notebooks.isEmpty()) item { Text("هنوز دفترچه‌ای نساخته‌ای.") }
                items(notebooks, key = { it.id }) { notebook ->
                    val body = remember(notebook.cipher) { container.encryptor.decrypt(notebook.cipher).orEmpty() }
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { selectedId = notebook.id }) {
                                Text("📓 ${notebook.title}", style = MaterialTheme.typography.titleMedium)
                                Text(toPersianDigits("${paginate(body).size} صفحه"), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = {
                                notebooks = notebooks.filterNot { it.id == notebook.id }
                                writeNotebooks(store, notebooks)
                                scope.launch { runCatching { container.tables.delete(TableIds.APP_STATE, rowId(notebook.id)) } }
                            }) { Text("حذف") }
                        }
                    }
                }
            } else {
                item {
                    Text("دفتر روی کاغذ واقعی نمایش داده می‌شود؛ بعد از ذخیره به فهرست برمی‌گردی.")
                    LinedNotebookInput(text, { text = it })
                    PrimaryButton("ذخیره و برگشت") {
                        if (text.isNotBlank()) {
                            val old = container.encryptor.decrypt(selected.cipher).orEmpty()
                            val joined = listOf(old, text.trim()).filter { it.isNotBlank() }.joinToString("\n\n")
                            val changed = selected.copy(cipher = container.encryptor.encrypt(joined))
                            notebooks = notebooks.map { if (it.id == changed.id) changed else it }
                            writeNotebooks(store, notebooks)
                            queue(changed)
                            text = ""
                            notice = "ذخیره شد."
                            selectedId = null
                        }
                    }
                    notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
                val fullText = container.encryptor.decrypt(selected.cipher).orEmpty()
                items(paginate(fullText).mapIndexed { index, page -> index to page }, key = { it.first }) { (index, page) ->
                    Card(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().aspectRatio(0.707f)) {
                            AsyncImage(model = "file:///android_asset/diary/page-lined.jpg", contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                            Column(Modifier.fillMaxSize().padding(start = 28.dp, end = 48.dp, top = 28.dp, bottom = 20.dp)) {
                                Text("صفحهٔ ${toPersianDigits((index + 1).toString())}", color = Color(0xFF5F4774))
                                Text(page, color = Color(0xFF172B3A), fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD), fontSize = 18.sp, lineHeight = 24.sp, textAlign = TextAlign.Right)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun writeDiary(store: LocalStore, entries: List<DiaryEntry>) {
    val array = JSONArray()
    entries.forEach { e ->
        array.put(JSONObject().put("id", e.id).put("createdAt", e.createdAt).put("title", e.title).put("cipher", e.cipher))
    }
    store.putString(DIARY_ENTRIES, array.toString())
}

private fun readNotebooks(store: LocalStore): List<Notebook> = runCatching {
    val array = JSONArray(store.getString(NOTEBOOKS, "[]"))
    buildList {
        for (i in 0 until array.length()) array.getJSONObject(i).let { o ->
            add(Notebook(o.getString("id"), o.getString("title"), o.getLong("createdAt"), o.getString("cipher")))
        }
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun writeNotebooks(store: LocalStore, notebooks: List<Notebook>) {
    val array = JSONArray()
    notebooks.forEach { n ->
        array.put(JSONObject().put("id", n.id).put("title", n.title).put("createdAt", n.createdAt).put("cipher", n.cipher))
    }
    store.putString(NOTEBOOKS, array.toString())
}

/** هر صفحه تقریباً یک برگ واقعی است؛ شکست در نزدیک‌ترین فاصله انجام می‌شود. */
private fun paginate(text: String, maxChars: Int = 720): List<String> {
    if (text.isBlank()) return listOf("")
    val out = mutableListOf<String>()
    var rest = text.trim()
    while (rest.length > maxChars) {
        val floor = (maxChars * 0.72f).toInt()
        val split = rest.lastIndexOfAny(charArrayOf('\n', ' ', '،', '.'), startIndex = maxChars)
            .takeIf { it >= floor } ?: maxChars
        out += rest.substring(0, split).trim()
        rest = rest.substring(split).trim()
    }
    if (rest.isNotBlank()) out += rest
    return out.ifEmpty { listOf("") }
}

@Composable
fun DiaryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, DIARY_STORE) }
    var coverId by remember { mutableStateOf(store.getString(DIARY_COVER, DiaryCovers.first().id)) }
    var entries by remember { mutableStateOf(readDiary(store)) }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    SecureWebEffect("Screenshots are disabled in the private diary.")

    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val payload = JSONObject()
            .put("format", "hamyar-diary-1")
            .put("cover", coverId)
            .put("entries", JSONArray(store.getString(DIARY_ENTRIES, "[]")))
            .toString()
        notice = runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
            "بکاپ رمز‌شدهٔ جلد و همهٔ صفحه‌ها ذخیره شد."
        }.getOrElse { "ساخت بکاپ ممکن نشد." }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        notice = runCatching {
            val payload = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val root = JSONObject(payload)
            require(root.optString("format") == "hamyar-diary-1")
            val incomingCover = root.optString("cover", DiaryCovers.first().id)
            val incomingEntries = root.getJSONArray("entries").toString()
            store.putString(DIARY_COVER, incomingCover)
            store.putString(DIARY_ENTRIES, incomingEntries)
            coverId = incomingCover
            entries = readDiary(store)
            "جلد و محتوای دفتر خاطرات از بکاپ بازیابی شد."
        }.getOrElse { "این فایل بکاپ معتبر نیست یا روی این دستگاه رمزگشایی نمی‌شود." }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("دفتر خاطرات", onBack)
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("جلد دفتر", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiaryCovers.forEach { cover ->
                        Column(
                            Modifier.weight(1f).clickable {
                                coverId = cover.id
                                store.putString(DIARY_COVER, cover.id)
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = cover.asset,
                                contentDescription = cover.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(if (cover.id == coverId) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier),
                            )
                            Text(
                                if (cover.id == coverId) "✓ ${cover.title}" else cover.title,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            item {
                Text("افزودن به دفتر", style = MaterialTheme.typography.titleMedium)
                Text(
                    JalaliDate.stampFa(System.currentTimeMillis()),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("عنوان یا زیرمتن (اختیاری)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LinedNotebookInput(text, { text = it })
                PrimaryButton(if (editingId == null) "افزودن صفحه‌ها به انتهای دفتر" else "ذخیرهٔ ویرایش صفحه‌ها") {
                    if (text.isBlank()) {
                        notice = "اول چیزی بنویس."
                    } else {
                        val current = editingId?.let { id -> entries.firstOrNull { it.id == id } }
                        val changed = DiaryEntry(
                            current?.id ?: UUID.randomUUID().toString(),
                            current?.createdAt ?: System.currentTimeMillis(),
                            title.trim(),
                            container.encryptor.encrypt(text.trim()),
                        )
                        val next = if (current == null) entries + changed else entries.map { if (it.id == changed.id) changed else it }
                        writeDiary(store, next)
                        entries = next
                        title = ""
                        text = ""
                        editingId = null
                        notice = if (current == null) {
                            "نوشته بدون پاک‌کردن صفحه‌های قبلی به انتهای دفتر اضافه شد."
                        } else {
                            "ویرایش ذخیره شد و ترتیب صفحه‌ها تغییر نکرد."
                        }
                    }
                }
                if (editingId != null) {
                    TextButton(onClick = { editingId = null; title = ""; text = "" }) { Text("لغو ویرایش") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewer = true }, modifier = Modifier.weight(1f), enabled = entries.isNotEmpty()) {
                        Text("ورق‌زدن تمام‌صفحه")
                    }
                    OutlinedButton(onClick = { backup.launch("hamyar-diary-${JalaliDate.todayIso()}.json") }, modifier = Modifier.weight(1f)) {
                        Text("بکاپ")
                    }
                    OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.weight(1f)) {
                        Text("بازیابی")
                    }
                }
                notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
            if (entries.isEmpty()) {
                item { Text("دفتر هنوز صفحه‌ای ندارد.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(entries.asReversed(), key = { it.id }) { entry ->
                val plain = remember(entry.cipher) { container.encryptor.decrypt(entry.cipher).orEmpty() }
                Card(Modifier.fillMaxWidth().clickable {
                    editingId = entry.id
                    title = entry.title
                    text = plain
                    notice = "این نوشته برای ویرایش در دفتر بالا باز شد."
                }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(JalaliDate.stampFa(entry.createdAt), color = MaterialTheme.colorScheme.primary)
                        if (entry.title.isNotBlank()) Text(entry.title, style = MaterialTheme.typography.titleSmall)
                        Text(plain, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = {
                            val next = entries.filterNot { it.id == entry.id }
                            writeDiary(store, next)
                            entries = next
                            if (editingId == entry.id) { editingId = null; title = ""; text = "" }
                        }) { Text("حذف این نوشته") }
                    }
                }
            }
        }
    }

    if (viewer) {
        DiaryFullscreenViewer(
            cover = DiaryCovers.firstOrNull { it.id == coverId } ?: DiaryCovers.first(),
            entries = entries,
            decrypt = { container.encryptor.decrypt(it).orEmpty() },
            onClose = { viewer = false },
        )
    }
}

private data class DiaryPage(val date: String, val subtitle: String, val text: String)

@Composable
private fun DiaryFullscreenViewer(
    cover: DiaryCover,
    entries: List<DiaryEntry>,
    decrypt: (String) -> String,
    onClose: () -> Unit,
) {
    val pages = remember(entries) {
        buildList {
            entries.forEach { entry ->
                paginate(decrypt(entry.cipher)).forEach { page ->
                    add(DiaryPage(JalaliDate.stampFa(entry.createdAt), entry.title, page))
                }
            }
        }
    }
    val pager = rememberPagerState(pageCount = { pages.size + 1 })
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF080B16))) {
            HorizontalPager(
                state = pager,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) { index ->
                // چرخش Y و جابه‌جایی افقی حس برگشتن ورق را می‌دهد؛ ترتیب خود Pager
                // راست‌به‌چپ است و برای همین جلد صفحهٔ صفر باقی می‌ماند.
                val pageOffset = (pager.currentPage - index) + pager.currentPageOffsetFraction
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        rotationY = (pageOffset * 24f).coerceIn(-34f, 34f)
                        translationX = pageOffset * 22f
                        cameraDistance = 18f * density
                        alpha = (1f - kotlin.math.abs(pageOffset) * 0.16f).coerceIn(0.72f, 1f)
                        shadowElevation = kotlin.math.abs(pageOffset) * 10f
                    },
                ) {
                    if (index == 0) {
                        AsyncImage(
                            model = cover.asset,
                            contentDescription = cover.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(18.dp),
                        )
                    } else {
                        val page = pages[index - 1]
                        Box(
                            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 36.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/diary/page-lined.jpg",
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Column(Modifier.fillMaxSize().padding(start = 34.dp, end = 58.dp, top = 42.dp, bottom = 30.dp)) {
                                Text(page.date, color = Color(0xFF36506B), fontFamily = EmbeddedFonts.family("vazirmatn", EmbeddedFonts.W_BOLD))
                                if (page.subtitle.isNotBlank()) {
                                    Text(page.subtitle, color = Color(0xFF5F4774), fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD), fontSize = 16.sp)
                                }
                                Text(
                                    page.text,
                                    color = Color(0xFF172B3A),
                                    fontFamily = EmbeddedFonts.family("vazirmatn", EmbeddedFonts.W_LIGHT),
                                    fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    textAlign = TextAlign.Right,
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    toPersianDigits("${pager.currentPage + 1}/${pages.size + 1}"),
                    color = Color.White,
                    modifier = Modifier.background(Color(0x88000000), RoundedCornerShape(8.dp)).padding(8.dp),
                )
                TextButton(onClick = onClose) { Text("بستن", color = Color.White) }
            }
        }
    }
}

@Composable
fun NotebooksScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, DIARY_STORE) }
    var notebooks by remember { mutableStateOf(readNotebooks(store)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val selected = notebooks.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(if (selected == null) "دفترچه‌های من" else selected.title, if (selected == null) onBack else ({ selectedId = null; text = "" }))
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selected == null) {
                item {
                    Text("دفترچه تازه", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(title, { title = it.take(80) }, label = { Text("نام دفترچه") }, modifier = Modifier.fillMaxWidth())
                    PrimaryButton("ساخت دفترچه") {
                        if (title.isNotBlank()) {
                            val item = Notebook(UUID.randomUUID().toString(), title.trim(), System.currentTimeMillis(), container.encryptor.encrypt(""))
                            notebooks = listOf(item) + notebooks
                            writeNotebooks(store, notebooks)
                            selectedId = item.id
                            title = ""
                        }
                    }
                }
                if (notebooks.isEmpty()) item { Text("هنوز دفترچه‌ای نساخته‌ای.") }
                items(notebooks, key = { it.id }) { notebook ->
                    val body = remember(notebook.cipher) { container.encryptor.decrypt(notebook.cipher).orEmpty() }
                    Card(Modifier.fillMaxWidth().clickable { selectedId = notebook.id }) {
                        Column(Modifier.padding(14.dp)) {
                            Text("📓 ${notebook.title}", style = MaterialTheme.typography.titleMedium)
                            Text(toPersianDigits("${paginate(body).size} صفحه"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                item {
                    Text("نوشته‌های قبلی پاک نمی‌شوند؛ متن تازه به انتهای دفترچه اضافه می‌شود.")
                    LinedNotebookInput(text, { text = it })
                    PrimaryButton("افزودن به انتهای دفترچه") {
                        if (text.isNotBlank()) {
                            val old = container.encryptor.decrypt(selected.cipher).orEmpty()
                            val joined = listOf(old, text.trim()).filter { it.isNotBlank() }.joinToString("\n\n")
                            val changed = selected.copy(cipher = container.encryptor.encrypt(joined))
                            notebooks = notebooks.map { if (it.id == changed.id) changed else it }
                            writeNotebooks(store, notebooks)
                            text = ""
                            notice = "متن ذخیره شد؛ دفترچه بسته شد."
                            selectedId = null
                        }
                    }
                    notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
                val fullText = container.encryptor.decrypt(selected.cipher).orEmpty()
                items(paginate(fullText).mapIndexed { index, page -> index to page }, key = { it.first }) { (index, page) ->
                    Card(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().aspectRatio(0.707f)) {
                            AsyncImage(
                                model = "file:///android_asset/diary/page-lined.jpg",
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Column(Modifier.padding(start = 28.dp, end = 48.dp, top = 28.dp, bottom = 20.dp)) {
                                Text("صفحهٔ ${toPersianDigits((index + 1).toString())}", color = Color(0xFF5F4774))
                                Text(
                                    page,
                                    color = Color(0xFF172B3A),
                                    fontFamily = EmbeddedFonts.family("vazirmatn", EmbeddedFonts.W_LIGHT),
                                    fontFamily = EmbeddedFonts.family("badkhat_bold", EmbeddedFonts.W_BOLD),
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    textAlign = TextAlign.Right,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
