package com.hamyareman.ir.platform.feature.hearttoheart

import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import com.hamyareman.ir.platform.core.common.AppResult
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
import com.hamyareman.ir.platform.core.appwrite.AppwriteRealtimeFeed
import com.hamyareman.ir.platform.core.appwrite.RealtimeRowEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.hamyareman.ir.platform.core.appwrite.ServerActions

/** چه کسی این خاطره را اضافه کرده. */
enum class AlbumAuthor(val label: String) { ZAHRA("زهرا"), FATHER("بابا") }

/**
 * یک خاطره در آلبوم مشترک.
 *
 * `approved` فقط برای خاطره‌هایی معنی دارد که پدر اضافه کرده: تا زهرا تأیید نکند،
 * در اپ زهرا «در انتظار تأیید» دیده می‌شود و در «آلبوم تأییدشده» نمی‌آید.
 * خاطره‌ی خود زهرا همیشه approved است چون خودش انتخاب کرده اضافه‌اش کند.
 */
data class AlbumItem(
    val id: String,
    val title: String,
    val note: String,
    val createdAt: Long,
    val addedBy: AlbumAuthor,
    val addedById: String,
    val mediaPath: String?,
    val mediaRef: String?,
    val approved: Boolean,
    val synced: Boolean,
)

/**
 * آلبوم خاطرات مشترک زهرا و پدر.
 *
 * طراحی (مثل «حرف دل»):
 *  1) **محلی‌اول** — خاطره فوری در کش دستگاه می‌نشیند؛ اگر اینترنت نبود `synced=false`
 *     می‌ماند و بعداً با [pushPending] می‌رود.
 *  2) **دسترسی سطر** — خواندن برای هر دو طرف پیوند؛ به‌روزرسانی برای زهرا (تأیید)
 *     و سازنده‌ی خاطره؛ حذف فقط برای سازنده و زهرا (مالک آلبوم).
 *  3) **رسانه** در باکت `father-album` با همان دسترسی‌ها.
 *  4) مالک آلبوم همیشه زهراست (`ownerId`)، حتی وقتی پدر خاطره اضافه می‌کند.
 */
