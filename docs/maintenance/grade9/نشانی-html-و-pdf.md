# نشانی همهٔ HTMLها و PDFهای اپ

این فهرست خودکار از دو دارایی خود اپ ساخته شده است (`assets/content/server-map.json` و `assets/content/catalog.json`) و نشانی‌ها با همان کدی که `ServerResolver` روی گوشی اجرا می‌کند ساخته شده‌اند. برای بازتولید:

```bash
python3 scripts/list_content_urls.py
```

## خلاصه

| مورد | مقدار |
|---|---|
| فایل HTML | **168** |
| فایل PDF | **28** |
| جمع | **196** |
| حجم کل | **199.4 MB** |
| سرور داخلی | `https://c539776.parspack.net` (پارس‌پک، باکت `c539776`) |
| سرور خارجی | `https://sgp.cloud.appwrite.io/v1` (Appwrite، باکت `6abb564d00155cc56d65`) |

هر فایل روی **هر دو** سرور هست. اپ در حالت «سریع‌ترین» اول سروری را
می‌زند که در سنجش Range برنده شده و دیگری fallback است.

نشانی سرور خارجی از روی شناسه ساخته می‌شود، پس در جدول‌ها تکرار نشده:

```
https://sgp.cloud.appwrite.io/v1/storage/buckets/6abb564d00155cc56d65/files/<شناسه>/view?project=6abb134a002025222005
```

ستون کامل نشانی خارجی در فایل CSV کنار همین سند هست.

### نکتهٔ مهم دربارهٔ `common/mirror`

از ۱۶۸ فایل HTML، ۷۳ تا زیر `common/mirror` با نام تخت نشسته‌اند و
محتوای همان درس‌های `html ها` هستند، فقط با شناسهٔ Appwrite جدا. پس
تعداد درس‌های **یکتا** ۹۵ تاست (۸۰ در `html ها` + ۱۵ در
`grades/grade9/study`) و بقیه مسیر دوم همان‌هاست.

همین‌طور ۲۵ از ۲۸ فایل PDF زیر `PDF ها` و ۳ تای باقی‌مانده (فصل ۸
ریاضی) زیر `grades/grade9/study` هستند.

هر ۱۹۶ نشانی در اجرای تأیید مهاجرت، ناشناس و با اندازهٔ درست خوانده
شدند — گزارش: `ci-report/parspack-verify.md`.

## فهرست پوشه‌ها

| پوشه | تعداد | حجم |
|---|---:|---:|
| `PDF ها` | 25 | 7.7 MB |
| `common/mirror` | 73 | 84.4 MB |
| `grades/grade9/study` | 18 | 1.5 MB |
| `html ها/آزمایشگاه` | 3 | 945 KB |
| `html ها/آموزشگاه` | 34 | 28.7 MB |
| `html ها/تمرینات تنفسی` | 8 | 6.0 MB |
| `html ها/جعبه ابزار ریاضی` | 2 | 115 KB |
| `html ها/جعبه ابزار عمومی` | 3 | 164 KB |
| `html ها/حرکات ورزشی` | 15 | 32.0 MB |
| `html ها/یوگا` | 15 | 37.9 MB |

