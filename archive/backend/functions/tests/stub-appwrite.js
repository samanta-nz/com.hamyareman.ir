/**
 * SDK ساختگی Appwrite — فقط برای تست منطق توابع، بدون شبکه.
 *
 * چرا؟ محیط توسعه به سرور Appwrite دسترسی نداشت؛ با این stub همان منطق‌ها
 * (دسترسی‌ها، محدودیت نرخ، opt-in، deterministic id، خطاهای ۴۰۳/۴۰۴/۴۰۹) روی
 * داده‌ی واقعی `backend/seed/content.json` اجرا و بررسی می‌شوند.
 *
 * جدول‌های درون حافظه در `global.__AW_STORE__` نگه داشته می‌شوند تا تست‌ها
 * بتوانند fixture بگذارند و نتیجه را ببینند.
 */
const store = global.__AW_STORE__ = global.__AW_STORE__ || {};
class Client {
  setEndpoint() { return this; } setProject() { return this; } setKey() { return this; }
}
const Query = {
  equal: (a, v) => ({ op: 'equal', a, v }),
  greaterThanEqual: (a, v) => ({ op: 'gte', a, v }),
  limit: (n) => ({ op: 'limit', n }),
};
function matches(row, q) {
  const data = row.data || row;
  return q.every((c) => {
    if (c.op === 'limit') return true;
    const val = data[c.a];
    if (c.op === 'equal') return String(val) === String(c.v);
    if (c.op === 'gte') return Number(val) >= Number(c.v);
    return true;
  });
}
class TablesDB {
  constructor(client) { this.client = client; }
  async createRow(db, table, id, data, perms) {
    store[table] = store[table] || [];
    if (store[table].some((r) => r.$id === id)) { const e = new Error('already exists'); e.code = 409; throw e; }
    const row = { $id: id, data: Object.assign({}, data), $permissions: perms || [] };
    store[table].push(row);
    return row;
  }
  async listRows(db, table, queries = []) {
    if (!store[table]) { const e = new Error('table not found'); e.code = 404; throw e; }
    const rows = store[table];
    const limit = (queries.find((q) => q.op === 'limit') || {}).n || 25;
    return { total: rows.length, rows: rows.filter((r) => matches(r, queries)).slice(0, limit) };
  }
  async updateRow(db, table, id, data) {
    const row = (store[table] || []).find((r) => r.$id === id);
    if (!row) { const e = new Error('not found'); e.code = 404; throw e; }
    row.data = Object.assign({}, row.data, data);
    return row;
  }
  async getRow(db, table, id) {
    const row = (store[table] || []).find((r) => r.$id === id);
    if (!row) { const e = new Error('not found'); e.code = 404; throw e; }
    return row;
  }
}
const ID = { unique: () => Math.random().toString(36).slice(2, 10) };
const Role = { user: (id) => `user:${id}`, any: () => 'any' };
const Permission = {
  read: (r) => `read("${r}")`, update: (r) => `update("${r}")`, delete: (r) => `delete("${r}")`,
};
module.exports = { Client, TablesDB, Query, ID, Role, Permission };
