package com.hamyareman.ir.platform.core.common

sealed class AppError(open val userMessage: String) {
    data class Network(override val userMessage: String = "الان اینترنت در دسترس نیست. چند دقیقه‌ی دیگه دوباره امتحان کن 🙂") : AppError(userMessage)
    data class Auth(override val userMessage: String = "برای ادامه باید وارد حساب بشی.") : AppError(userMessage)
    data class Permission(override val userMessage: String = "این بخش برای این حساب در دسترس نیست.") : AppError(userMessage)
    /** ورودی کاربر معتبر نیست (PIN، کد پیوند، مقدار آب و…). */
    data class Validation(override val userMessage: String) : AppError(userMessage)
    data class Local(override val userMessage: String) : AppError(userMessage)
    data class Unknown(override val userMessage: String = "یه چیزی درست پیش نرفت. بعداً دوباره امتحان کن.") : AppError(userMessage)
}

sealed class AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>()
    data class Err(val error: AppError) : AppResult<Nothing>()
}
