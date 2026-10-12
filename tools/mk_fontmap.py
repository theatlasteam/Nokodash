#!/usr/bin/env python3
"""Pusab bitmap font maps from gdcolon's BMFont data (real GD font with
authentic outline/shadow). Parses art/gjFont00.fnt, downscales glyphs
with Lanczos, repacks into assets/font16 + font24 with advances.

.tbl format per line:  c <id> <x> <y> <w> <h> <xoff> <yoff> <adv>
plus header:          size <lineHeight>
All values device pixels, ints. yoff is relative to line top.
"""
from PIL import Image
import os, re

ROOT = os.path.dirname(os.path.abspath(__file__))
ART = os.path.join(ROOT, "..", "art")
ASSETS = os.path.join(ROOT, "..", "assets")

CHARSET_FULL = ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                "0123456789%:.,!?-+x/()\"'<>=* ")
CHARSET_BIG = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789%!?.,-+/:x() "

SIZES = [(16, "font16", CHARSET_FULL)]
CAP = 78  # source cap/base height in gjFont00


def load_fnt():
    chars = {}
    for line in open(os.path.join(ART, "gjFont00.fnt")):
        m = re.match(r'char id=(\d+)\s+x=(\d+)\s+y=(\d+)\s+width=(-?\d+)'
                     r'\s+height=(-?\d+)\s+xoffset=(-?\d+)\s+yoffset=(-?\d+)'
                     r'\s+xadvance=(\d+)', line)
        if m:
            v = list(map(int, m.groups()))
            chars[v[0]] = v[1:]
    return chars


def pot(v):
    p = 1
    while p < v:
        p *= 2
    return p


def main():
    sheet = Image.open(os.path.join(ART, "gjFont00.png")).convert("RGBA")
    chars = load_fnt()
    for cap, name, charset in SIZES:
        sc = cap / float(CAP)
        missing = [c for c in charset if ord(c) not in chars]
        assert not missing, missing
        glyphs = []
        for ch in charset:
            cid = ord(ch)
            sx, sy, sw, sh, xoff, yoff, adv = chars[cid]
            w = max(1, int(round(sw * sc)))
            h = max(1, int(round(sh * sc)))
            if sw <= 0 or sh <= 0:
                glyphs.append((cid, None, 0, 0,
                               int(round(xoff * sc)), int(round(yoff * sc)),
                               int(round(adv * sc))))
                continue
            g = sheet.crop((sx, sy, sx + sw, sy + sh))
            g = g.resize((w, h), Image.LANCZOS)
            glyphs.append((cid, g, w, h,
                           int(round(xoff * sc)), int(round(yoff * sc)),
                           int(round(adv * sc))))
        glyphs.sort(key=lambda e: -(e[3] if e[1] else 1))
        W = 256
        x = y = row_h = 0
        place = []
        for cid, g, w, h, xoff, yoff, adv in glyphs:
            if g is None:
                place.append((cid, None, 0, 0, 0, 0, xoff, yoff, adv))
                continue
            if x + w > W:
                x = 0
                y += row_h
                row_h = 0
            place.append((cid, g, x, y, w, h, xoff, yoff, adv))
            x += w
            row_h = max(row_h, h)
        H = pot(y + row_h)
        out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        lines = ["size %d" % int(round(130 * sc))]
        for cid, g, px, py, w, h, xoff, yoff, adv in place:
            if g is not None:
                out.alpha_composite(g, (px, py))
            lines.append("c %d %d %d %d %d %d %d %d"
                         % (cid, px, py, w, h, xoff, yoff, adv))
        out.save(os.path.join(ASSETS, name + ".png"), optimize=True)
        open(os.path.join(ASSETS, name + ".tbl"), "w").write("\n".join(lines) + "\n")
        print(name, "%dx%d" % (W, H), len(place), "glyphs")


main()
