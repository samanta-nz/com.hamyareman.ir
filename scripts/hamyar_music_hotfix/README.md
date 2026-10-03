# همیار — Music/WebView Hotfix

مبنا: `19ad0d7f3c9c482616ce3beee512859997028bc5` (نسخهٔ منتشرشدهٔ 2.4.4)

## علت قطعیِ قابل اثبات از کد

### یوگا / ورزش
در HTML درس، `HamyaremanBackground.mount()` روی پیام `state.opened`، خود iframe موسیقی را `position:fixed; inset:0; z-index:2147483647` می‌کند و `document.body.style.overflow='hidden'` می‌گذارد.

در همان WebView، فایل `MusicFrameOverlayBridge.kt` نیز روی همان پیام، همین iframe را با `position:fixed`, `100vw/100vh`, `z-index:2147483647` و `overflow:hidden` مدیریت می‌کند.

یعنی یک state واحد، دو مالک layout/input دارد. این دقیقاً با علامت‌های گزارش‌شده هم‌خوان است: iframe اصلی ناپدید می‌شود، صفحهٔ میزبان دیگر scroll/touch نمی‌گیرد، و منوهای Compose/HTML زیر لایهٔ iframe قابل لمس نیستند.

پچ جدید یک مالک واحد ایجاد می‌کند: bridge در capture phase پیام `state` را می‌گیرد و قبل از listener خود HTML مصرف می‌کند، سپس فقط همان iframe را در flow صفحه به `min(620px, 78vh)` باز می‌کند؛ هیچ `position:fixed`, `inset:0`, `body overflow:hidden` روی سند میزبان اعمال نمی‌شود.

### بشنو و بخواب
`SleepNightScreen` یک `BackgroundMusicTileHost` را داخل `verticalScroll` قرار می‌دهد. خود `background-music-tile.html` در حالت open یک iframe داخلی را `position:fixed; inset:0; height:100dvh; z-index:2147483647` می‌کند، در حالی که Android نیز در نسخهٔ 2.4.4 ارتفاع WebView را با `animateDpAsState(... tween(340))` از 92dp به تقریباً ارتفاع صفحه تغییر می‌دهد.

پچ، انیمیشن/resize پیوستهٔ Android WebView را حذف می‌کند و fixed/inset iframe داخلی را داخل خود WebView به `position:absolute` و اندازهٔ محلی تبدیل می‌کند. در نتیجه ساختار لایه‌ای دیگر هم‌زمان دو بار روی یک transition سنگین نمی‌شود.

## چیزهایی که عمداً تغییر نکرده‌اند

- سه فایل کامل موسیقی روی ParsPack دست‌نخورده می‌مانند.
- رمزنگاری HMK1 و مسیر `shouldInterceptRequest` دست‌نخورده می‌مانند.
- URLهای باکت تغییر نمی‌کنند.
- فایل‌های صوتی embed شده تغییر نمی‌کنند.

## دربارهٔ مکث ۶–۷ ثانیه
کد موجود، یک تایمر native یا HTML با مدت ۶–۷ ثانیه برای این مکث نشان نمی‌دهد. بنابراین نسبت دادن این عدد دقیق به یک `setTimeout(6000/7000)` بدون Logcat/renderer trace درست نیست. علت layout/input که این پچ می‌زند از خود کد مستقیم قابل اثبات است؛ اما زمان دقیق stall را فقط runtime trace می‌تواند به یک پیام/renderer event مشخص وصل کند.

## استفاده

روی همان repository، فایل `build-hamyar-music-hotfix.yml` را در `.github/workflows/` قرار بده و از Actions > Run workflow اجرا کن. workflow به‌صورت پیش‌فرض دقیقاً از commit `19ad0d7f3c9c482616ce3beee512859997028bc5` build می‌کند و APK را به‌صورت artifact تحویل می‌دهد.

برای پچ دستی، repository را روی همان commit checkout کن و اجرا کن:

```bash
python3 apply_hotfix.py
```

بعد:

```bash
./gradlew :core-common:testDebugUnitTest :hamyar-app:testP09DebugUnitTest :hamyar-app:assembleP09Debug \
  -PhamyarVersionCode=2441 -PhamyarVersionName=2.4.4-hotfix
```
