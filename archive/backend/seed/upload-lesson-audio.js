#!/usr/bin/env node
/**
 * آپلود روخوانی درس‌ها (فایل صوتی هر درس) به باکت wellness-media + هماهنگ‌سازی جدول lesson_audio.
 *  - منبع: پوشه‌ی «media/» داخل هر درس از School-books-9 (فایل‌های *_AUDIO.mp3)
 *  - fileId = نام کامل فایل (C901_L01_AUDIO.mp3) — پلیر اپ از همین الگو URL می‌سازد.
 *  - Idempotent: فایل موجود skip می‌شود. --force = حذف و آپلود مجدد.
 *  - بعد از آپلود: سطرهای lesson_audio را upsert می‌کند (جدول اگر نبود ساخته می‌شود).
 * محیط: APPWRITE_ENDPOINT/APPWRITE_PROJECT_ID/APPWRITE_API_KEY/APPWRITE_BUCKET_ID/APPWRITE_DATABASE_ID
 */
process.env.NODE_ENV = 'production';
const fs = require('fs');
const path = require('path');
const sdk = require('node-appwrite');
const { InputFile } = require('node-appwrite/file');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT = process.env.APPWRITE_PROJECT_ID || '6a9d59e3002751cc3ea8';
const KEY = process.env.APPWRITE_API_KEY;
const BUCKET = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b';
const DB = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const TABLE = 'lesson_audio';
const ROOT = path.resolve(__dirname, '../../wellness-references/School-books-9');
const dryRun = process.argv.includes('--dry-run');
const force = process.argv.includes('--force');
const listOnly = process.argv.includes('--list');
const convention = process.argv.includes('--convention');

if (!KEY) { console.error('❌ APPWRITE_API_KEY ست نیست'); process.exit(2); }

const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT).setKey(KEY);
const storage = new sdk.Storage(client);

// --list: فهرست کامل فایل‌های باکت (fileId\tsize) — برای هماهنگ‌سازی/رینیم
if (listOnly) {
    (async () => {
        let offset = 0;
        let total = null;
        let count = 0;
        do {
            const res = await withRetry(() => storage.listFiles({
                bucketId: BUCKET,
                queries: [sdk.Query.limit(100), sdk.Query.offset(offset)],
            }));
            total = res.total;
            for (const f of res.files) { console.log(`${f.$id || f.name}\t${f.sizeOriginal}`); count++; }
            offset += 100;
        } while (offset < total);
        console.log(`#TOTAL ${count}`);
    })().then(() => process.exit(0)).catch(e => { console.error('❌', e.message); process.exit(1); });
} else if (convention) {
    // v1.17 — یکدست‌سازی باکت با قرارداد سراسری:
    //  صوت هر پک: <packId>_AUDIO.mp3 (placeholder برای ناقص‌ها؛ REALها دست‌نخورده)
    //  ویدیوی هر پک: <dash>-V01.mp4
    //  حذف: بخش‌های -A0n.mp3 و ویدیوهای -V02/-V03.mp4 (همه placeholder)
    (async () => {
        const meta = JSON.parse(fs.readFileSync(path.resolve(__dirname, 'pack-list.json'), 'utf8'));
        const sampleAudio = fs.readFileSync(path.resolve(__dirname, 'sample-audio.mp3'));
        const sampleVideo = fs.readFileSync(path.resolve(__dirname, 'sample-video.mp4'));

        const existing = new Map();
        let offset = 0;
        for (;;) {
            const res = await withRetry(() => storage.listFiles({ bucketId: BUCKET, queries: [sdk.Query.limit(100), sdk.Query.offset(offset)] }));
            for (const f of res.files) existing.set(f.$id || f.name, f.sizeOriginal);
            offset += 100;
            if (offset >= res.total) break;
        }
        console.log(`📦 باکت: ${existing.size} فایل`);

        let up = 0, keep = 0;
        const wanted = new Set();
        for (const pk of meta.packs) {
            const audio = `${pk.packId}_AUDIO.mp3`;
            wanted.add(audio);
            if (pk.authoredAudio) wanted.add(pk.authoredAudio);
            wanted.add(`${pk.dash}-V01.mp4`);
        }
        for (const name of wanted) {
            if (existing.has(name)) { keep++; continue; }
            const buf = name.endsWith('.mp4') ? sampleVideo : sampleAudio;
            try {
                await withRetry(() => storage.createFile({
                    bucketId: BUCKET, fileId: name,
                    file: InputFile.fromBuffer(buf, name),
                }));
                console.log(`  ⬆ ${name}`);
                up++;
            } catch (e) {
                if (/already exists/i.test(String(e && e.message))) { keep++; continue; }
                throw e;
            }
        }
        // حذف غیرقراردادی‌ها
        let del = 0;
        for (const name of existing.keys()) {
            const isStale = /-A0\d\.mp3$/.test(name) || /-V0[23]\.mp4$/.test(name);
            if (!isStale || wanted.has(name)) continue;
            await withRetry(() => storage.deleteFile({ bucketId: BUCKET, fileId: name }));
            console.log(`  🗑 ${name}`);
            del++;
        }
        console.log(`📊 آپلود: ${up} | از قبل: ${keep} | حذف: ${del}`);
    })().then(() => process.exit(0)).catch(e => { console.error('❌', e.message); process.exit(1); });
} else if (!fs.existsSync(ROOT)) { console.error('❌ مسیر کتاب‌ها نیست:', ROOT); process.exit(2); }
let tables = null;
try { tables = new sdk.TablesDB(client); } catch (_) { tables = new sdk.Databases(client); }

