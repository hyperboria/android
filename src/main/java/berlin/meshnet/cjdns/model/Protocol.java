package berlin.meshnet.cjdns.model;

/**
 * Model for connectivity protocol.
 */
public class Protocol {

    public final Interface transportInterface;

    public final Link link;

    public Protocol(Interface transportInterface, Link link) {
        this.transportInterface = transportInterface;
        this.link = link;
    }

    /**
     * The transport interface.
     */
    public enum Interface {
        udp,
        eth
    }

    /**
     * The physical link.
     */
    public enum Link {
        wifiDirect,
        bluetooth,
        overlay
    }
}
