# APPWRITE.md — دقیقاً چه چیزی از Appwrite لازم است

این سند جواب سؤال «**چه اطلاعاتی از Appwrite می‌خواهی تا همه‌چی درست باشد؟**» است.
هر بند یا یک مقدار است که باید به من بدهی (یا در کنسول بسازی) یا یک تنظیم است که باید
در کنسول Appwrite روشن/خاموش شود. در پایان، یک چک‌لیست تیک‌زدنی هست.

> نکته‌ی مهم: **بدون هیچ‌کدام از این‌ها اپ خراب نمی‌شود.** اگر `appwrite.projectId` خالی
> بماند، هر دو اپ در «حالت محلی» بالا می‌آیند، داده روی دستگاه ذخیره می‌شود و در UI
> هم صادقانه نوشته می‌شود که سرور وصل نیست.

---

## ۱) سه مقداری که باید در `local.properties` بنویسی

فایل `local.properties` در git نیست (در `.gitignore` است). از روی
`local.properties.example` کپی کن و این سه مقدار را پر کن:

| کلید | از کجا در کنسول | مقدار پیش‌فرض | مثال |
| --- | --- | --- | --- |
| `appwrite.projectId` | Project Settings › General › **Project ID** | *(خالی = حالت محلی)* | `5f4a2b1c9d8e7f6a5b4c` |
| `appwrite.endpoint` | همان صفحه، بخش **API Endpoint** (منطقه) | `https://fra.cloud.appwrite.io/v1` | `https://fra.cloud.appwrite.io/v1` |
| `appwrite.databaseId` | نام دیتابیسی که می‌سازی | `main_db` | `main_db` |

**تنها چیزی که واقعاً از تو می‌خواهم: Project ID و تأیید منطقه (region).**
بقیه‌ی موارد را خودم با همین مقادیر پیش‌فرض می‌سازم.

این مقادیر در زمان بیلد به `BuildConfig.APPWRITE_*` در هر دو اپ تبدیل می‌شوند
(نگاه کن به `apps/*/build.gradle.kts`)، پس بعد از تغییرشان باید بیلد دوباره اجرا شود.

منطقه‌های موجود در Appwrite Cloud: `fra` (فرانکفورت)، `nyc` (نیویورک)، `sgp` (سنگاپور)،
`sydney`. اگر پروژه‌ات جای دیگری است، فقط مقدار `appwrite.endpoint` عوض می‌شود.

---

## ۲) دو پلتفرم اندروید (خیلی مهم: بدون پسوند debug)

در کنسول: **Project Settings › Platforms › Add Platform › Android** و این دو را بساز:

| اپ | Package Name | نام پیشنهادی |
| --- | --- | --- |
| روزهای من (زهرا) | `com.hamyareman.ir` | Hamyar |
| همراه پدر | `ir.behzad.hamrahpadar` | Hamrah Padar |

چرا دو پلتفرم؟ چون هر اپ یک Project ID مشترک دارد ولی Package Name متفاوت است و
Appwrite اجازه‌ی درخواست را بر اساس Package می‌سنجد.

> **عمداً** در بیلد debug پسوند `.debug` روی `applicationId` نمی‌گذاریم
> (در `build.gradle.kts` هم توضیح داده شده). دلیلش این است که در غیر این صورت
> باید برای هر اپ **چهار** پلتفرم در کنسول بسازی. اگر روزی خواستی بیلد debug
> جدا داشته باشی، باید `applicationIdSuffix` را برگردانی و دو پلتفرم دیگر هم اضافه کنی.

فیلد **SHA-1** اختیاری است (برای Google Sign-In لازم می‌شود؛ ما فعلاً فقط
ایمیل/رمز و ورود مهمان داریم).

---

## ۳) روش‌های احراز هویت

کنسول: **Authentication › Settings › (Auth Providers / Security)**

- [ ] **Email / Password** روشن باشد (اپ با `Account.create` و
  `createEmailPasswordSession` کار می‌کند).
- [ ] **Anonymous sessions** روشن باشد (اپ پدر می‌تواند اول مهمان وارد شود و بعد
  با کد پیوند ارتقا پیدا کند).
