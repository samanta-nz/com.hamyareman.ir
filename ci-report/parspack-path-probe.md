# کدام شکل آدرس پارس‌پک کار می‌کند؟

| فایل | شکل | کد | نوع | HMK1 |
|---|---|---|---|---|
| `Bucket/Pdf-files/G09/g9-arabic/g9-arabic-ind` | A: host + key | 206 | binary/octet-stream | ❌ |
| `Bucket/Pdf-files/G09/g9-arabic/g9-arabic-ind` | B: host + bucket + key | 206 | binary/octet-stream | ❌ |
| `Bucket/Pdf-files/G09/g9-arabic/g9-arabic-ind` | C: endpoint + bucket+key | 404 | text/html; charset=utf-8 | — |
| `Bucket/Pdf-files/G09/g9-arabic/g9-arabic-ind` | D: s3 host | 404 | text/html | — |
| `Bucket/Pdf-files/G09/g9-math/menu.json` | A: host + key | 206 | binary/octet-stream | ❌ |
| `Bucket/Pdf-files/G09/g9-math/menu.json` | B: host + bucket + key | 206 | binary/octet-stream | ❌ |
| `Bucket/Pdf-files/G09/g9-math/menu.json` | C: endpoint + bucket+key | 404 | text/html; charset=utf-8 | — |
| `Bucket/Pdf-files/G09/g9-math/menu.json` | D: s3 host | 404 | text/html | — |
| `Bucket/Html-files/آموزشگاه/26-fake-news.html` | A: host + key | 206 | binary/octet-stream | ✅ |
| `Bucket/Html-files/آموزشگاه/26-fake-news.html` | B: host + bucket + key | 206 | binary/octet-stream | ✅ |
| `Bucket/Html-files/آموزشگاه/26-fake-news.html` | C: endpoint + bucket+key | TooManyRedirects | — | — |
| `Bucket/Html-files/آموزشگاه/26-fake-news.html` | D: s3 host | 404 | text/html | — |
| `Bucket/Html-files/01 - حالت کودک.html` | A: host + key | 206 | binary/octet-stream | ✅ |
| `Bucket/Html-files/01 - حالت کودک.html` | B: host + bucket + key | 206 | binary/octet-stream | ✅ |
| `Bucket/Html-files/01 - حالت کودک.html` | C: endpoint + bucket+key | TooManyRedirects | — | — |
| `Bucket/Html-files/01 - حالت کودک.html` | D: s3 host | 404 | text/html | — |
| `Bucket/Html-files/background-music.html` | A: host + key | 206 | binary/octet-stream | ✅ |
| `Bucket/Html-files/background-music.html` | B: host + bucket + key | 206 | binary/octet-stream | ✅ |
| `Bucket/Html-files/background-music.html` | C: endpoint + bucket+key | 404 | text/html; charset=utf-8 | — |
| `Bucket/Html-files/background-music.html` | D: s3 host | 404 | text/html | — |

**شکل‌های موفق:** ['A: host + key', 'B: host + bucket + key']
**رمزشده (HMK1):** 6 از 10
