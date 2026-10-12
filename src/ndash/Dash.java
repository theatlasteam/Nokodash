package ndash;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

/** Nokodash: Geometry Dash fan game for Java ME. */
public final class Dash extends MIDlet {

    protected void startApp() {
        Menu menu = new Menu(Display.getDisplay(this));
        Display.getDisplay(this).setCurrent(menu);
        try {
            if (menu.boot()) {
                menu.repaint();
                return;
            }
        } catch (Throwable t) {
            showError("menu data: " + shortMsg(t));
            return;
        }
        showError("menu data missing");
    }

    private void showError(String msg) {
        final String m = msg;
        Canvas c = new Canvas() {
            protected void paint(Graphics g) {
                g.setColor(0x550000);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(0xffffff);
                g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,
                        Font.SIZE_SMALL));
                g.drawString("NOKODASH ERR", 120, 120, Graphics.TOP | Graphics.HCENTER);
                g.drawString(m, 120, 145, Graphics.TOP | Graphics.HCENTER);
            }
            protected void keyPressed(int k) {
            }
        };
        Display.getDisplay(this).setCurrent(c);
        c.repaint();
    }

    private static String shortMsg(Throwable t) {
        String m = t.toString();
        if (m.length() > 40) m = m.substring(0, 40);
        return m;
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}
