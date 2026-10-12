package com.hamyareman.ir.ui.content

/** Titles and navigation metadata only; all authored payloads live in bucket HTML. */
object BuiltInContent {
    val recipes: List<Recipe> = listOf(
        Recipe("omelet-sabzi", "املت سبزی", 15, 2, "آسان", emptyList(), emptyList(), ""),
        Recipe("smoothie-moz-khorma", "اسموتی موز و خرما", 5, 1, "خیلی آسان", emptyList(), emptyList(), ""),
        Recipe("toast-panir-gerdoo", "نان تست و پنیر و گردو", 10, 1, "خیلی آسان", emptyList(), emptyList(), ""),
        Recipe("soup-jo", "سوپ جو", 40, 3, "آسان", emptyList(), emptyList(), "")
    )
    val lessons: List<Lesson> = listOf(
        Lesson("lesson-spaced-review", "مرور فاصله‌دار", "مهارت مطالعه", 0, ""),
        Lesson("lesson-cornell-notes", "یادداشت‌برداری کورنل", "مهارت مطالعه", 0, ""),
        Lesson("lesson-percent", "درصد و تخفیف", "ریاضی", 7, ""),
        Lesson("lesson-exam-night", "مدیریت شب امتحان", "مهارت مطالعه", 0, ""),
        Lesson("", "", "", 9, ""),
        Lesson("", "", "", 9, ""),
        Lesson("", "", "", 9, ""),
        Lesson("", "", "", 9, ""),
        Lesson("", "", "", 9, "")
    )
    val quizzes: List<QuizQuestion> = emptyList()
    val learningNodes: List<LearningNode> = listOf(
        LearningNode("node-basics-1", "مبانی: خواندن فعال", "مهارت مطالعه", 1, ""),
        LearningNode("node-basics-2", "مبانی: یادداشت‌برداری", "مهارت مطالعه", 2, "node-basics-1"),
        LearningNode("node-practice-1", "تمرین: مرور فاصله‌دار", "مهارت مطالعه", 3, "node-basics-2"),
        LearningNode("node-practice-2", "تمرین: آزمون از خود", "مهارت مطالعه", 4, "node-practice-1"),
        LearningNode("node-project-1", "پروژه: برنامه‌ی هفته‌ی خودت", "مهارت مطالعه", 5, "node-practice-2"),
        LearningNode("node-math-1", "ریاضی: نسبت و تناسب", "ریاضی", 1, ""),
        LearningNode("node-math-2", "ریاضی: درصد و تخفیف", "ریاضی", 2, "node-math-1"),
        LearningNode("node-math-3", "ریاضی: خواندن نمودار", "ریاضی", 3, "node-math-2"),
        LearningNode("node-ai-what", "هوش مصنوعی چیست و چه کاری نمی‌کند", "هوش مصنوعی", 1, ""),
        LearningNode("node-ai-data", "داده: سوختِ مدل", "هوش مصنوعی", 2, "node-ai-what"),
        LearningNode("node-ai-math", "پیش‌نیاز ریاضی: درصد، نسبت، میانگین", "هوش مصنوعی", 3, "node-math-1"),
        LearningNode("node-ai-ml", "یادگیری ماشین: سه نوع یادگیری", "هوش مصنوعی", 5, "node-ai-pattern"),
        LearningNode("node-ai-llm", "مدل زبانی بزرگ، توکن و پرامپت", "هوش مصنوعی", 8, "node-ai-nn")
    )
    val artPrompts: List<ArtPrompt> = listOf(
        ArtPrompt("art-leaf", "برگ پاییزی", "", "آرام"),
        ArtPrompt("art-tea", "فنجان چای بخاردار", "", "آرام"),
        ArtPrompt("art-cat", "گربه‌ی خوابیده", "", "بازیگوش"),
        ArtPrompt("art-window", "پنجره‌ی باران‌خورده", "", "دلگیر"),
        ArtPrompt("art-shoes", "کفش‌های کهنه‌ی خودت", "", "روزمره"),
        ArtPrompt("art-half-face", "چهره‌ی نیمه در سایه", "", "عاطفی"),
        ArtPrompt("art-hand-pencil", "دست خودت با مداد", "", "تمرین"),
        ArtPrompt("art-cloth-doll", "عروسک پارچه‌ای", "", "کودکی")
    )
}
