package com.hamyareman.ir.platform.core.common

/**
 * شناسه‌ی جداول TablesDB (نه Collections قدیمی).
 *
 * این مقادیر «قرارداد» بین کلاینت و `backend/appwrite.json` هستند: اگر در کنسول Appwrite
 * شناسه‌ی دیگری انتخاب کردید، باید هم اینجا و هم در appwrite.json همان مقدار بگذارید
 * (شناسه‌های سفارشی مثل `profiles` مجازند؛ لازم نیست ID تصادفی کنسول باشد).
 */
object TableIds {
    /** نام دیتابیس — از `appwrite.databaseId` در local.properties می‌آید و این مقدار پیش‌فرض است. */
    const val DATABASE = "ZahraDB"

    // --- هویت و پیوند ---
    const val PROFILES = "profiles"
    /** پروفایل دانش‌آموز (ثبت‌نام پس از ورود گوگل) — هر کاربر یک ردیف با userId واحد. */
    const val STUDENT_PROFILES = "student_profiles"
    const val USER_SETTINGS = "user_settings"
    const val FATHER_LINKS = "father_links"
    const val PAIRING_CODES = "pairing_codes"

    // --- ارتباط ---
    const val FATHER_MESSAGES = "father_messages"
    /** آلبوم خاطرات مشترک (زهرا مالک؛ پدر با تأیید زهرا اضافه می‌کند). */
    const val ALBUM_ITEMS = "album_items"
    const val CALL_SESSIONS = "call_sessions"
    const val CALL_SIGNALS = "call_signals"

    // --- داده‌ی قابل‌اشتراک (با opt-in زهرا) ---
    const val WEEKLY_SUMMARIES = "weekly_summaries"
    const val ROUTINE_BLOCKS = "routine_blocks"
    const val WATER_LOGS = "water_logs"
    const val EXERCISE_LOGS = "exercise_logs"
    const val BADGES = "badges"

    // --- داده‌ی صرفاً خصوصی زهرا (هرگز Sync نمی‌شود) ---
    const val CYCLE_ENTRIES = "cycle_entries"
    const val MOOD_ENTRIES = "mood_entries"
    const val JOURNAL_ENTRIES = "journal_entries"
    const val SCREEN_TIME_LOGS = "screen_time_logs"
    const val CHAT_HISTORY = "chat_history"

    // --- محتوا (کاتالوگ خواندنی، نوشتن فقط با نقش مدیر) ---
    const val LESSONS = "lessons"
    const val QUIZZES = "quizzes"
    const val RECIPES = "recipes"
    const val EXERCISES = "exercises"
    const val LEARNING_NODES = "learning_nodes"
    const val ART_PROMPTS = "art_prompts"

    // --- پرامپت ۰۱: حافظه‌ی پیشرفت پلیر ویدیو/صوت ---
    const val LESSON_MEDIA_PROGRESS = "lesson_media_progress"
    const val STUDY_PROGRESS = "study_progress"
    /** نکات درسی — متن/عنوان‌ها؛ فایل جزوه سینک نمی‌شود. */
    const val LESSON_NOTES = "lesson_notes"
    /** وضعیتِ عمومیِ برنامه (برنامه هفتگی، شیفت، کلاس مجازی، تیک‌ها) — کلید → JSON. */
    const val APP_STATE = "app_state"

    /**
     * نگاشتِ «نام کاربری → حساب» (v1.65): ورود با نامِ کاربری و بازیابیِ رمز.
     * دو جور سطر دارد: `u_<هشِ نام کاربری>` (خواندنِ عمومی برای لاگین) و
     * `p_<هشِ userId>` (خصوصی: «نام کاربریِ من» روی هر دستگاه/نصبِ تازه).
     */
    const val USERS = "users"

    /**
     * سفارش‌های اشتراک کارت‌به‌کارت و درخواست استرداد.
     * نوشتن فقط از تابع سرور (کلید API داخل APK نیست).
     */
    const val SUBSCRIPTION_ORDERS = "subscription_orders"

    const val INSTALLMENTS = "installments"

    // --- پرامپت ۰۲: ماژول ورزش/یوگا/تنفس/یادگیری ---
    const val WELLNESS_MOVES = "wellness_moves"
    const val SKETCH_REFERENCES = "sketch_references"
    const val WELLNESS_LOGS = "wellness_logs"

    /**
     * همه‌ی جداولی که واقعاً در Appwrite ساخته می‌شوند
     * (مطابق `backend/appwrite.json` — ۲۸ جدول؛ `subscription_orders` برای
     * صف پرداخت/استرداد). شمارِ دقیق را `PrivacyPolicyTest` نگهبانی می‌کند.
     */
    val serverTables: Set<String> = setOf(
        PROFILES, USER_SETTINGS, FATHER_LINKS, PAIRING_CODES,
        FATHER_MESSAGES, ALBUM_ITEMS, CALL_SESSIONS, CALL_SIGNALS,
        WEEKLY_SUMMARIES, ROUTINE_BLOCKS, WATER_LOGS, EXERCISE_LOGS, BADGES,
        LESSONS, QUIZZES, RECIPES, EXERCISES, LEARNING_NODES, ART_PROMPTS,
        LESSON_MEDIA_PROGRESS, WELLNESS_MOVES, SKETCH_REFERENCES, WELLNESS_LOGS,
        STUDY_PROGRESS, LESSON_NOTES, APP_STATE, USERS, SUBSCRIPTION_ORDERS,
    )

