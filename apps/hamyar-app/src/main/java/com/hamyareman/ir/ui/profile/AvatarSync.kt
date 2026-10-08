package com.hamyareman.ir.ui.profile

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.StorageService
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.BucketIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** همگام‌سازی تصویر پروفایل بین نصب‌های کاربر. */
object AvatarSync {
    private const val PREF = "hamyar_avatar_sync"
    private const val KEY_DIRTY = "dirty"

    fun localFile(ctx: Context): File = File(ctx.filesDir, "avatar.jpg")

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    fun fileId(userId: String): String {
        val id = userId.filter { it.isLetterOrDigit() }.take(20)
        return if (id.isBlank()) "" else "avt-$id"
    }

    /** وقتی کاربر عکس جدید را محلی ذخیره کرد، قبل از هر pull باید dirty بماند. */
    fun markDirty(ctx: Context) {
        store(ctx).putBool(KEY_DIRTY, true)
    }

    private fun clearDirty(ctx: Context) {
        store(ctx).putBool(KEY_DIRTY, false)
    }

    fun isDirty(ctx: Context): Boolean = store(ctx).getBool(KEY_DIRTY, false)

    suspend fun pull(ctx: Context, storage: StorageService, userId: String): Boolean {
        if (userId.isBlank() || !storage.isConfigured) return false
        // عکس محلیِ تازه هنوز باید فرصت push داشته باشد؛ pull نسخهٔ قدیمی سرور را روی آن ننویسد.
        if (isDirty(ctx)) return false

        val fid = fileId(userId)
        if (fid.isBlank()) return false

        return withContext(Dispatchers.IO) {
            when (val result = storage.download(BucketIds.AVATARS, fid)) {
                is AppResult.Ok -> {
                    val tmp = File(ctx.filesDir, ".avatar.download.tmp")
                    runCatching {
                        tmp.outputStream().use { it.write(result.value) }
                        if (tmp.length() < 400L) error("avatar payload too small")
                        val dest = localFile(ctx)
                        if (dest.exists()) dest.delete()
                        if (!tmp.renameTo(dest)) {
                            tmp.copyTo(dest, overwrite = true)
                            tmp.delete()
                        }
                        StudentProfileState.saveAvatarMirror(ctx, dest.absolutePath)
                        true
                    }.getOrElse {
                        tmp.delete()
                        false
                    }
                }
                is AppResult.Err -> false
            }
        }
    }

    suspend fun push(ctx: Context, storage: StorageService, userId: String): Boolean {
        val src = localFile(ctx)
        if (!src.exists() || src.length() < 400 || userId.isBlank() || !storage.isConfigured) return false
        val fid = fileId(userId)
        if (fid.isBlank()) return false

        val result = withContext(Dispatchers.IO) {
            runCatching { storage.delete(BucketIds.AVATARS, fid) }
            storage.upload(
                bucketId = BucketIds.AVATARS,
                localPath = src.absolutePath,
                permissions = AppwriteClientProvider.ownerOnly(userId),
                fileId = fid,
            )
        }
        val ok = result is AppResult.Ok
        if (ok) clearDirty(ctx)
        return ok
    }
}
