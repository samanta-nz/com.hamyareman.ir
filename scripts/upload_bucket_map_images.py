#!/usr/bin/env python3
"""Upload the 189 CSV-mapped practice-cover images from Books-Data to ParsPack.

Credentials are read only from environment variables; never print or persist them.
Object keys intentionally match the attached map: assets/practice-covers/<filename>.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import requests
from botocore.exceptions import ClientError

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from s3_targets import target  # noqa: E402

EXPECTED_COUNT = 189


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def is_missing_object(exc: ClientError) -> bool:
    response = exc.response or {}
    code = str((response.get("Error") or {}).get("Code", ""))
    status = (response.get("ResponseMetadata") or {}).get("HTTPStatusCode")
    return code in {"404", "NoSuchKey", "NotFound"} or status == 404


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    source = args.source
    files = sorted(p for p in source.glob("*.jpg") if p.is_file())
    if len(files) != EXPECTED_COUNT:
        raise SystemExit(
            f"Refusing upload: expected {EXPECTED_COUNT} JPGs under {source}, found {len(files)}."
        )

    dst = target("parspack")
    client = dst.client()
    args.out.parent.mkdir(parents=True, exist_ok=True)
    report = [
        "# ParsPack practice-cover upload",
        "",
        f"- Source: `{source.as_posix()}`",
        f"- Expected files: {EXPECTED_COUNT}",
        f"- Public base: `{dst.public_url_style}`",
        "",
    ]
    results: list[dict] = []

    def transfer(path: Path) -> dict:
        key = f"assets/practice-covers/{path.name}"
        size = path.stat().st_size
        sha = digest(path)
        existing = None
        try:
            existing = client.head_object(Bucket=dst.bucket, Key=key)
        except ClientError as exc:
            if not is_missing_object(exc):
                raise

        meta_sha = ((existing or {}).get("Metadata") or {}).get("sha256", "")
        if existing and int(existing.get("ContentLength") or -1) == size and meta_sha == sha:
            status = "skipped-identical"
        else:
            body = path.read_bytes()
            params = {
                "Bucket": dst.bucket,
                "Key": key,
                "Body": body,
                "ContentLength": size,
                "ContentType": mimetypes.guess_type(path.name)[0] or "image/jpeg",
                "CacheControl": "public, max-age=31536000, immutable",
                "Metadata": {"sha256": sha},
            }
            try:
                client.put_object(ACL="public-read", **params)
            except ClientError:
                client.put_object(**params)
            written = client.head_object(Bucket=dst.bucket, Key=key)
            written_sha = ((written.get("Metadata") or {}).get("sha256", ""))
            if int(written.get("ContentLength") or -1) != size or written_sha != sha:
                raise RuntimeError(f"Destination integrity check failed: {key}")
            status = "uploaded"

        url = dst.public_url(key)
        response = requests.get(
            url,
            headers={"Range": "bytes=0-1023", "Accept-Encoding": "identity"},
            timeout=(10, 30),
        )
        if response.status_code not in (200, 206) or not response.content:
            raise RuntimeError(f"Public read failed: {key} HTTP {response.status_code}")
        return {
            "key": key,
            "size": size,
            "sha256": sha,
            "status": status,
            "publicUrl": url,
            "publicHttp": response.status_code,
        }

    failures = []
    # A modest worker pool keeps the 189-file transfer quick but avoids flooding the endpoint.
    with ThreadPoolExecutor(max_workers=6) as pool:
        future_map = {pool.submit(transfer, p): p.name for p in files}
        for future in as_completed(future_map):
            name = future_map[future]
            try:
                item = future.result()
                results.append(item)
                print(f"{item['status']} {item['key']} ({item['size']} bytes), public HTTP {item['publicHttp']}")
            except Exception as exc:  # noqa: BLE001
                failures.append({"name": name, "error": f"{type(exc).__name__}: {exc}"})
                print(f"FAILED {name}: {type(exc).__name__}: {exc}")

    results.sort(key=lambda x: x["key"])
    counts = {}
    for item in results:
        counts[item["status"]] = counts.get(item["status"], 0) + 1
        report.append(
            f"- ✅ `{item['key']}` — {item['status']}; {item['size']} bytes; public HTTP {item['publicHttp']}"
        )
    report.extend(["", f"## Result", "", f"- Successful and publicly readable: {len(results)}/{EXPECTED_COUNT}", f"- Failures: {len(failures)}", f"- Counts: `{json.dumps(counts, ensure_ascii=False, sort_keys=True)}`"])
    if failures:
        report.extend(["", "## Failures", ""])
        report.extend(f"- ❌ `{x['name']}`: {x['error']}" for x in failures)
    args.out.write_text("\n".join(report) + "\n", encoding="utf-8")
    print(f"SUMMARY: public_readable={len(results)}/{EXPECTED_COUNT}, failures={len(failures)}")
    return 1 if failures or len(results) != EXPECTED_COUNT else 0


if __name__ == "__main__":
    raise SystemExit(main())
