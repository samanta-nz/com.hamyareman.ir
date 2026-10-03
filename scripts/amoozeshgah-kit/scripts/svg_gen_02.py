#!/usr/bin/env python3
"""Lesson-02 v4 STATIC: حروف ثابت آبی خودکاری روی دفتر سه‌خطه.
ترتیب دقیق مطابق متن و ویس: ب، ج، د، ر، س، ص، ط، ع، ف، ک، مستقل، چک‌لیست.
خطوط دفتر آبی کمرنگ، راهنمای بالایی نارنجی کمرنگ، راهنمای پایینی قرمز کمرنگ.
Badkhat فقط و امبدشده، بدون فلش، بدون انیمیشن.
"""
import os
import base64
from PIL import ImageFont

OUT = "/home/user/آموزشگاه/images/02-handwriting"
BK = "/home/user/fonts/Badkhat.ttf"
LBLF = "/home/user/fonts/Badkhat.ttf"

INK = "#2b2620"; PAPER = "#fbf7ec"; CARD = "#fffdf6"; RULE = "#e3dccb"
PEN = "#1e40c9"
NOTE_RULE = "#c3d2ef"
BASE_BLUE = "#4a86e8"
TOP_ORANGE = "#f0bd77"
LOW_RED = "#efa3a3"
FAINT_PEN = "#1e40c9"
GOOD = "#1c7a3d"; BAD = "#c22f2f"

W = 900; PAD = 20
T0 = 150; T1 = 215
HD = 116; LH = 320; JH = 280; TH = 210; SP = 30; CAP_H = 78

_FC = {}
def F(path, size):
    k = (path, size)
    if k not in _FC:
        _FC[k] = ImageFont.truetype(path, size)
    return _FC[k]

def textw(font, s):
    tot = 0
    words = s.split(" ")
    for w in words:
        if w:
            b = font.getbbox(w, direction="rtl", language="fa")
            tot += b[2] - b[0]
    if len(words) > 1:
        sp = font.getbbox(" ", direction="rtl", language="fa")
        tot += (len(words) - 1) * (sp[2] - sp[0])
    return tot

def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def _fit(text, fs, maxw, minfs=20):
    target = maxw * 0.90
    while fs > minfs and textw(F(LBLF, fs), text) > target:
        fs -= 2
    return fs

def tline(x, y, text, fs, fill=INK, anchor="middle"):
    return (f'<text x="{x:.0f}" y="{y:.0f}" text-anchor="{anchor}" font-family="Badkhat" '
            f'font-size="{fs}" fill="{fill}">{esc(text)}</text>')

def head_label(x, cy, text, maxw):
    fs = _fit(text, 56, maxw, 34)
    if fs >= 34:
        return tline(x, cy + fs * 0.35, text, fs)
    words = text.split(" ")
    mid = (len(words) + 1) // 2
    l1, l2 = " ".join(words[:mid]), " ".join(words[mid:])
    fs2 = min(_fit(l1, 56, maxw), _fit(l2, 56, maxw))
    return (tline(x, cy - 10, l1, fs2) + tline(x, cy + fs2 * 0.95, l2, fs2))

def label_at(x, y, text, size, maxw=None, color=INK):
    fs = size * 2
    if maxw:
        fs = _fit(text, fs, maxw)
    return tline(x, y, text, fs, color)

with open(BK, "rb") as _ff:
    _B64 = base64.b64encode(_ff.read()).decode()
_FONT_STYLE = ("<style>@font-face{font-family:'Badkhat';src:url(data:font/ttf;base64," + _B64
               + ") format('truetype');}text{direction:rtl;}</style>")

def svg_open(title, sub, H):
    tfs = _fit(title, 68, W - 2 * PAD - 20, 30)
    sfs = _fit(sub, 40, W - 2 * PAD - 40, 20) if sub else 0
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
            + _FONT_STYLE
            + f'<rect width="{W}" height="{H}" fill="{PAPER}"/>'
            f'<rect x="8" y="8" width="{W - 16}" height="{H - 16}" rx="14" fill="none" stroke="{RULE}" stroke-width="2"/>'
            + tline(W / 2, 82, title, tfs)
            + (tline(W / 2, 158, sub, sfs, "#6b6252") if sub else ""))

