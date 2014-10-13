package berlin.meshnet.cjdns;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MeshnetService extends Service
{
    private static final int NOTIFICATION_ID = 1;

    private Notification notification;
    private JSONObject cjdrouteconf;
    private int pid;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate() {
        try {
            this.notification = buildNotification();
            startForeground(NOTIFICATION_ID, notification);

            CjdrouteTask task = new CjdrouteTask() {
                @Override
                protected void onProgressUpdate(String... line) {
                    Log.i("cjdns.MeshnetService", "cjdroute: " + line[0]);
                }

                @Override
                protected void onPostExecute(Integer pid) {
                    MeshnetService.this.pid = pid.intValue();
                    Log.i("cjdns.MeshnetService", "pid: " + pid);
                }
            };
            task.execute(this);
        } catch (IOException e) {
            Log.e("cjdns.MeshnetService", "IOException: " + e.toString());
        } catch (JSONException e) {
            Log.e("cjdns.MeshnetService", "JSONException: " + e.toString());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("cjdns.MeshnetService", "onStartCommand action=" + intent.getAction());
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        Log.i("cjdns.MeshnetService", "onDestroy");
        Process.sendSignal(this.pid, Process.SIGNAL_KILL);
    }

    public InputStream cjdroute() throws IOException
    {
        return getAssets().open(Build.CPU_ABI + "/cjdroute");
    }

    public InputStream cjdrouteconf() throws IOException
    {
        return getAssets().open("cjdroute.conf");
    }

    public JSONObject config() throws IOException, JSONException
    {
        if (this.cjdrouteconf == null) {
            InputStream stream = cjdrouteconf();
            StringBuilder json = new StringBuilder();

            byte[] buf = new byte[1024];
            int len = stream.read(buf);
            while (len > 0) {
                json.append(new String(buf));
                len = stream.read(buf);
            }

            this.cjdrouteconf = (JSONObject) new JSONTokener(json.toString()).nextValue();
            Log.i("cjdns.MeshnetService", "parsed cjdrouteconf");
        }

        return this.cjdrouteconf;
    }

    public AdminAPI admin() throws IOException, JSONException, UnknownHostException
    {
        JSONObject admin = config().getJSONObject("admin");
        String[] bind = admin.getString("bind").split(":");

        InetAddress address = InetAddress.getByName(bind[0]);
        int port = Integer.parseInt(bind[1]);
        byte[] password = admin.getString("password").getBytes();

        return new AdminAPI(address, port, password);
    }

    protected Notification buildNotification() throws IOException, JSONException
    {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        return new Notification.Builder(this)
            .setContentTitle("Meshnet")
            .setContentText(config().getString("ipv6"))
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_launcher)
            // .setLargeIcon(R.drawable.ic_launcher)
            .build();
    }
}
