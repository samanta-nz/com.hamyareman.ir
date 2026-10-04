package com.hamyareman.ir.ui.safespace

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.security.Encryptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val BACKUP_FORMAT = "hamyar-safe-workspace-2"
private const val BACKUP_MAGIC = "HMYB1"
private const val JOURNAL_KEY = "journal_entries"
private const val GRATITUDE_KEY = "gratitude_journal_entries"
private const val FREE_WRITING_KEY = "safe_free_writing_entries"
private const val LEGACY_FREE_WRITING_KEY = "safe_free_writing"
private const val DIARY_STORE = "hamyar_private_diary"
private const val MEDIA_STORE = "hamyar_secure_media"

private enum class BackupKind(val title: String, val extension: String, val encrypted: Boolean, val zip: Boolean) {
    ENCRYPTED_ZIP("ZIP رمزگذاری‌شده (پیشنهادی)", "hmzip", true, true),
    ENCRYPTED_JSON("JSON رمزگذاری‌شده", "hmjson", true, false),
    PLAIN_ZIP("ZIP بدون رمز", "zip", false, true),
}

/** کارت واحد بکاپ همهٔ داده‌های فضای امن؛ قالب پیش‌فرض فقط توسط خود اپ باز می‌شود. */
@Composable
fun SafeSpaceBackupCard() {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(BackupKind.ENCRYPTED_ZIP) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            notice = withContext(Dispatchers.IO) {
                runCatching {
                    SafeBackupCodec.export(context, uri, kind, container.encryptor)
                    "بکاپ کامل فضای امن ذخیره شد."
                }.getOrElse { "ساخت بکاپ ممکن نشد: ${it.message.orEmpty().take(120)}" }
            }
            busy = false
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            notice = withContext(Dispatchers.IO) {
                runCatching {
                    val count = SafeBackupCodec.restore(context, uri, container.encryptor)
                    "بازیابی انجام شد؛ $count مورد با داده‌های فعلی ادغام شد."
                }.getOrElse { "فایل بکاپ معتبر نیست یا کامل خوانده نشد: ${it.message.orEmpty().take(120)}" }
            }
            busy = false
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("بکاپ فضای امن", style = MaterialTheme.typography.titleMedium)
            Text(
                "بکاپ رمزگذاری‌شده برای نگهداری امن و انتقال بین نصب‌های همیار من پیشنهاد می‌شود.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        val stamp = JalaliDate.todayIso()
                        create.launch("hamyar-safe-" + stamp + "." + kind.extension)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(if (busy) "در حال آماده‌سازی…" else "ساخت بکاپ") }
                OutlinedButton(
                    enabled = !busy,
                    onClick = { restore.launch(arrayOf("application/octet-stream", "application/zip", "application/json", "*/*")) },
                    modifier = Modifier.weight(1f),
                ) { Text("بازیابی") }
            }
            Text("قالب بکاپ", style = MaterialTheme.typography.labelMedium)
            BackupKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option,
                    onClick = { kind = option },
                    label = { Text(option.title) },
                )
            }
            if (kind == BackupKind.PLAIN_ZIP) {
                Text(
                    "هشدار: ZIP بدون رمز برای استفاده در دستگاه‌ها یا برنامه‌های دیگر است؛ هرکس فایل را داشته باشد می‌تواند محتوا را ببیند.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "نسخهٔ رمزگذاری‌شده در حالت عادی فقط با «همیار من» قابل بازیابی است.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

private object SafeBackupCodec {
    private val appBackupKey: ByteArray by lazy {
        MessageDigest.getInstance("SHA-256")
            .digest("com.hamyareman.ir/private-workspace/backup/v2".toByteArray())
    }

    fun export(context: Context, target: Uri, kind: BackupKind, encryptor: Encryptor) {
        val temp = File.createTempFile("safe-export-", if (kind.zip) ".zip" else ".json", context.cacheDir)
        try {
            val root = snapshot(context, encryptor, includeMediaBytes = !kind.zip)
            if (kind.zip) writeZip(context, temp, root) else temp.writeText(root.toString())
            context.contentResolver.openOutputStream(target)?.use { raw ->
                if (kind.encrypted) encrypt(FileInputStream(temp), raw) else FileInputStream(temp).use { it.copyTo(raw) }
            } ?: error("مسیر خروجی باز نشد")
        } finally {
            temp.delete()
        }
    }

    fun restore(context: Context, source: Uri, encryptor: Encryptor): Int {
        val incoming = File.createTempFile("safe-import-", ".bin", context.cacheDir)
        val plain = File.createTempFile("safe-plain-", ".bin", context.cacheDir)
        val mediaTemp = File(context.cacheDir, "safe-media-${System.nanoTime()}").apply { mkdirs() }
        try {
            context.contentResolver.openInputStream(source)?.use { input -> incoming.outputStream().use { input.copyTo(it) } }
                ?: error("فایل باز نشد")
            FileInputStream(incoming).buffered().use { probe ->
                val magic = ByteArray(BACKUP_MAGIC.length)
                val n = probe.read(magic)
                if (n == magic.size && String(magic) == BACKUP_MAGIC) {
                    decrypt(FileInputStream(incoming), FileOutputStream(plain))
                } else incoming.copyTo(plain, overwrite = true)
            }
            val root = if (plain.inputStream().buffered().use { a -> a.read() == 'P'.code && a.read() == 'K'.code }) {
                readZip(plain, mediaTemp)
            } else {
                JSONObject(plain.readText())
            }
            require(root.optString("format") == BACKUP_FORMAT) { "قالب ناشناخته" }
            return applySnapshot(context, root, mediaTemp, encryptor)
        } finally {
            incoming.delete(); plain.delete(); mediaTemp.deleteRecursively()
        }
    }

    private fun snapshot(context: Context, encryptor: Encryptor, includeMediaBytes: Boolean): JSONObject {
        val main = LocalStore(context)
        val diary = LocalStore(context, DIARY_STORE)
        val awareness = LocalStore(context, "hamyar_awareness")
        val mediaStore = LocalStore(context, MEDIA_STORE)
        val root = JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("createdAt", System.currentTimeMillis())
            .put("journal", decryptedEntryArray(main.getString(JOURNAL_KEY, "[]"), encryptor))
            .put("gratitude", decryptedEntryArray(main.getString(GRATITUDE_KEY, "[]"), encryptor))
            .put("freeWriting", decryptedEntryArray(main.getString(FREE_WRITING_KEY, "[]"), encryptor))
            .put("selfAwareness", awareness.getString("self_answers", "{}"))

        if (root.getJSONArray("freeWriting").length() == 0) {
            val legacy = encryptor.decrypt(main.getString(LEGACY_FREE_WRITING_KEY, "")).orEmpty()
            if (legacy.isNotBlank()) {
                root.put("freeWriting", JSONArray().put(JSONObject()
                    .put("id", "legacy-free-writing")
                    .put("createdAt", System.currentTimeMillis())
                    .put("title", "نوشتهٔ آزاد")
                    .put("text", legacy)))
            }
        }

        val diaryEntries = decryptedEntryArray(diary.getString("entries", "[]"), encryptor)
        root.put("diary", JSONObject().put("cover", diary.getString("cover", "celestial")).put("entries", diaryEntries))

        val media = JSONArray()
        val storedMedia = runCatching { JSONArray(mediaStore.getString("items", "[]")) }.getOrDefault(JSONArray())
        for (i in 0 until storedMedia.length()) {
            val old = storedMedia.getJSONObject(i)
            val file = File(old.optString("path"))
            if (!file.exists()) continue
            val exportedName = "${old.optString("id", "media-$i")}.${file.extension.ifBlank { "bin" }}"
            val row = JSONObject()
                .put("id", old.optString("id"))
                .put("name", old.optString("name"))
                .put("mime", old.optString("mime"))
                .put("addedAt", old.optLong("addedAt"))
                .put("file", exportedName)
            if (includeMediaBytes) {
                row.put("data", Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
            } else {
                row.put("sourcePath", file.absolutePath)
            }
            media.put(row)
        }
        root.put("media", media)
        return root
    }

    private fun decryptedEntryArray(raw: String, encryptor: Encryptor): JSONArray {
        val out = JSONArray()
        val source = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        for (i in 0 until source.length()) {
            val old = source.getJSONObject(i)
            val plain = encryptor.decrypt(old.optString("cipher")) ?: continue
            out.put(JSONObject()
                .put("id", old.optString("id"))
                .put("createdAt", old.optLong("createdAt"))
                .put("title", old.optString("title"))
                .put("text", plain))
        }
        return out
    }

    private fun writeZip(context: Context, file: File, root: JSONObject) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(root.toString().toByteArray())
            zip.closeEntry()
            val media = root.getJSONArray("media")
            for (i in 0 until media.length()) {
                val row = media.getJSONObject(i)
                val source = File(row.optString("sourcePath"))
                if (!source.exists()) continue
                zip.putNextEntry(ZipEntry("media/${row.getString("file")}"))
                source.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                row.remove("sourcePath")
            }
        }
        // data.json باید مسیر داخلی سیستم را نداشته باشد؛ فایل کوچک را یک بار تمیز بازنویسی می‌کنیم.
        val clean = File.createTempFile("safe-clean-", ".zip", context.cacheDir)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(clean))).use { out ->
            out.putNextEntry(ZipEntry("data.json")); out.write(root.toString().toByteArray()); out.closeEntry()
            val media = root.getJSONArray("media")
            ZipInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                var entry = input.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("media/")) {
                        out.putNextEntry(ZipEntry(entry.name)); input.copyTo(out); out.closeEntry()
                    }
                    input.closeEntry(); entry = input.nextEntry
                }
            }
        }
        clean.copyTo(file, overwrite = true); clean.delete()
    }

    private fun readZip(file: File, mediaTemp: File): JSONObject {
        var root: JSONObject? = null
        ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "data.json" -> root = JSONObject(zip.bufferedReader().readText())
                    entry.name.startsWith("media/") && !entry.isDirectory -> {
                        val safeName = File(entry.name).name
                        File(mediaTemp, safeName).outputStream().use { zip.copyTo(it) }
                    }
                }
                zip.closeEntry(); entry = zip.nextEntry
            }
        }
        return root ?: error("data.json پیدا نشد")
    }

    private fun applySnapshot(context: Context, root: JSONObject, mediaTemp: File, encryptor: Encryptor): Int {
        val main = LocalStore(context)
        val diary = LocalStore(context, DIARY_STORE)
        var count = 0
        fun mergeEntries(store: LocalStore, key: String, incoming: JSONArray) {
            val existing = runCatching { JSONArray(store.getString(key, "[]")) }.getOrDefault(JSONArray())
            val ids = mutableSetOf<String>()
            for (i in 0 until existing.length()) ids += existing.getJSONObject(i).optString("id")
            for (i in 0 until incoming.length()) {
                val row = incoming.getJSONObject(i)
                var id = row.optString("id").ifBlank { "restored-${System.nanoTime()}-$i" }
                if (id in ids) id = "$id-restored-${System.nanoTime()}"
                existing.put(JSONObject()
                    .put("id", id)
                    .put("createdAt", row.optLong("createdAt", System.currentTimeMillis()))
                    .put("title", row.optString("title"))
                    .put("cipher", encryptor.encrypt(row.optString("text"))))
                ids += id; count++
            }
            store.putString(key, existing.toString())
        }

        mergeEntries(main, JOURNAL_KEY, root.optJSONArray("journal") ?: JSONArray())
        mergeEntries(main, GRATITUDE_KEY, root.optJSONArray("gratitude") ?: JSONArray())
        mergeEntries(main, FREE_WRITING_KEY, root.optJSONArray("freeWriting") ?: JSONArray())
        root.optJSONObject("diary")?.let { d ->
            diary.putString("cover", d.optString("cover", diary.getString("cover", "celestial")))
            mergeEntries(diary, "entries", d.optJSONArray("entries") ?: JSONArray())
        }
        val awarenessIncoming = runCatching { JSONObject(root.optString("selfAwareness", "{}")) }.getOrDefault(JSONObject())
        if (awarenessIncoming.length() > 0) {
            val awareness = LocalStore(context, "hamyar_awareness")
            val current = runCatching { JSONObject(awareness.getString("self_answers", "{}")) }.getOrDefault(JSONObject())
            awarenessIncoming.keys().forEach { key -> current.put(key, awarenessIncoming.get(key)); count++ }
            awareness.putString("self_answers", current.toString())
        }

        val mediaStore = LocalStore(context, MEDIA_STORE)
        val currentMedia = runCatching { JSONArray(mediaStore.getString("items", "[]")) }.getOrDefault(JSONArray())
        val mediaIds = mutableSetOf<String>()
        for (i in 0 until currentMedia.length()) mediaIds += currentMedia.getJSONObject(i).optString("id")
        val mediaDir = File(context.filesDir, "secure-media").apply { mkdirs() }
        val incomingMedia = root.optJSONArray("media") ?: JSONArray()
        for (i in 0 until incomingMedia.length()) {
            val row = incomingMedia.getJSONObject(i)
            var id = row.optString("id").ifBlank { "restored-media-${System.nanoTime()}-$i" }
            if (id in mediaIds) id = "$id-restored-${System.nanoTime()}"
            val exportedName = row.optString("file", "$id.bin")
            val ext = File(exportedName).extension.ifBlank { "bin" }
            val target = File(mediaDir, "$id.$ext")
            val zipped = File(mediaTemp, File(exportedName).name)
            when {
                zipped.exists() -> zipped.copyTo(target, overwrite = true)
                row.has("data") -> target.writeBytes(Base64.decode(row.getString("data"), Base64.DEFAULT))
                else -> continue
            }
            currentMedia.put(JSONObject()
                .put("id", id)
                .put("name", row.optString("name", target.name))
                .put("mime", row.optString("mime", "application/octet-stream"))
                .put("path", target.absolutePath)
                .put("addedAt", row.optLong("addedAt", System.currentTimeMillis())))
            mediaIds += id; count++
        }
        mediaStore.putString("items", currentMedia.toString())
        return count
    }

    private fun encrypt(input: InputStream, output: OutputStream) {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        output.write(BACKUP_MAGIC.toByteArray()); output.write(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(appBackupKey, "AES"), GCMParameterSpec(128, iv))
        CipherOutputStream(BufferedOutputStream(output), cipher).use { encrypted -> input.use { it.copyTo(encrypted) } }
    }

    private fun decrypt(input: InputStream, output: OutputStream) {
        val buffered = BufferedInputStream(input)
        val magic = ByteArray(BACKUP_MAGIC.length)
        require(buffered.read(magic) == magic.size && String(magic) == BACKUP_MAGIC) { "هدر رمز نامعتبر" }
        val iv = ByteArray(12)
        require(buffered.read(iv) == iv.size) { "فایل ناقص است" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(appBackupKey, "AES"), GCMParameterSpec(128, iv))
        CipherInputStream(buffered, cipher).use { decrypted -> BufferedOutputStream(output).use { decrypted.copyTo(it) } }
    }
}
