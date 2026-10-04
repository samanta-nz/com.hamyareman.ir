package com.hamyareman.ir.ui.study

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.ui.text.style.TextAlign
import com.hamyareman.ir.ui.AppTypography
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.CircularProgressIndicator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.R
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.feature.hearttoheart.MediaFiles
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.profile.StudentProfileState
import com.hamyareman.ir.ui.profile.loadOrientedBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class NoteFile(
    val id: String,
    val title: String,
    val mime: String,
    val ext: String,
    val sizeKb: Long,
    val addedIso: String,
    val localPath: String)

private const val KEY_FILES = "study_pdfs"
private const val KEY_NOTES = "lesson_notes_text"
private const val KEY_NOTES_AT = "lesson_notes_at"
private const val FREE_FILE_CAP = 10
private val GalleryGroups = listOf("عکس", "ویدیو", "صوت", "PDF", "متن", "سایر")


internal fun readNoteFiles(store: LocalStore): List<NoteFile> = runCatching {
    val array = JSONArray(store.getString(KEY_FILES, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val name = o.optString("name")
            val path = o.optString("localPath")
            val ext = o.optString("ext").ifBlank { File(path).extension.ifBlank { "bin" } }
            add(
                NoteFile(
                    id = o.optString("id"),
                    title = o.optString("title").ifBlank { name.substringBeforeLast('.') },
                    mime = o.optString("mime").ifBlank { guessMime(ext) },
                    ext = ext,
                    sizeKb = o.optLong("sizeKb"),
                    addedIso = o.optString("addedIso"),
                    localPath = path))
        }
    }
}.getOrDefault(emptyList())

private fun writeNoteFiles(store: LocalStore, items: List<NoteFile>) {
    val array = JSONArray()
    items.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("name", "${item.title}.${item.ext}")
                .put("mime", item.mime)
                .put("ext", item.ext)
                .put("sizeKb", item.sizeKb)
                .put("addedIso", item.addedIso)
                .put("localPath", item.localPath)
                .put("reference", ""))
    }
    store.putString(KEY_FILES, array.toString())
}

private fun galleryDir(context: android.content.Context): File =
    File(context.filesDir, "notes_gallery").apply { mkdirs() }

// ------------------------------------------------------------------ نکات درسی

internal const val KEY_NOTES_ITEMS = "lesson_notes_items"

/** یک نکته‌ی ذخیره‌شده: عنوان + متن (دفتر ۸خط). */
internal data class LessonNote(
    val id: String,
    val title: String,
    val text: String,
    val updatedAt: Long)

private val DEFAULT_NOTE_TITLES = listOf(
    "نکته مهم",
    "فرمول‌ها",
    "اشتباه‌های من",
    "تمرین‌ها",
    "خلاصهٔ درس",
    "سؤال از معلم")

internal fun readNoteTitles(store: LocalStore): List<String> =
    (DEFAULT_NOTE_TITLES + parseNoteTitles(store.getString("note_titles", "[]"))).distinct()

internal fun writeNoteTitles(store: LocalStore, titles: List<String>) {
    store.putString("note_titles", noteTitlesJson(titles))
}

internal fun noteTitlesJson(titles: List<String>): String {
    val arr = JSONArray()
    titles.forEach { arr.put(it) }
    return arr.toString()
}

internal fun parseNoteTitles(raw: String): List<String> = runCatching {
    val arr = JSONArray(raw)
    (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
}.getOrDefault(emptyList())

internal fun readNotes(store: LocalStore): List<LessonNote> = runCatching {
    val arr = JSONArray(store.getString(KEY_NOTES_ITEMS, "[]"))
    buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(
                LessonNote(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    text = o.optString("text"),
                    updatedAt = o.optLong("updatedAt")))
        }
    }.sortedByDescending { it.updatedAt }
}.getOrDefault(emptyList())

internal fun writeNotes(store: LocalStore, notes: List<LessonNote>) {
    val arr = JSONArray()
    notes.forEach { n ->
        arr.put(
            JSONObject()
                .put("id", n.id)
                .put("title", n.title)
                .put("text", n.text)
                .put("updatedAt", n.updatedAt))
    }
    store.putString(KEY_NOTES_ITEMS, arr.toString())
}

/** متنِ دفتر نکات روی سرور: آرایه‌ی JSON (سازگار با نسخه‌ی قدیمی که متنِ ساده بود). */
internal fun encodeNotesForServer(notes: List<LessonNote>): String {
    val arr = JSONArray()
    notes.forEach { n ->
        arr.put(
            JSONObject()
                .put("id", n.id)
                .put("title", n.title)
                .put("text", n.text)
                .put("updatedAt", n.updatedAt))
    }
    return arr.toString()
}

internal fun decodeNotesFromServer(raw: String): List<LessonNote> {
    if (raw.isBlank()) return emptyList()
    val parsed = runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    LessonNote(
                        id = o.optString("id").ifBlank { "n_${System.currentTimeMillis()}_$i" },
                        title = o.optString("title"),
                        text = o.optString("text"),
                        updatedAt = o.optLong("updatedAt")))
            }
        }
    }.getOrNull()
    if (!parsed.isNullOrEmpty()) return parsed
    // نسخه‌ی قدیمی: یک متنِ ساده بدون عنوان
    return listOf(LessonNote("n_legacy", "بدون عنوان", raw, System.currentTimeMillis()))
}

