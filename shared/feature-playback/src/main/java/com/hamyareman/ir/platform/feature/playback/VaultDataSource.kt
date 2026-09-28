package com.hamyareman.ir.platform.feature.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.IOException

/**
 * پخشِ مستقیم از «گاوصندوق رسانه» با URI ساده‌ی `vault://<cacheKey>`.
 *
 * نسخه‌ی اولِ این مسیر کل فایل را در `open()` رمزگشایی و در حافظه می‌ریخت؛
 * برای فایلِ ~۲۵ مگابایتیِ تدریس این یعنی چند ثانیه بلوکه‌شدنِ لودرِ ExoPlayer —
 * پلیر در STATE_BUFFERING می‌ماند و واچ‌داگِ صفحه آن را «پخش محلی شروع نشد»
 * تلقی می‌کرد و بی‌دلیل به سرور سوییچ می‌کرد (و آفلاین، هیچ). حالا خوانش
 * **جسته‌گریخته و رمزگشاییِ تنبل (lazy)** است: بازکردن فایل آنی است و هر بلاک
 * درست لحظه‌ی نیاز رمزگشایی می‌شود — مثلِ یک فایلِ معمولی روی دیسک.
 *
 * لایه‌ی اپ (که `MediaVault` را می‌شناسد) فقط یک کارخانه‌ی جریان به
 * [VaultSourceHooks] می‌دهد؛ ماژولِ پخش به ماژولِ اپ وابسته نمی‌ماند.
 */

/** جریانِ رمزگشایی‌شده‌ی یک فایلِ گاوصندوق — خوانشِ تصادفی روی داده‌یPlain. */
interface VaultStream {
    /** طولِ داده‌یِ رمزگشایی‌شده (بایت). */
    val size: Long

    /**
     * حداقلِ [len] بایت از موقعیتِ [pos] را در dst[off] می‌نویسد و تعدادِ
     * بایت‌هایِ نوشته‌شده را برمی‌گرداند؛ اگر تمام شده ≤۰ برگردان.
     */
    fun read(pos: Long, dst: ByteArray, off: Int, len: Int): Int

    fun close()
}

object VaultSourceHooks {
    /** cacheKey → جریانِ بازِ گاوصندوق؛ null یعنی فایلِ معتبر موجود نیست. */
    @Volatile
    var open: ((String) -> VaultStream?)? = null
}

internal fun vaultKeyOf(uri: Uri): String =
    (uri.schemeSpecificPart ?: uri.toString()).removePrefix("//")

/** کارخانه‌ی منبعی که `vault://` را خودش و بقیه را به [default] می‌سپارد. */
@UnstableApi
fun vaultAwareDataSourceFactory(default: DataSource.Factory): DataSource.Factory =
    DataSource.Factory {
        object : DataSource {
            private var active: DataSource? = null

            override fun addTransferListener(transferListener: TransferListener) = Unit

            override fun open(dataSpec: DataSpec): Long {
                active = if (dataSpec.uri.scheme == "vault") {
                    VaultDataSource()
                } else {
                    default.createDataSource()
                }
                return active!!.open(dataSpec)
            }

            override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int =
                active!!.read(buffer, offset, readLength)

            override fun getUri(): Uri? = active?.uri

            override fun close() {
                runCatching { active?.close() }
                active = null
            }
        }
    }

/** منبعِ خوانشِ جسته‌گریخته روی [VaultStream] — بدونِ HTTP، بدونِ کلِ فایل در حافظه. */
@UnstableApi
private class VaultDataSource : DataSource {

    private var stream: VaultStream? = null
    private var uri: Uri? = null
    private var pos = 0L
    private var end = 0L

    override fun addTransferListener(transferListener: TransferListener) = Unit

    override fun open(dataSpec: DataSpec): Long {
        val key = vaultKeyOf(dataSpec.uri)
        val s = VaultSourceHooks.open?.invoke(key)
            ?: throw IOException("فایلِ گاوصندوق در دسترس نیست: $key")
        if (!s.openReady()) {
            runCatching { s.close() }
            throw IOException("فایلِ گاوصندوق خوانده نشد: $key")
        }
        stream = s
        uri = dataSpec.uri
        pos = dataSpec.position.coerceIn(0L, maxOf(0L, s.size))
        val remaining = (s.size - pos).coerceAtLeast(0L)
        end = pos + if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            remaining
        } else {
            dataSpec.length.coerceAtMost(remaining)
        }
        return (end - pos).coerceAtLeast(0L)
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        val s = stream ?: return C.RESULT_END_OF_INPUT
        if (pos >= end) return C.RESULT_END_OF_INPUT
        val want = minOf(readLength.toLong(), end - pos).toInt()
        val got = runCatching { s.read(pos, buffer, offset, want) }.getOrDefault(-1)
        if (got <= 0) return C.RESULT_END_OF_INPUT
        pos += got
        return got
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        runCatching { stream?.close() }
        stream = null
        uri = null
        pos = 0L
        end = 0L
    }

    /** بازگشتِ true یعنی جریان آماده است (کنترلی برای فایل‌های نیمه‌کاره). */
    private fun VaultStream.openReady(): Boolean = size > 0L
}

/** سازنده‌ی MediaSourceِ آگاه به گاوصندوق — برای پلیرهایی که خودشان ExoPlayer می‌سازند. */
@UnstableApi
fun vaultAwareMediaSourceFactory(context: Context): DefaultMediaSourceFactory =
    DefaultMediaSourceFactory(vaultAwareDataSourceFactory(DefaultDataSource.Factory(context)))
