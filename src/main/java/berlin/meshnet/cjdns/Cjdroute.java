package berlin.meshnet.cjdns;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.UUID;

import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.util.InputStreamObservable;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Methods for managing the execution of cjdroute.
 */
public abstract class Cjdroute {

    /**
     * The filename for the cjdroute executable.
     */
    static final String FILENAME_CJDROUTE = "cjdroute";

    /**
     * {@link SharedPreferences} key for cjdroute PID.
     */
    private static final String SHARED_PREFERENCES_KEY_CJDROUTE_PID = "cjdroutePid";

    /**
     * Value that represents an invalid PID.
     */
    static final long INVALID_PID = Long.MIN_VALUE;

    /**
     * Checks if the node is running.
     *
     * @return {@link Observable} that emits the PID if the node is running.
     */
    public static Observable<Long> running() throws UnknownHostException {
        final AdminApi api = new AdminApi();
        return AdminApi.ping(api)
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean isSuccessful) {
                        return isSuccessful;
                    }
                })
                .flatMap(new Func1<Boolean, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Boolean isSuccessful) {
                        return AdminApi.Core.pid(api);
                    }
                })
                .defaultIfEmpty(INVALID_PID);
    }

    /**
     * {@link Subscriber} that executes cjdroute.
     *
     * @return The {@link Subscriber}.
     */
    abstract Subscriber<JSONObject> execute();

    /**
     * {@link Subscriber} that terminates cjdroute.
     *
     * @return The {@link Subscriber}.
     */
    abstract Subscriber<Long> terminate();

    /**
     * TODO
     */
    abstract Observable<Boolean> start();

    /**
     * Default implementation of {@link Cjdroute}. This relies on {@link android.net.VpnService}
     * introduced in {@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH} and does not require
     * super user permission.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static class Default extends Cjdroute {

        /**
         * Log tag.
         */
        private static final String TAG = Default.class.getSimpleName();

        private Context mContext;

        public Default(Context context) {
            mContext = context;
        }

        @Override
        public Subscriber<JSONObject> execute() {
            throw new UnsupportedOperationException("Deprecated");
        }

        @Override
        public Subscriber<Long> terminate() {
            return new Subscriber<Long>() {
                @Override
                public void onNext(Long pid) {
                    Log.i(TAG, "Terminating cjdroute with pid=" + pid);

                    try {
                        AdminApi api = new AdminApi();
                        AdminApi.Core.exit(api)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe(new Action1<Boolean>() {
                                    @Override
                                    public void call(Boolean isSuccessful) {
                                        Log.i(TAG, "cjdroute terminated");
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        Log.e(TAG, "Failed to terminate cjdroute", throwable);
                                    }
                                });
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "Failed to start AdminApi", e);
                    }
                }

                @Override
                public void onCompleted() {
                    // Do nothing.
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "Failed to terminate cjdroute", e);
                }
            };
        }

        @Override
        public Observable<Boolean> start() {
            return CjdrouteConf.fetch0(mContext)
                    .flatMap(new Func1<Node.Me, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Node.Me me) {
                            return Observable.create(new Observable.OnSubscribe<Boolean>() {
                                @Override
                                public void call(Subscriber<? super Boolean> subscriber) {
                                    try {
                                        final File filesDir = mContext.getFilesDir();
                                        final String pipe = UUID.randomUUID().toString();

                                        // Start cjdroute.
                                        Process process = new ProcessBuilder("./cjdroute", "core", filesDir.getPath(), pipe)
                                                .directory(filesDir)
                                                .redirectErrorStream(true)
                                                .start();

                                        // Subscribe to input stream.
                                        final InputStream is = process.getInputStream();
                                        InputStreamObservable.line(is)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(Schedulers.io())
                                                .subscribe(
                                                        new Action1<String>() {
                                                            @Override
                                                            public void call(String line) {
                                                                Log.i(TAG, line);
                                                            }
                                                        }, new Action1<Throwable>() {
                                                            @Override
                                                            public void call(Throwable throwable) {
                                                                Log.e(TAG, "Failed to parse input stream", throwable);
                                                                if (is != null) {
                                                                    try {
                                                                        is.close();
                                                                    } catch (IOException e) {
                                                                        // Do nothing.
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        new Action0() {
                                                            @Override
                                                            public void call() {
                                                                Log.i(TAG, "Completed parsing of input stream");
                                                                if (is != null) {
                                                                    try {
                                                                        is.close();
                                                                    } catch (IOException e) {
                                                                        // Do nothing.
                                                                    }
                                                                }
                                                            }
                                                        });

                                        // TODO Replace this with directly passing params.
                                        CjdrouteConf.fetch0(mContext)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(Schedulers.io())
                                                .subscribe(new Action1<Node.Me>() {
                                                    @Override
                                                    public void call(Node.Me me) {
                                                        try {
                                                            Process initProcess = new ProcessBuilder("./cjdroute-init",
                                                                    filesDir.getPath(), pipe, me.privateKey, "127.0.0.1:11234", "NONE")
                                                                    .directory(filesDir)
                                                                    .redirectErrorStream(true)
                                                                    .start();
                                                        } catch (IOException e) {
                                                            Log.e(TAG, "Failed to start cjdroute-init process", e);
                                                        }
                                                    }
                                                }, new Action1<Throwable>() {
                                                    @Override
                                                    public void call(Throwable throwable) {
                                                        Log.e(TAG, "Failed to start cjdroute-init process", throwable);
                                                    }
                                                });

                                        // TODO Check for when cjdroute is ready instead.
                                        Thread.sleep(5000L);

                                        // Ping node to check if node is running.
                                        AdminApi api = new AdminApi();
                                        Boolean isNodeRunning = AdminApi.ping(api).toBlocking().first();
                                        if (isNodeRunning != null && isNodeRunning) {
                                            Log.i(TAG, "cjdroute started");
                                            subscriber.onNext(Boolean.TRUE);
                                            subscriber.onCompleted();
                                        } else {
                                            Log.i(TAG, "Failed to start cjdroute");
                                            subscriber.onError(new IOException("Failed to start cjdroute"));
                                        }
                                    } catch (IOException | InterruptedException e) {
                                        Log.e(TAG, "Failed to start cjdroute", e);
                                        subscriber.onError(e);
                                    }
                                }
                            });
                        }
                    });
        }
    }


    /**
     * Compat implementation of {@link Cjdroute}. This allows cjdroute to create a TUN device and
     * requires super user permission.
     */
    public static class Compat extends Cjdroute {

        /**
         * Log tag.
         */
        private static final String TAG = Compat.class.getSimpleName();

        /**
         * Command to substitute user to root.
         */
        private static final String CMD_SUBSTITUTE_ROOT_USER = "su";

        /**
         * Command divider.
         */
        private static final String CMD_NEWLINE = "\n";

        /**
         * Command to add a default route. Some browsers will not resolve DNS without a default route for IPv6.
         */
        private static final String CMD_ADD_DEFAULT_ROUTE = "ip -6 route add default via fc00::1 dev tun0 metric 4096";

        /**
         * Command template to execute cjdroute.
         */
        private static final String CMD_EXECUTE_CJDROUTE = "%1$s/" + FILENAME_CJDROUTE + " < %2$s/" + CjdrouteConf.FILENAME_CJDROUTE_CONF;

        /**
         * Command template to terminate process by PID.
         */
        private static final String CMD_KILL_PROCESS = "kill %1$s";

        /**
         * Line template to scan for getting cjdroute PID.
         */
        private static final String LINE_ADMIN_API = "Bound to address [%1$s]";

        private Context mContext;

        public Compat(Context context) {
            mContext = context;
        }

        @Override
        public Subscriber<JSONObject> execute() {
            return new Subscriber<JSONObject>() {
                @Override
                public void onNext(JSONObject cjdrouteConf) {
                    // TODO Fix Compat implementation.
//                    DataOutputStream os = null;
//                    try {
//                        java.lang.Process process = Runtime.getRuntime().exec(CMD_SUBSTITUTE_ROOT_USER);
//
//                        // Subscribe to input stream.
//                        final InputStream is = process.getInputStream();
//                        InputStreamObservable.line(is)
//                                .subscribeOn(Schedulers.io())
//                                .observeOn(Schedulers.io())
//                                .subscribe(
//                                        new Action1<String>() {
//                                            @Override
//                                            public void call(String line) {
//                                                Log.i(TAG, line);
//                                            }
//                                        }, new Action1<Throwable>() {
//                                            @Override
//                                            public void call(Throwable throwable) {
//                                                Log.e(TAG, "Failed to parse input stream", throwable);
//                                                if (is != null) {
//                                                    try {
//                                                        is.close();
//                                                    } catch (IOException e) {
//                                                        // Do nothing.
//                                                    }
//                                                }
//                                            }
//                                        },
//                                        new Action0() {
//                                            @Override
//                                            public void call() {
//                                                Log.i(TAG, "Completed parsing of input stream");
//                                                if (is != null) {
//                                                    try {
//                                                        is.close();
//                                                    } catch (IOException e) {
//                                                        // Do nothing.
//                                                    }
//                                                }
//                                            }
//                                        });
//
//                        // Subscribe to error stream.
//                        final AdminApi adminApi = AdminApi.from(cjdrouteConf);
//                        final String adminLine = String.format(Locale.ENGLISH, LINE_ADMIN_API, adminApi.getBind());
//                        final InputStream es = process.getErrorStream();
//                        InputStreamObservable.line(es)
//                                .subscribeOn(Schedulers.io())
//                                .observeOn(Schedulers.io())
//                                .subscribe(
//                                        new Action1<String>() {
//                                            @Override
//                                            public void call(String line) {
//                                                Log.i(TAG, line);
//
//                                                // Find and store cjdroute PID.
//                                                // TODO Apply filter operator on the line.
//                                                if (line.contains(adminLine)) {
//                                                    try {
//                                                        // TODO Apply corePid as operator.
//                                                        int pid = adminApi.corePid();
//
//                                                        // Store PID on disk to persist across java process crashes.
//                                                        SharedPreferences.Editor editor = PreferenceManager
//                                                                .getDefaultSharedPreferences(mContext.getApplicationContext()).edit();
//                                                        editor.putInt(SHARED_PREFERENCES_KEY_CJDROUTE_PID, pid);
//                                                        editor.apply();
//                                                    } catch (IOException e) {
//                                                        Log.e(TAG, "Failed to get cjdroute PID", e);
//                                                    }
//                                                }
//                                            }
//                                        }, new Action1<Throwable>() {
//                                            @Override
//                                            public void call(Throwable throwable) {
//                                                Log.e(TAG, "Failed to parse error stream", throwable);
//                                                if (es != null) {
//                                                    try {
//                                                        es.close();
//                                                    } catch (IOException e) {
//                                                        // Do nothing.
//                                                    }
//                                                }
//                                            }
//                                        },
//                                        new Action0() {
//                                            @Override
//                                            public void call() {
//                                                Log.i(TAG, "Completed parsing of error stream");
//                                                if (es != null) {
//                                                    try {
//                                                        es.close();
//                                                    } catch (IOException e) {
//                                                        // Do nothing.
//                                                    }
//                                                }
//                                            }
//                                        });
//
//                        // Execute cjdroute.
//                        String filesDir = mContext.getFilesDir().getPath();
//                        os = new DataOutputStream(process.getOutputStream());
//                        os.writeBytes(String.format(Locale.ENGLISH, CMD_EXECUTE_CJDROUTE, filesDir, filesDir));
//                        os.writeBytes(CMD_NEWLINE);
//                        os.writeBytes(CMD_ADD_DEFAULT_ROUTE);
//                        os.flush();
//                    } catch (IOException | JSONException e) {
//                        Log.e(TAG, "Failed to execute cjdroute", e);
//                    } finally {
//                        if (os != null) {
//                            try {
//                                os.close();
//                            } catch (IOException e) {
//                                // Do nothing.
//                            }
//                        }
//                    }
                }

                @Override
                public void onCompleted() {
                    // Do nothing.
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "Failed to execute cjdroute", e);
                }
            };
        }

        @Override
        public Subscriber<Long> terminate() {
            return new Subscriber<Long>() {
                @Override
                public void onNext(Long pid) {
                    Log.i(TAG, "Terminating cjdroute with pid=" + pid);

                    // Kill cjdroute as root.
                    DataOutputStream os = null;
                    try {
                        java.lang.Process process = Runtime.getRuntime().exec(CMD_SUBSTITUTE_ROOT_USER);
                        os = new DataOutputStream(process.getOutputStream());
                        os.writeBytes(String.format(Locale.ENGLISH, CMD_KILL_PROCESS, pid));
                        os.flush();

                        // Erase PID. TODO Change implementation.
//                    SharedPreferences.Editor editor = PreferenceManager
//                            .getDefaultSharedPreferences(mContext.getApplicationContext()).edit();
//                    editor.putLong(SHARED_PREFERENCES_KEY_CJDROUTE_PID, INVALID_PID);
//                    editor.apply();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to terminate cjdroute", e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Do nothing.
                            }
                        }
                    }
                }

                @Override
                public void onCompleted() {
                    // Do nothing.
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "Failed to terminate cjdroute", e);
                }
            };
        }

        @Override
        Observable<Boolean> start() {
            return null;
        }
    }
}
