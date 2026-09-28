package com.hamyareman.ir.platform.feature.hearttoheart

import io.appwrite.Query
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.AppwriteRealtimeFeed
import com.hamyareman.ir.platform.core.appwrite.RealtimeRowEvent
import com.hamyareman.ir.platform.core.appwrite.StorageService
import com.hamyareman.ir.platform.core.appwrite.StoredFile
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.BucketIds
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * «حرف دل» بین زهرا و پدر: متن، ویس، عکس، ویدیو و فایل.
 *
 * اصول پیاده‌سازی:
 *  1) اول محلی: پیام فوری در کش دستگاه ذخیره می‌شود و UI همان لحظه آن را نشان می‌دهد،
 *     حتی اگر اینترنت نباشد. بعداً [pushPending] آن را به سرور می‌فرستد.
 *  2) دسترسی سطر: `Role.user(زهرا)` + `Role.user(پدر پیوندشده)`. هیچ‌کس دیگر،
 *     حتی با دانستن شناسه‌ی سطر، نمی‌تواند بخواند.
 *  3) رسانه‌ها در باکت `heart-to-heart-media` می‌روند؛ باکت خصوصی زهرا
 *     (`zahra-private`) جای دیگری است و این ماژول هرگز به آن دست نمی‌زند.
 */
