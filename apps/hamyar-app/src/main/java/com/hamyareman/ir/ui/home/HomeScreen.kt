package com.hamyareman.ir.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.core.common.JalaliDate
import com.hamyareman.ir.platform.core.common.toPersianDigits
import com.hamyareman.ir.ui.hub.AutoShrinkTileText
import com.hamyareman.ir.ui.hub.HubCard
import com.hamyareman.ir.ui.hub.hubTo
import com.hamyareman.ir.ui.navigation.Screen
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.ui.profile.StudentProfileRepo
import com.hamyareman.ir.ui.profile.StudentProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.util.Calendar
import com.hamyareman.ir.ui.AppTypography

/** مارجینِ کناریِ بلوک‌های داشبورد (کارتِ سخن بزرگان عمداً پهن‌تر و بی‌مارجین‌تر است). */
private val HomeSide = 16.dp

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "صبح‌ت بخیر"
        in 12..16 -> "ظهرت بخیر"
        in 17..20 -> "عصرت بخیر"
        else -> "شبت بخیر"
    }
}

private val gregMonth = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec",
)

@Composable
fun HomeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val container = LocalAppContainer.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        val uid = container.auth.cachedUserId().orEmpty()
        if (uid.isNotBlank()) {
            val remote = withContext(Dispatchers.IO) { StudentProfileRepo.fetch(container.tables, uid) }
            if (remote != null) StudentProfileState.applyServer(ctx, remote)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val time = remember(now) { LocalDateTime.ofInstant(Instant.ofEpochMilli(now), JalaliDate.TEHRAN) }
    val jalali = remember(now) { JalaliDate.toJalali(now) }
    val iso = remember(now) { JalaliDate.todayIso() }
    val weekday = JalaliDate.weekDayFa(iso)
    val row1 = toPersianDigits("$weekday ${jalali.day} ${JalaliDate.monthName(jalali.month)} ${jalali.year}")
    val h24 = time.hour
    val h12 = val12(h24)
    val period = if (h24 < 12) "قبل از ظهر" else "بعد از ظهر"
    val row2Time = toPersianDigits("%d:%02d".format(h12, time.minute)) + " $period"
    val row2Greg = "${time.year}/${gregMonth[time.monthValue - 1]}/${time.dayOfMonth}"
    val holiday = CalendarOccasions.dashboardLine(ctx, jalali)
    val weekendNotice = when (time.dayOfWeek) {
        java.time.DayOfWeek.FRIDAY -> "جمعه تعطیل هفتگی" to Color(0xFFB91C1C)
        java.time.DayOfWeek.THURSDAY -> "پنجشنبه تعطیلی مدرسه" to Color(0xFF1D4ED8)
        else -> null
    }
    val occasionSize = (AppTypography.d3Date.size.value - 3f).coerceAtLeast(8f).sp
    val occasionFont = com.hamyareman.ir.ui.appearance.EmbeddedFonts.family(
        "vazirmatn",
        com.hamyareman.ir.ui.appearance.EmbeddedFonts.W_BOLD,
    )
    val who = StudentProfileState.firstName.ifBlank { "دوست من" }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { nav.navigate(Screen.BetweenLessons.route) }) { Text("💛") }
    }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(vertical = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GreetingBanner(
                title = "${greeting()} $who جان",
                subtitle = "همیار من کنارت است؛ از مدرسه تا آرامش",
                modifier = Modifier.padding(horizontal = HomeSide),
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HomeSide),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(Modifier.weight(1f).padding(end = 36.dp)) {
                                Text(
                                    row1,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = AppTypography.d3Date.family, fontWeight = AppTypography.d3Date.weight,
                                    fontSize = AppTypography.d3Date.size,
                                    modifier = Modifier.clickable { nav.navigate(Screen.ClassPlanCalendar.route) },
                                )
                                Text(
                                    row2Time,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = AppTypography.d4Clock.family, fontWeight = AppTypography.d4Clock.weight,
                                    fontSize = AppTypography.d4Clock.size,
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        row2Greg,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontFamily = AppTypography.d5Gregorian.family, fontWeight = AppTypography.d5Gregorian.weight,
                                        fontSize = AppTypography.d5Gregorian.size,
                                    )
                                }
                                weekendNotice?.let { (label, color) ->
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = occasionFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = occasionSize,
                                        color = color,
                                    )
                                }
                                if (!holiday.isNullOrBlank()) {
                                    Text(
                                        holiday,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = occasionFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = occasionSize,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(28.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ProfileClockAvatar(onClick = { nav.navigate(Screen.UserProfile.route) })
                            Spacer(Modifier.height(6.dp))
                            SubscriptionChip(StudentProfileState.subscription) { nav.navigate(Screen.Subscription.route) }
                        }
                    }
                }
            }

            WisdomCard(Modifier.padding(horizontal = AppTypography.quoteSide))

            Row(Modifier.fillMaxWidth().padding(horizontal = HomeSide), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🎒", "مدرسه", Modifier.weight(1f)) { nav.hubTo(Screen.Study.route) }
                QuickTile("📖", "کتاب متنی و صوتی", Modifier.weight(1f)) { nav.navigate(Screen.FreeReading.route) }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = HomeSide), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("🪷", "ذهن‌آگاهی", Modifier.weight(1f)) { nav.hubTo(Screen.AwarenessHub.route) }
                QuickTile("🌙", "کسب آرامش", Modifier.weight(1f)) { nav.navigate(Screen.CalmHub.route) }
            }

            ClassPlanCard(
                modifier = Modifier.padding(horizontal = HomeSide),
                onOpenPlan = { nav.navigate(Screen.ClassPlan.route) },
                onOpenPrep = { nav.navigate(Screen.TomorrowPrep.route) },
                onOpenAlarm = { nav.navigate(Screen.ClassPlanShift.route) },
                onOpenLeave = { nav.navigate(Screen.Leave.route) },
            )

            // آزمایشگاه فقط وقتی برای edition همین پایه فایل دوآدرسی واقعی دارد
            // نمایش داده می‌شود؛ پایهٔ دیگر هرگز به آزمایشگاه نهم وصل نمی‌شود.
            val toolTiles = buildList {
                // بعد از مهاجرت به پارس‌پک، هیچ‌کدام از این ابزارها فایل ندارند؛
                // کارت فقط وقتی ساخته می‌شود که دستهٔ متناظرش در کاتالوگ آیتم داشته باشد.
                if (com.hamyareman.ir.ui.tools.ToolRemote.hasCategory(ctx, "tool-gen")) {
                    add(Triple("🧰", "جعبه‌ابزار عمومی", Screen.GeneralToolkit.route))
                }
                if (com.hamyareman.ir.ui.tools.ToolRemote.isAvailable("chemistry")) {
                    add(Triple("⚗️", "آزمایشگاه شیمی", Screen.ChemistryLab.route))
                }
                if (com.hamyareman.ir.ui.tools.ToolRemote.isAvailable("physics")) {
                    add(Triple("🔬", "آزمایشگاه فیزیک", Screen.PhysicsLab.route))
                }
                if (com.hamyareman.ir.ui.tools.ToolRemote.hasCategory(ctx, "tool-math")) {
                    add(Triple("🧮", "جعبه‌ابزار ریاضی", Screen.MathToolkit.route))
                }
                if (com.hamyareman.ir.ui.tools.ToolRemote.isAvailable("biology")) {
                    add(Triple("🧬", "آزمایشگاه زیست‌شناسی", Screen.BiologyLab.route))
                }
            }
            toolTiles.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = HomeSide),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { tile ->
                        ToolTile(tile.first, tile.second, Modifier.weight(1f)) { nav.navigate(tile.third) }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Text(
                "امروز",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = AppTypography.d9Section.family, fontWeight = AppTypography.d9Section.weight,
                fontSize = AppTypography.d9Section.size,
                modifier = Modifier.padding(horizontal = HomeSide),
            )
            HubCard("🌤", "روتین امروز", "بلوک‌های روزت را ببین", Modifier.padding(horizontal = HomeSide), slotId = "page.home.tile") { nav.navigate(Screen.Routine.route) }
            HubCard("💧", "آب بنوش", "لیوان‌های امروزت را ثبت کن", Modifier.padding(horizontal = HomeSide), slotId = "page.home.tile") { nav.navigate(Screen.Water.route) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GreetingBanner(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0F766E), Color(0xFF115E59), Color(0xFF1E3A8A)),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    color = Color.White,
                    fontFamily = AppTypography.d1Greeting.family, fontWeight = AppTypography.d1Greeting.weight,
                    fontSize = AppTypography.d1Greeting.size,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.92f),
                    fontFamily = AppTypography.d2GreetingSub.family, fontWeight = AppTypography.d2GreetingSub.weight,
                    fontSize = AppTypography.d2GreetingSub.size,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun WisdomCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var all by remember { mutableStateOf(WisdomQuotes.load(ctx)) }
    var line by remember { mutableStateOf(WisdomQuotes.current(ctx, all)) }
    LaunchedEffect(Unit) {
        if (WisdomQuotes.refresh(ctx)) {
            all = WisdomQuotes.load(ctx)
            line = WisdomQuotes.current(ctx, all)
        }
    }
    LaunchedEffect(line, all) {
        delay(WisdomQuotes.remainingMs(ctx))
        line = WisdomQuotes.current(ctx, all)
    }
    val body = line.oneLine().replace('\n', ' ').replace("  ", " ").trim()

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111633), Color(0xFF1E1B4B), Color(0xFF3B0764)),
                ),
            )
            .clickable { line = WisdomQuotes.next(ctx, all) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        AutoFitQuote(
            text = body,
            fontFamily = AppTypography.d7Quote.family, fontWeight = AppTypography.d7Quote.weight,
            color = Color(0xFFFDE68A),
            modifier = Modifier.fillMaxWidth().height(36.dp),
        )
    }
}

