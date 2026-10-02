package com.hamyareman.ir.ui.study

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.hamyareman.ir.ui.appearance.UiPrefs
import org.json.JSONObject

/** نوع رنگ ویژهٔ iframe موسیقی؛ خود صفحهٔ درس رنگ اصلی خودش را نگه می‌دارد. */
enum class MusicEmbedPalette(val wire: String) {
    DEFAULT("default"),
    /** پالت روشن خاکی/نارنجی هماهنگ با فایل‌های حرکات ورزشی. */
    SPORT("sport"),
}

/**
 * قرارداد ظاهر بین اپ و هر سند HTML که در WebView باز می‌شود.
 *
 * HTML به SharedPreferences اندروید دسترسی ندارد. [installHamyarAppearanceBridge]
 * پیش از `loadUrl` نصب می‌شود تا script ابتدایی HTML هم وضعیت را بخواند؛ سپس
 * [publishHamyarAppearance] همان داده را در DOM و iframe موسیقیِ هم‌origin منتشر
 * می‌کند. هیچ HTML متن‌ساده‌ای روی دیسک نوشته یا در کش نگه‌داری نمی‌شود.
 */
class HamyarAppearanceBridge(private val prefs: UiPrefs) {
    @JavascriptInterface fun themePreference(): String = prefs.darkMode
    @JavascriptInterface fun resolvedTheme(): String = if (prefs.darkTheme) "dark" else "light"
    @JavascriptInterface fun appearanceJson(): String = JSONObject()
        .put("version", 1)
        .put("themePreference", themePreference())
        .put("resolvedTheme", resolvedTheme())
        .toString()
}

/** فقط API فقط‌خواندنی ظاهر را پیش از اجرای HTML در اختیار WebView می‌گذارد. */
fun WebView.installHamyarAppearanceBridge(prefs: UiPrefs) {
    addJavascriptInterface(HamyarAppearanceBridge(prefs), "HamyarAppearanceBridge")
}

/**
 * تم نهایی را به سند اصلی و iframeهای موسیقی هم‌origin می‌رساند.
 * `MusicEmbedPalette.SPORT` فقط iframeهای موسیقیِ داخل فایل‌های حرکات ورزشی را
 * با پالت بژ/قهوه‌ای روشن نمایش می‌دهد؛ خود HTML درس دست‌نخورده می‌ماند.
 */
