package com.hamyareman.admin

import android.content.Context
import com.hamyareman.ir.platform.core.common.LocalStore

/**
 * پروفایل اتصال مدیر.
 * شناسه‌ها و نشانی‌ها محرمانه نیستند؛ فقط credentialها به‌شکل رمز‌شده در
 * [SecureAdminVault] می‌مانند و هیچ‌کدام در APK ساخته‌شده قرار نمی‌گیرد.
 */
class AdminPrefs(context: Context, private val store: LocalStore) {
    private val vault = SecureAdminVault(context)

    init {
        migrateObsoleteConnectionProfile()
    }

    /**
     * نسخه‌های قبلی ادمین روی دیتابیس‌های تاریخی ZahraDB/HM_BCKT و پروژهٔ FRA
     * می‌ماندند؛ حتی بعد از نصب APK جدید. فقط همان مقادیر شناخته‌شدهٔ قدیمی را
     * به محیط فعلی منتقل می‌کنیم و تنظیم سفارشی مدیر را دست نمی‌زنیم.
     */
    private fun migrateObsoleteConnectionProfile() {
        val legacyDatabases = setOf("ZahraDB", "HM_BCKT")
        val legacyProjects = setOf("6a9d59e3002751cc3ea8")
        val legacyEndpoints = setOf("https://fra.cloud.appwrite.io/v1")
        var migrated = false
        if (store.getString(KEY_DATABASE) in legacyDatabases) {
            store.putString(KEY_DATABASE, DEFAULT_APPWRITE_DATABASE)
            migrated = true
        }
        if (store.getString(KEY_PROJECT) in legacyProjects) {
            store.putString(KEY_PROJECT, DEFAULT_APPWRITE_PROJECT)
            migrated = true
        }
        if (store.getString(KEY_ENDPOINT).trimEnd('/') in legacyEndpoints) {
            store.putString(KEY_ENDPOINT, DEFAULT_APPWRITE_ENDPOINT)
            migrated = true
        }
        // با تغییر خودکار مقصد، کش کلید HTML مربوط به محیط تاریخی معتبر نیست.
        if (migrated) vault.put(SECRET_HTML_MEDIA, "")
    }

    val name: String get() = store.getString(KEY_NAME).ifBlank { "همیار من" }
    val endpoint: String get() = store.getString(KEY_ENDPOINT).ifBlank { DEFAULT_APPWRITE_ENDPOINT }
    val projectId: String get() = store.getString(KEY_PROJECT).ifBlank { DEFAULT_APPWRITE_PROJECT }
    val databaseId: String get() = store.getString(KEY_DATABASE).ifBlank { DEFAULT_APPWRITE_DATABASE }
    val bucketId: String get() = store.getString(KEY_BUCKET).ifBlank { DEFAULT_APPWRITE_BUCKET }
    val supportTableId: String get() = store.getString(KEY_SUPPORT_TABLE).ifBlank { DEFAULT_SUPPORT_TABLE }

    /** فقط از Android Keystore باز می‌شود؛ هرگز در BuildConfig قرار ندارد. */
    val apiKey: String get() = vault.get(SECRET_APPWRITE_API)
    val htmlMediaKeyB64: String get() = vault.get(SECRET_HTML_MEDIA)
    val parsPackAccessKey: String get() = vault.get(SECRET_PARSPACK_ACCESS)
    val parsPackSecretKey: String get() = vault.get(SECRET_PARSPACK_SECRET)

    val parsPackEndpoint: String get() = store.getString(KEY_PARSPACK_ENDPOINT).ifBlank { DEFAULT_PARSPACK_ENDPOINT }
    val parsPackBucket: String get() = store.getString(KEY_PARSPACK_BUCKET).ifBlank { DEFAULT_PARSPACK_BUCKET }
    val parsPackPublicBase: String get() = store.getString(KEY_PARSPACK_PUBLIC).ifBlank { DEFAULT_PARSPACK_PUBLIC_BASE }
    val parsPackRegion: String get() = store.getString(KEY_PARSPACK_REGION).ifBlank { DEFAULT_PARSPACK_REGION }
    val parsPackPrefix: String get() = store.getString(KEY_PARSPACK_PREFIX)

    val hasAppwriteCredential: Boolean get() = apiKey.isNotBlank()
    val hasParsPackCredential: Boolean get() = parsPackAccessKey.isNotBlank() && parsPackSecretKey.isNotBlank()

