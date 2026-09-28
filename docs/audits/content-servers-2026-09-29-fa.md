# گزارش ممیزی فقط‌خواندنی سرورهای محتوای «همیار من»

**تاریخ ممیزی:** ۱۴۰۵/۰۷/۰۷ (۲۰۲۶-۰۹-۲۹، تهران)

**محیط‌های بررسی‌شده:** Appwrite فرانکفورت و Arvan Object Storage تهران

**حالت اجرا:** فقط‌خواندنی؛ هیچ فایل، ACL، policy، تنظیم bucket یا ردیف پایگاه‌داده ساخته، ویرایش یا حذف نشد.

**دادهٔ خامِ پالایش‌شده:** `docs/audits/content-servers-2026-09-29.json`

> credentialها و کلید ۳۲‌بایتی HTML فقط در حافظهٔ اجرای موقت استفاده شدند. هیچ secret، کلید رمزگشایی یا متن کامل HTML در خروجی ذخیره نشده است. شناسه‌های کاربری و شناسهٔ مالک bucket نیز در JSON نهایی حذف شده‌اند.

---

## ۱. جمع‌بندی مدیریتی

### حکم نهایی

وضعیت **محتوای واقعی** بهتر از وضعیت **تنظیمات و مسیر انتخاب سرور** است:

- هر **۱۳۳/۱۳۳** قلم catalog روی هر دو bucket وجود دارد و اندازهٔ متناظر آن‌ها برابر است.
- هر **۸۰/۸۰** HTML در هر دو bucket با موفقیت باز و با AES-GCM احراز اصالت شد؛ متن سادهٔ دو نسخه برای همهٔ ۸۰ فایل یکسان است.
- بااین‌حال اپ فقط می‌تواند **۷۲/۸۰** HTML آروان را به‌صورت عمومی دریافت کند. هشت ابزار/آزمایشگاه در آروان وجود دارند و سالم‌اند، اما public GET آن‌ها `403` می‌دهد.
- گزینهٔ «تنظیمات سرور» فقط روی بخشی از محتوا اثر واقعی دارد؛ صوت‌ها، چند PDF، HTMLهای تدریس ریاضی و چند مسیر دیگر عملاً همیشه Appwrite هستند.
- حالت «سریع‌ترین» سرعت دو سرور را مقایسه نمی‌کند؛ فقط قابل‌دسترسی‌بودن یک فایل نمونه روی آروان را با HEAD و timeout برابر ۱٫۵ ثانیه می‌سنجد.
- **بحرانی‌ترین یافته:** bucket Appwrite در سطح bucket دارای `create/read/update/delete("any")` و `fileSecurity=false` است. این تنظیم از دید مدل دسترسی Appwrite، bucket را برای عملیات نوشتن/ویرایش/حذف بیش از حد باز می‌کند و permissionهای اختصاصی چهار تصویر کاربر را نیز بی‌اثر می‌سازد. به‌دلیل قید فقط‌خواندنی، exploit نوشتن یا حذف آزمایش نشد.

### امتیاز هر حوزه

| حوزه | نتیجه | توضیح کوتاه |
|---|---|---|
| تطابق محتوای دو bucket | **خوب** | ۱۳۳/۱۳۳ موجود و هم‌اندازه؛ plaintext هر ۸۰ HTML یکسان |
| دسترسی واقعی اپ به آروان | **ناقص** | ۸ ابزار/آزمایشگاه در آروان private و برای اپ `403` |
| امنیت Appwrite bucket | **بحرانی** | `create/update/delete(any)` و `fileSecurity=false` |
| رمزگذاری HTML | **خوب، با چند ضعف پیرامونی** | HMK1/AES-256-GCM درست؛ اما fallback متن خام و cache سادهٔ ابزارها وجود دارد |
| انتخاب سرور | **نیمه‌کاره/گمراه‌کننده** | پوشش ناقص و FASTEST بدون سنجش سرعت |
| loading | **متوسط** | spinner و fallback وجود دارد؛ درصد/Retry/تفکیک خطا در HTML عمومی نیست |
| cache و freshness | **متوسط رو به ضعیف** | MediaVault خوب؛ cache ابزار و HTML تدریس می‌تواند نامحدود کهنه بماند |
| مدیریت دانلود | **عمدتاً خوب** | resume، Range، رمز محلی و کنترل سلامت؛ چند مسیر PDF ناسازگار |
| جایگاه UI | **عمدتاً درست** | دانلودها در «مدرسه» مناسب؛ تنظیم منبع محتوا با sync backend در یک صفحه مخلوط شده است |

