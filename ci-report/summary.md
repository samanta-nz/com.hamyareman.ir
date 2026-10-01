# گزارش بیلد کامل (پایهٔ نهم — اپ بیس)

## وضعیت کلی

- تسک‌های شکست‌خورده: **0**
- خطای کامپایل کاتلین: **0**
- هشدار کامپایلر (یکتا): **56**
- تست واحد: **160** اجرا، **0** ناموفق
- APK ساخته‌شده: **3**

## خروجی‌ها

- `apps/hamyar-admin/build/outputs/apk/debug/hamyar-admin-debug.apk — 22.5 MB`
- `apps/hamyar-app/build/outputs/apk/p09/debug/hamyar-app-p09-debug.apk — 68.7 MB`
- `apps/hamyar-app/build/outputs/apk/p09/release/hamyar-app-p09-release.apk — 44.9 MB`

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
| `com.hamyareman.ir.ui.art.ArtStreakTest` | 7 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.content.ContentCatalogCoverTest` | 2 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.cycle.MonthlyCycleTest` | 9 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.profile.GradeEditionTest` | 3 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.ClassPlanShiftTest` | 8 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.MediaVaultParallelCryptoTest` | 4 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.MediaVaultRangeTest` | 5 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.SchoolShiftTest` | 6 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.study.SpacedReviewMathTest` | 5 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.update.UpdatePlanTest` | 12 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.wellness.WellnessCatalogTest` | 18 | 0 | 0 | 0 |
| `com.hamyareman.ir.ui.wellness.WellnessTimingTest` | 13 | 0 | 0 | 0 |

## لینت

### `apps/hamyar-admin/build/reports/lint-results-debug.xml` — 0 خطا، 12 هشدار

- `Warning:UseKtx` × 3
- `Warning:IconLauncherShape` × 2
- `Warning:OldTargetApi` × 1
- `Warning:NewerVersionAvailable` × 1
- `Warning:SetJavaScriptEnabled` × 1
- `Warning:DataExtractionRules` × 1
- `Hint:AutoboxingStateCreation` × 1
- `Warning:UnusedResources` × 1
- `Warning:IconDuplicates` × 1
- `Warning:UseTomlInstead` × 1

### `apps/hamyar-app/build/reports/lint-results-p09Debug.xml` — 0 خطا، 81 هشدار

- `Warning:UseKtx` × 23
- `Warning:GradleDependency` × 11
- `Hint:AutoboxingStateCreation` × 9
- `Warning:UnusedResources` × 9
- `Warning:IconLauncherShape` × 7
- `Warning:AndroidGradlePluginVersion` × 3
- `Warning:SetJavaScriptEnabled` × 3
- `Warning:ObsoleteSdkInt` × 3
- `Warning:ClickableViewAccessibility` × 3
- `Warning:ExifInterface` × 2
- `Warning:InlinedApi` × 2
- `Warning:NewerVersionAvailable` × 2
- `Warning:ModifierParameter` × 2
- `Warning:StaticFieldLeak` × 2
- `Warning:OldTargetApi` × 1
- `Warning:CredentialManagerMisuse` × 1
- `Warning:VectorRaster` × 1
- `Warning:LocalContextResourcesRead` × 1
- `Warning:ChromeOsAbiSupport` × 1
- `Warning:HardwareIds` × 1
- `Warning:DataExtractionRules` × 1
- `Warning:FrequentlyChangingValue` × 1
- `Warning:NotShrinkingResources` × 1

## هشدارهای کامپایلر کاتلین

### Deprecated API — 29 مورد یکتا

- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/appearance/FontFloater.kt:147:56 'fun Modifier.menuAnchor(): Modifier' is deprecated. Use overload that takes ExposedDropdownMenuAnchorType and enabled parameters.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/auth/LoginScreen.kt:487:9 'fun Divider(modifier: Modifier = ..., thickness: Dp = ..., color: Color = ...): Unit' is deprecated. Renamed to HorizontalDivider.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/auth/LoginScreen.kt:489:9 'fun Divider(modifier: Modifier = ..., thickness: Dp = ..., color: Color = ...): Unit' is deprecated. Renamed to HorizontalDivider.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/cycle/PeriodTrainingScreen.kt:74:9 'fun TabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryTabRow and SecondaryTabRow.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/learning/LessonPlayerScreen.kt:302:30 'val Icons.Filled.VolumeUp: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.VolumeUp.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/IranPlaces.kt:101:48 'fun Modifier.menuAnchor(): Modifier' is deprecated. Use overload that takes ExposedDropdownMenuAnchorType and enabled parameters.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/JalaliBirthDateFields.kt:115:48 'fun Modifier.menuAnchor(): Modifier' is deprecated. Use overload that takes ExposedDropdownMenuAnchorType and enabled parameters.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/ClassPlanScreen.kt:92:9 'fun ScrollableTabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., edgePadding: Dp = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryScrollableTabRow and SecondaryScrollableTabRow tab variants.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/FreeReadingScreen.kt:23:9 'fun TabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryTabRow and SecondaryTabRow.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonStudyScreens.kt:148:9 'fun TabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryTabRow and SecondaryTabRow.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonTeachScreen.kt:628:55 'val Icons.Filled.VolumeOff: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.VolumeOff.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonTeachScreen.kt:628:83 'val Icons.Filled.VolumeUp: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.VolumeUp.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/MathLessonScreen.kt:328:5 'fun ScrollableTabRow(selectedTabIndex: Int, modifier: Modifier = ..., containerColor: Color = ..., contentColor: Color = ..., edgePadding: Dp = ..., indicator: ComposableFunction1<List<TabPosition>, Unit> = ..., divider: ComposableFunction0<Unit> = ..., tabs: ComposableFunction0<Unit>): Unit' is deprecated. Replaced with PrimaryScrollableTabRow and SecondaryScrollableTabRow tab variants.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:93:34 'var systemUiVisibility: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:95:19 'var systemUiVisibility: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:96:50 'static field SYSTEM_UI_FLAG_FULLSCREEN: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:97:35 'static field SYSTEM_UI_FLAG_HIDE_NAVIGATION: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:98:35 'static field SYSTEM_UI_FLAG_IMMERSIVE_STICKY: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:99:35 'static field SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:100:35 'static field SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:101:35 'static field SYSTEM_UI_FLAG_LAYOUT_STABLE: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/VideoTeachScreen.kt:104:38 'var systemUiVisibility: Int' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/update/ApkUpdate.kt:364:36 'static field ACTION_INSTALL_PACKAGE: String' is deprecated. Deprecated in Java.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/update/UpdateGate.kt:109:26 'val LocalLifecycleOwner: ProvidableCompositionLocal<LifecycleOwner>' is deprecated. Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.` (×2)
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/AuthService.kt:461:21 'suspend fun createVerification(url: String): Token' is deprecated. This API has been deprecated since 1.8.0. Please use `Account.createEmailVerification` instead.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/AuthService.kt:474:21 'suspend fun updateVerification(userId: String, secret: String): Token' is deprecated. This API has been deprecated since 1.8.0. Please use `Account.updateEmailVerification` instead.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/PlaybackService.kt:123:58 'constructor(p0: MediaSession): MediaSession.ConnectionResult.AcceptedResultBuilder' is deprecated. Deprecated in Java.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/PlaybackService.kt:158:34 'fun onPlayerCommandRequest(p0: MediaSession, p1: MediaSession.ControllerInfo, p2: Int): Int' is deprecated. Deprecated in Java.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/SleepPlaybackService.kt:61:58 'constructor(p0: MediaSession): MediaSession.ConnectionResult.AcceptedResultBuilder' is deprecated. Deprecated in Java.`

### Redundant code — 13 مورد یکتا

- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/content/ContentScreens.kt:200:41 Unnecessary safe call on a non-null receiver of type 'ContentItem'.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/content/ContentScreens.kt:200:64 Unnecessary safe call on a non-null receiver of type 'ContentItem'.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/StudentProfileScreen.kt:257:42 Condition is always 'true'.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/profile/StudentProfileScreen.kt:257:57 Condition is always 'true'.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/LessonStudyScreens.kt:693:35 Unnecessary non-null assertion (!!) on a non-null receiver of type 'StudyPack.Question'.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/RemoteHtmlCache.kt:79:34 Unnecessary non-null assertion (!!) on a non-null receiver of type 'HttpURLConnection'.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/RemoteHtmlCache.kt:81:33 Unnecessary non-null assertion (!!) on a non-null receiver of type 'HttpURLConnection'.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/RemoteHtmlCache.kt:83:21 Unnecessary non-null assertion (!!) on a non-null receiver of type 'HttpURLConnection'.` (×2)
- `apps/hamyar-admin/src/main/java/com/hamyareman/admin/AdminApi.kt:104:29 Unnecessary safe call on a non-null receiver of type 'ResponseBody'.`
- `apps/hamyar-admin/src/main/java/com/hamyareman/admin/AdminApi.kt:122:30 Unnecessary safe call on a non-null receiver of type 'ResponseBody'.`
- `shared/core-notifications/src/main/java/com/hamyareman/ir/platform/core/notifications/QuietHours.kt:153:28 Unnecessary safe call on a non-null receiver of type 'NotificationManager'.`
- `shared/core-notifications/src/main/java/com/hamyareman/ir/platform/core/notifications/QuietHours.kt:161:34 Unnecessary safe call on a non-null receiver of type 'NotificationManager'.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/AuthService.kt:643:34 Unnecessary safe call on a non-null receiver of type 'JSONObject'.`

