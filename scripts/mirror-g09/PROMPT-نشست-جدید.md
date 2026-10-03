# پرامپت آمادهٔ نشست جدید — اعمال پچ «music-iframe-host-fix»

> متن زیر را کامل کپی کن و در نشست تازه بفرست.

---

مخزن: `samanta-nz/com.hamyareman.ir` — شاخهٔ کاری: `arena/01a0fe97-com-hamyareman-ir`.

**کار این نشست: پچ `music-iframe-host-fix-bundle.zip` را که در ریشهٔ همین شاخه آپلود شده کامل اعمال کن، کامپایل را سبز کن و نتیجه را گزارش بده. فکر کردن و بحث لازم نیست؛ اعمال کن.**

## ۱) فایل‌های پچ

`music-iframe-host-fix-bundle.zip` در ریشهٔ شاخه شامل:
- `music-iframe-host-fix.patch` (قالب `*** Begin Patch` است، نه diff یونیفاید؛ با `git apply` اعمال نمی‌شود — تغییرها را دستی و دقیقاً مطابق پچ روی فایل‌ها بنویس)
- `CAUSE-REPORT-fa.md`, `README.md`, `verify-music-iframe-fix.yml`

## ۲) دقیقاً این سه تغییر

1. **`apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/MusicFrameOverlayBridge.kt`**
   پل فقط «گزارش‌دهندهٔ وضعیت» شود: حذف کاملِ تزریق stylesheet، حذف attribute `data-hamyar-music-open`، حذف کلاس `hamyar-music-open` روی `html`، حذف `iframe.focus()`. فقط با `activeFrame` و `HamyarMusicOverlay.onChanged(opened)` کار کند و `__hamyarCloseMusicOverlay` تنها پیام `close` را به همان iframe باز بفرستد و بعد از ۳۵۰ms وضعیت را false اعلام کند. (هندسهٔ iframe فقط مال خود HTML درس است: `HamyaremanBackground.mount` → `resize(open)`.)
2. **`apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/BackgroundMusicTileHost.kt`**
   حذف `animateDpAsState`/`tween` و ایمپورت‌هایشان؛ ارتفاع یک‌باره عوض شود: `Box(modifier.fillMaxWidth().height(target))`.
3. **`apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/SleepNightScreen.kt`**
   کل صفحه داخل یک `Box(Modifier.fillMaxSize())` برود؛ در ستون اسکرول‌شونده به‌جای خودِ tile یک `Spacer(Modifier.height(92.dp))` بماند؛ و `BackgroundMusicTileHost` با
   `Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(20f)` بیرون از `verticalScroll` به‌عنوان overlay قرار بگیرد. ایمپورت‌های لازم: `Box`, `Alignment`, `zIndex`.
   توجه: `SHOW_TOP_BAR` و `SHOW_SLEEP_TIMER` و بقیهٔ محتوا باید دست‌نخورده بمانند (فقط جابه‌جا می‌شوند).

### وضعیت فعلی (مهم)
بند ۱ و ۲ **قبلاً اعمال و push شده‌اند** (کامیت «اعمال پچ…»). فقط **بند ۳ (`SleepNightScreen.kt`) باقی مانده**؛ آن را اعمال کن و بعد کامپایل بگیر. اگر دیدی بند ۱ یا ۲ هم اعمال نشده (عقب‌گرد سندباکس)، دوباره اعمالشان کن.

## ۳) تأیید

- کامیت با `[compile]` در پیام تا ورکفلو `arena-compile-check.yml` اجرا شود؛ باید success شود.
- `gh run list --branch arena/01a0fe97-com-hamyareman-ir` برای دیدن نتیجه. (`gh workflow run` برای این توکن ۴۰۳ است؛ فقط با پیام کامیت تریگر کن.)
- بعد از سبز شدن، نتیجه را گزارش بده. **بدون اجازهٔ کاربر بیلد/انتشار نکن.**

## ۴) قواعد ثابت کاربر (رعایت کن)

