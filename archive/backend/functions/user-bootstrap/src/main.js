/**
 * user-bootstrap — قفل پایه/دستگاه + صف اشتراک/ادمین.
 *
 * بدون فیلد action → رفتار قبلی (grade + deviceId).
 * با action → عملیات billing/admin. کلید سرور فقط این‌جاست، نه در APK.
 *
 * جدول لازم در دیتابیس (ZahraDB یا APPWRITE_DATABASE_ID):
 *   subscription_orders
 * ستون‌ها (string مگر ذکر شود):
 *   userId, email, status, planId, createdAtMs (integer), payload (string JSON)
 *
 * Label ادمین روی کاربر: `admin`
 * یا متغیر محیطی ADMIN_EMAILS=a@x.com,b@y.com
 */
const sdk = require('node-appwrite');

const DATABASE_ID = process.env.APPWRITE_DATABASE_ID || 'ZahraDB';
const PROFILES = 'profiles';
const SETTINGS = 'user_settings';
const STUDENT_PROFILES = 'student_profiles';
const ORDERS = 'subscription_orders';
const MAX_DEVICES = 2;
const REFUND_DAYS = 7;
const REFUND_MS = REFUND_DAYS * 24 * 60 * 60 * 1000;

const GRADE_FA = {
  grade4: 'چهارم', grade5: 'پنجم', grade6: 'ششم', grade7: 'هفتم',
  grade8: 'هشتم', grade9: 'نهم', grade10: 'دهم', grade11: 'یازدهم', grade12: 'دوازدهم',
};

const PLANS = {
  monthly: { id: 'monthly', titleFa: 'ماهانه', amountToman: 49000 },
  yearly: { id: 'yearly', titleFa: 'سالانه', amountToman: 390000 },
};

const FAREWELL =
  'اشتراک برایت غیرفعال شد و مبلغ در مسیر بازگشت است. مسیر درس خواندن گاهی پیچ می‌خورد؛ ' +
  'این پایان داستان تو نیست. هر وقت آماده بودی، همیار با آغوش باز منتظر است. ' +
  'موفقیتت را از همین‌جا می‌بینیم — به امید دیدار.';

function adminClient() {
  const client = new sdk.Client()
    .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
    .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID);
  client.setKey(process.env.APPWRITE_FUNCTION_API_KEY);
  return client;
}

function tablesService(client) {
  const service = sdk.TablesDB ? new sdk.TablesDB(client) : new sdk.Databases(client);
  return {
    create: service.createRow
      ? (db, table, id, data, perms) => service.createRow(db, table, id, data, perms)
      : (db, table, id, data, perms) => service.createDocument(db, table, id, data, perms),
    update: service.updateRow
      ? (db, table, id, data) => service.updateRow(db, table, id, data)
      : (db, table, id, data) => service.updateDocument(db, table, id, data),
    get: service.getRow
      ? (db, table, id) => service.getRow(db, table, id)
      : (db, table, id) => service.getDocument(db, table, id),
    list: service.listRows
      ? (db, table, queries) => service.listRows(db, table, queries)
      : (db, table, queries) => service.listDocuments(db, table, queries),
  };
}

function userIdOf(req) {
  return (
    req.userId ||
    (req.headers && (req.headers['x-appwrite-user-id'] || req.headers['X-Appwrite-User-Id'])) ||
    ''
  );
}

function parseBody(req) {
  if (!req.body) return {};
  if (typeof req.body === 'string') {
    try { return JSON.parse(req.body); } catch (e) { return {}; }
  }
  return req.body;
}

function parseDevices(raw) {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.filter((d) => d && d.id);
  if (typeof raw === 'string') {
    try {
      const v = JSON.parse(raw);
      return Array.isArray(v) ? v.filter((d) => d && d.id) : [];
    } catch (e) {
      return [];
    }
  }
  return [];
}

function prefsOf(user) {
  const p = user && user.prefs ? user.prefs : {};
  if (typeof p === 'object' && p !== null) return { ...p };
  return {};
}

