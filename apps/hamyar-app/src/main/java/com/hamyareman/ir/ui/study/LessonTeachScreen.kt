package com.hamyareman.ir.ui.study

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.clickable
import kotlin.math.roundToInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hamyareman.ir.R
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.feature.study.StudyPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs

/** سرعت‌های پخش v1.9 — ترتیب کاربر: x2/x1.5/x1/x0.75/x0.5 (زیر نوار سیک). */
internal val TEACH_SPEEDS = listOf(2f, 1.5f, 1f, 0.75f, 0.5f)

// فونت‌های قراردادی خود پلیر؛ به انتخاب فونت عمومی صفحه وابسته نیستند.
private val TeachVazirmatnRegular = FontFamily(Font(R.font.vazirmatn_regular, FontWeight.Normal))
private val TeachVazirmatnBold = FontFamily(Font(R.font.vazirmatn_bold, FontWeight.Bold))

/** یک فایل صوتی قابل‌پخش در صفحه‌ی تدریس/خلاصه‌ها. */
internal data class TeachTrack(val label: String, val fileId: String, val cacheKey: String)

/**
 * v1.17 — قرارداد سراسری رسانه‌ها: هر پک «یک صوت» دارد:
 *  - اگر audioFileId پک پر باشد (authored/override حکایت سفر) از همان استفاده می‌شود؛
 *  - وگرنه نام قراردادی «<packId>_AUDIO.mp3» (در باکت برای همه‌ی ۲۰۹ پک موجود است).
 * ترک دوم (اینترو) حذف شد. هر ویدیو هم یک فایل قراردادی «<dash>-V01.mp4» است (StudyMedia).
 */
internal fun teachTracksOf(pack: StudyPack): List<TeachTrack> {
    val fid = pack.audioFileId.trim()
    if (fid.isBlank()) return emptyList()
    return listOf(TeachTrack("صوت درس", fid, fid))
}

/** شرط بازشدن مطالعه/سرعت تند = اتمام صوت تدریس؛ بدون فایل صوت قفل اعمال نمی‌شود. */
internal fun expectedTeachMedia(pack: StudyPack): Int = teachTracksOf(pack).size

/**
 * مقصد «لمس اعلان پخش» — سرویس رسانه PendingIntent به MainActivity می‌فرستد،
 * اینجا packId نگه داشته می‌شود و ZahraNavHost به صفحه‌ی تدریس همان درس می‌پرد.
 * قانون: صوت فقط داخل صفحه‌ی تدریس پخش می‌شود — پس پخش خودکار هم آنجا انجام می‌گیرد.
 */
object TeachLaunch {
    var pendingTeachPack by mutableStateOf<String?>(null)
}

/** سیک از فهرست HTML تدریس → پلیر بالای صفحه. */
object TeachSeekBus {
    var requestMs by mutableStateOf<Long?>(null)
    fun seekMs(ms: Long) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            requestMs = ms.coerceAtLeast(0L)
        }
    }
}

internal fun teachMmss(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000
    return toPersianDigits(String.format(Locale.US, "%d:%02d", s / 60, s % 60))
}

internal fun teachSpeedLabel(v: Float): String = when (v) {
    2f -> "×۲"
    1.5f -> "×۱٫۵"
    1f -> "×۱"
    0.75f -> "×۰٫۷۵"
    0.5f -> "×۰٫۵"
    else -> "${v}×"
}

/**
 * صفحه‌ی «تدریس» هر درس — طبق بازخورد مصوب:
 *  ۱) پلیر صوت «بالای صفحه»: تک‌پلیر (فقط یکی پخش می‌شود)، حافظه‌دار (موقعیت و
 *     سرعت هر ترک جداگانه)، با اعلان/کنترل قفل‌صفحه (سرویس رسانه)، پخش از سرور
 *     یا فایل رمزشده‌ی روی گوشی (دانلود/حذف)؛
 *  ۲) زیر پلیر «خود کتاب» باز می‌شود (PDF صفحه‌به‌صفحه) و صوت روی آن پخش می‌ماند؛
 *  ۳) بدون دکمه‌ی فلش‌کارت/آزمون/جزوه — مطالعه فقط بعد از اتمام اولین دوره‌ی
 *     تدریس از صفحه‌ی کتاب باز می‌شود؛
 *  ۴) همه‌ی رویدادها (نشست، ثانیه‌ی شنیدن، پرش، اتمام دوره) در TeachStats ثبت می‌شود.
 */
