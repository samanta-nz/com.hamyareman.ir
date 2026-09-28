package com.hamyareman.ir.ui.chatbot

import com.hamyareman.ir.platform.core.appwrite.FunctionsService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.FunctionIds
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.security.Encryptor
import com.hamyareman.ir.util.CrisisKeywordDetector
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.hamyareman.ir.platform.core.appwrite.ServerActions

/** یک خط گفت‌وگو. فقط روی دستگاه می‌ماند (`chat_history` در فهرست never-sync است). */
data class ChatLine(
    val id: String,
    val fromBot: Boolean,
    val text: String,
    val createdAt: Long,
    val crisis: Boolean = false,
    val source: ReplySource = ReplySource.RULES_LOCAL,
)

/** پاسخ از کجا آمده — چون صداقت درباره‌ی «کی جواب داد» برای نوجوان مهم است. */
enum class ReplySource(val label: String) {
    MODEL("لایه‌ی AI (سرور)"),
    SAFETY("پاسخ ایمنی محلی"),
    RULES_LOCAL("قواعد محلی"),
    SERVER_FALLBACK("سرور نرسید — قواعد محلی"),
}

enum class ChatTone(val key: String, val label: String) {
    WARM("warm", "صمیمی"),
    FORMAL("formal", "رسمی‌تر"),
}

data class CompanionReply(val text: String, val crisis: Boolean, val source: ReplySource)

/**
 * لایه‌ی AI «همراه زهرا».
 *
 * ترتیب تصمیم (و چرا):
 *  1) **بحران** ⇒ هرگز به مدل فرستاده نمی‌شود؛ پاسخ ایمنی محلی + شماره‌های کمک.
 *  2) لایه‌ی AI خاموش یا بک‌اند تنظیم نشده ⇒ قواعد محلی (بدون اینترنت).
 *  3) در غیر این صورت ⇒ تابع سرور `ai-companion` (کلید مدل آن‌جاست، نه در اپ).
 *  4) خطای سرور ⇒ بازگشت صادقانه به قواعد محلی با پیام «نرسیدم».
 *
 * تاریخچه فقط روی دستگاه ذخیره می‌شود، با AES-256-GCM رمز می‌شود و هرگز Sync نمی‌شود.
 *
 * [encryptor] اختیاری است تا این کلاس بدون Keystore قابل تست بماند؛ وقتی داده شود،
 * متن گفت‌وگو به‌شکل رمزنگاری‌شده روی دیسک می‌رود (مثل ژورنال و نامه‌ی «خونه‌ی قبلی»).
 */
