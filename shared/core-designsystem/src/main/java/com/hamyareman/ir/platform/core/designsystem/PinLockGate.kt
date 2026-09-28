package com.hamyareman.ir.platform.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * دروازه‌ی قفل PIN — یک کامپوننت خالص که منطق رمز در آن نیست.
 *
 * [onVerify] باید true برگرداند اگر PIN درست بود (منطق هش در `core-security/AppLock` است).
 * [maxAttempts] برای جلوگیری از حدس زدن بی‌پایان است؛ بعد از آن دکمه قفل می‌شود.
 *
 * بیومتریک اختیاری است: اگر [biometricLabel] و [onBiometricRequest] داده شوند، یک دکمه‌ی
 * «بازکردن با اثر انگشت/چهره» نشان داده می‌شود. اجرای واقعی پرامپت بیرون از این کامپوننت است
 * (`core-security/BiometricPromptRunner`) تا این لایه به androidx.biometric وابسته نشود.
 * [externalNotice] پیام خطای بیومتریک است که از بیرون می‌آید.
 */
@Composable
fun PinLockGate(
    title: String = "این اپ قفل است",
    subtitle: String = "برای ادامه PIN را وارد کن.",
    minLength: Int = 4,
    maxLength: Int = 8,
    maxAttempts: Int = 5,
    biometricLabel: String? = null,
    biometricBusy: Boolean = false,
    externalNotice: String? = null,
    onBiometricRequest: (() -> Unit)? = null,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    val lockedOut = attempts >= maxAttempts

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { value ->
                if (!lockedOut) {
                    pin = value.filter { it.isDigit() }.take(maxLength)
                    error = null
                }
            },
            label = { Text("PIN") },
            singleLine = true,
            isError = error != null,
            enabled = !lockedOut,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = if (lockedOut) "قفل موقت" else "بازکردن",
            onClick = {
                if (pin.length < minLength) {
                    error = "PIN حداقل $minLength رقم است."
                    return@PrimaryButton
                }
                if (onVerify(pin)) {
                    pin = ""
                    onUnlocked()
                } else {
                    attempts += 1
                    pin = ""
                    error = if (lockedOut) {
                        "چند بار اشتباه وارد شد. برای محافظت از داده‌ها فعلاً قفل می‌ماند."
                    } else {
                        "PIN درست نیست. (${attempts} تلاش از $maxAttempts)"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.7f),
        )

        if (biometricLabel != null && onBiometricRequest != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBiometricRequest,
                enabled = !biometricBusy,
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                if (biometricBusy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("در حال تأیید…")
                } else {
                    Icon(
                        Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(biometricLabel)
                }
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        externalNotice?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "یادت رفته؟ داده‌های این دستگاه پاک نمی‌شوند، ولی باید اپ را دوباره نصب کنی یا از راه حساب کاربری وارد شوی.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
