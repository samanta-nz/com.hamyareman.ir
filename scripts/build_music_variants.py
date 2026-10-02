#!/usr/bin/env python3
"""ساخت دو گونهٔ خودبسندهٔ موسیقی از روی فایل پلیر.

ورودی: `background-music.html` (پلیر کامل با ۲۹ صدای embed شده)
خروجی:
  * `background-music-tile.html` — کادر ۹۲px در جریان صفحه؛ با لمس، پاپ‌آپ
    تمام‌صفحه؛ پس از ۲ ثانیه بی‌لمسی بسته می‌شود و سر جای قبلی برمی‌گردد.
  * `background-music-full.html` — صفحهٔ کامل که هرگز بسته نمی‌شود، همهٔ منوها
    را دارد و هیچ سقف ارتفاعی ندارد.

هر دو خروجی **خودبسنده** هستند: همهٔ صداها و تصویرها داخل خودشان embed است و
هیچ iframe یا فایل جانبی لازم ندارند.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path

TILE_BOOT = """<script>window.BG_EMBED=true;window.BG_VARIANT='tile';</script>"""
FULL_BOOT = """<script>window.BG_EMBED=true;window.BG_VARIANT='full';</script>"""

TILE_TAIL = """<script>
/* رفتار کادر (tile): تنها منبع تایمر بی‌لمسی و پاپ‌آپ تمام‌صفحه همین‌جاست. */
(function(){
  var IDLE=2000,timer=0,wasOpen=false;
  function isOpen(){return document.body.classList.contains('is-open')}
  function closeNow(){try{window.BackgroundMusic.close()}catch(_){}}
  function arm(){clearTimeout(timer);if(isOpen())timer=setTimeout(closeNow,IDLE)}
  function sync(){
    var open=isOpen();
    if(open!==wasOpen){
      wasOpen=open;
      // در پاپ‌آپ، پخش‌کننده بدون سقف ارتفاع است ولی قابل بستن می‌ماند.
      try{window.BackgroundMusic.setFull(open,false)}catch(_){}
      try{window.HamyarMusicTileHost&&window.HamyarMusicTileHost.setExpanded&&window.HamyarMusicTileHost.setExpanded(open)}catch(_){}
      if(window.parent!==window)window.parent.postMessage({channel:'hamyareman-background-v1',type:'state',opened:open},'*');
    }
    arm();
  }
  ['pointerdown','touchstart','click','input','change','keydown'].forEach(function(name){
    document.addEventListener(name,arm,{capture:true,passive:true});
  });
  try{new MutationObserver(sync).observe(document.body,{attributes:true,attributeFilter:['class']})}catch(_){}
  document.addEventListener('visibilitychange',function(){if(document.hidden)clearTimeout(timer);else arm()});
  sync();
})();
</script>"""

FULL_TAIL = """<script>
/* صفحهٔ full: قفلِ باز. اگر چیزی بیرون آن را ببندد، دوباره باز می‌شود. */
(function(){
  function ensure(){try{if(document.getElementById('backdrop').hidden)window.BackgroundMusic.setFull(true,true)}catch(_){}}
  try{new MutationObserver(ensure).observe(document.body,{attributes:true,attributeFilter:['class']})}catch(_){}
  document.addEventListener('visibilitychange',function(){if(!document.hidden)ensure()});
  ensure();
})();
</script>"""

VARIANTS = {
    "background-music-tile.html": ("انتخاب صدای پس‌زمینه", TILE_BOOT, TILE_TAIL),
    "background-music-full.html": ("نجواهای آرام طبیعت", FULL_BOOT, FULL_TAIL),
}


def build(player: str, title: str, boot: str, tail: str) -> str:
    if "<body>" not in player or "</body>" not in player:
        raise SystemExit("❌ ساختار body در فایل پلیر پیدا نشد.")
    out = re.sub(r"<title>.*?</title>", f"<title>{title}</title>", player, count=1, flags=re.S)
    out = out.replace("<body>", "<body>" + boot, 1)
    out = out.replace("</body>", tail + "</body>", 1)
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--player", default="background-music.html")
    ap.add_argument("--out", default="Bucket/Html-files")
    args = ap.parse_args()

    player = Path(args.player).read_text(encoding="utf-8")
    tracks = player.count('"audio":"')
    if tracks < 1:
        raise SystemExit("❌ در فایل پلیر هیچ صوت embed شده‌ای نیست.")
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, (title, boot, tail) in VARIANTS.items():
        text = build(player, title, boot, tail)
        target = out_dir / name
        target.write_text(text, encoding="utf-8")
        print(f"✓ {target} — {len(text):,} بایت · {text.count(chr(34) + 'audio' + chr(34) + ':' + chr(34))} صدای embed شده")
    print(f"پلیر مرجع: {args.player} — {len(player):,} بایت · {tracks} صدا")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
