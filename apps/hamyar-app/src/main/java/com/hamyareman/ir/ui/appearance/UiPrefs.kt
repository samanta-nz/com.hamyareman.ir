package com.hamyareman.ir.ui.appearance

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.designsystem.BrandTheme
import com.hamyareman.ir.ui.AppTypography
import org.json.JSONArray
import org.json.JSONObject

/** دسترسی سراسری به وضعیت ظاهر از داخل کامپوزابل‌ها. */
val LocalUiPrefs = staticCompositionLocalOf<UiPrefs> { error("UiPrefs missing") }

class UiPrefs(context: Context) {
    private val store = LocalStore(context, "hamyar_appearance")

    var theme by mutableStateOf(
        runCatching { BrandTheme.valueOf(store.getString(KEY_THEME, BrandTheme.Stitch.name)) }
            .getOrDefault(BrandTheme.Stitch)
    )
        private set

    /** system / light / dark */
    var darkMode by mutableStateOf(store.getString(KEY_DARK, "system"))
        private set

    /** کلید فونت سراسری قدیمی؛ خالی = از تم پنج‌نقشه. */
    var fontKey by mutableStateOf(store.getString(KEY_FONT, ""))
        private set

    /**
     * لغزنده‌ی سایز متن سراسری: پایه ۱۳sp، از −۶ تا +۶، صفر = استاندارد.
     * روی کل اپ اعمال می‌شود (Density.fontScale).
     */
    var textSizeOffset by mutableIntStateOf(
        store.getString(KEY_SIZE, "0").toIntOrNull()?.coerceIn(-6, 6) ?: 0
    )
        private set

    var fontTheme by mutableStateOf(loadCurrentTheme())
        private set

    var fontSlots by mutableStateOf(loadSlots())
        private set

    var slotChoices by mutableStateOf(loadSlotChoices())
        private set

    private var themeUserSet: Boolean
        get() = store.getString(KEY_THEME_USER, "0") == "1"
        set(v) { store.putString(KEY_THEME_USER, if (v) "1" else "0") }

