package com.hamyareman.ir.ui.study

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** نوع محتوا؛ از روی پسوند آدرس تشخیص داده می‌شود. */
enum class ContentKind { HTML, PDF, AUDIO, IMAGE }

/** پیشرفت دانلود: حجم کل (۱- یعنی نامعلوم)، حجم دانلودشده، سرعت و وضعیت مکث. */
class DownloadProgress(
    val doneBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val paused: Boolean,
)

/** خروجی نهایی برای نمایش. [htmlBytes] فقط برای HTML است و فقط در RAM نگه‌داری می‌شود. */
class ContentReady(
    val kind: ContentKind,
    val file: File,
    val htmlBytes: ByteArray?,
    val fromCache: Boolean,
    val isPlaceholder: Boolean,
    /** با هر جایگزینی/بارگذاری مجدد زیاد می‌شود تا UI دوباره نمایش دهد. */
    val revision: Int,
)

sealed interface ContentState {
    data object Checking : ContentState

    /** مشکل اتصال: UI پیام «بررسی اتصال» و دکمه‌ی «تلاش دوباره» را نشان می‌دهد. */
    data object NoInternet : ContentState

    data class Downloading(val progress: DownloadProgress, val isUpdate: Boolean) : ContentState

    /** نسخه‌ی تازه روی باکت است؛ نمایش کش ادامه دارد و UI دیالوگ «اکنون / بعداً» را نشان می‌دهد. */
    data class UpdateAvailable(val ready: ContentReady) : ContentState

    class Ready(val value: ContentReady) : ContentState

    data class Failed(val message: String) : ContentState
}

/**
 * موتور یکپارچه‌ی بارگذاری برای html / pdf / mp3-opus / تصویر.
 *
 * - نبودن فایل در باکت (با اینترنت سالم) ← placeholder یکتا
 * - قطع اینترنت ← NoInternet + تلاش دوباره
 * - دانلود با پیشرفت، سرعت، مکث/ادامه (Range) و کش
 * - نمایش از کش + بررسی ETag با درخواست Range 0-0 (کوچک‌ترین مبادله)
 * - HTML رمزشده (HMK1 = AES-256-GCM) فقط در RAM رمزگشایی می‌شود
 */
object ContentLoader {
    const val BASE_URL = "https://c539776.parspack.net/"

    /** تنها placeholder مشترک همه‌ی بخش‌ها (از قبل در server-map.json هست). */
    const val PLACEHOLDER_URL = BASE_URL + "Bucket/Html-files/spaceholder.html"

    fun kindOf(url: String): ContentKind {
        val path = url.substringBefore('#').substringBefore('?').lowercase()
        return when {
            path.endsWith(".pdf") -> ContentKind.PDF
            path.endsWith(".mp3") || path.endsWith(".opus") || path.endsWith(".ogg") ||
                path.endsWith(".m4a") || path.endsWith(".aac") -> ContentKind.AUDIO
            path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") ||
                path.endsWith(".webp") || path.endsWith(".gif") -> ContentKind.IMAGE
            else -> ContentKind.HTML
        }
    }

    fun session(ctx: Context, url: String, kind: ContentKind = kindOf(url)): ContentSession =
        ContentSession(ctx.applicationContext, url, kind)

    internal fun isOnline(ctx: Context): Boolean = runCatching {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)

    /** هر IOException (DNS، timeout، قطع سوکت، SSL) یعنی مشکل اتصال. */
    internal fun isConnectivityError(t: Throwable): Boolean =
        t is IOException && t !is java.io.FileNotFoundException

    internal fun normEtag(raw: String?): String? =
        raw?.trim()?.removePrefix("W/")?.trim('"')?.takeIf { it.isNotEmpty() }

    internal fun cacheFile(ctx: Context, url: String): File = LessonCache.fileFor(ctx, url)
    internal fun etagFile(ctx: Context, url: String): File = File(cacheFile(ctx, url).path + ".etag")
    internal fun partFile(ctx: Context, url: String): File = File(cacheFile(ctx, url).path + ".part")

    private fun readPrefix(file: File, max: Int = 16 * 1024): ByteArray {
        if (!file.exists() || file.length() <= 0L) return ByteArray(0)
        val size = minOf(file.length(), max.toLong()).toInt()
        val out = ByteArray(size)
        var off = 0
        file.inputStream().use { input ->
            while (off < size) {
                val n = input.read(out, off, size - off)
                if (n <= 0) break
                off += n
            }
        }
        return if (off == size) out else out.copyOf(off)
    }

