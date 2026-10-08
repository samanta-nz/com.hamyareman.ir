#!/usr/bin/env python3
"""Generate deterministic square covers for every menu tile lacking artwork.

The committed JPEGs are build assets; this script is a reproducible fallback for
new tiles and needs Pillow only when artwork is regenerated.  It never overwrites
an existing cover unless --force is supplied.
"""
from __future__ import annotations

import argparse
import hashlib
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "apps/hamyar-app/src/main/assets/practice-covers"
SIZE = 512

# Legacy asset intentionally removed from the current wellness catalog.
FORBIDDEN_IDS = {"hl-sleep-calm"}

MOTIFS = {
    "hl-cycle": "calendar", "pd-child": "rest", "pd-diaph": "breath", "pd-478": "breath",
    "th-strengths": "journal", "mu-pitch": "piano", "mu-staff": "staff",
    "mu-rhythm": "drum", "mu-meter": "metronome",
    "sk-speed": "book", "sk-hand": "pen", "sk-type": "keyboard", "sk-cornell": "notes",
    "sk-summary": "summary", "sk-debate": "debate", "sk-fallacy": "puzzle",
    "sk-present": "microphone", "sk-voice": "voice", "sk-email": "envelope",
    "sk-listen": "listen", "sk-math": "math", "sk-palace": "castle", "sk-chain": "chain",
    "sk-lateral": "bulb", "sk-family": "home", "sk-friend": "friends",
    "sk-love-lang": "heart", "sk-feel": "face", "sk-esteem": "sprout", "sk-bias": "brain",
    "sk-resilience": "leaf", "sk-plan": "checklist", "sk-habit": "cycle", "sk-desk": "desk",
    "sk-win": "computer", "sk-ai-what": "network", "sk-ai-use": "ai-book",
    "sk-ai-limit": "warning", "sk-search": "search", "sk-privacy": "lock",
    "sk-fake": "news", "sk-team": "team", "sk-conflict": "peace", "sk-no": "stop",
    "sk-budget": "coins", "sk-need": "target", "sk-storm": "cloud", "sk-everyday": "tools",
}

PALETTES = {
    "hl": ((247, 210, 221), (245, 239, 227), (133, 52, 83)),
    "pd": ((241, 205, 215), (236, 222, 242), (117, 57, 92)),
    "mu": ((224, 204, 166), (244, 232, 207), (79, 57, 43)),
    "th": ((204, 226, 208), (239, 231, 205), (53, 92, 66)),
    "sk": ((193, 221, 230), (238, 226, 200), (42, 78, 92)),
}


def palette(tile_id: str):
    return PALETTES.get(tile_id.split("-", 1)[0], PALETTES["sk"])


def gradient(seed: int, start, end) -> Image.Image:
    image = Image.new("RGB", (SIZE, SIZE))
    px = image.load()
    rng = random.Random(seed)
    for y in range(SIZE):
        t = y / (SIZE - 1)
        for x in range(SIZE):
            glow = 0.08 * math.sin((x / SIZE) * math.pi)
            noise = rng.randint(-3, 3)
            px[x, y] = tuple(max(0, min(255, int(a * (1 - t) + b * t + 255 * glow + noise))) for a, b in zip(start, end))
    return image


def line(draw: ImageDraw.ImageDraw, points, color, width=13, joint="curve"):
    draw.line(points, fill=color, width=width, joint=joint)


def people(draw, ink, accent, count=2):
    xs = [256] if count == 1 else ([205, 307] if count == 2 else [170, 256, 342])
    for i, x in enumerate(xs):
        y = 212 if count < 3 or i == 1 else 230
        draw.ellipse((x-34, y-34, x+34, y+34), outline=ink, width=12, fill=(255, 248, 230))
        draw.arc((x-62, y+35, x+62, y+158), 190, 350, fill=accent if i % 2 else ink, width=14)


