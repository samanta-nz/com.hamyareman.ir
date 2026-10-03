package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.TableIds
import io.appwrite.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            .replace("إ", "ا").replace("أ", "ا")
            .replace("‌", "")
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
            .filterNot { it.isWhitespace() || it in ".,،؛,:!؟?/_|*~'\"()[]{}<>+=-" }

        return raw.map { c ->
            when (c) {
                '0' -> 'o'
                '1' -> 'i'
                '3' -> 'e'
                '4' -> 'a'
                '5' -> 's'
                '7' -> 't'
                'а' -> 'a'
                'е' -> 'e'
                'о' -> 'o'
                'р' -> 'p'
                'с' -> 'c'
                'х' -> 'x'
                else -> c
            }
        }.joinToString("")
    }

    fun check(input: String): CommentModeration {
        val trimmed = input.trim()
        if (trimmed.length !in 2..1000) return CommentModeration(false, "متن نظر باید بین ۲ تا ۱۰۰۰ نویسه باشد.")
        val n = normalize(trimmed)
        if (n.isBlank()) return CommentModeration(false, "متن نظر خالی است.")
        return if (BLOCKED.any { n.contains(it) })
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
    @Suppress("UNUSED_PARAMETER") private val context: Context,
) {
    companion object {
        private const val PUBLIC_READ = "read(\"any\")"
        private const val USER_UPDATE = "update(\"users\")"
    }

    suspend fun list(bookId: String): List<BookComment> = withContext(Dispatchers.IO) {
        when (val r = tables.list(
            TableIds.BOOK_COMMENTS,
            listOf(Query.equal("bookId", bookId), Query.orderAsc("createdAtMs"), Query.limit(100)),
        )) {
            is AppResult.Ok -> r.value.mapNotNull(::fromRow)
            is AppResult.Err -> emptyList()
        }
    }

    suspend fun create(bookId: String, userId: String, displayName: String, text: String, parentId: String = ""): Result<Unit> =
        withContext(Dispatchers.IO) {
            val moderation = CommentModerationFilter.check(text)
            if (!moderation.allowed) return@withContext Result.failure(IllegalArgumentException(moderation.reason))
            val id = "bc_" + userId.take(16) + "_" + System.currentTimeMillis().toString(36)
            when (val r = tables.create(
                TableIds.BOOK_COMMENTS,
                mapOf(
                    "bookId" to bookId, "userId" to userId, "displayName" to displayName.take(80),
                    "parentId" to parentId, "text" to text.trim().take(1000),
                    "createdAtMs" to System.currentTimeMillis(), "likes" to 0, "dislikes" to 0,
                ),
                listOf(PUBLIC_READ, USER_UPDATE), id,
            )) {
                is AppResult.Ok -> Result.success(Unit)
                is AppResult.Err -> Result.failure(RuntimeException("نظر ثبت نشد."))
            }
        }

    suspend fun react(comment: BookComment, userId: String, like: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val rowId = "bcr_" + comment.id + "_" + userId.take(20)
        val data = mapOf("commentId" to comment.id, "bookId" to comment.bookId, "userId" to userId, "value" to if (like) 1 else -1, "updatedAtMs" to System.currentTimeMillis())
        when (val e = tables.get(TableIds.BOOK_COMMENT_REACTIONS, rowId)) {
            is AppResult.Ok -> if (e.value == null) result(tables.create(TableIds.BOOK_COMMENT_REACTIONS, data, listOf(PUBLIC_READ, USER_UPDATE), rowId))
                else result(tables.update(TableIds.BOOK_COMMENT_REACTIONS, rowId, data))
            is AppResult.Err -> Result.failure(RuntimeException("واکنش ثبت نشد."))
        }
    }

    private fun result(r: AppResult<Unit>): Result<Unit> = when (r) {
        is AppResult.Ok -> Result.success(Unit)
        is AppResult.Err -> Result.failure(RuntimeException("عملیات سرور ناموفق بود."))
    }

    private fun fromRow(row: TableRow) = BookComment(
        id = row.id, bookId = row.string("bookId"), userId = row.string("userId"),
        displayName = row.string("displayName", "کاربر"), parentId = row.string("parentId"),
        text = row.string("text"), createdAtMs = row.long("createdAtMs"),
        likes = row.long("likes").toInt(), dislikes = row.long("dislikes").toInt(),
    ).takeIf { it.bookId.isNotBlank() && it.text.isNotBlank() }
}