// تستِ واقعیِ شیمِ سیک روی DOM (jsdom) — همان HTMLهایی که در اپ رندر می‌شوند.
// اجرا:  python3 tools/seek-shim/extract_shim.py > /tmp/shim.js
//         npm i jsdom && node tools/seek-shim/test_seek_shim.js
//
// زمان‌های انتظار از «tools/seek-shim/expected-seek.json» خوانده می‌شوند (خروجیِ
// `sync_seek_html.py`). چرا نه مستقیم از .txt: تعدادِ آیتم‌های .txt با تعدادِ
// آیتم‌های فهرستِ HTML یکی نیست («بخش صفر: مقدمه»، «استراحت» و گاهی «جمع‌بندی»
// در HTML جایی ندارند)، پس همترازیِ عنوان‌ها یک‌بار انجام و ثبت می‌شود. این تست
// افزون بر آن بررسی می‌کند که انتظار، زیردنبولهٔ فزایندهٔ خودِ .txt باشد تا
// با به‌روزرسانیِ .txt، انتظارِ کهنه سبز نماند.
const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const shimRaw = fs.readFileSync('/tmp/shim.js', 'utf8');
const base = path.join(__dirname, '..', '..', 'apps/hamyar-app/src/main/assets/math/c905/');
const AUD = path.join(__dirname, '..', '..', 'Books/Base-09/ریاضی/04- صوت تدریس/');
const EXPECTED_JSON = path.join(__dirname, 'expected-seek.json');
// TeachSeekMap یک object داخلی در MathLessonScreen.kt است (فایلِ جدا ندارد).
const SEEK_MAP = path.join(__dirname, '..', '..',
  'apps/hamyar-app/src/main/java/com/hamyareman/ir/ui/study/MathLessonScreen.kt');

// پارسرِ بردبار: ارقامِ فارسی/عربی، فاصلهٔ اختیاری کنارِ دونقطه، ثانیهٔ ۱ یا ۲ رقمی
// («۲:۸» یعنی ۲ دقیقه و ۸ ثانیه؛ «۱۴ :۴۵» هم قبول است).
const FA = { '۰':'0','۱':'1','۲':'2','۳':'3','۴':'4','۵':'5','۶':'6','۷':'7','۸':'8','۹':'9',
             '٠':'0','١':'1','٢':'2','٣':'3','٤':'4','٥':'5','٦':'6','٧':'7','٨':'8','٩':'9' };
function parseTimingList(text) {
  const out = [];
  for (const line of String(text).split('\n')) {
    const s = line.replace(/[۰-۹٠-٩]/g, (d) => FA[d]);
    const m = s.match(/(\d{1,3})\s*:\s*(\d{1,2})(?!\d)/);
    if (m && Number(m[2]) < 60) out.push(Number(m[1]) * 60000 + Number(m[2]) * 1000);
  }
  return out;
}

// انتظارات از JSONِ همگام‌شده (همان چیزی که در HTML و TeachSeekMap پخته شده).
const expect = {};
if (fs.existsSync(EXPECTED_JSON)) {
  const j = JSON.parse(fs.readFileSync(EXPECTED_JSON, 'utf8'));
  for (const name of Object.keys(j).sort()) {
    if (Array.isArray(j[name].ms) && j[name].ms.length > 0 && fs.existsSync(base + name + '.html')) {
      expect[name] = j[name].ms;
    }
  }
}

// کلیدِ بستهٔ صوت از رویِ نامِ فایل (همان قاعدهٔ StudyMedia/ExtraLessons).
function packOf(name) {
  let m = /^ryazif(\d{2})d(\d{2})$/.exec(name);
  if (m) return 'C905_E' + m[1] + '-L' + m[2];
  m = /^ryazif(\d{2})review$/.exec(name);
  if (m) return 'C905_E' + m[1] + '-SUM';
  return null;
}

// زمان‌های یک کلید از جدولِ TeachSeekMap (بدونِ کامپایلِ کاتلین). عددها در
// کاتلین با زیرخط گروه‌بندی شده‌اند («1_047_000»)؛ آن‌ها را هم می‌خواند.
function seekMapTimes(pack) {
  if (!pack || !fs.existsSync(SEEK_MAP)) return [];
  const src = fs.readFileSync(SEEK_MAP, 'utf8');
  const head = '"' + pack + '" to listOf(';
  const at = src.indexOf(head);
  if (at < 0) return [];
  const body = src.slice(at + head.length).split(')')[0];
  const nums = body.match(/\d[\d_]*/g);
  return nums ? nums.map((x) => Number(x.replace(/_/g, ''))) : [];
}

// آیا sub زیردنبولهٔ (نه لزوماً پیوستهٔ) فزایندهٔ all است؟
function isSubsequence(sub, all) {
  let i = 0;
  for (const v of all) { if (i < sub.length && sub[i] === v) i++; }
  return i === sub.length;
}

