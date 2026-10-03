package com.hamyareman.ir.ui.study

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.feature.playback.PlaybackController
import com.hamyareman.ir.platform.core.appwrite.HmkWebViewClient
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

internal enum class FreeBookType { TEXT, AUDIO }

internal data class FreeBookChapter(
    val id: String,
    val title: String,
    val audioKey: String,
    val startMs: Long,
    val durationMs: Long,
)

internal data class FreeBook(
    val id: String,
    val type: FreeBookType,
    val title: String,
    val author: String,
    val description: String,
    val coverKey: String,
    val htmlKey: String,
    val audioKey: String,
    val chapters: List<FreeBookChapter>,
)

internal data class FreeBookState(
    val status: String = "WANT",
    val positionMs: Long = 0L,
)

internal data class BookComment(
    val id: String,
    val bookId: String,
    val userId: String,
    val displayName: String,
    val body: String,
    val parentId: String,
    val createdAtMs: Long,
    val likes: Int,
    val dislikes: Int,
)

internal object PersianCommentFilter {
    private val builtin = setOf(
        "احمق", "احمقانه", "کودن", "بی شعور", "بیشعور", "بی‌عرضه", "بی عرضه",
        "عوضی", "آشغال", "کثافت", "حرومزاده", "حرامزاده", "گمشو", "خفه شو",
        "مزخرف", "کسخل", "دیوث", "fuck", "shit", "bitch", "asshole",
    )

    fun normalize(value: String): String {
        val nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase()
        return buildString(nfkc.length) {
            nfkc.forEach { ch ->
                when (ch) {
                    'ي' -> append('ی')
                    'ك' -> append('ک')
                    'ۀ' -> append('ه')
                    'ة' -> append('ت')
                    '۰', '٠' -> append('0')
                    '۱', '١' -> append('1')
                    '۲', '٢' -> append('2')
                    '۳', '٣' -> append('3')
                    '۴', '٤' -> append('4')
                    '۵', '٥' -> append('5')
                    '۶', '٦' -> append('6')
                    '۷', '٧' -> append('7')
                    '۸', '٨' -> append('8')
                    '۹', '٩' -> append('9')
                    'ـ', '\u200c', '\u200d', '\u200b', '\ufeff' -> Unit
                    ' ', '\t', '\n', '.', ',', '؛', '!', '?', '-', '_', '/', '\\', '|',
                    '(', ')', '[', ']', '{', '}' -> Unit
                    '@' -> append('a')
                    '$' -> append('s')
                    else -> if (!ch.isWhitespace()) append(ch)
                }
            }
        }
    }

    fun allowed(value: String, extraTerms: Set<String> = emptySet()): Boolean {
        val n = normalize(value)
        if (n.length < 2 || n.length > 4000) return false
        val terms = builtin + extraTerms.map(::normalize)
        return terms.none { it.isNotBlank() && n.contains(normalize(it)) }
    }
}

