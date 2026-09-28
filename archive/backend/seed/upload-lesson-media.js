#!/usr/bin/env node
/**
 * آپلود رسانه‌ی درس‌ها به باکت wellness-media (عملیاتیِ پرامپت ۰۱).
 *
 * منبع فایل‌ها: wellness-references/School-books-9 با قرارداد
 *   <Cxxx>-<Lnn>-<V|A><NN>.mp4|mp3   یا   <Cxxx>-<E01-Lnn>-... (کتاب‌های فصلی مثل ریاضی)
 * fileId = نام کامل فایل — چون پلیر با expandChapters فصل‌های V01..V03 را از روی نام می‌سازد.
 *
 * حالت‌ها:
 *   --test-fallback        برای placeholderهای صفر بایتی، «فایل مستر تست»
 *                          (artifacts/test-media/test.mp4|mp3) را زیر همان fileId واقعی آپلود می‌کند
 *                          تا کل زنجیره‌ی پخش بدون رسانه‌ی نهایی هم قابل تست باشد.
 *   --upgrade-test         اگر فایل موجود در باکت «تست» باشد (اندازه‌اش دقیقاً اندازه‌ی مستر)
 *                          و نسخه‌ی واقعی در ریپو باشد → حذف و آپلود واقعی (ارتقای رسانه).
 *   --seed-lessons         بعد از آپلود، سطر lessons (videoUrl/audioUrl/hasVideo/hasAudio)
 *                          فقط برای درس‌هایی که رسانه‌شان در باکت هست ساخته/به‌روز می‌شود.
 *   --dry-run              فقط گزارش، بدون نوشتن.
 *   --book C905            فقط یک کتاب.
 *
 * اسکوپ لازم: storage.write + files.read (+ tables.write برای سید)
 */
const sdk = require('node-appwrite');
const { InputFile } = require('node-appwrite/file');
const fs = require('fs');
const path = require('path');

const argv = process.argv.slice(2);
const dryRun = argv.includes('--dry-run');
const seedLessons = argv.includes('--seed-lessons');
const upgradeTest = argv.includes('--upgrade-test');
const testFallback = argv.includes('--test-fallback');
const bookFilter = (() => {
    const i = argv.indexOf('--book');
    return i > -1 ? argv[i + 1] : null;
})();