private fun guessMime(ext: String): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
        ?: when (ext.lowercase()) {
            "pdf" -> "application/pdf"
            "txt", "md" -> "text/plain"
            "html", "htm" -> "text/html"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }

private fun isImageMime(mime: String) = mime.startsWith("image/")
private fun isVideoItem(item: NoteFile) = item.mime.startsWith("video/") || extOf(item) in setOf("mp4", "mkv", "webm", "3gp")
private fun isAudioItem(item: NoteFile) = item.mime.startsWith("audio/") || extOf(item) in setOf("mp3", "m4a", "aac", "ogg", "wav", "flac")

/** پسوندِ واقعی — از مسیرِ فایل هم اگر در فهرست خالی مانده باشد. */
private fun extOf(item: NoteFile): String =
    item.ext.ifBlank { File(item.localPath).extension }.lowercase()

private fun readHead(path: String, n: Int): ByteArray? = runCatching {
    val f = File(path)
    if (!f.exists()) return@runCatching null
    val buf = ByteArray(n)
    val read = f.inputStream().use { it.read(buf, 0, n) }
    if (read <= 0) null else buf.copyOf(read)
}.getOrNull()

private fun isHtmlItem(item: NoteFile): Boolean {
    val ext = extOf(item)
    return ext == "html" || ext == "htm" || item.mime.contains("html")
}

private fun isPdfItem(item: NoteFile): Boolean {
    if (extOf(item) == "pdf" || item.mime == "application/pdf") return true
    val head = readHead(item.localPath, 5) ?: return false
    return String(head, Charsets.US_ASCII).startsWith("%PDF-")
}

private fun isPlainTextItem(item: NoteFile): Boolean {
    if (isHtmlItem(item) || isPdfItem(item)) return false
    val ext = extOf(item)
    if (item.mime.startsWith("text/") || ext in setOf("txt", "md", "rtf")) return true
    // بو کردنِ محتوا: اگر بایتِ کنترلی نداشت، متن است.
    val head = readHead(item.localPath, 512) ?: return false
    if (head.isEmpty()) return false
    return head.all { b -> b >= 9.toByte() }
}

/** هر چه بشود داخل اپ نشان داد. */
private fun canOpenInternal(item: NoteFile): Boolean =
    isImageMime(item.mime) || isVideoItem(item) || isAudioItem(item) ||
        isHtmlItem(item) || isPlainTextItem(item) || isPdfItem(item)

private fun fileGroup(item: NoteFile): String = when {
    isImageMime(item.mime) -> "عکس"
    isVideoItem(item) -> "ویدیو"
    isAudioItem(item) -> "صوت"
    item.mime == "application/pdf" || item.ext.equals("pdf", true) -> "PDF"
    item.mime.startsWith("text/") || item.ext.lowercase() in setOf("txt", "md", "rtf", "html", "htm") -> "متن"
    else -> "سایر"
}

