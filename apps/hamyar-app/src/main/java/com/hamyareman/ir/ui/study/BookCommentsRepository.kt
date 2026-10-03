package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.AppResult
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.Normalizer
import java.util.Locale

data class BookComment(
    val id: String,
    val bookId: String,
    val userId: String,
    val displayName: String,
    val parentId: String,
    val text: String,
    val createdAtMs: Long,
    val likes: Int,
    val dislikes: Int,
) { val isReply get() = parentId.isNotBlank() }

data class CommentModeration(val allowed: Boolean, val reason: String = "")

object CommentModerationFilter {
    fun normalize(input: String): String {
        val raw = Normalizer.normalize(input, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace("ي", "ی").replace("ى", "ی").replace("ك", "ک")
            .replace("ۀ", "ه").replace("ة", "ه").replace("ؤ", "و")
            .replace("إ", "ا").replace("أ", "ا").replace("‌", "")
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
            .filterNot { it.isWhitespace() || it in ".,،؛,:!؟?/_|*~'\"()[]{}<>+=-" }
        return raw.map { c ->
            when (c) {
                '0' -> 'o'; '1' -> 'i'; '3' -> 'e'; '4' -> 'a'; '5' -> 's'; '7' -> 't'
                'а' -> 'a'; 'е' -> 'e'; 'о' -> 'o'; 'р' -> 'p'; 'с' -> 'c'; 'х' -> 'x'
                else -> c
            }
        }.joinToString("")
    }

    fun check(input: String, extraBlocked: Set<String> = emptySet()): CommentModeration {
        val trimmed = input.trim()
        if (trimmed.length !in 2..1000) return CommentModeration(false, "متن نظر باید بین ۲ تا ۱۰۰۰ نویسه باشد.")
        val normalized = normalize(trimmed)
        if (normalized.isBlank()) return CommentModeration(false, "متن نظر خالی است.")
        return if ((BLOCKED + extraBlocked).any { it.isNotBlank() && normalized.contains(it) })
            CommentModeration(false, "این متن به دلیل واژه یا عبارت نامناسب قابل ارسال نیست.")
        else CommentModeration(true)
    }

    private val BLOCKED = setOf(
        "احمق", "نفهم", "بی شعور", "کثافت", "عوضی", "حرومزاده", "حرامزاده",
        "بی ناموس", "آشغال", "دیوث", "کونی", "لاشی", "فاحشه", "گمشو", "خفه",
        "fuck", "shit", "bitch", "asshole", "idiot", "stupid", "moron", "motherfucker",
    ).map(::normalize).toSet()
}

class BookCommentsRepository(
    private val tables: TablesDbService,
    private val context: Context,
) {
    companion object {
        private const val PUBLIC_READ = "read(\"any\")"
        private const val USER_CREATE = "create(\"users\")"
        private const val USER_UPDATE = "update(\"users\")"
    }

    suspend fun list(bookId: String): List<BookComment> = withContext(Dispatchers.IO) {
        when (val result = tables.list(
            TableIds.BOOK_COMMENTS,
            listOf(
                Query.equal("bookId", bookId),
                Query.orderAsc("createdAtMs"),
                Query.limit(100),
            ),
        )) {
            is AppResult.Ok -> result.value.mapNotNull(::fromRow)
            is AppResult.Err -> emptyList()
        }
    }

    suspend fun create(bookId: String, userId: String, username: String, text: String, parentId: String = ""): Result<Unit> =
        withContext(Dispatchers.IO) {
            val moderation = CommentModerationFilter.check(text, loadModerationTerms())
            if (!moderation.allowed) return@withContext Result.failure(IllegalArgumentException(moderation.reason))
            if (parentId.isNotBlank()) {
                when (val parent = tables.get(TableIds.BOOK_COMMENTS, parentId)) {
                    is AppResult.Ok -> if (parent.value == null) return@withContext Result.failure(IllegalArgumentException("این دیدگاه دیگر وجود ندارد."))
                    is AppResult.Err -> return@withContext Result.failure(RuntimeException("دیدگاه والد پیدا نشد."))
                }
            }
            val id = "bc_" + userId.take(18) + "_" + System.currentTimeMillis().toString(36)
            when (val result = tables.create(
                TableIds.BOOK_COMMENTS,
                mapOf(
                    "commentId" to id,
                    "bookId" to bookId,
                    "userId" to userId,
                    "displayName" to username.trim().take(32).ifBlank { "کاربر" },
                    "body" to text.trim().take(1000),
                    "parentId" to parentId,
                    "createdAtMs" to System.currentTimeMillis(),
                    "status" to "APPROVED",
                    "likes" to 0,
                    "dislikes" to 0,
                ),
                listOf(PUBLIC_READ, USER_CREATE, USER_UPDATE),
                id,
            )) {
                is AppResult.Ok -> Result.success(Unit)
                is AppResult.Err -> Result.failure(RuntimeException("نظر ثبت نشد."))
            }
        }

    suspend fun react(comment: BookComment, userId: String, like: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(IllegalArgumentException("کاربر وارد نشده است."))
        val rowId = "bcr_" + comment.id + "_" + userId.take(24)
        val reaction = if (like) "like" else "dislike"
        val existing = when (val result = tables.get(TableIds.BOOK_COMMENT_REACTIONS, rowId)) {
            is AppResult.Ok -> result.value
            is AppResult.Err -> return@withContext Result.failure(RuntimeException("واکنش ثبت نشد."))
        }
        val previous = existing?.string("reaction").orEmpty()
        if (previous == reaction) return@withContext Result.success(Unit)

        val saved = if (existing == null) {
            tables.create(
                TableIds.BOOK_COMMENT_REACTIONS,
                mapOf(
                    "userId" to userId,
                    "commentId" to comment.id,
                    "reaction" to reaction,
                    "updatedAtMs" to System.currentTimeMillis(),
                ),
                listOf(PUBLIC_READ, USER_CREATE, USER_UPDATE),
                rowId,
            )
        } else {
            tables.update(
                TableIds.BOOK_COMMENT_REACTIONS,
                rowId,
                mapOf("reaction" to reaction, "updatedAtMs" to System.currentTimeMillis()),
            )
        }
        if (saved is AppResult.Err) return@withContext Result.failure(RuntimeException("واکنش ثبت نشد."))

        val likeDelta = (if (like) 1 else 0) - (if (previous == "like") 1 else 0)
        val dislikeDelta = (if (!like) 1 else 0) - (if (previous == "dislike") 1 else 0)
        when (val updated = tables.update(
            TableIds.BOOK_COMMENTS,
            comment.id,
            mapOf(
                "likes" to (comment.likes + likeDelta).coerceAtLeast(0),
                "dislikes" to (comment.dislikes + dislikeDelta).coerceAtLeast(0),
            ),
        )) {
            is AppResult.Ok -> Result.success(Unit)
            is AppResult.Err -> Result.failure(RuntimeException("شمارش واکنش به‌روزرسانی نشد."))
        }
    }

    private suspend fun loadModerationTerms(): Set<String> {
        val store = LocalStore(context, "hamyar_comment_moderation")
        val cached = runCatching {
            val arr = JSONArray(store.getString("terms", "[]"))
            buildSet { for (i in 0 until arr.length()) add(arr.optString(i)) }
        }.getOrDefault(emptySet())
        val cachedAt = store.getLong("terms_at", 0L)
        if (cached.isNotEmpty() && System.currentTimeMillis() - cachedAt < 6 * 60 * 60 * 1000L) return cached
        return runCatching {
            when (val result = tables.list(
                TableIds.MODERATION_TERMS,
                listOf(Query.equal("active", 1), Query.limit(500)),
            )) {
                is AppResult.Ok -> result.value.mapNotNull {
                    it.string("term").takeIf(String::isNotBlank)?.let(CommentModerationFilter::normalize)
                }.toSet()
                is AppResult.Err -> emptySet()
            }.also { terms ->
                if (terms.isNotEmpty()) {
                    val arr = JSONArray()
                    terms.forEach(arr::put)
                    store.putString("terms", arr.toString())
                    store.putLong("terms_at", System.currentTimeMillis())
                }
            }
        }.getOrElse { cached }
    }

    private fun fromRow(row: TableRow): BookComment =
        BookComment(
            id = row.id.ifBlank { row.string("commentId") },
            bookId = row.string("bookId"),
            userId = row.string("userId"),
            displayName = row.string("displayName", "کاربر").trim().take(32),
            parentId = row.string("parentId"),
            text = row.string("body").ifBlank { row.string("text") },
            createdAtMs = row.long("createdAtMs"),
            likes = row.long("likes").toInt().coerceAtLeast(0),
            dislikes = row.long("dislikes").toInt().coerceAtLeast(0),
        ).takeIf { it.bookId.isNotBlank() && it.text.isNotBlank() }
}