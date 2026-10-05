package com.hamyareman.ir.ui.study

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class PremiumMediaQueueItem(
    val id: String,
    val title: String,
    val uri: android.net.Uri,
)

@Composable
fun PremiumMediaPlayer(
    items: List<PremiumMediaQueueItem>,
    initialIndex: Int = 0,
    video: Boolean,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("رسانه‌ای برای پخش وجود ندارد.", color = Color.White)
        }
        return
    }
    val context = LocalContext.current
    val queueKey = remember(items) { items.joinToString("|") { it.id } }
    val playback = remember(queueKey) {
        com.hamyareman.ir.platform.feature.playback.PlaybackController(context)
    }
    val state by playback.state.collectAsState()
    val store = remember(queueKey) {
        com.hamyareman.ir.platform.core.common.LocalStore(context, "premium_playback_resume")
    }
    var position by remember(queueKey) { mutableLongStateOf(0L) }
    var scrub by remember(queueKey) { mutableLongStateOf(-1L) }
    var phase by remember(queueKey) { mutableFloatStateOf(0f) }
    var muted by remember(queueKey) { mutableStateOf(false) }

    LaunchedEffect(queueKey) {
        if (!playback.connect()) return@LaunchedEffect
        val mediaItems = items.map { row ->
            MediaItem.Builder()
                .setMediaId(row.id)
                .setUri(row.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(row.title)
                        .setDisplayTitle(row.title)
                        .build(),
                )
                .build()
        }
        val start = initialIndex.coerceIn(0, mediaItems.lastIndex)
        val resume = store.getLong("resume_" + items[start].id, 0L)
        playback.setMediaItems(mediaItems, start, resume)
        if (autoPlay) {
            com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
            playback.play()
        }
    }

    LaunchedEffect(state.connected, state.playing, state.currentIndex) {
        if (!state.connected) return@LaunchedEffect
        while (true) {
            position = playback.positionMs
            val id = items.getOrNull(state.currentIndex)?.id
            if (id != null) store.putLong("resume_" + id, position)
            if (state.playing) phase += .13f
            delay(if (state.playing) 120L else 450L)
        }
    }

    DisposableEffect(queueKey) {
        com.hamyareman.ir.platform.feature.playback.TeachGate.enter()
        onDispose {
            val id = items.getOrNull(state.currentIndex)?.id
            if (id != null) store.putLong("resume_" + id, playback.positionMs)
            runCatching { playback.stop() }
            playback.release()
            com.hamyareman.ir.platform.feature.playback.TeachGate.exit()
        }
    }

    val duration = state.durationMs.coerceAtLeast(0L)
    val shown = if (scrub >= 0L) scrub else position
    val maxValue = maxOf(duration, shown, 1L)
    val current = items.getOrNull(state.currentIndex) ?: items.first()

    Column(
        modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (video) {
            Box(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.Black),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = playback.asPlayer()
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        }
                    },
                    update = { it.player = playback.asPlayer() },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { it.player = null },
                )
                if (state.buffering) {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = Color.White,
                    )
                }
            }
        } else {
            PremiumAudioArtwork(
                title = state.title.ifBlank { current.title },
                playing = state.playing,
                phase = phase,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }

        Text(
            state.title.ifBlank { current.title },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Slider(
            value = shown.coerceIn(0L, maxValue).toFloat(),
            onValueChange = { scrub = it.toLong() },
            onValueChangeFinished = {
                playback.seekTo(scrub.coerceAtLeast(0L))
                position = scrub.coerceAtLeast(0L)
                scrub = -1L
            },
            valueRange = 0f..maxValue.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(mediaTime(shown), color = Color(0xFFC8D5E2), style = MaterialTheme.typography.labelSmall)
            Text(mediaTime(duration), color = Color(0xFFC8D5E2), style = MaterialTheme.typography.labelSmall)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { playback.seekBy(-10_000L) }) {
                Icon(Icons.Default.Replay10, contentDescription = "ده ثانیه عقب", tint = Color.White)
            }
            IconButton(
                onClick = { playback.seekToPrevious() },
                enabled = state.mediaCount > 1,
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "قبلی", tint = Color.White)
            }
            IconButton(
                onClick = {
                    com.hamyareman.ir.platform.feature.playback.TeachGate.pulse()
                    if (state.playing) playback.pause() else playback.play()
                },
                modifier = Modifier.size(64.dp).clip(CircleShape).background(
                    Brush.radialGradient(listOf(Color(0xFF56C7C0), Color(0xFF164A5A))),
                ),
            ) {
                Icon(
                    if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.playing) "مکث" else "پخش",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(
                onClick = { playback.seekToNext() },
                enabled = state.mediaCount > 1,
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "بعدی", tint = Color.White)
            }
            IconButton(onClick = { playback.seekBy(10_000L) }) {
                Icon(Icons.Default.Forward10, contentDescription = "ده ثانیه جلو", tint = Color.White)
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { playback.setShuffle(!state.shuffleEnabled) }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "تصادفی",
                    tint = if (state.shuffleEnabled) Color(0xFF7FE0D4) else Color.White,
                )
            }
            IconButton(
                onClick = {
                    val player = playback.asPlayer()
                    if (player != null) {
                        player.repeatMode =
                            if (player.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF
                            else Player.REPEAT_MODE_ONE
                    }
                },
            ) {
                Icon(Icons.Default.RepeatOne, contentDescription = "تکرار", tint = Color.White)
            }
            listOf(.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                FilterChip(
                    selected = kotlin.math.abs(state.speed - speed) < .01f,
                    onClick = { playback.setSpeed(speed) },
                    label = { Text(speed.toString() + "x") },
                )
            }
            IconButton(
                onClick = {
                    val player = playback.asPlayer() ?: return@IconButton
                    muted = !muted
                    player.volume = if (muted) 0f else 1f
                },
            ) {
                Icon(
                    if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (muted) "رفع صدا" else "قطع صدا",
                    tint = Color.White,
                )
            }
        }

        if (state.buffering) {
            Text("در حال بارگذاری رسانه…", color = Color(0xFFB9C9D8), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PremiumAudioArtwork(
    title: String,
    playing: Boolean,
    phase: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF213D55), Color(0xFF0B1725)))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = minOf(size.width, size.height) * .22f
            repeat(42) { i ->
                val angle = (Math.PI * 2.0 * i / 42.0).toFloat()
                val wave = if (playing) ((sin(phase + i * .62f) + 1f) / 2f) else .12f
                val inner = base + wave * 15f
                val outer = inner + 9f + wave * 30f
                drawLine(
                    color = Color(0x995CCFC4),
                    start = androidx.compose.ui.geometry.Offset(
                        cx + cos(angle) * inner,
                        cy + sin(angle) * inner,
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        cx + cos(angle) * outer,
                        cy + sin(angle) * outer,
                    ),
                    strokeWidth = 4.2f,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFF4BBEB4), Color(0xFF143849))),
                radius = base,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
        }
        Text(
            title.take(24),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            modifier = Modifier.padding(22.dp),
        )
    }
}

private fun mediaTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return com.hamyareman.ir.platform.core.common.toPersianDigits(
        "%d:%02d".format(java.util.Locale.US, seconds / 60L, seconds % 60L),
    )
}
