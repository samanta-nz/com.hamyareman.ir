package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.ui.components.LinedNotebookInput
import com.hamyareman.ir.ui.study.StateSync
import com.hamyareman.ir.ui.wellness.PracticeHubScreen
import com.hamyareman.ir.ui.wellness.WellnessMenu
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

private val SelfQuestions = listOf(
    "امروز چه چیزی حالت را کمی بهتر کرد؟",
    "به خودت چه جملهٔ مهربانی می‌گویی؟",
    "امروز از چه چیزی دلت خواست بیشتر بدانی؟",
    "کدام لحظهٔ امروز ارزش ثبت‌شدن داشت؟",
    "اگر امروز فقط یک کار کوچک خوب انجام دهی، چیست؟",
    "امروز به کدام احساس خودت گوش دادی؟",
    "چه چیزی را از امروز می‌خواهی فردا تکرار کنی؟",
    "امروز کجا با خودت مهربان بودی؟",
    "امروز چه لطف کوچکی از کسی دیدی؟",
    "امروز چه چیزی دربارهٔ خودت یاد گرفتی؟",
    "کدام انتخاب امروزت را دوست داشتی؟",
    "اگر امروز را دوباره بسازی، چه چیزی را تغییر می‌دهی؟",
    "این روزها بیشتر به چه چیزی نیاز داری؟",
    "چه کاری به تو احساس آرامش می‌دهد؟",
    "امروز از چه چیزی به‌خاطر خودت تشکر می‌کنی؟",
    "کدام نگرانی را می‌توانی فقط برای امشب کنار بگذاری؟",
    "چه چیزی باعث شد امروز احساس توانمندی کنی؟",
    "امروز چه مرزی را بهتر رعایت کردی؟",
    "دوست داری فردای تو با چه احساسی شروع شود؟",
    "کدام عادت کوچک به تو کمک می‌کند؟",
    "اگر ترس نبود، امروز چه کاری می‌کردی؟",
    "چه کسی امروز به تو انرژی خوب داد؟",
    "کدام فکر تکراری دیگر به کارت نمی‌آید؟",
    "امروز بدنت چه پیامی به تو داد؟",
    "به کدام موفقیت کوچک خودت افتخار می‌کنی؟",
    "امروز چه چیزی را لازم نبود کامل انجام دهی؟",
    "کدام ویژگی خودت را بیشتر دوست داری؟",
    "چه چیزی باعث شد امروز لبخند بزنی؟",
    "امروز چه چیزی را بخشیدی یا رها کردی؟",
    "چه کمکی را می‌توانی از دیگران بخواهی؟",
    "این هفته دوست داری چه چیزی یاد بگیری؟",
    "امروز کجا شجاع بودی؟",
    "کدام کار کوچک تو را به هدفت نزدیک کرد؟",
    "اگر دوستت جای تو بود، به او چه می‌گفتی؟",
    "امروز چه چیزی تمرکزت را گرفت؟",
    "چه زمانی بیشترین حس خودِ واقعی‌بودن را داشتی؟",
    "کدام صدای درونی را بهتر است آرام‌تر کنی؟",
    "امروز چه چیزی ارزش صبرکردن داشت؟",
    "برای فردا چه قول کوچکی به خودت می‌دهی؟",
    "کدام تجربهٔ سخت تو را قوی‌تر کرده است؟",
    "امروز چه چیزی از کنترل تو خارج بود؟",
    "کدام بخش روز در کنترل تو بود؟",
    "چه چیزی را دوست داری بیشتر تمرین کنی؟",
    "امروز به چه چیزی نه گفتی یا می‌خواستی نه بگویی؟",
    "کدام رابطه به توجه آرام تو نیاز دارد؟",
    "امروز از چه اشتباهی چیزی یاد گرفتی؟",
    "اگر امروز یک رنگ بود، چه رنگی می‌شد و چرا؟",
    "کدام فکر خوب را می‌خواهی با خودت نگه داری؟",
    "امروز چه چیزی برایت واقعاً مهم بود؟",
    "الان، همین لحظه، چه چیزی می‌تواند حالت را یک درجه بهتر کند؟",
)

private data class SelfReflection(val date: String, val question: String, val answer: String)

private fun readSelfReflections(store: LocalStore): List<SelfReflection> = runCatching {
    val root = JSONObject(store.getString("self_answers", "{}"))
    buildList {
        root.keys().forEach { date ->
            val item = root.optJSONObject(date) ?: return@forEach
            val answer = item.optString("answer")
            if (answer.isNotBlank()) add(SelfReflection(date, item.optString("question"), answer))
        }
    }.sortedByDescending { it.date }
}.getOrDefault(emptyList())