## `PDF ها` — کتاب‌ها و جزوه‌های PDF

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `C905-fehrest.pdf` | فهرست ریاضی نهم | 144 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905-fehrest.pdf> |
| 2 | `C905f01d01.pdf` | ریاضی نهم — فصل 01، درس 01 | 151 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f01d01.pdf> |
| 3 | `C905f01d02.pdf` | ریاضی نهم — فصل 01، درس 02 | 227 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f01d02.pdf> |
| 4 | `C905f01d03.pdf` | ریاضی نهم — فصل 01، درس 03 | 221 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f01d03.pdf> |
| 5 | `C905f01d04.pdf` | ریاضی نهم — فصل 01، درس 04 | 245 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f01d04.pdf> |
| 6 | `C905f02d01.pdf` | ریاضی نهم — فصل 02، درس 01 | 252 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f02d01.pdf> |
| 7 | `C905f02d02.pdf` | ریاضی نهم — فصل 02، درس 02 | 311 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f02d02.pdf> |
| 8 | `C905f02d03.pdf` | ریاضی نهم — فصل 02، درس 03 | 278 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f02d03.pdf> |
| 9 | `C905f03d01.pdf` | ریاضی نهم — فصل 03، درس 01 | 122 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f03d01.pdf> |
| 10 | `C905f03d02.pdf` | ریاضی نهم — فصل 03، درس 02 | 257 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f03d02.pdf> |
| 11 | `C905f03d03.pdf` | ریاضی نهم — فصل 03، درس 03 | 226 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f03d03.pdf> |
| 12 | `C905f03d04.pdf` | ریاضی نهم — فصل 03، درس 04 | 198 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f03d04.pdf> |
| 13 | `C905f03d05.pdf` | ریاضی نهم — فصل 03، درس 05 | 354 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f03d05.pdf> |
| 14 | `C905f04d01.pdf` | ریاضی نهم — فصل 04، درس 01 | 273 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f04d01.pdf> |
| 15 | `C905f04d02.pdf` | ریاضی نهم — فصل 04، درس 02 | 159 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f04d02.pdf> |
| 16 | `C905f04d03.pdf` | ریاضی نهم — فصل 04، درس 03 | 233 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f04d03.pdf> |
| 17 | `C905f04d04.pdf` | ریاضی نهم — فصل 04، درس 04 | 372 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f04d04.pdf> |
| 18 | `C905f05d01.pdf` | ریاضی نهم — فصل 05، درس 01 | 251 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f05d01.pdf> |
| 19 | `C905f05d02.pdf` | ریاضی نهم — فصل 05، درس 02 | 197 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f05d02.pdf> |
| 20 | `C905f05d03.pdf` | ریاضی نهم — فصل 05، درس 03 | 414 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f05d03.pdf> |
| 21 | `C905f06d01.pdf` | ریاضی نهم — فصل 06، درس 01 | 215 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f06d01.pdf> |
| 22 | `C905f06d02.pdf` | ریاضی نهم — فصل 06، درس 02 | 269 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f06d02.pdf> |
| 23 | `C905f06d03.pdf` | ریاضی نهم — فصل 06، درس 03 | 2.0 MB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f06d03.pdf> |
| 24 | `C905f07d01.pdf` | ریاضی نهم — فصل 07، درس 01 | 242 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f07d01.pdf> |
| 25 | `C905f07d03.pdf` | ریاضی نهم — فصل 07، درس 03 | 242 KB | <https://c539776.parspack.net/PDF%20%D9%87%D8%A7/C905f07d03.pdf> |

