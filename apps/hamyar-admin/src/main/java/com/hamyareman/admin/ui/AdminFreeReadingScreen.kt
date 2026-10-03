package com.hamyareman.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.common.AppResult
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun AdminFreeReadingScreen() {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("requests") }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var addOpen by remember { mutableStateOf(false) }

    fun load(target: String = tab) {
        loading = true
        error = null
        scope.launch {
            val table = when (target) {
                "books" -> "free_books"
                "comments" -> "book_comments"
                else -> "free_book_requests"
            }
            when (val r = adminIo { api.listRows(table) }) {
                is AppResult.Ok -> rows = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }

    LaunchedEffect(tab) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("مطالعهٔ آزاد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == "requests", onClick = { tab = "requests" }, label = { Text("درخواست‌ها") })
            FilterChip(selected = tab == "books", onClick = { tab = "books" }, label = { Text("کتاب‌ها") })
            FilterChip(selected = tab == "comments", onClick = { tab = "comments" }, label = { Text("نظرات") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { load() }, enabled = !loading) { Text(if (loading) "…" else "تازه‌سازی") }
            if (tab == "books") {
                Button(onClick = { addOpen = true }, enabled = !loading) { Text("افزودن کتاب") }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows, key = { it.optString("\$id").ifBlank { it.optString("id") } }) { row ->
                when (tab) {
                    "requests" -> RequestRow(row, api, scope) { load() }
                    "books" -> BookRow(row)
                    else -> CommentRow(row, api, scope) { load() }
                }
            }
        }
    }

    if (addOpen) {
        AddFreeBookDialog(
            onDismiss = { addOpen = false },
            onSaved = { addOpen = false; load("books") },
        )
    }
}

@Composable
private fun RequestRow(
    row: JSONObject,
    api: com.hamyareman.admin.AdminApi,
    scope: kotlinx.coroutines.CoroutineScope,
    reload: () -> Unit,
) {
    val id = row.optString("\$id").ifBlank { row.optString("id") }
    val title = row.optString("title").ifBlank { "بدون عنوان" }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("نوع: " + row.optString("type"))
            Text("نویسنده: " + row.optString("author").ifBlank { "—" })
            Text(row.optString("note").ifBlank { "بدون توضیح" }, style = MaterialTheme.typography.bodySmall)
            Text("وضعیت: " + row.optString("status"))
            TextButton(
                onClick = {
                    scope.launch {
                        adminIo {
                            api.saveRow(
                                "free_book_requests",
                                id,
                                JSONObject()
                                    .put("status", "ANSWERED")
                                    .put("answeredAtMs", System.currentTimeMillis()),
                                create = false,
                            )
                        }
                        reload()
                    }
                },
            ) { Text("علامت‌گذاری به‌عنوان بررسی‌شده") }
        }
    }
}

@Composable
private fun BookRow(row: JSONObject) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.optString("title").ifBlank { "بدون عنوان" }, fontWeight = FontWeight.Bold)
            Text("شناسه: " + row.optString("bookId").ifBlank { row.optString("\$id") }, style = MaterialTheme.typography.labelSmall)
            Text("نوع: " + row.optString("type") + " · منتشر: " + row.optInt("published", 0))
            Text(row.optString("description").take(240), style = MaterialTheme.typography.bodySmall)
            if (row.optString("coverKey").isNotBlank()) Text("کاور: " + row.optString("coverKey"), style = MaterialTheme.typography.labelSmall)
            if (row.optString("htmlKey").isNotBlank()) Text("HTML: " + row.optString("htmlKey"), style = MaterialTheme.typography.labelSmall)
            if (row.optString("audioKey").isNotBlank()) Text("Audio: " + row.optString("audioKey"), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CommentRow(
    row: JSONObject,
    api: com.hamyareman.admin.AdminApi,
    scope: kotlinx.coroutines.CoroutineScope,
    reload: () -> Unit,
) {
    val id = row.optString("\$id").ifBlank { row.optString("id") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(row.optString("displayName").ifBlank { "کاربر" }, fontWeight = FontWeight.Bold)
            Text(row.optString("text").ifBlank { row.optString("body") })
            Text("کتاب: " + row.optString("bookId") + " · وضعیت: " + row.optString("status"))
            if (row.optString("status") != "APPROVED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            adminIo { api.saveRow("book_comments", id, JSONObject().put("status", "APPROVED"), create = false) }
                            reload()
                        }
                    }) { Text("تأیید") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            adminIo { api.saveRow("book_comments", id, JSONObject().put("status", "REJECTED"), create = false) }
                            reload()
                        }
                    }) { Text("رد") }
                }
            }
        }
    }
}

@Composable
private fun AddFreeBookDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var bookId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("TEXT") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coverKey by remember { mutableStateOf("") }
    var htmlKey by remember { mutableStateOf("") }
    var audioKey by remember { mutableStateOf("") }
    var chaptersJson by remember { mutableStateOf("[]") }
    var sortOrder by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن کتاب مطالعه آزاد") },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeId = bookId.trim()
                    if (safeId.isBlank() || title.trim().isBlank()) {
                        error = "شناسه و عنوان لازم است."
                        return@TextButton
                    }
                    runCatching { JSONArray(chaptersJson) }.onFailure {
                        error = "chaptersJson باید JSON معتبر باشد."
                        return@TextButton
                    }
                    scope.launch {
                        when (
                            val r = adminIo {
                                api.saveRow(
                                    "free_books",
                                    safeId,
                                    JSONObject()
                                        .put("bookId", safeId)
                                        .put("type", if (type == "AUDIO") "free_audio_book" else "free_text_book")
                                        .put("title", title.trim())
                                        .put("author", author.trim())
                                        .put("description", description.trim())
                                        .put("coverKey", coverKey.trim())
                                        .put("htmlKey", htmlKey.trim())
                                        .put("audioKey", audioKey.trim())
                                        .put("chaptersJson", chaptersJson.trim())
                                        .put("published", 1)
                                        .put("sortOrder", sortOrder.toIntOrNull() ?: 0)
                                        .put("updatedAtMs", System.currentTimeMillis()),
                                    create = true,
                                )
                            }
                        ) {
                            is AppResult.Ok -> onSaved()
                            is AppResult.Err -> error = r.error.userMessage
                        }
                    }
                },
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } },
        text = {
            Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "TEXT", onClick = { type = "TEXT" }, label = { Text("متنی") })
                    FilterChip(selected = type == "AUDIO", onClick = { type = "AUDIO" }, label = { Text("صوتی") })
                }
                OutlinedTextField(bookId, { bookId = it.take(128) }, label = { Text("bookId") })
                OutlinedTextField(title, { title = it.take(256) }, label = { Text("عنوان") })
                OutlinedTextField(author, { author = it.take(256) }, label = { Text("نویسنده") })
                OutlinedTextField(description, { description = it.take(4000) }, label = { Text("توضیح کوتاه") })
                OutlinedTextField(coverKey, { coverKey = it.take(512) }, label = { Text("مسیر کاور ParsPack") })
                if (type == "TEXT") {
                    OutlinedTextField(htmlKey, { htmlKey = it.take(512) }, label = { Text("مسیر HTML ParsPack") })
                } else {
                    OutlinedTextField(audioKey, { audioKey = it.take(512) }, label = { Text("مسیر صوت ParsPack") })
                    OutlinedTextField(chaptersJson, { chaptersJson = it.take(100000) }, label = { Text("فصل‌ها JSON") }, minLines = 4)
                }
                OutlinedTextField(sortOrder, { sortOrder = it.filter(Char::isDigit).take(6) }, label = { Text("ترتیب") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
    )
}
