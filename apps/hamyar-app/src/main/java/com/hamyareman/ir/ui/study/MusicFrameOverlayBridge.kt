package com.hamyareman.ir.ui.study

import android.webkit.WebView

/**
 * پلِ popup پلیر موسیقی برای iframeهای درس‌های یوگا و ورزش.
 *
 * iframe فقط پس از پیام صریحِ open تمام viewport را می‌گیرد و با پیام close دقیقاً
 * به style قبلی برمی‌گردد. هیچ navigation یا دست‌کاری history برای بستن انجام
 * نمی‌شود؛ بنابراین خروج از iframe/درسِ مادر دوباره رخ نمی‌دهد.
 */
internal fun WebView.installMusicFrameOverlayBridge() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMusicFrameOverlayBridge)return;
          window.__hamyarMusicFrameOverlayBridge=true;
          var saved=new WeakMap();
          var COLLAPSED_HEIGHT='92px';

          function usable(f){return f && !f.hasAttribute('data-hamyar-ignore-inflow-bridge');}
          function musicFrame(source){
            var frames=document.querySelectorAll('iframe'),i,f;
            // اولویت قطعی با همان iframe ای که پیام از آن آمده است.
            for(i=0;i<frames.length;i++){ f=frames[i];
              if(f.contentWindow===source && usable(f)) return f; }
            for(i=0;i<frames.length;i++){ f=frames[i];
              if((f.getAttribute('src')||'').toLowerCase().indexOf('background-music')>=0 && usable(f)) return f; }
            return null;
          }
          function theme(){
            var t=document.documentElement.getAttribute('data-hamyar-theme');
            return t==='dark'?'dark':'light';
          }

          function snapshot(frame){
            if(saved.has(frame))return saved.get(frame);
            var value={style:frame.getAttribute('style'),bodyOverflow:document.body.style.overflow,
              rootOverflow:document.documentElement.style.overflow,scrollX:window.scrollX,scrollY:window.scrollY};
            saved.set(frame,value); return value;
          }
          function close(frame){
            if(!frame)return;
            var old=saved.get(frame);
            if(old){
              old.style===null?frame.removeAttribute('style'):frame.setAttribute('style',old.style);
              document.body.style.overflow=old.bodyOverflow;
              document.documentElement.style.overflow=old.rootOverflow;
              window.scrollTo(old.scrollX,old.scrollY);
              saved.delete(frame);
            }else{
              frame.style.setProperty('height',COLLAPSED_HEIGHT,'important');
              frame.style.setProperty('min-height',COLLAPSED_HEIGHT,'important');
              frame.style.setProperty('max-height',COLLAPSED_HEIGHT,'important');
            }
            frame.setAttribute('data-hamyar-music-open','false');
          }
          function open(frame){
            if(!frame)return;
            snapshot(frame);
            document.body.style.overflow='hidden';
            document.documentElement.style.overflow='hidden';
            frame.style.cssText += ';display:block!important;position:fixed!important;inset:0!important;'+
              'width:100vw!important;height:100dvh!important;min-width:100vw!important;min-height:100dvh!important;'+
              'max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;'+
              'border:0!important;border-radius:0!important;z-index:2147483647!important;'+
              'background:'+(theme()==='dark'?'#101713':'#f4faf7')+'!important;'+
              'touch-action:auto!important;pointer-events:auto!important;';
            frame.setAttribute('data-hamyar-music-open','true');
            try{frame.focus()}catch(_){}
          }
          window.addEventListener('message',function(event){
            var data=event.data;
            if(!data || data.channel!=='hamyareman-background-v1' || typeof data.opened!=='boolean')return;
            var frame=musicFrame(event.source);
            data.opened?open(frame):close(frame);
          },false);
        })();
        """.trimIndent(),
        null,
    )
}
