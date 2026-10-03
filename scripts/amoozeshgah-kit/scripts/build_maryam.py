# -*- coding: utf-8 -*-
"""نمونه کامل فونت مریم: دفتر خط‌دار + راهنماهای دقیق حروف + انیمیشن مسیر"""
import base64, os, xml.etree.ElementTree as ET

TTF = "/home/user/fonts/Dabir-Maryam-Soft.ttf"
OUT = "/home/user/font-specimens/Maryam-full.svg"
b64 = base64.b64encode(open(TTF, "rb").read()).decode()

LINE1 = "به نام خداوند جان و خرد"
LINE2 = "هرگز نمیرد آنکه دلش زنده شد به عشق"

# rules کاغذ
rules = "\n".join(f'<line x1="0" y1="{y}" x2="720" y2="{y}" stroke="#d7e6fb" stroke-width="2"/>'
                  for y in range(40, 1000, 56))

def cell(x0, y0, idx, char, name, tip, order, dot_zone):
    pid = f"dirB{idx}"
    zone = ""
    if dot_zone:
        zone = (f'<ellipse cx="{x0+160}" cy="{y0+186}" rx="46" ry="16" fill="none" stroke="#d33f49" '
                f'stroke-width="2" stroke-dasharray="6 5"/><text x="{x0+236}" y="{y0+191}" text-anchor="middle" '
                f'font-size="12" fill="#d33f49" class="lbl">نقطه</text>')
    else:
        zone = (f'<text x="{x0+160}" y="{y0+191}" text-anchor="middle" font-size="13" '
                f'fill="#2e9e5b" class="lbl">بی‌نقطه ✓</text>')
    return f'''
<rect x="{x0}" y="{y0}" width="320" height="260" rx="14" fill="#ffffff" stroke="#e3e1f0" stroke-width="2"/>
<text x="{x0+160}" y="{y0+32}" text-anchor="middle" font-size="19" font-weight="bold" fill="#23232e" class="lbl">حرف {name}</text>
<text x="{x0+160}" y="{y0+54}" text-anchor="middle" font-size="13" fill="#6b7080" class="lbl">{tip}</text>
<path id="{pid}" d="M{x0+260},{y0+86} Q{x0+160},{y0+62} {x0+60},{y0+86}" fill="none" stroke="#7c5cd6" stroke-width="3" class="flow" marker-end="url(#arr)"/>
<circle r="6" fill="#2e9e5b" stroke="#fff" stroke-width="2"><animateMotion dur="2.6s" repeatCount="indefinite"><mpath xlink:href="#{pid}" href="#{pid}"/></animateMotion></circle>
<line x1="{x0+16}" y1="{y0+120}" x2="{x0+304}" y2="{y0+120}" stroke="#5383e8" stroke-width="2" stroke-dasharray="7 5"/>
<text x="{x0+42}" y="{y0+114}" text-anchor="middle" font-size="11" fill="#5383e8" class="lbl">دندانه</text>
<line x1="{x0+16}" y1="{y0+168}" x2="{x0+304}" y2="{y0+168}" stroke="#2e9e5b" stroke-width="3"/>
<text x="{x0+42}" y="{y0+162}" text-anchor="middle" font-size="11" fill="#2e9e5b" class="lbl">کرسی</text>
<line x1="{x0+16}" y1="{y0+205}" x2="{x0+304}" y2="{y0+205}" stroke="#e8933c" stroke-width="2" stroke-dasharray="7 5"/>
<text x="{x0+42}" y="{y0+199}" text-anchor="middle" font-size="11" fill="#e8933c" class="lbl">دنباله</text>
<text x="{x0+160}" y="{y0+168}" text-anchor="middle" font-size="100" fill="#23232e" class="f">{char}</text>
{zone}
<polygon points="{x0+272},{y0+142} {x0+272},{y0+158} {x0+284},{y0+150}" fill="#2e9e5b"><title>شروع از راست</title></polygon>
<text x="{x0+160}" y="{y0+240}" text-anchor="middle" font-size="13" fill="#6b7080" class="lbl">{order}</text>'''

