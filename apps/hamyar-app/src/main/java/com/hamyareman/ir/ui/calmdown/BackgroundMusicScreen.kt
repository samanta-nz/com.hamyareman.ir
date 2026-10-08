package com.hamyareman.ir.ui.calmdown

import androidx.compose.runtime.Composable

/**
 * صفحهٔ «فضای آرام من» از پلیر کامل روی باکت استفاده می‌کند.
 * میزبان mini محلی حذف شده و دیگر به assets/content/background-music.html وابستگی ندارد.
 */
@Composable
fun BackgroundMusicScreen(onBack: () -> Unit) = CalmWhispersScreen(onBack)
