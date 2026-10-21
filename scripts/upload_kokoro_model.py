#!/usr/bin/env python3
"""Upload the Hamyar English Kokoro FP16 model to ParsPack and verify public range reads."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path

import requests
from boto3.s3.transfer import TransferConfig
from botocore.exceptions import ClientError

from s3_targets import target

EXPECTED_SIZE = 163_527_961
EXPECTED_SHA256 = "f3a290d384fbb27966d462905c71a46cef9e5fd00516b40df32a0b4afe77ac96"
DEFAULT_KEY = "Hamyar-English/models/kokoro-v1.0.fp16.onnx"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(4 * 1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", type=Path, required=True)
    parser.add_argument("--key", default=DEFAULT_KEY)
    args = parser.parse_args()

    model = args.file.resolve()
    if not model.is_file():
        raise SystemExit(f"model not found: {model}")

    size = model.stat().st_size
    if size != EXPECTED_SIZE:
        raise SystemExit(f"model size mismatch: {size} != {EXPECTED_SIZE}")

    digest = sha256(model)
    if digest.lower() != EXPECTED_SHA256:
        raise SystemExit(f"model SHA-256 mismatch: {digest}")

    dst = target("parspack")
    client = dst.client()
    key = args.key.strip("/")

    existing = None
    try:
        existing = client.head_object(Bucket=dst.bucket, Key=key)
    except ClientError:
        pass

    stored_sha = ((existing or {}).get("Metadata") or {}).get("sha256", "").lower()
    if existing and int(existing.get("ContentLength") or -1) == size and stored_sha == digest:
        print(f"already current: s3://{dst.bucket}/{key}")
    else:
        extra = {
            "ContentType": "application/octet-stream",
            "CacheControl": "public, max-age=31536000, immutable",
            "Metadata": {"sha256": digest},
        }
        transfer = TransferConfig(
            multipart_threshold=16 * 1024 * 1024,
            multipart_chunksize=16 * 1024 * 1024,
            max_concurrency=4,
            use_threads=True,
        )
        try:
            client.upload_file(
                str(model), dst.bucket, key,
                ExtraArgs={**extra, "ACL": "public-read"},
                Config=transfer,
            )
        except ClientError:
            client.upload_file(str(model), dst.bucket, key, ExtraArgs=extra, Config=transfer)

    written = client.head_object(Bucket=dst.bucket, Key=key)
    if int(written.get("ContentLength") or -1) != size:
        raise SystemExit("uploaded object size mismatch")
    remote_sha = ((written.get("Metadata") or {}).get("sha256") or "").lower()
    if remote_sha != digest:
        raise SystemExit("uploaded object SHA metadata mismatch")

    public_url = dst.public_url(key)
    response = requests.get(public_url, headers={"Range": "bytes=0-1023"}, timeout=60)
    if response.status_code not in (200, 206) or not response.content:
        raise SystemExit(f"public range verification failed: HTTP {response.status_code}")

    print(f"PUBLIC_URL={public_url}")
    print(f"SIZE={size}")
    print(f"SHA256={digest}")
    print(f"HTTP={response.status_code}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
