package com.hamyareman.ir.ui.profile

import com.hamyareman.ir.BuildConfig

/**
 * هویت این APK — از flavor پایه می‌آید، نه از انتخاب کاربر.
 * نهم: `com.hamyareman.ir` (نصب‌های فعلی). بقیه: `com.hamyareman.p04` … `p12`.
 */
object AppEdition {
    val grade: GradeLevel get() = GradeLevel.byId(BuildConfig.GRADE_ID)
    val faNumeral: String get() = BuildConfig.GRADE_FA_NUMERAL
    val booksFolder: String get() = BuildConfig.BOOKS_FOLDER
    val faShort: String get() = BuildConfig.GRADE_FA_SHORT
    val gradeFa: String get() = grade.fa
    val appTitle: String
        get() = if (grade == GradeLevel.G9) "همیار من" else "همیار $faShort"
}