- قبل از هر بیلد و انتشار، **شمارهٔ نسخه و نوع آپدیت (اختیاری/اجباری)** را بپرس و منتظر جواب بمان؛ بعد تنظیمات Appwrite را هم اعمال کن.
- هر مغایرت پچ با معماری/قوانین اپ را بپرس، ولی برای همین پچ تصمیم گرفته شده: اعمال شود.
- سه فایل موسیقی باید **خودبسنده** بمانند (صداها embed)، معماری iframe-wrapper رد شده است.
- نسخهٔ مرجع فایل‌های موسیقی: `main:0000/`.
- کد پنهان‌شده حذف نشود (`SHOW_SLEEP_TIMER` / `SHOW_TOP_BAR`).
- فاصلهٔ polling وضعیت = ۳ ثانیه.

## ۵) کارِ بازِ بعدی (فقط اگر کاربر گفت)

کاربر خواسته **رمزگذاری HMK1 برای سه فایل موسیقی برداشته شود** و اپ آن‌ها را بدون رمز لود کند، چون «هر فایل ۲۹ صدا دارد و همه هم‌زمان دیکد می‌شوند».
شواهد اندازه‌گیری‌شدهٔ نشست قبل (با Chromium واقعی روی همین فایل‌ها) را به کاربر یادآوری کن:
- در انتهای هر سه فایل این خط هست: `const resuming=tryResume(); if(resuming){reconcile();setTimeout(warmContent,2500)} else warmContent();`
  یعنی **در هر بار لود، `warmContent()` خودکار اجرا می‌شود** و ۲۹ عکس (`img.decode()`) و ۲۹ صدا (`atob` + `decodeAudioData` روی `OfflineAudioContext`) را پشت‌سرهم آماده می‌کند.
- زمان‌های اندازه‌گیری‌شده تا `assetReady`: **۲٫۲ ثانیه** (بدون کندسازی)، **۸٫۷ ثانیه** (۶ برابر کندتر)، **۱۴٫۷ ثانیه** (۱۰ برابر کندتر)؛ با وقفه‌های ترد اصلی تا **۷۵۸ms**.
- رمزگشایی HMK1 فقط **یک بار AES-GCM روی ~۸٫۶ مگابایت** است (ده‌ها میلی‌ثانیه). پس برداشتن رمز این کندی را حل نمی‌کند؛ راه‌حل واقعی، تنبل‌کردن یا حذف `warmContent` (یا تکه‌تکه‌کردن آن) است.
- اندازه‌گیری صفحهٔ «بشنو و بخواب»: کاشی با `calc(100dvh - 92px)` هنگام لمس باز می‌شود در حالی که viewport هنوز ۹۲ پیکسل است (ارتفاع پنل = صفر)، و تایمر بی‌لمسی ۲ ثانیه‌ای (`AUTO_COLLAPSE_MS = TILE?2000:10000`) قبل از آماده‌شدن چیدمان آن را می‌بندد؛ در کندی ۲۰ برابر، پنل **هرگز** پیش از بسته‌شدن دیده نمی‌شود.

## ۶) تله‌های محیط (مهم)

- سندباکس بارها عقب برگشته است. قبل از هر ویرایش: `git log --oneline -1` و در صورت نیاز
  `git stash -u -q; git fetch -q origin arena/01a0fe97-com-hamyareman-ir; git reset --hard -q FETCH_HEAD`.
- فقط روی همین شاخه کار و push کن.
- گزارش‌های تشخیصی موجود: `ci-report/lesson-music-host.md`، `ci-report/lesson-yoga-stripped.html`، `ci-report/lesson-sport-stripped.html`.
- برای تست مرورگری: کروم از طریق `npm i @sparticuz/chromium puppeteer-core` و استخراج `bin/al2023.tar.br` در `/tmp/al2023` با `LD_LIBRARY_PATH=/tmp/al2023/lib:/tmp` اجرا می‌شود (دانلود مستقیم کروم بسته است).