def cap(text):
    fs = _fit(text, 44, W - 2 * PAD - 20)
    return tline(W / 2, 0, text, fs, "#6b6252")

def notebook(x0, y0, w, h, base, size):
    s = [f'<rect x="{x0}" y="{y0}" width="{w}" height="{h}" fill="{CARD}" stroke="{RULE}"/>']
    yy = y0 + 14
    while yy < y0 + h - 4:
        s.append(f'<line x1="{x0}" y1="{yy:.0f}" x2="{x0 + w}" y2="{yy:.0f}" stroke="{NOTE_RULE}" stroke-width="1"/>')
        yy += 28
    top = base - size * 0.95
    low = base + size * 0.60
    if y0 + 4 < top < y0 + h - 4:
        s.append(f'<line x1="{x0}" y1="{top:.0f}" x2="{x0 + w}" y2="{top:.0f}" stroke="{TOP_ORANGE}" stroke-width="2"/>')
    s.append(f'<line x1="{x0}" y1="{base:.0f}" x2="{x0 + w}" y2="{base:.0f}" stroke="{BASE_BLUE}" stroke-width="2.5"/>')
    if y0 + 4 < low < y0 + h - 4:
        s.append(f'<line x1="{x0}" y1="{low:.0f}" x2="{x0 + w}" y2="{low:.0f}" stroke="{LOW_RED}" stroke-width="2"/>')
    return "".join(s)

def glyph(text, cx, base, size, fill=PEN, opacity=1.0, tilt=0, sink=0):
    tr = f' transform="rotate({tilt} {cx:.0f} {base:.0f})"' if tilt else ""
    op = f' opacity="{opacity}"' if opacity < 1 else ""
    return (f'<text x="{cx:.0f}" y="{base + sink:.0f}" text-anchor="middle" font-family="Badkhat" '
            f'font-size="{size}" fill="{fill}"{op}{tr}>{esc(text)}</text>')

def head_row(y0, heads):
    n = len(heads); cw = (W - 2 * PAD) / n
    s = ""
    for i, h in enumerate(heads):
        x = PAD + i * cw
        s += f'<rect x="{x:.0f}" y="{y0}" width="{cw:.0f}" height="{HD}" fill="#f3eddc" stroke="{RULE}"/>'
        s += head_label(x + cw / 2, y0 + HD / 2, h, cw - 12)
    return s, y0 + HD

def letter_section(y0, items, size=120, form_size=24):
    n = len(items); cw = (W - 2 * PAD) / n
    base = y0 + LH * 0.56
    s = ""
    for i, (ch, name) in enumerate(items):
        x = PAD + i * cw
        cx = x + cw / 2
        s += notebook(x, y0, cw, LH, base, size)
        s += glyph(ch, cx, base, size)
        s += label_at(cx, y0 + LH - 24, name, form_size - 2, cw - 10)
    return s, y0 + LH

def practice_row(y0, items, size=76):
    n = len(items); cw = (W - 2 * PAD) / n
    base = y0 + TH * 0.70
    s = ""
    for i, (ch, name) in enumerate(items):
        x = PAD + i * cw
        cx = x + cw / 2
        s += notebook(x, y0, cw, TH, base, size)
        s += glyph(ch, cx, base, size, opacity=0.18)
        s += label_at(cx, y0 + 44, name, 20, cw - 10, "#6b6252")
    return s, y0 + TH

