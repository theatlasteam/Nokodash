package ndash;

import javax.microedition.rms.RecordStore;

/**
 * Best % + completions per level + kit, 10 bytes in RMS v2.
 * Falls back to v1 (6 bytes, kit defaults). Silent on failure.
 */
public final class Save {

    public static int[] best = new int[]{0, 0, 0};
    public static int[] wins = new int[]{0, 0, 0};

    public static void load() {
        try {
            RecordStore rs = RecordStore.openRecordStore("nokodash2", true);
            if (rs.getNumRecords() > 0) {
                byte[] b = rs.getRecord(1);
                for (int i = 0; i < 3 && i * 2 + 1 < b.length; i++) {
                    best[i] = b[i * 2] & 255;
                    wins[i] = b[i * 2 + 1] & 255;
                }
                if (b.length >= 10) {
                    Kit.cube = clamp1(b[6]);
                    Kit.ship = clamp1(b[7]);
                    Kit.wave = clamp1(b[8]);
                    Kit.color = b[9] & 7;
                }
            } else {
                rs.addRecord(new byte[10], 0, 10);
            }
            rs.closeRecordStore();
        } catch (Exception e) {
        }
        // v1 fallback for progress only
        if (best[0] == 0 && best[1] == 0 && best[2] == 0) {
            try {
                RecordStore rs = RecordStore.openRecordStore("nokodash1", false);
                byte[] b = rs.getRecord(1);
                for (int i = 0; i < 3 && i * 2 + 1 < b.length; i++) {
                    best[i] = b[i * 2] & 255;
                    wins[i] = b[i * 2 + 1] & 255;
                }
                rs.closeRecordStore();
            } catch (Exception e) {
            }
        }
    }

    private static int clamp1(int v) {
        v = v & 255;
        if (v < 1) return 1;
        if (v > 4) return 4;
        return v;
    }

    public static void store() {
        try {
            RecordStore rs = RecordStore.openRecordStore("nokodash2", true);
            byte[] b = new byte[10];
            for (int i = 0; i < 3; i++) {
                b[i * 2] = (byte) best[i];
                b[i * 2 + 1] = (byte) wins[i];
            }
            b[6] = (byte) Kit.cube;
            b[7] = (byte) Kit.ship;
            b[8] = (byte) Kit.wave;
            b[9] = (byte) (Kit.color & 7);
            if (rs.getNumRecords() > 0) rs.setRecord(1, b, 0, 10);
            else rs.addRecord(b, 0, 10);
            rs.closeRecordStore();
        } catch (Exception e) {
        }
    }
}
