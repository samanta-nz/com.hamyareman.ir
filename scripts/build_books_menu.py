#!/usr/bin/env python3
"""ساخت `books-menu.json` از منوهای تازهٔ باکت.

ورودی: `bucket-sync/Bucket/Pdf-files/G09/<book>/menu.json` (همان چیزی که همین
حالا روی پارس‌پک است، نه نسخهٔ مخزن).

سلسله‌مراتب (`children`) و سربرگ‌ها (`tabs`) عیناً حفظ می‌شوند — هر عمقی که
باشد. تعداد و نام سربرگ‌ها از کتابی به کتاب و حتی از گرهی به گره فرق می‌کند و
هیچ‌جا فهرست ثابتی فرض نمی‌شود.

پرچم `ready` با پرسش مستقیم از باکت تعیین می‌شود (`head_object`)، چون روی این
باکت فهرست‌برداری ممکن نیست.
"""

from __future__ import annotations

import argparse
import collections
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

BASE = "Bucket/Pdf-files/G09"

# گره‌های پیش‌درآمد که نباید وارد منو شوند (دستور کارفرما).
SKIP_PREFIXES = ("سخنی با", "مقدمه", "مقدّمه")

# slug پوشهٔ exam — فقط ریاضی تأیید شده؛ بقیه وقتی آپلود شدند اضافه می‌شوند.
EXAM_SLUGS = {"g9-math": "ryazi"}


def skipped(title: str) -> bool:
    t = (title or "").strip().lstrip("\u200f\ufeff")
    return any(t.startswith(p) for p in SKIP_PREFIXES)


def collect_pdfs(items: list) -> set[str]:
    found: set[str] = set()
    for it in items:
        if it.get("pdf"):
            found.add(it["pdf"])
        for t in it.get("tabs", []):
            if t.get("pdf"):
                found.add(t["pdf"])
        found |= collect_pdfs(it.get("children", []))
    return found


