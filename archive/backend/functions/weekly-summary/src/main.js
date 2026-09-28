/**
 * weekly-summary — ساخت خلاصه‌ی هفتگی برای پدر، فقط اگر زهرا opt-in داده باشد.
 *
 * اجرا: cron جمعه‌ها ساعت ۲۰ (در appwrite.json) یا دستی از اپ.
 * ورودی اختیاری: { "userId": "..." } — اگر نبود، برای همه‌ی کاربرانی که
 *   weeklyOptIn=true و پیوند فعال دارند خلاصه می‌سازد.
 *
 * چه چیزی به اشتراک می‌رود؟ فقط جدول‌های PrivacyPolicy.weeklyShareableTables:
 *   routine_blocks, water_logs, exercise_logs, badges
 * چه چیزی هرگز نمی‌رود؟ cycle/mood/journal/screen_time/chat_history — اصلاً در سرور نیستند.
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
    list: service.listRows
      ? (db, table, queries) => service.listRows(db, table, queries)
      : (db, table, queries) => service.listDocuments(db, table, queries),
  };
}

function rowsOf(result) {
  const items = (result && (result.rows || result.documents)) || [];
  return items.map((row) => row.data || row);
}

function parseBody(req) {
  if (!req.body) return {};
  if (typeof req.body === 'string') {
    try { return JSON.parse(req.body); } catch (e) { return {}; }
  }
  return req.body;
}

function weekStartIso(d = new Date()) {
  const date = new Date(d);
  const day = (date.getDay() + 6) % 7; // دوشنبه = 0
  date.setDate(date.getDate() - day);
  date.setHours(0, 0, 0, 0);
  return date.toISOString().slice(0, 10);
}

module.exports = async function (req, res) {
  const client = adminClient();
  const tables = tablesService(client);
  const { Permission, Role, ID, Query } = sdk;
  const body = parseBody(req);
  const weekStart = weekStartIso();
  const since = new Date(weekStart).getTime();

  try {
    const settingsResult = await tables.list(DATABASE_ID, SETTINGS, [Query.limit(100)]);
    const settings = rowsOf(settingsResult).filter((row) => row.weeklyOptIn === true);
    const targets = body.userId ? settings.filter((s) => s.userId === body.userId) : settings;

    const made = [];
    for (const setting of targets) {
      const userId = setting.userId;
      if (!userId) continue;

      const links = rowsOf(await tables.list(DATABASE_ID, LINKS, [
        Query.equal('zahraId', userId),
        Query.equal('status', 'active'),
        Query.limit(1),
      ]));
      const fatherId = links.length ? links[0].fatherId : '';
      if (!fatherId) continue; // بدون پیوند فعال، چیزی برای پدر ساخته نمی‌شود

      const ownerQuery = [Query.equal('ownerId', userId), Query.limit(200)];
      const water = rowsOf(await tables.list(DATABASE_ID, WATER, ownerQuery));
      const routine = rowsOf(await tables.list(DATABASE_ID, ROUTINE, ownerQuery));
      const exercise = rowsOf(await tables.list(DATABASE_ID, EXERCISE, ownerQuery));
      const badges = rowsOf(await tables.list(DATABASE_ID, BADGES, ownerQuery));

      const waterDays = new Set(water.filter((w) => Number(w.glasses) > 0).map((w) => w.dayIso)).size;
      const routineDays = new Set(
        routine.filter((r) => r.done === true).map((r) => r.dayIso),
      ).size;
      const exerciseMinutes = exercise.reduce((sum, e) => sum + Number(e.minutes || 0), 0);
      const badgeCount = badges.filter((b) => Number(b.earnedAtMs || 0) >= since).length;

      const note = `هفته‌ی ${weekStart}: ${waterDays} روز آب، ${routineDays} روز روتین، ${exerciseMinutes} دقیقه ورزش، ${badgeCount} نشان.`;

      await tables.create(
        DATABASE_ID,
        SUMMARIES,
        ID.unique(),
        {
          weekStartIso: weekStart,
          userId,
          fatherId,
          waterDays,
          routineDays,
          exerciseMinutes,
          badges: badgeCount,
          note,
          createdAtMs: Date.now(),
        },
        [
          Permission.read(Role.user(userId)),
          Permission.read(Role.user(fatherId)),
          Permission.update(Role.user(userId)),
        ],
      );
      made.push({ userId, fatherId });
    }

    return res.json({ ok: true, weekStart, count: made.length, summaries: made });
  } catch (err) {
    console.error('weekly-summary failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'server_error' }, 500);
  }
};
