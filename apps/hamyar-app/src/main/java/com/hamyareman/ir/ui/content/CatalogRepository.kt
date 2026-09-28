package com.hamyareman.ir.ui.content

import io.appwrite.Query
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.appwrite.TableRow
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.ui.exercise.Exercise
import com.hamyareman.ir.ui.exercise.ExerciseCatalog
import com.hamyareman.ir.ui.exercise.ExerciseCategory
import com.hamyareman.ir.ui.exercise.ExerciseStep
import com.hamyareman.ir.ui.content.BuiltInContent as BuiltIn
import org.json.JSONArray
import org.json.JSONObject
import com.hamyareman.ir.platform.core.appwrite.ServerActions
import java.time.LocalDate

/**
 * کاتالوگ محتوای خواندنی.
 *
 * ترتیب اولویت (و چرا):
 *  1) **سرور** (`TablesDB`) — تا بشود محتوا را بدون انتشار نسخه‌ی تازه به‌روز کرد.
 *  2) **کش محلی** — تا با اینترنت ضعیف هم صفحه خالی نماند.
 *  3) **محتوای داخلی اپ** ([BuiltInContent]) — تا اپ کاملاً آفلاین هم کار کند.
 *
 * این جدول‌ها خواندنی‌اند (`read("any")`) و نوشتنشان فقط با کلید سرور
 * (`backend/seed/import.js`) انجام می‌شود.
 */
