package com.hamyareman.ir.ui.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.net.NetState
import com.hamyareman.ir.ui.net.ResilientHttp
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** پیشرفتِ دانلودِ آپدیت: رفته، کل و سرعتِ لحظه‌ای. */
data class ApkProgress(val bytes: Long, val total: Long, val speedBps: Long) {
    val percent: Int get() = if (total > 0) ((bytes * 100) / total).toInt().coerceIn(0, 100) else 0

    /** «۲۳٫۴ از ۵۰٫۶ مگابایت» */
    val sizeText: String
        get() = if (total > 0) {
            faMb(bytes) + " از " + faMb(total) + " مگابایت"
        } else {
            faMb(bytes) + " مگابایت"
        }

    /** «۱٫۸ مگابایت/ثانیه» */
    val speedText: String get() = if (speedBps > 0) faMb(speedBps) + " مگابایت/ثانیه" else "—"
}

private fun faMb(bytes: Long): String =
    toPersianDigits(String.format(Locale.US, "%.1f", bytes / 1048576.0))

/**
 * دانلود و نصبِ درون‌برنامه‌ایِ APK — نسخهٔ مقاوم.
 *
 * تفاوت‌ها با نسخهٔ قبلی (که «نصب نمی‌شد»):
 *  ۱) دانلود با موتورِ خودِ اپ انجام می‌شود (نه DownloadManager) تا بشود درصد و
 *     سرعت را نشان داد و اگر شبکه/پروکسی وسطِ راه عوض شد، از **همان‌جا** ادامه داد.
 *  ۲) فایل داخلِ حافظهٔ خودِ اپ (`filesDir/updates`) می‌نشیند، پس FileProvider
 *     همیشه اجازهٔ خواندنش را دارد.
 *  ۳) نصب سه مسیر دارد: نشستِ `PackageInstaller` (مطمئن‌ترین راه روی اندروید
 *     جدید) → اینتنتِ FileProvider → `ACTION_INSTALL_PACKAGE`.
 *  ۴) اگر مجوزِ «نصب از منابعِ ناشناس» داده نشده باشد، مستقیم صفحهٔ تنظیمات همان
 *     مجوز باز می‌شود؛ قبلاً فقط یک پیامِ Toast بود و کاربر گیر می‌کرد.
 */
object ApkUpdate {

    const val DIR = "updates"
    const val FILE_NAME = "hamyar-update.apk"
    const val MIME_APK = "application/vnd.android.package-archive"
    const val ACTION_RESULT = "com.hamyareman.ir.INSTALL_RESULT"
    const val EXTRA_STATUS = "status"

    fun file(ctx: Context): File = File(File(ctx.filesDir, DIR).apply { mkdirs() }, FILE_NAME)

    private fun part(ctx: Context): File = File(file(ctx).parentFile, "$FILE_NAME.part")

    /** فایلِ دانلودشده هست و حجمِ معقولی دارد؟ */
    fun isReady(ctx: Context): Boolean = runCatching {
        val f = file(ctx)
        f.exists() && f.length() > 1024L * 100L
    }.getOrDefault(false)

    fun clear(ctx: Context) {
        runCatching { file(ctx).delete() }
        runCatching { part(ctx).delete() }
    }

