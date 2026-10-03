# -*- coding: utf-8 -*-
"""merge+compress+embed صوت + پلیر باریک حافظه‌دار — پارامتری برای همه درس‌ها
usage: python3 embed_audio.py TAG HTML KEY NSECS
example: python3 embed_audio.py 02-handwriting 02-handwriting.html lp_handwriting 13
"""
import re, os, glob, json, base64, subprocess, sys
import imageio_ffmpeg

TAG, HTMLF, KEY, N = sys.argv[1], sys.argv[2], sys.argv[3], int(sys.argv[4])
FF = imageio_ffmpeg.get_ffmpeg_exe()
ADIR = f"/home/user/آموزشگاه/audio/{TAG}"
SRC_HTML = f"/home/user/آموزشگاه/{HTMLF}"

for i in range(1, N + 1):
    parts = sorted(glob.glob(f"{ADIR}/sec{i}_part*.mp3"))
    out = f"{ADIR}/sec{i}.mp3"
    if not parts and os.path.exists(out):
        print(f"sec{i}: final exists, skip merge")
        continue
    assert parts, f"no parts for sec{i}"
    lst = f"{ADIR}/_list{i}.txt"
    with open(lst, "w", encoding="utf-8") as f:
        for p in parts:
            f.write(f"file '{p}'\n")
    out = f"{ADIR}/sec{i}.mp3"
    r = subprocess.run([FF, "-y", "-v", "error", "-f", "concat", "-safe", "0",
                        "-i", lst, "-c:a", "libmp3lame", "-b:a", "24k",
                        "-ac", "1", "-ar", "22050", out],
                       capture_output=True, text=True)
    assert os.path.exists(out) and os.path.getsize(out) > 0, f"merge failed sec{i}: {r.stderr}"
    r2 = subprocess.run([FF, "-i", out], capture_output=True, text=True)
    m = re.search(r"Duration: (\d+):(\d+):([\d.]+)", r2.stderr)
    print(f"sec{i}: {len(parts)} parts → {os.path.getsize(out)//1024} KB, duration {m.group(0).split(' ')[1].split('.')[0] if m else '?'}")
    os.remove(lst)

for p in glob.glob(f"{ADIR}/sec*_part*.mp3"):
    os.remove(p)
print("part files removed.")

uris = []
for i in range(1, N + 1):
    with open(f"{ADIR}/sec{i}.mp3", "rb") as f:
        uris.append("data:audio/mpeg;base64," + base64.b64encode(f.read()).decode())

with open(SRC_HTML, encoding="utf-8") as f:
    html = f.read()

css = """
.ap{display:flex;align-items:center;gap:8px;height:30px;padding:0 10px;margin:0 0 10px;border:1px solid var(--card-b);border-radius:999px;background:var(--bg)}
.ap .pp,.ap .mu{flex:none;width:22px;height:22px;border:none;border-radius:50%;background:linear-gradient(135deg,var(--accent),var(--accent2));color:#fff;display:flex;align-items:center;justify-content:center;cursor:pointer;padding:0}
.ap .pp svg,.ap .mu svg{width:12px;height:12px}
.ap .bar{flex:1;height:4px;border-radius:4px;background:var(--card-b);cursor:pointer;overflow:hidden}
.ap .fill{height:100%;width:0;border-radius:4px;background:linear-gradient(90deg,var(--accent),var(--accent2))}
.ap .tm{flex:none;font-size:10px;color:var(--muted);white-space:nowrap;min-width:70px;text-align:center}
.ap .vol{flex:none;width:80px;accent-color:var(--accent);cursor:pointer;padding:0;direction:ltr}
"""
assert ".ap{" not in html, "player already embedded!"
html = html.replace("</style>", css + "</style>")

