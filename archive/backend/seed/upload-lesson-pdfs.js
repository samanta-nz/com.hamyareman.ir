#!/usr/bin/env node
/**
 * آپلود PDF کتاب درس‌ها به باکت wellness-media — پرامپت ۰۵ + تصمیم مصوب.
 *  - منبع: پوشه‌ی «PDF/» داخل هر درس از School-books-9 (فایل‌های *_BOOK.pdf)
 *  - fileId = نام کامل فایل (C905f01d01.pdf یا C905_E01-L01_BOOK.pdf)
 *  - Idempotent: فایل موجود skip می‌شود. --force = حذف و آپلود مجدد.
 * محیط: APPWRITE_ENDPOINT/APPWRITE_PROJECT_ID/APPWRITE_API_KEY/APPWRITE_BUCKET_ID
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
const ROOT = path.resolve(__dirname, '../../wellness-references/School-books-9');
const BOOKS09 = path.resolve(__dirname, '../../../Books/Base-09');
const dryRun = process.argv.includes('--dry-run');
const force = process.argv.includes('--force');

if (!KEY) { console.error('❌ APPWRITE_API_KEY ست نیست'); process.exit(2); }

const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT).setKey(KEY);
const storage = new sdk.Storage(client);

function withRetry(fn, tries = 4) {
    return (async () => {
        let last;
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
        throw last;
    })();
}

(async () => {
    console.log('آپلود PDF کتاب درس‌ها');
    console.log(`endpoint: ${ENDPOINT} | project: ${PROJECT} | bucket: ${BUCKET} ${dryRun ? '| [DRY-RUN]' : ''}`);
    const items = [];
    if (fs.existsSync(ROOT)) {
        (function walk(dir) {
            for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
                const p = path.join(dir, e.name);
                if (e.isDirectory()) walk(p);
                else if (e.isFile() && e.name.endsWith('.pdf') && /BOOK\.pdf$/.test(e.name) && path.basename(dir) === 'PDF') {
                    items.push({ name: e.name, full: p, size: fs.statSync(p).size });
                }
            }
        })(ROOT);
    }
    if (fs.existsSync(BOOKS09)) {
        (function walk(dir) {
            for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
                const p = path.join(dir, e.name);
                if (e.isDirectory()) walk(p);
                else if (e.isFile() && /^C905.+\.pdf$/i.test(e.name)) {
                    items.push({ name: e.name, full: p, size: fs.statSync(p).size });
                }
            }
        })(BOOKS09);
    }
    items.sort((a, b) => a.name.localeCompare(b.name));
    const zero = items.filter(i => i.size < 1024);
    console.log(`📁 مجموع: ${items.length} | صفر بایتی (skip): ${zero.length}`);

    let ok = 0, skip = 0, failed = 0, zeroSkipped = 0;
    const failedNames = [];
    for (const f of items) {
        try {
            if (f.size < 1024) { zeroSkipped++; continue; }
            let exists = false;
            try { await storage.getFile({ bucketId: BUCKET, fileId: f.name }); exists = true; } catch (_) {}
            if (exists && !force) { skip++; console.log(`  · ${f.name} (از قبل)`); continue; }
            if (dryRun) { console.log(`  [dry] ${f.name} (${Math.round(f.size / 1024)}KB)`); ok++; continue; }
            if (exists && force) await withRetry(() => storage.deleteFile({ bucketId: BUCKET, fileId: f.name }));
            await withRetry(() => storage.createFile({
                bucketId: BUCKET,
                fileId: f.name,
                file: InputFile.fromPath(f.full, f.name),
            }));
            ok++; console.log(`  ✅ ${f.name} (${Math.round(f.size / 1024)}KB)`);
        } catch (e) {
            failed++; failedNames.push(f.name);
            console.log(`  ❌ ${f.name}: ${(e && e.message) || e}`);
            if (String((e && e.code)) === '401' || /scope/i.test(String((e && e.message) || e))) {
                console.error('اسکوپ storage.write لازم است — اجرا قطع شد.');
                process.exit(3);
            }
        }
    }
    console.log(`\n📊 آپلود: ${ok} | از قبل: ${skip} | صفر بایتی skip: ${zeroSkipped} | خطا: ${failed}`);
    if (failedNames.length) console.log(`⚠️ شکست‌ها: ${failedNames.join('، ')}`);
    if (failed > 0) process.exit(1);
    console.log('تمام شد.');
})();
