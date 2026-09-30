package com.hamyareman.ir.platform.feature.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine

/** وضعیت پخش، برای مصرف مستقیم در Compose. */
data class PlaybackState(
    val connected: Boolean = false,
    val hasMedia: Boolean = false,
    val playing: Boolean = false,
    val ended: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f,
    val error: String? = null,
    /** پخش به‌خاطرِ تمرکزِ صوتی (یا اپِ دیگر) سرکوب شده — بدون اینکه خطایی بیاید. */
    val suppressed: Boolean = false,
    /** پلیر «می‌خواهد» پخش کند ( playWhenReady ) — برای تشخیصِ توقفِ بی‌صدا. */
    val playWhenReady: Boolean = false,
    /** در حال بارگذاری/بافر کردن است (فایل هنوز آماده نیست). */
    val buffering: Boolean = false,
    val currentIndex: Int = 0,
    val mediaCount: Int = 0,
    val title: String = "",
    val shuffleEnabled: Boolean = false,
)

/**
 * کلاینتِ [PlaybackService] — چیزی که صفحه‌ی UI با آن حرف می‌زند.
 *
 * نکته‌ی مهم درباره‌ی چرخه‌ی عمر: `release()` فقط **اتصال این صفحه** را قطع می‌کند،
 * نه پخش را. برای قطع صدا قبل از release باید [stop] صدا زده شود.
 */
class PlaybackController(
    context: Context,
    private val serviceClass: Class<out androidx.media3.session.MediaSessionService> = PlaybackService::class.java,
) {

    private val appContext = context.applicationContext

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(playing = false, error = faMessage(error)) }
        }
    }

    val isConnected: Boolean get() = controller?.isConnected == true

    /** برای اتصال PlayerView ویدیو به همان سرویس واحد پخش. */
    fun asPlayer(): Player? = controller

    val positionMs: Long get() = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L

    val durationMs: Long get() = controller?.duration?.takeIf { it > 0 } ?: 0L

    /** اتصال به سرویس. اگر سرویس بالا نباشد، اندروید خودش آن را start می‌کند. */
    suspend fun connect(): Boolean {
        if (isConnected) return true
        val token = SessionToken(appContext, ComponentName(appContext, serviceClass))
        val future = MediaController.Builder(appContext, token).buildAsync()
        return suspendCancellableCoroutine { continuation ->
            future.addListener(
                {
                    val created = runCatching { future.get() }.getOrNull()
                    controller = created
                    if (created != null) {
                        created.addListener(listener)
                        publish(created)
                    } else {
                        _state.update { it.copy(error = "اتصال به سرویس پخش ممکن نشد.") }
                    }
                    if (continuation.isActive) {
                        continuation.resume(created != null) { _, _, _ -> }
                    }
                },
                ContextCompat.getMainExecutor(appContext),
            )
            continuation.invokeOnCancellation { runCatching { MediaController.releaseFuture(future) } }
        }
    }

    /** فایل را در صف پخش می‌گذارد و از [startPositionMs] آماده می‌کند (بدون پخش خودکار). */
    fun setMedia(uri: String, title: String, startPositionMs: Long = 0L) {
        val current = controller ?: return
        if (uri.isBlank()) return
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .build(),
            )
            .build()
        current.setMediaItem(item, startPositionMs.coerceAtLeast(0L))
        current.prepare()
        publish(current)
    }

    /**
     * صف چندترکه (مثلاً «صوت درس» + «مقدمه» یک درس) — اعلان سیستمی خودش
     * دکمه‌های قبلی/بعدی را نشان می‌دهد و جابه‌جایی ترک را انجام می‌دهد.
     */
    fun setMediaItems(items: List<MediaItem>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val current = controller ?: return
        if (items.isEmpty()) return
        current.setMediaItems(items, startIndex.coerceIn(0, items.size - 1), startPositionMs.coerceAtLeast(0L))
        current.prepare()
        publish(current)
    }

    /** شناسه‌ی ترک جاری — برای همگام‌سازی UI با دکمه‌های قبلی/بعدیِ اعلان. */
    fun currentMediaId(): String? = controller?.currentMediaItem?.mediaId

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    /** توقف کامل + خالی‌کردن صف تا اعلان هم بسته شود. */
    fun stop() {
        controller?.let { c ->
            runCatching { c.pause() }
            runCatching { c.stop() }
            runCatching { c.clearMediaItems() }
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        c.seekTo((c.currentPosition + deltaMs).coerceAtLeast(0L))
    }

    fun seekToPrevious() {
        controller?.let { if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() else it.seekTo(0L) }
    }

    fun seekToNext() {
        controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    }

    fun setShuffle(enabled: Boolean) {
        controller?.let { it.shuffleModeEnabled = enabled; publish(it) }
    }

    /** سرعت پخش — برای کتاب صوتی واقعاً استفاده می‌شود (۰٫۷۵ تا ۱٫۵). */
    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed.coerceIn(MIN_SPEED, MAX_SPEED))
    }

    /** قطع اتصال UI (پخش متوقف نمی‌شود). */
    fun release() {
        controller?.let { current ->
            runCatching { current.removeListener(listener) }
            runCatching { current.release() }
        }
        controller = null
        _state.value = PlaybackState()
    }

    private fun publish(player: Player) {
        _state.update {
            it.copy(
                connected = true,
                hasMedia = player.mediaItemCount > 0,
                playing = player.isPlaying,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { d -> d > 0 && d != C.TIME_UNSET } ?: 0L,
                speed = player.playbackParameters.speed,
                suppressed = player.playWhenReady &&
                    player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE,
                playWhenReady = player.playWhenReady,
                buffering = player.playbackState == Player.STATE_BUFFERING,
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                mediaCount = player.mediaItemCount,
                title = player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                shuffleEnabled = player.shuffleModeEnabled,
            )
        }
    }

    private fun faMessage(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "فایل پیدا نشد؛ شاید پاک یا جابه‌جا شده. دوباره انتخابش کن."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        -> "فرمت این فایل روی این دستگاه پخش نمی‌شود."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> "اتصال شبکه قطع است؛ اگر فایل روی گوشی نیست، اول دانلودش کن."
        PlaybackException.ERROR_CODE_TIMEOUT -> "پخش متوقف شد (پاسخی از منبع نرسید)."
        else -> "پخش با خطا متوقف شد."
    }

    companion object {
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2.0f
    }
}
