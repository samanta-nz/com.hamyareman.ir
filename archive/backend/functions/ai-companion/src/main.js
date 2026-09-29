/**
 * ai-companion — لایه‌ی AI «همراه زهرا».
 *
 * چرا یک تابع سرور و نه صدازدن مستقیم از اپ؟
 *  ۱) **کلید API هرگز در APK نیست** (مهم‌ترین دلیل).
 *  ۲) پرامپت سیستمی و قواعد ایمنی سمت سرور است و با به‌روزرسانی تابع عوض می‌شود،
 *     بدون انتشار نسخه‌ی تازه‌ی اپ.
 *  ۳) «تصمیم بحران» این‌جا هم گرفته می‌شود: اگر پیام نشانه‌ی آسیب به خود داشت،
 *     **اصلاً به مدل فرستاده نمی‌شود** و پاسخ ایمن + شماره‌های کمک برمی‌گردد.
 *
 * ورودی:
 *   { "message": "امروز خیلی خسته‌ام", "history": [{"role":"user"|"assistant","content":"..."}], "tone": "warm"|"formal" }
 * خروجی:
 *   { ok: true, reply: "...", model: "...", crisis: false }
 *   { ok: false, error: "not_configured"|"upstream_error", fallback: "..." }
 *
 * متغیرهای محیطی (در کنسول Appwrite › Functions › ai-companion › Settings › Variables):
 *   AI_API_KEY   — کلید ارائه‌دهنده (اجباری). هیچ‌وقت در ریپو commit نشود.
 *   AI_ENDPOINT  — آدرس کامل chat/completions. پیش‌فرض: https://api.openai.com/v1/chat/completions
 *                  (برای ارائه‌دهنده‌های سازگار با OpenAI — مثل همین چهار مدل — فقط این را عوض کن.)
 *   AI_MODELS    — فهرست مدل‌ها با کاما؛ اولین مدل استفاده می‌شود.
 *                  مثال: qwen3.8-flash,glm-5.3-flash,mimo-v2.5,hy3
 *   AI_MODEL     — اگر باشد بر AI_MODELS اولویت دارد.
 *   AI_MAX_TOKENS / AI_TEMPERATURE — اختیاری (پیش‌فرض ۴۰۰ / ۰.۶).
 *
 * حریم خصوصی: متن پیام **ذخیره نمی‌شود** و لاگ هم نمی‌شود (فقط طول پیام و وضعیت).
 * تاریخچه‌ی چت فقط روی دستگاه زهراست (`chat_history` در فهرست never-sync).
 */

const CRISIS_KEYWORDS = [
  'خودکشی', 'خودکشی کردن', 'می‌خوام بمیرم', 'میخوام بمیرم', 'کاش نبودم', 'کاش بمیرم',
  'آسیب به خود', 'خودزنی', 'بریدن دست', 'تمامش کنم', 'دیگه نمی‌تونم ادامه بدم',
];

const HELPLINES = [
  { name: 'صدای مشاور (بهزیستی)', number: '1480' },
  { name: 'اورژانس اجتماعی', number: '123' },
  { name: 'مشاوره‌ی نوجوان', number: '1570' },
];

const CRISIS_REPLY =
  'ممنون که این را گفتی — گفتنش شجاعت می‌خواهد. من یک برنامه‌ام و درمانگر نیستم، ' +
  'پس لطفاً همین حالا با یک آدم واقعی حرف بزن: بابا، مامان، مشاور مدرسه یا یکی از این شماره‌ها: ' +
  '۱۴۸۰ (صدای مشاور)، ۱۲۳ (اورژانس اجتماعی)، ۱۵۷۰ (مشاوره‌ی نوجوان). ' +
  'اگر الان در خطر فوری هستی با ۱۱۵ تماس بگیر. تو تنها نیستی.';

const SYSTEM_PROMPT_WARM = [
  'تو «همراه زهرا» هستی: یک همراه مهربان و کوتاه‌حرف برای یک نوجوان ایرانی.',
  'قواعد سخت:',
  '۱) همیشه فارسی ساده و صمیمی بنویس؛ حداکثر ۴ جمله.',
  '۲) هیچ‌وقت تظاهر نکن انسان یا درمانگر هستی؛ اگر پرسیدند بگو یک برنامه‌ای.',
  '۳) هیچ توصیه‌ی پزشکی، دارویی، حقوقی یا مالی نده؛ اگر لازم بود بگو «با یک بزرگ‌تر قابل اعتماد یا مشاور حرف بزن».',
  '۴) درباره‌ی آسیب به خود، خودکشی، خشونت یا مواد: راهنمایی عملی نده؛ فقط همدلی کن و ارجاع بده به بزرگ‌تر قابل اعتماد و شماره‌های ۱۴۸۰، ۱۲۳، ۱۵۷۰.',
  '۵) قضاوت نکن، نصیحت طولانی نکن، و در پایان یک سؤال کوچک و قابل‌جواب بپرس.',
  '۶) اگر نمی‌دانی، بگو نمی‌دانم؛ حدس نزن و منبع نساز.',
].join('\n');

