package com.hamyareman.ir.ui.wellness

import com.hamyareman.ir.ui.navigation.Screen

/**
 * درخت منوی «سلامتی و آرامش» مطابق فهرست محصول.
 * هاب‌ها از روی همین کاتالوگ ساخته می‌شوند؛ حرکت‌های از قبل موجود با
 * [PracticeItem.wellnessSlug] یا [PracticeItem.route] به صفحه‌ی قبلی وصل می‌شوند.
 *
 * معماری صفحه‌ها: صفحهٔ اول همیشه کاشی؛ صفحهٔ دوم کاشی یا «آکاردئون + کاشی».
 * هر گروهی که در [PracticeGroup.childGroupIds] یا [PracticeItem.childGroupId] زیرگروه داشته باشد،
 * در صفحهٔ دوم به‌صورت آکاردئون نمایش داده می‌شود (صفحهٔ واسط ساخته نمی‌شود).
 */
data class PracticeItem(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val minutes: String = "",
    val steps: List<String> = emptyList(),
    val body: String = "",
    val route: String = "",
    val wellnessSlug: String = "",
    val childGroupId: String = "",
)

data class PracticeGroup(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val items: List<PracticeItem> = emptyList(),
    val childGroupIds: List<String> = emptyList(),
)

object WellnessMenu {

    /** آیکون جدیدِ تمرینات کوتاه بین دروس. */
    const val BETWEEN_ICON = "⚡"

    fun group(id: String): PracticeGroup? = groups[id]
    fun item(id: String): PracticeItem? = items[id]
    fun groupsOf(ids: List<String>): List<PracticeGroup> = ids.mapNotNull { groups[it] }

    val mindfulnessIds = listOf("mf-presence", "mf-hypnosis", "mf-thoughts", "mf-social", "mf-journal", "mf-learn")

    // «آرام‌سازی پیش از خواب» (cl-bedtime) طبق ساختار نهایی از «کسب آرامش» حذف شد؛
    // مقصد درست آن سلامتی > خواب > آماده‌سازی آرام خواب است.
    val calmIds = listOf("cl-breath", "cl-pmr", "cl-journey", "cl-visual", "cl-story", "cl-sounds")
    val betweenIds = listOf("bl-desk", "bl-eyes", "bl-body", "bl-focus")
    val skillsIds = SkillsCatalog.ids

    private fun i(
        id: String,
        emoji: String,
        title: String,
        subtitle: String,
        minutes: String = "",
        steps: List<String> = emptyList(),
        body: String = "",
        route: String = "",
        slug: String = "",
        child: String = "",
    ) = PracticeItem(id, emoji, title, subtitle, minutes, steps, body, route, slug, child)

    private fun g(
        id: String,
        emoji: String,
        title: String,
        subtitle: String,
        items: List<PracticeItem> = emptyList(),
        children: List<String> = emptyList(),
    ) = PracticeGroup(id, emoji, title, subtitle, items, children)

