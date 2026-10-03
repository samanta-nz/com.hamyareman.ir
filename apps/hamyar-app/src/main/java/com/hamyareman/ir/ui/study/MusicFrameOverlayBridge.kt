package com.hamyareman.ir.ui.study

import android.webkit.JavascriptInterface
import android.webkit.WebView

private const val OVERLAY_BRIDGE = "HamyarMusicOverlay"

/**
 * پلِ popup پلیر موسیقی برای iframeهای درس‌های یوگا و ورزش.
 *
 * ⚠️ خود صفحهٔ درس (`HamyaremanBackground.mount`) هم روی پیام `state` همین iframe را
 * تمام‌صفحه و برگشت می‌دهد (`resize(open)` با `frame.style.cssText`) و `body.overflow`
 * را خودش نگه می‌دارد/برمی‌گرداند. نسخهٔ قبلیِ پل از style «snapshot» می‌گرفت؛ ولی
 * listener صفحه زودتر اجرا می‌شود، پس snapshot همان style تمام‌صفحهٔ صفحه بود و روی
 * close همان برمی‌گشت: iframe بعد از بستن روی کل درس می‌ماند و body قفل (hidden) می‌ماند.
 *
 * حالا پل هیچ style inline را نمی‌خواند و نمی‌نویسد. فقط attribute
 * `data-hamyar-music-open` را روی iframe می‌گذارد و یک stylesheet ثابت (با !important)
 * روی همین attribute، viewport واقعی (vh/dvh) را اعمال می‌کند. با برداشتن attribute،
 * همان style خود صفحه (۸۸px) بدون هیچ بازیابی دستی دوباره اثر می‌کند.
 */
internal fun WebView.installMusicFrameOverlayBridge() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMusicFrameOverlayBridge)return;
          window.__hamyarMusicFrameOverlayBridge=true;
          var CHANNEL='hamyareman-background-v1';
          var ATTR='data-hamyar-music-open';

          var css=
            'iframe['+ATTR+'="true"]{display:block!important;position:fixed!important;inset:0!important;'+
            'top:0!important;left:0!important;right:0!important;bottom:0!important;'+
            // ترتیب عمدی: اول vh به‌عنوان پشتیبان، بعد dvh که اگر WebView پشتیبانی کند جایش را بگیرد.
            'width:100vw!important;min-width:100vw!important;'+
            'height:100vh!important;min-height:100vh!important;'+
            'height:100dvh!important;min-height:100dvh!important;'+
            'max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;'+
            'border:0!important;border-radius:0!important;overflow:hidden!important;'+
            'overscroll-behavior:none!important;z-index:2147483647!important;'+
            'touch-action:auto!important;pointer-events:auto!important;background:#f4faf7!important}'+
            'html[data-hamyar-theme="dark"] iframe['+ATTR+'="true"]{background:#101713!important}'+
            'html.hamyar-music-open,html.hamyar-music-open body{overflow:hidden!important}';
          var style=document.createElement('style');
          style.setAttribute('data-hamyar-music-overlay','');
          style.textContent=css;
          (document.head||document.documentElement).appendChild(style);

          function usable(f){return f && !f.hasAttribute('data-hamyar-ignore-inflow-bridge');}
          function isOpen(f){return f.getAttribute(ATTR)==='true';}
          function musicFrame(source){
            var frames=document.querySelectorAll('iframe'),i,f;
            // اولویت قطعی با همان iframe ای که پیام از آن آمده است.
            for(i=0;i<frames.length;i++){ f=frames[i];
              if(f.contentWindow===source && usable(f)) return f; }
            for(i=0;i<frames.length;i++){ f=frames[i];
              if((f.getAttribute('src')||'').toLowerCase().indexOf('background-music')>=0 && usable(f)) return f; }
            return null;
          }
          function sync(){
            var any=!!document.querySelector('iframe['+ATTR+'="true"]');
            document.documentElement.classList.toggle('hamyar-music-open',any);
            try{HamyarMusicOverlay.onChanged(any);}catch(e){}
          }
          function set(frame,open){
            if(!frame || isOpen(frame)===open)return;   // پیام‌های state تکراری بی‌اثرند
            frame.setAttribute(ATTR,open?'true':'false');
            if(open){try{frame.focus()}catch(_){}}
            sync();
          }
          window.addEventListener('message',function(event){
            var data=event.data;
            if(!data || data.channel!==CHANNEL || typeof data.opened!=='boolean')return;
            set(musicFrame(event.source),data.opened);
          },false);

          // فراخوانی از سمت اپ (دکمهٔ Back): از iframe می‌خواهد خودش را ببندد؛ اگر جواب
          // نداد (نسخهٔ قدیمی فایل)، پس از ۳۵۰ms attribute به‌زور برداشته می‌شود.
          window.__hamyarCloseMusicOverlay=function(){
            var list=document.querySelectorAll('iframe['+ATTR+'="true"]');
            Array.prototype.forEach.call(list,function(f){
              try{f.contentWindow.postMessage({channel:CHANNEL,type:'close'},'*');}catch(e){}
              setTimeout(function(){ set(f,false); },350);
            });
          };
        })();
        """.trimIndent(),
        null,
    )
}

/** وقتی popup موسیقی داخل درس باز/بسته می‌شود به اپ خبر می‌دهد (روی main thread). */
internal fun WebView.installMusicOverlayHost(onChanged: (Boolean) -> Unit) {
    addJavascriptInterface(MusicOverlayHost(this, onChanged), OVERLAY_BRIDGE)
}

/** برای BackHandler: popup باز را می‌بندد. */
internal fun WebView.closeMusicFrameOverlay() {
    evaluateJavascript(
        "try{window.__hamyarCloseMusicOverlay&&window.__hamyarCloseMusicOverlay();}catch(e){}",
        null,
    )
}

private class MusicOverlayHost(
    private val web: WebView,
    private val callback: (Boolean) -> Unit,
) {
    @JavascriptInterface
    fun onChanged(open: Boolean) {
        web.post { callback(open) }
    }
}
