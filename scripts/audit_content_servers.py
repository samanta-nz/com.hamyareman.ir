#!/usr/bin/env python3
"""Read-only production audit for Hamyar's Appwrite and Arvan content stores.

The script never creates, updates, or deletes remote data.  It intentionally
writes only derived metadata and validation results; credentials and the HTML
media key are never included in its output.

Expected environment variables:
  APPWRITE_ENDPOINT, APPWRITE_PROJECT_ID, APPWRITE_BUCKET_ID, APPWRITE_API_KEY
  APPWRITE_DATABASE_ID (optional; defaults to ZahraDB)
  ARVAN_ENDPOINT, ARVAN_REGION, ARVAN_BUCKET,
  ARVAN_ACCESS_KEY, ARVAN_SECRET_KEY

Usage from the repository root:
  python3 scripts/audit_content_servers.py --output audit-output
"""

from __future__ import annotations

import argparse
import base64
import concurrent.futures
import hashlib
import html.parser
import json
import os
import re
import sys
import threading
import time
import urllib.parse
from collections import Counter
from pathlib import Path
from typing import Any, Iterable

import boto3
import requests
from botocore.config import Config
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

APPWRITE_DEFAULT = "https://fra.cloud.appwrite.io/v1"
PROJECT_DEFAULT = "6a9d59e3002751cc3ea8"
BUCKET_DEFAULT = "6aa1eaae00303400117b"
ARVAN_PUBLIC_DEFAULT = "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir"
DATABASE_DEFAULT = "ZahraDB"
HTML_KEY_ROW = "html_media_key"
HMK_MAGIC = b"HMK1"

# Keep network concurrency conservative: the audit reads large HTML objects from
# two production origins and must not behave like a load test.
WORKERS = 4
_thread_local = threading.local()


def session() -> requests.Session:
    value = getattr(_thread_local, "session", None)
    if value is None:
        value = requests.Session()
        value.headers.update({"User-Agent": "HamyarReadOnlyAudit/1.0", "Accept-Encoding": "identity"})
        _thread_local.session = value
    return value


def iso_now() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def compact_error(exc: BaseException) -> str:
    # Exception messages from the SDKs used here contain status/operation details,
    # not credential values.  Still cap output so accidental response bodies do
    # not make the report noisy.
    return re.sub(r"\s+", " ", str(exc)).strip()[:500]


def json_safe(value: Any) -> Any:
    """Recursively make SDK configuration responses JSON serializable."""
    if isinstance(value, dict):
        return {str(key): json_safe(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)):
        return [json_safe(item) for item in value]
    if isinstance(value, bytes):
        # Bucket-setting APIs should not return binary data; expose only length if
        # an S3-compatible implementation adds one unexpectedly.
        return {"binaryLength": len(value)}
    if hasattr(value, "isoformat"):
        return value.isoformat()
    return value


def query(method: str, *values: Any) -> str:
    """Appwrite 1.8+/2.x query wire format used by the REST API."""
    return json.dumps({"method": method, "values": list(values)}, separators=(",", ":"))


def safe_request(method: str, url: str, **kwargs: Any) -> requests.Response:
    timeout = kwargs.pop("timeout", (20, 180))
    response = session().request(method, url, timeout=timeout, allow_redirects=True, **kwargs)
    return response


def appwrite_headers(project: str, key: str | None = None) -> dict[str, str]:
    headers = {"X-Appwrite-Project": project}
    if key:
        headers["X-Appwrite-Key"] = key
    return headers


def get_json(url: str, headers: dict[str, str], *, allow_error: bool = False) -> dict[str, Any]:
    response = safe_request("GET", url, headers=headers, timeout=(20, 60))
    if not allow_error:
        response.raise_for_status()
    try:
        body = response.json()
        if allow_error and response.status_code not in range(200, 300) and isinstance(body, dict):
            body["_status"] = response.status_code
        return body
    except Exception:
        return {"_status": response.status_code, "_non_json": True}


