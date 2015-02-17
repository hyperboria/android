package berlin.meshnet.cjdns.model;

/**
 * Model for a node.
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
     * Model for the self node.
     */
    public static class Me extends Node {

        public final String privateKey;

        public final Stats.Me stats;

        public Me(String name, String publicKey, String privateKey) {
            super(name, publicKey);
            this.privateKey = privateKey;
            this.stats = new Stats.Me();
        }
    }

    /**
     * Model for a peer node.
     */
    public static class Peer extends Node {

        public final int id;

        private Credential[] outgoingConnections;

        public final Stats stats;

        public Peer(int id, String name, String publicKey, Credential[] outgoingConnections) {
            super(name, publicKey);
            this.id = id;
            this.outgoingConnections = outgoingConnections;
            this.stats = new Stats();
        }

        public Credential[] getOutgoingConnections() {
            return outgoingConnections;
        }

        public void setOutgoingConnections(Credential[] outgoingConnections) {
            this.outgoingConnections = outgoingConnections;
        }
    }
}
