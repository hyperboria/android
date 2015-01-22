package berlin.meshnet.cjdns.model;

/**
 * Model for the self node.
 */
public class Me {

    public final String name;

    public final String address;

    public final String publicKey;

    public Me(String name, String address, String publicKey) {
        this.name = name;
        this.address = address;
        this.publicKey = publicKey;
    }
}
