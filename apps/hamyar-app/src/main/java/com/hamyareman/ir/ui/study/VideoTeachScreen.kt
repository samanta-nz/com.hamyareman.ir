package com.hamyareman.ir.ui.study

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * «ویدیوی تدریس» (v1.18) — صفحه‌ی مجزا و تمام‌صفحه برای ویدیوی هر درس:
 *  - یک ویدیو قراردادی برای هر پک: `<packId با خط‌تیره>-V01.mp4`؛
 *  - تمام‌صفحه (نوار وضعیت/ناوبری مخفی) + کنترل‌های خود پلیر (جوست، توقف/پخش)؛
 *  - سرعت با چیپ‌ها — قفلِ سرعتِ تند مثل صوت: تا اتمام دوره‌ی اول، فقط ۱x؛
 *  - حافظه‌ی موقعیت: هر ۵ ثانیه و هنگام خروج ذخیره؛ موقع بازشدن ادامه می‌دهد؛
 *  - آمار غیرقابل‌ویرایش: نشست، ثانیه‌ی تماشا، پرش‌های >۳ ثانیه، پایان ≥۹۵٪
 *    (همان رسانه‌ی ویدیوی دوره‌ی اول تدریس) — با هر ثبت، صف سینک سرور هم پر می‌شود.
 */
