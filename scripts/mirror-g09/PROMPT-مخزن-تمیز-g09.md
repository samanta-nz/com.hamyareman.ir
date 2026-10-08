# پرامپت نشست تازه — ساخت مخزن تمیز `com.hamyareman.g09`

> کل متن زیر را کپی کن و در نشست تازه بفرست.

---

مخزن مبدأ: `samanta-nz/com.hamyareman.ir` — شاخهٔ کاری: `arena/01a0fe97-com-hamyareman-ir`.
مخزن مقصد (ساخته شده توسط کاربر، خالی): `samanta-nz/com.hamyareman.g09` — شاخهٔ `main`.

**کار این نشست: یک مخزن تمیز و کامل در مقصد بساز — فقط فایل‌های ضروریِ اپ، فقط ورکفلوهای موفق، مستندات کامل و به‌روز، بدون هیچ فایل اضافه. Secretها را خود کاربر اضافه می‌کند.**

## ۰) اول دسترسی را بررسی کن

```
gh api /repos/samanta-nz/com.hamyareman.g09 -q .full_name
```
بعد یک push آزمایشی بزن. اگر `403 Permission denied` گرفتی، به کاربر بگو در GitHub از مسیر
Settings → Applications → Arena → Configure → Repository access مخزن `com.hamyareman.g09` را هم اضافه کند و منتظر بمان. (این توکن اجازهٔ ساخت مخزن ندارد — `gh repo create` و `POST /user/repos` هر دو ۴۰۳ می‌دهند؛ تلاش نکن.)

## ۱) چه چیزی کپی شود — فهرست ضروری

از نوک شاخهٔ `arena/01a0fe97-com-hamyareman-ir` فقط این‌ها:

```
apps/                      ← سورس اپ دانش‌آموز و اپ ادمین (ضروری)
shared/                    ← ماژول‌های core-* و feature-*
gradle/  gradlew  gradlew.bat  build.gradle.kts  settings.gradle.kts  gradle.properties
local.properties.example   .gitignore   debug.keystore
config/                    ← اثر انگشت امضا
content/                   ← پیکربندی/کاتالوگ محتوا
scripts/                   ← فقط فایل‌های .py و .mjs و .sh کاربردی (نه zip، نه پچ‌های موقت)
tools/
bucket-sync/
Bucket/Html-files/*.html   ← سه فایل موسیقی (نسخهٔ منتشرشده روی باکت)
0000/*.html                ← نسخهٔ مرجع همان سه فایل
.github/workflows/         ← فقط فهرست «موفق» بند ۲
README.md  RELEASES.md  گزارش-تغییرات-همیار.md   ← بازنویسی‌شده طبق بند ۳
```

**کپی نشود (فایل اضافه):**
`Spliced-Books/` (۸۰۱ مگابایت)، `archive/` (۲۴٫۷)، `design-previews/` (۱۱)، `تصاویر یک/` (۳٫۳)،
`background-music.html` در ریشه (نسخهٔ تکراریِ همان فایل داخل `apps/.../assets/content/`)،
`background-music-auto-theme.zip` و `background-music-auto-theme-hmk1.zip` (۲۸٫۷ مگابایت)،
`music-iframe-host-fix-bundle.zip`، `scripts/I-F-html-patch/`، `ci-report/`، `docs/` (گزارش‌های تاریخی؛
فقط اگر کاربر خواست)، هر فایل `*.apk` / `dist/` / خروجی موقت.

نتیجهٔ هدف: حدود **۷۰ تا ۸۰ مگابایت و کمتر از ۸۰۰ فایل** (مبدأ ۹۶۲ مگابایت و ۳۳۳۴ فایل است).

## ۲) ورکفلوها — فقط ران‌های موفق

قاعده: ورکفلویی کپی می‌شود که در مخزن مبدأ **دست‌کم یک ران واقعاً موفق** داشته باشد.
هیچ ورکفلوی ۱ تا ۲ ثانیه‌ای (skip‌شده)، کنسل‌شده یا ناموفق کپی نشود.

فهرست را خودت از نو بساز تا تازه باشد:

