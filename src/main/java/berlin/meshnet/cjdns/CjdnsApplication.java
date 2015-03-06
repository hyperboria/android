package berlin.meshnet.cjdns;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.squareup.otto.Bus;

import javax.inject.Singleton;

import berlin.meshnet.cjdns.dialog.ConnectionsDialogFragment;
import berlin.meshnet.cjdns.page.CredentialsPageFragment;
import berlin.meshnet.cjdns.page.MePageFragment;
import berlin.meshnet.cjdns.page.PeersPageFragment;
import berlin.meshnet.cjdns.page.SettingsPageFragment;
import berlin.meshnet.cjdns.producer.CredentialsProducer;
import berlin.meshnet.cjdns.producer.MeProducer;
import berlin.meshnet.cjdns.producer.PeersProducer;
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
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
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
                    PeersPageFragment.class,
                    CredentialsPageFragment.class,
                    SettingsPageFragment.class,
                    ConnectionsDialogFragment.class
            }
    )
    public static class DefaultModule {

        private Context mContext;

        private DefaultModule(Context context) {
            mContext = context;
        }

        @Singleton
        @Provides
        public Context provideContext() {
            return mContext;
        }

        @Singleton
        @Provides
        public SharedPreferences provideSharedPreferences(Context context) {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }

        @Singleton
        @Provides
        public Bus provideBus() {
            return new Bus();
        }

        @Provides
        public ThemeProducer provideThemeProducer(Context context, SharedPreferences sharedPreferences) {
            return new ThemeProducer.Default(context, sharedPreferences);
        }

        @Provides
        public MeProducer provideMeProducer() {
            return new MeProducer.Mock();
        }

        @Provides
        public PeersProducer providePeerListProducer(Bus bus) {
            PeersProducer producer = new PeersProducer.Mock();
            bus.register(producer);
            return producer;
        }

        @Provides
        public CredentialsProducer provideCredentialListProducer(Bus bus) {
            CredentialsProducer producer = new CredentialsProducer.Mock();
            bus.register(producer);
            return producer;
        }
    }
}