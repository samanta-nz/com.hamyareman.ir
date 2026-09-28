package com.hamyareman.ir.ui.study

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.LocalAppContainer
import org.json.JSONArray
import org.json.JSONObject

/**
 * کتابخانه‌ی شخصی زهرا.
 *
 * عمدی است که «فهرست کتاب پیشنهادی» تحمیل نمی‌کنیم: ژانرها را خودش انتخاب می‌کند و
 * کتاب‌های خودش را اضافه می‌کند. همه‌چیز فقط روی دستگاه می‌ماند (privately local)
 * و هیچ‌وقت Sync نمی‌شود.
 */
internal data class Book(
    val id: String,
    val title: String,
    val author: String,
    val status: BookStatus,
    val addedIso: String,
)

internal enum class BookStatus(val label: String, val emoji: String) {
    WANT("می‌خوام بخونم", "📖"),
    READING("دارم می‌خونم", "👓"),
    DONE("تمام شد", "✅"),
    ;

    fun next(): BookStatus = entries[(ordinal + 1) % entries.size]
}

private const val BOOKS_KEY = "library_books"
private const val GENRES_KEY = "library_genres"

private val GENRES = listOf("داستانی", "رشد فردی", "مهارت زندگی", "علمی", "شعر", "زندگی‌نامه")

internal fun readBooks(store: LocalStore): List<Book> = runCatching {
    val array = JSONArray(store.getString(BOOKS_KEY, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val title = o.optString("title")
            if (title.isBlank()) continue
            add(
                Book(
                    id = o.optString("id").ifBlank { "b$i" },
                    title = title,
                    author = o.optString("author"),
                    status = runCatching { BookStatus.valueOf(o.optString("status")) }.getOrDefault(BookStatus.WANT),
                    addedIso = o.optString("addedIso"),
                ),
            )
        }
    }
}.getOrDefault(emptyList())

private fun writeBooks(store: LocalStore, books: List<Book>) {
    val array = JSONArray()
    books.forEach { b ->
        array.put(
            JSONObject()
                .put("id", b.id).put("title", b.title).put("author", b.author)
                .put("status", b.status.name).put("addedIso", b.addedIso),
        )
    }
    store.putString(BOOKS_KEY, array.toString())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onBack: () -> Unit) {
    val store = LocalAppContainer.current.store
    var books by remember { mutableStateOf(readBooks(store)) }
    var genres by remember { mutableStateOf(store.getStringSet(GENRES_KEY)) }
    var titleInput by remember { mutableStateOf("") }
    var authorInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("کتابخانه", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("کدوم ژانرها را دوست داری؟", style = MaterialTheme.typography.titleMedium)
            Text(
                "انتخاب تو، نه لیست تحمیلی. این‌ها فقط روی گوشی خودت می‌ماند.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GENRES.forEach { genre ->
                    FilterChip(
                        selected = genre in genres,
                        onClick = {
                            genres = if (genre in genres) genres - genre else genres + genre
                            store.putStringSet(GENRES_KEY, genres)
                        },
                        label = { Text(genre) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("کتاب‌های من", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("اسم کتاب") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = authorInput,
                onValueChange = { authorInput = it },
                label = { Text("نویسنده (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("افزودن به کتابخانه") {
                val title = titleInput.trim()
                if (title.isNotEmpty()) {
                    val book = Book(
                        id = "b${System.currentTimeMillis()}",
                        title = title,
                        author = authorInput.trim(),
                        status = BookStatus.WANT,
                        addedIso = JalaliDate.todayIso(),
                    )
                    books = listOf(book) + books
                    writeBooks(store, books)
                    titleInput = ""
                    authorInput = ""
                }
            }

            if (books.isEmpty()) {
                SectionCard(
                    title = "هنوز کتابی اضافه نکردی",
                    body = "اولین کتابی که دوست داری بخوانی را بالا بنویس؛ بعد با یک لمس وضعیتش را عوض کن.",
                ) { }
            }

            BookStatus.entries.forEach { status ->
                val items = books.filter { it.status == status }
                if (items.isEmpty()) return@forEach
                Text("${status.emoji} ${status.label} (${items.size})", style = MaterialTheme.typography.titleSmall)
                items.forEach { book ->
                    SectionCard(
                        title = book.title,
                        body = (if (book.author.isBlank()) "" else "${book.author} · ") +
                            JalaliDate.formatFaLong(book.addedIso) +
                            " · برای تغییر وضعیت لمس کن",
                        onClick = {
                            books = books.map { if (it.id == book.id) it.copy(status = status.next()) else it }
                            writeBooks(store, books)
                        },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            books = books.filterNot { it.id == book.id }
                            writeBooks(store, books)
                        }) { Text("حذف") }
                    }
                }
            }
        }
    }
}