@Composable
fun LessonTeachScreen(
    packId: String,
    onBack: () -> Unit,
    onStudy: (String) -> Unit,
    onPdf: (String) -> Unit,
) {
    val pack = remember(packId) { BookModuleRegistry.pack(packId) }
    // v1.25 — نام کتاب برای اعلان پخش («نام کتاب و درس»).
    val bookTitle = remember(packId) {
        BookModuleRegistry.modules.firstOrNull { m -> m.packs.any { it.packId == packId } }?.title.orEmpty()
    }
    if (pack == null) {
        AppTopBar(title = "تدریس درس", onBack = onBack)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { Text("این درس پیدا نشد.") }
        return
    }
    val ctx = LocalContext.current
    if (LessonAccess.gate(ctx, pack.bookCode, packId) == LessonAccess.Gate.NeedSub) {
        NeedSubScreen(onBack = onBack)
        return
    }
    if (pack.pdfOnly) {
        // سند تمام‌صفحه است؛ کنترل برگشتِ جداگانه و عنوان بالا جای صفحه را نمی‌گیرند.
        // برگشت استاندارد سیستم/ناوبری همچنان کار می‌کند.
        Column(Modifier.fillMaxSize()) {
            TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
        }
        return
    }
    if (pack.bookCode == "C905") {
        MathLessonScreen(packId = packId, initialTab = 0, onBack = onBack)
        return
    }

    // صفحهٔ رسانه تمام‌صفحه است؛ پلیر در بالا می‌ماند و کتاب (PDF) زیرش اسکرول می‌شود.
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tracks = teachTracksOf(pack)
        if (tracks.isNotEmpty()) TeachAudioBar(packId = packId, screenTitle = pack.title, bookTitle = bookTitle, tracks = tracks)

        // v1.18: ویدیو به صفحه‌ی مجزای «ویدیوی تدریس» منتقل شد (VideoTeachScreen).
        TeachPdfPages(modifier = Modifier.weight(1f), fileId = pack.pdfFileName, pack = pack)
    }
}

/**
 * پلیر صوتِ بالای صفحه — v1.62:
 *  - موتور: ExoPlayer داخلیِ خودِ صفحه. سرویسِ رسانه/اعلان/سشن برای صوتِ تدریس لازم نیست —
 *    این صوت به‌هرحال نباید به پس‌زمینه برود؛ پس بدونِ سرویس، بدونِ دروازه و بدونِ واچ‌داگ.
 *  - فقط یک ترک در لحظه؛ چیپِ ترک = ادامه از موقعیتِ ذخیره‌شده‌ی همان ترک.
 *  - منبع: فایلِ رمزشده‌ی گاوصندوق (vault:// با رمزگشاییِ جسته‌گریخته) یا استریمِ سرور؛
 *    خطایِ محلی ⇒ یک‌بار تلاشِ بی‌صدای سرور برای همان ترک.
 *  - دانلود/حذفِ آفلاین مثلِ قبل؛ ولومِ دستگاه با آیکون میوت + اسلایدرِ لغزنده (سبکِ یوتیوب).
 *  - «زمان درس» فعال ⇒ پخشِ صدا قفل است (یک بنرِ کوچک با «خاموش کن»).
 *  - ثانیه‌ی شنیدن/پرش >۳ثانیه/اتمام دوره → TeachStats (غیرقابل ویرایش).
 */
