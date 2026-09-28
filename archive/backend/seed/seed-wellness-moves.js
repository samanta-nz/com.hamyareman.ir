/**
 * وارد کردن ۴۳ حرکت سلامتی به جدول wellness_moves در Appwrite.
 *
 * الگوریتم:
 *  - اگر سطری با همان slug وجود داشته باشد، به‌روزرسانی می‌شود؛ در غیر این صورت ساخته می‌شود.
 *  - audioCueId کوتاه (slug-based) — v2: همه‌ی حرکات تک‌فایل تمام‌مدت هستند:
 *    فایل صوت = `cue-{audioCueId}.mp3` (الگوی قدیمی start|mid|end یوگا منسوخ شد).
 *  - referenceImageUrl از fileId کوتاه استفاده می‌کند.
 *
 * اجرا:  node backend/seed/seed-wellness-moves.js
 * خشک:  node backend/seed/seed-wellness-moves.js --dry-run
 */
const sdk = require('node-appwrite');

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID;
const API_KEY = process.env.APPWRITE_API_KEY;
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const TABLE_ID = 'wellness_moves';

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
}

const dryRun = process.argv.includes('--dry-run');
const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const tablesDb = new sdk.TablesDB(client);
const { Query } = sdk;

const BUCKET_ID = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b'; // Bucket ID واقعی (نه نام bucket)
const IMG_BASE = `${ENDPOINT}/storage/buckets/${BUCKET_ID}/files/`;
const IMG_VIEW = '/view?project=' + PROJECT_ID;
const img = (filename) => `${IMG_BASE}${filename}${IMG_VIEW}`;

