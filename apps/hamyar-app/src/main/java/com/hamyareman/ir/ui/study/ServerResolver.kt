package com.hamyareman.ir.ui.study

import com.hamyareman.ir.ui.content.ContentCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * انتخاب آدرس فایل بین دو سرور (نشانی دوگانه):
 *  - خارجی: باکت Appwrite (همان viewUrl موجود)
 *  - ایرانی: سراسری آروان — کلید catalog.json
 * طبق «تنظیمات سرور» (FASTEST / EXTERNAL / INTERNAL).
 */
object ServerResolver {

    const val ARVAN_PUBLIC = "https://hamyar-e-man.s3.ir-thr-at1.arvanstorage.ir"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var probeStarted = false
    @Volatile private var internalOkInMemory: Boolean? = null

    fun external(fileId: String): String = StudyMedia.externalUrl(fileId)

    fun internal(key: String): String =
        ARVAN_PUBLIC + "/" + key.split("/").joinToString("/") {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }

    /**
     * HEAD کوتاه به یک شیء نمونهٔ داخلی؛ نتیجه در حافظهٔ این اجرا و ServerPrefs کش می‌شود.
     * برای نمایش وضعیت در «تنظیمات سرور» هم استفاده می‌شود.
     */
    suspend fun probeInternal(): Boolean = withContext(Dispatchers.IO) {
        val key = ContentCatalog.sampleHtmlKey()
        val ok = if (key == null) false else runCatching {
            val conn = (URL(internal(key)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 1_500
                readTimeout = 1_500
                requestMethod = "HEAD"
            }
            try {
                conn.responseCode in 200..299
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
        internalOkInMemory = ok
        ServerPrefs.lastProbeOk = ok
        ok
    }

    /** کاوشِ بی‌صدا در آغاز اجرا — بدون انتظار. */
    fun probeAsync() {
        if (probeStarted) return
        synchronized(this) {
            if (probeStarted) return
            probeStarted = true
        }
        scope.launch { runCatching { probeInternal() } }
    }

    /** آدرس نهایی یک فایل. arvanKey = کلید catalog (برای فایل‌های بدون کلید: خارجی). */
    fun pick(fileId: String, arvanKey: String?): String = when (ServerPrefs.mode) {
        ServerPrefs.Mode.EXTERNAL -> external(fileId)
        ServerPrefs.Mode.INTERNAL -> arvanKey?.let { internal(it) } ?: external(fileId)
        ServerPrefs.Mode.FASTEST -> {
            val inner = internalOkInMemory
                ?: ServerPrefs.lastProbeOk
                ?: false
            if (inner && arvanKey != null) internal(arvanKey) else external(fileId)
        }
    }
}
