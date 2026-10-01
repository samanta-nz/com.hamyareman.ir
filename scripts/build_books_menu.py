#!/usr/bin/env python3
"""ساخت یک فایل دادهٔ واحد از ۱۴ فایل menu.json روی باکت پارس.

خروجی داده‌ی خالص است و به معماری بارگذاری کاری ندارد؛ زیر هر تصمیمی
(رمزشده یا ساده) یکسان می‌ماند.

برای هر سربرگ/آیتم مشخص می‌کند:
  ready = true   فایل PDF در پوشه موجود است
  ready = false  فایل اعلام شده ولی هنوز آپلود نشده  (missing)
  ready = null   اصلاً فایلی اعلام نشده (pdf: null) → «به‌زودی»
"""
from __future__ import annotations
import argparse, json, subprocess, collections, os
from pathlib import Path

PREFIX = "Bucket/Pdf-files/G09"


def git_show(ref: str, path: str) -> bytes:
    return subprocess.run(["git", "show", f"{ref}:{path}"], capture_output=True).stdout


def git_ls(ref: str, path: str) -> list[str]:
    out = subprocess.run(["git", "-c", "core.quotepath=false", "ls-tree", "-r", "--name-only", ref, "--", path],
                         capture_output=True, text=True).stdout
    return out.splitlines()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--ref", default="origin/main")
    ap.add_argument("--out", default="apps/hamyar-app/src/main/assets/content/books-menu.json")
    ap.add_argument("--report", default="docs/books/menu-build-report.md")
    args = ap.parse_args()

    all_paths = git_ls(args.ref, PREFIX)
    folders = sorted({p.split("/")[3] for p in all_paths if len(p.split("/")) > 4})
    present = collections.defaultdict(set)
    for p in all_paths:
        parts = p.split("/")
        if len(parts) == 5 and parts[4].lower().endswith(".pdf"):
            present[parts[3]].add(parts[4])

    books, stats = [], collections.Counter()

    def conv(items: list, folder: str, have: set) -> list:
        out = []
        for it in items:
            def ref(pdf):
                if not pdf:
                    stats["tab_none"] += 1
                    return None, None
                stats["tab_pdf"] += 1
                if pdf not in have:
                    stats["tab_missing"] += 1
                    return f"{PREFIX}/{folder}/{pdf}", False
                return f"{PREFIX}/{folder}/{pdf}", True

            key, ready = ref(it.get("pdf"))
            node = {
                "kind": it.get("kind"),
                "title": it.get("title"),
                "key": key,
                "ready": ready,
                "tabs": [],
                "children": conv(it.get("children", []), folder, have),
            }
            for t in it.get("tabs", []):
                tk, tr = ref(t.get("pdf"))
                node["tabs"].append({"title": t.get("title"), "key": tk, "ready": tr})
            out.append(node)
            stats["items"] += 1
        return out

    for folder in folders:
        raw = git_show(args.ref, f"{PREFIX}/{folder}/menu.json")
        if not raw:
            continue
        d = json.loads(raw.decode("utf-8-sig"))
        have = present[folder]
        books.append({
            "folder": folder,
            "code": str(d.get("code", "")),
            "subject": d.get("subject", ""),
            "items": conv(d.get("items", []), folder, have),
        })
        stats["books"] += 1

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"source": "ParsPack Bucket/Pdf-files/G09", "books": books},
                              ensure_ascii=False, indent=1), encoding="utf-8")

    rep = Path(args.report)
    rep.parent.mkdir(parents=True, exist_ok=True)
    md = ["# منوی کتاب‌ها — ساخته‌شده از menu.json روی باکت پارس", "",
          f"- کتاب: **{stats['books']}** · آیتم منو: **{stats['items']}**",
          f"- سربرگ/آیتم با PDF اعلام‌شده: **{stats['tab_pdf']}** (از این‌ها **{stats['tab_missing']}** هنوز آپلود نشده)",
          f"- بدون فایل (`pdf: null`) → «به‌زودی»: **{stats['tab_none']}**", "",
          "| کد | موضوع | پوشه | آیتم | آمادهٔ باز شدن | هنوز آپلود نشده | به‌زودی |",
          "|---|---|---|---:|---:|---:|---:|"]
    for b in books:
        r = g = n = 0
        def count(nodes):
            nonlocal r, g, n
            for x in nodes:
                for e in [x] + x["tabs"]:
                    if e.get("ready") is True: r += 1
                    elif e.get("ready") is False: g += 1
                    else: n += 1
                count(x["children"])
        count(b["items"])
        md.append(f"| {b['code']} | {b['subject']} | `{b['folder']}` | {len(b['items'])} | {r} | {g} | {n} |")
    rep.write_text("\n".join(md) + "\n", encoding="utf-8")
    print(json.dumps(dict(stats), ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
