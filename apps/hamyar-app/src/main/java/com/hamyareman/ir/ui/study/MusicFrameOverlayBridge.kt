package com.hamyareman.ir.ui.study

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView

private const val OVERLAY_BRIDGE = "HamyarMusicOverlay"

/**
 * وضعیت باز/بسته‌شدن iframe موسیقی را فقط گزارش می‌کند.
 *
 * مالک هندسه خود فایل HTML درس است: HamyaremanBackground.mount().
 * این پل عمداً هیچ style، position، اندازه یا scroll را روی iframe دستکاری نمی‌کند؛
 * چون دو مالک همزمانِ layout باعث گیرکردن لمس/اسکرول در WebView می‌شد.
 */
internal fun WebView.installMusicFrameOverlayBridge() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMusicStateReporter)return;
          window.__hamyarMusicStateReporter=true;
          var CHANNEL='hamyareman-background-v1';
          var activeFrame=null;

          function findFrame(source){
            var frames=document.querySelectorAll('iframe'),i,f,src;
            for(i=0;i<frames.length;i++){
              f=frames[i];
              if(f.contentWindow===source)return f;
            }
            for(i=0;i<frames.length;i++){
              f=frames[i];
              src=(f.getAttribute('src')||'').toLowerCase();
              if(src.indexOf('background-music')>=0)return f;
            }
            return null;
          }

          function report(open){
            try{window.HamyarMusicOverlay.onChanged(!!open)}catch(_){}
            if(!open)activeFrame=null;
          }

          window.addEventListener('message',function(event){
            var data=event.data;
            if(!data || data.channel!==CHANNEL || typeof data.opened!=='boolean')return;
            var frame=findFrame(event.source);
            if(!frame)return;
            activeFrame=frame;
            report(data.opened);
          },false);

          window.__hamyarCloseMusicOverlay=function(){
            var frame=activeFrame;
            if(!frame){
              var frames=document.querySelectorAll('iframe'),i,f;
              for(i=0;i<frames.length;i++){
                f=frames[i];
                if((f.getAttribute('src')||'').toLowerCase().indexOf('background-music')>=0){
                  frame=f;break;
                }
              }
            }
            if(!frame){
              report(false);
              return;
            }
            try{frame.contentWindow.postMessage({channel:CHANNEL,type:'close'},'*')}catch(_){}
            setTimeout(function(){
              if(activeFrame===frame){
                activeFrame=null;
                report(false);
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

/** Request the embedded music player to close; HTML itself owns the geometry restore. */
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
