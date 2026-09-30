#!/usr/bin/env python3
"""آمادگی‌سنجی سرور داخلی تازه (پارس‌پک) پیش از مهاجرت.

ارائه‌دهنده‌های S3-سازگار در دو چیز با آمازون فرق دارند و هر دو باعث خطای
گیج‌کننده می‌شوند: سبک نشانی (path یا virtual-host) و منطقهٔ امضا. این اسکریپت
به‌جای حدس‌زدن، همهٔ ترکیب‌ها را امتحان می‌کند و ترکیبی را که کار می‌کند گزارش
می‌دهد. هیچ کلیدی چاپ نمی‌شود.
"""

from __future__ import annotations

import json
import sys
import uuid
from pathlib import Path

import boto3
import requests
from botocore.client import Config
from botocore.exceptions import ClientError

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target  # noqa: E402

ADDRESSING = ("path",)          # virtual-host روی این endpoint اصلاً DNS ندارد
REGIONS = ("us-east-1", "")
SIGNATURES = ("s3v4", "s3")     # بعضی نصب‌های Ceph هنوز فقط SigV2 را می‌پذیرند

lines: list[str] = ["# آمادگی‌سنجی سرور داخلی", ""]


def say(text: str = "") -> None:
    lines.append(text)


def label(exc: BaseException) -> str:
    if isinstance(exc, ClientError):
        error = exc.response.get("Error") or {}
        status = (exc.response.get("ResponseMetadata") or {}).get("HTTPStatusCode")
        return f"`{error.get('Code', 'ClientError')}` (HTTP {status}) {str(error.get('Message') or '')[:200]}"
    return f"`{type(exc).__name__}` {str(exc)[:200]}"


def build(endpoint: str, region: str, addressing: str, access: str, secret: str, signature: str = "s3v4"):
    return boto3.client(
        "s3",
        endpoint_url=endpoint,
        region_name=region or None,
        aws_access_key_id=access,
        aws_secret_access_key=secret,
        config=Config(
            signature_version=signature,
            request_checksum_calculation="when_required",
            response_checksum_validation="when_required",
            s3={"addressing_style": addressing},
            connect_timeout=15,
            read_timeout=60,
            retries={"max_attempts": 1, "mode": "standard"},
        ),
    )


def raw_look(url: str) -> str:
    try:
        response = requests.get(url, timeout=30)
        body = response.text.strip().replace("\n", " ")[:300]
        return f"HTTP {response.status_code} — `{body}`"
    except Exception as exc:  # noqa: BLE001
        return f"{type(exc).__name__}: {str(exc)[:150]}"


