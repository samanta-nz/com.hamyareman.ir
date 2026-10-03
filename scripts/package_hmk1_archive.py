#!/usr/bin/env python3
"""Build one HMK1-encrypted ZIP while preserving every path below a source folder.

The payload contract is exactly the one used by the Android HTML decoder:
    HMK1 + 12-byte random IV + AES-256-GCM ciphertext/tag

No clear-text lesson is written to the output ZIP and the encryption key is never
printed or included in the report.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

import requests
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

MAGIC = b"HMK1"


def html_key() -> bytes:
    """Fetch the existing live content key, falling back to the CI secret."""
    endpoint = os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
    project = os.environ.get("APPWRITE_PROJECT_ID", "")
    database = os.environ.get("APPWRITE_DATABASE_ID", "")
    api_key = os.environ.get("APPWRITE_API_KEY", "")
    if project and database and api_key:
        headers = {"X-Appwrite-Project": project, "X-Appwrite-Key": api_key}
        for route in (
            f"/tablesdb/{database}/tables/app_state/rows/html_media_key",
            f"/databases/{database}/collections/app_state/documents/html_media_key",
        ):
            try:
                response = requests.get(endpoint + route, headers=headers, timeout=(20, 60))
                if response.status_code != 200:
                    continue
                payload = response.json().get("payload") or "{}"
                encoded = (json.loads(payload) if isinstance(payload, str) else payload).get("b", "")
                decoded = base64.b64decode(encoded)
                if len(decoded) == 32:
                    return decoded
            except Exception:  # noqa: BLE001 - try the safe CI fallback below
                continue
    try:
        decoded = base64.b64decode(os.environ.get("HTML_MEDIA_KEY_B64", ""))
    except Exception:  # noqa: BLE001
        decoded = b""
    if len(decoded) != 32:
        raise SystemExit("کلید ۳۲ بایتی HTML از Appwrite یا secret CI خوانده نشد.")
    return decoded


def digest_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--prefix", default="0000", help="Top-level path inside the ZIP")
    parser.add_argument("--report", required=True, type=Path)
    parser.add_argument("--source-revision", default="")
    args = parser.parse_args()

    source = args.source.resolve()
    if not source.is_dir():
        raise SystemExit(f"پوشهٔ منبع پیدا نشد: {source}")
    files = sorted(item for item in source.rglob("*") if item.is_file())
    if not files:
        raise SystemExit("فایلی برای بسته‌بندی نیست.")
    unexpected = [item for item in files if item.suffix.lower() != ".html"]
    if unexpected:
        names = ", ".join(item.relative_to(source).as_posix() for item in unexpected[:5])
        raise SystemExit(f"فایل غیر HTML در پوشه پیدا شد و عمداً جا نماند: {names}")

    key = html_key()
    cipher = AESGCM(key)
    prefix = args.prefix.strip("/")
    args.output.parent.mkdir(parents=True, exist_ok=True)

    total_plain = 0
    total_encrypted = 0
    entries: list[str] = []
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_STORED, allowZip64=True) as archive:
        for item in files:
            plain = item.read_bytes()
            iv = os.urandom(12)
            encrypted = MAGIC + iv + cipher.encrypt(iv, plain, None)
            # Verify the same in-memory bytes that will be placed in the archive.
            if cipher.decrypt(encrypted[4:16], encrypted[16:], None) != plain:
                raise SystemExit(f"راستی‌آزمایی رمزگذاری ناموفق بود: {item.name}")
            relative = item.relative_to(source).as_posix()
            arcname = f"{prefix}/{relative}" if prefix else relative
            archive.writestr(arcname, encrypted)
            entries.append(arcname)
            total_plain += len(plain)
            total_encrypted += len(encrypted)

    # Verify paths and HMK1 headers after writing; never extract plaintext to disk.
    with zipfile.ZipFile(args.output) as archive:
        if archive.namelist() != entries:
            raise SystemExit("نام یا ترتیب مسیرهای ZIP تغییر کرده است.")
        for name in entries:
            if not archive.read(name).startswith(MAGIC):
                raise SystemExit(f"هدر HMK1 برای {name} معتبر نیست.")

    report = {
        "sourceRevision": args.source_revision,
        "prefix": prefix,
        "files": len(entries),
        "paths": entries,
        "plainBytes": total_plain,
        "encryptedBytes": total_encrypted,
        "hmk1OverheadBytes": total_encrypted - total_plain,
        "zip": args.output.name,
        "zipBytes": args.output.stat().st_size,
        "zipSha256": digest_file(args.output),
        "verified": True,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: report[key] for key in ("files", "zip", "zipBytes", "zipSha256", "verified")}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
