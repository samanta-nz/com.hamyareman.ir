package com.hamyareman.ir.platform.core.appwrite

import io.appwrite.exceptions.AppwriteException
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult
import org.json.JSONObject

/** نتیجه‌ی اجرای یک تابع سرور. */
data class FunctionResult(val statusCode: Int, val body: String) {
    val isOk: Boolean get() = statusCode in 200..299
}

interface FunctionsService {
    val isConfigured: Boolean
    suspend fun call(functionId: String, body: String = "{}"): AppResult<FunctionResult>

    /** میان‌بر: فقط متن پاسخ، یا null در صورت خطا. */
    suspend fun callForBody(functionId: String, body: String = "{}"): String? =
        (call(functionId, body) as? AppResult.Ok)?.value?.body
}

class AppwriteFunctionsService(
    private val provider: AppwriteClientProvider,
) : FunctionsService {

    override val isConfigured: Boolean get() = provider.isConfigured

    override suspend fun call(functionId: String, body: String): AppResult<FunctionResult> {
        if (!provider.isConfigured) {
            return AppResult.Err(AppError.Local("تابع سرور در حالت محلی در دسترس نیست."))
        }
        // SDKی Execution.from روی نخ OkHttp کرش می‌کند اگر فیلدی مثل
        // requestMethod/resourceId تهی باشد. متن خام را می‌گیریم و خودمان می‌خوانیم.
        return try {
            val raw = provider.client.call(
                "POST",
                "/functions/$functionId/executions",
                mapOf(
                    "content-type" to "application/json",
                    "accept" to "application/json",
                    "X-Appwrite-Project" to provider.projectId,
                ),
                mapOf(
                    "body" to body,
                    "async" to false,
                ),
                String::class.java,
            )
            val json = runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrElse { JSONObject() }
            val statusCode = when (val code = json.opt("responseStatusCode")) {
                is Number -> code.toInt()
                is String -> code.toIntOrNull() ?: 200
                else -> 200
            }
            val responseBody = when (val rb = json.opt("responseBody")) {
                is String -> rb
                is JSONObject -> rb.toString()
                else -> json.optString("responseBody")
            }
            AppResult.Ok(FunctionResult(statusCode = statusCode, body = responseBody))
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            val e = t as? AppwriteException
            val type = runCatching { e?.type }.getOrNull().orEmpty()
            val code = runCatching { e?.code }.getOrNull() ?: 0
            if (code == 404 || type.contains("not_found", ignoreCase = true)) {
                AppResult.Err(AppError.Local(FUNCTION_MISSING))
            } else {
                AppResult.Err(AppwriteErrors.map(t, "اجرای تابع «$functionId» ناموفق بود."))
            }
        }
    }

    companion object {
        /**
         * پیامِ مشترکِ «تابعِ سرور مستقر نیست». روی پروژهٔ فعلی فقط `ai-companion`
         * مستقر است و بقیهٔ توابع (`notify-guardian`، `lesson-of-the-day`،
         * `user-bootstrap`، `pairing` و…) در کنسول ساخته نشده‌اند؛ سقفِ توابعِ پلن هم
         * پُر است. اپ آفلاین-اول است و همهٔ این مسیرها fallback محلی دارند، ولی پیامِ
         * خطا باید صادقانه و قابلِ فهم بماند.
         */
        const val FUNCTION_MISSING =
            "این قسمت الان سمتِ سرور فعال نیست. بقیه‌ی بخش‌ها بدونِ مشکل کار می‌کنند."
    }
}
