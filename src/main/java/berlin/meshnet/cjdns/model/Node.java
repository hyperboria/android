package berlin.meshnet.cjdns.model;

public class Node {

    public final String name;

    public final String publicKey;

    public final String address;

    public final int linkCount;

    public Node(String name, String publicKey, int linkCount) {
        this.name = name;
        this.publicKey = publicKey;
        this.address = "fc00:0000:0000:0000:0000:0000:0000:0000";
        this.linkCount = linkCount;
    }
}
