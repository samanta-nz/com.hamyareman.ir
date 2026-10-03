package com.hamyareman.ir.platform.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** جنسیت تم — برای گروه‌بندی در تنظیمات ظاهر. */
enum class ThemeGender { GIRL, BOY }

val BrandTheme.themeGender: ThemeGender
    get() = when (this) {
        BrandTheme.PetalBloom, BrandTheme.LilacAir, BrandTheme.BerryGlow,
        BrandTheme.DollStage, BrandTheme.MoonNight, BrandTheme.RoseGarden,
        BrandTheme.CandyCloud, BrandTheme.LavenderMist, BrandTheme.SunsetBloom,
        BrandTheme.CherryFizz, BrandTheme.StarryPink -> ThemeGender.GIRL
        else -> ThemeGender.BOY
    }

fun BrandTheme.swatches(): List<Color> = when (this) {
    BrandTheme.PetalBloom -> listOf(Color(0xFFD34D7B), Color(0xFF8B63C7), Color(0xFFE0A15D))
    BrandTheme.LilacAir -> listOf(Color(0xFF7E57C2), Color(0xFF46A79D), Color(0xFFB47AE0))
    BrandTheme.BerryGlow -> listOf(Color(0xFFB83272), Color(0xFFE47B9E), Color(0xFFE9B949))
    BrandTheme.OceanPulse -> listOf(Color(0xFF1677B8), Color(0xFF16A6A0), Color(0xFF5F88D3))
    BrandTheme.ForestForge -> listOf(Color(0xFF2F7A5A), Color(0xFF7AAE43), Color(0xFFC38B3A))
    BrandTheme.NightOrbit -> listOf(Color(0xFF4A67C9), Color(0xFF7A8AE6), Color(0xFF4AB8C4))
    BrandTheme.DollStage -> listOf(Color(0xFFE96A8D), Color(0xFFF4916B), Color(0xFFA77BD4))
    BrandTheme.Stitch -> listOf(Color(0xFF2F7FD6), Color(0xFFF26FA7), Color(0xFF27B5A8))
    BrandTheme.MoonNight -> listOf(Color(0xFF7C6BD9), Color(0xFFE8B84B), Color(0xFFC97BA8))
    BrandTheme.Mint -> listOf(Color(0xFF2FA37A), Color(0xFF8FBF3C), Color(0xFF4FA8D9))
    BrandTheme.CalmFather -> listOf(Color(0xFF5F8A78), Color(0xFFC39A6A), Color(0xFF9C8FC9))
    BrandTheme.RoseGarden -> listOf(Color(0xFFD94A7A), Color(0xFF5FA36A), Color(0xFFF2C3D0))
    BrandTheme.CandyCloud -> listOf(Color(0xFFFF7EB3), Color(0xFF7EC8E3), Color(0xFFFFE566))
    BrandTheme.LavenderMist -> listOf(Color(0xFF9B7EDE), Color(0xFF7DCEB8), Color(0xFFE5D4FF))
    BrandTheme.SunsetBloom -> listOf(Color(0xFFFF6F61), Color(0xFFFFB347), Color(0xFFE85D75))
    BrandTheme.CherryFizz -> listOf(Color(0xFFE63950), Color(0xFFFF8FAB), Color(0xFFFFF0C2))
    BrandTheme.StarryPink -> listOf(Color(0xFFE056A0), Color(0xFF5B4B8A), Color(0xFFC9B6FF))
    BrandTheme.OceanBlue -> listOf(Color(0xFF1E6BB8), Color(0xFF2EC4B6), Color(0xFFF2D6A2))
    BrandTheme.ForestTrail -> listOf(Color(0xFF2D6A4F), Color(0xFF95D5B2), Color(0xFFC9A227))
    BrandTheme.RocketNavy -> listOf(Color(0xFF243E8C), Color(0xFFFF6B35), Color(0xFF8FA4C8))
    BrandTheme.EmberSport -> listOf(Color(0xFFE6392B), Color(0xFF2B2D42), Color(0xFFF4A261))
    BrandTheme.ThunderLime -> listOf(Color(0xFF8BE000), Color(0xFF3D5A80), Color(0xFF1B263B))
}

