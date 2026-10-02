package com.hamyareman.ir.ui.study

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.ui.appearance.LocalUiPrefs
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.feature.study.StudyProgressRepository

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
internal fun MathInteractiveHtml(
    packId: String,
    kind: String,
    assetPath: String,
    modifier: Modifier = Modifier,
    onZoomChanged: (Boolean) -> Unit = {},
) {
    val ctx = LocalContext.current
    val appearance = LocalUiPrefs.current
    val progress = LocalAppContainer.current.studyProgress
    val store = remember { LocalStore(ctx, "hamyar_math_html") }
    val webRef = remember { arrayOfNulls<WebView>(1) }
    ManagedWebMediaEffect { webRef[0] }
    val bridge = remember(packId, kind) {
        HamyarHtmlBridge(ctx.applicationContext, packId, kind, store, progress)
    }
    AndroidView(
        factory = { c ->
            ZoomResetWebView(c).apply {
                installHamyarAppearanceBridge(appearance)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

                    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
                        /* زوم فقط مالِ WebView است — به Compose خبر نمی‌دهیم. */
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        view.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
                        view.evaluateJavascript(
                            "(function(){var s=document.createElement('script');s.src='file:///android_asset/math/c905/hamyar-persist.js';document.documentElement.appendChild(s);})();",
                            null,
                        )
                        view.bindManagedMediaLifecycle()
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = false
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.allowFileAccess = true
                addJavascriptInterface(bridge, "Hamyar")
                installManagedMediaLifecycle()
                webRef[0] = this
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(android.graphics.Color.WHITE)
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
            }
        },
        update = { wv ->
            wv.publishHamyarAppearance(appearance.darkMode, appearance.darkTheme)
            val target = "file:///android_asset/$assetPath"
            if (wv.tag != target) {
                wv.tag = target
                wv.loadUrl(target)
            }
        },
        modifier = modifier.fillMaxSize(),
        onRelease = {
            it.stopManagedMedia()
            if (webRef[0] === it) webRef[0] = null
            it.destroy()
        },
    )
}

internal class HamyarHtmlBridge(
    private val appCtx: android.content.Context,
    private val packId: String,
    private val kind: String,
    private val store: LocalStore,
    private val progress: StudyProgressRepository,
) {
    private val key get() = "html:$kind:$packId"

    @JavascriptInterface
    fun saveState(json: String) {
        store.putString(key, json)
    }

    @JavascriptInterface
    fun loadState(): String = store.getString(key)

    @JavascriptInterface
    fun recordItem(itemId: String, correct: Boolean) {
        val today = JalaliDate.todayIso()
        progress.recordExercise(packId, "${kind}_$itemId", correct, today)
        val mark = if (correct) "درست" else "نادرست"
        val kindFa = when (kind) {
            "book" -> "تمرین کتاب"
            "flash" -> "فلش‌کارت"
            "exam" -> "آزمون"
            else -> kind
        }
        StudyActivity.add(appCtx, packId, "item", "$kindFa $itemId — $mark")
        if (kind == "flash") {
            progress.reviewCardMath(packId, itemId, if (correct) 5 else 1, today)
        }
    }

    @JavascriptInterface
    fun recordExam(scorePct: Int, total: Int, correctCount: Int, wrongCsv: String) {
        val wrong = if (wrongCsv.isBlank()) emptyList() else wrongCsv.split(",").filter { it.isNotBlank() }
        if (kind != "book") {
            progress.recordHtmlExam(
                packId = packId,
                scorePct = scorePct,
                total = total,
                correctCount = correctCount,
                wrongIds = wrong,
                dateKey = JalaliDate.todayIso(),
            )
        }
        val kindFa = when (kind) {
            "book" -> "تمرین کتاب"
            "flash" -> "فلش‌کارت"
            "exam" -> "آزمون"
            else -> kind
        }
        StudyActivity.add(
            appCtx,
            packId,
            "exam",
            "صحت‌سنجی $kindFa: $correctCount از $total درست ($scorePct٪)",
        )
    }
}
