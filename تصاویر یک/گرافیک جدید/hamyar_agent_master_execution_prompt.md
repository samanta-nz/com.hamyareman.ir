# پرامپت اجرایی جامع برای بازطراحی Hamyar

## نقش Agent
تو یک Senior Android Product Designer + Compose UI Engineer + Interaction/Motion Designer هستی. پروژه `com.hamyareman.ir` را به‌صورت واقعی اصلاح کن، نه mockup. همه تغییرات مستقیم روی `main` انجام شوند و branch جدید نساز.

## اصل طلایی
خروجی نباید مجموعه‌ای از Cardهای تخت، Buttonهای پیش‌فرض Material یا فرم‌های اداری باشد. باید حس محصول تجاری ممتاز، دفتر کاغذی واقعی، کتاب واقعی، عمق، سایه، کاغذ، جلد، ورق‌خوردن، RTL فارسی، کنترل لمسی و motion حرفه‌ای را منتقل کند. Material فقط زیرساخت باشد؛ ظاهر نهایی اختصاصی باشد.

---

# 1. موتور مشترک دفترها

دفترچه‌ها، دفتر خاطرات و دفتر شعر باید از یک engine مشترک استفاده کنند، با personality جداگانه.

### کاغذ
- عرض کاغذ را به اندازه قبلی/اصلی پروژه برگردان؛ بیش از حد باریک نشود.
- صفحه تا حد ممکن تمام ارتفاع قابل استفاده را بگیرد.
- حاشیه اضافی کم باشد.
- حس A4/دفتر واقعی حفظ شود.
- فضای نوشتن قربانی UI نشود.

### قانون سطرها
- **دیگر ۶ سطر خالی از بالا وجود ندارد.**
- نوشتن از همان سطر اول شروع می‌شود.
- خطوط افقی منظم و قابل خواندن باشند.
- تعداد خطوط از ارتفاع واقعی صفحه، lineHeight و font scale محاسبه شود.
- با افزایش فونت، صفحه بی‌دلیل فشرده نشود.

### خطوط عمودی
در دفترهای معمولی:
- دو خط عمودی موازی حفظ شوند.
- شروع خطوط و ناحیه نوشتن از سمت راست باشد.
- خطوط بخشی از کاغذ باشند، نه border یک Card.

در تمام شعرها:
- **دو خط عمودی کاملاً حذف شوند.**
- شعر روی کاغذ آزاد و تمیز باشد.

---

# 2. عنوان و شماره

عنوان انتخابی بخشی از خود صفحه است.

در اولین سطر:
`عنوان + شماره`

مثال:
`قصیده شماره ۴`

بعد از عنوان دقیقاً یک سطر فاصله و سپس متن.

شماره بر اساس عنوان ثبت‌شده قبلی + 1 ساخته شود:
- قبلی: قصیده شماره ۳
- بعدی: قصیده شماره ۴

همان عنوان نهایی همراه شماره در storage ذخیره شود.

### Title Picker
برای تمام دفترها مشترک:
1. اولین گزینه: عنوان مرتبط با محتوا
2. گزینه دوم: `عنوان جدید`
3. سپس پیشنهادهای قبلی/مرتبط

عنوان:
- یک خط
- بدون wrap
- ellipsis در طول زیاد

---

# 3. Alignment

قانون معکوس:

- اگر متن وسط‌چین است → عنوان راست‌چین.
- اگر متن راست‌چین است → عنوان وسط‌چین.
- اگر متن چپ‌چین است → عنوان وسط‌چین.

کنترل alignment فقط با آیکون‌های حرفه‌ای:
- راست‌چین
- وسط‌چین
- چپ‌چین

حالت فعال، pressed، disabled و accessibility label داشته باشد.

---

# 4. شکست صفحه

دفتر نباید vertical scroll داشته باشد.

وقتی آخرین سطر پر شد:
- متن خودکار به صفحه بعد منتقل شود.
- صفحه بعد خودکار باز شود.
- cursor درست ادامه پیدا کند.
- page number به‌روزرسانی شود.
- swipe افقی کار کند.
- جهت ورق‌زدن برای فارسی RTL باشد.

تمام صفحات overflow باید ذخیره شوند؛ فقط صفحه اول ذخیره نشود.

---

# 5. گرافیک واقعی سه‌بعدی

یک صفحه نباید فقط Box + shadow باشد.

صفحه:
- shadow چندلایه
- اختلاف عمق ظریف
- لبه کاغذ
- page stack
- shadow داخلی/خارجی
- highlight ظریف
- texture بسیار ملایم در صورت امکان
- corner radius بسیار کم

کتاب:
- جلد جلو
- spine
- ضخامت صفحات
- لبه صفحات
- shadow زیر کتاب
- highlight جلد
- پرسپکتیو ظریف
- stack واقعی
- باز و بسته شدن کتاب
- ورق‌خوردن RTL

### Cover
جلد دقیقاً با نسبت صفحه داخلی هماهنگ باشد.

### Motion
باز شدن:
1. cover وارد شود
2. settle/scale کوتاه
3. shadow ظاهر شود
4. stack صفحات دیده شود
5. کتاب در حالت باز قرار گیرد

