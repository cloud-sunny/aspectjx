package sun.gradle.plugin.aspectjx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created on 2020/2/10
 *
 * @author WingHawk
 */
public class AJXLog {

    private static boolean enabled;
    private static BufferedWriter writer;
    private static File outFile;

    public synchronized static void init(String filePath) {
        outFile = new File(filePath);
        try {
            writer = new BufferedWriter(new FileWriter(outFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void setEnabled(boolean enabled) {
        AJXLog.enabled = enabled;
    }

    public synchronized static boolean isEnabled() {
        return enabled;
    }

    public synchronized static File getOutFile() {
        return outFile;
    }

    public synchronized static void d(String msg) {
        if (writer == null || !enabled) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String time = sdf.format(new Date());
        try {
            writer.write(time + "  " + msg);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void destroy() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer = null;
        }
    }
}