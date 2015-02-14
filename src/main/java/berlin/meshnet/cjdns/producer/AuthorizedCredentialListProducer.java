package berlin.meshnet.cjdns.producer;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import berlin.meshnet.cjdns.event.AuthorizedCredentialEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Protocol;

/**
 * Abstract class that produces the {@link AuthorizedCredentialListProducer.CredentialList}.
 */
public abstract class AuthorizedCredentialListProducer {

    public AuthorizedCredentialListProducer(Bus bus) {
        bus.register(this);
    }

    /**
     * Produces the {@link AuthorizedCredentialListProducer.CredentialList} to any subscribers. Must be annotated with {@link @Produce}.
     *
     * @return A {@link AuthorizedCredentialListProducer.CredentialList}.
     */
    public abstract CredentialList produce();

    /**
     * A list of authorized credentials.
     */
    public static class CredentialList extends ArrayList<Credential.Authorized> {
    }

    /**
     * Mock implementation of a {@link AuthorizedCredentialListProducer}.
     */
    public static class Mock extends AuthorizedCredentialListProducer {

        private CredentialList mCredentialList = new CredentialList() {{
            add(new Credential.Authorized(0, "Alice", new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential.Authorized(1, "Bob", new Protocol(Protocol.Interface.eth, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential.Authorized(2, "Caleb", new Protocol(Protocol.Interface.udp, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", false));
            add(new Credential.Authorized(3, "Danielle", new Protocol(Protocol.Interface.eth, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential.Authorized(4, "Ed", new Protocol(Protocol.Interface.udp, Protocol.Link.overlay), "Loremipsumdolorsitametpharetrae", false));
        }};

        private Bus mBus;

        public Mock(Bus bus) {
            super(bus);
            mBus = bus;
        }

        @Override
        @Produce
        public CredentialList produce() {
            CredentialList credentialList = new CredentialList();
            credentialList.addAll(mCredentialList);
            return credentialList;
        }

        @Subscribe
        public void handleEvent(AuthorizedCredentialEvents.Create event) {
            Credential.Authorized credential = new Credential.Authorized(mCredentialList.size(), UUID.randomUUID().toString(),
                    new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true);
            mCredentialList.add(credential);
            mBus.post(new AuthorizedCredentialEvents.New(credential));
        }

        @Subscribe
        public void handleEvent(AuthorizedCredentialEvents.Update event) {
            Iterator<Credential.Authorized> itr = mCredentialList.iterator();
            while (itr.hasNext()) {
                Credential.Authorized credential = itr.next();
                if (event.mCredential.id == credential.id) {
                    credential.setAllowed(event.mCredential.isAllowed());
                    break;
                }
            }
        }

        @Subscribe
        public void handleEvent(AuthorizedCredentialEvents.Remove event) {
            Iterator<Credential.Authorized> itr = mCredentialList.iterator();
            while (itr.hasNext()) {
                Credential credential = itr.next();
                if (event.mId == credential.id) {
                    itr.remove();
                    break;
                }
            }
        }
    }
}
