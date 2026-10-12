package com.hamyareman.ir.ui.study

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.feature.playback.PlaybackController
import com.hamyareman.ir.platform.feature.playback.SleepPlaybackService
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.calmdown.BackgroundMusicTileHost
import com.hamyareman.ir.ui.calmdown.MusicTileHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SleepLaunch {
    /** state است تا onNewIntent در حالی که اپ باز است هم ناوبری را فوراً اجرا کند. */
    var pendingDestination by mutableStateOf<String?>(null)
}

data class SleepTrack(val id: String, val title: String, val subtitle: String, val uri: String)

/** گزینه‌های تایمر خواب (دقیقه). صفر یعنی بدون تایمر. */
private val SLEEP_TIMER_OPTIONS = listOf(0, 15, 30, 60)

// این دو تکه به درخواست کاربر فعلاً نمایش داده نمی‌شوند ولی حذف هم نشده‌اند:
// تایمر خوابِ این صفحه (تایمرِ معتبر در «نجواهای آرام‌بخش» است) و نوار بالای صفحه.
private const val SHOW_SLEEP_TIMER = false
private const val SHOW_TOP_BAR = false

@Composable
fun SleepNightScreen(onBack: () -> Unit) {
    var remoteTrack by remember { mutableStateOf<SleepTrack?>(null) }
    if (remoteTrack != null) {
        BookNodeScreen(remoteTrack!!.title, BucketRoutes.key("sleep-" + remoteTrack!!.id), onBack = { remoteTrack = null })
        return
    }
    val ctx = LocalContext.current
    val playback = remember { PlaybackController(ctx, SleepPlaybackService::class.java) }
    val state by playback.state.collectAsState()
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) { onDispose { playback.release() } }
    LaunchedEffect(Unit) { playback.connect() }

    // صدای واقعی این صفحه از tile موسیقی (Web Audio) می‌آید؛ دکمه‌های پایین و تایمر
    // باید همان را کنترل کنند، نه فقط PlaybackController بومی.
    val music = remember { MusicTileHandle() }
    var musicPlaying by remember { mutableStateOf(false) }
    var nativeStarted by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            music.queryPlaying { musicPlaying = it }
            delay(3000)
        }
    }
    // تایمر خواب: پس از مدت انتخاب‌شده هر دو منبع صدا متوقف می‌شوند.
    LaunchedEffect(timerMinutes) {
        if (timerMinutes > 0) {
            delay(timerMinutes * 60_000L)
            music.pause()
            playback.stop()
            timerMinutes = 0
        }
    }

    val audioTracks = remember {
        listOf(
            SleepTrack("h1", "آرام‌سازی بدن", "خودهیپنوتیزم ملایم", ""),
            SleepTrack("h2", "موسیقی خواب", "صدای آرام شب", ""),
            SleepTrack("h3", "باران پشت پنجره", "صدای یکنواخت برای خواب", ""),
        )
    }

    fun play(t: SleepTrack) {
        if (t.uri.isBlank()) { remoteTrack = t; return }
        scope.launch {
            playback.connect()
            playback.setMedia(t.uri, t.title)
            playback.play()
            nativeStarted = true
        }
    }

    val anyPlaying = state.playing || musicPlaying

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(92.dp))
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "صدای دلخواهت را انتخاب کن، اگر خواستی دو صدا را با هم ترکیب کن و بعد صفحه را برای خواب آرام بگذار.",
                    style = AppTypography.pageBody.style,
                )
                audioTracks.forEach { TrackRow(it, state.playing) { play(it) } }
                PrimaryButton("شروع جلسهٔ شنیدن") {
                    val first = audioTracks.firstOrNull { it.uri.isNotBlank() }
                    if (first != null) play(first) else if (!musicPlaying) music.toggle()
                }
                if (SHOW_SLEEP_TIMER) {
                    Text("تایمر خواب", style = AppTypography.pageHeading.style)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SLEEP_TIMER_OPTIONS.forEach { m ->
                            val selected = timerMinutes == m
                            OutlinedButton(
                                onClick = { timerMinutes = m },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    (if (selected) "✓ " else "") +
                                        if (m == 0) "بدون" else toPersianDigits(m.toString()) + " دقیقه",
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            when {
                                state.playing -> playback.pause()
                                musicPlaying -> music.pause()
                                nativeStarted -> playback.play()
                                else -> music.toggle()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (anyPlaying) "استوپ" else "پلی")
                    }
                    OutlinedButton(
                        onClick = {
                            music.pause()
                            playback.stop()
                            nativeStarted = false
                            timerMinutes = 0
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("بستن")
                    }
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = AppTypography.pageBody.style,
                    )
                }
            }
        }
        BackgroundMusicTileHost(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(20f),
            handle = music,
        )
    }
}

@Composable
fun SleepBreathScreen(onBack: () -> Unit) {
    BookNodeScreen("تنفس آرام پیش از خواب", BucketRoutes.key("sleep-breath"), onBack = onBack)
}

@Composable
private fun TrackRow(t: SleepTrack, playing: Boolean, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onPlay)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title)
            Text(t.subtitle, style = MaterialTheme.typography.bodySmall)

        }
    }
}
