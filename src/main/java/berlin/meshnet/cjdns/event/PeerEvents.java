package berlin.meshnet.cjdns.event;

import berlin.meshnet.cjdns.model.Node;

/**
 * Events associated with {@link berlin.meshnet.cjdns.model.Node.Peer}.
 */
public interface PeerEvents {

    /**
     * Request to create a new {@link berlin.meshnet.cjdns.model.Node.Peer}.
     */
    static class Create {
    }

    /**
     * Request to update a {@link berlin.meshnet.cjdns.model.Node.Peer}.
     */
    static class Update {

        public final Node.Peer mPeer;

        public Update(Node.Peer peer) {
            mPeer = peer;
        }
    }

    /**
     * Request to remove a {@link berlin.meshnet.cjdns.model.Node.Peer}.
     */
    static class Remove {

        public final Node.Peer mPeer;

        public Remove(Node.Peer peer) {
            mPeer = peer;
        }
    }
}