---

## ۲. دامنه و روش ممیزی

ممیزی شامل این موارد بود:

1. دریافت احرازشدهٔ تنظیمات Appwrite bucket و تنظیمات قابل‌پشتیبانی Arvan S3.
2. inventory کامل هر دو bucket، بدون عملیات نوشتنی.
3. تطبیق همهٔ ۱۳۳ ورودی `catalog.json` با:
   - شناسهٔ Appwrite (`aw`)
   - کلید سلسله‌مراتبی آروان (`key`)
4. دریافت همهٔ ۸۰ HTML از مسیر عمومی‌ای که اپ استفاده می‌کند.
5. برای هشت شیء private آروان، دریافت فقط‌خواندنی احرازشده جهت تشخیص تفاوت «نبود فایل» با «ACL نادرست».
6. بازکردن envelope به‌شکل `HMK1 + IV 12-byte + AES-256-GCM ciphertext/tag`، UTF-8 strict، تشخیص ساختار HTML، استخراج referenceها و مقایسهٔ hash/plaintext.
7. ممیزی ایستای مسیرهای Kotlin مربوط به انتخاب سرور، loading، cache، دانلود، freshness، WebView و جایگاه منوها.
8. بررسی اسکریپت‌های mirror/upload برای تطبیق رفتار عملیاتی با کلیدهایی که اپ مصرف می‌کند.

### محدودیت

- ممیزی تعاملی روی یک گوشی واقعی و کلیک بصری تک‌تک کنترل‌های داخل ۸۰ WebView انجام نشد؛ اعتبار ساختاری، رمزگشایی، referenceها و مسیرهای کد بررسی شد.
- اجرای shell محلی این sandbox برای endpointها مشکل TLS داشت؛ ممیزی شبکه روی runner موقت با credential مهروموم‌شده انجام شد. این محدودیت روی نتیجهٔ نهایی اثر نگذاشت.

---

## ۳. نتیجهٔ زندهٔ bucket خارجی Appwrite

### inventory

| شاخص | مقدار |
|---|---:|
| تعداد فایل | ۲۶۴ |
| حجم اصلی کل | ۴۴۰٬۱۸۹٬۹۲۲ بایت (~۴۱۹٫۸ MiB) |
| `application/octet-stream` | ۱۶۸ |
| PDF | ۲۸ |
| JPEG | ۶۰ |
| MP3 | ۸ |
| ورودی catalog موجود | ۱۳۳/۱۳۳ |
| HTML catalog قابل GET عمومی | ۸۰/۸۰ |

### تنظیمات واقعی bucket

- نام: `wellness-media`
- فعال: بله
- سقف فایل: `50,000,000` بایت
- compression: `none`
- encryption: `true`
- antivirus: `true`
- `fileSecurity=false`
- permissionهای bucket:
  - `create("any")`
  - `read("any")`
  - `update("any")`
  - `delete("any")`

### موارد درست

- دسترسی عمومی خواندن برای محتوای عمومی اپ کار می‌کند.
- Range روی فایل نمونه صحیح بود: درخواست `bytes=0-1023` پاسخ `206` با `Content-Range` کامل داد؛ این با دانلود موازی/ادامه‌پذیر MediaVault سازگار است.
- ۲۵۶ فایل metadata برابر `encryption=true` دارند.
- همهٔ ۸۰ HTML catalog با HMK1 شروع می‌شوند و authenticated decryption آن‌ها موفق است.

### اشکال بحرانی امنیتی

`fileSecurity=false` یعنی کنترل در سطح فایل اعمال نمی‌شود و permissionهای bucket حاکم‌اند. هم‌زمان، چهار عمل create/read/update/delete برای `any` مجاز شده‌اند. پیامدها:

1. bucket عمومی محتوا فقط read-only نیست؛ تنظیم آن ظرفیت نوشتن/ویرایش/حذف ناشناس یا عمومی را ایجاد می‌کند.
2. چهار JPEG با permission کاربر خاص در inventory وجود دارند، اما metadata همان فایل‌ها بدون session قابل خواندن بود؛ چون file security خاموش و bucket دارای `read(any)` است، private بودن مورد انتظار آن‌ها قابل اتکا نیست.
3. شناسه و metadata فایل‌ها با anonymous list قابل enumeration است (`total=264`).

**اقدام فوری P0:**

- `update` و `delete` فقط برای service/team ادمین باشند.
- `create` حداقل به `users` یا role محدود اختصاصی کاهش یابد؛ برای محتوای release بهتر است فقط service account حق create داشته باشد.
- فایل‌های avatar/receipt/user upload به bucket جدا با `fileSecurity=true` منتقل شوند.
- bucket عمومی محتوا فقط `read(any)` داشته باشد.

