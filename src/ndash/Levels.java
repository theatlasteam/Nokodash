package ndash;

/**
 * Main levels only (no editor). Compact code-built patterns on a 24px
 * grid, floor top at y=272. All gaps checked against real jump reach:
 * cube clears ~105px far / ~58px up, ship corridors stay >= 100px.
 */
public final class Levels {

    public static final int BLOCK = 1;
    public static final int SLAB = 2;
    public static final int SPIKE = 3;
    public static final int HSPIKE = 4;
    public static final int SPIKE_D = 5;
    public static final int HSPIKE_D = 6;
    public static final int ORB_Y = 7;
    public static final int ORB_P = 8;
    public static final int PAD = 9;
    public static final int POR_S = 10;
    public static final int POR_C = 11;
    public static final int POR_W = 12;

    public static final int G = 24;
    public static final int FLOOR = 272;

    public static final class Def {
        public String name;
        public int face;
        public int speed;
        public int endX;
        public int[] ox;
        public int[] oy;
        public int[] ow;
        public int[] oh;
        public int[] ot;
        public int n;
    }

    private static final class B {
        int[] ox = new int[1400];
        int[] oy = new int[1400];
        int[] ow = new int[1400];
        int[] oh = new int[1400];
        int[] ot = new int[1400];
        int n;

        void add(int t, int x, int y, int w, int h) {
            if (n >= 1400) return;
            ox[n] = x;
            oy[n] = y;
            ow[n] = w;
            oh[n] = h;
            ot[n] = t;
            n++;
        }

        void blk(int x, int y, int w, int h) {
            add(BLOCK, x, y, w, h);
        }

        void spike(int x) {
            add(SPIKE, x, FLOOR - G, G, G);
        }

        void spikes(int x, int k) {
            for (int i = 0; i < k; i++) spike(x + i * G);
        }

        void hspike(int x) {
            add(HSPIKE, x, FLOOR - G / 2, G, G / 2);
        }

        void wall(int x, int h) {
            blk(x, FLOOR - h * G, G, h * G);
        }

        void stairsUp(int x, int steps) {
            for (int i = 0; i < steps; i++) {
                blk(x + i * G, FLOOR - (i + 1) * G, G, (i + 1) * G);
            }
        }

        void stairsDown(int x, int steps) {
            for (int i = 0; i < steps; i++) {
                blk(x + i * G, FLOOR - (steps - i) * G, G, (steps - i) * G);
            }
        }

        void plat(int x, int y, int cells) {
            blk(x, y, cells * G, G);
        }

        /** Long platform with spikes on top. spikeCells = 0-based cells
         * carrying a spike; keep >= 2 spike-free cells between groups
         * so there is always room to land. */
        void spikeTop(int x, int y, int cells, int[] spikeCells) {
            blk(x, y, cells * G, G);
            for (int i = 0; i < spikeCells.length; i++) {
                int sx = x + spikeCells[i] * G;
                add(SPIKE, sx, y - G, G, G);
            }
        }

        void orbY(int x, int y) {
            add(ORB_Y, x - 11, y - 11, 22, 22);
        }

        void orbP(int x, int y) {
            add(ORB_P, x - 11, y - 11, 22, 22);
        }

        void pad(int x) {
            add(PAD, x, FLOOR - 6, G, 6);
        }

        void ceilRow(int x, int len, int yTop) {
            blk(x, yTop, len, G);
        }

        void hangSpike(int x, int y) {
            add(SPIKE_D, x, y, G, G);
        }

        void portalS(int x, int yMid) {
            add(POR_S, x, 0, G, 320);
            ox[n - 1] = x;
            oy[n - 1] = 0;
            // draw anchor stored via ow/oh unused; game draws ring at yMid:
            ow[n - 1] = yMid;
        }

        void portalC(int x, int yMid) {
            add(POR_C, x, 0, G, 320);
            ow[n - 1] = yMid;
        }

        void portalW(int x, int yMid) {
            add(POR_W, x, 0, G, 320);
            ow[n - 1] = yMid;
        }

        void waveHall(int x, int len, int ceilTop) {
            // enclosed wave hall: block floor + ceiling, 24px grid
            blk(x, FLOOR - G, len, G);
            blk(x, ceilTop, len, G);
        }

        Def done(String name, int face, int speed, int endX) {
            Def d = new Def();
            d.name = name;
            d.face = face;
            d.speed = speed;
            d.endX = endX;
            d.ox = ox;
            d.oy = oy;
            d.ow = ow;
            d.oh = oh;
            d.ot = ot;
            d.n = n;
            return d;
        }
    }

    public static int count() {
        return 3;
    }

    public static Def get(int i) {
        if (i == 0) return level1();
        if (i == 1) return level2();
        return level3();
    }

