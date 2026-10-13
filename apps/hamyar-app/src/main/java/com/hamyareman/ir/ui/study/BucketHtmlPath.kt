package com.hamyareman.ir.ui.study

/** Shared document naming contract for every bucket consumer. */
internal object BucketHtmlPath {
    private fun path(url: String) = url.substringBefore('#').substringBefore('?')
    fun encrypted(url: String) = path(url).endsWith(".html.hmk1", ignoreCase = true)
    fun isHtml(url: String) = encrypted(url) || path(url).endsWith(".html", ignoreCase = true)
    fun alternative(url: String): String? {
        val p = path(url)
        val rest = url.substring(p.length)
        return when {
            encrypted(url) -> p.dropLast(5) + rest
            isHtml(url) -> p + ".hmk1" + rest
            else -> null
        }
    }
    fun plainPath(path: String): String = if (encrypted(path)) alternative(path)!! else path
}