### نکات تنظیمی دیگر

- هر هشت MP3 metadata برابر `encryption=false` دارند، هرچند encryption در سطح bucket روشن است. این هشت فایل مجموعاً فایل‌های صوت تدریس ریاضی هستند. انتقال با HTTPS و رمزگذاری محلی MediaVault برقرار است، اما at-rest encryption گزارش‌شدهٔ Appwrite برای این هشت فایل برقرار نیست.
- مقدار `totalSize` در پاسخ تنظیمات bucket صفر بود، درحالی‌که inventory واقعی ~۴۱۹٫۸ MiB است؛ برای ظرفیت‌سنجی نباید به این فیلد تکیه کرد.
- ۱۳۱ فایل خارج از catalog وجود دارد. همهٔ آن‌ها الزاماً orphan نیستند (صوت، HTML تدریس، avatar و سه PDF فصل ۸ در میان آن‌هاست)، اما نام‌های legacy مانند `Amzshghh-*`, `yg-*`, `br-*`, `ex-*` باید با usage واقعی تطبیق و سپس پاکسازی شوند.

---

## ۴. نتیجهٔ زندهٔ bucket ایرانی آروان

### inventory

| شاخص | مقدار |
|---|---:|
| تعداد object | ۱۵۰ |
| حجم کل | ۲۵۶٬۸۲۸٬۸۰۶ بایت (~۲۴۴٫۹ MiB) |
| ورودی catalog موجود | ۱۳۳/۱۳۳ |
| ورودی catalog هم‌اندازه با Appwrite | ۱۳۳/۱۳۳ |
| HTML قابل GET عمومی توسط اپ | ۷۲/۸۰ |
| HTML قابل خواندن احرازشده در audit | ۸۰/۸۰ |
| anonymous bucket listing | بسته (`403`) |

۱۷ object خارج از catalog وجود دارد که ۱۵ مورد folder marker صفر‌بایتی و دو مورد APK هم‌اندازه در دو مسیر با حجم حدود ۶۷٫۳ MB برای هر کدام‌اند؛ hash این دو APK در این ممیزی مقایسه نشد.

### تنظیمات واقعی

- ACL bucket: فقط مالک `FULL_CONTROL` دارد؛ خوب است.
- bucket policy عمومی نیست (`IsPublic=false`).
- anonymous LIST بسته است؛ خوب است.
- server-side encryption configuration وجود ندارد.
- versioning فعال نیست.
- lifecycle وجود ندارد.
- Public Access Block configuration وجود ندارد.
- website hosting فعال نیست.
- CORS:
  - Origin: `*.arvancloud.ir`
  - Headers: `*`
  - Methods: `GET, PUT, DELETE, HEAD, POST`
  - Max age: ۵۰۰۰ ثانیه

### موارد درست

- فهرست bucket برای ناشناس بسته است، درحالی‌که objectهای عمومی لازم با URL مستقیم قابل دریافت‌اند؛ این تفکیک مناسب است.
- همهٔ ۱۳۳ کلید سلسله‌مراتبی catalog واقعاً موجودند.
- اندازهٔ هر ۱۳۳ object با Appwrite متناظر برابر است.
- همهٔ ۸۰ HTML با credential audit معتبر، HMK1 و قابل decrypt هستند.
- plaintext هر ۸۰ HTML آروان دقیقاً با Appwrite متناظر برابر است.

### اشکال دسترسی هشت HTML

هشت object زیر وجود دارند و محتوایشان سالم و همسان Appwrite است، اما public GET اپ برای آن‌ها `403` می‌دهد:

- `lab-09-biology.html`
- `lab-09-chemistry.html`
- `lab-09-physics.html`
- `tool-calendar.html`
- `tool-casio991.html`
- `tool-converter.html`
- `tool-dj120d.html`
- `tool-ti-nspire.html`

۷ مورد از این هشت فایل ciphertext متفاوت ولی plaintext یکسان دارند؛ این موضوع خطا نیست و فقط نشان می‌دهد با IV تازه دوباره AES-GCM شده‌اند. `tool-calendar.html` حتی ciphertext یکسان دارد، ولی ACL آن همچنان عمومی نیست.

**اقدام P1:** public-read policy/ACL دقیق برای همین prefixهای محتوای عمومی اصلاح شود؛ نه public LIST و نه write عمومی لازم نیست.

### ضعف‌های تنظیمی

