package com.hamyareman.ir.util

object CrisisKeywordDetector {
    private val keys = listOf("خودکشی", "می‌خوام بمیرم", "کاش نبودم", "آسیب به خود")
    fun detect(text: String): Boolean = keys.any { text.contains(it) }
}