class CatalogRepository(
    private val tables: TablesDbService,
    private val store: LocalStore,
    private val serverActions: ServerActions? = null,
) {

    val isServerBacked: Boolean get() = tables.isConfigured

    /**
     * آیا کاتالوگِ کش‌شده هنوز معتبر است؟
     *
     * به‌جای خواندن چندصد سطر در هر بار بازکردن صفحه، اول اثر انگشت کاتالوگ از تابع
     * `catalog-digest` گرفته می‌شود (چند بایت). اگر با مقدار ذخیره‌شده یکی بود،
     * خواندن از سرور **کاملاً رد می‌شود** و همان کش استفاده می‌شود.
     *
     * نکات صداقت:
     *  - نتیجه تا [DIGEST_TTL_MS] در حافظه/دیسک نگه داشته می‌شود تا هر صفحه دوباره
     *    از سرور اثر انگشت نگیرد.
     *  - اگر digest عوض شده باشد، مقدار تازه ذخیره می‌شود و `false` برمی‌گردد ⇒
     *    همان مسیر قبلی (خواندن از سرور و نوشتن کش) اجرا می‌شود.
     *  - اگر تابع نبود/خطا داد ⇒ `false` ⇒ رفتار دقیقاً مثل قبل.
     */
    private suspend fun catalogIsFresh(): Boolean {
        val actions = serverActions ?: return false
        if (!actions.isConfigured) return false
        val saved = store.getString(KEY_DIGEST)
        if (saved.isBlank()) return false
        if (System.currentTimeMillis() - store.getLong(KEY_DIGEST_AT) < DIGEST_TTL_MS) return true

        return when (val result = actions.catalogDigest()) {
            is AppResult.Ok -> {
                store.putString(KEY_DIGEST, result.value.digest)
                store.putLong(KEY_DIGEST_AT, System.currentTimeMillis())
                result.value.digest == saved
            }
            is AppResult.Err -> false
        }
    }

    suspend fun recipes(): List<Recipe> = serverOrCacheOrBuiltIn(
        table = TableIds.RECIPES,
        cacheKey = CACHE_RECIPES,
        fromRow = { it.toRecipe() },
        toJson = { it.toJson() },
        fromJson = { recipeFromJson(it) },
        builtIn = BuiltIn.recipes,
    )

    suspend fun recipe(id: String): Recipe? = recipes().firstOrNull { it.id == id }

    suspend fun lessons(): List<Lesson> = serverOrCacheOrBuiltIn(
        table = TableIds.LESSONS,
        cacheKey = CACHE_LESSONS,
        fromRow = { it.toLesson() },
        toJson = { it.toJson() },
        fromJson = { lessonFromJson(it) },
        builtIn = BuiltIn.lessons,
    )

    suspend fun lesson(id: String): Lesson? = lessons().firstOrNull { it.id == id }

    suspend fun quizFor(lessonId: String?): List<QuizQuestion> =
        serverOrCacheOrBuiltIn(
            table = TableIds.QUIZZES,
            cacheKey = CACHE_QUIZZES,
            fromRow = { it.toQuiz() },
            toJson = { it.toJson() },
            fromJson = { quizQuestionFromJson(it) },
            builtIn = BuiltIn.quizzes,
        ).let { all -> if (lessonId.isNullOrBlank()) all else all.filter { it.lessonId == lessonId } }

    suspend fun learningNodes(): List<LearningNode> = serverOrCacheOrBuiltIn(
        table = TableIds.LEARNING_NODES,
        cacheKey = CACHE_NODES,
        fromRow = { it.toNode() },
        toJson = { it.toJson() },
        fromJson = { learningNodeFromJson(it) },
        builtIn = BuiltIn.learningNodes,
    )

    suspend fun artPrompts(): List<ArtPrompt> = serverOrCacheOrBuiltIn(
        table = TableIds.ART_PROMPTS,
        cacheKey = CACHE_ART,
        fromRow = { it.toArtPrompt() },
        toJson = { it.toJson() },
        fromJson = { artPromptFromJson(it) },
        builtIn = BuiltIn.artPrompts,
    )

    /**
     * فهرست ورزش/کشش.
     *
     * سرور و کاتالوگ داخلی اپ **ادغام** می‌شوند (سرور برنده است، چون ممکن است
     * محتوا بدون انتشار نسخه‌ی تازه عوض شود). کاتالوگ داخلی همیشه هست تا
     * تایمر ثانیه‌ای حرکات آفلاین هم کار کند.
     */
    suspend fun exercises(): List<Exercise> {
        val fromServer = serverOrCacheOrBuiltIn(
            table = TableIds.EXERCISES,
            cacheKey = CACHE_EXERCISES,
            fromRow = { it.toExercise() },
            toJson = { it.toJson() },
            fromJson = { exerciseFromJson(it) },
            builtIn = emptyList(),
        )
        val merged = LinkedHashMap<String, Exercise>()
        fromServer.forEach { merged[it.id] = it }
        ExerciseCatalog.exercises.forEach { merged.putIfAbsent(it.id, it) }
        return merged.values.toList()
    }

    suspend fun exercise(id: String): Exercise? = exercises().firstOrNull { it.id == id }

    /**
     * ایده‌ی روز: بر اساس روز سال انتخاب می‌شود تا در یک روز ثابت بماند
     * و «استریک» نقاشی معنی داشته باشد.
     */
    fun artPromptOfDay(prompts: List<ArtPrompt>, date: LocalDate = LocalDate.now()): ArtPrompt {
        val list = prompts.ifEmpty { BuiltIn.artPrompts }
        val index = (date.dayOfYear - 1).coerceAtLeast(0) % list.size
        return list[index]
    }

    // --- موتور مشترک -------------------------------------------------------

    private suspend fun <T> serverOrCacheOrBuiltIn(
        table: String,
        cacheKey: String,
        fromRow: (TableRow) -> T?,
        toJson: (T) -> JSONObject,
        fromJson: (JSONObject) -> T?,
        builtIn: List<T>,
    ): List<T> {
        if (tables.isConfigured && !catalogIsFresh()) {
            val result = tables.list(table, listOf(Query.limit(100)))
            if (result is AppResult.Ok) {
                val items = result.value.mapNotNull(fromRow)
                if (items.isNotEmpty()) {
                    writeCache(cacheKey, items, toJson)
                    return items
                }
            }
        }
        return readCache(cacheKey, fromJson).ifEmpty { builtIn }
    }

    private fun <T> writeCache(key: String, items: List<T>, toJson: (T) -> JSONObject) {
        val array = JSONArray()
        items.forEach { array.put(toJson(it)) }
        store.putString(key, array.toString())
    }

    private fun <T> readCache(key: String, fromJson: (JSONObject) -> T?): List<T> = runCatching {
        val array = JSONArray(store.getString(key, "[]"))
        buildList {
            for (i in 0 until array.length()) fromJson(array.getJSONObject(i))?.let { add(it) }
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY_DIGEST = "catalog_digest"
        private const val KEY_DIGEST_AT = "catalog_digest_at"

        /** هر ۱۲ ساعت یک‌بار اثر انگشت را از سرور می‌پرسیم، نه به‌ازای هر صفحه. */
        private const val DIGEST_TTL_MS = 12 * 60 * 60 * 1000L

        private const val CACHE_RECIPES = "catalog_recipes"
        private const val CACHE_LESSONS = "catalog_lessons"
        private const val CACHE_QUIZZES = "catalog_quizzes"
        private const val CACHE_NODES = "catalog_nodes"
        private const val CACHE_ART = "catalog_art"
        private const val CACHE_EXERCISES = "catalog_exercises"
    }
}

// --- نگاشت سطر سرور به مدل -----------------------------------------------

/**
 * گام‌های ورزش: هم آرایه‌ی رشته‌ای قبول می‌شود و هم آرایه‌ی
 * `{"title": "...", "seconds": 30}` تا ثانیه‌ی دقیق هر گام از سرور بیاید.
 */
private fun exerciseSteps(raw: String, totalMinutes: Int): List<ExerciseStep> = runCatching {
    val array = JSONArray(raw)
    val count = array.length()
    if (count == 0) return@runCatching emptyList()
    val fallbackSeconds = if (totalMinutes > 0) {
        ((totalMinutes * 60) / count).coerceAtLeast(5)
    } else {
        30
    }
    buildList {
        for (i in 0 until count) {
            when (val element = array.opt(i)) {
                is JSONObject -> {
                    val title = element.optString("title").ifBlank { element.optString("text") }
                    if (title.isNotBlank()) add(ExerciseStep(title, element.optInt("seconds", fallbackSeconds).coerceAtLeast(1)))
                }

                is String -> if (element.isNotBlank()) add(ExerciseStep(element, fallbackSeconds))
                else -> Unit
            }
        }
    }
}.getOrDefault(emptyList())

private fun parseExerciseCategory(raw: String): ExerciseCategory =
    ExerciseCategory.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) || it.displayName == raw.trim() }
        ?: ExerciseCategory.STRETCH

