package com.hamyareman.ir.ui.profile

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.StorageService
import com.hamyareman.ir.ui.study.StudyMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** عکس پروفایل: فایل محلی + همان نام در باکت عمومی درس‌ها تا بین دستگاه‌ها سینک شود. */
object AvatarSync {
    fun localFile(ctx: Context): File = File(ctx.filesDir, "avatar.jpg")

    fun fileId(userId: String): String {
        val id = userId.filter { it.isLetterOrDigit() }.take(20)
        return if (id.isBlank()) "" else "avt-$id"
    }

    suspend fun pull(ctx: Context, userId: String) {
        if (userId.isBlank()) return
        val dest = localFile(ctx)
        if (dest.exists() && dest.length() > 400) {
            StudentProfileState.saveAvatarMirror(ctx, dest.absolutePath)
            return
        }
        val fid = fileId(userId)
        if (fid.isBlank()) return
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(StudyMedia.viewUrl(fid)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 20000
                    instanceFollowRedirects = true
                }
                conn.connect()
                if (conn.responseCode !in 200..299) {
                    conn.disconnect()
                    return@runCatching
                }
                dest.outputStream().use { out -> conn.inputStream.copyTo(out) }
                conn.disconnect()
            }
        }
        if (dest.exists() && dest.length() > 400) {
            StudentProfileState.saveAvatarMirror(ctx, dest.absolutePath)
        }
    }

    suspend fun push(ctx: Context, storage: StorageService, userId: String) {
        val src = localFile(ctx)
        if (!src.exists() || src.length() < 400 || userId.isBlank() || !storage.isConfigured) return
        val fid = fileId(userId)
        if (fid.isBlank()) return
        withContext(Dispatchers.IO) {
            runCatching { storage.delete(StudyMedia.BUCKET, fid) }
            storage.upload(StudyMedia.BUCKET, src.absolutePath, emptyList(), fid)
        }
    }
}
