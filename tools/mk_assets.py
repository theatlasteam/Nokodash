#!/usr/bin/env python3
"""Nokodash asset pipeline: GD wiki sprites from ~/Downloads + procedural
deco -> downscaled game atlas, per-level backgrounds, menu logo.

Downscale: Lanczos to exact device sizes, 1px atlas padding, baked
rotation/tilt frames (runtime stays pure native blits, no transforms).
"""
from PIL import Image, ImageDraw, ImageOps
import os, shutil

ROOT = os.path.dirname(os.path.abspath(__file__))
DL = "/home/atlas/Downloads"
ART = os.path.join(ROOT, "..", "art")
ASSETS = os.path.join(ROOT, "..", "assets")
os.makedirs(ART, exist_ok=True)
os.makedirs(ASSETS, exist_ok=True)

G = 24  # GD grid cell on device


def src(name):
    p = os.path.join(DL, name)
    shutil.copy(p, os.path.join(ART, name.replace(" ", "_").replace("=", "")))
    return Image.open(p).convert("RGBA")


def down(im, w, h):
    return im.resize((w, h), Image.LANCZOS)


def ring(size, color, width=3):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([1, 1, size - 2, size - 2], outline=color + (255,), width=width)
    d.ellipse([4, 4, size - 5, size - 5], outline=(255, 255, 255, 200), width=1)
    return im


