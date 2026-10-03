package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun BookCommentsPanel(
    bookId: String,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(bookId) { BookCommentsRepository(container.tables, ctx) }
    val scope = rememberCoroutineScope()
    var comments by remember(bookId) { mutableStateOf(emptyList<BookComment>()) }
    var text by remember(bookId) { mutableStateOf("") }
    var replyTo by remember(bookId) { mutableStateOf<BookComment?>(null) }
    var message by remember(bookId) { mutableStateOf<String?>(null) }
    var loading by remember(bookId) { mutableStateOf(true) }

    LaunchedEffect(bookId) {
        loading = true
        comments = repo.list(bookId)
        loading = false
    }

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row {
            Icon(Icons.Outlined.Forum, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("نظرها و گفت‌وگو", style = MaterialTheme.typography.titleMedium)
        }

        replyTo?.let {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("در پاسخ به " + it.displayName, modifier = Modifier.weight(1f))
                    TextButton(onClick = { replyTo = null }) { Text("انصراف") }
                }
            }
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(1000) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("نظر خودت را بنویس") },
            minLines = 3,
            supportingText = { Text("۲ تا ۱۰۰۰ نویسه؛ بدون توهین و محتوای آزاردهنده") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                enabled = text.trim().length >= 2,
                onClick = {
                    val uid = container.auth.cachedUserId().orEmpty()
                    if (uid.isBlank()) {
                        message = "برای ثبت نظر باید وارد حساب شوی."
                    } else {
                        scope.launch {
                            val result = repo.create(bookId, uid, "کاربر", text, replyTo?.id.orEmpty())
                            if (result.isSuccess) {
                                text = ""
                                replyTo = null
                                comments = repo.list(bookId)
                                message = "نظر ثبت شد."
                            } else {
                                message = result.exceptionOrNull()?.message ?: "ثبت نظر ممکن نشد."
                            }
                        }
                    }
                },
            ) { Text("ثبت نظر") }
        }

        message?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        comments.filter { !it.isReply }.forEach { comment ->
            CommentCard(
                comment = comment,
                onReply = { replyTo = comment },
                onReact = { like ->
                    val uid = container.auth.cachedUserId().orEmpty()
                    if (uid.isNotBlank()) {
                        scope.launch { repo.react(comment, uid, like) }
                    }
                },
            )
            comments.filter { it.parentId == comment.id }.forEach { reply ->
                CommentCard(
                    comment = reply,
                    onReply = { replyTo = reply },
                    onReact = { like ->
                        val uid = container.auth.cachedUserId().orEmpty()
                        if (uid.isNotBlank()) scope.launch { repo.react(reply, uid, like) }
                    },
                    nested = true,
                )
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: BookComment,
    onReply: () -> Unit,
    onReact: (Boolean) -> Unit,
    nested: Boolean = false,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(start = if (nested) 18.dp else 0.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(comment.displayName, style = MaterialTheme.typography.labelLarge)
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { onReact(true) }) { Icon(Icons.Outlined.ThumbUp, contentDescription = "پسندیدن") }
                Text(comment.likes.toString(), style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = { onReact(false) }) { Icon(Icons.Outlined.ThumbDown, contentDescription = "نپسندیدن") }
                Text(comment.dislikes.toString(), style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onReply) { Text("پاسخ") }
            }
        }
    }
}