    fun saveAppwrite(
        name: String,
        endpoint: String,
        projectId: String,
        databaseId: String,
        bucketId: String,
        supportTableId: String,
        apiKey: String,
    ) {
        val nextEndpoint = endpoint.trim().trimEnd('/')
        val nextProject = projectId.trim()
        val nextDatabase = databaseId.trim()
        // با تغییر مقصد، cache کلیدِ پروژهٔ قبلی نباید به کار رود.
        if (nextEndpoint != this.endpoint || nextProject != this.projectId || nextDatabase != this.databaseId) {
            vault.put(SECRET_HTML_MEDIA, "")
        }
        store.putString(KEY_NAME, name.trim())
        store.putString(KEY_ENDPOINT, nextEndpoint)
        store.putString(KEY_PROJECT, nextProject)
        store.putString(KEY_DATABASE, nextDatabase)
        store.putString(KEY_BUCKET, bucketId.trim())
        store.putString(KEY_SUPPORT_TABLE, supportTableId.trim())
        vault.put(SECRET_APPWRITE_API, apiKey)
    }

    /**
     * کلید HTML از همان ردیف خصوصی `app_state/html_media_key` دانلود می‌شود.
     * این فقط کش رمز‌شدهٔ Keystore است؛ هیچ فیلد دستی برای ورود کلید نداریم.
     */
    fun cacheHtmlMediaKeyB64(value: String) {
        vault.put(SECRET_HTML_MEDIA, value)
    }

    fun saveParsPack(
        endpoint: String,
        bucket: String,
        publicBase: String,
        region: String,
        prefix: String,
        accessKey: String,
        secretKey: String,
    ) {
        store.putString(KEY_PARSPACK_ENDPOINT, endpoint.trim().trimEnd('/'))
        store.putString(KEY_PARSPACK_BUCKET, bucket.trim())
        store.putString(KEY_PARSPACK_PUBLIC, publicBase.trim().trimEnd('/'))
        store.putString(KEY_PARSPACK_REGION, region.trim())
        store.putString(KEY_PARSPACK_PREFIX, prefix.trim().trim('/'))
        vault.put(SECRET_PARSPACK_ACCESS, accessKey)
        vault.put(SECRET_PARSPACK_SECRET, secretKey)
    }

    fun clearSecrets() = vault.clearAll()

    companion object {
        /** مقصد زنده‌ای که اپ پایهٔ نهم و workflow انتشار از آن استفاده می‌کنند. */
        const val DEFAULT_APPWRITE_ENDPOINT = "https://sgp.cloud.appwrite.io/v1"
        const val DEFAULT_APPWRITE_PROJECT = "6abb134a002025222005"
        const val DEFAULT_APPWRITE_DATABASE = "6abb238d000d05730d10"
        const val DEFAULT_APPWRITE_BUCKET = "6abb564d00155cc56d65"
        const val DEFAULT_SUPPORT_TABLE = "suggestions"

        /** تنظیم تأییدشدهٔ ParsPack: virtual-host + SigV2 (نه SigV4). */
        const val DEFAULT_PARSPACK_ENDPOINT = "https://parspack.net"
        const val DEFAULT_PARSPACK_BUCKET = "c539776"
        const val DEFAULT_PARSPACK_PUBLIC_BASE = "https://c539776.parspack.net"
        const val DEFAULT_PARSPACK_REGION = "us-east-1"

        private const val KEY_NAME = "admin_server_name"
        private const val KEY_ENDPOINT = "admin_endpoint"
        private const val KEY_PROJECT = "admin_project_id"
        private const val KEY_DATABASE = "admin_database_id"
        private const val KEY_BUCKET = "admin_bucket_id"
        private const val KEY_SUPPORT_TABLE = "admin_support_table"
        private const val KEY_PARSPACK_ENDPOINT = "parspack_endpoint"
        private const val KEY_PARSPACK_BUCKET = "parspack_bucket"
        private const val KEY_PARSPACK_PUBLIC = "parspack_public_base"
        private const val KEY_PARSPACK_REGION = "parspack_region"
        private const val KEY_PARSPACK_PREFIX = "parspack_prefix"

        private const val SECRET_APPWRITE_API = "appwrite_api_key"
        private const val SECRET_HTML_MEDIA = "html_media_key_b64"
        private const val SECRET_PARSPACK_ACCESS = "parspack_access_key"
        private const val SECRET_PARSPACK_SECRET = "parspack_secret_key"
    }
}