class HeartRepository(
    private val store: LocalStore,
    private val tables: TablesDbService,
    private val storage: StorageService,
    private val provider: AppwriteClientProvider? = null,
    private val partnerUserId: () -> String? = { null },
    private val realtime: AppwriteRealtimeFeed? = null,
) {

    val isConfigured: Boolean get() = tables.isConfigured

    fun tableId(): String = TableIds.FATHER_MESSAGES

    /**
     * جریان رویدادهای زنده‌ی جدول پیام‌ها.
     *
     * اگر Realtime در دسترس نباشد `emptyFlow` برمی‌گردد، یعنی رفتار دقیقاً مثل قبل
     * می‌ماند (refresh دستی/هنگام بازکردن صفحه) و چیزی خراب نمی‌شود.
     */
    fun liveUpdates(): Flow<RealtimeRowEvent> =
        realtime?.messages() ?: emptyFlow()

    // --- کش محلی -----------------------------------------------------------

    fun messages(): List<HeartMessage> = readCache()

    fun pendingCount(): Int = readCache().count { !it.synced }

    // --- ارسال -------------------------------------------------------------

    suspend fun sendText(text: String, direction: MessageDirection): AppResult<HeartMessage> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return AppResult.Err(AppError.Validation("پیام خالی است."))
        return commit(HeartMessage(UUID.randomUUID().toString(), MessageType.TEXT, direction, trimmed, System.currentTimeMillis()))
    }

    suspend fun sendVoice(
        recording: VoiceRecorder.Recording,
        direction: MessageDirection,
    ): AppResult<HeartMessage> = commit(
        HeartMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.VOICE,
            direction = direction,
            text = "پیام صوتی",
            createdAt = System.currentTimeMillis(),
            mediaPath = recording.path,
            durationMs = recording.durationMs,
        ),
    )

    suspend fun sendMedia(
        file: File,
        type: MessageType,
        caption: String,
        direction: MessageDirection,
    ): AppResult<HeartMessage> {
        if (!file.exists()) return AppResult.Err(AppError.Validation("فایل پیدا نشد."))
        if (!MediaFiles.isWithinLimit(file)) {
            return AppResult.Err(AppError.Validation("حجم فایل بیشتر از ۱۰ مگابایت است."))
        }
        return commit(
            HeartMessage(
                id = UUID.randomUUID().toString(),
                type = type,
                direction = direction,
                text = caption.trim().ifBlank { file.name },
                createdAt = System.currentTimeMillis(),
                mediaPath = file.absolutePath,
            ),
        )
    }

    /**
     * ذخیره‌ی محلی + تلاش برای آپلود رسانه و ساخت سطر سرور.
     * اگر سرور در دسترس نبود، پیام با `synced = false` در کش می‌ماند.
     */
    private suspend fun commit(message: HeartMessage): AppResult<HeartMessage> {
        var prepared = message
        val localPath = message.mediaPath

        if (localPath != null && isConfigured) {
            val permissions = rowPermissions()
            when (val upload = storage.upload(BucketIds.HEART_MEDIA, localPath, permissions)) {
                is AppResult.Ok -> prepared = message.copy(mediaRef = upload.value.reference)
                is AppResult.Err -> Unit // بدون رسانه هم متن پیام می‌رود؛ بعداً دوباره تلاش می‌شود
            }
        }

        appendCache(prepared)

        if (!isConfigured) {
            return AppResult.Ok(prepared.copy(synced = false))
        }

        val payload = payloadOf(prepared)
        val permissions = rowPermissions()
        return when (val result = tables.upsert(tableId(), prepared.id, payload, permissions)) {
            is AppResult.Ok -> {
                markSynced(prepared.id)
                AppResult.Ok(prepared.copy(synced = true))
            }

            is AppResult.Err -> AppResult.Ok(prepared.copy(synced = false))
        }
    }

    // --- همگام‌سازی ---------------------------------------------------------

    /** ارسال پیام‌های باقی‌مانده در صف (آفلاین → آنلاین). */
    suspend fun pushPending(): AppResult<Int> {
        if (!isConfigured) return AppResult.Ok(0)
        val permissions = rowPermissions()
        var pushed = 0
        readCache().filterNot { it.synced }.forEach { message ->
            var toSend = message
            if (message.mediaRef == null && message.mediaPath != null) {
                when (val upload = storage.upload(BucketIds.HEART_MEDIA, message.mediaPath, permissions)) {
                    is AppResult.Ok -> toSend = message.copy(mediaRef = upload.value.reference)
                    is AppResult.Err -> return@forEach
                }
            }
            if (tables.upsert(tableId(), toSend.id, payloadOf(toSend), permissions) is AppResult.Ok) {
                markSynced(toSend.id)
                pushed++
            }
        }
        return AppResult.Ok(pushed)
    }

    /** گرفتن پیام‌های جدید از سرور و ادغام با کش محلی. */
    suspend fun refresh(): AppResult<List<HeartMessage>> {
        if (!isConfigured) return AppResult.Ok(readCache())
        val queries = listOf(
            Query.orderDesc("createdAtMs"),
            Query.limit(200),
        )
        return when (val result = tables.list(tableId(), queries)) {
            is AppResult.Ok -> {
                val remote = result.value.map { it.toMessage() }
                val merged = merge(remote, readCache())
                writeCache(merged)
                AppResult.Ok(merged)
            }

            is AppResult.Err -> AppResult.Ok(readCache())
        }
    }

    /** لینک پخش/نمایش رسانه از Storage (برای ویس یا عکسِ سمت مقابل). */
    suspend fun mediaUrl(message: HeartMessage): String? {
        val ref = message.mediaRef ?: return message.mediaPath
        val stored = StoredFile.parse(ref) ?: return message.mediaPath
        return storage.viewUrl(stored.bucketId, stored.fileId) ?: message.mediaPath
    }

    // --- نگاشت و ذخیره ------------------------------------------------------

    private fun payloadOf(message: HeartMessage): Map<String, Any?> = mapOf(
        "messageId" to message.id,
        "type" to message.type.name,
        "direction" to message.direction.name,
        "text" to message.text,
        "createdAtMs" to message.createdAt,
        "mediaRef" to (message.mediaRef ?: ""),
        "durationMs" to (message.durationMs ?: 0L),
        "toUserId" to (partnerUserId() ?: ""),
    )

    private fun TableRow.toMessage(): HeartMessage = HeartMessage(
        id = string("messageId").ifBlank { id },
        type = runCatching { MessageType.valueOf(string("type", "TEXT")) }.getOrDefault(MessageType.TEXT),
        direction = runCatching {
            MessageDirection.valueOf(string("direction", MessageDirection.TO_FATHER.name))
        }.getOrDefault(MessageDirection.TO_FATHER),
        text = string("text"),
        createdAt = long("createdAtMs").takeIf { it > 0 } ?: System.currentTimeMillis(),
        mediaRef = string("mediaRef").ifBlank { null },
        durationMs = long("durationMs").takeIf { it > 0 },
        synced = true,
    )

    /** دسترسی سطر: خواندن/به‌روزرسانی برای خودم و خواندن برای طرف پیوندشده. */
    private fun rowPermissions(): List<String> =
        provider?.let { AppwriteClientProvider.sharedWith(localSelfId(), partnerUserId()) }
            ?: emptyList()

    private fun localSelfId(): String = store.getString("auth_user_id")

    private fun merge(remote: List<HeartMessage>, local: List<HeartMessage>): List<HeartMessage> {
        val byId = LinkedHashMap<String, HeartMessage>()
        local.forEach { byId[it.id] = it }
        remote.forEach { message ->
            val existing = byId[message.id]
            byId[message.id] = if (existing == null) {
                message
            } else {
                // نسخه‌ی سرور معتبر است، اما مسیر محلی رسانه را نگه می‌داریم تا آفلاین پخش شود.
                message.copy(
                    mediaPath = existing.mediaPath ?: message.mediaPath,
                    synced = true,
                )
            }
        }
        return byId.values.sortedBy { it.createdAt }
    }

    private fun readCache(): List<HeartMessage> = runCatching {
        val array = JSONArray(store.getString(KEY_CACHE, "[]"))
        buildList {
            for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i)))
        }.sortedBy { it.createdAt }
    }.getOrDefault(emptyList())

    private fun writeCache(list: List<HeartMessage>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        store.putString(KEY_CACHE, array.toString())
    }

    private fun appendCache(message: HeartMessage) {
        writeCache((readCache() + message).sortedBy { it.createdAt })
    }

    private fun markSynced(id: String) {
        writeCache(readCache().map { if (it.id == id) it.copy(synced = true) else it })
    }

    /** پاک‌کردن کامل گفت‌وگو از این دستگاه («پاک‌کردن داده‌ی من» در تنظیمات). */
    fun clearLocal() {
        store.remove(KEY_CACHE)
    }

    private fun toJson(message: HeartMessage): JSONObject = JSONObject()
        .put("id", message.id)
        .put("type", message.type.name)
        .put("direction", message.direction.name)
        .put("text", message.text)
        .put("createdAt", message.createdAt)
        .put("mediaPath", message.mediaPath ?: "")
        .put("mediaRef", message.mediaRef ?: "")
        .put("durationMs", message.durationMs ?: 0L)
        .put("synced", message.synced)

    private fun fromJson(o: JSONObject): HeartMessage = HeartMessage(
        id = o.optString("id"),
        type = runCatching { MessageType.valueOf(o.optString("type", "TEXT")) }.getOrDefault(MessageType.TEXT),
        direction = runCatching {
            MessageDirection.valueOf(o.optString("direction", MessageDirection.TO_FATHER.name))
        }.getOrDefault(MessageDirection.TO_FATHER),
        text = o.optString("text"),
        createdAt = o.optLong("createdAt"),
        mediaPath = o.optString("mediaPath").ifBlank { null },
        mediaRef = o.optString("mediaRef").ifBlank { null },
        durationMs = o.optLong("durationMs").takeIf { it > 0 },
        synced = o.optBoolean("synced", false),
    )

    companion object {
        private const val KEY_CACHE = "heart_messages"
    }
}
