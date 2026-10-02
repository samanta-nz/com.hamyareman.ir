import { JSDOM, VirtualConsole } from "jsdom";

const BASE = "http://127.0.0.1:8000/";
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
  });
  const { window } = dom;
  window.matchMedia = (q) => ({
    matches: systemDark && /dark/.test(q),
    media: q,
    addEventListener() {},
    removeEventListener() {},
    addListener() {},
    removeListener() {},
  });
  if (appTheme) {
    // شبیه‌سازی اپ: همان کاری که publishHamyarAppearance می‌کند
    window.HamyarAppearanceBridge = { resolvedTheme: () => appTheme, themePreference: () => appTheme };
    window.document.documentElement.setAttribute("data-hamyar-theme-preference", appTheme);
    window.document.documentElement.setAttribute("data-hamyar-theme", appTheme);
  }
  await new Promise((r) => window.addEventListener("load", r, { once: true }));
  return dom;
}

const innerDoc = (w) => w.document.getElementById("backgroundMusicFrame").contentDocument;
const innerWin = (w) => w.document.getElementById("backgroundMusicFrame").contentWindow;

// ---------- ۱) tile ----------
{
  console.log("\n— tile (کادر ۹۲px + پاپ‌آپ تمام‌صفحه + بسته‌شدن پس از ۲ ثانیه) —");
  const dom = await load("background-music-tile.html", { appTheme: "dark" });
  const w = dom.window;
  await sleep(2500);
  const idoc = innerDoc(w);
  check("tile: iframe پخش‌کننده لود شد", !!idoc && !!idoc.getElementById("mini"));
  check("tile: embedded فعال است", idoc.body.classList.contains("embedded"));
  check("tile: تم اپ (dark) روی tile نشست", w.document.documentElement.getAttribute("data-theme") === "dark",
    "data-theme=" + w.document.documentElement.getAttribute("data-theme"));
  check("tile: تم اپ به پخش‌کنندهٔ داخل iframe هم رسید", idoc.documentElement.getAttribute("data-theme") === "dark",
    "data-theme=" + idoc.documentElement.getAttribute("data-theme"));
  check("tile: کلید ماه/خورشید محلی قفل شد", idoc.documentElement.hasAttribute("data-hamyar-app-theme"));
  check("tile: در شروع بسته است", !w.document.body.classList.contains("is-open"));

  // لمس کادر → باز شدن پاپ‌آپ
  idoc.getElementById("mini").dispatchEvent(new idoc.defaultView.MouseEvent("click", { bubbles: true }));
  await sleep(150);
  check("tile: با لمس، پاپ‌آپ تمام‌صفحه باز شد", w.document.body.classList.contains("is-open"));
  check("tile: پخش‌کننده در پاپ‌آپ بدون سقف ارتفاع است (variant-full)", idoc.body.classList.contains("variant-full"));
  check("tile: اورلی روی صفحهٔ میزبان هست", !!w.document.getElementById("scrim"));

  // ۲ ثانیه بی‌لمسی → بسته شدن و بازگشت سر جای قبلی
  await sleep(2400);
  check("tile: پس از ۲ ثانیه بی‌لمسی بسته شد", !w.document.body.classList.contains("is-open"));
  check("tile: پخش‌کننده به حالت کادر برگشت", !idoc.body.classList.contains("variant-full"));
  check("tile: شیت داخلی هم بسته شد", idoc.getElementById("backdrop").hidden === true);

  // باز کردن دوباره + لمس پیاپی باید تایمر را ری‌ست کند
  idoc.getElementById("mini").dispatchEvent(new idoc.defaultView.MouseEvent("click", { bubbles: true }));
  await sleep(100);
  for (let i = 0; i < 6; i++) {
    await sleep(400);
    idoc.getElementById("grid").dispatchEvent(new idoc.defaultView.MouseEvent("pointerdown", { bubbles: true }));
  }
  check("tile: لمس پیاپی تایمر را ری‌ست کرد (هنوز باز است)", w.document.body.classList.contains("is-open"));
  await sleep(2400);
  check("tile: پس از توقف لمس‌ها بسته شد", !w.document.body.classList.contains("is-open"));

  // دکمهٔ ضربدر داخل پاپ‌آپ باید کار کند (برخلاف صفحهٔ full)
  idoc.getElementById("mini").dispatchEvent(new idoc.defaultView.MouseEvent("click", { bubbles: true }));
  await sleep(150);
  idoc.getElementById("close").dispatchEvent(new idoc.defaultView.MouseEvent("click", { bubbles: true }));
  await sleep(200);
  check("tile: دکمهٔ بستن داخل پاپ‌آپ، کادر را به جای قبلی برمی‌گرداند",
    !w.document.body.classList.contains("is-open") && idoc.getElementById("backdrop").hidden === true);
  dom.window.close();
}

