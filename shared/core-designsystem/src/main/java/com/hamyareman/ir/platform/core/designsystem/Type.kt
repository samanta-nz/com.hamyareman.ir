package com.hamyareman.ir.platform.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * مقیاس داشبورد (پس از +۲): h1=۲۴، h2=۱۸، body=۱۶، caption=۱۴، button=۱۶.
 * فونت از ظاهر/تنظیمات می‌آید؛ پیش‌فرض اپ دانش‌آموز بدخط است.
 */
object TypeScale {
    val h1 = 24.sp
    val h2 = 18.sp
    val body = 16.sp
    val caption = 14.sp
    val button = 16.sp
}

fun platformTypography(font: FontFamily = FontFamily.Default): Typography = Typography(
    displayLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = TypeScale.h1, lineHeight = 32.sp),
    displayMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = TypeScale.h1, lineHeight = 32.sp),
    displaySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = TypeScale.h2, lineHeight = 26.sp),
    headlineLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = TypeScale.h1, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = TypeScale.h1, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = TypeScale.h2, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = TypeScale.h2, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = TypeScale.h2, lineHeight = 26.sp),
    titleSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = TypeScale.body, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = TypeScale.body, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = TypeScale.body, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = TypeScale.caption, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = TypeScale.button, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = TypeScale.caption, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = TypeScale.caption, lineHeight = 20.sp),
)

val PlatformTypography: Typography = platformTypography()
