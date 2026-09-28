package com.hamyareman.ir.ui.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.designsystem.AppTopBar

/**
 * صفحهٔ جداگانهٔ «مرخصی» — همان کارتِ کاملِ مرخصی که قبلاً انتهایِ برنامهٔ
 * هفتگی بود، بدونِ هیچ تغییری در محتوا، حالا در صفحهٔ خودش است و از منوی
 * «برنامه هفتگی و مرخصی» باز می‌شود.
 */
@Composable
fun LeaveScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    // مرخصی‌ها هم مثلِ بقیهٔ بخش‌ها با سرور یکی می‌شوند (آخرین نوشته برنده).
    LaunchedEffect(Unit) {
        val uid = runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
        if (uid.isBlank()) return@LaunchedEffect
        ClassPlanSync.pull(ctx, container.tables, uid, StateSync.KEY_LEAVES)
        ClassPlanSync.pushIfNewer(ctx, container.tables, uid, StateSync.KEY_LEAVES)
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar("مرخصی", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            LeaveSection()
        }
    }
}
