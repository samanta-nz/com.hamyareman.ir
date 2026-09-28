package com.hamyareman.admin

import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun <T> adminIo(block: suspend () -> AppResult<T>): AppResult<T> =
    withContext(Dispatchers.IO) {
        runCatching { block() }.getOrElse { t ->
            AppResult.Err(
                AppError.Unknown(
                    t.message?.ifBlank { null } ?: "خطای داخلی: ${t.javaClass.simpleName}",
                ),
            )
        }
    }
