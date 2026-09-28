/**
 * مهاجرت پرامپت ۰۳: ساخت جداول guided_sessions / guided_progress
 *                    (کاتالوگ جلسات هدایت‌شده ۴۵تایی + پیشرفت کاربر).
 *
 * الگو: migrate-prompt-02.js (همان قراردادها، همان envها، همان باکت wellness-media)
 * ایمن: اگر ستون/جدول از قبل باشد، skip می‌شود (best-effort).
 * اجرا:   node backend/seed/migrate-prompt-03.js
 * خشک:   node backend/seed/migrate-prompt-03.js --dry-run
 */
const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const BUCKET_ID = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b'; // همان باکت wellness-media

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const tablesDb = new sdk.TablesDB(client);
const databases = new sdk.Databases(client); // برای ایجاد database اگر نباشد

// نگاشت type فیلد به متد مناسب در TablesDB v18+ (الگوی prompt-02)
function createColumnFn(type) {
    const map = {
        'string': 'createStringColumn',
        'integer': 'createIntegerColumn',
        'boolean': 'createBooleanColumn',
        'float': 'createFloatColumn',
        'datetime': 'createDatetimeColumn',
        'enum': 'createEnumColumn',
    };
    return map[type] || null;
}

async function tryCreateTable(tableId, name, perms, rowSecurity) {
    try {
        if (dryRun) return console.log(`  [dry] createTable ${tableId}`);
        await tablesDb.createTable({
            databaseId: DATABASE_ID,
            tableId,
            name,
            permissions: perms,
            rowSecurity: !!rowSecurity,
        });
        console.log(`  ✅ table ${tableId}`);
    } catch (e) {
        const msg = String(e.message || e);
        if (msg.includes('already exists') || msg.includes('duplicate') || msg.includes('409')) {
            console.log(`  · table ${tableId} (exists)`);
        } else {
            console.log(`  ⚠️ table ${tableId}: ${msg}`);
        }
    }
}

async function tryCreateColumn(tableId, col) {
    const method = createColumnFn(col.type);
    if (!method) {
        console.log(`  ⚠️ نوع نامعتبر ${col.type} برای ${tableId}.${col.key}`);
        return;
    }
    const args = {
        databaseId: DATABASE_ID,
        tableId,
        key: col.key,
        size: col.size,
        required: !!col.required,
        xdefault: col.default, // v18: مقدار پیش‌فرض
        elements: col.elements, // فقط enum
    };
    if (col.default !== undefined && col.default !== null) {
        args.default = col.default;
    }
    try {
        if (dryRun) return console.log(`  [dry] ${method} ${tableId}.${col.key}`);
        await tablesDb[method](args);
        console.log(`  ✅ ${tableId}.${col.key}`);
    } catch (e) {
        const msg = String(e.message || e);
        if (msg.includes('already exists') || msg.includes('duplicate') || msg.includes('409')) {
            console.log(`  · ${tableId}.${col.key} (exists)`);
        } else {
            console.log(`  ⚠️ ${tableId}.${col.key}: ${msg}`);
        }
    }
}

async function tryCreateIndex(tableId, idx) {
    try {
        if (dryRun) return console.log(`  [dry] createIndex ${tableId}.${idx.key}`);
        // Appwrite v18+: نام پارامتر از attributes به columns تغییر کرده (الگوی prompt-02)
        await tablesDb.createIndex({
            databaseId: DATABASE_ID,
            tableId,
            key: idx.key,
            type: idx.type,
            columns: idx.attributes,
        });
        console.log(`  ✅ index ${tableId}.${idx.key}`);
    } catch (e) {
        const msg = String(e.message || e);
        if (msg.includes('already exists') || msg.includes('duplicate') || msg.includes('409')) {
            console.log(`  · index ${tableId}.${idx.key} (exists)`);
        } else {
            console.log(`  ⚠️ index ${tableId}.${idx.key}: ${msg}`);
        }
    }
}

