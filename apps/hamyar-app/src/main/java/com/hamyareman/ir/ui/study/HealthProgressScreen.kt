package com.hamyareman.ir.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.art.readGallery
import com.hamyareman.ir.ui.exercise.exerciseMinutesOn
import java.time.LocalDate

/** وقتی مطالعه قفل است — هیچ ورودی دیگری به فلش‌کارت/آزمون راه ندارد. */
@Composable
fun LockedStudyScreen(onBack: () -> Unit) {
    AppTopBar(title = "قفل است 🔒", onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("اول تدریس، بعد تمرین 🌱", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            "«مطالعه و آزمون» بعد از اتمام دوره‌ی اول تدریسِ همان درس باز می‌شود.\nصوت یا ویدیوی تدریس را تا انتها ببین؛ خودکار باز می‌شود.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("متوجه شدم", onClick = onBack)
    }
}

@Composable
fun NeedSubScreen(onBack: () -> Unit) {
    AppTopBar(title = "نیاز به اشتراک", onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("این درس با اشتراک فعال باز می‌شود", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            "فصل ۱ ریاضی و اولین درس هر کتاب دیگر بدون اشتراک باز است. بقیهٔ درس‌ها با اشتراک فعال باز می‌شوند.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("متوجه شدم", onClick = onBack)
    }
}

private fun fa(n: Int) = toPersianDigits(n.toString())

/**
 * «پیشرفت سلامتی» — آب / ورزش / نقاشی (۷ روز اخیر). آمار درس و فلش و آزمون
 * در «نمودار پیشرفت دروس» است.
 */
@Composable
fun HealthProgressScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val store = container.store
    AppTopBar(title = "پیشرفت سلامتی 📊", onBack = onBack)
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---------------------------------------------- ۱) آب / ورزش / نقاشی
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("سلامتی روزانه — ۷ روز اخیر", style = MaterialTheme.typography.titleMedium)
                val gallery = remember { readGallery(store) }
                val todayDate = remember { LocalDate.now() }
                (6 downTo 0).forEach { offset ->
                    val iso = todayDate.minusDays(offset.toLong()).toString()
                    val water = store.getInt("consumed_$iso")
                    val exMin = exerciseMinutesOn(store, iso)
                    val art = gallery.count { it.dateIso == iso }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            JalaliDate.weekDayFa(iso),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(64.dp),
                        )
                        StatBar("💧", water, 8, Modifier.weight(1f))
                        StatBar("🏃‍♀️", exMin, 45, Modifier.weight(1f))
                        StatBar("🎨", art, 5, Modifier.weight(1f))
                    }
                }
                Text(
                    "هدف: ۸ لیوان آب · ۴۵ دقیقه ورزش · تمرین هنری روزانه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            "🔒 این آمار خودکار و از روی رویدادهای واقعی ثبت می‌شود و قابل ویرایش نیست.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatBar(emoji: String, value: Int, target: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(emoji, style = MaterialTheme.typography.labelSmall)
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (if (target <= 0) 0f else value.toFloat() / target).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(fa(value), style = MaterialTheme.typography.labelSmall)
    }
}
