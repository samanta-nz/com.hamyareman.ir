package com.hamyareman.ir.ui.profile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.hamyareman.ir.platform.core.appwrite.RowPermissions
import com.hamyareman.ir.platform.core.appwrite.TablesDbService
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds

/** پایه‌های تحصیلی — هر APK یکی از این‌هاست (چهارم تا دوازدهم). */
enum class GradeLevel(val id: String, val fa: String, val num: Int) {
    G4("grade4", "پایه چهارم", 4),
    G5("grade5", "پایه پنجم", 5),
    G6("grade6", "پایه ششم", 6),
    G7("grade7", "پایه هفتم", 7),
    G8("grade8", "پایه هشتم", 8),
    G9("grade9", "پایه نهم", 9),
    G10("grade10", "پایه دهم", 10),
    G11("grade11", "پایه یازدهم", 11),
    G12("grade12", "پایه دوازدهم", 12);

    companion object {
        fun byId(id: String?): GradeLevel = entries.firstOrNull { it.id == id } ?: G9
        fun byNum(n: Int?): GradeLevel? = entries.firstOrNull { it.num == n }
    }
}

/**
 * نگاشت کتاب → پایه از روی کد کتاب (`C905` → نهم، `C10xx` → دهم).
 * اگر کد شناخته نشود، کتاب در هیچ پایه‌ی دیگری دیده نمی‌شود مگر همین اپ.
 */
fun gradeOfBook(bookCode: String): GradeLevel {
    val m = Regex("^C(1[0-2]|[1-9])").find(bookCode.trim())
    val n = m?.groupValues?.get(1)?.toIntOrNull()
    return GradeLevel.byNum(n) ?: AppEdition.grade
}

/**
 * راه‌اندازیِ دوباره‌ی اپ — برای وقتی که آیکون لانچر عوض شده و فقط با
 * بسته‌شدن کاملِ لانچر آیکونِ تازه روی صفحه‌ی گوشی می‌نشیند.
 */
fun restartHamyar(ctx: android.content.Context) {
    val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    runCatching { intent?.let { ctx.startActivity(it) } }
    android.os.Process.killProcess(android.os.Process.myPid())
}

/**
 * وضعیت زنده‌ی پایه‌ی کاربر — آینه‌ی محلی (فوری) + تأیید ابری (سینک).
 * صفحه‌های فهرست کتاب با [GradeGate] می‌خوانند.
 */
object StudentProfileState {
    @Volatile var grade: GradeLevel = GradeLevel.G9
    @Volatile var hasProfile: Boolean = false

    /** نام کوچک برای خوش‌آمد داشبورد — state تا UI پس از ذخیره فوری تازه شود. */
    var firstName: String by androidx.compose.runtime.mutableStateOf("")
        private set

    var subscription: String by androidx.compose.runtime.mutableStateOf("free")
        private set

    var subscriptionEndMs: Long by androidx.compose.runtime.mutableStateOf(0L)
        private set

    var gender: String by androidx.compose.runtime.mutableStateOf("")
        private set

    var avatarPath: String by androidx.compose.runtime.mutableStateOf("")
        private set

    fun isPaid(raw: String = subscription): Boolean =
        com.hamyareman.ir.platform.core.common.BillingStatus.isPaid(raw, subscriptionEndMs)

    fun loadMirror(ctx: Context) {
        val store = LocalStore(ctx, STORE)
        grade = AppEdition.grade
        hasProfile = store.getString(KEY_DONE, "0") == "1"
        firstName = store.getString(KEY_NAME, "").orEmpty()
        subscription = store.getString(KEY_SUB, "free").ifBlank { "free" }
        subscriptionEndMs = store.getString(KEY_SUB_END, "0").toLongOrNull() ?: 0L
        gender = store.getString(KEY_GENDER, "").orEmpty()
        avatarPath = store.getString(KEY_AVATAR, "").orEmpty()
        applyLauncherIcon(ctx, gender)
    }

    fun writeMirror(ctx: Context, g: GradeLevel, done: Boolean) {
        writeMirror(ctx, g, done, firstName, subscription)
    }

    fun clearMirror(ctx: Context) {
        writeMirror(ctx, GradeLevel.G9, false, "", "free", "")
        saveAvatarMirror(ctx, "")
    }

    fun writeMirror(ctx: Context, g: GradeLevel, done: Boolean, name: String, sub: String, genderId: String = gender) {
        val store = LocalStore(ctx, STORE)
        val keepName = if (done && name.isBlank()) firstName.ifBlank { store.getString(KEY_NAME, "").orEmpty() } else name
        store.putString(KEY_GRADE, AppEdition.grade.id)
        store.putString(KEY_DONE, if (done) "1" else "0")
        store.putString(KEY_NAME, keepName)
        store.putString(KEY_SUB, sub.ifBlank { "free" })
        store.putString(KEY_SUB_END, subscriptionEndMs.toString())
        store.putString(KEY_GENDER, genderId)
        grade = AppEdition.grade
        hasProfile = done
        firstName = keepName
        subscription = sub.ifBlank { "free" }
        if (com.hamyareman.ir.platform.core.common.BillingStatus.isExpired(subscriptionEndMs)) {
            subscription = "free"
        }
        gender = genderId
        applyLauncherIcon(ctx, genderId)
    }