function run(html, times) {
  const calls = [];
  const dom = new JSDOM(html, { runScripts: 'outside-only', url: 'https://local.hamyar/' });
  const w = dom.window;
  w.HamyarPlayer = { seek: (ms) => calls.push(Number(ms)) };
  w.Element.prototype.scrollIntoView = function () {};
  w.eval(shimRaw.replace(/var TIMES=\[[^\]]*\];/, `var TIMES=[${times.join(',')}];`));
  const d = w.document;
  const click = (el) => { calls.length = 0; el.dispatchEvent(new w.MouseEvent('click', { bubbles: true, cancelable: true })); return calls[0] ?? null; };
  return {
    toc: [...d.querySelectorAll('.toc a')].map((a, i) => ({ i, ms: click(a), label: a.querySelector('.t')?.textContent ?? null })),
    titles: [...d.querySelectorAll('.section-title')].map((el) => ({ id: el.id, ms: click(el) })),
  };
}

let fail = 0;
const names = Object.keys(expect);
if (names.length === 0) {
  console.log('⚠️  هیچ انتظاری در expected-seek.json پیدا نشد — تست رد شد (نه شکست).');
  process.exit(0);
}
for (const name of names) {
  const exp = expect[name];
  const html = fs.readFileSync(base + name + '.html', 'utf8');
  // انتظارِ ثبت‌شده باید در خودِ فهرستِ Books هم پیدا شود (نگهبانِ کهنگیِ JSON).
  const txtPath = path.join(AUD, name + '.txt');
  if (fs.existsSync(txtPath)) {
    const list = parseTimingList(fs.readFileSync(txtPath, 'utf8'));
    const sub = isSubsequence(exp, list);
    if (!sub) { fail++; console.log(`❌ ${name} [books] انتظار زیردنبولهٔ فهرستِ Books نیست`); }
    else console.log(`✅ ${name} [books] انتظار در فهرستِ Books هست (${exp.length}/${list.length})`);
  }
  // و TeachSeekMap هم باید همان زمان‌ها را داشته باشد (یا خالی باشد).
  const mapped = seekMapTimes(packOf(name));
  if (mapped.length > 0 && JSON.stringify(mapped) !== JSON.stringify(exp)) {
    fail++;
    console.log(`❌ ${name} [map-kotlin] TeachSeekMap با انتظار یکی نیست: ${mapped.join(',')}`);
  } else if (mapped.length > 0) {
    console.log(`✅ ${name} [map-kotlin] TeachSeekMap همگام است`);
  }
  for (const [mode, doc, times] of [['html', html, []], ['map', html.replace(/ data-seek-ms="\d+"/g, ''), exp]]) {
    const r = run(doc, times);
    const tocOk = exp.length > 0 && exp.every((ms, i) => r.toc[i] && r.toc[i].ms === ms);
    const titleOk = r.titles.length > 0 && r.titles.every((t) => t.ms != null);
    const ok = tocOk && titleOk;
    if (!ok) fail++;
    console.log(`${ok ? '✅' : '❌'} ${name} [${mode}] toc=${r.toc.length} titles=${r.titles.length} ` +
      `tocTimes=${tocOk} titleSeek=${titleOk} sampleLabel=${r.toc[0]?.label}`);
    if (!ok) console.log('   ', JSON.stringify(r.toc.slice(0, 3)), JSON.stringify(r.titles.slice(0, 3)), 'expected:', exp.join(','));
  }
}
// درس‌هایی که HTMLشان هست ولی هنوز فهرستِ زمان ندارند: فقط «دود نکردن» بررسی می‌شود.
for (const f of fs.readdirSync(base)) {
  if (!/^ryazif.+\.html$/.test(f)) continue;
  const name = f.replace(/\.html$/, '');
  if (expect[name]) continue;
  const html = fs.readFileSync(base + f, 'utf8');
  const r = run(html, []);
  // این درس‌ها هنوز فهرستِ زمان ندارند؛ فقط «ساختار سالم» بررسی می‌شود.
  // (سه درسِ فصلِ ۳ اساساً «.toc» ندارند؛ وقتی فهرستشان آمد اینجا هم سبز می‌شود.)
  const ok = r.titles.length > 0;
  if (!ok) fail++;
  console.log(`${ok ? '🕓' : '❌'} ${name} [بدون زمان] toc=${r.toc.length} titles=${r.titles.length} — منتظرِ فهرستِ زمان`);
}
console.log(fail === 0 ? '\n✅ همه‌ی بررسی‌ها پاس شد' : `\n❌ ${fail} مورد ناموفق`);
process.exit(fail === 0 ? 0 : 1);
