package com.hamyareman.ir.ui.exercise

object ExerciseCatalog {
    val exercises = listOf(
        Exercise("yoga-morning-sun", "سلام بر خورشید", ExerciseCategory.YOGA, "", emptyList(), "", 150),
        Exercise("stretch-neck-shoulders", "کشش گردن و شانه", ExerciseCategory.STRETCH, "", emptyList(), "", 70),
        Exercise("strength-light-legs", "تقویت نرم پاها", ExerciseCategory.STRENGTH, "", emptyList(), "", 120),
        Exercise("breath-box", "تنفس جعبه‌ای", ExerciseCategory.BREATH, "", emptyList(), "", 16),
        Exercise("yoga-cat-cow", "گربه و گاو", ExerciseCategory.YOGA, "", emptyList(), "", 75)
    )
    fun byId(id: String): Exercise? = exercises.firstOrNull { it.id == id }
}
