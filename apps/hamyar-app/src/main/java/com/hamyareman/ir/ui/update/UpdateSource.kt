package com.hamyareman.ir.ui.update

import com.hamyareman.ir.ui.study.ServerPrefs

/** انتخاب APK آپدیت با دقیقاً همان قرارداد انتخاب سرورِ محتوای اپ. */
object UpdateSource {
    fun candidates(info: UpdateInfo): List<String> {
        val external = info.externalUrl.ifBlank { info.url }.takeIf { it.isNotBlank() }
        val internal = info.internalUrl.takeIf { it.isNotBlank() }
        // از ۲٫۳ فایل نصبی همیشه اول از سرور داخلی (پارس‌پک، دیتاسنتر تهران)
        // گرفته می‌شود؛ سرور بیرونی فقط پشتیبان است. حالت دستی EXTERNAL همچنان
        // محترم است تا کسی که عمداً بیرونی را انتخاب کرده غافلگیر نشود.
        return when (ServerPrefs.mode) {
            ServerPrefs.Mode.EXTERNAL -> listOfNotNull(external, internal)
            else -> listOfNotNull(internal, external)
        }.distinct()
    }

    fun primary(info: UpdateInfo): String = candidates(info).firstOrNull().orEmpty()
}
