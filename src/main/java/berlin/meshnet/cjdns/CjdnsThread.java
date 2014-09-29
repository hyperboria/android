package berlin.meshnet.cjdns;

import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

public class CjdnsThread implements Runnable {
    private File cjdroute;
    private InputStream cjdrouteconf;
    private TextView logView;

    public CjdnsThread(File cjdroute, InputStream cjdrouteconf, TextView logView)
    {
        this.cjdroute = cjdroute;
        this.cjdrouteconf = cjdrouteconf;
        this.logView = logView;
    }

    @Override
    public void run()
    {
        ProcessBuilder builder = new ProcessBuilder(cjdroute.getPath());
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();

            OutputStream stdin = process.getOutputStream();
            // byte buf[] = new byte[4096];

            // int len = cjdrouteconf.read(buf);
            // while (len > 0) {
            //     stdin.write(buf, 0, len);
            //     len = cjdrouteconf.read(buf);
            // }
            stdin.close();

            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader stdout = new BufferedReader(reader);

            StringBuilder log = new StringBuilder();

            while (true) {
                String line = stdout.readLine();
                if (line == null)
                    return;

                logView.setText(log.append(line + "\n"));
            }
        } catch (IOException e) {
            Log.e("cjdns_CjdnsThread", "IOException: " + e.getMessage());
        }
    }
}
