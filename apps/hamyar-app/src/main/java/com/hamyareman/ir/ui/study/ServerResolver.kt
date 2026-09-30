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

    // سرور داخلی از ۲٫۲ پارس‌پک است (دیتاسنتر تهران، ترافیک نامحدود). باکت
    // آروان از سمت ارائه‌دهنده فقط‌خواندنی شد و دیگر قابل به‌روزرسانی نیست؛
    // همهٔ ۲۶۰ فایل محتوا با همان مسیر و پوشه‌بندی به اینجا منتقل شده‌اند.
    // نسخه‌های نصب‌شدهٔ قدیمی‌تر همچنان از آروان می‌خوانند و خواندن آنجا سالم است.
    const val INTERNAL_PUBLIC = "https://c539776.parspack.net"
    private const val EXTERNAL_ENDPOINT = "https://sgp.cloud.appwrite.io/v1"
    private const val EXTERNAL_PROJECT = "6abb134a002025222005"
    private const val EXTERNAL_BUCKET = "6abb564d00155cc56d65"
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

    /** نشانی عمومی فایل در سرور خارجی؛ شناسهٔ فایل مستقل از مسیر مجازی کاتالوگ است. */
    fun external(fileId: String): String {
        if (fileId.isBlank()) return ""
        val encoded = URLEncoder.encode(fileId, "UTF-8").replace("+", "%20")
        return "$EXTERNAL_ENDPOINT/storage/buckets/$EXTERNAL_BUCKET/files/$encoded/view?project=$EXTERNAL_PROJECT"
    }

    /** نشانی همان payload در سرور داخلی، با مسیر مجازی پوشه‌بندی‌شده. */
    fun internal(key: String): String =
        INTERNAL_PUBLIC + "/" + key.split("/").joinToString("/") {
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
            val urls = sampleUrls() ?: return@withContext SelectionTest(mode = mode)
            val (externalProbe, internalProbe) = coroutineScope {
                val ext = async { measure(ServerPrefs.Origin.EXTERNAL, urls.first) }
                val int = async { measure(ServerPrefs.Origin.INTERNAL, urls.second) }
                ext.await() to int.await()
            }
            val selected = when (mode) {
                ServerPrefs.Mode.EXTERNAL -> ServerPrefs.Origin.EXTERNAL.takeIf { externalProbe.ok }
                ServerPrefs.Mode.INTERNAL -> ServerPrefs.Origin.INTERNAL.takeIf { internalProbe.ok }
                ServerPrefs.Mode.FASTEST -> faster(externalProbe, internalProbe)
            }
            ServerPrefs.saveBenchmark(
                ServerPrefs.StoredProbe(externalProbe.ok, externalProbe.latencyMs, externalProbe.bytesPerSecond),
                ServerPrefs.StoredProbe(internalProbe.ok, internalProbe.latencyMs, internalProbe.bytesPerSecond),
                selected,
            )
            SelectionTest(
                mode = mode,
                external = externalProbe,
                internal = internalProbe,
                selected = selected,
            ).also { lastTest = it }
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

    fun preferredOrigin(): ServerPrefs.Origin = when (ServerPrefs.mode) {
        ServerPrefs.Mode.EXTERNAL -> ServerPrefs.Origin.EXTERNAL
        ServerPrefs.Mode.INTERNAL -> ServerPrefs.Origin.INTERNAL
        ServerPrefs.Mode.FASTEST -> ServerPrefs.fastestOrigin ?: ServerPrefs.Origin.INTERNAL
    }

    /**
     * حالت دستی فقط همان منبع را می‌گیرد. حالت سریع‌ترین، نتیجهٔ سنجش را اول و
     * منبع دوم را fallback می‌گذارد؛ هر URL فقط یک بار امتحان می‌شود.
     */
    fun candidates(fileId: String, internalKey: String?): List<String> {
        val externalUrl = external(fileId).takeIf { it.isNotBlank() }
        val internalUrl = internalKey?.takeIf { it.isNotBlank() }?.let(::internal)
        return when (ServerPrefs.mode) {
            ServerPrefs.Mode.EXTERNAL -> listOfNotNull(externalUrl)
            ServerPrefs.Mode.INTERNAL -> listOfNotNull(internalUrl)
            ServerPrefs.Mode.FASTEST -> {
                val preferred = preferredOrigin()
                if (preferred == ServerPrefs.Origin.EXTERNAL) {
                    listOfNotNull(externalUrl, internalUrl)
                } else {
                    listOfNotNull(internalUrl, externalUrl)
                }
            }
        }.distinct()
    }

    fun pick(fileId: String, internalKey: String?): String =
        candidates(fileId, internalKey).firstOrNull().orEmpty()
}
