package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.sync.SyncEngine
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class FreeStudyChapter(
    val id: String,
    val title: String,
    val mediaKey: String = "",
    val startMs: Long = 0L,
)

data class FreeStudyBook(
    val id: String,
    val title: String,
    val summary: String,
    val kind: String,
    val coverUrl: String,
    val mediaKey: String,
    val htmlKey: String,
    val chapters: List<FreeStudyChapter> = emptyList(),
) {
    val isAudio get() = kind == "free_audio_book"
    val isText get() = kind == "free_text_book"
}

object FreeStudyCatalog {
    private const val STORE = "free_study_catalog"
    private const val CACHE_KEY = "books"

    suspend fun load(context: Context, tables: TablesDbService, force: Boolean = false): List<FreeStudyBook> =
        withContext(Dispatchers.IO) {
            val cached = cached(context)
            if (!force && cached.isNotEmpty()) return@withContext cached
            val remote = runCatching {
                when (val result = tables.list(
                    TableIds.FREE_BOOKS,
                    listOf(
                        Query.orderAsc("sortOrder"),
                        Query.limit(100),
                    ),
                )) {
                    is AppResult.Ok -> result.value.mapNotNull(::fromRow)
                    is AppResult.Err -> emptyList()
                }
            }.getOrDefault(emptyList())
            if (remote.isNotEmpty()) save(context, remote)
            remote.ifEmpty { cached }
        }

    fun cacheKey(bookId: String, contentKey: String): String =
        "free-${contentKey}-${bookId.replace(Regex("[^A-Za-z0-9_-]"), "_")}"

    fun queueProgress(
        sync: SyncEngine,
        userId: String,
        book: FreeStudyBook,
        positionMs: Long,
        durationMs: Long,
        status: String,
    ) {
        val pct = if (durationMs > 0) (positionMs * 100L / durationMs).coerceIn(0, 100) else 0
        sync.enqueue(
            TableIds.LESSON_MEDIA_PROGRESS,
            "free-${userId}-${book.id}",
            mapOf(
                "userId" to userId,
                "mediaKey" to book.id,
                "mediaType" to book.kind,
                "positionMs" to positionMs.coerceAtLeast(0),
                "durationMs" to durationMs.coerceAtLeast(0),
                "progressPct" to pct,
                "status" to status,
                "updatedAtMs" to System.currentTimeMillis(),
            ),
        )
    }

    fun queueRequest(sync: SyncEngine, userId: String, book: FreeStudyBook?, requestText: String) {
        sync.enqueue(
            TableIds.APP_STATE,
            "free-book-request-${userId}-${System.currentTimeMillis()}",
            mapOf(
                "userId" to userId,
                "key" to "free_book_request",
                "payload" to JSONObject()
                    .put("bookId", book?.id.orEmpty())
                    .put("kind", book?.kind.orEmpty())
                    .put("request", requestText.trim().take(2000))
                    .put("createdAtMs", System.currentTimeMillis())
                    .toString(),
                "updatedAt" to System.currentTimeMillis(),
            ),
        )
    }

    private fun cached(context: Context): List<FreeStudyBook> = runCatching {
        val arr = JSONArray(LocalStore(context, STORE).getString(CACHE_KEY, "[]"))
        (0 until arr.length()).mapNotNull { fromJson(arr.optJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun save(context: Context, books: List<FreeStudyBook>) {
        val arr = JSONArray()
        books.forEach { book ->
            arr.put(
                JSONObject()
                    .put("id", book.id)
                    .put("title", book.title)
                    .put("summary", book.summary)
                    .put("kind", book.kind)
                    .put("coverUrl", book.coverUrl)
                    .put("mediaKey", book.mediaKey)
                    .put("htmlKey", book.htmlKey)
                    .put("chapters", JSONArray().apply {
                        book.chapters.forEach { c ->
                            put(JSONObject().put("id", c.id).put("title", c.title).put("mediaKey", c.mediaKey).put("startMs", c.startMs))
                        }
                    }),
            )
        }
        LocalStore(context, STORE).putString(CACHE_KEY, arr.toString())
    }

    private fun fromRow(row: TableRow): FreeStudyBook? {
        val kind = row.string("type").ifBlank { row.string("kind") }
        if (kind != "free_audio_book" && kind != "free_text_book") return null
        return FreeStudyBook(
            id = row.id,
            title = row.string("title").ifBlank { return null },
            summary = row.string("description").ifBlank { row.string("summary") },
            kind = kind,
            coverUrl = row.string("coverKey").ifBlank { row.string("coverUrl") },
            mediaKey = row.string("audioKey").ifBlank { row.string("mediaKey") },
            htmlKey = row.string("htmlKey"),
            chapters = parseChapters(row.string("chaptersJson").ifBlank { row.string("chapters") }),
        )
    }

    private fun fromJson(o: JSONObject?): FreeStudyBook? {
        if (o == null) return null
        return FreeStudyBook(
            id = o.optString("id"),
            title = o.optString("title"),
            summary = o.optString("summary"),
            kind = o.optString("kind"),
            coverUrl = o.optString("coverUrl"),
            mediaKey = o.optString("mediaKey"),
            htmlKey = o.optString("htmlKey"),
            chapters = parseChapters(o.optString("chapters")),
        ).takeIf { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    private fun parseChapters(raw: String): List<FreeStudyChapter> = runCatching {
        val arr = JSONArray(raw.ifBlank { "[]" })
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            FreeStudyChapter(
                id = o.optString("id").ifBlank { "ch-$i" },
                title = o.optString("title").ifBlank { "بخش ${i + 1}" },
                mediaKey = o.optString("mediaKey"),
                startMs = o.optLong("startMs", 0L),
            )
        }
    }.getOrDefault(emptyList())
}