    /**
     * دانلودِ APK با ادامه‌دادن از محلِ قطع.
     *
     * تا وقتی ناقص است تلاش می‌کند: اگر اینترنت برود، منتظرِ برگشتنش می‌ماند،
     * بعد با `Range: bytes=<مانده>-` از همان‌جا ادامه می‌دهد — پس تغییرِ شبکه،
     * پروکسی یا VPN دانلود را از صفر شروع نمی‌کند.
     */
    suspend fun download(
        ctx: Context,
        urls: List<String>,
        onProgress: (ApkProgress) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        val origins = urls.filter(String::isNotBlank).distinct()
        if (origins.isEmpty()) return@withContext null
        val target = file(ctx)
        val tmp = part(ctx)
        runCatching { target.delete() }

        var total = 0L
        var start = if (tmp.exists()) tmp.length() else 0L
        var speed = 0L
        var lastReport = 0L
        var windowBytes = 0L
        var windowStart = System.currentTimeMillis()
        var completed = false

        origins@ for (url in origins) {
            var attempts = 0
            while (attempts < 7) {
                if (total > 0 && start >= total) {
                    completed = true
                    break@origins
                }
                var conn: java.net.HttpURLConnection? = null
                try {
                    val range = if (start > 0) "bytes=$start-" else null
                    val active = ResilientHttp.openOnline(ctx, url, range, connectMs = 15_000, readMs = 25_000)
                    conn = active
                    val code = active.responseCode
                    if (code !in 200..299) error("HTTP $code")
                    // origin دوم می‌تواند Range را نادیده بگیرد؛ در آن حالت فایل
                    // نیمه‌کاره حذف و همان APK از صفر نوشته می‌شود.
                    if (start > 0 && code == 200) {
                        tmp.delete()
                        start = 0
                    }
                    val cl = active.contentLengthLong
                    if (cl > 0) total = if (code == 206) start + cl else cl

                    RandomAccessFile(tmp, "rw").use { raf ->
                        raf.seek(start)
                        raf.setLength(start)
                        active.inputStream.use { input ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                raf.write(buf, 0, n)
                                start += n
                                windowBytes += n
                                val now = System.currentTimeMillis()
                                val dt = now - windowStart
                                if (dt >= 400) {
                                    speed = windowBytes * 1000L / dt.coerceAtLeast(1)
                                    windowBytes = 0
                                    windowStart = now
                                }
                                if (now - lastReport >= 300 || (total in 1..start)) {
                                    lastReport = now
                                    runCatching { onProgress(ApkProgress(start, total, speed)) }
                                }
                            }
                        }
                    }
                    if (total <= 0 || start >= total) {
                        completed = true
                        break@origins
                    }
                    attempts++
                    delay(400)
                } catch (error: Throwable) {
                    // خطای قطعی 4xx (از جمله سهمیهٔ 402) فوراً به origin بعدی
                    // می‌رود؛ خطای گذرا روی همان origin تا هفت بار retry می‌شود.
                    attempts = if (error.message.orEmpty().startsWith("HTTP 4")) 7 else attempts + 1
                    if (!NetState.isOnline(ctx)) NetState.awaitOnline(ctx)
                    if (attempts < 7) delay((400L * attempts).coerceAtMost(4000L))
                } finally {
                    runCatching { conn?.disconnect() }
                }
            }
        }