```
gh api --paginate "/repos/samanta-nz/com.hamyareman.ir/actions/runs?status=success&per_page=100" \
  --jq '.workflow_runs[] | [.path, .run_started_at, .updated_at] | @tsv'
```
سپس برای هر `path` بیشینهٔ مدت‌زمان ران موفق را حساب کن و ورکفلوهای با بیشینهٔ **کمتر از ۳۰ ثانیه** را بینداز دور.

نتیجهٔ همین محاسبه در نشست قبل (۳۶ ورکفلو موفق؛ `drive-resumable-once.yml` با ۱۰ ثانیه حذف شد):

`arena-full-build` ۹۹۱s · `arena-parspack-migrate` ۷۵۴ · `arena-grade-validation` ۵۳۲ · `arena-parspack-acl` ۴۱۴ · `arena-bucket-sync` ۳۹۱ · `arena-release-grade9-v2.4` ۳۸۳ · `android` ۳۲۴ · `arena-release-grade9-v2.4.2` ۲۵۶ · `arena-release-admin-v2.1` ۲۵۰ · `install-hm-bckt-key` ۲۴۴ · `arena-release-grade9-v2.2` ۲۳۹ · `arena-release-grade9-v2.3` ۲۳۴ · `arena-release-grade9-v2.4.3` ۲۳۰ · `arena-release-grade9-v2.4.1` ۲۲۳ · `arena-music-html-sync` ۲۰۹ · `arena-release-admin-v2.0` ۲۰۸ · `arena-release-grade9-v2.4.4` ۲۰۲ · `arena-upload-tools-parspack` ۱۸۵ · `arena-parspack-html-zip` ۱۸۰ · `audit-hm-bckt` ۱۷۹ · `provision-grade9-v2` ۱۷۲ · `arena-compile-check` ۱۶۷ · `arena-parspack-probe` ۱۱۶ · `arena-apk-urls` ۷۱ · `arena-encode-html-bucket` ۶۴ · `arena-package-0000-hmk1` ۶۲ · `arena-encode-toolbox-hmk1` ۵۱ · `arena-parspack-html-decode` ۴۴ · `arena-check-music-url` ۳۹ · `arena-music-auto-theme-encode` ۳۶ · `relay-release-to-arena` ۳۳

**حذف قطعی** (هیچ ران موفقی نداشته‌اند): `arena-parspack-preflight`, `deploy-prompt-02/03/04`, `deploy-teach-stats`, `finalize-grade9-v2`, `publish-update`, `release-grade9-v2`, `setup-student-profiles`, `setup-study-tutor-ai`, `ship-ci-apk` `upload-lesson-audio`, `upload-lesson-pdfs`, `upload-prompt-01-media`, `drive-resumable-once`.

در فایل‌های کپی‌شده، `branches: [arena/…]` را به `branches: [main]` تغییر بده تا در مخزن تازه معنا داشته باشند (شرط‌های پیام کامیت مثل `[compile]` و `Release grade 9 v…` دست‌نخورده بماند).

## ۳) مستندات — بازنویسی کامل و به‌روز

سه فایل باید نوشته شوند (به فارسی، کامل، بدون اطلاعات کهنه):

