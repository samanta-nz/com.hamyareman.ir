# اشتراک و ادمین — استقرار

تابع همان `user-bootstrap` است (سقف پلن رایگان). APK هیچ کلید سروری ندارد.

## ۱) جدول `subscription_orders` در دیتابیس فعلی (ZahraDB)

ستون‌ها:

| ستون | نوع |
|---|---|
| userId | string |
| email | string |
| status | string |
| planId | string |
| createdAtMs | integer |
| payload | string (JSON بقیهٔ فیلدها) |

Permissions ردیف را خالی بگذار؛ فقط تابع با API key می‌نویسد.

## ۲) برچسب ادمین

روی کاربر ادمین در Auth → Labels مقدار `admin` را بگذار.
یا متغیر محیطی تابع: `ADMIN_EMAILS=you@example.com`

## ۳) استقرار تابع

کد `src/main.js` را روی تابع `user-bootstrap` دوباره deploy کن.
`APPWRITE_DATABASE_ID` باید همان دیتابیس اپ باشد.

## ۴) شماره کارت کسب‌وکار

در `shared/core-common/.../Billing.kt` سه ثابت `ACCOUNT_HOLDER` / `CARD_NUMBER` / `BANK_NAME`.
