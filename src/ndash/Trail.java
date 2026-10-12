package ndash;

import n2d.Particles;

/**
 * Trail emitter: drips particles behind a moving point at a fixed rate,
 * cycling regions for multicolor trails. Thin wrapper over Particles —
 * all storage stays in the pool, this only owns timing + color cycle.
 */
public final class Trail {

    private final Particles parts;
    private final int[] regions;
    private int ri;
    private int acc;
    private final int everyMs;

    public Trail(Particles parts, int[] regions, int everyMs) {
        this.parts = parts;
        this.regions = regions;
        this.everyMs = everyMs < 10 ? 10 : everyMs;
    }

    public void reset() {
        acc = 0;
        ri = 0;
    }

    /**
     * Emit while moving. x/y pixels, back = extra backward drift px/s,
     * spread = position jitter px, life ms, seed varies the jitter.
     */
    public void push(int dtMs, int x, int y, int back, int spread, int life, long seed) {
        acc += dtMs;
        while (acc >= everyMs) {
            acc -= everyMs;
            int r = regions[ri];
            ri++;
            if (ri >= regions.length) ri = 0;
            int jx = 0;
            int jy = 0;
            if (spread > 0) {
                jx = (int) ((seed + ri * 37) % (spread * 2 + 1)) - spread;
                jy = (int) ((seed + ri * 53) % (spread * 2 + 1)) - spread;
            }
            parts.spawn(x + jx, y + jy, -back, -15, life, r);
        }
    }
}