class AlbumRepository(
    private val store: LocalStore,
    private val tables: TablesDbService,
    private val storage: StorageService,
    private val author: AlbumAuthor,
    private val selfId: () -> String,
    private val zahraId: () -> String,
    private val fatherId: () -> String?,
    private val cacheKey: String = "album_cache",
    private val realtime: AppwriteRealtimeFeed? = null,
    private val serverActions: ServerActions? = null,
) {

    val isConfigured: Boolean get() = tables.isConfigured
    val myAuthor: AlbumAuthor get() = author

    /**
     * رویدادهای زنده‌ی آلبوم: وقتی پدر خاطره‌ای اضافه می‌کند یا زهرا تأیید می‌کند،
     * صفحه‌ی طرف مقابل بدون refresh دستی به‌روز می‌شود. بدون Realtime ⇒ `emptyFlow`.
     */
    fun liveUpdates(): Flow<RealtimeRowEvent> = realtime?.albumItems() ?: emptyFlow()

    fun items(): List<AlbumItem> = readCache().sortedByDescending { it.createdAt }

    /** فقط خاطره‌های تأییدشده — همان چیزی که در «آلبوم» نشان داده می‌شود. */
    fun approvedItems(): List<AlbumItem> = items().filter { it.approved }

    fun pendingApproval(): List<AlbumItem> = items().filterNot { it.approved }

    fun pendingCount(): Int = readCache().count { !it.synced }

    /** کپی فایل انتخاب‌شده در حافظه‌ی دائمی اپ تا آفلاین هم دیده شود. */
    fun importMedia(context: android.content.Context, source: File): File? = runCatching {
        val dir = File(context.filesDir, "media/album").apply { mkdirs() }
        val extension = source.extension.ifBlank { "jpg" }
        val target = File(dir, "album_${System.currentTimeMillis()}.$extension")
        source.copyTo(target, overwrite = true)
        target.takeIf { it.exists() && it.length() > 0 }
    }.getOrNull()

    suspend fun add(title: String, note: String, mediaFile: File?): AppResult<AlbumItem> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return AppResult.Err(AppError.Validation("عنوان خاطره خالی است."))
        if (mediaFile != null && !MediaFiles.isWithinLimit(mediaFile)) {
            return AppResult.Err(AppError.Validation("حجم فایل بیشتر از ۱۰ مگابایت است."))
        }
        val me = selfId()
        val item = AlbumItem(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            note = note.trim(),
            createdAt = System.currentTimeMillis(),
            addedBy = author,
            addedById = me,
            mediaPath = mediaFile?.absolutePath,
            mediaRef = null,
            // خاطره‌ی خود زهرا تأییدشده است؛ خاطره‌ی پدر منتظر تأیید زهرا می‌ماند.
            approved = author == AlbumAuthor.ZAHRA,
            synced = false,
        )

        val permissions = rowPermissions(item.addedById)
        var prepared = item
        if (mediaFile != null && isConfigured) {
            when (val upload = storage.upload(BucketIds.FATHER_ALBUM, mediaFile.absolutePath, permissions)) {
                is AppResult.Ok -> prepared = item.copy(mediaRef = upload.value.reference)
                is AppResult.Err -> Unit // بدون عکس هم خاطره ثبت می‌شود؛ بعداً دوباره تلاش می‌شود
            }
        }
        appendCache(prepared)
        if (!isConfigured) return AppResult.Ok(prepared)

        return when (tables.upsert(TableIds.ALBUM_ITEMS, prepared.id, payloadOf(prepared), permissions)) {
            is AppResult.Ok -> {
                markSynced(prepared.id)
                AppResult.Ok(prepared.copy(synced = true))
            }

            is AppResult.Err -> AppResult.Ok(prepared)
        }
    }

    /**
     * تأیید/رد خاطره — فقط از طرف زهرا معنی دارد.
     *
     * دو لایه‌ی بررسی:
     *  1) `album-consent` سمت سرور: مالکیت (`ownerId`) و «خاطره‌ی پدر بودن» را خودش
     *     چک می‌کند، پس حتی یک کلاینت دستکاری‌شده نمی‌تواند خاطره‌ی تأییدنشده را
     *     تأییدشده جا بزند.
     *  2) اگر تابع deploy نشده بود یا خطا داد، روی `update` مستقیم سطر می‌افتیم —
     *     که آن هم با Permission سطر محافظت می‌شود. یعنی هیچ‌وقت بن‌بست نمی‌شود.
     */
    suspend fun setApproved(item: AlbumItem, approved: Boolean): AppResult<AlbumItem> {
        val updated = item.copy(approved = approved)
        replaceInCache(updated)
        if (!isConfigured || !item.synced) return AppResult.Ok(updated)

        val consent = serverActions?.albumConsent(item.id, approved)
        if (consent is AppResult.Ok) return AppResult.Ok(updated.copy(synced = true))

        val result = tables.update(TableIds.ALBUM_ITEMS, item.id, mapOf("approved" to approved))
        return when (result) {
            is AppResult.Ok -> AppResult.Ok(updated.copy(synced = true))
            is AppResult.Err -> AppResult.Err(result.error)
        }
    }

    suspend fun delete(item: AlbumItem): AppResult<Unit> {
        removeFromCache(item.id)
        runCatching { item.mediaPath?.let { File(it).delete() } }
        if (!isConfigured) return AppResult.Ok(Unit)
        val stored = item.mediaRef?.let { StoredFile.parse(it) }
        if (stored != null) storage.delete(stored.bucketId, stored.fileId)
        return tables.delete(TableIds.ALBUM_ITEMS, item.id)
    }

    suspend fun pushPending(): AppResult<Int> {
        if (!isConfigured) return AppResult.Ok(0)
        var pushed = 0
        readCache().filterNot { it.synced }.forEach { item ->
            var toSend = item
            if (toSend.mediaRef == null && toSend.mediaPath != null) {
                val permissions = rowPermissions(item.addedById)
                when (val upload = storage.upload(BucketIds.FATHER_ALBUM, item.mediaPath, permissions)) {
                    is AppResult.Ok -> toSend = item.copy(mediaRef = upload.value.reference)
                    is AppResult.Err -> return@forEach
                }
            }
            val permissions = rowPermissions(toSend.addedById)
            if (tables.upsert(TableIds.ALBUM_ITEMS, toSend.id, payloadOf(toSend), permissions) is AppResult.Ok) {
                replaceInCache(toSend.copy(synced = true))
                pushed++
            }
        }
        return AppResult.Ok(pushed)
    }

    suspend fun refresh(): AppResult<List<AlbumItem>> {
        if (!isConfigured) return AppResult.Ok(items())
        val queries = listOf(Query.orderDesc("createdAtMs"), Query.limit(200))
        return when (val result = tables.list(TableIds.ALBUM_ITEMS, queries)) {
            is AppResult.Ok -> {
                val merged = merge(result.value.map { it.toAlbumItem() }, readCache())
                writeCache(merged)
                AppResult.Ok(merged.sortedByDescending { it.createdAt })
            }

            is AppResult.Err -> AppResult.Ok(items())
        }
    }

    /** لینک نمایش عکسِ سمت مقابل (یا مسیر محلی خودمان). */
    suspend fun mediaUrl(item: AlbumItem): String? {
        val ref = item.mediaRef ?: return item.mediaPath
        val stored = StoredFile.parse(ref) ?: return item.mediaPath
        return storage.viewUrl(stored.bucketId, stored.fileId) ?: item.mediaPath
    }

    // --- دسترسی‌ها و ذخیره‌سازی --------------------------------------------

    private fun rowPermissions(addedById: String): List<String> {
        val zahra = zahraId()
        val father = fatherId()
        val permissions = mutableListOf<String>()
        if (zahra.isNotBlank()) {
            // زهرا مالک آلبوم است: می‌خواند، تأیید می‌کند و می‌تواند حذف کند.
            permissions += Permission.read(Role.user(zahra))
            permissions += Permission.update(Role.user(zahra))
            permissions += Permission.delete(Role.user(zahra))
        }
        if (!father.isNullOrBlank()) {
            permissions += Permission.read(Role.user(father))
            if (father == addedById) {
                permissions += Permission.update(Role.user(father))
                permissions += Permission.delete(Role.user(father))
            }
        }
        return permissions
    }

    private fun payloadOf(item: AlbumItem): Map<String, Any?> = mapOf(
        "itemId" to item.id,
        "title" to item.title,
        "note" to item.note,
        "addedBy" to item.addedBy.name,
        "addedById" to item.addedById,
        "ownerId" to zahraId(),
        "mediaRef" to (item.mediaRef ?: ""),
        "approved" to item.approved,
        "createdAtMs" to item.createdAt,
    )

    private fun TableRow.toAlbumItem(): AlbumItem = AlbumItem(
        id = string("itemId").ifBlank { id },
        title = string("title"),
        note = string("note"),
        createdAt = long("createdAtMs").takeIf { it > 0 } ?: System.currentTimeMillis(),
        addedBy = runCatching { AlbumAuthor.valueOf(string("addedBy", AlbumAuthor.ZAHRA.name)) }
            .getOrDefault(AlbumAuthor.ZAHRA),
        addedById = string("addedById"),
        mediaPath = null,
        mediaRef = string("mediaRef").ifBlank { null },
        approved = data["approved"] as? Boolean ?: false,
        synced = true,
    )

    private fun merge(remote: List<AlbumItem>, local: List<AlbumItem>): List<AlbumItem> {
        val byId = LinkedHashMap<String, AlbumItem>()
        local.forEach { byId[it.id] = it }
        remote.forEach { item ->
            val existing = byId[item.id]
            byId[item.id] = if (existing == null) {
                item
            } else {
                // سرور معتبر است، ولی مسیر محلی عکس را نگه می‌داریم تا آفلاین دیده شود.
                item.copy(mediaPath = existing.mediaPath ?: item.mediaPath, synced = true)
            }
        }
        return byId.values.toList()
    }

    private fun readCache(): List<AlbumItem> = runCatching {
        val array = JSONArray(store.getString(cacheKey, "[]"))
        buildList { for (i in 0 until array.length()) fromJson(array.getJSONObject(i))?.let { add(it) } }
    }.getOrDefault(emptyList())

    private fun writeCache(list: List<AlbumItem>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        store.putString(cacheKey, array.toString())
    }

    private fun appendCache(item: AlbumItem) = writeCache(readCache() + item)

    private fun replaceInCache(item: AlbumItem) =
        writeCache(readCache().map { if (it.id == item.id) item else it })

    private fun removeFromCache(id: String) = writeCache(readCache().filterNot { it.id == id })

    private fun markSynced(id: String) =
        writeCache(readCache().map { if (it.id == id) it.copy(synced = true) else it })

    private fun toJson(item: AlbumItem): JSONObject = JSONObject()
        .put("id", item.id).put("title", item.title).put("note", item.note)
        .put("createdAt", item.createdAt).put("addedBy", item.addedBy.name)
        .put("addedById", item.addedById).put("mediaPath", item.mediaPath ?: "")
        .put("mediaRef", item.mediaRef ?: "").put("approved", item.approved)
        .put("synced", item.synced)

    private fun fromJson(o: JSONObject): AlbumItem? = runCatching {
        AlbumItem(
            id = o.getString("id"),
            title = o.optString("title"),
            note = o.optString("note"),
            createdAt = o.optLong("createdAt"),
            addedBy = runCatching { AlbumAuthor.valueOf(o.optString("addedBy")) }.getOrDefault(AlbumAuthor.ZAHRA),
            addedById = o.optString("addedById"),
            mediaPath = o.optString("mediaPath").ifBlank { null },
            mediaRef = o.optString("mediaRef").ifBlank { null },
            approved = o.optBoolean("approved"),
            synced = o.optBoolean("synced"),
        )
    }.getOrNull()
}
