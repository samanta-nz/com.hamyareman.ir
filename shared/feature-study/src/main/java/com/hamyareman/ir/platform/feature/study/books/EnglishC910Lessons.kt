package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.StudyPack

/**
 * درس‌های ۲ تا ۹ کتاب «زبان انگلیسی پایه نهم» (C910) — یعنی Lesson 2 تا Lesson 6
 * به‌همراه سه بخشِ مرور (Review 1/2/3) که در فهرست رسمی کتاب، پکِ مستقل دارند.
 *
 * ترتیب و گرامرِ هر درس بر اساس سرفصلِ رسمیِ Prospect 3:
 *   Lesson 1 شخصیت (فعل to be) · Lesson 2 سفر (حال استمراری) · Lesson 3 جشن‌ها و مراسم
 *   (حال ساده + ضمایر مفعولی و صفات ملکی و قیدهای تکرار) · Lesson 4 خدمات (سؤالات Wh)
 *   · Lesson 5 رسانه (گذشتهٔ ساده: افعال باقاعده) · Lesson 6 سلامت و آسیب‌ها (گذشتهٔ
 *   ساده: افعال بی‌قاعده).
 *
 * مکالمه‌های این درس‌ها «نمونهٔ الگو» هستند (نه نقل‌قولِ متنِ کتاب) تا دانش‌آموز با
 * ساختارِ همان درس تمرین کند؛ درس ۱ در `EnglishC910.kt` مانده است.
 */
internal object EnglishC910Lessons {

    private const val BOOK = "زبان انگلیسی پایه نهم"

    // ---- سازنده‌های کوتاه (همان قالبِ درس ۱) ----

    private fun sec(id: String, title: String, kind: String, body: String) =
        StudyPack.Section(id = id, title = title, kind = kind, body = body)

    private fun card(id: String, front: String, back: String, topic: String) =
        StudyPack.Flashcard(id, front, back, topic, "")

    private fun mcq(
        id: String, text: String, options: List<String>, answer: String,
        why: String, topic: String, level: Int, ref: String,
    ) = StudyPack.Question(id, "mcq", text, options, answer, why, topic, level, ref)

    private fun short(
        id: String, text: String, answer: String,
        why: String, topic: String, level: Int, ref: String,
    ) = StudyPack.Question(id, "short", text, emptyList(), answer, why, topic, level, ref)

    private fun pack(
        lessonId: String,
        title: String,
        sections: List<StudyPack.Section>,
        cards: List<StudyPack.Flashcard>,
        questions: List<StudyPack.Question>,
        solutions: List<StudyPack.Solution>,
        summary: String,
        examTips: String,
    ) = StudyPack(
        packId = "C910_$lessonId",
        bookCode = "C910",
        lessonId = lessonId,
        title = title,
        bookTitle = BOOK,
        pdfFileName = "C910_${lessonId}_BOOK.pdf",
        audioFileId = "C910_${lessonId}_AUDIO.mp3",
        sections = sections,
        flashcards = cards,
        questions = questions,
        solutions = solutions,
        summary = summary,
        examTips = examTips,
    )

    /** Lesson 2 — Travel (سفر): حال استمراری + واژه‌های سفر. */
    private fun l02() = pack(
        lessonId = "L02",
        title = "Lesson 2 — Travel (سفر)",
        sections = listOf(
            sec(
                "s1", "نمونه مکالمه — الان کجا می‌روی؟", "important",
                "A: Hi Nima! Where are you going?\n" +
                    "B: I'm going to the airport. My uncle is arriving from Shiraz today.\n" +
                    "A: Are you waiting for him alone?\n" +
                    "B: No, my father is coming with me. He's buying the tickets now.\n" +
                    "A: What time is the flight?\n" +
                    "B: It's at 6. We're leaving home at 4.\n" +
                    "ترجمه: سلام نیما! کجا می‌روی؟ — به فرودگاه می‌روم؛ عمویم امروز از شیراز می‌آید. — تنها منتظرش می‌مانی؟ — نه، پدرم با من می‌آید؛ او همین حالا بلیت‌ها را می‌خرد. — پرواز ساعت چند است؟ — ساعت ۶ است؛ ساعت ۴ از خانه راه می‌افتیم.\n" +
                    "📌 نکته‌ی امتحانی: «الان در حال انجام شدن» ⇒ حال استمراری (am/is/are + فعل + ing)، نه حال ساده.",
            ),
            sec(
                "s2", "گرامر: حال استمراری (Present Continuous)", "important",
                "ساختار: فاعل + am/is/are + فعل + ing\n" +
                    "I am traveling. / He is packing. / They are waiting.\n" +
                    "منفی: I'm not traveling. / She isn't packing. / We aren't waiting.\n" +
                    "پرسشی (جابجایی to be): Are you traveling? — Yes, I am. / No, I'm not.\n" +
                    "Is he packing? — Yes, he is. / No, he isn't.\n" +
                    "قیدهای نشانه‌دار: now، right now، at the moment، today، Look!، Listen!\n" +
                    "🔤 املای ing: ۱) بیشتر فعل‌ها فقط ing: go→going، wait→waiting. ۲) فعل‌های پایانیِ e، حذفِ e: make→making، take→taking، write→writing. ۳) یک‌بخشیِ پایانیِ CVC، دوبل‌شدنِ حرف آخر: run→running، sit→sitting، swim→swimming، get→getting. ۴) پایانیِ ie → y+ing: lie→lying.\n" +
                    "📌 نکته‌ی امتحانی: فعل‌های حالتی (know, like, want, need, understand) معمولاً ing نمی‌گیرند.",
            ),
            sec(
                "s3", "واژه‌های سفر", "important",
                "trip (سفر) · journey (سفرِ طولانی) · travel (سفرکردن) · tourist (گردشگر) · ticket (بلیت) · passport (گذرنامه) · visa (روادید) · luggage (چمدان‌ها) · suitcase (چمدان) · boarding pass (کارت پرواز) · flight (پرواز) · airport (فرودگاه) · station (ایستگاه) · timetable (برنامهٔ زمانی) · destination (مقصد) · hotel (هتل) · receptionist (مسئول پذیرش) · souvenir (سوغات) · map (نقشه) · guide (راهنما)\n" +
                    "عبارت‌های کاربردی: buy a ticket · check the passport · check in · check the timetable · take off (از زمین برخاستن) · land (فرود آمدن) · exchange money · fill out the form · book a hotel · pack for a trip · talk to a receptionist · meet the guide",
            ),
            sec(
                "s4", "حال ساده یا حال استمراری؟ (اشتباهِ رایج)", "note",
                "حال ساده = کارِ همیشگی و عادت: I go to school every day. / He travels every summer.\n" +
                    "حال استمراری = کارِ همین حالا: I'm going to school now. / He's traveling this week.\n" +
                    "نمونه: «هر سال به مشهد می‌رویم» → We go to Mashhad every year. «الان داریم به مشهد می‌رویم» → We are going to Mashhad now.\n" +
                    "⚠️ فعلِ to be با ing نمی‌آید: I am being happy ❌ → I am happy ✓.\n" +
                    "📌 نکته‌ی امتحانی: قیدِ زمان را بگرد؛ اگر every day/every year/usually بود، حال ساده می‌خواهد.",
            ),
        ),
        cards = listOf(
            card("c1", "ساختار حال استمراری چیست؟", "فاعل + am/is/are + فعل + ing — مثل He is packing.", "گرامر"),
            card("c2", "چرا run به running تبدیل می‌شود؟", "چون یک‌هجایی و پایانیِ CVC است؛ حرف آخر دوبل می‌شود.", "املای ing"),
            card("c3", "معنی take off و land؟", "take off: از زمین بلند شدن (هواپیما)؛ land: فرود آمدن.", "واژگان سفر"),
            card("c4", "«I'm checking in» یعنی چه؟", "دارم کارهای پذیرش/چک‌این (مثلاً در فرودگاه یا هتل) را انجام می‌دهم.", "واژگان سفر"),
            card("c5", "«هر جمعه به استخر می‌رویم» کدام زمان است؟", "حال ساده: We go to the pool every Friday (عادت، نه همین حالا).", "تمایز زمان‌ها"),
        ),
        questions = listOf(
            mcq("q1", "کدام جمله درست است؟", listOf("Look! The bus comes.", "Look! The bus is coming.", "Look! The bus coming.", "Look! The bus are coming."), "Look! The bus is coming.", "«Look!» نشانهٔ حال استمراری است؛ bus مفرد → is.", "گرامر", 1, "s2"),
            mcq("q2", "شکلِ درستِ ing برای write چیست؟", listOf("writeing", "writing", "writting", "writes"), "writing", "پایانیِ e حذف می‌شود: write → writing.", "املای ing", 1, "s2"),
            mcq("q3", "«او همین حالا دارد بلیت می‌خرد» کدام است؟", listOf("He buys a ticket now.", "He is buying a ticket now.", "He buy a ticket now.", "He is buy a ticket now."), "He is buying a ticket now.", "حال استمراری: is + buying.", "گرامر", 2, "s2"),
            short("q4", "دو واژهٔ انگلیسی برای «گذرنامه» و «کارت پرواز» بنویس.", "passport و boarding pass", "واژگان سفر.", "واژگان سفر", 1, "s3"),
            short("q5", "فرقِ دو جملهٔ زیر را توضیح بده: I travel to Tabriz. / I am traveling to Tabriz.", "اولی عادت است (هر بار/هر سال) و دومی کارِ همین حالا در جریان است", "کلیدِ تمایز زمان‌ها.", "تمایز زمان‌ها", 2, "s4"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — از «عادت» به «همین حالا» (چهار گام)",
                "گام ۱: جمله را بخوان و قیدِ زمان را پیدا کن: every day/usually → حال ساده؛ now/at the moment/Look! → استمراری.\n" +
                    "گام ۲: فعلِ اصلی را بنویس و ing بگیر: wait → waiting؛ make → making؛ sit → sitting.\n" +
                    "گام ۳: to be مناسبِ فاعل را بگذار: I am / he is / they are.\n" +
                    "گام ۴: در پرسش، to be را جلوی فاعل ببر: Are you waiting? — Yes, I am.\n" +
                    "نمونهٔ حل: «بچه‌ها دارند چمدان می‌بندند.» → The kids are packing. «بچه‌ها هر سال چمدان می‌بندند.» → The kids pack every year.",
            ),
        ),
        summary = "درسِ سفر روی «حال استمراری» (am/is/are + فعل + ing) تمرکز دارد: کارهایی که همین حالا در جریان‌اند. " +
            "قواعدِ املای ing (حذفِ e، دوبل‌شدنِ حرف آخر)، منفی و پرسشیِ آن و تفاوتش با حال ساده را می‌آموزیم و واژه‌های سفر — بلیت، گذرنامه، کارت پرواز، " +
            "چک‌این، فرودگاه، مقصد — را تمرین می‌کنیم.",
        examTips = "۱) الگوی حال استمراری و جابجاییِ to be در پرسش.\n۲) املای ing: make→making، run→running، write→writing.\n" +
            "۳) تفاوت حال ساده/استمراری با قیدهای زمان (every day ↔ now).\n۴) واژه‌های سفر: take off، land، check in، fill out the form.\n" +
            "۵) دامِ رایج: فعل‌های حالتی مثل know یا like با ing نمی‌آیند.",
    )

