package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hamyareman.ir.platform.feature.study.BookModuleRegistry
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.ui.navigation.Screen

/**
 * هاب «مدرسه» — ساختار تازه:
 *  ۱) همان اول: کتاب‌ها با کاورِ خودشان؛ با انتخاب هر کتاب، فهرست درس‌ها/فصل‌هایش
 *     باز می‌شود (تدریس + مطالعه/آزمون برای هر درس).
 *  ۲) برنامه‌ی هفتگی.
 *  ۳) آزمون و بازخورد (جزوه، نمودار پیشرفت مخصوص دروس مدرسه).
 * پلیرها، کتابخانه، مطالعات آزاد و آموزش هوش به تب «آموزشگاه» منتقل شدند.
 */
@Composable
fun SchoolHubScreen(nav: NavController) {
    // v1.18: آکاردئون منوی مدرسه — هر لحظه فقط یک گروه باز؛ وضعیت آخرین گروه باز حافظه‌دار.
    val ctxHub = LocalContext.current
    val hubStore = remember { LocalStore(ctxHub, "hamyar_hub") }
    var openGroup by remember { mutableStateOf(hubStore.getString("acc_school", "")) }
    fun toggleGroup(id: String) {
        openGroup = if (openGroup == id) "" else id
        hubStore.putString("acc_school", openGroup)
    }

    HubBody {
        HubHeader("مدرسه 🎒", "کلاسِ درس همیشه باز است — هر روز یک قدم با برنامه و درس‌هایت جلو برو", slotId = "hub.school.header")

        val books = remember {
            runCatching {
                com.hamyareman.ir.ui.profile.GradeGate.filter(BookModuleRegistry.modules) { it.bookCode }
            }.getOrDefault(emptyList())
        }
        HubMenuGroup(
            "📚 کتاب‌ها",
            "${books.size} کتاب درسی — هر کتاب با درس‌ها، صوت، ویدیو و آزمونش",
            open = openGroup == "books",
            onToggle = { toggleGroup("books") },
            slotId = "hub.school.group.books",
        ) {
            val ctx = LocalContext.current
            books.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { book ->
                        val cover = remember(book.bookCode) {
                            com.hamyareman.ir.ui.study.PdfSafe.decodeCover(ctx, book.bookCode)
                        }
                        Card(
                            Modifier.weight(1f).clickable { nav.hubTo(Screen.Book.of(book.bookCode)) },
                        ) {
                            Column {
                                if (cover != null) {
                                    Image(
                                        bitmap = cover.asImageBitmap(),
                                        contentDescription = "کاور ${book.title}",
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        book.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        fontFamily = com.hamyareman.ir.ui.appearance.TypeSlots.family("hub.school.item.book"),
                                        fontSize = com.hamyareman.ir.ui.appearance.TypeSlots.size("hub.school.item.book", 13),
                                    )
                                    Text(
                                        "${lessonCount(book)} درس",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = com.hamyareman.ir.ui.appearance.TypeSlots.family("hub.school.item.book"),
                                        fontSize = com.hamyareman.ir.ui.appearance.TypeSlots.size("hub.school.item.book", 11),
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        val gender = HubCatalog.gender()
        HubCatalog.schoolExtra().forEach { group ->
            HubMenuGroup(
                group.title,
                group.subtitle,
                open = openGroup == group.id,
                onToggle = { toggleGroup(group.id) },
                slotId = "hub.school.group.${group.id}",
            ) {
                group.items.forEach { item ->
                    val key = item.route.substringBefore("?").substringBefore("/")
                    HubCard(item.emojiFor(gender), item.title, item.subtitle, slotId = "hub.school.item.$key") { nav.hubTo(item.route) }
                }
            }
        }
    }
}

/**
 * شمارِ درس‌های کتاب برای کارتِ مدرسه.
 *
 * چرا: کتابِ تازه‌اضافه‌شده (مثل «آمادگی دفاعی» و «از من تا خدا») اول فقط فهرست و
 * جلد دارد و پکِ محتوایش بعداً می‌آید؛ اگر فقط `packs.size` را نشان دهیم کارت
 * می‌گوید «۰ درس» در حالی که فهرستِ رسمی پُر است. پس تا وقتی پکی ثبت نشده،
 * از فهرستِ رسمی ([com.hamyareman.ir.platform.feature.study.BookToc]) می‌شماریم.
 */
private fun lessonCount(book: com.hamyareman.ir.platform.feature.study.BookModule): Int {
    fun walk(nodes: List<com.hamyareman.ir.platform.feature.study.BookToc.TocNode>): Int =
        nodes.sumOf { (if (it.packId != null) 1 else 0) + walk(it.children) }
    val fromToc = walk(com.hamyareman.ir.platform.feature.study.BookToc.forBook(book.bookCode))
    // بیشینه: کتاب ممکن است پکی داشته باشد که در فهرست نیست (مرور/جمع‌بندی).
    return maxOf(book.packs.size, fromToc)
}
