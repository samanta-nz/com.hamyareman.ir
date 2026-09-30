#!/usr/bin/env python3
"""ساخت فهرست نشانی همهٔ فایل‌های HTML و PDF محتوای اپ.

منبع حقیقت، دو دارایی‌ای است که خودِ اپ در زمان اجرا می‌خواند:

  * assets/content/server-map.json  → کلید قطعی هر فایل روی سرور داخلی
  * assets/content/catalog.json     → عنوان فارسی و دستهٔ فایل‌های UI

نشانی‌ها دقیقاً با همان منطقی ساخته می‌شوند که در
`ui/study/ServerResolver.kt` هست، تا خروجی این اسکریپت با چیزی که روی گوشی
باز می‌شود بایت‌به‌بایت یکی باشد:

  internal(key) = INTERNAL_PUBLIC + "/" + هر بخش مسیر با URLEncoder و بعد +→%20
  external(id)  = <endpoint>/storage/buckets/<bucket>/files/<id>/view?project=<p>

خروجی: یک Markdown خوانا و یک CSV برای مصرف ماشینی.
"""

from __future__ import annotations

import argparse
import csv
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "apps/hamyar-app/src/main/assets/content"
RESOLVER = ROOT / "apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/ServerResolver.kt"

# بخش‌های ثابت external() — از خود ServerResolver خوانده می‌شوند تا اگر عوض
# شدند این فهرست بی‌سروصدا کهنه نشود.
DEFAULTS = {
    "INTERNAL_PUBLIC": "https://c539776.parspack.net",
    "EXTERNAL_ENDPOINT": "https://sgp.cloud.appwrite.io/v1",
    "EXTERNAL_PROJECT": "6abb134a002025222005",
    "EXTERNAL_BUCKET": "6abb564d00155cc56d65",
}

# کاراکترهایی که java.net.URLEncoder دست‌نخورده رها می‌کند.
_SAFE = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-*_")


def url_encoder(part: str) -> str:
    """هم‌ارز دقیق URLEncoder.encode(part,"UTF-8").replace("+","%20")."""
    out = []
    for ch in part:
        if ch in _SAFE:
            out.append(ch)
        elif ch == " ":
            out.append("%20")
        else:
            out.extend("%%%02X" % b for b in ch.encode("utf-8"))
    return "".join(out)


def read_resolver_consts() -> dict[str, str]:
    values = dict(DEFAULTS)
    if RESOLVER.is_file():
        text = RESOLVER.read_text(encoding="utf-8")
        for name in values:
            m = re.search(rf'{name}\s*=\s*"([^"]+)"', text)
            if m:
                values[name] = m.group(1)
    return values


def human(n: int) -> str:
    if n <= 0:
        return "—"
    mb = n / (1024 * 1024)
    return f"{mb:.1f} MB" if mb >= 1 else f"{n / 1024:.0f} KB"


