package com.hamyareman.ir.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.appwrite.BillingGateway
import com.hamyareman.ir.platform.core.appwrite.BillingOrder
import com.hamyareman.ir.platform.core.common.AppResult
import com.hamyareman.ir.platform.core.common.BillingConfig
import com.hamyareman.ir.platform.core.common.BillingStatus
import com.hamyareman.ir.platform.core.common.toLatinDigits
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.platform.core.designsystem.AppTopBar
import com.hamyareman.ir.ui.profile.StudentProfileRepo
import com.hamyareman.ir.ui.profile.StudentProfileState
import com.hamyareman.ir.ui.study.StudyMedia
import kotlinx.coroutines.launch
import java.io.File
import com.hamyareman.ir.ui.AppTypography

private fun priceFa(n: Int): String =
    toPersianDigits("%,d".format(n).replace(',', '٬')) + " تومان"

/** صفحهٔ تهیه اشتراک: کارت‌به‌کارت، فیش، انتظار تأیید، استرداد ۷روزه. */
@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    val gate = remember {
        BillingGateway(
            container.functions,
            container.tables,
            { container.auth.cachedUserId().orEmpty() },
            { container.auth.cachedUser()?.email.orEmpty() })
    }
    var installment by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var order by remember { mutableStateOf<BillingOrder?>(null) }
    var sub by remember { mutableStateOf(StudentProfileState.subscription) }
    var planId by remember { mutableStateOf(BillingConfig.YEARLY.id) }
    var payText by remember { mutableStateOf("") }
    var payerName by remember { mutableStateOf("") }
    var receiptPath by remember { mutableStateOf("") }
    var receiptName by remember { mutableStateOf("") }
    var showRefund by remember { mutableStateOf(false) }
    var shaba by remember { mutableStateOf("") }
    var card by remember { mutableStateOf("") }
    var refundName by remember { mutableStateOf("") }
    var refundReason by remember { mutableStateOf("") }

    fun applySub(next: String) {
        sub = next.ifBlank { BillingStatus.FREE }
        StudentProfileState.writeMirror(
            ctx,
            StudentProfileState.grade,
            StudentProfileState.hasProfile,
            StudentProfileState.firstName,
            sub,
            StudentProfileState.gender)
    }

    fun refresh() {
        loading = true
        error = null
        scope.launch {
            val uid = container.auth.cachedUserId().orEmpty()
            if (uid.isNotBlank()) {
                val remote = StudentProfileRepo.fetch(container.tables, uid)
                if (remote != null) StudentProfileState.applyServer(ctx, remote)
            }
            when (val r = gate.myOrder()) {
                is AppResult.Ok -> {
                    applySub(r.value.first)
                    order = r.value.second
                    if (BillingStatus.norm(r.value.first) == BillingStatus.REFUNDED ||
                        r.value.second?.status == "refunded"
                    ) {
                        notice = r.value.second?.farewell?.ifBlank { null } ?: BillingConfig.FAREWELL_FA
                    }
                }
                is AppResult.Err -> error = r.error.userMessage
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val pickReceipt = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val copied = copyUri(ctx, uri)
        if (copied != null) {
            receiptPath = copied.absolutePath
            receiptName = copied.name
        } else {
            error = "فیش انتخاب نشد؛ یک عکس واضح از رسید بفرست."
        }
    }

    val status = BillingStatus.norm(sub)
    val withinRefund = order?.paidAtMs?.let { it > 0L && System.currentTimeMillis() - it <= BillingConfig.REFUND_DAYS * 24L * 60 * 60 * 1000 } == true

    Column(Modifier.fillMaxSize()) {
        AppTopBar("اشتراک همیار من", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("گارانتی بازگشت وجه", style = AppTypography.pageHeading.style)
                    Text(BillingConfig.GUARANTEE_FA, style = AppTypography.pageBody.style)
                }
            }
            Text(
                "وضعیت: ${BillingStatus.chipFa(StudentProfileState.subscription, StudentProfileState.subscriptionEndMs)}" +
                    BillingStatus.rangeFa(0L, StudentProfileState.subscriptionEndMs).let { if (it.isBlank()) "" else " · $it" },
                style = AppTypography.pageBody.style,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
            notice?.let { Text(it, style = AppTypography.pageBody.style, color = MaterialTheme.colorScheme.primary) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            when (status) {
                BillingStatus.YEARLY, BillingStatus.MONTHLY, BillingStatus.INSTALLMENT, "paid" -> {
                    Text("اشتراک فعال است. درس‌های کامل برایت باز است.", style = AppTypography.pageBody.style)
                    // منوی انصراف فقط داخل همان ۷ روز بعد از خرید دیده می‌شود؛ بعدش کامل مخفی است.
                    if (withinRefund && !showRefund) {
                        OutlinedButton(onClick = { showRefund = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("انصراف و بازگشت وجه (تا ۷ روز)", style = AppTypography.pageBody.style)
                        }
                    }
                    if (withinRefund && showRefund) RefundForm(
                        shaba = shaba, card = card, name = refundName, reason = refundReason,
                        onShaba = { shaba = it }, onCard = { card = it }, onName = { refundName = it }, onReason = { refundReason = it },
                        busy = busy,
                        onSubmit = {
                            busy = true; error = null; notice = null
                            scope.launch {
                                when (val r = gate.requestRefund(shaba, card, refundName, refundReason)) {
                                    is AppResult.Ok -> {
                                        applySub(r.value.first)
                                        order = r.value.second
                                        showRefund = false
                                        notice = "درخواست بازگشت وجه ثبت شد. ادمین بررسی می‌کند."
                                    }
                                    is AppResult.Err -> error = r.error.userMessage
                                }
                                busy = false
                            }
                        })
                }
                BillingStatus.PENDING -> {
                    Text(
                        "سفارشت ثبت شد و در انتظار تأیید پرداخت است. به‌محض تأیید ادمین، اشتراک فعال می‌شود.",
                        style = AppTypography.pageBody.style)
                    order?.payText?.takeIf { it.isNotBlank() }?.let {
                        Text("متن واریز: $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
                BillingStatus.REFUND_PENDING -> {
                    Text(
                        "درخواست بازگشت وجه به ادمین رسیده. تا تأیید نهایی اشتراک هنوز فعال است.",
                        style = AppTypography.pageBody.style)
                }
                else -> {
                    BillingConfig.plans.forEach { p ->
                        val selected = planId == p.id
                        Card(onClick = { planId = p.id }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(selected = selected, onClick = { planId = p.id }, label = { Text(p.titleFa, style = AppTypography.pageButton.style) })
                                    Spacer(Modifier.weight(1f))
                                    Text(priceFa(p.priceToman), style = AppTypography.pageHeading.style, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(p.blurbFa, style = AppTypography.pageBody.style)
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("کارت‌به‌کارت", style = AppTypography.pageHeading.style)
                            Text("به نام: ${BillingConfig.ACCOUNT_HOLDER}", style = AppTypography.pageBody.style)
                            Text("بانک: ${BillingConfig.BANK_NAME}", style = AppTypography.pageBody.style)
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    if (BillingConfig.cardReady()) BillingConfig.CARD_NUMBER else "شماره کارت از پشتیبانی اعلام می‌شود",
                                    fontWeight = FontWeight.Bold,
                                    style = AppTypography.pageHeading.style)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = if (BillingConfig.cardReady()) BillingConfig.CARD_NUMBER else BillingConfig.ACCOUNT_HOLDER
                                    cm.setPrimaryClip(ClipData.newPlainText("card", text))
                                    notice = "کپی شد."
                                },
                                modifier = Modifier.fillMaxWidth()) { Text("کپی مشخصات واریز") }
                            Text(
                                "بعد از واریز، فیش را بفرست یا مشخصات واریز (زمان، مبلغ، چهار رقم آخر کارت مبدأ) را بنویس.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedTextField(
                        value = payerName,
                        onValueChange = { payerName = it },
                        label = { Text("نام صاحب حساب مبدأ *") },
                        supportingText = { Text("همان حسابی که از آن کارت‌به‌کارت کردی — برای بازگشت وجه لازم است") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = payText,
                        onValueChange = { payText = it },
                        label = { Text("متن مشخصات واریز") },
                        supportingText = { Text("اگر فیش نداری، زمان و مبلغ و چهار رقم آخر کارت مبدأ را بنویس") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { pickReceipt.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (receiptName.isBlank()) "ارسال عکس فیش" else "فیش: $receiptName")
                    }
                    if (planId == BillingConfig.YEARLY.id) {
                        FilterChip(
                            selected = installment,
                            onClick = { installment = !installment },
                            label = { Text("خرید اقساطی سالانه — ۴ قسط ماهانه") })
                        if (installment) {
                            Text(
                                "مبلغ کل ${priceFa(BillingConfig.YEARLY.priceToman)} در ۴ قسط ${priceFa(BillingConfig.YEARLY.priceToman / 4)}. قسط اول همین الان با همین فرم واریز می‌شود. گارانتی بازگشت وجه برای اقساط هم برقرار است.",
                                style = AppTypography.pageBody.style)
                        }
                    }
                    Button(
                        onClick = {
                            if (payerName.trim().length < 3) {
                                error = "نام صاحب حساب مبدأ را بنویس."
                                return@Button
                            }
                            if (payText.trim().isBlank() && receiptPath.isBlank()) {
                                error = "فیش یا متن مشخصات واریز را بفرست."
                                return@Button
                            }
                            busy = true; error = null; notice = null
                            scope.launch {
                                var fileId = ""
                                if (receiptPath.isNotBlank()) {
                                    val uid = container.auth.cachedUserId().orEmpty().ifBlank {
                                        runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
                                    }
                                    val fid = "rec" + uid.filter { it.isLetterOrDigit() }.take(12) + System.currentTimeMillis().toString().takeLast(8)
                                    when (val up = container.storage.upload(StudyMedia.BUCKET, receiptPath, emptyList(), fid)) {
                                        is AppResult.Ok -> fileId = up.value.fileId
                                        is AppResult.Err -> {
                                            error = "آپلود فیش ناموفق بود: ${up.error.userMessage}"
                                            busy = false
                                            return@launch
                                        }
                                    }
                                }
                                if (installment && planId == BillingConfig.YEARLY.id) {
                                    when (val r = gate.createInstallment(payerName.trim(), payText.trim(), fileId, "قسط اول از چهار")) {
                                        is AppResult.Ok -> {
                                            applySub(BillingStatus.INSTALLMENT)
                                            notice = "قسط اول ثبت شد. ادمین اقساط را از کنسول می‌بیند."
                                            refresh()
                                        }
                                        is AppResult.Err -> error = r.error.userMessage
                                    }
                                } else {
                                    when (val r = gate.createOrder(planId, payText.trim(), fileId, payerName.trim())) {
                                        is AppResult.Ok -> {
                                            applySub(r.value.first)
                                            order = r.value.second
                                            notice = "سفارش ثبت شد. وضعیت: انتظار برای پرداخت."
                                        }
                                        is AppResult.Err -> error = r.error.userMessage
                                    }
                                }
                                busy = false
                            }
                        },
                        enabled = !busy && !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        if (busy) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp)
                        else Text("ارسال و انتظار برای پرداخت", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RefundForm(
    shaba: String,
    card: String,
    name: String,
    reason: String,
    onShaba: (String) -> Unit,
    onCard: (String) -> Unit,
    onName: (String) -> Unit,
    onReason: (String) -> Unit,
    busy: Boolean,
    onSubmit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("بازگشت وجه", style = AppTypography.pageHeading.style)
            Text(
                "شبا و کارت باید متعلق به همان حسابی باشد که خرید از آن انجام شده، نه لزوماً به نام خودت.",
                style = MaterialTheme.typography.bodySmall)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = shaba,
                    onValueChange = { onShaba(toLatinDigits(it).filter { c -> c.isLetterOrDigit() }.take(26)) },
                    label = { Text("شبا") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = card,
                    onValueChange = { onCard(toLatinDigits(it).filter { c -> c.isDigit() }.take(16)) },
                    label = { Text("شماره کارت ۱۶ رقمی") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                label = { Text("نام صاحب همان حساب خرید *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = reason,
                onValueChange = onReason,
                label = { Text("دلیل انصراف (اختیاری)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth())
            Button(onClick = onSubmit, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "در حال ارسال…" else "ارسال درخواست بازگشت وجه")
            }
        }
    }
}

private fun copyUri(ctx: Context, uri: Uri): File? {
    val dest = File(ctx.cacheDir, "receipt-${System.currentTimeMillis()}.jpg")
    return runCatching {
        ctx.contentResolver.openInputStream(uri)?.use { inn ->
            dest.outputStream().use { inn.copyTo(it) }
        }
        dest.takeIf { it.exists() && it.length() > 80 }
    }.getOrNull()
}