## `common/mirror` — آینهٔ محتوای مشترک (همان HTMLهای بالا، مسیر دوم)

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `Amzshghh-01-speed-reading.html` | Amzshghh-01-speed-reading.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-01-speed-reading.html> |
| 2 | `Amzshghh-02-handwriting.html` | Amzshghh-02-handwriting.html | 7 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-02-handwriting.html> |
| 3 | `Amzshghh-03-touch-typing.html` | Amzshghh-03-touch-typing.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-03-touch-typing.html> |
| 4 | `Amzshghh-04-note-taking.html` | Amzshghh-04-note-taking.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-04-note-taking.html> |
| 5 | `Amzshghh-05-summarizing.html` | Amzshghh-05-summarizing.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-05-summarizing.html> |
| 6 | `Amzshghh-06-debate-principles.html` | Amzshghh-06-debate-principles.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-06-debate-principles.html> |
| 7 | `Amzshghh-07-logical-fallacies.html` | Amzshghh-07-logical-fallacies.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-07-logical-fallacies.html> |
| 8 | `Amzshghh-08-public-speaking.html` | Amzshghh-08-public-speaking.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-08-public-speaking.html> |
| 9 | `Amzshghh-09-speech-anxiety.html` | Amzshghh-09-speech-anxiety.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-09-speech-anxiety.html> |
| 10 | `Amzshghh-10-formal-email.html` | Amzshghh-10-formal-email.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-10-formal-email.html> |
| 11 | `Amzshghh-11-active-listening.html` | Amzshghh-11-active-listening.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-11-active-listening.html> |
| 12 | `Amzshghh-12-mental-math.html` | Amzshghh-12-mental-math.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-12-mental-math.html> |
| 13 | `Amzshghh-13-memory-boost.html` | Amzshghh-13-memory-boost.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-13-memory-boost.html> |
| 14 | `Amzshghh-14-problem-solving.html` | Amzshghh-14-problem-solving.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-14-problem-solving.html> |
| 15 | `Amzshghh-15-relationships.html` | Amzshghh-15-relationships.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-15-relationships.html> |
| 16 | `Amzshghh-16-self-awareness.html` | Amzshghh-16-self-awareness.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-16-self-awareness.html> |
| 17 | `Amzshghh-17-mental-traps.html` | Amzshghh-17-mental-traps.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-17-mental-traps.html> |
| 18 | `Amzshghh-18-resilience.html` | Amzshghh-18-resilience.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-18-resilience.html> |
| 19 | `Amzshghh-19-daily-planning.html` | Amzshghh-19-daily-planning.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-19-daily-planning.html> |
| 20 | `Amzshghh-20-habit-building.html` | Amzshghh-20-habit-building.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-20-habit-building.html> |
| 21 | `Amzshghh-21-study-space.html` | Amzshghh-21-study-space.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-21-study-space.html> |
| 22 | `Amzshghh-22-windows-basics.html` | Amzshghh-22-windows-basics.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-22-windows-basics.html> |
| 23 | `Amzshghh-23-ai-literacy.html` | Amzshghh-23-ai-literacy.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-23-ai-literacy.html> |
| 24 | `Amzshghh-24-smart-search.html` | Amzshghh-24-smart-search.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-24-smart-search.html> |
| 25 | `Amzshghh-25-online-safety.html` | Amzshghh-25-online-safety.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-25-online-safety.html> |
| 26 | `Amzshghh-26-fake-news.html` | Amzshghh-26-fake-news.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-26-fake-news.html> |
| 27 | `Amzshghh-27-teamwork.html` | Amzshghh-27-teamwork.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-27-teamwork.html> |
| 28 | `Amzshghh-28-conflict-friends.html` | Amzshghh-28-conflict-friends.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-28-conflict-friends.html> |
| 29 | `Amzshghh-29-saying-no.html` | Amzshghh-29-saying-no.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-29-saying-no.html> |
| 30 | `Amzshghh-30-budgeting.html` | Amzshghh-30-budgeting.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-30-budgeting.html> |
| 31 | `Amzshghh-31-needs-vs-wants.html` | Amzshghh-31-needs-vs-wants.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-31-needs-vs-wants.html> |
| 32 | `Amzshghh-32-brainstorming.html` | Amzshghh-32-brainstorming.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-32-brainstorming.html> |
| 33 | `Amzshghh-33-creative-thinking.html` | Amzshghh-33-creative-thinking.html | 5 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-33-creative-thinking.html> |
| 34 | `Amzshghh-34-music-theory.html` | Amzshghh-34-music-theory.html | 6 KB | <https://c539776.parspack.net/common/mirror/Amzshghh-34-music-theory.html> |
| 35 | `Background-music.html` | Background-music.html | 8.2 MB | <https://c539776.parspack.net/common/mirror/Background-music.html> |
| 36 | `br-01.html` | br-01.html | 801 KB | <https://c539776.parspack.net/common/mirror/br-01.html> |
| 37 | `br-02.html` | br-02.html | 659 KB | <https://c539776.parspack.net/common/mirror/br-02.html> |
| 38 | `br-03.html` | br-03.html | 901 KB | <https://c539776.parspack.net/common/mirror/br-03.html> |
| 39 | `br-04.html` | br-04.html | 774 KB | <https://c539776.parspack.net/common/mirror/br-04.html> |
| 40 | `br-05.html` | br-05.html | 781 KB | <https://c539776.parspack.net/common/mirror/br-05.html> |
| 41 | `br-06.html` | br-06.html | 722 KB | <https://c539776.parspack.net/common/mirror/br-06.html> |
| 42 | `br-07.html` | br-07.html | 732 KB | <https://c539776.parspack.net/common/mirror/br-07.html> |
| 43 | `br-08.html` | br-08.html | 803 KB | <https://c539776.parspack.net/common/mirror/br-08.html> |
| 44 | `ex-01.html` | ex-01.html | 1.8 MB | <https://c539776.parspack.net/common/mirror/ex-01.html> |
| 45 | `ex-02.html` | ex-02.html | 1.6 MB | <https://c539776.parspack.net/common/mirror/ex-02.html> |
| 46 | `ex-03.html` | ex-03.html | 2.1 MB | <https://c539776.parspack.net/common/mirror/ex-03.html> |
| 47 | `ex-04.html` | ex-04.html | 2.0 MB | <https://c539776.parspack.net/common/mirror/ex-04.html> |
| 48 | `ex-05.html` | ex-05.html | 3.0 MB | <https://c539776.parspack.net/common/mirror/ex-05.html> |
| 49 | `ex-06.html` | ex-06.html | 1.9 MB | <https://c539776.parspack.net/common/mirror/ex-06.html> |
| 50 | `ex-07.html` | ex-07.html | 3.2 MB | <https://c539776.parspack.net/common/mirror/ex-07.html> |
| 51 | `ex-08.html` | ex-08.html | 1.7 MB | <https://c539776.parspack.net/common/mirror/ex-08.html> |
| 52 | `ex-09.html` | ex-09.html | 1.8 MB | <https://c539776.parspack.net/common/mirror/ex-09.html> |
| 53 | `ex-10.html` | ex-10.html | 2.2 MB | <https://c539776.parspack.net/common/mirror/ex-10.html> |
| 54 | `ex-11.html` | ex-11.html | 2.1 MB | <https://c539776.parspack.net/common/mirror/ex-11.html> |
| 55 | `ex-12.html` | ex-12.html | 1.7 MB | <https://c539776.parspack.net/common/mirror/ex-12.html> |
| 56 | `ex-13.html` | ex-13.html | 3.2 MB | <https://c539776.parspack.net/common/mirror/ex-13.html> |
| 57 | `ex-14.html` | ex-14.html | 2.2 MB | <https://c539776.parspack.net/common/mirror/ex-14.html> |
| 58 | `ex-15.html` | ex-15.html | 1.5 MB | <https://c539776.parspack.net/common/mirror/ex-15.html> |
| 59 | `yg-01.html` | yg-01.html | 1.7 MB | <https://c539776.parspack.net/common/mirror/yg-01.html> |
| 60 | `yg-02.html` | yg-02.html | 2.1 MB | <https://c539776.parspack.net/common/mirror/yg-02.html> |
| 61 | `yg-03.html` | yg-03.html | 2.0 MB | <https://c539776.parspack.net/common/mirror/yg-03.html> |
| 62 | `yg-04.html` | yg-04.html | 2.5 MB | <https://c539776.parspack.net/common/mirror/yg-04.html> |
| 63 | `yg-05.html` | yg-05.html | 3.0 MB | <https://c539776.parspack.net/common/mirror/yg-05.html> |
| 64 | `yg-06.html` | yg-06.html | 3.0 MB | <https://c539776.parspack.net/common/mirror/yg-06.html> |
| 65 | `yg-07.html` | yg-07.html | 2.3 MB | <https://c539776.parspack.net/common/mirror/yg-07.html> |
| 66 | `yg-08.html` | yg-08.html | 1.8 MB | <https://c539776.parspack.net/common/mirror/yg-08.html> |
| 67 | `yg-09.html` | yg-09.html | 2.8 MB | <https://c539776.parspack.net/common/mirror/yg-09.html> |
| 68 | `yg-10.html` | yg-10.html | 2.0 MB | <https://c539776.parspack.net/common/mirror/yg-10.html> |
| 69 | `yg-11.html` | yg-11.html | 3.4 MB | <https://c539776.parspack.net/common/mirror/yg-11.html> |
| 70 | `yg-12.html` | yg-12.html | 2.8 MB | <https://c539776.parspack.net/common/mirror/yg-12.html> |
| 71 | `yg-13.html` | yg-13.html | 3.5 MB | <https://c539776.parspack.net/common/mirror/yg-13.html> |
| 72 | `yg-14.html` | yg-14.html | 2.7 MB | <https://c539776.parspack.net/common/mirror/yg-14.html> |
| 73 | `yg-15.html` | yg-15.html | 2.2 MB | <https://c539776.parspack.net/common/mirror/yg-15.html> |

