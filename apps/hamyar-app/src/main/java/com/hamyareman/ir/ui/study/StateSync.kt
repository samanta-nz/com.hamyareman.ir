package com.hamyareman.ir.ui.study

import android.content.Context
import com.hamyareman.ir.platform.core.appwrite.AppwriteClientProvider
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * سینکِ عمومیِ «وضعیتِ برنامه» روی جدول `app_state`.
 *
 * هر کلید (مثلاً برنامهٔ هفتگی، تنظیمات شیفت، کلاس مجازی، تیک‌های روزانه)
 * یک سطر با rowId قطعی دارد. آخرین نوشته برنده است (`updatedAt` بزرگ‌تر).
 * اگر سرور در دسترس نباشد، هیچ استثنایی بالا نمی‌آید و برنامه روی همان
 * نسخهٔ محلی ادامه می‌دهد.
 */
object StateSync {

    const val TABLE = com.hamyareman.ir.platform.core.common.TableIds.APP_STATE

    // --- کلیدها ---
    const val KEY_WEEK = "class_plan_week"
    const val KEY_SHIFT = "class_plan_shift"
    const val KEY_VIRTUAL = "virtual_class"
    const val KEY_CHECKS = "daily_checks"
    const val KEY_LEAVES = "class_plan_leaves"
    const val KEY_NOTE_TITLES = "note_titles"
    /** برنامهٔ شخصی هفتگی (جدول زمانی جدا از برنامهٔ کلاسی). */
    const val KEY_WEEK_PLAN = "week_plan"

    private const val PREF = "hamyar_state_sync"
    private const val KEY_LAST = "last_sync_at"

    /**
     * شناسهٔ قطعیِ سطر.
     *
     * Appwrite برای `documentId` حداکثر **۳۶ کاراکتر** قبول می‌کند و باید با حرف یا
     * رقم شروع شود؛ الگوی قبلی (`state_<uid>_<key>`) برای uidهای ۲۰ کاراکتری هم
     * ۴۱ کاراکتر می‌شد ⇒ سرور هر push/pull را با `general_argument_invalid` رد
     * می‌کرد و سینکِ برنامهٔ هفتگی/تیک‌ها/مرخصی بی‌صدا شکست می‌خورد.
     * حالا شناسه از هشِ SHA-256ِ «uid|key» ساخته می‌شود: قطعی، یکتا و همیشه
     * ۳۴ کاراکتر (`st` + ۳۲ رقمِ شانزده‌دهی).
     */
    fun rowId(uid: String, key: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest("$uid|$key".toByteArray(Charsets.UTF_8))
        val hex = StringBuilder(34).append("st")
        digest.forEach { hex.append("%02x".format(it.toInt() and 0xFF)) }
        return hex.substring(0, 34)
    }

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    /** آخرین زمانِ سینک موفق (برای نمایش در UI). */
    fun lastSyncAt(ctx: Context): Long = store(ctx).getLong(KEY_LAST, 0L)

    /** ذخیره روی سرور (نسخهٔ محلی هم به‌روز می‌شود). */
    suspend fun push(
        ctx: Context,
        tables: TablesDbService,
        uid: String,
        key: String,
        payload: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext false
        val now = System.currentTimeMillis()
        // snapshot محلی پیش از تماس شبکه نگه داشته می‌شود؛ قطع شبکه نباید تغییرات را از Sync cache حذف کند.
        store(ctx).putString("local_$key", payload)
        store(ctx).putLong("at_$key", now)
        val res = tables.upsert(
            TABLE,
            rowId(uid, key),
            mapOf(
                "userId" to uid,
                "key" to key,
                "payload" to payload,
                "updatedAt" to now,
            ),
            AppwriteClientProvider.ownerOnly(uid),
        )
        val ok = res is AppResult.Ok
        if (ok) {
            store(ctx).putLong(KEY_LAST, now)
            store(ctx).remove("err_$key")
        } else {
            // تا پیش از این، شکستِ سینک کاملاً بی‌صدا بود («سینک درست نمی‌شود»).
            noteError(ctx, key, res)
        }
        ok
    }

    /** ثبتِ دلیلِ آخرین شکستِ سینکِ یک کلید تا در UI دیده و گزارش شود. */
    private fun noteError(ctx: Context, key: String, res: AppResult<*>?) {
        val msg = (res as? AppResult.Err)?.error?.userMessage ?: "سرور پاسخ نداد"
        store(ctx).putString("err_$key", msg)
        store(ctx).putLong("err_at_$key", System.currentTimeMillis())
    }

    /** آخرین خطای سینکِ [key]؛ `null` یعنی آخرین تلاش موفق بوده. */
    fun lastError(ctx: Context, key: String): String? =
        store(ctx).getString("err_$key").takeIf { it.isNotBlank() }

