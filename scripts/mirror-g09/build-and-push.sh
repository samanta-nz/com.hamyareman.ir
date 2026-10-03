#!/usr/bin/env bash
# ساخت و انتشار آینهٔ مخزن در samanta-nz/com.hamyareman.g09
# اجرا از ریشهٔ مخزن مبدأ: bash scripts/mirror-g09/build-and-push.sh
set -euo pipefail
SRC="$(git rev-parse --show-toplevel)"
OUT="${1:-/tmp/mirror}"
KEEP="$SRC/scripts/mirror-g09/workflows-keep.txt"
rm -rf "$OUT"; mkdir -p "$OUT"
git -C "$SRC" archive HEAD | tar -x -C "$OUT"
# فقط ورکفلوهای موفق
cd "$OUT"
for f in .github/workflows/*.yml; do
  grep -qx "$(basename "$f")" "$KEEP" || rm -f "$f"
done
cp "$SRC"/scripts/mirror-g09/docs/* "$OUT"/
git init -q -b main .
git add -A
git -c user.email=agent@arena.ai -c user.name="Arena Agent" commit -q -m "آینهٔ همیار من پایهٔ نهم — نسخهٔ ۲٫۴٫۴ (کد ۲۴۴)"
git remote add origin https://github.com/samanta-nz/com.hamyareman.g09.git
git push -u origin main
