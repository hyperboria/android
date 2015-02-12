package berlin.meshnet.cjdns.model;

/**
 * Model for the self node.
 */
public class Me {

    public final String name;

    public final Node node;

    public Me(String name, Node node) {
        this.name = name;
        this.node = node;
    }
}
