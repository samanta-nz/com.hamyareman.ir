package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** هاب «سلامتی»: جلدهای مربعی ۲ در ردیف، مثل کتاب‌ها. */
@Composable
fun HealthHubScreen(nav: NavController) {
    val girl = com.hamyareman.ir.ui.profile.StudentProfileState.gender != "boy"
    val tiles = buildList {
        add(HubCoverTile("hl-progress", "پیشرفت سلامتی", "آب، ورزش و آمار درس", { nav.hubTo(Screen.HealthProgress.route) }))
        if (girl) {
            add(HubCoverTile("hl-period", "چرخه ماهانه", "تقویم، علائم، تنفس درد و تمرین ملایم", { nav.hubTo(Screen.PracticeGroup.of("hl-cycle")) }))
        }
        add(HubCoverTile("hl-yoga", "یوگا", "حرکات با راهنمای صوتی و تایمر", { nav.hubTo(Screen.Wellness.of("yoga")) }))
        add(HubCoverTile("hl-exercise", "ورزش عمومی", "کشش و تقویت ملایم", { nav.hubTo(Screen.Wellness.of("exercise")) }))
        add(HubCoverTile("hl-food", "آب و تغذیه", "یادآور آب و راهنمای تمرکز", { nav.hubTo(Screen.PracticeGroup.of("hl-nutrition")) }))
        add(HubCoverTile("hl-sleep", "خواب", "ثبت، قصه، بشنو و بخواب، تنفس شب", { nav.hubTo(Screen.PracticeGroup.of("hl-sleep")) }))
        add(HubCoverTile("hl-meds", "یادآور دارو و مراقبت", "هشدار سرِ وقت", { nav.hubTo(Screen.Meds.route) }))
        add(HubCoverTile("hl-routine", "روتین روز", "بلوک‌های روز یا روز سبک", { nav.hubTo(Screen.Routine.route) }))
    }
    HubBody {
        HubHeader("سلامتی 💚", "بدنت دوست توست — هر روز یک قدم مهربانی", slotId = "hub.health.header")
        HubCoverGrid(tiles)
    }
}

/**
 * ثبت خواب ساده و مهربان: ساعت خواب/بیداری هر شب + رشته‌ی شب‌های پیوسته.
 * داده فقط همین‌جا (LocalStore) می‌ماند.
 */
@Composable
fun SleepLogScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { LocalStore(context, "hamyar_health") }
    var bedtime by remember { mutableStateOf(store.getString("sleep_bed_${todayKey()}", "")) }
    var waketime by remember { mutableStateOf(store.getString("sleep_wake_${todayKey()}", "")) }

    HubBody {
        HubHeader("خواب من 😴", "امشب هم به بدنت آرامش بده", onBack)
        Text(
            "نیمی از شارژِ فردا در خوابِ امشب ذخیره می‌شود. ساعت‌ها را حدودی بنویس؛ کافی است.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = bedtime,
                onValueChange = { v -> if (v.length <= 5) { bedtime = v; store.putString("sleep_bed_${todayKey()}", v) } },
                label = { Text("خوابیدم (مثلاً 22:30)") },
                modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = waketime,
                onValueChange = { v -> if (v.length <= 5) { waketime = v; store.putString("sleep_wake_${todayKey()}", v) } },
                label = { Text("بیدار شدم") },
                modifier = Modifier.weight(1f))
        }
        val streak = remember { sleepStreak(store) }
        Text("🌙 رشته‌ی شب‌های ثبت‌شده: $streak شب پیوسته", style = MaterialTheme.typography.titleMedium)
        Text(
            "ثبتِ ناقص هم اشکال ندارد؛ مهم این است که زنجیره پاره نشود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

internal fun sleepStreak(store: LocalStore): Int {
    var streak = 0
    val cal = Calendar.getInstance()
    repeat(60) {
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        if (store.getString("sleep_bed_$key", "").isNotBlank() || store.getString("sleep_wake_$key", "").isNotBlank()) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else if (it == 0) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else return streak
    }
    return streak
}
