# نقشهٔ کامل شاخه‌ای اپلیکیشن «همیار من» و ادمین

> **نسخهٔ بررسی:** ۲ اکتبر ۲۰۲۶ · **پایهٔ فعال:** نهم (`p09`) · **زبان/جهت:** فارسی، RTL
> **دامنه:** APK دانش‌آموز `com.hamyareman.ir` و APK جداگانهٔ ادمین `com.hamyareman.admin`
> **علائم وضعیت:** ✅ آماده و قابل استفاده · ↔ همگام/دادهٔ پویا · ☁️ از سرور/باکت · 💾 داخل اپ یا ذخیرهٔ محلی · 🔒 نیازمند ورود/قفل · ⏳ صفحه/ویژگی با وضعیت «به‌زودی»

این سند نقشهٔ **UI واقعی** است، نه فقط فهرست فایل‌های Kotlin: تب‌ها، کارت‌ها، مسیرهای پارامتری، زیرشاخه‌های داده‌ای، محل محتوا، کش، وضعیت دسترسی و تایپوگرافی را یکجا نشان می‌دهد. مسیرهای پارامتری با `{…}` نشان داده شده‌اند؛ هر کدام ممکن است چندین صفحهٔ واقعی بسازند.

---

## 1. قرارداد کلی محتوا و وضعیت داده

| حوزه | منبع اول | fallback / کش | وضعیت و نکته |
|---|---|---|---|
| کارت‌ها، متن‌های ثابت، برنامه‌ها، یادداشت‌ها، تایمرها | 💾 کد و `LocalStore` دستگاه | — | ✅ آفلاین-اول؛ دادهٔ شخصی در صورت ورود همگام می‌شود. |
| کاتالوگ تعاملی سلامت/ابزار | 💾 `content/catalog.json` و `server-map.json` داخل APK | —؛ خود رسانه از ☁️ ParsPack می‌آید | ✅ ۷ دسته، ۸۰ آیتم؛ HTMLها در باکت HMK1 هستند و فقط در حافظه باز می‌شوند. |
| کتاب و فصل‌ها | 💾 `books-menu.json` داخل APK | ☁️ `Bucket/Pdf-files/G09` روی ParsPack و کش فایل | ✅ ۱۴ کتاب؛ PDF/صوت قابل دانلود و کش؛ PDF همیشه روشن. |
| PDF، صوت، ویدیو و HTML درس | ☁️ ParsPack/Appwrite Storage مطابق `server-map.json` | 💾 کش اختصاصی هر نوع | ✅ کش HTML فقط ciphertext دارد؛ plaintext HTML روی دیسک نوشته نمی‌شود. |
| پروفایل، اشتراک، تیکت، اطلاعات کاربر | ☁️ Appwrite TablesDB/Auth/Storage | 💾 mirror و صف همگام‌سازی | ↔ در نبود شبکه، قسمت‌های محلی کار می‌کنند؛ عملیات حساب بعداً همگام می‌شود. |
| یادداشت، ژورنال، آب، حال، چرخه، تمرین، گالری خصوصی | 💾 `LocalStore` / فایل خصوصی اپ | ☁️ StateSync در صورت ورود | ✅ دادهٔ خصوصی محلی است؛ صفحات حساس پشت قفل فضای امن‌اند. |
| آموزش AI، آزمون، دستور غذا، هنر، ورزش | ☁️ TablesDB | 💾 `BuiltInContent` و `ExerciseCatalog` | ↔ ترتیب: سرور → کش → محتوای داخلی؛ کش digest تا ۱۲ ساعت از درخواست تکراری جلوگیری می‌کند. |
| به‌روزرسانی APK دانش‌آموز | ☁️ manifest/update مسیر عمومی ParsPack | 💾 اطلاعات نسخه نصب‌شده | ✅ بررسی دستی/اعلان آپدیت؛ نصب فقط با نصب‌کنندهٔ رسمی Android. |

### شمارش محتوای بیرونیِ نسخهٔ بررسی‌شده

