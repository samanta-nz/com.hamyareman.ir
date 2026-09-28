package com.hamyareman.ir.ui.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private fun latinDigits(s: String): String = buildString {
    for (c in s) append(
        when (c) {
            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
            '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
            else -> c
        }
    )
}

/**
 * منوی پروفایل کاربری — تمام مشخصات ثبت‌شده با امکان ویرایش:
 * نام/نام‌خانوادگی/تاریخ تولد شمسی (سن خودکار)/ایمیل/موبایل + مدرسه/استان + عکس پروفایل (محلی).
 * پایه: متن ثابت این نسخهٔ اپ؛ غیرقابل تغییر.
 * اشتراک (رایگان/یک‌ساله): فقط نمایش — تغییر از سمت پشتیبانی/سرور.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    profile: StudentProfile?,
    onBack: () -> Unit,
    onSave: suspend (StudentProfile) -> Boolean,
    onLogout: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var saveError by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val app = com.hamyareman.ir.LocalAppContainer.current
    LaunchedEffect(Unit) {
        val uid = app.auth.cachedUserId().orEmpty()
        if (uid.isNotBlank()) {
            val remote = StudentProfileRepo.fetch(app.tables, uid)
            if (remote != null) StudentProfileState.applyServer(ctx, remote)
        }
    }
    val storage = com.hamyareman.ir.LocalAppContainer.current.storage
    var avatarPath by remember { mutableStateOf(com.hamyareman.ir.ui.profile.StudentProfileState.avatarPath) }
    var cropBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            cropBitmap = loadOrientedBitmap(ctx, uri)
        }
    }

    var firstName by remember { mutableStateOf(profile?.firstName.orEmpty()) }
    var lastName by remember { mutableStateOf(profile?.lastName.orEmpty()) }
    val initialBirth = remember { JalaliDate.parseJalali(profile?.birthDate.orEmpty()) }
    var birthYear by remember { mutableStateOf(initialBirth?.year) }
    var birthMonth by remember { mutableStateOf(initialBirth?.month) }
    var birthDay by remember { mutableStateOf(initialBirth?.day) }
    val birthJalali = remember(birthYear, birthMonth, birthDay) {
        val y = birthYear; val m = birthMonth; val d = birthDay
        if (y != null && m != null && d != null) JalaliDate.Jalali(y, m, d).takeIf { JalaliDate.isValid(it) } else null
    }
    val computedAge = remember(birthJalali) { birthJalali?.let { JalaliDate.ageYears(it) } }
    var email by remember { mutableStateOf(profile?.email.orEmpty()) }
    var phone by remember { mutableStateOf(profile?.phone.orEmpty()) }
    var schoolName by remember { mutableStateOf(profile?.schoolName.orEmpty()) }
    var province by remember { mutableStateOf(profile?.province.orEmpty()) }
    var county by remember { mutableStateOf(profile?.county.orEmpty()) }
    var city by remember { mutableStateOf(profile?.city.orEmpty()) }
    var gender by remember { mutableStateOf(profile?.gender.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val genderChanged = gender.isNotBlank() && gender != profile?.gender.orEmpty()
    var askRestart by remember { mutableStateOf(false) }
    var pendingRestart by remember { mutableStateOf(false) }
    LaunchedEffect(saving) {
        if (pendingRestart && !saving) {
            pendingRestart = false
            if (saveError.isNullOrBlank()) {
                delay(700)
                restartHamyar(ctx)
            }
        }
    }

    fun doSave(withRestart: Boolean) {
        val ageNow = computedAge
        val birth = birthJalali
        if (ageNow == null || birth == null) return
        StudentProfileState.applyLauncherIcon(ctx, gender)
        if (withRestart) pendingRestart = true
        saving = true
        saveError = null
        val payload = (profile ?: StudentProfile(
            userId = "", email = email, firstName = "", lastName = "",
            age = 0, grade = AppEdition.grade, phone = "",
        )).copy(
            firstName = firstName.trim(), lastName = lastName.trim(),
            age = ageNow, birthDate = birth.isoLike, email = email.trim(), phone = phone,
            schoolName = schoolName.trim(), province = province, county = county, city = city,
            gender = gender,
            grade = AppEdition.grade,
        )
        scope.launch {
            val ok = runCatching { onSave(payload) }.getOrDefault(false)
            saving = false
            saveError = if (ok) null else "ذخیره نشد؛ اینترنت را چک کن و دوباره بزن."
        }
    }

    val bad = showErrors && (
        firstName.trim().length < 2 || lastName.trim().length < 2 ||
            (computedAge == null || computedAge !in 5..60) ||
            phone.isNotBlank() && !Regex("^9\\d{9}$").matches(phone) ||
            gender.isBlank()
        )

    Column(Modifier.fillMaxSize()) {
        AppTopBar("پروفایل من", onBack = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ─── عکس پروفایل (محلی — انتخاب از گالری) ───
            Box(Modifier.size(96.dp)) {
                val bmp = remember(avatarPath) {
                    avatarPath.takeIf { it.isNotBlank() }?.let { p ->
                        runCatching { BitmapFactory.decodeFile(p) }.getOrNull()
                    }
                }
                if (bmp != null) {
                    Image(
                        bmp.asImageBitmap(), contentDescription = "عکس پروفایل",
                        modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop,
                    )
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(profile?.firstName?.take(1).orEmpty().ifBlank { "؟" }, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
            TextButton(onClick = { pickImage.launch("image/*") }) { Text("انتخاب عکس — زوم و برش دایره‌ای") }

            OutlinedTextField(
                value = firstName, onValueChange = { firstName = it },
                label = { Text("نام *") }, singleLine = true, isError = bad,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lastName, onValueChange = { lastName = it },
                label = { Text("نام خانوادگی *") }, singleLine = true, isError = bad,
                modifier = Modifier.fillMaxWidth(),
            )
            JalaliBirthDateFields(
                year = birthYear,
                month = birthMonth,
                day = birthDay,
                onChange = { y, m, d -> birthYear = y; birthMonth = m; birthDay = d },
                isError = showErrors && (computedAge == null || computedAge !in 5..60),
            )
            OutlinedTextField(
                value = computedAge?.let { toPersianDigits(it.toString()) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("سن") },
                isError = showErrors && (computedAge == null || computedAge !in 5..60),
                supportingText = { Text("از تاریخ تولد شمسی حساب می‌شود") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email, onValueChange = {},
                readOnly = true,
                label = { Text("ایمیل") }, singleLine = true,
                supportingText = { Text("تغییر ایمیل فعلاً از همین‌جا ممکن نیست") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            CredentialsSection()
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = latinDigits(it).filter { c -> c.isDigit() }.take(10) },
                label = { Text("شماره همراه") },
                supportingText = { Text(if (phone.isBlank()) "اختیاری — ۱۰ رقم با ۹" else if (Regex("^9\\d{9}$").matches(phone)) "✓" else "۱۰ رقم با ۹") },
                singleLine = true, isError = bad,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                prefix = { Text("+98", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
            )
            }

            Text("جنسیت *", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudentGender.entries.forEach { g ->
                    FilterChip(
                        selected = gender == g.id,
                        onClick = { gender = g.id },
                        label = { Text(g.fa) },
                    )
                }
            }
            if (showErrors && gender.isBlank()) {
                Text("پسر یا دختر را انتخاب کن", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = schoolName, onValueChange = { schoolName = it },
                label = { Text("نام مدرسه") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            IranLocationFields(
                province = province, county = county, city = city,
                onProvince = { province = it }, onCounty = { county = it }, onCity = { city = it },
            )

            OutlinedTextField(
                value = AppEdition.gradeFa,
                onValueChange = {}, readOnly = true,
                label = { Text("پایه تحصیلی") },
                modifier = Modifier.fillMaxWidth(),
            )

            // ─── وضعیت اشتراک (فقط نمایش — تغییر از سمت پشتیبانی) ───
            val sub = StudentProfileState.subscription.ifBlank { "free" }
            val paid = StudentProfileState.isPaid(sub)
            val pending = com.hamyareman.ir.platform.core.common.BillingStatus.norm(sub) ==
                com.hamyareman.ir.platform.core.common.BillingStatus.PENDING
            val chipColor = when {
                pending -> androidx.compose.ui.graphics.Color(0xFFFEF3C7)
                paid -> androidx.compose.ui.graphics.Color(0xFFDCFCE7)
                else -> androidx.compose.ui.graphics.Color(0xFFFEE2E2)
            }
            val chipFg = when {
                pending -> androidx.compose.ui.graphics.Color(0xFF92400E)
                paid -> androidx.compose.ui.graphics.Color(0xFF166534)
                else -> androidx.compose.ui.graphics.Color(0xFFB91C1C)
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = chipColor,
                border = BorderStroke(1.dp, chipFg),
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSubscription,
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        com.hamyareman.ir.platform.core.common.BillingStatus.chipFa(sub, StudentProfileState.subscriptionEndMs) +
                            com.hamyareman.ir.platform.core.common.BillingStatus.rangeFa(0L, StudentProfileState.subscriptionEndMs).let { if (it.isBlank()) "" else " · $it" },
                        fontWeight = FontWeight.Bold,
                        color = chipFg,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "جزئیات اشتراک ›",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (!saveError.isNullOrBlank()) {
                Text(saveError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    showErrors = true
                    val phoneOk = phone.isBlank() || Regex("^9\\d{9}$").matches(phone)
                    val age = computedAge
                    if (firstName.trim().length >= 2 && lastName.trim().length >= 2 &&
                        age != null && age in 5..60 && birthJalali != null && phoneOk && gender.isNotBlank()
                    ) {
                        if (genderChanged) askRestart = true else doSave(false)
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("ذخیره‌ی تغییرات", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("خروج از حساب") }
            Spacer(Modifier.height(14.dp))
        }
    }
    cropBitmap?.let { bmp ->
        AvatarCircleCropDialog(
            bitmap = bmp,
            onCancel = { cropBitmap = null },
            onCropped = { out ->
                runCatching {
                    val f = File(ctx.filesDir, "avatar.jpg")
                    f.outputStream().use { os -> out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, os) }
                    StudentProfileState.saveAvatarMirror(ctx, f.absolutePath)
                    avatarPath = f.absolutePath
                    scope.launch {
                        val uid = profile?.userId.orEmpty()
                        if (uid.isNotBlank()) {
                            runCatching { AvatarSync.push(ctx, storage, uid) }
                        }
                    }
                }
                cropBitmap = null
            },
        )
    }
    if (askRestart) {
        AlertDialog(
            onDismissRequest = { askRestart = false },
            title = { Text("راه‌اندازی دوباره‌ی برنامه") },
            text = {
                Text(
                    "جنسیت عوض شده و آیکون برنامه باید تازه شود. برای دیدن آیکون جدید، " +
                        "برنامه یک‌بار بسته و دوباره باز می‌شود. ادامه می‌دهی؟",
                )
            },
            confirmButton = {
                TextButton(onClick = { askRestart = false; doSave(true) }) { Text("تأیید و ادامه") }
            },
            dismissButton = {
                TextButton(onClick = { askRestart = false; doSave(false) }) { Text("فعلاً نه") }
            },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("خروج از حساب؟") },
            text = { Text("نشست بسته می‌شود و «مرا به خاطر بسپار» هم پاک می‌شود؛ برای ورود دوباره نام کاربری یا ایمیل و رمز (یا گوگل) لازم است.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("خروج") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("انصراف") }
            },
        )
    }
}

/**
 * «نام کاربری و رمزِ ورود» (v1.65) — همان چیزی که در فرم ثبت‌نام گرفته می‌شود و
 * این‌جا بدونِ تأییدِ رمزِ قبلی قابلِ ویرایش است:
 *  - نام کاربری یکتا ⇒ نگاشتِ `نام کاربری → ایمیل` در جدولِ `users` سرور.
 *  - رمزِ تازه: برای حسابِ گوگلی بدونِ رمزِ قبلی؛ اگر سرور رمزِ فعلی خواست، همان‌جا
 *    فیلدِ «رمز فعلی» باز می‌شود.
 */