    val darkTheme: Boolean
        get() = when (darkMode) {
            "light" -> false
            "dark" -> true
            else -> android.content.res.Resources.getSystem().configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

    init {
        // تایپ قفل‌شده در AppTypography؛ فلوتر و بکاپ فونت اعمال نمی‌شود.
    }

    fun updateTheme(value: BrandTheme) {
        theme = value
        store.putString(KEY_THEME, value.name)
        themeUserSet = true
    }

    /** پیش‌فرض تم بر اساس جنسیت — فقط اگر کاربر هنوز تم را دستی عوض نکرده. */
    fun applyDefaultForGender(genderId: String) {
        if (themeUserSet) return
        theme = if (genderId == "girl") BrandTheme.DollStage else BrandTheme.Stitch
        store.putString(KEY_THEME, theme.name)
    }

    fun updateDarkMode(value: String) {
        darkMode = value
        store.putString(KEY_DARK, value)
    }

    fun updateFontKey(value: String) {
        fontKey = value
        store.putString(KEY_FONT, value)
    }

    fun updateTextSizeOffset(value: Int) {
        textSizeOffset = value.coerceIn(-6, 6)
        store.putString(KEY_SIZE, textSizeOffset.toString())
    }

    fun updateFontTheme(value: FontTheme) {
        val facesChanged =
            value.greetingFont != fontTheme.greetingFont ||
                value.clockFont != fontTheme.clockFont ||
                value.headingFont != fontTheme.headingFont ||
                value.tileFont != fontTheme.tileFont ||
                value.bodyFont != fontTheme.bodyFont ||
                value.greetingSize != fontTheme.greetingSize ||
                value.clockSize != fontTheme.clockSize ||
                value.headingSize != fontTheme.headingSize ||
                value.tileSize != fontTheme.tileSize ||
                value.bodySize != fontTheme.bodySize
        fontTheme = value
        store.putString(KEY_FONT_THEME, value.toJson().toString())
        if (facesChanged) AppTypography.apply(value)
    }

    fun updateThemeName(name: String) {
        updateFontTheme(fontTheme.withName(name))
    }

    fun updateRoleFont(role: String, font: String) {
        updateFontTheme(fontTheme.withRole(role, font = font))
    }

    fun updateRoleSize(role: String, size: Int) {
        updateFontTheme(fontTheme.withRole(role, size = size))
    }

    fun updateSlot(id: String, font: String? = null, size: Int? = null, weight: String? = null) {
        val live = TypeSlots.resolved(id)
        val pad = id == "d7.pad"
        val next = SlotChoice(
            font = font?.takeIf { EmbeddedFonts.isKnown(it) }?.let { EmbeddedFonts.canonicalKey(it) } ?: live.font,
            size = if (pad) (size ?: live.size).coerceIn(0, 32) else (size ?: live.size).coerceIn(8, 40),
            weight = weight?.let { EmbeddedFonts.normalizeWeight(it) } ?: live.weight,
            absolute = true,
        )
        val map = slotChoices.toMutableMap()
        map[id] = next
        slotChoices = map
        store.putString(KEY_SLOT_MAP, slotMapJson(map))
        TypeSlots.load(map)
    }

    fun importSlotMap(raw: JSONObject) {
        val map = parseSlotMap(raw)
        if (map.isEmpty()) return
        slotChoices = map
        store.putString(KEY_SLOT_MAP, slotMapJson(map))
        TypeSlots.load(map)
    }

    fun slotMapObject(): JSONObject {
        val o = JSONObject()
        slotChoices.forEach { (id, c) ->
            o.put(id, JSONObject().put("font", c.font).put("sp", c.size).put("size", c.size).put("weight", c.weight))
        }
        return o
    }

    fun saveSlot(index: Int) {
        if (index !in 0..4) return
        val named = fontTheme.withName(fontTheme.name.ifBlank { "تم ${index + 1}" })
        val next = fontSlots.toMutableList()
        next[index] = named
        persistSlots(next)
        if (named != fontTheme) updateFontTheme(named)
    }

    fun loadSlot(index: Int) {
        val t = fontSlots.getOrNull(index) ?: return
        updateFontTheme(t)
    }

    fun clearSlot(index: Int) {
        if (index !in 0..4) return
        val next = fontSlots.toMutableList()
        next[index] = null
        persistSlots(next)
    }

    private fun persistSlots(next: List<FontTheme?>) {
        fontSlots = next
        val arr = JSONArray()
        next.forEach { slot ->
            if (slot == null) arr.put(JSONObject.NULL) else arr.put(slot.toJson())
        }
        store.putString(KEY_SLOTS, arr.toString())
    }

    private fun loadCurrentTheme(): FontTheme {
        val raw = store.getString(KEY_FONT_THEME, "")
        FontTheme.parse(raw)?.let { return it }
        val old = store.getString(KEY_FONT, "")
        val body = when (old) {
            "vazirmatn" -> "vazirmatn_regular"
            else -> if (EmbeddedFonts.isKnown(old)) old else "badkhat_bold"
        }
        return FontTheme(bodyFont = body)
    }

    private fun loadSlotChoices(): Map<String, SlotChoice> {
        val raw = store.getString(KEY_SLOT_MAP, "")
        if (raw.isBlank()) return emptyMap()
        return runCatching { parseSlotMap(JSONObject(raw)) }.getOrDefault(emptyMap())
    }

    private fun loadSlots(): List<FontTheme?> {
        val raw = store.getString(KEY_SLOTS, "")
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: JSONArray()
        return (0 until 5).map { i ->
            if (i < arr.length() && !arr.isNull(i)) {
                FontTheme.parse(arr.optJSONObject(i)?.toString().orEmpty())
            } else null
        }
    }

    companion object {
        private const val KEY_THEME = "appearance_theme"
        private const val KEY_THEME_USER = "appearance_theme_user"
        private const val KEY_DARK = "appearance_dark"
        private const val KEY_FONT = "appearance_font"
        private const val KEY_SIZE = "appearance_text_offset"
        private const val KEY_FONT_THEME = "appearance_font_theme"
        private const val KEY_SLOTS = "appearance_font_slots"
        private const val KEY_SLOT_MAP = "appearance_slot_map"
    }
}

private fun slotMapJson(map: Map<String, SlotChoice>): String {
    val o = JSONObject()
    map.forEach { (id, c) ->
        o.put(id, JSONObject().put("font", c.font).put("sp", c.size).put("size", c.size).put("weight", c.weight))
    }
    return o.toString()
}

private fun parseSlotMap(raw: JSONObject): Map<String, SlotChoice> {
    val out = mutableMapOf<String, SlotChoice>()
    val keys = raw.keys()
    while (keys.hasNext()) {
        val id = keys.next()
        val o = raw.optJSONObject(id) ?: continue
        val font = o.optString("font")
        if (!EmbeddedFonts.isKnown(font)) continue
        val hasSp = o.has("sp")
        val hasWeight = o.has("weight") && o.optString("weight").isNotBlank()
        val absolute = hasSp || hasWeight || id == "d7.pad"
        val size = if (hasSp) o.optInt("sp") else o.optInt("size", 0)
        val weight = EmbeddedFonts.normalizeWeight(
            o.optString("weight").ifBlank { EmbeddedFonts.weightFromKey(font) },
        )
        out[id] = SlotChoice(EmbeddedFonts.canonicalKey(font), size, weight, absolute)
    }
    return out
}
