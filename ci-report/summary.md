# گزارش بیلد کامل (پایهٔ نهم — اپ بیس)

## وضعیت کلی

- تسک‌های شکست‌خورده: **1**
- خطای کامپایل کاتلین: **0**
- هشدار کامپایلر (یکتا): **56**
- تست واحد: **162** اجرا، **0** ناموفق
- APK ساخته‌شده: **3**

## تسک‌های شکست‌خورده

- `:hamyar-app:lintP09Debug`

## جزئیات شکست Gradle

```
* What went wrong:
Execution failed for task ':hamyar-app:lintP09Debug' (registered by plugin 'com.android.internal.application').
> Lint found errors in the project; aborting build.

  Fix the issues identified by lint, or create a baseline to see only new errors.
  To create a baseline, run `gradlew updateLintBaseline` after adding the following to the module's build.gradle file:
  ```
  android {
      lint {
          baseline = file("lint-baseline.xml")
      }
  }
```

## خروجی‌ها

- `apps/hamyar-admin/build/outputs/apk/debug/hamyar-admin-debug.apk — 22.7 MB`
- `apps/hamyar-app/build/outputs/apk/p09/debug/hamyar-app-p09-debug.apk — 119.7 MB`
- `apps/hamyar-app/build/outputs/apk/p09/release/hamyar-app-p09-release.apk — 42.0 MB`

## تست واحد

| کلاس | تست | ناموفق | خطا | رد‌شده |
|---|---:|---:|---:|---:|
| `com.hamyareman.ir.platform.core.common.BillingStatusTest` | 3 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.core.common.JalaliDateTest` | 10 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.core.common.PrivacyPolicyTest` | 6 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.pairing.PairingCodeTest` | 5 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.playback.CtrPlainReaderTest` | 3 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.playback.ProgressEventMathTest` | 17 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.study.BookContentGuardTest` | 5 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.study.MathAnswerScriptTest` | 6 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.study.MathExamLedgerTest` | 4 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.study.QuizGraderTest` | 4 | 0 | 0 | 0 |
| `com.hamyareman.ir.platform.feature.study.Sm2Test` | 5 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.AppTypographyInitializationTest` | 1 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.art.ArtStreakTest` | 7 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.content.ContentCatalogCoverTest` | 2 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.cycle.MonthlyCycleTest` | 9 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.profile.GradeEditionTest` | 3 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.ClassPlanShiftTest` | 8 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.MediaVaultParallelCryptoTest` | 4 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.MediaVaultRangeTest` | 5 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.SchoolShiftTest` | 6 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.SpacedReviewMathTest` | 5 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.update.UpdatePlanTest` | 13 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.wellness.WellnessCatalogTest` | 18 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.wellness.WellnessTimingTest` | 13 | 0 | 0 | 0 |

## لینت

### `apps/hamyar-admin/build/reports/lint-results-debug.xml` — 0 خطا، 17 هشدار

- `Warning:UseKtx` × 7
- `Warning:SetJavaScriptEnabled` × 2
- `Warning:IconLauncherShape` × 2
- `Warning:OldTargetApi` × 1
- `Warning:NewerVersionAvailable` × 1
- `Warning:DataExtractionRules` × 1
- `Hint:AutoboxingStateCreation` × 1
- `Warning:UnusedResources` × 1
- `Warning:IconDuplicates` × 1
- `Warning:UseTomlInstead` × 1

### `apps/hamyar-app/build/reports/lint-results-p09Debug.xml` — 2 خطا، 84 هشدار

- `Warning:UseKtx` × 23
- `Warning:GradleDependency` × 11
- `Hint:AutoboxingStateCreation` × 11
- `Warning:IconLauncherShape` × 7
- `Warning:SetJavaScriptEnabled` × 6
- `Warning:ObsoleteSdkInt` × 5
- `Warning:ComposableNaming` × 4
- `Warning:ClickableViewAccessibility` × 4
- `Warning:AndroidGradlePluginVersion` × 3
- `Warning:NewerVersionAvailable` × 3
- `Warning:ModifierParameter` × 3
- `Warning:ExifInterface` × 2
- `Warning:ConfigurationScreenWidthHeight` × 2
- `Error:UnusedBoxWithConstraintsScope` × 2
- `Warning:StaticFieldLeak` × 2
- `Warning:MissingPermission` × 1
- `Warning:OldTargetApi` × 1
- `Warning:CredentialManagerMisuse` × 1
- `Warning:ManifestOrder` × 1
- `Warning:LocalContextResourcesRead` × 1
- `Warning:ChromeOsAbiSupport` × 1
- `Warning:HardwareIds` × 1
- `Warning:DataExtractionRules` × 1
- `Warning:UnusedResources` × 1

### نمونهٔ خطاهای لینت

- **UnusedBoxWithConstraintsScope**
  - apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:675 — BoxWithConstraints scope is not used
  - apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt:131 — BoxWithConstraints scope is not used

## هشدارهای کامپایلر کاتلین

### Deprecated API — 39 مورد یکتا

- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/AuthService.kt:461:21 'suspend fun createVerification(url: String): Token' is deprecated. This API has been deprecated since 1.8.0. Please use `Account.createEmailVerification` instead.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/AuthService.kt:474:21 'suspend fun updateVerification(userId: String, secret: String): Token' is deprecated. This API has been deprecated since 1.8.0. Please use `Account.updateEmailVerification` instead.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/PlaybackService.kt:130:58 'constructor(p0: MediaSession): MediaSession.ConnectionResult.AcceptedResultBuilder' is deprecated. Deprecated in Java.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/PlaybackService.kt:180:34 'fun onPlayerCommandRequest(p0: MediaSession, p1: MediaSession.ControllerInfo, p2: Int): Int' is deprecated. Deprecated in Java.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/SleepPlaybackService.kt:61:58 'constructor(p0: MediaSession): MediaSession.ConnectionResult.AcceptedResultBuilder' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/appearance/FontFloater.kt:154:56 'fun Modifier.menuAnchor(): Modifier' is deprecated. Use overload that takes ExposedDropdownMenuAnchorType and enabled parameters.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/auth/LoginScreen.kt:487:9 'fun Divider(modifier: Modifier = ..., thickness: Dp = ..., color: Color = ...): Unit' is deprecated. Renamed to HorizontalDivider.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/auth/LoginScreen.kt:489:9 'fun Divider(modifier: Modifier = ..., thickness: Dp = ..., color: Color = ...): Unit' is deprecated. Renamed to HorizontalDivider.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt:323:32 'val Icons.Filled.FormatAlignRight: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.FormatAlignRight.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/components/LinedNotebook.kt:329:32 'val Icons.Filled.FormatAlignLeft: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.FormatAlignLeft.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/cycle/PeriodTrainingScreen.kt:74:9 'fun TabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryTabRow and SecondaryTabRow.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/learning/LessonPlayerScreen.kt:302:30 'val Icons.Filled.VolumeUp: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.VolumeUp.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/IranPlaces.kt:101:48 'fun Modifier.menuAnchor(): Modifier' is deprecated. Use overload that takes ExposedDropdownMenuAnchorType and enabled parameters.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/JalaliBirthDateFields.kt:115:48 'fun Modifier.menuAnchor(): Modifier' is deprecated. Use overload that takes ExposedDropdownMenuAnchorType and enabled parameters.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:524:52 'val Icons.Filled.MenuBook: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.MenuBook.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/DiaryScreens.kt:933:52 'val Icons.Filled.MenuBook: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.MenuBook.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/safespace/PoetryBookScreen.kt:199:52 'val Icons.Filled.MenuBook: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.MenuBook.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/BookCommentsPanel.kt:189:37 'val Icons.Outlined.Reply: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.Reply.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/ClassPlanScreen.kt:92:9 'fun ScrollableTabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., edgePadding: Dp = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryScrollableTabRow and SecondaryScrollableTabRow tab variants.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/FreeReadingScreen.kt:124:13 'fun TabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryTabRow and SecondaryTabRow.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/FreeReadingScreen.kt:141:9 'fun TabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryTabRow and SecondaryTabRow.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/FreeReadingScreen.kt:196:45 'val Icons.Outlined.LibraryBooks: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Outlined.LibraryBooks.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonStudyScreens.kt:148:9 'fun TabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryTabRow and SecondaryTabRow.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonTeachScreen.kt:693:55 'val Icons.Filled.VolumeOff: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.VolumeOff.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonTeachScreen.kt:693:83 'val Icons.Filled.VolumeUp: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.VolumeUp.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/MathLessonScreen.kt:330:5 'fun ScrollableTabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., edgePadding: Dp = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryScrollableTabRow and SecondaryScrollableTabRow tab variants.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/PdfScreen.kt:1194:93 'val Icons.Filled.OpenInNew: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.OpenInNew.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/PdfScreen.kt:1238:71 'val Icons.Filled.OpenInNew: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.OpenInNew.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:93:34 'var systemUiVisibility: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:95:19 'var systemUiVisibility: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:96:50 'static field SYSTEM_UI_FLAG_FULLSCREEN: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:97:35 'static field SYSTEM_UI_FLAG_HIDE_NAVIGATION: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:98:35 'static field SYSTEM_UI_FLAG_IMMERSIVE_STICKY: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:99:35 'static field SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:100:35 'static field SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:101:35 'static field SYSTEM_UI_FLAG_LAYOUT_STABLE: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:104:38 'var systemUiVisibility: Int' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/update/ApkUpdate.kt:364:36 'static field ACTION_INSTALL_PACKAGE: String' is deprecated. Deprecated in Java.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/update/UpdateGate.kt:109:26 'val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>' is deprecated. Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.`

