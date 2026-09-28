/**
 * daily-checkin — خلاصه‌ی روزانه‌ی opt-in برای پدر.
 *
 * تفاوتش با weekly-summary: همان داده، اما هر روز. پدر به‌جای اینکه یک هفته
 * صبر کند، شب همان روز می‌بیند زهرا آب خورد، روتینش را انجام داد و ورزش کرد.
 *
 * حریم خصوصی (همان قوانین weekly-summary، بدون استثنا):
 *   - فقط اگر `user_settings.weeklyOptIn === true` باشد چیزی ساخته می‌شود.
 *     (عمداً از همان پرچم استفاده می‌کنیم تا ستون/جدول جدید لازم نشود و کاربر
 *      یک کلید را دو بار نبیند؛ opt-in یعنی «پدر وضعیت کلی من را ببیند».)
 *   - فقط جدول‌های قابل اشتراک خوانده می‌شود: water_logs, routine_blocks,
 *     exercise_logs, badges. چرخه/خلق‌وخو/ژورنال/زمان صفحه در سرور وجود ندارند.
 *   - شناسه‌ی سطر deterministic است (`checkin-<userId>-<dayIso>`)، پس اجرای چندباره
 *     در یک روز سطر تکراری نمی‌سازد — فقط به‌روزرسانی می‌شود.
 *
 * ورودی: {} یا { "userId": "..." } (فقط برای اجرای زمان‌بندی‌شده/ادمین)
 * خروجی: { ok, dayIso, fatherId, summary:{...} } | { ok:false, error:"opt_out"|"no_link" }
 */
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const SETTINGS = 'user_settings';
const LINKS = 'father_links';
const WATER = 'water_logs';
const ROUTINE = 'routine_blocks';
const EXERCISE = 'exercise_logs';
const BADGES = 'badges';
const SUMMARIES = 'weekly_summaries';

function adminClient() {
  return new sdk.Client()
    .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
    .setKey(process.env.APPWRITE_FUNCTION_API_KEY);
}

function tablesService(client) {
  const service = sdk.TablesDB ? new sdk.TablesDB(client) : new sdk.Databases(client);
  return {
    create: service.createRow
      ? (db, table, id, data, perms) => service.createRow(db, table, id, data, perms)
      : (db, table, id, data, perms) => service.createDocument(db, table, id, data, perms),
    update: service.updateRow
      ? (db, table, id, data) => service.updateRow(db, table, id, data)
      : (db, table, id, data) => service.updateDocument(db, table, id, data),
    get: service.getRow
      ? (db, table, id) => service.getRow(db, table, id)
      : (db, table, id) => service.getDocument(db, table, id),
    list: service.listRows
      ? (db, table, queries) => service.listRows(db, table, queries)
      : (db, table, queries) => service.listDocuments(db, table, queries),
  };
}

function rowsOf(result) {
  const items = (result && (result.rows || result.documents)) || [];
  return items.map((row) => row.data || row);
}

function userIdOf(req) {
  return (
    req.userId ||
    (req.headers && (req.headers['x-appwrite-user-id'] || req.headers['X-Appwrite-User-Id'])) ||
    ''
  );
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

module.exports = async function (req, res) {
  const { Permission, Role, Query } = sdk;
  const tables = tablesService(adminClient());
  const body = parseBody(req);
  // در حالت عادی فقط داده‌ی خودِ صدازننده خوانده می‌شود؛ body.userId برای cron است.
  const userId = userIdOf(req) || String(body.userId || '');
  const iso = dayIso();

  if (!userId) return res.json({ ok: false, error: 'no_user' }, 401);

  try {
    const settings = rowsOf(await tables.list(DATABASE_ID, SETTINGS, [
      Query.equal('userId', userId), Query.limit(1),
    ]));
    if (!settings.length || settings[0].weeklyOptIn !== true) {
      return res.json({ ok: false, error: 'opt_out', dayIso: iso });
    }

    const links = rowsOf(await tables.list(DATABASE_ID, LINKS, [
      Query.equal('zahraId', userId), Query.equal('status', 'active'), Query.limit(1),
    ]));
    const fatherId = links.length ? String(links[0].fatherId || '') : '';
    if (!fatherId) return res.json({ ok: false, error: 'no_link', dayIso: iso });

    const dayQuery = [Query.equal('ownerId', userId), Query.equal('dayIso', iso), Query.limit(200)];
    const water = rowsOf(await tables.list(DATABASE_ID, WATER, dayQuery));
    const routine = rowsOf(await tables.list(DATABASE_ID, ROUTINE, dayQuery));
    const exercise = rowsOf(await tables.list(DATABASE_ID, EXERCISE, dayQuery));
    const badges = rowsOf(await tables.list(DATABASE_ID, BADGES, [
      Query.equal('ownerId', userId), Query.limit(200),
    ])).filter((b) => String(b.dayIso || '') === iso);

    const glasses = water.reduce((sum, w) => sum + Number(w.glasses || 0), 0);
    const routineDone = routine.filter((r) => r.done === true).length;
    const exerciseMinutes = exercise.reduce((sum, e) => sum + Number(e.minutes || 0), 0);
    const badgeCount = badges.length;

    const note = `امروز ${iso}: ${glasses} لیوان آب، ${routineDone} بخش روتین، `
      + `${exerciseMinutes} دقیقه ورزش، ${badgeCount} نشان.`;
    const rowId = `checkin-${userId}-${iso}`;
    const data = {
      weekStartIso: iso,
      userId,
      fatherId,
      waterDays: glasses,
      routineDays: routineDone,
      exerciseMinutes,
      badges: badgeCount,
      note,
      createdAtMs: Date.now(),
    };
    const perms = [
      Permission.read(Role.user(userId)),
      Permission.read(Role.user(fatherId)),
      Permission.update(Role.user(userId)),
    ];

    let existed = true;
    try {
      await tables.get(DATABASE_ID, SUMMARIES, rowId);
    } catch (notFound) {
      existed = false;
    }
    if (existed) {
      await tables.update(DATABASE_ID, SUMMARIES, rowId, data);
    } else {
      await tables.create(DATABASE_ID, SUMMARIES, rowId, data, perms);
    }

    return res.json({
      ok: true,
      dayIso: iso,
      fatherId,
      updated: existed,
      summary: { glasses, routineDone, exerciseMinutes, badgeCount, note },
    });
  } catch (err) {
    console.error('daily-checkin failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'server_error' }, 500);
  }
};
