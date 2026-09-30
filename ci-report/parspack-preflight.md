# آمادگی‌سنجی سرور داخلی

Access Key: طول 16، شروع با `D01d…` · Secret: طول 32

## ۱) کدام پیکربندی کار می‌کند؟

| پیکربندی | HeadBucket | PutObject |
|---|---|---|
| virtual روی parspack.net | ✅ | `SignatureDoesNotMatch` (HTTP 403) The request signatur |
| virtual + SigV2 | ✅ | ✅ |

**پیکربندی درست: virtual + SigV2**
- endpoint `https://parspack.net` · bucket `c539776` · addressing `virtual` · signature `s3` · region `us-east-1`

## ۲) نشانی عمومی

- ✅ `ACL=public-read` روی شیء پذیرفته شد
- ✅ **میزبان اختصاصی** → HTTP 200 — `https://c539776.parspack.net/_<redacted>.txt`
- ❌ **path روی endpoint** → HTTP 404 — `https://parspack.net/c539776/_<redacted>.txt`

**پایهٔ نشانی عمومی: `https://c539776.parspack.net`**

## ۳) وضعیت فعلی باکت

- ❌ `500` (HTTP 500) Internal Server Error