    /** Review 1 — مرور درس ۱ و ۲. */
    private fun l03() = pack(
        lessonId = "L03",
        title = "Review 1 — مرور درس ۱ و ۲",
        sections = listOf(
            sec(
                "s1", "جمع‌بندی گرامر درس ۱ و ۲", "concept",
                "درس ۱ — فعل to be (حال): I am · he/she/it is · you/we/they are.\n" +
                    "منفی: isn't / aren't · پرسشی: Is he...? / Are they...? · There is/are برای «وجود داشتن».\n" +
                    "درس ۲ — حال استمراری: am/is/are + فعل + ing برای کارِ همین حالا.\n" +
                    "جدولِ یک‌نگاهی:\n" +
                    "• توصیفِ شخصیت (صفات): He is kind. (to be + صفت)\n" +
                    "• کارِ در جریان: He is helping his brother. (to be + ing)\n" +
                    "• جملهٔ وجودی: There is an eraser on the desk. / There are two maps on the wall.",
            ),
            sec(
                "s2", "واژه‌های کلیدی درس ۱ و ۲", "important",
                "شخصیت: clever، kind، helpful، hard-working، brave، neat، funny، patient، polite، generous، talkative، quiet، selfish، rude، careless، nervous، angry، cruel.\n" +
                    "سفر: ticket، passport، luggage، boarding pass، flight، airport، timetable، destination، hotel، receptionist، souvenir، guide، map — و فعل‌های buy، check in، take off، land، exchange، book، pack.",
            ),
            sec(
                "s3", "تمرین‌های ترکیبی (با پاسخ)", "exam",
                "۱) Mina — (be) very patient. → is\n" +
                    "۲) — (be) you listening to me now? → Are\n" +
                    "۳) There — (be) four suitcases in the hall. → are\n" +
                    "۴) My father — (travel) to Isfahan at the moment. → is traveling\n" +
                    "۵) — (be) he your best friend? — Yes, he —. → Is / is\n" +
                    "۶) The students — (not/wait) for the bus now. → aren't waiting\n" +
                    "📌 درست/نادرست: «He is knowing the answer.» (نادرست — know فعلِ حالتی است).",
            ),
            sec(
                "s4", "چک‌لیست پیش از امتحانِ درس ۱ و ۲", "note",
                "□ جای درستِ am/is/are را در جمله‌های خبری، منفی و پرسشی می‌دانم.\n" +
                    "□ صفتِ شخصیت را در جای درست (بعد از to be) می‌گذارم.\n" +
                    "□ ing را با قواعدِ املا می‌سازم (making، running، writing).\n" +
                    "□ تفاوتِ «عادت» و «همین حالا» را با قیدِ زمان تشخیص می‌دهم.\n" +
                    "□ واژه‌های شخصیت و سفر را معنی می‌کنم و در جمله می‌گذارم.",
            ),
        ),
        cards = listOf(
            card("c1", "کدام ساختار برای «وجود داشتن» است؟", "There is + مفرد / There are + جمع — There are two passports.", "جمع‌بندی درس ۱"),
            card("c2", "پرسشیِ Are you happy؟ چه جواب کوتاهی دارد؟", "Yes, I am. / No, I'm not.", "جمع‌بندی درس ۱"),
            card("c3", "Do you speak English? با استمراری چه فرقی دارد؟", "حال ساده: توانایی/عادت. استمراری: کارِ همین حالا — I'm speaking English now.", "جمع‌بندی درس ۲"),
            card("c4", "شکل ing برای sit و study؟", "sitting (دوبل‌شدن) و studying (بدونِ تغییر).", "املای ing"),
            card("c5", "سه واژهٔ سفر که در فرودگاه می‌شنوی؟", "boarding pass، flight، luggage (همچنین check in و take off).", "واژگان سفر"),
        ),
        questions = listOf(
            mcq("q1", "«۲۵ دانش‌آموز در کلاس هستند» کدام است؟", listOf("There is 25 students in the class.", "There are 25 students in the class.", "It are 25 students.", "They is 25 students."), "There are 25 students in the class.", "students جمع → There are.", "جمع‌بندی درس ۱", 1, "s3"),
            mcq("q2", "پاسخِ کوتاهِ «Is your sister kind?» چیست؟", listOf("Yes, she does.", "Yes, she is.", "Yes, it is.", "Yes, they are."), "Yes, she is.", "پرسش با is → پاسخ کوتاه با is و ضمیرِ she (sister).", "جمع‌بندی درس ۱", 1, "s3"),
            mcq("q3", "کدام جمله غلط است؟", listOf("I am reading a book now.", "She is wanting a new bag.", "They are checking in.", "Are you traveling today?"), "She is wanting a new bag.", "want فعلِ حالتی است و ing نمی‌گیرد: She wants a new bag.", "گرامر", 2, "s3"),
            short("q4", "چهار صفتِ مثبتِ شخصیت با معنی بنویس.", "kind (مهربان)، helpful (کمک‌گر)، hard-working (سخت‌کوش)، patient (صبور) — و نمونه‌های دیگر مانند brave، neat، funny", "واژگان درس ۱.", "واژگان", 1, "s2"),
            short("q5", "دو جمله بنویس: یکی عادت و یکی کارِ همین حالا (دربارهٔ سفر).", "نمونه: We travel to the north every summer. / We are traveling to the north now.", "تمرینِ تمایز زمان‌ها.", "گرامر", 2, "s4"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — پاسخِ تمرین‌های ترکیبی با چرایی",
                "۱) is → Mina مفرد. ۲) Are → پرسشِ حال استمراری، فاعل you. ۳) are → four suitcases جمع. ۴) is traveling → at the moment نشانهٔ استمراری.\n" +
                    "۵) Is / is → پرسش با فاعلِ مفرد و پاسخِ کوتاهِ مثبت (Yes, he is). ۶) aren't waiting → منفیِ استمراری برای فاعلِ جمع.\n" +
                    "جمع‌بندی درست/نادرست: «He is knowing the answer» نادرست است چون know فعلِ حالتی است؛ شکل درست: He knows the answer.",
            ),
        ),
        summary = "Review 1 جمع‌بندیِ درسِ شخصیت و سفر است: فعل to be و There is/are در برابرِ حال استمراری، " +
            "واژه‌های توصیفِ شخصیت و واژه‌های سفر، و تمرین‌های ترکیبیِ جای‌خالی همراه با پاسخِ چرایی‌دار.",
        examTips = "۱) جدولِ am/is/are و جابجایی در پرسش.\n۲) There is/are با اسمِ مفرد و جمع.\n" +
            "۳) ing درست بنویس (making، running).\n۴) فعل‌های حالتی در استمراری نمی‌آیند (know, want, like).\n" +
            "۵) واژه‌های شخصیت و سفر را در جمله به‌کار ببر.",
    )

