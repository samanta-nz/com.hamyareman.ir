#!/usr/bin/env python3
"""Atomic Appwrite → Arvan mirror for public app content.

Final object keys come only from assets/content/server-map.json. User uploads are
not mirrored. Uploads go to a private staging key, are size/hash verified, then
server-side copied to the public final key; an interrupted upload can therefore
never replace a healthy final object with a partial file.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import os
import tempfile
import uuid
from pathlib import Path
from typing import Any

import requests

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MAP = ROOT / "apps/hamyar-app/src/main/assets/content/server-map.json"
# مبدأ صوت‌های قدیمی فقط هنگام نبود مقصد خوانده می‌شود؛ اپ runtime به آن وابسته نیست.
APPWRITE_ENDPOINT = os.getenv("SOURCE_APPWRITE_ENDPOINT", "https://fra.cloud.appwrite.io/v1").rstrip("/")
APPWRITE_PROJECT = os.getenv("SOURCE_APPWRITE_PROJECT_ID", "6a9d59e3002751cc3ea8")
APPWRITE_BUCKET = os.getenv("APPWRITE_BUCKET_ID", "6aa1eaae00303400117b")
ARVAN_ENDPOINT = os.getenv("ARVAN_ENDPOINT", "https://s3.ir-thr-at1.arvanstorage.ir")
ARVAN_REGION = os.getenv("ARVAN_REGION", "ir-thr-at1")
ARVAN_BUCKET = os.getenv("ARVAN_BUCKET", "hamyar-e-man")


def load_local_env() -> None:
    for path in (
        ROOT / "Hidden Files/02-appwrite-hamyar.env",
        ROOT / "Hidden Files/01-arvan-iran-bucket.env",
        ROOT / "Hidden Files/arvan-ir-bucket.env",
        Path.home() / ".hamyar-secrets/arvan-ir-bucket.env",
    ):
        if not path.is_file():
            continue
        for raw in path.read_text().splitlines():
            raw = raw.strip()
            if not raw or raw.startswith("#") or "=" not in raw:
                continue
            key, value = raw.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))
    credentials = ROOT / "Hidden Files/02-appwrite-hamyar.json"
    if credentials.is_file() and not os.getenv("APPWRITE_API_KEY"):
        data = json.loads(credentials.read_text())
        os.environ["APPWRITE_API_KEY"] = data.get("api_key", "")


def query(method: str, *values: Any) -> str:
    return json.dumps({"method": method, "values": list(values)}, separators=(",", ":"))


def appwrite_headers() -> dict[str, str]:
    key = os.getenv("SOURCE_APPWRITE_API_KEY") or os.getenv("APPWRITE_API_KEY", "")
    if not key:
        raise RuntimeError("missing APPWRITE_API_KEY")
    return {
        "X-Appwrite-Project": os.getenv("SOURCE_APPWRITE_PROJECT_ID", APPWRITE_PROJECT),
        "X-Appwrite-Key": key,
        "Accept-Encoding": "identity",
    }


def get_appwrite_file(file_id: str) -> dict[str, Any]:
    """Fetch source metadata only after S3 HEAD proves an upload is needed."""
    endpoint = os.getenv("SOURCE_APPWRITE_ENDPOINT", APPWRITE_ENDPOINT).rstrip("/")
    bucket = os.getenv("SOURCE_APPWRITE_BUCKET_ID", APPWRITE_BUCKET)
    response = requests.get(
        f"{endpoint}/storage/buckets/{bucket}/files/{file_id}",
        headers=appwrite_headers(),
        timeout=(20, 90),
    )
    response.raise_for_status()
    return response.json()


def load_manifest(path: Path) -> list[dict[str, Any]]:
    payload = json.loads(path.read_text())
    entries = payload.get("entries") or []
    ids: set[str] = set()
    keys: set[str] = set()
    for entry in entries:
        file_id = str(entry.get("id") or "")
        key = str(entry.get("key") or "")
        if not file_id or not key or file_id in ids or key in keys:
            raise ValueError(f"invalid/duplicate mirror entry: {file_id!r} -> {key!r}")
        if file_id.startswith(("avt-", "rec")):
            raise ValueError("user-private object present in public mirror manifest")
        ids.add(file_id)
        keys.add(key)
    return entries


def s3_client():
    import boto3
    from botocore.config import Config

    access = os.getenv("ARVAN_ACCESS_KEY") or os.getenv("ARVAN_ACCESS_KEY_ID")
    secret = os.getenv("ARVAN_SECRET_KEY") or os.getenv("ARVAN_SECRET_ACCESS_KEY")
    if not access or not secret:
        raise RuntimeError("missing Arvan credentials")
    return boto3.client(
        "s3",
        aws_access_key_id=access,
        aws_secret_access_key=secret,
        endpoint_url=os.getenv("ARVAN_ENDPOINT", ARVAN_ENDPOINT),
        region_name=os.getenv("ARVAN_REGION", ARVAN_REGION),
        config=Config(
            signature_version="s3v4",
            s3={"addressing_style": "path"},
            connect_timeout=20,
            read_timeout=240,
            retries={"max_attempts": 8, "mode": "adaptive"},
        ),
    )


def head(s3: Any, key: str) -> dict[str, Any] | None:
    try:
        return s3.head_object(Bucket=os.getenv("ARVAN_BUCKET", ARVAN_BUCKET), Key=key)
    except Exception:
        return None


def hash_s3(s3: Any, key: str) -> str:
    response = s3.get_object(Bucket=os.getenv("ARVAN_BUCKET", ARVAN_BUCKET), Key=key)
    digest = hashlib.sha256()
    try:
        while True:
            chunk = response["Body"].read(1024 * 1024)
            if not chunk:
                break
            digest.update(chunk)
    finally:
        response["Body"].close()
    return digest.hexdigest()


def download_appwrite(file_id: str, destination: Path, expected_size: int) -> tuple[int, str]:
    endpoint = os.getenv("SOURCE_APPWRITE_ENDPOINT", APPWRITE_ENDPOINT).rstrip("/")
    bucket = os.getenv("SOURCE_APPWRITE_BUCKET_ID", APPWRITE_BUCKET)
    project = os.getenv("SOURCE_APPWRITE_PROJECT_ID", APPWRITE_PROJECT)
    url = f"{endpoint}/storage/buckets/{bucket}/files/{file_id}/view?project={project}"
    digest = hashlib.sha256()
    size = 0
    with requests.get(url, headers=appwrite_headers(), stream=True, timeout=(20, 300)) as response:
        response.raise_for_status()
        with destination.open("wb") as output:
            for chunk in response.iter_content(1024 * 1024):
                if not chunk:
                    continue
                output.write(chunk)
                digest.update(chunk)
                size += len(chunk)
            output.flush()
            os.fsync(output.fileno())
    if expected_size and size != expected_size:
        destination.unlink(missing_ok=True)
        raise RuntimeError(f"incomplete source download {file_id}: {size}/{expected_size}")
    return size, digest.hexdigest()


def copy_with_metadata(s3: Any, source_key: str, final_key: str, size: int, sha256: str, mime: str) -> None:
    bucket = os.getenv("ARVAN_BUCKET", ARVAN_BUCKET)
    args = {
        "Bucket": bucket,
        "Key": final_key,
        "CopySource": {"Bucket": bucket, "Key": source_key},
        "MetadataDirective": "REPLACE",
        "Metadata": {"sha256": sha256, "source": "appwrite"},
        "ContentType": mime,
        "CacheControl": "public, max-age=3600, must-revalidate",
    }
    try:
        s3.copy_object(ACL="public-read", **args)
    except Exception:
        s3.copy_object(**args)
        # Some S3-compatible deployments reject ACL on copy but accept it separately.
        s3.put_object_acl(Bucket=bucket, Key=final_key, ACL="public-read")
    final = head(s3, final_key)
    if final is None or int(final.get("ContentLength") or -1) != size:
        raise RuntimeError(f"final object verification failed: {final_key}")
    metadata = {str(k).lower(): str(v) for k, v in (final.get("Metadata") or {}).items()}
    if metadata.get("sha256") != sha256:
        raise RuntimeError(f"final sha metadata missing: {final_key}")


def atomic_publish(s3: Any, source: Path, key: str, size: int, sha256: str, mime: str) -> None:
    bucket = os.getenv("ARVAN_BUCKET", ARVAN_BUCKET)
    staging = f".staging/{uuid.uuid4().hex}/{key}"
    extra = {
        "ContentType": mime,
        "Metadata": {"sha256": sha256, "source": "appwrite"},
        "CacheControl": "no-store",
    }
    try:
        s3.upload_file(str(source), bucket, staging, ExtraArgs=extra)
        staged = head(s3, staging)
        if staged is None or int(staged.get("ContentLength") or -1) != size:
            raise RuntimeError(f"staging verification failed: {key}")
        staged_meta = {str(k).lower(): str(v) for k, v in (staged.get("Metadata") or {}).items()}
        if staged_meta.get("sha256") != sha256:
            raise RuntimeError(f"staging hash metadata failed: {key}")
        copy_with_metadata(s3, staging, key, size, sha256, mime)
    finally:
        try:
            s3.delete_object(Bucket=bucket, Key=staging)
        except Exception:
            pass


def main() -> int:
    load_local_env()
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MAP)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--only", action="append", default=[], help="file ID; repeatable")
    args = parser.parse_args()

    manifest = load_manifest(args.manifest)
    only = set(args.only)
    selected = [entry for entry in manifest if not only or entry["id"] in only]
    if only and len(selected) != len(only):
        found = {entry["id"] for entry in selected}
        raise RuntimeError(f"unknown manifest IDs: {sorted(only - found)}")
    print(f"plan public files={len(selected)} bytes={sum(int(e.get('size') or 0) for e in selected)}")
    if args.dry_run:
        for entry in selected:
            print("DRY", entry["id"], "->", entry["key"], entry.get("size", 0))
        return 0

    s3 = s3_client()
    published = skipped = failed = 0
    with tempfile.TemporaryDirectory(prefix="hamyar-mirror-") as temporary:
        work = Path(temporary)
        for index, entry in enumerate(selected, 1):
            file_id = entry["id"]
            key = entry["key"]
            manifest_size = int(entry.get("size") or 0)
            target_head = head(s3, key)
            target_size = int((target_head or {}).get("ContentLength") or -1)
            target_meta = {str(k).lower(): str(v) for k, v in ((target_head or {}).get("Metadata") or {}).items()}
            # مهم: قبل از هر تماس با Appwrite قدیمی، وجود و اندازهٔ مقصد را بررسی کن.
            # sha256 متادیتا (اگر موجود باشد) نیز ثبت می‌شود؛ برای فایل هم‌اندازه هیچ
            # GET پرهزینه‌ای از هیچ‌کدام از دو سرویس انجام نمی‌دهیم.
            if not args.force and target_head and manifest_size > 0 and target_size == manifest_size:
                marker = target_meta.get("sha256") or str(target_head.get("ETag") or "").strip('"')
                skipped += 1
                print(f"[{index}/{len(selected)}] SKIP existing-size {file_id} -> {key} ({target_size}, hash={marker[:16] or 'etag-unavailable'})")
                continue
            try:
                # فقط فایل غایب/ناقص یک بار از مبدأ خوانده می‌شود؛ retry سطح برنامه نداریم.
                row = get_appwrite_file(file_id)
                expected = int(row.get("sizeOriginal") or manifest_size)
                if manifest_size and expected != manifest_size:
                    raise RuntimeError(f"source/manifest size mismatch {file_id}: {expected}/{manifest_size}")
                mime = row.get("mimeType") or entry.get("mime") or mimetypes.guess_type(file_id)[0] or "application/octet-stream"
                source = work / (hashlib.sha256(file_id.encode()).hexdigest() + ".bin")
                size, source_sha = download_appwrite(file_id, source, expected)
                atomic_publish(s3, source, key, size, source_sha, mime)
                source.unlink(missing_ok=True)
                published += 1
                print(f"[{index}/{len(selected)}] OK {file_id} -> {key} ({size}, sha256={source_sha})")
            except Exception as exc:
                failed += 1
                print(f"[{index}/{len(selected)}] FAIL {file_id}: {type(exc).__name__}: {str(exc)[:240]}")
    print(f"done published={published} skipped={skipped} failed={failed}")
    return 0 if failed == 0 else 1

if __name__ == "__main__":
    raise SystemExit(main())
