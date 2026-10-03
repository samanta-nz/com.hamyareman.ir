package com.hamyareman.ir.ui.study

import android.webkit.JavascriptInterface
import android.webkit.WebView

private const val OVERLAY_BRIDGE = "HamyarMusicOverlay"

/**
 * Bridge گزارش‌دهندهٔ وضعیت برای popup موسیقیِ iframeهای درس.
 *
 * هندسه و اندازهٔ iframe فقط متعلق به خود HTML درس است؛ این پل نباید آن را
 * دوباره resize/position کند، چون در WebView باعث رقابت دو مالک و از دست‌رفتن
 * لمس/اسکرول می‌شود.
 */
internal fun WebView.installMusicFrameOverlayBridge() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMusicReporter)return;
          window.__hamyarMusicReporter=true;
          var CHANNEL='hamyareman-background-v1';
          var activeFrame=null;

          function musicFrame(source){
            var frames=document.querySelectorAll('iframe'),i,f,src;
            for(i=0;i<frames.length;i++){
              f=frames[i];
              if(f.contentWindow===source)return f;
            }
            for(i=0;i<frames.length;i++){
              f=frames[i];
              src=(f.getAttribute('src')||'').toLowerCase();
              if(src.indexOf('background-music')>=0 || src.indexOf('%D9%85%D9%88%D8%B3%DB%8C%D9%82%DB%8C')>=0)return f;
            }
            return null;
          }

          window.addEventListener('message',function(event){
            var data=event.data;
            if(!data || data.channel!==CHANNEL || typeof data.opened!=='boolean')return;
            var frame=musicFrame(event.source);
            if(!frame)return;
            if(data.opened)activeFrame=frame;
            else if(activeFrame===frame)activeFrame=null;
            try{window.HamyarMusicOverlay.onChanged(!!data.opened)}catch(_){}
          },false);

          window.__hamyarCloseMusicOverlay=function(){
            var frame=activeFrame;
            if(!frame)return;
            try{frame.contentWindow.postMessage({channel:CHANNEL,type:'close'},'*')}catch(_){}
            setTimeout(function(){
              if(activeFrame===frame){
                activeFrame=null;
                try{window.HamyarMusicOverlay.onChanged(false)}catch(_){}
              }
            },350);
          };
        })();
        """.trimIndent(),
        null,
    )
}

/** WebView -> Compose notification used only for BackHandler/state UI. */
internal fun WebView.installMusicOverlayHost(onChanged: (Boolean) -> Unit) {
    addJavascriptInterface(MusicOverlayHost(this, onChanged), OVERLAY_BRIDGE)
}

/** Request the embedded music player to close; the HTML keeps ownership of geometry. */
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