const SYSTEM_PROMPT_FORMAL =
  SYSTEM_PROMPT_WARM.replace('فارسی ساده و صمیمی', 'فارسی ساده و کمی رسمی‌تر');

function parseBody(req) {
  if (!req || !req.body) return {};
  if (typeof req.body === 'string') {
    try { return JSON.parse(req.body); } catch (e) { return {}; }
  }
  return req.body;
}

function isCrisis(text) {
  const normalized = String(text || '').replace(/[يی]/g, 'ی').replace(/ك/g, 'ک').toLowerCase();
  return CRISIS_KEYWORDS.some((k) => normalized.includes(k));
}

function pickModel() {
  if (process.env.AI_MODEL) return process.env.AI_MODEL.trim();
  const list = String(process.env.AI_MODELS || '')
    .split(',')
    .map((m) => m.trim())
    .filter(Boolean);
  return list[0] || '';
}

function endpoint() {
  return String(process.env.AI_ENDPOINT || 'https://api.openai.com/v1/chat/completions').trim();
}

function sanitizeHistory(history) {
  if (!Array.isArray(history)) return [];
  return history
    .filter((m) => m && (m.role === 'user' || m.role === 'assistant') && typeof m.content === 'string')
    .slice(-6)
    .map((m) => ({ role: m.role, content: m.content.slice(0, 2000) }));
}

/** پاسخ هم از فرمت OpenAI-سازگار و هم از فرمت Gemini خوانده می‌شود. */
function extractReply(payload) {
  if (!payload) return '';
  const openai = payload.choices && payload.choices[0];
  if (openai) {
    if (openai.message && typeof openai.message.content === 'string') return openai.message.content.trim();
    if (typeof openai.text === 'string') return openai.text.trim();
  }
  const gemini = payload.candidates && payload.candidates[0];
  if (gemini) {
    const parts = gemini.content && gemini.content.parts;
    if (Array.isArray(parts)) return parts.map((p) => p.text || '').join('').trim();
    if (typeof gemini.output === 'string') return gemini.output.trim();
  }
  if (typeof payload.reply === 'string') return payload.reply.trim();
  if (typeof payload.response === 'string') return payload.response.trim();
  return '';
}

/**
 * حالت «معلم خصوصی» مطالعه (پرامپت ۰۴) — همان تابع، کلید و مدل ai-companion.
 * ورودی: { mode:"study-tutor", lessonTitle, sectionTitle, question, correctAnswer, studentAnswer }
 * خروجی: { ok:true, reply, model } — متن ذخیره/لاگ نمی‌شود؛ فقط طول‌ها.
 * چرا این‌جا؟ پلن رایگان Appwrite سقف تعداد functions دارد؛ به‌جای فانکشن تازه، یک mode اضافه شد.
 */
const STUDY_SYSTEM_PROMPT = [
  'تو «معلم خصوصی» یک نوجوان ایرانی پایه نهم هستی و فقط رفع اشکال مفهومی می‌کنی.',
  'قواعد سخت:',
  '۱) همیشه فارسی ساده، گرم و کوتاه بنویس؛ حداکثر ۵ جمله.',
  '۲) نقص مفهومی جواب دانش‌آموز را نسبت به جواب درست پیدا کن و همان را توضیح بده.',
  '۳) جواب درست را مستقیم لو نده؛ با یک اشاره‌ی کوچک به راه درست برسان و آخر یک جمله‌ی تشویق کوتاه بگو.',
  '۴) نمره نده و درباره‌ی چیزهای غیر از همین سوال حرف نزن.',
  '۵) اگر جواب دانش‌آموز تقریباً درست بود، همان نکته‌ی ظریف را بگو.',
  '۶) اگر نمی‌دانی یا سوال مبهم است، صادقانه بگو و به «توضیح آفلاین زیر همان سوال» ارجاع بده.',
].join('\n');

