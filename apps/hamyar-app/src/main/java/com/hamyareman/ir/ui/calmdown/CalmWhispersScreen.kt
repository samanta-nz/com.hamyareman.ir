package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import com.hamyareman.ir.ui.study.HmkWebViewClient
import com.hamyareman.ir.ui.study.HtmlAudioKeepAliveService
import com.hamyareman.ir.ui.study.SecureWebEffect
import com.hamyareman.ir.ui.study.ServerResolver
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installHamyarAppearanceBridge
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.publishHamyarAppearance
import com.hamyareman.ir.ui.study.stopManagedMedia
import kotlinx.coroutines.delay

/** «نجواهای آرام‌بخش طبیعت» — صفحهٔ کامل موسیقی با تایمر خواب بومی. */
private const val MUSIC_FULL_KEY = "Bucket/Html-files/background-music-full.html"
private val TIMER_CHOICES = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)

/** فونت اختصاصی ناحیهٔ تایمر: پنج واحد کوچک‌تر از تایپوگرافی عادی همان نقش. */
private fun TextStyle.sleepTimerSmall(): TextStyle = copy(
    fontSize = (fontSize.value - 5f).coerceAtLeast(8f).sp,
    lineHeight = if (lineHeight.isSpecified) (lineHeight.value - 5f).coerceAtLeast(10f).sp else lineHeight,
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CalmWhispersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appearance = LocalUiPrefs.current
    val webRef = remember { arrayOfNulls<WebView>(1) }
    SecureWebEffect()

    var pageReady by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var timerOpen by remember { mutableStateOf(false) }
    var choiceMenuOpen by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    fun touchTimer() {
        timerOpen = true
        interactionTick++
    }

    // سه ثانیه بعد از آخرین تعامل، پنل جمع می‌شود؛ پخش یا تایمر متوقف نمی‌شود.
    LaunchedEffect(timerOpen, interactionTick) {
        if (timerOpen) {
            delay(3_000)
            timerOpen = false
        }
    }

    // شمارش معکوس؛ در پایان صدا و سرویس با هم بسته می‌شوند.
    LaunchedEffect(selectedMinutes) {
        if (selectedMinutes <= 0) {
            secondsLeft = 0
            HtmlAudioKeepAliveService.stop(context)
            return@LaunchedEffect
        }
        HtmlAudioKeepAliveService.start(context)
        secondsLeft = selectedMinutes * 60
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft -= 1
        }
        webRef[0]?.stopManagedMedia()
        HtmlAudioKeepAliveService.stop(context)
        selectedMinutes = 0
    }

    BackHandler {
        if (timerOpen) {
            timerOpen = false
            choiceMenuOpen = false
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webRef[0]?.stopManagedMedia()
            HtmlAudioKeepAliveService.stop(context)
        }
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (timerOpen) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "timer arrow",
    )
    val pulseTransition = rememberInfiniteTransition(label = "timer arrow pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "timer arrow scale",
    )

    // هیچ AppTopBarی اینجا نیست: WebView تمام صفحهٔ محتواست و فقط پنل تایمر
    // روی آن می‌نشیند. برگشت با کلید back دستگاه انجام می‌شود.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    installHamyarAppearanceBridge(appearance)
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    webViewClient = object : HmkWebViewClient(
                        viewContext.applicationContext,
                        HmkWebViewClient.bucketHost(),
                    ) {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            view.post { pageReady = true }
                            view.publishHamyarAppearance(
                                appearance.darkMode,
                                appearance.darkTheme,
                                cacheHit = mainDocumentWasLoadedFromCache(),
                            )
                            view.bindManagedMediaLifecycle()
                        }
                    }
                    installManagedMediaLifecycle()
                    webRef[0] = this
                    loadUrl(ServerResolver.internal(MUSIC_FULL_KEY))
                }
            },
            update = { it.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme) },
            modifier = Modifier.fillMaxSize(),
            onRelease = {
                it.stopManagedMedia()
                if (webRef[0] === it) webRef[0] = null
                it.destroy()
            },
        )

        if (!pageReady) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(46.dp),
                color = Color(0xFF62D4A9),
            )
        }

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = {
                    timerOpen = !timerOpen
                    choiceMenuOpen = false
                    interactionTick++
                },
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer {
                        rotationZ = arrowRotation
                        scaleX = if (timerOpen) 1f else pulse
                        scaleY = if (timerOpen) 1f else pulse
                    },
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (timerOpen) "بستن پنل تایمر" else "باز کردن پنل تایمر",
                    tint = Color.White,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x990B1713), RoundedCornerShape(20.dp)),
                )
            }

            AnimatedVisibility(
                visible = timerOpen,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xF21A2A24),
                        contentColor = Color.White,
                    ),
                    border = BorderStroke(1.dp, Color(0xFF85D6B2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (secondsLeft > 0) "تایمر خواب · ${format(secondsLeft)} مانده" else "تایمر خواب",
                            style = MaterialTheme.typography.titleSmall.sleepTimerSmall(),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box {
                                OutlinedButton(
                                    onClick = {
                                        choiceMenuOpen = true
                                        touchTimer()
                                    },
                                ) {
                                    Text(
                                        if (selectedMinutes > 0) "${selectedMinutes} دقیقه" else "انتخاب زمان",
                                        style = MaterialTheme.typography.labelLarge.sleepTimerSmall(),
                                    )
                                }
                                DropdownMenu(
                                    expanded = choiceMenuOpen,
                                    onDismissRequest = {
                                        choiceMenuOpen = false
                                        touchTimer()
                                    },
                                ) {
                                    TIMER_CHOICES.forEach { minutes ->
                                        DropdownMenuItem(
                                            text = { Text("$minutes دقیقه", style = MaterialTheme.typography.bodySmall.sleepTimerSmall()) },
                                            onClick = {
                                                selectedMinutes = minutes
                                                choiceMenuOpen = false
                                                touchTimer()
                                            },
                                        )
                                    }
                                }
                            }
                            if (selectedMinutes > 0) {
                                TextButton(
                                    onClick = {
                                        selectedMinutes = 0
                                        touchTimer()
                                    },
                                ) { Text("لغو", style = MaterialTheme.typography.labelLarge.sleepTimerSmall()) }
                            }
                            Spacer(Modifier.width(1.dp))
                        }
                        Text(
                            if (secondsLeft > 0) "در پایان، پخش صدا خودکار قطع می‌شود."
                            else "زمان دلخواهت را انتخاب کن؛ پنل خودکار جمع می‌شود.",
                            style = MaterialTheme.typography.labelSmall.sleepTimerSmall(),
                            color = Color(0xFFC7D8CF),
                        )
                    }
                }
            }
        }
    }
}

private fun format(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:" + seconds.toString().padStart(2, '0')
}