- `catalog.json`: **۷ دسته / ۸۰ آیتم HTML**.
- `books-menu.json`: **۱۴ کتاب** پایهٔ نهم.
- `server-map.json`: **۳۷۸ entry** با base `https://c539776.parspack.net`.
- آرشیو جداگانهٔ `0000`: **۴۱ HTML** HMK1، با نام و مسیر اصلی `0000/...` حفظ‌شده؛ [ZIP عمومی](https://c539776.parspack.net/archives/0000-hmk1.zip).

---

## 2. تایپوگرافی؛ فونت، جای استفاده و اندازهٔ پیش‌فرض

همهٔ نقش‌ها در `AppTypography.kt` تعریف شده‌اند. کاربر از «ظاهر و فونت» می‌تواند نقش‌های قابل تغییر را تنظیم کند؛ بنابراین اعداد زیر **default** هستند، نه مقدار قفل‌شده. نقش‌های A/B/E و نوار پایین عمداً به **Vazirmatn Light** اجبار شده‌اند تا منوها یکدست باشند.

| شناسه | محل استفاده | فونت / وزن پیش‌فرض | اندازه |
|---|---|---|---:|
| A.title | عنوان آکاردئونِ منوها و کتاب‌ها | Vazirmatn Light | ۱۹sp |
| A.sub | توضیح آکاردئون | Vazirmatn Light | ۱۴sp |
| B.title | عنوان کارت و زیرکارت | Vazirmatn Light | ۲۰sp |
| B.sub | توضیح کارت | Vazirmatn Light | ۱۴sp |
| C.title | عنوان اصلی صفحهٔ داخلی | Vazirmatn Bold | ۱۹sp |
| C.heading | تیترهای داخلی | Vazirmatn Bold | ۲۱sp |
| C.body | متن اصلی/بدنه | Gandom Bold | ۱۸sp |
| C.table | جدول‌ها | Badkhat Bold | ۱۷sp |
| C.button | برچسب دکمه | Estedad Bold | ۱۶sp |
| E.title | عنوان هاب‌های غیر داشبورد | Vazirmatn Light | ۲۵sp |
| E.sub | زیرعنوان هاب | Vazirmatn Light | ۱۴sp |
| D1 | خوش‌آمد داشبورد | Aviny Bold | ۲۹sp |
| D2 | زیر خوش‌آمد | Shekari Bold | ۲۲sp |
| D3 | تاریخ جلالی | Estedad Bold | ۱۸sp |
| D4 | ساعت | Estedad Bold | ۱۸sp |
| D5 | تاریخ میلادی | Estedad Thin | ۱۸sp |
| D6 | کادر اشتراک | Sahel Bold | ۱۷sp |
| D7 | سخن روز/بزرگان | Tanha Bold | ۱۷sp |
| D8 | ۹ کاشی داشبورد | Parastoo Bold | ۱۸sp |
| D9 | تیتر بخش‌های برنامهٔ کلاسی | Lalezar Thin | ۱۹sp |
| D10 | سه کادر برنامه کلاسی | Badkhat Bold | ۲۱sp |
| D11 | متن تیک‌ها | Sahel Bold | ۱۲sp |
| D12 | روز و تاریخ کادرهای کلاس | Sahel Bold | ۱۵sp |
| nav.bar | عنوان ۶ تب پایین | Vazirmatn Light | ۱۳sp |

**قاعدهٔ صفحات HTML/PDF:** فونت متن HTML از خود HTML/وب‌ویو می‌آید؛ فایل‌های HTML بسته به `HMK1` در حافظه باز می‌شوند. PDF فونت را از خود PDF می‌گیرد و هرگز dark-theme نمی‌شود. برای کارت‌ها/هاب‌های خاص، `TypeSlots` می‌تواند یک override ذخیره‌شدهٔ کاربر اعمال کند.

---

## 3. درخت اصلی APK دانش‌آموز

```text
شروع اپ
├─ ورود/بازیابی نشست  🔒 ↔ Appwrite Auth + شناسه دستگاه
│  ├─ ورود ایمیل/رمز، ساخت حساب، بازیابی رمز، تأیید ایمیل و ورود Google Native
│  ├─ کنترل تأیید ایمیل/Account Gate پیش از ورود به پوسته
│  └─ تعیین پایه/دستگاه و mirror پروفایل
└─ پوستهٔ اصلی (۶ تب پایین)
   ├─ 1) داشبورد
   ├─ 2) مدرسه
   ├─ 3) آموزشگاه
   ├─ 4) سلامتی
   ├─ 5) همراه من
   └─ 6) بیشتر
```

### 3.1 تب «داشبورد» — `home`

**نوع:** داشبورد شخصی، اسکرول عمودی؛ تاریخ/ساعت هر ثانیه تازه می‌شود.
**منبع:** ترکیب 💾 تنظیمات محلی، ↔ پروفایل/اشتراک، و دادهٔ تقویم داخلی.
**وضعیت:** ✅؛ ورود لازم نیست، اما نام/اشتراک واقعی بعد از ورود خوانده می‌شود.

```text
داشبورد
├─ بنر خوش‌آمد D1/D2 → نام mirror پروفایل
├─ کارت زمان
│  ├─ تاریخ جلالی D3 → تقویم برنامه کلاسی
│  ├─ ساعت D4 و تاریخ میلادی D5
│  └─ تعطیلی/مناسبت → دادهٔ تقویم داخلی
├─ آواتار/پروفایل → پروفایل من
├─ چیپ اشتراک D6 → اشتراک
├─ کاشی‌های سریع D8
│  ├─ 🎒 مدرسه → هاب مدرسه
│  ├─ 📖 کتاب متنی و صوتی → مطالعهٔ آزاد
│  ├─ 🪷 ذهن‌آگاهی → هاب ذهن‌آگاهی
│  └─ 🌙 کسب آرامش → هاب آرامش
├─ برنامهٔ کلاسی امروز D9–D12
│  ├─ برنامهٔ کلاس → برنامهٔ هفتگی/کلاسی
│  ├─ آماده‌سازی فردا → Tomorrow Prep
│  ├─ آلارم/شیفت → شیفت مدرسه
│  └─ مرخصی → ثبت مرخصی
├─ کارت سخن روز D7 → متن داخلی روزانه
├─ روتین امروز → روتین
├─ آب بنوش → ثبت آب
└─ FAB قلب زرد → تمرین‌های بین دروس
```

### 3.2 تب «مدرسه» — `study`

**نوع:** هاب کتاب‌ها + آکاردئون‌های برنامه‌ریزی.
**منبع:** کتاب‌ها ☁️/💾، برنامه و یادداشت‌ها 💾/↔. **وضعیت:** ✅.

```text
مدرسه
├─ کتاب‌های پایه نهم (۱۴ جلد؛ هر جلد کارت بزرگ‌تر)
│  └─ `study-book/{bookCode}` → صفحهٔ کتاب
│     ├─ جلد، توضیح و پیشرفت
│     ├─ آکاردئون فصل/درس؛ در هر سطح فقط یک sibling باز می‌ماند
│     ├─ `book-node/{key}/{title}?audio=` → PDF/HTML/صوت یک گره
│     ├─ `study-teach/{packId}` → تدریس/جمع‌بندی
│     ├─ `study-lesson/{packId}` → مطالعهٔ عمیق/فلش‌کارت/تمرین
│     ├─ `study-lesson-pdf/{packId}` → PDF با pinch/pan/reset
│     ├─ `video-teach/{packId}` → ویدیوی تدریس تمام‌صفحه
│     └─ `quiz?lessonId=` → آزمون درس → مرور آزمون
├─ دانلودها
│  └─ مدیریت دانلود صوت/PDF هر کتاب؛ حجم، وضعیت، حذف/بازخوانی کش
├─ برنامه هفتگی و مرخصی
│  ├─ برنامهٔ هفتگی من
│  ├─ مرخصی (علت، بازه، گواهی، وضعیت توجیه)
│  └─ برنامهٔ مدرسه/شیفت/زنگ‌ها و کلاس مجازی
└─ آزمون و بازخورد
   ├─ جزوه‌های شخصی و آزمونی (PDF/فایل محلی)
   └─ نمودار پیشرفت دروس، قابل فیلتر بر اساس کتاب
```

**کتاب‌ها/گره‌های پویا:** عنوان و سلسله‌مراتب از `books-menu.json` می‌آید؛ فایل واقعی از ParsPack مطابق `server-map.json` خوانده می‌شود. وضعیت هر گره: PDF/صوت/HTML آماده، یا «در دست تولید» اگر key فایل منتشر نشده باشد. صفحهٔ کتاب از asset menu کار می‌کند و برای رسانه به شبکه/کش نیاز دارد.

### 3.3 تب «آموزشگاه» — `academy`

**نوع:** هاب مهارت و خلاقیت. **منبع:** ↔ کاتالوگ سرور/کش/محتوای داخلی. **وضعیت:** ✅؛ صفحهٔ «به‌زودی» فقط برای مسیرهای ناتمام استفاده می‌شود.

```text
آموزشگاه
├─ آموزش هوش مصنوعی
│  ├─ درس‌های من → فهرست درس → صفحهٔ درس → آزمون
│  ├─ یادگیری با AI → خانهٔ AI → ارزیابی AI
│  └─ مسیرهای یادگیری → roadmap با فیلتر track
└─ متفرقه
   └─ هنر روزانه → prompt روز → گالری آثار محلی/همگام‌شده
```

### 3.4 تب «سلامتی» — `health`

**نوع:** هاب کارتی؛ اقلام زیر با `PracticeGroup` و `PracticeItem` شاخه می‌سازند.
**منبع:** کارت‌ها 💾، تمرین‌های راهنما 💾، HTMLهای تعاملی ☁️/💾. **وضعیت:** ✅.

```text
سلامتی
├─ پیشرفت سلامتی → آب، ورزش و آمار درس
├─ چرخه ماهانه (در صورت پروفایل دختر)
│  └─ تقویم، ثبت علائم، امروز، تمرینات دوره
├─ یوگا → category:yoga → فهرست HTML → وب‌ویو HMK1
├─ ورزش عمومی → category:sport → فهرست HTML → وب‌ویو HMK1
├─ آب و تغذیه
│  ├─ ثبت لیوان آب
│  └─ راهنمای تغذیه/تمرکز
├─ خواب
│  ├─ ثبت خواب
│  ├─ قصه/آماده‌سازی شب
│  ├─ تنفس شب
│  └─ موسیقی و صدای طبیعت
├─ یادآور دارو و مراقبت
└─ روتین روز
```

### 3.5 تب «همراه من» — `chat`

**نوع:** گفت‌وگو با همراه AI، تاریخچه و تنظیمات.
**منبع:** 💾 تاریخچه/تنظیم محلی؛ ☁️ Appwrite Function برای پاسخ در صورت فعال‌بودن.
**وضعیت:** ✅ با fallback امن محلی؛ پیام بحران به مدل ارسال نمی‌شود و صفحهٔ کمک ارائه می‌شود.

```text
همراه من
├─ گفت‌وگوی اصلی
├─ تنظیمات گفت‌وگو
└─ در وضعیت حساس → شماره‌های کمک / مسیر امن
```

### 3.6 تب «بیشتر» — `more`

```text
بیشتر
├─ فضای امن من 🔒 → PIN/قفل و هاب خصوصی
│  ├─ دفترچه خصوصی
│  ├─ دفترها
│  ├─ نوشتن آزاد
│  ├─ گالری خصوصی
│  └─ نوشتن هدایت‌شده
├─ شماره‌های کمک → شماره‌های اضطراری/حمایتی
├─ زمان درس → کلید بی‌صداکردن صوت/ویدیوی تدریس
├─ پروفایل من ↔ → مشخصات، مدرسه، عکس، اشتراک، خروج
├─ تنظیمات
│  ├─ ظاهر و فونت
│  ├─ حریم خصوصی
│  ├─ تنظیمات گفت‌وگو
│  ├─ نشان‌ها
│  ├─ قفل برنامه
│  ├─ یادآورها
│  └─ همگام‌سازی
├─ بررسی به‌روزرسانی APK
├─ دربارهٔ ما
└─ تماس با ما ↔ → پیش‌نویس، ارسال، پیگیری و پاسخ پشتیبانی
```

---

## 4. شاخه‌های جزئی و صفحات مرتبط

### 4.1 چرخه، حال و آرام‌سازی

| مسیر | عنوان/کارکرد | محتوا و وضعیت |
|---|---|---|
| `cycle` | هاب چرخهٔ قدیمی | ⏳/unavailable: route تعریف شده، اما `composable` ندارد؛ از UI باز نمی‌شود. |
| `cycle-cal` | تقویم چرخه | 💾 علائم و تاریخ‌ها؛ ✅ |
| `cycle-log` | ثبت روزانه چرخه | 💾/↔؛ ✅ |
| `cycle-today` | امروز چرخه | توصیه بر پایهٔ داده محلی؛ ✅ |
| `cycle-training` | تمرینات مخصوص دوره | ۴ تب یوگا/تنفس/کنترل درد/آرامش؛ 💾 و ☁️ HTML؛ ✅ |
| `mood` | ثبت حال | 💾/↔؛ ✅ |
| `calm-hub` | کسب آرامش | تمرین تنفس، ژورنال، شماره کمک و زیرگروه‌ها؛ ✅ |
| `background-music` | نجواهای آرام‌بخش طبیعت | ☁️ `background-music-full.html` در باکت؛ پلیر داخلی؛ ✅ |
| `sleep-breath` | تنفس شب | راهنمای داخلی؛ ✅ |
| `sleep-night` | آماده‌سازی/قصهٔ شب | ترکیب 💾/☁️ رسانه؛ ✅ |
| `breath` | تنفس هدایت‌شده | تایمر و انیمیشن محلی؛ ✅ |
| `mindfulness` / `awareness` | ذهن‌آگاهی | شاخهٔ تمرین + سؤال روزانه؛ 💾/↔؛ ✅ |

### 4.2 درخت «ذهن‌آگاهی»، «کسب آرامش» و «بین دروس»

این سه هاب از `WellnessMenu.kt` خوانده می‌شوند؛ هر group کارت زیرگروه و هر item صفحهٔ راهنمای مرحله‌ای با زمان/بدنهٔ محلی است. اگر item به HTML یا route موجود وصل باشد، همان مقصد باز می‌شود.

```text
ذهن‌آگاهی
├─ حضور ذهن: اسکن بدن، ۵-۴-۳-۲-۱، یک دقیقه نفس
├─ خودهیپنوز سالم
│  ├─ ترغیب مطالعه
│  ├─ اعتمادبه‌نفس امتحان
│  ├─ افکار سالم و گفت‌وگوی درونی
│  ├─ مسائل اجتماعی و روابط
│  └─ خواب
├─ افکار/روابط/ژورنال/یادگیری (زیرگروه‌های راهنما)
└─ سؤال خودشناسی روزانه → دفتر خط‌دار و تاریخچه پاسخ‌ها ↔

کسب آرامش
├─ تنفس
├─ آرام‌سازی عضلانی (PMR)
├─ سفر ذهنی و تجسم
├─ داستان و صداهای آرام‌بخش
└─ برنامهٔ خواب

تمرین بین دروس (FAB داشبورد)
├─ پشت میز
├─ چشم‌ها
├─ بدن
└─ تمرکز
```

### 4.3 محتوا، ابزارها و وب‌ویو

| شاخه | مقصد | نوع/منبع | وضعیت |
|---|---|---|---|
| `content-hub` | همه دسته‌های محتوا | `catalog.json` asset/ParsPack | ✅ |
| `content-category/{cat}` | یک دسته | ۷ دسته / ۸۰ آیتم | ✅ |
| `content-html/{id}` | نمایشگر HTML | ☁️ HMK1 → رمزگشایی فقط در RAM → WebView | ✅، pinch/pan/double-tap reset |
| `wellness?cat=` | هاب سلامت/یوگا/تنفس/ورزش | منوی Wellness + catalog | ✅ |
| `toolkit-general` | ابزار عمومی | HTMLهای bucket | ✅ |
| `toolkit-math` | ابزار ریاضی | HTMLهای bucket/Math WebView | ✅ |
| `lab-chemistry` | آزمایشگاه شیمی | HTML/کاتالوگ | ✅ |
| `lab-physics` | آزمایشگاه فیزیک | HTML/کاتالوگ | ✅ |
| `lab-biology` | آزمایشگاه زیست | HTML/کاتالوگ | ✅ |
| `tool/{toolId}` | ابزار جزئی | ID پویا از کاتالوگ | ✅ |
| `sketch-gallery` | مرجع سیاه‌قلم | asset/رسانه‌های کاتالوگ | ✅ |

### 4.4 یادگیری، آزمون، هنر، غذا و ورزش

| شاخه | درخت/صفحه | منبع و وضعیت |
|---|---|---|
| یادگیری | `learning` → `lesson/{id}` → `quiz?lessonId=` → `quizreview` | ↔ TablesDB → 💾 cache → BuiltInContent؛ ✅ |
| تعیین سطح | `placement` | 💾/↔؛ ✅ |
| نقشه راه | `roadmap?track=` | ↔ nodes/cached/built-in؛ ✅ |
| AI learning | `ailearning` → `aiassessment` | ☁️ در صورت فعال‌بودن سرویس + fallback؛ ✅ |
| هنر | `art` → `gallery` | prompt سرور/کش/داخلی، آثار کاربر 💾/↔؛ ✅ |
| غذا | `recipes` → `recipedetail/{id}` | TablesDB → cache → BuiltInContent؛ ✅ |
| ورزش | `exercise` → `exercise/{id}` | server/کش + `ExerciseCatalog` داخلی؛ تایمر گام‌به‌گام؛ ✅ |
| آب | `water` | 💾/↔؛ ✅ |
| نشان‌ها | `badges` | پیشرفت محلی/همگام؛ ✅ |

### 4.5 کتابخانه، مطالعه و رسانه

| مسیر | توضیح | منبع/وضعیت |
|---|---|---|
| `free-reading` | کتاب متنی و صوتی | HTML/صوت bucket و کش؛ ✅ |
| `library` | کتابخانه | داده داخلی/فهرست؛ ✅ |
| `audiobook` | صوت کتاب | ☁️/کش و پلیر داخلی؛ ✅ |
| `reading-corner` | قفسهٔ شخصی | 💾؛ ثبت عنوان کتاب؛ ✅، پلیر و هدف صفحات در توضیح خود صفحه «به‌زودی» است. |
| `downloads` | مدیریت دانلود | ☁️/کش؛ ✅ |
| `pdf` | جزوه/فایل شخصی | انتخاب و نگهداری محلی؛ ✅ |
| `book-node` | یک گره کتاب | PDF/HTML/صوت بسته به key؛ ✅ یا ⏳ در صورت نبود فایل محتوا |

---

## 5. رجیستری کامل مسیرهای دانش‌آموز

این فهرست همهٔ routeهای تعریف‌شده در `Screen.kt` را پوشش می‌دهد؛ موارد تکراریِ درخت بالا برای کنترل کامل بودن نیز اینجا هستند.

```text
home, study, study-home, chat, more, about, contact,
cycle, cycle-cal, cycle-log, cycle-today,
sleep-breath, background-music, mood, mindfulness, calm, calm-hub,
free-reading, journal, gratitude-journal, breath, routine,
safespace, diary, notebooks, safe-free-writing, secure-gallery, album, writing,
helplines, library, audiobook,
school, leave, class-plan, class-plan-shift, class-plan-cal, virtual-class,
subscription, tomorrow-prep, sleep-night,
health, awareness, academy, academy-coming-soon,
study-book/{bookCode}, study-teach/{packId}, weekly-schedule, meds, sleep-log,
reading-corner, appearance,
quiz?lessonId={lessonId}, quizreview,
study-lesson/{packId}, study-lesson-pdf/{packId}, pdf,
charts?bookCode={bookCode}, video-teach/{packId}, study-downloads,
health-progress, art, gallery,
learning, lesson/{id}, placement, roadmap?track={track}, ailearning, aiassessment,
recipes, recipedetail/{id}, exercise, exercise/{id}, water, call,
settings, user-profile, privacy, chatsettings, badges, lock, reminders, sync,
content-hub, content-category/{cat}, content-html/{id},
book-node/{key}/{title}?audio={audio}, wellness?cat={cat},
practice-group/{groupId}, practice/{itemId}, between-lessons, sketch-gallery,
cycle-training,
toolkit-general, lab-chemistry, lab-physics, lab-biology, toolkit-math, tool/{toolId}
```

### 5.1 نگاشت route به فایل Kotlin (مرجع پیاده‌سازی)

`ZahraNavHost.kt` نقطهٔ اتصال همهٔ routeهای بالا به Composable است. جدول زیر فایلِ پیاده‌سازی هر قالب/گره را می‌دهد؛ routeهای پارامتری در همان فایل برای **تمام شناسه‌های** پیوست ۹ به‌کار می‌روند.

| route/گره | Composable یا قالب | فایل Kotlin |
|---|---|---|
| `home` | HomeScreen | `ui/home/HomeScreen.kt` |
| `study`, `study-book/{bookCode}` | SchoolHubScreen، BookDetailScreen | `ui/hub/SchoolHubScreen.kt`؛ `ui/study/BookDetailScreen.kt` |
| `book-node/{...}` | BookNodeScreen | `ui/study/BookNodeScreen.kt` |
| `study-teach/{packId}` | LessonTeachScreen | `ui/study/LessonTeachScreen.kt` |
| `study-lesson/{packId}`, `study-lesson-pdf/{packId}` | LessonStudyScreen، LessonPdfScreen | `ui/study/LessonStudyScreens.kt`؛ `ui/study/LessonPdfScreen.kt` |
| `video-teach/{packId}` | VideoTeachScreen | `ui/study/VideoTeachScreen.kt` |
| `quiz`, `quizreview`, `charts` | QuizScreen، QuizReviewScreen، ProgressChartsScreen | `ui/study/StudyScreens.kt`؛ `ui/study/ProgressChartsScreen.kt` |
| `study-downloads`, `pdf` | DownloadsScreen، PdfUploadScreen | `ui/study/DownloadsScreen.kt`؛ `ui/study/PdfScreen.kt` |
| برنامه/مدرسه (`weekly-schedule`, `leave`, `school`, `class-plan*`, `virtual-class`, `tomorrow-prep`) | برنامه، مرخصی، شیفت و کلاس | `ui/hub/WeeklyScheduleScreen.kt`؛ `ui/study/LeaveScreen.kt`؛ `ClassPlanScreen.kt`؛ `VirtualClassScreen.kt`؛ `TomorrowPrepScreen.kt` |
| `academy`, `academy-coming-soon` | AcademyHubScreen، AcademySoonScreen | `ui/study/AcademyHubScreen.kt`؛ `AcademySoonScreen.kt` |
| `learning`, `lesson/{id}`, `placement`, `roadmap` | LearningHomeScreen، LessonScreen، PlacementTestScreen، RoadmapScreen | `ui/learning/LearningScreens.kt`؛ داده: `ui/content/CatalogRepository.kt` |
| `ailearning`, `aiassessment` | AiLearningHomeScreen، AiAssessmentScreen | `ui/ailearning/AiLearningScreens.kt` |
| `art`, `gallery` | DailyArtPromptScreen، ArtGalleryScreen | `ui/art/ArtScreens.kt` |
| `recipes`, `recipedetail/{id}` | RecipesScreen، RecipeDetailScreen | `ui/recipes/RecipeScreens.kt` |
| `exercise`, `exercise/{id}` | ExerciseScreen، ExerciseDetailScreen | `ui/exercise/ExerciseScreen.kt`؛ `ExerciseDetailScreen.kt` |
| `health`, `health-progress`, `water`, `meds`, `sleep-log`, `routine` | HealthHubScreen و ابزارهای سلامت | `ui/hub/HealthHubScreen.kt`؛ `HealthProgressScreen.kt`؛ `MedsScreen.kt`؛ `SleepLogScreen.kt`؛ `ui/water/WaterScreen.kt`؛ `ui/routine/RoutineScreen.kt` |
| `cycle*`, `mood`, `mindfulness` | صفحات چرخه و حال | `ui/cycle/CyclePages.kt`؛ `CycleScreens.kt`؛ `PeriodTrainingScreen.kt` |
| `awareness`, `calm*`, `breath`, `journal`, `gratitude-journal`, `background-music` | هاب و صفحات آرامش | `ui/hub/AwarenessHubScreen.kt`؛ `ui/calmdown/CalmHubScreen.kt`؛ `CalmScreens.kt`؛ `CalmWhispersScreen.kt` |
| `practice-group/{id}`, `practice/{id}`, `between-lessons` | قالب گروه/آیتم تمرین | `ui/wellness/PracticeScreens.kt`؛ منو: `WellnessMenu.kt` |
| `content-hub`, `content-category/{cat}`, `content-html/{id}` | قالب فهرست/وب‌ویو محتوا | `ui/content/ContentScreens.kt`؛ مدل: `ContentCatalog.kt` |
| `wellness`, `sketch-gallery` | WellnessScreen، SketchGalleryScreen | `ui/wellness/WellnessScreen.kt`؛ `SketchGalleryScreen.kt` |
| `toolkit-general`, `lab-*`, `toolkit-math`, `tool/{toolId}` | Tool/Lab WebView | `ui/tools/ToolScreens.kt`؛ `ToolRemote.kt`؛ `ui/study/MathHtmlWebView.kt` |
| `free-reading`, `library`, `audiobook`, `reading-corner` | مطالعهٔ آزاد/کتابخانه/قفسه | `ui/study/FreeReadingScreen.kt`؛ `LibraryScreen.kt`؛ `AudiobookScreen.kt`؛ `ui/hub/AwarenessHubScreen.kt` |
| `chat`, `chatsettings` | ChatScreen، ChatSettingsScreen | `ui/chatbot/ChatScreens.kt` |
| `more`, `about`, `contact` | MoreScreen، AboutScreen، ContactScreen | `ui/more/MoreScreen.kt`؛ `SupportScreens.kt` |
| `safespace` و محتوای امن | SafeSpaceScreen و Guard | `ui/safespace/SafeSpaceScreens.kt`؛ `SafeSpaceGuard.kt`؛ `DiaryScreens.kt`؛ `SecureMediaGallery.kt` |
| `settings`, `appearance`, `privacy`, `lock`, `reminders`, `sync` | تنظیمات | `ui/settings/SettingsScreens.kt`؛ `ui/appearance/AppearanceScreen.kt`؛ `ui/sync/SyncCenter.kt` |
| `user-profile`, `subscription`, `badges`, `call` | حساب/اشتراک/نشان/تماس | `ui/profile/UserProfileScreen.kt`؛ `ui/home/SubscriptionScreen.kt`؛ `ui/gamification/BadgesScreen.kt`؛ `ui/safespace/SafeSpaceScreens.kt` |

**routeهای ثبت‌شده اما بی‌صفحه:** سه object در `Screen.kt` route دارند ولی در `ZahraNavHost.kt` `composable` ندارند: `cycle` (هاب قدیمی چرخه)، `album` و `call`. بنابراین در نسخهٔ بررسی‌شده **unavailable** هستند؛ تنها مسیرهای قابل استفادهٔ چرخه `cycle-cal`، `cycle-log`، `cycle-today` و `cycle-training`اند. این سه نباید به‌عنوان صفحهٔ آماده فهرست شوند.

**مقصدهای حفاظتی:** `diary`، `notebooks`، `safe-free-writing`، `secure-gallery` و `writing` با `SafeContentGuard` محافظت می‌شوند؛ اگر قفل باز نباشد به `safespace` برمی‌گردند. `user-profile`، `subscription`، sync و هر داده Appwrite در نبود نشست، UI محلی/fallback نشان می‌دهد و عملیات سروری را انجام نمی‌دهد.

---

## 6. اجزای سیستمی، نه یک صفحهٔ مستقل

| جزء | مسئولیت | وضعیت |
|---|---|---|
| Bottom navigation | ۶ تب اصلی؛ RTL؛ `nav.bar` | ✅ |
| Top bar / BackHandler | در صفحهٔ سند تمام‌صفحه پنهان؛ بازگشت با دکمهٔ دستگاه | ✅ |
| PDF viewer | روشن، pinch، pan، double-tap reset | ✅ |
| HTML WebView | HMK1 در RAM، zoom/pan/reset، iframe bridge موسیقی | ✅ |
| Media player | صوت کتاب/آرامش/محتوای HTML | ✅ |
| دانلود و cache | PDF، صوت و ciphertext HTML به تفکیک نوع | ✅ |
| Font floater / Appearance | تغییر نقش تایپوگرافی و تم | ✅ |
| StateSync | همگام‌سازی امن stateهای شخصی در صورت ورود | ↔ |
| Update gate | بررسی نسخه و نصب رسمی APK | ✅ |

---

## 7. نقشهٔ APK ادمین 2.1

APK ادمین جداست و در تجربهٔ دانش‌آموز نشان داده نمی‌شود.

```text
ورود مدیر
├─ ایمیل/رمز Appwrite؛ فقط حساب با label=admin
├─ تنظیمات اتصال
│  ├─ endpoint / project / database / bucket / support-table
│  ├─ آزمون Appwrite و ParsPack
│  ├─ دریافت خودکار کلید HTML از app_state/html_media_key
│  └─ بررسی به‌روزرسانی Admin از manifest ParsPack
└─ مرکز مدیریت
   ├─ دید کلی: کاربران، پرداخت‌های معلق، استرداد، اشتراک فعال
   ├─ کاربران: پروفایل، پایه، اشتراک، دستگاه، نشست، بلوک/خروج/ریست
   ├─ پیام‌ها: suggestions/tickets، پاسخ دوطرفه، وضعیت، حذف با تأیید
   ├─ داده‌ها: TablesDB Excel-like grid
   │  ├─ انتخاب جدول، فیلتر همه ستون‌ها، sort، اسکرول افقی/عمودی
   │  ├─ ایجاد/ویرایش کنترل‌شده/حذف با تأیید
   │  └─ student_profiles و داده حساس: فقط‌خواندنی در grid
   ├─ رسانه
   │  ├─ ParsPack Explorer: پوشه، parent، جست‌وجو، آپلود/جایگزینی/حذف
   │  ├─ preview: تصویر/HTML/HMK1/متن/صوت/PDF
   │  └─ حجم پوشه (نه آمار ترافیک ساختگی)
   └─ بیشتر: پرداخت/اشتراک، اقساط، Functions، Auth، اتصال، خروج
```

| بخش ادمین | منبع/وضعیت |
|---|---|
| داده و حساب‌ها | ☁️ Appwrite؛ scopeهای لازم باید سمت Console/سرور داده شوند. |
| فایل‌های ParsPack | ☁️ SigV2، XML/JSON listing و fallback virtual-host/path-style؛ ✅. |
| preview HMK1 | کلید از ردیف Appwrite، کش Keystore، plaintext فقط در RAM؛ ✅. |
| Google login | عمداً نمایش داده نمی‌شود تا زمانی که provider در Appwrite فعال نشده است. |
| update Admin | manifest `apk/admin/hamyar-admin-latest.json`؛ ✅. |

---

## 8. مرجع فایل‌های اصلی برای نگهداری

| موضوع | فایل/مسیر مرجع |
|---|---|
| همه routeها و تب‌ها | `apps/hamyar-app/.../ui/navigation/Screen.kt` |
| اتصال route به Composable | `.../ui/navigation/ZahraNavHost.kt` |
| داشبورد | `.../ui/home/HomeScreen.kt` |
| مدرسه/آموزشگاه | `.../ui/hub/SchoolHubScreen.kt`, `HubCatalog.kt` |
| سلامت/آگاهی | `.../ui/hub/HealthHubScreen.kt`, `AwarenessHubScreen.kt`, `.../ui/wellness/WellnessMenu.kt` |
| کتاب‌ها | `assets/content/books-menu.json`, `.../ui/study/BooksMenu.kt`, `BookNodeScreen.kt` |
| HTML و نگاشت باکت | `assets/content/catalog.json`, `assets/content/server-map.json`, `.../ui/content/ContentScreens.kt` |
| منبع رسانه و رمزگشایی | `.../ui/study/HtmlCodec.kt`, `HmkWebViewClient.kt`, `StudyMedia.kt` |
| فونت‌ها | `.../ui/AppTypography.kt`, `.../ui/appearance/EmbeddedFonts.kt`, `TypeSlots.kt` |
| محتوا/کش/سرور | `.../ui/content/CatalogRepository.kt` |
| منوی بیشتر | `.../ui/more/MoreScreen.kt` |
| مرکز ادمین | `apps/hamyar-admin/.../ui/AdminCommandCenter.kt`, `AdminConsoleScreens.kt` |
```

---

## 9. فهرست دقیق اقلام پویای کاتالوگ، تمرین و کتاب

این پیوست نام‌های واقعی را از assetهای فعلی استخراج می‌کند. با این روش، «یک template برای همهٔ IDها» به اشتباه به‌عنوان صفحهٔ hard-coded معرفی نشده است. همهٔ موارد کاتالوگ در جدول اول یک template مشترک `content-html/{id}` دارند؛ وضعیت رسانه به وجود فایل با همان `key` در باکت وابسته است.

### 9.1 کاتالوگ HTML (۷ دسته / ۸۰ آیتم)

| دسته | شناسه | عنوان نمایش‌داده‌شده | نوع/مسیر | منبع و وضعیت |
|---|---|---|---|---|
| آموزشگاه | `amz-01` | سرعت خواندن | `html` → `content-html/amz-01` | ☁️ `Bucket/Html-files/آموزشگاه/01-speed-reading.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-02` | خوشنویسی | `html` → `content-html/amz-02` | ☁️ `Bucket/Html-files/آموزشگاه/02-handwriting.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-03` | تایپ لمسی | `html` → `content-html/amz-03` | ☁️ `Bucket/Html-files/آموزشگاه/03-touch-typing.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-04` | یادداشت‌برداری | `html` → `content-html/amz-04` | ☁️ `Bucket/Html-files/آموزشگاه/04-note-taking.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-05` | خلاصه‌نویسی | `html` → `content-html/amz-05` | ☁️ `Bucket/Html-files/آموزشگاه/05-summarizing.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-06` | اصول مناظره | `html` → `content-html/amz-06` | ☁️ `Bucket/Html-files/آموزشگاه/06-debate-principles.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-07` | مغالطه‌های منطقی | `html` → `content-html/amz-07` | ☁️ `Bucket/Html-files/آموزشگاه/07-logical-fallacies.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-08` | فن بیان | `html` → `content-html/amz-08` | ☁️ `Bucket/Html-files/آموزشگاه/08-public-speaking.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-09` | اضطراب سخنرانی | `html` → `content-html/amz-09` | ☁️ `Bucket/Html-files/آموزشگاه/09-public-speaking-anxiety.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-10` | ایمیل رسمی | `html` → `content-html/amz-10` | ☁️ `Bucket/Html-files/آموزشگاه/10-formal-email.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-11` | گوش دادن فعال | `html` → `content-html/amz-11` | ☁️ `Bucket/Html-files/آموزشگاه/11-active-listening.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-12` | محاسبات ذهنی | `html` → `content-html/amz-12` | ☁️ `Bucket/Html-files/آموزشگاه/12-mental-math.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-13` | تقویت حافظه | `html` → `content-html/amz-13` | ☁️ `Bucket/Html-files/آموزشگاه/13-memory-boost.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-14` | حل مسئله | `html` → `content-html/amz-14` | ☁️ `Bucket/Html-files/آموزشگاه/14-problem-solving.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-15` | مهارت روابط | `html` → `content-html/amz-15` | ☁️ `Bucket/Html-files/آموزشگاه/15-relationships.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-16` | خودآگاهی | `html` → `content-html/amz-16` | ☁️ `Bucket/Html-files/آموزشگاه/16-self-awareness.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-17` | تله‌های ذهنی | `html` → `content-html/amz-17` | ☁️ `Bucket/Html-files/آموزشگاه/17-mental-traps.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-18` | انعطاف‌پذیری | `html` → `content-html/amz-18` | ☁️ `Bucket/Html-files/آموزشگاه/18-resilience.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-19` | برنامه‌ریزی روزانه | `html` → `content-html/amz-19` | ☁️ `Bucket/Html-files/آموزشگاه/19-daily-planning.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-20` | ساخت عادت | `html` → `content-html/amz-20` | ☁️ `Bucket/Html-files/آموزشگاه/20-habit-building.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-21` | فضای مطالعه | `html` → `content-html/amz-21` | ☁️ `Bucket/Html-files/آموزشگاه/21-study-space.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-22` | ویندوز (مبانی) | `html` → `content-html/amz-22` | ☁️ `Bucket/Html-files/آموزشگاه/22-windows-basics.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-23` | سواد هوش مصنوعی | `html` → `content-html/amz-23` | ☁️ `Bucket/Html-files/آموزشگاه/23-ai-literacy.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-24` | جست‌وجوی هوشمند | `html` → `content-html/amz-24` | ☁️ `Bucket/Html-files/آموزشگاه/24-smart-search.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-25` | ایمنی در فضای مجازی | `html` → `content-html/amz-25` | ☁️ `Bucket/Html-files/آموزشگاه/25-online-safety.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-26` | اخبار جعلی | `html` → `content-html/amz-26` | ☁️ `Bucket/Html-files/آموزشگاه/26-fake-news.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-27` | کار تیمی | `html` → `content-html/amz-27` | ☁️ `Bucket/Html-files/آموزشگاه/27-teamwork.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-28` | مدیریت اختلاف با دوستان | `html` → `content-html/amz-28` | ☁️ `Bucket/Html-files/آموزشگاه/28-conflict-with-friends.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-29` | هنر نه گفتن | `html` → `content-html/amz-29` | ☁️ `Bucket/Html-files/آموزشگاه/29-saying-no.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-30` | مدیریت بودجه | `html` → `content-html/amz-30` | ☁️ `Bucket/Html-files/آموزشگاه/30-budgeting.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-31` | نیاز در برابر خواسته | `html` → `content-html/amz-31` | ☁️ `Bucket/Html-files/آموزشگاه/31-needs-vs-wants.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-32` | طوفان فکری | `html` → `content-html/amz-32` | ☁️ `Bucket/Html-files/آموزشگاه/32-brainstorming.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-33` | تفکر خلاق | `html` → `content-html/amz-33` | ☁️ `Bucket/Html-files/آموزشگاه/33-creative-thinking.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آموزشگاه | `amz-34` | تئوری موسیقی | `html` → `content-html/amz-34` | ☁️ `Bucket/Html-files/آموزشگاه/34-music-theory.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-01` | حالت کودک | `html` → `content-html/yga-01` | ☁️ `Bucket/Html-files/01 - حالت کودک.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-02` | گربه–گاو | `html` → `content-html/yga-02` | ☁️ `Bucket/Html-files/02 - گربه–گاو.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-03` | سگ سر پایین | `html` → `content-html/yga-03` | ☁️ `Bucket/Html-files/03 - سگ سر پایین.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-04` | کبرای ملایم | `html` → `content-html/yga-04` | ☁️ `Bucket/Html-files/04 - کبرای ملایم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-05` | جنگجوی یک | `html` → `content-html/yga-05` | ☁️ `Bucket/Html-files/05 - جنگجوی یک.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-06` | جنگجوی دو | `html` → `content-html/yga-06` | ☁️ `Bucket/Html-files/06 - جنگجوی دو.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-07` | درخت | `html` → `content-html/yga-07` | ☁️ `Bucket/Html-files/07 - درخت.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-08` | پل | `html` → `content-html/yga-08` | ☁️ `Bucket/Html-files/08 - پل.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-09` | چرخش نشسته | `html` → `content-html/yga-09` | ☁️ `Bucket/Html-files/09 - چرخش نشسته.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-10` | حالت کودک با زانوهای باز | `html` → `content-html/yga-10` | ☁️ `Bucket/Html-files/10 - حالت کودک با زانوهای باز.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-11` | کبوتر حمایت‌شده | `html` → `content-html/yga-11` | ☁️ `Bucket/Html-files/11 - کبوتر حمایت‌شده.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-12` | مثلث | `html` → `content-html/yga-12` | ☁️ `Bucket/Html-files/12 - مثلث.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-13` | استراحت به پشت | `html` → `content-html/yga-13` | ☁️ `Bucket/Html-files/13 - استراحت به پشت.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-14` | نیم‌قایق با زانوهای خم | `html` → `content-html/yga-14` | ☁️ `Bucket/Html-files/14 - نیم‌قایق با زانوهای خم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| یوگا | `yga-15` | خم‌شدن به جلو ایستاده | `html` → `content-html/yga-15` | ☁️ `Bucket/Html-files/15 - خم‌شدن به جلو ایستاده.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-01` | اسکوات آرام | `html` → `content-html/spo-01` | ☁️ `Bucket/Html-files/اسکوات آرام.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-02` | بالا بردن پاشنه‌ها | `html` → `content-html/spo-02` | ☁️ `Bucket/Html-files/بالا بردن پاشنه‌ها.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-03` | جامپینگ‌جک بدون پرش | `html` → `content-html/spo-03` | ☁️ `Bucket/Html-files/جامپینگ‌جک بدون پرش.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-04` | سوپرمنِ دست‌وپای مخالف | `html` → `content-html/spo-04` | ☁️ `Bucket/Html-files/سوپرمنِ دست‌وپای مخالف.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-05` | لانگز آرام | `html` → `content-html/spo-05` | ☁️ `Bucket/Html-files/لانگز آرام.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-06` | پلانک روی زانو | `html` → `content-html/spo-06` | ☁️ `Bucket/Html-files/پلانک روی زانو.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-07` | چرخش مچ دست و پا | `html` → `content-html/spo-07` | ☁️ `Bucket/Html-files/چرخش مچ دست و پا.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-08` | چرخش کوچک تنه | `html` → `content-html/spo-08` | ☁️ `Bucket/Html-files/چرخش کوچک تنه.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-09` | کرانچ ملایم | `html` → `content-html/spo-09` | ☁️ `Bucket/Html-files/کرانچ ملایم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-10` | کشش ساق به دیوار | `html` → `content-html/spo-10` | ☁️ `Bucket/Html-files/کشش ساق به دیوار.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-11` | کشش شانهٔ ضربدری | `html` → `content-html/spo-11` | ☁️ `Bucket/Html-files/کشش شانهٔ ضربدری.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-12` | کشش ملایم گردن | `html` → `content-html/spo-12` | ☁️ `Bucket/Html-files/کشش ملایم گردن.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-13` | کشش مچ و ساعد | `html` → `content-html/spo-13` | ☁️ `Bucket/Html-files/کشش مچ و ساعد.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-14` | کشش همسترینگ نشسته | `html` → `content-html/spo-14` | ☁️ `Bucket/Html-files/کشش همسترینگ نشسته.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| حرکات ورزشی | `spo-15` | کشش پروانه | `html` → `content-html/spo-15` | ☁️ `Bucket/Html-files/کشش پروانه.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-01` | تنفس آرام هنگام قاعدگی | `html` → `content-html/bre-01` | ☁️ `Bucket/Html-files/تمرینات تنفسی/تنفس آرام هنگام قاعدگی.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-02` | تنفس آرام پیش از خواب | `html` → `content-html/bre-02` | ☁️ `Bucket/Html-files/تمرینات تنفسی/تنفس آرام پیش از خواب.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-03` | تنفس بینی متناوب | `html` → `content-html/bre-03` | ☁️ `Bucket/Html-files/تمرینات تنفسی/تنفس بینی متناوب.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-04` | تنفس راحت | `html` → `content-html/bre-04` | ☁️ `Bucket/Html-files/تمرینات تنفسی/تنفس راحت.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-05` | تنفس شمارشی پیش از امتحان | `html` → `content-html/bre-05` | ☁️ `Bucket/Html-files/تمرینات تنفسی/تنفس شمارشی پیش از امتحان.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-06` | تنفس شکمی | `html` → `content-html/bre-06` | ☁️ `Bucket/Html-files/تمرینات تنفسی/تنفس شکمی.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-07` | تنفس شیر ملایم | `html` → `content-html/bre-07` | ☁️ `Bucket/Html-files/تمرینات تنفسی/تنفس شیر ملایم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| تمرینات تنفسی | `bre-08` | یک دقیقه توجه به نفس | `html` → `content-html/bre-08` | ☁️ `Bucket/Html-files/تمرینات تنفسی/یک دقیقه توجه به نفس.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آزمایشگاه | `lab-09-biology.html` | Hamyar e Man - آزمایشگاه زیست پایه نهم | `html` → `content-html/lab-09-biology.html` | ☁️ `Bucket/Html-files/آزمایشگاه 9/زیست/Hamyar e Man - آزمایشگاه زیست پایه نهم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آزمایشگاه | `lab-09-chemistry.html` | Hamyar e Man - آزمایشگاه شیمی پایه نهم | `html` → `content-html/lab-09-chemistry.html` | ☁️ `Bucket/Html-files/آزمایشگاه 9/شیمی/Hamyar e Man - آزمایشگاه شیمی پایه نهم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| آزمایشگاه | `lab-09-physics.html` | Hamyar e Man - آزمایشگاه فیزیک پایه نهم | `html` → `content-html/lab-09-physics.html` | ☁️ `Bucket/Html-files/آزمایشگاه 9/فیزیک/Hamyar e Man - آزمایشگاه فیزیک پایه نهم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| جعبه ابزار عمومی | `tool-calendar.html` | تقویم | `html` → `content-html/tool-calendar.html` | ☁️ `Bucket/Html-files/جعبه ابزار های عمومی/تقویم.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| جعبه ابزار عمومی | `tool-converter.html` | مبدل همه‌کاره مهندسی | `html` → `content-html/tool-converter.html` | ☁️ `Bucket/Html-files/جعبه ابزار های عمومی/مبدل همه‌کاره مهندسی.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| جعبه ابزار عمومی | `tool-dj120d.html` | DJ-120D Plus | `html` → `content-html/tool-dj120d.html` | ☁️ `Bucket/Html-files/جعبه ابزار های عمومی/DJ-120D Plus.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| جعبه ابزار ریاضی | `tool-casio991.html` | CASIO fx-991CW ClassWiz | `html` → `content-html/tool-casio991.html` | ☁️ `Bucket/Html-files/جعبه ابزار های ریاضی/CASIO fx-991CW ClassWiz.html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |
| جعبه ابزار ریاضی | `tool-ti-nspire.html` | TI-Nspire CX II-T CAS | `html` → `content-html/tool-ti-nspire.html` | ☁️ `Bucket/Html-files/جعبه ابزار های ریاضی/TI-Nspire CX II-T CAS .html`، HMK1 در RAM؛ ✅ در صورت انتشار فایل |

### 9.2 درخت کامل گروه‌ها و تمرین‌های WellnessMenu

**قالب مشترک:** group → `practice-group/{groupId}`؛ item → `practice/{itemId}`. نام و توضیح همهٔ کارتها زیر آمده است. در وضعیت‌ها دقت شود: itemهای صرفاً دارای متن/گام، به‌سبب شرط فعلی `PracticeItemScreen`، به‌جای اجرای راهنما پیام «به‌زودی» دارند؛ این یک مورد قابل پیگیری است، نه آماده‌بودن پنهان.

#### ذهن‌آگاهی (`awareness`)

- **تمرین‌های کوتاه حضور ذهن** — `mf-presence`؛ ۳ تا ۵ دقیقه؛ همین‌جا و همین حالا
  - اسکن بدن — `mf-body-scan`؛ ۳ تا ۵ دقیقه از سر تا پا؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - زمین‌گیری ۵-۴-۳-۲-۱ — `mf-grounding`؛ حواس پنج‌گانه برای برگشتن به الان؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - یک دقیقه نفس — `mf-one-minute`؛ بین کارها؛ فقط شصت ثانیه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **خودهیپنوز سالم** — `mf-hypnosis`؛ آرام‌سازی + تلقین مثبت + تجسم — برای تمرکز، امتحان، فکر و رابطه
  - **برای ترغیب به مطالعه** — `mf-hyp-study`؛ قبل از نشستن سر درس
    - من می‌تونم تمرکز کنم — `hyp-focus`؛ القای آرامش پیش از درس؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - تجسم کامل‌کردن یک فصل — `hyp-chapter`؛ دیدنِ تمام‌شدن تمرین؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - رهاسازی اهمال‌کاری — `hyp-procrastinate`؛ شروع ۲ دقیقه‌ای به‌جای منتظر ماندن؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - **برای اعتمادبه‌نفس امتحان** — `mf-hyp-exam`؛ اضطرابِ جلسه را کم کن
    - تجسم ورود آروم به جلسه — `hyp-enter`؛ راهرو، صندلی، برگه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - حافظه‌ام در دسترسمه — `hyp-memory`؛ تلقینِ بازیابی آرام؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - مدیریت اضطراب پیش از امتحان — `hyp-anxiety`؛ Test Anxiety — موج می‌آید و می‌رود؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - **برای افکار سالم و گفت‌وگوی درونی** — `mf-hyp-thoughts`؛ جایگزینی مهربان، نه شعار توخالی
    - بازنویسی فکر منفی — `hyp-reframe`؛ Cognitive Reframing واقع‌بینانه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - تلقین عزت‌نفس — `hyp-esteem`؛ برای نوجوان؛ بدون مقایسه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - رهاسازی مقایسه با دیگران — `hyp-compare`؛ مسیر خودت؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - **برای مسائل اجتماعی و روابط** — `mf-hyp-social`؛ گفت‌وگوی سخت، همدلی، رها کردن دلخوری
    - آرامش پیش از گفت‌وگوی سخت — `hyp-talk`؛ با دوست، خانواده یا معلم؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - دیدن از زاویه‌ی دیگری — `hyp-empathy`؛ تمرین همدلی؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - رهاسازی خشم کوچک — `hyp-anger`؛ دلخوری را زمین بگذار؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - **برای خواب بهتر** — `mf-hyp-sleep`؛ پل به آرام‌سازی پیش از خواب
    - آرام‌سازی پیش از خواب با تلقین — `hyp-sleep`؛ بدن سنگین، فکر سبک؛ ✅ مسیر مستقیم `Screen.SleepNight.route`
- **افکار سالم و گفت‌وگوی درونی مثبت** — `mf-thoughts`؛ تشخیص، بازنویسی، سه نقطه‌قوت
  - تشخیص فکر منفی خودکار — `th-auto`؛ اول ببین، بعد عوض کن؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - بازنویسی جمله‌ی منفی — `th-rewrite`؛ واقع‌بینانه، نه شعار؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - سه نقطه‌قوت امروز — `th-strengths`؛ شکرگزاریِ عملی؛ ✅ مسیر مستقیم `Screen.Journal.route`
- **آگاهی اجتماعی و مسائل اجتماعی** — `mf-social`؛ احساس، زاویه دید، مهربانی
  - نام‌گذاری احساس — `so-label`؛ Emotion Labeling؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - دیدن از دید دیگران — `so-perspective`؛ Perspective Taking؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - مسئله‌ی اجتماعی روز — `so-issue`؛ پرسش‌های تأملی، نه شعار؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - مهربانی با غریبه — `so-kindness`؛ یک لطف کوچک امروز — از پرسش روزانه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **دفترچه‌های من** — `mf-journal`؛ ثبت سریع شکرگزاری روزانه
  - دفترچه‌ی من — `jo-free`؛ حرف‌های بلندتر؛ فقط برای خودت؛ ✅ مسیر مستقیم `Screen.Journal.route`
- **تمرینات تمرکز و یادگیری** — `mf-learn`؛ پومودورو، مرور، فاینمن و شروع ۲ دقیقه‌ای
  - تکنیک پومودورو — `ln-pomo`؛ ۲۵ دقیقه کار، ۵ دقیقه استراحت؛ ↔ حرکت کاتالوگی `learn-pomodoro`
  - تکرار فاصله‌دار — `ln-spaced`؛ مرور با فاصله‌ی روبه‌رشد؛ ↔ حرکت کاتالوگی `learn-spaced-repetition`
  - روش فاینمن — `ln-feynman`؛ به زبان ساده توضیح بده؛ ↔ حرکت کاتالوگی `learn-feynman`
  - نقشه‌ی ذهنی — `ln-map`؛ موضوع در مرکز، شاخه‌ها دورش؛ ↔ حرکت کاتالوگی `learn-mind-map`
  - فعال‌سازی حافظه — `ln-recall`؛ بدون نگاه‌کردن بگو؛ ↔ حرکت کاتالوگی `learn-active-recall`
  - تکنیک ۲ دقیقه — `ln-two-min`؛ شروع کار سخت فقط با دو دقیقه‌ی اول؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - مرور فعال با سؤال‌سازی — `ln-quiz`؛ Self-Quizzing؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)

#### کسب آرامش (`calm-hub`)

- **تنفس آرام‌بخش** — `cl-breath`؛ ۴-۷-۸، جعبه‌ای، نادی، شیر
  - تنفس ۴-۷-۸ — `br-478`؛ دم ۴، نگه ۷، بازدم ۸؛ ↔ حرکت کاتالوگی `breath-4-7-8`
  - تنفس جعبه‌ای — `br-box`؛ ۴-۴-۴-۴؛ ↔ حرکت کاتالوگی `breath-box`
  - تنفس بینی متناوب — `br-nadi`؛ Nadi Shodhana؛ ↔ حرکت کاتالوگی `breath-nadi`
  - تنفس شیر — `br-lion`؛ رهاسازی فک و تنش؛ ↔ حرکت کاتالوگی `breath-lion`
- **آرام‌سازی عضلانی پیش‌رونده** — `cl-pmr`؛ سفت کن، رها کن — Progressive Muscle Relaxation
  - نسخه‌ی کامل سر تا پا — `pmr-full`؛ ۱۰ تا ۱۲ دقیقه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - نسخه‌ی کوتاه دست و شانه — `pmr-short`؛ وسط روز، پشت میز؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **سفر ذهنی** — `cl-journey`؛ روایت با شروع، میانه و پایان
  - جنگل آرام — `jn-forest`؛ باد، برگ، پرنده؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - ساحل و امواج — `jn-beach`؛ دریا، شن گرم؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - کلبه‌ی کوهستانی برفی — `jn-cabin`؛ سکوت، آتش، پتو؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - باغ مخفی — `jn-garden`؛ رایحه‌ی گل، صدای آب؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - قایق روی رودخانه‌ی آرام — `jn-boat`؛ جریان تو را می‌برد؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - پیاده‌روی زیر باران بهاری — `jn-rain`؛ بوی خاک، قطره‌های نرم؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **تصویرسازی ذهنی** — `cl-visual`؛ هدف‌محور؛ برای حالت یا نتیجه
  - مه رنگی آرامش — `vz-relax`؛ Relaxation Imagery؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - تصویرسازی عملکردی — `vz-perf`؛ موفقیت در امتحان یا ارائه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - تصویرسازی مرحله‌به‌مرحله — `vz-process`؛ Process Imagery — قدم‌های درس؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - تصویرسازی حالت‌نهایی — `vz-end`؛ خودِ رسیده‌به هدف؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - حباب محافظ — `vz-bubble`؛ برای اضطراب اجتماعی؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - جعبه‌ی نگرانی — `vz-box`؛ Worry Box — بگذار برای بعد؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **قصه‌ی شب** — `cl-story`؛ روایت کامل برای خواب؛ صدا هم در آرام‌سازی پیش از خواب
  - قصه‌های طبیعت — `st-nature`؛ جنگل، دریا، کوه، دشت؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - افسانه‌ی آرام — `st-myth`؛ روایت کوتاه ایرانی؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - قصه‌ی پشتکار آرام — `st-inspire`؛ غلبه بر سختی، بدون فریاد؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - خانه‌ی مادربزرگ — `st-nostalgia`؛ نوستالژی آرام؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - شهر ابری — `st-cloud`؛ سفر تخیلی آرام؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - روزی که تمام شد — `st-school`؛ تم مدرسه؛ فردا شروع تازه؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **صداهای آرامش‌بخش** — `cl-sounds`؛ صدای دلخواهت را انتخاب کن و فضای آرام خودت را بساز.
  - نجواهای آرام‌بخش طبیعت — `soundscape-full`؛ موسیقی کامل از سرور، با تایمر خواب.؛ ✅ مسیر مستقیم `Screen.BackgroundMusic.route`
- **آرام‌سازی پیش از خواب** — `cl-bedtime`؛ پل به قصه‌ی شب و تنفس خواب
  - آرامش قبل خواب — `bd-night`؛ صوت، قصه، تنفس؛ ✅ مسیر مستقیم `Screen.SleepNight.route`
  - تنفس قبل خواب — `bd-breath`؛ ۴ دم، ۶ بازدم؛ ↔ حرکت کاتالوگی `breath-bedtime`

#### تمرین بین دروس (`between-lessons`)

- **حرکات کشش سریع پشت میز** — `bl-desk`؛ گردن، شانه، مچ — ۱ تا ۲ دقیقه
  - کشش گردن ۴ جهت — `bl-neck`؛ قبلاً در حرکات سلامتی؛ ↔ حرکت کاتالوگی `ex-neck-4way`
  - کشش شانه ضربدری — `bl-shoulder`؛ قبلاً در حرکات سلامتی؛ ↔ حرکت کاتالوگی `ex-shoulder-cross`
  - چرخش مچ دست و پا — `bl-wrist`؛ قبلاً در حرکات سلامتی؛ ↔ حرکت کاتالوگی `ex-wrist-ankle`
- **استراحت چشم** — `bl-eyes`؛ برای خستگی صفحه
  - قانون ۲۰-۲۰-۲۰ — `bl-202020`؛ هر ۲۰ دقیقه، ۲۰ ثانیه به فاصله‌ی حدود ۶ متر؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - فوکوس نزدیک/دور — `bl-focus`؛ نوک انگشت و افق؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **فعال‌سازی سریع بدن** — `bl-body`؛ ایستادن، جا به‌جا شدن، تکان‌دادن
  - ایستادن و کشش کامل — `bl-stand`؛ دست‌ها بالا، قد بکش؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - راه‌رفتن درجا / پله‌ی کوتاه — `bl-walk`؛ خون‌رسانی بدون عرق؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - تکان دادن دست و پا — `bl-shake`؛ Shake it off؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **بازگشت به تمرکز** — `bl-focus`؛ بعد از استراحت، قبل از صفحه‌ی بعد
  - تنفس شمارشی ۴-۴-۴ — `bl-box`؛ قبلاً در تنفس سلامتی؛ ↔ حرکت کاتالوگی `breath-counting`
  - یک جمله‌ی تمرکز — `bl-sentence`؛ الان فقط همینو انجام بده؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)

#### سلامتی (`health`) — شاخه‌های PracticeGroup

- **چرخه ماهانه** — `hl-cycle`؛ تقویم، علائم، تنفس درد و تمرین ملایم
  - تقویم ماهانه با فازبندی — `cy-cal`؛ پیش‌قاعدگی / قاعدگی / پس از قاعدگی / میانه؛ ✅ مسیر مستقیم `Screen.CycleCal.route`
  - علائم و یادداشت روزانه — `cy-log`؛ درد، خلق، جریان، سردرد، نفخ؛ ✅ مسیر مستقیم `Screen.CycleLog.route`
  - امروز بدنت چی می‌خواد — `cy-today`؛ راهنما بر اساس فاز همان روز؛ ✅ مسیر مستقیم `Screen.CycleToday.route`
  - تنفس برای کرامپ و کنترل درد — `cy-pain`؛ تنفس شکمی آرام‌بخش؛ ✅ زیرگروه `practice-group/pd-breath`
  - **یوگای دوره‌ی قاعدگی** — `pd-yoga`؛ ملایم؛ اگر درد زیاد شد بایست
    - گربه-گاو — `pd-catcow`؛ تمرین تعاملی؛ ✅ مسیر مستقیم `Screen.ContentHtml.of("yga-02")`
    - حالت کودک — `pd-child`؛ تمرین تعاملی؛ ✅ مسیر مستقیم `Screen.ContentHtml.of("yga-01")`
    - پروانه‌ی خوابیده — `pd-butterfly`؛ Reclined Butterfly؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - پیچش خوابیده — `pd-twist`؛ Supine Twist؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - پا به دیوار — `pd-legs`؛ Legs-Up-the-Wall؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - **کشش‌های هدفمند** — `pd-stretch`؛ کمر، لگن، گرما
    - کشش کمر و لگن — `pd-hips`؛ ملایم، بدون فشار؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - کمپرس گرم + وضعیت تسکین — `pd-heat`؛ گرما + دراز کشیدن؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
  - **راهنمای تغذیه‌ی هر فاز** — `pd-food`؛ آهن، منیزیم، آب — نه رژیم سخت
    - فاز قاعدگی — `pd-food-period`؛ آهن و گرما؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - بعد از قاعدگی — `pd-food-after`؛ انرژی آرام برمی‌گردد؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - میانه‌ی چرخه — `pd-food-mid`؛ تمرکز بهتر؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
    - پیش‌قاعدگی — `pd-food-pms`؛ منیزیم و خواب؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **آب و تغذیه** — `hl-nutrition`؛ تمرکز و انرژی درس
  - یادآور نوشیدن آب — `hl-water`؛ لیوان‌های امروز؛ ✅ مسیر مستقیم `Screen.Water.route`
  - تغذیه‌ی سالم برای تمرکز — `hl-focus-food`؛ راهنمای ساده، نه رژیم؛ ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد)
- **خواب** — `hl-sleep`؛ ثبت، قصه، بشنو و بخواب، تنفس پیش از خواب
  - ثبت خواب و رشته — `hl-sleep-log`؛ ساعت خواب و بیداری؛ ✅ مسیر مستقیم `Screen.SleepLog.route`
  - قصه‌ی شب — `hl-sleep-story`؛ روایت برای خواب؛ ✅ زیرگروه `practice-group/cl-story`
  - بشنو و بخواب — `hl-sleep-listen`؛ صوت یکنواخت شب؛ ✅ مسیر مستقیم `Screen.SleepNight.route`
  - تنفس پیش از خواب — `hl-sleep-breath`؛ شکمی و ۴-۷-۸ برای خواب، نه بیداری؛ ✅ مسیر مستقیم `Screen.SleepBreath.route`

#### گروه‌های قابل دسترسی غیرمستقیم/بدون root مشخص در همین فایل

- **تنفس آرام‌بخش برای کرامپ** — `pd-breath`؛ شکمی و ۴-۷-۸
  - تنفس شکمی — `pd-diaph`؛ قبلاً موجود؛ ↔ حرکت کاتالوگی `breath-diaphragm`
  - تنفس ۴-۷-۸ — `pd-478`؛ قبلاً موجود؛ ↔ حرکت کاتالوگی `breath-4-7-8`

### 9.3 جدول کنترل ۱۲۰ گروه/تمرینِ متصل به منوهای Wellness

این جدول برای جست‌وجوی سریع ID است؛ رابطهٔ والد/فرزند را در درخت ۹.۲ ببینید. کاتالوگِ جداگانهٔ `SkillsCatalog` به‌دلیل نداشتن root UI در نسخهٔ بررسی‌شده در این جدول نیامده و وضعیتش در بخش ۱۰ شفاف شده است.

| نوع | ID | عنوان | توضیح نمایش‌داده‌شده | وضعیت/مقصد |
|---|---|---|---|---|
| گروه | `mf-presence` | تمرین‌های کوتاه حضور ذهن | ۳ تا ۵ دقیقه؛ همین‌جا و همین حالا | ✅ `practice-group/mf-presence` |
| گروه | `mf-hypnosis` | خودهیپنوز سالم | آرام‌سازی + تلقین مثبت + تجسم — برای تمرکز، امتحان، فکر و رابطه | ✅ `practice-group/mf-hypnosis` |
| گروه | `mf-hyp-study` | برای ترغیب به مطالعه | قبل از نشستن سر درس | ✅ `practice-group/mf-hyp-study` |
| گروه | `mf-hyp-exam` | برای اعتمادبه‌نفس امتحان | اضطرابِ جلسه را کم کن | ✅ `practice-group/mf-hyp-exam` |
| گروه | `mf-hyp-thoughts` | برای افکار سالم و گفت‌وگوی درونی | جایگزینی مهربان، نه شعار توخالی | ✅ `practice-group/mf-hyp-thoughts` |
| گروه | `mf-hyp-social` | برای مسائل اجتماعی و روابط | گفت‌وگوی سخت، همدلی، رها کردن دلخوری | ✅ `practice-group/mf-hyp-social` |
| گروه | `mf-hyp-sleep` | برای خواب بهتر | پل به آرام‌سازی پیش از خواب | ✅ `practice-group/mf-hyp-sleep` |
| گروه | `mf-thoughts` | افکار سالم و گفت‌وگوی درونی مثبت | تشخیص، بازنویسی، سه نقطه‌قوت | ✅ `practice-group/mf-thoughts` |
| گروه | `mf-social` | آگاهی اجتماعی و مسائل اجتماعی | احساس، زاویه دید، مهربانی | ✅ `practice-group/mf-social` |
| گروه | `mf-journal` | دفترچه‌های من | ثبت سریع شکرگزاری روزانه | ✅ `practice-group/mf-journal` |
| گروه | `mf-learn` | تمرینات تمرکز و یادگیری | پومودورو، مرور، فاینمن و شروع ۲ دقیقه‌ای | ✅ `practice-group/mf-learn` |
| گروه | `cl-breath` | تنفس آرام‌بخش | ۴-۷-۸، جعبه‌ای، نادی، شیر | ✅ `practice-group/cl-breath` |
| گروه | `cl-pmr` | آرام‌سازی عضلانی پیش‌رونده | سفت کن، رها کن — Progressive Muscle Relaxation | ✅ `practice-group/cl-pmr` |
| گروه | `cl-journey` | سفر ذهنی | روایت با شروع، میانه و پایان | ✅ `practice-group/cl-journey` |
| گروه | `cl-visual` | تصویرسازی ذهنی | هدف‌محور؛ برای حالت یا نتیجه | ✅ `practice-group/cl-visual` |
| گروه | `cl-story` | قصه‌ی شب | روایت کامل برای خواب؛ صدا هم در آرام‌سازی پیش از خواب | ✅ `practice-group/cl-story` |
| گروه | `cl-sounds` | صداهای آرامش‌بخش | صدای دلخواهت را انتخاب کن و فضای آرام خودت را بساز. | ✅ `practice-group/cl-sounds` |
| گروه | `cl-bedtime` | آرام‌سازی پیش از خواب | پل به قصه‌ی شب و تنفس خواب | ✅ `practice-group/cl-bedtime` |
| گروه | `bl-desk` | حرکات کشش سریع پشت میز | گردن، شانه، مچ — ۱ تا ۲ دقیقه | ✅ `practice-group/bl-desk` |
| گروه | `bl-eyes` | استراحت چشم | برای خستگی صفحه | ✅ `practice-group/bl-eyes` |
| گروه | `bl-body` | فعال‌سازی سریع بدن | ایستادن، جا به‌جا شدن، تکان‌دادن | ✅ `practice-group/bl-body` |
| گروه | `bl-focus` | بازگشت به تمرکز | بعد از استراحت، قبل از صفحه‌ی بعد | ✅ `practice-group/bl-focus` |
| گروه | `pd-yoga` | یوگای دوره‌ی قاعدگی | ملایم؛ اگر درد زیاد شد بایست | ✅ `practice-group/pd-yoga` |
| گروه | `pd-breath` | تنفس آرام‌بخش برای کرامپ | شکمی و ۴-۷-۸ | ✅ `practice-group/pd-breath` |
| گروه | `pd-stretch` | کشش‌های هدفمند | کمر، لگن، گرما | ✅ `practice-group/pd-stretch` |
| گروه | `pd-food` | راهنمای تغذیه‌ی هر فاز | آهن، منیزیم، آب — نه رژیم سخت | ✅ `practice-group/pd-food` |
| گروه | `hl-cycle` | چرخه ماهانه | تقویم، علائم، تنفس درد و تمرین ملایم | ✅ `practice-group/hl-cycle` |
| گروه | `hl-nutrition` | آب و تغذیه | تمرکز و انرژی درس | ✅ `practice-group/hl-nutrition` |
| گروه | `hl-sleep` | خواب | ثبت، قصه، بشنو و بخواب، تنفس پیش از خواب | ✅ `practice-group/hl-sleep` |
| تمرین | `mf-body-scan` | اسکن بدن | ۳ تا ۵ دقیقه از سر تا پا | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `mf-grounding` | زمین‌گیری ۵-۴-۳-۲-۱ | حواس پنج‌گانه برای برگشتن به الان | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `mf-one-minute` | یک دقیقه نفس | بین کارها؛ فقط شصت ثانیه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-focus` | من می‌تونم تمرکز کنم | القای آرامش پیش از درس | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-chapter` | تجسم کامل‌کردن یک فصل | دیدنِ تمام‌شدن تمرین | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-procrastinate` | رهاسازی اهمال‌کاری | شروع ۲ دقیقه‌ای به‌جای منتظر ماندن | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-enter` | تجسم ورود آروم به جلسه | راهرو، صندلی، برگه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-memory` | حافظه‌ام در دسترسمه | تلقینِ بازیابی آرام | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-anxiety` | مدیریت اضطراب پیش از امتحان | Test Anxiety — موج می‌آید و می‌رود | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-reframe` | بازنویسی فکر منفی | Cognitive Reframing واقع‌بینانه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-esteem` | تلقین عزت‌نفس | برای نوجوان؛ بدون مقایسه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-compare` | رهاسازی مقایسه با دیگران | مسیر خودت | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-talk` | آرامش پیش از گفت‌وگوی سخت | با دوست، خانواده یا معلم | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-empathy` | دیدن از زاویه‌ی دیگری | تمرین همدلی | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-anger` | رهاسازی خشم کوچک | دلخوری را زمین بگذار | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hyp-sleep` | آرام‌سازی پیش از خواب با تلقین | بدن سنگین، فکر سبک | ✅ مسیر مستقیم `Screen.SleepNight.route` |
| تمرین | `th-auto` | تشخیص فکر منفی خودکار | اول ببین، بعد عوض کن | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `th-rewrite` | بازنویسی جمله‌ی منفی | واقع‌بینانه، نه شعار | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `th-strengths` | سه نقطه‌قوت امروز | شکرگزاریِ عملی | ✅ مسیر مستقیم `Screen.Journal.route` |
| تمرین | `so-label` | نام‌گذاری احساس | Emotion Labeling | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `so-perspective` | دیدن از دید دیگران | Perspective Taking | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `so-issue` | مسئله‌ی اجتماعی روز | پرسش‌های تأملی، نه شعار | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `so-kindness` | مهربانی با غریبه | یک لطف کوچک امروز — از پرسش روزانه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `jo-free` | دفترچه‌ی من | حرف‌های بلندتر؛ فقط برای خودت | ✅ مسیر مستقیم `Screen.Journal.route` |
| تمرین | `ln-pomo` | تکنیک پومودورو | ۲۵ دقیقه کار، ۵ دقیقه استراحت | ↔ حرکت کاتالوگی `learn-pomodoro` |
| تمرین | `ln-spaced` | تکرار فاصله‌دار | مرور با فاصله‌ی روبه‌رشد | ↔ حرکت کاتالوگی `learn-spaced-repetition` |
| تمرین | `ln-feynman` | روش فاینمن | به زبان ساده توضیح بده | ↔ حرکت کاتالوگی `learn-feynman` |
| تمرین | `ln-map` | نقشه‌ی ذهنی | موضوع در مرکز، شاخه‌ها دورش | ↔ حرکت کاتالوگی `learn-mind-map` |
| تمرین | `ln-recall` | فعال‌سازی حافظه | بدون نگاه‌کردن بگو | ↔ حرکت کاتالوگی `learn-active-recall` |
| تمرین | `ln-two-min` | تکنیک ۲ دقیقه | شروع کار سخت فقط با دو دقیقه‌ی اول | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `ln-quiz` | مرور فعال با سؤال‌سازی | Self-Quizzing | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `br-478` | تنفس ۴-۷-۸ | دم ۴، نگه ۷، بازدم ۸ | ↔ حرکت کاتالوگی `breath-4-7-8` |
| تمرین | `br-box` | تنفس جعبه‌ای | ۴-۴-۴-۴ | ↔ حرکت کاتالوگی `breath-box` |
| تمرین | `br-nadi` | تنفس بینی متناوب | Nadi Shodhana | ↔ حرکت کاتالوگی `breath-nadi` |
| تمرین | `br-lion` | تنفس شیر | رهاسازی فک و تنش | ↔ حرکت کاتالوگی `breath-lion` |
| تمرین | `pmr-full` | نسخه‌ی کامل سر تا پا | ۱۰ تا ۱۲ دقیقه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pmr-short` | نسخه‌ی کوتاه دست و شانه | وسط روز، پشت میز | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `jn-forest` | جنگل آرام | باد، برگ، پرنده | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `jn-beach` | ساحل و امواج | دریا، شن گرم | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `jn-cabin` | کلبه‌ی کوهستانی برفی | سکوت، آتش، پتو | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `jn-garden` | باغ مخفی | رایحه‌ی گل، صدای آب | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `jn-boat` | قایق روی رودخانه‌ی آرام | جریان تو را می‌برد | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `jn-rain` | پیاده‌روی زیر باران بهاری | بوی خاک، قطره‌های نرم | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `vz-relax` | مه رنگی آرامش | Relaxation Imagery | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `vz-perf` | تصویرسازی عملکردی | موفقیت در امتحان یا ارائه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `vz-process` | تصویرسازی مرحله‌به‌مرحله | Process Imagery — قدم‌های درس | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `vz-end` | تصویرسازی حالت‌نهایی | خودِ رسیده‌به هدف | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `vz-bubble` | حباب محافظ | برای اضطراب اجتماعی | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `vz-box` | جعبه‌ی نگرانی | Worry Box — بگذار برای بعد | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `st-nature` | قصه‌های طبیعت | جنگل، دریا، کوه، دشت | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `st-myth` | افسانه‌ی آرام | روایت کوتاه ایرانی | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `st-inspire` | قصه‌ی پشتکار آرام | غلبه بر سختی، بدون فریاد | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `st-nostalgia` | خانه‌ی مادربزرگ | نوستالژی آرام | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `st-cloud` | شهر ابری | سفر تخیلی آرام | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `st-school` | روزی که تمام شد | تم مدرسه؛ فردا شروع تازه | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `soundscape-full` | نجواهای آرام‌بخش طبیعت | موسیقی کامل از سرور، با تایمر خواب. | ✅ مسیر مستقیم `Screen.BackgroundMusic.route` |
| تمرین | `bd-night` | آرامش قبل خواب | صوت، قصه، تنفس | ✅ مسیر مستقیم `Screen.SleepNight.route` |
| تمرین | `bd-breath` | تنفس قبل خواب | ۴ دم، ۶ بازدم | ↔ حرکت کاتالوگی `breath-bedtime` |
| تمرین | `bl-neck` | کشش گردن ۴ جهت | قبلاً در حرکات سلامتی | ↔ حرکت کاتالوگی `ex-neck-4way` |
| تمرین | `bl-shoulder` | کشش شانه ضربدری | قبلاً در حرکات سلامتی | ↔ حرکت کاتالوگی `ex-shoulder-cross` |
| تمرین | `bl-wrist` | چرخش مچ دست و پا | قبلاً در حرکات سلامتی | ↔ حرکت کاتالوگی `ex-wrist-ankle` |
| تمرین | `bl-202020` | قانون ۲۰-۲۰-۲۰ | هر ۲۰ دقیقه، ۲۰ ثانیه به فاصله‌ی حدود ۶ متر | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `bl-focus` | فوکوس نزدیک/دور | نوک انگشت و افق | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `bl-stand` | ایستادن و کشش کامل | دست‌ها بالا، قد بکش | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `bl-walk` | راه‌رفتن درجا / پله‌ی کوتاه | خون‌رسانی بدون عرق | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `bl-shake` | تکان دادن دست و پا | Shake it off | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `bl-box` | تنفس شمارشی ۴-۴-۴ | قبلاً در تنفس سلامتی | ↔ حرکت کاتالوگی `breath-counting` |
| تمرین | `bl-sentence` | یک جمله‌ی تمرکز | الان فقط همینو انجام بده | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-catcow` | گربه-گاو | تمرین تعاملی | ✅ مسیر مستقیم `Screen.ContentHtml.of("yga-02")` |
| تمرین | `pd-child` | حالت کودک | تمرین تعاملی | ✅ مسیر مستقیم `Screen.ContentHtml.of("yga-01")` |
| تمرین | `pd-butterfly` | پروانه‌ی خوابیده | Reclined Butterfly | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-twist` | پیچش خوابیده | Supine Twist | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-legs` | پا به دیوار | Legs-Up-the-Wall | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-diaph` | تنفس شکمی | قبلاً موجود | ↔ حرکت کاتالوگی `breath-diaphragm` |
| تمرین | `pd-478` | تنفس ۴-۷-۸ | قبلاً موجود | ↔ حرکت کاتالوگی `breath-4-7-8` |
| تمرین | `pd-hips` | کشش کمر و لگن | ملایم، بدون فشار | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-heat` | کمپرس گرم + وضعیت تسکین | گرما + دراز کشیدن | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-food-period` | فاز قاعدگی | آهن و گرما | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-food-after` | بعد از قاعدگی | انرژی آرام برمی‌گردد | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-food-mid` | میانه‌ی چرخه | تمرکز بهتر | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `pd-food-pms` | پیش‌قاعدگی | منیزیم و خواب | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `cy-cal` | تقویم ماهانه با فازبندی | پیش‌قاعدگی / قاعدگی / پس از قاعدگی / میانه | ✅ مسیر مستقیم `Screen.CycleCal.route` |
| تمرین | `cy-log` | علائم و یادداشت روزانه | درد، خلق، جریان، سردرد، نفخ | ✅ مسیر مستقیم `Screen.CycleLog.route` |
| تمرین | `cy-today` | امروز بدنت چی می‌خواد | راهنما بر اساس فاز همان روز | ✅ مسیر مستقیم `Screen.CycleToday.route` |
| تمرین | `cy-pain` | تنفس برای کرامپ و کنترل درد | تنفس شکمی آرام‌بخش | ✅ زیرگروه `practice-group/pd-breath` |
| تمرین | `hl-water` | یادآور نوشیدن آب | لیوان‌های امروز | ✅ مسیر مستقیم `Screen.Water.route` |
| تمرین | `hl-focus-food` | تغذیه‌ی سالم برای تمرکز | راهنمای ساده، نه رژیم | ⏳ در UI فعلی placeholder «به‌زودی» (متن/گام محلی وجود دارد) |
| تمرین | `hl-sleep-log` | ثبت خواب و رشته | ساعت خواب و بیداری | ✅ مسیر مستقیم `Screen.SleepLog.route` |
| تمرین | `hl-sleep-story` | قصه‌ی شب | روایت برای خواب | ✅ زیرگروه `practice-group/cl-story` |
| تمرین | `hl-sleep-listen` | بشنو و بخواب | صوت یکنواخت شب | ✅ مسیر مستقیم `Screen.SleepNight.route` |
| تمرین | `hl-sleep-breath` | تنفس پیش از خواب | شکمی و ۴-۷-۸ برای خواب، نه بیداری | ✅ مسیر مستقیم `Screen.SleepBreath.route` |

### 9.4 درخت کامل منوی کتاب‌ها (۱۴ کتاب)

**قالب مشترک هر سطر:** `study-book/{bookCode}` → آکاردئونِ item. برای هر item، اگر `key`/`ready=true` داشته باشد `book-node` رسانه را باز می‌کند؛ اگر `key` خالی باشد خود item یا tab به‌درستی «در دست تولید» است. این یک وضعیت محتوایی است، نه نبودن صفحهٔ Kotlin. Tabها نیز فقط هنگامی active هستند که key آن‌ها منتشر شده باشد.

<details>
<summary><strong>عربی نهم</strong> — کد <code>909</code>، 12 گره، 2 گرهٔ مستقیم آماده، 60 tab</summary>

- **فهرست: الفهرس** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-arabic/g9-arabic-index.pdf`
- **درس اوّل: مراجعة دروس الصفّین السابع و الثامن؛ أهلاً و سهلاً بالخریف** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس دوم: اَلْعُبورُ الْمِنْ** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس سوم: جِسْرُ اَلصَّداقَة** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس چهارم: اَلصَّبْرُ مِفتاحُ اَلْفَرَج** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس پنجم: اَلرَّجاء** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس ششم: تَغییرُ اَلْحَیاة** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس هفتم: ثَمَرَةُ اَلْجِدّ** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس هشتم: حِوارٌ بَیْنَ اَلزّائِرِ وَ سائِقِ سَیّارَةِ اَلْأُجْرَة** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس نهم: نُصوصٌ حَوْلَ اَلصِّحَّة** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **درس دهم: رِسالَةُ اَلشَّهیدِ سُلَیمانی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، تمرینات کتابی ⏳، نکات گرامری ⏳، خلاصه درس و نکات امتحانی ⏳، نمونه سوالات جامع درس ⏳، کتاب درسی ✅
- **پیام پایانی: فی أمانِ الله** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-arabic/g9-arabic-p11.pdf`

</details>

<details>
<summary><strong>فرهنگ و هنر نهم</strong> — کد <code>912</code>، 8 گره، 8 گرهٔ مستقیم آماده، 0 tab</summary>

- **تابلوی هنری: نقاشی رنگ‌وروغن ـ استاد کیخسرو خروش** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-p01.pdf`
- **فهرست مطالب** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-index.pdf`
- **بخش اوّل: هنرهای تجسّمی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-sec1.pdf`
  - زیرگره: **فصل اوّل: طرّاحی** — `container`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-f01-1.pdf`
  - زیرگره: **فصل دوم: نگاشتار (گرافیک)** — `container`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-f02-1.pdf`
  - زیرگره: **فصل سوم: عکّاسی** — `container`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-f03-1.pdf`
- **بخش دوم: خوشنویسی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-sec2.pdf`
  - زیرگره: **بخش دوم درس اوّل: نحوهٔ تراشیدن قلم ــ قالب‌های اجرایی خوشنویسی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش دوم درس دوم: معرفی خط نستعلیق و مشق سطرنویسی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش دوم درس سوم: معرفی خط شکستهٔ نستعلیق و مشق دو سطری‌نویسی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش دوم درس چهارم: گرایش‌های هنری با بنیان خوشنویسی و مشق دفتری‌نویسی** — `lesson`؛ ⏳؛ key: `—`
- **بخش سوم: هنرهای سنتی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-sec3.pdf`
  - زیرگره: **فصل اوّل: طرّاحی نقوش تزیینی** — `container`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-f01-2.pdf`
  - زیرگره: **فصل دوم: هنرهای زیرالکی** — `container`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-f02-2.pdf`
  - زیرگره: **فصل سوم: سوزن‌دوزی‌های سنتی ایران** — `container`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-f03-2.pdf`
- **بخش چهارم: هنرهای آوایی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-sec4.pdf`
  - زیرگره: **بخش چهارم درس اوّل: آواهای قومی (مردمی)** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش چهارم درس دوم: ردیف دستگاهی ایران** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش چهارم درس سوم: آواهای دینی ایران** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش چهارم درس چهارم: وزن و ریتم در آواهای ایرانی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش چهارم درس پنجم: شکل (فرم) و گونه (ژانر)های آوایی** — `lesson`؛ ⏳؛ key: `—`
- **بخش پنجم: هنرهای نمایشی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-sec5.pdf`
  - زیرگره: **بخش پنجم درس اوّل: کارگردانی و انتخاب نمایشنامه** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش پنجم درس دوم: انتخاب بازیگر و خواندن متن** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **بخش پنجم درس سوم: حرکت و چیدمان روی صحنه** — `lesson`؛ ⏳؛ key: `—`
- **واژه‌نامه** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-art/g9-art-dict.pdf`

</details>

<details>
<summary><strong>آمادگی دفاعی نهم</strong> — کد <code>915</code>، 4 گره، 4 گرهٔ مستقیم آماده، 0 tab</summary>

- **فهرست** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-defa/g9-defa-index.pdf`
- **فصل اول: مفاهیم و ضرورت آمادگی دفاعی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-defa/g9-defa-f01.pdf`
  - زیرگره: **فصل اول درس یکم: تهدید چیست؟ دشمن کیست؟** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل اول درس دوم: موقعیت ایران و تهدیدهای آن** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل اول** — `wrapup`؛ ⏳؛ key: `—`
- **فصل دوم: فرهنگ دفاعی ما** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-defa/g9-defa-f02.pdf`
  - زیرگره: **فصل دوم درس سوم: روایت فتح** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل دوم درس چهارم: باید برخاست** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل دوم درس پنجم: رویارویی قدرت‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل دوم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل سوم: مهارت‌های دفاعی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-defa/g9-defa-f03.pdf`
  - زیرگره: **فصل سوم درس ششم: نظام جمع و مهارت‌های رزم** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل سوم درس هفتم: پدافند غیرعامل** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل سوم درس هشتم: جنگ پنهان** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل سوم درس نهم: امداد و نجات** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل سوم** — `wrapup`؛ ⏳؛ key: `—`

</details>

<details>
<summary><strong>انگلیسی نهم</strong> — کد <code>910</code>، 14 گره، 7 گرهٔ مستقیم آماده، 42 tab</summary>

- **آیه قرآن** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p01.pdf`
- **توصیه به دبیران** — `plain`؛ ☁️ نیازمند باکت؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p02.pdf`
- **نقشهٔ کتاب: Map of Prospect 3** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p03.pdf`
- **صفحهٔ Spotlight: خوش‌آمد** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p04.pdf`
- **درس یکم: Lesson 1: Personality** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Textbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Textbook PDF ✅
- **درس دوم: Lesson 2: Travel** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Textbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Textbook PDF ✅
- **مرور یکم: Review 1** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p07.pdf`
- **درس سوم: Lesson 3** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Textbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Textbook PDF ✅
- **درس چهارم: Lesson 4: Services** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Textbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Textbook PDF ✅
- **مرور دوم: Review 2** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p10.pdf`
- **درس پنجم: Lesson 5: Media** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Textbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Textbook PDF ✅
- **درس ششم: Lesson 6** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Textbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Textbook PDF ✅
- **مرور سوم: Review 3** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p13.pdf`
- **فرهنگ تصویری: Photo Dictionary** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english/g9-english-p14.pdf`

</details>

<details>
<summary><strong>کتاب کار انگلیسی نهم</strong> — کد <code>913</code>، 7 گره، 1 گرهٔ مستقیم آماده، 42 tab</summary>

- **صفحه عنوان: Workbook: English for Schools 3** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-english-workbook/g9-english-titlepage.pdf`
- **درس یکم: Lesson 1** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Workbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Workbook PDF ✅
- **درس دوم: Lesson 2** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Workbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Workbook PDF ✅
- **درس سوم: Lesson 3** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Workbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Workbook PDF ✅
- **درس چهارم: Lesson 4** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Workbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Workbook PDF ✅
- **درس پنجم: Lesson 5** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Workbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Workbook PDF ✅
- **درس ششم: Lesson 6** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: Lesson Teaching ⏳، Workbook Exercises ⏳، Grammar Notes ⏳، Pronunciation Practice ⏳، Lesson Summary & Exam Tips ⏳، Comprehensive Lesson Test ⏳، Workbook PDF ✅

</details>

<details>
<summary><strong>فارسی نهم</strong> — کد <code>903</code>، 15 گره، 14 گرهٔ مستقیم آماده، 0 tab</summary>

- **فهرست** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-index.pdf`
- **پیشگفتار** — `plain`؛ ☁️ نیازمند باکت؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-preface.pdf`
- **ستایش** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-praise.pdf`
- **فصل اول: زیبایی آفرینش** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-f01.pdf`
  - زیرگره: **فصل اول درس یکم: آفرینش همه تنبیه خداوندِ دل است** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **حکایت: سفر** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-st01.pdf`
  - زیرگره: **فصل اول درس دوم: عجایبِ صنعِ حق تعالی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **شعرخوانی: پرواز** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-sh01.pdf`
- **فصل دوم: شکفتن** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-f02.pdf`
  - زیرگره: **فصل دوم درس سوم: مثل آینه، کار و شایستگی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **حکایت: باغبانِ نیک‌اندیش** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-st02.pdf`
  - زیرگره: **فصل دوم درس چهارم: هم‌نشین** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **روان‌خوانی: دریچه‌های شکوفایی** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-rv01.pdf`
- **فصل آزاد: ادبیات بومی ۱** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-azad1.pdf`
  - زیرگره: **فصل آزاد درس پنجم: درس آزاد (۱)** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **حکایت: ادبیات بومی ۱** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-st03.pdf`
  - زیرگره: **شعرخوانی: ادبیات بومی ۱** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-sh02.pdf`
- **فصل سوم: سبکِ زندگی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-f03.pdf`
  - زیرگره: **فصل سوم درس ششم: آداب زندگی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **حکایت: شو، خطرکن!** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-st04.pdf`
  - زیرگره: **فصل سوم درس هفتم: پرتوِ امید** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل سوم درس هشتم: همزیستی با مامِ میهن** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **شعرخوانی: دوراندیشی** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-sh03.pdf`
- **فصل چهارم: نام‌ها و یادها** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-f04.pdf`
  - زیرگره: **فصل چهارم درس نهم: راز موفّقیت** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل چهارم درس دهم: آرشی دیگر** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **حکایت: نیک رایان** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-st05.pdf`
  - زیرگره: **فصل چهارم درس یازدهم: زنِ پارسا** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **روان‌خوانی: دروازه‌ای به آسمان** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-rv02.pdf`
- **فصل پنجم: اسلام و انقلاب اسلامی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-f05.pdf`
  - زیرگره: **فصل پنجم درس دوازدهم: پیام‌آور رحمت** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **حکایت: سیرت سلمان** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-st06.pdf`
  - زیرگره: **فصل پنجم درس سیزدهم: آشنای غریبان، میلادِ گل** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل پنجم درس چهاردهم: پیدای پنهان** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **شعرخوانی: بُوَد قدر تو افزون از مالِک** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-sh04.pdf`
- **فصل آزاد: ادبیات بومی ۲** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-azad2.pdf`
  - زیرگره: **فصل آزاد درس پانزدهم: درس آزاد (۲)** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **روان‌خوانی: ادبیات بومی ۲** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-rv03.pdf`
- **فصل ششم: ادبیات جهان** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-f06.pdf`
  - زیرگره: **فصل ششم درس شانزدهم: آرزو** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ششم درس هفدهم: شازده کوچولو** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **روان‌خوانی: دو نقّاش** — `plain`؛ ✅؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-rv04.pdf`
- **نیایش: بیا تا برآریم دستی ز دل** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-pray.pdf`
- **واژه‌نامه** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-dict.pdf`
- **اعلام: اشخاص، آثار** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-ann.pdf`
- **کتاب‌نامه** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-farsi/g9-farsi-biblio.pdf`

</details>

<details>
<summary><strong>هدیه‌های آسمان نهم</strong> — کد <code>941</code>، 13 گره، 1 گرهٔ مستقیم آماده، 48 tab</summary>

- **فهرست** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-hedye/g9-hedy-index.pdf`
- **درس یکم: شروع یک ماجرا** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس دوم: توطئه‌ای به نام بازی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس سوم: کاروان سرنوشت** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس چهارم: فرار به سوی خدا** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس پنجم: مهمانی رسوایی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس ششم: پیروزی تأمل‌برانگیز** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس هفتم: کلاس توحید در زندان** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس هشتم: رؤیای سرنوشت‌ساز** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس نهم: اسیری که امیر شد** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس دهم: دیدارها تازه می‌شود** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس یازدهم: ماجرای ظرف قیمتی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس دوازدهم: پایان یک ماجرا** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅

</details>

<details>
<summary><strong>کار و فناوری نهم</strong> — کد <code>917</code>، 12 گره، 1 گرهٔ مستقیم آماده، 44 tab</summary>

- **فهرست مطالب** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-karfan/g9-karfan-index.pdf`
- **پودمان یکم: الگوریتم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان دوم: ترسیم با رایانه** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان سوم: ساز و کارهای حرکتی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان چهارم: برنامه‌نویسی پایتون (۳)** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان پنجم: هدایت تحصیلی ــ حرفه‌ای** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان ششم: برق** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان هفتم: تأسیسات مکانیکی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان هشتم: عمران** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان نهم: خودرو** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان دهم: پایش رشد و تکامل کودک** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پودمان یازدهم: صنایع دستی (برجسته‌کاری روی فلز مس)** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅

</details>

<details>
<summary><strong>ریاضی نهم</strong> — کد <code>905</code>، 9 گره، 9 گرهٔ مستقیم آماده، 0 tab</summary>

- **فهرست** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-index.pdf`
- **فصل ۱: مجموعه‌ها** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f01.pdf`
  - زیرگره: **فصل ۱ درس اوّل: معرفی مجموعه** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۱ درس دوم: مجموعه‌های برابر و نمایش مجموعه‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۱ درس سوم: اجتماع، اشتراک و تفاضلِ مجموعه‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۱ درس چهارم: مجموعه‌ها و احتمال** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۱** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ۲: عددهای حقیقی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f02.pdf`
  - زیرگره: **فصل ۲ درس اوّل: عددهای گویا** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۲ درس دوم: عددهای حقیقی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۲ درس سوم: قدر مطلق و محاسبهٔ تقریبی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۲** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ۳: استدلال و اثبات در هندسه** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f03.pdf`
  - زیرگره: **فصل ۳ درس اوّل: استدلال** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۳ درس دوم: آشنایی با اثبات در هندسه** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۳ درس سوم: هم‌نهشتی مثلث‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۳ درس چهارم: حل مسئله در هندسه** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۳ درس پنجم: شکل‌های متشابه** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۳** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ۴: توان و ریشه** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f04.pdf`
  - زیرگره: **فصل ۴ درس اوّل: توان صحیح** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۴ درس دوم: نماد علمی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۴ درس سوم: ریشه‌گیری** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۴ درس چهارم: جمع و تفریق رادیکال‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۴** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ۵: عبارت‌های جبری** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f05.pdf`
  - زیرگره: **فصل ۵ درس اوّل: عبارت‌های جبری و مفهوم اتحاد** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۵ درس دوم: چند اتحاد دیگر، تجزیه و کاربردها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۵ درس سوم: نابرابری‌ها و نامعادله‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۵** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ۶: خط و معادله‌های خطی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f06.pdf`
  - زیرگره: **فصل ۶ درس اوّل: معادلهٔ خط** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۶ درس دوم: شیب خط و عرض از مبدأ** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۶ درس سوم: دستگاه معادله‌های خطی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۶** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ۷: عبارت‌های گویا** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f07.pdf`
  - زیرگره: **فصل ۷ درس اوّل: معرفی و ساده‌کردن عبارت‌های گویا** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۷ درس دوم: محاسبات عبارت‌های گویا** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۷ درس سوم: تقسیم چندجمله‌ای‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۷** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ۸: حجم و مساحت** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-math/g9-math-f08.pdf`
  - زیرگره: **فصل ۸ درس اوّل: حجم و مساحت کره** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۸ درس دوم: حجم هرم و مخروط** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ۸ درس سوم: سطح و حجم** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ۸** — `wrapup`؛ ⏳؛ key: `—`

</details>

<details>
<summary><strong>نگارش نهم</strong> — کد <code>904</code>، 12 گره، 4 گرهٔ مستقیم آماده، 32 tab</summary>

- **فهرست** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-negar/g9-negar-index.pdf`
- **ستایش** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-negar/g9-negar-praise.pdf`
- **درس یکم: با نظام ذهنی «پرورده» و «مند» بنویسیم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس دوم: واژه‌ها را بشناسیم، گزینش کنیم و به کار بگیریم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس سوم: نوعِ زبانِ نوشته را انتخاب کنیم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس چهارم: فضا و رنگِ نوشته را تغییر دهیم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس پنجم: نوشته را خوش آغاز کنیم؛ زیبا به پایان ببریم و نیکو نام‌گذاری کنیم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس ششم: قالبی برای نوشتن برگزینیم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس هفتم: وسعت و عمق نوشته را بیشتر کنیم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **درس هشتم: نوشته را ویرایش کنیم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **نیایش** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-negar/g9-negar-pray.pdf`
- **کتاب‌نامه** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-negar/g9-negar-biblio.pdf`

</details>

<details>
<summary><strong>پیام‌های آسمان نهم</strong> — کد <code>902</code>، 5 گره، 5 گرهٔ مستقیم آماده، 0 tab</summary>

- **فصل اول: خداشناسی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-payam/g9-hedy-f01.pdf`
  - زیرگره: **فصل اول درس یکم: تو را چگونه بشناسم؟** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل اول درس دوم: در پناه ایمان** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل اول** — `wrapup`؛ ⏳؛ key: `—`
- **فصل دوم: راهنماشناسی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-payam/g9-hedy-f02.pdf`
  - زیرگره: **فصل دوم درس سوم: راهنمایان الهی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل دوم درس چهارم: خورشید پنهان** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل دوم درس پنجم: رهبری در دوران غیبت** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل دوم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل سوم: راه و توشه** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-payam/g9-hedy-f03.pdf`
  - زیرگره: **فصل سوم درس ششم: وضو، غسل و تیمم** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل سوم درس هفتم: احکام نماز** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل سوم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل چهارم: اخلاق** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-payam/g9-hedy-f04.pdf`
  - زیرگره: **فصل چهارم درس هشتم: همدلی و همراهی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل چهارم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل پنجم: جامعه اسلامی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-payam/g9-hedy-f05.pdf`
  - زیرگره: **فصل پنجم درس نهم: انقلاب اسلامی ایران** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل پنجم درس دهم: مسئولیت همگانی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل پنجم درس یازدهم: انفاق** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل پنجم درس دوازدهم: جهاد** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل پنجم** — `wrapup`؛ ⏳؛ key: `—`

</details>

<details>
<summary><strong>آموزش قرآن نهم</strong> — کد <code>901</code>، 14 گره، 3 گرهٔ مستقیم آماده، 33 tab</summary>

- **فهرست** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-quran/g9-quran-index.pdf`
- **درس یکم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس دوم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس سوم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس چهارم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس پنجم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس ششم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس هفتم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس هشتم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس نهم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس دهم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **درس یازدهم** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، خانه‌های نورانی ⏳، کتاب درسی ✅
- **نیایش: دعای ختم قرآن کریم** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-quran/g9-quran-pray.pdf`
- **فهرست کلمات** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-quran/g9-quran-vocab.pdf`

</details>

<details>
<summary><strong>علوم تجربی نهم</strong> — کد <code>906</code>، 16 گره، 1 گرهٔ مستقیم آماده، 60 tab</summary>

- **فصل اول: مواد و نقش آنها در زندگی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل دوم: رفتار اتم‌ها با یکدیگر** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل سوم: به دنبال محیطی بهتر برای زندگی** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل چهارم: حرکت چیست؟** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل پنجم: نیرو** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل ششم: زمین‌ساخت ورقه‌ای** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل هفتم: آثاری از گذشته زمین** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل هشتم: فشار و آثار آن** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل نهم: ماشین‌ها** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل دهم: نگاهی به فضا** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل یازدهم: گوناگونی جانداران** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل دوازدهم: دنیای گیاهان** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل سیزدهم: جانوران بی‌مهره** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل چهاردهم: جانوران مهره‌دار** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **فصل پانزدهم: باهم‌زیستن** — `lesson`؛ ⏳ گره/رسانهٔ مستقیم ندارد؛ key: `—`
  - tabها: تدریس ⏳، نمونه سوالات کتابی ⏳، نمونه سوالات آزمونی ⏳، کتاب درسی ✅
- **پیوست: جدول تناوبی عناصر** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-sci/g9-sci-appendix.pdf`

</details>

<details>
<summary><strong>مطالعات اجتماعی نهم</strong> — کد <code>907</code>، 15 گره، 15 گرهٔ مستقیم آماده، 0 tab</summary>

- **فهرست** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-index.pdf`
- **فصل اول: سیارهٔ ما، زمین** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f01.pdf`
  - زیرگره: **فصل اول درس ۱: زمین، مهد زیبای انسان‌ها** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل اول درس ۲: حرکات زمین** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل اول** — `wrapup`؛ ⏳؛ key: `—`
- **فصل دوم: سنگ‌کره، آب‌کره، هواکره** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f02.pdf`
  - زیرگره: **فصل دوم درس ۳: چهرهٔ زمین** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل دوم درس ۴: آب فراوان، هوای پاک** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل دوم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل سوم: زیست‌کره، تنوع شگفت‌انگیز** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f03.pdf`
  - زیرگره: **فصل سوم درس ۵: پراکندگی زیست‌بوم‌های جهان** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل سوم درس ۶: زیست‌بوم‌ها در خطرند** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل سوم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل چهارم: ساکنان سیارهٔ زمین** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f04.pdf`
  - زیرگره: **فصل چهارم درس ۷: جمعیت جهان** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل چهارم درس ۸: بی‌عدالتی و نابرابری در جهان** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل چهارم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل پنجم: عصر یکپارچگی و شکوفایی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f05.pdf`
  - زیرگره: **فصل پنجم درس ۹: ایرانی متحد و یکپارچه** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل پنجم درس ۱۰: اوضاع اجتماعی، اقتصادی، علمی و فرهنگی ایران در عصر صفوی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل پنجم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل ششم: ایران از عهد نادرشاه تا ناصرالدین‌شاه** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f06.pdf`
  - زیرگره: **فصل ششم درس ۱۱: تلاش برای حفظ استقلال و اتحاد سیاسی ایران** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل ششم درس ۱۲: در جست‌وجوی پیشرفت و رهایی از سلطهٔ خارجی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل ششم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل هفتم: ایران در عصر مشروطه** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f07.pdf`
  - زیرگره: **فصل هفتم درس ۱۳: نهضت مشروطه** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل هفتم درس ۱۴: ایران در دوران حکومت پهلوی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل هفتم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل هشتم: سقوط حکومت شاهنشاهی و شکل‌گیری نظام جمهوری اسلامی** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f08.pdf`
  - زیرگره: **فصل هشتم درس ۱۵: انقلاب اسلامی ایران** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل هشتم درس ۱۶: ایران در دوران پس از پیروزی انقلاب اسلامی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل هشتم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل نهم: فرهنگ و هویت** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f09.pdf`
  - زیرگره: **فصل نهم درس ۱۷: فرهنگ** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل نهم درس ۱۸: هویت** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل نهم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل دهم: خانواده و جامعه** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f10.pdf`
  - زیرگره: **فصل دهم درس ۱۹: ارزش‌ها و کارکردهای خانواده** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل دهم درس ۲۰: آرامش در خانواده** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل دهم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل یازدهم: حکومت و مردم** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f11.pdf`
  - زیرگره: **فصل یازدهم درس ۲۱: نهاد حکومت** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل یازدهم درس ۲۲: حقوق و تکالیف شهروندی** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل یازدهم** — `wrapup`؛ ⏳؛ key: `—`
- **فصل دوازدهم: بهره‌وری** — `container`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-f12.pdf`
  - زیرگره: **فصل دوازدهم درس ۲۳: بهره‌وری چیست؟** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **فصل دوازدهم درس ۲۴: اقتصاد و بهره‌وری** — `lesson`؛ ⏳؛ key: `—`
  - زیرگره: **جمع‌بندی فصل دوازدهم** — `wrapup`؛ ⏳؛ key: `—`
- **کاربرگه‌های فعّالیت** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-ws01.pdf`
- **واژه‌نامه** — `plain`؛ ✅ مستقیم آماده؛ key: `Bucket/Pdf-files/G09/g9-soc/g9-soc-dict.pdf`

</details>


---

## 10. درخت واقعی کارت‌های آموزشگاه

این بخش جدا از `SkillsCatalog` است: UI قابل دسترسِ تب آموزشگاه مستقیماً از `AcademySections` در `AcademyHubScreen.kt` تولید می‌شود. هر گروه یک آکاردئون تک‌انتخابی است و وضعیت باز آن در `LocalStore("hamyar_academy_ui")` می‌ماند. **فقط سه کارت اول** `publishedContentId` دارند و به HTML می‌روند؛ همهٔ کارت‌های دیگر عمداً به `academy-coming-soon` می‌روند.

### خواندن و نوشتن — `reading`

- **سرعت خواندن** — ID `speed`، کاور `sk-speed` → ✅ `content-html/amz-01`
- **خوشنویسی** — ID `handwriting`، کاور `sk-hand` → ✅ `content-html/amz-02`
- **تایپ لمسی** — ID `typing`، کاور `sk-type` → ✅ `content-html/amz-03`
- **یادداشت‌برداری** — ID `notes`، کاور `sk-cornell` → ⏳ `academy-coming-soon`
- **خلاصه‌نویسی** — ID `summary`، کاور `sk-summary` → ⏳ `academy-coming-soon`
### بیان و تفکر نقاد — `critical`

- **اصول مناظره** — ID `debate`، کاور `sk-debate` → ⏳ `academy-coming-soon`
- **مغالطه‌های منطقی** — ID `fallacies`، کاور `sk-fallacy` → ⏳ `academy-coming-soon`
- **فن بیان** — ID `speaking`، کاور `sk-present` → ⏳ `academy-coming-soon`
- **اضطراب سخنرانی** — ID `speech-anxiety`، کاور `sk-voice` → ⏳ `academy-coming-soon`
- **ایمیل رسمی** — ID `email`، کاور `sk-email` → ⏳ `academy-coming-soon`
- **گوش‌دادن فعال** — ID `listening`، کاور `sk-listen` → ⏳ `academy-coming-soon`
### ذهن، حافظه و منطق — `mind`

- **محاسبات ذهنی** — ID `mental-math`، کاور `sk-math` → ⏳ `academy-coming-soon`
- **قصر ذهنی و تقویت حافظه** — ID `memory-palace`، کاور `sk-palace` → ⏳ `academy-coming-soon`
- **زنجیره تداعی** — ID `association`، کاور `sk-chain` → ⏳ `academy-coming-soon`
- **حل مسئله ۵ مرحله‌ای** — ID `problem-solving`، کاور `sk-lateral` → ⏳ `academy-coming-soon`
### روان‌شناسی فردی — `psychology`

- **توقف نشخوار فکری** — ID `rumination`، کاور `hyp-reframe` → ⏳ `academy-coming-soon`
- **خودآگاهی و گفت‌وگوی درونی** — ID `self-awareness`، کاور `sk-feel` → ⏳ `academy-coming-soon`
- **انعطاف‌پذیری و انگیزه** — ID `resilience`، کاور `sk-resilience` → ⏳ `academy-coming-soon`
### مدیریت زمان — `time`

- **پومودورو** — ID `pomodoro`، کاور `ln-pomo` → ⏳ `academy-coming-soon`
- **برنامه‌ریزی روزانه** — ID `daily-plan`، کاور `sk-plan` → ⏳ `academy-coming-soon`
- **ساخت عادت** — ID `habit`، کاور `sk-habit` → ⏳ `academy-coming-soon`
- **مدیریت فضای مطالعه** — ID `study-space`، کاور `sk-desk` → ⏳ `academy-coming-soon`
### مهارت دیجیتال — `digital`

- **ویندوز کاربردی** — ID `windows`، کاور `sk-digital` → ⏳ `academy-coming-soon`
- **سواد هوش مصنوعی** — ID `ai-literacy`، کاور `sk-ai-what` → ⏳ `academy-coming-soon`
- **جست‌وجوی هوشمند** — ID `search`، کاور `sk-search` → ⏳ `academy-coming-soon`
- **ایمنی مجازی** — ID `online-safety`، کاور `sk-privacy` → ⏳ `academy-coming-soon`
- **تشخیص اخبار جعلی** — ID `fake-news`، کاور `sk-fake` → ⏳ `academy-coming-soon`
### زندگی اجتماعی و روابط — `social`

- **کار تیمی** — ID `teamwork`، کاور `sk-team` → ⏳ `academy-coming-soon`
- **مدیریت اختلاف با دوستان** — ID `conflict`، کاور `sk-conflict` → ⏳ `academy-coming-soon`
- **هنر نه گفتن** — ID `saying-no`، کاور `sk-no` → ⏳ `academy-coming-soon`
### سواد مالی — `finance`

- **مدیریت بودجه** — ID `budget`، کاور `sk-budget` → ⏳ `academy-coming-soon`
- **تشخیص نیاز در برابر خواسته** — ID `need-want`، کاور `sk-need` → ⏳ `academy-coming-soon`
### خلاقیت — `creativity`

- **طوفان فکری** — ID `brainstorm`، کاور `sk-storm` → ⏳ `academy-coming-soon`
- **تفکر خلاق** — ID `creative-thinking`، کاور `sk-create` → ⏳ `academy-coming-soon`
### تئوری موسیقی — `music`

- **درس جامع تئوری موسیقی** — ID `music-theory`، کاور `sk-music` → ⏳ `academy-coming-soon`

### وضعیت کاتالوگ مهارتِ غیرمتصل

`SkillsCatalog.kt` شامل ۱۱ گروه و آیتم‌های Practice است و `WellnessMenu` آن‌ها را به map داخلی خود اضافه می‌کند، اما در نسخهٔ بررسی‌شده هیچ `PracticeHubScreen` با `skillsIds` فراخوانی نمی‌شود. در نتیجه این‌ها **کارت reachable در UI نیستند** و نباید به‌عنوان محتوای قابل استفاده اعلام شوند؛ به‌عنوان کاتالوگ خام/غیرمتصل باقی می‌مانند.
