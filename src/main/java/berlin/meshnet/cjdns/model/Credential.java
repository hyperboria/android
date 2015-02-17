package berlin.meshnet.cjdns.model;

/**
 * Model for a peering credential.
 */
public class Credential {

    public final int id;

    public final String label;

    public final Protocol protocol;

    public final String password;

    public Credential(int id, String label, Protocol protocol, String password) {
        this.id = id;
        this.label = label;
        this.protocol = protocol;
        this.password = password;
    }

    /**
     * Model for a peering credential authorized for the self node.
     */
    public static class Authorized extends Credential {

        private boolean isAllowed;

        public Authorized(int id, String label, Protocol protocol, String password, boolean isAllowed) {
            super(id, label, protocol, password);
            this.isAllowed = isAllowed;
        }

        public boolean isAllowed() {
            return isAllowed;
        }

        public void setAllowed(boolean isAllowed) {
            this.isAllowed = isAllowed;
        }
    }
}
