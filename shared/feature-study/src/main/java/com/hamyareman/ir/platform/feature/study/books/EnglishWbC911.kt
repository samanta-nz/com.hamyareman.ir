package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.BookModule
import com.hamyareman.ir.platform.feature.study.StudyPack

/**
 * ماژول کتاب «کتاب کار زبان انگلیسی پایه نهم» (C911) — معماری مصوب: کل محتوای درس داخل کد.
 * منبع تألیف: PDF کتاب درسی ریپو.
 */
object EnglishWbC911 {
    private fun l01(): StudyPack = StudyPack(
        packId = "C911_L01", bookCode = "C911", lessonId = "L01",
        title = "Lesson 1 — Personality (تمرین‌ها)", bookTitle = "کتاب کار زبان انگلیسی پایه نهم", pdfFileName = "C911_L01_BOOK.pdf",
        audioFileId = "C911_L01_AUDIO.mp3",
        sections = listOf(
            StudyPack.Section(id = "s1", title = "تمرین ۱ و ۲: انتخاب درست و to be", kind = "important", body = "تمرین ۱ — Choose the correct forms:\n1. Kate **isn't** funny.  2. There **is** a car in the street.  3. There **are** fifteen benches in the class.  4. It **is** really beautiful.  5. Iranians **are** very brave.\nتمرین ۲ — Fill in the blanks (to be): I am Ali Rasooli. I **am** 14 years old. My school **is** beautiful. There **are** 30 students... My classmates **are** clever and friendly. Mr. Ahmadi ... He **is** hard-working but he **isn't** nervous at all. He's **very** kind and patient."),
            StudyPack.Section(id = "s2", title = "تمرین ۳: مرتب‌کردن جمله‌ها", kind = "important", body = "1. I am not nervous.\n2. You and your friend are not selfish.\n3. Is Mina careless?\n4. Our house has two rooms → There are two rooms in our house.\n5. Is there an orange on the table?"),
            StudyPack.Section(id = "s3", title = "تمرین ۴: وصل جمله‌ها به تصاویر", kind = "note", body = "My teacher is kind. / The man is cruel. / The girl is quiet. / They are not neat. / There is a book on the desk. / There are five students in the classroom.\nهر جمله را با تصویرِ هم‌معنایش وصل کنید."),
            StudyPack.Section(id = "s4", title = "تمرین ۶: جدول کلمات (Personality)", kind = "important", body = "A) شش واژه‌ی شخصیت در جدول حروف‌بازی: BRAVE، ANGRY، CLEVER (و neat، rude...).\nB) ستون‌ها:\nPositive: brave, neat, clever, kind...\nNegative: angry, rude, selfish, careless...\nC) جمله‌ها را با کلمات مناسب پر کنید."),
            StudyPack.Section(id = "s5", title = "نکات امتحانی کتاب کار", kind = "exam", body = "۱) There is + مفرد / There are + جمع.\n۲) I→am؛ He/She/It→is؛ We/You/They→are.\n۳) منفی: isn't / aren't / I'm not.\n۴) سوال There: Is there...? / Are there...?"),
        ),
        flashcards = listOf(
            StudyPack.Flashcard("c1", "Kate ..... funny — is یا are؟", "isn't — چون Kate مفرد است.", "to be", ""),
            StudyPack.Flashcard("c2", "There ..... a car — is یا are؟", "is — a car مفرد است.", "There is/are", ""),
            StudyPack.Flashcard("c3", "There ..... fifteen benches؟", "are — benches جمع است.", "There is/are", ""),
            StudyPack.Flashcard("c4", "کدام واژه در حروف‌بازی کتاب کار «شجاع» است؟", "BRAVE", "جدول کلمات", ""),
            StudyPack.Flashcard("c5", "You and your friend ..... (to be)", "are — فاعل دوتایی همیشه are می‌گیرد.", "to be", ""),
            StudyPack.Flashcard("c6", "سوال برای «an orange on the table»؟", "Is there an orange on the table?", "There is/are", ""),
            StudyPack.Flashcard("c7", "ترجمه‌ی «The girl is quiet»؟", "دختر آرام/ساکت است.", "واژگان", ""),
            StudyPack.Flashcard("c8", "Iranians ..... very brave.", "are — جمع است.", "to be", ""),
        ),
        questions = listOf(
            StudyPack.Question(id = "qq1", type = "mcq", text = "There ..... two rooms in our house.", options = listOf("is", "are", "am", "be"), answer = "are", explanation = "جمع → are.", topic = "There is/are", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq2", type = "short", text = "مرتب کن: Mina/is/careless/؟", options = emptyList(), answer = "Is Mina careless?", explanation = "سوال با Is آغاز می‌شود.", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq3", type = "mcq", text = "کدام گزینه منفی درست است؟", options = listOf("He isn't shy.", "He not shy is.", "He doesn't shy.", "He are not shy."), answer = "He isn't shy.", explanation = "شکل منفی مفرد: isn't (یا 's not).", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq4", type = "short", text = "پنج جمله‌ی تمرین ۴ را معنی کنید.", options = emptyList(), answer = "معلم من مهربان است؛ مرد بی‌رحم است؛ دختر آرام است؛ آنها مرتب نیستند؛ روی میز کتابی هست؛ پنج دانش‌آموز در کلاس هستند.", explanation = "تمرین وصل‌کردن به تصاویر.", topic = "واژگان", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq5", type = "mcq", text = "«There is a book on the desk» یعنی چه؟", options = listOf("روی میز کتاب‌هایی هست", "روی میز یک کتاب هست", "کتاب روی صندلی است", "میز کتاب ندارد"), answer = "روی میز یک کتاب هست", explanation = "There is = وجود دارد (مفرد).", topic = "There is/are", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq6", type = "short", text = "برای تصویر «She is angry» دو جمله‌ی مشابه بنویس.", options = emptyList(), answer = "نمونه: The man is angry. / My brother is angry.", explanation = "الگو: فاعل + is/are + صفت شخصیت.", topic = "واژگان", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq7", type = "mcq", text = "کدام ستون «rude» است؟", options = listOf("Positive", "Negative", "هر دو", "هیچ‌کدام"), answer = "Negative", explanation = "rude یعنی گستاخ؛ صفت منفی است.", topic = "واژگان", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq8", type = "short", text = "جمله را کامل کن: My classmates ..... clever and friendly.", options = emptyList(), answer = "are", explanation = "جمع → are.", topic = "to be", difficulty = 1, refSectionId = "s1"),
        ),
        solutions = listOf(
            StudyPack.Solution("sol1", "پاسخ تمرین ۱ — جواب و چرای کامل", "1. Kate isn't funny — چرا: Kate = she → is + not → isn't.\n2. There is a car — چرا: اولین اسم بعد از فعل (a car) مفرد است.\n3. There are fifteen benches — چرا: benches جمع.\n4. It is really beautiful — چرا: it → is.\n5. Iranians are very brave — چرا: جمع → are.\nنکته‌ی آزمونی: در There is/are فقط به اسمِ بلافاصله بعد از فعل نگاه کن؛ بقیه‌ی جمله مهم نیست."),
            StudyPack.Solution("sol2", "پاسخ تمرین ۲ — متن کامل با چرا", "I **am** Ali Rasooli. I **am** 14 years old. (I → am)\nMy school **is** beautiful. (school مفرد → is)\nThere **are** 30 students in my class. (students جمع → are)\nMy classmates **are** clever and friendly. (جمع → are)\nMr. Ahmadi ... He **is** hard-working but he **isn't** nervous at all. (he → is/isn't؛ at all فقط با منفی)\nHe's **very** kind and patient. (کوتاه‌نویسی He is → He's)\nچرا «at all» با isn't می‌آید؟ چون تقویت‌کننده‌ی منفی است: «اصلاً»."),
            StudyPack.Solution("sol3", "پاسخ تمرین ۳ — چرای مرتب‌سازی", "1. I am not nervous. — فاعل + to be + not + صفت.\n2. You and your friend are not selfish. — فاعل دوتایی → are.\n3. Is Mina careless? — سوال: to be اول.\n4. There are two rooms in our house. — rooms جمع → are؛ ترتیب: There + be + تعداد + اسم + مکان.\n5. Is there an orange on the table? — سوالِ There: Is there + a/an + اسم + مکان؟\nچرا ترتیب مهم است؟ چون انگلیسی زبانِ «ترتیبی» است؛ برخلاف فارسی، جابه‌جایی کلمات معنی را می‌شکند (Mina is careless ≠ Is Mina careless)."),
        ),
        summary = "کتاب کار درس ۱ تمرین‌های درسِ شخصیت است: جای‌خالیِ فعل to be، مرتب‌کردنِ جمله‌ها، وصل‌کردنِ جمله به تصویر، " +
            "جدولِ واژه‌های شخصیت و سؤال‌های پایان درس — همه با پاسخِ چرایی‌دار.",
        examTips = "۱) to be: I am · he/she/it is · you/we/they are؛ منفی: isn't/aren't.\n" +
            "۲) There is + مفرد / There are + جمع — فقط اسمِ بعد از فعل مهم است.\n" +
            "۳) سؤال: to be را اول جمله ببر (Is Mina careless?).\n" +
            "۴) ستون‌های واژگان: brave/neat/clever در برابر rude/selfish/careless.\n" +
            "۵) «at all» فقط در جملهٔ منفی می‌آید (he isn't nervous at all).",
    )
    val packs: List<StudyPack> = listOf(l01()) + EnglishWbC911Lessons.packs

    val module = BookModule(
        bookCode = "C911", title = "کتاب کار زبان انگلیسی پایه نهم", subject = "", packs = packs,
    )
}
