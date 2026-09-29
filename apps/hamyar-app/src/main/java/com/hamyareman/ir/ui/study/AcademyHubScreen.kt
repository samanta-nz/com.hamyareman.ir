package com.hamyareman.ir.ui.study

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubCoverGrid
import com.hamyareman.ir.ui.hub.HubCoverTile
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.HubMenuGroup
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.navigation.Screen

private data class AcademyLesson(
    val id: String,
    val title: String,
    val coverId: String,
    val publishedContentId: String? = null,
)

private data class AcademySection(
    val id: String,
    val icon: String,
    val title: String,
    val lessons: List<AcademyLesson>,
)

private fun lesson(id: String, title: String, coverId: String, published: String? = null) =
    AcademyLesson(id, title, coverId, published)

/** فهرست نهایی آموزشگاه؛ فقط سه درس نخست محتوای تحویلی دارند. */
private val AcademySections = listOf(
    AcademySection("reading", "📚", "خواندن و نوشتن", listOf(
        lesson("speed", "سرعت خواندن", "sk-speed", "amz-01"),
        lesson("handwriting", "خوشنویسی", "sk-hand", "amz-02"),
        lesson("typing", "تایپ لمسی", "sk-type", "amz-03"),
        lesson("notes", "یادداشت‌برداری", "sk-cornell"),
        lesson("summary", "خلاصه‌نویسی", "sk-summary"),
    )),
    AcademySection("critical", "🎙️", "بیان و تفکر نقاد", listOf(
        lesson("debate", "اصول مناظره", "sk-debate"),
        lesson("fallacies", "مغالطه‌های منطقی", "sk-fallacy"),
        lesson("speaking", "فن بیان", "sk-present"),
        lesson("speech-anxiety", "اضطراب سخنرانی", "sk-voice"),
        lesson("email", "ایمیل رسمی", "sk-email"),
        lesson("listening", "گوش‌دادن فعال", "sk-listen"),
    )),
    AcademySection("mind", "🧠", "ذهن، حافظه و منطق", listOf(
        lesson("mental-math", "محاسبات ذهنی", "sk-math"),
        lesson("memory-palace", "قصر ذهنی و تقویت حافظه", "sk-palace"),
        lesson("association", "زنجیره تداعی", "sk-chain"),
        lesson("problem-solving", "حل مسئله ۵ مرحله‌ای", "sk-lateral"),
    )),
    AcademySection("psychology", "🌱", "روان‌شناسی فردی", listOf(
        lesson("rumination", "توقف نشخوار فکری", "hyp-reframe"),
        lesson("self-awareness", "خودآگاهی و گفت‌وگوی درونی", "sk-feel"),
        lesson("resilience", "انعطاف‌پذیری و انگیزه", "sk-resilience"),
    )),
    AcademySection("time", "⏳", "مدیریت زمان", listOf(
        lesson("pomodoro", "پومودورو", "ln-pomo"),
        lesson("daily-plan", "برنامه‌ریزی روزانه", "sk-plan"),
        lesson("habit", "ساخت عادت", "sk-habit"),
        lesson("study-space", "مدیریت فضای مطالعه", "sk-desk"),
    )),
    AcademySection("digital", "💻", "مهارت دیجیتال", listOf(
        lesson("windows", "ویندوز کاربردی", "sk-digital"),
        lesson("ai-literacy", "سواد هوش مصنوعی", "sk-ai-what"),
        lesson("search", "جست‌وجوی هوشمند", "sk-search"),
        lesson("online-safety", "ایمنی مجازی", "sk-privacy"),
        lesson("fake-news", "تشخیص اخبار جعلی", "sk-fake"),
    )),
    AcademySection("social", "🤝", "زندگی اجتماعی و روابط", listOf(
        lesson("teamwork", "کار تیمی", "sk-team"),
        lesson("conflict", "مدیریت اختلاف با دوستان", "sk-conflict"),
        lesson("saying-no", "هنر نه گفتن", "sk-no"),
    )),
    AcademySection("finance", "💳", "سواد مالی", listOf(
        lesson("budget", "مدیریت بودجه", "sk-budget"),
        lesson("need-want", "تشخیص نیاز در برابر خواسته", "sk-need"),
    )),
    AcademySection("creativity", "💡", "خلاقیت", listOf(
        lesson("brainstorm", "طوفان فکری", "sk-storm"),
        lesson("creative-thinking", "تفکر خلاق", "sk-create"),
    )),
    AcademySection("music", "🎼", "تئوری موسیقی", listOf(
        lesson("music-theory", "درس جامع تئوری موسیقی", "sk-music"),
    )),
)

/**
 * آموزشگاه به شکل آکاردئون تک‌انتخابی است. اولین اجرا همه بسته‌اند؛ پس از آن آخرین
 * وضعیت باز/بسته در گوشی می‌ماند. متن توضیحی داخل کاشی‌ها عمداً حذف شده است.
 */
@Composable
fun AcademyHubScreen(nav: NavController) {
    val context = LocalContext.current
    val prefs = remember { LocalStore(context, "hamyar_academy_ui") }
    var expandedId by remember {
        mutableStateOf(
            prefs.getString("expanded_section", "")
                .takeIf { saved -> AcademySections.any { it.id == saved } },
        )
    }

    HubBody {
        HubHeader(
            "آموزشگاه ✨",
            "مهارت‌های کاربردی در ده دسته",
            slotId = "hub.academy.header",
        )
        AcademySections.forEach { section ->
            val open = expandedId == section.id
            HubMenuGroup(
                title = "${section.icon}  ${section.title}",
                subtitle = "",
                open = open,
                onToggle = {
                    expandedId = if (open) null else section.id
                    prefs.putString("expanded_section", expandedId.orEmpty())
                },
                slotId = "hub.academy.group.${section.id}",
            ) {
                HubCoverGrid(
                    tiles = section.lessons.map { item ->
                        HubCoverTile(
                            id = item.coverId,
                            title = item.title,
                            subtitle = "",
                            onClick = {
                                val route = item.publishedContentId?.let(Screen.ContentHtml::of)
                                    ?: Screen.AcademySoon.route
                                nav.layerTo(route)
                            },
                        )
                    },
                )
            }
        }
    }
}