    /** Lesson 3 — Festivals and Ceremonies (جشن‌ها و مراسم): حال ساده + ضمایر و قیدها. */
    private fun l04() = pack(
        lessonId = "L04",
        title = "Lesson 3 — Festivals and Ceremonies (جشن‌ها و مراسم)",
        sections = listOf(
            sec(
                "s1", "گرامر: حال ساده (Present Simple)", "important",
                "کارهای همیشگی و آیین‌های هر ساله: We celebrate Nowruz every year. / My family visits our relatives on the first day of spring.\n" +
                    "سوم‌شخص مفرد (he/she/it): فعل + s یا es — watch→watches، go→goes، study→studies، have→has، do→does.\n" +
                    "منفی: I don't watch TV. / She doesn't wear special clothes.\n" +
                    "پرسشی: Do you celebrate Yalda? / Does your mother bake a cake? — Yes, she does. / No, she doesn't.\n" +
                    "📌 نکتهٔ امتحانی: بعد از doesn't و Does، فعل بدونِ s می‌آید: She doesn't bakes ❌ → She doesn't bake ✓.",
            ),
            sec(
                "s2", "ضمایر مفعولی و صفت‌های ملکی", "important",
                "ضمیر فاعلی → مفعولی → صفت ملکی:\n" +
                    "I → me → my · you → you → your · he → him → his · she → her → her · it → it → its · we → us → our · they → them → their\n" +
                    "جای ضمیر مفعولی: بعد از فعل یا حرف اضافه — I invited them. / This gift is for me.\n" +
                    "جای صفت ملکی: پیش از اسم — our ceremony، their guests، her dress، his poem.\n" +
                    "📌 نکتهٔ امتحانی: it صفت ملکی ندارد؛ «its» را با «it's (it is)» اشتباه نکن.",
            ),
            sec(
                "s3", "قیدهای تکرار و واژه‌های جشن", "important",
                "قیدهای تکرار: always (همیشه) · usually (معمولاً) · often (غالباً) · sometimes (گاهی) · never (هرگز) — جای آن‌ها پیش از فعلِ اصلی و بعد از to be است: We usually hold a ceremony. / He is always kind.\n" +
                    "واژه‌ها: festival (جشنواره) · ceremony (مراسم) · celebrate (جشن گرفتن) · guest (مهمان) · invitation (دعوت‌نامه) · decorate (تزیین کردن) · traditional (سنتی) · gift/present (هدیه) · fireworks (آتش‌بازی) · national anthem (سرود ملی) · clothes (لباس‌ها) · relatives (اقوام)\n" +
                    "عبارت‌ها: hold a ceremony · sing the national anthem · watch fireworks · wear special clothes · bake a cake · set the table · clear the table · read poems of Hafez · go out on nature day · visit our relatives",
            ),
            sec(
                "s4", "نمونه مکالمه — آماده‌شدن برای مراسم", "concept",
                "A: What are you doing for Yalda this year?\n" +
                    "B: My mother always bakes a cake and we read poems of Hafez together.\n" +
                    "A: Do you invite your cousins too?\n" +
                    "B: Yes, they usually come with their children. We set the table and put fruit on it.\n" +
                    "ترجمه: امسال شب یلدا چه می‌کنید؟ — مادرم همیشه کیک می‌پزد و ما با هم شعرهای حافظ می‌خوانیم. — پسرعموهایت را هم دعوت می‌کنید؟ — بله، آن‌ها معمولاً با بچه‌هایشان می‌آیند؛ ما میز را می‌چینیم و میوه رویش می‌گذاریم.\n" +
                    "📌 نیمرخِ گرامر: always و usually قیدهای تکرارند؛ با فعلِ حال ساده می‌آیند.",
            ),
        ),
        cards = listOf(
            card("c1", "شکل سوم‌شخصِ «study» چیست؟", "studies — y بعد از حرفِ صامت به ies تبدیل می‌شود.", "حال ساده"),
            card("c2", "«مراسم برگزار می‌کنیم» به انگلیسی؟", "We hold a ceremony.", "عبارت‌های جشن"),
            card("c3", "جای صفت ملکی کجاست؟", "پیش از اسم: our guests، his poem، her dress.", "ملکی"),
            card("c4", "قید تکرار کجا می‌نشیند؟", "پیش از فعل و بعد از to be: They always come. / He is always helpful.", "قید تکرار"),
            card("c5", "معنی guest و invitation؟", "guest: مهمان؛ invitation: دعوت‌نامه.", "واژگان جشن"),
        ),
        questions = listOf(
            mcq("q1", "کدام جمله درست است؟", listOf("My sister watch fireworks.", "My sister watches fireworks.", "My sister watchs fireworks.", "My sister is watch fireworks."), "My sister watches fireworks.", "سوم‌شخص مفرد + es: watch → watches.", "حال ساده", 1, "s1"),
            mcq("q2", "«او (مؤنث) را دعوت کردم» کدام است؟", listOf("I invited she.", "I invited her.", "I invited hers.", "I invited herself."), "I invited her.", "بعد از فعل، ضمیر مفعولی می‌آید: her.", "ضمایر", 2, "s2"),
            mcq("q3", "پاسخِ «Does your father sing the national anthem?» چیست؟", listOf("Yes, he sings.", "Yes, he does.", "Yes, he is.", "Yes, he do."), "Yes, he does.", "پرسش با Does → پاسخ کوتاه با does.", "حال ساده", 2, "s1"),
            short("q4", "دو کارِ رایجِ شب یلدا در ایران را انگلیسی بنویس.", "We read poems of Hafez and eat fruit (همچنین set the table / stay awake / visit relatives)", "پیوند با فرهنگ.", "واژگان جشن", 1, "s3"),
            short("q5", "«آن‌ها معمولاً مهمان‌هایشان را دعوت می‌کنند» را انگلیسی بنویس.", "They usually invite their guests.", "جای قید تکرار و صفت ملکی.", "گرامر", 2, "s3"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — توصیفِ یک مراسم با حال ساده (پنج جمله)",
                "گام ۱: مراسم را انتخاب کن (نوروز، یلدا، عروسی، تولد).\n" +
                    "گام ۲: کارهای تکرارشونده را با حال ساده بنویس: We clean our house, my mother bakes a cake, we visit our relatives.\n" +
                    "گام ۳: قیدِ تکرار بگذار تا «همیشگی‌بودن» روشن شود: We always/usually/sometimes ...\n" +
                    "گام ۴: صفت ملکی و ضمیر مفعولی را درست به‌کار ببر: our house، their children، invite them.\n" +
                    "نمونهٔ حل: Every year our family celebrates Nowruz. We always set the table and put fruit on it. My grandmother bakes a special cake. We visit our relatives and they give us gifts.",
            ),
        ),
        summary = "درسِ جشن‌ها و مراسم سه ستون دارد: حال ساده برای آیین‌های همیشگی (با قواعدِ s/es در سوم‌شخص مفرد)، " +
            "ضمایر مفعولی و صفت‌های ملکی، و قیدهای تکرار. واژه‌ها و عبارت‌های جشن — decorate، invitation، fireworks، " +
            "خواندنِ شعر حافظ، چیدنِ سفره — در همین درس تمرین می‌شوند.",
        examTips = "۱) سوم‌شخص مفرد: watch→watches، go→goes، study→studies، have→has.\n۲) بعد از Does/Doesn't فعل بدون s.\n" +
            "۳) ضمایر: I→me→my ... they→them→their.\n۴) جای قیدِ تکرار (before فعل اصلی، after to be).\n" +
            "۵) عبارت‌های جشن را در جمله به‌کار ببر (hold a ceremony، sing the national anthem).",
    )

