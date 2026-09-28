package com.hamyareman.ir.ui.appearance

import com.hamyareman.ir.ui.AppTypography

data class FontSlot(
    val id: String,
    val title: String,
    val group: String,
    val fallbackId: String,
    val defaultFont: String,
    val sample: String = "نمونه متن همیار من",
)

data class SlotChoice(
    val font: String,
    val size: Int = 0,
    val weight: String = EmbeddedFonts.W_REGULAR,
    val absolute: Boolean = true,
)

/** نمای نازک روی [AppTypography] — طبقه‌بندی لازم‌الاجرا همان‌جاست. */
object FontCatalog {
    const val ROLE_GREETING = "d1"
    const val ROLE_CLOCK = "d4"
    const val ROLE_HEADING = "e.title"
    const val ROLE_TILE = "b.title"
    const val ROLE_BODY = "c.body"
    const val ROLE_NAV = "nav.bar"

    val all: List<FontSlot>
        get() = AppTypography.slots.map { r ->
            FontSlot(r.spec.id, r.spec.title, r.spec.group, r.spec.id, r.spec.defaultFont, r.spec.sample)
        } + FontSlot("d7.pad", "D7 فاصله از دو طرف کارت سخنان", "D داشبورد", "d7.pad", "badkhat_bold", "حاشیه")

    fun slot(id: String): FontSlot? = all.firstOrNull { it.id == id }

    fun normalize(route: String?): String {
        if (route.isNullOrBlank()) return "home"
        var r = route.substringBefore("?")
        r = r.substringBefore("/{")
        val known = setOf(
            "study-book", "study-teach", "study-lesson", "study-lesson-pdf",
            "video-teach", "lesson", "exercise", "recipedetail", "tool",
        )
        val first = r.substringBefore("/")
        return if (first in known) first else r
    }

    fun forRoute(route: String?): List<FontSlot> {
        val roles = AppTypography.slotsFor(route)
        val list = roles.map { r ->
            FontSlot(r.spec.id, r.spec.title, r.spec.group, r.spec.id, r.spec.defaultFont, r.spec.sample)
        }.toMutableList()
        if (normalize(route) == "home") {
            list += FontSlot("d7.pad", "D7 فاصله از دو طرف کارت سخنان", "D داشبورد", "d7.pad", "badkhat_bold", "حاشیه")
        }
        return list
    }

    fun pageTitle(route: String?): String = when (normalize(route)) {
        "home" -> "داشبورد — A/B/C/E + D1…D12"
        else -> "A آکاردئون · B کارت · C صفحه · E عنوان"
    }
}
