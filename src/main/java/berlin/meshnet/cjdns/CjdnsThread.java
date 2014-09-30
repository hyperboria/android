package berlin.meshnet.cjdns;

import android.os.Process;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class CjdnsThread implements Runnable {
    private File cjdroute;
    private InputStream cjdrouteconf;
    private TextView logView;
    private java.lang.Process process;

    public CjdnsThread(File cjdroute, InputStream cjdrouteconf, TextView logView)
    {
        this.cjdroute = cjdroute;
        this.cjdrouteconf = cjdrouteconf;
        this.logView = logView;
    }

    /*
     * possible approaches:
     * - kill core/angel via jni
     * - Core_exit() via admin
     * - top-level forwards signals to angel
     * - get rid of top-level
     * - terminal line mode
     */
    public void terminateCjdroute()
    {
        try {
            Field field = this.process.getClass().getDeclaredField("pid");
            Log.i("cjdns_CjdnsThread", "this.process.getClass() = " + this.process.getClass());
            field.setAccessible(true);
            int pid = field.getInt(this.process);

            Log.i("cjdns_CjdnsThread", "Sending TERM to cjdroute pid=" + pid);
            android.os.Process.sendSignal(pid, 15);
            this.process.waitFor();
        } catch (NoSuchFieldException e) {
            Log.e("cjdns_CjdnsThread", "NoSuchFieldException: " + e.getMessage());
            this.process.destroy();
        } catch (IllegalAccessException e) {
            Log.e("cjdns_CjdnsThread", "IllegalAccessException: " + e.getMessage());
            this.process.destroy();
        } catch (InterruptedException e) {
            Log.e("cjdns_CjdnsThread", "InterruptedException: " + e.getMessage());
            this.process.destroy();
        }
    }

    @Override
    public void run()
    {
        ProcessBuilder builder = new ProcessBuilder(cjdroute.getPath());
        builder.redirectErrorStream(true);

        try {
            this.process = builder.start();

            InputStreamReader reader = new InputStreamReader(this.process.getInputStream());
            BufferedReader stdout = new BufferedReader(reader);

            OutputStream stdin = this.process.getOutputStream();
            byte buf[] = new byte[4096];

            int len = cjdrouteconf.read(buf);
            while (len > 0) {
                stdin.write(buf, 0, len);
                len = cjdrouteconf.read(buf);
            }
            stdin.close();

            StringBuilder log = new StringBuilder();

            while (true) {
                String line = stdout.readLine();
                Log.i("cjdns_CjdnsThread", "cjdroute: " + line);
                if (line == null)
                    return;

                // logView.setText(log.append(line + "\n"));
            }
        } catch (IOException e) {
            terminateCjdroute();
            Log.e("cjdns_CjdnsThread", "IOException: " + e.getMessage());
        }
    }
}
