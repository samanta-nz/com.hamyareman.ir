package com.hamyareman.ir.ui.study

import com.hamyareman.ir.ui.content.ContentCatalog
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** انتخاب origin و سنجش واقعی latency + سرعت دریافت Range از هر دو سرور. */
object ServerResolver {

    const val ARVAN_PUBLIC = "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir"
    private const val SAMPLE_BYTES = 256 * 1024
    private const val BENCHMARK_TTL_MS = 30L * 60L * 1000L

    data class Probe(
        val origin: ServerPrefs.Origin,
        val ok: Boolean,
        val status: Int = 0,
        val latencyMs: Long = 0,
        val bytesPerSecond: Long = 0,
        val bytesRead: Int = 0,
        val elapsedMs: Long = 0,
        val error: String = "",
    )

    data class SelectionTest(
        val mode: ServerPrefs.Mode,
        val external: Probe? = null,
        val internal: Probe? = null,
        val selected: ServerPrefs.Origin? = null,
        val measuredAt: Long = System.currentTimeMillis(),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var benchmarkStarted = false
    @Volatile private var lastTest: SelectionTest? = null

    fun external(fileId: String): String =
        ContentCatalog.keyFor(fileId)?.let(::internal).orEmpty()

    fun internal(key: String): String =
        ARVAN_PUBLIC + "/" + key.split("/").joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }

    private fun sampleUrls(): Pair<String, String>? {
        val item = ContentCatalog.sampleHtmlItem() ?: return null
        return external(item.aw) to internal(item.key)
    }

    private fun measure(origin: ServerPrefs.Origin, url: String): Probe {
        val started = System.nanoTime()
        var firstByteAt = 0L
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-${SAMPLE_BYTES - 1}")
                setRequestProperty("Accept-Encoding", "identity")
            }
            val status = conn.responseCode
            if (status !in 200..299) {
                Probe(origin, ok = false, status = status, error = "HTTP $status")
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
                val speed = read * 1000L / transferMs
                Probe(
                    origin = origin,
                    ok = read > 0,
                    status = status,
                    latencyMs = latencyMs,
                    bytesPerSecond = speed,
                    bytesRead = read,
                    elapsedMs = elapsedMs,
                    error = if (read > 0) "" else "پاسخ بدون داده",
                )
            }
        } catch (t: Throwable) {
            Probe(origin, ok = false, error = t.javaClass.simpleName)
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun faster(external: Probe, internal: Probe): ServerPrefs.Origin? = when {
        external.ok && !internal.ok -> ServerPrefs.Origin.EXTERNAL
        internal.ok && !external.ok -> ServerPrefs.Origin.INTERNAL
        !external.ok && !internal.ok -> null
        // elapsed واقعی یک Range هم latency و هم throughput را وارد تصمیم می‌کند.
        internal.elapsedMs < external.elapsedMs -> ServerPrefs.Origin.INTERNAL
        else -> ServerPrefs.Origin.EXTERNAL
    }

    suspend fun testSelection(mode: ServerPrefs.Mode = ServerPrefs.mode): SelectionTest =
        withContext(Dispatchers.IO) {
            val key = ContentCatalog.sampleHtmlKey()
                ?: return@withContext SelectionTest(mode = mode)
            // از نسخهٔ ۲٫۰ فقط یک origin عمومی داریم؛ هر تست دقیقاً یک Range کوچک می‌گیرد.
            val probe = measure(ServerPrefs.Origin.INTERNAL, internal(key))
            val selected = ServerPrefs.Origin.INTERNAL.takeIf { probe.ok }
            ServerPrefs.saveBenchmark(null, ServerPrefs.StoredProbe(probe.ok, probe.latencyMs, probe.bytesPerSecond), selected)
            SelectionTest(mode = mode, internal = probe, selected = selected).also { lastTest = it }
        }

    fun lastSelectionTest(): SelectionTest? = lastTest

    /** سنجش بی‌صدا در آغاز؛ نتیجهٔ ذخیره‌شدهٔ تازه دوباره دانلود نمی‌شود. */
    fun probeAsync() {
        if (benchmarkStarted) return
        synchronized(this) {
            if (benchmarkStarted) return
            benchmarkStarted = true
        }
        val fresh = System.currentTimeMillis() - ServerPrefs.lastBenchmarkAt < BENCHMARK_TTL_MS
        if (!fresh) scope.launch { runCatching { testSelection(ServerPrefs.Mode.FASTEST) } }
    }

    fun preferredOrigin(): ServerPrefs.Origin = ServerPrefs.Origin.INTERNAL

    /** یک کلید عمومی دقیقاً یک URL آروان دارد؛ fallback پرمصرف Appwrite حذف شده است. */
    fun candidates(fileId: String, arvanKey: String?): List<String> =
        arvanKey?.let { listOf(internal(it)) }.orEmpty()

    fun pick(fileId: String, arvanKey: String?): String =
        candidates(fileId, arvanKey).firstOrNull().orEmpty()
}
