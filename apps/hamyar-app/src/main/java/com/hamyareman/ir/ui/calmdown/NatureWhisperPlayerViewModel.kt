package com.hamyareman.ir.ui.calmdown

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamyareman.ir.ui.appearance.UiPrefs
import com.hamyareman.ir.ui.study.HmkWebViewClient
import com.hamyareman.ir.ui.study.HtmlAudioKeepAliveService
import com.hamyareman.ir.ui.study.bindManagedMediaLifecycle
import com.hamyareman.ir.ui.study.installHamyarAppearanceBridge
import com.hamyareman.ir.ui.study.installManagedMediaLifecycle
import com.hamyareman.ir.ui.study.publishHamyarAppearance
import com.hamyareman.ir.ui.study.stopManagedMedia
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MUSIC_FULL_KEY = "Bucket/Html-files/background-music-full.html"
private val MUSIC_FULL_URL = HmkWebViewClient.bucketUrl(MUSIC_FULL_KEY)

/**
 * Activity-level owner for «نجواهای آرام‌بخش طبیعت».
 *
 * The WebView intentionally survives route changes so the same HTML document and
 * AudioContext can keep playing when the user chooses «ادامه پخش».
 */
class NatureWhisperPlayerViewModel : ViewModel() {
    internal var webView: WebView? = null
        private set

    val pageReady = mutableStateOf(false)
    val webViewReady = mutableStateOf(false)
    val selectedMinutes = mutableIntStateOf(0)
    val secondsLeft = mutableIntStateOf(0)

    private var timerJob: Job? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun ensureWebView(context: Context, appearance: UiPrefs) {
        webView?.let {
            it.onResume()
            it.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
            return
        }

        val web = WebView(context).apply {
            installHamyarAppearanceBridge(appearance)
            addJavascriptInterface(KeepAliveBridge(context.applicationContext), "HamyarHost")
            installManagedMediaLifecycle()

            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            webViewClient = object : HmkWebViewClient(
                context.applicationContext,
                HmkWebViewClient.bucketHost(),
            ) {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.publishHamyarAppearance(
                        appearance.darkMode,
                        appearance.darkTheme,
                        cacheHit = mainDocumentWasLoadedFromCache(),
                    )
                    view.bindManagedMediaLifecycle(watchWebAudio = true)
                    pageReady.value = true
                }
            }
        }

        webView = web
        webViewReady.value = true
        pageReady.value = false
        web.loadUrl(MUSIC_FULL_URL)
    }

    fun updateAppearance(appearance: UiPrefs) {
        webView?.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
    }

    fun startTimer(context: Context, minutes: Int) {
        val m = minutes.coerceAtLeast(0)
        timerJob?.cancel()

        if (m == 0) {
            selectedMinutes.intValue = 0
            secondsLeft.intValue = 0
            return
        }

        selectedMinutes.intValue = m
        secondsLeft.intValue = m * 60
        HtmlAudioKeepAliveService.start(context)

        timerJob = viewModelScope.launch {
            while (secondsLeft.intValue > 0) {
                delay(1_000)
                secondsLeft.intValue -= 1
            }
            webView?.stopManagedMedia()
            HtmlAudioKeepAliveService.stop(context)
            selectedMinutes.intValue = 0
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        selectedMinutes.intValue = 0
        secondsLeft.intValue = 0
        // Cancelling the timer must not stop active playback.
    }

    fun stopPlayback(context: Context) {
        timerJob?.cancel()
        timerJob = null
        selectedMinutes.intValue = 0
        secondsLeft.intValue = 0
        webView?.stopManagedMedia()
        HtmlAudioKeepAliveService.stop(context)
    }

    private fun onHtmlKeepAlive(context: Context, on: Boolean) {
        if (on) {
            HtmlAudioKeepAliveService.start(context)
        } else {
            HtmlAudioKeepAliveService.stop(context)
        }
    }

    private inner class KeepAliveBridge(private val context: Context) {
        @JavascriptInterface
        fun keepAlive(on: Boolean) {
            onHtmlKeepAlive(context, on)
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        val web = webView
        webView = null
        webViewReady.value = false
        if (web != null) {
            HtmlAudioKeepAliveService.stop(web.context)
            web.destroy()
        }
        super.onCleared()
    }
}
