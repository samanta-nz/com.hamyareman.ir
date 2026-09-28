package com.hamyareman.ir.platform.feature.calls

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hamyareman.ir.platform.core.appwrite.AppwriteRealtimeFeed
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.common.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID

/** یک تماس ورودی که زنگ می‌خورد. */
data class IncomingCall(
    val callId: String,
    val fromUserId: String,
    val fromLabel: String,
    val kind: CallKind,
    val startedAtMs: Long,
) {
    fun ageMs(now: Long = System.currentTimeMillis()): Long = now - startedAtMs
}

/**
 * نگهبانِ زنگِ تماس ورودی.
 *
 * تا پیش از این، `CallEngine.acceptIncoming` وجود داشت ولی **هیچ‌وقت صدا زده نمی‌شد**:
 * یعنی زهرا می‌توانست زنگ بزند، اما گوشی پدر هیچ‌وقت زنگ نمی‌خورد. این کلاس همان
 * حلقه‌ی گم‌شده است.
 *
 * دو لایه، دقیقاً مثل سیگنالینگ:
 *  1) **Realtime** روی کانال `tablesdb.<db>.tables.call_sessions.rows` — زنگ تقریباً
 *     بی‌درنگ می‌خورد؛
 *  2) **polling** به‌عنوان تور ایمنی (وقتی socket قطع است یا Realtime تنظیم نشده).
 *
 * سه نکته‌ی صداقت:
 *  - اگر بک‌اند تنظیم نباشد، `start()` بی‌صدا برمی‌گردد و هیچ زنگی نمایش داده نمی‌شود
 *    (وانمود نمی‌کنیم تماس آنلاین داریم).
 *  - زنگ فقط تا [RING_TTL_MS] اعتبار دارد؛ سطر کهنه‌ی `call_sessions` که کسی جوابش را
 *    نداده، تا ابد گوشی را زنگ نمی‌زند.
 *  - هر `callId` یک‌بار مصرف است (`seen`)، پس یک تماس دو بار زنگ نمی‌زند.
 *
 * این کلاس **فقط وقتی اپ باز است** کار می‌کند. برای زنگ در حالت بسته‌بودن اپ به یک
 * سرویس پیش‌زمینه‌ی دائمی + FCM نیاز است که عمداً اضافه نشد (باتری و حریم خصوصی).
 */
