package com.hamyareman.admin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.hamyareman.admin.ui.AdminHomeScreen
import com.hamyareman.admin.ui.AdminLoginScreen
import com.hamyareman.admin.ui.AdminSettingsScreen
import com.hamyareman.ir.platform.core.appwrite.AuthUser
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.designsystem.BrandTheme
import com.hamyareman.ir.platform.core.designsystem.PlatformTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

val LocalAdmin = staticCompositionLocalOf<AdminContainer> { error("AdminContainer missing") }

class AdminMainActivity : AppCompatActivity() {

    private val loggedIn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AdminApplication
        enableEdgeToEdge()
        setContent {
            var tick by remember { mutableStateOf(0) }
            var showSettings by remember { mutableStateOf(false) }
            val container = remember(tick) { app.container }
            val scope = rememberCoroutineScope()
            var signingIn by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(tick) {
                val lastCrash = app.consumeLastCrash()
                if (lastCrash != null) {
                    error = "اپ دفعهٔ قبل بسته شد. اگر تکرار شد این متن را بفرست:\n$lastCrash"
                }
                val outcome = runCatching {
                    val u = withTimeoutOrNull(8_000) {
                        runCatching { container.auth.currentUser() }.getOrNull()
                    } ?: return@runCatching false to null
                    verifyAdmin(container, u)
                }
                outcome.fold(
                    onSuccess = { (ok, msg) ->
                        loggedIn.value = ok
                        if (msg != null) error = msg
                    },
                    onFailure = {
                        loggedIn.value = false
                    },
                )
            }

            PlatformTheme(brand = BrandTheme.Mint, darkTheme = isSystemInDarkTheme()) {
                CompositionLocalProvider(LocalAdmin provides container) {
                    Surface(Modifier.fillMaxSize()) {
                        when {
                            showSettings -> AdminSettingsScreen(
                                onBack = { showSettings = false },
                                onSaved = {
                                    app.rebuildContainer()
                                    tick++
                                    showSettings = false
                                },
                            )
                            loggedIn.value -> AdminHomeScreen(
                                onLogout = {
                                    scope.launch {
                                        runCatching { adminIo { container.auth.logout() } }
                                        loggedIn.value = false
                                    }
                                },
                                onSettings = { showSettings = true },
                            )
                            else -> AdminLoginScreen(
                                loading = signingIn,
                                error = error,
                                onSettings = { showSettings = true },
                                onSignIn = { email, password ->
                                    signingIn = true
                                    error = null
                                    scope.launch {
                                        val outcome = runCatching {
                                            when (val r = adminIo { container.auth.signIn(email, password) }) {
                                                is AppResult.Err -> false to r.error.userMessage
                                                is AppResult.Ok -> verifyAdmin(container, r.value)
                                            }
                                        }
                                        outcome.fold(
                                            onSuccess = { (ok, msg) ->
                                                loggedIn.value = ok
                                                error = msg
                                            },
                                            onFailure = { t ->
                                                loggedIn.value = false
                                                error = "ورود ناموفق: " + (t.message?.ifBlank { null } ?: t.javaClass.simpleName)
                                            },
                                        )
                                        signingIn = false
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun verifyAdmin(container: AdminContainer, user: AuthUser): Pair<Boolean, String?> {
    // نقش admin باید سمت Appwrite روی حساب ست شود. هیچ ایمیل hard-code شده‌ای
    // راه ورود نیست؛ این کار با تعویض ایمیل/مهندسی APK قابل دور زدن می‌شد.
    val labelOk = user.labels.any { it.equals("admin", true) }
    if (container.api.configured) {
        return when (val p = adminIo { container.api.requireAdmin(user.id) }) {
            is AppResult.Ok -> true to null
            is AppResult.Err -> {
                runCatching { container.auth.logout() }
                false to p.error.userMessage
            }
        }
    }
    if (labelOk) return true to null
    runCatching { container.auth.logout() }
    return false to "این حساب برچسب admin ندارد. یک مدیرِ فعال باید این برچسب را در Appwrite تنظیم کند."
}