// ---------- ۲) full ----------
{
  console.log("\n— full (همیشه باز، همهٔ منوها، بدون سقف ارتفاع) —");
  const dom = await load("background-music-full.html", { appTheme: "light" });
  const w = dom.window;
  await sleep(2500);
  const idoc = innerDoc(w);
  const iwin = innerWin(w);
  check("full: پخش‌کننده لود شد", !!idoc && !!idoc.getElementById("sheet"));
  check("full: گونهٔ full فعال است", idoc.body.classList.contains("variant-full"));
  check("full: پنجره از ابتدا باز است", idoc.getElementById("backdrop").hidden === false);
  iwin.BackgroundMusic.close();
  await sleep(100);
  check("full: close() هم آن را نمی‌بندد (هرگز بسته نمی‌شود)", idoc.getElementById("backdrop").hidden === false);
  check("full: همهٔ منوها هست (دسته‌بندی + کاتالوگ + دو لایه + ولوم)",
    !!idoc.getElementById("filters").children.length &&
      !!idoc.getElementById("grid").children.length &&
      !!idoc.getElementById("slot0") && !!idoc.getElementById("slot1") &&
      !!idoc.getElementById("volume"));
  check("full: دکمهٔ بستن مخفی است", (idoc.defaultView.getComputedStyle(idoc.getElementById("close")).display || "") === "none");
  check("full: embedded فعال است (state به میزبان می‌رسد)", idoc.body.classList.contains("embedded"));
  check("full: تم روشنِ اپ روی صفحه و پخش‌کننده", w.document.documentElement.getAttribute("data-theme") === "light" &&
    idoc.documentElement.getAttribute("data-theme") === "light");

  // تغییر تم زنده از سمت اپ
  w.HamyarAppearanceBridge = { resolvedTheme: () => "dark", themePreference: () => "dark" };
  w.document.documentElement.setAttribute("data-hamyar-theme", "dark");
  await sleep(300);
  check("full: تغییر زندهٔ تم اپ تا داخل پخش‌کننده رفت",
    w.document.documentElement.getAttribute("data-theme") === "dark" &&
    idoc.documentElement.getAttribute("data-theme") === "dark",
    "tile=" + w.document.documentElement.getAttribute("data-theme") + " player=" + idoc.documentElement.getAttribute("data-theme"));
  dom.window.close();
}

// ---------- ۳) پخش‌کنندهٔ تنها، بدون اپ ----------
{
  console.log("\n— player مستقل (بدون اپ): تم سیستم —");
  const dom = await load("background-music.html", { systemDark: true });
  const w = dom.window;
  await sleep(2000);
  check("player: بدون اپ هم تم دارد", ["dark", "light"].includes(w.document.documentElement.getAttribute("data-theme")),
    "data-theme=" + w.document.documentElement.getAttribute("data-theme"));
  check("player: بدون اپ، کلید ماه/خورشید آزاد است", !w.document.documentElement.hasAttribute("data-hamyar-app-theme"));
  check("player: پیش‌فرض گونهٔ کادر (نه full)", !w.document.body.classList.contains("variant-full"));
  w.BackgroundMusic.setFull(true, true);
  await sleep(100);
  check("player: setFull(true,true) سقف ارتفاع را برمی‌دارد و باز می‌کند",
    w.document.body.classList.contains("variant-full") && w.document.getElementById("backdrop").hidden === false);
  dom.window.close();
}

const failed = results.filter((r) => r[0] === "FAIL");
console.log(`\nخلاصه: ${results.length - failed.length}/${results.length} پاس`);
process.exit(failed.length ? 1 : 0);