def convert(items: list, folder: str, exists: dict[str, bool], stats: collections.Counter,
            chapter: list | None = None) -> list:
    out = []
    chapter = chapter if chapter is not None else [0]
    slug = EXAM_SLUGS.get(folder)
    lesson_no = 0
    for it in items:
        if skipped(it.get("title", "")):
            stats["skipped"] += 1
            continue
        def ref(pdf):
            if not pdf:
                stats["none"] += 1
                return None, None
            key = f"{BASE}/{folder}/{pdf}"
            ok = exists.get(key, False)
            stats["ready" if ok else "missing"] += 1
            return key, ok

        key, ready = ref(it.get("pdf"))
        is_container = bool(it.get("children"))
        if is_container:
            chapter[0] += 1
        else:
            lesson_no += 1

        # صفحهٔ تدریس و صوت از پوشهٔ exam: <slug>f<فصل>d<درس>.{html,mp3}
        teach_key = audio_key = None
        if slug and not is_container and it.get("tabs") and chapter[0] > 0:
            stem = f"{BASE}/{folder}/exam/{slug}f{chapter[0]:02d}d{lesson_no:02d}"
            if exists.get(stem + ".html"):
                teach_key = stem + ".html"
                stats["teach"] += 1
            if exists.get(stem + ".mp3"):
                audio_key = stem + ".mp3"
                stats["audio"] += 1

        node = {
            "kind": it.get("kind", "plain"),
            "title": it.get("title", ""),
            "key": key,
            "ready": ready,
            "teachKey": teach_key,
            "audioKey": audio_key,
            "tabs": [],
            "children": convert(it.get("children", []), folder, exists, stats, chapter),
        }
        for t in it.get("tabs", []):
            tk, tr = ref(t.get("pdf"))
            node["tabs"].append({"title": t.get("title", ""), "key": tk, "ready": tr})
        out.append(node)
        stats["nodes"] += 1
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", default="bucket-sync")
    ap.add_argument("--out", default="apps/hamyar-app/src/main/assets/content/books-menu.json")
    ap.add_argument("--report", default="docs/books/menu-build-report.md")
    ap.add_argument("--no-probe", action="store_true", help="بدون پرسش از باکت؛ همه ready=false")
    args = ap.parse_args()

    root = Path(args.source) / BASE
    folders = sorted(p.name for p in root.iterdir() if (p / "menu.json").is_file())
    if not folders:
        raise SystemExit(f"❌ هیچ menu.json زیر {root} نیست — اول همگام‌سازی باکت را اجرا کن.")

    raw = {}
    wanted: set[str] = set()
    for folder in folders:
        data = json.loads((root / folder / "menu.json").read_text(encoding="utf-8-sig"))
        raw[folder] = data
        for pdf in collect_pdfs(data.get("items", [])):
            wanted.add(f"{BASE}/{folder}/{pdf}")
        slug = EXAM_SLUGS.get(folder)
        if slug:
            for c in range(1, 13):
                for d in range(1, 10):
                    stem = f"{BASE}/{folder}/exam/{slug}f{c:02d}d{d:02d}"
                    wanted.add(stem + ".html")
                    wanted.add(stem + ".mp3")

    exists: dict[str, bool] = {}
    if args.no_probe:
        exists = {k: False for k in wanted}
    else:
        from s3_targets import target as load_target  # noqa: PLC0415

        tgt = load_target("parspack")
        client = tgt.client()
        for key in sorted(wanted):
            try:
                client.head_object(Bucket=tgt.bucket, Key=key)
                exists[key] = True
            except Exception:  # noqa: BLE001
                exists[key] = False
        print(f"سنجش روی باکت: {sum(exists.values())} از {len(exists)} فایل موجود است.")

    stats = collections.Counter()
    books = []
    for folder in folders:
        d = raw[folder]
        books.append({
            "folder": folder,
            "code": str(d.get("code", "")),
            "subject": d.get("subject", ""),
            "items": convert(d.get("items", []), folder, exists, stats),
        })

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        json.dumps({"source": "ParsPack Bucket/Pdf-files/G09", "books": books},
                   ensure_ascii=False, indent=1),
        encoding="utf-8",
    )

    def depth_counts(nodes, depth=0, acc=None):
        acc = acc if acc is not None else collections.Counter()
        for n in nodes:
            acc[depth] += 1
            depth_counts(n["children"], depth + 1, acc)
        return acc

    rep = Path(args.report)
    rep.parent.mkdir(parents=True, exist_ok=True)
    md = ["# منوی کتاب‌ها — از منوهای زندهٔ باکت", "",
          f"- کتاب: **{len(books)}** · گره: **{stats['nodes']}**",
          f"- ارجاع با فایل موجود: **{stats['ready']}** · اعلام‌شده ولی نبود: **{stats['missing']}**",
          f"- بدون فایل (`pdf: null`) → اسپیس‌هولدر: **{stats['none']}**",
          f"- گرهٔ حذف‌شده (مقدمه / سخنی با…): **{stats['skipped']}**",
          f"- صفحهٔ تدریس از exam: **{stats['teach']}** · صوت: **{stats['audio']}**", "",
          "| کد | موضوع | پوشه | عمق ۰ | عمق ۱ | عمق ۲ | سربرگ |",
          "|---|---|---|---:|---:|---:|---:|"]
    for b in books:
        dc = depth_counts(b["items"])
        tabs = 0

        def count_tabs(nodes):
            nonlocal tabs
            for n in nodes:
                tabs += len(n["tabs"])
                count_tabs(n["children"])
        count_tabs(b["items"])
        md.append(
            f"| {b['code']} | {b['subject']} | `{b['folder']}` | "
            f"{dc.get(0, 0)} | {dc.get(1, 0)} | {dc.get(2, 0)} | {tabs} |"
        )
    rep.write_text("\n".join(md) + "\n", encoding="utf-8")

    print(json.dumps(dict(stats), ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
