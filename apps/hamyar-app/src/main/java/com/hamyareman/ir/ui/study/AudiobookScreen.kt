package com.hamyareman.ir.ui.study

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import com.hamyareman.ir.platform.feature.hearttoheart.MediaFiles
import com.hamyareman.ir.platform.feature.playback.PlaybackController
import com.hamyareman.ir.LocalAppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * پخش‌کننده‌ی کتاب صوتی — با پخش در پس‌زمینه.
 *
 * از Media3 (`:feature-playback`) استفاده می‌کند، پس:
 *  ۱) با بستن اپ، خاموش‌شدن صفحه یا رفتن به اپ دیگر، **صدا قطع نمی‌شود** و اعلان
 *     سیستمی با کنترل پخش/توقف و جابه‌جایی ساخته می‌شود؛
 *  ۲) AudioFocus و «کشیدن هدفون = توقف» به خود Media3 سپرده شده؛
 *  ۳) **بوکمارک** و **ادامه از آخرین موقعیت** (حتی بعد از بستن اپ) سر جایشان هستند؛
 *  ۴) **تایمر خواب** پخش را متوقف و موقعیت را ذخیره می‌کند؛
 *  ۵) سرعت پخش (۰٫۷۵× تا ۱٫۵×) برای کتاب صوتی اضافه شده.
 *
 * دو نکته‌ی صادقانه:
 *  - `release()` در `onDispose` فقط اتصال این صفحه را قطع می‌کند؛ پخش ادامه دارد.
 *  - تایمر خواب در همین صفحه زمان‌بندی می‌شود؛ اگر فرآیند اپ کامل کشته شود، تایمر هم
 *    می‌رود (پخش هم در آن حالت توسط سیستم متوقف می‌شود).
 */
internal data class AudioBookmark(val label: String, val positionMs: Int, val createdAtMs: Long)

private const val KEY_URI = "audiobook_uri"
private const val KEY_TITLE = "audiobook_title"
private const val KEY_POSITION = "audiobook_position"
private const val KEY_BOOKMARKS = "audiobook_bookmarks"
private const val KEY_SPEED = "audiobook_speed"

private val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f)

private fun readBookmarks(store: LocalStore): List<AudioBookmark> = runCatching {
    val array = JSONArray(store.getString(KEY_BOOKMARKS, "[]"))
    buildList {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            add(AudioBookmark(o.optString("label"), o.optInt("positionMs"), o.optLong("createdAtMs")))
        }
    }
}.getOrDefault(emptyList())

private fun writeBookmarks(store: LocalStore, bookmarks: List<AudioBookmark>) {
    val array = JSONArray()
    bookmarks.forEach { b ->
        array.put(
            JSONObject()
                .put("label", b.label).put("positionMs", b.positionMs).put("createdAtMs", b.createdAtMs),
        )
    }
    store.putString(KEY_BOOKMARKS, array.toString())
}

private fun mmss(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    // Locale.US عمدی است: با locale فارسی، `format` رقم فارسی می‌دهد و بعد
    // `toPersianDigits` روی آن بی‌اثر می‌شود (جداساز اعشار هم قاطی می‌کند).
    val latin = String.format(Locale.US, "%d:%02d", safe / 60_000, (safe % 60_000) / 1_000)
    return toPersianDigits(latin)
}

private fun speedLabel(speed: Float): String = when (speed) {
    0.75f -> "۰٫۷۵×"
    1f -> "۱×"
    1.25f -> "۱٫۲۵×"
    1.5f -> "۱٫۵×"
    else -> speed.toString() + "×"
}

