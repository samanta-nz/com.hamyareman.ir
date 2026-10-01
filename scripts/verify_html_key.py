#!/usr/bin/env python3
"""بررسی فقط-خواندنیِ کلید رمز محتوا (html_media_key).

این اسکریپت هیچ چیزی را روی سرور نمی‌سازد، عوض نمی‌کند و پاک نمی‌کند.
هرگز خودِ کلید را چاپ نمی‌کند؛ فقط اثر انگشت کوتاه SHA-256 را.

چه چیزی را می‌سنجد:
  ۱) ردیف app_state/html_media_key روی Appwrite سنگاپور هست یا نه.
  ۲) ساختار payload درست است و کلید دقیقاً ۳۲ بایت است.
  ۳) permission ردیف شامل read("users") هست (وگرنه کاربر عادی نمی‌خواندش).
  ۴) آیا همان کلید واقعاً فایل‌های روی باکت را باز می‌کند — هم پارس‌پک هم آروان.

متغیرهای محیطی:
  APPWRITE_ENDPOINT, APPWRITE_PROJECT_ID, APPWRITE_DATABASE_ID, APPWRITE_API_KEY
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import sys
import urllib.parse
from pathlib import Path

import requests
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

ENDPOINT = os.environ.get("APPWRITE_ENDPOINT", "https://sgp.cloud.appwrite.io/v1").rstrip("/")
PROJECT = os.environ.get("APPWRITE_PROJECT_ID", "6abb134a002025222005")
DATABASE = os.environ.get("APPWRITE_DATABASE_ID", "6abb238d000d05730d10")
API_KEY = os.environ.get("APPWRITE_API_KEY", "")

ROW_ID = "html_media_key"
TABLE = "app_state"
MAGIC = b"HMK1"

PARSPACK_BASE = "https://c539776.parspack.net"
ARVAN_BASE = "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir"

SERVER_MAP = Path("apps/hamyar-app/src/main/assets/content/server-map.json")


def fingerprint(raw: bytes) -> str:
    """اثر انگشت کوتاه — برای مقایسه، نه برای بازسازی کلید."""
    return hashlib.sha256(raw).hexdigest()[:12]


def fetch_key_row() -> dict:
    """ردیف کلید را با API key سرور می‌خواند (نه با سشن کاربر)."""
    out: dict = {"endpoint": ENDPOINT, "project": PROJECT, "database": DATABASE, "row": ROW_ID}
    if not API_KEY:
        out["error"] = "APPWRITE_API_KEY تنظیم نشده است."
        return out
    headers = {"X-Appwrite-Project": PROJECT, "X-Appwrite-Key": API_KEY}
    paths = [
        f"/tablesdb/{DATABASE}/tables/{TABLE}/rows/{ROW_ID}",
        f"/databases/{DATABASE}/collections/{TABLE}/documents/{ROW_ID}",
    ]
    for path in paths:
        try:
            r = requests.get(ENDPOINT + path, headers=headers, timeout=(20, 60))
        except Exception as exc:  # noqa: BLE001
            out.setdefault("attempts", []).append({"path": path, "error": str(exc)})
            continue
        out.setdefault("attempts", []).append({"path": path, "status": r.status_code})
        if r.status_code != 200:
            continue
        body = r.json()
        out["found"] = True
        out["api_path"] = path
        out["permissions"] = body.get("$permissions", [])
        out["updatedAt"] = body.get("updatedAt") or body.get("$updatedAt")
        payload = body.get("payload") or ""
        try:
            b64 = json.loads(payload).get("b", "")
        except Exception:  # noqa: BLE001
            out["payload_valid_json"] = False
            return out
        out["payload_valid_json"] = True
        out["payload_has_b"] = bool(b64)
        if not b64:
            return out
        try:
            raw = base64.b64decode(b64)
        except Exception:  # noqa: BLE001
            out["base64_valid"] = False
            return out
        out["base64_valid"] = True
        out["key_bytes"] = len(raw)
        out["key_ok"] = len(raw) == 32
        out["key_fingerprint"] = fingerprint(raw)
        out["_raw"] = raw
        return out
    out["found"] = False
    return out


def pick_samples(count: int) -> list[dict]:
    """کوچک‌ترین فایل‌های html را برمی‌دارد تا دانلود سبک بماند."""
    if not SERVER_MAP.exists():
        return []
    entries = json.loads(SERVER_MAP.read_text(encoding="utf-8")).get("entries", [])
    html = [e for e in entries if str(e.get("key", "")).lower().endswith(".html")]
    html.sort(key=lambda e: e.get("size", 0))
    return html[:count]


def try_decrypt(raw_key: bytes, blob: bytes) -> dict:
    res: dict = {"bytes": len(blob)}
    res["magic_hmk1"] = blob[:4] == MAGIC
    if not res["magic_hmk1"]:
        res["head_hex"] = blob[:8].hex()
        return res
    if raw_key is None:
        return res
    iv, ct = blob[4:16], blob[16:]
    try:
        plain = AESGCM(raw_key).decrypt(iv, ct, None)
    except Exception as exc:  # noqa: BLE001
        res["decrypted"] = False
        res["reason"] = type(exc).__name__
        return res
    res["decrypted"] = True
    res["plain_bytes"] = len(plain)
    head = plain[:400].lower()
    res["looks_like_html"] = b"<html" in head or b"<!doctype" in head
    return res


def probe(base: str, key: str, raw_key: bytes, encode_slashes: bool) -> dict:
    safe = "" if encode_slashes else "/"
    url = f"{base}/{urllib.parse.quote(key, safe=safe)}"
    out: dict = {}
    try:
        r = requests.get(url, timeout=(20, 120))
    except Exception as exc:  # noqa: BLE001
        return {"error": str(exc)}
    out["status"] = r.status_code
    if r.status_code != 200:
        return out
    out.update(try_decrypt(raw_key, r.content))
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--output", default="ci-report")
    ap.add_argument("--samples", type=int, default=3)
    args = ap.parse_args()

    row = fetch_key_row()
    raw_key = row.pop("_raw", None)

    samples = pick_samples(args.samples)
    checks = []
    for entry in samples:
        key = entry["key"]
        checks.append(
            {
                "key": key,
                "size": entry.get("size"),
                "parspack": probe(PARSPACK_BASE, key, raw_key, encode_slashes=False),
                "arvan": probe(ARVAN_BASE, key, raw_key, encode_slashes=True),
            }
        )

    out_dir = Path(args.output)
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "html-key-status.json").write_text(
        json.dumps({"row": row, "checks": checks}, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    def yn(v) -> str:
        return "✅" if v is True else ("❌" if v is False else "—")

    md = ["# وضعیت کلید رمز محتوا (`html_media_key`)", ""]
    md += [
        "## ردیف روی Appwrite", "",
        f"- سرور: `{row.get('endpoint')}`",
        f"- پروژه: `{row.get('project')}` · دیتابیس: `{row.get('database')}` · جدول: `{TABLE}` · ردیف: `{ROW_ID}`",
        f"- ردیف پیدا شد: {yn(row.get('found'))}",
        f"- payload معتبر: {yn(row.get('payload_valid_json'))} · شامل کلید `b`: {yn(row.get('payload_has_b'))}",
        f"- طول کلید ۳۲ بایت: {yn(row.get('key_ok'))} (اندازه: {row.get('key_bytes', '—')})",
        f"- اثر انگشت کلید: `{row.get('key_fingerprint', '—')}`",
        f"- دسترسی‌ها: `{row.get('permissions', '—')}`",
        f"- آخرین تغییر: `{row.get('updatedAt', '—')}`",
        "",
    ]
    perms = row.get("permissions") or []
    reads_users = any('read("users")' in str(p) or "read(\"users\")" in str(p) for p in perms)
    md += [f"- کاربر لاگین‌کرده می‌تواند بخواند (`read(\"users\")`): {yn(reads_users)}", ""]
    if row.get("error"):
        md += [f"> ⚠️ {row['error']}", ""]

    md += ["## باز شدن واقعی فایل‌ها با همین کلید", "",
           "| فایل | پارس‌پک | رمز باز شد | آروان | رمز باز شد |", "|---|---|---|---|---|"]
    for c in checks:
        p, a = c["parspack"], c["arvan"]
        md.append(
            f"| `{c['key']}` | {p.get('status', p.get('error', '—'))} / HMK1 {yn(p.get('magic_hmk1'))} "
            f"| {yn(p.get('decrypted'))} "
            f"| {a.get('status', a.get('error', '—'))} / HMK1 {yn(a.get('magic_hmk1'))} "
            f"| {yn(a.get('decrypted'))} |"
        )
    md.append("")

    ok_pars = sum(1 for c in checks if c["parspack"].get("decrypted") is True)
    ok_arvan = sum(1 for c in checks if c["arvan"].get("decrypted") is True)
    md += ["## نتیجه", "",
           f"- پارس‌پک: {ok_pars} از {len(checks)} فایل با کلید سرور باز شد.",
           f"- آروان: {ok_arvan} از {len(checks)} فایل با کلید سرور باز شد.", ""]
    if not row.get("found"):
        md.append("> ❌ **ردیف کلید روی این پروژه نیست.** هر نصب تازه نمی‌تواند محتوا را باز کند.")
    elif not row.get("key_ok"):
        md.append("> ❌ **کلید ۳۲ بایت نیست.** اپ آن را رد می‌کند (`if (raw.size != 32) return null`).")
    elif not reads_users:
        md.append("> ⚠️ **ردیف `read(\"users\")` ندارد؛** کاربر عادی بعد از ورود هم نمی‌تواند بخواندش.")
    elif ok_pars == len(checks) and ok_arvan == len(checks):
        md.append("> ✅ کلید سر جایش است و محتوای هر دو سرور را باز می‌کند.")
    else:
        md.append("> ⚠️ کلید هست ولی بعضی فایل‌ها با آن باز نشدند — جدول بالا را ببینید.")

    (out_dir / "html-key-status.md").write_text("\n".join(md) + "\n", encoding="utf-8")

    print(json.dumps({
        "row_found": row.get("found"),
        "key_ok": row.get("key_ok"),
        "reads_users": reads_users,
        "parspack_decrypted": f"{ok_pars}/{len(checks)}",
        "arvan_decrypted": f"{ok_arvan}/{len(checks)}",
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