class AiCompanion(
    private val functions: FunctionsService,
    private val store: LocalStore,
    private val encryptor: Encryptor? = null,
    private val serverActions: ServerActions? = null,
) {

    val isConfigured: Boolean get() = functions.isConfigured

    var aiEnabled: Boolean
        get() = store.getBool(KEY_AI_ENABLED, true)
        set(value) = store.putBool(KEY_AI_ENABLED, value)

    var chatDisabled: Boolean
        get() = store.getBool(KEY_DISABLED, false)
        set(value) = store.putBool(KEY_DISABLED, value)

    var tone: ChatTone
        get() = ChatTone.entries.firstOrNull { it.key == store.getString(KEY_TONE, ChatTone.WARM.key) } ?: ChatTone.WARM
        set(value) = store.putString(KEY_TONE, value.key)

    // --- حافظه‌ی گفت‌وگو (فقط محلی) -----------------------------------------

    fun lines(): List<ChatLine> = runCatching {
        val array = JSONArray(readRaw())
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val text = o.optString("text")
                if (text.isBlank()) continue
                add(
                    ChatLine(
                        id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                        fromBot = o.optBoolean("fromBot"),
                        text = text,
                        createdAt = o.optLong("createdAt"),
                        crisis = o.optBoolean("crisis"),
                        source = runCatching { ReplySource.valueOf(o.optString("source")) }
                            .getOrDefault(ReplySource.RULES_LOCAL),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    fun append(line: ChatLine): ChatLine {
        val all = (lines() + line).takeLast(MAX_LINES)
        val array = JSONArray()
        all.forEach { l ->
            array.put(
                JSONObject()
                    .put("id", l.id).put("fromBot", l.fromBot).put("text", l.text)
                    .put("createdAt", l.createdAt).put("crisis", l.crisis).put("source", l.source.name),
            )
        }
        writeRaw(array.toString())
        return line
    }

    /**
     * خواندن حافظه: اول رمزگشایی؛ اگر نشد، همان متن خام را امتحان می‌کنیم تا داده‌ی
     * نسخه‌های قبلی (که رمز نشده بود) از دست نرود.
     */
    private fun readRaw(): String {
        val stored = store.getString(KEY_LINES, "")
        if (stored.isBlank()) return "[]"
        if (encryptor == null) return stored
        return encryptor.decrypt(stored) ?: stored
    }

    /**
     * نوشتن حافظه: اگر رمزنگاری ممکن نباشد، **ذخیره نمی‌کنیم** (نه اینکه متن ساده بنویسیم)؛
     * حریم خصوصی اولویت دارد و گفت‌وگو در همین نشست در حافظه می‌ماند.
     */
    private fun writeRaw(json: String) {
        val payload = encryptor?.let { runCatching { it.encrypt(json) }.getOrNull() } ?: if (encryptor == null) json else null
        if (payload != null) store.putString(KEY_LINES, payload)
    }

    fun userLine(text: String): ChatLine = append(
        ChatLine(UUID.randomUUID().toString(), false, text.trim(), System.currentTimeMillis(), source = ReplySource.RULES_LOCAL),
    )

    fun botLine(reply: CompanionReply): ChatLine = append(
        ChatLine(UUID.randomUUID().toString(), true, reply.text, System.currentTimeMillis(), reply.crisis, reply.source),
    )

    fun clearHistory() = store.remove(KEY_LINES)

    // --- پاسخ --------------------------------------------------------------

    suspend fun replyTo(message: String): CompanionReply {
        val text = message.trim()
        if (text.isBlank()) return CompanionReply("چیزی ننوشتی؛ هر وقت خواستی بنویس.", false, ReplySource.RULES_LOCAL)

        // ۱) ایمنی اول: این متن به هیچ مدلی نمی‌رود.
        if (CrisisKeywordDetector.detect(text)) {
            return CompanionReply(CRISIS_REPLY, crisis = true, source = ReplySource.SAFETY)
        }

        // ۲) قواعد محلی وقتی AI خاموش است یا بک‌اند تنظیم نشده.
        if (!aiEnabled || !isConfigured) {
            return CompanionReply(localRuleReply(text), false, ReplySource.RULES_LOCAL)
        }

        // ۳) تابع سرور
        val history = JSONArray()
        lines().takeLast(6).forEach { line ->
            history.put(
                JSONObject()
                    .put("role", if (line.fromBot) "assistant" else "user")
                    .put("content", line.text),
            )
        }
        val body = JSONObject()
            .put("message", text)
            .put("tone", tone.key)
            .put("history", history)
            .toString()

        return when (val result = functions.call(FunctionIds.AI_COMPANION, body)) {
            is AppResult.Ok -> parseReply(result.value.body, text)
            is AppResult.Err -> CompanionReply(
                "${result.error.userMessage}\n${localRuleReply(text)}",
                crisis = false,
                source = ReplySource.SERVER_FALLBACK,
            )
        }
    }

    private fun parseReply(rawBody: String, userMessage: String): CompanionReply = runCatching {
        val o = JSONObject(rawBody)
        val reply = o.optString("reply").ifBlank { o.optString("fallback") }
        when {
            o.optBoolean("crisis") -> CompanionReply(reply.ifBlank { CRISIS_REPLY }, true, ReplySource.SAFETY)
            o.optBoolean("ok") && reply.isNotBlank() -> CompanionReply(reply, false, ReplySource.MODEL)
            reply.isNotBlank() -> CompanionReply(reply, false, ReplySource.SERVER_FALLBACK)
            else -> CompanionReply(localRuleReply(userMessage), false, ReplySource.RULES_LOCAL)
        }
    }.getOrDefault(CompanionReply(localRuleReply(userMessage), false, ReplySource.RULES_LOCAL))

    companion object {
        private const val KEY_LINES = "chat_lines"
        private const val KEY_AI_ENABLED = "chat_ai_enabled"
        private const val KEY_DISABLED = "chat_disabled"
        private const val KEY_TONE = "chat_tone"
        private const val MAX_LINES = 200

        val WELCOME = "سلام. من «همراه زهرا»ام — یک برنامه، نه یک انسان. می‌تونم گوش بدم. " +
            "اگه دوست داشتی از حال امروزت بگو؛ اجباری نیست."

        const val CRISIS_REPLY = "ممنون که گفتی — گفتنش شجاعت می‌خواهد. من یک برنامه‌ام و درمانگر نیستم، " +
            "پس لطفاً همین حالا با یک آدم واقعی حرف بزن: بابا، مامان، مشاور مدرسه، یا یکی از شماره‌های " +
            "۱۴۸۰ (مشاوره‌ی بهزیستی)، ۱۲۳ (اورژانس اجتماعی) و ۱۵۷۰ (مشاوره‌ی دانش‌آموزان). " +
            "اگر در خطر فوری هستی با ۱۱۵ تماس بگیر. تو تنها نیستی."
    }
}

/**
 * قواعد محلیِ پشتیبان: همدلی کوتاه + یک سؤال کوچک.
 *
 * عمداً ساده است و هیچ‌وقت ادعای هوشمندی نمی‌کند؛ فقط وقتی که لایه‌ی AI در دسترس
 * نیست، گفت‌وگو را زنده و امن نگه می‌دارد.
 */
internal fun localRuleReply(message: String): String {
    val text = message.lowercase()
    val rules = listOf(
        listOf("خسته", "بی‌حال", "خوابم نمیبره", "بی‌خواب") to
            "خستگی واقعی است، نه بهانه. امروز یک چیز کوچیک را کم کن: یک اسکرول کمتر یا ده دقیقه زودتر خوابیدن. الان بیشتر جسمی خسته‌ای یا ذهنی؟",
        listOf("تنها", "دلتنگ", "کسی نیست") to
            "تنهایی حس سنگینی است و خیلی‌ها در سن تو تجربه‌اش می‌کنند. الان پیش چه کسی می‌تونستی فقط ده دقیقه بشینی؟",
        listOf("دعوا", "عصبانی", "جر و بحث", "داد") to
            "عصبانیت یعنی یک چیزی برات مهم بوده. قبل از هر جوابی، یک لیوان آب و پنج نفس عمیق. موضوع دعوا چی بود؟",
        listOf("امتحان", "درس", "نمره", "مدرسه") to
            "فشار درس واقعی است. بهترین قدم: فقط ۱۵ دقیقه روی یک درس، بعد استراحت. کدام درس بیشتر نگرانت کرده؟",
        listOf("بابا", "پدر", "مادر", "مامان", "خونه") to
            "رابطه‌ها گاهی پیچیده می‌شوند و این تقصیر تو نیست. دوست داری فقط تعریفش کنی یا دنبال یک قدم کوچیک باشی؟",
        listOf("غمگین", "گریه", "دلم گرفته") to
            "گریه کردن ضعف نیست؛ راه بدن برای خالی‌کردن فشار است. می‌خوای بنویسی چه چیزی امروز بیشتر از همه دلت را گرفته؟",
    )
    rules.forEach { (keys, reply) -> if (keys.any { text.contains(it) }) return reply }
    return "شنیدم. می‌خوای بیشتر ازش بگی، یا با هم یک کار کوچیک برای امروز انتخاب کنیم؟"
}
