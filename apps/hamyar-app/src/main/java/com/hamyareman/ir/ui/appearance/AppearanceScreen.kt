package com.hamyareman.ir.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.platform.core.designsystem.BrandTheme
import com.hamyareman.ir.platform.core.designsystem.ThemeGender
import com.hamyareman.ir.platform.core.designsystem.themeGender
import com.hamyareman.ir.platform.core.designsystem.swatches
import com.hamyareman.ir.platform.core.designsystem.AppTopBar

/** صفحه‌ی ظاهر: تم رنگ و اندازهٔ نوشته. فونت قفل است. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit, onHelp: () -> Unit = {}) {
    val prefs = LocalUiPrefs.current
    var pendingTheme by remember { mutableStateOf<BrandTheme?>(null) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("ظاهر", onBack, onHelp = onHelp)
        Column(
            Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("تم‌های تازهٔ هماهنگ با هویت همیار من، همراه با اندازهٔ نوشتهٔ سراسری ✨", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("اندازهٔ نوشتهٔ سراسری", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { prefs.resetFontTheme() }) {
                    Text("بازگردانی فونت پیش‌فرض")
                }
            }
            Text(
                if (prefs.textSizeOffset == 0) "استاندارد (۱۳)"
                else {
                    val n = 13 + prefs.textSizeOffset
                    val sign = if (prefs.textSizeOffset > 0) "+" else ""
                    "پایه ۱۳  ·  ${toPersianDigits(sign + prefs.textSizeOffset.toString())}  ·  ${toPersianDigits(n.toString())}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = prefs.textSizeOffset.toFloat(),
                onValueChange = { prefs.updateTextSizeOffset(it.toInt()) },
                valueRange = -6f..6f,
                steps = 11,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("نمونه: این جمله با اندازهٔ انتخابی دیده می‌شود.", style = MaterialTheme.typography.bodyLarge)

            Text("حالت رنگ", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "سیستم", "light" to "روشن", "dark" to "تاریک").forEach { (key, label) ->
                    FilterChip(
                        selected = prefs.darkMode == key,
                        onClick = { prefs.updateDarkMode(key) },
                        label = { Text(label) },
                    )
                }
            }

            val girl = com.hamyareman.ir.ui.profile.StudentProfileState.gender.equals("girl", ignoreCase = true)
            if (girl) {
                Text("تم‌های دخترانه", style = MaterialTheme.typography.titleMedium)
                ThemeGroup(BrandTheme.entries.filter { it.visibleInAppearance && it.themeGender == ThemeGender.GIRL }, prefs) { pendingTheme = it }
            } else {
                Text("تم‌های پسرانه", style = MaterialTheme.typography.titleMedium)
                ThemeGroup(BrandTheme.entries.filter { it.visibleInAppearance && it.themeGender == ThemeGender.BOY }, prefs) { pendingTheme = it }
            }

            pendingTheme?.let { brand ->
                AlertDialog(
                    onDismissRequest = { pendingTheme = null },
                    title = { Text("پیش‌نمایش تم «${brand.label}»") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                brand.swatches().forEach { c ->
                                    Box(Modifier.size(36.dp).background(c, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                                }
                            }
                            Text("اگر تأیید کنی، تم همین حالا روی کل اپ اعمال می‌شود.")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { prefs.updateTheme(brand); pendingTheme = null }) { Text("اعمال تم") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingTheme = null }) { Text("انصراف") }
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeGroup(brands: List<BrandTheme>, prefs: UiPrefs, onPreview: (BrandTheme) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        brands.forEach { brand ->
            val selected = prefs.theme == brand
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth().clickable { onPreview(brand) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        brand.swatches().forEach { c ->
                            Box(Modifier.size(22.dp).background(c, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(brand.label, style = MaterialTheme.typography.titleMedium)
                        if (selected) Text("انتخاب‌شده ✓", style = AppTypography.caption, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
