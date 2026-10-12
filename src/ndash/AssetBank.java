package ndash;

import n2d.Atlas;
import javax.microedition.lcdui.Image;

/**
 * Shared decoded assets: Menu and DashGame draw from the same Atlas
 * and font maps instead of loading duplicates (the old double-load
 * blew the 6300 heap on level launch). Level bg stays per-game.
 */
public final class AssetBank {

    public static Atlas atlas;
    public static BitmapFont fnt16;
    public static BitmapFont fnt24;
    public static Image logo;
    public static boolean ready;

    public static boolean boot() {
        if (ready) return true;
        try {
            atlas = Atlas.load("/atlas.png", "/atlas.tbl");
            fnt16 = BitmapFont.load("/font16.png", "/font16.tbl");
            fnt24 = BitmapFont.load("/font24.png", "/font24.tbl");
            logo = Image.createImage("/logo.png");
            Kit.refresh(atlas);
            ready = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
