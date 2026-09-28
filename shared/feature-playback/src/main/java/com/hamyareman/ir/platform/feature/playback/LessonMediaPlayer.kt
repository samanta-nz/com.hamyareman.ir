package com.hamyareman.ir.platform.feature.playback

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * پرامپت ۰۱ — کنترلر مشترک ویدیو + صوت.
 *
 * چرا کنترلر جدای `PlaybackController`:
 *  - این کنترلر **هم ویدیو و هم صوت** را پشتیبانی می‌کند.
 *  - **حافظه‌ی پیشرفت**: موقع setMedia، lastPositionSec را از [progressRepo] می‌خواند
 *    و پخش از همان‌جا شروع می‌شود.
 *  - **سینک خودکار**: یک Coroutine هر [PROGRESS_TICK_MS] میلی‌ثانیه (پیش‌فرض ۵ ثانیه) و
 *    روی رویدادهای مکث/پایان/seek بزرگ/تغییر سرعت، [LessonMediaProgressRepository.recordProgress] را صدا می‌زند.
 *  - **debounce**: اگر در ۲۵۰ میلی‌ثانیه‌ی اخیر چند رویداد seek آمده باشد، فقط آخری ثبت می‌شود.
 *  - **تماشا تا انتها**: وقتی به بالای ۹۰٪ رسید، isCompleted=true و یک ViewEvent با
 *    isReplay=viewCount>1 ثبت می‌شود.
 *  - **Media Session**: این کنترلر خودش ExoPlayer می‌سازد (نه از طریق Service). اگر
 *    پلیر در یک صفحه‌ی Compose استفاده شود، [release] فقط پلیر را می‌بندد؛ اگر
 *    پشتیبانی از background نیاز باشد، از [PlaybackService] استفاده می‌شود.
 */
@UnstableApi
class LessonMediaPlayer(
    context: Context,
    private val progressRepo: LessonMediaProgressRepository,
    private val userId: () -> String?,
) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var tickJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var current: MediaContext? = null
    private var lastReportedPositionMs: Long = 0L
    private var lastReportAt: Long = 0L
    private var lastSeekReportAt: Long = 0L

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    data class MediaContext(
        val uri: String,
        val title: String,
        val bookCode: String,
        val lessonId: String,
        val mediaType: LessonMediaProgressRepository.MediaType,    val chapterUrls: List<String> = emptyList(),
    )

    data class PlayerState(
        val connected: Boolean = false,
        val playing: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val speed: Float = 1f,
        val bufferedFraction: Float = 0f,
        val mediaType: LessonMediaProgressRepository.MediaType? = null,
        val bookCode: String = "",
        val lessonId: String = "",
        val chapterIndex: Int = 0,
        val chapterCount: Int = 0,
        val error: String? = null,
    )

    /**
     * بارگذاری و پخش (یا آماده‌سازی) یک رسانه از URL.
     * اگر [autoplay] نباشد، فقط prepare می‌شود تا کاربر بتواند دکمه‌ی «ادامه» بزند.
     */
