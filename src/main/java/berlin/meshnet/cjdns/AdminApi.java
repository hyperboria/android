package berlin.meshnet.cjdns;

import android.util.Log;

import org.bitlet.wetorrent.bencode.Bencode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API for administration of the cjdns node.
 */
class AdminApi {

    /*
//        AdminLog_logMany(count)
//        AdminLog_subscribe(line='', file=0, level=0)
//        AdminLog_subscriptions()
//        AdminLog_unsubscribe(streamId)
//        Admin_asyncEnabled()
//        Admin_availableFunctions(page='')
//        Allocator_bytesAllocated()
//        Allocator_snapshot(includeAllocations='')
//        AuthorizedPasswords_add(password, user=0, ipv6=0)
//        AuthorizedPasswords_list()
//        AuthorizedPasswords_remove(user)
        Core_exit()
        Core_initTunfd(tunfd, type='')
//        Core_initTunnel(desiredTunName=0)
        Core_pid()
//        ETHInterface_beacon(interfaceNumber='', state='')
//        ETHInterface_beginConnection(publicKey, macAddress, interfaceNumber='', login=0, password=0)
//        ETHInterface_listDevices()
//        ETHInterface_new(bindDevice)
        FileNo_import(path, type=0)
        InterfaceController_disconnectPeer(pubkey)
        InterfaceController_peerStats(page='')
        InterfaceController_resetPeering(pubkey=0)
        IpTunnel_allowConnection(publicKeyOfAuthorizedNode, ip4Alloc='', ip6Alloc='', ip4Address=0, ip4Prefix='', ip6Address=0, ip6Prefix='')
        IpTunnel_connectTo(publicKeyOfNodeToConnectTo)
        IpTunnel_listConnections()
        IpTunnel_removeConnection(connection)
        IpTunnel_showConnection(connection)
        Janitor_dumpRumorMill(mill, page)
        NodeStore_dumpTable(page)
        NodeStore_getLink(linkNum, parent=0)
        NodeStore_getRouteLabel(pathParentToChild, pathToParent)
        NodeStore_nodeForAddr(ip=0)
        RouteGen_addException(route)
        RouteGen_addLocalPrefix(route)
        RouteGen_addPrefix(route)
        RouteGen_commit(tunName)
        RouteGen_getExceptions(ip6='', page='')
        RouteGen_getGeneratedRoutes(ip6='', page='')
        RouteGen_getLocalPrefixes(ip6='', page='')
        RouteGen_getPrefixes(ip6='', page='')
        RouteGen_removeException(route)
        RouteGen_removeLocalPrefix(route)
        RouteGen_removePrefix(route)
        RouterModule_findNode(nodeToQuery, target, timeout='')
        RouterModule_getPeers(path, nearbyPath=0, timeout='')
        RouterModule_lookup(address)
        RouterModule_nextHop(nodeToQuery, target, timeout='')
        RouterModule_pingNode(path, timeout='')
        SearchRunner_search(ipv6, maxRequests='')
        SearchRunner_showActiveSearch(number)
        Security_checkPermissions()
        Security_chroot(root)
        Security_getUser(user=0)
        Security_nofiles()
        Security_noforks()
        Security_seccomp()
        Security_setUser(keepNetAdmin, uid, gid='')
        Security_setupComplete()
        SessionManager_getHandles(page='')
        SessionManager_sessionStats(handle)
        SwitchPinger_ping(path, data=0, keyPing='', timeout='')
        UDPInterface_beginConnection(publicKey, address, interfaceNumber='', login=0, password=0)
        UDPInterface_new(bindAddress=0)
        memory()
        ping()
    */

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /**
     * UDP datagram socket timeout in milliseconds.
     */
    // TODO FIX
    public static final int SOCKET_TIMEOUT = 5000000;

    /**
     * UDP datagram length.
     */
    public static final int DATAGRAM_LENGTH = 4096;

    /**
     * The local IP address to bind the admin RPC server.
     */
    private InetAddress mAddress;

