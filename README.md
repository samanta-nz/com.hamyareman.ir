# همیار من — com.hamyareman.ir

اپ اندروید «همیار من» (Kotlin + Compose، ماژولار) با بک‌اند Appwrite.

## ساختار

```
apps/hamyar-app     ← اپ (نقطه‌ی کامپایل APK)
shared/             ← ماژول‌های اشتراکی (core-* و feature-*)
gradle/             ← wrapper + کاتالوگ وابستگی‌ها
.github/workflows/  ← بیلد/تست CI و دیپلوی فانکشن‌ها
archive/            ← بک‌اند، مستندات و خروجی‌های تاریخی (غیرِکامپایل)
```

- شناسه‌ی اپ: `com.hamyareman.ir` — پکیج‌نیم همه‌ی سورس‌ها: `com.hamyareman.ir(.platform.*)`
- هویت‌های دیتا که عمداً تغییر نکرده‌اند: `roozhayeman_local` (نام استور)،
  `roozhayeman_private_v1` (alias کلید رمزنگاری)، نقش‌های سروری `zahra/father/guest`.
