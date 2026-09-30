package com.hamyareman.ir.ui.more

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.BuildConfig
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.appwrite.RowPermissions
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import io.appwrite.Query
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** معرفی محصول؛ عمداً هیچ نام شخصی یا نام سازنده‌ای در این صفحه نمی‌آید. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("درباره ما", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("همیار من", style = MaterialTheme.typography.headlineSmall)
            Text(
                "همیار من یک همراه آموزشی و شخصی برای برنامه‌ریزی درس، تمرین مهارت‌ها، " +
                    "سلامتی و نگهداری امن یادداشت‌هاست. هدف برنامه این است که ابزارهای روزمره " +
                    "در یک فضای ساده، فارسی و قابل‌اعتماد در دسترس باشند.",
                style = MaterialTheme.typography.bodyLarge,
            )
            SectionCard(
                "حریم خصوصی",
                "بخش‌های حساس با قفل مستقل و رمزگذاری دستگاه محافظت می‌شوند. " +
                    "هرجا داده‌ای برای همگام‌سازی یا پشتیبانی به سرور فرستاده شود، همان‌جا صریح اعلام می‌شود.",
            ) { }
            SectionCard("نسخه برنامه", BuildConfig.VERSION_NAME) { }
        }
    }
}

private data class SupportTicket(
    val id: String,
    val tracking: String,
    val category: String,
    val subject: String,
    val message: String,
    val createdAt: Long,
    val status: String = "ثبت‌شده",
    val reply: String = "",
)

private const val SUPPORT_STORE = "hamyar_support"
private const val SUPPORT_DRAFT_SUBJECT = "draft_subject"
private const val SUPPORT_DRAFT_MESSAGE = "draft_message"
private const val SUPPORT_DRAFT_CATEGORY = "draft_category"
private const val SUPPORT_HISTORY = "history"
private val supportCategories = listOf("فنی", "محتوا", "حساب و اشتراک", "پیشنهاد", "سایر")

private fun readSupportHistory(store: LocalStore): List<SupportTicket> = runCatching {
    val a = JSONArray(store.getString(SUPPORT_HISTORY, "[]"))
    buildList {
        for (i in 0 until a.length()) add(ticketFromJson(a.getJSONObject(i)))
    }.sortedByDescending { it.createdAt }
}.getOrDefault(emptyList())

private fun ticketFromJson(o: JSONObject) = SupportTicket(
    id = o.optString("id"),
    tracking = o.optString("tracking"),
    category = o.optString("category", "سایر"),
    subject = o.optString("subject"),
    message = o.optString("message"),
    createdAt = o.optLong("createdAt"),
    status = o.optString("status", "ثبت‌شده"),
    reply = o.optString("reply"),
)

private fun ticketJson(t: SupportTicket) = JSONObject()
    .put("id", t.id)
    .put("tracking", t.tracking)
    .put("category", t.category)
    .put("subject", t.subject)
    .put("message", t.message)
    .put("createdAt", t.createdAt)
    .put("status", t.status)
    .put("reply", t.reply)

private fun writeSupportHistory(store: LocalStore, tickets: List<SupportTicket>) {
    store.putString(SUPPORT_HISTORY, JSONArray().apply { tickets.forEach { put(ticketJson(it)) } }.toString())
}

private fun mergeTickets(local: List<SupportTicket>, remote: List<SupportTicket>): List<SupportTicket> =
    (local + remote).groupBy { it.id }.map { (_, versions) ->
        // پاسخ سرور بر نسخهٔ محلی مقدم است.
        versions.maxWithOrNull(compareBy<SupportTicket> { it.reply.isNotBlank() }.thenBy { it.status != "ثبت‌شده" })!!
    }.sortedByDescending { it.createdAt }

