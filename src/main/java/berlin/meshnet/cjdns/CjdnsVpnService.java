package berlin.meshnet.cjdns;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import berlin.meshnet.cjdns.event.ApplicationEvents;
import berlin.meshnet.cjdns.model.Node;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class CjdnsVpnService extends VpnService {

    private static final String TAG = CjdnsVpnService.class.getSimpleName();

    /**
     * The VPN session name.
     */
    private static final String SESSION_NAME = "VPN over cjdns";

    /**
     * The maximum transmission unit for the VPN interface.
     */
    private static final int MTU = 1304;

    /**
     * Route for cjdns addresses.
     */
    private static final String CJDNS_ROUTE = "fc00::";

    /**
     * Default route for the VPN interface. A default route is needed for some applications to work.
     */
    private static final String DEFAULT_ROUTE = "::";

    /**
     * DNS server for the VPN connection. We must set a DNS server for Lollipop devices to work.
     */
    private static final String DNS_SERVER = "8.8.8.8";

    /**
     * Path to a transient named pipe for sending a message from the Java process to the native
     * process. The VPN interface file descriptor is translated by the kernel across this named pipe.
     */
    private static final String SEND_FD_PIPE_PATH_TEMPLATE = "%1$s/pipe_%2$s";

    /**
     * VPN interface.
     */
    private ParcelFileDescriptor mInterface;

    @Inject
    Bus mBus;

    @Inject
    Cjdroute mCjdroute;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Inject dependencies.
        ((CjdnsApplication) getApplication()).inject(this);

        // Register so we can subscribe to stop events.
        mBus.register(this);

        // Start cjdns process and VPN.
        final String pipePath = String.format(Locale.ENGLISH, SEND_FD_PIPE_PATH_TEMPLATE,
                getFilesDir().getPath(), UUID.randomUUID());
        try {
            final AdminApi api = new AdminApi();
            // TODO Move UDP interface adding to separate place.
            CjdrouteConf.fetch0(this)
                    .flatMap(new Func1<Node.Me, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Node.Me me) {
                            return mCjdroute.start();
                        }
                    })
                    .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Boolean isSuccessful) {
                            return AdminApi.Security.setupComplete(api);
                        }
                    })
                    .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Boolean isSuccessful) {
                            return close();
                        }
                    })
                    .flatMap(new Func1<Boolean, Observable<Long>>() {
                        @Override
                        public Observable<Long> call(Boolean isSuccessful) {
                            return AdminApi.UdpInterface.new0(api);
                        }
                    })
                    .flatMap(new Func1<Long, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Long udpInterfaceNumber) {
                            return AdminApi.UdpInterface.beginConnection(api,
                                    "1941p5k8qqvj17vjrkb9z97wscvtgc1vp8pv1huk5120cu42ytt0.k",
                                    "104.200.29.163:53053",
                                    udpInterfaceNumber,
                                    null,
                                    "8fVMl0oo6QI6wKeMneuY26x1MCgRemg");
                        }
                    })
                    .flatMap(new Func1<Boolean, Observable<Long>>() {
                        @Override
                        public Observable<Long> call(Boolean isSuccessful) {
                            return AdminApi.UdpInterface.new0(api);
                        }
                    })
                    .flatMap(new Func1<Long, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Long udpInterfaceNumber) {
                            return AdminApi.UdpInterface.beginConnection(api,
                                    "2scyvybg4qqms1c5c9nyt50b1cdscxnr6ycpwsxf6pccbmwuynk0.k",
                                    "159.203.5.91:30664",
                                    udpInterfaceNumber,
                                    "android-public",
                                    "kj1rur4buavtyp2mavch5nghsnd4bpf");
                        }
                    })
                    .flatMap(new Func1<Boolean, Observable<Node.Me>>() {
                        @Override
                        public Observable<Node.Me> call(Boolean isSuccessful) {
                            return CjdrouteConf.fetch0(CjdnsVpnService.this);
                        }
                    })
                    .flatMap(new Func1<Node.Me, Observable<Integer>>() {
                        @Override
                        public Observable<Integer> call(final Node.Me me) {
                            return Observable.create(new Observable.OnSubscribe<Integer>() {
                                @Override
                                public void call(Subscriber<? super Integer> subscriber) {
                                    // Start new session.
                                    mInterface = new Builder()
                                            .setSession(SESSION_NAME)
                                            .setMtu(MTU)
                                            .addAddress(me.address, 8)
                                            .addRoute(CJDNS_ROUTE, 8)
                                            .addRoute(DEFAULT_ROUTE, 0)
                                            .addDnsServer(DNS_SERVER)
                                            .establish();
                                    subscriber.onNext(mInterface.getFd());
                                    subscriber.onCompleted();
                                }
                            });
                        }
                    })
                    .flatMap(new Func1<Integer, Observable<Map<String, Long>>>() {
                        @Override
                        public Observable<Map<String, Long>> call(Integer fd) {
                            // Send VPN interface file descriptor through the pipe after
                            // AdminApi.FileNo.import0() constructs that pipe.
                            FileDescriptorSender.send(pipePath, fd)
                                    .delaySubscription(3L, TimeUnit.SECONDS)
                                    .subscribe(new Action1<Boolean>() {
                                        @Override
                                        public void call(Boolean isSuccessful) {
                                            Log.i(TAG, "VPN interface file descriptor imported to native process");
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            Log.e(TAG, "Failed to import VPN interface file descriptor to native process", throwable);
                                        }
                                    });

                            // Start named pipe to import VPN interface file descriptor.
                            return AdminApi.FileNo.import0(api, pipePath);
                        }
                    })
                    .flatMap(new Func1<Map<String, Long>, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Map<String, Long> file) {
                            return AdminApi.Core.initTunfd(api, file.get("tunfd"), file.get("type"));
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                            new Action1<Boolean>() {
                                @Override
                                public void call(Boolean isSuccessful) {
                                    Log.i(TAG, "TUN interface initialized");
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    Log.e(TAG, "Failed to initialize TUN interface", throwable);
                                }
                            });
        } catch (UnknownHostException e) {
            Log.e(TAG, "Failed to start AdminApi", e);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Unregister from bus.
        mBus.unregister(this);
    }

    @Subscribe
    public void handleEvent(ApplicationEvents.StopCjdnsService event) {
        close().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        stopSelf();
                    }
                });

        // TODO Kill native process if VPN service is closed in some other way.
    }

    /**
     * Closes any existing session.
     */
    private Observable<Boolean> close() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                // Close VPN interface.
                if (mInterface != null) {
                    try {
                        mInterface.close();
                    } catch (IOException e) {
                        // Do nothing.
                    }
                    mInterface = null;
                }

                // TODO Purge stale pipes.

                subscriber.onNext(Boolean.TRUE);
                subscriber.onCompleted();
            }
        });
    }
}
