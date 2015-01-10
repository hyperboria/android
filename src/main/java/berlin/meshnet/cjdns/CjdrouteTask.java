package berlin.meshnet.cjdns;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;

public class CjdrouteTask extends AsyncTask<CjdnsService, String, Integer> {
    @Override
    protected Integer doInBackground(CjdnsService... service) {
        Integer pid = new Integer(0);

        try {
            File executable = new File(service[0].getApplicationInfo().dataDir, "cjdroute");
            writeCjdroute(service[0].cjdroute(), executable);

            ProcessBuilder builder = new ProcessBuilder(executable.getPath());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            writeCjdrouteconf(service[0].cjdrouteconf(), process.getOutputStream());

            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader stdout = new BufferedReader(reader);

            AdminAPI admin = service[0].admin();
            String adminLine = "Bound to address [" + admin.getBind() + "]";
            while (true) {
                String line = stdout.readLine();
                publishProgress(line);
                if (line == null) {
                    break;
                } else if (line.contains(adminLine)) {
                    pid = new Integer(admin.Core_pid());
                }
            }
        } catch (IOException e) {
            Log.e("cjdns.CjdrouteTask", "IOException: " + e.toString());
        } catch (JSONException e) {
            Log.e("cjdns.CjdrouteTask", "JSONException: " + e.toString());
        }

        return pid;
    }

    private void writeCjdroute(InputStream cjdroute, File target) throws IOException
    {
        if (target.exists() && target.canExecute()) {
            Log.i("cjdns.CjdrouteTask", "cjdroute exists and is executable");
            return;
        }

        FileOutputStream file = new FileOutputStream(target);
        byte buf[] = new byte[4096];

        int len = cjdroute.read(buf);
        while (len > 0) {
            file.write(buf, 0, len);
            len = cjdroute.read(buf);
        }
        file.close();

        if (!target.setExecutable(true)) {
            Log.e("cjdns.CjdrouteTask", "setExecutable() failed");
            return;
        }

        Log.i("cjdns.CjdrouteTask", "Created cjdroute and made it executable");
    }

    private void writeCjdrouteconf(InputStream cjdrouteconf, OutputStream stdin) throws IOException {
        byte buf[] = new byte[4096];
        int len = cjdrouteconf.read(buf);
        while (len > 0) {
            stdin.write(buf, 0, len);
            len = cjdrouteconf.read(buf);
        }
        stdin.close();
    }
}
