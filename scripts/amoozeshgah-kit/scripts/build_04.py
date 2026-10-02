# -*- coding: utf-8 -*-
"""بیلد ۰۴ یادداشت‌برداری (ایدempotent): قالب خام از بکاپ + SECTIONS از content_04 + امبد عکس‌ها."""
import json, base64, io, re
from PIL import Image
import sys
sys.path.insert(0, "/home/user")
from content_04 import SECTIONS

SRC = "/home/user/آموزشگاه-original-backup/04-note-taking.html"
DST = "/home/user/آموزشگاه/04-note-taking.html"
IMG_DIR = "/home/user/آموزشگاه/images/04-note-taking"

def data_uri(n, max_w=720, q=62):
    p = f"{IMG_DIR}/img-{n:02d}.jpg"
    im = Image.open(p).convert("RGB")
    if im.width > max_w:
        im = im.resize((max_w, int(im.height * max_w / im.width)), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "JPEG", quality=q, optimize=True)
    return "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode()

CSS = """
.item.open .body{max-height:12000px}
figure{margin:14px 0;text-align:center}
figure img{max-width:100%;border-radius:12px;box-shadow:var(--shadow)}
figcaption{font-size:12.5px;color:var(--muted);margin-top:6px;line-height:1.9}
figcaption .ref{font-weight:700;color:var(--accent)}
.tech{font-weight:700;margin:20px 0 8px;font-size:15.5px}
.tnum{background:var(--accent);color:#fff;border-radius:8px;padding:2px 12px;margin-left:10px;font-size:13px;white-space:nowrap}
"""

s = open(SRC, encoding="utf-8").read()
bodies = []
for sec in SECTIONS:
    b = sec["b"]
    for m in sorted(set(re.findall(r"FIG(\d+)", b)), key=int):
        b = b.replace(f"FIG{m}", data_uri(int(m)))
    bodies.append({"h": sec["h"], "b": b})
js = "const SECTIONS = " + json.dumps(bodies, ensure_ascii=False) + ";"
s2, n = re.subn(r"const SECTIONS = \[.*?\];", lambda m: js, s, count=1, flags=re.S)
assert n == 1, "SECTIONS not replaced"
s2 = s2.replace("</style>", CSS + "\n</style>", 1)
open(DST, "w", encoding="utf-8").write(s2)
words = sum(len(re.sub(r"<[^>]+>", " ", x["b"]).split()) for x in SECTIONS)
print("words:", words, "| figs:", sum(len(re.findall(r"<figure>", x["b"])) for x in SECTIONS),
      "| KB:", len(s2.encode()) // 1024)
json.loads(re.search(r"const SECTIONS = (\[.*?\]);", s2, re.S).group(1))
print("SECTIONS JSON OK")