internal class FreeReadingRepository(
    private val tables: com.hamyareman.ir.platform.core.appwrite.TablesDbService,
    private val sync: com.hamyareman.ir.platform.core.sync.SyncEngine,
    private val userId: () -> String,
) {
    suspend fun books(type: FreeBookType? = null): List<FreeBook> {
        val q = mutableListOf("equal(\"published\",[1])", "orderAsc(\"sortOrder\")", "limit(100)")
        if (type != null) q += "equal(\"type\",[\"" + type.name + "\"])"
        return when (val r = tables.list(TableIds.FREE_BOOKS, q)) {
            is AppResult.Ok -> r.value.mapNotNull(::parseBook)
            else -> emptyList()
        }
    }

    suspend fun state(bookId: String): FreeBookState {
        val uid = userId().ifBlank { return FreeBookState() }
        val row = rowId("state", uid + ":" + bookId)
        return when (val r = tables.get(TableIds.FREE_BOOK_USER_STATE, row)) {
            is AppResult.Ok -> r.value?.payload?.let {
                FreeBookState(
                    status = it["status"]?.toString().orEmpty().ifBlank { "WANT" },
                    positionMs = it["positionMs"]?.toString()?.toLongOrNull() ?: 0L,
                )
            } ?: FreeBookState()
            else -> FreeBookState()
        }
    }

    fun saveState(bookId: String, status: String, positionMs: Long) {
        val uid = userId().ifBlank { return }
        sync.enqueue(
            TableIds.FREE_BOOK_USER_STATE,
            rowId("state", uid + ":" + bookId),
            mapOf(
                "userId" to uid,
                "bookId" to bookId,
                "status" to status,
                "positionMs" to positionMs.coerceAtLeast(0L),
                "updatedAtMs" to System.currentTimeMillis(),
            ),
        )
    }

    fun requestBook(title: String, author: String, type: FreeBookType, note: String) {
        val uid = userId().ifBlank { return }
        val now = System.currentTimeMillis()
        sync.enqueue(
            TableIds.FREE_BOOK_REQUESTS,
            rowId("request", uid + ":" + now),
            mapOf(
                "userId" to uid,
                "title" to title.trim(),
                "author" to author.trim(),
                "type" to type.name,
                "note" to note.trim(),
                "createdAtMs" to now,
                "status" to "PENDING",
            ),
        )
    }

    suspend fun comments(bookId: String): List<BookComment> {
        val q = listOf(
            "equal(\"bookId\",[ \"" + bookId + "\"])".replace("[ \"", "[\""),
            "equal(\"status\",[\"APPROVED\"])",
            "orderDesc(\"createdAtMs\")",
            "limit(100)",
        )
        return when (val r = tables.list(TableIds.BOOK_COMMENTS, q)) {
            is AppResult.Ok -> r.value.map { row ->
                val p = row.payload
                BookComment(
                    id = row.id,
                    bookId = p["bookId"]?.toString().orEmpty(),
                    userId = p["userId"]?.toString().orEmpty(),
                    displayName = p["displayName"]?.toString().orEmpty().ifBlank { "کاربر" },
                    body = p["body"]?.toString().orEmpty(),
                    parentId = p["parentId"]?.toString().orEmpty(),
                    createdAtMs = p["createdAtMs"]?.toString()?.toLongOrNull() ?: 0L,
                    likes = p["likes"]?.toString()?.toIntOrNull() ?: 0,
                    dislikes = p["dislikes"]?.toString()?.toIntOrNull() ?: 0,
                )
            }
            else -> emptyList()
        }
    }

    fun addComment(bookId: String, body: String, parentId: String = "", extraTerms: Set<String> = emptySet()): Boolean {
        if (!PersianCommentFilter.allowed(body, extraTerms)) return false
        val uid = userId().ifBlank { return false }
        val id = rowId("comment", uid + ":" + bookId + ":" + System.currentTimeMillis() + ":" + body.hashCode())
        sync.enqueue(
            TableIds.BOOK_COMMENTS,
            id,
            mapOf(
                "commentId" to id,
                "bookId" to bookId,
                "userId" to uid,
                "displayName" to "کاربر",
                "body" to body.trim(),
                "parentId" to parentId,
                "createdAtMs" to System.currentTimeMillis(),
                "status" to "APPROVED",
                "likes" to 0,
                "dislikes" to 0,
            ),
        )
        return true
    }

    fun react(commentId: String, reaction: String) {
        val uid = userId().ifBlank { return }
        sync.enqueue(
            TableIds.BOOK_COMMENT_REACTIONS,
            rowId("reaction", uid + ":" + commentId),
            mapOf(
                "userId" to uid,
                "commentId" to commentId,
                "reaction" to reaction,
                "updatedAtMs" to System.currentTimeMillis(),
            ),
        )
    }

    suspend fun moderationTerms(): Set<String> = when (
        val r = tables.list(TableIds.MODERATION_TERMS, listOf("equal(\"active\",[1])", "limit(500)"))
    ) {
        is AppResult.Ok -> r.value.mapNotNull {
            it.payload["term"]?.toString()?.takeIf(String::isNotBlank)
        }.toSet()
        else -> emptySet()
    }

    private fun parseBook(row: com.hamyareman.ir.platform.core.appwrite.TableRow): FreeBook? {
        val p = row.payload
        val type = runCatching {
            FreeBookType.valueOf(p["type"]?.toString().orEmpty().uppercase(Locale.ROOT))
        }.getOrNull() ?: return null
        val chapters = runCatching {
            val arr = JSONArray(p["chaptersJson"]?.toString().orEmpty())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        FreeBookChapter(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            audioKey = o.optString("audioKey"),
                            startMs = o.optLong("startMs", 0L),
                            durationMs = o.optLong("durationMs", 0L),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
        return FreeBook(
            id = row.id.ifBlank { p["bookId"]?.toString().orEmpty() },
            type = type,
            title = p["title"]?.toString().orEmpty(),
            author = p["author"]?.toString().orEmpty(),
            description = p["description"]?.toString().orEmpty(),
            coverKey = p["coverKey"]?.toString().orEmpty(),
            htmlKey = p["htmlKey"]?.toString().orEmpty(),
            audioKey = p["audioKey"]?.toString().orEmpty(),
            chapters = chapters,
        ).takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    private fun rowId(kind: String, raw: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((kind + ":" + raw).toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)

    companion object {
        fun absoluteKey(key: String): String {
            val raw = key.trim()
            if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
            return "https://c539776.parspack.net/" + raw.trimStart('/')
        }
    }
}

@Composable
internal fun FreeReadingCatalogScreen() {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val repo = remember(container) {
        FreeReadingRepository(container.tables, container.sync) {
            container.auth.cachedUserId().orEmpty()
        }
    }
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf<FreeBookType?>(null) }
    var books by remember { mutableStateOf<List<FreeBook>>(emptyList()) }
    var selected by remember { mutableStateOf<FreeBook?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") }
    var requestOpen by remember { mutableStateOf(false) }
    var requestType by remember { mutableStateOf(FreeBookType.TEXT) }
    var requestTitle by remember { mutableStateOf("") }
    var requestAuthor by remember { mutableStateOf("") }
    var requestNote by remember { mutableStateOf("") }
    var states by remember { mutableStateOf<Map<String, FreeBookState>>(emptyMap()) }

    LaunchedEffect(type) {
        books = withContext(Dispatchers.IO) { repo.books(type) }
        val next = mutableMapOf<String, FreeBookState>()
        books.forEach { next[it.id] = repo.state(it.id) }
        states = next
    }

    if (selected != null) {
        FreeReadingBookScreen(
            book = selected!!,
            initialState = states[selected!!.id] ?: FreeBookState(),
            repo = repo,
            onBack = { selected = null },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("کتابخانه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { requestOpen = true }) {
                Icon(Icons.Outlined.Send, contentDescription = "درخواست کتاب")
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(selected = type == null, onClick = { type = null }, label = { Text("همه") })
            FilterChip(selected = type == FreeBookType.TEXT, onClick = { type = FreeBookType.TEXT }, label = { Text("متنی") })
            FilterChip(selected = type == FreeBookType.AUDIO, onClick = { type = FreeBookType.AUDIO }, label = { Text("صوتی") })
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "ALL" to "همه",
                "WANT" to "می‌خواهم",
                "READING" to "در حال مطالعه",
                "DONE" to "خوانده‌شده",
                "ARCHIVE" to "آرشیو",
            ).forEach { (id, label) ->
                FilterChip(selected = statusFilter == id, onClick = { statusFilter = id }, label = { Text(label) })
            }
        }
        if (books.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز کتابی در سرور منتشر نشده است.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    books.filter {
                        statusFilter == "ALL" || (states[it.id]?.status ?: "WANT") == statusFilter
                    },
                    key = { it.id },
                ) { book ->
                    FreeBookCard(
                        book = book,
                        state = states[book.id] ?: FreeBookState(),
                        onOpen = { selected = book },
                        onStatus = { newStatus ->
                            states = states + (book.id to (states[book.id] ?: FreeBookState()).copy(status = newStatus))
                            repo.saveState(book.id, newStatus, states[book.id]?.positionMs ?: 0L)
                        },
                    )
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    if (requestOpen) {
        AlertDialog(
            onDismissRequest = { requestOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (requestTitle.isNotBlank()) {
                            repo.requestBook(requestTitle, requestAuthor, requestType, requestNote)
                            requestTitle = ""
                            requestAuthor = ""
                            requestNote = ""
                            requestOpen = false
                        }
                    },
                ) { Text("ارسال درخواست") }
            },
            dismissButton = { TextButton(onClick = { requestOpen = false }) { Text("لغو") } },
            title = { Text("درخواست کتاب از همیار") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = requestType == FreeBookType.TEXT, onClick = { requestType = FreeBookType.TEXT })
                        Text("کتاب متنی")
                        RadioButton(selected = requestType == FreeBookType.AUDIO, onClick = { requestType = FreeBookType.AUDIO })
                        Text("کتاب صوتی")
                    }
                    OutlinedTextField(requestTitle, { requestTitle = it.take(256) }, label = { Text("عنوان") })
                    OutlinedTextField(requestAuthor, { requestAuthor = it.take(256) }, label = { Text("نویسنده") })
                    OutlinedTextField(requestNote, { requestNote = it.take(2048) }, label = { Text("توضیح") })
                }
            },
        )
    }
}

@Composable
private fun FreeBookCard(
    book: FreeBook,
    state: FreeBookState,
    onOpen: () -> Unit,
    onStatus: (String) -> Unit,
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.tween(1),
        label = "no-op",
    )
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (book.coverKey.isNotBlank()) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (book.type == FreeBookType.AUDIO) Icons.Outlined.Headphones else Icons.Outlined.Book, null)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.bodySmall)
                Text(
                    book.description.ifBlank { if (book.type == FreeBookType.AUDIO) "کتاب صوتی" else "کتاب متنی" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Text(
                    statusLabel(state.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                if (book.type == FreeBookType.AUDIO) Icons.Outlined.Headphones else Icons.Outlined.MenuBook,
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "READING" -> "در حال مطالعه"
    "DONE" -> "خوانده‌شده"
    "ARCHIVE" -> "آرشیو شخصی"
    else -> "می‌خواهم"
}

@Composable
private fun FreeReadingBookScreen(
    book: FreeBook,
    initialState: FreeBookState,
    repo: FreeReadingRepository,
    onBack: () -> Unit,
) {
    var status by remember(book.id) { mutableStateOf(initialState.status) }
    if (book.type == FreeBookType.TEXT) {
        FreeTextReader(book, initialState, repo) { newStatus, position ->
            status = newStatus
            repo.saveState(book.id, newStatus, position)
        }
    } else {
        FreeAudioReader(book, initialState, repo) { newStatus, position ->
            status = newStatus
            repo.saveState(book.id, newStatus, position)
        }
    }
}

@Composable
private fun FreeTextReader(
    book: FreeBook,
    initial: FreeBookState,
    repo: FreeReadingRepository,
    onState: (String, Long) -> Unit,
) {
    val context = LocalContext.current
    var reading by remember(book.id) { mutableStateOf(initial.status == "WANT") }
    var scrollY by remember(book.id) { mutableLongStateOf(initial.positionMs) }
    LaunchedEffect(book.id) {
        delay(800)
        reading = true
        onState("READING", scrollY)
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    webViewClient = HmkWebViewClient(ctx.applicationContext, HmkWebViewClient.bucketHost())
                    loadUrl(book.htmlUrl)
                    postDelayed({ scrollTo(0, initial.positionMs.toInt().coerceAtLeast(0)) }, 700)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { web ->
                web.postDelayed({
                    val y = web.scrollY.toLong()
                    if (kotlin.math.abs(y - scrollY) > 40L) {
                        scrollY = y
                        onState("READING", scrollY)
                    }
                }, 1200)
            },
            onRelease = { web -> web.stopLoading(); web.destroy() },
        )
    }
}

@Composable
private fun FreeAudioReader(
    book: FreeBook,
    initial: FreeBookState,
    repo: FreeReadingRepository,
    onState: (String, Long) -> Unit,
) {
    val context = LocalContext.current
    val playback = remember(book.id) { PlaybackController(context) }
    val state by playback.state.collectAsState()
    val scope = rememberCoroutineScope()
    var currentIndex by remember(book.id) { mutableStateOf(0) }
    var sleepEnds by remember(book.id) { mutableLongStateOf(0L) }
    var sleepMinutes by remember(book.id) { mutableStateOf("") }
    var background by remember(book.id) { mutableStateOf(true) }
    val currentChapter = book.chapters.getOrNull(currentIndex)
    val currentUrl = if (currentChapter?.audioKey?.isNotBlank() == true) {
        FreeReadingRepository.absoluteKey(currentChapter.audioKey)
    } else {
        FreeReadingRepository.absoluteKey(book.audioKey)
    }

    LaunchedEffect(book.id) {
        if (playback.connect()) {
            playback.setMedia(
                currentUrl,
                book.title + if (currentChapter != null) " · " + currentChapter.title else "",
                initial.positionMs,
            )
        }
    }

    LaunchedEffect(state.playing, sleepEnds) {
        if (!state.playing) return@LaunchedEffect
        while (sleepEnds > 0L && System.currentTimeMillis() < sleepEnds) delay(500)
        if (sleepEnds > 0L && System.currentTimeMillis() >= sleepEnds) {
            sleepEnds = 0L
            playback.pause()
            onState("READING", playback.positionMs)
        }
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            onState("READING", playback.positionMs)
            playback.release()
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .46f))) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(188.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = .09f),
                            radius = size.minDimension * .45f,
                        )
                        drawArc(
                            color = MaterialTheme.colorScheme.primary,
                            startAngle = -90f,
                            sweepAngle = (if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f).coerceIn(0f, 1f) * 360f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .08f),
                            size = androidx.compose.ui.geometry.Size(size.width * .84f, size.height * .84f),
                            style = Stroke(width = 8f, cap = StrokeCap.Round),
                        )
                    }
                    if (book.coverKey.isNotBlank()) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            modifier = Modifier.size(138.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            Modifier.size(138.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Headphones, null, Modifier.size(52.dp))
                        }
                    }
                }
                Text(book.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                currentChapter?.let { Text(it.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (state.durationMs > 0) {
                    Slider(
                        value = (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f),
                        onValueChange = { playback.seekTo((it * state.durationMs).roundToInt().toLong()) },
                    )
                    Text(
                        toPersianDigits(timeText(state.positionMs)) + " / " + toPersianDigits(timeText(state.durationMs)),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { playback.seekBy(-15000) }) { Icon(Icons.Outlined.FastRewind, null) }
                    IconButton(onClick = { if (state.playing) playback.pause() else playback.play() }) {
                        Icon(if (state.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, Modifier.size(32.dp))
                    }
                    IconButton(onClick = { playback.seekBy(30000) }) { Icon(Icons.Outlined.FastForward, null) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Headphones, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("پخش در پس‌زمینه", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = background, onCheckedChange = { background = it })
                }
            }
        }

        if (book.chapters.isNotEmpty()) {
            Text("فصل‌ها و بخش‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            book.chapters.forEachIndexed { index, chapter ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        currentIndex = index
                        val url = if (chapter.audioKey.isNotBlank()) FreeReadingRepository.absoluteKey(chapter.audioKey) else currentUrl
                        playback.setMedia(
                            url,
                            book.title + " · " + chapter.title,
                            chapter.startMs,
                        )
                        onState("READING", chapter.startMs)
                    },
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(chapter.title, Modifier.weight(1f))
                        IconButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val key = "free-audio-" + book.id + "-" + chapter.id
                                    MediaVault.downloadEncrypted(
                                        context,
                                        listOf(if (chapter.audioKey.isNotBlank()) FreeReadingRepository.absoluteKey(chapter.audioKey) else currentUrl),
                                        key,
                                    ) { _, _ -> }
                                }
                            }
                        }) { Icon(Icons.Outlined.Download, "دانلود") }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0.75f, 1f, 1.25f, 1.5f).forEach { speed ->
                FilterChip(
                    selected = kotlin.math.abs(state.speed - speed) < .01f,
                    onClick = { playback.setSpeed(speed) },
                    label = { Text(speed.toString() + "×", fontSize = 11.sp) },
                )
            }
        }

        Text("تایمر خواب", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15, 30, 45).forEach { m ->
                TextButton(onClick = { sleepEnds = System.currentTimeMillis() + m * 60_000L }) {
                    Text(toPersianDigits(m.toString()) + " دقیقه")
                }
            }
            OutlinedTextField(
                value = sleepMinutes,
                onValueChange = { sleepMinutes = it.filter(Char::isDigit).take(4) },
                label = { Text("دقیقه دلخواه") },
                modifier = Modifier.width(120.dp),
                singleLine = true,
            )
            TextButton(onClick = {
                val m = sleepMinutes.toLongOrNull()?.coerceIn(1, 1440) ?: return@TextButton
                sleepEnds = System.currentTimeMillis() + m * 60_000L
            }) { Text("تنظیم") }
        }

        BookCommentsSection(book.id, repo)
    }
}

