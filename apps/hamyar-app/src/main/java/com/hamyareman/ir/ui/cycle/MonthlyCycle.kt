package com.hamyareman.ir.ui.cycle

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.study.StateSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * «چرخه ی ماهانه» — تقویم پریود + راهنمای روزبه‌روز + تمرین‌های کم‌کردن درد.
 *
 * چرا این‌جا: نسخه‌ی قبلی («چرخه و حال‌ها») فقط یک دکمه‌ی «ثبت شروع دوره» روی
 * LocalStore بود؛ نه تقویم داشت، نه راهنما، نه سینکِ سرور. این فایل مدل و منطق را
 * نگه می‌دارد (خالص و تست‌پذیر). نمایش در CycleCal / CycleLog / CycleToday است.
 *
 * **حریم خصوصی:** داده زیرِ کلیدِ `cycle_month` در جدولِ `app_state` و با مجوزِ
 * مالکیتِ خودِ کاربر (`ownerOnly`) سینک می‌شود، یعنی همان قراردادِ «چرخه هرگز
 * برای کسی دیگر خواندنی نیست». برای پسران کلِ این بخش پنهان است.
 */
object MonthlyCycle {

    /** کلیدِ سینک در `app_state` (ردیفِ قطعیِ همان کاربر). */
    const val KEY = "cycle_month"

    const val DEFAULT_CYCLE = 28
    const val DEFAULT_PERIOD = 5

    data class State(
        /** روزهای ثبت‌شده‌ی پریود: ISO → «1» (کلید = تاریخِ میلادیِ ISO). */
        val periodDays: Set<String> = emptySet(),
        /** تاریخِ ISOِ آخرین شروعی که خودِ کاربر گفته (پایه‌ی پیش‌بینی). */
        val lastStart: String = "",
        /** طولِ چرخه (۲۱..۳۵). */
        val cycleLength: Int = DEFAULT_CYCLE,
        /** طولِ پریود (۲..۱۰) برای پیش‌بینی روزهای بعدی. */
        val periodLength: Int = DEFAULT_PERIOD)

    /** فازِ چرخه برای یک روز — برای راهنمای همان روز. */
    enum class Phase { PERIOD, PMS, FOLLICULAR, OVULATION, LUTEAL }

    // ---------------- خواندن/نوشتن محلی ----------------

    private const val PREF = "hamyar_cycle"
    private const val P_JSON = "cycle_month_json"
    private const val P_AT = "cycle_month_at"

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun load(ctx: Context): State = fromJson(store(ctx).getString(P_JSON))

    /** زمانِ آخرین تغییرِ محلی — همان زبانی که [StateSync] می‌فهمد. */
    fun localAt(ctx: Context): Long = StateSync.localAt(ctx, KEY)

    fun saveLocal(ctx: Context, state: State) {
        store(ctx).putString(P_JSON, toJson(state))
        store(ctx).putLong(P_AT, System.currentTimeMillis())
        // به StateSync بگو «این کلید محلی عوض شد» تا pullِ بعدی نسخهٔ کهنهٔ سرور را
        // روی داده‌ی تازه نگذارد.
        StateSync.markLocal(ctx, KEY)
    }

    /** اعمالِ نسخهٔ سرور روی دستگاه (بدونِ push). */
    fun applyRemote(ctx: Context, json: String, at: Long) {
        store(ctx).putString(P_JSON, json)
        StateSync.markSyncedAt(ctx, KEY, at)
    }

    // ---------------- JSON ----------------

    fun toJson(state: State): String {
        val arr = JSONArray()
        state.periodDays.sorted().forEach { arr.put(it) }
        return JSONObject()
            .put("days", arr)
            .put("lastStart", state.lastStart)
            .put("cycle", state.cycleLength)
            .put("period", state.periodLength)
            .toString()
    }

