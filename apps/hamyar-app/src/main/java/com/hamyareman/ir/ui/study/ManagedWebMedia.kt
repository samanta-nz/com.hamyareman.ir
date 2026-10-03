package com.hamyareman.ir.ui.study

import android.content.Context
import android.os.PowerManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val BRIDGE_NAME = "HamyarMediaLifecycle"

/** روی WebView کنترل lifecycle صوت HTML را نصب می‌کند. */
fun WebView.installManagedMediaLifecycle() {
    addJavascriptInterface(HtmlMediaLifecycleBridge(context.applicationContext), BRIDGE_NAME)
}

/**
 * بعد از هر load صدا/ویدیوهای فعلی و عناصر بعدی را زیر نظر می‌گیرد.
 *
 * پلیر موسیقی (`BackgroundMusic`) با Web Audio پخش می‌کند، نه با `<audio>`؛ پس رویداد
 * `play` هرگز نمی‌آمد و HtmlAudioKeepAliveService با خاموش‌شدن صفحه شروع نمی‌شد
 * (صدای «بشنو و بخواب» با قفل صفحه قطع می‌شد). حالا وضعیت `BackgroundMusic.state.playing`
 * هر ثانیه از خود صفحه و iframeهای هم‌origin خوانده می‌شود.
 */
fun WebView.bindManagedMediaLifecycle() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMediaLifecycleInstalled){return;}
          window.__hamyarMediaLifecycleInstalled=true;
          var active=new Set(), webAudio=false, last=null;
          function tell(){
            var now=active.size>0||webAudio;
            if(now===last)return;
            last=now;
            try{HamyarMediaLifecycle.onPlaybackChanged(now);}catch(e){}
          }
          function bind(el){
            if(el.__hamyarBound)return; el.__hamyarBound=true;
            el.addEventListener('play',function(){active.add(el);tell();});
            ['pause','ended','emptied','abort'].forEach(function(n){el.addEventListener(n,function(){active.delete(el);tell();});});
            if(!el.paused&&!el.ended){active.add(el);tell();}
          }
          function scan(root){
            if(root&&root.matches&&root.matches('audio,video'))bind(root);
            (root||document).querySelectorAll('audio,video').forEach(bind);
          }
          // صفحه + iframeهای هم‌origin (تو در تو). cross-origin بی‌صدا رد می‌شود.
          function windows(win,out,depth){
            out.push(win);
            if(depth>3)return out;
            try{
              var l=win.document.querySelectorAll('iframe'),i;
              for(i=0;i<l.length;i++){
                try{ if(l[i].contentWindow && l[i].contentWindow.document) windows(l[i].contentWindow,out,depth+1); }catch(e){}
              }
            }catch(e){}
            return out;
          }
          function musicPlaying(){
            var w=windows(window,[],0),i;
            for(i=0;i<w.length;i++){
              try{var b=w[i].BackgroundMusic; if(b&&b.state&&b.state.playing)return true;}catch(e){}
            }
            return false;
          }
          scan(document);
          new MutationObserver(function(ms){ms.forEach(function(m){m.addedNodes.forEach(scan);});})
            .observe(document.documentElement,{childList:true,subtree:true});
          setInterval(function(){webAudio=musicPlaying();tell();},1000);
          window.__hamyarStopAllMedia=function(){
            windows(window,[],0).forEach(function(w){
              try{w.document.querySelectorAll('audio,video').forEach(function(el){try{el.pause();el.currentTime=0;}catch(e){}});}catch(e){}
              try{if(w.BackgroundMusic&&w.BackgroundMusic.pause)w.BackgroundMusic.pause();}catch(e){}
            });
            active.clear(); webAudio=false; last=null; tell();
          };
        })();
        """.trimIndent(),
        null,
    )
}

/** قطع کامل صدا هنگام خروج از صفحه یا رفتن واقعی اپ به پس‌زمینه. */
fun WebView.stopManagedMedia() {
    evaluateJavascript(
        "try{if(window.__hamyarStopAllMedia)window.__hamyarStopAllMedia();else{document.querySelectorAll('audio,video').forEach(function(x){x.pause();x.currentTime=0;});if(window.BackgroundMusic&&window.BackgroundMusic.pause)window.BackgroundMusic.pause();}}catch(e){}",
        null,
    )
    HtmlAudioKeepAliveService.stop(context)
}

/**
 * ON_STOP با صفحهٔ روشن یعنی Home/Recent apps/اپ دیگر و باید صدا قطع شود. وقتی صفحه
 * قفل شده isInteractive=false است؛ پخش و foreground service تا پایان فایل می‌مانند.
 */
@Composable
fun ManagedWebMediaEffect(webView: () -> WebView?) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val view = webView() ?: return@LifecycleEventObserver
                    val power = view.context.getSystemService(PowerManager::class.java)
                    if (power?.isInteractive != false) view.stopManagedMedia()
                }
                Lifecycle.Event.ON_DESTROY -> webView()?.stopManagedMedia()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            webView()?.stopManagedMedia()
        }
    }
}

private class HtmlMediaLifecycleBridge(private val context: Context) {
    @JavascriptInterface
    fun onPlaybackChanged(active: Boolean) {
        if (active) HtmlAudioKeepAliveService.start(context)
        else HtmlAudioKeepAliveService.stop(context)
    }
}
