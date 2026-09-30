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
<?xml version="1.0" encoding="UTF-8"?><Error><Code>InvalidArgument</Code><Message></Message><BucketName>hamyar-e-man</BucketName><RequestId>tx000006b0ce6b38bca406f-006abc6eeb-3164336851-ir-thr-at1</RequestId><HostId>3164336851-ir-thr-at1-ir-thr</HostId></Error>
```

- **virtual-host** → HTTP 400

```xml
<?xml version="1.0" encoding="UTF-8"?><Error><Code>InvalidArgument</Code><Message></Message><BucketName>hamyar-e-man</BucketName><RequestId>tx00000ccb605bd519103bf-006abc6eec-3164329961-ir-thr-at1</RequestId><HostId>3164329961-ir-thr-at1-ir-thr</HostId></Error>
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
  "date": "Wed, 30 Sep 2026 02:07:41 GMT",
  "content-type": "application/xml",
  "content-length": "261",
  "connection": "keep-alive",
  "keep-alive": "timeout=65",
  "x-amz-request-id": "tx0000076acb4c22e6d89b2-006abc6eed-3164195419-ir-thr-at1",
  "x-robots-tag": "noindex, nofollow",
  "server": "ArvanCloud",
  "server-timing": "total;dur=420",
  "x-request-id": "b7beaa0e54d60287cb71d3e64c26fb28",
  "x-sid": "6980"
}
```

درخواستی که فرستاده شد:

```json
{
  "method": "PUT",
  "url": "https://s3.ir-thr-at1.arvanstorage.ir/hamyar-e-man/apk/_diagnose/45f56280a55c4cb29fcb5b9d95f261b0.bin",
  "headers": {
    "User-Agent": "Boto3/1.43.105 md/Botocore#1.43.105 ua/2.1 os/linux#6.8.0-1064-azure md/arch#x86_64 lang/python#3.12.14 md/pyimpl#CPython m/N,E,e,a,c cfg/retry-mode#standard Botocore/1.43.105",
    "Expect": "100-continue",
    "X-Amz-Date": "20260930T020740Z",
    "X-Amz-Content-SHA256": "<redacted>",
    "Authorization": "<redacted>",
    "amz-sdk-invocation-id": "df6a80a7-19c1-4830-be0c-c86a8178edab",
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

- ✅ CreateBucket موفق (`hamyar-diag-d0a89483ceae`)
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
- ❌ **ir-thr-ba1** — `EndpointConnectionError` — Could not connect to the endpoint URL: "https://s3.ir-thr-ba1.arvanstorage.ir/hamyar-e-man/apk/_diagnose/f4600c12c2b341f7badaa8eff049a438.bin"
- ❌ **ir-bnd-ba1** — `EndpointConnectionError` — Could not connect to the endpoint URL: "https://s3.ir-bnd-ba1.arvanstorage.ir/hamyar-e-man/apk/_diagnose/db2654b858284259994a25666977214d.bin"

## ۵) وضعیت APKهای موجود روی آروان

- (خالی)

## ۶) بازتولید مسیر publish_public_s3_object.py

```
S3 publish failed: RuntimeError: S3 PUT header preflight failed: minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
current bytes_match=False, public=False (bytes differ or absent)
S3 one-byte PUT preflight minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
