/**
 * notify-guardian — «کمک می‌خوام» با اختیار سرور.
 *
 * چرا تابع سرور و نه کلاینت؟
 *   ۱) متن هشدار را **سرور** می‌نویسد، پس یک اپ دستکاری‌شده نمی‌تواند هشدار را
 *      بی‌متن/بی‌مقصد بفرستد یا مقصدش را عوض کند.
 *   ۲) وجود پیوند فعال با پدر سمت سرور بررسی می‌شود (نه اعتماد به کش کلاینت).
 *   ۳) محدودیت نرخ (Rate limit) سمت سرور است تا یک بحران واقعی زیر پیام‌های
 *      تکراری گم نشود و پدر هم اسپم نگیرد.
 *   ۴) حتی وقتی پیوندی وجود ندارد، شماره‌های اضطراری از سرور برمی‌گردد.
 *
 * ورودی:  { "kind": "crisis"|"help"|"checkin", "note": "اختیاری، تا ۲۰۰ حرف" }
 * خروجی موفق: { ok:true, alertId, fatherId, createdAtMs, helplines:[...] }
 * بدون پیوند: { ok:false, error:"no_link", helplines:[...] }   (کد ۲۰۰ — کلاینت
 *             باید بتواند همین شماره‌ها را نشان بدهد، نه اینکه خطا بگیرد)
 */
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const LINKS = 'father_links';
const MESSAGES = 'father_messages';
const RATE_LIMIT_MS = Number(process.env.GUARDIAN_RATE_LIMIT_MS || 5 * 60 * 1000);
const MAX_NOTE = 200;

const HELPLINES = [
  { name: 'صدای مشاور (بهزیستی)', number: '1480' },
  { name: 'اورژانس اجتماعی', number: '123' },
  { name: 'مشاوره‌ی نوجوان', number: '1570' },
  { name: 'اورژانس', number: '115' },
];

/** متن‌ها را سرور می‌سازد؛ کلاینت فقط «نوع» را انتخاب می‌کند. */
const KINDS = {
  crisis: '🚨 هشدار: زهرا کمک خواست. لطفاً همین حالا زنگ بزن.',
  help: 'زهرا درخواست کمک کرد. لطفاً در اولین فرصت با او تماس بگیر.',
  checkin: 'زهرا خواست امروز حالش را بپرسی — یک پیام کوتاه بده.',
};

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

module.exports = async function (req, res) {
  const { Permission, Role, ID, Query } = sdk;
  const client = adminClient();
  const tables = tablesService(client);
  const body = parseBody(req);
  const userId = userIdOf(req);
  const kind = KINDS[body.kind] ? body.kind : 'help';
  const note = String(body.note || '').slice(0, MAX_NOTE).trim();

  if (!userId) return res.json({ ok: false, error: 'no_user', helplines: HELPLINES }, 401);

  try {
    const links = rowsOf(await tables.list(DATABASE_ID, LINKS, [
      Query.equal('zahraId', userId),
      Query.equal('status', 'active'),
      Query.limit(1),
    ]));
    const fatherId = links.length ? String(links[0].fatherId || '') : '';
    if (!fatherId) {
      // بدون پدرِ پیوندشده هم کاربر تنها نمی‌ماند: شماره‌های اضطراری برگردانده می‌شود.
      return res.json({ ok: false, error: 'no_link', helplines: HELPLINES });
    }

    const now = Date.now();
    const recent = rowsOf(await tables.list(DATABASE_ID, MESSAGES, [
      Query.equal('toUserId', fatherId),
      Query.greaterThanEqual('createdAtMs', now - RATE_LIMIT_MS),
      Query.limit(50),
    ]));
    const duplicate = recent.find((row) => String(row.messageId || '').startsWith('alert-'));
    if (duplicate) {
      return res.json({
        ok: true,
        deduped: true,
        alertId: duplicate.messageId,
        fatherId,
        createdAtMs: Number(duplicate.createdAtMs || now),
        helplines: HELPLINES,
      });
    }

    const alertId = `alert-${now}-${ID.unique()}`;
    const text = note ? `${KINDS[kind]}\nیادداشت زهرا: ${note}` : KINDS[kind];

    await tables.create(
      DATABASE_ID,
      MESSAGES,
      alertId,
      {
        messageId: alertId,
        type: 'TEXT',
        direction: 'TO_FATHER',
        text,
        mediaRef: '',
        durationMs: 0,
        createdAtMs: now,
        toUserId: fatherId,
      },
      [
        Permission.read(Role.user(userId)),
        Permission.read(Role.user(fatherId)),
        Permission.update(Role.user(userId)),
        Permission.delete(Role.user(userId)),
      ],
    );

    return res.json({ ok: true, alertId, fatherId, createdAtMs: now, helplines: HELPLINES });
  } catch (err) {
    console.error('notify-guardian failed', err && err.message ? err.message : err);
    // حتی در خطای سرور، شماره‌های اضطراری باید به کاربر برسد.
    return res.json({ ok: false, error: 'server_error', helplines: HELPLINES }, 500);
  }
};