@Composable
fun PdfUploadScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = container.store
    val scope = rememberCoroutineScope()
    val paid = StudentProfileState.isPaid()

    var items by remember { mutableStateOf(readNoteFiles(store)) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf(store.getString(KEY_NOTES, "")) }
    var cropBmp by remember { mutableStateOf<Bitmap?>(null) }
    var titleDraft by remember { mutableStateOf<Pair<File, String>?>(null) }
    var titleText by remember { mutableStateOf("") }
    var needSubMsg by remember { mutableStateOf<String?>(null) }
    var imageAlbum by remember { mutableStateOf<List<NoteFile>?>(null) }
    var imageStart by remember { mutableIntStateOf(0) }
    var internalView by remember { mutableStateOf<NoteFile?>(null) }
    var noteItems by remember { mutableStateOf(readNotes(store)) }
    var noteTitles by remember { mutableStateOf(readNoteTitles(store)) }
    var noteTitle by remember { mutableStateOf("") }
    var editingNoteId by remember { mutableStateOf<String?>(null) }
    var notesOpen by remember { mutableStateOf(false) }
    var askDeleteNote by remember { mutableStateOf<LessonNote?>(null) }
    var pdfView by remember { mutableStateOf<NoteFile?>(null) }
    var editTarget by remember { mutableStateOf<NoteFile?>(null) }
    var exportTarget by remember { mutableStateOf<NoteFile?>(null) }

    fun openExternal(item: NoteFile) {
        runCatching {
            val file = File(item.localPath)
            if (!file.exists()) {
                notice = "فایل روی گوشی پیدا نشد."
                return
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = item.mime.ifBlank { guessMime(item.ext) }.ifBlank { "*/*" }
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(view, "باز کردن با"))
        }.onFailure { notice = "برنامه‌ای برای بازکردن این فایل پیدا نشد." }
    }

    fun openInternal(item: NoteFile) {
        when {
            isImageMime(item.mime) -> {
                val album = items.filter { isImageMime(it.mime) }
                imageStart = album.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                imageAlbum = album
            }
            isPdfItem(item) -> pdfView = item
            isHtmlItem(item) || isPlainTextItem(item) || isVideoItem(item) || isAudioItem(item) -> internalView = item
            else -> openExternal(item)
        }
    }

    LaunchedEffect(Unit) {
        val uid0 = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid0.isNotBlank()) {
            val remote = StateSync.pull(context, container.tables, uid0, StateSync.KEY_NOTE_TITLES)
            if (remote != null) {
                val merged = (readNoteTitles(store) + parseNoteTitles(remote.first)).distinct()
                if (merged != noteTitles) {
                    noteTitles = merged
                    writeNoteTitles(store, merged)
                }
            } else {
                StateSync.push(
                    ctx = context,
                    tables = container.tables,
                    uid = uid0,
                    key = StateSync.KEY_NOTE_TITLES,
                    payload = noteTitlesJson(noteTitles))
            }
        }
    }

    LaunchedEffect(Unit) {
        val uid = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isBlank()) return@LaunchedEffect
        when (val remote = container.tables.get(TableIds.LESSON_NOTES, "notes_$uid")) {
            is AppResult.Ok -> {
                val row = remote.value ?: return@LaunchedEffect
                val remoteText = row.string("payload").ifBlank { row.string("text") }
                val remoteAt = row.long("updatedAt")
                val localAt = store.getLong(KEY_NOTES_AT, 0L)
                if (remoteText.isNotBlank() && remoteAt >= localAt) {
                    val remoteNotes = decodeNotesFromServer(remoteText)
                    val localNotes = readNotes(store)
                    val merged = (remoteNotes + localNotes)
                        .distinctBy { it.id }
                        .sortedByDescending { it.updatedAt }
                    noteItems = merged
                    writeNotes(store, merged)
                    store.putLong(KEY_NOTES_AT, remoteAt)
                }
            }
            is AppResult.Err -> { }
        }
    }

    fun canAddMore(): Boolean {
        if (paid || items.size < FREE_FILE_CAP) return true
        needSubMsg = "تا ۱۰ فایل برای «مهمان همیار من» رایگان است. از فایل یازدهم اشتراک فعال لازم است."
        return false
    }

    fun persist(file: File, title: String, mime: String) {
        val ext = file.extension.ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(mime).orEmpty().ifBlank { "bin" } }
        val item = NoteFile(
            id = "nf_${System.currentTimeMillis()}",
            title = title.trim().ifBlank { "جزوه" },
            mime = mime,
            ext = ext,
            sizeKb = (file.length() / 1024).coerceAtLeast(1),
            addedIso = JalaliDate.todayIso(),
            localPath = file.absolutePath)
        items = listOf(item) + items
        writeNoteFiles(store, items)
        notice = "فقط روی همین گوشی ذخیره شد — فایل‌ها هرگز به سرور نمی‌روند."
    }

    // انتخاب‌گر عکسِ سیستمی (Photo Picker): مستقیم روی تصاویرِ گوشی می‌افتد و بدون
    // گرفتن «دسترسی کامل به تصاویر» کار می‌کند (فقط همان عکسِ انتخابی به دست اپ می‌رسد).
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (!canAddMore()) return@rememberLauncherForActivityResult
        cropBmp = loadOrientedBitmap(context, uri, maxSide = 2400)
        if (cropBmp == null) notice = "خواندن عکس ممکن نشد."
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked == null) return@rememberLauncherForActivityResult
        if (!canAddMore()) return@rememberLauncherForActivityResult
        busy = true
        notice = null
        scope.launch {
            val name = MediaFiles.displayName(context, picked) ?: "file.bin"
            val extGuess = File(name).extension
            val mimeRaw = context.contentResolver.getType(picked) ?: guessMime(extGuess)
            val mime = when {
                extGuess.equals("txt", true) -> "text/plain"
                extGuess.equals("html", true) || extGuess.equals("htm", true) -> "text/html"
                mimeRaw == "application/octet-stream" && extGuess.isNotBlank() -> guessMime(extGuess)
                else -> mimeRaw
            }
            if (isImageMime(mime)) {
                cropBmp = loadOrientedBitmap(context, picked, maxSide = 2400)
                busy = false
                return@launch
            }
            val copied = withContext(Dispatchers.IO) {
                runCatching {
                    val ext = extGuess.ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin" }
                    val target = File(galleryDir(context), "f_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(picked)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@runCatching null
                    target.takeIf { it.exists() }
                }.getOrNull()
            }
            busy = false
            if (copied == null) {
                notice = "کپی فایل ممکن نشد."
                return@launch
            }
            titleText = name.substringBeforeLast('.')
            titleDraft = copied to mime
        }
    }

    val exportCreate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val item = exportTarget
        exportTarget = null
        if (uri == null || item == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        File(item.localPath).inputStream().use { it.copyTo(output) }
                    } ?: error("output")
                    true
                }.getOrDefault(false)
            }
            notice = if (ok) "فایل در حافظهٔ انتخابی ذخیره شد." else "خروجی گرفتن ممکن نشد."
        }
    }

    val backupCreate = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        ZipOutputStream(out).use { zip ->
                            zip.putNextEntry(ZipEntry("manifest.json"))
                            zip.write(JSONArray().also { arr ->
                                items.forEach { it ->
                                    arr.put(
                                        JSONObject()
                                            .put("id", it.id).put("title", it.title).put("mime", it.mime)
                                            .put("ext", it.ext).put("addedIso", it.addedIso)
                                            .put("file", File(it.localPath).name))
                                }
                            }.toString().toByteArray())
                            zip.closeEntry()
                            items.forEach { item ->
                                val f = File(item.localPath)
                                if (!f.exists()) return@forEach
                                zip.putNextEntry(ZipEntry("files/${f.name}"))
                                FileInputStream(f).use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                    true
                }.getOrDefault(false)
            }
            notice = if (ok) "بکاپ روی گوشی ذخیره شد." else "بکاپ ساخته نشد."
        }
    }

    val restoreOpen = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val dir = galleryDir(context)
                    val restored = mutableListOf<NoteFile>()
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        ZipInputStream(input).use { zip ->
                            var entry = zip.nextEntry
                            var manifest: JSONArray? = null
                            val blobs = mutableMapOf<String, File>()
                            while (entry != null) {
                                if (entry.isDirectory) {
                                    entry = zip.nextEntry
                                    continue
                                }
                                if (entry.name == "manifest.json") {
                                    manifest = JSONArray(zip.readBytes().decodeToString())
                                } else {
                                    val dest = File(dir, "r_${System.currentTimeMillis()}_${File(entry.name).name}")
                                    dest.outputStream().use { zip.copyTo(it) }
                                    blobs[File(entry.name).name] = dest
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                            val arr = manifest ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val o = arr.getJSONObject(i)
                                val fname = o.optString("file")
                                val f = blobs[fname] ?: continue
                                restored += NoteFile(
                                    id = o.optString("id").ifBlank { "nf_${System.currentTimeMillis()}_$i" },
                                    title = o.optString("title").ifBlank { fname },
                                    mime = o.optString("mime").ifBlank { guessMime(o.optString("ext")) },
                                    ext = o.optString("ext").ifBlank { File(fname).extension },
                                    sizeKb = (f.length() / 1024).coerceAtLeast(1),
                                    addedIso = o.optString("addedIso").ifBlank { JalaliDate.todayIso() },
                                    localPath = f.absolutePath)
                            }
                        }
                    }
                    restored
                }.getOrDefault(emptyList())
            }
            if (result.isEmpty()) {
                notice = "بازگردانی چیزی پیدا نکرد."
            } else {
                items = (result + items).distinctBy { it.id }
                writeNoteFiles(store, items)
                notice = "${toPersianDigits(result.size.toString())} فایل بازگردانده شد."
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("جزوه‌های شخصی و آزمونی", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "ثبت نکات درسی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right)
            NoteTitlePicker(
                titles = noteTitles,
                selected = noteTitle,
                onSelect = { noteTitle = it },
                onAdd = { title ->
                    if (title.isNotBlank() && title !in noteTitles) {
                        noteTitles = noteTitles + title
                        writeNoteTitles(store, noteTitles)
                        scope.launch {
                            val uid = container.auth.cachedUserId()
                                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                            if (uid.isNotBlank()) {
                                StateSync.push(ctx = context, tables = container.tables, uid = uid, key = StateSync.KEY_NOTE_TITLES, payload = noteTitlesJson(noteTitles))
                            }
                        }
                    }
                    noteTitle = title
                })
            LinedNotesPaper(
                value = notes,
                onValueChange = { notes = it })
            PrimaryButton(if (editingNoteId == null) "ذخیره نکات و همگام با سرور" else "به‌روزرسانی نکته") {
                val now = System.currentTimeMillis()
                val title = noteTitle.trim().ifBlank { "بدون عنوان" }
                val id = editingNoteId ?: "n_${System.currentTimeMillis()}"
                val next = listOf(LessonNote(id, title, notes, now)) +
                    noteItems.filterNot { it.id == id }
                noteItems = next.sortedByDescending { it.updatedAt }
                writeNotes(store, noteItems)
                editingNoteId = null
                // پس از ذخیره، دفترچه و عنوان خالی می‌شوند.
                noteTitle = ""
                notes = ""
                scope.launch {
                    val uid = container.auth.cachedUserId()
                        ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                    if (uid.isBlank()) {
                        notice = "نکات روی دستگاه ذخیره شد. برای سینک با سرور وارد شو."
                        return@launch
                    }
                    val encoded = encodeNotesForServer(noteItems)
                    val payload = mapOf(
                        "userId" to uid,
                        "payload" to encoded,
                        "text" to encoded.take(7000),
                        "updatedAt" to now)
                    val perms = AppwriteClientProvider.ownerOnly(uid)
                    when (val saved = container.tables.upsert(TableIds.LESSON_NOTES, "notes_$uid", payload, perms)) {
                        is AppResult.Ok -> notice = "نکات ذخیره شد و با سرور همگام شد."
                        is AppResult.Err -> {
                            container.sync.enqueue(TableIds.LESSON_NOTES, "notes_$uid", payload)
                            runCatching { container.sync.pushAll() }
                            notice = "نکات روی دستگاه ماند؛ صف سینک: ${saved.error.userMessage}"
                        }
                    }
                }
            }
            if (editingNoteId != null) {
                TextButton(onClick = {
                    editingNoteId = null
                    noteTitle = ""
                    notes = ""
                }) { Text("لغو ویرایش") }
            }

            NotesAccordion(
                notes = noteItems,
                expanded = notesOpen,
                onToggle = { notesOpen = !notesOpen },
                onPick = { n ->
                    editingNoteId = n.id
                    noteTitle = n.title
                    notes = n.text
                    notice = "«${n.title}» در دفتر بارگذاری شد."
                },
                onDelete = { askDeleteNote = it })

            Text(
                "گالری جزوه — فقط همین گوشی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right)
            Text(
                "عکس جزوه و کتاب، متن، PDF یا هر فرمت دیگر. فایل‌ها هرگز به سرور نمی‌روند. مهمان همیار من تا ۱۰ فایل؛ از یازدهم اشتراک فعال. بکاپ و بازگردانی هم با اشتراک فعال.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right)
            Text(
                "الان ${toPersianDigits(items.size.toString())} فایل" +
                    if (!paid) " از ${toPersianDigits(FREE_FILE_CAP.toString())} سهمیهٔ مهمان" else "",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (!busy && canAddMore()) imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)) { Text("عکس + برش") }
                OutlinedButton(
                    onClick = { if (!busy && canAddMore()) filePicker.launch("*/*") },
                    modifier = Modifier.weight(1f)) { Text("هر فایل") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (!paid) needSubMsg = "بکاپ جزوه‌ها فقط با اشتراک فعال."
                        else backupCreate.launch("hamyar-joozve-${JalaliDate.todayIso()}.zip")
                    },
                    modifier = Modifier.weight(1f)) { Text("بکاپ روی گوشی") }
                OutlinedButton(
                    onClick = {
                        if (!paid) needSubMsg = "بازگردانی جزوه‌ها فقط با اشتراک فعال."
                        else restoreOpen.launch(arrayOf("application/zip", "*/*"))
                    },
                    modifier = Modifier.weight(1f)) { Text("بازگردانی") }
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (busy) Text("در حال ذخیره…", style = MaterialTheme.typography.bodySmall)

            if (items.isEmpty()) {
                Text("گالری خالی است.", style = MaterialTheme.typography.bodySmall)
            }
            GalleryGroups.forEach { group ->
                val groupItems = items.filter { fileGroup(it) == group }
                if (groupItems.isEmpty()) return@forEach
                Text(
                    "$group · ${toPersianDigits(groupItems.size.toString())}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val span = when {
                        maxWidth.value >= 900f -> 4
                        maxWidth.value >= 610f -> 3
                        else -> 2
                    }
                    groupItems.chunked(span).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { item ->
                                GalleryTile(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onOpen = { if (canOpenInternal(item)) openInternal(item) else openExternal(item) },
                                    onExternal = { openExternal(item) },
                                    onExport = { exportTarget = item; exportCreate.launch(item.title + "." + item.ext) },
                                    onDelete = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { runCatching { File(item.localPath).delete() } }
                                            items = items.filterNot { it.id == item.id }
                                            writeNoteFiles(store, items)
                                            notice = "از گالری حذف شد."
                                        }
                                    },
                                )
                            }
                            for (i in row.size until span) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }            Spacer(Modifier.height(12.dp))
        }
    }

    cropBmp?.let { bmp ->
        ImageRectCropDialog(
            bitmap = bmp,
            onCancel = { cropBmp = null },
            onCropped = { out ->
                scope.launch {
                    val replace = editTarget
                    val saved = withContext(Dispatchers.IO) {
                        val (scaled, q) = compressReadableJpeg(out)
                        val target = if (replace != null) File(replace.localPath)
                        else File(galleryDir(context), "img_${System.currentTimeMillis()}.jpg")
                        runCatching {
                            target.outputStream().use { os -> scaled.compress(Bitmap.CompressFormat.JPEG, q, os) }
                        }
                        target.takeIf { it.exists() && it.length() > 0 }
                    }
                    cropBmp = null
                    if (saved == null) {
                        notice = "ذخیرهٔ عکس ممکن نشد."
                    } else if (replace != null) {
                        // همان فایل جایگزین شد — فقط اندازه در فهرست به‌روز می‌شود.
                        items = items.map { if (it.id == replace.id) it.copy(sizeKb = (saved.length() / 1024).coerceAtLeast(1)) else it }
                        writeNoteFiles(store, items)
                        editTarget = null
                        notice = "ویرایش عکس ذخیره شد."
                    } else {
                        titleText = "عکس جزوه"
                        titleDraft = saved to "image/jpeg"
                    }
                }
            })
    }

    imageAlbum?.let { album ->
        ImageGalleryPager(
            album = album,
            start = imageStart,
            onClose = { imageAlbum = null },
            onDelete = { gone ->
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { File(gone.localPath).delete() } }
                    items = items.filterNot { it.id == gone.id }
                    writeNoteFiles(store, items)
                    val next = album.filterNot { it.id == gone.id }
                    imageAlbum = next.ifEmpty { null }
                    notice = "از گالری حذف شد."
                }
            },
            onExternal = { target -> openExternal(target) },
            onExport = { target -> exportTarget = target; exportCreate.launch(target.title + "." + target.ext) },
            onEdit = { target ->
                // ویرایش با همان اسکریپتِ کات و چرخش؛ خروجی جایگزینِ همان فایل می‌شود.
                imageAlbum = null
                scope.launch {
                    val bmp = withContext(Dispatchers.IO) {
                        runCatching { BitmapFactory.decodeFile(target.localPath) }.getOrNull()
                    }
                    if (bmp == null) {
                        notice = "خواندن عکس ممکن نشد."
                    } else {
                        editTarget = target
                        cropBmp = bmp
                    }
                }
            })
    }

    titleDraft?.let { (file, mime) ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("عنوان فایل") },
            text = {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("عنوان + فرمت در گالری دیده می‌شود") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    persist(file, titleText, mime)
                    titleDraft = null
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = {
                    file.delete()
                    titleDraft = null
                }) { Text("انصراف") }
            })
    }

    pdfView?.let { item ->
        Dialog(
            onDismissRequest = { pdfView = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right)
                Spacer(Modifier.height(8.dp))
                InternalPdfViewer(file = File(item.localPath), modifier = Modifier.weight(1f).fillMaxWidth())
                OutlinedButton(onClick = { pdfView = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("بستن")
                }
            }
        }
    }

    askDeleteNote?.let { n ->
        AlertDialog(
            onDismissRequest = { askDeleteNote = null },
            title = { Text("حذف نکته؟") },
            text = { Text("«${n.title}» برای همیشه از دفتر و سرور پاک می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    val next = noteItems.filterNot { it.id == n.id }
                    noteItems = next
                    writeNotes(store, next)
                    if (editingNoteId == n.id) {
                        editingNoteId = null
                        noteTitle = ""
                        notes = ""
                    }
                    askDeleteNote = null
                    scope.launch {
                        val uid = container.auth.cachedUserId()
                            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                        if (uid.isBlank()) return@launch
                        container.tables.upsert(
                            TableIds.LESSON_NOTES,
                            "notes_$uid",
                            mapOf(
                                "userId" to uid,
                                "text" to encodeNotesForServer(next),
                                "updatedAt" to System.currentTimeMillis()),
                            AppwriteClientProvider.ownerOnly(uid))
                    }
                    notice = "نکته حذف شد."
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { askDeleteNote = null }) { Text("انصراف") } })
    }

    needSubMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { needSubMsg = null },
            title = { Text("نیاز به اشتراک فعال") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { needSubMsg = null }) { Text("متوجه شدم") } })
    }

    internalView?.let { item ->
        val internalWebRef = remember(item.id) { arrayOfNulls<WebView>(1) }
        ManagedWebMediaEffect { internalWebRef[0] }
        Dialog(
            onDismissRequest = { internalView = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val body = remember(item.localPath) {
                    if (isHtmlItem(item) || isPlainTextItem(item)) {
                        runCatching { File(item.localPath).readText(Charsets.UTF_8) }.getOrDefault("خواندن فایل ممکن نشد.")
                    } else ""
                }
                if (isVideoItem(item) || isAudioItem(item)) {
                    NotebookMediaViewer(item, Modifier.weight(1f).fillMaxWidth())
                } else if (isHtmlItem(item)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, url: String) {
                                        view.bindManagedMediaLifecycle()
                                    }
                                }
                                settings.javaScriptEnabled = true
                                settings.allowFileAccess = true
                                installManagedMediaLifecycle()
                                internalWebRef[0] = this
                                loadDataWithBaseURL(null, body, "text/html", "utf-8", null)
                            }
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        onRelease = {
                            it.stopManagedMedia()
                            if (internalWebRef[0] === it) internalWebRef[0] = null
                            it.destroy()
                        })
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedButton(onClick = { internalView = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("بستن")
                }
            }
        }
    }
}

