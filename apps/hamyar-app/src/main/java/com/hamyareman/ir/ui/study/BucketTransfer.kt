package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** One transfer UI for every bucket consumer. All blocking calls run on IO threads. */
object BucketTransfer {
    data class State(val id: String, val url: String, val done: Long = 0,
        val total: Long = -1, val speed: Long = 0, val paused: Boolean = false,
        val confirmation: Boolean = false, val failure: Boolean = false)
    class Cancelled : IOException("دانلود متوقف شد")
    private class Control {
        var decision = CountDownLatch(1)
        @Volatile var accepted = false
        @Volatile var paused = false
        @Volatile var cancelled = false
        var at = System.nanoTime()
        var bytes = 0L
    }
    val states = MutableStateFlow<List<State>>(emptyList())
    val missing = MutableStateFlow<String?>(null)
    private val controls = ConcurrentHashMap<String, Control>()
    @Volatile var visible = false

    fun confirm(url: String): Boolean {
        if (!visible) return false
        val id = UUID.randomUUID().toString()
        val control = Control()
        controls[id] = control
        states.update { it + State(id, url, confirmation = true) }
        try {
            control.decision.await()
            return control.accepted
        } finally { finish(id) }
    }
    fun decide(id: String, accepted: Boolean) {
        controls[id]?.let { it.accepted = accepted; it.decision.countDown() }
    }
    fun retryAfterFailure(id: String): Boolean {
        val c = controls[id] ?: return false
        if (!visible) return false
        c.decision = CountDownLatch(1); c.accepted = false
        states.update { list -> list.map { if (it.id == id) it.copy(failure = true, speed = 0) else it } }
        c.decision.await()
        states.update { list -> list.map { if (it.id == id) it.copy(failure = false) else it } }
        return c.accepted && !c.cancelled
    }
    fun begin(url: String): String {
        val id = UUID.randomUUID().toString()
        controls[id] = Control()
        states.update { it + State(id, url) }
        return id
    }
    fun progress(id: String, done: Long, total: Long) {
        val c = controls[id] ?: return
        checkpoint(id)
        val now = System.nanoTime()
        val elapsed = now - c.at
        if (elapsed < 150_000_000L && done != total) return
        val speed = ((done - c.bytes).coerceAtLeast(0) * 1_000_000_000L / elapsed.coerceAtLeast(1))
        c.bytes = done; c.at = now
        states.update { list -> list.map { if (it.id == id) it.copy(done = done, total = total, speed = speed, paused = c.paused) else it } }
    }
    fun checkpoint(id: String) {
        val c = controls[id] ?: return
        while (c.paused && !c.cancelled) Thread.sleep(100)
        if (c.cancelled || Thread.currentThread().isInterrupted) throw Cancelled()
    }
    fun isPaused(id: String): Boolean = controls[id]?.paused == true
    fun pause(id: String) {
        controls[id]?.let { c -> c.paused = !c.paused
            c.at = System.nanoTime()
            states.update { list -> list.map { if (it.id == id) it.copy(paused = c.paused, speed = 0) else it } }
        }
    }
    fun cancel(id: String) { controls[id]?.let { it.cancelled = true; it.paused = false; it.decision.countDown() } }
    fun finish(id: String) { controls.remove(id); states.update { list -> list.filterNot { it.id == id } } }
    fun unavailable(url: String) { missing.value = url }
}

@Composable
fun BucketTransferHost() {
    val transfers by BucketTransfer.states.collectAsState()
    val missing by BucketTransfer.missing.collectAsState()
    DisposableEffect(Unit) {
        BucketTransfer.visible = true
        onDispose {
            BucketTransfer.visible = false
            BucketTransfer.states.value.forEach { BucketTransfer.cancel(it.id) }
        }
    }
    val current = transfers.firstOrNull()
    if (current != null) {
        if (current.confirmation) {
            AlertDialog(onDismissRequest = { BucketTransfer.decide(current.id, false) },
                title = { Text("محتوا به‌روزرسانی شده") },
                text = { Text("برای نمایش نسخهٔ جدید لازم است محتوا دوباره بارگذاری شود.") },
                confirmButton = { TextButton(onClick = { BucketTransfer.decide(current.id, true) }) { Text("بارگذاری مجدد") } },
                dismissButton = { TextButton(onClick = { BucketTransfer.decide(current.id, false) }) { Text("بعداً") } })
        } else if (current.failure) {
            AlertDialog(onDismissRequest = { BucketTransfer.decide(current.id, false) },
                title = { Text("دریافت محتوا انجام نشد") },
                text = { Text("اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.") },
                confirmButton = { TextButton(onClick = { BucketTransfer.decide(current.id, true) }) { Text("تلاش دوباره") } },
                dismissButton = { TextButton(onClick = { BucketTransfer.decide(current.id, false) }) { Text("توقف") } })
        } else {
            AlertDialog(onDismissRequest = {}, title = { Text(if (current.paused) "دانلود مکث شده" else "در حال دانلود محتوا") },
                text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (current.total > 0) LinearProgressIndicator(progress = { (current.done.toFloat() / current.total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(bytesLabel(current.done) + " از " + if (current.total > 0) bytesLabel(current.total) else "حجم نامشخص")
                    Text(bytesLabel(current.speed) + " در ثانیه")
                } },
                confirmButton = { TextButton(onClick = { BucketTransfer.pause(current.id) }) { Text(if (current.paused) "ادامه" else "مکث") } },
                dismissButton = { TextButton(onClick = { BucketTransfer.cancel(current.id) }) { Text("توقف") } })
        }
    }
    if (missing != null && current == null) {
        Dialog(onDismissRequest = { BucketTransfer.missing.value = null }) {
            Surface { Column(Modifier.fillMaxWidth().height(520.dp)) {
                TextButton(onClick = { BucketTransfer.missing.value = null }) { Text("برگشت") }
                RemoteHtmlPage(BooksMenu.SPACEHOLDER_KEY)
            } }
        }
    }
}

private fun bytesLabel(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.2f مگابایت".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.1f کیلوبایت".format(bytes / 1024.0)
    else -> "$bytes بایت"
}
