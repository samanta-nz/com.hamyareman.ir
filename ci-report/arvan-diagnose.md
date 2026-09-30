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

### ۴-خام) PUT دستی‌امضاشده — متن کامل XML خطا

- **path-style** → HTTP 400

```xml
<?xml version="1.0" encoding="UTF-8"?><Error><Code>InvalidArgument</Code><Message></Message><BucketName>hamyar-e-man</BucketName><RequestId>tx0000009c702f1aa08b9fc-006abc701d-3164183704-ir-thr-at1</RequestId><HostId>3164183704-ir-thr-at1-ir-thr</HostId></Error>
```

- **virtual-host** → HTTP 400

```xml
<?xml version="1.0" encoding="UTF-8"?><Error><Code>InvalidArgument</Code><Message></Message><BucketName>hamyar-e-man</BucketName><RequestId>tx000003b00ebb886e44ca6-006abc701e-3164950168-ir-thr-at1</RequestId><HostId>3164950168-ir-thr-at1-ir-thr</HostId></Error>
```

### ۴-صفر) پاسخ خام آروان به یک PutObject کمینه

- کد: `InvalidArgument` · HTTP 400

```json
{
  "Error": {
    "Code": "InvalidArgument",
    "Message": null,
    "BucketName": "hamyar-e-man"
  },
  "extra": {}
}
```

پاسخ سرور (هدرها):

```json
{
  "date": "Wed, 30 Sep 2026 02:12:47 GMT",
  "content-type": "application/xml",
  "content-length": "261",
  "connection": "keep-alive",
  "keep-alive": "timeout=65",
  "x-amz-request-id": "tx00000cc47b542be7ecebf-006abc701f-3164905057-ir-thr-at1",
  "x-robots-tag": "noindex, nofollow",
  "server": "ArvanCloud",
  "server-timing": "total;dur=405",
  "x-request-id": "577eb62f5387d29dd38e1147a148ffb1",
  "x-sid": "6980"
}
```

درخواستی که فرستاده شد:

```json
{
  "method": "PUT",
  "url": "https://s3.ir-thr-at1.arvanstorage.ir/hamyar-e-man/apk/_diagnose/a147b01b3b5643a49e33a9e0047c433e.bin",
  "headers": {
    "User-Agent": "Boto3/1.43.105 md/Botocore#1.43.105 ua/2.1 os/linux#6.8.0-1064-azure md/arch#x86_64 lang/python#3.12.14 md/pyimpl#CPython m/c,e,N,E,a cfg/retry-mode#standard Botocore/1.43.105",
    "Expect": "100-continue",
    "X-Amz-Date": "20260930T021247Z",
    "X-Amz-Content-SHA256": "<redacted>",
    "Authorization": "<redacted>",
    "amz-sdk-invocation-id": "c331d9b3-b2e9-47bf-b4ce-915fea3f9837",
    "amz-sdk-request": "attempt=1",
    "Content-Length": "1"
  }
}
```

### ۴-الف) اثر تنظیم چک‌سام

- ❌ بدون اصلاح چک‌سام (s3v4 خام) — `InvalidArgument` (HTTP 400) — 
- ❌ با when_required — `InvalidArgument` (HTTP 400) — 

### ۴-ب) همان مسیر انتشار (هدرهای واقعی)

- ❌ PutObject ناموفق — `InvalidArgument` (HTTP 400) — 

## ۴-پ) حساب یا باکت؟ (تست قطعی)

اگر ساختن باکت تازه و نوشتن در آن کار کند، ایراد فقط مال باکت فعلی است؛
اگر آن هم رد شود، نوشتن در کل حساب بسته است (اعتبار/طرح/تعلیق).

- ✅ CreateBucket موفق (`hamyar-diag-b0b8c899a94d`)
- ✅ PutObject در باکت تازه موفق → **ایراد فقط مال باکت `hamyar-e-man` است**
- 🧹 باکت آزمایشی پاک شد

## ۴-ت) پیکربندی باکت `hamyar-e-man`

- **Versioning**: `{"Status": "Enabled", "MFADelete": "Disabled"}`
- **ACL**: `{"Owner": {"DisplayName": "behzad a", "ID": "6479490a-0b6d-4fd0-9d83-c2b55e0a21bd"}, "Grants": [{"Grantee": {"DisplayName": "behzad a", "ID": "6479490a-0b6d-4fd0-9d83-c2b55e0a21bd", "Type": "CanonicalUser"}, "Permission": "FULL_CONTROL"}]}`
- **Policy**: `NoSuchBucketPolicy` (HTTP 404) — The bucket policy does not exist
- **Lifecycle**: `{"Rules": [{"Expiration": {"Days": 2}, "ID": "abort-incomplete-mirror-uploads", "Filter": {"Prefix": ".staging/"}, "Status": "Enabled", "AbortIncompleteMultipartUpload": {"DaysAfterInitiation": 1}}, {"ID": "expire-old-versions", "Prefix": "", "Status": "Enabled", "NoncurrentVersionExpiration": {"NoncurrentDays": 30}}]}`
- **ObjectLock**: `ObjectLockConfigurationNotFoundError` (HTTP 404) — 

## ۴-ث) منطقهٔ باکت و تست روی همهٔ endpointهای آروان

- `GetBucketLocation` → `ir-thr-at1`

- ❌ **ir-thr-at1** — `InvalidArgument` (HTTP 400) — 
- ❌ **ir-tbz-sh1** — `RecursionError` — maximum recursion depth exceeded
- ❌ **ir-thr-ba1** — `EndpointConnectionError` — Could not connect to the endpoint URL: "https://s3.ir-thr-ba1.arvanstorage.ir/hamyar-e-man/apk/_diagnose/a6dd5df50a0a4df9995465ba272e4848.bin"
- ❌ **ir-bnd-ba1** — `EndpointConnectionError` — Could not connect to the endpoint URL: "https://s3.ir-bnd-ba1.arvanstorage.ir/hamyar-e-man/apk/_diagnose/3bf5d95e7979441485d32fa3194d9f52.bin"

## ۴-ج) آیا نسخه‌بندی مقصر است؟

- ✅ باکت تازه بدون نسخه‌بندی — نوشتن موفق
- ✅ نسخه‌بندی روشن شد
- ✅ نوشتن بعد از روشن‌کردن نسخه‌بندی هم موفق → **نسخه‌بندی مقصر نیست**
- 🧹 باکت آزمایشی پاک شد

## ۴-چ) حجم و تعداد اشیای باکت اصلی (بررسی سهمیه)

- نسخهٔ جاری: **275** شیء، **0.41 GB**
- با همهٔ نسخه‌های قدیمی: **277** نسخه، **0.53 GB**

## ۴-ح) بازنویسی شیء موجود در برابر ساخت شیء تازه

اگر بازنویسی کار کند ولی کلید تازه نه، یعنی **سقف تعداد اشیای باکت** پر شده است.

- شیء آزمایشی: `html ها/آموزشگاه/26-fake-news.html` (4853 بایت)
- ❌ بازنویسی هم رد شد — `InvalidArgument` (HTTP 400) — 
- → کل نوشتن روی این باکت بسته است (تنظیم سمت آروان).

## ۵) وضعیت APKهای موجود روی آروان

- (خالی)

## ۶) بازتولید مسیر publish_public_s3_object.py

```
S3 publish failed: RuntimeError: S3 PUT header preflight failed: minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
current bytes_match=False, public=False (bytes differ or absent)
S3 one-byte PUT preflight minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