/**
 * دفتر نکات (دفتر ۸خطِ وکتور با قاب) — فونت بدخط و اندازه‌ی همسان با خط‌ها.
 */
@Composable
private fun NotebookMediaViewer(item: NoteFile, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playback = remember(item.id) { com.hamyareman.ir.platform.feature.playback.PlaybackController(context) }
    val state by playback.state.collectAsState()
    LaunchedEffect(item.id) {
        if (playback.connect()) {
            val media = androidx.media3.common.MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(android.net.Uri.fromFile(File(item.localPath)))
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(item.title).build())
                .build()
            playback.setMediaItems(listOf(media), 0)
        }
    }
    DisposableEffect(playback) {
        onDispose { runCatching { playback.stop() }; playback.release() }
    }
    Column(modifier.fillMaxSize().background(Color.Black), horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(
            factory = { ctx -> androidx.media3.ui.PlayerView(ctx).apply { useController = true; player = playback.asPlayer() } },
            update = { it.player = playback.asPlayer() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onRelease = { it.player = null },
        )
        Text(if (state.playing) "در حال پخش" else "مکث", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LinedNotesPaper(value: String, onValueChange: (String) -> Unit) {
    LinedNotebookInput(value = value, onValueChange = onValueChange)
}

/**
 * آکاردیونِ نکات — پیش‌فرض بسته؛ نکته‌ها بر حسب عنوان دسته‌بندی می‌شوند.
 * لمسِ هر نکته آن را در همان دفترِ بالا بار می‌کند.
 */
@Composable
private fun NotesAccordion(
    notes: List<LessonNote>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (LessonNote) -> Unit,
    onDelete: (LessonNote) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    "نکته‌های ذخیره‌شده (${toPersianDigits(notes.size.toString())})",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Right)
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notes.isEmpty()) {
                        Text(
                            "هنوز نکته‌ای ذخیره نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right)
                    }
                    notes.groupBy { it.title.ifBlank { "بدون عنوان" } }.forEach { (title, group) ->
                        Text(
                            "$title · ${toPersianDigits(group.size.toString())}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right)
                        group.forEach { n ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onDelete(n) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "حذف نکته",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { onPick(n) }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "ویرایش نکته")
                                }
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .clickable { onPick(n) },
                                    horizontalAlignment = Alignment.End) {
                                    Text(
                                        n.text.take(60),
                                        maxLines = 2,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Right)
                                    Text(
                                        JalaliDate.toJalali(
                                            java.time.Instant.ofEpochMilli(n.updatedAt)
                                                .atZone(JalaliDate.TEHRAN).toLocalDate().toString())?.fa ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * نمایشگر عکس‌های گالری — زوم دو انگشتی + دکمه‌ی ویرایش (همان اسکریپت کات و چرخش).
 */
@Composable
private fun ImageGalleryPager(
    album: List<NoteFile>,
    start: Int,
    onClose: () -> Unit,
    onDelete: (NoteFile) -> Unit,
    onEdit: (NoteFile) -> Unit,
    onExternal: (NoteFile) -> Unit,
    onExport: (NoteFile) -> Unit) {
    val pager = rememberPagerState(
        initialPage = start.coerceIn(0, (album.size - 1).coerceAtLeast(0)),
        pageCount = { album.size.coerceAtLeast(1) })
    // صفحه‌ای که کاربر در آن زوم کرده است؛ تا وقتی زوم است، سوایپِ گالری کار نمی‌کند
    // تا وسطِ دیدنِ جزئیاتِ عکس، ناخواسته به عکسِ بعدی نپرد.
    var zoomedPage by remember { mutableIntStateOf(-1) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = zoomedPage != pager.currentPage,
                modifier = Modifier.fillMaxSize()) { page ->
                val item = album.getOrNull(page)
                val bmp = remember(item?.localPath) {
                    item?.localPath?.let { PdfSafe.decodeFileCapped(it, maxSide = 1600) }
                }
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                    if (bmp != null) {
                        ZoomablePdfPage(
                            bitmap = bmp,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            onZoomed = { z -> zoomedPage = if (z) page else -1 })
                    } else {
                        Text("خوانده نشد", color = Color.White)
                    }
                }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                val cur = album.getOrNull(pager.currentPage)
                Text(cur?.title.orEmpty(), color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "${toPersianDigits((pager.currentPage + 1).toString())} از ${toPersianDigits(album.size.toString())}",
                    color = Color.White.copy(alpha = 0.8f))
                Text(
                    "دو انگشت برای زوم؛ لمس برای برگشت",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onClose) { Text("بستن") }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = { cur?.let(onExternal) }) { Icon(Icons.Default.OpenInNew, contentDescription = "با برنامهٔ دیگر") }
                        IconButton(onClick = { cur?.let(onExport) }) { Icon(Icons.Default.Download, contentDescription = "خروجی") }
                        IconButton(onClick = { cur?.let(onEdit) }) { Icon(Icons.Outlined.Edit, contentDescription = "ویرایش") }
                        IconButton(onClick = { cur?.let(onDelete) }) { Icon(Icons.Outlined.Delete, contentDescription = "حذف") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTile(
    item: NoteFile,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onExternal: () -> Unit = {},
    onExport: () -> Unit = {}) {
    Column(modifier.clickable(onClick = onOpen)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center) {
            GalleryThumb(item = item)
        }
        Text(
            item.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right)
        Text(
            "${item.ext.uppercase()} · ${toPersianDigits(item.sizeKb.toString())}کب",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (canOpenInternal(item)) {
                IconButton(onClick = onExternal) { Icon(Icons.Default.OpenInNew, contentDescription = "با برنامهٔ دیگر") }
            }
            IconButton(onClick = onExport) { Icon(Icons.Default.Download, contentDescription = "خروجی به حافظه") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "حذف") }
        }
    }
}

/**
 * نمایشگر PDFِ داخل اپ — صفحه‌به‌صفحه با PdfRenderer (بدون نیاز به برنامه‌ی بیرونی).
 */
@Composable
private fun InternalPdfViewer(file: File, modifier: Modifier = Modifier) {
    var pages by remember(file.absolutePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var error by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    LaunchedEffect(file.absolutePath) {
        withContext(Dispatchers.IO) {
            val out = mutableListOf<Bitmap>()
            val renderer = runCatching {
                android.graphics.pdf.PdfRenderer(
                    android.os.ParcelFileDescriptor.open(
                        file,
                        android.os.ParcelFileDescriptor.MODE_READ_ONLY))
            }.getOrNull()
            if (renderer == null) {
                error = "این PDF روی گوشی باز نشد."
                return@withContext
            }
            runCatching {
                val count = renderer.pageCount.coerceAtMost(40)
                for (i in 0 until count) {
                    val page = renderer.openPage(i)
                    val bmp = PdfSafe.renderPage(page, maxW = 900, maxH = 1400)
                    page.close()
                    if (bmp != null) out += bmp
                }
            }
            runCatching { renderer.close() }
            pages = out
            if (out.isEmpty()) error = "صفحه‌ای از این PDF خوانده نشد."
        }
    }
    Box(modifier.fillMaxSize()) {
        when {
            !error.isNullOrBlank() -> Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center)
            pages.isEmpty() -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> {
                var idx by remember { mutableIntStateOf(0) }
                val i = idx.coerceIn(0, pages.lastIndex)
                Column(Modifier.fillMaxSize()) {
                    ZoomablePdfPage(
                        bitmap = pages[i],
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp))
                    Text(
                        "${toPersianDigits((i + 1).toString())} از ${toPersianDigits(pages.size.toString())}",
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp),
                        style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { idx = (i - 1).coerceAtLeast(0) },
                            enabled = i > 0,
                            modifier = Modifier.weight(1f)) { Text("صفحه قبل") }
                        OutlinedButton(
                            onClick = { idx = (i + 1).coerceAtMost(pages.lastIndex) },
                            enabled = i < pages.lastIndex,
                            modifier = Modifier.weight(1f)) { Text("صفحه بعد") }
                    }
                }
            }
        }
    }
}

/** تامبنیل متنی/پی‌دی‌اف برای کادرِ گالری. */
@Composable
private fun GalleryThumb(item: NoteFile) {
    val bmp = remember(item.localPath) {
        when {
            isImageMime(item.mime) -> runCatching {
                BitmapFactory.decodeFile(item.localPath, BitmapFactory.Options().apply { inSampleSize = 4 })
            }.getOrNull()
            isPdfItem(item) -> runCatching {
                val renderer = android.graphics.pdf.PdfRenderer(
                    android.os.ParcelFileDescriptor.open(
                        File(item.localPath),
                        android.os.ParcelFileDescriptor.MODE_READ_ONLY))
                val page = renderer.openPage(0)
                val scale = 320f / page.width.coerceAtLeast(1)
                val out = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888)
                page.render(out, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                out
            }.getOrNull()
            else -> null
        }
    }
    val previewText = remember(item.localPath) {
        if (bmp == null && isPlainTextItem(item)) {
            runCatching { File(item.localPath).bufferedReader().use { it.readText().take(200) } }.getOrDefault("")
        } else ""
    }
    when {
        bmp != null -> Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop)
        previewText.isNotBlank() -> Text(
            previewText,
            style = MaterialTheme.typography.labelSmall.copy(
                textAlign = TextAlign.Right),
            maxLines = 8,
            modifier = Modifier.fillMaxSize().padding(6.dp))
        isVideoItem(item) -> Text("🎬", style = MaterialTheme.typography.headlineLarge)
        isAudioItem(item) -> Text("🎧", style = MaterialTheme.typography.headlineLarge)
        else -> Text(item.ext.uppercase().ifBlank { "FILE" }, fontWeight = FontWeight.Bold)
    }
}

/**
 * انتخاب عنوانِ نکته: چند عنوانِ آماده + گزینهٔ آخر برای ساختِ عنوانِ جدید.
 */
@Composable
private fun NoteTitlePicker(
    titles: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onAdd: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    selected.ifBlank { "انتخاب عنوان نکته" },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                titles.forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                        onClick = { onSelect(t); menu = false })
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            "＋ افزودن عنوان جدید…",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right,
                            fontWeight = FontWeight.Bold)
                    },
                    onClick = { menu = false; adding = true })
            }
        }
        if (adding) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("عنوان جدید") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { adding = false; draft = "" },
                    modifier = Modifier.weight(1f)) { Text("انصراف") }
                OutlinedButton(
                    onClick = {
                        val t = draft.trim()
                        if (t.isNotBlank()) onAdd(t)
                        adding = false
                        draft = ""
                    },
                    modifier = Modifier.weight(1f)) { Text("افزودن") }
            }
        }
    }
}
