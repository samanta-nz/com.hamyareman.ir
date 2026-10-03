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


CANDIDATES = [
    # (برچسب, endpoint, bucket, addressing, signature, region)
    ("virtual روی parspack.net", "https://parspack.net", "c539776", "virtual", "s3v4", "us-east-1"),
    ("virtual + SigV2", "https://parspack.net", "c539776", "virtual", "s3", "us-east-1"),
    ("virtual بدون منطقه", "https://parspack.net", "c539776", "virtual", "s3v4", ""),
    ("path روی s3.parspack.net", "https://s3.parspack.net", "c539776", "path", "s3v4", "us-east-1"),
    ("path روی خودِ میزبان", "https://c539776.parspack.net", "c539776", "path", "s3v4", "us-east-1"),
]


def main() -> int:
    where = target("parspack")
    access, secret = where.access_key, where.secret_key
    say(f"Access Key: طول {len(access)}، شروع با `{access[:4]}…` · Secret: طول {len(secret)}")
    say()

    say("## ۱) کدام پیکربندی کار می‌کند؟")
    say()
    say("| پیکربندی | HeadBucket | PutObject |")
    say("|---|---|---|")
    winner = None
    for name, endpoint, bucket, addressing, signature, region in CANDIDATES:
        client = build(endpoint, region, addressing, access, secret, signature)
        try:
            client.head_bucket(Bucket=bucket)
            head = "✅"
        except Exception as exc:  # noqa: BLE001
            head = label(exc)[:55]
        put = "—"
        if head == "✅":
            key = f"_preflight/{uuid.uuid4().hex}.txt"
            try:
                client.put_object(Bucket=bucket, Key=key, Body=b"hamyar", ContentType="text/plain")
                put = "✅"
                if winner is None:
                    winner = (name, endpoint, bucket, addressing, signature, region)
                try:
                    client.delete_object(Bucket=bucket, Key=key)
                except Exception:  # noqa: BLE001
                    pass
            except Exception as exc:  # noqa: BLE001
                put = label(exc)[:55]
        say(f"| {name} | {head} | {put} |")
        if winner:
            break
    say()

    if winner is None:
        say("**هیچ پیکربندی‌ای ننوشت.** محتمل‌ترین علت: کلید مخفی هنگام کپی ناقص/با فاصله ذخیره شده.")
        say("در پنل پارس‌پک کلید را دوباره بسازید و در Secrets مخزن جایگزین کنید.")
        print("\n".join(lines))
        return 1

    name, endpoint, bucket, addressing, signature, region = winner
    say(f"**پیکربندی درست: {name}**")
    say(f"- endpoint `{endpoint}` · bucket `{bucket}` · addressing `{addressing}` · signature `{signature}` · region `{region or 'پیش‌فرض'}`")
    say()
    client = build(endpoint, region, addressing, access, secret, signature)

    say("## ۲) نشانی عمومی")
    say()
    key = f"_preflight/{uuid.uuid4().hex}.txt"
    body = b"hamyar-preflight"
    acl_ok = False
    try:
        client.put_object(Bucket=bucket, Key=key, Body=body, ContentType="text/plain", ACL="public-read")
        acl_ok = True
        say("- ✅ `ACL=public-read` روی شیء پذیرفته شد")
    except Exception as exc:  # noqa: BLE001
        say(f"- ⚠️ ACL روی شیء پذیرفته نشد — {label(exc)}")
        client.put_object(Bucket=bucket, Key=key, Body=body, ContentType="text/plain")
        say("- ✅ نوشتن بدون ACL موفق")
    public_base = f"https://{bucket}.parspack.net"
    working_url = None
    for style, url in (
        ("میزبان اختصاصی", f"{public_base}/{key}"),
        ("path روی endpoint", f"{endpoint.rstrip('/')}/{bucket}/{key}"),
    ):
        try:
            response = requests.get(url, timeout=30)
            ok = response.status_code == 200 and response.content == body
            say(f"- {'✅' if ok else '❌'} **{style}** → HTTP {response.status_code} — `{url}`")
            if ok and working_url is None:
                working_url = url.rsplit("/_preflight", 1)[0]
        except Exception as exc:  # noqa: BLE001
            say(f"- ❌ **{style}** — {label(exc)}")
    try:
        client.delete_object(Bucket=bucket, Key=key)
    except Exception:  # noqa: BLE001
        pass
    say()
    if working_url:
        say(f"**پایهٔ نشانی عمومی: `{working_url}`**")
    else:
        say("**خواندن ناشناس کار نکرد** — در پنل پارس‌پک دسترسی باکت را روی عمومی بگذارید.")
    say()

    say("## ۳) وضعیت فعلی باکت")
    say()
    count = size = 0
    try:
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
                "endpoint": endpoint,
                "bucket": bucket,
                "addressingStyle": addressing,
                "signatureVersion": signature,
                "signingRegion": region,
                "objectAcl": acl_ok,
                "publicBase": working_url,
                "objects": count,
                "bytes": size,
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
