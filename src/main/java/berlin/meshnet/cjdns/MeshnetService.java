package berlin.meshnet.cjdns;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MeshnetService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification.Builder(this)
            .setContentTitle("Meshnet")
            .setContentText("je suis le contentText")
            .setContentIntent(contentIntent)
            .setSmallIcon(R.drawable.ic_launcher)
            // .setLargeIcon(R.drawable.ic_launcher)
            .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("cjdns.MeshnetService", "onStartCommand action=" + intent.getAction());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("cjdns.MeshnetService", "onDestroy");
    }
}