    /** Lesson 4 — Services (خدمات): سؤالات Wh. */
    private fun l05() = pack(
        lessonId = "L05",
        title = "Lesson 4 — Services (خدمات)",
        sections = listOf(
            sec(
                "s1", "گرامر: سؤالات Wh (کلمهٔ پرسشی)", "important",
                "الگو: کلمهٔ پرسشی + فعل کمکی (am/is/are/do/does) + فاعل + فعل اصلی؟\n" +
                    "What (چه چیزی) · Where (کجا) · When (کِی) · Who (چه کسی) · Why (چرا) · How (چگونه) · How much (چه مقدار/قیمت) · How many (چند تا) · What time (چه ساعتی) · Whose (مالِ کی)\n" +
                    "نمونه‌ها: Where do you send the parcel? / When does the bank open? / How much does this book cost? / How many stamps do you need?\n" +
                    "Who اگر فاعلِ جمله باشد، فعل کمکی نمی‌خواهد: Who helps you at the post office?\n" +
                    "📌 نکتهٔ امتحانی: How much برای قیمت و اسم‌های غیرقابل‌شمارش، How many برای اسم‌های قابل‌شمارش جمع.",
            ),
            sec(
                "s2", "واژه‌های خدمات", "important",
                "خدمات و مکان‌ها: post office (ادارهٔ پست) · bank (بانک) · restaurant (رستوران) · hotel (هتل) · hospital (بیمارستان) · police station (کلانتری) · fire station (آتش‌نشانی) · gas station (پمپ بنزین) · bakery (نانوایی) · pharmacy/drugstore (داروخانه)\n" +
                    "کارها و واژه‌ها: send a letter/parcel (نامه/بسته فرستادن) · stamp (تمبر) · envelope (پاکت) · deposit (واریز) · withdraw (برداشت) · cash (نقد) · check (چک) · bill (صورت‌حساب) · receipt (رسید) · menu (منو) · order (سفارش دادن) · waiter (پیشخدمت) · rent (اجاره) · charge (هزینه) · cost (قیمت داشتن) · emergency (اضطراری) · call the emergency (زنگِ اورژانس زدن) · discount (تخفیف)\n" +
                    "عبارت‌های مؤدبانه: Can I help you? · I'd like a ticket, please. · Could you help me? · How much is it? · Here you are. · Thank you for your help.",
            ),
            sec(
                "s3", "نمونه مکالمه — در بانک و در مغازه", "concept",
                "A: Good morning. Can I help you?\n" +
                    "B: Yes, please. I'd like to exchange some money. How much is the dollar today?\n" +
                    "A: Let me check. Where are you going to travel?\n" +
                    "B: To Turkey, next month. How many days does the exchange take?\n" +
                    "ترجمه: صبح بخیر، کمکی می‌توانم بکنم؟ — بله لطفاً؛ می‌خواهم مقداری پول عوض کنم. امروز دلار چند است؟ — بگذار بررسی کنم. کجا می‌خواهید سفر کنید؟ — به ترکیه، ماه آینده. چند روز طول می‌کشد تا تبدیل انجام شود؟\n" +
                    "📌 نیمرخِ گرامر: How much (قیمت)، Where (مکان)، How many (شمارش‌پذیر).",
            ),
            sec(
                "s4", "صفت‌های ملکی و قیدهای زمان در خدمات", "note",
                "در جمله‌های خدماتی زیاد لازم می‌شوند:\n" +
                    "قیدهای زمان: now، today، tomorrow، yesterday، next week، last month، at 8 o'clock.\n" +
                    "صفت ملکی: my account، your receipt، their order، our bill.\n" +
                    "نمونه: I sent their parcel yesterday. / Your receipt is in my bag.\n" +
                    "📌 نکتهٔ امتحانی: قیدِ زمان می‌تواند ابتدا یا انتهای جمله بیاید؛ جای فعلِ کمکی را عوض نمی‌کند.",
            ),
        ),
        cards = listOf(
            card("c1", "برای پرسیدنِ قیمت از چه می‌پرسیم؟", "How much — How much does it cost?", "Wh-questions"),
            card("c2", "فرق How much و How many؟", "much برای غیرقابل‌شمارش/قیمت؛ many برای جمعِ قابل‌شمارش: How many stamps?", "Wh-questions"),
            card("c3", "معنی parcel و stamp؟", "parcel: بستهٔ پستی؛ stamp: تمبر.", "واژگان خدمات"),
            card("c4", "«رسید» و «صورت‌حساب» به انگلیسی؟", "receipt و bill.", "واژگان خدمات"),
            card("c5", "در جملهٔ Who helps you? چرا did/does نداریم؟", "چون Who خودش فاعل است؛ در این حالت فعل کمکی لازم نیست.", "Wh-questions"),
        ),
        questions = listOf(
            mcq("q1", "کدام پرسش برای «تعداد» درست است؟", listOf("How much books do you need?", "How many books do you need?", "How many book you need?", "How much book do you need?"), "How many books do you need?", "books جمعِ قابل‌شمارش است → How many + فعل کمکی do.", "Wh-questions", 2, "s1"),
            mcq("q2", "تکمیل: — does the post office open? — At 8.", listOf("What", "When", "Where", "Who"), "When", "پاسخ «At 8» زمان است → When.", "Wh-questions", 1, "s1"),
            mcq("q3", "«این کتاب چند است؟» کدام است؟", listOf("How many is this book?", "How much is this book?", "How much this book is?", "What much is this book?"), "How much is this book?", "قیمت → How much + is + فاعل (جابجایی to be).", "Wh-questions", 1, "s1"),
            short("q4", "سه واژه از خدمات بانکی را بنویس.", "deposit (واریز)، withdraw (برداشت)، cash (نقد) — همچنین check و account", "واژگان خدمات.", "واژگان خدمات", 2, "s2"),
            short("q5", "یک مکالمهٔ دو خطی در رستوران بنویس (سفارش + پرسیدنِ قیمت).", "نمونه: — I'd like a pizza, please. — Sure. — How much is it? / It's 120 thousand tomans.", "کاربردی‌سازیِ Wh-questions.", "Wh-questions", 2, "s3"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — ساختنِ پرسشِ درست در چهار گام",
                "گام ۱: جوابِ فرضی را ببین: اگر جواب مکان است → Where؛ زمان → When؛ قیمت → How much؛ تعداد → How many؛ شخص → Who.\n" +
                    "گام ۲: فعلِ کمکی را انتخاب کن: to be (is/are) یا do/does.\n" +
                    "گام ۳: ترتیب را رعایت کن: کلمهٔ پرسشی + کمکی + فاعل + فعل.\n" +
                    "گام ۴: اگر Who فاعل است، کمکی حذف می‌شود: Who sends this parcel?\n" +
                    "نمونهٔ حل: «کِی پست باز می‌شود؟» → When does the post office open? / «سفارشت را کجا داد؟» → Where did you order it?",
            ),
        ),
        summary = "درسِ خدمات روی سؤالات Wh می‌ایستد: ترتیبِ «کلمهٔ پرسشی + فعل کمکی + فاعل + فعل»، تفاوت How much و How many، " +
            "و حالتِ ویژه‌ای که Who خودش فاعل است. واژه‌های خدمات (پست، بانک، رستوران، اجاره، هزینه، اورژانس) و عبارت‌های مؤدبانه هم در همین درس تمرین می‌شوند.",
        examTips = "۱) جدولِ کلمات پرسشی و کاربردِ هرکدام.\n۲) How much (قیمت/غیرقابل‌شمارش) ↔ How many (جمعِ قابل‌شمارش).\n" +
            "۳) ترتیبِ درستِ پرسش (Wh + auxiliary + subject + verb).\n۴) Who به‌عنوان فاعل: بدونِ کمکی.\n" +
            "۵) واژه‌های خدمات: receipt، bill، rent، charge، emergency، discount.",
    )

