#!/usr/bin/env python3
"""کدام شکل آدرس پارس‌پک واقعاً فایل‌های Bucket/ را می‌دهد؟ (فقط خواندن)"""
from __future__ import annotations
import json, sys, urllib.parse
from pathlib import Path
import requests

SAMPLES = [
    "Bucket/Pdf-files/G09/g9-arabic/g9-arabic-index.pdf",
    "Bucket/Pdf-files/G09/g9-math/menu.json",
    "Bucket/Html-files/آموزشگاه/26-fake-news.html",
    "Bucket/Html-files/01 - حالت کودک.html",
    "Bucket/Html-files/background-music.html",
]
FORMS = {
    "A: host + key":            "https://c539776.parspack.net/{k}",
    "B: host + bucket + key":   "https://c539776.parspack.net/c539776/{k}",
    "C: endpoint + bucket+key": "https://parspack.net/c539776/{k}",
    "D: s3 host":               "https://s3.parspack.com/c539776/{k}",
}

def probe(url: str) -> dict:
    try:
        r = requests.get(url, headers={"Range": "bytes=0-63"}, timeout=(15, 45))
    except Exception as exc:  # noqa: BLE001
        return {"error": type(exc).__name__}
    out = {"status": r.status_code, "len": r.headers.get("Content-Length"),
           "type": r.headers.get("Content-Type")}
    if r.status_code in (200, 206):
        out["head"] = r.content[:8].hex()
        out["hmk1"] = r.content[:4] == b"HMK1"
    return out

rows = []
for key in SAMPLES:
    for label, tpl in FORMS.items():
        enc = urllib.parse.quote(key, safe="/")
        rows.append({"key": key, "form": label, "url": tpl.format(k=enc), **probe(tpl.format(k=enc))})

Path("ci-report").mkdir(exist_ok=True)
Path("ci-report/parspack-path-probe.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
md = ["# کدام شکل آدرس پارس‌پک کار می‌کند؟", "", "| فایل | شکل | کد | نوع | HMK1 |", "|---|---|---|---|---|"]
for r in rows:
    md.append(f"| `{r['key'][:44]}` | {r['form']} | {r.get('status', r.get('error','—'))} "
              f"| {r.get('type','—')} | {'✅' if r.get('hmk1') else ('❌' if 'hmk1' in r else '—')} |")
ok = [r for r in rows if r.get("status") in (200, 206)]
md += ["", f"**شکل‌های موفق:** {sorted({r['form'] for r in ok}) or 'هیچ‌کدام'}",
       f"**رمزشده (HMK1):** {sum(1 for r in ok if r.get('hmk1'))} از {len(ok)}"]
Path("ci-report/parspack-path-probe.md").write_text("\n".join(md) + "\n", encoding="utf-8")
print(json.dumps({"ok_forms": sorted({r['form'] for r in ok}),
                  "encrypted": sum(1 for r in ok if r.get('hmk1')), "tried": len(rows)}, ensure_ascii=False))
