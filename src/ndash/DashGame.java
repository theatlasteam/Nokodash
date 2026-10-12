package ndash;

import n2d.Atlas;
import n2d.Camera;
import n2d.Game;
import n2d.Particles;
import n2d.Rects;
import n2d.Sfx;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;

/**
 * Nokodash gameplay: cube + ship, orbs/pads/portals, trail particles,
 * attempts/best% with RMS save. Tap FIRE/UP/2/5 to jump, hold to fly
 * the ship, 0 mutes, * aborts to menu.
 */
public final class DashGame extends Game {

    private static final int G = 24;
    private static final int FLOOR = 272;
    private static final int PX = 64;
    private static final int CUBE = 0;
    private static final int SHIP = 1;
    private static final int WAVE = 2;
    private static final int PLAY = 0;
    private static final int DEAD = 1;
    private static final int WIN = 2;
    private static final int JMASK = GameCanvas.UP_PRESSED | GameCanvas.FIRE_PRESSED;

    private final Menu menu;
    private final int levelIdx;
    private Levels.Def lv;

    private Atlas atlas;
    private Image bgtile;
    private Image strip;
    private Camera cam;
    private Particles parts;
    private Trail trail;
    private BitmapFont fnt16;

    private int mode = CUBE;
    private int state = PLAY;
    private int stateT;
    private int wx; // milli-px world x of player left edge reference
    private int py; // milli-px center y
    private int vy; // px/s
    private int angle;
    private long simMs;
    private long queueAt = -99999;
    private boolean keyTap;
    private boolean numHeld;
    private int attempts = 1;
    private int jumps;
    private boolean grounded = true;
    private int portalCool;
    private int[] orbCool;
    private boolean practice;
    private boolean cpSet;
    private int cpX, cpY, cpM;
    private int lastAutoCp;
    private final int[] scratch = new int[4];

    private int rBlock, rSlab, rSpike, rHspike, rSpikeD, rHspikeD;
    private int rOrbY, rOrbP, rPad, rPorS, rPorC, rPorW;
    private int[][] trailSets = new int[3][];

    private boolean ready;

    public DashGame(Display display, Menu menu, int levelIdx) {
        this.menu = menu;
        this.levelIdx = levelIdx;
    }

