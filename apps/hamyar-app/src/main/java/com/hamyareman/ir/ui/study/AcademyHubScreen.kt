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
        lesson("notes", "یادداشت‌برداری", "sk-cornell", "amz-04"),
        lesson("summary", "خلاصه‌نویسی", "sk-summary", "amz-05"),
    )),
    AcademySection("critical", "🎙️", "بیان و تفکر نقاد", listOf(
        lesson("debate", "اصول مناظره", "sk-debate", "amz-06"),
        lesson("fallacies", "مغالطه‌های منطقی", "sk-fallacy", "amz-07"),
        lesson("speaking", "فن بیان", "sk-present", "amz-08"),
        lesson("speech-anxiety", "اضطراب سخنرانی", "sk-voice", "amz-09"),
        lesson("email", "ایمیل رسمی", "sk-email", "amz-10"),
        lesson("listening", "گوش‌دادن فعال", "sk-listen", "amz-11"),
    )),
    AcademySection("mind", "🧠", "ذهن، حافظه و منطق", listOf(
        lesson("mental-math", "محاسبات ذهنی", "sk-math", "amz-12"),
        lesson("memory-palace", "قصر ذهنی و تقویت حافظه", "sk-palace", "amz-13"),
        lesson("association", "زنجیره تداعی", "sk-chain", "amz-14"),
        lesson("problem-solving", "حل مسئله ۵ مرحله‌ای", "sk-lateral", "amz-15"),
    )),
    AcademySection("psychology", "🌱", "روان‌شناسی فردی", listOf(
        lesson("rumination", "توقف نشخوار فکری", "hyp-reframe", "amz-16"),
        lesson("self-awareness", "خودآگاهی و گفت‌وگوی درونی", "sk-feel", "amz-17"),
        lesson("resilience", "انعطاف‌پذیری و انگیزه", "sk-resilience", "amz-18"),
    )),
    AcademySection("time", "⏳", "مدیریت زمان", listOf(
        lesson("pomodoro", "پومودورو", "ln-pomo"),
        lesson("daily-plan", "برنامه‌ریزی روزانه", "sk-plan", "amz-19"),
        lesson("habit", "ساخت عادت", "sk-habit", "amz-20"),
        lesson("study-space", "مدیریت فضای مطالعه", "sk-desk", "amz-21"),
    )),
    AcademySection("digital", "💻", "مهارت دیجیتال", listOf(
        lesson("windows", "ویندوز کاربردی", "sk-digital", "amz-22"),
        lesson("ai-literacy", "سواد هوش مصنوعی", "sk-ai-what", "amz-23"),
        lesson("search", "جست‌وجوی هوشمند", "sk-search", "amz-24"),
        lesson("online-safety", "ایمنی مجازی", "sk-privacy", "amz-25"),
        lesson("fake-news", "تشخیص اخبار جعلی", "sk-fake", "amz-26"),
    )),
    AcademySection("social", "🤝", "زندگی اجتماعی و روابط", listOf(
        lesson("teamwork", "کار تیمی", "sk-team", "amz-27"),
        lesson("conflict", "مدیریت اختلاف با دوستان", "sk-conflict", "amz-28"),
        lesson("saying-no", "هنر نه گفتن", "sk-no", "amz-29"),
    )),
    AcademySection("finance", "💳", "سواد مالی", listOf(
        lesson("budget", "مدیریت بودجه", "sk-budget", "amz-30"),
        lesson("need-want", "تشخیص نیاز در برابر خواسته", "sk-need", "amz-31"),
    )),
    AcademySection("creativity", "💡", "خلاقیت", listOf(
        lesson("brainstorm", "طوفان فکری", "sk-storm", "amz-32"),
        lesson("creative-thinking", "تفکر خلاق", "sk-create", "amz-33"),
    )),
    AcademySection("music", "🎼", "تئوری موسیقی", listOf(
        lesson("music-theory", "درس جامع تئوری موسیقی", "sk-music", "amz-34"),
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
                                val route = when {
                                    item.id == "pomodoro" -> Screen.PracticeItem.of("ln-pomo")
                                    item.publishedContentId != null -> Screen.ContentHtml.of(item.publishedContentId)
                                    else -> Screen.AcademySoon.route
                                }
                                nav.layerTo(route)
                            },
                        )
                    },
                )
            }
        }
    }
}
