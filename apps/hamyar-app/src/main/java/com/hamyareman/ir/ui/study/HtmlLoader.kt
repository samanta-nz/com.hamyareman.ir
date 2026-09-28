package com.hamyareman.ir.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.platform.core.common.toPersianDigits

internal val HtmlLoaderBg = Color(0xFF0B0D12)
private val LoaderRing = Color(0xFF3DFF7A)
private val LoaderTrack = Color(0xFF2A2E38)
private val LoaderMuted = Color(0xFFB8BCC8)

@Composable
internal fun HtmlPercentLoader(percent: Int) {
    Box(
        Modifier.fillMaxSize().background(HtmlLoaderBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
                CircularProgressIndicator(
                    progress = { (percent.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.size(168.dp),
                    color = LoaderRing,
                    trackColor = LoaderTrack,
                    strokeWidth = 11.dp,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    toPersianDigits(percent.coerceIn(0, 100).toString()) + "٪",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text("در حال بارگذاری...", color = LoaderMuted, fontSize = 15.sp)
        }
    }
}