    public boolean boot() {
        try {
            lv = Levels.get(levelIdx);
            atlas = AssetBank.gameAtlas;
            fnt16 = AssetBank.fnt16;
            bgtile = AssetBank.bgtile;
            strip = AssetBank.strip;
            Kit.refreshFull(atlas);
            rBlock = atlas.find("block");
            rSlab = atlas.find("slab");
            rSpike = atlas.find("spike");
            rHspike = atlas.find("hspike");
            rSpikeD = atlas.find("spike_d");
            rHspikeD = atlas.find("hspike_d");
            rOrbY = atlas.find("orb_y");
            rOrbP = atlas.find("orb_p");
            rPad = atlas.find("pad");
            rPorS = atlas.find("por_s");
            rPorC = atlas.find("por_c");
            rPorW = atlas.find("por_w");
            trailSets[0] = new int[]{atlas.find("dot_c6"), atlas.find("dot_c4"), atlas.find("dot_c2")};
            trailSets[1] = new int[]{atlas.find("dot_w6"), atlas.find("dot_w4"), atlas.find("dot_w2")};
            trailSets[2] = new int[]{atlas.find("dot_m6"), atlas.find("dot_m4"), atlas.find("dot_m2")};
            cam = new Camera(0, 0, 1 << 20, 0);
            parts = new Particles(96);
            parts.gravity = 700;
            trail = new Trail(parts, trailSets, 40);
            orbCool = new int[lv.n];
            for (int i = 0; i < lv.n; i++) orbCool[i] = -99999;
            reset();
            ready = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void reset() {
        wx = 0;
        cpSet = false;
        lastAutoCp = 0;
        mode = CUBE;
        py = (FLOOR - 12) * 1000;
        vy = 0;
        grounded = true;
        angle = 0;
        queueAt = -99999;
        keyTap = false;
        portalCool = 0;
        trail.reset();
    }

    protected void keyPressed(int keyCode) {
        if (keyCode == KEY_NUM0) {
            Sfx.enabled = !Sfx.enabled;
            if (!Sfx.enabled) Sfx.stopMusic();
            return;
        }
        if (keyCode == KEY_STAR) {
            stop();
            menu.show();
            return;
        }
        if (state == WIN) {
            stop();
            menu.show();
            return;
        }
        if (keyCode == KEY_NUM7 && state == PLAY) {
            practice = !practice;
            if (practice) storeCp();
            Sfx.click();
            repaint();
            return;
        }
        if (keyCode == KEY_NUM9 && state == PLAY) {
            storeCp();
            int pcx9 = wx / 1000 + PX;
            parts.burstAnim(pcx9, py / 1000, 6, 100, 350, trailSets[1], simMs);
            Sfx.click();
            repaint();
            return;
        }
        if (keyCode == KEY_NUM2 || keyCode == KEY_NUM5 || gameAct(keyCode) == FIRE) {
            keyTap = true;
            numHeld = true;
        }
    }

    protected void keyReleased(int keyCode) {
        if (keyCode == KEY_NUM2 || keyCode == KEY_NUM5 || gameAct(keyCode) == FIRE) {
            numHeld = false;
        }
    }

    private int gameAct(int keyCode) {
        try {
            return getGameAction(keyCode);
        } catch (Exception e) {
            return 0;
        }
    }

    private int progress() {
        int p = (wx / 1000) * 100 / lv.endX;
        if (p < 0) p = 0;
        if (p > 100) p = 100;
        return p;
    }

    private void die() {
        if (state != PLAY) return;
        state = DEAD;
        stateT = 0;
        int p = progress();
        if (p > Save.best[levelIdx]) {
            Save.best[levelIdx] = p;
            Save.store();
        }
        int cx = wx / 1000 + PX;
        int cy = py / 1000;
        parts.burstAnim(cx, cy, 18, 190, 600, trailSets[1], simMs);
        parts.burstAnim(cx, cy, 10, 120, 500, trailSets[2], simMs + 7);
        Sfx.death();
        cam.shake(12);
    }

    private void win() {
        state = WIN;
        Save.best[levelIdx] = 100;
        Save.wins[levelIdx]++;
        Save.store();
        Sfx.tone(new int[]{72, 4, 76, 4, 79, 4, 84, 12}, 36, 50, 5, 0);
    }

    protected void update() {
        if (!ready) return;
        if (state == WIN) return;
        simMs += SIM_DT;
        if (stateT < 100000) stateT += SIM_DT;
        if (state == DEAD) {
            parts.update(SIM_DT);
            if (stateT > 700) {
                state = PLAY;
                stateT = 0;
                attempts++;
                if (practice && cpSet) {
                    wx = cpX - 24 * 1000;
                    if (wx < 0) wx = 0;
                    py = cpY;
                    mode = cpM;
                    vy = 0;
                    portalCool = 600;
                    lastAutoCp = wx / 1000;
                    trail.reset();
                } else {
                    reset();
                }
            }
            return;
        }
        if (keyTap || hit(JMASK)) {
            keyTap = false;
            queueAt = simMs;
            jumps++;
        }
        boolean tap = simMs - queueAt < 130;
        boolean held = (keys() & JMASK) != 0 || numHeld;
        int stepPx = lv.speed * SIM_DT / 1000;
        wx += stepPx * 1000;

        if (mode == CUBE) {
            if ((tap || held) && grounded) {
                queueAt = -99999;
                vy = -580;
                grounded = false;
                Sfx.jump();
            }
            vy += 2800 * SIM_DT / 1000;
            py += vy * SIM_DT;
            grounded = false;
            if (py >= (FLOOR - 12) * 1000) {
                if (vy > 900) {
                    parts.burstAnim(wx / 1000 + PX, FLOOR - 2, 4, 90, 300,
                            trailSets[0], simMs);
                }
                py = (FLOOR - 12) * 1000;
                vy = 0;
                grounded = true;
                angle = ((angle + 45) / 90) * 90 % 360;
            } else {
                angle = (angle + 880 * SIM_DT / 1000) % 360;
            }
        } else if (mode == SHIP) {
            if (held) vy -= 2600 * SIM_DT / 1000;
            else vy += 1600 * SIM_DT / 1000;
            if (vy < -230) vy = -230;
            if (vy > 230) vy = 230;
            py += vy * SIM_DT;
            if (py < 16 * 1000) {
                py = 16 * 1000;
                if (vy < 0) vy = 0;
            }
        } else {
            vy = held ? -lv.speed : lv.speed;
            py += vy * SIM_DT;
            if (py < 16 * 1000) {
                py = 16 * 1000;
                if (vy < 0) vy = 0;
            }
        }

        int pcx = wx / 1000 + PX;
        int pcy = py / 1000;
        int hw = mode == CUBE ? 11 : mode == SHIP ? 10 : 8;
        int hh = mode == CUBE ? 11 : mode == SHIP ? 4 : 6;
        int prevBottom = (py - vy * SIM_DT) / 1000 + 11;

        // portals first (full-height columns)
        if (portalCool > 0) portalCool -= SIM_DT;
        for (int i = 0; i < lv.n; i++) {
            int t = lv.ot[i];
            if (t != Levels.POR_S && t != Levels.POR_C && t != Levels.POR_W) continue;
            if (pcx + hw > lv.ox[i] && pcx - hw < lv.ox[i] + 24 && portalCool <= 0) {
                int nm = mode;
                if (t == Levels.POR_S && mode != SHIP) nm = SHIP;
                else if (t == Levels.POR_C && mode != CUBE) nm = CUBE;
                else if (t == Levels.POR_W && mode != WAVE) nm = WAVE;
                if (nm != mode) {
                    mode = nm;
                    vy = 0;
                    portalCool = 600;
                    parts.burstAnim(pcx, pcy, 10, 130, 450, trailSets[0], simMs);
                    Sfx.click();
                }
            }
        }

        // solids + hazards near the player
        for (int i = 0; i < lv.n; i++) {
            int t = lv.ot[i];
            if (t != Levels.BLOCK && t != Levels.SLAB && t != Levels.SPIKE
                    && t != Levels.HSPIKE && t != Levels.SPIKE_D
                    && t != Levels.HSPIKE_D) continue;
            if (lv.ox[i] > pcx + 60 || lv.ox[i] + lv.ow[i] < pcx - 60) continue;
            if (t == Levels.BLOCK || t == Levels.SLAB) {
                if (!Rects.overlap(pcx - hw, pcy - hh, hw * 2, hh * 2,
                        lv.ox[i], lv.oy[i], lv.ow[i], lv.oh[i])) continue;
                if (mode == CUBE && vy > 0 && prevBottom <= lv.oy[i] + 8) {
                    py = (lv.oy[i] - 12) * 1000;
                    vy = 0;
                    grounded = true;
                    angle = ((angle + 45) / 90) * 90 % 360;
                } else {
                    die();
                    return;
                }
            } else if (spikeRect(t, lv.ox[i], lv.oy[i], lv.ow[i], lv.oh[i], scratch)) {
                if (Rects.overlap(pcx - hw, pcy - hh, hw * 2, hh * 2,
                        scratch[0], scratch[1], scratch[2], scratch[3])) {
                    die();
                    return;
                }
            }
        }

        // ship/wave floor is lava (cube lands via blocks above)
        if ((mode == SHIP || mode == WAVE) && pcy + hh >= FLOOR) {
            die();
            return;
        }

        // orbs + pads
        for (int i = 0; i < lv.n; i++) {
            int t = lv.ot[i];
            if (t != Levels.ORB_Y && t != Levels.ORB_P && t != Levels.PAD) continue;
            int ocx = lv.ox[i] + lv.ow[i] / 2;
            int ocy = lv.oy[i] + lv.oh[i] / 2;
            int dx = pcx - ocx;
            if (dx < 0) dx = -dx;
            int dy = pcy - ocy;
            if (dy < 0) dy = -dy;
            if (t == Levels.PAD) {
                if (dx < 20 && dy < 16 && simMs - orbCool[i] > 300) {
                    orbCool[i] = (int) simMs;
                    vy = -760;
                    if (mode == SHIP) vy = -560;
                    parts.burstAnim(ocx, ocy, 6, 110, 350, trailSets[1], simMs);
                    Sfx.click();
                }
                continue;
            }
            if (dx < 24 && dy < 24 && (tap || held) && simMs - orbCool[i] > 300) {
                queueAt = -99999;
                orbCool[i] = (int) simMs;
                if (mode == CUBE) vy = (t == Levels.ORB_Y) ? -620 : -800;
                else vy = (t == Levels.ORB_Y) ? -520 : -650;
                parts.burstAnim(ocx, ocy, 8, 130, 400, trailSets[2], simMs + i);
                Sfx.coin();
            }
        }

        if (practice && wx / 1000 - lastAutoCp >= 400) {
            int hwA = mode == CUBE ? 11 : mode == SHIP ? 10 : 8;
            int hhA = mode == CUBE ? 11 : mode == SHIP ? 4 : 6;
            int pcxA = wx / 1000 + PX;
            if (!dangerAt(pcxA, pcy, hwA, hhA)) {
                cpX = wx;
                cpY = py;
                cpM = mode;
                cpSet = true;
            }
            lastAutoCp = wx / 1000;
        }

        // trail + win
        int feet = mode == CUBE ? pcy + 11 : mode == SHIP ? pcy + 4 : pcy;
        trail.push(SIM_DT, pcx - 8, feet, 60, 4, 450, simMs);
        cam.x = wx / 1000;
        cam.tick();
        parts.update(SIM_DT);
        if (wx / 1000 >= lv.endX) win();
    }

    /** Kill rect for spike types into out[4]. False for non-spikes. */
    private static boolean spikeRect(int t, int ox, int oy, int ow, int oh, int[] out) {
        if (t == Levels.SPIKE) {
            out[0] = ox + 9; out[1] = oy + 8; out[2] = 6; out[3] = 16;
        } else if (t == Levels.HSPIKE) {
            out[0] = ox + 9; out[1] = oy + 4; out[2] = 6; out[3] = 8;
        } else if (t == Levels.SPIKE_D) {
            out[0] = ox + 9; out[1] = oy + 2; out[2] = 6; out[3] = 14;
        } else if (t == Levels.HSPIKE_D) {
            out[0] = ox + 9; out[1] = oy + 2; out[2] = 6; out[3] = 8;
        } else return false;
        return true;
    }

    /** True if the box touches anything lethal (checkpoint must be safe). */
    private boolean dangerAt(int pcx, int pcy, int hw, int hh) {
        for (int i = 0; i < lv.n; i++) {
            int t = lv.ot[i];
            if (t == Levels.BLOCK || t == Levels.SLAB) {
                if (Rects.overlap(pcx - hw, pcy - hh, hw * 2, hh * 2,
                        lv.ox[i], lv.oy[i], lv.ow[i], lv.oh[i])) return true;
            } else if (spikeRect(t, lv.ox[i], lv.oy[i], lv.ow[i], lv.oh[i], scratch)) {
                if (Rects.overlap(pcx - hw, pcy - hh, hw * 2, hh * 2,
                        scratch[0], scratch[1], scratch[2], scratch[3])) return true;
            }
        }
        return false;
    }

    /** Store a checkpoint if the spot is safe; always rearms the timer. */
    private void storeCp() {
        int pcx = wx / 1000 + PX;
        int pcy = py / 1000;
        int hw = mode == CUBE ? 11 : mode == SHIP ? 10 : 8;
        int hh = mode == CUBE ? 11 : mode == SHIP ? 4 : 6;
        if (!dangerAt(pcx, pcy, hw, hh)) {
            cpX = wx;
            cpY = py;
            cpM = mode;
            cpSet = true;
        }
        lastAutoCp = wx / 1000;
    }

    private int regionFor(int t) {
        if (t == Levels.BLOCK) return rBlock;
        if (t == Levels.SLAB) return rSlab;
        if (t == Levels.SPIKE) return rSpike;
        if (t == Levels.HSPIKE) return rHspike;
        if (t == Levels.SPIKE_D) return rSpikeD;
        if (t == Levels.HSPIKE_D) return rHspikeD;
        if (t == Levels.ORB_Y) return rOrbY;
        if (t == Levels.ORB_P) return rOrbP;
        if (t == Levels.PAD) return rPad;
        if (t == Levels.POR_S) return rPorS;
        if (t == Levels.POR_W) return rPorW;
        return rPorC;
    }

    protected void render(Graphics g) {
        if (!ready) {
            g.setColor(0x05060f);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        int camPx = state == PLAY ? wx / 1000 : cam.x;
        int shx = cam.shakeX(simMs);
        int shy = cam.shakeY(simMs);
        // tiled gray bg, mirror-wrapped for seamlessness, slow parallax
        int scroll = camPx / 6;
        int x0 = -(scroll % 256);
        for (int tx = x0; tx < 240; tx += 128) {
            int ix = ((tx + scroll + 262144) / 128) & 1;
            for (int ty = -128; ty < 320; ty += 128) {
                int iy = (ty / 128) & 1;
                int tr;
                if (ix == 0) tr = iy == 0 ? Sprite.TRANS_NONE : Sprite.TRANS_MIRROR_ROT180;
                else tr = iy == 0 ? Sprite.TRANS_MIRROR : Sprite.TRANS_ROT180;
                g.drawRegion(bgtile, 0, 0, 128, 128, tr,
                        tx + shx, ty + shy, Graphics.TOP | Graphics.LEFT);
            }
        }
        // one wide ground strip, wrapped (no tile grid)
        int off = camPx % 240;
        g.drawImage(strip, -off + shx, FLOOR + shy, Graphics.TOP | Graphics.LEFT);
        g.drawImage(strip, -off + 240 + shx, FLOOR + shy, Graphics.TOP | Graphics.LEFT);
        // objects
        for (int i = 0; i < lv.n; i++) {
            int t = lv.ot[i];
            int sx = lv.ox[i] - camPx + shx;
            if (sx < -80 || sx > 320) continue;
            if (t == Levels.BLOCK) {
                for (int bx = 0; bx < lv.ow[i]; bx += G) {
                    for (int by = 0; by < lv.oh[i]; by += G) {
                        atlas.draw(g, rBlock, sx + bx, lv.oy[i] + by + shy);
                    }
                }
            } else if (t == Levels.SLAB || t == Levels.SPIKE || t == Levels.HSPIKE
                    || t == Levels.SPIKE_D || t == Levels.HSPIKE_D || t == Levels.PAD) {
                atlas.draw(g, regionFor(t), sx, lv.oy[i] + shy);
            } else if (t == Levels.ORB_Y || t == Levels.ORB_P) {
                atlas.draw(g, regionFor(t),
                        sx + lv.ow[i] / 2, lv.oy[i] + lv.oh[i] / 2 + shy);
            } else {
                // portals: ring art centered on stored mid height
                atlas.draw(g, regionFor(t), sx + 12, lv.ow[i] + shy);
            }
        }
        // finish bars
        int fx = lv.endX - camPx + shx;
        if (fx > -20 && fx < 260) {
            g.setColor(0xffffff);
            for (int k = 0; k < 3; k++) {
                g.fillRect(fx + k * 4, 40 + shy, 2, FLOOR - 40);
            }
        }
        // particles under player, player on top
        if (state != WIN) {
            parts.render(g, atlas, camPx - shx, -shy);
        }
        if (state != DEAD) {
            int pcy = py / 1000 + shy;
            if (mode == CUBE) {
                int f = ((angle % 360) + 360) % 360;
                f = (f + 22) / 45 % 8;
                g.drawImage(Kit.cubeFrame(f), PX + shx - 18, pcy - 18,
                        Graphics.TOP | Graphics.LEFT);
            } else if (mode == SHIP) {
                int ti = (230 - vy) * 4 / 460;
                if (ti < 0) ti = 0;
                if (ti > 4) ti = 4;
                g.drawImage(Kit.cargoFrame(Kit.cube), PX + shx - 6, pcy - 12,
                        Graphics.TOP | Graphics.LEFT);
                g.drawImage(Kit.shipFrame(ti), PX + shx - 19, pcy - 11,
                        Graphics.TOP | Graphics.LEFT);
            } else {
                g.drawImage(Kit.waveFrame(vy < 0 ? 1 : 0), PX + shx - 17, pcy - 13,
                        Graphics.TOP | Graphics.LEFT);
            }
        }
        // HUD in Pusab
        int pct = progress();
        fnt16.draw(g, pct + "%", 120, 2, 1);
        g.setColor(0x3a3f5a);
        g.fillRect(70, 20, 100, 4);
        g.setColor(0x50dcff);
        g.fillRect(70, 20, pct, 4);
        fnt16.draw(g, "TRY " + attempts, 4, 2, 0);
        if (practice) fnt16.draw(g, "PRAC", 236, 2, 2);
        if (practice && cpSet) {
            int mx = cpX / 1000 - camPx + shx;
            int my = cpY / 1000 + shy;
            if (mx > -10 && mx < 250) {
                g.setColor(0x50dcff);
                g.fillTriangle(mx, my - 7, mx - 5, my, mx + 5, my);
                g.fillTriangle(mx, my + 7, mx - 5, my, mx + 5, my);
            }
        }
        if (state == WIN) {
            int pw = 200;
            int ph = 120;
            int px = (240 - pw) / 2;
            int py2 = (320 - ph) / 2;
            g.setColor(0x0a0e22);
            g.fillRect(px, py2, pw, ph);
            g.setColor(0x50dcff);
            g.drawRect(px, py2, pw - 1, ph - 1);
            fnt16.draw(g, "COMPLETE!", 120, py2 + 10, 1);
            fnt16.draw(g, lv.name.toUpperCase(), 120, py2 + 44, 1);
            fnt16.draw(g, "jumps " + jumps + " tries " + attempts, 120, py2 + 64, 1);
            fnt16.draw(g, "best " + Save.best[levelIdx] + "%", 120, py2 + 82, 1);
            fnt16.draw(g, "FIRE: levels", 120, py2 + 100, 1);
        }
    }
}
