#!/usr/bin/env python3
"""اعمال «لایهٔ مشترک تم» و رفتارهای تایل/فول روی background-music.html.

این اسکریپت idempotent است: اگر فایل قبلاً وصله خورده باشد دوباره چیزی اضافه
نمی‌کند. فقط رشته‌های کوچکِ مشخص را جایگزین می‌کند و به data-URIهای صدا
(حجم ~۸٫۵MB) دست نمی‌زند.

استفاده:
    python3 scripts/apply_music_theme_patch.py background-music.html [...]
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MARK = "HamyarThemeLayer"

THEME_LAYER = """<script>
/* لایهٔ مشترک تم همیار — اولویت: ۱) تم اپ  ۲) انتخاب ذخیره‌شدهٔ کاربر  ۳) تم سیستم.
   در هر سه فایل (tile / full / player) عیناً یکسان است و پیش از اولین پینت اجرا می‌شود. */
(function(){
  var root=document.documentElement,KEY='hamyareman-background-v1',EVT='hamyarthemechange',current=null;
  function ok(t){return t==='dark'||t==='light'?t:null}
  function fromApp(){
    try{if(window.HamyarAppearanceBridge&&window.HamyarAppearanceBridge.resolvedTheme){
      var t=ok(window.HamyarAppearanceBridge.resolvedTheme());if(t)return t}}catch(_){}
    if(root.getAttribute('data-hamyar-theme-preference'))return ok(root.getAttribute('data-hamyar-theme'));
    return null;
  }
  function fromStore(){try{var p=JSON.parse(localStorage.getItem(KEY)||'null');return p?ok(p.theme):null}catch(_){return null}}
  function fromSystem(){try{return window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'}catch(_){return 'light'}}
  function appOwns(){return fromApp()!==null}
  function resolve(){return fromApp()||fromStore()||fromSystem()}
  function apply(value,silent){
    var t=ok(value)||'light';
    if(t===current&&root.getAttribute('data-theme')===t&&!silent)return;
    current=t;
    root.setAttribute('data-theme',t);
    root.setAttribute('data-hamyar-theme',t);
    try{root.style.colorScheme=t}catch(_){}
    if(appOwns())root.setAttribute('data-hamyar-app-theme','');else root.removeAttribute('data-hamyar-app-theme');
    var meta=document.querySelector('meta[name=theme-color]');
    if(meta)meta.setAttribute('content',t==='dark'?'#101713':'#f4faf7');
    if(!silent){try{window.dispatchEvent(new CustomEvent(EVT,{detail:t}))}catch(_){}}
  }
  window.HamyarThemeLayer={event:EVT,current:function(){return current||resolve()},resolve:resolve,apply:apply,appOwns:appOwns};
  apply(resolve(),true);
  window.addEventListener('hamyarappearancechange',function(){apply(resolve())});
  try{new MutationObserver(function(){var t=fromApp();if(t&&t!==current)apply(t)})
    .observe(root,{attributes:true,attributeFilter:['data-hamyar-theme','data-hamyar-theme-preference']})}catch(_){}
  try{window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change',function(e){
    if(!appOwns()&&!fromStore())apply(e.matches?'dark':'light')})}catch(_){}
  window.addEventListener('message',function(e){
    var d=e.data;if(d&&d.channel===KEY&&d.type==='theme'&&ok(d.theme))apply(d.theme)});
})();
</script>"""

VARIANT_CSS = """
/* --- قرارداد تم: کلید ماه/خورشیدِ محلی وقتی اپ تم را اعلام کرده باشد پنهان است --- */
html[data-hamyar-app-theme] #theme,html[data-hamyar-app-theme] #miniTheme,html[data-hamyar-app-theme] [data-hamyar-local-theme]{display:none!important}
/* --- گونهٔ full و پاپ‌آپ تمام‌صفحه: هیچ محدودیت ارتفاعی نیست --- */
body.variant-full .backdrop{padding:0;background:#0f2a2199}
body.variant-full .sheet{width:100%;max-width:none;height:100dvh;max-height:none;border-radius:0;border:0;box-shadow:none}
body.variant-full .sheet-head{max-height:none;overflow:visible;padding-top:max(14px,env(safe-area-inset-top))}
body.variant-full .catalog{max-height:none;padding-bottom:max(18px,env(safe-area-inset-bottom))}
body.variant-full .heading p{display:block}
/* در صفحهٔ full (نه در پاپ‌آپ تایل) هیچ بسته‌شدنی وجود ندارد؛ دکمهٔ بستن حذف می‌شود. */
body.locked-open #close{display:none}
body.locked-open #mini{display:none}
html[data-theme=dark] body.variant-full .backdrop{background:#06130ef2}
"""

REPLACEMENTS: list[tuple[str, str]] = [
    # تشخیص گونه: embedded و full از hash یا متغیر سراسری
    (
        "const CHANNEL='hamyareman-background-v1',embedded=window.BG_EMBED===true||location.hash==='#embedded';",
        "const CHANNEL='hamyareman-background-v1',hashFlags=(location.hash||'').toLowerCase(),"
        "embedded=window.BG_EMBED===true||hashFlags.indexOf('embedded')>=0,"
        "startFull=window.BG_VARIANT==='full'||hashFlags.indexOf('full')>=0;\n"
        "let fullMode=false,lockedOpen=false;"
        "function setFull(v,locked){fullMode=!!v;if(locked!==undefined)lockedOpen=!!locked;"
        "document.body.classList.toggle('variant-full',fullMode);"
        "document.body.classList.toggle('locked-open',lockedOpen);"
        "document.body.setAttribute('data-hamyar-music-variant',fullMode?'full':'tile');"
        "if(lockedOpen&&typeof setOpen==='function')setOpen(true)}",
    ),
    # تم اولیه از لایهٔ مشترک می‌آید، نه از مقدار ثابت light
    (
        "errorText='',theme='light',interacted=false;",
        "errorText='',theme=(window.HamyarThemeLayer&&window.HamyarThemeLayer.current())||'light',interacted=false;",
    ),
    # تم ذخیره‌شده نباید تم اپ را بشکند
    (
        "try{restore(JSON.parse(localStorage.getItem(CHANNEL)||'null'))}catch(e){}",
        "try{restore(JSON.parse(localStorage.getItem(CHANNEL)||'null'))}catch(e){}\n"
        "if(window.HamyarThemeLayer)theme=window.HamyarThemeLayer.current();",
    ),
    # setTheme از لایهٔ مشترک عبور می‌کند تا data-theme/data-hamyar-theme/color-scheme یکی بماند
    (
        "function setTheme(v){theme=v==='dark'?'dark':'light';save();render()}",
        "function setTheme(v){theme=v==='dark'?'dark':'light';"
        "if(window.HamyarThemeLayer)window.HamyarThemeLayer.apply(theme,true);"
        "if(!(window.HamyarThemeLayer&&window.HamyarThemeLayer.appOwns()))save();render()}",
    ),
    # پیام variant از میزبان (tile/full/پل اپ) گونهٔ نمایش را عوض می‌کند
    (
        "case 'theme':setTheme(e.data.theme);break;",
        "case 'theme':setTheme(e.data.theme);break;"
        "case 'variant':setFull(e.data.full===true||e.data.variant==='full',e.data.locked);break;",
    ),
    # در گونهٔ full پنجره هرگز بسته نمی‌شود
    (
        "function setOpen(v){opened=!!v;",
        "function setOpen(v){if(lockedOpen)v=true;opened=!!v;",
    ),
    # راه‌اندازی: گونه پیش از اولین رندر تعیین می‌شود + گوش‌دادن به تغییر تم لایهٔ مشترک
    (
        "document.body.classList.toggle('embedded',embedded);drawTiles();setOpen(false);render();",
        "document.body.classList.toggle('embedded',embedded);setFull(startFull,startFull);"
        "window.addEventListener('hamyarthemechange',function(e){var t=e.detail==='dark'?'dark':'light';"
        "if(t!==theme){theme=t;render()}});"
        "drawTiles();setOpen(lockedOpen);render();",
    ),
    # API عمومی: میزبان‌ها می‌توانند گونه را هم عوض کنند
    (
        "window.BackgroundMusic={open:()=>setOpen(true),close:()=>setOpen(false),pause,setVolume,setLayerVolume,setTheme,select:selectAndPlay,removeSecond,get state(){return state()}};",
        "window.BackgroundMusic={open:()=>setOpen(true),close:()=>setOpen(false),pause,setVolume,setLayerVolume,setTheme,"
        "setFull,select:selectAndPlay,removeSecond,get state(){return state()}};",
    ),
]


CF_MARKERS = ("window.__CF$cv$params", "static.cloudflareinsights.com")


def strip_cloudflare(text: str) -> str:
    """حذف اسکریپت‌های بازماندهٔ Cloudflare (از دانلود وب) — در اپ فقط درخواست مرده می‌سازند."""
    out, changed = [], False
    for part in re.split(r"(<script\b.*?</script>)", text, flags=re.S):
        if part.startswith("<script") and any(m in part for m in CF_MARKERS):
            changed = True
            continue
        out.append(part)
    return "".join(out) if changed else text


def patch(text: str) -> str:
    text = strip_cloudflare(text)
    if MARK in text:
        return text
    anchor = "</title><style>"
    if anchor not in text:
        raise SystemExit("❌ ساختار <title>/<style> پیدا نشد.")
    text = text.replace(anchor, "</title>" + THEME_LAYER + "<style>", 1)
    close_style = "</style></head><body>"
    if close_style not in text:
        raise SystemExit("❌ پایان <style> پیدا نشد.")
    text = text.replace(close_style, VARIANT_CSS + close_style, 1)
    for old, new in REPLACEMENTS:
        if old not in text:
            raise SystemExit(f"❌ رشتهٔ مورد انتظار پیدا نشد: {old[:60]}…")
        text = text.replace(old, new, 1)
    return text


def main(paths: list[str]) -> int:
    for name in paths:
        path = Path(name)
        raw = path.read_text(encoding="utf-8")
        out = patch(raw)
        if out == raw:
            print(f"= بدون تغییر (قبلاً وصله خورده): {path}")
            continue
        path.write_text(out, encoding="utf-8")
        print(f"✓ وصله خورد: {path} ({len(raw)} → {len(out)} بایت)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:] or ["background-music.html"]))
