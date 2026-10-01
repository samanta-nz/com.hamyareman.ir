#!/usr/bin/env python3
"""کشف فایل‌های پوشهٔ exam با head مستقیم (چون فهرست‌برداری روی باکت کار نمی‌کند).

الگوی تأییدشده:  Bucket/Pdf-files/G09/<book>/exam/<slug>f<NN>d<NN>.<ext>
"""
from __future__ import annotations
import json, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from s3_targets import target as load_target  # noqa: E402

BASE = "Bucket/Pdf-files/G09"
# تنها slug تأییدشده؛ بقیه حدس است و اگر نبود، بی‌سروصدا رد می‌شود.
SLUGS = {
    "g9-math": ["ryazi"],
    "g9-arabic": ["arabi"], "g9-farsi": ["farsi"], "g9-sci": ["olum"],
    "g9-soc": ["ejtemai"], "g9-english": ["english", "zaban"],
    "g9-art": ["honar"], "g9-defa": ["defaei", "defa"],
    "g9-payam": ["payam"], "g9-hedye": ["hedye"], "g9-karfan": ["karfan", "kar"],
    "g9-negar": ["negaresh", "negar"], "g9-quran": ["quran"],
    "g9-english-workbook": ["workbook", "karenglish"],
}
EXTS = ("html", "mp3")


def main() -> int:
    tgt = load_target("parspack")
    client = tgt.client()
    found, tried = [], 0
    for book, slugs in SLUGS.items():
        for slug in slugs:
            hit_for_slug = 0
            for f in range(1, 13):
                for d in range(1, 10):
                    stem = f"{slug}f{f:02d}d{d:02d}"
                    for ext in EXTS:
                        key = f"{BASE}/{book}/exam/{stem}.{ext}"
                        tried += 1
                        try:
                            h = client.head_object(Bucket=tgt.bucket, Key=key)
                            found.append({"book": book, "slug": slug, "f": f, "d": d,
                                          "ext": ext, "key": key,
                                          "size": int(h["ContentLength"])})
                            hit_for_slug += 1
                        except Exception:  # noqa: BLE001
                            pass
                # اگر برای این فصل هیچ چیز نبود و قبلاً هم چیزی نیافته‌ایم، slug غلط است
                if f >= 2 and hit_for_slug == 0:
                    break
    Path("ci-report").mkdir(exist_ok=True)
    Path("ci-report/exam-assets.json").write_text(
        json.dumps({"tried": tried, "found": found}, ensure_ascii=False, indent=1), encoding="utf-8")
    md = ["# فایل‌های پوشهٔ exam روی باکت", "",
          f"- کلید آزموده: **{tried}** · پیداشده: **{len(found)}**", "",
          "| کتاب | فصل | درس | نوع | حجم | کلید |", "|---|---:|---:|---|---:|---|"]
    for x in found:
        md.append(f"| {x['book']} | {x['f']} | {x['d']} | {x['ext']} | {x['size']:,} | `{x['key']}` |")
    Path("ci-report/exam-assets.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print(json.dumps({"tried": tried, "found": len(found)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
