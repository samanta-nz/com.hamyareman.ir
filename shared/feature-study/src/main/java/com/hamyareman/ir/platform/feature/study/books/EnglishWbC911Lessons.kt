package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.StudyPack

/**
 * درس‌های ۲ تا ۶ کتاب «کتاب کار زبان انگلیسی پایه نهم» (C911).
 *
 * کتاب کار، تمرین‌های همراهِ همان شش درسِ کتابِ دانش‌آموز است؛ پس ساختار هر پک این‌جا
 * «تمرین + پاسخِ چرایی‌دار» است: تمرین‌های واژگان، تمرین‌های گرامر، مکالمه/تصویر،
 * خواندن و نوشتن با نمونهٔ پاسخ، و در پایان نکاتِ امتحانی. درس ۱ در `EnglishWbC911.kt`
 * مانده و این‌ها با سازنده‌های کوتاه (همان قالبِ C915/C941/C910) نوشته شده‌اند.
 */
internal object EnglishWbC911Lessons {

    private const val BOOK = "کتاب کار زبان انگلیسی پایه نهم"

    // ---- سازنده‌های کوتاه ----

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
        packId = "C911_$lessonId",
        bookCode = "C911",
        lessonId = lessonId,
        title = title,
        bookTitle = BOOK,
        pdfFileName = "C911_${lessonId}_BOOK.pdf",
        audioFileId = "C911_${lessonId}_AUDIO.mp3",
        sections = sections,
        flashcards = cards,
        questions = questions,
        solutions = solutions,
        summary = summary,
        examTips = examTips,
    )

    /** Lesson 2 — Travel: تمرین‌های حال استمراری و واژه‌های سفر. */
    private fun l02() = pack(
        lessonId = "L02",
        title = "Lesson 2 — Travel (تمرین‌ها)",
        sections = listOf(
            sec(
                "s1", "تمرین ۱ و ۲: جای‌خالی با حال استمراری", "important",
                "تمرین ۱ — فعل را به حال استمراری ببر:\n" +
                    "۱) Look! The bus — (come). → is coming\n" +
                    "۲) My father — (buy) the tickets now. → is buying\n" +
                    "۳) We — (wait) for the train at the moment. → are waiting\n" +
                    "۴) I — (pack) my suitcase right now. → am packing\n" +
                    "۵) The children — (not / sleep); they — (watch) a film. → aren't sleeping / are watching\n" +
                    "تمرین ۲ — املای ing را کامل کن: run → running · make → making · write → writing · sit → sitting · study → studying · travel → traveling\n" +
                    "📌 یادآوری: یک‌هجاییِ پایانیِ صامت+واکه+صامت، حرف آخر دوبل می‌شود.",
            ),
            sec(
                "s2", "تمرین ۳ و ۴: تصحیح خطا و مرتب‌کردن جمله", "important",
                "تمرین ۳ — جمله‌های نادرست را درست کن:\n" +
                    "۱) He is wanting a new suitcase. ✗ → He wants a new suitcase. (want فعلِ حالتی است)\n" +
                    "۲) Are you going to airport? ✗ → Are you going to the airport?\n" +
                    "۳) They is waiting for the bus. ✗ → They are waiting for the bus.\n" +
                    "۴) I am travel to Shiraz now. ✗ → I am traveling to Shiraz now.\n" +
                    "تمرین ۴ — مرتب کن:\n" +
                    "۱) is / where / going / she / ? → Where is she going?\n" +
                    "۲) at / the / is / plane / moment / landing / the → The plane is landing at the moment.\n" +
                    "۳) you / are / what / doing / ? → What are you doing?\n" +
                    "۴) for / waiting / we / are / the / guide → We are waiting for the guide.",
            ),
            sec(
                "s3", "تمرین ۵ و ۶: خواندن و نوشتن (نمونهٔ پاسخ)", "exam",
                "تمرین ۵ — متن را بخوان و پاسخ بده:\n" +
                    "«It is Friday morning. My family and I are at the airport. My sister is checking the luggage and my mother is buying some water. I am looking at the timetable. Our flight is at 10:30.»\n" +
                    "۱) Where are they? → At the airport.\n" +
                    "۲) What is the sister doing? → She is checking the luggage.\n" +
                    "۳) What time is the flight? → At 10:30.\n" +
                    "تمرین ۶ — نوشتن: چهار جمله بنویس که الان در خانه‌ات در جریان است.\n" +
                    "نمونهٔ پاسخ: I am sitting in my room. My mother is cooking lunch. My brother is doing his homework. Our cat is sleeping on the sofa.",
            ),
            sec(
                "s4", "تمرین ۷: واژگان و تصویر", "note",
                "واژه‌ها را با تعریفشان وصل کن:\n" +
                    "۱) a place where planes take off → airport\n" +
                    "۲) a paper that shows your seat on the plane → boarding pass\n" +
                    "۳) bags and suitcases that you take on a trip → luggage\n" +
                    "۴) a book with times of buses and trains → timetable\n" +
                    "۵) to go from one place to another → travel\n" +
                    "📌 نکتهٔ امتحانی: «trip» یعنی سفر (اسم)، ولی «travel» هم اسم است و هم فعل؛ در کتاب کار بیشتر به‌شکل فعل می‌آید: We travel by train.",
            ),
        ),
        cards = listOf(
            card("c1", "Look! The bus — (come).", "is coming — «Look!» نشانهٔ حال استمراری است.", "حال استمراری"),
            card("c2", "املای ing برای travel و sit؟", "traveling و sitting (sit دوبل می‌شود، travel در انگلیسیِ کتابِ درسی یک l می‌گیرد).", "املای ing"),
            card("c3", "چرا «He is wanting» غلط است؟", "چون want فعلِ حالتی است و در استمراری به‌کار نمی‌رود: He wants.", "حال استمراری"),
            card("c4", "معنی boarding pass و luggage؟", "boarding pass: کارت پرواز؛ luggage: چمدان‌ها/بارِ سفر.", "واژگان سفر"),
            card("c5", "«What are you doing?» چه زمانی پرسیده می‌شود؟", "وقتی می‌خواهیم بدانیم طرف مقابل همین حالا مشغولِ چه کاری است.", "مکالمه"),
        ),
        questions = listOf(
            mcq("q1", "تکمیل: Look! The children — in the pool.", listOf("swim", "are swimming", "is swimming", "swims"), "are swimming", "children جمع → are + swimming.", "حال استمراری", 1, "s1"),
            mcq("q2", "کدام جمله نادرست است؟", listOf("I am packing my bag.", "She is knowing the answer.", "They are waiting outside.", "We are checking in."), "She is knowing the answer.", "know فعلِ حالتی است: She knows the answer.", "حال استمراری", 3, "s2"),
            mcq("q3", "مرتب‌سازی: at / is / the / landing / plane / moment / the", listOf("The plane is landing at the moment.", "The plane landing is at the moment.", "At the moment is landing the plane.", "Is the plane at the moment landing."), "The plane is landing at the moment.", "فاعل + is + فعل + ing + قیدِ زمان.", "ترتیب جمله", 2, "s2"),
            short("q4", "سه واژهٔ سفر با تعریفشان (انگلیسی) بنویس.", "airport: a place where planes take off · boarding pass: a paper for your seat on the plane · timetable: a book of times of buses and trains", "تمرینِ واژگانِ تعریفی.", "واژگان سفر", 2, "s4"),
            short("q5", "سه جمله بنویس که همین حالا در کلاس در جریان است.", "نمونه: The teacher is speaking. My friend is writing in his notebook. We are listening carefully.", "تمرینِ نوشتارِ استمراری.", "نوشتار", 2, "s3"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "پاسخ تمرین‌ها با چرایی",
                "جای‌خالی: is coming (Look! + مفرد) · is buying (now + مفرد) · are waiting (we) · am packing (I) · aren't sleeping / are watching (منفیِ جمع + فعلِ هم‌زمان).\n" +
                    "املای ing: running/sitting (دوبل‌شدنِ CVC) · making/writing (حذفِ e) · studying (بدونِ تغییر).\n" +
                    "تصحیح خطا: ۱) want استمراری نمی‌گیرد. ۲) پیش از airport حرف تعریفِ the می‌آید (مکانِ شناخته‌شده). ۳) فاعلِ جمع → are. ۴) پس از be، فعلِ ing می‌آید (traveling).\n" +
                    "مرتب‌سازی: سؤال = کلمهٔ پرسشی + is/are + فاعل + فعل+ing؛ خبر = فاعل + is/are + فعل+ing + قیدِ زمان.\n" +
                    "خواندن: پاسخ‌ها در متن پیداست (at the airport / checking the luggage / at 10:30).\n" +
                    "نوشتار: قالبِ امن = فاعل + am/is/are + فعل+ing + جای/زمان؛ برای تنوع یک جملهٔ منفی هم اضافه کن (My brother isn't studying now).",
            ),
        ),
        summary = "کتاب کار درس ۲ روی حال استمراری تمرین می‌دهد: جای‌خالی، املای ing، تصحیح خطا، مرتب‌سازی جمله، خواندنِ متنِ فرودگاهی و نوشتنِ چهار جمله از کارهای همین حالا. " +
            "واژه‌های سفر (airport، boarding pass، luggage، timetable) هم با تمرینِ وصل‌کردن به تعریف مرور می‌شوند.",
        examTips = "۱) am/is/are + فعل+ing با نشانه‌های now / at the moment / Look!.\n۲) املای ing: running، making، writing، studying.\n" +
            "۳) فعل‌های حالتی (know، want، like) استمراری نمی‌گیرند.\n۴) ترتیبِ جمله و سؤال در استمراری.\n۵) واژه‌های سفر با تعریف انگلیسی.",
    )

    /** Lesson 3 — Festivals and Ceremonies: تمرین‌های حال ساده، ضمایر و قیدها. */
    private fun l03() = pack(
        lessonId = "L03",
        title = "Lesson 3 — Festivals and Ceremonies (تمرین‌ها)",
        sections = listOf(
            sec(
                "s1", "تمرین ۱ و ۲: حال ساده و سوم‌شخص مفرد", "important",
                "تمرین ۱ — فعل را درست به‌کار ببر:\n" +
                    "۱) My mother — (bake) a cake for the ceremony. → bakes\n" +
                    "۲) We — (not / watch) TV on Yalda; we — (read) poems. → don't watch / read\n" +
                    "۳) — your father — (sing) the national anthem? → Does / sing\n" +
                    "۴) The students — (decorate) the class every year. → decorate\n" +
                    "۵) Ali — (not / wear) special clothes on nature day. → doesn't wear\n" +
                    "تمرین ۲ — شکل سوم‌شخص مفرد را بنویس: watch → watches · go → goes · study → studies · have → has · do → does · carry → carries",
            ),
            sec(
                "s2", "تمرین ۳ و ۴: ضمایر مفعولی و صفت‌های ملکی", "important",
                "تمرین ۳ — جای‌خالی را با ضمیر مفعولی پر کن:\n" +
                    "۱) I invited — to the party. (they) → them\n" +
                    "۲) This gift is for —. (I) → me\n" +
                    "۳) Please help — with the table. (she) → her\n" +
                    "۴) We visited — last week. (he) → him\n" +
                    "تمرین ۴ — صفت ملکی درست را انتخاب کن:\n" +
                    "۱) — (We/Our) ceremony starts at 9. → Our\n" +
                    "۲) — (Their/Them) guests came early. → Their\n" +
                    "۳) — (She/Her) dress is traditional. → Her\n" +
                    "۴) The cat is playing with — (it's/its) toy. → its\n" +
                    "📌 یادآوری: it's = it is؛ its = صفت ملکی.",
            ),
            sec(
                "s3", "تمرین ۵ و ۶: قیدهای تکرار + خواندن و نوشتن", "exam",
                "تمرین ۵ — قیدِ تکرار را در جای درست بگذار:\n" +
                    "۱) We (usually) hold a ceremony in the school. → We usually hold a ceremony in the school.\n" +
                    "۲) He is (always) kind to guests. → He is always kind to guests.\n" +
                    "۳) She (never) reads poems. → She never reads poems.\n" +
                    "تمرین ۶ — خواندن: «My name is Sara. In my town, we celebrate the first day of spring. We clean our house and buy new clothes. On nature day, we go out and eat lunch in the park. My grandmother always bakes a special cake.»\n" +
                    "۱) Which day does Sara celebrate? → The first day of spring.\n" +
                    "۲) What does her grandmother do? → She always bakes a special cake.\n" +
                    "تمرین ۷ — نوشتن: پنج جمله دربارهٔ یک مراسمِ خانوادگی بنویس (با صفت ملکی و قیدِ تکرار).\n" +
                    "نمونهٔ پاسخ: Our family celebrates Yalda every year. We always set the table with fruit. My mother sometimes makes a special dinner. We read poems of Hafez and my father reads them aloud. Their guests usually come at night.",
            ),
            sec(
                "s4", "تمرین ۸: واژگان جشن‌ها + دام‌های امتحانی", "note",
                "واژه‌ها: invitation (دعوت‌نامه) · guest (مهمان) · decorate (تزیین‌کردن) · fireworks (آتش‌بازی) · national anthem (سرود ملی) · traditional (سنتی) · relatives (اقوام) · hold a ceremony (مراسم برگزار کردن)\n" +
                    "دام‌های رایج: ۱) She doesn't bakes ✗ → doesn't bake ✓ ۲) Do she decorate ✗ → Does she decorate ✓ ۳) I invited she ✗ → I invited her ✓ ۴) Them house ✗ → Their house ✓ ۵) He is never kind ✗ (معنی می‌دهد ولی در تمرین‌ها «never» پیش از فعلِ اصلی می‌آید: He never sings) — جای استانداردِ قیدِ تکرار را رعایت کن.",
            ),
        ),
        cards = listOf(
            card("c1", "شکل سوم‌شخص فعل‌های watch و study؟", "watches و studies (es و ies).", "حال ساده"),
            card("c2", "«I invited — (they)» چه می‌شود؟", "them — ضمیر مفعولی.", "ضمایر"),
            card("c3", "فرق it's و its؟", "it's = it is؛ its = صفت ملکی (مالِ آن).", "ملکی"),
            card("c4", "جای قید تکرار با فعل to be؟", "بعد از to be: He is always kind.", "قید تکرار"),
            card("c5", "معنی invitation و fireworks؟", "invitation: دعوت‌نامه؛ fireworks: آتش‌بازی.", "واژگان جشن"),
        ),
        questions = listOf(
            mcq("q1", "تکمیل: My uncle — a ceremony every spring.", listOf("hold", "holds", "holding", "is hold"), "holds", "سوم‌شخص مفرد + عادتِ هرساله.", "حال ساده", 1, "s1"),
            mcq("q2", "کدام جمله درست است؟", listOf("I invited they to the party.", "I invited them to the party.", "I invited their to the party.", "I invited theirs to the party."), "I invited them to the party.", "پس از فعل، ضمیر مفعولی: them.", "ضمایر", 2, "s2"),
            mcq("q3", "تکمیل: — your sister wear traditional clothes on nature day?", listOf("Do", "Does", "Is", "Are"), "Does", "سوم‌شخص مفرد در پرسشِ حال ساده → Does + فعلِ ساده.", "حال ساده", 2, "s1"),
            short("q4", "دو جمله با its و it's بنویس.", "نمونه: The cat is playing with its toy. / It's a beautiful ceremony.", "تمرینِ تمایزِ دو شکلِ شبیه.", "ملکی", 2, "s2"),
            short("q5", "سه واژهٔ جشن با معنی بنویس.", "guest (مهمان)، invitation (دعوت‌نامه)، fireworks (آتش‌بازی) — یا decorate، traditional، relatives", "واژگان درس.", "واژگان جشن", 1, "s4"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "پاسخ تمرین‌ها با چرایی",
                "جای‌خالی: bakes (مادر، سوم‌شخص) · don't watch / read (فاعلِ we و عادتِ هرساله) · Does … sing (پرسشِ سوم‌شخص با فعلِ ساده) · decorate (جمع) · doesn't wear (منفیِ سوم‌شخص).\n" +
                    "سوم‌شخص: watches/studies/has/does — قاعدهٔ es بعد از ch/sh/s/x/o و ies بعد از صامت+y.\n" +
                    "ضمایر: them/me/her/him — همیشه پس از فعل یا حرف اضافه.\n" +
                    "ملکی: Our/Their/Her/its — پیش از اسم؛ و it's همیشه به‌معنی it is است.\n" +
                    "قیدها: پیش از فعلِ اصلی (usually hold) و بعد از to be (is always kind).\n" +
                    "خواندن: پاسخ‌ها مستقیم در متن‌اند (first day of spring / always bakes a special cake).\n" +
                    "نوشتار: برای نمرهٔ کامل، هر جمله یک قیدِ تکرار + یک صفت ملکی داشته باشد و فعل با فاعل هم‌خوان باشد.",
            ),
        ),
        summary = "کتاب کار درس ۳ تمرین‌های حال ساده (سوم‌شخص مفرد، منفی و پرسشی)، ضمایر مفعولی و صفت‌های ملکی و قیدهای تکرار را با جای‌خالی، پرسش، " +
            "خواندنِ متنِ یک مراسم و نوشتنِ پنج‌جمله‌ای مرور می‌کند؛ به‌همراه واژه‌های جشن و فهرستِ دام‌های امتحانی.",
        examTips = "۱) s/es/ies سوم‌شخص مفرد.\n۲) Does/Doesn't + فعلِ ساده.\n۳) me/you/him/her/it/us/them در برابر my/your/his/her/its/our/their.\n" +
            "۴) جای قیدِ تکرار.\n۵) دام‌ها: doesn't bakes، invited she، them house.",
    )

    /** Lesson 4 — Services: تمرین‌های سؤالات Wh و واژه‌های خدمات. */
    private fun l04() = pack(
        lessonId = "L04",
        title = "Lesson 4 — Services (تمرین‌ها)",
        sections = listOf(
            sec(
                "s1", "تمرین ۱ و ۲: کلمهٔ پرسشی و ترتیبِ سؤال", "important",
                "تمرین ۱ — کلمهٔ پرسشی درست را انتخاب کن:\n" +
                    "۱) — does the bank open? — At 8. → When\n" +
                    "۲) — are you sending the parcel? — To Tehran. → Where\n" +
                    "۳) — does this book cost? — 80 thousand tomans. → How much\n" +
                    "۴) — stamps do you need? — Two, please. → How many\n" +
                    "۵) — helps you at the post office? — My brother. → Who\n" +
                    "۶) — are you late? — Because the bus was full. → Why\n" +
                    "تمرین ۲ — مرتب کن:\n" +
                    "۱) does / when / the / open / bank / ? → When does the bank open?\n" +
                    "۲) you / where / do / live / ? → Where do you live?\n" +
                    "۳) much / does / how / cost / it / ? → How much does it cost?\n" +
                    "📌 الگو: کلمهٔ پرسشی + فعل کمکی + فاعل + فعل اصلی؟",
            ),
            sec(
                "s2", "تمرین ۳ و ۴: واژگان خدمات و مکالمهٔ کامل‌کردنی", "important",
                "تمرین ۳ — واژه را با تعریف وصل کن:\n" +
                    "۱) a paper that shows you paid → receipt\n" +
                    "۲) money you pay for using something → charge\n" +
                    "۳) when you need help very quickly → emergency\n" +
                    "۴) a list of food in a restaurant → menu\n" +
                    "۵) to put money in your bank account → deposit\n" +
                    "تمرین ۴ — مکالمه را کامل کن:\n" +
                    "A: Good morning. — I help you? → Can\n" +
                    "B: Yes, please. I'd like to — some money. → exchange\n" +
                    "A: — do you want to exchange? → How much\n" +
                    "B: Two hundred dollars, please.\n" +
                    "A: Please — this form. → fill out\n" +
                    "B: Thank you. — is the commission? → How much",
            ),
            sec(
                "s3", "تمرین ۵ و ۶: خواندن و نوشتن (نمونهٔ پاسخ)", "exam",
                "تمرین ۵ — خواندن: «The City Bank is open from 8 to 14 from Saturday to Wednesday. On Thursday it closes at 12. You can deposit or withdraw money, exchange dollars and pay your bills there. For lost cards, call the emergency number 1554.»\n" +
                    "۱) How many hours is the bank open on Saturday? → Six hours (8 to 14).\n" +
                    "۲) What can you do there? → Deposit or withdraw money, exchange dollars and pay bills.\n" +
                    "۳) What is 1554 for? → For lost cards (the emergency number).\n" +
                    "تمرین ۶ — نوشتن: مکالمه‌ای چهارخطی در یک رستوران بنویس (سفارش، قیمت، تشکر).\n" +
                    "نمونهٔ پاسخ: Waiter: Can I help you? / I: Yes, I'd like a chicken sandwich, please. / Waiter: Anything to drink? / I: No, thanks. How much is it? / Waiter: 95 thousand tomans. / I: Here you are. Thank you.",
            ),
            sec(
                "s4", "تمرین ۷: تلفظ و دام‌های امتحانی", "note",
                "تلفظ/آهنگ: در سؤال‌های Wh صدای آخر جمله پایین می‌آید (↘) ولی در سؤال‌های بله/خیر بالا می‌رود (↗):\n" +
                    "Where do you live? ↘ · Do you live in Tehran? ↗\n" +
                    "دام‌ها: ۱) How much books ✗ → How many books ✓ ۲) How many money ✗ → How much money ✓ ۳) Where you are going? ✗ → Where are you going? ✓ ۴) Who does help you? ✗ → Who helps you? ✓ (Who فاعل است)",
            ),
        ),
        cards = listOf(
            card("c1", "برای پرسیدن ساعتِ باز شدن بانک چه می‌پرسیم؟", "When does the bank open?", "Wh-questions"),
            card("c2", "فرق How much و How many؟", "much برای قیمت/غیرقابل‌شمارش، many برای جمعِ قابل‌شمارش.", "Wh-questions"),
            card("c3", "معنی receipt و charge؟", "receipt: رسید؛ charge: هزینه/کارمزد.", "واژگان خدمات"),
            card("c4", "اگر Who فاعل باشد، سؤال چه تفاوتی دارد؟", "فعل کمکی نمی‌آید: Who helps you?", "Wh-questions"),
            card("c5", "«فرم را پر کن» به انگلیسی؟", "Fill out the form.", "عبارت‌های خدمات"),
        ),
        questions = listOf(
            mcq("q1", "تکمیل: — do you go to the dentist?", listOf("How much", "How often", "How many", "Whose"), "How often", "پرسش از تعدادِ دفعات → How often (twice a year).", "Wh-questions", 3, "s1"),
            mcq("q2", "کدام جمله درست است؟", listOf("How many money do you need?", "How much money do you need?", "How much moneys do you need?", "How many moneys do you need?"), "How much money do you need?", "money غیرقابل‌شمارش است → How much.", "Wh-questions", 2, "s1"),
            mcq("q3", "تکمیل: — helps you at the bank?", listOf("Who", "Whom", "Whose", "What"), "Who", "Who به‌عنوان فاعل: بدونِ فعل کمکی.", "Wh-questions", 2, "s1"),
            short("q4", "سه واژهٔ خدمات با تعریف انگلیسی بنویس.", "receipt: a paper that shows you paid · menu: a list of food in a restaurant · emergency: when you need help very quickly", "تمرینِ واژگانِ تعریفی.", "واژگان خدمات", 2, "s2"),
            short("q5", "چهار خط مکالمه در مغازه بنویس (پرسشِ قیمت و پرداخت).", "نمونه: — Can I help you? — Yes, how much is this notebook? — 40 thousand tomans. — Here you are. Thank you.", "کاربردی‌سازیِ Wh-questions.", "مکالمه", 2, "s3"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "پاسخ تمرین‌ها با چرایی",
                "کلمهٔ پرسشی: When (ساعت) · Where (مکان) · How much (قیمت) · How many (تعدادِ جمع) · Who (فاعل/شخص) · Why (دلیل).\n" +
                    "مرتب‌سازی: الگوی ثابت — کلمهٔ پرسشی + فعل کمکی + فاعل + فعل اصلی.\n" +
                    "واژگان: receipt/charge/emergency/menu/deposit — نکته: «charge» در بانک به‌معنی کارمزد و در مغازه به‌معنی هزینه است.\n" +
                    "مکالمه: Can I help you? / exchange / How much / fill out — این چهار عبارت، ستونِ مکالمه‌های خدماتی‌اند.\n" +
                    "خواندن: عدد و ساعت در متن است (8 to 14 = شش ساعت؛ استثنای پنجشنبه تا 12).\n" +
                    "نوشتار: مکالمه را با سلام، درخواستِ مؤدبانه (I'd like …)، پرسشِ قیمت و تشکر پایان بده.",
            ),
        ),
        summary = "کتاب کار درس ۴ تمرین‌های سؤالات Wh را پوشش می‌دهد: انتخاب کلمهٔ پرسشی، مرتب‌سازیِ سؤال، مکالمهٔ کامل‌کردنی در بانک، " +
            "خواندنِ متنِ خدماتِ بانکی و نوشتنِ مکالمهٔ رستوران؛ به‌همراه واژه‌های receipt، charge، emergency، menu، deposit و تمرینِ آهنگِ سؤال.",
        examTips = "۱) شش کلمهٔ پرسشی اصلی + کاربردِ هرکدام.\n۲) How much ↔ How many.\n۳) ترتیبِ سؤال و حالتِ فاعلیِ Who.\n" +
            "۴) واژه‌های خدمات و عبارت‌های مؤدبانه (I'd like … , Fill out the form).\n۵) آهنگِ سؤال Wh (پایین‌رونده) در برابر سؤال بله/خیر (بالارونده).",
    )

    /** Lesson 5 — Media: تمرین‌های گذشتهٔ ساده با افعال باقاعده. */
    private fun l05() = pack(
        lessonId = "L05",
        title = "Lesson 5 — Media (تمرین‌ها)",
        sections = listOf(
            sec(
                "s1", "تمرین ۱ و ۲: گذشتهٔ ساده و املای ed", "important",
                "تمرین ۱ — فعل را به گذشته ببر:\n" +
                    "۱) I — (watch) a documentary last night. → watched\n" +
                    "۲) She — (not / listen) to the radio yesterday. → didn't listen\n" +
                    "۳) — you — (call) your friend? → Did / call\n" +
                    "۴) The students — (study) English two hours ago. → studied\n" +
                    "۵) We — (play) football in the park. → played\n" +
                    "تمرین ۲ — املای ed: live → lived · like → liked · study → studied · stop → stopped · carry → carried · visit → visited\n" +
                    "📌 قاعده: فعلِ پایانیِ e فقط d می‌گیرد؛ صامت+y → ied؛ یک‌هجاییِ CVC دوبل می‌شود.",
            ),
            sec(
                "s2", "تمرین ۳ و ۴: منفی، پرسشی و was/were", "important",
                "تمرین ۳ — منفی و پرسشی کن:\n" +
                    "۱) He watched the news. → He didn't watch the news. / Did he watch the news?\n" +
                    "۲) They played a game. → They didn't play a game. / Did they play a game?\n" +
                    "۳) I listened to music. → I didn't listen to music. / Did you listen to music?\n" +
                    "تمرین ۴ — was یا were:\n" +
                    "۱) My sister — at home last night. → was\n" +
                    "۲) We — at the cinema yesterday. → were\n" +
                    "۳) — you tired after the match? → Were\n" +
                    "۴) The film — very interesting. → was\n" +
                    "📌 دام: Did you watched …؟ ✗ → Did you watch …؟ ✓ (پس از Did فعلِ ساده).",
            ),
            sec(
                "s3", "تمرین ۵ و ۶: خواندن و نوشتن (نمونهٔ پاسخ)", "exam",
                "تمرین ۵ — خواندن: «Last weekend my family and I visited my grandparents. In the evening, we watched a football match on TV. My mother didn't watch it; she listened to the radio in the kitchen. My father called my uncle and they talked for an hour.»\n" +
                    "۱) Who did they visit? → Their grandparents.\n" +
                    "۲) What did they watch? → A football match.\n" +
                    "۳) What did the mother do? → She listened to the radio in the kitchen.\n" +
                    "تمرین ۶ — نوشتن: پنج جمله بنویس که دیروز/آخر هفته انجام دادی (یک منفی و یک پرسشی هم داشته باش).\n" +
                    "نمونهٔ پاسخ: Last Friday I visited my cousin. We played computer games and watched a cartoon. I didn't do my homework in the morning. I studied English at night. Did you watch the match yesterday?",
            ),
            sec(
                "s4", "تمرین ۷: واژگان رسانه + تلفظ ed", "note",
                "واژه‌ها: channel (شبکه) · program (برنامه) · the news (اخبار) · magazine (مجله) · website (وب‌سایت) · advertisement (آگهی) · documentary (مستند) · message (پیام) · download/upload (دانلود/آپلود) · turn on/off (روشن/خاموش کردن)\n" +
                    "تلفظ ed: /t/ در watched و stopped · /d/ در played و listened · /ɪd/ در wanted و visited.\n" +
                    "📌 نکتهٔ شنیداری: «visited» سه‌هجا شنیده می‌شود؛ اگر در فایل صوتی «visid» شنیدی، شکلِ /ɪd/ است.",
            ),
        ),
        cards = listOf(
            card("c1", "شکل گذشتهٔ stop و study؟", "stopped و studied.", "املای ed"),
            card("c2", "پرسشیِ «He watched the news» چیست؟", "Did he watch the news?", "گرامر گذشته"),
            card("c3", "was را برای چه فاعل‌هایی می‌آوریم؟", "I / he / she / it؛ برای بقیه were.", "گرامر گذشته"),
            card("c4", "تلفظ ed در played؟", "/d/ — چون فعل با صدای واکدار تمام می‌شود.", "تلفظ"),
            card("c5", "معنی advertisement و documentary؟", "آگهی و مستند.", "واژگان رسانه"),
        ),
        questions = listOf(
            mcq("q1", "تکمیل: We — a documentary last night.", listOf("watch", "watched", "watching", "are watching"), "watched", "last night ⇒ گذشتهٔ سادهٔ باقاعده.", "گرامر گذشته", 1, "s1"),
            mcq("q2", "کدام جمله درست است؟", listOf("Did you watched the match?", "Did you watch the match?", "Do you watched the match?", "Did you watching the match?"), "Did you watch the match?", "پس از Did فعلِ ساده می‌آید.", "گرامر گذشته", 2, "s2"),
            mcq("q3", "تکمیل: My parents — at home yesterday evening.", listOf("was", "were", "is", "are"), "were", "parents جمع ⇒ were.", "گرامر گذشته", 1, "s2"),
            short("q4", "چهار واژهٔ رسانه با معنی بنویس.", "channel (شبکه)، program (برنامه)، magazine (مجله)، advertisement (آگهی) — یا documentary، website، message", "واژگان رسانه.", "واژگان رسانه", 1, "s4"),
            short("q5", "سه جملهٔ گذشته بنویس: یکی مثبت، یکی منفی، یکی پرسشی.", "نمونه: I watched a film last night. / I didn't play games. / Did you listen to music?", "تمرینِ سه حالتِ گذشته.", "نوشتار", 2, "s3"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "پاسخ تمرین‌ها با چرایی",
                "جای‌خالی: watched (last night) · didn't listen (منفی با فعلِ ساده) · Did … call (پرسشیِ گذشته) · studied (صامت+y) · played (عادی).\n" +
                    "املای ed: lived/liked (e داری فقط d) · studied/carried (y → ied) · stopped (دوبل‌شدن) · visited (عادی).\n" +
                    "منفی و پرسشی: همیشه didn't/Did + فعلِ ساده — نه شکلِ گذشته.\n" +
                    "was/were: مفرد was، جمع were؛ در پرسش، was/were جلوی فاعل می‌آید (Were you tired?).\n" +
                    "خواندن: پاسخ‌ها مستقیم در متن‌اند (grandparents / a football match / radio in the kitchen).\n" +
                    "نوشتار: برای نمرهٔ کامل، قیدِ زمان گذشته (last …/yesterday) + یک جملهٔ منفی + یک جملهٔ پرسشی بگذار؛ همه را با did/didn't سازگار نگه دار.",
            ),
        ),
        summary = "کتاب کار درس ۵ گذشتهٔ سادهٔ افعالِ باقاعده را تمرین می‌دهد: جای‌خالی، املای ed، ساختنِ منفی و پرسشی، was/were، " +
            "خواندنِ متنِ آخر هفته و نوشتنِ پنج جملهٔ گذشته؛ به‌همراه واژه‌های رسانه و تمرینِ تلفظِ ed.",
        examTips = "۱) ed: watched/studied/stopped/lived.\n۲) didn't/Did + فعلِ ساده.\n۳) was/were با فاعلِ درست.\n" +
            "۴) تلفظ ed: /t/ /d/ /ɪd/.\n۵) واژه‌های رسانه و قیدهای زمانِ گذشته.",
    )

    /** Lesson 6 — Health and Injuries: تمرین‌های گذشته با افعال بی‌قاعده. */
    private fun l06() = pack(
        lessonId = "L06",
        title = "Lesson 6 — Health and Injuries (تمرین‌ها)",
        sections = listOf(
            sec(
                "s1", "تمرین ۱ و ۲: گذشتهٔ افعال بی‌قاعده", "important",
                "تمرین ۱ — شکل گذشته را بنویس: go → went · have → had · eat → ate · drink → drank · break → broke · take → took · feel → felt · see → saw · catch → caught · hurt → hurt\n" +
                    "تمرین ۲ — جای‌خالی:\n" +
                    "۱) My brother — (break) his arm last week. → broke\n" +
                    "۲) I — (have) a terrible headache yesterday. → had\n" +
                    "۳) She — (not / take) her medicine last night. → didn't take\n" +
                    "۴) We — (go) to the hospital at 8. → went\n" +
                    "۵) He — (feel) better after a day of rest. → felt\n" +
                    "۶) I — (hurt) my leg while playing football. → hurt (بدونِ تغییر)\n" +
                    "📌 دام: «I hurted my leg» ✗ — hurt بی‌قاعده است.",
            ),
            sec(
                "s2", "تمرین ۳ و ۴: واژگان بیماری و مکالمهٔ مطب", "important",
                "تمرین ۳ — واژه را با تعریف وصل کن:\n" +
                    "۱) pain in your head → headache\n" +
                    "۲) you can't talk easily and your throat hurts → sore throat\n" +
                    "۳) a high body temperature → fever\n" +
                    "۴) you broke a bone in your arm → broken arm\n" +
                    "۵) a small injury with a knife → cut\n" +
                    "تمرین ۴ — مکالمه را کامل کن:\n" +
                    "Doctor: What's the —? → matter\n" +
                    "Patient: I have a — and I didn't sleep well. → headache\n" +
                    "Doctor: Did you — any medicine? → take\n" +
                    "Patient: Yes, but it didn't help.\n" +
                    "Doctor: You — rest and drink warm water. → should\n" +
                    "Patient: Thank you, doctor. I'll come back if it doesn't get better.",
            ),
            sec(
                "s3", "تمرین ۵ و ۶: خواندن و نوشتن (نمونهٔ پاسخ)", "exam",
                "تمرین ۵ — خواندن: «Yesterday Ali didn't come to school. He had a bad stomachache. His mother took him to the doctor. The doctor told him to rest and not to eat fast food. Ali stayed in bed and drank a lot of water. Today he feels much better.»\n" +
                    "۱) Why didn't Ali come to school? → Because he had a bad stomachache.\n" +
                    "۲) What did the doctor tell him? → To rest and not to eat fast food.\n" +
                    "۳) How does he feel today? → Much better.\n" +
                    "تمرین ۶ — نوشتن: یک یادداشتِ سه‌جمله‌ای برای معلم بنویس و دلیلِ غیبتت را توضیح بده.\n" +
                    "نمونهٔ پاسخ: Dear Mr. Ahmadi, I was absent yesterday because I had a fever and my mother took me to the doctor. I studied the lesson at home. Thank you for your help.",
            ),
            sec(
                "s4", "تمرین ۷: عبارت‌های درمانی + اشتباه‌های رایج", "note",
                "عبارت‌ها: see a doctor (پزشک را دیدن) · take medicine (دارو خوردن) · stay in bed (در رختخواب ماندن) · get well soon (زود خوب شو) · twice a day (دو بار در روز) · What happened? (چه شد؟)\n" +
                    "اشتباه‌های رایج: ۱) He goed to the doctor ✗ → went ✓ ۲) I didn't took it ✗ → didn't take ✓ ۳) I have a fever yesterday ✗ → had ✓ ۴) She has a broken arm last week ✗ → had ✓ ۵) What's the matter? ✓ (شکلِ درستِ پرسیدنِ حالِ بیمار)",
            ),
        ),
        cards = listOf(
            card("c1", "شکل گذشتهٔ feel، catch و hurt؟", "felt، caught و hurt (بی‌تغییر).", "افعال بی‌قاعده"),
            card("c2", "«دیشب تب داشتم» کدام است؟", "I had a fever last night.", "افعال بی‌قاعده"),
            card("c3", "معنی sore throat و broken arm؟", "sore throat: گلودرد؛ broken arm: شکستگیِ دست.", "واژگان سلامت"),
            card("c4", "«زود خوب شو» به انگلیسی؟", "Get well soon!", "عبارت‌های درمانی"),
            card("c5", "برای توصیه چه فعل کمکی به‌کار می‌رود؟", "should — You should rest.", "توصیه"),
        ),
        questions = listOf(
            mcq("q1", "تکمیل: She — her ankle last week.", listOf("break", "broke", "breaked", "breaking"), "broke", "break بی‌قاعده است: broke.", "افعال بی‌قاعده", 2, "s1"),
            mcq("q2", "تکمیل: I — to the dentist two days ago.", listOf("go", "went", "goed", "going"), "went", "go → went؛ قیدِ دو روز پیش.", "افعال بی‌قاعده", 1, "s1"),
            mcq("q3", "کدام جمله درست است؟", listOf("I didn't took my medicine.", "I didn't take my medicine.", "I not took my medicine.", "I don't took my medicine."), "I didn't take my medicine.", "منفیِ گذشته: didn't + فعلِ ساده.", "گرامر گذشته", 2, "s1"),
            short("q4", "چهار بیماری/آسیب با معنی بنویس.", "headache (سردرد)، fever (تب)، sore throat (گلودرد)، broken arm (شکستگیِ دست) — یا cut، stomachache، cough", "واژگان سلامت.", "واژگان سلامت", 1, "s2"),
            short("q5", "سه توصیهٔ پزشکی با should بنویس.", "نمونه: You should rest at home. / You should take this pill twice a day. / You should drink warm water.", "کاربردِ should.", "توصیه", 2, "s4"),
        ),
        solutions = listOf(
            StudyPack.Solution(
                "sol1", "پاسخ تمرین‌ها با چرایی",
                "جای‌خالی: broke (بی‌قاعده + قیدِ last week) · had (گذشتهٔ have) · didn't take (منفی با فعلِ ساده) · went (بی‌قاعده) · felt (بی‌قاعده) · hurt (بدونِ تغییر).\n" +
                    "واژگان: headache/sore throat/fever/broken arm/cut — تعریف‌ها را با معنیِ فارسی جفت کن تا در آزمونِ وصل‌کردن اشتباه نشود.\n" +
                    "مکالمهٔ مطب: What's the matter? / take medicine / should — سه عبارتِ کلیدیِ این مکالمه‌اند.\n" +
                    "خواندن: پاسخ‌ها در متن‌اند (stomachache / rest + no fast food / much better).\n" +
                    "نوشتار: یادداشتِ غیبت را با خطابِ محترمانه، دلیلِ گذشته (I was absent because …) و یک جملهٔ کوتاهِ توضیحی بنویس؛ در پایان تشکر بگذار.",
            ),
        ),
        summary = "کتاب کار درس ۶ گذشتهٔ افعالِ بی‌قاعده را تمرین می‌دهد (went، had، broke، took، felt، hurt)، واژه‌های بیماری و آسیب را با تعریف مرور می‌کند، " +
            "مکالمهٔ مطب و پرسشِ حالِ بیمار (What's the matter?) را کامل می‌کند و با خواندن و نوشتنِ یادداشتِ غیبت پایان می‌یابد.",
        examTips = "۱) جدولِ فعل‌های بی‌قاعده — مخصوصاً hurt که تغییر نمی‌کند.\n۲) didn't/Did + فعلِ ساده.\n" +
            "۳) had برای بیماریِ گذشته.\n۴) واژه‌های بیماری + سه توصیهٔ پزشکی با should.\n۵) عبارت‌های What's the matter? و Get well soon!",
    )

    /** درس‌های ۲ تا ۶ کتاب کار. */
    val packs: List<StudyPack> = listOf(l02(), l03(), l04(), l05(), l06())
}
