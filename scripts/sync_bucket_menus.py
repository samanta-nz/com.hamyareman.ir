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
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target as load_target  # noqa: E402

MENU_NAMES = ("menu.txt", "menu.json", "files.json")
AUDIO_EXT = (".mp3", ".m4a", ".aac", ".ogg", ".opus", ".wav")


BOOKS = [
    "g9-arabic", "g9-art", "g9-defa", "g9-english", "g9-english-workbook",
    "g9-farsi", "g9-hedye", "g9-karfan", "g9-math", "g9-negar",
    "g9-payam", "g9-quran", "g9-sci", "g9-soc",
]
BASE = "Bucket/Pdf-files/G09"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="bucket-sync")
    ap.add_argument("--report", default="ci-report/bucket-inventory")
    args = ap.parse_args()

    tgt = load_target("parspack")
    client = tgt.client()

    # فهرست‌برداری روی این باکت جواب نمی‌دهد (virtual→500، path→404)، پس فقط
    # کلیدهای شناخته‌شده را مستقیم می‌خوانیم.
    out_dir = Path(args.out)
    fetched, missing = [], []
    for book in BOOKS:
        for name in MENU_NAMES:
            key = f"{BASE}/{book}/{name}"
            try:
                body = client.get_object(Bucket=tgt.bucket, Key=key)["Body"].read()
            except Exception as exc:  # noqa: BLE001
                code = getattr(exc, "response", {}).get("Error", {}).get("Code", type(exc).__name__)
                missing.append((key, str(code)))
                continue
            dest = out_dir / key
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(body)
            fetched.append((key, len(body)))

    # هر ارجاع صوتی/exam که داخل منوهای تازه آمده باشد را بیرون می‌کشیم و
    # وجودش را با head تک‌به‌تک می‌سنجیم (چون نمی‌شود پوشه را فهرست کرد).
    refs: set[str] = set()
    for key, _ in fetched:
        text = (out_dir / key).read_bytes().decode("utf-8-sig", "replace")
        for m in re.finditer(r"[\w\-. ]+\.(?:mp3|m4a|aac|ogg|opus|wav)", text, re.IGNORECASE):
            refs.add(m.group(0).strip())
    probes = []
    for book in BOOKS:
        for name in sorted(refs):
            key = f"{BASE}/{book}/exam/{name}"
            try:
                head = client.head_object(Bucket=tgt.bucket, Key=key)
                probes.append((key, int(head["ContentLength"]), "ok"))
            except Exception:  # noqa: BLE001
                pass

    rep = Path(args.report)
    rep.parent.mkdir(parents=True, exist_ok=True)
    inv = {
        "fetched": [{"key": k, "size": s} for k, s in fetched],
        "missing": [{"key": k, "code": c} for k, c in missing],
        "audioRefsInMenus": sorted(refs),
        "examFound": [{"key": k, "size": s} for k, s, _ in probes],
    }
    rep.with_suffix(".json").write_text(json.dumps(inv, ensure_ascii=False, indent=1), encoding="utf-8")

    md = ["# منوهای تازه از باکت پارس", "",
          "> فهرست‌برداری روی این باکت ممکن نیست (virtual→500، path→404)؛",
          "> فقط کلیدهای شناخته‌شده مستقیم خوانده شدند.", "",
          f"- فایل آورده‌شده: **{len(fetched)}** · نبود: **{len(missing)}**",
          f"- ارجاع صوتی داخل منوها: **{len(refs)}** · فایل exam پیداشده: **{len(probes)}**", "",
          "| فایل | حجم |", "|---|---:|"]
    for k, s in fetched:
        md.append(f"| `{k}` | {s:,} |")
    if missing:
        md += ["", "## نبودها", "", "| کلید | کد |", "|---|---|"]
        for k, c in missing:
            md.append(f"| `{k}` | {c} |")
    if refs:
        md += ["", "## ارجاع‌های صوتی داخل منوها", ""] + [f"- `{r}`" for r in sorted(refs)]
    if probes:
        md += ["", "## فایل‌های exam که واقعاً روی باکت‌اند", "", "| کلید | حجم |", "|---|---:|"]
        for k, s, _ in probes:
            md.append(f"| `{k}` | {s:,} |")
    rep.with_suffix(".md").write_text("\n".join(md) + "\n", encoding="utf-8")

    print(json.dumps({"fetched": len(fetched), "missing": len(missing),
                      "audioRefs": len(refs), "examFound": len(probes)}, ensure_ascii=False))
    return 0 if fetched else 1


if __name__ == "__main__":
    raise SystemExit(main())
