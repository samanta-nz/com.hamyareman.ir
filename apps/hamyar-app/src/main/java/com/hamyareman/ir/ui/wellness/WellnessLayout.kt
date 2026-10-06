package com.hamyareman.ir.ui.wellness

import com.hamyareman.ir.ui.content.ContentCatalog
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.ui.profile.StudentProfileState

/**
 * لایهٔ «ساختار نهایی» روی [WellnessMenu]: گروه‌هایی که محتوای HTML مستقل روی ParsPack دارند
 * (Bucket/Html-files/app/...) از اینجا ساخته می‌شوند و بقیه همان گروه‌های قبلی می‌مانند.
 *
 * قاعده‌ها:
 * - کاشی‌های HTML از ContentCatalog (assets/content/app-content.tsv) می‌آیند؛ هر مورد با
 *   Screen.ContentHtml باز می‌شود، نه با صفحهٔ واسط.
 * - نسخهٔ چرخه‌ای یوگا/تنفس/کشش/تغذیه فایل مستقل خودش را دارد و هرگز به یوگای اصلی (yga-*) وصل نیست.
 * - قصه‌ها: ۳ بازهٔ پایه × ۲ جنسیت × ۱۰ داستان؛ فقط ۱۰ قصهٔ بازهٔ پایهٔ همین APK و جنسیت کاربر دیده می‌شود.
 */
object WellnessLayout {

    fun group(id: String): PracticeGroup? = override(id) ?: WellnessMenu.group(id)

    fun groupsOf(ids: List<String>): List<PracticeGroup> = ids.mapNotNull { group(it) }

    /** گروه‌هایی که حالا محتوای واقعی دارند؛ قاب عنوان/فلش خودشان نمایش داده می‌شود. */
    private val liveGroups = setOf(
        "cl-story", "cl-pmr", "cl-journey", "cl-sounds",
        "sd-study", "sd-stress", "sd-nature", "sd-noise", "sd-freq", "sd-sleep",
        "hl-sleep-prep", "hl-nutrition",
        "pd-breath", "pd-yoga", "pd-stretch", "pd-food",
        "mf-hypnosis", "mf-hyp-study", "mf-hyp-exam", "mf-hyp-thoughts", "mf-hyp-social",
    )

    fun hideInternalChromeForGroup(id: String): Boolean =
        id !in liveGroups && WellnessMenu.hideInternalChromeForGroup(id)

    /** کاشی‌هایی که route HTML دارند هرگز «به‌زودی» نیستند. */
    fun isPlaceholderItem(itemId: String): Boolean =
        ContentCatalog.item(itemId) == null && WellnessMenu.isPlaceholderItem(itemId)

    private fun htmlItem(emoji: String, contentId: String, title: String? = null): PracticeItem? {
        val c = ContentCatalog.item(contentId) ?: return null
        return PracticeItem(
            id = contentId,
            emoji = emoji,
            title = title ?: c.title,
            subtitle = c.sub,
            route = Screen.ContentHtml.of(contentId),
        )
    }

    /** آیتم‌های گروه پایه را نگه می‌دارد و فقط فهرست کاشی‌ها را با HTMLهای جدید جایگزین می‌کند. */
    private fun withItems(baseId: String, items: List<PracticeItem?>): PracticeGroup? {
        val base = WellnessMenu.group(baseId) ?: return null
        val real = items.filterNotNull()
        return if (real.isEmpty()) base else base.copy(items = real, childGroupIds = emptyList())
    }

    private fun custom(
        id: String,
        emoji: String,
        title: String,
        subtitle: String,
        items: List<PracticeItem?> = emptyList(),
        children: List<String> = emptyList(),
    ) = PracticeGroup(id, emoji, title, subtitle, items.filterNotNull(), children)

    private fun legacyHtml(emoji: String, contentId: String): PracticeItem? =
        ContentCatalog.item(contentId)?.let {
            PracticeItem(
                id = it.id,
                emoji = emoji,
                title = it.title,
                subtitle = "تمرین تنفسی تعاملی",
                route = Screen.ContentHtml.of(it.id),
            )
        }