- [ ] محدودیت «رمز ضعیف» را روشن بگذار (Appwrite خودش `user_weak_password`
  برمی‌گرداند؛ متن فارسی خطا در `core-appwrite/AppwriteErrors.kt` نگاشت شده).
- [ ] اگر **JWT duration** را تغییر دادی، فقط بدان که طول عمر نشست را عوض می‌کند؛
  کد اپ به آن وابسته نیست.
- [ ] **Labels**: هیچ کاری لازم نیست بکنی — برچسب‌های `zahra` / `father` / `guest`
  را خود توابع سرور می‌گذارند (`users.updateLabels`). **کلاینت هرگز نقش نمی‌نویسد.**

---

## ۴) دیتابیس و جدول‌ها

یک دیتابیس با شناسه‌ی `main_db` بساز (یا هر شناسه‌ای که دوست داری و همان را در
`appwrite.databaseId` بگذار). داخلش **۱۹ جدول** لازم است. تعریف کامل ستون‌ها،
نوع‌ها و ایندکس‌ها در [`backend/appwrite.json`](backend/appwrite.json) است؛ آن فایل را
با `appwrite push tables` بفرست یا دستی از رویش در کنسول بساز.

| گروه | جدول‌ها | چه کسی می‌خواند |
| --- | --- | --- |
| هویت و پیوند | `profiles`, `user_settings`, `pairing_codes`, `father_links` | کاربر خودش؛ `pairing_codes` و `father_links` عملاً فقط از راه تابع `pairing` |
| ارتباط | `father_messages`, `album_items`, `call_sessions`, `call_signals` | دو طرف پیوند (Role.user هر دو) — در `album_items` خاطره‌ی پدر تا `approved=true` نشود در آلبوم زهرا «در انتظار تأیید» است |
| خلاصه‌ی هفتگی | `weekly_summaries` | زهرا + پدرِ پیوندشده، فقط با opt-in |
| داده‌ی قابل اشتراک | `routine_blocks`, `water_logs`, `exercise_logs`, `badges` | زهرا؛ پدر فقط از راه خلاصه‌ی هفتگی |
| کاتالوگ محتوا | `lessons`, `quizzes`, `recipes`, `exercises`, `learning_nodes`, `art_prompts` | همه (`read("any")`) — نوشتن فقط با کلید سرور |

### ۴-۱) پنج جدولی که **نباید** در Appwrite بسازی

این‌ها عمداً در سرور وجود ندارند و فقط روی دستگاه زهرا می‌مانند:

`cycle_entries`, `mood_entries`, `journal_entries`, `screen_time_logs`, `chat_history`

`SyncEngine` این جدول‌ها را حتی وارد صف ارسال هم نمی‌کند
(`shared/core-common/.../TableIds.kt › PrivacyPolicy.neverSyncTables`) و
`TablesDbService.upsert` هم اگر کسی اشتباهی صدایشان بزند، خطای `Permission`
برمی‌گرداند. این همان وعده‌ای است که در صفحه‌ی «EthicalNotice» اپ پدر به کاربر داده‌ایم.

### ۴-۲) دسترسی سطرها (Row Security)

- برای همه‌ی جدول‌های خصوصی/کاربری: **Row Security = ON**.
- سطرها با `Permission.read(Role.user(id))` ساخته می‌شوند
  (`AppwriteClientProvider.ownerOnly` و `.sharedWith`).
- برای جدول‌های پیام و تماس، دسترسی **هر دو** کاربر پیوندشده گذاشته می‌شود.
- برای کاتالوگ محتوا (`lessons`, `quizzes`, ...): `read("any")` و **بدون** `create("any")`.

### ۴-۳) پرکردن کاتالوگ محتوا (seed) — یک بار، بعد از ساخت جدول‌ها

محتوای خواندنی (درس، آزمون، دستور آشپزی، ورزش، گره‌های نقشه‌ی راه، ایده‌های نقاشی)
در [`backend/seed/content.json`](backend/seed/content.json) است و با
[`backend/seed/import.js`](backend/seed/import.js) به TablesDB فرستاده می‌شود:

