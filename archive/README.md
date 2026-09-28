# archive/ — هرچه برای کامپایل اپ لازم نیست

اپ و ابزار بیلد فقط از این‌ها استفاده می‌کنند: `apps/` ، `shared/` ، `gradle/` ،
`settings.gradle.kts` ، `build.gradle.kts` (ریشه) ، `gradle.properties` ، `gradlew*`
و ورک‌فلوهای `.github/`. بقیه این‌جاست:

| مسیر | محتوا |
|---|---|
| `backend/` | فانکشن‌های Appwrite (ai-companion/study-tutor/google-auth و…) + seed + `appwrite.json` — با ورک‌فلوهای dispatch دیپلوی می‌شوند |
| `docs/` | مستندات عملیاتی (WORKFLOWS، STUDY-PILOT) |
| `artifacts/` | پرامپت‌های تاریخی و خروجی‌های مرحله‌ی ساخت محتوا |
| `APPWRITE.md` / `AUDIT.md` | مستندات اولیه‌ی بک‌اند و ممیزی |

**`wellness-references/`** (کتاب‌ها و رسانه‌های مرجع) از درخت فعلی حذف شد تا مخزن
سبک بماند؛ تاریخچه‌ی کاملش در همین مخزن موجود است:
`git log --oneline -- wellness-references` و بازیابی با
`git checkout <کامیت> -- wellness-references`.