function isAdminUser(user) {
  const labels = Array.isArray(user.labels) ? user.labels : [];
  if (labels.some((l) => String(l).toLowerCase() === 'admin')) return true;
  const emails = String(process.env.ADMIN_EMAILS || '')
    .split(',')
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean);
  return emails.includes(String(user.email || '').toLowerCase());
}

function payloadOf(row) {
  if (!row) return {};
  const raw = row.payload;
  if (typeof raw === 'string' && raw.trim()) {
    try { return JSON.parse(raw); } catch (e) { return {}; }
  }
  if (raw && typeof raw === 'object') return raw;
  return {};
}

function flattenOrder(row) {
  if (!row) return null;
  const extra = payloadOf(row);
  const id = row.$id || row.id || extra.id || '';
  return {
    id,
    userId: row.userId || extra.userId || '',
    email: row.email || extra.email || '',
    status: row.status || extra.status || '',
    planId: row.planId || extra.planId || '',
    createdAtMs: Number(row.createdAtMs || extra.createdAtMs || 0),
    ...extra,
  };
}

function digits(s) {
  return String(s || '').replace(/[^\d]/g, '');
}

async function getProfile(tables, userId) {
  try {
    const row = await tables.get(DATABASE_ID, STUDENT_PROFILES, userId);
    const d = row && (row.data || row) ? (row.data || row) : {};
    return d;
  } catch (e) {
    return {};
  }
}

async function setSubscription(tables, userId, status) {
  try {
    await tables.update(DATABASE_ID, STUDENT_PROFILES, userId, { subscription: status });
    return true;
  } catch (e) {
    return false;
  }
}

async function listUserOrders(tables, userId) {
  const Query = sdk.Query;
  const queries = [];
  if (Query && Query.equal) queries.push(Query.equal('userId', userId));
  if (Query && Query.orderDesc) queries.push(Query.orderDesc('createdAtMs'));
  if (Query && Query.limit) queries.push(Query.limit(25));
  const res = await tables.list(DATABASE_ID, ORDERS, queries);
  const rows = (res && (res.rows || res.documents)) || [];
  return rows.map(flattenOrder).filter(Boolean);
}

async function listByStatus(tables, status) {
  const Query = sdk.Query;
  const queries = [];
  if (Query && Query.equal) queries.push(Query.equal('status', status));
  if (Query && Query.orderDesc) queries.push(Query.orderDesc('createdAtMs'));
  if (Query && Query.limit) queries.push(Query.limit(50));
  const res = await tables.list(DATABASE_ID, ORDERS, queries);
  const rows = (res && (res.rows || res.documents)) || [];
  return rows.map(flattenOrder).filter(Boolean);
}

module.exports = async function (req, res) {
  const userId = userIdOf(req);
  if (!userId) {
    return res.json({ ok: false, code: 'UNAUTHENTICATED', messageFa: 'نشست معتبر نیست.' }, 401);
  }

  const body = parseBody(req);
  const action = String(body.action || '').trim();
  if (action) {
    return handleOps(req, res, userId, body, action);
  }
  return handleBootstrap(req, res, userId, body);
};