def motif(draw: ImageDraw.ImageDraw, kind: str, ink, accent):
    if kind == "book":
        draw.rounded_rectangle((105,170,407,355),24,fill=(255,249,232),outline=ink,width=12); line(draw,[(256,180),(256,350)],accent,9); line(draw,[(130,210),(220,225)],ink,8); line(draw,[(292,225),(382,210)],ink,8)
    elif kind == "pen":
        line(draw,[(150,350),(342,158)],ink,30); line(draw,[(326,150),(365,189)],accent,30); draw.polygon([(135,365),(155,315),(185,345)],fill=ink); line(draw,[(118,390),(390,390)],accent,9)
    elif kind == "keyboard":
        draw.rounded_rectangle((90,175,422,350),22,fill=(255,249,232),outline=ink,width=12)
        for r in range(3):
            for c in range(8): draw.rounded_rectangle((115+c*36,200+r*38,141+c*36,226+r*38),5,outline=accent,width=5)
        draw.rounded_rectangle((180,318,332,336),5,fill=ink)
    elif kind in {"notes","checklist","journal"}:
        draw.rounded_rectangle((135,105,377,407),22,fill=(255,249,232),outline=ink,width=12)
        for y in (180,240,300,360): line(draw,[(190,y),(340,y)],accent,8)
        if kind == "checklist":
            for y in (180,240,300): draw.rectangle((155,y-12,178,y+11),outline=ink,width=5)
        elif kind == "journal": draw.ellipse((165,135,210,180),fill=accent)
        else: line(draw,[(175,115),(175,400)],accent,8)
    elif kind == "summary":
        for y,w in ((165,235),(215,185),(265,145)): line(draw,[(110,y),(110+w,y)],ink,10)
        line(draw,[(130,340),(340,340)],accent,18); draw.polygon([(340,315),(405,340),(340,365)],fill=accent)
    elif kind == "debate":
        people(draw,ink,accent,2); draw.rounded_rectangle((100,105,235,170),24,outline=ink,width=9); draw.rounded_rectangle((277,95,412,160),24,outline=accent,width=9)
    elif kind == "puzzle":
        draw.rounded_rectangle((135,135,377,377),28,fill=(255,249,232),outline=ink,width=12); draw.ellipse((225,105,287,165),fill=accent); draw.ellipse((347,225,407,287),fill=accent); line(draw,[(256,145),(256,367)],ink,8); line(draw,[(145,256),(367,256)],ink,8)
    elif kind in {"microphone","voice"}:
        draw.rounded_rectangle((210,105,302,295),46,fill=(255,249,232),outline=ink,width=12); draw.arc((165,170,347,350),0,180,fill=accent,width=14); line(draw,[(256,350),(256,410)],ink,14); line(draw,[(190,410),(322,410)],ink,14)
        if kind == "voice":
            for dx in (0,25): draw.arc((315+dx,145-dx,390+dx,255+dx),270,90,fill=accent,width=8)
    elif kind == "envelope":
        draw.rounded_rectangle((90,150,422,365),22,fill=(255,249,232),outline=ink,width=12); line(draw,[(105,170),(256,285),(407,170)],accent,13); line(draw,[(105,350),(215,255)],ink,8); line(draw,[(407,350),(297,255)],ink,8)
    elif kind == "listen":
        draw.arc((130,95,345,405),265,100,fill=ink,width=18); draw.arc((190,170,305,330),265,105,fill=accent,width=14); draw.arc((300,165,430,345),270,90,fill=accent,width=9)
    elif kind == "math":
        line(draw,[(110,210),(220,210)],ink,16); line(draw,[(165,155),(165,265)],ink,16); line(draw,[(295,165),(395,265)],accent,16); line(draw,[(395,165),(295,265)],accent,16); line(draw,[(135,345),(377,345)],ink,13)
    elif kind == "castle":
        draw.rectangle((135,205,377,390),fill=(255,249,232),outline=ink,width=12); draw.rectangle((115,155,190,390),fill=(255,249,232),outline=ink,width=12); draw.rectangle((322,155,397,390),fill=(255,249,232),outline=ink,width=12); draw.arc((215,285,297,425),180,360,fill=accent,width=12)
    elif kind == "chain":
        draw.ellipse((105,185,275,335),outline=ink,width=20); draw.ellipse((237,185,407,335),outline=accent,width=20); line(draw,[(215,260),(297,260)],ink,15)
    elif kind == "bulb":
        draw.ellipse((160,90,352,310),fill=(255,249,215),outline=ink,width=12); line(draw,[(210,285),(225,385),(287,385),(302,285)],accent,12); line(draw,[(220,420),(292,420)],ink,14)
        for a in range(0,360,45):
            x=256+int(145*math.cos(math.radians(a))); y=200+int(145*math.sin(math.radians(a))); x2=256+int(180*math.cos(math.radians(a))); y2=200+int(180*math.sin(math.radians(a))); line(draw,[(x,y),(x2,y2)],accent,8)
    elif kind == "home":
        draw.polygon([(95,260),(256,105),(417,260)],fill=(255,249,232),outline=ink); draw.rectangle((130,245,382,405),fill=(255,249,232),outline=ink,width=12); draw.rounded_rectangle((225,300,287,405),20,outline=accent,width=10)
    elif kind in {"friends","team"}:
        people(draw,ink,accent,3 if kind=="team" else 2)
    elif kind == "heart":
        draw.polygon([(256,405),(110,245),(120,155),(190,120),(256,185),(322,120),(392,155),(402,245)],fill=(255,241,232),outline=ink); line(draw,[(155,255),(220,255),(245,215),(280,300),(310,255),(365,255)],accent,11)
    elif kind == "face":
        draw.ellipse((105,105,407,407),fill=(255,249,232),outline=ink,width=12); draw.ellipse((175,205,205,235),fill=ink); draw.ellipse((307,205,337,235),fill=ink); draw.arc((180,225,332,345),15,165,fill=accent,width=12)
    elif kind in {"sprout","leaf"}:
        line(draw,[(256,405),(256,205)],ink,16); draw.ellipse((130,155,255,265),fill=(226,245,212),outline=accent,width=10); draw.ellipse((257,120,392,245),fill=(226,245,212),outline=ink,width=10); draw.arc((120,355,392,455),180,360,fill=accent,width=10)
    elif kind == "brain":
        draw.ellipse((115,125,397,370),fill=(255,239,225),outline=ink,width=12); line(draw,[(256,135),(256,360)],accent,9); draw.arc((145,165,260,280),110,300,fill=ink,width=8); draw.arc((250,205,370,340),250,80,fill=accent,width=8)
    elif kind == "cycle":
        draw.arc((110,110,402,402),35,210,fill=ink,width=20); draw.polygon([(105,285),(125,205),(180,265)],fill=ink); draw.arc((110,110,402,402),215,390,fill=accent,width=20); draw.polygon([(407,227),(387,307),(332,247)],fill=accent)
    elif kind == "desk":
        draw.rectangle((95,235,417,285),fill=(255,249,232),outline=ink,width=11); line(draw,[(135,285),(115,415)],ink,14); line(draw,[(377,285),(397,415)],ink,14); draw.rectangle((170,145,342,235),outline=accent,width=10); line(draw,[(256,235),(256,275)],accent,10)
    elif kind == "computer":
        draw.rounded_rectangle((100,100,412,335),24,fill=(244,250,249),outline=ink,width=12); line(draw,[(100,175),(412,175)],accent,9); line(draw,[(256,335),(256,405)],ink,14); line(draw,[(185,405),(327,405)],ink,14)
        for x in (130,165,200): draw.ellipse((x,132,x+16,148),fill=accent)
    elif kind in {"network","ai-book"}:
        nodes=[(150,170),(360,155),(256,255),(155,355),(360,365)]
        for a,b in ((0,2),(1,2),(2,3),(2,4)): line(draw,[nodes[a],nodes[b]],accent,9)
        for x,y in nodes: draw.ellipse((x-25,y-25,x+25,y+25),fill=(255,249,232),outline=ink,width=8)
        if kind=="ai-book": draw.rounded_rectangle((170,300,342,425),18,outline=ink,width=10)
    elif kind == "warning":
        draw.polygon([(256,80),(435,405),(77,405)],fill=(255,243,210),outline=ink); line(draw,[(256,175),(256,305)],accent,22); draw.ellipse((243,340,269,366),fill=accent)
    elif kind == "search":
        draw.ellipse((105,105,330,330),fill=(246,251,247),outline=ink,width=16); line(draw,[(310,310),(415,415)],accent,28)
    elif kind == "lock":
        draw.rounded_rectangle((135,225,377,420),26,fill=(255,249,232),outline=ink,width=12); draw.arc((175,80,337,300),180,360,fill=accent,width=18); draw.ellipse((238,285,274,321),fill=ink); line(draw,[(256,315),(256,365)],ink,12)
    elif kind == "news":
        draw.rounded_rectangle((105,95,407,417),20,fill=(255,249,232),outline=ink,width=12); draw.rectangle((135,135,245,245),outline=accent,width=10)
        for y in (150,195,270,315,360): line(draw,[(270 if y<245 else 135,y),(375,y)],ink,8)
        line(draw,[(145,380),(225,300)],accent,13); line(draw,[(145,300),(225,380)],accent,13)
    elif kind == "peace":
        draw.rounded_rectangle((80,120,235,240),30,outline=ink,width=10); draw.rounded_rectangle((277,120,432,240),30,outline=accent,width=10); draw.arc((135,230,377,440),180,360,fill=ink,width=13); draw.polygon([(256,315),(220,365),(292,365)],fill=accent)
    elif kind == "stop":
        pts=[(180,85),(332,85),(427,180),(427,332),(332,427),(180,427),(85,332),(85,180)]
        draw.polygon(pts,fill=(255,239,225),outline=ink); line(draw,[(150,256),(362,256)],accent,28)
    elif kind == "coins":
        for x,y in ((165,315),(256,285),(347,330)): draw.ellipse((x-65,y-35,x+65,y+35),fill=(255,231,155),outline=ink,width=9)
        draw.rounded_rectangle((110,105,402,255),35,outline=accent,width=12); draw.ellipse((226,140,286,200),outline=ink,width=9)
    elif kind == "target":
        for r,c in ((155,ink),(110,accent),(60,ink)): draw.ellipse((256-r,256-r,256+r,256+r),outline=c,width=13)
        line(draw,[(256,256),(405,107)],accent,16); draw.polygon([(405,107),(350,118),(394,162)],fill=accent)
    elif kind == "cloud":
        draw.ellipse((95,190,250,350),fill=(247,250,246),outline=ink,width=10); draw.ellipse((180,120,345,350),fill=(247,250,246),outline=ink,width=10); draw.ellipse((275,195,425,350),fill=(247,250,246),outline=ink,width=10); line(draw,[(145,385),(365,385)],accent,12)
    elif kind == "tools":
        line(draw,[(135,385),(350,170)],ink,28); draw.ellipse((285,105,405,225),outline=accent,width=18); draw.polygon([(120,405),(155,330),(195,370)],fill=accent)
    elif kind == "calendar":
        draw.rounded_rectangle((105,115,407,405),25,fill=(255,249,245),outline=ink,width=12); line(draw,[(105,190),(407,190)],accent,13)
        for y in (235,290,345):
            for x in (155,220,285,350): draw.ellipse((x-9,y-9,x+9,y+9),fill=accent if (x+y)%3 else ink)
    elif kind in {"rest","breath"}:
        draw.ellipse((190,125,322,257),fill=(255,249,232),outline=ink,width=11); draw.arc((125,230,387,430),190,350,fill=accent,width=16)
        if kind=="breath":
            for r in (45,75,105): draw.arc((256-r,245-r,256+r,245+r),290,70,fill=ink,width=7)
        else: line(draw,[(145,350),(367,350)],ink,12)
    elif kind == "piano":
        draw.rounded_rectangle((75,145,437,365),18,fill=(255,249,232),outline=ink,width=12)
        for x in range(95,420,45): line(draw,[(x,155),(x,355)],ink,6)
        for x in (125,215,260,350): draw.rectangle((x,155,x+30,270),fill=ink)
    elif kind == "staff":
        for y in (170,215,260,305,350): line(draw,[(85,y),(427,y)],ink,8)
        draw.ellipse((215,225,275,270),fill=accent); line(draw,[(268,245),(268,120)],accent,10)
    elif kind == "drum":
        draw.ellipse((120,150,392,245),fill=(255,249,232),outline=ink,width=12); draw.polygon([(120,198),(150,390),(362,390),(392,198)],fill=(255,240,215),outline=ink); line(draw,[(155,105),(335,300)],accent,13); line(draw,[(357,105),(177,300)],accent,13)
    elif kind == "metronome":
        draw.polygon([(165,405),(347,405),(320,105),(192,105)],fill=(255,249,232),outline=ink); line(draw,[(256,345),(310,145)],accent,12); draw.ellipse((295,130,325,160),fill=accent)
    else:
        draw.ellipse((120,120,392,392),fill=(255,249,232),outline=ink,width=14); draw.ellipse((205,205,307,307),fill=accent)


