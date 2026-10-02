# -*- coding: utf-8 -*-
"""نمونه SVG هر ۴ فونت + انیمیشن مسیر درست نوشتن + گالری مقایسه"""
import base64, os, xml.etree.ElementTree as ET

FONTS = [
    ("Dabir", "/home/user/fonts/Dabir.ttf", "دبیر"),
    ("Dabir Maryam Soft", "/home/user/fonts/Dabir-Maryam-Soft.ttf", "دبیر مریم سافت"),
    ("Hilda", "/home/user/fonts/Hilda.ttf", "هیلدا"),
    ("Badkhat", "/home/user/fonts/Badkhat.ttf", "بدخط"),
]
OUT = "/home/user/font-specimens"
os.makedirs(OUT, exist_ok=True)

ROW1 = "ا ب پ ت ث ج چ ح خ د ذ ر ز ژ س ش"
ROW2 = "ص ض ط ظ ع غ ف ق ک گ ل م ن و ه ی"
LINE1 = "به نام خداوند جان و خرد"
LINE2 = "هرگز نمیرد آنکه دلش زنده شد به عشق"

def specimen(fam, fa_name, b64):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 720 620">
<style>
@font-face{{font-family:'{fam}';src:url(data:font/ttf;base64,{b64}) format('truetype');}}
.f{{font-family:'{fam}','Vazirmatn',Tahoma,sans-serif;}}
.flow{{stroke-dasharray:12 9;animation:ants .9s linear infinite;}}
.draw{{fill:rgba(124,92,214,.10);stroke:#7c5cd6;stroke-width:2;stroke-dasharray:9 7;animation:ants 1.1s linear infinite;}}
@keyframes ants{{to{{stroke-dashoffset:-21;}}}}
.lbl{{font-family:Vazirmatn,Tahoma,sans-serif;}}
</style>
<rect x="2" y="2" width="716" height="616" rx="18" fill="#ffffff" stroke="#e3e1f0" stroke-width="2"/>
<rect x="2" y="2" width="716" height="80" rx="18" fill="#7c5cd6"/>
<text x="360" y="36" text-anchor="middle" font-size="28" font-weight="bold" fill="#fff" class="lbl">نمونه فونت: {fa_name} ({fam})</text>
<text x="360" y="62" text-anchor="middle" font-size="14" fill="#efeaff" class="lbl">حروف • متن • انیمیشن مسیر درست نوشتن</text>
<defs><marker id="arr" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0 0L10 5L0 10z" fill="#7c5cd6"/></marker></defs>
<text x="648" y="122" text-anchor="end" font-size="15" fill="#6b7080" class="lbl">حروف الفبا</text>
<text x="360" y="162" text-anchor="middle" font-size="38" fill="#23232e" class="f">{ROW1}</text>
<text x="360" y="208" text-anchor="middle" font-size="38" fill="#23232e" class="f">{ROW2}</text>
<line x1="60" y1="230" x2="660" y2="230" stroke="#e3e1f0" stroke-width="2"/>
<text x="648" y="262" text-anchor="end" font-size="15" fill="#6b7080" class="lbl">نمونه متن</text>
<text x="360" y="304" text-anchor="middle" font-size="36" fill="#23232e" class="f">{LINE1}</text>
<text x="360" y="348" text-anchor="middle" font-size="36" fill="#23232e" class="f">{LINE2}</text>
<line x1="60" y1="370" x2="660" y2="370" stroke="#e3e1f0" stroke-width="2"/>
<text x="648" y="402" text-anchor="end" font-size="15" fill="#6b7080" class="lbl">انیمیشن مسیر درست (راست به چپ)</text>
<path id="dirP" d="M600,436 L120,436" fill="none" stroke="#7c5cd6" stroke-width="4" class="flow" marker-end="url(#arr)"/>
<circle r="8" fill="#2e9e5b" stroke="#fff" stroke-width="2"><animateMotion dur="3s" repeatCount="indefinite"><mpath xlink:href="#dirP" href="#dirP"/></animateMotion></circle>
<text x="360" y="520" text-anchor="middle" font-size="76" class="f draw">بنویس</text>
<text x="360" y="556" text-anchor="middle" font-size="14" fill="#6b7080" class="lbl">واژه «بنویس» با خط‌چین متحرک = نمایش مسیر قلم</text>
<text x="360" y="592" text-anchor="middle" font-size="13" fill="#9aa0b4" class="lbl">فونت داخل همین فایل جاسازی شده و همه‌جا یکسان دیده می‌شود</text>
</svg>'''

files = []
for fam, path, fa in FONTS:
    b64 = base64.b64encode(open(path, "rb").read()).decode()
    svg = specimen(fam, fa, b64)
    ET.fromstring(svg)  # اعتبارسنجی XML
    fp = f"{OUT}/{fam.replace(' ', '-')}.svg"
    open(fp, "w", encoding="utf-8").write(svg)
    files.append((fam, fa, fp))
    print(f"{fp}: {os.path.getsize(fp)//1024} KB — XML OK")

# گالری مقایسه (هر ۴ نمونه یکجا)
cards = []
for fam, fa, fp in files:
    svg = open(fp, encoding="utf-8").read()
    # حذف اعلامیه‌های تکراری برای درج inline (نگه داشتن xmlns)
    cards.append(f"<h2>۱. {fa} <span>({fam})</span></h2>\n{svg}")
gal = f"""<!DOCTYPE html><html lang="fa" dir="rtl"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>مقایسه ۴ فونت — انتخاب برای درس خودکارنویسی</title>
<style>body{{margin:0;background:#f4f5fb;color:#1b1c26;font-family:'Vazirmatn',Tahoma,sans-serif;padding:16px}}
.wrap{{max-width:760px;margin:0 auto}}h1{{font-size:20px;text-align:center}}h2{{font-size:16px;margin:26px 0 8px;
background:#fff;border:1px solid #e7e8f2;border-radius:12px;padding:10px 14px}}h2 span{{color:#6b7080;font-weight:400;font-size:13px}}
svg{{width:100%;height:auto;display:block;background:#fff;border-radius:18px}}
.note{{background:#fff8e6;border:1px solid #f0d48a;border-radius:12px;padding:10px 14px;font-size:13px;line-height:2;margin-bottom:8px}}
</style></head><body><div class="wrap">
<h1>مقایسه ۴ فونت برای دیاگرام‌های خودکارنویسی</h1>
<div class="note">هر کارت شامل: حروف الفبا + دو بیت نمونه متن + <b>انیمیشن مسیر نوشتن</b> (پیکان متحرک و نقطه سبز که مسیر درست را طی می‌کند + واژه «بنویس» با خط‌چین متحرک).<br>پیشنهاد: فونت <b>بدخط</b> را می‌توان برای نمونه‌های <b>غلط ✗</b> نگه داشت و یکی از سه تای دیگر برای نمونه صحیح ✓.</div>
{'<hr>'.join(cards)}
</div></body></html>"""
open(f"{OUT}/gallery.html", "w", encoding="utf-8").write(gal)
print(f"gallery: {os.path.getsize(f'{OUT}/gallery.html')//1024} KB")
