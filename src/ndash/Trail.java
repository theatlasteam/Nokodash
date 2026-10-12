package ndash;

import n2d.Particles;

/**
 * Trail emitter: drips shrinking/fading dots behind a moving point at a
 * fixed rate, cycling color sets. Thin wrapper over Particles — all
 * storage stays in the pool, this only owns timing + color cycle.
 */
public final class Trail {

    private final Particles parts;
    private final int[][] sets;
    private int si;
    private int acc;
    private final int everyMs;

    public Trail(Particles parts, int[][] sets, int everyMs) {
        this.parts = parts;
        this.sets = sets;
        this.everyMs = everyMs < 10 ? 10 : everyMs;
    }

    public void reset() {
        acc = 0;
        si = 0;
    }

    /**
     * Emit while moving. x/y pixels, back = extra backward drift px/s,
     * spread = position jitter px, life ms, seed varies the jitter.
     * Dots shrink through their frame set as they age (fade-out).
     */
    public void push(int dtMs, int x, int y, int back, int spread, int life, long seed) {
        acc += dtMs;
        while (acc >= everyMs) {
            acc -= everyMs;
            int[] set = sets[si];
            si++;
            if (si >= sets.length) si = 0;
            int jx = 0;
            int jy = 0;
            if (spread > 0) {
                jx = (int) ((seed + si * 37) % (spread * 2 + 1)) - spread;
                jy = (int) ((seed + si * 53) % (spread * 2 + 1)) - spread;
            }
            parts.spawnAnim(x + jx, y + jy, -back, -15, life, set);
        }
    }
}
