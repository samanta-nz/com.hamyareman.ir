package com.hamyareman.ir.platform.core.appwrite

import androidx.activity.ComponentActivity
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.enums.IdTokenProvider
import io.appwrite.enums.OAuthProvider
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import com.hamyareman.ir.platform.core.common.AppError
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.FunctionIds
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/** زمینهٔ قفل پایه + سقف دو دستگاه — از اپ (flavor) تزریق می‌شود. */
data class AuthGateContext(
    val gradeId: String,
    val deviceId: String,
    val deviceLabel: String,
)

/** کاربر جاری از دید اپ — نقش همیشه از Labels سرور می‌آید، نه از یک فیلد کلاینتی. */
data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val labels: List<String>,
    val role: UserRole,
    /** نامِ کاربریِ یکتا (v1.65) — خالی یعنی هنوز تعیین نشده. */
    val username: String = "",
) {
    val isGuest: Boolean get() = role == UserRole.GUEST
}

interface AuthService {
    val isConfigured: Boolean

    suspend fun currentUser(): AuthUser?
    suspend fun currentUserId(): String?
    suspend fun currentLabels(): List<String>
    suspend fun currentRole(): UserRole

    suspend fun signUp(name: String, email: String, password: String): AppResult<AuthUser>
    suspend fun signIn(email: String, password: String): AppResult<AuthUser>
    suspend fun signInWithGoogleIdToken(idToken: String): AppResult<AuthUser>
    suspend fun signInWithGoogle(activity: ComponentActivity): AppResult<AuthUser>
    suspend fun signInAsGuest(): AppResult<AuthUser>
    suspend fun logout(): AppResult<Unit>

    // --- v1.65: نام کاربری، «مرا به خاطر بسپار»، ورودِ آفلاین، بازیابیِ رمز، OTP ---

    /** ورود با «نام کاربری یا ایمیل» + رمز. [remember] = مرا به خاطر بسپار. */
    suspend fun signInWithIdentifier(
        identifier: String,
        password: String,
        remember: Boolean,
    ): AppResult<AuthUser>

    /** ثبت‌نام با ایمیل + نام کاربری + رمز (یکتاییِ نام کاربری سمت سرور چک می‌شود). */
    suspend fun signUpWithUsername(
        name: String,
        email: String,
        username: String,
        password: String,
        remember: Boolean,
    ): AppResult<AuthUser>

    /**
     * تعیین/تغییرِ نام کاربریِ حسابِ جاری؛ اگر [newPassword] داده شود رمز هم عوض
     * می‌شود (برای حسابِ گوگلی بدونِ رمزِ قبلی؛ حسابِ ایمیلی [currentPassword] می‌خواهد).
     */
    suspend fun saveUsername(
        username: String,
        newPassword: String? = null,
        currentPassword: String? = null,
    ): AppResult<String>

    /** برای حسابی که با Google ساخته شده، یک رمز اختیاری روی همان ایمیل می‌گذارد. */
    suspend fun setPasswordForCurrentEmail(newPassword: String): AppResult<Unit>

    /** روش آخرین ورود موفق؛ برای فرم پروفایل یک‌باره روی نصب مجدد هم نگه داشته می‌شود. */
    fun lastSignInWasGoogle(): Boolean

    suspend fun currentUsername(): String?

    /** فرستادنِ ایمیلِ بازیابیِ رمز به آدرسِ همین نام کاربری/ایمیل. */
    suspend fun requestRecovery(identifier: String): AppResult<Unit>

    /** کامل‌کردنِ بازیابی با لینکِ داخلِ ایمیل (userId و secret از همان لینک خوانده می‌شود). */
    suspend fun completeRecovery(link: String, newPassword: String): AppResult<Unit>

    /** ارسال و تکمیل تأیید ایمیل؛ صفحهٔ تأیید در UI از ثبت‌نام جداست. */
    suspend fun requestEmailVerification(): AppResult<Unit>
    suspend fun completeEmailVerification(link: String): AppResult<Unit>

    /** فرستادنِ کد ۶ رقمی یک‌بارمصرف به ایمیلِ همان نام کاربری/ایمیل. */
    suspend fun sendOtp(identifier: String): AppResult<Unit>

    /** ورود با کد یک‌بارمصرف (بدونِ رمز) — برای وقتی رمز یادشان رفته. */
    suspend fun signInWithOtp(otp: String, remember: Boolean): AppResult<AuthUser>

    /** روشن/خاموش کردنِ «مرا به خاطر بسپار» (ورودِ آفلاینِ دفعهٔ بعد). */
    fun setRemember(on: Boolean)