fun WebView.publishHamyarAppearance(
    preference: String,
    isDark: Boolean,
    musicPalette: MusicEmbedPalette = MusicEmbedPalette.DEFAULT,
    cacheHit: Boolean = false,
) {
    val safePreference = preference.takeIf { it == "light" || it == "dark" || it == "system" } ?: "system"
    val resolvedTheme = if (isDark) "dark" else "light"
    val preferenceJs = JSONObject.quote(safePreference)
    val resolvedJs = JSONObject.quote(resolvedTheme)
    val paletteJs = JSONObject.quote(musicPalette.wire)
    val cachedJs = if (cacheHit) "true" else "false"
    val script =
        """
        (function () {
          var preference = $preferenceJs;
          var resolvedTheme = $resolvedJs;
          var musicPalette = $paletteJs;
          var fromCache = $cachedJs;
          var musicCss =
            "#theme,#miniTheme,[data-hamyar-local-theme]{display:none!important}" +
            "html[data-hamyar-breathing='true'] input[type='range']{direction:ltr!important;writing-mode:horizontal-tb!important;text-align:left!important;unicode-bidi:isolate!important;transform:none!important}" +
            "html[data-hamyar-breathing='true'] input[type='range']::-webkit-slider-runnable-track{direction:ltr!important;transform:none!important}" +
            "html[data-hamyar-breathing='true'] input[type='range']::-webkit-slider-thumb{direction:ltr!important}" +
            "html[data-hamyar-theme='dark']{color-scheme:dark;--green:#53d4a7;--dark:#e3eee8;--soft:#213f34;--border:#344c40}" +
            "html[data-hamyar-theme='dark'] body{background:#17271f!important;color:#e3eee8!important}" +
            "html[data-hamyar-music-palette='sport'][data-hamyar-theme='light']{--green:#a86f53;--dark:#604438;--soft:#f4e3d9;--border:#dfc0af}" +
            "html[data-hamyar-music-palette='sport'][data-hamyar-theme='light'] body{background:#f8ede5!important;color:#604438!important}";

          // پالت ورزش هرگز به سند درس تزریق نمی‌شود؛ فقط سندهای داخل iframe
          // پلیر آن را می‌گیرند. این جداسازی یوگا و سایر HTMLها را روی سبز قبلی نگه می‌دارد.
          function styleFor(doc, isMusicFrame) {
            if (!doc || !doc.documentElement) return;
            var root = doc.documentElement;
            root.setAttribute('data-hamyar-theme-preference', preference);
            root.setAttribute('data-hamyar-theme', resolvedTheme);
            root.setAttribute('data-hamyar-music-palette', isMusicFrame ? musicPalette : 'default');
            var documentUrl = doc.URL || '';
            try { documentUrl = decodeURIComponent(documentUrl); } catch (_) {}
            root.setAttribute('data-hamyar-breathing', /تمرینات تنفسی|breath/i.test(documentUrl) ? 'true' : 'false');
            // music player قدیمی با data-theme رنگ می‌گیرد؛ این مقدار نیز متعلق به
            // اپ است و کلید ماه/خورشید دیگر اجازهٔ override ندارد.
            root.setAttribute('data-theme', resolvedTheme);
            var style = doc.getElementById('__hamyar-appearance-style');
            if (!style) {
              style = doc.createElement('style');
              style.id = '__hamyar-appearance-style';
              (doc.head || root).appendChild(style);
            }
            style.textContent = musicCss;
            // هر سه گونهٔ پلیر ممکن است state تم خود را داشته باشند؛ اولویت قطعی
            // با تم اپ است، نه localStorage یا کلید ماه/خورشید داخل HTML.
            try {
              var api = doc.defaultView && doc.defaultView.BackgroundMusic;
              if (api && typeof api.setTheme === 'function') api.setTheme(resolvedTheme);
            } catch (_) {}
          }

          function cachedMusicBadge(doc) {
            var documentCacheHit = false;
            try { documentCacheHit = !!(doc && doc.defaultView && doc.defaultView.__hamyarHmkCacheHit); } catch (_) {}
            if ((!fromCache && !documentCacheHit) || !doc || doc.__hamyarCachedBadgeDone) return;
            var badge = doc.getElementById('loadBadge');
            var number = doc.getElementById('loadNumber');
            if (!badge || !number) return;
            doc.__hamyarCachedBadgeDone = true;
            // warmContent در فایل اصلی برای شبکه progress واقعی می‌کشد. در hit
            // کش، همان handler را بی‌اثر می‌کنیم تا هیچ progress جعلیِ دوم یا
            // زمان پنهان‌شدنِ دیگری نتواند قرارداد دقیق یک‌ثانیه‌ای را بشکند.
            try { doc.defaultView.drawLoading = function() {}; } catch (_) {}
            var started = performance.now();
            function frame(now) {
              // floor مانع رسیدن زودتر از یک ثانیه به ۱۰۰ می‌شود؛ در نخستین فریم
              // پس از ۱۰۰۰ms دقیقاً ۱۰۰ ثبت و نشانگر پنهان می‌شود.
              var pct = Math.min(100, Math.floor((now - started) / 10));
              badge.hidden = false;
              badge.classList.remove('failed', 'complete', 'indeterminate');
              number.textContent = String(pct).replace(/\d/g, function(d){ return '۰۱۲۳۴۵۶۷۸۹'[d]; });
              badge.style.setProperty('--progress', String(pct));
              if (pct < 100) requestAnimationFrame(frame);
              else badge.hidden = true;
            }
            requestAnimationFrame(frame);
          }

          function styleMusicFrames() {
            var frames = document.querySelectorAll('iframe');
            for (var i = 0; i < frames.length; i++) {
              var frame = frames[i];
              var src = (frame.getAttribute('src') || '').toLowerCase();
              if (src.indexOf('background-music') < 0) continue;
              function applyFrame(target) {
                try {
                  var doc = target.contentDocument;
                  styleFor(doc, true);
                  cachedMusicBadge(doc);
                  // گونهٔ full یک iframe پلیر درون خودش دارد؛ پالت را تا همان
                  // سند هم‌origin ادامه می‌دهیم، نه تا HTML درس میزبان.
                  var nested = doc.querySelectorAll('iframe');
                  for (var j = 0; j < nested.length; j++) {
                    var nestedSrc = (nested[j].getAttribute('src') || '').toLowerCase();
                    if (nestedSrc.indexOf('background-music') >= 0) applyFrame(nested[j]);
                  }
                  target.contentWindow.postMessage({
                    channel: 'hamyareman-background-v1',
                    type: 'theme',
                    theme: resolvedTheme,
                    palette: musicPalette
                  }, '*');
                } catch (_) {}
                if (!target.__hamyarAppearanceLoad) {
                  target.__hamyarAppearanceLoad = true;
                  target.addEventListener('load', function(event){ applyFrame(event.currentTarget); });
                }
              }
              applyFrame(frame);
            }
          }

          styleFor(document, false);
          cachedMusicBadge(document);
          window.HamyarAppearance = {
            version: 1,
            themePreference: preference,
            resolvedTheme: resolvedTheme,
            musicPalette: musicPalette
          };
          styleMusicFrames();
          try {
            window.dispatchEvent(new CustomEvent('hamyarappearancechange', {
              detail: window.HamyarAppearance
            }));
          } catch (_) {}
        })();
        """.trimIndent()
    runCatching { evaluateJavascript(script, null) }
}
