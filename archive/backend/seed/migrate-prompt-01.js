/**
 * مهاجرت پرامپت ۰۱: ساخت جدول lesson_media_progress + ستون‌های جدید درس.
 *
 *  - اضافه می‌کند به جدول lessons: bookCode, lessonNumber, hasVideo, hasAudio,
 *    videoUrl, audioUrl, chapterMarkers, flashcardSetId, questionBankId,
 *    examRangeGroup, prerequisiteSummaryId.
 *  - جدول lesson_media_progress را از صفر می‌سازد.
 *
 * پیش‌نیاز:   APPWRITE_PROJECT_ID و APPWRITE_API_KEY در محیط.
 * اجرا:        node backend/seed/migrate-prompt-01.js
 * خشک:        node backend/seed/migrate-prompt-01.js --dry-run
 *
 * ایمن: اگر ستون/جدول از قبل باشد، خطا می‌دهد ولی متوقف نمی‌شود (best-effort).
 */
const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');

const client = new sdk.Client()
    .setEndpoint(ENDPOINT)
    .setProject(PROJECT_ID)
    .setKey(API_KEY);
const tables = new sdk.TablesDB(client);

async function tryCreateColumn(tableId, column) {
    try {
        if (dryRun) {
            console.log(`  [dry] createColumn ${tableId}.${column.key} (${column.type})`);
            return;
        }
        // SDK 18 (Appwrite 2.x): هر نوع، متد خودش را دارد.
        const base = { databaseId: DATABASE_ID, tableId, key: column.key, required: !!column.required };
        if (column.type === 'string') {
            await tables.createStringColumn({ ...base, size: column.size || 256, default: column.default ?? '' });
        } else if (column.type === 'boolean') {
            await tables.createBooleanColumn({ ...base, default: column.default ?? false });
        } else if (column.type === 'integer') {
            await tables.createIntegerColumn({ ...base, default: column.default ?? 0 });
        } else if (column.type === 'double') {
            await tables.createFloatColumn({ ...base, default: column.default ?? 0 });
        } else {
            throw new Error(`نوع پشتیبانی‌نشده: ${column.type}`);
        }
        console.log(`  ✅ ${tableId}.${column.key}`);
    } catch (e) {
        const msg = String(e && e.message || e);
        if (msg.includes('already exists') || msg.includes('duplicate')) {
            console.log(`  · ${tableId}.${column.key} (exists)`);
        } else {
            console.log(`  ⚠️ ${tableId}.${column.key}: ${msg}`);
        }
    }
}

async function ensureProgressTable() {
    if (dryRun) {
        console.log('[dry] ensure lesson_media_progress table');
        return;
    }
    try {
        await tables.createTable({
            databaseId: DATABASE_ID,
            tableId: 'lesson_media_progress',
            name: 'پیشرفت رسانه‌ی درس',
            permissions: ['create("users")'],
            rowSecurity: true,
        });
        console.log('✅ lesson_media_progress table created');
    } catch (e) {
        if (String(e.message || e).includes('already exists')) {
            console.log('· lesson_media_progress already exists');
        } else {
            throw e;
        }
    }
}

async function migrate() {
    console.log('پرامپت ۰۱ migration: lessons + lesson_media_progress');

    // ستون‌های جدید درس
    const newLessonCols = [
        { key: 'bookCode', type: 'string', size: 16, required: false, default: '' },
        { key: 'bookTitleFa', type: 'string', size: 128, required: false, default: '' },
        { key: 'subjectOrder', type: 'integer', required: false, default: 0 },
        { key: 'lessonNumber', type: 'integer', required: false, default: 0 },
        { key: 'lessonTitleFa', type: 'string', size: 256, required: false, default: '' },
        { key: 'hasVideo', type: 'boolean', required: false, default: false },
        { key: 'hasAudio', type: 'boolean', required: false, default: false },
        { key: 'videoUrl', type: 'string', size: 1024, required: false, default: '' },
        { key: 'audioUrl', type: 'string', size: 1024, required: false, default: '' },
        { key: 'chapterMarkers', type: 'string', size: 1024, required: false, default: '[]' },
        { key: 'flashcardSetId', type: 'string', size: 64, required: false, default: '' },
        { key: 'questionBankId', type: 'string', size: 64, required: false, default: '' },
        { key: 'examRangeGroup', type: 'string', size: 32, required: false, default: '' },
        { key: 'prerequisiteSummaryId', type: 'string', size: 64, required: false, default: '' },
    ];
    for (const col of newLessonCols) {
        await tryCreateColumn('lessons', col);
    }

    // ساخت جدول پیشرفت + ستون‌ها
    await ensureProgressTable();
    const progressCols = [
        { key: 'userId', type: 'string', size: 64, required: true },
        { key: 'bookCode', type: 'string', size: 16, required: true },
        { key: 'lessonId', type: 'string', size: 64, required: true },
        { key: 'mediaType', type: 'string', size: 8, required: true, default: 'video' },
        { key: 'lastPositionSec', type: 'double', required: false, default: 0 },
        { key: 'durationSec', type: 'double', required: false, default: 0 },
        { key: 'playbackSpeed', type: 'double', required: false, default: 1 },
        { key: 'isCompleted', type: 'boolean', required: false, default: false },
        { key: 'viewCount', type: 'integer', required: false, default: 0 },
        { key: 'lastViewedAtIso', type: 'string', size: 32, required: false, default: '' },
        { key: 'viewHistory', type: 'string', size: 16384, required: false, default: '[]' },
        { key: 'seekJumps', type: 'string', size: 16384, required: false, default: '[]' },
        { key: 'updatedAtIso', type: 'string', size: 32, required: false, default: '' },
    ];
    for (const col of progressCols) {
        await tryCreateColumn('lesson_media_progress', col);
    }

    console.log('\n✅ مهاجرت تمام شد.');
}

migrate().catch((e) => {
    console.error('❌', e);
    process.exit(1);
});