    /** هویتِ کش‌شده روی دستگاه — بدونِ هیچ درخواستِ شبکه‌ای. */
    fun cachedUser(): AuthUser?

    /** نام کاربریِ کش‌شده (بدونِ suspend). */
    fun cachedUsername(): String?
}

/**
 * پیاده‌سازی Appwrite.
 *
 * نکات مهم:
 *  - نقش کاربر با Label سمت سرور تعیین می‌شود (`zahra`) و هرگز یک فیلدِ نوشتنیِ
 *    کلاینتی نیست. تابع `user-bootstrap` برچسب پایه را می‌گذارد و ورود را به
 *    همان پایه + حداکثر دو دستگاه قفل می‌کند. اگر تابع مستقر نباشد ورود قطع نمی‌شود.
 *  - در حالت محلی (بدون projectId) یک کاربر محلی با [fallbackRole] برگردانده می‌شود
 *    تا UI و جریان‌ها قابل تست باشند.
 *  - **v1.65 — ورودِ آفلاین:** فقط «بارِ اول» اینترنت لازم است. بعد از هر ورودِ
 *    موفق، پروفایلِ هویت روی دستگاه کش می‌شود و اگر «مرا به خاطر بسپار» روشن باشد
 *    [currentUser] بدونِ هیچ درخواستی همان را برمی‌گرداند و تازه‌سازی در پس‌زمینه
 *    انجام می‌شود؛ پس نصبِ دوباره/نبودِ نت، کاربر را پشتِ دروازهٔ لاگین نگه نمی‌دارد.
 *  - **v1.65 — نام کاربری:** Appwrite فقط با ایمیل سشن می‌سازد، پس نگاشتِ
 *    «نام کاربری → ایمیل» در جدولِ [TableIds.USERS] نگه داشته می‌شود. شناسهٔ سطر از
 *    هشِ خودِ نام ساخته می‌شود ⇒ خواندنِ نگاشت پیش از ورود (با شناسه، بدونِ list)
 *    ممکن است و یکتایی هم تضمین می‌شود.
 */
