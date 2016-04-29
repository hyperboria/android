package berlin.meshnet.cjdns.producer;

import com.squareup.otto.Subscribe;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import berlin.meshnet.cjdns.AdminApi;
import berlin.meshnet.cjdns.event.PeerEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.model.Protocol;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

/**
 * Producer of a {@link berlin.meshnet.cjdns.model.Node.Peer} themeStream.
 */
public interface PeersProducer {

    Observable<Node.Peer> createStream();

    Observable<Node.Peer> updateStream();

    Observable<Node.Peer> removeStream();

    /**
     * Default implementation of a {@link PeersProducer}.
     */
    class Default implements PeersProducer {

        private AdminApi mApi;

        private List<Node.Peer> mPeers = new ArrayList<>();

        private ReplaySubject<Node.Peer> mCreateStream = ReplaySubject.create();

        private ReplaySubject<Node.Peer> mUpdateStream = ReplaySubject.create();

        private ReplaySubject<Node.Peer> mRemoveStream = ReplaySubject.create();

        public Default() throws UnknownHostException {
            mApi = new AdminApi();
        }

        @Override
        public Observable<Node.Peer> createStream() {
            return mCreateStream.startWith(
                    Observable.create(new Observable.OnSubscribe<Node.Peer>() {
                        @Override
                        public void call(Subscriber<? super Node.Peer> subscriber) {
                            List<Node.Peer> peers = AdminApi.InterfaceController.peerStats(mApi)
                                    .toBlocking().first();
                            for (Node.Peer peer : peers) {
                                subscriber.onNext(peer);
                            }
                            subscriber.onCompleted();
                        }
                    }))
                    .subscribeOn(Schedulers.io());
        }

        @Override
        public Observable<Node.Peer> updateStream() {
            return mUpdateStream.subscribeOn(Schedulers.io());
        }

        @Override
        public Observable<Node.Peer> removeStream() {
            return mRemoveStream.subscribeOn(Schedulers.io());
        }

        @Subscribe
        public void handleEvent(PeerEvents.Create event) {
            Node.Peer peer = new Node.Peer(mPeers.size(), UUID.randomUUID().toString(), "fc::/8",
                    "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", null);
            mPeers.add(peer);
            mCreateStream.onNext(peer);
        }

        @Subscribe
        public void handleEvent(PeerEvents.Update event) {
            int index = mPeers.indexOf(event.mPeer);
            if (mPeers.remove(event.mPeer)) {
                mPeers.add(index, event.mPeer);
                mUpdateStream.onNext(event.mPeer);
            }
        }

        @Subscribe
        public void handleEvent(PeerEvents.Remove event) {
            if (mPeers.remove(event.mPeer)) {
                mRemoveStream.onNext(event.mPeer);
            }
        }
    }

    /**
     * Mock implementation of a {@link PeersProducer}.
     */
    class Mock implements PeersProducer {

        private static List<Node.Peer> sPeers = new ArrayList<Node.Peer>() {{
            add(new Node.Peer(0, "Alice", "", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", new Credential[]{
                    new Credential(0, "Alice credential 0", new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae"),
                    new Credential(1, "Alice credential 1", new Protocol(Protocol.Interface.eth, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae")
            }));
            add(new Node.Peer(1, "Bob", "", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", new Credential[]{
                    new Credential(2, "Bob credential 0", new Protocol(Protocol.Interface.udp, Protocol.Link.overlay), "Loremipsumdolorsitametpharetrae")
            }));
            add(new Node.Peer(2, "Caleb", "", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", new Credential[]{}));
            add(new Node.Peer(3, "Danielle", "", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", null));
        }};

        private ReplaySubject<Node.Peer> mCreateStream = ReplaySubject.create();

        private ReplaySubject<Node.Peer> mUpdateStream = ReplaySubject.create();

        private ReplaySubject<Node.Peer> mRemoveStream = ReplaySubject.create();

        @Override
        public Observable<Node.Peer> createStream() {
            return mCreateStream.startWith(sPeers);
        }

        @Override
        public Observable<Node.Peer> updateStream() {
            return mUpdateStream;
        }

        @Override
        public Observable<Node.Peer> removeStream() {
            return mRemoveStream;
        }

        @Subscribe
        public void handleEvent(PeerEvents.Create event) {
            Node.Peer peer = new Node.Peer(sPeers.size(), UUID.randomUUID().toString(), "",
                    "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", null);
            sPeers.add(peer);
            mCreateStream.onNext(peer);
        }

        @Subscribe
        public void handleEvent(PeerEvents.Update event) {
            int index = sPeers.indexOf(event.mPeer);
            if (sPeers.remove(event.mPeer)) {
                sPeers.add(index, event.mPeer);
                mUpdateStream.onNext(event.mPeer);
            }
        }

        @Subscribe
        public void handleEvent(PeerEvents.Remove event) {
            if (sPeers.remove(event.mPeer)) {
                mRemoveStream.onNext(event.mPeer);
            }
        }
    }
}
