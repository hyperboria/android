package berlin.meshnet.cjdns;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class CjdnsService extends Service {

    private static final String TAG = CjdnsService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 1;

    private JSONObject mCjdrouteConf;

    private int mPid;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            Log.i(TAG, "onStartCommand action=" + intent.getAction());
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            final Notification notification = buildNotification();
            startForeground(NOTIFICATION_ID, notification);

            CjdrouteTask task = new CjdrouteTask() {
                @Override
                protected void onProgressUpdate(String... line) {
                    Log.i(TAG, "cjdroute: " + line[0]);
                }

                @Override
                protected void onPostExecute(Integer pid) {
                    CjdnsService.this.mPid = pid.intValue();
                    Log.i(TAG, "mPid: " + pid);
                }
            };
            task.execute(this);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSONException: " + e.toString());
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        Process.sendSignal(mPid, Process.SIGNAL_KILL);
        stopForeground(true);
        super.onDestroy();
    }

    public InputStream cjdroute() throws IOException {
        return getAssets().open(Build.CPU_ABI + "/cjdroute");
    }

    public InputStream cjdrouteconf() throws IOException {
        //TODO run this stuff separate from UI thread.
        File cjdroute = new File(getApplicationInfo().dataDir + "/files/cjdroute.conf");
        if(cjdroute.exists()) {
            //return stream
            return new FileInputStream(cjdroute);
        } else {
            //create cjdroute.conf and return stream
            File executable = new File(getApplicationInfo().dataDir, "cjdroute");
            CjdrouteTask.writeCjdroute(cjdroute(), executable);

            Runtime rt = Runtime.getRuntime();
            String command = executable.getPath() + " --cleanconf";
            java.lang.Process proc = rt.exec(command);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            String s;
            String output = "";
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                output += s + "\n";
            }

            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(cjdroute.getName(), Context.MODE_PRIVATE);
                outputStream.write(output.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return new FileInputStream(cjdroute);
        }
        //return getAssets().open("cjdroute.conf");
    }

    public JSONObject config() throws IOException, JSONException {
        if (mCjdrouteConf == null) {
            InputStream stream = cjdrouteconf();
            StringBuilder json = new StringBuilder();

            byte[] buf = new byte[1024];
            int len = stream.read(buf);
            while (len > 0) {
                json.append(new String(buf));
                len = stream.read(buf);
            }

            mCjdrouteConf = (JSONObject) new JSONTokener(json.toString()).nextValue();
            Log.i(TAG, "parsed mCjdrouteConf");
        }

        return mCjdrouteConf;
    }

    public AdminAPI admin() throws IOException, JSONException, UnknownHostException {
        JSONObject admin = config().getJSONObject("admin");
        String[] bind = admin.getString("bind").split(":");

        InetAddress address = InetAddress.getByName(bind[0]);
        int port = Integer.parseInt(bind[1]);
        byte[] password = admin.getString("password").getBytes();

        return new AdminAPI(address, port, password);
    }

    private Notification buildNotification() throws IOException, JSONException {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.cjdns_service_notification_title))
                .setContentText(getString(R.string.cjdns_service_notification_text, config().getString("ipv6")))
                .setContentIntent(contentIntent)
                .build();
    }
}
