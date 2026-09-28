/**
 * اجرای سناریوهای تست توابع سرور با SDK ساختگی.
 *
 *   cd backend/functions/tests && node run-tests.js
 *
 * کد خروج ۰ یعنی همه پاس؛ ۱ یعنی حداقل یک سناریو شکست (مناسب CI).
 */
const path = require('node:path');
const fs = require('node:fs');
const Module = require('node:module');

// توابع `require('node-appwrite')` می‌کنند؛ بدون نصب، stub خودمان را می‌دهیم.
const STUB = path.join(__dirname, 'stub-appwrite.js');
const originalLoad = Module._load;
Module._load = function (request, parent, isMain) {
  if (request === 'node-appwrite') return require(STUB);
  return originalLoad.apply(this, arguments);
};

const FN = path.join(__dirname, '..');
const SEED = path.join(__dirname, '..', '..', '..', 'backend', 'seed', 'content.json');
const content = JSON.parse(fs.readFileSync(fs.existsSync(SEED) ? SEED : path.join(__dirname, '..', '..', 'seed', 'content.json'), 'utf8'));
const store = global.__AW_STORE__ = {};
const TODAY = new Date().toISOString().slice(0, 10);

// فیکسچرها
store.father_links = [{ $id: 'link1', data: { zahraId: 'z1', fatherId: 'f1', status: 'active', linkedAt: 1 } }];
store.user_settings = [{ $id: 's1', data: { userId: 'z1', weeklyOptIn: true } },
                       { $id: 's2', data: { userId: 'z2', weeklyOptIn: false } }];
store.water_logs = [{ $id: 'w1', data: { ownerId: 'z1', dayIso: TODAY, glasses: 3 } }];
store.routine_blocks = [{ $id: 'r1', data: { ownerId: 'z1', dayIso: TODAY, done: true } },
                        { $id: 'r2', data: { ownerId: 'z1', dayIso: TODAY, done: true } },
                        { $id: 'r3', data: { ownerId: 'z1', dayIso: TODAY, done: false } }];
store.exercise_logs = [{ $id: 'e1', data: { ownerId: 'z1', dayIso: TODAY, minutes: 20 } }];
store.badges = [{ $id: 'b1', data: { ownerId: 'z1', dayIso: TODAY } }];
store.album_items = [
  { $id: 'item1', data: { itemId: 'item1', title: 'خاطره‌ی پدر', addedBy: 'FATHER', addedById: 'f1', ownerId: 'z1', approved: false, createdAtMs: 1 } },
  { $id: 'item2', data: { itemId: 'item2', title: 'خاطره‌ی زهرا', addedBy: 'ZAHRA', addedById: 'z1', ownerId: 'z1', approved: true, createdAtMs: 2 } },
];
store.lessons = content.lessons.map((l) => ({ $id: l.id, data: { title: l.title, subject: l.subject || '', grade: l.grade || 0, body: l.body || '' } }));
store.quizzes = content.quizzes.map((q) => ({ $id: q.id, data: { lessonId: q.lessonId || '', question: q.question, choices: JSON.stringify(q.choices || []), answerIndex: q.answerIndex || 0 } }));
store.learning_nodes = content.learningNodes.map((n) => ({ $id: n.id, data: { title: n.title, track: n.track || '', orderIndex: n.orderIndex || 0, prerequisiteId: n.prerequisiteId || '' } }));
store.father_messages = [];
store.weekly_summaries = [];

function call(name, { userId = 'z1', body = {} } = {}) {
  const fn = require(path.join(FN, name, 'src/main.js'));
  const req = { userId, headers: {}, body: JSON.stringify(body) };
  let out = null;
  const res = { json: (payload, code = 200) => { out = { payload, code }; } };
  return fn(req, res).then(() => out);
}
let pass = 0, fail = 0;
const check = (label, cond, extra = '') => { if (cond) { pass++; console.log('  ✓', label); } else { fail++; console.log('  ✗', label, extra); } };

