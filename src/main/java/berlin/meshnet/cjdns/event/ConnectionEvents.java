package berlin.meshnet.cjdns.event;

public class ConnectionEvents {

    public static class List {

        public final int mPeerId;

        public List(int peerId) {
            mPeerId = peerId;
        }
    }

    public static class Remove {

        public final int mPeerId;

        public final int mConnectionId;

        public Remove(int peerId, int connectionId) {
            mPeerId = peerId;
            mConnectionId = connectionId;
        }
    }
}
