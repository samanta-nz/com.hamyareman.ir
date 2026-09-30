# آمادگی‌سنجی سرور داخلی

مقصد: **پارس‌پک** · endpoint `https://c539776.parspack.net`
Access Key: طول 16، شروع با `D01d…` · Secret: طول 32

## ۱) کدام ترکیبِ «سبک نشانی × منطقهٔ امضا» کار می‌کند؟

| امضا | منطقه | ListBuckets |
|---|---|---|
| `s3v4` | `us-east-1` | ❌ `500` (HTTP 500) Internal Server Error |
| `s3v4` | `—` | ❌ `500` (HTTP 500) Internal Server Error |
| `s3` | `us-east-1` | ❌ `500` (HTTP 500) Internal Server Error |
| `s3` | `—` | ❌ `500` (HTTP 500) Internal Server Error |

### نگاه خام و ناشناس به سرور

- ریشهٔ endpoint: HTTP 200 — `Â <!DOCTYPE html> <html lang="en"> <head>     <meta charset="utf-8">     <meta name="viewport" content="width=device-width, initial-scale=1">      <title>ÙØ¶Ø§Û Ø°Ø®ÛØ±Ù Ø³Ø§Ø²Û Ø§Ø¨Ø±Û</title>      <link rel="stylesheet" href="/ade042da-593e-4376-aefa-bf4a16bf647a-error-pages.css"> </head> <b`
- مسیر باکت: HTTP 404 — `<?xml version="1.0" encoding="UTF-8"?> <Error><Code>NoSuchKey</Code><Message>The specified key does not exist.</Message><Key>hamyar-e-man</Key><BucketName>c539776</BucketName><Resource>/c539776/hamyar-e-man</Resource><RequestId>18D9FBB7662123F6</RequestId><HostId>dd9025bab4ad464b049177c95eb6ebf374d3`
- باکتِ قطعاً ناموجود: HTTP 404 — `<?xml version="1.0" encoding="UTF-8"?> <Error><Code>NoSuchKey</Code><Message>The specified key does not exist.</Message><Key>bucket-that-does-not-exist-x9</Key><BucketName>c539776</BucketName><Resource>/c539776/bucket-that-does-not-exist-x9</Resource><RequestId>18D9FBB7968F7CBA</RequestId><HostId>dd`

`ListBuckets` هیچ‌جا جواب نداد (بعضی ارائه‌دهنده‌ها آن را پشتیبانی نمی‌کنند).
سراغ عملیات سطح باکت می‌رویم.

## ۲) عملیات سطح باکت

باکت هدف: `hamyar-e-man`

| امضا | منطقه | HeadBucket | CreateBucket | PutObject |
|---|---|---|---|---|
| `s3v4` | `us-east-1` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |
| `s3v4` | `—` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |
| `s3` | `us-east-1` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |
| `s3` | `—` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |

**هیچ ترکیبی نتوانست بنویسد.** یا کلیدها اشتباه ذخیره شده‌اند (فاصله/کاراکتر جا افتاده)،
یا باکت باید اول در پنل پارس‌پک ساخته شود.