@Composable
private fun CredentialsSection() {
    val auth = com.hamyareman.ir.LocalAppContainer.current.auth
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf(auth.cachedUsername().orEmpty()) }
    var password by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var askCurrent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        username = runCatching { auth.currentUsername() }.getOrNull().orEmpty()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("نام کاربری و رمزِ ورود", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "با همین نام کاربری (یا ایمیل) و رمز می‌توانی دوباره وارد شوی؛ اگر «مرا به خاطر بسپار» روشن باشد، دفعهٔ بعد بدونِ اینترنت هم اپ باز می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim().lowercase().replace(' ', '_') },
                    label = { Text("نام کاربری یکتا") },
                    singleLine = true,
                    supportingText = { Text("a-z، ۰-۹ و _ — بین ۳ تا ۲۴ کاراکتر") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("رمز تازه (اختیاری)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (askCurrent) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("رمز فعلی (برای تغییرِ رمز لازم است)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Button(
                enabled = !busy,
                onClick = {
                    busy = true; err = null; notice = null
                    scope.launch {
                        val r = auth.saveUsername(username, password.ifBlank { null }, currentPassword.ifBlank { null })
                        when (r) {
                            is com.hamyareman.ir.platform.core.common.AppResult.Ok -> {
                                notice = "ذخیره شد ✅ نام کاربری: ${r.value}"
                                password = ""; currentPassword = ""; askCurrent = false
                            }
                            is com.hamyareman.ir.platform.core.common.AppResult.Err -> {
                                err = r.error.userMessage
                                askCurrent = err?.contains("رمز فعلی") == true ||
                                    err?.contains("فراموشی رمز") == true
                            }
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "در حال ذخیره…" else "ذخیرهٔ نام کاربری و رمز") }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            err?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}
