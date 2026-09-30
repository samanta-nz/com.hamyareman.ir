# آمادگی‌سنجی سرور داخلی

مقصد: **پارس‌پک** · endpoint `https://c539776.parspack.net`
Access Key: طول 16، شروع با `D01d…` · Secret: طول 32

## ۱) کدام ترکیبِ «سبک نشانی × منطقهٔ امضا» کار می‌کند؟

| سبک | منطقه | ListBuckets | نتیجه |
|---|---|---|---|
| `path` | `us-east-1` | ❌ | `500` (HTTP 500) Internal Server Error |
| `path` | `default` | ❌ | `500` (HTTP 500) Internal Server Error |
| `path` | `ir-thr-c1` | ❌ | `500` (HTTP 500) Internal Server Error |
| `path` | `—` | ❌ | `500` (HTTP 500) Internal Server Error |
| `virtual` | `us-east-1` | ❌ | `500` (HTTP 500) Internal Server Error |
| `virtual` | `default` | ❌ | `500` (HTTP 500) Internal Server Error |
| `virtual` | `ir-thr-c1` | ❌ | `500` (HTTP 500) Internal Server Error |
| `virtual` | `—` | ❌ | `500` (HTTP 500) Internal Server Error |

`ListBuckets` هیچ‌جا جواب نداد (بعضی ارائه‌دهنده‌ها آن را پشتیبانی نمی‌کنند).
سراغ عملیات سطح باکت می‌رویم.

## ۲) عملیات سطح باکت

باکت هدف: `hamyar-e-man`

| سبک | منطقه | HeadBucket | CreateBucket | PutObject |
|---|---|---|---|---|
| `path` | `us-east-1` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |
| `path` | `default` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |
| `path` | `ir-thr-c1` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |
| `path` | `—` | `403` (HTTP 403) Forbidden | `SignatureDoesNotMatch` (HTTP 403) The request signature we  | — |
| `virtual` | `us-east-1` | `EndpointConnectionError` Could not connect to the endpoint  | `EndpointConnectionError` Could not connect to the endpoint  | — |
| `virtual` | `default` | `EndpointConnectionError` Could not connect to the endpoint  | `EndpointConnectionError` Could not connect to the endpoint  | — |
| `virtual` | `ir-thr-c1` | `EndpointConnectionError` Could not connect to the endpoint  | `EndpointConnectionError` Could not connect to the endpoint  | — |
| `virtual` | `—` | `EndpointConnectionError` Could not connect to the endpoint  | `EndpointConnectionError` Could not connect to the endpoint  | — |

**هیچ ترکیبی نتوانست بنویسد.** یا کلیدها اشتباه ذخیره شده‌اند (فاصله/کاراکتر جا افتاده)،
یا باکت باید اول در پنل پارس‌پک ساخته شود.
