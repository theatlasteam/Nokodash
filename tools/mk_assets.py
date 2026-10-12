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

# --- customizable kit: REAL GD icons from the iconkit premades
# (art/kit_*.png), grayscale-friendly for runtime tinting.
# Cubes: 24px + 8 baked rotations. Ships: fit 36x14 + 5 tilts.
# Waves: fit 30x22 + 2 tilts.
def kit_src(name):
    p = os.path.join(ART, name)
    return Image.open(p).convert("RGBA")


def bake_rot(im, cell, anchor):
    out = []
    for i in range(8):
        r = im.rotate(-45 * i, resample=Image.BICUBIC, expand=True)
        box = Image.new("RGBA", (cell, cell), (0, 0, 0, 0))
        box.alpha_composite(r, ((cell - r.size[0]) // 2, (cell - r.size[1]) // 2))
        out.append((box, anchor))
    return out


def bake_tilt(im, n, cellw, cellh, anchor, angs):
    out = []
    for ang in angs:
        r = im.rotate(ang, resample=Image.BICUBIC, expand=True)
        box = Image.new("RGBA", (cellw, cellh), (0, 0, 0, 0))
        box.alpha_composite(r, ((cellw - r.size[0]) // 2, (cellh - r.size[1]) // 2))
        out.append((box, anchor))
    return out


for v in range(1, 5):
    cb = down(kit_src("kit_cube_%d.png" % v), 24, 24)
    for i, (im, an) in enumerate(bake_rot(cb, 36, (18, 18))):
        sprites["cb%d_%d" % (v, i)] = (im, an)
    sh = kit_src("kit_ship_%d.png" % v)
    sh.thumbnail((36, 14), Image.LANCZOS)
    for i, (im, an) in enumerate(bake_tilt(sh, 5, 38, 22, (19, 11),
                                           [-24, -12, 0, 12, 24])):
        sprites["sh%d_%d" % (v, i)] = (im, an)
    wv = kit_src("kit_wave_%d.png" % v)
    wv.thumbnail((30, 22), Image.LANCZOS)
    for i, (im, an) in enumerate(bake_tilt(wv, 2, 34, 26, (17, 13), [-18, 18])):
        sprites["wv%d_%d" % (v, i)] = (im, an)

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
sprites["por_w"] = (ring(40, (255, 90, 220), width=5), (20, 20))
sprites["dot_w"] = (dot(6, (255, 255, 255)), (3, 3))
sprites["dot_c"] = (dot(6, (80, 220, 255)), (3, 3))
sprites["dot_m"] = (dot(6, (240, 120, 250)), (3, 3))
sprites["spark"] = (dot(5, (255, 240, 180)), (2, 2))

# difficulty faces for level select
for fn, sid in [("Easy_Icon.png_v=26", "face0"), ("Normal_Icon.png_v=26", "face1"),
                ("Hard_Icon.png_v=26", "face2"), ("Harder_Icon.png_v=26", "face3")]:
    sprites[sid] = (down(src(fn), 44, 44), (0, 0))

# level style colors (bg tile tint, ground tint/edge)
LEVEL_STYLE = [((40, 60, 140), (90, 220, 255)),
               ((70, 40, 120), (240, 130, 250)),
               ((120, 40, 40), (255, 170, 60))]

# wide horizontal ground strips (240x48, single row blit, faint seams)
def ground_strip(base, tint, edge):
    im = base.resize((240, 48), Image.LANCZOS)
    px = im.load()
    for y in range(48):
        for x in range(240):
            r, g, b, a = px[x, y]
            px[x, y] = (r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, a)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 239, 2], fill=edge + (255,))
    for x in range(0, 240, 24):
        d.line([x, 3, x, 47], fill=(edge[0] // 3, edge[1] // 3, edge[2] // 3, 255))
    return im


gsq = src("White_Ground_Square.png_v=26")
for i, (tint, edge) in enumerate(LEVEL_STYLE):
    ground_strip(gsq, tint, edge).save(os.path.join(ASSETS, "strip%d.png" % i),
                                       optimize=True)
print("ground strips done")

# big tiled bg: 128px crops of the gray panel asset, tinted per level
wbg = src("White_Background.png_v=26")
for i, (tint, edge) in enumerate(LEVEL_STYLE):
    t = wbg.crop((192, 192, 320, 320)).copy()
    px = t.load()
    for y in range(128):
        for x in range(128):
            r, g, b, a = px[x, y]
            px[x, y] = (r * tint[0] // 255, g * tint[1] // 255, b * tint[2] // 255, a)
    t.save(os.path.join(ASSETS, "bgtile%d.png" % i), optimize=True)
print("bg tiles done")

# pack (shelf, POT) into per-screen sheets so each screen loads only
# what it needs: menu sheet (faces + kit preview frames) vs game sheet.
PAD = 1

MENU_IDS = set(["face0", "face1", "face2", "face3"])
for v in range(1, 5):
    MENU_IDS.add("cb%d_0" % v)
    MENU_IDS.add("sh%d_2" % v)
    MENU_IDS.add("wv%d_0" % v)
    MENU_IDS.add("cbm%d" % v)


def pack(sub, width, name):
    items = sorted(sub.items(), key=lambda kv: -kv[1][0].size[1])
    x = y = row_h = 0
    place = []
    for sid, (im, anchor) in items:
        w, h = im.size
        if x + w + PAD > width:
            x = 0
            y += row_h + PAD
            row_h = 0
        place.append((sid, im, anchor, x + PAD, y + PAD))
        x += w + PAD * 2
        row_h = max(row_h, h)
    HEIGHT = pot(y + row_h + PAD * 2)
    assert HEIGHT <= 512, (name, HEIGHT)
    sheet = Image.new("RGBA", (width, HEIGHT), (0, 0, 0, 0))
    lines = ["N2ATLAS 1", "sheet %d %d" % (width, HEIGHT)]
    for sid, im, anchor, px, py in place:
        sheet.alpha_composite(im, (px, py))
        w, h = im.size
        lines.append("s %s %d %d %d %d %d %d" % (sid, px, py, w, h, anchor[0], anchor[1]))
    sheet.save(os.path.join(ASSETS, name + ".png"), optimize=True)
    open(os.path.join(ASSETS, name + ".tbl"), "w").write("\n".join(lines) + "\n")
    print("%s %dx%d, %d sprites" % (name, width, HEIGHT, len(place)))


for v in range(1, 5):
    sprites["cbm%d" % v] = (down(kit_src("kit_cube_%d.png" % v), 12, 12), (6, 6))
menu_sprites = dict((k, v) for k, v in sprites.items() if k in MENU_IDS)
game_sprites = dict(sprites)


def pot(v):
    p = 1
    while p < v:
        p *= 2
    return p


pack(menu_sprites, 128, "atlas_menu")
pack(game_sprites, 256, "atlas_game")



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
