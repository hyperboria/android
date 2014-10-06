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

import berlin.meshnet.cjdns.AdminAPI;

public class CjdnsThread implements Runnable {
    private File cjdroute;
    private InputStream cjdrouteconf;
    private TextView logView;
    private java.lang.Process process;
    private AdminAPI admin;
    private int pid;

    public CjdnsThread(File cjdroute, InputStream cjdrouteconf, TextView logView) throws UnknownHostException
    {
        this.cjdroute = cjdroute;
        this.cjdrouteconf = cjdrouteconf;
        this.logView = logView;

        InetAddress address = InetAddress.getByName("127.0.0.1");
        int port = 11234;
        byte[] password = "8rukfvq3y1d15g1zgtgdnw07f57hvy7".getBytes();
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
            while (true) {
                String line = stdout.readLine();
                Log.i("cjdns_CjdnsThread", "cjdroute: " + line);
                if (line == null) {
                    return;
                } else if (line.contains("Bound to address [127.0.0.1:11234]")) {
                    // the admin api is ready
                    this.pid = this.admin.Core_pid();
                    Log.i("cjdns_CjdnsThread", "Started cjdroute with pid " + this.pid);
                }

                // logView.setText(log.append(line + "\n"));
            }
        } catch (IOException e) {
            terminateCjdroute();
            Log.e("cjdns_CjdnsThread", "IOException: " + e.toString());
        }
    }
}