private fun dailySelfQuestion(date: LocalDate): String {
    val size = SelfQuestions.size
    val epoch = date.toEpochDay()
    val cycle = Math.floorDiv(epoch, size.toLong())
    val index = Math.floorMod(epoch, size.toLong()).toInt()
    val shuffled = SelfQuestions.shuffled(Random((cycle xor 0x48A91L).hashCode()))
    if (index > 0) return shuffled[index]
    val previous = SelfQuestions.shuffled(Random(((cycle - 1) xor 0x48A91L).hashCode())).last()
    val first = shuffled.first()
    return if (first != previous) first else shuffled[1]
}

/** هاب ذهن‌آگاهی؛ پرسش روزانه بعد از همهٔ کاشی‌ها و با فرم دفترچه قرار دارد. */
@Composable
fun AwarenessHubScreen(nav: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val store = remember { LocalStore(context, "hamyar_awareness") }
    val today = remember { LocalDate.now() }
    val dateKey = remember(today) { today.toString() }
    val question = remember(today) { dailySelfQuestion(today) }
    var answer by remember(dateKey) { mutableStateOf(store.getString("self_answer_$dateKey", "")) }
    var reflections by remember { mutableStateOf(readSelfReflections(store)) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dateKey) {
        val uid = container.auth.cachedUserId()
            ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isNotBlank()) {
            StateSync.pull(context, container.tables, uid, "self_reflections")?.first?.let { payload ->
                val remote = runCatching { JSONObject(payload) }.getOrNull() ?: return@let
                val remoteAnswer = remote.optJSONObject(dateKey)?.optString("answer").orEmpty()
                if (answer.isBlank() && remoteAnswer.isNotBlank()) {
                    answer = remoteAnswer
                    store.putString("self_answer_$dateKey", remoteAnswer)
                }
                val local = runCatching { JSONObject(store.getString("self_answers", "{}")) }.getOrDefault(JSONObject())
                remote.keys().forEach { key -> if (!local.has(key)) local.put(key, remote.get(key)) }
                store.putString("self_answers", local.toString())
                reflections = readSelfReflections(store)
            }
        }
    }

    PracticeHubScreen(
        nav = nav,
        rootIds = WellnessMenu.mindfulnessIds,
        title = "ذهن‌آگاهی 🪷",
        subtitle = "حضور، فکر سالم، یادگیری آرام",
        headerSlot = "hub.awareness.header",
        accKey = "acc_mindfulness",
        onBack = onBack,
        extraBottom = {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سؤال خودشناسی امروز", style = MaterialTheme.typography.titleMedium)
                    Text(question, style = MaterialTheme.typography.bodyLarge)
                    LinedNotebookInput(answer, { answer = it })
                    PrimaryButton("ذخیره و همگام‌سازی") {
                        store.putString("self_answer_$dateKey", answer)
                        val all = runCatching { JSONObject(store.getString("self_answers", "{}")) }.getOrDefault(JSONObject())
                        all.put(dateKey, JSONObject().put("question", question).put("answer", answer))
                        store.putString("self_answers", all.toString())
                        reflections = readSelfReflections(store)
                        scope.launch {
                            val uid = container.auth.cachedUserId()
                                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                            val synced = if (uid.isBlank()) false else StateSync.push(
                                context,
                                container.tables,
                                uid,
                                "self_reflections",
                                all.toString(),
                            )
                            notice = if (synced) "ذخیره و همگام شد." else "روی گوشی ذخیره شد؛ همگام‌سازی بعداً انجام می‌شود."
                        }
                    }
                    notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                }
            }
            if (reflections.isNotEmpty()) {
                Text("پاسخ‌های قبلی من", style = MaterialTheme.typography.titleMedium)
                reflections.forEach { reflection ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                com.hamyareman.ir.platform.core.common.toPersianDigits(reflection.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(reflection.question, style = MaterialTheme.typography.titleSmall)
                            Text(reflection.answer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
    )
}

/**
 * گوشه‌ی مطالعه‌ی غیردرسی — ماژول اختصاصی «قفسه‌ی من».
 * فاز فعلی: ثبت کتاب‌ها و وضعیت خواندن؛ اتصال به پلیر/متن کتاب در فاز بعد تکمیل می‌شود.
 */
@Composable
fun ReadingCornerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_reading") }
    var title by remember { mutableStateOf("") }
    val books = remember { store.getStringSet("my_shelf").filter { it.isNotBlank() }.sorted() }

    HubBody {
        HubHeader("قفسه‌ی من 📖", "مطالعه‌ی غیردرسی — دنیای خودت")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("نام کتابی که می‌خواهی بخوانی…") },
            modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            if (title.isNotBlank()) {
                store.putStringSet("my_shelf", store.getStringSet("my_shelf") + title.trim())
                title = ""
            }
        }) { Text("به قفسه اضافه کن") }

        if (books.isEmpty()) {
            Text("قفسه‌ات هنوز خالی است؛ اولین کتاب را اضافه کن ✨", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            books.forEach { Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text("📗 $it", Modifier.padding(12.dp)) } }
        }

        Text(
            "به‌زودی: پلیر مخصوص کتاب‌ها، هدف‌گذاری صفحات روزانه و پیشنهاد کتاب — این ماژول کامل جدا توسعه داده می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
