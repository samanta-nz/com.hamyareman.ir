/**
 * آپلود assets پرامپت ۰۳ (صوت جلسات + تصاویر مرجع) به باکت wellness-media در Appwrite.
 *
 * الگو: upload-wellness-assets.js (همان قراردادها — fileId از مسیر نسبی، idempotent، best-effort)
 * ورودی: artifacts/prompt-03-push/guided-audio (فایل‌های mp3)
 *        + artifacts/prompt-03-push/guided-references (فایل‌های jpg و png)
 *
 * اجرا:  node backend/seed/upload-guided-assets.js
 * خشک:  node backend/seed/upload-guided-assets.js --dry-run
 */
const sdk = require('node-appwrite');
const { InputFile } = require('node-appwrite/file');
const fs = require('fs');
const path = require('path');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const BUCKET_ID = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b'; // همان باکت wellness-media
const SOURCE_DIR = path.resolve(__dirname, '../../artifacts/prompt-03-push');

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
        } else if (entry.name.endsWith('.mp3') || entry.name.endsWith('.jpg') || entry.name.endsWith('.png')) {
            const rel = path.relative(SOURCE_DIR, full).replace(/\\/g, '/');
            results.push({ full, rel });
        }
    }
    return results;
}

// fileId یکتا و پایدار از مسیر نسبی: guided-audio/hypnosis/hyp-01-exam-calm/p1.mp3 → g-a-hypnosis-hyp-01-exam-calm-p1.mp3
function makeFileId(file) {
    const flat = file.rel.replace(/[\/\\]/g, '-').replace(/[^a-zA-Z0-9._-]/g, '_');
    return flat.length > 100 ? flat.slice(flat.length - 100) : flat;
}

async function fileExists(fileId) {
    try {
        await storage.getFile({ bucketId: BUCKET_ID, fileId });
        return true;
    } catch (e) {
        return false;
    }
}

async function uploadFile(file) {
    const fileId = makeFileId(file);
    if (await fileExists(fileId)) {
        console.log(`  · ${fileId} (exists)`);
        return { skipped: true };
    }
    if (dryRun) {
        console.log(`  [dry] ${fileId}`);
        return { dry: true };
    }
    await storage.createFile({
        bucketId: BUCKET_ID,
        fileId,
        file: InputFile.fromPath(file.full, fileId),
    });
    console.log(`  ✅ ${fileId}`);
    return { uploaded: true };
}

async function main() {
    const audioDir = path.join(SOURCE_DIR, 'guided-audio');
    const refDir = path.join(SOURCE_DIR, 'guided-references');

    const audioFiles = listAllFiles(audioDir);
    const refFiles = listAllFiles(refDir);
    const all = [...refFiles, ...audioFiles];

    console.log(`\n📁 ${refFiles.length} تصویر + ${audioFiles.length} فایل صوتی = ${all.length} فایل`);

    if (all.length === 0) {
        console.log('⚠️ فایلی برای آپلود پیدا نشد. اسکریپت‌ها/فایل‌های صوتی را در artifacts/prompt-03-push قرار بده.');
        return;
    }

    // بررسی تکراری نبودن fileId (الگوی prompt-02)
    const seenIds = new Map();
    const duplicates = [];
    for (const file of all) {
        const id = makeFileId(file);
        if (seenIds.has(id)) duplicates.push({ id, files: [seenIds.get(id), file.rel] });
        else seenIds.set(id, file.rel);
    }
    if (duplicates.length > 0) {
        console.log(`\n⚠️  ${duplicates.length} fileId تکراری شناسایی شد:`);
        for (const d of duplicates.slice(0, 5)) console.log(`   - ${d.id}: ${d.files.join(' | ')}`);
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
    if (failed > 0) process.exit(2);
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