ورق:
- curl/flip
- حرکت shadow
- سرعت طبیعی
- RTL
- قابل قطع و ادامه

---

# 6. دفتر خاطرات

### لیست
هر آیتم یک ردیف حرفه‌ای:
- preview جلد/صفحه
- عنوان
- تاریخ
- شماره صفحه/تعداد صفحات در صورت نیاز
- edit
- delete

عنوان طولانی هرگز به خط دوم نرود.

### افزودن
دکمه `+` باید اختصاصی، زیبا، tactile، animated و accessible باشد.

### Viewer
Tap روی صفحه:
- همان page دقیقاً باز شود.
- viewer باشد، نه edit.
- edit فقط با action جداگانه.
- navigation بین صفحات حفظ شود.

### تصویر صفحه
برای هر page:
1. انتخاب تصویر
2. تثبیت انتخاب
3. سپس caption/title
4. caption زیر تصویر

امکانات:
- auto-fit
- resize
- عرض قابل کنترل
- vertical offset
- wrap: NONE / TOP / BOTTOM
- تصویر نباید تصادفی روی متن بیفتد.

اگر wrap ثبت شده، متن و تصویر باید واقعاً در layout همان page قرار بگیرند، نه overlay تصادفی.

---

# 7. دفتر شعر

همان notebook engine با تفاوت‌های زیر:

- انتخاب نوع شعر
- حذف خطوط عمودی
- layout مخصوص شعر

انواع:
- غزل
- قصیده
- دوبیتی
- رباعی
- قطعه
- مثنوی
- شعر سپید
- نثر شاعرانه

### فرم‌های دو مصراعی
برای غزل، قصیده، دوبیتی، رباعی و فرم‌های مشابه:
- دو ناحیه نامرئی چپ/راست
- بدون border
- هر مصراع مستقل
- justification حرفه‌ای
- tap روی هر مصراع
- selection با background بسیار ظریف
- امکان ویرایش همان بخش

### راهنما
Guide مستقل، collapsible، غیرمزاحم، با مثال فارسی و illustration کوچک.

---

# 8. تمام‌صفحه بودن

دفتر خاطرات و دفتر شعر هنگام باز شدن باید تمام‌صفحه باشند:
- حداقل padding
- استفاده از تمام ارتفاع منطقی
- keyboard inset صحیح
- نسبت واقعی کاغذ
- فضای نوشتن حداکثری

---

# 9. آلبوم شخصی

Gallery واقعی و adaptive:

- Grid adaptive، نه تعداد ستون ثابت
- موبایل/تبلت/landscape خودکار تطبیق یابد
- spacing حرفه‌ای

### عکس
- zoom
- pan
- rotate
- crop
- mirror
- fit
- actual size در صورت نیاز

### ویدئو
- play/pause
- seek
- timeline
- fullscreen
- volume
- speed
- repeat
- ±10s
- resume
- loading/error

### صوت
از PlaybackController/engine مشترک پروژه استفاده کن.

Audio player باید واقعاً کار کند:
- load media
- play/pause واقعی
- seek واقعی
- elapsed/duration
- ±10s
- speed
- repeat
- resume
- loading
- error
- background playback در صورت نیاز

صرف ساختن UI دکمه Play کافی نیست؛ tap باید واقعاً media را آماده و پخش کند.

### Actionهای فایل
در tile فقط icon:
- external open
- share/export
- delete

برای همه contentDescription و برای delete confirmation.

---

# 10. Safe Space

ترتیب:
1. ساخت بکاپ / بازیابی
2. فرمت بکاپ
3. هشدارها
4. سایر امکانات
5. تنظیمات امنیتی مستقل در انتها

Security:
- مستقل از App Lock
- مستقل از login
- مستقل از notebook lock
- collapsible
- همیشه در حالت اولیه collapsed

---

# 11. App Lock

App Lock کاملاً مستقل از Safe Space Lock:
- credential جدا
- state جدا
- settings جدا
- unlock جدا

### Pattern
در هر دو App Lock و Safe Space:
**Pattern واقعی نقطه + خط**

نه button grid.

کاربر با انگشت روی شبکه 3×3 drag کند.

ویژگی‌ها:
- نقاط واضح
- خط هنگام drag
- selected-point state
- glow ظریف
- haptic feedback
- invalid feedback
- success animation

صفحه قفل:
- background اختصاصی
- depth
- نور/سایه
- pattern واقعی
- lock state
- animation
- error/success state

---

# 12. Navigation

RTL واقعی:
- back
- swipe
- title placement
- transitions
- save/cancel/delete/edit/view
- مسیرهای ورود و خروج

هیچ navigation action نباید ظاهراً وجود داشته باشد ولی عملیاتی نباشد.

---

# 13. Button و Control Design

از Buttonهای متنی بزرگ و اداری پرهیز کن.

برای actions معمول:
- icon-first
- compact
- touch target مناسب
- tooltip
- selected/pressed/disabled/loading states
- accessibility

Danger actions:
- visual warning
- confirmation

---

# 14. Responsive و Typography

