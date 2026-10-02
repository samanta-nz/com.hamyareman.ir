package com.hamyareman.ir.ui.study

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.hamyareman.ir.ui.appearance.UiPrefs
import org.json.JSONObject

/**
 * قرارداد ظاهر بین اپ و هر سند HTML که در WebView باز می‌شود.
 *
 * HTML به تنظیمات SharedPreferences اندروید دسترسی ندارد. [installHamyarAppearanceBridge]
 * را پیش از `loadUrl` صدا می‌زنیم تا script ابتدایی HTML هم بتواند وضعیت را بخواند؛
 * سپس [publishHamyarAppearance] بعد از آماده‌شدن سند و هر بار که کاربر ظاهر را عوض
 * می‌کند، همان وضعیت را در DOM منتشر می‌کند. هیچ HTML متن‌ساده‌ای روی دیسک نوشته یا
 * در کش نگه‌داری نمی‌شود.
 *
 * - `data-hamyar-theme-preference`: `light`، `dark` یا `system` (انتخاب کاربر)
 * - `data-hamyar-theme`: `light` یا `dark` (نتیجهٔ نهایی همین لحظه)
 * - `window.HamyarAppearance`: همان داده برای JavaScript بعد از آماده‌شدن DOM
 * - `window.HamyarAppearanceBridge`: خواندن هم‌زمانِ وضعیت در script ابتدایی
 * - `hamyarappearancechange`: رویداد تغییر زنده با `event.detail`
 */
class HamyarAppearanceBridge(private val prefs: UiPrefs) {
    @JavascriptInterface
    fun themePreference(): String = prefs.darkMode

    @JavascriptInterface
    fun resolvedTheme(): String = if (prefs.darkTheme) "dark" else "light"

    @JavascriptInterface
    fun appearanceJson(): String = JSONObject()
        .put("version", 1)
        .put("themePreference", themePreference())
        .put("resolvedTheme", resolvedTheme())
        .toString()
}

/**
 * نصب bridge فقط-خواندنی پیش از بارگذاری سند. هیچ API حساس یا امکان نوشتنی به HTML
 * داده نمی‌شود؛ محتوا فقط می‌تواند رنگ نهایی/انتخاب کاربر را بخواند.
 */
fun WebView.installHamyarAppearanceBridge(prefs: UiPrefs) {
    addJavascriptInterface(HamyarAppearanceBridge(prefs), "HamyarAppearanceBridge")
}

fun WebView.publishHamyarAppearance(
    preference: String,
    isDark: Boolean,
) {
    val safePreference = preference.takeIf { it == "light" || it == "dark" || it == "system" } ?: "system"
    val resolvedTheme = if (isDark) "dark" else "light"
    val preferenceJs = JSONObject.quote(safePreference)
    val resolvedJs = JSONObject.quote(resolvedTheme)
    val script =
        """
        (function () {
          var root = document.documentElement;
          if (!root) return;
          var preference = $preferenceJs;
          var resolvedTheme = $resolvedJs;
          root.setAttribute('data-hamyar-theme-preference', preference);
          root.setAttribute('data-hamyar-theme', resolvedTheme);
          window.HamyarAppearance = {
            version: 1,
            themePreference: preference,
            resolvedTheme: resolvedTheme
          };
          try {
            window.dispatchEvent(new CustomEvent('hamyarappearancechange', {
              detail: window.HamyarAppearance
            }));
          } catch (_) {}
        })();
        """.trimIndent()
    // تمام فراخوانی‌ها از lifecycle WebView روی main thread می‌آیند؛ runCatching فقط
    // race طبیعیِ destroy شدن WebView در بازگشت سریع کاربر را بی‌خطر می‌کند.
    runCatching { evaluateJavascript(script, null) }
}
