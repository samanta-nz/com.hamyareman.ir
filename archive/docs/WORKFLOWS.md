# GitHub Actions Workflows

## فایل‌ها

### 1. `android.yml`

### 2. `wellness-tests.yml`
- **هدف**: اجرای unit test های JVM خالص WellnessCatalog و WellnessTimingProvider
- **تریگر**: push به `main`/`arena/**` یا PR
- **نیاز به**: secrets ندارد
- **می‌توان مستقیماً اجرا کرد**: ✅

### 3. `deploy-prompt-02.yml` ⭐
- **هدف**: deploy کامل پرامپت ۰۲ روی Appwrite production
- **تریگر**: فقط `workflow_dispatch` (دستی)
- **۳ گام**:
  1. **Migration**: ساخت جداول `wellness_moves`/`sketch_references`/`wellness_logs` + indexes + bucket `wellness-media`
  2. **Upload**: آپلود ۴۶ تصویر + ۷۵ فایل صوتی به bucket
  3. **Seed**: وارد کردن ۴۳ حرکت به جدول `wellness_moves`
- **هر گام idempotent** (skip می‌کند اگر موجود باشد)
- **کلیدها hardcode شده** (project `6a9d59e3002751cc3ea8`)

## نحوه‌ی اجرای deploy

### گزینه ۱: از GitHub UI (ساده‌ترین)
1. به `https://github.com/behzadinfo-dotcom/com.hamyareman.ir/actions` بروید
2. در سمت چپ روی "Deploy Prompt 02 (wellness)" کلیک کنید
3. دکمه‌ی "Run workflow" را بزنید
4. (اختیاری) گزینه‌های skip را تنظیم کنید
5. دکمه‌ی سبز "Run workflow" را بزنید

### گزینه ۲: از خط فرمان (با `gh` CLI)
```bash
gh workflow run deploy-prompt-02.yml
```

یا با skip یک گام:
```bash
gh workflow run deploy-prompt-02.yml -f skip_migration=true
```

## منابع Appwrite

| منبع | شناسه |
|---|---|
| Endpoint | `https://fra.cloud.appwrite.io/v1` |
| Project | `6a9d59e3002751cc3ea8` |
| Database | `main_db` |
| جدول wellness_moves | `wellness_moves` (ساخته می‌شود) |
| جدول sketch_references | `sketch_references` (ساخته می‌شود) |
| جدول wellness_logs | `wellness_logs` (ساخته می‌شود) |
| باکت wellness-media | `wellness-media` (ساخته می‌شود) |

## خروجی‌های workflow

### گام ۱: migration
```
پرامپت ۰۲ migration: wellness tables + bucket
  ✅ table wellness_moves
  ✅ wellness_moves.category
  ✅ wellness_moves.titleFa
  ...
```

### گام ۲: upload
```
آپلود assets پرامپت ۰۲ از /home/runner/work/.../artifacts/prompt-02-push
به باکت: wellness-media
پروژه: 6a9d59e3002751cc3ea8
📁 46 تصویر + 75 فایل صوتی = 121 فایل
  · yoga/01-balasana-child-pose.jpg (exists as yoga-01-balasana-child-pose-jpg)
  ...
✅ خلاصه: 0 آپلود، 121 رد شد، 0 شکست
```
(اگر قبلاً آپلود شده باشد، همه skip می‌شوند)

### گام ۳: seed
```
بارگذاری 43 حرکت به جدول wellness_moves
  ✅ yoga-balasana (created)
  ✅ yoga-cat-cow (created)
  ...
✅ خلاصه: 43 ساخته، 0 به‌روز، 0 شکست
```

## Rollback

اگر deploy مشکل داشت:
1. به Appwrite Console بروید: https://cloud.appwrite.io
2. جداول را می‌توانید حذف کنید: `wellness_moves`, `sketch_references`, `wellness_logs`
3. باکت `wellness-media` را می‌توانید حذف کنید
4. سطرهای جدول را می‌توانید با `tablesDb.deleteRow` حذف کنید

## امنیت

⚠️ **مهم**: workflow شامل **API key با دسترسی کامل** است. پس از deploy موفق:
1. به `https://github.com/behzadinfo-dotcom/com.hamyareman.ir/settings/secrets/actions` بروید
2. **API key را در Appwrite Console تغییر دهید** (یکی جدید بسازید)
3. کلید قدیمی را revoke کنید
4. کلید جدید را فقط در GitHub Secrets ذخیره کنید
5. workflow را به‌روزرسانی کنید تا از `secrets.APPWRITE_API_KEY` استفاده کند (نه hardcoded)

## الگوی بهتر (پس از deploy اول)

```yaml
# به جای hardcoded:
env:
  APPWRITE_API_KEY: ${{ secrets.APPWRITE_API_KEY }}

# GitHub Secrets اضافه کنید:
# Settings → Secrets and variables → Actions → New repository secret
# Name: APPWRITE_API_KEY
# Value: standard_3e7b1708...
```
