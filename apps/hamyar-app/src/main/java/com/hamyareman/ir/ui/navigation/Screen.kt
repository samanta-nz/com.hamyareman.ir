package com.hamyareman.ir.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Study : Screen("study")
    data object StudyHome : Screen("study-home")
    /** جایگزین تب بازنشسته‌شدهٔ «همراه من»: فایل placeholder مشترک. */
    data object Placeholder : Screen("placeholder")
    data object More : Screen("more")
    data object About : Screen("about")
    data object Contact : Screen("contact")
    data object Cycle : Screen("cycle")
    data object CycleCal : Screen("cycle-cal")
    data object CycleLog : Screen("cycle-log")
    data object CycleToday : Screen("cycle-today")
    data object SleepBreath : Screen("sleep-breath")
    data object BackgroundMusic : Screen("background-music")
    data object Mood : Screen("mood")
    data object Mindfulness : Screen("mindfulness")
    data object Calm : Screen("calm")
    data object CalmHub : Screen("calm-hub")
    data object FreeReading : Screen("free-reading")
    data object Journal : Screen("journal")
    data object GratitudeJournal : Screen("gratitude-journal")
    data object Breath : Screen("breath")
    data object Routine : Screen("routine")
    data object SafeSpace : Screen("safespace")
    data object Diary : Screen("diary")
    data object Notebooks : Screen("notebooks")
    data object SafeFreeWriting : Screen("safe-free-writing")
    data object SecureGallery : Screen("secure-gallery")
    data object Album : Screen("album")
    data object Writing : Screen("writing")
    data object Helplines : Screen("helplines")
    data object Library : Screen("library")
    data object Audiobook : Screen("audiobook")
    data object School : Screen("school")
    /** صفحهٔ جداگانهٔ «مرخصی» (از منوی برنامه هفتگی و مرخصی). */
    data object Leave : Screen("leave")
    data object ClassPlan : Screen("class-plan")
    data object ClassPlanShift : Screen("class-plan-shift")
    /** سربرگ تقویم برنامه کلاسی — از تاریخ داشبورد. */
    data object ClassPlanCalendar : Screen("class-plan-cal")
    /** ساعت‌های کلاس مجازی + بازه‌های روزهای مجازی. */
    data object VirtualClass : Screen("virtual-class")
    data object Subscription : Screen("subscription")
    data object TomorrowPrep : Screen("tomorrow-prep")
    data object SleepNight : Screen("sleep-night")
    data object HealthHub : Screen("health")
    data object AwarenessHub : Screen("awareness")
    data object Academy : Screen("academy")
    data object AcademySoon : Screen("academy-coming-soon")
    data object Book : Screen("study-book/{bookCode}") {
        fun of(bookCode: String) = "study-book/${Uri.encode(bookCode)}"
    }
    data object LessonTeach : Screen("study-teach/{packId}") {
        fun of(packId: String) = "study-teach/${Uri.encode(packId)}"
    }
    data object WeeklySchedule : Screen("weekly-schedule")
    data object Meds : Screen("meds")
    data object SleepLog : Screen("sleep-log")
    data object ReadingCorner : Screen("reading-corner")
    data object Appearance : Screen("appearance")
    /** آزمون. `lessonId` اختیاری است تا از صفحه‌ی درس فقط سؤال‌های همان درس بیاید. */
    data object Quiz : Screen("quiz?lessonId={lessonId}") {
        fun of(lessonId: String? = null) =
            if (lessonId.isNullOrBlank()) "quiz" else "quiz?lessonId=${Uri.encode(lessonId)}"
    }
    data object QuizReview : Screen("quizreview")

    /** صفحه‌ی مطالعه‌ی عمیق یک درس (فلش‌کارت/آزمون/حل) — پک با `packId` مثل C905_E01-L01. */
    data object LessonStudy : Screen("study-lesson/{packId}") {
        fun of(packId: String) = "study-lesson/" + Uri.encode(packId)
    }

    /** نمایشگر PDF کتاب درس (از باکت Appwrite — کش فقط روی گوشی). */
    data object LessonPdf : Screen("study-lesson-pdf/{packId}") {
        fun of(packId: String) = "study-lesson-pdf/" + Uri.encode(packId)
    }
    data object Pdf : Screen("pdf")

    /** نمودار پیشرفت — v1.18: برای هر کتاب اختصاصی؛ bookCode اختیاری (بدون آن = انتخاب کتاب). */
    data object Charts : Screen("charts?bookCode={bookCode}") {
        fun of(bookCode: String? = null) =
            if (bookCode.isNullOrBlank()) "charts" else "charts?bookCode=${Uri.encode(bookCode)}"
    }

    /** ویدیوی تدریس هر درس — صفحه‌ی مجزا و تمام‌صفحه (v1.18). */
    data object VideoTeach : Screen("video-teach/{packId}") {
        fun of(packId: String) = "video-teach/" + Uri.encode(packId)
    }

    /** مدیریت دانلود صوت/PDF کتاب‌ها (v1.14). */
    data object Downloads : Screen("study-downloads")
    data object HealthProgress : Screen("health-progress")
    data object Art : Screen("art")
    data object Gallery : Screen("gallery")
    data object Learning : Screen("learning")
    data object Lesson : Screen("lesson/{id}") {
        fun of(id: String) = "lesson/${Uri.encode(id)}"
    }
    data object Placement : Screen("placement")
    /** نقشه‌ی راه. `track` اختیاری است تا ماژول هوش مصنوعی فقط گره‌های خودش را ببیند. */
    data object Roadmap : Screen("roadmap?track={track}") {
        fun of(track: String? = null) = if (track.isNullOrBlank()) "roadmap" else "roadmap?track=${Uri.encode(track)}"
    }
    data object AiLearning : Screen("ailearning")
    data object AiAssessment : Screen("aiassessment")
    data object Recipes : Screen("recipes")
    data object RecipeDetail : Screen("recipedetail/{id}") {
        fun of(id: String) = "recipedetail/${Uri.encode(id)}"
    }
    data object Exercise : Screen("exercise")
    data object ExerciseDetail : Screen("exercise/{id}") { fun of(id: String) = "exercise/${Uri.encode(id)}" }
    data object Water : Screen("water")
    data object Call : Screen("call")
    data object Settings : Screen("settings")
    data object UserProfile : Screen("user-profile")
    data object Privacy : Screen("privacy")
    data object Badges : Screen("badges")
    data object Lock : Screen("lock")
    data object Reminders : Screen("reminders")
    data object Sync : Screen("sync")

    /** محتوای همیار: منوی دسته‌ها + فهرست و نمایش HTML از کاتالوگ assets. */
    data object ContentHub : Screen("content-hub")
    data object ContentCategory : Screen("content-category/{cat}") {
        fun of(cat: String) = "content-category/${Uri.encode(cat)}"
    }
    data object ContentHtml : Screen("content-html/{id}") {
        fun of(id: String) = "content-html/${Uri.encode(id)}"
    }

    /** یک گرهٔ منوی کتاب: اگر فایلش آماده باشد PDF، وگرنه «در دست تولید». */
    data object BookNode : Screen("book-node/{key}/{title}?audio={audio}") {
        fun of(key: String, title: String, audio: String = "") =
            "book-node/${Uri.encode(key)}/${Uri.encode(title.ifBlank { "درس" })}" +
                "?audio=${Uri.encode(audio)}"
    }
    /** پرامپت ۰۲: ماژول سلامتی (یوگا/ورزش/تنفس/یادگیری). `cat` اختیاری: yoga/exercise/breathing/learning. */
    data object Wellness : Screen("wellness?cat={cat}") {
        fun of(cat: String? = null) =
            if (cat.isNullOrBlank()) "wellness" else "wellness?cat=${Uri.encode(cat)}"
    }
    /** گروه تو در توی ذهن‌آگاهی / کسب آرامش / بین دروس / پریود. */
    data object PracticeGroup : Screen("practice-group/{groupId}") {
        fun of(id: String) = "practice-group/${Uri.encode(id)}"
    }
    data object PracticeItem : Screen("practice/{itemId}") {
        fun of(id: String) = "practice/${Uri.encode(id)}"
    }
    /** فلوتر داشبورد: تمرینات کوتاه بین دروس. */
    data object BetweenLessons : Screen("between-lessons")
    /** پرامپت ۰۲: گالری مرجع‌های نقاشی سیاه‌قلم. */
    data object SketchGallery : Screen("sketch-gallery")
    /** «تمرینات مخصوص این دوره» — چهار سربرگِ یوگا/تنفس/کنترل درد/آرامش برای روزهای چرخه. */
    data object PeriodTraining : Screen("cycle-training")

    /** کارت‌های تازهٔ داشبورد: جعبه‌ابزار عمومی / آزمایشگاه شیمی / آزمایشگاه فیزیک / جعبه‌ابزار ریاضی. */
    data object GeneralToolkit : Screen("toolkit-general")
    data object ChemistryLab : Screen("lab-chemistry")
    data object PhysicsLab : Screen("lab-physics")
    data object BiologyLab : Screen("lab-biology")
    data object MathToolkit : Screen("toolkit-math")
    data object ToolHtml : Screen("tool/{toolId}") {
        fun of(toolId: String) = "tool/${Uri.encode(toolId)}"
    }
}

data class Tab(val route: String, val icon: ImageVector, val label: String)
val Tabs = listOf(
    Tab(Screen.Home.route, Icons.Filled.Home, "داشبورد"),
    Tab(Screen.Study.route, Icons.Filled.School, "مدرسه"),
    Tab(Screen.Academy.route, Icons.Filled.LocalLibrary, "آموزشگاه"),
    Tab(Screen.HealthHub.route, Icons.Filled.FitnessCenter, "سلامتی"),
    Tab(Screen.Placeholder.route, Icons.Filled.SmartToy, "به‌زودی"),
    Tab(Screen.More.route, Icons.Filled.MoreHoriz, "بیشتر"))
val TopRoutes = Tabs.map { it.route }.toSet()
