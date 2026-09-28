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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.ui.study.ServerPrefs
import com.hamyareman.ir.ui.study.ServerResolver
import kotlinx.coroutines.launch

/**
 * سه گزینهٔ بالای «تنظیمات سرور»:
 *  ۱) پیش‌فرض: سریع‌ترین سرور  ۲) سرور خارجی (Appwrite)  ۳) سرور ایرانی (آروان)
 */
@Composable
fun ServerOptionsSection() {
    var mode by remember { mutableStateOf(ServerPrefs.mode) }
    var probe by remember { mutableStateOf(ServerPrefs.lastProbeOk) }
    val scope = rememberCoroutineScope()

    fun setMode(m: ServerPrefs.Mode) {
        mode = m
        ServerPrefs.mode = m
        if (m == ServerPrefs.Mode.FASTEST) {
            scope.launch { probe = ServerResolver.probeInternal() }
        }
    }

    LaunchedEffect(Unit) {
        if (probe == null) probe = ServerResolver.probeInternal()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("سرور محتوا", style = MaterialTheme.typography.titleSmall)
            Text(
                "کتاب‌ها، صداها و صفحه‌ها از کدام سرور بیایند؟",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ServerOption(
                "۱. پیش‌فرض: سریع‌ترین سرور",
                "خودکار — اگر سرور ایرانی در دسترس باشد از آن، وگرنه از سرور خارجی.",
                ServerPrefs.Mode.FASTEST, mode, ::setMode,
            )
            ServerOption(
                "۲. سرور خارجی (Appwrite)",
                "همیشه از سرور خارجی — پایدار؛ در ایران ممکن است کندتر باشد.",
                ServerPrefs.Mode.EXTERNAL, mode, ::setMode,
            )
            ServerOption(
                "۳. سرور ایرانی (آروان)",
                "همیشه از سرور داخلی — سریع در ایران؛ اگر در دسترس نباشد فایل باز نمی‌شود.",
                ServerPrefs.Mode.INTERNAL, mode, ::setMode,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val status = when (probe) {
                    true -> "سرور ایرانی: در دسترس ✅"
                    false -> "سرور ایرانی: در دسترس نیست ❌"
                    null -> "سرور ایرانی: در حال بررسی…"
                }
                Text(status, style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = { scope.launch { probe = ServerResolver.probeInternal() } }) {
                    Text("بررسی دوباره")
                }
            }
        }
    }
}

@Composable
private fun ServerOption(
    title: String,
    desc: String,
    value: ServerPrefs.Mode,
    selected: ServerPrefs.Mode,
    onSelect: (ServerPrefs.Mode) -> Unit,
) {
    val isSel = value == selected
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSel) {
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
            RadioButton(selected = isSel, onClick = { onSelect(value) })
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
