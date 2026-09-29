#!/usr/bin/env python3
"""Credential-free smoke test of every app-selected Appwrite and Arvan URL."""
from __future__ import annotations

import argparse
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.parse import quote

import requests

ROOT = Path(__file__).resolve().parents[1]
MAP = ROOT / "apps/hamyar-app/src/main/assets/content/server-map.json"
APPWRITE = "https://fra.cloud.appwrite.io/v1/storage/buckets/6aa1eaae00303400117b/files"
PROJECT = "6a9d59e3002751cc3ea8"
ARVAN = "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir"


def total_size(response: requests.Response) -> int:
    content_range = response.headers.get("Content-Range", "")
    if "/" in content_range:
        return int(content_range.rsplit("/", 1)[1])
    return int(response.headers.get("Content-Length") or -1)


def probe(url: str, expected: int) -> dict[str, object]:
    try:
        with requests.get(
            url,
            headers={"Range": "bytes=0-7", "Accept-Encoding": "identity"},
            stream=True,
            timeout=(10, 45),
            allow_redirects=True,
        ) as response:
            prefix = next(response.iter_content(8), b"")[:8]
            size = total_size(response)
            ok = response.status_code in range(200, 300) and prefix != b"" and size == expected
            return {"ok": ok, "status": response.status_code, "size": size, "prefix": prefix.hex()}
    except requests.RequestException as error:
        return {"ok": False, "status": 0, "size": -1, "error": type(error).__name__}


def check(entry: dict[str, object]) -> dict[str, object]:
    file_id, key, size = str(entry["id"]), str(entry["key"]), int(entry["size"])
    appwrite_url = f"{APPWRITE}/{quote(file_id, safe='')}/view?project={PROJECT}"
    arvan_url = ARVAN + "/" + "/".join(quote(part, safe="") for part in key.split("/"))
    appwrite = probe(appwrite_url, size)
    arvan = probe(arvan_url, size)
    return {"id": file_id, "key": key, "expected": size, "appwrite": appwrite, "arvan": arvan}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--workers", type=int, default=16)
    parser.add_argument("--output")
    args = parser.parse_args()
    entries = json.loads(MAP.read_text())["entries"]
    results: list[dict[str, object]] = []
    with ThreadPoolExecutor(max_workers=max(1, min(args.workers, 32))) as pool:
        futures = [pool.submit(check, entry) for entry in entries]
        for future in as_completed(futures):
            results.append(future.result())
    results.sort(key=lambda item: str(item["id"]))
    failed = [
        item for item in results
        if not item["appwrite"]["ok"] or not item["arvan"]["ok"]  # type: ignore[index]
    ]
    report = {
        "checked": len(results),
        "appwriteHealthy": sum(bool(item["appwrite"]["ok"]) for item in results),  # type: ignore[index]
        "arvanHealthy": sum(bool(item["arvan"]["ok"]) for item in results),  # type: ignore[index]
        "failures": failed,
    }
    text = json.dumps(report, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(text + "\n")
    print(text)
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
