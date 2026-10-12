package ndash;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Pusab bitmap font blitter. Glyph map built by tools/mk_fontmap.py
 * from gdcolon's BMFont data (authentic outline + shadow).
 * tbl lines: size H / c id x y w h xoff yoff adv. All ints, Java 1.3.
 */
public final class BitmapFont {

    private Image sheet;
    private int[] idx = new int[128];
    private int[] sx;
    private int[] sy;
    private int[] sw;
    private int[] sh;
    private int[] xo;
    private int[] yo;
    private int[] ad;
    private int lineH;

    private BitmapFont() {
        for (int i = 0; i < 128; i++) idx[i] = -1;
    }

    public static BitmapFont load(String pngPath, String tblPath) throws Exception {
        BitmapFont f = new BitmapFont();
        f.sheet = Image.createImage(pngPath);
        InputStream in = "".getClass().getResourceAsStream(tblPath);
        if (in == null) throw new Exception("missing " + tblPath);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        byte[] d = out.toByteArray();
        int[] tmp = new int[96 * 8];
        int ng = 0;
        int s = 0;
        for (int i = 0; i <= d.length; i++) {
            if (i == d.length || d[i] == '\n') {
                int e = i;
                if (e > s && d[e - 1] == '\r') e--;
                if (e - s > 0 && d[s] == 'c' && ng < 96) {
                    int[] p = new int[8];
                    if (parseInts(d, s + 1, e, p, 8) == 8 && p[0] >= 0 && p[0] < 128) {
                        f.idx[p[0]] = ng;
                        for (int k = 0; k < 8; k++) tmp[ng * 8 + k] = p[k];
                        ng++;
                    }
                } else if (e - s > 0 && d[s] == 's') {
                    int[] p = new int[1];
                    if (parseInts(d, s + 4, e, p, 1) == 1) f.lineH = p[0];
                }
                s = i + 1;
            }
        }
        f.sx = new int[ng];
        f.sy = new int[ng];
        f.sw = new int[ng];
        f.sh = new int[ng];
        f.xo = new int[ng];
        f.yo = new int[ng];
        f.ad = new int[ng];
        for (int i = 0; i < ng; i++) {
            f.sx[i] = tmp[i * 8 + 1];
            f.sy[i] = tmp[i * 8 + 2];
            f.sw[i] = tmp[i * 8 + 3];
            f.sh[i] = tmp[i * 8 + 4];
            f.xo[i] = tmp[i * 8 + 5];
            f.yo[i] = tmp[i * 8 + 6];
            f.ad[i] = tmp[i * 8 + 7];
        }
        return f;
    }

    static int parseInts(byte[] d, int s, int e, int[] out, int want) {
        int n = 0;
        int i = s;
        while (i < e && n < want) {
            while (i < e && (d[i] == ' ' || d[i] == '\t')) i++;
            if (i >= e) break;
            boolean neg = false;
            if (d[i] == '-') {
                neg = true;
                i++;
            }
            if (i >= e || d[i] < '0' || d[i] > '9') break;
            int v = 0;
            while (i < e && d[i] >= '0' && d[i] <= '9') {
                v = v * 10 + (d[i] - '0');
                i++;
            }
            out[n++] = neg ? -v : v;
        }
        return n;
    }

    public int lineHeight() {
        return lineH;
    }

    public int width(String str) {
        int w = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 0 || c >= 128 || idx[c] < 0) continue;
            w += ad[idx[c]];
        }
        return w;
    }

    /** align 0 left, 1 center, 2 right around x. */
    public void draw(Graphics g, String str, int x, int yTop, int align) {
        int w = width(str);
        int pen = x;
        if (align == 1) pen = x - w / 2;
        else if (align == 2) pen = x - w;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < 0 || c >= 128 || idx[c] < 0) continue;
            int gi = idx[c];
            if (sw[gi] > 0 && sh[gi] > 0) {
                g.drawRegion(sheet, sx[gi], sy[gi], sw[gi], sh[gi], 0,
                        pen + xo[gi], yTop + yo[gi], Graphics.TOP | Graphics.LEFT);
            }
            pen += ad[gi];
        }
    }
}
