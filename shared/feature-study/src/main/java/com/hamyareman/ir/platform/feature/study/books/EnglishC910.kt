package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.BookModule
import com.hamyareman.ir.platform.feature.study.StudyPack

/**
 * ماژول کتاب «زبان انگلیسی پایه نهم» (C910) — معماری مصوب: کل محتوای درس داخل کد.
 * منبع تألیف: PDF کتاب درسی ریپو.
 */
object EnglishC910 {
    private fun l01(): StudyPack = StudyPack(
        packId = "C910_L01", bookCode = "C910", lessonId = "L01",
        title = "Lesson 1 — Personality (شخصیت)", bookTitle = "زبان انگلیسی پایه نهم", pdfFileName = "C910_L01_BOOK.pdf",
        audioFileId = "C910_L01_AUDIO.mp3",
        audio2FileId = "C910_L01_INTRO_AUDIO.mp3", audio2Title = "معرفی صداها (اختیاری)",
        sections = listOf(
            StudyPack.Section(id = "s1", title = "Conversation: دو پسرعمه درباره‌ی بهترین دوست", kind = "important", body = "Ehsan: Who is your best friend at school?\nParham: Reza.\nEhsan: What is he like?\nParham: Oh, he is really great! He's clever and kind.\nEhsan: Is he hard-working too?\nParham: Yes! And he's always very helpful.\nEhsan: How?\nParham: He always helps me with my lessons.\nترجمه: احسان: بهترین دوستت در مدرسه کیست؟ — پرهام: رضا. — او چه‌طور آدمی است؟ — واقعاً عالی است؛ باهوش و مهربان. — سخت‌کوش هم هست؟ — بله! و همیشه خیلی کمک‌گر است. — چطور؟ — همیشه در درس‌هایم کمکم می‌کند.\n📌 نکته‌ی امتحانی: What's he like? یعنی «او چه‌طور آدمی است؟» (شخصیت)؛ اما What does he like? یعنی «چه چیزهایی دوست دارد؟» — این دو را اشتباه نگیرید.\n📌 یادآوری کلاس هشتم: «He always helps me» — حال ساده با سوم‌شخص مفرد، فعل +s می‌گیرد (help→helps)."),
            StudyPack.Section(id = "s2", title = "Practice 1: سوال با Am/Is/Are", kind = "important", body = "Are you hard-working? — Yes, I am.\nIs he clever? — Yes, he is.\nIs Zahra talkative? — No, she isn't.\nAre they neat? — Yes, they are.\nAre they upset? — No, they're not.\nالگو: Are/Is + فاعل + صفت؟ جواب کوتاه: Yes/No + ضمیر + (n't) am/is/are.\n📌 یادآوری کلاس هفتم: I→am؛ he/she/it→is؛ you/we/they→are. برای سؤال، فعل to be به اول جمله می‌پرد."),
            StudyPack.Section(id = "s3", title = "Practice 2: What ... like؟", kind = "important", body = "What's your friend like? — He's very funny.\nWhat's your mother like? — She's very kind and patient.\nWhat's he like? — He is quiet.\nWhat's she like? — She is clever.\nWhat are you like? — I'm a bit serious.\nWhat are they like? — They are very kind.\n«What is he like?» برای پرسیدن شخصیت و اخلاق است."),
            StudyPack.Section(id = "s4", title = "واژه‌های شخصیت (صفت‌ها)", kind = "important", body = "مثبت: brave (شجاع)، neat (مرتب)، quiet (آرام)، funny (بامزه)، clever (باهوش)، kind (مهربان)، helpful (کمک‌گر)، hard-working (سخت‌کوش)، patient (صبور)، generous (بخشنده)، polite (مؤدب).\nمنفی: angry (خشمگین)، careless (بی‌دقت)، cruel (بی‌رحم)، rude (گستاخ)، nervous (عصبی)، selfish (خودخواه).\nهمچنین: talkative (پرحرف)، patient (صبور)، generous (بخشنده)، shy (خجالتی).\n📌 یادآوری: صفت همیشه بعد از فعل to be می‌آید: He is brave. / They are neat."),
            StudyPack.Section(id = "s5", title = "Language Melody: آهنگ جمله‌های خبری", kind = "note", body = "در جمله‌های خبری (affirmative) صدای آخر جمله پایین می‌آید:\nTeacher: Farzaneh is a clever student. Everybody likes her.\nSamira: Yes. I know. She is also very helpful.\nتمرین: 1. He's very kind. 2. She's very patient. 3. You are very clever. 4. Everybody likes her. 5. I do my homework. 6. She works for a company."),
            StudyPack.Section(id = "s6", title = "Grammar: فعل to be و There is/are", kind = "important", body = "I am — He/She/It is — We/You/They are:\nAli is clever. / It is red. / Zahra and Nadia are generous.\nپرسشی: Am I...? / Is he...? / Are you...?\nمنفی: I'm not talkative. / He isn't shy. / They are not rude.\nکوتاه‌نویسی: I'm, He's, It's, We're, You're, They're, isn't, aren't.\nThere is + مفرد: There is an eraser in the classroom.\nThere are + جمع: There are two computers in the classroom.\nپرسش: Is there an apple on the table? — Yes, there is. / No, there isn't.\nکلید مفرد/جمع: a، an، one → مفرد؛ two، three، many، some → جمع.\n📌 یادآوری کلاس هفتم: an قبل از واکه (an apple، an eraser)؛ a قبل از صامت (a car)."),
            StudyPack.Section(id = "s7", title = "Talk to Your Teacher + نکات", kind = "note", body = "«Let me check it in the dictionary.» — بگذار در فرهنگ‌لغت چکش کنم.\nبرای پرسیدن معنی: What's the meaning of ...? / What does ... mean?"),
            StudyPack.Section(id = "s8", title = "Find it: فعل‌های to be را پیدا کن (صفحه‌ی ۲۳)", kind = "exam",
                body = "متن: I'm Mohsen. This is my classroom. There are 25 students in my class. I have a lot of friends. My best friend is Vahid. He's a good student. He is helpful and hard-working, but he is not very careful. He usually forgets important things. It's a big problem.\nترجمه: من محسن هستم. این کلاس من است. ۲۵ دانش‌آموز در کلاس من هستند. دوستان زیادی دارم. بهترین دوستم وحید است. او دانش‌آموز خوبی است؛ مفید و سخت‌کوش، اما خیلی با‌دقت نیست و معمولاً چیزهای مهم را فراموش می‌کند. این یک مشکل بزرگ است.\nپاسخ: فعل‌های to be: I'm، is (This is)، are (There are)، is (my best friend is)، 's (He's)، is، is (he is not)، It's."),
            StudyPack.Section(id = "s9", title = "Tell Your Classmates: پنج جمله درباره‌ی خودت و خانواده (صفحه‌ی ۲۳)", kind = "concept",
                body = "مثال کتاب: My sister is really kind.\nالگو: فاعل + to be + صفت. نمونه‌ها: My father is brave. / My mother is kind. / My brother is clever. / My best friend is funny. / I am neat.\n📌 یادآوری: برای «من» همیشه am؛ برای هر مفردِ غیر از I فعل is است."),
            StudyPack.Section(id = "s10", title = "Listening & Speaking A: جدول Name و Personality (صفحه‌ی ۲۴)", kind = "concept",
                body = "به مکالمه‌ی فایل صوتی کتاب گوش کن؛ در ستون Name نام شخص و در ستون Personality سه صفت او را بنویس. متن مکالمه در کتاب چاپ نشده است."),
            StudyPack.Section(id = "s11", title = "Listening & Writing B: ایران (صفحه‌ی ۲۴)", kind = "concept",
                body = "1. What's Iran like? → Iran is a beautiful (great) country.\n2. What are Iranian people like? → Iranian people are brave, kind, hard-working and friendly.\n📌 دقت: در سؤال دوم فاعل جمع (people) است → are."),
            StudyPack.Section(id = "s12", title = "کارت‌های A و B (صفحه‌ی ۲۶) + Role Play (صفحه‌ی ۲۷)", kind = "exam",
                body = "کارت A — سوال‌ها: 1. Are you brave? 2. Is your brother talkative? 3. Are your family members neat? 4. Who is brave? 5. Who is friendly? 6. What's your father like?\nپاسخ‌های نمونه (کارت B): 1. Yes, I am. 2. No, he isn't. 3. Yes, they are. 4. Ali is brave. 5. My teacher is friendly. 6. He is kind and serious.\nRole Play: درباره‌ی friends / classmates / teachers / relatives گفت‌وگو کن.\n📌 یادآوری جواب کوتاه: از همان to be سؤال استفاده می‌کنیم."),
        ),
        flashcards = listOf(
            StudyPack.Flashcard("c1", "معنی «hard-working» چیست؟", "سخت‌کوش، پرتلاش.", "واژگان", ""),
            StudyPack.Flashcard("c2", "معنی «selfish» و «generous»؟", "selfish: خودخواه؛ generous: بخشنده.", "واژگان", "متضاد یکدیگرند"),
            StudyPack.Flashcard("c3", "What's he like? چه زمانی به‌کار می‌رود؟", "برای پرسیدن شخصیت و اخلاقِ کسی؛ جواب با صفت: He's clever.", "Practice 2", ""),
            StudyPack.Flashcard("c4", "جواب کوتاه Is she neat? چیست؟", "Yes, she is. / No, she isn't.", "Practice 1", ""),
            StudyPack.Flashcard("c5", "برای «they» کدام شکل to be می‌آید؟", "are — They are kind.", "Grammar", ""),
            StudyPack.Flashcard("c6", "شکل کوتاه «He is not» چیست؟", "He isn't. (یا He's not)", "Grammar", ""),
            StudyPack.Flashcard("c7", "There is یا There are: «... two students»", "There are two students — جمع با are.", "Grammar", ""),
            StudyPack.Flashcard("c8", "معنی «careless» و «rude»؟", "careless: بی‌دقت/بی‌احتیاط؛ rude: بی‌ادب/گستاخ.", "واژگان", ""),
            StudyPack.Flashcard("c9", "معنی «brave»؟", "شجاع.", "واژگان", ""),
            StudyPack.Flashcard("c10", "در Language Melody صدای آخر جمله‌ی خبری چه می‌کند؟", "پایین می‌آید.", "Language Melody", ""),
            StudyPack.Flashcard("c11", "معنی «generous» چیست؟", "بخشنده — Zahra and Nadia are generous.", "واژگان", ""),
            StudyPack.Flashcard("c12", "چطور بپرسیم کسی پرحرف است؟", "Is he/she talkative?", "Grammar / Speaking", ""),
        ),
        questions = listOf(
            StudyPack.Question(id = "qq1", type = "mcq", text = "Choose the correct form: Kate ..... funny.", options = listOf("isn't", "aren't", "am not", "not is"), answer = "isn't", explanation = "Kate مفرد سوم‌شخص است → is + not = isn't.", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq2", type = "mcq", text = "Choose: There ..... fifteen benches in the class.", options = listOf("is", "are", "am", "be"), answer = "are", explanation = "benches جمع است → there are.", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq3", type = "short", text = "جمله را مرتب کن: am/I/nervous/not/.", options = emptyList(), answer = "I am not nervous.", explanation = "ترتیب: فاعل + to be + not + صفت.", topic = "Grammar", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq4", type = "short", text = "جمله را مرتب کن: and/you/your friend/selfish/not/are/.", options = emptyList(), answer = "You and your friend are not selfish.", explanation = "فاعل دوتایی → are.", topic = "Grammar", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq5", type = "short", text = "سوال بساز: Mina/is/careless/؟", options = emptyList(), answer = "Is Mina careless?", explanation = "Is + فاعل + صفت + ? — جواب: Yes, she is.", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq6", type = "short", text = "جمله را مرتب کن: there/an/orange/table/is/on the/؟", options = emptyList(), answer = "Is there an orange on the table?", explanation = "سوال There: Is there + ... ?", topic = "Grammar", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq7", type = "mcq", text = "در مکالمه، Parham درباره‌ی شخصیت رضا چه می‌گوید؟", options = listOf("He's quiet and shy", "He's clever and kind", "He's rude", "He's careless"), answer = "He's clever and kind", explanation = "کلمه‌به‌کلمه از متن مکالمه.", topic = "Conversation", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq8", type = "mcq", text = "پرسیدن «شخصیت مادرت چه‌طور است؟» به انگلیسی:", options = listOf("Who is your mother?", "What's your mother like?", "How is your mother do?", "What does your mother like?"), answer = "What's your mother like?", explanation = "الگوی What + be + فاعل + like؟", topic = "Practice 2", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq9", type = "short", text = "با کلمه‌ی «patient» یک جمله بنویس.", options = emptyList(), answer = "نمونه: My teacher is very patient.", explanation = "هر جمله‌ی درست با patient پذیرفته است.", topic = "واژگان", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq10", type = "mcq", text = "کدام واژه شخصیت منفی است؟", options = listOf("brave", "neat", "selfish", "polite"), answer = "selfish", explanation = "brave/neat/polite مثبت‌اند؛ selfish یعنی خودخواه.", topic = "واژگان", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq11", type = "short", text = "How do you ask if someone is a talkative person?", options = emptyList(), answer = "Is he/she talkative?", explanation = "با آوردن Is به ابتدای جمله، سؤال بله/خیر ساخته می‌شود.", topic = "Grammar", difficulty = 1, refSectionId = "s2"),
        ),
        solutions = listOf(
            StudyPack.Solution("sol1", "پاسخ کتاب کار تمرین ۱ — چرای هر گزینه", "1. Kate **isn't** funny → چرا؟ Kate مفرد سوم‌شخص است؛ to be برای he/she/it فقط «is» است، پس منفی‌اش «isn't».\n2. There **is** a car → چرا؟ بعد از There، فعل با «اولین اسم بعدی» سازگار است؛ a car مفرد → is.\n3. There **are** fifteen benches → چرا؟ benches جمع → are.\n4. It **is** really beautiful → چرا؟ it همیشه is.\n5. Iranians **are** very brave → چرا؟ Iranians جمع (حرف آخر s) → are.\nقاعده‌ی کلی: I→am؛ he/she/it و هر مفرد→is؛ you/we/they و هر جمع→are."),
            StudyPack.Solution("sol2", "پاسخ کتاب کار تمرین ۲ — چرا این to beها؟", "I **am** 14 years old → چرا؟ «I» همیشه am.\nMy school **is** beautiful → چرا؟ my school یک چیز مفرد = he/she/it خانواده → is.\nThere **are** 30 students → چرا؟ students جمع → are.\nMy classmates **are** clever → چرا؟ جمع → are.\nHe **is** hard-working → he → is.\nhe **isn't** nervous at all → چرا منفی؟ چون «but he ... nervous at all» منطق جمله یعنی «اصلاً عصبی نیست»؛ at all فقط در جمله‌ی منفی می‌آید و معنی «اصلاً» می‌دهد.\nچرا نکته‌ی at all مهم است؟ چون در مثبت نمی‌آید: He is nervous at all ❌."),
            StudyPack.Solution("sol3", "پاسخ تمرین ۳ — چرا جمله این‌طور مرتب می‌شود؟", "I am not nervous → چرا؟ الگوی جمله‌ی انگلیسی ثابت است: فاعل + فعل (to be) + بقیه. «not» همیشه بلافاصله بعد از to be می‌آید.\nYou and your friend are not selfish → چرا are؟ چون فاعلِ دوتایی (شما + دوستت) مثل you جمع است.\nIs Mina careless? → چرا Is اول؟ در جمله‌ی پرسشی، to be جلوی فاعل می‌پرد (Is + فاعل + صفت؟) — سواژینور (inversion).\nIs there an orange on the table? → چرا Is there؟ «there» در سوال هم مثل فاعل رفتار می‌کند: Is there...? / Are there...?\nقاعده‌ی طلایی مرتب‌کردن: ۱) فاعل را پیدا کن ۲) to be را بعدش بگذار ۳) در سوال، to be برود اول جمله."),
            StudyPack.Solution("sol4", "Find it (ص۲۲) — چرا این‌ها to be هستند؟", "I'm = I am؛ This is: این مفرد است؛ There are: students جمع؛ my best friend is: مفرد؛ He's = He is؛ he is not very careful: منفی با not؛ It's = It is. هر جای جمله که «بودن/وصف/وجود» معنا بدهد فعل am/is/are است."),
            StudyPack.Solution("sol5", "Role Play (ص۲۶) — پاسخ کارت B با چرایی", "1. Yes, I am (سؤال با are، جواب با am چون فاعل I). 2. No, he isn't (brother مفرد). 3. Yes, they are (family members جمع). 4. Ali is brave (سوم‌شخص مفرد → is). 5. My teacher is friendly (مفرد). 6. He is kind and serious (What...like → جواب با صفت)."),
            StudyPack.Solution("sol6", "Listening B (ص۲۴) — پاسخ ایران با چرایی", "1. Iran is a beautiful/great country (Iran مفرد → is + a + صفت). 2. Iranian people are brave, kind, hard-working and friendly (people جمع → are)."),
        ),
        summary = "درس ۱ زبان نهم دربارهٔ شخصیت آدم‌هاست: با فعل to be (am/is/are) و صفت‌ها توصیف می‌کنیم، " +
            "با «What's he like?» شخصیت می‌پرسیم و با There is/are از وجودِ چیزها حرف می‌زنیم. " +
            "جوابِ کوتاه (Yes, he is / No, she isn't)، صفت‌های مثبت و منفی و کلمات پرسشیِ همین درس در تمرین‌های کتاب به‌کار می‌آیند.",
        examTips = "۱) جدولِ to be: I am · he/she/it is · you/we/they are (+ کوتاه‌نویسی‌ها).\n" +
            "۲) پرسشی و منفی: Is he...? / isn't — Are they...? / aren't.\n" +
            "۳) There is + مفرد ↔ There are + جمع و پاسخِ کوتاهِ Yes, there is/are.\n" +
            "۴) تفاوتِ What's he like? (شخصیت) با What does he like? (علاقه).\n" +
            "۵) صفت‌ها: kind، helpful، hard-working، neat، selfish، careless، nervous.",
    )

    /** درس ۱ + درس‌های ۲ تا ۹ (از `EnglishC910Lessons`؛ شاملِ Review 1/2/3) — کلِ کتاب. */
    val packs: List<StudyPack> = listOf(l01()) + EnglishC910Lessons.packs

    val module = BookModule(
        bookCode = "C910", title = "زبان انگلیسی پایه نهم", subject = "", packs = packs,
    )
}
