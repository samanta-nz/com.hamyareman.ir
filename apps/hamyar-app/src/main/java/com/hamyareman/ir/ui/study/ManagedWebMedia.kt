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

/** بعد از هر load صدا/ویدیوهای فعلی و عناصر بعدی را زیر نظر می‌گیرد. */
fun WebView.bindManagedMediaLifecycle() {
    evaluateJavascript(
        """
        (function(){
          if(window.__hamyarMediaLifecycleInstalled){return;}
          window.__hamyarMediaLifecycleInstalled=true;
          var active=new Set();
          function tell(){try{HamyarMediaLifecycle.onPlaybackChanged(active.size>0);}catch(e){}}
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
          scan(document);
          new MutationObserver(function(ms){ms.forEach(function(m){m.addedNodes.forEach(scan);});})
            .observe(document.documentElement,{childList:true,subtree:true});
          window.__hamyarStopAllMedia=function(){
            document.querySelectorAll('audio,video').forEach(function(el){try{el.pause();el.currentTime=0;}catch(e){}});
            try{if(window.AudioContext){} }catch(e){}
            active.clear();tell();
          };
        })();
        """.trimIndent(),
        null,
    )
}

/** قطع کامل صدا هنگام خروج از صفحه یا رفتن واقعی اپ به پس‌زمینه. */
fun WebView.stopManagedMedia() {
    evaluateJavascript(
        "try{if(window.__hamyarStopAllMedia)window.__hamyarStopAllMedia();else document.querySelectorAll('audio,video').forEach(function(x){x.pause();x.currentTime=0;});}catch(e){}",
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
