#!/usr/bin/env python3
"""دفتر تمرین خط خوش (PDF برداری A4) — همراه درس ۰۲ خودکارنویسی.
حروف: outline برداری با شکل‌دهی uharfbuzz (بدون نیاز به فونت در چاپ).
خط‌چین تمرین: دقیقاً پهنای نوک خودکار ۰/۷ میلی‌متر.
"""
import os
import uharfbuzz as hb
import numpy as np
from skimage.morphology import skeletonize
from fontTools.ttLib import TTFont
from fontTools.pens.recordingPen import DecomposingRecordingPen
from reportlab.lib.units import mm
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas as canvmod

BK = "/home/user/fonts/Badkhat.ttf"
OUTDIR = "/home/user/آموزشگاه/files/02-handwriting"
PDF = os.path.join(OUTDIR, "daftar-tamrin.pdf")
SAMPLE = os.path.join(OUTDIR, "nemune.png")

PEN = HexColor("#1e40c9"); INK = HexColor("#2b2620"); GRAY = HexColor("#6b6252")
RULE = HexColor("#c3d2ef"); BASE = HexColor("#4a86e8")
ORANGE = HexColor("#eaa94f"); RED = HexColor("#e89393")
TRACE = HexColor("#8a93a8"); HEAD_BG = HexColor("#f3eddc")
TIP = 0.7 * mm  # پهنای نوک خودکار واقعی

PW, PH = 210 * mm, 297 * mm
M = 14 * mm
X0, X1 = M, PW - M
YTOP = PH - M
YBOT = 16 * mm
CW = X1 - X0

# ── موتور متن برداری ──
_blob = hb.Blob.from_file_path(BK); _face = hb.Face(_blob); _font = hb.Font(_face)
UPEM = _face.upem
_tt = TTFont(BK); _order = _tt.getGlyphOrder(); _gs = _tt.getGlyphSet()

def shape(text):
    buf = hb.Buffer(); buf.add_str(text)
    buf.direction = "rtl"; buf.language = "fa"; buf.script = "Arab"
    hb.shape(_font, buf, {})
    return buf.glyph_infos, buf.glyph_positions

def text_width(text, size):
    _, poss = shape(text)
    return sum(p.x_advance for p in poss) / UPEM * size

def _contours(ops):
    cons = []; cur = None
    for op, args in ops:
        if op == "moveTo":
            cur = [(op, args)]; cons.append(cur)
        elif cur is not None:
            cur.append((op, args))
    return cons