    fun fromJson(json: String): State {
        if (json.isBlank()) return State()
        return runCatching {
            val o = JSONObject(json)
            val arr = o.optJSONArray("days") ?: JSONArray()
            val days = (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.length == 10 } }.toSet()
            State(
                periodDays = days,
                lastStart = o.optString("lastStart"),
                cycleLength = o.optInt("cycle", DEFAULT_CYCLE).coerceIn(21, 35),
                periodLength = o.optInt("period", DEFAULT_PERIOD).coerceIn(2, 10))
        }.getOrDefault(State())
    }

    // ---------------- ویرایش ----------------

    /** مهارِ عددها به بازهٔ مجاز — منبعِ داده ممکن است هر عددی بدهد. */
    private fun State.saned(): State = copy(
        cycleLength = cycleLength.coerceIn(21, 35),
        periodLength = periodLength.coerceIn(2, 10))

    fun toggleDay(state: State, iso: String): State = state.copy(
        periodDays = if (iso in state.periodDays) state.periodDays - iso else state.periodDays + iso)

    /** «از این روز دوره شروع شد»: این روز و روزهای بعدیِ آن دوره علامت می‌خورند. */
    fun markStart(state: State, iso: String): State {
        val days = mutableSetOf<String>()
        var d = LocalDate.parse(iso)
        repeat(state.saned().periodLength) {
            days += d.toString()
            d = d.plusDays(1)
        }
        return state.copy(periodDays = state.periodDays + days, lastStart = iso)
    }

    // ---------------- محاسبه ----------------

    /** تاریخِ ISOِ شروعِ بعدیِ پیش‌بینی‌شده. */
    fun nextStart(state: State): String? {
        if (state.lastStart.length != 10) return null
        return runCatching { LocalDate.parse(state.lastStart).plusDays(state.cycleLength.toLong()).toString() }
            .getOrNull()
    }

    /** چند روز تا شروعِ بعدی (منفی = گذشته). */
    fun daysToNext(state: State, today: String = LocalDate.now(JalaliDate.TEHRAN).toString()): Int? {
        val next = nextStart(state) ?: return null
        return runCatching {
            (LocalDate.parse(next).toEpochDay() - LocalDate.parse(today).toEpochDay()).toInt()
        }.getOrNull()
    }

    /**
     * «روزِ چندمِ این دوره» برای روزِ [iso] — اگر آن روز در روزهای ثبت‌شده باشد.
     *
     * از **ابتدای همان رشتهٔ پیوسته** می‌شماریم (نه از آخرین روزِ ثبت‌شده):
     * نسخهٔ اول از نزدیک‌ترین روزِ کوچک‌تر می‌شمرد و برای ۳ شهریور در دوره‌ای که
     * ۱ تا ۵ شهریور ثبت شده بود، «روزِ ۱» می‌داد.
     */
    fun periodDayNumber(state: State, iso: String): Int? {
        if (iso !in state.periodDays) return null
        var day = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return null
        var n = 1
        while (n < 12) {
            val prev = day.minusDays(1)
            if (prev.toString() !in state.periodDays) break
            day = prev
            n++
        }
        return if (n in 1..10) n else null
    }

    /**
     * فازِ روز. اولویت با داده‌ی ثبت‌شده است (اگر خودِ کاربر «پریود» علامت زده،
     * همان حرف اول را می‌زند) و بعد از روی تاریخ‌ها پیش‌بینی می‌شود.
     */
    fun phase(state: State, iso: String): Phase {
        if (iso in state.periodDays) return Phase.PERIOD
        val n = daysToNext(state, iso)
        val cyc = state.cycleLength.coerceIn(21, 35)
        if (n != null && n in 0..cyc) {
            // n = چند روز تا شروعِ بعدی؛ روزِ چرخه = cyc - n.
            val dayInCycle = cyc - n
            return when {
                n in 0..2 -> Phase.PMS              // ۲ روزِ آخرِ چرخه
                dayInCycle in 12..16 -> Phase.OVULATION
                dayInCycle < 12 -> Phase.FOLLICULAR
                else -> Phase.LUTEAL
            }
        }
        return Phase.FOLLICULAR
    }

    /** کارتِ «امروز»: عنوان + متنِ راهنما. */
    fun todayCard(state: State, iso: String): Pair<String, String> {
        val phase = phase(state, iso)
        val periodDay = periodDayNumber(state, iso)
        val title = when (phase) {
            Phase.PERIOD -> "روزِ پریود" + (periodDay?.let { " — روزِ ${toPersianDigits(it.toString())}" } ?: "")
            Phase.PMS -> "نزدیکِ پریود (PMS)"
            Phase.FOLLICULAR -> "روزهای آرام بعد از پریود"
            Phase.OVULATION -> "روزهای میانهٔ چرخه"
            Phase.LUTEAL -> "فازِ بعد از میانه"
        }
        val body = when (phase) {
            Phase.PERIOD -> periodAdvice(periodDay ?: 1)
            Phase.PMS -> PMS_ADVICE
            Phase.FOLLICULAR -> FOLLICULAR_ADVICE
            Phase.OVULATION -> OVULATION_ADVICE
            Phase.LUTEAL -> LUTEAL_ADVICE
        }
        return title to body
    }

    // ---------------- راهنماها (متن نوشته‌شده برای همین صفحه) ----------------

    /** راهنمای روزبه‌روزِ پریود: روزِ ۱ تا ۵ به بعد. */
    fun periodAdvice(day: Int): String = when (day) {
        1 -> "روزِ اول معمولاً سنگین‌ترین روز است: کمپرسِ گرم روی شکم/کمر، آبِ کافی و استراحت. " +
            "اگر مدرسه داری، زنگ‌های تفریح یک‌بار دراز بکش. دردِ شدید و غیرمعمول را به مامان یا پزشک بگو."
        2 -> "روزِ دوم: گرم بمان، مایعات گرم بخور و از حرکاتِ ملایمِ کششی کمک بگیر (نه ورزشِ سخت). " +
            "اگر سرگیجه داری، کمی نمک و مایع شیرین کمکت می‌کند."
        3 -> "روزِ سوم: معمولاً درد کم می‌شود. یک پیاده‌رویِ کوتاه به جریانِ خون و بهتر شدنِ حالت کمک می‌کند."
        4 -> "روزِ چهارم: انرژی برمی‌گردد؛ کارهای عقب‌افتادهٔ سبک را انجام بده و خوب بخواب."
        else -> "روزهای پایانی: سبک ورزش کن، آهن‌دار بخور (عدس، اسفناج، خرما) و آب زیاد بنوش."
    }

    private const val PMS_ADVICE =
        "چند روز مانده به پریود: خلق ممکن است نوسان کند و کمی دل‌درد داشته باشی. " +
            "قندِ زیاد و کافئینِ زیاد حال را بدتر می‌کند؛ خوابِ منظم، حرکاتِ ملایم و شکلاتِ تلخِ کم بهترین‌اند. " +
            "اگر خودت یا دیگران را اذیت می‌کند، همان را بگو — این حالت طبیعی است، نه تقصیرِ تو."

    private const val FOLLICULAR_ADVICE =
        "روزهای آرام و پرانرژی: بدن در حال بازسازی است. ورزش و کارهای سخت را اگر می‌خواهی همین روزها بگذار."

    private const val OVULATION_ADVICE =
        "روزهای میانهٔ چرخه: ممکن است کمی ترشحِ شفاف یا دردِ خفیفِ یک‌طرفه ببینی؛ معمولاً طبیعی است. " +
            "اگر دردِ شدید یا لکه‌بینیِ بی‌دلیل داشتی، پیگیری کن."

    private const val LUTEAL_ADVICE =
        "فازِ بعد از میانه: خواب و آب را جدی بگیر، نمکِ زیاد و فست‌فود حالِ نفخ را بدتر می‌کند."

    /**
     * تمرین‌های کم‌کردنِ درد. هیچ‌کدام دردناک نیستند؛ اگر حرکتی درد را بیشتر کرد،
     * همان را کنار بگذار. (فایل‌های صوتی همین تمرین‌ها در «حرکات ورزشی و یوگا»ست.)
     */
    val painExercises: List<Exercise> = listOf(
        Exercise("🌡", "کمپرسِ گرم", "۱۰ تا ۱۵ دقیقه روی شکم یا کمر", "کیسهٔ آب گرم یا حولهٔ گرمِ مرطوب؛ ساده‌ترین و اثرگذارترین کار در روزِ اول."),
        Exercise("🫁", "تنفسِ آرامِ ۴-۷-۸", "۳ دقیقه", "۴ شماره دم، ۷ شماره نگه، ۸ شماره بازدم. ضربان و گرفتگی را کم می‌کند."),
        Exercise("🧘", "وضعیتِ درازکشِ راحت", "۵ دقیقه", "به پشت بخواب، زانوها را خم کن و زیرشان بالش بگذار؛ فشارِ شکم کم می‌شود."),
        Exercise("🐈", "گربه-شترِ ملایم", "۸ تکرارِ آرام", "روی چهار دست‌وپا، ستون فقرات را آرام قوس بده و رها کن؛ نه تند، نه تا درد."),
        Exercise("🚶", "پیاده‌رویِ کوتاه", "۱۰ دقیقه", "قدمِ آرام و نفسِ راحت؛ جریانِ خون بهتر می‌شود و دل‌درد سبک‌تر."),
        Exercise("💧", "آب و خوراکِ گرم", "در طول روز", "سوپ، دمنوش و آب؛ کافئین و نوشابهٔ گازدار را کم کن."),
        Exercise("😴", "خوابِ کافی", "۸ ساعت", "خوابِ کم درد را بدتر می‌کند؛ شبِ قبلِ دوره کمی زودتر بخواب."))

    data class Exercise(val emoji: String, val title: String, val duration: String, val how: String)

    // ---------------- سینک با سرور ----------------

    /**
     * یکی‌کردنِ محلی و سرور (آخرین نوشته برنده) — همان قراردادِ `StateSync`.
     * خروجی: `true` اگر چیزی تغییر کرد.
     */
    suspend fun sync(
        ctx: Context,
        tables: TablesDbService,
        uid: String): Boolean = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext false
        val local = store(ctx).getString(P_JSON)
        val localAt = StateSync.localAt(ctx, KEY)
        when (val r = StateSync.pull(ctx, tables, uid, KEY)) {
            null -> {
                // سطرِ سرور نداریم (یا سرور در دسترس نیست) → نسخهٔ محلی را بفرست.
                if (local.isNotBlank()) StateSync.push(ctx, tables, uid, KEY, local)
                false
            }
            else -> {
                val (remoteJson, remoteAt) = r
                when {
                    remoteAt > localAt && remoteJson != local -> {
                        applyRemote(ctx, remoteJson, remoteAt)
                        true
                    }
                    local.isNotBlank() && remoteJson != local ->
                        StateSync.push(ctx, tables, uid, KEY, local)
                    else -> false
                }
            }
        }
    }

    /** دکمهٔ «ذخیره در سرور» — صریح و با نتیجهٔ صادقانه. */
    suspend fun pushNow(ctx: Context, tables: TablesDbService, uid: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            if (uid.isBlank()) {
                AppResult.Err(com.hamyareman.ir.platform.core.common.AppError.Auth())
            } else {
                if (StateSync.push(ctx, tables, uid, KEY, toJson(load(ctx)))) {
                    AppResult.Ok(Unit)
                } else {
                    AppResult.Err(
                        com.hamyareman.ir.platform.core.common.AppError.Local(
                            "سرور جواب نداد؛ داده همین‌جا روی گوشی محفوظ است و بعداً دوباره فرستاده می‌شود."))
                }
            }
        }
}
