package berlin.meshnet.cjdns;

import android.app.Application;
import android.content.Context;

import com.squareup.otto.Bus;

import javax.inject.Singleton;

import berlin.meshnet.cjdns.page.CredentialsPageFragment;
import berlin.meshnet.cjdns.page.MePageFragment;
import berlin.meshnet.cjdns.producer.CredentialListProducer;
import berlin.meshnet.cjdns.producer.MeProducer;
import berlin.meshnet.cjdns.producer.ThemeProducer;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

/**
 * The {@link android.app.Application}.
 */
public class CjdnsApplication extends Application {

    private ObjectGraph mObjectGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        mObjectGraph = ObjectGraph.create(new DefaultModule(this));
    }

    /**
     * Injects the dependencies of {@code object} from the {@link dagger.ObjectGraph}.
     *
     * @param object The object instance to inject dependencies.
     */
    public void inject(Object object) {
        mObjectGraph.inject(object);
    }

    /**
     * {@link dagger.Module} providing default dependencies.
     */
    @Module(
            injects = {
                    MainActivity.class,
                    MePageFragment.class,
                    CredentialsPageFragment.class
            }
    )
    public static class DefaultModule {

        private Context mContext;

        private DefaultModule(Context context) {
            mContext = context;
        }

        @Singleton
        @Provides
        public Bus provideBus() {
            return new Bus();
        }

        @Singleton
        @Provides
        public ThemeProducer provideThemeProducer(Bus bus) {
            return new ThemeProducer.VerboseMock(bus);
        }

        @Singleton
        @Provides
        public MeProducer provideMeProducer(Bus bus) {
            return new MeProducer.Mock(bus);
        }

        @Singleton
        @Provides
        public CredentialListProducer provideCredentialProducer(Bus bus) {
            return new CredentialListProducer.Mock(bus);
        }
    }
}