def _replay(c, ops, ox, oy, s, fill, strip, stroke, sw, dash):
    for con in _contours(ops):
        pts = []
        for op, args in con:
            if op in ("moveTo", "lineTo"):
                pts.append(args[0])
            elif op == "qCurveTo":
                pts.extend(args)
            elif op == "curveTo":
                pts.extend(args)
        if not pts:
            continue
        if strip:
            xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
            if max(max(xs) - min(xs), max(ys) - min(ys)) < 0.11 * UPEM:
                continue
        pth = c.beginPath()
        cur = None
        for op, args in con:
            if op == "moveTo":
                cur = args[0]
                pth.moveTo(ox + cur[0] * s, oy + cur[1] * s)
            elif op == "lineTo":
                cur = args[0]
                pth.lineTo(ox + cur[0] * s, oy + cur[1] * s)
            elif op == "qCurveTo":
                offs = list(args[:-1]); end = args[-1]
                seq = [(cur[0], cur[1], False)] + [(p[0], p[1], True) for p in offs] + [(end[0], end[1], False)]
                res = [seq[0]]
                for a, b in zip(seq, seq[1:]):
                    if a[2] and b[2]:
                        res.append(((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, False))
                    res.append(b)
                for k in range(0, len(res) - 2, 2):
                    p0, cc, p3 = res[k], res[k + 1], res[k + 2]
                    c1 = (p0[0] + 2 * (cc[0] - p0[0]) / 3, p0[1] + 2 * (cc[1] - p0[1]) / 3)
                    c2 = (p3[0] + 2 * (cc[0] - p3[0]) / 3, p3[1] + 2 * (cc[1] - p3[1]) / 3)
                    pth.curveTo(ox + c1[0] * s, oy + c1[1] * s, ox + c2[0] * s, oy + c2[1] * s,
                                ox + p3[0] * s, oy + p3[1] * s)
                cur = end
            elif op == "curveTo":
                c1, c2, p3 = args
                pth.curveTo(ox + c1[0] * s, oy + c1[1] * s, ox + c2[0] * s, oy + c2[1] * s,
                            ox + p3[0] * s, oy + p3[1] * s)
                cur = p3
            elif op == "closePath":
                pth.close()
        if stroke:
            c.setStrokeColor(fill); c.setLineWidth(sw); c.setLineCap(1)
            c.setDash(dash[0], dash[1]) if dash else c.setDash([])
            c.drawPath(pth, stroke=1, fill=0)
            c.setDash([])
        else:
            c.setFillColor(fill)
            c.drawPath(pth, stroke=0, fill=1)

def draw_text(c, text, x_right, base_y, size, fill, strip=False, stroke=False, sw=0, dash=None):
    infos, poss = shape(text)
    s = size / UPEM
    x = x_right - sum(p.x_advance for p in poss) * s
    for info, p in zip(infos, poss):
        rp = DecomposingRecordingPen(_gs)
        _gs[_order[info.codepoint]].draw(rp)
        _replay(c, rp.value, x + p.x_offset * s, base_y + p.y_offset * s,
                s, fill, strip, stroke, sw, dash)
        x += p.x_advance * s

def draw_centered(c, text, cx, base_y, size, fill, **kw):
    draw_text(c, text, cx + text_width(text, size) / 2, base_y, size, fill, **kw)

# ── خط میانی نقطه‌چین (اسکلت برداری هر گلیف) ──
_CLCACHE = {}
CL_PX = 300  # پیکسل رستر بر ام، برای اسکلت

def _quad(p0, p1, p2, n=10):
    for k in range(1, n + 1):
        t = k / n; u = 1 - t
        yield (u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
               u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1])

def _cubic(p0, p1, p2, p3, n=12):
    for k in range(1, n + 1):
        t = k / n; u = 1 - t
        yield (u**3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t**3 * p3[0],
               u**3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t**3 * p3[1])

def _flat_con(con):
    pts = []; cur = None
    for op, args in con:
        if op in ("moveTo", "lineTo"):
            cur = tuple(args[0]); pts.append(cur)
        elif op == "qCurveTo":
            *offs, end = [tuple(a) for a in args]
            exp = [cur]; prev = None
            for o in offs:
                if prev is not None:
                    exp.append(((prev[0] + o[0]) / 2, (prev[1] + o[1]) / 2))
                exp.append(o); prev = o
            exp.append(tuple(end))
            for k in range(0, len(exp) - 2, 2):
                pts.extend(_quad(exp[k], exp[k + 1], exp[k + 2]))
            cur = tuple(end)
        elif op == "curveTo":
            c1, c2, p3 = [tuple(a) for a in args]
            pts.extend(_cubic(cur, c1, c2, p3))
            cur = p3
    return pts

def _con_pts(con):
    pts = []
    for op, args in con:
        if op in ("moveTo", "lineTo"):
            pts.append(args[0])
        elif op in ("qCurveTo", "curveTo"):
            pts.extend(args)
    return pts