def generate(tile_id: str, destination: Path) -> None:
    seed = int(hashlib.sha256(tile_id.encode()).hexdigest()[:8], 16)
    start, end, ink = palette(tile_id)
    image = gradient(seed, start, end)
    draw = ImageDraw.Draw(image)
    accent = tuple(max(0, min(255, x + d)) for x, d in zip(ink, (55, 35, 45)))
    draw.rounded_rectangle((45,45,467,467),55,fill=(255,255,255,90),outline=(255,255,255),width=5)
    motif(draw, MOTIFS[tile_id], ink, accent)
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination, "JPEG", quality=88, optimize=True, progressive=True)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()
    forbidden_in_generator = FORBIDDEN_IDS.intersection(MOTIFS)
    if forbidden_in_generator:
        raise SystemExit(
            "forbidden legacy practice-cover IDs present in generator: "
            + ", ".join(sorted(forbidden_in_generator))
        )

    for legacy_id in sorted(FORBIDDEN_IDS):
        stale = OUTPUT / f"{legacy_id}.jpg"
        if stale.exists():
            stale.unlink()
            print("removed legacy", stale.relative_to(ROOT))

    made = skipped = 0
    for tile_id in sorted(MOTIFS):
        target = OUTPUT / f"{tile_id}.jpg"
        if target.exists() and not args.force:
            skipped += 1
            continue
        generate(tile_id, target)
        made += 1
        print("generated", target.relative_to(ROOT))
    print(f"done generated={made} existing={skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