def list_appwrite_files(base: str, project: str, bucket: str, api_key: str | None) -> list[dict[str, Any]]:
    url = f"{base}/storage/buckets/{bucket}/files"
    headers = appwrite_headers(project, api_key)
    offset = 0
    total: int | None = None
    found: dict[str, dict[str, Any]] = {}
    while total is None or offset < total:
        params = [
            ("queries[]", query("limit", 100)),
            ("queries[]", query("offset", offset)),
        ]
        response = safe_request("GET", url, headers=headers, params=params, timeout=(20, 90))
        response.raise_for_status()
        body = response.json()
        page = body.get("files") or []
        total = int(body.get("total", len(page)))
        if not page:
            break
        before = len(found)
        for item in page:
            file_id = str(item.get("$id", ""))
            if file_id:
                found[file_id] = item
        # A guard against an endpoint/proxy ignoring the queries (the old sync
        # script's plain limit/offset parameters are ignored by current Appwrite).
        if len(found) == before:
            raise RuntimeError("Appwrite pagination repeated a page; refusing an incomplete inventory")
        offset += len(page)
    if total is not None and len(found) != total:
        raise RuntimeError(f"Appwrite inventory incomplete: unique={len(found)}, total={total}")
    return list(found.values())


def public_appwrite_url(base: str, project: str, bucket: str, file_id: str) -> str:
    encoded = urllib.parse.quote(file_id, safe="")
    return f"{base}/storage/buckets/{bucket}/files/{encoded}/view?project={urllib.parse.quote(project, safe='')}"


def public_arvan_url(public_base: str, key: str) -> str:
    return public_base.rstrip("/") + "/" + "/".join(urllib.parse.quote(part, safe="") for part in key.split("/"))


def public_probe(url: str, *, method: str = "GET", byte_range: str | None = None) -> dict[str, Any]:
    headers = {"Range": byte_range} if byte_range else {}
    try:
        response = safe_request(method, url, headers=headers, stream=True, timeout=(15, 45))
        # Read only a tiny prefix for GET probes.
        prefix = b""
        if method == "GET":
            prefix = next(response.iter_content(chunk_size=16), b"")[:16]
        result = {
            "status": response.status_code,
            "contentType": response.headers.get("Content-Type"),
            "contentLength": response.headers.get("Content-Length"),
            "contentRange": response.headers.get("Content-Range"),
            "acceptRanges": response.headers.get("Accept-Ranges"),
            "cacheControl": response.headers.get("Cache-Control"),
            "etag": response.headers.get("ETag"),
            "etagPresent": bool(response.headers.get("ETag")),
            "lastModified": response.headers.get("Last-Modified"),
            "hmk1Prefix": prefix.startswith(HMK_MAGIC),
        }
        response.close()
        return result
    except Exception as exc:
        return {"error": compact_error(exc)}


