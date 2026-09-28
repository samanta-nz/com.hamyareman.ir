package com.hamyareman.ir.ui.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.playback.LessonMediaPlayer
import com.hamyareman.ir.platform.feature.playback.LessonMediaProgressRepository
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.content.Lesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * پرامپت ۰۱ — صفحه‌ی پلیر درس.
 *
 * این صفحه:
 *  - اگر درس [Lesson.videoUrl] داشته باشد → پلیر ویدیویی با کنترل‌های کامل.
 *  - اگر [Lesson.audioUrl] داشته باشد → پلیر صوتی با همان کنترل‌ها.
 *  - اگر هر دو داشته باشد → انتخاب‌گر تب.
 *  - اگر هیچ‌کدام نباشد → پیام «رسانه‌ای برای این درس آماده نشده».
 *
 * نوار وضعیت درس (طبق پرامپت):
 *  - «تماشا نشده» یا «مرور دوباره — مشاهده‌شده در ۱۴۰۴/۰۶/۱۸ با سرعت ۱٫۲۵x»
 *  - نوار پیشرفت با نقاط کوچک روی seek-jump ها
 */
@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LessonPlayerScreen(
    lesson: Lesson,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val userId = remember { { container.auth.cachedUserId() } }

    // یک پلیر برای کل صفحه (ویدیو و صوت هر دو با همین کنترلر کار می‌کنند)
    val player = remember(container) {
        LessonMediaPlayer(
            context = container.appContext,
            progressRepo = container.mediaProgress,
            userId = userId,
        )
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // هرگز در پس‌زمینه: هوم/مینیمایز/جابجایی اپ/قفل گوشی → مکث فوری؛
    // برگشت به صفحه → ادامه از همان جایی که مانده بود.
    com.hamyareman.ir.ui.study.PauseOnStopEffect(pause = { player.pause() })

    val state by player.state.collectAsState()

    // بارگذاری خودکار وقتی وارد صفحه می‌شویم: ویدیو اولویت دارد
    var selectedTab by remember { mutableStateOf<LessonMediaProgressRepository.MediaType?>(null) }
    LaunchedEffect(lesson.id) {
        val initial = when {
            lesson.videoUrl.isNotBlank() -> LessonMediaProgressRepository.MediaType.VIDEO
            lesson.audioUrl.isNotBlank() -> LessonMediaProgressRepository.MediaType.AUDIO
            else -> null
        }
        selectedTab = initial
        if (initial != null) {
            val url = if (initial == LessonMediaProgressRepository.MediaType.VIDEO) lesson.videoUrl else lesson.audioUrl
            player.load(
                uri = url,
                title = lesson.title,
                bookCode = lesson.bookCode.ifBlank { lesson.subject.ifBlank { "lesson" } },
                lessonId = lesson.id,
                mediaType = initial,
                autoplay = true,
            )
        }
    }

    // snapshot زنده از progress برای نوار وضعیت
    val progressFlow = remember { MutableStateFlow<LessonMediaProgressRepository.Progress?>(null) }
    LaunchedEffect(lesson.id, selectedTab) {
        val tab = selectedTab ?: return@LaunchedEffect
        progressFlow.value = container.mediaProgress.load(
            userId = container.auth.cachedUserId() ?: "",
            bookCode = lesson.bookCode.ifBlank { lesson.subject.ifBlank { "lesson" } },
            lessonId = lesson.id,
            mediaType = tab,
        )
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(lesson.title, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- نوار وضعیت درس (پرامپت ۰۱) ---
            LessonStatusBar(lesson = lesson, progress = progressFlow.collectAsState().value)

            // پرش‌های seek به‌صورت نقطه روی خط زمان (پرامپت ۰۱)
            SeekJumpDots(progressFlow.collectAsState().value)
            if (state.chapterCount > 1) {
                Text(
                    "فصل ${state.chapterIndex + 1} از ${state.chapterCount} — فصل‌ها پشت‌سرهم به‌صورت پلی‌لیست پخش می‌شوند",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // --- انتخاب‌گر تب ویدیو/صوت ---
            if (lesson.videoUrl.isNotBlank() && lesson.audioUrl.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabChip("ویدیو", selected = selectedTab == LessonMediaProgressRepository.MediaType.VIDEO) {
                        switchTab(player, lesson, LessonMediaProgressRepository.MediaType.VIDEO, scope) { selectedTab = it }
                    }
                    TabChip("صوت", selected = selectedTab == LessonMediaProgressRepository.MediaType.AUDIO) {
                        switchTab(player, lesson, LessonMediaProgressRepository.MediaType.AUDIO, scope) { selectedTab = it }
                    }
                }
            }

            val activeUrl = when (selectedTab) {
                LessonMediaProgressRepository.MediaType.VIDEO -> lesson.videoUrl
                LessonMediaProgressRepository.MediaType.AUDIO -> lesson.audioUrl
                else -> ""
            }

            if (activeUrl.isBlank()) {
                EmptyMediaCard()
            } else {
                // --- پلیر (ویدیو با PlayerView؛ صوت فقط کنترل‌ها) ---
                if (selectedTab == LessonMediaProgressRepository.MediaType.VIDEO) {
                    VideoPlayerView(player)
                } else {
                    AudioOnlyHeader(state = state)
                }

                // --- کنترل‌های مشترک ---
                PlayerControls(
                    state = state,
                    player = player,
                    chapterMarkers = if (selectedTab == LessonMediaProgressRepository.MediaType.VIDEO) lesson.chapterMarkers else emptyList(),
                )

                // --- نوار پیشرفت با نقاط seek-jump ---
                progressFlow.collectAsState().value?.let { p ->
                    if (p.seekJumps.isNotEmpty()) {
                        SeekJumpsLegend(p)
                    }
                }
            }

            // متن درس
            if (lesson.body.isNotBlank()) {
                Text("متن درس", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        lesson.body,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonStatusBar(
    lesson: Lesson,
    progress: LessonMediaProgressRepository.Progress?,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            val label = if (progress == null || progress.viewCount == 0) {
                "تماشا نشده"
            } else {
                val dateText = progress.lastViewedAtIso.takeIf { it.isNotBlank() }
                    ?.let { com.hamyareman.ir.platform.core.common.JalaliDate.formatFa(it) }
                    ?: "—"
                val speedText = "%.2fx".format(Locale.US, progress.playbackSpeed)
                if (progress.isCompleted) {
                    "تکمیل‌شده · ${progress.viewCount} بار دیده‌شده · آخرین بار $dateText با سرعت $speedText"
                } else {
                    "در حال تماشا · ${progress.viewCount} بار دیده‌شده · آخرین بار $dateText با سرعت $speedText"
                }
            }
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (progress != null && progress.durationSec > 0) {
                Spacer(Modifier.height(8.dp))
                val pct = (progress.fraction * 100).toInt().coerceIn(0, 100)
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                )
                Text("$pct٪", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@UnstableApi
@Composable
private fun VideoPlayerView(player: LessonMediaPlayer) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player.exoPlayer()
                useController = false // کنترل‌ها را خودمان می‌سازیم
            }
        },
        update = { view ->
            view.player = player.exoPlayer()
        }
    )
}

@Composable
private fun AudioOnlyHeader(state: LessonMediaPlayer.PlayerState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Column {
                Text(
                    "پخش صوتی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (state.playing) "در حال پخش…" else "متوقف",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@UnstableApi
@Composable
private fun PlayerControls(
    state: LessonMediaPlayer.PlayerState,
    player: LessonMediaPlayer,
    chapterMarkers: List<Double>,
) {
    var speedOpen by remember { mutableStateOf(false) }
    var sliderValue by remember(state.positionMs) { mutableStateOf(state.positionMs.toFloat()) }
    var userIsSeeking by remember { mutableStateOf(false) }

    // اگر کاربر دارد درگ می‌کند، از آپدیت بیرونی جلوگیری کن
    LaunchedEffect(state.positionMs) {
        if (!userIsSeeking) sliderValue = state.positionMs.toFloat()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            // SeekBar با نمایش زمان
            val duration = state.durationMs.coerceAtLeast(0L)
            Slider(
                value = sliderValue.coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
                onValueChange = { sliderValue = it; userIsSeeking = true },
                onValueChangeFinished = {
                    player.seekTo(sliderValue.toLong())
                    userIsSeeking = false
                },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatMs(state.positionMs), style = MaterialTheme.typography.labelSmall)
                Text(formatMs(duration), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (chapterMarkers.isNotEmpty()) {
                    IconButton(onClick = { player.jumpChapter(chapterMarkers, -1) }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "فصل قبل")
                    }
                }
                IconButton(onClick = { player.seekBy(-10_000) }) {
                    Icon(Icons.Filled.Replay10, contentDescription = "عقب ۱۰ ثانیه")
                }
                IconButton(
                    onClick = { if (state.playing) player.pause() else player.play() },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.playing) "توقف" else "پخش",
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = { player.seekBy(+10_000) }) {
                    Icon(Icons.Filled.Forward10, contentDescription = "جلو ۱۰ ثانیه")
                }
                if (chapterMarkers.isNotEmpty()) {
                    IconButton(onClick = { player.jumpChapter(chapterMarkers, +1) }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "فصل بعد")
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    AssistChip(
                        onClick = { speedOpen = true },
                        leadingIcon = { Icon(Icons.Filled.Speed, null) },
                        label = { Text("%.2fx".format(Locale.US, state.speed)) },
                        colors = AssistChipDefaults.assistChipColors(),
                    )
                    DropdownMenu(expanded = speedOpen, onDismissRequest = { speedOpen = false }) {
                        LessonMediaPlayer.ALLOWED_SPEEDS.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("%.2fx".format(Locale.US, s)) },
                                onClick = { player.setSpeed(s); speedOpen = false },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.error != null) "خطا: ${state.error}" else " ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SeekJumpsLegend(progress: LessonMediaProgressRepository.Progress) {
    val total = progress.durationSec.coerceAtLeast(0.0)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "پرش‌های اخیر (${progress.seekJumps.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            progress.seekJumps.takeLast(5).forEach { sj ->
                Text(
                    "از ${formatSec(sj.fromSec)} تا ${formatSec(sj.toSec)} (${if (sj.direction == "forward") "جلو" else "عقب"})",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun SeekJumpDots(progress: LessonMediaProgressRepository.Progress?) {
    val jumps = progress?.seekJumps.orEmpty().takeLast(8)
    val dur = progress?.durationSec ?: 0.0
    if (jumps.isEmpty() || dur <= 0.0) return
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 4.dp),
    ) {
        val w = maxWidth
        jumps.forEach { sj ->
            val f = (sj.toSec / dur).toFloat().coerceIn(0f, 1f)
            Box(
                Modifier
                    .offset(x = w * f - 3.dp)
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun EmptyMediaCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "رسانه‌ای برای این درس آماده نشده",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "وقتی فایل ویدیو یا صوت آماده شود، اینجا پخش می‌شود.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// ---------- ابزارهای کمکی ----------

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatSec(sec: Double): String {
    val total = sec.toLong().coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}

private fun switchTab(
    player: LessonMediaPlayer,
    lesson: Lesson,
    type: LessonMediaProgressRepository.MediaType,
    scope: kotlinx.coroutines.CoroutineScope,
    onSelected: (LessonMediaProgressRepository.MediaType) -> Unit,
) {
    onSelected(type)
    val url = if (type == LessonMediaProgressRepository.MediaType.VIDEO) lesson.videoUrl else lesson.audioUrl
    if (url.isNotBlank()) {
        scope.launch {
            player.load(
                uri = url,
                title = lesson.title,
                bookCode = lesson.bookCode.ifBlank { lesson.subject.ifBlank { "lesson" } },
                lessonId = lesson.id,
                mediaType = type,
                autoplay = true,
            )
        }
    }
}