@Composable
fun AudiobookScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = LocalAppContainer.current.store
    val playback = remember { PlaybackController(context) }
    val state by playback.state.collectAsState()
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(store.getString(KEY_TITLE)) }
    var positionMs by remember { mutableLongStateOf(store.getInt(KEY_POSITION).toLong()) }
    var scrubMs by remember { mutableLongStateOf(-1L) }
    var error by remember { mutableStateOf<String?>(null) }
    var bookmarks by remember { mutableStateOf(readBookmarks(store)) }
    var sleepEndsAt by remember { mutableLongStateOf(0L) }
    var sleepRemainingSeconds by remember { mutableLongStateOf(0L) }

    val latestPosition = rememberUpdatedState(if (scrubMs >= 0L) scrubMs else positionMs)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            // مجوز پایدار می‌گیریم تا سرویس هم بتواند بعداً همان فایل را بخواند.
            runCatching {
                context.contentResolver.takePersistableUriPermission(picked, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = MediaFiles.displayName(context, picked) ?: "کتاب صوتی"
            store.putString(KEY_URI, picked.toString())
            store.putString(KEY_TITLE, name)
            store.putInt(KEY_POSITION, 0)
            title = name
            positionMs = 0L
            error = null
            // اگر اتصال هنوز برقرار نشده (کاربر بلافاصله بعد از بازکردن صفحه فایل گرفت)،
            // اول وصل شو بعد فایل را بده؛ وگرنه setMedia بی‌اثر می‌ماند.
            scope.launch {
                if (!playback.isConnected) playback.connect()
                playback.setMedia(picked.toString(), name, 0L)
            }
        }
    }

    // اتصال به سرویس + بازگرداندن آخرین کتاب و آخرین موقعیت
    LaunchedEffect(Unit) {
        if (!playback.connect()) {
            error = "سرویس پخش بالا نیامد؛ یک بار دیگر امتحان کن."
            return@LaunchedEffect
        }
        val savedSpeed = store.getFloat(KEY_SPEED, 1f)
        if (savedSpeed != 1f) playback.setSpeed(savedSpeed)
        val savedUri = store.getString(KEY_URI)
        if (savedUri.isNotBlank() && !playback.state.value.hasMedia) {
            playback.setMedia(
                savedUri,
                store.getString(KEY_TITLE).ifBlank { "کتاب صوتی" },
                store.getInt(KEY_POSITION).toLong(),
            )
        }
    }

    // موقعیت را از سرویس بخوان: رویدادهای Player برای نوار پیشرفت کافی نیستند.
    LaunchedEffect(state.connected, state.playing) {
        if (!state.connected) return@LaunchedEffect
        while (true) {
            positionMs = playback.positionMs
            if (!state.playing) break
            delay(500L)
        }
    }

    // ذخیره‌ی دوره‌ای موقعیت تا اگر اپ در پس‌زمینه کشته شد، از همین‌جا ادامه دهی.
    LaunchedEffect(state.playing) {
        if (!state.playing) {
            store.putInt(KEY_POSITION, latestPosition.value.toInt())
            return@LaunchedEffect
        }
        while (state.playing) {
            delay(5_000L)
            store.putInt(KEY_POSITION, playback.positionMs.toInt())
        }
    }

    // تایمر خواب
    LaunchedEffect(sleepEndsAt) {
        if (sleepEndsAt <= 0L) return@LaunchedEffect
        while (System.currentTimeMillis() < sleepEndsAt) {
            sleepRemainingSeconds = (sleepEndsAt - System.currentTimeMillis()) / 1_000L
            delay(1_000L)
        }
        sleepEndsAt = 0L
        sleepRemainingSeconds = 0L
        playback.pause()
        store.putInt(KEY_POSITION, playback.positionMs.toInt())
    }

    DisposableEffect(Unit) {
        // قاعده‌ی سراسری صوت: صفحه‌ی پخش باز = پخش مجاز؛ بسته = مکث فوری.
        com.hamyareman.ir.platform.feature.playback.TeachGate.enter()
        onDispose {
            store.putInt(KEY_POSITION, playback.positionMs.toInt())
            runCatching { playback.stop() }
            com.hamyareman.ir.platform.feature.playback.TeachGate.exit()
            playback.release()
        }
    }

    // هرگز در پس‌زمینه: هوم/مینیمایز/جابجایی اپ/قفل گوشی → مکث؛ برگشت → ادامه از همان‌جا.
    com.hamyareman.ir.ui.study.PauseOnStopEffect(pause = { runCatching { playback.pause() } }, stop = { runCatching { playback.stop() } })

    fun seekTo(ms: Long) {
        val target = ms.coerceIn(0L, (if (state.durationMs > 0L) state.durationMs else ms).coerceAtLeast(0L))
        playback.seekTo(target)
        positionMs = target
        scrubMs = -1L
        store.putInt(KEY_POSITION, target.toInt())
    }

    val duration = state.durationMs.coerceAtLeast(0L)
    val shownPosition = if (scrubMs >= 0L) scrubMs else positionMs.coerceAtLeast(0L)
    // تا وقتی duration معلوم نشده (صفر است)، اسلایدر را روی موقعیت فعلی باز می‌کنیم
    // تا مقدار بیرون از بازه نرود.
    val sliderMax = maxOf(duration, shownPosition, 1L)
    val ready = state.connected && state.hasMedia

    Column(Modifier.fillMaxSize()) {
        AppTopBar("کتاب صوتی", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (title.isBlank()) "هنوز فایلی انتخاب نکردی" else title,
                style = MaterialTheme.typography.titleMedium,
            )
            PrimaryButton("انتخاب فایل صوتی از گوشی") { picker.launch(arrayOf("audio/*")) }
            (error ?: state.error)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            if (ready) {
                Slider(
                    value = shownPosition.toFloat(),
                    onValueChange = { scrubMs = it.toLong() },
                    onValueChangeFinished = { seekTo(if (scrubMs >= 0L) scrubMs else positionMs) },
                    valueRange = 0f..sliderMax.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mmss(shownPosition), style = MaterialTheme.typography.bodySmall)
                    Text(mmss(duration), style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { seekTo(positionMs - 30_000L) }) { Text("۳۰ ثانیه عقب") }
                    TextButton(onClick = { if (state.playing) playback.pause() else playback.play() }) {
                        Text(if (state.playing) "توقف" else "پخش")
                    }
                    TextButton(onClick = { seekTo(positionMs + 30_000L) }) { Text("۳۰ ثانیه جلو") }
                }

                Text("سرعت پخش", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SPEEDS.forEach { speed ->
                        TextButton(
                            onClick = {
                                playback.setSpeed(speed)
                                store.putFloat(KEY_SPEED, speed)
                            },
                        ) {
                            Text(
                                speedLabel(speed),
                                color = if (speed == state.speed) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("بوکمارک‌ها", style = MaterialTheme.typography.titleSmall)
                PrimaryButton("بوکمارک همین‌جا (${mmss(positionMs)})") {
                    val mark = AudioBookmark(
                        label = "${mmss(positionMs)} — ${JalaliDate.formatFa(System.currentTimeMillis())}",
                        positionMs = positionMs.toInt(),
                        createdAtMs = System.currentTimeMillis(),
                    )
                    bookmarks = listOf(mark) + bookmarks
                    writeBookmarks(store, bookmarks)
                }
                if (bookmarks.isEmpty()) {
                    Text("بوکمارکی نداری؛ هر جای جذاب کتاب را نگه دار.", style = MaterialTheme.typography.bodySmall)
                }
                bookmarks.forEach { mark ->
                    SectionCard(
                        title = mark.label,
                        body = "برای پریدن به این قسمت لمس کن",
                        onClick = { seekTo(mark.positionMs.toLong()) },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            bookmarks = bookmarks.filterNot { it.createdAtMs == mark.createdAtMs }
                            writeBookmarks(store, bookmarks)
                        }) { Text("حذف بوکمارک") }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("تایمر خواب", style = MaterialTheme.typography.titleSmall)
            if (sleepEndsAt > 0L) {
                SectionCard(
                    title = "تا خاموش‌شدن پخش: ${mmss(sleepRemainingSeconds * 1_000L)}",
                    body = "وقتی تمام شود، پخش می‌ایستد و موقعیتت ذخیره می‌شود.",
                ) { }
                PrimaryButton("لغو تایمر") { sleepEndsAt = 0L; sleepRemainingSeconds = 0L }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45).forEach { minutes ->
                        TextButton(onClick = {
                            sleepEndsAt = System.currentTimeMillis() + minutes * 60_000L
                            sleepRemainingSeconds = minutes * 60L
                            if (!state.playing) playback.play()
                        }) { Text(toPersianDigits(minutes.toString()) + " دقیقه") }
                    }
                }
            }

            SectionCard(
                title = "پخش در پس‌زمینه",
                body = "می‌توانی صفحه را ببندی یا از اپ بیرون بروی؛ پخش ادامه دارد و از اعلان " +
                    "سیستم کنترل می‌شود. هدفون که کشیده شود، پخش خودش می‌ایستد.",
            ) { }
            SectionCard(
                title = "ادامه از آخرین موقعیت",
                body = "دفعه‌ی بعد از ${mmss(positionMs)} ادامه می‌دهی — حتی اگر اپ را ببندی.",
            ) { }
        }
    }
}
