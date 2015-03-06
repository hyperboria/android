package berlin.meshnet.cjdns.producer;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import berlin.meshnet.cjdns.event.AuthorizedCredentialEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Protocol;
import rx.Observable;
import rx.subjects.ReplaySubject;

/**
 * Abstract class that produces {@link berlin.meshnet.cjdns.model.Credential.Authorized}.
 */
public interface CredentialsProducer {

    Observable<Credential.Authorized> createStream();

    Observable<Credential.Authorized> updateStream();

    Observable<Credential.Authorized> removeStream();

    /**
     * Mock implementation of a {@link CredentialsProducer}.
     */
    public static class Mock implements CredentialsProducer {

        private static List<Credential.Authorized> sCredentials = new ArrayList<Credential.Authorized>() {{
            add(new Credential.Authorized(0, "Alice", new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential.Authorized(1, "Bob", new Protocol(Protocol.Interface.eth, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential.Authorized(2, "Caleb", new Protocol(Protocol.Interface.udp, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", false));
            add(new Credential.Authorized(3, "Danielle", new Protocol(Protocol.Interface.eth, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential.Authorized(4, "Ed", new Protocol(Protocol.Interface.udp, Protocol.Link.overlay), "Loremipsumdolorsitametpharetrae", false));
        }};

        private ReplaySubject<Credential.Authorized> mCreateStream = ReplaySubject.create();

        private ReplaySubject<Credential.Authorized> mUpdateStream = ReplaySubject.create();

        private ReplaySubject<Credential.Authorized> mRemoveStream = ReplaySubject.create();

        @Override
        public Observable<Credential.Authorized> createStream() {
            return mCreateStream.startWith(sCredentials);
        }

        @Override
        public Observable<Credential.Authorized> updateStream() {
            return mUpdateStream;
        }

        @Override
        public Observable<Credential.Authorized> removeStream() {
            return mRemoveStream;
        }

        @Subscribe
        public void handleEvent(AuthorizedCredentialEvents.Create event) {
            Credential.Authorized credential = new Credential.Authorized(sCredentials.size(), UUID.randomUUID().toString(),
                    new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true);
            sCredentials.add(credential);
            mCreateStream.onNext(credential);
        }

        @Subscribe
        public void handleEvent(AuthorizedCredentialEvents.Update event) {
            int index = sCredentials.indexOf(event.mCredential);
            if (sCredentials.remove(event.mCredential)) {
                sCredentials.add(index, event.mCredential);
                mUpdateStream.onNext(event.mCredential);
            }
        }

        @Subscribe
        public void handleEvent(AuthorizedCredentialEvents.Remove event) {
            if (sCredentials.remove(event.mCredential)) {
                mRemoveStream.onNext(event.mCredential);
            }
        }
    }
}
