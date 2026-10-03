#!/usr/bin/env python3
"""Aggressively but conservatively optimize embedded JPEG assets.

Rules:
- practice-covers: 320x320 for square covers; 320x480 for 2:3 yoga/sport covers.
- diary: max 448x672, preserving the original aspect ratio.
- book-covers: max dimension 448, preserving the original aspect ratio.
- JPEG quality 84, optimized/progressive, never upscale.
- A replacement is kept only when it is smaller than the original.
"""
from __future__ import annotations
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "apps/hamyar-app/src/main/assets"
REPORT = ROOT / "ci-report/image-optimization.json"

def target(path: Path, im: Image.Image) -> tuple[int, int]:
    w, h = im.size
    rel = path.relative_to(ASSETS).as_posix()
    if rel.startswith("practice-covers/"):
        if abs((w / h) - 2 / 3) < 0.03 and h > w:
            return (320, 480)
        return (320, 320)
    if rel.startswith("diary/"):
        scale = min(1.0, 448 / max(w, h))
    elif rel.startswith("book-covers/"):
        scale = min(1.0, 448 / max(w, h))
    else:
        return (w, h)
    return (max(1, round(w * scale)), max(1, round(h * scale)))

def optimize(path: Path) -> dict:
    before = path.stat().st_size
    with Image.open(path) as src:
        src.load()
        old_size = src.size
        new_size = target(path, src)
        if new_size == old_size:
            return {"path": str(path.relative_to(ROOT)), "before": before, "after": before, "width": old_size[0], "height": old_size[1], "changed": False}
        rgb = src.convert("RGB")
        resized = rgb.resize(new_size, Image.Resampling.LANCZOS)
        tmp = path.with_suffix(".optimized.jpg")
        resized.save(tmp, "JPEG", quality=84, optimize=True, progressive=True, subsampling="4:2:0")
        after = tmp.stat().st_size
    if after < before:
        tmp.replace(path)
        return {"path": str(path.relative_to(ROOT)), "before": before, "after": after, "width": new_size[0], "height": new_size[1], "changed": True}
    tmp.unlink(missing_ok=True)
    return {"path": str(path.relative_to(ROOT)), "before": before, "after": before, "width": old_size[0], "height": old_size[1], "changed": False}

def main():
    paths = sorted(p for p in ASSETS.rglob("*.jpg") if p.is_file() and (
        "practice-covers" in p.parts or "diary" in p.parts or "book-covers" in p.parts
    ))
    rows = [optimize(p) for p in paths]
    before = sum(r["before"] for r in rows)
    after = sum(r["after"] for r in rows)
    report = {
        "files": len(rows),
        "changed": sum(r["changed"] for r in rows),
        "before_bytes": before,
        "after_bytes": after,
        "saved_bytes": before - after,
        "saved_percent": round((before - after) * 100 / before, 2) if before else 0,
        "rules": {"practice_square": "320x320", "practice_portrait": "320x480", "other_max_dimension": 448, "jpeg_quality": 84},
        "items": rows,
    }
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps({k: report[k] for k in ("files","changed","before_bytes","after_bytes","saved_bytes","saved_percent")}, ensure_ascii=False))

if __name__ == "__main__":
    main()
