package com.hamyareman.ir.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private enum class LoginMode { SIGN_IN, SIGN_UP, RECOVER, OTP }

/**
 * دروازه‌ی ورود — تا کاربر وارد نشود، هیچ محتوایی رندر نمی‌شود.
 *
 * v1.65:
 *  - ورود با **نام کاربری یا ایمیل + رمز** (یک فیلد مشترک).
 *  - «مرا به خاطر بسپار» **پیش‌فرض روشن** ⇒ از بارِ دوم به بعد، اپ **بدونِ اینترنت**
 *    بالا می‌آید (بارِ اول برای ورود اینترنت لازم است).
 *  - ثبت‌نام: نام + ایمیل + **نام کاربری یکتا** + رمز (یکتایی سمتِ سرور چک می‌شود).
 *  - «فراموشی رمز عبور»: ایمیلِ بازیابی + جای‌گذاریِ لینکِ همان ایمیل در اپ.
 *  - «کد یک‌بارمصرف ایمیل»: ورودِ بدونِ رمز (برای حساب‌های گوگلی هم کار می‌کند).
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    notice: String?,
    onGoogle: () -> Unit,
    onSignIn: (identifier: String, password: String, remember: Boolean) -> Unit,
    onSignUp: (name: String, email: String, username: String, password: String, remember: Boolean) -> Unit,
    onRecover: (identifier: String) -> Unit,
    onRecoverComplete: (link: String, newPassword: String) -> Unit,
    onSendOtp: (identifier: String) -> Unit,
    onSignInOtp: (otp: String, remember: Boolean) -> Unit,
) {
    var mode by remember { mutableStateOf(LoginMode.SIGN_IN) }
    var identifier by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var recoveryLink by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    // مرحلهٔ دومِ بازیابی/OTP فقط بعد از فرستادنِ موفق ایمیل باز می‌شود.
    var codeSent by remember { mutableStateOf(false) }
    var linkSent by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    // خطای تازه ⇒ مرحلهٔ دومِ بازیابی/OTP بسته می‌شود (ایمیل نرفته).
    androidx.compose.runtime.LaunchedEffect(error, mode) {
        if (error != null) {
            linkSent = false
            codeSent = false
        }
    }

    fun resetErrors() {
        localError = null
    }

    fun submitSignIn() {
        resetErrors()
        val id = identifier.trim()
        if (id.length < 3) {
            localError = "نام کاربری یا ایمیل را بنویس."
            return
        }
        if (password.length < 8) {
            localError = "رمز باید حداقل ۸ کاراکتر باشد."
            return
        }
        onSignIn(id, password, rememberMe)
    }

    fun submitSignUp() {
        resetErrors()
        val em = email.trim()
        if (name.trim().length < 2) {
            localError = "نام را کامل بنویس."
            return
        }
        if (!em.contains("@") || em.length < 5) {
            localError = "یک ایمیل معتبر بنویس."
            return
        }
        val u = username.trim().lowercase()
        if (u.length < 3 || u.length > 24 || !u.matches(Regex("^[a-z0-9_]+$"))) {
            localError = "نام کاربری: ۳ تا ۲۴ کاراکتر، فقط حروف انگلیسی کوچک، عدد و _"
            return
        }
        if (password.length < 8) {
            localError = "رمز باید حداقل ۸ کاراکتر باشد."
            return
        }
        onSignUp(name.trim(), em, u, password, rememberMe)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(com.hamyareman.ir.ui.profile.AppEdition.appTitle + " 💜", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            when (mode) {
                LoginMode.SIGN_IN -> "با نام کاربری یا ایمیل وارد شو. اگر «مرا به خاطر بسپار» روشن باشد، دفعهٔ بعد اپ بدونِ اینترنت هم باز می‌شود."
                LoginMode.SIGN_UP -> "یک‌بار حساب بساز: نام، ایمیل، نام کاربری یکتا و رمز. نام کاربری در سرور چک می‌شود."
                LoginMode.RECOVER -> "رمزت را فراموش کرده‌ای؟ لینک بازیابی به ایمیلت می‌فرستیم؛ لینک را در اپ جای‌گذاری کن و رمز تازه بگذار."
                LoginMode.OTP -> "یک کد ۶ رقمی به ایمیلت می‌فرستیم؛ بدونِ رمز وارد می‌شوی."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    when (mode) {
                        LoginMode.SIGN_IN -> {
                            IdentifierField(identifier) { identifier = it }
                            Spacer(Modifier.height(8.dp))
                            PasswordField(password, "رمز عبور") { password = it }
                            RememberRow(rememberMe) { rememberMe = it }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { submitSignIn() }, modifier = Modifier.fillMaxWidth()) {
                                Text("ورود")
                            }
                            TextButton(onClick = { resetErrors(); mode = LoginMode.RECOVER; linkSent = false }) {
                                Text("فراموشی رمز عبور")
                            }
                            TextButton(onClick = { resetErrors(); mode = LoginMode.OTP; codeSent = false }) {
                                Text("ورود با کد یک‌بارمصرف ایمیل")
                            }
                            TextButton(onClick = { resetErrors(); mode = LoginMode.SIGN_UP }) {
                                Text("حساب نداری؟ ثبت‌نام")
                            }
                        }

                        LoginMode.SIGN_UP -> {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("نام و نام خانوادگی") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it.trim() },
                                    label = { Text("ایمیل") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it.trim().lowercase().replace(' ', '_') },
                                    label = { Text("نام کاربری یکتا") },
                                    singleLine = true,
                                    supportingText = { Text("a-z، ۰-۹ و _ — با این نام وارد می‌شوی") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            PasswordField(password, "رمز عبور (حداقل ۸ کاراکتر)") { password = it }
                            RememberRow(rememberMe) { rememberMe = it }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { submitSignUp() }, modifier = Modifier.fillMaxWidth()) {
                                Text("ساخت حساب و ورود")
                            }
                            TextButton(onClick = { resetErrors(); mode = LoginMode.SIGN_IN }) {
                                Text("حساب داری؟ ورود")
                            }
                        }

                        LoginMode.RECOVER -> {
                            IdentifierField(identifier) { identifier = it }
                            if (!linkSent) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        resetErrors()
                                        if (identifier.trim().length < 3) {
                                            localError = "نام کاربری یا ایمیلت را بنویس."
                                        } else {
                                            linkSent = true
                                            onRecover(identifier.trim())
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("فرستادن ایمیل بازیابی") }
                            } else {
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = recoveryLink,
                                    onValueChange = { recoveryLink = it },
                                    label = { Text("لینکِ داخل ایمیل را این‌جا بچسبان") },
                                    supportingText = {
                                        Text("اگر لینک صفحهٔ خالی باز کرد، نشانی را از نوارِ مرورگر کپی کن.")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                PasswordField(newPassword, "رمز تازه (حداقل ۸ کاراکتر)") { newPassword = it }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        resetErrors()
                                        onRecoverComplete(recoveryLink.trim(), newPassword)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("تنظیم رمز تازه") }
                            }
                            TextButton(onClick = { resetErrors(); mode = LoginMode.SIGN_IN }) {
                                Text("بازگشت به ورود")
                            }
                        }

                        LoginMode.OTP -> {
                            IdentifierField(identifier) { identifier = it }
                            if (codeSent) {
                                Spacer(Modifier.height(8.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    OutlinedTextField(
                                        value = otp,
                                        onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                                        label = { Text("کد ۶ رقمی") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            RememberRow(rememberMe) { rememberMe = it }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    resetErrors()
                                    if (!codeSent) {
                                        if (identifier.trim().length < 3) {
                                            localError = "نام کاربری یا ایمیلت را بنویس."
                                        } else {
                                            codeSent = true
                                            onSendOtp(identifier.trim())
                                        }
                                    } else if (otp.trim().length != 6) {
                                        localError = "کد ۶ رقمی را کامل بنویس."
                                    } else {
                                        onSignInOtp(otp.trim(), rememberMe)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(if (codeSent) "ورود با کد" else "فرستادن کد") }
                            if (codeSent) {
                                TextButton(onClick = { resetErrors(); codeSent = false }) {
                                    Text("فرستادن دوبارهٔ کد")
                                }
                            }
                            TextButton(onClick = { resetErrors(); mode = LoginMode.SIGN_IN }) {
                                Text("بازگشت به ورود")
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = onGoogle, modifier = Modifier.fillMaxWidth()) {
                        Text("ورود با گوگل")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "بعد از ورود با گوگل، در فرم ثبت‌نام یک نام کاربری و رمز هم برایت ساخته می‌شود تا دفعهٔ بعد بدونِ گوگل وارد شوی.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                notice?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                val shown = localError ?: error
                shown?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "نسخه‌ی " + com.hamyareman.ir.BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "🔒 چرخه، ژورنال و چتِ خام هرگز به سرور نمی‌رود؛ لاگین فقط هویت تو را تأیید می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IdentifierField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.trim()) },
        label = { Text("نام کاربری یا ایمیل") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PasswordField(value: String, label: String, onChange: (String) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RememberRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(
            "مرا به خاطر بسپار (ورودِ بدونِ اینترنت از دفعهٔ بعد)",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
