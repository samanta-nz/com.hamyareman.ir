#!/usr/bin/env python3
"""اعلام یک نسخهٔ تازهٔ «همیار من نهم» در ردیف app_release_grade9.

قرارداد پروژه (RELEASES.md + UpdatePlan.kt):
  * هر پایه یک ردیف مستقل در جدول `app_state` دارد؛ اینجا فقط `grade9` عوض می‌شود.
  * `latest` باید از مقدار فعلی بزرگ‌تر باشد، وگرنه انتشار رد می‌شود.
  * `min` بزرگ‌تر از صفر یعنی نصب‌های پایین‌تر از آن **اجباری** آپدیت می‌شوند.
  * `externalUrl` (مخزن عمومی گیت‌هاب) و `internalUrl` (پارس‌پک) هر دو باید پیش از
    عوض‌شدن ردیف، ناشناس و بدون توکن قابل دانلود باشند — وگرنه کاربر با آپدیت
    اجباری پشت یک لینک خراب گیر می‌افتد.

کلیدها فقط از محیط رانر خوانده می‌شوند و هیچ‌وقت چاپ یا ذخیره نمی‌شوند.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

APPWRITE_ENDPOINT = "https://sgp.cloud.appwrite.io/v1"
DATABASE_ID = "6abb238d000d05730d10"
TABLE_ID = "app_state"
DEFAULT_PROJECT = "6abb134a002025222005"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def appwrite_headers(write: bool = False) -> dict[str, str]:
    key = os.getenv("APPWRITE_API_KEY", "").strip()
    if not key:
        raise SystemExit("متغیر محیطی APPWRITE_API_KEY تنظیم نشده است.")
    headers = {
        "X-Appwrite-Project": os.getenv("APPWRITE_PROJECT_ID", "").strip() or DEFAULT_PROJECT,
        "X-Appwrite-Key": key,
    }
    if write:
        headers["Content-Type"] = "application/json"
    return headers


def row_url(row_id: str) -> str:
    return f"{APPWRITE_ENDPOINT}/tablesdb/{DATABASE_ID}/tables/{TABLE_ID}/rows/{row_id}"


def get_row(row_id: str) -> dict:
    request = urllib.request.Request(row_url(row_id), headers=appwrite_headers())
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def patch_row(row_id: str, payload: dict) -> int:
    body = json.dumps(
        {
            "data": {
                "userId": "global",
                "key": row_id,
                "payload": json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
                "updatedAt": int(time.time() * 1000),
            },
            "permissions": ['read("users")'],
        },
        ensure_ascii=False,
    ).encode("utf-8")
    request = urllib.request.Request(
        row_url(row_id), data=body, method="PATCH", headers=appwrite_headers(write=True)
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.status


def probe(url: str, expected_size: int) -> tuple[bool, str]:
    """دانلودپذیریِ ناشناس (بدون هیچ هدر احراز هویت) را ثابت می‌کند."""
    try:
        request = urllib.request.Request(url, method="GET", headers={"Range": "bytes=0-1023"})
        with urllib.request.urlopen(request, timeout=120) as response:
            status = response.status
            head = response.read(1024)
            length = response.headers.get("Content-Range") or response.headers.get("Content-Length")
    except urllib.error.HTTPError as exc:
        return False, f"HTTP {exc.code}"
    except Exception as exc:  # noqa: BLE001 — گزارش می‌شود، پنهان نمی‌شود
        return False, f"{type(exc).__name__}: {exc}"
    if status not in (200, 206):
        return False, f"HTTP {status}"
    if not head.startswith(b"PK"):
        return False, "پاسخ یک فایل APK/ZIP نیست"
    total = ""
    if length and "/" in str(length):
        total = str(length).rsplit("/", 1)[-1]
        if total.isdigit() and int(total) != expected_size:
            return False, f"اندازه نمی‌خواند: {total} ≠ {expected_size}"
    return True, f"HTTP {status}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--version-code", type=int, required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--min-code", type=int, default=0)
    parser.add_argument("--rollout", type=int, default=100)
    parser.add_argument("--notes", default="", help="با | جدا می‌شود")
    parser.add_argument("--external-url", default="")
    parser.add_argument("--internal-url", default="")
    parser.add_argument("--signer-sha256", required=True)
    parser.add_argument("--row-id", default="app_release_grade9")
    parser.add_argument("--package", default="com.hamyareman.p09")
    parser.add_argument("--grade-id", default="grade9")
    parser.add_argument("--out", type=Path, default=Path("ci-report/release.json"))
    parser.add_argument("--dry-run", action="store_true", help="فقط بررسی کن، ردیف را عوض نکن")
    args = parser.parse_args()

    if not args.apk.is_file():
        raise SystemExit(f"فایل پیدا نشد: {args.apk}")
    size = args.apk.stat().st_size
    digest = sha256_file(args.apk)

    if not args.external_url and not args.internal_url:
        raise SystemExit("دست‌کم یکی از external-url / internal-url لازم است.")

    result: dict = {
        "rowId": args.row_id,
        "versionCode": args.version_code,
        "versionName": args.version_name,
        "min": args.min_code,
        "forced": args.min_code > 0,
        "size": size,
        "sha256": digest,
        "externalUrl": args.external_url,
        "internalUrl": args.internal_url,
        "checks": {},
    }

    current_row = get_row(args.row_id)
    current_payload = json.loads(current_row.get("payload") or "{}")
    current_latest = int(current_payload.get("latest") or 0)
    result["previous"] = {
        "latest": current_latest,
        "name": current_payload.get("name", ""),
        "min": current_payload.get("min", 0),
    }
    if args.version_code <= current_latest:
        raise SystemExit(
            f"versionCode باید بزرگ‌تر شود: فعلی {current_latest} → درخواستی {args.version_code}"
        )

    for label, url in (("external", args.external_url), ("internal", args.internal_url)):
        if not url:
            result["checks"][label] = "تنظیم نشده"
            continue
        ok, detail = probe(url, size)
        result["checks"][label] = detail
        if not ok:
            args.out.parent.mkdir(parents=True, exist_ok=True)
            args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
            raise SystemExit(f"لینک {label} ناشناس قابل دانلود نیست ({detail}): {url}")

    payload = {
        "latest": args.version_code,
        "name": args.version_name,
        "min": args.min_code,
        "url": args.external_url or args.internal_url,
        "externalUrl": args.external_url,
        "internalUrl": args.internal_url,
        "size": size,
        "sha256": digest,
        "chan": "stable",
        "rollout": max(1, min(100, args.rollout)),
        "notes": [x.strip() for x in args.notes.split("|") if x.strip()],
        "packageName": args.package,
        "gradeId": args.grade_id,
        "signingSha256": args.signer_sha256.lower(),
    }
    result["payload"] = payload

    if args.dry_run:
        result["published"] = False
    else:
        patch_row(args.row_id, payload)
        verify = json.loads(get_row(args.row_id).get("payload") or "{}")
        result["published"] = (
            int(verify.get("latest") or 0) == args.version_code
            and int(verify.get("min") or 0) == args.min_code
        )
        if not result["published"]:
            args.out.parent.mkdir(parents=True, exist_ok=True)
            args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
            raise SystemExit("ردیف بعد از PATCH مقدار مورد انتظار را ندارد.")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
