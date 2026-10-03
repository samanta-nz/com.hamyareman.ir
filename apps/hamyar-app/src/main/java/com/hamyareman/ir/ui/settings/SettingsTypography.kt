package com.hamyareman.ir.ui.settings

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R

private val SettingsVazirmatnLight = FontFamily(Font(R.font.vazirmatn_light, FontWeight.Light))

/**
 * تایپوگرافی محلی صفحات تنظیمات؛ نقش‌های Material همان نقش‌ها می‌مانند تا با
 * پروفایل و دیگر زیرصفحه‌ها هم‌خوان باشد، فقط اندازه/خانواده طبق نیاز صفحه عوض می‌شود.
 */
internal fun settingsTypography(
    base: Typography,
    sizeDelta: Float,
    forceVazirmatnLight: Boolean = false,
): Typography {
    fun scaled(style: TextStyle): TextStyle = style.copy(
        fontSize = (style.fontSize.value + sizeDelta).coerceAtLeast(7f).sp,
        fontFamily = if (forceVazirmatnLight) SettingsVazirmatnLight else style.fontFamily,
        fontWeight = if (forceVazirmatnLight) FontWeight.Light else style.fontWeight,
    )
    return Typography(
        displayLarge = scaled(base.displayLarge),
        displayMedium = scaled(base.displayMedium),
        displaySmall = scaled(base.displaySmall),
        headlineLarge = scaled(base.headlineLarge),
        headlineMedium = scaled(base.headlineMedium),
        headlineSmall = scaled(base.headlineSmall),
        titleLarge = scaled(base.titleLarge),
        titleMedium = scaled(base.titleMedium),
        titleSmall = scaled(base.titleSmall),
        bodyLarge = scaled(base.bodyLarge),
        bodyMedium = scaled(base.bodyMedium),
        bodySmall = scaled(base.bodySmall),
        labelLarge = scaled(base.labelLarge),
        labelMedium = scaled(base.labelMedium),
        labelSmall = scaled(base.labelSmall),
    )
}
