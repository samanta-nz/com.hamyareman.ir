# -*- coding: utf-8 -*-
"""جایگزینی هدر/فوتر از الگوی همیارمن + لودر درصدی (فایل‌های بالای ۲ مگ).
usage: python3 apply_chrome.py HTML NUM TITLE_FA TITLE_EN PREV PREV_LABEL NEXT NEXT_LABEL
مارکرها: class="heading" و <!--LOADER--> (ایزدمپوتنت نیست؛ روی فایل نهایی یک‌بار اجرا شود)
"""
import re, os, sys

HTML, NUM, T_FA, T_EN = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
PREV, PREV_L, NEXT, NEXT_L = sys.argv[5], sys.argv[6], sys.argv[7], sys.argv[8]

h = open(HTML, encoding="utf-8").read()
assert 'class="heading"' not in h, "chrome already applied!"
hue = int(re.search(r"--hue:(\d+)", h).group(1))

meta = ""
m = re.search(r'<div class="meta">(.*?)</div>', h)
if m:
    meta = f'<div class="meta">{m.group(1)}</div>'
meta = meta.replace("15 فایل صوتی", "۱۵ فایل صوتی")

header = (f'<header class="heading"><div><small>{NUM}</small><h1>{T_FA}</h1>'
          f'<div class="en" lang="en" dir="ltr">{T_EN}</div>{meta}</div>'
          f'<div class="brand header-brand">همیار من '
          f'<span class="tag" lang="en" dir="ltr">HAMYAR E MAN</span></div>'
          f'<button class="theme" id="theme" aria-label="تغییر تم روشن و تاریک">☀</button></header>')
h = re.sub(r"<header>.*?</header>", header, h, count=1, flags=re.S)

nav = ""
if PREV:
    nav += f'<a class="lnav" href="{PREV}">› {PREV_L}</a>'
if NEXT:
    nav += f'<a class="lnav" href="{NEXT}">{NEXT_L} ‹</a>'
nav = f'<div class="lesson-nav">{nav}</div>' if nav else ""
footer = (f"<footer>{nav}<p>تهیه شده اختصاصی برای اپلیکیشن</p>"
          f'<div class="brand">همیار من <span class="tag" lang="en">HAMYAR E MAN</span></div>'
          f"<p>برای امیدان آینده ایران زمین</p>"
          f"<p><strong>اپلیکیشن همیار من (فراتر از آموزش)</strong></p></footer>")
h = re.sub(r"<footer>.*?</footer>", footer, h, count=1, flags=re.S)

css = f"""
header.heading{{position:static;background:none;box-shadow:none;padding:14px 14px 0;color:inherit;max-width:540px;margin:0 auto 15px;display:grid;grid-template-columns:minmax(0,1fr) auto 39px;gap:7px;align-items:center;margin:0 2px 15px}}
.heading small{{color:var(--accent);font-size:10px}}
.heading h1{{margin:2px 0;font-size:23px;line-height:1.5}}
.heading .en{{font-size:10px;color:var(--muted);direction:ltr;text-align:right;letter-spacing:1px}}
.heading .meta{{font-size:10px;color:var(--muted);margin-top:3px}}
.brand{{display:flex;justify-content:center;align-items:center;gap:10px;font-size:19px;font-weight:800;letter-spacing:-.3px;color:var(--accent)}}
.tag{{direction:ltr;font-size:10px;letter-spacing:.5px;background:linear-gradient(135deg,#9becbe,#36a57c);color:#092c20;padding:3px 9px;border-radius:6px;font-weight:800}}
.header-brand{{flex-direction:column;gap:4px;font-size:14px;line-height:1.45;white-space:nowrap}}
.header-brand .tag{{font-size:7px;padding:3px 5px}}
.theme{{background:var(--card);border:1px solid var(--card-b);border-radius:50%;width:39px;height:39px;color:var(--accent);font-size:20px;cursor:pointer}}
footer{{border-top:1px solid var(--card-b);padding-top:20px;margin-top:25px}}
footer p{{margin:5px 0}}footer .brand{{font-size:17px;margin:10px 0}}footer strong{{font-size:11px;color:var(--text)}}
.lesson-nav{{display:flex;gap:10px;justify-content:center;margin-bottom:14px}}
.lnav{{font-size:12px;font-weight:800;color:var(--accent);text-decoration:none;border:1px solid var(--card-b);background:var(--card);border-radius:999px;padding:8px 18px}}
html[data-theme="light"]{{--bg:#f4f5fb;--card:#ffffff;--card-b:#e7e8f2;--text:#1b1c26;--muted:#6b7080;--accent:hsl({hue} 78% 46%);--accent2:hsl({hue+40} 85% 55%);color-scheme:light}}
html[data-theme="dark"]{{--bg:#0d0e14;--card:#171826;--card-b:#262838;--text:#eef0f7;--muted:#9aa0b4;--accent:hsl({hue} 85% 62%);--accent2:hsl({hue+40} 90% 65%);color-scheme:dark}}
"""
h = h.replace("</style>", css + "</style>")