class AppwriteAuthService(
    private val provider: AppwriteClientProvider,
    private val fallbackRole: UserRole,
    private val store: LocalStore? = null,
    private val functions: FunctionsService? = null,
    private val gateContext: () -> AuthGateContext? = { null },
) : AuthService {

    override val isConfigured: Boolean get() = provider.isConfigured

    private val account get() = Account(provider.client)

    /** دسترسیِ مستقیم به جدولِ نگاشتِ نام کاربری (بدونِ وابستگی به DI بیرونی). */
    private val tables: TablesDbService by lazy { AppwriteTablesDbService(provider) }

    private val localUser = AuthUser(
        id = "local-${fallbackRole.label}",
        name = fallbackRole.label,
        email = "",
        labels = listOf(fallbackRole.label),
        role = fallbackRole,
    )

    // ---------------------------------------------------------------- هویتِ جاری

    /** نسخهٔ سرورِ کاربر؛ اگر موفق باشد کشِ محلی هم به‌روز می‌شود. */
    private suspend fun fetchRemote(): AuthUser? = runCatching {
        val user = account.get()
        val labels = user.labels
        AuthUser(
            id = user.id,
            name = user.name,
            email = user.email,
            labels = labels,
            role = roleOf(labels),
            username = cachedUsername().orEmpty(),
        ).also { remember(it) }
    }.getOrNull()

    /**
     * نقش از Labelهای سرور؛ ولی **نبودِ Label** یعنی «هنوز کسی نقش نداده»، نه «مهمان».
     *
     * چرا: تابعِ `user-bootstrap` که Label را سمتِ سرور می‌گذارد، در حال حاضر روی پروژه
     * مستقر نیست (در کنسول فقط `google-auth` و `ai-companion` هستند) و هر سه کاربرِ
     * واقعی `labels: []` دارند. با `fromLabels` همه‌شان `GUEST` می‌شدند و همان نقش در
     * کشِ محلی هم می‌نشست. پس در این حالت نقشِ پیش‌فرضِ خودِ اپ ([fallbackRole])
     * برمی‌گردد — همان کاری که [cachedRole] از قبل می‌کرد. Labelِ صریحِ سرور
     * (مثلاً `father` بعد از پیوند) همیشه بر این پیش‌فرض اولویت دارد.
     */
    private fun roleOf(labels: List<String>): UserRole {
        if (labels.any { it.equals(UserRole.FATHER.label, true) }) return UserRole.FATHER
        if (labels.any { it.equals(UserRole.ZAHRA.label, true) }) return UserRole.ZAHRA
        if (labels.any { it.equals(UserRole.GUEST.label, true) }) return UserRole.GUEST
        // لیبل پایه/ادمین بدون zahra را مهمان نکن — اپ دانش‌آموز همان نقش پیش‌فرض می‌ماند.
        return fallbackRole
    }

    override suspend fun currentUser(): AuthUser? {
        if (!provider.isConfigured) return localUser
        val cached = cachedUser()
        // «مرا به خاطر بسپار» روشن + پروفایلِ کش‌شده ⇒ بدونِ انتظار برای شبکه وارد می‌شویم.
        if (cached != null && rememberMe()) {
            refreshInBackground()
            return cached
        }
        return fetchRemote()
    }

    override suspend fun currentUserId(): String? = currentUser()?.id

    override suspend fun currentLabels(): List<String> = currentUser()?.labels ?: emptyList()

    override suspend fun currentRole(): UserRole = currentUser()?.role ?: UserRole.GUEST

    private val lastRefresh = AtomicLong(0)

    /** تازه‌سازیِ هویت در پس‌زمینه، حداکثر یک‌بار در هر [REFRESH_THROTTLE_MS]. */
    private fun refreshInBackground() {
        val now = System.currentTimeMillis()
        val prev = lastRefresh.get()
        if (now - prev < REFRESH_THROTTLE_MS) return
        if (!lastRefresh.compareAndSet(prev, now)) return
        CoroutineScope(Dispatchers.IO).launch { runCatching { fetchRemote() } }
    }

    // --------------------------------------------------------------------- ورود

    override suspend fun signUp(name: String, email: String, password: String): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        setRemember(true)
        return runCatching {
            runCatching { account.deleteSession("current") }
            account.create(userId = ID.unique(), email = email, password = password, name = name)
            account.createEmailPasswordSession(email = email, password = password)
            afterSession().also { if (it is AppResult.Ok) markAuthMethod(google = false) }
        }.getOrElse { AppResult.Err(mapAuth(it, "رمز یا ایمیل نامعتبر است.", "این ایمیل قبلاً ثبت شده.")) }
    }

    override suspend fun signIn(email: String, password: String): AppResult<AuthUser> =
        signInWithIdentifier(email, password, remember = true)

    override suspend fun signInWithIdentifier(
        identifier: String,
        password: String,
        remember: Boolean,
    ): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        setRemember(remember)
        val email = emailForIdentifier(identifier)
            ?: return AppResult.Err(
                AppError.Validation(
                    "نام کاربری «${normalizeUsername(identifier)}» در سرور ثبت نشده. " +
                        "با ایمیل وارد شو یا از «فراموشی رمز عبور» کد یک‌بارمصرف بگیر.",
                ),
            )
        return runCatching {
            runCatching { account.deleteSession("current") }
            account.createEmailPasswordSession(email = email, password = password)
            afterSession().also {
                if (it is AppResult.Ok) {
                    markAuthMethod(google = false)
                    adoptUsername(it.value.id, email)
                }
            }
        }.getOrElse { AppResult.Err(mapAuth(it, "نام کاربری/ایمیل یا رمز عبور درست نیست.")) }
    }

    override suspend fun signUpWithUsername(
        name: String,
        email: String,
        username: String,
        password: String,
        remember: Boolean,
    ): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        val u = normalizeUsername(username)
        usernameError(u)?.let { return AppResult.Err(AppError.Validation(it)) }
        if (password.length < 8) {
            return AppResult.Err(AppError.Validation("رمز باید حداقل ۸ کاراکتر باشد."))
        }
        setRemember(remember)
        // یکتاییِ نام کاربری روی سرور، پیش از ساختِ حساب.
        takenByOther(u, null)?.let { return AppResult.Err(AppError.Validation(it)) }
        return runCatching {
            runCatching { account.deleteSession("current") }
            account.create(userId = ID.unique(), email = email, password = password, name = name)
            account.createEmailPasswordSession(email = email, password = password)
            val res = afterSession()
            if (res is AppResult.Ok) {
                markAuthMethod(google = false)
                publishUsername(res.value.id, u, email)
                store?.putString(KEY_USERNAME, u)
                remember(res.value.copy(username = u))
            }
            res
        }.getOrElse { AppResult.Err(mapAuth(it, "رمز یا ایمیل نامعتبر است.", "این ایمیل قبلاً ثبت شده.")) }
    }

    override suspend fun signInAsGuest(): AppResult<AuthUser> {
        return AppResult.Err(AppError.Auth("ورود مهمان حذف شده است. با نام کاربری یا ایمیل وارد شو."))
    }

    /** ورود native: Credential Manager توکن گوگل را می‌گیرد و Appwrite آن را اعتبارسنجی می‌کند. */
    override suspend fun signInWithGoogleIdToken(idToken: String): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        if (idToken.isBlank()) return AppResult.Err(AppError.Validation("توکن ورود گوگل دریافت نشد."))
        setRemember(true)
        return runCatching {
            runCatching { account.deleteSession("current") }
            account.createIdTokenSession(provider = IdTokenProvider.GOOGLE, idToken = idToken)
            afterSession().also {
                if (it is AppResult.Ok) {
                    markAuthMethod(google = true)
                    adoptUsername(it.value.id, it.value.email)
                }
            }
        }.getOrElse { AppResult.Err(mapAuth(it, "ورود امن با گوگل ناموفق بود.")) }
    }

    /** مسیر مرورگری قدیمی فقط برای سازگاری داخلی؛ UI نسخهٔ ۲٫۰ از native استفاده می‌کند. */
    override suspend fun signInWithGoogle(activity: ComponentActivity): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        setRemember(true)
        return runCatching {
            // مهم: اگر سشن فعلی (مهمان یا لاگین قبلی) زنده باشد، Appwrite اکانت گوگل را
            // به همان کاربرِ فعلی می‌چسباند و کاربر جدیدی با ایمیل جدید ساخته نمی‌شود
            // (همان باگ «فقط ایمیل اولین ورود ذخیره می‌شود»). پس اول سشن فعلی را
            // تمام می‌کنیم تا OAuth همیشه از صفر شروع کند و هر اکانت گوگل، کاربر خودش را بسازد.
            runCatching { account.deleteSession("current") }
            account.createOAuth2Session(activity = activity, provider = OAuthProvider.GOOGLE)
            afterSession().also {
                if (it is AppResult.Ok) {
                    markAuthMethod(google = true)
                    adoptUsername(it.value.id, it.value.email)
                }
            }
        }.getOrElse { AppResult.Err(mapAuth(it, "ورود با گوگل ناموفق بود.")) }
    }

    override suspend fun logout(): AppResult<Unit> {
        // اول پاک‌کردنِ محلی: حتی اگر نت نباشد، کاربر از دستگاه خارج می‌شود.
        forgetRole()
        if (!provider.isConfigured) return AppResult.Ok(Unit)
        runCatching { account.deleteSession("current") }
        return AppResult.Ok(Unit)
    }

    // ------------------------------------------------- نام کاربری و بازیابیِ رمز

    override suspend fun saveUsername(
        username: String,
        newPassword: String?,
        currentPassword: String?,
    ): AppResult<String> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Auth())
        val me = fetchRemote() ?: cachedUser()
            ?: return AppResult.Err(AppError.Auth())
        val u = normalizeUsername(username)
        usernameError(u)?.let { return AppResult.Err(AppError.Validation(it)) }
        takenByOther(u, me.id)?.let { return AppResult.Err(AppError.Validation(it)) }

        val pw = newPassword?.trim().orEmpty()
        if (pw.isNotEmpty()) {
            if (pw.length < 8) return AppResult.Err(AppError.Validation("رمز باید حداقل ۸ کاراکتر باشد."))
            // حسابِ گوگلی رمزِ قبلی ندارد ⇒ بدونِ oldPassword عوض می‌شود؛ حسابِ ایمیلی
            // رمزِ فعلی را می‌خواهد (سرور بدونِ آن 401 می‌دهد) ⇒ همان‌جا fallback.
            val ok = runCatching { account.updatePassword(pw) }.isSuccess ||
                (currentPassword?.takeIf { it.isNotBlank() }?.let { old ->
                    runCatching { account.updatePassword(pw, old) }.isSuccess
                } ?: false)
            if (!ok) {
                return AppResult.Err(
                    AppError.Validation(
                        "رمز عوض نشد. اگر قبلاً با ایمیل و رمز ثبت‌نام کرده‌ای، رمزِ فعلی را هم بنویس؛ " +
                            "وگرنه از «فراموشی رمز عبور» استفاده کن.",
                    ),
                )
            }
        }

        val old = cachedUsername().orEmpty()
        publishUsername(me.id, u, me.email)
        if (old.isNotBlank() && old != u) {
            runCatching { tables.delete(TableIds.USERS, usernameRowId(old)) }
        }
        store?.putString(KEY_USERNAME, u)
        remember(me.copy(username = u))
        return AppResult.Ok(u)
    }

    override suspend fun setPasswordForCurrentEmail(newPassword: String): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Auth())
        val password = newPassword.trim()
        if (password.isEmpty()) return AppResult.Ok(Unit)
        if (password.length < 8) {
            return AppResult.Err(AppError.Validation("رمز باید حداقل ۸ کاراکتر باشد."))
        }
        return runCatching {
            // حسابی که با Google ساخته شده رمز قبلی ندارد؛ Appwrite در این حالت
            // oldPassword را اختیاری می‌داند و رمز روی همان ایمیل حساب ثبت می‌شود.
            account.updatePassword(password)
            AppResult.Ok(Unit)
        }.getOrElse {
            AppResult.Err(mapAuth(it, "ثبت رمز برای این ایمیل انجام نشد؛ از بازیابی رمز استفاده کن."))
        }
    }

    override fun lastSignInWasGoogle(): Boolean =
        store?.getString(KEY_AUTH_METHOD) == AUTH_METHOD_GOOGLE

    private fun markAuthMethod(google: Boolean) {
        store?.putString(KEY_AUTH_METHOD, if (google) AUTH_METHOD_GOOGLE else AUTH_METHOD_EMAIL)
    }

    override suspend fun currentUsername(): String? {
        cachedUsername()?.let { return it }
        if (!provider.isConfigured) return null
        val uid = cachedUserId() ?: currentUserId() ?: return null
        val row = (tables.get(TableIds.USERS, ownerRowId(uid)) as? AppResult.Ok)?.value ?: return null
        val u = row.string("username")
        if (u.isBlank()) return null
        store?.putString(KEY_USERNAME, u)
        return u
    }

    override suspend fun requestRecovery(identifier: String): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Auth())
        val email = emailForIdentifier(identifier)
            ?: return AppResult.Err(
                AppError.Validation("نام کاربری «${normalizeUsername(identifier)}» در سرور ثبت نشده."),
            )
        return runCatching {
            account.createRecovery(email = email, url = RECOVERY_URL)
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(mapAuth(it, "فرستادنِ ایمیلِ بازیابی ناموفق بود.")) }
    }

    override suspend fun completeRecovery(link: String, newPassword: String): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Auth())
        if (newPassword.length < 8) {
            return AppResult.Err(AppError.Validation("رمز باید حداقل ۸ کاراکتر باشد."))
        }
        val userId = extractParam(link, "userId")
        val secret = extractParam(link, "secret")
        if (userId.isNullOrBlank() || secret.isNullOrBlank()) {
            return AppResult.Err(
                AppError.Validation(
                    "لینکِ کامل را از ایمیل کپی کن (باید userId و secret داشته باشد).",
                ),
            )
        }
        return runCatching {
            account.updateRecovery(userId = userId, secret = secret, password = newPassword)
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(mapAuth(it, "لینکِ بازیابی نامعتبر یا منقضی است (یک ساعت اعتبار دارد).")) }
    }

    override suspend fun requestEmailVerification(): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Auth())
        return runCatching {
            account.createVerification(url = VERIFY_URL)
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(mapAuth(it, "فرستادن ایمیل تأیید ناموفق بود.")) }
    }

    override suspend fun completeEmailVerification(link: String): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Auth())
        val userId = extractParam(link, "userId")
        val secret = extractParam(link, "secret")
        if (userId.isNullOrBlank() || secret.isNullOrBlank()) {
            return AppResult.Err(AppError.Validation("لینک کامل تأیید را از ایمیل کپی کن."))
        }
        return runCatching {
            account.updateVerification(userId = userId, secret = secret)
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(mapAuth(it, "لینک تأیید نامعتبر یا منقضی است.")) }
    }

    override suspend fun sendOtp(identifier: String): AppResult<Unit> {
        if (!provider.isConfigured) return AppResult.Err(AppError.Auth())
        val email = emailForIdentifier(identifier)
            ?: return AppResult.Err(
                AppError.Validation("نام کاربری «${normalizeUsername(identifier)}» در سرور ثبت نشده."),
            )
        return runCatching {
            // برای ایمیلِ ثبت‌شده، userIdِ داده‌شده نادیده گرفته می‌شود و همان حسابِ
            // موجود برمی‌گردد؛ کدِ ۶ رقمی به ایمیل می‌رود و ۱۵ دقیقه اعتبار دارد.
            val token = account.createEmailToken(userId = ID.unique(), email = email)
            store?.putString(KEY_OTP_UID, token.userId)
            AppResult.Ok(Unit)
        }.getOrElse { AppResult.Err(mapAuth(it, "فرستادنِ کد یک‌بارمصرف ناموفق بود.")) }
    }

    override suspend fun signInWithOtp(otp: String, remember: Boolean): AppResult<AuthUser> {
        if (!provider.isConfigured) return AppResult.Ok(localUser)
        val uid = store?.getString(KEY_OTP_UID).orEmpty()
        if (uid.isBlank()) {
            return AppResult.Err(AppError.Validation("اول کد را به ایمیلت بفرست."))
        }
        setRemember(remember)
        return runCatching {
            runCatching { account.deleteSession("current") }
            account.createSession(userId = uid, secret = otp.trim())
            afterSession().also {
                if (it is AppResult.Ok) {
                    markAuthMethod(google = false)
                    adoptUsername(it.value.id, it.value.email)
                }
            }
        }.getOrElse { AppResult.Err(mapAuth(it, "کد یک‌بارمصرف درست نیست یا منقضی شده.")) }
    }

    // -------------------------------------------------------- نگاشتِ نام کاربری

    /** ایمیلِ یک «نام کاربری یا ایمیل»؛ اگر پیدا نشود null. */
    private suspend fun emailForIdentifier(identifier: String): String? {
        val id = identifier.trim()
        if (id.isEmpty()) return null
        if (id.contains("@")) return id.lowercase()
        val row = (tables.get(TableIds.USERS, usernameRowId(normalizeUsername(id))) as? AppResult.Ok)?.value
        return row?.string("email")?.takeIf { it.isNotBlank() }
    }

    /** اگر این نام کاربری مالِ کاربرِ دیگری باشد، پیامِ خطا؛ وگرنه null. */
    private suspend fun takenByOther(username: String, myUserId: String?): String? {
        val row = (tables.get(TableIds.USERS, usernameRowId(username)) as? AppResult.Ok)?.value
            ?: return null
        val owner = row.string("userId")
        if (owner.isBlank() || owner == myUserId) return null
        return "نام کاربری «$username» قبلاً گرفته شده. یکی دیگر انتخاب کن."
    }

    /**
     * بعد از ورود، نام کاربریِ همین حساب از سرور خوانده و کش می‌شود (روی نصبِ تازه
     * هم «نام کاربریِ من» در پروفایل دیده می‌شود).
     */
    private suspend fun adoptUsername(userId: String, email: String) {
        if (userId.isBlank()) return
        if (!cachedUsername().isNullOrBlank()) return
        val row = (tables.get(TableIds.USERS, ownerRowId(userId)) as? AppResult.Ok)?.value ?: return
        val u = row.string("username")
        if (u.isNotBlank()) {
            store?.putString(KEY_USERNAME, u)
            // اگر ایمیلِ حساب عوض شده باشد، نگاشتِ عمومی هم تازه می‌شود.
            if (row.string("email") != email && email.isNotBlank()) {
                publishUsername(userId, u, email)
            }
        }
    }

    /**
     * نوشتنِ نگاشتِ عمومی (نام → ایمیل) + ردیفِ خصوصیِ «نام کاربریِ من».
     *
     * نکتهٔ سروری: در جدولِ [TableIds.USERS] **ایندکسِ یکتا روی `username` نداریم**
     * (عمداً حذف شد) چون هر نام کاربری دو سطر دارد و ایندکس، سطرِ دوم را با
     * `409 document_unique_constraint_violation` رد می‌کرد. یکتاییِ نام کاربری را
     * خودِ «شناسهٔ قطعیِ سطر» تضمین می‌کند: دو کاربر نمی‌توانند هم‌زمان مالکِ
     * `u_<هشِ یک نام>` باشند — [takenByOther] پیش از نوشتن مالک را چک می‌کند و
     * دسترسیِ `update/delete` همان سطر هم فقط به مالک داده شده است. اگر روزی
     * ایندکسِ یکتا برگشت، این دو سطر باید به دو جدولِ جدا بروند.
     */
    private suspend fun publishUsername(userId: String, username: String, email: String) {
        val data = mapOf(
            "userId" to userId,
            "username" to username,
            "email" to email,
            "updatedAt" to System.currentTimeMillis(),
        )
        val owner = Role.user(userId)
        // سطرِ عمومی: فقط با شناسهٔ سطر خوانده می‌شود (listِ عمومی وجود ندارد).
        tables.upsert(
            TableIds.USERS,
            usernameRowId(username),
            data,
            listOf(
                Permission.read(Role.any()),
                Permission.update(owner),
                Permission.write(owner),
                Permission.delete(owner),
            ),
        )
        // سطرِ خصوصی: «نام کاربریِ من» روی هر دستگاهِ تازه.
        tables.upsert(
            TableIds.USERS,
            ownerRowId(userId),
            data,
            listOf(
                Permission.read(owner),
                Permission.update(owner),
                Permission.write(owner),
                Permission.delete(owner),
            ),
        )
    }

    /** نام کاربریِ معتبر: ۳ تا ۲۴ کاراکتر، فقط `a-z`، رقم و `_`. */
    fun normalizeUsername(raw: String): String = raw.trim().lowercase().replace(' ', '_')

    fun usernameError(username: String): String? = when {
        username.length < 3 -> "نام کاربری باید حداقل ۳ کاراکتر باشد."
        username.length > 24 -> "نام کاربری حداکثر ۲۴ کاراکتر است."
        !username.matches(USERNAME_REGEX) ->
            "نام کاربری فقط حروف انگلیسی کوچک، عدد و _ — بدون فاصله و حرف فارسی."
        else -> null
    }

    /** `userId` و `secret` از لینکِ ایمیلِ بازیابی (کپی‌شده در اپ). */
    private fun extractParam(link: String, name: String): String? {
        val m = Regex("[?&]$name=([^&#\\s]+)").find(link) ?: return null
        val raw = m.groupValues[1]
        return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrNull() ?: raw
    }

    // ------------------------------------------------------------------- کشِ محلی

    /** بعد از ساخت سشن: قفل پایه + سقف دو دستگاه، بعد هویت. */
    private suspend fun afterSession(): AppResult<AuthUser> {
        when (val g = bootstrap()) {
            is AppResult.Err -> return g
            is AppResult.Ok -> Unit
        }
        return requireUser()
    }

    /**
     * صدا زدن `user-bootstrap`: قفل ایمیل به یک پایه و حداکثر دو دستگاه.
     * اگر تابع مستقر نباشد ورود قطع نمی‌شود؛ رد صریح (پایه/دستگاه) نشست را می‌بندد.
     */
    private suspend fun bootstrap(): AppResult<Unit> {
        val svc = functions ?: return AppResult.Ok(Unit)
        val ctx = gateContext() ?: return AppResult.Ok(Unit)
        val body = JSONObject()
            .put("app", "zahra")
            .put("grade", ctx.gradeId)
            .put("deviceId", ctx.deviceId)
            .put("deviceLabel", ctx.deviceLabel)
            .toString()
        return when (val r = svc.call(FunctionIds.USER_BOOTSTRAP, body)) {
            is AppResult.Ok -> {
                val parsed = runCatching { JSONObject(r.value.body.ifBlank { "{}" }) }.getOrNull()
                val ok = parsed?.optBoolean("ok", true) ?: true
                if (ok) return AppResult.Ok(Unit)
                val code = parsed?.optString("code").orEmpty()
                if (code == "GRADE_MISMATCH" || code == "DEVICE_LIMIT") {
                    runCatching { account.deleteSession("current") }
                    forgetRole()
                    AppResult.Err(AppError.Permission(gateMessage(parsed)))
                } else {
                    AppResult.Ok(Unit)
                }
            }
            is AppResult.Err -> AppResult.Ok(Unit)
        }
    }

    private fun gateMessage(o: JSONObject?): String {
        val msg = o?.optString("messageFa").orEmpty().ifBlank { o?.optString("error").orEmpty() }
        return when (o?.optString("code")) {
            "GRADE_MISMATCH" ->
                msg.ifBlank { "این ایمیل برای پایهٔ دیگری ثبت شده و نمی‌تواند وارد این اپ شود." }
            "DEVICE_LIMIT" ->
                msg.ifBlank { "این حساب روی دو دستگاه دیگر فعال است." }
            else -> msg.ifBlank { "ورود در این دستگاه مجاز نیست." }
        }
    }

    /** بررسی دوبارهٔ قفل پایه/دستگاه برای سشنِ از قبل باز. */
    suspend fun enforceAccountGate(): AppResult<Unit> = bootstrap()

    private suspend fun requireUser(): AppResult<AuthUser> =
        fetchRemote()?.let { AppResult.Ok(it) }
            ?: cachedUser()?.let { AppResult.Ok(it) }
            ?: AppResult.Err(AppError.Auth())

    /**
     * کشِ کاملِ هویت: هم برای ساخت دسترسی سطرها (`Role.user(id)`) و نمایش فوری نقش،
     * هم برای **ورودِ آفلاین** از دفعهٔ دوم به بعد.
     */
    private fun remember(user: AuthUser) {
        store?.putString(KEY_ROLE, user.role.label)
        store?.putString(KEY_USER_ID, user.id)
        if (user.username.isNotBlank()) store?.putString(KEY_USERNAME, user.username)
        store?.putString(
            KEY_PROFILE,
            JSONObject()
                .put("id", user.id)
                .put("name", user.name)
                .put("email", user.email)
                .put("labels", JSONArray(user.labels))
                .put("role", user.role.label)
                .put("username", user.username)
                .toString(),
        )
    }

    private fun forgetRole() {
        store?.remove(KEY_ROLE, KEY_USER_ID, KEY_PROFILE, KEY_USERNAME, KEY_REMEMBER, KEY_OTP_UID, KEY_AUTH_METHOD)
    }

    /** نقش کش‌شده برای نمایش فوری UI قبل از رسیدن پاسخ سرور. */
    fun cachedRole(): UserRole =
        store?.getString(KEY_ROLE)?.takeIf { it.isNotBlank() }
            ?.let { label -> UserRole.entries.firstOrNull { it.label == label } }
            ?: fallbackRole

    /** شناسه‌ی کاربر جاری از کش محلی (بدون نیاز به suspend). */
    fun cachedUserId(): String? = store?.getString(KEY_USER_ID)?.takeIf { it.isNotBlank() }

    override fun cachedUsername(): String? = store?.getString(KEY_USERNAME)?.takeIf { it.isNotBlank() }

    override fun cachedUser(): AuthUser? {
        val raw = store?.getString(KEY_PROFILE).orEmpty()
        if (raw.isBlank()) return null
        return runCatching {
            val o = JSONObject(raw)
            val arr = o.optJSONArray("labels")
            val labels = if (arr == null) emptyList() else (0 until arr.length()).map { arr.optString(it) }
            val roleLabel = o.optString("role")
            AuthUser(
                id = o.optString("id"),
                name = o.optString("name"),
                email = o.optString("email"),
                labels = labels,
                role = UserRole.entries.firstOrNull { it.label == roleLabel } ?: fallbackRole,
                username = o.optString("username").ifBlank { cachedUsername().orEmpty() },
            ).takeIf { it.id.isNotBlank() }
        }.getOrNull()
    }

    override fun setRemember(on: Boolean) { store?.putBool(KEY_REMEMBER, on) }

    private fun rememberMe(): Boolean = store?.getBool(KEY_REMEMBER, true) ?: true

    /** خطاهای احرازِ هویت با پیامِ قابلِ فهم برای کاربر. */
    private fun mapAuth(
        throwable: Throwable,
        credentialMessage: String,
        conflictMessage: String = "این ایمیل قبلاً ثبت شده.",
    ): AppError {
        val e = throwable as? AppwriteException
        val type = runCatching { e?.type }.getOrNull().orEmpty()
        val code = runCatching { e?.code }.getOrNull() ?: 0
        return when {
            type.contains("already_exists", ignoreCase = true) || code == 409 ->
                AppError.Validation(conflictMessage)
            type.contains("invalid_credentials", ignoreCase = true) ||
                type.contains("invalid_password", ignoreCase = true) || code == 401 ->
                AppError.Auth(credentialMessage)
            code == 0 || type.contains("network", ignoreCase = true) -> AppError.Network()
            else -> AppwriteErrors.map(throwable, credentialMessage)
        }
    }

    private fun usernameRowId(username: String) = hashRowId("u_", username)

    private fun ownerRowId(userId: String) = hashRowId("p_", userId)

    /**
     * شناسهٔ سطرِ قطعی. Appwrite برای `documentId` حداکثر ۳۶ کاراکتر قبول می‌کند و
     * باید با حرف/رقم شروع شود ⇒ هشِ SHA-256 با یک پیشوندِ دو کاراکتری (۳۲ کاراکتر).
     */
    private fun hashRowId(prefix: String, value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        val hex = StringBuilder(32)
        digest.forEach { hex.append("%02x".format(it.toInt() and 0xFF)) }
        return prefix + hex.substring(0, 30)
    }

    companion object {
        /** همین کلیدها را ماژول‌های دیگر (حرف دل، تماس) برای دسترسی سطرها می‌خوانند. */
        const val KEY_ROLE = "auth_role"
        const val KEY_USER_ID = "auth_user_id"

        /** v1.65 — کشِ هویت برای ورودِ آفلاین و «مرا به خاطر بسپار». */
        const val KEY_PROFILE = "auth_profile"
        const val KEY_REMEMBER = "auth_remember"
        const val KEY_USERNAME = "auth_username"
        const val KEY_OTP_UID = "auth_otp_uid"
        const val KEY_AUTH_METHOD = "auth_method"
        private const val AUTH_METHOD_GOOGLE = "google"
        private const val AUTH_METHOD_EMAIL = "email"

        /** Deep links متعلق به Android Platform پروژه؛ هیچ دامنهٔ غیرفعالی استفاده نمی‌شود. */
        const val RECOVERY_URL = "appwrite-callback-6abb134a002025222005://recovery"
        const val VERIFY_URL = "appwrite-callback-6abb134a002025222005://verify"

        private const val REFRESH_THROTTLE_MS = 5 * 60 * 1000L
        private val USERNAME_REGEX = Regex("^[a-z0-9_]{3,24}$")
    }
}