async function handleBootstrap(req, res, userId, body) {
  const grade = String(body.grade || '').trim().toLowerCase();
  const deviceId = String(body.deviceId || '').trim();
  const deviceLabel = String(body.deviceLabel || 'دستگاه ناشناس').trim().slice(0, 80);

  if (!GRADE_FA[grade] || !deviceId) {
    return res.json({
      ok: false,
      code: 'BAD_INPUT',
      messageFa: 'شناسهٔ پایه یا دستگاه ناقص است.',
    }, 200);
  }

  const client = adminClient();
  const users = new sdk.Users(client);
  const tables = tablesService(client);
  const Permission = sdk.Permission;
  const Role = sdk.Role;

  try {
    const user = await users.get(userId);
    const prefs = prefsOf(user);
    const bound = String(prefs.hamyarGrade || '').trim().toLowerCase();

    if (bound && bound !== grade) {
      const boundFa = GRADE_FA[bound] || bound;
      const thisFa = GRADE_FA[grade] || grade;
      return res.json({
        ok: false,
        code: 'GRADE_MISMATCH',
        boundGrade: bound,
        messageFa:
          'این ایمیل برای پایهٔ ' + boundFa +
          ' ثبت شده و نمی‌تواند وارد همیار ' + thisFa + ' شود.',
      }, 200);
    }

    let devices = parseDevices(prefs.hamyarDevices);
    const now = Date.now();
    const existing = devices.find((d) => d.id === deviceId);
    if (existing) {
      existing.label = deviceLabel || existing.label;
      existing.lastAt = now;
    } else if (devices.length < MAX_DEVICES) {
      devices.push({
        id: deviceId,
        label: deviceLabel,
        firstAt: now,
        lastAt: now,
      });
    } else {
      const others = devices
        .map((d) => d.label || 'دستگاه')
        .filter(Boolean)
        .slice(0, 2);
      const listed = others.length ? others.join(' و ') : 'دو دستگاه دیگر';
      return res.json({
        ok: false,
        code: 'DEVICE_LIMIT',
        devices: devices.map((d) => ({ label: d.label || 'دستگاه' })),
        messageFa:
          'این حساب روی دو دستگاه دیگر فعال است (' + listed +
          '). ورود از دستگاه سوم مجاز نیست.',
      }, 200);
    }

    prefs.hamyarGrade = grade;
    prefs.hamyarDevices = JSON.stringify(devices);
    await users.updatePrefs(userId, prefs);

    const perms = [
      Permission.read(Role.user(userId)),
      Permission.update(Role.user(userId)),
    ];

    await tables.create(
      DATABASE_ID,
      PROFILES,
      `profile_${userId}`,
      { userId, app: 'zahra', grade, displayName: 'دانش‌آموز', createdAtMs: now },
      perms,
    ).catch(() => null);

    await tables.create(
      DATABASE_ID,
      SETTINGS,
      `settings_${userId}`,
      { userId, weeklyOptIn: false, quietStart: 22, quietEnd: 7, themeMode: 'system' },
      perms,
    ).catch(() => null);

    const prevLabels = Array.isArray(user.labels) ? user.labels : [];
    const labels = prevLabels
      .filter((l) => l && l !== 'father' && l !== 'guest' && !String(l).startsWith('grade'))
      .concat(['zahra', grade]);
    await users.updateLabels(userId, Array.from(new Set(labels)));

    return res.json({
      ok: true,
      userId,
      grade,
      devices: devices.map((d) => ({ id: d.id, label: d.label })),
      labels: Array.from(new Set(labels)),
    }, 200);
  } catch (err) {
    console.error('user-bootstrap failed', err && err.message ? err.message : err);
    return res.json({ ok: false, code: 'BOOTSTRAP_FAILED', messageFa: 'ثبت حساب روی سرور ناموفق بود.' }, 200);
  }
}