@Composable
fun VideoTeachScreen(packId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val store = remember { LocalStore(context, "hamyar_teach") }
    val scope = rememberCoroutineScope()
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    if (pack != null && LessonAccess.gate(context, pack.bookCode, packId) == LessonAccess.Gate.NeedSub) {
        NeedSubScreen(onBack = onBack)
        return
    }
    val fileId = remember(packId) { StudyMedia.videoIds(packId).firstOrNull() ?: "${packId.replace("_", "-")}-V01.mp4" }

    // --- تمام‌صفحه: مخفی‌کردن نوار وضعیت/ناوبری تا وقتی صفحه باز است ---
    val activity = LocalActivity.current
    // v1.19: دکمه‌ی فول‌اسکرین خود پلیر — چرخش افقی و حذف کنترل‌های بالایی
    var fullscreen by remember { mutableStateOf(false) }
    val startedLandscape = remember {
        activity?.resources?.configuration?.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }
    androidx.compose.runtime.LaunchedEffect(fullscreen) {
        activity?.requestedOrientation = when {
            fullscreen -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            startedLandscape -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    DisposableEffect(Unit) {
        val window = activity?.window
        val decor = window?.decorView
        val oldSystemUi = decor?.systemUiVisibility ?: 0
        if (decor != null) {
            decor.systemUiVisibility =
                oldSystemUi or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        onDispose {
            if (decor != null) decor.systemUiVisibility = oldSystemUi
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    androidx.activity.compose.BackHandler { onBack() }

    // --- منبع پخش: سرور یا گاوصندوق محلی ---
    var useLocal by remember(packId) { mutableStateOf(MediaVault.isCached(context, fileId)) }
    var downloading by remember { mutableStateOf(false) }
    var progressPct by remember { mutableIntStateOf(-1) }
    var cacheTick by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<String?>(null) }

    val remoteUris = StudyMedia.candidateUrls(fileId)
    var remoteIndex by remember(fileId) { mutableIntStateOf(0) }
    val uri = if (cacheTick >= 0 && useLocal && MediaVault.isCached(context, fileId)) {
        MediaVault.localUrl(context, fileId)
    } else {
        remoteUris.getOrElse(remoteIndex) { remoteUris.first() }
    }

    // --- سرعت (قفل تا اتمام دوره‌ی اول — همان قانون صوت) ---
    var speed by remember(packId) {
        val saved = store.getString("vid_${packId}_speed", "1").toFloatOrNull() ?: 1f
        mutableFloatStateOf(if (TEACH_SPEEDS.contains(saved)) saved else 1f)
    }

    // --- پلیر ---
    var player by remember(fileId, uri) { mutableStateOf<ExoPlayer?>(null) }
    var watchAccum by remember(fileId, uri) { mutableLongStateOf(0L) }
    var posMs by remember(fileId, uri) {
        mutableLongStateOf(store.getString("vid_${packId}_pos", "0").toLongOrNull() ?: 0L)
    }

    LaunchedEffect(packId) { TeachStats.enter(context, packId) }

    // v1.25 — خروج از صفحه‌ی ویدیو (هوم/پنجره‌ها/قفل صفحه) = مکث پخش.
    PauseOnStopEffect(pause = { player?.pause() })

    DisposableEffect(fileId, uri) {
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                com.hamyareman.ir.platform.feature.playback.vaultAwareMediaSourceFactory(context),
            )
            .build().apply {
            setMediaItem(
                MediaItem.Builder().setUri(uri).setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(pack?.title ?: "ویدیوی تدریس").build(),
                ).build(),
            )
            setPlaybackSpeed(speed)
            if (posMs > 3000L) seekTo(posMs)
            prepare()
            playWhenReady = true
        }
        player = p
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    TeachStats.markTrackDone(context, packId, fileId)
                    store.putString("vid_${packId}_pos", "0")
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!useLocal && remoteIndex < remoteUris.lastIndex) {
                    posMs = p.currentPosition.coerceAtLeast(posMs)
                    remoteIndex++
                    msg = "سرور سریع‌تر پاسخ نداد؛ پخش از سرور دوم ادامه پیدا می‌کند."
                } else {
                    msg = "پخش آنلاین ممکن نشد؛ اتصال یا سرور انتخاب‌شده را بررسی کنید."
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                    abs(newPosition.positionMs - oldPosition.positionMs) > 3000
                ) {
                    TeachStats.addJump(context, packId)
                }
            }
        }
        p.addListener(listener)
        onDispose {
            val pos = p.currentPosition
            if (pos > 0) store.putString("vid_${packId}_pos", pos.toString())
            p.removeListener(listener)
            p.release()
            player = null
        }
    }

    // تیک تماشای واقعی: هر ثانیه هنگام پخش؛ ثبت هر ۵ ثانیه؛ ≥۹۵٪ = پایان دوره + ذخیره‌ی موقعیت.
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            delay(1000)
            if (store.getString("quiet_mode", "0") == "1") {
                if (p.isPlaying) p.pause()
                continue
            }
            p.volume = 1f
            if (p.isPlaying) {
                watchAccum += 1000
                val durSec = (p.duration.takeIf { it > 0 } ?: 0L) / 1000
                if (watchAccum % 5000L == 0L) {
                    TeachStats.addVideo(context, packId, 5, durSec.toInt())
                    val pos = p.currentPosition
                    if (pos > 0) store.putString("vid_${packId}_pos", pos.toString())
                    val d = p.duration
                    if (d > 0 && p.currentPosition * 100 / d >= 95) {
                        TeachStats.markTrackDone(context, packId, fileId)
                    }
                    runCatching { TeachCloud.push(appContainer.sync) }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (!fullscreen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "🎬 ویدیوی تدریس",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    pack?.title ?: packId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        }

        PlayerViewHost(player, fullscreen = fullscreen, onToggleFullscreen = { fullscreen = !fullscreen })

        if (!fullscreen) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
            // سرعت‌ها — قفلِ تازمانِ اتمام دوره‌ی اول
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("سرعت:", style = MaterialTheme.typography.bodySmall)
                TEACH_SPEEDS.forEach { v ->
                    FilterChip(
                        selected = speed == v,
                        onClick = {
                            speed = v
                            store.putString("vid_${packId}_speed", v.toString())
                            player?.setPlaybackSpeed(v)
                        },
                        label = { Text("${toPersianDigits(if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString())}x") },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            if (downloading) {
                LinearProgressIndicator(
                    progress = { (if (progressPct < 0) 0 else progressPct) / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                TextButton(onClick = { useLocal = false; msg = null }) { Text("🌐 از سرور") }
                when {
                    downloading -> Unit
                    MediaVault.isCached(context, fileId) -> {
                        Text("✓ روی گوشی", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (!useLocal) {
                            TextButton(onClick = { useLocal = true; cacheTick++ }) { Text("▶ محلی") }
                        }
                        var confirmDelV by remember { mutableStateOf(false) }
                        if (confirmDelV) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { confirmDelV = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        confirmDelV = false
                                        MediaVault.delete(context, fileId)
                                        useLocal = false; cacheTick++
                                        msg = "ویدیو از حافظه‌ی گوشی حذف شد."
                                    }) { Text("حذف") }
                                },
                                dismissButton = { TextButton(onClick = { confirmDelV = false }) { Text("نگه‌دار") } },
                                title = { Text("حذف ویدیوی دانلودشده؟") },
                                text = { Text("پخش بعدی از سرور انجام می‌شود.") },
                            )
                        }
                        TextButton(onClick = { confirmDelV = true }) { Text("🗑") }
                    }
                    else -> TextButton(onClick = {
                        downloading = true; progressPct = -1
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    MediaVault.downloadEncrypted(context, StudyMedia.candidateUrls(fileId), fileId) { p, t -> progressPct = if (t > 0) ((p * 100) / t).toInt() else -1 }
                                }
                                downloading = false; cacheTick++; useLocal = true
                                msg = "دانلود شد — پخش محلی رمزشده."
                            } catch (e: Exception) {
                                downloading = false
                                msg = "دانلود ناموفق بود."
                            }
                        }
                    }) { Text("⬇ دانلود") }
                }
                msg?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        }
    }
}

@Composable
private fun PlayerViewHost(player: ExoPlayer?, fullscreen: Boolean, onToggleFullscreen: () -> Unit) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                // دکمه‌ی فول‌اسکرین داخل کنترل‌های خود پلیر
                setFullscreenButtonClickListener { onToggleFullscreen() }
            }
        },
        update = { view ->
            view.player = player
            view.setFullscreenButtonClickListener { onToggleFullscreen() }
        },
        modifier = if (fullscreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        },
    )
}