def _rasterize(polys, sc, pad=4):
    allx = [p[0] for pg in polys for p in pg]; ally = [p[1] for pg in polys for p in pg]
    minx, maxx = min(allx), max(allx); miny, maxy = min(ally), max(ally)
    W = int((maxx - minx) * sc) + 2 * pad + 1
    H = int((maxy - miny) * sc) + 2 * pad + 1
    X = lambda x: (x - minx) * sc + pad
    Y = lambda y: (maxy - y) * sc + pad
    edges = []
    for pg in polys:
        q = [(X(x), Y(y)) for x, y in pg]
        for a, b in zip(q, q[1:] + q[:1]):
            if abs(a[1] - b[1]) > 1e-9:
                edges.append((a, b))
    mask = np.zeros((H, W), bool)
    for yy in range(H):
        yc = yy + 0.5; xs = []
        for (x1, y1), (x2, y2) in edges:
            if (y1 > yc) != (y2 > yc):
                xs.append(x1 + (yc - y1) * (x2 - x1) / (y2 - y1))
        xs.sort()
        for xa, xb in zip(xs[::2], xs[1::2]):
            mask[yy, max(0, int(xa)):min(W, int(xb) + 1)] = True
    return mask, minx, maxy, sc, pad

def _walk(mask, minlen=4):
    H, W = mask.shape
    P = mask.astype(np.int8)
    nb = np.zeros((H, W), np.int8)
    nb[1:, :] += P[:-1, :]; nb[:-1, :] += P[1:, :]
    nb[:, 1:] += P[:, :-1]; nb[:, :-1] += P[:, 1:]
    nb[1:, 1:] += P[:-1, :-1]; nb[1:, :-1] += P[:-1, 1:]
    nb[:-1, 1:] += P[1:, :-1]; nb[:-1, :-1] += P[1:, 1:]
    nb *= mask
    visited = np.zeros((H, W), bool)
    lines = []
    def nbs(r, c):
        out = []
        for dr in (-1, 0, 1):
            for dc in (-1, 0, 1):
                if not dr and not dc:
                    continue
                rr, cc = r + dr, c + dc
                if 0 <= rr < H and 0 <= cc < W and mask[rr, cc]:
                    out.append((rr, cc))
        return out
    def walk_from(r, c):
        path = [(r, c)]
        if nb[r, c] <= 2:
            visited[r, c] = True
        while True:
            cands = [q for q in nbs(r, c) if not visited[q]]
            if not cands:
                break
            if len(path) > 1:
                pr, pc = path[-2]; dr, dc = r - pr, c - pc
                st = [q for q in cands if q == (r + dr, c + dc)]
                cands = st or sorted(cands, key=lambda q: -((q[0] - r) * dr + (q[1] - c) * dc))
            r, c = cands[0]; path.append((r, c))
            if nb[r, c] <= 2:
                visited[r, c] = True
            else:
                break
            if nb[r, c] == 1 and len(path) > 1:
                break
        return path
    for r in range(H):
        for c in range(W):
            if mask[r, c] and not visited[r, c] and nb[r, c] == 1:
                pl = walk_from(r, c)
                if len(pl) >= minlen:
                    lines.append(pl)
    for r in range(H):
        for c in range(W):
            if mask[r, c] and not visited[r, c] and nb[r, c] <= 2:
                pl = walk_from(r, c)
                if len(pl) >= minlen:
                    lines.append(pl)
    return lines

def _dp(pts, eps):
    if len(pts) < 3:
        return pts
    (x0, y0), (x1, y1) = pts[0], pts[-1]
    dx, dy = x1 - x0, y1 - y0
    den = (dx * dx + dy * dy) ** 0.5
    best, bi = -1, 0
    for i in range(1, len(pts) - 1):
        d = abs(dy * pts[i][0] - dx * pts[i][1] + x1 * y0 - y1 * x0) / (den or 1)
        if d > best:
            best, bi = d, i
    if best > eps:
        return _dp(pts[:bi + 1], eps)[:-1] + _dp(pts[bi:], eps)
    return [pts[0], pts[-1]]

