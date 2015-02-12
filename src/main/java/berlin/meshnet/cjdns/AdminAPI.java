package berlin.meshnet.cjdns;

import org.bitlet.wetorrent.bencode.Bencode;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;

import berlin.meshnet.cjdns.model.Node;

public class AdminAPI {
    public static final int TIMEOUT = 5000;
    public static final int DGRAM_LENGTH = 4096;

    private InetAddress address;
    private int port;
    private byte[] password;

    public AdminAPI(InetAddress address, int port, byte[] password)
    {
        this.address = address;
        this.port = port;
        this.password = password;
    }

    public String getBind()
    {
        return this.address.getHostAddress() + ":" + this.port;
    }

    public int Core_pid() throws IOException
    {
        // try {
            HashMap<ByteBuffer, Object> request = new HashMap<ByteBuffer, Object>();
            request.put(ByteBuffer.wrap("q".getBytes()), ByteBuffer.wrap("Core_pid".getBytes()));

            Map response = perform(request);
            Long pid = (Long)response.get(ByteBuffer.wrap("pid".getBytes()));

            return pid.intValue();
        // } catch (IOException e) {
        //     return 0;
        // }
    }

    public Node NodeStore_nodeForAddr() throws IOException
    {
        return new Node("Some Peer Node", "foo.k", 123);
    }

    public Map perform(Map request) throws IOException
    {
        DatagramSocket socket = newSocket();

        byte[] data = serialize(request);
        DatagramPacket dgram = new DatagramPacket(data, data.length, this.address, this.port);
        socket.send(dgram);

        DatagramPacket responseDgram = new DatagramPacket(new byte[DGRAM_LENGTH], DGRAM_LENGTH);
        socket.receive(responseDgram);
        socket.close();

        Map response = parse(responseDgram.getData());
        Log.i("cjdns_AdminAPI", "response: " + response.toString());
        return response;
    }

    protected byte[] serialize(Map request) throws IOException
    {
        Bencode serializer = new Bencode();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serializer.setRootElement(request);
        serializer.print(output);

        return output.toByteArray();
    }

    protected Map parse(byte[] data) throws IOException
    {
        StringReader input = new StringReader(new String(data));
        Bencode parser = new Bencode(input);

        return (Map)parser.getRootElement();
    }

    protected DatagramSocket newSocket() throws SocketException
    {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);
        return socket;
    }
}
