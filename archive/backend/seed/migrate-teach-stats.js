/**
 * Migration «آمار تدریس» (v1.6) — جدول teach_stats در ZahraDB.
 *
 * هدف: «پیشرفت سلامتی» و آمار تدریس/مرور/آزمون به‌صورت خودکار روی سرور هم
 * ذخیره شود (سینک خودکار، غیرقابل ویرایش از سمت کاربر — فقط اپ می‌نویسد).
 *
 * اجرا: APPWRITE_DATABASE_ID=ZahraDB node migrate-teach-stats.js
 * الگو: migrate-prompt-01.js (idempotent — اجرای مجدد بی‌ضرر است).
 */
const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';

const tables = new sdk.TablesDB(
    new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY),
);

const DRY = String(process.env.DRY_RUN || '') === '1';

async function tryCreateColumn(tableId, col) {
    const base = { databaseId: DATABASE_ID, tableId, key: col.key, required: !!col.required };
    try {
        if (col.type === 'string') {
            await tables.createStringColumn({ ...base, size: col.size, default: col.default });
        } else if (col.type === 'integer') {
            await tables.createIntegerColumn({ ...base, default: col.default });
        } else if (col.type === 'boolean') {
            await tables.createBooleanColumn({ ...base, default: col.default });
        }
        console.log(`  ✅ ${tableId}.${col.key}`);
    } catch (e) {
        const msg = String(e.message || e);
        if (msg.includes('already exists') || msg.includes('already created')) {
            console.log(`  · ${tableId}.${col.key} موجود است`);
        } else {
            console.log(`  ⚠️ ${tableId}.${col.key}: ${msg}`);
        }
    }
}

async function ensureTable() {
    if (DRY) {
        console.log('[dry] ensure teach_stats table');
        return;
    }
    try {
        await tables.createTable({
            databaseId: DATABASE_ID,
            tableId: 'teach_stats',
            name: 'آمار تدریس',
            permissions: ['create("users")'],
            rowSecurity: true,
        });
        console.log('✅ teach_stats table created');
    } catch (e) {
        if (String(e.message || e).includes('already exists')) {
            console.log('· teach_stats already exists');
        } else {
            throw e;
        }
    }
}

async function migrate() {
    console.log(`v1.6 migration: teach_stats در ${DATABASE_ID}`);
    await ensureTable();
    const cols = [
        { key: 'userId', type: 'string', size: 64, required: true },
        { key: 'packId', type: 'string', size: 64, required: true },
        { key: 'stats', type: 'string', size: 16384, required: false, default: '{}' },
        { key: 'updatedAtIso', type: 'string', size: 40, required: false, default: '' },
        { key: 'lastSessionIso', type: 'string', size: 40, required: false, default: '' },
    ];
    for (const col of cols) {
        await tryCreateColumn('teach_stats', col);
    }
    console.log('✅ migration تمام شد.');
}

migrate().catch((e) => {
    console.error('❌ migration failed:', e.message || e);
    process.exit(1);
});