- نبود versioning در bucketی که mirror و APK دارد، بازیابی overwrite/delete اشتباه را دشوار می‌کند.
- نبود SSE برای PDF/JPG/APK و دیگر فایل‌های غیر-HMK1 یک ضعف دفاع در عمق است.
- CORS شامل PUT/POST/DELETE است، درحالی‌که اپ native به CORS نیاز ندارد و مصرف عمومی محتوا فقط GET/HEAD می‌خواهد. روش‌ها و originها باید حداقلی شوند.
- نبود Public Access Block باعث می‌شود ACL اشتباه بتواند ناخواسته objectی را عمومی کند. اگر سرویس آروان پشتیبانی می‌کند، دسترسی عمومی بهتر است با policy محدود به prefixهای محتوایی مدیریت شود.

---

## ۵. صحت ۸۰ HTML و لینک‌دهی آن‌ها

### نتیجهٔ رمزگشایی و تطبیق

| آزمون | نتیجه |
|---|---:|
| دانلود عمومی از Appwrite | ۸۰/۸۰ |
| دانلود عمومی از آروان | ۷۲/۸۰ |
| دریافت احرازشده از آروان | ۸۰/۸۰ |
| AES-GCM authenticated decrypt در Appwrite | ۸۰/۸۰ |
| AES-GCM authenticated decrypt در آروان | ۸۰/۸۰ |
| UTF-8 strict + ساختار HTML | ۸۰/۸۰ |
| plaintext یکسان بین دو سرور | ۸۰/۸۰ |
| ciphertext یکسان | ۷۳/۸۰ |

بنابراین مشکل هشت فایل آروان **محتوا یا رمز نیست**؛ فقط public access است.

### referenceهای مشکل‌دار

تنها سه صفحه reference نسبی به HTML دیگر دارند:

- `amz-01` → `02-handwriting.html`
- `amz-02` → `01-speed-reading.html`, `03-touch-typing.html`
- `amz-03` → `02-handwriting.html`, `04-note-taking.html`

اما `ContentHtmlScreen` HTML را با base برابر `https://local.hamyar/` لود می‌کند و navigation این نام‌ها را به itemهای catalog ترجمه نمی‌کند (`ContentScreens.kt:171-185`). بنابراین کلیک این لینک‌ها به مسیر catalog بعدی نمی‌رود و به URL ساختگی local.hamyar resolve می‌شود. این لینک‌ها باید یا:

- در HTML به scheme داخلی مثل `hamyar://content/amz-02` تبدیل شوند و در WebViewClient intercept شوند؛ یا
- قبل از load، hrefها به routeهای واقعی نگاشت شوند.

همچنین `tool-calendar.html` یک وابستگی به `cdn.jsdelivr.net` دارد. پس cache محلی HTML ابزار به‌تنهایی تضمین offline کامل تقویم نیست و اختلال/فیلتر CDN می‌تواند بخشی از صفحه را خراب کند.

---

## ۶. اثر واقعی «تنظیمات سرور»

### رفتار Resolver

`ServerResolver.pick` از نظر پایه درست بین Appwrite و آروان انتخاب می‌کند، ولی فقط وقتی `arvanKey` موجود باشد (`ServerResolver.kt:67-76`). کاتالوگ در startup بارگذاری می‌شود، پس mapping در استفادهٔ عادی موجود است (`HamyarApplication.kt:26-28`).

اما «سریع‌ترین» فقط یک HEAD روی نخستین HTML آروان با timeout برابر ۱٫۵ ثانیه است (`ServerResolver.kt:34-54`). latency Appwrite اندازه‌گیری یا با آروان مقایسه نمی‌شود. در صورت پاسخ آروان، همهٔ فایل‌های mapped به آروان می‌روند؛ حتی اگر Appwrite سریع‌تر باشد.

### ماتریس واقعی پوشش

| نوع محتوا | Appwrite | آروان در اپ | اثر انتخاب کاربر |
|---|---|---|---|
| ۷۲ HTML محتوای عمومی | سالم | سالم و عمومی | انتخاب اثر دارد |
| ۸ ابزار/آزمایشگاه | سالم | object موجود ولی GET عمومی `403` | INTERNAL خراب؛ FASTEST به Appwrite fallback می‌کند |
| ۲۵ PDF داخل catalog | سالم | سالم و عمومی | انتخاب اثر دارد |
| سه PDF فصل ۸ | سالم | mapping catalog ندارد | همیشه Appwrite |
| هشت MP3 تدریس | سالم | mapping ندارد | همیشه Appwrite |
| HTMLهای تدریس ریاضی `ryazif*` | سالم | mapping ندارد | همیشه Appwrite |
| فایل‌های بدون کلید مانند video/avatar | بسته به فایل | mapping ندارد | همیشه Appwrite |

