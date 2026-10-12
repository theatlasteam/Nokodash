# Nokodash 0.2.0

Geometry Dash fan game for Java ME (CLDC 1.1 / MIDP 2.0), Nokia 6300
target. Cube + ship modes, orbs, pads, portals, trail particles,
3 hand-built main levels, best-% saved to RMS. No level creator (later).

## Play

2/8 or arrows: menu. FIRE / UP / 2 / 5: jump (tap; cube needs re-taps,
no hold-repeat). Hold the same keys to fly the ship. 0: mute. *: menu.

## Tech

Built on the Nitro2D engine (fixed timestep, native blits only, zero
per-frame allocation). Sprites are GD wiki rips, Lanczos-downscaled to
a 24px grid with pre-baked cube rotations and ship tilts, packed into
one 256x256 atlas. Jump arcs were numerically verified against the
actual integration before shipping.

Fan project, not affiliated with RobTop Games. Art belongs to its
respective owners; source in tools/mk_assets.py documents provenance.

## Build

```sh
python3 tools/mk_assets.py  # art -> assets (needs ~/Downloads sprites)
./build.sh                  # dist/Nokodash.jar + .jad, class version 47
```
