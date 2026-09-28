package com.hamyareman.ir

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.designsystem.BrandTheme
import com.hamyareman.ir.platform.core.designsystem.PinLockGate
import com.hamyareman.ir.platform.core.designsystem.PlatformTheme
import com.hamyareman.ir.platform.core.notifications.NotificationPermissions
import com.hamyareman.ir.platform.core.security.AppLock
import com.hamyareman.ir.platform.core.security.BiometricPromptRunner
import com.hamyareman.ir.di.AppContainer
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import com.hamyareman.ir.ui.auth.LoginScreen
import com.hamyareman.ir.ui.navigation.ZahraNavHost
import kotlinx.coroutines.launch
import com.hamyareman.ir.ui.AppTypography

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer missing") }

/**
 * [FragmentActivity] به‌جای ComponentActivity: سازنده‌ی `androidx.biometric.BiometricPrompt`
 * فقط FragmentActivity (یا Fragment) می‌گیرد. FragmentActivity خودش از ComponentActivity
 * ارث می‌برد، پس `enableEdgeToEdge()` و `setContent {}` دقیقاً مثل قبل کار می‌کنند.
 *
 * ترتیب بازکردن قفل:
 * 1. اگر قفل فعال نیست → مستقیم داخل اپ.
 * 2. اگر بیومتریک روشن است و دستگاه آماده → پرامپت به‌صورت خودکار بالا می‌آید.
 * 3. وگرنه (یا اگر کاربر «واردکردن PIN» را زد) → همان PIN همیشگی.
 *
 * نکته‌ی امنیتی: بیومتریک هرگز جای PIN را نمی‌گیرد؛ فقط `AppLock.markUnlocked()` را صدا می‌زند.
 */
class MainActivity : FragmentActivity() {

    /**
     * وضعیت قفل به‌صورت state نگه داشته می‌شود تا در `onResume` (بعد از بازگشت از
     * پس‌زمینه) دوباره ارزیابی شود و تایم‌اوت قفل خودکار واقعاً کار کند.
     */
    private val unlocked = mutableStateOf(true)

    /** null = در حال بررسی سشن؛ true = وارد شده؛ false = باید صفحه‌ی ورود ببیند. */
    private val loggedIn = mutableStateOf<Boolean?>(null)

    /** null = در حال بررسی؛ true = پروفایل ثبت نشده → فرم ثبت‌نام اجباری. */
    private val profileNeeded = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HamyarApplication
        enableEdgeToEdge()
        unlocked.value = !app.container.lock.isLockedNow()
        // آینه‌ی محلی پروفایل/پایه — پیش از هر پاسخ شبکه (فیلتر فوری محتوا).
        com.hamyareman.ir.ui.profile.StudentProfileState.loadMirror(this)
        // لمس اعلان پخش (حتی با اپ کاملاً بسته) → بعد از لاگین/باز شدن قفل،
        // صفحه‌ی تدریس همان درس باز و پخش همان‌جا شروع می‌شود.
        captureTeachIntent(intent)
        captureSleepIntent(intent)

