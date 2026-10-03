#!/usr/bin/env python3
"""گذاشتن یک فایل روی سرور داخلی فعال و اثبات خواندن ناشناس آن.

اگر شیء از قبل با همان اندازه و همان SHA-256 آنجا باشد، دوباره آپلود نمی‌شود.
نشانی عمومی در stdout و در GITHUB_ENV (کلید دلخواه) نوشته می‌شود.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import sys
from pathlib import Path

import requests
from botocore.exceptions import ClientError

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target  # noqa: E402


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def probe(url: str, size: int) -> tuple[bool, str]:
    try:
        response = requests.get(
            url, headers={"Range": "bytes=0-1023", "Accept-Encoding": "identity"}, timeout=60
        )
    except Exception as exc:  # noqa: BLE001
        return False, f"{type(exc).__name__}: {exc}"
    if response.status_code not in (200, 206):
        return False, f"HTTP {response.status_code}"
    content_range = response.headers.get("Content-Range", "")
    if "/" in content_range:
        tail = content_range.rsplit("/", 1)[1]
        if tail.isdigit() and int(tail) != size:
            return False, f"اندازه {tail} ≠ {size}"
    if not response.content.startswith(b"PK"):
        return True, f"HTTP {response.status_code} (بدون بررسی امضای ZIP)"
    return True, f"HTTP {response.status_code}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", type=Path, required=True)
    parser.add_argument("--key", required=True)
    parser.add_argument("--provider", default="parspack")
    parser.add_argument("--content-type", default="application/octet-stream")
    parser.add_argument("--cache-control", default="public, max-age=3600, must-revalidate")
    parser.add_argument("--env-name", default="INTERNAL_URL")
    args = parser.parse_args()

    if not args.file.is_file() or args.file.stat().st_size == 0:
        raise SystemExit(f"فایل نیست یا خالی است: {args.file}")

    where = target(args.provider)
    client = where.client()
    size = args.file.stat().st_size
    digest = sha256_file(args.file)
    url = where.public_url(args.key)

    existing = None
    try:
        existing = client.head_object(Bucket=where.bucket, Key=args.key)
    except ClientError:
        existing = None

    already = (
        existing is not None
        and int(existing.get("ContentLength") or -1) == size
        and (existing.get("Metadata") or {}).get("sha256") == digest
    )

    if already:
        print(f"از قبل موجود و یکسان: {args.key}")
    else:
        body = args.file.read_bytes()
        params = {
            "Bucket": where.bucket,
            "Key": args.key,
            "Body": body,
            "ContentType": args.content_type,
            "CacheControl": args.cache_control,
            "Metadata": {"sha256": digest},
            "ContentLength": len(body),
        }
        try:
            client.put_object(ACL="public-read", **params)
        except ClientError:
            client.put_object(**params)
        written = client.head_object(Bucket=where.bucket, Key=args.key)
        if int(written.get("ContentLength") or -1) != size:
            raise SystemExit("اندازهٔ مقصد بعد از نوشتن نمی‌خواند")
        print(f"آپلود شد: {args.key} ({size / 1048576:.1f} MB)")

    ok, detail = probe(url, size)
    print(f"خواندن ناشناس: {detail} — {url}")
    if not ok:
        raise SystemExit("نشانی عمومی قابل دانلود نیست؛ انتشار متوقف شد.")

    github_env = os.getenv("GITHUB_ENV", "")
    if github_env:
        with open(github_env, "a", encoding="utf-8") as handle:
            handle.write(f"{args.env_name}={url}\n")
    print(url)
    return 0


if __name__ == "__main__":
    sys.exit(main())