// audioCueId: فقط یک رشته کوتاه (slug-like) — نام فایل در اپ ساخته می‌شود
// 43 حرکت: 15 yoga + 15 exercise + 8 breathing + 5 learning
const moves = [
    // --- ۱۵ یوگا (v2: تک‌فایل تمام‌مدت مربی‌محور) ---
    { slug: 'yoga-balasana', category: 'yoga', titleFa: 'کودک (Balasana)', level: 1, durationSec: 60, audioCueId: 'balasana', referenceImageUrl: img('w01_balasana_child_pose'), orderIndex: 1, tags: '["آرامش‌بخش","کشش کمر"]' },
    { slug: 'yoga-cat-cow', category: 'yoga', titleFa: 'گربه-گاو', level: 2, durationSec: 60, audioCueId: 'catcow', referenceImageUrl: img('w02_cat_cow'), orderIndex: 2, tags: '[]' },
    { slug: 'yoga-downward-dog', category: 'yoga', titleFa: 'سگ رو به پایین', level: 4, durationSec: 45, audioCueId: 'downdog', referenceImageUrl: img('w03_downward_dog'), orderIndex: 3, tags: '["کشش همسترینگ","تقویت شانه"]' },
    { slug: 'yoga-cobra', category: 'yoga', titleFa: 'کبرا', level: 3, durationSec: 30, audioCueId: 'cobra', referenceImageUrl: img('w04_cobra'), orderIndex: 4, tags: '["تقویت کمر"]' },
    { slug: 'yoga-warrior-1', category: 'yoga', titleFa: 'جنگجو ۱', level: 4, durationSec: 30, audioCueId: 'warrior1', referenceImageUrl: img('w05_warrior_1'), orderIndex: 5, tags: '[]' },
    { slug: 'yoga-warrior-2', category: 'yoga', titleFa: 'جنگجو ۲', level: 4, durationSec: 30, audioCueId: 'warrior2', referenceImageUrl: img('w06_warrior_2'), orderIndex: 6, tags: '[]' },
    { slug: 'yoga-tree', category: 'yoga', titleFa: 'درخت', level: 5, durationSec: 30, audioCueId: 'tree', referenceImageUrl: img('w07_tree_pose'), orderIndex: 7, tags: '["تعادل"]' },
    { slug: 'yoga-bridge', category: 'yoga', titleFa: 'پل', level: 3, durationSec: 30, audioCueId: 'bridge', referenceImageUrl: img('w08_bridge_pose'), orderIndex: 8, tags: '["تقویت باسن","کشش شکم"]' },
    { slug: 'yoga-spinal-twist', category: 'yoga', titleFa: 'پیچش ستون فقرات', level: 3, durationSec: 45, audioCueId: 'spinaltwist', referenceImageUrl: img('w09_seated_spinal_twist'), orderIndex: 9, tags: '[]' },
    { slug: 'yoga-wide-child', category: 'yoga', titleFa: 'کودک گسترده', level: 2, durationSec: 60, audioCueId: 'widechild', referenceImageUrl: img('w10_wide_legged_child'), orderIndex: 10, tags: '[]' },
    { slug: 'yoga-pigeon', category: 'yoga', titleFa: 'کبوتر', level: 5, durationSec: 60, audioCueId: 'pigeon', referenceImageUrl: img('w11_pigeon_pose'), orderIndex: 11, tags: '["بازکننده‌ی لگن"]' },
    { slug: 'yoga-triangle', category: 'yoga', titleFa: 'مثلث', level: 5, durationSec: 30, audioCueId: 'triangle', referenceImageUrl: img('w12_triangle_pose'), orderIndex: 12, tags: '[]' },
    { slug: 'yoga-savasana', category: 'yoga', titleFa: 'شاواسانا (جسد)', level: 1, durationSec: 180, audioCueId: 'savasana', referenceImageUrl: img('w13_savasana'), orderIndex: 13, tags: '["ریلکسیشن نهایی"]' },
    { slug: 'yoga-half-boat', category: 'yoga', titleFa: 'نیم‌قایق', level: 6, durationSec: 30, audioCueId: 'halfboat', referenceImageUrl: img('w14_half_boat'), orderIndex: 14, tags: '["تقویت شکم"]' },
    { slug: 'yoga-standing-forward-bend', category: 'yoga', titleFa: 'خم‌شدن ایستاده', level: 2, durationSec: 45, audioCueId: 'forwardbend', referenceImageUrl: img('w15_standing_forward_bend'), orderIndex: 15, tags: '["کشش همسترینگ"]' },

    // --- ۱۵ ورزش (تک‌فایل تمام‌مدت با شمارش صوتی تکرارها) ---
    { slug: 'ex-neck-4way', category: 'exercise', titleFa: 'کشش گردن ۴ جهت', level: 1, durationSec: 60, audioCueId: 'ex_neck', referenceImageUrl: img('w01_neck_stretch'), orderIndex: 1, tags: '[]' },
    { slug: 'ex-shoulder-cross', category: 'exercise', titleFa: 'کشش شانه ضربدری', level: 1, durationSec: 30, audioCueId: 'ex_shoulder', referenceImageUrl: img('w02_shoulder_stretch'), orderIndex: 2, tags: '[]' },
    { slug: 'ex-wrist-ankle', category: 'exercise', titleFa: 'چرخش مچ دست و پا', level: 1, durationSec: 60, audioCueId: 'ex_wrist_ankle', referenceImageUrl: img('w03_wrist_ankle_rotation'), orderIndex: 3, tags: '[]' },
    { slug: 'ex-squat', category: 'exercise', titleFa: 'اسکوات آرام', level: 4, durationSec: 60, reps: 10, audioCueId: 'ex_squat', referenceImageUrl: img('w04_bodyweight_squat'), orderIndex: 4, tags: '[]' },
    { slug: 'ex-lunge', category: 'exercise', titleFa: 'لانگز', level: 4, durationSec: 60, reps: 10, audioCueId: 'ex_lunge', referenceImageUrl: img('w05_lunges'), orderIndex: 5, tags: '[]' },
    { slug: 'ex-plank', category: 'exercise', titleFa: 'پلانک (روی آرنج)', level: 5, durationSec: 30, audioCueId: 'ex_plank', referenceImageUrl: img('w06_plank_beginner'), orderIndex: 6, tags: '[]' },
    { slug: 'ex-crunch', category: 'exercise', titleFa: 'کرانچ شکم ملایم', level: 4, durationSec: 60, reps: 10, audioCueId: 'ex_crunch', referenceImageUrl: img('w07_gentle_crunch'), orderIndex: 7, tags: '[]' },
    { slug: 'ex-hamstring', category: 'exercise', titleFa: 'کشش همسترینگ نشسته', level: 2, durationSec: 60, audioCueId: 'ex_hamstring', referenceImageUrl: img('w08_hamstring_stretch'), orderIndex: 8, tags: '[]' },
    { slug: 'ex-butterfly', category: 'exercise', titleFa: 'حرکت پروانه', level: 1, durationSec: 45, audioCueId: 'ex_butterfly', referenceImageUrl: img('w09_butterfly_stretch'), orderIndex: 9, tags: '[]' },
    { slug: 'ex-calf-raise', category: 'exercise', titleFa: 'بالا-پایین پاشنه', level: 3, durationSec: 45, reps: 15, audioCueId: 'ex_calf', referenceImageUrl: img('w10_calf_raises'), orderIndex: 10, tags: '[]' },
    { slug: 'ex-trunk-rotation', category: 'exercise', titleFa: 'چرخش تنه ایستاده', level: 2, durationSec: 45, audioCueId: 'ex_trunk', referenceImageUrl: img('w11_standing_trunk_rotation'), orderIndex: 11, tags: '[]' },
    { slug: 'ex-wall-calf', category: 'exercise', titleFa: 'کشش ساق پا به دیوار', level: 2, durationSec: 45, audioCueId: 'ex_wall_calf', referenceImageUrl: img('w12_wall_calf_stretch'), orderIndex: 12, tags: '[]' },
    { slug: 'ex-superman', category: 'exercise', titleFa: 'سوپرمن (تقویت کمر)', level: 5, durationSec: 45, reps: 8, audioCueId: 'ex_superman', referenceImageUrl: img('w13_superman'), orderIndex: 13, tags: '[]' },
    { slug: 'ex-jumping-jack', category: 'exercise', titleFa: 'جامپینگ جک ملایم', level: 4, durationSec: 60, reps: 15, audioCueId: 'ex_jj', referenceImageUrl: img('w14_jumping_jacks'), orderIndex: 14, tags: '["گرم‌کردن"]' },
    { slug: 'ex-wrist-forearm', category: 'exercise', titleFa: 'کشش مچ و ساعد', level: 1, durationSec: 45, audioCueId: 'ex_wrist_forearm', referenceImageUrl: img('w15_wrist_forearm_stretch'), orderIndex: 15, tags: '[]' },

    // --- ۸ تنفس (تک‌فایل با شمارش صوتی سیکل‌ها + انیمیشن BREATH_ANIM) ---
    { slug: 'breath-diaphragm', category: 'breathing', titleFa: 'تنفس شکمی', level: 1, durationSec: 120, audioCueId: 'breath_diaphragm', referenceImageUrl: img('w01_diaphragmatic'), orderIndex: 1, tags: '[]' },
    { slug: 'breath-4-7-8', category: 'breathing', titleFa: 'تنفس ۴-۷-۸', level: 2, durationSec: 120, audioCueId: 'breath_478', referenceImageUrl: img('w02_4_7_8_breath'), orderIndex: 2, tags: '["خواب"]' },
    { slug: 'breath-box', category: 'breathing', titleFa: 'تنفس جعبه‌ای', level: 2, durationSec: 180, audioCueId: 'breath_box', referenceImageUrl: img('w03_box_breathing'), orderIndex: 3, tags: '["تمرکز"]' },
    { slug: 'breath-nadi', category: 'breathing', titleFa: 'تنفس بینی متناوب', level: 4, durationSec: 180, audioCueId: 'breath_nadi', referenceImageUrl: img('w04_nadi_shodhana'), orderIndex: 4, tags: '[]' },
    { slug: 'breath-sigh', category: 'breathing', titleFa: 'آه‌کشیدن آرام‌بخش', level: 1, durationSec: 90, audioCueId: 'breath_sigh', referenceImageUrl: img('w05_sighing_breath'), orderIndex: 5, tags: '[]' },
    { slug: 'breath-lion', category: 'breathing', titleFa: 'تنفس شیر', level: 5, durationSec: 60, audioCueId: 'breath_lion', referenceImageUrl: img('w06_lions_breath'), orderIndex: 6, tags: '["رهاسازی تنش"]' },
    { slug: 'breath-counting', category: 'breathing', titleFa: 'تنفس شمارشی قبل امتحان', level: 2, durationSec: 120, audioCueId: 'breath_counting', referenceImageUrl: img('w07_counting_breath'), orderIndex: 7, tags: '["تمرکز","امتحان"]' },
    { slug: 'breath-bedtime', category: 'breathing', titleFa: 'آرام‌سازی قبل خواب', level: 1, durationSec: 300, audioCueId: 'breath_bedtime', referenceImageUrl: img('w08_bedtime_breath'), orderIndex: 8, tags: '["خواب"]' },

    // --- ۵ یادگیری (کوئه شروع ~۵۰ث؛ اجرای تکنیک با تایمر اپ — durationSec = مدت تایمر، نه طول صوت) ---
    { slug: 'learn-pomodoro', category: 'learning', titleFa: 'تکنیک پومودورو (۲۵/۵)', level: 1, durationSec: 1500, audioCueId: 'learn_pomodoro', referenceImageUrl: img('w01_pomodoro'), orderIndex: 1, tags: '[]' },
    { slug: 'learn-spaced-repetition', category: 'learning', titleFa: 'تکنیک تکرار فاصله‌دار', level: 2, durationSec: 600, audioCueId: 'learn_spaced', referenceImageUrl: img('w02_spaced_repetition'), orderIndex: 2, tags: '[]' },
    { slug: 'learn-feynman', category: 'learning', titleFa: 'روش فاینمن', level: 2, durationSec: 600, audioCueId: 'learn_feynman', referenceImageUrl: img('w03_feynman'), orderIndex: 3, tags: '[]' },
    { slug: 'learn-mind-map', category: 'learning', titleFa: 'نقشه‌ی ذهنی', level: 2, durationSec: 900, audioCueId: 'learn_mindmap', referenceImageUrl: img('w04_mind_map'), orderIndex: 4, tags: '[]' },
    { slug: 'learn-active-recall', category: 'learning', titleFa: 'فعال‌سازی حافظه', level: 2, durationSec: 600, audioCueId: 'learn_recall', referenceImageUrl: img('w05_active_recall'), orderIndex: 5, tags: '[]' },
];