    /**
     * The port to bind the admin RPC server.
     */
    private int mPort;

    /**
     * The password for authenticated requests.
     */
    private byte[] mPassword;

    /**
     * Creates an {@link AdminApi} object from the
     *
     * @param cjdrouteConf
     * @return
     * @throws IOException
     * @throws JSONException
     */
    static AdminApi from(JSONObject cjdrouteConf) throws IOException, JSONException {
        JSONObject admin = cjdrouteConf.getJSONObject("admin");
        String[] bind = admin.getString("bind").split(":");

        InetAddress address = InetAddress.getByName(bind[0]);
        int port = Integer.parseInt(bind[1]);
        byte[] password = admin.getString("password").getBytes();

        return new AdminApi(address, port, password);
    }

    /**
     * Constructor.
     *
     * @param address  The local IP address to bind the admin RPC server.
     * @param port     The port to bind the admin RPC server.
     * @param password The password for authenticated requests.
     */
    public AdminApi(InetAddress address, int port, byte[] password) {
        mAddress = address;
        mPort = port;
        mPassword = password;
    }

    public String getBind() {
        return mAddress.getHostAddress() + ":" + mPort;
    }

    public void functions(long page) throws IOException {
        // Get cookie.
        HashMap<ByteBuffer, Object> request = new LinkedHashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("cookie".getBytes()));
        Map response = send(request);
        String cookie = new String(((ByteBuffer) response.get(ByteBuffer.wrap("cookie".getBytes()))).array());
        Log.d("BEN", "Cookie: " + cookie);

