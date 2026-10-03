# همیار من — پایهٔ نهم (`com.hamyareman.g09`)

اپ اندروید «همیار من» (Kotlin + Jetpack Compose، ماژولار) با بک‌اند Appwrite و
سرور محتوای داخلی پارس‌پک.

این مخزن، کپی کاملِ شاخهٔ کاریِ `arena/01a0fe97-com-hamyareman-ir` از مخزن
`samanta-nz/com.hamyareman.ir` در نسخهٔ **۲٫۴٫۴ (کد ۲۴۴)** است. فقط ورکفلوهایی
منتقل شده‌اند که دست‌کم یک اجرای **واقعی و موفق** داشته‌اند.

---

## ۱) ساختار

```
apps/hamyar-app      ← اپ دانش‌آموز، نقطهٔ کامپایل APK (فلیور p09)
apps/hamyar-admin    ← اپ ادمین (صف پرداخت/استرداد)
shared/              ← ماژول‌های اشتراکی: core-* و feature-*
gradle/              ← wrapper + کاتالوگ وابستگی‌ها
.github/workflows/   ← CI و انتشار (فقط ورکفلوهای موفق)
scripts/             ← ابزارهای پایتون/نود: رمزگذاری، آپلود، تست‌های jsdom
Bucket/Html-files/   ← نسخهٔ خام سه قاب موسیقی که روی باکت منتشر می‌شوند
0000/                ← نسخهٔ مرجع (canonical) همان سه قاب
content/, config/    ← کاتالوگ محتوا و پیکربندی امضا
archive/             ← مستندات و خروجی‌های تاریخی (کامپایل نمی‌شوند)
```

- namespace سورس‌ها: `com.hamyareman.ir(.platform.*)`
- `applicationId` نصبی پایهٔ نهم: **`com.hamyareman.p09`**
- هویت‌های دیتا که عمداً ثابت مانده‌اند: `roozhayeman_local` (نام استور)،
  `roozhayeman_private_v1` (alias کلید رمزنگاری)، نقش‌های `zahra/father/guest`.

## ۲) نسخهٔ فعلی

| مورد | مقدار |
|---|---|
| `versionName` / `versionCode` | `2.4.4` / `244` (در `apps/hamyar-app/build.gradle.kts`) |
| override در CI | `-PhamyarVersionName` و `-PhamyarVersionCode` |
| اثرِ انگشت کلید امضا | `5f091b6bf47d7294b37f8994099a6c88ac2e25182dd6cbe6a36da6b3bf804959` |
| ردیف اعلام نسخه | `app_state/app_release_grade9` در Appwrite |

## ۳) سرور محتوا

- سرور داخلی (تنها مقصد فعلی): `https://c539776.parspack.net`
- همهٔ HTMLها روی باکت **رمزشده** ذخیره می‌شوند: `HMK1` + ۱۲ بایت IV + AES-256-GCM.
  کلید از ردیف زندهٔ `app_state/html_media_key` خوانده می‌شود و هرگز داخل APK نیست.
- رمزگشایی در `ui/study/HmkWebViewClient.kt` و فقط برای همان هاست باکت انجام می‌شود:
  `BUCKET_BASE`/`BUCKET_HOST` ثابت‌اند و از انتخاب‌گر سرور (`ServerResolver`) مستقل‌اند.

### سه قاب موسیقی

| قاب | کجای اپ | نشانی |
|---|---|---|
| tile | «بشنو و بخواب» (`SleepNightScreen` → `BackgroundMusicTileHost`) | `…/Bucket/Html-files/background-music-tile.html` |
| full | «نجواهای آرام‌بخش طبیعت» (`CalmWhispersScreen`) | `…/Bucket/Html-files/background-music-full.html` |
| mini | فعلاً به هیچ صفحه‌ای وصل نیست (`BackgroundMusicHost`) | `file:///android_asset/content/background-music.html` |

هر سه فایل خودبسنده‌اند (۲۹ صدای embed، ≈۸٫۶ مگابایت) و تمِ روشن/تاریک را از
اپ (`HamyarAppearanceBridge`) یا از `prefers-color-scheme` می‌گیرند.

## ۴) ساخت محلی

```bash
./gradlew :hamyar-app:compileP09DebugKotlin          # فقط کامپایل
./gradlew :core-common:testDebugUnitTest \
          :hamyar-app:testP09DebugUnitTest           # تست‌های JVM
./gradlew :hamyar-app:assembleP09Debug \
  -PhamyarVersionCode=244 -PhamyarVersionName=2.4.4  # APK نصبی
```

تست رفتاری قاب‌های موسیقی (بدون اندروید، با jsdom):

```bash
npm install --no-save jsdom
python3 -m http.server 8000 --directory 0000 &
node scripts/test_background_music.mjs
```

## ۵) ورکفلوها

فقط ورکفلوهایی اینجا هستند که اجرای موفقِ واقعی داشته‌اند (اجراهای skip‌شدهٔ
یک‌دوثانیه‌ای، ناموفق و کنسل‌شده کنار گذاشته شدند). مهم‌ترین‌ها:

| ورکفلو | کار |
|---|---|
| `android.yml` | تست + ساخت APK نهم روی هر push به main |
| `arena-compile-check.yml` | فقط کامپایل و تست JVM (کامیت با `[compile]`) |
| `arena-release-grade9-v2.4.4.yml` | ساخت، امضا، آپلود دوگانه و اعلام نسخهٔ ۲٫۴٫۴ |
| `arena-music-html-sync.yml` | رمزگذاری و انتشار سه قاب موسیقی روی باکت |
| `arena-music-auto-theme-encode.yml` | رمزگذاری HMK1 نسخهٔ «تمِ خودکار» قاب‌ها |
| `arena-bucket-sync.yml` / `arena-parspack-*` | همگام‌سازی، ACL و بررسی باکت |

### Secretهای لازم

`APPWRITE_API_KEY`، `APPWRITE_PROJECT_ID`، `APPWRITE_DATABASE_ID`،
`PARSPACK_ACCESS_KEY`، `PARSPACK_SECRET_KEY`، و برای انتشار عمومی
`RELEASES_TOKEN` + متغیر `RELEASES_REPO`. بدون این‌ها ورکفلوهای انتشار در
همان مرحلهٔ صفر با پیام روشن متوقف می‌شوند و چیزی نیمه‌کاره منتشر نمی‌کنند.

## ۶) قرارداد انتشار

کاملش در [`RELEASES.md`](RELEASES.md) است؛ خلاصه: اول فایل روی هر دو مقصد
می‌نشیند و دانلودپذیری‌اش تأیید می‌شود، **بعد** ردیف اعلام نسخه عوض می‌شود.