/**
 * یک سطر، اندازهٔ فونت خودکار تا کل کادر را پر کند.
 */
@Composable
private fun AutoFitQuote(
    text: String,
    fontFamily: FontFamily,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val maxW = constraints.maxWidth
        val sizeSp = remember(text, maxW, fontFamily, fontWeight) {
            if (maxW <= 0) 16f
            else {
                var lo = 11f
                var hi = 30f
                var best = 12f
                repeat(12) {
                    val mid = (lo + hi) / 2f
                    val layout = measurer.measure(
                        text = AnnotatedString(text),
                        style = TextStyle(
                            fontSize = mid.sp,
                            fontFamily = fontFamily,
                            fontWeight = fontWeight,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false,
                        constraints = Constraints(maxWidth = maxW),
                    )
                    val fits = !layout.didOverflowWidth && layout.size.width <= maxW
                    if (fits) {
                        best = mid
                        lo = mid
                    } else {
                        hi = mid
                    }
                }
                best
            }
        }
        Text(
            text,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = sizeSp.sp,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun val12(h24: Int): Int {
    val h = h24 % 12
    return if (h == 0) 12 else h
}

@Composable
/**
 * برچسب وضعیت اشتراک — قابل لمس است و به صفحه‌ی اشتراک می‌رود
 * (پیش‌تر فقط متن بود و هیچ‌جا لینک نمی‌شد).
 */
internal fun SubscriptionChip(raw: String, onClick: () -> Unit = {}) {
    val s = com.hamyareman.ir.platform.core.common.BillingStatus.effective(raw, com.hamyareman.ir.ui.profile.StudentProfileState.subscriptionEndMs)
    val paid = com.hamyareman.ir.platform.core.common.BillingStatus.isPaid(raw, com.hamyareman.ir.ui.profile.StudentProfileState.subscriptionEndMs)
    val pending = s == com.hamyareman.ir.platform.core.common.BillingStatus.PENDING
    val refunding = s == com.hamyareman.ir.platform.core.common.BillingStatus.REFUND_PENDING
    val bg = when {
        pending -> Color(0xFFFEF3C7)
        refunding -> Color(0xFFE0E7FF)
        paid -> Color(0xFFDCFCE7)
        else -> Color(0xFFFEE2E2)
    }
    val fg = when {
        pending -> Color(0xFF92400E)
        refunding -> Color(0xFF3730A3)
        paid -> Color(0xFF166534)
        else -> Color(0xFFB91C1C)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        onClick = onClick,
    ) {
        Text(
            com.hamyareman.ir.platform.core.common.BillingStatus.chipFa(s),
            color = fg,
            fontFamily = AppTypography.d6Subscription.family, fontWeight = AppTypography.d6Subscription.weight,
            fontSize = AppTypography.d6Subscription.size,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/**
 * کارتِ کم‌عرضِ داشبورد برای جعبه‌ابزارها — چهار عدد در یک ردیف؛
 * عنوان همیشه یک سطر است و فقط برای جا شدن کوچک می‌شود.
 */
@Composable
private fun ToolTile(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.height(73.dp),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            AutoShrinkTileText(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = AppTypography.d8Tile.family,
                    fontWeight = AppTypography.d8Tile.weight,
                    fontSize = AppTypography.d8Tile.size,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun QuickTile(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(67.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            AutoShrinkTileText(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = AppTypography.d8Tile.family,
                    fontWeight = AppTypography.d8Tile.weight,
                    fontSize = AppTypography.d8Tile.size,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}