async function handleOps(req, res, userId, body, action) {
  const client = adminClient();
  const users = new sdk.Users(client);
  const tables = tablesService(client);

  try {
    const user = await users.get(userId);
    switch (action) {
      case 'billing_my':
        return res.json(await opMy(tables, userId, user), 200);
      case 'billing_create':
        return res.json(await opCreate(tables, userId, user, body), 200);
      case 'billing_refund':
        return res.json(await opRefundReq(tables, userId, user, body), 200);
      case 'admin_ping':
      case 'admin_list':
      case 'admin_get':
      case 'admin_approve':
      case 'admin_reject':
      case 'admin_refund_ok':
      case 'admin_search':
        if (!isAdminUser(user)) {
          return res.json({
            ok: false,
            code: 'NOT_ADMIN',
            messageFa: 'این حساب ادمین نیست. در کنسول Appwrite برچسب admin را روی کاربر بگذار.',
          }, 200);
        }
        if (action === 'admin_ping') {
          return res.json({ ok: true, admin: true, email: user.email || '', userId }, 200);
        }
        if (action === 'admin_list') return res.json(await opAdminList(tables, body), 200);
        if (action === 'admin_get') return res.json(await opAdminGet(tables, users, body), 200);
        if (action === 'admin_approve') return res.json(await opAdminApprove(tables, body), 200);
        if (action === 'admin_reject') return res.json(await opAdminReject(tables, body), 200);
        if (action === 'admin_refund_ok') return res.json(await opAdminRefund(tables, body), 200);
        if (action === 'admin_search') return res.json(await opAdminSearch(tables, users, body), 200);
        break;
      default:
        return res.json({ ok: false, code: 'BAD_ACTION', messageFa: 'عملیات ناشناخته است.' }, 200);
    }
  } catch (err) {
    const msg = err && err.message ? String(err.message) : String(err);
    console.error('ops failed', action, msg);
    const missing = /not found|could not be found|404/i.test(msg);
    return res.json({
      ok: false,
      code: missing ? 'TABLE_MISSING' : 'OPS_FAILED',
      messageFa: missing
        ? 'جدول سفارش اشتراک روی سرور ساخته نشده. در کنسول Appwrite جدول subscription_orders را بساز.'
        : 'عملیات سرور ناموفق بود. کمی بعد دوباره امتحان کن.',
    }, 200);
  }
}

async function opMy(tables, userId, user) {
  const profile = await getProfile(tables, userId);
  let orders = [];
  try {
    orders = await listUserOrders(tables, userId);
  } catch (e) {
    orders = [];
  }
  const latest = orders[0] || null;
  const subscription = String(profile.subscription || (latest && latest.profileSub) || 'free');
  return {
    ok: true,
    subscription,
    order: latest,
    profile: {
      userId,
      email: user.email || profile.email || '',
      firstName: profile.firstName || '',
      lastName: profile.lastName || '',
      grade: profile.grade || '',
      gender: profile.gender || '',
      phone: profile.phone || '',
    },
  };
}

async function opCreate(tables, userId, user, body) {
  const profile = await getProfile(tables, userId);
  const sub = String(profile.subscription || 'free').toLowerCase();
  if (sub === 'yearly' || sub === 'paid' || sub === 'refund_pending') {
    return { ok: false, code: 'ALREADY_PAID', messageFa: 'اشتراک فعال است؛ سفارش تازه لازم نیست.' };
  }
  if (sub === 'pending') {
    return { ok: false, code: 'ALREADY_PENDING', messageFa: 'یک سفارش در انتظار تأیید داری. تا نتیجه صبر کن.' };
  }
  const plan = PLANS[String(body.planId || 'yearly')] || PLANS.yearly;
  const payText = String(body.payText || '').trim().slice(0, 2000);
  const receiptFileId = String(body.receiptFileId || '').trim().slice(0, 80);
  const payerName = String(body.payerName || '').trim().slice(0, 80);
  if (!payText && !receiptFileId) {
    return { ok: false, code: 'NEED_PROOF', messageFa: 'فیش یا متن مشخصات واریز را بفرست.' };
  }
  if (!payerName || payerName.length < 3) {
    return { ok: false, code: 'NEED_PAYER', messageFa: 'نام صاحب حسابی که از آن واریز کردی را بنویس.' };
  }
  const now = Date.now();
  const orderId = sdk.ID.unique();
  const payload = {
    userId,
    email: user.email || profile.email || '',
    firstName: profile.firstName || '',
    lastName: profile.lastName || '',
    grade: profile.grade || '',
    gender: profile.gender || '',
    phone: profile.phone || '',
    schoolName: profile.schoolName || '',
    province: profile.province || '',
    city: profile.city || '',
    county: profile.county || '',
    age: profile.age || 0,
    birthDate: profile.birthDate || '',
    planId: plan.id,
    planTitle: plan.titleFa,
    amountToman: plan.amountToman,
    payText,
    receiptFileId,
    payerName,
    status: 'pending',
    createdAtMs: now,
  };
  await tables.create(
    DATABASE_ID,
    ORDERS,
    orderId,
    {
      userId,
      email: payload.email,
      status: 'pending',
      planId: plan.id,
      createdAtMs: now,
      payload: JSON.stringify(payload),
    },
    [],
  );
  await setSubscription(tables, userId, 'pending');
  return { ok: true, subscription: 'pending', order: { id: orderId, ...payload } };
}

