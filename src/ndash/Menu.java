package ndash;

import n2d.Atlas;
import n2d.Sfx;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Level select: logo, difficulty faces, best %, tap to launch. */
public final class Menu extends Canvas {

    private final Display display;
    private Atlas atlas;
    private Image logo;
    private Font hud;
    private Font big;
    private int sel;
    private int rFace0, rFace1, rFace2;
    private boolean ready;

    public Menu(Display display) {
        this.display = display;
        setFullScreenMode(true);
    }

    public boolean boot() {
        try {
            Save.load();
            atlas = Atlas.load("/atlas.png", "/atlas.tbl");
            logo = Image.createImage("/logo.png");
            rFace0 = atlas.find("face0");
            rFace1 = atlas.find("face1");
            rFace2 = atlas.find("face2");
            hud = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            big = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            ready = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void refresh() {
        Save.load();
        repaint();
    }

    private int faceReg(int i) {
        if (i == 0) return rFace0;
        if (i == 1) return rFace1;
        return rFace2;
    }

    protected void keyPressed(int keyCode) {
        if (!ready) return;
        int ga = 0;
        try {
            ga = getGameAction(keyCode);
        } catch (Exception e) {
        }
        if (ga == UP || keyCode == KEY_NUM2) {
            sel = (sel + 2) % 3;
            Sfx.click();
            repaint();
        } else if (ga == DOWN || keyCode == KEY_NUM8) {
            sel = (sel + 1) % 3;
            Sfx.click();
            repaint();
        } else if (ga == FIRE || keyCode == KEY_NUM5
                || keyCode == KEY_NUM1 || keyCode == KEY_NUM3) {
            int idx = sel;
            if (keyCode == KEY_NUM1) idx = 0;
            if (keyCode == KEY_NUM3) idx = 2;
            launch(idx);
        } else if (keyCode == KEY_NUM0) {
            Sfx.enabled = !Sfx.enabled;
            repaint();
        }
    }

    private void launch(int idx) {
        DashGame g = new DashGame(display, this, idx);
        display.setCurrent(g);
        if (g.boot()) g.start();
        else display.setCurrent(this);
    }

    protected void paint(Graphics g) {
        g.setColor(0x05060f);
        g.fillRect(0, 0, 240, 320);
        if (!ready) return;
        g.drawImage(logo, 12, 14, Graphics.TOP | Graphics.LEFT);
        g.setFont(hud);
        g.setColor(0x8a93b2);
        g.drawString("main levels", 12, 62, Graphics.TOP | Graphics.LEFT);
        for (int i = 0; i < 3; i++) {
            int y = 84 + i * 66;
            Levels.Def lv = Levels.get(i);
            if (i == sel) {
                g.setColor(0x182044);
                g.fillRect(4, y - 4, 232, 60);
                g.setColor(0x50dcff);
                g.drawRect(4, y - 4, 231, 59);
            }
            atlas.draw(g, faceReg(i), 12, y + 26);
            g.setFont(big);
            g.setColor(0xffffff);
            String nm = (i + 1) + ". " + lv.name;
            g.drawString(nm, 64, y, Graphics.TOP | Graphics.LEFT);
            g.setFont(hud);
            String sub = Save.wins[i] > 0 ? "done " + Save.wins[i] + "x"
                    : "best " + Save.best[i] + "%";
            g.setColor(Save.wins[i] > 0 ? 0x7dff9a : 0x8a93b2);
            g.drawString(sub, 64, y + 24, Graphics.TOP | Graphics.LEFT);
        }
        g.setFont(hud);
        g.setColor(0x5a6285);
        g.drawString("2/8 move  5 play  0 sound", 120, 300, Graphics.TOP | Graphics.HCENTER);
    }
}
