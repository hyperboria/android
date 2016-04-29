package berlin.meshnet.cjdns;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;

import berlin.meshnet.cjdns.model.Node;
import rx.Observable;
import rx.Subscriber;

/**
 * Configurations for cjdroute.
 */
public abstract class CjdrouteConf {

    /**
     * The filename for the cjdroute configurations.
     */
    static final String FILENAME_CJDROUTE_CONF = "cjdroute.conf";

    /**
     * Command for system shell.
     */
    private static final String CMD_SET_UP_SHELL = "/system/bin/sh";

    /**
     * Command to execute command.
     */
    private static final String CMD_EXECUTE_COMMAND = "-c";

    /**
     * Command template to generate the cjdroute configurations.
     */
    private static final String CMD_GENERATE_CJDROUTE_CONF_TEMPLATE = "%1$s/" + Cjdroute.FILENAME_CJDROUTE + " --genconf | %2$s/" + Cjdroute.FILENAME_CJDROUTE + " --cleanconf";

    /**
     * Lock to ensure cjdroute configurations is only generated once.
     */
    private static final Object sLock = new Object();

    /**
     * Shared preference key for storing this node's address.
     */
    private static final String SHARED_PREFERENCES_KEY_ADDRESS = "address";

    /**
     * Shared preference key for storing this node's public key.
     */
    private static final String SHARED_PREFERENCES_KEY_PUBLIC_KEY = "publicKey";

    /**
     * Shared preference key for storing this node's private key.
     */
    private static final String SHARED_PREFERENCES_KEY_PRIVATE_KEY = "privateKey";

    /**
     * Default public peer interface. TODO Remove.
     */
    private static final String DEFAULT_PEER_INTERFACE = "104.200.29.163:53053";

    /**
     * Default public peer credentials. TODO Remove.
     */
    private static final String DEFAULT_PEER_CREDENTIALS = "{\n" +
            "  \"publicKey\": \"1941p5k8qqvj17vjrkb9z97wscvtgc1vp8pv1huk5120cu42ytt0.k\",\n" +
            "  \"password\": \"8fVMl0oo6QI6wKeMneuY26x1MCgRemg\",\n" +
            "  \"contact\": \"ansuz@transitiontech.ca\",\n" +
            "  \"location\": \"Newark,NJ,USA\"\n" +
            "}";