private fun stringList(raw: String): List<String> = runCatching {
    val array = JSONArray(raw)
    buildList { for (i in 0 until array.length()) add(array.optString(i)) }
}.getOrDefault(raw.split("\n").filter { it.isNotBlank() })

private fun TableRow.toRecipe(): Recipe = Recipe(
    id = id,
    title = string("title"),
    minutes = long("minutes").toInt(),
    servings = long("servings", 2).toInt(),
    difficulty = string("difficulty", "آسان"),
    ingredients = stringList(string("ingredients")),
    steps = stringList(string("steps")),
    tip = string("tip"),
)

private fun TableRow.toLesson(): Lesson = Lesson(
    id = id,
    title = string("title"),
    subject = string("subject"),
    grade = long("grade").toInt(),
    body = string("body"),
    bookCode = string("bookCode"),
    lessonNumber = long("lessonNumber").toInt(),
    videoUrl = string("videoUrl"),
    audioUrl = string("audioUrl"),
    chapterMarkers = stringList(string("chapterMarkers")).mapNotNull { it.toDoubleOrNull() },
)

private fun TableRow.toQuiz(): QuizQuestion? {
    val choices = stringList(string("choices"))
    if (choices.size < 2) return null
    val answer = long("answerIndex").toInt().coerceIn(0, choices.size - 1)
    return QuizQuestion(id, string("lessonId"), string("question"), choices, answer)
}

private fun TableRow.toNode(): LearningNode = LearningNode(
    id = id,
    title = string("title"),
    track = string("track"),
    orderIndex = long("orderIndex").toInt(),
    prerequisiteId = string("prerequisiteId"),
)

private fun TableRow.toExercise(): Exercise? {
    val title = string("title")
    if (title.isBlank()) return null
    val totalMinutes = long("minutes").toInt()
    val note = string("note")
    val steps = exerciseSteps(string("steps"), totalMinutes)
    if (steps.isEmpty()) return null
    return Exercise(
        id = id,
        title = title,
        category = parseExerciseCategory(string("category")),
        summary = note.ifBlank { title },
        steps = steps,
        tips = note,
    )
}

private fun TableRow.toArtPrompt(): ArtPrompt = ArtPrompt(
    id = id,
    title = string("title"),
    prompt = string("prompt"),
    moodTag = string("moodTag"),
)

// --- (de)serialization کش محلی -------------------------------------------

private fun Recipe.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("minutes", minutes).put("servings", servings)
    .put("difficulty", difficulty).put("tip", tip)
    .put("ingredients", JSONArray(ingredients)).put("steps", JSONArray(steps))

