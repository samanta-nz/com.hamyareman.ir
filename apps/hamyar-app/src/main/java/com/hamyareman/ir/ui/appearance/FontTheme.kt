package com.hamyareman.ir.ui.appearance

import com.hamyareman.ir.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

/** یک تنظیم پنج‌نقشهٔ فونت + سایز؛ قابل ذخیره در ۵ اسلات و بکاپ JSON. */
data class FontTheme(
    val name: String = "پیش‌فرض",
    val greetingFont: String = "aviny",
    val greetingSize: Int = 0,
    val clockFont: String = "estedad_bold",
    val clockSize: Int = 0,
    val headingFont: String = "titr",
    val headingSize: Int = 0,
    val tileFont: String = "parastoo_bold",
    val tileSize: Int = 0,
    val bodyFont: String = "badkhat_bold",
    val bodySize: Int = 0,
) {
    fun fontOf(role: String): String = when (role) {
        ROLE_GREETING -> greetingFont
        ROLE_CLOCK -> clockFont
        ROLE_HEADING -> headingFont
        ROLE_TILE -> tileFont
        else -> bodyFont
    }

    fun sizeOf(role: String): Int = when (role) {
        ROLE_GREETING -> greetingSize
        ROLE_CLOCK -> clockSize
        ROLE_HEADING -> headingSize
        ROLE_TILE -> tileSize
        else -> bodySize
    }

    fun withName(value: String) = copy(name = value.trim().ifBlank { "پیش‌فرض" }.take(40))

    fun withRole(role: String, font: String? = null, size: Int? = null): FontTheme {
        val f = font?.let { if (EmbeddedFonts.isKnown(it)) it else fontOf(role) }
        val s = size?.coerceIn(-20, 20)
        return when (role) {
            ROLE_GREETING -> copy(greetingFont = f ?: greetingFont, greetingSize = s ?: greetingSize)
            ROLE_CLOCK -> copy(clockFont = f ?: clockFont, clockSize = s ?: clockSize)
            ROLE_HEADING -> copy(headingFont = f ?: headingFont, headingSize = s ?: headingSize)
            ROLE_TILE -> copy(tileFont = f ?: tileFont, tileSize = s ?: tileSize)
            else -> copy(bodyFont = f ?: bodyFont, bodySize = s ?: bodySize)
        }
    }

    fun toJson(): JSONObject = JSONObject()
        .put("app", "hamyar")
        .put("package", "com.hamyareman.ir")
        .put("lab", "appearance")
        .put("versionName", BuildConfig.VERSION_NAME)
        .put("name", name)
        .put("roles", JSONArray().apply {
            ROLES.forEach { (id, title) ->
                put(
                    JSONObject()
                        .put("id", id)
                        .put("slot", title)
                        .put("font", fontOf(id))
                        .put("fontLabel", EmbeddedFonts.labelOf(fontOf(id)))
                        .put("size", sizeOf(id)),
                )
            }
        })

    companion object {
        const val ROLE_GREETING = "greeting"
        const val ROLE_CLOCK = "clock"
        const val ROLE_HEADING = "heading"
        const val ROLE_TILE = "tile"
        const val ROLE_BODY = "body"

        val ROLES = listOf(
            ROLE_GREETING to "خوش‌آمد",
            ROLE_CLOCK to "ساعت",
            ROLE_HEADING to "عنوان کارت",
            ROLE_TILE to "کاشی میانبر",
            ROLE_BODY to "متن عمومی",
        )

        val SAMPLES = mapOf(
            ROLE_GREETING to "صبح‌ت بخیر دوست من",
            ROLE_CLOCK to "۱۴:۳۰ بعد از ظهر",
            ROLE_HEADING to "عنوان کارت و بخش",
            ROLE_TILE to "میانبر",
            ROLE_BODY to "برچسب، بدنه، دکمه و جزوه",
        )

        fun fromJson(raw: JSONObject): FontTheme {
            val roles = raw.optJSONArray("roles")
            fun roleFont(id: String, fallbackKey: String, default: String): String {
                val fromArr = roleObj(roles, id)?.optString("font").orEmpty()
                val fromFlat = raw.optString(fallbackKey)
                val v = fromArr.ifBlank { fromFlat }.ifBlank { default }
                return if (EmbeddedFonts.isKnown(v)) v else default
            }
            fun roleSize(id: String, fallbackKey: String): Int {
                val o = roleObj(roles, id)
                val n = when {
                    o != null && o.has("size") -> o.optInt("size", 0)
                    raw.has(fallbackKey) -> raw.optInt(fallbackKey, 0)
                    else -> 0
                }
                return n.coerceIn(-20, 20)
            }
            return FontTheme(
                name = raw.optString("name").ifBlank { "پیش‌فرض" }.take(40),
                greetingFont = roleFont(ROLE_GREETING, "greetingFont", "aviny"),
                greetingSize = roleSize(ROLE_GREETING, "greetingSize"),
                clockFont = roleFont(ROLE_CLOCK, "clockFont", "estedad_bold"),
                clockSize = roleSize(ROLE_CLOCK, "clockSize"),
                headingFont = roleFont(ROLE_HEADING, "headingFont", "titr"),
                headingSize = roleSize(ROLE_HEADING, "headingSize"),
                tileFont = roleFont(ROLE_TILE, "tileFont", "parastoo_bold"),
                tileSize = roleSize(ROLE_TILE, "tileSize"),
                bodyFont = roleFont(ROLE_BODY, "bodyFont", "badkhat_bold"),
                bodySize = roleSize(ROLE_BODY, "bodySize"),
            )
        }

        fun parse(text: String): FontTheme? {
            val t = text.trim()
            if (t.isEmpty()) return null
            return runCatching { fromJson(JSONObject(t)) }.getOrNull()
        }

        private fun roleObj(arr: JSONArray?, id: String): JSONObject? {
            if (arr == null) return null
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val rid = o.optString("id").ifBlank { o.optString("kind") }
                if (rid == id) return o
            }
            return null
        }
    }
}
