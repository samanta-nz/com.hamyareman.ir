package com.hamyareman.ir.platform.feature.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * وضعیت «صفحه‌ی تدریس باز است» — پل بین سرویس رسانه و UI (همان پروسه).
 * قانون v1.10: صوت تدریس فقط داخل صفحه‌ی تدریس پخش می‌شود؛ تا این صفحه باز
 * نشده، دکمه‌ی پلی اعلان به‌جای پخش، همان صفحه را باز می‌کند.
 */
object TeachGate {
    /**
     * شمارندهٔ بازبودنِ صفحه‌های پخش: چند صفحه می‌توانند هم‌زمان ادعای «باز» بودن
     * کنند (مثلاً هنگام جابه‌جایی بین صفحه‌ها) و فقط با بسته‌شدنِ آخری،
     * پخشِ پس‌زمینه ممنوع می‌شود. (قبلاً با یک boolean، بسته‌شدنِ صفحهٔ قبلی
     * پخشِ صفحهٔ جدید را بلافاصله متوقف می‌کرد.)
     */
    private var openCount = 0

    @Volatile var teachPageOpen: Boolean = false
        private set

    @Synchronized
    fun enter() {
        openCount++
        teachPageOpen = true
    }

    @Synchronized
    fun exit() {
        openCount = (openCount - 1).coerceAtLeast(0)
        teachPageOpen = openCount > 0
    }

    /**
     * «تپش» — اطمینان از بازبودنِ دروازه هنگامی که می‌دانیم صفحه‌ی پخش باز است.
     *
     * چرا لازم است: شمارنده ممکن است بر اثرِ چرخه‌های نامتقارنِ چرخه‌عمر
     * (بسته‌شدنِ یک صفحه بیش از بازشدن) به صفر برسد؛ آن‌وقت سرویس هر پخشی را
     * بلافاصله و **بی‌هیچ پیامی** متوقف می‌کند. وقتی کاربر روی «پخش» می‌زند،
     * صفحه قطعاً باز است؛ پس دروازه را بی‌قیدوشرط باز می‌کنیم.
     */
    @Synchronized
    fun pulse() {
        if (openCount < 1) openCount = 1
        teachPageOpen = true
    }

    @Synchronized
    fun closeAll() {
        openCount = 0
        teachPageOpen = false
    }
    /** درسی که باید در باز شدن بعدی اپ، صفحه‌ی تدریسش باز شود. */
    @Volatile var requestedPack: String? = null

    /** v1.25 — درسی که همین الان در سرویس پخش است؛ مرجع واحد «همان درس» برای لمس اعلان. */
    @Volatile var currentPack: String? = null
}