```bash
cd backend/seed
npm install

export APPWRITE_PROJECT_ID="پروژه‌ی تو"
export APPWRITE_API_KEY="کلید API (Console › API Keys › scope: tables.write)"
# اختیاری — پیش‌فرض https://fra.cloud.appwrite.io/v1
export APPWRITE_ENDPOINT="https://fra.cloud.appwrite.io/v1"
# اختیاری — پیش‌فرض main_db
export APPWRITE_DATABASE_ID="main_db"

node import.js --dry-run            # فقط نشان می‌دهد چه سطرهایی می‌فرستد (بدون نصب SDK هم کار می‌کند)
node import.js                      # همه‌ی ۸۷ سطر
node import.js --only=recipes,quizzes   # فقط بعضی جدول‌ها
# یا با npm:  npm run seed:dry   /   npm run seed
```

نکته‌های مهم:

- شناسه‌ی هر سطر **همان `id` داخل content.json** است، پس اجرای دوباره safe است
  (اول `updateRow`، و اگر نبود `createRow` — همان الگوی `TablesDbService.upsert`).
- کلاینت **هیچ‌وقت** به این جدول‌ها نمی‌نویسد؛ فقط می‌خواند. اگر جدول خالی باشد،
  اپ به کش محلی و بعد به محتوای داخلی (`BuiltInContent.kt`) برمی‌گردد، پس صفحه‌ی خالی دیده نمی‌شود.
- ستون‌های آرایه‌ای (`ingredients`, `steps`, `choices`) به‌صورت **رشته‌ی JSON** ذخیره می‌شوند
  (چون TablesDB ستون آرایه‌ای ندارد). ورزش‌ها هم `{"title": "...", "seconds": 30}` قبول می‌کنند
  و هم رشته‌ی ساده (در حالت رشته، ثانیه از `minutes` تقسیم بر تعداد گام‌ها ساخته می‌شود).
- اگر محتوای بیشتری خواستی، فقط `content.json` را ویرایش کن و دوباره `node import.js` بزن؛
  **نیازی به انتشار نسخه‌ی تازه‌ی اپ نیست** (کلاینت هر بار فهرست را از سرور می‌گیرد و کش می‌کند).

---

## ۵) باکت‌های Storage

کنسول: **Storage › Create bucket** — دقیقاً با همین شناسه‌ها:

| Bucket ID | کاربرد | سقف حجم پیشنهادی | دسترسی |
| --- | --- | --- | --- |
| `heart-to-heart-media` | ویس/عکس/ویدیو/فایل «حرف دل» | ۱۰ مگابایت | `fileSecurity=true` + دسترسی هر دو طرف پیوند |
| `avatars` | تصویر پروفایل | ۲ مگابایت | فقط خود کاربر |
| `father-album` | آلبوم خاطرات مشترک (`album_items` در TablesDB؛ بندانگشتی محلی در `files/media/album`) | ۱۰ مگابایت | دو طرف پیوند؛ به‌روزرسانی/حذف فقط زهرا و سازنده‌ی خاطره |
| `zahra-private` | رسانه‌ی کاملاً خصوصی زهرا (ژورنال/چرخه) | ۱۰ مگابایت | **خالی** — فقط از راه تابع سرور قابل خواندن |

کلاینت هم همین سقف ۱۰ مگابایت را قبل از آپلود چک می‌کند
(`MediaFiles.MAX_BYTES`) تا آپلود طولانی و شکست‌خورده درست نشود.

---

## ۶) نه تابع سرور (Functions)

هر نه در `backend/functions/` با Node 20 نوشته شده‌اند. کد آماده است؛ فقط باید
در کنسول ساخته و deploy شوند.

اصل طراحی: **هر منطقی که دورزدنش خطرناک است، سمت سرور است** — متن هشدار، بررسی
مالکیت آلبوم، ترتیب درس‌ها، تجمیع داده‌ی قابل اشتراک. کلاینت فقط درخواست می‌دهد و
نتیجه را صادقانه نشان می‌دهد؛ اگر تابعی deploy نشده باشد اپ روی مسیر محلی می‌ماند.

