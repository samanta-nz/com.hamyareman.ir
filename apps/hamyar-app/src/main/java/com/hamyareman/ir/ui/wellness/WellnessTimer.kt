package com.hamyareman.ir.ui.wellness

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
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

/**
 * پرامپت ۰۲ — تایمر سلامتی هماهنگ با اعلان صوتی فارسی.
 *
 * مسئولیت:
 *  - شمارنده‌ی بصری (ثانیه‌ای) در صفحه.
 *  - پخش `audioCueId` در ثانیه‌های مشخص (`cues` در [WellnessTiming]).
 *  - بوق کوتاه (beep) در ثانیه‌های ۱، ۲، ۳ آخرِ هر مرحله.
 *  - لرزش ملایم در پایان هر مرحله (اختیاری).
 *  - ثبت نتیجه در [wellnessLogSink] برای سینک با Appwrite.
 *
 * نکته‌ی طراحی:
 *  - پلیر صوتی TTS فارسی فقط **یک‌بار** در طول یک جلسه ساخته می‌شود (lazy).
 *  - فایل‌های صوتی در Storage قرار می‌گیرند و در زمان اولین نیاز دانلود می‌شوند.
 *  - در محیط بدون Storage (تست/آفلاین) فقط شمارنده‌ی بصری و بوق کار می‌کند.
 */
