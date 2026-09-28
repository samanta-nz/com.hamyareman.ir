#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build-full-session.py — میکس N فایل گفتار TTS به «یک فایل کامل دقیقاً ۱۵ دقیقه‌ای»
================================================================================
استراتژی: هر فراخوانی TTS سقف ~۱۵۰۰ کاراکتر دارد؛ پس جلسه به f1..fN شکسته می‌شود.
این اسکریپت همه را: کشش آرام ۰.۹۵x → نرمال‌سازی بلندی صدا (loudnorm) → پد سکوت
توزیع‌شده → ادغام بی‌درز → فید-این/فید-اوت حرفه‌ای، به «یک mp3 واحد» تبدیل می‌کند
که جمعش دقیقاً == total (پیش‌فرض ۹۰۰ ثانیه = ۱۵:۰۰) است و حجم آن بسیار زیر ۴۹MB.

استفاده:
  python3 build-full-session.py <dir-with-f*.mp3> [--total 900] [--tempo 0.95] [--out full-15min.mp3]

پیش‌نیاز: ffmpeg + ffprobe در PATH
"""
import argparse
import glob
import os
import subprocess
import sys
import tempfile


def dur(f: str) -> float:
    out = subprocess.run(
        ["ffprobe", "-v", "quiet", "-show_entries", "format=duration", "-of", "csv=p=0", f],
        capture_output=True, text=True, check=True,
    )
    return float(out.stdout.strip())


def run(cmd):
    subprocess.run(cmd, check=True, capture_output=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dir", help="پوشه‌ی شامل f1.mp3 f2.mp3 ...")
    ap.add_argument("--total", type=float, default=900.0, help="مدت نهایی ثانیه (پیش‌فرض 900)")
    ap.add_argument("--tempo", type=float, default=0.95, help="کندکردن ریتم (پیش‌فرض 0.95)")
    ap.add_argument("--out", default=None, help="مسیر خروجی (پیش‌فرض: همان پوشه/full-15min.mp3)")
    a = ap.parse_args()

    files = sorted(glob.glob(os.path.join(a.dir, "f*.mp3")))
    if not files:
        sys.exit("❌ هیچ f*.mp3 در پوشه پیدا نشد")

    durs = [dur(f) for f in files]
    print(f"📥 {len(files)} سگمنت — گفتار خام: {sum(durs):.0f}s")

    stretched = [d / a.tempo for d in durs]
    leftover = a.total - sum(stretched)
    if leftover < 0:
        # گفتار بیش از کل جلسه: ریتم را کمی تندتر کن تا دقیقاً جا شود
        a.tempo = sum(durs) / a.total
        stretched = [d / a.tempo for d in durs]
        leftover = 0.0
        print(f"⚙️ گفتار بلندتر از هدف بود؛ tempo اصلاح شد به {a.tempo:.3f}")

    pad = leftover / len(files)  # توزیع یکنواخت مکث بین سگمنت‌ها
    finals = [s + pad for s in stretched]
    print(f"🧮 کشش {a.tempo}x + سکوت {pad:.1f}s به‌ازای هر سگمنت → هدف {a.total:.0f}s")

    out = a.out or os.path.join(a.dir, "full-15min.mp3")
    with tempfile.TemporaryDirectory() as td:
        wavs = []
        for i, (f, target_d) in enumerate(zip(files, finals), 1):
            w = os.path.join(td, f"seg{i:02d}.wav")
            run([
                "ffmpeg", "-y", "-v", "error", "-i", f,
                "-filter:a",
                f"atempo={a.tempo},apad=pad_dur={pad + 1:.3f},loudnorm=I=-22:TP=-2.0:LRA=7",
                "-ar", "44100", "-ac", "1", "-t", f"{target_d:.3f}", w,
            ])
            wavs.append(w)

        lst = os.path.join(td, "list.txt")
        with open(lst, "w") as fh:
            for w in wavs:
                fh.write(f"file '{w}'\n")

        big = os.path.join(td, "all.wav")
        run(["ffmpeg", "-y", "-v", "error", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy", big])

        fade_start = a.total - 5
        run([
            "ffmpeg", "-y", "-v", "error", "-i", big,
            "-filter:a", f"afade=t=in:st=0:d=2,afade=t=out:st={fade_start}:d=5",
            "-ar", "44100", "-ac", "1", "-c:a", "libmp3lame", "-b:a", "64k",
            out,
        ])

    fd = dur(out)
    mb = os.path.getsize(out) / 1048576
    print(f"✅ {out}")
    print(f"   مدت: {fd:.1f}s = {int(fd // 60)}:{int(fd % 60):02d}")
    print(f"   حجم: {mb:.1f} MB (سقف مجاز 49 MB {'✅' if mb <= 49 else '❌'})")
    if abs(fd - a.total) > 1.0:
        print("⚠️ انحراف از مدت هدف بیش از ۱ ثانیه!")


if __name__ == "__main__":
    main()
