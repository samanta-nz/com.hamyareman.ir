package com.hamyareman.ir.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage as CoilImage
import coil.request.ImageRequest
import com.hamyareman.ir.ui.study.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cached image first, silent replacement, and the same remote placeholder for confirmed absence. */
@Composable
fun BucketImage(model: Any?, contentDescription: String?, modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit) {
    val context = LocalContext.current
    val address = (if (model is ImageRequest) model.data else model)?.toString().orEmpty()
    if (!address.startsWith(HmkWebViewClient.BUCKET_BASE + "/")) {
        CoilImage(model, contentDescription, modifier, contentScale = contentScale)
        return
    }
    var source by remember(address) { mutableStateOf(LessonCache.fileFor(context, address).takeIf { it.exists() }) }
    var revision by remember(address) { mutableLongStateOf(source?.lastModified() ?: 0) }
    var missing by remember(address) { mutableStateOf(false) }
    var failed by remember(address) { mutableStateOf(false) }
    var retry by remember(address) { mutableIntStateOf(0) }
    LaunchedEffect(address, retry) {
        failed = false
        val result = withContext(Dispatchers.IO) { LessonCache.prepare(context, address, confirmUpdates = false) }
        if (result != null) {
            missing = result.file != LessonCache.fileFor(context, address)
            source = if (missing) null else result.file
            revision = result.file.lastModified()
        } else failed = source == null
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            missing -> RemoteHtmlPage(BooksMenu.SPACEHOLDER_KEY, confirmUpdates = false)
            source != null -> CoilImage(
                (if (model is ImageRequest) model.newBuilder(context) else ImageRequest.Builder(context))
                    .data(source).memoryCacheKey(address + "@" + revision).build(),
                contentDescription, Modifier.matchParentSize(), contentScale = contentScale,
                onError = { LessonCache.evict(context, address); source = null; failed = true },
            )
            failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("اتصال اینترنت را بررسی کنید.")
                TextButton(onClick = { retry++ }) { Text("تلاش دوباره") }
            }
            else -> CircularProgressIndicator()
        }
    }
}