function withRetry(fn, tries = 4) {
    return (async () => {
        for (let i = 0; i < tries; i++) {
            try { return await fn(); }
            catch (e) {
                const code = String((e && e.code) || '');
                const msg = String((e && e.message) || e);
                const transient = code.startsWith('5') || /timeout|ECONN|socket|network|Server Error/i.test(msg);
                if (!transient || i === tries - 1) throw e;
                const wait = 2000 * (i + 1) * (i + 1);
                console.log(`    … خطای گذرا (${msg.slice(0, 40)}) — تلاش مجدد تا ${wait / 1000}s`);
                await new Promise(r => setTimeout(r, wait));
            }
        }
    })();
}

// شناسه‌های پک و عنوان‌ها — هماهنگ با BookModuleRegistry اپ
const PACK_INFO = {
    'C901_L01_AUDIO.mp3':        { packId: 'C901_L01',      bookCode: 'C901', lessonId: 'L01',      title: 'آموزش قرآن نهم — درس ۱ (روخوانی آیات دو زبانه)', voices: 'مرد عربی + زن فارسی' },
    'C903_E01-L01-1_AUDIO.mp3':  { packId: 'C903_E01-L01-1', bookCode: 'C903', lessonId: 'E01-L01-1', title: 'فارسی نهم — درس ۱ بخش ۱ (قصیده، آموزش و تمرین‌ها)', voices: 'مرد فارسی (با اعراب خفیف)' },
    'C903_E01-L01-2_AUDIO.mp3':  { packId: 'C903_E01-L01-2', bookCode: 'C903', lessonId: 'E01-L01-2', title: 'فارسی نهم — درس ۱ بخش ۲ (حکایت: سفر)', voices: 'مرد فارسی (با اعراب خفیف)' },
    'C909_L01_AUDIO.mp3':        { packId: 'C909_L01',      bookCode: 'C909', lessonId: 'L01',      title: 'عربی نهم — درس ۱ (واژه‌نامه دو زبانه)', voices: 'مرد عربی + زن فارسی' },
    'C910_L01_AUDIO.mp3':        { packId: 'C910_L01',      bookCode: 'C910', lessonId: 'L01',      title: 'انگلیسی نهم — درس ۱ (مکالمه، ملودی و گرامر)', voices: '۲ مرد + ۲ زن (انگلیسی) + زن فارسی' },
};

async function ensureTable() {
    try {
        await tables.getTable({ databaseId: DB, tableId: TABLE });
        console.log('جدول lesson_audio از قبل هست');
    } catch (e) {
        if (!/404|not_found/i.test(String((e && e.code) || '') + String((e && e.message) || e))) throw e;
        console.log('ساخت جدول lesson_audio…');
        await tables.createTable({ databaseId: DB, tableId: TABLE, name: 'lesson_audio', permissions: [] });
        const cols = [
            ['packId', 'string', 128, true], ['bookCode', 'string', 16, true],
            ['lessonId', 'string', 64, true], ['fileId', 'string', 256, true],
            ['title', 'string', 256, false], ['voices', 'string', 256, false],
            ['durationSec', 'integer', false], ['sizeBytes', 'integer', false],
            ['updatedAt', 'datetime', false],
        ];
        for (const [key, type, size, required] of cols) {
            const base = { databaseId: DB, tableId: TABLE, key, required: !!required };
            if (type === 'string') await tables.createStringColumn({ ...base, size: typeof size === 'number' ? size : 256 });
            else if (type === 'integer') await tables.createIntegerColumn({ ...base, required: false, default: null });
            else await tables.createDatetimeColumn({ ...base, required: false });
            console.log(`  + ستون ${key} (${type})`);
        }
        // ایندکس یکتا روی packId
        try { await tables.createIndex({ databaseId: DB, tableId: TABLE, key: 'packId', type: 'unique' }); } catch (_) {}
    }
}

