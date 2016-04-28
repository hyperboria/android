package berlin.meshnet.cjdns;

import android.util.Log;

import org.bitlet.wetorrent.bencode.Bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;

/**
 * API for administration of the cjdns node.
 */
class AdminApi {

    private static final String TAG = AdminApi.class.getSimpleName();

    /**
     * Name of this class.
     */
    private static final String CLASS_NAME = AdminApi.class.getSimpleName();

    /**
     * UDP datagram socket timeout in milliseconds.
     */
    private static final int SOCKET_TIMEOUT = 30000;

    /**
     * UDP datagram length.
     */
    private static final int DATAGRAM_LENGTH = 4096;

    /**
     * Array used for HEX encoding.
     */
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /**
     * Request to {@link AdminApi} for authentication cookie.
     */
    private static final HashMap<ByteBuffer, Object> REQUEST_COOKIE = new LinkedHashMap<ByteBuffer, Object>() {{
        put(wrapString("q"), wrapString("cookie"));
    }};

    /**
     * The local IP address to bind the admin RPC server.
     */
    private static final String ADMIN_API_ADDRESS = "127.0.0.1";

    /**
     * The port to bind the admin RPC server.
     */
    private static final int ADMIN_API_PORT = 11234;

    /**
     * The password for authenticated requests.
     */
    private static final byte[] ADMIN_API_PASSWORD = "NONE".getBytes();

    /**
     * The local IP address to bind the admin RPC server, as an {@link InetAddress}.
     */
    private final InetAddress mAdminApiAddress;

    /**
     * Constructor.
     */
    public AdminApi() throws UnknownHostException {
        mAdminApiAddress = InetAddress.getByName(ADMIN_API_ADDRESS);
    }

    public static class AdminLog {

