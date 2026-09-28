#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build-cue.py — میکس کوئه‌ی تمام‌مدت از سکشن‌های TTS (voice-01)

مصرف:
    python build-cue.py --slug yoga-balasana --audio parts/yoga-balasana
    (فایل‌های سکشن در پوشه: f1.mp3 .. fN.mp3 — خروجی: cue-{cue}.mp3)

منطق (مطابق cue-spec.json):
    concat سکشن‌ها + سکوت silAfter بعد از هر سکشن + پد نهایی تا مدت هدف
    → loudnorm I=-16 / TP=-1.5 / LRA=9
    → fade in 0.5s / fade out 1.5s → atrim تا مدت هدف
    → AAC 64k mono 44.1kHz
"""
import json, argparse, subprocess, os, sys

ap = argparse.ArgumentParser()
ap.add_argument('--spec', default=os.path.join(os.path.dirname(__file__), 'cue-spec.json'))
ap.add_argument('--slug', required=True)
ap.add_argument('--audio', required=True, help='پوشه‌ی سکشن‌ها (f1.mp3 ...)')
ap.add_argument('--out', default=None)
a = ap.parse_args()

spec = json.load(open(a.spec, encoding='utf-8'))
e = next((x for x in spec if x['slug'] == a.slug), None)
if e is None:
    sys.exit(f'slug یافت نشد: {a.slug}')
out = a.out or f"cue-{e['cue']}.mp3"
TARGET = e['durationSec']

inputs, filters, concat = [], [], []
idx = [0]
def add_input(path):
    inputs.extend(['-i', path])
    idx[0] += 1
    return idx[0] - 1

n = len(e['sections'])
for i, sec in enumerate(e['sections']):
    v = add_input(os.path.join(a.audio, f'f{i+1}.mp3'))
    chain = f'[{v}:a]aresample=44100,aformat=channel_layouts=mono'
    last = (i == n - 1)
    sil = sec['silAfter'] + (e['finalPadSec'] if last else 0)
    if sil > 0:
        filters.append(f'{chain},apad=pad_dur={sil}[s{i}]')
    else:
        filters.append(f'{chain}[s{i}]')
    concat.append(f'[s{i}]')

filters.append(''.join(concat) + f'concat=n={len(concat)}:v=0:a=1[mix]')
filters.append('[mix]loudnorm=I=-16:TP=-1.5:LRA=9,aresample=44100[ln]')
filters.append(f'[ln]afade=t=in:st=0:d=0.5,afade=t=out:st={TARGET-1.5}:d=1.5,atrim=0:{TARGET}[fin]')

if out.lower().endswith('.mp3'):
    codec = ['-c:a', 'libmp3lame', '-b:a', '96k']
else:  # .m4a و مشابه
    codec = ['-c:a', 'aac', '-b:a', '64k']
cmd = (['ffmpeg', '-y'] + inputs + ['-filter_complex', ';'.join(filters),
       '-map', '[fin]'] + codec + ['-ar', '44100', '-ac', '1', out])
print('>>', ' '.join(cmd))
subprocess.run(cmd, check=True)
print(f'✔ {out} (هدف {TARGET}s)')
