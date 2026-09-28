package com.hamyareman.ir.platform.feature.playback

import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.BackendConfig
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.sync.SyncEngine
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/**
 * پرامپت ۰۱ — حافظه‌ی پیشرفت پلیر ویدیو/صوت.
 *
 * هر سطر در `lesson_media_progress` با کلید (userId, bookCode, lessonId, mediaType) یکتا است.
 * رفتار:
 *  - [load] از سرور می‌خواند و در کش محلی نگه می‌دارد؛ اگر بک‌اند نبود، فقط محلی کار می‌کند.
 *  - [recordProgress] هر ۵ ثانیه یا رویداد (مکث/خروج/تغییر سرعت/seek بزرگ) صدا زده می‌شود.
 *  - رویدادهای `viewHistory` و `seekJumps` در **سمت کلاینت append** می‌شوند و در سینک
 *    بعدی به سرور append می‌شوند (طبق پرامپت: append، نه overwrite).
 *  - **debounce سمت کلاینت**: اگر همان کلید در outbox موجود باشد، رکورد جدید جایگزین
 *    می‌شود تا صف ارسال باد نکند.
 */
class LessonMediaProgressRepository(
    private val store: LocalStore,
    private val tables: TablesDbService,
    private val provider: BackendConfig,
    private val sync: SyncEngine,
) {

    /**
     * ساختار پیشرفت یک رسانه (ویدیو یا صوت) برای یک درس.
     * صفر = «تماشا نشده».
     */
    data class Progress(
        val bookCode: String,
        val lessonId: String,
        val mediaType: MediaType,
        val lastPositionSec: Double = 0.0,
        val durationSec: Double = 0.0,
        val playbackSpeed: Double = 1.0,
        val isCompleted: Boolean = false,
        val viewCount: Int = 0,
        val chapterIndex: Int = 0,
        val lastViewedAtIso: String = "",
        val viewHistory: List<ViewEvent> = emptyList(),
        val seekJumps: List<SeekEvent> = emptyList(),
    ) {
        /** درصد پیشرفت ۰ تا ۱ (اگر مدت‌زمان نامعتبر باشد، صفر). */
        val fraction: Float
            get() = if (durationSec > 0.0) (lastPositionSec / durationSec).coerceIn(0.0, 1.0).toFloat() else 0f
    }

    data class ViewEvent(
        val dateIso: String,
        val speed: Double,
        val fromSec: Double,
        val toSec: Double,
        val isReplay: Boolean,
    )

    data class SeekEvent(
        val atClientTimeIso: String,
        val fromSec: Double,
        val toSec: Double,
        val direction: String, // "forward" | "backward"
    )

    enum class MediaType(val wire: String) {
        VIDEO("video"),
        AUDIO("audio");

        companion object {
            fun fromWire(s: String?): MediaType = entries.firstOrNull { it.wire == s } ?: VIDEO
        }
    }

    /**
     * کلید محلی برای کش پیشرفت. شامل userId تا در دستگاه‌های مشترک (اپ پدر) قاطی نشود.
     */
    private fun cacheKey(userId: String, bookCode: String, lessonId: String, mediaType: MediaType): String =
        "media_progress_${userId}_${bookCode}_${lessonId}_${mediaType.wire}"

    /**
     * ساخت شناسه‌ی پایدار سطر. اگر userId نداشته باشیم از «anon» استفاده می‌شود؛
     * یعنی پیشرفت قبل از لاگین هم ذخیره می‌شود ولی بعد از اولین لاگین قابل‌انتقال نیست.
     */
    private fun stableRowId(userId: String, bookCode: String, lessonId: String, mediaType: MediaType): String {
        val u = userId.ifBlank { "anon" }
        return "${u}__${bookCode}__${lessonId}__${mediaType.wire}".take(64)
    }

    /**
     * بارگذاری پیشرفت از کش محلی. اگر [forceRefresh] باشد از سرور هم می‌پرسد.
     * در حالت بدون بک‌اند، فقط کش برمی‌گردد.
     */
    suspend fun load(
        userId: String,
        bookCode: String,
        lessonId: String,
        mediaType: MediaType,
        forceRefresh: Boolean = false,
    ): Progress {
        val key = cacheKey(userId, bookCode, lessonId, mediaType)
        val cached = readCache(key)

        if (!forceRefresh && cached != null) return cached

        if (provider.isConfigured) {
            val rowId = stableRowId(userId, bookCode, lessonId, mediaType)
            val query = listOf("equal(\"\$id\",[\"$rowId\"])")
            when (val res = tables.list(TableIds.LESSON_MEDIA_PROGRESS, query)) {
                is AppResult.Ok -> {
                    val row = res.value.firstOrNull()
                    if (row != null) {
                        val p = parseRow(row.data, bookCode, lessonId, mediaType)
                        writeCache(key, p)
                        return p
                    }
                }
                is AppResult.Err -> { /* ignore, use cached */ }
            }
        }
        return cached ?: Progress(bookCode, lessonId, mediaType)
    }

    /**
     * ثبت یک snapshot پیشرفت.
     * - اگر [appendView] باشد، یک [ViewEvent] به `viewHistory` اضافه می‌شود
     *   و اگر [markCompleted] هم true باشد، `isCompleted` و `viewCount++` ست می‌شوند.
     * - اگر [seekJump] غیر null باشد، یک [SeekEvent] ثبت می‌شود (فقط برای پرش‌های > 3s).
     *
     * این تابع **همیشه محلی** می‌نویسد و در [SyncEngine] صف می‌سازد.
     * هیچ تماس مستقیمی با شبکه در حین پخش نمی‌کند.
     */
    fun recordProgress(
        userId: String,
        bookCode: String,
        lessonId: String,
        mediaType: MediaType,
        positionSec: Double,
        durationSec: Double,
        speed: Double,
        chapterIndex: Int = 0,
        seekJump: SeekEvent? = null,
        appendView: Boolean = false,
        markCompleted: Boolean = false,
    ) {
        val key = cacheKey(userId, bookCode, lessonId, mediaType)
        val current = readCache(key) ?: Progress(bookCode, lessonId, mediaType)

        val nowIso = Instant.now().toString()
        val newViewCount = if (markCompleted && !current.isCompleted) current.viewCount + 1 else current.viewCount
        val isReplay = newViewCount > 1

        val newHistory = if (appendView) {
            current.viewHistory + ViewEvent(
                dateIso = nowIso,
                speed = speed,
                fromSec = if (markCompleted) 0.0 else current.lastPositionSec,
                toSec = positionSec,
                isReplay = isReplay,
            )
        } else current.viewHistory

        val newSeeks = if (seekJump != null) current.seekJumps + seekJump else current.seekJumps

        val updated = current.copy(
            lastPositionSec = positionSec,
            durationSec = if (durationSec > 0) durationSec else current.durationSec,
            playbackSpeed = speed,
            isCompleted = current.isCompleted || markCompleted,
            viewCount = newViewCount,
            chapterIndex = chapterIndex,
            lastViewedAtIso = nowIso,
            viewHistory = newHistory,
            seekJumps = newSeeks,
        )
        writeCache(key, updated)

        // صف ارسال
        val rowId = stableRowId(userId, bookCode, lessonId, mediaType)
        val payload = mapOf(
            "userId" to userId.ifBlank { "anon" },
            "bookCode" to bookCode,
            "lessonId" to lessonId,
            "mediaType" to mediaType.wire,
            "lastPositionSec" to updated.lastPositionSec,
            "durationSec" to updated.durationSec,
            "playbackSpeed" to updated.playbackSpeed,
            "chapterIndex" to updated.chapterIndex,
            "isCompleted" to updated.isCompleted,
            "viewCount" to updated.viewCount,
            "lastViewedAtIso" to updated.lastViewedAtIso,
            "viewHistory" to encodeJsonArray(updated.viewHistory.map { it.toJson() }),
            "seekJumps" to encodeJsonArray(updated.seekJumps.map { it.toJson() }),
            "updatedAtIso" to nowIso,
        )
        sync.enqueue(TableIds.LESSON_MEDIA_PROGRESS, rowId, payload)
    }

    /** پاک کردن کش محلی (برای تست یا «شروع تازه»). */
    fun reset(userId: String, bookCode: String, lessonId: String, mediaType: MediaType) {
        store.putString(cacheKey(userId, bookCode, lessonId, mediaType), "")
    }

    // ---------- داخلی ----------

    private fun readCache(key: String): Progress? {
        val raw = store.getString(key)
        if (raw.isBlank()) return null
        return runCatching {
            val o = JSONObject(raw)
            Progress(
                bookCode = o.optString("bookCode"),
                lessonId = o.optString("lessonId"),
                mediaType = MediaType.fromWire(o.optString("mediaType")),
                lastPositionSec = o.optDouble("lastPositionSec", 0.0),
                durationSec = o.optDouble("durationSec", 0.0),
                playbackSpeed = o.optDouble("playbackSpeed", 1.0),
                chapterIndex = o.optInt("chapterIndex", 0),
                isCompleted = o.optBoolean("isCompleted", false),
                viewCount = o.optInt("viewCount", 0),
                lastViewedAtIso = o.optString("lastViewedAtIso"),
                viewHistory = parseViewEvents(o.optJSONArray("viewHistory")),
                seekJumps = parseSeekEvents(o.optJSONArray("seekJumps")),
            )
        }.getOrNull()
    }

    private fun writeCache(key: String, p: Progress) {
        val o = JSONObject()
            .put("bookCode", p.bookCode)
            .put("lessonId", p.lessonId)
            .put("mediaType", p.mediaType.wire)
            .put("chapterIndex", p.chapterIndex)
            .put("lastPositionSec", p.lastPositionSec)
            .put("durationSec", p.durationSec)
            .put("playbackSpeed", p.playbackSpeed)
            .put("isCompleted", p.isCompleted)
            .put("viewCount", p.viewCount)
            .put("lastViewedAtIso", p.lastViewedAtIso)
            .put("viewHistory", JSONArray(p.viewHistory.map { it.toJson() }))
            .put("seekJumps", JSONArray(p.seekJumps.map { it.toJson() }))
        store.putString(key, o.toString())
    }

    private fun parseRow(data: Map<String, Any?>, bookCode: String, lessonId: String, mediaType: MediaType): Progress {
        val viewHistoryRaw = data["viewHistory"] as? String ?: "[]"
        val seekJumpsRaw = data["seekJumps"] as? String ?: "[]"
        return Progress(
            bookCode = (data["bookCode"] as? String) ?: bookCode,
            lessonId = (data["lessonId"] as? String) ?: lessonId,
            mediaType = MediaType.fromWire(data["mediaType"] as? String),
            lastPositionSec = (data["lastPositionSec"] as? Number)?.toDouble() ?: 0.0,
            durationSec = (data["durationSec"] as? Number)?.toDouble() ?: 0.0,
            playbackSpeed = (data["playbackSpeed"] as? Number)?.toDouble() ?: 1.0,
            isCompleted = data["isCompleted"] as? Boolean ?: false,
            viewCount = (data["viewCount"] as? Number)?.toInt() ?: 0,
            lastViewedAtIso = (data["lastViewedAtIso"] as? String) ?: "",
            viewHistory = parseViewEvents(runCatching { JSONArray(viewHistoryRaw) }.getOrNull()),
            seekJumps = parseSeekEvents(runCatching { JSONArray(seekJumpsRaw) }.getOrNull()),
        )
    }

    private fun ViewEvent.toJson(): JSONObject = JSONObject()
        .put("date", dateIso)
        .put("speed", speed)
        .put("fromSec", fromSec)
        .put("toSec", toSec)
        .put("isReplay", isReplay)

    private fun SeekEvent.toJson(): JSONObject = JSONObject()
        .put("atClientTime", atClientTimeIso)
        .put("fromSec", fromSec)
        .put("toSec", toSec)
        .put("direction", direction)

    private fun parseViewEvents(arr: JSONArray?): List<ViewEvent> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ViewEvent(
                        dateIso = o.optString("date"),
                        speed = o.optDouble("speed", 1.0),
                        fromSec = o.optDouble("fromSec", 0.0),
                        toSec = o.optDouble("toSec", 0.0),
                        isReplay = o.optBoolean("isReplay", false),
                    )
                )
            }
        }
    }

    private fun parseSeekEvents(arr: JSONArray?): List<SeekEvent> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    SeekEvent(
                        atClientTimeIso = o.optString("atClientTime"),
                        fromSec = o.optDouble("fromSec", 0.0),
                        toSec = o.optDouble("toSec", 0.0),
                        direction = o.optString("direction", "forward"),
                    )
                )
            }
        }
    }

    private fun encodeJsonArray(jsonObjects: List<JSONObject>): String {
        val arr = JSONArray()
        jsonObjects.forEach { arr.put(it) }
        return arr.toString()
    }

    companion object {
        /** آستانه‌ی تشخیص «پرش بزرگ» برای ثبت در seekJumps (طبق پرامپت: ۳ ثانیه). */
        const val SEEK_JUMP_THRESHOLD_SEC = 3.0
    }
}