async function upsertRow(info, sizeBytes, durationSec) {
    const now = new Date().toISOString();
    const data = {
        packId: info.packId, bookCode: info.bookCode, lessonId: info.lessonId,
        fileId: info.fileId, title: info.title, voices: info.voices,
        sizeBytes: Math.round(sizeBytes), updatedAt: now,
    };
    if (durationSec) data.durationSec = Math.round(durationSec);
    try {
        const list = await tables.listRows({ databaseId: DB, tableId: TABLE, queries: [sdk.Query.equal('packId', info.packId)] });
        if (list.rows.length > 0) {
            await tables.updateRow({ databaseId: DB, tableId: TABLE, rowId: list.rows[0].$id, data });
            return 'updated';
        }
        await tables.createRow({ databaseId: DB, tableId: TABLE, rowId: 'unique()', data });
        return 'created';
    } catch (e) {
        console.log(`  ⚠️ upsert row ${info.packId}: ${e.message}`);
        return 'failed';
    }
}

function mp3DurationSec(buf) {
    // تخمین ساده: بیت‌ریت ثابت 64kbps جبری — برای نمایش کافی است؛ دقیق‌تر با ffprobe سرور.
    return Math.round(buf.length * 8 / 64000);
}

if (!listOnly && !convention) (async () => {
    console.log('آپلود روخوانی درس‌ها + هماهنگ‌سازی lesson_audio');
    console.log(`endpoint: ${ENDPOINT} | project: ${PROJECT} | bucket: ${BUCKET} | db: ${DB} ${dryRun ? '| [DRY-RUN]' : ''}`);
    // پیدا کردن *_AUDIO.mp3 داخل پوشه‌های media/
    const items = [];
    (function walk(dir) {
        for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
            const p = path.join(dir, e.name);
            if (e.isDirectory()) walk(p);
            else if (e.isFile() && /_AUDIO\.mp3$/.test(e.name) && path.basename(dir) === 'media') {
                items.push({ name: e.name, full: p, size: fs.statSync(p).size });
            }
        }
    })(ROOT);
    console.log(`📁 مجموع: ${items.length}`);
    if (items.length === 0) { console.log('هیچ *_AUDIO.mp3 در media/ نیست — هیچ کاری انجام نمی‌شود.'); return; }

    if (!dryRun) await ensureTable();

    let uploaded = 0, skipped = 0, failed = 0, rowsOk = 0;
    for (const it of items) {
        if (it.size < 1024) { console.log(`  ⏭ صفر/خیلی کوچک skip: ${it.name}`); skipped++; continue; }
        const info = PACK_INFO[it.name] || { packId: it.name.replace('_AUDIO.mp3', ''), bookCode: it.name.slice(0, 4), lessonId: '', title: it.name, voices: '' };
        try {
            if (dryRun) { console.log(`  [dry] آپلود ${it.name} (${(it.size / 1024).toFixed(0)}KB)`); continue; }
            let exists = false;
            try { await storage.getFile({ bucketId: BUCKET, fileId: it.name }); exists = true; } catch (_) {}
            if (exists && force) { try { await storage.deleteFile({ bucketId: BUCKET, fileId: it.name }); } catch (_) {} exists = false; }
            if (exists) {
                console.log(`  ✓ از قبل: ${it.name}`);
            } else {
                await withRetry(() => storage.createFile({
                    bucketId: BUCKET, fileId: it.name, file: InputFile.fromPath(it.full, it.name),
                }));
                console.log(`  ⬆ آپلود شد: ${it.name} (${(it.size / 1024).toFixed(0)}KB)`);
                uploaded++;
            }
            if (/INTRO/.test(it.name)) { console.log('    (فایل اینترو — فقط آپلود، بدون سطر جدول)'); continue; }
            const r = await upsertRow({ ...info, fileId: it.name }, it.size, mp3DurationSec(it.size));
            if (r !== 'failed') rowsOk++;
        } catch (e) {
            console.log(`  ❌ ${it.name}: ${e.message}`);
            failed++;
        }
    }
    // حذف فایل‌های منسوخ (رینیم‌شده) از باکت و جدول
    const PRUNE_FILEIDS = ['C903_E01-L01_AUDIO.mp3'];
    for (const fid of PRUNE_FILEIDS) {
        if (dryRun) { console.log(`  [dry] حذف منسوخ: ${fid}`); continue; }
        try { await storage.deleteFile({ bucketId: BUCKET, fileId: fid }); console.log(`  🗑 فایل منسوخ حذف شد: ${fid}`); }
        catch (e) { console.log(`  (فایل منسوخ در باکت نبود: ${fid})`); }
        try {
            const list = await tables.listRows({ databaseId: DB, tableId: TABLE, queries: [sdk.Query.equal('fileId', fid)] });
            for (const r of list.rows) { await tables.deleteRow({ databaseId: DB, tableId: TABLE, rowId: r.$id }); console.log(`  🗑 سطر منسوخ حذف شد: ${fid}`); }
        } catch (e) { console.log(`  ⚠ حذف سطر ${fid}: ${e.message}`); }
    }
    console.log(`📊 آپلود: ${uploaded} | از قبل: ${items.length - uploaded - skipped - failed} | skip: ${skipped} | خطا: ${failed} | سطر جدول: ${rowsOk}`);
    if (failed > 0) process.exit(1);
})().catch(e => { console.error('❌ شکست کلی:', e); process.exit(1); });