@Composable
internal fun TeachAudioBar(packId: String, screenTitle: String, bookTitle: String, tracks: List<TeachTrack>) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_teach") }
    val scope = rememberCoroutineScope()

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(0L) }
    var loadedKey by remember { mutableStateOf<String?>(null) }
    var loadedLocal by remember { mutableStateOf(false) }
    var loadedRemoteIndex by remember { mutableIntStateOf(0) }

    var activeIdx by remember {
        mutableIntStateOf(store.getString("teach_${packId}_track", "0").toIntOrNull()?.coerceIn(0, tracks.size - 1) ?: 0)
    }
    var speed by remember {
        val saved = store.getString("teach_${packId}_speed", "1").toFloatOrNull() ?: 1f
        mutableFloatStateOf(if (TEACH_SPEEDS.contains(saved)) saved else 1f)
    }
    var downloading by remember { mutableStateOf(false) }
    var doneBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var cacheTick by remember { mutableIntStateOf(0) }
    var listenAccumMs by remember { mutableLongStateOf(0L) }
    var lastSaveMs by remember { mutableLongStateOf(0L) }
    var dragMs by remember { mutableLongStateOf(-1L) }
    var quiet by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var playerExpanded by remember { mutableStateOf(true) }
    var playerTouchTick by remember { mutableIntStateOf(0) }

    val track = tracks[activeIdx]
    LaunchedEffect(packId, tracks.size) {
        TeachStats.expectMedia(context, packId, tracks.size.coerceAtLeast(1))
    }

    fun posKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_pos"
    fun durKey(t: TeachTrack) = "teach_${packId}_${t.cacheKey}_dur"
    /** «تله‌ی انتها» — موقعیتِ بیرون از انتهای فایل (=۰) تا پخشِ بی‌صدا از انتها ممکن نشود. */
    fun savedPos(t: TeachTrack): Long {
        val p = store.getString(posKey(t), "0").toLongOrNull() ?: 0L
        if (p <= 0) return 0L
        val d = store.getString(durKey(t), "0").toLongOrNull() ?: 0L
        return if (d > 3000 && p >= d - 1500) 0L else p
    }
    fun savePos(t: TeachTrack, v: Long) { if (v > 0) store.putString(posKey(t), v.toString()) else store.remove(posKey(t)) }
    fun cached(t: TeachTrack) = cacheTick >= 0 && MediaVault.isVerified(context, t.cacheKey)

    /** «زمان درس» — سکوتِ اجباری پلیر دروس (کلید سراسری از «بیشتر»). */
    var quietTick by remember { mutableIntStateOf(0) }
    fun quietOn(): Boolean = quietTick.let { store.getString("quiet_mode", "0") == "1" }

    val appContainer = com.hamyareman.ir.LocalAppContainer.current
    LaunchedEffect(packId) {
        TeachStats.enter(context, packId)
        runCatching { TeachCloud.push(appContainer.sync) }
    }

    /** آیا صوتِ این درس واقعاً در دسترس است؟ null = در حال بررسی؛ false = نیست. */
    var availability by remember(packId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(packId, tracks.size) {
        availability = withContext(Dispatchers.IO) {
            tracks.any { t -> MediaVault.isVerified(context, t.cacheKey) || StudyMedia.audioExists(t.fileId) }
        }
    }

    fun remoteUris(t: TeachTrack): List<String> =
        StudyMedia.candidateUrls(StudyMedia.resolveFileId(t.fileId))

    fun uriFor(t: TeachTrack, preferLocal: Boolean, remoteIndex: Int = 0): String =
        if (preferLocal && MediaVault.isVerified(context, t.cacheKey)) MediaVault.localUrl(context, t.cacheKey)
        else remoteUris(t).let { urls -> urls.getOrElse(remoteIndex) { urls.first() } }

    DisposableEffect(packId) {
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                com.hamyareman.ir.platform.feature.playback.vaultAwareMediaSourceFactory(context),
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .build()
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                playing = p.isPlaying
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (p.duration > 0) {
                            durMs = p.duration
                            loadedKey?.let { k -> store.putString("teach_${packId}_${k}_dur", p.duration.toString()) }
                            if (p.currentPosition >= p.duration - 1500) {
                                p.seekTo(0L)
                                posMs = 0L
                                tracks.firstOrNull { it.cacheKey == loadedKey }?.let { savePos(it, 0L) }
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        playing = false
                        val key = loadedKey ?: track.cacheKey
                        TeachStats.markTrackDone(context, packId, key)
                        tracks.firstOrNull { it.cacheKey == key }?.let { savePos(it, 0L) }
                        posMs = 0L
                    }
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }

            override fun onPlayerError(error: PlaybackException) {
                val key = loadedKey ?: return
                val t = tracks.firstOrNull { it.cacheKey == key } ?: return
                val urls = remoteUris(t)
                val next = if (loadedLocal) 0 else loadedRemoteIndex + 1
                if (next !in urls.indices) {
                    note = "پخش از سرور انتخاب‌شده ممکن نشد."
                    return
                }
                val resumeAt = p.currentPosition.coerceAtLeast(posMs)
                scope.launch {
                    runCatching {
                        loadedLocal = false
                        loadedRemoteIndex = next
                        p.setMediaItem(MediaItem.Builder().setUri(urls[next]).setMediaId(key).build())
                        if (resumeAt > 0) p.seekTo(resumeAt)
                        p.prepare()
                        p.playWhenReady = true
                        if (next > 0) note = "پخش از سرور دوم ادامه پیدا کرد."
                    }
                }
            }
        })
        player = p
        onDispose {
            runCatching { p.stop() }
            runCatching { p.release() }
            player = null
            loadedKey = null
            playing = false
            posMs = 0L
            durMs = 0L
        }
    }

    // قانون: خروج از صفحه (هوم/پنجره‌ها/قفل) = توقفِ کامل — صوتِ تدریس هرگز پس‌زمینه نیست.
    PauseOnStopEffect(
        pause = { },
        stop = {
            player?.let { runCatching { it.stop() } }
            loadedKey = null
            playing = false
        },
    )

    fun startTrack(t: TeachTrack, autoplay: Boolean, startMs: Long? = null, preferLocal: Boolean = true) {
        if (availability == false) return
        if (autoplay && quietOn()) return
        val p = player ?: return
        val pos = startMs ?: if (preferLocal && cached(t)) savedPos(t) else 0L
        scope.launch {
            val uri = withContext(Dispatchers.IO) { uriFor(t, preferLocal = preferLocal) }
            loadedLocal = uri.startsWith("vault://")
            loadedRemoteIndex = 0
            loadedKey = t.cacheKey
            lastSaveMs = 0L
            posMs = pos
            runCatching {
                p.setMediaItem(
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaId(t.cacheKey)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(screenTitle)
                                .setArtist(bookTitle.ifBlank { t.label })
                                .build(),
                        )
                        .build(),
                )
                if (pos > 0) p.seekTo(pos)
                p.setPlaybackSpeed(speed)
                p.prepare()
                p.playWhenReady = autoplay
            }
        }
    }

    // درخواستِ «پخشِ همین درس» (مثلاً از جای دیگر) → همین‌جا شروع کن.
    LaunchedEffect(packId) {
        if (TeachLaunch.pendingTeachPack == packId) {
            TeachLaunch.pendingTeachPack = null
            startTrack(track, autoplay = true)
        }
    }

    // سیک از فهرست HTML تدریس (پلِ HamyarPlayer) → همان دقیقه‌ثانیه.
    val tocSeekMs = TeachSeekBus.requestMs
    LaunchedEffect(tocSeekMs) {
        val ms = tocSeekMs ?: return@LaunchedEffect
        TeachSeekBus.requestMs = null
        if (quietOn()) return@LaunchedEffect
        val p = player ?: return@LaunchedEffect
        if (abs(ms - posMs) > 3000) TeachStats.addJump(context, packId)
        savePos(track, ms)
        if (loadedKey == track.cacheKey) {
            p.seekTo(ms)
            posMs = ms
            if (!p.isPlaying) p.play()
        } else {
            startTrack(track, autoplay = true, startMs = ms)
        }
    }

    // نظرسنجیِ موقعیت + آمارِ شنیدن + ذخیره‌ی دوره‌ای + قفلِ «زمان درس».
    LaunchedEffect(playing, quietTick, loadedKey) {
        while (true) {
            delay(500)
            val p = player ?: continue
            if (quietOn()) {
                if (!quiet) {
                    quiet = true
                    if (p.isPlaying) { savePos(track, p.currentPosition); runCatching { p.pause() } }
                }
                continue
            }
            quiet = false
            if (!p.isPlaying) continue
            posMs = p.currentPosition
            if (p.duration > 0) durMs = p.duration
            listenAccumMs += 500
            if (listenAccumMs >= 5000) {
                TeachStats.addListen(context, packId, (listenAccumMs / 1000).toInt(), (durMs / 1000).toInt())
                listenAccumMs = 0
            }
            if (posMs - lastSaveMs >= 4000 || posMs < lastSaveMs) {
                savePos(track, posMs)
                lastSaveMs = posMs
            }
        }
    }
    // ≥۹۵٪ ثانیه‌ی شنیده‌شده = اتمامِ دوره، حتی بی‌رویدادِ پایان.
    LaunchedEffect(playing) {
        if (playing) return@LaunchedEffect
        val snap = TeachStats.snap(context, packId)
        if (snap.audioDurSec > 0 && snap.listenSec >= snap.audioDurSec * 95 / 100) {
            TeachStats.markTrackDone(context, packId, loadedKey ?: track.cacheKey)
        }
    }

    var volOpen by remember { mutableStateOf(false) }

    LaunchedEffect(playerTouchTick, playing, downloading, volOpen, activeIdx) {
        delay(5000)
        playerExpanded = false
    }

    // --- ولومِ دستگاه به سبکِ یوتیوب: آیکون میوت + اسلایدرِ لغزنده (بدون سطرِ جدید) ---
    val am = remember { context.getSystemService(android.media.AudioManager::class.java) }
    val sysMax = remember { runCatching { am?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15 }.getOrDefault(15) }
    var sysVol by remember { mutableIntStateOf(runCatching { am?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 5 }.getOrDefault(5)) }
    var lastUnmuted by remember { mutableIntStateOf(if (sysVol > 0) sysVol else (sysMax / 2).coerceAtLeast(1)) }
    var volTick by remember { mutableIntStateOf(0) }
    fun applyVol(v: Int) {
        sysVol = v.coerceIn(0, sysMax)
        if (sysVol > 0) lastUnmuted = sysVol
        runCatching { am?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, sysVol, 0) }
    }
    LaunchedEffect(volOpen, volTick) { if (volOpen) { delay(4600); volOpen = false } }
    // پیامِ کوتاه (نتیجه‌ی دانلود) خودش بعدِ چند ثانیه پاک می‌شود.
    LaunchedEffect(note) { if (note != null) { delay(6000); note = null } }
    // کلیدهایِ فیزیکیِ ولوم → همگام‌سازیِ آیکون/اسلایدر.
    LaunchedEffect(Unit) {
        while (true) {
            val v = runCatching { am?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) }.getOrNull()
            if (v != null && v != sysVol) { sysVol = v; if (v > 0) lastUnmuted = v }
            delay(600)
        }
    }

    Card(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) { playerExpanded = true; playerTouchTick++ }
                    }
                }
            },
    ) {
        if (!playerExpanded) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { playerExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = "باز کردن پلیر",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("نمایش کنترل‌های پخش", style = MaterialTheme.typography.labelLarge)
            }
        } else {
        Column(Modifier.padding(10.dp)) {
            // انتخاب ترک — فقط وقتی درس چند صوت دارد.
            if (tracks.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tracks.forEachIndexed { i, t ->
                        FilterChip(
                            selected = i == activeIdx,
                            onClick = {
                                if (i != activeIdx) {
                                    savePos(track, posMs)
                                    activeIdx = i
                                    store.putString("teach_${packId}_track", i.toString())
                                    startTrack(tracks[i], autoplay = playing)
                                }
                            },
                            label = { Text(t.label) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = {
                    playerExpanded = true
                    val p = player ?: return@OutlinedButton
                    if (playing) {
                        runCatching { p.pause() }
                        savePos(track, p.currentPosition)
                    } else if (quietOn()) {
                        // «زمان درس» — پخش نداریم؛ بنرِ زیر، راهِ خاموش‌کردن را نشان می‌دهد.
                    } else if (loadedKey == track.cacheKey) {
                        // بعدِ دانلود/حذف، منبعِ آیتمِ فعلی عوض شده ⇒ دوباره در صف بگذار.
                        if (loadedLocal != cached(track)) startTrack(track, autoplay = true) else runCatching { p.play() }
                    } else {
                        startTrack(track, autoplay = true)
                    }
                }) { Text(if (playing) "⏸ توقف" else "▶ پخش") }
                if (buffering && !playing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                }
                // منبع + دانلود/حذف آفلاین.
                val online = !cached(track)
                val srcColor = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                Icon(
                    if (online) Icons.Outlined.Cloud else Icons.Outlined.Smartphone,
                    contentDescription = null,
                    tint = srcColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    if (online) "پخش آنلاین" else "پخش آفلاین",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = TeachVazirmatnRegular,
                        fontWeight = FontWeight.Normal,
                        fontSize = (MaterialTheme.typography.labelMedium.fontSize.value - 2f).coerceAtLeast(8f).sp,
                    ),
                    color = srcColor,
                    maxLines = 1,
                )
                when {
                    downloading -> {
                        // جزئیاتِ حجم/درصد به «بالای» نوارِ وضعیت منتقل شد (بلوکِ زیرِ ردیف).
                    }
                    cached(track) -> {
                        var confirmDelete by remember { mutableStateOf(false) }
                        if (confirmDelete) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        confirmDelete = false
                                        MediaVault.delete(context, track.cacheKey)
                                        cacheTick++
                                    }) { Text("حذف") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("نگه‌دار") }
                                },
                                title = { Text("حذف فایل آفلاین؟") },
                                text = { Text("فایل از حافظه‌ی گوشی پاک می‌شود و پخش بعدی آنلاین انجام می‌گیرد؛ هر وقت خواستی دوباره دانلود می‌کنی.") },
                            )
                        }
                        TextButton(
                            onClick = { playerExpanded = true; confirmDelete = true },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "حذف آفلاین",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = TeachVazirmatnRegular,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = (MaterialTheme.typography.labelMedium.fontSize.value - 6f).coerceAtLeast(7f).sp,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                    else -> TextButton(onClick = {
                        playerExpanded = true
                        downloading = true; doneBytes = 0L; totalBytes = 0L
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val remoteId = StudyMedia.resolveFileId(track.fileId)
                                    MediaVault.downloadEncrypted(
                                        context,
                                        StudyMedia.candidateUrls(remoteId),
                                        track.cacheKey,
                                    ) { done, total ->
                                        doneBytes = done
                                        totalBytes = total
                                    }
                                }
                                note = "دانلود کامل شد؛ پخشِ بعدی آفلاین است."
                                // دانلود که تمام، آیتمِ آنلاینِ کهنه را با همان ترکِ محلی عوض کن.
                                if (loadedKey == track.cacheKey) startTrack(track, autoplay = playing)
                            } catch (e: Exception) {
                                android.util.Log.w("TeachVault", "download failed: ${track.fileId}", e)
                                note = "دانلود کامل نشد؛ دوباره تلاش کن."
                            }
                            downloading = false
                            cacheTick++
                        }
                    }) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("دانلود", maxLines = 1)
                    }
                }
                Spacer(Modifier.weight(1f))
                // ولوم به سبکِ یوتیوب: وقتی نوار بسته است، لمسِ آیکون فقط نوار را باز
                // می‌کند (بی‌صدا نمی‌کند)؛ میوت/لغوِ میوت فقط وقتی نوار باز است.
                Box(
                    Modifier
                        .clickable {
                            if (volOpen) {
                                if (sysVol > 0) { lastUnmuted = sysVol; applyVol(0) } else applyVol(lastUnmuted.coerceAtLeast(1))
                                volTick++
                            } else {
                                volOpen = true
                            }
                        }
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (sysVol == 0) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (sysVol == 0) "بی‌صدا" else "صدایِ دستگاه",
                        modifier = Modifier.size(20.dp),
                        tint = if (sysVol == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = volOpen,
                    enter = androidx.compose.animation.expandHorizontally() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkHorizontally() + androidx.compose.animation.fadeOut(),
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Ltr,
                    ) {
                        Slider(
                            value = sysVol.toFloat(),
                            onValueChange = { applyVol(it.roundToInt()) },
                            valueRange = 0f..sysMax.toFloat(),
                            steps = sysMax - 1,
                            modifier = Modifier.width(125.dp).height(26.dp),
                        )
                    }
                }
            }
            if (downloading) {
                // «حجم و درصدِ دانلود» بالای نوارِ وضعیت، با فونتِ ۱٫۵ برابر
                Text(
                    if (totalBytes > 0) {
                        toPersianDigits(((doneBytes * 100L) / totalBytes).toString()) + "٪ — " +
                            humanSize(doneBytes) + " از " + humanSize(totalBytes)
                    } else {
                        humanSize(doneBytes)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = TeachVazirmatnBold,
                        fontWeight = FontWeight.Bold,
                        fontSize = (MaterialTheme.typography.bodySmall.fontSize.value + 2f).coerceAtLeast(12f).sp,
                    ),
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        if (totalBytes > 0) (doneBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.height(6.dp))
            }
            if (quietOn()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "\uD83D\uDD07 «زمان درس» روشن است",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        store.putString("quiet_mode", "0")
                        quietTick++
                    }) { Text("خاموش کن") }
                }
            }
            if (availability == false) {
                Text(
                    "صوت این درس هنوز روی سرور نیست — متن تدریس در دسترس است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = {
                    tracks.forEach { StudyMedia.forgetMissing(it.fileId) }
                    availability = null
                }) { Text("بررسی دوباره") }
            }
            note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            if (durMs > 0) {
                Slider(
                    value = ((if (dragMs >= 0) dragMs else posMs).toFloat() / durMs).coerceIn(0f, 1f),
                    onValueChange = { dragMs = (it * durMs).toLong() },
                    onValueChangeFinished = {
                        if (dragMs >= 0) {
                            if (abs(dragMs - posMs) > 3000) TeachStats.addJump(context, packId)
                            player?.seekTo(dragMs)
                            posMs = dragMs
                            savePos(track, dragMs)
                            dragMs = -1
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(26.dp),
                )
            }
            // زمان یک اسلات با عرض ثابت دارد؛ نه تغییر ثانیه و نه تفاوت عرض رقم‌ها
            // نمی‌تواند محل کنترل‌های سرعت را جابه‌جا کند. ترتیبِ زمان عمداً LTR است.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
                ) {
                    Box(Modifier.width(132.dp), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            if (durMs > 0) {
                                "${teachMmss(if (dragMs >= 0) dragMs else posMs)} / ${teachMmss(durMs)}"
                            } else {
                                "--:-- / --:--"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = TeachVazirmatnBold,
                                fontWeight = FontWeight.Bold,
                                fontSize = (MaterialTheme.typography.bodySmall.fontSize.value + 2f).coerceAtLeast(12f).sp,
                                fontFeatureSettings = "tnum",
                                textDirection = TextDirection.Ltr,
                            ),
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TEACH_SPEEDS.forEach { v ->
                        FilterChip(
                            selected = speed == v,
                            onClick = {
                                speed = v
                                runCatching { player?.setPlaybackSpeed(v) }
                                store.putString("teach_${packId}_speed", v.toString())
                            },
                            label = {
                                Text(
                                    teachSpeedLabel(v),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = (MaterialTheme.typography.labelMedium.fontSize.value - 2f).coerceAtLeast(9f).sp,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
        }
    }
}

// ------------------------------------------------------------- کتاب (PDF)

private fun Modifier.androidClickable(onClick: () -> Unit): Modifier =
    this.pointerInput(Unit) { detectTapGestures { onClick() } }

private sealed class TeachPdfState {
    data object Idle : TeachPdfState()
    data class Downloading(val pct: Int) : TeachPdfState()
    data class Ready(val pageCount: Int) : TeachPdfState()
    data class Error(val message: String) : TeachPdfState()
}

/** خطای قابل‌گزارش (دانلود/اعتبارسنجی) — پیامش مستقیم به کاربر نشان داده می‌شود. */
private class PdfUnavailable(message: String) : Exception(message)

/** دانلود/اعتبارسنجی/بازکردن PDF — فقط روی IO صدا زده می‌شود. */
private fun openTeachPdf(ctx: android.content.Context, fileId: String, onProgress: (Int) -> Unit): PdfRenderer {
    val target = try {
        StudyPdfCache.obtain(ctx, fileId, onProgress)
    } catch (error: Throwable) {
        throw PdfUnavailable(error.message ?: "دریافت PDF ممکن نشد.")
    }
    val fd = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
    return PdfRenderer(fd)
}

/** کش LRU صفحه‌های PDF (~۴ صفحهٔ نزدیک). بیت‌مپِ بیرون‌رفته recycle می‌شود. */
private class TeachPageCache(private val maxPages: Int = 4) {
    private val map = LinkedHashMap<Int, Bitmap>(16, 0.75f, true)

    operator fun get(index: Int): Bitmap? = synchronized(this) {
        val b = map[index] ?: return null
        if (b.isRecycled) {
            map.remove(index)
            null
        } else b
    }

    operator fun set(index: Int, value: Bitmap) {
        synchronized(this) {
            val old = map.put(index, value)
            if (old != null && old !== value && !old.isRecycled) old.recycle()
            while (map.size > maxPages) {
                val k = map.keys.first()
                val evicted = map.remove(k)
                if (evicted != null && evicted !== value && !evicted.isRecycled) evicted.recycle()
            }
        }
    }

    fun evictAll(keep: Bitmap? = null) {
        synchronized(this) {
            val it = map.entries.iterator()
            while (it.hasNext()) {
                val b = it.next().value
                if (b !== keep && !b.isRecycled) b.recycle()
                it.remove()
            }
        }
    }
}

/**
 * کتابِ درس — دانلود PDF از باکت و رندر صفحه‌به‌صفحه (PdfRenderer).
 * اگر PDF هنوز روی سرور نبود، متن سکشن‌های غیرامتحانی جایگزین می‌شود.
 */
@Composable
internal fun TeachPdfPages(
    modifier: Modifier = Modifier,
    fileId: String,
    pack: StudyPack,
    /** زوم داخلِ صفحهٔ PDF: سوایپِ سربرگ/صفحه باید تا وقتی زوم است قفل شود. */
    onZoomChange: (Boolean) -> Unit = {},
) {
    val ctx = LocalContext.current
    var state by remember(fileId) { mutableStateOf<TeachPdfState>(TeachPdfState.Idle) }
    val pageCache = remember(fileId) { TeachPageCache() }
    var renderer by remember(fileId) { mutableStateOf<PdfRenderer?>(null) }
    val renderLock = remember(fileId) { Any() }

    DisposableEffect(fileId) {
        onDispose {
            pageCache.evictAll()
            synchronized(renderLock) {
                runCatching { renderer?.close() }
                renderer = null
            }
        }
    }

    LaunchedEffect(fileId) {
        if (fileId.isBlank()) {
            state = TeachPdfState.Error("برای این بخش، کتابِ PDF جداگانه‌ای نیست.")
            return@LaunchedEffect
        }
        state = TeachPdfState.Downloading(0)
        try {
            // PdfRenderer فقط از یک نخ باید دیده شود — همهٔ کار روی IO.
            val r = withContext(Dispatchers.IO) {
                openTeachPdf(ctx, fileId) { pct ->
                    state = TeachPdfState.Downloading(pct)
                }
            }
            synchronized(renderLock) { renderer = r }
            state = TeachPdfState.Ready(r.pageCount)
        } catch (e: PdfUnavailable) {
            state = TeachPdfState.Error(e.message ?: "PDF در دسترس نیست.")
        } catch (oom: OutOfMemoryError) {
            state = TeachPdfState.Error("حافظه برای بازکردن کتاب کافی نبود؛ یک‌بار دیگر تلاش کن.")
        } catch (e: Exception) {
            // کش دانلودشده را پاک نکن — شاید رندر مشکل داشت نه فایل.
            state = TeachPdfState.Error("بازکردن PDF ناموفق بود؛ دوباره تلاش کن.")
        }
    }

    when (val st = state) {
        is TeachPdfState.Error -> {
            Column(
                modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("📕 کتاب درس", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(st.message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "تا آپلودشدن PDF، متن درس را همین‌جا می‌بینی:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                pack.sections.filter { it.kind != "exam" }.forEach { sec ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(sec.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(sec.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        is TeachPdfState.Downloading -> Card(modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("در حال آماده‌سازی کتاب… (${toPersianDigits(st.pct.toString())}٪)", style = MaterialTheme.typography.bodySmall)
            }
        }
        is TeachPdfState.Ready -> Column(modifier = modifier) {
            var pageIdx by remember(fileId) { mutableIntStateOf(0) }
            val seenPages = remember(fileId) { mutableSetOf<Int>() }
            val screenW = remember {
                ctx.resources.displayMetrics.widthPixels.coerceIn(640, 1080)
            }
            val index = pageIdx.coerceIn(0, (st.pageCount - 1).coerceAtLeast(0))
            Text(
                "📕 کتاب درس — صفحه ${toPersianDigits((index + 1).toString())} از ${toPersianDigits(st.pageCount.toString())}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(6.dp))
            var bmp by remember(fileId, index) { mutableStateOf<Bitmap?>(pageCache[index]) }
            LaunchedEffect(fileId, index) {
                val current = bmp
                if (current != null && !current.isRecycled) return@LaunchedEffect
                val rendered: Bitmap? = withContext(Dispatchers.IO) {
                    fun once(maxW: Int): Bitmap? {
                        val raw = synchronized(renderLock) {
                            val r = renderer ?: return@synchronized null
                            r.openPage(index).use { page ->
                                PdfSafe.renderPage(page, maxW = maxW)
                            }
                        } ?: return null
                        val deg = com.hamyareman.ir.platform.feature.study.PdfRotations.degrees[fileId] ?: 0
                        return PdfSafe.rotate(raw, deg)
                    }
                    try {
                        once(screenW) ?: run {
                            pageCache.evictAll()
                            once((screenW / 2).coerceAtLeast(320))
                        }
                    } catch (_: OutOfMemoryError) {
                        pageCache.evictAll()
                        runCatching { once((screenW / 2).coerceAtLeast(320)) }.getOrNull()
                    } catch (_: Exception) {
                        null
                    }
                }
                if (rendered != null) {
                    pageCache[index] = rendered
                    bmp = rendered
                    if (seenPages.add(index)) {
                        StudyActivity.add(ctx, pack.packId, "pdf", "مشاهده صفحه ${index + 1} کتاب درسی")
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (bmp == null) {
                    CircularProgressIndicator()
                } else {
                    ZoomablePdfPage(bitmap = bmp!!, modifier = Modifier.fillMaxSize())
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { pageIdx = (index - 1).coerceAtLeast(0) },
                    enabled = index > 0,
                    modifier = Modifier.weight(1f),
                ) { Text("صفحه قبل") }
                OutlinedButton(
                    onClick = { pageIdx = (index + 1).coerceAtMost(st.pageCount - 1) },
                    enabled = index < st.pageCount - 1,
                    modifier = Modifier.weight(1f),
                ) { Text("صفحه بعد") }
            }
        }
        TeachPdfState.Idle -> Card(modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

/** حجمِ خوانا برای نوارِ دانلود (مگابایت/کیلوبایت با ارقام فارسی). */
internal fun humanSize(bytes: Long): String {
    val mb = bytes / 1_048_576.0
    return if (mb >= 1) toPersianDigits("%.1f".format(mb)) + " مگابایت"
    else toPersianDigits((bytes / 1024).coerceAtLeast(0).toString()) + " کیلوبایت"
}
