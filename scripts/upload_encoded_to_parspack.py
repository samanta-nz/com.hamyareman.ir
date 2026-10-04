#!/usr/bin/env python3
"""رمزگذاری HMK1 و آپلود روی پارس‌پک، دقیقاً با همان پوشه‌بندی مبدأ.

کلید رمز از ردیف زندهٔ `app_state/html_media_key` روی Appwrite خوانده می‌شود —
همان کلیدی که بقیهٔ HTMLهای باکت با آن رمز شده‌اند.

هیچ فایل متن‌ساده‌ای آپلود نمی‌شود و کلید هرگز چاپ نمی‌شود.
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

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target as load_target  # noqa: E402

MAGIC = b"HMK1"


def fetch_key() -> bytes:
    endpoint = os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
    project = os.environ.get("APPWRITE_PROJECT_ID", "")
    database = os.environ.get("APPWRITE_DATABASE_ID", "")
    api_key = os.environ.get("APPWRITE_API_KEY", "")
    if project and database and api_key:
        headers = {"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}
        for path in (
            f"/tablesdb/{database}/tables/app_state/rows/html_media_key",
            f"/databases/{database}/collections/app_state/documents/html_media_key",
        ):
            try:
                r = requests.get(endpoint + path, headers=headers, timeout=(20, 60))
                if r.status_code != 200:
                    continue
                payload = r.json().get("payload") or "{}"
                encoded = (json.loads(payload) if isinstance(payload, str) else payload).get("b", "")
                raw = base64.b64decode(encoded)
                if len(raw) == 32:
                    return raw
            except Exception:
                continue
    fallback = os.environ.get("HTML_MEDIA_KEY_B64", "").strip()
    if fallback:
        try:
            raw = base64.b64decode(fallback)
        except Exception:
            raw = b""
        if len(raw) == 32:
            return raw
    raise SystemExit("❌ کلید ۳۲ بایتی پیدا نشد (نه Appwrite، نه HTML_MEDIA_KEY_B64).")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", required=True, help="ریشهٔ فایل‌های خام")
    ap.add_argument("--prefix", default="", help="پیشوند کلید روی باکت")
    ap.add_argument("--report", default="ci-report/parspack-upload.md")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    key = fetch_key()
    aes = AESGCM(key)
    fp = hashlib.sha256(key).hexdigest()[:12]

    src = Path(args.source)
    files = sorted(p for p in src.rglob("*") if p.is_file() and p.suffix.lower() == ".html")
    if not files:
        raise SystemExit(f"❌ فایلی در {src} نیست.")

    tgt = load_target("parspack")
    client = None if args.dry_run else tgt.client()
    rows = []
    for p in files:
        rel = p.relative_to(src).as_posix()
        remote = f"{args.prefix.rstrip('/')}/{rel}" if args.prefix else rel
        plain = p.read_bytes()
        iv = os.urandom(12)
        blob = MAGIC + iv + aes.encrypt(iv, plain, None)
        ok_round = aes.decrypt(blob[4:16], blob[16:], None) == plain and len(blob) == len(plain) + 32

        status = "dry-run"
        if client is not None:
            try:
                client.put_object(
                    Bucket=tgt.bucket, Key=remote, Body=blob,
                    ContentType="text/html; charset=utf-8", ACL="public-read",
                )
                head = client.head_object(Bucket=tgt.bucket, Key=remote)
                status = "ok" if head["ContentLength"] == len(blob) else f"اندازه {head['ContentLength']}"
            except Exception as exc:  # noqa: BLE001
                detail = getattr(exc, "response", {}).get("Error", {}) if hasattr(exc, "response") else {}
                code = detail.get("Code") or type(exc).__name__
                msg = (detail.get("Message") or str(exc))[:120]
                status = f"خطا {code}: {msg}"
        rows.append((remote, len(plain), len(blob), ok_round, status))

    rep = Path(args.report)
    rep.parent.mkdir(parents=True, exist_ok=True)
    md = ["# آپلود رمزشده روی پارس‌پک", "",
          f"- اثر انگشت کلید: `{fp}`",
          f"- باکت: `{tgt.bucket}` · پیشوند: `{args.prefix or '(ریشه)'}`",
          f"- فایل: **{len(rows)}** · موفق: **{sum(1 for r in rows if r[4] == 'ok')}**", "",
          "| کلید روی باکت | خام | رمزشده | سالم | وضعیت |", "|---|---:|---:|---|---|"]
    for k, a, b, ok, st in rows:
        md.append(f"| `{k}` | {a:,} | {b:,} | {'✅' if ok else '❌'} | {st} |")
    rep.write_text("\n".join(md) + "\n", encoding="utf-8")

    bad = [r for r in rows if r[4] not in ("ok", "dry-run")]
    print(json.dumps({"files": len(rows), "failed": len(bad), "key": fp}, ensure_ascii=False))
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