    /** بررسی ساختاری بایت‌ها تا صفحه‌ی خطا یا فایل ناقص هرگز کش نشود. */
    internal fun isValid(kind: ContentKind, file: File): Boolean {
        if (!file.exists() || file.length() <= 0L) return false
        val p = readPrefix(file)
        if (p.isEmpty()) return false
        return when (kind) {
            ContentKind.HTML ->
                (HtmlCodec.hasMagic(p) && file.length() >= HtmlCodec.MIN_WRAPPED_BYTES) || HtmlCodec.isPlainHtml(p)
            ContentKind.PDF -> p.size >= 5 && p[0] == '%'.code.toByte() && p[1] == 'P'.code.toByte() &&
                p[2] == 'D'.code.toByte() && p[3] == 'F'.code.toByte()
            ContentKind.AUDIO, ContentKind.IMAGE ->
                p[0] != '<'.code.toByte() && p[0] != '{'.code.toByte()
        }
    }
}

/**
 * یک نشست بارگذاری برای یک آدرس. UI فقط [state] را مشاهده می‌کند و این متدها را صدا می‌زند:
 * [pause] / [resume] / [stop] / [retry] / [answerUpdate].
 */
class ContentSession internal constructor(
    private val ctx: Context,
    private val url: String,
    private val kind: ContentKind,
) {
    private val _state = MutableStateFlow<ContentState>(ContentState.Checking)
    val state: StateFlow<ContentState> = _state.asStateFlow()

    @Volatile private var paused = false
    @Volatile private var stopped = false
    @Volatile private var updateAnswer: ((Boolean) -> Unit)? = null
    private var job: Job? = null
    private var revision = 0

    fun start(scope: CoroutineScope) {
        job?.cancel()
        paused = false
        stopped = false
        job = scope.launch(Dispatchers.IO) { runFlow() }
    }

    fun retry(scope: CoroutineScope) = start(scope)

    fun pause() {
        paused = true
        (_state.value as? ContentState.Downloading)?.let {
            val p = it.progress
            _state.value = it.copy(progress = DownloadProgress(p.doneBytes, p.totalBytes, 0L, true))
        }
    }

    fun resume() { paused = false }
    fun stop() { stopped = true; paused = false }
    fun cancel() { job?.cancel(); updateAnswer?.invoke(false) }

    /** پاسخ کاربر به دیالوگ «محتوا بروزرسانی شده». */
    fun answerUpdate(accept: Boolean) { updateAnswer?.invoke(accept) }

    // ───────────────────────── جریان اصلی ─────────────────────────

    private suspend fun runFlow() {
        val target = ContentLoader.cacheFile(ctx, url)
        val isPlaceholder = url == ContentLoader.PLACEHOLDER_URL
        if (ContentLoader.isValid(kind, target)) {
            // اگر از قبل در کش بود: همان لحظه نمایش بده، سپس بررسی سبک به‌روزرسانی.
            if (showCached(target, isPlaceholder) == null) return
            checkForUpdate(target, isPlaceholder)
            return
        }
        _state.value = ContentState.Checking
        if (!ContentLoader.isOnline(ctx)) {
            _state.value = ContentState.NoInternet
            return
        }
        when (val r = download(isUpdate = false, showProgress = true)) {
            is Dl.Done -> showCached(target, isPlaceholder, fromCache = false)
            is Dl.Missing -> {
                if (isPlaceholder) {
                    _state.value = ContentState.Failed("صفحه‌ی جایگزین در باکت نیست.")
                } else {
                    showPlaceholder()
                }
            }
            is Dl.Offline -> _state.value = ContentState.NoInternet
            is Dl.Stopped -> _state.value = ContentState.Failed("دانلود متوقف شد.")
            is Dl.Error -> _state.value = ContentState.Failed(r.message)
        }
    }

    /** فایل در باکت نبود: همان placeholder یکتا با همان منطق (بدون دیالوگ) بارگذاری می‌شود. */
    private suspend fun showPlaceholder() {
        val sub = ContentSession(ctx, ContentLoader.PLACEHOLDER_URL, ContentKind.HTML)
        sub.runFlow()
        val s = sub.state.value
        _state.value = if (s is ContentState.Ready) {
            val v = s.value
            ContentState.Ready(ContentReady(v.kind, v.file, v.htmlBytes, v.fromCache, true, ++revision))
        } else {
            s
        }
    }

    /** نمایش از کش؛ HTML رمزشده در صورت لزوم فقط در RAM رمزگشایی می‌شود. */
    private fun showCached(target: File, isPlaceholder: Boolean, fromCache: Boolean = true): ContentReady? {
        val ready = buildReady(target, isPlaceholder, fromCache)
        if (ready == null) {
            _state.value = ContentState.Failed("خواندن یا رمزگشایی محتوا ممکن نشد. دوباره وارد شو و تلاش کن.")
            return null
        }
        _state.value = ContentState.Ready(ready)
        return ready
    }

    private fun buildReady(target: File, isPlaceholder: Boolean, fromCache: Boolean): ContentReady? {
        if (kind != ContentKind.HTML) {
            return ContentReady(kind, target, null, fromCache, isPlaceholder, ++revision)
        }
        val bytes = runCatching {
            if (target.length() > LessonCache.MAX_DECRYPT_BYTES) error("too large")
            HtmlCodec.decodeHtml(ctx, target.readBytes())
        }.getOrNull() ?: return null
        return ContentReady(kind, target, bytes, fromCache, isPlaceholder, ++revision)
    }

    // ───────────────────────── بررسی به‌روزرسانی (ETag + Range 0-0) ─────────────────────────

    private sealed interface Probe {
        data class Ok(val etag: String?, val total: Long) : Probe
        data object Unavailable : Probe
    }

    /** درخواست Range: bytes=0-0 — فقط هدرها اهمیت دارند و بدنه خوانده نمی‌شود. */
    private fun probe(): Probe {
        var conn: HttpURLConnection? = null
        return try {
            val c = (URL(LessonCache.canonical(url)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("Range", "bytes=0-0")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Cache-Control", "no-cache")
            }
            conn = c
            when (c.responseCode) {
                206 -> {
                    val total = c.getHeaderField("Content-Range")
                        ?.substringAfterLast('/')?.trim()?.toLongOrNull() ?: -1L
                    Probe.Ok(ContentLoader.normEtag(c.getHeaderField("ETag")), total)
                }
                200 -> Probe.Ok(ContentLoader.normEtag(c.getHeaderField("ETag")), c.contentLengthLong)
                else -> Probe.Unavailable // آفلاین/حذف‌شده/خطای سرور: نمایش کش ادامه دارد.
            }
        } catch (t: Throwable) {
            Probe.Unavailable
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private suspend fun checkForUpdate(target: File, isPlaceholder: Boolean) {
        val remote = probe() as? Probe.Ok ?: return

        val savedEtag = runCatching { ContentLoader.etagFile(ctx, url).readLines().firstOrNull() }
            .getOrNull()?.takeIf { it.isNotBlank() }
        val changed = when {
            remote.etag != null && savedEtag != null -> remote.etag != savedEtag
            // کش قدیمی ETag ذخیره‌شده ندارد: اندازه‌ی بایت معیار جایگزین است.
            remote.total >= 0L -> remote.total != target.length()
            else -> false
        }
        if (!changed) {
            if (remote.etag != null && savedEtag == null) saveEtag(remote.etag)
            return
        }

        // تصاویر (و placeholder) بدون دیالوگ و در پس‌زمینه جایگزین می‌شوند.
        val silent = kind == ContentKind.IMAGE || isPlaceholder
        if (!silent) {
            val current = (_state.value as? ContentState.Ready)?.value ?: return
            val accepted = suspendCancellableCoroutine<Boolean> { cont ->
                updateAnswer = { yes -> if (cont.isActive) cont.resume(yes) }
                cont.invokeOnCancellation { updateAnswer = null }
                _state.value = ContentState.UpdateAvailable(current)
            }
            updateAnswer = null
            if (!accepted) {
                // «بعداً»: دیالوگ بسته می‌شود، نمایش قبلی ادامه می‌یابد؛ اجرای بعدی همین روال تکرار می‌شود.
                _state.value = ContentState.Ready(current)
                return
            }
        }

        when (download(isUpdate = true, showProgress = !silent)) {
            is Dl.Done -> showCached(target, isPlaceholder, fromCache = false)
            else -> showCached(target, isPlaceholder) // ناموفق/متوقف: نسخه‌ی قبلی دست‌نخورده
        }
    }

    private fun saveEtag(etag: String) {
        runCatching { ContentLoader.etagFile(ctx, url).writeText(etag) }
    }

    // ───────────────────────── دانلود با مکث / ادامه ─────────────────────────

    private sealed interface Dl {
        data object Done : Dl
        data object Missing : Dl
        data object Offline : Dl
        data object Stopped : Dl
        data class Error(val message: String) : Dl
    }

    private suspend fun download(isUpdate: Boolean, showProgress: Boolean): Dl = withContext(Dispatchers.IO) {
        val target = ContentLoader.cacheFile(ctx, url)
        val part = ContentLoader.partFile(ctx, url)
        var etag: String? = null
        var total = -1L
        var lastTick = System.currentTimeMillis()
        var lastBytes = if (part.exists()) part.length() else 0L
        var speed = 0L
        var shortReads = 0

        while (isActive) {
            if (stopped) return@withContext Dl.Stopped
            val resumeFrom = if (part.exists()) part.length() else 0L
            var conn: HttpURLConnection? = null
            try {
                val c = (URL(LessonCache.canonical(url)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Cache-Control", "no-cache")
                    if (resumeFrom > 0L) {
                        setRequestProperty("Range", "bytes=$resumeFrom-")
                        val e = etag
                        if (e != null) setRequestProperty("If-Range", "\"$e\"")
                    }
                }
                conn = c
                val code = c.responseCode
                if (code == 404 || code == 410 || code == 403) {
                    runCatching { part.delete() }
                    return@withContext Dl.Missing
                }
                if (code == 416) {
                    runCatching { part.delete() }
                    continue
                }
                if (code !in 200..299) return@withContext Dl.Error("پاسخ نامعتبر از سرور ($code)")

                val append = code == 206 && resumeFrom > 0L
                if (!append) runCatching { part.delete() }
                etag = ContentLoader.normEtag(c.getHeaderField("ETag")) ?: etag
                total = if (code == 206) {
                    c.getHeaderField("Content-Range")?.substringAfterLast('/')?.trim()?.toLongOrNull() ?: -1L
                } else {
                    c.contentLengthLong
                }

                var done = if (append) resumeFrom else 0L
                val buffer = ByteArray(64 * 1024)
                var interrupted = false
                c.inputStream.use { input ->
                    FileOutputStream(part, append).use { out ->
                        while (isActive) {
                            if (stopped || paused) {
                                interrupted = true
                                break
                            }
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            done += n
                            val now = System.currentTimeMillis()
                            val dt = now - lastTick
                            if (dt >= 500) {
                                val inst = ((done - lastBytes) * 1000L) / dt
                                speed = if (speed == 0L) inst else (speed * 6 + inst * 4) / 10
                                lastTick = now
                                lastBytes = done
                            }
                            if (showProgress) {
                                _state.value = ContentState.Downloading(
                                    DownloadProgress(done, total, speed, false),
                                    isUpdate,
                                )
                            }
                        }
                        out.flush()
                        out.fd.sync()
                    }
                }
                if (stopped) return@withContext Dl.Stopped
                if (interrupted) {
                    // مکث: اتصال بسته شد؛ فایل ناقص می‌ماند و با Range ادامه پیدا می‌کند.
                    if (showProgress) {
                        _state.value = ContentState.Downloading(
                            DownloadProgress(part.length(), total, 0L, true),
                            isUpdate,
                        )
                    }
                    while (isActive && paused && !stopped) delay(150)
                    lastTick = System.currentTimeMillis()
                    lastBytes = part.length()
                    speed = 0L
                    continue
                }
                if (total >= 0L && part.length() != total) {
                    if (++shortReads > 2) return@withContext Dl.Error("دانلود ناقص ماند.")
                    continue
                }
                if (!ContentLoader.isValid(kind, part)) {
                    runCatching { part.delete() }
                    return@withContext Dl.Error("فایل دریافت‌شده معتبر نیست.")
                }
                if (!replace(part, target)) return@withContext Dl.Error("ذخیره‌ی فایل در کش ممکن نشد.")
                etag?.let { saveEtag(it) }
                return@withContext Dl.Done
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                // فایل ناقص برای ادامه نگه داشته می‌شود؛ «تلاش دوباره» با Range ادامه می‌دهد.
                return@withContext if (ContentLoader.isConnectivityError(t)) {
                    Dl.Offline
                } else {
                    Dl.Error(t.message ?: "خطای ناشناخته")
                }
            } finally {
                runCatching { conn?.disconnect() }
            }
        }
        Dl.Stopped
    }

    private fun replace(tmp: File, target: File): Boolean {
        if (!target.exists()) return tmp.renameTo(target)
        if (tmp.renameTo(target)) return true
        val backup = File(target.path + ".bak")
        runCatching { backup.delete() }
        if (!target.renameTo(backup)) return false
        if (tmp.renameTo(target)) {
            runCatching { backup.delete() }
            return true
        }
        backup.renameTo(target)
        return false
    }
}
