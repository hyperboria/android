package berlin.meshnet.cjdns.producer;

import berlin.meshnet.cjdns.model.Node;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Producer of a {@link berlin.meshnet.cjdns.model.Node.Me} stream.
 */
public interface MeProducer {

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
