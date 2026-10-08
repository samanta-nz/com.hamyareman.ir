package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.feature.playback.BackgroundPlaybackGate
import com.hamyareman.ir.platform.feature.playback.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val FREE_STATE_STORE = "free_reading_state"

private object FreeReadingState {
    private fun store(ctx: Context) = LocalStore(ctx, FREE_STATE_STORE)
    fun pos(ctx: Context, id: String): Long = store(ctx).getLong("pos_" + id, 0L)
    fun pct(ctx: Context, id: String): Int = store(ctx).getInt("pct_" + id, 0)
    fun status(ctx: Context, id: String): String = store(ctx).getString("status_" + id, "unread")
    fun save(ctx: Context, id: String, pos: Long, duration: Long, state: String) {
        val percent = if (duration > 0L) (pos * 100L / duration).coerceIn(0L, 100L).toInt() else pct(ctx, id)
        store(ctx).putLong("pos_" + id, pos.coerceAtLeast(0L))
        store(ctx).putInt("pct_" + id, percent)
        store(ctx).putString("status_" + id, state)
    }
}

private fun clock(ms: Long): String {
    val sec = ms.coerceAtLeast(0L) / 1000L
    return toPersianDigits(String.format(Locale.US, "%d:%02d", sec / 60L, sec % 60L))
}

private fun freeDownloadKey(book: FreeStudyBook, key: String) = FreeStudyCatalog.cacheKey(book.id, key)

