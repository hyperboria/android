package berlin.meshnet.cjdns.model;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Date;

/**
 * Base representation of a node.
 */
public abstract class Node {

    public final String name;

    public final String publicKey;

    public final String address;

    public Node(String name, String publicKey) {
        this.name = name;
        this.publicKey = publicKey;
        this.address = "fc00:0000:0000:0000:0000:0000:0000:0000";
    }

    /**
     * Transient statistics about a node.
     */
    public static class Stats {

        private String version;

        private boolean isActive;

        private long lastActive;

        private int linkCount;

        private int bytesIn;

        private int bytesOut;

        private int bandwidthIn;

        private int bandwidthOut;

        public String getVersion() {
            return version;
        }

        public boolean isActive() {
            return isActive;
        }

        public long getLastActive() {
            return lastActive;
        }

        public String getLastActive(Context context) {
            return DateFormat.getTimeFormat(context).format(new Date(lastActive));
        }

        public int getLinkCount() {
            return linkCount;
        }

        public int getBytesIn() {
            return bytesIn;
        }

        public int getBytesOut() {
            return bytesOut;
        }

        public int getBandwidthIn() {
            return bandwidthIn;
        }

        public int getBandwidthOut() {
            return getBandwidthOut();
        }
    }
}