        HashMap<ByteBuffer, Object> request3 = new LinkedHashMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mPassword);
            digest.update(cookie.getBytes());
            byte[] dummyHash = digest.digest();
            Log.d("BEN", "dummyHash: " + bytesToHex(dummyHash));

            request3.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("auth".getBytes()));
            request3.put(ByteBuffer.wrap("aq".getBytes()), ByteBuffer.wrap("Admin_availableFunctions".getBytes()));

            // Args.
            HashMap<ByteBuffer, Object> args = new LinkedHashMap<>();
            args.put(ByteBuffer.wrap("page".getBytes()), new Long(page));
            request3.put(ByteBuffer.wrap("args".getBytes()), args);

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(dummyHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
            byte[] requestBytes = serialize(request3);
            Log.d("BEN", "requestBytes: " + new String(requestBytes));

            MessageDigest digest2 = MessageDigest.getInstance("SHA-256");
            digest2.update(requestBytes);
            byte[] actualHash = digest2.digest();
            Log.d("BEN", "actualHash: " + bytesToHex(actualHash));

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(actualHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Map response3 = send(request3);
//        String error = new String(((ByteBuffer) response3.get(ByteBuffer.wrap("error".getBytes()))).array());
        Map availableFunctions = (Map) response3.get(ByteBuffer.wrap("availableFunctions".getBytes()));
//        Log.d("BEN", "error: " + error);
        for (Object o : availableFunctions.keySet()) {
            Log.d("BEN", new String(((ByteBuffer) o).array()) + " ->");
            Map func = (Map) availableFunctions.get(o);
            for (Object p : func.keySet()) {
                Log.d("BEN", "  " + new String(((ByteBuffer) p).array()));
//                Log.d("BEN", new String(((ByteBuffer) func.get(p)).array()));
            }
        }
    }

    public int corePid() throws IOException {
        // Get cookie.
        HashMap<ByteBuffer, Object> request = new LinkedHashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("cookie".getBytes()));
        Map response = send(request);
        String cookie = new String(((ByteBuffer) response.get(ByteBuffer.wrap("cookie".getBytes()))).array());
        Log.d("BEN", "Cookie: " + cookie);

        HashMap<ByteBuffer, Object> request3 = new LinkedHashMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mPassword);
            digest.update(cookie.getBytes());
            byte[] dummyHash = digest.digest();
            Log.d("BEN", "dummyHash: " + bytesToHex(dummyHash));

            request3.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("auth".getBytes()));
            request3.put(ByteBuffer.wrap("aq".getBytes()), ByteBuffer.wrap("Core_pid".getBytes()));
            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(dummyHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
            byte[] requestBytes = serialize(request3);
            Log.d("BEN", "requestBytes: " + new String(requestBytes));

            MessageDigest digest2 = MessageDigest.getInstance("SHA-256");
            digest2.update(requestBytes);
            byte[] actualHash = digest2.digest();
            Log.d("BEN", "actualHash: " + bytesToHex(actualHash));

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(actualHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Map response3 = send(request3);
        Long pid = (Long) response3.get(ByteBuffer.wrap("pid".getBytes()));
        Log.d("BEN", "PID: " + pid);

        return pid.intValue();
    }

    public int udpInterfaceNew() throws IOException {
        // Get cookie.
        HashMap<ByteBuffer, Object> request = new LinkedHashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("cookie".getBytes()));
        Map response = send(request);
        String cookie = new String(((ByteBuffer) response.get(ByteBuffer.wrap("cookie".getBytes()))).array());
        Log.d("BEN", "Cookie: " + cookie);

        HashMap<ByteBuffer, Object> request3 = new LinkedHashMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mPassword);
            digest.update(cookie.getBytes());
            byte[] dummyHash = digest.digest();
            Log.d("BEN", "dummyHash: " + bytesToHex(dummyHash));

            request3.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("auth".getBytes()));
            request3.put(ByteBuffer.wrap("aq".getBytes()), ByteBuffer.wrap("UDPInterface_new".getBytes()));

            // Args.
            HashMap<ByteBuffer, Object> args = new LinkedHashMap<>();
            // TODO Replace port.
            args.put(ByteBuffer.wrap("bindAddress".getBytes()), ByteBuffer.wrap("127.0.0.1:0".getBytes()));
            request3.put(ByteBuffer.wrap("args".getBytes()), args);

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(dummyHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
            byte[] requestBytes = serialize(request3);
            Log.d("BEN", "requestBytes: " + new String(requestBytes));

            MessageDigest digest2 = MessageDigest.getInstance("SHA-256");
            digest2.update(requestBytes);
            byte[] actualHash = digest2.digest();
            Log.d("BEN", "actualHash: " + bytesToHex(actualHash));

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(actualHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Map response3 = send(request3);
        String error = new String(((ByteBuffer) response3.get(ByteBuffer.wrap("error".getBytes()))).array());
        interfaceNumber = (Long) response3.get(ByteBuffer.wrap("interfaceNumber".getBytes()));
        Log.d("BEN", "error: " + error + " interfaceNumber: " + interfaceNumber);

        return 1;
    }

    Long interfaceNumber;

    public int udpInterfaceBeginConnection() throws IOException {
        // Get cookie.
        HashMap<ByteBuffer, Object> request = new LinkedHashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("cookie".getBytes()));
        Map response = send(request);
        String cookie = new String(((ByteBuffer) response.get(ByteBuffer.wrap("cookie".getBytes()))).array());
        Log.d("BEN", "Cookie: " + cookie);

        HashMap<ByteBuffer, Object> request3 = new LinkedHashMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mPassword);
            digest.update(cookie.getBytes());
            byte[] dummyHash = digest.digest();
            Log.d("BEN", "dummyHash: " + bytesToHex(dummyHash));

            request3.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("auth".getBytes()));
            request3.put(ByteBuffer.wrap("aq".getBytes()), ByteBuffer.wrap("UDPInterface_beginConnection".getBytes()));

            // Args.

