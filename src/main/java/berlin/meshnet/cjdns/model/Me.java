package berlin.meshnet.cjdns.model;

/**
 * Model for the self node.
 */
public class Me extends Node {

    public final String privateKey;

    public final Me.Stats stats;

    public Me(String name, String publicKey, String privateKey) {
        super(name, publicKey);
        this.privateKey = privateKey;
        this.stats = new Stats();
    }

    /**
     * Transient statistics about the self node.
     */
    public static class Stats extends Node.Stats {

        private int linkCountActive;

        public int getLinkCountActive() {
            return linkCountActive;
        }
    }
}