| Function ID | چه کار می‌کند | چه کسی صدایش می‌زند | Execute permission |
| --- | --- | --- | --- |
| `user-bootstrap` | ساخت `profiles`/`user_settings` + گذاشتن Label (`zahra` یا `guest`) | اپ، بعد از ورود/ثبت‌نام | `any` (بدون نشست هم برای دیباگ اجرا می‌شود، ولی کاربر باید لاگین باشد) |
| `pairing` | `create` / `revoke` / `redeem` / `unlink` کد شش‌رقمی + ساخت `father_links` + ارتقای Label پدر به `father` | اپ زهرا (ساخت کد)، اپ پدر (مصرف کد) | `users` |
| `weekly-summary` | ساخت خلاصه‌ی هفتگی، **فقط** اگر `weeklyOptIn=true` و پیوند فعال باشد | cron (جمعه ۲۰:۰۰) یا دستی | `users` |
| `ai-companion` | لایه‌ی AI «همراه زهرا»: پرامپت سیستمی + صدا‌زدن مدل + **فیلتر بحران سمت سرور** | اپ زهرا، صفحه‌ی چت (اگر لایه‌ی AI روشن باشد) | `users` |
| `notify-guardian` | **جدید** — هشدار «کمک می‌خوام» به پدر: متن و مقصد را سرور می‌سازد، پیوند فعال را بررسی می‌کند، محدودیت نرخ ۵ دقیقه‌ای دارد و همیشه شماره‌های اضطراری را برمی‌گرداند | اپ زهرا: مسیر بحران در چت + دکمه‌ی «به بابا خبر بده» در صفحه‌ی شماره‌های کمک | `users` |
| `lesson-of-the-day` | **جدید** — انتخاب «درس امروز» از کاتالوگ سرور با رعایت ترتیب (`grade`)، پیش‌نیازها و فهرست درس‌های تمام‌شده؛ حالت `review` وقتی همه خوانده شده باشند | اپ زهرا: صفحه‌ی «آموزش هوش مصنوعی» | `any` |
| `catalog-digest` | **جدید** — اثر انگشت (sha256/16) کاتالوگ محتوا؛ اگر عوض نشده باشد اپ دانلود چندصدسطری را **انجام نمی‌دهد** | اپ زهرا: `CatalogRepository` قبل از خواندن هر جدول محتوا (هر ۱۲ ساعت یک‌بار) | `any` |
| `daily-checkin` | **جدید** — خلاصه‌ی روزانه‌ی opt-in برای پدر با شناسه‌ی deterministic (اجرای چندباره سطر تکراری نمی‌سازد) | اپ زهرا هنگام روشن‌کردن opt-in + cron هر شب ۲۱:۰۰ | `users` |
| `album-consent` | **جدید** — تأیید/رد خاطره‌ی پدر در آلبوم با بررسی **مالکیت سمت سرور** (`ownerId`) | اپ زهرا: `AlbumRepository.setApproved` (با بازگشت به update مستقیم اگر تابع نبود) | `users` |

### قرارداد JSON تابع `pairing`

```jsonc
// زهرا → ساخت کد
{ "action": "create" }        →  { "ok": true, "code": "123456", "expiresAtMs": 1767700000000 }
// زهرا → باطل‌کردن
{ "action": "revoke" }        →  { "ok": true }
// پدر → مصرف کد
{ "action": "redeem", "code": "123456" }
                              →  { "ok": true, "partnerId": "<userId زهرا>", "partnerName": "زهرا",
                                   "linkedAt": 1767700000000, "status": "active" }
// خطاها: invalid_code | code_not_found | code_expired | cannot_pair_self | unauthenticated
// هر دو → قطع پیوند
{ "action": "unlink" }        →  { "ok": true }
```

کد پیوند: شش رقم، **یک‌بارمصرف**، عمر ۱۰ دقیقه
(`TTL_MS` در `backend/functions/pairing/src/main.js` — قابل تغییر است).

### قرارداد JSON تابع `ai-companion`

