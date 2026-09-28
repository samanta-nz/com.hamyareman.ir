/**
 * lesson-of-the-day — انتخاب «درس امروز» با اختیار سرور.
 *
 * چرا سرور؟
 *   - انتخاب درس بین دو دستگاه (موبایل/تبلت) و دو اپ یکسان می‌ماند.
 *   - ترتیب و پیش‌نیازها از کاتالوگ سرور خوانده می‌شود، نه از کش کلاینت؛ پس
 *     دستکاری کش دستگاه نمی‌تواند کاربر را از «مبتدی» مستقیم به «حرفه‌ای» ببرد.
 *   - ساعت دستگاه قابل تغییر است؛ روز از سرور می‌آید.
 *
 * ورودی: { "track": "هوش مصنوعی" (اختیاری), "done": ["lesson-a", ...] (اختیاری) }
 *   `done` فقط شناسه‌ی درس‌های تمام‌شده است — هیچ داده‌ی خصوصی دیگری رد و بدل نمی‌شود.
 * خروجی: { ok, dayIso, track, lesson:{id,title,subject,grade,body}, quizCount,
 *          mode:"next"|"review", position:{index,total}, node:{...}|null }
 */
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const LESSONS = 'lessons';
const QUIZZES = 'quizzes';
const NODES = 'learning_nodes';

function adminClient() {
  return new sdk.Client()
    .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
    .setKey(process.env.APPWRITE_FUNCTION_API_KEY);
}

function tablesService(client) {
  const service = sdk.TablesDB ? new sdk.TablesDB(client) : new sdk.Databases(client);
  return {
    list: service.listRows
      ? (db, table, queries) => service.listRows(db, table, queries)
      : (db, table, queries) => service.listDocuments(db, table, queries),
  };
}

/** بر خلاف rowsOf، شناسه‌ی سطر هم لازم است (درس‌ها با $id ارجاع داده می‌شوند). */
function rowsWithIds(result) {
  const items = (result && (result.rows || result.documents)) || [];
  return items.map((row) => Object.assign({ id: row.$id }, row.data || row));
}

function parseBody(req) {
  if (!req.body) return {};
  if (typeof req.body === 'string') {
    try { return JSON.parse(req.body); } catch (e) { return {}; }
  }
  return req.body;
}

function dayIso(d = new Date()) {
  return new Date(d).toISOString().slice(0, 10);
}

function dayOfYear(iso) {
  const start = new Date(`${iso.slice(0, 4)}-01-01T00:00:00Z`).getTime();
  const now = new Date(`${iso}T00:00:00Z`).getTime();
  return Math.floor((now - start) / 86400000);
}

module.exports = async function (req, res) {
  const { Query } = sdk;
  const tables = tablesService(adminClient());
  const body = parseBody(req);
  const track = String(body.track || '').trim();
  const done = new Set(Array.isArray(body.done) ? body.done.map(String) : []);
  const iso = dayIso();

  try {
    let lessons = rowsWithIds(await tables.list(DATABASE_ID, LESSONS, [Query.limit(500)]));
    if (track) {
      const filtered = lessons.filter((l) => String(l.subject || '') === track);
      // اگر مسیر خواسته‌شده در کاتالوگ نبود، کاربر را با صفحه‌ی خالی رها نمی‌کنیم.
      if (filtered.length) lessons = filtered;
    }
    if (!lessons.length) return res.json({ ok: false, error: 'empty_catalog', dayIso: iso });

    // مرتب‌سازی deterministic: سطح (مبتدی→حرفه‌ای)، بعد شناسه.
    lessons.sort((a, b) => (Number(a.grade) || 0) - (Number(b.grade) || 0)
      || String(a.id).localeCompare(String(b.id)));

    const firstUnfinished = lessons.findIndex((l) => !done.has(String(l.id)));
    const mode = firstUnfinished >= 0 ? 'next' : 'review';
    const index = firstUnfinished >= 0 ? firstUnfinished : dayOfYear(iso) % lessons.length;
    const lesson = lessons[index];

    const quizzes = rowsWithIds(await tables.list(DATABASE_ID, QUIZZES, [Query.limit(500)]));
    const quizCount = quizzes.filter((q) => String(q.lessonId || '') === String(lesson.id)).length;

    // گره‌ی نقشه‌ی راه هم‌مسیر، برای نشان‌دادن جای کاربر در مسیر یادگیری.
    const nodes = rowsWithIds(await tables.list(DATABASE_ID, NODES, [Query.limit(500)]))
      .filter((n) => !track || String(n.track || '') === track)
      .sort((a, b) => (Number(a.orderIndex) || 0) - (Number(b.orderIndex) || 0));
    const node = nodes.find((n) => !done.has(String(n.id))) || nodes[0] || null;

    return res.json({
      ok: true,
      dayIso: iso,
      track: track || String(lesson.subject || ''),
      lesson: {
        id: String(lesson.id),
        title: String(lesson.title || ''),
        subject: String(lesson.subject || ''),
        grade: Number(lesson.grade) || 0,
        body: String(lesson.body || ''),
      },
      quizCount,
      mode,
      position: { index: index + 1, total: lessons.length },
      node: node ? {
        id: String(node.id),
        title: String(node.title || ''),
        orderIndex: Number(node.orderIndex) || 0,
        prerequisiteId: String(node.prerequisiteId || ''),
      } : null,
    });
  } catch (err) {
    console.error('lesson-of-the-day failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'server_error' }, 500);
  }
};
