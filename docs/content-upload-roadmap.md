# نقشهٔ مسیرهای محتوای همیار — نسخهٔ هدف 3.0.4

## دامنه و محدودیت

این خروجی از منابع واقعی اپ ساخته شده است: `server-map.json`، `catalog.json`، `app-content.tsv`، `books-menu.json` و `Read-Only-Books-Data/app-Parspack-upload-map.csv`. هر ردیف یک `object_key` یکتا است و منابع مربوطه در ستون جداگانه آمده‌اند.

**این خروجی فهرست زندهٔ ParsPack را نمی‌خواند.** ستون `live_bucket_status` عمداً «بررسی زنده انجام نشده» است؛ URL مورد انتظار با وجود واقعی فایل یکسان نیست.

## آمار

- CSV بیرونی: 237 ردیف؛ همه با status برابر `active`.
- ورودی‌های `server-map.json` قبل از افزوده‌شدن نگاشت رسانه: 399.
- نگاشت‌های MP3/MP4 افزوده‌شده: 504؛ `server-map.json` اکنون 903 ورودی دارد.
- `catalog.json`: 100 ورودی؛ `app-content.tsv`: 120 ردیف.
- مسیرهای یکتای نقشه: 1001.
- مسیرهای تعریف‌شده در منابع اپ: 1001.
- CSV-only در منابع بررسی‌شدهٔ اپ: 0.
- شناسه‌های slug/فایل که به بیش از یک مسیر اشاره دارند: 34; ردیف‌های roadmap دارای تعارض: 67.

## نمونه تعارض شناسه

- `g9-english-p01.pdf`: `Bucket/Pdf-files/G09/g9-english-workbook/g9-english-p01.pdf`؛ `Bucket/Pdf-files/G09/g9-english/g9-english-p01.pdf`
- `g9-english-p02.pdf`: `Bucket/Pdf-files/G09/g9-english-workbook/g9-english-p02.pdf`؛ `Bucket/Pdf-files/G09/g9-english/g9-english-p02.pdf`
- `g9-english-p03.pdf`: `Bucket/Pdf-files/G09/g9-english-workbook/g9-english-p03.pdf`؛ `Bucket/Pdf-files/G09/g9-english/g9-english-p03.pdf`
- `g9-english-p04.pdf`: `Bucket/Pdf-files/G09/g9-english-workbook/g9-english-p04.pdf`؛ `Bucket/Pdf-files/G09/g9-english/g9-english-p04.pdf`
- `g9-english-p05.pdf`: `Bucket/Pdf-files/G09/g9-english-workbook/g9-english-p05.pdf`؛ `Bucket/Pdf-files/G09/g9-english/g9-english-p05.pdf`
- `g9-english-p06.pdf`: `Bucket/Pdf-files/G09/g9-english-workbook/g9-english-p06.pdf`؛ `Bucket/Pdf-files/G09/g9-english/g9-english-p06.pdf`
- `g9-hedy-p04.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p04.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p04.pdf`
- `g9-hedy-p05.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p05.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p05.pdf`
- `g9-hedy-p06.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p06.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p06.pdf`
- `g9-hedy-p07.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p07.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p07.pdf`
- `g9-hedy-p08.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p08.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p08.pdf`
- `g9-hedy-p09.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p09.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p09.pdf`
- `g9-hedy-p10.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p10.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p10.pdf`
- `g9-hedy-p11.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p11.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p11.pdf`
- `g9-hedy-p12.pdf`: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-p12.pdf`؛ `Bucket/Pdf-files/G09/g9-payam/g9-hedy-p12.pdf`

تعارض‌های بالا خودکار یکی یا حذف نشده‌اند؛ ممکن است به دو فایل واقعی در دو مسیر کتاب اشاره کنند. داشبورد ستون و فیلتر جداگانه برای مشاهدهٔ این ردیف‌ها دارد.

## مسیرهای رسانه

- MP3: `Bucket/Pdf-files/G09/<folder کتاب>/<شناسهٔ صوت>.mp3`.
- MP4: `Bucket/Pdf-files/G09/<folder کتاب>/<packId با خط تیره>-V01.mp4`.
- صوت ریاضی نهم با قراردادهای legacy در `g9-math/exam/` (مثل `ryazifNNdNN.mp3` و `ryazifNNreview.mp3`) تعریف شده است.
- مسیرها از روی شناسه‌های مطالعه و قرارداد کد موجود در اپ تعریف شده‌اند؛ وجود همهٔ objectها روی باکت زنده هنوز تست نشده است.

## کش و تشخیص تغییر

- **HTML رمز‌شدهٔ HMK1:** `LessonCache` در `main` از ETag، طول، Last-Modified و در نبود validator از Range ابتدای/انتهای فایل استفاده می‌کند؛ فاصلهٔ بررسی حدود ۳۰ ثانیه است.
- **PDF:** در شاخهٔ کاری، `StudyPdfCache` هنگام بازکردن کش متادیتا/Range را بررسی می‌کند، PDF را اعتبارسنجی می‌کند و در خطای شبکه نسخهٔ سالم قبلی را نگه می‌دارد.
- **MP3/MP4 دانلودشده:** `MediaVault/HMV1` کش محلی دارد؛ در شاخهٔ کاری تازگی کش دانلودشده با متادیتای HTTP بررسی می‌شود. پخش مستقیم استریم مسیر جداگانه‌ای است.
- **تصویر/فونت/CSS/JavaScript:** از مسیر WebView/شبکه‌اند؛ در کد بررسی‌شده کش مستقل دارای metadata تازگی برای آن‌ها تأیید نشده است.
- مقایسهٔ ابتدا و انتهای فایل، وقتی ETag/Last-Modified وجود ندارد، تضمین بررسی تمام بایت‌ها نیست؛ CDN و فایل هم‌طول باید با آزمایش واقعی بررسی شوند.
- تغییرات کد در شاخهٔ کاری هنوز build/test نشده‌اند.

## وضعیت انتشار 3.0.4

در اجرای [GitHub Actions 38018067039](https://github.com/samanta-nz/com.hamyareman.ir/actions/runs/38018067039)، build/test APK، GitHub Release و آپلود ParsPack موفق بودند؛ اما به‌روزرسانی وضعیت نسخه در Appwrite با `HTTP 403 Forbidden` شکست خورد. بنابراین آن اجرا انتشار کاملاً موفق نیست.

## فایل‌ها

- `content-upload-roadmap.html`: داشبورد فیلترپذیر فارسی با گزینهٔ تعارض شناسه؛ CSV صادراتی فقط نتایج فیلترشده و همهٔ ستون‌ها را شامل می‌شود.
- `content-upload-roadmap.csv`: نقشهٔ کامل با یک ردیف برای هر مسیر یکتا.