    /** Review 2 — مرور درس ۳ و ۴. */
    private fun l06() = pack(
        lessonId = "L06",
        title = "Review 2 — مرور درس ۳ و ۴",
        sections = listOf(
            sec(
                "s1", "جمع‌بندی گرامر درس ۳ و ۴", "concept",
                "درس ۳ — حال ساده: کارهای همیشگی و آیین‌ها (We celebrate Nowruz every year).\n" +
                    "• سوم‌شخص مفرد: +s/es (watches، goes، studies، has)\n" +
                    "• منفی و پرسشی با don't/doesn't و Do/Does\n" +
                    "• ضمایر مفعولی (me, you, him, her, it, us, them) و صفت‌های ملکی (my, your, his, her, its, our, their)\n" +
                    "• قیدهای تکرار: always, usually, often, sometimes, never\n" +
                    "درس ۴ — سؤالات Wh: کلمهٔ پرسشی + فعل کمکی + فاعل + فعل.\n" +
                    "• How much برای قیمت/غیرقابل‌شمارش، How many برای جمعِ قابل‌شمارش\n" +
                    "• Who به‌عنوانِ فاعل: بدونِ فعل کمکی",
            ),
            sec(
                "s2", "واژه‌های کلیدی درس ۳ و ۴", "important",
                "جشن‌ها: festival، ceremony، celebrate، guest، invitation، decorate، traditional، gift، fireworks، national anthem، relatives، clothes.\n" +
                    "خدمات: post office، bank، parcel، stamp، envelope، deposit، withdraw، cash، bill، receipt، menu، order، waiter، rent، charge، emergency، discount، exchange money، fill out the form.",
            ),
            sec(
                "s3", "تمرین‌های ترکیبی (با پاسخ)", "exam",
                "۱) My brother — (hold) a ceremony every year. → holds\n" +
                    "۲) — she decorate the hall? — Yes, she —. → Does / does\n" +
                    "۳) We don't invite — (they) to the party. → them\n" +
                    "۴) — (they) house is near the post office. → Their\n" +
                    "۵) — does the parcel cost? → How much\n" +
                    "۶) — many guests do you have? → How\n" +
                    "۷) She — (not / wear) traditional clothes every day. → doesn't wear\n" +
                    "۸) — helps you at the bank? — Mr. Karimi. → Who",
            ),
            sec(
                "s4", "چک‌لیست پیش از امتحانِ درس ۳ و ۴", "note",
                "□ s/es سوم‌شخص مفرد را درست می‌سازم (es بعد از ch/sh/s/x/o؛ ies بعد از صامت+y).\n" +
                    "□ بعد از doesn't و Does، فعل را بدونِ s می‌نویسم.\n" +
                    "□ ضمیر مفعولی و صفت ملکی را در جای درست می‌گذارم.\n" +
                    "□ جای قیدِ تکرار را می‌دانم.\n" +
                    "□ سؤال Wh را با ترتیبِ درست می‌سازم و How much/How many را اشتباه نمی‌کنم.",
            ),
        ),
        cards = listOf(
            card("c1", "شکل درست: «ساعت ۸ باز می‌کند» کدام است؟", "It opens at 8 o'clock. (سوم‌شخص مفرد → opens).", "جمع‌بندی درس ۳"),
            card("c2", "«دعوت‌نامه‌شان را فرستادیم» با ضمیر مفعولی؟", "We sent them the invitation. (them = آن‌ها).", "ضمایر"),
            card("c3", "جوابِ How much does it cost؟ چه نوع جوابی است؟", "پاسخِ قیمت: It costs 50 thousand tomans.", "جمع‌بندی درس ۴"),
            card("c4", "Who opens the shop؟ چرا does ندارد؟", "چون Who فاعل است؛ در این حالت فعل کمکی نمی‌آید.", "Wh-questions"),
            card("c5", "دو واژهٔ خدماتِ مرتبط با پول؟", "deposit (واریز) و withdraw (برداشت) — همچنین cash، receipt، bill.", "واژگان خدمات"),
        ),
        questions = listOf(
            mcq("q1", "کدام جمله درست است؟", listOf("Does she decorates the hall?", "Does she decorate the hall?", "Do she decorates the hall?", "She does decorate the hall?"), "Does she decorate the hall?", "بعد از Does فعل بدونِ s می‌آید.", "حال ساده", 2, "s3"),
            mcq("q2", "«کتابِ آن‌ها رویِ میز است» کدام است؟", listOf("Them book is on the table.", "Their book is on the table.", "They book is on the table.", "Theirs book is on the table."), "Their book is on the table.", "پیش از اسم، صفت ملکی می‌آید: their.", "ملکی", 1, "s3"),
            mcq("q3", "تکمیل: — do you go to the post office? — Twice a month.", listOf("How much", "How many", "How often", "How long"), "How often", "«Twice a month» تعدادِ دفعات است → How often.", "Wh-questions", 3, "s3"),
            short("q4", "سه واژهٔ جشن‌ها با معنی بنویس.", "fireworks (آتش‌بازی)، ceremony (مراسم)، invitation (دعوت‌نامه) — یا decorate، guest، traditional", "واژگان درس ۳.", "واژگان", 1, "s2"),
            short("q5", "دو سؤال Wh بنویس: یکی با How much و یکی با How many.", "نمونه: How much does the ticket cost? / How many guests did you invite?", "تمرینِ سؤال‌سازی.", "Wh-questions", 2, "s3"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — پاسخِ تمرین‌های ترکیبی با چرایی",
                "۱) holds → سوم‌شخص مفرد، عادتِ هرساله. ۲) Does / does → پرسش و پاسخِ کوتاهِ هم‌خانواده.\n" +
                    "۳) them → بعد از فعل، ضمیر مفعولی. ۴) Their → صفت ملکی پیش از اسمِ house.\n" +
                    "۵) How much → پرسشِ قیمت. ۶) How → چون ساختارِ How many است.\n" +
                    "۷) doesn't wear → منفیِ سوم‌شخص با فعلِ بدونِ s. ۸) Who → فاعلِ جمله، بدونِ کمکی.\n" +
                    "قاعدهٔ جمعی: اول فاعل و زمان را ببین، بعد کمکی و شکلِ فعل را انتخاب کن.",
            ),
        ),
        summary = "Review 2 درسِ جشن‌ها و خدمات را جمع می‌بندد: حال ساده و سوم‌شخص مفرد، ضمایر مفعولی و صفت‌های ملکی، " +
            "قیدهای تکرار و سؤالات Wh با تمرکز بر How much/How many و حالتِ فاعلیِ Who؛ همراه با واژه‌های جشن و خدمات.",
        examTips = "۱) قواعدِ s/es/ies سوم‌شخص مفرد.\n۲) don't/doesn't + فعلِ بدونِ s.\n" +
            "۳) ضمایر و صفت‌های ملکی در جای درست.\n۴) ترتیبِ سؤال Wh.\n۵) واژه‌های جشن و خدمات را در جمله به‌کار ببر.",
    )

