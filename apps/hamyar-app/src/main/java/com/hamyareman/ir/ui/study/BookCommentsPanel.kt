package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.toPersianDigits
import kotlinx.coroutines.launch

@Composable
fun BookCommentsPanel(bookId: String, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(bookId) { BookCommentsRepository(container.tables, context) }
    val scope = rememberCoroutineScope()
    var comments by remember(bookId) { mutableStateOf(emptyList<BookComment>()) }
    var draft by remember(bookId) { mutableStateOf("") }
    var replyTo by remember(bookId) { mutableStateOf<BookComment?>(null) }
    var notice by remember(bookId) { mutableStateOf<String?>(null) }
    var loading by remember(bookId) { mutableStateOf(true) }

    val username = remember {
        container.auth.cachedUsername().orEmpty()
            .ifBlank { container.auth.cachedUser()?.username.orEmpty() }
            .ifBlank { "کاربر" }
    }

    suspend fun reload() {
        loading = true
        comments = repository.list(bookId)
        loading = false
    }

    LaunchedEffect(bookId) { reload() }

    val children = remember(comments) { comments.groupBy { it.parentId } }

    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Forum, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("دیدگاه‌ها و گفتگو", style = MaterialTheme.typography.titleLarge)
        }

        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            comments.isEmpty() -> Card(Modifier.fillMaxWidth()) {
                Text("هنوز دیدگاهی ثبت نشده است.", Modifier.padding(16.dp))
            }
            else -> comments.filter { it.parentId.isBlank() }.forEach { root ->
                CommentThread(root, children, onReply = { replyTo = it }) { target ->
                    scope.launch {
                        if (repository.react(target, username, like = true).isSuccess) reload()
                    }
                }
            }
        }

        replyTo?.let { target ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("پاسخ به @" + target.displayName, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { replyTo = null }) { Text("لغو") }
                }
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(1000) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text(if (replyTo == null) "دیدگاه خودت را بنویس" else "پاسخت را بنویس") },
        )
        TextButton(
            enabled = draft.trim().length >= 2,
            onClick = {
                val userId = container.auth.cachedUserId().orEmpty()
                if (userId.isBlank()) {
                    notice = "برای ثبت دیدگاه باید وارد حساب شوی."
                    return@TextButton
                }
                scope.launch {
                    val result = repository.create(bookId, userId, username, draft, replyTo?.id.orEmpty())
                    if (result.isSuccess) {
                        draft = ""
                        replyTo = null
                        notice = "ثبت شد."
                        reload()
                    } else {
                        notice = result.exceptionOrNull()?.message ?: "ثبت دیدگاه ممکن نشد."
                    }
                }
            },
        ) { Text("ثبت دیدگاه") }

        notice?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CommentThread(
    root: BookComment,
    children: Map<String, List<BookComment>>,
    onReply: (BookComment) -> Unit,
    onLike: (BookComment) -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            CommentBubble(root, onReply, onLike)
            renderReplies(root.id, children, onReply, onLike, depth = 1)
        }
    }
}

@Composable
private fun renderReplies(
    parentId: String,
    children: Map<String, List<BookComment>>,
    onReply: (BookComment) -> Unit,
    onLike: (BookComment) -> Unit,
    depth: Int,
) {
    if (depth > 6) return
    children[parentId].orEmpty().forEach { reply ->
        CommentBubble(reply, onReply, onLike, nested = true, depth = depth)
        renderReplies(reply.id, children, onReply, onLike, depth + 1)
    }
}

@Composable
private fun CommentBubble(
    comment: BookComment,
    onReply: (BookComment) -> Unit,
    onLike: (BookComment) -> Unit,
    nested: Boolean = false,
    depth: Int = 0,
) {
    Column(
        Modifier.fillMaxWidth().padding(start = if (nested) (20 + depth * 4).dp else 0.dp, top = 3.dp, bottom = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("@" + comment.displayName, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text(relativeCommentTime(comment.createdAtMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onLike(comment) }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "پسندیدن")
            }
            Text(toPersianDigits(comment.likes.toString()), style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = { onReply(comment) }) {
                Icon(Icons.Outlined.Reply, contentDescription = "پاسخ")
            }
            Text("پاسخ", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun relativeCommentTime(createdAtMs: Long): String {
    val age = (System.currentTimeMillis() - createdAtMs).coerceAtLeast(0L)
    val minutes = age / 60_000L
    return when {
        minutes < 1 -> "اکنون"
        minutes < 60 -> toPersianDigits(minutes.toString()) + " دقیقه پیش"
        minutes < 1440 -> toPersianDigits((minutes / 60).toString()) + " ساعت پیش"
        else -> toPersianDigits((minutes / 1440).toString()) + " روز پیش"
    }
}
