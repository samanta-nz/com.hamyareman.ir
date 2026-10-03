# -*- coding: utf-8 -*-
"""امبد دفتر تمرین PDF + کارت دانلود با عکس نمونه در انتهای درس"""
import base64, io, os, re
from PIL import Image

HTML = "/home/user/آموزشگاه/02-handwriting.html"
PDF = "/home/user/آموزشگاه/files/02-handwriting/daftar-tamrin.pdf"
SAMPLE = "/home/user/آموزشگاه/files/02-handwriting/nemune.png"

im = Image.open(SAMPLE).convert("RGB")
im = im.resize((700, int(im.height * 700 / im.width)), Image.LANCZOS)
buf = io.BytesIO()
im.save(buf, "JPEG", quality=68, optimize=True)
img_uri = "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode()
with open(PDF, "rb") as f:
    pdf_uri = "data:application/pdf;base64," + base64.b64encode(f.read()).decode()

css = """
.dlcard{margin:14px 0;padding:14px;border:2px dashed var(--accent);border-radius:16px;background:var(--bg);text-align:center}
.dlcard img{width:100%;max-width:420px;height:auto;border-radius:10px;border:1px solid var(--card-b)}
.dlcard h4{margin:10px 0 4px;font-size:15px}
.dlcard p{font-size:12px;color:var(--muted);margin:0 0 10px}
.dlcard a.dlbtn{display:inline-block;padding:10px 26px;border-radius:999px;color:#fff;font-weight:800;font-size:14px;text-decoration:none;background:linear-gradient(135deg,var(--accent),var(--accent2))}
"""
card = (f'<div class="dlcard"><img src="{img_uri}" alt="نمونه دفتر تمرین" loading="lazy">'
        f'<h4>دفتر تمرین خط خوش (PDF)</h4>'
        f'<p>۲۷ صفحه A4 آماده چاپ: ۱۷ حرف با خط‌چین، ۱۲ صفحه مشق جمله</p>'
        f'<a class="dlbtn" href="{pdf_uri}" download="daftar-tamrin.pdf">دانلود دفتر تمرین</a></div>')
card = card.replace('"', '\\"')  # مارکر داخل رشته JSON است؛ کوتیشن باید اسکیپ شود

with open(HTML, encoding="utf-8") as f:
    h = f.read()
assert "<!--PDFDL-->" in h, "marker missing"
h = h.replace("<!--PDFDL-->", card)
assert ".dlcard{" not in h, "dlcard already embedded!"
h = h.replace("</style>", css + "</style>")
h = re.sub(r'(<div class="meta">.*?)</div>', r"\1 • دفتر تمرین PDF</div>", h, count=1)
with open(HTML, "w", encoding="utf-8") as f:
    f.write(h)
print("PDF embedded, HTML MB:", round(os.path.getsize(HTML) / 1024 / 1024, 2))
