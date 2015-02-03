package berlin.meshnet.cjdns.producer;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import java.util.ArrayList;

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

        public Mock(Bus bus) {
            super(bus);
        }

        @Override
        @Produce
        public CredentialList produce() {
            return new CredentialList() {{
                add(new Credential("Alice", new Protocol(Protocol.Interface.udp, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
                add(new Credential("Bob", new Protocol(Protocol.Interface.eth, Protocol.Link.wifiDirect), "Loremipsumdolorsitametpharetrae", true));
                add(new Credential("Caleb", new Protocol(Protocol.Interface.udp, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", false));
                add(new Credential("Danielle", new Protocol(Protocol.Interface.eth, Protocol.Link.bluetooth), "Loremipsumdolorsitametpharetrae", true));
                add(new Credential("Ed", new Protocol(Protocol.Interface.udp, Protocol.Link.overlay), "Loremipsumdolorsitametpharetrae", false));
            }};
        }
    }
}
