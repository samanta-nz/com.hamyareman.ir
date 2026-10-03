package com.hamyareman.admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * قرارداد کوچک و عمومیِ به‌روزرسانی ادمین. Manifest روی ParsPack قرار می‌گیرد و
 * هیچ کلید یا endpoint مدیریتی در آن وجود ندارد. نصب همچنان از installer رسمی
 * Android انجام می‌شود؛ APK با همان package/signing lineage اعتبارسنجی می‌شود.
 */
data class AdminUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val sha256: String,
)

object AdminUpdateChecker {
    const val MANIFEST_URL = "https://c539776.parspack.net/apk/admin/hamyar-admin-latest.json"

    suspend fun check(): AdminUpdateInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.responseCode !in 200..299) error("سرور به‌روزرسانی HTTP ${connection.responseCode} پاسخ داد.")
            val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val code = json.optInt("versionCode", 0)
            val url = json.optString("url").trim()
            if (code <= 0 || !url.startsWith("https://")) error("manifest به‌روزرسانی معتبر نیست.")
            AdminUpdateInfo(code, json.optString("versionName").ifBlank { code.toString() }, url, json.optString("sha256"))
        } finally {
            connection.disconnect()
        }
    }

    fun hasUpdate(info: AdminUpdateInfo): Boolean = info.versionCode > BuildConfig.VERSION_CODE

    fun openDownload(context: Context, info: AdminUpdateInfo) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(info.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