    /** Lesson 5 — Media (رسانه): گذشتهٔ ساده با افعال باقاعده. */
    private fun l07() = pack(
        lessonId = "L07",
        title = "Lesson 5 — Media (رسانه)",
        sections = listOf(
            sec(
                "s1", "گرامر: گذشتهٔ ساده (Simple Past)", "important",
                "برای کارهایی که در گذشته تمام شده‌اند: I watched a documentary last night. / She listened to the radio yesterday.\n" +
                    "ساختار: فاعل + شکلِ گذشتهٔ فعل + بقیه؛ برای همهٔ فاعل‌ها یکسان است (he watched = they watched).\n" +
                    "افعال باقاعده: +ed — watch→watched، listen→listened، play→played، talk→talked، help→helped، call→called.\n" +
                    "منفی: didn't + شکلِ ساده — I didn't watch TV last night.\n" +
                    "پرسشی: Did + فاعل + شکلِ ساده؟ — Did you call me? Yes, I did. / No, I didn't.\n" +
                    "فعل to be در گذشته: was (برای I/he/she/it) · were (برای you/we/they) — He was at home. / They were busy.\n" +
                    "📌 نکتهٔ امتحانی: بعد از did و didn't، فعلِ گذشته نمی‌آید: Did you watched ❌ → Did you watch ✓.",
            ),
            sec(
                "s2", "املای ed و تلفظِ آن", "important",
                "املای ed:\n" +
                    "• حالتِ عادی: watched، played، listened\n" +
                    "• پایانیِ e: live→lived، like→liked، use→used\n" +
                    "• پایانیِ صامت+y: study→studied، carry→carried، try→tried\n" +
                    "• یک‌هجاییِ پایانیِ CVC: stop→stopped، plan→planned\n" +
                    "تلفظِ ed (شنیداری): /t/ بعد از صداهای بی‌صدا (watched، stopped) · /d/ بعد از صداهای واکدار (played، listened) · /ɪd/ بعد از t یا d (wanted، needed، visited).\n" +
                    "📌 نکتهٔ امتحانی: wanted دو هجا می‌شود، ولی played یک‌هجا می‌ماند.",
            ),
            sec(
                "s3", "واژه‌ها و عبارت‌های رسانه", "important",
                "TV (تلویزیون) · channel (شبکه) · program (برنامه) · the news (اخبار) · radio (رادیو) · newspaper (روزنامه) · magazine (مجله) · the Internet (اینترنت) · website (وب‌سایت) · mobile phone (تلفن همراه) · message (پیام) · advertisement/ad (آگهی) · cartoon (کارتون) · documentary (مستند) · series (سریال) · cartoon/sport/match (ورزش/مسابقه)\n" +
                    "فعل‌های رسانه‌ای: watch TV · listen to the radio · read a newspaper/a magazine · search the Internet · download a file · upload a photo · send a message · surf the Internet · take a picture · turn on/off · turn up/down\n" +
                    "قیدهای زمانِ گذشته: yesterday · last night/week/month/year · two days ago · in 1400 (در سالِ گذشته) · this morning",
            ),
            sec(
                "s4", "نمونه متن — دیشب چه کردم؟", "exam",
                "Last night I watched a documentary about Iranian deserts. My sister didn't watch it with me, because she listened to music and chatted with her friends. We called our grandmother in the evening and sent her some pictures. My father read a newspaper and turned on the radio at 10.\n" +
                    "ترجمه: دیشب مستندی دربارهٔ کویرهای ایران تماشا کردم. خواهرم با من تماشا نکرد، چون به موسیقی گوش داد و با دوستانش گپ زد. غروب با مادربزرگمان تماس گرفتیم و چند عکس برایش فرستادیم. پدرم روزنامه خواند و ساعت ۱۰ رادیو را روشن کرد.\n" +
                    "پرسش: ۱) What did the writer watch? ۲) Why didn't the sister watch it? ۳) What did the father do?\n" +
                    "پاسخ: ۱) A documentary about Iranian deserts. ۲) Because she listened to music and chatted with her friends. ۳) He read a newspaper and turned on the radio.",
            ),
        ),
        cards = listOf(
            card("c1", "شکل گذشتهٔ watch، study و stop؟", "watched، studied، stopped.", "املای ed"),
            card("c2", "پرسشیِ گذشتهٔ ساده چطور ساخته می‌شود؟", "Did + فاعل + فعلِ ساده: Did you watch it?", "گرامر گذشته"),
            card("c3", "was را برای چه فاعل‌هایی به‌کار می‌بریم؟", "I / he / she / it — برای بقیه were.", "گرامر گذشته"),
            card("c4", "تلفظِ ed در wanted چیست؟", "/ɪd/ — دو هجا می‌شود (چون فعل با t تمام شده است).", "تلفظ"),
            card("c5", "معنی advertisement و documentary؟", "advertisement: آگهی؛ documentary: مستند.", "واژگان رسانه"),
        ),
        questions = listOf(
            mcq("q1", "کدام جمله درست است؟", listOf("I watched TV last night.", "I watch TV last night.", "I did watched TV last night.", "I was watch TV last night."), "I watched TV last night.", "last night ⇒ گذشتهٔ سادهٔ باقاعده: watched.", "گرامر گذشته", 1, "s1"),
            mcq("q2", "شکل گذشتهٔ study چیست؟", listOf("studyed", "studied", "studed", "studying"), "studied", "صامت + y ⇒ y به ied تبدیل می‌شود.", "املای ed", 1, "s2"),
            mcq("q3", "تکمیل: — you listen to the radio yesterday?", listOf("Do", "Did", "Was", "Were"), "Did", "پرسشِ گذشته با Did + فاعل + فعلِ ساده.", "گرامر گذشته", 2, "s1"),
            short("q4", "سه قیدِ زمانِ گذشته را بنویس.", "yesterday، last night/week، two days ago", "قیدهای زمان.", "قیدهای زمان", 1, "s3"),
            short("q5", "«دیشب پیام فرستادم» را منفی و پرسشی کن.", "منفی: I didn't send a message last night. / پرسشی: Did you send a message last night?", "تمرینِ did/didn't.", "گرامر گذشته", 2, "s1"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — بازگوییِ یک روز گذشته (پنج جمله)",
                "گام ۱: سه کارِ دیروزت را انتخاب کن و فعل را به گذشته ببر: watch→watched، help→helped، talk→talked، study→studied.\n" +
                    "گام ۲: قیدِ زمان بگذار تا «گذشته‌بودن» روشن شود: yesterday / last night / two days ago.\n" +
                    "گام ۳: یک جملهٔ منفی و یک پرسشیِ کوتاه اضافه کن: I didn't play games. Did you study English?\n" +
                    "گام ۴: به فعل‌های to be دقت کن: I was tired. / They were at school.\n" +
                    "نمونهٔ حل: Yesterday I studied English for an hour. I helped my mother in the kitchen. I didn't watch TV. Then I called my friend and we talked about school. I was tired, so I went to bed early.",
            ),
        ),
        summary = "درسِ رسانه گذشتهٔ ساده را می‌آموزد: افعالِ باقاعده با ed، قواعدِ املای آن، منفی و پرسشیِ did/didn't، " +
            "و was/were. واژه‌های رسانه (channel، program، advertisement، website) و قیدهای زمانِ گذشته (yesterday، last night، two days ago) هم تمرین می‌شوند.",
        examTips = "۱) ed باقاعده: watched/studied/stopped.\n۲) بعد از did و didn't فعلِ ساده بیاید (نه گذشته).\n" +
            "۳) was/were و جای درستشان.\n۴) تلفظِ ed: /t/ /d/ /ɪd/.\n۵) قیدهای زمانِ گذشته + واژه‌های رسانه.",
    )

