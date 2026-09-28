package com.hamyareman.ir.platform.core.common

/** نقش سمت سرور (Appwrite Labels) — هرگز فیلد قابل‌نوشتن کلاینت نیست. */
enum class UserRole(val label: String) {
    ZAHRA("zahra"),
    FATHER("father"),
    GUEST("guest");

    companion object {
        fun fromLabels(labels: Collection<String>): UserRole = when {
            labels.any { it.equals(ZAHRA.label, true) } -> ZAHRA
            labels.any { it.equals(FATHER.label, true) } -> FATHER
            else -> GUEST
        }
    }
}
