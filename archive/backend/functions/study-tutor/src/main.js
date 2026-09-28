/**
 * study-tutor — لایه‌ی AI «معلم خصوصی» برای سیستم مطالعه (پرامپت ۰۴).
 *
 * کِی صدا زده می‌شود؟ فقط وقتی زهرا در آزمون سوالی را غلط جواب داده و روی
 * «رفع اشکال با هوش مصنوعی» لمس کند. بقیه‌ی مسیر مطالعه (فلش‌کارت، تصحیح
 * آزمون، حل تشریحی) کاملاً آفلاین است و به این تابع هیچ نیازی ندارد.
 *
 * چرا تابع سرور و نه مستقیم از اپ؟ (الگوی ai-companion)
 *  ۱) کلید API هرگز در APK نیست.
 *  ۲) پرامپت معلم سمت سرور است و با به‌روزرسانی تابع عوض می‌شود.
 *
 * ورودی:
 *   {
 *     "lessonTitle": "درس ۱ — معرفی مجموعه",
 *     "sectionTitle": "عضویت: ∈ و ∉",      // اختیاری
 *     "question": "…",                      // سوال آزمون
 *     "correctAnswer": "…",                 // جواب درست (سمت اپ تصحیح قطعی شده)
 *     "studentAnswer": "…"                  // جواب زهرا
 *   }
 * خروجی:
 *   { ok: true, reply: "…", model: "…" }
 *   { ok: false, error: "not_configured"|"upstream_error"|…, fallback: "…" }
 *
 * حریم خصوصی: هیچ متنی ذخیره یا لاگ نمی‌شود — فقط طول ورودی/خروجی و وضعیت.
 * نمره و اشتباه‌ها همیشه سمت اپ (QuizGrader آفلاین) قطعی است؛ این تابع فقط
 * «توضیح مفهومی» می‌دهد و حق نمره‌دادن ندارد.
 */

const SYSTEM_PROMPT = [
  'تو «معلم خصوصی» یک نوجوان ایرانی پایه نهم هستی و فقط رفع اشکال مفهومی می‌کنی.',
  'قواعد سخت:',
  '۱) همیشه فارسی ساده، گرم و کوتاه بنویس؛ حداکثر ۵ جمله.',
  '۲) نقص مفهومی جواب دانش‌آموز را نسبت به جواب درست پیدا کن و همان را توضیح بده.',
  '۳) جواب درست را مستقیم لو نده؛ با یک اشاره‌ی کوچک به راه درست برسان و آخر یک جمله‌ی تشویق کوتاه بگو.',
  '۴) نمره نده و درباره‌ی چیزهای غیر از همین سوال حرف نزن.',
  '۵) اگر جواب دانش‌آموز تقریباً درست بود، همان نکته‌ی ظریف را بگو.',
  '۶) اگر نمی‌دانی یا سوال مبهم است، صادقانه بگو و به «توضیح آفلاین زیر همان سوال» ارجاع بده.',
].join('\n');

function parseBody(req) {
  try {
    if (req.bodyJson && typeof req.bodyJson === 'object') return req.bodyJson;
    if (typeof req.body === 'string') return JSON.parse(req.body || '{}');
    if (req.body && typeof req.body === 'object') return req.body;
  } catch (_) { /* بدنه‌ی خراب = خالی */ }
  return {};
}

function endpoint() {
  return (process.env.AI_ENDPOINT || 'https://api.openai.com/v1/chat/completions').trim();
}

function pickModel() {
  const one = (process.env.AI_MODEL || '').trim();
  if (one) return one;
  const list = (process.env.AI_MODELS || '').split(',').map(s => s.trim()).filter(Boolean);
  return list[0] || '';
}

function extractReply(payload) {
  if (!payload || typeof payload !== 'object') return '';
  const choice = Array.isArray(payload.choices) ? payload.choices[0] : null;
  if (choice) {
    const c = choice.message && typeof choice.message.content === 'string' ? choice.message.content : choice.text;
    if (typeof c === 'string') return c.trim();
  }
  if (typeof payload.reply === 'string') return payload.reply.trim();
  if (typeof payload.response === 'string') return payload.response.trim();
  return '';
}

module.exports = async function studyTutor(req, res) {
  const body = parseBody(req);
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
    return res.json({
      ok: false,
      error: 'not_configured',
      fallback: 'رفع اشکال با هوش مصنوعی هنوز روی سرور تنظیم نشده — توضیح آفلاین زیر همان سوال کمکت می‌کند.',
    });
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
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model,
        messages: [
          { role: 'system', content: SYSTEM_PROMPT },
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
      // متن خطای upstream برنمی‌گردد (ممکن است کلید یا اطلاعات داخلی داشته باشد).
      console.error('study-tutor upstream status', upstream.status);
      return res.json({ ok: false, error: 'upstream_error', status: upstream.status, fallback: 'الان به مدل نرسیدم؛ بعداً دوباره امتحان کن.' });
    }

    const payload = await upstream.json();
    const reply = extractReply(payload);
    if (!reply) {
      return res.json({ ok: false, error: 'empty_reply', fallback: 'جوابی از مدل نگرفتم؛ دوباره امتحان کن.' });
    }
    // فقط طول‌ها لاگ می‌شوند، نه محتوا.
    console.log('study-tutor ok', { model, inLen: userContent.length, outLen: reply.length });
    return res.json({ ok: true, reply, model });
  } catch (err) {
    console.error('study-tutor failed', err && err.name ? err.name : 'error');
    return res.json({ ok: false, error: 'network_error', fallback: 'اتصال به مدل برقرار نشد؛ بعداً دوباره امتحان کن.' });
  }
};
