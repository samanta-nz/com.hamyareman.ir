package com.hamyareman.ir.platform.core.appwrite

import io.appwrite.exceptions.AppwriteException
import com.hamyareman.ir.platform.core.common.AppError
import java.io.IOException

/** نگاشت خطاهای Appwrite/شبکه به [AppError] تا متن فارسی و قابل‌فهم به کاربر برسد. */
internal object AppwriteErrors {

    fun map(throwable: Throwable, contextMessage: String? = null): AppError = when {
        throwable is AppwriteException -> fromAppwrite(throwable, contextMessage)
        throwable is IOException -> AppError.Network()
        throwable is SecurityException -> AppError.Permission()
        else -> AppError.Unknown(contextMessage ?: "یه چیزی درست پیش نرفت. بعداً دوباره امتحان کن.")
    }

    private fun fromAppwrite(e: AppwriteException, contextMessage: String?): AppError {
        val type = runCatching { e.type }.getOrNull().orEmpty()
        val code = runCatching { e.code }.getOrNull() ?: 0
        return when {
            code == 401 || code == 403 ||
                type.contains("user_", ignoreCase = true) ||
                type.contains("auth", ignoreCase = true) -> AppError.Auth()

            type.contains("permission", ignoreCase = true) ||
                type.contains("not_allowed", ignoreCase = true) -> AppError.Permission()

            code == 0 ||
                type.contains("network", ignoreCase = true) ||
                type.contains("device", ignoreCase = true) -> AppError.Network()

            else -> AppError.Unknown(contextMessage ?: e.message ?: "خطای ناشناخته از سرور.")
        }
    }
}
