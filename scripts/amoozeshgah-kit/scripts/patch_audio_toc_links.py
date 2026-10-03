#!/usr/bin/env python3
"""
Patch an HTML table of contents so each matching link seeks a native Hamyar audio player.

Mapping JSON:
[
  {"text": "مقدمه", "ms": 0},
  {"text": "فصل اول", "ms": 185000}
]

The generated HTML uses data-audio-ms and calls window.HamyarAudio.seek(ms).
The Android WebView bridge exposes that function in audio-book mode.
"""
from __future__ import annotations

import argparse
import json
import re
from html import escape
from pathlib import Path


BRIDGE = r"""
<script>
(function () {
  function norm(s) {
    return (s || "").replace(/[\u200c\u200d\u200e\u200f]/g, "")
      .replace(/[يى]/g, "ی").replace(/ك/g, "ک")
      .replace(/[\u064B-\u065F\u0670]/g, "")
      .replace(/\s+/g, " ").trim();
  }
  document.addEventListener("click", function (ev) {
    var a = ev.target && ev.target.closest ? ev.target.closest("a[data-audio-ms]") : null;
    if (!a) return;
    var ms = Number(a.getAttribute("data-audio-ms"));
    if (!isFinite(ms) || ms < 0) return;
    if (window.HamyarAudio && window.HamyarAudio.seek) {
      ev.preventDefault();
      window.HamyarAudio.seek(Math.round(ms));
    }
  }, true);
})();
</script>
"""


def normalize(text: str) -> str:
    return re.sub(r"\s+", " ", text.replace("\u200c", "").replace("\u200d", "").replace("\u200e", "").replace("\u200f", "")
                  .replace("ي", "ی").replace("ى", "ی").replace("ك", "ک")).strip()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--html", required=True, type=Path)
    ap.add_argument("--map", required=True, type=Path, dest="mapping")
    ap.add_argument("--out", type=Path)
    args = ap.parse_args()

    mappings = json.loads(args.mapping.read_text(encoding="utf-8"))
    by_text = {}
    for item in mappings:
        text = normalize(str(item.get("text", "")))
        ms = int(item.get("ms", 0))
        if text and ms >= 0:
            by_text[text] = ms

    html = args.html.read_text(encoding="utf-8")
    changed = 0

    def patch_anchor(match: re.Match[str]) -> str:
        nonlocal changed
        whole, attrs, inner = match.group(0), match.group("attrs"), match.group("inner")
        key = normalize(re.sub(r"<[^>]+>", " ", inner))
        if key not in by_text:
            return whole
        ms = by_text[key]
        if re.search(r"\bdata-audio-ms\s*=", attrs, flags=re.I):
            new_attrs = re.sub(r'\bdata-audio-ms\s*=\s*["\'][^"\']*["\']',
                               f'data-audio-ms="{ms}"', attrs, flags=re.I)
        else:
            new_attrs = attrs + f' data-audio-ms="{ms}"'
        changed += 1
        return f"<a{new_attrs}>{inner}</a>"

    html = re.sub(r"<a(?P<attrs>[^>]*)>(?P<inner>.*?)</a>", patch_anchor, html, flags=re.I | re.S)

    if "window.HamyarAudio" not in html:
        html = html.replace("</body>", BRIDGE + "\n</body>") if re.search(r"</body>", html, re.I) else html + BRIDGE

    out = args.out or args.html
    out.write_text(html, encoding="utf-8")
    print(json.dumps({"output": str(out), "patched_links": changed, "mappings": len(by_text)}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
