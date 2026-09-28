package com.hamyareman.ir.platform.feature.pairing

import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.appwrite.AuthService
import com.hamyareman.ir.platform.core.appwrite.FunctionResult
import com.hamyareman.ir.platform.core.appwrite.FunctionsService
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.FunctionIds
import com.hamyareman.ir.platform.core.common.LocalStore
import io.appwrite.Query
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.UserRole
import com.hamyareman.ir.platform.core.common.toLatinDigits
import org.json.JSONObject
import kotlin.random.Random

/** وضعیت پیوند زهرا ↔ پدر. */
data class PairingState(
    val partnerId: String? = null,
    val partnerName: String? = null,
    val linkedAt: Long? = null,
    val status: String = STATUS_NONE,
) {
    val isLinked: Boolean get() = status == STATUS_ACTIVE && partnerId != null

    companion object {
        const val STATUS_NONE = "none"
        const val STATUS_ACTIVE = "active"
        const val STATUS_REVOKED = "revoked"
    }
}

interface PairingRepository {
    val isConfigured: Boolean

    /** سمت زهرا: ساخت کد شش‌رقمی یک‌بارمصرف. */
    suspend fun createCode(): AppResult<String>

    /** سمت زهرا: باطل‌کردن کد جاری. */
    suspend fun revokeCode(): AppResult<Unit>

    /** سمت پدر: واردکردن کد و فعال‌شدن پیوند. */
    suspend fun redeemCode(code: String): AppResult<PairingState>

    /** وضعیت پیوند فعلی. */
    suspend fun currentLink(): AppResult<PairingState>

    /** قطع پیوند از هر دو سمت. */
    suspend fun unlink(): AppResult<Unit>

    /**
     * نسخه‌ی همگام (بدون suspend) از وضعیت پیوند برای UI؛ از کش محلی می‌آید
     * و تا رسیدن پاسخ سرور چیزی برای نمایش دارد.
     */
    fun cachedLink(): PairingState

    /** کد جاری که زهرا ساخته (فقط سمت زهرا معنی دارد). */
    fun cachedCode(): String?
}

/**
 * پیاده‌سازی واقعی با تابع سرور `pairing`.
 *
 * چرا تابع سرور و نه نوشتن مستقیم در جدول؟
 *  - کد باید یک‌بارمصرف و کوتاه‌عمر باشد (TTL) تا قابل حدس نباشد.
 *  - ارتقای نقش پدر به `father` یک عملیات Admin است و کلاینت هرگز نباید بتواند آن را انجام دهد.
 *  - ساخت سطر `father_links` با دسترسی‌های درست در یک تراکنش اتمی انجام می‌شود.
 *
 * اگر Appwrite پیکربندی نشده باشد، یک «پیوند محلی روی همین دستگاه» شبیه‌سازی می‌شود
 * تا جریان UI قابل تست باشد؛ این حالت در خود UI هم علامت‌گذاری می‌شود.
 */
