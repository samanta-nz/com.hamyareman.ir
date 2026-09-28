#!/usr/bin/env node
/**
 * دیپلوی فانکشن study-tutor (لایه‌ی AI مطالعه — پرامپت ۰۴).
 *
 * کارها:
 *   ۱) بسته‌بندی backend/functions/study-tutor به tar.gz
 *   ۲) ساخت فانکشن اگر وجود نداشت / همگام‌سازی تنظیمات اگر بود
 *   ۳) آپلود دیپلوی جدید + فعال‌سازی
 *   ۴) انتظار تا status=ready (حداکثر ~۶ دقیقه)
 *
 * اجرا:        node deploy-prompt-04-study-tutor.js [--dry-run]
 * متغیرها:     APPWRITE_ENDPOINT / APPWRITE_PROJECT_ID / APPWRITE_API_KEY
 * Idempotent:  اجرای تکراری فقط یک دیپلوی تازه می‌سازد؛ فانکشن را دوباره نمی‌سازد.
 *
 * نکته‌ی کلید: APPWRITE_API_KEY باید دسترسی functions.write داشته باشد.
 */
const sdk = require('node-appwrite');
const { InputFile } = require('node-appwrite/file');
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

const dryRun = process.argv.includes('--dry-run');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID || '';
const API_KEY = process.env.APPWRITE_API_KEY || '';

// هدف دیپلوی: پیش‌فرض study-tutor (فانکشن مستقل)؛ با STUDY_FUNCTION_ID=ai-companion
// روی فانکشن موجود ai-companion دیپلوی می‌شود (پلن رایگان Appwrite سقف functions دارد).
const FUNCTION_ID = process.env.STUDY_FUNCTION_ID || 'study-tutor';
const FUNCTION_DIR = path.join(__dirname, '..', 'functions', FUNCTION_ID);

/** تنظیمات مرجع — هم‌قرارداد با backend/appwrite.json */
const SETTINGS = {
    name: 'study-tutor',
    runtime: 'node-20.0',
    execute: ['users'],
    events: [],
    schedule: '',
    timeout: 30,
    enabled: true,
    logging: true,
    entrypoint: 'src/main.js',
    commands: 'npm install',
};

function packCode() {
    const tmp = path.join(os.tmpdir(), `study-tutor-${Date.now()}.tar.gz`);
    execSync(`tar -czf "${tmp}" -C "${FUNCTION_DIR}" .`);
    const size = fs.statSync(tmp).size;
    console.log(`📦 بسته ساخته شد: ${tmp} (${(size / 1024).toFixed(1)} KB)`);
    return tmp;
}

function assertInputs() {
    if (!PROJECT_ID || !API_KEY) {
        console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
        process.exit(1);
    }
}

async function ensureFunction(functions) {
    let existing = null;
    try {
        existing = await functions.get({ functionId: FUNCTION_ID });
        console.log('· فانکشن از قبل وجود دارد — همگام‌سازی تنظیمات …');
    } catch (_) {
        console.log('➕ فانکشن وجود ندارد — ساخته می‌شود …');
    }

    if (dryRun) {
        console.log(`[dry] ${existing ? 'update' : 'create'} function ${FUNCTION_ID}`, JSON.stringify(SETTINGS));
        return;
    }

    if (existing) {
        await functions.update({ functionId: FUNCTION_ID, ...SETTINGS });
    } else {
        try {
            await functions.create({ functionId: FUNCTION_ID, ...SETTINGS });
        } catch (e) {
            const msg = String(e && e.message || e);
            if (msg.includes('scope') || msg.includes('unauthorized') || msg.includes('permission')) {
                console.error('❌ کلید API دسترسی functions.write ندارد. در کنسول Appwrite › Overview › Integrations، به کلیدِ مربوطه اسکوپ functions.write اضافه کنید.');
                throw e;
            }
            throw e;
        }
    }
    console.log(`✅ تنظیمات فانکشن ${FUNCTION_ID} همگام شد (runtime=${SETTINGS.runtime}, entrypoint=${SETTINGS.entrypoint})`);
}

async function deploy(functions) {
    const codePath = packCode();
    if (dryRun) {
        console.log(`[dry] createDeployment { functionId: ${FUNCTION_ID}, activate: true }`);
        return;
    }
    const dep = await functions.createDeployment({
        functionId: FUNCTION_ID,
        code: InputFile.fromPath(codePath, 'code.tar.gz'),
        activate: true,
    });
    console.log(`⬆ دیپلوی ${dep.$id} آپلود شد — انتظار برای build …`);

    const deadline = Date.now() + 6 * 60 * 1000;
    while (Date.now() < deadline) {
        await new Promise(r => setTimeout(r, 10000));
        const d = await functions.getDeployment({ functionId: FUNCTION_ID, deploymentId: dep.$id });
        process.stdout.write(`  … status=${d.status}\n`);
        if (d.status === 'ready') {
            console.log(`✅ دیپلوی فعال شد (${d.size ? (d.size / 1024).toFixed(0) + ' KB' : 'ok'}).`);
            return;
        }
        if (d.status === 'failed') {
            console.error('❌ build فانکشن شکست خورد. لاگ کامل: کنسول Appwrite › Functions › study-tutor › Deployments');
            console.error(JSON.stringify({ id: d.$id, status: d.status, buildTime: d.buildTime || '' }, null, 1));
            process.exit(2);
        }
    }
    console.error('⌛ بیش از ۶ دقیقه طول کشید؛ وضعیت را در کنسول چک کنید (build ممکن است هنوز در جریان باشد).');
    process.exit(3);
}

(async () => {
    console.log('دیپلوی پرامپت ۰۴: فانکشن study-tutor');
    console.log(`endpoint: ${ENDPOINT} | project: ${PROJECT_ID || '(dry)'}`);
    assertInputs();
    const client = new sdk.Client()
        .setEndpoint(ENDPOINT)
        .setProject(PROJECT_ID)
        .setKey(API_KEY);
    const functions = new sdk.Functions(client);
    await ensureFunction(functions);
    await deploy(functions);
    console.log('تمام شد.');
})();
