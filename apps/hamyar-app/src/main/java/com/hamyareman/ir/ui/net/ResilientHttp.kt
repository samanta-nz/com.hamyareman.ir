package com.hamyareman.ir.ui.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * وضعیتِ شبکه — یک‌جا و مشترک برای همهٔ دانلودها.
 *
 * چرا لازم است: روی موبایل، بینِ راهِ دانلود شبکه عوض می‌شود (وای‌فای → دیتا) یا
 * پروکسی/VPN جابه‌جا می‌شود؛ در آن لحظه سوکتِ قبلی بی‌اعتبار می‌شود و دانلود
 * می‌شکند. با این آبجکت، دانلودکننده می‌فهمد «الان اینترنت نیست» و به‌جای
 * سوزاندنِ تلاش‌ها، **صبر می‌کند تا اینترنت برگردد** و بعد از همان‌جا ادامه می‌دهد.
 */
object NetState {

    /** الان اینترنتِ واقعی داریم؟ */
    fun isOnline(ctx: Context): Boolean = runCatching {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)

    /**
     * تا برگشتنِ اینترنت صبر می‌کند (حداکثر [timeoutMs]).
     * @return true اگر اینترنت آمد (یا همین حالا بود).
     */
    suspend fun awaitOnline(ctx: Context, timeoutMs: Long = 120_000L): Boolean {
        if (isOnline(ctx)) return true
        val cm = runCatching {
            ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }.getOrNull() ?: return false
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && cont.isActive) {
                            cont.resume(true)
                        }
                    }
                }
                val registered = runCatching {
                    cm.registerDefaultNetworkCallback(callback)
                }.isSuccess
                if (!registered) {
                    // بدترین حالت: ثبت نشد → با یک تأخیرِ کوتاه، وضعیت را گزارش کن.
                    if (cont.isActive) cont.resume(false)
                }
                cont.invokeOnCancellation { runCatching { cm.unregisterNetworkCallback(callback) } }
            }
        } ?: false
    }
}

/**
 * نسخهٔ بلوکه‌ایِ [NetState.awaitOnline] — برای کدهای دانلودِ ساده که سواسپند نیستند
 * (روی نخِ پس‌زمینه اجرا می‌شوند).
 */
fun NetState.awaitOnlineBlocking(ctx: Context, timeoutMs: Long = 120_000L): Boolean {
    if (isOnline(ctx)) return true
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (isOnline(ctx)) return true
        runCatching { Thread.sleep(700) }
    }
    return isOnline(ctx)
}

/**
 * اتصالِ «مقاوم» — همان چیزی که دانلود را وقتی شبکه/پروکسی عوض می‌شود زنده نگه می‌دارد.
 *
 * هر تلاش یک اتصالِ تازه می‌سازد (بدونِ کش، با `Connection: close`) تا پروکسی و
 * مسیرِ شبکه از نو خوانده شود؛ اگر اتصال بشکند یا پاسخِ موقتِ سرور بیاید،
 * با عقب‌نشینیِ پرشونده دوباره تلاش می‌کند و اگر اینترنت نبود، اول منتظرِ
 * برگشتنش می‌ماند.
 */
object ResilientHttp {

    const val ATTEMPTS = 6

    /** کدهایی که ارزشِ تلاشِ دوباره دارند (بقیه قطعی‌اند). */
    private fun retriable(code: Int): Boolean =
        code == 408 || code == 425 || code == 429 || code in 500..599

    /**
     * یک اتصالِ آماده برمی‌گرداند. خطاهای گذرا خودکار تلاشِ دوباره می‌شوند؛
     * خطاهای قطعی (۴۰۴/۴۱۶/…) همین‌طور برگردانده می‌شوند تا فراخوان تصمیم بگیرد.
     */
    fun open(
        url: String,
        range: String? = null,
        connectMs: Int = 12_000,
        readMs: Int = 25_000,
        attempts: Int = ATTEMPTS,
    ): HttpURLConnection {
        var last: Throwable? = null
        for (i in 0 until attempts) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = connectMs
                    readTimeout = readMs
                    instanceFollowRedirects = true
                    useCaches = false
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Connection", "close")
                    if (range != null) setRequestProperty("Range", range)
                }
                conn.connect()
                val code = conn.responseCode
                if (code in 200..299) return conn
                if (!retriable(code)) return conn
                last = IOException("HTTP $code")
                runCatching { conn.disconnect() }
            } catch (t: Throwable) {
                last = t
                runCatching { conn?.disconnect() }
            }
            backoff(i)
        }
        throw IOException("اتصال برقرار نشد (${last?.message ?: "دلیل نامعلوم"})")
    }

    /** مثلِ [open]، ولی اگر اینترنت نباشد اول تا برگشتنش صبر می‌کند. */
    suspend fun openOnline(
        ctx: Context,
        url: String,
        range: String? = null,
        connectMs: Int = 12_000,
        readMs: Int = 25_000,
        attempts: Int = ATTEMPTS,
    ): HttpURLConnection {
        if (!NetState.isOnline(ctx)) NetState.awaitOnline(ctx)
        return open(url, range, connectMs, readMs, attempts)
    }

    /** عقب‌نشینیِ پرشونده: ۰٫۳s، ۰٫۶s، ۱٫۲s … حداکثر ۴s. */
    fun backoff(i: Int) {
        runCatching { Thread.sleep((300L shl i.coerceAtMost(4)).coerceAtMost(4000L)) }
    }
}