```jsonc
// ورودی
{ "message": "امروز خیلی خسته‌ام", "tone": "warm" | "formal",
  "history": [ { "role": "user" | "assistant", "content": "..." } ] }   // حداکثر ۶ پیام آخر

// پاسخ مدل
{ "ok": true, "crisis": false, "reply": "...", "model": "qwen3.8-flash" }

// پیام بحران (هرگز به مدل فرستاده نمی‌شود)
{ "ok": true, "crisis": true, "reply": "...", "helplines": [ { "name": "...", "number": "1480" } ],
  "model": "safety-local" }

// خطاها — اپ به قواعد محلی برمی‌گردد و صادقانه می‌گوید
{ "ok": false, "error": "not_configured" | "upstream_error" | "network_error" | "empty_message" | "empty_reply",
  "fallback": "..." }
```

قواعد امنیتیِ داخل خود تابع (نه در اپ، تا با deploy عوض شوند):

- پرامپت سیستمی: فارسی ساده، حداکثر ۴ جمله، «درمانگر نیستم»، بدون توصیه‌ی
  پزشکی/دارویی/حقوقی، بدون ساختن منبع، و پایان با یک سؤال کوچک.
- پیام‌های با نشانه‌ی آسیب به خود **به مدل نمی‌روند**؛ پاسخ ایمنی + شماره‌های
  ۱۴۸۰ / ۱۲۳ / ۱۵۷۰ / ۱۱۵ برمی‌گردد.
- متن پیام **لاگ نمی‌شود** (فقط طول پیام و نام مدل) و هیچ‌جا ذخیره نمی‌شود.
- متن خطای upstream به کلاینت برنمی‌گردد (ممکن است کلید یا اطلاعات داخلی داشته باشد).
- هم فرمت پاسخ OpenAI-سازگار (`choices[0].message.content`) خوانده می‌شود و هم
  Gemini-مانند (`candidates[0].content.parts[].text`).

### متغیرهای محیطی توابع

`APPWRITE_FUNCTION_API_ENDPOINT` و `APPWRITE_FUNCTION_PROJECT_ID` را خود Appwrite
تزریق می‌کند. اما یک مورد را باید دستی بگذاری:

- [ ] یک **کلید API با دسترسی سرور** بساز (کنسول: *Project Settings › API Keys* یا
  در نسخه‌های جدیدتر *Scoped/Dynamic Keys*) با این دامنه‌ها:
  `users.write`, `tables.write` (یا `documents.write` در پروژه‌های قدیمی‌تر),
  `collections.read`.
- [ ] مقدارش را به‌عنوان متغیر محیطی **`APPWRITE_FUNCTION_API_KEY`** روی هر نه تابع بگذار.

برای تابع `ai-companion` این متغیرها را هم بگذار (کنسول: *Functions › ai-companion › Settings › Variables*):

| متغیر | مقدار | لازم؟ |
| --- | --- | --- |
| `AI_API_KEY` | کلید ارائه‌دهنده‌ی مدل | **بله** — بدون آن تابع `not_configured` برمی‌گرداند و اپ با قواعد محلی جواب می‌دهد |
| `AI_MODELS` | فهرست مدل‌ها با کاما؛ **اولی** استفاده می‌شود. مثال: `qwen3.8-flash,glm-5.3-flash,mimo-v2.5,hy3` | بله (یا `AI_MODEL`) |
| `AI_MODEL` | اگر باشد بر `AI_MODELS` اولویت دارد | خیر |
| `AI_ENDPOINT` | آدرس کامل chat/completions. پیش‌فرض: `https://api.openai.com/v1/chat/completions` | فقط اگر ارائه‌دهنده‌ات سازگار با OpenAI نیست |
| `AI_MAX_TOKENS` / `AI_TEMPERATURE` | پیش‌فرض `400` / `0.6` | خیر |

> ⚠️ **کلید را در چت یا در ریپو نگذار.** کلیدی که در پیام‌ها جابه‌جا شود «لو رفته»
> حساب می‌شود: همان را در کنسول ارائه‌دهنده باطل (revoke) کن و کلید تازه بساز،
> بعد فقط در متغیرهای محیطی تابع بگذار. در این ریپو هیچ کلیدی commit نشده و
> `local.properties` هم در `.gitignore` است.
- [ ] (اختیاری) `APPWRITE_DATABASE_ID=main_db` — اگر نام دیتابیست چیز دیگری است.