def judge_row(y0, quads, size=110):
    """quads: [(متن, نام, درست؟, شکاف)] — غلط: گسسته قرمز یا کج فرورفته."""
    n = len(quads); cw = (W - 2 * PAD) / n
    base = y0 + JH * 0.54
    s = ""
    for i, q in enumerate(quads):
        ch, name, ok = q[0], q[1], q[2]
        sk = q[3] if len(q) > 3 else None
        x = PAD + i * cw
        cx = x + cw / 2
        s += notebook(x, y0, cw, JH, base, size)
        if ok:
            s += glyph(ch, cx, base, size)
            col, mark, verdict = GOOD, "✓", "درست"
        elif sk is not None:
            p1, p2 = ch[:sk], ch[sk:]
            f = F(BK, size)
            w1, w2 = textw(f, p1), textw(f, p2)
            gap = size * 0.35
            xr = cx + (w1 + gap + w2) / 2
            s += glyph(p1, xr - w1 / 2, base, size, fill=BAD)
            s += glyph(p2, xr - w1 - gap - w2 / 2, base, size, fill=BAD)
            col, mark, verdict = BAD, "✗", "نادرست"
        else:
            s += glyph(ch, cx, base, size, fill=BAD, tilt=-10, sink=size * 0.18)
            col, mark, verdict = BAD, "✗", "نادرست"
        s += f'<text x="{x + 34:.0f}" y="{y0 + JH - 20:.0f}" text-anchor="middle" font-size="44" font-weight="bold" fill="{col}">{mark}</text>'
        s += label_at(cx + 20, y0 + JH - 20, f"{name} {verdict}", 20, cw - 90, col)
    return s, y0 + JH

def build_family(num, title, sub, forms, dots, judge_pair, size=120):
    H = T1 + HD + LH + SP + HD + LH + SP + TH + SP + JH + CAP_H + SP
    p = [svg_open(title, sub, H)]
    y = T1
    h, y = head_row(y, [f[1] for f in forms]); p.append(h)
    s, y = letter_section(y, forms, size); p.append(s)
    y += SP
    h, y = head_row(y, [d[1] for d in dots]); p.append(h)
    s, y = letter_section(y, dots, 84); p.append(s)
    y += SP
    s, y = practice_row(y, forms, 76); p.append(s)
    y += SP
    (c1, n1), (c2, n2) = judge_pair
    s, y = judge_row(y, [(c1, n1, True), (c1, n1, False), (c2, n2, True), (c2, n2, False)], 110)
    p.append(s)
    y += 50
    p.append(cap(f"تصویر شماره {num} - {title}").replace('y="0"', f'y="{y:.0f}"'))
    p.append("</svg>")
    return "".join(p)

def build_independent(num, title, sub):
    row1 = [("ا", "الف"), ("ل", "لام"), ("م", "میم"), ("ن", "نون")]
    row2 = [("و", "واو"), ("ه", "ه تنها"), ("ی", "ی")]
    row3 = [("هـ", "ه آغازی"), ("ـهـ", "ه میانی"), ("ـه", "ه پایانی")]
    H = T1 + (HD + LH + SP) * 3 + TH + SP + JH + CAP_H + SP
    p = [svg_open(title, sub, H)]
    y = T1
    for row in (row1, row2, row3):
        h, y = head_row(y, [r[1] for r in row]); p.append(h)
        s, y = letter_section(y, row, 120); p.append(s)
        y += SP
    s, y = practice_row(y, [("ا", "الف"), ("ل", "لام"), ("م", "میم"), ("ن", "نون"),
                                 ("و", "واو"), ("ه", "ه"), ("ی", "ی")], 64)
    p.append(s)
    y += SP
    s, y = judge_row(y, [("ه", "ه", True), ("ه", "ه", False), ("ی", "ی", True), ("ی", "ی", False)], 110)
    p.append(s)
    y += 50
    p.append(cap(f"تصویر شماره {num} - {title}").replace('y="0"', f'y="{y:.0f}"'))
    p.append("</svg>")
    return "".join(p)

CHECK_QS = [
    "۱ ــ از یک متری خوانده می‌شود؟",
    "۲ ــ نقطه‌ها سر جایشان هستند؟",
    "۳ ــ حروف روی خط نشسته‌اند؟",
    "۴ ــ فاصله واژه‌ها یکسان است؟",
    "۵ ــ سرعت بیشتر شده؟",
]

