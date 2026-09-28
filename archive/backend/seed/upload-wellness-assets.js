/**
 * آپلود assets پرامپت ۰۲ (تصاویر + صوت) به باکت wellness-media در Appwrite.
 * Bucket ID (استفاده‌شده در API): 6aa1eaae00303400117b
 * Bucket name (فقط نمایشی در کنسول): wellness-media
 *
 * ورودی: artifacts/prompt-02-push/wellness-references/ + wellness-audio/
 *
 * اجرا:  node backend/seed/upload-wellness-assets.js
 * خشک:  node backend/seed/upload-wellness-assets.js --dry-run
 */
const sdk = require('node-appwrite');
const { InputFile } = require('node-appwrite/file');
const fs = require('fs');
const path = require('path');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const BUCKET_ID = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b'; // Bucket ID واقعی (نه نام bucket) — Appwrite API به bucketId نیاز دارد
const SOURCE_DIR = path.resolve(__dirname, '../../artifacts/prompt-02-push');

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const storage = new sdk.Storage(client);

function listAllFiles(root) {
    const results = [];
    if (!fs.existsSync(root)) return results;
    for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
        const full = path.join(root, entry.name);
        if (entry.isDirectory()) {
            results.push(...listAllFiles(full));
        } else if (entry.name.endsWith('.jpg') || entry.name.endsWith('.png') || entry.name.endsWith('.mp3')) {
            // مسیر نسبی به SOURCE_DIR
            const rel = path.relative(SOURCE_DIR, full).replace(/\\/g, '/');
            results.push({ abs: full, rel });
        }
    }
    return results;
}

/**
 * ساختن fileId معتبر Appwrite از روی نام فایل.
 * - فقط کاراکترهای a-z A-Z 0-9 _
 * - حداکثر ۳۶ کاراکتر
 * - اگر تکراری بود، نوع (img/aud) اضافه می‌شود
 * - prefix "w_" برای تشخیص فایل‌های wellness در bucket اشتراکی "default"
 */
function makeFileId(file) {
    // نام بدون پسوند
    const ext = path.extname(file.rel); // .jpg / .png / .mp3
    const baseName = path.basename(file.rel, ext);

    // نوع فایل برای تشخیص تکراری نبودن
    const typePrefix = ext === '.mp3' ? 'aud' : 'img';

    // تمیز کردن: فقط a-z A-Z 0-9 _
    let cleaned = baseName.replace(/[^a-zA-Z0-9_]/g, '_');

    // اگر با _ شروع می‌شود (نادر)، با حرف شروع کن
    if (cleaned.startsWith('_')) {
        cleaned = 'w' + cleaned;
    }

    // prefix "w" برای تشخیص wellness files (مثلاً w01_balasana_child_pose)
    if (!cleaned.startsWith('w') && !cleaned.startsWith('cue') && !cleaned.startsWith('ref')) {
        cleaned = 'w' + cleaned;
    }

    // محدود کردن طول: 36 کاراکتر
    const MAX_LEN = 36;
    if (cleaned.length > MAX_LEN) {
        // hash کوتاه از full path
        const crypto = require('crypto');
        const hash = crypto.createHash('md5').update(file.rel).digest('hex').slice(0, 6);
        cleaned = cleaned.slice(0, MAX_LEN - 7) + '_' + hash;
    }

    return cleaned;
}

async function fileExistsInBucket(fileId) {
    try {
        await storage.getFile({ bucketId: BUCKET_ID, fileId });
        return true;
    } catch (e) {
        // با کد HTTP چک می‌کنیم، نه با متن پیام —
        // پیام واقعی Appwrite «The requested file could not be found.» است
        // که شامل زیررشته‌ی «not found» نیست («could not be found» است)
        // و همین باعث می‌شد این تابع همیشه throw کند و createFile اصلاً اجرا نشود.
        if (e.code === 404) return false;
        const msg = String(e.message || e);
        if (msg.includes('could not be found') || msg.includes('404')) return false;
        throw e;
    }
}

async function uploadFile(file) {
    const fileId = makeFileId(file);
    if (await fileExistsInBucket(fileId)) {
        console.log(`  · ${file.rel} (exists as ${fileId})`);
        return { skipped: true, fileId };
    }
    if (dryRun) {
        console.log(`  [dry] upload ${file.rel} as ${fileId}`);
        return { skipped: false, fileId };
    }
    // فایل باید به‌صورت InputFile به SDK داده شود، نه Buffer خام —
    // وگرنه Appwrite با خطای "The requested file could not be found" رد می‌کند.
    await storage.createFile({
        bucketId: BUCKET_ID,
        fileId,
        file: InputFile.fromPath(file.abs, path.basename(file.abs)),
        // permissions: ['read("any")']  // از permissions باکت به ارث می‌برد
    });
    console.log(`  ✅ ${file.rel} → ${fileId}`);
    return { skipped: false, fileId };
}

async function main() {
    console.log(`آپلود assets پرامپت ۰۲ از ${SOURCE_DIR}`);
    console.log(`به باکت: ${BUCKET_ID}`);
    console.log(`پروژه: ${PROJECT_ID}`);

    const refDir = path.join(SOURCE_DIR, 'wellness-references');
    const audioDir = path.join(SOURCE_DIR, 'wellness-audio');

    const refFiles = listAllFiles(refDir);
    const audioFiles = listAllFiles(audioDir);
    const all = [...refFiles, ...audioFiles];

    console.log(`\n📁 ${refFiles.length} تصویر + ${audioFiles.length} فایل صوتی = ${all.length} فایل`);

    // بررسی تکراری نبودن fileId
    const seenIds = new Map();
    const duplicates = [];
    for (const file of all) {
        const id = makeFileId(file);
        if (seenIds.has(id)) {
            duplicates.push({ id, files: [seenIds.get(id), file.rel] });
        } else {
            seenIds.set(id, file.rel);
        }
    }
    if (duplicates.length > 0) {
        console.log(`\n⚠️  ${duplicates.length} fileId تکراری شناسایی شد (ممکن است فایل overwrite شود):`);
        for (const d of duplicates.slice(0, 5)) {
            console.log(`   - ${d.id}: ${d.files.join(' | ')}`);
        }
        if (duplicates.length > 5) console.log(`   ... و ${duplicates.length - 5} مورد دیگر`);
    }

    let uploaded = 0, skipped = 0, failed = 0;
    for (const file of all) {
        try {
            const res = await uploadFile(file);
            if (res.skipped) skipped++;
            else if (!dryRun) uploaded++;
        } catch (e) {
            console.log(`  ❌ ${file.rel}: ${e.message || e}`);
            failed++;
        }
    }

    console.log(`\n${dryRun ? '🔍' : '✅'} خلاصه: ${uploaded} آپلود، ${skipped} رد شد، ${failed} شکست`);
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
