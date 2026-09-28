package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore
import org.json.JSONObject

/**
 * موتور آمارِ «تدریس» — ثبتِ خودکارِ رویدادها + محاسبه‌ی فقط‌خواندنی.
 *
 * اصول طراحی (مصوب):
 *  - فقط رویداد را می‌توان ثبت کرد (ثانیه‌ی پخش، پرش، نشست، اتمام دوره)؛
 *    هیچ API ای برای ویرایش/حذف عدد قبلی وجود ندارد → آمار «غیرقابل ویرایش».
 *  - هر پک یک سطر JSON در LocalStore("hamyar_teach_stats") دارد.
 *  - سنجه‌ها: نشست‌ها، ثانیه‌ی شنیدن صوت، ثانیه‌ی تماشای ویدیو، پرش‌های >۳s،
 *    اتمام «اولین دوره‌ی تدریس» (صوت تا انتها یا ویدیو ≥۹۵٪) در چند نشست.
 */
object TeachStats {

    data class Snap(
        val sessions: Int = 0,
        val listenSec: Int = 0,
        val videoSec: Int = 0,
        val jumps: Int = 0,
        val done: Boolean = false,
        val doneMedia: Int = 0,
        val startedAtMs: Long = 0L,
        val lastSessionAtMs: Long = 0L,
        val completedAtMs: Long = 0L,
        val audioDurSec: Int = 0,
        val videoDurSec: Int = 0,
        /** دوره‌ی اولِ تدریس در چند نشست تمام شد (۰ = هنوز تمام نشده). */
        val sessionsToDone: Int = 0,
    ) {
        /** کل زمان تدریس‌شده (صوت + ویدیو) به ثانیه. */
        val watchedSec: Int get() = listenSec + videoSec

        /** کل طول محتوایی که تاکنون دیده/شنیده‌ایم (بیشینه‌ی طول‌های گزارش‌شده). */
        val totalSec: Int get() = audioDurSec + videoDurSec

        /** ثانیه‌ی باقی‌مانده تا پایان اولین دوره (هرگز منفی نمی‌شود). */
        val remainSec: Int get() = (totalSec - watchedSec).coerceAtLeast(0)

        /** درصد پیشرفت دوره‌ی اول (۰ تا ۱۰۰). */
        val passPct: Int
            get() = if (totalSec <= 0) 0 else ((watchedSec * 100L) / totalSec).toInt().coerceIn(0, 100)
    }

    private fun store(ctx: Context) = LocalStore(ctx.applicationContext, "hamyar_teach_stats")

    private fun key(packId: String) = "ts_$packId"

    /**
     * قلاب سینک ابری — AppContainer آن را به TeachCloud وصل می‌کند؛ هر ثبت رویداد
     * وضعیت پک را در صف‌ی outbox هم می‌گذارد (ارسال در لحظه‌های مناسب انجام می‌شود).
     * آمار محلی مرجع است؛ صف فقط برای «سینک‌شونده بودن» است.
     */
    @Volatile
    var cloudSink: ((packId: String) -> Unit)? = null

    private fun read(ctx: Context, packId: String): JSONObject =
        runCatching { JSONObject(store(ctx).getString(key(packId), "{}")) }.getOrDefault(JSONObject())

    private fun write(ctx: Context, packId: String, o: JSONObject) {
        store(ctx).putString(key(packId), o.toString())
        runCatching { cloudSink?.invoke(packId) }
    }

    /** شروع یک نشست تدریس — هر بازشدن صفحه‌ی تدریس یک بار + تاریخ نشست‌ها (حداکثر ۶۰ تا). */
    fun enter(ctx: Context, packId: String) {
        val o = read(ctx, packId)
        o.put("s", o.optInt("s") + 1)
        if (o.optLong("st") == 0L) o.put("st", System.currentTimeMillis())
        val sl = o.optJSONArray("sl") ?: org.json.JSONArray()
        sl.put(System.currentTimeMillis())
        while (sl.length() > 60) sl.remove(0)
        o.put("sl", sl)
        write(ctx, packId, o)
        StudyActivity.add(ctx, packId, "session", "شروع نشست تدریس")
    }

    /** ثبت ثانیه‌های شنیدن صوت (رویدادی از پلیر؛ [sec] ذخیره‌ی دوره‌ای است). */
    fun addListen(ctx: Context, packId: String, sec: Int, trackDurationSec: Int) {
        if (sec <= 0) return
        val o = read(ctx, packId)
        val before = o.optInt("ls")
        val after = before + sec
        o.put("ls", after)
        if (trackDurationSec > o.optInt("ad")) o.put("ad", trackDurationSec)
        write(ctx, packId, o)
        if (before / 10 != after / 10) {
            StudyActivity.add(ctx, packId, "listen", "شنیدن صوت — جمعاً ${after} ثانیه")
        }
    }

    /** ثبت ثانیه‌های تماشای واقعی ویدیو. */
    fun addVideo(ctx: Context, packId: String, sec: Int, trackDurationSec: Int) {
        if (sec <= 0) return
        val o = read(ctx, packId)
        val before = o.optInt("vs")
        val after = before + sec
        o.put("vs", after)
        if (trackDurationSec > o.optInt("vd")) o.put("vd", trackDurationSec)
        write(ctx, packId, o)
        if (before / 10 != after / 10) {
            StudyActivity.add(ctx, packId, "video", "تماشای ویدیو — جمعاً ${after} ثانیه")
        }
    }

