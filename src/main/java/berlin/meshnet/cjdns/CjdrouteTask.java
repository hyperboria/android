package berlin.meshnet.cjdns;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class CjdrouteTask extends AsyncTask<CjdnsService, String, Integer> {
    public static void writeCjdroute(InputStream cjdroute, File target) throws IOException {
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

    @Override
    protected Integer doInBackground(CjdnsService... service) {
        Integer pid = 0;

        try {
            service[0].cjdrouteconf();
            File executable = new File(service[0].getApplicationInfo().dataDir, "cjdroute");

            Runtime rt = Runtime.getRuntime();
            java.lang.Process proc = rt.exec(executable.getPath());

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            AdminAPI admin = service[0].admin();
            String adminLine = "Bound to address [" + admin.getBind() + "]";

            String line;
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
                publishProgress(line);
                if (line.contains(adminLine)) {
                    pid = admin.Core_pid();
                }
            }
        } catch (IOException e) {
            Log.e("cjdns.CjdrouteTask", "IOException: " + e.toString());
        } catch (JSONException e) {
            Log.e("cjdns.CjdrouteTask", "JSONException: " + e.toString());
        }

        return pid;
    }
}
