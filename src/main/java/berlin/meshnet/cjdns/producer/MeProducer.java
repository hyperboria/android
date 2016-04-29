package berlin.meshnet.cjdns.producer;

import android.content.Context;

import berlin.meshnet.cjdns.CjdrouteConf;
import berlin.meshnet.cjdns.model.Node;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/**
 * Producer of a {@link berlin.meshnet.cjdns.model.Node.Me} stream.
 */
public interface MeProducer {

    Observable<Node.Me> stream(Context context);

    /**
     * Default implementation of a {@link MeProducer}.
     */
    class Default implements MeProducer {

        @Override
        public Observable<Node.Me> stream(Context context) {
            return CjdrouteConf.fetch0(context)
                    .subscribeOn(Schedulers.io());
        }
    }

    /**
     * Mock implementation of a {@link MeProducer}.
     */
    class Mock implements MeProducer {

        @Override
        public Observable<Node.Me> stream(Context context) {
            BehaviorSubject<Node.Me> stream = BehaviorSubject.create();
            return stream.startWith(new Node.Me("Hyperborean", "", "Loremipsumdolorsitametpharetraeratestvivamusrisusi.k", "LoremipsumdolorsitametpraesentconsequatliberolacusmagnisEratgrav"));
        }
    }
}
