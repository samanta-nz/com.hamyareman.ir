#!/usr/bin/env python3
"""آمادگی‌سنجی سرور داخلی تازه (پارس‌پک) پیش از مهاجرت.

کشف می‌کند: باکت‌های موجود، سبک نشانی (path یا virtual-host)، امکان نوشتن،
امکان عمومی‌کردن شیء و خواندن ناشناس. خروجی مارک‌داون است و هیچ کلیدی چاپ
نمی‌شود.
"""

from __future__ import annotations

import json
import sys
import uuid

import requests
from botocore.exceptions import ClientError

sys.path.insert(0, str(__import__("pathlib").Path(__file__).resolve().parent))
from s3_targets import target  # noqa: E402

lines: list[str] = ["# آمادگی‌سنجی سرور داخلی", ""]


def say(text: str = "") -> None:
    lines.append(text)


def label(exc: BaseException) -> str:
    if isinstance(exc, ClientError):
        error = exc.response.get("Error") or {}
        status = (exc.response.get("ResponseMetadata") or {}).get("HTTPStatusCode")
        return f"`{error.get('Code', 'ClientError')}` (HTTP {status}) — {str(error.get('Message') or '')[:300]}"
    return f"`{type(exc).__name__}` — {str(exc)[:300]}"


def main() -> int:
    where = target("parspack")
    say(f"مقصد: **{where.label}** · endpoint `{where.endpoint}` · region `{where.region}`")
    say(f"کلید: طول {len(where.access_key)}، شروع با `{where.access_key[:4]}…`")
    say()

    say("## ۱) اتصال و فهرست باکت‌ها")
    say()
    buckets: list[str] = []
    client = where.client()
    try:
        buckets = [b["Name"] for b in client.list_buckets().get("Buckets", [])]
        say(f"- ✅ `ListBuckets` — {len(buckets)} باکت: {', '.join(f'`{b}`' for b in buckets) or '—'}")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ `ListBuckets` ناموفق — {label(exc)}")
        say()
        say("**کلید یا endpoint درست نیست؛ مهاجرت شروع نمی‌شود.**")
        print("\n".join(lines))
        return 1
    say()

    bucket = where.bucket
    say(f"## ۲) باکت مقصد `{bucket}`")
    say()
    if bucket not in buckets:
        try:
            client.create_bucket(Bucket=bucket)
            say(f"- ✅ ساخته شد")
        except Exception as exc:  # noqa: BLE001
            say(f"- ❌ ساخت ناموفق — {label(exc)}")
            if buckets:
                bucket = buckets[0]
                say(f"- ↪️ به‌جایش از باکت موجود `{bucket}` استفاده می‌شود.")
            else:
                print("\n".join(lines))
                return 1
    else:
        say("- ✅ از قبل موجود است")
    say()

    say("## ۳) نوشتن و عمومی‌کردن")
    say()
    key = f"_preflight/{uuid.uuid4().hex}.txt"
    body = b"hamyar-preflight"
    wrote = False
    for acl in (None, "public-read"):
        params = {"Bucket": bucket, "Key": key, "Body": body, "ContentType": "text/plain"}
        if acl:
            params["ACL"] = acl
        try:
            client.put_object(**params)
            wrote = True
            say(f"- ✅ `PutObject`{' با ACL=public-read' if acl else ' خصوصی'} موفق")
        except Exception as exc:  # noqa: BLE001
            say(f"- {'⚠️' if acl else '❌'} `PutObject`{' با ACL=public-read' if acl else ''} — {label(exc)}")
    say()

    if wrote:
        say("## ۴) سبک نشانی عمومی")
        say()
        host = where.host
        candidates = {
            "path": f"{where.endpoint.rstrip('/')}/{bucket}/{key}",
            "virtual-host": f"https://{bucket}.{host}/{key}",
        }
        working: list[str] = []
        for style, url in candidates.items():
            try:
                response = requests.get(url, timeout=30)
                ok = response.status_code == 200 and response.content == body
                say(f"- {'✅' if ok else '❌'} **{style}** → HTTP {response.status_code} — `{url}`")
                if ok:
                    working.append(style)
            except Exception as exc:  # noqa: BLE001
                say(f"- ❌ **{style}** — {label(exc)}")
        say()
        if working:
            say(f"**سبک نشانی عمومی: `{working[0]}`** — همین را در config و در اپ می‌گذاریم.")
        else:
            say("**هیچ نشانی عمومی جواب نداد** — باید در پنل پارس‌پک دسترسی باکت روی عمومی برود.")
        say()
        try:
            client.delete_object(Bucket=bucket, Key=key)
            say("- 🧹 شیء آزمایشی پاک شد")
        except Exception as exc:  # noqa: BLE001
            say(f"- ⚠️ پاک‌سازی ناموفق — {label(exc)}")
        say()

    say("## ۵) محتوای فعلی باکت مقصد")
    say()
    try:
        total = 0
        size = 0
        for page in client.get_paginator("list_objects_v2").paginate(Bucket=bucket):
            for obj in page.get("Contents") or []:
                total += 1
                size += int(obj.get("Size") or 0)
        say(f"- {total} شیء، {size / 1073741824:.2f} GB از ۲۰ GB")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ {label(exc)}")

    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except SystemExit:
        raise
    except Exception as exc:  # noqa: BLE001
        print("\n".join(lines))
        print(f"\n**خطای پیش‌بینی‌نشده:** `{type(exc).__name__}` — {str(exc)[:300]}")
        sys.exit(1)
