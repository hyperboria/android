package berlin.meshnet.cjdns.producer;

import berlin.meshnet.cjdns.model.Node;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Abstract class that produces {@link berlin.meshnet.cjdns.model.Node.Me}.
 */
public interface MeProducer {

    /**
     * Produces {@link berlin.meshnet.cjdns.model.Node.Me} to any subscribers. Must be annotated with {@link @Produce}.
     *
     * @return A {@link berlin.meshnet.cjdns.model.Node.Me}.
     */
    Observable<Node.Me> stream();

    /**
     * Mock implementation of a {@link MeProducer}.
     */
    public static class Mock implements MeProducer {

        @Override
        public Observable<Node.Me> stream() {
            BehaviorSubject<Node.Me> stream = BehaviorSubject.create();
            return stream.startWith(new Node.Me("Hyperborean", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", "LoremipsumdolorsitametpraesentconsequatliberolacusmagnisEratgrav"));
        }
    }
}
