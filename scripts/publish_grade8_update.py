#!/usr/bin/env python3
"""Publish one immutable grade-8 APK to Appwrite + Arvan, then force row v100.

Credentials are read only from the runner environment. No secret is printed or
written to the repository. The update row is switched only after both uploads
and their server-side metadata have been verified.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import time
from pathlib import Path
from urllib.parse import quote

import boto3
import requests
from appwrite.client import Client
from appwrite.id import ID
from appwrite.input_file import InputFile
from appwrite.permission import Permission
from appwrite.role import Role
from appwrite.services.storage import Storage
from botocore.client import Config

APPWRITE_ENDPOINT = "https://fra.cloud.appwrite.io/v1"
APPWRITE_PROJECT = "6a9d59e3002751cc3ea8"
APPWRITE_BUCKET = "6aa1eaae00303400117b"
DATABASE = "ZahraDB"
COLLECTION = "app_state"
ROW_ID = "app_release_grade8"
PACKAGE = "com.hamyareman.p08"
GRADE = "grade8"
ARVAN_ENDPOINT = "https://s3.ir-thr-at1.arvanstorage.ir"
ARVAN_REGION = "ir-thr-at1"
ARVAN_BUCKET = "hamyar-e-man"
ARVAN_PUBLIC = "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir"


def required(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"missing required environment variable {name}")
    return value


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def appwrite_headers() -> dict[str, str]:
    return {
        "X-Appwrite-Project": APPWRITE_PROJECT,
        "X-Appwrite-Key": required("APPWRITE_API_KEY"),
        "Content-Type": "application/json",
    }


def row_url(row_id: str = "") -> str:
    base = f"{APPWRITE_ENDPOINT}/databases/{DATABASE}/collections/{COLLECTION}/documents"
    return base + (f"/{quote(row_id, safe='')}" if row_id else "")


def ensure_row() -> None:
    response = requests.get(row_url(ROW_ID), headers=appwrite_headers(), timeout=(15, 60))
    if response.status_code in range(200, 300):
        return
    if response.status_code != 404:
        raise RuntimeError(f"update-row probe HTTP {response.status_code}")
    empty = {
        "latest": 0, "name": "", "min": 0, "url": "", "externalUrl": "", "internalUrl": "",
        "size": 0, "sha256": "", "chan": "stable", "rollout": 100, "notes": [],
        "packageName": PACKAGE, "gradeId": GRADE, "signingSha256": "",
    }
    body = {
        "documentId": ROW_ID,
        "data": {
            "userId": "global", "key": ROW_ID,
            "payload": json.dumps(empty, ensure_ascii=False, separators=(",", ":")),
            "updatedAt": int(time.time() * 1000),
        },
        "permissions": ['read("any")'],
    }
    created = requests.post(row_url(), headers=appwrite_headers(), json=body, timeout=(15, 60))
    if created.status_code not in range(200, 300):
        raise RuntimeError(f"update-row create HTTP {created.status_code}: {created.text[:200]}")


def upload_appwrite(apk: Path, file_id: str, size: int) -> str:
    metadata_url = f"{APPWRITE_ENDPOINT}/storage/buckets/{APPWRITE_BUCKET}/files/{quote(file_id, safe='')}"
    existing = requests.get(metadata_url, headers=appwrite_headers(), timeout=(15, 60))
    if existing.status_code in range(200, 300):
        if int(existing.json().get("sizeOriginal") or -1) != size:
            raise RuntimeError("immutable Appwrite file ID exists with a different size")
    elif existing.status_code == 404:
        client = Client().set_endpoint(APPWRITE_ENDPOINT).set_project(APPWRITE_PROJECT).set_key(required("APPWRITE_API_KEY"))
        Storage(client).create_file(
            bucket_id=APPWRITE_BUCKET,
            file_id=file_id,
            file=InputFile.from_path(str(apk)),
            permissions=[Permission.read(Role.any())],
        )
    else:
        raise RuntimeError(f"Appwrite file probe HTTP {existing.status_code}")
    verified = requests.get(metadata_url, headers=appwrite_headers(), timeout=(15, 60))
    if verified.status_code not in range(200, 300) or int(verified.json().get("sizeOriginal") or -1) != size:
        raise RuntimeError("Appwrite upload verification failed")
    return f"{metadata_url}/view?project={APPWRITE_PROJECT}"


def arvan_client():
    return boto3.client(
        "s3",
        endpoint_url=ARVAN_ENDPOINT,
        region_name=ARVAN_REGION,
        aws_access_key_id=required("ARVAN_ACCESS_KEY"),
        aws_secret_access_key=required("ARVAN_SECRET_KEY"),
        config=Config(signature_version="s3v4", s3={"addressing_style": "path"}),
    )


def upload_arvan(apk: Path, final: str, size: int, digest: str) -> str:
    s3 = arvan_client()
    staging = f".staging/apk/{GRADE}/{int(time.time())}-{digest[:12]}.apk"
    try:
        s3.upload_file(
            str(apk), ARVAN_BUCKET, staging,
            ExtraArgs={
                "ACL": "private", "ContentType": "application/vnd.android.package-archive",
                "CacheControl": "no-store", "Metadata": {"sha256": digest, "grade": GRADE},
            },
        )
        staged = s3.head_object(Bucket=ARVAN_BUCKET, Key=staging)
        if int(staged.get("ContentLength") or -1) != size or (staged.get("Metadata") or {}).get("sha256") != digest:
            raise RuntimeError("Arvan staging verification failed")
        copy = {
            "Bucket": ARVAN_BUCKET, "Key": final,
            "CopySource": {"Bucket": ARVAN_BUCKET, "Key": staging},
            "MetadataDirective": "REPLACE", "ContentType": "application/vnd.android.package-archive",
            "CacheControl": "public, max-age=3600, must-revalidate",
            "Metadata": {"sha256": digest, "grade": GRADE},
        }
        try:
            s3.copy_object(ACL="public-read", **copy)
        except Exception:
            s3.copy_object(**copy)
            s3.put_object_acl(Bucket=ARVAN_BUCKET, Key=final, ACL="public-read")
        result = s3.head_object(Bucket=ARVAN_BUCKET, Key=final)
        if int(result.get("ContentLength") or -1) != size or (result.get("Metadata") or {}).get("sha256") != digest:
            raise RuntimeError("Arvan final verification failed")
    finally:
        try:
            s3.delete_object(Bucket=ARVAN_BUCKET, Key=staging)
        except Exception:
            pass
    return ARVAN_PUBLIC + "/" + "/".join(quote(part, safe="") for part in final.split("/"))


def publish_row(payload: dict[str, object]) -> None:
    body = {
        "data": {
            "userId": "global", "key": ROW_ID,
            "payload": json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
            "updatedAt": int(time.time() * 1000),
        },
        "permissions": ['read("any")'],
    }
    response = requests.patch(row_url(ROW_ID), headers=appwrite_headers(), json=body, timeout=(15, 60))
    if response.status_code not in range(200, 300):
        raise RuntimeError(f"update-row publish HTTP {response.status_code}: {response.text[:200]}")
    check = requests.get(row_url(ROW_ID), headers=appwrite_headers(), timeout=(15, 60))
    stored = json.loads(check.json().get("payload") or "{}")
    for key in ("latest", "min", "sha256", "packageName", "gradeId", "externalUrl", "internalUrl"):
        if stored.get(key) != payload.get(key):
            raise RuntimeError(f"update-row verification mismatch: {key}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--version-code", type=int, default=100)
    parser.add_argument("--version-name", default="1.99")
    parser.add_argument("--signer-sha256", required=True)
    args = parser.parse_args()
    if args.version_code != 100:
        raise RuntimeError("this controlled test publisher is pinned to versionCode 100")
    apk = args.apk.resolve()
    if not apk.is_file():
        raise RuntimeError(f"APK not found: {apk}")
    size, digest = apk.stat().st_size, sha256(apk)
    immutable = f"g8-v{args.version_code}-{digest[:12]}"
    appwrite_url = upload_appwrite(apk, immutable, size)
    arvan_key = f"apk/{GRADE}/hamyar-{GRADE}-{args.version_name}-v{args.version_code}.apk"
    arvan_url = upload_arvan(apk, arvan_key, size, digest)
    ensure_row()
    payload: dict[str, object] = {
        "latest": args.version_code, "name": args.version_name, "min": args.version_code,
        # Backward-safe URL uses Arvan while Appwrite is over its bandwidth cap.
        "url": arvan_url, "externalUrl": appwrite_url, "internalUrl": arvan_url,
        "size": size, "sha256": digest, "chan": "stable", "rollout": 100,
        "notes": ["تکمیل نسخهٔ پایه هشتم", "آزمایش آپدیت اجباری از Appwrite و آروان"],
        "packageName": PACKAGE, "gradeId": GRADE,
        "signingSha256": args.signer_sha256.lower().replace(":", ""),
    }
    publish_row(payload)
    # Derived public information only; credential values are never included.
    print(json.dumps({
        "rowId": ROW_ID, "versionCode": args.version_code, "versionName": args.version_name,
        "size": size, "sha256": digest, "externalUrl": appwrite_url, "internalUrl": arvan_url,
        "forcedBelow": args.version_code,
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