async function opRefundReq(tables, userId, user, body) {
  const profile = await getProfile(tables, userId);
  const sub = String(profile.subscription || 'free').toLowerCase();
  if (sub !== 'yearly' && sub !== 'paid') {
    return { ok: false, code: 'NOT_PAID', messageFa: 'اشتراک فعال نداری که بشود استرداد کرد.' };
  }
  let orders = [];
  try { orders = await listUserOrders(tables, userId); } catch (e) { orders = []; }
  const paid = orders.find((o) => o.status === 'approved' || o.status === 'yearly' || o.paidAtMs);
  if (!paid) {
    return { ok: false, code: 'NO_ORDER', messageFa: 'سفارش تأییدشده‌ای برای این حساب پیدا نشد.' };
  }
  const paidAt = Number(paid.paidAtMs || 0);
  if (!paidAt || Date.now() - paidAt > REFUND_MS) {
    return {
      ok: false,
      code: 'WINDOW_CLOSED',
      messageFa: 'پنجرهٔ بازگشت وجه هفت‌روزه تمام شده است.',
    };
  }
  const shaba = digits(body.refundShaba).slice(0, 26);
  const card = digits(body.refundCard).slice(0, 16);
  const refundAccountName = String(body.refundAccountName || '').trim().slice(0, 80);
  const reason = String(body.refundReason || '').trim().slice(0, 500);
  if (shaba.length < 24) {
    return { ok: false, code: 'BAD_SHABA', messageFa: 'شماره شبا را کامل بنویس (۲۴ رقم، با یا بدون IR).' };
  }
  if (card.length !== 16) {
    return { ok: false, code: 'BAD_CARD', messageFa: 'شماره کارت ۱۶ رقمی را بنویس.' };
  }
  if (!refundAccountName || refundAccountName.length < 3) {
    return { ok: false, code: 'NEED_ACCOUNT', messageFa: 'نام صاحب همان حسابی که خرید با آن انجام شد را بنویس.' };
  }
  const now = Date.now();
  const extra = {
    ...paid,
    status: 'refund_pending',
    refundShaba: shaba,
    refundCard: card,
    refundAccountName,
    refundReason: reason,
    refundAtMs: now,
  };
  await tables.update(DATABASE_ID, ORDERS, paid.id, {
    status: 'refund_pending',
    payload: JSON.stringify(extra),
  });
  await setSubscription(tables, userId, 'refund_pending');
  return { ok: true, subscription: 'refund_pending', order: extra };
}

async function opAdminList(tables, body) {
  const queue = String(body.queue || 'pay').toLowerCase();
  const status = queue === 'refund' ? 'refund_pending' : 'pending';
  const orders = await listByStatus(tables, status);
  return { ok: true, queue, orders };
}

async function opAdminGet(tables, users, body) {
  const orderId = String(body.orderId || '').trim();
  if (!orderId) return { ok: false, code: 'BAD_INPUT', messageFa: 'شناسه سفارش خالی است.' };
  const row = await tables.get(DATABASE_ID, ORDERS, orderId);
  const order = flattenOrder(row);
  const uid = order.userId;
  const profile = await getProfile(tables, uid);
  let account = {};
  try {
    const u = await users.get(uid);
    account = { email: u.email || '', name: u.name || '', labels: u.labels || [] };
  } catch (e) {
    account = {};
  }
  const avatarFileId = uid ? 'avt-' + String(uid).replace(/[^a-zA-Z0-9]/g, '').slice(0, 20) : '';
  return {
    ok: true,
    order,
    profile: { userId: uid, ...profile, ...account },
    avatarFileId,
  };
}