    /** ثبت یک پرشِ بیش از ۳ ثانیه در پلیر. */
    fun addJump(ctx: Context, packId: String) {
        val o = read(ctx, packId)
        o.put("j", o.optInt("j") + 1)
        write(ctx, packId, o)
    }

    /**
     * پایانِ کاملِ یک رسانه‌ی درس (صوت درس/مقدمه/ویدیو) — کلید رسانه ثبت می‌شود
     * و تاریخ «اتمام اولین دوره» فقط با اولین بارِ کامل‌شدن همه‌ی رسانه‌ها ثبت می‌شود.
     */
    fun markTrackDone(ctx: Context, packId: String, mediaKey: String) {
        val o = read(ctx, packId)
        val dt = o.optJSONArray("dt") ?: org.json.JSONArray()
        val exists = (0 until dt.length()).any { dt.optString(it) == mediaKey }
        if (!exists) { dt.put(mediaKey); o.put("dt", dt) }
        val exp = o.optInt("exp", 1).coerceAtLeast(1)
        if (!o.optBoolean("d") && dt.length() >= exp) {
            o.put("d", true); o.put("ca", System.currentTimeMillis())
            // v1.12: «اتمام دوره‌ی اول در چند نشست» — شمار نشست‌های همین پک هنگام اولین اتمام.
            if (o.optInt("sc") == 0) o.put("sc", o.optInt("s").coerceAtLeast(1))
            StudyActivity.add(ctx, packId, "done", "اتمام دوره‌ی اول تدریس")
        }
        write(ctx, packId, o)
        StudyActivity.add(ctx, packId, "track", "اتمام رسانهٔ $mediaKey")
    }

    /** ثبت تعداد کل رسانه‌های درس — شرط اتمام دوره = کامل‌شدن همین تعداد. */
    fun expectMedia(ctx: Context, packId: String, count: Int) {
        if (count <= 0) return
        val o = read(ctx, packId)
        if (o.optInt("exp") != count) {
            o.put("exp", count)
            if (o.optBoolean("d") && (o.optJSONArray("dt")?.length() ?: 0) < count) {
                o.put("d", false); o.remove("ca")
            }
            write(ctx, packId, o)
        }
    }

    /** آیا دوره‌ی اول تدریس (همه‌ی رسانه‌ها) تمام شده؟ */
    /** JSON خام آمار یک پک — برای سینک ابری. */
    fun raw(ctx: Context, packId: String): JSONObject = read(ctx, packId)

    fun isDone(ctx: Context, packId: String): Boolean {
        val o = read(ctx, packId)
        val exp = o.optInt("exp", 1).coerceAtLeast(1)
        return (o.optJSONArray("dt")?.length() ?: 0) >= exp
    }

    fun sessionTimes(ctx: Context, packId: String): List<Long> {
        val sl = read(ctx, packId).optJSONArray("sl") ?: return emptyList()
        return (0 until sl.length()).map { sl.optLong(it) }
    }

    fun snap(ctx: Context, packId: String): Snap {
        val o = read(ctx, packId)
        val sl = o.optJSONArray("sl")
        return Snap(
            sessions = o.optInt("s"),
            listenSec = o.optInt("ls"),
            videoSec = o.optInt("vs"),
            jumps = o.optInt("j"),
            done = o.optBoolean("d"),
            doneMedia = o.optJSONArray("dt")?.length() ?: 0,
            startedAtMs = o.optLong("st"),
            lastSessionAtMs = if (sl != null && sl.length() > 0) sl.optLong(sl.length() - 1) else o.optLong("st"),
            completedAtMs = o.optLong("ca"),
            audioDurSec = o.optInt("ad"),
            videoDurSec = o.optInt("vd"),
            sessionsToDone = o.optInt("sc"),
        )
    }

    /** همه‌ی پک‌هایی که آمار دارند (برای صفحه‌ی پیشرفت). */
    /** گزارش امتحان مدرسه — روی همان پک درس، برای نمودار پیشرفت و سینک. */
    fun noteSchoolExam(ctx: Context, packId: String, iso: String, text: String) {
        if (packId.isBlank() || text.isBlank()) return
        val o = read(ctx, packId)
        val arr = o.optJSONArray("sx") ?: org.json.JSONArray()
        arr.put(
            JSONObject()
                .put("iso", iso)
                .put("t", text)
                .put("at", System.currentTimeMillis()),
        )
        o.put("sx", arr)
        write(ctx, packId, o)
    }

    fun schoolExams(ctx: Context, packId: String): List<Pair<String, String>> {
        val arr = read(ctx, packId).optJSONArray("sx") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val iso = o.optString("iso")
            val t = o.optString("t")
            if (t.isBlank()) null else iso to t
        }
    }

    fun allPackIds(ctx: Context): List<String> {
        val s = store(ctx)
        // LocalStore همه‌ی کلیدها را ندارد؛ پس کلیدهای شناخته‌شده‌ی رجیستری را می‌پیماییم.
        val ids = com.hamyareman.ir.platform.feature.study.BookModuleRegistry.modules
            .asSequence().flatMap { it.packs }.map { it.packId }.toList()
        return ids.filter { read(ctx, it).length() > 0 }
    }
}
