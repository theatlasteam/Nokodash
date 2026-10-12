package ndash;

import javax.microedition.rms.RecordStore;

/** Best % + completions per level, 6 bytes in RMS. Silent on failure. */
public final class Save {

    public static int[] best = new int[]{0, 0, 0};
    public static int[] wins = new int[]{0, 0, 0};

    public static void load() {
        try {
            RecordStore rs = RecordStore.openRecordStore("nokodash1", true);
            if (rs.getNumRecords() > 0) {
                byte[] b = rs.getRecord(1);
                for (int i = 0; i < 3 && i < b.length / 2; i++) {
                    best[i] = b[i * 2] & 255;
                    wins[i] = b[i * 2 + 1] & 255;
                }
            } else {
                rs.addRecord(new byte[6], 0, 6);
            }
            rs.closeRecordStore();
        } catch (Exception e) {
        }
    }

    public static void store() {
        try {
            RecordStore rs = RecordStore.openRecordStore("nokodash1", true);
            byte[] b = new byte[6];
            for (int i = 0; i < 3; i++) {
                b[i * 2] = (byte) best[i];
                b[i * 2 + 1] = (byte) wins[i];
            }
            if (rs.getNumRecords() > 0) rs.setRecord(1, b, 0, 6);
            else rs.addRecord(b, 0, 6);
            rs.closeRecordStore();
        } catch (Exception e) {
        }
    }
}
