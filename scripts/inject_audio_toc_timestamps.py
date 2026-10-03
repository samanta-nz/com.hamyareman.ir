#!/usr/bin/env python3
"""Attach audio seek timestamps to HTML table-of-contents links."""
from __future__ import annotations
import argparse
import html as html_lib
import json
import re
from difflib import SequenceMatcher
from pathlib import Path

ANCHOR_RE = re.compile(r"<a(?P<attrs>[^>]*)>(?P<body>.*?)</a\\s*>", re.I | re.S)
TAG_RE = re.compile(r"<[^>]+>")

def normalize(text: str) -> str:
    text = html_lib.unescape(text or "").lower()
    text = text.replace("ي", "ی").replace("ك", "ک").replace("ة", "ه").replace("ۀ", "ه").replace("‌", "")
    return re.sub(r"[^0-9a-z\\u0600-\\u06FF]+", "", text)

def parse_ms(raw: str) -> int:
    parts = raw.strip().strip("[]()").split(":")
    if len(parts) == 2:
        return int(round((int(parts[0]) * 60 + float(parts[1])) * 1000))
    if len(parts) == 3:
        return int(round((int(parts[0]) * 3600 + int(parts[1]) * 60 + float(parts[2])) * 1000))
    raise ValueError(f"invalid timestamp: {raw}")

def parse_timings(path: Path) -> list[tuple[str, int]]:
    raw = path.read_text(encoding="utf-8-sig")
    try:
        obj = json.loads(raw)
    except json.JSONDecodeError:
        obj = None
    if obj is not None:
        items = obj.get("items", obj) if isinstance(obj, dict) else obj
        return [
            (str(item.get("title") or item.get("text")).strip(),
             parse_ms(str(item.get("time") or item.get("timestamp")).strip()))
            for item in items
            if isinstance(item, dict)
            and str(item.get("title") or item.get("text")).strip()
            and str(item.get("time") or item.get("timestamp")).strip()
        ]

    result: list[tuple[str, int]] = []
    line_re = re.compile(
        r"^\\s*\\[?(\\d+(?::\\d{1,2}(?:\\.\\d+)?)?(?::\\d{1,2}(?:\\.\\d+)?)?)\\]?\\s+(.+?)\\s*$"
    )
    for line in raw.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        m = line_re.match(line)
        if m:
            result.append((m.group(2).strip(), parse_ms(m.group(1))))
    return result

def similarity(a: str, b: str) -> float:
    if not a or not b:
        return 0.0
    if a == b:
        return 1.0
    if a in b or b in a:
        return 0.9
    return SequenceMatcher(None, a, b).ratio()

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--html", type=Path, required=True)
    ap.add_argument("--timings", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--min-score", type=float, default=0.62)
    args = ap.parse_args()

    document = args.html.read_text(encoding="utf-8")
    timings = parse_timings(args.timings)
    if not timings:
        raise SystemExit("no valid audio timings found")

    used: set[int] = set()
    matched: list[dict] = []
    unmatched: list[str] = []

    def replace_anchor(match: re.Match[str]) -> str:
        attrs = match.group("attrs")
        body = match.group("body")
        toc_text = normalize(TAG_RE.sub(" ", body))
        ranked = sorted(
            (
                (similarity(toc_text, normalize(title)), index, title, ms)
                for index, (title, ms) in enumerate(timings)
                if index not in used
            ),
            reverse=True,
        )
        if not ranked or ranked[0][0] < args.min_score:
            unmatched.append(toc_text)
            return match.group(0)
        score, index, timing_title, ms = ranked[0]
        used.add(index)
        attrs = re.sub(r"\\sdata-seek-ms\\s*=\\s*(['"]).*?\\1", "", attrs, flags=re.I)
        attrs = f'{attrs} data-seek-ms="{ms}"'
        matched.append({"toc": toc_text, "timing": timing_title, "ms": ms, "score": round(score, 3)})
        return f"<a{attrs}>{body}</a>"

    output = ANCHOR_RE.sub(replace_anchor, document)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(output, encoding="utf-8")
    print(json.dumps({"timings": len(timings), "matched": matched, "unmatched": unmatched}, ensure_ascii=False, indent=2))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
