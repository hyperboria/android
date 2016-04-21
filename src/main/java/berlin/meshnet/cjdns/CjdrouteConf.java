package berlin.meshnet.cjdns;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

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

import rx.Observable;
import rx.Subscriber;

/**
 * Configurations for cjdroute.
 */
abstract class CjdrouteConf {

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

//    /**
//     * Default public peer interface. TODO Remove.
//     */
//    private static final String DEFAULT_PEER_INTERFACE = "104.200.29.163:53053";
//
//    /**
//     * Default public peer credentials. TODO Remove.
//     */
//    private static final String DEFAULT_PEER_CREDENTIALS = "{\n" +
//            "  \"publicKey\": \"1941p5k8qqvj17vjrkb9z97wscvtgc1vp8pv1huk5120cu42ytt0.k\",\n" +
//            "  \"password\": \"8fVMl0oo6QI6wKeMneuY26x1MCgRemg\",\n" +
//            "  \"contact\": \"ansuz@transitiontech.ca\",\n" +
//            "  \"location\": \"Newark,NJ,USA\"\n" +
//            "}";

    /**
     * {@link Observable} for cjdroute configuration JSON object.
     *
     * @param context The {@link Context}.
     * @return The {@link Observable}.
     */
    public static Observable<JSONObject> fetch(Context context) {
        final Context appContext = context.getApplicationContext();
        return Observable.create(new Observable.OnSubscribe<JSONObject>() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
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
                        // If cjdroute is not present in the files directory, it needs to be copied over from assets.
                        File cjdroutefile = new File(filesDir, Cjdroute.FILENAME_CJDROUTE);
                        if (!cjdroutefile.exists()) {
                            // Copy cjdroute from assets folder to the files directory.
                            InputStream is = null;
                            FileOutputStream os = null;
                            try {
                                String abi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? Build.SUPPORTED_ABIS[0] : Build.CPU_ABI;
                                is = appContext.getAssets().open(abi + "/" + Cjdroute.FILENAME_CJDROUTE);
                                os = appContext.openFileOutput(Cjdroute.FILENAME_CJDROUTE, Context.MODE_PRIVATE);
                                copyStream(is, os);
                            } catch (IOException e) {
                                subscriber.onError(e);
                                return;
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

                        // Create new configuration file from which to return JSON object.
                        if (cjdroutefile.exists()) {
                            if (cjdroutefile.canExecute() || cjdroutefile.setExecutable(true)) {
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
                                    String jsonString = fromInputStream(is);

                                    // TODO Hack this in for now.
                                    jsonString = "{\n" +
                                            "  \"pipe\": \"/data/data/berlin.meshnet.cjdns\",\n" +
//                                            "  \"security\": [\n" +
//                                            "    {\n" +
//                                            "      \"keepNetAdmin\": 1,\n" +
//                                            "      \"setuser\": \"nobody\"\n" +
//                                            "    },\n" +
//                                            "    {\n" +
//                                            "      \"chroot\": 0\n" +
//                                            "    },\n" +
//                                            "    {\n" +
//                                            "      \"nofiles\": 0\n" +
//                                            "    },\n" +
//                                            "    {\n" +
//                                            "      \"noforks\": 1\n" +
//                                            "    },\n" +
//                                            "    {\n" +
//                                            "      \"seccomp\": 0\n" +
//                                            "    },\n" +
//                                            "    {\n" +
//                                            "      \"setupComplete\": 1\n" +
//                                            "    }\n" +
//                                            "  ],\n" +
                                            "  \"admin\": {\n" +
                                            "    \"password\": \"none\",\n" +
                                            "    \"bind\": \"127.0.0.1:11234\"\n" +
                                            "  },\n" +
                                            "  \"privateKey\": \"59ae83c9cd94a18add9d76096ca85a4005683f18ad997236e7ad5660b9b77c4c\",\n" +
                                            "  \"publicKey\": \"pmr3bqsp33rdu9d6grf243wrc7kbsdzwubg5sg3gmz68u1hgznn0.k\",\n" +
                                            "  \"ipv6\": \"fce5:c180:6bff:a33f:c0b3:f22a:945d:ca39\"\n" +
                                            "}";
                                    JSONObject json = new JSONObject(jsonString);

//                                    // Append default peer credentials. TODO Remove.
//                                    json.getJSONObject("interfaces")
//                                            .getJSONArray("UDPInterface")
//                                            .getJSONObject(0)
//                                            .getJSONObject("connectTo")
//                                            .put(DEFAULT_PEER_INTERFACE, new JSONObject(DEFAULT_PEER_CREDENTIALS));

                                    // Write configurations to file.
                                    os = appContext.openFileOutput(FILENAME_CJDROUTE_CONF, Context.MODE_PRIVATE);
                                    os.write(jsonString.getBytes());
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
                            } else {
                                subscriber.onError(new IOException("Failed to execute cjdroute in " + cjdroutefile.getPath()));
                            }
                        } else {
                            subscriber.onError(new FileNotFoundException("Failed to find cjdroute in " + cjdroutefile.getPath()));
                        }
                    }
                }
            }
        });
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
