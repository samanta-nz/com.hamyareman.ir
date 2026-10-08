package com.hamyareman.ir.ui.update

import java.net.URI

/** دانلود APK فقط از URL عمومی پارس‌پک؛ نشانی‌های خارجی یا تاریخی نادیده گرفته می‌شوند. */
object UpdateSource {
    private const val PARSPACK_HOST = "c539776.parspack.net"

    fun candidates(info: UpdateInfo): List<String> {
        // internalUrl مسیر اصلی است. url فقط اگر خودش URL پارس‌پک باشد، سازگاری قدیمی دارد.
        return listOf(info.internalUrl, info.url)
            .map { it.trim() }
            .filter { it.isNotBlank() && isParspackUrl(it) }
            .distinct()
    }

    fun primary(info: UpdateInfo): String = candidates(info).firstOrNull().orEmpty()

    private fun isParspackUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(PARSPACK_HOST, ignoreCase = true) &&
            !uri.path.isNullOrBlank()
    }.getOrDefault(false)
}
