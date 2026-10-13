#!/usr/bin/env python3
"""Upload the 189 tile-cover images listed by hamyar-bucket-map-3.0.8.csv.

Source files are read from Books-Data/Bucket/Html-files/assets/practice-covers.
S3 keys intentionally omit the repository-only "Bucket/Html-files/" prefix,
matching the public keys/URLs in the CSV. Credentials are read only from
PARSPACK_ACCESS_KEY and PARSPACK_SECRET_KEY environment variables.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import sys
from pathlib import Path

import requests
from botocore.exceptions import ClientError

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from s3_targets import target  # noqa: E402

EXPECTED_COUNT = 189
BUCKET_PREFIX = "assets/practice-covers/"
CACHE_CONTROL = "public, max-age=3600, must-revalidate"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def missing_object(exc: ClientError) -> bool:
    code = str(exc.response.get("Error", {}).get("Code", ""))
    status = exc.response.get("ResponseMetadata", {}).get("HTTPStatusCode")
    return code in {"404", "NoSuchKey", "NotFound"} or status == 404


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True,
                        help="Books-Data/Bucket/Html-files/assets/practice-covers")
    parser.add_argument("--report", type=Path, default=Path("ci-report/practice-covers-upload.md"))
    args = parser.parse_args()

    source = args.source.resolve()
    if not source.is_dir():
        raise SystemExit(f"Source directory does not exist: {source}")
    files = sorted(p for p in source.iterdir() if p.is_file())
    if len(files) != EXPECTED_COUNT:
        raise SystemExit(f"Expected exactly {EXPECTED_COUNT} source files; found {len(files)}")
    invalid = [p.name for p in files if p.suffix.lower() not in {".jpg", ".jpeg"}]
    if invalid:
        raise SystemExit("Unexpected non-JPEG files in source: " + ", ".join(invalid))
    if len({p.name for p in files}) != EXPECTED_COUNT:
        raise SystemExit("Duplicate or ambiguous filenames in source directory")

    dst = target("parspack")
    if dst.bucket != "c539776":
        raise SystemExit("Safety check failed: configured ParsPack bucket is not c539776")
    client = dst.client()

    rows = []
    uploaded = skipped = failures = 0
    total_bytes = 0
    for path in files:
        key = BUCKET_PREFIX + path.name
        body = path.read_bytes()
        digest = sha256_bytes(body)
        size = len(body)
        total_bytes += size
        existing = None
        try:
            existing = client.head_object(Bucket=dst.bucket, Key=key)
        except ClientError as exc:
            if not missing_object(exc):
                raise

        metadata = (existing or {}).get("Metadata") or {}
        identical = (
            existing is not None
            and int(existing.get("ContentLength") or -1) == size
            and metadata.get("sha256", "") == digest
        )
        status = "already-identical"
        if not identical:
            content_type = mimetypes.guess_type(path.name)[0] or "image/jpeg"
            params = {
                "Bucket": dst.bucket,
                "Key": key,
                "Body": body,
                "ContentLength": size,
                "ContentType": content_type,
                "CacheControl": CACHE_CONTROL,
                "Metadata": {"sha256": digest},
            }
            try:
                client.put_object(ACL="public-read", **params)
            except ClientError:
                # Some S3-compatible setups disable per-object ACL; bucket policy
                # may still make the object public.
                client.put_object(**params)
            status = "uploaded"

        stored = client.head_object(Bucket=dst.bucket, Key=key)
        if int(stored.get("ContentLength") or -1) != size:
            status = "failed-size-check"
        elif (stored.get("Metadata") or {}).get("sha256") != digest:
            status = "failed-hash-metadata"

        public_url = dst.public_url(key)
        public_status = "not-checked"
        if status in {"uploaded", "already-identical"}:
            try:
                response = requests.get(public_url, timeout=45)
                if response.status_code == 200 and sha256_bytes(response.content) == digest:
                    public_status = "verified"
                else:
                    public_status = f"failed-http-{response.status_code}-or-hash"
            except Exception as exc:  # noqa: BLE001
                public_status = "failed-" + type(exc).__name__

        ok = status in {"uploaded", "already-identical"} and public_status == "verified"
        if not ok:
            failures += 1
        elif status == "uploaded":
            uploaded += 1
        else:
            skipped += 1
        rows.append({
            "key": key, "bytes": size, "sha256": digest,
            "write": status, "publicRead": public_status, "url": public_url
        })
        print(f"{'OK' if ok else 'FAIL'} {key} {size}B write={status} public={public_status}")

    report = [
        "# ParsPack tile-cover upload",
        "",
        f"- Bucket: `{dst.bucket}`",
        f"- Required CSV-mapped keys: {EXPECTED_COUNT}",
        f"- Successfully uploaded: {uploaded}",
        f"- Already identical: {skipped}",
        f"- Failed upload/verification: {failures}",
        f"- Source bytes: {total_bytes}",
        f"- Public URL prefix: `https://c539776.parspack.net/assets/practice-covers/`",
        "",
        "| Key | Bytes | Write | Public read |",
        "|---|---:|---|---|",
    ]
    report.extend(
        f"| `{r['key']}` | {r['bytes']} | {r['write']} | {r['publicRead']} |"
        for r in rows
    )
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text("\n".join(report) + "\n", encoding="utf-8")
    print(json.dumps({
        "expected": EXPECTED_COUNT, "uploaded": uploaded, "identical": skipped,
        "failures": failures, "bytes": total_bytes,
    }, ensure_ascii=False))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
