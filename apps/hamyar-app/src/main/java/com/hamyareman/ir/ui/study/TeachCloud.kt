package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.AuthService
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * سینک ابری «آمار تدریس» — تکمیل‌کننده‌ی ثبتِ محلیِ غیرقابل‌ویرایش:
 *  - آمار هر پک یک سطر «تدریس_<userId>» دارد که آخرین وضعیت JSON آن upsert می‌شود؛
 *  - صف از [SyncEngine] می‌آید (آفلاین‌محور: اول محلی، بعد ارسال)؛
 *  - جدول teach_stats rowSecurity دارد → هر کاربر فقط سطر خودش را می‌بیند/می‌نویسد؛
 *  - آمار «غیرقابل ویرایش» از دید کاربر می‌ماند: هیچ UI ویرایشی وجود ندارد و
 *    کلیدها فقط با رویدادهای واقعی پلیر به‌روز می‌شوند.
 */
object TeachCloud {

    const val TABLE = "teach_stats"
    private const val KEY_LAST = "teach_cloud_last_sync"

    fun rowIdFor(userId: String, packId: String) = "teach_${userId}_$packId"

    /** صف‌کردن وضعیت فعلی یک پک (بعد از هر ثبت رویداد یا هر نشست). */
    fun enqueue(ctx: Context, sync: com.hamyareman.ir.platform.core.sync.SyncEngine?, userId: String?, packId: String) {
        if (sync == null || userId.isNullOrBlank()) return
        val snap = TeachStats.raw(ctx, packId)
        if (snap.length() == 0) return
        sync.enqueue(
            table = TABLE,
            rowId = rowIdFor(userId, packId),
            payload = mapOf(
                "userId" to userId,
                "packId" to packId,
                "stats" to snap.toString(),
                "updatedAtIso" to java.time.Instant.now().toString(),
                "lastSessionIso" to (if (snap.optLong("st") > 0) java.time.Instant.ofEpochMilli(snap.optLong("st")).toString() else ""),
            ),
        )
    }

    /** صف‌کردنِ همه‌ی پک‌هایی که آمار محلی دارند (برای نمودار پیشرفت). */
    fun enqueueAll(ctx: Context, sync: com.hamyareman.ir.platform.core.sync.SyncEngine?, userId: String?, packIds: List<String> = emptyList()) {
        if (sync == null || userId.isNullOrBlank()) return
        val ids = packIds.ifEmpty { TeachStats.allPackIds(ctx) }
        ids.forEach { packId -> enqueue(ctx, sync, userId, packId) }
    }

    /** ارسال صف (اوت‌باکس) — سبک و بی‌دردسر؛ خطا بی‌صدا برای دفعه‌ی بعد می‌ماند. */
    suspend fun push(sync: com.hamyareman.ir.platform.core.sync.SyncEngine?): Int =
        withContext(Dispatchers.IO) {
            sync ?: return@withContext 0
            val r = sync.pushAll()
            r.pushed
        }

    fun lastSyncAt(ctx: Context): Long = LocalStore(ctx, "hamyar_teach").getLong(KEY_LAST)
    fun markSynced(ctx: Context) {
        LocalStore(ctx, "hamyar_teach").putLong(KEY_LAST, System.currentTimeMillis())
    }
}
