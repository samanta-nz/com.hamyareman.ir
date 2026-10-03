#!/usr/bin/env python3
"""تأیید اینکه هر فایل مبدأ روی مقصد هست و ناشناس با همان اندازه خوانده می‌شود.

فقط یک کیلوبایت اول هر فایل خوانده می‌شود (درخواست Range)، پس بررسی کامل
۲۶۰ فایل چند ثانیه طول می‌کشد نه چند دقیقه.
"""

from __future__ import annotations

import argparse
import json
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import requests

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import list_all, target  # noqa: E402

SKIP_PREFIXES = ("staging-v2/", ".staging/", "_preflight/", "apk/_diagnose/")


def check(item: tuple[str, int, str]) -> tuple[str, bool, str]:
    key, size, url = item
    try:
        response = requests.get(
            url, headers={"Range": "bytes=0-1023", "Accept-Encoding": "identity"}, timeout=45
        )
        if response.status_code not in (200, 206):
            return key, False, f"HTTP {response.status_code}"
        content_range = response.headers.get("Content-Range", "")
        total = None
        if "/" in content_range:
            tail = content_range.rsplit("/", 1)[1]
            if tail.isdigit():
                total = int(tail)
        elif response.status_code == 200:
            length = response.headers.get("Content-Length", "")
            if length.isdigit():
                total = int(length)
        if total is not None and total != size:
            return key, False, f"اندازه {total} ≠ {size}"
        return key, True, f"HTTP {response.status_code}"
    except Exception as exc:  # noqa: BLE001
        return key, False, f"{type(exc).__name__}: {str(exc)[:120]}"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default="arvan")
    parser.add_argument("--dest", default="parspack")
    parser.add_argument("--out", type=Path, default=Path("ci-report/parspack-verify.md"))
    parser.add_argument("--workers", type=int, default=12)
    args = parser.parse_args()

    source = target(args.source)
    dest = target(args.dest)

    objects = [
        (o["Key"], int(o["Size"]))
        for o in list_all(source.client(), source.bucket, source)
        if not o["Key"].endswith("/") and not any(o["Key"].startswith(p) for p in SKIP_PREFIXES)
    ]
    work = [(key, size, dest.public_url(key)) for key, size in objects]

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        results = list(pool.map(check, work))

    bad = [(k, d) for k, ok, d in results if not ok]
    total_bytes = sum(size for _, size in objects)

    lines = [
        f"# تأیید آینه — {dest.label}",
        "",
        f"- پایهٔ نشانی: `{dest.public_url('').rstrip('/')}`",
        f"- بررسی‌شده: **{len(results)}** فایل، **{total_bytes / 1048576:.1f} MB**",
        f"- سالم: **{len(results) - len(bad)}** · خراب: **{len(bad)}**",
        "",
    ]
    if bad:
        lines += ["## فایل‌های خراب", ""] + [f"- `{k}` — {d}" for k, d in bad[:80]] + [""]
    else:
        lines += ["✅ هر فایل مبدأ روی مقصد ناشناس و با اندازهٔ درست خوانده می‌شود.", ""]

    # نمونه‌ای از درخت پوشه‌ها برای چشم انسان
    folders: dict[str, int] = {}
    for key, _ in objects:
        top = key.split("/")[0] if "/" in key else "(ریشه)"
        folders[top] = folders.get(top, 0) + 1
    lines += ["## پوشه‌بندی منتقل‌شده", "", "| پوشه | تعداد فایل |", "|---|---:|"]
    lines += [f"| `{name}` | {count} |" for name, count in sorted(folders.items(), key=lambda kv: -kv[1])]
    lines.append("")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    args.out.with_suffix(".json").write_text(
        json.dumps({"checked": len(results), "bad": len(bad), "bytes": total_bytes}, ensure_ascii=False),
        encoding="utf-8",
    )
    print("\n".join(lines))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