class ReferenceParser(html.parser.HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.refs: list[tuple[str, str, str]] = []
        self.tags: Counter[str] = Counter()
        self.title_depth = 0
        self.title_parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        tag = tag.lower()
        self.tags[tag] += 1
        if tag == "title":
            self.title_depth += 1
        for name, value in attrs:
            if value is not None and name.lower() in {"src", "href", "action", "poster"}:
                self.refs.append((tag, name.lower(), value.strip()))

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() == "title" and self.title_depth:
            self.title_depth -= 1

    def handle_data(self, data: str) -> None:
        if self.title_depth:
            self.title_parts.append(data)


def classify_ref(value: str) -> str:
    lower = value.lower()
    if not value:
        return "empty"
    if lower.startswith(("https://", "http://")):
        return "remote"
    if lower.startswith("data:"):
        return "data"
    if lower.startswith(("javascript:", "mailto:", "tel:")):
        return "special"
    if value.startswith("#"):
        return "fragment"
    return "relative"


def validate_plain_html(plain: bytes) -> dict[str, Any]:
    result: dict[str, Any] = {
        "utf8": False,
        "htmlStructure": False,
        "plainSize": len(plain),
        "plainSha256": sha256(plain),
    }
    try:
        text = plain.decode("utf-8", errors="strict")
        result["utf8"] = True
    except UnicodeDecodeError as exc:
        result["decodeError"] = f"offset {exc.start}"
        return result
    lower = text[:20000].lower()
    result["htmlStructure"] = "<html" in lower or "<!doctype html" in lower
    parser = ReferenceParser()
    try:
        parser.feed(text)
    except Exception as exc:
        result["parserError"] = compact_error(exc)
    kinds = Counter(classify_ref(value) for _, _, value in parser.refs)
    remote_hosts = Counter()
    relative_refs: list[str] = []
    for _, _, value in parser.refs:
        category = classify_ref(value)
        if category == "remote":
            remote_hosts[urllib.parse.urlparse(value).netloc.lower()] += 1
        elif category == "relative" and len(relative_refs) < 20:
            relative_refs.append(value[:250])
    result.update(
        {
            "title": " ".join("".join(parser.title_parts).split())[:300],
            "referenceCounts": dict(sorted(kinds.items())),
            "remoteHosts": dict(sorted(remote_hosts.items())),
            "relativeReferenceExamples": relative_refs,
            "scriptTags": parser.tags.get("script", 0),
            "styleTags": parser.tags.get("style", 0),
        }
    )
    return result


def decrypt_hmk1(blob: bytes, key: bytes) -> bytes:
    if not blob.startswith(HMK_MAGIC) or len(blob) <= 4 + 12 + 16:
        raise ValueError("missing or truncated HMK1 envelope")
    iv = blob[4:16]
    return AESGCM(key).decrypt(iv, blob[16:], None)


def fetch_blob(url: str) -> tuple[bytes | None, dict[str, Any]]:
    try:
        response = safe_request("GET", url, timeout=(20, 240))
        status = response.status_code
        headers = {
            "status": status,
            "contentType": response.headers.get("Content-Type"),
            "contentLength": response.headers.get("Content-Length"),
            "cacheControl": response.headers.get("Cache-Control"),
            "etagPresent": bool(response.headers.get("ETag")),
        }
        if status not in range(200, 300):
            response.close()
            return None, headers
        blob = response.content
        response.close()
        headers.update({"receivedSize": len(blob), "sha256": sha256(blob), "hmk1": blob.startswith(HMK_MAGIC)})
        return blob, headers
    except Exception as exc:
        return None, {"error": compact_error(exc)}


def fetch_s3_blob(s3: Any, bucket: str, key: str) -> tuple[bytes | None, dict[str, Any]]:
    try:
        response = s3.get_object(Bucket=bucket, Key=key)
        blob = response["Body"].read()
        return blob, {
            "status": 200,
            "contentType": response.get("ContentType"),
            "contentLength": response.get("ContentLength"),
            "etagPresent": bool(response.get("ETag")),
            "receivedSize": len(blob),
            "sha256": sha256(blob),
            "hmk1": blob.startswith(HMK_MAGIC),
        }
    except Exception as exc:
        return None, {"error": compact_error(exc)}


def audit_html_item(
    item: dict[str, Any],
    base: str,
    project: str,
    bucket: str,
    arvan_public: str,
    html_key: bytes | None,
    archive_dir: Path | None,
    s3: Any | None,
    arvan_bucket: str,
) -> dict[str, Any]:
    file_id = item["aw"]
    arvan_key = item["key"]
    aw_url = public_appwrite_url(base, project, bucket, file_id)
    arvan_url = public_arvan_url(arvan_public, arvan_key)
    aw_blob, aw_info = fetch_blob(aw_url)
    ar_public_blob, ar_info = fetch_blob(arvan_url)
    ar_blob = ar_public_blob
    ar_authenticated_info: dict[str, Any] | None = None
    if ar_blob is None and s3 is not None:
        ar_blob, ar_authenticated_info = fetch_s3_blob(s3, arvan_bucket, arvan_key)
    result: dict[str, Any] = {
        "id": item["id"],
        "appwriteId": file_id,
        "arvanKey": arvan_key,
        "appwrite": aw_info,
        "arvan": ar_info,
        "arvanPubliclyReadable": ar_public_blob is not None,
        "ciphertextEqual": bool(aw_blob is not None and ar_blob is not None and aw_blob == ar_blob),
    }
    if ar_authenticated_info is not None:
        result["arvanAuthenticatedRead"] = ar_authenticated_info
    if archive_dir is not None and aw_blob is not None:
        archive_dir.mkdir(parents=True, exist_ok=True)
        # Catalog IDs are Appwrite-safe ASCII IDs.  Re-check before using one as
        # a transfer filename so this utility remains safe if the catalog changes.
        safe_id = re.sub(r"[^A-Za-z0-9._-]", "_", str(item["id"]))
        target = archive_dir / f"{safe_id}.hmk1"
        target.write_bytes(aw_blob)
        result["appwrite"]["archivedForOfflineValidation"] = target.name
    if html_key is None:
        result["decryption"] = {"status": "not-run", "reason": "HTML key unavailable to audit runner"}
        return result
    validations: dict[str, Any] = {}
    plaintexts: dict[str, bytes] = {}
    for label, blob in (("appwrite", aw_blob), ("arvan", ar_blob)):
        if blob is None:
            validations[label] = {"ok": False, "error": "download failed"}
            continue
        try:
            plain = decrypt_hmk1(blob, html_key)
            plaintexts[label] = plain
            check = validate_plain_html(plain)
            validations[label] = {"ok": bool(check.get("utf8") and check.get("htmlStructure")), **check}
        except Exception as exc:
            validations[label] = {"ok": False, "error": compact_error(exc)}
    validations["plaintextEqual"] = bool(
        plaintexts.get("appwrite") is not None
        and plaintexts.get("arvan") is not None
        and plaintexts["appwrite"] == plaintexts["arvan"]
    )
    result["decryption"] = validations
    # Explicitly drop plaintext/ciphertext references as soon as the derived
    # checks are complete; neither is written to the artifact.
    del aw_blob, ar_blob, plaintexts
    return result


def summarize_s3_setting(callable_: Any) -> dict[str, Any]:
    def redact_identity(value: Any) -> Any:
        if isinstance(value, dict):
            return {
                str(key): ("<redacted>" if key in {"ID", "DisplayName"} else redact_identity(item))
                for key, item in value.items()
                if key != "ResponseMetadata"
            }
        if isinstance(value, list):
            return [redact_identity(item) for item in value]
        return value

    try:
        # Drop request IDs/headers and canonical-owner identifiers.  They are not
        # needed to assess ACL posture and should not appear in a shared report.
        return redact_identity(callable_())
    except Exception as exc:
        return {"unavailable": compact_error(exc)}


def extract_html_key(base: str, project: str, api_key: str, database: str) -> tuple[bytes | None, str]:
    headers = appwrite_headers(project, api_key)
    paths = [
        f"{base}/tablesdb/{database}/tables/app_state/rows/{HTML_KEY_ROW}",
        f"{base}/databases/{database}/collections/app_state/documents/{HTML_KEY_ROW}",
    ]
    for path in paths:
        try:
            response = safe_request("GET", path, headers=headers, timeout=(20, 60))
            if response.status_code not in range(200, 300):
                response.close()
                continue
            row = response.json()
            response.close()
            payload = row.get("payload", "")
            parsed = json.loads(payload) if isinstance(payload, str) else payload
            raw = base64.b64decode(parsed.get("b", ""), validate=True)
            if len(raw) == 32:
                return raw, "available (used in memory only)"
        except Exception:
            continue
    return None, "unavailable"


def redact_permission(value: str) -> str:
    return re.sub(r"user:[A-Za-z0-9._-]+", "user:<redacted>", value)


def redact_file_id(value: str) -> str:
    if value.startswith("avt-"):
        return "avt-<redacted>"
    if value.startswith("rec"):
        return "rec<redacted>"
    return value


def sanitize_appwrite_file(item: dict[str, Any]) -> dict[str, Any]:
    keep = [
        "$id",
        "$createdAt",
        "$updatedAt",
        "$permissions",
        "name",
        "mimeType",
        "sizeOriginal",
        "sizeActual",
        "chunksTotal",
        "chunksUploaded",
        "encryption",
        "compression",
    ]
    output = {key: item.get(key) for key in keep if key in item}
    if "$id" in output:
        output["$id"] = redact_file_id(str(output["$id"]))
    if "$permissions" in output:
        output["$permissions"] = [redact_permission(str(value)) for value in output["$permissions"] or []]
    return output


def sanitize_bucket(bucket: dict[str, Any]) -> dict[str, Any]:
    keep = [
        "$id",
        "$createdAt",
        "$updatedAt",
        "$permissions",
        "name",
        "enabled",
        "maximumFileSize",
        "allowedFileExtensions",
        "compression",
        "encryption",
        "antivirus",
        "fileSecurity",
        "totalFiles",
        "totalSize",
    ]
    return {key: bucket.get(key) for key in keep if key in bucket}


def probe_catalog_arvan_objects(public_base: str, items: Iterable[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    """Build catalog-key metadata using only the same anonymous HEADs as the app."""
    output: dict[str, dict[str, Any]] = {}

    def one(item: dict[str, Any]) -> tuple[str, dict[str, Any]]:
        key = str(item["key"])
        probe = public_probe(public_arvan_url(public_base, key), method="HEAD")
        size_raw = probe.get("contentLength")
        size = int(size_raw) if isinstance(size_raw, str) and size_raw.isdigit() else 0
        return key, {
            "Size": size,
            "ETag": str(probe.get("etag") or "").strip('"'),
            "LastModified": probe.get("lastModified"),
            "StorageClass": None,
            "PublicStatus": probe.get("status"),
            "ContentType": probe.get("contentType"),
            "CacheControl": probe.get("cacheControl"),
        }

    with concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS) as pool:
        futures = [pool.submit(one, item) for item in items]
        for future in concurrent.futures.as_completed(futures):
            key, metadata = future.result()
            if metadata.get("PublicStatus") in range(200, 300):
                output[key] = metadata
    return output


def run(args: argparse.Namespace) -> dict[str, Any]:
    base = os.getenv("APPWRITE_ENDPOINT", APPWRITE_DEFAULT).rstrip("/")
    project = os.getenv("APPWRITE_PROJECT_ID", PROJECT_DEFAULT).strip()
    bucket = os.getenv("APPWRITE_BUCKET_ID", BUCKET_DEFAULT).strip()
    api_key = os.getenv("APPWRITE_API_KEY", "").strip()
    database = os.getenv("APPWRITE_DATABASE_ID", DATABASE_DEFAULT).strip()
    arvan_endpoint = os.getenv("ARVAN_ENDPOINT", "https://s3.ir-thr-at1.arvanstorage.ir").strip()
    arvan_region = os.getenv("ARVAN_REGION", "ir-thr-at1").strip()
    arvan_bucket = os.getenv("ARVAN_BUCKET", "hamyar-e-man").strip()
    arvan_public = os.getenv("ARVAN_PUBLIC", ARVAN_PUBLIC_DEFAULT).strip().rstrip("/")
    arvan_access = os.getenv("ARVAN_ACCESS_KEY", "").strip()
    arvan_secret = os.getenv("ARVAN_SECRET_KEY", "").strip()

    authenticated_appwrite = bool(api_key)
    authenticated_arvan = bool(arvan_access and arvan_secret)

    catalog_path = Path(args.catalog)
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    items: list[dict[str, Any]] = catalog["items"]
    html_items = [item for item in items if item.get("kind") == "html"]

    result: dict[str, Any] = {
        "schemaVersion": 1,
        "generatedAt": iso_now(),
        "mode": "read-only",
        "secretsIncluded": False,
        "catalog": {
            "path": str(catalog_path),
            "categories": len(catalog.get("categories", [])),
            "items": len(items),
            "kinds": dict(sorted(Counter(str(item.get("kind", "")) for item in items).items())),
            "htmlItems": len(html_items),
            "duplicateIds": sorted(key for key, count in Counter(str(item.get("id")) for item in items).items() if count > 1),
            "missingAddressFields": [item.get("id") for item in items if not item.get("aw") or not item.get("key")],
        },
    }

    aw_headers = appwrite_headers(project, api_key or None)
    aw_bucket_raw = get_json(
        f"{base}/storage/buckets/{bucket}", aw_headers, allow_error=not authenticated_appwrite
    )
    aw_files_raw = list_appwrite_files(base, project, bucket, api_key or None)
    aw_files = {str(item["$id"]): item for item in aw_files_raw}
    result["appwrite"] = {
        "endpoint": base,
        "projectId": project,
        "authenticatedSettings": authenticated_appwrite,
        "bucket": sanitize_bucket(aw_bucket_raw),
        "bucketSettingsResponseStatus": aw_bucket_raw.get("_status") if not authenticated_appwrite else 200,
        "fileCount": len(aw_files),
        "totalOriginalBytes": sum(int(item.get("sizeOriginal") or 0) for item in aw_files.values()),
        "mimeTypes": dict(sorted(Counter(str(item.get("mimeType", "")) for item in aw_files.values()).items())),
        "encryptedMetadataCount": sum(item.get("encryption") is True for item in aw_files.values()),
        "filePermissionPatterns": dict(
            sorted(
                Counter(
                    json.dumps(
                        [redact_permission(str(value)) for value in item.get("$permissions") or []],
                        ensure_ascii=False,
                        sort_keys=True,
                    )
                    for item in aw_files.values()
                ).items()
            )
        ),
        "public": {
            "anonymousList": public_probe(f"{base}/storage/buckets/{bucket}/files?project={urllib.parse.quote(project)}"),
            "anonymousBucketSettings": public_probe(f"{base}/storage/buckets/{bucket}?project={urllib.parse.quote(project)}"),
        },
    }

    s3 = None
    arvan_objects: dict[str, dict[str, Any]] = {}
    arvan_settings: dict[str, Any] = {"status": "not authenticated; not inspected"}
    inventory_scope = "catalog keys probed anonymously"
    if authenticated_arvan:
        s3 = boto3.client(
            "s3",
            aws_access_key_id=arvan_access,
            aws_secret_access_key=arvan_secret,
            endpoint_url=arvan_endpoint,
            region_name=arvan_region,
            config=Config(
                signature_version="s3v4",
                s3={"addressing_style": "path"},
                connect_timeout=20,
                read_timeout=120,
                retries={"max_attempts": 4, "mode": "standard"},
            ),
        )
        for page in s3.get_paginator("list_objects_v2").paginate(Bucket=arvan_bucket, PaginationConfig={"PageSize": 1000}):
            for obj in page.get("Contents", []):
                arvan_objects[str(obj["Key"])] = {
                    "Size": int(obj.get("Size") or 0),
                    "ETag": str(obj.get("ETag", "")).strip('"'),
                    "LastModified": obj.get("LastModified").isoformat() if obj.get("LastModified") else None,
                    "StorageClass": obj.get("StorageClass"),
                }
        inventory_scope = "full authenticated bucket listing"
        arvan_settings = {
            "acl": summarize_s3_setting(lambda: s3.get_bucket_acl(Bucket=arvan_bucket)),
            "cors": summarize_s3_setting(lambda: s3.get_bucket_cors(Bucket=arvan_bucket)),
            "encryption": summarize_s3_setting(lambda: s3.get_bucket_encryption(Bucket=arvan_bucket)),
            "versioning": summarize_s3_setting(lambda: s3.get_bucket_versioning(Bucket=arvan_bucket)),
            "policyStatus": summarize_s3_setting(lambda: s3.get_bucket_policy_status(Bucket=arvan_bucket)),
            "publicAccessBlock": summarize_s3_setting(lambda: s3.get_public_access_block(Bucket=arvan_bucket)),
            "ownershipControls": summarize_s3_setting(lambda: s3.get_bucket_ownership_controls(Bucket=arvan_bucket)),
            "lifecycle": summarize_s3_setting(lambda: s3.get_bucket_lifecycle_configuration(Bucket=arvan_bucket)),
            "website": summarize_s3_setting(lambda: s3.get_bucket_website(Bucket=arvan_bucket)),
        }
    else:
        arvan_objects = probe_catalog_arvan_objects(arvan_public, items)

    result["arvan"] = {
        "endpoint": arvan_endpoint,
        "publicEndpoint": arvan_public,
        "bucketName": arvan_bucket,
        "authenticatedSettings": authenticated_arvan,
        "inventoryScope": inventory_scope,
        "objectCount": len(arvan_objects) if authenticated_arvan else None,
        "catalogObjectsProbed": len(arvan_objects),
        "totalBytes": sum(item["Size"] for item in arvan_objects.values()),
        "topLevelPrefixes": dict(sorted(Counter(key.split("/", 1)[0] for key in arvan_objects).items())),
        "settings": arvan_settings,
        "public": {
            "anonymousList": public_probe(arvan_public + "/?list-type=2&max-keys=1"),
        },
    }

    # Catalog metadata coverage, using the exact two addresses selected by the app.
    coverage: list[dict[str, Any]] = []
    for item in items:
        file_id = str(item.get("aw", ""))
        arvan_key = str(item.get("key", ""))
        aw = aw_files.get(file_id)
        ar = arvan_objects.get(arvan_key)
        coverage.append(
            {
                "id": item.get("id"),
                "kind": item.get("kind"),
                "appwriteId": file_id,
                "arvanKey": arvan_key,
                "appwriteExists": aw is not None,
                "arvanExists": ar is not None,
                "appwriteSize": int(aw.get("sizeOriginal") or 0) if aw else None,
                "arvanSize": int(ar.get("Size") or 0) if ar else None,
                "sizeEqual": bool(aw and ar and int(aw.get("sizeOriginal") or 0) == int(ar.get("Size") or 0)),
            }
        )
    result["catalogCoverage"] = {
        "entries": coverage,
        "appwritePresent": sum(row["appwriteExists"] for row in coverage),
        "arvanPresent": sum(row["arvanExists"] for row in coverage),
        "bothPresent": sum(row["appwriteExists"] and row["arvanExists"] for row in coverage),
        "bothSameSize": sum(row["sizeEqual"] for row in coverage),
        "missingAppwrite": [row["id"] for row in coverage if not row["appwriteExists"]],
        "missingArvan": [row["id"] for row in coverage if not row["arvanExists"]],
        "sizeMismatches": [row["id"] for row in coverage if row["appwriteExists"] and row["arvanExists"] and not row["sizeEqual"]],
    }

    # Diagnose the flat-ID mirror produced by scripts/sync_appwrite_to_arvan.py,
    # separately from the hierarchical catalog keys actually used by the app.
    if authenticated_arvan:
        flat_present = [file_id for file_id in aw_files if file_id in arvan_objects]
        result["flatMirror"] = {
            "status": "inspected",
            "appwriteIdsAlsoAtArvanRoot": len(flat_present),
            "appwriteIdsMissingAtArvanRoot": sorted(
                redact_file_id(file_id) for file_id in aw_files if file_id not in arvan_objects
            ),
            "sizeMismatches": sorted(
                redact_file_id(file_id)
                for file_id in flat_present
                if int(aw_files[file_id].get("sizeOriginal") or 0) != int(arvan_objects[file_id]["Size"])
            ),
        }
    else:
        result["flatMirror"] = {"status": "not inspected; authenticated Arvan listing unavailable"}

    supplied_html_key = os.getenv("HTML_MEDIA_KEY_B64", "").strip()
    html_key: bytes | None = None
    key_status = "unavailable"
    if supplied_html_key:
        try:
            candidate = base64.b64decode(supplied_html_key, validate=True)
            if len(candidate) == 32:
                html_key = candidate
                key_status = "available from ephemeral audit environment (used in memory only)"
        except Exception:
            pass
    if html_key is None:
        html_key, key_status = (
            extract_html_key(base, project, api_key, database)
            if authenticated_appwrite
            else (None, "unavailable; authenticated Appwrite access not supplied")
        )
    result["htmlKey"] = {"status": key_status, "includedInOutput": False}

    archive_dir = Path(args.archive_dir) if args.archive_dir else None
    print(f"Auditing {len(html_items)} encrypted HTML pairs with {WORKERS} workers...", flush=True)
    html_results: list[dict[str, Any]] = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS) as pool:
        futures = {
            pool.submit(
                audit_html_item,
                item,
                base,
                project,
                bucket,
                arvan_public,
                html_key,
                archive_dir,
                s3,
                arvan_bucket,
            ): item["id"]
            for item in html_items
        }
        completed = 0
        for future in concurrent.futures.as_completed(futures):
            item_id = futures[future]
            try:
                html_results.append(future.result())
            except Exception as exc:
                html_results.append({"id": item_id, "fatalError": compact_error(exc)})
            completed += 1
            print(f"  {completed}/{len(html_items)}", flush=True)
    html_results.sort(key=lambda row: str(row.get("id", "")))

    def dec_ok(row: dict[str, Any], origin: str) -> bool:
        return bool((row.get("decryption") or {}).get(origin, {}).get("ok"))

    remote_hosts: Counter[str] = Counter()
    relative_ref_items: list[str] = []
    for row in html_results:
        check = (row.get("decryption") or {}).get("appwrite") or {}
        remote_hosts.update(check.get("remoteHosts") or {})
        if (check.get("referenceCounts") or {}).get("relative", 0):
            relative_ref_items.append(str(row.get("id")))
    result["htmlAudit"] = {
        "items": html_results,
        "appwriteDownloaded": sum((row.get("appwrite") or {}).get("status") in range(200, 300) for row in html_results),
        "arvanDownloaded": sum((row.get("arvan") or {}).get("status") in range(200, 300) for row in html_results),
        "arvanAuthenticatedDownloaded": sum(
            (row.get("arvan") or {}).get("status") in range(200, 300)
            or (row.get("arvanAuthenticatedRead") or {}).get("status") in range(200, 300)
            for row in html_results
        ),
        "arvanNotPubliclyReadable": [row.get("id") for row in html_results if row.get("arvanPubliclyReadable") is False],
        "ciphertextEqual": sum(row.get("ciphertextEqual") is True for row in html_results),
        "appwriteDecryptValid": sum(dec_ok(row, "appwrite") for row in html_results),
        "arvanDecryptValid": sum(dec_ok(row, "arvan") for row in html_results),
        "plaintextEqual": sum((row.get("decryption") or {}).get("plaintextEqual") is True for row in html_results),
        "remoteHosts": dict(sorted(remote_hosts.items())),
        "itemsWithRelativeReferences": relative_ref_items,
    }

    sample = html_items[0] if html_items else None
    if sample:
        aw_url = public_appwrite_url(base, project, bucket, sample["aw"])
        ar_url = public_arvan_url(arvan_public, sample["key"])
        result["transportProbe"] = {
            "sampleId": sample["id"],
            "appwriteHead": public_probe(aw_url, method="HEAD"),
            "arvanHead": public_probe(ar_url, method="HEAD"),
            "appwriteRange": public_probe(aw_url, method="GET", byte_range="bytes=0-1023"),
            "arvanRange": public_probe(ar_url, method="GET", byte_range="bytes=0-1023"),
        }

    # Include only compact inventory records needed to investigate extras and
    # permission/configuration patterns.  No content bytes are persisted.
    result["inventory"] = {
        "appwrite": [sanitize_appwrite_file(item) for item in sorted(aw_files.values(), key=lambda value: str(value.get("$id", "")))],
        "arvan": [{"Key": key, **arvan_objects[key]} for key in sorted(arvan_objects)],
    }
    return result


def render_summary(report: dict[str, Any]) -> str:
    cat = report["catalog"]
    aw = report["appwrite"]
    ar = report["arvan"]
    cov = report["catalogCoverage"]
    html = report["htmlAudit"]
    n = cat["htmlItems"]
    lines = [
        "# Hamyar content-server audit (read-only)",
        "",
        f"Generated: `{report['generatedAt']}`",
        "",
        "> This artifact contains derived metadata only. No credential or HTML media key is included.",
        "",
        "## Inventory",
        "",
        f"- Appwrite files: **{aw['fileCount']}**",
        f"- Arvan inventory scope: **{ar['inventoryScope']}**",
        f"- Arvan objects in scope: **{ar['objectCount'] if ar['objectCount'] is not None else ar['catalogObjectsProbed']}**",
        f"- Catalog entries: **{cat['items']}** ({n} HTML)",
        f"- Catalog present on Appwrite: **{cov['appwritePresent']}/{cat['items']}**",
        f"- Catalog present at app-selected Arvan keys: **{cov['arvanPresent']}/{cat['items']}**",
        f"- Present on both with equal size: **{cov['bothSameSize']}/{cat['items']}**",
        "",
        "## Encrypted HTML validation",
        "",
        f"- Downloaded from Appwrite: **{html['appwriteDownloaded']}/{n}**",
        f"- Publicly downloadable from Arvan (the app path): **{html['arvanDownloaded']}/{n}**",
        f"- Read from Arvan with audit credentials: **{html.get('arvanAuthenticatedDownloaded', html['arvanDownloaded'])}/{n}**",
        f"- Byte-identical HMK1 ciphertext pairs: **{html['ciphertextEqual']}/{n}**",
        f"- Appwrite decrypt + UTF-8/HTML valid: **{html['appwriteDecryptValid']}/{n}**",
        f"- Arvan decrypt + UTF-8/HTML valid: **{html['arvanDecryptValid']}/{n}**",
        f"- Plaintext-identical pairs: **{html['plaintextEqual']}/{n}**",
        "",
        "## Exceptions",
        "",
        f"- Missing from Appwrite: `{cov['missingAppwrite']}`",
        f"- Missing from Arvan: `{cov['missingArvan']}`",
        f"- Catalog size mismatches: `{cov['sizeMismatches']}`",
        f"- HTML pages with relative references: `{html['itemsWithRelativeReferences']}`",
        f"- Remote hosts referenced by decrypted HTML: `{html['remoteHosts']}`",
        "",
        "See `audit.json` for settings, per-object inventory, and per-HTML validation details.",
        "",
    ]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", default="apps/hamyar-app/src/main/assets/content/catalog.json")
    parser.add_argument("--output", default="audit-output")
    parser.add_argument(
        "--archive-dir",
        default="",
        help="optional directory for public HMK1 Appwrite blobs used by an offline decrypt audit",
    )
    args = parser.parse_args()
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    try:
        report = run(args)
    except Exception as exc:
        failure = {
            "schemaVersion": 1,
            "generatedAt": iso_now(),
            "mode": "read-only",
            "secretsIncluded": False,
            "fatalError": compact_error(exc),
        }
        (output / "audit.json").write_text(json.dumps(failure, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Audit failed: {compact_error(exc)}", file=sys.stderr)
        return 1
    (output / "audit.json").write_text(json.dumps(json_safe(report), ensure_ascii=False, indent=2), encoding="utf-8")
    (output / "summary.md").write_text(render_summary(report), encoding="utf-8")
    print(render_summary(report), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