const ENDPOINT = (process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1').replace(/\/+$/, '');
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID || '';
const API_KEY = (process.env.APPWRITE_API_KEY || '').trim();
const BUCKET = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b';
const DB = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const ROOT = path.join(__dirname, '..', '..', 'wellness-references', 'School-books-9');
const TEST_DIR = path.join(__dirname, '..', '..', 'artifacts', 'test-media');

const MEDIA_RE = /^(C\d+)-((?:E\d+-)?(?:L|R)\d+)-([VA])(\d{2})\.(mp4|mp3)$/;
const CONCURRENCY = 3;

const MASTER = {
    V: path.join(TEST_DIR, 'test.mp4'),
    A: path.join(TEST_DIR, 'test.mp3'),
};
const masterSize = {};
for (const k of ['V', 'A']) {
    if (fs.existsSync(MASTER[k])) masterSize[k] = fs.statSync(MASTER[k]).size;
}
const useTestFallback = testFallback && masterSize.V && masterSize.A;

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const storage = new sdk.Storage(client);
const tables = new sdk.TablesDB(client);

function viewUrl(fileId) {
    return `${ENDPOINT}/storage/buckets/${BUCKET}/files/${fileId}/view?project=${PROJECT_ID}`;
}

/** همه‌ی رسانه‌ها: واقعی (غیرصفر) و placeholder (صفر) */
function collect() {
    const items = [];
    const walk = (dir) => {
        let entries = [];
        try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch (_) { return; }
        for (const e of entries) {
            const full = path.join(dir, e.name);
            if (e.isDirectory()) walk(full);
            else if (MEDIA_RE.test(e.name)) {
                const m = e.name.match(MEDIA_RE);
                const size = fs.statSync(full).size;
                const lessonDir = path.dirname(path.dirname(full));
                items.push({
                    full,
                    name: e.name,
                    size,
                    real: size > 0,
                    bookCode: m[1],
                    lessonCode: m[2],
                    kind: m[3],
                    lessonTitle: lessonDir.split(path.sep).pop().replace(/^[A-Za-z0-9-]+\s*-\s*/, ''),
                    bookDir: path.dirname(lessonDir).split(path.sep).pop().replace(/\s*\(C\d+\)$/, ''),
                });
            }
        }
    };
    walk(ROOT);
    return items;
}

async function getFileMeta(fileId) {
    try { return await storage.getFile({ bucketId: BUCKET, fileId }); }
    catch (e) {
        if (String(e && e.code) === '404') return null;
        throw e;
    }
}

function dieOnScope(e, what) {
    const msg = String(e && e.message || e);
    if (String(e && e.code) === '401' || /scope/i.test(msg)) {
        console.error(`\n❌ کلید API اجازه‌ی ${what} ندارد (401/scope).`);
        console.error('   کنسول Appwrite › Overview › Integrations › API Keys ← اسکوپ storage.write و files.read');
        console.error('   (و برای سید: tables.write) را اضافه کن؛ بعد secret گیت‌هاب APPWRITE_API_KEY را به‌روز کن.');
        process.exit(2);
    }
    throw e;
}

/** خطای گذرای سرور (5xx/شبکه) را با backoff دوباره امتحان می‌کند. */
async function withRetry(fn, tries = 4) {
    let last;
    for (let i = 0; i < tries; i++) {
        try {
            return await fn();
        } catch (e) {
            const code = String(e && e.code || '');
            const msg = String(e && e.message || e);
            const transient = code.startsWith('5') || /timeout|ECONN|socket|network|Server Error/i.test(msg);
            if (!transient || i === tries - 1) throw e;
            last = e;
            const wait = 2000 * (i + 1) * (i + 1);
            console.log(`    … خطای گذرا (${msg.slice(0, 40)}) — تلاش مجدد تا ${wait / 1000}s`);
            await new Promise(r => setTimeout(r, wait));
        }
    }
    throw last;
}

async function uploadOne(f, label) {
    if (dryRun) {
        console.log(`  [dry] ${label} (${((f.size || masterSize[f.kind]) / 1024).toFixed(0)} KB)`);
        return;
    }
    await withRetry(() => storage.createFile({
        bucketId: BUCKET,
        fileId: f.name,
        file: InputFile.fromPath(f.full, f.name),
    }));
    console.log(`  ${label} ✅`);
}

async function processItem(f) {
    // فیلتر کتاب
    if (bookFilter && f.bookCode !== bookFilter) return null;
    const meta = await getFileMeta(f.name);

    // ۱) فایل واقعی در ریپو
    if (f.real) {
        if (meta) {
            const isTest = upgradeTest && masterSize[f.kind] === meta.sizeOriginal;
            if (isTest) {
                if (!dryRun) await withRetry(() => storage.deleteFile({ bucketId: BUCKET, fileId: f.name }));
                await uploadOne(f, `🔁 ارتقای تست→واقعی ${f.name}`);
                return { state: 'upgraded', f };
            }
            console.log(`  · ${f.name} هست (skip)`);
            return { state: 'exists', f };
        }
        await uploadOne(f, `⬆ ${f.name} (${(f.size / 1048576).toFixed(2)} MB)`);
        return { state: 'uploaded-real', f };
    }

    // ۲) placeholder صفر بایتی
    if (useTestFallback) {
        if (meta) { console.log(`  · ${f.name} هست (تست — skip)`); return { state: 'exists', f }; }
        await uploadOne({ ...f, full: MASTER[f.kind], size: masterSize[f.kind] }, `🧪 تست ${f.name} ← مستر`);
        return { state: 'uploaded-test', f };
    }
    return { state: 'placeholder', f };
}

async function uploadAll(items) {
    const uniq = new Map();
    for (const f of items) if (!uniq.has(f.name)) uniq.set(f.name, f);
    const list = [...uniq.values()].filter(f => !bookFilter || f.bookCode === bookFilter);
    const realCount = list.filter(f => f.real).length;
    const phCount = list.length - realCount;

    console.log(`📁 مجموع رسانه‌ها: ${list.length} (واقعی: ${realCount} | placeholder: ${phCount})`);
    if (useTestFallback) console.log(`🧪 حالت تست فعال: placeholderها با مستر (${masterSize.V / 1024 | 0}KB mp4 / ${masterSize.A / 1024 | 0}KB mp3) پر می‌شوند.`);
    if (list.length === 0) { console.log('⚠️ چیزی برای انجام نیست.'); return new Map(); }

    const stats = { 'uploaded-real': 0, 'uploaded-test': 0, exists: 0, upgraded: 0, placeholder: 0 };
    const byLesson = new Map();
    let idx = 0, failed = 0;
    const failedNames = [];

    async function worker() {
        while (idx < list.length) {
            const f = list[idx++];
            try {
                const r = await processItem(f);
                if (!r) continue;
                stats[r.state]++;
                if (r.state !== 'placeholder') {
                    const key = `${f.bookCode}|${f.lessonCode}`;
                    const g = byLesson.get(key) || { hasVideo: false, hasAudio: false, f };
                    if (f.kind === 'V') g.hasVideo = true; else g.hasAudio = true;
                    byLesson.set(key, g);
                }
            } catch (e) {
                failed++;
                failedNames.push(f.name);
                console.log(`  ❌ ${f.name}: ${e && e.message || e}`);
                const msg = String(e && e.message || e);
                if (String(e && e.code) === '401' || /scope/i.test(msg)) dieOnScope(e, 'آپلود (storage.write)');
            }
        }
    }
    await Promise.all(Array.from({ length: CONCURRENCY }, worker));

    console.log(`\n📊 آپلود واقعی: ${stats['uploaded-real']} | تست: ${stats['uploaded-test']} | ارتقا: ${stats.upgraded} | از قبل بود: ${stats.exists} | placeholder باقی‌مانده: ${stats.placeholder} | خطا: ${failed}`);
    if (failedNames.length) {
        console.log(`⚠️ شکست‌خورده‌ها (اجرای مجدد idempotent است): ${failedNames.slice(0, 10).join('، ')}${failedNames.length > 10 ? ' …' : ''}`);
    }
    global.__uploadFailed = failedNames.length;
    return byLesson;
}

/** سطر را می‌نویسد؛ اگر سرور «Unknown attribute: X» داد، X را از payload حذف و دوباره. */
async function writeRowResilient(rowId, data, exists) {
    let payload = { ...data };
    for (let i = 0; i < 8; i++) {
        try {
            if (exists) await tables.updateRow({ databaseId: DB, tableId: 'lessons', rowId, data: payload });
            else await tables.createRow({ databaseId: DB, tableId: 'lessons', rowId, data: payload });
            return { ok: true };
        } catch (e) {
            const m = String(e && e.message || e).match(/Unknown attribute:\s*"?([A-Za-z0-9_]+)"?/i);
            if (m && payload[m[1]] !== undefined) {
                console.log(`    · ستون «${m[1]}» روی سرور نیست — از payload حذف شد`);
                delete payload[m[1]];
                continue;
            }
            throw e;
        }
    }
    return { ok: false };
}

async function seedLessonsRows(byLesson) {
    const keys = [...byLesson.keys()];
    if (keys.length === 0) { console.log('\n(سید lessons: درسی با رسانه‌ی موجود پیدا نشد)'); return; }
    console.log(`\n🌱 سید lessons برای ${keys.length} درس …`);
    let created = 0, updated = 0, failed = 0;
    for (const key of keys) {
        const g = byLesson.get(key);
        const { hasVideo, hasAudio } = g;
        const f = g.f;
        const rowId = `lesson-${f.bookCode}-${f.lessonCode}`;
        const data = {
            title: f.lessonTitle || `${f.bookDir} — ${f.lessonCode}`,
            subject: f.bookDir || '',
            grade: 9,
            body: '',
            bookCode: f.bookCode,
            bookTitleFa: f.bookDir || '',
            lessonTitleFa: f.lessonTitle || f.lessonCode,
            lessonNumber: 0,
            hasVideo, hasAudio,
            videoUrl: hasVideo ? viewUrl(`${f.bookCode}-${f.lessonCode}-V01.mp4`) : '',
            audioUrl: hasAudio ? viewUrl(`${f.bookCode}-${f.lessonCode}-A01.mp3`) : '',
            chapterMarkers: '[]',
        };
        try {
            let exists = true;
            try { await tables.getRow({ databaseId: DB, tableId: 'lessons', rowId }); }
            catch (_) { exists = false; }
            if (dryRun) { console.log(`  [dry] ${exists ? 'update' : 'create'} ${rowId} (V:${hasVideo ? '✓' : '—'} A:${hasAudio ? '✓' : '—'})`); continue; }
            const r = await writeRowResilient(rowId, data, exists);
            if (!r.ok) { failed++; console.log(`  ⚠️ ${rowId}: چند ستون غایب بود و نوشته نشد`); continue; }
            if (exists) updated++; else created++;
            console.log(`  ✅ ${rowId} (V:${hasVideo ? '✓' : '—'} A:${hasAudio ? '✓' : '—'})`);
        } catch (e) {
            failed++;
            console.log(`  ❌ ${rowId}: ${e && e.message || e}`);
            dieOnScope(e, 'نوشتن جدول lessons (tables.write)');
        }
    }
    console.log(`📊 سید: ساخته ${created} | به‌روز ${updated} | خطا ${failed}`);
}

(async () => {
    console.log('آپلود رسانه‌ی درس‌ها به باکت wellness-media');
    console.log(`endpoint: ${ENDPOINT} | project: ${PROJECT_ID} | bucket: ${BUCKET}${dryRun ? ' | [DRY-RUN]' : ''}`);
    const items = collect();
    const byLesson = await uploadAll(items);
    if (seedLessons) await seedLessonsRows(byLesson);
    console.log('\nتمام شد.');
    if (global.__uploadFailed) process.exit(1);
})();
