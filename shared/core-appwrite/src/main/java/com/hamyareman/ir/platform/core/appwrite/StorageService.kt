package com.hamyareman.ir.platform.core.appwrite

import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult

/** فایل آپلودشده در Storage. */
data class StoredFile(val bucketId: String, val fileId: String, val name: String) {
    /** ارجاع فشرده برای ذخیره در سطر TablesDB. */
    val reference: String get() = "$bucketId/$fileId"

    companion object {
        fun parse(reference: String): StoredFile? {
            val parts = reference.split("/")
            if (parts.size != 2 || parts.any { it.isBlank() }) return null
            return StoredFile(parts[0], parts[1], "")
        }
    }
}

interface StorageService {
    val isConfigured: Boolean
    suspend fun upload(bucketId: String, localPath: String, permissions: List<String> = emptyList(), fileId: String? = null): AppResult<StoredFile>
    suspend fun viewUrl(bucketId: String, fileId: String): String?
    suspend fun delete(bucketId: String, fileId: String): AppResult<Unit>
}

/**
 * رسانه‌های «حرف دل» (ویس/عکس/ویدیو/فایل)، آلبوم مشترک و آواتارها.
 *
 * مهم: سطل `zahra-private` هیچ دسترسی خواندنی به پدر نمی‌دهد و فقط از طرف تابع سرور
 * قابل خواندن است؛ این تنها راهی است که «حتی فنی هم نتواند» داده‌ی خصوصی را ببیند.
 */
class AppwriteStorageService(
    private val provider: AppwriteClientProvider,
) : StorageService {

    override val isConfigured: Boolean get() = provider.isConfigured

    private val storage get() = Storage(provider.client)

    override suspend fun upload(
        bucketId: String,
        localPath: String,
        permissions: List<String>,
        fileId: String?,
    ): AppResult<StoredFile> {
        if (!provider.isConfigured) {
            return AppResult.Err(AppError.Local("در حالت محلی، رسانه فقط روی همین دستگاه می‌ماند."))
        }
        return runCatching {
            val file = storage.createFile(
                bucketId = bucketId,
                fileId = fileId?.ifBlank { null } ?: ID.unique(),
                file = InputFile.fromPath(localPath),
                permissions = permissions,
            )
            AppResult.Ok(StoredFile(bucketId, file.id, file.name))
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it, "آپلود رسانه ناموفق بود.")) }
    }

    override suspend fun viewUrl(bucketId: String, fileId: String): String? {
        if (!provider.isConfigured) return null
        return runCatching {
            storage.getFileView(bucketId = bucketId, fileId = fileId).toString()
        }.getOrNull()
    }

    override suspend fun delete(bucketId: String, fileId: String): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Ok(Unit)
        return runCatching {
            storage.deleteFile(bucketId = bucketId, fileId = fileId)
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(AppwriteErrors.map(it)) }
    }
}
