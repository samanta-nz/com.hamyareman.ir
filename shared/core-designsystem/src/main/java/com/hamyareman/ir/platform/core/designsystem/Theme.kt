package com.hamyareman.ir.platform.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * تم‌های قابل‌انتخاب اپ «همیار من»:
 * - DollStage: تم عروسکی اصلی (پیش‌فرض)
 * - Stitch: جزیره‌ای/فضایی (آبی الکتریکی + صورتی مرجانی)
 * - MoonNight: بنفش شبانه با طلایی
 * - Mint: سبز نعنایی تازه
 * - CalmFather: تم آرامِ اپ پدر (تغییر نکند)
 */
enum class BrandTheme(
    val label: String,
    val visibleInAppearance: Boolean = true,
) {
    // مجموعهٔ تازهٔ کاربری: سه تم دخترانه و سه تم پسرانه، با نام‌های کوتاه و مدرن.
    PetalBloom("شکوفه"),
    LilacAir("یاسی هوا"),
    BerryGlow("بری گلو"),
    OceanPulse("پالس اقیانوس"),
    ForestForge("جنگل نیرومند"),
    NightOrbit("مدار شب"),

    // تم‌های قدیمی فقط برای سازگاری با تنظیمات ذخیره‌شده نگه داشته شده‌اند.
    DollStage("عروسکی", false),
    Stitch("جزیره‌ای (استیچ)", false),
    MoonNight("شب ماه", false),
    Mint("نعنایی", false),
    CalmFather("آرام جنگلی", false),
    RoseGarden("باغ گل", false),
    CandyCloud("آبنبات ابری", false),
    LavenderMist("مه بنفش", false),
    SunsetBloom("غروب گل", false),
    CherryFizz("آلبالو", false),
    StarryPink("صورتی ستاره‌ای", false),
    OceanBlue("اقیانوس", false),
    ForestTrail("مسیر جنگل", false),
    RocketNavy("موشک", false),
    EmberSport("ورزشی", false),
    ThunderLime("رعد لیمویی", false),
}

/** فونت جاری اپ — از ظاهر/تنظیمات عوض می‌شود؛ پیش‌فرض فونت سیستم. */
val LocalPlatformFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

private val DollLight = lightColorScheme(
    primary = DollPink, onPrimary = DollCreamOn,
    primaryContainer = DollPinkContainer, onPrimaryContainer = DollOnPinkContainer,
    secondary = DollPeach, onSecondary = DollOnPeach,
    secondaryContainer = DollPeachContainer, onSecondaryContainer = DollOnPeachContainer,
    tertiary = DollLavender, onTertiary = DollOnLavender,
    tertiaryContainer = DollLavenderContainer, onTertiaryContainer = DollOnLavenderContainer,
    background = DollCream, onBackground = DollCreamOn,
    surface = DollSurface, onSurface = DollCreamOn,
    surfaceVariant = DollSurfaceVariant, onSurfaceVariant = DollOnSurfaceVariant,
    outline = DollOutline, error = DollError,
)
private val DollDark = darkColorScheme(
    primary = DollPinkNight, onPrimary = DollOnPinkNight,
    primaryContainer = DollPinkNightContainer, onPrimaryContainer = DollOnPinkNightContainer,
    secondary = DollPeachNight, onSecondary = DollOnPeachNight,
    secondaryContainer = DollPeachNightContainer, onSecondaryContainer = DollOnPeachNightContainer,
    tertiary = DollLavenderNight, onTertiary = DollOnLavenderNight,
    tertiaryContainer = DollLavenderNightContainer, onTertiaryContainer = DollOnLavenderNightContainer,
    background = DollNightBackground, onBackground = DollNightOnBackground,
    surface = DollNightSurface, onSurface = DollNightOnBackground,
    surfaceVariant = DollNightSurfaceVariant, onSurfaceVariant = DollNightOnSurfaceVariant,
    outline = DollNightOutline, error = DollNightError,
)
private val CalmLight = lightColorScheme(
    primary = CalmPrimary, onPrimary = CalmOnPrimary,
    primaryContainer = CalmPrimaryContainer, onPrimaryContainer = CalmOnPrimaryContainer,
    secondary = CalmSecondary, onSecondary = CalmOnSecondary,
    secondaryContainer = CalmSecondaryContainer, onSecondaryContainer = CalmOnSecondaryContainer,
    tertiary = CalmTertiary, onTertiary = CalmOnTertiary,
    tertiaryContainer = CalmTertiaryContainer, onTertiaryContainer = CalmOnTertiaryContainer,
    background = CalmBackground, onBackground = CalmOnBackground,
    surface = CalmSurface, onSurface = CalmOnBackground,
    surfaceVariant = CalmSurfaceVariant, onSurfaceVariant = CalmOnSurfaceVariant,
    outline = CalmOutline, error = CalmError,
)
private val CalmDark = darkColorScheme(
    primary = CalmPrimaryNight, onPrimary = CalmOnPrimaryNight,
    background = CalmBackgroundNight, onBackground = CalmOnBackgroundNight,
    surface = CalmSurfaceNight, onSurface = CalmOnBackgroundNight,
    surfaceVariant = CalmSurfaceVariantNight, onSurfaceVariant = CalmOnSurfaceVariantNight,
    outline = CalmOutlineNight, error = CalmErrorNight,
)

