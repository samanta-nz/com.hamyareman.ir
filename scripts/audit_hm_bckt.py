#!/usr/bin/env python3
"""Read-only audit of the current HM_BCKT Appwrite bucket.

The audit lists files, checks catalog coverage, probes only a tiny prefix of each
HTML object, and verifies that the HTML media-key row exists. It never uploads,
changes, decrypts, or prints credentials/key material.
"""
from __future__ import annotations

import argparse
import json
import os
import pathlib
import urllib.parse
from collections import Counter
from typing import Any

import requests

ENDPOINT = os.getenv("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
PROJECT = os.getenv("APPWRITE_PROJECT_ID", "6abb134a002025222005")
BUCKET = os.getenv("APPWRITE_BUCKET_ID", "6abb564d00155cc56d65")
DATABASE = os.getenv("APPWRITE_DATABASE_ID", "6abb238d000d05730d10")
API_KEY = os.getenv("APPWRITE_API_KEY", "").strip()
HMK1 = b"HMK1"


def query(method: str, *values: Any) -> str:
    return json.dumps({"method": method, "values": list(values)}, separators=(",", ":"))


def headers(with_key: bool = True) -> dict[str, str]:
    h = {"X-Appwrite-Project": PROJECT, "Accept-Encoding": "identity"}
    if with_key and API_KEY:
        h["X-Appwrite-Key"] = API_KEY
    return h


def get_json(path: str, *, params: list[tuple[str, str]] | None = None) -> dict[str, Any]:
    r = requests.get(ENDPOINT + path, headers=headers(), params=params, timeout=(20, 90))
    r.raise_for_status()
    return r.json()


def list_files() -> list[dict[str, Any]]:
    found: dict[str, dict[str, Any]] = {}
    offset = 0
    total: int | None = None
    while total is None or offset < total:
        body = get_json(
            f"/storage/buckets/{BUCKET}/files",
            params=[("queries[]", query("limit", 100)), ("queries[]", query("offset", offset))],
        )
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
            raise RuntimeError("Appwrite pagination repeated a page")
        offset += len(page)
    if total is not None and len(found) != total:
        raise RuntimeError(f"incomplete inventory: unique={len(found)}, total={total}")
    return list(found.values())


def public_url(file_id: str) -> str:
    fid = urllib.parse.quote(file_id, safe="")
    project = urllib.parse.quote(PROJECT, safe="")
    return f"{ENDPOINT}/storage/buckets/{BUCKET}/files/{fid}/view?project={project}"


def prefix_probe(file_id: str, public: bool) -> dict[str, Any]:
    h = {"Accept-Encoding": "identity", "Range": "bytes=0-31"}
    if not public:
        h.update(headers())
    try:
        with requests.get(public_url(file_id), headers=h, stream=True, timeout=(15, 45)) as r:
            prefix = next(r.iter_content(32), b"")[:32] if r.status_code in (200, 206) else b""
            return {
                "status": r.status_code,
                "contentRange": r.headers.get("Content-Range", ""),
                "contentType": r.headers.get("Content-Type", ""),
                "hmk1": prefix.startswith(HMK1),
                "bytesRead": len(prefix),
            }
    except Exception as exc:  # derived diagnostics only
        return {"status": 0, "error": type(exc).__name__, "hmk1": False, "bytesRead": 0}


def catalog_expected(root: pathlib.Path) -> tuple[dict[str, str], set[str]]:
    catalog = json.loads((root / "apps/hamyar-app/src/main/assets/content/catalog.json").read_text())
    server_map = json.loads((root / "apps/hamyar-app/src/main/assets/content/server-map.json").read_text())
    expected: dict[str, str] = {}
    html_ids: set[str] = set()
    for item in catalog.get("items", []):
        fid = str(item.get("aw") or item.get("id") or "")
        if not fid:
            continue
        expected[fid] = str(item.get("key") or "")
        if item.get("kind") == "html":
            html_ids.add(fid)
    for item in server_map.get("entries", []):
        fid = str(item.get("id") or "")
        key = str(item.get("key") or "")
        if not fid:
            continue
        expected.setdefault(fid, key)
        if key.lower().endswith(('.html', '.htm', '.hmk1')):
            html_ids.add(fid)
    return expected, html_ids


def media_key_status() -> dict[str, Any]:
    path = f"/tablesdb/{DATABASE}/tables/app_state/rows/html_media_key"
    try:
        row = get_json(path)
        payload = json.loads(row.get("payload") or "{}")
        b64 = str(payload.get("b") or "")
        return {
            "exists": True,
            "encodedLength": len(b64),
            "looksPresent": len(b64) >= 40,
            "permissions": row.get("$permissions") or [],
        }
    except requests.HTTPError as exc:
        return {"exists": False, "status": exc.response.status_code if exc.response else 0}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="hm-bckt-audit")
    args = parser.parse_args()
    if not API_KEY:
        raise SystemExit("APPWRITE_API_KEY is required")

    root = pathlib.Path(__file__).resolve().parents[1]
    output = pathlib.Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    bucket_meta = get_json(f"/storage/buckets/{BUCKET}")
    files = list_files()
    by_id = {str(item.get("$id")): item for item in files}
    by_name = {str(item.get("name")): item for item in files}
    expected, html_ids = catalog_expected(root)

    resolved: dict[str, dict[str, Any]] = {}
    missing: list[str] = []
    for fid, path in expected.items():
        item = by_id.get(fid) or by_name.get(fid) or by_name.get(path) or by_name.get(path.rsplit('/', 1)[-1])
        if item is None:
            missing.append(fid)
        else:
            resolved[fid] = item

    html_results = []
    for fid in sorted(html_ids):
        item = resolved.get(fid)
        if item is None:
            html_results.append({"id": fid, "present": False})
            continue
        actual_id = str(item.get("$id"))
        public = prefix_probe(actual_id, public=True)
        authenticated = public if public.get("status") in (200, 206) else prefix_probe(actual_id, public=False)
        html_results.append({
            "id": fid,
            "actualId": actual_id,
            "name": str(item.get("name") or ""),
            "size": int(item.get("sizeOriginal") or 0),
            "permissions": item.get("$permissions") or [],
            "public": public,
            "authenticated": authenticated,
        })

    names = [str(f.get("name") or "") for f in files]
    folder_fields = sorted({k for f in files for k in f if "folder" in k.lower() or "path" in k.lower()})
    public_ok = sum(1 for r in html_results if r.get("public", {}).get("status") in (200, 206))
    hmk_ok = sum(1 for r in html_results if r.get("authenticated", {}).get("hmk1"))
    present_html = sum(1 for r in html_results if r.get("present", True))
    result = {
        "bucket": {
            "id": BUCKET,
            "name": bucket_meta.get("name"),
            "enabled": bucket_meta.get("enabled"),
            "fileSecurity": bucket_meta.get("fileSecurity"),
            "permissions": bucket_meta.get("$permissions") or [],
            "totalFiles": len(files),
        },
        "folderSupport": {
            "apiFolderOrPathFields": folder_fields,
            "directoryLikeNames": sum('/' in n for n in names),
            "conclusion": (
                "native-metadata" if folder_fields else
                "directory-like-file-names" if any('/' in n for n in names) else
                "flat-storage"
            ),
        },
        "catalog": {
            "expectedFiles": len(expected),
            "resolvedFiles": len(resolved),
            "missingCount": len(missing),
            "missingIds": missing,
            "expectedHtml": len(html_ids),
            "presentHtml": present_html,
            "publicHtml": public_ok,
            "validHmk1Prefixes": hmk_ok,
        },
        "mediaKey": media_key_status(),
        "mimeTypes": dict(Counter(str(f.get("mimeType") or "") for f in files)),
        "html": html_results,
    }
    (output / "audit.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    summary = [
        "# HM_BCKT read-only audit",
        "",
        f"- Bucket: `{BUCKET}` / `{result['bucket']['name']}`",
        f"- Files: **{len(files)}**",
        f"- Catalog coverage: **{len(resolved)}/{len(expected)}**",
        f"- HTML present: **{present_html}/{len(html_ids)}**",
        f"- HTML publicly readable: **{public_ok}/{len(html_ids)}**",
        f"- HMK1 prefix valid: **{hmk_ok}/{len(html_ids)}**",
        f"- Folder model: **{result['folderSupport']['conclusion']}**",
        f"- HTML media key row present: **{result['mediaKey'].get('looksPresent', False)}**",
        "",
        "The report contains metadata and status only; it contains no key material.",
    ]
    (output / "summary.md").write_text("\n".join(summary) + "\n")
    print(json.dumps({
        "bucket": BUCKET,
        "files": len(files),
        "catalog": f"{len(resolved)}/{len(expected)}",
        "html": f"{present_html}/{len(html_ids)}",
        "publicHtml": f"{public_ok}/{len(html_ids)}",
        "hmk1": f"{hmk_ok}/{len(html_ids)}",
        "folderModel": result["folderSupport"]["conclusion"],
        "mediaKey": result["mediaKey"].get("looksPresent", False),
    }, ensure_ascii=False, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