@Composable
fun FreeReadingScreen(nav: androidx.navigation.NavController, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    var books by remember { mutableStateOf(emptyList<FreeStudyBook>()) }
    var selected by remember { mutableStateOf<FreeStudyBook?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var detailTab by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf("همه") }
    var requestBook by remember { mutableStateOf<FreeStudyBook?>(null) }
    var requestText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        books = FreeStudyCatalog.load(ctx, container.tables, force = true)
    }
    BackHandler(enabled = selected != null) { selected = null }

    requestBook?.let { book ->
        AlertDialog(
            onDismissRequest = { requestBook = null },
            title = { Text("درخواست کتاب از همیار") },
            text = {
                OutlinedTextField(
                    value = requestText,
                    onValueChange = { requestText = it.take(2000) },
                    label = { Text("درخواست") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val uid = container.auth.cachedUserId().orEmpty()
                    if (uid.isNotBlank()) FreeStudyCatalog.queueRequest(container.sync, uid, book, requestText)
                    requestText = ""
                    requestBook = null
                }) { Text("ارسال") }
            },
            dismissButton = { TextButton(onClick = { requestBook = null }) { Text("لغو") } },
        )
    }

    selected?.let { book ->
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = detailTab) {
                Tab(selected = detailTab == 0, onClick = { detailTab = 0 }, text = { Text(if (book.isAudio) "کتاب صوتی" else "کتاب متنی") })
                Tab(selected = detailTab == 1, onClick = { detailTab = 1 }, text = { Text("متای کتاب") })
                Tab(selected = detailTab == 2, onClick = { detailTab = 2 }, text = { Text("دیدگاه‌ها و گفتگو") })
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (detailTab) {
                    0 -> if (book.isAudio) FreeAudioReader(book) else FreeHtmlReader(book)
                    1 -> FreeBookMeta(book)
                    else -> BookCommentsPanel(bookId = book.id, modifier = Modifier.fillMaxSize().padding(12.dp))
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("کتاب متنی") }, icon = { Icon(Icons.Outlined.Book, null) })
            Tab(tab == 1, { tab = 1 }, text = { Text("کتاب صوتی") }, icon = { Icon(Icons.Outlined.Headphones, null) })
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("همه", "در حال خواندن", "خوانده‌شده", "آرشیو").forEach { item ->
                FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) })
            }
        }
        val kind = if (tab == 0) "free_text_book" else "free_audio_book"
        val visible = books.filter { it.kind == kind }.filter { b ->
            when (filter) {
                "در حال خواندن" -> FreeReadingState.status(ctx, b.id) == "reading"
                "خوانده‌شده" -> FreeReadingState.status(ctx, b.id) == "done"
                "آرشیو" -> FreeReadingState.status(ctx, b.id) == "archive"
                else -> FreeReadingState.status(ctx, b.id) != "archive"
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visible.forEach { book ->
                Card(onClick = { selected = book }, modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = book.coverUrl.takeIf { it.isNotBlank() },
                            contentDescription = book.title,
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(book.title, style = MaterialTheme.typography.titleMedium)
                            Text(book.summary.ifBlank { "توضیح کوتاه کتاب" }, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                            val pct = FreeReadingState.pct(ctx, book.id)
                            if (pct > 0) LinearProgressIndicator({ pct / 100f }, Modifier.fillMaxWidth().height(5.dp))
                            Text(
                                when (FreeReadingState.status(ctx, book.id)) {
                                    "done" -> "خوانده‌شده"
                                    "archive" -> "آرشیو شخصی"
                                    "reading" -> "در حال مطالعه"
                                    else -> "شروع نشده"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { selected = book }) { Text(if (book.isAudio) "پخش" else "خواندن") }
                                TextButton(onClick = { requestBook = book }) { Text("درخواست") }
                            }
                        }
                    }
                }
            }
            if (visible.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.LibraryBooks, null, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("کتابی در این بخش منتشر نشده است.")
                        Text("ادمین می‌تواند کتاب، کاور، توضیح، HTML، صوت و فصل‌ها را بدون انتشار APK اضافه کند.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeBookMeta(book: FreeStudyBook) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    book.summary.ifBlank { "اطلاعات کتاب در حال تکمیل است." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(if (book.isAudio) "کتاب صوتی" else "کتاب متنی") },
                )
            }
        }
        if (book.chapters.isNotEmpty()) {
            Text("فهرست بخش‌ها", style = MaterialTheme.typography.titleMedium)
            book.chapters.forEachIndexed { index, chapter ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(toPersianDigits((index + 1).toString()), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chapter.title, style = MaterialTheme.typography.bodyMedium)
                            if (chapter.startMs > 0) Text(clock(chapter.startMs), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private class AudioSeekBridge(private val onSeek: (Long) -> Unit) {
    @JavascriptInterface
    fun seek(ms: Long) {
        onSeek(ms.coerceAtLeast(0L))
    }
}

@Composable
private fun FreeAudioReader(book: FreeStudyBook) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val playback = remember { PlaybackController(ctx) }
    val state by playback.state.collectAsState()
    val prefs = remember { LocalStore(ctx, FREE_STATE_STORE) }
    var phase by remember { mutableFloatStateOf(0f) }
    var customTimer by remember { mutableStateOf("") }
    var timerUntil by remember { mutableLongStateOf(prefs.getString("sleep_timer_until", "0").toLongOrNull()?.coerceAtLeast(0L) ?: 0L) }
    var backgroundPlayback by remember { mutableStateOf(prefs.getBool("background_playback", true)) }
    var note by remember { mutableStateOf<String?>(null) }
    val downloaded = remember(book.id, book.mediaKey) { mutableStateOf(MediaVault.isVerified(ctx, freeDownloadKey(book, book.mediaKey))) }

    LaunchedEffect(backgroundPlayback) {
        BackgroundPlaybackGate.enabled = backgroundPlayback
        if (!backgroundPlayback) playback.stop()
    }

    LaunchedEffect(Unit) {
        playback.connect()
        val key = book.mediaKey
        if (key.isNotBlank() && !state.hasMedia) {
            val cache = freeDownloadKey(book, key)
            val uri = if (MediaVault.isVerified(ctx, cache)) MediaVault.localUrl(ctx, cache) else StudyMedia.viewUrl(key)
            downloaded.value = MediaVault.isVerified(ctx, cache)
            playback.setMedia(uri, book.title, FreeReadingState.pos(ctx, book.id))
        }
        if (timerUntil > System.currentTimeMillis()) playback.setSleepTimer(timerUntil)
    }

    LaunchedEffect(state.playing) {
        while (state.playing) {
            phase += 0.45f
            FreeReadingState.save(ctx, book.id, playback.positionMs, playback.durationMs, "reading")
            val uid = container.auth.cachedUserId().orEmpty()
            if (uid.isNotBlank()) FreeStudyCatalog.queueProgress(container.sync, uid, book, playback.positionMs, playback.durationMs, "reading")
            delay(15_000L)
        }
    }

    LaunchedEffect(timerUntil) {
        if (timerUntil <= 0L) return@LaunchedEffect
        while (System.currentTimeMillis() < timerUntil) delay(500L)
        playback.pause()
        playback.clearSleepTimer()
        timerUntil = 0L
        prefs.putString("sleep_timer_until", "0")
    }

    DisposableEffect(Unit) {
        onDispose {
            FreeReadingState.save(ctx, book.id, playback.positionMs, playback.durationMs, "reading")
            if (!backgroundPlayback) {
                BackgroundPlaybackGate.enabled = false
                playback.stop()
            }
            playback.release()
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val duration = state.durationMs.coerceAtLeast(0L)
    val position = state.positionMs.coerceAtLeast(0L)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(model = book.coverUrl.takeIf { it.isNotBlank() }, contentDescription = book.title, modifier = Modifier.size(176.dp).clip(CircleShape))
                    Canvas(Modifier.matchParentSize()) {
                        repeat(40) { i ->
                            val angle = i * (Math.PI.toFloat() * 2f / 40f)
                            val amp = if (state.playing) 0.28f + 0.72f * abs(sin(phase + i)) else 0.10f
                            val r1 = size.minDimension * 0.46f
                            val r2 = r1 + size.minDimension * 0.018f * amp
                            drawLine(accent.copy(alpha = 0.70f), androidx.compose.ui.geometry.Offset(size.width / 2 + cos(angle) * r1, size.height / 2 + sin(angle) * r1), androidx.compose.ui.geometry.Offset(size.width / 2 + cos(angle) * r2, size.height / 2 + sin(angle) * r2), 3f, StrokeCap.Round)
                        }
                    }
                }
                Text(book.title, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (downloaded.value) Icons.Outlined.Smartphone else Icons.Outlined.CloudOff, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (downloaded.value) "پخش آفلاین" else "پخش آنلاین", style = MaterialTheme.typography.labelMedium, color = accent)
                }
                Slider(value = position.toFloat(), onValueChange = { playback.seekTo(it.toLong()) }, valueRange = 0f..max(duration, 1L).toFloat(), modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(Modifier.width(72.dp), contentAlignment = Alignment.CenterStart) {
                        Text(clock(position), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Text(clock(duration), Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { playback.seekBy(-30_000L) }) { Icon(Icons.Filled.SkipPrevious, null) }
                    IconButton(onClick = { if (state.playing) playback.pause() else playback.play() }, modifier = Modifier.size(58.dp).background(accent, CircleShape)) {
                        Icon(if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    IconButton(onClick = { playback.seekBy(30_000L) }) { Icon(Icons.Filled.SkipNext, null) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        TextButton(onClick = { playback.setSpeed(speed) }) { Text(speed.toString() + "×", fontSize = 12.sp) }
                    }
                }
                if (book.mediaKey.isNotBlank() && !downloaded.value) {
                    OutlinedButton(onClick = {
                        note = "در حال دانلود…"
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            try {
                                MediaVault.downloadEncrypted(ctx, StudyMedia.candidateUrls(book.mediaKey), freeDownloadKey(book, book.mediaKey)) { _, _ -> }
                                withContext(Dispatchers.Main) {
                                    downloaded.value = true
                                    note = "دانلود کامل شد."
                                    playback.setMedia(MediaVault.localUrl(ctx, freeDownloadKey(book, book.mediaKey)), book.title, position)
                                }
                            } catch (_: Throwable) {
                                withContext(Dispatchers.Main) { note = "دانلود کامل نشد." }
                            }
                        }
                    }) {
                        Icon(Icons.Outlined.CloudDownload, null)
                        Spacer(Modifier.width(4.dp))
                        Text("دانلود کامل")
                    }
                }
            }
        }

        note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        if (book.htmlKey.isNotBlank()) {
            val tocUrl = remember(book.htmlKey) {
                StudyMedia.viewUrl(book.htmlKey).takeIf { LessonCache.isCached(ctx, it) }
            }
            var readyTocUrl by remember(book.htmlKey, tocUrl) { mutableStateOf(tocUrl) }
            var tocLoading by remember(book.htmlKey) { mutableStateOf(false) }
            var tocTitle by remember(book.htmlKey) { mutableStateOf("محتوا در حال دانلود") }
            var tocProgress by remember(book.htmlKey) { mutableIntStateOf(0) }
            var tocReload by remember(book.htmlKey) { mutableIntStateOf(0) }

            LaunchedEffect(book.htmlKey) {
                val url = StudyMedia.viewUrl(book.htmlKey)
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                withContext(Dispatchers.IO) {
                    val prepared = LessonCache.prepare(
                        ctx = ctx,
                        url = url,
                        onProgress = { done, total ->
                            if (total > 0) {
                                handler.post {
                                    tocProgress = ((done.toLong() * 100L) / total).toInt().coerceIn(0, 100)
                                }
                            }
                        },
                        onStatus = { status ->
                            when (status) {
                                LessonCache.Freshness.CACHED -> handler.post { tocLoading = false }
                                LessonCache.Freshness.UPDATED -> handler.post {
                                    tocTitle = "محتوا در حال بروزرسانی"
                                    tocLoading = true
                                }
                                LessonCache.Freshness.DOWNLOADED -> handler.post {
                                    tocTitle = "محتوا در حال دانلود"
                                    tocLoading = true
                                }
                            }
                        },
                    )
                    handler.post {
                        when {
                            prepared == null -> {
                                if (!LessonCache.isCached(ctx, url)) readyTocUrl = null
                                tocLoading = false
                            }
                            prepared.freshness == LessonCache.Freshness.CACHED -> {
                                readyTocUrl = url
                                tocLoading = false
                            }
                            else -> {
                                readyTocUrl = url
                                tocReload++
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text("فهرست مطالب صوتی", style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.fillMaxWidth().height(260.dp)) {
                        if (readyTocUrl == null) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                            ) {
                                CircularProgressIndicator()
                                Text(tocTitle, style = MaterialTheme.typography.bodyMedium)
                                if (tocProgress > 0) Text(tocProgress.toString() + "٪")
                            }
                        } else {
                            AndroidView(
                                factory = { context ->
                                    android.webkit.WebView(context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        addJavascriptInterface(AudioSeekBridge { ms ->
                                            playback.seekTo(ms)
                                            playback.play()
                                        }, "HamyarAudio")
                                        webViewClient = HmkWebViewClient(
                                            context.applicationContext,
                                            HmkWebViewClient.bucketHost(),
                                        )
                                        tag = readyTocUrl + "#0"
                                        loadUrl(readyTocUrl!!)
                                    }
                                },
                                update = { view ->
                                    val tag = readyTocUrl + "#" + tocReload
                                    if (view.tag != tag) {
                                        view.tag = tag
                                        view.loadUrl(readyTocUrl!!)
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (tocLoading) {
                                Box(
                                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.90f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator()
                                        Text(tocTitle, style = MaterialTheme.typography.bodyMedium)
                                        if (tocProgress > 0) Text(tocProgress.toString() + "٪")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (book.chapters.isNotEmpty()) {
            Text("فصل‌ها و بخش‌ها", style = MaterialTheme.typography.titleMedium)
            book.chapters.forEach { chapter ->
                val key = chapter.mediaKey.ifBlank { book.mediaKey }
                Card(Modifier.fillMaxWidth().clickable {
                    if (chapter.mediaKey.isNotBlank()) {
                        val local = freeDownloadKey(book, chapter.mediaKey)
                        playback.setMedia(
                            if (MediaVault.isVerified(ctx, local)) MediaVault.localUrl(ctx, local) else StudyMedia.viewUrl(chapter.mediaKey),
                            book.title + " — " + chapter.title,
                            chapter.startMs,
                        )
                        downloaded.value = MediaVault.isVerified(ctx, local)
                    } else playback.seekTo(chapter.startMs)
                    playback.play()
                }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Book, null)
                        Spacer(Modifier.width(8.dp))
                        Text(chapter.title, Modifier.weight(1f))
                        Text(clock(chapter.startMs), style = MaterialTheme.typography.labelSmall)
                        if (key.isNotBlank()) IconButton(onClick = {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                runCatching { MediaVault.downloadEncrypted(ctx, StudyMedia.candidateUrls(key), freeDownloadKey(book, key)) { _, _ -> } }
                                withContext(Dispatchers.Main) {
                                    downloaded.value = MediaVault.isVerified(ctx, freeDownloadKey(book, key))
                                }
                            }
                        }) {
                            Icon(if (MediaVault.isVerified(ctx, freeDownloadKey(book, key))) Icons.Outlined.Smartphone else Icons.Outlined.CloudDownload, contentDescription = "دانلود فصل")
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Headphones, null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("پخش در پس‌زمینه", style = MaterialTheme.typography.titleSmall)
                    Text("با بسته‌شدن صفحه، صدا ادامه پیدا کند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = backgroundPlayback,
                    onCheckedChange = {
                        backgroundPlayback = it
                        prefs.putBool("background_playback", it)
                        BackgroundPlaybackGate.enabled = it
                        if (!it) playback.stop()
                    },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, null)
                    Spacer(Modifier.width(8.dp))
                    Text("تایمر خواب", style = MaterialTheme.typography.titleSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15, 30, 45, 60).forEach { m ->
                        FilterChip(
                            selected = timerUntil > System.currentTimeMillis() && timerUntil <= System.currentTimeMillis() + m * 60_000L,
                            onClick = {
                                timerUntil = System.currentTimeMillis() + m.toLong() * 60_000L
                                prefs.putString("sleep_timer_until", timerUntil.toString())
                                playback.setSleepTimer(timerUntil)
                            },
                            label = { Text(toPersianDigits(m.toString())) },
                        )
                    }
                }
                OutlinedTextField(value = customTimer, onValueChange = { customTimer = it.filter(Char::isDigit).take(18) }, label = { Text("زمان دلخواه (دقیقه)") }, singleLine = true)
                OutlinedButton(onClick = {
                    val m = customTimer.toLongOrNull()?.takeIf { it > 0L } ?: return@OutlinedButton
                    val maxMinutes = Long.MAX_VALUE / 60_000L
                    timerUntil = System.currentTimeMillis() + m.coerceAtMost(maxMinutes) * 60_000L
                    prefs.putString("sleep_timer_until", timerUntil.toString())
                    playback.setSleepTimer(timerUntil)
                }) {
                    Icon(Icons.Outlined.MoreTime, null)
                    Spacer(Modifier.width(4.dp))
                    Text("ثبت تایمر")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FreeHtmlReader(book: FreeStudyBook) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val url = StudyMedia.viewUrl(book.htmlKey)
    var readyUrl by remember(book.htmlKey) {
        mutableStateOf(url.takeIf { it.isNotBlank() && LessonCache.isCached(ctx, it) })
    }
    var transferLoading by remember(book.htmlKey) { mutableStateOf(false) }
    var transferTitle by remember(book.htmlKey) { mutableStateOf("محتوا در حال دانلود") }
    var transferProgress by remember(book.htmlKey) { mutableIntStateOf(0) }
    var reloadToken by remember(book.htmlKey) { mutableIntStateOf(0) }
    var scrollY by remember { mutableIntStateOf(FreeReadingState.pos(ctx, book.id).toInt()) }

    LaunchedEffect(book.id) {
        while (true) {
            delay(15_000L)
            FreeReadingState.save(ctx, book.id, scrollY.toLong(), 100L, if (scrollY > 0) "reading" else "unread")
            val uid = container.auth.cachedUserId().orEmpty()
            if (uid.isNotBlank()) {
                FreeStudyCatalog.queueProgress(
                    container.sync, uid, book, scrollY.toLong(), 100L, "reading",
                )
            }
        }
    }

    LaunchedEffect(book.htmlKey) {
        if (url.isBlank()) return@LaunchedEffect
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        withContext(Dispatchers.IO) {
            val prepared = LessonCache.prepare(
                ctx = ctx,
                url = url,
                onProgress = { done, total ->
                    if (total > 0) {
                        handler.post {
                            transferProgress = ((done.toLong() * 100L) / total).toInt().coerceIn(0, 100)
                        }
                    }
                },
                onStatus = { status ->
                    when (status) {
                        LessonCache.Freshness.CACHED -> handler.post { transferLoading = false }
                        LessonCache.Freshness.UPDATED -> handler.post {
                            transferTitle = "محتوا در حال بروزرسانی"
                            transferLoading = true
                        }
                        LessonCache.Freshness.DOWNLOADED -> handler.post {
                            transferTitle = "محتوا در حال دانلود"
                            transferLoading = true
                        }
                    }
                },
            )
            handler.post {
                when {
                    prepared == null && !LessonCache.isCached(ctx, url) -> {
                        readyUrl = null
                        transferLoading = false
                    }
                    prepared?.freshness == LessonCache.Freshness.CACHED -> {
                        readyUrl = url
                        transferLoading = false
                    }
                    prepared != null -> {
                        readyUrl = url
                        reloadToken++
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (readyUrl != null) {
            AndroidView(
                factory = { context ->
                    ZoomResetWebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                        webViewClient = object : HmkWebViewClient(
                            context.applicationContext,
                            HmkWebViewClient.bucketHost(),
                        ) {
                            override fun onPageFinished(view: android.webkit.WebView, loadedUrl: String) {
                                super.onPageFinished(view, loadedUrl)
                                transferLoading = false
                                view.scrollTo(0, scrollY)
                            }
                        }
                        setOnScrollChangeListener { _, _, y, _, _ -> scrollY = y }
                        tag = url + "#0"
                        loadUrl(url)
                    }
                },
                update = { view ->
                    val tag = url + "#" + reloadToken
                    if (view.tag != tag) {
                        view.tag = tag
                        view.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { it.destroy() },
            )

            if (transferLoading) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(transferTitle, style = MaterialTheme.typography.titleMedium)
                        if (transferProgress > 0) Text(transferProgress.toString() + "٪")
                    }
                }
            }
        } else {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(transferTitle, style = MaterialTheme.typography.titleMedium)
                    if (transferProgress > 0) Text(transferProgress.toString() + "٪")
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    BookCommentsPanel(bookId = book.id, modifier = Modifier.fillMaxWidth())
}
