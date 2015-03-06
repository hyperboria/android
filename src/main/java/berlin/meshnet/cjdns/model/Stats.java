package berlin.meshnet.cjdns.model;

/**
 * Model for the transient statistics about a node.
 */
public class Stats {

    public final String version;

    public final boolean isActive;

    public final long lastActive;

    public final int linkCount;

    public final int bytesIn;

    public final int bytesOut;

    public final int bandwidthIn;

    public final int bandwidthOut;

    public Stats(String version, boolean isActive, long lastActive, int linkCount,
                 int bytesIn, int bytesOut, int bandwidthIn, int bandwidthOut) {
        this.version = version;
        this.isActive = isActive;
        this.lastActive = lastActive;
        this.linkCount = linkCount;
        this.bytesIn = bytesIn;
        this.bytesOut = bytesOut;
        this.bandwidthIn = bandwidthIn;
        this.bandwidthOut = bandwidthOut;
    }

    /**
     * Model for the transient statistics about the self node.
     */
    public static class Me extends Stats {

        public final int linkCountActive;

        public Me(String version, boolean isActive, long lastActive, int linkCount,
                  int bytesIn, int bytesOut, int bandwidthIn, int bandwidthOut, int linkCountActive) {
            super(version, isActive, lastActive, linkCount, bytesIn, bytesOut, bandwidthIn, bandwidthOut);
            this.linkCountActive = linkCountActive;
        }
    }
}
