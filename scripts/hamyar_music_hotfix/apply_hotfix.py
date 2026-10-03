#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

REPO = Path.cwd()

FILES = {
    "bridge": REPO / "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/MusicFrameOverlayBridge.kt",
    "tile": REPO / "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/BackgroundMusicTileHost.kt",
}

SAFE_BRIDGE = r'''package com.hamyareman.ir.ui.study

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
'''


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    s = path.read_text(encoding="utf-8")
    n = s.count(old)
    if n == 0:
        raise SystemExit(f"[FAIL] anchor not found: {label} in {path}")
    if n > 1:
        raise SystemExit(f"[FAIL] anchor appears {n} times: {label} in {path}")
    path.write_text(s.replace(old, new, 1), encoding="utf-8")
    print(f"[OK] {label}")


def main() -> int:
    for k, p in FILES.items():
        if not p.exists():
            raise SystemExit(f"[FAIL] missing source file: {p}")

    # 1) Replace the fixed/fullscreen native bridge with a capture-phase in-flow bridge.
    FILES["bridge"].write_text(SAFE_BRIDGE, encoding="utf-8")
    print("[OK] replace MusicFrameOverlayBridge.kt with in-flow capture-phase bridge")

    # 2) Sleep page: remove continuous Android WebView resize while the tile HTML
    # switches its nested iframe layout.
    tile = FILES["tile"]
    replace_once(tile, "import androidx.compose.animation.core.animateDpAsState\n", "", "remove WebView height animation")
    replace_once(tile, "import androidx.compose.animation.core.tween\n", "", "remove animation tween import")

    old_anim = """            // باز که می‌شود، میزبان تا ته صفحه بالا می‌آید تا آکاردئون بریده نشود.\n            val target = if (expanded) (screenHeight - 24.dp).coerceAtLeast(420.dp) else TILE_HEIGHT\n            val height by animateDpAsState(target, tween(340), label = \"music tile height\")\n            Box(modifier.fillMaxWidth().height(height)) {\n"""
    new_anim = """            // فقط یک اندازهٔ نهایی؛ بدون انیمیشنِ چندچرخه‌ای روی Android WebView.\n            val height = if (expanded) (screenHeight - 24.dp).coerceAtLeast(420.dp) else TILE_HEIGHT\n            Box(modifier.fillMaxWidth().height(height)) {\n"""
    replace_once(tile, old_anim, new_anim, "disable animated WebView resizing")

    old_after = """                            // تنها صفحات موسیقی/خواب نگهبان Web Audio دارند تا با قفل صفحه قطع نشوند.\n                            view.bindManagedMediaLifecycle(watchWebAudio = true)\n                            // پشتیبان: اگر رابط HamyarHost به هر دلیل صدا نخورد،\n"""
    new_after = """                            // تنها صفحات موسیقی/خواب نگهبان Web Audio دارند تا با قفل صفحه قطع نشوند.\n                            view.bindManagedMediaLifecycle(watchWebAudio = true)\n                            // background-music-tile.html خودش iframe داخلی را fixed می‌کند.\n                            // در Android WebView آن لایهٔ fixed را به flow محلی تبدیل می‌کنیم؛\n                            // خود فایل باکت، رمزنگاری و ۲۹ صدای embed شده دست‌نخورده می‌مانند.\n                            view.evaluateJavascript(\n                                \"\"\"\n                                (function(){\n                                  if(window.__hamyarSleepTileSafe)return;\n                                  window.__hamyarSleepTileSafe=true;\n                                  var s=document.createElement('style');\n                                  s.setAttribute('data-hamyar-sleep-tile-safe','');\n                                  s.textContent=\n                                    'body.is-open{overflow:visible!important}' +\n                                    'body.is-open #scrim{position:absolute!important;inset:0!important;z-index:2147483646!important}' +\n                                    'body.is-open #backgroundMusicFrame{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;max-height:none!important;z-index:2147483647!important}';\n                                  (document.head||document.documentElement).appendChild(s);\n                                })();\n                                \"\"\".trimIndent(),\n                                null,\n                            )\n                            // پشتیبان: اگر رابط HamyarHost به هر دلیل صدا نخورد،\n"""
    replace_once(tile, old_after, new_after, "neutralize nested fixed iframe in tile WebView")

    print("\nHOTFIX APPLIED")
    print("- source HTML files untouched")
    print("- ParsPack URLs unchanged")
    print("- yoga/sport: message ownership is single + in-flow")
    print("- sleep: no animated WebView resizing + nested fixed iframe neutralized")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
