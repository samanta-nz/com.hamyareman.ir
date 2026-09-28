package com.hamyareman.admin.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hamyareman.admin.LocalAdmin
import com.hamyareman.admin.adminIo
import com.hamyareman.ir.platform.core.appwrite.AdminUser
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.BillingStatus
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private fun jsonId(o: JSONObject): String = o.optString("\$id").ifBlank { o.optString("id") }

private fun jsonCell(o: JSONObject, key: String): String {
    if (!o.has(key) || o.isNull(key)) return ""
    return when (val v = o.opt(key)) {
        null, JSONObject.NULL -> ""
        is JSONObject, is JSONArray -> v.toString()
        else -> v.toString()
    }
}

private fun rowDataMap(o: JSONObject): Map<String, String> {
    val out = linkedMapOf<String, String>()
    val keys = o.keys()
    while (keys.hasNext()) {
        val k = keys.next()
        if (k.startsWith("$")) continue
        out[k] = jsonCell(o, k)
    }
    return out
}

@Composable
fun AdminStorageScreen() {
    val api = LocalAdmin.current.api
    val context = LocalContext.current
    var buckets by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var files by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var bucketId by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null || bucketId.isBlank()) return@rememberLauncherForActivityResult
        scope.launch {
            loading = true
            error = null
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes == null) {
                error = "خواندن فایل انتخاب‌شده ممکن نشد."
                loading = false
                return@launch
            }
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val name = newName.ifBlank { uri.lastPathSegment?.substringAfterLast('/') ?: "file" }
            when (val r = adminIo { api.uploadFile(bucketId, name, bytes, mime) }) {
                is AppResult.Ok -> files = (adminIo { api.listFiles(bucketId) } as? AppResult.Ok)?.value ?: files
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        when (val r = adminIo { api.listBuckets() }) {
            is AppResult.Ok -> {
                buckets = r.value
                if (bucketId.isBlank()) bucketId = r.value.firstOrNull()?.let { jsonId(it) }.orEmpty()
            }
            is AppResult.Err -> error = r.error.userMessage
        }
        loading = false
    }
    LaunchedEffect(bucketId) {
        if (bucketId.isBlank()) return@LaunchedEffect
        loading = true
        when (val r = adminIo { api.listFiles(bucketId) }) {
            is AppResult.Ok -> files = r.value
            is AppResult.Err -> error = r.error.userMessage
        }
        loading = false
    }

    val open = preview
    if (open != null) {
        AdminFilePreview(bucketId = bucketId, fileId = open.first, name = open.second, mime = open.third, onClose = { preview = null })
        return
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("Storage")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("همهٔ نوع فایل‌ها با صفحه‌بندی کامل. عکس، صدا، پی‌دی‌اف، HTML و متن داخل همین اپ باز می‌شوند.")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buckets.forEach { b ->
                    val id = jsonId(b)
                    FilterChip(selected = bucketId == id, onClick = { bucketId = id }, label = { Text(b.optString("name").ifBlank { id }) })
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (loading) CircularProgressIndicator()
            Text(toPersianDigits(files.size.toString()) + " فایل")
            OutlinedTextField(newName, { newName = it }, label = { Text("نام فایل برای آپلود") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            PrimaryButton("انتخاب و آپلود") { pick.launch("*/*") }
            files.forEach { f ->
                val id = jsonId(f)
                val name = f.optString("name").ifBlank { id }
                val mime = f.optString("mimeType").ifBlank { f.optString("mime_type") }
                val size = f.optLong("sizeOriginal")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Text("$mime · ${if (size > 0) toPersianDigits(size.toString()) + " بایت" else id}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { preview = Triple(id, name, mime) }) { Text("باز کردن داخل اپ") }
                            TextButton(onClick = {
                                scope.launch {
                                    when (val r = adminIo { api.deleteFile(bucketId, id) }) {
                                        is AppResult.Ok -> files = files.filterNot { jsonId(it) == id }
                                        is AppResult.Err -> error = r.error.userMessage
                                    }
                                }
                            }) { Text("حذف") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTablesScreen() {
    val api = LocalAdmin.current.api
    var tables by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var tableId by remember { mutableStateOf("") }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    var creating by remember { mutableStateOf(false) }
    var dump by remember { mutableStateOf("") }
    var ok by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reloadRows() {
        when (val c = adminIo { api.listColumns(tableId) }) {
            is AppResult.Ok -> columns = c.value
            is AppResult.Err -> error = c.error.userMessage
        }
        when (val r = adminIo { api.listRows(tableId) }) {
            is AppResult.Ok -> {
                rows = r.value
                if (columns.isEmpty() && r.value.isNotEmpty()) {
                    columns = rowDataMap(r.value.first()).keys.toList()
                }
            }
            is AppResult.Err -> error = r.error.userMessage
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        when (val r = adminIo { api.listTables() }) {
            is AppResult.Ok -> {
                tables = r.value
                if (tableId.isBlank()) tableId = r.value.firstOrNull()?.first.orEmpty()
            }
            is AppResult.Err -> error = r.error.userMessage
        }
        loading = false
    }
    LaunchedEffect(tableId) {
        if (tableId.isBlank()) return@LaunchedEffect
        loading = true
        error = null
        columns = emptyList()
        rows = emptyList()
        reloadRows()
        loading = false
    }

    val edit = editing
    if (edit != null) {
        AdminRowEditor(
            title = "ویرایش ردیف",
            columns = columns.ifEmpty { rowDataMap(edit).keys.toList() },
            initial = rowDataMap(edit),
            onCancel = { editing = null },
            onSave = { map ->
                scope.launch {
                    loading = true
                    val data = JSONObject()
                    map.forEach { (k, v) -> data.put(k, v) }
                    when (val r = adminIo { api.saveRow(tableId, jsonId(edit), data, false) }) {
                        is AppResult.Ok -> {
                            editing = null
                            reloadRows()
                        }
                        is AppResult.Err -> error = r.error.userMessage
                    }
                    loading = false
                }
            },
        )
        return
    }
    if (creating) {
        AdminRowEditor(
            title = "ردیف تازه",
            columns = columns,
            initial = emptyMap(),
            onCancel = { creating = false },
            onSave = { map ->
                scope.launch {
                    loading = true
                    val data = JSONObject()
                    map.forEach { (k, v) -> data.put(k, v) }
                    when (val r = adminIo { api.saveRow(tableId, "", data, true) }) {
                        is AppResult.Ok -> {
                            creating = false
                            reloadRows()
                        }
                        is AppResult.Err -> error = r.error.userMessage
                    }
                    loading = false
                }
            },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("جداول")
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("هر ستون جدا دیده و ویرایش می‌شود؛ JSON خام نشان داده نمی‌شود.")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tables.forEach { (id, name) ->
                    FilterChip(selected = tableId == id, onClick = { tableId = id }, label = { Text(name.ifBlank { id }) })
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ok?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (loading) CircularProgressIndicator()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { creating = true }, enabled = tableId.isNotBlank()) { Text("ردیف تازه") }
                OutlinedButton(onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        ok = null
                        when (val r = adminIo { api.backupDatabase() }) {
                            is AppResult.Ok -> {
                                dump = r.value.toString(2)
                                ok = "بکاپ گرفته شد" + r.value.optString("fileName").let { if (it.isBlank()) "" else " · $it" }
                            }
                            is AppResult.Err -> error = r.error.userMessage
                        }
                        loading = false
                    }
                }) { Text("بکاپ") }
            }
            Text(toPersianDigits(rows.size.toString()) + " ردیف · " + toPersianDigits(columns.size.toString()) + " ستون")
            val hScroll = rememberScrollState()
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).horizontalScroll(hScroll)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("عمل", modifier = Modifier.width(96.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    columns.forEach { col ->
                        Text(col, modifier = Modifier.widthIn(min = 120.dp, max = 220.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(Modifier.width(96.dp)) {
                            TextButton(onClick = { editing = row }) { Text("ویرایش") }
                            TextButton(onClick = {
                                scope.launch {
                                    when (val r = adminIo { api.deleteRow(tableId, jsonId(row)) }) {
                                        is AppResult.Ok -> rows = rows.filterNot { jsonId(it) == jsonId(row) }
                                        is AppResult.Err -> error = r.error.userMessage
                                    }
                                }
                            }) { Text("حذف") }
                        }
                        columns.forEach { col ->
                            Text(jsonCell(row, col).ifBlank { "—" }, modifier = Modifier.widthIn(min = 120.dp, max = 220.dp), style = MaterialTheme.typography.bodySmall, maxLines = 3)
                        }
                    }
                }
            }
            if (dump.isNotBlank()) {
                OutlinedTextField(dump, { dump = it }, label = { Text("JSON بکاپ") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), minLines = 6)
                PrimaryButton("بازیابی بکاپ") {
                    scope.launch {
                        loading = true
                        error = null
                        ok = null
                        val obj = runCatching { JSONObject(dump) }.getOrNull()
                        if (obj == null) {
                            error = "JSON نامعتبر است."
                            loading = false
                            return@launch
                        }
                        when (val r = adminIo { api.restoreDatabase(obj) }) {
                            is AppResult.Ok -> {
                                ok = r.value
                                reloadRows()
                            }
                            is AppResult.Err -> error = r.error.userMessage
                        }
                        loading = false
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRowEditor(
    title: String,
    columns: List<String>,
    initial: Map<String, String>,
    onCancel: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
) {
    var values by remember(initial, columns) {
        mutableStateOf(columns.associateWith { initial[it].orEmpty() })
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(title, onBack = onCancel)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (columns.isEmpty()) Text("ستونی برای این جدول خوانده نشد.")
            columns.forEach { col ->
                OutlinedTextField(
                    value = values[col].orEmpty(),
                    onValueChange = { v -> values = values.toMutableMap().also { it[col] = v } },
                    label = { Text(col) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 6,
                )
            }
            PrimaryButton("ذخیره") { onSave(values) }
            TextButton(onClick = onCancel) { Text("انصراف") }
        }
    }
}

@Composable
fun AdminFunctionsScreen() {
    val api = LocalAdmin.current.api
    var fns by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var body by remember { mutableStateOf("{}") }
    var result by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        when (val r = adminIo { api.listFunctions() }) {
            is AppResult.Ok -> fns = r.value
            is AppResult.Err -> error = r.error.userMessage
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("توابع")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("اجرای تابع برای کار ادمین لازم نیست؛ این صفحه فقط آزمایش است.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (loading) CircularProgressIndicator()
            fns.forEach { f ->
                val id = jsonId(f)
                FilterChip(selected = selected == id, onClick = { selected = id }, label = { Text(f.optString("name").ifBlank { id }) })
            }
            OutlinedTextField(body, { body = it }, label = { Text("بدنه JSON") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), minLines = 3)
            PrimaryButton("اجرا") {
                val id = selected ?: return@PrimaryButton
                scope.launch {
                    loading = true
                    result = null
                    error = null
                    when (val r = adminIo { api.executeFunction(id, body) }) {
                        is AppResult.Ok -> result = r.value.toString(2)
                        is AppResult.Err -> error = r.error.userMessage
                    }
                    loading = false
                }
            }
            result?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun AdminInstallmentsScreen(onOpenUser: (String) -> Unit = {}) {
    val api = LocalAdmin.current.api
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var planId by remember { mutableStateOf("yearly") }
    var total by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("2") }
    var note by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            when (val r = adminIo { api.listInstallments() }) {
                is AppResult.Ok -> rows = r.value
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("اقساط")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (loading) CircularProgressIndicator()
            OutlinedTextField(userId, { userId = it }, label = { Text("شناسه کاربر") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(email, { email = it }, label = { Text("ایمیل") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(planId, { planId = it }, label = { Text("طرح") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(total, { total = it }, label = { Text("مبلغ کل") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(count, { count = it }, label = { Text("تعداد قسط") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("یادداشت") }, modifier = Modifier.fillMaxWidth())
            PrimaryButton("ثبت قسط") {
                scope.launch {
                    loading = true
                    error = null
                    when (
                        val r = adminIo {
                            api.createInstallment(
                                userId.trim(),
                                email.trim(),
                                planId.trim().ifBlank { "yearly" },
                                total.trim().toIntOrNull() ?: 0,
                                count.trim().toIntOrNull() ?: 1,
                                note.trim(),
                            )
                        }
                    ) {
                        is AppResult.Ok -> {
                            userId = ""; email = ""; total = ""; note = ""
                            reload()
                        }
                        is AppResult.Err -> error = r.error.userMessage
                    }
                    loading = false
                }
            }
            rows.forEach { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(jsonId(row), style = MaterialTheme.typography.labelSmall)
                        rowDataMap(row).forEach { (k, v) -> Text("$k: $v", style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = {
                            scope.launch {
                                when (val r = adminIo { api.payInstallment(jsonId(row)) }) {
                                    is AppResult.Ok -> reload()
                                    is AppResult.Err -> error = r.error.userMessage
                                }
                            }
                        }) { Text("ثبت پرداخت یک قسط") }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPresentUsersScreen(onOpen: (String) -> Unit) {
    val api = LocalAdmin.current.api
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        when (val r = adminIo { api.presentUsers() }) {
            is AppResult.Ok -> users = r.value
            is AppResult.Err -> error = r.error.userMessage
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("کاربرانی که نشست باز دارند بالاترند. برای پایه و اشتراک روی پرونده بزن.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator()
        users.forEach { u ->
            Card(onClick = { onOpen(u.userId) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text((u.profile.firstName + " " + u.profile.lastName).ifBlank { u.name }.ifBlank { u.email }.ifBlank { u.userId }, fontWeight = FontWeight.Bold)
                    Text(u.email.ifBlank { u.userId })
                    Text("پایه: ${gradeFa(u.hamyarGrade.ifBlank { u.profile.grade })} · ${BillingStatus.chipFa(u.subscription.ifBlank { u.profile.subscription })} · نشست: ${toPersianDigits(u.sessionCount.toString())}")
                }
            }
        }
    }
}

@Composable
fun AdminAuthScreen(onOpen: (String) -> Unit) {
    val api = LocalAdmin.current.api
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search() {
        scope.launch {
            loading = true
            error = null
            when (val r = adminIo { api.adminSearch(query.trim()) }) {
                is AppResult.Ok -> {
                    hits = r.value.map { p ->
                        AdminUser(
                            userId = p.userId,
                            email = p.email,
                            name = (p.firstName + " " + p.lastName).trim(),
                            hamyarGrade = p.hamyarGrade.ifBlank { p.grade },
                            subscription = p.subscription,
                            profile = p,
                        )
                    }
                }
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("پایه و اشتراک را همین‌جا مثل پرونده کاربر عوض کن. مقدار اشتراک روی سرور yearly است نه premium.")
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        OutlinedTextField(newName, { newName = it }, label = { Text("نام کاربر تازه") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(newEmail, { newEmail = it }, label = { Text("ایمیل کاربر تازه") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(newPass, { newPass = it }, label = { Text("رمز (حداقل ۸)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(
            onClick = {
                scope.launch {
                    loading = true; error = null; info = null
                    when (val r = adminIo { api.createUser(newName.trim(), newEmail.trim(), newPass) }) {
                        is AppResult.Ok -> { info = "ساخته شد: ${r.value}"; newName = ""; newEmail = ""; newPass = "" }
                        is AppResult.Err -> error = r.error.userMessage
                    }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("ساخت کاربر") }
        OutlinedTextField(query, { query = it }, label = { Text("ایمیل یا شناسه") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button(onClick = { search() }, modifier = Modifier.fillMaxWidth()) { Text("جستجو در Auth") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator()
        hits.forEach { u ->
            val uid = u.userId
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(u.name.ifBlank { u.email }.ifBlank { uid }, fontWeight = FontWeight.Bold)
                    Text(u.email.ifBlank { uid })
                    Text("اشتراک فعلی: ${BillingStatus.chipFa(u.subscription)}")
                    Text("تعویض پایه", fontWeight = FontWeight.Bold)
                    GRADE_OPTIONS.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (id, fa) ->
                                FilterChip(
                                    selected = u.hamyarGrade == id,
                                    onClick = {
                                        if (busyId != null) return@FilterChip
                                        scope.launch {
                                            busyId = uid
                                            when (val r = adminIo { api.adminSetGrade(uid, id) }) {
                                                is AppResult.Ok -> hits = hits.map { if (it.userId == uid) r.value else it }
                                                is AppResult.Err -> error = r.error.userMessage
                                            }
                                            busyId = null
                                        }
                                    },
                                    label = { Text(fa) },
                                    enabled = busyId == null,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Text("اشتراک", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (busyId != null) return@Button
                                scope.launch {
                                    busyId = uid
                                    when (val r = adminIo { api.adminSetPremium(uid, true) }) {
                                        is AppResult.Ok -> hits = hits.map { if (it.userId == uid) r.value else it }
                                        is AppResult.Err -> error = r.error.userMessage
                                    }
                                    busyId = null
                                }
                            },
                            enabled = busyId == null,
                            modifier = Modifier.weight(1f),
                        ) { Text("پرمیوم (yearly)") }
                        OutlinedButton(
                            onClick = {
                                if (busyId != null) return@OutlinedButton
                                scope.launch {
                                    busyId = uid
                                    when (val r = adminIo { api.adminSetPremium(uid, false) }) {
                                        is AppResult.Ok -> hits = hits.map { if (it.userId == uid) r.value else it }
                                        is AppResult.Err -> error = r.error.userMessage
                                    }
                                    busyId = null
                                }
                            },
                            enabled = busyId == null,
                            modifier = Modifier.weight(1f),
                        ) { Text("مهمان") }
                    }
                    TextButton(onClick = { onOpen(uid) }) { Text("پرونده کامل") }
                    TextButton(onClick = {
                        scope.launch {
                            busyId = uid
                            when (val r = adminIo { api.deleteUser(uid) }) {
                                is AppResult.Ok -> hits = hits.filterNot { it.userId == uid }
                                is AppResult.Err -> error = r.error.userMessage
                            }
                            busyId = null
                        }
                    }, enabled = busyId == null) { Text("حذف حساب") }
                }
            }
        }
    }
}


@Composable
fun AdminDatabaseScreen() {
    AdminTablesScreen()
}