private val StitchLight = lightColorScheme(
    primary = StitchBlue, onPrimary = StitchOnBlue,
    primaryContainer = StitchBlueContainer, onPrimaryContainer = StitchOnBlueContainer,
    secondary = StitchPink, onSecondary = StitchOnPink,
    secondaryContainer = StitchPinkContainer, onSecondaryContainer = StitchOnPinkContainer,
    tertiary = StitchTeal, onTertiary = StitchOnTeal,
    tertiaryContainer = StitchTealContainer, onTertiaryContainer = StitchOnTealContainer,
    background = StitchBg, onBackground = StitchOnBg,
    surface = StitchSurface, onSurface = StitchOnBg,
    surfaceVariant = StitchSurfaceVariant, onSurfaceVariant = StitchOnSurfaceVariant,
    outline = StitchOutline, error = StitchError,
)
private val StitchDark = darkColorScheme(
    primary = StitchBlueNight, onPrimary = StitchOnBlueNight,
    primaryContainer = StitchBlueNightContainer, onPrimaryContainer = StitchOnBlueNightContainer,
    secondary = StitchPinkNight, onSecondary = StitchOnPinkNight,
    secondaryContainer = StitchPinkNightContainer, onSecondaryContainer = StitchOnPinkNightContainer,
    tertiary = StitchTealNight, onTertiary = StitchOnTealNight,
    tertiaryContainer = StitchTealNightContainer, onTertiaryContainer = StitchOnTealNightContainer,
    background = StitchNightBg, onBackground = StitchNightOnBg,
    surface = StitchNightSurface, onSurface = StitchNightOnBg,
    surfaceVariant = StitchNightSurfaceVariant, onSurfaceVariant = StitchNightOnSurfaceVariant,
    outline = StitchNightOutline, error = StitchNightError,
)

