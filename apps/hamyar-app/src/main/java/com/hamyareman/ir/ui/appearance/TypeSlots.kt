package com.hamyareman.ir.ui.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.hamyareman.ir.ui.AppTypography

object TypeSlots {
    var rev by mutableIntStateOf(0)
        private set
    var currentRoute by mutableStateOf("home")

    fun load(map: Map<String, SlotChoice>) {
        AppTypography.applyAll(map)
        rev++
    }

    fun choice(id: String): SlotChoice? {
        if (id == "d7.pad") {
            return SlotChoice("badkhat_bold", AppTypography.quoteSideDp, EmbeddedFonts.W_REGULAR, absolute = true)
        }
        val r = AppTypography.slot(id) ?: return null
        return SlotChoice(r.fontKey, r.sizeSp, r.weightKey, absolute = true)
    }

    fun resolved(id: String): SlotChoice = choice(id) ?: SlotChoice("badkhat_bold", 16, EmbeddedFonts.W_REGULAR)

    fun family(id: String): FontFamily {
        val _r = rev
        if (id == "d7.pad") return AppTypography.d7Quote.family
        if (id.startsWith("nav.")) return AppTypography.navBar.family
        return AppTypography.slot(id)?.family ?: AppTypography.pageBody.family
    }

    fun weight(id: String): FontWeight {
        val _r = rev
        if (id.startsWith("nav.")) return AppTypography.navBar.weight
        return AppTypography.slot(id)?.weight ?: AppTypography.pageBody.weight
    }

    fun size(id: String, baseSp: Int): TextUnit {
        val _r = rev
        if (id.startsWith("nav.")) return AppTypography.navBar.size
        val role = AppTypography.slot(id)
        return role?.size ?: AppTypography.bump(baseSp)
    }

    fun setRoute(route: String?) {
        currentRoute = FontCatalog.normalize(route)
        rev++
    }
}
