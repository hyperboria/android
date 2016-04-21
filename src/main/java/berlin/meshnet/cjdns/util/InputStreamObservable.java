package berlin.meshnet.cjdns.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import rx.Observable;
import rx.Subscriber;

/**
 * {@link Observable} from {@link InputStream}.
 */
public abstract class InputStreamObservable {

    /**
     * Creates an {@link Observable} for lines in an {@link InputStream}.
     *
     * @param is The {@link InputStream}.
     * @return The {@link Observable}.
     */
    public static Observable<String> line(final InputStream is) {
        return Observable
                .create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                subscriber.onNext(line);
                            }
                            subscriber.onCompleted();
                        } catch (IOException e) {
                            subscriber.onError(e);
                        }
                    }
                });
    }
}
