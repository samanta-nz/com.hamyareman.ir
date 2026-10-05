from pathlib import Path
import math
import random
from PIL import Image, ImageDraw, ImageFilter, ImageChops

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "apps/hamyar-app/src/main/assets"
# تصویرهای واقعی (Gemini) در assets/ ریشه هستند و با sourceSets به APK می‌روند.
# اگر فایلی همان‌جا باشد، نسخهٔ رسم‌شده با کد ساخته نمی‌شود تا Gradle خطای
# «Duplicate resources» ندهد؛ فقط جای فایل‌های ناموجود fallback ساخته می‌شود.
REAL = ROOT / "assets"


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


def vignette(image, strength=0.55):
    w, h = image.size
    mask = Image.new("L", (w, h), 0)
    px = mask.load()
    cx, cy = w / 2.0, h / 2.0
    max_d = math.hypot(cx, cy)
    for y in range(h):
        for x in range(w):
            d = math.hypot(x - cx, y - cy) / max_d
            px[x, y] = int(255 * min(1.0, d * d * strength))
    shade = Image.new("RGB", (w, h), (0, 0, 0))
    return Image.composite(shade, image, mask)


def grain(image, amount=12, seed=7):
    rng = random.Random(seed)
    noise = Image.new("L", image.size)
    np = noise.load()
    for y in range(image.height):
        for x in range(image.width):
            np[x, y] = 128 + rng.randint(-amount, amount)
    noise = noise.filter(ImageFilter.GaussianBlur(0.18))
    return ImageChops.multiply(image.convert("RGB"), Image.merge("RGB", (noise, noise, noise)))


def lock_bg(safe=False):
    w, h = 900, 1950
    if safe:
        image = gradient(w, h, [(0, (3, 8, 23)), (0.50, (7, 25, 44)), (0.76, (10, 39, 50)), (1, (4, 13, 19))])
        seed = 29
        ridge_colors = [(0.69, (16, 40, 54, 255)), (0.79, (8, 28, 39, 255)), (0.91, (4, 16, 24, 255))]
    else:
        image = gradient(w, h, [(0, (6, 16, 38)), (0.31, (42, 47, 108)), (0.57, (126, 81, 139)), (0.75, (218, 119, 132)), (1, (242, 177, 120))])
        seed = 11
        ridge_colors = [(0.71, (63, 52, 111, 255)), (0.80, (33, 30, 76, 255)), (0.91, (12, 15, 42, 255))]
    draw = ImageDraw.Draw(image, "RGBA")
    rng = random.Random(seed)
    for _ in range(420 if safe else 250):
        x = rng.randint(0, w - 1)
        y = rng.randint(20, int(h * (0.64 if safe else 0.53)))
        r = rng.choice([1, 1, 2, 2, 3, 4])
        alpha = rng.randint(35, 160)
        draw.ellipse((x - r, y - r, x + r, y + r), fill=(245, 248, 255, alpha))
    if safe:
        draw.ellipse((w * .79, h * .09, w * .86, h * .16), fill=(239, 233, 255, 245))
        draw.ellipse((w * .755, h * .055, w * .89, h * .195), outline=(206, 221, 255, 45), width=5)
    for base, color in ridge_colors:
        points = []
        for i in range(181):
            x = w * i / 180
            t = x / w
            y = h * base - h * .075 * (
                .55 * math.sin(t * 2 * math.pi * 1.18 + .6) +
                .30 * math.sin(t * 2 * math.pi * 3.1 + 1.0) +
                .15 * math.sin(t * 2 * math.pi * 8.0 + .4)
            )
            points.append((x, y))
        points += [(w, h), (0, h)]
        draw.polygon(points, fill=color)
    image = grain(image, 8, seed)
    return vignette(image, 0.50)


def desk(kind):
    palette = {
        "diary": ((66, 39, 27), (154, 111, 67), (42, 25, 18)),
        "poetry": ((49, 42, 36), (173, 137, 84), (30, 25, 22)),
        "album": ((16, 27, 42), (69, 122, 154), (8, 15, 25)),
    }[kind]
    w, h = 1600, 1000
    image = gradient(w, h, [(0, palette[1]), (0.55, palette[0]), (1, palette[2])])
    draw = ImageDraw.Draw(image, "RGBA")
    rng = random.Random(100 + len(kind))
    for _ in range(1200):
        y = rng.randint(0, h)
        x = rng.randint(-300, w)
        length = rng.randint(120, 520)
        bend = rng.randint(8, 44)
        alpha = rng.randint(7, 28)
        draw.arc((x, y, x + length, y + bend), 0, 180, fill=palette[1] + (alpha,), width=rng.choice([1, 1, 2]))
    for _ in range(18):
        y = rng.randint(0, h)
        draw.line((0, y, w, y + rng.randint(-20, 20)), fill=(0, 0, 0, 10), width=rng.randint(5, 14))
    image = image.filter(ImageFilter.GaussianBlur(.42))
    return vignette(grain(image, 9, 100 + len(kind)), 0.32)


def paper():
    w, h = 900, 1273
    image = Image.new("RGB", (w, h), (247, 241, 226))
    draw = ImageDraw.Draw(image, "RGBA")
    rng = random.Random(77)
    for _ in range(5200):
        x, y = rng.randrange(w), rng.randrange(h)
        tone = rng.choice([(82, 65, 45, 6), (255, 255, 255, 9), (139, 116, 79, 5)])
        draw.point((x, y), fill=tone)
    for y in range(24, h - 24, 33):
        draw.line((70, y, w - 70, y), fill=(91, 129, 157, 34), width=1)
    draw.line((w - 106, 12, w - 106, h - 12), fill=(84, 121, 145, 82), width=2)
    draw.line((w - 98, 12, w - 98, h - 12), fill=(84, 121, 145, 38), width=1)
    return image.filter(ImageFilter.GaussianBlur(.10))


assets = {
    "lock/app-lock-bg.jpg": lambda: lock_bg(False),
    "lock/safespace-lock-bg.jpg": lambda: lock_bg(True),
    "diary/desk-bg.jpg": lambda: desk("diary"),
    "poetry/poetry-bg.jpg": lambda: desk("poetry"),
    "album/album-bg.jpg": lambda: desk("album"),
    "diary/paper-cream.jpg": paper,
}

for relative, make in assets.items():
    if (REAL / relative).exists():
        print(f"skip {relative}: real image exists in assets/")
        stale = OUT / relative
        if stale.exists():
            stale.unlink()
        continue
    image = make()
    target = OUT / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, "JPEG", quality=82, optimize=True, progressive=True)
    print(f"generated {target} ({target.stat().st_size} bytes)")
