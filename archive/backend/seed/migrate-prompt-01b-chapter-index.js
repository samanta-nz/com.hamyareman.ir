#!/usr/bin/env node
/**
 * مهاجرت تکمیلی پرامپت ۰۱ (ب): ستون chapterIndex برای پلی‌لیست فصل‌های درس.
 * قرارداد رسانه: C905-L01-V01.mp4 … V03.mp4 / A01.mp3 … A03.mp3 — پلی‌لیست فصل‌ها؛
 * موقعیت پخش = (chapterIndex, lastPositionSec داخل همان فصل).
 * Idempotent: اگر ستون موجود باشد، دست نمی‌زند.
 *
 * اجرا:  node migrate-prompt-01b-chapter-index.js [--dry-run]
 * متغیرها: APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY / APPWRITE_DATABASE_ID
 */
const sdk = require('node-appwrite');

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client()
    .setEndpoint(process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1')
    .setProject(process.env.APPWRITE_PROJECT_ID || '')
    .setKey(process.env.APPWRITE_API_KEY || '');
const tables = new sdk.TablesDB(client);
const DB = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';

async function tryCreateColumn(tableId, col) {
    try {
        if (dryRun) { console.log(`[dry] create column ${tableId}.${col.key}`); return; }
        let res;
        if (col.type === 'integer') {
            res = await tables.createIntegerColumn({
                databaseId: DB, tableId, key: col.key, size: undefined,
                required: col.required ?? false, default: col.default ?? 0,
            });
        } else {
            res = await tables.createStringColumn({
                databaseId: DB, tableId, key: col.key, size: col.size,
                required: col.required ?? false, default: col.default ?? '',
            });
        }
        console.log(`✅ ${tableId}.${col.key} ساخته شد`);
    } catch (e) {
        if (String(e.message || e).includes('already exists')) {
            console.log(`· ${tableId}.${col.key} از قبل هست`);
        } else {
            console.log(`⚠️ ${tableId}.${col.key}: ${e.message || e}`);
        }
    }
}

(async () => {
    console.log('مهاجرت ۰۱ب: chapterIndex (پلی‌لیست فصل‌ها)');
    await tryCreateColumn('lesson_media_progress', { key: 'chapterIndex', type: 'integer', default: 0 });
    console.log('تمام شد.');
})();
