package com.hamyareman.ir.ui.study

import android.webkit.JavascriptInterface
import androidx.compose.runtime.Composable

@Composable
fun MathLessonScreen(packId: String, initialTab: Int = 0, onBack: () -> Unit) {
    RemoteLessonScreen(packId, initialTab, onBack)
}

internal object TeachSeekMap {
    // منبعِ حقیقت: فهرست‌های زمان‌بندیِ پوشهٔ صوتِ تدریس در Books
    // (ryazif01d01.txt … ryazif01review.txt). این جدول با همان فایل‌ها و با
    // «data-seek-ms»های داخلِ HTMLهای assets همگام است؛ آزمونِ
    // MediaVaultRangeTest هر سه را با هم مقایسه می‌کند تا از هم دور نیفتند.
    private val table: Map<String, List<Long>> = mapOf(
        "C905_E01-L01" to listOf(
            57_000, 221_000, 340_000, 443_000, 525_000,
            674_000, 749_000, 891_000, 1_047_000, 1_495_000,
        ),
        "C905_E01-L02" to listOf(
            60_000, 131_000, 286_000, 457_000, 624_000,
            786_000, 928_000, 1_164_000, 1_636_000,
        ),
        "C905_E01-L03" to listOf(
            70_000, 128_000, 233_000, 462_000, 595_000,
            779_000, 903_000, 972_000, 1_070_000, 1_325_000,
            1_796_000,
        ),
        "C905_E01-L04" to listOf(
            70_000, 137_000, 245_000, 341_000, 405_000,
            525_000, 664_000, 805_000, 885_000, 1_085_000,
            1_681_000,
        ),
        "C905_E01-SUM" to listOf(
            76_000, 172_000, 244_000, 366_000, 454_000,
            552_000, 648_000, 1_184_000, 1_283_000,
        ),
        // فصلِ ۲ — درسِ ۱ (عددهای گویا) و درسِ ۲ (عددهای حقیقی).
        // «بخش صفر: مقدمه»، «استراحت» و «جمع‌بندی»ِ فایلِ .txt در فهرستِ HTML جایی
        // ندارند، پس نگاشت با همترازیِ عنوان‌ها انجام شده (tools/seek-shim/sync_seek_html.py).
        "C905_E02-L01" to listOf(
            65_000, 149_000, 262_000, 365_000, 498_000,
            631_000, 897_000, 1_045_000, 1_234_000, 1_692_000,
        ),
        "C905_E02-L02" to listOf(
            67_000, 149_000, 308_000, 444_000, 573_000,
            774_000, 842_000, 912_000, 1_022_000, 1_433_000,
        ),
        // درسِ ۳ فصلِ ۲ (قدر مطلق) — همان قاعدهٔ همترازیِ عنوان‌ها.
        "C905_E02-L03" to listOf(
            81_000, 167_000, 230_000, 406_000, 568_000,
            878_000, 1_037_000, 1_162_000, 1_301_000, 1_777_000,
        ),
    )
    fun times(packId: String): List<Long> = table[packId].orEmpty()
}

/**
 * شیمِ سیکِ فهرست/سرفصل‌های HTML تدریس:
 *  ۱) زمان‌های [times] (به ترتیبِ فهرست) به لینک‌هایِ بدونِ `data-seek-ms` داده می‌شود؛
 *  ۲) نقشهٔ `#id → ms` از هر عنصرِ زمان‌دار ساخته می‌شود و **سرفصل‌های داخلِ متن**
 *     (`.section-title` / `.subsection-title` / هر `id="secN"`) هم همان زمان را
 *     می‌گیرند و کلیک‌پذیر می‌شوند — یعنی خودِ سرفصل هم پلیر را می‌برد؛
 *  ۳) کنارِ هر آیتمِ فهرست، برچسبِ زمان (`m:ss`) نمایش داده می‌شود (`.toc .t`)؛
 *  ۴) شنوندهٔ کلیک روی هر عنصرِ زمان‌دار (نه فقط `<a>`) `HamyarPlayer.seek(ms)`
 *     را صدا می‌زند و همان بند را اسکرول می‌کند؛ اگر زمانی نبود، از href/متن
 *     (`#t=5:32` یا `۵:۳۲`) استخراج می‌شود.
 */
