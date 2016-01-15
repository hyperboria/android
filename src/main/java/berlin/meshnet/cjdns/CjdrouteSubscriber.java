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
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import berlin.meshnet.cjdns.util.InputStreamObservable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * {@link Subscriber}s for managing the execution of cjdroute.
 */
interface CjdrouteSubscriber {

    /**
     * The filename for the cjdroute executable.
     */
    String FILENAME_CJDROUTE = "cjdroute";

    /**
     * {@link SharedPreferences} key for cjdroute PID.
     */
    String SHARED_PREFERENCES_KEY_CJDROUTE_PID = "cjdroutePid";

    /**
     * {@link Subscriber} that executes cjdroute.
     *
     * @return The {@link Subscriber}.
     */
    Subscriber<JSONObject> execute();

    /**
     * {@link Subscriber} that terminates cjdroute.
     *
     * @return The {@link Subscriber}.
     */
    Subscriber<Integer> terminate();

    /**
     * Default implementation of {@link CjdrouteSubscriber}. This relies on {@link android.net.VpnService}
     * introduced in {@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH} and does not require
     * super user permission.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    class Default implements CjdrouteSubscriber {

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
     * Compat implementation of {@link CjdrouteSubscriber}. This allows cjdroute to create a TUN device and
     * requires super user permission.
     */
    class Compat implements CjdrouteSubscriber {

        /**
         * Log tag.
         */
        private static final String TAG = Compat.class.getSimpleName();

        /**
         * Command to substitute user to root.
         */
        private static final String CMD_SUBSTITUTE_ROOT_USER = "su";

        /**
         * Command template to execute cjdroute.
         */
        private static final String CMD_EXECUTE_CJDROUTE = "%1$s/" + FILENAME_CJDROUTE + " < %2$s/" + CjdrouteConfObservable.FILENAME_CJDROUTE_CONF;

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
                        java.lang.Process process = Runtime.getRuntime().exec(CMD_SUBSTITUTE_ROOT_USER);

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

                        // Subscribe to error stream.
                        final AdminApi adminApi = AdminApi.from(cjdrouteConf);
                        final String adminLine = String.format(Locale.ENGLISH, LINE_ADMIN_API, adminApi.getBind());
                        final InputStream es = process.getErrorStream();
                        InputStreamObservable.line(es)
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe(
                                        new Action1<String>() {
                                            @Override
                                            public void call(String line) {
                                                Log.i(TAG, line);

                                                // Find and store cjdroute PID.
                                                if (line.contains(adminLine)) {
                                                    try {
                                                        int pid = adminApi.corePid();

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
                        os.writeBytes(String.format(CMD_EXECUTE_CJDROUTE, filesDir, filesDir));
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

                    // Kill cjdroute as root.
                    DataOutputStream os = null;
                    try {
                        java.lang.Process process = Runtime.getRuntime().exec(CMD_SUBSTITUTE_ROOT_USER);
                        os = new DataOutputStream(process.getOutputStream());
                        os.writeBytes(String.format(Locale.ENGLISH, CMD_KILL_PROCESS, pid));
                        os.flush();
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
    }
}
