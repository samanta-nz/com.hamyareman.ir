package com.hamyareman.ir.platform.core.appwrite

/**
 * پرمیشن‌های سطح ردیف: فقط صاحبِ سطر می‌خواند و ویرایش می‌کند —
 * پایه‌ی «دسترسی به اطلاعات شخصی خود» بدون واسطه.
 */
object RowPermissions {
    fun forUser(userId: String): List<String> {
        val role = io.appwrite.Role.user(userId)
        return listOf(
            io.appwrite.Permission.read(role),
            io.appwrite.Permission.update(role),
            io.appwrite.Permission.write(role),
        )
    }
}