### ناسازگاری fallbackها

- `ContentHtmlScreen` همیشه candidate انتخابی، سپس Appwrite، سپس آروان را امتحان می‌کند (`ContentScreens.kt:132-157`). بنابراین حتی در حالت **INTERNAL** اگر آروان خراب باشد، ممکن است بی‌صدا Appwrite باز شود. متن UI که می‌گوید «اگر در دسترس نباشد فایل باز نمی‌شود» برای این مسیر درست نیست.
- `ToolRemote` فقط در حالت **FASTEST** fallback به Appwrite دارد (`ToolRemote.kt:90-102`). بنابراین INTERNAL برای هر هشت ابزار/آزمایشگاه فعلاً خطا می‌دهد.
- `StudyMedia.existsOnServer` و resolve نام‌های جایگزین همیشه Appwrite را HEAD می‌کنند (`StudyMedia.kt:66-90`).

### نتیجه

گزینهٔ سرور **تزئینی نیست**، اما سراسری و یکدست هم نیست. عبارت UI «کتاب‌ها، صداها و صفحه‌ها از کدام سرور بیایند؟» (`ServerOptionsSection.kt:52-71`) بیش از پوشش واقعی وعده می‌دهد؛ مخصوصاً «صداها» اکنون همیشه خارجی‌اند.

**پیشنهاد:** یا همهٔ رسانه‌ها mapping دوگانه بگیرند، یا متن UI دقیقاً بگوید «برای محتوای دارای نسخهٔ ایرانی». برای FASTEST نیز یا latency واقعی هر دو origin سنجیده شود، یا نام آن به «خودکار: ایرانی در صورت دسترسی» تغییر کند.

---

## ۷. کدینگ، دیکدینگ و امنیت رمز

### موارد درست

- فرمت HMK1 صحیح و ساده است: magic چهار‌بایتی، IV دوازده‌بایتی و AES-GCM tag 128-bit (`HtmlCodec.kt:8-29`).
- تمام ۸۰ HTML عملاً authenticated decrypt شدند؛ پس کلید، IV/tag و encoding فعلی درست‌اند.
- کلید HTML داخل APK نیست؛ از ردیف خصوصی گرفته و با Android Keystore پوشانده می‌شود (`HtmlMediaKey.kt:23-54`, `60-91`).
- MediaVault رسانهٔ دانلودی را با AES-CTR و IV تازه روی دیسک نگه می‌دارد و ابتدا/انتهای فایل را کنترل می‌کند (`MediaVault.kt:490-510`).
- کلید HTML shared فقط برای HTML استفاده شده و secret در گزارش/ریپو افشا نشده است.

### ضعف‌ها

1. `HtmlCodec.unwrap` اگر magic HMK1 نباشد داده را متن خام می‌پذیرد (`HtmlCodec.kt:22-24`). برای محتوایی که باید همیشه رمز باشد، این رفتار fail-open است. بهتر است call site جدید حالت `requireWrapped=true` داشته باشد.
2. کلید HTML مشترک با `read("users")` به هر کاربر واردشده داده می‌شود. این رمزگذاری مانع مشاهدهٔ ناشناس است، نه DRM در برابر کاربر مجاز؛ این محدودیت طراحی باید مستند باشد.
3. `MediaVault` اگر wrap با Android Keystore شکست بخورد، کلید داده را Base64 خام در SharedPreferences ذخیره می‌کند (`MediaVault.kt:138-143`). این downgrade خاموش بهتر است حذف یا با خطای صریح جایگزین شود.
4. Appwrite خود bucket را encrypted اعلام می‌کند، اما metadata هشت MP3 `encryption=false` است.
5. آروان SSE ندارد؛ HTMLها با HMK1 محافظت می‌شوند ولی PDF/JPG/APK نه.

---

## ۸. loading

### درست

- Content HTML spinner دارد و دریافت شبکه روی `Dispatchers.IO` انجام می‌شود.
- timeout اتصال/خواندن و fallback چند origin وجود دارد (`ContentScreens.kt:132-159`).
- ابزارها ابتدا cache را بررسی و فایل موقت `.part` استفاده می‌کنند (`ToolRemote.kt:90-127`).
- دانلود رسانه progress throttling دارد و UI را با هر packet دوباره compose نمی‌کند (`MediaVault.kt:223-243`).
- PDF/صوت در صفحهٔ مدیریت درصد و وضعیت خطای شبکه/404 را جدا می‌کنند (`DownloadsScreen.kt:211-236`).