suspend fun load(
        uri: String,
        title: String,
        bookCode: String,
        lessonId: String,
        mediaType: LessonMediaProgressRepository.MediaType,
        autoplay: Boolean = true,
    ) {
        if (uri.isBlank()) {
            _state.update { it.copy(error = "آدرس رسانه خالی است.") }
            return
        }
        ensurePlayer()

        // قرارداد فصل‌ها: فایل همنام با پسوند 01/02/03 → یک پلی‌لیست برای کل درس.
        // پخش با استریم و بافر از سرور انجام می‌شود و سگمنت‌ها در SimpleCache
        // فقط روی گوشی کش می‌شوند (پخش مجدد = بدون دانلود مجدد).
        val chapters = expandChapters(uri)
        val uid = userId() ?: ""
        val progress = progressRepo.load(uid, bookCode, lessonId, mediaType)
        val chapterIndex = progress.chapterIndex.coerceIn(0, chapters.lastIndex)
        val startMs = (progress.lastPositionSec * 1000.0).toLong().coerceAtLeast(0L)
        val startSpeed = progress.playbackSpeed.toFloat().coerceIn(MIN_SPEED, MAX_SPEED)

        current = MediaContext(chapters.first(), title, bookCode, lessonId, mediaType, chapters)

        val p = player ?: return
        val items = chapters.mapIndexed { i, url ->
            val t = if (chapters.size > 1) "$title — فصل ${i + 1}" else title
            MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(t).setDisplayTitle(t).build())
                .build()
        }
        p.setMediaItems(items, chapterIndex, startMs)
        p.playbackParameters = PlaybackParameters(startSpeed)
        p.prepare()
        if (autoplay) p.playWhenReady = true
        publishFromPlayer(p, mediaType, bookCode, lessonId)
        startTicker()
    }

    fun play() { player?.play() }
    fun pause() {
        player?.let {
            it.playWhenReady = false
            it.pause()
            flushProgress(force = true)
        }
    }

    fun stop() {
        player?.let {
            flushProgress(force = true)
            it.stop()
            it.clearMediaItems()
        }
        current = null
        publish()
    }

    /** عقب/جلو ۱۰ ثانیه. */
    fun seekBy(deltaMs: Long) {
        val p = player ?: return
        val target = (p.currentPosition + deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    /** رفتن به ثانیه‌ی دقیق (مثلاً یک chapter marker). */
    fun seekTo(positionMs: Long) {
        val p = player ?: return
        val from = p.currentPosition
        p.seekTo(positionMs.coerceAtLeast(0L))
        // تشخیص «پرش بزرگ» طبق پرامپت (بیش از ۳ ثانیه)
        val fromSec = from / 1000.0
        val toSec = positionMs / 1000.0
        if (kotlin.math.abs(toSec - fromSec) >= LessonMediaProgressRepository.SEEK_JUMP_THRESHOLD_SEC) {
            val now = System.currentTimeMillis()
            // debounce: اگر در ۲۵۰ میلی‌ثانیه‌ی اخیر گزارش شده، فقط آخری ثبت شود
            if (now - lastSeekReportAt > 250) {
                val ctx = current
                if (ctx != null) {
                    val uid = userId() ?: ""
                    progressRepo.recordProgress(
                        userId = uid,
                        bookCode = ctx.bookCode,
                        lessonId = ctx.lessonId,
                        mediaType = ctx.mediaType,
                        positionSec = toSec,
                        durationSec = (p.duration.takeIf { it > 0 } ?: 0L) / 1000.0,
                        speed = p.playbackParameters.speed.toDouble(),
                        seekJump = LessonMediaProgressRepository.SeekEvent(
                            atClientTimeIso = Instant.now().toString(),
                            fromSec = fromSec,
                            toSec = toSec,
                            direction = if (toSec > fromSec) "forward" else "backward",
                        ),
                    )
                }
                lastSeekReportAt = now
            }
        }
    }

    fun setSpeed(speed: Float) {
        val p = player ?: return
        val s = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        p.playbackParameters = PlaybackParameters(s)
        // تغییر سرعت هم یک رویداد snapshot است
        val ctx = current ?: return
        val uid = userId() ?: ""
        progressRepo.recordProgress(
            userId = uid,
            bookCode = ctx.bookCode,
            lessonId = ctx.lessonId,
            mediaType = ctx.mediaType,
            positionSec = p.currentPosition / 1000.0,
            durationSec = (p.duration.takeIf { it > 0 } ?: 0L) / 1000.0,
            speed = s.toDouble(),
        )
        publish()
    }

    /** رفتن به ابتدای chapter بعدی/قبلی (اگر chapter markers تعریف شده باشند). */
    fun jumpChapter(markers: List<Double>, direction: Int) {
        if (markers.isEmpty()) return
        val p = player ?: return
        val posSec = p.currentPosition / 1000.0
        val target = if (direction > 0) {
            markers.firstOrNull { it > posSec + 0.5 } ?: markers.last()
        } else {
            markers.lastOrNull { it < posSec - 0.5 } ?: markers.first()
        }
        seekTo((target * 1000.0).toLong())
    }

    fun release() {
        tickJob?.cancel()
        tickJob = null
        scope.cancel()
        flushProgress(force = true)
        player?.release()
        player = null
        _state.value = PlayerState()
    }

    /**
     * دسترسیِ فقط‌خواندنی به ExoPlayer برای استفاده در [androidx.media3.ui.PlayerView].
     * در صورت null بودن، هنوز چیزی load نشده.
     */
    fun exoPlayer(): ExoPlayer? = player

    // ---------- داخلی ----------

    private fun ensurePlayer() {
        if (player != null) return
        val cacheFactory = CacheDataSource.Factory()
            .setCache(obtainCache(appContext))
            .setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true),
            )
        val p = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
        p.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                publishFromPlayerObject(player)
            }
            override fun onPlayerError(error: PlaybackException) {
                _state.update { it.copy(error = faErrorMessage(error)) }
            }
        })
        player = p
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(PROGRESS_TICK_MS)
                val p = player ?: break
                if (!p.isPlaying) continue
                val now = System.currentTimeMillis()
                if (now - lastReportAt < PROGRESS_TICK_MS - 250) continue
                flushProgress(force = false)
            }
        }
    }

    private fun flushProgress(force: Boolean) {
        val p = player ?: return
        val ctx = current ?: return
        val pos = p.currentPosition.coerceAtLeast(0L)
        val dur = p.duration.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
        val now = System.currentTimeMillis()

        val shouldReport = force || (pos - lastReportedPositionMs) >= 1000 || (now - lastReportAt) >= PROGRESS_TICK_MS
        if (!shouldReport) return
        lastReportedPositionMs = pos
        lastReportAt = now

        val posSec = pos / 1000.0
        val durSec = dur / 1000.0
        val completed = dur > 0 && posSec >= durSec * COMPLETION_FRACTION
        val uid = userId() ?: ""
        progressRepo.recordProgress(
            userId = uid,
            bookCode = ctx.bookCode,
            lessonId = ctx.lessonId,
            mediaType = ctx.mediaType,
            positionSec = posSec,
            durationSec = durSec,
            speed = p.playbackParameters.speed.toDouble(),
            appendView = completed,
            markCompleted = completed,
            chapterIndex = p.currentMediaItemIndex,
        )
    }

    private fun publish() {
        val p = player ?: return
        publishFromPlayerObject(p)
    }

    private fun publishFromPlayerObject(p: Player) {
        _state.update {
            it.copy(
                connected = true,
                playing = p.isPlaying,
                positionMs = p.currentPosition.coerceAtLeast(0L),
                durationMs = p.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET } ?: 0L,
                speed = p.playbackParameters.speed,
                bufferedFraction = p.bufferedPosition
                    .let { bp -> if (p.duration > 0) bp.toFloat() / p.duration else 0f }
                    .coerceIn(0f, 1f),
                chapterIndex = if (p.mediaItemCount > 0) p.currentMediaItemIndex else 0,
                chapterCount = current?.chapterUrls?.size ?: 0,
            )
        }
    }

    private fun publishFromPlayer(
        p: Player,
        mediaType: LessonMediaProgressRepository.MediaType,
        bookCode: String,
        lessonId: String,
    ) {
        publishFromPlayerObject(p)
        _state.update {
            it.copy(mediaType = mediaType, bookCode = bookCode, lessonId = lessonId)
        }
    }

    private fun faErrorMessage(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "فایل پیدا نشد؛ شاید پاک یا جابه‌جا شده."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ->
            "فرمت این فایل روی این دستگاه پخش نمی‌شود."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "اتصال شبکه قطع است؛ برای فایل‌های آنلاین اول دانلودش کن."
        PlaybackException.ERROR_CODE_TIMEOUT ->
            "پخش متوقف شد (پاسخی از منبع نرسید)."
        else -> "پخش با خطا متوقف شد."
    }

    companion object {
        /** ۱۰ ثانیه — عقب/جلو. */
        const val SEEK_INCREMENT_MS = 10_000L
        const val MIN_SPEED = 0.75f
        const val MAX_SPEED = 2.0f
        /** هر ۵ ثانیه یک snapshot پیشرفت. */
        const val PROGRESS_TICK_MS = 5_000L
        /** آستانه‌ی «تمام‌شده» — ۹۰٪ پیشرفت. */
        const val COMPLETION_FRACTION = 0.9
        /** سرعت‌های قابل‌انتخاب در UI. */
        val ALLOWED_SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

        /** کش رسانه فقط روی گوشی — سقف ۵۱۲MB با حذف خودکار قدیمی‌ها (LRU). */
        const val CACHE_MAX_BYTES = 512L * 1024 * 1024
        const val CHAPTER_COUNT = 3

        @Volatile private var sharedCache: SimpleCache? = null

        fun obtainCache(context: android.content.Context): SimpleCache = sharedCache ?: synchronized(this) {
            sharedCache ?: SimpleCache(
                java.io.File(context.cacheDir, "lesson_media_cache"),
                LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES),
                StandaloneDatabaseProvider(context),
            ).also { sharedCache = it }
        }

        /**
         * گسترش فصل‌ها: هر URI که پیش از پسوند با عدد تمام شود (مثلاً C905-L01-V01.mp4)
         * به فصل‌های 01..CHAPTER_COUNT همنام گسترش می‌یابد؛ اگر الگو نخورد، تک‌فایل است.
         */
        fun expandChapters(uri: String): List<String> {
            val m = Regex("^(.*?)(\\d+)(\\.[A-Za-z0-9]+)$").find(uri) ?: return listOf(uri)
            val width = m.groupValues[2].length
            return (1..CHAPTER_COUNT).map { "${m.groupValues[1]}${it.toString().padStart(width, '0')}${m.groupValues[3]}" }
        }
    }
}