FOLDER_TITLES = {
    "PDF ها": "کتاب‌ها و جزوه‌های PDF",
    "common/mirror": "آینهٔ محتوای مشترک (همان HTMLهای بالا، مسیر دوم)",
    "grades/grade9/study": "HTMLهای درسی پایهٔ نهم",
    "html ها/آزمایشگاه": "آزمایشگاه",
    "html ها/آموزشگاه": "آموزشگاه مهارت",
    "html ها/تمرینات تنفسی": "تمرین‌های تنفسی",
    "html ها/جعبه ابزار ریاضی": "جعبه‌ابزار ریاضی",
    "html ها/جعبه ابزار عمومی": "جعبه‌ابزار عمومی",
    "html ها/حرکات ورزشی": "حرکات ورزشی",
    "html ها/یوگا": "یوگا",
}


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-md", default="docs/maintenance/grade9/نشانی-html-و-pdf.md")
    ap.add_argument("--out-csv", default="docs/maintenance/grade9/نشانی-html-و-pdf.csv")
    args = ap.parse_args()

    c = read_resolver_consts()
    smap = json.loads((ASSETS / "server-map.json").read_text(encoding="utf-8"))
    catalog = json.loads((ASSETS / "catalog.json").read_text(encoding="utf-8"))

    titles = {i["id"]: i["title"] for i in catalog["items"]}
    cat_of = {i["id"]: i["cat"] for i in catalog["items"]}
    cat_titles = {x["id"]: x["title"] for x in catalog["categories"]}

    rows = []
    for e in smap["entries"]:
        key = e["key"]
        low = key.lower()
        if not low.endswith((".html", ".htm", ".pdf")):
            continue
        fid = e["id"]
        internal = c["INTERNAL_PUBLIC"] + "/" + "/".join(url_encoder(p) for p in key.split("/"))
        external = (
            f"{c['EXTERNAL_ENDPOINT']}/storage/buckets/{c['EXTERNAL_BUCKET']}"
            f"/files/{url_encoder(fid)}/view?project={c['EXTERNAL_PROJECT']}"
        )
        rows.append(
            {
                "kind": "PDF" if low.endswith(".pdf") else "HTML",
                "folder": "/".join(key.split("/")[:-1]),
                "file": key.split("/")[-1],
                "id": fid,
                "title": titles.get(fid, ""),
                "cat": cat_titles.get(cat_of.get(fid, ""), ""),
                "size": int(e.get("size") or 0),
                "key": key,
                "internal": internal,
                "external": external,
            }
        )

    rows.sort(key=lambda r: (r["folder"], r["file"]))

    with open(ROOT / args.out_csv, "w", encoding="utf-8-sig", newline="") as fh:
        w = csv.DictWriter(
            fh,
            fieldnames=["kind", "folder", "file", "id", "title", "cat", "size", "key", "internal", "external"],
        )
        w.writeheader()
        w.writerows(rows)

    html_n = sum(1 for r in rows if r["kind"] == "HTML")
    pdf_n = sum(1 for r in rows if r["kind"] == "PDF")
    total = sum(r["size"] for r in rows)

    md: list[str] = []
    md.append("# نشانی همهٔ HTMLها و PDFهای اپ\n")
    md.append(
        "این فهرست خودکار از دو دارایی خود اپ ساخته شده است "
        "(`assets/content/server-map.json` و `assets/content/catalog.json`) و "
        "نشانی‌ها با همان کدی که `ServerResolver` روی گوشی اجرا می‌کند ساخته "
        "شده‌اند. برای بازتولید:\n"
    )
    md.append("```bash\npython3 scripts/list_content_urls.py\n```\n")
    md.append("## خلاصه\n")
    md.append("| مورد | مقدار |")
    md.append("|---|---|")
    md.append(f"| فایل HTML | **{html_n}** |")
    md.append(f"| فایل PDF | **{pdf_n}** |")
    md.append(f"| جمع | **{len(rows)}** |")
    md.append(f"| حجم کل | **{human(total)}** |")
    md.append(f"| سرور داخلی | `{c['INTERNAL_PUBLIC']}` (پارس‌پک، باکت `c539776`) |")
    md.append(f"| سرور خارجی | `{c['EXTERNAL_ENDPOINT']}` (Appwrite، باکت `{c['EXTERNAL_BUCKET']}`) |")
    md.append("")
    md.append("هر فایل روی **هر دو** سرور هست. اپ در حالت «سریع‌ترین» اول سروری را")
    md.append("می‌زند که در سنجش Range برنده شده و دیگری fallback است.")
    md.append("")
    md.append("نشانی سرور خارجی از روی شناسه ساخته می‌شود، پس در جدول‌ها تکرار نشده:")
    md.append("")
    md.append("```")
    md.append(
        f"{c['EXTERNAL_ENDPOINT']}/storage/buckets/{c['EXTERNAL_BUCKET']}"
        f"/files/<شناسه>/view?project={c['EXTERNAL_PROJECT']}"
    )
    md.append("```")
    md.append("")
    md.append("ستون کامل نشانی خارجی در فایل CSV کنار همین سند هست.\n")
    md.append("### نکتهٔ مهم دربارهٔ `common/mirror`\n")
    md.append("از ۱۶۸ فایل HTML، ۷۳ تا زیر `common/mirror` با نام تخت نشسته‌اند و")
    md.append("محتوای همان درس‌های `html ها` هستند، فقط با شناسهٔ Appwrite جدا. پس")
    md.append("تعداد درس‌های **یکتا** ۹۵ تاست (۸۰ در `html ها` + ۱۵ در")
    md.append("`grades/grade9/study`) و بقیه مسیر دوم همان‌هاست.")
    md.append("")
    md.append("همین‌طور ۲۵ از ۲۸ فایل PDF زیر `PDF ها` و ۳ تای باقی‌مانده (فصل ۸")
    md.append("ریاضی) زیر `grades/grade9/study` هستند.")
    md.append("")
    md.append("هر ۱۹۶ نشانی در اجرای تأیید مهاجرت، ناشناس و با اندازهٔ درست خوانده")
    md.append("شدند — گزارش: `ci-report/parspack-verify.md`.\n")

    md.append("## فهرست پوشه‌ها\n")
    md.append("| پوشه | تعداد | حجم |")
    md.append("|---|---:|---:|")
    folders: dict[str, list[dict]] = {}
    for r in rows:
        folders.setdefault(r["folder"], []).append(r)
    for folder, items in folders.items():
        md.append(f"| `{folder}` | {len(items)} | {human(sum(i['size'] for i in items))} |")
    md.append("")

    for folder, items in folders.items():
        note = FOLDER_TITLES.get(folder, "")
        md.append(f"## `{folder}`" + (f" — {note}" if note else "") + "\n")
        md.append("| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |")
        md.append("|---:|---|---|---:|---|")
        for n, r in enumerate(items, 1):
            title = r["title"] or r["file"]
            md.append(f"| {n} | `{r['id']}` | {title} | {human(r['size'])} | <{r['internal']}> |")
        md.append("")

    (ROOT / args.out_md).write_text("\n".join(md), encoding="utf-8")
    print(f"HTML={html_n} PDF={pdf_n} total={len(rows)} → {args.out_md}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