        setContent {
            val container = app.container
            val activity = this@MainActivity

            val notificationLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()) { /* اگر کاربر نداد، یادآورها بی‌صدا می‌مانند؛ اصرار نمی‌کنیم */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !NotificationPermissions.isGranted(activity)
                ) {
                    notificationLauncher.launch(NotificationPermissions.POST_NOTIFICATIONS)
                }
            }

            val isUnlocked by unlocked
            var bioBusy by remember { mutableStateOf(false) }
            var bioNotice by remember { mutableStateOf<String?>(null) }

            // هر بار که صفحه‌ی قفل نشان داده می‌شود دوباره بررسی می‌شود (مثلاً کاربر همین
            // الان در تنظیمات بیومتریک را روشن کرده باشد یا حسگر موقتاً از دسترس خارج شده باشد).
            val offerBiometric = remember(isUnlocked) { container.biometric.shouldOffer(activity) }

            fun unlockWithBiometric() {
                bioBusy = true
                bioNotice = null
                BiometricPromptRunner.show(
                    activity = activity,
                    title = "همیار من",
                    subtitle = "برای بازکردن دفترچه‌ات اثر انگشت یا چهره‌ات را تأیید کن.",
                    negativeText = "واردکردن PIN",
                    onSuccess = {
                        container.lock.markUnlocked()
                        bioBusy = false
                        unlocked.value = true
                    },
                    onError = { message ->
                        bioBusy = false
                        bioNotice = message
                    },
                    onCancelled = { bioBusy = false })
            }

            // وقتی اپ قفل است و بیومتریک فعال، پرامپت خودش بالا می‌آید تا کاربر معطل نشود.
            LaunchedEffect(isUnlocked, offerBiometric) {
                if (!isUnlocked && offerBiometric) unlockWithBiometric()
            }

            val scope = androidx.compose.runtime.rememberCoroutineScope()
            var loginLoading by remember { mutableStateOf(false) }
            var loginError by remember { mutableStateOf<String?>(null) }
            var loginNotice by remember { mutableStateOf<String?>(null) }

            // v1.25 — «مرا به خاطر بسپار»: سشنِ معتبر = ورود مستقیم به اپ؛
            // صفحه‌ی لاگین فقط وقتی سشنی نیست. (قانون قدیمیِ «لاگین هر اجرا» حذف شد.)
            LaunchedEffect(Unit) {
                if (loggedIn.value == null) {
                    val u = runCatching { container.auth.currentUser() }.getOrNull()
                    if (u == null) {
                        loggedIn.value = false
                    } else {
                        when (val g = container.auth.enforceAccountGate()) {
                            is AppResult.Err -> {
                                loginError = g.error.userMessage
                                loggedIn.value = false
                            }
                            is AppResult.Ok -> loggedIn.value = true
                        }
                    }
                }
            }

            // v1.25 — پس از ورود: اگر ردیف پروفایل دانش‌آموز ندارد → فرم ثبت‌نام اجباری.
            // (آفلاین بودن سرور را با آینه‌ی محلی جبران می‌کنیم تا فرم بی‌دلیل نیاید.)
            LaunchedEffect(loggedIn.value == true) {
                if (loggedIn.value == true) {
                    val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                    val fetched = if (uid.isBlank()) null else
                        runCatching {
                            com.hamyareman.ir.ui.profile.StudentProfileRepo.fetch(container.tables, uid)
                        }.getOrNull()
                    if (fetched != null) {
                        // v1.31 — نام و اشتراک بازیابی‌شده هم در آینه نوشته شود؛
                        // وگرنه سلام داشبورد «دوست من» می‌ماند (باگ گزارش‌شده).
                        var sub = fetched.subscription
                        runCatching {
                            val gate = com.hamyareman.ir.platform.core.appwrite.BillingGateway(container.functions)
                            when (val b = gate.myOrder()) {
                                is AppResult.Ok -> sub = b.value.first.ifBlank { sub }
                                else -> Unit
                            }
                        }
                        com.hamyareman.ir.ui.profile.StudentProfileState.writeMirror(
                            activity, fetched.grade, /* done = */ true,
                            name = fetched.firstName, sub = sub,
                            genderId = fetched.gender)
                        container.uiPrefs.applyDefaultForGender(fetched.gender)
                        runCatching { com.hamyareman.ir.ui.profile.AvatarSync.pull(activity, uid) }
                    }
                    profileNeeded.value = fetched == null && !com.hamyareman.ir.ui.profile.StudentProfileState.hasProfile
                }
            }

            // v1.14: با ورود، همه‌ی آمار مدرسه (تدریس/فلش‌کارت/آزمون/نمودار پیشرفت)
            // از سرور بازیابی و ادغام می‌شود — تعویض گوشی یا نصب مجدد هیچ‌چیز را از دست نمی‌دهد.
            LaunchedEffect(loggedIn.value == true) {
                if (loggedIn.value == true) {
                    val uid = runCatching { container.auth.currentUserId() }.getOrNull()
                    runCatching {
                        com.hamyareman.ir.ui.study.SchoolSync.restoreAll(activity, container.tables, container.sync, uid)
                    }
                    runCatching {
                        com.hamyareman.ir.ui.study.SchoolSync.watchLive(activity, container.realtime, uid)
                    }
                    // برنامهٔ هفتگی، شیفت/ساعت خروج، کلاس مجازی و تیک‌ها:
                    // با ورود به اپ خودکار از سرور گرفته و بعد به سرور فرستاده می‌شوند.
                    runCatching {
                        com.hamyareman.ir.ui.study.ClassPlanSync.pullAll(activity, container.tables, uid.orEmpty())
                    }
                    runCatching {
                        com.hamyareman.ir.ui.study.ClassPlanSync.pushAll(activity, container.tables, uid.orEmpty())
                    }
                    runCatching {
                        com.hamyareman.ir.ui.tools.ToolSaveStore.pull(activity, container.tables, uid.orEmpty())
                    }
                    runCatching {
                        val weekKey = com.hamyareman.ir.ui.study.StateSync.KEY_WEEK_PLAN
                        val remote = com.hamyareman.ir.ui.study.StateSync.pull(activity, container.tables, uid.orEmpty(), weekKey)
                        if (remote != null && remote.first.isNotBlank()) {
                            val localAt = com.hamyareman.ir.ui.study.StateSync.localAt(activity, weekKey)
                            if (remote.second >= localAt) {
                                com.hamyareman.ir.platform.core.common.LocalStore(activity, "hamyar_week_plan")
                                    .putString("week_plan_blocks", remote.first)
                                com.hamyareman.ir.ui.study.StateSync.markSyncedAt(activity, weekKey, remote.second)
                            }
                        }
                    }
                }
            }

            val uiPrefs = container.uiPrefs
            CompositionLocalProvider(LocalUiPrefs provides uiPrefs) {
                PlatformTheme(
                    brand = uiPrefs.theme,
                    darkTheme = uiPrefs.darkTheme,
                    fontFamily = AppTypography.pageBody.family,
                    textSizeOffset = uiPrefs.textSizeOffset,
                    typography = AppTypography.material()) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    Surface(Modifier.fillMaxSize()) {
                        when {
                            // ۰) هنوز وضعیت سشن نامعلوم است — لحظه‌ای خالی تا پرش نبینیم.
                            loggedIn.value == null -> Unit

                            // ۱) وارد نشده: دروازه‌ی ورود (فقط گوگل — v1.25: مهمان حذف شد).
                            loggedIn.value == false -> LoginScreen(
                                loading = loginLoading,
                                error = loginError,
                                notice = loginNotice,
                                onSignIn = { id, pw, rem ->
                                    loginLoading = true; loginError = null; loginNotice = null
                                    scope.launch {
                                        when (val r = container.auth.signInWithIdentifier(id, pw, rem)) {
                                            is AppResult.Ok -> loggedIn.value = true
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                                onSignUp = { nm, em, un, pw, rem ->
                                    loginLoading = true; loginError = null; loginNotice = null
                                    scope.launch {
                                        when (val r = container.auth.signUpWithUsername(nm, em, un, pw, rem)) {
                                            is AppResult.Ok -> loggedIn.value = true
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                                onRecover = { id ->
                                    loginLoading = true; loginError = null; loginNotice = null
                                    scope.launch {
                                        when (val r = container.auth.requestRecovery(id)) {
                                            is AppResult.Ok -> {
                                                loginNotice = "ایمیلِ بازیابی فرستاده شد. لینکِ داخلش را کپی کن و در اپ بچسبان."
                                                loginLoading = false
                                            }
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                                onRecoverComplete = { link, pw ->
                                    loginLoading = true; loginError = null; loginNotice = null
                                    scope.launch {
                                        when (val r = container.auth.completeRecovery(link, pw)) {
                                            is AppResult.Ok -> {
                                                // لینکِ بازیابی فقط رمز را عوض می‌کند (سشن نمی‌سازد)؛
                                                // کاربر با همان رمزِ تازه از فرمِ ورود وارد می‌شود.
                                                loginNotice = "رمز عوض شد ✅ حالا با نام کاربری و رمز تازه وارد شو."
                                                loginLoading = false
                                            }
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                                onSendOtp = { id ->
                                    loginLoading = true; loginError = null; loginNotice = null
                                    scope.launch {
                                        when (val r = container.auth.sendOtp(id)) {
                                            is AppResult.Ok -> {
                                                loginNotice = "کد ۶ رقمی به ایمیلت فرستاده شد (۱۵ دقیقه اعتبار دارد)."
                                                loginLoading = false
                                            }
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                                onSignInOtp = { code, rem ->
                                    loginLoading = true; loginError = null; loginNotice = null
                                    scope.launch {
                                        when (val r = container.auth.signInWithOtp(code, rem)) {
                                            is AppResult.Ok -> loggedIn.value = true
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                },
                                onGoogle = {
                                    loginLoading = true; loginError = null; loginNotice = null
                                    scope.launch {
                                        when (val r = container.auth.signInWithGoogle(activity)) {
                                            is AppResult.Ok -> loggedIn.value = true
                                            is AppResult.Err -> { loginError = r.error.userMessage; loginLoading = false }
                                        }
                                    }
                                })

                            // ۱.۵) وارد شده ولی پروفایل دانش‌آموز ندارد → فرم ثبت‌نام (یک‌بار).
                            loggedIn.value == true && profileNeeded.value == true -> {
                                var saving by remember { mutableStateOf(false) }
                                var formError by remember { mutableStateOf<String?>(null) }
                                var email by remember { mutableStateOf("") }
                                var currentUsername by remember { mutableStateOf("") }
                                LaunchedEffect(Unit) {
                                    val me = runCatching { container.auth.currentUser() }.getOrNull()
                                    email = me?.email.orEmpty()
                                    currentUsername = me?.username?.ifBlank { null }
                                        ?: runCatching { container.auth.currentUsername() }.getOrNull().orEmpty()
                                }
                                com.hamyareman.ir.ui.profile.StudentProfileScreen(
                                    email = email,
                                    saving = saving,
                                    error = formError,
                                    currentUsername = currentUsername,
                                    onSubmit = { fn, ln, age, birthDate, grade, phone, gender, province, county, city, un, pw ->
                                        saving = true; formError = null
                                        scope.launch {
                                            // v1.65 — نام کاربریِ یکتا (+ رمز برای حسابِ گوگلی) پیش از
                                            // ثبتِ پروفایل ذخیره می‌شود تا ورودِ بعدی بدونِ گوگل/اینترنت ممکن شود.
                                            if (un.isNotBlank() && un != currentUsername) {
                                                val r = container.auth.saveUsername(un, pw.ifBlank { null }, null)
                                                if (r is AppResult.Err) {
                                                    formError = r.error.userMessage
                                                    saving = false
                                                    return@launch
                                                }
                                                currentUsername = un
                                            }
                                            val uid = container.auth.currentUserId().orEmpty()
                                            val ok = com.hamyareman.ir.ui.profile.StudentProfileRepo.save(
                                                container.tables,
                                                email,
                                                com.hamyareman.ir.ui.profile.StudentProfile(
                                                    userId = uid, email = email, firstName = fn,
                                                    lastName = ln, age = age, birthDate = birthDate, grade = grade, phone = phone,
                                                    province = province, county = county, city = city, gender = gender))
                                            if (ok) {
                                                com.hamyareman.ir.ui.profile.StudentProfileState.writeMirror(
                                                    activity, grade, true, fn,
                                                    com.hamyareman.ir.ui.profile.StudentProfileState.subscription,
                                                    gender)
                                                container.uiPrefs.applyDefaultForGender(gender)
                                                profileNeeded.value = false
                                            } else {
                                                formError = "ثبت در سرور انجام نشد؛ اینترنت را چک کن و دوباره بزن."
                                            }
                                            saving = false
                                        }
                                    })
                            }

                            // ۲) وارد شده و قفل باز: اپ.
                            isUnlocked -> {
                            ZahraNavHost()

                            // کانالِ آپدیت (v1.66): تنظیماتش روی سرور است (ردیفِ
                            // `app_release` در `app_state`) و فایل در ریپوی عمومیِ
                            // انتشار میماند. اگر نسخهٔ تازه‌تری باشد، همین‌جا پیام
                            // داده می‌شود و دانلود/نصب از خودِ اپ انجام می‌گیرد.
                            com.hamyareman.ir.ui.update.UpdateGateHost()
                            }
                            // ۳) وارد شده ولی قفل فعال: صفحه‌ی PIN.
                            else -> {
                            PinLockGate(
                                title = "همیار من قفل است",
                                subtitle = "برای دیدن دفترچه‌ات PIN را وارد کن.",
                                minLength = AppLock.MIN_PIN,
                                maxLength = AppLock.MAX_PIN,
                                biometricLabel = if (offerBiometric) "بازکردن با اثر انگشت/چهره" else null,
                                biometricBusy = bioBusy,
                                externalNotice = bioNotice,
                                onBiometricRequest = if (offerBiometric) {
                                    { unlockWithBiometric() }
                                } else {
                                    null
                                },
                                onVerify = { pin ->
                                    container.lock.verify(pin).also { ok -> if (ok) container.lock.markUnlocked() }
                                },
                                onUnlocked = { unlocked.value = true })
                            }
                        }
                    }
                }
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as HamyarApplication).container
        unlocked.value = !container.lock.isLockedNow()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureTeachIntent(intent)
        captureSleepIntent(intent)
    }

    private fun captureSleepIntent(intent: android.content.Intent?) {
        val sleepAction = com.hamyareman.ir.platform.feature.playback.SleepPlaybackService.OPEN_ACTION
        val extra = intent?.getBooleanExtra(com.hamyareman.ir.platform.feature.playback.SleepPlaybackService.OPEN_EXTRA, false) == true ||
            intent?.getBooleanExtra(com.hamyareman.ir.platform.core.notifications.ReminderReceiver.EXTRA_OPEN_SLEEP, false) == true
        if (intent?.action == sleepAction || extra) {
            com.hamyareman.ir.ui.study.SleepLaunch.pending = true
        }
    }

    /** extra درسِ اعلان پخش → [TeachLaunch] (nav به صفحه‌ی تدریس می‌پرد). */
    private fun captureTeachIntent(intent: android.content.Intent?) {
        val action = com.hamyareman.ir.platform.feature.playback.PlaybackService.TEACH_OPEN_ACTION
        val packId = intent?.getStringExtra(com.hamyareman.ir.platform.feature.playback.PlaybackService.TEACH_OPEN_EXTRA)
        if (intent?.action == action && !packId.isNullOrBlank()) {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = packId
            com.hamyareman.ir.platform.feature.playback.TeachGate.requestedPack = null
            return
        }
        // پلیِ اعلان وقتی اکتیویتی بدون extra بالا آمده (همان پروسه) — از گیتِ سرویس بخوان.
        val req = com.hamyareman.ir.platform.feature.playback.TeachGate.requestedPack
        if (!req.isNullOrBlank()) {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = req
            com.hamyareman.ir.platform.feature.playback.TeachGate.requestedPack = null
            return
        }
        // v1.25 — لمس اعلان با extra خالی (سرویس تازه ساخته شده): پکِ جاری‌ی سرویس را
        // مستقیم از TeachGate بخوان — زنجیره‌ی «همیشه همان درس» را می‌بندد.
        val live = com.hamyareman.ir.platform.feature.playback.TeachGate.currentPack
        if (intent?.action == action && !live.isNullOrBlank()) {
            com.hamyareman.ir.ui.study.TeachLaunch.pendingTeachPack = live
        }
    }

    override fun onPause() {
        super.onPause()
        // خروج از اپ = شروع دوباره‌ی تایمر قفل خودکار (اگر کاربر فعالش کرده باشد).
        (application as HamyarApplication).container.lock.lock()
    }

    /** خروج از حساب — صفحه‌ی ورود دوباره نشان داده می‌شود. */
    fun onLoggedOut() {
        loggedIn.value = false
        profileNeeded.value = null
        com.hamyareman.ir.platform.feature.playback.TeachGate.exit()
    }
}
