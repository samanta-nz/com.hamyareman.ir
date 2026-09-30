#!/usr/bin/env python3
"""عیب‌یابی خوانا برای فضای ابری آروان (S3 سازگار).

هر مرحله را جدا گزارش می‌کند تا معلوم شود ایراد از «کلید»، «دسترسی باکت»،
«اجازهٔ نوشتن» یا «عمومی‌شدن شیء» است. هیچ کلیدی چاپ نمی‌شود؛ فقط طول و
چهار کاراکتر اولِ Access Key برای تشخیص جابه‌جایی کلیدها.
"""

from __future__ import annotations

import hashlib
import json
import os
import sys
import uuid

import boto3
import requests
from botocore.auth import SigV4Auth
from botocore.awsrequest import AWSRequest
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

    def make_client(**extra) -> object:
        return boto3.session.Session().client(
            "s3",
            endpoint_url=ENDPOINT,
            region_name=REGION,
            aws_access_key_id=access.strip(),
            aws_secret_access_key=secret.strip(),
            config=Config(retries={"max_attempts": 2, "mode": "standard"}, **extra),
        )

    # همان تنظیمی که publish_public_s3_object.py دارد: از botocore 1.36 به بعد
    # چک‌سام CRC32 روی هر PutObject اجباری می‌شود و ارائه‌دهنده‌های S3-سازگار
    # آن را با 400 InvalidArgument رد می‌کنند.
    s3 = make_client(
        request_checksum_calculation="when_required",
        response_checksum_validation="when_required",
    )
    s3_legacy = make_client(signature_version="s3v4")

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

    say("### ۴-خام) PUT دستی‌امضاشده — متن کامل XML خطا")
    say("")
    frozen = boto3.session.Session(
        aws_access_key_id=access.strip(), aws_secret_access_key=secret.strip()
    ).get_credentials().get_frozen_credentials()
    body = b"x"
    body_sha = hashlib.sha256(body).hexdigest()
    host = ENDPOINT.split("://", 1)[1]
    styles = {
        "path-style": f"{ENDPOINT}/{BUCKET}/apk/_diagnose/{uuid.uuid4().hex}.bin",
        "virtual-host": f"https://{BUCKET}.{host}/apk/_diagnose/{uuid.uuid4().hex}.bin",
    }
    for style, url in styles.items():
        try:
            request = AWSRequest(
                method="PUT",
                url=url,
                data=body,
                headers={"x-amz-content-sha256": body_sha, "content-length": str(len(body))},
            )
            SigV4Auth(frozen, "s3", REGION).add_auth(request)
            response = requests.put(url, data=body, headers=dict(request.headers), timeout=60)
            say(f"- **{style}** → HTTP {response.status_code}")
            if response.text.strip():
                say("")
                say("```xml")
                say(response.text.strip()[:1500])
                say("```")
            say("")
        except Exception as exc:  # noqa: BLE001
            say(f"- **{style}** → ❌ {label(exc)}")
            say("")

    say("### ۴-صفر) پاسخ خام آروان به یک PutObject کمینه")
    say("")
    sent: dict = {}

    def capture(request, **_kwargs):  # noqa: ANN001
        sent["method"] = request.method
        sent["url"] = request.url
        sent["headers"] = {
            str(k): (
                "<redacted>"
                if str(k).lower() in {"authorization", "x-amz-content-sha256"}
                else (v.decode("utf-8", "replace") if isinstance(v, bytes) else str(v))
            )
            for k, v in dict(request.headers).items()
        }

    s3.meta.events.register("before-send.s3.PutObject", capture)
    try:
        s3.put_object(Bucket=BUCKET, Key=f"apk/_diagnose/{uuid.uuid4().hex}.bin", Body=b"x")
        say("- ✅ موفق")
    except ClientError as exc:
        payload = json.loads(json.dumps(exc.response, default=str))
        meta = payload.get("ResponseMetadata") or {}
        say(f"- کد: `{(payload.get('Error') or {}).get('Code')}` · HTTP {meta.get('HTTPStatusCode')}")
        extra = {k: v for k, v in payload.items() if k not in {"Error", "ResponseMetadata"}}
        say("")
        say("```json")
        say(json.dumps({"Error": payload.get("Error"), "extra": extra}, ensure_ascii=False, indent=2))
        say("```")
        say("")
        say("پاسخ سرور (هدرها):")
        say("")
        say("```json")
        say(json.dumps(meta.get("HTTPHeaders") or {}, ensure_ascii=False, indent=2))
        say("```")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ {label(exc)}")
    finally:
        s3.meta.events.unregister("before-send.s3.PutObject", capture)
    say("")
    say("درخواستی که فرستاده شد:")
    say("")
    say("```json")
    say(json.dumps(sent, ensure_ascii=False, indent=2))
    say("```")
    say("")

    say("### ۴-الف) اثر تنظیم چک‌سام")
    say("")
    for name, client in (("بدون اصلاح چک‌سام (s3v4 خام)", s3_legacy), ("با when_required", s3)):
        key = f"apk/_diagnose/{uuid.uuid4().hex}.txt"
        try:
            client.put_object(Bucket=BUCKET, Key=key, Body=b"x", ContentType="text/plain")
            say(f"- ✅ {name} — PutObject موفق")
            try:
                client.delete_object(Bucket=BUCKET, Key=key)
            except Exception:  # noqa: BLE001
                pass
        except Exception as exc:  # noqa: BLE001
            say(f"- ❌ {name} — {label(exc)}")
    say("")

    say("### ۴-ب) همان مسیر انتشار (هدرهای واقعی)")
    say("")
    wrote = False
    try:
        s3.put_object(
            Bucket=BUCKET,
            Key=probe_key,
            Body=b"hamyar-diagnose",
            ContentType="text/plain",
            CacheControl="no-store",
            Metadata={"sha256": "probe"},
            ContentLength=15,
        )
        wrote = True
        say(f"- ✅ PutObject خصوصی با ContentType+CacheControl+Metadata موفق (`{probe_key}`)")
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

    say("## ۴-پ) حساب یا باکت؟ (تست قطعی)")
    say("")
    say("اگر ساختن باکت تازه و نوشتن در آن کار کند، ایراد فقط مال باکت فعلی است؛")
    say("اگر آن هم رد شود، نوشتن در کل حساب بسته است (اعتبار/طرح/تعلیق).")
    say("")
    probe_bucket = f"hamyar-diag-{uuid.uuid4().hex[:12]}"
    made = False
    try:
        s3.create_bucket(Bucket=probe_bucket)
        made = True
        say(f"- ✅ CreateBucket موفق (`{probe_bucket}`)")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ CreateBucket ناموفق — {label(exc)}")
    if made:
        try:
            s3.put_object(Bucket=probe_bucket, Key="probe.txt", Body=b"x")
            say("- ✅ PutObject در باکت تازه موفق → **ایراد فقط مال باکت `hamyar-e-man` است**")
            s3.delete_object(Bucket=probe_bucket, Key="probe.txt")
        except Exception as exc:  # noqa: BLE001
            say(f"- ❌ PutObject در باکت تازه هم ناموفق — {label(exc)}")
            say("- → **نوشتن در کل حساب آروان بسته است.**")
        try:
            s3.delete_bucket(Bucket=probe_bucket)
            say("- 🧹 باکت آزمایشی پاک شد")
        except Exception as exc:  # noqa: BLE001
            say(f"- ⚠️ پاک‌کردن باکت آزمایشی ناموفق — {label(exc)}")
    say("")

    say("## ۴-ت) پیکربندی باکت `" + BUCKET + "`")
    say("")
    for name, call in (
        ("Versioning", lambda: s3.get_bucket_versioning(Bucket=BUCKET)),
        ("ACL", lambda: s3.get_bucket_acl(Bucket=BUCKET)),
        ("Policy", lambda: s3.get_bucket_policy(Bucket=BUCKET)),
        ("Lifecycle", lambda: s3.get_bucket_lifecycle_configuration(Bucket=BUCKET)),
        ("ObjectLock", lambda: s3.get_object_lock_configuration(Bucket=BUCKET)),
    ):
        try:
            value = call()
            value.pop("ResponseMetadata", None)
            say(f"- **{name}**: `{json.dumps(value, ensure_ascii=False, default=str)[:400]}`")
        except Exception as exc:  # noqa: BLE001
            say(f"- **{name}**: {label(exc)}")
    say("")

    say("## ۴-ث) منطقهٔ باکت و تست روی همهٔ endpointهای آروان")
    say("")
    try:
        location = s3.get_bucket_location(Bucket=BUCKET)
        say(f"- `GetBucketLocation` → `{location.get('LocationConstraint')}`")
    except Exception as exc:  # noqa: BLE001
        say(f"- `GetBucketLocation` → {label(exc)}")
    say("")
    endpoints = {
        "ir-thr-at1": "https://s3.ir-thr-at1.arvanstorage.ir",
        "ir-tbz-sh1": "https://s3.ir-tbz-sh1.arvanstorage.ir",
        "ir-thr-ba1": "https://s3.ir-thr-ba1.arvanstorage.ir",
        "ir-bnd-ba1": "https://s3.ir-bnd-ba1.arvanstorage.ir",
    }
    for region, endpoint in endpoints.items():
        client = boto3.session.Session().client(
            "s3",
            endpoint_url=endpoint,
            region_name=region,
            aws_access_key_id=access.strip(),
            aws_secret_access_key=secret.strip(),
            config=Config(
                request_checksum_calculation="when_required",
                response_checksum_validation="when_required",
                retries={"max_attempts": 1, "mode": "standard"},
                connect_timeout=15,
                read_timeout=30,
            ),
        )
        key = f"apk/_diagnose/{uuid.uuid4().hex}.bin"
        try:
            client.put_object(Bucket=BUCKET, Key=key, Body=b"x")
            say(f"- ✅ **{region}** — PutObject موفق")
            try:
                client.delete_object(Bucket=BUCKET, Key=key)
            except Exception:  # noqa: BLE001
                pass
        except Exception as exc:  # noqa: BLE001
            say(f"- ❌ **{region}** — {label(exc)}")
    say("")

    say("## ۴-ج) آیا نسخه‌بندی مقصر است؟")
    say("")
    vtest = f"hamyar-diag-{uuid.uuid4().hex[:12]}"
    try:
        s3.create_bucket(Bucket=vtest)
        s3.put_object(Bucket=vtest, Key="before.txt", Body=b"x")
        say("- ✅ باکت تازه بدون نسخه‌بندی — نوشتن موفق")
        s3.put_bucket_versioning(Bucket=vtest, VersioningConfiguration={"Status": "Enabled"})
        say("- ✅ نسخه‌بندی روشن شد")
        try:
            s3.put_object(Bucket=vtest, Key="after.txt", Body=b"x")
            say("- ✅ نوشتن بعد از روشن‌کردن نسخه‌بندی هم موفق → **نسخه‌بندی مقصر نیست**")
        except Exception as exc:  # noqa: BLE001
            say(f"- ❌ نوشتن بعد از نسخه‌بندی ناموفق — {label(exc)}")
            say("- → **نسخه‌بندی مقصر است؛ باید روی باکت اصلی Suspend شود.**")
    except Exception as exc:  # noqa: BLE001
        say(f"- ⚠️ تست ناتمام — {label(exc)}")
    finally:
        try:
            for page in s3.get_paginator("list_object_versions").paginate(Bucket=vtest):
                for item in (page.get("Versions") or []) + (page.get("DeleteMarkers") or []):
                    s3.delete_object(Bucket=vtest, Key=item["Key"], VersionId=item["VersionId"])
            s3.delete_bucket(Bucket=vtest)
            say("- 🧹 باکت آزمایشی پاک شد")
        except Exception as exc:  # noqa: BLE001
            say(f"- ⚠️ پاک‌سازی ناتمام — {label(exc)}")
    say("")

    say("## ۴-چ) حجم و تعداد اشیای باکت اصلی (بررسی سهمیه)")
    say("")
    try:
        total_size = 0
        total_count = 0
        for page in s3.get_paginator("list_objects_v2").paginate(Bucket=BUCKET):
            for obj in page.get("Contents") or []:
                total_size += int(obj.get("Size") or 0)
                total_count += 1
        say(f"- نسخهٔ جاری: **{total_count}** شیء، **{total_size / 1073741824:.2f} GB**")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ {label(exc)}")
    try:
        ver_size = 0
        ver_count = 0
        for page in s3.get_paginator("list_object_versions").paginate(Bucket=BUCKET):
            for item in page.get("Versions") or []:
                ver_size += int(item.get("Size") or 0)
                ver_count += 1
        say(f"- با همهٔ نسخه‌های قدیمی: **{ver_count}** نسخه، **{ver_size / 1073741824:.2f} GB**")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ شمارش نسخه‌ها — {label(exc)}")
    say("")

    say("## ۴-ح) بازنویسی شیء موجود در برابر ساخت شیء تازه")
    say("")
    say("اگر بازنویسی کار کند ولی کلید تازه نه، یعنی **سقف تعداد اشیای باکت** پر شده است.")
    say("")
    try:
        listed = s3.list_objects_v2(Bucket=BUCKET, MaxKeys=1000)
        objects = sorted(listed.get("Contents") or [], key=lambda o: int(o.get("Size") or 0))
        target = next((o for o in objects if 0 < int(o["Size"]) < 200_000), None)
        if not target:
            say("- ⚠️ شیء کوچکی برای تست پیدا نشد.")
        else:
            key = target["Key"]
            head_row = s3.head_object(Bucket=BUCKET, Key=key)
            original = s3.get_object(Bucket=BUCKET, Key=key)["Body"].read()
            say(f"- شیء آزمایشی: `{key}` ({len(original)} بایت)")
            params = {
                "Bucket": BUCKET,
                "Key": key,
                "Body": original,
                "ContentType": head_row.get("ContentType", "application/octet-stream"),
            }
            metadata = head_row.get("Metadata") or {}
            if metadata:
                params["Metadata"] = metadata
            if head_row.get("CacheControl"):
                params["CacheControl"] = head_row["CacheControl"]
            try:
                s3.put_object(**params)
                say("- ✅ **بازنویسی همان شیء با همان بایت‌ها موفق شد**")
                say("- → باکت فقط‌خواندنی نیست؛ **سقف تعداد اشیاء پر است**.")
            except Exception as exc:  # noqa: BLE001
                say(f"- ❌ بازنویسی هم رد شد — {label(exc)}")
                say("- → کل نوشتن روی این باکت بسته است (تنظیم سمت آروان).")
    except Exception as exc:  # noqa: BLE001
        say(f"- ⚠️ تست ناتمام — {label(exc)}")
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
