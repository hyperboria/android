package berlin.meshnet.cjdns.model;

import android.content.res.Resources;

import berlin.meshnet.cjdns.R;

/**
 * Immutable model object for a connectivity protocol.
 */
public class Protocol {

    public final Interface transportInterface;

    public final Link link;

    public Protocol(Interface transportInterface, Link link) {
        this.transportInterface = transportInterface;
        this.link = link;
    }

    /**
     * Formats the protocol description.
     */
    public static String formatDescription(Resources res, Protocol protocol) {
        String interfaceString = null;
        String linkString = null;
        switch (protocol.transportInterface) {
            case udp:
                interfaceString = res.getString(R.string.interface_udp);
                break;
            case eth:
                interfaceString = res.getString(R.string.interface_eth);
                break;
        }
        switch (protocol.link) {
            case wifiDirect:
                linkString = res.getString(R.string.link_wifi_direct);
                break;
            case bluetooth:
                linkString = res.getString(R.string.link_bluetooth);
                break;
            case overlay:
                linkString = res.getString(R.string.link_overlay);
                break;
        }
        return res.getString(R.string.protocol_description, interfaceString, linkString);
    }

    /**
     * The list of transport interfaces.
     */
    public enum Interface {
        udp,
        eth
    }

    /**
     * The list of physical links.
     */
    public enum Link {
        wifiDirect,
        bluetooth,
        overlay
    }
}