### اشتباه/کمبود

- Content HTML فقط spinner نامعین دارد؛ درصد، نام سرور جاری، Retry و تفکیک «کلید موجود نیست / 403 / decrypt fail / timeout» نشان داده نمی‌شود. همه در پیام عمومی «اتصال اینترنت را بررسی کن» ادغام می‌شوند.
- فایل HTML تا انتها در ByteArray و سپس String نگه داشته می‌شود. بزرگ‌ترین plaintext بررسی‌شده حدود ۱۳٫۹ MB بود؛ هم‌زمانی ciphertext، plaintext و String می‌تواند memory spike بسازد.
- ابزارها در خطا نیز فقط «برای نمایش این صفحه به اینترنت نیاز است» می‌گویند، حتی وقتی اینترنت هست ولی ACL آروان `403` است.
- تغییر source در Settings روی صفحه‌ای که cache موجود دارد به کاربر feedback فوری نمی‌دهد.

---

## ۹. cache و freshness

### MediaVault — خوب

- فایل ناقص فقط بعد از verify قابل استفاده است (`MediaVault.kt:57-73`).
- دانلود Range موازی، fallback تک‌رشته‌ای و ادامه پس از تغییر شبکه وجود دارد.
- فایل محلی رمز است و پخش از stream رمزگشایی‌شونده انجام می‌شود.
- tail با سرور مقایسه می‌شود و نمونهٔ زندهٔ هر دو origin Range 206 را پشتیبانی کرد.

### cache ابزار — مشکل‌دار

`ToolRemote` plaintext HTML را در `filesDir/hamyar-tools` می‌نویسد و معتبر بودن را فقط با `length > 64` می‌سنجد (`ToolRemote.kt:50-64`). TTL، signature، ETag، version یا hash وجود ندارد. نتیجه:

- ابزار می‌تواند برای همیشه stale بماند.
- پس از cache شدن، تغییر گزینهٔ سرور هیچ اثری بر آن ابزار ندارد.
- محتوای رمزگشایی‌شده روی دیسک خصوصی اپ ذخیره می‌شود؛ برخلاف MediaVault رمز محلی ندارد.

### HTML محتوای عمومی

`ContentHtmlScreen` cache دیسکی ندارد؛ هر بار بازشدن دوباره دانلود و decrypt می‌کند. این freshness خوب ولی latency و مصرف داده نامناسب است. cache رمز‌شده همراه signature/TTL تعادل بهتری می‌دهد.

### freshness دانلودها — باگ حالت داخلی

`MediaFreshness.metaUrl` URL انتخاب‌شده با `StudyMedia.viewUrl` را می‌گیرد و فقط `/view` را حذف می‌کند (`MediaFreshness.kt:63-88`). اگر source آروان باشد، URL یک object S3 است و metadata JSON Appwrite برنمی‌گرداند؛ parser شکست می‌خورد. بنابراین freshness برای PDFهای mapped در INTERNAL/FASTEST-Internal خراب است. metadata freshness باید **همیشه** از `StudyMedia.externalUrl(fileId)` گرفته شود، حتی اگر payload از آروان دانلود شده باشد.

HTML تدریس ریاضی نیز مستقیماً در MediaVault cache می‌شود ولی در `MediaFreshness.rememberDownload` ثبت نمی‌شود (`MathLessonScreen.kt:394-409`)، پس نسخهٔ verified می‌تواند بدون expiry کهنه بماند.

---

## ۱۰. مدیریت دانلود

### موارد درست

- جایگاه «مدرسه → دانلودها → مدیریت دانلود کتاب‌ها» متناسب با مدل ذهنی کاربر است (`HubCatalog.kt:49-56`).
- PDF و صوت تفکیک شده‌اند؛ دانلود تکی و گروهی، حجم، درصد، حذف و 404 وجود دارد.
- `DownloadsScreen.downloadPdfBlocking` از `.part`، Range resume، تشخیص `%PDF-` و rename اتمیک استفاده می‌کند (`DownloadsScreen.kt:129-181`).
- صوت‌ها در MediaVault رمز و verify می‌شوند.
- Appwrite و آروان هر دو Range استاندارد فایل نمونه را پشتیبانی کردند.

### موارد قابل اصلاح

