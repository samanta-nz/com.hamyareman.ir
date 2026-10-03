#!/usr/bin/env python3
"""وصلهٔ سه فایل کاملِ موسیقی (نسخهٔ 0000) — تم و رفتار tile/full/popup.

ورودی: همان سه فایل خودبسندهٔ ۸٫۶ مگابایتی (player / tile / full) که هرکدام ۲۹
صدا را embed دارند. اسکریپت فقط رشته‌های کوچکِ منطق و CSS را عوض می‌کند و به
داده‌های base64 دست نمی‌زند. idempotent است.

    python3 scripts/patch_music_html.py 0000/background-music*.html
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

MARK = "hy-theme-v2"

OLD_BOOT_START = '<script id="hy-theme">'

# ── لایهٔ تم (نسخهٔ ۲): همان API قبلی + قرارداد ظاهرِ اپ + تم سیستم ──────────
NEW_BOOT = """<script id="hy-theme" data-version="hy-theme-v2">(function(){
var W=window,D=document,R=D.documentElement,K='hy-theme-override',CH='hamyareman-background-v1',EVT='hamyarthemechange',cur=null;
function ok(v){v=String(v||'').toLowerCase();return v==='dark'||v==='light'?v:null}
/* تم «اپ» — هر کدام زودتر پاسخ داد: پل ظاهر همیار، HamyarHost، متغیر سراسری،
   صفت data-hamyar-theme (وقتی اپ آن را اعلام کرده)، پارامتر theme در URL، یا تم والد. */
function appTheme(){var v=null;
  try{if(W.HamyarAppearanceBridge&&typeof W.HamyarAppearanceBridge.resolvedTheme==='function')v=ok(W.HamyarAppearanceBridge.resolvedTheme())}catch(e){}
  if(!v){try{if(W.HamyarHost&&typeof W.HamyarHost.getTheme==='function')v=ok(W.HamyarHost.getTheme())}catch(e){}}
  if(!v)v=ok(W.HAMYAR_THEME);
  if(!v&&R.getAttribute('data-hamyar-theme-preference'))v=ok(R.getAttribute('data-hamyar-theme'));
  if(!v){try{v=ok(new URLSearchParams(location.search).get('theme'))||ok(new URLSearchParams(location.hash.replace(/^#/,'')).get('theme'))}catch(e){}}
  if(!v){try{v=ok(W.parent&&W.parent!==W&&W.parent.HAMYAR_THEME)}catch(e){}}
  return v}
function stored(){try{var o=JSON.parse(sessionStorage.getItem(K)||'null');if(o&&ok(o.c)&&o.b===(appTheme()||'none'))return o.c}catch(e){}return null}
function system(){try{return W.matchMedia&&W.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'}catch(e){return 'light'}}
/* اولویت قطعی: ۱) تم اپ  ۲) انتخاب خود کاربر در همین نشست  ۳) تم سیستم */
function initial(){return appTheme()||stored()||system()}
function apply(v,silent){var t=ok(v)||'light';cur=t;
  R.setAttribute('data-theme',t);R.setAttribute('data-hamyar-theme',t);
  try{R.style.colorScheme=t}catch(e){}
  if(appTheme())R.setAttribute('data-hamyar-app-theme','');else R.removeAttribute('data-hamyar-app-theme');
  var m=D.querySelector('meta[name=theme-color]');if(m)m.setAttribute('content',t==='dark'?'#101713':'#f4faf7');
  if(!silent){try{W.dispatchEvent(new CustomEvent(EVT,{detail:t}))}catch(e){}}}
W.HamyarTheme={app:appTheme,initial:initial,current:function(){return cur||initial()},apply:apply,
  appOwns:function(){return appTheme()!==null},
  save:function(c){try{sessionStorage.setItem(K,JSON.stringify({c:ok(c)||'light',b:appTheme()||'none'}))}catch(e){}}};
W.HamyarSetTheme=function(t){t=ok(t);if(!t)return;W.HAMYAR_THEME=t;
  if(W.BackgroundMusic&&typeof W.BackgroundMusic.setTheme==='function')W.BackgroundMusic.setTheme(t);else apply(t)};
apply(initial(),true);
/* تغییر زندهٔ تم: رویداد اپ، تغییر صفت‌های اپ، پیام میزبان، یا تغییر تم سیستم */
W.addEventListener('hamyarappearancechange',function(){apply(initial())});
try{new MutationObserver(function(){var a=appTheme();if(a&&a!==cur)apply(a)})
  .observe(R,{attributes:true,attributeFilter:['data-hamyar-theme','data-hamyar-theme-preference']})}catch(e){}
try{W.matchMedia('(prefers-color-scheme: dark)').addEventListener('change',function(e){
  if(!appTheme()&&!stored())apply(e.matches?'dark':'light')})}catch(e){}
W.addEventListener('message',function(e){var d=e.data;
  if(d&&d.channel===CH&&d.type==='theme'&&ok(d.theme))apply(d.theme)});
})();</script>"""

# ── CSS: قانون تم + برداشتن سقف‌های ارتفاع ──────────────────────────────────
EXTRA_CSS = """
/* ===== وصلهٔ همیار ===== */
/* قانون تم: وقتی تم را اپ تعیین کرده، کلید ماه/خورشیدِ محلی دیده نمی‌شود. */
html[data-hamyar-app-theme] #theme,html[data-hamyar-app-theme] #miniTheme{display:none!important}
/* گونهٔ full: هیچ سقف ارتفاعی نیست و کل صفحه قابل دیدن/اسکرول است. */
body.full.is-open{overflow:auto!important;overflow-x:hidden}
body.full{height:auto;min-height:100dvh}
body.full .sheet,body.full .sheet-head,body.full .catalog{max-height:none!important;overflow:visible}
body.full .backdrop{max-height:none}
/* گونهٔ tile: آکاردئونِ رو به پایین تا ته فضای میزبان باز می‌شود. */
body.tilemode .backdrop.open{max-height:calc(100dvh - 92px)}
body.tilemode .sheet{max-height:calc(100dvh - 100px)}
body.tilemode .catalog{overscroll-behavior:contain}
"""

REPLACEMENTS: list[tuple[str, str]] = [
    # embedded را با هر ترکیبی از hash بپذیر (#embedded, #embedded-tile, …)
    (
        "const CHANNEL='hamyareman-background-v1',embedded=window.BG_EMBED===true||location.hash==='#embedded';",
        "const CHANNEL='hamyareman-background-v1',embedded=window.BG_EMBED===true||/(^|[#&-])embedded/.test(location.hash||'');",
    ),
    # تم محلی هم از همان لایهٔ مشترک عبور می‌کند (data-theme/color-scheme/meta یکی می‌مانند)
    (
        "function setTheme(v){theme=v==='dark'?'dark':'light';HamyarTheme.save(theme);save();render()}",
        "function setTheme(v){theme=v==='dark'?'dark':'light';"
        "if(!HamyarTheme.appOwns())HamyarTheme.save(theme);"
        "HamyarTheme.apply(theme,true);save();render()}",
    ),
    # گزارش باز/بسته شدن tile به هر دو نام رابط اندرویدی
    (
        "if(TILE){try{window.HamyarHost&&window.HamyarHost.onMusicTile&&window.HamyarHost.onMusicTile(opened)}catch(e){}",
        "if(TILE){try{window.HamyarHost&&window.HamyarHost.onMusicTile&&window.HamyarHost.onMusicTile(opened)}catch(e){}"
        "try{window.HamyarMusicTileHost&&window.HamyarMusicTileHost.setExpanded&&window.HamyarMusicTileHost.setExpanded(opened)}catch(e){}",
    ),
    # تغییر زندهٔ تم از سمت اپ باید UI را هم تازه کند
    (
        "document.body.classList.toggle('embedded',embedded);document.body.classList.toggle('full',FULL);",
        "window.addEventListener('hamyarthemechange',function(e){var t=e.detail==='dark'?'dark':'light';"
        "if(t!==theme){theme=t;render()}});"
        "document.body.classList.toggle('embedded',embedded);document.body.classList.toggle('full',FULL);",
    ),
]


def patch(text: str) -> str:
    if MARK in text:
        return text
    start = text.find(OLD_BOOT_START)
    if start < 0:
        raise SystemExit("❌ بوت‌استرپ تم (#hy-theme) پیدا نشد.")
    end = text.find("</script>", start) + len("</script>")
    text = text[:start] + NEW_BOOT + text[end:]

    anchor = "</style></head><body>"
    if anchor not in text:
        raise SystemExit("❌ پایان <style> پیدا نشد.")
    text = text.replace(anchor, EXTRA_CSS + anchor, 1)

    for old, new in REPLACEMENTS:
        if old not in text:
            raise SystemExit(f"❌ رشتهٔ مورد انتظار نیست: {old[:70]}…")
        text = text.replace(old, new, 1)
    return text


def main(paths: list[str]) -> int:
    if not paths:
        raise SystemExit("مسیر فایل‌ها را بده.")
    for name in paths:
        path = Path(name)
        raw = path.read_text(encoding="utf-8")
        out = patch(raw)
        tracks = out.count('"audio":"')
        variant = "full" if "window.BG_FULL=true" in out else ("tile" if "window.BG_TILE=true" in out else "player")
        if out == raw:
            print(f"= بدون تغییر (قبلاً وصله خورده): {path} [{variant}]")
            continue
        path.write_text(out, encoding="utf-8")
        print(f"✓ {path} [{variant}] — {len(raw):,} → {len(out):,} نویسه · {tracks} صدای embed شده")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