def build_checklist(num, title, sub):
    """نسخه موبایل: سؤال درشت + مربع‌ها زیرش، نوارها زیر هم."""
    QH = 170; BH = 96
    H = T1 + QH * 5 + SP + BH * 3 + CAP_H + SP
    p = [svg_open(title, sub, H)]
    y = T1
    for q in CHECK_QS:
        p.append(f'<rect x="{PAD}" y="{y}" width="{W - 2 * PAD}" height="{QH - 8}" rx="12" fill="{CARD}" stroke="{RULE}" stroke-width="2"/>')
        qfs = _fit(q, 54, W - 2 * PAD - 40, 28)
        p.append(tline(W - PAD - 16, y + 64, q, qfs, PEN, anchor="end"))
        bw, gap, bh = 30, 8, 36
        tot = 10 * bw + 9 * gap
        bx = (W - tot) / 2
        by = y + QH - 8 - bh - 14
        for k in range(10):
            p.append(f'<rect x="{bx + k * (bw + gap):.0f}" y="{by:.0f}" width="{bw}" height="{bh}" rx="6" fill="#fff" stroke="{BASE_BLUE}" stroke-width="2.5"/>')
        y += QH
    y += SP - 8
    bands = ["زیر ۳۰ - هنوز فرم حروف", "۳۰ تا ۴۰ - خوانا شده‌ای", "بالای ۴۰ - آماده سرعت"]
    for btxt in bands:
        p.append(f'<rect x="{PAD}" y="{y}" width="{W - 2 * PAD}" height="{BH - 8}" rx="12" fill="#f3eddc" stroke="{RULE}" stroke-width="2"/>')
        bfs = _fit(btxt, 48, W - 2 * PAD - 40, 26)
        p.append(tline(W / 2, y + (BH - 8) / 2 + bfs * 0.35, btxt, bfs, PEN))
        y += BH
    y += 42
    p.append(cap(f"تصویر شماره {num} - {title}").replace('y="0"', f'y="{y:.0f}"'))
    p.append("</svg>")
    return "".join(p)

def build_words(num, title, sub, words, judge):
    H = T1 + HD + LH + SP + TH + SP + JH + CAP_H + SP
    p = [svg_open(title, sub, H)]
    y = T1
    h, y = head_row(y, [w[1] for w in words]); p.append(h)
    s, y = letter_section(y, words, 92, form_size=20); p.append(s)
    y += SP
    s, y = practice_row(y, words, 64); p.append(s)
    y += SP
    quads = []
    for w, k in judge:
        quads.append((w, w, True, None))
        quads.append((w, w, False, k))
    s, y = judge_row(y, quads, 76); p.append(s)
    y += 50
    p.append(cap(f"تصویر شماره {num} - {title}").replace('y="0"', f'y="{y:.0f}"'))
    p.append("</svg>")
    return "".join(p)

# ── اجرا ──
FIGS = {}

def fa(n):
    return str(n).replace("0", "۰").replace("1", "۱").replace("2", "۲").replace("3", "۳").replace("4", "۴").replace("5", "۵").replace("6", "۶").replace("7", "۷").replace("8", "۸").replace("9", "۹")

LEGEND = "حروف آبی خودکاری روی خط آبی می‌نشینند؛ نارنجی سقف و قرمز کف حرکت است"

FIGS["img-03.svg"] = build_family(
    fa(3), "شکل ۳ ــ خانواده‌ی ب (ب، پ، ت، ث)",
    "چهار شکل ب و نقطه‌ها را با دقت ببینید. " + LEGEND,
    [("ب", "تنها"), ("بـ", "آغازی"), ("ـبـ", "میانی"), ("ـب", "پایانی")],
    [("پ", "پ سه‌نقطه"), ("ت", "ت دو نقطه"), ("ث", "ث سه‌نقطه")],
    [(("ب", "ب")), (("پ", "پ"))])

