package com.hamyareman.admin

import com.hamyareman.ir.platform.core.common.LocalStore

/** اتصال سرور — قابل تغییر از تنظیمات، بدون ساخت دوباره. */
class AdminPrefs(private val store: LocalStore) {

    val name: String get() = store.getString(KEY_NAME).ifBlank { "همیار من" }
    val endpoint: String get() = store.getString(KEY_ENDPOINT).ifBlank { BuildConfig.APPWRITE_ENDPOINT }
    val projectId: String get() = store.getString(KEY_PROJECT).ifBlank { BuildConfig.APPWRITE_PROJECT_ID }
    val databaseId: String get() = store.getString(KEY_DATABASE).ifBlank { BuildConfig.APPWRITE_DATABASE_ID }
    val apiKey: String get() = store.getString(KEY_API).ifBlank { BuildConfig.APPWRITE_API_KEY }
    val bucketId: String get() = store.getString(KEY_BUCKET).ifBlank { DEFAULT_BUCKET }

    fun save(
        name: String,
        endpoint: String,
        projectId: String,
        databaseId: String,
        apiKey: String,
        bucketId: String,
    ) {
        store.putString(KEY_NAME, name.trim())
        store.putString(KEY_ENDPOINT, endpoint.trim().trimEnd('/'))
        store.putString(KEY_PROJECT, projectId.trim())
        store.putString(KEY_DATABASE, databaseId.trim())
        store.putString(KEY_API, apiKey.trim())
        store.putString(KEY_BUCKET, bucketId.trim())
    }

    companion object {
        const val DEFAULT_BUCKET = "6aa1eaae00303400117b"
        private const val KEY_NAME = "admin_server_name"
        private const val KEY_ENDPOINT = "admin_endpoint"
        private const val KEY_PROJECT = "admin_project_id"
        private const val KEY_DATABASE = "admin_database_id"
        private const val KEY_API = "admin_api_key"
        private const val KEY_BUCKET = "admin_bucket_id"
    }
}
