# سیستم مطالعه — پایلوت ریاضی نهم E01-L01 (پرامپت ۰۴)

## معماری: آفلاین‌محور، AI فقط تقویت

```
assets/studypacks/C905_E01-L01.json   ← محتوای پک (بخش‌ها/فلش‌کارت/سوال/حل)
shared/feature-study/                 ← موتور مستقل از UI
  Sm2.kt            تکرار فاصله‌دار سبک (q<3 → فردا؛ lapse؛ MIN_EASE=1.3)
  StudyPack.kt      مدل + fromJson
  QuizGrader.kt     تصحیح قطعی آفلاین (mcq/numeric/short با ارقام فارسی+کسر)
  StudyProgressRepository.kt  کش محلی + outbox → جدول study_progress
  StudyPackRepository.kt      خواندن از assets
apps/hamyar-app  ui/study/LessonStudyScreens.kt   ← ۴ تب: خلاصه‌ها/فلش‌کارت/آزمون/حل
backend/functions/study-tutor/        ← AI اختیاری رفع اشکال مفهومی (کلید سمت سرور)
backend/seed/migrate-prompt-04-study-progress.js  ← جدول study_progress (idempotent)
```

## جریان‌ها

- **فلش‌کارت:** همیشه اولین کارتِ سررسید؛ ۴ دکمه‌ی کیفیت (q=1/3/4/5)؛ بعد از مرور،
  کارت به فردا یا دورتر می‌رود و از صف خارج می‌شود. «تسلط٪» بالای صفحه.
- **آزمون:** ۱۰ سوال (اشتباه‌های قبلی اولویت دارند). تصحیح همیشه آفلاین و قطعی؛
  نمره با AI ساخته نمی‌شود. سوال غلط → دکمه «رفع اشکال با هوش مصنوعی» (فقط آنلاین).
- **آزمون دوره‌ای:** هر ۷ روز از آخرین آزمون، بازپرسی اشتباه‌های قبلی پیشنهاد می‌شود
  (`PERIODIC_DAYS=7`) تا «تا اطمینان» تکرار شود.

## سینک

نوشتن اول در کش گوشی (`study:<packId>:cards|attempts`)؛ سپس صف outbox در
SyncEngine به جدول `study_progress` (سطر `sp-<uid>-<packId>`، همه‌ی ستون‌ها string).
جدول با مایگریشن بالا ساخته می‌شود (در CI: الگوی `deploy-prompt-02.yml` با secret
`APPWRITE_API_KEY`؛ در لوکال: متغیرهای محیطی + حذف `--dry-run`).

## گسترش به بقیه‌ی دروس (بعد از تأیید پایلوت)

۱. JSON پک جدید در `apps/hamyar-app/src/main/assets/studypacks/<bookId>-<lessonId>.json`
   با همین اسکیمای ۵ کلید (sections/flashcards/questions/solutions/…).
۲. ورودی هاب: `Screen.LessonStudy.of(packId)` — همین.
۳. نیازی به تغییر موتور/UI نیست؛ محتوای فعلی نمونه‌ی واقعی فصل۱ درس۱ است و بعداً
   محتوای کامل کاربر جایگزین می‌شود.