def _glyph_centerlines(gname, strip):
    key = (gname, strip)
    if key in _CLCACHE:
        return _CLCACHE[key]
    rp = DecomposingRecordingPen(_gs)
    _gs[gname].draw(rp)
    body = []; dot_ops = []
    for con in _contours(rp.value):
        pts = _con_pts(con)
        xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
        is_dot = max(max(xs) - min(xs), max(ys) - min(ys)) < 0.11 * UPEM
        if is_dot and strip:
            continue
        if is_dot:
            dot_ops.extend(con)
        else:
            body.append(_flat_con(con))
    lines = []
    if body:
        mask, minx, maxy, sc, pad = _rasterize(body, CL_PX / UPEM)
        for pl in _walk(skeletonize(mask)):
            pl = _dp([(c, r) for r, c in pl], 1.2)
            if len(pl) >= 2:
                lines.append([((x - pad) / sc + minx, maxy - (y - pad) / sc) for x, y in pl])
    _CLCACHE[key] = (lines, dot_ops)
    return lines, dot_ops

TRACE_DASH = (1.2, 0.8)  # طول خط و فاصله (میلی‌متر)؛ (0.01, گام) = نقطه‌ای


def draw_trace(c, text, x_right, base_y, size, strip, dash=None):
    infos, poss = shape(text)
    s = size / UPEM
    x = x_right - sum(p.x_advance for p in poss) * s
    for info, p in zip(infos, poss):
        lines, dot_ops = _glyph_centerlines(_order[info.codepoint], strip)
        ox = x + p.x_offset * s; oy = base_y + p.y_offset * s
        for pl in lines:
            pth = c.beginPath()
            pth.moveTo(ox + pl[0][0] * s, oy + pl[0][1] * s)
            for q in pl[1:]:
                pth.lineTo(ox + q[0] * s, oy + q[1] * s)
            c.setStrokeColor(TRACE); c.setLineWidth(TIP); c.setLineCap(1)
            d = dash or TRACE_DASH
            c.setDash([d[0] * mm, d[1] * mm], 0)
            c.drawPath(pth, stroke=1, fill=0)
        c.setDash([])
        if dot_ops:
            _replay(c, dot_ops, ox, oy, s, TRACE, False, False, 0, None)
        x += p.x_advance * s

# ── داده‌ها ──
GROUPS = [
    ("ب پ ت ث", [("ب", "تنها"), ("بـ", "آغازی"), ("ـبـ", "میانی"), ("ـب", "پایانی")]),
    ("ج چ ح خ", [("ج", "تنها"), ("جـ", "آغازی"), ("ـجـ", "میانی"), ("ـج", "پایانی")]),
    ("د ذ", [("د", "تنها"), ("ـد", "پایانی")]),
    ("ر ز ژ", [("ر", "تنها"), ("ـر", "پایانی")]),
    ("س ش", [("س", "تنها"), ("سـ", "آغازی"), ("ـسـ", "میانی"), ("ـس", "پایانی")]),
    ("ص ض", [("ص", "تنها"), ("صـ", "آغازی"), ("ـصـ", "میانی"), ("ـص", "پایانی")]),
    ("ط ظ", [("ط", "تنها"), ("طـ", "آغازی"), ("ـطـ", "میانی"), ("ـط", "پایانی")]),
    ("ع غ", [("ع", "تنها"), ("عـ", "آغازی"), ("ـعـ", "میانی"), ("ـع", "پایانی")]),
    ("ف ق", [("ف", "تنها"), ("فـ", "آغازی"), ("ـفـ", "میانی"), ("ـف", "پایانی")]),
    ("ک گ", [("ک", "تنها"), ("کـ", "آغازی"), ("ـکـ", "میانی"), ("ـک", "پایانی")]),
]
SINGLES = [
    ("الف", [("ا", "الف تنها"), ("لا", "لا")]),
    ("لام", [("ل", "لام تنها"), ("لـ", "لام آغازی")]),
    ("میم", [("م", "تنها"), ("مـ", "آغازی"), ("ـمـ", "میانی"), ("ـم", "پایانی")]),
    ("نون", [("ن", "تنها"), ("نـ", "آغازی"), ("ـنـ", "میانی"), ("ـن", "پایانی")]),
    ("واو", [("و", "واو تنها"), ("ـو", "واو پایانی")]),
    ("ه", [("ه", "تنها"), ("هـ", "آغازی"), ("ـهـ", "میانی"), ("ـه", "پایانی")]),
    ("ی", [("ی", "تنها"), ("یـ", "آغازی"), ("ـینـ", "میانی"), ("ـی", "پایانی")]),
]
SENTS = [
    # سطح ۱ (پایه چهارم تا ششم): کوتاه و ساده
    "بابا آب داد.",
    "مادر نان تازه آورد.",
    "سارا در باغ گل کاشت.",
    "گنجشک روی درخت آواز خواند.",
    # سطح ۲ (پایه هفتم تا نهم): بلندتر
    "کتاب خوب، بهترین دوست انسان است.",
    "تلاش هر روز، ما را به هدف نزدیک می‌کند.",
    "در آسمان شب، ستاره‌ها می‌درخشند.",
    "معلم با صبر و حوصله به ما آموخت.",
    # سطح ۳ (پایه دهم تا دوازدهم): ادبی و بلند
    "خط خوش، آیینه نظم فکر و آرامش دست است.",
    "دانش، گنجی است که با بخشیدن بیشتر می‌شود.",
    "پشتکار در کارهای کوچک، راز پیروزی‌های بزرگ است.",
    "آن‌که می‌آموزد و می‌آموزاند، چراغ راه دیگران است.",
]

