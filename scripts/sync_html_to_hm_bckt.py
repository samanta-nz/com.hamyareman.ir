#!/usr/bin/env python3
"""Idempotently mirror encrypted HTML into the current HM_BCKT bucket.

Source bytes are read from the already-public internal content origin. Only
HMK1 ciphertext is handled; plaintext and key material are never written to
logs. Existing target files with the expected size are skipped.

The script also installs the shared HTML media key row in the current HM_DB,
using HTML_MEDIA_KEY_B64 when provided or reading the legacy private row with
SOURCE_APPWRITE_API_KEY. It never prints the key.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import pathlib
import tempfile
import time
import urllib.parse
from typing import Any

import requests
from appwrite.client import Client
from appwrite.input_file import InputFile
from appwrite.permission import Permission
from appwrite.role import Role
from appwrite.services.storage import Storage

TARGET_ENDPOINT = os.getenv("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
TARGET_PROJECT = os.getenv("APPWRITE_PROJECT_ID", "6abb134a002025222005")
TARGET_DATABASE = os.getenv("APPWRITE_DATABASE_ID", "6abb238d000d05730d10")
TARGET_BUCKET = os.getenv("APPWRITE_BUCKET_ID", "6abb564d00155cc56d65")
TARGET_KEY = os.getenv("APPWRITE_API_KEY", "").strip()
INTERNAL_PUBLIC = os.getenv(
    "INTERNAL_CONTENT_PUBLIC",
    "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir",
).rstrip("/")
SOURCE_ENDPOINT = os.getenv("SOURCE_APPWRITE_ENDPOINT", "https://fra.cloud.appwrite.io/v1").rstrip("/")
SOURCE_PROJECT = os.getenv("SOURCE_APPWRITE_PROJECT_ID", "6a9d59e3002751cc3ea8")
SOURCE_DATABASE = os.getenv("SOURCE_APPWRITE_DATABASE_ID", "ZahraDB")
SOURCE_KEY = os.getenv("SOURCE_APPWRITE_API_KEY", "").strip()
HMK1 = b"HMK1"


def query(method: str, *values: Any) -> str:
    return json.dumps({"method": method, "values": list(values)}, separators=(",", ":"))


def target_headers() -> dict[str, str]:
    return {"X-Appwrite-Project": TARGET_PROJECT, "X-Appwrite-Key": TARGET_KEY}


def list_target_files() -> dict[str, dict[str, Any]]:
    url = f"{TARGET_ENDPOINT}/storage/buckets/{TARGET_BUCKET}/files"
    found: dict[str, dict[str, Any]] = {}
    offset = 0
    total: int | None = None
    while total is None or offset < total:
        params = [("queries[]", query("limit", 100)), ("queries[]", query("offset", offset))]
        r = requests.get(url, headers=target_headers(), params=params, timeout=(20, 90))
        r.raise_for_status()
        body = r.json()
        page = body.get("files") or []
        total = int(body.get("total", len(page)))
        if not page:
            break
        before = len(found)
        for item in page:
            fid = str(item.get("$id") or "")
            if fid:
                found[fid] = item
        if len(found) == before:
            raise RuntimeError("target pagination repeated a page")
        offset += len(page)
    return found


def html_entries(root: pathlib.Path) -> list[dict[str, Any]]:
    manifest = json.loads((root / "apps/hamyar-app/src/main/assets/content/server-map.json").read_text())
    entries = [
        item for item in manifest.get("entries", [])
        if str(item.get("key") or "").lower().endswith((".html", ".htm", ".hmk1"))
    ]
    ids = [str(item.get("id") or "") for item in entries]
    if not ids or len(ids) != len(set(ids)):
        raise RuntimeError("HTML manifest IDs are empty or duplicated")
    return sorted(entries, key=lambda item: str(item["id"]))


def internal_url(key: str) -> str:
    return INTERNAL_PUBLIC + "/" + "/".join(urllib.parse.quote(part, safe="") for part in key.split("/"))


def public_target_probe(file_id: str) -> tuple[int, bool]:
    fid = urllib.parse.quote(file_id, safe="")
    project = urllib.parse.quote(TARGET_PROJECT, safe="")
    url = f"{TARGET_ENDPOINT}/storage/buckets/{TARGET_BUCKET}/files/{fid}/view?project={project}"
    with requests.get(
        url,
        headers={"Range": "bytes=0-31", "Accept-Encoding": "identity"},
        stream=True,
        timeout=(15, 45),
    ) as response:
        prefix = next(response.iter_content(32), b"")[:32] if response.status_code in (200, 206) else b""
        return response.status_code, prefix.startswith(HMK1)


def obtain_media_key() -> str:
    supplied = os.getenv("HTML_MEDIA_KEY_B64", "").strip()
    if supplied:
        raw = base64.b64decode(supplied, validate=True)
        if len(raw) != 32:
            raise RuntimeError("HTML_MEDIA_KEY_B64 does not decode to 32 bytes")
        return supplied
    if not SOURCE_KEY:
        raise RuntimeError("neither HTML_MEDIA_KEY_B64 nor SOURCE_APPWRITE_API_KEY is available")
    headers = {"X-Appwrite-Project": SOURCE_PROJECT, "X-Appwrite-Key": SOURCE_KEY}
    paths = [
        f"/tablesdb/{SOURCE_DATABASE}/tables/app_state/rows/html_media_key",
        f"/databases/{SOURCE_DATABASE}/collections/app_state/documents/html_media_key",
    ]
    errors: list[str] = []
    for path in paths:
        response = requests.get(SOURCE_ENDPOINT + path, headers=headers, timeout=(20, 60))
        if response.status_code not in range(200, 300):
            errors.append(str(response.status_code))
            continue
        payload = json.loads(response.json().get("payload") or "{}")
        encoded = str(payload.get("b") or "")
        raw = base64.b64decode(encoded, validate=True)
        if len(raw) != 32:
            raise RuntimeError("legacy HTML media-key row is malformed")
        return encoded
    raise RuntimeError("legacy HTML media-key row unavailable: " + ",".join(errors))


def install_media_key(encoded: str) -> str:
    path = f"/tablesdb/{TARGET_DATABASE}/tables/app_state/rows/html_media_key"
    headers = {**target_headers(), "Content-Type": "application/json"}
    existing = requests.get(TARGET_ENDPOINT + path, headers=headers, timeout=(20, 60))
    data = {
        "userId": "global",
        "key": "html_media_key",
        "payload": json.dumps({"b": encoded}, separators=(",", ":")),
        "updatedAt": int(time.time() * 1000),
    }
    if existing.status_code == 404:
        body = {"rowId": "html_media_key", "data": data, "permissions": ['read("users")']}
        response = requests.post(
            f"{TARGET_ENDPOINT}/tablesdb/{TARGET_DATABASE}/tables/app_state/rows",
            headers=headers,
            json=body,
            timeout=(20, 60),
        )
        response.raise_for_status()
        return "created"
    existing.raise_for_status()
    old_payload = json.loads(existing.json().get("payload") or "{}")
    if str(old_payload.get("b") or "") == encoded:
        return "unchanged"
    response = requests.patch(TARGET_ENDPOINT + path, headers=headers, json={"data": data}, timeout=(20, 60))
    response.raise_for_status()
    return "updated"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=0, help="diagnostic cap; zero mirrors all HTML")
    parser.add_argument("--fail-fast", action="store_true", help="stop after the first file failure")
    args = parser.parse_args()
    if not TARGET_KEY:
        raise SystemExit("APPWRITE_API_KEY is required")

    root = pathlib.Path(__file__).resolve().parents[1]
    entries = html_entries(root)
    if args.limit > 0:
        entries = entries[: args.limit]
    target = list_target_files()
    client = Client().set_endpoint(TARGET_ENDPOINT).set_project(TARGET_PROJECT).set_key(TARGET_KEY)
    storage = Storage(client)
    skipped = uploaded = 0
    failures: list[str] = []

    with tempfile.TemporaryDirectory(prefix="hm-bckt-") as temp:
        temp_dir = pathlib.Path(temp)
        for number, item in enumerate(entries, 1):
            fid = str(item["id"])
            key = str(item["key"])
            expected = int(item.get("size") or 0)
            present = target.get(fid)
            if present is not None and (expected <= 0 or int(present.get("sizeOriginal") or 0) == expected):
                status, wrapped = public_target_probe(fid)
                if status in (200, 206) and wrapped:
                    skipped += 1
                    print(f"[{number}/{len(entries)}] SKIP {fid} size={present.get('sizeOriginal')}", flush=True)
                    continue
            local = temp_dir / fid
            try:
                with requests.get(
                    internal_url(key),
                    headers={"Accept-Encoding": "identity"},
                    stream=True,
                    timeout=(20, 180),
                ) as source:
                    source.raise_for_status()
                    with local.open("wb") as out:
                        for chunk in source.iter_content(256 * 1024):
                            if chunk:
                                out.write(chunk)
                size = local.stat().st_size
                if expected > 0 and size != expected:
                    raise RuntimeError(f"source size mismatch {size}/{expected}")
                if local.read_bytes()[:4] != HMK1:
                    raise RuntimeError("source is not an HMK1 envelope")
                if present is not None:
                    storage.delete_file(bucket_id=TARGET_BUCKET, file_id=fid)
                result = storage.create_file(
                    bucket_id=TARGET_BUCKET,
                    file_id=fid,
                    file=InputFile.from_path(str(local)),
                    permissions=[Permission.read(Role.any())],
                )
                actual = int(getattr(result, "size_original", 0) or 0)
                if actual != size:
                    raise RuntimeError(f"target size mismatch {actual}/{size}")
                status, wrapped = public_target_probe(fid)
                if status not in (200, 206) or not wrapped:
                    raise RuntimeError(f"public HMK1 probe failed HTTP {status}")
                uploaded += 1
                print(f"[{number}/{len(entries)}] UPLOAD {fid} size={size}", flush=True)
            except Exception as exc:
                detail = f"{fid}:{type(exc).__name__}:{str(exc)[:300]}"
                failures.append(detail)
                print(f"[{number}/{len(entries)}] FAIL {fid} {type(exc).__name__}", flush=True)
                print("ERROR " + detail, flush=True)
            finally:
                local.unlink(missing_ok=True)
            if failures and args.fail_fast:
                break

    try:
        media_key = install_media_key(obtain_media_key())
    except Exception as exc:
        detail = f"media-key:{type(exc).__name__}:{str(exc)[:300]}"
        failures.append(detail)
        media_key = "failed"
        print("ERROR " + detail, flush=True)
    final = list_target_files()
    verified = 0
    for item in entries:
        fid = str(item["id"])
        expected = int(item.get("size") or 0)
        got = final.get(fid)
        if got is not None and (expected <= 0 or int(got.get("sizeOriginal") or 0) == expected):
            verified += 1
    summary = {
        "bucket": TARGET_BUCKET,
        "folderModel": "flat-with-catalog-virtual-paths",
        "expectedHtml": len(entries),
        "verifiedHtml": verified,
        "uploaded": uploaded,
        "skipped": skipped,
        "mediaKey": media_key,
        "failures": len(failures),
    }
    print(json.dumps(summary, separators=(",", ":")), flush=True)
    if failures or verified != len(entries):
        for failure in failures[:20]:
            print("ERROR", failure, flush=True)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
