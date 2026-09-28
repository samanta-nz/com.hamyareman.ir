#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
همگام‌سازیِ «فهرستِ زمان‌بندیِ صوتِ تدریس» (Books/.../04- صوت تدریس/<درس>.txt)
با لینک‌های درون‌متنیِ HTML همان درس در assets.

چرا لازم است: تعدادِ آیتم‌های .txt با تعدادِ آیتم‌های فهرستِ HTML یکی نیست
(«بخش صفر: مقدمه»، «استراحت» و گاهی «جمع‌بندی» در HTML جایی ندارند)، پس
نگاشتِ جایگاهی غلط می‌شود. این اسکریپت با «همترازیِ یکنواِ عنوان‌ها» زمانِ هر
آیتمِ فهرست را پیدا می‌کند و `data-seek-ms` می‌زند.

خروجی: HTML به‌روز + چاپِ آرایهٔ زمان‌ها برای `TeachSeekMap`.
"""
import io, re, sys, unicodedata

BOOKS = "Books/Base-09/ریاضی/04- صوت تدریس"
ASSETS = "apps/hamyar-app/src/main/assets/math/c905"

FA_DIGITS = str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")
WORD_NUM = {"صفر": "0", "یک": "1", "دو": "2", "سه": "3", "چهار": "4", "پنج": "5",
            "شش": "6", "هفت": "7", "هشت": "8", "نه": "9", "ده": "10"}
STOP = {"و", "با", "در", "از", "که", "ها", "ی", "یک", "the"}


def fa_to_en(s):
    return s.translate(FA_DIGITS).replace("ٔ", "").replace("\u200c", " ")


def parse_times(path):
    """فهرستِ (میلی‌ثانیه, عنوان) از فایلِ .txt — ارقامِ فارسی و فاصلهٔ اطرافِ دونقطه قبول است."""
    out = []
    for line in io.open(path, encoding="utf-8"):
        line = fa_to_en(line.strip())
        if not line:
            continue
        m = re.search(r"(\d{1,3})\s*:\s*(\d{1,2})(?!\d)", line)
        if not m:
            continue
        ms = (int(m.group(1)) * 60 + int(m.group(2))) * 1000
        title = re.sub(r"^\s*\d+\s*[.\-)]\s*", "", line)          # «۱.»
        title = title.split("—")[-1].split("-")[-1].strip() if "—" in title else title
        title = re.sub(r"^\d+\s*[:.]?\s*", "", title).strip()
        out.append((ms, title))
    return out


def norm(title):
    t = fa_to_en(title)
    t = unicodedata.normalize("NFKC", t)
    t = t.replace("‌", " ").replace("ـ", "")
    t = re.sub(r"[^\w\s]", " ", t)
    words = []
    for w in t.split():
        w = WORD_NUM.get(w, w)
        if w in STOP or len(w) < 2:
            continue
        words.append(w)
    return set(words)


def score(a, b):
    A, B = norm(a), norm(b)
    if not A or not B:
        return 0.0
    inter = len(A & B)
    return inter / max(1, min(len(A), len(B)))


def align(times, toc_titles):
    """هر آیتمِ فهرستِ HTML را به اولین آیتمِ .txt با شباهتِ کافی (و رو به جلو) نگاشت می‌کند."""
    res, cursor = [], 0
    for title in toc_titles:
        best, best_i = 0.0, -1
        for i in range(cursor, len(times)):
            s = score(title, times[i][1])
            if s > best:
                best, best_i = s, i
        if best_i >= 0 and best >= 0.34:
            res.append((times[best_i][0], title, times[best_i][1], round(best, 2)))
            cursor = best_i + 1
        else:
            res.append((None, title, "", round(best, 2)))

    # پاسِ دوم: اگر آخرین آیتمِ فهرست بی‌نگاشت ماند و دقیقاً یک زمانِ مصرف‌نشده
    # باقی بود، همان را می‌گیرد («جدول خلاصه و نکات امتحانی» ↔ «جمع‌بندی» —
    # عنوان‌ها هیچ واژهٔ مشترکی ندارند ولی هر دو آخرین بندِ درس‌اند).
    if res and res[-1][0] is None and cursor < len(times):
        ms, src = times[cursor]
        res[-1] = (ms, res[-1][1], src + " [پایانی]", 0.0)
    return res


# فقط داخلِ بلوکِ «.toc» — لینک‌های درونِ متن دست‌نخورده می‌مانند.
TOC_BLOCK = re.compile(r'<div class="toc">.*?</div>', re.S)
# هم `<li><a href="#sec1">` (درس‌ها) و هم `<a href="#lesson1" class="toc-item">` (جمع‌بندی).
TOC_ITEM = re.compile(
    r'(?P<pre><li>\s*)?<a\s+href="(?P<href>#[A-Za-z0-9_-]+)"(?P<attrs>[^>]*)>(?P<inner>.*?)</a>',
    re.S,
)
T_SPAN = re.compile(r'\s*<span class="t">.*?</span>', re.S)


def label(ms):
    """«۱:۰۵» — همان قالبِ برچسبِ زمان در خودِ شیم."""
    digits = str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹")
    m, sec = divmod(int(ms) // 1000, 60)
    return f"{m}:{sec:02d}".translate(digits)


def toc_block(html):
    m = TOC_BLOCK.search(html)
    return (m.start(), m.end(), m.group(0)) if m else None


def toc_titles(html):
    b = toc_block(html)
    if not b:
        return []
    return [T_SPAN.sub("", m.group("inner")).strip() for m in TOC_ITEM.finditer(b[2])]


def inject(html, ms_by_index):
    """`data-seek-ms` و برچسبِ زمانِ هر آیتمِ فهرست را (دوباره) می‌نویسد."""
    b = toc_block(html)
    if not b:
        return html, 0
    idx = [-1]

    def repl(m):
        idx[0] += 1
        ms = ms_by_index[idx[0]] if idx[0] < len(ms_by_index) else None
        title = T_SPAN.sub("", m.group("inner")).strip()
        # بقیهٔ صفت‌ها (مثل class="toc-item") نگه داشته می‌شوند.
        rest = re.sub(r'\s*data-seek-ms="\d+"', "", m.group("attrs")).rstrip()
        attr = f' data-seek-ms="{ms}"' if ms else ""
        lab = f' <span class="t">{label(ms)}</span>' if ms else ""
        pre = m.group("pre") or ""
        return f'{pre}<a href="{m.group("href")}"{rest}{attr}>{title}{lab}</a>'

    block = TOC_ITEM.sub(repl, b[2])
    return html[:b[0]] + block + html[b[1]:], idx[0] + 1


def pack_of(stem):
    """`ryazif02d01` → `C905_E02-L01`؛ `ryazif01review` → `C905_E01-SUM`."""
    m = re.match(r"ryazif(\d{2})d(\d{2})$", stem)
    if m:
        return f"C905_E{int(m.group(1)):02d}-L{int(m.group(2)):02d}"
    m = re.match(r"ryazif(\d{2})review$", stem)
    if m:
        return f"C905_E{int(m.group(1)):02d}-SUM"
    return None


def main(argv):
    import json, os
    os.chdir(os.path.join(os.path.dirname(__file__), "..", ".."))
    only = set(argv)
    expected = {}
    for f in sorted(os.listdir(BOOKS)):
        if not re.match(r"ryazif.+\.txt$", f):
            continue
        stem = f[:-4]
        html_path = f"{ASSETS}/{stem}.html"
        if not os.path.exists(html_path):
            continue
        times = parse_times(f"{BOOKS}/{f}")
        html = io.open(html_path, encoding="utf-8").read()
        titles = toc_titles(html)
        rows = align(times, titles)
        ms_list = [ms for ms, *_ in rows if ms]
        expected[stem] = {"pack": pack_of(stem), "ms": ms_list,
                          "titles": [t for ms, t, _, _ in rows if ms]}
        if not titles:
            print(f"⚠ {stem}: ساختارِ فهرست شناخته نشد (بدونِ تغییر) — {len(times)} زمان در .txt")
            continue
        if only and stem not in only:
            continue
        if len(ms_list) != len(titles):
            print(f"⚠ {stem}: {len(titles) - len(ms_list)} آیتمِ فهرست بی‌نگاشت ماند — ننوشتم.")
            continue
        new, n = inject(html, [ms for ms, *_ in rows])
        if n != len(titles):
            print(f"⚠ {stem}: تعدادِ جای‌گذاری ({n}) ≠ آیتم‌های فهرست ({len(titles)}) — ننوشتم.")
            continue
        if new != html:
            io.open(html_path, "w", encoding="utf-8").write(new)
            print(f"✓ {stem}: {n} لینکِ data-seek-ms نوشته شد → {ms_list}")
        else:
            print(f"= {stem}: همگام بود ({n} لینک)")
    out = "tools/seek-shim/expected-seek.json"
    io.open(out, "w", encoding="utf-8").write(
        json.dumps(expected, ensure_ascii=False, indent=1, sort_keys=True) + "\n")
    print(f"\n✓ {out} با {len(expected)} درس نوشته شد (منبعِ انتظارِ تست‌های JVM و jsdom).")


if __name__ == "__main__":
    main(sys.argv[1:])
