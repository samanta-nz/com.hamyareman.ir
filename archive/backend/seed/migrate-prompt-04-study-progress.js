#!/usr/bin/env node
/**
 * مهاجرت پرامپت ۰۴: جدول study_progress — بک‌آپ ابریِ پیشرفت مطالعه.
 *
 * هر کاربر یک سطر برای هر پک مطالعه:
 *   id = "sp-<userId>-<packId>"  (اپ خودش این id قطعی می‌سازد → upsert امن)
 *   userId      — صاحب سطر
 *   packId      — مثل C905_E01-L01
 *   srsState    — JSON وضعیت SM-2 همه‌ی کارت‌ها (کش گوشی = منبع حقیقت روزمره)
 *   attempts    — JSON تاریخچه‌ی آزمون‌ها (نمره، اشتباه‌ها، موضوع‌های ضعیف)
 *   updatedAtIso — آخرین به‌روزرسانی
 *
 * Idempotent: جدول/ستون موجود را دست نمی‌زند.
 * اجرا:  node backend/seed/migrate-prompt-04-study-progress.js [--dry-run]
 * متغیرها: APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY / APPWRITE_DATABASE_ID
 * (در CI: secrets.APPWRITE_API_KEY — الگوی deploy-prompt-02.yml)
 */
const sdk = require('node-appwrite');

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client()
    .setEndpoint(process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1')
    .setProject(process.env.APPWRITE_PROJECT_ID || '')
    .setKey(process.env.APPWRITE_API_KEY || '');
const tables = new sdk.TablesDB(client);
const DB = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const TABLE = 'study_progress';

async function ensureTable() {
    try {
        if (dryRun) { console.log(`[dry] create table ${TABLE}`); return; }
        await tables.createTable({
            databaseId: DB,
            tableId: TABLE,
            name: 'پیشرفت مطالعه (SRS + آزمون‌ها)',
            permissions: ['create("users")'],
            rowSecurity: true,
        });
        console.log(`✅ جدول ${TABLE} ساخته شد`);
    } catch (e) {
        if (String(e.message || e).includes('already exists')) {
            console.log(`· جدول ${TABLE} از قبل هست`);
        } else {
            throw e;
        }
    }
}

async function tryCreateStringColumn(key, size) {
    try {
        if (dryRun) { console.log(`[dry] create column ${TABLE}.${key} (string ${size})`); return; }
        await tables.createStringColumn({
            databaseId: DB, tableId: TABLE, key, size,
            required: false, default: '',
        });
        console.log(`✅ ${TABLE}.${key} ساخته شد`);
    } catch (e) {
        if (String(e.message || e).includes('already exists')) {
            console.log(`· ${TABLE}.${key} از قبل هست`);
        } else {
            console.log(`⚠️ ${TABLE}.${key}: ${e.message || e}`);
        }
    }
}

(async () => {
    console.log('مهاجرت پرامپت ۰۴: جدول study_progress');
    try {
        await ensureTable();
        await tryCreateStringColumn('userId', 64);
        await tryCreateStringColumn('packId', 128);
        await tryCreateStringColumn('srsState', 100000);   // JSON وضعیت SM-2 همه‌ی کارت‌ها
        await tryCreateStringColumn('attempts', 200000);   // JSON تاریخچه‌ی آزمون‌ها (تا ۵۰ آزمون)
        await tryCreateStringColumn('updatedAtIso', 32);
    } catch (e) {
        const msg = String(e && e.message || e);
        if (String(e && e.code) === '401' || msg.includes('unauthorized') || msg.includes('scope')) {
            console.error('');
            console.error('❌ کلید API معتبر نیست یا اسکوپ tables.write ندارد.');
            console.error('   گیت‌هاب › Settings › Secrets › APPWRITE_API_KEY را با کلیدی که');
            console.error('   tables.write + documents.write + functions.write دارد جایگزین کن،');
            console.error('   بعد ورک‌فلو را دوباره اجرا کن.');
        }
        throw e;
    }
    console.log('تمام شد.');
})();