1. صوت‌ها mapping آروان ندارند؛ انتخاب «سرور ایرانی» سرعت دانلود صوت را تغییر نمی‌دهد.
2. سه PDF فصل ۸ (`C905f08d01/02/03.pdf`) در Appwrite هستند ولی در catalog نیستند؛ آن‌ها نیز همیشه خارجی می‌مانند.
3. `LessonPdfScreen` مستقیم در فایل target می‌نویسد، نه `.part`، و وجود فایل بیش از ۱KB را کافی می‌داند (`LessonPdfScreen.kt:88-145`). قطع دانلود می‌تواند فایل ناقص >۱KB باقی بگذارد که دفعهٔ بعد دوباره دانلود نشود.
4. مسیر `openTeachPdf` implementation جداگانه و بدون resume مقاوم `DownloadsScreen` دارد (`LessonTeachScreen.kt:762-800`). سه implementation دانلود PDF بهتر است به یک downloader مشترک تبدیل شوند.
5. در `tailMatchesServer` اگر Range پاسخ 206 ندهد یا طول پاسخ غیرمنتظره باشد، بعضی شاخه‌ها verify را آسان می‌گیرند (`MediaVault.kt:513-546`). برای originهای فعلی Range درست است، اما fail-closed مطمئن‌تر است.

---

## ۱۱. HTMLهای ریاضی داخل assets

`MathHtmlAssets` مسیرهایی مانند `math/c905/ryazif...-tamrinat.html`, `...-cards.html` و `...-soalat.html` می‌سازد (`MathHtmlAssets.kt:18-68`) و `MathInteractiveHtml` آن‌ها را با `file:///android_asset/...` باز می‌کند (`MathHtmlWebView.kt:68-73`).

اما inventory واقعی `apps/hamyar-app/src/main/assets/math/c905` فقط این فایل را دارد:

- `hamyar-persist.js`

هیچ HTML مورد ارجاع وجود ندارد. بنابراین تب‌هایی که branch asset را انتخاب می‌کنند به فایل ناموجود می‌روند. این مشکل مستقل از دو bucket و انتخاب سرور است؛ گزینهٔ سرور آن را اصلاح نمی‌کند.

**اقدام P1:** یا HTMLهای لازم واقعاً به assets اضافه شوند، یا همین مسیرها به loader رمز‌شدهٔ remote با mapping دوگانه منتقل شوند.

---

## ۱۲. اسکریپت‌های mirror/upload

### خطای pagination در sync

`scripts/sync_appwrite_to_arvan.py` پارامترهای سادهٔ `limit/offset` می‌فرستد (`lines 62-79`). API فعلی Appwrite این دو پارامتر ساده را نادیده می‌گیرد و query استاندارد `queries[]` می‌خواهد. تست زنده با `limit=2&offset=100` همچنان صفحهٔ اول را برگرداند. نتیجهٔ محتمل: اسکریپت همان ۲۵ فایل اول را تکرار می‌کند و inventory کامل را mirror نمی‌کند.

### کلید مقصد اشتباه

sync هر فایل Appwrite را با `Key=fid` در ریشهٔ آروان می‌نویسد (`sync_appwrite_to_arvan.py:232-254`)، درحالی‌که اپ کلیدهای سلسله‌مراتبی catalog مانند `html ها/...`, `PDF ها/...` را می‌خواند. inventory زنده نشان داد **صفر** شناسهٔ Appwrite در ریشهٔ آروان وجود دارد؛ یعنی این workflow با ساختار فعلی bucket همسو نیست.

اسکریپت رمز HTML نیز همان `fid` را برای Appwrite و Arvan استفاده می‌کند (`Hidden Files/encrypt_html_to_buckets.py:201-219`)، پس به‌روزرسانی `tool-calendar.html` آن را در ریشه می‌گذارد، نه در `html ها/جعبه ابزار عمومی/...`.

### skip بر اساس اندازه

sync در صورت برابری اندازه، فایل را skip می‌کند (`sync_appwrite_to_arvan.py:237-242`). دو محتوای متفاوت می‌توانند اندازهٔ برابر داشته باشند. signature/hash باید معیار باشد.

### مسیر secret محلی

اسکریپت‌ها `/home/user/.hamyar-secrets` و `/home/user/appwrite/credentials.json` را می‌خوانند، اما فایل‌های این checkout در `Hidden Files/` هستند. بدون export دستی env، اجرای local طبق وضعیت فعلی credential را پیدا نمی‌کند.

### ریسک replace در uploader

`encrypt_html_to_buckets.py` ابتدا فایل Appwrite را DELETE و سپس POST می‌کند (`lines 78-95`). اگر POST شکست بخورد، فایل تولیدی از دسترس خارج می‌شود. upload باید با ID موقت + verify + swap امن یا update پشتیبانی‌شده انجام شود.

