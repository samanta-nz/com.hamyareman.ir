package com.hamyareman.ir.platform.feature.hearttoheart

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.platform.core.designsystem.InlineButton
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.platform.core.designsystem.SectionCard
import kotlinx.coroutines.launch
import java.io.File

/**
 * آلبوم خاطرات مشترک زهرا و پدر — یک صفحه برای هر دو اپ.
 *
 * تفاوت دو طرف فقط در نقش است ([AlbumRepository.myAuthor]):
 *  - **زهرا**: مالک آلبوم؛ خاطره‌های پدر را «تأیید» یا حذف می‌کند.
 *  - **پدر**: خاطره اضافه می‌کند و وضعیت تأیید را می‌بیند (بدون دسترسی به داده‌ی خصوصی زهرا).
 *
 * تصویر بندانگشتی از فایل **محلی** با `BitmapFactory` ساخته می‌شود (بدون وابستگی
 * Coil/Glide)؛ برای عکسی که طرف مقابل گذاشته، «بازکردن» لینک سرور را باز می‌کند.
 */
@Composable
fun SharedAlbumScreen(
    repo: AlbumRepository,
    onBack: (() -> Unit)? = null,
    title: String = "آلبوم خاطرات",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var pickedFile by remember { mutableStateOf<File?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val isZahra = repo.myAuthor == AlbumAuthor.ZAHRA

    suspend fun reload() {
        items = repo.refresh().let { result ->
            when (result) {
                is AppResult.Ok -> result.value
                is AppResult.Err -> repo.items()
            }
        }
    }

    LaunchedEffect(Unit) {
        items = repo.items()
        if (repo.isConfigured) {
            repo.pushPending()
            reload()
        }
    }

    // رویداد زنده‌ی آلبوم: خاطره‌ی تازه‌ی پدر یا تأیید زهرا بدون refresh دستی
    // روی صفحه می‌آید. بدون Realtime ⇒ جریان خالی ⇒ رفتار قبلی.
    LaunchedEffect(Unit) {
        repo.liveUpdates().collect { reload() }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedFile = MediaFiles.copyToCache(context, uri, "album")
            notice = if (pickedFile == null) "کپی‌گرفتن از عکس ممکن نشد." else null
        }
    }

    val pendingApproval = items.filterNot { it.approved }
    val approved = items.filter { it.approved }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title, onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionCard(
                title = if (isZahra) "آلبوم تو، تصمیم تو" else "آلبوم مشترک با زهرا",
                body = if (isZahra) {
                    "هر خاطره‌ای که بابا اضافه کند تا تأیید نکنی در آلبومت نمی‌نشیند. " +
                        "عکس‌ها در باکت مشترک `father-album` با دسترسی فقط شما دو نفر است."
                } else {
                    "خاطره‌ای که اضافه می‌کنی منتظر تأیید زهرا می‌ماند؛ تا تأیید نکند در آلبوم او «در انتظار» است. " +
                        "هیچ دسترسی به داده‌ی خصوصی او نداری."
                },
                onClick = {},
            )

            if (!repo.isConfigured) {
                SectionCard(
                    title = "حالت محلی",
                    body = "بک‌اند پیکربندی نشده؛ خاطره‌ها فقط روی همین دستگاه می‌مانند و به طرف مقابل نمی‌رسند.",
                    onClick = {},
                )
            } else if (repo.pendingCount() > 0) {
                SectionCard(
                    title = "${repo.pendingCount()} خاطره در صف ارسال",
                    body = "با «همگام‌سازی» دوباره امتحان کن.",
                    onClick = {},
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                InlineButton(if (showForm) "بستن فرم" else "افزودن خاطره") { showForm = !showForm }
                InlineButton("همگام‌سازی") {
                    scope.launch {
                        busy = true
                        val pushed = repo.pushPending()
                        reload()
                        notice = when (pushed) {
                            is AppResult.Ok -> if (pushed.value > 0) "${pushed.value} خاطره ارسال شد." else "همه‌چیز همگام است."
                            is AppResult.Err -> pushed.error.userMessage
                        }
                        busy = false
                    }
                }
            }

            if (showForm) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("عنوان خاطره") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("توضیح کوتاه (اختیاری)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                InlineButton(pickedFile?.let { "عکس انتخاب شد: ${it.name}" } ?: "انتخاب عکس") {
                    picker.launch(arrayOf("image/*"))
                }
                PrimaryButton(if (busy) "در حال ثبت…" else "ثبت در آلبوم") {
                    if (busy || titleInput.isBlank()) return@PrimaryButton
                    scope.launch {
                        busy = true
                        val persistent = pickedFile?.let { repo.importMedia(context, it) }
                        when (val result = repo.add(titleInput, noteInput, persistent)) {
                            is AppResult.Ok -> {
                                notice = if (result.value.synced) "خاطره ثبت و ارسال شد." else "خاطره روی دستگاه ثبت شد؛ بعداً ارسال می‌شود."
                                titleInput = ""
                                noteInput = ""
                                pickedFile?.delete()
                                pickedFile = null
                                showForm = false
                                reload()
                            }

                            is AppResult.Err -> notice = result.error.userMessage
                        }
                        busy = false
                    }
                }
            }

            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            if (pendingApproval.isNotEmpty()) {
                Text(
                    if (isZahra) "در انتظار تأیید تو (${pendingApproval.size})" else "در انتظار تأیید زهرا (${pendingApproval.size})",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            pendingApproval.forEach { item ->
                AlbumCard(
                    item = item,
                    repo = repo,
                    isZahra = isZahra,
                    onNotice = { notice = it },
                    onChanged = { scope.launch { reload() } },
                )
            }

            Spacer(Modifier.height(2.dp))
            Text("آلبوم (${approved.size})", style = MaterialTheme.typography.titleMedium)
            if (approved.isEmpty()) {
                Text(
                    "هنوز خاطره‌ای اینجا نیست. اولین عکس یا یادگاری را اضافه کن.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            approved.forEach { item ->
                AlbumCard(
                    item = item,
                    repo = repo,
                    isZahra = isZahra,
                    onNotice = { notice = it },
                    onChanged = { scope.launch { reload() } },
                )
            }
        }
    }
}

@Composable
private fun AlbumCard(
    item: AlbumItem,
    repo: AlbumRepository,
    isZahra: Boolean,
    onNotice: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val thumb: ImageBitmap? = remember(item.mediaPath) {
        item.mediaPath?.let { decodeThumbnail(it) }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = item.title,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${JalaliDate.stampFa(item.createdAt)} · ${item.addedBy.label}" +
                            if (item.synced) "" else " · در صف ارسال",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.mediaPath != null || item.mediaRef != null) {
                    TextButton(onClick = {
                        scope.launch {
                            openMedia(context, repo, item) { onNotice(it) }
                        }
                    }) { Text("بازکردن عکس") }
                }
                if (isZahra) {
                    TextButton(onClick = {
                        scope.launch {
                            val result = repo.setApproved(item, !item.approved)
                            onNotice(
                                when (result) {
                                    is AppResult.Ok -> if (item.approved) "از آلبوم بیرون گذاشته شد." else "تأیید شد و در آلبوم نشست."
                                    is AppResult.Err -> result.error.userMessage
                                },
                            )
                            onChanged()
                        }
                    }) { Text(if (item.approved) "لغو تأیید" else "تأیید کن") }
                }
                if (isZahra || item.addedBy == repo.myAuthor) {
                    TextButton(onClick = {
                        scope.launch {
                            val result = repo.delete(item)
                            onNotice(
                                when (result) {
                                    is AppResult.Ok -> "حذف شد."
                                    is AppResult.Err -> result.error.userMessage
                                },
                            )
                            onChanged()
                        }
                    }) { Text("حذف") }
                }
            }
        }
    }
}

private suspend fun openMedia(
    context: android.content.Context,
    repo: AlbumRepository,
    item: AlbumItem,
    onError: (String) -> Unit,
) {
    val local = item.mediaPath?.let { File(it) }?.takeIf { it.exists() }
    if (local != null) {
        val opened = runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", local)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
            true
        }.getOrDefault(false)
        if (!opened) onError("برنامه‌ای برای بازکردن عکس پیدا نشد.")
        return
    }
    val url = repo.mediaUrl(item)
    if (url == null) {
        onError("عکس این خاطره روی این دستگاه نیست.")
        return
    }
    val opened = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        true
    }.getOrDefault(false)
    if (!opened) onError("بازکردن عکس ممکن نشد.")
}

/** بندانگشتی کوچک از فایل محلی — بدون وابستگی بارگذاری تصویر. */
private fun decodeThumbnail(path: String, maxPx: Int = 240): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (bounds.outWidth / sample > maxPx * 2 || bounds.outHeight / sample > maxPx * 2) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}.getOrNull()
