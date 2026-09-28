# تستِ شیمِ سیکِ فهرست/سرفصل‌های HTML تدریس

شیم (`ensureSeekShim` در `MathLessonScreen.kt`) لینک‌های فهرست و سرفصل‌های داخلِ
متنِ تدریس را به پلیر وصل می‌کند: `HamyarPlayer.seek(ms)`.

این پوشه همان شیم را **روی DOM واقعی** (jsdom) با HTMLهای داخلِ
`apps/hamyar-app/src/main/assets/math/c905/` تست می‌کند؛ زمان‌های انتظار از
فهرست‌های زمان‌بندیِ `Books/Base-09/ریاضی/04- صوت تدریس/ryazif01*.txt` آمده‌اند.

```bash
python3 tools/seek-shim/extract_shim.py > /tmp/shim.js
node --check /tmp/shim.js          # syntax
npm i jsdom                        # یک‌بار
node tools/seek-shim/test_seek_shim.js
```

هر درس در دو حالت بررسی می‌شود: HTMLِ خودش (با `data-seek-ms`) و HTMLِ
«استریپ‌شده» (بدونِ `data-seek-ms`) که زمان‌ها از `TeachSeekMap` تزریق می‌شود —
یعنی نسخهٔ ریموت/قدیمی هم باید کار کند.
