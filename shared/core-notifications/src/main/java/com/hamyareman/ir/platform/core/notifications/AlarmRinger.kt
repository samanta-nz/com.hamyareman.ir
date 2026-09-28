package com.hamyareman.ir.platform.core.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.hamyareman.ir.platform.core.common.LocalStore

/**
 * زنگِ واقعیِ آلارم‌های مدرسه.
 *
 * اعلان به‌تنهایی فقط صدای پیش‌فرضِ سیستم را پخش می‌کند؛ این کلاس آهنگِ انتخابیِ
 * کاربر را (از فهرست آهنگ‌های گوشی) با بلندی و تکرارِ تنظیم‌شده پخش می‌کند تا
 * آلارم واقعاً زنگ بزند.
 *
 * داده‌ها در همان استورِ برنامه‌ی کلاسی خوانده می‌شوند (`hamyar_class_plan`) تا
 * گیرنده‌ی اعلان بدون وابستگی به ماژول اپ به آن‌ها دسترسی داشته باشد.
 */
object AlarmRinger {

    private const val STORE = "hamyar_class_plan"
    private const val KEY_SOUND = "alarm_sound"
    private const val KEY_VOLUME = "alarm_volume"
    private const val KEY_REPEAT = "alarm_repeat"
    private const val KEY_CRESCENDO = "alarm_crescendo"

    /** بیشینه‌ی زمان زنگ (تا اگر کاربر برنداشت، تا ابد پخش نشود). */
    private const val MAX_RING_MS = 3 * 60_000L

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    /** آهنگِ انتخابی کاربر (URI رشته‌ای)؛ خالی یعنی پیش‌فرض سیستم. */
    fun savedSound(ctx: Context): String =
        LocalStore(ctx, STORE).getString(KEY_SOUND).orEmpty()

    fun saveSound(ctx: Context, uri: String) {
        LocalStore(ctx, STORE).putString(KEY_SOUND, uri)
    }

    /**
     * فهرست آهنگ‌های در دسترس گوشی (زنگ، اعلان، آلارم) برای انتخاب کاربر.
     * اگر دسترسی ممکن نبود، لیست خالی برمی‌گردد و UI همان پیش‌فرض را نشان می‌دهد.
     */
    fun deviceSounds(ctx: Context): List<Pair<String, String>> {
        val out = LinkedHashMap<String, String>()
        val manager = RingtoneManager(ctx).apply {
            setType(RingtoneManager.TYPE_ALL)
        }
        runCatching {
            val cursor = manager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
                if (!out.containsKey(uri)) out[uri] = title
            }
        }
        if (out.isEmpty()) {
            listOf(
                RingtoneManager.TYPE_ALARM,
                RingtoneManager.TYPE_RINGTONE,
                RingtoneManager.TYPE_NOTIFICATION,
            ).forEach { type ->
                runCatching {
                    val m = RingtoneManager(ctx).apply { setType(type) }
                    val c = m.cursor
                    while (c.moveToNext()) {
                        val title = c.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: continue
                        val uri = m.getRingtoneUri(c.position)?.toString() ?: continue
                        out.putIfAbsent(uri, title)
                    }
                }
            }
        }
        return out.map { it.value to it.key }
    }

    fun titleOf(ctx: Context, uriString: String): String {
        if (uriString.isBlank()) return "پیش‌فرض گوشی"
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return "پیش‌فرض گوشی"
        return runCatching { RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx) }.getOrNull()
            ?: uri.lastPathSegment
            ?: "آهنگ انتخابی"
    }

    /** پخشِ آزمایشیِ کوتاه (برای انتخاب آهنگ در تنظیمات). */
    fun preview(ctx: Context, uriString: String, volumePct: Int = 80) {
        stop()
        val uri = uriString.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val mp = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(ctx, uri)
                isLooping = false
                setVolume(volumePct / 100f, volumePct / 100f)
                setOnCompletionListener { releasePlayer() }
                prepare()
                start()
            }
        }.getOrNull() ?: return
        player = mp
        scheduleStop(8_000L)
    }

    /** شروع زنگ آلارم با تنظیمات ذخیره‌شده. */
    fun start(ctx: Context) {
        stop()
        val store = LocalStore(ctx, STORE)
        val sound = store.getString(KEY_SOUND).orEmpty()
        val volume = store.getInt(KEY_VOLUME, 80).coerceIn(10, 100) / 100f
        val repeat = store.getInt(KEY_REPEAT, 2).coerceIn(1, 5)
        val crescendo = store.getBool(KEY_CRESCENDO, false)

        val uri = sound.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val uriFinal = runCatching {
            if (uri.toString().isBlank()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) else uri
        }.getOrDefault(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))

        val mp = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(ctx, uriFinal)
                isLooping = repeat > 1
                setVolume(if (crescendo) 0.08f else volume, if (crescendo) 0.08f else volume)
                setOnCompletionListener { releasePlayer() }
                setOnErrorListener { _, _, _ -> releasePlayer(); true }
                prepare()
                start()
            }
        }.getOrNull()
        if (mp == null) {
            // آخرین راه: زنگِ پیش‌فرض سیستم با RingtoneManager.
            runCatching {
                RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))?.play()
            }
            return
        }
        player = mp
        if (crescendo) rampUp(mp, volume)
        scheduleStop(MAX_RING_MS)
    }

    /** بلندی را در ۱۲ گام تا حداکثرِ تنظیم‌شده بالا می‌برد. */
    private fun rampUp(mp: MediaPlayer, target: Float) {
        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                if (player !== mp) return
                step++
                val v = (0.08f + (target - 0.08f) * (step / 12f)).coerceIn(0f, 1f)
                runCatching { mp.setVolume(v, v) }
                if (step < 12) handler.postDelayed(this, 900L)
            }
        }
        handler.postDelayed(runnable, 900L)
    }

    private fun scheduleStop(delayMs: Long) {
        stopRunnable?.let { handler.removeCallbacks(it) }
        val r = Runnable { stop() }
        stopRunnable = r
        handler.postDelayed(r, delayMs)
    }

    fun stop() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        player = null
    }
}