    /** جدول‌های «فقط روی دستگاه» — در سرور هیچ سطری ندارند و ساخته هم نمی‌شوند. */
    val deviceOnlyTables: Set<String> = setOf(
        CYCLE_ENTRIES, MOOD_ENTRIES, JOURNAL_ENTRIES, SCREEN_TIME_LOGS, CHAT_HISTORY,
    )
}

/** سطل‌های Storage. دسترسی پدر فقط به سطل مشترک و فقط از راه پیوند فعال است. */
object BucketIds {
    const val HEART_MEDIA = "heart-to-heart-media"
    const val AVATARS = "avatars"
    const val FATHER_ALBUM = "father-album"
    const val ZAHRA_PRIVATE = "zahra-private"
    /**
     * پرامپت ۰۲: تصاویر مرجع و فایل‌های صوتی راهنما برای حرکات و مرجع‌های سیاه‌قلم.
     * نام bucket در کنسول: wellness-media
     */
    const val WELLNESS_MEDIA = "6aa1eaae00303400117b"
}

/** شناسه‌ی توابع سرور (Function ID در کنسول Appwrite). */
object FunctionIds {
    /**
     * قفل پایه/دستگاه + صف اشتراک/ادمین.
     * پلن رایگان سقف توابع دارد؛ روی همان `ai-companion` سوار است
     * (فیلد `action` یا `grade`+`deviceId`). کلید سرور داخل تابع است، نه APK.
     */
    const val USER_BOOTSTRAP = "ai-companion"
    const val ADMIN_OPS = "ai-companion"
    const val PAIRING = "pairing"
    const val WEEKLY_SUMMARY = "weekly-summary"
    /** لایه‌ی AI «همراه زهرا» — proxy سمت سرور؛ کلید مدل هرگز در اپ نیست. */
    const val AI_COMPANION = "ai-companion"

    /**
     * لایه‌ی AI «معلم خصوصی» مطالعه — رفع اشکال مفهومی؛ آفلاین بدون آن کامل است.
     * روی همان فانکشن ai-companion سوار است (mode=study-tutor) چون پلن رایگان
     * Appwrite سقف تعداد functions دارد.
     */
    const val STUDY_TUTOR = "ai-companion"

    /**
     * هشدار «کمک می‌خوام» به پدر: متن و مقصد را سرور می‌سازد، پیوند فعال را بررسی
     * می‌کند و شماره‌های اضطراری را حتی بدون پیوند برمی‌گرداند.
     */
    const val NOTIFY_GUARDIAN = "notify-guardian"

    /** «درس امروز» با ترتیب و پیش‌نیازهای سمت سرور (ساعت دستگاه نقشی ندارد). */
    const val LESSON_OF_THE_DAY = "lesson-of-the-day"

    /** اثر انگشت کاتالوگ؛ اگر عوض نشده باشد دانلود کامل انجام نمی‌شود. */
    const val CATALOG_DIGEST = "catalog-digest"

    /** خلاصه‌ی روزانه‌ی opt-in برای پدر (فقط داده‌های قابل اشتراک). */
    const val DAILY_CHECKIN = "daily-checkin"

    /** تأیید/رد خاطره‌ی پدر در آلبوم، با بررسی مالکیت سمت سرور. */
    const val ALBUM_CONSENT = "album-consent"

    /**
     * پرامپت ۰۲: تولید تصویر مرجع سیاه‌قلم (pencil sketch) برای تمرین نقاشی.
     * ورودی: subject + level (۴-۱۰). خروجی: URL تصویر سیاه‌وسفید.
     */
    const val GENERATE_SKETCH_REFERENCE = "generate-sketch-reference"
}

/**
 * جداولی که به‌هیچ‌وجه از دستگاه زهرا بیرون نمی‌روند.
 * این لیست هم در SyncEngine و هم در لایه‌ی دسترسی سرور (Permissions) باید رعایت شود.
 */
object PrivacyPolicy {
    val neverSyncTables: Set<String> = setOf(
        TableIds.CYCLE_ENTRIES,
        TableIds.MOOD_ENTRIES,
        TableIds.JOURNAL_ENTRIES,
        TableIds.SCREEN_TIME_LOGS,
        TableIds.CHAT_HISTORY,
    )

    /** داده‌هایی که در خلاصه‌ی هفتگی (فقط با opt-in) به پدر نشان داده می‌شود. */
    val weeklyShareableTables: Set<String> = setOf(
        TableIds.ROUTINE_BLOCKS,
        TableIds.WATER_LOGS,
        TableIds.EXERCISE_LOGS,
        TableIds.BADGES,
    )

    fun isNeverSynced(table: String): Boolean = table in neverSyncTables
}