/**
 * سرویس پخش کتاب صوتی (Media3).
 *
 * اعلان: پلی/مکث + سیک + نام‌ها + دکمهٔ خروج (توقف و بستن اعلان).
 * فرمان‌های session را برای کنترلر اپ فیلتر نکن — setMediaItems نباید بشکند.
 *
 * `@UnstableApi` قرارداد نسخه‌گذاری Media3 است.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val sleepHandler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable {
        mediaSession?.player?.pause()
        clearSleepTimer()
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val exo = ExoPlayer.Builder(this)
            // پخشِ فایل‌های دانلودشده‌ی گاوصندوق (vault://) دیگر از سرور HTTP محلی نمی‌گذرد.
            .setMediaSourceFactory(vaultAwareMediaSourceFactory(this))
            // تمرکزِ صوتی را خودمان مدیریت نمی‌کنیم: اگر برنامه یا سیستمی تمرکز را در دست
            // داشته باشد، ExoPlayer پخش را **بی‌هیچ خطایی** متوقف می‌کند و کاربر فقط سکوت
            // می‌بیند. برای پخشِ درس، «حتماً پخش شود» مهم‌تر از تعارف با دیگر پلیرهاست؛
            // قطعِ صدا هنگام جدا شدنِ هندزفری (setHandleAudioBecomingNoisy) همچنان هست.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
        // صف‌های آلبوم شخصی و کتاب صوتی به قبلی/بعدی واقعی نیاز دارند؛ فرمان‌های
        // استاندارد Media3 را پنهان نمی‌کنیم تا UI و اعلان سیستم هر دو یک موتور باشند.
        val player: Player = exo

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(teachPendingIntent(currentPackOf(player)))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    val base = super.onConnect(session, controller)
                    if (!base.isAccepted) return base
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(
                            base.availableSessionCommands.buildUpon()
                                .add(STOP_COMMAND)
                                .add(SET_SLEEP_TIMER_COMMAND)
                                .add(CLEAR_SLEEP_TIMER_COMMAND)
                                .build(),
                        )
                        .setAvailablePlayerCommands(base.availablePlayerCommands)
                        .setCustomLayout(listOf(stopButton()))
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle,
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        ACTION_STOP_TEACH -> {
                            halt(session.player)
                            clearSleepTimer()
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        ACTION_SET_SLEEP_TIMER -> {
                            setSleepTimer(args.getLong(EXTRA_SLEEP_UNTIL, 0L))
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        ACTION_CLEAR_SLEEP_TIMER -> {
                            clearSleepTimer()
                            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }

                override fun onPlayerCommandRequest(
                    mediaSession: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    playerCommand: Int,
                ): Int {
                    val pack = currentPackOf(mediaSession.player) ?: TeachGate.currentPack
                    val fromApp = controllerInfo.packageName == packageName
                    if (playerCommand == Player.COMMAND_PLAY_PAUSE && pack != null && !TeachGate.teachPageOpen && !fromApp) {
                        TeachGate.requestedPack = pack
                        mediaSession.setSessionActivity(teachPendingIntent(pack))
                        runCatching { teachPendingIntent(pack).send() }
                        return SessionResult.RESULT_ERROR_UNKNOWN
                    }
                    return super.onPlayerCommandRequest(mediaSession, controllerInfo, playerCommand)
                }
            })
            .build()
        mediaSession?.setCustomLayout(listOf(stopButton()))
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val pack = currentPackOf(player)
                TeachGate.currentPack = pack
                mediaSession?.setSessionActivity(teachPendingIntent(pack))
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val pack = currentPackOf(player)
                    TeachGate.currentPack = pack
                    mediaSession?.setSessionActivity(teachPendingIntent(pack))
                }
                enforceForegroundOnly(player)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                enforceForegroundOnly(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                enforceForegroundOnly(player)
            }

            override fun onPositionDiscontinuity(
                oldPosition: androidx.media3.common.Player.PositionInfo,
                newPosition: androidx.media3.common.Player.PositionInfo,
                reason: Int,
            ) {
                val pack = currentPackOf(player)
                if (pack != TeachGate.currentPack) {
                    TeachGate.currentPack = pack
                    mediaSession?.setSessionActivity(teachPendingIntent(pack))
                }
            }
        })

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_stat_audiobook)
            },
        )

        restoreSleepTimer()
    }

    private fun currentPackOf(player: Player?): String? {
        val id = player?.currentMediaItem?.mediaId ?: return null
        var pid = id.removeSuffix("_AUDIO.mp3").removeSuffix("_INTRO.mp3")
        if (pid.endsWith("-1") || pid.endsWith("-2")) pid = pid.dropLast(2)
        return pid.takeIf { it.isNotBlank() }
    }

    private fun teachPendingIntent(packId: String?): PendingIntent =
        PendingIntent.getActivity(
            this,
            (packId ?: "none").hashCode(),
            Intent().apply {
                setClassName(packageName, TEACH_ACTIVITY)
                action = TEACH_OPEN_ACTION
                putExtra(TEACH_OPEN_EXTRA, packId)
                putExtra(TEACH_OPEN_AUTOPLAY, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** اگر صفحه‌ی پخش باز نیست، هرگز صدا ادامه پیدا نکند. */
    private fun enforceForegroundOnly(player: Player) {
        // صوت تدریس فقط با صفحهٔ تدریس باز مجاز است؛ کتاب آزاد می‌تواند با
        // مجوز صریح کاربر در پس‌زمینه ادامه پیدا کند.
        if ((player.playWhenReady || player.isPlaying) &&
            !TeachGate.teachPageOpen &&
            !BackgroundPlaybackGate.enabled
        ) {
            player.pause()
        }
    }

    private fun halt(player: Player) {
        runCatching { player.pause() }
        runCatching { player.stop() }
        runCatching { player.clearMediaItems() }
        TeachGate.currentPack = null
    }

    private fun timerStore() = getSharedPreferences(SLEEP_PREFS, MODE_PRIVATE)

    private fun setSleepTimer(untilEpochMs: Long) {
        if (untilEpochMs <= System.currentTimeMillis()) {
            clearSleepTimer()
            mediaSession?.player?.pause()
            return
        }
        timerStore().edit().putLong(SLEEP_UNTIL_KEY, untilEpochMs).apply()
        sleepHandler.removeCallbacks(sleepRunnable)
        sleepHandler.postDelayed(sleepRunnable, (untilEpochMs - System.currentTimeMillis()).coerceAtLeast(1L))
    }

    private fun clearSleepTimer() {
        sleepHandler.removeCallbacks(sleepRunnable)
        timerStore().edit().putLong(SLEEP_UNTIL_KEY, 0L).apply()
    }

    private fun restoreSleepTimer() {
        val until = timerStore().getLong(SLEEP_UNTIL_KEY, 0L)
        if (until > System.currentTimeMillis()) {
            sleepHandler.postDelayed(sleepRunnable, until - System.currentTimeMillis())
        } else if (until != 0L) {
            clearSleepTimer()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!BackgroundPlaybackGate.enabled) {
            mediaSession?.player?.let { halt(it) }
            stopSelf()
        }
        // در حالت پخش پس‌زمینهٔ کتاب آزاد، سرویس و اعلان زنده می‌مانند.
    }

    override fun onDestroy() {
        sleepHandler.removeCallbacks(sleepRunnable)
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val SEEK_INCREMENT_MS = 30_000L
        const val TEACH_OPEN_ACTION = "com.hamyareman.ir.OPEN_TEACH"
        const val TEACH_OPEN_EXTRA = "open_pack"
        const val TEACH_OPEN_AUTOPLAY = "open_pack_autoplay"
        const val TEACH_ACTIVITY = "com.hamyareman.ir.MainActivity"
        const val ACTION_STOP_TEACH = "com.hamyareman.ir.STOP_TEACH"
        const val ACTION_SET_SLEEP_TIMER = "com.hamyareman.ir.SET_SLEEP_TIMER"
        const val ACTION_CLEAR_SLEEP_TIMER = "com.hamyareman.ir.CLEAR_SLEEP_TIMER"
        const val EXTRA_SLEEP_UNTIL = "sleep_until_epoch_ms"
        private const val SLEEP_PREFS = "hamyar_playback_prefs"
        private const val SLEEP_UNTIL_KEY = "sleep_timer_until"
        val STOP_COMMAND = SessionCommand(ACTION_STOP_TEACH, Bundle.EMPTY)
        val SET_SLEEP_TIMER_COMMAND = SessionCommand(ACTION_SET_SLEEP_TIMER, Bundle.EMPTY)
        val CLEAR_SLEEP_TIMER_COMMAND = SessionCommand(ACTION_CLEAR_SLEEP_TIMER, Bundle.EMPTY)

        fun stopButton(): CommandButton =
            CommandButton.Builder(CommandButton.ICON_STOP)
                .setDisplayName("خروج")
                .setSessionCommand(STOP_COMMAND)
                .setEnabled(true)
                .build()
    }
}
