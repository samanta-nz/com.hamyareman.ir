package com.hamyareman.ir.platform.feature.playback

/**
 * مجوز مستقلِ پخش کتاب آزاد در پس‌زمینه.
 *
 * صوت تدریس همچنان تابع TeachGate است؛ این مجوز فقط برای کتاب‌های مطالعهٔ آزاد
 * است که کاربر صراحتاً «پخش در پس‌زمینه» را روشن کرده است.
 */
object BackgroundPlaybackGate {
    @Volatile
    var enabled: Boolean = false
}
