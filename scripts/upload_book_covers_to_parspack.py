#!/usr/bin/env python3
"""Upload the remaining book cover JPGs to ParsPack without renaming files."""
from __future__ import annotations

import argparse
import hashlib
import mimetypes
import sys
from pathlib import Path

import requests
from botocore.exceptions import ClientError

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from s3_targets import target  # noqa: E402

EXPECTED_COUNT = 12
KEY_PREFIX = "assets/book-covers/"
CACHE_CONTROL = "public, max-age=3600, must-revalidate"


def digest(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def is_missing(exc: ClientError) -> bool:
    error = exc.response.get("Error", {})
    status = exc.response.get("ResponseMetadata", {}).get("HTTPStatusCode")
    return str(error.get("Code", "")) in {"404", "NoSuchKey", "NotFound"} or status == 404


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--report", type=Path, default=Path("ci-report/book-covers-upload.md"))
    args = parser.parse_args()

    source = args.source.resolve()
    if not source.is_dir():
        raise SystemExit(f"Source directory does not exist: {source}")
    files = sorted(p for p in source.iterdir() if p.is_file())
    if len(files) != EXPECTED_COUNT:
        raise SystemExit(f"Expected exactly {EXPECTED_COUNT} book covers, found {len(files)}")
    if any(p.suffix.lower() not in {".jpg", ".jpeg"} for p in files):
        raise SystemExit("Unexpected non-JPEG file in book-covers source")
    if len({p.name for p in files}) != EXPECTED_COUNT:
        raise SystemExit("Duplicate filenames in book-covers source")

    dst = target("parspack")
    if dst.bucket != "c539776":
        raise SystemExit("Safety check failed: configured ParsPack bucket is not c539776")
    client = dst.client()

    rows = []
    uploaded = identical = failures = 0
    total_bytes = 0
    for path in files:
        key = KEY_PREFIX + path.name
        body = path.read_bytes()
        size = len(body)
        sha = digest(body)
        total_bytes += size

        current = None
        try:
            current = client.head_object(Bucket=dst.bucket, Key=key)
        except ClientError as exc:
            if not is_missing(exc):
                raise

        metadata = (current or {}).get("Metadata") or {}
        already_identical = (
            current is not None
            and int(current.get("ContentLength") or -1) == size
            and metadata.get("sha256") == sha
        )
        write_status = "already-identical"
        if not already_identical:
            params = {
                "Bucket": dst.bucket,
                "Key": key,
                "Body": body,
                "ContentLength": size,
                "ContentType": mimetypes.guess_type(path.name)[0] or "image/jpeg",
                "CacheControl": CACHE_CONTROL,
                "Metadata": {"sha256": sha},
            }
            try:
                client.put_object(ACL="public-read", **params)
            except ClientError:
                client.put_object(**params)
            write_status = "uploaded"

        stored = client.head_object(Bucket=dst.bucket, Key=key)
        if int(stored.get("ContentLength") or -1) != size or (stored.get("Metadata") or {}).get("sha256") != sha:
            write_status = "failed-head-check"

        url = dst.public_url(key)
        public_status = "not-checked"
        if write_status in {"uploaded", "already-identical"}:
            try:
                response = requests.get(url, timeout=45)
                if response.status_code == 200 and digest(response.content) == sha:
                    public_status = "verified-200"
                elif response.status_code == 206 and digest(response.content) == sha:
                    public_status = "verified-206"
                else:
                    public_status = f"failed-http-{response.status_code}-or-hash"
            except Exception as exc:  # noqa: BLE001
                public_status = "failed-" + type(exc).__name__

        ok = write_status in {"uploaded", "already-identical"} and public_status in {"verified-200", "verified-206"}
        if ok and write_status == "uploaded":
            uploaded += 1
        elif ok:
            identical += 1
        else:
            failures += 1
        rows.append((key, size, write_status, public_status, url, sha))
        print(f"{'OK' if ok else 'FAIL'} {key} {size} bytes; {write_status}; {public_status}")

    report = [
        "# ParsPack book-cover upload",
        "",
        f"- Bucket: `{dst.bucket}`",
        f"- Expected files: {EXPECTED_COUNT}",
        f"- Newly uploaded: {uploaded}",
        f"- Already identical: {identical}",
        f"- Failed: {failures}",
        f"- Total bytes: {total_bytes}",
        f"- Public URL prefix: `https://c539776.parspack.net/assets/book-covers/`",
        "",
        "| Key | Bytes | Write status | Public read |",
        "|---|---:|---|---|",
    ]
    report.extend(f"| `{k}` | {n} | {w} | {pub} |" for k, n, w, pub, _, _ in rows)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text("\n".join(report) + "\n", encoding="utf-8")
    print(f"SUMMARY: expected={EXPECTED_COUNT}, uploaded={uploaded}, identical={identical}, failures={failures}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