پشتیبانی:
- گوشی کوچک
- گوشی بزرگ
- landscape
- tablet

هیچ چیزی:
- clip نشود
- روی هم نیفتد
- از صفحه خارج نشود
- عنوان ناخواسته دو خط نشود

برای فارسی:
- font مناسب
- lineHeight صحیح
- baseline صحیح
- RTL
- font scale واقعی

افزایش font size نباید فضای نوشتن را نابود کند.

---

# 15. اصل مهم درباره فضای نوشتن

کاربر هرگز درخواست نکرده:
- متن دفتر کم شود
- عرض کاغذ کم شود
- تعداد خطوط بی‌دلیل کم شود
- فضای نوشتن کم شود
- title فضای زیادی بگیرد.

اگر برای جا دادن UI چنین اتفاقی می‌افتد، معماری را اصلاح کن؛ فضای نوشتن را قربانی نکن.

---

# 16. Persistence

این موارد باید کامل ذخیره و restore شوند:
- title
- number
- alignment
- text
- all pages
- images
- captions
- image width
- image position
- wrap
- poem type
- current page

Reopen باید همان وضعیت قبلی را برگرداند.

---

# 17. Preview

Preview هر دفتر باید خودش حس دفتر/کتاب داشته باشد:
- paper
- lines
- title
- متن نمونه
- shadow
- page edge
- cover در صورت وجود

---

# 18. کیفیت بصری

ممنوع:
- Material Cardهای تخت به‌عنوان ظاهر اصلی
- border ضخیم
- shadow مصنوعی
- gradient ارزان
- تعداد زیاد Button متنی
- spacing نامتناسب
- صفحات خالی و تخت
- icon بدون hierarchy

مطلوب:
- paper feel
- leather/cover feel
- depth
- realistic shadows
- page stack
- subtle highlight
- motion
- tactile feedback
- visual hierarchy
- premium Persian editorial design

---

# 19. معیار پذیرش

## Notebook
- [ ] شروع نوشتن از سطر اول
- [ ] بدون ۶ سطر خالی
- [ ] دو خط عمودی در دفتر معمولی
- [ ] بدون دو خط عمودی در شعر
- [ ] شروع خطوط از راست
- [ ] no vertical scrolling
- [ ] overflow خودکار
- [ ] عرض کاغذ به اندازه قبلی
- [ ] فضای نوشتن کافی
- [ ] title در سطر اول
- [ ] یک سطر فاصله
- [ ] شماره صحیح
- [ ] inverse alignment
- [ ] alignment icons
- [ ] title picker
- [ ] single-line title

## Diary
- [ ] full screen
- [ ] real cover
- [ ] 3D stack
- [ ] shadow
- [ ] page turn
- [ ] RTL
- [ ] exact-page viewer
- [ ] edit جدا
- [ ] image per page
- [ ] caption after stabilization
- [ ] resize
- [ ] vertical position
- [ ] wrap
- [ ] persistence

## Poetry
- [ ] full screen
- [ ] shared notebook engine
- [ ] poem type
- [ ] no vertical lines
- [ ] two-hemistich layout
- [ ] invisible columns
- [ ] justified hemistich
- [ ] clickable hemistich
- [ ] numbering
- [ ] guide

## Album
- [ ] adaptive grid
- [ ] image editing
- [ ] video playback
- [ ] audio playback
- [ ] shared player engine
- [ ] speed
- [ ] seek
- [ ] repeat
- [ ] ±10 sec
- [ ] external/share/delete icons

## Locks
- [ ] App Lock مستقل
- [ ] Safe Space Lock مستقل
- [ ] real dot+line pattern
- [ ] drag gesture
- [ ] visual feedback
- [ ] haptic
- [ ] success/error

## Safe Space
- [ ] backup/restore first
- [ ] format after
- [ ] security last
- [ ] security collapsed initially

---

# 20. روش اجرای Agent

قبل از تغییر:
1. repository را بررسی کن.
2. engineهای موجود را reuse کن.
3. duplicate نساز.
4. persistence موجود را خراب نکن.
5. navigation را trace کن.
6. player engine موجود را پیدا کن.
7. App Lock و Safe Space Lock را جدا نگه دار.

ترتیب اجرا:
1. Design System مشترک
2. Notebook engine
3. Diary
4. Poetry
5. Album/player
6. Locks
7. Navigation/integration
8. persistence
9. build/test
10. release verification

بعد از هر بخش compile و API/state/persistence/navigation را بررسی کن.

در پایان:
- full build
- lint/compile
- versionName/versionCode verification
- signing verification
- APK verification
- release asset verification
- forced-update backend verification

---

# قانون نهایی

اگر implementation فعلی با این specification تضاد دارد، این specification اولویت دارد.

قابلیت ظاهری کافی نیست:

> Audio player بدون پخش واقعی، player نیست.

> Card با shadow، کتاب سه‌بعدی نیست.

> Buttonهای ساده، Pattern Lock نیستند.

هدف نهایی این است که تجربه واقعی محصول همان کیفیت و حس طرح مرجع را داشته باشد، نه اینکه فقط نام قابلیت‌ها مشابه باشد.
