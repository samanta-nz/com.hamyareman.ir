#!/usr/bin/env node
/**
 * verify.js — مقایسه‌ی پروژه‌ی واقعی Appwrite با آنچه در `backend/appwrite.json` آمده.
 *
 * چرا لازم است؟ محیط توسعه‌ی این ریپو به سرور Appwrite دسترسی شبکه‌ای نداشت، پس
 * «درست ساخته شدن جدول‌ها/توابع» قابل بررسی خودکار نبود. این اسکریپت همان بررسی را
 * روی ماشین خودت انجام می‌دهد و دقیقاً می‌گوید چه چیزی کم است.
 *
 * اجرا:
 *   cd backend/scripts
 *   npm install            # فقط node-appwrite
 *   export APPWRITE_PROJECT_ID="..."
 *   export APPWRITE_API_KEY="..."        # کلیدی با دسترسی tables.read / functions.read
 *   node verify.js                       # گزارش خوانا
 *   node verify.js --json                # خروجی JSON (برای CI)
 *   node verify.js --only=tables         # فقط جدول‌ها (یا buckets / functions)
 *
 * کد خروج: ۰ اگر همه‌چیز سبز بود، ۱ اگر چیزی کم بود (برای CI مناسب است).
 *
 * نکته‌ی امنیتی: کلید را **هرگز** commit نکن؛ فقط از محیط بده.
 */
const fs = require('node:fs');
const path = require('node:path');
// SDK فقط وقتی لازم می‌شود که واقعاً بخواهیم به سرور بزنیم؛ پس بدون نصبِ آن هم
// پیام «کلید لازم است» درست نمایش داده می‌شود (نه خطای module not found).
let sdk = null;
function loadSdk() {
  if (!sdk) sdk = require('node-appwrite');
  return sdk;
}

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID || '';
const API_KEY = process.env.APPWRITE_API_KEY || '';
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';

const args = process.argv.slice(2);
const asJson = args.includes('--json');
const onlyArg = args.find((a) => a.startsWith('--only='));
const only = onlyArg ? onlyArg.split('=')[1].split(',').map((s) => s.trim()) : null;
const want = (name) => !only || only.includes(name);

const SPEC_PATH = path.join(__dirname, '..', 'appwrite.json');

function client() {
  const sdk = loadSdk();
  return new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
}

/** نام متدها بین نسخه‌های SDK (TablesDB قدیم/جدید) فرق می‌کند؛ هر دو پوشش داده شد. */
function tablesApi(c) {
  const sdk = loadSdk();
  const service = sdk.TablesDB ? new sdk.TablesDB(c) : new sdk.Databases(c);
  const listTables = service.listTables
    ? (db) => service.listTables(db, [sdk.Query.limit(500)])
    : (db) => service.listCollections(db, [sdk.Query.limit(500)]);
  const listColumns = service.listColumns
    ? (db, t) => service.listColumns(db, t, [sdk.Query.limit(500)])
    : service.listAttributes
      ? (db, t) => service.listAttributes(db, t, [sdk.Query.limit(500)])
      : null;
  return { listTables, listColumns };
}

function items(result) {
  return (result && (result.tables || result.collections || result.buckets || result.functions
    || result.rows || result.documents || result.columns || result.attributes)) || [];
}

const OK = '✓';
const NO = '✗';
const WARN = '!';