### Other — 9 مورد یکتا

- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/learning/LearningScreens.kt:94:8 Annotation 'androidx.media3.common.util.UnstableApi' is not annotated with '@RequiresOptIn'. '@OptIn' has no effect.` (×2)
- `apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/wellness/WellnessScreen.kt:187:49 The corresponding parameter in the supertype 'WellnessLogSink' is named 'move'. This may cause problems when calling this function with named arguments.` (×2)
- `apps/hamyar-admin/src/main/java/com/hamyareman/admin/AdminApi.kt:199:19 'when' expression over a subject of type 'Any?' is not exhaustive. Add a 'null' or 'else' branch.`
- `apps/hamyar-admin/src/main/java/com/hamyareman/admin/AdminApi.kt:343:13 'when' expression over a subject of type 'Any?' is not exhaustive. Add a 'null' or 'else' branch.`
- `apps/hamyar-admin/src/main/java/com/hamyareman/admin/ui/AdminConsoleScreens.kt:57:12 'when' expression over a subject of type 'Any?' is not exhaustive. Add a 'null' or 'else' branch.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/FunctionsService.kt:50:30 'when' expression over a subject of type 'Any?' is not exhaustive. Add a 'null' or 'else' branch.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/FunctionsService.kt:55:32 'when' expression over a subject of type 'Any?' is not exhaustive. Add a 'null' or 'else' branch.`
- `shared/feature-calls/src/main/java/com/hamyareman/ir/platform/feature/calls/CallEngine.kt:389:58 The corresponding parameter in the supertype 'SdpAdapter' is named 'sdp'. This may cause problems when calling this function with named arguments.`
- `shared/feature-playback/src/main/java/com/hamyareman/ir/platform/feature/playback/PlaybackService.kt:145:30 This declaration overrides a deprecated member but is not marked as deprecated itself. Add the '@Deprecated' annotation or suppress the diagnostic.`

### Unused code — 5 مورد یکتا

- `apps/hamyar-admin/src/main/java/com/hamyareman/admin/AdminApi.kt:739:9 Expression is unused.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:103:9 Expression is unused.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:113:9 Expression is unused.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:129:9 Expression is unused.`
- `shared/core-appwrite/src/main/java/com/hamyareman/ir/platform/core/appwrite/TablesDbService.kt:138:9 Expression is unused.`

