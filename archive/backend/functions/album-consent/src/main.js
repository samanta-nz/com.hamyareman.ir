/**
 * album-consent — تأیید/رد خاطره‌ی پدر در آلبوم مشترک، با اختیار سرور.
 *
 * قانون محصول: آلبوم مال زهراست؛ خاطره‌ای که پدر اضافه می‌کند تا زهرا تأیید نکند
 * در آلبوم او نمی‌نشیند. این قانون تا امروز با Permission سطر در کلاینت اجرا می‌شد
 * که درست است، اما «چه کسی مالک است» را کلاینت اعلام می‌کند. اینجا همان بررسی
 * سمت سرور هم انجام می‌شود تا یک کلاینت دستکاری‌شده نتواند خاطره‌ی تأییدنشده را
 * تأییدشده جا بزند یا خاطره‌ی دیگری را رد کند.
 *
 * ورودی: { "action": "approve"|"reject", "itemId": "..." }
 * خروجی: { ok:true, itemId, approved } | { ok:false, error:"not_owner"|"not_found"|... }
 */
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const ALBUM = 'album_items';

function adminClient() {
  return new sdk.Client()
    .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
    .setKey(process.env.APPWRITE_FUNCTION_API_KEY);
}

function tablesService(client) {
  const service = sdk.TablesDB ? new sdk.TablesDB(client) : new sdk.Databases(client);
  return {
    update: service.updateRow
      ? (db, table, id, data) => service.updateRow(db, table, id, data)
      : (db, table, id, data) => service.updateDocument(db, table, id, data),
    list: service.listRows
      ? (db, table, queries) => service.listRows(db, table, queries)
      : (db, table, queries) => service.listDocuments(db, table, queries),
  };
}

function rawRows(result) {
  return (result && (result.rows || result.documents)) || [];
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
  const { Query } = sdk;
  const tables = tablesService(adminClient());
  const body = parseBody(req);
  const userId = userIdOf(req);
  const action = body.action === 'reject' ? 'reject' : body.action === 'approve' ? 'approve' : '';
  const itemId = String(body.itemId || '').trim();

  if (!userId) return res.json({ ok: false, error: 'no_user' }, 401);
  if (!action) return res.json({ ok: false, error: 'bad_action' }, 400);
  if (!itemId) return res.json({ ok: false, error: 'bad_item' }, 400);

  try {
    const found = rawRows(await tables.list(DATABASE_ID, ALBUM, [
      Query.equal('itemId', itemId), Query.limit(1),
    ]));
    if (!found.length) return res.json({ ok: false, error: 'not_found' }, 404);

    const row = found[0];
    const data = row.data || row;
    if (String(data.ownerId || '') !== userId) {
      // فقط صاحب آلبوم (زهرا) می‌تواند تأیید یا رد کند.
      return res.json({ ok: false, error: 'not_owner' }, 403);
    }
    if (String(data.addedBy || '') !== 'FATHER') {
      return res.json({ ok: false, error: 'not_pending' }, 409);
    }

    const approved = action === 'approve';
    await tables.update(DATABASE_ID, ALBUM, row.$id, { approved });

    return res.json({ ok: true, itemId, approved });
  } catch (err) {
    console.error('album-consent failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'server_error' }, 500);
  }
};
