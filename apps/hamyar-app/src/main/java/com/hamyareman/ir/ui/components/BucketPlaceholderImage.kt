package com.hamyareman.ir.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hamyareman.ir.ui.study.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

private data class PlaceholderSnapshot(val revision: Long = 0, val owner: String? = null, val bitmap: Bitmap? = null)
private val placeholderSnapshot = MutableStateFlow(PlaceholderSnapshot())

/** One renderer for the shared downloaded HTML; hundreds of missing thumbnails do not create hundreds of WebViews. */
@Composable
internal fun BucketPlaceholderImage(requestedRevision: Long, description: String?, modifier: Modifier, scale: ContentScale) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val revision = LessonCache.fileFor(context, ServerResolver.internal(BooksMenu.SPACEHOLDER_KEY)).lastModified()
    val token = remember(revision) { UUID.randomUUID().toString() }
    val snapshot by placeholderSnapshot.collectAsState()
    SideEffect {
        placeholderSnapshot.update {
            if (it.revision != revision) PlaceholderSnapshot(revision, token)
            else if (it.bitmap == null && it.owner == null) it.copy(owner = token)
            else it
        }
    }
    DisposableEffect(token) {
        onDispose { placeholderSnapshot.update { if (it.owner == token) it.copy(owner = null) else it } }
    }
    if (snapshot.revision == revision && snapshot.bitmap != null) {
        Image(snapshot.bitmap!!.asImageBitmap(), description, modifier, contentScale = scale)
    } else {
        CircularProgressIndicator()
        if (snapshot.owner == token && snapshot.revision == revision) {
            AndroidView(modifier = Modifier.size(1.dp).alpha(0f), factory = { context ->
                WebView(context).apply {
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : HmkWebViewClient(context.applicationContext, HmkWebViewClient.bucketHost(), confirmUpdates = false) {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            view.measure(View.MeasureSpec.makeMeasureSpec(512, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(512, View.MeasureSpec.EXACTLY))
                            view.layout(0, 0, 512, 512)
                            view.postDelayed({
                                if (placeholderSnapshot.value.owner != token) return@postDelayed
                                val image = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                                view.draw(Canvas(image))
                                placeholderSnapshot.update { if (it.revision == revision && it.owner == token) it.copy(bitmap = image, owner = null) else it }
                            }, 150)
                        }
                    }
                    loadUrl(ServerResolver.internal(BooksMenu.SPACEHOLDER_KEY))
                }
            }, onRelease = { (it.webViewClient as? HmkWebViewClient)?.dispose(); it.stopLoading(); it.destroy() })
        }
    }
}
