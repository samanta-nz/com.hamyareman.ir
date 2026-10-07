package com.hamyareman.ir.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hamyareman.ir.platform.core.common.LocalStore
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * پیش‌نویس خودکار دفترهای تایپی.
 *
 * [current] همان محتوای فعلیِ در حال تایپ (JSON) است و `null` یعنی چیزی برای نگه‌داشتن نیست
 * (پیش‌نویس پاک می‌شود). ذخیره‌سازی چند لحظه بعد از آخرین تغییر انجام می‌شود و همچنین
 * بلافاصله هنگام رفتن به پس‌زمینه، بستن صفحه یا خروج از اپ. تا وقتی [enabled] نشده
 * (مثلاً پیش‌نویس قبلی هنوز بازگردانده نشده) چیزی نوشته نمی‌شود تا پیش‌نویس قدیمی با
 * فرم خالی بازنویسی نشود.
 */
@Composable
internal fun DraftAutoSave(
    enabled: Boolean,
    current: String?,
    onSave: (String?) -> Unit,
) {
    val latest by rememberUpdatedState(current)
    val latestEnabled by rememberUpdatedState(enabled)
    val save by rememberUpdatedState(onSave)

    LaunchedEffect(current, enabled) {
        if (enabled) {
            delay(400)
            save(current)
        }
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (latestEnabled && (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP)) {
                save(latest)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (latestEnabled) save(latest)
        }
    }
}

/** پیش‌نویس رمزگذاری‌شده را می‌خواند؛ نبود یا خرابی = null. */
internal fun readDraft(store: LocalStore, key: String, decrypt: (String) -> String?): JSONObject? {
    val raw = store.getString(key)
    if (raw.isBlank()) return null
    return runCatching { JSONObject(decrypt(raw).orEmpty()) }.getOrNull()
}

/** پیش‌نویس را رمزگذاری‌شده می‌نویسد؛ [json] = null یعنی پاک‌کردن. */
internal fun writeDraft(store: LocalStore, key: String, json: String?, encrypt: (String) -> String) {
    if (json == null) store.remove(key) else store.putString(key, encrypt(json))
}
