package berlin.meshnet.cjdns.model;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Date;

/**
 * Model for the transient statistics about a node.
 */
public class Stats {

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
        return bandwidthOut;
    }

    /**
     * Model for the transient statistics about the self node.
     */
    public static class Me extends Stats {

        private int linkCountActive;

        public int getLinkCountActive() {
            return linkCountActive;
        }
    }
}
