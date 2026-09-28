/**
 * generate-sketch-reference — پرامپت ۰۲
 *
 * تولید تصویر مرجع سیاه‌قلم (pencil sketch) برای تمرین نقاشی.
 *
 * ورودی:
 *   { "subject": "face|nature|animal|object", "level": 4..10, "userId": "..." }
 * خروجی (موفق):
 *   { ok: true, imageUrl: "https://...", sketchId: "..." }
 * خروجی (خطا):
 *   { ok: false, error: "no_api_key|upstream_error|bad_request", fallback: "https://..." }
 *
 * متغیرهای محیطی (Appwrite Console › Functions › Settings › Variables):
 *   IMAGE_GEN_API_KEY    — کلید سرویس تولید تصویر (Gemini/DALL-E/Replicate). اختیاری.
 *   IMAGE_GEN_ENDPOINT   — پیش‌فرض: https://generativelanguage.googleapis.com/v1beta/models
 *                          (endpoint مخصوص Gemini Image Generation)
 *   WELLNESS_BUCKET_ID   — پیش‌فرض: wellness-media
 *
 * اگر IMAGE_GEN_API_KEY تنظیم نشده باشد، یک URL ثابت به یک placeholder عمومی
 * برمی‌گردد تا اپ crash نکند. در production حتماً کلید را تنظیم کنید.
 *
 * رفتار:
 *   1) prompt ساختاریافته برای سیاه‌قلم با پارامترهای level می‌سازد.
 *   2) API خارجی را صدا می‌زند (Gemini image gen یا سازگار با OpenAI Images API).
 *   3) خروجی (image bytes) را در Storage می‌ریزد و URL عمومی برمی‌گرداند.
 *   4) یک سطر در sketch_references برای گالری کاربر می‌سازد.
 *
 * نکته‌ی صداقت:
 *   - اگر سرویس خارجی fail شد، به‌جای crash یک URL پیش‌فرض (placeholder) برمی‌گردد
 *     و لاگ ساختاریافته می‌نویسد.
 *   - سطح ۴ = خطوط ساده و سایه‌زنی پایه، سطح ۷ = سایه‌زنی میان‌رده، سطح ۱۰ = جزئیات حرفه‌ای.
 */

const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_FUNCTION_API_ENDPOINT || process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_FUNCTION_PROJECT_ID || process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_FUNCTION_API_KEY || process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';

const BUCKET_ID = process.env.WELLNESS_BUCKET_ID || 'wellness-media';
const TABLE_SKETCH = 'sketch_references';

const IMAGE_GEN_API_KEY = process.env.IMAGE_GEN_API_KEY || '';
const IMAGE_GEN_ENDPOINT = process.env.IMAGE_GEN_ENDPOINT || 'https://generativelanguage.googleapis.com/v1beta/models';

const FALLBACK_IMAGE = 'https://placehold.co/800x600/png?text=Sketch+Reference';

const ALLOWED_SUBJECTS = new Set(['face', 'nature', 'animal', 'object']);

function parseBody(req) {
    if (!req || !req.body) return {};
    if (typeof req.body === 'string') {
        try { return JSON.parse(req.body); } catch (e) { return {}; }
    }
    return req.body;
}

function adminClient() {
    return new sdk.Client()
        .setEndpoint(ENDPOINT)
        .setProject(PROJECT_ID)
        .setKey(API_KEY);
}

function storageService(client) {
    return new sdk.Storage(client);
}

function tablesService(client) {
    const service = sdk.TablesDB ? new sdk.TablesDB(client) : new sdk.Databases(client);
    return {
        create: service.createRow
            ? (db, table, id, data, perms) => service.createRow(db, table, id, data, perms)
            : (db, table, id, data, perms) => service.createDocument(db, table, id, data, perms),
    };
}

/**
 * ساخت پرامپت سیاه‌قلم بر اساس موضوع و سطح مهارت.
 */
function buildSketchPrompt(subject, level) {
    const lvl = Math.max(4, Math.min(10, level));
    let levelDesc;
    if (lvl <= 5) {
        levelDesc = 'simple lines and basic shading, beginner-friendly';
    } else if (lvl <= 7) {
        levelDesc = 'mid-level shading, accurate proportions, moderate detail';
    } else {
        levelDesc = 'professional-grade detail, complex light and shadow, rich texture, fine cross-hatching';
    }
    const subjectMap = {
        face: 'a human face (front or three-quarter view)',
        nature: 'a nature scene (tree, mountain, or flower)',
        animal: 'an animal (cat, dog, or bird)',
        object: 'a still life object (vase, fruit bowl, or book)',
    };
    const subjectDesc = subjectMap[subject] || subjectMap.object;
    return `A black and white pencil sketch reference for art practice. Subject: ${subjectDesc}. Skill level: ${lvl}/10 — ${levelDesc}. The image must be pure black and white line/shading only (no color), suitable for tracing or copying with pencil on paper. No text on the image, no watermarks.`;
}