> این کلید **فقط** داخل توابع است و هرگز وارد APK نمی‌شود. به همین دلیل
> ارتقای نقش پدر فقط سمت سرور ممکن است.

### deploy

```bash
npm install -g appwrite          # یا npx appwrite
appwrite login
appwrite init                    # پروژه را انتخاب کن
appwrite push tables             # جدول‌ها از backend/appwrite.json
appwrite deploy function user-bootstrap
appwrite deploy function pairing
appwrite deploy function weekly-summary
```

اگر خواستی وضعیت واقعی پروژه را بکشی داخل ریپو: `appwrite pull tables`
(فایل `backend/appwrite.json` بازنویسی می‌شود).

---

## ۷) Realtime — روشن است (نیاز به تنظیم اضافه ندارد)

کلاینت الان روی این کانال‌ها مشترک می‌شود (`core-appwrite/RealtimeFeed.kt`):

```
tablesdb.main_db.tables.father_messages.rows
tablesdb.main_db.tables.album_items.rows
tablesdb.main_db.tables.call_signals.rows
```

دو نکته‌ی مهم:

- **قالب نام کانال** `tablesdb.<db>.tables.<table>.rows` است (از کلاس `Channel` در
  SDK رسمی Appwrite استخراج شد). قالب قدیمی `databases.<db>.collections.<col>.documents`
  برای TablesDB معتبر نیست.
- **هیچ تنظیمی در کنسول لازم نیست**: Realtime از همان Permissionهای سطر پیروی
  می‌کند. اگر socket باز نشود، اپ خودش روی polling می‌ماند (هنگام تماس هر ۵ ثانیه،
  در «حرف دل» با بازکردن صفحه/دکمه‌ی تازه‌سازی) و چیزی خراب نمی‌شود.

اگر در کنسول Realtime را برای پروژه خاموش کرده‌ای، فقط همین یک کلید را روشن کن؛
بقیه‌ی کارها در کد انجام شده است.

---

## ۸) تماس صوتی: STUN و TURN

- **STUN**: پیش‌فرض در کد هست (`stun:stun.cloudflare.com:3478` و
  `stun:stun.l.google.com:19302`) و برای شبکه‌ی خانوادگی معمولاً کافی است.
- **TURN**: اگر تماس پشت NAT سخت‌گیر (اینترنت همراه/شبکه‌ی اداری) وصل نشد، به یک
  سرور TURN نیاز داری. آن وقت این سه مقدار را به من بده تا در
  `AppContainer` به `CallEngine` پاس داده شود:

```kotlin
val calls = CallEngine(
    context = context,
    signaling = signaling,
    selfUserId = { auth.currentUserId() },
    iceServers = CallEngine.DEFAULT_ICE_SERVERS + listOf(
        IceServerConfig("turn:turn.example.com:3478", username = "...", credential = "..."),
    ),
)
```

گزینه‌های رایگان/ارزان: Cloudflare Calls TURN، Twilio Network Traversal، Metered.ca،
یا self-host با `coturn`.

**تماس تصویری هم پیاده شده است** (دوربین جلو با `Camera2Enumerator`، ترک ویدیو،
رندر با `SurfaceViewRenderer`، چرخاندن دوربین، خاموش/روشن‌کردن ویدیو). برای ویدیو
به TURN بیشتر از صوتی نیاز پیدا می‌کنی، چون حجم داده چند برابر است:

- [ ] مجوز `CAMERA` در منیفست هر دو اپ هست ✅ و در زمان تماس درخواست می‌شود.
- [ ] اگر کیفیت ویدیو در شبکه‌ی موبایل بد بود، رزولوشن پیش‌فرض
  (`VIDEO_WIDTH/VIDEO_HEIGHT/VIDEO_FPS` در `CallEngine` = ۶۴۰×۴۸۰@۲۴) را پایین بیاور
  یا TURN اضافه کن.
