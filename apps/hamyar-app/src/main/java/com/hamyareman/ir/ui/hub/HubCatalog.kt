package com.hamyareman.ir.ui.hub

import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.profile.AppEdition
import com.hamyareman.ir.ui.profile.GradeLevel
import com.hamyareman.ir.ui.profile.StudentProfileState

/**
 * کاتالوگ منوی «مدرسه» و «آموزشگاه» — هر پایه فایل/شاخه‌ی خودش را دارد
 * تا بعداً آیتم‌ها و آیکون دختر/پسر جدا شوند، بدون دست‌زدن به موتور.
 *
 * گروه «کتاب‌ها» از رجیستری کتاب می‌آید و اینجا نیست.
 */
data class HubItemSpec(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val route: String,
    /** اگر خالی باشد همان [emoji] برای دختر هم استفاده می‌شود. */
    val emojiGirl: String = "",
) {
    fun emojiFor(gender: String): String =
        if (gender.equals("girl", true) && emojiGirl.isNotBlank()) emojiGirl else emoji
}

data class HubGroupSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val items: List<HubItemSpec>,
)

data class GradeHubSpec(
    val grade: GradeLevel,
    val schoolExtra: List<HubGroupSpec>,
    val academy: List<HubGroupSpec>,
)

object HubCatalog {
    fun schoolExtra(): List<HubGroupSpec> = GradeHubs.of(AppEdition.grade).schoolExtra
    fun academy(): List<HubGroupSpec> = GradeHubs.of(AppEdition.grade).academy
    fun gender(): String = StudentProfileState.gender
}

/**
 * پیش‌فرض فعلی (پایه‌ی نهم). پایه‌های دیگر فعلاً همین را ارث می‌برند
 * تا منو جدا ساخته شود — بعد هر پایه را جدا ویرایش کن.
 */
internal fun defaultSchoolExtra(): List<HubGroupSpec> = listOf(
    HubGroupSpec(
        id = "downloads",
        title = "⬇️ دانلودها",
        subtitle = "صوت و PDF هر کتاب — دانلود یکجا با نمایش حجم",
        items = listOf(
            HubItemSpec("📶", "مدیریت دانلود کتاب‌ها", "وضعیت دانلود صوت‌ها و PDFها به تفکیک کتاب", Screen.Downloads.route),
        ),
    ),
    HubGroupSpec(
        id = "schedule",
        title = "🗓 برنامه هفتگی و مرخصی",
        subtitle = "برنامهٔ شخصی تو، شیفت مدرسه و مرخصی‌ها",
        items = listOf(
            HubItemSpec("⏰", "برنامه‌ی هفتگی من", "جدول زمانی شخصی شنبه تا جمعه — درس، تکلیف، مرور، ورزش", Screen.WeeklySchedule.route),
            HubItemSpec("📄", "مرخصی", "ثبت بازهٔ مرخصی با علت، گواهی پزشکی و وضعیتِ توجیه", Screen.Leave.route),
            HubItemSpec("🏫", "برنامه‌ی مدرسه", "شیفت چرخشی و زنگ‌های کلاسی", Screen.School.route),
        ),
    ),
    HubGroupSpec(
        id = "quiz",
        title = "📝 آزمون و بازخورد",
        subtitle = "سنجش دروس مدرسه",
        items = listOf(
            HubItemSpec("🧪", "جزوه‌های شخصی و آزمونی", "دفتر نکات + گالری فایل روی گوشی", Screen.Pdf.route),
            HubItemSpec("📈", "نمودار پیشرفت دروس", "رشدت در هر درس — طبق آزمون‌ها و فلش‌کارت‌ها", Screen.Charts.of(null)),
        ),
    ),
)

internal fun defaultAcademy(): List<HubGroupSpec> = listOf(
    HubGroupSpec(
        id = "ai",
        title = "🤖 آموزش هوش مصنوعی",
        subtitle = "مسیر پیش‌نیازدار و ارزیابی",
        items = listOf(
            HubItemSpec("📖", "درس‌های من", "محتوای تعاملی یادگیری، آزمون تعیین سطح و نقشه‌ی راه", Screen.Learning.route),
            HubItemSpec("✨", "یادگیری با AI", "از پایه تا پروژه، درسِ روزانه‌شده", Screen.AiLearning.route),
            HubItemSpec("🗺", "مسیرهای یادگیری", "فهرست مسیرها بر اساس علاقه", Screen.Roadmap.of("")),
        ),
    ),
    HubGroupSpec(
        id = "misc",
        title = "🎨 متفرقه",
        subtitle = "خلاقیت و آرامش",
        items = listOf(
            HubItemSpec("🎨", "هنر روزانه", "هر روز یک تمرین کوچک هنری", Screen.Art.route),
        ),
    ),
)

object GradeHubs {
    fun of(grade: GradeLevel): GradeHubSpec = when (grade) {
        GradeLevel.G4 -> g(GradeLevel.G4)
        GradeLevel.G5 -> g(GradeLevel.G5)
        GradeLevel.G6 -> g(GradeLevel.G6)
        GradeLevel.G7 -> g(GradeLevel.G7)
        GradeLevel.G8 -> g(GradeLevel.G8)
        GradeLevel.G9 -> g(GradeLevel.G9)
        GradeLevel.G10 -> g(GradeLevel.G10)
        GradeLevel.G11 -> g(GradeLevel.G11)
        GradeLevel.G12 -> g(GradeLevel.G12)
    }

    private fun g(grade: GradeLevel) = GradeHubSpec(
        grade = grade,
        schoolExtra = defaultSchoolExtra(),
        academy = defaultAcademy(),
    )
}
