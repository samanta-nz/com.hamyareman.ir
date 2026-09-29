package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.ui.AppTypography
import com.hamyareman.ir.ui.hub.HubBody
import com.hamyareman.ir.ui.hub.HubMenuGroup
import com.hamyareman.ir.ui.hub.HubCard
import com.hamyareman.ir.ui.hub.HubCatalog
import com.hamyareman.ir.ui.hub.HubCoverGrid
import com.hamyareman.ir.ui.hub.HubCoverTile
import com.hamyareman.ir.ui.hub.HubHeader
import com.hamyareman.ir.ui.hub.hubTo
import com.hamyareman.ir.ui.hub.layerTo
import com.hamyareman.ir.ui.content.ContentCatalog
import com.hamyareman.ir.ui.navigation.Screen

/**
 * هاب «آموزشگاه» — هوش مصنوعی از [HubCatalog]؛ مهارت زندگی با جلد مربعی.
 */
@Composable
fun AcademyHubScreen(nav: NavController) {
    val gender = HubCatalog.gender()
    val ctx = LocalContext.current
    val academyContent = remember(ctx, gender) {
        ContentCatalog.apply { load(ctx) }.itemsOf("amozesh", gender)
    }
    HubBody {
        HubHeader("آموزشگاه ✨", "هوش مصنوعی، مهارت زندگی و کلاس‌های مهارتی", slotId = "hub.academy.header")
        HubCatalog.academy().forEach { group ->
            HubMenuGroup(group.title, group.subtitle, slotId = "hub.academy.group.${group.id}") {
                group.items.forEach { item ->
                    val key = item.route.substringBefore("?").substringBefore("/")
                    HubCard(item.emojiFor(gender), item.title, item.subtitle, slotId = "hub.academy.item.$key") { nav.hubTo(item.route) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("مهارت زندگی و یادگیری", style = AppTypography.pageHeading.style)
        Text(
            "تمام کلاس‌های تعاملی آموزشگاه؛ هر مهارت در کاشی خودش",
            style = AppTypography.pageBody.style,
        )
        Spacer(Modifier.height(8.dp))
        HubCoverGrid(
            academyContent.map { item ->
                HubCoverTile(
                    id = ContentCatalog.coverId(item),
                    title = item.title,
                    subtitle = ContentCatalog.tileSubtitle(item),
                    onClick = { nav.layerTo(Screen.ContentHtml.of(item.id)) },
                )
            },
        )
    }
}
