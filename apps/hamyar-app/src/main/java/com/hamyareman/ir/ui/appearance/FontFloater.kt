package com.hamyareman.ir.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.platform.core.common.toPersianDigits

/** فلوتر جمع‌شوندهٔ فونت روی همهٔ صفحات. */
@Composable
fun BoxScope.FontFloater(route: String?) {
    val prefs = LocalUiPrefs.current
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(route) { TypeSlots.setRoute(route) }
    val slots = remember(route, TypeSlots.rev) { FontCatalog.forRoute(route) }
    val grouped = remember(slots) { slots.groupBy { it.group } }

    if (open) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("فونت نقش‌ها", style = MaterialTheme.typography.titleMedium)
                        Text(
                            FontCatalog.pageTitle(route),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { open = false }) { Text("بستن") }
                }
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    grouped.forEach { (group, list) ->
                        Text(group, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        list.forEach { slot ->
                            FloaterSlotRow(
                                slot = slot,
                                choice = TypeSlots.resolved(slot.id),
                                onFont = { prefs.updateSlot(slot.id, font = it) },
                                onSize = { prefs.updateSlot(slot.id, size = it) },
                                onWeight = { prefs.updateSlot(slot.id, weight = it) },
                            )
                        }
                    }
                }
            }
        }
    } else {
        FloatingActionButton(
            onClick = { open = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .size(48.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text("آ", fontSize = 20.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloaterSlotRow(
    slot: FontSlot,
    choice: SlotChoice,
    onFont: (String) -> Unit,
    onSize: (Int) -> Unit,
    onWeight: (String) -> Unit,
) {
    val pad = slot.id == "d7.pad"
    var menu by remember { mutableStateOf(false) }
    val dashboard = slot.group == "D داشبورد"
    val standardFace = EmbeddedFonts.face("vazirmatn")
    val face = remember(choice.font, dashboard) {
        if (dashboard) EmbeddedFonts.face(choice.font) else standardFace
    }
    val family = remember(choice.font, choice.weight, dashboard) {
        if (dashboard) EmbeddedFonts.family(choice.font, EmbeddedFonts.W_BOLD)
        else EmbeddedFonts.family("vazirmatn", EmbeddedFonts.W_BOLD)
    }
    val fw = if (dashboard) EmbeddedFonts.fontWeight(EmbeddedFonts.W_BOLD) else EmbeddedFonts.fontWeight(choice.weight)
    val sampleSp = if (dashboard) choice.size.coerceIn(8, 40) else slot.baseSp
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(slot.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!pad) {
            Text(
                slot.sample,
                fontFamily = family,
                fontWeight = fw,
                fontSize = sampleSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                OutlinedTextField(
                    value = face.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("فونت") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    (if (dashboard) EmbeddedFonts.catalog else listOf(standardFace)).forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label, fontFamily = EmbeddedFonts.family(item.key, item.defaultWeight)) },
                            onClick = { onFont(item.key); menu = false },
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (dashboard) listOf(EmbeddedFonts.W_BOLD) else listOf(
                    EmbeddedFonts.W_REGULAR,
                    EmbeddedFonts.W_BOLD,
                )).forEach { w ->
                    FilterChip(
                        selected = EmbeddedFonts.normalizeWeight(choice.weight) == w,
                        onClick = { onWeight(w) },
                        label = { Text(EmbeddedFonts.weightLabel(w)) },
                    )
                }
            }
            Text(
                if (dashboard) "سایز " + toPersianDigits(sampleSp.toString())
                else "اندازهٔ استاندارد نقش: " + toPersianDigits(slot.baseSp.toString()) + "sp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (dashboard) {
                Slider(
                    value = sampleSp.toFloat(),
                    onValueChange = { onSize(it.toInt()) },
                    valueRange = 8f..40f,
                    steps = 31,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Text(
                "فاصله از دو طرف: ${toPersianDigits(choice.size.toString())} dp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = choice.size.toFloat().coerceIn(0f, 32f),
                onValueChange = { onSize(it.toInt()) },
                valueRange = 0f..32f,
                steps = 31,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
