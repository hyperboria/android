package berlin.meshnet.cjdns.event;

/**
 * Events associated with the application state.
 */
public interface ApplicationEvents {

    /**
     * Request to start the {@link berlin.meshnet.cjdns.CjdnsService}.
     */
    static class StartCjdnsService {
    }

    /**
     * Request to stop the {@link berlin.meshnet.cjdns.CjdnsService}.
     */
    static class StopCjdnsService {
    }

    /**
     * Request to change the page.
     */
    static class ChangePage {

        public final String mSelectedContent;

        public ChangePage(String selectedContent) {
            mSelectedContent = selectedContent;
        }
    }

    /**
     * Request to list the connections of a {@link berlin.meshnet.cjdns.model.Node.Peer}.
     */
    static class ListConnections {

        public final int mPeerId;

        public ListConnections(int peerId) {
            mPeerId = peerId;
        }
    }

    /**
     * Request to exchange a {@link berlin.meshnet.cjdns.model.Credential}.
     */
    static class ExchangeCredential {

        public final Type mType;

        public final String mMessage;

        public ExchangeCredential(Type type, String message) {
            mType = type;
            mMessage = message;
        }

        /**
         * The list of exchange types.
         */
        public enum Type {
            target,
            broadcast
        }
    }
}