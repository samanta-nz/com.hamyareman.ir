package com.hamyareman.ir.ui.calmdown

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.hamyareman.ir.ui.wellness.PracticeHubScreen
import com.hamyareman.ir.ui.wellness.WellnessMenu

@Composable
fun CalmHubScreen(nav: NavController, onBack: () -> Unit) {
    PracticeHubScreen(
        nav = nav,
        rootIds = WellnessMenu.calmIds,
        title = "کسب آرامش 🌙",
        subtitle = "تنفس، سفر ذهنی، تصویرسازی، قصه و صدا",
        headerSlot = "hub.calm.header",
        accKey = "acc_calm",
        onBack = onBack,
    )
}
