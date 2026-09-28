#!/usr/bin/env python3
"""Idempotent storage hardening for Hamyar public content.

Safe order on Appwrite: grant every manifest object file-level read(any), verify,
then enable fileSecurity and remove bucket-wide read/write/delete. User-private
files retain their owner permissions. Arvan changes are defensive bucket settings
only; object payloads are handled by sync_appwrite_to_arvan.py.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from typing import Any
from urllib.parse import quote

import requests

from sync_appwrite_to_arvan import (
    APPWRITE_BUCKET,
    APPWRITE_ENDPOINT,
    APPWRITE_PROJECT,
    ARVAN_BUCKET,
    appwrite_headers,
    list_appwrite,
    load_local_env,
    load_manifest,
    s3_client,
    DEFAULT_MAP,
)


def endpoint() -> str:
    return os.getenv("APPWRITE_ENDPOINT", APPWRITE_ENDPOINT).rstrip("/")


def bucket_id() -> str:
    return os.getenv("APPWRITE_BUCKET_ID", APPWRITE_BUCKET)


def request(method: str, path: str, **kwargs: Any) -> requests.Response:
    response = requests.request(
        method,
        endpoint() + path,
        headers={**appwrite_headers(), "Content-Type": "application/json"},
        timeout=(20, 90),
        **kwargs,
    )
    if response.status_code not in range(200, 300):
        raise RuntimeError(f"Appwrite {method} {path}: HTTP {response.status_code} {response.text[:300]}")
    return response


def bucket_settings() -> dict[str, Any]:
    return request("GET", f"/storage/buckets/{bucket_id()}").json()


def make_content_public(file_id: str) -> None:
    encoded = quote(file_id, safe="")
    request(
        "PUT",
        f"/storage/buckets/{bucket_id()}/files/{encoded}",
        json={"permissions": ['read("any")']},
    )


def harden_bucket(current: dict[str, Any]) -> None:
    body = {
        "name": current["name"],
        # Authenticated users may create owner-scoped avatar/receipt files. No
        # bucket-level read/update/delete: those come from each file only.
        "permissions": ['create("users")'],
        "fileSecurity": True,
        "enabled": bool(current.get("enabled", True)),
        "maximumFileSize": int(current.get("maximumFileSize") or 50_000_000),
        "allowedFileExtensions": current.get("allowedFileExtensions") or [],
        "compression": current.get("compression") or "none",
        "encryption": bool(current.get("encryption", True)),
        "antivirus": bool(current.get("antivirus", True)),
    }
    request("PUT", f"/storage/buckets/{bucket_id()}", json=body)


def anonymous_status(file_id: str) -> int:
    project = os.getenv("APPWRITE_PROJECT_ID", APPWRITE_PROJECT)
    url = f"{endpoint()}/storage/buckets/{bucket_id()}/files/{quote(file_id, safe='')}"
    return requests.get(url, headers={"X-Appwrite-Project": project}, timeout=(15, 45)).status_code


def configure_arvan() -> dict[str, str]:
    s3 = s3_client()
    bucket = os.getenv("ARVAN_BUCKET", ARVAN_BUCKET)
    result: dict[str, str] = {}

    def attempt(name: str, fn) -> None:
        try:
            fn()
            result[name] = "enabled"
        except Exception as exc:
            result[name] = f"unsupported-or-failed:{type(exc).__name__}"

    attempt("versioning", lambda: s3.put_bucket_versioning(
        Bucket=bucket, VersioningConfiguration={"Status": "Enabled"},
    ))
    attempt("encryption", lambda: s3.put_bucket_encryption(
        Bucket=bucket,
        ServerSideEncryptionConfiguration={
            "Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}}],
        },
    ))
    attempt("lifecycle", lambda: s3.put_bucket_lifecycle_configuration(
        Bucket=bucket,
        LifecycleConfiguration={
            "Rules": [
                {
                    "ID": "abort-incomplete-mirror-uploads",
                    "Status": "Enabled",
                    "Filter": {"Prefix": ".staging/"},
                    "AbortIncompleteMultipartUpload": {"DaysAfterInitiation": 1},
                    "Expiration": {"Days": 2},
                },
                {
                    "ID": "expire-old-versions",
                    "Status": "Enabled",
                    "Filter": {"Prefix": ""},
                    "NoncurrentVersionExpiration": {"NoncurrentDays": 30},
                },
            ],
        },
    ))
    attempt("cors", lambda: s3.put_bucket_cors(
        Bucket=bucket,
        CORSConfiguration={
            "CORSRules": [{
                "AllowedHeaders": ["Range", "If-None-Match", "If-Modified-Since"],
                "AllowedMethods": ["GET", "HEAD"],
                "AllowedOrigins": ["*"],
                "ExposeHeaders": ["ETag", "Content-Length", "Content-Range"],
                "MaxAgeSeconds": 3600,
            }],
        },
    ))
    return result


def main() -> int:
    load_local_env()
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MAP)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()

    manifest = load_manifest(args.manifest)
    public_ids = {entry["id"] for entry in manifest}
    inventory = list_appwrite()
    missing = sorted(public_ids - inventory.keys())
    private = [row for file_id, row in inventory.items() if file_id not in public_ids]
    settings = bucket_settings()
    print(json.dumps({
        "mode": "apply" if args.apply else "plan",
        "publicManifestFiles": len(public_ids),
        "privateExcludedFiles": len(private),
        "missingManifestFiles": missing,
        "currentFileSecurity": settings.get("fileSecurity"),
        "currentBucketPermissions": settings.get("$permissions"),
        "targetBucketPermissions": ['create("users")'],
    }, ensure_ascii=False, indent=2))
    if missing:
        raise RuntimeError("manifest is incomplete on Appwrite; refusing to change permissions")
    if not args.apply:
        return 0

    # Public file grants first, so switching off bucket read(any) has no outage.
    for index, file_id in enumerate(sorted(public_ids), 1):
        permissions = inventory[file_id].get("$permissions") or []
        if permissions != ['read("any")']:
            make_content_public(file_id)
            print(f"permission {index}/{len(public_ids)}")

    bad = [file_id for file_id in sorted(public_ids) if anonymous_status(file_id) not in range(200, 300)]
    if bad:
        raise RuntimeError(f"public verification failed before bucket switch: {len(bad)}")

    harden_bucket(settings)
    after = bucket_settings()
    if not after.get("fileSecurity") or set(after.get("$permissions") or []) != {'create("users")'}:
        raise RuntimeError("Appwrite bucket hardening did not persist")

    # Private object names/owners are intentionally not printed.
    private_leaks = sum(anonymous_status(str(row["$id"])) in range(200, 300) for row in private)
    if private_leaks:
        raise RuntimeError(f"private files still anonymously readable: {private_leaks}")

    arvan = configure_arvan()
    print(json.dumps({
        "appwrite": "hardened-and-verified",
        "privateAnonymousLeaks": private_leaks,
        "arvan": arvan,
    }, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