//            /**
//             * Default public peer interface. TODO Remove.
//             */
//            private static final String DEFAULT_PEER_INTERFACE = "104.200.29.163:53053";
//
//            /**
//             * Default public peer credentials. TODO Remove.
//             */
//            private static final String DEFAULT_PEER_CREDENTIALS = "{\n" +
//                    "  \"publicKey\": \"1941p5k8qqvj17vjrkb9z97wscvtgc1vp8pv1huk5120cu42ytt0.k\",\n" +
//                    "  \"password\": \"8fVMl0oo6QI6wKeMneuY26x1MCgRemg\",\n" +
//                    "  \"contact\": \"ansuz@transitiontech.ca\",\n" +
//                    "  \"location\": \"Newark,NJ,USA\"\n" +
//                    "}";
            HashMap<ByteBuffer, Object> args = new LinkedHashMap<>();
            args.put(ByteBuffer.wrap("publicKey".getBytes()), ByteBuffer.wrap("1941p5k8qqvj17vjrkb9z97wscvtgc1vp8pv1huk5120cu42ytt0.k".getBytes()));
            args.put(ByteBuffer.wrap("address".getBytes()), ByteBuffer.wrap("104.200.29.163:53053".getBytes()));
            args.put(ByteBuffer.wrap("interfaceNumber".getBytes()), interfaceNumber);