cells = "\n".join([
    cell(376, 348, 1, "ب", "ب", "یک نقطه زیر خط", "ترتیب: ۱ بدنه ← ۲ نقطه", True),
    cell(24, 348, 2, "ج", "ج", "خمیدگی زیر خط + نقطه", "ترتیب: ۱ دندانه و خم ← ۲ نقطه", True),
    cell(376, 623, 3, "س", "س", "سه دندانه، بی‌نقطه", "ترتیب: هر سه دندانه یک‌نفس", False),
    cell(24, 623, 4, "ع", "ع", "قوس دوتایی، بی‌نقطه", "ترتیب: ۱ قوس اول ← ۲ قوس دوم", False),
])

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 720 980">
<style>
@font-face{{font-family:'Maryam';src:url(data:font/ttf;base64,{b64}) format('truetype');}}
.f{{font-family:'Maryam','Vazirmatn',Tahoma,sans-serif;}}
.lbl{{font-family:Vazirmatn,Tahoma,sans-serif;}}
.flow{{stroke-dasharray:12 9;animation:ants .9s linear infinite;}}
@keyframes ants{{to{{stroke-dashoffset:-21;}}}}
</style>
<rect x="0" y="0" width="720" height="980" rx="18" fill="#ffffff"/>
{rules}
<line x1="664" y1="0" x2="664" y2="980" stroke="#f0a0a8" stroke-width="3"/>
<rect x="16" y="14" width="624" height="76" rx="14" fill="#7c5cd6"/>
<text x="328" y="46" text-anchor="middle" font-size="29" font-weight="bold" fill="#fff" class="lbl">نمونه کامل فونت مریم</text>
<text x="328" y="72" text-anchor="middle" font-size="14" fill="#efeaff" class="lbl">دفتر خط‌دار + راهنماهای دقیق حروف + انیمیشن مسیر درست</text>
<defs><marker id="arr" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="#7c5cd6"/></marker></defs>
<text x="640" y="118" text-anchor="end" font-size="14" fill="#6b7080" class="lbl">سطر نمونه روی خط کرسی</text>
<path id="dirP" d="M600,134 L110,134" fill="none" stroke="#7c5cd6" stroke-width="3.5" class="flow" marker-end="url(#arr)"/>
<circle r="7" fill="#2e9e5b" stroke="#fff" stroke-width="2"><animateMotion dur="3s" repeatCount="indefinite"><mpath xlink:href="#dirP" href="#dirP"/></animateMotion></circle>
<line x1="90" y1="158" x2="600" y2="158" stroke="#d33f49" stroke-width="2" stroke-dasharray="7 5"/>
<text x="84" y="162" text-anchor="end" font-size="12" fill="#d33f49" class="lbl">نقطه بالا</text>
<line x1="90" y1="182" x2="600" y2="182" stroke="#5383e8" stroke-width="2" stroke-dasharray="7 5"/>
<text x="84" y="186" text-anchor="end" font-size="12" fill="#5383e8" class="lbl">دندانه</text>
<line x1="90" y1="208" x2="600" y2="208" stroke="#2e9e5b" stroke-width="3.5"/>
<text x="84" y="212" text-anchor="end" font-size="12" fill="#2e9e5b" class="lbl">کرسی</text>
<text x="345" y="208" text-anchor="middle" font-size="44" fill="#23232e" class="f">{LINE1}</text>
<line x1="90" y1="250" x2="600" y2="250" stroke="#e8933c" stroke-width="2" stroke-dasharray="7 5"/>
<text x="84" y="254" text-anchor="end" font-size="12" fill="#e8933c" class="lbl">نقطه پایین</text>
<text x="640" y="292" text-anchor="end" font-size="14" fill="#6b7080" class="lbl">نمونه آزاد</text>
<text x="345" y="320" text-anchor="middle" font-size="40" fill="#23232e" class="f">{LINE2}</text>
{cells}
<rect x="24" y="900" width="672" height="62" rx="12" fill="#fff8e6" stroke="#f0d48a" stroke-width="2"/>
<text x="360" y="926" text-anchor="middle" font-size="13.5" fill="#6b5320" class="lbl">راهنما: خط سبز = خط کرسی • خط‌چین آبی = سقف دندانه • خط‌چین نارنجی = حد دنباله • بیضی قرمز = ناحیه نقطه</text>
<text x="360" y="948" text-anchor="middle" font-size="13.5" fill="#6b5320" class="lbl">فونت جاسازی‌شده: Dabir Maryam Soft • پیکان‌ها و نقطه‌های سبز متحرک‌اند</text>
</svg>'''

ET.fromstring(svg)
open(OUT, "w", encoding="utf-8").write(svg)
print(f"{OUT}: {os.path.getsize(OUT)//1024} KB — XML OK")
