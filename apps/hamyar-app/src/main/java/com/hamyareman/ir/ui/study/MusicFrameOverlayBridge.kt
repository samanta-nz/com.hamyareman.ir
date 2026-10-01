package com.hamyareman.ir.ui.study

import android.webkit.WebView

/**
 * iframe کوچکِ background music در فایل‌های یوگا/ورزش را بدون تغییر فایل HTML
 * به یک sheet تمام‌صفحه تبدیل می‌کند. خود player هنگام باز/بسته‌شدن message با
 * channel ثابت `hamyareman-background-v1` می‌فرستد؛ ما فقط قاب iframe را بزرگ
 * می‌کنیم و هنگام بستن دقیقاً style اولیه را برمی‌گردانیم.
 */
internal fun WebView.installMusicFrameOverlayBridge() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMusicFrameOverlayBridge)return;
          window.__hamyarMusicFrameOverlayBridge=true;
          var opened=new WeakMap();
          function findFrame(source){
            var frames=document.querySelectorAll('iframe');
            for(var i=0;i<frames.length;i++){
              var f=frames[i],src=(f.getAttribute('src')||'').toLowerCase();
              if(f.contentWindow===source || src.indexOf('background-music')>=0 || src.indexOf('music-background')>=0)return f;
            }
            return null;
          }
          function openFrame(f){
            if(!f)return;
            if(!opened.has(f))opened.set(f,{
              style:f.getAttribute('style'),
              bodyOverflow:document.body.style.overflow,
              rootOverflow:document.documentElement.style.overflow
            });
            f.style.setProperty('position','fixed','important');
            f.style.setProperty('inset','0','important');
            f.style.setProperty('display','block','important');
            f.style.setProperty('width','100vw','important');
            f.style.setProperty('height','100dvh','important');
            f.style.setProperty('max-width','none','important');
            f.style.setProperty('max-height','none','important');
            f.style.setProperty('border','0','important');
            f.style.setProperty('margin','0','important');
            f.style.setProperty('padding','0','important');
            f.style.setProperty('z-index','2147483647','important');
            document.body.style.overflow='hidden';
            document.documentElement.style.overflow='hidden';
          }
          function closeFrame(f){
            var previous=opened.get(f); if(!f||!previous)return;
            if(previous.style===null)f.removeAttribute('style');else f.setAttribute('style',previous.style);
            document.body.style.overflow=previous.bodyOverflow;
            document.documentElement.style.overflow=previous.rootOverflow;
            opened.delete(f);
          }
          window.addEventListener('message',function(e){
            var data=e.data;
            if(!data||data.channel!=='hamyareman-background-v1'||typeof data.opened!=='boolean')return;
            var frame=findFrame(e.source);
            if(data.opened)openFrame(frame);else closeFrame(frame);
          },false);
        })();
        """.trimIndent(),
        null,
    )
}
