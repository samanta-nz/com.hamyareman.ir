// تستِ دو قابِ «تمِ خودکار» (بدون دکمهٔ ماه/خورشید).
//   python3 -m http.server 8010 --bind 0.0.0.0 --directory dist/music-auto-theme
//   MUSIC_BASE=http://127.0.0.1:8010/ node scripts/test_music_auto_theme.mjs
import { JSDOM, VirtualConsole } from "jsdom";

const BASE = process.env.MUSIC_BASE || "http://127.0.0.1:8010/";
const results = [];
const check = (name, cond, extra = "") => {
  results.push(cond);
  console.log(`${cond ? "✅" : "❌"} ${name}${extra ? " — " + extra : ""}`);
};

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
        addEventListener() {}, removeEventListener() {},
        addListener() {}, removeListener() {},
      });
      if (appTheme) {
        window.HamyarAppearanceBridge = { resolvedTheme: () => appTheme, themePreference: () => appTheme };
      }
    },
  });
  await new Promise((r) => dom.window.addEventListener("load", r, { once: true }));
  return dom;
}

for (const page of ["background-music-tile.html", "background-music-full.html"]) {
  console.log(`\n— ${page} —`);
  const text = await (await fetch(BASE + page)).text();
  check(`${page}: ۲۹ صدای embed و خودبسنده`,
    (text.match(/"audio":"/g) || []).length === 29 && !/<iframe[^>]+background-music/.test(text),
    `${(text.length / 1048576).toFixed(2)}MB`);
  check(`${page}: هیچ نشانِ ☾/☀ در فایل نمانده`, !text.includes("☾") && !text.includes("☀"));

  // الف) سیستم روشن، بدون اپ
  let dom = await load(page);
  let doc = dom.window.document;
  check("دکمهٔ تم در DOM نیست", !doc.getElementById("theme") && !doc.getElementById("miniTheme"));
  check("سیستم روشن ← تمِ روشن", doc.documentElement.getAttribute("data-theme") === "light");
  dom.window.close();

  // ب) سیستم تاریک، بدون اپ ← خودکار تاریک
  dom = await load(page, { systemDark: true });
  doc = dom.window.document;
  check("سیستم تاریک ← تمِ تاریک خودکار", doc.documentElement.getAttribute("data-theme") === "dark");
  check("meta theme-color هم تاریک شد",
    doc.querySelector("meta[name=theme-color]").getAttribute("content") === "#101713");
  dom.window.close();

  // ج) اپ تاریک ولی سیستم روشن ← اپ برنده است
  dom = await load(page, { appTheme: "dark", systemDark: false });
  doc = dom.window.document;
  check("تمِ اپ بر سیستم مقدم است", doc.documentElement.getAttribute("data-theme") === "dark");
  // د) تغییر زندهٔ تمِ اپ
  dom.window.HamyarAppearanceBridge.resolvedTheme = () => "light";
  dom.window.dispatchEvent(new dom.window.Event("hamyarappearancechange"));
  await new Promise((r) => setTimeout(r, 60));
  check("تغییر زندهٔ تمِ اپ اعمال شد", doc.documentElement.getAttribute("data-theme") === "light");
  // ه) فرمان مستقیم میزبان
  dom.window.HamyarSetTheme("dark");
  await new Promise((r) => setTimeout(r, 60));
  check("HamyarSetTheme کار می‌کند", doc.documentElement.getAttribute("data-theme") === "dark");
  check("UI هم تاریک شد (state.theme)", dom.window.BackgroundMusic.state.theme === "dark");
  dom.window.close();
}

const bad = results.filter((r) => !r).length;
console.log(`\n${results.length - bad}/${results.length} تست سبز`);
process.exit(bad ? 1 : 0);
