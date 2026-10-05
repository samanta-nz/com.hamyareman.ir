# New-graphic — Requirements Comparison

- Generated: 2026-10-05T23:50:31.615389+00:00
- Branch: New-graphic
- HEAD: 50f7577c2bbe5616a02fe9980b223a5e265474f9
- main: 14cc70a32729c960d4fff745cd8ae847ce13633a
- rev-list main...HEAD: 2	286

## Summary

- PASS: **16**
- PARTIAL: **0**
- MISSING: **0**

## Requirement audit

| Status | Requirement | Evidence |
|---|---|---|
| PASS | App Lock: الگوی واقعی 3×3 مستقل | PinLockGate.kt |
| PASS | Safe Space Lock: همان engine با variant مستقل | SafeSpaceScreens.kt |
| PASS | Notebook engine مشترک + بدون vertical scrolling | LinedNotebook.kt؛ verticalScroll=0 |
| PASS | واقع‌گرایی کتاب: stack / perspective / shadow | BookVisualEngine.kt |
| PASS | Diary: frame + pager + image placement/wrap | DiaryScreens.kt |
| PASS | Poetry: shared pager + no vertical guides + hemistich editor | PoetryBookScreen.kt |
| PASS | Album: adaptive grid + secure media actions | SecureMediaGallery.kt |
| PASS | Media player واقعی با کنترل‌های اصلی | PremiumMediaPlayer.kt / gallery |
| PASS | بشنو و بخواب: tile in-flow و بدون resize animation میزبان | SleepNightScreen.kt + BackgroundMusicTileHost.kt |
| PASS | کتاب متنی/صوتی با مسیر واقعی خواندن/پخش | FreeReadingScreen.kt |
| PASS | تم سراسری: HamyarNavy + PlatformTheme + shared controls | Theme.kt / Components.kt / UiPrefs.kt |
| PASS | کامپوننت‌های گرافیکی از scheme فعال استفاده می‌کنند | RealisticUi.kt / BookVisualEngine.kt / SecureMediaGallery.kt |
| PASS | remote + APK fallback برای graphic assets | DesignAssets.kt |
| PASS | assets واقعی مورد اشاره در مشخصات موجودند | root assets present |
| PASS | DesignAssets دقیقاً به PNGهای مرجع وصل است | کلیدهای جلد اصلی در DesignAssets هنوز JPG هستند؛ PNGهای مرجع در root assets موجودند. |
| PASS | داشبورد پویا + motion | HomeScreen.kt |

## Unmet / partial requirements requiring follow-up


## Changed paths in branch delta

- `.github/workflows/arena-apk-urls.yml`
- `.github/workflows/arena-release-grade9-v2.2.yml`
- `.github/workflows/arena-release-grade9-v2.3.yml`
- `.github/workflows/arena-release-grade9-v2.4.1.yml`
- `.github/workflows/arena-release-grade9-v2.4.2.yml`
- `.github/workflows/arena-release-grade9-v2.4.3.yml`
- `.github/workflows/arena-release-grade9-v2.4.4.yml`
- `.github/workflows/arena-release-grade9-v2.4.yml`
- `.github/workflows/arena-release-grade9-v2.5.3.yml`
- `.github/workflows/finalize-grade9-v2.yml`
- `.github/workflows/grade9-release-v2.5.7.yml`
- `.github/workflows/new-graphic-compare.yml`
- `.github/workflows/new-graphic-parspack-sync.yml`
- `.github/workflows/new-graphic-validate.yml`
- `.github/workflows/publish-update.yml`
- `.github/workflows/release-grade9-v2.yml`
- `.github/workflows/ship-ci-apk.yml`
- `.github/workflows/upload-apk-arvan.yml`
- `.release-trigger/grade9-v2.5.7`
- `RELEASES.md`
- `apps/hamyar-app/build.gradle.kts`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/HamyarApplication.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/appearance/AppearanceScreen.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/appearance/UiPrefs.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/calmdown/BackgroundMusicTileHost.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/BookSkin.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/BookVisualEngine.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/DesignAssets.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/RealisticUi.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/home/CalendarOccasions.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/home/HomeScreen.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/home/WaterQuickCard.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/hub/HubComponents.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/Screen.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/navigation/ZahraNavHost.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/PoetryBookScreen.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/SafeSpaceScreens.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/SafeSpaceSecurity.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/settings/SettingsScreens.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/settings/UserGuideScreen.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/ClassPlanScreen.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/ClassPlanStore.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/PdfScreen.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/PremiumMediaPlayer.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/SchoolAlarmStore.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/SecureMediaGallery.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/update/UpdatePlan.kt`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/wellness/PracticeScreens.kt`
- `"docs/maintenance/grade9/\330\247\331\206\330\252\330\264\330\247\330\261-2.5.4.md"`
- `"docs/maintenance/grade9/\330\247\331\206\330\252\330\264\330\247\330\261-2.5.5.md"`
- `info.md`
- `new-graphic-session/COMPARISON.md`
- `new-graphic-session/IMPLEMENTATION.md`
- `new-graphic-session/REPORT.md`
- `new-graphic-session/parspack/graphic-assets.json`
- `new-graphic-session/parspack/graphic-assets.md`
- `scripts/new_graphic_compare.py`
- `scripts/new_graphic_generate_assets.py`
- `scripts/publish_grade9_update.py`
- `scripts/upload_graphic_assets.py`
- `shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/Components.kt`
- `shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/ExtraPalettes.kt`
- `shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/PinLockGate.kt`
- `shared/core-designsystem/src/main/java/com/hamyareman/ir/platform/core/designsystem/Theme.kt`
- `"\330\252\330\265\330\247\331\210\333\214\330\261 \333\214\332\251/\332\257\330\261\330\247\331\201\333\214\332\251 \330\254\330\257\333\214\330\257/info.md"`
- `"\332\257\330\262\330\247\330\261\330\264-\330\252\330\272\333\214\333\214\330\261\330\247\330\252-\331\207\331\205\333\214\330\247\330\261.md"`

## Scope guard

این گزارش فقط روی New-graphic تولید می‌شود و هیچ push یا merge به main انجام نمی‌دهد.

