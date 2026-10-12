package ndash;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** Nokodash: Geometry Dash fan game for Java ME. */
public final class Dash extends MIDlet {

    protected void startApp() {
        Menu menu = new Menu(Display.getDisplay(this));
        Display.getDisplay(this).setCurrent(menu);
        if (menu.boot()) menu.repaint();
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}
