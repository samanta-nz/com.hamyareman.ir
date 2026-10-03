package com.hamyareman.admin.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.ParsPackListing
import com.hamyareman.admin.ParsPackObject
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.appwrite.AdminStats
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.toPersianDigits
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

private enum class CommandTab(val title: String) {
    DASHBOARD("دید کلی"), USERS("کاربران"), INBOX("پیام‌ها"), DATA("داده‌ها"), MEDIA("رسانه"),
}

/** نقطهٔ ورود نسخهٔ ۲؛ نام قدیمی حفظ شده تا activity و deep state تغییر نکند. */
@Composable
fun AdminHomeScreen(onLogout: () -> Unit, onSettings: () -> Unit = {}) {
    AdminCommandCenter(onLogout = onLogout, onSettings = onSettings)
}

/** پوستهٔ نسخهٔ ۲: مسیرهای اصلی محدود و روشن‌اند، عملیات تخصصی در منوی بیشتر قرار دارد. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCommandCenter(onLogout: () -> Unit, onSettings: () -> Unit) {
    var tab by remember { mutableStateOf(CommandTab.DASHBOARD) }
    var moreOpen by remember { mutableStateOf(false) }
    var moreScreen by remember { mutableStateOf<String?>(null) }
    var selectedUser by remember { mutableStateOf<String?>(null) }

    val user = selectedUser
    if (user != null) {
        AdminUserScreen(userId = user, onBack = { selectedUser = null })
        return
    }
    when (moreScreen) {
        "payments" -> {
            AdminAuxiliaryFrame("پرداخت‌ها و اشتراک", onBack = { moreScreen = null }) { AdminStatsScreen(onOpen = { selectedUser = it }) }
            return
        }
        "installments" -> {
            AdminAuxiliaryFrame("اقساط", onBack = { moreScreen = null }) { AdminInstallmentsScreen(onOpenUser = { selectedUser = it }) }
            return
        }
        "functions" -> {
            AdminAuxiliaryFrame("توابع سرور", onBack = { moreScreen = null }) { AdminFunctionsScreen() }
            return
        }
        "auth" -> {
            AdminAuxiliaryFrame("حساب‌ها و نشست‌ها", onBack = { moreScreen = null }) { AdminAuthScreen(onOpen = { selectedUser = it }) }
            return
        }
        "free-reading" -> {
            AdminAuxiliaryFrame("مطالعهٔ آزاد", onBack = { moreScreen = null }) { AdminFreeReadingScreen() }
            return
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("مرکز مدیریت همیار", style = MaterialTheme.typography.titleLarge)
                        Text(tab.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { moreOpen = true }) {
                            Icon(Icons.Outlined.MoreHoriz, contentDescription = "گزینه‌های بیشتر")
                        }
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("پرداخت‌ها، اشتراک و آمار") },
                                leadingIcon = { Icon(Icons.Outlined.Analytics, null) },
                                onClick = { moreOpen = false; moreScreen = "payments" },
                            )
                            DropdownMenuItem(
                                text = { Text("اقساط") },
                                leadingIcon = { Icon(Icons.Outlined.AccountCircle, null) },
                                onClick = { moreOpen = false; moreScreen = "installments" },
                            )
                            DropdownMenuItem(
                                text = { Text("توابع و اجرای سرور") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                onClick = { moreOpen = false; moreScreen = "functions" },
                            )
                            DropdownMenuItem(
                                text = { Text("حساب‌ها و نشست‌ها") },
                                leadingIcon = { Icon(Icons.Outlined.PersonSearch, null) },
                                onClick = { moreOpen = false; moreScreen = "auth" },
                            )
                            DropdownMenuItem(
                                text = { Text("مطالعهٔ آزاد") },
                                leadingIcon = { Icon(Icons.Outlined.Book, null) },
                                onClick = { moreOpen = false; moreScreen = "free-reading" },
                            )
                            DropdownMenuItem(
                                text = { Text("تنظیمات اتصال و کلیدها") },
                                leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                onClick = { moreOpen = false; onSettings() },
                            )
                            DropdownMenuItem(
                                text = { Text("خروج امن") },
                                leadingIcon = { Icon(Icons.Outlined.Logout, null) },
                                onClick = { moreOpen = false; onLogout() },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                CommandNavItem(tab, CommandTab.DASHBOARD, Icons.Outlined.Dashboard) { tab = CommandTab.DASHBOARD }
                CommandNavItem(tab, CommandTab.USERS, Icons.Outlined.PersonSearch) { tab = CommandTab.USERS }
                CommandNavItem(tab, CommandTab.INBOX, Icons.Outlined.Inbox) { tab = CommandTab.INBOX }
                CommandNavItem(tab, CommandTab.DATA, Icons.Outlined.Storage) { tab = CommandTab.DATA }
                CommandNavItem(tab, CommandTab.MEDIA, Icons.Outlined.FolderOpen) { tab = CommandTab.MEDIA }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                CommandTab.DASHBOARD -> AdminOverviewScreen(onUsers = { tab = CommandTab.USERS }, onPayments = { moreScreen = "payments" })
                CommandTab.USERS -> AdminPresentUsersScreen(onOpen = { selectedUser = it })
                CommandTab.INBOX -> AdminSupportInboxScreen()
                CommandTab.DATA -> AdminTablesScreen()
                CommandTab.MEDIA -> AdminMediaHub()
            }
        }
    }
}

@Composable
private fun CommandNavItem(tab: CommandTab, item: CommandTab, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    TextButton(onClick = click, modifier = Modifier.padding(horizontal = 1.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = item.title,
                tint = if (tab == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(item.title, style = MaterialTheme.typography.labelSmall, fontWeight = if (tab == item) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAuxiliaryFrame(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { TextButton(onClick = onBack) { Text("بازگشت") } },
        )
        Box(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun AdminOverviewScreen(onUsers: () -> Unit, onPayments: () -> Unit) {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    fun refresh() {
        loading = true; error = null
        scope.launch {
            when (val r = adminIo { api.adminStats() }) {
                is AppResult.Ok -> stats = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("فرماندهی روزانه", style = MaterialTheme.typography.titleLarge)
                Text("کارهای حساس را از صف‌ها انجام دهید؛ هر تغییر حساب یا داده مستقیماً روی محیط انتخاب‌شده اعمال می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onUsers) { Text("کاربران") }
                    OutlinedButton(onClick = onPayments) { Text("صف مالی") }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        stats?.let { s ->
            OverviewMetric("کاربران ثبت‌شده", s.usersTotal, onUsers)
            OverviewMetric("پرداخت‌های در انتظار", s.pendingPay, onPayments)
            OverviewMetric("درخواست‌های استرداد", s.pendingRefund, onPayments)
            OverviewMetric("اشتراک‌های فعال", s.paidProfiles, onPayments)
        }
        OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text("به‌روزرسانی داشبورد") }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("کنترل امنیت", fontWeight = FontWeight.Bold)
                Text("کلیدها در vault محلی رمز شده‌اند. از منوی بیشتر، اتصال‌ها را آزمایش کنید و پس از تعویض کلید سرویس، کلید قدیمی را پاک کنید.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OverviewMetric(label: String, value: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(if (value < 0) "—" else toPersianDigits(value.toString()), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** صندوق پیشنهادها/تیکت‌ها با table قابل تنظیم؛ برای schemaهای جدید نیاز به هاردکد ندارد. */