FIGS["img-04.svg"] = build_family(
    fa(4), "شکل ۴ ــ خانواده‌ی ج (ج، چ، ح، خ)",
    "کاسه‌ی ج باید گرد و کامل زیر خط بنشیند. " + LEGEND,
    [("ج", "تنها"), ("جـ", "آغازی"), ("ـجـ", "میانی"), ("ـج", "پایانی")],
    [("چ", "چ سه‌نقطه"), ("ح", "ح بی‌نقطه"), ("خ", "خ یک نقطه")],
    [(("ج", "ج")), (("چ", "چ"))])

FIGS["img-05.svg"] = build_family(
    fa(5), "شکل ۵ ــ خانواده‌ی د (د، ذ)",
    "قوس د بلند و رو به بالاست و به حرف بعد نمی‌چسبد. " + LEGEND,
    [("د", "تنها"), ("ـد", "پایانی")],
    [("ذ", "ذ تنها"), ("ـذ", "ذ پایانی")],
    [(("د", "د")), (("ذ", "ذ"))])

FIGS["img-06.svg"] = build_family(
    fa(6), "شکل ۶ ــ خانواده‌ی ر (ر، ز، ژ)",
    "فرود کوتاه و مایل با زاویه یکسان در هر سه حرف. " + LEGEND,
    [("ر", "تنها"), ("ـر", "پایانی")],
    [("ز", "ز یک نقطه"), ("ژ", "ژ سه‌نقطه")],
    [(("ر", "ر")), (("ژ", "ژ"))])

FIGS["img-07.svg"] = build_family(
    fa(7), "شکل ۷ ــ خانواده‌ی س (س، ش)",
    "سه دندانه‌ی س کوتاه و یکدست است. " + LEGEND,
    [("س", "تنها"), ("سـ", "آغازی"), ("ـسـ", "میانی"), ("ـس", "پایانی")],
    [("ش", "ش تنها"), ("شـ", "ش آغازی"), ("ـش", "ش پایانی")],
    [(("س", "س")), (("ش", "ش"))])

FIGS["img-08.svg"] = build_family(
    fa(8), "شکل ۸ ــ خانواده‌ی ص (ص، ض)",
    "دندان کوچک ص پیش از کاسه‌ی گرد می‌نشیند. " + LEGEND,
    [("ص", "تنها"), ("صـ", "آغازی"), ("ـصـ", "میانی"), ("ـص", "پایانی")],
    [("ض", "ض تنها"), ("ضـ", "ض آغازی"), ("ـض", "ض پایانی")],
    [(("ص", "ص")), (("ض", "ض"))])

FIGS["img-09.svg"] = build_family(
    fa(9), "شکل ۹ ــ خانواده‌ی ط (ط، ظ)",
    "قد بلند عمودی با کاسه‌ی متناسب در پایین. " + LEGEND,
    [("ط", "تنها"), ("طـ", "آغازی"), ("ـطـ", "میانی"), ("ـط", "پایانی")],
    [("ظ", "ظ تنها"), ("ظـ", "ظ آغازی"), ("ـظ", "ظ پایانی")],
    [(("ط", "ط")), (("ظ", "ظ"))])

FIGS["img-10.svg"] = build_family(
    fa(10), "شکل ۱۰ ــ خانواده‌ی ع (ع، غ)",
    "کاسه‌ی ع همیشه باز و لبخندوار است. " + LEGEND,
    [("ع", "تنها"), ("عـ", "آغازی"), ("ـعـ", "میانی"), ("ـع", "پایانی")],
    [("غ", "غ یک نقطه"), ("غـ", "غ آغازی"), ("ـغ", "غ پایانی")],
    [(("ع", "ع")), (("غ", "غ"))])

FIGS["img-11.svg"] = build_family(
    fa(11), "شکل ۱۱ ــ خانواده‌ی ف (ف، ق)",
    "سر ف کوچک و گرد و فرود آن نرم است. " + LEGEND,
    [("ف", "تنها"), ("فـ", "آغازی"), ("ـفـ", "میانی"), ("ـف", "پایانی")],
    [("ق", "ق تنها"), ("قـ", "ق آغازی"), ("ـق", "ق پایانی")],
    [(("ف", "ف")), (("ق", "ق"))])

