#!/usr/bin/env python3
"""Generate the seven missing wellness menu covers, upload them, and verify public bytes."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

import requests
from PIL import Image, ImageDraw, ImageFilter
from botocore.exceptions import ClientError

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from s3_targets import target  # noqa: E402

SIZE = 512
NAMES = (
    "hl-sleep-prep.jpg",
    "sd-study.jpg",
    "sd-stress.jpg",
    "sd-nature.jpg",
    "sd-noise.jpg",
    "sd-freq.jpg",
    "sd-sleep.jpg",
)
PREFIX = "assets/practice-covers/"
CACHE_CONTROL = "public, max-age=3600, must-revalidate"


def rgb(hex_color: str) -> tuple[int, int, int]:
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def gradient(top: str, bottom: str) -> Image.Image:
    a, b = rgb(top), rgb(bottom)
    im = Image.new("RGB", (SIZE, SIZE))
    pixels = im.load()
    for y in range(SIZE):
        t = y / (SIZE - 1)
        color = tuple(round(a[c] * (1 - t) + b[c] * t) for c in range(3))
        for x in range(SIZE):
            pixels[x, y] = color
    return im


def soft_glow(im: Image.Image, xy: tuple[int, int, int, int], color: str, blur: int = 26) -> None:
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    c = rgb(color)
    d.ellipse(xy, fill=(*c, 100))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    im.paste(Image.alpha_composite(im.convert("RGBA"), layer).convert("RGB"))


def draw_star(d: ImageDraw.ImageDraw, x: int, y: int, r: int, color: str) -> None:
    c = rgb(color)
    d.polygon([(x, y-r), (x+r//4, y-r//4), (x+r, y), (x+r//4, y+r//4),
               (x, y+r), (x-r//4, y+r//4), (x-r, y), (x-r//4, y-r//4)], fill=c)


def wave_points(y0: int, amp: int, cycles: float, phase: float = 0, x0: int = 34, x1: int = 478) -> list[tuple[int, int]]:
    return [(x, round(y0 + math.sin((x / SIZE) * cycles * 2 * math.pi + phase) * amp))
            for x in range(x0, x1 + 1, 3)]


def cover_sleep_prep() -> Image.Image:
    im = gradient("#D6E9E8", "#B7C7DD")
    soft_glow(im, (60, 25, 390, 355), "#F5E9CC", 30)
    d = ImageDraw.Draw(im)
    # Soft moon and stars.
    d.ellipse((332, 60, 420, 148), fill=rgb("#F5D88B"))
    d.ellipse((358, 44, 438, 126), fill=rgb("#DCE6E7"))
    for x, y, r in [(116, 78, 12), (220, 132, 8), (430, 187, 8), (286, 72, 7)]:
        draw_star(d, x, y, r, "#FFF5D7")
    # A peaceful bed/pillow, with a folded blanket.
    d.rounded_rectangle((72, 285, 445, 412), radius=28, fill=rgb("#688E9A"))
    d.rounded_rectangle((91, 272, 428, 367), radius=26, fill=rgb("#E9F0E7"))
    d.rounded_rectangle((104, 276, 220, 340), radius=20, fill=rgb("#FFF7E8"))
    d.polygon([(90, 344), (425, 344), (425, 392), (90, 392)], fill=rgb("#92B9B6"))
    d.rounded_rectangle((72, 395, 445, 430), radius=16, fill=rgb("#526D83"))
    # Small plant at the bedside.
    d.line((70, 302, 56, 235), fill=rgb("#446B5B"), width=7)
    d.ellipse((29, 225, 67, 258), fill=rgb("#6E9C77"))
    d.ellipse((53, 244, 91, 277), fill=rgb("#80B58C"))
    d.ellipse((35, 264, 68, 293), fill=rgb("#5D906D"))
    return im


def cover_study() -> Image.Image:
    im = gradient("#D9ECF4", "#E6D4B8")
    soft_glow(im, (245, 32, 473, 251), "#FFF4CE", 20)
    d = ImageDraw.Draw(im)
    # Sunlit window.
    d.rounded_rectangle((290, 55, 447, 211), radius=18, fill=rgb("#F8E7BC"), outline=rgb("#FFFFFF"), width=8)
    d.line((369, 60, 369, 207), fill=rgb("#D8B987"), width=6)
    d.line((297, 133, 442, 133), fill=rgb("#D8B987"), width=6)
    # Open book on desk.
    d.polygon([(54, 285), (248, 253), (250, 421), (64, 450)], fill=rgb("#3D628B"))
    d.polygon([(250, 253), (454, 286), (445, 450), (250, 421)], fill=rgb("#496C93"))
    d.polygon([(70, 293), (244, 266), (244, 403), (77, 426)], fill=rgb("#FFF7DE"))
    d.polygon([(257, 266), (438, 294), (428, 426), (257, 403)], fill=rgb("#FFF9E9"))
    for y in [318, 345, 372, 395]:
        d.line((94, y, 218, y-18), fill=rgb("#B8C9C7"), width=4)
        d.line((279, y-15, 409, y+5), fill=rgb("#B8C9C7"), width=4)
    d.line((250, 258, 250, 421), fill=rgb("#B08356"), width=6)
    # Floating music notes.
    d.ellipse((122, 198, 151, 222), fill=rgb("#D77FA0"))
    d.line((146, 209, 146, 151), fill=rgb("#D77FA0"), width=7)
    d.ellipse((168, 174, 196, 198), fill=rgb("#D77FA0"))
    d.line((191, 185, 191, 126), fill=rgb("#D77FA0"), width=7)
    d.line((146, 151, 191, 143), fill=rgb("#D77FA0"), width=7)
    # Desk edge.
    d.polygon([(36, 435), (475, 435), (495, 485), (18, 485)], fill=rgb("#A97F58"))
    return im


def cover_stress() -> Image.Image:
    im = gradient("#F8D8B4", "#F0A8A0")
    d = ImageDraw.Draw(im)
    # Sun and pastel horizon.
    d.ellipse((182, 82, 330, 230), fill=rgb("#FFE9A6"))
    d.rectangle((0, 218, 512, 512), fill=rgb("#E5A2A2"))
    d.rectangle((0, 258, 512, 512), fill=rgb("#D99DAD"))
    # Layered ocean waves.
    for y, color, amp, phase in [(294, "#9CBFD3", 20, 0.0), (342, "#6899B8", 23, 1.3),
                                  (392, "#487E9B", 17, 2.0), (445, "#38677F", 20, 0.8)]:
        points = [(0, 512), (0, y)]
        points += wave_points(y, amp, 2.2, phase, 0, 512)
        points += [(512, 512)]
        d.polygon(points, fill=rgb(color))
        for yy in range(y+5, y+25, 12):
            d.line(wave_points(yy, 6, 2.1, phase, 18, 495), fill=rgb("#D7E7E8"), width=3)
    # Distant birds.
    d.arc((100, 178, 133, 200), 190, 335, fill=rgb("#936D86"), width=4)
    d.arc((126, 176, 158, 199), 205, 345, fill=rgb("#936D86"), width=4)
    # Subtle foreground leaves.
    d.line((37, 476, 68, 413), fill=rgb("#517F6D"), width=6)
    d.ellipse((29, 424, 62, 454), fill=rgb("#70A28A"))
    d.ellipse((56, 438, 91, 465), fill=rgb("#81B398"))
    return im


def cover_nature() -> Image.Image:
    im = gradient("#D4E8D0", "#9BC5B7")
    soft_glow(im, (155, 30, 386, 271), "#EBEAC8", 25)
    d = ImageDraw.Draw(im)
    # Layered forest canopy.
    for x, y, r, color in [(16, 62, 102, "#548F65"), (94, 38, 88, "#7EB17B"),
                           (454, 64, 110, "#4B865C"), (383, 25, 88, "#75A970"),
                           (43, 202, 77, "#78A972"), (467, 204, 84, "#5E9A69")]:
        d.ellipse((x-r//2, y-r//2, x+r//2, y+r//2), fill=rgb(color))
    # Rock ledges.
    d.polygon([(110, 215), (230, 188), (294, 215), (380, 199), (446, 247),
               (407, 282), (146, 286)], fill=rgb("#687C77"))
    # Waterfall and pool.
    d.polygon([(205, 183), (309, 183), (333, 354), (184, 354)], fill=rgb("#A5DAE4"))
    d.polygon([(230, 190), (247, 190), (238, 345), (214, 345)], fill=rgb("#DFF7F4"))
    d.polygon([(284, 189), (299, 190), (316, 345), (293, 345)], fill=rgb("#7CC0D1"))
    d.ellipse((84, 325, 430, 459), fill=rgb("#5CA7B5"))
    d.ellipse((119, 350, 398, 447), fill=rgb("#7DC7CE"))
    # Foreground banks and foliage.
    d.polygon([(0, 386), (112, 345), (177, 405), (140, 512), (0, 512)], fill=rgb("#3F7857"))
    d.polygon([(512, 376), (401, 338), (358, 408), (392, 512), (512, 512)], fill=rgb("#477E58"))
    for x, y, color in [(61, 389, "#8EB879"), (90, 423, "#9FC987"), (438, 397, "#91BE7C"),
                        (405, 432, "#7EAF72"), (132, 463, "#6AA36B"), (380, 466, "#5F9964")]:
        d.ellipse((x-18, y-10, x+18, y+10), fill=rgb(color))
    return im


def cover_noise() -> Image.Image:
    im = gradient("#192650", "#392D78")
    soft_glow(im, (68, 54, 453, 440), "#4D65C5", 30)
    d = ImageDraw.Draw(im)
    # Soft speaker body and concentric sound waves.
    d.rounded_rectangle((178, 168, 336, 344), radius=28, fill=rgb("#273B72"), outline=rgb("#8DAAE7"), width=5)
    d.polygon([(205, 216), (249, 183), (249, 329), (205, 296)], fill=rgb("#D4DDF5"))
    d.ellipse((258, 211, 317, 271), outline=rgb("#A3BBEE"), width=7)
    d.ellipse((270, 223, 305, 258), outline=rgb("#E3CBFF"), width=5)
    for radius, color, width in [(104, "#79D4E4", 8), (143, "#8F9BEB", 7), (182, "#D18CE8", 5)]:
        d.arc((256-radius, 256-radius, 256+radius, 256+radius), 300, 60, fill=rgb(color), width=width)
        d.arc((256-radius, 256-radius, 256+radius, 256+radius), 120, 240, fill=rgb(color), width=width)
    # Sound waves at the base.
    d.line(wave_points(421, 18, 4.4, .3, 45, 467), fill=rgb("#84E2DD"), width=5)
    d.line(wave_points(447, 11, 4.4, 1.2, 45, 467), fill=rgb("#E6A9E9"), width=4)
    for x, y in [(79, 105), (413, 105), (115, 360), (390, 360)]:
        draw_star(d, x, y, 9, "#F5DCA7")
    return im


def cover_frequency() -> Image.Image:
    im = gradient("#171D52", "#322878")
    soft_glow(im, (58, 62, 456, 445), "#5144AA", 33)
    d = ImageDraw.Draw(im)
    for box, color, width in [
        ((52, 72, 460, 440), "#6C8AE3", 5),
        ((80, 98, 432, 414), "#8F7BE9", 5),
        ((111, 124, 401, 387), "#B58EE8", 4),
    ]:
        d.ellipse(box, outline=rgb(color), width=width)
    # Waveform with a calm spectrum across the center.
    colors = ["#68D5E7", "#76E3CB", "#E9D18A", "#EBA5C4", "#A5A1FF"]
    points = []
    for x in range(78, 435, 2):
        envelope = 0.35 + 0.65 * math.exp(-((x - 256) / 100) ** 2)
        y = 256 + math.sin((x - 78) / 356 * 18 * math.pi) * 70 * envelope
        points.append((x, round(y)))
    # Draw layered segments so the spectrum reads without text.
    for i, color in enumerate(colors):
        seg = points[i*70:(i+1)*70+1]
        if len(seg) > 1:
            d.line(seg, fill=rgb(color), width=7)
    d.line([(80, 256), (432, 256)], fill=rgb("#D7D9FF"), width=3)
    for x in [110, 158, 206, 254, 302, 350, 398]:
        d.ellipse((x-5, 251, x+5, 261), fill=rgb("#FFF1C2"))
    return im


def cover_sleep_audio() -> Image.Image:
    im = gradient("#252B66", "#6B68AB")
    soft_glow(im, (100, 28, 417, 346), "#7A7BC1", 28)
    d = ImageDraw.Draw(im)
    # Crescent moon.
    d.ellipse((183, 65, 332, 214), fill=rgb("#F6D68F"))
    d.ellipse((224, 42, 348, 174), fill=rgb("#31366F"))
    for x, y, r in [(107, 99, 11), (391, 101, 10), (365, 220, 8), (130, 230, 8)]:
        draw_star(d, x, y, r, "#FFF2C7")
    # Floating notes imply a quiet audio track.
    d.ellipse((352, 169, 379, 192), fill=rgb("#9BE1E1"))
    d.line((374, 180, 374, 131), fill=rgb("#9BE1E1"), width=6)
    d.ellipse((133, 179, 157, 201), fill=rgb("#F0AFD2"))
    d.line((153, 190, 153, 150), fill=rgb("#F0AFD2"), width=6)
    # Soft clouds along the bottom.
    for x, y, w, h, color in [
        (15, 323, 170, 100, "#C1C7E8"), (120, 348, 210, 95, "#D7D8EF"),
        (302, 319, 194, 112, "#B0B7E0"), (29, 380, 236, 99, "#A7B0D7"),
        (233, 384, 264, 94, "#C4CBE9"),
    ]:
        d.ellipse((x, y, x+w, y+h), fill=rgb(color))
    d.arc((54, 405, 224, 462), 205, 335, fill=rgb("#F5E9C8"), width=5)
    return im


GENERATORS = {
    "hl-sleep-prep.jpg": cover_sleep_prep,
    "sd-study.jpg": cover_study,
    "sd-stress.jpg": cover_stress,
    "sd-nature.jpg": cover_nature,
    "sd-noise.jpg": cover_noise,
    "sd-freq.jpg": cover_frequency,
    "sd-sleep.jpg": cover_sleep_audio,
}


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def missing_object(exc: ClientError) -> bool:
    code = str(exc.response.get("Error", {}).get("Code", ""))
    status = exc.response.get("ResponseMetadata", {}).get("HTTPStatusCode")
    return code in {"404", "NoSuchKey", "NotFound"} or status == 404


def upload_and_verify(dst, path: Path) -> tuple[str, int, str, str]:
    client = dst.client()
    body = path.read_bytes()
    digest = sha256_bytes(body)
    size = len(body)
    key = PREFIX + path.name
    existing = None
    try:
        existing = client.head_object(Bucket=dst.bucket, Key=key)
    except ClientError as exc:
        # This bucket may hide a not-yet-created key with 403 instead of 404.
        response_meta = exc.response.get("ResponseMetadata", {})
        code = str(exc.response.get("Error", {}).get("Code", ""))
        status = response_meta.get("HTTPStatusCode")
        if not missing_object(exc) and status != 403 and code not in {"403", "Forbidden"}:
            raise
    metadata = (existing or {}).get("Metadata") or {}
    identical = (
        existing is not None
        and int(existing.get("ContentLength") or -1) == size
        and metadata.get("sha256", "") == digest
    )
    state = "already-identical"
    if not identical:
        args = {
            "Bucket": dst.bucket, "Key": key, "Body": body,
            "ContentLength": size, "ContentType": "image/jpeg",
            "CacheControl": CACHE_CONTROL, "Metadata": {"sha256": digest},
        }
        try:
            client.put_object(ACL="public-read", **args)
        except ClientError:
            client.put_object(**args)
        state = "uploaded"

    stored = client.head_object(Bucket=dst.bucket, Key=key)
    if (int(stored.get("ContentLength") or -1) != size
            or (stored.get("Metadata") or {}).get("sha256") != digest):
        return path.name, size, state, "failed-head-size-or-hash"

    url = dst.public_url(key)
    try:
        response = requests.get(url, timeout=45)
        public = "verified-200-hash-match" if (
            response.status_code == 200 and sha256_bytes(response.content) == digest
        ) else f"failed-http-{response.status_code}-or-hash"
    except Exception as exc:  # noqa: BLE001
        public = "failed-" + type(exc).__name__
    return path.name, size, state, public


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True, help="App assets/practice-covers folder")
    parser.add_argument("--report", type=Path, default=Path("ci-report/missing-tile-covers-upload.md"))
    args = parser.parse_args()
    source = args.source.resolve()
    source.mkdir(parents=True, exist_ok=True)

    # Regenerate deterministic 512x512 illustrations on every run so the seven
    # committed APK covers and ParsPack objects always come from the same bytes.
    for name, factory in GENERATORS.items():
        image = factory().convert("RGB")
        if image.size != (SIZE, SIZE):
            raise SystemExit(f"Unexpected output size for {name}: {image.size}")
        path = source / name
        image.save(path, format="JPEG", quality=88, optimize=True, progressive=True)
        with Image.open(path) as check:
            check.verify()
        print(f"RENDERED {path.relative_to(ROOT)} {path.stat().st_size}B")

    dst = target("parspack")
    if dst.bucket != "c539776":
        raise SystemExit("Safety check failed: configured ParsPack bucket is not c539776")

    rows = []
    failures = 0
    uploaded = identical = 0
    total_bytes = 0
    for name in NAMES:
        result = upload_and_verify(dst, source / name)
        rows.append(result)
        filename, size, state, public = result
        total_bytes += size
        if public != "verified-200-hash-match":
            failures += 1
        elif state == "uploaded":
            uploaded += 1
        else:
            identical += 1
        print(f"{'OK' if public == 'verified-200-hash-match' else 'FAIL'} {PREFIX}{filename} {size}B write={state} public={public}")

    report = [
        "# ParsPack missing wellness tile covers", "",
        f"- Bucket: `{dst.bucket}`", f"- Expected files: {len(NAMES)}",
        f"- Uploaded: {uploaded}", f"- Already identical: {identical}",
        f"- Failures: {failures}", f"- Source bytes: {total_bytes}", "",
        "| File | Bytes | Write | Public verification | URL |",
        "|---|---:|---|---|---|",
    ]
    report.extend(
        f"| `{name}` | {size} | {state} | {public} | {dst.public_url(PREFIX + name)} |"
        for name, size, state, public in rows
    )
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text("\n".join(report) + "\n", encoding="utf-8")
    print(json.dumps({
        "expected": len(NAMES), "uploaded": uploaded, "identical": identical,
        "failures": failures, "bytes": total_bytes,
    }, ensure_ascii=False))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