---

## ۱۳. جایگاه گزینه‌ها در UI

### درست

- منبع محتوا در «بیشتر → تنظیمات → تنظیمات سرور» قرار دارد و در ابتدای صفحه دیده می‌شود (`SettingsScreens.kt:60-86`, `461-481`). این برای تنظیم پیشرفته قابل قبول است.
- مدیریت دانلود داخل هاب مدرسه قرار دارد، نه در تنظیمات عمومی؛ این انتخاب درست است.
- «محتوای همیار» و ابزارها از Home قابل دسترسی‌اند.

### نیازمند شفاف‌سازی

صفحهٔ «تنظیمات سرور» هم‌زمان source محتوای حجیم، وضعیت اتصال backend، outbox، sync و cache داده را نشان می‌دهد. کاربر ممکن است تصور کند انتخاب آروان روی login/sync داده هم اثر دارد، درحالی‌که فقط برخی payloadهای محتوا را تغییر می‌دهد.

پیشنهاد:

1. عنوان بخش اول: «منبع دانلود محتوا».
2. نمایش source مؤثر فعلی: «این فایل از آروان/Appwrite دریافت شد».
3. توضیح پوشش: «فقط فایل‌های دارای نسخهٔ ایرانی» تا زمان تکمیل mapping.
4. جداکردن تنظیمات sync حساب از تنظیمات CDN/content.
5. تغییر نام FASTEST به «خودکار (ایرانی در صورت دسترسی)» مگر اینکه benchmark واقعی پیاده شود.

---

## ۱۴. اولویت‌بندی اصلاحات

| اولویت | اقدام |
|---|---|
| **P0 فوری** | حذف `create/update/delete(any)` از Appwrite bucket؛ جداسازی user uploads؛ روشن‌کردن file security برای دادهٔ کاربر |
| **P1** | عمومی‌کردن read-only هشت object ابزار/آزمایشگاه آروان، بدون بازکردن LIST/WRITE |
| **P1** | اصلاح sync: Appwrite `queries[]`، استفاده از mapping catalog و hash/signature به‌جای size |
| **P1** | تکمیل mapping برای ۸ صوت، سه PDF فصل ۸ و HTMLهای تدریس یا اصلاح متن UI |
| **P1** | رفع HTMLهای ناموجود `assets/math/c905` |
| **P1** | اصلاح MediaFreshness تا metadata را همیشه از Appwrite بگیرد |
| **P2** | cache رمز‌شده و versioned برای ContentHtml و ToolRemote؛ حذف cache plaintext نامحدود |
| **P2** | اصلاح لینک‌های نسبی `amz-01..03` با route داخلی |
| **P2** | حذف/باندل dependency تقویم از jsDelivr برای offline واقعی |
| **P2** | یکپارچه‌سازی downloaderهای PDF و استفادهٔ همیشگی از `.part` + verify |
| **P2** | فعال‌سازی versioning/SSE/lifecycle آروان و محدودکردن CORS به GET/HEAD |
| **P3** | پیاده‌سازی benchmark واقعی دو origin یا تغییر نام FASTEST |

---

## ۱۵. نتیجهٔ نهایی

- **کدینگ/دیکدینگ HTML:** از نظر فرمت و محتوای واقعی درست است؛ همهٔ ۸۰ فایل در هر دو سرور decrypt و یکسان شدند.
- **لینک‌دهی دو سرور:** catalog برای ۱۳۳ قلم صحیح است، اما public ACL هشت HTML آروان اشتباه و mapping رسانه‌های خارج از catalog ناقص است.
- **اثر تنظیمات سرور:** واقعی ولی جزئی و ناسازگار است؛ ادعای «کتاب‌ها، صداها و صفحه‌ها» کامل نیست.
- **loading:** قابل استفاده ولی خطاها بیش از حد کلی و نمایش پیشرفت HTML ضعیف است.
- **cache:** MediaVault خوب؛ cache ابزار و remote teach HTML نیازمند freshness و امنیت بیشتر است.
- **دانلود:** زیرساخت صوت قوی و صفحهٔ مدیریت مناسب است؛ مسیرهای PDF باید یکپارچه شوند.
- **UI:** جایگاه کلی مناسب است، اما source محتوا باید از sync حساب تفکیک و صادقانه‌تر نام‌گذاری شود.
- **تنظیمات bucket:** آروان عمدتاً read-oriented ولی فاقد versioning/SSE و دارای ۸ ACL نادرست است؛ Appwrite از نظر مجوز write/delete عمومی نیازمند اصلاح فوری است.