FIGS["img-12.svg"] = build_family(
    fa(12), "شکل ۱۲ ــ خانواده‌ی ک (ک، گ)",
    "دندانه‌ی ک تیز و سر آن کشیده است. " + LEGEND,
    [("ک", "تنها"), ("کـ", "آغازی"), ("ـکـ", "میانی"), ("ـک", "پایانی")],
    [("گ", "گ تنها"), ("گـ", "گ آغازی"), ("ـگ", "گ پایانی")],
    [(("ک", "ک")), (("گ", "گ"))])

FIGS["img-13.svg"] = build_independent(
    fa(13), "شکل ۱۳ ــ حروف مستقل (ا، ل، م، ن، و، ه، ی)",
    "هر حرف ساز خودش را می‌زند؛ ه سه شکل متفاوت دارد. " + LEGEND)

FIGS["img-15.svg"] = build_checklist(
    fa(15), "شکل ۱۵ ــ چک‌لیست خودارزیابی خوانایی",
    "هر جمعه به یک نوشته‌ات از هر معیار ۰ تا ۱۰ بده و جمع بزن")

FIGS["img-16.svg"] = build_words(
    fa(16), "شکل ۱۶ ــ اتصال آغازی (بـ، سـ، مـ)",
    "حرف آغازی دستش را به جلو دراز می‌کند. " + LEGEND,
    [("با", "بـ + الف"), ("سلام", "سـ + ل"), ("مادر", "مـ + الف")],
    [("با", 1), ("سلام", 1)])

FIGS["img-17.svg"] = build_words(
    fa(17), "شکل ۱۷ ــ اتصال میانی (ـیـ، ـعـ، ـمـ)",
    "حرف میانی پلی دوطرفه است؛ از هر دو سو دست می‌دهد. " + LEGEND,
    [("نیم", "ـیـ میانی"), ("سعی", "ـعـ میانی"), ("همه", "ـمـ میانی")],
    [("نیم", 2), ("سعی", 1)])

FIGS["img-18.svg"] = build_words(
    fa(18), "شکل ۱۸ ــ اتصال پایانی (ـب، ـس، ـه)",
    "حرف پایانی با یک فرود تمیز واژه را تمام می‌کند. " + LEGEND,
    [("کتاب", "ـب پایانی"), ("درس", "ـس پایانی"), ("خانه", "ـه پایانی")],
    [("کتاب", 3), ("خانه", 3)])

FIGS["img-19.svg"] = build_words(
    fa(19), "شکل ۱۹ ــ حروف نچسب (د، ر، و)",
    "بعد از این حروف قلم را بردار؛ چسباندن آن‌ها غلط است. " + LEGEND,
    [("دریا", "حرف د"), ("نور", "حرف و"), ("روز", "حرف ر")],
    [("دریا", None), ("روز", None)])

FIGS["img-20.svg"] = build_words(
    fa(20), "شکل ۲۰ ــ لا و سه‌حرفی‌ها",
    "لا زیباترین اتصال فارسی است؛ سه‌حرفی‌ها را یک‌نفس بنویس. " + LEGEND,
    [("لاله", "لا"), ("کلاس", "ـلا میانی"), ("شیر", "سه‌حرفی")],
    [("لاله", 2), ("شیر", 2)])

FIGS["img-21.svg"] = build_words(
    fa(21), "شکل ۲۱ ــ ه و ی در اتصال",
    "اول بدنه را کامل بنویس بعد برگرد و نقطه‌ها را بگذار. " + LEGEND,
    [("ماهی", "ـهـ میانی"), ("نامه", "ـه پایانی"), ("بازی", "ـی پایانی")],
    [("ماهی", 3), ("بازی", 3)])

for name, svg in FIGS.items():
    with open(os.path.join(OUT, name), "w") as f:
        f.write(svg)
    print("wrote", name)
print("ALL SVG DONE (v4 static notebook)")