AUDIO_JS = "const AUDIO = " + json.dumps(uris) + ";\n" + """
const store={get(k,d){try{const v=localStorage.getItem(KEY+'_'+k);return v===null?d:v;}catch(e){return d;}},set(k,v){try{localStorage.setItem(KEY+'_'+k,v);}catch(e){}}};
const FA_D='۰۱۲۳۴۵۶۷۸۹';
const faT=s=>{s=Math.max(0,Math.floor(s||0));const m=Math.floor(s/60),r=s%60;const f=n=>String(n).replace(/[0-9]/g,d=>FA_D[d]);return f(m)+':'+String(r).padStart(2,'0').replace(/[0-9]/g,d=>FA_D[d]);};
const SVG_PLAY='<svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z" fill="#fff"/></svg>';
const SVG_PAUSE='<svg viewBox="0 0 24 24"><path d="M6 5h4v14H6zM14 5h4v14h-4z" fill="#fff"/></svg>';
const SVG_VOL='<svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3z" fill="#fff"/><path d="M16 8.5a5 5 0 010 7M18.5 6a8.5 8.5 0 010 12" stroke="#fff" stroke-width="2" fill="none" stroke-linecap="round"/></svg>';
const SVG_MUTE='<svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3z" fill="#fff"/><path d="M16 9l6 6M22 9l-6 6" stroke="#fff" stroke-width="2" fill="none" stroke-linecap="round"/></svg>';
const audios=AUDIO.map((src,i)=>{
  const a=new Audio();a.preload='metadata';a.src=src;
  a.volume=parseFloat(store.get('vol','1'));if(isNaN(a.volume))a.volume=1;
  a.muted=store.get('mut','0')==='1';
  const saved=parseFloat(store.get('t'+i,'0'))||0;let lastSave=0;
  a.addEventListener('loadedmetadata',()=>{if(saved>1&&a.duration&&saved<a.duration-5){try{a.currentTime=saved;}catch(e){}}syncUI(i);});
  a.addEventListener('timeupdate',()=>{syncUI(i);const n=Date.now();if(n-lastSave>4000){lastSave=n;store.set('t'+i,a.currentTime.toFixed(1));}});
  a.addEventListener('play',()=>{audios.forEach((o,j)=>{if(j!==i&&!o.paused)o.pause();});syncUI(i);});
  a.addEventListener('pause',()=>{store.set('t'+i,a.currentTime.toFixed(1));syncUI(i);});
  a.addEventListener('ended',()=>{store.set('t'+i,'0');try{a.currentTime=0;}catch(e){}syncUI(i);});
  return a;
});
function syncUI(i){const box=document.querySelector('.ap[data-i="'+i+'"]');if(!box)return;const a=audios[i];box.querySelector('.pp').innerHTML=a.paused?SVG_PLAY:SVG_PAUSE;const d=a.duration||0,c=a.currentTime||0;box.querySelector('.fill').style.width=(d>0?(c/d*100):0)+'%';box.querySelector('.tm').textContent=faT(c)+' / '+faT(d);box.querySelector('.mu').innerHTML=(a.muted||a.volume===0)?SVG_MUTE:SVG_VOL;const v=box.querySelector('.vol');if(document.activeElement!==v)v.value=a.muted?0:a.volume;}
function apToggle(i){const a=audios[i];if(a.paused){a.play().catch(()=>{});}else{a.pause();}}
function apSeek(i,ev){const a=audios[i];if(!a.duration)return;const bar=ev.currentTarget,r=bar.getBoundingClientRect();let p=(ev.clientX-r.left)/r.width;if(getComputedStyle(bar).direction==='rtl')p=1-p;a.currentTime=Math.min(Math.max(p,0),1)*a.duration;syncUI(i);}
function apVol(i,v){v=parseFloat(v);audios.forEach(a=>{a.volume=v;if(v>0)a.muted=false;});store.set('vol',String(v));store.set('mut',v===0?'1':'0');audios.forEach((_,j)=>syncUI(j));}
function apMute(){const m=!audios[0].muted;audios.forEach(a=>{a.muted=m;});store.set('mut',m?'1':'0');audios.forEach((_,j)=>syncUI(j));}
function playerHTML(i){const vv=store.get('mut','0')==='1'?0:store.get('vol','1');return '<div class="ap" data-i="'+i+'"><button class="pp" onclick="apToggle('+i+')" aria-label="پخش / مکث">'+SVG_PLAY+'</button><div class="bar" onclick="apSeek('+i+',event)"><div class="fill"></div></div><span class="tm">۰:۰۰ / ۰:۰۰</span><button class="mu" onclick="apMute()" aria-label="بی‌صدا">'+SVG_VOL+'</button><input class="vol" type="range" min="0" max="1" step="0.05" value="'+vv+'" oninput="apVol('+i+',this.value)" aria-label="بلندی صدا"></div>';}
"""

html = html.replace("function toggle(i){", AUDIO_JS + "function toggle(i){", 1)
html = html.replace('<div class="inner">${s.b}</div>',
                    '<div class="inner">${playerHTML(i)}${s.b}</div>', 1)
html = html.replace("\nrender();\n</script>",
                    "\nrender();\naudios.forEach((_,j)=>syncUI(j));\n</script>", 1)
html = re.sub(r'(<div class="meta">.*?)</div>', rf"\1 • {N} فایل صوتی</div>", html, count=1)

with open(SRC_HTML, "w", encoding="utf-8") as f:
    f.write(html)

print(f"HTML size: {os.path.getsize(SRC_HTML)/1024/1024:.1f} MB — done")
