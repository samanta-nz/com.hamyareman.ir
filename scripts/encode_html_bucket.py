#!/usr/bin/env python3
"""رمزگذاری فایل‌های HTML خام به فرمت باکت (HMK1) و بسته‌بندی در زیپ.

فرمت خروجی دقیقاً همان چیزی است که اپ انتظار دارد و `HtmlCodec.unwrap` می‌خواند:

    b"HMK1" (۴ بایت) + IV (۱۲ بایت) + AES-256-GCM(ciphertext + tag ۱۶ بایتی)

یعنی هر فایل دقیقاً **۳۲ بایت** از نسخهٔ خام بزرگ‌تر می‌شود. هیچ فشرده‌سازی
انجام نمی‌شود چون `HtmlCodec` خروجی رمزگشایی را مستقیم به WebView می‌دهد و
هیچ لایهٔ gzip در اپ وجود ندارد — دقیقاً مطابق فایل‌های فعلی روی باکت.

مسیر هر فایل در زیپ از `server-map.json` و بر اساس نام فایل پیدا می‌شود تا
خروجی عیناً روی ساختار باکت بنشیند.

کلید از Appwrite (ردیف app_state/html_media_key) خوانده می‌شود؛ اگر در دسترس
نبود از متغیر HTML_MEDIA_KEY_B64 استفاده می‌شود. کلید هرگز چاپ نمی‌شود.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

import requests
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

MAGIC = b"HMK1"
ROW_ID = "html_media_key"
TABLE = "app_state"

# نام زیپ انگلیسی است (پیدا کردنش آسان باشد) ولی مسیر داخلش فارسیِ باکت می‌ماند.
ZIP_NAMES = {
    "html ها/یوگا": "html-yoga",
    "html ها/حرکات ورزشی": "html-sport",
    "html ها/آموزشگاه": "html-academy",
    "html ها/تمرینات تنفسی": "html-breathing",
    "html ها/آزمایشگاه": "html-lab",
    "html ها/جعبه ابزار ریاضی": "html-mathtools",
    "html ها/جعبه ابزار عمومی": "html-tools",
}
UNMAPPED_DIR = "_بدون-نگاشت"
UNMAPPED_ZIP = "html-unmapped"


def fp(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()[:12]


def key_from_appwrite() -> bytes | None:
    endpoint = os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
    project = os.environ.get("APPWRITE_PROJECT_ID", "")
    database = os.environ.get("APPWRITE_DATABASE_ID", "")
    api_key = os.environ.get("APPWRITE_API_KEY", "")
    if not (project and database and api_key):
        return None
    headers = {"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}
    for path in (
        f"/tablesdb/{database}/tables/{TABLE}/rows/{ROW_ID}",
        f"/databases/{database}/collections/{TABLE}/documents/{ROW_ID}",
    ):
        try:
            r = requests.get(endpoint + path, headers=headers, timeout=(20, 60))
        except Exception:  # noqa: BLE001
            continue
        if r.status_code != 200:
            continue
        try:
            b64 = json.loads(r.json().get("payload") or "{}").get("b", "")
            raw = base64.b64decode(b64)
        except Exception:  # noqa: BLE001
            continue
        if len(raw) == 32:
            return raw
    return None


def key_from_env() -> bytes | None:
    b64 = os.environ.get("HTML_MEDIA_KEY_B64", "").strip()
    if not b64:
        return None
    try:
        raw = base64.b64decode(b64)
    except Exception:  # noqa: BLE001
        return None
    return raw if len(raw) == 32 else None


def bucket_layout(server_map: Path) -> dict[str, str]:
    """نام فایل → مسیر کامل روی باکت."""
    entries = json.loads(server_map.read_text(encoding="utf-8")).get("entries", [])
    out: dict[str, str] = {}
    for e in entries:
        k = str(e.get("key", ""))
        if k.lower().endswith(".html"):
            out.setdefault(os.path.basename(k), k)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", required=True, help="پوشهٔ فایل‌های خام")
    ap.add_argument("--server-map", default="apps/hamyar-app/src/main/assets/content/server-map.json")
    ap.add_argument("--zips", default="dist-zips")
    ap.add_argument("--report", default="ci-report/html-encode-report.md")
    args = ap.parse_args()

    raw_key = key_from_appwrite()
    key_source = "Appwrite (ردیف زنده)"
    env_key = key_from_env()
    if raw_key is None:
        raw_key, key_source = env_key, "متغیر HTML_MEDIA_KEY_B64"
    if raw_key is None:
        print("❌ کلید ۳۲ بایتی در دسترس نیست.", file=sys.stderr)
        return 2
    keys_match = (env_key is not None and env_key == raw_key)

    layout = bucket_layout(Path(args.server_map))
    src = Path(args.source)
    files = sorted(p for p in src.rglob("*") if p.is_file())
    if not files:
        print(f"❌ هیچ فایلی در {src} نیست.", file=sys.stderr)
        return 2

    zips_dir = Path(args.zips)
    zips_dir.mkdir(parents=True, exist_ok=True)
    aes = AESGCM(raw_key)

    groups: dict[str, list[tuple[str, bytes]]] = {}
    rows = []
    bad = 0
    for p in files:
        plain = p.read_bytes()
        iv = os.urandom(12)
        blob = MAGIC + iv + aes.encrypt(iv, plain, None)

        # راستی‌آزمایی رفت‌وبرگشت: همان بایت‌ها دوباره در می‌آیند؟
        back = aes.decrypt(blob[4:16], blob[16:], None)
        ok = back == plain and len(blob) == len(plain) + 32
        if not ok:
            bad += 1

        bucket_key = layout.get(p.name)
        if bucket_key:
            folder = os.path.dirname(bucket_key)
            arc = bucket_key
        else:
            folder = UNMAPPED_DIR
            arc = f"{UNMAPPED_DIR}/{p.relative_to(src).as_posix()}"
        groups.setdefault(folder, []).append((arc, blob))
        rows.append((arc, len(plain), len(blob), ok, bool(bucket_key)))

    made = []
    for folder, items in sorted(groups.items()):
        name = ZIP_NAMES.get(folder, UNMAPPED_ZIP if folder == UNMAPPED_DIR else "html-other")
        path = zips_dir / f"{name}.zip"
        # محتوای رمزشده تصادفی است و فشرده نمی‌شود؛ STORED سریع‌تر و کم‌ریسک‌تر است.
        with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_STORED) as z:
            for arc, blob in sorted(items):
                z.writestr(arc, blob)
        made.append((path.name, folder, len(items), path.stat().st_size))

    rep = Path(args.report)
    rep.parent.mkdir(parents=True, exist_ok=True)
    md = ["# رمزگذاری فایل‌های HTML برای باکت", "",
          f"- منبع کلید: **{key_source}** · اثر انگشت: `{fp(raw_key)}`",
          f"- کلید Appwrite با `HTML_MEDIA_KEY_B64` یکی است: "
          + ("✅" if keys_match else ("—  (متغیر تنظیم نشده)" if env_key is None else "❌ **فرق دارند**")),
          f"- فایل پردازش‌شده: **{len(rows)}**",
          f"- راستی‌آزمایی رفت‌وبرگشت ناموفق: **{bad}**",
          f"- بدون نگاشت در `server-map.json`: **{sum(1 for r in rows if not r[4])}**", "",
          "## زیپ‌ها", "", "| فایل زیپ | پوشهٔ باکت | تعداد | حجم |", "|---|---|---:|---:|"]
    for n, folder, cnt, size in made:
        md.append(f"| `{n}` | `{folder}` | {cnt} | {size/1048576:.1f} MB |")
    md += ["", "## فایل‌ها", "", "| مسیر در باکت | خام | رمزشده | +۳۲ و سالم |", "|---|---:|---:|---|"]
    for arc, a, b, ok, mapped in sorted(rows):
        md.append(f"| `{arc}` | {a:,} | {b:,} | {'✅' if ok else '❌'} |")
    rep.write_text("\n".join(md) + "\n", encoding="utf-8")

    print(json.dumps({
        "files": len(rows), "roundtrip_failed": bad,
        "unmapped": sum(1 for r in rows if not r[4]),
        "zips": [m[0] for m in made],
        "key_fingerprint": fp(raw_key), "key_source": key_source,
        "env_key_matches": keys_match,
    }, ensure_ascii=False))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
