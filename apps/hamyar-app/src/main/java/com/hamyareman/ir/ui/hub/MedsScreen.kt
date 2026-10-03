package com.hamyareman.ir.ui.hub

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAlarm
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.appwrite.AppResult
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.common.toPersianDigits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private const val MEDS_STORE = "hamyar_health"
private const val MEDS_KEY = "meds_list"
private const val MEDS_SYNC_ROW_PREFIX = "meds_"

internal data class MedicationReminder(
    val id: String,
    val name: String,
    val dose: String,
    val note: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
)

object MedsStore {
    internal fun load(store: LocalStore): List<MedicationReminder> = runCatching {
        val arr = JSONArray(store.getString(MEDS_KEY, "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            MedicationReminder(
                id = o.optString("id").ifBlank { "med-$i" },
                name = o.optString("name"),
                dose = o.optString("dose"),
                note = o.optString("note"),
                hour = o.optInt("hour", 20).coerceIn(0, 23),
                minute = o.optInt("minute", 0).coerceIn(0, 59),
                enabled = o.optBoolean("enabled", true),
            ).takeIf { it.name.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    internal fun save(store: LocalStore, meds: List<MedicationReminder>) {
        val arr = JSONArray()
        meds.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id).put("name", m.name).put("dose", m.dose)
                    .put("note", m.note).put("hour", m.hour).put("minute", m.minute)
                    .put("enabled", m.enabled),
            )
        }
        store.putString(MEDS_KEY, arr.toString())
    }

    internal fun syncRowId(userId: String) = MEDS_SYNC_ROW_PREFIX + userId.take(40)

    internal fun payload(meds: List<MedicationReminder>): String {
        val arr = JSONArray()
        meds.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id).put("name", m.name).put("dose", m.dose)
                    .put("note", m.note).put("hour", m.hour).put("minute", m.minute)
                    .put("enabled", m.enabled),
            )
        }
        return arr.toString()
    }

    internal fun decode(raw: String): List<MedicationReminder> = runCatching {
        val arr = JSONArray(raw.ifBlank { "[]" })
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            MedicationReminder(
                id = o.optString("id").ifBlank { "med-$i" },
                name = o.optString("name"),
                dose = o.optString("dose"),
                note = o.optString("note"),
                hour = o.optInt("hour", 20).coerceIn(0, 23),
                minute = o.optInt("minute", 0).coerceIn(0, 59),
                enabled = o.optBoolean("enabled", true),
            ).takeIf { it.name.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    internal fun cancel(context: Context, id: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pending(context, id))
    }

    private fun pending(context: Context, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, com.hamyareman.ir.notifications.MedsReceiver::class.java)
                .putExtra("id", id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    internal fun scheduleNext(context: Context, id: String) {
        val med = load(LocalStore(context, MEDS_STORE)).firstOrNull { it.id == id && it.enabled } ?: return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Calendar.getInstance()
        val whenAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, med.hour)
            set(Calendar.MINUTE, med.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        runCatching {
            val intent = pending(context, med.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarm.canScheduleExactAlarms()) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt.timeInMillis, intent)
            } else {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt.timeInMillis, intent)
            }
        }
    }

    internal fun scheduleAll(context: Context) {
        val meds = load(LocalStore(context, MEDS_STORE))
        meds.filter { it.enabled }.forEach { scheduleNext(context, it.id) }
    }

    internal suspend fun pullRemote(container: com.hamyareman.ir.di.AppContainer): List<MedicationReminder>? {
        val userId = container.auth.cachedUserId().orEmpty()
        if (!container.appwrite.isConfigured || userId.isBlank()) return null
        return when (val result = container.tables.get(TableIds.APP_STATE, syncRowId(userId))) {
            is AppResult.Ok -> result.value?.string("payload")?.let(::decode)
            is AppResult.Err -> null
        }
    }

    internal fun queueRemote(container: com.hamyareman.ir.di.AppContainer, meds: List<MedicationReminder>) {
        val userId = container.auth.cachedUserId().orEmpty()
        if (userId.isBlank()) return
        container.sync.enqueue(
            TableIds.APP_STATE,
            syncRowId(userId),
            mapOf(
                "userId" to userId,
                "key" to "medication_reminders",
                "payload" to payload(meds),
                "updatedAt" to System.currentTimeMillis(),
            ),
        )
    }
}

