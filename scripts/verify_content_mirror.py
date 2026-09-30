#!/usr/bin/env python3
"""Read-only post-deploy verification for the 260-file public content mirror."""
from __future__ import annotations

import argparse
import json
import os
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.parse import quote

import requests

from sync_appwrite_to_arvan import (
    APPWRITE_BUCKET,
    APPWRITE_ENDPOINT,
    APPWRITE_PROJECT,
    ARVAN_BUCKET,
    DEFAULT_MAP,
    list_appwrite,
    load_local_env,
    load_manifest,
    s3_client,
)


def main() -> int:
    load_local_env()
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MAP)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    entries = load_manifest(args.manifest)
    appwrite = list_appwrite()
    s3 = s3_client()
    bucket = os.getenv("ARVAN_BUCKET", ARVAN_BUCKET)
    public_base = "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir"
    project = os.getenv("APPWRITE_PROJECT_ID", APPWRITE_PROJECT)
    endpoint = os.getenv("APPWRITE_ENDPOINT", APPWRITE_ENDPOINT).rstrip("/")
    aw_bucket = os.getenv("APPWRITE_BUCKET_ID", APPWRITE_BUCKET)

    def public_status(url: str) -> int:
        # Some Appwrite/S3-compatible routes reject or mishandle HEAD even though
        # the exact GET path used by the app is healthy. Probe one byte instead.
        try:
            with requests.get(
                url,
                headers={"Range": "bytes=0-0", "Accept-Encoding": "identity"},
                stream=True,
                timeout=(10, 45),
            ) as response:
                if response.status_code in range(200, 300):
                    next(response.iter_content(1), b"")
                return response.status_code
        except requests.RequestException:
            return 0

    def check(entry):
        file_id, key = entry["id"], entry["key"]
        expected = int(appwrite.get(file_id, {}).get("sizeOriginal") or -1)
        try:
            head = s3.head_object(Bucket=bucket, Key=key)
            actual = int(head.get("ContentLength") or -1)
            sha = bool((head.get("Metadata") or {}).get("sha256"))
        except Exception:
            actual, sha = -1, False
        arvan_status = public_status(
            public_base + "/" + "/".join(quote(part, safe="") for part in key.split("/")),
        )
        appwrite_status = public_status(
            f"{endpoint}/storage/buckets/{aw_bucket}/files/{quote(file_id, safe='')}/view?project={project}",
        )
        return {
            "id": file_id,
            "key": key,
            "expected": expected,
            "arvan": actual,
            "sameSize": expected == actual,
            "shaMetadata": sha,
            "appwritePublic": appwrite_status in range(200, 300),
            "arvanPublic": arvan_status in range(200, 300),
        }

    rows = []
    with ThreadPoolExecutor(max_workers=12) as pool:
        futures = [pool.submit(check, entry) for entry in entries]
        for future in as_completed(futures):
            rows.append(future.result())
    rows.sort(key=lambda row: row["id"].lower())
    summary = {
        "manifest": len(entries),
        "presentSameSize": sum(row["sameSize"] for row in rows),
        "shaMetadata": sum(row["shaMetadata"] for row in rows),
        "appwritePublic": sum(row["appwritePublic"] for row in rows),
        "arvanPublic": sum(row["arvanPublic"] for row in rows),
    }
    report = {"summary": summary, "failures": [row for row in rows if not all(
        row[key] for key in ("sameSize", "shaMetadata", "appwritePublic", "arvanPublic")
    )]}
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2))
    print(json.dumps(summary, ensure_ascii=False))
    return 0 if not report["failures"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
