package com.hamyareman.ir.ui.hub

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.platform.core.common.LocalStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private const val MEDS_KEY = "meds_list"

private data class Med(val name: String, val hour: Int, val minute: Int)

private fun loadMeds(store: LocalStore): List<Med> = runCatching {
    val arr = JSONArray(store.getString(MEDS_KEY, "[]"))
    (0 until arr.length()).map { val o = arr.getJSONObject(it); Med(o.getString("name"), o.getInt("hour"), o.getInt("minute")) }
}.getOrDefault(emptyList())

private fun saveMeds(store: LocalStore, meds: List<Med>) {
    val arr = JSONArray()
    meds.forEach { arr.put(JSONObject().put("name", it.name).put("hour", it.hour).put("minute", it.minute)) }
    store.putString(MEDS_KEY, arr.toString())
}

/** زمان‌بندی هشدار بعدی برای همه‌ی داروها (هشدار درشتِ غیردقیق — بدون مجوز اضافه). */
fun scheduleAllMeds(context: Context, store: LocalStore) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = Calendar.getInstance()
    loadMeds(store).forEach { med ->
        val t = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, med.hour); set(Calendar.MINUTE, med.minute); set(Calendar.SECOND, 0) }
        if (t.before(now)) t.add(Calendar.DAY_OF_YEAR, 1)
        val pi = PendingIntent.getBroadcast(
            context,
            ("med_${med.name}_${med.hour}_${med.minute}").hashCode(),
            Intent(context, com.hamyareman.ir.notifications.MedsReceiver::class.java)
                .putExtra("name", med.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.timeInMillis, pi) }
    }
}

/** یادآور دارو و مراقبت روزانه — ساخت/حذف با هشدار سرِ وقت. */
@Composable
fun MedsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_health") }
    var meds by remember { mutableStateOf(loadMeds(store)) }
    var name by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("20") }
    var minute by remember { mutableStateOf("00") }

    fun persist(list: List<Med>) {
        meds = list
        saveMeds(store, list)
        scheduleAllMeds(context, store)
    }

    HubBody {
        HubHeader("یادآور دارو 💊", "دارو، مکمل یا مراقبت روزانه‌ات را بده به من")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام (مثلاً قرص آهن)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hour, onValueChange = { hour = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("ساعت") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minute, onValueChange = { minute = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("دقیقه") }, modifier = Modifier.weight(1f))
                }
                OutlinedButton(onClick = {
                    val h = hour.toIntOrNull() ?: return@OutlinedButton
                    val m = minute.toIntOrNull() ?: return@OutlinedButton
                    if (name.isBlank() || h !in 0..23 || m !in 0..59) return@OutlinedButton
                    persist(meds + Med(name.trim(), h, m))
                    name = ""
                }) { Text("افزودن یادآور ⏰") }
            }
        }

        if (meds.isEmpty()) {
            Text("هنوز چیزی ثبت نشده؛ مثلاً «قرص آهن — ۲۰:۰۰» را اضافه کن.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            meds.forEach { med ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("💊 ${med.name}", style = MaterialTheme.typography.titleMedium)
                            Text("هر روز ساعت %02d:%02d".format(med.hour, med.minute), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { persist(meds - med) }) { Text("حذف") }
                    }
                }
            }
        }
        Text(
            "یادآور با هشدار گوشی کار می‌کند؛ اگر گوشی خاموش/ریستارت شود، با یک بار بازکردن اپ دوباره فعال می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
