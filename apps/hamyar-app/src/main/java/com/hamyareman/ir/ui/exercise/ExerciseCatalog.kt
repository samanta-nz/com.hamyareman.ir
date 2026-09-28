package com.hamyareman.ir.ui.exercise

object ExerciseCatalog {
    val exercises: List<Exercise> = listOf(
        Exercise("yoga-morning-sun", "سلام بر خورشید", ExerciseCategory.YOGA, "شروع نرم روز با کشش‌های سبک.",
            listOf(ExerciseStep("دست‌ها بالا، نفس عمیق", 30), ExerciseStep("خم به جلو", 30), ExerciseStep("قدم به عقب", 30), ExerciseStep("سینه بالا", 30), ExerciseStep("بازگشت ایستاده", 30)),
            "اگر درد داشتی متوقف کن."),
        Exercise("stretch-neck-shoulders", "کشش گردن و شانه", ExerciseCategory.STRETCH, "برای خستگی مطالعه.",
            listOf(ExerciseStep("سر به راست", 20), ExerciseStep("سر به چپ", 20), ExerciseStep("شانه بالا", 15), ExerciseStep("شانه عقب", 15)),
            "اگر سرگیجه داشتی بنشین."),
        Exercise("strength-light-legs", "تقویت نرم پاها", ExerciseCategory.STRENGTH, "چند حرکت ساده.",
            listOf(ExerciseStep("اسکوات سبک", 30), ExerciseStep("راه‌رفتن درجا", 40), ExerciseStep("زانو به سینه", 30), ExerciseStep("نفس عمیق", 20)),
            "درد یعنی توقف."),
        Exercise("breath-box", "تنفس جعبه‌ای", ExerciseCategory.BREATH, "آرام‌سازی ۴-۴-۴-۴.",
            listOf(ExerciseStep("دم ۴", 4), ExerciseStep("نگه ۴", 4), ExerciseStep("بازدم ۴", 4), ExerciseStep("سکوت ۴", 4)),
            "سه تا پنج دور."),
        Exercise("yoga-cat-cow", "گربه و گاو", ExerciseCategory.YOGA, "نرمی ستون فقرات.",
            listOf(ExerciseStep("چهار دست و پا", 15), ExerciseStep("گربه", 20), ExerciseStep("گاو", 20), ExerciseStep("تکرار نرم", 20)),
            "با نفس هماهنگ کن."),
    )
    fun byId(id: String): Exercise? = exercises.firstOrNull { it.id == id }
}