    private val allGroups: List<PracticeGroup> = listOf(
        // ---- ذهن‌آگاهی ----
        g(
            "mf-presence", "🌱", "تمرین‌های کوتاه حضور ذهن", "۳ تا ۵ دقیقه؛ همین‌جا و همین حالا",
            items = listOf(
                i(
                    "mf-body-scan", "🫀", "اسکن بدن", "۳ تا ۵ دقیقه از سر تا پا", "۴ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "mf-grounding", "🖐️", "زمین‌گیری ۵-۴-۳-۲-۱", "حواس پنج‌گانه برای برگشتن به الان", "۳ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "mf-one-minute", "⏳", "یک دقیقه نفس", "بین کارها؛ فقط شصت ثانیه", "۱ دقیقه",
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "mf-hypnosis", "🌙", "خودهیپنوز سالم",
            "آرام‌سازی + تلقین مثبت + تجسم — برای تمرکز، امتحان، فکر و رابطه",
            children = listOf("mf-hyp-study", "mf-hyp-exam", "mf-hyp-thoughts", "mf-hyp-social", "mf-hyp-sleep"),
        ),
        g(
            "mf-hyp-study", "📚", "برای ترغیب به مطالعه", "قبل از نشستن سر درس",
            items = listOf(
                i(
                    "hyp-focus", "🎯", "من می‌تونم تمرکز کنم", "القای آرامش پیش از درس", "۵ دقیقه",
                    route = Screen.ContentHtml.of("mh-study-01"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-chapter", "✅", "تجسم کامل‌کردن یک فصل", "دیدنِ تمام‌شدن تمرین", "۴ دقیقه",
                    route = Screen.ContentHtml.of("mh-study-02"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-procrastinate", "🚪", "رهاسازی اهمال‌کاری", "شروع ۲ دقیقه‌ای به‌جای منتظر ماندن", "۳ دقیقه",
                    route = Screen.ContentHtml.of("mh-study-03"),
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "mf-hyp-exam", "📝", "برای اعتمادبه‌نفس امتحان", "اضطرابِ جلسه را کم کن",
            items = listOf(
                i(
                    "hyp-enter", "🏫", "تجسم ورود آروم به جلسه", "راهرو، صندلی، برگه", "۵ دقیقه",
                    route = Screen.ContentHtml.of("mh-exam-01"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-memory", "🧠", "حافظه‌ام در دسترسمه", "تلقینِ بازیابی آرام", "۴ دقیقه",
                    route = Screen.ContentHtml.of("mh-exam-02"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-anxiety", "🌊", "مدیریت اضطراب پیش از امتحان", "Test Anxiety — موج می‌آید و می‌رود", "۵ دقیقه",
                    route = Screen.ContentHtml.of("mh-exam-03"),
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "mf-hyp-thoughts", "💬", "برای افکار سالم و گفت‌وگوی درونی", "جایگزینی مهربان، نه شعار توخالی",
            items = listOf(
                i(
                    "hyp-reframe", "🔄", "بازنویسی فکر منفی", "Cognitive Reframing واقع‌بینانه", "۵ دقیقه",
                    route = Screen.ContentHtml.of("mh-thoughts-01"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-esteem", "🌟", "تلقین عزت‌نفس", "برای نوجوان؛ بدون مقایسه", "۴ دقیقه",
                    route = Screen.ContentHtml.of("mh-thoughts-02"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-compare", "🕊️", "رهاسازی مقایسه با دیگران", "مسیر خودت", "۴ دقیقه",
                    route = Screen.ContentHtml.of("mh-thoughts-03"),
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "mf-hyp-social", "🤝", "برای مسائل اجتماعی و روابط", "گفت‌وگوی سخت، همدلی، رها کردن دلخوری",
            items = listOf(
                i(
                    "hyp-talk", "🗣️", "آرامش پیش از گفت‌وگوی سخت", "با دوست، خانواده یا معلم", "۴ دقیقه",
                    route = Screen.ContentHtml.of("mh-social-01"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-empathy", "👁️", "دیدن از زاویه‌ی دیگری", "تمرین همدلی", "۵ دقیقه",
                    route = Screen.ContentHtml.of("mh-social-02"),
                    steps = emptyList(),
                ),
                i(
                    "hyp-anger", "🔥", "رهاسازی خشم کوچک", "دلخوری را زمین بگذار", "۴ دقیقه",
                    route = Screen.ContentHtml.of("mh-social-03"),
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "mf-hyp-sleep", "😴", "برای خواب بهتر", "پل به آرام‌سازی پیش از خواب",
            items = listOf(
                i(
                    "hyp-sleep", "🛏️", "آرام‌سازی پیش از خواب با تلقین", "بدن سنگین، فکر سبک", "۶ دقیقه",
                    steps = emptyList(),
                    route = Screen.SleepNight.route,
                ),
            ),
        ),
        g(
            "mf-thoughts", "🪞", "افکار سالم و گفت‌وگوی درونی مثبت", "تشخیص، بازنویسی، سه نقطه‌قوت",
            items = listOf(
                i(
                    "th-auto", "⚡", "تشخیص فکر منفی خودکار", "اول ببین، بعد عوض کن", "۳ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "th-rewrite", "✏️", "بازنویسی جمله‌ی منفی", "واقع‌بینانه، نه شعار", "۴ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "th-strengths", "📒", "سه نقطه‌قوت امروز", "شکرگزاریِ عملی", "۴ دقیقه",
                    body = "",
                    route = Screen.Journal.route,
                ),
            ),
        ),
        g(
            "mf-social", "🌍", "آگاهی اجتماعی و مسائل اجتماعی", "احساس، زاویه دید، مهربانی",
            items = listOf(
                i(
                    "so-label", "🏷️", "نام‌گذاری احساس", "Emotion Labeling", "۲ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "so-perspective", "🔭", "دیدن از دید دیگران", "Perspective Taking", "۵ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "so-issue", "📰", "مسئله‌ی اجتماعی روز", "پرسش‌های تأملی، نه شعار", "۶ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "so-kindness", "💛", "مهربانی با غریبه", "یک لطف کوچک امروز — از پرسش روزانه", "۲ دقیقه",
                    body = "",
                ),
            ),
        ),
        g(
            "mf-journal", "📓", "دفترچه‌های من", "ثبت سریع شکرگزاری روزانه",
            items = listOf(
                i("jo-free", "✍️", "دفترچه‌ی من", "حرف‌های بلندتر؛ فقط برای خودت", route = Screen.Journal.route),
            ),
        ),
        g(
            "mf-learn", "🎓", "تمرینات تمرکز و یادگیری", "پومودورو، مرور، فاینمن و شروع ۲ دقیقه‌ای",
            items = listOf(
                i("ln-pomo", "🍅", "تکنیک پومودورو", "۲۵ دقیقه کار، ۵ دقیقه استراحت", slug = "learn-pomodoro"),
                i("ln-spaced", "📆", "تکرار فاصله‌دار", "مرور با فاصله‌ی روبه‌رشد", slug = "learn-spaced-repetition"),
                i("ln-feynman", "🧒", "روش فاینمن", "به زبان ساده توضیح بده", slug = "learn-feynman"),
                i("ln-map", "🗺️", "نقشه‌ی ذهنی", "موضوع در مرکز، شاخه‌ها دورش", slug = "learn-mind-map"),
                i("ln-recall", "🔁", "فعال‌سازی حافظه", "بدون نگاه‌کردن بگو", slug = "learn-active-recall"),
                i(
                    "ln-two-min", "⏱️", "تکنیک ۲ دقیقه", "شروع کار سخت فقط با دو دقیقه‌ی اول", "۲ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "ln-quiz", "❓", "مرور فعال با سؤال‌سازی", "Self-Quizzing", "۵ دقیقه",
                    steps = emptyList(),
                ),
            ),
        ),

        // ---- کسب آرامش ----
        g(
            "cl-breath", "🌬️", "تنفس آرام‌بخش", "۴-۷-۸، جعبه‌ای، نادی، شیر",
            items = listOf(
                i("br-478", "8️⃣", "تنفس ۴-۷-۸", "دم ۴، نگه ۷، بازدم ۸", slug = "breath-4-7-8"),
                i("br-box", "⬛", "تنفس جعبه‌ای", "۴-۴-۴-۴", slug = "breath-box"),
                i("br-nadi", "👃", "تنفس بینی متناوب", "Nadi Shodhana", slug = "breath-nadi"),
                i("br-lion", "🦁", "تنفس شیر", "رهاسازی فک و تنش", slug = "breath-lion"),
            ),
        ),
        g(
            "cl-pmr", "💆", "آرام‌سازی عضلانی پیش‌رونده", "سفت کن، رها کن — Progressive Muscle Relaxation",
            items = listOf(
                i(
                    "pmr-full", "🧘", "نسخه‌ی کامل سر تا پا", "۱۰ تا ۱۲ دقیقه", "۱۲ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "pmr-short", "🙌", "نسخه‌ی کوتاه دست و شانه", "وسط روز، پشت میز", "۳ دقیقه",
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "cl-journey", "🚶", "سفر ذهنی", "روایت با شروع، میانه و پایان",
            items = listOf(
                i(
                    "jn-forest", "🌲", "جنگل آرام", "باد، برگ، پرنده", "۷ دقیقه",
                    route = Screen.ContentHtml.of("cj-01"),
                    body = "",
                    steps = emptyList(),
                ),
                i(
                    "jn-beach", "🌊", "ساحل و امواج", "دریا، شن گرم", "۷ دقیقه",
                    route = Screen.ContentHtml.of("cj-02"),
                    body = "",
                    steps = emptyList(),
                ),
                i(
                    "jn-cabin", "🔥", "کلبه‌ی کوهستانی برفی", "سکوت، آتش، پتو", "۷ دقیقه",
                    route = Screen.ContentHtml.of("cj-03"),
                    body = "",
                    steps = emptyList(),
                ),
                i(
                    "jn-garden", "🌸", "باغ مخفی", "رایحه‌ی گل، صدای آب", "۶ دقیقه",
                    route = Screen.ContentHtml.of("cj-04"),
                    body = "",
                    steps = emptyList(),
                ),
                i(
                    "jn-boat", "🛶", "قایق روی رودخانه‌ی آرام", "جریان تو را می‌برد", "۶ دقیقه",
                    route = Screen.ContentHtml.of("cj-05"),
                    body = "",
                    steps = emptyList(),
                ),
                i(
                    "jn-rain", "🌧️", "پیاده‌روی زیر باران بهاری", "بوی خاک، قطره‌های نرم", "۶ دقیقه",
                    route = Screen.ContentHtml.of("cj-06"),
                    body = "",
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "cl-visual", "🎨", "تصویرسازی ذهنی", "هدف‌محور؛ برای حالت یا نتیجه",
            items = listOf(
                i(
                    "vz-relax", "🌫️", "مه رنگی آرامش", "Relaxation Imagery", "۵ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "vz-perf", "🏅", "تصویرسازی عملکردی", "موفقیت در امتحان یا ارائه", "۵ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "vz-process", "🪜", "تصویرسازی مرحله‌به‌مرحله", "Process Imagery — قدم‌های درس", "۵ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "vz-end", "🌅", "تصویرسازی حالت‌نهایی", "خودِ رسیده‌به هدف", "۴ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "vz-bubble", "🫧", "حباب محافظ", "برای اضطراب اجتماعی", "۴ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "vz-box", "📦", "جعبه‌ی نگرانی", "Worry Box — بگذار برای بعد", "۳ دقیقه",
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "cl-story", "📖", "قصه‌ی شب", "روایت کامل برای خواب؛ صدا هم در آرام‌سازی پیش از خواب",
            items = listOf(
                i(
                    "st-nature", "🌿", "قصه‌های طبیعت", "جنگل، دریا، کوه، دشت", "۸ دقیقه",
                    body = "",
                ),
                i(
                    "st-myth", "📜", "افسانه‌ی آرام", "روایت کوتاه ایرانی", "۸ دقیقه",
                    body = "",
                ),
                i(
                    "st-inspire", "🕯️", "قصه‌ی پشتکار آرام", "غلبه بر سختی، بدون فریاد", "۸ دقیقه",
                    body = "",
                ),
                i(
                    "st-nostalgia", "🏡", "خانه‌ی مادربزرگ", "نوستالژی آرام", "۸ دقیقه",
                    body = "",
                ),
                i(
                    "st-cloud", "☁️", "شهر ابری", "سفر تخیلی آرام", "۸ دقیقه",
                    body = "",
                ),
                i(
                    "st-school", "🎒", "روزی که تمام شد", "تم مدرسه؛ فردا شروع تازه", "۶ دقیقه",
                    body = "",
                ),
            ),
        ),
        g(
            "cl-sounds", "🎧", "صداهای آرامش‌بخش", "صدای دلخواهت را انتخاب کن و فضای آرام خودت را بساز.",
            items = listOf(
                i(
                    "soundscape-full", "🎧", "نجواهای آرام‌بخش طبیعت",
                    "موسیقی کامل از سرور، با تایمر خواب.",
                    route = Screen.BackgroundMusic.route,
                ),
            ),
        ),
        g(
            "cl-bedtime", "🌜", "آرام‌سازی پیش از خواب", "پل به قصه‌ی شب و تنفس خواب",
            items = listOf(
                i("bd-night", "🎧", "آرامش قبل خواب", "صوت، قصه، تنفس", route = Screen.SleepNight.route),
                i("bd-breath", "🌬️", "تنفس قبل خواب", "۴ دم، ۶ بازدم", slug = "breath-bedtime"),
            ),
        ),

        // ---- بین دروس (آیکون جدید ⚡) ----
        g(
            "bl-desk", BETWEEN_ICON, "حرکات کشش سریع پشت میز", "گردن، شانه، مچ — ۱ تا ۲ دقیقه",
            items = listOf(
                i("bl-neck", BETWEEN_ICON, "کشش گردن ۴ جهت", "قبلاً در حرکات سلامتی", slug = "ex-neck-4way"),
                i("bl-shoulder", BETWEEN_ICON, "کشش شانه ضربدری", "قبلاً در حرکات سلامتی", slug = "ex-shoulder-cross"),
                i("bl-wrist", BETWEEN_ICON, "چرخش مچ دست و پا", "قبلاً در حرکات سلامتی", slug = "ex-wrist-ankle"),
            ),
        ),
        g(
            "bl-eyes", "👁️", "استراحت چشم", "برای خستگی صفحه",
            items = listOf(
                i(
                    "bl-202020", BETWEEN_ICON, "قانون ۲۰-۲۰-۲۰", "هر ۲۰ دقیقه، ۲۰ ثانیه به فاصله‌ی حدود ۶ متر", "۲۰ ثانیه",
                    steps = emptyList(),
                ),
                i(
                    "bl-focus", BETWEEN_ICON, "فوکوس نزدیک/دور", "نوک انگشت و افق", "۱ دقیقه",
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "bl-body", "🧍", "فعال‌سازی سریع بدن", "ایستادن، جا به‌جا شدن، تکان‌دادن",
            items = listOf(
                i(
                    "bl-stand", BETWEEN_ICON, "ایستادن و کشش کامل", "دست‌ها بالا، قد بکش", "۱ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "bl-walk", BETWEEN_ICON, "راه‌رفتن درجا / پله‌ی کوتاه", "خون‌رسانی بدون عرق", "۲ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "bl-shake", BETWEEN_ICON, "تکان دادن دست و پا", "Shake it off", "۳۰ ثانیه",
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "bl-focus", "🎯", "بازگشت به تمرکز", "بعد از استراحت، قبل از صفحه‌ی بعد",
            items = listOf(
                i("bl-box", BETWEEN_ICON, "تنفس شمارشی ۴-۴-۴", "قبلاً در تنفس سلامتی", slug = "breath-counting"),
                i(
                    "bl-sentence", BETWEEN_ICON, "یک جمله‌ی تمرکز", "الان فقط همینو انجام بده", "۳۰ ثانیه",
                    steps = emptyList(),
                ),
            ),
        ),

        // ---- دوره‌ی پریود (برای هاب سلامتی) ----
        g(
            "pd-yoga", "🧘", "یوگای دوره‌ی قاعدگی", "ملایم؛ اگر درد زیاد شد بایست",
            items = listOf(
                i("pd-catcow", "🐱", "گربه-گاو", "تمرین تعاملی", route = Screen.ContentHtml.of("yga-02")),
                i("pd-child", "🙇", "حالت کودک", "تمرین تعاملی", route = Screen.ContentHtml.of("yga-01")),
                i(
                    "pd-butterfly", "🦋", "پروانه‌ی خوابیده", "Reclined Butterfly", "۲ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "pd-twist", "🌀", "پیچش خوابیده", "Supine Twist", "۲ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "pd-legs", "🦵", "پا به دیوار", "Legs-Up-the-Wall", "۳ دقیقه",
                    steps = emptyList(),
                ),
            ),
        ),
        g(
            "pd-breath", "🌬️", "تنفس آرام‌بخش برای کرامپ", "شکمی و ۴-۷-۸",
            items = listOf(
                i("pd-diaph", "🫧", "تنفس شکمی", "قبلاً موجود", slug = "breath-diaphragm"),
                i("pd-478", "8️⃣", "تنفس ۴-۷-۸", "قبلاً موجود", slug = "breath-4-7-8"),
            ),
        ),
        g(
            "pd-stretch", "🔥", "کشش‌های هدفمند", "کمر، لگن، گرما",
            items = listOf(
                i(
                    "pd-hips", "🦴", "کشش کمر و لگن", "ملایم، بدون فشار", "۳ دقیقه",
                    steps = emptyList(),
                ),
                i(
                    "pd-heat", "🌡️", "کمپرس گرم + وضعیت تسکین", "گرما + دراز کشیدن", "۱۰ دقیقه",
                    body = "",
                ),
            ),
        ),
        g(
            "pd-food", "🍎", "راهنمای تغذیه‌ی هر فاز", "آهن، منیزیم، آب — نه رژیم سخت",
            items = listOf(
                i(
                    "pd-food-period", "🩸", "فاز قاعدگی", "آهن و گرما",
                    body = "",
                ),
                i(
                    "pd-food-after", "🌱", "بعد از قاعدگی", "انرژی آرام برمی‌گردد",
                    body = "",
                ),
                i(
                    "pd-food-mid", "☀️", "میانه‌ی چرخه", "تمرکز بهتر",
                    body = "",
                ),
                i(
                    "pd-food-pms", "🌙", "پیش‌قاعدگی", "منیزیم و خواب",
                    body = "",
                ),
            ),
        ),
        g(
            "hl-cycle", "🌸", "چرخه ماهانه", "تقویم، علائم، تنفس درد و تمرین ملایم",
            items = listOf(
                i("cy-cal", "📅", "تقویم ماهانه با فازبندی", "پیش‌قاعدگی / قاعدگی / پس از قاعدگی / میانه", route = Screen.CycleCal.route),
                i("cy-log", "📝", "علائم و یادداشت روزانه", "درد، خلق، جریان، سردرد، نفخ", route = Screen.CycleLog.route),
                i("cy-today", "💗", "امروز بدنت چی می‌خواد", "راهنما بر اساس فاز همان روز", route = Screen.CycleToday.route),
                i("cy-pain", "🌬️", "تنفس برای کرامپ و کنترل درد", "تنفس شکمی آرام‌بخش", child = "pd-breath"),
            ),
            children = listOf("pd-yoga", "pd-stretch", "pd-food"),
        ),
        g(
            "hl-nutrition", "🥗", "آب و تغذیه", "تمرکز و انرژی درس",
            items = listOf(
                i("hl-water", "💧", "یادآور نوشیدن آب", "لیوان‌های امروز", route = Screen.Water.route),
                i(
                    "hl-focus-food", "🧠", "تغذیه‌ی سالم برای تمرکز", "راهنمای ساده، نه رژیم",
                    body = "",
                ),
            ),
        ),
        g(
            "hl-sleep", "😴", "خواب", "ثبت، قصه، بشنو و بخواب، تنفس و آماده‌سازی خواب",
            items = listOf(
                i("hl-sleep-log", "🌙", "ثبت خواب و رشته", "الگوی خواب خود را ثبت کنید و پیشرفتتان را ببینید.", route = Screen.SleepLog.route),
                i("hl-sleep-story", "📖", "قصه‌ی شب", "داستان‌های آرامش‌بخش برای خواب بهتر", child = "cl-story"),
                i("hl-sleep-listen", "🎧", "بشنو و بخواب", "موسیقی و صدای طبیعت برای آرامش ذهن", route = Screen.SleepNight.route),
                i(
                    "hl-sleep-breath", "🌬️", "تنفس پیش از خواب", "با چند نفس عمیق، آرام شوید.",
                    route = Screen.SleepBreath.route,
                ),
            ),
            // آکاردئون: «آماده‌سازی آرام خواب» — کاشی‌های محتوا مستقیم زیر آن باز می‌شوند.
            children = listOf("hl-sleep-prep"),
        ),
        // HTML هر دو مورد روی ParsPack:
        //   Bucket/Html-files/app/health/sleep/preparation/01-pre-sleep-body-relaxation.html
        //   Bucket/Html-files/app/health/sleep/preparation/02-release-day-thoughts.html
        g(
            "hl-sleep-prep", "🛏️", "آماده‌سازی آرام خواب", "بدن و ذهن خود را برای خوابی عمیق‌تر آماده کنید.",
            items = listOf(
                i(
                    "sp-body", "🧘", "آرام‌سازی بدنی قبل خواب", "۲۸ دقیقه", "۲۸ دقیقه",
                    route = Screen.ContentHtml.of("sp-body"),
                ),
                i(
                    "sp-release", "🍵", "رهاکردن فکرهای روز", "۲۵ دقیقه", "۲۵ دقیقه",
                    route = Screen.ContentHtml.of("sp-release"),
                ),
            ),
        ),
    ) + SkillsCatalog.groups

    private val groups: Map<String, PracticeGroup> = allGroups.associateBy { it.id }

    private val items: Map<String, PracticeItem> =
        allGroups.flatMap { it.items }.associateBy { it.id }

    private val mindfulnessInternalGroups: Set<String> = buildSet {
        addAll(mindfulnessIds)
        mindfulnessIds.mapNotNull { groups[it] }.flatMapTo(this) { it.childGroupIds }
    }
    private val placeholderGroups = mindfulnessInternalGroups + setOf(
        "cl-pmr", "cl-journey", "cl-visual", "cl-bedtime", "cl-story",
    )
    private val explicitPlaceholderItems = setOf(
        "pd-hips", "pd-heat", "pd-food-period", "pd-food-after", "pd-food-mid", "pd-food-pms",
        "hl-focus-food", "mf-body-scan", "mf-grounding", "mf-one-minute",
    )

    /** صفحات داخلیِ این شاخه‌ها عمداً بدون قاب عنوان/فلش خود صفحه نمایش داده می‌شوند. */
    fun hideInternalChromeForGroup(groupId: String): Boolean =
        groupId in mindfulnessInternalGroups || groupId in setOf("cl-pmr", "cl-journey", "cl-visual", "cl-bedtime", "cl-story")

    /** محتوای قبلی این مقصدها بنا بر تصمیم محصول برداشته و با جای‌نگهدار عوض شده است. */
    fun isPlaceholderItem(itemId: String): Boolean {
        if (itemId in explicitPlaceholderItems) return true
        return allGroups.any { itemId in it.items.map(PracticeItem::id) && it.id in placeholderGroups }
    }

    fun hideInternalChromeForItem(itemId: String): Boolean = isPlaceholderItem(itemId)

    fun resolveInstructions(item: PracticeItem): Pair<String, List<String>> {
        if (item.wellnessSlug.isNotBlank()) {
            val move = WellnessCatalog.bySlug(item.wellnessSlug)
            if (move != null) {
                val steps = emptyList()}
        }
        return item.body to item.steps
    }
}