## `grades/grade9/study` — HTMLهای درسی پایهٔ نهم

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `C905f08d01.pdf` | C905f08d01.pdf | 183 KB | <https://c539776.parspack.net/grades/grade9/study/C905f08d01.pdf> |
| 2 | `C905f08d02.pdf` | C905f08d02.pdf | 274 KB | <https://c539776.parspack.net/grades/grade9/study/C905f08d02.pdf> |
| 3 | `C905f08d03.pdf` | C905f08d03.pdf | 282 KB | <https://c539776.parspack.net/grades/grade9/study/C905f08d03.pdf> |
| 4 | `ryazif01d01.html` | ryazif01d01.html | 58 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif01d01.html> |
| 5 | `ryazif01d02.html` | ryazif01d02.html | 61 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif01d02.html> |
| 6 | `ryazif01d03.html` | ryazif01d03.html | 72 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif01d03.html> |
| 7 | `ryazif01d04.html` | ryazif01d04.html | 62 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif01d04.html> |
| 8 | `ryazif01review.html` | ryazif01review.html | 56 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif01review.html> |
| 9 | `ryazif02d01.html` | ryazif02d01.html | 59 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif02d01.html> |
| 10 | `ryazif02d02.html` | ryazif02d02.html | 50 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif02d02.html> |
| 11 | `ryazif02d03.html` | ryazif02d03.html | 55 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif02d03.html> |
| 12 | `ryazif02review.html` | ryazif02review.html | 48 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif02review.html> |
| 13 | `ryazif03d01.html` | ryazif03d01.html | 55 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif03d01.html> |
| 14 | `ryazif03d02.html` | ryazif03d02.html | 67 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif03d02.html> |
| 15 | `ryazif03d03.html` | ryazif03d03.html | 41 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif03d03.html> |
| 16 | `ryazif03d04.html` | ryazif03d04.html | 32 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif03d04.html> |
| 17 | `ryazif03d05.html` | ryazif03d05.html | 43 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif03d05.html> |
| 18 | `ryazif03review.html` | ryazif03review.html | 52 KB | <https://c539776.parspack.net/grades/grade9/study/ryazif03review.html> |

