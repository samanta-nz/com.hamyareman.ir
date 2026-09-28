package com.hamyareman.ir.platform.core.common

/**
 * تبدیل رقم‌ها به‌صورت تابع سطح‌بسته (top-level) تا در Compose کوتاه صدا زده شود.
 *
 * چرا جدا از [JalaliDate]؟ چون هم صفحه‌های UI (زمان صفحه، دفترچه، نمودارها) و هم
 * ماژول پیوند (نرمال‌سازی کد شش‌رقمی با رقم فارسی/عربی) به آن نیاز دارند و
 * `JalaliDate.toPersianDigits` عضو یک object است. نسخه‌ی عضو هم سر جایش باقی است.
 */
fun toPersianDigits(input: String): String = JalaliDate.toPersianDigits(input)

/**
 * رقم‌های فارسی (۰-۹) و عربی (٠-٩) را به رقم لاتین تبدیل می‌کند و بقیه‌ی نویسه‌ها
 * را دست‌نخورده می‌گذارد؛ برای کد پیوندی که کاربر با کیبورد فارسی تایپ کرده.
 */
fun toLatinDigits(input: String): String = buildString(input.length) {
    input.forEach { ch ->
        when (ch) {
            in '۰'..'۹' -> append('0' + (ch - '۰'))
            in '٠'..'٩' -> append('0' + (ch - '٠'))
            else -> append(ch)
        }
    }
}