async function tryCreateDatabase() {
    try {
        await databases.get({ databaseId: DATABASE_ID });
        console.log(`  · database ${DATABASE_ID} (exists)`);
    } catch (e) {
        if (e.code === 404) {
            try {
                if (dryRun) return console.log(`  [dry] createDatabase ${DATABASE_ID}`);
                await databases.create({ databaseId: DATABASE_ID, name: 'پایگاه‌داده‌ی زهرا' });
                console.log(`  ✅ database ${DATABASE_ID}`);
            } catch (e2) {
                console.log(`  ⚠️ database ${DATABASE_ID}: ${e2.message || e2}`);
            }
        } else {
            console.log(`  ⚠️ database ${DATABASE_ID}: ${e.message || e}`);
        }
    }
}

// ---------- تعریف اسکیمای پرامپت ۰۳ ----------

const GUIDED_SESSIONS_COLS = [
    { key: 'slug', type: 'string', size: 64, required: true },
    { key: 'category', type: 'enum', elements: ['mindfulness', 'calm', 'hypnosis'], required: true },
    { key: 'titleFa', type: 'string', size: 128, required: true },
    { key: 'subtitleFa', type: 'string', size: 256, required: false },
    { key: 'eyesClosed', type: 'boolean', required: false, default: true },
    { key: 'totalSec', type: 'integer', required: true },
    { key: 'audioCueId', type: 'string', size: 128, required: true },  // g-hyp01:start|deep|core|end
    { key: 'referenceImageUrl', type: 'string', size: 512, required: false },
    { key: 'timingMap', type: 'string', size: 2000, required: false }, // JSON استاندارد ۴ بخش
    { key: 'addressFa', type: 'string', size: 32, required: false, default: 'زهرا جان' },
    { key: 'orderIndex', type: 'integer', required: false, default: 0 },
    { key: 'tags', type: 'string', size: 512, required: false, default: '[]' },
];

const GUIDED_PROGRESS_COLS = [
    { key: 'userId', type: 'string', size: 64, required: true },
    { key: 'slug', type: 'string', size: 64, required: true },
    { key: 'lastPart', type: 'integer', required: false, default: 1 },
    { key: 'lastPositionSec', type: 'float', required: false, default: 0 },
    { key: 'isCompleted', type: 'boolean', required: false, default: false },
    { key: 'viewCount', type: 'integer', required: false, default: 0 },
    { key: 'updatedAt', type: 'datetime', required: false },
];

async function main() {
    console.log('🚀 مهاجرت پرامپت ۰۳ — guided sessions');
    await tryCreateDatabase();

    console.log('\n🗄 جدول guided_sessions (کاتالوگ عمومی — فقط‌خواندنی برای کاربر)');
    await tryCreateTable('guided_sessions', 'جلسات هدایت‌شده (ذهن‌آگاهی/آرامش/خودهیپنوتیزم)', ['read("any")'], false);
    for (const col of GUIDED_SESSIONS_COLS) await tryCreateColumn('guided_sessions', col);
    await tryCreateIndex('guided_sessions', { key: 'by_category_order', type: 'key', attributes: ['category', 'orderIndex'] });
    await tryCreateIndex('guided_sessions', { key: 'by_slug', type: 'unique', attributes: ['slug'] });

    console.log('\n🗄 جدول guided_progress (خصوصی — Row Security روشن، الگوی prompt-01)');
    await tryCreateTable('guided_progress', 'پیشرفت جلسات هدایت‌شده', ['create("users")'], true);
    for (const col of GUIDED_PROGRESS_COLS) await tryCreateColumn('guided_progress', col);
    await tryCreateIndex('guided_progress', { key: 'by_user_slug', type: 'unique', attributes: ['userId', 'slug'] });

    console.log(`\n📦 باکت مدیا: ${BUCKET_ID} (wellness-media موجود — از پرامپت ۰۲ استفاده می‌شود، ساخت جدید لازم نیست)`);

    console.log('\n✅ مهاجرت پرامپت ۰۳ تمام شد.');
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