ROW_H = 26 * mm
DEMO_SIZE = 30
LBL_SIZE = 6.5

def footer(c, n):
    draw_centered(c, f"صفحه {n}", PW / 2, 10 * mm, 8, GRAY)

def letter_header(c, y_top, title, hint):
    c.setFillColor(HEAD_BG)
    c.setStrokeColor(RULE); c.setLineWidth(0.5)
    c.roundRect(X0, y_top - 12 * mm, CW, 12 * mm, 2 * mm, fill=1, stroke=1)
    draw_text(c, title, X1 - 4 * mm, y_top - 8 * mm, 12, PEN)
    draw_text(c, hint, X0 + text_width(hint, 8) + 4 * mm, y_top - 7.5 * mm, 8, GRAY)
    return y_top - 12 * mm

def guides(c, y_top, h, size):
    em = size * 0.3528 * mm
    base = y_top - h * 0.615
    top_g = base + em * 0.95
    low_g = base - em * 0.60
    c.setLineWidth(0.4); c.setStrokeColor(RULE)
    c.line(X0, base + em * 0.45, X1, base + em * 0.45)
    c.line(X0, base - em * 0.30, X1, base - em * 0.30)
    c.setLineWidth(0.8); c.setStrokeColor(ORANGE)
    c.line(X0, top_g, X1, top_g)
    c.setLineWidth(1.0); c.setStrokeColor(BASE)
    c.line(X0, base, X1, base)
    c.setLineWidth(0.8); c.setStrokeColor(RED)
    c.line(X0, low_g, X1, low_g)
    return base

def forms_row(c, y_top, forms, size, mode, strip):
    """mode: demo | trace | empty"""
    h = ROW_H
    base = guides(c, y_top, h, size)
    n = len(forms)
    cw = CW / n
    for i, (ch, name) in enumerate(forms):
        cx = X1 - cw * (i + 0.5)
        if mode == "demo":
            draw_centered(c, ch, cx, base, size, PEN, strip=strip)
            draw_centered(c, name, cx, y_top - h + 1.2 * mm, LBL_SIZE, GRAY)
        elif mode == "trace":
            w = text_width(ch, size)
            gap = 6 * mm
            reps = max(2, int((cw - 6 * mm) / (w + gap)))
            x = X1 - cw * i - 3 * mm
            for _ in range(reps):
                draw_trace(c, ch, x, base, size, strip)
                x -= (w + gap)
            draw_centered(c, name, cx, y_top - h + 1.2 * mm, LBL_SIZE, GRAY)
    return y_top - h

def plain_block(c, y_top):
    for k in range(3):
        yy = y_top - k * 9 * mm
        c.setLineWidth(0.5); c.setStrokeColor(RULE)
        c.line(X0, yy, X1, yy)
    return y_top - 2 * 9 * mm - 4 * mm

