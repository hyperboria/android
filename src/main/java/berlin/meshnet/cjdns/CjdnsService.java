package berlin.meshnet.cjdns;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Service for managing cjdroute.
 */
public class CjdnsService extends Service {

    /**
     * Log tag.
     */
    private static final String TAG = CjdnsService.class.getSimpleName();

    /**
     * Value that represents an invalid PID.
     */
    private static final int INVALID_PID = Integer.MIN_VALUE;

    /**
     * ID for foreground service {@link Notification}.
     */
    private static final int NOTIFICATION_ID = 1;

    /**
     * List of {@link Subscription}s to unsubscribe {@link #onDestroy()}.
     */
    private List<Subscription> mSubscriptions = new ArrayList<>();

    @Inject
    CjdrouteSubscriber mCjdrouteSubscriber;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Inject dependencies.
        ((CjdnsApplication) getApplication()).inject(this);

        // Start foreground service.
        mSubscriptions.add(CjdrouteConfObservable.just(this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<JSONObject>() {
                    @Override
                    public void call(JSONObject cjdrouteConf) {
                        startForeground(NOTIFICATION_ID, buildNotification(cjdrouteConf));
                    }
                }));

        // Execute cjdroute.
        mSubscriptions.add(CjdrouteConfObservable.just(this)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(mCjdrouteSubscriber.execute()));
    }

    @Override
    public void onDestroy() {
        // Unsubscribe from observables.
        Iterator<Subscription> itr = mSubscriptions.iterator();
        while (itr.hasNext()) {
            itr.next().unsubscribe();
            itr.remove();
        }

        // Kill cjdroute process.
        Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        int pid = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                .getInt(CjdrouteSubscriber.SHARED_PREFERENCES_KEY_CJDROUTE_PID, INVALID_PID);
                        subscriber.onNext(pid);
                        subscriber.onCompleted();
                    }
                })
                .filter(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer pid) {
                        return pid != INVALID_PID;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(mCjdrouteSubscriber.terminate());

        // Stop foreground service.
        stopForeground(true);

        super.onDestroy();
    }

    /**
     * Build foreground service {@link Notification}.
     *
     * @param cjdrouteConf The configurations for cjdroute.
     * @return The {@link Notification}.
     */
    private Notification buildNotification(JSONObject cjdrouteConf) {
        // Get node address.
        String text;
        try {
            text = getString(R.string.cjdns_service_notification_text, cjdrouteConf.getString("ipv6"));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse the address of this node", e);
            text = getString(R.string.cjdns_service_notification_text_missing_address);
        }

        // Create pending intent to launch MainActivity.
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Build notification.
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.cjdns_service_notification_title))
                .setContentText(text)
                .setContentIntent(contentIntent)
                .build();
    }
}
