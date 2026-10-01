package com.hamyareman.ir

import android.app.Application
import com.hamyareman.ir.platform.core.notifications.NotificationChannels
import com.hamyareman.ir.platform.core.notifications.Reminder
import com.hamyareman.ir.di.AppContainer
import com.hamyareman.ir.ui.ailearning.AI_LESSON_REMINDER_ID
import com.hamyareman.ir.ui.study.SchoolAlarmStore

class HamyarApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val name = thread.name.orEmpty()
            if (name.contains("OkHttp", ignoreCase = true) || name.contains("DefaultDispatcher")) {
                return@setDefaultUncaughtExceptionHandler
            }
            previous?.uncaughtException(thread, error)
        }
        container = AppContainer(this)
        // اگر process نسخهٔ قبلی پیش از callback نصب بسته شد، نسخهٔ تازه در همین
        // شروع فایل APK/part را بعد از تطبیق versionCode حذف می‌کند.
        runCatching { com.hamyareman.ir.ui.update.ApkUpdate.cleanupAfterSuccessfulInstall(this) }
        // نشانی دوگانهٔ محتوا: تنظیمات سرور + کاتالوگ assets (پیش‌بارگذاری در آغاز اجرا)
        com.hamyareman.ir.ui.study.ServerPrefs.init(this)
        runCatching { com.hamyareman.ir.ui.content.ContentCatalog.load(this) }
        runCatching { com.hamyareman.ir.ui.tools.ToolRemote.clearPlainCache(this) }
        // کش قدیمی درس‌ها با کلید و مسیر دیگری ساخته شده بود؛ بعد از تغییر آدرس‌ها
        // به Bucket/... دیگر هیچ‌وقت hit نمی‌شود و فقط جا می‌گیرد.
        runCatching { com.hamyareman.ir.ui.study.RemoteHtmlCache.clear(this) }
        com.hamyareman.ir.ui.study.ServerResolver.probeAsync()
        // پخشِ فایل‌های گاوصندوق: طرحِ vault:// به جریانِ رمزگشاییِ تنبل وصل می‌شود
        // (خوانشِ جسته‌گریخته؛ بدونِ بلوکه‌شدنِ لودرِ پلیر برای رمزگشاییِ کل فایل).
        com.hamyareman.ir.platform.feature.playback.VaultSourceHooks.open = { key ->
            runCatching { com.hamyareman.ir.ui.study.MediaVault.openPlainStream(this, key) }.getOrNull()
        }
        NotificationChannels.ensure(this)
        // بازهٔ سکوت بعد از بسته‌شدن برنامه و reboot مستقل از UI اجرا می‌شود.
        com.hamyareman.ir.platform.core.notifications.QuietHoursAutomation.schedule(this)
        com.hamyareman.ir.ui.profile.StudentProfileState.loadMirror(this)
        com.hamyareman.ir.ui.profile.StudentProfileState.applyLauncherIcon(this, com.hamyareman.ir.ui.profile.StudentProfileState.gender)
        runCatching {
            val snap = com.hamyareman.ir.ui.study.ClassPlanStore.load(this)
            com.hamyareman.ir.ui.study.ClassPlanStore.refreshOffCache(this)
            com.hamyareman.ir.ui.study.ClassPlanStore.syncAlarms(
                this, container.reminders, snap, java.time.LocalDate.now(com.hamyareman.ir.platform.core.common.JalaliDate.TEHRAN))
        }
        seedDefaultReminders()
    }

    /**
     * سه یادآور پیش‌فرض ملایم. فقط یک‌بار (اولین اجرا) ساخته می‌شوند و کاربر
     * می‌تواند خاموششان کند؛ یادآور اجباری، ابزار مراقبتی نیست بلکه آزار است.
     */
    private fun seedDefaultReminders() {
        val scheduler = container.reminders
        val existing = scheduler.all()
        if (existing.isEmpty()) {
            listOf(
                Reminder("water-morning", "یک لیوان آب", "صبح‌ها با یک لیوان آب شروع کن 🙂", 9, 30),
                Reminder("study-review", "مرور درس امروز", "ده دقیقه مرور، فردا خیلی راحت‌تر می‌شود.", 18, 0),
                Reminder("calm-evening", "آرام‌سازی شبانه", "چند نفس عمیق و یک کشش کوتاه پیش از خواب.", 21, 30)).forEach { scheduler.upsert(it) }
        }
        // مهاجرت نسخهٔ ۲.۱: این دو نوع یادآور طبق طراحی تازه دیگر وجود ندارند.
        // remove علاوه بر پاک‌کردن state، PendingIntent نسخهٔ قبلی را هم لغو می‌کند.
        scheduler.remove(AI_LESSON_REMINDER_ID)
        scheduler.remove(SchoolAlarmStore.SCHOOL_M)
        scheduler.remove(SchoolAlarmStore.SCHOOL_N)
    }
}
