package com.hamyareman.ir.ui.study

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

/**
 * مطالعهٔ آزاد: یک کاتالوگ مشترک برای کتاب متنی و صوتی.
 * عنوان/فلش مستقل هر بخش حذف شده‌اند؛ بازگشت با Back سیستم انجام می‌شود.
 */
@Composable
fun FreeReadingScreen(nav: NavController, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    FreeReadingCatalogScreen()
}