async function main() {
  if (!PROJECT_ID || !API_KEY) {
    console.error('APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است (کلید را commit نکن).');
    process.exit(2);
  }
  const spec = JSON.parse(fs.readFileSync(SPEC_PATH, 'utf8'));
  loadSdk();
  const c = client();
  const report = { endpoint: ENDPOINT, projectId: PROJECT_ID, databaseId: DATABASE_ID, tables: [], buckets: [], functions: [], problems: [] };
  const log = (...a) => { if (!asJson) console.log(...a); };

  // --- جدول‌ها -------------------------------------------------------------
  if (want('tables')) {
    const api = tablesApi(c);
    let liveTables = [];
    try {
      liveTables = items(await api.listTables(DATABASE_ID));
    } catch (err) {
      report.problems.push(`دیتابیس «${DATABASE_ID}» خوانده نشد: ${err.message || err}`);
      log(`${NO} دیتابیس «${DATABASE_ID}» خوانده نشد: ${err.message || err}`);
    }
    const liveIds = new Set(liveTables.map((t) => t.$id));
    log(`\n— جدول‌ها (${spec.tablesDB.length} مورد انتظار، ${liveIds.size} موجود)`);

    for (const table of spec.tablesDB) {
      const entry = { id: table.$id, exists: liveIds.has(table.$id), missingColumns: [], extraColumns: [] };
      if (!entry.exists) {
        log(`${NO} جدول «${table.$id}» ساخته نشده`);
        report.problems.push(`جدول «${table.$id}» ساخته نشده`);
        report.tables.push(entry);
        continue;
      }
      if (api.listColumns) {
        try {
          const cols = items(await api.listColumns(DATABASE_ID, table.$id)).map((x) => x.$id || x.key);
          const expected = table.columns.map((x) => x.$id);
          entry.missingColumns = expected.filter((e) => !cols.includes(e));
          entry.extraColumns = cols.filter((x) => !expected.includes(x));
          if (entry.missingColumns.length) {
            log(`${NO} «${table.$id}» ستون‌های کم دارد: ${entry.missingColumns.join('، ')}`);
            report.problems.push(`«${table.$id}» ستون کم دارد: ${entry.missingColumns.join(', ')}`);
          } else if (entry.extraColumns.length) {
            log(`${WARN} «${table.$id}» ستون اضافه دارد (مشکلی نیست): ${entry.extraColumns.join('، ')}`);
          } else {
            log(`${OK} «${table.$id}» — ${expected.length} ستون`);
          }
        } catch (err) {
          log(`${WARN} «${table.$id}» هست، ولی ستون‌هایش خوانده نشد: ${err.message || err}`);
        }
      } else {
        log(`${OK} «${table.$id}» (ستون‌ها با این نسخه‌ی SDK قابل بررسی نیست)`);
      }
      report.tables.push(entry);
    }

    // جدول‌هایی که عمداً نباید باشند — اگر باشند یعنی داده‌ی خصوصی به سرور رفته.
    const NEVER = ['cycle_entries', 'mood_entries', 'journal_entries', 'screen_time_logs', 'chat_history'];
    const leaked = NEVER.filter((t) => liveIds.has(t));
    if (leaked.length) {
      log(`${NO} جدول خصوصی ساخته شده (نباید!): ${leaked.join('، ')}`);
      report.problems.push(`جدول خصوصی ساخته شده: ${leaked.join(', ')}`);
    } else {
      log(`${OK} هیچ‌کدام از ۵ جدول خصوصی ساخته نشده (درست)`);
    }
    report.neverTablesPresent = leaked;

    const unexpected = [...liveIds].filter((id) => !spec.tablesDB.some((t) => t.$id === id) && !NEVER.includes(id));
    if (unexpected.length) log(`${WARN} جدول‌های اضافه در پروژه: ${unexpected.join('، ')}`);
    report.extraTables = unexpected;
  }

  // --- باکت‌ها -------------------------------------------------------------
  if (want('buckets')) {
    log('\n— باکت‌ها');
    let live = [];
    try {
      live = items(await new (loadSdk().Storage)(c).listBuckets());
    } catch (err) {
      report.problems.push(`باکت‌ها خوانده نشد: ${err.message || err}`);
      log(`${NO} باکت‌ها خوانده نشد: ${err.message || err}`);
    }
    const liveIds = new Set(live.map((b) => b.$id));
    for (const bucket of spec.buckets) {
      const found = liveIds.has(bucket.$id);
      report.buckets.push({ id: bucket.$id, exists: found });
      if (!found) {
        log(`${NO} باکت «${bucket.$id}» ساخته نشده`);
        report.problems.push(`باکت «${bucket.$id}» ساخته نشده`);
        continue;
      }
      const liveBucket = live.find((b) => b.$id === bucket.$id) || {};
      if (bucket.maximumFileSize && liveBucket.maximumFileSize && liveBucket.maximumFileSize !== bucket.maximumFileSize) {
        log(`${WARN} «${bucket.$id}» سقف حجمش ${liveBucket.maximumFileSize} است، انتظار ${bucket.maximumFileSize}`);
      }
      log(`${OK} باکت «${bucket.$id}»`);
    }
  }

  // --- توابع --------------------------------------------------------------
  if (want('functions')) {
    log('\n— توابع سرور');
    let live = [];
    try {
      live = items(await new (loadSdk().Functions)(c).list());
    } catch (err) {
      report.problems.push(`توابع خوانده نشد: ${err.message || err}`);
      log(`${NO} توابع خوانده نشد: ${err.message || err}`);
    }
    const liveIds = new Set(live.map((f) => f.$id));
    for (const fn of spec.functions) {
      const found = liveIds.has(fn.$id);
      const liveFn = live.find((f) => f.$id === fn.$id) || null;
      const entry = {
        id: fn.$id,
        exists: found,
        enabled: liveFn ? liveFn.enabled !== false : null,
        schedule: liveFn ? (liveFn.schedule || '') : null,
      };
      report.functions.push(entry);
      if (!found) {
        log(`${NO} تابع «${fn.$id}» deploy نشده`);
        report.problems.push(`تابع «${fn.$id}» deploy نشده`);
        continue;
      }
      if (entry.enabled === false) {
        log(`${WARN} تابع «${fn.$id}» غیرفعال است`);
        report.problems.push(`تابع «${fn.$id}» غیرفعال است`);
      }
      if (fn.schedule && !entry.schedule) {
        log(`${WARN} تابع «${fn.$id}» cron ندارد (انتظار: ${fn.schedule})`);
      }
      log(`${OK} تابع «${fn.$id}»${entry.schedule ? ` — cron: ${entry.schedule}` : ''}`);
    }
  }

  log(`\n${report.problems.length ? `${NO} ${report.problems.length} مشکل` : `${OK} همه‌چیز مطابق backend/appwrite.json است`}`);
  if (asJson) console.log(JSON.stringify(report, null, 2));
  process.exit(report.problems.length ? 1 : 0);
}

main().catch((err) => {
  console.error('verify.js failed:', err && err.message ? err.message : err);
  process.exit(2);
});
