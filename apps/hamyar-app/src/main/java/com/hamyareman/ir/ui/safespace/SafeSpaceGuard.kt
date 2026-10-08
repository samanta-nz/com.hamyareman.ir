package com.hamyareman.ir.ui.safespace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** هیچ محتوای خصوصی حتی برای یک frame پیش از تأیید نشست compose نمی‌شود. */
@Composable
fun SafeContentGuard(onLocked: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    if (SafeSpaceSession.isUnlocked(context)) {
        content()
    } else {
        LaunchedEffect(Unit) { onLocked() }
    }
}