    fun applyLauncherIcon(ctx: Context, genderId: String) {
        val pm = ctx.packageManager
        val boy = android.content.ComponentName(ctx, "com.hamyareman.ir.LauncherBoy")
        val girl = android.content.ComponentName(ctx, "com.hamyareman.ir.LauncherGirl")
        val useGirl = genderId.equals("girl", ignoreCase = true)
        val enabled = android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val disabled = android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        runCatching {
            pm.setComponentEnabledSetting(boy, if (useGirl) disabled else enabled, android.content.pm.PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(girl, if (useGirl) enabled else disabled, android.content.pm.PackageManager.DONT_KILL_APP)
        }
    }

    fun loadAvatarMirror(ctx: Context) {
        avatarPath = LocalStore(ctx, STORE).getString(KEY_AVATAR, "").orEmpty()
    }

    fun saveAvatarMirror(ctx: Context, path: String) {
        LocalStore(ctx, STORE).putString(KEY_AVATAR, path)
        avatarPath = path
    }

    private const val STORE = "hamyar_profile"
    private const val KEY_GRADE = "grade"
    private const val KEY_DONE = "registered"
    private const val KEY_NAME = "firstName"
    fun applyServer(ctx: Context, p: StudentProfile) {
        subscriptionEndMs = p.subscriptionEndMs
        writeMirror(ctx, p.grade, true, p.firstName, p.subscription, p.gender)
    }

    private const val KEY_SUB = "subscription"
    private const val KEY_SUB_END = "subscription_end_ms"
    private const val KEY_AVATAR = "avatarPath"
    private const val KEY_GENDER = "gender"
}

/** فیلتر مرکزی محتوای مدرسه بر اساس پایه‌ی کاربر. */
object GradeGate {
    fun canSeeBook(bookCode: String): Boolean = gradeOfBook(bookCode) == StudentProfileState.grade
    fun <T> filter(list: List<T>, bookCodeOf: (T) -> String): List<T> = list.filter { canSeeBook(bookCodeOf(it)) }
}

/**
 * پروفایل دانش‌آموز — جدول واحد `student_profiles`؛ هر کاربر دقیقاً یک ردیف با
 * `rowId = userId` گوگل (یک یوزرآی‌دی واحد برای همه‌ی دسترسی‌ها و سینک‌های بعدی).
 */
data class StudentProfile(
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    /** تاریخ تولد شمسی `yyyy-MM-dd` لاتین؛ سن از روی همین حساب می‌شود. */
    val birthDate: String = "",
    val grade: GradeLevel,
    val phone: String, // ۱۰ رقم، بدون +98 (پیش‌شماره در UI ثابت است)
    val schoolName: String = "",
    val province: String = "",
    val city: String = "",
    val county: String = "",
    val gender: String = "", // boy | girl
    val subscription: String = "free",
    val subscriptionEndMs: Long = 0L,
)

enum class StudentGender(val id: String, val fa: String) {
    BOY("boy", "پسر"),
    GIRL("girl", "دختر");
    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id }
    }
}

object StudentProfileRepo {
    private const val TABLE = TableIds.STUDENT_PROFILES

    /** ردیف کاربر؛ نبود/خطا → null (خطا هرگز نباید فرم را بی‌دلیل نشان دهد). */
    suspend fun fetch(tables: TablesDbService, userId: String): StudentProfile? {
        if (userId.isBlank()) return null
        val r = tables.get(TABLE, userId)
        return when (r) {
            is com.hamyareman.ir.platform.core.common.AppResult.Ok -> r.value?.let { row ->
                val d = row.data
                StudentProfile(
                    userId = userId,
                    email = d["email"]?.toString().orEmpty(),
                    firstName = d["firstName"]?.toString().orEmpty(),
                    lastName = d["lastName"]?.toString().orEmpty(),
                    age = d["age"]?.toString()?.toIntOrNull() ?: 0,
                    birthDate = d["birthDate"]?.toString().orEmpty(),
                    grade = AppEdition.grade,
                    phone = d["phone"]?.toString().orEmpty(),
                    schoolName = d["schoolName"]?.toString().orEmpty(),
                    province = d["province"]?.toString().orEmpty(),
                    city = d["city"]?.toString().orEmpty(),
                    county = d["county"]?.toString().orEmpty(),
                    gender = d["gender"]?.toString().orEmpty(),
                    subscription = (d["subscription"]?.toString()?.ifBlank { null } ?: "free"),
                    subscriptionEndMs = d["subscriptionEndMs"]?.toString()?.toLongOrNull() ?: 0L,
                )
            }
            else -> null
        }
    }

    /** ذخیره (ساخت/بازنویسی) + پرمیشن فقط-خودِ-کاربر؛ true = در سرور ثبت شد. */
    suspend fun save(tables: TablesDbService, email: String, p: StudentProfile): Boolean {
        val keepSub = fetch(tables, p.userId)?.subscription?.ifBlank { null }
            ?: p.subscription.ifBlank { "free" }
        val data = mapOf(
            "userId" to p.userId,
            "email" to email,
            "firstName" to p.firstName,
            "lastName" to p.lastName,
            "age" to p.age,
            "birthDate" to p.birthDate,
            "grade" to AppEdition.grade.id,
            "phone" to p.phone,
            "schoolName" to p.schoolName,
            "province" to p.province,
            "city" to p.city,
            "county" to p.county,
            "gender" to p.gender,
            // مقدار قبلی حفظ می‌شود؛ تغییر اشتراک فقط از تابع سرور است.
            "subscription" to keepSub,
            "updatedAtMs" to System.currentTimeMillis(),
        )
        return when (tables.upsert(TABLE, p.userId, data, RowPermissions.forUser(p.userId))) {
            is com.hamyareman.ir.platform.core.common.AppResult.Ok -> true
            else -> false
        }
    }
}
