package com.hamyareman.ir.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.hamyareman.ir.ui.study.ServerResolver
import java.util.Locale

/** وضعیت اتصال منبع محتوای راه‌دور؛ محتوای برنامه فقط از پارس‌پک دریافت می‌شود. */
@Composable
fun ServerOptionsSection() {
    var result by remember { mutableStateOf<ServerResolver.SelectionTest?>(null) }
    var testing by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(retry) {
        testing = true
        result = runCatching { ServerResolver.testSelection() }.getOrNull()
        testing = false
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("منبع دانلود محتوا", style = MaterialTheme.typography.titleSmall)
            Text(
                "تمام HTMLها و فایل‌های محتواییِ دارای مسیر باکت از پارس‌پک دریافت می‌شوند. آروان و باکت ذخیره‌سازی Appwrite منبع جایگزینِ محتوای HTML نیستند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (testing) {
                Text("در حال آزمایش اتصال به پارس‌پک…", style = MaterialTheme.typography.labelSmall)
            } else {
                val probe = result?.internal
                if (probe == null) {
                    Text("آزمون اتصال اجرا نشد.", style = MaterialTheme.typography.labelSmall)
                } else {
                    ProbeLine("پارس‌پک", probe)
                    Text(
                        if (result?.selected != null) "مبدأ محتوای HTML: پارس‌پک" else "پارس‌پک در دسترس نبود.",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(enabled = !testing, onClick = { retry++ }) { Text("آزمایش دوباره") }
                Text(
                    "آزمون از طریق GET/Range انجام می‌شود.",
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
        toPersianDigits(probe.latencyMs.toString()) + " ms • " + toPersianDigits(mbps) + " MB/s ✅"
    } else {
        "ناموفق (" + probe.error.ifBlank { "HTTP " + probe.status } + ") ❌"
    }
    Text(name + ": " + detail, style = MaterialTheme.typography.labelSmall)
}
