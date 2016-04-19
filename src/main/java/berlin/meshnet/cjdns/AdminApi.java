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
import java.util.HashMap;
import java.util.Map;

/**
 * API for administration of the cjdns node.
 */
class AdminApi {

    /*
        AdminLog_logMany(count)
        AdminLog_subscribe(line='', file=0, level=0)
        AdminLog_subscriptions()
        AdminLog_unsubscribe(streamId)
        Admin_asyncEnabled()
        Admin_availableFunctions(page='')
        Allocator_bytesAllocated()
        Allocator_snapshot(includeAllocations='')
        AuthorizedPasswords_add(password, user=0, ipv6=0)
        AuthorizedPasswords_list()
        AuthorizedPasswords_remove(user)
        Core_exit()
        Core_initTunnel(desiredTunName=0)
        Core_pid()
        ETHInterface_beacon(interfaceNumber='', state='')
        ETHInterface_beginConnection(publicKey, macAddress, interfaceNumber='', login=0, password=0)
        ETHInterface_listDevices()
        ETHInterface_new(bindDevice)
        InterfaceController_disconnectPeer(pubkey)
        InterfaceController_peerStats(page='')
        InterfaceController_resetPeering(pubkey=0)
        IpTunnel_allowConnection(publicKeyOfAuthorizedNode, ip4Alloc='', ip6Alloc='', ip4Address=0, ip4Prefix='', ip6Address=0, ip6Prefix='')
        InterfaceController_resetPeering(pubkey=0)                                                                                                                                    [0/229]
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

    /**
     * UDP datagram socket timeout in milliseconds.
     */
    public static final int SOCKET_TIMEOUT = 5000;

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
    private AdminApi(InetAddress address, int port, byte[] password) {
        mAddress = address;
        mPort = port;
        mPassword = password;
    }

    public String getBind() {
        return mAddress.getHostAddress() + ":" + mPort;
    }

    public int corePid() throws IOException {
        // try {
        HashMap<ByteBuffer, Object> request = new HashMap<>();
        request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("Core_pid".getBytes()));

        Map response = send(request);
        Long pid = (Long) response.get(ByteBuffer.wrap("pid".getBytes()));

        return pid.intValue();
        // } catch (IOException e) {
        //     return 0;
        // }
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
}
