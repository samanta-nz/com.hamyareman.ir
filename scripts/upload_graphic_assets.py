#!/usr/bin/env python3
"""Upload generated New-graphic visual assets to ParsPack and verify public reads.

Only the explicit six design assets are synchronized. The operation is resumable:
objects whose stored sha256 already matches are skipped; changed/missing objects are
uploaded with public-read ACL when supported. Secrets stay in GitHub Actions env.
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

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "apps/hamyar-app/src/main/assets"
ASSETS = (
    "lock/app-lock-bg.jpg",
    "lock/safespace-lock-bg.jpg",
    "diary/desk-bg.jpg",
    "poetry/poetry-bg.jpg",
    "album/album-bg.jpg",
    "diary/paper-cream.jpg",
)


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, default=Path("new-graphic-session/parspack/graphic-assets.md"))
    parser.add_argument("--json-out", type=Path, default=Path("new-graphic-session/parspack/graphic-assets.json"))
    args = parser.parse_args()

    dst = target("parspack")
    client = dst.client()
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.json_out.parent.mkdir(parents=True, exist_ok=True)

    report = ["# New-graphic ParsPack asset sync", "", f"Destination: {dst.public_url_style} public URLs", ""]
    result = {"bucket": dst.bucket, "publicBase": dst.endpoint, "assets": []}
    failures = 0

    for relative in ASSETS:
        local = ASSET_ROOT / relative
        key = "assets/" + relative
        item = {"key": key, "local": str(local.relative_to(ROOT)), "status": ""}
        if not local.exists() or local.stat().st_size < 5000:
            item["status"] = "missing-local"
            report.append(f"- ❌ `{key}` — فایل محلی موجود/معتبر نیست")
            result["assets"].append(item)
            failures += 1
            continue

        sha = digest(local)
        size = local.stat().st_size
        existing = None
        try:
            existing = client.head_object(Bucket=dst.bucket, Key=key)
        except ClientError:
            existing = None

        stored_sha = ((existing or {}).get("Metadata") or {}).get("sha256", "")
        if existing and int(existing.get("ContentLength") or -1) == size and stored_sha == sha:
            item["status"] = "skipped"
            report.append(f"- ✅ `{key}` — از قبل همسان است ({size} bytes)")
        else:
            body = local.read_bytes()
            content_type = mimetypes.guess_type(key)[0] or "application/octet-stream"
            params = {
                "Bucket": dst.bucket,
                "Key": key,
                "Body": body,
                "ContentType": content_type,
                "CacheControl": "public, max-age=31536000, immutable",
                "Metadata": {"sha256": sha},
            }
            try:
                client.put_object(ACL="public-read", **params)
            except ClientError:
                client.put_object(**params)
            written = client.head_object(Bucket=dst.bucket, Key=key)
            if int(written.get("ContentLength") or -1) != size:
                raise RuntimeError(f"size mismatch after upload for {key}")
            item["status"] = "uploaded"
            report.append(f"- ⬆️ `{key}` — آپلود شد ({size} bytes)")

        public_url = dst.public_url(key)
        try:
            response = requests.get(public_url, headers={"Range": "bytes=0-1023"}, timeout=30)
            ok = response.status_code in (200, 206) and len(response.content) > 0
        except Exception as exc:
            ok = False
            response = None
            item["error"] = type(exc).__name__
        item["publicUrl"] = public_url
        item["publicRead"] = ok
        if ok:
            report.append(f"  - 🌐 خواندن عمومی: HTTP {response.status_code}")
        else:
            report.append(f"  - ❌ خواندن عمومی ناموفق است")
            failures += 1
        result["assets"].append(item)

    result["failed"] = failures
    args.out.write_text("\n".join(report) + "\n", encoding="utf-8")
    args.json_out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print("\n".join(report))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())