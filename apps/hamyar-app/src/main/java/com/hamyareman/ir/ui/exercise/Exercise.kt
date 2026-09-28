package com.hamyareman.ir.ui.exercise

data class ExerciseStep(val title: String, val seconds: Int)
data class Exercise(
    val id: String, val title: String, val category: ExerciseCategory,
    val summary: String, val steps: List<ExerciseStep>, val tips: String,
) {
    val totalDurationSeconds: Int get() = steps.sumOf { it.seconds }
    val totalDurationMinutes: Int get() = (totalDurationSeconds / 60).coerceAtLeast(1)
}
enum class ExerciseCategory(val displayName: String, val emoji: String) {
    YOGA("یوگا", "🧘"), STRETCH("کشش", "🙆"), STRENGTH("تقویت", "💪"), BREATH("تنفس", "🌬️")
}
