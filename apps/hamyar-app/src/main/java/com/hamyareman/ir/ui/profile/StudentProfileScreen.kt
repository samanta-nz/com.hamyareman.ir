package com.hamyareman.ir.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import kotlinx.coroutines.delay

/** فقط ارقام لاتین — کیبورد فارسی هم ممکن است ۰-۹ بدهد؛ همه را لاتین می‌کنیم. */
private fun latinDigits(s: String): String = buildString {
    for (c in s) append(
        when (c) {
            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
            '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
            else -> c
        },
    )
}

/**
 * فرم ثبت‌نام دانش‌آموز — بلافاصله پس از اولین ورود موفق اگر پروفایل نباشد.
 * قواعد اجباری: نام و نام‌خانوادگی، تاریخ تولد شمسی (سن خودکار)، جنسیت، موبایل. پایهٔ این اپ ثابت است.
 * استان→شهر اختیاری با پیش‌فرض خالی.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    email: String,
    saving: Boolean,
    error: String?,
    /** نام کاربریِ فعلیِ حساب (خالی = هنوز تعیین نشده و در این فرم گرفته می‌شود). */
    currentUsername: String = "",
    onSubmit: (
        firstName: String, lastName: String, age: Int, birthDate: String, grade: GradeLevel, phone: String,
        gender: String, province: String, county: String, city: String,
        username: String, password: String,
    ) -> Unit,
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf<Int?>(null) }
    var birthMonth by remember { mutableStateOf<Int?>(null) }
    var birthDay by remember { mutableStateOf<Int?>(null) }
    val birthJalali = remember(birthYear, birthMonth, birthDay) {
        val y = birthYear; val m = birthMonth; val d = birthDay
        if (y != null && m != null && d != null) JalaliDate.Jalali(y, m, d).takeIf { JalaliDate.isValid(it) } else null
    }
    val computedAge = remember(birthJalali) { birthJalali?.let { JalaliDate.ageYears(it) } }
    val grade = AppEdition.grade
    var phone by remember { mutableStateOf("") }
    // v1.65 — نام کاربری + رمز: برای حسابِ گوگلی این‌جا ساخته می‌شود تا دفعهٔ بعد
    // بدونِ گوگل (و حتی بدونِ اینترنت) وارد اپ شود.
    var username by remember { mutableStateOf(currentUsername) }
    var accountPassword by remember { mutableStateOf("") }
    val needCredentials = currentUsername.isBlank()
    var gender by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var county by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    // آیکون لانچر بر اساس جنسیت عوض می‌شود؛ اعمالِ کاملش یک راه‌اندازیِ دوباره می‌خواهد.
    var askRestart by remember { mutableStateOf(false) }
    var pendingRestart by remember { mutableStateOf(false) }
    LaunchedEffect(saving) {
        if (pendingRestart && !saving) {
            pendingRestart = false
            if (error.isNullOrBlank()) {
                delay(700)
                restartHamyar(ctx)
            }
        }
    }
    var showErrors by remember { mutableStateOf(false) }

    val firstNameBad = showErrors && firstName.trim().length < 2
    val lastNameBad = showErrors && lastName.trim().length < 2
    val ageBad = showErrors && (computedAge == null || computedAge !in 5..60)
    val phoneBad = showErrors && !Regex("^9\\d{9}$").matches(phone)
    val usernameOk = username.trim().lowercase().matches(Regex("^[a-z0-9_]{3,24}$"))
    val usernameBad = showErrors && needCredentials && !usernameOk
    val passwordBad = showErrors && needCredentials && accountPassword.length < 8

    Column(Modifier.fillMaxSize()) {
        AppTopBar("ثبت‌نام دانش‌آموز", onBack = {})

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "خوش آمدی! برای همگام‌سازی پیشرفتت، این فرم را یک‌بار کامل کن.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = email,
                onValueChange = {},
                label = { Text("ایمیل حساب") },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim().lowercase().replace(' ', '_') },
                    label = { Text(if (needCredentials) "نام کاربری یکتا *" else "نام کاربری") },
                    isError = usernameBad,
                    singleLine = true,
                    supportingText = {
                        Text(
                            when {
                                usernameBad -> "۳ تا ۲۴ کاراکتر؛ فقط a-z، ۰-۹ و _"
                                else -> "با همین نام (یا ایمیل) و رمز، دوباره وارد می‌شوی"
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = accountPassword,
                    onValueChange = { accountPassword = it },
                    label = { Text(if (needCredentials) "رمز عبور *" else "رمز تازه (اختیاری)") },
                    isError = passwordBad,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = {
                        Text(
                            if (passwordBad) "حداقل ۸ کاراکتر"
                            else "برای ورودِ بدونِ گوگل لازم است؛ هیچ‌جا نمایش داده نمی‌شود",
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("نام *") },
                isError = firstNameBad,
                supportingText = { if (firstNameBad) Text("نام را فارسی و کامل بنویس") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("نام خانوادگی *") },
                isError = lastNameBad,
                supportingText = { if (lastNameBad) Text("نام خانوادگی را کامل بنویس") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            JalaliBirthDateFields(
                year = birthYear,
                month = birthMonth,
                day = birthDay,
                onChange = { y, m, d -> birthYear = y; birthMonth = m; birthDay = d },
                isError = ageBad,
            )
            OutlinedTextField(
                value = computedAge?.let { toPersianDigits(it.toString()) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text("سن") },
                isError = ageBad,
                supportingText = {
                    Text(if (ageBad) "سن باید بین ۵ تا ۶۰ باشد" else "از تاریخ تولد شمسی حساب می‌شود")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("جنسیت *", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudentGender.entries.forEach { g ->
                    FilterChip(
                        selected = gender == g.id,
                        onClick = { gender = g.id },
                        label = { Text(g.fa) },
                    )
                }
            }
            if (showErrors && gender.isBlank()) {
                Text("پسر یا دختر را انتخاب کن", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = AppEdition.gradeFa,
                onValueChange = {},
                readOnly = true,
                label = { Text("پایه تحصیلی") },
                modifier = Modifier.fillMaxWidth(),
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = latinDigits(it).filter { c -> c.isDigit() }.take(10) },
                    label = { Text("شماره همراه *") },
                    isError = phoneBad,
                    supportingText = {
                        Text(if (phoneBad) "دقیقاً ۱۰ رقم — با ۹ شروع شود" else "۱۰ رقم؛ ۹۸ به‌صورت خودکار اولش هست")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    prefix = { Text("+98", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IranLocationFields(
                province = province,
                county = county,
                city = city,
                onProvince = { province = it },
                onCounty = { county = it },
                onCity = { city = it },
            )

            if (!error.isNullOrBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    showErrors = true
                    val age = computedAge
                    val ok = firstName.trim().length >= 2 && lastName.trim().length >= 2 &&
                        age != null && age in 5..60 && birthJalali != null &&
                        Regex("^9\\d{9}$").matches(phone) && gender.isNotBlank() &&
                        (!needCredentials || (usernameOk && accountPassword.length >= 8))
                    if (ok && !saving && age != null && birthJalali != null) askRestart = true
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("ثبت و ورود به همیار", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (askRestart) {
        AlertDialog(
            onDismissRequest = { askRestart = false },
            title = { Text("راه‌اندازی دوباره‌ی برنامه") },
            text = {
                Text(
                    "آیکون برنامه بر اساس جنسیت تو عوض می‌شود. برای اینکه آیکون تازه روی صفحه‌ی " +
                        "گوشی دیده شود، برنامه باید یک‌بار بسته و دوباره باز شود. ادامه می‌دهی؟",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askRestart = false
                    StudentProfileState.applyLauncherIcon(ctx, gender)
                    pendingRestart = true
                    val age = computedAge
                    if (age != null && birthJalali != null) {
                        onSubmit(
                            firstName.trim(), lastName.trim(), age, birthJalali.isoLike, grade, phone,
                            gender, province, county, city,
                            username.trim().lowercase(), accountPassword,
                        )
                    }
                }) { Text("تأیید و ادامه", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    askRestart = false
                    StudentProfileState.applyLauncherIcon(ctx, gender)
                    val age = computedAge
                    if (age != null && birthJalali != null) {
                        onSubmit(
                            firstName.trim(), lastName.trim(), age, birthJalali.isoLike, grade, phone,
                            gender, province, county, city,
                            username.trim().lowercase(), accountPassword,
                        )
                    }
                }) { Text("فعلاً نه") }
            },
        )
    }
}