private fun timeText(ms: Long): String {
    val s = (ms.coerceAtLeast(0L) / 1000L)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.US, "%02d:%02d", m, sec)
}

@Composable
private fun BookCommentsSection(bookId: String, repo: FreeReadingRepository) {
    val scope = rememberCoroutineScope()
    var comments by remember(bookId) { mutableStateOf<List<BookComment>>(emptyList()) }
    var input by remember(bookId) { mutableStateOf("") }
    var replyTo by remember(bookId) { mutableStateOf<BookComment?>(null) }
    var terms by remember(bookId) { mutableStateOf<Set<String>>(emptySet()) }
    var msg by remember(bookId) { mutableStateOf<String?>(null) }

    LaunchedEffect(bookId) {
        comments = repo.comments(bookId)
        terms = repo.moderationTerms()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Comment, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("دیدگاه‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.take(4000) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (replyTo == null) "نظر خودت را بنویس" else "پاسخ به " + replyTo!!.displayName) },
            minLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (replyTo == null) {
                    val ok = repo.addComment(bookId, input, "", terms)
                    msg = if (ok) "نظر در صف ارسال قرار گرفت." else "این متن قابل انتشار نیست."
                } else {
                    val ok = repo.addComment(bookId, input, replyTo!!.id, terms)
                    msg = if (ok) "پاسخ در صف ارسال قرار گرفت." else "این متن قابل انتشار نیست."
                    replyTo = null
                }
                if (msg?.startsWith("نظر") == true || msg?.startsWith("پاسخ") == true) input = ""
            }) { Text(if (replyTo == null) "ثبت نظر" else "ثبت پاسخ") }
            if (replyTo != null) TextButton(onClick = { replyTo = null }) { Text("لغو") }
        }
        msg?.let { Text(it, style = MaterialTheme.typography.labelSmall) }

        comments.forEach { comment ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .40f))) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(comment.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(comment.body, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { repo.react(comment.id, "LIKE") }) {
                            Icon(Icons.Outlined.ThumbUp, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(toPersianDigits(comment.likes.toString()))
                        }
                        TextButton(onClick = { repo.react(comment.id, "DISLIKE") }) {
                            Icon(Icons.Outlined.ThumbDown, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(toPersianDigits(comment.dislikes.toString()))
                        }
                        TextButton(onClick = { replyTo = comment }) { Text("پاسخ") }
                    }
                }
            }
        }
    }
}
