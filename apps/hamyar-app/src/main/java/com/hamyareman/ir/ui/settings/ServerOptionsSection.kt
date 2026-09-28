package com.hamyareman.ir.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.study.ServerPrefs
import com.hamyareman.ir.ui.study.ServerResolver
import java.util.Locale

/** انتخاب منبع محتوا همراه با تست واقعی Range برای همان گزینهٔ انتخاب‌شده. */
@Composable
fun ServerOptionsSection() {
    var mode by remember { mutableStateOf(ServerPrefs.mode) }
    var result by remember { mutableStateOf<ServerResolver.SelectionTest?>(null) }
    var testing by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    fun setMode(value: ServerPrefs.Mode) {
        mode = value
        ServerPrefs.mode = value
        result = null
    }

    LaunchedEffect(mode, retry) {
        testing = true
        result = ServerResolver.testSelection(mode)
        testing = false
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("منبع دانلود محتوا", style = MaterialTheme.typography.titleSmall)
            Text(
                "کتاب‌ها، صداها و صفحه‌های دارای نسخهٔ دوم از کدام سرور دریافت شوند؟",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ServerOption(
                "۱. سریع‌ترین واقعی",
                "هر دو سرور با دانلود آزمایشی ۲۵۶ کیلوبایتی سنجیده می‌شوند؛ سریع‌تر انتخاب و نتیجه نمایش داده می‌شود.",
                ServerPrefs.Mode.FASTEST, mode, ::setMode,
            )
            ServerOption(
                "۲. سرور خارجی (Appwrite)",
                "فقط Appwrite؛ خطا بی‌صدا به آروان منتقل نمی‌شود.",
                ServerPrefs.Mode.EXTERNAL, mode, ::setMode,
            )
            ServerOption(
                "۳. سرور ایرانی (آروان)",
                "فقط آروان؛ خطا بی‌صدا به Appwrite منتقل نمی‌شود.",
                ServerPrefs.Mode.INTERNAL, mode, ::setMode,
            )

            if (testing) {
                Text("در حال سنجش واقعی اتصال و سرعت…", style = MaterialTheme.typography.labelSmall)
            } else {
                result?.external?.let { ProbeLine("Appwrite", it) }
                result?.internal?.let { ProbeLine("آروان", it) }
                if (mode == ServerPrefs.Mode.FASTEST) {
                    val selected = when (result?.selected) {
                        ServerPrefs.Origin.EXTERNAL -> "Appwrite"
                        ServerPrefs.Origin.INTERNAL -> "آروان"
                        null -> "هیچ‌کدام"
                    }
                    Text("انتخاب فعلی: $selected", style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(enabled = !testing, onClick = { retry++ }) { Text("تست دوباره") }
                Text(
                    "آزمون با GET/Range انجام می‌شود، نه HEAD.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProbeLine(name: String, probe: ServerResolver.Probe) {
    val detail = if (probe.ok) {
        val mbps = String.format(Locale.US, "%.1f", probe.bytesPerSecond / 1048576.0)
        "${toPersianDigits(probe.latencyMs.toString())} ms • ${toPersianDigits(mbps)} MB/s ✅"
    } else {
        "ناموفق (${probe.error.ifBlank { "HTTP ${probe.status}" }}) ❌"
    }
    Text("$name: $detail", style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun ServerOption(
    title: String,
    desc: String,
    value: ServerPrefs.Mode,
    selected: ServerPrefs.Mode,
    onSelect: (ServerPrefs.Mode) -> Unit,
) {
    val isSelected = value == selected
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isSelected, onClick = { onSelect(value) })
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