// ثابت‌های انیمیشن برداری تنفس (v2) — کلاینت با slug می‌خواند (نیازی به ستون دیتابیس نیست).
// phases = [دم(پر), نگه(مکث), بازدم(خالی), مکث۲] بر حسب ثانیه؛ cycles = تعداد دور.
// استیت‌های UI: fill → hold → empty → rest؛ شمارش صوتی فایل با همین اعداد همگام است.
const BREATH_ANIM = {
    'breath-diaphragm': { shape: 'circle', phases: [4, 0, 6, 0], cycles: 10 },
    'breath-4-7-8':     { shape: 'circle', phases: [4, 7, 8, 0], cycles: 4 },
    'breath-box':       { shape: 'square', phases: [4, 4, 4, 4], cycles: 5 },
    'breath-nadi':      { shape: 'circle', phases: [4, 4, 4, 0], cycles: 5, nostrilMarker: true },
    'breath-sigh':      { shape: 'wave',   phases: [3, 0, 5, 0], cycles: 6 },
    'breath-lion':      { shape: 'burst',  phases: [3, 0, 2, 0], cycles: 5 },
    'breath-counting':  { shape: 'circle', phases: [4, 4, 4, 0], cycles: 8, centerCounter: true },
    'breath-bedtime':   { shape: 'circle', phases: [4, 0, 6, 0], cycles: 10, dim: true },
};
module.exports.BREATH_ANIM = BREATH_ANIM;

