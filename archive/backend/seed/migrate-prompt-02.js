/**
 * مهاجرت پرامپت ۰۲: ساخت جداول wellness_moves / sketch_references / wellness_logs
 *                    و باکت wellness-media.
 *
 * ایمن: اگر ستون/جدول/باکت از قبل باشد، skip می‌شود (best-effort).
 * اجرا:   node backend/seed/migrate-prompt-02.js
 * خشک:   node backend/seed/migrate-prompt-02.js --dry-run
 */
const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const BUCKET_ID = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b'; // Bucket ID واقعی wellness-media
const BUCKET_NAME = 'wellness-media';

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const tablesDb = new sdk.TablesDB(client);
const storage = new sdk.Storage(client);
const databases = new sdk.Databases(client); // برای ایجاد database اگر نباشد

// نگاشت type فیلد به متد مناسب در TablesDB v18+
function createColumnFn(type) {
    const map = {
        'string': 'createStringColumn',
        'integer': 'createIntegerColumn',
        'boolean': 'createBooleanColumn',
        'float': 'createFloatColumn',
        'datetime': 'createDatetimeColumn',
        'email': 'createEmailColumn',
        'url': 'createUrlColumn',
        'ip': 'createIpColumn',
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
        console.log(`  ⚠️ ${tableId}.${col.key}: type ${col.type} ناشناخته`);
        return;
    }
    const args = {
        databaseId: DATABASE_ID,
        tableId,
        key: col.key,
        size: col.size,
        required: !!col.required,
    };
    // فقط برای ستون‌هایی که default دارند، default بفرست
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
        // Appwrite v18+: نام پارامتر از attributes به columns تغییر کرده
        await tablesDb.createIndex({
            databaseId: DATABASE_ID,
            tableId,
            key: idx.key,
            type: idx.type,
            columns: idx.attributes, // نام قدیم: attributes
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
    // ابتدا چک کن database وجود دارد یا نه
    try {
        await databases.get({ databaseId: DATABASE_ID });
        console.log(`  · database ${DATABASE_ID} (exists)`);
    } catch (e) {
        if (e.code === 404) {
            try {
                if (dryRun) return console.log(`  [dry] createDatabase ${DATABASE_ID}`);
                await databases.create({
                    databaseId: DATABASE_ID,
                    name: 'پایگاه‌داده‌ی زهرا',
                });
                console.log(`  ✅ database ${DATABASE_ID}`);
            } catch (e2) {
                console.log(`  ⚠️ database ${DATABASE_ID}: ${e2.message || e2}`);
            }
        } else {
            console.log(`  ⚠️ database ${DATABASE_ID}: ${e.message || e}`);
        }
    }
}

async function tryCreateBucket(id, name, maxSize, perms) {
    try {
        if (dryRun) return console.log(`  [dry] createBucket ${id}`);
        await storage.createBucket({
            bucketId: id,
            name,
            maximumFileSize: maxSize,
            fileSecurity: false,
            permissions: perms,
        });
        console.log(`  ✅ bucket ${id}`);
    } catch (e) {
        const msg = String(e.message || e);
        if (msg.includes('already exists') || msg.includes('duplicate') || msg.includes('409')) {
            console.log(`  · bucket ${id} (exists)`);
        } else {
            console.log(`  ⚠️ bucket ${id}: ${msg}`);
        }
    }
}

/** بررسی وجود bucket واقعی wellness-media؛ اگر نبود، با همان ID می‌سازد. */
async function tryEnsureBucket(id, name) {
    try {
        if (dryRun) return console.log(`  [dry] getBucket ${id} (${name})`);
        await storage.getBucket({ bucketId: id });
        console.log(`  · bucket ${id} (exists — ${name})`);
    } catch (e) {
        if (e.code === 404) {
            await tryCreateBucket(id, name, 50 * 1024 * 1024, ['read("any")']);
        } else {
            console.log(`  ⚠️ bucket ${id}: ${e.message || e}`);
        }
    }
}

async function migrate() {
    console.log('پرامپت ۰۲ migration: wellness tables + bucket');

    // 0) database — اگر نباشد، ایجاد کن
    await tryCreateDatabase();

    // 1) wellness_moves
    await tryCreateTable('wellness_moves', 'حرکات سلامتی', ['read("any")'], false);
    const movesCols = [
        { key: 'category', type: 'string', size: 16, required: true, default: 'yoga' },
        { key: 'titleFa', type: 'string', size: 128, required: true },
        { key: 'slug', type: 'string', size: 64, required: true },
        { key: 'level', type: 'integer', required: false, default: 1 },
        { key: 'durationSec', type: 'integer', required: false, default: 60 },
        { key: 'reps', type: 'integer', required: false, default: 0 },
        { key: 'instructionsFa', type: 'string', size: 2048, required: false, default: '' },
        { key: 'audioCueId', type: 'string', size: 256, required: false, default: '' },
        { key: 'referenceImageUrl', type: 'string', size: 1024, required: false, default: '' }, // 1024 کافی است برای URL کامل
        { key: 'referenceImagePromptTemplate', type: 'string', size: 4096, required: false, default: '' },
        { key: 'orderIndex', type: 'integer', required: false, default: 0 },
        { key: 'tags', type: 'string', size: 512, required: false, default: '[]' },
    ];
    for (const col of movesCols) await tryCreateColumn('wellness_moves', col);
    await tryCreateIndex('wellness_moves', { key: 'moveSlugIdx', type: 'unique', attributes: ['slug'] });
    await tryCreateIndex('wellness_moves', { key: 'moveCategoryIdx', type: 'key', attributes: ['category', 'orderIndex'] });

    // 2) sketch_references
    await tryCreateTable('sketch_references', 'مرجع‌های نقاشی', ['create("users")'], true);
    const sketchCols = [
        { key: 'userId', type: 'string', size: 64, required: true },
        { key: 'subject', type: 'string', size: 32, required: true },
        { key: 'level', type: 'integer', required: true },
        { key: 'titleFa', type: 'string', size: 128, required: false, default: '' },
        { key: 'imageUrl', type: 'string', size: 1024, required: true },
        { key: 'generatedAtIso', type: 'string', size: 32, required: true },
    ];
    for (const col of sketchCols) await tryCreateColumn('sketch_references', col);
    await tryCreateIndex('sketch_references', { key: 'sketchUserIdx', type: 'key', attributes: ['userId', 'generatedAtIso'] });

    // 3) wellness_logs
    await tryCreateTable('wellness_logs', 'گزارش جلسه‌ی سلامتی', ['create("users")'], true);
    const logCols = [
        { key: 'userId', type: 'string', size: 64, required: true },
        { key: 'moveSlug', type: 'string', size: 64, required: true },
        { key: 'category', type: 'string', size: 16, required: true },
        { key: 'dayIso', type: 'string', size: 10, required: true },
        { key: 'secondsSpent', type: 'integer', required: false, default: 0 },
        { key: 'completed', type: 'boolean', required: false, default: false },
    ];
    for (const col of logCols) await tryCreateColumn('wellness_logs', col);
    await tryCreateIndex('wellness_logs', { key: 'wellnessLogUserDayIdx', type: 'key', attributes: ['userId', 'dayIso'] });

    // 4) bucket — بررسی/ایجاد bucket واقعی wellness-media (ID: 6aa1eaae00303400117b)
    await tryEnsureBucket(BUCKET_ID, BUCKET_NAME);

    console.log('\n✅ مهاجرت پرامپت ۰۲ تمام شد.');
}

migrate().catch((e) => { console.error('❌', e); process.exit(1); });
