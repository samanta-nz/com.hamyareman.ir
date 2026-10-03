#!/usr/bin/env python3
"""آینه‌کردن کامل یک باکت S3 روی باکت دیگر، با حفظ مسیر و پوشه‌بندی.

از آروان (که فقط خواندنش کار می‌کند) به سرور داخلی تازه. هر شیء با همان کلید،
همان ContentType، همان CacheControl و همان متادیتا منتقل می‌شود؛ `sha256` هم
به متادیتای مقصد اضافه می‌شود تا اجرای بعدی بتواند کارِ انجام‌شده را رد کند.

قابل‌ازسرگیری است: اگر ران نیمه‌کاره بماند، اجرای بعدی فقط تفاوت‌ها را می‌برد.
هیچ کلیدی چاپ نمی‌شود.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import sys
import time
from pathlib import Path

from botocore.exceptions import ClientError

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import list_all, target  # noqa: E402

SKIP_PREFIXES = ("staging-v2/", ".staging/", "_preflight/", "apk/_diagnose/")


def error_label(exc: BaseException) -> str:
    if isinstance(exc, ClientError):
        error = exc.response.get("Error") or {}
        status = (exc.response.get("ResponseMetadata") or {}).get("HTTPStatusCode")
        return f"{error.get('Code', 'ClientError')} (HTTP {status}) {str(error.get('Message') or '')[:200]}"
    return f"{type(exc).__name__}: {str(exc)[:200]}"


def guess_type(key: str, fallback: str) -> str:
    if fallback and fallback != "binary/octet-stream":
        return fallback
    guessed, _ = mimetypes.guess_type(key)
    return guessed or "application/octet-stream"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default="arvan")
    parser.add_argument("--dest", default="parspack")
    parser.add_argument("--limit", type=int, default=0, help="۰ = بدون سقف")
    parser.add_argument("--prefix", default="")
    parser.add_argument("--out", type=Path, default=Path("ci-report/mirror.md"))
    parser.add_argument("--json-out", type=Path, default=Path("ci-report/mirror.json"))
    args = parser.parse_args()

    source = target(args.source)
    dest = target(args.dest)
    src = source.client()
    dst = dest.client()

    lines: list[str] = [
        f"# آینه‌سازی {source.label} → {dest.label}",
        "",
        f"مبدأ: `{source.endpoint}/{source.bucket}`",
        f"مقصد: `{dest.endpoint}/{dest.bucket}`",
        "",
    ]

    # باکت مقصد باید در دسترس باشد (ساختنش کار پنل ارائه‌دهنده است)
    try:
        dst.head_bucket(Bucket=dest.bucket)
        lines.append(f"- ✅ باکت مقصد `{dest.bucket}` در دسترس است.")
    except Exception as exc:  # noqa: BLE001
        lines.append(f"- ❌ باکت مقصد در دسترس نیست — {error_label(exc)}")
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print("\n".join(lines))
        return 1

    # فهرست مبدأ
    objects: list[dict] = []
    for obj in list_all(src, source.bucket, source, args.prefix):
        key = obj["Key"]
        if key.endswith("/") or any(key.startswith(p) for p in SKIP_PREFIXES):
            continue
        objects.append(obj)
    objects.sort(key=lambda o: o["Key"])
    total_bytes = sum(int(o["Size"]) for o in objects)
    lines += [
        f"- مبدأ: **{len(objects)}** شیء، **{total_bytes / 1048576:.1f} MB**",
        "",
    ]

    copied = skipped = failed = 0
    moved_bytes = 0
    failures: list[str] = []
    started = time.time()

    for index, obj in enumerate(objects, 1):
        if args.limit and copied >= args.limit:
            break
        key = obj["Key"]
        size = int(obj["Size"])

        existing = None
        try:
            existing = dst.head_object(Bucket=dest.bucket, Key=key)
        except ClientError:
            existing = None
        if existing is not None and int(existing.get("ContentLength") or -1) == size:
            skipped += 1
            continue

        try:
            source_object = src.get_object(Bucket=source.bucket, Key=key)
            body = source_object["Body"].read()
            if len(body) != size:
                raise RuntimeError(f"اندازهٔ دانلود نمی‌خواند: {len(body)} ≠ {size}")
            digest = hashlib.sha256(body).hexdigest()

            metadata = {str(k).lower(): str(v) for k, v in (source_object.get("Metadata") or {}).items()}
            metadata["sha256"] = digest

            params = {
                "Bucket": dest.bucket,
                "Key": key,
                "Body": body,
                "ContentType": guess_type(key, source_object.get("ContentType", "")),
                "Metadata": metadata,
                "ContentLength": len(body),
            }
            if source_object.get("CacheControl"):
                params["CacheControl"] = source_object["CacheControl"]
            if source_object.get("ContentDisposition"):
                params["ContentDisposition"] = source_object["ContentDisposition"]

            try:
                dst.put_object(ACL="public-read", **params)
            except ClientError:
                # بعضی ارائه‌دهنده‌ها ACL روی شیء را نمی‌پذیرند و دسترسی را
                # سطح باکت تنظیم می‌کنند.
                dst.put_object(**params)

            written = dst.head_object(Bucket=dest.bucket, Key=key)
            if int(written.get("ContentLength") or -1) != size:
                raise RuntimeError("اندازهٔ مقصد بعد از نوشتن نمی‌خواند")

            copied += 1
            moved_bytes += size
        except Exception as exc:  # noqa: BLE001
            failed += 1
            failures.append(f"`{key}` — {error_label(exc)}")

        if index % 25 == 0:
            print(f"… {index}/{len(objects)} (کپی {copied} / رد {skipped} / خطا {failed})", flush=True)

    elapsed = time.time() - started
    lines += [
        "## نتیجه",
        "",
        f"- کپی‌شده: **{copied}** شیء، **{moved_bytes / 1048576:.1f} MB**",
        f"- از قبل موجود (رد شد): **{skipped}**",
        f"- ناموفق: **{failed}**",
        f"- زمان: {elapsed / 60:.1f} دقیقه",
        "",
    ]
    if failures:
        lines += ["### خطاها", ""] + [f"- {f}" for f in failures[:60]] + [""]

    # شمارش نهایی مقصد
    dest_count = 0
    dest_bytes = 0
    try:
        for obj in list_all(dst, dest.bucket, dest):
            dest_count += 1
            dest_bytes += int(obj.get("Size") or 0)
        lines += [
            f"- وضعیت مقصد: **{dest_count}** شیء، **{dest_bytes / 1073741824:.2f} GB**",
            "",
        ]
    except Exception as exc:  # noqa: BLE001
        lines.append(f"- ❌ شمارش مقصد — {error_label(exc)}")

    # نمونهٔ خواندن ناشناس
    if objects:
        import requests  # وارد کردن دیرهنگام تا اسکریپت بدون شبکه هم قابل‌ایمپورت باشد

        lines += ["### تست خواندن ناشناس", ""]
        samples = [objects[0]["Key"], objects[len(objects) // 2]["Key"], objects[-1]["Key"]]
        for key in dict.fromkeys(samples):
            url = dest.public_url(key)
            try:
                response = requests.get(url, headers={"Range": "bytes=0-1023"}, timeout=45)
                mark = "✅" if response.status_code in (200, 206) else "❌"
                lines.append(f"- {mark} HTTP {response.status_code} — `{url}`")
            except Exception as exc:  # noqa: BLE001
                lines.append(f"- ❌ {error_label(exc)} — `{url}`")
        lines.append("")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    args.json_out.write_text(
        json.dumps(
            {
                "source": {"endpoint": source.endpoint, "bucket": source.bucket},
                "dest": {"endpoint": dest.endpoint, "bucket": dest.bucket},
                "sourceObjects": len(objects),
                "copied": copied,
                "skipped": skipped,
                "failed": failed,
                "destObjects": dest_count,
                "destBytes": dest_bytes,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print("\n".join(lines))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
