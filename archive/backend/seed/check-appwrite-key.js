#!/usr/bin/env node
/**
 * چک سلامت کلید Appwrite — با خود SDK (مسیرها همیشه با نسخه‌ی سرور هم‌خوان است).
 *
 * استفاده:
 *   node check-appwrite-key.js tables     → چک دسترسی TablesDB (databases.read)
 *   node check-appwrite-key.js functions  → چک دسترسی Functions (functions.read)
 *
 * خروجی سبز = ادامه؛ در غیر این صورت پیام راهنما + exit 1.
 * هیچ مقدار کلیدی چاپ نمی‌شود — فقط وضعیت و پیام خطای سرور (کد/نوع).
 */
const sdk = require('node-appwrite');

const which = (process.argv[2] || 'tables').toLowerCase();
const ENDPOINT = process.env.APPWRITE_ENDPOINT || 'https://fra.cloud.appwrite.io/v1';
const PROJECT_ID = process.env.APPWRITE_PROJECT_ID || '';
const API_KEY = (process.env.APPWRITE_API_KEY || '').trim();
const DB = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';

if (!PROJECT_ID || !API_KEY) {
    console.error('❌ APPWRITE_PROJECT_ID یا APPWRITE_API_KEY خالی است.');
    console.error('   گیت‌هاب › Settings › Secrets and variables › Actions را چک کن.');
    process.exit(1);
}

const client = new sdk.Client().setEndpoint(ENDPOINT).setProject(PROJECT_ID).setKey(API_KEY);

(async () => {
    try {
        if (which === 'functions') {
            const functions = new sdk.Functions(client);
            await functions.list();
            console.log('✅ کلید با اسکوپ functions معتبر است.');
        } else if (which === 'storage') {
            const BUCKET = process.env.APPWRITE_BUCKET_ID || '6aa1eaae00303400117b';
            const storage = new sdk.Storage(client);
            await storage.listFiles({ bucketId: BUCKET, queries: [sdk.Query.limit(1)] });
            console.log(`✅ کلید با باکت ${BUCKET} (wellness-media) کار می‌کند.`);
        } else {
            const tables = new sdk.TablesDB(client);
            await tables.listTables({ databaseId: DB });
            console.log(`✅ کلید با اسکوپ tables معتبر است (دیتابیس ${DB} در دسترس).`);
        }
        process.exit(0);
    } catch (e) {
        const code = e && (e.code || e.status);
        const type = e && e.type ? e.type : '';
        console.error(`❌ کلید کار نکرد (HTTP ${code || '?'} ${type}):`);
        console.error(`   ${(e && e.message) || e}`);
        console.error('');
        if (String(code) === '401') {
            console.error('   کلید نامعتبر یا بی‌اسکوپ است. در کنسول Appwrite › Overview ›');
            console.error('   Integrations › API Keys یک کلید با این اسکوپ‌ها بساز و مقدار secret');
            console.error('   APPWRITE_API_KEY را در گیت‌هاب با آن جایگزین کن:');
            console.error('     tables.write / documents.write / functions.write / storage.write');
        } else if (String(code) === '404') {
            console.error('   پروژه یا دیتابیس پیدا نشد. APPWRITE_PROJECT_ID و');
            console.error('   APPWRITE_DATABASE_ID (ZahraDB) را با کنسول بسنج.');
        } else {
            console.error('   اتصال/پیکربندی را با کنسول Appwrite بسنج.');
        }
        process.exit(1);
    }
})();
