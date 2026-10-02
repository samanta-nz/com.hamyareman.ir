# -*- coding: utf-8 -*-
"""رندر دقیق هر ۴ فونت با raqm (شکل‌دهی صحیح فارسی) + شیت مقایسه"""
import os
from PIL import Image, ImageDraw, ImageFont

FONTS = [
    ("Dabir", "/home/user/fonts/Dabir.ttf"),
    ("Dabir-Maryam-Soft", "/home/user/fonts/Dabir-Maryam-Soft.ttf"),
    ("Hilda", "/home/user/fonts/Hilda.ttf"),
    ("Badkhat", "/home/user/fonts/Badkhat.ttf"),
]
OUT = "/home/user/font-specimens"
ROW1 = "ا ب پ ت ث ج چ ح خ د ذ ر ز ژ س ش"
ROW2 = "ص ض ط ظ ع غ ف ق ک گ ل م ن و ه ی"
LINE1 = "به نام خداوند جان و خرد"
LINE2 = "هرگز نمیرد آنکه دلش زنده شد به عشق"
WORD = "بنویس"

def render(name, path):
    W, H = 1400, 1000
    img = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(img)
    lbl = ImageFont.load_default()
    f_big = ImageFont.truetype(path, 130)
    f_mid = ImageFont.truetype(path, 62)
    f_txt = ImageFont.truetype(path, 58)
    d.rectangle([0, 0, W, 80], fill=(124, 92, 214))
    d.text((W // 2, 40), name, font=lbl, fill="white", anchor="mm")
    y = 150
    for t in (ROW1, ROW2):
        d.text((W // 2, y), t, font=f_mid, fill=(35, 35, 46), anchor="ma",
               direction="rtl", language="fa")
        y += 120
    d.line([60, y - 10, W - 60, y - 10], fill=(200, 200, 210), width=2)
    for t in (LINE1, LINE2):
        d.text((W // 2, y + 40), t, font=f_txt, fill=(35, 35, 46), anchor="ma",
               direction="rtl", language="fa")
        y += 115
    d.line([60, y + 10, W - 60, y + 10], fill=(200, 200, 210), width=2)
    d.text((W // 2, y + 120), WORD, font=f_big, fill=(124, 92, 214), anchor="ma",
           direction="rtl", language="fa")
    fp = f"{OUT}/{name}.png"
    img.save(fp)
    print(fp)
    return fp

files = [render(n, p) for n, p in FONTS]
ims = [Image.open(f) for f in files]
sheet = Image.new("RGB", (1400, 1000 * len(ims)), "white")
for i, im in enumerate(ims):
    sheet.paste(im, (0, i * 1000))
sheet.save(f"{OUT}/compare.png")
print(f"{OUT}/compare.png", sheet.size)