        public static Observable<Boolean> logMany(final AdminApi api) {
            throw new UnsupportedOperationException("AdminLog_logMany is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> subscribe(final AdminApi api) {
            throw new UnsupportedOperationException("AdminLog_subscribe is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> subscriptions(final AdminApi api) {
            throw new UnsupportedOperationException("AdminLog_subscriptions is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> unsubscribe(final AdminApi api) {
            throw new UnsupportedOperationException("AdminLog_unsubscribe is not implemented in " + CLASS_NAME);
        }
    }

    public static class Admin {

        public static Observable<Boolean> asyncEnabled(final AdminApi api) {
            throw new UnsupportedOperationException("Admin_asyncEnabled is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> availableFunctions(final AdminApi api) {
            throw new UnsupportedOperationException("Admin_availableFunctions is not implemented in " + CLASS_NAME);
        }
    }

    public static class Allocator {

        public static Observable<Boolean> bytesAllocated(final AdminApi api) {
            throw new UnsupportedOperationException("Allocator_bytesAllocated is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> snapshot(final AdminApi api) {
            throw new UnsupportedOperationException("Allocator_snapshot is not implemented in " + CLASS_NAME);
        }
    }

    public static class AuthorizedPasswords {

        public static Observable<Boolean> add(final AdminApi api) {
            throw new UnsupportedOperationException("AuthorizedPasswords_add is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> list(final AdminApi api) {
            throw new UnsupportedOperationException("AuthorizedPasswords_list is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> remove(final AdminApi api) {
            throw new UnsupportedOperationException("AuthorizedPasswords_remove is not implemented in " + CLASS_NAME);
        }
    }

    public static class Core {

        public static Observable<Boolean> exit(final AdminApi api) {
            return Observable.create(new BaseOnSubscribe<Boolean>(api, new Request("Core_exit")) {
                @Override
                protected Boolean parseResult(final Map response) {
                    return Boolean.TRUE;
                }
            });
        }

        public static Observable<Boolean> initTunfd(final AdminApi api, final Long tunfd, final Long type) {
            return Observable.create(new BaseOnSubscribe<Boolean>(api, new Request("Core_initTunfd",
                    new LinkedHashMap<ByteBuffer, Object>() {{
                        put(wrapString("tunfd"), tunfd);
                        put(wrapString("type"), type);
                    }})) {
                @Override
                protected Boolean parseResult(final Map response) {
                    return Boolean.TRUE;
                }
            });
        }

        public static Observable<Boolean> initTunnel(final AdminApi api) {
            throw new UnsupportedOperationException("Core_initTunnel is not implemented in " + CLASS_NAME);
        }

        public static Observable<Long> pid(final AdminApi api) {
            return Observable.create(new BaseOnSubscribe<Long>(api, new Request("Core_pid")) {
                @Override
                protected Long parseResult(final Map response) {
                    return (Long) response.get(wrapString("pid"));
                }
            });
        }
    }

    public static class EthInterface {

        public static Observable<Boolean> beacon(final AdminApi api) {
            throw new UnsupportedOperationException("ETHInterface_beacon is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> beginConnection(final AdminApi api) {
            throw new UnsupportedOperationException("ETHInterface_beginConnection is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> listDevices(final AdminApi api) {
            throw new UnsupportedOperationException("ETHInterface_listDevices is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> new0(final AdminApi api) {
            throw new UnsupportedOperationException("ETHInterface_new is not implemented in " + CLASS_NAME);
        }
    }

    public static class FileNo {

        public static Observable<Map<String, Long>> import0(final AdminApi api, final String path) {
            return Observable.create(new BaseOnSubscribe<Map<String, Long>>(api, new Request("FileNo_import",
                    new LinkedHashMap<ByteBuffer, Object>() {{
                        put(wrapString("path"), wrapString(path));
                        put(wrapString("type"), wrapString("android"));
                    }})) {
                @Override
                protected Map<String, Long> parseResult(final Map response) {
                    return new HashMap<String, Long>() {{
                        put("tunfd", (Long) response.get(wrapString("tunfd")));
                        put("type", (Long) response.get(wrapString("type")));
                    }};
                }
            });
        }
    }

    public static class InterfaceController {

        public static Observable<Boolean> disconnectPeer(final AdminApi api) {
            throw new UnsupportedOperationException("InterfaceController_disconnectPeer is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> peerStatsConnection(final AdminApi api) {
            throw new UnsupportedOperationException("InterfaceController_peerStats is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> resetPeering(final AdminApi api) {
            throw new UnsupportedOperationException("InterfaceController_resetPeering is not implemented in " + CLASS_NAME);
        }
    }

    public static class IpTunnel {

        public static Observable<Boolean> allowConnection(final AdminApi api) {
            throw new UnsupportedOperationException("IpTunnel_allowConnection is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> connectTo(final AdminApi api) {
            throw new UnsupportedOperationException("IpTunnel_connectTo is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> listConnections(final AdminApi api) {
            throw new UnsupportedOperationException("IpTunnel_listConnections is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> removeConnection(final AdminApi api) {
            throw new UnsupportedOperationException("IpTunnel_removeConnection is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> showConnection(final AdminApi api) {
            throw new UnsupportedOperationException("IpTunnel_showConnection is not implemented in " + CLASS_NAME);
        }
    }

    public static class Janitor {

        public static Observable<Boolean> dumpRumorMill(final AdminApi api) {
            throw new UnsupportedOperationException("Janitor_dumpRumorMill is not implemented in " + CLASS_NAME);
        }
    }

    public static class NodeStore {

        public static Observable<Boolean> dumpTable(final AdminApi api) {
            throw new UnsupportedOperationException("NodeStore_dumpTable is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getLink(final AdminApi api) {
            throw new UnsupportedOperationException("NodeStore_getLink is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getRouteLabel(final AdminApi api) {
            throw new UnsupportedOperationException("NodeStore_getRouteLabel is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> nodeForAddr(final AdminApi api) {
            throw new UnsupportedOperationException("NodeStore_nodeForAddr is not implemented in " + CLASS_NAME);
        }
    }

    public static class RouteGen {

        public static Observable<Boolean> addException(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_addException is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> addLocalPrefix(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_addLocalPrefix is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> addPrefix(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_addPrefix is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> commit(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_commit is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getExceptions(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_getExceptions is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getGeneratedRoutes(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_getGeneratedRoutes is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getLocalPrefixes(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_getLocalPrefixes is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getPrefixes(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_getPrefixes is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> removeException(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_removeException is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> removeLocalPrefix(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_removeLocalPrefix is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> removePrefix(final AdminApi api) {
            throw new UnsupportedOperationException("RouteGen_removePrefix is not implemented in " + CLASS_NAME);
        }
    }

    public static class RouterModule {

        public static Observable<Boolean> findNode(final AdminApi api) {
            throw new UnsupportedOperationException("RouterModule_findNode is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getPeers(final AdminApi api) {
            throw new UnsupportedOperationException("RouterModule_getPeers is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> lookup(final AdminApi api) {
            throw new UnsupportedOperationException("RouterModule_lookup is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> nextHop(final AdminApi api) {
            throw new UnsupportedOperationException("RouterModule_nextHop is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> pingNode(final AdminApi api) {
            throw new UnsupportedOperationException("RouterModule_pingNode is not implemented in " + CLASS_NAME);
        }
    }

    public static class SearchRunner {

        public static Observable<Boolean> search(final AdminApi api) {
            throw new UnsupportedOperationException("SearchRunner_search is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> showActiveSearch(final AdminApi api) {
            throw new UnsupportedOperationException("SearchRunner_showActiveSearch is not implemented in " + CLASS_NAME);
        }
    }

    public static class Security {

        public static Observable<Boolean> checkPermissions(final AdminApi api) {
            throw new UnsupportedOperationException("Security_checkPermissions is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> chroot(final AdminApi api) {
            throw new UnsupportedOperationException("Security_chroot is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> getUser(final AdminApi api) {
            throw new UnsupportedOperationException("Security_getUser is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> nofiles(final AdminApi api) {
            throw new UnsupportedOperationException("Security_nofiles is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> noforks(final AdminApi api) {
            throw new UnsupportedOperationException("Security_noforks is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> seccomp(final AdminApi api) {
            throw new UnsupportedOperationException("Security_seccomp is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> setUser(final AdminApi api) {
            throw new UnsupportedOperationException("Security_setUser is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> setupComplete(final AdminApi api) {
            return Observable.create(new BaseOnSubscribe<Boolean>(api, new Request("Security_setupComplete", null)) {
                @Override
                protected Boolean parseResult(final Map response) {
                    return Boolean.TRUE;
                }
            });
        }
    }

    public static class SessionManager {

        public static Observable<Boolean> getHandles(final AdminApi api) {
            throw new UnsupportedOperationException("SessionManager_getHandles is not implemented in " + CLASS_NAME);
        }

        public static Observable<Boolean> sessionStats(final AdminApi api) {
            throw new UnsupportedOperationException("SessionManager_sessionStats is not implemented in " + CLASS_NAME);
        }
    }

    public static class SwitchPinger {

        public static Observable<Boolean> ping(final AdminApi api) {
            throw new UnsupportedOperationException("SwitchPinger_ping is not implemented in " + CLASS_NAME);
        }
    }

    public static class UdpInterface {

        public static Observable<Long> new0(final AdminApi api) {
            return Observable.create(new BaseOnSubscribe<Long>(api, new Request("UDPInterface_new",
                    new LinkedHashMap<ByteBuffer, Object>() {{
                        put(wrapString("bindAddress"), wrapString("0.0.0.0:0"));
                    }})) {
                @Override
                protected Long parseResult(Map response) {
                    return (Long) response.get(wrapString("interfaceNumber"));
                }
            });
        }

        public static Observable<Boolean> beginConnection(final AdminApi api, final String publicKey, final String address,
                                                          final Long interfaceNumber, final String login, final String password) {
            return Observable.create(new BaseOnSubscribe<Boolean>(api, new Request("UDPInterface_beginConnection",
                    new LinkedHashMap<ByteBuffer, Object>() {{
                        put(wrapString("publicKey"), wrapString(publicKey));
                        put(wrapString("address"), wrapString(address));
                        put(wrapString("interfaceNumber"), interfaceNumber);
                        if (login != null) {
                            put(wrapString("login"), wrapString(login));
                        }
                        put(wrapString("password"), wrapString(password));
                    }})) {
                @Override
                protected Boolean parseResult(Map response) {
                    return Boolean.TRUE;
                }
            });
        }
    }

    public static Observable<Boolean> memory(final AdminApi api) {
        throw new UnsupportedOperationException("memory is not implemented in " + CLASS_NAME);
    }

    public static Observable<Boolean> ping(final AdminApi api) {
        return Observable.create(new BaseOnSubscribe<Boolean>(api, new Request("ping")) {
            @Override
            protected Boolean parseResult(Map response) {
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Create a new UDP datagram socket.
     *
     * @return The socket.
     * @throws SocketException Thrown if failed to create or bind.
     */
    private static DatagramSocket newSocket() throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(SOCKET_TIMEOUT);
        return socket;
    }

    /**
     * Serializes request into bencoded byte array.
     *
     * @param request The request as a map.
     * @return The bencoded byte array.
     * @throws IOException
     */
    private static byte[] serialize(Map request) throws IOException {
        Bencode serializer = new Bencode();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.setRootElement(request);
        serializer.print(output);
        return output.toByteArray();
    }

    /**
     * Parses response from a bencoded byte array.
     *
     * @param data The bencoded data.
     * @return The response as a map.
     * @throws IOException
     */
    private static Map parse(byte[] data) throws IOException {
        StringReader input = new StringReader(new String(data));
        Bencode parser = new Bencode(input);
        return (Map) parser.getRootElement();
    }

    /**
     * Converts bytes to a HEX encoded string.
     *
     * @param bytes The byte array.
     * @return The HEX encoded string.
     */
    private static String bytesToHex(byte[] bytes) {
        String hexString = null;
        if (bytes != null && bytes.length > 0) {
            final char[] hexChars = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xFF;
                hexChars[i * 2] = HEX_ARRAY[v >>> 4];
                hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
            }
            hexString = new String(hexChars);
        }
        return hexString;
    }

    /**
     * Wraps a string into a {@link ByteBuffer}.
     *
     * @param value The string.
     * @return The wrapped {@link ByteBuffer}.
     */
    private static ByteBuffer wrapString(String value) {
        return ByteBuffer.wrap(value.getBytes());
    }

    /**
     * Sends an authenticated request to the {@link AdminApi}.
     *
     * @param request The request.
     * @param api     The {@link AdminApi}.
     * @return The response as a map.
     * @throws NoSuchAlgorithmException Thrown if SHA-256 is missing.
     * @throws IOException              Thrown if request failed.
     */
    private static Map sendAuthenticatedRequest(Request request, AdminApi api) throws NoSuchAlgorithmException, IOException {
        Log.i(TAG, request.name + " sent");

        // Get authentication session cookie.
        String cookie = getCookie(api);

        // Generate dummy hash.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(ADMIN_API_PASSWORD);
        digest.update(cookie.getBytes());
        String dummyHash = bytesToHex(digest.digest());

        // Assemble unsigned request.
        HashMap<ByteBuffer, Object> authenticatedRequest = new LinkedHashMap<>();
        authenticatedRequest.put(wrapString("q"), wrapString("auth"));
        authenticatedRequest.put(wrapString("aq"), wrapString(request.name));
        if (request.args != null) {
            authenticatedRequest.put(wrapString("args"), request.args);
        }
        authenticatedRequest.put(wrapString("hash"), wrapString(dummyHash));
        authenticatedRequest.put(wrapString("cookie"), wrapString(cookie));

        // Sign request.
        byte[] requestBytes = serialize(authenticatedRequest);
        digest.reset();
        digest.update(requestBytes);
        String hash = bytesToHex(digest.digest());
        authenticatedRequest.put(wrapString("hash"), wrapString(hash));

        // Send request.
        return send(authenticatedRequest, api);
    }

    /**
     * Gets an authentication cookie from the {@link AdminApi}.
     *
     * @param api The {@link AdminApi}.
     * @return The cookie.
     * @throws IOException Thrown if request failed.
     */
    private static String getCookie(AdminApi api) throws IOException {
        Map response = send(REQUEST_COOKIE, api);
        Object cookie = response.get(wrapString("cookie"));
        if (cookie instanceof ByteBuffer) {
            return new String(((ByteBuffer) cookie).array());
        } else {
            throw new IOException("Unable to fetch authentication cookie");
        }
    }

    /**
     * Sends a request to the {@link AdminApi}.
     *
     * @param request The request.
     * @param api     The {@link AdminApi}.
     * @return The response as a map.
     * @throws IOException Thrown if request failed.
     */
    private static Map send(Map request, AdminApi api) throws IOException {
        DatagramSocket socket = newSocket();

        byte[] data = serialize(request);
        DatagramPacket dgram = new DatagramPacket(data, data.length, api.mAdminApiAddress, ADMIN_API_PORT);
        socket.send(dgram);

        DatagramPacket responseDgram = new DatagramPacket(new byte[DATAGRAM_LENGTH], DATAGRAM_LENGTH);
        socket.receive(responseDgram);
        socket.close();

        byte[] resData = responseDgram.getData();
        int i = resData.length - 1;
        while (resData[i] == 0) {
            --i;
        }
        byte[] resDataClean = Arrays.copyOf(resData, i + 1);
        return parse(resDataClean);
    }

    /**
     * Model object encapsulating the name and arguments of a request.
     */
    private static class Request {

        private final String name;

        private final LinkedHashMap<ByteBuffer, Object> args;

        private Request(String name, LinkedHashMap<ByteBuffer, Object> args) {
            this.name = name;
            this.args = args;
        }

        private Request(String name) {
            this(name, null);
        }
    }

    /**
     * Abstract class that implements the basic {@link rx.Observable.OnSubscribe} behaviour of each API.
     *
     * @param <T> The return type of the API response.
     */
    private static abstract class BaseOnSubscribe<T> implements Observable.OnSubscribe<T> {

        private static final ByteBuffer ERROR_KEY = wrapString("error");

        private static final String ERROR_NONE = "none";

        private AdminApi mApi;

        private Request mRequest;

        private BaseOnSubscribe(AdminApi api, Request request) {
            mApi = api;
            mRequest = request;
        }

        @Override
        public void call(Subscriber<? super T> subscriber) {
            try {
                final Map response = AdminApi.sendAuthenticatedRequest(mRequest, mApi);

                // Check for error in response.
                final Object error = response.get(ERROR_KEY);
                if (error instanceof ByteBuffer) {
                    String errorString = new String(((ByteBuffer) error).array());
                    if (!ERROR_NONE.equals(errorString)) {
                        Log.e(TAG, mRequest.name + " failed: " + errorString);
                        subscriber.onError(new IOException(mRequest.name + " failed: " + errorString));
                        return;
                    }
                }

                // Parse response for result.
                final T result = parseResult(response);
                if (result != null) {
                    Log.e(TAG, mRequest.name + " completed");
                    subscriber.onNext(result);
                    subscriber.onCompleted();
                } else {
                    Log.e(TAG, "Failed to parse result from " + mRequest.name);
                    subscriber.onError(new IOException("Failed to parse result from " + mRequest.name));
                }
            } catch (SocketTimeoutException e) {
                Log.e(TAG, mRequest.name + " timed out");
                subscriber.onError(e);
            } catch (NoSuchAlgorithmException | IOException e) {
                Log.e(TAG, "Unexpected failure from " + mRequest.name, e);
                subscriber.onError(e);
            }
        }

        /**
         * Implementation must specify how to parse the response and return a value of type {@link T}.
         * Returning {@code null} will lead to {@link Subscriber#onError(Throwable)} being called.
         *
         * @param response The response from the API as a {@link Map}.
         * @return A value of type {@link T}.
         */
        protected abstract T parseResult(final Map response);
    }
}
