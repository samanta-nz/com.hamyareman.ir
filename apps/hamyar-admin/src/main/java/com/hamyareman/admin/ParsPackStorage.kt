package com.hamyareman.admin

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** یک شیء در باکت ParsPack (S3 سازگار، SigV2). */
data class ParsPackObject(
    val key: String,
    val size: Long = 0,
    val modified: String = "",
)

/** خروجی یک پوشه؛ [folders] از CommonPrefixes استاندارد S3 می‌آید. */
data class ParsPackListing(
    val prefix: String,
    val folders: List<String> = emptyList(),
    val objects: List<ParsPackObject> = emptyList(),
    val bytes: Long = 0,
)

/**
 * کلاینت مستقیم ParsPack با پشتیبانی از هر دو نشانی virtual-host و path-style.
 *
 * بعضی gatewayهای ParsPack listing را فقط روی یکی از این دو مسیر پاسخ می‌دهند؛
 * کلاینت هر دو را با همان امضای SigV2 امتحان می‌کند و سبک موفق را تا پایان نشست
 * نگه می‌دارد. Secrets از SecureAdminVault می‌آیند و در URL/گزارش چاپ نمی‌شوند.
 */
class ParsPackStorage(private val prefs: AdminPrefs) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    @Volatile private var pathStyle: Boolean = false

    val configured: Boolean
        get() = prefs.hasParsPackCredential && prefs.parsPackBucket.isNotBlank() && prefs.parsPackEndpoint.startsWith("https://")

    /** API سازگار پیشین برای آزمون اتصال و شمارش ساده. */
    suspend fun list(prefix: String = ""): List<ParsPackObject> = listDirectory(prefix).objects

    suspend fun listDirectory(prefix: String = ""): ParsPackListing = withContext(Dispatchers.IO) {
        requireConfigured()
        val cleanPrefix = prefix.trim().trimStart('/')
        val styles = if (pathStyle) listOf(true, false) else listOf(false, true)
        var lastFailure: Throwable? = null
        for (usePath in styles.distinct()) {
            try {
                val query = "?prefix=${enc(cleanPrefix)}&delimiter=%2F&max-keys=1000"
                val response = execute(signedRequest("GET", apiRoot(usePath) + "/" + query, canonicalResource("")))
                response.use {
                    val body = it.body?.bytes() ?: ByteArray(0)
                    ensureSuccessful(it.code, body)
                    val listing = parseListing(body, cleanPrefix)
                    pathStyle = usePath
                    return@withContext listing
                }
            } catch (t: Throwable) {
                lastFailure = t
            }
        }
        throw IllegalStateException(lastFailure?.message ?: "فهرست ParsPack دریافت نشد.")
    }

    suspend fun read(key: String): ByteArray = withContext(Dispatchers.IO) {
        requireConfigured()
        executeWithStyleFallback(method = "GET", key = key, block = { request ->
            request.use {
                val bytes = it.body?.bytes() ?: ByteArray(0)
                ensureSuccessful(it.code, bytes)
                bytes
            }
        })
    }

    suspend fun upload(key: String, bytes: ByteArray, mime: String): Unit = withContext(Dispatchers.IO) {
        requireConfigured()
        require(key.isNotBlank()) { "نام/مسیر فایل خالی است." }
        require(bytes.isNotEmpty()) { "فایل خالی است." }
        val contentType = mime.ifBlank { "application/octet-stream" }
        executeWithStyleFallback(
            method = "PUT",
            key = key,
            block = { response ->
                response.use {
                val body = it.body?.bytes() ?: ByteArray(0)
                // نخست ACL عمومی می‌خواهیم؛ اگر policy آن را رد کند همان PUT بدون ACL
                // روی همان endpoint تکرار می‌شود.
                    if (it.code in 200..299) Unit else throw IllegalStateException(body.errorText(it.code))
                }
            },
            bytes = bytes,
            contentType = contentType,
            publicAcl = true,
            retryWithoutAcl = true,
        )
    }

    suspend fun delete(key: String): Unit = withContext(Dispatchers.IO) {
        requireConfigured()
        require(key.isNotBlank()) { "نام/مسیر فایل خالی است." }
        executeWithStyleFallback(method = "DELETE", key = key, block = { response ->
            response.use {
                val body = it.body?.bytes() ?: ByteArray(0)
                ensureSuccessful(it.code, body)
                Unit
            }
        })
    }

    fun publicUrl(key: String): String = prefs.parsPackPublicBase.trimEnd('/') + "/" + encodedKey(key)

    private fun requireConfigured() {
        check(configured) { "کلیدهای ParsPack یا تنظیم باکت کامل نیستند." }
    }

    private fun apiRoot(usePath: Boolean): String {
        val endpoint = prefs.parsPackEndpoint.trimEnd('/')
        if (usePath) return "$endpoint/${prefs.parsPackBucket}"
        val uri = URI(endpoint)
        val scheme = uri.scheme ?: "https"
        val host = uri.host ?: error("نشانی ParsPack معتبر نیست.")
        val port = if (uri.port > 0) ":${uri.port}" else ""
        return "$scheme://${prefs.parsPackBucket}.$host$port"
    }

    private fun objectUrl(key: String, usePath: Boolean): String = apiRoot(usePath) + "/" + encodedKey(key)

    private fun canonicalResource(key: String): String =
        if (key.isBlank()) "/${prefs.parsPackBucket}/" else "/${prefs.parsPackBucket}/$key"

    private fun signedRequest(
        method: String,
        url: String,
        canonical: String,
        bytes: ByteArray? = null,
        contentType: String = "",
        publicAcl: Boolean = false,
    ): Request {
        val date = httpDate()
        val extraHeaders = if (publicAcl) mapOf("x-amz-acl" to "public-read") else emptyMap()
        val canonicalAmz = extraHeaders.entries
            .sortedBy { it.key.lowercase(Locale.US) }
            .joinToString(separator = "") { (k, v) -> "${k.lowercase(Locale.US)}:${v.trim()}\n" }
        val stringToSign = listOf(method, "", contentType, date).joinToString("\n") + "\n" + canonicalAmz + canonical
        val signature = hmacSha1(prefs.parsPackSecretKey, stringToSign)
        val builder = Request.Builder()
            .url(url)
            .header("Date", date)
            .header("Authorization", "AWS ${prefs.parsPackAccessKey}:$signature")
            .header("Accept", "application/xml, application/json, */*")
        extraHeaders.forEach { (name, value) -> builder.header(name, value) }
        if (contentType.isNotBlank()) builder.header("Content-Type", contentType)
        return builder.method(method, bytes?.toRequestBody(contentType.toMediaType())).build()
    }

    /** درخواست object را روی مسیر موفق اجرا می‌کند و اگر لازم شد مسیر دیگر را می‌آزماید. */
    private fun <T> executeWithStyleFallback(
        method: String,
        key: String,
        block: (okhttp3.Response) -> T,
        bytes: ByteArray? = null,
        contentType: String = "",
        publicAcl: Boolean = false,
        retryWithoutAcl: Boolean = false,
    ): T {
        var last: Throwable? = null
        for (usePath in (if (pathStyle) listOf(true, false) else listOf(false, true)).distinct()) {
            try {
                val first = execute(signedRequest(method, objectUrl(key, usePath), canonicalResource(key), bytes, contentType, publicAcl))
                try {
                    return block(first)
                } catch (failure: Throwable) {
                    if (!retryWithoutAcl || !publicAcl || method != "PUT") throw failure
                }
                val retry = execute(signedRequest(method, objectUrl(key, usePath), canonicalResource(key), bytes, contentType, publicAcl = false))
                return block(retry)
            } catch (t: Throwable) {
                last = t
            }
        }
        throw IllegalStateException(last?.message ?: "عملیات ParsPack ناموفق بود.")
    }

    private fun execute(request: Request) = http.newCall(request).execute()

    private fun ensureSuccessful(code: Int, bytes: ByteArray) {
        if (code in 200..299) return
        throw IllegalStateException(bytes.errorText(code))
    }

    private fun ByteArray.errorText(code: Int): String {
        val raw = toString(Charsets.UTF_8)
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(220)
        return if (raw.isBlank()) "ParsPack پاسخ HTTP $code داد." else "ParsPack: $raw"
    }

    private fun parseListing(bytes: ByteArray, prefix: String): ParsPackListing {
        val text = bytes.toString(Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        if (text.isBlank()) return ParsPackListing(prefix)
        return when {
            text.startsWith("<") -> parseXmlListing(text.toByteArray(Charsets.UTF_8), prefix)
            text.startsWith("{") || text.startsWith("[") -> parseJsonListing(text, prefix)
            else -> throw IllegalStateException(
                "ParsPack پاسخ فهرست S3 نداد (XML/JSON نبود). نشانی S3 یا سطح List در access key را در تنظیمات بررسی کنید.",
            )
        }
    }

    private fun parseXmlListing(bytes: ByteArray, prefix: String): ParsPackListing {
        val objects = mutableListOf<ParsPackObject>()
        val folders = linkedSetOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var currentTag = ""
        var inContents = false
        var inPrefix = false
        var key = ""
        var size = 0L
        var modified = ""
        var folder = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.orEmpty()
                    if (currentTag == "Contents") { inContents = true; key = ""; size = 0; modified = "" }
                    if (currentTag == "CommonPrefixes") { inPrefix = true; folder = "" }
                }
                XmlPullParser.TEXT -> {
                    val value = parser.text.orEmpty()
                    if (inContents) when (currentTag) {
                        "Key" -> key += value
                        "Size" -> size = value.toLongOrNull() ?: 0L
                        "LastModified" -> modified = value
                    }
                    if (inPrefix && currentTag == "Prefix") folder += value
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "Contents" -> { if (key.isNotBlank()) objects += ParsPackObject(key, size, modified); inContents = false }
                    "CommonPrefixes" -> { if (folder.isNotBlank()) folders += folder; inPrefix = false }
                }
            }
            parser.next()
        }
        return ParsPackListing(prefix, folders.toList().sorted(), objects.sortedBy { it.key.lowercase(Locale.getDefault()) }, objects.sumOf { it.size })
    }

    private fun parseJsonListing(text: String, prefix: String): ParsPackListing {
        val root = if (text.startsWith("[")) JSONObject().put("Contents", JSONArray(text)) else JSONObject(text)
        val items = root.optJSONArray("Contents") ?: root.optJSONArray("objects") ?: root.optJSONArray("items") ?: JSONArray()
        val objects = (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val key = item.optString("Key").ifBlank { item.optString("key") }.ifBlank { item.optString("name") }
            if (key.isBlank()) null else ParsPackObject(
                key = key,
                size = item.optLong("Size", item.optLong("size", 0L)),
                modified = item.optString("LastModified").ifBlank { item.optString("modified") },
            )
        }
        val prefixes = root.optJSONArray("CommonPrefixes") ?: root.optJSONArray("folders") ?: JSONArray()
        val folders = (0 until prefixes.length()).mapNotNull { index ->
            val value = prefixes.optJSONObject(index)?.optString("Prefix") ?: prefixes.optString(index)
            value.takeIf { it.isNotBlank() }
        }
        return ParsPackListing(prefix, folders.distinct().sorted(), objects.sortedBy { it.key.lowercase(Locale.getDefault()) }, objects.sumOf { it.size })
    }

    private fun httpDate(): String = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }.format(Date())

    private fun hmacSha1(secret: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return Base64.encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun encodedKey(key: String): String = key.split('/').joinToString("/") { enc(it) }
    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
