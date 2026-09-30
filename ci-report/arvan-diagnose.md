نسخه‌ها: boto3 1.43.105 botocore 1.43.105

# تشخیص آروان

## ۱) کلیدها

- `ARVAN_ACCESS_KEY`: طول 36، شروع با `6479…`
- `ARVAN_SECRET_KEY`: طول 64
- endpoint: `https://s3.ir-thr-at1.arvanstorage.ir` · region: `ir-thr-at1` · bucket: `hamyar-e-man`

## ۲) اعتبار کلید (ListBuckets)

- ✅ موفق — 1 باکت: `hamyar-e-man`

## ۳) دسترسی به باکت `hamyar-e-man`

- ✅ HeadBucket موفق
- ✅ ListObjects روی `apk/` — 0 شیء

## ۴) نوشتن شیء آزمایشی

### ۴-الف) اثر تنظیم چک‌سام

- ❌ بدون اصلاح چک‌سام (s3v4 خام) — `InvalidArgument` (HTTP 400) — 
- ❌ با when_required — `InvalidArgument` (HTTP 400) — 

### ۴-ب) همان مسیر انتشار (هدرهای واقعی)

- ❌ PutObject ناموفق — `InvalidArgument` (HTTP 400) — 

## ۵) وضعیت APKهای موجود روی آروان

- (خالی)

## ۶) بازتولید مسیر publish_public_s3_object.py

```
S3 publish failed: RuntimeError: S3 PUT header preflight failed: minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
current bytes_match=False, public=False (bytes differ or absent)
S3 one-byte PUT preflight minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