    /** Lesson 6 — Health and Injuries (سلامت و آسیب‌ها): گذشتهٔ ساده با افعال بی‌قاعده. */
    private fun l08() = pack(
        lessonId = "L08",
        title = "Lesson 6 — Health and Injuries (سلامت و آسیب‌ها)",
        sections = listOf(
            sec(
                "s1", "گرامر: افعال بی‌قاعده در گذشته", "important",
                "خیلی از فعل‌های پرکاربرد شکلِ ed نمی‌گیرند و شکلِ خودشان را دارند:\n" +
                    "go→went · have→had · eat→ate · drink→drank · break→broke · take→took · feel→felt · see→saw · do→did · say→said · get→got · come→came · hurt→hurt · cut→cut · catch→caught · sleep→slept\n" +
                    "نمونه: I hurt my leg yesterday. / He had a fever last night. / They went to the hospital this morning.\n" +
                    "منفی و پرسشی مثل گذشتهٔ باقاعده است (didn't + فعلِ ساده / Did + فاعل + فعلِ ساده): I didn't break my arm. / Did she take her medicine?\n" +
                    "📌 نکتهٔ امتحانی: hurt و cut در گذشته تغییر نمی‌کنند؛ put هم سه‌شکلش یکی است.",
            ),
            sec(
                "s2", "واژه‌های سلامت و آسیب", "important",
                "بیماری‌ها: headache (سردرد) · stomachache (دل‌درد) · toothache (دندان‌درد) · sore throat (گلودرد) · cold (سرماخوردگی) · flu (آنفلوآنزا) · fever (تب) · cough (سرفه) · backache (کمردرد) · pain (درد) · injury (آسیب)\n" +
                    "آسیب‌ها: broken arm/leg (شکستگیِ دست/پا) · cut (بریدگی) · burn (سوختگی) · sprained ankle (پیچ‌خوردگیِ مچ) · accident (حادثه) · hurt (آسیب زدن/درد داشتن)\n" +
                    "درمان و مراقبت: doctor (پزشک) · dentist (دندان‌پزشک) · nurse (پرستار) · hospital (بیمارستان) · medicine/pill (دارو/قرص) · syrup (شربت) · rest (استراحت) · take medicine · see a doctor · stay in bed\n" +
                    "عبارت‌های کاربردی: What's the matter? (چه شده؟) · What happened? · I don't feel well. · He has a fever. · You should see a doctor. · Take this medicine twice a day. · Get well soon!",
            ),
            sec(
                "s3", "نمونه مکالمه — در مطب", "concept",
                "Doctor: Hello. What's the matter?\n" +
                    "Patient: I have a terrible headache and I didn't sleep well last night.\n" +
                    "Doctor: Did you take any medicine?\n" +
                    "Patient: Yes, I did, but it didn't help. I also hurt my back when I carried a heavy bag.\n" +
                    "Doctor: You should rest and take these pills twice a day. If it doesn't get better, come back.\n" +
                    "ترجمه: سلام، چه شده؟ — سردرد شدیدی دارم و دیشب خوب نخوابیدم. — دارویی خوردی؟ — بله، ولی فایده‌ای نداشت. وقتی کیف سنگینی را حمل کردم، کمرم هم آسیب دید. — باید استراحت کنی و این قرص‌ها را دو بار در روز بخوری. اگر بهتر نشد، برگرد.\n" +
                    "📌 نیمرخِ گرامر: didn't sleep (نکتهٔ گذشته)، hurt و took (بی‌قاعده)، should برای توصیه.",
            ),
            sec(
                "s4", "تمرین و اشتباه‌های رایج", "exam",
                "درست/نادرست:\n" +
                    "۱) He goed to the dentist yesterday. (نادرست → went)\n" +
                    "۲) I hurted my leg. (نادرست → hurt؛ این فعل تغییر نمی‌کند)\n" +
                    "۳) She didn't took her medicine. (نادرست → didn't take)\n" +
                    "۴) What happened to your hand? (درست)\n" +
                    "۵) I have a fever yesterday. (نادرست → I had a fever yesterday)\n" +
                    "تمرینِ جای‌خالی: ۱) My brother — (break) his arm last week. ۲) We — (go) to the pharmacy at 9. ۳) — you — (see) the nurse? ۴) She — (not / feel) well this morning.\n" +
                    "پاسخ: ۱) broke ۲) went ۳) Did … see ۴) didn't feel",
            ),
        ),
        cards = listOf(
            card("c1", "شکل گذشتهٔ go، take و break؟", "went، took، broke.", "افعال بی‌قاعده"),
            card("c2", "چرا گفته می‌شود I hurt my leg، نه I hurted؟", "چون hurt فعل بی‌قاعده است و در گذشته تغییر نمی‌کند.", "افعال بی‌قاعده"),
            card("c3", "معنی sore throat و fever؟", "sore throat: گلودرد؛ fever: تب.", "واژگان سلامت"),
            card("c4", "برای توصیه به پزشک رفتن چه می‌گوییم؟", "You should see a doctor.", "عبارت‌های کاربردی"),
            card("c5", "«دو بار در روز» به انگلیسی؟", "twice a day — و «یک بار در روز»: once a day.", "عبارت‌های کاربردی"),
        ),
        questions = listOf(
            mcq("q1", "کدام جمله درست است؟", listOf("She breaked her arm.", "She broke her arm.", "She did broke her arm.", "She is break her arm."), "She broke her arm.", "break بی‌قاعده است: broke.", "افعال بی‌قاعده", 2, "s1"),
            mcq("q2", "شکل گذشتهٔ تدوین‌شده: Did you — ( take ) your medicine?", listOf("took", "taken", "take", "takes"), "take", "بعد از Did، فعلِ ساده می‌آید.", "گرامر گذشته", 2, "s1"),
            mcq("q3", "«دیشب تب داشتم» کدام است؟", listOf("I have a fever last night.", "I had a fever last night.", "I has a fever last night.", "I having a fever last night."), "I had a fever last night.", "گذشتهٔ have = had.", "افعال بی‌قاعده", 1, "s4"),
            short("q4", "چهار بیماری/عارضه با معنی بنویس.", "headache (سردرد)، stomachache (دل‌درد)، fever (تب)، sore throat (گلودرد) — یا cough، cold، flu، broken arm", "واژگان سلامت.", "واژگان سلامت", 1, "s2"),
            short("q5", "یک توصیهٔ پزشکی دو جمله‌ای بنویس (با should).", "نمونه: You should rest at home. You should take this medicine twice a day.", "کاربردِ should و واژه‌های درمان.", "عبارت‌های کاربردی", 2, "s3"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — پاسخِ تمرین‌ها با چرایی + جدولِ فعل‌های بی‌قاعده",
                "پاسخِ جای‌خالی: ۱) broke → بی‌قاعدهٔ break. ۲) went → بی‌قاعدهٔ go، با قیدِ ساعتِ گذشته. ۳) Did … see → ساختارِ پرسشیِ گذشته، فعلِ ساده. ۴) didn't feel → منفی با didn't + فعل ساده (feel).\n" +
                    "جدولِ کوتاهِ حفظی: go→went · have→had · eat→ate · drink→drank · break→broke · take→took · feel→felt · see→saw · do→did · say→said · get→got · come→came · hurt→hurt · cut→cut · catch→caught · sleep→slept.\n" +
                    "روشِ حفظ: فعل‌ها را گروهی بخوان (go/went، have/had) و برای هرکدام یک جملهٔ بیمارستانی بساز: I went to the doctor. / He had a fever. / I caught a cold.",
            ),
        ),
        summary = "درسِ سلامت و آسیب‌ها گذشتهٔ ساده را با افعالِ بی‌قاعده کامل می‌کند: go→went، have→had، break→broke، hurt→hurt (بدونِ تغییر). " +
            "واژه‌های بیماری و آسیب (fever، sore throat، broken arm، cut)، واژه‌های درمان (medicine، rest، hospital) و عبارت‌های مطب (What's the matter? / You should rest.) هم در همین درس می‌آید.",
        examTips = "۱) جدولِ فعل‌های بی‌قاعده — مخصوصاً hurt/cut که تغییر نمی‌کنند.\n۲) بعد از did/didn't فعلِ ساده.\n" +
            "۳) had برای بیماریِ گذشته (I had a fever).\n۴) واژه‌های بیماری + واژه‌های درمان.\n۵) عبارت‌های What's the matter? و You should …",
    )

