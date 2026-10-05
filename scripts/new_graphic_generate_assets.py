from pathlib import Path
import math
import random
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "apps/hamyar-app/src/main/assets"


def gradient(w, h, stops):
    image = Image.new("RGB", (w, h))
    px = image.load()
    for y in range(h):
        t = y / max(1, h - 1)
        for i in range(len(stops) - 1):
            if stops[i][0] <= t <= stops[i + 1][0]:
                a, c1 = stops[i]
                b, c2 = stops[i + 1]
                u = (t - a) / max(1e-6, b - a)
                c = tuple(int(c1[k] * (1 - u) + c2[k] * u) for k in range(3))
                break
        for x in range(w):
            px[x, y] = c
    return image


def lock_bg(safe=False):
    w, h = 720, 1560
    if safe:
        image = gradient(w, h, [(0, (4, 10, 28)), (0.55, (7, 24, 45)), (1, (4, 15, 22))])
        ridges = [(0.70, (13, 34, 51, 255)), (0.81, (7, 24, 34, 255)), (0.92, (4, 15, 22, 255))]
        seed = 29
    else:
        image = gradient(w, h, [(0, (8, 18, 43)), (0.34, (45, 47, 112)), (0.60, (153, 91, 150)), (0.78, (229, 128, 135)), (1, (244, 178, 122))])
        ridges = [(0.72, (58, 49, 112, 255)), (0.80, (30, 29, 74, 255)), (0.90, (12, 15, 42, 255))]
        seed = 11
    draw = ImageDraw.Draw(image, "RGBA")
    rng = random.Random(seed)
    for _ in range(180 if safe else 100):
        x = rng.randint(0, w - 1)
        y = rng.randint(20, int(h * (0.62 if safe else 0.50)))
        r = rng.choice([1, 1, 2, 2, 3])
        draw.ellipse((x-r, y-r, x+r, y+r), fill=(255, 255, 255, rng.randint(60, 190)))
    if safe:
        draw.ellipse((w*.79, h*.10, w*.85, h*.16), fill=(238, 232, 255, 255))
    for base, color in ridges:
        points = []
        for i in range(121):
            x = w * i / 120
            t = x / w
            y = h * base - h * .065 * (
                .55 * math.sin(t * 2 * math.pi * 1.2 + .6) +
                .30 * math.sin(t * 2 * math.pi * 3.3 + 1.0) +
                .15 * math.sin(t * 2 * math.pi * 8.2 + .4)
            )
            points.append((x, y))
        points += [(w, h), (0, h)]
        draw.polygon(points, fill=color)
    return image


def desk(kind):
    palette = {
        "diary": ((72, 45, 30), (141, 100, 59)),
        "poetry": ((53, 46, 39), (166, 128, 73)),
        "album": ((20, 31, 45), (63, 112, 145)),
    }[kind]
    w, h = 1440, 900
    image = Image.new("RGB", (w, h), palette[0])
    draw = ImageDraw.Draw(image, "RGBA")
    rng = random.Random(100 + len(kind))
    for _ in range(260):
        y = rng.randint(0, h)
        x = rng.randint(-250, w)
        length = rng.randint(80, 430)
        draw.arc((x, y, x + length, y + rng.randint(10, 45)), 0, 180,
                 fill=palette[1] + (rng.randint(12, 50),), width=1)
    return image.filter(ImageFilter.GaussianBlur(.35))


def paper():
    w, h = 900, 1273
    image = Image.new("RGB", (w, h), (247, 241, 226))
    draw = ImageDraw.Draw(image, "RGBA")
    rng = random.Random(77)
    for _ in range(2600):
        draw.point((rng.randrange(w), rng.randrange(h)),
                   fill=(90, 72, 48, rng.randint(3, 12)))
    return image


assets = {
    "lock/app-lock-bg.jpg": lock_bg(False),
    "lock/safespace-lock-bg.jpg": lock_bg(True),
    "diary/desk-bg.jpg": desk("diary"),
    "poetry/poetry-bg.jpg": desk("poetry"),
    "album/album-bg.jpg": desk("album"),
    "diary/paper-cream.jpg": paper(),
}

for relative, image in assets.items():
    target = OUT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, "JPEG", quality=78, optimize=True, progressive=True)
    print(f"generated {target} ({target.stat().st_size} bytes)")
