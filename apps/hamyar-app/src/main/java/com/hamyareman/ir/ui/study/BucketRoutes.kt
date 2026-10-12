package com.hamyareman.ir.ui.study

import com.hamyareman.ir.ui.content.ContentCatalog
import java.security.MessageDigest

/** Stable independent destinations. A missing object is resolved by the cache, never by menu flags. */
object BucketRoutes {
    fun key(id: String): String = ContentCatalog.keyFor(id) ?: run {
        val digest = MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        "Bucket/Html-files/app/routes/$digest.html"
    }
    fun lesson(packId: String, kind: String): String {
        ContentCatalog.keyFor(packId + "_" + kind.uppercase() + ".html")?.let { return it }
        if (packId.startsWith("C905_")) {
            val spec = MathHtmlAssets.of(packId)
            val asset = when (kind) {
                "teach" -> MathHtmlAssets.teachAsset(packId)
                "book" -> spec?.bookAsset
                "flash" -> spec?.flashAsset
                "exam" -> spec?.examAsset
                else -> null
            }
            if (asset != null) return "Bucket/Pdf-files/G09/g9-math/exam/" + asset.substringAfterLast('/')
        }
        return "Bucket/Html-files/app/lessons/$packId/$kind.html"
    }
}
