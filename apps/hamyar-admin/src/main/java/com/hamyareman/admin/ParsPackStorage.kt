package com.hamyareman.admin

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

/** شیء در باکت ParsPack (S3 سازگار، virtual-host و امضای SigV2). */
data class ParsPackObject(
    val key: String,
    val size: Long = 0,
    val modified: String = "",
)

/**
 * کلاینت مستقیم ParsPack برای دستگاه مدیر.
 *
 * Secrets از [SecureAdminVault] خوانده می‌شوند و هیچ‌وقت در URL، لاگ یا گزارش خطا
 * نمی‌آیند. این پیاده‌سازی عمداً از SigV2 استفاده می‌کند؛ نتیجهٔ preflight پروژه
 * نشان داده که باکت c539776 همان امضا را می‌پذیرد.
 */
class ParsPackStorage(private val prefs: AdminPrefs) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val configured: Boolean
        get() = prefs.hasParsPackCredential && prefs.parsPackBucket.isNotBlank() && prefs.parsPackEndpoint.startsWith("https://")

    suspend fun list(prefix: String = ""): List<ParsPackObject> = withContext(Dispatchers.IO) {
        requireConfigured()
        val target = apiRoot() + "/?prefix=" + enc(prefix) + "&max-keys=1000"
        val response = execute(signedRequest("GET", target, canonicalResource("")))
        response.use {
            val bytes = it.body?.bytes() ?: ByteArray(0)
            ensureSuccessful(it.code, bytes)
            parseList(bytes)
        }
    }

    suspend fun read(key: String): ByteArray = withContext(Dispatchers.IO) {
        requireConfigured()
        val response = execute(signedRequest("GET", objectUrl(key), canonicalResource(key)))
        response.use {
            val bytes = it.body?.bytes() ?: ByteArray(0)
            ensureSuccessful(it.code, bytes)
            bytes
        }
    }

    suspend fun upload(key: String, bytes: ByteArray, mime: String): Unit = withContext(Dispatchers.IO) {
        requireConfigured()
        require(key.isNotBlank()) { "نام/مسیر فایل خالی است." }
        require(bytes.isNotEmpty()) { "فایل خالی است." }
        val contentType = mime.ifBlank { "application/octet-stream" }
        // باکت عمومی است؛ نخست ACL عمومی را درخواست می‌کنیم و اگر ارائه‌دهنده آن را
        // نپذیرفت، همان فایل را بدون ACL می‌گذاریم (policy باکت تعیین‌کننده است).
        val first = execute(
            signedRequest(
                method = "PUT",
                url = objectUrl(key),
                canonical = canonicalResource(key),
                bytes = bytes,
                contentType = contentType,
                extraHeaders = mapOf("x-amz-acl" to "public-read"),
            ),
        )
        val firstBody = first.use { it.body?.bytes() ?: ByteArray(0) }
        if (first.code in 200..299) return@withContext
        val retry = execute(
            signedRequest(
                method = "PUT",
                url = objectUrl(key),
                canonical = canonicalResource(key),
                bytes = bytes,
                contentType = contentType,
            ),
        )
        retry.use {
            val body = it.body?.bytes() ?: ByteArray(0)
            ensureSuccessful(it.code, body, fallback = firstBody)
        }
    }

    suspend fun delete(key: String): Unit = withContext(Dispatchers.IO) {
        requireConfigured()
        require(key.isNotBlank()) { "نام/مسیر فایل خالی است." }
        val response = execute(signedRequest("DELETE", objectUrl(key), canonicalResource(key)))
        response.use {
            val body = it.body?.bytes() ?: ByteArray(0)
            ensureSuccessful(it.code, body)
        }
    }

    fun publicUrl(key: String): String = prefs.parsPackPublicBase.trimEnd('/') + "/" + encodedKey(key)

    private fun requireConfigured() {
        check(configured) { "کلیدهای ParsPack یا تنظیم باکت کامل نیستند." }
    }

    private fun apiRoot(): String {
        val endpoint = URI(prefs.parsPackEndpoint.trimEnd('/'))
        val scheme = endpoint.scheme ?: "https"
        val host = endpoint.host ?: error("نشانی ParsPack معتبر نیست.")
        val port = if (endpoint.port > 0) ":${endpoint.port}" else ""
        return "$scheme://${prefs.parsPackBucket}.$host$port"
    }

    private fun objectUrl(key: String): String = apiRoot() + "/" + encodedKey(key)

    private fun canonicalResource(key: String): String =
        if (key.isBlank()) "/${prefs.parsPackBucket}/" else "/${prefs.parsPackBucket}/$key"

    private fun signedRequest(
        method: String,
        url: String,
        canonical: String,
        bytes: ByteArray? = null,
        contentType: String = "",
        extraHeaders: Map<String, String> = emptyMap(),
    ): Request {
        val date = httpDate()
        val canonicalAmz = extraHeaders.entries
            .sortedBy { it.key.lowercase(Locale.US) }
            .joinToString(separator = "") { (k, v) -> "${k.lowercase(Locale.US)}:${v.trim()}\n" }
        val stringToSign = listOf(method, "", contentType, date).joinToString("\n") + "\n" + canonicalAmz + canonical
        val signature = hmacSha1(prefs.parsPackSecretKey, stringToSign)
        val builder = Request.Builder()
            .url(url)
            .header("Date", date)
            .header("Authorization", "AWS ${prefs.parsPackAccessKey}:$signature")
            .header("Accept", "*/*")
        extraHeaders.forEach { (key, value) -> builder.header(key, value) }
        if (contentType.isNotBlank()) builder.header("Content-Type", contentType)
        val requestBody = bytes?.toRequestBody(contentType.toMediaType())
        return builder.method(method, requestBody).build()
    }

    private fun execute(request: Request) = http.newCall(request).execute()

    private fun ensureSuccessful(code: Int, bytes: ByteArray, fallback: ByteArray = ByteArray(0)) {
        if (code in 200..299) return
        val raw = (if (bytes.isNotEmpty()) bytes else fallback).toString(Charsets.UTF_8)
        val message = raw
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(180)
        error(if (message.isBlank()) "ParsPack پاسخ HTTP $code داد." else "ParsPack: $message")
    }

    private fun parseList(bytes: ByteArray): List<ParsPackObject> {
        val out = mutableListOf<ParsPackObject>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var inside = false
        var key = ""
        var size = 0L
        var modified = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "Contents") {
                    inside = true; key = ""; size = 0; modified = ""
                }
                XmlPullParser.TEXT -> if (inside) when (parser.name) {
                    "Key" -> key += parser.text.orEmpty()
                    "Size" -> size = (parser.text ?: "0").toLongOrNull() ?: 0L
                    "LastModified" -> modified = parser.text.orEmpty()
                }
                XmlPullParser.END_TAG -> if (parser.name == "Contents") {
                    if (key.isNotBlank()) out += ParsPackObject(key, size, modified)
                    inside = false
                }
            }
            parser.next()
        }
        return out.sortedBy { it.key.lowercase(Locale.getDefault()) }
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
