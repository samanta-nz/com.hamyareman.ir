package com.hamyareman.ir.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamyareman.ir.R
import kotlinx.coroutines.launch

/**
 * فرم ورودی مشترک همهٔ دفترچه‌ها: کاغذ ثابت است و فقط متنِ نامحدود روی خطوط
 * اسکرول می‌شود. با رسیدن به سطر آخر، سطر تازه نمایان و سطر نخست زیر قاب می‌رود.
 */
@Composable
fun LinedNotebookInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val lineSp = with(density) { 24.dp.toSp() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(value.length, scroll.maxValue) {
        if (scroll.maxValue > 0) scroll.animateScrollTo(scroll.maxValue)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFBEB), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.notes_lined_paper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = 180f },
            contentScale = ContentScale.FillBounds,
        )
        BasicTextField(
            value = value,
            onValueChange = {
                onValueChange(it.replace("\r", ""))
                scope.launch { if (scroll.maxValue > 0) scroll.animateScrollTo(scroll.maxValue) }
            },
            textStyle = TextStyle(
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = lineSp,
                color = Color(0xFF1E3A5F),
                textAlign = TextAlign.Right,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom,
                    trim = LineHeightStyle.Trim.None,
                ),
            ),
            cursorBrush = SolidColor(Color(0xFF1E3A5F)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .verticalScroll(scroll),
        )
    }
}
