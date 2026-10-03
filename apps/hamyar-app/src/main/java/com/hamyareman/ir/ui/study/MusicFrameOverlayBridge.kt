package com.hamyareman.ir.ui.study

import android.webkit.JavascriptInterface
import android.webkit.WebView

private const val OVERLAY_BRIDGE = "HamyarMusicOverlay"

/**
 * Stable in-flow bridge for music iframes inside lesson HTML.
 *
 * The lesson document already has its own message listener which changes the
 * music iframe to position:fixed and locks the parent body. In Android WebView
 * that creates a nested fullscreen iframe transition and steals the host input
 * surface. This bridge listens in the capture phase, consumes the state message
 * before the lesson listener sees it, and expands only the iframe's own flow box.
 */
internal fun WebView.installMusicFrameOverlayBridge() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMusicInflowBridge)return;
          window.__hamyarMusicInflowBridge=true;
          var CHANNEL='hamyareman-background-v1';
          var ATTR='data-hamyar-music-open';
          var OPEN_HEIGHT='min(620px,78vh)';
          var CLOSED_HEIGHT='88px';

          function musicFrame(source){
            var frames=document.querySelectorAll('iframe'),i,f,src;
            for(i=0;i<frames.length;i++){
              f=frames[i];
              if(f.contentWindow===source){
                if(f.hasAttribute('data-hamyar-ignore-inflow-bridge'))return null;
                return f;
              }
            }
            for(i=0;i<frames.length;i++){
              f=frames[i];
              if(f.hasAttribute('data-hamyar-ignore-inflow-bridge'))continue;
              src=(f.getAttribute('src')||'').toLowerCase();
              if(src.indexOf('background-music')>=0)return f;
            }
            return null;
          }

          function setOpen(frame,open){
            if(!frame)return;
            var h=open?OPEN_HEIGHT:CLOSED_HEIGHT;
            frame.style.setProperty('display','block','important');
            frame.style.setProperty('position','relative','important');
            frame.style.setProperty('inset','auto','important');
            frame.style.setProperty('top','auto','important');
            frame.style.setProperty('right','auto','important');
            frame.style.setProperty('bottom','auto','important');
            frame.style.setProperty('left','auto','important');
            frame.style.setProperty('width','100%','important');
            frame.style.setProperty('min-width','0','important');
            frame.style.setProperty('max-width','100%','important');
            frame.style.setProperty('height',h,'important');
            frame.style.setProperty('min-height',h,'important');
            frame.style.setProperty('max-height',h,'important');
            frame.style.setProperty('margin','0','important');
            frame.style.setProperty('padding','0','important');
            frame.style.setProperty('border','0','important');
            frame.style.setProperty('z-index','auto','important');
            frame.setAttribute(ATTR,open?'true':'false');
            try{window.HamyarMusicOverlay.onChanged(!!open)}catch(_){ }
          }

          /* Capture-phase is the key: the lesson's own bubble listener never gets
             the state message, so its fixed/inset/body-overflow code cannot run. */
          window.addEventListener('message',function(event){
            var data=event.data;
            if(!data || data.channel!==CHANNEL || typeof data.opened!=='boolean')return;
            var frame=musicFrame(event.source);
            if(!frame)return;
            try{event.stopImmediatePropagation();}catch(_){ }
            setOpen(frame,data.opened);
          },true);

          window.__hamyarCloseMusicOverlay=function(){
            var list=document.querySelectorAll('iframe['+ATTR+'="true"]'),i,f;
            for(i=0;i<list.length;i++){
              f=list[i];
              try{f.contentWindow.postMessage({channel:CHANNEL,type:'close'},'*')}catch(_){ }
              setOpen(f,false);
            }
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

/** Request the embedded music player to close, then collapse the flow box. */
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