(async () => {
  console.log('— notify-guardian');
  let r = await call('notify-guardian', { body: { kind: 'crisis', note: 'حالم خوب نیست' } });
  check('هشدار ساخته شد', r.payload.ok === true && String(r.payload.alertId).startsWith('alert-'), JSON.stringify(r.payload));
  check('سطر پیام برای پدر با متن سرور', store.father_messages.length === 1 &&
        store.father_messages[0].data.toUserId === 'f1' &&
        store.father_messages[0].data.text.includes('حالا زنگ بزن') &&
        store.father_messages[0].data.text.includes('حالم خوب نیست'),
        JSON.stringify(store.father_messages[0] && store.father_messages[0].data));
  check('دسترسی‌ها: خواندن برای هر دو طرف', (store.father_messages[0].$permissions || []).some((p) => p.includes('user:z1')) &&
        (store.father_messages[0].$permissions || []).some((p) => p.includes('user:f1')));
  check('شماره‌های اضطراری برگشت', Array.isArray(r.payload.helplines) && r.payload.helplines.length === 4);
  r = await call('notify-guardian', { body: { kind: 'help' } });
  check('محدودیت نرخ → تکراری ساخته نشد', r.payload.ok === true && r.payload.deduped === true && store.father_messages.length === 1);
  r = await call('notify-guardian', { userId: 'z9', body: { kind: 'help' } });
  check('بدون پیوند → no_link ولی شماره‌ها هست', r.payload.ok === false && r.payload.error === 'no_link' && r.payload.helplines.length === 4 && r.code === 200);
  r = await call('notify-guardian', { userId: '', body: {} });
  check('بدون کاربر → ۴۰۱', r.code === 401);
  r = await call('notify-guardian', { body: { kind: 'ناشناخته' } });
  check('نوع ناشناخته → به help برمی‌گردد (سقوط امن)', r.payload.ok === true && r.payload.deduped === true);

  console.log('— lesson-of-the-day');
  r = await call('lesson-of-the-day', { userId: '', body: {} });
  check('درس امروز انتخاب شد', r.payload.ok === true && !!r.payload.lesson.id && r.payload.mode === 'next', JSON.stringify(r.payload).slice(0, 200));
  check('اولین درسِ ناتمام = کمترین grade', r.payload.lesson.grade === Math.min(...store.lessons.map((l) => l.data.grade)));
  check('جایگاه در مسیر', r.payload.position.index === 1 && r.payload.position.total === store.lessons.length);
  check('گره‌ی نقشه‌ی راه برگشت', !!r.payload.node && !!r.payload.node.id);
  const first = r.payload.lesson.id;
  r = await call('lesson-of-the-day', { userId: '', body: { done: [first] } });
  check('با done، درس بعدی می‌آید', r.payload.lesson.id !== first && r.payload.position.index === 2);
  const ai = await call('lesson-of-the-day', { userId: '', body: { track: 'هوش مصنوعی' } });
  check('مسیر «هوش مصنوعی» فیلتر می‌شود', ai.payload.ok === true && ai.payload.lesson.subject === 'هوش مصنوعی' && ai.payload.track === 'هوش مصنوعی');
  check('آزمون‌های همان درس شمرده شد', ai.payload.quizCount === store.quizzes.filter((q) => q.data.lessonId === ai.payload.lesson.id).length);
  const allDone = await call('lesson-of-the-day', { userId: '', body: { done: store.lessons.map((l) => l.$id) } });
  check('همه تمام‌شده → حالت مرور', allDone.payload.mode === 'review' && !!allDone.payload.lesson.id);
  const badTrack = await call('lesson-of-the-day', { userId: '', body: { track: 'مسیرِ وجود ندارد' } });
  check('مسیر ناموجود → خالی نمی‌ماند', badTrack.payload.ok === true && !!badTrack.payload.lesson.id);

  console.log('— catalog-digest');
  r = await call('catalog-digest', { userId: '', body: {} });
  check('digest شانزده‌نویسی', r.payload.ok === true && /^[0-9a-f]{16}$/.test(r.payload.digest), JSON.stringify(r.payload).slice(0, 160));
  check('شمارش جدول‌ها', r.payload.tables.lessons.count === store.lessons.length && r.payload.tables.quizzes.count === store.quizzes.length);
  check('جدولِ ساخته‌نشده → count=-1 نه خطا', r.payload.tables.recipes.count === -1);
  const d1 = r.payload.digest;
  r = await call('catalog-digest', { userId: '', body: {} });
  check('پایدار در اجرای دوباره', r.payload.digest === d1);
  store.lessons.push({ $id: 'lesson-new', data: { title: 'تازه', subject: 'هوش مصنوعی', grade: 9, body: '...' } });
  r = await call('catalog-digest', { userId: '', body: {} });
  check('با محتوای تازه عوض می‌شود', r.payload.digest !== d1);
  r = await call('catalog-digest', { userId: '', body: { tables: ['lessons'] } });
  check('فیلتر جدول‌ها', r.payload.ok === true && Object.keys(r.payload.tables).join() === 'lessons');

  console.log('— daily-checkin');
  r = await call('daily-checkin', { body: {} });
  check('خلاصه‌ی امروز ساخته شد', r.payload.ok === true && r.payload.summary.glasses === 3 &&
        r.payload.summary.routineDone === 2 && r.payload.summary.exerciseMinutes === 20 &&
        r.payload.summary.badgeCount === 1, JSON.stringify(r.payload));
  check('روتینِ انجام‌نشده شمرده نشد', r.payload.summary.routineDone === 2);
  check('یک سطر با شناسه‌ی deterministic', store.weekly_summaries.length === 1 &&
        store.weekly_summaries[0].$id === `checkin-z1-${TODAY}`);
  r = await call('daily-checkin', { body: {} });
  check('اجرای دوباره → به‌روزرسانی نه سطر تازه', r.payload.ok === true && r.payload.updated === true && store.weekly_summaries.length === 1);
  r = await call('daily-checkin', { userId: 'z2', body: {} });
  check('بدون opt-in → چیزی ساخته نمی‌شود', r.payload.ok === false && r.payload.error === 'opt_out' && store.weekly_summaries.length === 1);
  r = await call('daily-checkin', { userId: 'z3', body: {} });
  check('بدون تنظیمات → opt_out', r.payload.ok === false && r.payload.error === 'opt_out');
  r = await call('daily-checkin', { userId: '', body: {} });
  check('بدون کاربر → ۴۰۱', r.code === 401);

  console.log('— album-consent');
  r = await call('album-consent', { body: { action: 'approve', itemId: 'item1' } });
  check('تأیید توسط صاحب آلبوم', r.payload.ok === true && r.payload.approved === true &&
        store.album_items[0].data.approved === true, JSON.stringify(r.payload));
  r = await call('album-consent', { userId: 'f1', body: { action: 'approve', itemId: 'item1' } });
  check('پدر نمی‌تواند خودش تأیید کند', r.code === 403 && r.payload.error === 'not_owner');
  r = await call('album-consent', { body: { action: 'reject', itemId: 'item1' } });
  check('رد کردن', r.payload.ok === true && r.payload.approved === false && store.album_items[0].data.approved === false);
  r = await call('album-consent', { body: { action: 'approve', itemId: 'item2' } });
  check('خاطره‌ی خودِ زهرا نیاز به تأیید ندارد', r.code === 409 && r.payload.error === 'not_pending');
  r = await call('album-consent', { body: { action: 'approve', itemId: 'nope' } });
  check('شناسه‌ی ناموجود → ۴۰۴', r.code === 404);
  r = await call('album-consent', { body: { action: 'delete-everything', itemId: 'item1' } });
  check('عمل نامعتبر → ۴۰۰', r.code === 400);

  console.log(`\n${pass} پاس / ${fail} شکست`);
  process.exit(fail ? 1 : 0);
})();