async function rowExists(slug) {
    try {
        const res = await tablesDb.listRows({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            queries: [Query.equal('slug', slug)],
        });
        if (res.rows && res.rows.length > 0) {
            return res.rows[0];
        }
        return null;
    } catch (e) {
        console.log(`  ⚠️ listRows(${slug}) خطا: ${e.message || e}`);
        return null;
    }
}

async function upsertMove(move) {
    const data = {
        slug: move.slug,
        category: move.category,
        titleFa: move.titleFa,
        level: move.level,
        durationSec: move.durationSec,
        reps: move.reps || 0,
        instructionsFa: move.instructionsFa || '',
        audioCueId: move.audioCueId,
        referenceImageUrl: move.referenceImageUrl,
        referenceImagePromptTemplate: move.referenceImagePromptTemplate || '',
        orderIndex: move.orderIndex,
        tags: move.tags || '[]',
    };
    const existing = await rowExists(move.slug);
    if (existing) {
        if (dryRun) return console.log(`  [dry] update ${move.slug}`);
        await tablesDb.updateRow({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            rowId: existing.$id,
            data,
        });
        console.log(`  ✅ ${move.slug} (updated, $id=${existing.$id})`);
    } else {
        if (dryRun) return console.log(`  [dry] create ${move.slug}`);
        // استفاده از slug به عنوان rowId برای predictability و جلوگیری از ID تکراری
        await tablesDb.createRow({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            rowId: move.slug.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 36),
            data,
        });
        console.log(`  ✅ ${move.slug} (created)`);
    }
}

async function main() {
    console.log(`بارگذاری ${moves.length} حرکت به جدول ${TABLE_ID}`);
    let created = 0, updated = 0, failed = 0;
    for (const m of moves) {
        try {
            const before = await rowExists(m.slug);
            await upsertMove(m);
            if (before) updated++;
            else created++;
        } catch (e) {
            console.log(`  ❌ ${m.slug}: ${e.message || e}`);
            failed++;
        }
    }
    console.log(`\n${dryRun ? '🔍' : '✅'} خلاصه: ${created} ساخته، ${updated} به‌روز، ${failed} شکست`);
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
