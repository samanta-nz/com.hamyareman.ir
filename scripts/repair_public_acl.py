#!/usr/bin/env python3
"""تشخیص و ترمیم فایل‌هایی که روی سرور داخلی هستند ولی عمومی خوانده نمی‌شوند.

پس‌زمینه: در پارس‌پک (Ceph RGW) وقتی کاربر ناشناس مجوز `ListBucket` ندارد،
برای شیء **موجودِ خصوصی** به‌جای `403 AccessDenied` جواب `404 NoSuchKey`
می‌دهد. پس ۴۰۴ لزوماً یعنی «فایل نیست» نیست؛ می‌تواند یعنی «فایل هست ولی
عمومی نیست». این اسکریپت این دو حالت را از هم جدا می‌کند:

    head_object با کلید  →  فایل واقعاً هست؟
    GET ناشناس           →  عمومی خوانده می‌شود؟

  هست + خوانده می‌شود  → سالم
  هست + ۴۰۴/۴۰۳        → فقط ACL خراب است  → با --fix درست می‌شود
  نیست                  → باید دوباره از مبدأ کپی شود (کار mirror_storage)

فهرست کلیدها از `assets/content/server-map.json` می‌آید، یعنی دقیقاً همان
نشانی‌هایی که اپ واقعاً صدا می‌زند — نه هرچه اتفاقی در باکت افتاده.
"""

from __future__ import annotations

import argparse
import json
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import requests

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
SERVER_MAP = ROOT / "apps/hamyar-app/src/main/assets/content/server-map.json"


def load_keys(extra: list[str]) -> list[tuple[str, int]]:
    data = json.loads(SERVER_MAP.read_text(encoding="utf-8"))
    out = [(e["key"], int(e.get("size") or 0)) for e in data["entries"]]
    out += [(k, 0) for k in extra]
    seen: set[str] = set()
    uniq = []
    for key, size in out:
        if key not in seen:
            seen.add(key)
            uniq.append((key, size))
    return uniq


def anon(url: str) -> tuple[int, int]:
    """(کد وضعیت، اندازهٔ واقعی) با یک Range کوچک."""
    try:
        r = requests.get(url, headers={"Range": "bytes=0-1", "Accept-Encoding": "identity"}, timeout=30)
        cr = r.headers.get("Content-Range", "")
        total = int(cr.rsplit("/", 1)[1]) if "/" in cr and cr.rsplit("/", 1)[1].isdigit() else -1
        return r.status_code, total
    except Exception:  # noqa: BLE001
        return 0, -1


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--provider", default="parspack")
    ap.add_argument("--fix", action="store_true", help="روی موارد خصوصی ACL عمومی بگذار")
    ap.add_argument("--extra-key", action="append", default=[])
    ap.add_argument("--workers", type=int, default=10)
    ap.add_argument("--out", type=Path, default=Path("ci-report/parspack-acl.md"))
    args = ap.parse_args()

    dest = target(args.provider)
    client = dest.client()
    keys = load_keys(args.extra_key)

    def probe(item: tuple[str, int]) -> dict:
        key, want = item
        row: dict = {"key": key, "want": want, "exists": False, "real": -1, "status": 0, "fixed": ""}
        try:
            head = client.head_object(Bucket=dest.bucket, Key=key)
            row["exists"] = True
            row["real"] = int(head.get("ContentLength") or 0)
        except Exception as exc:  # noqa: BLE001
            row["headError"] = type(exc).__name__
        row["status"], _ = anon(dest.public_url(key))
        if args.fix and row["exists"] and row["status"] not in (200, 206):
            try:
                client.put_object_acl(Bucket=dest.bucket, Key=key, ACL="public-read")
                row["fixed"] = "acl"
            except Exception as exc:  # noqa: BLE001
                row["fixed"] = f"خطا: {type(exc).__name__}"
            row["status"], _ = anon(dest.public_url(key))
        return row

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        rows = list(pool.map(probe, keys))

    healthy = [r for r in rows if r["status"] in (200, 206)]
    private = [r for r in rows if r["exists"] and r["status"] not in (200, 206)]
    absent = [r for r in rows if not r["exists"]]
    wrong_size = [r for r in rows if r["exists"] and r["want"] and r["real"] != r["want"]]

    def folder(r: dict) -> str:
        return r["key"].split("/")[0]

    tally: dict[str, list[int]] = {}
    for r in rows:
        t = tally.setdefault(folder(r), [0, 0, 0, 0])
        t[0] += 1
        if r["status"] in (200, 206):
            t[1] += 1
        elif r["exists"]:
            t[2] += 1
        else:
            t[3] += 1

    lines = [
        f"# سلامت دسترسی عمومی — {dest.label}",
        "",
        f"- کلیدهای بررسی‌شده (از server-map اپ): **{len(rows)}**",
        f"- سالم و عمومی: **{len(healthy)}**",
        f"- روی سرور هست ولی عمومی نیست: **{len(private)}**",
        f"- اصلاً روی سرور نیست: **{len(absent)}**",
        f"- اندازهٔ ناهماهنگ: **{len(wrong_size)}**",
        f"- حالت ترمیم: **{'روشن' if args.fix else 'خاموش (فقط تشخیص)'}**",
        "",
        "| پوشه | کل | عمومی | خصوصی | غایب |",
        "|---|---:|---:|---:|---:|",
    ]
    for name, t in sorted(tally.items(), key=lambda kv: -kv[1][0]):
        lines.append(f"| `{name}` | {t[0]} | {t[1]} | {t[2]} | {t[3]} |")
    lines.append("")

    if args.fix:
        ok = [r for r in rows if r["fixed"] == "acl" and r["status"] in (200, 206)]
        failed = [r for r in rows if r["fixed"] and r["status"] not in (200, 206)]
        lines += [
            "## نتیجهٔ ترمیم",
            "",
            f"- ACL عمومی ست شد و بعدش خوانده شد: **{len(ok)}**",
            f"- ترمیم نشد: **{len(failed)}**",
            "",
        ]
        for r in failed[:40]:
            lines.append(f"- `{r['key']}` — {r['fixed']} — وضعیت {r['status']}")
        lines.append("")

    if absent:
        lines += ["## غایب روی سرور (نیاز به کپی دوباره)", ""]
        lines += [f"- `{r['key']}` — {r.get('headError', '')}" for r in absent[:60]]
        lines.append("")
    if wrong_size:
        lines += ["## اندازهٔ ناهماهنگ", ""]
        lines += [f"- `{r['key']}` — روی سرور {r['real']} ≠ انتظار {r['want']}" for r in wrong_size[:40]]
        lines.append("")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    args.out.with_suffix(".json").write_text(
        json.dumps(
            {
                "checked": len(rows),
                "healthy": len(healthy),
                "private": len(private),
                "absent": len(absent),
                "wrongSize": len(wrong_size),
                "fix": bool(args.fix),
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    print("\n".join(lines))
    return 1 if (private or absent) else 0


if __name__ == "__main__":
    sys.exit(main())
