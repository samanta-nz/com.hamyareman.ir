package com.hamyareman.ir.ui.study

import android.webkit.WebView

/**
 * پلِ iframe موسیقیِ پس‌زمینه در HTMLهای یوگا و ورزش.
 *
 * نسخهٔ قدیمی iframe را با `position:fixed` و `100dvh` از صفحهٔ مادر جدا می‌کرد.
 * این روش در Android WebView لمس را روی صفحهٔ میزبان می‌گرفت و بعد از بسته‌شدن هم
 * گاهی قاب را در بالای صفحه نگه می‌داشت. این پل فقط همان iframe را **در جای خودش**
 * بلند و کوتاه می‌کند؛ صفحهٔ مادر همیشه اسکرول‌پذیر و قابل لمس باقی می‌ماند.
 */
internal fun WebView.installMusicFrameOverlayBridge() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMusicFrameResizeBridge)return;
          window.__hamyarMusicFrameResizeBridge=true;
          var state=new WeakMap();
          var OPEN_HEIGHT='min(620px, 78vh)';
          var COLLAPSED_HEIGHT='92px';

          function musicFrame(source){
            var frames=document.querySelectorAll('iframe');
            for(var i=0;i<frames.length;i++){
              var frame=frames[i];
              var src=(frame.getAttribute('src')||'').toLowerCase();
              if(frame.contentWindow===source || src.indexOf('background-music')>=0){
                // صفحهٔ full خودش viewport کامل دارد؛ فقط iframeهای درونِ درس
                // باید با پیام باز/بسته resize شوند.
                if(frame.hasAttribute('data-hamyar-ignore-inflow-bridge'))return null;
                return frame;
              }
            }
            return null;
          }

          function setOpen(frame, open){
            if(!frame)return;
            if(!state.has(frame)){
              state.set(frame,{
                style:frame.getAttribute('style'),
                height:frame.style.height,
                minHeight:frame.style.minHeight,
                maxHeight:frame.style.maxHeight
              });
            }
            // مهم: هیچ position fixed / inset / z-index به iframe نمی‌دهیم.
            // iframe باید در flow طبیعی HTML مادر بماند تا touch و scroll والد خراب نشود.
            frame.style.setProperty('display','block','important');
            frame.style.setProperty('position','relative','important');
            frame.style.setProperty('inset','auto','important');
            frame.style.setProperty('width','100%','important');
            frame.style.setProperty('max-width','100%','important');
            frame.style.setProperty('border','0','important');
            frame.style.setProperty('margin','0','important');
            frame.style.setProperty('padding','0','important');
            frame.style.setProperty('z-index','auto','important');
            frame.style.setProperty('height',open?OPEN_HEIGHT:COLLAPSED_HEIGHT,'important');
            frame.style.setProperty('min-height',open?OPEN_HEIGHT:COLLAPSED_HEIGHT,'important');
            frame.style.setProperty('max-height',open?OPEN_HEIGHT:COLLAPSED_HEIGHT,'important');
            frame.setAttribute('data-hamyar-music-open',open?'true':'false');
          }

          window.addEventListener('message',function(event){
            var data=event.data;
            if(!data || data.channel!=='hamyareman-background-v1' || typeof data.opened!=='boolean')return;
            setOpen(musicFrame(event.source),data.opened);
          },false);
        })();
        """.trimIndent(),
        null,
    )
}
