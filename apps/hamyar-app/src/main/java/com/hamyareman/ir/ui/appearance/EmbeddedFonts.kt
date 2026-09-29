package com.hamyareman.ir.ui.appearance

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hamyareman.ir.R

/** فونت‌های امبدشده در APK — هر خانواده سه وزن نازک / معمولی / ضخیم. */
object EmbeddedFonts {

    const val W_THIN = "thin"
    const val W_LIGHT = "light"
    const val W_REGULAR = "regular"
    const val W_BOLD = "bold"

    data class Face(
        val key: String,
        val label: String,
        val group: String,
        val resId: Int,
        val thinRes: Int = resId,
        val lightRes: Int = thinRes,
        val regularRes: Int = resId,
        val boldRes: Int = resId,
        val defaultWeight: String = W_REGULAR,
    ) {
        val hasRealWeights: Boolean get() =
            thinRes != regularRes || lightRes != regularRes || boldRes != regularRes
    }

    val catalog: List<Face> = listOf(
        Face("aviny", "آوینی", "ریپو همیار", R.font.aviny),
        Face("estedad_bold", "استعداد", "ریپو همیار", R.font.estedad_bold, defaultWeight = W_BOLD),
        Face("titr", "تیتر", "ریپو همیار", R.font.titr),
        Face("parastoo_bold", "پرستو", "ریپو همیار", R.font.parastoo_bold, defaultWeight = W_BOLD),
        Face("shekari", "شکاری", "ریپو همیار", R.font.shekari),
        Face("badkhat_bold", "بدخط", "ریپو همیار", R.font.badkhat_bold, defaultWeight = W_BOLD),
        Face(
            "vazirmatn",
            "وزیرمتن",
            "وزیرمتن",
            R.font.vazirmatn_regular,
            thinRes = R.font.vazirmatn_thin,
            lightRes = R.font.vazirmatn_light,
            regularRes = R.font.vazirmatn_regular,
            boldRes = R.font.vazirmatn_bold,
        ),
        Face("lalezar", "لاله‌زار", "آزاد فارسی", R.font.lalezar),
        Face("sahel", "ساحل", "آزاد فارسی", R.font.sahel),
        Face("samim", "صمیم", "آزاد فارسی", R.font.samim),
        Face("shabnam", "شبنم", "آزاد فارسی", R.font.shabnam),
        Face("tanha", "تنها", "آزاد فارسی", R.font.tanha),
        Face("gandom", "گندم", "آزاد فارسی", R.font.gandom),
        Face("markazi", "مرکزی", "آزاد فارسی", R.font.markazi),
        Face("amiri", "امیری", "آزاد عربی", R.font.amiri),
        Face("noto_naskh", "نوتو نسخ", "آزاد عربی", R.font.noto_naskh),
    )

    private val aliases = mapOf(
        "vazirmatn_thin" to ("vazirmatn" to W_THIN),
        "vazirmatn_extralight" to ("vazirmatn" to W_THIN),
        "vazirmatn_light" to ("vazirmatn" to W_LIGHT),
        "vazirmatn_regular" to ("vazirmatn" to W_REGULAR),
        "vazirmatn_medium" to ("vazirmatn" to W_REGULAR),
        "vazirmatn_semibold" to ("vazirmatn" to W_BOLD),
        "vazirmatn_bold" to ("vazirmatn" to W_BOLD),
        "vazirmatn_extrabold" to ("vazirmatn" to W_BOLD),
        "vazirmatn_black" to ("vazirmatn" to W_BOLD),
    )

    private val byKey: Map<String, Face> = catalog.associateBy { it.key }

    private val cache = mutableMapOf<String, FontFamily>()

    fun normalizeWeight(w: String?): String = when (w?.lowercase()?.trim()) {
        "thin", "extralight", "نازک" -> W_THIN
        "light", "سبک" -> W_LIGHT
        "bold", "semibold", "extrabold", "black", "ضخیم" -> W_BOLD
        else -> W_REGULAR
    }

    fun fontWeight(w: String?): FontWeight = when (normalizeWeight(w)) {
        W_THIN -> FontWeight.Thin
        W_LIGHT -> FontWeight.Light
        W_BOLD -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    fun weightLabel(w: String?): String = when (normalizeWeight(w)) {
        W_THIN -> "نازک"
        W_LIGHT -> "سبک"
        W_BOLD -> "ضخیم"
        else -> "معمولی"
    }

    fun canonicalKey(key: String): String = aliases[key]?.first ?: key

    fun weightFromKey(key: String, fallback: String = W_REGULAR): String =
        aliases[key]?.second ?: run {
            val face = byKey[key]
            when {
                face != null -> face.defaultWeight
                key.contains("thin") || key.contains("light") -> W_THIN
                key.contains("bold") || key.contains("black") -> W_BOLD
                else -> fallback
            }
        }

    fun defaultWeightOf(key: String): String = byKey[canonicalKey(key)]?.defaultWeight ?: W_REGULAR

    fun face(key: String): Face = byKey[canonicalKey(key)] ?: byKey.getValue("badkhat_bold")

    fun labelOf(key: String): String = face(key).label

    fun isKnown(key: String): Boolean = byKey.containsKey(canonicalKey(key)) || key in aliases

    @Synchronized
    fun family(key: String, weight: String = W_REGULAR): FontFamily {
        val canon = canonicalKey(key)
        val w = normalizeWeight(weight)
        val ck = "$canon|$w"
        cache[ck]?.let { return it }
        val fam = familyFresh(canon, w)
        cache[ck] = fam
        return fam
    }

    fun familyFresh(key: String, weight: String = W_REGULAR): FontFamily {
        val face = face(key)
        return runCatching {
            if (face.hasRealWeights) {
                FontFamily(
                    Font(face.thinRes, FontWeight.Thin),
                    Font(face.lightRes, FontWeight.Light),
                    Font(face.regularRes, FontWeight.Normal),
                    Font(face.boldRes, FontWeight.Bold),
                )
            } else {
                FontFamily(Font(face.resId, FontWeight.Normal))
            }
        }.getOrDefault(FontFamily.Default)
    }
}
