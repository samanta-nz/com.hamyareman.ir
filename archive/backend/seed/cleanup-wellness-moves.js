/**
 * پاکسازی سطرهای تکراری wellness_moves.
 *
 * اگر قبلاً چند بار seed اجرا شده، ممکن است سطرهای تکراری وجود داشته باشد
 * که مانع update می‌شود.
 *
 * این اسکریپت همه‌ی سطرها را با همان slug پیدا می‌کند و فقط اولی را نگه می‌دارد.
 *
 * اجرا: node backend/seed/cleanup-wellness-moves.js
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

const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);
const tablesDb = new sdk.TablesDB(client);
const { Query } = sdk;

async function main() {
    console.log(`🧹 پاکسازی سطرهای تکراری در ${TABLE_ID}`);
    let offset = 0;
    const LIMIT = 100;
    let allRows = [];
    while (true) {
        const res = await tablesDb.listRows({
            databaseId: DATABASE_ID,
            tableId: TABLE_ID,
            queries: [Query.limit(LIMIT), Query.offset(offset)],
        });
        allRows = allRows.concat(res.rows);
        if (res.rows.length < LIMIT) break;
        offset += LIMIT;
    }
    console.log(`📊 ${allRows.length} سطر یافت شد`);

    // گروه‌بندی بر اساس slug
    const bySlug = new Map();
    for (const row of allRows) {
        const slug = row.slug || row.$id;
        if (!bySlug.has(slug)) bySlug.set(slug, []);
        bySlug.get(slug).push(row);
    }

    let deleted = 0;
    for (const [slug, rows] of bySlug.entries()) {
        if (rows.length > 1) {
            // اولی را نگه می‌داریم، بقیه را حذف می‌کنیم
            for (let i = 1; i < rows.length; i++) {
                try {
                    await tablesDb.deleteRow({
                        databaseId: DATABASE_ID,
                        tableId: TABLE_ID,
                        rowId: rows[i].$id,
                    });
                    console.log(`  🗑️ ${slug} ($id=${rows[i].$id}) حذف شد`);
                    deleted++;
                } catch (e) {
                    console.log(`  ❌ ${slug} ($id=${rows[i].$id}): ${e.message || e}`);
                }
            }
        }
    }

    console.log(`\n✅ خلاصه: ${deleted} سطر تکراری حذف شد`);
    console.log(`📊 ${bySlug.size} slug منحصر بفرد باقی ماند`);
}

main().catch((e) => { console.error('❌', e); process.exit(1); });
