package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.wellness.PracticeHubScreen
import com.hamyareman.ir.ui.wellness.WellnessMenu
import java.util.Calendar

private val SelfQuestions = listOf(
    "امروز چه چیزی حالت را کمی بهتر کرد؟",
    "به خودت چه یک جمله‌ی مهربان می‌گفتی؟",
    "امروز از چه چیزی دلت خواست بیشتر بدانی؟",
    "کدام لحظه‌ی امروز ارزش ثبت‌شدن داشت؟",
    "اگر امروز فقط یک کار کوچکِ خوب می‌کردی، چه بود؟",
    "امروز به کدام احساسِ خودت گوش دادی؟",
    "چه چیزی را از امروز می‌خواهی فردا هم تکرار کنی؟",
    "امروز کجا مهربانِ خودت بودی؟",
    "یک لطف کوچکِ کسی که دیدی چه بود؟",
    "امروز چه چیزی را از خودت یاد گرفتی؟")

/**
 * هاب «ذهن‌آگاهی» — حضور ذهن، خودهیپنوز سالم، افکار، آگاهی اجتماعی، روزنوشت، یادگیری.
 */
@Composable
fun AwarenessHubScreen(nav: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_awareness") }
    val dayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val question = remember { SelfQuestions[dayIndex % SelfQuestions.size] }
    val dateKey = remember { todayKey() }
    var answer by remember { mutableStateOf(store.getString("self_answer_$dateKey", "")) }

    PracticeHubScreen(
        nav = nav,
        rootIds = WellnessMenu.mindfulnessIds,
        title = "ذهن‌آگاهی 🪷",
        subtitle = "حضور، فکر سالم، یادگیری آرام",
        headerSlot = "hub.awareness.header",
        accKey = "acc_mindfulness",
        onBack = onBack,
        extraTop = {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سوال امروز", style = MaterialTheme.typography.titleMedium)
                    Text(question, style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { v -> answer = v; store.putString("self_answer_$dateKey", v) },
                        label = { Text("جواب کوتاه تو…") },
                        modifier = Modifier.fillMaxWidth().height(110.dp))
                    Text("فقط برای خودت است؛ همین‌جا می‌ماند.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