    private static Def level1() {
        B b = new B();
        b.spike(600);
        b.spikes(900, 2);
        b.wall(1200, 2);
        b.spike(1560);
        b.wall(1850, 2);
        b.spikes(2150, 3);
        b.orbY(2610, 205);
        b.wall(2680, 3);
        b.stairsDown(2704, 3);
        b.pad(3400);
        b.spikes(3460, 3);
        b.spike(3900);
        b.spikes(4150, 2);
        b.spike(4450);
        b.wall(4750, 2);
        b.wall(4950, 2);
        b.orbY(5210, 205);
        b.orbY(5280, 150);
        b.spikes(5200, 5);
        b.hspike(5050);
        b.plat(5700, 224, 3);
        b.spike(6160);
        b.spikes(6450, 3);
        b.orbY(6750, 205);
        b.wall(6820, 3);
        b.plat(7150, 224, 3);
        b.spike(7330);
        b.spikes(7600, 2);
        b.hspike(7820);
        b.spikes(7900, 2);
        b.spike(8200);
        b.pad(8440);
        b.spikes(8500, 3);
        b.spikeTop(8800, 224, 8, new int[]{3, 4});
        return b.done("Stereo Sprint", 0, 250, 9200);
    }

    private static Def level2() {
        B b = new B();
        b.spike(600);
        b.spikes(950, 2);
        b.wall(1300, 2);
        b.wall(1750, 2);
        b.spikes(2050, 2);
        b.orbY(2355, 205);
        b.wall(2430, 3);
        b.portalS(3000, 180);
        // ship corridor: floor spikes, single ceiling row, two pillars
        b.spikes(3250, 90);
        b.ceilRow(3200, 2300, 56);
        b.blk(3900, 80, G, 70);
        b.blk(4600, 80, G, 70);
        b.portalC(5700, 180);
        b.spikes(6200, 2);
        b.stairsUp(6600, 2);
        b.orbY(7260, 205);
        b.orbY(7330, 145);
        b.spikes(7250, 5);
        b.spikeTop(7400, 224, 10, new int[]{2, 3, 6, 7});
        b.wall(7700, 2);
        b.pad(8100);
        b.spikes(8170, 3);
        b.portalS(8800, 170);
        b.spikes(8950, 60);
        b.ceilRow(8900, 1500, 64);
        b.blk(9500, 88, G, 62);
        b.portalC(10550, 170);
        b.spikes(11000, 3);
        b.wall(11400, 2);
        b.wall(11600, 2);
        b.orbP(11900, 200);
        b.wall(11960, 3);
        b.portalW(12180, 170);
        b.waveHall(12180, 1000, 88);
        b.blk(12450, 200, 72, 48);
        b.blk(12800, 112, 96, 24);
        b.portalC(13220, 170);
        return b.done("Neon Voyage", 1, 270, 13600);
    }

    private static Def level3() {
        B b = new B();
        b.spikes(550, 2);
        b.spikes(900, 3);
        b.wall(1250, 2);
        b.spike(1520);
        b.wall(1800, 2);
        b.wall(1872, 2);
        b.spikes(2200, 3);
        b.orbY(2560, 205);
        b.orbY(2630, 140);
        b.spikes(2550, 6);
        b.plat(3050, 224, 2);
        b.spike(3180);
        b.plat(3450, 224, 2);
        b.spikes(4150, 3);
        b.pad(4550);
        b.spikes(4620, 4);
        b.portalS(5200, 170);
        // ship: tighter corridor, hanging spikes
        b.spikes(5350, 80);
        b.ceilRow(5300, 2000, 48);
        b.ceilRow(5300, 2000, 72);
        b.blk(5800, 96, G, 60);
        b.hangSpike(6200, 96);
        b.blk(6600, 96, G, 60);
        b.portalC(7450, 170);
        b.spikes(7900, 3);
        b.wall(8300, 2);
        b.orbP(8620, 200);
        b.wall(8680, 3);
        b.spikes(9100, 2);
        b.spikes(9400, 3);
        b.spikeTop(9550, 224, 10, new int[]{2, 3, 6, 7});
        b.portalS(9900, 170);
        b.spikes(10050, 70);
        b.ceilRow(10000, 1750, 56);
        b.blk(10600, 80, G, 70);
        b.blk(11200, 80, G, 70);
        b.portalC(11900, 170);
        b.wall(12300, 2);
        b.wall(12420, 2);
        b.portalW(12700, 170);
        b.waveHall(12700, 1200, 104);
        b.blk(13200, 128, 24, 60);
        b.blk(13700, 128, 24, 60);
        b.portalC(13950, 170);
        b.orbP(14210, 205);
        b.spikes(14250, 4);
        b.pad(14900);
        b.spikes(14970, 4);
        b.spikes(15400, 3);
        return b.done("Spike Storm", 2, 290, 16100);
    }
}
