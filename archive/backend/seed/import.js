/**
 * واردکردن محتوای کاتالوگ (`backend/seed/content.json`) در Appwrite.
 *
 * پیش‌نیاز:
 *   npm install node-appwrite      (در پوشه‌ی backend/seed)
 *
 * متغیرهای محیطی:
 *   APPWRITE_PROJECT_ID   (اجباری)
 *   APPWRITE_API_KEY      (اجباری — کلیدی با دسترسی tables.write / documents.write)
 *   APPWRITE_ENDPOINT     (پیش‌فرض: https://fra.cloud.appwrite.io/v1)
 *   APPWRITE_DATABASE_ID  (پیش‌فرض: main_db)
 *
 * اجرا:
 *   node backend/seed/import.js                 # همه‌ی جدول‌ها
 *   node backend/seed/import.js --only=recipes,artPrompts
 *   node backend/seed/import.js --dry-run       # فقط چاپ می‌کند، چیزی نمی‌نویسد
 *
 * شناسه‌ی سطرها همان `id` داخل content.json است، پس اجرای دوباره سطر تکراری نمی‌سازد
 * (اول update، اگر نبود create).
 */
const fs = require('node:fs');
const path = require('node:path');
// SDK فقط وقتی لازم می‌شود که واقعاً بخواهیم بنویسیم (dry-run بدون نصب هم کار می‌کند).
let sdk = null;
function loadSdk() {
  if (!sdk) sdk = require('node-appwrite');
  return sdk;
}

const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID || '';
const API_KEY = process.env.APPWRITE_API_KEY || '';
const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'main_db';

const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const onlyArg = args.find((a) => a.startsWith('--only='));
const only = onlyArg ? onlyArg.split('=')[1].split(',').map((x) => x.trim()) : null;

const asJson = (value) => JSON.stringify(value || [], null, 0);

/** نگاشت content.json → ستون‌های جدول (دقیقاً مطابق backend/appwrite.json). */
const TABLES = {
  recipes: {
    table: 'recipes',
    rows: (items) => items.map((r) => ({
      id: r.id,
      data: {
        title: r.title,
        ingredients: asJson(r.ingredients),
        steps: asJson(r.steps),
        minutes: Number(r.minutes || 0),
        servings: Number(r.servings || 2),
        difficulty: String(r.difficulty || 'آسان'),
        tip: String(r.tip || ''),
      },
    })),
  },
  lessons: {
    table: 'lessons',
    rows: (items) => items.map((l) => ({
      id: l.id,
      data: {
        title: l.title,
        subject: String(l.subject || ''),
        grade: Number(l.grade || 0),
        body: String(l.body || ''),
      },
    })),
  },
  quizzes: {
    table: 'quizzes',
    rows: (items) => items.map((q) => ({
      id: q.id,
      data: {
        lessonId: String(q.lessonId || ''),
        question: q.question,
        choices: asJson(q.choices),
        answerIndex: Number(q.answerIndex || 0),
      },
    })),
  },
  learningNodes: {
    table: 'learning_nodes',
    rows: (items) => items.map((n) => ({
      id: n.id,
      data: {
        title: n.title,
        track: String(n.track || ''),
        orderIndex: Number(n.orderIndex || 0),
        prerequisiteId: String(n.prerequisiteId || ''),
      },
    })),
  },
  artPrompts: {
    table: 'art_prompts',
    rows: (items) => items.map((a) => ({
      id: a.id,
      data: {
        title: a.title,
        prompt: String(a.prompt || ''),
        moodTag: String(a.moodTag || ''),
      },
    })),
  },
  exercises: {
    table: 'exercises',
    rows: (items) => items.map((e) => ({
      id: e.id,
      data: {
        title: e.title,
        category: String(e.category || ''),
        minutes: Number(e.minutes || 0),
        steps: asJson(e.steps),
        note: String(e.note || ''),
      },
    })),
  },
};

function tablesService(client) {
  const { TablesDB, Databases } = loadSdk();
  const service = TablesDB ? new TablesDB(client) : new Databases(client);
  return {
    create: service.createRow
      ? (db, table, id, data, perms) => service.createRow(db, table, id, data, perms)
      : (db, table, id, data, perms) => service.createDocument(db, table, id, data, perms),
    update: service.updateRow
      ? (db, table, id, data) => service.updateRow(db, table, id, data)
      : (db, table, id, data) => service.updateDocument(db, table, id, data),
  };
}

async function main() {
  if (!dryRun && (!PROJECT_ID || !API_KEY)) {
    console.error('APPWRITE_PROJECT_ID و APPWRITE_API_KEY لازم است.');
    process.exit(1);
  }

  const file = path.join(__dirname, 'content.json');
  const content = JSON.parse(fs.readFileSync(file, 'utf8'));

  // در حالت dry-run اصلاً به SDK و شبکه کاری نداریم.
  let tables = null;
  let publicRead = [];
  if (!dryRun) {
    const { Client, Permission, Role } = loadSdk();
    const client = new Client()
      .setEndpoint(ENDPOINT)
      .setProject(PROJECT_ID)
      .setKey(API_KEY);
    tables = tablesService(client);
    publicRead = [Permission.read(Role.any())];
  }

  const keys = Object.keys(TABLES).filter((key) => !only || only.includes(key));
  let created = 0;
  let updated = 0;
  let failed = 0;

  for (const key of keys) {
    const spec = TABLES[key];
    const items = content[key];
    if (!Array.isArray(items)) {
      console.warn(`⚠️  ${key}: در content.json پیدا نشد، رد شد.`);
      continue;
    }
    const rows = spec.rows(items);
    console.log(`\n▸ ${spec.table} (${rows.length} سطر)`);

    for (const row of rows) {
      if (dryRun) {
        console.log(`   [dry-run] ${row.id}: ${row.data.title || row.data.question || ''}`);
        continue;
      }
      try {
        await tables.update(DATABASE_ID, spec.table, row.id, row.data);
        updated += 1;
        console.log(`   ↻ به‌روز شد: ${row.id}`);
      } catch (updateErr) {
        try {
          await tables.create(DATABASE_ID, spec.table, row.id, row.data, publicRead);
          created += 1;
          console.log(`   + ساخته شد: ${row.id}`);
        } catch (createErr) {
          failed += 1;
          console.error(`   ✗ ناموفق: ${row.id} — ${createErr.message || createErr}`);
        }
      }
    }
  }

  console.log(`\nنتیجه: ${created} ساخته شد، ${updated} به‌روز شد، ${failed} ناموفق${dryRun ? ' (dry-run)' : ''}.`);
  if (failed > 0) process.exit(2);
}

main().catch((err) => {
  console.error('خطای کلی:', err.message || err);
  process.exit(1);
});