    /** Review 3 — مرور درس ۵ و ۶. */
    private fun l09() = pack(
        lessonId = "L09",
        title = "Review 3 — مرور درس ۵ و ۶",
        sections = listOf(
            sec(
                "s1", "جمع‌بندی گرامر درس ۵ و ۶", "concept",
                "گذشتهٔ ساده = کارهایی که در گذشته تمام شده‌اند.\n" +
                    "• باقاعده: +ed (watched، played، studied، stopped)\n" +
                    "• بی‌قاعده: شکلِ ویژه (went، had، broke، took، felt، saw، hurt)\n" +
                    "• منفی: didn't + فعلِ ساده · پرسشی: Did + فاعل + فعلِ ساده؟\n" +
                    "• to be: was (I/he/she/it) · were (you/we/they)\n" +
                    "• قیدهای زمان: yesterday، last night/week/month، two days ago\n" +
                    "جدولِ یک‌نگاهی: عادت (حال ساده) → yesterday (گذشتهٔ ساده) → now (حال استمراری).",
            ),
            sec(
                "s2", "واژه‌های کلیدی درس ۵ و ۶", "important",
                "رسانه: channel، program، the news، radio، newspaper، magazine، the Internet، website، advertisement، cartoon، documentary، message، download، upload، search، turn on/off.\n" +
                    "سلامت: headache، stomachache، toothache، sore throat، cold، flu، fever، cough، pain، injury، broken arm، cut، burn، doctor، dentist، nurse، medicine، rest، hospital.",
            ),
            sec(
                "s3", "تمرین‌های ترکیبی (با پاسخ)", "exam",
                "۱) Last night we — (watch) a documentary. → watched\n" +
                    "۲) He — (have) a toothache yesterday. → had\n" +
                    "۳) — she — (go) to the dentist? (پرسشی کن) → Did she go\n" +
                    "۴) They — (not / sleep) well last night. → didn't sleep\n" +
                    "۵) My father — (be) in the hospital two days ago. → was\n" +
                    "۶) I — (break) my leg last summer. → broke\n" +
                    "۷) We — (not / see) the advertisement. → didn't see\n" +
                    "📌 تمرینِ دام‌دار: Did you hurted your hand? → نادرست؛ درست: Did you hurt your hand?",
            ),
            sec(
                "s4", "چک‌لیست پیش از امتحانِ درس ۵ و ۶", "note",
                "□ ed باقاعده را با قاعده‌های املا می‌نویسم (lived، studied، stopped).\n" +
                    "□ جدولِ فعل‌های بی‌قاعده را می‌دانم (went، had، broke، took، felt، saw، hurt).\n" +
                    "□ پس از did/didn't فعلِ ساده به‌کار می‌برم.\n" +
                    "□ was/were را درست انتخاب می‌کنم.\n" +
                    "□ واژه‌های رسانه و سلامت را در جمله می‌گذارم و یک متنِ کوتاهِ گذشته می‌نویسم.",
            ),
        ),
        cards = listOf(
            card("c1", "تفاوتِ watched و watched? (املای ed)", "watched حالتِ عادی است؛ studied و stopped دو نمونهٔ تغییرشکل دارند (ies و دوبل‌شدن).", "جمع‌بندی گذشتهٔ ساده"),
            card("c2", "شکل گذشتهٔ feel و see؟", "felt و saw.", "افعال بی‌قاعده"),
            card("c3", "جوابِ کوتاهِ Did they go? چیست؟", "Yes, they did. / No, they didn't.", "گرامر گذشته"),
            card("c4", "سه واژهٔ رسانه‌ای بنویس.", "channel، documentary، website (یا advertisement، radio، newspaper).", "واژگان رسانه"),
            card("c5", "«چه شده؟» و «زود خوب شو!» به انگلیسی؟", "What's the matter? / What happened? و Get well soon!", "عبارت‌های کاربردی"),
        ),
        questions = listOf(
            mcq("q1", "کدام جمله درست است؟", listOf("I didn't watched this series.", "I didn't watch this series.", "I not watched this series.", "I don't watched this series."), "I didn't watch this series.", "منفیِ گذشته: didn't + فعلِ ساده.", "گرامر گذشته", 2, "s3"),
            mcq("q2", "شکل گذشتهٔ have در «او دیشب تب داشت»؟", listOf("haved", "had", "has", "having"), "had", "have بی‌قاعده است: had.", "افعال بی‌قاعده", 1, "s3"),
            mcq("q3", "تکمیل: My sister — (be) very tired last night.", listOf("is", "was", "were", "be"), "was", "sister مفرد ⇒ was.", "گرامر گذشته", 1, "s3"),
            short("q4", "سه فعلِ بی‌قاعده با گذشته‌شان بنویس.", "نمونه: go→went، break→broke، take→took (یا have→had، feel→felt، see→saw)", "جدولِ افعال.", "افعال بی‌قاعده", 2, "s2"),
            short("q5", "یک متنِ سه‌جمله‌ای بنویس دربارهٔ کاری که دیروز کردی (با یک فعلِ بی‌قاعده و یک منفی).", "نمونه: Yesterday I went to my uncle's home. We watched a football match. I didn't do my homework, but I studied English at night.", "نوشتارِ گذشته.", "نوشتار", 3, "s4"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "کارگاه — پاسخِ تمرین‌های ترکیبی با چرایی",
                "۱) watched → قیدِ last night و فعلِ باقاعده. ۲) had → گذشتهٔ بی‌قاعدهٔ have.\n" +
                    "۳) Did she go → پرسشیِ گذشته با Did + فعلِ ساده. ۴) didn't sleep → منفیِ گذشته برای فاعلِ they.\n" +
                    "۵) was → father مفرد، to be گذشته. ۶) broke → بی‌قاعده و اتفاقِ تمام‌شده در گذشته.\n" +
                    "۷) didn't see → منفی با فعلِ ساده.\n" +
                    "دامِ امتحانی: «Did you hurted your hand?» غلط است؛ پس از Did فعلِ ساده (hurt) می‌آید و hurt در گذشته هم تغییر نمی‌کند.",
            ),
        ),
        summary = "Review 3 گذشتهٔ ساده را کامل جمع می‌بندد: افعالِ باقاعده و بی‌قاعده، منفی و پرسشی، was/were و قیدهای زمانِ گذشته؛ " +
            "همراه با واژه‌های رسانه و سلامت و تمرین‌های ترکیبیِ جای‌خالی با پاسخِ چرایی‌دار.",
        examTips = "۱) ed + جدولِ بی‌قاعده‌ها.\n۲) didn't/did + فعلِ ساده.\n۳) was/were با فاعلِ درست.\n" +
            "۴) واژه‌های رسانه و سلامت.\n۵) نوشتنِ متنِ کوتاهِ گذشته (سه تا پنج جمله) با یک منفی.",
    )

    /** درس‌های ۲ تا ۹ (Review 1 تا Review 3 هم پکِ خودشان را دارند). */
    val packs: List<StudyPack> = listOf(l02(), l03(), l04(), l05(), l06(), l07(), l08(), l09())
}
