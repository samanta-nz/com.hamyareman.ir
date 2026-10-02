# -*- coding: utf-8 -*-
"""استخراج متن گویندگی ۶ بخش از HTML + تمیزکاری برای TTS + تکه‌تکه کردن ≤۱۴۰۰ نویسه"""
import json, re, os, sys

TAG = sys.argv[1] if len(sys.argv) > 1 else "01-speed-reading"
HTML = sys.argv[2] if len(sys.argv) > 2 else "01-speed-reading.html"
SRC_HTML = f"/home/user/آموزشگاه/{HTML}"
OUT_DIR = f"/home/user/آموزشگاه/audio/{TAG}/narration"
os.makedirs(OUT_DIR, exist_ok=True)

FA_D = "۰۱۲۳۴۵۶۷۸۹"
ONES = ["صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
        "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده",
        "هجده", "نوزده"]
TENS = ["", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود"]

def num2fa(n):
    if n < 20: return ONES[n]
    t, o = divmod(n, 10)
    return TENS[t] if o == 0 else f"{TENS[t]} و {ONES[o]}"

def fa_digits_to_words(m):
    n = int("".join(str(FA_D.index(c)) for c in m.group(0)))
    return num2fa(n) if n < 100 else m.group(0)

def clean(html):
    t = re.sub(r"<img[^>]*>", " ", html)
    t = t.replace("</span>", " ").replace("<li>", " ").replace("</li>", ". ")
    t = t.replace("</p>", "\n").replace("</figcaption>", "\n")
    t = re.sub(r"<[^>]+>", " ", t)
    t = t.replace("(Pacer)", "")
    for en, fa in [("Home Row", "ردیف خانه"), ("Caps Lock", "کپس‌لاک"),
                   ("Num Lock", "نام‌لاک"), ("Backspace", "بک‌اسپیس"),
                   ("Shift", "شیفت"), ("Space", "فاصله"), ("Enter", "اینتر"),
                   ("Ctrl", "کنترل"), ("Tab", "تب"), ("WPM", "دابلیو‌پی‌ام"),
                   ("Cornell", "کرنل")]:
        t = t.replace(en, fa)
    t = t.replace("+", " و ")
    t = t.replace("(فاصله)", "")
    t = t.replace("(کرنل)", "")
    t = t.replace(";", "")
    for en, fa in {"A": "آ", "B": "بِ", "C": "سی", "D": "دی", "E": "ای",
                   "F": "اف", "G": "گِ", "H": "اچ", "I": "آی", "J": "جی",
                   "K": "کِی", "L": "ال", "M": "ام", "N": "ان", "O": "اُ",
                   "P": "پی", "Q": "کیو", "R": "آر", "T": "تی", "U": "یو",
                   "V": "وی", "W": "دابلیو", "X": "اکس", "Y": "وای"}.items():
        t = re.sub(r"\b%s\b" % en, fa, t)
    t = re.sub(r"\bS\b", "اس", t)
    t = re.sub(r"\bZ\b", "زد", t)
    t = re.sub(r"\btype\b", "تایپ", t)
    _lm = {"a": "آ", "b": "بِ", "c": "سی", "d": "دی", "e": "ای",
           "f": "اف", "g": "گِ", "h": "اچ", "i": "آی", "j": "جی",
           "k": "کِی", "l": "ال", "m": "ام", "n": "ان", "o": "اُ",
           "p": "پی", "q": "کیو", "r": "آر", "s": "اس", "t": "تی",
           "u": "یو", "v": "وی", "w": "دابلیو", "x": "اکس", "y": "وای",
           "z": "زد"}
    t = re.sub(r"\b[a-z]{2,5}\b", lambda m: " ".join(_lm[c] for c in m.group(0)), t)
    t = re.sub(r"[۰۱۲۳۴۵۶۷۸۹]+", fa_digits_to_words, t)
    t = re.sub(r"[0-9]+", lambda m: num2fa(int(m.group(0))) if int(m.group(0)) < 100 else m.group(0), t)
    t = re.sub(r"[ \t]+", " ", t)
    t = re.sub(r"\s*\n\s*", " ", t).strip()
    t = re.sub(r"\s+([.؟!،؛:])", r"\1", t)
    t = re.sub(r"\s{2,}", " ", t)
    return t

def chunkify(text, limit=1490):
    sents = re.split(r"(?<=[.؟!])\s+", text)
    chunks, cur = [], ""
    for s in sents:
        if len(cur) + len(s) + 1 <= limit:
            cur = (cur + " " + s).strip()
        else:
            if cur: chunks.append(cur)
            while len(s) > limit:  # جمله خیلی بلند: شکستن از روی ویرگول
                cut = s.rfind("،", 0, limit)
                cut = cut + 1 if cut > 0 else limit
                chunks.append(s[:cut].strip()); s = s[cut:].strip()
            cur = s
    if cur: chunks.append(cur)
    return chunks

with open(SRC_HTML, encoding="utf-8") as f:
    html = f.read()
start = html.index("const SECTIONS = ") + len("const SECTIONS = ")
end = html.index(";\nconst KEY")
sections = json.loads(html[start:end])

ORD = ["اول", "دوم", "سوم", "چهارم", "پنجم", "ششم", "هفتم", "هشتم", "نهم", "دهم", "یازدهم", "دوازدهم", "سیزدهم", "چهاردهم", "پانزدهم"]
manifest, total = [], 0
for i, s in enumerate(sections, 1):
    heading = clean(s["h"])
    body = clean(s["b"])
    latin = sorted(set(re.findall(r"[A-Za-z]+", heading + " " + body)))
    if latin: print(f"! sec{i} latin leftovers: {latin}")
    full = f"بخش {ORD[i-1]}: {heading}. {body}"
    chunks = chunkify(full)
    with open(f"{OUT_DIR}/sec{i}.txt", "w", encoding="utf-8") as f:
        f.write(full + "\n")
    for j, c in enumerate(chunks, 1):
        with open(f"{OUT_DIR}/sec{i}_part{j}.txt", "w", encoding="utf-8") as f:
            f.write(c)
    manifest.append({"section": i, "heading": heading, "chars": len(full), "chunks": len(chunks)})
    total += len(full)
    print(f"sec{i}: {len(full)} chars → {len(chunks)} chunks")

with open(f"{OUT_DIR}/manifest.json", "w", encoding="utf-8") as f:
    json.dump({"total_chars": total, "sections": manifest}, f, ensure_ascii=False, indent=1)
print(f"TOTAL: {total} chars, {sum(m['chunks'] for m in manifest)} chunks")