        if (!completed || (total > 0 && start < total) || start <= 1024L * 100L) {
            return@withContext null
        }
        runCatching { onProgress(ApkProgress(start, if (total > 0) total else start, speed)) }
        if (!tmp.renameTo(target)) {
            runCatching {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }.getOrElse { return@withContext null }
        }
        target
    }

    suspend fun download(
        ctx: Context,
        url: String,
        onProgress: (ApkProgress) -> Unit,
    ): File? = download(ctx, listOf(url), onProgress)

    /** sha256 یک فایل. */
    fun sha256Of(f: File): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ins ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    /**
     * بررسیِ اصالتِ فایلِ دانلودشده با `sha256`ِ اعلام‌شدهٔ سرور.
     * اگر سرور هش نداده باشد `true` برمی‌گردد: در آن حالت تنها نگهبان، امضای
     * APK است که خودِ اندروید پیش از نصب بررسی می‌کند.
     */
    fun verify(ctx: Context, expected: String): Boolean {
        if (expected.isBlank()) return true
        val actual = sha256Of(file(ctx)) ?: return false
        return actual.equals(expected.trim(), ignoreCase = true)
    }

    private const val INSTALL_PREF = "hamyar_update_install"
    private const val KEY_PENDING_VERSION = "pending_version"

    @Suppress("DEPRECATION")
    private fun archiveInfo(ctx: Context) = if (Build.VERSION.SDK_INT >= 33) {
        ctx.packageManager.getPackageArchiveInfo(
            file(ctx).absolutePath,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
        )
    } else {
        ctx.packageManager.getPackageArchiveInfo(file(ctx).absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
    }

    @Suppress("DEPRECATION")
    private fun installedInfo(ctx: Context) = if (Build.VERSION.SDK_INT >= 33) {
        ctx.packageManager.getPackageInfo(
            ctx.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
        )
    } else {
        ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    }

    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: android.content.pm.PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

    private fun signerSha256(info: android.content.pm.PackageInfo): String? = runCatching {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION") info.signatures
        }.orEmpty()
        val cert = signatures.firstOrNull()?.toByteArray() ?: return@runCatching null
        MessageDigest.getInstance("SHA-256").digest(cert).joinToString("") { "%02x".format(it) }
    }.getOrNull()

    /**
     * پیش از بازکردن نصب‌کننده سه نگهبان قطعی دارد: package همان پایه، versionCode
     * دقیق payload و همان گواهی امضای نسخهٔ نصب‌شده. null یعنی فایل مجاز است.
     */
    fun identityError(ctx: Context, info: UpdateInfo): String? {
        val archive = archiveInfo(ctx) ?: return "فایل، APK معتبر اندروید نیست."
        if (archive.packageName != ctx.packageName) {
            return "این فایل برای یک پایهٔ دیگر است و روی این برنامه نصب نمی‌شود."
        }
        if (versionCodeOf(archive) != info.latest.toLong()) {
            return "کد نسخهٔ فایل با نسخهٔ اعلام‌شدهٔ سرور یکسان نیست."
        }
        val archiveSigner = signerSha256(archive) ?: return "امضای فایل قابل بررسی نیست."
        val installedSigner = runCatching { signerSha256(installedInfo(ctx)) }.getOrNull()
            ?: return "امضای نسخهٔ نصب‌شده قابل بررسی نیست."
        if (!archiveSigner.equals(installedSigner, ignoreCase = true)) {
            return "کلید امضای فایل با نسخهٔ نصب‌شده یکسان نیست."
        }
        val declared = info.signingSha256.filter { it.isLetterOrDigit() }.lowercase()
        if (declared.isNotBlank() && declared != archiveSigner.lowercase()) {
            return "اثر انگشت امضای فایل با اعلام سرور یکسان نیست."
        }
        return null
    }

    private fun markPendingInstall(ctx: Context, version: Int) {
        ctx.getSharedPreferences(INSTALL_PREF, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PENDING_VERSION, version).apply()
    }

    private fun clearPendingInstall(ctx: Context) {
        ctx.getSharedPreferences(INSTALL_PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * اجرای نسخهٔ تازه ممکن است process قبلی را قبل از callback ببندد؛ در شروع
     * نسخهٔ جدید نیز فایل APK و part حتماً پاک می‌شوند.
     */
    fun cleanupAfterSuccessfulInstall(ctx: Context) {
        val pending = ctx.getSharedPreferences(INSTALL_PREF, Context.MODE_PRIVATE)
            .getInt(KEY_PENDING_VERSION, 0)
        val current = runCatching { versionCodeOf(installedInfo(ctx)).toInt() }.getOrDefault(0)
        if (pending > 0 && current >= pending) {
            clear(ctx)
            clearPendingInstall(ctx)
        }
    }

    fun onInstallSucceeded(ctx: Context) {
        clear(ctx)
        clearPendingInstall(ctx)
    }

    /** آیا اجازهٔ «نصب از منابع ناشناس» برای همین اپ داده شده؟ */
    fun canInstall(ctx: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }.getOrDefault(true)

    /** باز کردن صفحهٔ تنظیماتِ همان مجوز (برای وقتی کاربر گفت «مجوز داده‌ام» ولی نشده). */
    fun openInstallPermission(ctx: Context): Boolean = runCatching {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${ctx.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        true
    }.getOrElse {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

    /** نصب با نشستِ PackageInstaller — روی اندرویدهای تازه مطمئن‌ترین راه. */
    private fun installViaSession(ctx: Context): Boolean = runCatching {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            file(ctx).inputStream().use { input ->
                session.openWrite("hamyar", 0, file(ctx).length()).use { out ->
                    input.copyTo(out, 64 * 1024)
                    session.fsync(out)
                }
            }
            val result = Intent(ctx, UpdateInstallReceiver::class.java)
                .setAction(ACTION_RESULT)
                .putExtra("sessionId", sessionId)
            val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.app.PendingIntent.FLAG_MUTABLE else 0
            val pending = android.app.PendingIntent.getBroadcast(ctx, sessionId, result, flags)
            session.commit(pending.intentSender)
        }
        true
    }.getOrDefault(false)

    /** نصب با اینتنتِ FileProvider (راهِ کلاسیک). */
    private fun installViaIntent(ctx: Context): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file(ctx))
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        val open = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, MIME_APK)
            .addFlags(flags)
            .apply { clipData = ClipData.newRawUri("apk", uri) }
        runCatching { ctx.startActivity(open); return true }
        val legacy = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setData(uri)
            .addFlags(flags)
        runCatching { ctx.startActivity(legacy); return true }
        false
    }.getOrDefault(false)

    /**
     * اجرای نصب‌کنندهٔ سیستم، به ترتیبِ مطمئن‌ترین راه‌ها.
     * @return false یعنی هیچ راهی باز نشد (فراخوان باید راهِ جایگزین نشان دهد).
     */
    fun install(ctx: Context, info: UpdateInfo): Boolean {
        val f = file(ctx)
        if (!f.exists() || f.length() <= 0L) return false
        if (identityError(ctx, info) != null) return false
        markPendingInstall(ctx, info.latest)
        if (installViaIntent(ctx)) return true
        if (installViaSession(ctx)) return true
        clearPendingInstall(ctx)
        return false
    }

    /** خروجیِ کوتاه برای کاربر وقتی نصب‌کننده باز نمی‌شود. */
    fun explain(ctx: Context) {
        runCatching {
            Toast.makeText(
                ctx,
                "برای نصب، اجازهٔ «نصب برنامه‌های ناشناس» را برای همیار من روشن کن.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}
