// تست رفتاری سه فایل کاملِ موسیقی (نسخهٔ 0000) با jsdom.
//   python3 -m http.server 8000 --directory Bucket/Html-files   # یا 0000
//   node scripts/test_background_music.mjs
import { JSDOM, VirtualConsole } from "jsdom";

const BASE = process.env.MUSIC_BASE || "http://127.0.0.1:8000/";
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const results = [];
function check(name, cond, extra = "") {
  results.push([cond ? "PASS" : "FAIL", name, extra]);
  console.log(`${cond ? "✅" : "❌"} ${name}${extra ? " — " + extra : ""}`);
}

async function load(page, { appTheme = null, systemDark = false, host = null } = {}) {
  const vc = new VirtualConsole();
  vc.on("jsdomError", (e) => {
    if (!/Not implemented|AudioContext|OfflineAudio/i.test(String(e.message))) {
      console.log("   [jsdomError]", String(e.message).slice(0, 160));
    }
  });
  const dom = await JSDOM.fromURL(BASE + page, {
    runScripts: "dangerously",
    resources: "usable",
    pretendToBeVisual: true,
    virtualConsole: vc,
    beforeParse(window) {
      window.matchMedia = (q) => ({
        matches: systemDark && /dark/.test(q),
        media: q,
        addEventListener() {}, removeEventListener() {}, addListener() {}, removeListener() {},
      });
      // پل ظاهر اپ دقیقاً مثل addJavascriptInterface پیش از لود صفحه نصب می‌شود
      if (appTheme) window.HamyarAppearanceBridge = { resolvedTheme: () => appTheme, themePreference: () => appTheme };
      if (host) window.HamyarHost = host;
    },
  });
  if (appTheme) {
    const root = dom.window.document.documentElement;
    root.setAttribute("data-hamyar-theme-preference", appTheme);
    root.setAttribute("data-hamyar-theme", appTheme);
  }
  await new Promise((r) => dom.window.addEventListener("load", r, { once: true }));
  return dom;
}

const click = (doc, id) =>
  doc.getElementById(id).dispatchEvent(new doc.defaultView.MouseEvent("click", { bubbles: true }));
const cssHas = (doc, needle) =>
  [...doc.querySelectorAll("style")].some((s) => s.textContent.includes(needle));