## `html ها/آزمایشگاه` — آزمایشگاه

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `lab-09-biology.html` | Hamyar e Man - آزمایشگاه زیست پایه نهم | 457 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D8%B2%D9%85%D8%A7%DB%8C%D8%B4%DA%AF%D8%A7%D9%87/lab-09-biology.html> |
| 2 | `lab-09-chemistry.html` | Hamyar e Man - آزمایشگاه شیمی پایه نهم | 252 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D8%B2%D9%85%D8%A7%DB%8C%D8%B4%DA%AF%D8%A7%D9%87/lab-09-chemistry.html> |
| 3 | `lab-09-physics.html` | Hamyar e Man - آزمایشگاه فیزیک پایه نهم | 236 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D8%B2%D9%85%D8%A7%DB%8C%D8%B4%DA%AF%D8%A7%D9%87/lab-09-physics.html> |

## `html ها/آموزشگاه` — آموزشگاه مهارت

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `amz-01` | سرعت خواندن | 6.8 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/01-speed-reading.html> |
| 2 | `amz-02` | خوشنویسی | 13.3 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/02-handwriting.html> |
| 3 | `amz-03` | تایپ لمسی | 8.5 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/03-touch-typing.html> |
| 4 | `amz-04` | یادداشت‌برداری | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/04-note-taking.html> |
| 5 | `amz-05` | خلاصه‌نویسی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/05-summarizing.html> |
| 6 | `amz-06` | اصول مناظره | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/06-debate-principles.html> |
| 7 | `amz-07` | مغالطه‌های منطقی | 6 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/07-logical-fallacies.html> |
| 8 | `amz-08` | فن بیان | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/08-public-speaking.html> |
| 9 | `amz-09` | اضطراب سخنرانی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/09-public-speaking-anxiety.html> |
| 10 | `amz-10` | ایمیل رسمی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/10-formal-email.html> |
| 11 | `amz-11` | گوش دادن فعال | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/11-active-listening.html> |
| 12 | `amz-12` | محاسبات ذهنی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/12-mental-math.html> |
| 13 | `amz-13` | تقویت حافظه | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/13-memory-boost.html> |
| 14 | `amz-14` | حل مسئله | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/14-problem-solving.html> |
| 15 | `amz-15` | مهارت روابط | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/15-relationships.html> |
| 16 | `amz-16` | خودآگاهی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/16-self-awareness.html> |
| 17 | `amz-17` | تله‌های ذهنی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/17-mental-traps.html> |
| 18 | `amz-18` | انعطاف‌پذیری | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/18-resilience.html> |
| 19 | `amz-19` | برنامه‌ریزی روزانه | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/19-daily-planning.html> |
| 20 | `amz-20` | ساخت عادت | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/20-habit-building.html> |
| 21 | `amz-21` | فضای مطالعه | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/21-study-space.html> |
| 22 | `amz-22` | ویندوز (مبانی) | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/22-windows-basics.html> |
| 23 | `amz-23` | سواد هوش مصنوعی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/23-ai-literacy.html> |
| 24 | `amz-24` | جست‌وجوی هوشمند | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/24-smart-search.html> |
| 25 | `amz-25` | ایمنی در فضای مجازی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/25-online-safety.html> |
| 26 | `amz-26` | اخبار جعلی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/26-fake-news.html> |
| 27 | `amz-27` | کار تیمی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/27-teamwork.html> |
| 28 | `amz-28` | مدیریت اختلاف با دوستان | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/28-conflict-with-friends.html> |
| 29 | `amz-29` | هنر نه گفتن | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/29-saying-no.html> |
| 30 | `amz-30` | مدیریت بودجه | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/30-budgeting.html> |
| 31 | `amz-31` | نیاز در برابر خواسته | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/31-needs-vs-wants.html> |
| 32 | `amz-32` | طوفان فکری | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/32-brainstorming.html> |
| 33 | `amz-33` | تفکر خلاق | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/33-creative-thinking.html> |
| 34 | `amz-34` | تئوری موسیقی | 5 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%A2%D9%85%D9%88%D8%B2%D8%B4%DA%AF%D8%A7%D9%87/34-music-theory.html> |

