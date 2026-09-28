#!/usr/bin/env bash
# =====================================================================
# pad-guided-audio.sh — استراتژی «۴ فایل = دقیقاً ۱۵:۰۰ پشت‌سرهم»
# ---------------------------------------------------------------------
# محدودیت: هر فراخوانی TTS حداکثر ~۱۵۰۰ کاراکتر ≈ ۲.۵ تا ۳ دقیقه گفتار.
# راه‌حل: هر بخش = گفتار TTS (تا سقف) + کشش آرام ۰.۹۵x (ریتم مدیتیشن)
#         + پد سکوت تا مدت دقیق هدف. جمع ۴ بخش = ۹۰۰ ثانیه (۱۵ دقیقه).
#
# اهداف مدت هر فایل (ثانیه):
#   p1 (استارت)  = ۲۷۰   (شامل مکث پایانی بلندِ آغاز)
#   p2 (دوم)     = ۲۴۰
#   p3 (سوم)     = ۲۴۰
#   p4 (پایانی)  = ۱۵۰   (بدون مکث — پایان جلسه)
#   جمع = ۹۰۰ ثانیه = ۱۵:۰۰ دقیقاً
#
# استفاده:  ./pad-guided-audio.sh <پوشه‌ی شامل p1..p4.mp3>
# مثال:     ./pad-guided-audio.sh artifacts/prompt-03-push/guided-audio/hypnosis/hyp-01-exam-calm
# پیش‌نیاز: ffmpeg
# نکته:     idempotent است — اجرای دوباره فقط فایل‌های کوتاه‌تر از هدف را پد می‌کند
#           (فایل کامل‌شده دست نمی‌خورد؛ چون atempo روی فایل پدشده دوباره اجرا نمی‌شود:
#            با فلگ duration در نام فایل کمکی کنترل می‌شود).
# =====================================================================
set -euo pipefail

DIR="${1:?usage: pad-guided-audio.sh <session-dir>}"
TEMPO="0.95"
TARGETS=(270 240 240 150)

command -v ffmpeg >/dev/null || { echo "❌ ffmpeg نصب نیست"; exit 1; }

dur() { ffprobe -v quiet -show_entries format=duration -of csv=p=0 "$1" | cut -d. -f1; }

echo "🎯 پد کردن ۴ بخش به جمع ۹۰۰ ثانیه: $DIR"
total=0
for i in 1 2 3 4; do
    f="$DIR/p$i.mp3"
    target="${TARGETS[$((i-1))]}"
    [ -f "$f" ] || { echo "  ⚠️ $f نیست — رد شد"; continue; }

    d=$(dur "$f")
    if [ "$d" -ge "$target" ]; then
        echo "  · p$i (${d}s ≥ ${target}s) از قبل کامل است"
        total=$((total + d))
        continue
    fi

    tmp="$f.pad.mp3"
    ffmpeg -y -v error -i "$f" -filter:a "atempo=${TEMPO},apad" -t "$target" \
        -c:a libmp3lame -b:a 48k "$tmp"
    mv "$tmp" "$f"
    d2=$(dur "$f")
    echo "  ✅ p$i: ${d}s → ${d2}s (هدف ${target}s)"
    total=$((total + d2))
done

echo "------------------------------------"
echo "🧮 جمع فعلی ۴ فایل: ${total}s (هدف: 900s)"
if [ "$total" -eq 900 ]; then
    echo "✅ دقیقاً ۱۵:۰۰ — آماده‌ی آپلود"
else
    echo "⚠️ فایل‌های ناقص موجودند؛ پس از تولید هر ۴ بخش دوباره اجرا کنید."
fi