    public static Observable<Node.Me> fetch0(Context context) {
        final Context appContext = context.getApplicationContext();
        return Observable.create(new Observable.OnSubscribe<Node.Me>() {
            @Override
            public void call(Subscriber<? super Node.Me> subscriber) {
                String filesDir = appContext.getFilesDir().getPath();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
                Node.Me me = from(sharedPref.getString(SHARED_PREFERENCES_KEY_ADDRESS, null),
                        sharedPref.getString(SHARED_PREFERENCES_KEY_PUBLIC_KEY, null),
                        sharedPref.getString(SHARED_PREFERENCES_KEY_PRIVATE_KEY, null));
                if (me != null) {
                    // Return existing node info.
                    subscriber.onNext(me);
                    subscriber.onCompleted();
                } else {
                    // Generate new node and return info.
                    try {
                        // Copy executables.
                        copyExecutable(appContext, filesDir, Cjdroute.FILENAME_CJDROUTE);
                        copyExecutable(appContext, filesDir, Cjdroute.FILENAME_CJDROUTE + "-init"); // TODO Remove.

                        // Create new configuration file from which to get node info.
                        String[] cmd = {
                                CMD_SET_UP_SHELL,
                                CMD_EXECUTE_COMMAND,
                                String.format(Locale.ENGLISH, CMD_GENERATE_CJDROUTE_CONF_TEMPLATE, filesDir, filesDir)
                        };
                        InputStream is = null;
                        try {
                            // Generate new configurations.
                            Process process = Runtime.getRuntime().exec(cmd);
                            is = process.getInputStream();
                            JSONObject json = new JSONObject(fromInputStream(is));

                            // Get node info.
                            String ipv6 = (String) json.get("ipv6");
                            String publicKey = (String) json.get("publicKey");
                            String privateKey = (String) json.get("privateKey");
                            me = from(ipv6, publicKey, privateKey);
                            if (me != null) {
                                // Store node info.
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(SHARED_PREFERENCES_KEY_ADDRESS, ipv6);
                                editor.putString(SHARED_PREFERENCES_KEY_PUBLIC_KEY, publicKey);
                                editor.putString(SHARED_PREFERENCES_KEY_PRIVATE_KEY, privateKey);
                                editor.apply();

                                // Return JSON object and complete Rx contract.
                                subscriber.onNext(new Node.Me("Me", ipv6, publicKey, privateKey));
                                subscriber.onCompleted();
                            } else {
                                subscriber.onError(new IOException("Failed to generate node info"));
                            }
                        } catch (IOException | JSONException e) {
                            subscriber.onError(e);
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                    // Do nothing.
                                }
                            }
                        }
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    /**
     * {@link Observable} for cjdroute configuration JSON object.
     *
     * @param context The {@link Context}.
     * @return The {@link Observable}.
     */
    public static Observable<JSONObject> fetch(Context context) {
        final Context appContext = context.getApplicationContext();
        return Observable.create(new Observable.OnSubscribe<JSONObject>() {
            @Override
            public void call(Subscriber<? super JSONObject> subscriber) {
                synchronized (sLock) {
                    String filesDir = appContext.getFilesDir().getPath();
                    File confFile = new File(filesDir, FILENAME_CJDROUTE_CONF);
                    if (confFile.exists()) {
                        // Create configuration JSON object from existing file.
                        InputStream is = null;
                        try {
                            // Create JSON object.
                            is = new FileInputStream(confFile);
                            JSONObject json = new JSONObject(fromInputStream(is));

                            // Return JSON object and complete Rx contract.
                            subscriber.onNext(json);
                            subscriber.onCompleted();
                        } catch (IOException | JSONException e) {
                            subscriber.onError(e);
                        } finally {
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                    // Do nothing.
                                }
                            }
                        }
                    } else {
                        try {
                            // Copy executables.
                            copyExecutable(appContext, filesDir, Cjdroute.FILENAME_CJDROUTE);
                            copyExecutable(appContext, filesDir, Cjdroute.FILENAME_CJDROUTE + "-init"); // TODO Remove.

                            // Create new configuration file from which to return JSON object.
                            String[] cmd = {
                                    CMD_SET_UP_SHELL,
                                    CMD_EXECUTE_COMMAND,
                                    String.format(Locale.ENGLISH, CMD_GENERATE_CJDROUTE_CONF_TEMPLATE, filesDir, filesDir)
                            };
                            InputStream is = null;
                            FileOutputStream os = null;
                            try {
                                // Generate new configurations.
                                Process process = Runtime.getRuntime().exec(cmd);
                                is = process.getInputStream();
                                JSONObject json = new JSONObject(fromInputStream(is));

                                // Append default peer credentials. TODO Remove.
                                json.getJSONObject("interfaces")
                                        .getJSONArray("UDPInterface")
                                        .getJSONObject(0)
                                        .getJSONObject("connectTo")
                                        .put(DEFAULT_PEER_INTERFACE, new JSONObject(DEFAULT_PEER_CREDENTIALS));

                                // Write configurations to file.
                                os = appContext.openFileOutput(FILENAME_CJDROUTE_CONF, Context.MODE_PRIVATE);
                                os.write(json.toString().getBytes());
                                os.flush();

                                // Return JSON object and complete Rx contract.
                                subscriber.onNext(json);
                                subscriber.onCompleted();
                            } catch (IOException | JSONException e) {
                                subscriber.onError(e);
                            } finally {
                                if (is != null) {
                                    try {
                                        is.close();
                                    } catch (IOException e) {
                                        // Do nothing.
                                    }
                                }
                                if (os != null) {
                                    try {
                                        os.close();
                                    } catch (IOException e) {
                                        // Do nothing.
                                    }
                                }
                            }
                        } catch (IOException e) {
                            subscriber.onError(e);
                        }
                    }
                }
            }
        });
    }

    /**
     * Creates a {@link berlin.meshnet.cjdns.model.Node.Me}.
     *
     * @param address    The ipv6 address.
     * @param publicKey  The public key.
     * @param privateKey The private key.
     * @return The {@link berlin.meshnet.cjdns.model.Node.Me}; or {@code null} if invalid input.
     */
    private static Node.Me from(String address, String publicKey, String privateKey) {
        if (!TextUtils.isEmpty(address) && !TextUtils.isEmpty(publicKey) && !TextUtils.isEmpty(privateKey)) {
            return new Node.Me("Me", address, publicKey, privateKey);
        }
        return null;
    }

    /**
     * Copies a file from assets folder and makes executable. If the file is already in that state,
     * this is a no-op.
     *
     * @param context  The {@link Context}.
     * @param filesDir The files directory.
     * @param filename The filename to copy.
     * @return The copied executable file.
     * @throws IOException Thrown if copying failed.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static File copyExecutable(Context context, String filesDir, String filename) throws IOException {
        // If file is not present in the files directory, it needs to be copied over from assets.
        File copyFile = new File(filesDir, filename);
        if (!copyFile.exists()) {
            // Copy file from assets folder to the files directory.
            InputStream is = null;
            FileOutputStream os = null;
            try {
                String abi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? Build.SUPPORTED_ABIS[0] : Build.CPU_ABI;
                is = context.getAssets().open(abi + "/" + filename);
                os = context.openFileOutput(filename, Context.MODE_PRIVATE);
                copyStream(is, os);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Do nothing.
                    }
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        // Do nothing.
                    }
                }
            }
        }

        // Check file existence and permissions.
        if (copyFile.exists()) {
            if (copyFile.canExecute() || copyFile.setExecutable(true)) {
                return copyFile;
            } else {
                throw new IOException("Failed to make " + copyFile + " executable in " + copyFile.getPath());
            }
        } else {
            throw new FileNotFoundException("Failed to create " + copyFile + " in " + copyFile.getPath());
        }
    }

    /**
     * Writes an {@link InputStream} to an {@link OutputStream}.
     *
     * @param is The {@link InputStream}.
     * @param os The {@link OutputStream}.
     * @throws IOException Thrown if writing failed.
     */
    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte buf[] = new byte[4096];
        int len = is.read(buf);
        while (len > 0) {
            os.write(buf, 0, len);
            len = is.read(buf);
        }
        os.flush();
    }

    /**
     * Parses an {@link InputStream} into string.
     *
     * @param is The {@link InputStream}.
     * @return The string.
     * @throws IOException Thrown if parsing failed.
     */
    private static String fromInputStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