h = h.replace("<head>", '<head>\n<script>try{var t=localStorage.getItem("amz-theme");if(t)document.documentElement.dataset.theme=t;}catch(e){}</script>', 1)

theme_js = """<script>
(function(){var b=document.getElementById("theme");
function cur(){return document.documentElement.dataset.theme||(matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light");}
function paint(){b.textContent=cur()==="dark"?"☀":"☾";}
b.onclick=function(){var n=cur()==="dark"?"light":"dark";document.documentElement.dataset.theme=n;try{localStorage.setItem("amz-theme",n);}catch(e){}paint();};paint();})();
</script>
</body>"""
h = h.replace("</body>", theme_js, 1)

size_mb = os.path.getsize(HTML) / 1024 / 1024
if size_mb > 2 and "<!--LOADER-->" not in h:
    loader_html = """<!--LOADER-->
<div id="loader"><div class="ld-box"><svg viewBox="0 0 140 140"><circle class="ld-bg" cx="70" cy="70" r="60"/><circle class="ld-fg" id="ldFg" cx="70" cy="70" r="60"/></svg><div class="ld-num" id="ldNum">۰٪</div><div class="ld-cap">در حال بارگذاری…</div></div></div>
<script>(function(){var C=376.99,p=0,done=false,fg=document.getElementById("ldFg"),nm=document.getElementById("ldNum");
var FA="۰۱۲۳۴۵۶۷۸۹";function fa(n){return String(n).replace(/[0-9]/g,function(d){return FA[d];});}
function set(v){p=v;fg.style.strokeDashoffset=(C-C*v/100).toFixed(1);nm.textContent=fa(Math.round(v))+"٪";}
set(4);var t=setInterval(function(){if(p<90)set(p+Math.max(.3,(90-p)/40));},50);
function fin(){if(done)return;done=true;clearInterval(t);set(100);var L=document.getElementById("loader");L.classList.add("done");setTimeout(function(){L.remove();},450);}
window.addEventListener("load",function(){setTimeout(fin,150)});setTimeout(fin,12000);})();</script>
"""
    h = re.sub(r"(<body[^>]*>)", r"\1\n" + loader_html, h, count=1)
    loader_css = """#loader{position:fixed;inset:0;z-index:9999;background:var(--bg);display:flex;align-items:center;justify-content:center}
#loader.done{opacity:0;pointer-events:none;transition:opacity .4s}
.ld-box{position:relative;width:170px;text-align:center}
.ld-box svg{width:170px;height:170px;transform:rotate(-90deg)}
.ld-bg,.ld-fg{fill:none;stroke-width:12}
.ld-bg{stroke:var(--card-b)}.ld-fg{stroke:var(--accent);stroke-linecap:round;stroke-dasharray:377;stroke-dashoffset:377}
.ld-num{position:absolute;top:56px;width:100%;font-size:32px;font-weight:800}
.ld-cap{margin-top:10px;font-size:12px;color:var(--muted)}
"""
    h = h.replace("</style>", loader_css + "</style>")
    print("loader injected")
else:
    print("loader skipped")

open(HTML, "w", encoding="utf-8").write(h)
print(f"chrome done: {HTML} ({os.path.getsize(HTML)/1024/1024:.2f} MB)")