class AppwritePairingRepository(
    private val functions: FunctionsService,
    private val tables: TablesDbService,
    private val auth: AuthService,
    private val pairingStore: LocalStore,
    private val role: UserRole,
) : PairingRepository {

    override val isConfigured: Boolean get() = tables.isConfigured

    override suspend fun createCode(): AppResult<String> {
        if (!isConfigured) return localCreateCode()
        val result = callPairing("""{"action":"create"}""")
        return when (result) {
            is AppResult.Ok -> {
                val code = result.value.json().optString("code").takeIf { it.isNotBlank() }
                    ?: return AppResult.Err(AppError.Unknown("سرور کدی برنگرداند."))
                rememberLocal("code", code)
                AppResult.Ok(code)
            }

            is AppResult.Err -> result
        }
    }

    override suspend fun revokeCode(): AppResult<Unit> {
        forgetLocal("code")
        if (!isConfigured) return AppResult.Ok(Unit)
        return when (val result = callPairing("""{"action":"revoke"}""")) {
            is AppResult.Ok -> AppResult.Ok(Unit)
            is AppResult.Err -> result
        }
    }

    override suspend fun redeemCode(code: String): AppResult<PairingState> {
        val normalized = normalizeCode(code)
        if (normalized == null) return AppResult.Err(AppError.Validation("کد باید شش رقم باشد."))
        if (!isConfigured) return localRedeem(normalized)

        val payload = JSONObject()
            .put("action", "redeem")
            .put("code", normalized)
            .toString()

        return when (val result = callPairing(payload)) {
            is AppResult.Ok -> {
                val json = result.value.json()
                val state = PairingState(
                    partnerId = json.optString("partnerId").ifBlank { json.optString("zahraId").ifBlank { null } },
                    partnerName = json.optString("partnerName").ifBlank { null },
                    linkedAt = json.optLong("linkedAt").takeIf { it > 0 } ?: System.currentTimeMillis(),
                    status = json.optString("status", PairingState.STATUS_ACTIVE),
                )
                rememberLink(state)
                AppResult.Ok(state)
            }

            is AppResult.Err -> result
        }
    }

    override suspend fun currentLink(): AppResult<PairingState> {
        if (!isConfigured) return AppResult.Ok(localLink())

        // اول سرور (منبع حقیقت)، بعد کش محلی برای نمایش فوری.
        return when (val result = tables.list(TableIds.FATHER_LINKS, listOf(Query.equal("status", "active")))) {
            is AppResult.Ok -> {
                val row = result.value.firstOrNull()
                if (row == null) AppResult.Ok(localLink()) else AppResult.Ok(fromRow(row))
            }

            is AppResult.Err -> AppResult.Ok(localLink())
        }
    }

    override suspend fun unlink(): AppResult<Unit> {
        forgetLocal("partnerId", "partnerName", "linkedAt", "status", "code")
        if (!isConfigured) return AppResult.Ok(Unit)
        return when (val result = callPairing("""{"action":"unlink"}""")) {
            is AppResult.Ok -> AppResult.Ok(Unit)
            is AppResult.Err -> result
        }
    }

    override fun cachedLink(): PairingState = localLink()

    override fun cachedCode(): String? = pairingStore.getString(KEY_CODE).takeIf { it.isNotBlank() }

    private suspend fun callPairing(body: String): AppResult<FunctionResult> =
        functions.call(FunctionIds.PAIRING, body)

    private fun fromRow(row: TableRow): PairingState = PairingState(
        partnerId = row.string("partnerId").ifBlank { row.string("zahraId").ifBlank { null } },
        partnerName = row.string("partnerName").ifBlank { null },
        linkedAt = row.long("linkedAt").takeIf { it > 0 } ?: System.currentTimeMillis(),
        status = row.string("status", PairingState.STATUS_ACTIVE),
    )

    // --- حالت محلی (بدون سرور) -------------------------------------------

    private fun localCreateCode(): AppResult<String> {
        val code = buildString {
            repeat(CODE_LENGTH) { append(Random.nextInt(10)) }
        }
        rememberLocal("code", code)
        return AppResult.Ok(code)
    }

    private fun localRedeem(code: String): AppResult<PairingState> {
        val stored = pairingStore.getString(KEY_CODE)
        if (stored.isBlank() || stored != code) {
            return AppResult.Err(AppError.Validation("این کد پیدا نشد (در حالت محلی، کد را در اپ دیگر بسازید)."))
        }
        val partnerRole = if (role == UserRole.ZAHRA) "father" else "zahra"
        val state = PairingState(
            partnerId = "local-$partnerRole",
            partnerName = partnerRole,
            linkedAt = System.currentTimeMillis(),
            status = PairingState.STATUS_ACTIVE,
        )
        rememberLink(state)
        forgetLocal(KEY_CODE) // یک‌بارمصرف
        return AppResult.Ok(state)
    }

    private fun localLink(): PairingState {
        val partnerId = pairingStore.getString(KEY_PARTNER_ID)
        if (partnerId.isBlank()) return PairingState()
        return PairingState(
            partnerId = partnerId,
            partnerName = pairingStore.getString(KEY_PARTNER_NAME).ifBlank { null },
            linkedAt = pairingStore.getLong(KEY_LINKED_AT).takeIf { it > 0 },
            status = pairingStore.getString(KEY_STATUS, PairingState.STATUS_ACTIVE),
        )
    }

    // --- ذخیره‌ی محلی ------------------------------------------------------

    private fun rememberLink(state: PairingState) {
        pairingStore.putString(KEY_PARTNER_ID, state.partnerId.orEmpty())
        pairingStore.putString(KEY_PARTNER_NAME, state.partnerName.orEmpty())
        pairingStore.putLong(KEY_LINKED_AT, state.linkedAt ?: System.currentTimeMillis())
        pairingStore.putString(KEY_STATUS, state.status)
    }

    private fun rememberLocal(key: String, value: String) = pairingStore.putString(key, value)

    private fun forgetLocal(vararg keys: String) = keys.forEach { pairingStore.remove(it) }

    companion object {
        /** نام فروشگاه مشترک پیوند؛ هر دو اپ روی یک دستگاه آن را می‌بینند. */
        const val PAIRING_STORE = "platform_pairing"
        const val KEY_CODE = "code"
        const val KEY_PARTNER_ID = "partnerId"
        const val KEY_PARTNER_NAME = "partnerName"
        const val KEY_LINKED_AT = "linkedAt"
        const val KEY_STATUS = "status"
        const val CODE_LENGTH = 6

        /** ارقام فارسی/عربی را به لاتین تبدیل می‌کند و فاصله‌ها را می‌گیرد. */
        fun normalizeCode(input: String): String? {
            val digits = toLatinDigits(input).filter { it.isDigit() }
            return digits.takeIf { it.length == CODE_LENGTH }
        }
    }
}

/** میان‌بر برای خواندن JSON از پاسخ تابع سرور. */
internal fun FunctionResult.json(): JSONObject = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