## `html ها/تمرینات تنفسی` — تمرین‌های تنفسی

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `bre-01` | تنفس آرام هنگام قاعدگی | 802 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%D8%AA%D9%86%D9%81%D8%B3%20%D8%A2%D8%B1%D8%A7%D9%85%20%D9%87%D9%86%DA%AF%D8%A7%D9%85%20%D9%82%D8%A7%D8%B9%D8%AF%DA%AF%DB%8C.html> |
| 2 | `bre-02` | تنفس آرام پیش از خواب | 732 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%D8%AA%D9%86%D9%81%D8%B3%20%D8%A2%D8%B1%D8%A7%D9%85%20%D9%BE%DB%8C%D8%B4%20%D8%A7%D8%B2%20%D8%AE%D9%88%D8%A7%D8%A8.html> |
| 3 | `bre-03` | تنفس بینی متناوب | 901 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%D8%AA%D9%86%D9%81%D8%B3%20%D8%A8%DB%8C%D9%86%DB%8C%20%D9%85%D8%AA%D9%86%D8%A7%D9%88%D8%A8.html> |
| 4 | `bre-04` | تنفس راحت | 659 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%D8%AA%D9%86%D9%81%D8%B3%20%D8%B1%D8%A7%D8%AD%D8%AA.html> |
| 5 | `bre-05` | تنفس شمارشی پیش از امتحان | 721 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%D8%AA%D9%86%D9%81%D8%B3%20%D8%B4%D9%85%D8%A7%D8%B1%D8%B4%DB%8C%20%D9%BE%DB%8C%D8%B4%20%D8%A7%D8%B2%20%D8%A7%D9%85%D8%AA%D8%AD%D8%A7%D9%86.html> |
| 6 | `bre-06` | تنفس شکمی | 800 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%D8%AA%D9%86%D9%81%D8%B3%20%D8%B4%DA%A9%D9%85%DB%8C.html> |
| 7 | `bre-07` | تنفس شیر ملایم | 774 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%D8%AA%D9%86%D9%81%D8%B3%20%D8%B4%DB%8C%D8%B1%20%D9%85%D9%84%D8%A7%DB%8C%D9%85.html> |
| 8 | `bre-08` | یک دقیقه توجه به نفس | 780 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AA%D9%85%D8%B1%DB%8C%D9%86%D8%A7%D8%AA%20%D8%AA%D9%86%D9%81%D8%B3%DB%8C/%DB%8C%DA%A9%20%D8%AF%D9%82%DB%8C%D9%82%D9%87%20%D8%AA%D9%88%D8%AC%D9%87%20%D8%A8%D9%87%20%D9%86%D9%81%D8%B3.html> |

## `html ها/جعبه ابزار ریاضی` — جعبه‌ابزار ریاضی

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `tool-casio991.html` | CASIO fx-991CW ClassWiz | 51 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AC%D8%B9%D8%A8%D9%87%20%D8%A7%D8%A8%D8%B2%D8%A7%D8%B1%20%D8%B1%DB%8C%D8%A7%D8%B6%DB%8C/tool-casio991.html> |
| 2 | `tool-ti-nspire.html` | TI-Nspire CX II-T CAS | 64 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AC%D8%B9%D8%A8%D9%87%20%D8%A7%D8%A8%D8%B2%D8%A7%D8%B1%20%D8%B1%DB%8C%D8%A7%D8%B6%DB%8C/tool-ti-nspire.html> |

