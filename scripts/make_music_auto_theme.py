#!/usr/bin/env python3
"""ساخت نسخهٔ «تمِ خودکار» از دو قاب موسیقی.

ورودی فقط و فقط همان دو فایلِ شاخهٔ main است:

    main:0000/background-music-tile.html
    main:0000/background-music-full.html

دو کارِ این اسکریپت:

۱) حذفِ کاملِ دکمه‌های ☾/☀ (ids: `theme` و `miniTheme`) از مارک‌آپ، از render
   و از سیم‌کشیِ کلیک — یعنی هیچ کلیدِ دستیِ تم در صفحه نمی‌ماند.
۲) جایگزینیِ بوت‌استرپِ `#hy-theme` با نسخه‌ای که تمِ روشن/تاریک را «خودکار»
   می‌گیرد: اول از برنامهٔ میزبان، وگرنه از `prefers-color-scheme` سیستم، و
   هر تغییرِ بعدی را هم زنده دنبال می‌کند.

اسکریپت idempotent است (نگهبان: `hy-theme-auto`) و اگر رشته‌ای که انتظار
می‌رود پیدا نشود با خطا می‌ایستد تا تغییرِ نیمه‌کاره تولید نشود.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

GUARD = "hy-theme-auto"

AUTO_BOOTSTRAP = """<script id="hy-theme" data-mode="hy-theme-auto">(function(){var W=window,D=document,E=D.documentElement,LIGHT='#f4faf7',DARK='#101713';
function ok(v){v=String(v==null?'':v).toLowerCase();return v==='dark'||v==='light'?v:null}
function appTheme(){var v=null;
 try{if(W.HamyarAppearanceBridge&&typeof W.HamyarAppearanceBridge.resolvedTheme==='function')v=ok(W.HamyarAppearanceBridge.resolvedTheme())}catch(e){}
 if(!v){try{if(W.HamyarHost&&typeof W.HamyarHost.getTheme==='function')v=ok(W.HamyarHost.getTheme())}catch(e){}}
 if(!v)v=ok(W.HAMYAR_THEME);
 if(!v){try{if(E.getAttribute('data-hamyar-theme-preference'))v=ok(E.getAttribute('data-hamyar-theme'))}catch(e){}}
 if(!v){try{v=ok(new URLSearchParams(location.search).get('theme'))||ok(new URLSearchParams(location.hash.replace(/^#/,'')).get('theme'))}catch(e){}}
 if(!v){try{v=ok(W.parent&&W.parent!==W&&W.parent.HAMYAR_THEME)}catch(e){}}
 return v}
function sysTheme(){try{return (W.matchMedia&&W.matchMedia('(prefers-color-scheme: dark)').matches)?'dark':'light'}catch(e){return 'light'}}
function resolve(){return appTheme()||sysTheme()}
var cur=null;
function apply(v,silent){var t=ok(v)||resolve(),changed=t!==cur;cur=t;
 try{E.setAttribute('data-theme',t);E.style.colorScheme=t;var m=D.querySelector('meta[name=theme-color]');if(m)m.setAttribute('content',t==='dark'?DARK:LIGHT)}catch(e){}
 if(changed&&!silent){try{W.dispatchEvent(new CustomEvent('hamyarthemechange',{detail:{theme:t}}))}catch(e){}}
 return t}
function auto(){var t=resolve();if(t!==cur)apply(t);return cur}
W.HamyarTheme={app:appTheme,system:sysTheme,initial:resolve,current:function(){return cur||resolve()},apply:apply,auto:auto,save:function(){}};
W.HamyarSetTheme=function(t){t=ok(t);if(!t)return;W.HAMYAR_THEME=t;apply(t)};
apply(null,true);
try{var mq=W.matchMedia&&W.matchMedia('(prefers-color-scheme: dark)');if(mq){var h=function(){if(!appTheme())auto()};mq.addEventListener?mq.addEventListener('change',h):mq.addListener(h)}}catch(e){}
try{W.addEventListener('hamyarappearancechange',auto)}catch(e){}
try{new MutationObserver(auto).observe(E,{attributes:true,attributeFilter:['data-hamyar-theme','data-hamyar-theme-preference']})}catch(e){}
try{W.addEventListener('message',function(e){var d=e&&e.data;if(d&&d.type==='theme'&&ok(d.theme)){W.HAMYAR_THEME=ok(d.theme);apply(d.theme)}})}catch(e){}
try{D.addEventListener('visibilitychange',function(){if(!D.hidden)auto()})}catch(e){}
})();</script>"""

BTN_MINI = '<button id="miniTheme" type="button" aria-label="حالت تاریک">☾</button>'
BTN_FULL = '<button id="theme" class="close" type="button" aria-label="حالت تاریک">☾</button>'

OLD_SET_THEME = (
    "function setTheme(v){theme=v==='dark'?'dark':'light';"
    "HamyarTheme.save(theme);save();render()}"
)
NEW_SET_THEME = (
    "function setTheme(v){theme=HamyarTheme.apply(v,true);save();render()}"
)

CLICK_WIRING = (
    "for(const id of ['theme','miniTheme'])$(id).onclick="
    "e=>{e.stopPropagation();setTheme(theme==='dark'?'light':'dark')};\n"
)

RENDER_LOOP = re.compile(
    r"for\(const id of \['theme','miniTheme'\]\)\{\$\(id\)\.setAttribute\([^\n]*?'☀':'☾'\}"
)

CSS_EXTRA = "#theme,#miniTheme{display:none!important}"

LISTENER = (
    "window.addEventListener('hamyarthemechange',function(e){"
    "var t=(e&&e.detail&&e.detail.theme)||HamyarTheme.current();"
    "if(t&&t!==theme){theme=t;render()}});\n"
)
ANCHOR = "window.BackgroundMusic={open:()=>setOpen(true)"


def fail(msg: str) -> None:
    print(f"✗ {msg}", file=sys.stderr)
    sys.exit(1)


def once(html: str, needle: str, repl: str, label: str) -> str:
    n = html.count(needle)
    if n != 1:
        fail(f"«{label}» باید دقیقاً یک بار باشد، ولی {n} بار پیدا شد.")
    return html.replace(needle, repl, 1)


def patch(html: str, name: str) -> str:
    if GUARD in html:
        print(f"• {name}: از قبل وصله خورده است.")
        return html

    # ۱) بوت‌استرپ تم → نسخهٔ خودکار
    m = re.search(r'<script id="hy-theme">.*?</script>', html, re.S)
    if not m:
        fail(f"{name}: بوت‌استرپ #hy-theme پیدا نشد.")
    html = html[: m.start()] + AUTO_BOOTSTRAP + html[m.end() :]

    # ۲) حذف دو دکمهٔ ماه/خورشید از مارک‌آپ
    html = once(html, BTN_MINI, "", f"{name}: دکمهٔ miniTheme")
    html = once(html, BTN_FULL, "", f"{name}: دکمهٔ theme")

    # ۳) حذف ارجاع‌های همان دو دکمه در render و در سیم‌کشیِ کلیک
    html, n = RENDER_LOOP.subn("", html)
    if n != 1:
        fail(f"{name}: حلقهٔ render دکمه‌های تم {n} بار پیدا شد.")
    html = once(html, CLICK_WIRING, "", f"{name}: onclick دکمه‌های تم")

    # ۴) setTheme دیگر «انتخابِ کاربر» را ذخیره نمی‌کند؛ فقط تم را اعمال می‌کند
    html = once(html, OLD_SET_THEME, NEW_SET_THEME, f"{name}: setTheme")

    # ۵) اگر جایی دکمه‌ای با همان id ساخته شد، دیده نشود
    i = html.find("</style>")
    if i < 0:
        fail(f"{name}: </style> پیدا نشد.")
    html = html[:i] + CSS_EXTRA + html[i:]

    # ۶) تغییر تمِ بیرونی → رنگِ همین لحظهٔ UI
    html = once(html, ANCHOR, LISTENER + ANCHOR, f"{name}: لنگرِ BackgroundMusic")
    return html


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--ref", default="origin/main", help="شاخه‌ای که فایل‌ها از آن خوانده می‌شوند")
    ap.add_argument("--out", required=True, help="پوشهٔ خروجی")
    args = ap.parse_args()

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    for name in ("background-music-tile.html", "background-music-full.html"):
        raw = subprocess.run(
            ["git", "show", f"{args.ref}:0000/{name}"],
            check=True, capture_output=True,
        ).stdout.decode("utf-8")
        patched = patch(raw, name)
        dest = out / name
        dest.write_text(patched, encoding="utf-8")
        print(f"✓ {name}: {len(raw):,} → {dest.stat().st_size:,} بایت")


if __name__ == "__main__":
    main()
