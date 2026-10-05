# New-graphic session

Scope: visual/product implementation only on branch `New-graphic`.

## Hard invariants
- Other app section structures and routes remain untouched unless the requested graphic surface itself requires a local UI replacement.
- Existing storage, encryption, playback, navigation and server contracts are preserved.
- The shared notebook presentation is centralized in the existing notebook component; diary and poetry opt into the realistic surface without changing persistence.
- Global HamyarNavy colors/elevation/surface treatment are applied through the shared design system.
- Remote design assets use `https://c539776.parspack.net/` through the existing resolver; local fallback remains available.

## Implemented
- Fixed the missing realistic book-page implementation used by `NotebookBookPage`.
- Added first-line title presentation with one blank line before body text for diary/poetry.
- Added realistic paper/stack/shadow surfaces and desk backgrounds for diary, poetry and personal album screens.
- Kept adaptive album grid, real Media3 playback and private-storage behavior intact.
- Upgraded shared app top bar / primary button / section-card visual treatment without changing navigation or content structure.
- Added automated generation/compression script for lock, diary, poetry and album raster backgrounds.
- Added branch-only validation workflow with compile/test and a report under `new-graphic-session/`.

## Visual asset note
The raster backgrounds are reproducible from `scripts/new_graphic_generate_assets.py`. CI generates them into the Android asset tree before compilation, so APK builds contain local fallbacks even when the remote bucket is unavailable.

## اجرای فعلی نشست
- پوستهٔ کنترل‌های عمومی اپ (`AppTopBar`، `PrimaryButton`، `InlineButton`، `SectionCard`) با رنگ/عمق/سایهٔ تم Hamyar یکپارچه شد و API/ساختار صفحات تغییر نکرد.
- سطوح واقع‌گرایانهٔ دفترها و جلدها تقویت شد: نور، بافت، لبه، stack واقعی ورق‌ها و افکت خم/چرخش ورق.
- ویرایش شعرهای دو-مصراعی از صفر به دو ناحیهٔ مستقل با تورق RTL بازسازی شد و خطوط عمودی برای شعر حفظ نمی‌شود.
- چیدمان عکس دفتر خاطرات اکنون `TOP / BOTTOM / NONE` را در خروجی واقعی صفحه رعایت می‌کند و تصویر تصادفی روی متن نمی‌افتد.
- مولد بک‌گراندها با texture/vignette/atmosphere تکرارپذیر و فشرده‌سازی JPEG به‌روزرسانی شد.
- workflow مستقل `new-graphic-parspack-sync.yml` و ابزار `upload_graphic_assets.py` برای تولید، آپلود، skip بر اساس SHA-256 و تأیید خواندن عمومی با مسیر `https://c539776.parspack.net/assets/...` اضافه شد.
- گزارش‌های اجرایی فقط زیر `new-graphic-session/` نگهداری می‌شوند.