internal fun ensureSeekShim(html: String, times: List<Long> = emptyList()): String {
    if (html.isBlank()) return html
    val arr = times.joinToString(",") { it.toString() }
    val shim = "<script>(function(){\"use strict\";" +
        "function digits(s){return String(s).replace(/[\u06F0-\u06F9]/g,function(d){return String(d.charCodeAt(0)-0x06F0);})" +
            ".replace(/[\u0660-\u0669]/g,function(d){return String(d.charCodeAt(0)-0x0660);});}" +
        "function parseMs(v){if(v==null)return null;var x=parseInt(digits(v).replace(/[^0-9]/g,''),10);" +
            "return (isNaN(x)||x<0)?null:x;}" +
        "function fa(s){return String(s).replace(/[0-9]/g,function(d){return String.fromCharCode(0x06F0+parseInt(d,10));});}" +
        "function label(ms){var m=Math.floor(ms/60000),s=Math.floor((ms%60000)/1000);return fa(m+':'+(s<10?'0':'')+s);}" +
        "function msOfText(el){" +
        "var h=(el.getAttribute&&el.getAttribute('href'))||'';" +
        "var m=digits(h).match(/(?:t=|#t)([0-9]{1,3}):([0-9]{1,2})(?::([0-9]{1,2}))?/);" +
        "if(m){var a=parseInt(m[1],10),b=parseInt(m[2],10);if(m[3]){a=a*60+b;b=parseInt(m[3],10);}return (a*60+b)*1000;}" +
        "var t=digits((el.textContent||'').slice(0,120)).match(/([0-9]{1,3}):([0-9]{1,2})(?::([0-9]{1,2}))?/);" +
        "if(t){var c=parseInt(t[1],10),d=parseInt(t[2],10);if(t[3]){c=c*60+d;d=parseInt(t[3],10);}return (c*60+d)*1000;}" +
        "return null;}" +
        "var TIMES=[$arr];" +
        "var links=document.querySelectorAll('.toc ol li a, .toc li a, .toc a');" +
        "for(var i=0;i<links.length;i++){var a0=links[i];" +
        "if(!a0.getAttribute('data-seek-ms')&&i<TIMES.length&&TIMES[i]>0){a0.setAttribute('data-seek-ms',String(TIMES[i]));}}" +
        "var map={};" +
        "document.querySelectorAll('[data-seek-ms]').forEach(function(el){" +
        "var ms=parseMs(el.getAttribute('data-seek-ms'));if(ms==null)return;" +
        "var h=el.getAttribute('href')||'';if(h.charAt(0)==='#'&&h.length>1){map[h.slice(1)]=ms;}" +
        "if(el.id){map[el.id]=ms;}});" +
        "document.querySelectorAll('.section-title,.subsection-title,[id^=\"sec\"]').forEach(function(el){" +
        "if(el.getAttribute('data-seek-ms'))return;var ms=map[el.id];if(ms==null)return;" +
        "el.setAttribute('data-seek-ms',String(ms));el.style.cursor='pointer';});" +
        "document.querySelectorAll('.toc a[data-seek-ms]').forEach(function(a){" +
        "if(a.querySelector('.t'))return;var ms=parseMs(a.getAttribute('data-seek-ms'));if(ms==null)return;" +
        "var sp=document.createElement('span');sp.className='t';sp.textContent=label(ms);" +
        "sp.style.marginLeft='6px';a.insertBefore(sp,a.firstChild);});" +
        "function go(el,ms,ev){if(ev)ev.preventDefault();" +
        "try{if(window.HamyarPlayer&&HamyarPlayer.seek){HamyarPlayer.seek(ms);}}catch(_){}" +
        "var id=(el.getAttribute&&el.getAttribute('href'))||'';" +
        "if(id.charAt(0)==='#'&&id.length>1){try{var t=document.querySelector(id);if(t)t.scrollIntoView({behavior:'smooth',block:'start'});}catch(_){}}" +
        "else if(el.id){try{el.scrollIntoView({behavior:'smooth',block:'start'});}catch(_){}}}" +
        "document.addEventListener('click',function(e){" +
        "var el=e.target,hops=0;" +
        "while(el&&hops<6){" +
        "var v=el.getAttribute?el.getAttribute('data-seek-ms'):null;" +
        "if(v!=null){var ms=parseMs(v);if(ms!=null){go(el,ms,e);return;}}" +
        "el=el.parentElement;hops++;}" +
        "var a=e.target;while(a&&a.tagName!=='A')a=a.parentElement;if(!a)return;" +
        "var ms2=msOfText(a);if(ms2!=null){go(a,ms2,e);}" +
        "},true);})();</script>"
    val i = html.lastIndexOf("</body>")
    return if (i >= 0) html.substring(0, i) + shim + html.substring(i) else html + shim
}

/** پل JS فهرست HTML → سیک پلیر تدریس. */
internal class TeachHtmlBridge {
    @JavascriptInterface
    fun seek(ms: Int) {
        TeachSeekBus.seekMs(ms.toLong())
    }
}

