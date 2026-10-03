#!/usr/bin/env python3
"""بررسی اینکه سه فایل موسیقیِ روی باکت دقیقاً همان نسخهٔ مخزن‌اند.

برای هر فایل: شیء عمومی را از پارس‌پک می‌گیرد، هدر HMK1 و طول را می‌سنجد،
با کلید زندهٔ محتوا رمزگشایی می‌کند و sha256 را با فایل خام مخزن مقایسه می‌کند.
کلید هرگز چاپ نمی‌شود — فقط اثر انگشت کوتاه.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import sys
from pathlib import Path

import requests
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

MAGIC = b"HMK1"
PUBLIC_BASE = os.environ.get("PARSPACK_PUBLIC_BASE", "https://c539776.parspack.net").rstrip("/")
FILES = [
    "background-music.html",
    "background-music-tile.html",
    "background-music-full.html",
]


def fetch_key() -> bytes:
    raw_b64 = os.environ.get("HTML_MEDIA_KEY_B64", "").strip()
    if raw_b64:
        raw = base64.b64decode(raw_b64)
        if len(raw) == 32:
            return raw
    endpoint = os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
    headers = {
        "X-Appwrite-Project": os.environ["APPWRITE_PROJECT_ID"],
        "X-Appwrite-Key": os.environ["APPWRITE_API_KEY"],
    }
    database = os.environ["APPWRITE_DATABASE_ID"]
    for path in (
        f"/tablesdb/{database}/tables/app_state/rows/html_media_key",
        f"/databases/{database}/collections/app_state/documents/html_media_key",
    ):
        response = requests.get(endpoint + path, headers=headers, timeout=(20, 60))
        if response.status_code != 200:
            continue
        raw = base64.b64decode(json.loads(response.json().get("payload") or "{}").get("b", ""))
        if len(raw) == 32:
            return raw
    raise SystemExit("❌ کلید ۳۲ بایتی به دست نیامد.")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", default="Bucket/Html-files")
    ap.add_argument("--prefix", default="Bucket/Html-files")
    ap.add_argument("--report", default="ci-report/music-bucket-verify.md")
    args = ap.parse_args()

    key = fetch_key()
    aes = AESGCM(key)
    rows, ok_all = [], True
    for name in FILES:
        local = Path(args.source) / name
        plain = local.read_bytes()
        url = f"{PUBLIC_BASE}/{args.prefix}/{name}"
        try:
            response = requests.get(url, timeout=(20, 180))
            blob = response.content if response.status_code == 200 else b""
            status = str(response.status_code)
        except Exception as exc:  # noqa: BLE001
            blob, status = b"", type(exc).__name__
        note = ""
        same = False
        if blob[:4] == MAGIC:
            try:
                decoded = aes.decrypt(blob[4:16], blob[16:], None)
                same = hashlib.sha256(decoded).digest() == hashlib.sha256(plain).digest()
                note = "یکسان ✅" if same else "رمزگشایی شد ولی محتوا فرق دارد ❌"
            except Exception:  # noqa: BLE001
                note = "با کلید فعلی باز نشد ❌"
        else:
            note = f"HMK1 نیست (HTTP {status}) ❌"
        ok_all = ok_all and same
        rows.append(
            f"| `{name}` | {len(plain):,} | {len(blob):,} | "
            f"{'۳۲+ درست' if len(blob) == len(plain) + 32 else 'نامنطبق'} | {note} |"
        )

    report = [
        "# بررسی فایل‌های موسیقی روی باکت",
        "",
        f"اثر انگشت کلید: `{hashlib.sha256(key).hexdigest()[:12]}`",
        f"مبنا: `{PUBLIC_BASE}/{args.prefix}/`",
        "",
        "| فایل | خام (بایت) | روی باکت (بایت) | اختلاف | نتیجه |",
        "| --- | ---: | ---: | --- | --- |",
        *rows,
        "",
        ("✅ هر سه فایل روی باکت دقیقاً همان نسخهٔ مخزن‌اند." if ok_all else "❌ دست‌کم یک فایل منطبق نیست."),
    ]
    text = "\n".join(report)
    print(text)
    out = Path(args.report)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text + "\n", encoding="utf-8")
    return 0 if ok_all else 1


if __name__ == "__main__":
    sys.exit(main())
