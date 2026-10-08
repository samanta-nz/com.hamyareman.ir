package com.hamyareman.ir.ui.study

import com.hamyareman.ir.ui.content.ContentCatalog
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ساخت URL و سنجش دسترسی محتوای راه‌دور؛ تنها مبدأ فایل، پارس‌پک است. */
object ServerResolver {

    const val INTERNAL_PUBLIC = "https://c539776.parspack.net"
    private const val SAMPLE_BYTES = 256 * 1024
    private const val BENCHMARK_TTL_MS = 30L * 60L * 1000L

    data class Probe(
        val ok: Boolean,
        val status: Int = 0,
        val latencyMs: Long = 0,
        val bytesPerSecond: Long = 0,
        val bytesRead: Int = 0,
        val elapsedMs: Long = 0,
        val error: String = "",
    )

    data class SelectionTest(
        val mode: ServerPrefs.Mode = ServerPrefs.Mode.INTERNAL,
        val internal: Probe? = null,
        val selected: ServerPrefs.Origin? = null,
        val measuredAt: Long = System.currentTimeMillis(),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var benchmarkStarted = false
    @Volatile private var lastTest: SelectionTest? = null

    /** نشانی همان payload با کلید واقعی باکت پارس‌پک. */
    fun internal(key: String): String =
        INTERNAL_PUBLIC + "/" + key.split("/").joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }

    private fun sampleUrl(): String? =
        ContentCatalog.sampleHtmlItem()?.key?.takeIf { it.isNotBlank() }?.let(::internal)

    private fun measure(url: String): Probe {
        val started = System.nanoTime()
        var firstByteAt = 0L
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-" + (SAMPLE_BYTES - 1))
                setRequestProperty("Accept-Encoding", "identity")
            }
            val status = conn.responseCode
            if (status !in 200..299) {
                Probe(ok = false, status = status, error = "HTTP " + status)
            } else {
                var read = 0
                conn.inputStream.use { input ->
                    val buffer = ByteArray(32 * 1024)
                    while (read < SAMPLE_BYTES) {
                        val n = input.read(buffer, 0, minOf(buffer.size, SAMPLE_BYTES - read))
                        if (n <= 0) break
                        if (firstByteAt == 0L) firstByteAt = System.nanoTime()
                        read += n
                    }
                }
                val ended = System.nanoTime()
                val elapsedMs = ((ended - started) / 1_000_000L).coerceAtLeast(1L)
                val latencyMs = if (firstByteAt > 0L) {
                    ((firstByteAt - started) / 1_000_000L).coerceAtLeast(1L)
                } else elapsedMs
                val transferMs = (elapsedMs - latencyMs).coerceAtLeast(1L)
                Probe(
                    ok = read > 0,
                    status = status,
                    latencyMs = latencyMs,
                    bytesPerSecond = read * 1000L / transferMs,
                    bytesRead = read,
                    elapsedMs = elapsedMs,
                    error = if (read > 0) "" else "پاسخ بدون داده",
                )
            }
        } catch (t: Throwable) {
            Probe(ok = false, error = t.javaClass.simpleName)
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    suspend fun testSelection(
        @Suppress("UNUSED_PARAMETER") mode: ServerPrefs.Mode = ServerPrefs.Mode.INTERNAL,
    ): SelectionTest = withContext(Dispatchers.IO) {
        val url = sampleUrl() ?: return@withContext SelectionTest()
        val probe = measure(url)
        val selected = ServerPrefs.Origin.INTERNAL.takeIf { probe.ok }
        ServerPrefs.saveBenchmark(
            ServerPrefs.StoredProbe(probe.ok, probe.latencyMs, probe.bytesPerSecond),
        )
        SelectionTest(internal = probe, selected = selected).also { lastTest = it }
    }

    fun lastSelectionTest(): SelectionTest? = lastTest

    /** سنجش بی‌صدا در آغاز؛ فقط اتصال پارس‌پک را آزمایش می‌کند. */
    fun probeAsync() {
        if (benchmarkStarted) return
        synchronized(this) {
            if (benchmarkStarted) return
            benchmarkStarted = true
        }
        val fresh = System.currentTimeMillis() - ServerPrefs.lastBenchmarkAt < BENCHMARK_TTL_MS
        if (!fresh) scope.launch { runCatching { testSelection() } }
    }

    /** برای شناسه یا کلید مسیر، فقط URLهای پارس‌پک تولید می‌شود. */
    fun candidates(fileId: String, internalKey: String?): List<String> {
        val key = internalKey?.takeIf { it.isNotBlank() }
            ?: fileId.takeIf { it.startsWith("Bucket/") && it.isNotBlank() }
            ?: ContentCatalog.keyFor(fileId)
        return key?.let { listOf(internal(it)) }.orEmpty()
    }

    fun pick(fileId: String, internalKey: String?): String =
        candidates(fileId, internalKey).firstOrNull().orEmpty()
}