private val MoonLight = lightColorScheme(
    primary = MoonViolet, onPrimary = MoonOnViolet,
    primaryContainer = MoonVioletContainer, onPrimaryContainer = MoonOnVioletContainer,
    secondary = MoonGold, onSecondary = MoonOnGold,
    secondaryContainer = MoonGoldContainer, onSecondaryContainer = MoonOnGoldContainer,
    tertiary = MoonRose, onTertiary = MoonOnRose,
    tertiaryContainer = MoonRoseContainer, onTertiaryContainer = MoonOnRoseContainer,
    background = MoonBg, onBackground = MoonOnBg,
    surface = MoonSurface, onSurface = MoonOnBg,
    surfaceVariant = MoonSurfaceVariant, onSurfaceVariant = MoonOnSurfaceVariant,
    outline = MoonOutline, error = MoonError,
)
private val MoonDark = darkColorScheme(
    primary = MoonVioletNight, onPrimary = MoonOnVioletNight,
    primaryContainer = MoonVioletNightContainer, onPrimaryContainer = MoonOnVioletNightContainer,
    secondary = MoonGoldNight, onSecondary = MoonOnGoldNight,
    secondaryContainer = MoonGoldNightContainer, onSecondaryContainer = MoonOnGoldNightContainer,
    tertiary = MoonRoseNight, onTertiary = MoonOnRoseNight,
    tertiaryContainer = MoonRoseNightContainer, onTertiaryContainer = MoonOnRoseNightContainer,
    background = MoonNightBg, onBackground = MoonNightOnBg,
    surface = MoonNightSurface, onSurface = MoonNightOnBg,
    surfaceVariant = MoonNightSurfaceVariant, onSurfaceVariant = MoonNightOnSurfaceVariant,
    outline = MoonNightOutline, error = MoonNightError,
)

private val MintLight = lightColorScheme(
    primary = MintGreen, onPrimary = MintOnGreen,
    primaryContainer = MintGreenContainer, onPrimaryContainer = MintOnGreenContainer,
    secondary = MintLime, onSecondary = MintOnLime,
    secondaryContainer = MintLimeContainer, onSecondaryContainer = MintOnLimeContainer,
    tertiary = MintSky, onTertiary = MintOnSky,
    tertiaryContainer = MintSkyContainer, onTertiaryContainer = MintOnSkyContainer,
    background = MintBg, onBackground = MintOnBg,
    surface = MintSurface, onSurface = MintOnBg,
    surfaceVariant = MintSurfaceVariant, onSurfaceVariant = MintOnSurfaceVariant,
    outline = MintOutline, error = MintError,
)
private val MintDark = darkColorScheme(
    primary = MintGreenNight, onPrimary = MintOnGreenNight,
    primaryContainer = MintGreenNightContainer, onPrimaryContainer = MintOnGreenNightContainer,
    secondary = MintLimeNight, onSecondary = MintOnLimeNight,
    secondaryContainer = MintLimeNightContainer, onSecondaryContainer = MintOnLimeNightContainer,
    tertiary = MintSkyNight, onTertiary = MintOnSkyNight,
    tertiaryContainer = MintSkyNightContainer, onTertiaryContainer = MintOnSkyNightContainer,
    background = MintNightBg, onBackground = MintNightOnBg,
    surface = MintNightSurface, onSurface = MintNightOnBg,
    surfaceVariant = MintNightSurfaceVariant, onSurfaceVariant = MintNightOnSurfaceVariant,
    outline = MintNightOutline, error = MintNightError,
)

@Composable
fun PlatformTheme(
    brand: BrandTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontFamily: FontFamily = FontFamily.Default,
    textSizeOffset: Int = 0,
    typography: Typography? = null,
    content: @Composable () -> Unit,
) {
    val scheme = extraScheme(brand, darkTheme) ?: when (brand) {
        BrandTheme.DollStage -> if (darkTheme) DollDark else DollLight
        BrandTheme.Stitch -> if (darkTheme) StitchDark else StitchLight
        BrandTheme.MoonNight -> if (darkTheme) MoonDark else MoonLight
        BrandTheme.Mint -> if (darkTheme) MintDark else MintLight
        BrandTheme.CalmFather -> if (darkTheme) CalmDark else CalmLight
        else -> if (darkTheme) DollDark else DollLight
    }
    val base = LocalDensity.current
    // مقیاس دسترس‌پذیری فقط یک‌بار روی تایپوگرافی استاندارد اعمال می‌شود.
    val scale = (1f + textSizeOffset.coerceIn(-6, 6) * 0.06f).coerceIn(0.70f, 1.36f)
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalPlatformFont provides fontFamily,
        LocalDensity provides Density(base.density, base.fontScale * scale),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography ?: platformTypography(fontFamily),
            shapes = PlatformShapes,
            content = content,
        )
    }
}