@Composable
fun MedsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val store = remember { LocalStore(context, MEDS_STORE) }
    val scope = rememberCoroutineScope()
    var meds by remember { mutableStateOf(MedsStore.load(store)) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("20") }
    var minute by remember { mutableStateOf("00") }

    fun resetForm() {
        editingId = null
        name = ""; dose = ""; note = ""; hour = "20"; minute = "00"
    }

    fun persist(next: List<MedicationReminder>) {
        val sorted = next.sortedWith(compareBy<MedicationReminder> { it.hour }.thenBy { it.minute }.thenBy { it.name })
        val oldIds = meds.map { it.id }.toSet()
        val newIds = sorted.map { it.id }.toSet()
        meds = sorted
        MedsStore.save(store, meds)
        oldIds.filterNot(newIds::contains).forEach { MedsStore.cancel(context, it) }
        MedsStore.scheduleAll(context)
        MedsStore.queueRemote(container, meds)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && context is Activity &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7101)
        }
        withContext(Dispatchers.IO) {
            val remote = runCatching { MedsStore.pullRemote(container) }.getOrNull()
            if (remote != null) {
                withContext(Dispatchers.Main) {
                    meds = remote.sortedWith(compareBy<MedicationReminder> { it.hour }.thenBy { it.minute }.thenBy { it.name })
                    MedsStore.save(store, meds)
                    MedsStore.scheduleAll(context)
                }
            }
        }
        MedsStore.scheduleAll(context)
    }

    HubBody {
        HubHeader("یادآور دارو 💊", "زمان داروها و مراقبت‌های روزانه")

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AddAlarm, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (editingId == null) "یادآور جدید" else "ویرایش یادآور", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("نام دارو یا مراقبت") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("ساعت") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, label = { Text("دقیقه") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(dose, { dose = it.take(80) }, label = { Text("دوز یا مقدار") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(note, { note = it.take(160) }, label = { Text("یادداشت") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(8, 12, 14, 18, 20, 22).forEach { h ->
                        FilterChip(
                            selected = hour.toIntOrNull() == h && minute.toIntOrNull() == 0,
                            onClick = { hour = h.toString().padStart(2, '0'); minute = "00" },
                            label = { Text(toPersianDigits("%02d:00".format(h))) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = name.isNotBlank() && hour.toIntOrNull() in 0..23 && minute.toIntOrNull() in 0..59,
                        onClick = {
                            val current = editingId?.let { id -> meds.firstOrNull { it.id == id } }
                            val value = MedicationReminder(
                                id = current?.id ?: "med_" + System.currentTimeMillis().toString(36),
                                name = name.trim(),
                                dose = dose.trim(),
                                note = note.trim(),
                                hour = hour.toInt(),
                                minute = minute.toInt(),
                                enabled = current?.enabled ?: true,
                            )
                            persist(if (current == null) meds + value else meds.map { if (it.id == value.id) value else it })
                            resetForm()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (editingId == null) "افزودن" else "ذخیره") }
                    if (editingId != null) TextButton(onClick = ::resetForm) { Text("انصراف") }
                }
            }
        }

        meds.forEach { med ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Medication, null, Modifier.size(32.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(med.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            toPersianDigits("%02d:%02d".format(med.hour, med.minute)) +
                                if (med.dose.isBlank()) "" else " · " + med.dose,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (med.note.isNotBlank()) Text(med.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        editingId = med.id
                        name = med.name; dose = med.dose; note = med.note
                        hour = med.hour.toString().padStart(2, '0'); minute = med.minute.toString().padStart(2, '0')
                    }) { Icon(Icons.Outlined.Edit, "ویرایش") }
                    Switch(
                        checked = med.enabled,
                        onCheckedChange = { enabled ->
                            persist(meds.map { if (it.id == med.id) it.copy(enabled = enabled) else it })
                        },
                    )
                    IconButton(onClick = { persist(meds.filterNot { it.id == med.id }) }) {
                        Icon(Icons.Outlined.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (meds.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.NotificationsActive, null, Modifier.size(44.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("هنوز یادآوری‌ای ثبت نشده است.")
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, null)
            Spacer(Modifier.width(8.dp))
            Text("اعلان‌ها برای هر روز در همان ساعت دوباره برنامه‌ریزی می‌شوند.", style = MaterialTheme.typography.labelSmall)
        }
    }
}
