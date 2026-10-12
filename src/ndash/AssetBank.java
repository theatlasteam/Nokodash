package ndash;

import n2d.Atlas;
import javax.microedition.lcdui.Image;

/**
 * Lazy per-screen assets: the menu set (128x256 sheet + font16 + logo)
 * and the game set (256x512 sheet + fonts + bg) are never resident at
 * the same time. Switching screens frees the old set first, then loads
 * the new one, so peak heap stays far below the 6300 ceiling.
 */
public final class AssetBank {

    public static Atlas menuAtlas;
    public static Atlas gameAtlas;
    public static BitmapFont fnt16;
    public static Image logo;
    public static Image bgtile;
    public static Image strip;

    public static boolean bootMenu() {
        freeGame();
        if (menuAtlas != null) {
            Kit.refreshPrev(menuAtlas);
            return true;
        }
        try {
            menuAtlas = Atlas.load("/atlas_menu.png", "/atlas_menu.tbl");
            if (fnt16 == null) {
                fnt16 = BitmapFont.load("/font16.png", "/font16.tbl");
            }
            if (logo == null) logo = Image.createImage("/logo.png");
            Kit.refreshPrev(menuAtlas);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean bootGame(int level) {
        freeMenu();
        try {
            if (gameAtlas == null) {
                gameAtlas = Atlas.load("/atlas_game.png", "/atlas_game.tbl");
            }
            if (fnt16 == null) {
                fnt16 = BitmapFont.load("/font16.png", "/font16.tbl");
            }
            bgtile = Image.createImage("/bgtile" + level + ".png");
            strip = Image.createImage("/strip" + level + ".png");
            Kit.refreshFull(gameAtlas);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void freeMenu() {
        menuAtlas = null;
        logo = null;
        System.gc();
    }

    public static void freeGame() {
        gameAtlas = null;
        bgtile = null;
        strip = null;
        System.gc();
    }
}
