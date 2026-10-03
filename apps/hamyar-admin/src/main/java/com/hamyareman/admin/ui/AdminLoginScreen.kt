package com.hamyareman.admin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun AdminLoginScreen(
    loading: Boolean,
    error: String?,
    onSignIn: (email: String, password: String) -> Unit,
    // OAuth گوگل در پروژهٔ فعلی Appwrite غیرفعال است؛ نمایش دکمهٔ خراب فقط
    // کاربر را به Error 412 می‌فرستاد. ورود مدیر با email/password انجام می‌شود.
    onSettings: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        val e = email.trim()
        val p = password
        when {
            loading -> return
            e.isBlank() || p.isBlank() -> localError = "ایمیل و رمز را بنویس."
            !e.contains("@") -> localError = "ایمیل را کامل بنویس."
            p.length < 8 -> localError = "رمز حداقل ۸ نویسه است."
            else -> {
                localError = null
                keyboard?.hide()
                onSignIn(e, p)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ادمین همیار من", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "ورود امن با ایمیل و رمز حسابی که روی Appwrite برچسب admin دارد. کلید سرور برای مدیریت داده است، نه ورود.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; localError = null },
                        label = { Text("ایمیل") },
                        singleLine = true,
                        enabled = !loading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; localError = null },
                        label = { Text("رمز") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { submit() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (loading) "در حال ورود…" else "ورود") }
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator()
                }
                val shown = localError ?: error
                shown?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("تنظیمات اتصال سرور")
                }
            }
        }
    }
}