def main() -> int:
    where = target("parspack")
    access, secret = where.access_key, where.secret_key
    say(f"مقصد: **{where.label}** · endpoint `{where.endpoint}`")
    say(f"Access Key: طول {len(access)}، شروع با `{access[:4]}…` · Secret: طول {len(secret)}")
    say()

    say("## ۱) کدام ترکیبِ «سبک نشانی × منطقهٔ امضا» کار می‌کند؟")
    say()
    say("| امضا | منطقه | ListBuckets |")
    say("|---|---|---|")
    winner = None
    buckets: list[str] = []
    for signature in SIGNATURES:
        for region in REGIONS:
            client = build(where.endpoint, region, "path", access, secret, signature)
            try:
                names = [b["Name"] for b in client.list_buckets().get("Buckets", [])]
                say(f"| `{signature}` | `{region or '—'}` | ✅ {len(names)} باکت: {', '.join(names) or '—'} |")
                if winner is None:
                    winner = (signature, region)
                    buckets = names
            except Exception as exc:  # noqa: BLE001
                say(f"| `{signature}` | `{region or '—'}` | ❌ {label(exc)} |")
    say()
    say("### نگاه خام و ناشناس به سرور")
    say()
    say(f"- ریشهٔ endpoint: {raw_look(where.endpoint)}")
    say(f"- مسیر باکت: {raw_look(where.endpoint.rstrip('/') + '/' + where.bucket)}")
    say(f"- باکتِ قطعاً ناموجود: {raw_look(where.endpoint.rstrip('/') + '/bucket-that-does-not-exist-x9')}")
    say()

    if winner is None:
        say("`ListBuckets` هیچ‌جا جواب نداد (بعضی ارائه‌دهنده‌ها آن را پشتیبانی نمی‌کنند).")
        say("سراغ عملیات سطح باکت می‌رویم.")
        say()

    say("## ۲) عملیات سطح باکت")
    say()
    bucket = where.bucket
    say(f"باکت هدف: `{bucket}`" + (f" · باکت‌های دیده‌شده: {', '.join(f'`{b}`' for b in buckets)}" if buckets else ""))
    say()
    say("| امضا | منطقه | HeadBucket | CreateBucket | PutObject |")
    say("|---|---|---|---|---|")
    working = None
    for addressing in SIGNATURES:
        for region in REGIONS:
            client = build(where.endpoint, region, "path", access, secret, addressing)
            try:
                client.head_bucket(Bucket=bucket)
                head = "✅"
            except Exception as exc:  # noqa: BLE001
                head = label(exc)[:60]
            created = "—"
            if head != "✅":
                try:
                    client.create_bucket(Bucket=bucket)
                    created = "✅"
                except Exception as exc:  # noqa: BLE001
                    created = label(exc)[:60]
            put = "—"
            if head == "✅" or created == "✅":
                key = f"_preflight/{uuid.uuid4().hex}.txt"
                try:
                    client.put_object(Bucket=bucket, Key=key, Body=b"hamyar", ContentType="text/plain")
                    put = "✅"
                    if working is None:
                        working = (addressing, region)
                    try:
                        client.delete_object(Bucket=bucket, Key=key)
                    except Exception:  # noqa: BLE001
                        pass
                except Exception as exc:  # noqa: BLE001
                    put = label(exc)[:60]
            say(f"| `{addressing}` | `{region or '—'}` | {head} | {created} | {put} |")
            if working:
                break
        if working:
            break
    say()

    if working is None:
        say("**هیچ ترکیبی نتوانست بنویسد.** یا کلیدها اشتباه ذخیره شده‌اند (فاصله/کاراکتر جا افتاده)،")
        say("یا باکت باید اول در پنل پارس‌پک ساخته شود.")
        print("\n".join(lines))
        return 1

    addressing, region = working
    say(f"**ترکیب درست: امضای `{addressing}` · منطقهٔ `{region or 'پیش‌فرض'}` · سبک path**")
    say()
    client = build(where.endpoint, region, "path", access, secret, addressing)

    say("## ۳) نشانی عمومی")
    say()
    key = f"_preflight/{uuid.uuid4().hex}.txt"
    body = b"hamyar-preflight"
    acl_ok = False
    try:
        client.put_object(Bucket=bucket, Key=key, Body=body, ContentType="text/plain", ACL="public-read")
        acl_ok = True
        say("- ✅ `PutObject` با `ACL=public-read` پذیرفته شد")
    except Exception as exc:  # noqa: BLE001
        say(f"- ⚠️ ACL روی شیء پذیرفته نشد — {label(exc)}")
        try:
            client.put_object(Bucket=bucket, Key=key, Body=body, ContentType="text/plain")
            say("- ✅ نوشتن بدون ACL موفق (دسترسی باید سطح باکت عمومی شود)")
        except Exception as exc2:  # noqa: BLE001
            say(f"- ❌ نوشتن ناموفق — {label(exc2)}")
    host = where.host
    working_styles: list[str] = []
    for style, url in (
        ("path", f"{where.endpoint.rstrip('/')}/{bucket}/{key}"),
        ("virtual-host", f"https://{bucket}.{host}/{key}"),
    ):
        try:
            response = requests.get(url, timeout=30)
            ok = response.status_code == 200 and response.content == body
            say(f"- {'✅' if ok else '❌'} **{style}** → HTTP {response.status_code} — `{url}`")
            if ok:
                working_styles.append(style)
        except Exception as exc:  # noqa: BLE001
            say(f"- ❌ **{style}** — {label(exc)}")
    try:
        client.delete_object(Bucket=bucket, Key=key)
    except Exception:  # noqa: BLE001
        pass
    say()
    if working_styles:
        say(f"**نشانی عمومی: `{working_styles[0]}`**")
    else:
        say("**هیچ نشانی عمومی جواب نداد** — در پنل پارس‌پک دسترسی باکت را روی عمومی بگذارید.")
    say()

    say("## ۴) وضعیت فعلی باکت")
    say()
    try:
        count = size = 0
        for page in client.get_paginator("list_objects_v2").paginate(Bucket=bucket):
            for obj in page.get("Contents") or []:
                count += 1
                size += int(obj.get("Size") or 0)
        say(f"- {count} شیء، {size / 1073741824:.2f} GB از ۲۰ GB")
    except Exception as exc:  # noqa: BLE001
        say(f"- ❌ {label(exc)}")
    say()

    Path("ci-report").mkdir(exist_ok=True)
    Path("ci-report/parspack-settings.json").write_text(
        json.dumps(
            {
                "endpoint": where.endpoint,
                "bucket": bucket,
                "addressingStyle": "path",
                "signatureVersion": addressing,
                "signingRegion": region,
                "objectAcl": acl_ok,
                "publicUrlStyle": working_styles[0] if working_styles else None,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print("\n".join(lines))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except SystemExit:
        raise
    except Exception as exc:  # noqa: BLE001
        print("\n".join(lines))
        print(f"\n**خطای پیش‌بینی‌نشده:** `{type(exc).__name__}` {str(exc)[:300]}")
        sys.exit(1)