/** پالت‌های ۱۱ تم جدید — روشن/تاریک. */
fun extraScheme(brand: BrandTheme, dark: Boolean): ColorScheme? = when (brand) {
    BrandTheme.PetalBloom -> trio(
        dark, 0xFFD34D7B, 0xFF8B63C7, 0xFFE0A15D,
        0xFFFFF8FB, 0xFF2A141E, 0xFF24121A, 0xFFF9D8E5, 0xFF482132,
    )
    BrandTheme.LilacAir -> trio(
        dark, 0xFF7E57C2, 0xFF46A79D, 0xFFB47AE0,
        0xFFF8F5FF, 0xFF1D1630, 0xFF191327, 0xFFE6DFFF, 0xFF34274E,
    )
    BrandTheme.BerryGlow -> trio(
        dark, 0xFFB83272, 0xFFE47B9E, 0xFFE9B949,
        0xFFFFF5FA, 0xFF2A101E, 0xFF24101A, 0xFFF6D1E5, 0xFF452039,
    )
    BrandTheme.OceanPulse -> trio(
        dark, 0xFF1677B8, 0xFF16A6A0, 0xFF5F88D3,
        0xFFF2F9FD, 0xFF0E1B26, 0xFF0B1720, 0xFFD5ECF7, 0xFF18374A,
    )
    BrandTheme.ForestForge -> trio(
        dark, 0xFF2F7A5A, 0xFF7AAE43, 0xFFC38B3A,
        0xFFF3F9F4, 0xFF102019, 0xFF0D1A14, 0xFFD6EBD9, 0xFF234331,
    )
    BrandTheme.NightOrbit -> trio(
        dark, 0xFF4A67C9, 0xFF7A8AE6, 0xFF4AB8C4,
        0xFFF1F5FF, 0xFF0F1528, 0xFF0B1122, 0xFFDDE4FF, 0xFF1E2C4A,
    )
    BrandTheme.RoseGarden -> trio(
        dark, 0xFFD94A7A, 0xFF5FA36A, 0xFFC47BA0,
        0xFFFFF6F8, 0xFF2A1218, 0xFF2A1218, 0xFFFFD0DC, 0xFF3A1822,
    )
    BrandTheme.CandyCloud -> trio(
        dark, 0xFFFF7EB3, 0xFF5AA9C8, 0xFFE8C84A,
        0xFFFFF7FB, 0xFF2A1520, 0xFF241820, 0xFFFFD0E6, 0xFF3A2030,
    )
    BrandTheme.LavenderMist -> trio(
        dark, 0xFF9B7EDE, 0xFF4EAEA0, 0xFFC9A0E8,
        0xFFF7F3FF, 0xFF1C1730, 0xFF1A1628, 0xFFE5D4FF, 0xFF2E2648,
    )
    BrandTheme.SunsetBloom -> trio(
        dark, 0xFFE85D4C, 0xFFE09A2E, 0xFFD4536A,
        0xFFFFF6F0, 0xFF2A1610, 0xFF241410, 0xFFFFD4C4, 0xFF3A2218,
    )
    BrandTheme.CherryFizz -> trio(
        dark, 0xFFE63950, 0xFFFF8FAB, 0xFFE8B84A,
        0xFFFFF5F6, 0xFF2A1014, 0xFF241014, 0xFFFFD0D6, 0xFF3A1820,
    )
    BrandTheme.StarryPink -> trio(
        dark, 0xFFE056A0, 0xFF6B5B9A, 0xFFC9B6FF,
        0xFFFBF0FF, 0xFF1A1228, 0xFF161022, 0xFFF0D4FF, 0xFF2A2040,
    )
    BrandTheme.OceanBlue -> trio(
        dark, 0xFF1E6BB8, 0xFF1AA394, 0xFFC9A24A,
        0xFFF3F8FF, 0xFF0E1A28, 0xFF0C1824, 0xFFCDE4FF, 0xFF163048,
    )
    BrandTheme.ForestTrail -> trio(
        dark, 0xFF2D6A4F, 0xFFC9A227, 0xFF5FA36A,
        0xFFF4FBF6, 0xFF102018, 0xFF0E1C16, 0xFFC5EBD4, 0xFF1A3A2C,
    )
    BrandTheme.RocketNavy -> trio(
        dark, 0xFF243E8C, 0xFFE85A28, 0xFF6B82B8,
        0xFFF4F6FC, 0xFF101828, 0xFF0E1624, 0xFFD0D8F0, 0xFF1C2848,
    )
    BrandTheme.EmberSport -> trio(
        dark, 0xFFE6392B, 0xFF3D405B, 0xFFE09A3A,
        0xFFFFF6F4, 0xFF1A1010, 0xFF161010, 0xFFFFD4CC, 0xFF3A1818,
    )
    BrandTheme.ThunderLime -> trio(
        dark, 0xFF6BB000, 0xFF3D5A80, 0xFF8BE000,
        0xFFF4FBE8, 0xFF101820, 0xFF0E161C, 0xFFD8F0A0, 0xFF1C2830,
    )
    else -> null
}

private fun argb(v: Long) = Color(v.toInt())

private fun trio(
    dark: Boolean,
    p: Long, s: Long, t: Long,
    bgL: Long, bgD: Long, surfaceD: Long, containerL: Long, containerD: Long,
): ColorScheme {
    val primary = argb(p)
    val secondary = argb(s)
    val tertiary = argb(t)
    return if (!dark) lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = argb(containerL), onPrimaryContainer = argb(bgD),
        secondary = secondary, onSecondary = Color.White,
        tertiary = tertiary, onTertiary = Color.White,
        background = argb(bgL), onBackground = argb(bgD),
        surface = argb(bgL), onSurface = argb(bgD),
        surfaceVariant = argb(containerL), onSurfaceVariant = argb(bgD).copy(alpha = 0.72f),
    ) else darkColorScheme(
        primary = Color(
            red = (primary.red + 0.25f).coerceAtMost(1f),
            green = (primary.green + 0.2f).coerceAtMost(1f),
            blue = (primary.blue + 0.2f).coerceAtMost(1f),
        ),
        onPrimary = argb(bgD),
        primaryContainer = argb(containerD), onPrimaryContainer = argb(containerL),
        secondary = secondary,
        tertiary = tertiary,
        background = argb(bgD), onBackground = Color(0xFFE8E4DC),
        surface = argb(surfaceD), onSurface = Color(0xFFE8E4DC),
        surfaceVariant = argb(containerD), onSurfaceVariant = Color(0xFFC8C0B8),
    )
}
