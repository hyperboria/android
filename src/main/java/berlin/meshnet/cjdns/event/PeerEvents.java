package berlin.meshnet.cjdns.event;

import berlin.meshnet.cjdns.model.Node;

public class PeerEvents {

    public static class Create {
    }

    public static class Update {

        public final Node.Peer mPeer;

        public Update(Node.Peer peer) {
            mPeer = peer;
        }
    }

    public static class Remove {

        public final int mId;

        public Remove(int id) {
            mId = id;
        }
    }

    public static class New {

        public final Node.Peer mPeer;

        public New(Node.Peer peer) {
            mPeer = peer;
        }
    }
}