- **`README.md`**: معرفی، ساختار پوشه‌ها، `applicationId` نصبی `com.hamyareman.p09` و namespace `com.hamyareman.ir`، نسخهٔ فعلی **۲٫۴٫۴ / کد ۲۴۴**، سرور محتوا `https://c539776.parspack.net` و رمزنگاری HMK1، جدول سه قاب موسیقی (tile = «بشنو و بخواب»، full = «نجواهای آرام‌بخش طبیعت»، mini = فعلاً بدون مصرف)، دستورهای بیلد (`:hamyar-app:compileP09DebugKotlin`، `assembleP09Debug`)، فهرست ورکفلوهای نگه‌داشته‌شده و **فهرست Secretهای لازم**: `APPWRITE_API_KEY`, `APPWRITE_PROJECT_ID`, `APPWRITE_DATABASE_ID`, `HTML_MEDIA_KEY_B64`, `PARSPACK_ACCESS_KEY`, `PARSPACK_SECRET_KEY`, `RELEASES_TOKEN` + متغیر `RELEASES_REPO`.
- **`RELEASES.md`**: قرارداد واقعی امروز — شرط‌های نصب روی نسخهٔ قبلی (پکیج یکی، امضا `5f091b6bf47d7294b37f8994099a6c88ac2e25182dd6cbe6a36da6b3bf804959`، versionCode بزرگ‌تر)، ردیف `app_state/app_release_grade9` و معنی `latest/min/rollout/notes` (**`min = latest` یعنی اجباری**)، ترتیب «اول فایل روی هر دو مقصد، بعد اعلام»، مسیر عملی ساخت یک انتشار تازه از روی `arena-release-grade9-v2.4.4.yml`، و جدول نسخه‌های ۲٫۴٫۱ تا ۲٫۴٫۴.
- **`گزارش-تغییرات-همیار.md`**: تاریخچهٔ موجود + بخش‌های کامل ۲٫۴٫۲، ۲٫۴٫۳ و ۲٫۴٫۴ (رفع قطع صدا با قفل صفحه، پنجرهٔ تمام‌صفحهٔ موسیقی و دکمهٔ برگشت، اثرگذاری دکمه‌های پلی/بستن، قانون تم روشن/تاریک، نمایش کامل سه شمارهٔ نسخه).

یک `MIRROR.md` هم بساز که بگوید چه چیزی کپی شد و چه چیزی عمداً کپی نشد و چرا.

## ۴) تاریخچه — «کامیت‌های موفق»

تاریخچهٔ کامل گیت ≈ ۹۵۰ مگابایت است (پر از APK و فایل‌های دورریخته) و نباید منتقل شود.
یک **کامیت تمیز و واحد** روی `main` مقصد بساز با پیام:
`آینهٔ همیار من پایهٔ نهم — نسخهٔ ۲٫۴٫۴ (کد ۲۴۴)` و در بدنهٔ کامیت فهرست کوتاه نسخه‌های موفق (۲٫۴٫۱ → ۲٫۴٫۴) را بیاور.
اگر کاربر صراحتاً تاریخچه خواست، فقط کامیت‌های انتشارهای موفق را با `git cherry-pick` روی یک شاخهٔ نو بگذار — نه کل تاریخچه.

## ۵) روش اجرا

```
git -C <مبدأ> archive HEAD | tar -x -C /tmp/mirror      # فقط فایل‌های ردیابی‌شده
# حذف پوشه‌ها و فایل‌های بند ۱، فیلتر ورکفلوها طبق بند ۲، نوشتن مستندات بند ۳
cd /tmp/mirror && git init -b main . && git add -A && git commit -m "…" 
git remote add origin https://github.com/samanta-nz/com.hamyareman.g09.git && git push -u origin main
```
ابزار آمادهٔ نشست قبل در مبدأ هست و می‌توانی از آن شروع کنی:
`scripts/mirror-g09/build-and-push.sh`، `scripts/mirror-g09/workflows-keep.txt`، `scripts/mirror-g09/docs/`.

## ۶) بعد از push

- تأیید کن: `gh api /repos/samanta-nz/com.hamyareman.g09 -q .size` و تعداد فایل‌ها.
- به کاربر گزارش بده: چه کپی شد، چه حذف شد و چرا، حجم نهایی، و اینکه فقط `android.yml` با push به `main` اجرا می‌شود (بقیه به پیام کامیت وابسته‌اند).
- Secretها را خود کاربر اضافه می‌کند؛ از او نخواه که توکن در چت بفرستد.

## ۷) هشدارهای محیط

- شاخهٔ مبدأ چند بار «عقب‌گرد» کرده است؛ قبل از هر کار `git log --oneline -3` بگیر و اگر تاریخچه کوتاه شده بود، ref را با
  `gh api -X PATCH /repos/samanta-nz/com.hamyareman.ir/git/refs/heads/arena/01a0fe97-com-hamyareman-ir -f sha=<آخرین SHA درست> -F force=true` برگردان.
- فقط روی شاخهٔ `arena/01a0fe97-com-hamyareman-ir` در مبدأ کار کن.
- `gh workflow run` برای این توکن ۴۰۳ است؛ CI فقط با پیام کامیت تریگر می‌شود.
