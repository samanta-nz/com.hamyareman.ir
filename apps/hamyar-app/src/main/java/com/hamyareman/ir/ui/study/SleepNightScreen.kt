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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
            SleepTrack("h1", "آرام‌سازی بدن", "خودهیپنوتیزم ملایم — فایل بعداً در باکت", ""),
            SleepTrack("h2", "موسیقی خواب", "صدای آرام شب — فایل بعداً در باکت", ""),
            SleepTrack("h3", "باران پشت پنجره", "صدای یکنواخت برای خواب", ""),
        )
    }

    fun play(t: SleepTrack) {
        if (t.uri.isBlank()) return
        scope.launch {
            playback.connect()
            playback.setMedia(t.uri, t.title)
            playback.play()
            nativeStarted = true
        }
    }

    val anyPlaying = state.playing || musicPlaying

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // نسخهٔ قبلی نوار بالا را نداشت و onBack هیچ‌جا استفاده نمی‌شد؛ تنها راه
            // برگشت دکمهٔ سیستم بود.
            if (SHOW_TOP_BAR) AppTopBar("بشنو و بخواب", onBack)
            // کاشی در جریان محتوای اسکرول جای رزروشدهٔ ثابت دارد، اما خود WebView
            // خارج از verticalScroll به‌صورت overlay روی آن قرار می‌گیرد تا gesture
            // باز/بسته‌شدن را از scroll container جدا نگه دارد.
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(Modifier.height(92.dp))
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("صدای دلخواهت را انتخاب کن، اگر خواستی دو صدا را با هم ترکیب کن و بعد صفحه را برای خواب آرام بگذار.", style = AppTypography.pageBody.style)
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
                                OutlinedButton(onClick = { timerMinutes = m }, modifier = Modifier.weight(1f)) {
                                    Text(
                                        (if (selected) "✓ " else "") +
                                            if (m == 0) "بدون" else toPersianDigits(m.toString()) + " دقیقه",
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.pageBody.style, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        BackgroundMusicTileHost(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(20f),
            handle = music,
        )
    }
}

@Composable
fun SleepBreathScreen(onBack: () -> Unit) {
    var session by remember { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    val steps = listOf(
        "به پهلو یا پشت دراز بکش. بالش بین زانو اگر راحت‌تر است.",
        "دست روی شکم. دم از بینی؛ شکم بالا بیاید نه سینه.",
        "بازدم آرام‌تر از دم؛ مثل بادکنکی که آرام خالی می‌شود.",
        "چهار دم، هفت نگه، هشت بازدم — سه دور. اگر گیج شدی برگرد به نفس معمولی.",
        "بگو: «امشب کارم تمام است.» اگر فکر آمد، روی بازدم سوارش کن و بفرست.",
    )
    Column(Modifier.fillMaxSize()) {
        AppTopBar("تنفس پیش از خواب", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("برای خواب، نه برای بیداری. چراغ کم، گوشی رو به میز.", style = AppTypography.pageBody.style)
            if (!session) {
                steps.forEachIndexed { i, s ->
                    Text(toPersianDigits((i + 1).toString()) + ". " + s, style = AppTypography.pageBody.style)
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton("شروع جلسه") { session = true; step = 0 }
            } else {
                Text("گام " + toPersianDigits((step + 1).toString()) + " از " + toPersianDigits(steps.size.toString()), style = AppTypography.pageHeading.style)
                Text(steps[step], style = AppTypography.pageBody.style)
                if (step < steps.lastIndex) {
                    PrimaryButton("گام بعد") { step++ }
                } else {
                    PrimaryButton("پایان جلسه") { session = false; step = 0 }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(t: SleepTrack, playing: Boolean, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = t.uri.isNotBlank(), onClick = onPlay)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title)
            Text(t.subtitle, style = MaterialTheme.typography.bodySmall)
            if (t.uri.isBlank()) {
                Text("این بخش به‌زودی فعال می‌شود", color = MaterialTheme.colorScheme.onSurfaceVariant, style = AppTypography.pageBody.style)
            }
        }
    }
}
