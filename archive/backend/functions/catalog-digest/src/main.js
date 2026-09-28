/**
 * catalog-digest — اثر انگشت کاتالوگ محتوا.
 *
 * مشکل: اپ برای آفلاین‌بودن، کل کاتالوگ (درس/آزمون/غذا/ورزش/گره‌ها/ایده‌ها) را
 * از سرور می‌خواند. این کار هر بار چند صد سطر است، درحالی‌که محتوا ماه‌ها عوض نمی‌شود.
 *
 * راه‌حل: قبل از دانلود کامل، این تابع صدا زده می‌شود و یک `digest` برمی‌گرداند.
 * اگر digest با مقدار کش‌شده یکی بود، دانلود کامل **اصلاً انجام نمی‌شود**.
 *
 * ورودی: {} (اختیاری: { "tables": ["lessons"] })
 * خروجی: { ok, digest, generatedAtMs, tables: { lessons:{count,ids}, ... } }
 */
const crypto = require('node:crypto');
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';
const CATALOG_TABLES = ['lessons', 'quizzes', 'recipes', 'exercises', 'learning_nodes', 'art_prompts'];

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

function rowIds(result) {
  const items = (result && (result.rows || result.documents)) || [];
  return items.map((row) => String(row.$id)).sort();
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
  const wanted = Array.isArray(body.tables) && body.tables.length
    ? body.tables.filter((t) => CATALOG_TABLES.includes(String(t)))
    : CATALOG_TABLES;

  try {
    const summary = {};
    for (const table of wanted) {
      try {
        const ids = rowIds(await tables.list(DATABASE_ID, table, [Query.limit(500)]));
        summary[table] = { count: ids.length, ids };
      } catch (tableErr) {
        // یک جدول ساخته‌نشده نباید کل digest را خراب کند؛ با count=-1 علامت می‌خورد
        // تا کلاینت بداند آن جدول هنوز آماده نیست و دانلودش را امتحان نکند.
        console.warn(`catalog-digest: table ${table} unavailable`, tableErr && tableErr.message);
        summary[table] = { count: -1, ids: [] };
      }
    }

    const digest = crypto.createHash('sha256')
      .update(JSON.stringify(summary))
      .digest('hex')
      .slice(0, 16);

    return res.json({ ok: true, digest, generatedAtMs: Date.now(), tables: summary });
  } catch (err) {
    console.error('catalog-digest failed', err && err.message ? err.message : err);
    return res.json({ ok: false, error: 'server_error' }, 500);
  }
};
