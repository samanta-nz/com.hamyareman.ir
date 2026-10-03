#!/usr/bin/env python3
"""رمزگذاری HMK1 روی فایل‌های یک پوشه، بدون هیچ آپلودی.

قرارداد دقیقاً همان است که دیکودر اندروید می‌فهمد:

    HMK1 + ۱۲ بایت IV تصادفی + AES-256-GCM(ciphertext+tag)

کلید از ردیف زندهٔ `app_state/html_media_key` روی Appwrite خوانده می‌شود و اگر
نشد، از سکرت `HTML_MEDIA_KEY_B64`. کلید هرگز چاپ نمی‌شود؛ فقط ۱۲ رقم اول
اثرانگشت sha256 آن برای مقایسه نمایش داده می‌شود.
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


def html_key() -> bytes:
    endpoint = os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
    project = os.environ.get("APPWRITE_PROJECT_ID", "")
    database = os.environ.get("APPWRITE_DATABASE_ID", "")
    api_key = os.environ.get("APPWRITE_API_KEY", "")
    if project and database and api_key:
        headers = {"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}
        for route in (
            f"/tablesdb/{database}/tables/app_state/rows/html_media_key",
            f"/databases/{database}/collections/app_state/documents/html_media_key",
        ):
            try:
                r = requests.get(endpoint + route, headers=headers, timeout=(20, 60))
                if r.status_code != 200:
                    continue
                payload = r.json().get("payload") or "{}"
                raw = base64.b64decode(
                    (json.loads(payload) if isinstance(payload, str) else payload).get("b", "")
                )
                if len(raw) == 32:
                    return raw
            except Exception:  # noqa: BLE001
                continue
    fallback = os.environ.get("HTML_MEDIA_KEY_B64", "")
    if fallback:
        raw = base64.b64decode(fallback)
        if len(raw) == 32:
            return raw
    raise SystemExit("❌ کلید ۳۲ بایتی پیدا نشد (نه Appwrite، نه HTML_MEDIA_KEY_B64).")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--pattern", default="*.html")
    ap.add_argument("--report", default="")
    args = ap.parse_args()

    src, dst = Path(args.source), Path(args.out)
    dst.mkdir(parents=True, exist_ok=True)
    files = sorted(p for p in src.rglob(args.pattern) if p.is_file())
    if not files:
        raise SystemExit(f"❌ فایلی در {src} نیست.")

    key = html_key()
    aes = AESGCM(key)
    fp = hashlib.sha256(key).hexdigest()[:12]

    rows = []
    for p in files:
        plain = p.read_bytes()
        iv = os.urandom(12)
        blob = MAGIC + iv + aes.encrypt(iv, plain, None)
        # رفت‌وبرگشت همین‌جا بررسی می‌شود تا فایلِ خراب تحویل داده نشود
        assert aes.decrypt(blob[4:16], blob[16:], None) == plain
        assert len(blob) == len(plain) + 32
        target = dst / p.relative_to(src)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(blob)
        rows.append((p.relative_to(src).as_posix(), len(plain), len(blob)))

    if args.report:
        rep = Path(args.report)
        rep.parent.mkdir(parents=True, exist_ok=True)
        md = ["# رمزگذاری HMK1", "", f"- اثر انگشت کلید: `{fp}`", "",
              "| فایل | خام | رمزشده (+۳۲ بایت سرآیند) |", "|---|---:|---:|"]
        md += [f"| `{k}` | {a:,} | {b:,} |" for k, a, b in rows]
        rep.write_text("\n".join(md) + "\n", encoding="utf-8")

    print(json.dumps({"files": len(rows), "key": fp}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