class WellnessTimer(
    private val context: Context,
    private val timingProvider: TimingProvider,
    private val wellnessLogSink: WellnessLogSink? = null,
) {

    /** تأمین‌کننده‌ی مراحل و اعلان‌های صوتی برای یک حرکت. */
    interface TimingProvider {
        fun timingFor(move: WellnessMove): WellnessTiming
    }

    /** یک ثبت لاگ (write-only) برای سینک. */
    interface WellnessLogSink {
        suspend fun logSession(move: WellnessMove, secondsSpent: Int, completed: Boolean)
    }

    enum class Status { IDLE, RUNNING, PAUSED, FINISHED }

    data class TimerState(
        val status: Status = Status.IDLE,
        val currentStepIndex: Int = 0,
        val stepSecondsLeft: Int = 0,
        val stepSecondsTotal: Int = 0,
        val totalSecondsElapsed: Int = 0,
        val totalSecondsPlanned: Int = 0,
        val currentMoveTitle: String = "",
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var currentMove: WellnessMove? = null
    private var currentTiming: WellnessTiming? = null
    private var playedCueIndexes: MutableSet<Int> = mutableSetOf()
    private var pendingBeepAtSec: Int? = null

    /**
     * شروع یک جلسه با [move]. اگر تایمر قبلاً در حال اجراست، لغو می‌شود.
     */
    fun start(move: WellnessMove) {
        stop()
        val timing = timingProvider.timingFor(move)
        currentMove = move
        currentTiming = timing
        playedCueIndexes = mutableSetOf()
        val firstStep = timing.steps.firstOrNull() ?: return
        _state.value = TimerState(
            status = Status.RUNNING,
            currentStepIndex = 0,
            stepSecondsLeft = firstStep.seconds,
            stepSecondsTotal = firstStep.seconds,
            totalSecondsPlanned = timing.totalSeconds,
            currentMoveTitle = move.titleFa,
        )
        startTickLoop()
        // پخش اعلان معرفی در ثانیه‌ی صفر
        playIntroCue(timing)
    }

    fun pause() {
        if (_state.value.status != Status.RUNNING) return
        tickJob?.cancel()
        _state.update { it.copy(status = Status.PAUSED) }
    }

    fun resume() {
        if (_state.value.status != Status.PAUSED) return
        _state.update { it.copy(status = Status.RUNNING) }
        startTickLoop()
    }

    /** لغو و آزادسازی منابع. در پایان جلسه یا خروج از صفحه صدا زده می‌شود. */
    fun stop() {
        tickJob?.cancel()
        tickJob = null
        releaseMediaPlayer()
        val finalState = _state.value
        // اگر به پایان نرسیده ولی لغو شده، لاگ ناقص می‌فرستیم (تا داده‌ی از دست رفته نباشد)
        val move = currentMove
        if (move != null && finalState.status != Status.FINISHED && finalState.totalSecondsElapsed > 5) {
            scope.launch {
                wellnessLogSink?.logSession(
                    move = move,
                    secondsSpent = finalState.totalSecondsElapsed,
                    completed = false,
                )
            }
        }
        _state.value = TimerState()
        currentMove = null
        currentTiming = null
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (s.status != Status.RUNNING) break
                tick()
            }
        }
    }

    private fun tick() {
        val move = currentMove ?: return
        val timing = currentTiming ?: return
        val currentState = _state.value

        // ۱) آیا باید اعلان صوتی پخش شود؟
        val elapsedInStep = currentState.stepSecondsTotal - currentState.stepSecondsLeft
        timing.cues.forEachIndexed { i, cue ->
            if (i in playedCueIndexes) return@forEachIndexed
            if (elapsedInStep >= cue.atSec) {
                playedCueIndexes.add(i)
                playCue(cue)
            }
        }

        // ۲) بوق‌های آخر هر مرحله (۳-۲-۱)
        val newLeft = currentState.stepSecondsLeft - 1
        if (newLeft in 1..3 && newLeft != pendingBeepAtSec) {
            playBeep(newLeft)
            pendingBeepAtSec = newLeft
        } else if (newLeft > 3) {
            pendingBeepAtSec = null
        }

        // ۳) پایان مرحله؟
        if (newLeft <= 0) {
            val nextIndex = currentState.currentStepIndex + 1
            vibrate(150)
            if (nextIndex >= timing.steps.size) {
                finishSession(move, completed = true)
                return
            } else {
                val nextStep = timing.steps[nextIndex]
                playedCueIndexes = mutableSetOf()
                pendingBeepAtSec = null
                _state.update {
                    it.copy(
                        currentStepIndex = nextIndex,
                        stepSecondsLeft = nextStep.seconds,
                        stepSecondsTotal = nextStep.seconds,
                        totalSecondsElapsed = it.totalSecondsElapsed + 1,
                    )
                }
                return
            }
        }

        // ۴) فقط کم کردن ثانیه
        _state.update {
            it.copy(
                stepSecondsLeft = newLeft,
                totalSecondsElapsed = it.totalSecondsElapsed + 1,
            )
        }
    }

    private fun finishSession(move: WellnessMove, completed: Boolean) {
        tickJob?.cancel()
        vibrate(400)
        _state.update { it.copy(status = Status.FINISHED) }
        playFinishCue()
        scope.launch {
            wellnessLogSink?.logSession(
                move = move,
                secondsSpent = _state.value.totalSecondsElapsed,
                completed = completed,
            )
        }
    }

    private fun playIntroCue(timing: WellnessTiming) {
        val intro = timing.cues.firstOrNull { it.kind == AudioCue.Kind.INTRO } ?: return
        playCue(intro)
    }

    private fun playFinishCue() {
        // بوق بلند در پایان
        playBeep(long = true)
    }

    private fun playCue(cue: AudioCue) {
        // فایل صوتی cue از Storage دانلود می‌شود.
        // استراتژی:
        //  - cue.kind == INTRO → فایل شروع (startCueId یا اولین فایل)
        //  - cue.kind == FINISH → فایل پایان (endCueId یا آخرین فایل)
        //  - سایر cue ها (GUIDE) → فایل میانه (midCueId یا فایل وسط)
        //  - اگر فقط یک فایل باشد، همان برای همه پخش می‌شود.
        val filename = pickCueFilename(cue) ?: return
        scope.launch {
            val local = withContext(Dispatchers.IO) {
                AudioCueCache.getLocalFile(context, filename)
            }
            if (local != null && local.exists()) {
                playLocalFile(local.absolutePath)
            }
            // اگر فایل در کش نبود، skip می‌کنیم (timer فقط بصری ادامه می‌دهد).
        }
    }

    /**
     * انتخاب فایل صوتی مناسب بر اساس نوع cue و مرحله‌ی جلسه.
     *
     *  - شروع (INTRO): فایل اول audioCueIds
     *  - پایان (FINISH): فایل آخر audioCueIds
     *  - میانه (GUIDE/...): فایل میانی audioCueIds
     *  - اگر فقط ۱ فایل باشد: همان
     */
    private fun pickCueFilename(cue: AudioCue): String? {
        val move = currentMove ?: return null
        val ids = move.audioCueIds
        if (ids.isEmpty()) return null
        return when {
            ids.size == 1 -> ids[0]
            cue.kind == AudioCue.Kind.INTRO -> ids.first()
            cue.kind == AudioCue.Kind.FINISH -> ids.last()
            else -> ids.getOrNull(1) ?: ids.first()
        }
    }

    private fun playBeep(secondsLeft: Int = 0, long: Boolean = false) {
        // بوق با ToneGenerator یا فایل mp3 کوتاه.
        // در نسخه‌ی ساده، فقط لرزش می‌فرستیم (تضمینی cross-device).
        if (long) vibrate(300) else vibrate(80)
    }

    private fun playLocalFile(path: String) {
        runCatching {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(path)
                setOnCompletionListener { releaseMediaPlayer() }
                prepare()
                start()
            }
        }
    }

    private fun releaseMediaPlayer() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
    }

    private fun vibrate(durationMs: Long) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ContextCompat.getSystemService(context, VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(context, Vibrator::class.java)
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }

    companion object {
        const val TICK_INTERVAL_MS = 1000L
    }
}

/**
 * مراحل و اعلان‌های صوتی یک حرکت.
 */
data class WellnessTiming(
    val move: WellnessMove,
    val steps: List<WellnessStep>,
    val cues: List<AudioCue>,
) {
    val totalSeconds: Int = steps.sumOf { it.seconds }
}
