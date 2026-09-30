package com.hamyareman.ir.ui.update

import com.hamyareman.ir.ui.study.ServerPrefs
import com.hamyareman.ir.ui.study.ServerResolver

/** انتخاب APK آپدیت با دقیقاً همان قرارداد انتخاب سرورِ محتوای اپ. */
object UpdateSource {
    fun candidates(info: UpdateInfo): List<String> {
        val external = info.externalUrl.ifBlank { info.url }.takeIf { it.isNotBlank() }
        val internal = info.internalUrl.takeIf { it.isNotBlank() }
        return when (ServerPrefs.mode) {
            ServerPrefs.Mode.EXTERNAL -> listOfNotNull(external)
            ServerPrefs.Mode.INTERNAL -> listOfNotNull(internal)
            ServerPrefs.Mode.FASTEST -> when (ServerResolver.preferredOrigin()) {
                ServerPrefs.Origin.INTERNAL -> listOfNotNull(internal, external)
                ServerPrefs.Origin.EXTERNAL -> listOfNotNull(external, internal)
            }
        }.distinct()
    }

    fun primary(info: UpdateInfo): String = candidates(info).firstOrNull().orEmpty()
}
