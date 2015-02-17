package berlin.meshnet.cjdns.producer;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import berlin.meshnet.cjdns.event.PeerEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.model.Protocol;

/**
 * Abstract class that produces the {@link berlin.meshnet.cjdns.producer.PeerListProducer.PeerList}.
 */
public abstract class PeerListProducer {

    public PeerListProducer(Bus bus) {
        bus.register(this);
    }

    /**
     * Produces the {@link berlin.meshnet.cjdns.producer.PeerListProducer.PeerList} to any subscribers. Must be annotated with {@link @Produce}.
     *
     * @return A {@link berlin.meshnet.cjdns.producer.PeerListProducer.PeerList}.
     */
    public abstract PeerList produce();

    /**
     * A list of peers.
     */
    public static class PeerList extends ArrayList<Node.Peer> {
    }

    /**
     * Mock implementation of a {@link berlin.meshnet.cjdns.producer.PeerListProducer}.
     */
    public static class Mock extends PeerListProducer {

        private PeerList mPeerList = new PeerList() {{
            add(new Node.Peer(0, "Alice", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", new Credential[]{
                    new Credential(0, "Alice credential 0", new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae"),
                    new Credential(1, "Alice credential 1", new Protocol(Protocol.Interface.eth, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae")
            }));
            add(new Node.Peer(1, "Bob", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", new Credential[]{
                    new Credential(2, "Bob credential 0", new Protocol(Protocol.Interface.udp, Protocol.Link.overlay), "Loremipsumdolorsitametpharetrae")
            }));
            add(new Node.Peer(2, "Caleb", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", new Credential[]{}));
            add(new Node.Peer(3, "Danielle", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", null));
        }};

        private Bus mBus;

        public Mock(Bus bus) {
            super(bus);
            mBus = bus;
        }

        @Override
        @Produce
        public PeerList produce() {
            PeerList peerList = new PeerList();
            peerList.addAll(mPeerList);
            return peerList;
        }

        @Subscribe
        public void handleEvent(PeerEvents.Create event) {
            Node.Peer peer = new Node.Peer(mPeerList.size(), UUID.randomUUID().toString(),
                    "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", null);
            mPeerList.add(peer);
            mBus.post(new PeerEvents.New(peer));
        }

        @Subscribe
        public void handleEvent(PeerEvents.Update event) {
            Iterator<Node.Peer> itr = mPeerList.iterator();
            while (itr.hasNext()) {
                Node.Peer peer = itr.next();
                if (event.mPeer.id == peer.id) {
                    peer.setOutgoingConnections(event.mPeer.getOutgoingConnections());
                    break;
                }
            }
        }

        @Subscribe
        public void handleEvent(PeerEvents.Remove event) {
            Iterator<Node.Peer> itr = mPeerList.iterator();
            while (itr.hasNext()) {
                Node.Peer peer = itr.next();
                if (event.mId == peer.id) {
                    itr.remove();
                    break;
                }
            }
        }
    }
}