### Redundant code — 6 مورد یکتا

- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/AuthService.kt:643:34 Unnecessary safe call on a non-null receiver of type 'JSONObject'.`
- `shared/core-notifications/src/main/java/com/hamyareman/ir/platform/core/notifications/QuietHours.kt:153:28 Unnecessary safe call on a non-null receiver of type 'NotificationManager'.`
- `shared/core-notifications/src/main/java/com/hamyareman/ir/platform/core/notifications/QuietHours.kt:161:34 Unnecessary safe call on a non-null receiver of type 'NotificationManager'.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/StudentProfileScreen.kt:257:42 Condition is always 'true'.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/StudentProfileScreen.kt:257:57 Condition is always 'true'.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonStudyScreens.kt:693:35 Unnecessary non-null assertion (!!) on a non-null receiver of type 'StudyPack.Question'.`

### Other — 6 مورد یکتا

- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/FunctionsService.kt:50:30 'when' expression over a subject of type 'Any?' is not exhaustive. Add a 'null' or 'else' branch.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/FunctionsService.kt:55:32 'when' expression over a subject of type 'Any?' is not exhaustive. Add a 'null' or 'else' branch.`
- `shared/feature-calls/src/main/java/com/hamyareman/ir/platform/feature/calls/CallEngine.kt:389:58 The corresponding parameter in the supertype 'SdpAdapter' is named 'sdp'. This may cause problems when calling this function with named arguments.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/PlaybackService.kt:167:30 This declaration overrides a deprecated member but is not marked as deprecated itself. Add the '@Deprecated' annotation or suppress the diagnostic.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/learning/LearningScreens.kt:94:8 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/wellness/WellnessScreen.kt:188:49 The corresponding parameter in the supertype 'WellnessLogSink' is named 'move'. This may cause problems when calling this function with named arguments.`

### Unused code — 5 مورد یکتا

- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:103:9 Expression is unused.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:113:9 Expression is unused.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:129:9 Expression is unused.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:138:9 Expression is unused.`
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/di/AppContainer.kt:71:53 Expression is unused.`