/** فرم سروری پشتیبانی همراه با پیش‌نویس، کد پیگیری و تاریخچهٔ پاسخ‌ها. */
@Composable
fun ContactScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { LocalStore(context, SUPPORT_STORE) }
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf(store.getString(SUPPORT_DRAFT_CATEGORY, supportCategories.first())) }
    var subject by remember { mutableStateOf(store.getString(SUPPORT_DRAFT_SUBJECT, "")) }
    var message by remember { mutableStateOf(store.getString(SUPPORT_DRAFT_MESSAGE, "")) }
    var tickets by remember { mutableStateOf(readSupportHistory(store)) }
    var sending by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            refreshing = true
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isNotBlank() && container.tables.isConfigured) {
                when (val result = container.tables.list(
                    TableIds.APP_STATE,
                    listOf(
                        Query.equal("userId", uid),
                        Query.equal("key", "support_ticket"),
                        Query.orderDesc("updatedAt"),
                        Query.limit(50),
                    ),
                )) {
                    is AppResult.Ok -> {
                        val remote = result.value.mapNotNull { row ->
                            runCatching {
                                val root = JSONObject(row.payload["payload"]?.toString().orEmpty())
                                ticketFromJson(root).copy(id = root.optString("id", row.id))
                            }.getOrNull()
                        }
                        tickets = mergeTickets(tickets, remote)
                        writeSupportHistory(store, tickets)
                    }
                    is AppResult.Err -> if (tickets.isEmpty()) notice = result.error.userMessage
                }
            }
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("تماس با ما", onBack)
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "درخواست ابتدا به‌صورت پیش‌نویس روی همین گوشی می‌ماند و فقط با دکمهٔ ارسال به سرور می‌رود. " +
                        "لطفاً رمز، PIN یا اطلاعات بانکی را ننویس.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Text("موضوع درخواست", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    supportCategories.take(3).forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item; store.putString(SUPPORT_DRAFT_CATEGORY, item) },
                            label = { Text(item) },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    supportCategories.drop(3).forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item; store.putString(SUPPORT_DRAFT_CATEGORY, item) },
                            label = { Text(item) },
                        )
                    }
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it.take(120); store.putString(SUPPORT_DRAFT_SUBJECT, subject) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(4_000); store.putString(SUPPORT_DRAFT_MESSAGE, message) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("شرح درخواست") },
                    minLines = 6,
                )
                Text("پیش‌نویس خودکار ذخیره می‌شود.", style = MaterialTheme.typography.labelSmall)
                PrimaryButton(if (sending) "در حال ارسال…" else "ارسال برای پشتیبانی") {
                    if (sending) return@PrimaryButton
                    when {
                        subject.trim().length < 3 -> notice = "یک عنوان کوتاه و روشن بنویس."
                        message.trim().length < 10 -> notice = "شرح درخواست باید دست‌کم ۱۰ نویسه باشد."
                        else -> scope.launch {
                            sending = true
                            val uid = container.auth.cachedUserId()
                                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                            if (uid.isBlank()) {
                                notice = "برای ارسال و پیگیری درخواست وارد حسابت شو."
                                sending = false
                                return@launch
                            }
                            val id = "support_${UUID.randomUUID().toString().replace("-", "").take(24)}"
                            val tracking = id.removePrefix("support_").take(8).uppercase()
                            val ticket = SupportTicket(
                                id = id,
                                tracking = tracking,
                                category = category,
                                subject = subject.trim(),
                                message = message.trim(),
                                createdAt = System.currentTimeMillis(),
                            )
                            val payload = ticketJson(ticket).put("appVersion", BuildConfig.VERSION_NAME)
                                .put("android", Build.VERSION.SDK_INT)
                            val data = mapOf(
                                "userId" to uid,
                                "key" to "support_ticket",
                                "payload" to payload.toString(),
                                "text" to "[$category] ${subject.trim()}\n${message.trim()}".take(7_000),
                                "updatedAt" to ticket.createdAt,
                            )
                            when (val result = container.tables.upsert(
                                tableId = TableIds.APP_STATE,
                                rowId = id,
                                data = data,
                                permissions = RowPermissions.forUser(uid),
                            )) {
                                is AppResult.Ok -> {
                                    tickets = listOf(ticket) + tickets.filterNot { it.id == ticket.id }
                                    writeSupportHistory(store, tickets)
                                    subject = ""
                                    message = ""
                                    store.remove(SUPPORT_DRAFT_SUBJECT)
                                    store.remove(SUPPORT_DRAFT_MESSAGE)
                                    notice = "درخواست ثبت شد؛ کد پیگیری: $tracking"
                                }
                                is AppResult.Err -> notice = result.error.userMessage
                            }
                            sending = false
                        }
                    }
                }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("درخواست‌ها و پاسخ‌ها", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.TextButton(onClick = { if (!refreshing) refresh() }) {
                        Text(if (refreshing) "در حال بررسی…" else "به‌روزرسانی")
                    }
                }
            }
            if (tickets.isEmpty()) {
                item { Text("هنوز درخواستی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(tickets, key = { it.id }) { ticket ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ticket.subject, style = MaterialTheme.typography.titleSmall)
                        Text("${ticket.category} · کد ${ticket.tracking}", color = MaterialTheme.colorScheme.primary)
                        Text(JalaliDate.stampFa(ticket.createdAt), style = MaterialTheme.typography.labelSmall)
                        Text(ticket.message, style = MaterialTheme.typography.bodyMedium)
                        Text("وضعیت: ${ticket.status}", style = MaterialTheme.typography.bodySmall)
                        if (ticket.reply.isNotBlank()) {
                            Text("پاسخ پشتیبانی", style = MaterialTheme.typography.titleSmall)
                            Text(ticket.reply, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
