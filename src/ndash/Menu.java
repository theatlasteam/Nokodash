package ndash;

import n2d.Atlas;
import n2d.Sfx;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Level select + kit customizer. All text in Pusab bitmap font. */
public final class Menu extends Canvas {

    private final Display display;
    private Atlas atlas;
    private Image logo;
    private BitmapFont fnt16;
    private int sel;
    private int rFace0, rFace1, rFace2;
    private boolean ready;
    private boolean customizing;
    private int crow;

    public Menu(Display display) {
        this.display = display;
        setFullScreenMode(true);
    }

    public boolean boot() {
        Save.load();
        if (!AssetBank.bootMenu()) return false;
        syncBank();
        ready = true;
        repaint();
        return true;
    }

    private void syncBank() {
        atlas = AssetBank.menuAtlas;
        logo = AssetBank.logo;
        fnt16 = AssetBank.fnt16;
        if (atlas != null) {
            rFace0 = atlas.find("face0");
            rFace1 = atlas.find("face1");
            rFace2 = atlas.find("face2");
        }
    }

    /** Return here from the game: reload the menu set, then show. */
    public void show() {
        AssetBank.bootMenu();
        syncBank();
        customizing = false;
        Save.load();
        display.setCurrent(this);
    }

    public void refresh() {
        Save.load();
        if (ready) Kit.refreshPrev(AssetBank.menuAtlas);
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
        boolean up = ga == UP || keyCode == KEY_NUM2;
        boolean down = ga == DOWN || keyCode == KEY_NUM8;
        boolean left = ga == LEFT || keyCode == KEY_NUM4;
        boolean right = ga == RIGHT || keyCode == KEY_NUM6;
        boolean fire = ga == FIRE || keyCode == KEY_NUM5;
        if (customizing) {
            if (up) {
                crow = (crow + 4) % 5;
                Sfx.click();
                repaint();
            } else if (down) {
                crow = (crow + 1) % 5;
                Sfx.click();
                repaint();
            } else if (left || right) {
                if (crow == 4) {
                    customizing = false;
                    repaint();
                    return;
                }
                int d = right ? 1 : -1;
                if (crow == 0) Kit.cube = (Kit.cube - 1 + d + 4) % 4 + 1;
                else if (crow == 1) Kit.ship = (Kit.ship - 1 + d + 4) % 4 + 1;
                else if (crow == 2) Kit.wave = (Kit.wave - 1 + d + 4) % 4 + 1;
                else Kit.color = (Kit.color + d + 8) % 8;
                Kit.refreshPrev(AssetBank.menuAtlas);
                Save.store();
                Sfx.click();
                repaint();
            } else if (fire) {
                if (crow == 4) customizing = false;
                repaint();
            } else if (keyCode == KEY_STAR) {
                customizing = false;
                repaint();
            }
            return;
        }
        if (up) {
            sel = (sel + 3) % 4;
            Sfx.click();
            repaint();
        } else if (down) {
            sel = (sel + 1) % 4;
            Sfx.click();
            repaint();
        } else if (fire || keyCode == KEY_NUM1 || keyCode == KEY_NUM3) {
            int idx = sel;
            if (keyCode == KEY_NUM1) idx = 0;
            if (keyCode == KEY_NUM2) idx = 1;
            if (keyCode == KEY_NUM3) idx = 2;
            if (idx > 2) {
                customizing = true;
                crow = 0;
                repaint();
            } else {
                launch(idx);
            }
        } else if (keyCode == KEY_NUM0) {
            Sfx.enabled = !Sfx.enabled;
            repaint();
        }
    }

    private void launch(int idx) {
        if (!AssetBank.bootGame(idx)) {
            AssetBank.bootMenu();
            syncBank();
            repaint();
            return;
        }
        DashGame g = new DashGame(display, this, idx);
        display.setCurrent(g);
        if (g.boot()) g.start();
        else show();
    }

    protected void paint(Graphics g) {
        g.setColor(0x05060f);
        g.fillRect(0, 0, 240, 320);
        if (!ready) return;
        if (customizing) {
            paintKit(g);
            return;
        }
        g.drawImage(logo, 12, 14, Graphics.TOP | Graphics.LEFT);
        fnt16.draw(g, "main levels", 12, 62, 0);
        for (int i = 0; i < 4; i++) {
            int y = 80 + i * 52;
            if (i == sel) {
                g.setColor(0x182044);
                g.fillRect(4, y - 4, 232, 46);
                g.setColor(0x50dcff);
                g.drawRect(4, y - 4, 231, 45);
            }
            if (i < 3) {
                Levels.Def lv = Levels.get(i);
                atlas.draw(g, faceReg(i), 12, y - 1);
                fnt16.draw(g, (i + 1) + ". " + lv.name, 64, y, 0);
                String sub = Save.wins[i] > 0 ? "done " + Save.wins[i] + "x"
                        : "best " + Save.best[i] + "%";
                fnt16.draw(g, sub, 64, y + 22, 0);
            } else {
                fnt16.draw(g, "4. kustomize", 64, y, 0);
                fnt16.draw(g, "icons + color", 64, y + 22, 0);
            }
        }
        fnt16.draw(g, "v0.3.3 2/8 move 5 play", 120, 300, 1);
    }

    private void paintKit(Graphics g) {
        fnt16.draw(g, "KUSTOMIZE", 120, 28, 1);
        // preview of the focused row's item, tinted live
        Image img = Kit.prevCube();
        int iw = 36;
        int ih = 36;
        if (crow == 1) {
            img = Kit.prevShip();
            iw = 38;
            ih = 22;
        } else if (crow == 2) {
            img = Kit.prevWave();
            iw = 34;
            ih = 26;
        }
        g.setColor(0x10142a);
        g.fillRect(70, 62, 100, 60);
        g.setColor(0x50dcff);
        g.drawRect(70, 62, 99, 59);
        g.drawImage(img, 120 - iw / 2, 92 - ih / 2, Graphics.TOP | Graphics.LEFT);
        if (crow == 1) {
            // ship preview carries the cube inside, like in-game
            g.drawImage(Kit.prevCube(), 120 - 18, 92 - 18,
                    Graphics.TOP | Graphics.LEFT);
        }
        String[] names = {"CUBE", "SHIP", "WAVE", "COLOR", "BACK"};
        for (int i = 0; i < 5; i++) {
            int y = 140 + i * 28;
            if (i == crow) {
                g.setColor(0x182044);
                g.fillRect(4, y - 3, 232, 24);
                g.setColor(0x50dcff);
                g.drawRect(4, y - 3, 231, 23);
            }
            String val = "";
            if (i == 0) val = "< " + Kit.cube + "/4 >";
            else if (i == 1) val = "< " + Kit.ship + "/4 >";
            else if (i == 2) val = "< " + Kit.wave + "/4 >";
            else if (i == 3) val = "< " + (Kit.color + 1) + "/8 >";
            fnt16.draw(g, names[i], 16, y, 0);
            if (i == 3) {
                g.setColor(Kit.primary());
                g.fillRect(150, y, 44, 14);
                g.setColor(0xffffff);
                g.drawRect(150, y, 43, 13);
                fnt16.draw(g, val, 200, y - 1, 0);
            } else if (i < 4) {
                fnt16.draw(g, val, 196, y - 1, 0);
            }
        }
        fnt16.draw(g, "4/6 change  * back", 120, 300, 1);
    }
}
