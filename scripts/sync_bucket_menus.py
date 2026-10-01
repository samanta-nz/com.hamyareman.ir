#!/usr/bin/env python3
"""خواندن فهرست کامل باکت پارس و آوردن فایل‌های منو + شناسایی پوشه‌های exam.

چرا لازم است: نسخهٔ `menu.txt` داخل مخزن قدیمی است. مرجع، همان چیزی است که
همین حالا روی باکت نشسته. این اسکریپت فقط می‌خواند و چیزی را عوض نمی‌کند.

خروجی:
  bucket-sync/<key>            هر menu.txt و menu.json عیناً
  ci-report/bucket-inventory.json / .md   فهرست و آمار، شامل فایل‌های exam
"""

from __future__ import annotations

import argparse
import collections
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target as load_target  # noqa: E402

MENU_NAMES = ("menu.txt", "menu.json", "files.json")
AUDIO_EXT = (".mp3", ".m4a", ".aac", ".ogg", ".opus", ".wav")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--prefix", default="Bucket/")
    ap.add_argument("--out", default="bucket-sync")
    ap.add_argument("--report", default="ci-report/bucket-inventory")
    args = ap.parse_args()

    tgt = load_target("parspack")

    # این نصب Ceph سر فهرست‌برداری حساس است؛ ترکیب‌ها را امتحان می‌کنیم و
    # اولین چیزی که واقعاً جواب بدهد برنده است. خطاها چاپ می‌شوند، بلعیده نه.
    objects = []
    client = None
    attempts = []
    for addressing in ("virtual", "path"):
        c = tgt.client(addressing=addressing)
        for version in (1, 2):
            try:
                if version == 2:
                    found = []
                    for page in c.get_paginator("list_objects_v2").paginate(
                        Bucket=tgt.bucket, Prefix=args.prefix
                    ):
                        found.extend(page.get("Contents") or [])
                else:
                    found = []
                    marker = ""
                    while True:
                        kw = {"Bucket": tgt.bucket, "Prefix": args.prefix}
                        if marker:
                            kw["Marker"] = marker
                        resp = c.list_objects(**kw)
                        batch = resp.get("Contents") or []
                        found.extend(batch)
                        if not resp.get("IsTruncated"):
                            break
                        marker = batch[-1]["Key"]
                attempts.append(f"{addressing}/v{version}: {len(found)} شیء")
                if found:
                    objects, client = found, c
                    break
            except Exception as exc:  # noqa: BLE001
                detail = getattr(exc, "response", {}).get("Error", {}) if hasattr(exc, "response") else {}
                attempts.append(
                    f"{addressing}/v{version}: {detail.get('Code') or type(exc).__name__}"
                    f" — {(detail.get('Message') or str(exc))[:90]}"
                )
        if objects:
            break

    print("تلاش‌های فهرست‌برداری:")
    for a in attempts:
        print("   ", a)
    if not objects:
        raise SystemExit("❌ هیچ ترکیبی از فهرست‌برداری جواب نداد.")

    keys = sorted((o["Key"], int(o.get("Size") or 0)) for o in objects)
    print(f"اشیاء زیر «{args.prefix}»: {len(keys)}")

    out_dir = Path(args.out)
    fetched = []
    for key, size in keys:
        if os.path.basename(key) in MENU_NAMES:
            dest = out_dir / key
            dest.parent.mkdir(parents=True, exist_ok=True)
            body = client.get_object(Bucket=tgt.bucket, Key=key)["Body"].read()
            dest.write_bytes(body)
            fetched.append((key, len(body)))

    # ---- آمار ----
    by_ext = collections.Counter(os.path.splitext(k)[1].lower() or "(بدون پسوند)" for k, _ in keys)
    by_top = collections.Counter(k.split("/")[1] if k.count("/") > 1 else "(ریشه)" for k, _ in keys)

    # پوشه‌های exam و فایل‌های صوتی داخلشان
    exam = collections.defaultdict(list)
    for key, size in keys:
        parts = key.split("/")
        if any(p.lower() == "exam" for p in parts):
            book = parts[3] if len(parts) > 3 else "(?)"
            exam[book].append((key, size))
    audio = [(k, s) for k, s in keys if k.lower().endswith(AUDIO_EXT)]

    inv = {
        "prefix": args.prefix,
        "total": len(keys),
        "menus": [k for k, _ in fetched],
        "exam": {b: [{"key": k, "size": s} for k, s in v] for b, v in sorted(exam.items())},
        "audio": [{"key": k, "size": s} for k, s in audio],
        "keys": [{"key": k, "size": s} for k, s in keys],
    }
    rep = Path(args.report)
    rep.parent.mkdir(parents=True, exist_ok=True)
    rep.with_suffix(".json").write_text(json.dumps(inv, ensure_ascii=False, indent=1), encoding="utf-8")

    md = ["# فهرست باکت پارس", "",
          f"- پیشوند: `{args.prefix}` · شیء: **{len(keys)}**",
          f"- فایل منوی آورده‌شده: **{len(fetched)}**",
          f"- فایل صوتی در کل باکت: **{len(audio)}**",
          f"- پوشهٔ exam: **{len(exam)}** کتاب", "",
          "## پسوندها", "", "| پسوند | تعداد |", "|---|---:|"]
    for e, n in by_ext.most_common():
        md.append(f"| `{e}` | {n} |")
    md += ["", "## پوشه‌های سطح دو", "", "| پوشه | تعداد |", "|---|---:|"]
    for t, n in by_top.most_common():
        md.append(f"| `{t}` | {n} |")
    if exam:
        md += ["", "## محتوای پوشه‌های exam", "", "| کتاب | فایل | حجم |", "|---|---|---:|"]
        for book, items in sorted(exam.items()):
            for k, s in items:
                md.append(f"| `{book}` | `{os.path.basename(k)}` | {s:,} |")
    else:
        md += ["", "> هیچ پوشهٔ `exam` روی باکت پیدا نشد."]
    if audio:
        md += ["", "## همهٔ فایل‌های صوتی", "", "| کلید | حجم |", "|---|---:|"]
        for k, s in audio:
            md.append(f"| `{k}` | {s:,} |")
    rep.with_suffix(".md").write_text("\n".join(md) + "\n", encoding="utf-8")

    print(json.dumps({"objects": len(keys), "menus": len(fetched),
                      "exam_books": len(exam), "audio": len(audio)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
