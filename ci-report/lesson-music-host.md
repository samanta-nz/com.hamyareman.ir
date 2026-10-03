# بخش میزبانِ موسیقی در فایل‌های درس

## یوگا — `Bucket/Html-files/01 - حالت کودک.html`

- HTTP 200، 1,792,747 بایت، HMK1: True
- طول متنِ رمزگشایی‌شده: 1,791,364 نویسه

### متای viewport

```html
<meta name="viewport" content="width=device-width,initial-scale=1">
```

### تگ‌های iframe

### هر جای کد که iframe موسیقی را می‌سازد/تغییر می‌دهد

```js
cument.querySelector(options.container):options.container;if(!container)throw Error('Background audio container not found');if(container.__backgroundMusic)return container.__backgroundMusic;const frame=document.createElement('iframe');frame.title='انتخاب و کنترل صدای پس‌زمینه';frame.allow='autoplay';frame.setAttribute('scrolling','no');frame.style.cssText='display:block;border:0;width:100%;height:88px;background:transparent;';container.style.minHeight='88px';let expectedOrigin='null';if(options.html){frame.srcdoc=options.html.replace('const TRACKS=','window.BG_EMBED=true;const TRACKS=');frame.setAttribute('sandbox','allow-scripts')}else{const url=new URL(options.src||'موسیقی پس زمینه.html',location.href);url.hash='embedded';expectedOrigin=/^https?:$/.test(url.protocol)?url.origin:'null';fr

/* ---- */

ht='88px';let expectedOrigin='null';if(options.html){frame.srcdoc=options.html.replace('const TRACKS=','window.BG_EMBED=true;const TRACKS=');frame.setAttribute('sandbox','allow-scripts')}else{const url=new URL(options.src||'موسیقی پس زمینه.html',location.href);url.hash='embedded';expectedOrigin=/^https?:$/.test(url.protocol)?url.origin:'null';frame.src=url.href}let expanded=false,oldOverflow='',destroyed=false;function send(type,extra={}){if(destroyed)return;frame.contentWindow?.postMessage({channel:CHANNEL,type,...extra},expectedOrigin==='null'?'*':expectedOrigin)}function resize(open){if(expanded===open)return;expanded=open;if(open){oldOverflow=document.body.style.overflow;document.body.style.overflow='hidden';frame.style.cssText='display:block;border:0;position:fixed;top:0;right:0;botto

/* ---- */

nded===open)return;expanded=open;if(open){oldOverflow=document.body.style.overflow;document.body.style.overflow='hidden';frame.style.cssText='display:block;border:0;position:fixed;top:0;right:0;bottom:0;left:0;z-index:2147483647;width:100%;height:100%;max-width:none;background:transparent;'}else{document.body.style.overflow=oldOverflow;frame.style.cssText='display:block;border:0;width:100%;height:88px;background:transparent;'}}function receive(e){if(e.source!==frame.contentWindow||e.data?.channel!==CHANNEL)return;if(expectedOrigin!=='null'&&e.origin!==expectedOrigin)return;if(e.data.type==='prefs-get'){let preferences=null;try{preferences=JSON.parse(localStorage.getItem(CHANNEL)||'null')}catch(err){}send('prefs',{preferences});return}if(e.data.type==='prefs-set'){try{localStorage.setItem(C

/* ---- */

en:()=>send('open'),close:()=>send('close'),pause:()=>send('pause'),setVolume:volume=>send('volume',{volume}),setTheme:theme=>send('theme',{theme}),destroy(){send('pause');resize(false);destroyed=true;window.removeEventListener('message',receive);frame.remove();delete container.__backgroundMusic}};container.__backgroundMusic=api;return api}
window.HamyaremanBackground={mount};function auto(){document.querySelectorAll('[data-bg-player]').forEach(container=>mount({container,src:container.dataset.src||'موسیقی پس زمینه.html'}))}if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',auto);else auto();
})();

(()=>{'use strict';const $=id=>document.getElementById(id),fa=s=>String(s).replace(/\d/g,n=>'۰۱۲۳۴۵۶۷۸۹'[n]),clock=t=>fa(Math.floor(Math.max(0,t)/60)+':'+String(Mat

/* ---- */

myarTheme.initial();let v=localStorage.getItem('yoga-coach-volume');audio.volume=v===null?.85:Math.max(0,Math.min(1,Number(v)))}catch(e){audio.volume=.85}
function themeUI(value){theme=value==='light'?'light':'dark';document.documentElement.dataset.theme=theme;$('theme').textContent=theme==='dark'?'☀':'☾';HamyarTheme.save(theme);}
const background=HamyaremanBackground.mount({container:'#background-slot',src:'./background-music.html',onState(s){if(!ready){ready=true;background.setTheme(theme)}else if(s.theme&&s.theme!==theme)themeUI(s.theme)}});
$('theme').onclick=()=>{themeUI(theme==='dark'?'light':'dark');background.setTheme(theme)};themeUI(theme);audio.src=CONFIG.audio;document.title=CONFIG.title+' | همیار من';$('title').textContent=CONFIG.title;$('english').textContent=CONFIG.english;$(

/* ---- */

ost)return;const animation=ghost.animate([{opacity:1},{opacity:0}],{duration:450,easing:'ease-in-out',fill:'forwards'});animation.onfinish=()=>ghost.remove();animation.oncancel=()=>ghost.remove()}
// Preload each distinct embedded image once; no extra image bytes are embedded.
Object.values(CONFIG.frames).forEach(src=>{const image=new Image();image.src=src;image.decode?.().catch(()=>{})});
function render(){let t=Math.min(CONFIG.duration,audio.currentTime||0),idx=0;for(let i=0;i<CONFIG.events.length;i++){if(CONFIG.events[i].t<=t)idx=i;else break}let e=CONFIG.events[idx];
const changed=frame!==e.frame;const key=e.frame+'/'+(e.focus||'full');const focusChanged=key!==lastFocus;let mainGhost=changed&&frame?outgoing($('photo')):null;let insetGhost=focusChanged&&lastFocus?outgoing($('zoom')):nul

/* ---- */

G.events.length;i++){if(CONFIG.events[i].t<=t)idx=i;else break}let e=CONFIG.events[idx];
const changed=frame!==e.frame;const key=e.frame+'/'+(e.focus||'full');const focusChanged=key!==lastFocus;let mainGhost=changed&&frame?outgoing($('photo')):null;let insetGhost=focusChanged&&lastFocus?outgoing($('zoom')):null;
if(changed){frame=e.frame;$('photo').src=CONFIG.frames[frame];$('photo').alt=CONFIG.title+' — '+e.title+'؛ بدن و زیرانداز کامل';$('detailPhoto').setAttribute('href',CONFIG.frames[frame]);fadeAway(mainGhost)}
const p=CONFIG.focus[frame]?.[e.focus];$('focusInset').hidden=false;if(focusChanged){lastFocus=key;$('zoom').setAttribute('viewBox',p?p.box:'0 0 1120 751');$('zoom').setAttribute('aria-label',p?'نقطه توجه: '+p.title:'نمای کامل حرکت');$('focusTitle').textContent=p?p.title:'نمای 

/* ---- */

t=t}}
function loop(){if(!counting)return;const left=Math.max(0,(deadline-performance.now())/1000),sec=Math.ceil(left),fg=$('hyFg'),num=$('hyNum');if(fg)fg.style.strokeDashoffset=String(100*(1-left/WAIT));if(num)num.textContent=fa(sec);if(sec>0&&sec<=TICK_FROM&&sec!==lastTick){lastTick=sec;tick(false,audio.volume)}if(left<=0){finish();return}timer=setTimeout(loop,100)}
function startCount(){if(counting||navigating)return;counting=true;deadline=performance.now()+WAIT*1000;lastTick=-1;nextBtn.hidden=false;nextBtn.innerHTML=ringHTML(hasNext?'بعدی':'پایان');nextBtn.setAttribute('aria-label',hasNext?'رفتن به حرکت بعدی؛ ادامهٔ خودکار':'پایان دوره؛ شمارش معکوس');loop()}
function cancelCount(){if(!counting)return;counting=false;clearTimeout(timer);nextBtn.innerHTML=nextHTML;nextBtn.setAttribute('a

/* ---- */

ria-label',hasNext?'رفتن به حرکت بعدی؛ ادامهٔ خودکار':'پایان دوره؛ شمارش معکوس');loop()}
function cancelCount(){if(!counting)return;counting=false;clearTimeout(timer);nextBtn.innerHTML=nextHTML;nextBtn.setAttribute('aria-label',nextLabel);nextBtn.hidden=!hasNext}
function go(dir,auto){if(!ready(dir)||navigating)return;cancelCount();navigating=true;setTimeout(()=>{navigating=false},8000);try{audio.pause()}catch(e){}const u=new URL(CONFIG[dir],location.href);u.search=location.search;u.hash=auto?'autostart':'';location.href=u.href}
function finish(){counting=false;clearTimeout(timer);if(hasNext){tick(true,audio.volume);go('next',true)}else{nextBtn.innerHTML=nextHTML;nextBtn.hidden=true;rawPause();say('دورهٔ تمرین‌ها به پایان رسید؛ موسیقی پس‌زمینه متوقف شد.')}}
nextBtn.onclick=()=>{if(counting

/* ---- */

as=counting;cancelCount();if(!audio.paused)audio.pause();rawPause();if(was)say('با خروج از صفحه، ادامهٔ خودکار متوقف شد.')});
if(location.hash==='#autostart'){try{history.replaceState(null,'',location.pathname+location.search)}catch(e){}const run=()=>{window.scrollTo(0,0);Promise.resolve(P.play()).catch(()=>{})};if(document.readyState==='complete')setTimeout(run,350);else window.addEventListener('load',()=>setTimeout(run,350),{once:true})}
window.HyFlow={get counting(){return counting},startCount,cancelCount};
})();</script>
</body></html>
```

