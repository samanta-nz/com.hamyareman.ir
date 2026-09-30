#!/usr/bin/env python3
"""عیب‌یابی خوانا برای فضای ابری آروان (S3 سازگار).

هر مرحله را جدا گزارش می‌کند تا معلوم شود ایراد از «کلید»، «دسترسی باکت»،
«اجازهٔ نوشتن» یا «عمومی‌شدن شیء» است. هیچ کلیدی چاپ نمی‌شود؛ فقط طول و
چهار کاراکتر اولِ Access Key برای تشخیص جابه‌جایی کلیدها.
"""

from __future__ import annotations

import os
import sys
import uuid

import boto3
import requests
from botocore.client import Config
from botocore.exceptions import ClientError

ENDPOINT = os.getenv("ARVAN_ENDPOINT", "https://s3.ir-thr-at1.arvanstorage.ir")
REGION = os.getenv("ARVAN_REGION", "ir-thr-at1")
BUCKET = os.getenv("ARVAN_BUCKET", "hamyar-e-man")

lines: list[str] = ["# تشخیص آروان", ""]


def say(text: str) -> None:
    lines.append(text)


def label(exc: BaseException) -> str:
    if isinstance(exc, ClientError):
        error = exc.response.get("Error") or {}
        status = (exc.response.get("ResponseMetadata") or {}).get("HTTPStatusCode")
        return f"`{error.get('Code', 'ClientError')}` (HTTP {status}) — {str(error.get('Message') or '')[:300]}"
    return f"`{type(exc).__name__}` — {str(exc)[:300]}"


def main() -> int:
    access = os.getenv("ARVAN_ACCESS_KEY", "")
    secret = os.getenv("ARVAN_SECRET_KEY", "")

    say("## ۱) کلیدها")
    say("")
    say(f"- `ARVAN_ACCESS_KEY`: طول {len(access)}، شروع با `{access[:4]}…`" if access else "- ❌ `ARVAN_ACCESS_KEY` خالی است")
    say(f"- `ARVAN_SECRET_KEY`: طول {len(secret)}" if secret else "- ❌ `ARVAN_SECRET_KEY` خالی است")
    say(f"- endpoint: `{ENDPOINT}` · region: `{REGION}` · bucket: `{BUCKET}`")
    say("")
    if not access or not secret:
        say("**نتیجه: کلیدها در secretهای مخزن تنظیم نشده‌اند.**")
        print("\n".join(lines))
        return 1

    if access.strip() != access or secret.strip() != secret:
        say("- ⚠️ کلید فاصله/خط جدید اضافی دارد (هنگام کپی در secret جا افتاده).")
        say("")

    session = boto3.session.Session()
    s3 = session.client(
        "s3",
        endpoint_url=ENDPOINT,
        region_name=REGION,
        aws_access_key_id=access.strip(),
        aws_secret_access_key=secret.strip(),
        config=Config(signature_version="s3v4", retries={"max_attempts": 2, "mode": "standard"}),
    )

    say("## ۲) اعتبار کلید (ListBuckets)")
    say("")
    try:
        buckets = [b["Name"] for b in s3.list_buckets().get("Buckets", [])]
        say(f"- ✅ موفق — {len(buckets)} باکت: {', '.join(f'`{b}`' for b in buckets) or '—'}")
        if BUCKET not in buckets:
            say(f"- ⚠️ باکت `{BUCKET}` در فهرست نیست (شاید کلید به حساب دیگری تعلق دارد).")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ ناموفق — {label(exc)}")
        say("")
        say("**نتیجه: خودِ کلید نامعتبر/منقضی است.**")
        print("\n".join(lines))
        return 1
    say("")

    say(f"## ۳) دسترسی به باکت `{BUCKET}`")
    say("")
    try:
        s3.head_bucket(Bucket=BUCKET)
        say("- ✅ HeadBucket موفق")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ HeadBucket ناموفق — {label(exc)}")
    try:
        listed = s3.list_objects_v2(Bucket=BUCKET, Prefix="apk/", MaxKeys=10)
        keys = [o["Key"] for o in listed.get("Contents", [])]
        say(f"- ✅ ListObjects روی `apk/` — {listed.get('KeyCount', 0)} شیء")
        for key in keys:
            say(f"  - `{key}`")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ ListObjects ناموفق — {label(exc)}")
    say("")

    probe_key = f"apk/_diagnose/{uuid.uuid4().hex}.txt"
    say("## ۴) نوشتن شیء آزمایشی")
    say("")
    wrote = False
    try:
        s3.put_object(Bucket=BUCKET, Key=probe_key, Body=b"hamyar-diagnose", ContentType="text/plain")
        wrote = True
        say(f"- ✅ PutObject خصوصی موفق (`{probe_key}`)")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ PutObject ناموفق — {label(exc)}")

    if wrote:
        try:
            s3.put_object(
                Bucket=BUCKET,
                Key=probe_key,
                Body=b"hamyar-diagnose",
                ContentType="text/plain",
                ACL="public-read",
            )
            say("- ✅ PutObject با `ACL=public-read` موفق")
            url = f"https://{BUCKET}.s3.{REGION}.arvanstorage.ir/{probe_key}"
            try:
                response = requests.get(url, timeout=30)
                say(f"- خواندن ناشناس: HTTP {response.status_code}")
            except Exception as exc:  # noqa: BLE001
                say(f"- ❌ خواندن ناشناس ناموفق — {label(exc)}")
        except Exception as exc:  # noqa: BLE001
            say(f"- ⚠️ ACL public-read پذیرفته نشد — {label(exc)}")
        try:
            s3.delete_object(Bucket=BUCKET, Key=probe_key)
            say("- ✅ DeleteObject موفق (پاک‌سازی شد)")
        except Exception as exc:  # noqa: BLE001
            say(f"- ⚠️ DeleteObject ناموفق — {label(exc)}")
    say("")

    say("## ۵) وضعیت APKهای موجود روی آروان")
    say("")
    try:
        listed = s3.list_objects_v2(Bucket=BUCKET, Prefix="apk/grade9/", MaxKeys=20)
        for obj in listed.get("Contents", []):
            say(f"- `{obj['Key']}` — {obj['Size'] / 1048576:.1f} MB — {obj['LastModified']}")
        if not listed.get("Contents"):
            say("- (خالی)")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ {label(exc)}")

    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # noqa: BLE001
        print("\n".join(lines))
        print(f"\n**خطای پیش‌بینی‌نشده:** `{type(exc).__name__}` — {str(exc)[:300]}")
        sys.exit(1)