private fun recipeFromJson(o: JSONObject): Recipe? = runCatching {
    Recipe(
        id = o.getString("id"),
        title = o.getString("title"),
        minutes = o.optInt("minutes"),
        servings = o.optInt("servings", 2),
        difficulty = o.optString("difficulty", "آسان"),
        ingredients = o.optJSONArray("ingredients")?.let { a -> buildList { for (i in 0 until a.length()) add(a.optString(i)) } } ?: emptyList(),
        steps = o.optJSONArray("steps")?.let { a -> buildList { for (i in 0 until a.length()) add(a.optString(i)) } } ?: emptyList(),
        tip = o.optString("tip"),
    )
}.getOrNull()

private fun Lesson.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("subject", subject).put("grade", grade).put("body", body)
    .put("bookCode", bookCode).put("lessonNumber", lessonNumber)
    .put("videoUrl", videoUrl).put("audioUrl", audioUrl)
    .put("chapterMarkers", JSONArray(chapterMarkers))

private fun lessonFromJson(o: JSONObject): Lesson? = runCatching {
    val markers = o.optJSONArray("chapterMarkers")?.let { a ->
        buildList { for (i in 0 until a.length()) add(a.optDouble(i, 0.0)) }
    } ?: emptyList()
    Lesson(
        id = o.getString("id"),
        title = o.getString("title"),
        subject = o.optString("subject"),
        grade = o.optInt("grade"),
        body = o.optString("body"),
        bookCode = o.optString("bookCode"),
        lessonNumber = o.optInt("lessonNumber"),
        videoUrl = o.optString("videoUrl"),
        audioUrl = o.optString("audioUrl"),
        chapterMarkers = markers,
    )
}.getOrNull()

private fun QuizQuestion.toJson(): JSONObject = JSONObject()
    .put("id", id).put("lessonId", lessonId).put("question", question)
    .put("choices", JSONArray(choices)).put("answerIndex", answerIndex)

private fun quizQuestionFromJson(o: JSONObject): QuizQuestion? = runCatching {
    val choices = o.optJSONArray("choices")?.let { a -> buildList { for (i in 0 until a.length()) add(a.optString(i)) } } ?: emptyList()
    if (choices.size < 2) return@runCatching null
    QuizQuestion(
        id = o.getString("id"),
        lessonId = o.optString("lessonId"),
        question = o.getString("question"),
        choices = choices,
        answerIndex = o.optInt("answerIndex").coerceIn(0, choices.size - 1),
    )
}.getOrNull()

private fun LearningNode.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("track", track)
    .put("orderIndex", orderIndex).put("prerequisiteId", prerequisiteId)

private fun learningNodeFromJson(o: JSONObject): LearningNode? = runCatching {
    LearningNode(
        id = o.getString("id"),
        title = o.getString("title"),
        track = o.optString("track"),
        orderIndex = o.optInt("orderIndex"),
        prerequisiteId = o.optString("prerequisiteId"),
    )
}.getOrNull()

private fun ArtPrompt.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("prompt", prompt).put("moodTag", moodTag)

private fun artPromptFromJson(o: JSONObject): ArtPrompt? = runCatching {
    ArtPrompt(o.getString("id"), o.getString("title"), o.optString("prompt"), o.optString("moodTag"))
}.getOrNull()

private fun Exercise.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("category", category.name)
    .put("summary", summary).put("tips", tips)
    .put("steps", JSONArray().also { array ->
        steps.forEach { array.put(JSONObject().put("title", it.title).put("seconds", it.seconds)) }
    })

private fun exerciseFromJson(o: JSONObject): Exercise? = runCatching {
    val stepsArray = o.optJSONArray("steps") ?: return@runCatching null
    val steps = buildList {
        for (i in 0 until stepsArray.length()) {
            val step = stepsArray.getJSONObject(i)
            val title = step.optString("title")
            if (title.isNotBlank()) add(ExerciseStep(title, step.optInt("seconds", 30).coerceAtLeast(1)))
        }
    }
    if (steps.isEmpty()) return@runCatching null
    Exercise(
        id = o.getString("id"),
        title = o.getString("title"),
        category = runCatching { ExerciseCategory.valueOf(o.optString("category")) }.getOrDefault(ExerciseCategory.STRETCH),
        summary = o.optString("summary"),
        steps = steps,
        tips = o.optString("tips"),
    )
}.getOrNull()