async function studyTutorReply(body, res) {
  const question = String(body.question || '').trim().slice(0, 1500);
  const correct = String(body.correctAnswer || '').trim().slice(0, 500);
  const student = String(body.studentAnswer || '').trim().slice(0, 500);
  const lesson = String(body.lessonTitle || '').trim().slice(0, 200);
  const section = String(body.sectionTitle || '').trim().slice(0, 200);

  if (!question || !correct) {
    return res.json({ ok: false, error: 'empty_question', fallback: 'این سوال برای رفع اشکال کامل نیست.' });
  }

  const apiKey = process.env.AI_API_KEY;
  const model = pickModel();
  if (!apiKey || !model) {
    return res.json({ ok: false, error: 'not_configured', fallback: 'رفع اشکال با هوش مصنوعی هنوز تنظیم نشده — توضیح آفلاین زیر همان سوال کمکت می‌کند.' });
  }

  const userContent = [
    lesson ? `درس: ${lesson}` : '',
    section ? `بخش مرتبط: ${section}` : '',
    `سوال: ${question}`,
    `جواب درست: ${correct}`,
    `جواب دانش‌آموز: ${student || '(بی‌جواب)'}`,
  ].filter(Boolean).join('\n');

  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 15000);
    const upstream = await fetch(endpoint(), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${apiKey}` },
      body: JSON.stringify({
        model,
        messages: [
          { role: 'system', content: STUDY_SYSTEM_PROMPT },
          { role: 'user', content: userContent },
        ],
        temperature: Number(process.env.AI_TEMPERATURE || 0.4),
        max_tokens: Number(process.env.AI_MAX_TOKENS || 350),
        stream: false,
      }),
      signal: controller.signal,
    });
    clearTimeout(timer);
    if (!upstream.ok) {
      console.error('study-tutor upstream status', upstream.status);
      return res.json({ ok: false, error: 'upstream_error', status: upstream.status, fallback: 'الان به مدل نرسیدم؛ بعداً دوباره امتحان کن.' });
    }
    const reply = extractReply(await upstream.json());
    if (!reply) {
      return res.json({ ok: false, error: 'empty_reply', fallback: 'جوابی از مدل نگرفتم؛ دوباره امتحان کن.' });
    }
    console.log('study-tutor ok', { model, inLen: userContent.length, outLen: reply.length });
    return res.json({ ok: true, reply, model });
  } catch (err) {
    console.error('study-tutor failed', err && err.name ? err.name : 'error');
    return res.json({ ok: false, error: 'network_error', fallback: 'اتصال به مدل برقرار نشد؛ بعداً دوباره امتحان کن.' });
  }
}

/**
 * google-auth — ورود استاندارد گوگل روی اندروید (Credential Manager).
 * روی همین فانکشن سوار شد (mode=google-auth) چون پلن رایگان سقف functions دارد.
 *
 * ورودی: { "mode": "google-auth", "idToken": "...", "nonce": "..." }
 * کار: صحت idToken با tokeninfo گوگل (aud/nonce/email_verified) → پیدا/ساخت کاربر
 *      با شناسه‌ی قطعی از sub → ساخت سشن با کلید تابع.
 * خروجی: { ok: true, userId, secret } — اپ با account.createSession(userId, secret) وارد می‌شود.
 */
const GOOGLE_AUD = process.env.GOOGLE_WEB_CLIENT_ID ||
  '548109780863-84ub4jlu08a436mfi0hmn252kge7gv61.apps.googleusercontent.com';

async function googleAuthSession(body, res, logErr) {
  try {
    const idToken = String(body.idToken || '');
    const nonce = String(body.nonce || '');
    if (!idToken) {
      return res.json({ ok: false, error: 'idToken missing' });
    }

    const r = await fetch('https://oauth2.googleapis.com/tokeninfo?id_token=' + encodeURIComponent(idToken));
    const info = await r.json();
    if (!info || !info.sub) {
      logErr && logErr('google tokeninfo rejected the token');
      return res.json({ ok: false, error: 'توکن گوگل نامعتبر است.' });
    }
    if (info.aud !== GOOGLE_AUD) {
      logErr && logErr('google aud mismatch: ' + info.aud);
      return res.json({ ok: false, error: 'توکن برای این اپ صادر نشده است.' });
    }
    if (String(info.email_verified) !== 'true') {
      return res.json({ ok: false, error: 'ایمیل گوگل تأیید نشده است.' });
    }
    if (nonce && info.nonce && String(info.nonce) !== nonce) {
      return res.json({ ok: false, error: 'نشست ورود معتبر نیست (nonce).' });
    }

    const sdk = require('node-appwrite');
    const client = new sdk.Client()
      .setEndpoint(process.env.APPWRITE_FUNCTION_API_ENDPOINT)
      .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
      .setKey(process.env.APPWRITE_FUNCTION_API_KEY);
    const users = new sdk.Users(client);

    // شناسه‌ی قطعی از sub — دوباره ورود، همان کاربر.
    const crypto = require('crypto');
    const userId = 'g' + crypto.createHash('sha256').update('google:' + info.sub).digest('hex').slice(0, 34);
    try {
      await users.get(userId);
    } catch (e) {
      await users.create(userId, info.email, info.name || '');
    }

    const session = await users.createSession(userId);
    return res.json({ ok: true, userId: session.userId || userId, secret: session.secret });
  } catch (err) {
    logErr && logErr('google-auth failed: ' + (err && err.message ? err.message : err));
    return res.json({ ok: false, error: 'ورود ناموفق: ' + (err && err.message ? err.message : err) });
  }
}

// Appwrite 2.x: امضای context — همه‌چیز از یک آبجکت می‌آید ({req, res, log, error}).
module.exports = async function aiCompanion(ctx) {
  const { req, res, log: logInfo, error: logErr } = ctx;
  const body = parseBody(req);

  // قفل پایه/دستگاه + صف اشتراک/ادمین — روی همین فانکشن سوار است
  // چون پلن رایگان سقف functions دارد (user-bootstrap ساخته نمی‌شود).
  if (body.action || (body.grade && body.deviceId && !body.mode && !body.message && !body.idToken)) {
    return require('./ops')(req, res);
  }

  // مسیریابی ورود native گوگل (Credential Manager) — idToken → سشن.
  if (body.mode === 'google-auth') {
    return googleAuthSession(body, res, logErr);
  }

  // مسیریابی حالت مطالعه — قبل از منطق گفت‌وگو و ایمنی بحران (سوال درسی است).
  if (body.mode === 'study-tutor') {
    return studyTutorReply(body, res);
  }
  const message = String(body.message || '').trim().slice(0, 2000);
  const tone = body.tone === 'formal' ? 'formal' : 'warm';

  // ۱) ایمنی اول: پیام بحران اصلاً به مدل نمی‌رود.
  if (isCrisis(message)) {
    return res.json({ ok: true, crisis: true, reply: CRISIS_REPLY, helplines: HELPLINES, model: 'safety-local' });
  }

  if (!message) {
    return res.json({ ok: false, error: 'empty_message', fallback: 'چیزی ننوشتی؛ هر وقت خواستی بنویس.' });
  }

  const apiKey = process.env.AI_API_KEY;
  const model = pickModel();
  if (!apiKey || !model) {
    // صادقانه: اپ این را می‌بیند و به قواعد محلی برمی‌گردد.
    return res.json({
      ok: false,
      error: 'not_configured',
      fallback: 'لایه‌ی AI روی سرور تنظیم نشده (AI_API_KEY یا AI_MODEL). فعلاً با قواعد محلی جواب می‌دهم.',
    });
  }

  const messages = [
    { role: 'system', content: tone === 'formal' ? SYSTEM_PROMPT_FORMAL : SYSTEM_PROMPT_WARM },
    ...sanitizeHistory(body.history),
    { role: 'user', content: message },
  ];

  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 12000);
    const upstream = await fetch(endpoint(), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model,
        messages,
        temperature: Number(process.env.AI_TEMPERATURE || 0.6),
        max_tokens: Number(process.env.AI_MAX_TOKENS || 400),
        stream: false,
      }),
      signal: controller.signal,
    });
    clearTimeout(timer);

    if (!upstream.ok) {
      // متن خطای upstream را برنمی‌گردانیم (ممکن است کلید یا اطلاعات داخلی داشته باشد).
      logErr('ai-companion upstream status', upstream.status);
      return res.json({ ok: false, error: 'upstream_error', status: upstream.status, fallback: 'الان به مدل نرسیدم. چند دقیقه دیگر دوباره امتحان کن.' });
    }

    const payload = await upstream.json();
    const reply = extractReply(payload);
    if (!reply) {
      return res.json({ ok: false, error: 'empty_reply', fallback: 'جوابی از مدل نگرفتم؛ دوباره امتحان کن.' });
    }
    // فقط طول پیام لاگ می‌شود، نه محتوایش.
    logInfo('ai-companion ok', { model, inLen: message.length, outLen: reply.length });
    return res.json({ ok: true, crisis: false, reply, model });
  } catch (err) {
    logErr('ai-companion failed', err && err.name ? err.name : 'error');
    return res.json({ ok: false, error: 'network_error', fallback: 'اتصال به مدل برقرار نشد. اینترنت را چک کن یا بعداً دوباره بیا.' });
  }
};
