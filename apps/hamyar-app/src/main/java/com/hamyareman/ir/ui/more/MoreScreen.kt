package com.hamyareman.ir.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.study.ClassPlanStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.ui.hub.HubCard
import com.hamyareman.ir.ui.hub.hubTo
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.update.UpdateCheckCard

/**
 * «بیشتر» — همه‌چیز غیر از ۵ تب اصلی:
 * فضای امن و شماره‌های کمک، پروفایل، ظاهر و فونت، و تنظیمات.
 * (کارت‌های «امتیاز و بج»، «نقاشی سیاه‌قلم» و «آشپزی» طبقِ درخواست برداشته شدند.)
 */
@Composable
fun MoreScreen(nav: NavController) {
    HubBody {
        HubHeader("بیشتر", "هر چیز دیگر که به کارت می‌آید", slotId = "hub.more.header")


        HubCard("🛟", "فضای امن من", "نوشتن، آلبوم، شماره‌های کمک", slotId = "hub.more.item.safespace") { nav.hubTo(Screen.SafeSpace.route) }
        HubCard("🚨", "شماره‌های کمک", "همیشه در دسترس", slotId = "hub.more.item.helplines") { nav.hubTo(Screen.Helplines.route) }

        QuietModeCard()

        HubCard("👤", "پروفایل من", "مشخصات من، مدرسه، عکس و وضعیت اشتراک", slotId = "hub.more.item.user-profile") { nav.hubTo(Screen.UserProfile.route) }
        HubCard("⚙️", "تنظیمات", "تم برنامه، قفل، یادآورها و تنظیمات سرور", slotId = "hub.more.item.settings") { nav.hubTo(Screen.Settings.route) }
        HubCard("ℹ️", "درباره ما", "هدف و حریم خصوصی همیار من", slotId = "hub.more.item.about") { nav.hubTo(Screen.About.route) }
        HubCard("💬", "تماس با ما", "ارسال مستقیم درخواست به پشتیبانی", slotId = "hub.more.item.contact") { nav.hubTo(Screen.Contact.route) }

        // بررسی دستی نسخه باید در صفحهٔ «بیشتر» همیشه در دسترس بماند.
        UpdateCheckCard()

        Text(
            "همیار من — نسخه‌ی " + com.hamyareman.ir.BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * «زمان درس» — کلیدِ سکوتِ سراسریِ پلیر دروس: وقتی روشن است، هیچ صدایی از
 * پلیر صوت/ویدیوی تدریس و خلاصه‌ها پخش نمی‌شود (خودکار متوقف و بی‌صدا می‌ماند).
 */
@Composable
private fun QuietModeCard() {
    val ctx = LocalContext.current
    val store = remember { LocalStore(ctx, "hamyar_teach") }
    var on by remember { mutableStateOf(store.getString("quiet_mode", "0") == "1") }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🔇 زمان درس", style = MaterialTheme.typography.titleMedium, fontFamily = com.hamyareman.ir.ui.appearance.TypeSlots.family("hub.more.item.quiet"), fontSize = com.hamyareman.ir.ui.appearance.TypeSlots.size("hub.more.item.quiet", 16))
                Text(
                    "وقتی روشن است هیچ صدایی از پلیر دروس (صوت و ویدیوی تدریس) فعال نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = com.hamyareman.ir.ui.appearance.TypeSlots.family("hub.more.item.quiet"),
                    fontSize = com.hamyareman.ir.ui.appearance.TypeSlots.size("hub.more.item.quiet", 12))
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = on,
                onCheckedChange = {
                    on = it
                    store.putString("quiet_mode", if (it) "1" else "0")
                })
        }
    }
}

/** تیک انواع مناسبت که زیر تاریخ داشبورد دیده می‌شوند. */
@Composable
private fun CalendarOccasionCard() {
    val ctx = LocalContext.current
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("📅 تنظیم تقویم", style = MaterialTheme.typography.titleMedium)
            Text(
                "مناسبت‌هایی که زیر تاریخ داشبورد نشان داده می‌شوند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            com.hamyareman.ir.ui.home.OccasionKind.entries.forEach { kind ->
                var on by remember(kind) { mutableStateOf(com.hamyareman.ir.ui.home.CalendarPrefs.show(ctx, kind)) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = {
                            on = it
                            com.hamyareman.ir.ui.home.CalendarPrefs.setShow(ctx, kind, it)
                        })
                    Text(kind.fa, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
