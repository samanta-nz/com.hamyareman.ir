#!/usr/bin/env python3
"""استخراج نشانی واقعی هر فایل از دل یک APK ساخته‌شده.

فرق این اسکریپت با `list_content_urls.py` این است که آنجا از سورس مخزن
می‌خوانیم، اینجا از **باینریِ منتشرشده**. پس اگر روزی سورس و APK از هم جدا
بیفتند، همین‌جا معلوم می‌شود.

از داخل APK سه چیز بیرون کشیده می‌شود:

  ۱. assets/content/server-map.json و catalog.json  → کلید هر فایل
  ۲. استخر رشته‌های classes*.dex                     → ثابت‌های کامپایل‌شده
     (میزبان سرور داخلی، endpoint و باکت Appwrite) بدون هیچ ابزار خارجی
  ۳. هر نشانی http(s) دیگری که در باینری هاردکد شده

بعد نشانی‌ها با همان مقادیری که **در خود APK** پیدا شده‌اند بازسازی و
اختیاراً به‌صورت زنده probe می‌شوند.
"""

from __future__ import annotations

import argparse
import csv
import json
import pathlib
import re
import struct
import zipfile
from concurrent.futures import ThreadPoolExecutor

_SAFE = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-*_")

URL_RE = re.compile(rb"https?://[A-Za-z0-9._~:/?#@!$&*+,;=%-]{4,}")


def url_encoder(part: str) -> str:
    """هم‌ارز java.net.URLEncoder.encode(x,"UTF-8").replace("+","%20")."""
    out = []
    for ch in part:
        if ch in _SAFE:
            out.append(ch)
        elif ch == " ":
            out.append("%20")
        else:
            out.extend("%%%02X" % b for b in ch.encode("utf-8"))
    return "".join(out)


# ---------------------------------------------------------------- dex strings


def _uleb128(buf: bytes, pos: int) -> tuple[int, int]:
    result = 0
    shift = 0
    while True:
        b = buf[pos]
        pos += 1
        result |= (b & 0x7F) << shift
        if not b & 0x80:
            return result, pos
        shift += 7


def dex_strings(buf: bytes) -> list[str]:
    """خواندن دقیق string_ids از هدر DEX؛ بدون وابستگی بیرونی."""
    if buf[:4] != b"dex\n":
        raise ValueError("DEX magic نیست")
    count, offset = struct.unpack_from("<II", buf, 56)
    out: list[str] = []
    for i in range(count):
        (data_off,) = struct.unpack_from("<I", buf, offset + 4 * i)
        _, pos = _uleb128(buf, data_off)          # طول UTF-16، لازم نداریم
        end = buf.index(b"\x00", pos)
        out.append(buf[pos:end].decode("utf-8", "replace"))
    return out


def raw_strings(buf: bytes) -> list[str]:
    """تور پشتیبان: هر نشانی http(s) در بایت‌های خام."""
    return [m.group(0).decode("utf-8", "replace") for m in URL_RE.finditer(buf)]


# ------------------------------------------------------------------ constants