def dot(size, color, ring=False):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if ring:
        d.ellipse([0, 0, size - 1, size - 1], outline=color + (255,))
    else:
        d.ellipse([0, 0, size - 1, size - 1], fill=color + (255,))
        d.ellipse([size // 3, size // 3, size - 1 - size // 3, size - 1 - size // 3],
                  fill=(255, 255, 255, 255))
    return im


sprites = {}  # id -> (image, (ax, ay))

# player cube + 8 baked rotations (24px = one grid cell, center anchor)
cube = down(src("Cube_3.png_v=26"), 24, 24)
for i in range(8):
    r = cube.rotate(-45 * i, resample=Image.BICUBIC, expand=True)
    box = Image.new("RGBA", (36, 36), (0, 0, 0, 0))
    box.alpha_composite(r, ((36 - r.size[0]) // 2, (36 - r.size[1]) // 2))
    sprites["cube%d" % i] = (box, (18, 18))

# ship + 5 baked tilts
ship = src("Ship_166.png_v=26")
ship = ship.resize((34, 10), Image.LANCZOS)
for i, ang in enumerate([-24, -12, 0, 12, 24]):
    r = ship.rotate(ang, resample=Image.BICUBIC, expand=True)
    box = Image.new("RGBA", (38, 22), (0, 0, 0, 0))
    box.alpha_composite(r, ((38 - r.size[0]) // 2, (22 - r.size[1]) // 2))
    sprites["ship%d" % i] = (box, (19, 11))

# blocks & hazards
sprites["block"] = (down(src("Block.webp_v=26"), G, G), (0, 0))
sprites["slab"] = (down(src("Slab.png_v=26"), G, G // 2), (0, 0))
spike = down(src("Spike.png_v=26"), G, G)
sprites["spike"] = (spike, (0, 0))
sprites["spike_d"] = (ImageOps.flip(spike), (0, 0))
hspike = down(src("Half_Spike.png_v=26"), G, G // 2)
sprites["hspike"] = (hspike, (0, 0))
sprites["hspike_d"] = (ImageOps.flip(hspike), (0, 0))
sprites["pad"] = (down(src("Yellow_Pad.png_v=26"), G, 6), (0, 0))
# trail dots: 3 shrink stages per color (smallest is a hollow ring = fade)
for dn, col in [("w", (255, 255, 255)), ("c", (80, 220, 255)), ("m", (240, 120, 250))]:
    sprites["dot_%s6" % dn] = (dot(6, col), (3, 3))
    sprites["dot_%s4" % dn] = (dot(4, col), (2, 2))
    sprites["dot_%s2" % dn] = (dot(3, col, ring=True), (1, 1))

# procedural orbs / portals / trail
sprites["orb_y"] = (ring(22, (250, 204, 21)), (11, 11))
sprites["orb_p"] = (ring(22, (240, 120, 250)), (11, 11))
sprites["por_s"] = (ring(40, (80, 200, 255), width=5), (20, 20))
sprites["por_c"] = (ring(40, (255, 160, 40), width=5), (20, 20))
sprites["dot_w"] = (dot(6, (255, 255, 255)), (3, 3))
sprites["dot_c"] = (dot(6, (80, 220, 255)), (3, 3))
sprites["dot_m"] = (dot(6, (240, 120, 250)), (3, 3))
sprites["spark"] = (dot(5, (255, 240, 180)), (2, 2))

# difficulty faces for level select
for fn, sid in [("Easy_Icon.png_v=26", "face0"), ("Normal_Icon.png_v=26", "face1"),
                ("Hard_Icon.png_v=26", "face2"), ("Harder_Icon.png_v=26", "face3")]:
    sprites[sid] = (down(src(fn), 44, 44), (0, 0))

# ground tiles per level: tinted + bright top edge
def ground(base, tint, edge):
    im = down(base, G, G)
    px = im.load()
    for y in range(G):
        for x in range(G):
            r, g, b, a = px[x, y]
            px[x, y] = (r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, a)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, G - 1, 1], fill=edge + (255,))
    return im

gsq = src("White_Ground_Square.png_v=26")
LEVEL_STYLE = [((40, 60, 140), (90, 220, 255)),
               ((70, 40, 120), (240, 130, 250)),
               ((120, 40, 40), (255, 170, 60))]
for i, (tint, edge) in enumerate(LEVEL_STYLE):
    sprites["ground%d" % i] = (ground(gsq, tint, edge), (0, 0))

# pack (shelf, POT)
PAD = 1
items = sorted(sprites.items(), key=lambda kv: -kv[1][0].size[1])
WIDTH = 256
x = y = row_h = 0
place = []
for sid, (im, anchor) in items:
    w, h = im.size
    if x + w + PAD > WIDTH:
        x = 0
        y += row_h + PAD
        row_h = 0
    place.append((sid, im, anchor, x + PAD, y + PAD))
    x += w + PAD * 2
    row_h = max(row_h, h)


def pot(v):
    p = 1
    while p < v:
        p *= 2
    return p


HEIGHT = pot(y + row_h + PAD * 2)
assert HEIGHT <= 512, HEIGHT
sheet = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
lines = ["N2ATLAS 1", "sheet %d %d" % (WIDTH, HEIGHT)]
for sid, im, anchor, px, py in place:
    sheet.alpha_composite(im, (px, py))
    w, h = im.size
    lines.append("s %s %d %d %d %d %d %d" % (sid, px, py, w, h, anchor[0], anchor[1]))
sheet.save(os.path.join(ASSETS, "atlas.png"), optimize=True)
open(os.path.join(ASSETS, "atlas.tbl"), "w").write("\n".join(lines) + "\n")
print("atlas %dx%d, %d sprites" % (WIDTH, HEIGHT, len(place)))

# per-level backgrounds: gradient + perspective grid + glow sun
BG_STYLE = [((8, 10, 40), (40, 30, 90), (90, 220, 255)),
            ((12, 6, 34), (70, 20, 80), (240, 130, 250)),
            ((30, 8, 10), (90, 30, 20), (255, 170, 60))]
import random
for li, (top, bot, glow) in enumerate(BG_STYLE):
    rnd = random.Random(100 + li)
    bg = Image.new("RGB", (240, 320))
    px = bg.load()
    for yy in range(320):
        t = yy / 319.0
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        for xx in range(240):
            px[xx, yy] = c
    d = ImageDraw.Draw(bg, "RGBA")
    d.ellipse([150, 40, 220, 110], fill=glow)
    d.ellipse([162, 52, 208, 98], fill=tuple(min(255, c + 60) for c in glow))
    for r in range(6):  # ground grid glow lines
        yy = 230 + r * r * 3
        if yy < 320:
            d.line([0, yy, 239, yy], fill=glow + (90,), width=1)
    for i in range(11):
        d.line([120 + (i - 5) * 14, 230, 120 + (i - 5) * 46, 319], fill=glow + (50,))
    for _ in range(40):  # stars
        sx, sy = rnd.randint(0, 239), rnd.randint(0, 200)
        b = rnd.randint(60, 200)
        d.point((sx, sy), fill=(b, b, b + 40))
    bg.save(os.path.join(ASSETS, "bg%d.png" % li), optimize=True)
print("backgrounds done")

# menu logo
logo = src("logo - NOKoDASH.png")
logo.thumbnail((216, 216), Image.LANCZOS)
logo.save(os.path.join(ASSETS, "logo.png"), optimize=True)
print("logo", logo.size)

# icon
icon = Image.new("RGBA", (32, 32), (8, 10, 40, 255))
icon.alpha_composite(down(src("Cube_3.png_v=26"), 26, 26), (3, 3))
icon.save(os.path.join(ASSETS, "icon.png"))
print("icon done")