- [ ] تماس با سرویس پیش‌زمینه (`CallForegroundService`، نوع `microphone`) نگه داشته
  می‌شود تا با خاموش‌شدن صفحه قطع نشود؛ مجوزهای
  `FOREGROUND_SERVICE_MICROPHONE`/`FOREGROUND_SERVICE_PHONE_CALL` در منیفست هستند.

---

## ۹) چک‌لیست نهایی (اگر همه تیک خورد، همه‌چی درست کار می‌کند)

**مقادیر**
- [ ] `appwrite.projectId` در `local.properties`
- [ ] `appwrite.endpoint` تأیید شد (منطقه‌ی درست)
- [ ] `appwrite.databaseId` = `main_db` (یا نام دلخواه + ساخت دیتابیس با همان نام)

**کنسول**
- [ ] دیتابیس `main_db` ساخته شد، Row Security روشن است
- [ ] ۱۹ جدول از `backend/appwrite.json` ساخته شد (شامل `album_items` برای آلبوم مشترک)
- [ ] ۵ جدول خصوصی **ساخته نشد** (عمداً)
- [ ] ۴ باکت با همان شناسه‌ها ساخته شد (`zahra-private` بدون دسترسی خواندن عمومی)
- [ ] Email/Password و Anonymous در Authentication روشن است
- [ ] دو پلتفرم اندروید: `com.hamyareman.ir` و `ir.behzad.hamrahpadar`
- [ ] **نه تابع** deploy شد و کلید `APPWRITE_FUNCTION_API_KEY` روی همه‌شان گذاشته شد
- [ ] `notify-guardian`: متغیر `GUARDIAN_RATE_LIMIT_MS` (اختیاری، پیش‌فرض ۳۰۰۰۰۰)
- [ ] `ai-companion`: `AI_API_KEY` و `AI_MODELS`/`AI_MODEL`/`AI_ENDPOINT`
- [ ] cron تابع `weekly-summary` روی جمعه ۲۰:۰۰ و `daily-checkin` روی هر شب ۲۱:۰۰
- [ ] Realtime در سطح پروژه روشن است (تنظیم اضافه‌ای برای جدول‌ها لازم نیست)
- [ ] `node backend/scripts/verify.js` اجرا شد و همه‌ی ✓ سبز است (مقایسه‌ی پروژه‌ی واقعی
      با `backend/appwrite.json`)
- [ ] `node backend/functions/tests/run-tests.js` → ۳۶ پاس (بدون شبکه و بدون نصب؛
      منطق توابع را با SDK ساختگی روی داده‌ی واقعی seed بررسی می‌کند)
- [ ] (اختیاری) سرور TURN برای تماس (برای تماس تصویری تقریباً واجب است)

**تست سریع بعد از تنظیم**
1. اپ زهرا را باز کن › تنظیمات › «همگام‌سازی و بک‌اند» باید بگوید *متصل به Appwrite*.
2. در «افزودن بابا» یک کد شش‌رقمی بساز.
3. اپ پدر را روی همان دستگاه/دستگاه دیگر باز کن، مهمان وارد شو و کد را بده.
4. در «حرف دل» یک پیام بفرست؛ باید در کنسول در جدول `father_messages` دیده شود
   و در اپ پدر هم ظاهر شود.
5. در کنسول › Users، کاربر پدر باید Label `father` داشته باشد.

---

## ۱۰) اگر فقط همین الان می‌خواهی شروع کنی

کمترین کاری که لازم است:

```properties
# local.properties
appwrite.projectId=<Project ID از کنسول>
appwrite.endpoint=https://fra.cloud.appwrite.io/v1
appwrite.databaseId=main_db
```

بعد `appwrite push tables` و `appwrite deploy function` برای هر سه تابع.
بقیه‌ی موارد (TURN، آواتار) اختیاری‌اند و اپ بدون آن‌ها هم کامل کار می‌کند.
(Realtime دیگر اختیاری نیست: کد کلاینت رویش مشترک می‌شود، ولی اگر در کنسول خاموش باشد
اپ خودش روی polling می‌ماند.)