def pick(strings: list[str], pattern: str) -> list[str]:
    rx = re.compile(pattern)
    return sorted({s for s in strings if rx.search(s)})


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apk", required=True)
    ap.add_argument("--out-md", default="ci-report/apk-urls.md")
    ap.add_argument("--out-csv", default="ci-report/apk-urls.csv")
    ap.add_argument("--probe", action="store_true", help="هر نشانی را زنده تست کن")
    ap.add_argument("--workers", type=int, default=12)
    args = ap.parse_args()

    apk = pathlib.Path(args.apk)
    zf = zipfile.ZipFile(apk)
    names = zf.namelist()

    # ۱) دارایی‌های نگاشت، مستقیم از داخل APK
    smap = json.loads(zf.read("assets/content/server-map.json"))
    catalog = json.loads(zf.read("assets/content/catalog.json"))

    # ۲) رشته‌های کامپایل‌شده
    dex_names = sorted(n for n in names if re.fullmatch(r"classes\d*\.dex", n))
    strings: list[str] = []
    dex_report: list[str] = []
    for n in dex_names:
        buf = zf.read(n)
        try:
            s = dex_strings(buf)
            dex_report.append(f"`{n}` — {len(buf):,} بایت، {len(s):,} رشته (استخر DEX)")
        except Exception as exc:  # noqa: BLE001
            s = raw_strings(buf)
            dex_report.append(f"`{n}` — {len(buf):,} بایت، اسکن خام ({exc})")
        strings.extend(s)
    strings.extend(raw_strings(zf.read("resources.arsc")) if "resources.arsc" in names else [])

    internal_hosts = pick(strings, r"parspack|arvanstorage|\.ir/?$")
    appwrite = pick(strings, r"cloud\.appwrite\.io")
    all_urls = sorted({s for s in strings if s.startswith(("http://", "https://"))})

    # مقادیری که نشانی‌ها با آن‌ها ساخته می‌شوند — از خود APK
    internal_public = next(
        (s for s in strings if re.fullmatch(r"https://[a-z0-9.-]*parspack\.net", s)),
        next((s for s in strings if "arvanstorage" in s and s.startswith("https://")), ""),
    )
    endpoint = next((s for s in appwrite if s.endswith("/v1")), "")
    # باکت‌ها را از الگوی URL خارجی جمع می‌کنیم. باکت محتوای جاری شناسهٔ hex
    # اپ‌رایت است؛ نام‌های خوانا (مثل wellness-media) میراث endpoint قدیمی‌اند،
    # پس اولویت با شناسهٔ hex است و بقیه جدا گزارش می‌شوند.
    found_buckets = []
    for u in all_urls:
        m = re.search(r"buckets/([^/]+)/files", u)
        if m and m.group(1) not in found_buckets:
            found_buckets.append(m.group(1))
    hex_buckets = [b for b in found_buckets if re.fullmatch(r"6[a-f0-9]{15,}", b)]
    bucket = next(iter(hex_buckets), "")
    if not bucket:
        bucket = next(iter(pick(strings, r"^6[a-f0-9]{15,}$")), "")
        if not bucket and found_buckets:
            bucket = found_buckets[0]
    legacy_buckets = [b for b in found_buckets if b != bucket]

    # شناسهٔ پروژهٔ Appwrite: رشتهٔ hex بیست‌کاراکتری که باکت نیست.
    hex20 = [s for s in sorted(set(strings)) if re.fullmatch(r"6[a-f0-9]{19}", s)]
    project = next((h for h in hex20 if h != bucket), "")
    ext_base = next(
        (u for u in all_urls if re.search(r"/storage/buckets/[^/]+/files/?$", u) and bucket and bucket in u),
        f"{endpoint}/storage/buckets/{bucket}/files/" if endpoint and bucket else "",
    )

    # ۲) مسیرهای کش روی دستگاه — رشته‌های واقعی داخل باینری
    cache_markers = [
        "html-cipher-cache", ".hmk1", "media/pdf-cache/", "media-vault",
        "secure-media", "hamyar-tools", "hamyar-tools-plain", "notes_gallery",
        "admin-pdf-", "safe-plain-",
    ]
    cache_found = [m for m in cache_markers if m in set(strings)]

    titles = {i["id"]: i["title"] for i in catalog["items"]}
    cat_keys = {i["aw"]: i["key"] for i in catalog["items"]}

    rows = []
    for e in smap["entries"]:
        key = e["key"]
        low = key.lower()
        kind = "PDF" if low.endswith(".pdf") else "HTML" if low.endswith((".html", ".htm")) else \
               "MP3" if low.endswith(".mp3") else "JPG" if low.endswith((".jpg", ".jpeg", ".png")) else "OTHER"
        fid = e["id"]
        internal = internal_public + "/" + "/".join(url_encoder(p) for p in key.split("/"))
        rows.append(
            {
                "kind": kind,
                "folder": "/".join(key.split("/")[:-1]),
                "file": key.split("/")[-1],
                "id": fid,
                "title": titles.get(fid, ""),
                "size": int(e.get("size") or 0),
                "key": key,
                "catalogKey": cat_keys.get(fid, ""),
                "url": internal,
                "externalUrl": (ext_base.rstrip("/") + "/" + url_encoder(fid) +
                                "/view?project=" + project) if ext_base else "",
                "status": "",
                "realSize": "",
            }
        )
    rows.sort(key=lambda r: (r["kind"], r["folder"], r["file"]))

    # ۳) probe زنده
    bad = 0
    if args.probe:
        import requests  # فقط وقتی لازم است

        def check(r: dict) -> None:
            try:
                resp = requests.get(r["url"], headers={"Range": "bytes=0-1"}, timeout=25)
                r["status"] = str(resp.status_code)
                cr = resp.headers.get("Content-Range", "")
                r["realSize"] = cr.rsplit("/", 1)[1] if "/" in cr else resp.headers.get("Content-Length", "")
            except Exception as exc:  # noqa: BLE001
                r["status"] = type(exc).__name__

        with ThreadPoolExecutor(max_workers=args.workers) as pool:
            list(pool.map(check, rows))
        bad = sum(1 for r in rows if r["status"] not in ("200", "206") or
                  (r["realSize"].isdigit() and r["size"] and int(r["realSize"]) != r["size"]))

    pathlib.Path(args.out_csv).parent.mkdir(parents=True, exist_ok=True)
    with open(args.out_csv, "w", encoding="utf-8-sig", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)

    def human(n: int) -> str:
        return f"{n / 1048576:.1f} MB" if n >= 1048576 else f"{n / 1024:.0f} KB"

    md: list[str] = []
    md.append("# نشانی واقعی فایل‌ها — استخراج‌شده از خود APK\n")
    md.append(f"فایل: `{apk.name}` — {apk.stat().st_size:,} بایت\n")
    md.append("این گزارش از باینری منتشرشده ساخته شده، نه از سورس مخزن.\n")

    md.append("## ثابت‌های کامپایل‌شده در APK\n")
    md.append("| مورد | مقدار داخل APK |")
    md.append("|---|---|")
    md.append(f"| سرور داخلی (`INTERNAL_PUBLIC`) | `{internal_public or '—'}` |")
    md.append(f"| endpoint خارجی | `{endpoint or '—'}` |")
    md.append(f"| باکت محتوا | `{bucket or '—'}` |")
    if legacy_buckets:
        md.append("| باکت‌های دیگری که در باینری دیده شد | " +
                  "، ".join(f"`{b}`" for b in legacy_buckets) + " |")
    md.append(f"| شناسهٔ پروژهٔ خارجی | `{project or '—'}` |")
    md.append("")
    md.append("### قالب نشانی، همان‌طور که در APK ساخته می‌شود\n")
    md.append("```")
    md.append(f"داخلی : {internal_public}/<کلید، هر بخش URL-encode شده>")
    md.append(f"خارجی : {ext_base}<شناسهٔ فایل>/view?project={project}")
    md.append("```")
    md.append("")
    md.append("### کجا روی گوشی کش می‌شود\n")
    md.append("رشته‌های زیر عیناً در باینری هستند:\n")
    md.append("| مسیر کش | چه چیزی |")
    md.append("|---|---|")
    notes = {
        "html-cipher-cache": "HTMLهای درسی — `filesDir/html-cipher-cache/<sha256(fileId|url)[:32]>.hmk1`، فقط ciphertext، TTL شش ساعت",
        ".hmk1": "پسوند فایل کش رمزشدهٔ HTML",
        "media/pdf-cache/": "PDFها — `filesDir/media/pdf-cache/<fileId>`",
        "media-vault": "رسانهٔ شخصی کاربر",
        "secure-media": "گالری امن",
        "hamyar-tools": "HTML ابزارها",
        "hamyar-tools-plain": "نسخهٔ رمزگشایی‌شدهٔ موقت ابزارها در `cacheDir`",
        "notes_gallery": "جزوه‌های شخصی",
        "admin-pdf-": "پیش‌نمایش PDF در اپ ادمین",
        "safe-plain-": "فایل موقت فضای امن",
    }
    for mk in cache_found:
        md.append(f"| `{mk}` | {notes.get(mk, '')} |")
    if not cache_found:
        md.append("| — | هیچ‌کدام از نشانه‌های کش در رشته‌ها پیدا نشد |")
    md.append("")
    md.append("نکتهٔ مهم: نام فایل کش HTML از **خودِ نشانی** ساخته می‌شود")
    md.append("(`sha256(\"fileId|url\")`), پس هر تغییر آدرس، کل کش آن فایل‌ها را")
    md.append("باطل می‌کند و دوباره دانلود می‌شوند.")
    md.append("")
    md.append("### آیا اثری از آروان مانده؟\n")
    arvan = [s for s in strings if "arvan" in s.lower() or "ir-thr" in s.lower() or "hamyar-e-man" in s.lower()]
    if arvan:
        md.append("بله — این رشته‌ها هنوز در باینری هستند:\n")
        for s in sorted(set(arvan)):
            md.append(f"- `{s}`")
    else:
        md.append("**نه.** هیچ رشته‌ای شامل `arvan` / `ir-thr` / `hamyar-e-man` در")
        md.append("باینری نیست؛ سرور داخلی کاملاً پارس‌پک است.")
    md.append("")

    md.append("### همهٔ نشانی‌های هاردکد در باینری\n")
    md.append("| نشانی | توضیح |")
    md.append("|---|---|")
    for u in all_urls:
        note = ""
        if "fra.cloud.appwrite.io" in u:
            note = "⚠️ endpoint قدیمی"
        elif "parspack" in u:
            note = "سرور داخلی"
        elif "sgp.cloud.appwrite.io" in u:
            note = "سرور خارجی جاری"
        md.append(f"| `{u}` | {note} |")
    md.append("")
    md.append("بخش‌های DEX: " + "؛ ".join(dex_report) + "\n")

    kinds: dict[str, list[dict]] = {}
    for r in rows:
        kinds.setdefault(r["kind"], []).append(r)
    md.append("## جمع‌بندی فایل‌ها\n")
    md.append("| نوع | تعداد | حجم |" + (" سالم |" if args.probe else ""))
    md.append("|---|---:|---:|" + ("---:|" if args.probe else ""))
    for k, items in sorted(kinds.items()):
        line = f"| {k} | {len(items)} | {human(sum(i['size'] for i in items))} |"
        if args.probe:
            ok = sum(1 for i in items if i["status"] in ("200", "206"))
            line += f" {ok}/{len(items)} |"
        md.append(line)
    md.append("")

    mismatch = [r for r in rows if r["catalogKey"] and r["catalogKey"] != r["key"]]
    md.append("## ناسازگاری catalog.json با server-map.json\n")
    if mismatch:
        md.append("| شناسه | کلید در catalog | کلید در server-map |")
        md.append("|---|---|---|")
        for r in mismatch:
            md.append(f"| `{r['id']}` | `{r['catalogKey']}` | `{r['key']}` |")
    else:
        md.append("موردی نیست — هر شناسه‌ای که در هر دو فایل هست، کلید یکسان دارد.")
    md.append("")

    for k, items in sorted(kinds.items()):
        md.append(f"## {k} — {len(items)} فایل\n")
        head = "| # | شناسه | عنوان | حجم | نشانی واقعی |"
        sep = "|---:|---|---|---:|---|"
        if args.probe:
            head += " وضعیت |"
            sep += "---|"
        md.append(head)
        md.append(sep)
        for n, r in enumerate(items, 1):
            line = (
                f"| {n} | `{r['id']}` | {r['title'] or r['file']} | "
                f"{human(r['size'])} | <{r['url']}> |"
            )
            # نشانی خارجی در CSV هست؛ در جدول تکرار نمی‌شود تا خوانا بماند.
            if args.probe:
                okmark = "✅" if r["status"] in ("200", "206") else "❌"
                line += f" {okmark} {r['status']} |"
            md.append(line)
        md.append("")

    pathlib.Path(args.out_md).write_text("\n".join(md), encoding="utf-8")
    print(f"apk={apk.name} objects={len(rows)} internal={internal_public} bad={bad}")
    return 1 if (args.probe and bad) else 0


if __name__ == "__main__":
    raise SystemExit(main())
