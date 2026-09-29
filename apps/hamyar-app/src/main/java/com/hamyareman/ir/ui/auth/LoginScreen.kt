package com.hamyareman.ir.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R

private enum class AuthPage { SIGN_IN, SIGN_UP, FORGOT, RESET, VERIFY }

private val VazirmatnFamily = FontFamily(
    Font(R.font.vazirmatn_thin, FontWeight.Thin),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private fun vazirmatnTypography(base: Typography): Typography = Typography(
    displayLarge = base.displayLarge.withVazir(), displayMedium = base.displayMedium.withVazir(),
    displaySmall = base.displaySmall.withVazir(), headlineLarge = base.headlineLarge.withVazir(),
    headlineMedium = base.headlineMedium.withVazir(), headlineSmall = base.headlineSmall.withVazir(),
    titleLarge = base.titleLarge.withVazir(), titleMedium = base.titleMedium.withVazir(),
    titleSmall = base.titleSmall.withVazir(), bodyLarge = base.bodyLarge.withVazir(),
    bodyMedium = base.bodyMedium.withVazir(), bodySmall = base.bodySmall.withVazir(),
    labelLarge = base.labelLarge.withVazir(), labelMedium = base.labelMedium.withVazir(),
    labelSmall = base.labelSmall.withVazir(),
)

private fun TextStyle.withVazir(): TextStyle = copy(fontFamily = VazirmatnFamily)

/**
 * دروازهٔ احراز هویت نسخهٔ ۲٫۰: یک فرم متمرکز و کم‌ازدحام، با مسیرهای مستقل
 * ثبت‌نام، فراموشی/تغییر رمز و تأیید ایمیل. تمام متن‌ها از فونت داخلی وزیرمتن
 * استفاده می‌کنند و تنها فیلدهای فنی (ایمیل/رمز/لینک) LTR هستند.
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    notice: String?,
    verificationRequired: Boolean = false,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (name: String, email: String, password: String) -> Unit,
    onRecover: (email: String) -> Unit,
    onRecoverComplete: (link: String, password: String) -> Unit,
    onVerificationResend: () -> Unit,
    onVerificationComplete: (link: String) -> Unit,
    onVerificationContinue: () -> Unit,
    onGoogle: () -> Unit,
) {
    var page by remember { mutableStateOf(AuthPage.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    LaunchedEffect(verificationRequired) {
        if (verificationRequired) {
            page = AuthPage.VERIFY
            password = ""
            confirm = ""
            link = ""
        }
    }

    MaterialTheme(typography = vazirmatnTypography(MaterialTheme.typography)) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "آیکون همیار من نهم",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp)),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "همیار من نهم",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "فضای امن یادگیری و برنامه‌ریزی",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            AuthHeader(page) { page = AuthPage.SIGN_IN }

                            when (page) {
                                AuthPage.SIGN_IN -> SignInForm(
                                    email = email,
                                    password = password,
                                    loading = loading,
                                    onEmail = { email = it },
                                    onPassword = { password = it },
                                    onGoogle = onGoogle,
                                    onSubmit = { onSignIn(email.trim(), password) },
                                    onForgot = { page = AuthPage.FORGOT },
                                    onSignUp = { page = AuthPage.SIGN_UP },
                                )

                                AuthPage.SIGN_UP -> SignUpForm(
                                    name = name,
                                    email = email,
                                    password = password,
                                    confirm = confirm,
                                    loading = loading,
                                    onName = { name = it },
                                    onEmail = { email = it },
                                    onPassword = { password = it },
                                    onConfirm = { confirm = it },
                                    onSubmit = { onSignUp(name.trim(), email.trim(), password) },
                                    onVerify = { page = AuthPage.VERIFY },
                                )

                                AuthPage.FORGOT -> ForgotForm(
                                    email = email,
                                    loading = loading,
                                    onEmail = { email = it },
                                    onSubmit = { onRecover(email.trim()) },
                                    onReset = { page = AuthPage.RESET },
                                )

                                AuthPage.RESET -> ResetForm(
                                    link = link,
                                    password = password,
                                    confirm = confirm,
                                    loading = loading,
                                    onLink = { link = it },
                                    onPassword = { password = it },
                                    onConfirm = { confirm = it },
                                    onSubmit = { onRecoverComplete(link.trim(), password) },
                                )

                                AuthPage.VERIFY -> VerifyForm(
                                    link = link,
                                    loading = loading,
                                    onLink = { link = it },
                                    onComplete = { onVerificationComplete(link.trim()) },
                                    onResend = onVerificationResend,
                                    onContinue = onVerificationContinue,
                                )
                            }

                            if (!error.isNullOrBlank()) StatusMessage(error, isError = true)
                            if (!notice.isNullOrBlank()) StatusMessage(notice, isError = false)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "با ادامه، قوانین استفاده و حریم خصوصی برنامه را می‌پذیرید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthHeader(page: AuthPage, onBack: () -> Unit) {
    val title = when (page) {
        AuthPage.SIGN_IN -> "ورود به حساب"
        AuthPage.SIGN_UP -> "ساخت حساب"
        AuthPage.FORGOT -> "فراموشی رمز"
        AuthPage.RESET -> "تعیین رمز تازه"
        AuthPage.VERIFY -> "تأیید ایمیل"
    }
    val subtitle = when (page) {
        AuthPage.SIGN_IN -> "برای ادامه یکی از روش‌های امن را انتخاب کنید."
        AuthPage.SIGN_UP -> "اطلاعات اصلی حساب را وارد کنید."
        AuthPage.FORGOT -> "لینک بازیابی به ایمیل شما فرستاده می‌شود."
        AuthPage.RESET -> "لینک ایمیل و رمز تازه را وارد کنید."
        AuthPage.VERIFY -> "لینک دریافت‌شده را اینجا تأیید کنید."
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (page != AuthPage.SIGN_IN) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "بازگشت")
            }
        }
    }
}

@Composable
private fun SignInForm(
    email: String,
    password: String,
    loading: Boolean,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onGoogle: () -> Unit,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
    onSignUp: () -> Unit,
) {
    OutlinedButton(
        onClick = onGoogle,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google_g),
            contentDescription = "Google",
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text("ادامه با Google", fontWeight = FontWeight.Bold)
    }
    OrDivider()
    EmailField(email, onEmail, ImeAction.Next)
    PasswordField(password, onPassword, "رمز عبور", ImeAction.Done, onSubmit)
    PrimaryAction("ورود", loading, email.isNotBlank() && password.isNotBlank(), onSubmit)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onForgot, enabled = !loading) { Text("رمز را فراموش کردم") }
        TextButton(onClick = onSignUp, enabled = !loading) { Text("ساخت حساب") }
    }
}

@Composable
private fun SignUpForm(
    name: String,
    email: String,
    password: String,
    confirm: String,
    loading: Boolean,
    onName: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit: () -> Unit,
    onVerify: () -> Unit,
) {
    OutlinedTextField(
        value = name, onValueChange = onName, modifier = Modifier.fillMaxWidth(),
        label = { Text("نام و نام خانوادگی") }, singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Person, null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        shape = RoundedCornerShape(14.dp),
    )
    EmailField(email, onEmail, ImeAction.Next)
    PasswordField(password, onPassword, "رمز عبور (حداقل ۸ کاراکتر)", ImeAction.Next)
    PasswordField(confirm, onConfirm, "تکرار رمز", ImeAction.Done, onSubmit)
    val valid = name.isNotBlank() && email.contains('@') && password.length >= 8 && password == confirm
    PrimaryAction("ساخت حساب", loading, valid, onSubmit)
    TextButton(onClick = onVerify, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        Text("لینک تأیید ایمیل را دارم")
    }
}

@Composable
private fun ForgotForm(
    email: String,
    loading: Boolean,
    onEmail: (String) -> Unit,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
) {
    EmailField(email, onEmail, ImeAction.Done, onSubmit)
    PrimaryAction("ارسال لینک بازیابی", loading, email.contains('@'), onSubmit)
    TextButton(onClick = onReset, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        Text("لینک بازیابی را دریافت کرده‌ام")
    }
}

@Composable
private fun ResetForm(
    link: String,
    password: String,
    confirm: String,
    loading: Boolean,
    onLink: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    LinkField(link, onLink)
    PasswordField(password, onPassword, "رمز تازه (حداقل ۸ کاراکتر)", ImeAction.Next)
    PasswordField(confirm, onConfirm, "تکرار رمز تازه", ImeAction.Done, onSubmit)
    PrimaryAction("ثبت رمز تازه", loading, link.isNotBlank() && password.length >= 8 && password == confirm, onSubmit)
}

@Composable
private fun VerifyForm(
    link: String,
    loading: Boolean,
    onLink: (String) -> Unit,
    onComplete: () -> Unit,
    onResend: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(10.dp))
            Text(
                "برای محافظت از حساب، لینک کامل داخل ایمیل را کپی کنید.",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    LinkField(link, onLink)
    PrimaryAction("تأیید ایمیل", loading, link.isNotBlank(), onComplete)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onResend, enabled = !loading) { Text("ارسال دوباره") }
        TextButton(onClick = onContinue, enabled = !loading) { Text("بعداً انجام می‌دهم") }
    }
}

@Composable
private fun EmailField(value: String, onValue: (String) -> Unit, ime: ImeAction, onDone: (() -> Unit)? = null) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email", fontFamily = VazirmatnFamily) },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ime),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Left),
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    ime: ImeAction,
    onDone: (() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, fontFamily = VazirmatnFamily) },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ime),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Left),
        )
    }
}

@Composable
private fun LinkField(value: String, onValue: (String) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Link", fontFamily = VazirmatnFamily) },
            minLines = 2,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Left),
        )
    }
}

@Composable
private fun PrimaryAction(text: String, loading: Boolean, valid: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = valid && !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Divider(Modifier.weight(1f))
        Text("یا", Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Divider(Modifier.weight(1f))
    }
}

@Composable
private fun StatusMessage(text: String, isError: Boolean) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Text(
        text,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(container).padding(12.dp),
        color = content,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Start,
    )
}