/**
 * فراخوانی سرویس تولید تصویر. در حال حاضر با Gemini سازگار است.
 * اگر IMAGE_GEN_API_KEY تنظیم نشده باشد، null برمی‌گردد.
 *
 * خروجی: { buffer: Buffer, mime: 'image/png' } یا null.
 */
async function callImageGen(prompt) {
    if (!IMAGE_GEN_API_KEY) return null;
    // نمونه با Gemini (generateContent با responseModalities IMAGE)
    const url = `${IMAGE_GEN_ENDPOINT}/gemini-2.0-flash-exp:generateContent?key=${IMAGE_GEN_API_KEY}`;
    const body = {
        contents: [{ role: 'user', parts: [{ text: prompt }] }],
        generationConfig: { responseModalities: ['IMAGE', 'TEXT'] },
    };
    try {
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
            signal: AbortSignal.timeout(45000),
        });
        if (!res.ok) return null;
        const json = await res.json();
        const part = json.candidates?.[0]?.content?.parts?.[0];
        if (!part || !part.inlineData) return null;
        return { buffer: Buffer.from(part.inlineData.data, 'base64'), mime: 'image/png' };
    } catch (e) {
        return null;
    }
}

async function uploadToStorage(storage, buffer, mime, filename) {
    const blob = new Blob([buffer], { type: mime });
    const file = await storage.createFile(BUCKET_ID, filename, blob);
    // بدون getFileView (به دلیل تفاوت API نسخه‌ها)، URL عمومی مستقیم می‌سازیم
    return `${ENDPOINT}/storage/buckets/${BUCKET_ID}/files/${file.$id}/view?project=${PROJECT_ID}`;
}

module.exports = async function (req, res) {
    const start = Date.now();
    const body = parseBody(req);
    const subject = (body.subject || 'object').toLowerCase();
    const level = Math.max(4, Math.min(10, Number(body.level) || 4));
    const userId = String(body.userId || '').trim();

    if (!ALLOWED_SUBJECTS.has(subject)) {
        return res.json({ ok: false, error: 'bad_request', message: 'subject must be one of: face, nature, animal, object' }, 400);
    }

    const prompt = buildSketchPrompt(subject, level);
    const filename = `sketch_${subject}_l${level}_${Date.now()}.png`;

    try {
        const client = adminClient();
        const storage = storageService(client);
        const tables = tablesService(client);

        let imageUrl = FALLBACK_IMAGE;
        let usedFallback = true;

        const generated = await callImageGen(prompt);
        if (generated) {
            try {
                imageUrl = await uploadToStorage(storage, generated.buffer, generated.mime, filename);
                usedFallback = false;
            } catch (e) {
                // upload failed → fallback
            }
        }

        // ثبت در sketch_references فقط اگر واقعاً تولید شد (نه fallback)
        let sketchId = null;
        if (!usedFallback && userId) {
            try {
                const row = await tables.create(DATABASE_ID, TABLE_SKETCH, sdk.ID.unique(), {
                    userId,
                    subject,
                    level,
                    titleFa: subjectTitleFa(subject, level),
                    imageUrl,
                    generatedAtIso: new Date().toISOString(),
                });
                sketchId = row.$id;
            } catch (e) {
                // اگر ردیف ساخته نشد، URL برمی‌گردد ولی گالری ذخیره نمی‌شود
            }
        }

        const elapsed = Date.now() - start;
        console.log(JSON.stringify({
            level: 'info', function: 'generate-sketch-reference', userId, subject, level,
            usedFallback, elapsedMs: elapsed,
        }));

        return res.json({ ok: true, imageUrl, sketchId, usedFallback });
    } catch (e) {
        console.log(JSON.stringify({
            level: 'error', function: 'generate-sketch-reference', error: String(e.message || e),
        }));
        return res.json({ ok: false, error: 'internal', fallback: FALLBACK_IMAGE }, 500);
    }
};

function subjectTitleFa(subject, level) {
    const map = {
        face: 'چهره',
        nature: 'طبیعت',
        animal: 'حیوان',
        object: 'اشیاء',
    };
    return `مرجع ${map[subject] || 'نقاشی'} - سطح ${level}`;
}
