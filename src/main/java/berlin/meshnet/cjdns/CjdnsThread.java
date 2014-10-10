package berlin.meshnet.cjdns;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;

public class CjdnsThread implements Runnable
{
    private MeshnetService service;
    private File executable;

    public CjdnsThread(MeshnetService service)
    {
        this.service = service;
        this.executable = new File(service.getApplicationInfo().dataDir, "cjdroute");
    }

    private void writeCjdroute(InputStream cjdroute, File target) throws IOException
    {
        if (target.exists() && target.canExecute()) {
            Log.i("cjdns.CjdnsThread", "cjdroute exists and is executable");
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
            Log.e("cjdns.CjdnsThread", "setExecutable() failed");
            return;
        }

        Log.i("cjdns.CjdnsThread", "Created cjdroute and made it executable");
    }

    @Override
    public void run()
    {
        try {
            writeCjdroute(this.service.cjdroute(), this.executable);

            ProcessBuilder builder = new ProcessBuilder(this.executable.getPath(), "--nobg");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            OutputStream stdin = process.getOutputStream();
            InputStream cjdrouteconf = this.service.cjdrouteconf();

            byte buf[] = new byte[4096];
            int len = cjdrouteconf.read(buf);
            while (len > 0) {
                stdin.write(buf, 0, len);
                len = cjdrouteconf.read(buf);
            }
            stdin.close();

            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader stdout = new BufferedReader(reader);

            int pid = 0;
            AdminAPI admin = this.service.admin();
            while (true) {
                String line = stdout.readLine();
                Log.i("cjdns.CjdnsThread", "cjdroute: " + line);
                if (line == null) {
                    return;
                } else if (pid == 0 && line.contains("Bound to address [" + admin.getBind() + "]")) {
                    // the admin api is ready
                    pid = admin.Core_pid();
                    Log.i("cjdns.CjdnsThread", "Started cjdroute with pid " + pid);
                }
            }
        } catch (IOException e) {
            Log.e("cjdns.CjdnsThread", "IOException: " + e.toString());
        } catch (JSONException e) {
            Log.e("cjdns.CjdnsThread", "JSONException: " + e.toString());
        }
    }
}
