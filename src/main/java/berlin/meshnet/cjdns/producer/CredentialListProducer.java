package berlin.meshnet.cjdns.producer;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import berlin.meshnet.cjdns.event.CredentialEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Protocol;

/**
 * Abstract class that produces the {@link CredentialListProducer.CredentialList}.
 */
public abstract class CredentialListProducer {

    public CredentialListProducer(Bus bus) {
        bus.register(this);
    }

    /**
     * Produces the {@link CredentialListProducer.CredentialList} to any subscribers. Must be annotated with {@link @Produce}.
     *
     * @return A {@link CredentialListProducer.CredentialList}.
     */
    public abstract CredentialList produce();

    /**
     * A list of {@link berlin.meshnet.cjdns.model.Credential}s.
     */
    public static class CredentialList extends ArrayList<Credential> {
    }

    /**
     * Mock implementation of a {@link CredentialListProducer}.
     */
    public static class Mock extends CredentialListProducer {

        private CredentialList mCredentialList = new CredentialList() {{
            add(new Credential(0, "Alice", new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential(1, "Bob", new Protocol(Protocol.Interface.eth, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential(2, "Caleb", new Protocol(Protocol.Interface.udp, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", false));
            add(new Credential(3, "Danielle", new Protocol(Protocol.Interface.eth, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", true));
            add(new Credential(4, "Ed", new Protocol(Protocol.Interface.udp, Protocol.Link.overlay), "Loremipsumdolorsitametpharetrae", false));
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
        public void handleEvent(CredentialEvents.Create event) {
            Credential credential = new Credential(mCredentialList.size(), UUID.randomUUID().toString(), new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true);
            mCredentialList.add(credential);
            mBus.post(new CredentialEvents.New(credential));
        }

        @Subscribe
        public void handleEvent(CredentialEvents.Update event) {
            Iterator<Credential> itr = mCredentialList.iterator();
            while (itr.hasNext()) {
                Credential credential = itr.next();
                if (event.mCredential.id == credential.id) {
                    credential.setAllowed(event.mCredential.isAllowed());
                    break;
                }
            }
        }

        @Subscribe
        public void handleEvent(CredentialEvents.Remove event) {
            Iterator<Credential> itr = mCredentialList.iterator();
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