async function opAdminApprove(tables, body) {
  const orderId = String(body.orderId || '').trim();
  if (!orderId) return { ok: false, code: 'BAD_INPUT', messageFa: 'شناسه سفارش خالی است.' };
  const row = await tables.get(DATABASE_ID, ORDERS, orderId);
  const order = flattenOrder(row);
  if (order.status !== 'pending') {
    return { ok: false, code: 'BAD_STATE', messageFa: 'این سفارش در صف پرداخت نیست.' };
  }
  const now = Date.now();
  const extra = { ...order, status: 'approved', paidAtMs: now, decidedAtMs: now };
  await tables.update(DATABASE_ID, ORDERS, orderId, {
    status: 'approved',
    payload: JSON.stringify(extra),
  });
  await setSubscription(tables, order.userId, 'yearly');
  return { ok: true, subscription: 'yearly', order: extra };
}

async function opAdminReject(tables, body) {
  const orderId = String(body.orderId || '').trim();
  if (!orderId) return { ok: false, code: 'BAD_INPUT', messageFa: 'شناسه سفارش خالی است.' };
  const row = await tables.get(DATABASE_ID, ORDERS, orderId);
  const order = flattenOrder(row);
  const now = Date.now();
  const note = String(body.adminNote || '').trim().slice(0, 400);
  const extra = { ...order, status: 'rejected', decidedAtMs: now, adminNote: note };
  await tables.update(DATABASE_ID, ORDERS, orderId, {
    status: 'rejected',
    payload: JSON.stringify(extra),
  });
  await setSubscription(tables, order.userId, 'free');
  return { ok: true, subscription: 'free', order: extra };
}

async function opAdminRefund(tables, body) {
  const orderId = String(body.orderId || '').trim();
  if (!orderId) return { ok: false, code: 'BAD_INPUT', messageFa: 'شناسه سفارش خالی است.' };
  const row = await tables.get(DATABASE_ID, ORDERS, orderId);
  const order = flattenOrder(row);
  if (order.status !== 'refund_pending') {
    return { ok: false, code: 'BAD_STATE', messageFa: 'این سفارش در صف استرداد نیست.' };
  }
  const now = Date.now();
  const extra = { ...order, status: 'refunded', refundDoneAtMs: now, farewell: FAREWELL };
  await tables.update(DATABASE_ID, ORDERS, orderId, {
    status: 'refunded',
    payload: JSON.stringify(extra),
  });
  await setSubscription(tables, order.userId, 'free');
  return { ok: true, subscription: 'free', order: extra };
}

async function opAdminSearch(tables, users, body) {
  const q = String(body.q || '').trim();
  if (q.length < 3) {
    return { ok: false, code: 'BAD_INPUT', messageFa: 'حداقل سه نویسه برای جستجو بنویس.' };
  }
  const hits = [];
  const Query = sdk.Query;
  try {
    const ures = await users.list(
      Query && Query.limit ? [Query.limit(20), Query.search ? Query.search('email', q) : Query.equal('email', q)] : [],
    );
    const list = (ures && ures.users) || [];
    for (const u of list) {
      const profile = await getProfile(tables, u.$id);
      hits.push({
        userId: u.$id,
        email: u.email || '',
        name: u.name || '',
        labels: u.labels || [],
        profile,
      });
    }
  } catch (e) {
    try {
      const u = await users.get(q);
      const profile = await getProfile(tables, u.$id);
      hits.push({ userId: u.$id, email: u.email || '', name: u.name || '', labels: u.labels || [], profile });
    } catch (e2) { /* ignore */ }
  }
  try {
    const profile = await getProfile(tables, q);
    if (profile && (profile.email || profile.firstName) && !hits.some((h) => h.userId === q)) {
      hits.push({ userId: q, email: profile.email || '', name: (profile.firstName || '') + ' ' + (profile.lastName || ''), labels: [], profile });
    }
  } catch (e) { /* ignore */ }
  return { ok: true, hits };
}
