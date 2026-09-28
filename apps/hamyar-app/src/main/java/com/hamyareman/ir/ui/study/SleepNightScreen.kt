package com.hamyareman.ir.ui.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.feature.playback.PlaybackController
import com.hamyareman.ir.platform.feature.playback.SleepPlaybackService
import com.hamyareman.ir.ui.AppTypography
import kotlinx.coroutines.launch

object SleepLaunch {
    @Volatile var pending: Boolean = false
}

data class SleepTrack(val id: String, val title: String, val subtitle: String, val uri: String)

@Composable
fun SleepNightScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val playback = remember { PlaybackController(ctx, SleepPlaybackService::class.java) }
    val state by playback.state.collectAsState()
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) { onDispose { playback.release() } }
    LaunchedEffect(Unit) { playback.connect() }

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
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("بشنو و بخواب", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("صوت یکنواخت برای خواب. قصه و تنفس در کاشی‌های جدا هستند.", style = AppTypography.pageBody.style)
            audioTracks.forEach { TrackRow(it, state.playing) { play(it) } }
            PrimaryButton("شروع جلسهٔ شنیدن") {
                audioTracks.firstOrNull { it.uri.isNotBlank() }?.let { play(it) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (state.playing) playback.pause() else playback.play() }, modifier = Modifier.weight(1f)) {
                Text(if (state.playing) "استوپ" else "پلی")
            }
            OutlinedButton(onClick = { playback.stop() }, modifier = Modifier.weight(1f)) {
                Text("بستن")
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = AppTypography.pageBody.style, modifier = Modifier.padding(horizontal = 16.dp))
        }
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
                Text("فایل هنوز روی سرور نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = AppTypography.pageBody.style)
            }
        }
    }
}
