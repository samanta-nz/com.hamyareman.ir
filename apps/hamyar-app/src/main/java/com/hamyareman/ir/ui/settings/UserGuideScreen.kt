package com.hamyareman.ir.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.designsystem.AppTopBar

private data class GuideSection(
    val id: String,
    val title: String,
    val icon: String,
    val text: String,
)

private val guideSections = listOf(
    GuideSection("school", "برنامهٔ کلاسی و مدرسه", "🏫", "در برنامهٔ کلاسی، درس‌ها را برای روزهای هفته می‌چینی، ساعت زنگ‌ها را یک‌بار وارد می‌کنی و در تب تقویم روزهای تعطیل را می‌بینی. بخش شیفت مدرسه، شیفت هفته، چرخهٔ یک/دو/چهارهفته‌ای، ساعت ورود، ساعت خروج و مدت آماده‌سازی قبل از حرکت را نگه می‌دارد."),
    GuideSection("alarms", "آلارم‌های مدرسه", "⏰", "آلارم بیداری شیفت صبح و آلارم آماده‌شدن شیفت ظهر دستی نیستند. ابتدا ساعت حضور همان شیفت و سپس مدت آماده‌سازی قبل از حرکت محاسبه می‌شود؛ بنابراین تغییر ساعت حضور یا مدت آماده‌سازی، هر دو زمان نمایشی و آلارم واقعی را خودکار جابه‌جا می‌کند. سرویس و خواب تنظیمات مستقل خودشان را دارند."),
    GuideSection("diary", "دفتر خاطرات و دفترچه‌ها", "📔", "صفحهٔ دفتر را مثل کاغذ واقعی ببین و روی همان ورق بنویس. فاصله‌ها حفظ می‌شوند و متن پررنگ و خوانا روی خطوط قرار می‌گیرد. در دفتر خاطرات می‌توان عکس را مستقیماً روی همان کاغذ جابه‌جا و بزرگ/کوچک کرد و برای آن عنوان یا پانویس گذاشت."),
    GuideSection("poetry", "دفتر شعر", "✒️", "نوع شعر را از فهرست انتخاب کن؛ گزینه‌های غزل، قصیده، دوبیتی، رباعی، مثنوی، شعر نو، سپید، نثر شاعرانه، ترانه و سایر در دسترس‌اند. عنوان بعد از انتخاب نوع شعر خودکار پیشنهاد می‌شود ولی می‌توانی آن را آزادانه ویرایش کنی. شعر هنگام تورق روی کاغذ دفتر نمایش داده می‌شود."),
    GuideSection("album", "آلبوم شخصی و رسانه‌ها", "🖼️", "آلبوم شخصی برای عکس، ویدیو و صوت است. رسانه‌های انتخاب‌شده در پوشهٔ خصوصی برنامه نگه‌داری می‌شوند و برای ویدیو/صوت از پلیر اختصاصی استفاده می‌شود. دفتر خاطرات قدیمی از این فهرست حذف شده و مسیر مستقل خودش را دارد."),
    GuideSection("sleep", "بشنو و بخواب", "🌙", "پلیر موسیقی در جریان همان صفحه باز می‌شود و محتوای پایین صفحه نباید با بازشدن آن ناپدید شود. دکمه‌های پخش و توقف همین پلیر را کنترل می‌کنند. کارت بالایی را لمس کن؛ بستن، پلیر را می‌بندد و خروج از صفحه پخش را متوقف می‌کند."),
    GuideSection("appearance", "ظاهر و فونت", "🔤", "اندازهٔ متن و ظاهر برنامه از بخش ظاهر تنظیم می‌شوند. بعد از تغییر فونت یا تنظیمات تایپوگرافی، گزینهٔ «بازگردانی فونت پیش‌فرض» تنظیمات فونت را به مقدار استاندارد برمی‌گرداند."),
    GuideSection("safe", "فضای امن", "🔒", "دفتر خاطرات، دفتر شعر، نوشته‌های آزاد و آلبوم شخصی داخل فضای امن قرار دارند. ورود به این بخش طبق قفل برنامه کنترل می‌شود و داده‌های خصوصی محلی هستند."),
    GuideSection("media", "صوت و ویدیوی آموزشی", "🎧", "در صفحات آموزشی، فایل محلیِ رمزنگاری‌شده در صورت وجود از روی گوشی پخش می‌شود و در غیر این صورت منبع آنلاین استفاده می‌شود. پخش، مکث، جلو/عقب، سرعت و ادامه از موقعیت قبلی روی همان رسانه اعمال می‌شوند."),
    GuideSection("settings", "تنظیمات و به‌روزرسانی", "⚙️", "از تنظیمات می‌توان ظاهر، قفل، یادآورها، همگام‌سازی و همین راهنمای کاربری را باز کرد. هنگام وجود یک نسخهٔ اجباری، صفحهٔ به‌روزرسانی بدون گزینهٔ ردکردن نمایش داده می‌شود تا نصب نسخهٔ جدید کامل شود."),
)

@Composable
fun UserGuideScreen(
    onBack: () -> Unit,
    initialSection: String = "",
) {
    val ordered = if (initialSection.isBlank()) {
        guideSections
    } else {
        buildList {
            guideSections.firstOrNull { it.id == initialSection }?.let { add(it) }
            addAll(guideSections.filter { it.id != initialSection })
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar("راهنمای کاربری برنامه", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "این راهنما برای استفادهٔ روزمره نوشته شده است. هر بخش توضیح می‌دهد چه چیزی قابل تنظیم است و چه چیزی خودکار انجام می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
            )
            ordered.forEach { section ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(section.icon, style = MaterialTheme.typography.titleLarge)
                            Text(
                                section.title,
                                Modifier.weight(1f).padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Outlined.HelpOutline,
                                    contentDescription = "راهنمای " + section.title,
                                )
                            }
                        }
                        Text(section.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
