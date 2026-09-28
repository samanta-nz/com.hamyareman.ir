package com.hamyareman.admin

import com.hamyareman.ir.platform.core.appwrite.AdminDevice
import com.hamyareman.ir.platform.core.appwrite.AdminStats
import com.hamyareman.ir.platform.core.appwrite.AdminUser
import com.hamyareman.ir.platform.core.appwrite.BillingGateway
import com.hamyareman.ir.platform.core.appwrite.BillingOrder
import com.hamyareman.ir.platform.core.appwrite.BillingProfile
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class AdminApi(
    private val endpoint: String,
    private val projectId: String,
    private val apiKey: String,
    private val databaseId: String = "ZahraDB",
    private val bucketId: String = MEDIA_BUCKET,
) {
    val configured: Boolean get() = apiKey.isNotBlank()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

    /** Appwrite 2.2 query language is JSON objects, not limit(100). */
    private fun awQuery(method: String, vararg values: Any): String {
        val arr = JSONArray()
        values.forEach { v ->
            when (v) {
                is Int -> arr.put(v)
                is Long -> arr.put(v)
                is Boolean -> arr.put(v)
                else -> arr.put(v.toString())
            }
        }
        return JSONObject().put("method", method).put("values", arr).toString()
    }

    private suspend fun call(
        method: String,
        path: String,
        body: JSONObject? = null,
        query: Map<String, String> = emptyMap(),
        queries: List<String> = emptyList(),
        bytes: ByteArray? = null,
        mime: String? = null,
        fileName: String? = null,
        fileId: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IllegalStateException("کلید سرور در این ساخت نیست.")
        val url = StringBuilder(endpoint.trimEnd('/') + path)
        val parts = mutableListOf<String>()
        query.forEach { (k, v) ->
            parts += java.net.URLEncoder.encode(k, "UTF-8") + "=" + java.net.URLEncoder.encode(v, "UTF-8")
        }
        queries.forEach { q ->
            parts += "queries%5B%5D=" + java.net.URLEncoder.encode(q, "UTF-8")
        }
        if (parts.isNotEmpty()) url.append('?').append(parts.joinToString("&"))
        val builder = Request.Builder()
            .url(url.toString())
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .header("Accept", "application/json")
        val reqBody = when {
            bytes != null && fileName != null -> {
                val part = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("fileId", fileId ?: newId())
                    .addFormDataPart(
                        "file",
                        fileName,
                        bytes.toRequestBody((mime ?: "application/octet-stream").toMediaType()),
                    )
                    .build()
                builder.header("content-type", part.contentType().toString())
                part
            }
            method == "GET" || method == "DELETE" -> {
                if (method != "GET" && body != null) body.toString().toRequestBody(jsonType) else null
            }
            else -> (body ?: JSONObject()).toString().toRequestBody(jsonType)
        }
        builder.method(method, if (method == "GET") null else reqBody)
        val resp = http.newCall(builder.build()).execute()
        val text = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) {
            val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                ?.ifBlank { null } ?: text.take(180).ifBlank { "HTTP ${resp.code}" }
            throw IllegalStateException(msg)
        }
        if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject().put("raw", text) }
    }

    private suspend fun callBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IllegalStateException("کلید سرور را از تنظیمات اتصال بگذار.")
        val req = Request.Builder()
            .url(endpoint.trimEnd('/') + path)
            .header("X-Appwrite-Project", projectId)
            .header("X-Appwrite-Key", apiKey)
            .get()
            .build()
        val resp = http.newCall(req).execute()
        val bytes = resp.body?.bytes() ?: ByteArray(0)
        if (!resp.isSuccessful) {
            val msg = runCatching { JSONObject(String(bytes, Charsets.UTF_8)).optString("message") }.getOrNull()
            throw IllegalStateException(msg?.ifBlank { null } ?: "HTTP ${resp.code}")
        }
        bytes
    }

    private fun arr(o: JSONObject, vararg keys: String): List<JSONObject> {
        val a = keys.firstNotNullOfOrNull { k -> o.optJSONArray(k) } ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
    }

    suspend fun <T> run(block: suspend AdminApi.() -> T): AppResult<T> =
        try {
            AppResult.Ok(block())
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            AppResult.Err(AppError.Local(t.message?.ifBlank { null } ?: t.javaClass.simpleName))
        }

    fun isAdminUser(user: JSONObject): Boolean {
        val labels = user.optJSONArray("labels")
        if (labels != null) {
            for (i in 0 until labels.length()) {
                if (labels.optString(i).equals("admin", true)) return true
            }
        }
        val email = user.optString("email").lowercase()
        return email == "behzadinfo@gmail.com" || email == "aydinnz.designer@gmail.com"
    }

    suspend fun requireAdmin(userId: String): AppResult<Unit> = run {
        val u = call("GET", "/users/$userId")
        if (!isAdminUser(u)) error("این حساب ادمین نیست.")
    }

    suspend fun ping(): AppResult<String> = run {
        val health = call("GET", "/health")
        val users = call("GET", "/users", query = mapOf("limit" to "1"))
        val db = call("GET", "/tablesdb/$databaseId/tables", query = mapOf("limit" to "1"))
        "سلامت ${health.optString("status").ifBlank { "ok" }} · کاربران ${users.optInt("total")} · جدول‌ها ${db.optInt("total")}"
    }

    suspend fun getUserRaw(id: String): JSONObject = call("GET", "/users/$id")

    suspend fun listUsersRaw(limit: Int = 100): Pair<Int, List<JSONObject>> {
        val o = call("GET", "/users", query = mapOf("limit" to limit.toString()))
        return o.optInt("total") to arr(o, "users")
    }

    suspend fun listSessions(userId: String): Pair<Int, List<JSONObject>> {
        val o = call("GET", "/users/$userId/sessions")
        return o.optInt("total") to arr(o, "sessions")
    }

    suspend fun presentUsers(): AppResult<List<AdminUser>> = run {
        val (_, users) = listUsersRaw(100)
        coroutineScope {
            users.map { u ->
                async {
                    val id = u.optString("\$id").ifBlank { u.optString("id") }
                    val sessions = runCatching { listSessions(id).first }.getOrDefault(0)
                    pack(u, sessions)
                }
            }.map { it.await() }
        }.sortedByDescending { it.sessionCount }
    }

    private suspend fun profileOf(userId: String): BillingProfile {
        val o = runCatching { call("GET", "/tablesdb/$databaseId/tables/student_profiles/rows/$userId") }.getOrNull()
        return if (o == null) BillingProfile(userId = userId) else BillingGateway.parseProfile(o)
    }

    private fun devicesOf(user: JSONObject): List<AdminDevice> {
        val prefs = user.optJSONObject("prefs") ?: return emptyList()
        val raw = prefs.opt("hamyarDevices")
        val arr = when (raw) {
            is JSONArray -> raw
            is String -> runCatching { JSONArray(raw) }.getOrNull()
            else -> null
        } ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val d = arr.optJSONObject(i) ?: return@mapNotNull null
            AdminDevice(id = d.optString("id"), label = d.optString("label").ifBlank { "دستگاه" }, lastAt = d.optLong("lastAt"))
        }
    }

    private suspend fun pack(u: JSONObject, sessionCount: Int = 0, extra: Map<String, String> = emptyMap()): AdminUser {
        val id = u.optString("\$id").ifBlank { u.optString("id") }
        val labels = u.optJSONArray("labels")
        val lab = if (labels == null) emptyList() else (0 until labels.length()).map { labels.optString(it) }.filter { it.isNotBlank() }
        val blocked = u.optBoolean("status", true).not() || lab.any { it.equals("blocked", true) }
        val p = profileOf(id)
        val prefs = u.optJSONObject("prefs")
        val grade = prefs?.optString("hamyarGrade").orEmpty().ifBlank { p.grade }
        return AdminUser(
            userId = id,
            email = u.optString("email").ifBlank { p.email },
            name = u.optString("name"),
            labels = lab,
            status = u.optBoolean("status", true),
            blocked = blocked,
            hamyarGrade = grade,
            devices = devicesOf(u),
            sessionCount = sessionCount,
            subscription = p.subscription,
            profile = p.copy(userId = id, email = u.optString("email").ifBlank { p.email }, hamyarGrade = grade),
            tempPassword = extra["tempPassword"].orEmpty(),
        )
    }

    suspend fun adminUser(userId: String): AppResult<AdminUser> = run {
        val u = getUserRaw(userId)
        pack(u, listSessions(userId).first)
    }

    suspend fun adminSetGrade(userId: String, grade: String): AppResult<AdminUser> = run {
        runCatching {
            call("PATCH", "/tablesdb/$databaseId/tables/student_profiles/rows/$userId", JSONObject().put("data", JSONObject().put("grade", grade)))
        }
        val u = getUserRaw(userId)
        val labels = u.optJSONArray("labels")
        val next = mutableListOf<String>()
        if (labels != null) for (i in 0 until labels.length()) {
            val x = labels.optString(i)
            if (x.isNotBlank() && !x.startsWith("grade")) next += x
        }
        if ("zahra" !in next) next += "zahra"
        next += grade
        call("PUT", "/users/$userId/labels", JSONObject().put("labels", JSONArray(next)))
        val prefs = u.optJSONObject("prefs") ?: JSONObject()
        prefs.put("hamyarGrade", grade)
        call("PATCH", "/users/$userId/prefs", JSONObject().put("prefs", prefs))
        pack(getUserRaw(userId), listSessions(userId).first)
    }

    suspend fun adminSetPremium(userId: String, paid: Boolean): AppResult<AdminUser> =
        adminSetPlan(userId, if (paid) "yearly" else "free")

    suspend fun adminSetPlan(userId: String, plan: String): AppResult<AdminUser> = run {
        val now = System.currentTimeMillis()
        val sub = when (plan.lowercase()) {
            "monthly" -> "monthly"
            "yearly", "premium", "paid" -> "yearly"
            "installment" -> "installment"
            else -> "free"
        }
        val start = if (sub == "free") 0L else now
        val end = when (sub) {
            "monthly" -> com.hamyareman.ir.platform.core.common.BillingStatus.monthlyEndMs(now)
            "yearly", "installment" -> com.hamyareman.ir.platform.core.common.BillingStatus.yearlyEndMs(now)
            else -> 0L
        }
        runCatching {
            call(
                "PATCH",
                "/tablesdb/$databaseId/tables/student_profiles/rows/$userId",
                JSONObject().put(
                    "data",
                    JSONObject()
                        .put("subscription", sub)
                        .put("subscriptionStartMs", start)
                        .put("subscriptionEndMs", end),
                ),
            )
        }
        pack(getUserRaw(userId), listSessions(userId).first)
    }

    suspend fun adminRevokeDevice(userId: String, deviceId: String): AppResult<AdminUser> = run {
        val u = getUserRaw(userId)
        val devices = devicesOf(u).filter { it.id != deviceId }
        val prefs = u.optJSONObject("prefs") ?: JSONObject()
        val arr = JSONArray()
        devices.forEach { d ->
            arr.put(JSONObject().put("id", d.id).put("label", d.label).put("lastAt", d.lastAt))
        }
        prefs.put("hamyarDevices", arr.toString())
        call("PATCH", "/users/$userId/prefs", JSONObject().put("prefs", prefs))
        pack(getUserRaw(userId), listSessions(userId).first)
    }

    suspend fun adminClearDevices(userId: String): AppResult<AdminUser> = run {
        val u = getUserRaw(userId)
        val prefs = u.optJSONObject("prefs") ?: JSONObject()
        prefs.put("hamyarDevices", "[]")
        call("PATCH", "/users/$userId/prefs", JSONObject().put("prefs", prefs))
        pack(getUserRaw(userId), listSessions(userId).first)
    }

    suspend fun adminForceLogout(userId: String): AppResult<AdminUser> = run {
        call("DELETE", "/users/$userId/sessions")
        pack(getUserRaw(userId), 0)
    }

    suspend fun adminBlock(userId: String, block: Boolean): AppResult<AdminUser> = run {
        call("PATCH", "/users/$userId/status", JSONObject().put("status", !block))
        val u = getUserRaw(userId)
        val labels = u.optJSONArray("labels")
        val next = mutableListOf<String>()
        if (labels != null) for (i in 0 until labels.length()) {
            val x = labels.optString(i)
            if (x.isNotBlank() && !x.equals("blocked", true)) next += x
        }
        if (block) next += "blocked"
        call("PUT", "/users/$userId/labels", JSONObject().put("labels", JSONArray(next)))
        if (block) runCatching { call("DELETE", "/users/$userId/sessions") }
        pack(getUserRaw(userId), if (block) 0 else listSessions(userId).first)
    }

    suspend fun adminResetPassword(userId: String): AppResult<AdminUser> = run {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
        val pass = (1..10).map { alphabet.random() }.joinToString("")
        call("PATCH", "/users/$userId/password", JSONObject().put("password", pass))
        runCatching { call("DELETE", "/users/$userId/sessions") }
        pack(getUserRaw(userId), 0).copy(tempPassword = pass)
    }

    private fun orderFromRow(row: JSONObject): BillingOrder {
        val extra = row.opt("payload").let { p ->
            when (p) {
                is JSONObject -> p
                is String -> runCatching { JSONObject(p) }.getOrNull()
                else -> null
            }
        } ?: JSONObject()
        val merged = JSONObject()
        extra.keys().forEach { merged.put(it, extra.get(it)) }
        row.keys().forEach { k -> if (k != "payload") merged.put(k, row.get(k)) }
        if (merged.optString("id").isBlank()) merged.put("id", row.optString("\$id"))
        return BillingGateway.parseOrder(merged)
    }

    suspend fun adminList(queue: String): AppResult<List<BillingOrder>> = run {
        val status = if (queue == "refund") "refund_pending" else "pending"
        listAllOrders().let { (it as AppResult.Ok).value.filter { o -> o.status == status } }
    }

    suspend fun listAllOrders(): AppResult<List<BillingOrder>> = run {
        arr(
            call("GET", "/tablesdb/$databaseId/tables/subscription_orders/rows", queries = listOf(awQuery("limit", 100))),
            "rows",
            "documents",
        ).map { orderFromRow(it) }
    }

    suspend fun adminGet(orderId: String): AppResult<Triple<BillingOrder, BillingProfile, String>> = run {
        val row = call("GET", "/tablesdb/$databaseId/tables/subscription_orders/rows/$orderId")
        val order = orderFromRow(row)
        Triple(order, profileOf(order.userId), "")
    }

    private suspend fun patchOrder(orderId: String, status: String, extra: JSONObject) {
        call(
            "PATCH",
            "/tablesdb/$databaseId/tables/subscription_orders/rows/$orderId",
            JSONObject().put(
                "data",
                JSONObject()
                    .put("status", status)
                    .put("payload", extra.toString()),
            ),
        )
    }

    suspend fun adminApprove(orderId: String): AppResult<Unit> = run {
        val row = call("GET", "/tablesdb/$databaseId/tables/subscription_orders/rows/$orderId")
        val order = orderFromRow(row)
        val extra = JSONObject()
            .put("id", order.id)
            .put("userId", order.userId)
            .put("status", "approved")
            .put("paidAtMs", System.currentTimeMillis())
        patchOrder(orderId, "approved", extra)
        runCatching { adminSetPlan(order.userId, if (order.planId == "monthly") "monthly" else "yearly") }
    }

    suspend fun adminReject(orderId: String, note: String = ""): AppResult<Unit> = run {
        val row = call("GET", "/tablesdb/$databaseId/tables/subscription_orders/rows/$orderId")
        val order = orderFromRow(row)
        val extra = JSONObject().put("id", order.id).put("userId", order.userId).put("status", "rejected").put("adminNote", note)
        patchOrder(orderId, "rejected", extra)
        runCatching { adminSetPlan(order.userId, "free") }
    }

    suspend fun adminRefundOk(orderId: String): AppResult<Unit> = run {
        val row = call("GET", "/tablesdb/$databaseId/tables/subscription_orders/rows/$orderId")
        val order = orderFromRow(row)
        val extra = JSONObject().put("id", order.id).put("userId", order.userId).put("status", "refunded")
        patchOrder(orderId, "refunded", extra)
        runCatching { adminSetPlan(order.userId, "free") }
    }

    suspend fun adminSearch(q: String): AppResult<List<BillingProfile>> = run {
        val needle = q.trim().lowercase()
        if (needle.length < 2) error("حداقل دو نویسه بنویس.")
        val (_, users) = listUsersRaw(100)
        users.map { u ->
            val packed = pack(u)
            packed.profile.copy(
                userId = packed.userId,
                email = packed.email,
                name = packed.name,
                labels = packed.labels,
                blocked = packed.blocked,
                hamyarGrade = packed.hamyarGrade,
                deviceCount = packed.devices.size,
                subscription = packed.subscription,
            )
        }.filter { p ->
            p.email.lowercase().contains(needle) ||
                p.userId.lowercase().contains(needle) ||
                p.firstName.contains(q) || p.lastName.contains(q) || p.name.contains(q)
        }
    }

    suspend fun adminStats(): AppResult<AdminStats> = run {
        val (usersTotal, _) = listUsersRaw(1)
        val orders = arr(call("GET", "/tablesdb/$databaseId/tables/subscription_orders/rows"), "rows", "documents")
        fun n(st: String) = orders.count { it.optString("status") == st }
        val profiles = arr(call("GET", "/tablesdb/$databaseId/tables/student_profiles/rows"), "rows", "documents")
        val paid = profiles.count {
            val s = it.optString("subscription").lowercase()
            s == "premium" || s == "yearly" || s == "paid"
        }
        AdminStats(
            usersTotal = usersTotal,
            pendingPay = n("pending"),
            pendingRefund = n("refund_pending"),
            approved = n("approved"),
            refunded = n("refunded"),
            paidProfiles = paid,
        )
    }

    suspend fun listTables(): AppResult<List<Pair<String, String>>> = run {
        val o = runCatching {
            call("GET", "/tablesdb/$databaseId/tables", query = mapOf("limit" to "100"))
        }.getOrElse {
            call("GET", "/databases/$databaseId/collections", query = mapOf("limit" to "100"))
        }
        arr(o, "tables", "collections").map {
            it.optString("\$id").ifBlank { it.optString("id") } to it.optString("name")
        }.filter { it.first.isNotBlank() }
    }

    suspend fun listColumns(tableId: String): AppResult<List<String>> = run {
        val o = runCatching {
            call("GET", "/tablesdb/$databaseId/tables/$tableId/columns", query = mapOf("limit" to "100"))
        }.getOrElse {
            runCatching {
                call("GET", "/databases/$databaseId/collections/$tableId/attributes", query = mapOf("limit" to "100"))
            }.getOrNull()
        }
        val cols = if (o != null) arr(o, "columns", "attributes") else emptyList()
        val names = cols.map { it.optString("key").ifBlank { it.optString("\$id") } }.filter { it.isNotBlank() }
        if (names.isNotEmpty()) names
        else {
            val rows = arr(
                call("GET", "/tablesdb/$databaseId/tables/$tableId/rows", queries = listOf(awQuery("limit", 1))),
                "rows",
                "documents",
            )
            if (rows.isEmpty()) emptyList()
            else {
                val keys = mutableListOf<String>()
                val iter = rows.first().keys()
                while (iter.hasNext()) {
                    val k = iter.next()
                    if (!k.startsWith("$")) keys += k
                }
                keys
            }
        }
    }

    suspend fun listRows(tableId: String): AppResult<List<JSONObject>> = run {
        val out = mutableListOf<JSONObject>()
        var cursor: String? = null
        repeat(40) {
            val qs = mutableListOf(awQuery("limit", 100))
            val c = cursor
            if (!c.isNullOrBlank()) qs += awQuery("cursorAfter", c)
            val batch = runCatching {
                arr(call("GET", "/tablesdb/$databaseId/tables/$tableId/rows", queries = qs), "rows", "documents")
            }.getOrElse {
                arr(call("GET", "/databases/$databaseId/collections/$tableId/documents", queries = qs), "documents", "rows")
            }
            out += batch
            if (batch.size < 100) return@run out
            cursor = batch.last().optString("\$id").ifBlank { batch.last().optString("id") }
            if (cursor.isNullOrBlank()) return@run out
        }
        out
    }

    suspend fun getRow(tableId: String, rowId: String): AppResult<JSONObject> = run {
        call("GET", "/tablesdb/$databaseId/tables/$tableId/rows/$rowId")
    }

    suspend fun saveRow(tableId: String, rowId: String, data: JSONObject, create: Boolean): AppResult<Unit> = run {
        val body = JSONObject().put("data", data)
        if (create) {
            body.put("rowId", rowId.ifBlank { newId() })
            call("POST", "/tablesdb/$databaseId/tables/$tableId/rows", body)
        } else {
            call("PATCH", "/tablesdb/$databaseId/tables/$tableId/rows/$rowId", body)
        }
    }

    suspend fun deleteRow(tableId: String, rowId: String): AppResult<Unit> = run {
        call("DELETE", "/tablesdb/$databaseId/tables/$tableId/rows/$rowId")
    }

    suspend fun backupDatabase(): AppResult<JSONObject> = run {
        val dump = JSONObject()
            .put("databaseId", databaseId)
            .put("atMs", System.currentTimeMillis())
        val tables = JSONObject()
        listTables().let { r ->
            val list = (r as AppResult.Ok).value
            for ((id, name) in list) {
                val rows = arr(call("GET", "/tablesdb/$databaseId/tables/$id/rows"), "rows", "documents")
                val a = JSONArray()
                rows.forEach { a.put(it) }
                tables.put(id, JSONObject().put("name", name).put("rows", a))
            }
        }
        dump.put("tables", tables)
        val bytes = dump.toString().toByteArray(Charsets.UTF_8)
        val fname = "hamyar-backup-${System.currentTimeMillis()}.json"
        runCatching {
            call(
                "POST",
                "/storage/buckets/$bucketId/files",
                bytes = bytes,
                mime = "application/json",
                fileName = fname,
            )
        }
        dump.put("fileName", fname)
        dump
    }

    suspend fun restoreDatabase(dump: JSONObject): AppResult<String> = run {
        val tables = dump.optJSONObject("tables") ?: error("فایل پشتیبان جدول ندارد.")
        var n = 0
        val keys = tables.keys()
        while (keys.hasNext()) {
            val tableId = keys.next()
            val pack = tables.optJSONObject(tableId) ?: continue
            val rows = pack.optJSONArray("rows") ?: continue
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val id = row.optString("\$id").ifBlank { newId() }
                val data = JSONObject()
                row.keys().forEach { k ->
                    if (!k.startsWith("$")) data.put(k, row.get(k))
                }
                runCatching {
                    call(
                        "PATCH",
                        "/tablesdb/$databaseId/tables/$tableId/rows/$id",
                        JSONObject().put("data", data),
                    )
                }.getOrElse {
                    runCatching {
                        call(
                            "POST",
                            "/tablesdb/$databaseId/tables/$tableId/rows",
                            JSONObject().put("rowId", id).put("data", data),
                        )
                    }
                }
                n++
            }
        }
        "$n سطر بازگردانی شد."
    }

    suspend fun listBuckets(): AppResult<List<JSONObject>> = run {
        arr(call("GET", "/storage/buckets"), "buckets")
    }

    suspend fun listFiles(bucketId: String): AppResult<List<JSONObject>> = run {
        val out = mutableListOf<JSONObject>()
        var cursor: String? = null
        repeat(40) {
            val qs = mutableListOf(awQuery("limit", 100))
            val c = cursor
            if (!c.isNullOrBlank()) qs += awQuery("cursorAfter", c)
            val batch = arr(call("GET", "/storage/buckets/$bucketId/files", queries = qs), "files")
            out += batch
            if (batch.size < 100) return@run out
            cursor = batch.last().optString("\$id").ifBlank { batch.last().optString("id") }
            if (cursor.isNullOrBlank()) return@run out
        }
        out
    }

    suspend fun deleteFile(bucketId: String, fileId: String): AppResult<Unit> = run {
        call("DELETE", "/storage/buckets/$bucketId/files/$fileId")
    }

    fun fileView(bucketId: String, fileId: String): String =
        "${endpoint.trimEnd('/')}/storage/buckets/$bucketId/files/$fileId/view?project=$projectId"

    fun mediaView(fileId: String): String = if (fileId.isBlank()) "" else fileView(bucketId, fileId)

    suspend fun listFunctions(): AppResult<List<JSONObject>> = run {
        arr(call("GET", "/functions"), "functions")
    }

    suspend fun setFunctionEnabled(id: String, enabled: Boolean): AppResult<Unit> = run {
        call("PATCH", "/functions/$id", JSONObject().put("enabled", enabled))
    }

    suspend fun listExecutions(fnId: String): AppResult<List<JSONObject>> = run {
        arr(call("GET", "/functions/$fnId/executions", query = mapOf("limit" to "20")), "executions")
    }

    suspend fun executeFunction(fnId: String, body: String): AppResult<JSONObject> = run {
        call(
            "POST",
            "/functions/$fnId/executions",
            JSONObject().put("body", body).put("async", false),
        )
    }

    suspend fun createUser(name: String, email: String, password: String): AppResult<String> = run {
        val o = call(
            "POST",
            "/users",
            JSONObject()
                .put("userId", newId())
                .put("email", email.trim())
                .put("password", password)
                .put("name", name.trim()),
        )
        o.optString("\$id")
    }

    suspend fun deleteUser(userId: String): AppResult<Unit> = run {
        call("DELETE", "/users/$userId")
    }

    suspend fun listInstallments(): AppResult<List<JSONObject>> = run {
        arr(call("GET", "/tablesdb/$databaseId/tables/installments/rows"), "rows", "documents")
    }

    suspend fun createInstallment(
        userId: String,
        email: String,
        planId: String,
        total: Int,
        count: Int,
        note: String,
    ): AppResult<Unit> = run {
        val each = if (count <= 0) total else total / count
        val data = JSONObject()
            .put("userId", userId)
            .put("email", email)
            .put("planId", planId)
            .put("totalToman", total)
            .put("installmentCount", count)
            .put("paidCount", 0)
            .put("amountEach", each)
            .put("status", "active")
            .put("nextDueMs", System.currentTimeMillis())
            .put("createdAtMs", System.currentTimeMillis())
            .put("note", note)
        call("POST", "/tablesdb/$databaseId/tables/installments/rows", JSONObject().put("rowId", newId()).put("data", data))
    }

    suspend fun payInstallment(rowId: String): AppResult<Unit> = run {
        val row = call("GET", "/tablesdb/$databaseId/tables/installments/rows/$rowId")
        val paid = row.optInt("paidCount") + 1
        val total = row.optInt("installmentCount")
        val status = if (paid >= total && total > 0) "done" else "active"
        call(
            "PATCH",
            "/tablesdb/$databaseId/tables/installments/rows/$rowId",
            JSONObject().put(
                "data",
                JSONObject()
                    .put("paidCount", paid)
                    .put("status", status)
                    .put("nextDueMs", System.currentTimeMillis() + 30L * 24 * 3600 * 1000),
            ),
        )
        if (status == "done") {
            val uid = row.optString("userId")
            if (uid.isNotBlank()) {
                runCatching {
                    call(
                        "PATCH",
                        "/tablesdb/$databaseId/tables/student_profiles/rows/$uid",
                        JSONObject().put("data", JSONObject().put("subscription", "premium")),
                    )
                }
            }
        }
    }

    suspend fun downloadFile(bucketId: String, fileId: String): AppResult<ByteArray> = run {
        callBytes("/storage/buckets/$bucketId/files/$fileId/view")
    }

    suspend fun uploadFile(bucketId: String, name: String, bytes: ByteArray, mime: String): AppResult<Unit> = run {
        call(
            "POST",
            "/storage/buckets/$bucketId/files",
            bytes = bytes,
            mime = mime.ifBlank { "application/octet-stream" },
            fileName = name.ifBlank { "file" },
        )
        Unit
    }

    suspend fun adminSetLabels(userId: String, labels: List<String>): AppResult<AdminUser> = run {
        call("PUT", "/users/$userId/labels", JSONObject().put("labels", JSONArray(labels)))
        pack(getUserRaw(userId), listSessions(userId).first)
    }

    suspend fun adminUpdateName(userId: String, name: String): AppResult<AdminUser> = run {
        call("PATCH", "/users/$userId/name", JSONObject().put("name", name))
        pack(getUserRaw(userId), listSessions(userId).first)
    }

    companion object {
        const val MEDIA_BUCKET = "6aa1eaae00303400117b"
    }
}