@Composable
fun AdminSupportInboxScreen() {
    val container = LocalAdmin.current
    val api = container.api
    val scope = rememberCoroutineScope()
    var tableId by remember { mutableStateOf(container.prefs.supportTableId) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }

    fun load() {
        loading = true; error = null
        scope.launch {
            val cols = adminIo { api.listColumns(tableId.trim()) }
            val data = adminIo { api.listRows(tableId.trim()) }
            when (cols) {
                is AppResult.Ok -> columns = cols.value
                is AppResult.Err -> error = cols.error.userMessage
            }
            when (data) {
                is AppResult.Ok -> rows = data.value
                is AppResult.Err -> error = data.error.userMessage
            }
            loading = false
        }
    }
    val open = selected
    if (open != null) {
        AdminSupportReplyScreen(
            tableId = tableId,
            row = open,
            columns = columns,
            onBack = { selected = null },
            onSaved = { selected = null; load() },
        )
        return
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("پیشنهادها و مکاتبات", style = MaterialTheme.typography.titleLarge)
        Text("نام جدول را با schema فعلی Appwrite تنظیم کنید. ستون‌های reply / adminReply و status اگر وجود داشته باشند مستقیم به‌روزرسانی می‌شوند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(tableId, { tableId = it }, label = { Text("شناسهٔ جدول تیکت/پیشنهاد") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { if (tableId.isNotBlank()) load() }, modifier = Modifier.fillMaxWidth(), enabled = !loading && tableId.isNotBlank()) { Text(if (loading) "در حال دریافت…" else "دریافت صندوق") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        if (!loading && rows.isEmpty() && error == null) Text("پس از انتخاب جدول، مکاتبات اینجا دیده می‌شوند.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { inboxRowId(it) }) { row ->
                val map = inboxFields(row)
                Card(onClick = { selected = row }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(firstNonBlank(map, listOf("title", "subject", "name", "email", "userId")).ifBlank { "پیام بدون عنوان" }, fontWeight = FontWeight.Bold)
                        Text(firstNonBlank(map, listOf("message", "text", "suggestion", "body", "content")).take(220).ifBlank { "جزئیات را باز کنید." })
                        val status = map["status"].orEmpty()
                        if (status.isNotBlank()) Text("وضعیت: $status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSupportReplyScreen(
    tableId: String,
    row: JSONObject,
    columns: List<String>,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val api = LocalAdmin.current.api
    val scope = rememberCoroutineScope()
    val id = inboxRowId(row)
    val fields = remember(row) { inboxFields(row) }
    val replyColumn = columns.firstOrNull { it.equals("adminReply", true) }
        ?: columns.firstOrNull { it.equals("reply", true) }
        ?: columns.firstOrNull { it.equals("response", true) }
    var reply by remember { mutableStateOf(replyColumn?.let { fields[it] }.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        androidx.compose.material3.TopAppBar(
            title = { Text("پاسخ مکاتبه") },
            navigationIcon = { IconButton(onClick = onBack) { Text("بازگشت") } },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("شناسه: $id", style = MaterialTheme.typography.labelSmall)
            fields.forEach { (key, value) ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp)) { Text(key, style = MaterialTheme.typography.labelMedium); Text(value.ifBlank { "—" }) } }
            }
            if (replyColumn == null) {
                Text("در schema این جدول ستون reply، response یا adminReply نیست. از بخش داده‌ها ستون پاسخ را بسازید یا همان‌جا ویرایش کنید.", color = MaterialTheme.colorScheme.error)
            } else {
                OutlinedTextField(reply, { reply = it }, label = { Text("پاسخ مدیر") }, modifier = Modifier.fillMaxWidth().height(150.dp))
                Button(
                    onClick = {
                        busy = true; error = null
                        scope.launch {
                            val patch = JSONObject().put(replyColumn, reply.trim())
                            if (columns.any { it.equals("status", true) }) patch.put("status", "answered")
                            if (columns.any { it.equals("answeredAtMs", true) }) patch.put("answeredAtMs", System.currentTimeMillis())
                            when (val result = adminIo { api.saveRow(tableId, id, patch, create = false) }) {
                                is AppResult.Ok -> onSaved()
                                is AppResult.Err -> error = result.error.userMessage
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && reply.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "در حال ارسال…" else "ثبت پاسخ و بستن تیکت") }
            }
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text("حذف این مکاتبه") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف مکاتبه؟") },
            text = { Text("این عمل بازگشت ندارد.") },
            confirmButton = { TextButton(onClick = {
                confirmDelete = false; busy = true
                scope.launch {
                    when (val r = adminIo { api.deleteRow(tableId, id) }) {
                        is AppResult.Ok -> onSaved()
                        is AppResult.Err -> error = r.error.userMessage
                    }
                    busy = false
                }
            }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } },
        )
    }
}

private fun inboxRowId(row: JSONObject): String = row.optString("\$id").ifBlank { row.optString("id") }
private fun inboxFields(row: JSONObject): Map<String, String> = buildMap {
    val it = row.keys()
    while (it.hasNext()) {
        val key = it.next()
        if (!key.startsWith("$")) put(key, row.opt(key)?.toString().orEmpty())
    }
}
private fun firstNonBlank(map: Map<String, String>, candidates: List<String>): String = candidates.firstNotNullOfOrNull { map[it]?.takeIf(String::isNotBlank) }.orEmpty()

@Composable
private fun AdminMediaHub() {
    var source by remember { mutableStateOf("parspack") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = source == "parspack", onClick = { source = "parspack" }, label = { Text("ParsPack") })
            FilterChip(selected = source == "appwrite", onClick = { source = "appwrite" }, label = { Text("Appwrite Storage") })
        }
        if (source == "parspack") ParsPackBrowser() else AdminStorageScreen()
    }
}

/** مرور، پیش‌نمایش، overwrite و حذف مستقیم اشیای ParsPack. */
@Composable
private fun ParsPackBrowser() {
    val container = LocalAdmin.current
    val storage = container.parsPack
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prefix by remember { mutableStateOf(container.prefs.parsPackPrefix.trim().trim('/').let { if (it.isBlank()) "" else "$it/" }) }
    var listing by remember { mutableStateOf(ParsPackListing(prefix = prefix)) }
    var filter by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<ParsPackObject?>(null) }
    var deleteTarget by remember { mutableStateOf<ParsPackObject?>(null) }

    fun refresh(targetPrefix: String = prefix) {
        prefix = targetPrefix.trim().trimStart('/').let { if (it.isBlank()) "" else "$it".trimEnd('/') + "/" }
        loading = true; error = null
        scope.launch {
            try {
                listing = storage.listDirectory(prefix)
            } catch (t: Throwable) {
                error = t.message ?: "دریافت فهرست ناموفق بود."
            }
            loading = false
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            loading = true; error = null
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes == null) {
                error = "فایل انتخاب‌شده خوانده نشد."
            } else {
                val typed = key.trim().trimStart('/')
                val target = if ('/' in typed || prefix.isBlank()) typed else prefix + typed
                if (target.isBlank()) {
                    error = "نام فایل یا مسیر مقصد را بنویسید؛ همان مسیر فایل قبلی را جایگزین می‌کند."
                } else {
                    try {
                        storage.upload(target, bytes, context.contentResolver.getType(uri).orEmpty())
                        key = ""
                        listing = storage.listDirectory(prefix)
                    } catch (t: Throwable) {
                        error = t.message ?: "آپلود ناموفق بود."
                    }
                }
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh(prefix) }

    val open = preview
    if (open != null) {
        ParsPackObjectPreview(objectInfo = open, onBack = { preview = null })
        return
    }
    val visibleFiles = listing.objects.filter { filter.isBlank() || it.key.contains(filter.trim(), ignoreCase = true) }
    val parent = prefix.trimEnd('/').substringBeforeLast('/', "").let { if (it.isBlank()) "" else "$it/" }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("فایل‌اکسپلورر ParsPack", style = MaterialTheme.typography.titleMedium)
        Text("باکت ${container.prefs.parsPackBucket} · ${if (prefix.isBlank()) "ریشه" else prefix}", style = MaterialTheme.typography.labelMedium)
        if (!storage.configured) {
            Text("کلیدهای ParsPack هنوز در vault ذخیره نشده‌اند. از تنظیمات اتصال، access/secret key را وارد و آزمون کنید.", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { refresh(parent) }, enabled = !loading && prefix.isNotBlank(), modifier = Modifier.weight(1f)) { Text("پوشهٔ بالاتر") }
            Button(onClick = { refresh() }, enabled = !loading, modifier = Modifier.weight(1f)) { Text(if (loading) "…" else "تازه‌سازی") }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val quota = 20L * 1024 * 1024 * 1024
                Text("حجم پوشهٔ باز: ${humanBytes(listing.bytes)} · ${toPersianDigits(listing.objects.size.toString())} فایل · ${toPersianDigits(listing.folders.size.toString())} پوشه")
                LinearProgressIndicator(progress = { (listing.bytes.toFloat() / quota).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("نمودار حجم این پوشه است؛ ترافیک کل سرویس فقط از پنل آمار ParsPack قابل دریافت است و این اپ مقدار ساختگی نشان نمی‌دهد.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(filter, { filter = it }, label = { Text("جست‌وجو در پوشهٔ باز") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(key, { key = it }, label = { Text("نام فایل یا مسیر مقصد برای آپلود / جایگزینی") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedButton(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth(), enabled = !loading && key.isNotBlank()) { Text("انتخاب و آپلود / جایگزینی") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (listing.folders.isNotEmpty()) item { Text("پوشه‌ها", fontWeight = FontWeight.Bold) }
            items(listing.folders, key = { "folder:$it" }) { folder ->
                Card(onClick = { refresh(folder) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Column { Text(folder.trimEnd('/').substringAfterLast('/'), fontWeight = FontWeight.Medium); Text(folder, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
            if (visibleFiles.isNotEmpty()) item { Text("فایل‌ها", fontWeight = FontWeight.Bold) }
            items(visibleFiles, key = { it.key }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.key.substringAfterLast('/'), fontWeight = FontWeight.Medium)
                        Text("${humanBytes(item.size)} · ${item.modified.ifBlank { "زمان نامشخص" }}", style = MaterialTheme.typography.labelSmall)
                        Text(item.key, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { preview = item }) { Text("پیش‌نمایش") }
                            TextButton(onClick = { key = item.key }) { Text("جایگزینی") }
                            TextButton(onClick = { deleteTarget = item }) { Text("حذف") }
                        }
                    }
                }
            }
            if (!loading && listing.folders.isEmpty() && visibleFiles.isEmpty()) item { Text("این پوشه خالی است یا کلید شما اجازهٔ فهرست‌کردن ندارد.") }
        }
    }
    val target = deleteTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف فایل؟") },
            text = { Text(target.key) },
            confirmButton = { TextButton(onClick = {
                deleteTarget = null; loading = true
                scope.launch {
                    try {
                        storage.delete(target.key)
                        listing = storage.listDirectory(prefix)
                    } catch (t: Throwable) {
                        error = t.message ?: "حذف ناموفق بود."
                    }
                    loading = false
                }
            }) { Text("حذف") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("انصراف") } },
        )
    }
}

private fun humanBytes(value: Long): String = when {
    value >= 1024L * 1024 * 1024 -> "%.2f GB".format(Locale.US, value.toDouble() / (1024 * 1024 * 1024))
    value >= 1024L * 1024 -> "%.1f MB".format(Locale.US, value.toDouble() / (1024 * 1024))
    value >= 1024L -> "%.1f KB".format(Locale.US, value.toDouble() / 1024)
    else -> "${toPersianDigits(value.toString())} بایت"
}

/** HTML رمز‌شده را تنها در حافظه unwrap می‌کند؛ هیچ plaintext روی دیسک ذخیره نمی‌شود. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParsPackObjectPreview(objectInfo: ParsPackObject, onBack: () -> Unit) {
    val container = LocalAdmin.current
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var decoded by remember { mutableStateOf(false) }
    var keyLoading by remember { mutableStateOf(false) }
    var keyError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(objectInfo.key) {
        try {
            bytes = container.parsPack.read(objectInfo.key)
        } catch (t: Throwable) {
            error = t.message ?: "فایل خوانده نشد."
        }
        loading = false
    }
    val raw = bytes ?: ByteArray(0)
    val isHmk = raw.size >= 4 && raw[0] == 'H'.code.toByte() && raw[1] == 'M'.code.toByte() && raw[2] == 'K'.code.toByte() && raw[3] == '1'.code.toByte()

    // تنها هنگام درخواست نمایش بازشده، ردیف کلیدِ همان دیتابیس خوانده می‌شود.
    // کش آن با Android Keystore پوشانده می‌شود و خود plaintext HTML هرگز نوشته نمی‌شود.
    LaunchedEffect(decoded, isHmk, container.prefs.htmlMediaKeyB64) {
        if (decoded && isHmk && container.prefs.htmlMediaKeyB64.isBlank() && !keyLoading) {
            keyLoading = true
            keyError = null
            when (val result = adminIo { container.api.fetchHtmlMediaKeyB64() }) {
                is AppResult.Ok -> container.prefs.cacheHtmlMediaKeyB64(result.value)
                is AppResult.Err -> keyError = result.error.userMessage
            }
            keyLoading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        androidx.compose.material3.TopAppBar(
            title = { Text(objectInfo.key.substringAfterLast('/').ifBlank { "پیش‌نمایش" }) },
            navigationIcon = { IconButton(onClick = onBack) { Text("بازگشت") } },
        )
        when {
            loading -> CircularProgressIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
            error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            else -> {
                val key = container.prefs.htmlMediaKeyB64
                val decodedResult = if (decoded && isHmk && key.isNotBlank()) {
                    runCatching { unwrapHmk1(raw, key) }
                } else null
                val rendered = decodedResult?.getOrElse { raw } ?: raw
                val decodeError = decodedResult?.exceptionOrNull()?.message
                Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${toPersianDigits(raw.size.toString())} بایت · ${if (isHmk) "HMK1 encrypted" else "raw"}", style = MaterialTheme.typography.labelMedium)
                    if (isHmk) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !decoded, onClick = { decoded = false; keyError = null }, label = { Text("بدون رمزگشایی") })
                            FilterChip(selected = decoded, onClick = { decoded = true }, enabled = !keyLoading, label = { Text(if (keyLoading) "دریافت کلید…" else "نمایش decode‌شده") })
                        }
                        if (decoded && key.isBlank() && !keyLoading) {
                            Text(keyError ?: "کلید HTML از app_state/html_media_key دریافت نشد.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    decodeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    ParsPackPreviewContent(objectInfo.key, rendered, decoded && isHmk && decodedResult?.isSuccess == true)
                }
            }
        }
    }
}

@Composable
private fun ParsPackPreviewContent(key: String, bytes: ByteArray, decodedHtml: Boolean) {
    val kind = if (!decodedHtml) previewKind(key, "") else PreviewKind.HTML
    when (kind) {
        PreviewKind.IMAGE -> {
            val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            if (bitmap == null) Text("تصویر قابل خواندن نیست.") else Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().height(360.dp), contentScale = ContentScale.Fit)
        }
        PreviewKind.HTML -> AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> WebView(ctx).apply { webViewClient = WebViewClient(); settings.javaScriptEnabled = true; settings.domStorageEnabled = true } },
            update = { it.loadDataWithBaseURL(null, bytes.toString(Charsets.UTF_8), "text/html", "utf-8", null) },
        )
        PreviewKind.TEXT -> Text(bytes.toString(Charsets.UTF_8).take(200_000), Modifier.fillMaxSize().verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
        PreviewKind.AUDIO -> AudioPlayer(bytes, key.hashCode().toString(), LocalContext.current.cacheDir)
        PreviewKind.PDF -> PdfPager(bytes, key.hashCode().toString(), LocalContext.current.cacheDir)
        PreviewKind.OTHER -> Text("این نوع فایل پیش‌نمایش تصویری ندارد. حالت raw انتخاب شده است.")
    }
}

private fun unwrapHmk1(wrapped: ByteArray, keyB64: String): ByteArray {
    require(wrapped.size >= 32) { "فایل HMK1 کوتاه یا نامعتبر است." }
    require(keyB64.isNotBlank()) { "کلید HTML تنظیم نشده است." }
    val key = Base64.decode(keyB64, Base64.DEFAULT)
    require(key.size == 32) { "کلید HTML باید ۳۲ بایت Base64 باشد." }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, wrapped.copyOfRange(4, 16)))
    return cipher.doFinal(wrapped, 16, wrapped.size - 16)
}
