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
<?xml version="1.0" encoding="UTF-8"?><Error><Code>InvalidArgument</Code><Message></Message><BucketName>hamyar-e-man</BucketName><RequestId>tx0000021c070df3b636360-006abc6e0a-3164252725-ir-thr-at1</RequestId><HostId>3164252725-ir-thr-at1-ir-thr</HostId></Error>
```

- **virtual-host** → HTTP 400

```xml
<?xml version="1.0" encoding="UTF-8"?><Error><Code>InvalidArgument</Code><Message></Message><BucketName>hamyar-e-man</BucketName><RequestId>tx00000df23bd76ca4c1ec5-006abc6e0b-3164332801-ir-thr-at1</RequestId><HostId>3164332801-ir-thr-at1-ir-thr</HostId></Error>
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
  "date": "Wed, 30 Sep 2026 02:03:56 GMT",
  "content-type": "application/xml",
  "content-length": "261",
  "connection": "keep-alive",
  "keep-alive": "timeout=65",
  "x-amz-request-id": "tx0000033f280179ba5c5df-006abc6e0c-3164929517-ir-thr-at1",
  "x-robots-tag": "noindex, nofollow",
  "server": "ArvanCloud",
  "server-timing": "total;dur=390",
  "x-request-id": "bec59a117ee149f7fabf2ae0ec0d4325",
  "x-sid": "6980"
}
```

درخواستی که فرستاده شد:

```json
{
  "method": "PUT",
  "url": "https://s3.ir-thr-at1.arvanstorage.ir/hamyar-e-man/apk/_diagnose/c651e9f77adb453cbdf41b56d957b6b6.bin",
  "headers": {
    "User-Agent": "Boto3/1.43.105 md/Botocore#1.43.105 ua/2.1 os/linux#6.8.0-1064-azure md/arch#x86_64 lang/python#3.12.14 md/pyimpl#CPython m/a,e,E,c,N cfg/retry-mode#standard Botocore/1.43.105",
    "Expect": "100-continue",
    "X-Amz-Date": "20260930T020356Z",
    "X-Amz-Content-SHA256": "<redacted>",
    "Authorization": "<redacted>",
    "amz-sdk-invocation-id": "6aeb1a5d-2a5f-4149-8ad8-6e62b8917c15",
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

## ۵) وضعیت APKهای موجود روی آروان

- (خالی)

## ۶) بازتولید مسیر publish_public_s3_object.py

```
S3 publish failed: RuntimeError: S3 PUT header preflight failed: minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
current bytes_match=False, public=False (bytes differ or absent)
S3 one-byte PUT preflight minimal=InvalidArgument: , content-type=InvalidArgument: , cache-control=InvalidArgument: , metadata=InvalidArgument: , combined=InvalidArgument: 