    /**
     * خواندن از سرور؛ اگر سطر نبود → null. مقدارِ برگشتی = payload و updatedAt.
     */
    suspend fun pull(
        ctx: Context,
        tables: TablesDbService,
        uid: String,
        key: String,
    ): Pair<String, Long>? = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext null
        when (val r = tables.get(TABLE, rowId(uid, key))) {
            is AppResult.Ok -> {
                store(ctx).remove("err_$key")
                val row = r.value ?: return@withContext null
                val payload = row.string("payload")
                if (payload.isBlank()) return@withContext null
                payload to row.long("updatedAt")
            }
            is AppResult.Err -> {
                noteError(ctx, key, r)
                null
            }
        }
    }

    /**
     * یکی‌کردن: هر طرف جدیدتر بود برنده می‌شود و نتیجه روی هر دو طرف نوشته می‌شود.
     * [localAt] زمانِ آخرین تغییرِ محلی است.
     */
    suspend fun merge(
        ctx: Context,
        tables: TablesDbService,
        uid: String,
        key: String,
        localPayload: String,
        localAt: Long,
        onRemoteNewer: (String) -> Unit,
    ) {
        if (uid.isBlank()) return
        val remote = pull(ctx, tables, uid, key)
        if (remote == null) {
            push(ctx, tables, uid, key, localPayload)
            return
        }
        val (remotePayload, remoteAt) = remote
        if (remoteAt > localAt && remotePayload != localPayload) {
            onRemoteNewer(remotePayload)
        } else if (localAt >= remoteAt && remotePayload != localPayload) {
            push(ctx, tables, uid, key, localPayload)
        }
    }

    fun localAt(ctx: Context, key: String): Long = store(ctx).getLong("at_$key", 0L)

    /**
     * «تغییرِ محلی» بدونِ push: زمانِ محلیِ کلید جلو می‌رود تا pullِ بعدی نسخهٔ
     * کهنه‌ی سرور را جای دادهٔ تازهٔ دستگاه نگذارد (باگِ قبلی: تیک‌ها و برنامهٔ
     * هفتگی با اولین pull پاک می‌شدند).
     */
    fun markLocal(ctx: Context, key: String) {
        store(ctx).putLong("at_$key", System.currentTimeMillis())
    }

    /** بعد از اِعمالِ نسخهٔ سرور، زمانِ محلی روی زمانِ همان نسخه می‌نشیند. */
    fun markSyncedAt(ctx: Context, key: String, at: Long) {
        if (at > 0) store(ctx).putLong("at_$key", at)
    }
}

/**
 * هماهنگ‌کردنِ تنظیماتِ برنامهٔ کلاسی با سرور — هر کلید یک سطر در `app_state`.
 */
object ClassPlanSync {

    val keys = listOf(
        StateSync.KEY_WEEK,
        StateSync.KEY_SHIFT,
        StateSync.KEY_VIRTUAL,
        StateSync.KEY_CHECKS,
        StateSync.KEY_LEAVES,
    )

    /** کشیدنِ همهٔ کلیدها از سرور (اگر سرور جدیدتر باشد روی دستگاه اعمال می‌شود). */
    suspend fun pullAll(
        ctx: android.content.Context,
        tables: com.hamyareman.ir.platform.core.appwrite.TablesDbService,
        uid: String,
    ): Boolean {
        if (uid.isBlank()) return false
        var any = false
        keys.forEach { key ->
            val remote = StateSync.pull(ctx, tables, uid, key)
            if (remote != null) {
                val local = ClassPlanStore.exportState(ctx, key)
                if (remote.second >= StateSync.localAt(ctx, key) && remote.first != local) {
                    ClassPlanStore.importState(ctx, key, remote.first)
                    // زمانِ محلی = زمانِ سرور؛ وگرنه pushِ بعدی بی‌دلیل تکرار می‌شود.
                    StateSync.markSyncedAt(ctx, key, remote.second)
                    any = true
                }
            }
        }
        return any
    }

    /** کشیدنِ یک کلید از سرور (اگر سرور جدیدتر باشد روی دستگاه اعمال می‌شود). */
    suspend fun pull(
        ctx: android.content.Context,
        tables: com.hamyareman.ir.platform.core.appwrite.TablesDbService,
        uid: String,
        key: String,
    ): Boolean {
        if (uid.isBlank()) return false
        val remote = StateSync.pull(ctx, tables, uid, key) ?: return false
        val local = ClassPlanStore.exportState(ctx, key)
        if (remote.second < StateSync.localAt(ctx, key) || remote.first == local) return false
        ClassPlanStore.importState(ctx, key, remote.first)
        StateSync.markSyncedAt(ctx, key, remote.second)
        return true
    }

    /** فرستادنِ یک کلید به سرور (بعد از هر تغییرِ محلی). */
    suspend fun push(
        ctx: android.content.Context,
        tables: com.hamyareman.ir.platform.core.appwrite.TablesDbService,
        uid: String,
        key: String,
    ): Boolean {
        if (uid.isBlank()) return false
        return StateSync.push(ctx, tables, uid, key, ClassPlanStore.exportState(ctx, key))
    }

    /**
     * push فقط وقتی نسخهٔ محلی از سرور تازه‌تر باشد. اگر سرور تازه‌تر است، همان
     * نسخه روی دستگاه اعمال می‌شود (آخرین نوشته برنده) — این همان چیزی است که
     * «سینک درست» یعنی: نه دادهٔ سرور پاک می‌شود، نه تغییرِ محلی از دست می‌رود.
     */
    suspend fun pushIfNewer(
        ctx: android.content.Context,
        tables: com.hamyareman.ir.platform.core.appwrite.TablesDbService,
        uid: String,
        key: String,
    ): Boolean {
        if (uid.isBlank()) return false
        val local = ClassPlanStore.exportState(ctx, key)
        val remote = StateSync.pull(ctx, tables, uid, key)
        if (remote == null) return StateSync.push(ctx, tables, uid, key, local)
        if (remote.first == local) {
            StateSync.markSyncedAt(ctx, key, remote.second)
            return false
        }
        return if (StateSync.localAt(ctx, key) > remote.second) {
            StateSync.push(ctx, tables, uid, key, local)
        } else {
            ClassPlanStore.importState(ctx, key, remote.first)
            StateSync.markSyncedAt(ctx, key, remote.second)
            false
        }
    }

    suspend fun pushAll(
        ctx: android.content.Context,
        tables: com.hamyareman.ir.platform.core.appwrite.TablesDbService,
        uid: String,
    ): Boolean {
        if (uid.isBlank()) return false
        var ok = true
        keys.forEach { key -> ok = ok && push(ctx, tables, uid, key) }
        return ok
    }
}