def cover(c):
    y = YTOP - 30 * mm
    draw_centered(c, "دفتر تمرین خط خوش", PW / 2, y, 30, PEN)
    y -= 16 * mm
    draw_centered(c, "همراه درس دوم: خودکارنویسی", PW / 2, y, 15, INK)
    y -= 20 * mm
    alpha = "ا ب پ ت ث ج چ ح خ د ذ ر ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ه ی"
    draw_centered(c, alpha, PW / 2, y, 13, PEN)
    y -= 18 * mm
    tips = [
        "۱. اول سطر نمایشی را خوب نگاه کن: جای هر حرف روی خط آبی.",
        "۲. با مداد روی خط‌چین‌ها بکش؛ خط‌چین‌ها به پهنای نوک خودکار است.",
        "۳. دو سطر خالی راهنمادار را خودت بنویس.",
        "۴. دو سطر دفتری را بدون راهنما بنویس.",
        "۵. در صفحه‌های مشق، هر جمله را شش بار از روی نمونه بنویس.",
    ]
    for t in tips:
        draw_text(c, t, X1 - 6 * mm, y, 11, INK)
        y -= 9 * mm
    y -= 8 * mm
    draw_centered(c, "آموزشگاه - درس ۲ از ۳۴", PW / 2, y, 11, GRAY)

def sentence_page(c, idx, sent):
    y = letter_header(c, YTOP, f"مشق جمله {idx + 1} از ۱۲", "از روی نمونه بنویس")
    sz = 15 * min(1.0, (CW - 6 * mm) / max(1.0, text_width(sent, 15)))
    for _ in range(6):
        y -= 2 * mm
        c.setLineWidth(0.4); c.setStrokeColor(RULE)
        c.line(X0, y - 2 * mm, X1, y - 2 * mm)
        c.setLineWidth(1.0); c.setStrokeColor(BASE)
        c.line(X0, y - 9 * mm, X1, y - 9 * mm)
        draw_text(c, sent, X1 - 3 * mm, y - 9 * mm, sz, PEN)
        y -= 14 * mm
        for _ in range(2):
            c.setLineWidth(0.4); c.setStrokeColor(RULE)
            c.line(X0, y - 2 * mm, X1, y - 2 * mm)
            c.setLineWidth(1.0); c.setStrokeColor(BASE)
            c.line(X0, y - 9 * mm, X1, y - 9 * mm)
            y -= 11 * mm

def build():
    os.makedirs(OUTDIR, exist_ok=True)
    c = canvmod.Canvas(PDF, pagesize=(PW, PH))
    c.setTitle("دفتر تمرین خط خوش - درس خودکارنویسی")
    c.setAuthor("آموزشگاه")
    n = 1
    cover(c); footer(c, n); c.showPage(); n += 1

    blocks = ([("بدنه مشترک: " + g, f, True) for g, f in GROUPS]
              + [("حرف مستقل: " + g, f, False) for g, f in SINGLES])
    HINT = "نمایشی، سپس خط‌چین، سپس خالی"
    y = YTOP
    for title, forms, strip in blocks:
        if y < YBOT + 60 * mm:
            footer(c, n); c.showPage(); n += 1; y = YTOP
        y = letter_header(c, y, title, HINT)
        for mode in ("demo", "trace", "trace", "trace", "empty", "empty"):
            if y - ROW_H < YBOT:
                footer(c, n); c.showPage(); n += 1; y = YTOP
            y = forms_row(c, y, forms, DEMO_SIZE, mode, strip)
        if y - 22 * mm < YBOT:
            footer(c, n); c.showPage(); n += 1; y = YTOP
        y = plain_block(c, y - 2 * mm)
        y -= 4 * mm
    footer(c, n); c.showPage(); n += 1

    for i, sent in enumerate(SENTS):
        sentence_page(c, i, sent)
        footer(c, n); c.showPage(); n += 1
    c.save()
    print("pages:", n - 1)
    print("PDF KB:", os.path.getsize(PDF) // 1024)

if __name__ == "__main__":
    build()
