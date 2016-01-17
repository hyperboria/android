package berlin.meshnet.cjdns;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.squareup.otto.Bus;

import javax.inject.Singleton;

import berlin.meshnet.cjdns.dialog.ConnectionsDialogFragment;
import berlin.meshnet.cjdns.page.AboutPageFragment;
import berlin.meshnet.cjdns.page.CredentialsPageFragment;
import berlin.meshnet.cjdns.page.MePageFragment;
import berlin.meshnet.cjdns.page.PeersPageFragment;
import berlin.meshnet.cjdns.page.SettingsPageFragment;
import berlin.meshnet.cjdns.producer.CredentialsProducer;
import berlin.meshnet.cjdns.producer.MeProducer;
import berlin.meshnet.cjdns.producer.PeersProducer;
import berlin.meshnet.cjdns.producer.SettingsProducer;
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
                    CjdnsService.class,
                    MePageFragment.class,
                    PeersPageFragment.class,
                    CredentialsPageFragment.class,
                    SettingsPageFragment.class,
                    ConnectionsDialogFragment.class,
                    AboutPageFragment.class
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

        @Singleton
        @Provides
        public Cjdroute provideCjdroute(Context context) {
            // TODO Change this conditional to (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) when VpnService is implemented.
            // TODO Use Lollipop for now to allow any API level below to connect with tun device.
            // TODO Unable to run cjdroute as root since Lollipop, so there is no point trying.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return new Cjdroute.Compat(context.getApplicationContext());
            }
            return new Cjdroute.Default(context.getApplicationContext());
        }

        @Provides
        public SettingsProducer provideSettingsProducer(Context context, SharedPreferences sharedPreferences) {
            return new SettingsProducer.Default(context, sharedPreferences);
        }

        @Provides
        public MeProducer provideMeProducer() {
            return new MeProducer.Mock();
        }

        @Provides
        public PeersProducer providePeerListProducer() {
            return new PeersProducer.Mock();
        }

        @Provides
        public CredentialsProducer provideCredentialListProducer() {
            return new CredentialsProducer.Mock();
        }
    }
}