    private fun override(id: String): PracticeGroup? = when (id) {
        // ---- قصه‌ها: مقطع + جنسیت ----
        "cl-story" -> {
            val stories = ContentCatalog.storiesFor(StudentProfileState.gender)
            if (stories.isEmpty()) {
                null
            } else {
                custom(
                    "cl-story", "📖", "قصه‌ی شب", "داستان‌های آرامش‌بخش برای خواب بهتر",
                    items = stories.map {
                        PracticeItem(
                            id = it.id,
                            emoji = "📖",
                            title = it.title,
                            subtitle = it.sub,
                            route = Screen.ContentHtml.of(it.id),
                        )
                    },
                )
            }
        }

        // ---- سلامتی: خواب / تغذیه ----
        "hl-sleep-prep" -> withItems(
            id,
            listOf(htmlItem("🧘", "hs-prep-01"), htmlItem("🍵", "hs-prep-02")),
        )
        "hl-nutrition" -> WellnessMenu.group(id)?.let { base ->
            val keep = base.items.filter { it.id == "hl-water" }
            val html = htmlItem("🧠", "hn-focus-01")
            if (html == null) base else base.copy(items = keep + html)
        }

        // ---- سلامتی: چرخه ماهانه (نسخه‌های مستقل از Yoga اصلی) ----
        "pd-breath" -> withItems(
            id,
            listOf(
                legacyHtml("🌸", "bre-01"),
                legacyHtml("🫧", "bre-06"),
                htmlItem("8️⃣", "hc-breath-03"),
            ),
        )
        "pd-yoga" -> withItems(
            id,
            listOf(
                htmlItem("🐱", "hc-yoga-01"),
                htmlItem("🙇", "hc-yoga-02"),
                htmlItem("🦋", "hc-yoga-03"),
                htmlItem("🌀", "hc-yoga-04"),
                htmlItem("🦵", "hc-yoga-05"),
            ),
        )
        "pd-stretch" -> withItems(id, listOf(htmlItem("🦴", "hc-stretch-01"), htmlItem("🌡️", "hc-stretch-02")))
        "pd-food" -> withItems(
            id,
            listOf(
                htmlItem("🩸", "hc-food-01"),
                htmlItem("🌱", "hc-food-02"),
                htmlItem("☀️", "hc-food-03"),
                htmlItem("🌙", "hc-food-04"),
            ),
        )

        // ---- کسب آرامش ----
        "cl-pmr" -> withItems(id, listOf(htmlItem("🧘", "cp-pmr-01"), htmlItem("🙌", "cp-pmr-02")))
        "cl-journey" -> withItems(
            id,
            listOf(
                htmlItem("🌲", "cj-01"), htmlItem("🌊", "cj-02"), htmlItem("🏔️", "cj-03"),
                htmlItem("🌸", "cj-04"), htmlItem("🛶", "cj-05"), htmlItem("🌧️", "cj-06"),
                htmlItem("🌿", "cj-07"), htmlItem("🛖", "cj-08"), htmlItem("🌳", "cj-09"),
                htmlItem("👁️", "cj-10"), htmlItem("💧", "cj-11"), htmlItem("🌌", "cj-12"),
            ),
        )
        "cl-sounds" -> custom(
            "cl-sounds", "🎧", "صداهای آرامش‌بخش",
            "صدای دلخواهت را انتخاب کن و فضای آرام خودت را بساز.",
            children = listOf("sd-study", "sd-stress", "sd-nature", "sd-noise", "sd-freq", "sd-sleep"),
        )
        "sd-study" -> custom(
            "sd-study", "📚", "موسیقی مطالعه", "بدون کلام، برای تمرکز",
            items = listOf(htmlItem("🎼", "cs-study-01"), htmlItem("🔇", "cs-study-02"), htmlItem("🧠", "cs-study-03")),
        )
        "sd-stress" -> custom(
            "sd-stress", "🌿", "کاهش استرس", "آرام، طبیعت و هم‌نفس",
            items = listOf(htmlItem("🎵", "cs-stress-01"), htmlItem("🍃", "cs-stress-02"), htmlItem("🌬️", "cs-stress-03")),
        )
        "sd-nature" -> custom(
            "sd-nature", "🌧️", "صداهای طبیعت", "باران، جنگل، رودخانه، دریا",
            items = listOf(
                htmlItem("🌧️", "cs-nature-01"), htmlItem("🌲", "cs-nature-02"),
                htmlItem("🏞️", "cs-nature-03"), htmlItem("🌊", "cs-nature-04"),
            ),
        )
        "sd-noise" -> custom(
            "sd-noise", "🔊", "نویز", "سفید، صورتی، قهوه‌ای",
            items = listOf(htmlItem("⚪", "cs-noise-01"), htmlItem("🩷", "cs-noise-02"), htmlItem("🟤", "cs-noise-03")),
        )
        "sd-freq" -> custom(
            "sd-freq", "📡", "فرکانس‌ها و تون‌ها", "با بخش «آنچه شواهد نشان می‌دهد»",
            items = listOf(
                htmlItem("🎻", "cs-freq-01"), htmlItem("🎹", "cs-freq-02"), htmlItem("🌀", "cs-freq-03"),
                htmlItem("〰️", "cs-freq-04"), htmlItem("📊", "cs-freq-05"),
            ),
        )
        "sd-sleep" -> custom(
            "sd-sleep", "🌙", "محتوای صوتی خواب", "برای شب و آرام‌سازی",
            items = listOf(
                PracticeItem(
                    "soundscape-full", "🎧", "نجواهای آرام‌بخش طبیعت",
                    "موسیقی کامل از سرور، با تایمر خواب.",
                    route = Screen.BackgroundMusic.route,
                ),
                PracticeItem(
                    "hl-sleep-listen", "🛌", "بشنو و بخواب",
                    "موسیقی و صدای طبیعت برای آرامش ذهن",
                    route = Screen.SleepNight.route,
                ),
            ),
        )

        // ---- ذهن‌آگاهی: خودهیپنوز ----
        "mf-hyp-study" -> withItems(
            id,
            listOf(htmlItem("🎯", "mh-study-01"), htmlItem("✅", "mh-study-02"), htmlItem("🚪", "mh-study-03")),
        )
        "mf-hyp-exam" -> withItems(
            id,
            listOf(htmlItem("🏫", "mh-exam-01"), htmlItem("🧠", "mh-exam-02"), htmlItem("🌊", "mh-exam-03")),
        )
        "mf-hyp-thoughts" -> withItems(
            id,
            listOf(htmlItem("🔄", "mh-thoughts-01"), htmlItem("🌟", "mh-thoughts-02"), htmlItem("🕊️", "mh-thoughts-03")),
        )
        "mf-hyp-social" -> withItems(
            id,
            listOf(htmlItem("🗣️", "mh-social-01"), htmlItem("👁️", "mh-social-02"), htmlItem("🔥", "mh-social-03")),
        )
        else -> null
    }
}