## ورزش — `Bucket/Html-files/اسکوات آرام.html`

- HTTP 200، 1,884,196 بایت، HMK1: True
- طول متنِ رمزگشایی‌شده: 1,878,782 نویسه

### متای viewport

```html
<meta name="viewport" content="width=device-width,initial-scale=1">
```

### تگ‌های iframe

### هر جای کد که iframe موسیقی را می‌سازد/تغییر می‌دهد

```js
cument.querySelector(options.container):options.container;if(!container)throw Error('Background audio container not found');if(container.__backgroundMusic)return container.__backgroundMusic;const frame=document.createElement('iframe');frame.title='انتخاب و کنترل صدای پس‌زمینه';frame.allow='autoplay';frame.setAttribute('scrolling','no');frame.style.cssText='display:block;border:0;width:100%;height:88px;background:transparent;';container.style.minHeight='88px';let expectedOrigin='null';if(options.html){frame.srcdoc=options.html.replace('const TRACKS=','window.BG_EMBED=true;const TRACKS=');frame.setAttribute('sandbox','allow-scripts')}else{const url=new URL(options.src||'موسیقی پس زمینه.html',location.href);url.hash='embedded';expectedOrigin=/^https?:$/.test(url.protocol)?url.origin:'null';fr

/* ---- */

ht='88px';let expectedOrigin='null';if(options.html){frame.srcdoc=options.html.replace('const TRACKS=','window.BG_EMBED=true;const TRACKS=');frame.setAttribute('sandbox','allow-scripts')}else{const url=new URL(options.src||'موسیقی پس زمینه.html',location.href);url.hash='embedded';expectedOrigin=/^https?:$/.test(url.protocol)?url.origin:'null';frame.src=url.href}let expanded=false,oldOverflow='',destroyed=false;function send(type,extra={}){if(destroyed)return;frame.contentWindow?.postMessage({channel:CHANNEL,type,...extra},expectedOrigin==='null'?'*':expectedOrigin)}function resize(open){if(expanded===open)return;expanded=open;if(open){oldOverflow=document.body.style.overflow;document.body.style.overflow='hidden';frame.style.cssText='display:block;border:0;position:fixed;top:0;right:0;botto

/* ---- */

nded===open)return;expanded=open;if(open){oldOverflow=document.body.style.overflow;document.body.style.overflow='hidden';frame.style.cssText='display:block;border:0;position:fixed;top:0;right:0;bottom:0;left:0;z-index:2147483647;width:100%;height:100%;max-width:none;background:transparent;'}else{document.body.style.overflow=oldOverflow;frame.style.cssText='display:block;border:0;width:100%;height:88px;background:transparent;'}}function receive(e){if(e.source!==frame.contentWindow||e.data?.channel!==CHANNEL)return;if(expectedOrigin!=='null'&&e.origin!==expectedOrigin)return;if(e.data.type==='prefs-get'){let preferences=null;try{preferences=JSON.parse(localStorage.getItem(CHANNEL)||'null')}catch(err){}send('prefs',{preferences});return}if(e.data.type==='prefs-set'){try{localStorage.setItem(C

/* ---- */

en:()=>send('open'),close:()=>send('close'),pause:()=>send('pause'),setVolume:volume=>send('volume',{volume}),setTheme:theme=>send('theme',{theme}),destroy(){send('pause');resize(false);destroyed=true;window.removeEventListener('message',receive);frame.remove();delete container.__backgroundMusic}};container.__backgroundMusic=api;return api}
window.HamyaremanBackground={mount};function auto(){document.querySelectorAll('[data-bg-player]').forEach(container=>mount({container,src:container.dataset.src||'موسیقی پس زمینه.html'}))}if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',auto);else auto();
})();

(()=>{'use strict';const $=id=>document.getElementById(id),fa=s=>String(s).replace(/\d/g,n=>'۰۱۲۳۴۵۶۷۸۹'[n]),clock=t=>fa(Math.floor(Math.max(0,t)/60)+':'+String(Mat

/* ---- */

arTheme.initial();let v=localStorage.getItem('sports-coach-volume');audio.volume=v===null?.85:Math.max(0,Math.min(1,Number(v)))}catch(e){audio.volume=.85}
function themeUI(value){theme=value==='light'?'light':'dark';document.documentElement.dataset.theme=theme;$('theme').textContent=theme==='dark'?'☀':'☾';HamyarTheme.save(theme);}
const background=HamyaremanBackground.mount({container:'#background-slot',src:'./background-music.html',onState(s){if(!ready){ready=true;background.setTheme(theme)}else if(s.theme&&s.theme!==theme)themeUI(s.theme)}});
$('theme').onclick=()=>{themeUI(theme==='dark'?'light':'dark');background.setTheme(theme)};themeUI(theme);audio.src=CONFIG.audio;document.title=CONFIG.title+' | همیار من';$('title').textContent=CONFIG.title;$('english').textContent=CONFIG.english;$(

/* ---- */

ost)return;const animation=ghost.animate([{opacity:1},{opacity:0}],{duration:450,easing:'ease-in-out',fill:'forwards'});animation.onfinish=()=>ghost.remove();animation.oncancel=()=>ghost.remove()}
// Preload each distinct embedded image once; no extra image bytes are embedded.
Object.values(CONFIG.frames).forEach(src=>{const image=new Image();image.src=src;image.decode?.().catch(()=>{})});
function render(){let t=Math.min(CONFIG.duration,audio.currentTime||0),idx=0;for(let i=0;i<CONFIG.events.length;i++){if(CONFIG.events[i].t<=t)idx=i;else break}let e=CONFIG.events[idx];
const changed=frame!==e.frame;const key=e.frame+'/'+(e.focus||'full');const focusChanged=key!==lastFocus;let mainGhost=changed&&frame?outgoing($('photo')):null;let insetGhost=focusChanged&&lastFocus?outgoing($('zoom')):nul

/* ---- */

G.events.length;i++){if(CONFIG.events[i].t<=t)idx=i;else break}let e=CONFIG.events[idx];
const changed=frame!==e.frame;const key=e.frame+'/'+(e.focus||'full');const focusChanged=key!==lastFocus;let mainGhost=changed&&frame?outgoing($('photo')):null;let insetGhost=focusChanged&&lastFocus?outgoing($('zoom')):null;
if(changed){frame=e.frame;$('photo').src=CONFIG.frames[frame];$('photo').alt=CONFIG.title+' — '+e.title+'؛ بدن و زیرانداز کامل';$('detailPhoto').src=CONFIG.frames[frame];fadeAway(mainGhost)}
const p=CONFIG.focus[frame]?.[e.focus];$('focusInset').hidden=false;if(focusChanged){lastFocus=key;$('zoom').setAttribute('aria-label',p?'نقطه توجه: '+p.title:'نمای کامل حرکت');$('focusTitle').textContent=p?p.title:'نمای کامل حرکت';fadeAway(insetGhost)}
const [bx,by,bw,bh]=p?p.box.split(' ').ma

/* ---- */

t=t}}
function loop(){if(!counting)return;const left=Math.max(0,(deadline-performance.now())/1000),sec=Math.ceil(left),fg=$('hyFg'),num=$('hyNum');if(fg)fg.style.strokeDashoffset=String(100*(1-left/WAIT));if(num)num.textContent=fa(sec);if(sec>0&&sec<=TICK_FROM&&sec!==lastTick){lastTick=sec;tick(false,audio.volume)}if(left<=0){finish();return}timer=setTimeout(loop,100)}
function startCount(){if(counting||navigating)return;counting=true;deadline=performance.now()+WAIT*1000;lastTick=-1;nextBtn.hidden=false;nextBtn.innerHTML=ringHTML(hasNext?'بعدی':'پایان');nextBtn.setAttribute('aria-label',hasNext?'رفتن به حرکت بعدی؛ ادامهٔ خودکار':'پایان دوره؛ شمارش معکوس');loop()}
function cancelCount(){if(!counting)return;counting=false;clearTimeout(timer);nextBtn.innerHTML=nextHTML;nextBtn.setAttribute('a

/* ---- */

ria-label',hasNext?'رفتن به حرکت بعدی؛ ادامهٔ خودکار':'پایان دوره؛ شمارش معکوس');loop()}
function cancelCount(){if(!counting)return;counting=false;clearTimeout(timer);nextBtn.innerHTML=nextHTML;nextBtn.setAttribute('aria-label',nextLabel);nextBtn.hidden=!hasNext}
function go(dir,auto){if(!ready(dir)||navigating)return;cancelCount();navigating=true;setTimeout(()=>{navigating=false},8000);try{audio.pause()}catch(e){}const u=new URL(CONFIG[dir],location.href);u.search=location.search;u.hash=auto?'autostart':'';location.href=u.href}
function finish(){counting=false;clearTimeout(timer);if(hasNext){tick(true,audio.volume);go('next',true)}else{nextBtn.innerHTML=nextHTML;nextBtn.hidden=true;rawPause();say('دورهٔ تمرین‌ها به پایان رسید؛ موسیقی پس‌زمینه متوقف شد.')}}
nextBtn.onclick=()=>{if(counting

/* ---- */

as=counting;cancelCount();if(!audio.paused)audio.pause();rawPause();if(was)say('با خروج از صفحه، ادامهٔ خودکار متوقف شد.')});
if(location.hash==='#autostart'){try{history.replaceState(null,'',location.pathname+location.search)}catch(e){}const run=()=>{window.scrollTo(0,0);Promise.resolve(P.play()).catch(()=>{})};if(document.readyState==='complete')setTimeout(run,350);else window.addEventListener('load',()=>setTimeout(run,350),{once:true})}
window.HyFlow={get counting(){return counting},startCount,cancelCount};
})();</script>
</body></html>
```

