#!/usr/bin/env bash
# قانون دائمی: بعد از رینیم استاندارد و قبل از آپلود باکت،
# PDF را با Ghostscript فشرده کن تا حدود نصف حجم اصلی شود.
# اگر خروجی بزرگ‌تر از مبدأ شد، همان فایل اصلی می‌ماند.
set -euo pipefail
if [[ $# -lt 2 ]]; then
  echo "usage: $0 input.pdf output.pdf" >&2
  exit 2
fi
in="$1"
out="$2"
tmp="$(mktemp --suffix=.pdf)"
trap 'rm -f "$tmp"' EXIT
gs -q -dNOPAUSE -dBATCH -dSAFER \
  -sDEVICE=pdfwrite \
  -dCompatibilityLevel=1.4 \
  -dPDFSETTINGS=/ebook \
  -dDetectDuplicateImages=true \
  -dCompressFonts=true \
  -dSubsetFonts=true \
  -dColorImageDownsampleType=/Bicubic \
  -dGrayImageDownsampleType=/Bicubic \
  -dMonoImageDownsampleType=/Bicubic \
  -dColorImageResolution=110 \
  -dGrayImageResolution=110 \
  -dMonoImageResolution=300 \
  -sOutputFile="$tmp" \
  "$in"
in_sz=$(stat -c%s "$in")
out_sz=$(stat -c%s "$tmp")
if [[ "$out_sz" -lt 1024 ]]; then
  echo "compress failed (tiny output), keep original" >&2
  cp -f "$in" "$out"
  exit 0
fi
# اگر فشرده از ۹۵٪ مبدأ بزرگ‌تر بود، مبدأ را نگه دار
if [[ "$out_sz" -ge $((in_sz * 95 / 100)) ]]; then
  echo "no gain ($out_sz >= 95% of $in_sz), keep original"
  cp -f "$in" "$out"
else
  echo "compressed $in_sz -> $out_sz"
  cp -f "$tmp" "$out"
fi
