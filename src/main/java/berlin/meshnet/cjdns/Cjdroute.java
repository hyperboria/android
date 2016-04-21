package berlin.meshnet.cjdns;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import berlin.meshnet.cjdns.util.InputStreamObservable;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Methods for managing the execution of cjdroute.
 */
abstract class Cjdroute {

    static {
        System.loadLibrary("sendfd");
    }

    public static native int sendfd(String path, int tun_fd);

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
    private static final int INVALID_PID = Integer.MIN_VALUE;

    /**
     * {@link Observable} for the PID of any currently running cjdroute process. If none is running,
     * this {@link Observable} will complete without calling {@link Subscriber#onNext(Object)}.
     *
     * @param context The {@link Context}.
     * @return The {@link Observable}.
     */
    public static Observable<Integer> running(Context context) {
        final Context appContext = context.getApplicationContext();
        return Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        int pid = PreferenceManager.getDefaultSharedPreferences(appContext)
                                .getInt(SHARED_PREFERENCES_KEY_CJDROUTE_PID, INVALID_PID);
                        subscriber.onNext(pid);
                        subscriber.onCompleted();
                    }
                })
                .filter(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer pid) {
                        return pid != INVALID_PID;
                    }
                });
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
    abstract Subscriber<Integer> terminate();

    /**
     * Default implementation of {@link Cjdroute}. This relies on {@link android.net.VpnService}
     * introduced in {@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH} and does not require
     * super user permission.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    static class Default extends Cjdroute {

        /**
         * Log tag.
         */
        private static final String TAG = Default.class.getSimpleName();

        private Context mContext;

        Default(Context context) {
            mContext = context;
        }

        @Override
        public Subscriber<JSONObject> execute() {
            // TODO Make this work.
            throw new UnsupportedOperationException("Execution of cjdroute is not yet supported for your API level");
        }

        @Override
        public Subscriber<Integer> terminate() {
            return new Subscriber<Integer>() {
                @Override
                public void onNext(Integer pid) {
                    Log.i(TAG, "Terminating cjdroute with pid=" + pid);
                    Process.killProcess(pid);

                    // Erase PID.
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(mContext.getApplicationContext()).edit();
                    editor.putInt(SHARED_PREFERENCES_KEY_CJDROUTE_PID, INVALID_PID);
                    editor.apply();
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
    }

    /**
     * Compat implementation of {@link Cjdroute}. This allows cjdroute to create a TUN device and
     * requires super user permission.
     */
    static class Compat extends Cjdroute {

        /**
         * Log tag.
         */
        private static final String TAG = Compat.class.getSimpleName();

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
        private static final String CMD_EXECUTE_CJDROUTE = "%1$s/" + FILENAME_CJDROUTE + " --nobg";

        /**
         * Command template to terminate process by PID.
         */
        private static final String CMD_KILL_PROCESS = "kill %1$s";

        /**
         * Line template to scan for getting cjdroute PID.
         */
        private static final String LINE_ADMIN_API = "Bound to address [%1$s]";

        private Context mContext;

        Compat(Context context) {
            mContext = context;
        }

        @Override
        public Subscriber<JSONObject> execute() {
            return new Subscriber<JSONObject>() {
                @Override
                public void onNext(JSONObject cjdrouteConf) {
                    DataOutputStream os = null;
                    try {
//                        java.lang.Process process = Runtime.getRuntime().exec(String.format(CMD_EXECUTE_CJDROUTE, mContext.getFilesDir().getPath()));

                        java.lang.Process process = new ProcessBuilder("./cjdroute")
                                .directory(new File("/data/data/berlin.meshnet.cjdns/files"))
                                .redirectErrorStream(true)
                                .start();

//                        // Subscribe to input stream.
//                        final InputStream is = process.getInputStream();
//                        InputStreamObservable.line(is)
//                                .subscribeOn(Schedulers.newThread())
//                                .observeOn(Schedulers.immediate())
//                                .subscribe(
//                                        new Action1<String>() {
//                                            @Override
//                                            public void call(String line) {
//                                                Log.i(TAG, "IS: " + line);
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

                        // Subscribe to error stream.
                        final AdminApi adminApi = AdminApi.from(cjdrouteConf);
                        final String adminLine = String.format(Locale.ENGLISH, LINE_ADMIN_API, adminApi.getBind());
//                        final InputStream es = process.getErrorStream();
                        final InputStream es = process.getInputStream();
                        InputStreamObservable.line(es)
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(Schedulers.immediate())
                                .subscribe(
                                        new Action1<String>() {
                                            @Override
                                            public void call(String line) {
                                                Log.i(TAG, line);

                                                // Find and store cjdroute PID.
                                                // TODO Apply filter operator on the line.
                                                if (line.contains(adminLine)) {
                                                    try {
                                                        // TODO Apply runStuff as operator.
                                                        int pid = adminApi.runStuff();

                                                        // Store PID on disk to persist across java process crashes.
                                                        SharedPreferences.Editor editor = PreferenceManager
                                                                .getDefaultSharedPreferences(mContext.getApplicationContext()).edit();
                                                        editor.putInt(SHARED_PREFERENCES_KEY_CJDROUTE_PID, pid);
                                                        editor.apply();
                                                    } catch (IOException e) {
                                                        Log.e(TAG, "Failed to get cjdroute PID", e);
                                                    }
                                                }
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable throwable) {
                                                Log.e(TAG, "Failed to parse error stream", throwable);
                                                if (es != null) {
                                                    try {
                                                        es.close();
                                                    } catch (IOException e) {
                                                        // Do nothing.
                                                    }
                                                }
                                            }
                                        },
                                        new Action0() {
                                            @Override
                                            public void call() {
                                                Log.i(TAG, "Completed parsing of error stream");
                                                if (es != null) {
                                                    try {
                                                        es.close();
                                                    } catch (IOException e) {
                                                        // Do nothing.
                                                    }
                                                }
                                            }
                                        });

                        // Execute cjdroute.
                        String filesDir = mContext.getFilesDir().getPath();
                        os = new DataOutputStream(process.getOutputStream());
                        String conf = "{\n" +
                                "  \"pipe\": \"/data/data/berlin.meshnet.cjdns\",\n" +
                                "  \"security\": [\n" +
                                "    {\n" +
                                "      \"keepNetAdmin\": 1,\n" +
                                "      \"setuser\": 0\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"chroot\": 0\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"nofiles\": 0\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"noforks\": 1\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"seccomp\": 0\n" +
                                "    },\n" +
                                "    {\n" +
                                "      \"setupComplete\": 1\n" +
                                "    }\n" +
                                "  ],\n" +
                                "  \"admin\": {\n" +
                                "    \"password\": \"none\",\n" +
                                "    \"bind\": \"127.0.0.1:11234\"\n" +
                                "  },\n" +
                                "  \"privateKey\": \"59ae83c9cd94a18add9d76096ca85a4005683f18ad997236e7ad5660b9b77c4c\",\n" +
                                "  \"publicKey\": \"pmr3bqsp33rdu9d6grf243wrc7kbsdzwubg5sg3gmz68u1hgznn0.k\",\n" +
                                "  \"ipv6\": \"fce5:c180:6bff:a33f:c0b3:f22a:945d:ca39\"\n" +
                                "}";
                        os.writeBytes(conf);
//                        os.writeBytes(CMD_NEWLINE);
//                        os.writeBytes(CMD_ADD_DEFAULT_ROUTE);
                        os.flush();
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Failed to execute cjdroute", e);
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
                    Log.e(TAG, "Failed to execute cjdroute", e);
                }
            };
        }

        @Override
        public Subscriber<Integer> terminate() {
            return new Subscriber<Integer>() {
                @Override
                public void onNext(Integer pid) {
                    Log.i(TAG, "Terminating cjdroute with pid=" + pid);

                    try {
                        AdminApi api = new AdminApi(InetAddress.getByName("127.0.0.1"), 11234, "none".getBytes());
                        api.coreExit();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    Process.killProcess(pid);

                    // Erase PID.
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(mContext.getApplicationContext()).edit();
                    editor.putInt(SHARED_PREFERENCES_KEY_CJDROUTE_PID, INVALID_PID);
                    editor.apply();
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
    }
}
