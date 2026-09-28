/**
 * pairing — ساخت/ابطال/مصرف کد پیوند زهرا ↔ پدر.
 *
 * قرارداد (JSON):
 *   { "action": "create" }                      → { "code": "123456", "expiresAtMs": ... }   (زهرا)
 *   { "action": "revoke" }                      → { "ok": true }                             (زهرا)
 *   { "action": "redeem", "code": "123456" }    → { "partnerId", "partnerName", "linkedAt" }  (پدر)
 *   { "action": "unlink" }                      → { "ok": true }                             (هر دو)
 *
 * چرا اینجا و نه کلاینت؟
 *   - کد یک‌بارمصرف و کوتاه‌عمر است (۱۰ دقیقه).
 *   - ارتقای Label پدر به `father` یک عملیات Admin است.
 *   - سطر `father_links` با دسترسی‌های دقیق ساخته می‌شود.
 */
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const CODES = 'pairing_codes';
const LINKS = 'father_links';
const PROFILES = 'profiles';
const TTL_MS = 10 * 60 * 1000;

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
    list: service.listRows
      ? (db, table, queries) => service.listRows(db, table, queries)
      : (db, table, queries) => service.listDocuments(db, table, queries),
  };
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

function sixDigits() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

async function profileName(tables, userId, fallback) {
  try {
    const rows = await tables.list(DATABASE_ID, PROFILES, [sdk.Query.equal('userId', userId)]);
    const items = rows.rows || rows.documents || [];
    if (items.length && items[0].displayName) return items[0].displayName;
    const data = items.length ? (items[0].data || items[0]) : null;
    return (data && data.displayName) || fallback;
  } catch (e) {
    return fallback;
  }
}

module.exports = async function (req, res) {
  const userId = userIdOf(req);
  if (!userId) return res.json({ ok: false, error: 'unauthenticated' }, 401);

  const body = parseBody(req);
  const action = String(body.action || '').toLowerCase();
  const client = adminClient();
  const tables = tablesService(client);
  const users = new sdk.Users(client);
  const { Permission, Role, ID, Query } = sdk;

  try {
    if (action === 'create') {
      // کدهای قبلی این کاربر باطل می‌شوند تا فقط یک کد فعال بماند.
      const mine = await tables.list(DATABASE_ID, CODES, [
        Query.equal('createdBy', userId),
        Query.equal('status', 'active'),
      ]);
      const mineItems = mine.rows || mine.documents || [];
      for (const row of mineItems) {
        await tables.update(DATABASE_ID, CODES, row.$id, { status: 'revoked' }).catch(() => null);
      }
      const code = sixDigits();
      await tables.create(
        DATABASE_ID,
        CODES,
        ID.unique(),
        {
          code,
          createdBy: userId,
          usedBy: '',
          status: 'active',
          expiresAtMs: Date.now() + TTL_MS,
          createdAtMs: Date.now(),
        },
        [Permission.read(Role.user(userId)), Permission.update(Role.user(userId))],
      );
      return res.json({ ok: true, code, expiresAtMs: Date.now() + TTL_MS });
    }

    if (action === 'revoke') {
      const mine = await tables.list(DATABASE_ID, CODES, [
        Query.equal('createdBy', userId),
        Query.equal('status', 'active'),
      ]);
      const items = mine.rows || mine.documents || [];
      for (const row of items) {
        await tables.update(DATABASE_ID, CODES, row.$id, { status: 'revoked' });
      }
      return res.json({ ok: true });
    }

    if (action === 'redeem') {
      const code = String(body.code || '').replace(/\D/g, '');
      if (code.length !== 6) return res.json({ ok: false, error: 'invalid_code' }, 400);

      const found = await tables.list(DATABASE_ID, CODES, [
        Query.equal('code', code),
        Query.equal('status', 'active'),
        Query.limit(1),
      ]);
      const items = found.rows || found.documents || [];
      if (!items.length) return res.json({ ok: false, error: 'code_not_found' }, 404);

      const row = items[0];
      const data = row.data || row;
      if (!data.expiresAtMs || Number(data.expiresAtMs) < Date.now()) {
        await tables.update(DATABASE_ID, CODES, row.$id, { status: 'expired' }).catch(() => null);
        return res.json({ ok: false, error: 'code_expired' }, 410);
      }
      if (data.createdBy === userId) {
        return res.json({ ok: false, error: 'cannot_pair_self' }, 400);
      }

      const zahraId = data.createdBy;
      const fatherId = userId;
      const linkedAt = Date.now();
      const partnerName = await profileName(tables, zahraId, 'زهرا');

      await tables.create(
        DATABASE_ID,
        LINKS,
        ID.unique(),
        { zahraId, fatherId, partnerName, status: 'active', linkedAt },
        [
          Permission.read(Role.user(zahraId)),
          Permission.update(Role.user(zahraId)),
          Permission.read(Role.user(fatherId)),
        ],
      );
      await tables.update(DATABASE_ID, CODES, row.$id, { status: 'used', usedBy: fatherId });

      // ارتقای نقش پدر — فقط سمت سرور ممکن است.
      await users.updateLabels(fatherId, ['father']);

      return res.json({
        ok: true,
        partnerId: zahraId,
        partnerName,
        linkedAt,
        status: 'active',
      });
    }

    if (action === 'unlink') {
      const asZahra = await tables.list(DATABASE_ID, LINKS, [
        Query.equal('zahraId', userId),
        Query.equal('status', 'active'),
      ]);
      const asFather = await tables.list(DATABASE_ID, LINKS, [
        Query.equal('fatherId', userId),
        Query.equal('status', 'active'),
      ]);
      const items = [...(asZahra.rows || asZahra.documents || []), ...(asFather.rows || asFather.documents || [])];
      for (const row of items) {
        await tables.update(DATABASE_ID, LINKS, row.$id, { status: 'revoked' });
      }
      await users.updateLabels(userId, ['guest']);
      return res.json({ ok: true });
    }

    return res.json({ ok: false, error: 'unknown_action' }, 400);
  } catch (err) {
    console.error('pairing failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'server_error' }, 500);
  }
};
