package berlin.meshnet.cjdns.model;

/**
 * Model for a peer node.
 */
public class Peer extends Node {

    public final Protocol[] protocols;

    public final Node.Stats stats;

    public Peer(String name, String publicKey, Protocol[] protocols) {
        super(name, publicKey);
        this.protocols = protocols;
        this.stats = new Stats();
    }
}
