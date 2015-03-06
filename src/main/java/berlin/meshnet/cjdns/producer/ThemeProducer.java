package berlin.meshnet.cjdns.producer;

import android.content.Context;
import android.content.SharedPreferences;

import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.model.Theme;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Abstract class that produces {@link berlin.meshnet.cjdns.model.Theme}.
 */
public interface ThemeProducer {

    /**
     * Produces {@link berlin.meshnet.cjdns.model.Theme} to any subscribers. Must be annotated with {@link @Produce}.
     *
     * @return A {@link berlin.meshnet.cjdns.model.Theme}.
     */
    Observable<Theme> stream();

    /**
     * Minimalist implementation of a {@link ThemeProducer}.
     */
    public static class MinimalMock implements ThemeProducer {

        @Override
        public Observable<Theme> stream() {
            return Observable.just(new Theme(false));
        }
    }

    /**
     * Verbose implementation of a {@link ThemeProducer}.
     */
    public static class VerboseMock implements ThemeProducer {

        @Override
        public Observable<Theme> stream() {
            return Observable.just(new Theme(true));
        }
    }

    /**
     * Verbose implementation of a {@link ThemeProducer}.
     */
    public static class Default implements ThemeProducer {

        private Context mContext;

        private SharedPreferences mSharedPreferences;

        public Default(Context context, SharedPreferences sharedPreferences) {
            mContext = context;
            mSharedPreferences = sharedPreferences;
        }

        @Override
        public Observable<Theme> stream() {
            BehaviorSubject<Theme> stream = BehaviorSubject.create();
            return stream.startWith(new Theme(mSharedPreferences.getBoolean(mContext.getString(R.string.setting_verbose_enabled_key), false)));
        }
    }
}
