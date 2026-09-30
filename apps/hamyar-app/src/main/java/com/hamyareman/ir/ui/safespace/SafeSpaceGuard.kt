package com.hamyareman.ir.ui.safespace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** هیچ محتوای خصوصی حتی برای یک frame پیش از تأیید نشست compose نمی‌شود. */
@Composable
fun SafeContentGuard(onLocked: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    com.hamyareman.ir.ui.study.SecureWebEffect("Screenshots are disabled in the private workspace.")
    if (SafeSpaceSession.isUnlocked(context)) {
        content()
    } else {
        LaunchedEffect(Unit) { onLocked() }
    }
}