## `html ها/جعبه ابزار عمومی` — جعبه‌ابزار عمومی

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `tool-calendar.html` | تقویم | 91 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AC%D8%B9%D8%A8%D9%87%20%D8%A7%D8%A8%D8%B2%D8%A7%D8%B1%20%D8%B9%D9%85%D9%88%D9%85%DB%8C/tool-calendar.html> |
| 2 | `tool-converter.html` | مبدل همه‌کاره مهندسی | 36 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AC%D8%B9%D8%A8%D9%87%20%D8%A7%D8%A8%D8%B2%D8%A7%D8%B1%20%D8%B9%D9%85%D9%88%D9%85%DB%8C/tool-converter.html> |
| 3 | `tool-dj120d.html` | DJ-120D Plus | 37 KB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AC%D8%B9%D8%A8%D9%87%20%D8%A7%D8%A8%D8%B2%D8%A7%D8%B1%20%D8%B9%D9%85%D9%88%D9%85%DB%8C/tool-dj120d.html> |

## `html ها/حرکات ورزشی` — حرکات ورزشی

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `spo-01` | اسکوات آرام | 1.8 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%D8%A7%D8%B3%DA%A9%D9%88%D8%A7%D8%AA%20%D8%A2%D8%B1%D8%A7%D9%85.html> |
| 2 | `spo-02` | بالا بردن پاشنه‌ها | 1.6 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%D8%A8%D8%A7%D9%84%D8%A7%20%D8%A8%D8%B1%D8%AF%D9%86%20%D9%BE%D8%A7%D8%B4%D9%86%D9%87%E2%80%8C%D9%87%D8%A7.html> |
| 3 | `spo-03` | جامپینگ‌جک بدون پرش | 2.1 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%D8%AC%D8%A7%D9%85%D9%BE%DB%8C%D9%86%DA%AF%E2%80%8C%D8%AC%DA%A9%20%D8%A8%D8%AF%D9%88%D9%86%20%D9%BE%D8%B1%D8%B4.html> |
| 4 | `spo-04` | سوپرمنِ دست‌وپای مخالف | 2.0 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%D8%B3%D9%88%D9%BE%D8%B1%D9%85%D9%86%D9%90%20%D8%AF%D8%B3%D8%AA%E2%80%8C%D9%88%D9%BE%D8%A7%DB%8C%20%D9%85%D8%AE%D8%A7%D9%84%D9%81.html> |
| 5 | `spo-05` | لانگز آرام | 3.0 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%D9%84%D8%A7%D9%86%DA%AF%D8%B2%20%D8%A2%D8%B1%D8%A7%D9%85.html> |
| 6 | `spo-06` | پلانک روی زانو | 1.9 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%D9%BE%D9%84%D8%A7%D9%86%DA%A9%20%D8%B1%D9%88%DB%8C%20%D8%B2%D8%A7%D9%86%D9%88.html> |
| 7 | `spo-07` | چرخش مچ دست و پا | 3.2 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%86%D8%B1%D8%AE%D8%B4%20%D9%85%DA%86%20%D8%AF%D8%B3%D8%AA%20%D9%88%20%D9%BE%D8%A7.html> |
| 8 | `spo-08` | چرخش کوچک تنه | 1.7 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%86%D8%B1%D8%AE%D8%B4%20%DA%A9%D9%88%DA%86%DA%A9%20%D8%AA%D9%86%D9%87.html> |
| 9 | `spo-09` | کرانچ ملایم | 1.8 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%A9%D8%B1%D8%A7%D9%86%DA%86%20%D9%85%D9%84%D8%A7%DB%8C%D9%85.html> |
| 10 | `spo-10` | کشش ساق به دیوار | 2.2 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%A9%D8%B4%D8%B4%20%D8%B3%D8%A7%D9%82%20%D8%A8%D9%87%20%D8%AF%DB%8C%D9%88%D8%A7%D8%B1.html> |
| 11 | `spo-11` | کشش شانهٔ ضربدری | 2.1 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%A9%D8%B4%D8%B4%20%D8%B4%D8%A7%D9%86%D9%87%D9%94%20%D8%B6%D8%B1%D8%A8%D8%AF%D8%B1%DB%8C.html> |
| 12 | `spo-12` | کشش ملایم گردن | 1.7 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%A9%D8%B4%D8%B4%20%D9%85%D9%84%D8%A7%DB%8C%D9%85%20%DA%AF%D8%B1%D8%AF%D9%86.html> |
| 13 | `spo-13` | کشش مچ و ساعد | 3.2 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%A9%D8%B4%D8%B4%20%D9%85%DA%86%20%D9%88%20%D8%B3%D8%A7%D8%B9%D8%AF.html> |
| 14 | `spo-14` | کشش همسترینگ نشسته | 2.2 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%A9%D8%B4%D8%B4%20%D9%87%D9%85%D8%B3%D8%AA%D8%B1%DB%8C%D9%86%DA%AF%20%D9%86%D8%B4%D8%B3%D8%AA%D9%87.html> |
| 15 | `spo-15` | کشش پروانه | 1.5 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%D8%AD%D8%B1%DA%A9%D8%A7%D8%AA%20%D9%88%D8%B1%D8%B2%D8%B4%DB%8C/%DA%A9%D8%B4%D8%B4%20%D9%BE%D8%B1%D9%88%D8%A7%D9%86%D9%87.html> |

