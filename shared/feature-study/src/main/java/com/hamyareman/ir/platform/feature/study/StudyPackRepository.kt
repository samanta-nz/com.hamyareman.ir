package com.hamyareman.ir.platform.feature.study

import android.content.Context

/** لودر «پک مطالعه» از assets اپ — آفلاین کامل، بدون سرور. */
class StudyPackRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dir = "studypacks"

    private val cache = mutableMapOf<String, StudyPack?>()

    fun ids(): List<String> = runCatching {
        appContext.assets.list(dir)?.toList().orEmpty()
            .filter { it.endsWith(".json") }.map { it.removeSuffix(".json") }.sorted()
    }.getOrDefault(emptyList())

    fun pack(packId: String): StudyPack? {
        // اول ماژول‌های کدی کتاب‌ها (منبع اصلی)، بعد assets (سازگاری قدیمی)
        BookModuleRegistry.pack(packId)?.let { return it }
        if (cache.containsKey(packId)) return cache[packId]
        val raw = runCatching {
            appContext.assets.open("$dir/$packId.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        if (raw == null) {
            cache[packId] = null
            return null
        }
        val parsed = StudyPack.fromJson(raw)
        cache[packId] = parsed
        return parsed
    }
}