//            args.put(ByteBuffer.wrap("login".getBytes()), ByteBuffer.wrap("ansuz".getBytes()));
            args.put(ByteBuffer.wrap("password".getBytes()), ByteBuffer.wrap("8fVMl0oo6QI6wKeMneuY26x1MCgRemg".getBytes()));
            request3.put(ByteBuffer.wrap("args".getBytes()), args);

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(dummyHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
            byte[] requestBytes = serialize(request3);
            Log.d("BEN", "requestBytes: " + new String(requestBytes));

            MessageDigest digest2 = MessageDigest.getInstance("SHA-256");
            digest2.update(requestBytes);
            byte[] actualHash = digest2.digest();
            Log.d("BEN", "actualHash: " + bytesToHex(actualHash));

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(actualHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Map response3 = send(request3);
        String error = new String(((ByteBuffer) response3.get(ByteBuffer.wrap("error".getBytes()))).array());
        Log.d("BEN", "error: " + error);

        return 1;
    }

    public Long fileNoImport(String path) throws IOException {
        // Get cookie.
        HashMap<ByteBuffer, Object> request = new LinkedHashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("cookie".getBytes()));
        Map response = send(request);
        String cookie = new String(((ByteBuffer) response.get(ByteBuffer.wrap("cookie".getBytes()))).array());
        Log.d("BEN", "Cookie: " + cookie);

        HashMap<ByteBuffer, Object> request3 = new LinkedHashMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mPassword);
            digest.update(cookie.getBytes());
            byte[] dummyHash = digest.digest();
            Log.d("BEN", "dummyHash: " + bytesToHex(dummyHash));

            request3.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("auth".getBytes()));
            request3.put(ByteBuffer.wrap("aq".getBytes()), ByteBuffer.wrap("FileNo_import".getBytes()));

            // Args.
            HashMap<ByteBuffer, Object> args = new LinkedHashMap<>();
            args.put(ByteBuffer.wrap("path".getBytes()), ByteBuffer.wrap(path.getBytes()));
            args.put(ByteBuffer.wrap("type".getBytes()), new Long(1L));
            request3.put(ByteBuffer.wrap("args".getBytes()), args);

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(dummyHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
            byte[] requestBytes = serialize(request3);
            Log.d("BEN", "requestBytes: " + new String(requestBytes));

            MessageDigest digest2 = MessageDigest.getInstance("SHA-256");
            digest2.update(requestBytes);
            byte[] actualHash = digest2.digest();
            Log.d("BEN", "actualHash: " + bytesToHex(actualHash));

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(actualHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Map response3 = send(request3);
        String error = new String(((ByteBuffer) response3.get(ByteBuffer.wrap("error".getBytes()))).array());
        Long tunfd = (Long) response3.get(ByteBuffer.wrap("tunfd".getBytes()));
        Long type = (Long) response3.get(ByteBuffer.wrap("type".getBytes()));
        Log.d("BEN", "error: " + error + " tunfd: " + tunfd + " type: " + type);

        return tunfd;
    }

    public int coreInitTunFd(Long tunFd, Long type) throws IOException {
        // Get cookie.
        HashMap<ByteBuffer, Object> request = new LinkedHashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("cookie".getBytes()));
        Map response = send(request);
        String cookie = new String(((ByteBuffer) response.get(ByteBuffer.wrap("cookie".getBytes()))).array());
        Log.d("BEN", "Cookie: " + cookie);

        HashMap<ByteBuffer, Object> request3 = new LinkedHashMap<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(mPassword);
            digest.update(cookie.getBytes());
            byte[] dummyHash = digest.digest();
            Log.d("BEN", "dummyHash: " + bytesToHex(dummyHash));

            request3.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("auth".getBytes()));
            request3.put(ByteBuffer.wrap("aq".getBytes()), ByteBuffer.wrap("Core_initTunfd".getBytes()));

            // Args.
            HashMap<ByteBuffer, Object> args = new LinkedHashMap<>();
            args.put(ByteBuffer.wrap("tunfd".getBytes()), tunFd);
            args.put(ByteBuffer.wrap("type".getBytes()), type);
            request3.put(ByteBuffer.wrap("args".getBytes()), args);

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(dummyHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
            byte[] requestBytes = serialize(request3);
            Log.d("BEN", "requestBytes: " + new String(requestBytes));

            MessageDigest digest2 = MessageDigest.getInstance("SHA-256");
            digest2.update(requestBytes);
            byte[] actualHash = digest2.digest();
            Log.d("BEN", "actualHash: " + bytesToHex(actualHash));

            request3.put(ByteBuffer.wrap("hash".getBytes()), ByteBuffer.wrap(bytesToHex(actualHash).getBytes()));
            request3.put(ByteBuffer.wrap("cookie".getBytes()), ByteBuffer.wrap(cookie.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Map response3 = send(request3);
        String error = new String(((ByteBuffer) response3.get(ByteBuffer.wrap("error".getBytes()))).array());
        Log.d("BEN", "error: " + error);

        return 1;
    }

    public int runStuff() throws IOException {
//        functions(0L);
//        functions(1L);
//        functions(2L);
//        functions(3L);
//        functions(4L);
//        functions(5L);
//        functions(6L);
//        functions(7L);
//        functions(8L);
//        functions(9L);
//        functions(10L);

        // TODO Not returning interfaceNumber atm.
//        udpInterfaceNew();
//        udpInterfaceBeginConnection();

//        Long tunFd = fileNoImport();
//        coreInitTunFd(tunFd, 1L);

        return corePid();

        /*
        // try {
        HashMap<ByteBuffer, Object> request = new HashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("Core_pid".getBytes()));

        Map response = send(request);
        Long pid = (Long) response.get(ByteBuffer.wrap("pid".getBytes()));

        return pid.intValue();
        // } catch (IOException e) {
        //     return 0;
        // }
        */
    }

    /**
     * Sends a request to the {@link AdminApi} socket.
     *
     * @param request The {@link AdminApi} request.
     * @return The response as a map.
     * @throws IOException
     */
    private Map send(Map request) throws IOException {
        DatagramSocket socket = newSocket();

        byte[] data = serialize(request);
        DatagramPacket dgram = new DatagramPacket(data, data.length, mAddress, mPort);
        socket.send(dgram);

        DatagramPacket responseDgram = new DatagramPacket(new byte[DATAGRAM_LENGTH], DATAGRAM_LENGTH);
        socket.receive(responseDgram);
        socket.close();

        Map response = parse(responseDgram.getData());
        Log.i("cjdns_AdminAPI", "response: " + response.toString());
        return response;
    }


    /**
     * Create a new UDP datagram socket.
     *
     * @return The socket.
     * @throws SocketException Thrown if failed to create or bind.
     */
    private DatagramSocket newSocket() throws SocketException {
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
    private byte[] serialize(Map request) throws IOException {
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
    private Map parse(byte[] data) throws IOException {
        StringReader input = new StringReader(new String(data));
        Bencode parser = new Bencode(input);
        return (Map) parser.getRootElement();
    }

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
}
