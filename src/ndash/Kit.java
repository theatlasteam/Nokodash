package ndash;

import n2d.Atlas;
import javax.microedition.lcdui.Image;

/**
 * Player kit: chosen icons (1-4 per mode) + primary color, persisted in
 * Save. Grayscale-friendly premade art is tinted once per change into a
 * small frame cache (8 cube + 5 ship + 2 wave); per-frame rendering
 * stays a single native blit. Shared by Menu (preview) and DashGame.
 */
public final class Kit {

    public static final int[] COLORS = {
        0xFFDE00, 0x00E5FF, 0xFF5AA0, 0x39FF6A,
        0xFF8A00, 0xFF3B30, 0xFFFFFF, 0xB45CFF
    };

    public static int cube = 1;
    public static int ship = 1;
    public static int wave = 1;
    public static int color;

    private static Image[] cache = new Image[15];
    private static Image[] prev = new Image[3];
    private static int cachedCube;
    private static int cachedShip;
    private static int cachedWave;
    private static int cachedColor = -1;
    private static int prevCube;
    private static int prevShip;
    private static int prevWave;
    private static int prevColor = -1;

    public static int primary() {
        return COLORS[color & 7];
    }

    /** Luminance tint: white takes full color, gray shades it, black stays. */
    public static Image tint(Image src, int rgb) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = new int[w * h];
        src.getRGB(px, 0, w, 0, 0, w, h);
        int cr = (rgb >> 16) & 255;
        int cg = (rgb >> 8) & 255;
        int cb = rgb & 255;
        for (int i = 0; i < px.length; i++) {
            int a = px[i] >>> 24;
            if (a == 0) continue;
            int lum = (((px[i] >> 16) & 255) + ((px[i] >> 8) & 255)
                    + (px[i] & 255)) / 3;
            px[i] = (a << 24) | (lum * cr / 255 << 16)
                    | (lum * cg / 255 << 8) | (lum * cb / 255);
        }
        return Image.createRGBImage(px, w, h, true);
    }

    private static Image region(Atlas atlas, String id) {
        int idx = atlas.find(id);
        int w = atlas.table.sw[idx];
        int h = atlas.table.sh[idx];
        return Image.createImage(atlas.sheet(),
                atlas.table.sx[idx], atlas.table.sy[idx], w, h, 0);
    }

    /** Rebuild the tinted cache if selection changed. Call on show/boot. */
    public static void refresh(Atlas atlas) {
        refreshFull(atlas);
    }

    /** Full 15-frame cache from the game atlas. */
    public static void refreshFull(Atlas atlas) {
        if (cachedColor == (color & 7) && cachedCube == cube
                && cachedShip == ship && cachedWave == wave
                && cache[0] != null) return;
        int col = primary();
        for (int i = 0; i < 8; i++) {
            cache[i] = tint(region(atlas, "cb" + cube + "_" + i), col);
        }
        for (int i = 0; i < 5; i++) {
            cache[8 + i] = tint(region(atlas, "sh" + ship + "_" + i), col);
        }
        for (int i = 0; i < 2; i++) {
            cache[13 + i] = tint(region(atlas, "wv" + wave + "_" + i), col);
        }
        cachedCube = cube;
        cachedShip = ship;
        cachedWave = wave;
        cachedColor = color & 7;
    }

    /** 3-frame preview cache from the menu atlas (customizer). */
    public static void refreshPrev(Atlas atlas) {
        if (prevColor == (color & 7) && prevCube == cube
                && prevShip == ship && prevWave == wave
                && prev[0] != null) return;
        int col = primary();
        prev[0] = tint(region(atlas, "cb" + cube + "_0"), col);
        prev[1] = tint(region(atlas, "sh" + ship + "_2"), col);
        prev[2] = tint(region(atlas, "wv" + wave + "_0"), col);
        prevCube = cube;
        prevShip = ship;
        prevWave = wave;
        prevColor = color & 7;
    }

    public static Image cubeFrame(int r) {
        return cache[r & 7];
    }

    public static Image shipFrame(int t) {
        if (t < 0) t = 0;
        if (t > 4) t = 4;
        return cache[8 + t];
    }

    public static Image waveFrame(int up) {
        return cache[13 + (up != 0 ? 1 : 0)];
    }

    public static Image prevCube() {
        return prev[0];
    }

    public static Image prevShip() {
        return prev[1];
    }

    public static Image prevWave() {
        return prev[2];
    }
}
