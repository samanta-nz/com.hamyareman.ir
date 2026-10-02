// تست رفتاری سه فایل موسیقی با jsdom.
//   node scripts/test_background_music.mjs            (پیش‌فرض: http://127.0.0.1:8000/)
// سرور ساده: python3 -m http.server 8000 --directory Bucket/Html-files
import { JSDOM, VirtualConsole } from "jsdom";

const BASE = process.env.MUSIC_BASE || "http://127.0.0.1:8000/";
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const results = [];
function check(name, cond, extra = "") {
  results.push([cond ? "PASS" : "FAIL", name, extra]);
  console.log(`${cond ? "✅" : "❌"} ${name}${extra ? " — " + extra : ""}`);
}

async function load(page, { appTheme = null, systemDark = false } = {}) {
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
      if (appTheme) {
        // شبیه‌سازی اپ: پلِ ظاهر پیش از اجرای HTML نصب می‌شود (مثل addJavascriptInterface)
        window.HamyarAppearanceBridge = { resolvedTheme: () => appTheme, themePreference: () => appTheme };
      }
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

// ---------- ۰) خودبسنده بودن ----------
{
  console.log("— خودبسندگی: هر سه فایل باید همهٔ صداها را embed داشته باشند —");
  for (const name of ["background-music.html", "background-music-tile.html", "background-music-full.html"]) {
    const text = await (await fetch(BASE + name)).text();
    const tracks = (text.match(/"audio":"/g) || []).length;
    const iframe = /<iframe[^>]+background-music/.test(text);
    check(`${name}: ${tracks} صدای embed شده، بدون وابستگی به فایل دیگر`, tracks === 29 && !iframe,
      `${(text.length / 1048576).toFixed(2)}MB`);
  }
}

// ---------- ۱) tile ----------
{
  console.log("\n— tile (کادر ۹۲px + پاپ‌آپ تمام‌صفحه + بسته‌شدن پس از ۲ ثانیه) —");
  const dom = await load("background-music-tile.html", { appTheme: "dark" });
  const w = dom.window, doc = w.document;
  await sleep(1500);
  check("tile: حالت کادر (embedded) فعال است", doc.body.classList.contains("embedded"));
  check("tile: گونهٔ tile است، نه full", doc.body.getAttribute("data-hamyar-music-variant") === "tile");
  check("tile: در شروع بسته است", !doc.body.classList.contains("is-open") && doc.getElementById("backdrop").hidden);
  check("tile: تم اپ (dark) نشست", doc.documentElement.getAttribute("data-theme") === "dark");
  check("tile: کلید ماه/خورشید محلی قفل شد", doc.documentElement.hasAttribute("data-hamyar-app-theme"));

  click(doc, "mini");
  await sleep(150);
  check("tile: با لمس، پاپ‌آپ باز شد", doc.body.classList.contains("is-open") && !doc.getElementById("backdrop").hidden);
  check("tile: پاپ‌آپ بدون سقف ارتفاع است (variant-full)", doc.body.classList.contains("variant-full"));

  await sleep(2400);
  check("tile: پس از ۲ ثانیه بی‌لمسی بسته شد", !doc.body.classList.contains("is-open"));
  check("tile: به حالت کادر برگشت", !doc.body.classList.contains("variant-full") && doc.getElementById("backdrop").hidden);

  click(doc, "mini");
  await sleep(100);
  for (let i = 0; i < 6; i++) {
    await sleep(400);
    doc.getElementById("grid").dispatchEvent(new w.MouseEvent("pointerdown", { bubbles: true }));
  }
  check("tile: لمس پیاپی تایمر را ری‌ست کرد (هنوز باز است)", doc.body.classList.contains("is-open"));
  await sleep(2400);
  check("tile: پس از توقف لمس‌ها بسته شد", !doc.body.classList.contains("is-open"));

  click(doc, "mini");
  await sleep(150);
  click(doc, "close");
  await sleep(150);
  check("tile: دکمهٔ ✕ هم می‌بندد و کادر برمی‌گردد",
    !doc.body.classList.contains("is-open") && doc.getElementById("backdrop").hidden);
  w.close();
}

// ---------- ۲) full ----------
{
  console.log("\n— full (همیشه باز، همهٔ منوها، بدون سقف ارتفاع) —");
  const dom = await load("background-music-full.html", { appTheme: "light" });
  const w = dom.window, doc = w.document;
  await sleep(1500);
  check("full: گونهٔ full فعال است", doc.body.classList.contains("variant-full") && doc.body.classList.contains("locked-open"));
  check("full: از ابتدا باز است", doc.getElementById("backdrop").hidden === false);
  w.BackgroundMusic.close();
  await sleep(200);
  check("full: close() هم آن را نمی‌بندد", doc.getElementById("backdrop").hidden === false);
  doc.getElementById("backdrop").dispatchEvent(new w.MouseEvent("click", { bubbles: true }));
  await sleep(200);
  check("full: لمس بیرونی هم آن را نمی‌بندد", doc.getElementById("backdrop").hidden === false);
  check("full: همهٔ منوها هست (دسته‌بندی + کاتالوگ + دو لایه + ولوم)",
    doc.getElementById("filters").children.length > 0 &&
    doc.getElementById("grid").children.length > 0 &&
    !!doc.getElementById("slot0") && !!doc.getElementById("slot1") && !!doc.getElementById("volume"));
  check("full: دکمهٔ بستن مخفی است", w.getComputedStyle(doc.getElementById("close")).display === "none");
  check("full: تم روشن اپ نشست", doc.documentElement.getAttribute("data-theme") === "light");
  w.HamyarAppearanceBridge = { resolvedTheme: () => "dark", themePreference: () => "dark" };
  doc.documentElement.setAttribute("data-hamyar-theme", "dark");
  await sleep(300);
  check("full: تغییر زندهٔ تم اپ اعمال شد", doc.documentElement.getAttribute("data-theme") === "dark");
  w.close();
}

// ---------- ۳) پلیر مستقل، بدون اپ ----------
{
  console.log("\n— player مستقل (بدون اپ) —");
  const dom = await load("background-music.html", { systemDark: true });
  const w = dom.window, doc = w.document;
  await sleep(1200);
  check("player: بدون اپ هم تم دارد", ["dark", "light"].includes(doc.documentElement.getAttribute("data-theme")),
    "data-theme=" + doc.documentElement.getAttribute("data-theme"));
  check("player: بدون اپ، کلید ماه/خورشید آزاد است", !doc.documentElement.hasAttribute("data-hamyar-app-theme"));
  check("player: پیش‌فرض گونهٔ کادر (نه full)", !doc.body.classList.contains("variant-full"));
  w.BackgroundMusic.setFull(true, true);
  await sleep(150);
  check("player: setFull(true,true) باز و بدون سقف ارتفاع می‌کند",
    doc.body.classList.contains("variant-full") && doc.getElementById("backdrop").hidden === false);
  w.close();
}

const failed = results.filter((r) => r[0] === "FAIL");
console.log(`\nخلاصه: ${results.length - failed.length}/${results.length} پاس`);
process.exit(failed.length ? 1 : 0);