## `html ها/یوگا` — یوگا

| # | شناسه | عنوان | حجم | نشانی روی سرور داخلی |
|---:|---|---|---:|---|
| 1 | `yga-01` | حالت کودک | 1.7 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/01%20-%20%D8%AD%D8%A7%D9%84%D8%AA%20%DA%A9%D9%88%D8%AF%DA%A9.html> |
| 2 | `yga-02` | گربه–گاو | 2.1 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/02%20-%20%DA%AF%D8%B1%D8%A8%D9%87%E2%80%93%DA%AF%D8%A7%D9%88.html> |
| 3 | `yga-03` | سگ سر پایین | 2.0 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/03%20-%20%D8%B3%DA%AF%20%D8%B3%D8%B1%20%D9%BE%D8%A7%DB%8C%DB%8C%D9%86.html> |
| 4 | `yga-04` | کبرای ملایم | 2.5 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/04%20-%20%DA%A9%D8%A8%D8%B1%D8%A7%DB%8C%20%D9%85%D9%84%D8%A7%DB%8C%D9%85.html> |
| 5 | `yga-05` | جنگجوی یک | 3.0 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/05%20-%20%D8%AC%D9%86%DA%AF%D8%AC%D9%88%DB%8C%20%DB%8C%DA%A9.html> |
| 6 | `yga-06` | جنگجوی دو | 3.0 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/06%20-%20%D8%AC%D9%86%DA%AF%D8%AC%D9%88%DB%8C%20%D8%AF%D9%88.html> |
| 7 | `yga-07` | درخت | 2.3 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/07%20-%20%D8%AF%D8%B1%D8%AE%D8%AA.html> |
| 8 | `yga-08` | پل | 1.8 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/08%20-%20%D9%BE%D9%84.html> |
| 9 | `yga-09` | چرخش نشسته | 2.8 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/09%20-%20%DA%86%D8%B1%D8%AE%D8%B4%20%D9%86%D8%B4%D8%B3%D8%AA%D9%87.html> |
| 10 | `yga-10` | حالت کودک با زانوهای باز | 2.0 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/10%20-%20%D8%AD%D8%A7%D9%84%D8%AA%20%DA%A9%D9%88%D8%AF%DA%A9%20%D8%A8%D8%A7%20%D8%B2%D8%A7%D9%86%D9%88%D9%87%D8%A7%DB%8C%20%D8%A8%D8%A7%D8%B2.html> |
| 11 | `yga-11` | کبوتر حمایت‌شده | 3.4 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/11%20-%20%DA%A9%D8%A8%D9%88%D8%AA%D8%B1%20%D8%AD%D9%85%D8%A7%DB%8C%D8%AA%E2%80%8C%D8%B4%D8%AF%D9%87.html> |
| 12 | `yga-12` | مثلث | 2.8 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/12%20-%20%D9%85%D8%AB%D9%84%D8%AB.html> |
| 13 | `yga-13` | استراحت به پشت | 3.5 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/13%20-%20%D8%A7%D8%B3%D8%AA%D8%B1%D8%A7%D8%AD%D8%AA%20%D8%A8%D9%87%20%D9%BE%D8%B4%D8%AA.html> |
| 14 | `yga-14` | نیم‌قایق با زانوهای خم | 2.7 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/14%20-%20%D9%86%DB%8C%D9%85%E2%80%8C%D9%82%D8%A7%DB%8C%D9%82%20%D8%A8%D8%A7%20%D8%B2%D8%A7%D9%86%D9%88%D9%87%D8%A7%DB%8C%20%D8%AE%D9%85.html> |
| 15 | `yga-15` | خم‌شدن به جلو ایستاده | 2.2 MB | <https://c539776.parspack.net/html%20%D9%87%D8%A7/%DB%8C%D9%88%DA%AF%D8%A7/15%20-%20%D8%AE%D9%85%E2%80%8C%D8%B4%D8%AF%D9%86%20%D8%A8%D9%87%20%D8%AC%D9%84%D9%88%20%D8%A7%DB%8C%D8%B3%D8%AA%D8%A7%D8%AF%D9%87.html> |
