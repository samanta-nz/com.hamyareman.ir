package com.hamyareman.ir.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hamyareman.ir.BuildConfig
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * خواندنِ تنظیماتِ آپدیت از سرور + کشِ محلی.
 *
 * چرا کش و TTL: اپ آفلاین-اول است. اگر اینترنت نبود، همان آخرین ردیفِ
 * خوانده‌شده تصمیم می‌گیرد و **هیچ‌وقت** بخاطرِ نبودِ اینترنت کاربر بیرون از اپ
 * قفل نمی‌شود (آپدیتِ اجباری فقط وقتی معنا دارد که ردیفِ تازه خوانده شده باشد).
 */
object UpdateChecker {

    private const val PREF = "hamyar_update"
    private const val KEY_PAYLOAD = "release_payload"
    private const val KEY_AT = "release_at"
    private const val KEY_SKIP = "skip_version"
    private const val KEY_INSTALL = "install_id"

    /** عمرِ کش: شش ساعت. */
    const val TTL_MS = 6L * 60L * 60L * 1000L

    private fun store(ctx: Context) = LocalStore(ctx, PREF)

    /** آخرین تنظیماتِ ذخیره‌شده (بی‌شبکه). */
    fun cached(ctx: Context): UpdateInfo? = store(ctx)
        .getString(KEY_PAYLOAD)
        .takeIf { it.isNotBlank() }
        ?.let { UpdatePlan.parse(it) }
        ?.takeIf { UpdatePlan.isCompatible(it, ctx.packageName, BuildConfig.GRADE_ID) }

    fun fetchedAt(ctx: Context): Long = store(ctx).getLong(KEY_AT, 0L)

    fun isFresh(ctx: Context): Boolean = System.currentTimeMillis() - fetchedAt(ctx) < TTL_MS

    /** نسخه‌ای که کاربر «بعداً» زد؛ تا نسخهٔ بعدی دوباره پرسیده نمی‌شود. */
    fun skipVersion(ctx: Context): Int = store(ctx).getInt(KEY_SKIP, 0)

    fun skip(ctx: Context, latest: Int) {
        store(ctx).putInt(KEY_SKIP, latest)
    }

    /** شناسهٔ تصادفیِ این نصب — پایهٔ رول‌آوتِ تدریجی (پایدار، بی‌ارتباط با هویت). */
    fun installId(ctx: Context): String {
        val s = store(ctx).getString(KEY_INSTALL)
        if (s.isNotBlank()) return s
        val id = UUID.randomUUID().toString()
        store(ctx).putString(KEY_INSTALL, id)
        return id
    }

    /**
     * خواندنِ ردیفِ `app_release` از `app_state`.
     * در خطا/آفلاین/نبودِ ردیف، مقدارِ کش‌شده برمی‌گردد (اگر باشد).
     */
    suspend fun refresh(ctx: Context, tables: TablesDbService): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!tables.isConfigured) return@withContext cached(ctx)
        when (val r = tables.get(TableIds.APP_STATE, UpdatePlan.ROW_ID)) {
            is AppResult.Ok -> {
                val body = r.value?.string("payload").orEmpty()
                if (body.isBlank()) {
                    cached(ctx)
                } else {
                    val parsed = UpdatePlan.parse(body)
                    if (!UpdatePlan.isCompatible(parsed, ctx.packageName, BuildConfig.GRADE_ID)) {
                        cached(ctx)
                    } else {
                        store(ctx).putString(KEY_PAYLOAD, body)
                        store(ctx).putLong(KEY_AT, System.currentTimeMillis())
                        parsed
                    }
                }
            }
            is AppResult.Err -> cached(ctx)
        }
    }

    /**
     * تصمیمِ نهایی برای این نصب (با رعایتِ «بعداً» و رول‌آوت).
     * [forceNetwork] = هر اجرای کامل اپ باید از سرور بپرسد (نه از کشِ ۶ساعته).
     */
    suspend fun decide(
        ctx: Context,
        tables: TablesDbService,
        current: Int,
        forceNetwork: Boolean = false,
    ): UpdateDecision {
        val info = if (!forceNetwork && isFresh(ctx)) {
            cached(ctx) ?: refresh(ctx, tables)
        } else {
            refresh(ctx, tables) ?: cached(ctx)
        }
        val ready = info ?: return UpdateDecision.None
        val bucket = UpdatePlan.rolloutBucket(installId(ctx))
        return when (val d = UpdatePlan.decisionFor(current, ready, bucket)) {
            is UpdateDecision.Optional ->
                if (skipVersion(ctx) >= ready.latest) UpdateDecision.None else d
            else -> d
        }
    }

    /** بازکردنِ نشانیِ دانلود در مرورگر — راهِ پشتیبانِ همیشه‌کار. */
    fun openInBrowser(ctx: Context, url: String) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