// ---------- ۰) خودبسندگی ----------
{
  console.log("— خودبسندگی: هر سه فایل همهٔ صداها را embed دارند —");
  for (const name of ["background-music.html", "background-music-tile.html", "background-music-full.html"]) {
    const text = await (await fetch(BASE + name)).text();
    const tracks = (text.match(/"audio":"/g) || []).length;
    const iframe = /<iframe[^>]+background-music/.test(text);
    check(`${name}: ${tracks} صدای embed، بدون وابستگی به فایل دیگر`, tracks === 29 && !iframe,
      `${(text.length / 1048576).toFixed(2)}MB`);
  }
}

// ---------- ۱) tile ----------
{
  console.log("\n— tile: کادر در جریان صفحه، باز شدن رو به پایین، بسته‌شدن پس از ۲ ثانیه —");
  let tileEvents = [];
  const dom = await load("background-music-tile.html", {
    appTheme: "dark",
    host: { getTheme: () => "dark", onMusicTile: (v) => tileEvents.push(v) },
  });
  const w = dom.window, doc = w.document;
  await sleep(1200);
  const backdrop = doc.getElementById("backdrop");
  check("tile: حالت tilemode فعال است", doc.body.classList.contains("tilemode"));
  check("tile: در شروع بسته است", !backdrop.classList.contains("open"));
  check("tile: تم اپ (dark) نشست", doc.documentElement.getAttribute("data-theme") === "dark");
  check("tile: کلید ماه/خورشید محلی قفل شد", doc.documentElement.hasAttribute("data-hamyar-app-theme"));
  check("tile: سقف ارتفاع آکاردئون با dvh است", cssHas(doc, "body.tilemode .backdrop.open{max-height:calc(100dvh - 92px)}"));

  click(doc, "mini");
  await sleep(150);
  check("tile: با لمس، رو به پایین باز شد", backdrop.classList.contains("open"));
  check("tile: میزبان اندرویدی خبردار شد", tileEvents[tileEvents.length - 1] === true);

  await sleep(2400);
  check("tile: پس از ۲ ثانیه بی‌لمسی بسته شد", !backdrop.classList.contains("open"));
  check("tile: بسته‌شدن هم به میزبان گزارش شد", tileEvents[tileEvents.length - 1] === false);

  click(doc, "mini");
  await sleep(100);
  for (let i = 0; i < 6; i++) {
    await sleep(400);
    doc.dispatchEvent(new w.MouseEvent("pointerdown", { bubbles: true }));
  }
  check("tile: لمس پیاپی تایمر را ری‌ست کرد", backdrop.classList.contains("open"));
  await sleep(2500);
  check("tile: پس از توقف لمس‌ها بسته شد", !backdrop.classList.contains("open"));
  w.close();
}

// ---------- ۲) full ----------
{
  console.log("\n— full: همیشه باز، همهٔ منوها، بدون سقف ارتفاع —");
  const dom = await load("background-music-full.html", { appTheme: "light" });
  const w = dom.window, doc = w.document;
  await sleep(1200);
  check("full: حالت full فعال است", doc.body.classList.contains("full"));
  check("full: از ابتدا باز است", doc.body.classList.contains("is-open") && doc.getElementById("backdrop").hidden === false);
  w.BackgroundMusic.close();
  await sleep(200);
  check("full: close() آن را نمی‌بندد", doc.body.classList.contains("is-open"));
  doc.getElementById("backdrop").dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  await sleep(200);
  check("full: لمس بیرونی هم نمی‌بندد", doc.body.classList.contains("is-open"));
  check("full: دکمهٔ بستن پنهان است", doc.getElementById("close").hidden === true);
  check("full: همهٔ منوها هست", doc.getElementById("filters").children.length > 0 &&
    doc.getElementById("grid").children.length > 0 &&
    !!doc.getElementById("slot0") && !!doc.getElementById("slot1") && !!doc.getElementById("volume"));
  check("full: قفلِ اسکرول برداشته شد (کل صفحه دیده می‌شود)",
    cssHas(doc, "body.full.is-open{overflow:auto!important;overflow-x:hidden}"));
  check("full: سقف ارتفاع شیت/سربرگ/کاتالوگ برداشته شد",
    cssHas(doc, "body.full .sheet,body.full .sheet-head,body.full .catalog{max-height:none!important;overflow:visible}"));
  check("full: سربرگ و پانوشت صفحه هست", !!doc.querySelector(".page-head") && !!doc.querySelector(".page-foot"));
  check("full: تم روشن اپ نشست", doc.documentElement.getAttribute("data-theme") === "light");
  w.HamyarAppearanceBridge = { resolvedTheme: () => "dark", themePreference: () => "dark" };
  doc.documentElement.setAttribute("data-hamyar-theme", "dark");
  await sleep(300);
  check("full: تغییر زندهٔ تم اپ اعمال شد", doc.documentElement.getAttribute("data-theme") === "dark");
  w.close();
}

// ---------- ۳) player در iframe ----------
{
  console.log("\n— player: کادر در iframe، باز شدن مودال و خبر دادن به میزبان —");
  const messages = [];
  const hostDom = new JSDOM(
    `<!doctype html><html><body><iframe id="f" src="${BASE}background-music.html#embedded"></iframe></body></html>`,
    { runScripts: "dangerously", resources: "usable", pretendToBeVisual: true, url: "http://127.0.0.1:8000/host.html" },
  );
  hostDom.window.addEventListener("message", (e) => { if (e.data && e.data.channel) messages.push(e.data); });
  await new Promise((r) => hostDom.window.addEventListener("load", r, { once: true }));
  await sleep(2500);
  const frame = hostDom.window.document.getElementById("f");
  const idoc = frame.contentDocument, iwin = frame.contentWindow;
  check("player: در iframe لود شد و حالت embedded دارد", idoc.body.classList.contains("embedded"));
  check("player: گونهٔ مودال است (نه tile/full)",
    !idoc.body.classList.contains("tilemode") && !idoc.body.classList.contains("full"));
  click(idoc, "mini");
  await sleep(300);
  check("player: با لمس باز شد", idoc.getElementById("backdrop").hidden === false);
  const openMsg = messages.filter((m) => m.type === "state").pop();
  check("player: وضعیت باز به میزبان پیام داده شد (برای پاپ‌آپ تمام‌صفحه)", openMsg && openMsg.opened === true,
    JSON.stringify(openMsg && { type: openMsg.type, opened: openMsg.opened }));
  iwin.BackgroundMusic.close();
  await sleep(300);
  const closeMsg = messages.filter((m) => m.type === "state").pop();
  check("player: وضعیت بسته هم پیام داده شد (بازگشت به جای قبلی)", closeMsg && closeMsg.opened === false);
  check("player: پس از بستن، کادر سر جای خودش است", idoc.getElementById("backdrop").hidden === true);
  hostDom.window.close();
}

// ---------- ۴) بدون اپ: تم سیستم ----------
{
  console.log("\n— بدون اپ: تم از سیستم می‌آید —");
  const dom = await load("background-music.html", { systemDark: true });
  const doc = dom.window.document;
  await sleep(800);
  check("بدون اپ: تم سیستم (dark) اعمال شد", doc.documentElement.getAttribute("data-theme") === "dark",
    "data-theme=" + doc.documentElement.getAttribute("data-theme"));
  check("بدون اپ: کلید ماه/خورشید آزاد است", !doc.documentElement.hasAttribute("data-hamyar-app-theme"));
  dom.window.close();
}

const failed = results.filter((r) => r[0] === "FAIL");
console.log(`\nخلاصه: ${results.length - failed.length}/${results.length} پاس`);
process.exit(failed.length ? 1 : 0);
