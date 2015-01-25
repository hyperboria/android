package berlin.meshnet.cjdns.model;

public class Node {

    public final String publicKey;

    public final int linkCount;

    public Node(String publicKey, int linkCount) {
        this.publicKey = publicKey;
        this.linkCount = linkCount;
    }

    public final String getAddress() {
        return "fc12:34::";
    }
}