class IncomingCallWatcher(
    private val context: Context,
    private val signaling: CallSignaling?,
    private val selfUserId: suspend () -> String?,
    private val realtime: AppwriteRealtimeFeed? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {

    private val _incoming = MutableStateFlow<IncomingCall?>(null)

    /** تماس ورودیِ در حال زنگ؛ `null` یعنی چیزی زنگ نمی‌خورد. */
    val incoming: StateFlow<IncomingCall?> = _incoming.asStateFlow()

    private val seen = Collections.synchronizedSet(mutableSetOf<String>())
    private var watchJob: Job? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    /** شروع دیدبانی. اگر بک‌اند نباشد، بی‌صدا برمی‌گردد. */
    fun start() {
        if (watchJob?.isActive == true) return
        val engine = signaling ?: return
        if (!engine.isConfigured) return
        watchJob = scope.launch {
            val me = selfUserId()
            if (me.isNullOrBlank()) return@launch

            // لایه‌ی ۱: Realtime
            launch {
                realtime?.callSessions()?.collect { event ->
                    val row = event.row
                    if (row.string("toUserId") == me && row.string("status") == "ringing") {
                        row.toIncoming()?.let { offer(it) }
                    }
                }
            }

            // لایه‌ی ۲: polling — با Realtime فعال کندتر است تا سرور بیهوده خوانده نشود.
            val interval = if (engine.hasRealtime) REALTIME_BACKUP_POLL_MS else POLL_INTERVAL_MS
            while (isActive) {
                refresh(me)
                expireStale()
                delay(interval)
            }
        }
    }

    /** یک بار خواندن تماس‌های زنگ‌خورده (هم از دیدبان و هم از بیرون قابل صداست). */
    suspend fun refresh(me: String? = null) {
        val engine = signaling ?: return
        val userId = me ?: selfUserId() ?: return
        if (userId.isBlank() || !engine.isConfigured) return
        val result = withContext(Dispatchers.IO) { engine.incomingRinging(userId) }
        if (result !is AppResult.Ok) return
        result.value
            .mapNotNull { it.toIncoming() }
            .filter { it.ageMs() in 0..RING_TTL_MS }
            // خودش زنگ نزند: سطرِ تماسی که خودم شروع کرده‌ام toUserId=من ندارد،
            // ولی اگر داشت (دو دستگاه یک کاربر) نادیده گرفته می‌شود.
            .filter { it.fromUserId != userId }
            .maxByOrNull { it.startedAtMs }
            ?.let { offer(it) }
    }

    /** ردکردن تماس: سیگنال `REJECT` برای طرف مقابل + بستن سطر جلسه. */
    suspend fun decline(call: IncomingCall) {
        seen.add(call.callId)
        val me = selfUserId().orEmpty()
        val engine = signaling
        if (engine != null && engine.isConfigured && me.isNotBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    engine.send(
                        CallSignal(
                            id = UUID.randomUUID().toString(),
                            callId = call.callId,
                            fromUserId = me,
                            toUserId = call.fromUserId,
                            type = SignalType.REJECT,
                            payload = "{}",
                            createdAtMs = System.currentTimeMillis(),
                        ),
                    )
                }
                runCatching { engine.closeSession(call.callId, "rejected") }
            }
        }
        clear()
    }

    /** پاک‌کردن زنگ (بعد از پاسخ یا وقتی کاربر صفحه را بست). */
    fun clear() {
        _incoming.value = null
        stopRinging()
    }

    /** پایان دیدبانی (مثلاً وقتی اپ بسته می‌شود). */
    fun stop() {
        watchJob?.cancel()
        watchJob = null
        clear()
    }

    // --- درونی ---------------------------------------------------------------

    private fun offer(call: IncomingCall) {
        if (!seen.add(call.callId)) return
        // اگر زنگ دیگری روی صفحه است، تازه‌تر جایگزین می‌شود.
        _incoming.value = call
        startRinging()
    }

    private fun expireStale() {
        val current = _incoming.value ?: return
        if (current.ageMs() > RING_TTL_MS) clear()
    }

    private fun startRinging() {
        runCatching {
            if (ringtone == null) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ringtone = uri?.let { RingtoneManager.getRingtone(context, it) }
            }
            ringtone?.let { tone ->
                runCatching {
                    tone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                if (!tone.isPlaying) tone.play()
            }
        }
        runCatching {
            val vibe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator = vibe
            vibe?.takeIf { it.hasVibrator() }?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 700, 700), 0),
            )
        }
    }

    private fun stopRinging() {
        runCatching { ringtone?.takeIf { it.isPlaying }?.stop() }
        runCatching { vibrator?.cancel() }
    }

    private fun TableRow.toIncoming(): IncomingCall? {
        val callId = string("callId").ifBlank { id }
        val from = string("fromUserId")
        if (callId.isBlank() || from.isBlank()) return null
        return IncomingCall(
            callId = callId,
            fromUserId = from,
            fromLabel = string("fromLabel").ifBlank { "تماس" },
            kind = runCatching { CallKind.valueOf(string("kind", CallKind.AUDIO.name)) }
                .getOrDefault(CallKind.AUDIO),
            startedAtMs = long("startedAt").takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
    }

    companion object {
        /** بدون Realtime: هر ۶ ثانیه یک‌بار می‌پرسیم. */
        private const val POLL_INTERVAL_MS = 6_000L

        /** با Realtime: polling فقط تور ایمنی است. */
        private const val REALTIME_BACKUP_POLL_MS = 15_000L

        /** زنگِ بیشتر از یک دقیقه، کهنه است و نادیده گرفته می‌شود. */
        private const val RING_TTL_MS = 60_000L
    }
}
