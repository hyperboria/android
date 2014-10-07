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
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import berlin.meshnet.cjdns.AdminAPI;

public class CjdnsThread implements Runnable {
    private File cjdroute;
    private InputStream cjdrouteconf;
    private JSONObject config;
    private TextView logView;
    private java.lang.Process process;
    private AdminAPI admin;
    private int pid;

    public CjdnsThread(File cjdroute, InputStream cjdrouteconf, TextView logView) throws UnknownHostException, IOException, JSONException
    {
        this.cjdroute = cjdroute;
        this.cjdrouteconf = cjdrouteconf;
        this.logView = logView;

        StringBuilder json = new StringBuilder();
        byte[] buf = new byte[1024];
        int len = cjdrouteconf.read(buf);
        while (len > 0) {
            json.append(new String(buf));
            len = cjdrouteconf.read(buf);
        }
        cjdrouteconf.reset();

        this.config = (JSONObject) new JSONTokener(json.toString()).nextValue();

        JSONObject adminConfig = this.config.getJSONObject("admin");

        // TODO: doesn't support IPv6 addresses like [::1]:11234
        String[] adminBind = adminConfig.getString("bind").split(":");
        InetAddress address = InetAddress.getByName(adminBind[0]);
        int port = Integer.parseInt(adminBind[1]);
        byte[] password = adminConfig.getString("password").getBytes();
        this.admin = new AdminAPI(address, port, password);
    }

    public void terminateCjdroute()
    {
        if (this.pid > 0) {
            Process.sendSignal(this.pid, Process.SIGNAL_KILL);
        }
        return;
    }

    @Override
    public void run()
    {
        ProcessBuilder builder = new ProcessBuilder(cjdroute.getPath(), "--nobg");
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
            String adminBind = this.admin.getBind();
            while (true) {
                String line = stdout.readLine();
                Log.i("cjdns_CjdnsThread", "cjdroute: " + line);
                if (line == null) {
                    return;
                } else if (this.pid == 0 && line.contains("Bound to address [" + adminBind + "]")) {
                    // the admin api is ready
                    this.pid = this.admin.Core_pid();
                    Log.i("cjdns_CjdnsThread", "Started cjdroute with pid " + this.pid);
                    // logView.setText("Started cjdroute with pid " + this.pid);
                }

                // logView.setText(log.append(line + "\n"));
            }
        } catch (IOException e) {
            terminateCjdroute();
            Log.e("cjdns_CjdnsThread", "IOException: " + e.toString());
        }
    }
}
