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
  "date": "Wed, 30 Sep 2026 02:02:24 GMT",
  "content-type": "application/xml",
  "content-length": "261",
  "connection": "keep-alive",
  "keep-alive": "timeout=65",
  "x-amz-request-id": "tx000000a8b6e9c65e85b34-006abc6db0-3164929517-ir-thr-at1",
  "x-robots-tag": "noindex, nofollow",
  "server": "ArvanCloud",
  "server-timing": "total;dur=472",
  "x-request-id": "b78580b8cbdd43dc180deb3d5006216e",
  "x-sid": "6980"
}
```

درخواستی که فرستاده شد:

```json

**خطای پیش‌بینی‌نشده:** `TypeError` — Object of type bytes is not JSON serializable
